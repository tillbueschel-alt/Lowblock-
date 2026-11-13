package de.server.network;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Warnt Admins (Permission duelnetwork.admin), wenn die TPS dreimal in Folge (alle 30 s) unter lag.warn-tps liegen,
 * und nennt die drei langsamsten Plugin-Aufgaben. Hoechstens eine Warnung alle 10 Minuten.
 */
final class LagWatch {
    private final DuelNetworkPlugin plugin;
    private int low;
    private long lastWarn;

    LagWatch(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("LagWatch", this::check), 600L, 600L);
    }

    private void check() {
        double limit = this.plugin.getConfig().getDouble("lag.warn-tps", 15.0);
        if (!this.plugin.getConfig().getBoolean("lag.enabled", true) || limit <= 0.0) {
            return;
        }
        double tps = 20.0;
        try {
            double[] t = this.plugin.getServer().getTPS();
            if (t != null && t.length > 0) {
                tps = t[0];
            }
        }
        catch (Throwable ex) {
            return;
        }
        this.low = tps < limit ? this.low + 1 : 0;
        if (this.low < 3 || System.currentTimeMillis() - this.lastWarn < 600_000L) {
            return;
        }
        this.lastWarn = System.currentTimeMillis();
        this.low = 0;
        for (Player p : this.plugin.getServer().getOnlinePlayers()) {
            if (p.hasPermission("duelnetwork.admin")) {
                p.sendMessage("§e⚠ Lag: TPS seit ~1,5 Min. bei §f" + String.format(java.util.Locale.ROOT, "%.1f", Math.min(20.0, tps)) + " §7(Grenze " + limit + "). Langsamste Plugin-Aufgaben:");
                for (String line : Perf.report(3)) {
                    p.sendMessage(line);
                }
            }
        }
        this.plugin.getLogger().warning("Lag: TPS " + String.format(java.util.Locale.ROOT, "%.1f", tps));
    }
}
