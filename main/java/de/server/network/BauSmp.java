package de.server.network;

import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * Bau-SMP (frueher "Creative"): Survival-Bauwelt ohne Spieler-gegen-Spieler-Schaden.
 * Mobs greifen weiter an und koennen bekaempft werden, das Inventar bleibt beim Tod erhalten.
 * Der Spawn wird wie bisher mit /duelladmin setspawn creative gesetzt.
 * Optional: bausmp.radius in der config.yml (Quadrat um den Spawn), falls die Bauwelt dieselbe Welt wie Lobby/SMP nutzt.
 */
final class BauSmp implements Listener {
    private final DuelNetworkPlugin plugin;

    BauSmp(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
    }

    /** Betritt das Bau-SMP: SMP-Inventar, Survival, Teleport zum Bau-Spawn. */
    static void enter(DuelNetworkPlugin plugin, Player player) {
        UUID id = player.getUniqueId();
        if (plugin.activeDuelPlayers.contains(id) || plugin.activePartyMatches.containsKey(id) || plugin.activeParkourMaps.containsKey(id)) {
            player.sendMessage("§cVerlasse zuerst deine laufende Runde (/lobby).");
            return;
        }
        if (!plugin.getConfig().contains("locations.spawns.creative.default.world")) {
            player.sendMessage("§eBau-SMP: Zurzeit ist noch kein Spawn eingerichtet.");
            if (player.hasPermission("duelnetwork.admin")) {
                player.sendMessage("§7Admin: §d/duelladmin setspawn creative");
            }
            return;
        }
        if (plugin.isInSmpCombat(player)) {
            player.sendMessage("§cDu bist noch im Combat - warte, bis der Timer abgelaufen ist.");
            return;
        }
        if (!plugin.profiles.getOrDefault(id, "lobby").equals("smp")) {
            plugin.switchProfile(player, "smp");
        }
        DragonEgg.dropCarried(player);
        plugin.bauTransit.add(id);
        boolean arrived;
        try {
            arrived = plugin.teleportToSpawn(player, "creative");
        }
        finally {
            plugin.bauTransit.remove(id);
        }
        if (arrived) {
            plugin.enterBauMode(player);
            player.setGameMode(GameMode.SURVIVAL);
            title(player, "bau");
        }
    }

    /** Einheitlicher Titel beim Betreten eines Bereichs. */
    static void title(Player player, String mode) {
        switch (mode) {
            case "smp":
                player.sendTitle("§a§lSMP", "§7Überleben §8• §7handeln §8• §7bauen", 5, 40, 15);
                break;
            case "bau":
                player.sendTitle("§e§lBau-SMP", "§7Frei bauen §8• §akein PvP §8• §7Inventar bleibt", 5, 50, 15);
                break;
            case "events":
                player.sendTitle("§6§lEvents", "§7Mach mit und gewinne", 5, 40, 15);
                break;
            case "parkour":
                player.sendTitle("§b§lParkour", "§7Bestzeiten: §d/rangliste parkour", 5, 40, 15);
                break;
            default:
                break;
        }
    }

    /** Bau-Schutz gilt nur fuer SMP-Spieler im Bau-Bereich - nie in Duellen, Party-Matches, Parkour oder Events. */
    private boolean protectedHere(Player player) {
        java.util.UUID id = player.getUniqueId();
        if (!"smp".equals(this.plugin.profiles.getOrDefault(id, "lobby")) || this.plugin.activeDuelPlayers.contains(id)
                || this.plugin.activePartyMatches.containsKey(id) || this.plugin.activeParkourMaps.containsKey(id) || EventGames.busy(id)) {
            return false;
        }
        return this.inZone(player.getLocation());
    }

    boolean inZone(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return this.plugin.insideBauBorder(location);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !this.protectedHere(victim)) {
            return;
        }
        Entity damager = event.getDamager();
        Player attacker = null;
        if (damager instanceof Player direct) {
            attacker = direct;
        } else if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Player shooter) {
                attacker = shooter;
            }
        }
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        event.setCancelled(true);
        attacker.sendMessage("§cIm Bau-SMP kannst du keine anderen Spieler angreifen.");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!this.protectedHere(player)) {
            return;
        }
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        event.setDroppedExp(0);
        event.getDrops().clear();
    }
}
