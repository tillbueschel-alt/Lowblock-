package de.server.network;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** /shop - Standard-Items kaufen (End, Nether, Gear, Food wie bei DonutSMP). Keine Spawner, keine Spezial-Items. */
final class Shop implements Listener, CommandExecutor {
    private static final String TAG = "Shop";
    private static final String[] CATEGORIES = {"End", "Nether", "Gear", "Food", "Blocks"};
    private static final String[] ICON_NAMES = {"ENDER_PEARL", "BLAZE_ROD", "TOTEM_OF_UNDYING", "COOKED_BEEF", "STONE"};
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    /** Bau-SMP-Shop: Kategorie -> "MATERIAL:Preis" (optional ":Flugstaerke" fuer Raketen). Feste Preise, kein PvP-Zubehoer. */
    private static final Map<String, String[]> BAU = new LinkedHashMap<String, String[]>();
    private static final Map<String, String> BAU_ICON = new LinkedHashMap<String, String>();
    private static final Map<String, String> BAU_INFO = new LinkedHashMap<String, String>();
    static {
        BAU.put("Overworld", new String[]{"STONE:2", "COBBLESTONE:1", "STONE_BRICKS:3", "SMOOTH_STONE:3", "GRANITE:2", "DIORITE:2", "ANDESITE:2", "DEEPSLATE:3", "DEEPSLATE_BRICKS:5", "DEEPSLATE_TILES:5", "TUFF:2", "CALCITE:6", "AMETHYST_BLOCK:18", "DRIPSTONE_BLOCK:5", "MOSS_BLOCK:5",
            "OAK_LOG:4", "SPRUCE_LOG:4", "BIRCH_LOG:4", "JUNGLE_LOG:4", "ACACIA_LOG:4", "DARK_OAK_LOG:4", "CHERRY_LOG:5", "MANGROVE_LOG:5", "OAK_PLANKS:1", "SPRUCE_PLANKS:1", "BRICKS:5", "MUD_BRICKS:5", "SANDSTONE:3", "RED_SANDSTONE:4", "GLASS:3", "WHITE_CONCRETE:4", "BLACK_CONCRETE:4", "WHITE_CONCRETE_POWDER:3",
            "TERRACOTTA:4", "WHITE_WOOL:3", "DIRT:1", "GRASS_BLOCK:2", "SAND:2", "RED_SAND:2", "GRAVEL:2", "CLAY:4", "QUARTZ_BLOCK:8", "BOOKSHELF:12", "LANTERN:15", "SEA_LANTERN:22", "PRISMARINE:18", "ICE:6", "PACKED_ICE:12"});
        BAU.put("Nether", new String[]{"NETHERRACK:1", "NETHER_BRICKS:4", "RED_NETHER_BRICKS:5", "BLACKSTONE:3", "POLISHED_BLACKSTONE:4", "POLISHED_BLACKSTONE_BRICKS:4", "BASALT:3", "SMOOTH_BASALT:4", "GILDED_BLACKSTONE:25",
            "GLOWSTONE:12", "SHROOMLIGHT:12", "CRIMSON_STEM:5", "WARPED_STEM:5", "CRIMSON_PLANKS:2", "WARPED_PLANKS:2", "NETHER_WART_BLOCK:6", "WARPED_WART_BLOCK:6",
            "SOUL_SAND:3", "SOUL_SOIL:3", "MAGMA_BLOCK:6", "CRYING_OBSIDIAN:60", "SOUL_LANTERN:18", "CHAIN:6", "NETHER_BRICK_FENCE:5"});
        BAU.put("End", new String[]{"END_STONE:3", "END_STONE_BRICKS:4", "PURPUR_BLOCK:6", "PURPUR_PILLAR:6", "PURPUR_STAIRS:8", "PURPUR_SLAB:5", "END_ROD:20", "CHORUS_FLOWER:30", "CHORUS_PLANT:20",
            "CHORUS_FRUIT:8", "OBSIDIAN:40", "ENDER_CHEST:300", "SHULKER_BOX:800", "DRAGON_BREATH:50", "END_PORTAL_FRAME:1000"});
        BAU.put("Fliegen", new String[]{"ELYTRA:900", "FIREWORK_ROCKET:8:3", "FIREWORK_ROCKET:5:1"});
        BAU.put("Essen", new String[]{"BREAD:5", "COOKED_BEEF:12", "COOKED_PORKCHOP:12", "COOKED_CHICKEN:10", "BAKED_POTATO:6", "GOLDEN_CARROT:15", "APPLE:6", "PUMPKIN_PIE:10", "CAKE:25"});
        BAU_ICON.put("Overworld", "GRASS_BLOCK");
        BAU_ICON.put("Nether", "NETHERRACK");
        BAU_ICON.put("End", "END_STONE");
        BAU_ICON.put("Fliegen", "ELYTRA");
        BAU_ICON.put("Essen", "COOKED_BEEF");
        BAU_INFO.put("Overworld", "Bausteine für den Alltag");
        BAU_INFO.put("Nether", "Nether-Blöcke ohne Nether-Reise");
        BAU_INFO.put("End", "End-Blöcke - das End ist hier nicht offen");
        BAU_INFO.put("Fliegen", "Elytra und Raketen, günstig für alle");
        BAU_INFO.put("Essen", "Essen für unterwegs");
    }

    private record BauItem(Material material, long price, int power) { }

    private static boolean excludedBuildMaterial(Material material) {
        String n = material.name();
        return material.isAir() || material.isLegacy() || !material.isItem() || material.getMaxStackSize() <= 1 || n.contains("COMMAND") || n.contains("STRUCTURE")
                || n.contains("JIGSAW") || n.contains("BARRIER") || n.contains("BEDROCK") || n.contains("SPAWNER")
                || n.contains("PORTAL") || n.equals("END_GATEWAY") || n.equals("LIGHT") || n.equals("MOVING_PISTON");
    }

    private static boolean netherBlock(String n) {
        return n.contains("NETHER") || n.contains("CRIMSON") || n.contains("WARPED") || n.contains("BLACKSTONE")
                || n.contains("BASALT") || n.contains("SOUL") || n.contains("MAGMA") || n.contains("GLOWSTONE")
                || n.contains("SHROOMLIGHT") || n.contains("QUARTZ") || n.contains("GILDED");
    }

    private static boolean endBlock(String n) {
        return n.startsWith("END_") || n.startsWith("PURPUR") || n.startsWith("CHORUS") || n.contains("SHULKER")
                || n.equals("OBSIDIAN") || n.equals("CRYING_OBSIDIAN") || n.equals("DRAGON_EGG") || n.equals("DRAGON_HEAD");
    }

    private static long generatedPrice(Material material) {
        String n = material.name();
        if (n.contains("ORE") || n.contains("DIAMOND") || n.contains("EMERALD") || n.contains("AMETHYST")) return 30L;
        if (n.contains("GOLD") || n.contains("IRON") || n.contains("COPPER") || n.contains("LAPIS")) return 20L;
        if (n.contains("GLASS") || n.contains("CONCRETE") || n.contains("TERRACOTTA") || n.contains("WOOL")) return 6L;
        if (n.endsWith("LOG") || n.endsWith("WOOD") || n.endsWith("PLANKS") || n.endsWith("STEM")) return 4L;
        if (n.contains("STAIRS") || n.contains("SLAB") || n.contains("WALL") || n.contains("FENCE")) return 5L;
        return 3L;
    }

    private static List<BauItem> bauItems(String category) {
        List<BauItem> out = new ArrayList<BauItem>();
        java.util.Set<Material> seen = java.util.EnumSet.noneOf(Material.class);
        String[] rows = BAU.get(category);
        if (rows == null) {
            return out;
        }
        for (String row : rows) {
            String[] f = row.split(":");
            Material m = Material.matchMaterial(f[0]);
            if (m != null) {
                seen.add(m);
                out.add(new BauItem(m, Long.parseLong(f[1]), f.length > 2 ? Integer.parseInt(f[2]) : 0));
            }
        }
        // Neue Minecraft-Blöcke werden automatisch aufgenommen; dadurch wächst der Katalog
        // ohne manuelle Pflege auf viele Seiten und bleibt bei Serverupdates aktuell.
        if (category.equals("Overworld") || category.equals("Nether") || category.equals("End")) {
            for (Material material : Material.values()) {
                if (!material.isBlock() || excludedBuildMaterial(material) || seen.contains(material)) continue;
                String n = material.name();
                boolean include = category.equals("Nether") ? netherBlock(n) : category.equals("End") ? endBlock(n) : !netherBlock(n) && !endBlock(n);
                if (include) {
                    out.add(new BauItem(material, generatedPrice(material), 0));
                }
            }
        }
        return out;
    }

    private boolean bau(Player player) {
        return this.plugin.inBauMode(player.getUniqueId());
    }

    /** Zustand einer geoeffneten Shop-Seite haengt direkt am Inventar (kein Abgleich ueber eine Map mehr). */
    private static final class ShopView implements org.bukkit.inventory.InventoryHolder {
        final String category;
        final int page;
        final boolean bau;
        Inventory inv;

        ShopView(String category, int page, boolean bau) {
            this.category = category;
            this.page = page;
            this.bau = bau;
        }

        @Override
        public Inventory getInventory() {
            return this.inv;
        }
    }

    private final DuelNetworkPlugin plugin;
    private final Map<UUID, String> viewing = new HashMap<UUID, String>();

    Shop(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl ist nur im Spiel verfügbar.");
            return true;
        }
        if (this.plugin.activeDuelPlayers.contains(player.getUniqueId()) || this.plugin.activePartyMatches.containsKey(player.getUniqueId())) {
            player.sendMessage("§cIm Duel kannst du den Shop nicht nutzen.");
            return true;
        }
        this.openMain(player);
        return true;
    }

    private static String display(String category) {
        return category + " Shop";
    }

    /** Beim Blaettern/Kaufen wird das offene Fenster nur neu befuellt (Maus bleibt, kein Flackern); sonst normal geoeffnet. */
    private Inventory reuse;

    private void show(Player player, Inventory inv) {
        Inventory open = this.reuse;
        this.reuse = null;
        if (open != null && open.getSize() == inv.getSize()) {
            open.setContents(inv.getContents());
        } else {
            player.openInventory(inv);
        }
    }

    /** Seiten-Leiste: Zurueck/Vorherige (45), Anfang (47), Seitenzahl (46), Ende (52), Naechste (53; auf der letzten Seite leer). */
    private void pager(Inventory inv, int page, int pages) {
        inv.setItem(45, Gui.item(Material.ARROW, page > 0 ? "§7« Vorherige Seite" : "§7« Zurück", page > 0 ? "§8Shift-Klick: 5 Seiten zurück" : "§8Zurück zum Shop-Menü"));
        inv.setItem(46, Gui.item(Material.PAPER, "§dSeite " + (page + 1) + "/" + pages));
        if (page < pages - 1) {
            inv.setItem(53, Gui.item(Material.ARROW, "§7Nächste Seite »", "§8Shift-Klick: 5 Seiten vor"));
        }
        if (pages > 3 && page > 0) {
            inv.setItem(47, Gui.item(Material.SPECTRAL_ARROW, "§7« Erste Seite"));
        }
        if (pages > 3 && page < pages - 1) {
            inv.setItem(52, Gui.item(Material.SPECTRAL_ARROW, "§7Letzte Seite »", "§8Seite " + pages));
        }
    }

    /** Ziel-Seite fuer einen Klick auf die Seiten-Leiste: -2 = keine Leiste, -1 = zurueck zum Menue, sonst Seitenindex. */
    private static int navTarget(int slot, int page, int pages, boolean shift) {
        return switch (slot) {
            case 45 -> page > 0 ? Math.max(0, page - (shift ? 5 : 1)) : -1;
            case 47 -> pages > 3 && page > 0 ? 0 : page;
            case 52 -> pages > 3 ? pages - 1 : page;
            case 53 -> Math.min(pages - 1, page + (shift ? 5 : 1));
            default -> -2;
        };
    }

    /** Seitenwechsel ausserhalb des Klick-Events oeffnen, damit Client und Server denselben Inventarstand haben. */
    private void openPageLater(Player player, String category, int currentPage, int targetPage, boolean build) {
        this.reuse = null;
        org.bukkit.inventory.InventoryHolder source = player.getOpenInventory().getTopInventory().getHolder();
        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            // nur oeffnen, wenn der Spieler noch genau diese Seite offen hat
            if (!player.isOnline() || source == null || player.getOpenInventory().getTopInventory().getHolder() != source) {
                return;
            }
            try {
                if (build) {
                    this.openBauCategory(player, category, targetPage);
                } else {
                    this.openCategory(player, category, targetPage);
                }
            }
            catch (RuntimeException ex) {
                Store.problem("Shop:Seite " + (targetPage + 1), ex);
                player.sendMessage("§cDiese Seite konnte nicht geladen werden.");
            }
        });
    }

    private static final int[] BAU_MAIN = {10, 12, 14, 16, 22};

    private void openBauMain(Player player) {
        Inventory inv = Gui.frame(this.plugin, TAG, "Bau-Shop", 36);
        inv.setItem(4, Gui.item(Material.GOLD_INGOT, "§6§lDein Geld", "§a" + Gui.money(Economy.balance(player.getUniqueId())), "§7Bau-Shop: feste, günstige Preise"));
        int i = 0;
        for (String category : BAU.keySet()) {
            Material icon = Material.matchMaterial(BAU_ICON.get(category));
            inv.setItem(BAU_MAIN[i++], Gui.item(icon == null ? Material.BOOK : icon, "§d§l" + (category.equals("Fliegen") ? "Flug-Shop" : category + " Shop"),
                    "§7" + BAU_INFO.get(category), "§7" + bauItems(category).size() + " Artikel", "§e▶ Klicken zum Öffnen"));
        }
        this.viewing.remove(player.getUniqueId());
        player.openInventory(inv);
    }

    private void openBauCategory(Player player, String category, int page) {
        List<BauItem> entries = bauItems(category);
        int pages = Math.max(1, (entries.size() + SLOTS.length - 1) / SLOTS.length);
        page = Math.max(0, Math.min(page, pages - 1));
        ShopView view = new ShopView(category, page, true);
        Inventory inv = Gui.frame(this.plugin, TAG, (category.equals("Fliegen") ? "Flug-Shop" : category + " Shop") + " (Bau) [" + (page + 1) + "/" + pages + "]", 54, view);
        view.inv = inv;
        int start = page * SLOTS.length;
        for (int i = 0; i < SLOTS.length && start + i < entries.size(); i++) {
            BauItem entry = entries.get(start + i);
            if (!entry.material().isItem()) {
                continue;
            }
            List<String> lore = new ArrayList<String>();
            lore.add("§7Preis pro Stück: §a" + Gui.money(entry.price()));
            if (entry.power() > 0) {
                lore.add("§7Flugdauer: §f" + entry.power());
            }
            lore.add("");
            lore.add("§eLinksklick: §f1 kaufen");
            lore.add("§eRechtsklick: §f16 kaufen");
            lore.add("§eShift-Klick: §f" + entry.material().getMaxStackSize() + " kaufen");
            inv.setItem(SLOTS[i], Gui.item(entry.material(), 1, "§f" + (entry.power() > 0 ? "Rakete (Flug " + entry.power() + ")" : Gui.prettyName(entry.material())), lore));
        }
        this.pager(inv, page, pages);
        inv.setItem(49, Gui.item(Material.GOLD_INGOT, "§6§lDein Geld", "§a" + Gui.money(Economy.balance(player.getUniqueId()))));
        this.viewing.put(player.getUniqueId(), category + "|" + page);
        this.show(player, inv);
    }

    private void buyBau(Player player, BauItem entry, int amount) {
        long price = entry.price() * amount;
        if (!Economy.take(player.getUniqueId(), price)) {
            player.sendMessage("§cDu hast nicht genug Geld (§f" + Gui.money(price) + "§c benötigt).");
            Chat.no(player);
            return;
        }
        ItemStack stack = new ItemStack(entry.material(), amount);
        if (entry.power() > 0) {
            org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
            if ((Object)meta instanceof org.bukkit.inventory.meta.FireworkMeta fw) {
                fw.setPower(entry.power());
                stack.setItemMeta(meta);
            }
        }
        int left = Gui.give(player, stack);
        int bought = amount - left;
        if (bought < amount) {
            Economy.add(player.getUniqueId(), player.getName(), entry.price() * (amount - bought));
        }
        if (bought <= 0) {
            player.sendMessage("§cDein Inventar ist voll.");
            Chat.no(player);
            return;
        }
        Chat.sound(player, org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.1f);
        player.sendMessage("§7Gekauft: §f" + bought + "x " + Gui.prettyName(entry.material()) + " §7für §a" + Gui.money(entry.price() * bought));
    }

    private void openMain(Player player) {
        if (this.bau(player)) {
            this.openBauMain(player);
            return;
        }
        Inventory inv = Gui.frame(this.plugin, TAG, "Kategorien", 36);
        long money = Economy.balance(player.getUniqueId());
        inv.setItem(4, Gui.item(Material.GOLD_INGOT, "§6§lDein Geld", "§a" + Gui.money(money), "§7Verdienen: §f/sell§7, §f/ah§7, §f/order"));
        for (int i = 0; i < CATEGORIES.length; i++) {
            Material icon = Material.matchMaterial(ICON_NAMES[i]);
            inv.setItem(10 + i * 2, Gui.item(icon == null ? Material.BOOK : icon, "§d§l" + display(CATEGORIES[i]), "§7" + Economy.instance.catalog(CATEGORIES[i]).size() + " Artikel", "§e▶ Klicken zum Öffnen"));
        }
        this.viewing.remove(player.getUniqueId());
        player.openInventory(inv);
    }

    private void openCategory(Player player, String category, int page) {
        List<Economy.Entry> entries = Economy.instance.catalog(category);
        int pages = Math.max(1, (entries.size() + SLOTS.length - 1) / SLOTS.length);
        page = Math.max(0, Math.min(page, pages - 1));
        ShopView view = new ShopView(category, page, false);
        Inventory inv = Gui.frame(this.plugin, TAG, display(category) + " [" + (page + 1) + "/" + pages + "]", 54, view);
        view.inv = inv;
        int start = page * SLOTS.length;
        for (int i = 0; i < SLOTS.length && start + i < entries.size(); i++) {
            Economy.Entry entry = entries.get(start + i);
            if (!entry.material().isItem()) {
                continue;
            }
            List<String> lore = new ArrayList<String>();
            lore.add("§7Preis pro Stück: §a" + Gui.money(Market.unit(entry.material(), entry.buy(), true)) + Market.trend(player, entry.material()));
            lore.add("§7Verkaufswert: §8" + Gui.money(Market.unit(entry.material(), entry.sell(), false)));
            if (Market.seeTrend(player)) {
                lore.add("§8Der Preis steigt beim Kaufen und fällt beim Verkaufen.");
            }
            lore.add("");
            lore.add("§eLinksklick: §f1 kaufen");
            lore.add("§eRechtsklick: §f16 kaufen");
            lore.add("§eShift-Klick: §f" + entry.material().getMaxStackSize() + " kaufen");
            inv.setItem(SLOTS[i], Gui.item(entry.material(), 1, "§f" + Gui.prettyName(entry.material()), lore));
        }
        this.pager(inv, page, pages);
        inv.setItem(49, Gui.item(Material.GOLD_INGOT, "§6§lDein Geld", "§a" + Gui.money(Economy.balance(player.getUniqueId()))));
        this.viewing.put(player.getUniqueId(), category + "|" + page);
        this.show(player, inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        this.reuse = Gui.isTitle(event.getView().getTitle(), TAG) ? event.getView().getTopInventory() : null;
        try {
            this.handleClick(event);
        }
        finally {
            this.reuse = null;
        }
    }

    private void handleClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!Gui.isTitle(title, TAG) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (slot < 0 || slot >= event.getView().getTopInventory().getSize() || clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        org.bukkit.inventory.InventoryHolder holder = event.getView().getTopInventory().getHolder();
        String category = holder instanceof ShopView sv ? sv.category + "|" + sv.page : null;
        boolean bauView = holder instanceof ShopView sv2 ? sv2.bau : this.bau(player);
        if (bauView) {
            this.clickBau(player, slot, category, event);
            return;
        }
        if (category == null) {
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (slot == 10 + i * 2) {
                    this.openCategory(player, CATEGORIES[i], 0);
                }
            }
            return;
        }
        String[] state = category.split("\\|", 2);
        String baseCategory = state[0];
        int page = state.length > 1 ? Integer.parseInt(state[1]) : 0;
        int pagesN = Math.max(1, (Economy.instance.catalog(baseCategory).size() + SLOTS.length - 1) / SLOTS.length);
        int target = navTarget(slot, page, pagesN, event.isShiftClick());
        if (target == -1) {
            this.reuse = null;
            this.openMain(player);
            return;
        }
        if (target >= 0) {
            if (target != page) {
                this.openPageLater(player, baseCategory, page, target, false);
            }
            return;
        }
        for (int i = 0; i < SLOTS.length; i++) {
            if (SLOTS[i] != slot) {
                continue;
            }
            List<Economy.Entry> entries = Economy.instance.catalog(baseCategory);
            if (page * SLOTS.length + i >= entries.size()) {
                return;
            }
            Economy.Entry entry = entries.get(page * SLOTS.length + i);
            int max = entry.material().getMaxStackSize();
            int amount = event.isShiftClick() ? max : event.isRightClick() ? Math.min(16, max) : 1;
            this.buy(player, entry, amount);
            this.openCategory(player, baseCategory, page);
            return;
        }
    }

    private void clickBau(Player player, int slot, String category, InventoryClickEvent event) {
        if (category == null) {
            int i = 0;
            for (String name : BAU.keySet()) {
                if (BAU_MAIN[i++] == slot) {
                    this.openBauCategory(player, name, 0);
                    return;
                }
            }
            return;
        }
        String[] state = category.split("\\|", 2);
        String baseCategory = state[0];
        int page = state.length > 1 ? Integer.parseInt(state[1]) : 0;
        List<BauItem> entries = bauItems(baseCategory);
        int pagesB = Math.max(1, (entries.size() + SLOTS.length - 1) / SLOTS.length);
        int target = navTarget(slot, page, pagesB, event.isShiftClick());
        if (target == -1) {
            this.reuse = null;
            this.openBauMain(player);
            return;
        }
        if (target >= 0) {
            if (target != page) {
                this.openPageLater(player, baseCategory, page, target, true);
            }
            return;
        }
        for (int i = 0; i < SLOTS.length && page * SLOTS.length + i < entries.size(); i++) {
            if (SLOTS[i] == slot) {
                BauItem entry = entries.get(page * SLOTS.length + i);
                int max = entry.material().getMaxStackSize();
                int amount = event.isShiftClick() ? max : event.isRightClick() ? Math.min(16, max) : 1;
                this.buyBau(player, entry, amount);
                this.openBauCategory(player, baseCategory, page);
                return;
            }
        }
    }

    private void buy(Player player, Economy.Entry entry, int amount) {
        long price = Market.quoteBuy(entry.material(), entry.buy(), amount);
        if (!Economy.take(player.getUniqueId(), price)) {
            player.sendMessage("§cDu hast nicht genug Geld (§f" + Gui.money(price) + "§c benötigt).");
            Chat.no(player);
            return;
        }
        int left = Gui.give(player, new ItemStack(entry.material(), amount));
        int bought = amount - left;
        if (bought <= 0) {
            Economy.add(player.getUniqueId(), player.getName(), price);
            player.sendMessage("§cDein Inventar ist voll.");
            Chat.no(player);
            return;
        }
        long cost = Market.quoteBuy(entry.material(), entry.buy(), bought);
        if (price > cost) {
            Economy.add(player.getUniqueId(), player.getName(), price - cost);
        }
        Market.buy(entry.material(), entry.buy(), bought);
        Chat.sound(player, org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.1f);
        player.sendMessage("§7Gekauft: §f" + bought + "x " + Gui.prettyName(entry.material()) + " §7für §a" + Gui.money(cost) + Market.trend(player, entry.material()));
    }
}
