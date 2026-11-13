package de.server.network;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class ClaimProtectionListener implements Listener {
    private final ClaimManager claims;
    private final Map<UUID, Long> messages = new ConcurrentHashMap<>();

    ClaimProtectionListener(ClaimManager claims) { this.claims = claims; }

    private void deny(Player player, ClaimManager.Action action, Location where) {
        if (player == null) return;
        long now = System.currentTimeMillis();
        Long last = messages.put(player.getUniqueId(), now);
        if (last == null || now - last > 1800L) player.sendMessage(claims.denyMessage(action, claims.find(where)));
    }

    private boolean block(Player player, Location location, ClaimManager.Action action) {
        if (claims.allowed(player, location, action)) return false;
        deny(player, action, location);
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) { if (block(event.getPlayer(), event.getBlock().getLocation(), ClaimManager.Action.BUILD)) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) { if (block(event.getPlayer(), event.getBlock().getLocation(), ClaimManager.Action.BUILD)) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        ClaimManager.Action action = event.getAction().name().equals("PHYSICAL") ? ClaimManager.Action.BUILD : ClaimManager.Action.INTERACT;
        if (block(event.getPlayer(), event.getClickedBlock().getLocation(), action)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onContainer(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player) || event.getInventory().getLocation() == null) return;
        if (block(player, event.getInventory().getLocation(), ClaimManager.Action.CONTAINER)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) { if (block(event.getPlayer(), event.getBlock().getLocation(), ClaimManager.Action.BUILD)) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) { if (block(event.getPlayer(), event.getBlock().getLocation(), ClaimManager.Action.BUILD)) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) { if (event.getPlayer() != null && block(event.getPlayer(), event.getBlock().getLocation(), ClaimManager.Action.BUILD)) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) { if (block(event.getPlayer(), event.getEntity().getLocation(), ClaimManager.Action.BUILD)) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Player player = player(event.getRemover());
        if (block(player, event.getEntity().getLocation(), ClaimManager.Action.BUILD)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) { if (block(event.getPlayer(), event.getRightClicked().getLocation(), ClaimManager.Action.BUILD)) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity instanceof Player) return;
        if (block(event.getPlayer(), entity.getLocation(), ClaimManager.Action.INTERACT)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = player(event.getDamager());
        if (attacker != null && !claims.allowed(victim, victim.getLocation(), ClaimManager.Action.PVP)) {
            event.setCancelled(true);
            deny(attacker, ClaimManager.Action.PVP, victim.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) { event.blockList().removeIf(block -> claims.find(block.getLocation()) != null && claims.protectedExplosions()); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) { event.blockList().removeIf(block -> claims.find(block.getLocation()) != null && claims.protectedExplosions()); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) { if (claims.find(event.getBlock().getLocation()) != null) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) { if (event.getBlocks().stream().anyMatch(block -> claims.find(block.getLocation()) != null)) event.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) { if (event.getBlocks().stream().anyMatch(block -> claims.find(block.getLocation()) != null)) event.setCancelled(true); }

    private Player player(Entity entity) {
        if (entity instanceof Player p) return p;
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player p) return p;
        return null;
    }
}
