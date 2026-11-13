package de.server.network;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Bossbar "x/y schlafen" pro Welt+Modus; nur die mitzaehlenden Spieler sehen sie. */
final class SleepBar {
    private static final Map<String, BossBar> BARS = new HashMap<String, BossBar>();
    private static final Map<String, Set<UUID>> VIEWERS = new HashMap<String, Set<UUID>>();

    private SleepBar() {
    }

    static void show(String key, Collection<Player> group, int sleeping, int needed, String label) {
        BossBar bar = BARS.get(key);
        Component title = Component.text("§e☾ §f" + sleeping + "/" + needed + " §7schlafen §8" + label);
        float progress = Math.max(0.0f, Math.min(1.0f, needed <= 0 ? 0.0f : (float)sleeping / (float)needed));
        if (bar == null) {
            bar = BossBar.bossBar(title, progress, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);
            BARS.put(key, bar);
            VIEWERS.put(key, new HashSet<UUID>());
        } else {
            bar.name(title);
            bar.progress(progress);
        }
        bar.color(sleeping >= needed ? BossBar.Color.GREEN : BossBar.Color.BLUE);
        Set<UUID> seen = VIEWERS.get(key);
        Set<UUID> now = new HashSet<UUID>();
        for (Player p : group) {
            now.add(p.getUniqueId());
            if (seen.add(p.getUniqueId())) {
                p.showBossBar(bar);
            }
        }
        for (UUID id : new HashSet<UUID>(seen)) {
            if (!now.contains(id)) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    p.hideBossBar(bar);
                }
                seen.remove(id);
            }
        }
    }

    static void hide(String key) {
        BossBar bar = BARS.remove(key);
        Set<UUID> seen = VIEWERS.remove(key);
        if (bar == null || seen == null) {
            return;
        }
        for (UUID id : seen) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.hideBossBar(bar);
            }
        }
    }
}
