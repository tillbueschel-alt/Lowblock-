package de.server.network;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.plugin.Plugin;

/**
 * Schlaf-Abstimmung (SMP und Bau-SMP): schlaeft der Anteil sleep.percent (Standard 30 %) der aktiven Spieler dieser Welt/dieses Modus,
 * wird es nach kurzer Zeit Tag und das Wetter klar. AFK-Spieler zaehlen nicht mit. Vanilla (alle muessen schlafen) wird damit ersetzt.
 */
final class Sleep implements Listener {
    private final DuelNetworkPlugin plugin;
    private final java.util.Set<String> pending = new java.util.HashSet<String>();

    Sleep(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBed(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK || !Zone.survival(this.plugin, event.getPlayer())) {
            return;
        }
        Player sleeper = event.getPlayer();
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> this.check(sleeper), 5L);
    }

    /** Steht jemand auf, aktualisiert sich die Anzeige fuer die anderen sofort. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(PlayerBedLeaveEvent event) {
        Player p = event.getPlayer();
        if (!Zone.survival(this.plugin, p)) {
            return;
        }
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> this.check(p), 2L);
    }

    /** Zaehlt aktive Schlafende der Gruppe (gleiche Welt, gleicher Modus, kein AFK/Spectator). Liefert {schlafend, benoetigt}. */
    private int[] count(World world, String zone, List<Player> group) {
        int sleeping = 0;
        for (Player p : world.getPlayers()) {
            if (!Zone.survival(this.plugin, p) || !Zone.key(this.plugin, p).equals(zone) || p.getGameMode() == GameMode.SPECTATOR || Afk.isAfk(p)) {
                continue;
            }
            group.add(p);
            if (p.isSleeping()) {
                sleeping++;
            }
        }
        int percent = Math.max(1, Math.min(100, this.plugin.getConfig().getInt("sleep.percent", 30)));
        int needed = Math.max(1, (int)Math.ceil(group.size() * percent / 100.0));
        return new int[]{sleeping, needed};
    }

    private void check(Player sleeper) {
        if (!sleeper.isOnline()) {
            return;
        }
        World world = sleeper.getWorld();
        String zone = Zone.key(this.plugin, sleeper);
        List<Player> group = new ArrayList<Player>();
        int[] c = this.count(world, zone, group);
        int sleeping = c[0];
        int needed = c[1];
        String barKey = world.getName() + ":" + zone;
        if (sleeping > 0) {
            if (this.plugin.getConfig().getBoolean("sleep.bossbar", true)) {
                SleepBar.show(barKey, group, sleeping, needed, "");
            } else {
                for (Player p : group) {
                    p.sendActionBar(net.kyori.adventure.text.Component.text("§e☾ §f" + sleeping + "/" + needed + " §7schlafen"));
                }
            }
        } else {
            SleepBar.hide(barKey);
        }
        if (sleeping >= needed && this.pending.add(world.getName() + zone)) {
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                this.pending.remove(world.getName() + zone);
                // Noch genug Schlafende? (jemand kann in der Wartezeit aufgestanden sein)
                List<Player> g2 = new ArrayList<Player>();
                int[] c2 = this.count(world, zone, g2);
                if (c2[0] < c2[1]) {
                    return;
                }
                this.skip(world, zone, g2);
            }, group.size() <= 1 ? 10L : 40L);
        }
    }

    /** Nacht ueberspringen: weich beschleunigt (nur wenn Bau-SMP nicht in dieser Welt mitlaeuft), sonst sofort. */
    private void skip(World world, String zone, List<Player> group) {
        boolean smooth = this.plugin.getConfig().getBoolean("sleep.smooth", true) && world.getTime() >= 12000L && (WorldTime.instance == null || WorldTime.instance.beginSkip(world) < 0L);
        if (smooth) {
            long start = world.getTime();
            long target = 24000L; // bis zum naechsten Morgen
            long span = Math.max(1L, target - start);
            int steps = 30; // 1,5 Sekunden
            final long per = Math.max(1L, span / steps);
            org.bukkit.scheduler.BukkitTask[] holder = new org.bukkit.scheduler.BukkitTask[1];
            holder[0] = this.plugin.getServer().getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
                long next = world.getTime() + per;
                if (next >= target || world.getTime() < start) {
                    holder[0].cancel();
                    world.setTime(0L);
                    this.afterSkip(world, zone);
                    return;
                }
                world.setTime(next);
            }, 1L, 1L);
            return;
        }
        long bauBefore = WorldTime.instance == null ? -1L : WorldTime.instance.beginSkip(world);
        world.setTime(0L);
        if (WorldTime.instance != null) {
            WorldTime.instance.finishSkip(world, bauBefore);
        }
        this.afterSkip(world, zone);
    }

    private void afterSkip(World world, String zone) {
        SleepBar.hide(world.getName() + ":" + zone);
        int gems = this.plugin.getConfig().getInt("sleep.reward-gems", 0);
        world.setStorm(false);
        world.setThundering(false);
        for (Player p : world.getPlayers()) {
            if (Zone.survival(this.plugin, p) && Zone.key(this.plugin, p).equals(zone)) {
                try {
                    p.setStatistic(org.bukkit.Statistic.TIME_SINCE_REST, 0); // keine Phantome nach dem Schlafen
                }
                catch (Throwable ignored) {
                    // Statistik nicht verfuegbar
                }
                boolean slept = p.isSleeping();
                p.sendMessage("§e☀ §7Die Nacht wurde übersprungen." + (slept && gems > 0 ? " §8(+" + gems + " Gems fürs Schlafen)" : ""));
                if (slept) {
                    p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.REGENERATION, 100, 0));
                    if (gems > 0) {
                        Gems.give(p.getUniqueId(), gems);
                    }
                }
            }
        }
    }
}
