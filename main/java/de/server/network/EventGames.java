package de.server.network;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Fertige Events per Befehl: FFA (alle gegen alle, z.B. 10 Min Anmeldezeit) und 1v1-Turnier mit automatischem Turnierbaum.
 * Admin: /duelladmin game ffa|turnier [Kit] [Minuten] | startnow | stop | status
 * Spieler: /events join | leave | bracket (oder die Buttons im Chat).
 */
final class EventGames implements Listener {
    static EventGames instance;

    private static final class Match {
        final UUID a;
        final UUID b;
        final String na;
        final String nb;
        UUID winner;
        boolean running;
        long waitingSince = System.currentTimeMillis();

        Match(UUID a, String na, UUID b, String nb) {
            this.a = a;
            this.na = na;
            this.b = b;
            this.nb = nb;
        }
    }

    private final DuelNetworkPlugin plugin;
    private boolean ffa;
    private boolean joining;
    private boolean running;
    private String kit = "";
    private long joinEnd;
    private final Set<UUID> joined = new LinkedHashSet<UUID>();
    private final java.util.Map<UUID, String> names = new java.util.HashMap<UUID, String>();
    private int announced = Integer.MAX_VALUE;
    private int task = -1;
    private String ffaMatch;
    private final List<List<Match>> rounds = new ArrayList<List<Match>>();
    private final List<UUID> advancing = new ArrayList<UUID>();
    private long nextRoundAt;

    EventGames(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    private String label() {
        return this.ffa ? "FFA" : "Turnier";
    }

    private boolean active() {
        return this.joining || this.running;
    }

    /** Vom Event-Plan: beim Start eines Events automatisch die Anmeldung fuer FFA/Turnier oeffnen. */
    static void autoStart(boolean ffa, String kit) {
        if (instance == null || instance.active()) {
            return;
        }
        String chosen = kit == null || kit.isBlank() ? instance.plugin.getConfig().getString("eventgames.default-kit", "sword pvp") : kit;
        if (!instance.validKit(chosen)) {
            instance.plugin.getLogger().warning("Event-Spiel: Kit '" + chosen + "' ist nicht verfuegbar.");
            return;
        }
        instance.open(ffa, chosen, instance.plugin.getConfig().getInt("eventgames.join-minutes", 10));
    }

    /* ---------- Admin ---------- */

    static void admin(DuelNetworkPlugin plugin, Player admin, String[] args) {
        if (instance != null) {
            instance.handleAdmin(admin, args);
        }
    }

    private void handleAdmin(Player admin, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "ffa":
            case "turnier": {
                if (this.active()) {
                    admin.sendMessage("§cEs läuft schon ein " + this.label() + ". §7Beenden mit §d/duelladmin game stop");
                    return;
                }
                String chosen = args.length >= 3 ? args[2] : this.plugin.getConfig().getString("eventgames.default-kit", "sword pvp");
                if (args.length >= 3 && args.length >= 4 && !args[args.length - 1].matches("\\d+")) {
                    chosen = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                }
                int minutes = this.plugin.getConfig().getInt("eventgames.join-minutes", 10);
                if (args.length >= 3 && args[args.length - 1].matches("\\d+")) {
                    minutes = Math.max(1, Math.min(120, Integer.parseInt(args[args.length - 1])));
                    if (args.length == 3) {
                        chosen = this.plugin.getConfig().getString("eventgames.default-kit", "sword pvp");
                    } else {
                        chosen = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length - 1));
                    }
                }
                if (!this.validKit(chosen)) {
                    admin.sendMessage("§cKit nicht gefunden: §f" + chosen + " §7(verfügbar: " + String.join(", ", this.plugin.getConfig().getStringList("kits.available")) + ")");
                    return;
                }
                this.open(sub.equals("ffa"), chosen, minutes);
                admin.sendMessage("§a" + this.label() + " eröffnet: Kit §f" + chosen + "§a, Anmeldung " + minutes + " Min.");
                return;
            }
            case "startnow": {
                if (!this.joining) {
                    admin.sendMessage("§cZurzeit läuft keine Anmeldung.");
                    return;
                }
                this.begin();
                return;
            }
            case "stop": {
                if (!this.active()) {
                    admin.sendMessage("§cEs läuft kein Event.");
                    return;
                }
                this.stop("§cDas " + this.label() + " wurde von einem Admin beendet.");
                admin.sendMessage("§aBeendet.");
                return;
            }
            case "status": {
                admin.sendMessage(this.active() ? "§d" + this.label() + " §7Kit §f" + this.kit + " §7• Teilnehmer: §f" + this.joined.size() + " §7• " + (this.joining ? "Anmeldung" : "läuft") : "§7Kein Event aktiv.");
                return;
            }
            default:
                admin.sendMessage("§d/duelladmin game ffa [Kit] [Minuten] §7– FFA mit Anmeldezeit (Standard 10 Min)");
                admin.sendMessage("§d/duelladmin game turnier [Kit] [Minuten] §7– 1v1-Turnier mit Turnierbaum");
                admin.sendMessage("§d/duelladmin game startnow §7| §dstop §7| §dstatus");
        }
    }

    private boolean validKit(String kit) {
        return kit != null && !kit.isBlank() && this.plugin.getConfig().getStringList("kits.available").stream().anyMatch(v -> v.equalsIgnoreCase(kit));
    }

    /* ---------- Anmeldung ---------- */

    private void open(boolean ffa, String kit, int minutes) {
        this.reset();
        this.ffa = ffa;
        this.kit = kit.toLowerCase(Locale.ROOT);
        this.joining = true;
        this.joinEnd = System.currentTimeMillis() + minutes * 60000L;
        this.announced = Integer.MAX_VALUE;
        this.task = this.plugin.getServer().getScheduler().runTaskTimer((Plugin)this.plugin, Perf.wrap("EventGames", () -> this.tick()), 20L, 20L).getTaskId();
        this.announce(true);
    }

    private void announce(boolean title) {
        long left = Math.max(0L, (this.joinEnd - System.currentTimeMillis()) / 1000L);
        String time = left >= 60L ? (left / 60L) + " Min" + (left % 60L > 0L ? " " + (left % 60L) + " s" : "") : left + " s";
        for (Player p : this.plugin.getServer().getOnlinePlayers()) {
            if (this.joined.contains(p.getUniqueId())) {
                continue;
            }
            Chat.buttons(p, "§d✦ §f" + this.label() + " §7(Kit §f" + this.plugin.displayKitName(this.kit) + "§7) – Anmeldung noch §e" + time + "§7. §8Teilnehmer: " + this.joined.size(),
                    new String[]{"§a§l[✔ Mitmachen]", "/duelnetwork:events join", "Klicken, um dich anzumelden"});
            if (title) {
                p.sendTitle("§d§l" + this.label().toUpperCase(Locale.ROOT), "§fAnmeldung offen: §e/events join", 10, 60, 20);
                Chat.ping(p);
            }
        }
    }

    static void join(Player player) {
        if (instance == null) {
            player.sendMessage("§cZurzeit läuft kein Event zum Mitmachen.");
            return;
        }
        instance.doJoin(player);
    }

    private void doJoin(Player player) {
        UUID id = player.getUniqueId();
        if (!this.joining) {
            player.sendMessage(this.running ? "§cDas " + this.label() + " läuft schon – die Anmeldung ist vorbei." : "§cZurzeit läuft kein Event zum Mitmachen.");
            Chat.no(player);
            return;
        }
        if (this.joined.contains(id)) {
            player.sendMessage("§7Du bist schon angemeldet. Abmelden: §d/events leave");
            return;
        }
        String why = this.blocked(player);
        if (why != null) {
            player.sendMessage("§c" + why);
            Chat.no(player);
            return;
        }
        this.joined.add(id);
        this.names.put(id, player.getName());
        Chat.yes(player);
        player.sendMessage("§a✔ Angemeldet für das " + this.label() + "! §7Bleib in der Lobby – es geht automatisch los. Teilnehmer: §f" + this.joined.size());
    }

    static void leave(Player player) {
        if (instance == null || !instance.joined.remove(player.getUniqueId())) {
            player.sendMessage("§7Du bist für kein Event angemeldet.");
            return;
        }
        player.sendMessage("§7Du hast dich abgemeldet.");
        if (instance.running && !instance.ffa) {
            instance.forfeit(player.getUniqueId());
        }
    }

    private String blocked(Player player) {
        UUID id = player.getUniqueId();
        if (!"lobby".equals(this.plugin.profiles.getOrDefault(id, "lobby"))) {
            return "Geh zuerst in die Lobby (/lobby), dann kannst du mitmachen.";
        }
        if (this.plugin.activeDuelPlayers.contains(id) || this.plugin.activePartyMatches.containsKey(id) || this.plugin.activeParkourMaps.containsKey(id)) {
            return "Beende zuerst deine laufende Runde (/lobby).";
        }
        return null;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (this.joining) {
            // Abmeldung beim Logout: wer sich ausloggt, ist nicht mehr angemeldet
            if (this.joined.remove(id)) {
                this.names.remove(id);
            }
        } else if (this.running && !this.ffa && this.joined.contains(id)) {
            // Turnier laeuft: noch nicht gestartetes Match wird kampflos gewertet (laufende Kaempfe regelt das Duell-System)
            this.forfeit(id);
        }
    }

    /* ---------- Ablauf ---------- */

    private void tick() {
        try {
            long now = System.currentTimeMillis();
            if (this.joining) {
                int left = (int)Math.max(0L, (this.joinEnd - now) / 1000L);
                int[] marks = {600, 300, 120, 60, 30, 10};
                for (int mark : marks) {
                    if (left <= mark && this.announced > mark) {
                        this.announced = mark;
                        if (left > 0) {
                            this.announce(false);
                        }
                        break;
                    }
                }
                for (Player p : this.plugin.getServer().getOnlinePlayers()) {
                    if (this.joined.contains(p.getUniqueId())) {
                        p.sendActionBar(net.kyori.adventure.text.Component.text("§d" + this.label() + " §7startet in §e" + (left / 60) + ":" + String.format("%02d", left % 60) + " §8• §7" + this.joined.size() + " angemeldet"));
                    }
                }
                if (left <= 0) {
                    this.begin();
                }
                return;
            }
            if (this.running && !this.ffa) {
                this.pump(now);
            }
        }
        catch (Throwable t) {
            this.plugin.getLogger().warning("Event-Spiel: " + t);
        }
    }

    private void begin() {
        this.joining = false;
        Webhook.send(this.plugin, "event", "🎮 Event gestartet: " + this.label() + " (" + this.joined.size() + " Teilnehmer)");
        List<Player> players = new ArrayList<Player>();
        for (UUID id : new ArrayList<UUID>(this.joined)) {
            Player p = this.plugin.getServer().getPlayer(id);
            if (p != null && p.isOnline() && this.blocked(p) == null) {
                players.add(p);
            } else {
                this.joined.remove(id);
                if (p != null) {
                    p.sendMessage("§cDu kannst gerade nicht teilnehmen und wurdest abgemeldet.");
                }
            }
        }
        if (players.size() < 2) {
            this.broadcastAll("§c" + this.label() + " abgesagt: zu wenige Teilnehmer (mindestens 2).");
            this.reset();
            return;
        }
        this.running = true;
        if (this.ffa) {
            this.plugin.startPartyMatch(players.get(0), players, this.kit, true);
            this.ffaMatch = this.plugin.activePartyMatches.get(players.get(0).getUniqueId());
            if (this.ffaMatch == null) {
                this.broadcastAll("§cFFA konnte nicht starten (keine freie Arena eingerichtet?).");
                this.reset();
                return;
            }
            this.broadcastAll("§d✦ FFA gestartet mit §f" + players.size() + " §dSpielern! Der Letzte gewinnt.");
            return;
        }
        this.advancing.clear();
        for (Player p : players) {
            this.advancing.add(p.getUniqueId());
        }
        Collections.shuffle(this.advancing);
        this.broadcastAll("§d✦ Turnier gestartet mit §f" + players.size() + " §dSpielern! Kit: §f" + this.plugin.displayKitName(this.kit));
        this.nextRound();
    }

    /* ---------- FFA-Ende ---------- */

    /** Vom Hauptplugin: ein Party-Match ist zu Ende. */
    static void partyFinished(String match, UUID winner) {
        if (instance != null && instance.running && instance.ffa && match != null && match.equals(instance.ffaMatch)) {
            instance.finishFfa(winner);
        }
    }

    private void finishFfa(UUID winner) {
        String name = winner == null ? null : this.names.getOrDefault(winner, "?");
        if (name == null) {
            this.broadcastAll("§d✦ FFA beendet – kein Sieger.");
        } else {
            this.broadcastAll("§6§l♕ §e" + name + " §dgewinnt das FFA!");
            Webhook.send(this.plugin, "event", "🏆 " + name + " gewinnt das FFA-Event.");
            this.reward(winner, name, this.plugin.getConfig().getLong("eventgames.reward-winner", 5000L));
        }
        this.reset();
    }

    private void reward(UUID id, String name, long amount) {
        if (amount > 0L) {
            Economy.add(id, name, amount);
            Player p = this.plugin.getServer().getPlayer(id);
            if (p != null) {
                p.sendMessage("§a+ " + Gui.money(amount) + " §7Event-Belohnung");
                Chat.coins(p);
            }
        }
    }

    /* ---------- Turnier ---------- */

    private void nextRound() {
        List<UUID> field = new ArrayList<UUID>();
        for (UUID id : this.advancing) {
            if (this.joined.contains(id)) {
                field.add(id);
            }
        }
        this.advancing.clear();
        if (field.size() <= 1) {
            this.finishTournament(field.isEmpty() ? null : field.get(0));
            return;
        }
        List<Match> round = new ArrayList<Match>();
        List<String> lines = new ArrayList<String>();
        UUID bye = null;
        for (int i = 0; i + 1 < field.size(); i += 2) {
            UUID a = field.get(i);
            UUID b = field.get(i + 1);
            round.add(new Match(a, this.names.getOrDefault(a, "?"), b, this.names.getOrDefault(b, "?")));
        }
        if (field.size() % 2 == 1) {
            bye = field.get(field.size() - 1);
            this.advancing.add(bye);
        }
        this.rounds.add(round);
        int no = this.rounds.size();
        String title = field.size() == 2 ? "FINALE" : field.size() <= 4 ? "Halbfinale" : "Runde " + no;
        this.broadcastAll("§d✦ §f" + title + " §7– " + field.size() + " Spieler");
        for (Match m : round) {
            lines.add("§8  • §f" + m.na + " §7⚔ §f" + m.nb);
        }
        if (bye != null) {
            lines.add("§8  • §f" + this.names.getOrDefault(bye, "?") + " §7hat ein Freilos");
            Player p = this.plugin.getServer().getPlayer(bye);
            if (p != null) {
                p.sendMessage("§aFreilos! §7Du ziehst direkt in die nächste Runde ein.");
            }
        }
        for (String line : lines) {
            this.broadcastAll(line);
        }
        this.nextRoundAt = 0L;
        if (round.isEmpty()) {
            this.roundCheck();
        }
    }

    private void pump(long now) {
        if (this.rounds.isEmpty()) {
            return;
        }
        if (this.nextRoundAt > 0L) {
            if (now >= this.nextRoundAt) {
                this.nextRoundAt = 0L;
                this.nextRound();
            }
            return;
        }
        List<Match> round = this.rounds.get(this.rounds.size() - 1);
        for (Match m : round) {
            if (m.winner != null || m.running) {
                continue;
            }
            Player a = this.plugin.getServer().getPlayer(m.a);
            Player b = this.plugin.getServer().getPlayer(m.b);
            boolean aok = a != null && a.isOnline() && this.joined.contains(m.a);
            boolean bok = b != null && b.isOnline() && this.joined.contains(m.b);
            if (!aok && !bok) {
                this.settle(m, m.a, null, "beide Spieler fehlen");
                continue;
            }
            if (!aok || !bok) {
                this.settle(m, aok ? m.a : m.b, null, "Gegner fehlt");
                continue;
            }
            boolean ready = this.blocked(a) == null && this.blocked(b) == null && !DuelFixes.get().resultPhase.contains(m.a) && !DuelFixes.get().resultPhase.contains(m.b);
            if (!ready) {
                if (now - m.waitingSince > 180000L) {
                    boolean aFree = this.blocked(a) == null;
                    this.settle(m, aFree ? m.a : m.b, null, "Gegner war nicht bereit");
                }
                continue;
            }
            this.plugin.duelPartners.put(m.a, m.b);
            this.plugin.duelPartners.put(m.b, m.a);
            this.plugin.startDuelMatch(a, b, this.kit);
            if (this.plugin.activeDuelPlayers.contains(m.a)) {
                m.running = true;
                a.sendMessage("§d✦ Turnier-Match gegen §f" + m.nb + " §dgestartet!");
                b.sendMessage("§d✦ Turnier-Match gegen §f" + m.na + " §dgestartet!");
            } else {
                this.plugin.duelPartners.remove(m.a);
                this.plugin.duelPartners.remove(m.b);
                return; // keine freie Arena - naechster Versuch in einer Sekunde
            }
        }
        this.roundCheck();
    }

    /** Vom Duel-System: ein Duell hat einen Sieger. */
    static void duelResult(Player winner, Player loser) {
        if (instance != null && instance.running && !instance.ffa && winner != null && loser != null) {
            instance.onResult(winner.getUniqueId(), loser.getUniqueId());
        }
    }

    /** Vom Duel-System: Unentschieden - das Match wird wiederholt. */
    static void duelDraw(Player first, Player second) {
        if (instance == null || !instance.running || instance.ffa || first == null || second == null || instance.rounds.isEmpty()) {
            return;
        }
        for (Match m : instance.rounds.get(instance.rounds.size() - 1)) {
            if (m.running && m.winner == null && ((m.a.equals(first.getUniqueId()) && m.b.equals(second.getUniqueId())) || (m.b.equals(first.getUniqueId()) && m.a.equals(second.getUniqueId())))) {
                m.running = false;
                m.waitingSince = System.currentTimeMillis();
                first.sendMessage("§dUnentschieden – das Match wird gleich wiederholt.");
                second.sendMessage("§dUnentschieden – das Match wird gleich wiederholt.");
            }
        }
    }

    private void onResult(UUID winner, UUID loser) {
        if (this.rounds.isEmpty()) {
            return;
        }
        for (Match m : this.rounds.get(this.rounds.size() - 1)) {
            if (m.winner == null && m.running && ((m.a.equals(winner) && m.b.equals(loser)) || (m.b.equals(winner) && m.a.equals(loser)))) {
                this.settle(m, winner, loser, null);
                return;
            }
        }
    }

    private void forfeit(UUID id) {
        if (this.rounds.isEmpty()) {
            return;
        }
        for (Match m : this.rounds.get(this.rounds.size() - 1)) {
            if (m.winner == null && !m.running && (m.a.equals(id) || m.b.equals(id))) {
                this.settle(m, m.a.equals(id) ? m.b : m.a, id, "hat sich abgemeldet");
            }
        }
    }

    private void settle(Match m, UUID winner, UUID loser, String note) {
        if (m.winner != null) {
            return;
        }
        m.winner = winner;
        m.running = false;
        this.advancing.add(winner);
        String wn = this.names.getOrDefault(winner, "?");
        UUID l = loser != null ? loser : (m.a.equals(winner) ? m.b : m.a);
        String ln = this.names.getOrDefault(l, "?");
        this.broadcastAll("§d✦ §f" + wn + " §7schlägt §f" + ln + (note == null ? "" : " §8(" + note + ")"));
        Player lp = this.plugin.getServer().getPlayer(l);
        if (lp != null) {
            lp.sendMessage("§cDu bist im Turnier ausgeschieden. §7Danke fürs Mitmachen! Zuschauen: §d/events bracket");
        }
        this.joined.remove(l);
        this.roundCheck();
    }

    private void roundCheck() {
        if (this.rounds.isEmpty() || this.nextRoundAt > 0L) {
            return;
        }
        for (Match m : this.rounds.get(this.rounds.size() - 1)) {
            if (m.winner == null) {
                return;
            }
        }
        this.nextRoundAt = System.currentTimeMillis() + 12000L; // Ergebnis-Phase der Duelle abwarten
        if (this.advancing.size() > 1) {
            this.broadcastAll("§7Nächste Runde in wenigen Sekunden …");
        }
    }

    private void finishTournament(UUID champion) {
        if (champion == null) {
            this.broadcastAll("§d✦ Turnier beendet – kein Sieger.");
        } else {
            String name = this.names.getOrDefault(champion, "?");
            this.broadcastAll("§6§l♕ §e" + name + " §dgewinnt das Turnier!");
            Webhook.send(this.plugin, "event", "🏆 " + name + " gewinnt das Turnier.");
            for (Player p : this.plugin.getServer().getOnlinePlayers()) {
                p.sendTitle("§6§l" + name, "§dist Turniersieger!", 10, 80, 20);
            }
            this.reward(champion, name, this.plugin.getConfig().getLong("eventgames.reward-winner", 5000L));
            if (!this.rounds.isEmpty()) {
                for (Match m : this.rounds.get(this.rounds.size() - 1)) {
                    UUID second = m.a.equals(champion) ? m.b : m.a;
                    if (m.winner != null && m.winner.equals(champion)) {
                        this.reward(second, this.names.getOrDefault(second, "?"), this.plugin.getConfig().getLong("eventgames.reward-second", 1500L));
                    }
                }
            }
        }
        this.reset();
    }

    /* ---------- Turnierbaum ---------- */

    static void bracket(Player player) {
        if (instance == null || !instance.active()) {
            player.sendMessage("§7Zurzeit läuft kein Turnier.");
            return;
        }
        instance.showBracket(player);
    }

    private void showBracket(Player player) {
        if (this.joining) {
            player.sendMessage("§d✦ " + this.label() + " – Anmeldung (" + this.joined.size() + " Spieler):");
            StringBuilder sb = new StringBuilder("§7");
            for (UUID id : this.joined) {
                sb.append(this.names.getOrDefault(id, "?")).append("§8, §7");
            }
            player.sendMessage(sb.toString());
            return;
        }
        if (this.ffa) {
            player.sendMessage("§d✦ FFA läuft – Teilnehmer: §f" + this.names.size());
            return;
        }
        player.sendMessage("§8§m                    §r §d✦ Turnierbaum §8§m                    ");
        for (int r = 0; r < this.rounds.size(); r++) {
            List<Match> round = this.rounds.get(r);
            player.sendMessage("§7Runde " + (r + 1) + ":");
            for (Match m : round) {
                String a = (m.winner != null && m.winner.equals(m.a) ? "§a§l" : m.winner != null ? "§8§m" : "§f") + m.na;
                String b = (m.winner != null && m.winner.equals(m.b) ? "§a§l" : m.winner != null ? "§8§m" : "§f") + m.nb;
                player.sendMessage("§8  " + (m.winner != null ? "✔ " : m.running ? "⚔ " : "… ") + a + " §7vs " + b);
            }
        }
    }

    /* ---------- Hilfen ---------- */

    private void broadcastAll(String message) {
        for (Player p : this.plugin.getServer().getOnlinePlayers()) {
            p.sendMessage(message);
        }
    }

    /** Beim Herunterfahren: laufendes Event sauber beenden. */
    void shutdown() {
        try {
            if (this.running || this.joining) {
                this.stop("\u00a7cEvent wegen Server-Neustart beendet.");
            }
        }
        catch (Throwable ignored) {
            // egal
            Store.problem("EventGames:603", ignored);
        }
    }

    private void stop(String message) {
        if (this.ffaMatch != null) {
            try {
                this.plugin.finishPartyMatch(this.ffaMatch, null, "Event beendet.");
            }
            catch (Throwable ignored) {
                // egal
                Store.problem("EventGames:613", ignored);
            }
        }
        this.broadcastAll(message);
        this.reset();
    }

    private void reset() {
        if (this.task != -1) {
            this.plugin.getServer().getScheduler().cancelTask(this.task);
            this.task = -1;
        }
        this.joining = false;
        this.running = false;
        this.ffaMatch = null;
        this.joined.clear();
        this.names.clear();
        this.rounds.clear();
        this.advancing.clear();
        this.nextRoundAt = 0L;
    }

    /** Fuer die Event-Seite: gibt es gerade etwas zum Mitmachen? */
    static boolean joinable() {
        return instance != null && instance.joining;
    }

    static String describe() {
        return instance == null || !instance.active() ? "" : instance.label() + " (" + instance.joined.size() + ")";
    }

    /** FFA bekommt im Menü ein Diamantschwert, Turnier/1v1 ein Eisenschwert. */
    static boolean isFfa() {
        return instance != null && instance.active() && instance.ffa;
    }

    /** Fuer die Seitenleiste: Spieler ist fuer das aktuelle Event angemeldet? */
    static boolean participant(UUID id) {
        return instance != null && instance.active() && instance.joined.contains(id);
    }

    static int participants() {
        return instance == null ? 0 : instance.joined.size();
    }

    /** Ist der Spieler gerade in einem laufenden Turnier-Match? (Fuer andere Systeme.) */
    static boolean busy(UUID id) {
        return instance != null && instance.running && instance.joined.contains(id);
    }

    /** /events <join|leave|bracket> */
    static boolean command(Player player, String[] args) {
        if (args.length == 0) {
            return false;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "join", "mitmachen" -> join(player);
            case "leave", "abmelden" -> leave(player);
            case "bracket", "baum", "turnierbaum" -> bracket(player);
            default -> {
                return false;
            }
        }
        return true;
    }
}
