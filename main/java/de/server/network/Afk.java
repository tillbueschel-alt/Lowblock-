package de.server.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * AFK-Anzeige (nur SMP und Bau-SMP): nach afk.minutes (Standard 5) ohne Bewegung, Chat oder Klick steht "[AFK]" in der Tab-Liste.
 * Einkommen und Gem-Zeit zaehlt weiterhin nicht (Economy.afk). Wer AFK ist, zaehlt bei der Schlaf-Abstimmung nicht mit.
 */
final class Afk implements Listener {
    static Afk instance;

    private final DuelNetworkPlugin plugin;
    private final Map<UUID, Long> last = new HashMap<UUID, Long>();
    private final Map<UUID, Boolean> marked = new HashMap<UUID, Boolean>();

    Afk(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("Afk", this::tick), 100L, 100L);
    }

    static boolean isAfk(Player player) {
        return instance != null && Boolean.TRUE.equals(instance.marked.get(player.getUniqueId()));
    }

    private void touch(Player player) {
        UUID id = player.getUniqueId();
        this.last.put(id, System.currentTimeMillis());
        if (Boolean.TRUE.equals(this.marked.remove(id))) {
            player.setPlayerListName(null);
            this.plugin.applyRankDisplay(player);
            player.sendMessage("§7Du bist nicht mehr AFK.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() != null && (event.getFrom().getBlockX() != event.getTo().getBlockX() || event.getFrom().getBlockZ() != event.getTo().getBlockZ()
                || event.getFrom().getYaw() != event.getTo().getYaw())) {
            this.touch(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        this.touch(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> this.touch(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.last.remove(event.getPlayer().getUniqueId());
        this.marked.remove(event.getPlayer().getUniqueId());
    }

    private void tick() {
        long limit = Math.max(1L, this.plugin.getConfig().getLong("afk.minutes", 5L)) * 60000L;
        long now = System.currentTimeMillis();
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            Long seen = this.last.putIfAbsent(id, now);
            if (!Zone.survival(this.plugin, player)) {
                if (Boolean.TRUE.equals(this.marked.remove(id))) {
                    player.setPlayerListName(null);
                    this.plugin.applyRankDisplay(player);
                }
                this.last.put(id, now);
                continue;
            }
            if (seen != null && now - seen > limit) {
                if (!Boolean.TRUE.equals(this.marked.get(id))) {
                    this.marked.put(id, Boolean.TRUE);
                    player.sendMessage("§7Du bist jetzt AFK.");
                }
                player.setPlayerListName("§7[AFK] " + player.getName());
            }
        }
    }
}
