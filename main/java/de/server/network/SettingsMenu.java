package de.server.network;

import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

/** /settings: persönliche Einstellungen als Menü (Claim-Hinweis, @Name-Ping, Kopfgeld-Meldungen, Hinweise). */
final class SettingsMenu implements Listener, CommandExecutor {
    private static final String TAG = "Einstellungen";
    /** Slot, Schlüssel, Name, Beschreibung */
    private static final Object[][] ROWS = {
        {10, "claim", "Claim-Hinweis", "Actionbar beim Betreten/Verlassen eines Claims"},
        {12, "mention", "@Name-Ping", "Ton und Hervorhebung, wenn dich jemand im Chat nennt"},
        {14, "bounty", "Kopfgeld-Meldungen", "Chat-Meldungen über Kopfgelder"},
        {16, "hints", "Modus-Hinweise", "Kurze Hinweise beim ersten Betreten eines Modus"}
    };
    private final DuelNetworkPlugin plugin;

    SettingsMenu(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        if (plugin.getCommand("settings") != null) {
            plugin.getCommand("settings").setExecutor((CommandExecutor)this);
        }
    }

    private void open(Player player) {
        Inventory inv = Gui.frame(this.plugin, TAG, "Deine Einstellungen", 27);
        UUID id = player.getUniqueId();
        for (Object[] r : ROWS) {
            boolean on = Prefs.on(id, (String)r[1]);
            inv.setItem((Integer)r[0], Gui.item(on ? Material.LIME_DYE : Material.GRAY_DYE, (on ? "§a" : "§7") + r[2] + (on ? " §8(an)" : " §8(aus)"), "§7" + r[3], "", "§eKlicken zum Umschalten"));
        }
        player.openInventory(inv);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            this.open(player);
        } else {
            sender.sendMessage("Nur im Spiel.");
        }
        return true;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!Gui.isTitle(event.getView().getTitle(), TAG)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        for (Object[] r : ROWS) {
            if ((Integer)r[0] == event.getSlot()) {
                Prefs.flip(player.getUniqueId(), (String)r[1]);
                Chat.click(player);
                this.open(player);
                return;
            }
        }
    }
}
