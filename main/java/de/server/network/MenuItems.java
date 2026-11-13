/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.inventory.InventoryOpenEvent
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerSwapHandItemsEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package de.server.network;

import de.server.network.DuelFixes;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

final class MenuItems
implements Listener {
    private static final Map<Kind, List<Def>> DEFS = new HashMap<Kind, List<Def>>();
    private static MenuItems instance;
    private final DuelNetworkPlugin plugin;
    private final DuelFixes fixes;
    private final Map<UUID, String> profiles;
    private final Map<UUID, String> activeParkourMaps;
    private final Map<UUID, String> activePartyMatches;
    private final Set<UUID> activeDuelPlayers;
    private final Map<UUID, Long> lastUse = new HashMap<UUID, Long>();
    private final NamespacedKey trainingItemKey;
    private static final String[] NAV_TYPES;
    private static final String LOCK_NAME = "\u00a7cNPC derzeit deaktiviert";
    private static final String LOCK_MSG = "\u00a7cDieser Bereich ist derzeit gesperrt.";

    MenuItems(DuelNetworkPlugin javaPlugin, DuelFixes duelFixes) {
        this.plugin = javaPlugin;
        this.fixes = duelFixes;
        this.profiles = javaPlugin.profiles;
        this.activeParkourMaps = javaPlugin.activeParkourMaps;
        this.activePartyMatches = javaPlugin.activePartyMatches;
        this.activeDuelPlayers = javaPlugin.activeDuelPlayers;
        this.trainingItemKey = new NamespacedKey(javaPlugin, "training_weapon");
        instance = this;
    }

    private static String strip(String string) {
        return string == null ? "" : string.replaceAll("\u00a7.", "");
    }

    private static Def defOf(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        try {
            Material material = itemStack.getType();
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta == null) {
                return null;
            }
            String string = MenuItems.strip(itemMeta.getDisplayName());
            List list = itemMeta.getLore();
            for (List<Def> list2 : DEFS.values()) {
                for (Def def : list2) {
                    if (def.material != material || !def.plain.equals(string) || list == null || !list.contains(def.lore)) continue;
                    return def;
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
            Store.problem("MenuItems:112", throwable);
        }
        return null;
    }

    private static boolean isMenu(ItemStack itemStack) {
        return MenuItems.defOf(itemStack) != null;
    }

    private static ItemStack build(Def def) {
        ItemStack itemStack = new ItemStack(def.material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(def.name);
        ArrayList<String> arrayList = new ArrayList<String>();
        arrayList.add(def.lore);
        itemMeta.setLore(arrayList);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private Kind kindOf(Player player) {
        UUID uUID = player.getUniqueId();
        if (this.fixes.resultPhase.contains(uUID)) {
            if (this.activeDuelPlayers.contains(uUID)) {
                return Kind.DUELEND;
            }
            this.fixes.resultPhase.remove(uUID);
        }
        if (this.activeDuelPlayers.contains(uUID) || this.activePartyMatches.containsKey(uUID)) {
            return null;
        }
        if (this.activeParkourMaps.containsKey(uUID)) {
            return Kind.PARKOUR;
        }
        if (!"lobby".equals(this.profiles.getOrDefault(uUID, "lobby"))) {
            return null;
        }
        if (this.nearConfiguredLobby(player, "duell")) return Kind.DUEL_LOBBY;
        if (this.nearConfiguredLobby(player, "events")) return Kind.EVENT_LOBBY;
        return this.inLobby(player) ? Kind.LOBBY : null;
    }

    private boolean nearConfiguredLobby(Player player, String name) {
        Location location = this.plugin.readConfiguredLocation("locations.lobbies." + name);
        if (location == null || location.getWorld() == null || !location.getWorld().equals(player.getWorld())) return false;
        double radius = Math.max(8.0, this.plugin.getConfig().getDouble("lobby-items.context-radius", 48.0));
        return location.distanceSquared(player.getLocation()) <= radius * radius;
    }

    private boolean nearDummy(Player player) {
        for (Entity entity : player.getNearbyEntities(7.0, 5.0, 7.0)) {
            if ("dummy".equals(entity.getPersistentDataContainer().get(this.plugin.npcKey, PersistentDataType.STRING))) return true;
        }
        return false;
    }

    private boolean inLobby(Player player) {
        try {
            return this.plugin.isInLobby(player.getLocation());
        }
        catch (Throwable throwable) {
            return false;
        }
    }

    private int refreshRound;

    void refreshAll() {
        try {
            int div = Math.max(1, instance == null ? 1 : DuelNetworkPlugin.getPlugin(DuelNetworkPlugin.class).getConfig().getInt("performance.menu-items-seconds", 2));
            int turn = this.refreshRound++ % div;
            int index = 0;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (index++ % div == turn) {
                    this.refresh(player);
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
            Store.problem("MenuItems:168", throwable);
        }
    }

    static void refreshNow(Player player) {
        MenuItems menuItems = instance;
        if (menuItems != null) {
            menuItems.refresh(player);
        }
    }

    private void refresh(Player player) {
        try {
            int n;
            Def def;
            ItemStack stack;
            if (player.getGameMode() == GameMode.CREATIVE) {
                this.refreshTrainingWeapons(player, false);
                return;
            }
            Kind kind = this.kindOf(player);
            List<Def> list = kind == null ? new ArrayList<Def>() : DEFS.get(kind);
            PlayerInventory playerInventory = player.getInventory();
            for (int i = 0; i < 36; ++i) {
                ItemStack object = playerInventory.getItem(i);
                def = MenuItems.defOf(object);
                if (def == null) continue;
                n = 0;
                for (Def def2 : list) {
                    if (def2 != def || def2.slot != i) continue;
                    n = 1;
                }
                if (n != 0) continue;
                playerInventory.setItem(i, null);
            }
            for (Def def3 : list) {
                stack = playerInventory.getItem(def3.slot);
                if (MenuItems.defOf(stack) == def3) {
                    continue;
                }
                if (stack != null) {
                    n = -1;
                    for (int i = 9; i < 36; ++i) {
                        if (playerInventory.getItem(i) != null) continue;
                        n = i;
                        break;
                    }
                    if (n < 0) continue;
                    playerInventory.setItem(n, stack);
                }
                playerInventory.setItem(def3.slot, MenuItems.build(def3));
            }
            this.refreshTrainingWeapons(player, kind != null && this.nearDummy(player));
        }
        catch (Throwable throwable) {
            // empty catch block
            Store.problem("MenuItems:223", throwable);
        }
    }

    private boolean isTrainingItem(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(this.trainingItemKey, PersistentDataType.STRING);
    }

    private ItemStack trainingWeapon(Material material, String name, String role) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of("§7Trainingswaffe am Dummy", "§8Wird beim Verlassen wieder entfernt"));
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(this.trainingItemKey, PersistentDataType.STRING, role);
        item.setItemMeta(meta);
        return item;
    }

    private void refreshTrainingWeapons(Player player, boolean enabled) {
        PlayerInventory inventory = player.getInventory();
        if (!enabled) {
            for (int slot = 0; slot <= 1; slot++) {
                if (this.isTrainingItem(inventory.getItem(slot))) inventory.setItem(slot, null);
            }
            return;
        }
        Material[] materials = {Material.DIAMOND_SWORD, Material.DIAMOND_AXE};
        String[] names = {"§b§lTrainingsschwert", "§6§lTrainingsaxt"};
        String[] roles = {"sword", "axe"};
        for (int slot = 0; slot < materials.length; slot++) {
            ItemStack current = inventory.getItem(slot);
            if (this.isTrainingItem(current) && current.getType() == materials[slot]) continue;
            if (current != null && current.getType() != Material.AIR && !this.isTrainingItem(current)) {
                int free = -1;
                for (int i = 9; i < 36; i++) {
                    if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) { free = i; break; }
                }
                if (free < 0) continue;
                inventory.setItem(free, current);
            }
            inventory.setItem(slot, this.trainingWeapon(materials[slot], names[slot], roles[slot]));
        }
    }

    static void removeTrainingItems(Player player) {
        if (instance == null) return;
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            if (instance.isTrainingItem(player.getInventory().getItem(slot))) player.getInventory().setItem(slot, null);
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent playerInteractEvent) {
        Def def = MenuItems.defOf(playerInteractEvent.getItem());
        if (def == null) {
            return;
        }
        playerInteractEvent.setCancelled(true);
        Action action = playerInteractEvent.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (playerInteractEvent.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        Player player = playerInteractEvent.getPlayer();
        long l = System.currentTimeMillis();
        Long l2 = this.lastUse.get(player.getUniqueId());
        if (l2 != null && l - l2 < 400L) {
            return;
        }
        this.lastUse.put(player.getUniqueId(), l);
        Def def2 = def;
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (def.action.startsWith("cmd:") && this.blockedCommand(player, def.action.substring(4))) {
                return;
            }
            if (def.action.startsWith("type:") && this.locked(def.action.substring(5))) {
                player.sendMessage(LOCK_MSG);
                return;
            }
            this.fixes.action(player, def.action);
        }, 1L);
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent playerDropItemEvent) {
        if (MenuItems.isMenu(playerDropItemEvent.getItemDrop().getItemStack()) || this.isTrainingItem(playerDropItemEvent.getItemDrop().getItemStack())) {
            playerDropItemEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onSwap(PlayerSwapHandItemsEvent playerSwapHandItemsEvent) {
        if (MenuItems.isMenu(playerSwapHandItemsEvent.getMainHandItem()) || MenuItems.isMenu(playerSwapHandItemsEvent.getOffHandItem())
                || this.isTrainingItem(playerSwapHandItemsEvent.getMainHandItem()) || this.isTrainingItem(playerSwapHandItemsEvent.getOffHandItem())) {
            playerSwapHandItemsEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onDrag(InventoryDragEvent inventoryDragEvent) {
        if (MenuItems.isMenu(inventoryDragEvent.getOldCursor()) || this.isTrainingItem(inventoryDragEvent.getOldCursor())) {
            inventoryDragEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onClick(InventoryClickEvent inventoryClickEvent) {
        try {
            if (MenuItems.isMenu(inventoryClickEvent.getCurrentItem()) || MenuItems.isMenu(inventoryClickEvent.getCursor())
                    || this.isTrainingItem(inventoryClickEvent.getCurrentItem()) || this.isTrainingItem(inventoryClickEvent.getCursor())) {
                inventoryClickEvent.setCancelled(true);
                return;
            }
            String string = inventoryClickEvent.getClick().name();
            if (string.equals("NUMBER_KEY")) {
                int n = inventoryClickEvent.getHotbarButton();
                if (n >= 0 && (MenuItems.isMenu(inventoryClickEvent.getWhoClicked().getInventory().getItem(n))
                        || this.isTrainingItem(inventoryClickEvent.getWhoClicked().getInventory().getItem(n)))) {
                    inventoryClickEvent.setCancelled(true);
                }
            } else if (string.equals("SWAP_OFFHAND") && (MenuItems.isMenu(inventoryClickEvent.getWhoClicked().getInventory().getItemInOffHand())
                    || this.isTrainingItem(inventoryClickEvent.getWhoClicked().getInventory().getItemInOffHand()))) {
                inventoryClickEvent.setCancelled(true);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
            Store.problem("MenuItems:300", throwable);
        }
    }

    private boolean locked(String string) {
        try {
            return string != null && this.plugin.getConfig().getBoolean("npcs.locked." + string, false);
        }
        catch (Throwable throwable) {
            return false;
        }
    }

    static String cmdType(String string) {
        switch (string) {
            case "navigator": 
            case "nav": 
            case "menu": {
                return "navigator";
            }
            case "smp": {
                return "smp";
            }
            case "duell": 
            case "duel": {
                return "duell";
            }
            case "events": {
                return "events";
            }
            case "parkour": {
                return "parkour";
            }
            case "creative":
            case "bausmp":
            case "bau":
            case "build": {
                return "creative";
            }
        }
        return null;
    }

    boolean blockedCommand(Player player, String string) {
        String string2 = string.trim().toLowerCase();
        int n = string2.indexOf(32);
        if (n > 0) {
            string2 = string2.substring(0, n);
        }
        if (string2.startsWith("/")) {
            string2 = string2.substring(1);
        }
        if (!this.locked(MenuItems.cmdType(string2))) {
            return false;
        }
        player.sendMessage(LOCK_MSG);
        return true;
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent playerCommandPreprocessEvent) {
        try {
            String string = playerCommandPreprocessEvent.getMessage();
            if (string == null) {
                return;
            }
            Player player = playerCommandPreprocessEvent.getPlayer();
            if (this.blockedCommand(player, string)) {
                playerCommandPreprocessEvent.setCancelled(true);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
            Store.problem("MenuItems:372", throwable);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onNavigatorOpen(InventoryOpenEvent inventoryOpenEvent) {
        try {
            String string = inventoryOpenEvent.getView().getTitle();
            if (string == null || !string.contains("Netzwerk-Navigator")) {
                return;
            }
            Inventory inventory = inventoryOpenEvent.getInventory();
            for (int i = 0; i < NAV_TYPES.length; ++i) {
                ItemMeta itemMeta;
                ItemStack itemStack;
                if (!this.locked(NAV_TYPES[i]) || (itemStack = inventory.getItem(11 + i)) == null || (itemMeta = itemStack.getItemMeta()) == null) continue;
                itemMeta.setDisplayName(LOCK_NAME);
                ArrayList<String> arrayList = new ArrayList<String>();
                arrayList.add("\u00a77Derzeit gesperrt");
                itemMeta.setLore(arrayList);
                itemStack.setItemMeta(itemMeta);
                inventory.setItem(11 + i, itemStack);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
            Store.problem("MenuItems:397", throwable);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onNavigatorClick(InventoryClickEvent inventoryClickEvent) {
        try {
            if (!(inventoryClickEvent.getWhoClicked() instanceof Player)) {
                return;
            }
            String string = inventoryClickEvent.getView().getTitle();
            if (string == null || !string.contains("Netzwerk-Navigator")) {
                return;
            }
            int n = inventoryClickEvent.getRawSlot();
            if (n < 11 || n > 15 || inventoryClickEvent.getCurrentItem() == null) {
                return;
            }
            inventoryClickEvent.setCancelled(true);
            if (inventoryClickEvent.getCurrentItem().getType() == Material.BARRIER) {
                ((Player)inventoryClickEvent.getWhoClicked()).sendMessage("\u00a7cDieser Bereich ist derzeit nicht verf\u00fcgbar.");
                return;
            }
            inventoryClickEvent.setCurrentItem(null);
            Player player = (Player)inventoryClickEvent.getWhoClicked();
            String string2 = NAV_TYPES[n - 11];
            if (this.locked(string2)) {
                player.sendMessage(LOCK_MSG);
                return;
            }
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> this.fixes.action(player, "type:" + string2), 1L);
        }
        catch (Throwable throwable) {
            // empty catch block
            Store.problem("MenuItems:430", throwable);
        }
    }

    static {
        String string = "\u00a7";
        ArrayList<Def> arrayList = new ArrayList<Def>();
        arrayList.add(new Def(4, Material.COMPASS, string + "d" + string + "l\u2726 Navigator", "type:navigator", string + "7Rechtsklick: \u00f6ffnet alle Spielmodi"));
        arrayList.add(new Def(2, Material.BOOK, string + "6" + string + "l\u2726 Rangliste", "cmd:rangliste", string + "7Rechtsklick: Duel- & Parkour-Ranglisten"));
        arrayList.add(new Def(6, Material.NETHER_STAR, string + "5" + string + "l\u2726 Party", "cmd:party", string + "7Rechtsklick: Party erstellen / verwalten"));
        DEFS.put(Kind.LOBBY, arrayList);
        ArrayList<Def> duelLobby = new ArrayList<Def>();
        duelLobby.add(new Def(2, Material.DIAMOND_SWORD, string + "c" + string + "l\u2694 Duell-Kits", "duel_menu", string + "7Rechtsklick: Duell-Kit-Auswahl öffnen"));
        duelLobby.add(new Def(4, Material.COMPASS, string + "d" + string + "l\u2726 Navigator", "type:navigator", string + "7Rechtsklick: alle Spielmodi öffnen"));
        duelLobby.add(new Def(6, Material.NETHER_STAR, string + "5" + string + "l\u2726 Party", "cmd:party", string + "7Rechtsklick: Party erstellen / verwalten"));
        DEFS.put(Kind.DUEL_LOBBY, duelLobby);
        ArrayList<Def> eventLobby = new ArrayList<Def>();
        eventLobby.add(new Def(2, Material.FIREWORK_ROCKET, string + "d" + string + "l\u2726 Event-Menü", "cmd:events", string + "7Rechtsklick: Events und Termine anzeigen"));
        eventLobby.add(new Def(4, Material.COMPASS, string + "d" + string + "l\u2726 Navigator", "type:navigator", string + "7Rechtsklick: alle Spielmodi öffnen"));
        eventLobby.add(new Def(6, Material.NETHER_STAR, string + "5" + string + "l\u2726 Party", "cmd:party", string + "7Rechtsklick: Party erstellen / verwalten"));
        DEFS.put(Kind.EVENT_LOBBY, eventLobby);
        ArrayList<Def> arrayList2 = new ArrayList<Def>();
        arrayList2.add(new Def(2, Material.LIME_DYE, string + "a" + string + "l\u2691 Checkpoint", "parkour_cp", string + "7Rechtsklick: zum letzten Checkpoint"));
        arrayList2.add(new Def(4, Material.CLOCK, string + "e" + string + "l\u21bb Neustart", "cmd:parkour restart", string + "7Rechtsklick: Strecke neu starten"));
        arrayList2.add(new Def(6, Material.RED_DYE, string + "c" + string + "l\u2716 Parkour verlassen", "cmd:parkour leave", string + "7Rechtsklick: zur\u00fcck in die Lobby"));
        DEFS.put(Kind.PARKOUR, arrayList2);
        ArrayList<Def> arrayList3 = new ArrayList<Def>();
        arrayList3.add(new Def(2, Material.DIAMOND_SWORD, string + "a" + string + "l\u2694 Revanche", "rematch", string + "7Rechtsklick: Revanche anfragen"));
        arrayList3.add(new Def(4, Material.ENDER_PEARL, string + "d" + string + "l\u27a4 Nächster Gegner", "requeue", string + "7Rechtsklick: gleiches Kit, neuer Gegner"));
        arrayList3.add(new Def(6, Material.RED_DYE, string + "c" + string + "l\u2716 Duel verlassen", "duel_leave", string + "7Rechtsklick: zur\u00fcck in die Lobby"));
        DEFS.put(Kind.DUELEND, arrayList3);
        NAV_TYPES = new String[]{"smp", "duell", "creative", "events", "parkour"};
    }

    private static final class Def {
        final int slot;
        final Material material;
        final String name;
        final String plain;
        final String action;
        final String lore;

        Def(int n, Material material, String string, String string2, String string3) {
            this.slot = n;
            this.material = material;
            this.name = string;
            this.plain = MenuItems.strip(string);
            this.action = string2;
            this.lore = string3;
        }
    }

    private static enum Kind {
        LOBBY,
        DUEL_LOBBY,
        EVENT_LOBBY,
        PARKOUR,
        DUELEND;

    }
}
