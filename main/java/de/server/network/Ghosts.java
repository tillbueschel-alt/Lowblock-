package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Parkour-Geister: Waehrend eines Parkour-Laufs wird alle 4 Ticks die Position aufgezeichnet. Beim Ziel wird der Lauf als
 * persoenliche Bestzeit (und, wenn er schneller als alles bisherige ist, als Rekord-Geist) gespeichert. Beim naechsten
 * Start laufen beide Geister mit (nur fuer den Spieler sichtbar): ein blauer "Deine Bestzeit" und ein goldener "Rekord".
 */
final class Ghosts implements Listener {
    static Ghosts instance;

    private static final int STEP_TICKS = 4;
    private static final int MAX_SAMPLES = 3000;

    private static final class Run {
        final String map;
        final List<double[]> samples = new ArrayList<double[]>();
        int taskId;
        int tick;
        final List<Ghost> ghosts = new ArrayList<Ghost>();

        Run(String map) {
            this.map = map;
        }
    }

    private static final class Ghost {
        final List<double[]> path;
        final String label;
        final Particle.DustOptions dust;
        TextDisplay display;

        Ghost(List<double[]> path, String label, Particle.DustOptions dust) {
            this.path = path;
            this.label = label;
            this.dust = dust;
        }
    }

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<UUID, Run> runs = new HashMap<UUID, Run>();

    Ghosts(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.file = new File(plugin.getDataFolder(), "ghosts.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("ghosts.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    /* ---------- Aufzeichnung ---------- */

    static void begin(Player player, String map) {
        if (instance != null && player != null && map != null) {
            try {
                instance.start(player, map);
            }
            catch (Throwable t) {
                Store.problem("Ghosts:begin", t);
            }
        }
    }

    /** Ziel erreicht: Lauf speichern, falls Bestzeit. */
    static void finish(Player player, String map, long elapsedMs) {
        if (instance != null && player != null) {
            try {
                instance.complete(player, map, elapsedMs);
            }
            catch (Throwable t) {
                Store.problem("Ghosts:finish", t);
            }
        }
    }

    /** Lauf beendet oder abgebrochen: Aufzeichnung und Geister entfernen. */
    static void stop(UUID id) {
        if (instance != null) {
            instance.end(id);
        }
    }

    private static String encode(List<double[]> samples) {
        StringBuilder sb = new StringBuilder();
        for (double[] s : samples) {
            sb.append(String.format(java.util.Locale.ROOT, "%.1f,%.1f,%.1f;", s[0], s[1], s[2]));
        }
        return sb.toString();
    }

    private static List<double[]> decode(String raw) {
        List<double[]> out = new ArrayList<double[]>();
        if (raw == null || raw.isEmpty()) {
            return out;
        }
        for (String part : raw.split(";")) {
            String[] xyz = part.split(",");
            if (xyz.length == 3) {
                try {
                    out.add(new double[]{Double.parseDouble(xyz[0]), Double.parseDouble(xyz[1]), Double.parseDouble(xyz[2])});
                }
                catch (NumberFormatException ignored) {
                    // kaputter Punkt
                }
            }
        }
        return out;
    }

    private void start(Player player, String map) {
        this.end(player.getUniqueId());
        UUID id = player.getUniqueId();
        Run run = new Run(map);
        boolean showGhosts = this.runs.size() < Math.max(1, this.plugin.getConfig().getInt("ghosts.max-active", 12));
        World world = player.getWorld();
        List<double[]> mine = decode(this.data.getString("maps." + map + ".players." + id + ".path"));
        List<double[]> record = decode(this.data.getString("maps." + map + ".record.path"));
        boolean sameRun = this.data.getString("maps." + map + ".record.owner", "").equals(id.toString());
        if (!showGhosts) {
            mine.clear();
            record.clear();
        }
        if (!mine.isEmpty()) {
            run.ghosts.add(new Ghost(mine, "§b👻 Deine Bestzeit", new Particle.DustOptions(org.bukkit.Color.AQUA, 1.2f)));
        }
        if (!record.isEmpty() && !sameRun) {
            run.ghosts.add(new Ghost(record, "§6👻 Rekord: " + this.data.getString("maps." + map + ".record.name", "?"), new Particle.DustOptions(org.bukkit.Color.YELLOW, 1.2f)));
        }
        for (Ghost ghost : run.ghosts) {
            double[] first = ghost.path.get(0);
            ghost.display = world.spawn(new Location(world, first[0], first[1] + 2.1, first[2]), TextDisplay.class, e -> {
                e.setVisibleByDefault(false);
                e.setBillboard(Display.Billboard.CENTER);
                e.text(Component.text(ghost.label));
                e.setSeeThrough(true);
            });
            player.showEntity((Plugin)this.plugin, ghost.display);
        }
        this.runs.put(id, run);
        run.taskId = this.plugin.getServer().getScheduler().runTaskTimer((Plugin)this.plugin, Perf.wrap("Ghosts", () -> this.tick(player, run)), STEP_TICKS, STEP_TICKS).getTaskId();
    }

    private void tick(Player player, Run run) {
        if (!player.isOnline() || this.runs.get(player.getUniqueId()) != run) {
            this.end(player.getUniqueId());
            return;
        }
        Location loc = player.getLocation();
        if (run.samples.size() < MAX_SAMPLES) {
            run.samples.add(new double[]{loc.getX(), loc.getY(), loc.getZ()});
        }
        int index = run.tick++;
        for (Ghost ghost : run.ghosts) {
            if (ghost.display == null || ghost.display.isDead()) {
                continue;
            }
            if (index >= ghost.path.size()) {
                ghost.display.remove();
                continue;
            }
            double[] p = ghost.path.get(index);
            Location at = new Location(loc.getWorld(), p[0], p[1], p[2]);
            ghost.display.teleport(at.clone().add(0.0, 2.1, 0.0));
            if (this.plugin.getConfig().getBoolean("performance.particles", true)) {
                player.spawnParticle(Particle.DUST, at.clone().add(0.0, 1.0, 0.0), 6, 0.2, 0.4, 0.2, 0.0, ghost.dust);
            }
        }
    }

    private void end(UUID id) {
        Run run = this.runs.remove(id);
        if (run == null) {
            return;
        }
        this.plugin.getServer().getScheduler().cancelTask(run.taskId);
        for (Ghost ghost : run.ghosts) {
            if (ghost.display != null) {
                ghost.display.remove();
            }
        }
    }

    private void complete(Player player, String map, long elapsedMs) {
        UUID id = player.getUniqueId();
        Run run = this.runs.get(id);
        if (run == null || !run.map.equals(map) || run.samples.size() < 5) {
            return;
        }
        String base = "maps." + map;
        boolean changed = false;
        long best = this.data.getLong(base + ".players." + id + ".time", Long.MAX_VALUE);
        if (elapsedMs < best) {
            this.data.set(base + ".players." + id + ".time", elapsedMs);
            this.data.set(base + ".players." + id + ".path", encode(run.samples));
            changed = true;
            player.sendMessage("§b👻 Neue persönliche Bestzeit! Dein Geist läuft beim nächsten Mal mit.");
        }
        long record = this.data.getLong(base + ".record.time", Long.MAX_VALUE);
        if (elapsedMs < record) {
            this.data.set(base + ".record.time", elapsedMs);
            this.data.set(base + ".record.owner", id.toString());
            this.data.set(base + ".record.name", player.getName());
            this.data.set(base + ".record.path", encode(run.samples));
            changed = true;
            player.sendMessage("§6👻 Du bist jetzt der Rekord-Geist auf dieser Strecke!");
        }
        if (changed) {
            this.save();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.end(event.getPlayer().getUniqueId());
    }

    void shutdown() {
        for (UUID id : new ArrayList<UUID>(this.runs.keySet())) {
            this.end(id);
        }
    }
}
