package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Grabstein (nur SMP und Bau-SMP): beim Tod landet das Inventar in einer Kiste am Todesort (Items darueber in einer zweiten Kiste).
 * grave.owner-only-minutes (5): nur der Besitzer kann sie oeffnen/abbauen, danach jeder. Nach grave.expire-minutes (30) loest sich
 * der Grabstein auf und die Items fallen auf den Boden. Gespeichert in graves.yml. Deaktivieren: grave.enabled: false.
 */
final class Grave implements Listener {
    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    Grave(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "graves.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("Grave", this::expire), 1200L, 1200L);
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("graves.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private static String key(Block block) {
        return block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
    }

    private static String safe(String key) {
        return key.replace('.', '_').replace(',', '_').replace(' ', '_');
    }

    /** Grab-ID zu einem Block oder null. */
    private String find(Block block) {
        String k = key(block);
        ConfigurationSection section = this.data.getConfigurationSection("graves");
        if (section == null) {
            return null;
        }
        for (String id : section.getKeys(false)) {
            if (k.equals(section.getString(id + ".a")) || k.equals(section.getString(id + ".b"))) {
                return id;
            }
        }
        return null;
    }

    private Block free(Location from) {
        Block block = from.getBlock();
        for (int up = 0; up < 4; up++) {
            Block candidate = block.getRelative(0, up, 0);
            Material type = candidate.getType();
            if (type.isAir() || type == Material.WATER || type == Material.SHORT_GRASS || type == Material.SNOW) {
                return candidate;
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!this.plugin.getConfig().getBoolean("grave.enabled", true) || !Zone.survival(this.plugin, player) || event.getKeepInventory()
                || event.getDrops().isEmpty() || player.getLocation().getBlockY() < player.getWorld().getMinHeight()) {
            return;
        }
        Block first = this.free(player.getLocation());
        if (first == null) {
            return;
        }
        List<ItemStack> items = new ArrayList<ItemStack>();
        for (ItemStack stack : event.getDrops()) {
            if (stack != null && !stack.getType().isAir()) {
                items.add(stack);
            }
        }
        if (items.isEmpty()) {
            return;
        }
        first.setType(Material.CHEST);
        Chest chest = (Chest)first.getState();
        int index = 0;
        while (index < items.size() && chest.getBlockInventory().firstEmpty() != -1) {
            chest.getBlockInventory().addItem(items.get(index++));
        }
        String second = "";
        if (index < items.size()) {
            Block above = first.getRelative(0, 1, 0);
            if (above.getType().isAir()) {
                above.setType(Material.CHEST);
                Chest chest2 = (Chest)above.getState();
                while (index < items.size() && chest2.getBlockInventory().firstEmpty() != -1) {
                    chest2.getBlockInventory().addItem(items.get(index++));
                }
                second = key(above);
            }
        }
        List<ItemStack> overflow = new ArrayList<ItemStack>(items.subList(index, items.size()));
        event.getDrops().clear();
        event.getDrops().addAll(overflow);
        String id = safe(key(first));
        this.data.set("graves." + id + ".a", key(first));
        this.data.set("graves." + id + ".b", second.isEmpty() ? null : second);
        this.data.set("graves." + id + ".owner", player.getUniqueId().toString());
        this.data.set("graves." + id + ".time", System.currentTimeMillis());
        this.save();
        long lock = this.plugin.getConfig().getLong("grave.owner-only-minutes", 5L);
        player.sendMessage("§7☠ §fGrabstein bei §d" + first.getX() + " " + first.getY() + " " + first.getZ() + " §7– nur du kannst ihn §f" + lock
                + " Minuten §7öffnen, er hält §f" + this.plugin.getConfig().getLong("grave.expire-minutes", 30L) + " Minuten§7.");
        String coords = first.getX() + " " + first.getY() + " " + first.getZ();
        player.sendMessage(net.kyori.adventure.text.Component.text("§7☠ §8Welt §7" + first.getWorld().getName() + " §8• §d[Koordinaten kopieren]")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(coords))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(net.kyori.adventure.text.Component.text("§7" + coords + " – klicken zum Kopieren"))));
    }

    private boolean locked(String id, Player player) {
        String owner = this.data.getString("graves." + id + ".owner", "");
        long lock = Math.max(0L, this.plugin.getConfig().getLong("grave.owner-only-minutes", 5L)) * 60000L;
        boolean fresh = System.currentTimeMillis() - this.data.getLong("graves." + id + ".time", 0L) < lock;
        return fresh && !owner.equals(player.getUniqueId().toString()) && !player.hasPermission("duelnetwork.admin");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !(event.getInventory().getHolder() instanceof Chest chest)) {
            return;
        }
        String id = this.find(chest.getBlock());
        if (id != null && this.locked(id, player)) {
            event.setCancelled(true);
            player.sendMessage("§cDieser Grabstein gehört jemand anderem (noch für kurze Zeit gesperrt).");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        String id = this.find(event.getBlock());
        if (id == null) {
            return;
        }
        if (this.locked(id, event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDieser Grabstein gehört jemand anderem (noch für kurze Zeit gesperrt).");
            return;
        }
        this.remove(id, false);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> block.getType() == Material.CHEST && this.find(block) != null);
    }

    /** Leere Grabsteine verschwinden, wenn der Besitzer fertig ist. */
    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest)) {
            return;
        }
        String id = this.find(chest.getBlock());
        if (id == null) {
            return;
        }
        boolean empty = true;
        for (ItemStack stack : chest.getBlockInventory().getContents()) {
            if (stack != null && !stack.getType().isAir()) {
                empty = false;
                break;
            }
        }
        if (empty) {
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> this.remove(id, true));
        }
    }

    /** Entfernt den Eintrag; mit dropItems=false wird nur vergessen (Block wurde schon abgebaut), sonst Items fallen lassen und Bloecke weg. */
    private void remove(String id, boolean clearBlocks) {
        for (String slot : new String[]{"a", "b"}) {
            String raw = this.data.getString("graves." + id + "." + slot, "");
            if (raw.isEmpty() || !clearBlocks) {
                continue;
            }
            String[] p = raw.split(",");
            org.bukkit.World world = this.plugin.getServer().getWorld(p[0]);
            if (world == null) {
                continue;
            }
            Block block = world.getBlockAt(Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
            if (block.getState() instanceof Chest chest) {
                for (ItemStack stack : chest.getBlockInventory().getContents()) {
                    if (stack != null && !stack.getType().isAir()) {
                        world.dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), stack);
                    }
                }
                chest.getBlockInventory().clear();
                block.setType(Material.AIR);
            }
        }
        this.data.set("graves." + id, null);
        this.save();
    }

    private void expire() {
        ConfigurationSection section = this.data.getConfigurationSection("graves");
        if (section == null) {
            return;
        }
        long life = Math.max(1L, this.plugin.getConfig().getLong("grave.expire-minutes", 30L)) * 60000L;
        long now = System.currentTimeMillis();
        for (String id : new ArrayList<String>(section.getKeys(false))) {
            if (now - section.getLong(id + ".time", now) >= life) {
                this.remove(id, true);
            }
        }
    }
}
