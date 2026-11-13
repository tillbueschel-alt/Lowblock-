package de.server.network;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Anti-Cheat, Anti-Multi-Account und Schutz vor Statistik-/Rekord-Betrug.
 *
 * Grundsatz: Das ist ein serverseitiger Schutz ohne Paket-Zugriff. Er faengt die haeufigsten Cheats
 * (Speed, Fly, Reach, Killaura, Autoclicker, Spam, Bots, Zweitaccounts, Win-Trading) ab, kann aber
 * keinen dedizierten Anti-Cheat wie Grim/Vulcan ersetzen. Alles ist in config.yml unter "anticheat" einstellbar.
 * Standardmaessig wird zurueckgesetzt und gekickt (nicht gebannt), damit keine Unschuldigen gesperrt werden.
 */
final class AntiCheat implements Listener, CommandExecutor, TabCompleter {
    private static AntiCheat instance;

    private final JavaPlugin plugin;
    private final File accountsFile;
    private final YamlConfiguration accounts;
    private final File logFile;
    private final Map<UUID, State> states = new ConcurrentHashMap<UUID, State>();
    private final Map<String, List<Long>> joinTimes = new ConcurrentHashMap<String, List<Long>>();
    private final Map<String, List<Long>> pairWins = new ConcurrentHashMap<String, List<Long>>();
    private final Map<String, Long> pairAlerted = new ConcurrentHashMap<String, Long>();
    private final Map<UUID, String> ipHashes = new ConcurrentHashMap<UUID, String>();
    private final java.util.Set<UUID> alertsOff = ConcurrentHashMap.newKeySet();
    private final String salt;

    private static final String PREFIX = "§4§l[AC] §r";

    private static final class State {
        Location lastSafe;
        long exemptUntil;
        long winStart;
        double winDist;
        int hover;
        double lastGroundY;
        double vy;
        int airMoves;
        int devCount;
        int groundMoves;
        boolean launched;
        long launchedAt;
        final List<Long> launches = new ArrayList<Long>();
        long lastCpsFlag;
        long lastHitFlag;
        long lastChat;
        long chatWindowStart;
        int chatCount;
        String lastMessage = "";
        long lastMessageAt;
        long cmdWindowStart;
        int cmdCount;
        final List<Long> clicks = new ArrayList<Long>();
        final List<Long> hits = new ArrayList<Long>();
        final Map<UUID, Long> recentVictims = new HashMap<UUID, Long>();
        final Map<String, Double> vl = new ConcurrentHashMap<String, Double>();
        final Map<String, Long> lastAlert = new HashMap<String, Long>();
        final Map<String, Double> pendCount = new ConcurrentHashMap<String, Double>();
        final Map<String, Double> pendMax = new ConcurrentHashMap<String, Double>();
        final java.util.ArrayDeque<String> trail = new java.util.ArrayDeque<String>();
        long trailAt;
        long lastReplay;
        long timerStart;
        int timerCount;
        int timerBad;
        int waterWalk;
        int noFall;
        final List<Long> places = new ArrayList<Long>();
        final List<Long> breaks = new ArrayList<Long>();
        final List<Long> clickTimes = new ArrayList<Long>();
        long lastPlaceFlag;
        long lastBreakFlag;
        long kbUntil;
        long windUntil;
        double kbExpected;
        double kbDist;
        int kbToken;
        int scaffold;
        long scaffoldStart;
        String effects = "";
        long effectsAt;
    }

    AntiCheat(JavaPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.accountsFile = new File(plugin.getDataFolder(), "accounts.yml");
        this.accounts = YamlConfiguration.loadConfiguration(this.accountsFile);
        this.logFile = new File(plugin.getDataFolder(), "anticheat.log");
        String stored = this.accounts.getString("salt", "");
        if (stored.isEmpty()) {
            stored = UUID.randomUUID().toString();
            this.accounts.set("salt", stored);
            this.saveAccounts();
        }
        this.salt = stored;
        // Verstoesse klingen langsam ab (ein Punkt pro 10 Sekunden)
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, () -> {
            long cutoff = System.currentTimeMillis();
            this.joinTimes.values().removeIf(list -> {
                synchronized (list) {
                    list.removeIf(time -> cutoff - time > 20000L);
                    return list.isEmpty();
                }
            });
            for (State state : this.states.values()) {
                for (Map.Entry<String, Double> entry : state.vl.entrySet()) {
                    double next = entry.getValue() - 1.0;
                    if (next <= 0.0) {
                        state.vl.remove(entry.getKey());
                    } else {
                        state.vl.put(entry.getKey(), next);
                    }
                }
            }
        }, 200L, 200L);
        // Sammelmeldung: alle 30 s eine Zusammenfassung der unterdrueckten Wiederholungen
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("AntiCheat-Summary", this::flushSummaries), 600L, 600L);
    }

    /** Fasst unterdrueckte Wiederholungs-Warnungen je Spieler zu einer Zeile zusammen. */
    private void flushSummaries() {
        for (Map.Entry<UUID, State> entry : this.states.entrySet()) {
            State state = entry.getValue();
            if (state.pendCount.isEmpty()) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Double> c : state.pendCount.entrySet()) {
                if (sb.length() > 0) {
                    sb.append("§8, ");
                }
                sb.append("§c").append(c.getKey()).append(" §7×").append(c.getValue().intValue())
                        .append(" §8[max VL ").append(String.format(Locale.ROOT, "%.1f", state.pendMax.getOrDefault(c.getKey(), 0.0))).append("]");
            }
            state.pendCount.clear();
            state.pendMax.clear();
            String name = this.plugin.getServer().getOfflinePlayer(entry.getKey()).getName();
            if (name == null) {
                name = String.valueOf(entry.getKey());
            }
            this.alert(PREFIX + "§7Zusammenfassung §f" + name + "§7: " + sb, name);
        }
    }

    /** Merkt sich Position und Zustand (max. 2x pro Sekunde) fuer das Replay-Protokoll. */
    private void trail(Player player, State state, long now, String tag) {
        if (tag.equals("move") && now - state.trailAt < 500L) {
            return;
        }
        state.trailAt = now;
        Location l = player.getLocation();
        String line = java.time.LocalTime.now().withNano(0) + " " + tag + " " + (l.getWorld() == null ? "?" : l.getWorld().getName())
                + String.format(Locale.ROOT, " %.2f %.2f %.2f", l.getX(), l.getY(), l.getZ())
                + " ground=" + player.isOnGround() + " ping=" + player.getPing() + " gm=" + player.getGameMode()
                + (player.isGliding() ? " glide" : "") + (player.isFlying() ? " flying" : "");
        synchronized (state.trail) {
            state.trail.addLast(line);
            while (state.trail.size() > 60) {
                state.trail.removeFirst();
            }
        }
    }

    /** Schreibt die letzten Bewegungen/Aktionen vor einem Verstoss in anticheat-replays/. */
    private void dumpReplay(Player player, State state, String check, double vl, boolean force) {
        long now = System.currentTimeMillis();
        if (!this.flag("replay", true) || (!force && now - state.lastReplay < 30000L)) {
            return;
        }
        state.lastReplay = now;
        this.trail(player, state, now, "VIOLATION " + check);
        final List<String> lines;
        synchronized (state.trail) {
            lines = new ArrayList<String>(state.trail);
        }
        final String name = player.getName();
        final String header = "# " + name + " (" + player.getUniqueId() + ") " + check + " VL " + String.format(Locale.ROOT, "%.1f", vl);
        this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try {
                File dir = new File(this.plugin.getDataFolder(), "anticheat-replays");
                dir.mkdirs();
                String stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new java.util.Date());
                File file = new File(dir, name + "-" + stamp + "-" + check.replaceAll("[^A-Za-z0-9]", "") + ".log");
                List<String> out = new ArrayList<String>();
                out.add(header);
                out.addAll(lines);
                java.nio.file.Files.write(file.toPath(), out, StandardCharsets.UTF_8);
                File[] all = dir.listFiles((d, n) -> n.endsWith(".log"));
                int keep = Math.max(10, this.plugin.getConfig().getInt("anticheat.replay-keep", 100));
                if (all != null && all.length > keep) {
                    java.util.Arrays.sort(all, java.util.Comparator.comparingLong(File::lastModified));
                    for (int i = 0; i < all.length - keep; i++) {
                        all[i].delete();
                    }
                }
            }
            catch (IOException | RuntimeException ex) {
                Store.problem("AntiCheat.replay", ex);
            }
        });
    }

    /* ---------- Konfiguration ---------- */

    // Die Werte werden kurz zwischengespeichert: die Bewegungs-Pruefung laeuft bei jedem Paket,
    // und ein Config-Lookup mit String-Verkettung ist dafuer zu teuer. Aenderungen gelten nach hoechstens 5 s.
    private final Map<String, Double> numCache = new ConcurrentHashMap<String, Double>();
    private final Map<String, Integer> intCache = new ConcurrentHashMap<String, Integer>();
    private final Map<String, Boolean> flagCache = new ConcurrentHashMap<String, Boolean>();
    private volatile long cacheStamp;

    private void refreshCache() {
        long now = System.currentTimeMillis();
        if (now - this.cacheStamp > 5000L) {
            this.cacheStamp = now;
            this.numCache.clear();
            this.intCache.clear();
            this.flagCache.clear();
        }
    }

    private boolean enabled() {
        return this.flag("enabled", true);
    }

    private double num(String path, double fallback) {
        this.refreshCache();
        Double cached = this.numCache.get(path);
        if (cached == null) {
            cached = this.plugin.getConfig().getDouble("anticheat." + path, fallback);
            this.numCache.put(path, cached);
        }
        return cached;
    }

    private int integer(String path, int fallback) {
        this.refreshCache();
        Integer cached = this.intCache.get(path);
        if (cached == null) {
            cached = this.plugin.getConfig().getInt("anticheat." + path, fallback);
            this.intCache.put(path, cached);
        }
        return cached;
    }

    private boolean flag(String path, boolean fallback) {
        this.refreshCache();
        Boolean cached = this.flagCache.get(path);
        if (cached == null) {
            cached = this.plugin.getConfig().getBoolean("anticheat." + path, fallback);
            this.flagCache.put(path, cached);
        }
        return cached;
    }

    private State state(Player player) {
        return this.states.computeIfAbsent(player.getUniqueId(), id -> new State());
    }

    /** Bei Server-Lag oder sehr hohem Ping sind Bewegungsdaten unzuverlaessig - dann nicht anklagen. */
    private long tpsStamp;
    private boolean tpsLow;

    private boolean laggy(Player player) {
        try {
            long now = System.currentTimeMillis();
            if (now - this.tpsStamp > 1000L) {
                // getTPS() erzeugt jedes Mal ein neues Array - einmal pro Sekunde reicht
                this.tpsStamp = now;
                double[] tps = this.plugin.getServer().getTPS();
                this.tpsLow = tps != null && tps.length > 0 && tps[0] < this.num("min-tps", 18.0);
            }
            if (this.tpsLow) {
                return true;
            }
            return player.getPing() > this.integer("max-ping", 400);
        }
        catch (Throwable t) {
            return false;
        }
    }

    private boolean bypass(Player player) {
        return !this.enabled() || player.hasPermission("duelnetwork.anticheat.bypass");
    }

    /* ---------- Verstoesse ---------- */

    private void violation(Player player, String check, double weight, String detail, boolean setback) {
        State state = this.state(player);
        double total = state.vl.merge(check, weight, Double::sum);
        long now = System.currentTimeMillis();
        Long last = state.lastAlert.get(check);
        boolean summary = this.flag("alert-summary", true);
        long window = summary ? Math.max(5L, (long)this.num("alert-window", 60.0)) * 1000L : 3000L;
        if (last != null && now - last <= window && summary) {
            state.pendCount.merge(check, 1.0, Double::sum);
            state.pendMax.merge(check, total, Math::max);
        }
        if (last == null || now - last > window) {
            state.lastAlert.put(check, now);
            this.dumpReplay(player, state, check, total, false);
            String line = player.getName() + " " + check + " VL " + String.format(Locale.ROOT, "%.1f", total) + " (" + detail + ")";
            this.log(line);
            this.alert(PREFIX + "§f" + player.getName() + " §7→ §c" + check + " §8[VL " + String.format(Locale.ROOT, "%.1f", total) + "] §7" + detail, player.getName());
        }
        boolean airborneBoost = state.launched || now < state.windUntil;
        if (setback && !airborneBoost && state.lastSafe != null && state.lastSafe.getWorld() != null) {
            state.exemptUntil = now + 400L;
            player.teleport(state.lastSafe);
        }
        double kickAt = this.num("punish-vl", 40.0);
        if (kickAt > 0.0 && total >= kickAt) {
            state.vl.clear();
            this.dumpReplay(player, state, check, total, true);
            String mode = this.plugin.getConfig().getString("anticheat.punishment", "kick");
            this.log("PUNISH " + player.getName() + " " + check + " -> " + mode);
            Audit.log("ANTICHEAT", "PUNISH " + player.getName() + " (" + player.getUniqueId() + ") " + check + " VL " + String.format(Locale.ROOT, "%.1f", total) + " -> " + mode);
            if (mode.equalsIgnoreCase("ban")) {
                // frueher: Konsolenbefehl "ban" - den lehnt das Plugin fuer Nicht-Spieler ab, der Bann kam nie an
                long banMs = Moderation.parseDuration(this.plugin.getConfig().getString("anticheat.ban-duration", ""));
                if (Moderation.instance != null) {
                    Moderation.instance.banPlayer((org.bukkit.OfflinePlayer)player, player.getName(), Math.max(0L, banMs), "Anti-Cheat: " + check, "Anti-Cheat");
                } else {
                    player.kickPlayer("§c§lUnfaire Vorteile erkannt\n§7Anti-Cheat: " + check);
                }
            } else if (!mode.equalsIgnoreCase("none")) {
                player.kickPlayer("§c§lUnfaire Vorteile erkannt\n§7Anti-Cheat: " + check);
            }
        }
    }

    private void alert(String message) {
        this.alert(message, null);
    }

    /** Warnung an alle Admins; mit Spielername gibt es klickbare [TP]- und [Info]-Buttons. */
    private void alert(String message, String subject) {
        for (Player online : this.plugin.getServer().getOnlinePlayers()) {
            if (online.hasPermission("duelnetwork.admin") && !this.alertsOff.contains(online.getUniqueId())) {
                if (subject != null) {
                    Chat.buttons(online, message, new String[]{"§e§l[TP]", "/tp " + subject, "§7Zu " + subject + " teleportieren"},
                            new String[]{"§b§l[Info]", "/duelnetwork:anticheat info " + subject, "§7Verstöße anzeigen"});
                } else {
                    online.sendMessage(message);
                }
                try {
                    online.sendActionBar(net.kyori.adventure.text.Component.text(message));
                }
                catch (Throwable ignored) {
                    // nur Anzeige
                }
            }
        }
        this.plugin.getServer().getConsoleSender().sendMessage(message);
        Webhook.send(this.plugin, "anticheat", "⚠ " + message);
    }

    private synchronized void log(String line) {
        try (FileWriter writer = new FileWriter(this.logFile, StandardCharsets.UTF_8, true)) {
            writer.write(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) + " " + line + "\n");
        }
        catch (IOException ignored) {
            Store.problem("AntiCheat:300", ignored);
        }
    }

    double totalVl(UUID id) {
        State state = this.states.get(id);
        double sum = 0.0;
        if (state != null) {
            for (double value : state.vl.values()) {
                sum += value;
            }
        }
        return sum;
    }

    /* ---------- Bewegung: Speed / Fly ---------- */

    private static boolean passable(Material type) {
        return type.isAir();
    }

    private static final java.util.Set<Material> SPECIAL_MEDIUM = java.util.EnumSet.noneOf(Material.class);
    private static final java.util.Set<Material> ICY = java.util.EnumSet.noneOf(Material.class);

    static {
        // einmalig aufbauen statt bei jeder Bewegung Materialnamen zu vergleichen
        for (Material material : Material.values()) {
            String name = material.name();
            if (name.contains("WATER") || name.contains("LAVA") || name.contains("LADDER") || name.contains("VINE") || name.contains("SCAFFOLD")
                    || name.contains("COBWEB") || name.contains("BUBBLE") || name.contains("POWDER_SNOW") || name.contains("HONEY") || name.contains("SLIME")
                    || name.contains("BED") || name.contains("SEAGRASS") || name.contains("KELP") || name.contains("TWISTING") || name.contains("WEEPING") || name.contains("CAVE_VINES")) {
                SPECIAL_MEDIUM.add(material);
            }
            if (name.contains("ICE") || name.contains("SOUL")) {
                ICY.add(material);
            }
        }
    }

    private static int floor(double value) {
        return (int)Math.floor(value);
    }

    /** Hat der Spieler irgendwo unter den Fuessen einen festen Block? (gleiche Pruefpunkte wie zuvor, aber ohne Objekt-Erzeugung) */
    private boolean groundNear(Location at) {
        org.bukkit.World world = at.getWorld();
        if (world == null) {
            return false;
        }
        int x0 = floor(at.getX() - 0.3);
        int x1 = floor(at.getX() + 0.3);
        int z0 = floor(at.getZ() - 0.3);
        int z1 = floor(at.getZ() + 0.3);
        int y0 = floor(at.getY() - 0.65);
        int y1 = floor(at.getY());
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    if (!passable(world.getBlockAt(x, y, z).getType())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean specialMedium(Location at) {
        org.bukkit.World world = at.getWorld();
        if (world == null) {
            return false;
        }
        int x = at.getBlockX();
        int z = at.getBlockZ();
        return SPECIAL_MEDIUM.contains(world.getBlockAt(x, at.getBlockY(), z).getType())
                || SPECIAL_MEDIUM.contains(world.getBlockAt(x, floor(at.getY() + 1.0), z).getType())
                || SPECIAL_MEDIUM.contains(world.getBlockAt(x, floor(at.getY() - 0.5), z).getType());
    }

    private boolean icy(Location at) {
        org.bukkit.World world = at.getWorld();
        return world != null && ICY.contains(world.getBlockAt(at.getBlockX(), floor(at.getY() - 0.6), at.getBlockZ()).getType());
    }

    private boolean exempt(Player player, State state, long now) {
        if (player.isGliding() || player.isRiptiding()) {
            // Elytra/Riptide: waehrend des Flugs und noch einige Sekunden danach nicht pruefen
            state.exemptUntil = Math.max(state.exemptUntil, now + 4000L);
            state.launched = true;
            state.launchedAt = now;
        }
        if (now < state.exemptUntil) {
            return true;
        }
        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR || player.getAllowFlight() || player.isFlying() || player.isInsideVehicle() || player.isDead()) {
            return true;
        }
        // nach einem Start (Windcharge, Knockback, Explosion ...) bis zur Landung nicht pruefen (maximal 12 s)
        return state.launched && now - state.launchedAt < 12000L;
    }

    /** Traenke aendern sich selten - hoechstens alle 500 ms neu einlesen statt bei jeder Bewegung. */
    private String effects(Player player, State state, long now) {
        if (now - state.effectsAt > 500L) {
            state.effectsAt = now;
            java.util.Collection<?> active = player.getActivePotionEffects();
            if (active.isEmpty()) {
                state.effects = "";
            } else {
                StringBuilder builder = new StringBuilder();
                for (Object effect : active) {
                    builder.append(String.valueOf(effect).toLowerCase(Locale.ROOT)).append(';');
                }
                state.effects = builder.toString();
            }
        }
        return state.effects;
    }

    /** Steht der Spieler auf einem Boot, Shulker o.A.? Dann ist "keine Bodenhaftung" legitim. */
    private boolean standingOnEntity(Player player) {
        for (org.bukkit.entity.Entity entity : player.getNearbyEntities(1.6, 1.6, 1.6)) {
            if (entity instanceof Vehicle || entity instanceof Shulker || entity instanceof ArmorStand) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (this.bypass(player)) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return; // nur Kopfdrehung
        }
        State state = this.state(player);
        long now = System.currentTimeMillis();
        this.trail(player, state, now, "move");
        double dy = to.getY() - from.getY();
        // Vanilla-Schwerkraft: jeder Positionspaket entspricht genau einem Client-Tick
        double predicted = (state.vy - 0.08) * 0.98;
        double deviation = dy - predicted;
        state.vy = dy;
        if (state.kbUntil > now) {
            state.kbDist += Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ());
        }
        boolean ground = this.groundNear(to);
        boolean medium = this.specialMedium(to);
        if (this.exempt(player, state, now)) {
            // Sicherheitsgrenze: Rueckstoss/Explosion/Drache duerfen kraeftig schleudern, aber nicht wie ein Teleport wirken
            double jump = Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ());
            double cap = this.num("launch-max-move", 7.0);
            GameMode gm = player.getGameMode();
            if (cap > 0.0 && jump > cap && gm != GameMode.CREATIVE && gm != GameMode.SPECTATOR && !player.getAllowFlight() && !player.isInsideVehicle()
                    && !player.isGliding() && !player.isRiptiding() && !this.laggy(player)) {
                this.violation(player, "Speed", 2.0, String.format(Locale.ROOT, "Schub-Sprung %.1f Bloecke in einem Schritt", jump), false);
            }
            state.winStart = 0L;
            state.hover = 0;
            state.devCount = 0;
            state.airMoves = 0;
            state.lastGroundY = to.getY();
            if (ground) {
                state.lastSafe = to.clone();
                if (state.launched && !player.isGliding() && now - state.launchedAt > 1500L && ++state.groundMoves >= 2) {
                    state.launched = false;
                    state.groundMoves = 0;
                    state.exemptUntil = Math.max(state.exemptUntil, now + 1000L);
                }
            } else {
                state.groundMoves = 0;
            }
            return;
        }
        boolean laggy = this.laggy(player);
        // --- Timer: zu viele Bewegungspakete pro Sekunde ---
        if (state.timerStart == 0L) {
            state.timerStart = now;
            state.timerCount = 0;
        }
        state.timerCount++;
        if (now - state.timerStart >= 1000L) {
            double rate = state.timerCount * 1000.0 / (now - state.timerStart);
            if (!laggy && rate > this.num("timer-max-moves", 30.0)) {
                if (++state.timerBad >= 3) {
                    state.timerBad = 0;
                    this.violation(player, "Timer", 2.0, String.format(Locale.ROOT, "%.0f Bewegungen/s", rate), false);
                }
            } else if (state.timerBad > 0) {
                state.timerBad--;
            }
            state.timerStart = now;
            state.timerCount = 0;
        }
        // --- Phase: im Block (Fuesse und Kopf in festem Block) ---
        if (!laggy && !player.isInsideVehicle() && to.getBlock().getType().isOccluding() && to.clone().add(0.0, 1.0, 0.0).getBlock().getType().isOccluding()
                && Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ()) > 0.12) {
            this.violation(player, "Phase", 2.0, "Bewegung durch feste Blöcke", true);
            return;
        }
        // --- Jesus: auf Wasser/Lava laufen ---
        Material under = to.getWorld().getBlockAt(to.getBlockX(), floor(to.getY() - 0.3), to.getBlockZ()).getType();
        if ((under == Material.WATER || under == Material.LAVA) && to.getBlock().getType().isAir() && Math.abs(dy) < 0.02
                && Math.hypot(to.getX() - from.getX(), to.getZ() - from.getZ()) > 0.05 && !player.isInsideVehicle() && !this.standingOnEntity(player)) {
            if (++state.waterWalk >= this.integer("jesus-moves", 12)) {
                state.waterWalk = 0;
                this.violation(player, "Jesus", 2.0, "läuft auf Flüssigkeit", true);
            }
        } else {
            state.waterWalk = 0;
        }
        String effects = this.effects(player, state, now);
        boolean boosted = effects.contains("speed") || effects.contains("dolphin");
        boolean lifted = effects.contains("jump") || effects.contains("levitation") || effects.contains("slow_falling");
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        // --- Speed: Strecke pro Sekunde ---
        if (state.winStart == 0L) {
            state.winStart = now;
            state.winDist = 0.0;
        }
        state.winDist += dist;
        if (now - state.winStart >= 1000L) {
            double seconds = (now - state.winStart) / 1000.0;
            double speed = state.winDist / seconds;
            double limit = this.num("speed-limit-v2", 9.2);
            if (boosted) {
                limit *= 1.7;
            }
            if (this.icy(to) || medium) {
                limit *= 1.6;
            }
            if (speed > limit && !laggy) {
                this.violation(player, "Speed", 1.0 + Math.min(3.0, (speed - limit) / limit * 3.0), String.format(Locale.ROOT, "%.1f Bl\u00f6cke/s (max %.1f)", speed, limit), speed > limit * 1.25);
            }
            state.winStart = now;
            state.winDist = 0.0;
        }
        // --- Step/HighJump: einzelne Bewegung nach oben viel groesser als ein Sprung ---
        if (!laggy && !lifted && !medium && dy > this.num("step-max", 0.75)) {
            this.violation(player, "Step", 1.5, String.format(Locale.ROOT, "%.2f Blöcke in einer Bewegung", dy), true);
            return;
        }
        // --- NoFall: meldet Bodenkontakt, obwohl nichts unter den Fuessen ist ---
        if (!ground && !medium && dy < -0.4 && player.isOnGround()) {
            if (++state.noFall >= this.integer("nofall-moves", 4)) {
                state.noFall = 0;
                this.violation(player, "NoFall", 2.0, "Bodenkontakt in der Luft", true);
            }
        } else if (ground || medium) {
            state.noFall = 0;
        }
        // --- Fly: Bewegung muss der Schwerkraft folgen ---
        if (ground || medium || lifted) {
            state.airMoves = 0;
            state.devCount = 0;
            state.lastGroundY = to.getY();
            if (ground && !medium) {
                state.lastSafe = to.clone();
            }
            return;
        }
        state.airMoves++;
        if (state.airMoves <= 3) {
            return; // Absprung
        }
        if (deviation > 0.30) {
            // ploetzlicher Schub nach oben (Windcharge, Explosion, Trampolin ...): bis zur Landung nicht pruefen
            state.launched = true;
            state.launchedAt = now;
            state.devCount = 0;
            state.winStart = 0L;
            state.launches.add(now);
            state.launches.removeIf(time -> now - time > 10000L);
            if (state.launches.size() > Math.max(30, this.integer("max-launches-per-10s", 40))) {
                state.launches.clear();
                this.violation(player, "Fly", 2.0, "wiederholte Spr\u00fcnge in der Luft", true);
            }
            return;
        }
        if (deviation > 0.035) {
            state.devCount++;
            if (state.devCount >= this.integer("fly-moves", 8)) {
                state.devCount = 0;
                if (!laggy && !this.standingOnEntity(player)) {
                    this.violation(player, "Fly", 2.0, "Bewegung ohne Schwerkraft", true);
                }
            }
        } else if (state.devCount > 0) {
            state.devCount--;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        State state = this.state(event.getPlayer());
        state.exemptUntil = System.currentTimeMillis() + 1500L;
        state.winStart = 0L;
        state.hover = 0;
        state.vy = 0.0;
        state.airMoves = 0;
        state.devCount = 0;
        if (event.getTo() != null) {
            state.lastSafe = event.getTo().clone();
            state.lastGroundY = event.getTo().getY();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVelocity(PlayerVelocityEvent event) {
        State state = this.state(event.getPlayer());
        long now = System.currentTimeMillis();
        state.exemptUntil = Math.max(state.exemptUntil, now + 1800L);
        state.launched = true; // Knockback/Windcharge: bis zur Landung nicht pruefen
        state.launchedAt = now;
        state.winStart = 0L;
        state.devCount = 0;
        this.watchKnockback(event, state, now);
    }

    /** Anti-Knockback: Wer kraeftig zurueckgeschleudert wird, sich aber kaum bewegt, hat den Rueckstoss vermutlich abgeschaltet. */
    private void watchKnockback(PlayerVelocityEvent event, State state, long now) {
        try {
            org.bukkit.util.Vector v = event.getVelocity();
            if (v == null) {
                return;
            }
            double horizontal = Math.hypot(v.getX(), v.getZ());
            if (horizontal < 0.45 || state.kbUntil > now || now < state.windUntil) {
                return;
            }
            final Player player = event.getPlayer();
            state.kbExpected = horizontal;
            state.kbDist = 0.0;
            state.kbUntil = now + 700L;
            final int token = ++state.kbToken;
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> this.evaluateKnockback(player, state, token), 16L);
        }
        catch (Throwable ignored) {
            // nur Pruefung
            Store.problem("AntiCheat:639", ignored);
        }
    }

    private boolean boxedIn(Location at) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    if (!at.clone().add(dx, dy, dz).getBlock().getType().isAir()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void evaluateKnockback(Player player, State state, int token) {
        if (state.kbToken != token || !player.isOnline() || this.bypass(player)) {
            return;
        }
        double expected = state.kbExpected;
        double moved = state.kbDist;
        state.kbExpected = 0.0;
        state.kbUntil = 0L;
        if (player.isDead() || player.isGliding() || player.isInsideVehicle() || this.laggy(player) || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR || this.specialMedium(player.getLocation()) || this.boxedIn(player.getLocation())) {
            return;
        }
        if (moved < expected * 0.25) {
            this.violation(player, "Velocity", 1.5, String.format(Locale.ROOT, "Rückstoß %.2f ignoriert (bewegt %.2f)", expected, moved), false);
        }
    }

    /** Windladung/Mace-Schub: Spieler wird ohne Velocity-Event geschleudert - alle Bewegungspruefungen bis zur Landung aussetzen. */
    private void markLaunch(Player player, long ms) {
        State state = this.state(player);
        long now = System.currentTimeMillis();
        state.exemptUntil = Math.max(state.exemptUntil, now + ms);
        state.windUntil = Math.max(state.windUntil, now + ms + 4000L);
        state.kbToken++;
        state.kbUntil = 0L;
        state.kbExpected = 0.0;
        state.launched = true;
        state.launchedAt = now;
        state.winStart = 0L;
        state.hover = 0;
        state.vy = 0.0;
        state.airMoves = 0;
        state.devCount = 0;
    }

    private static boolean windCharge(org.bukkit.entity.Entity entity) {
        return entity != null && entity.getClass().getSimpleName().contains("WindCharge");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWindLaunch(org.bukkit.event.entity.ProjectileLaunchEvent event) {
        try {
            if (windCharge(event.getEntity()) && event.getEntity().getShooter() instanceof Player player) {
                this.markLaunch(player, 6000L);
            }
        }
        catch (Throwable ignored) {
            // nur Komfort
            Store.problem("AntiCheat:703", ignored);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWindHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        try {
            if (!windCharge(event.getEntity())) {
                return;
            }
            Location at = event.getEntity().getLocation();
            for (Player other : this.plugin.getServer().getOnlinePlayers()) {
                if (other.getWorld().equals(at.getWorld()) && other.getLocation().distanceSquared(at) <= 100.0) {
                    this.markLaunch(other, 8000L);
                }
            }
        }
        catch (Throwable ignored) {
            // nur Komfort
            Store.problem("AntiCheat:721", ignored);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMaceSmash(EntityDamageByEntityEvent event) {
        try {
            if (event.getDamager() instanceof Player attacker && attacker.getInventory().getItemInMainHand().getType().name().equals("MACE")) {
                this.markLaunch(attacker, 8000L);
            }
        }
        catch (Throwable ignored) {
            // nur Komfort
            Store.problem("AntiCheat:733", ignored);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAnyDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            State state = this.state(player);
            state.exemptUntil = Math.max(state.exemptUntil, System.currentTimeMillis() + 1200L);
            state.winStart = 0L;
            EntityDamageEvent.DamageCause cause = event.getCause();
            String causeName = cause.name();
            if (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                    || causeName.equals("SONIC_BOOM") || causeName.equals("WIND_BURST") || causeName.equals("FIREWORKS")) {
                this.markLaunch(player, 6000L);
            }
        }
    }

    /** Gegner, die Spieler weit wegschleudern (Enderdrache, Wither, Warden, Eisengolem, Hoglin ...): kein Cheat-Alarm durch den Rueckstoss. */
    private static final String[] HEAVY_HITTERS = {"EnderDragon", "Wither", "Warden", "Ravager", "Breeze", "Ghast", "Fireball", "TNTPrimed", "Creeper",
            "EnderCrystal", "IronGolem", "Hoglin", "Zoglin", "Evoker", "Vex", "Phantom", "Guardian"};

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeavyHit(EntityDamageByEntityEvent event) {
        try {
            if (!(event.getEntity() instanceof Player victim)) {
                return;
            }
            org.bukkit.entity.Entity source = event.getDamager();
            if (source instanceof org.bukkit.entity.Projectile projectile && projectile.getShooter() instanceof org.bukkit.entity.Entity shooter) {
                source = shooter;
            }
            String name = source.getClass().getSimpleName();
            for (String heavy : HEAVY_HITTERS) {
                if (name.contains(heavy)) {
                    this.markLaunch(victim, name.contains("EnderDragon") || name.contains("Wither") ? 9000L : 6000L);
                    return;
                }
            }
        }
        catch (Throwable ignored) {
            Store.problem("AntiCheat:heavyHit", ignored);
        }
    }

    /* ---------- Kampf: Reach / KillAura / AutoClick ---------- */

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (this.bypass(attacker) || event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }
        GameMode mode = attacker.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
            return;
        }
        State state = this.state(attacker);
        long now = System.currentTimeMillis();
        Location eye = attacker.getEyeLocation();
        Location feet = victim.getLocation();
        // Abstand vom Auge zur naechsten Stelle der Hitbox (0,6 x 1,8) statt zur Mitte - das entspricht dem echten Reichweitenlimit
        double nx = Math.max(feet.getX() - 0.3, Math.min(eye.getX(), feet.getX() + 0.3));
        double ny = Math.max(feet.getY(), Math.min(eye.getY(), feet.getY() + 1.8));
        double nz = Math.max(feet.getZ() - 0.3, Math.min(eye.getZ(), feet.getZ() + 0.3));
        double reach = Math.sqrt((eye.getX() - nx) * (eye.getX() - nx) + (eye.getY() - ny) * (eye.getY() - ny) + (eye.getZ() - nz) * (eye.getZ() - nz));
        boolean airborne = state.launched || now < state.windUntil;
        double lagAllowance = Math.min(1.2, Math.max(0, attacker.getPing()) / 1000.0 * 6.0) + (airborne ? 1.0 : 0.0);
        double legit = this.num("reach-v2-base", 3.05) + lagAllowance;
        double flagAt = legit + this.num("reach-v2-flag-margin", 0.35);
        double cancelAt = legit + this.num("reach-v2-cancel-margin", 1.1);
        if (reach > cancelAt) {
            event.setCancelled(true);
            this.violation(attacker, "Reach", 2.0, String.format(Locale.ROOT, "%.2f Blöcke (blockiert, erlaubt %.2f)", reach, legit), false);
            return;
        }
        if (reach > flagAt && !this.laggy(attacker)) {
            this.violation(attacker, "Reach", 1.0, String.format(Locale.ROOT, "%.2f Blöcke (erlaubt %.2f)", reach, legit), false);
        }
        // Blickwinkel: wie weit zeigt das Fadenkreuz am Ziel vorbei? (dreidimensional, nicht nur seitlich)
        double yawRad = Math.toRadians(attacker.getLocation().getYaw());
        double pitchRad = Math.toRadians(attacker.getLocation().getPitch());
        double lookX = -Math.sin(yawRad) * Math.cos(pitchRad);
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * Math.cos(pitchRad);
        double toX = feet.getX() - eye.getX();
        double toY = feet.getY() + 0.9 - eye.getY();
        double toZ = feet.getZ() - eye.getZ();
        double toLen = Math.sqrt(toX * toX + toY * toY + toZ * toZ);
        if (toLen > 1.8) {
            double cos = (lookX * toX + lookY * toY + lookZ * toZ) / toLen;
            double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cos))));
            if (angle > this.num("aim-cancel-angle", 100.0) && !airborne) {
                event.setCancelled(true);
                this.violation(attacker, "KillAura", 2.0, String.format(Locale.ROOT, "Ziel %.0f° neben dem Fadenkreuz (blockiert)", angle), false);
                return;
            }
            if (angle > this.num("aim-flag-angle", 62.0) && !this.laggy(attacker)) {
                this.violation(attacker, "KillAura", 0.8, String.format(Locale.ROOT, "Ziel %.0f° neben dem Fadenkreuz", angle), false);
            }
        }
        // Blickrichtung: trifft man jemanden hinter sich?
        double vx = victim.getLocation().getX() - attacker.getLocation().getX();
        double vz = victim.getLocation().getZ() - attacker.getLocation().getZ();
        double horizontal = Math.sqrt(vx * vx + vz * vz);
        if (horizontal > 1.5) {
            double yaw = Math.toRadians(attacker.getLocation().getYaw());
            double lx = -Math.sin(yaw);
            double lz = Math.cos(yaw);
            double dot = (lx * vx + lz * vz) / horizontal;
            if (dot < -0.6 && !airborne) {
                event.setCancelled(true);
                this.violation(attacker, "KillAura", 2.0, "Treffer hinter dem Blickfeld (blockiert)", false);
                return;
            }
            if (dot < -0.3) {
                this.violation(attacker, "KillAura", 0.5, "Treffer seitlich/hinten", false);
            }
        }
        // Treffer pro Sekunde
        state.hits.add(now);
        state.hits.removeIf(time -> now - time > 1000L);
        if (state.hits.size() > this.integer("max-hits-per-second", 20)) {
            event.setCancelled(true);
            if (now - state.lastHitFlag > 1000L) {
                state.lastHitFlag = now;
                this.violation(attacker, "AutoClick", 1.5, state.hits.size() + " Treffer/s", false);
            }
            return;
        }
        // mehrere Ziele fast gleichzeitig
        state.recentVictims.put(victim.getUniqueId(), now);
        state.recentVictims.values().removeIf(time -> now - time > 350L);
        if (state.recentVictims.size() >= 3) {
            state.recentVictims.clear();
            this.violation(attacker, "KillAura", 2.0, "3 Ziele in 0,35 s", false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (this.bypass(player)) {
            return;
        }
        State state = this.state(player);
        long now = System.currentTimeMillis();
        state.clicks.add(now);
        state.clicks.removeIf(time -> now - time > 1000L);
        state.clickTimes.add(now);
        while (state.clickTimes.size() > 21) {
            state.clickTimes.remove(0);
        }
        if (state.clickTimes.size() >= 21 && state.clicks.size() >= this.integer("steady-cps", 8)) {
            long first = state.clickTimes.get(1) - state.clickTimes.get(0);
            boolean steady = true;
            for (int i = 2; i < state.clickTimes.size(); i++) {
                if (Math.abs((state.clickTimes.get(i) - state.clickTimes.get(i - 1)) - first) > 4L) {
                    steady = false;
                    break;
                }
            }
            if (steady && now - state.lastCpsFlag > 1000L) {
                state.lastCpsFlag = now;
                state.clickTimes.clear();
                this.violation(player, "AutoClick", 2.0, "perfekt gleichmäßige Klicks (" + first + " ms)", false);
            }
        }
        if (state.clicks.size() > this.integer("max-cps", 24) && now - state.lastCpsFlag > 1000L) {
            state.lastCpsFlag = now;
            this.violation(player, "AutoClick", 1.0, state.clicks.size() + " Klicks/s", false);
        }
    }

    /** Fuer andere Klassen: true, wenn dieses Break-Event von einem Gem-Werkzeug/Vein Miner stammt. */
    static boolean skipBreak(Player player) {
        return Gems.busyFor(player.getUniqueId()) || VeinMiner.busyFor(player.getUniqueId());
    }

    /** Fuer andere Klassen (Anti-X-Ray): Verstoss melden, ohne Zurueckversetzen. */
    static void report(Player player, String check, double weight, String detail) {
        if (instance != null && !instance.bypass(player)) {
            instance.violation(player, check, weight, detail, false);
        }
    }

    static boolean active() {
        return instance != null && instance.enabled();
    }

    /* ---------- Explosionen: Rueckstoss und Fallschaden duerfen nie als Cheat zaehlen ---------- */

    private void explosionAt(Location at, double radius) {
        if (at == null || at.getWorld() == null) {
            return;
        }
        double r2 = radius * radius;
        for (Player other : this.plugin.getServer().getOnlinePlayers()) {
            if (other.getWorld().equals(at.getWorld()) && other.getLocation().distanceSquared(at) <= r2) {
                this.markLaunch(other, 6000L);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        try {
            this.explosionAt(event.getLocation(), 16.0);
        }
        catch (Throwable ignored) {
            Store.problem("AntiCheat:explode", ignored);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        try {
            this.explosionAt(event.getBlock().getLocation(), 16.0);
        }
        catch (Throwable ignored) {
            Store.problem("AntiCheat:blockexplode", ignored);
        }
    }

    /* ---------- Bauen: FastPlace / FastBreak ---------- */

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (this.bypass(player) || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        State state = this.state(player);
        long now = System.currentTimeMillis();
        // Scaffold: Block unter den Fuessen setzen, obwohl man geradeaus/nach oben schaut (legal ist nur der Blick nach unten)
        Location placed = event.getBlock().getLocation();
        Location at = player.getLocation();
        boolean below = placed.getBlockY() == at.getBlockY() - 1 && Math.abs(placed.getBlockX() + 0.5 - at.getX()) < 1.3 && Math.abs(placed.getBlockZ() + 0.5 - at.getZ()) < 1.3;
        if (below && at.getPitch() < this.num("scaffold-pitch", 18.0) && !this.laggy(player) && !player.isInsideVehicle()) {
            if (now - state.scaffoldStart > 6000L) {
                state.scaffoldStart = now;
                state.scaffold = 0;
            }
            if (++state.scaffold >= this.integer("scaffold-blocks", 4)) {
                state.scaffold = 0;
                event.setCancelled(true);
                this.violation(player, "Scaffold", 2.0, String.format(Locale.ROOT, "Blöcke unter den Füßen bei Blickwinkel %.0f°", (double)at.getPitch()), false);
                return;
            }
        }
        state.places.add(now);
        state.places.removeIf(time -> now - time > 1000L);
        if (state.places.size() > this.integer("max-places-per-second", 14)) {
            event.setCancelled(true);
            if (now - state.lastPlaceFlag > 1000L) {
                state.lastPlaceFlag = now;
                this.violation(player, "FastPlace", 1.5, state.places.size() + " Blöcke/s", false);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (this.bypass(player) || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        // Extra-Bloecke der Gem-Werkzeuge (3x3, Baumfaellen) und des Vein Miners sind keine Spieler-Abbauten
        if (skipBreak(player)) {
            return;
        }
        State state = this.state(player);
        long now = System.currentTimeMillis();
        state.breaks.add(now);
        state.breaks.removeIf(time -> now - time > 1000L);
        int allowed = this.integer("max-breaks-per-second", 16);
        String active = this.effects(player, state, now);
        if (active.contains("haste") || active.contains("fast_digging")) {
            allowed += 8;
        }
        if (state.breaks.size() > allowed) {
            event.setCancelled(true);
            if (now - state.lastBreakFlag > 1000L) {
                state.lastBreakFlag = now;
                this.violation(player, "FastBreak", 1.5, state.breaks.size() + " Blöcke/s", false);
            }
        }
    }

    /* ---------- Chat- und Befehls-Spam ---------- */

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (this.bypass(player) || !this.flag("spam.enabled", true)) {
            return;
        }
        State state = this.state(player);
        long now = System.currentTimeMillis();
        synchronized (state) {
            if (now - state.chatWindowStart > 4000L) {
                state.chatWindowStart = now;
                state.chatCount = 0;
            }
            state.chatCount++;
            String message = event.getMessage().trim().toLowerCase(Locale.ROOT);
            boolean repeated = message.equals(state.lastMessage) && now - state.lastMessageAt < 8000L;
            state.lastMessage = message;
            state.lastMessageAt = now;
            if (state.chatCount > this.integer("spam.messages-per-4s", 5) || repeated || message.length() > 256) {
                event.setCancelled(true);
                this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage("§cBitte nicht spammen – warte einen Moment.");
                    }
                });
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommandSpam(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (this.bypass(player) || !this.flag("spam.enabled", true)) {
            return;
        }
        State state = this.state(player);
        long now = System.currentTimeMillis();
        if (now - state.cmdWindowStart > 3000L) {
            state.cmdWindowStart = now;
            state.cmdCount = 0;
        }
        state.cmdCount++;
        if (state.cmdCount > this.integer("spam.commands-per-3s", 8)) {
            event.setCancelled(true);
            player.sendMessage("§cZu viele Befehle in kurzer Zeit.");
        }
    }

    /* ---------- Accounts: Zweitaccounts, Bots ---------- */

    private String hash(String ip) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((this.salt + ip).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 12; i++) {
                builder.append(String.format("%02x", bytes[i]));
            }
            return builder.toString();
        }
        catch (Exception e) {
            return "x" + ip.hashCode();
        }
    }

    private void saveAccounts() {
        try {
            Store.save(this.accounts, this.accountsFile);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("accounts.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    /** Spieler, die dieselbe (gehashte) IP benutzt haben, ohne den Spieler selbst. */
    List<UUID> linked(UUID id) {
        List<UUID> out = new ArrayList<UUID>();
        String hash = this.ipHashes.get(id);
        if (hash == null) {
            return out;
        }
        ConfigurationSection section = this.accounts.getConfigurationSection("ips." + hash);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID other = UUID.fromString(key);
                    if (!other.equals(id)) {
                        out.add(other);
                    }
                }
                catch (IllegalArgumentException ignored) {
                }
            }
        }
        return out;
    }

    private String nameOf(UUID id) {
        String hash = this.ipHashes.get(id);
        String stored = hash == null ? null : this.accounts.getString("ips." + hash + "." + id);
        if (stored != null) {
            return stored;
        }
        String name = this.plugin.getServer().getOfflinePlayer(id).getName();
        return name == null ? id.toString().substring(0, 8) : name;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!this.enabled() || player.getAddress() == null) {
            return;
        }
        String hash = this.hash(player.getAddress().getAddress().getHostAddress());
        UUID id = player.getUniqueId();
        this.ipHashes.put(id, hash);
        long now = System.currentTimeMillis();
        // Bot-/Join-Flut
        List<Long> times = this.joinTimes.computeIfAbsent(hash, key -> new ArrayList<Long>());
        int recent;
        synchronized (times) {
            times.add(now);
            times.removeIf(time -> now - time > 20000L);
            recent = times.size();
        }
        if (!player.hasPermission("duelnetwork.anticheat.bypass") && recent > this.integer("join-flood.max-joins-per-20s", 6)) {
            this.log("JOIN-FLOOD " + player.getName() + " (" + recent + " Joins/20s)");
            player.kickPlayer("§cZu viele Verbindungen in kurzer Zeit.\n§7Bitte warte einen Moment.");
            return;
        }
        // Zweitaccounts erfassen
        this.accounts.set("ips." + hash + "." + id, player.getName());
        this.saveAccounts();
        List<UUID> others = this.linked(id);
        if (others.isEmpty()) {
            return;
        }
        boolean bannedLink = false;
        StringBuilder names = new StringBuilder();
        for (UUID other : others) {
            boolean banned = this.plugin.getServer().getOfflinePlayer(other).isBanned();
            bannedLink |= banned;
            if (names.length() > 0) {
                names.append("§7, ");
            }
            names.append(banned ? "§4" : "§f").append(this.nameOf(other)).append(banned ? " (gebannt)" : "");
        }
        this.log("ALT " + player.getName() + " teilt Netzwerk mit " + names.toString().replaceAll("§.", ""));
        this.alert(PREFIX + "§f" + player.getName() + " §7teilt ein Netzwerk mit: " + names + (bannedLink ? " §c⚠ gebannter Account!" : ""));
        int max = this.integer("alts.max-accounts-per-ip", 0);
        if (max > 0 && others.size() + 1 > max && !player.hasPermission("duelnetwork.anticheat.bypass")) {
            player.kickPlayer("§cVon deinem Netzwerk sind zu viele Accounts online/registriert.\n§7Maximal " + max + " Accounts.");
            return;
        }
        if (bannedLink && this.flag("alts.kick-if-linked-to-banned", false) && !player.hasPermission("duelnetwork.anticheat.bypass")) {
            player.kickPlayer("§cDein Netzwerk ist mit einem gebannten Account verbunden.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        this.states.remove(event.getPlayer().getUniqueId());
    }

    /* ---------- Schutz fuer Statistiken und Rekorde ---------- */

    /** Zaehlt ein Duell nur, wenn es nicht nach Win-Trading oder Zweitaccount-Boosting aussieht. */
    static boolean allowStats(Player winner, Player loser) {
        AntiCheat ac = instance;
        if (ac == null || !ac.enabled() || !ac.flag("stats.protect", true)) {
            return true;
        }
        long now = System.currentTimeMillis();
        String pair = winner.getUniqueId() + ">" + loser.getUniqueId();
        String hw = ac.ipHashes.get(winner.getUniqueId());
        String hl = ac.ipHashes.get(loser.getUniqueId());
        if (hw != null && hw.equals(hl)) {
            Long last = ac.pairAlerted.get(pair);
            if (last == null || now - last > 600000L) {
                ac.pairAlerted.put(pair, now);
                ac.alert(PREFIX + "§f" + winner.getName() + " §7und §f" + loser.getName() + " §7teilen ein Netzwerk – Duel zählt nicht für die Statistik.");
                ac.log("STATS-IGNORED same-network " + winner.getName() + " vs " + loser.getName());
            }
            return false;
        }
        int limit = ac.integer("stats.max-same-pair-per-hour", 8);
        List<Long> times = ac.pairWins.computeIfAbsent(pair, key -> new ArrayList<Long>());
        synchronized (times) {
            times.removeIf(time -> now - time > 3600000L);
            if (times.size() >= limit) {
                Long last = ac.pairAlerted.get(pair);
                if (last == null || now - last > 600000L) {
                    ac.pairAlerted.put(pair, now);
                    ac.alert(PREFIX + "§f" + winner.getName() + " §7gewinnt auffallend oft gegen §f" + loser.getName() + " §7(Win-Trading?) – weitere Duels zählen nicht.");
                    ac.log("STATS-IGNORED win-trading " + winner.getName() + " vs " + loser.getName());
                }
                return false;
            }
            times.add(now);
        }
        return true;
    }

    /** Parkour-Zeit nur werten, wenn sie plausibel und nicht von einem auffaelligen Spieler ist. */
    static boolean allowRecord(Player player, String map, long millis) {
        AntiCheat ac = instance;
        if (ac == null || !ac.enabled() || !ac.flag("parkour.protect", true) || player.hasPermission("duelnetwork.anticheat.bypass")) {
            return true;
        }
        double minSeconds = ac.num("parkour.min-seconds", 3.0);
        double suspicious = ac.totalVl(player.getUniqueId());
        if (millis < minSeconds * 1000.0 || suspicious >= ac.num("parkour.max-vl", 8.0)) {
            ac.log("RECORD-REJECTED " + player.getName() + " " + map + " " + millis + "ms VL " + suspicious);
            ac.alert(PREFIX + "§f" + player.getName() + " §7Parkour-Zeit auf §f" + map + " §7nicht gewertet (" + ParkourEnhancements.formatTime(millis) + ", VL " + String.format(Locale.ROOT, "%.1f", suspicious) + ").");
            player.sendMessage("§cDeine Zeit wurde nicht gewertet (auffällige Bewegung). Versuch es noch einmal.");
            return false;
        }
        return true;
    }

    /* ---------- /anticheat ---------- */

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("duelnetwork.admin")) {
            sender.sendMessage("§cDafür brauchst du die Permission duelnetwork.admin.");
            return true;
        }
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("status") && args.length >= 2) {
            sub = "info"; // /anticheat status <Spieler> = Verstoesse des Spielers
        }
        switch (sub) {
            case "on": case "off": {
                boolean on = sub.equals("on");
                this.plugin.getConfig().set("anticheat.enabled", on);
                this.plugin.saveConfig();
                this.cacheStamp = 0L;
                this.refreshCache();
                this.log("Anti-Cheat " + (on ? "AN" : "AUS") + " durch " + sender.getName());
                sender.sendMessage(PREFIX + "Anti-Cheat ist jetzt " + (on ? "§aan" : "§caus") + "§7.");
                this.alert(PREFIX + "§7" + sender.getName() + " hat den Anti-Cheat " + (on ? "§aeingeschaltet" : "§causgeschaltet") + "§7.");
                return true;
            }
            case "xray": {
                sender.sendMessage(PREFIX + "Anti-X-Ray: " + (this.flag("xray.enabled", true) ? "§aan" : "§caus") + " §8(§7anticheat.xray.enabled§8)");
                sender.sendMessage("§7Der Plugin-Teil erkennt Auffälligkeiten beim Erzabbau (verdeckte Erze direkt angesteuert). Die Engine gegen X-Ray-Texturpakete liegt in Paper:");
                sender.sendMessage("§fconfig/paper-world-defaults.yml §7→ §fanticheat.anti-xray.enabled: true§7, §fengine-mode: 2§7 (Server danach neu starten).");
                return true;
            }
            case "replay": {
                if (args.length < 2) {
                    sender.sendMessage("§cNutze: /anticheat replay <Spieler>");
                    return true;
                }
                File dir = new File(this.plugin.getDataFolder(), "anticheat-replays");
                File[] all = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT) + "-") && n.endsWith(".log"));
                if (all == null || all.length == 0) {
                    sender.sendMessage(PREFIX + "Kein Replay für §f" + args[1] + " §7vorhanden.");
                    return true;
                }
                java.util.Arrays.sort(all, java.util.Comparator.comparingLong(File::lastModified));
                File newest = all[all.length - 1];
                try {
                    List<String> lines = java.nio.file.Files.readAllLines(newest.toPath(), StandardCharsets.UTF_8);
                    sender.sendMessage(PREFIX + "Replay §f" + newest.getName() + " §8(" + all.length + " Datei(en), letzte 20 Zeilen)");
                    for (int i = Math.max(0, lines.size() - 20); i < lines.size(); i++) {
                        sender.sendMessage("§8" + lines.get(i));
                    }
                }
                catch (IOException ex) {
                    sender.sendMessage("§cReplay nicht lesbar: " + ex.getMessage());
                }
                return true;
            }
            case "alerts": {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Nur im Spiel.");
                    return true;
                }
                boolean on = args.length >= 2 ? args[1].equalsIgnoreCase("on") : this.alertsOff.contains(player.getUniqueId());
                if (on) {
                    this.alertsOff.remove(player.getUniqueId());
                } else {
                    this.alertsOff.add(player.getUniqueId());
                }
                sender.sendMessage(PREFIX + "Warnungen sind jetzt " + (on ? "§aan" : "§caus") + "§7.");
                return true;
            }
            case "info": case "vl": {
                if (args.length < 2) {
                    sender.sendMessage("§cNutze: /anticheat info <Spieler>");
                    return true;
                }
                Player target = this.plugin.getServer().getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cSpieler nicht online.");
                    return true;
                }
                State state = this.states.get(target.getUniqueId());
                sender.sendMessage(PREFIX + "§f" + target.getName() + " §7– Gesamt-VL §c" + String.format(Locale.ROOT, "%.1f", this.totalVl(target.getUniqueId())));
                if (state != null) {
                    for (Map.Entry<String, Double> entry : state.vl.entrySet()) {
                        sender.sendMessage("§8 • §7" + entry.getKey() + ": §f" + String.format(Locale.ROOT, "%.1f", entry.getValue()));
                    }
                }
                List<UUID> others = this.linked(target.getUniqueId());
                sender.sendMessage("§7Verknüpfte Accounts: " + (others.isEmpty() ? "§akeine" : "§f" + others.size() + " §7(/anticheat alts " + target.getName() + ")"));
                return true;
            }
            case "alts": {
                if (args.length < 2) {
                    sender.sendMessage("§cNutze: /anticheat alts <Spieler>");
                    return true;
                }
                Player target = this.plugin.getServer().getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("§cSpieler nicht online (Verknüpfungen werden beim Beitritt ermittelt).");
                    return true;
                }
                List<UUID> others = this.linked(target.getUniqueId());
                if (others.isEmpty()) {
                    sender.sendMessage(PREFIX + "§f" + target.getName() + " §7hat keine verknüpften Accounts.");
                    return true;
                }
                sender.sendMessage(PREFIX + "§f" + target.getName() + " §7teilt ein Netzwerk mit:");
                for (UUID other : others) {
                    boolean banned = this.plugin.getServer().getOfflinePlayer(other).isBanned();
                    sender.sendMessage("§8 • " + (banned ? "§4" : "§f") + this.nameOf(other) + (banned ? " §c(gebannt)" : ""));
                }
                return true;
            }
            case "log": {
                int count = 10;
                try {
                    count = args.length >= 2 ? Math.max(1, Math.min(40, Integer.parseInt(args[1]))) : 10;
                }
                catch (NumberFormatException ignored) {
                    count = 10;
                }
                try {
                    List<String> lines = java.nio.file.Files.readAllLines(this.logFile.toPath(), StandardCharsets.UTF_8);
                    sender.sendMessage(PREFIX + "Letzte " + Math.min(count, lines.size()) + " Einträge:");
                    for (int i = Math.max(0, lines.size() - count); i < lines.size(); i++) {
                        sender.sendMessage("§8" + lines.get(i));
                    }
                }
                catch (IOException e) {
                    sender.sendMessage(PREFIX + "Noch kein Log vorhanden.");
                }
                return true;
            }
            case "top": {
                List<Map.Entry<UUID, Double>> list = new ArrayList<Map.Entry<UUID, Double>>();
                for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                    double vl = this.totalVl(online.getUniqueId());
                    if (vl > 0.0) {
                        list.add(Map.entry(online.getUniqueId(), vl));
                    }
                }
                list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
                sender.sendMessage(PREFIX + "Auffälligste Spieler:");
                if (list.isEmpty()) {
                    sender.sendMessage("§8 • §akeine");
                }
                for (int i = 0; i < Math.min(8, list.size()); i++) {
                    Player p = this.plugin.getServer().getPlayer(list.get(i).getKey());
                    sender.sendMessage("§8 • §f" + (p == null ? "?" : p.getName()) + " §7VL §c" + String.format(Locale.ROOT, "%.1f", list.get(i).getValue()));
                }
                return true;
            }
            case "reset": {
                if (args.length < 2) {
                    sender.sendMessage("§cNutze: /anticheat reset <Spieler>");
                    return true;
                }
                Player target = this.plugin.getServer().getPlayerExact(args[1]);
                State state = target == null ? null : this.states.get(target.getUniqueId());
                if (state == null) {
                    sender.sendMessage("§cKeine Daten für diesen Spieler.");
                    return true;
                }
                state.vl.clear();
                sender.sendMessage(PREFIX + "Verstöße von §f" + target.getName() + " §7zurückgesetzt.");
                return true;
            }
            default: {
                int flagged = 0;
                for (State state : this.states.values()) {
                    if (!state.vl.isEmpty()) {
                        flagged++;
                    }
                }
                sender.sendMessage(PREFIX + "Anti-Cheat ist " + (this.enabled() ? "§aaktiv" : "§cdeaktiviert") + "§7 – auffällige Spieler: §f" + flagged);
                sender.sendMessage("§7Befehle: §d/anticheat on|off §7(ganz ein-/ausschalten) | §d/anticheat alerts [on|off] §7(Warnungen für dich) | info <Spieler> | alts <Spieler> | top | log [Anzahl] | reset <Spieler> | xray");
                sender.sendMessage("§7Prüfungen: Speed, Fly, Step, NoFall, Jesus, Phase, Timer, Reach, KillAura, Velocity, Scaffold, AutoClick, FastPlace, FastBreak, Spam, Join-Flut, Zweitaccounts, Win-Trading, Parkour-Zeiten, Anti-X-Ray.");
                sender.sendMessage("§7Log: §fplugins/DuelNetwork/anticheat.log §8• §7Bypass: §fduelnetwork.anticheat.bypass");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            for (String sub : List.of("status", "on", "off", "alerts", "info", "alts", "top", "log", "reset", "xray", "replay")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("alerts")) {
            out.addAll(List.of("on", "off"));
        } else if (args.length == 2) {
            for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(online.getName());
                }
            }
        }
        return out;
    }
}
