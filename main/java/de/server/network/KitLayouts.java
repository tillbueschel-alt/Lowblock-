package de.server.network;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Inventar-Anordnungen fuer Kits (kitlayouts.yml): 37 Slots: 0-8 = Hotbar, 9-35 = Inventar, 36 = Offhand.
 * "standard" = vom Admin festgelegte Grundanordnung, "players.UUID" = persoenliche Anordnung eines Spielers.
 */
final class KitLayouts {
    static final int SLOTS = 37; // 0-8 Hotbar, 9-35 Inventar, 36 = Offhand

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    KitLayouts(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kitlayouts.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
    }

    private static String key(String kit) {
        return kit.toLowerCase().replace(' ', '_').replace('.', '_');
    }

    ItemStack[] personal(UUID id, String kit) {
        return this.read("players." + id + "." + key(kit));
    }

    ItemStack[] standard(String kit) {
        return this.read("standard." + key(kit));
    }

    synchronized void setPersonal(UUID id, String kit, ItemStack[] layout) {
        this.write("players." + id + "." + key(kit), layout);
    }

    synchronized void clearPersonal(UUID id, String kit) {
        this.data.set("players." + id + "." + key(kit), null);
        this.save();
    }

    synchronized void setStandard(String kit, ItemStack[] layout) {
        this.write("standard." + key(kit), layout);
    }

    private ItemStack[] read(String path) {
        List<?> list = this.data.getList(path);
        if (list == null) {
            return null;
        }
        ItemStack[] out = new ItemStack[Math.min(SLOTS, list.size())]; // aeltere Dateien haben nur 36 Eintraege (ohne Offhand)
        for (int i = 0; i < out.length; ++i) {
            Object o = list.get(i);
            if (o instanceof ItemStack stack && stack.getType() != Material.AIR) {
                out[i] = stack;
            }
        }
        return out;
    }

    private void write(String path, ItemStack[] layout) {
        List<ItemStack> list = new ArrayList<ItemStack>();
        for (int i = 0; i < SLOTS; ++i) {
            ItemStack stack = layout != null && i < layout.length ? layout[i] : null;
            list.add(stack == null ? new ItemStack(Material.AIR) : stack.clone());
        }
        this.data.set(path, list);
        this.save();
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("kitlayouts.yml konnte nicht gespeichert werden: " + e);
        }
    }
}
