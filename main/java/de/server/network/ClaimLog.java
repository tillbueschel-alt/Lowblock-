package de.server.network;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Leichtes Claim-Protokoll (Bau-SMP): merkt sich Abbauen/Platzieren von Bloecken in Claims (nur dort, nicht in der ganzen Welt)
 * und kann die Aenderungen eines Spielers zurueckspielen. Schreibt asynchron in claimlog.tsv, behaelt claims.log-days Tage (Standard 3).
 * /claim log - letzte Aktionen im Claim, in dem du stehst. /claim rollback <Spieler> [Minuten] - Claim-Besitzer (in seinem Claim) und
 * Admins (ueberall). Nicht zurueckgespielt werden Kisteninhalte und Entities. Das Zurueckspielen laeuft in kleinen Paketen (kein Lag).
 */
final class ClaimLog implements Listener {
    static ClaimLog instance;

    private record Entry(long ts, long claimId, UUID player, String world, int x, int y, int z, String before, String after) { }

    private final DuelNetworkPlugin plugin;
    private final ClaimManager claims;
    private final Path file;
    private final Deque<Entry> entries = new ArrayDeque<Entry>();
    private final Queue<String> pending = new ConcurrentLinkedQueue<String>();
    private final int maxEntries;

    ClaimLog(DuelNetworkPlugin plugin, ClaimManager claims) {
        this.plugin = plugin;
        this.claims = claims;
        instance = this;
        this.file = plugin.getDataFolder().toPath().resolve("claimlog.tsv");
        this.maxEntries = Math.max(1000, plugin.getConfig().getInt("claims.log-max-entries", 50000));
        this.load();
        plugin.getServer().getScheduler().runTaskTimerAsynchronously((Plugin)plugin, this::flush, 600L, 600L);
    }

    private synchronized void load() {
        long cutoff = System.currentTimeMillis() - Math.max(1, this.plugin.getConfig().getInt("claims.log-days", 3)) * 86400000L;
        List<String> keep = new ArrayList<String>();
        try {
            if (!Files.exists(this.file)) {
                return;
            }
            for (String line : Files.readAllLines(this.file, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t", -1);
                if (p.length < 9) {
                    continue;
                }
                try {
                    long ts = Long.parseLong(p[0]);
                    if (ts < cutoff) {
                        continue;
                    }
                    this.entries.addLast(new Entry(ts, Long.parseLong(p[1]), UUID.fromString(p[2]), p[3], Integer.parseInt(p[4]), Integer.parseInt(p[5]), Integer.parseInt(p[6]), p[7], p[8]));
                    keep.add(line);
                }
                catch (RuntimeException ignored) {
                    // kaputte Zeile
                }
            }
            while (this.entries.size() > this.maxEntries) {
                this.entries.removeFirst();
            }
            Files.write(this.file, keep, StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("claimlog.tsv konnte nicht gelesen werden: " + ex.getMessage());
        }
    }

    /** Schreibt wartende Zeilen (laeuft asynchron, beim Abschalten synchron). */
    void flush() {
        List<String> lines = new ArrayList<String>();
        String line;
        while ((line = this.pending.poll()) != null) {
            lines.add(line);
        }
        if (lines.isEmpty()) {
            return;
        }
        try {
            Files.write(this.file, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("claimlog.tsv konnte nicht geschrieben werden: " + ex.getMessage());
        }
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() == this.plugin) {
            this.flush();
        }
    }

    private synchronized void record(Claim claim, Player player, Block block, String before, String after) {
        Entry e = new Entry(System.currentTimeMillis(), claim.id, player.getUniqueId(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), before, after);
        this.entries.addLast(e);
        while (this.entries.size() > this.maxEntries) {
            this.entries.removeFirst();
        }
        this.pending.add(e.ts() + "\t" + e.claimId() + "\t" + e.player() + "\t" + e.world() + "\t" + e.x() + "\t" + e.y() + "\t" + e.z() + "\t" + before + "\t" + after);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Claim claim = this.claims.find(event.getBlock().getLocation());
        if (claim != null) {
            this.record(claim, event.getPlayer(), event.getBlock(), event.getBlock().getBlockData().getAsString(), "minecraft:air");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Claim claim = this.claims.find(event.getBlock().getLocation());
        if (claim != null) {
            this.record(claim, event.getPlayer(), event.getBlock(), event.getBlockReplacedState().getBlockData().getAsString(), event.getBlock().getBlockData().getAsString());
        }
    }

    private String ago(long ts) {
        long s = Math.max(0L, (System.currentTimeMillis() - ts) / 1000L);
        return s < 60L ? s + " s" : s < 3600L ? (s / 60L) + " Min" : (s / 3600L) + " Std";
    }

    private String name(UUID id) {
        String n = this.plugin.getServer().getOfflinePlayer(id).getName();
        return n == null ? id.toString().substring(0, 8) : n;
    }

    private String shortBlock(String data) {
        String t = data.replace("minecraft:", "");
        int i = t.indexOf('[');
        return i > 0 ? t.substring(0, i) : t;
    }

    /** /claim log */
    synchronized void show(Player player) {
        Claim claim = this.claims.find(player.getLocation());
        if (claim == null || (!claim.owner.equals(player.getUniqueId()) && !player.hasPermission("duelnetwork.admin"))) {
            player.sendMessage("§cStell dich in deinen eigenen Claim.");
            return;
        }
        player.sendMessage("§d✦ §fClaim-Protokoll §7" + claim.name + " §8(neueste zuerst)");
        int shown = 0;
        java.util.Iterator<Entry> it = this.entries.descendingIterator();
        while (it.hasNext() && shown < 10) {
            Entry e = it.next();
            if (e.claimId() != claim.id) {
                continue;
            }
            boolean broke = e.after().equals("minecraft:air");
            player.sendMessage("§8• §7vor " + this.ago(e.ts()) + " §f" + this.name(e.player()) + " §7" + (broke ? "baute ab: §f" + this.shortBlock(e.before()) : "platzierte: §f" + this.shortBlock(e.after()))
                    + " §8(" + e.x() + " " + e.y() + " " + e.z() + ")");
            shown++;
        }
        if (shown == 0) {
            player.sendMessage("§7Keine Einträge.");
        }
    }

    /** /claim rollback <Spieler> [Minuten] */
    void rollback(Player sender, String targetName, int minutes) {
        boolean admin = sender.hasPermission("duelnetwork.admin");
        Claim here = this.claims.find(sender.getLocation());
        if (!admin && (here == null || !here.owner.equals(sender.getUniqueId()))) {
            sender.sendMessage("§cStell dich in deinen eigenen Claim (Admins dürfen überall).");
            return;
        }
        org.bukkit.OfflinePlayer target = this.plugin.getServer().getOfflinePlayer(targetName);
        if (!target.isOnline() && !target.hasPlayedBefore()) {
            sender.sendMessage("§cSpieler nicht gefunden.");
            return;
        }
        long since = System.currentTimeMillis() - Math.max(1, Math.min(4320, minutes)) * 60000L;
        List<Entry> todo = new ArrayList<Entry>();
        synchronized (this) {
            java.util.Iterator<Entry> it = this.entries.descendingIterator();
            while (it.hasNext() && todo.size() < 20000) {
                Entry e = it.next();
                if (e.ts() < since) {
                    break;
                }
                if (e.player().equals(target.getUniqueId()) && (admin || e.claimId() == here.id)) {
                    todo.add(e);
                }
            }
        }
        if (todo.isEmpty()) {
            sender.sendMessage("§7Keine Änderungen von §f" + targetName + " §7in den letzten " + minutes + " Minuten.");
            return;
        }
        sender.sendMessage("§aStelle " + todo.size() + " Blöcke von §f" + targetName + " §awieder her …");
        final Queue<Entry> queue = new ArrayDeque<Entry>(todo);
        new BukkitRunnable() {
            int restored;
            @Override
            public void run() {
                for (int i = 0; i < 400 && !queue.isEmpty(); i++) {
                    Entry e = queue.poll();
                    World world = Bukkit.getWorld(e.world());
                    if (world == null || !world.isChunkLoaded(e.x() >> 4, e.z() >> 4)) {
                        continue;
                    }
                    try {
                        world.getBlockAt(e.x(), e.y(), e.z()).setBlockData(Bukkit.createBlockData(e.before()), false);
                        this.restored++;
                    }
                    catch (IllegalArgumentException ignored) {
                        // Blockdaten ungueltig
                    }
                }
                if (queue.isEmpty()) {
                    sender.sendMessage("§aFertig: §f" + this.restored + " §aBlöcke wiederhergestellt. §7(Kisteninhalte nicht.)");
                    this.cancel();
                }
            }
        }.runTaskTimer((Plugin)ClaimLog.this.plugin, 1L, 1L);
    }
}
