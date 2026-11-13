package de.server.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Duell-Extras: (1) Kampfuebersicht nach dem Tod (Schaden, Treffer, Dauer), (2) Sudden Death: nach duel.sudden-death-minutes (5, 0 = aus)
 * verlieren alle Kaempfer alle 2 Sekunden ein halbes Herz, (3) Trainings-Dummy-NPC ("dummy"): Treffer werden mit Schaden und DPS angezeigt.
 */
final class DuelExtras implements Listener {
    private static final class Stat {
        double dealt;
        double taken;
        int hitsDealt;
        int hitsTaken;
        long first = System.currentTimeMillis();
    }

    private final DuelNetworkPlugin plugin;
    private final Map<UUID, Stat> stats = new HashMap<UUID, Stat>();
    private final Map<UUID, Long> started = new HashMap<UUID, Long>();
    private final Set<UUID> announced = new HashSet<UUID>();
    private final Map<UUID, double[]> dummy = new HashMap<UUID, double[]>();

    DuelExtras(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("SuddenDeath", this::tick), 40L, 40L);
    }

    private boolean fighting(UUID id) {
        return this.plugin.activeDuelPlayers.contains(id) || this.plugin.activePartyMatches.containsKey(id);
    }

    private static Player shooter(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player p) {
            return p;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        Player attacker = shooter(event.getDamager());
        if (attacker == null) {
            return;
        }
        Entity victim = event.getEntity();
        String type = (String)victim.getPersistentDataContainer().get(this.plugin.npcKey, PersistentDataType.STRING);
        if ("dummy".equals(type)) {
            this.dummyHit(attacker, event.getFinalDamage());
            return;
        }
        if (event.isCancelled() || !(victim instanceof Player target) || !this.fighting(attacker.getUniqueId()) || !this.fighting(target.getUniqueId())) {
            return;
        }
        double damage = event.getFinalDamage();
        Stat a = this.stats.computeIfAbsent(attacker.getUniqueId(), k -> new Stat());
        a.dealt += damage;
        a.hitsDealt++;
        Stat t = this.stats.computeIfAbsent(target.getUniqueId(), k -> new Stat());
        t.taken += damage;
        t.hitsTaken++;
    }

    private void dummyHit(Player player, double damage) {
        if (!Double.isFinite(damage) || damage <= 0.0) return;
        long now = System.currentTimeMillis();
        double[] d = this.dummy.get(player.getUniqueId());
        if (d == null || now - (long)d[0] > 10000L) {
            d = new double[]{now, 0, 0};
            this.dummy.put(player.getUniqueId(), d);
        }
        d[1] += 1;
        d[2] += damage;
        double seconds = Math.max(1.0, (now - d[0]) / 1000.0);
        String hit = String.format(java.util.Locale.ROOT, "−%.1f HP / %.1f❤", damage, damage / 2.0);
        String total = String.format(java.util.Locale.ROOT, "Gesamt %.1f HP", d[2]);
        String dps = String.format(java.util.Locale.ROOT, "%.1f DPS", d[2] / seconds);
        Component bar = Component.text(hit, NamedTextColor.RED)
                .append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Treffer " + (int)d[1], NamedTextColor.GRAY))
                .append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
                .append(Component.text(total, NamedTextColor.WHITE))
                .append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
                .append(Component.text(dps, NamedTextColor.GOLD));
        player.sendActionBar(bar);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Stat v = this.stats.remove(victim.getUniqueId());
        if (v == null) {
            return;
        }
        this.recap(victim, v);
        Player killer = victim.getKiller();
        if (killer != null) {
            Stat k = this.stats.remove(killer.getUniqueId());
            if (k != null) {
                this.recap(killer, k);
            }
        }
    }

    private void recap(Player player, Stat s) {
        long seconds = Math.max(1L, (System.currentTimeMillis() - s.first) / 1000L);
        player.sendMessage("§d⚔ §fKampfübersicht §8(" + seconds + " s)");
        player.sendMessage("§7Du hast ausgeteilt: §a" + String.format(java.util.Locale.ROOT, "%.1f", s.dealt / 2.0) + " ❤ §7in §f" + s.hitsDealt + " Treffern");
        player.sendMessage("§7Du hast eingesteckt: §c" + String.format(java.util.Locale.ROOT, "%.1f", s.taken / 2.0) + " ❤ §7in §f" + s.hitsTaken + " Treffern");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        this.stats.remove(id);
        this.started.remove(id);
        this.announced.remove(id);
        this.dummy.remove(id);
    }

    /** Alle 2 Sekunden: Sudden Death und Aufraeumen. */
    private void tick() {
        long limit = Math.max(0L, this.plugin.getConfig().getLong("duel.sudden-death-minutes", 5L)) * 60000L;
        long now = System.currentTimeMillis();
        this.stats.keySet().removeIf(id -> !this.fighting(id));
        this.started.keySet().removeIf(id -> !this.fighting(id));
        this.announced.removeIf(id -> !this.fighting(id));
        if (limit <= 0L) {
            return;
        }
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            if (!this.fighting(id) || player.isDead()) {
                continue;
            }
            long start = this.started.computeIfAbsent(id, k -> now);
            if (now - start < limit) {
                continue;
            }
            if (this.announced.add(id)) {
                player.sendTitle("§c§lSUDDEN DEATH", "§7Das Duell dauert zu lange – alle verlieren Leben!", 5, 50, 10);
            }
            double health = player.getHealth();
            if (health <= 1.0) {
                player.damage(100.0);
            } else {
                player.setHealth(health - 1.0);
            }
        }
    }
}
