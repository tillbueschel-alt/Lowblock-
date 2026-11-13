package de.server.network;

import org.bukkit.inventory.meta.ItemMeta;

/** Kleine Helfer fuer Menue-Items (ohne Texture-Pack: es werden nur normale Minecraft-Items verwendet). */
final class ItemSkins {
    private ItemSkins() { }

    /** Spieler, die das Texture-Pack (nur Sidebar-Logo/Rahmen) geladen haben. */
    static final java.util.Set<java.util.UUID> LOADED = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static java.lang.reflect.Method hide;
    private static boolean hideFailed;

    /** Namenlose Fuell-Items (Rahmen/Glas mit Name " ") bekommen gar keinen Tooltip - kein leerer Balken mehr. */
    static void hideIfBlank(ItemMeta meta) {
        if (meta == null || hideFailed) {
            return;
        }
        try {
            if (!meta.hasDisplayName() || !meta.getDisplayName().replaceAll("§.", "").trim().isEmpty()) {
                return;
            }
            if (hide == null) {
                hide = ItemMeta.class.getMethod("setHideTooltip", boolean.class);
            }
            hide.invoke(meta, true);
        }
        catch (Throwable t) {
            hideFailed = true;
        }
    }

    /** Alle namenlosen Items eines geoeffneten Menues ohne Tooltip. */
    static void hideBlanks(org.bukkit.inventory.Inventory inv) {
        if (inv == null || hideFailed) {
            return;
        }
        for (int i = 0; i < inv.getSize(); i++) {
            org.bukkit.inventory.ItemStack stack = inv.getItem(i);
            if (stack == null || !stack.hasItemMeta()) {
                continue;
            }
            ItemMeta meta = stack.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().replaceAll("§.", "").trim().isEmpty()) {
                hideIfBlank(meta);
                stack.setItemMeta(meta);
            }
        }
    }
}
