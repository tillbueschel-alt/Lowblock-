package de.server.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Admin-Menue (/adminmenu): Spieler stummschalten/kicken/bannen, Gem-Boost, Event-Vorlagen starten, Wartung (Backup, Performance).
 * Jede Aktion fuehrt den normalen Befehl als der Admin aus, es gelten also dieselben Rechte wie bei der Eingabe.
 * Bann nur mit Shift-Klick, damit nichts aus Versehen passiert.
 */
final class AdminMenu implements CommandExecutor, Listener {
    private static final String TAG = "Admin";
    private static final String TAG_PLAYERS = "Admin-Spieler";
    private static final String TAG_ACTION = "Admin-Aktion";
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    private final DuelNetworkPlugin plugin;
    private final Map<UUID, List<UUID>> listed = new HashMap<UUID, List<UUID>>();
    private final Map<UUID, UUID> chosen = new HashMap<UUID, UUID>();

    AdminMenu(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        if (plugin.getCommand("adminmenu") != null) {
            plugin.getCommand("adminmenu").setExecutor((CommandExecutor)this);
        }
    }

    private boolean admin(Player player) {
        return player.isOp() || player.hasPermission("duelnetwork.admin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        if (!this.admin(player)) {
            player.sendMessage("§cDafür fehlt dir die Berechtigung.");
            return true;
        }
        this.openMain(player);
        return true;
    }

    private void openMain(Player player) {
        Inventory inv = Gui.frame(this.plugin, TAG, "Admin-Menü", 54);
        inv.setItem(10, Gui.item(Material.PLAYER_HEAD, "§b§lSpieler", "§7Stummschalten, Timeout, Kick, Bann", "", "§e▶ Klicken"));
        inv.setItem(12, Gui.item(Material.AMETHYST_SHARD, "§b§lGem-Boost x2 (60 Min)", "§e▶ Klicken"));
        inv.setItem(13, Gui.item(Material.AMETHYST_CLUSTER, "§b§lGem-Boost x5 (60 Min)", "§e▶ Klicken"));
        inv.setItem(14, Gui.item(Material.BARRIER, "§c§lGem-Boost beenden", "§e▶ Klicken"));
        inv.setItem(19, Gui.item(Material.IRON_SWORD, "§d§lFFA-Abend starten", "§7Event aus der Vorlage, sofort.", "§e▶ Klicken"));
        inv.setItem(20, Gui.item(Material.NETHERITE_SWORD, "§d§lTurnier-Abend starten", "§7Event aus der Vorlage, sofort.", "§e▶ Klicken"));
        inv.setItem(21, Gui.item(Material.GOLDEN_SWORD, "§d§lSchnell-FFA starten", "§7Kurze Runde (20 Min).", "§e▶ Klicken"));
        inv.setItem(23, Gui.item(Material.CHEST, "§a§lBackup erstellen", "§e▶ Klicken"));
        inv.setItem(24, Gui.item(Material.CLOCK, "§a§lPerformance", "§7Langsamste Plugin-Tasks im Chat.", "§e▶ Klicken"));
        inv.setItem(25, Gui.item(Material.COMPARATOR, "§a§lConfig prüfen", "§e▶ Klicken"));
        player.openInventory(inv);
    }

    private void openPlayers(Player admin) {
        List<UUID> ids = new ArrayList<UUID>();
        Inventory inv = Gui.frame(this.plugin, TAG_PLAYERS, "Spieler wählen", 54);
        int i = 0;
        for (Player online : this.plugin.getServer().getOnlinePlayers()) {
            if (i >= SLOTS.length) {
                break;
            }
            ItemStack head = Gui.item(Material.PLAYER_HEAD, "§f" + online.getName(), "§e▶ Klicken für Aktionen");
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(online);
                head.setItemMeta(meta);
            }
            inv.setItem(SLOTS[i++], head);
            ids.add(online.getUniqueId());
        }
        inv.setItem(49, Gui.item(Material.ARROW, "§7« Zurück"));
        this.listed.put(admin.getUniqueId(), ids);
        admin.openInventory(inv);
    }

    private void openAction(Player admin, Player target) {
        this.chosen.put(admin.getUniqueId(), target.getUniqueId());
        Inventory inv = Gui.frame(this.plugin, TAG_ACTION, target.getName(), 27);
        inv.setItem(10, Gui.item(Material.CLOCK, "§e§lTimeout 10 Min", "§7Chat-Sperre", "§e▶ Klicken"));
        inv.setItem(11, Gui.item(Material.CLOCK, "§e§lTimeout 1 Std", "§7Chat-Sperre", "§e▶ Klicken"));
        inv.setItem(12, Gui.item(Material.LIME_DYE, "§a§lTimeout aufheben", "§e▶ Klicken"));
        inv.setItem(14, Gui.item(Material.LEATHER_BOOTS, "§6§lKick", "§e▶ Klicken"));
        inv.setItem(16, Gui.item(Material.TNT, "§c§lBann 1 Tag", "§7Nur mit §fShift-Klick§7.", "§8Grund: Admin-Menü"));
        inv.setItem(22, Gui.item(Material.ARROW, "§7« Zurück"));
        admin.openInventory(inv);
    }

    private void run(Player admin, String command) {
        admin.closeInventory();
        admin.performCommand(command);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        boolean main = Gui.isTitle(title, TAG);
        boolean players = Gui.isTitle(title, TAG_PLAYERS);
        boolean action = Gui.isTitle(title, TAG_ACTION);
        if (!main && !players && !action) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player admin) || !this.admin(admin) || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        int slot = event.getSlot();
        if (main) {
            switch (slot) {
                case 10 -> this.openPlayers(admin);
                case 12 -> this.run(admin, "gemboost 2 60");
                case 13 -> this.run(admin, "gemboost 5 60");
                case 14 -> this.run(admin, "gemboost off");
                case 19 -> this.run(admin, "duelladmin event template ffa-abend");
                case 20 -> this.run(admin, "duelladmin event template turnier-abend");
                case 21 -> this.run(admin, "duelladmin event template ffa-schnell");
                case 23 -> this.run(admin, "dnwartung backup");
                case 24 -> this.run(admin, "dnwartung perf");
                case 25 -> this.run(admin, "dnwartung config");
                default -> { }
            }
            return;
        }
        if (players) {
            if (slot == 49) {
                this.openMain(admin);
                return;
            }
            List<UUID> ids = this.listed.get(admin.getUniqueId());
            for (int i = 0; ids != null && i < ids.size() && i < SLOTS.length; i++) {
                if (SLOTS[i] == slot) {
                    Player target = this.plugin.getServer().getPlayer(ids.get(i));
                    if (target != null) {
                        this.openAction(admin, target);
                    }
                    return;
                }
            }
            return;
        }
        UUID id = this.chosen.get(admin.getUniqueId());
        Player target = id == null ? null : this.plugin.getServer().getPlayer(id);
        if (slot == 22) {
            this.openPlayers(admin);
            return;
        }
        if (target == null) {
            admin.sendMessage("§cSpieler ist nicht mehr online.");
            this.openPlayers(admin);
            return;
        }
        switch (slot) {
            case 10 -> this.run(admin, "timeout " + target.getName() + " 10m Admin-Menü");
            case 11 -> this.run(admin, "timeout " + target.getName() + " 1h Admin-Menü");
            case 12 -> this.run(admin, "unmute " + target.getName());
            case 14 -> this.run(admin, "kick " + target.getName() + " Admin-Menü");
            case 16 -> {
                if (event.isShiftClick()) {
                    this.run(admin, "ban " + target.getName() + " 1d Admin-Menü");
                } else {
                    admin.sendMessage("§cZum Bannen Shift gedrückt halten und klicken.");
                }
            }
            default -> { }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (Gui.isTitle(title, TAG) || Gui.isTitle(title, TAG_PLAYERS) || Gui.isTitle(title, TAG_ACTION)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.listed.remove(event.getPlayer().getUniqueId());
        this.chosen.remove(event.getPlayer().getUniqueId());
    }
}
