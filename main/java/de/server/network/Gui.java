package de.server.network;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Gemeinsame Helfer fuer die Menues (Shop, Auktionshaus, Orders) und Geldformate. */
final class Gui {
    private Gui() { }

    static ItemStack item(Material material, String name, String... lore) {
        return item(material, 1, name, Arrays.asList(lore));
    }

    static ItemStack item(Material material, int amount, String name, List<String> lore) {
        // Bloecke ohne Item-Form (Wandschilder, Staengel ...) koennen nicht angezeigt werden -> Platzhalter statt Fehler
        ItemStack stack = new ItemStack(material == null || !material.isItem() || material.isAir() ? Material.PAPER : material, Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(new ArrayList<String>(lore));
            ItemSkins.hideIfBlank(meta);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** Menue mit lila Rahmen. Titel: "<tag> » <titel>" - tag dient zum Erkennen beim Klick. */
    static Inventory frame(DuelNetworkPlugin plugin, String tag, String title, int size) {
        return frame(plugin, tag, title, size, null);
    }

    /** Wie frame, aber mit Halter: der Halter traegt den Zustand des Menues (z. B. Kategorie/Seite) direkt am Inventar. */
    static Inventory frame(DuelNetworkPlugin plugin, String tag, String title, int size, org.bukkit.inventory.InventoryHolder holder) {
        Inventory inv = plugin.getServer().createInventory(holder, size, "§8" + tag + " §8» §d" + title);
        ItemStack border = item(Material.PURPLE_STAINED_GLASS_PANE, " ");
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < size; i++) {
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            inv.setItem(i, edge ? border.clone() : filler.clone());
        }
        return inv;
    }

    static boolean isTitle(String title, String tag) {
        return title != null && title.contains("§8" + tag + " §8»");
    }

    /** "$1,234" */
    static String money(long value) {
        return "$" + String.format(Locale.GERMANY, "%,d", value);
    }

    /** "$1.2K" fuer die Sidebar */
    static String shortMoney(long value) {
        double v = value;
        if (value >= 1_000_000_000L) {
            return String.format(Locale.ROOT, "$%.1fB", v / 1_000_000_000d);
        }
        if (value >= 1_000_000L) {
            return String.format(Locale.ROOT, "$%.1fM", v / 1_000_000d);
        }
        if (value >= 10_000L) {
            return String.format(Locale.ROOT, "$%.1fK", v / 1000d);
        }
        return "$" + value;
    }

    /** Betrag mit k/m/b-Suffix, z.B. 1.5k. @return -1 bei Fehler */
    static long parseAmount(String text) {
        try {
            String t = text.trim().toLowerCase(Locale.ROOT).replace(",", ".").replace("$", "");
            double factor = 1d;
            if (t.endsWith("k")) {
                factor = 1_000d;
                t = t.substring(0, t.length() - 1);
            } else if (t.endsWith("m")) {
                factor = 1_000_000d;
                t = t.substring(0, t.length() - 1);
            } else if (t.endsWith("b")) {
                factor = 1_000_000_000d;
                t = t.substring(0, t.length() - 1);
            }
            double value = Double.parseDouble(t) * factor;
            if (Double.isNaN(value) || value < 1d || value > 1e12) {
                return -1L;
            }
            return Math.round(value);
        }
        catch (RuntimeException ex) {
            return -1L;
        }
    }

    /** Nur unveraenderte Items (ohne Namen/Verzauberung/...) werden gehandelt. */
    static boolean plain(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        if (!stack.hasItemMeta()) {
            return true;
        }
        // Items, die nur unsere "Wert"-Zeile tragen, gelten weiterhin als normal
        ItemMeta meta = stack.getItemMeta();
        return meta != null && hasMark(meta) && !meta.hasDisplayName() && stack.getEnchantments().isEmpty() && loreWithoutMark(meta).isEmpty();
    }

    /** Beginn der Wert-Zeile im Item-Tooltip (nur im SMP sichtbar). */
    static final String MARK = "\u00a77Wert: \u00a7a";

    static boolean hasMark(ItemMeta meta) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            return false;
        }
        for (String line : lore) {
            if (line.startsWith(MARK)) {
                return true;
            }
        }
        return false;
    }

    static List<String> loreWithoutMark(ItemMeta meta) {
        List<String> out = new ArrayList<String>();
        List<String> lore = meta.getLore();
        if (lore != null) {
            for (String line : lore) {
                if (!line.startsWith(MARK)) {
                    out.add(line);
                }
            }
        }
        return out;
    }

    static String prettyName(Material material) {
        String[] parts = material.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    /** Gibt Items; was nicht passt, wird zurueckgegeben (Anzahl uebrig). */
    static int give(Player player, ItemStack stack) {
        int left = 0;
        for (ItemStack rest : player.getInventory().addItem(new ItemStack[]{stack}).values()) {
            left += rest.getAmount();
        }
        return left;
    }

    /** Materialien, die nie gehandelt werden duerfen (Spawner, Admin-Bloecke ...). */
    static boolean blocked(Material material) {
        if (material == null || material.isAir()) {
            return true;
        }
        String n = material.name();
        return n.contains("SPAWNER") || n.contains("COMMAND_BLOCK") || n.equals("BEDROCK") || n.equals("BARRIER")
                || n.equals("STRUCTURE_BLOCK") || n.equals("STRUCTURE_VOID") || n.equals("JIGSAW") || n.equals("LIGHT")
                || n.equals("DEBUG_STICK") || n.equals("KNOWLEDGE_BOOK") || n.equals("COMMAND_BLOCK_MINECART")
                || n.equals("REINFORCED_DEEPSLATE") || n.equals("END_PORTAL_FRAME") || n.equals("PETRIFIED_OAK_SLAB")
                || n.equals("FARMLAND") || n.equals("BUDDING_AMETHYST") || n.endsWith("_SPAWN_EGG") || n.equals("TRIAL_SPAWNER")
                || n.equals("VAULT") || n.equals("OMINOUS_VAULT") || n.equals("ENDER_DRAGON_SPAWN_EGG");
    }
}
