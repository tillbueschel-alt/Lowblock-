/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.scoreboard.Scoreboard
 *  org.bukkit.scoreboard.Team
 *  org.bukkit.scoreboard.Team$Option
 *  org.bukkit.scoreboard.Team$OptionStatus
 */
package de.server.network;

import de.server.network.MenuItems;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.GameMode;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class DuelFixes
implements Listener {
    private static final String PREFIX = "dnp_";
    private final DuelNetworkPlugin plugin;
    private final Map<UUID, String> activeParkourMaps;
    private final Set<UUID> activeDuelPlayers;
    private final Map<UUID, UUID> duelPartners;
    private final Map<UUID, String> activePartyMatches;
    private final Map<UUID, String> parkourEntries = new HashMap<UUID, String>();
    private final Map<UUID, String> profiles;
    private final Map<String, Set<UUID>> partyMatchPlayers;
    private final Set<UUID> pendingDuelLosers;
    private final Set<UUID> duelResultPlayers;
    private final Map<UUID, Location> duelDeathLocations;
    private final Set<UUID> partyBypass = new HashSet<UUID>();
    private final Map<UUID, Integer> duelDeathFinishTasks;
    private final Map<UUID, String> duelModes;
    private final Map<UUID, Location> parkourCheckpoints;
    final Set<UUID> resultPhase = ConcurrentHashMap.newKeySet();
    private final Map<UUID, UUID> rematchRequests = new HashMap<UUID, UUID>();
    private static DuelFixes instance;
    /** Kennzeichnet das Duel-Ende, zu dem die verzoegerten Aufgaben gehoeren (verhindert doppelte Teleports). */
    private final Map<UUID, Long> epochs = new ConcurrentHashMap<UUID, Long>();
    private long epochCounter;

    private boolean isEpoch(UUID id, long epoch) {
        Long current = id == null ? null : this.epochs.get(id);
        return current != null && current == epoch;
    }

    /** Wird von finishDuel aufgerufen: alle offenen Ergebnis-/Revanche-Zustaende dieses Spielers vergessen. */
    static void forget(UUID id) {
        DuelFixes fixes = instance;
        if (fixes == null || id == null) {
            return;
        }
        fixes.epochs.remove(id);
        fixes.resultPhase.remove(id);
        fixes.rematchRequests.remove(id);
        fixes.rematchRequests.values().removeIf(other -> other.equals(id));
    }

    /** Vorzeitig verlassen waehrend des Ergebnis-Menues (z.B. per /leave). @return true wenn behandelt */
    static boolean leaveResult(Player player) {
        DuelFixes fixes = instance;
        if (fixes == null || player == null || !fixes.resultPhase.contains(player.getUniqueId())) {
            return false;
        }
        fixes.duelLeave(player);
        return true;
    }

    static DuelFixes get() {
        return instance;
    }

    private DuelFixes(DuelNetworkPlugin javaPlugin) {
        this.plugin = javaPlugin;
        this.activeParkourMaps = javaPlugin.activeParkourMaps;
        this.activeDuelPlayers = javaPlugin.activeDuelPlayers;
        this.duelPartners = javaPlugin.duelPartners;
        this.activePartyMatches = javaPlugin.activePartyMatches;
        this.profiles = javaPlugin.profiles;
        this.partyMatchPlayers = javaPlugin.partyMatchPlayers;
        this.pendingDuelLosers = javaPlugin.pendingDuelLosers;
        this.duelResultPlayers = javaPlugin.duelResultPlayers;
        this.duelDeathLocations = javaPlugin.duelDeathLocations;
        this.duelDeathFinishTasks = javaPlugin.duelDeathFinishTasks;
        this.duelModes = javaPlugin.duelModes;
        this.parkourCheckpoints = javaPlugin.parkourCheckpoints;
    }

    public static void install(DuelNetworkPlugin javaPlugin) {
        try {
            DuelFixes duelFixes;
            instance = duelFixes = new DuelFixes(javaPlugin);
            javaPlugin.getServer().getPluginManager().registerEvents((Listener)duelFixes, (Plugin)javaPlugin);
            MenuItems menuItems = new MenuItems(javaPlugin, duelFixes);
            javaPlugin.getServer().getPluginManager().registerEvents((Listener)menuItems, (Plugin)javaPlugin);
            // Die Inventarprüfung scannt bis zu 36 Slots pro Spieler; ein Sekundentakt ist ausreichend.
            javaPlugin.getServer().getScheduler().runTaskTimer((Plugin)javaPlugin, Perf.wrap("MenuItems", menuItems::refreshAll), 20L, 20L);
            // Parkour-Teams müssen nicht fünfmal pro Sekunde neu berechnet werden.
            javaPlugin.getServer().getScheduler().runTaskTimer((Plugin)javaPlugin, Perf.wrap("ParkourTeams", duelFixes::tickParkourTeams), 20L, 10L);
        }
        catch (Throwable throwable) {
            System.err.println("[DuelNetwork-Fix] konnte nicht installiert werden: " + String.valueOf(throwable));
        }
    }

    public static Location lobbyCenter(JavaPlugin javaPlugin) {
        String[] stringArray;
        FileConfiguration fileConfiguration = javaPlugin.getConfig();
        for (String string : stringArray = new String[]{"locations.lobbies.haupt", "locations.spawns.haupt", "locations.spawns.haupt.default"}) {
            World world;
            if (!fileConfiguration.contains(string + ".world")) continue;
            String string2 = fileConfiguration.getString(string + ".world");
            World world2 = world = string2 == null ? null : Bukkit.getWorld((String)string2);
            if (world == null) continue;
            return new Location(world, fileConfiguration.getDouble(string + ".x"), fileConfiguration.getDouble(string + ".y"), fileConfiguration.getDouble(string + ".z"), (float)fileConfiguration.getDouble(string + ".yaw"), (float)fileConfiguration.getDouble(string + ".pitch"));
        }
        return null;
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent playerDeathEvent) {
        Player player = playerDeathEvent.getEntity();
        if (player == null) {
            return;
        }
        UUID uUID = player.getUniqueId();
        if (!this.activeDuelPlayers.contains(uUID) || this.activePartyMatches.containsKey(uUID)) {
            return;
        }
        UUID uUID2 = this.duelPartners.get(uUID);
        long epoch = ++this.epochCounter;
        this.epochs.put(uUID, epoch);
        if (uUID2 != null) {
            this.epochs.put(uUID2, epoch);
        }
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            Player target = this.plugin.getServer().getPlayer(uUID);
            if (target != null && target.isDead()) {
                DuelFixes.respawn(target);
            }
        }, 1L);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> this.beginResult(uUID, uUID2, epoch), 3L);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> this.forceFinish(uUID, uUID2, epoch), 160L);
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onRespawn(PlayerRespawnEvent playerRespawnEvent) {
        Player player;
        Player player2 = playerRespawnEvent.getPlayer();
        if (player2 == null) {
            return;
        }
        UUID uUID = player2.getUniqueId();
        if (!this.pendingDuelLosers.contains(uUID)) {
            return;
        }
        Location location = this.duelDeathLocations.get(uUID);
        if (location == null || location.getWorld() == null || location.getY() >= (double)(location.getWorld().getMinHeight() + 2)) {
            return;
        }
        UUID uUID2 = this.duelPartners.get(uUID);
        Player player3 = player = uUID2 == null ? null : this.plugin.getServer().getPlayer(uUID2);
        if (player != null) {
            this.duelDeathLocations.put(uUID, player.getLocation().clone());
        }
    }

    private void beginResult(UUID uUID, UUID uUID2, long epoch) {
        if (uUID2 == null || !this.isEpoch(uUID, epoch)) {
            return;
        }
        Player player = this.plugin.getServer().getPlayer(uUID);
        Player player2 = this.plugin.getServer().getPlayer(uUID2);
        if (player == null || player2 == null) {
            return;
        }
        if (!this.activeDuelPlayers.contains(uUID) || !this.activeDuelPlayers.contains(uUID2)) {
            return;
        }
        if (!this.duelResultPlayers.contains(uUID)) {
            return;
        }
        // Die Ergebnisphase wird allein von forceFinish beendet: den 60-Tick-Abschluss des Hauptplugins abbrechen
        for (UUID tid : new UUID[]{uUID, uUID2}) {
            Integer task = this.duelDeathFinishTasks.remove(tid);
            if (task != null) {
                this.plugin.getServer().getScheduler().cancelTask(task.intValue());
            }
        }
        if (player.isDead()) {
            DuelFixes.respawn(player);
        }
        DuelStats.record(player2, player);
        player.sendMessage("\u00a77R\u00fcckkehr zur Lobby in \u00a7e7 Sekunden\u00a77 \u2013 Revanche oder Verlassen \u00fcber die Items.");
        player2.sendMessage("\u00a77R\u00fcckkehr zur Lobby in \u00a7e7 Sekunden\u00a77 \u2013 Revanche oder Verlassen \u00fcber die Items.");
        for (Player player3 : new Player[]{player, player2}) {
            try {
                player3.getInventory().clear();
                player3.getInventory().setArmorContents(new ItemStack[4]);
                player3.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            }
            catch (Throwable throwable) {
                // empty catch block
                Store.problem("DuelFixes:246", throwable);
            }
            this.resultPhase.add(player3.getUniqueId());
        }
        MenuItems.refreshNow(player);
        MenuItems.refreshNow(player2);
    }

    private boolean rematchPending(UUID uUID, UUID uUID2) {
        return this.rematchRequests.containsKey(uUID) || uUID2 != null && this.rematchRequests.containsKey(uUID2);
    }

    void openByType(Player player, String string) {
        try {
            switch (string) {
                case "navigator": {
                    this.plugin.openNavigator(player);
                    break;
                }
                case "smp": {
                    this.plugin.switchProfile(player, "smp");
                    BauSmp.title(player, "smp");
                    if (this.plugin.teleportToLastSmpLocation(player)) break;
                    this.plugin.teleportToSpawn(player, "smp");
                    break;
                }
                case "duell": {
                    this.plugin.enterDuelLobby(player);
                    break;
                }
                case "events": {
                    this.plugin.enterEventLobby(player);
                    break;
                }
                case "parkour": {
                    this.plugin.openParkourMenu(player);
                    break;
                }
                case "creative": {
                    BauSmp.enter(this.plugin, player);
                    break;
                }
            }
        }
        catch (Throwable throwable) {
            System.err.println("[DuelNetwork-Fix] Aktion " + string + " fehlgeschlagen: " + String.valueOf(throwable));
        }
    }

    public static String parkourStatus(DuelNetworkPlugin javaPlugin, String string, Player player) {
        String string2;
        try {
            string2 = javaPlugin.parkourValidationMessage(string);
        }
        catch (Throwable throwable) {
            return "?";
        }
        if (player != null && player.hasPermission("duelnetwork.admin")) {
            return string2;
        }
        return string2.startsWith("OK") ? "OK" : "Noch nicht spielbar";
    }

    void action(Player player, String string) {
        UUID uUID = player.getUniqueId();
        if (string.startsWith("type:")) {
            this.openByType(player, string.substring(5));
        } else if (string.startsWith("cmd:")) {
            player.performCommand(string.substring(4));
        } else if (string.equals("duel_menu")) {
            this.plugin.openDuelKits(player, null);
        } else if (string.equals("parkour_cp")) {
            Location location = this.parkourCheckpoints.get(uUID);
            if (location != null) {
                player.teleport(location);
            }
        } else if (string.equals("duel_leave")) {
            this.duelLeave(player);
        } else if (string.equals("rematch")) {
            this.rematch(player);
        } else if (string.equals("requeue")) {
            this.requeue(player);
        }
    }

    /** "Nächster Gegner": Duel verlassen und mit demselben Kit sofort wieder in die Warteschlange (anderer Gegner, keine Revanche). */
    private void requeue(Player player) {
        UUID id = player.getUniqueId();
        if (!this.resultPhase.contains(id) || !this.activeDuelPlayers.contains(id)) {
            return;
        }
        String kit = this.duelModes.get(id);
        this.duelLeave(player);
        if (kit == null || kit.isBlank()) {
            return;
        }
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, () -> {
            if (player.isOnline()) {
                this.plugin.requeueDuel(player, kit);
            }
        }, 10L);
    }

    private void duelLeave(Player player) {
        UUID uUID = player.getUniqueId();
        if (!this.resultPhase.contains(uUID) || !this.activeDuelPlayers.contains(uUID)) {
            return;
        }
        UUID uUID2 = this.duelPartners.get(uUID);
        boolean bl = this.rematchPending(uUID, uUID2);
        this.resultPhase.remove(uUID);
        this.rematchRequests.remove(uUID);
        try {
            this.plugin.finishDuel(player);
        }
        catch (Throwable throwable) {
            this.activeDuelPlayers.remove(uUID);
        }
        if (bl && uUID2 != null) {
            this.rematchRequests.remove(uUID2);
            Player player2 = this.plugin.getServer().getPlayer(uUID2);
            if (player2 != null && this.activeDuelPlayers.contains(uUID2)) {
                player2.sendMessage("\u00a77Dein Gegner hat das Duel verlassen.");
                this.resultPhase.remove(uUID2);
                try {
                    this.plugin.finishDuel(player2);
                }
                catch (Throwable throwable) {
                    this.activeDuelPlayers.remove(uUID2);
                }
            }
        }
    }

    private void rematch(Player player) {
        Player player2;
        UUID uUID = player.getUniqueId();
        UUID uUID2 = this.duelPartners.get(uUID);
        Player player3 = player2 = uUID2 == null ? null : this.plugin.getServer().getPlayer(uUID2);
        if (!(uUID2 != null && player2 != null && this.resultPhase.contains(uUID) && this.resultPhase.contains(uUID2) && this.activeDuelPlayers.contains(uUID) && this.activeDuelPlayers.contains(uUID2))) {
            player.sendMessage("\u00a7cEine Revanche ist nicht mehr moeglich.");
            return;
        }
        if (uUID.equals(this.rematchRequests.get(uUID2))) {
            this.startRematch(player, player2);
            return;
        }
        if (this.rematchRequests.containsKey(uUID)) {
            player.sendMessage("\u00a77Du hast bereits eine Revanche angefragt.");
            return;
        }
        this.rematchRequests.put(uUID, uUID2);
        for (UUID uUID3 : new UUID[]{uUID, uUID2}) {
            Integer n = this.duelDeathFinishTasks.remove(uUID3);
            if (n == null) continue;
            this.plugin.getServer().getScheduler().cancelTask(n.intValue());
        }
        player.sendMessage("\u00a7dRevanche angefragt \u00a77\u2013 warte auf " + player2.getName() + " (15 Sekunden).");
        Chat.buttons(player2, "\u00a7d" + player.getName() + " m\u00f6chte eine Revanche!", new String[]{"\u00a7a\u00a7l[\u2694 Revanche annehmen]", "/duelnetwork:duel rematch", "\u00a77Noch eine Runde spielen"});
        Chat.ping(player2);
        Chat.click(player);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!uUID2.equals(this.rematchRequests.get(uUID))) {
                return;
            }
            this.rematchRequests.remove(uUID);
            this.endResult(uUID, uUID2, "\u00a77Keine Revanche \u2013 Rueckkehr zur Lobby.");
        }, 300L);
    }

    private void endResult(UUID uUID, UUID uUID2, String string) {
        for (UUID uUID3 : new UUID[]{uUID, uUID2}) {
            this.resultPhase.remove(uUID3);
            Player player = this.plugin.getServer().getPlayer(uUID3);
            if (player == null) {
                this.activeDuelPlayers.remove(uUID3);
                continue;
            }
            if (!this.activeDuelPlayers.contains(uUID3)) continue;
            if (string != null) {
                player.sendMessage(string);
            }
            try {
                this.plugin.finishDuel(player);
            }
            catch (Throwable throwable) {
                this.activeDuelPlayers.remove(uUID3);
            }
        }
    }

    private void startRematch(Player player, Player player2) {
        Chat.yes(player);
        Chat.yes(player2);
        UUID uUID = player.getUniqueId();
        UUID uUID2 = player2.getUniqueId();
        String string = this.duelModes.get(uUID);
        this.rematchRequests.remove(uUID);
        this.rematchRequests.remove(uUID2);
        if (string == null) {
            this.endResult(uUID, uUID2, "\u00a77Revanche nicht moeglich.");
            return;
        }
        for (UUID uUID3 : new UUID[]{uUID, uUID2}) {
            Integer n = this.duelDeathFinishTasks.remove(uUID3);
            if (n != null) {
                this.plugin.getServer().getScheduler().cancelTask(n.intValue());
            }
            this.resultPhase.remove(uUID3);
        }
        try {
            this.plugin.finishDuel(player);
            this.plugin.finishDuel(player2);
            this.duelPartners.put(uUID, uUID2);
            this.duelPartners.put(uUID2, uUID);
            this.plugin.prepareRematchSeries(player, player2);
            this.plugin.startDuelMatch(player, player2, string);
        }
        catch (Throwable throwable) {
            System.err.println("[DuelNetwork-Fix] Revanche fehlgeschlagen: " + String.valueOf(throwable));
            player.sendMessage("\u00a7cRevanche konnte nicht gestartet werden.");
            player2.sendMessage("\u00a7cRevanche konnte nicht gestartet werden.");
        }
    }

    private void forceFinish(UUID uUID, UUID uUID2, long epoch) {
        if (!this.isEpoch(uUID, epoch) && !this.isEpoch(uUID2, epoch)) {
            return; // das Duel wurde schon vorher verlassen/beendet (oder eine Revanche laeuft)
        }
        if (this.rematchPending(uUID, uUID2)) {
            return;
        }
        ArrayList<UUID> ids = new ArrayList<UUID>();
        ids.add(uUID);
        if (uUID2 != null) {
            ids.add(uUID2);
        }
        ArrayList<Player> finished = new ArrayList<Player>();
        for (UUID id : ids) {
            if (!this.isEpoch(id, epoch)) {
                continue;
            }
            Player player = this.plugin.getServer().getPlayer(id);
            if (player == null) {
                this.activeDuelPlayers.remove(id);
                this.epochs.remove(id);
                continue;
            }
            if (!this.activeDuelPlayers.contains(id)) {
                this.epochs.remove(id);
                continue;
            }
            try {
                this.plugin.finishDuel(player);
                finished.add(player);
            }
            catch (Throwable throwable) {
                this.activeDuelPlayers.remove(id);
            }
            this.epochs.remove(id);
        }
        // Nur wer hier wirklich beendet wurde, wird (falls noetig) zur Lobby gebracht - nicht noch einmal, wer schon weg ist.
        for (Player player : finished) {
            try {
                if (player.isDead()) {
                    DuelFixes.respawn(player);
                }
                if (this.plugin.recentlyReturned(player.getUniqueId())) {
                    continue;
                }
                Location location = DuelFixes.lobbyCenter(this.plugin);
                if (location != null) {
                    player.teleport(location);
                }
            }
            catch (Throwable throwable) {
                Store.problem("DuelFixes:500", throwable);
            }
        }
    }

    /** Notnagel: nur noetig, falls ein Spieler trotz abgebrochenem Tod tot ist. */
    private static void respawn(Player player) {
        try {
            player.spigot().respawn();
        }
        catch (Throwable throwable) {
            // nichts zu tun
            Store.problem("DuelFixes:509", throwable);
        }
    }

    /* ---------- Tod abbrechen statt Respawn-Bildschirm ---------- */

    /**
     * Auf Paper 1.21.11 bleibt ein Spieler nach dem Tod im Duel sonst auf dem Todesbildschirm haengen
     * (Player#respawn funktioniert dort nicht zuverlaessig). Darum wird der Tod abgebrochen: Der Spieler
     * bleibt am Leben, der restliche Ablauf (Ergebnis, Rematch, Lobby-Rueckkehr) laeuft unveraendert.
     * Party-Teilnehmer werden danach zu Zuschauern.
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onDeathCancel(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID id = player.getUniqueId();
        boolean party = this.activePartyMatches.containsKey(id);
        if (!party && !this.activeDuelPlayers.contains(id)) {
            return;
        }
        try {
            event.setCancelled(true);
            event.setReviveHealth(20.0);
        }
        catch (Throwable t) {
            return;
        }
        Location where = player.getLocation().clone();
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (player.getHealth() <= 0.0) {
                player.setHealth(20.0);
            }
            player.setFireTicks(0);
            if (where.getWorld() != null && where.getY() < where.getWorld().getMinHeight()) {
                UUID partner = this.duelPartners.get(id);
                Player other = partner == null ? null : this.plugin.getServer().getPlayer(partner);
                if (other != null) {
                    player.teleport(other.getLocation());
                }
            }
            if (party) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }, 2L);
    }

    /** Wer sein Duel schon verloren hat (oder im Ergebnis-Menue ist), nimmt keinen Schaden mehr. Void-Fall: zurueck zum Gegner. */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onDamageGuard(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID id = player.getUniqueId();
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
            if (!this.activeDuelPlayers.contains(id) || this.activePartyMatches.containsKey(id)) {
                return;
            }
            event.setCancelled(true);
            UUID partner = this.duelPartners.get(id);
            Player other = partner == null ? null : this.plugin.getServer().getPlayer(partner);
            if (other != null) {
                player.teleport(other.getLocation());
            }
            return;
        }
        if (!this.activeDuelPlayers.contains(id) && !this.activePartyMatches.containsKey(id)) {
            return; // ausserhalb eines Duells nie unverwundbar (haengengebliebene Markierungen)
        }
        if (this.pendingDuelLosers.contains(id) || this.duelResultPlayers.contains(id) || this.resultPhase.contains(id)) {
            event.setCancelled(true);
        }
    }

    /* ---------- /lobby verlaesst alles ---------- */

    /**
     * /lobby beendet vorher, woran der Spieler gerade teilnimmt: Parkour, Duel-Warteschlange,
     * Party-Match und Duel/Ergebnis-Menue.
     */
    @EventHandler(priority=EventPriority.LOWEST)
    public void onLobbyCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null) {
            return;
        }
        String label = message.trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
        if (label.startsWith("/")) {
            label = label.substring(1);
        }
        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }
        if (!label.equals("lobby")) {
            return;
        }
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        try {
            if (this.activeParkourMaps.containsKey(id)) {
                this.plugin.leaveParkour(player, true);
            }
            boolean queued = false;
            for (List<UUID> queue : this.plugin.duelQueues.values()) {
                queued |= queue.contains(id);
            }
            if (queued) {
                this.plugin.leaveDuelQueue(player);
            }
            String match = this.activePartyMatches.get(id);
            if (match != null) {
                this.plugin.finishPartyMatch(match, null, player.getName() + " hat das Party-Match verlassen.");
            }
            if (this.resultPhase.contains(id)) {
                this.duelLeave(player);
            } else if (this.activeDuelPlayers.contains(id)) {
                String mode = this.duelModes.get(id);
                if (mode != null && this.plugin.duelLeaveCheck(player, mode)) {
                    event.setCancelled(true); // Duel wird aufgegeben, danach bringt dich das Plugin in die Lobby
                }
            }
        }
        catch (Throwable t) {
            System.err.println("[DuelNetwork] /lobby konnte nicht alles verlassen: " + t);
        }
    }

    public static void partyDeath(DuelNetworkPlugin javaPlugin, Player player) {
        DuelFixes duelFixes = instance;
        if (duelFixes == null) {
            DuelFixes.callOriginalParty(javaPlugin, player);
            return;
        }
        duelFixes.handlePartyDeath(player);
    }

    private static void callOriginalParty(DuelNetworkPlugin javaPlugin, Player player) {
        try {
            javaPlugin.handlePartyDeath0(player);
        }
        catch (Throwable throwable) {
            System.err.println("[DuelNetwork-Fix] handlePartyDeath fehlgeschlagen: " + String.valueOf(throwable));
        }
    }

    private void handlePartyDeath(Player player) {
        Set<UUID> set;
        UUID uUID = player.getUniqueId();
        if (this.partyBypass.contains(uUID)) {
            DuelFixes.callOriginalParty(this.plugin, player);
            return;
        }
        String string = this.activePartyMatches.get(uUID);
        Set<UUID> set2 = set = string == null ? null : this.partyMatchPlayers.get(string);
        if (set == null) {
            DuelFixes.callOriginalParty(this.plugin, player);
            return;
        }
        HashSet<UUID> hashSet = new HashSet<UUID>(set);
        hashSet.remove(uUID);
        if (hashSet.size() > 1) {
            DuelFixes.callOriginalParty(this.plugin, player);
            return;
        }
        this.pendingDuelLosers.add(uUID);
        this.duelResultPlayers.add(uUID);
        this.duelResultPlayers.addAll(hashSet);
        this.duelDeathLocations.put(uUID, player.getLocation().clone());
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            Player target = this.plugin.getServer().getPlayer(uUID);
            if (target != null && target.isDead()) {
                DuelFixes.respawn(target);
            }
        }, 5L);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            Player target = this.plugin.getServer().getPlayer(uUID);
            this.partyBypass.add(uUID);
            try {
                if (target != null) {
                    DuelFixes.callOriginalParty(this.plugin, target);
                }
            }
            finally {
                this.partyBypass.remove(uUID);
                this.pendingDuelLosers.remove(uUID);
                this.duelResultPlayers.remove(uUID);
                this.duelResultPlayers.removeAll(hashSet);
                this.duelDeathLocations.remove(uUID);
            }
        }, 59L);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent playerJoinEvent) {
        Player player = playerJoinEvent.getPlayer();
        if (player == null) {
            return;
        }
        UUID uUID = player.getUniqueId();
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> this.returnToLobby(uUID), 2L);
    }

    private void returnToLobby(UUID uUID) {
        Player player = this.plugin.getServer().getPlayer(uUID);
        if (player == null || this.activeDuelPlayers.contains(uUID)) {
            return;
        }
        try {
            if (this.activeParkourMaps.containsKey(uUID)) {
                this.plugin.leaveParkour(player, false);
            }
            if ("smp".equals(this.profiles.get(uUID))) {
                this.plugin.switchProfile(player, "lobby");
            }
            if (DuelFixes.lobbyCenter(this.plugin) != null) {
                this.plugin.teleportToLobby(player);
            }
            player.setLevel(0);
            player.setExp(0.0f);
        }
        catch (Throwable throwable) {
            System.err.println("[DuelNetwork-Fix] Lobby-Rueckkehr beim Join fehlgeschlagen: " + String.valueOf(throwable));
        }
    }

    private List<Scoreboard> scoreboards() {
        ArrayList<Scoreboard> arrayList = new ArrayList<Scoreboard>();
        arrayList.add(Bukkit.getScoreboardManager().getMainScoreboard());
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = player.getScoreboard();
            if (arrayList.contains(scoreboard)) continue;
            arrayList.add(scoreboard);
        }
        return arrayList;
    }

    private void tickParkourTeams() {
        try {
            Player object;
            List<Scoreboard> list = this.scoreboards();
            HashSet<UUID> hashSet = new HashSet<UUID>();
            for (UUID uUID : new ArrayList<UUID>(this.activeParkourMaps.keySet())) {
                object = this.plugin.getServer().getPlayer(uUID);
                if (object == null) continue;
                hashSet.add(uUID);
                this.parkourEntries.put(uUID, object.getName());
                try {
                    object.setCollidable(false);
                }
                catch (Throwable throwable) {
                    // empty catch block
                    Store.problem("DuelFixes:759", throwable);
                }
                for (Scoreboard scoreboard : list) {
                    this.noCollision(scoreboard, object.getName());
                }
            }
            for (UUID uUID : new ArrayList<UUID>(this.parkourEntries.keySet())) {
                if (hashSet.contains(uUID)) continue;
                String entryName = this.parkourEntries.remove(uUID);
                this.restore(list, entryName);
                Player player = this.plugin.getServer().getPlayer(uUID);
                if (player == null) continue;
                try {
                    player.setCollidable(true);
                }
                catch (Throwable throwable) {
                    Store.problem("DuelFixes:775", throwable);
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
            Store.problem("DuelFixes:778", throwable);
        }
    }

    private void noCollision(Scoreboard scoreboard, String string) {
        try {
            String string2;
            Team team = scoreboard.getEntryTeam(string);
            String string3 = string2 = team == null ? "dn_default" : team.getName();
            if (string2.startsWith(PREFIX)) {
                return;
            }
            Team team2 = scoreboard.getTeam(PREFIX + string2);
            if (team2 == null) {
                team2 = scoreboard.registerNewTeam(PREFIX + string2);
                team2.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
                if (team != null) {
                    try {
                        team2.setPrefix(team.getPrefix());
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                        Store.problem("DuelFixes:799", throwable);
                    }
                    try {
                        team2.setColor(team.getColor());
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                        Store.problem("DuelFixes:805", throwable);
                    }
                }
            }
            team2.addEntry(string);
        }
        catch (Throwable throwable) {
            // empty catch block
            Store.problem("DuelFixes:812", throwable);
        }
    }

    private void restore(List<Scoreboard> list, String string) {
        for (Scoreboard scoreboard : list) {
            try {
                Team team = scoreboard.getEntryTeam(string);
                if (team == null || !team.getName().startsWith(PREFIX)) continue;
                String string2 = team.getName().substring(PREFIX.length());
                team.removeEntry(string);
                Team team2 = scoreboard.getTeam(string2);
                if (team2 == null) continue;
                team2.addEntry(string);
            }
            catch (Throwable throwable) {
                Store.problem("DuelFixes:828", throwable);
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent playerQuitEvent) {
        Player player = playerQuitEvent.getPlayer();
        if (player == null) {
            return;
        }
        UUID uUID = player.getUniqueId();
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            String string;
            if (!this.activeParkourMaps.containsKey(uUID) && (string = this.parkourEntries.remove(uUID)) != null) {
                this.restore(this.scoreboards(), string);
            }
        }, 2L);
    }
}
