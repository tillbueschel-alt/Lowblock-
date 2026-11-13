/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.entity.ArmorStand
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Hanging
 *  org.bukkit.entity.ItemFrame
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockIgniteEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.hanging.HangingBreakByEntityEvent
 *  org.bukkit.event.hanging.HangingPlaceEvent
 *  org.bukkit.event.player.PlayerArmorStandManipulateEvent
 *  org.bukkit.event.player.PlayerBucketEmptyEvent
 *  org.bukkit.event.player.PlayerBucketFillEvent
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.projectiles.ProjectileSource
 */
package de.server.network;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

public final class ProtectionListener
implements Listener {
    private final DuelNetworkPlugin plugin;
    private final Map<UUID, String> profiles;
    private final Set<UUID> activeDuelPlayers;
    private final Map<UUID, String> activePartyMatches;
    private final Map<UUID, String> activeParkourMaps;
    private final Map<UUID, Long> messageCooldown = new ConcurrentHashMap<UUID, Long>();
    private static final String NO_BUILD = "\u00a7cHier kannst du nichts abbauen oder platzieren.";
    private static final String NO_PARKOUR_BUILD = "\u00a7cDu kannst im Parkour nicht bauen oder abbauen.";
    private static final String NO_PVP = "\u00a7cPvP ist hier nicht erlaubt (nur in Duel-Arenen und im SMP).";
    private static final String NO_PUSH = "\u00a7cDu kannst keine anderen Spieler im Parkour ansto\u00dfen.";

    public ProtectionListener(DuelNetworkPlugin javaPlugin) {
        this.plugin = javaPlugin;
        this.profiles = javaPlugin.profiles;
        this.activeDuelPlayers = javaPlugin.activeDuelPlayers;
        this.activePartyMatches = javaPlugin.activePartyMatches;
        this.activeParkourMaps = javaPlugin.activeParkourMaps;
    }

    private boolean inDuel(Player player) {
        UUID uUID = player.getUniqueId();
        return this.activeDuelPlayers.contains(uUID) || this.activePartyMatches.containsKey(uUID);
    }

    private boolean inParkour(Player player) {
        return this.activeParkourMaps.containsKey(player.getUniqueId());
    }

    private boolean inLobby(Player player, Location location) {
        try {
            return this.plugin.lobbyProtected(player, location);
        }
        catch (Exception exception) {
            return false;
        }
    }

    private boolean inSmp(Player player, Location location) {
        try {
            if (!"smp".equals(this.profiles.getOrDefault(player.getUniqueId(), "lobby"))) {
                return false;
            }
            if (location == null || location.getWorld() == null) {
                return false;
            }
            // Bau-SMP ist eine eigene, geschützte Zone: Dort darf normal gebaut
            // und abgebaut werden; Claims sperren nur fremde Claims zusätzlich.
            if (this.plugin.inBauMode(player.getUniqueId()) && this.plugin.insideBauBorder(location)) {
                return true;
            }
            if (this.inLobby(player, location)) {
                return false;
            }
            Location location2 = this.plugin.readConfiguredLocation("locations.spawns.smp.default");
            if (location2 == null || location2.getWorld() == null) {
                return true;
            }
            if (!location2.getWorld().equals((Object)location.getWorld())) {
                return true;
            }
            return this.plugin.isInsideSmpBorder(location);
        }
        catch (Exception exception) {
            return false;
        }
    }

    private boolean mayBuild(Player player, Location location) {
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        if (this.inParkour(player)) {
            return false;
        }
        if (this.inDuel(player)) {
            return this.plugin.insideDuelArena(player, location);
        }
        return this.inSmp(player, location);
    }

    private boolean buildRestricted() {
        return this.plugin.getConfig().getBoolean("protection.restrict-building", true);
    }

    private boolean pvpRestricted() {
        return this.plugin.getConfig().getBoolean("protection.restrict-pvp", true);
    }

    private void deny(Player player, String string) {
        long l = System.currentTimeMillis();
        Long l2 = this.messageCooldown.get(player.getUniqueId());
        if (l2 == null || l - l2 > 2000L) {
            this.messageCooldown.put(player.getUniqueId(), l);
            player.sendMessage(string);
        }
    }

    private boolean forbidden(Player player, Location location) {
        return this.buildRestricted() && !this.mayBuild(player, location);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPvp(EntityDamageByEntityEvent entityDamageByEntityEvent) {
        boolean bl;
        ProjectileSource projectileSource;
        Entity entity = entityDamageByEntityEvent.getEntity();
        Entity entity2 = entityDamageByEntityEvent.getDamager();
        Player player = null;
        if (entity2 instanceof Player) {
            player = (Player)entity2;
        } else if (entity2 instanceof Projectile && ((Projectile)entity2).getShooter() instanceof Player) {
            player = (Player)((Projectile)entity2).getShooter();
        }
        if (player == null) {
            return;
        }
        if (entity instanceof Hanging || entity instanceof ArmorStand) {
            if (this.forbidden(player, entity.getLocation())) {
                entityDamageByEntityEvent.setCancelled(true);
                this.deny(player, NO_BUILD);
            }
            return;
        }
        if (!(entity instanceof Player)) {
            return;
        }
        Player victimPlayer = (Player)entity;
        if (victimPlayer.getUniqueId().equals(player.getUniqueId())) {
            return;
        }
        if (!this.pvpRestricted()) {
            return;
        }
        boolean bl2 = this.inDuel(player) && this.inDuel(victimPlayer);
        boolean bl3 = bl = this.inSmp(player, player.getLocation()) && this.inSmp(victimPlayer, victimPlayer.getLocation());
        if (!bl2 && !bl) {
            entityDamageByEntityEvent.setCancelled(true);
            this.deny(player, NO_PVP);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onParkourPush(EntityDamageByEntityEvent entityDamageByEntityEvent) {
        Entity entity = entityDamageByEntityEvent.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        if (this.inParkour(player)) {
            entityDamageByEntityEvent.setCancelled(true);
            this.deny(player, NO_PUSH);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent blockBreakEvent) {
        Player player = blockBreakEvent.getPlayer();
        if (player != null && this.forbidden(player, blockBreakEvent.getBlock().getLocation())) {
            blockBreakEvent.setCancelled(true);
            this.deny(player, this.inParkour(player) ? NO_PARKOUR_BUILD : NO_BUILD);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent blockPlaceEvent) {
        Player player = blockPlaceEvent.getPlayer();
        if (player != null && this.forbidden(player, blockPlaceEvent.getBlock().getLocation())) {
            blockPlaceEvent.setCancelled(true);
            this.deny(player, this.inParkour(player) ? NO_PARKOUR_BUILD : NO_BUILD);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent playerBucketEmptyEvent) {
        Player player = playerBucketEmptyEvent.getPlayer();
        if (player != null && this.forbidden(player, player.getLocation())) {
            playerBucketEmptyEvent.setCancelled(true);
            this.deny(player, this.inParkour(player) ? NO_PARKOUR_BUILD : NO_BUILD);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onBucketFill(PlayerBucketFillEvent playerBucketFillEvent) {
        Player player = playerBucketFillEvent.getPlayer();
        if (player != null && this.forbidden(player, player.getLocation())) {
            playerBucketFillEvent.setCancelled(true);
            this.deny(player, this.inParkour(player) ? NO_PARKOUR_BUILD : NO_BUILD);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onHangingPlace(HangingPlaceEvent hangingPlaceEvent) {
        Player player = hangingPlaceEvent.getPlayer();
        if (player != null && this.forbidden(player, player.getLocation())) {
            hangingPlaceEvent.setCancelled(true);
            this.deny(player, this.inParkour(player) ? NO_PARKOUR_BUILD : NO_BUILD);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onHangingBreak(HangingBreakByEntityEvent hangingBreakByEntityEvent) {
        ProjectileSource projectileSource;
        Entity entity = hangingBreakByEntityEvent.getRemover();
        Player player = null;
        if (entity instanceof Player) {
            player = (Player)entity;
        } else if (entity instanceof Projectile && (projectileSource = ((Projectile)entity).getShooter()) instanceof Player) {
            player = (Player)projectileSource;
        }
        if (player != null && this.forbidden(player, player.getLocation())) {
            hangingBreakByEntityEvent.setCancelled(true);
            this.deny(player, this.inParkour(player) ? NO_PARKOUR_BUILD : NO_BUILD);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onArmorStand(PlayerArmorStandManipulateEvent playerArmorStandManipulateEvent) {
        Player player = playerArmorStandManipulateEvent.getPlayer();
        if (player != null && this.forbidden(player, player.getLocation())) {
            playerArmorStandManipulateEvent.setCancelled(true);
            this.deny(player, this.inParkour(player) ? NO_PARKOUR_BUILD : NO_BUILD);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onIgnite(BlockIgniteEvent blockIgniteEvent) {
        Player player = blockIgniteEvent.getPlayer();
        if (player != null && this.forbidden(player, player.getLocation())) {
            blockIgniteEvent.setCancelled(true);
            this.deny(player, this.inParkour(player) ? NO_PARKOUR_BUILD : NO_BUILD);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onFrameInteract(PlayerInteractEntityEvent playerInteractEntityEvent) {
        Entity entity = playerInteractEntityEvent.getRightClicked();
        if (!(entity instanceof ItemFrame)) {
            return;
        }
        Player player = playerInteractEntityEvent.getPlayer();
        if (player != null && this.forbidden(player, player.getLocation())) {
            playerInteractEntityEvent.setCancelled(true);
            this.deny(player, this.inParkour(player) ? NO_PARKOUR_BUILD : NO_BUILD);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onTrample(PlayerInteractEvent playerInteractEvent) {
        if (playerInteractEvent.getAction() != Action.PHYSICAL || playerInteractEvent.getClickedBlock() == null) {
            return;
        }
        Material material = playerInteractEvent.getClickedBlock().getType();
        if (material != Material.FARMLAND && material != Material.TURTLE_EGG) {
            return;
        }
        Player player = playerInteractEvent.getPlayer();
        if (player != null && this.forbidden(player, player.getLocation())) {
            playerInteractEvent.setCancelled(true);
        }
    }
}
