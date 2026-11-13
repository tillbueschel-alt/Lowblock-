package de.server.network;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sidebar-Anzeige pro Spieler: Parkour (Rekord, Zeit, Rang), SMP (Geld, Rang, Online ...) und Lobby.
 * Jeder Spieler bekommt ein eigenes Scoreboard; die Rang-Teams werden danach fuer alle neu gesetzt.
 */
final class Sidebar implements Listener, CommandExecutor {
    private static final String OBJECTIVE = "lowblock";

    private final DuelNetworkPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<UUID, Scoreboard>();
    private final Map<UUID, Objective> objectives = new HashMap<UUID, Objective>();
    private final Map<UUID, List<String>> shown = new HashMap<UUID, List<String>>();
    private final Map<UUID, Boolean> shownFancy = new HashMap<UUID, Boolean>();
    private final Map<UUID, String> titles = new HashMap<UUID, String>();
    private final Set<UUID> disabled = new HashSet<UUID>();

    Sidebar(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        // Einmal pro Sekunde reicht für Geld/Ping/Online und halbiert die Sidebar-Arbeit gegenüber dem alten 10-Tick-Intervall.
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("Sidebar", () -> this.tick()), 40L, 20L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        UUID id = player.getUniqueId();
        if (this.disabled.remove(id)) {
            player.sendMessage("§7Sidebar: §aan");
        } else {
            this.disabled.add(id);
            this.hide(player);
            player.sendMessage("§7Sidebar: §caus §7(§f/sidebar§7 zum Einschalten)");
        }
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        this.boards.remove(id);
        this.objectives.remove(id);
        this.shown.remove(id);
        this.shownFancy.remove(id);
        this.titles.remove(id);
    }

    private void tick() {
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            try {
                this.update(player);
            }
            catch (Throwable t) {
                if (this.disabled.add(player.getUniqueId())) {
                    this.plugin.getLogger().warning("Sidebar fuer " + player.getName() + " abgeschaltet: " + t);
                }
            }
        }
    }

    private void hide(Player player) {
        Scoreboard board = this.boards.get(player.getUniqueId());
        if (board != null) {
            board.clearSlot(DisplaySlot.SIDEBAR);
            this.shown.remove(player.getUniqueId());
            this.shownFancy.remove(player.getUniqueId());
            this.titles.remove(player.getUniqueId());
        }
    }

    private Objective create(Scoreboard board) {
        Objective objective;
        try {
            objective = board.registerNewObjective(OBJECTIVE, "dummy", Component.text("LowBlock"));
        }
        catch (Throwable t) {
            try {
                Method m = Scoreboard.class.getMethod("registerNewObjective", String.class, String.class, String.class);
                objective = (Objective)m.invoke(board, OBJECTIVE, "dummy", "LowBlock");
            }
            catch (Throwable inner) {
                throw new IllegalStateException(inner);
            }
        }
        try {
            Object blank = blankFormat();
            if (blank != null) {
                for (Method m : Objective.class.getMethods()) {
                    if (m.getName().equals("numberFormat") && m.getParameterCount() == 1) {
                        m.invoke(objective, blank);
                    }
                }
            }
        }
        catch (Throwable t) {
            this.warnOnce("Objective-Zahlenformat: " + t);
        }
        return objective;
    }

    private static Object blank;

    private static Object blankFormat() throws Exception {
        if (blank == null) {
            Class<?> format = Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat");
            blank = format.getMethod("blank").invoke(null);
        }
        return blank;
    }

    private boolean warned;

    private void warnOnce(String text) {
        if (!this.warned) {
            this.warned = true;
            this.plugin.getLogger().warning("Sidebar: rote Zahlen koennen nicht ausgeblendet werden (" + text + ")");
        }
    }

    private static final String GEM = "\ue100";
    private static final String COIN = "\ue101";
    private static final String PANEL_LINE = "\ue200";
    private static final String PANEL_SHIFT = "\ue201";
    private static final String INSET = "\ue202";
    private static final String PANEL_BG = "\ue208";
    private static final String TITLE_LOGO = "\ue209";

    /** Erstes Zeichen der Kasten-Glyphen: BODY_FIRST + Zeilenanzahl (2-15), siehe pack-gen.py. */
    private static final char BODY_FIRST = '\ue210';

    /**
     * Setzt EINEN durchgehenden Kasten (Seiten + Unterkante) als Glyph in die erste Zeile; er ist so hoch wie alle Zeilen
     * zusammen und liegt hinter dem Text der folgenden Zeilen. Die letzte Zeile (PANEL_LINE) ist nur Platz fuer die Unterkante.
     * Alle Zeilen bekommen einen kleinen Einzug, damit der Text nicht am Rand klebt.
     */
    private List<String> frameLines(List<String> source) {
        int n = source.size();
        List<String> framed = new ArrayList<String>(n);
        boolean box = n >= 2 && n <= 15;
        for (int i = 0; i < n; i++) {
            String line = source.get(i);
            if (line.equals(PANEL_LINE)) {
                framed.add(line);
            } else if (i == 0 && box) {
                framed.add(String.valueOf((char)(BODY_FIRST + n)) + PANEL_SHIFT + INSET + line);
            } else {
                framed.add(INSET + line);
            }
        }
        return framed;
    }

    private static boolean isBodyGlyph(char c) {
        return c >= BODY_FIRST + 2 && c <= BODY_FIRST + 15;
    }

    private static boolean isSidebarGlyph(char c) {
        return c == PANEL_LINE.charAt(0) || isBodyGlyph(c) || c == PANEL_SHIFT.charAt(0) || c == INSET.charAt(0) || c == TITLE_LOGO.charAt(0);
    }

    /** Zeilen mit Gem-/Muenzen-Symbol: Anzeigename mit Pack-Schrift lowblock:icons (nur Spieler mit geladenem Pack). */
    private void glyphName(Object score, String entry) {
        boolean special = false;
        for (int i = 0; i < entry.length() && !special; i++) {
            char c = entry.charAt(i);
            special = c == GEM.charAt(0) || c == COIN.charAt(0) || isSidebarGlyph(c);
        }
        if (!special) {
            return;
        }
        try {
            Class<?> keyClass = Class.forName("net.kyori.adventure.key.Key");
            Object key = keyClass.getMethod("key", String.class, String.class).invoke(null, "lowblock", "icons");
            Object sidebarKey = keyClass.getMethod("key", String.class, String.class).invoke(null, "lowblock", "sidebar");
            Component result = Component.empty();
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < entry.length(); i++) {
                char c = entry.charAt(i);
                if (c == GEM.charAt(0) || c == COIN.charAt(0) || isSidebarGlyph(c)) {
                    if (text.length() > 0) {
                        result = result.append(LegacyComponentSerializer.legacySection().deserialize(text.toString()));
                        text.setLength(0);
                    }
                    Object font = isSidebarGlyph(c) ? sidebarKey : key;
                    Object glyph = Component.class.getMethod("font", keyClass).invoke(Component.text(String.valueOf(c)), font);
                    result = result.append((Component) glyph);
                } else {
                    text.append(c);
                }
            }
            if (text.length() > 0) {
                result = result.append(LegacyComponentSerializer.legacySection().deserialize(text.toString()));
            }
            for (Method m : org.bukkit.scoreboard.Score.class.getMethods()) {
                if (m.getName().equals("customName") && m.getParameterCount() == 1) {
                    m.invoke(score, result);
                    return;
                }
            }
        }
        catch (Throwable t) {
            this.warnOnce("Sidebar-Symbole: " + t);
        }
    }

    /** Blendet die rote Punktzahl rechts neben einer Zeile aus. */
    private void blankScore(Object score) {
        try {
            Object format = blankFormat();
            for (Method m : org.bukkit.scoreboard.Score.class.getMethods()) {
                if (m.getName().equals("numberFormat") && m.getParameterCount() == 1 && m.getParameterTypes()[0].isInstance(format)) {
                    m.invoke(score, format);
                    return;
                }
            }
        }
        catch (Throwable t) {
            this.warnOnce("Score-Zahlenformat: " + t);
        }
    }

    /** Pixel-Titel aus dem Texture-Pack (Schrift lowblock:title). Nur fuer Spieler mit geladenem Pack. */
    private boolean setFontTitle(Objective objective, String title) {
        // Das Logo steht als Titel UEBER dem Leaderboard, egal in welchem Bereich.
        String glyph = TITLE_LOGO;
        if (glyph == null) {
            return false;
        }
        try {
            Class<?> keyClass = Class.forName("net.kyori.adventure.key.Key");
            Object key = keyClass.getMethod("key", String.class, String.class).invoke(null, "lowblock", "sidebar");
            Object component = Component.text(glyph);
            component = Component.class.getMethod("font", keyClass).invoke(component, key);
            objective.getClass();
            for (Method m : Objective.class.getMethods()) {
                if (m.getName().equals("displayName") && m.getParameterCount() == 1) {
                    m.invoke(objective, component);
                    return true;
                }
            }
        }
        catch (Throwable ignored) {
            // dann normaler Titel
        }
        return false;
    }

    private void setTitle(Objective objective, String title) {
        try {
            objective.displayName(LegacyComponentSerializer.legacySection().deserialize(title));
        }
        catch (Throwable t) {
            try {
                objective.getClass().getMethod("setDisplayName", String.class).invoke(objective, title);
            }
            catch (Throwable ignored) {
                // Titel bleibt
                Store.problem("Sidebar:190", ignored);
            }
        }
    }

    private void update(Player player) {
        UUID id = player.getUniqueId();
        if (this.disabled.contains(id)) {
            return;
        }
        List<String> lines = new ArrayList<String>();
        String title = this.build(player, lines);
        Scoreboard board = this.boards.get(id);
        if (board == null) {
            if (title == null) {
                return;
            }
            board = org.bukkit.Bukkit.getScoreboardManager().getNewScoreboard();
            this.boards.put(id, board);
            this.objectives.put(id, this.create(board));
            player.setScoreboard(board);
            this.plugin.refreshAllRanks();
        }
        if (title == null) {
            this.hide(player);
            return;
        }
        Objective objective = this.objectives.get(id);
        boolean fancy = ItemSkins.LOADED.contains(id);
        String cacheKey = title + (fancy ? "#font" : "");
        if (!cacheKey.equals(this.titles.get(id))) {
            if (!(fancy && this.setFontTitle(objective, title))) {
                this.setTitle(objective, title.isBlank() ? "§5✦ §d§lLowBlock" : title);
            }
            this.titles.put(id, cacheKey);
        }
        if (!fancy) {
            // ohne Texture-Pack: Symbol durch den normalen Punkt ersetzen
            lines.removeIf(l -> l.equals(PANEL_LINE));
            lines.replaceAll(l -> l.replace(COIN, "\u00a76\u25cf").replace(GEM, "\u00a7d\u25c6"));
        }
        if (fancy) {
            lines = this.frameLines(lines);
        }
        List<String> old = this.shown.get(id);
        // Der Pack-Status kann sich nach dem ersten Sidebar-Tick ändern. Ohne
        // diesen Zustandscache blieb die alte Vanilla-Sidebar sichtbar und das
        // Custom-Leaderboard mit Rahmen/Logo wurde nie nachgerendert.
        Boolean oldFancy = this.shownFancy.get(id);
        if (old != null && old.equals(lines) && oldFancy != null && oldFancy.booleanValue() == fancy
                && board.getObjective(DisplaySlot.SIDEBAR) != null) {
            return;
        }
        // eindeutige Eintraege
        List<String> entries = new ArrayList<String>();
        for (String line : lines) {
            String entry = line;
            while (entries.contains(entry)) {
                entry = entry + "§r";
            }
            entries.add(entry);
        }
        for (int i = 0; i < entries.size(); i++) {
            org.bukkit.scoreboard.Score score = objective.getScore(entries.get(i));
            score.setScore(entries.size() - i);
            this.blankScore(score);
            if (fancy) {
                this.glyphName(score, entries.get(i));
            }
        }
        if (old != null) {
            List<String> oldEntries = this.entriesOf(old);
            for (String entry : oldEntries) {
                if (!entries.contains(entry)) {
                    board.resetScores(entry);
                }
            }
        }
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        this.shown.put(id, new ArrayList<String>(lines));
        this.shownFancy.put(id, fancy);
    }

    /** Party-Zeile ("Party: x/y online") nur fuer Spieler in einer Party. */
    private String partyLine(UUID id, String dot) {
        String party = this.plugin.partyOf(id);
        if (party == null) {
            return null;
        }
        int online = this.plugin.partyOnline(party).size();
        return dot + "§7Party: §d" + online + " §8online";
    }

    private List<String> entriesOf(List<String> lines) {
        List<String> entries = new ArrayList<String>();
        for (String line : lines) {
            String entry = line;
            while (entries.contains(entry)) {
                entry = entry + "§r";
            }
            entries.add(entry);
        }
        return entries;
    }

    /** Baut die Zeilen (oben nach unten). @return Titel oder null, wenn keine Sidebar gezeigt werden soll. */
    private String build(Player player, List<String> lines) {
        UUID id = player.getUniqueId();
        if (this.plugin.activeDuelPlayers.contains(id) || this.plugin.activePartyMatches.containsKey(id)) {
            return null;
        }
        String foot = "§5✦ §dLowBlock";
        String dot = "§8▪ ";
        // Das Logo bleibt unabhängig vom aktuellen Modus immer oben sichtbar.
        String parkourMap = this.plugin.activeParkourMaps.get(id);
        if (parkourMap != null) {
            RecordStore records = this.plugin.records;
            long best = records == null ? Long.MAX_VALUE : records.best(parkourMap, id);
            List<RecordStore.Entry> top = records == null ? new ArrayList<RecordStore.Entry>() : records.top(parkourMap);
            Long start = this.plugin.parkourStart(id);
            long elapsed = start == null ? 0L : Math.max(0L, System.currentTimeMillis() - start);
            lines.add("§r");
            lines.add(dot + "§7Map: §d" + this.plugin.parkourName(parkourMap));
            lines.add("§r§r");
            lines.add(dot + "§7Rekord: §e" + (best == Long.MAX_VALUE ? "–" : ParkourEnhancements.formatTime(best)));
            lines.add(dot + "§7Zeit: §a" + ParkourEnhancements.formatTime(elapsed));
            int rank = -1;
            for (int i = 0; i < top.size(); i++) {
                if (top.get(i).id().equals(id.toString())) {
                    rank = i + 1;
                }
            }
            int live = 1;
            for (RecordStore.Entry entry : top) {
                if (!entry.id().equals(id.toString()) && entry.timeMs() < elapsed) {
                    live++;
                }
            }
            lines.add(dot + "§7Rang: §d" + (rank < 0 ? "–" : "#" + rank) + " §8/ " + top.size());
            lines.add(dot + "§7Lauf-Rang: §b#" + live);
            lines.add("§8" + GEM + " §7Gems: §d" + Gems.balance(id));
            lines.add("§r§r");
            lines.add(PANEL_LINE);
            lines.add(foot);
            return " ";
        }
        String profile = this.plugin.profiles.getOrDefault(id, "lobby");
        int online = this.plugin.getServer().getOnlinePlayers().size();
        if (EventGames.participant(id)) {
            lines.add("§r");
            lines.add(dot + "§7Event: §6" + EventGames.describe());
            lines.add(dot + "§7Teilnehmer: §f" + EventGames.participants());
            lines.add("§8" + GEM + " §7Gems: §d" + Gems.balance(id));
            lines.add("§r§r");
            lines.add(dot + "§7Bracket: §d/events bracket");
            lines.add(dot + "§7Verlassen: §d/events leave");
            lines.add("§r§r");
            lines.add(PANEL_LINE);
            lines.add(foot);
            return " ";
        }
        if (profile.equals("smp") && this.plugin.inBauMode(id)) {
            lines.add("§r");
            lines.add(dot + "§7Modus: §eFrei bauen");
            lines.add(dot + "§7PvP: §aaus §8• §7Inventar bleibt");
            lines.add("§r§r");
            lines.add("§8" + COIN + " §7Geld: §a" + Gui.shortMoney(Economy.balance(id)));
            lines.add("§8" + GEM + " §7Gems: §d" + Gems.balance(id));
            lines.add("§r");
            lines.add(dot + "§7Online: §f" + online);
            lines.add(dot + "§7Ping: §a" + player.getPing() + "ms");
            lines.add("§r§r");
            lines.add(PANEL_LINE);
            lines.add(foot);
            return " ";
        }
        if (profile.equals("smp")) {
            lines.add("§r");
            lines.add("§8" + COIN + " §7Geld: §a" + Gui.shortMoney(Economy.balance(id)));
            lines.add("§8" + GEM + " §7Gems: §d" + Gems.balance(id));
            lines.add(dot + "§7Rang: §d#" + Economy.rank(id) + " §8(Geld)");
            lines.add(dot + "§7Kills: §a" + SmpTop.killsOf(id) + " §8/ §7Tode: §c" + SmpTop.deathsOf(id));
            lines.add(dot + "§7Spielzeit: §b" + SmpTop.time(SmpTop.minutesOf(id)));
            lines.add("§r§r");
            String partySmp = this.partyLine(id, dot);
            if (partySmp != null) {
                lines.add(partySmp);
            }
            lines.add(dot + "§7Online: §f" + online);
            lines.add(dot + "§7Ping: §a" + player.getPing() + "ms");
            lines.add("§r§r");
            lines.add(PANEL_LINE);
            lines.add(foot);
            return " ";
        }
        // Lobby und alle anderen Bereiche: bewusst ohne SMP-Geld
        lines.add("§r");
        lines.add(dot + "§7Name: §f" + player.getName());
        lines.add(dot + "§7Rang: " + this.plugin.rankLabel(id));
        lines.add("§8" + GEM + " §7Gems: §d" + Gems.balance(id));
        lines.add("§r§r");
        String partyLobby = this.partyLine(id, dot);
        if (partyLobby != null) {
            lines.add(partyLobby);
        }
        lines.add(dot + "§7Online: §f" + online);
        lines.add("§r§r");
        lines.add(PANEL_LINE);
        lines.add(foot);
        return " ";
    }
}
