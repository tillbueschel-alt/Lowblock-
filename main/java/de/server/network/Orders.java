package de.server.network;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Orders (/order): Spieler hinterlegen Kaufauftraege (Item, Menge, Preis pro Stueck). Andere Spieler beliefern sie
 * und bekommen sofort das Geld; der Besteller holt die Items ab.
 */
final class Orders implements Listener, CommandExecutor, TabCompleter {
    private static final String TAG = "Orders";
    private static final int PAGE = 45;

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    private static final class View {
        boolean mine;
        int page;
        final List<String> ids = new ArrayList<String>();
    }

    private final Map<UUID, View> views = new HashMap<UUID, View>();

    Orders(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "orders.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("orders.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private List<String> ids() {
        List<String> ids = new ArrayList<String>();
        ConfigurationSection section = this.data.getConfigurationSection("orders");
        if (section != null) {
            ids.addAll(section.getKeys(false));
        }
        // beste Angebote (hoechster Preis pro Stueck) zuerst
        ids.sort((a, b) -> Long.compare(this.data.getLong("orders." + b + ".price", 0L), this.data.getLong("orders." + a + ".price", 0L)));
        return ids;
    }

    private int countOf(UUID buyer) {
        int n = 0;
        for (String id : this.ids()) {
            if (buyer.toString().equals(this.data.getString("orders." + id + ".buyer"))) {
                n++;
            }
        }
        return n;
    }

    private int deliveredCount(UUID owner) {
        int total = 0;
        ConfigurationSection section = this.data.getConfigurationSection("delivered." + owner);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                total += this.data.getInt("delivered." + owner + "." + key, 0);
            }
        }
        return total;
    }

    /* ---------- Befehle ---------- */

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl ist nur im Spiel verfügbar.");
            return true;
        }
        if (this.plugin.activeDuelPlayers.contains(player.getUniqueId()) || this.plugin.activePartyMatches.containsKey(player.getUniqueId())) {
            player.sendMessage("§cIm Duel kannst du keine Orders nutzen.");
            return true;
        }
        String sub = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "create": {
                this.create(player, args);
                return true;
            }
            case "collect": {
                this.collect(player);
                return true;
            }
            case "mine": {
                this.open(player, true, 0);
                return true;
            }
            default: {
                this.open(player, false, 0);
                return true;
            }
        }
    }

    private void create(Player player, String[] args) {
        if (args.length < 4) {
            if (Wizard.instance != null) {
                Wizard.instance.start(player, Wizard.Mode.ORDER);
            } else {
                player.sendMessage("§cNutze: /order create <Item> <Menge> <Preis pro Stück>  (z.B. /order create diamond 64 250)");
            }
            return;
        }
        Material material = Material.matchMaterial(args[1].toUpperCase(Locale.ROOT));
        if (material == null || Gui.blocked(material)) {
            player.sendMessage("§cDieses Item kann nicht bestellt werden.");
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        }
        catch (NumberFormatException ex) {
            amount = -1;
        }
        long price = Gui.parseAmount(args[3]);
        if (amount < 1 || amount > 2304 || price < 1L || price > 1_000_000_000L) {
            player.sendMessage("§cMenge 1-2304, Preis pro Stück 1 bis 1.000.000.000.");
            return;
        }
        this.placeOrder(player, material, amount, price);
    }

    /** Legt eine Order an und hinterlegt das Geld (vom Befehl und vom Erstell-Menue benutzt). */
    boolean placeOrder(Player player, Material material, int amount, long price) {
        if (material == null || Gui.blocked(material) || amount < 1 || amount > 2304 || price < 1L || price > 1_000_000_000L) {
            player.sendMessage("§cUngültige Order (Menge 1-2304, Preis pro Stück 1 bis 1.000.000.000).");
            return false;
        }
        int max = Math.max(1, this.plugin.getConfig().getInt("orders.max-per-player", 5));
        if (this.countOf(player.getUniqueId()) >= max) {
            player.sendMessage("§cDu hast schon " + max + " Orders. Beende eine mit /order mine.");
            return false;
        }
        long total = price * amount;
        long id = this.data.getLong("next", 1L);
        String tx = Economy.beginJournal("ORDER_CREATE", "order=" + id + ";buyer=" + player.getUniqueId() + ";amount=" + total + ";bau=" + (this.plugin.inBauMode(player.getUniqueId()) ? "1" : "0"));
        if (tx == null) {
            player.sendMessage("§cDie Order-Datenbank ist momentan nicht verfügbar.");
            return false;
        }
        this.data.set("next", id + 1L);
        String p = "orders." + id;
        this.data.set(p + ".buyer", player.getUniqueId().toString());
        this.data.set(p + ".name", player.getName());
        this.data.set(p + ".zone", this.plugin.inBauMode(player.getUniqueId()) ? "bau" : "smp");
        this.data.set(p + ".material", material.name());
        this.data.set(p + ".remaining", amount);
        this.data.set(p + ".price", price);
        this.save();
        Store.flush();
        if (!Economy.debitOnce(Economy.key(player.getUniqueId()), total, tx + ":debit")) {
            this.data.set(p, null);
            this.save();
            Store.flush();
            Economy.completeJournal(tx);
            player.sendMessage("§cDu brauchst " + Gui.money(total) + " (wird als Sicherheit hinterlegt).");
            return false;
        }
        Economy.completeJournal(tx);
        Chat.coins(player);
        player.sendMessage("§aOrder erstellt: §f" + amount + "x " + Gui.prettyName(material) + " §7à §a" + Gui.money(price) + "§7 (" + Gui.money(total) + " hinterlegt).");
        return true;
    }

    private void collect(Player player) {
        UUID id = player.getUniqueId();
        ConfigurationSection section = this.data.getConfigurationSection("delivered." + id);
        if (section == null || section.getKeys(false).isEmpty()) {
            player.sendMessage("§7Keine gelieferten Items.");
            return;
        }
        boolean full = false;
        for (String key : new ArrayList<String>(section.getKeys(false))) {
            Material material = Material.matchMaterial(key);
            int amount = this.data.getInt("delivered." + id + "." + key, 0);
            if (material == null) {
                this.data.set("delivered." + id + "." + key, null);
                continue;
            }
            int left = amount;
            while (left > 0 && !full) {
                int chunk = Math.min(left, material.getMaxStackSize());
                int notFit = Gui.give(player, new ItemStack(material, chunk));
                left -= chunk - notFit;
                if (notFit > 0) {
                    full = true;
                }
            }
            this.data.set("delivered." + id + "." + key, left <= 0 ? null : left);
        }
        this.save();
        player.sendMessage(full ? "§eInventar voll - den Rest holst du später mit /order collect." : "§aAlle gelieferten Items abgeholt.");
    }

    /* ---------- Menue ---------- */

    void show(Player player, boolean mine) {
        this.open(player, mine, 0);
    }

    private void open(Player player, boolean mine, int page) {
        UUID me = player.getUniqueId();
        View view = new View();
        view.mine = mine;
        List<String> all = new ArrayList<String>();
        for (String id : this.ids()) {
            boolean own = me.toString().equals(this.data.getString("orders." + id + ".buyer"));
            if (mine == own) {
                all.add(id);
            }
        }
        int pages = Math.max(1, (all.size() + PAGE - 1) / PAGE);
        view.page = Math.max(0, Math.min(page, pages - 1));
        Inventory inv = Gui.frame(this.plugin, TAG, mine ? "Meine Orders" : "Offene Orders", 54);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, null);
        }
        for (int i = 0; i < PAGE; i++) {
            int index = view.page * PAGE + i;
            if (index >= all.size()) {
                break;
            }
            String id = all.get(index);
            String p = "orders." + id;
            Material material = Material.matchMaterial(this.data.getString(p + ".material", ""));
            if (material == null) {
                view.ids.add(null);
                continue;
            }
            int remaining = this.data.getInt(p + ".remaining", 0);
            long price = this.data.getLong(p + ".price", 0L);
            List<String> lore = new ArrayList<String>();
            lore.add("§7Besteller: §f" + this.data.getString(p + ".name", "?"));
            lore.add("§7Gesucht: §f" + remaining + "x");
            lore.add("§7Preis pro Stück: §a" + Gui.money(price));
            lore.add("§7Gesamt: §a" + Gui.money(price * remaining));
            lore.add("");
            lore.add(mine ? "§e▶ Klicken: Order abbrechen (Geld zurück)" : "§e▶ Klicken: aus deinem Inventar liefern");
            inv.setItem(i, Gui.item(material, Math.min(64, Math.max(1, remaining)), "§f" + Gui.prettyName(material), lore));
            view.ids.add(id);
        }
        while (view.ids.size() < PAGE) {
            view.ids.add(null);
        }
        inv.setItem(45, Gui.item(Material.ARROW, "§7« Vorherige Seite"));
        inv.setItem(46, Gui.item(Material.PAPER, "§dSeite " + (view.page + 1) + "/" + pages));
        inv.setItem(47, Gui.item(Material.GOLD_INGOT, "§6§lDein Geld", "§a" + Gui.money(Economy.balance(me))));
        inv.setItem(49, mine ? Gui.item(Material.CHEST, "§d§lOffene Orders", "§e▶ Klicken") : Gui.item(Material.ENDER_CHEST, "§d§lMeine Orders", "§7" + this.countOf(me) + " aktiv", "§e▶ Klicken"));
        inv.setItem(51, Gui.item(Material.HOPPER, "§a§lLieferungen abholen", "§7Items: §f" + this.deliveredCount(me), "§e▶ Klicken"));
        inv.setItem(52, Gui.item(Material.BOOK, "§f§lNeue Order", "§7Item, Menge und Preis im Menü wählen.", "§7Das Geld wird hinterlegt.", "§e▶ Klicken"));
        inv.setItem(53, Gui.item(Material.ARROW, "§7Nächste Seite »"));
        this.views.put(me, view);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!Gui.isTitle(title, TAG) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        View view = this.views.get(player.getUniqueId());
        if (view == null || slot < 0 || slot >= 54) {
            return;
        }
        if (slot >= 45) {
            switch (slot) {
                case 45 -> this.open(player, view.mine, view.page - 1);
                case 53 -> this.open(player, view.mine, view.page + 1);
                case 49 -> this.open(player, !view.mine, 0);
                case 51 -> {
                    player.closeInventory();
                    this.collect(player);
                }
                case 52 -> {
                    if (Wizard.instance != null) {
                        Wizard.instance.start(player, Wizard.Mode.ORDER);
                    }
                }
                default -> { }
            }
            return;
        }
        if (slot >= view.ids.size() || view.ids.get(slot) == null) {
            return;
        }
        String id = view.ids.get(slot);
        String p = "orders." + id;
        if (!this.data.contains(p)) {
            player.sendMessage("§cDiese Order gibt es nicht mehr.");
            this.open(player, view.mine, view.page);
            return;
        }
        UUID me = player.getUniqueId();
        String buyerText = this.data.getString(p + ".buyer", "");
        Material material = Material.matchMaterial(this.data.getString(p + ".material", ""));
        int remaining = this.data.getInt(p + ".remaining", 0);
        long price = this.data.getLong(p + ".price", 0L);
        String buyerName = this.data.getString(p + ".name", null);
        if (view.mine) {
            if (!buyerText.equals(me.toString())) {
                return;
            }
            long refund = price * remaining;
            boolean bauOrder = "bau".equals(this.data.getString(p + ".zone", "smp"));
            String tx = Economy.beginJournal("ORDER_CANCEL", "order=" + id + ";buyer=" + me + ";amount=" + refund + ";bau=" + (bauOrder ? "1" : "0"));
            if (tx == null) {
                player.sendMessage("§cDie Economy-Datenbank ist momentan nicht verfügbar.");
                return;
            }
            this.data.set(p, null);
            this.save();
            Store.flush();
            if (!Economy.creditOnce(Economy.key(me, bauOrder), player.getName(), refund, tx + ":refund")) {
                player.sendMessage("§cRückerstattung wartet auf Recovery. Bitte nicht erneut abbrechen.");
                return;
            }
            Economy.completeJournal(tx);
            player.sendMessage("§aOrder abgebrochen, §f" + Gui.money(refund) + " §azurück.");
            this.open(player, true, view.page);
            return;
        }
        if (buyerText.equals(me.toString())) {
            player.sendMessage("§cDas ist deine eigene Order.");
            return;
        }
        if (material == null || remaining < 1) {
            return;
        }
        if (!this.data.getString(p + ".zone", "smp").equals(this.plugin.inBauMode(me) ? "bau" : "smp")) {
            player.sendMessage("§cDiese Order gehört zum anderen Modus (SMP / Bau-SMP).");
            return;
        }
        // Items liefern: nur unveraenderte Items
        int delivered = 0;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < Math.min(36, contents.length) && delivered < remaining; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() != material || !Gui.plain(stack)) {
                continue;
            }
            int take = Math.min(stack.getAmount(), remaining - delivered);
            delivered += take;
            if (take >= stack.getAmount()) {
                player.getInventory().setItem(i, null);
            } else {
                stack.setAmount(stack.getAmount() - take);
                player.getInventory().setItem(i, stack);
            }
        }
        if (delivered == 0) {
            player.sendMessage("§cDu hast kein passendes Item (" + Gui.prettyName(material) + ", unverändert) im Inventar.");
            return;
        }
        long pay = price * delivered;
        String tx = Economy.beginJournal("ORDER_DELIVERY", "order=" + id + ";supplier=" + me + ";amount=" + pay + ";bau=" + (this.plugin.inBauMode(me) ? "1" : "0"));
        if (tx == null) {
            player.sendMessage("§cDie Economy-Datenbank ist momentan nicht verfügbar.");
            return;
        }
        int left = remaining - delivered;
        if (left <= 0) {
            this.data.set(p, null);
        } else {
            this.data.set(p + ".remaining", left);
        }
        try {
            UUID buyer = UUID.fromString(buyerText);
            String key = "delivered." + buyer + "." + material.name();
            this.data.set(key, this.data.getInt(key, 0) + delivered);
            Player online = this.plugin.getServer().getPlayer(buyer);
            if (online != null) {
                online.sendMessage("§a" + player.getName() + " hat §f" + delivered + "x " + Gui.prettyName(material) + " §ageliefert! Hole sie mit §d/order collect§a ab.");
            }
        }
        catch (IllegalArgumentException ignored) {
            // kaputter Besteller-Eintrag
        }
        this.save();
        Store.flush();
        if (!Economy.creditOnce(Economy.key(me), player.getName(), pay, tx + ":pay")) {
            player.sendMessage("§cDie Zahlung wartet auf Recovery. Bitte nicht erneut liefern.");
            return;
        }
        Economy.completeJournal(tx);
        Chat.coins(player);
        player.sendMessage("§7Geliefert: §f" + delivered + "x " + Gui.prettyName(material) + " §7an " + buyerName + " für §a" + Gui.money(pay));
        this.open(player, false, view.page);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            out.add("create");
            out.add("mine");
            out.add("collect");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
            for (String name : List.of("diamond", "iron_ingot", "gold_ingot", "netherite_scrap", "end_crystal", "totem_of_undying", "ender_pearl", "obsidian", "cooked_beef")) {
                if (name.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(name);
                }
            }
        }
        return out;
    }
}
