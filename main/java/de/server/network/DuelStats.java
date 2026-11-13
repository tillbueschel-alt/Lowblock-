package de.server.network;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Duel-Statistiken (Siege, Niederlagen, Siegesserie) in stats.yml plus der Befehl /duelstats. */
final class DuelStats implements CommandExecutor, TabCompleter, org.bukkit.event.Listener {
    private static DuelStats instance;

    static DuelStats get() {
        return instance;
    }

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<String, Long> recent = new HashMap<String, Long>();
    private int resultCount;

    DuelStats(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        instance = this;
    }

    /** Wird von mehreren Stellen im Duel-Ablauf aufgerufen; doppelte Meldungen werden ignoriert. */
    static void record(Player winner, Player loser) {
        DuelStats stats = instance;
        if (stats == null || winner == null || loser == null) {
            return;
        }
        if (!stats.add(winner, loser)) return;
        EventGames.duelResult(winner, loser);
        Cosmetics.killEffect(winner, loser);
        Social.summary(winner, loser);
    }

    private synchronized boolean add(Player winner, Player loser) {
        long now = System.currentTimeMillis();
        String key = winner.getUniqueId() + ">" + loser.getUniqueId();
        Long last = this.recent.get(key);
        if (last != null && now - last < 15000L) {
            return false;
        }
        this.recent.put(key, now);
        if ((++this.resultCount & 127) == 0) this.recent.entrySet().removeIf(entry -> now - entry.getValue() >= 15000L);
        if (!AntiCheat.allowStats(winner, loser)) {
            return false;
        }
        String w = "players." + winner.getUniqueId();
        String l = "players." + loser.getUniqueId();
        this.data.set(w + ".name", winner.getName());
        this.data.set(l + ".name", loser.getName());
        int wins = this.data.getInt(w + ".wins", 0) + 1;
        int streak = this.data.getInt(w + ".streak", 0) + 1;
        this.data.set(w + ".wins", wins);
        this.data.set(w + ".streak", streak);
        this.data.set(w + ".best-streak", Math.max(streak, this.data.getInt(w + ".best-streak", 0)));
        this.data.set(l + ".losses", this.data.getInt(l + ".losses", 0) + 1);
        this.data.set(l + ".streak", 0);
        // Ranked: Elo je Kit (Start 1000) + Gesamt-Elo als Summe der Aenderungen. Unranked und direkte Herausforderungen: keine Elo.
        String kit = this.plugin instanceof DuelNetworkPlugin dn ? dn.duelKitLabel(winner.getUniqueId()) : "";
        Boolean flag = this.plugin instanceof DuelNetworkPlugin dnf ? dnf.queueRankedFlag(winner.getUniqueId()) : null;
        boolean casual = flag == null || !flag || kit.isBlank();
        String slug = kit.replaceAll("§.", "").replaceAll("[^A-Za-z0-9_-]", "_");
        int kw = this.data.getInt(w + ".kits." + slug + ".elo", 1000);
        int kl = this.data.getInt(l + ".kits." + slug + ".elo", 1000);
        int[] elo = casual
                ? new int[]{kw, kl, 0, 0}
                : Rules.elo(kw, kl, this.plugin.getConfig().getInt("duels.elo-k", 32));
        if (!casual) {
            this.data.set(w + ".kits." + slug + ".elo", elo[0]);
            this.data.set(l + ".kits." + slug + ".elo", elo[1]);
            this.data.set(w + ".elo", this.data.getInt(w + ".elo", 1000) + elo[2]);
            this.data.set(l + ".elo", this.data.getInt(l + ".elo", 1000) - elo[3]);
        }
        if (!kit.isBlank()) {
            this.data.set(w + ".kits." + slug + ".name", kit.replaceAll("§.", ""));
            this.data.set(w + ".kits." + slug + ".w", this.data.getInt(w + ".kits." + slug + ".w", 0) + 1);
            this.data.set(l + ".kits." + slug + ".name", kit.replaceAll("§.", ""));
            this.data.set(l + ".kits." + slug + ".l", this.data.getInt(l + ".kits." + slug + ".l", 0) + 1);
        }
        if (!casual) {
            this.trend(w, this.data.getInt(w + ".elo", 1000));
            this.trend(l, this.data.getInt(l + ".elo", 1000));
        }
        this.remember(winner.getUniqueId().toString(), loser.getName(), true, kit, elo[2]);
        this.remember(loser.getUniqueId().toString(), winner.getName(), false, kit, -elo[3]);
        if (this.plugin instanceof DuelNetworkPlugin dnw) {
            Webhook.send(dnw, "duel", "⚔ " + winner.getName() + " besiegt " + loser.getName() + " (" + kit + ", Elo " + elo[0] + "/" + elo[1] + ")");
        }
        if (casual) {
            DuelSummary.note(winner, "§7Unranked-Duell – keine Elo-Wertung.");
            DuelSummary.note(loser, "§7Unranked-Duell – keine Elo-Wertung.");
        } else {
            DuelSummary.note(winner, "§7" + kit + "-Elo: §f" + elo[0] + " §a(+" + elo[2] + ") §8• §7Gegner-Elo: §f" + kl + (kl > kw ? " §8(stärker = mehr Punkte)" : kl < kw ? " §8(schwächer = weniger Punkte)" : ""));
            DuelSummary.note(loser, "§7" + kit + "-Elo: §f" + elo[1] + " §c(-" + elo[3] + ") §8• §7Gegner-Elo: §f" + kw);
        }
        DuelSummary.endNote(winner, true, casual ? "§7Unranked" : "§7" + kit + "-Elo §f" + elo[0] + " §a(+" + elo[2] + ")");
        DuelSummary.endNote(loser, false, casual ? "§7Unranked" : "§7" + kit + "-Elo §f" + elo[1] + " §c(-" + elo[3] + ")");
        this.save();
        if (streak >= 3) {
            winner.sendMessage("§d✦ Siegesserie: §f" + streak + " §dSiege in Folge!");
        }
        return true;
    }

    /** Werte fuer das Stats-Menue: {Elo, Siege, Niederlagen, Serie, Bestserie, Platz (0 = ungewertet)}. */
    static synchronized int[] snapshot(UUID id) {
        DuelStats stats = instance;
        if (stats == null) {
            return new int[]{1000, 0, 0, 0, 0, 0};
        }
        String k = "players." + id;
        int elo = stats.data.getInt(k + ".elo", 1000);
        int wins = stats.data.getInt(k + ".wins", 0);
        int losses = stats.data.getInt(k + ".losses", 0);
        int place = 0;
        if (wins + losses > 0) {
            place = 1;
            ConfigurationSection players = stats.data.getConfigurationSection("players");
            if (players != null) {
                for (String other : players.getKeys(false)) {
                    if (!other.equals(id.toString()) && stats.data.getInt("players." + other + ".wins", 0) + stats.data.getInt("players." + other + ".losses", 0) > 0
                            && stats.data.getInt("players." + other + ".elo", 1000) > elo) {
                        place++;
                    }
                }
            }
        }
        return new int[]{elo, wins, losses, stats.data.getInt(k + ".streak", 0), stats.data.getInt(k + ".best-streak", 0), place};
    }

    /** Zeilen fuer das Profil: je Kit Siege/Niederlagen, meistgespielte zuerst. */
    static synchronized List<String> kitLines(UUID id, int limit) {
        List<String> out = new ArrayList<String>();
        DuelStats stats = instance;
        ConfigurationSection kits = stats == null ? null : stats.data.getConfigurationSection("players." + id + ".kits");
        if (kits == null) {
            return out;
        }
        List<String[]> rows = new ArrayList<String[]>();
        for (String slug : kits.getKeys(false)) {
            int w = kits.getInt(slug + ".w", 0);
            int l = kits.getInt(slug + ".l", 0);
            rows.add(new String[]{kits.getString(slug + ".name", slug), String.valueOf(w), String.valueOf(l), String.valueOf(kits.getInt(slug + ".elo", 1000))});
        }
        rows.sort((x, y) -> Integer.compare(Integer.parseInt(y[1]) + Integer.parseInt(y[2]), Integer.parseInt(x[1]) + Integer.parseInt(x[2])));
        for (int i = 0; i < Math.min(limit, rows.size()); i++) {
            out.add("§f" + rows.get(i)[0] + " §8» §a" + rows.get(i)[1] + "S §c" + rows.get(i)[2] + "N §7Elo §b" + rows.get(i)[3]);
        }
        return out;
    }

    /** Elo des Spielers in einem Kit (Anzeigename wie im Kit-Menue); Start 1000. */
    static synchronized int kitElo(UUID id, String kitLabel) {
        DuelStats stats = instance;
        if (stats == null) {
            return 1000;
        }
        String slug = kitLabel.replaceAll("§.", "").replaceAll("[^A-Za-z0-9_-]", "_");
        return stats.data.getInt("players." + id + ".kits." + slug + ".elo", 1000);
    }

    static synchronized boolean casual(UUID id) {
        return instance != null && instance.data.getBoolean("players." + id + ".casual", false);
    }

    /** Schaltet Ranked/Casual um und liefert den neuen Zustand (true = Casual). */
    static synchronized boolean toggleCasual(UUID id) {
        DuelStats stats = instance;
        if (stats == null) {
            return false;
        }
        boolean now = !stats.data.getBoolean("players." + id + ".casual", false);
        stats.data.set("players." + id + ".casual", now);
        stats.save();
        return now;
    }

    /** Bester Spieler nach Elo als UUID + Elo (nur Spieler mit mindestens einem Duell), fuer Season-Belohnungen. */
    static synchronized List<Object[]> eloLeaders(int limit) {
        List<Object[]> rows = new ArrayList<Object[]>();
        DuelStats stats = instance;
        ConfigurationSection players = stats == null ? null : stats.data.getConfigurationSection("players");
        if (players == null) {
            return rows;
        }
        for (String id : players.getKeys(false)) {
            if (stats.data.getInt("players." + id + ".wins", 0) + stats.data.getInt("players." + id + ".losses", 0) == 0) {
                continue;
            }
            try {
                rows.add(new Object[]{UUID.fromString(id), stats.data.getInt("players." + id + ".elo", 1000), stats.data.getString("players." + id + ".name", id)});
            }
            catch (IllegalArgumentException ignored) {
                // kaputter Eintrag
            }
        }
        rows.sort((x, y) -> Integer.compare((Integer)y[1], (Integer)x[1]));
        return new ArrayList<Object[]>(rows.subList(0, Rules.boundedLimit(limit, rows.size())));
    }

    /** Neue Season: Elo wird weich zurueckgesetzt (Abstand zu 1000 wird um den Faktor halbiert). Siege/Niederlagen bleiben. */
    static synchronized void softResetElo(double keep) {
        DuelStats stats = instance;
        ConfigurationSection players = stats == null ? null : stats.data.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String id : players.getKeys(false)) {
            int elo = stats.data.getInt("players." + id + ".elo", 1000);
            stats.data.set("players." + id + ".elo", 1000 + (int)Math.round((elo - 1000) * keep));
            ConfigurationSection kc = stats.data.getConfigurationSection("players." + id + ".kits");
            if (kc != null) {
                for (String slug : kc.getKeys(false)) {
                    if (kc.contains(slug + ".elo")) {
                        kc.set(slug + ".elo", 1000 + (int)Math.round((kc.getInt(slug + ".elo", 1000) - 1000) * keep));
                    }
                }
            }
        }
        stats.save();
    }

    /** Rangliste nach einem Wert (elo, wins, best-streak): je Zeile {Name, Wert}. */
    static synchronized List<String[]> top(String key, int limit) {
        List<String[]> out = new ArrayList<String[]>();
        DuelStats stats = instance;
        if (stats == null) {
            return out;
        }
        ConfigurationSection players = stats.data.getConfigurationSection("players");
        if (players == null) {
            return out;
        }
        List<String[]> rows = new ArrayList<String[]>();
        for (String id : players.getKeys(false)) {
            int fallback = key.equals("elo") ? 1000 : 0;
            int value = stats.data.getInt("players." + id + "." + key, fallback);
            if (key.equals("elo") && stats.data.getInt("players." + id + ".wins", 0) + stats.data.getInt("players." + id + ".losses", 0) == 0) {
                continue;
            }
            if (value > 0) {
                rows.add(new String[]{stats.data.getString("players." + id + ".name", id), String.valueOf(value)});
            }
        }
        rows.sort((x, y) -> Integer.compare(Integer.parseInt(y[1]), Integer.parseInt(x[1])));
        return new ArrayList<String[]>(rows.subList(0, Rules.boundedLimit(limit, rows.size())));
    }

    /** Zeigt dem Spieler seine letzten Duelle. */
    static synchronized void showHistory(Player player) {
        DuelStats stats = instance;
        if (stats == null) {
            return;
        }
        List<String> history = stats.data.getStringList("players." + player.getUniqueId() + ".history");
        List<Integer> trend = stats.data.getIntegerList("players." + player.getUniqueId() + ".elo-trend");
        if (trend.size() >= 2) {
            int lo = Integer.MAX_VALUE;
            int hi = Integer.MIN_VALUE;
            for (int v : trend) {
                lo = Math.min(lo, v);
                hi = Math.max(hi, v);
            }
            String blocks = "\u2581\u2582\u2583\u2584\u2585\u2586\u2587\u2588";
            StringBuilder line = new StringBuilder();
            for (int v : trend) {
                int idx = hi == lo ? 3 : (int)Math.round((v - lo) * 7.0 / (hi - lo));
                line.append(blocks.charAt(idx));
            }
            int diff = trend.get(trend.size() - 1) - trend.get(0);
            player.sendMessage("\u00a77Elo-Verlauf \u00a78(letzte " + trend.size() + " gewertete Duelle): \u00a7d" + line + " \u00a77" + trend.get(0) + " \u2192 \u00a7f" + trend.get(trend.size() - 1) + " " + (diff >= 0 ? "\u00a7a+" : "\u00a7c") + diff);
        }
        if (history.isEmpty()) {
            player.sendMessage("\u00a77Noch keine Duelle gespeichert.");
            return;
        }
        player.sendMessage("\u00a7d\u00a7l\u2694 Deine letzten Duelle \u00a78(neueste zuerst)");
        for (int i = history.size() - 1; i >= 0; i--) {
            String[] p = history.get(i).split("\\|", -1);
            if (p.length < 5) {
                continue;
            }
            boolean won = p[2].equals("W");
            player.sendMessage("\u00a78" + p[0] + " " + (won ? "\u00a7a\u2714 Sieg" : "\u00a7c\u2716 Niederlage") + " \u00a77gegen \u00a7f" + p[1]
                    + " \u00a78\u2022 \u00a77" + p[3] + " \u00a78\u2022 \u00a77Elo " + (won ? "\u00a7a" : "\u00a7c") + p[4]);
        }
    }

    /** Merkt die letzten 20 Elo-Werte (nur gewertete Duelle) fuer den Verlauf. */
    private void trend(String path, int elo) {
        List<Integer> t = new ArrayList<Integer>(this.data.getIntegerList(path + ".elo-trend"));
        t.add(elo);
        while (t.size() > 20) {
            t.remove(0);
        }
        this.data.set(path + ".elo-trend", t);
    }

    private void remember(String id, String opponent, boolean won, String kit, int delta) {
        String time = new java.text.SimpleDateFormat("dd.MM. HH:mm", java.util.Locale.ROOT).format(new java.util.Date());
        List<String> history = new ArrayList<String>(this.data.getStringList("players." + id + ".history"));
        history.add(time + "|" + opponent.replace("|", "") + "|" + (won ? "W" : "L") + "|" + kit.replace("|", "") + "|" + (delta >= 0 ? "+" : "") + delta);
        while (history.size() > 10) {
            history.remove(0);
        }
        this.data.set("players." + id + ".history", history);
    }

    /** Einzelner Statistik-Wert eines Spielers (wins, losses, streak, elo ...), z. B. fuer PlaceholderAPI. */
    static int stat(UUID id, String key, int fallback) {
        DuelStats stats = instance;
        if (stats == null) {
            return fallback;
        }
        synchronized (stats) {
            return stats.data.getInt("players." + id + "." + key, fallback);
        }
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("stats.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    /* ---------- Anzeige ---------- */

    private static final int PAGE_SIZE = 10;

    private record Row(String id, String name, int kills, int deaths, int streak, int bestStreak) {
        int duels() {
            return this.kills + this.deaths;
        }

        double quote() {
            return this.duels() == 0 ? 0.0 : 100.0 * this.kills / this.duels();
        }
    }

    private List<Row> rows() {
        List<Row> rows = new ArrayList<Row>();
        ConfigurationSection players = this.data.getConfigurationSection("players");
        if (players != null) {
            for (String key : players.getKeys(false)) {
                String base = "players." + key;
                rows.add(new Row(key, this.data.getString(base + ".name", key), this.data.getInt(base + ".wins", 0),
                        this.data.getInt(base + ".losses", 0), this.data.getInt(base + ".streak", 0), this.data.getInt(base + ".best-streak", 0)));
            }
        }
        return rows;
    }

    private static String pct(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value);
    }

    /** Balken mit 10 Feldern, z.B. Siegquote. */
    private static String bar(double percent) {
        int filled = (int)Math.round(Math.max(0.0, Math.min(100.0, percent)) / 10.0);
        return "§d" + "█".repeat(filled) + "§8" + "█".repeat(10 - filled);
    }

    private static List<Row> sorted(List<Row> rows, String mode) {
        List<Row> out = new ArrayList<Row>(rows);
        switch (mode) {
            case "tode" -> out.sort(Comparator.comparingInt(Row::deaths).reversed().thenComparingInt(Row::kills));
            case "quote" -> {
                out.removeIf(r -> r.duels() < 5);
                out.sort(Comparator.comparingDouble(Row::quote).reversed().thenComparing(Comparator.comparingInt(Row::duels).reversed()));
            }
            default -> out.sort(Comparator.comparingInt(Row::kills).reversed().thenComparingInt(Row::deaths));
        }
        return out;
    }

    private static int rankOf(List<Row> ranking, String id) {
        for (int i = 0; i < ranking.size(); i++) {
            if (ranking.get(i).id().equals(id)) {
                return i + 1;
            }
        }
        return -1;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rangliste")) {
            if (sender instanceof Player p) {
                String first = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "kills";
                if (first.equals("parkour")) {
                    this.openParkourPicker(p);
                } else {
                    this.openDuelGui(p, first.equals("tode") || first.equals("quote") ? first : "kills");
                }
            } else {
                this.sendTop(sender, "kills", 1);
            }
            return true;
        }
        if (sender instanceof Player gp && args.length >= 1 && (args[0].equalsIgnoreCase("top") || args[0].equalsIgnoreCase("rangliste") || args[0].equalsIgnoreCase("bestenliste")) && !(args.length >= 2 && args[args.length - 1].equalsIgnoreCase("chat"))) {
            String mode = "kills";
            for (int i = 1; i < args.length; i++) {
                String a = args[i].toLowerCase(Locale.ROOT);
                if (a.equals("tode") || a.equals("deaths")) {
                    mode = "tode";
                } else if (a.equals("quote") || a.equals("winrate")) {
                    mode = "quote";
                }
            }
            this.openDuelGui(gp, mode);
            return true;
        }
        if (args.length >= 1 && (args[0].equalsIgnoreCase("top") || args[0].equalsIgnoreCase("bestenliste") || args[0].equalsIgnoreCase("rangliste"))) {
            String mode = "kills";
            int page = 1;
            for (int i = 1; i < args.length; i++) {
                String a = args[i].toLowerCase(Locale.ROOT);
                if (a.matches("\\d{1,3}")) {
                    page = Math.max(1, Integer.parseInt(a));
                } else if (a.equals("tode") || a.equals("deaths")) {
                    mode = "tode";
                } else if (a.equals("quote") || a.equals("winrate")) {
                    mode = "quote";
                } else if (a.equals("kills")) {
                    mode = "kills";
                }
            }
            this.sendTop(sender, mode, page);
            return true;
        }
        List<Row> rows = this.rows();
        Row me = null;
        if (args.length >= 1) {
            for (Row r : rows) {
                if (r.name().equalsIgnoreCase(args[0]) || r.id().equalsIgnoreCase(args[0])) {
                    me = r;
                }
            }
            if (me == null) {
                sender.sendMessage("§cFür diesen Spieler gibt es noch keine Duel-Statistik.");
                return true;
            }
        } else if (sender instanceof Player p) {
            for (Row r : rows) {
                if (r.id().equals(p.getUniqueId().toString())) {
                    me = r;
                }
            }
            if (me == null) {
                sender.sendMessage("§7Du hast noch kein Duel gespielt. §d/duel");
                return true;
            }
        } else {
            sender.sendMessage("Nutze: /duelstats <Spieler|top>");
            return true;
        }
        int totalKills = 0;
        int totalDeaths = 0;
        for (Row r : rows) {
            totalKills += r.kills();
            totalDeaths += r.deaths();
        }
        double killShare = totalKills == 0 ? 0.0 : 100.0 * me.kills() / totalKills;
        double deathShare = totalDeaths == 0 ? 0.0 : 100.0 * me.deaths() / totalDeaths;
        String kd = me.deaths() == 0 ? String.valueOf(me.kills()) : String.format(Locale.ROOT, "%.2f", (double)me.kills() / me.deaths());
        int rankKills = rankOf(sorted(rows, "kills"), me.id());
        int rankDeaths = rankOf(sorted(rows, "tode"), me.id());
        int rankQuote = rankOf(sorted(rows, "quote"), me.id());
        sender.sendMessage("");
        sender.sendMessage(ParkourEnhancements.BANNER);
        sender.sendMessage("§d§l⚔ Duel-Statistik §8» §f§l" + me.name());
        sender.sendMessage("");
        sender.sendMessage(" §a✔ Kills: §f" + me.kills() + " §8(" + pct(killShare) + " aller Kills)  §7Platz §d" + (rankKills < 0 ? "-" : "#" + rankKills));
        sender.sendMessage(" §c✘ Tode: §f" + me.deaths() + " §8(" + pct(deathShare) + " aller Tode)  §7Platz §d" + (rankDeaths < 0 ? "-" : "#" + rankDeaths));
        sender.sendMessage(" §d✦ Siegquote: §f" + (me.duels() == 0 ? "-" : pct(me.quote())) + " " + bar(me.quote()) + " §7Platz §d" + (rankQuote < 0 ? "§8(ab 5 Duels)" : "#" + rankQuote));
        sender.sendMessage(" §7⚖ K/D: §f" + kd + " §8• §7Duels: §f" + me.duels());
        sender.sendMessage(" §6♛ Serie: §f" + me.streak() + " §8(Rekord §f" + me.bestStreak() + "§8)");
        sender.sendMessage(" §b⚔ Elo: §f" + stat(UUID.fromString(me.id()), "elo", 1000));
        sender.sendMessage("");
        sender.sendMessage("§7Rangliste (Men\u00fc): §d/rangliste §8| §7Chat: §d/duelstats top chat");
        sender.sendMessage(ParkourEnhancements.LINE);
        return true;
    }

    private void sendTop(CommandSender sender, String mode, int requestedPage) {
        List<Row> all = this.rows();
        List<Row> ranking = sorted(all, mode);
        int totalKills = 0;
        int totalDeaths = 0;
        for (Row r : all) {
            totalKills += r.kills();
            totalDeaths += r.deaths();
        }
        int pages = Math.max(1, (ranking.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(requestedPage, pages);
        String meId = sender instanceof Player p ? p.getUniqueId().toString() : "";
        String title = mode.equals("tode") ? "Meiste Tode" : mode.equals("quote") ? "Beste Siegquote" : "Meiste Kills";
        sender.sendMessage("");
        sender.sendMessage(ParkourEnhancements.BANNER);
        sender.sendMessage("§d§l⚔ Duel-Rangliste §8» §f§l" + title);
        sender.sendMessage("");
        if (ranking.isEmpty()) {
            sender.sendMessage(mode.equals("quote") ? "§7Noch niemand mit mindestens 5 Duels." : "§7Noch keine Duels gespielt.");
        }
        int from = (page - 1) * PAGE_SIZE;
        for (int i = from; i < Math.min(ranking.size(), from + PAGE_SIZE); i++) {
            Row r = ranking.get(i);
            sender.sendMessage(this.line(i, r, mode, totalKills, totalDeaths, r.id().equals(meId)));
        }
        int myRank = rankOf(ranking, meId);
        if (myRank > from + PAGE_SIZE) {
            sender.sendMessage("§8 ...");
            sender.sendMessage(this.line(myRank - 1, ranking.get(myRank - 1), mode, totalKills, totalDeaths, true));
        }
        sender.sendMessage("");
        String switchHint = "§d/duelstats top " + (mode.equals("kills") ? "tode" : "kills");
        sender.sendMessage((pages > 1 ? "§7Seite §f" + page + "§7/§f" + pages + " §8• " : "") + switchHint + " §8• §d/duelstats <Spieler>");
        sender.sendMessage(ParkourEnhancements.LINE);
    }

    private String line(int index, Row r, String mode, int totalKills, int totalDeaths, boolean you) {
        String name = r.name();
        String value;
        if (mode.equals("tode")) {
            value = "§c" + r.deaths() + " §7Tode §8(" + pct(totalDeaths == 0 ? 0.0 : 100.0 * r.deaths() / totalDeaths) + ") §8• §a" + r.kills() + " §7Kills";
        } else if (mode.equals("quote")) {
            value = "§d" + pct(r.quote()) + " §8• §a" + r.kills() + "§7/§c" + r.deaths();
        } else {
            value = "§a" + r.kills() + " §7Kills §8(" + pct(totalKills == 0 ? 0.0 : 100.0 * r.kills() / totalKills) + ") §8• §c" + r.deaths() + " §7Tode";
        }
        String mark = you ? " §d◀ Du" : "";
        return switch (index) {
            case 0 -> " §6§l♛ 1. §e§l" + name + " §8» " + value + mark;
            case 1 -> " §f§l★ 2. §f" + name + " §8» " + value + mark;
            case 2 -> " §c§l✦ 3. §c" + name + " §8» " + value + mark;
            default -> " §8▪ §7" + (index + 1) + ". §7" + name + " §8» " + value + mark;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        if (command.getName().equalsIgnoreCase("rangliste")) {
            if (args.length == 1) {
                for (String mode : List.of("kills", "tode", "quote", "parkour")) {
                    if (mode.startsWith(typed)) {
                        out.add(mode);
                    }
                }
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            for (String mode : List.of("kills", "tode", "quote", "chat")) {
                if (mode.startsWith(typed)) {
                    out.add(mode);
                }
            }
            return out;
        }
        if (args.length != 1) {
            return out;
        }
        if ("top".startsWith(typed)) {
            out.add("top");
        }
        ConfigurationSection players = this.data.getConfigurationSection("players");
        if (players != null) {
            for (String key : players.getKeys(false)) {
                String n = this.data.getString("players." + key + ".name", "");
                if (!n.isEmpty() && n.toLowerCase(Locale.ROOT).startsWith(typed)) {
                    out.add(n);
                }
            }
        }
        return out;
    }

    /* ---------- Ranglisten-Men\u00fc (Duel + Parkour) ---------- */

    private static final String GUI = "Rangliste";
    private static final int[] HEAD_SLOT = {13, 21, 23};
    private static final int[][] PILLAR_SLOTS = {{22, 31}, {30}, {32}};
    private static final int[] REST = {37, 38, 39, 40, 41, 42, 43};
    private static final Material[] PILLAR = {Material.YELLOW_STAINED_GLASS_PANE, Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE};
    private static final String[] PILLAR_NAME = {"§6§l\u2655 1. PLATZ", "§f§l\u2726 2. PLATZ", "§c§l\u2726 3. PLATZ"};

    private static final String SEP = "§8§m                         ";

    /** Setzt Spielerkopf (Platz 1-3 auf dem Podest mit Saeule, 4-10 in der unteren Reihe). */
    private void place(Inventory inv, int index, String uuid, String name, boolean you, String... lore) {
        String title = (index < 3 ? rankColor(index) + "#" + (index + 1) + " §f§l" : "§7#" + (index + 1) + " §f") + name + (you ? " §d\u25c0 Du" : "");
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        meta.setDisplayName(title);
        List<String> full = new ArrayList<String>();
        full.add(SEP);
        full.addAll(java.util.Arrays.asList(lore));
        full.add(SEP);
        meta.setLore(full);
        if (meta instanceof org.bukkit.inventory.meta.SkullMeta skull) {
            try {
                skull.setOwningPlayer(this.plugin.getServer().getOfflinePlayer(java.util.UUID.fromString(uuid)));
            }
            catch (Exception ignored) {
                Store.problem("DuelStats:378", ignored);
            }
        }
        head.setItemMeta(meta);
        if (index < 3) {
            inv.setItem(HEAD_SLOT[index], head);
            for (int pillar : PILLAR_SLOTS[index]) {
                inv.setItem(pillar, item(PILLAR[index], PILLAR_NAME[index], "§7" + name));
            }
        } else if (index - 3 < REST.length) {
            inv.setItem(REST[index - 3], head);
        }
    }

    private static ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(java.util.Arrays.asList(lore));
        ItemSkins.hideIfBlank(meta);
        stack.setItemMeta(meta);
        return stack;
    }

    private static Inventory frame(String title, int size) {
        Inventory inv = instance.plugin.getServer().createInventory(null, size, "§8" + GUI + " §8» §d" + title);
        ItemStack border = item(Material.PURPLE_STAINED_GLASS_PANE, " ");
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < size; i++) {
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            inv.setItem(i, edge ? border : filler);
        }
        return inv;
    }

    private static Material medal(int index) {
        return switch (index) {
            case 0 -> Material.GOLD_INGOT;
            case 1 -> Material.IRON_INGOT;
            case 2 -> Material.COPPER_INGOT;
            default -> Material.PAPER;
        };
    }

    private static String rankColor(int index) {
        return index == 0 ? "§6§l" : index == 1 ? "§f§l" : index == 2 ? "§c§l" : "§7";
    }

    /** Obere Reihe: Kategorien (Duelle / SMP / Parkour), in allen Ranglisten-Menues gleich. */
    static void categories(Inventory inv, String cat) {
        inv.setItem(2, item(Material.DIAMOND_SWORD, (cat.equals("duel") ? "§a\u25b6 " : "§f") + "§lDuelle", "§7Kills, Tode und Siegquote", cat.equals("duel") ? "§aAusgew\u00e4hlt" : "§eKlicken zum Anzeigen"));
        inv.setItem(4, item(Material.GOLD_INGOT, (cat.equals("smp") ? "§a\u25b6 " : "§f") + "§lSMP", "§7Geld, Spielzeit und Kills", cat.equals("smp") ? "§aAusgew\u00e4hlt" : "§eKlicken zum Anzeigen"));
        inv.setItem(6, item(Material.FEATHER, (cat.equals("parkour") ? "§a\u25b6 " : "§f") + "§lParkour", "§7Bestzeiten je Strecke", cat.equals("parkour") ? "§aAusgew\u00e4hlt" : "§eKlicken zum Anzeigen"));
    }

    private void tabs(Inventory inv, String selected) {
        boolean duel = selected.equals("kills") || selected.equals("tode") || selected.equals("quote");
        categories(inv, duel ? "duel" : "parkour");
        if (duel) {
            inv.setItem(46, item(Material.DIAMOND_SWORD, (selected.equals("kills") ? "§a\u25b6 " : "§f") + "§lMeiste Kills", "§7Duel-Rangliste nach Siegen", selected.equals("kills") ? "§aAusgew\u00e4hlt" : "§eKlicken zum Anzeigen"));
            inv.setItem(47, item(Material.SKELETON_SKULL, (selected.equals("tode") ? "§a\u25b6 " : "§f") + "§lMeiste Tode", "§7Duel-Rangliste nach Niederlagen", selected.equals("tode") ? "§aAusgew\u00e4hlt" : "§eKlicken zum Anzeigen"));
            inv.setItem(48, item(Material.NETHER_STAR, (selected.equals("quote") ? "§a\u25b6 " : "§f") + "§lBeste Siegquote", "§7Anteil gewonnener Duels", "§8(ab 5 Duels)", selected.equals("quote") ? "§aAusgew\u00e4hlt" : "§eKlicken zum Anzeigen"));
        }
    }

    void openDuelGui(Player player, String mode) {
        List<Row> all = this.rows();
        List<Row> ranking = sorted(all, mode);
        int totalKills = 0;
        int totalDeaths = 0;
        for (Row r : all) {
            totalKills += r.kills();
            totalDeaths += r.deaths();
        }
        String title = mode.equals("tode") ? "Tode" : mode.equals("quote") ? "Siegquote" : "Kills";
        Inventory inv = frame("Duel " + title, 54);
        this.tabs(inv, mode);
        String meId = player.getUniqueId().toString();
        for (int i = 0; i < Math.min(10, ranking.size()); i++) {
            Row r = ranking.get(i);
            boolean you = r.id().equals(meId);
            double killShare = totalKills == 0 ? 0.0 : 100.0 * r.kills() / totalKills;
            double deathShare = totalDeaths == 0 ? 0.0 : 100.0 * r.deaths() / totalDeaths;
            String kd = r.deaths() == 0 ? String.valueOf(r.kills()) : String.format(Locale.ROOT, "%.2f", (double)r.kills() / r.deaths());
            String headline = mode.equals("tode") ? "§c" + r.deaths() + " Tode §8(" + pct(deathShare) + ")" : mode.equals("quote") ? "§d" + pct(r.quote()) + " Siegquote" : "§a" + r.kills() + " Kills §8(" + pct(killShare) + ")";
            this.place(inv, i, r.id(), r.name(), you,
                    headline,
                    "§7Kills: §a" + r.kills() + " §8\u2502 §7Tode: §c" + r.deaths(),
                    "§7K/D: §f" + kd + " §8\u2502 §7Duels: §f" + r.duels(),
                    "§7Siegquote: §f" + pct(r.quote()),
                    bar(r.quote()),
                    "§7Serie: §6" + r.streak() + " §8(Rekord " + r.bestStreak() + ")");
        }
        if (ranking.isEmpty()) {
            inv.setItem(22, item(Material.BARRIER, "§7Noch keine Eintr\u00e4ge", mode.equals("quote") ? "§8Es braucht mindestens 5 Duels." : "§8Spiele ein Duel mit /duel <Spieler>."));
        }
        Row me = null;
        for (Row r : all) {
            if (r.id().equals(meId)) {
                me = r;
            }
        }
        if (me == null) {
            inv.setItem(49, item(Material.BOOK, "§d§lDeine Statistik", "§7Du hast noch kein Duel gespielt.", "§7Starte eins mit §d/duel <Spieler>"));
        } else {
            int rk = rankOf(sorted(all, "kills"), meId);
            int rd = rankOf(sorted(all, "tode"), meId);
            int rq = rankOf(sorted(all, "quote"), meId);
            inv.setItem(49, item(Material.BOOK, "§d§lDeine Statistik",
                    "§7Kills: §a" + me.kills() + " §8(Platz " + (rk < 0 ? "-" : "#" + rk) + ")",
                    "§7Tode: §c" + me.deaths() + " §8(Platz " + (rd < 0 ? "-" : "#" + rd) + ")",
                    "§7Quote: §d" + pct(me.quote()) + " §8(Platz " + (rq < 0 ? "ab 5 Duels" : "#" + rq) + ")",
                    "§7Serie: §6" + me.streak() + " §8(Rekord " + me.bestStreak() + ")"));
        }
        player.openInventory(inv);
    }

    void openParkourPicker(Player player) {
        DuelNetworkPlugin main = (DuelNetworkPlugin)this.plugin;
        ConfigurationSection maps = this.plugin.getConfig().getConfigurationSection("parkour.maps");
        Inventory inv = frame("Parkour", 54);
        this.tabs(inv, "parkour");
        int slot = 19;
        if (maps != null) {
            for (String map : maps.getKeys(false)) {
                if (slot > 43) {
                    break;
                }
                List<RecordStore.Entry> top = main.records.top(map);
                String best = top.isEmpty() ? "§8noch keine Zeit" : "§e" + ParkourEnhancements.formatTime(top.get(0).timeMs()) + " §7von §f" + top.get(0).name();
                inv.setItem(slot++, item(main.icon("map." + map.toLowerCase(), Material.FEATHER), "§f" + map.replace('-', ' ').replace('_', ' '), "§7L\u00e4ufer: §f" + top.size(), "§7Bestzeit: " + best, "§eKlicken: Bestenliste"));
                if (slot % 9 == 8) {
                    slot += 2;
                }
            }
        }
        if (slot == 19) {
            inv.setItem(22, item(Material.BARRIER, "§7Keine Parkour-Strecken vorhanden"));
        }
        player.openInventory(inv);
    }

    void openParkourGui(Player player, String map) {
        DuelNetworkPlugin main = (DuelNetworkPlugin)this.plugin;
        List<RecordStore.Entry> rows = main.records.top(map);
        Inventory inv = frame("Parkour " + map.replace('-', ' ').replace('_', ' '), 54);
        this.tabs(inv, "parkour");
        String meId = player.getUniqueId().toString();
        long best = rows.isEmpty() ? 0L : rows.get(0).timeMs();
        int myRank = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).id().equals(meId)) {
                myRank = i + 1;
            }
        }
        for (int i = 0; i < Math.min(10, rows.size()); i++) {
            RecordStore.Entry row = rows.get(i);
            boolean you = row.id().equals(meId);
            this.place(inv, i, row.id(), row.name(), you,
                    "§7Zeit: §e" + ParkourEnhancements.formatTime(row.timeMs()),
                    i == 0 ? "§6\u2655 Streckenrekord" : "§7+" + ParkourEnhancements.formatTime(row.timeMs() - best) + " zum Rekord");
        }
        if (rows.isEmpty()) {
            inv.setItem(22, item(Material.BARRIER, "§7Noch keine Zeiten", "§7Sei der Erste: §d/parkour join " + map));
        }
        inv.setItem(45, item(Material.ARROW, "§7\u00ab Zur\u00fcck zur Streckenwahl"));
        inv.setItem(49, item(Material.BOOK, "§d§lDein Platz", myRank < 0 ? "§7Noch keine Zeit auf dieser Strecke." : "§7Platz §d#" + myRank + "§7: §e" + ParkourEnhancements.formatTime(rows.get(myRank - 1).timeMs())));
        player.openInventory(inv);
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains(GUI + " §8» ") || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        int slot = event.getRawSlot();
        boolean parkourView = title.contains("Parkour");
        if (slot == 2) {
            this.openDuelGui(player, "kills");
        } else if (slot == 4) {
            if (SmpTop.instance != null) {
                SmpTop.instance.open(player, "geld");
            }
        } else if (slot == 6 || (slot == 45 && parkourView)) {
            this.openParkourPicker(player);
        } else if (!parkourView && slot == 46) {
            this.openDuelGui(player, "kills");
        } else if (!parkourView && slot == 47) {
            this.openDuelGui(player, "tode");
        } else if (!parkourView && slot == 48) {
            this.openDuelGui(player, "quote");
        } else if (slot >= 19 && clicked.getType() != Material.BARRIER && clicked.getType() != Material.PURPLE_STAINED_GLASS_PANE && clicked.getType() != Material.BLACK_STAINED_GLASS_PANE) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String shown = ChatColor.stripColor(meta.getDisplayName());
                ConfigurationSection maps = this.plugin.getConfig().getConfigurationSection("parkour.maps");
                if (maps != null) {
                    for (String map : maps.getKeys(false)) {
                        if (map.replace('-', ' ').replace('_', ' ').equalsIgnoreCase(shown)) {
                            this.openParkourGui(player, map);
                            return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onGuiDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().contains(GUI + " §8» ")) {
            event.setCancelled(true);
        }
    }
}
