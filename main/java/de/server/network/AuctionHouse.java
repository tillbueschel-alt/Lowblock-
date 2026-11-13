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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Auktionshaus (/ah): Spieler verkaufen Items zu einem Festpreis, alle anderen koennen kaufen. */
final class AuctionHouse implements Listener, CommandExecutor, TabCompleter {
    private static final String TAG = "Auktionshaus";
    private static final int PAGE = 45;

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    /** Zustand der geoeffneten Ansicht eines Spielers. */
    private static final class View {
        boolean mine;
        int page;
        int sort;
        String filter = "";
        final List<String> ids = new ArrayList<String>();
    }

    private final Map<UUID, View> views = new HashMap<UUID, View>();

    AuctionHouse(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ah.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, () -> this.expire(), 1200L, 1200L);
    }

    private int maxListings() {
        return Math.max(1, this.plugin.getConfig().getInt("ah.max-listings", 10));
    }

    private long hours() {
        return Math.max(1L, this.plugin.getConfig().getLong("ah.duration-hours", 48L));
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("ah.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    /* ---------- Daten ---------- */

    private List<String> listingIds() {
        List<String> ids = new ArrayList<String>();
        ConfigurationSection section = this.data.getConfigurationSection("listings");
        if (section != null) {
            ids.addAll(section.getKeys(false));
        }
        ids.sort((a, b) -> Long.compare(Long.parseLong(b), Long.parseLong(a)));
        return ids;
    }

    private int countOf(UUID seller) {
        int n = 0;
        for (String id : this.listingIds()) {
            if (seller.toString().equals(this.data.getString("listings." + id + ".seller"))) {
                n++;
            }
        }
        return n;
    }

    /** Bau-SMP und SMP haben getrennte Auktionshaeuser. */
    private String zone(Player player) {
        return this.plugin.inBauMode(player.getUniqueId()) ? "bau" : "smp";
    }

    private String claimKey(UUID owner, String zone) {
        return ("bau".equals(zone) ? "claimsbau." : "claims.") + owner;
    }

    private void addClaim(UUID owner, ItemStack stack, String zone) {
        List<ItemStack> list = this.claims(owner, zone);
        list.add(stack);
        this.data.set(this.claimKey(owner, zone), list);
    }

    @SuppressWarnings("unchecked")
    private List<ItemStack> claims(UUID owner, String zone) {
        List<ItemStack> out = new ArrayList<ItemStack>();
        List<?> raw = this.data.getList(this.claimKey(owner, zone));
        if (raw != null) {
            for (Object o : raw) {
                if (o instanceof ItemStack stack) {
                    out.add(stack);
                }
            }
        }
        return out;
    }

    private void expire() {
        try {
            long now = System.currentTimeMillis();
            boolean changed = false;
            for (String id : this.listingIds()) {
                String p = "listings." + id;
                if (this.data.getLong(p + ".expires", 0L) > now) {
                    continue;
                }
                ItemStack stack = this.data.getItemStack(p + ".item");
                String seller = this.data.getString(p + ".seller", "");
                String zoneOfListing = this.data.getString(p + ".zone", "smp");
                this.data.set(p, null);
                changed = true;
                if (stack == null) {
                    continue;
                }
                try {
                    UUID owner = UUID.fromString(seller);
                    this.addClaim(owner, stack, zoneOfListing);
                    Player online = this.plugin.getServer().getPlayer(owner);
                    if (online != null) {
                        online.sendMessage("§7Ein Angebot ist abgelaufen. Hole das Item mit §d/ah claim §7ab.");
                    }
                }
                catch (IllegalArgumentException ignored) {
                    // kaputter Eintrag
                }
            }
            if (changed) {
                this.save();
            }
        }
        catch (Throwable t) {
            this.plugin.getLogger().warning("Auktionshaus-Pruefung fehlgeschlagen: " + t);
        }
    }

    /* ---------- Befehle ---------- */

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl ist nur im Spiel verfügbar.");
            return true;
        }
        if (this.plugin.activeDuelPlayers.contains(player.getUniqueId()) || this.plugin.activePartyMatches.containsKey(player.getUniqueId())) {
            player.sendMessage("§cIm Duel kannst du das Auktionshaus nicht nutzen.");
            return true;
        }
        String sub = args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "";
        switch (sub) {
            case "sell": {
                this.sell(player, args.length >= 2 ? args[1] : "");
                return true;
            }
            case "claim": {
                this.claim(player);
                return true;
            }
            case "mine": {
                this.open(player, true, 0);
                return true;
            }
            case "search": case "suche": {
                this.search(player, args.length >= 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : "");
                return true;
            }
            default: {
                this.open(player, false, 0);
                return true;
            }
        }
    }

    /** Meldet normale Items (keine Werkzeuge/Verzauberungen/Shulker) mit Stueckpreis an den Markt. */
    private void signal(ItemStack stack, long price, boolean sale) {
        try {
            Economy economy = Economy.instance;
            if (economy == null || stack == null || stack.getAmount() < 1 || !stack.getEnchantments().isEmpty() || Economy.isShulker(stack) || stack.getType().getMaxDurability() > 0) {
                return;
            }
            long base = economy.basePrice(stack.getType());
            Market.auction(stack.getType(), base, Math.max(1L, price / stack.getAmount()), sale);
        }
        catch (Throwable ignored) {
            // nur Markt
            Store.problem("AuctionHouse:193", ignored);
        }
    }

    private void sell(Player player, String priceText) {
        long price = Gui.parseAmount(priceText);
        if (price < 1L) {
            if (Wizard.instance != null) {
                Wizard.instance.start(player, Wizard.Mode.AH);
            } else {
                player.sendMessage("§cNutze: /ah sell <Preis>  (Item in der Hand, z.B. /ah sell 1.5k)");
            }
            return;
        }
        int slot = player.getInventory().getHeldItemSlot();
        ItemStack hand = player.getInventory().getItem(slot);
        if (hand == null || hand.getType() == Material.AIR) {
            player.sendMessage("§cDu hältst nichts in der Hand.");
            return;
        }
        this.listItem(player, slot, hand, hand.getAmount(), price);
    }

    /** Stellt "amount" Stueck aus dem Inventar-Slot zum Festpreis ein (vom Befehl und vom Erstell-Menue benutzt). */
    boolean listItem(Player player, int slot, ItemStack proto, int amount, long price) {
        ItemStack inSlot = slot < 0 ? null : player.getInventory().getItem(slot);
        if (inSlot == null || inSlot.getType() == Material.AIR || proto == null || !inSlot.isSimilar(proto) || amount < 1 || inSlot.getAmount() < amount) {
            Chat.no(player);
            player.sendMessage("§cDas Item ist nicht mehr (in dieser Menge) in deinem Inventar.");
            return false;
        }
        if (Gui.blocked(inSlot.getType())) {
            player.sendMessage("§cDieses Item darf nicht verkauft werden.");
            return false;
        }
        if (price < 1L || price > 1_000_000_000_000L) {
            player.sendMessage("§cUngültiger Preis.");
            return false;
        }
        if (this.countOf(player.getUniqueId()) >= this.maxListings()) {
            player.sendMessage("§cDu hast schon " + this.maxListings() + " Angebote. Hole eines zurück (/ah mine).");
            return false;
        }
        ItemStack listed = inSlot.clone();
        listed.setAmount(amount);
        long id = this.data.getLong("next", 1L);
        this.data.set("next", id + 1L);
        String p = "listings." + id;
        this.data.set(p + ".zone", this.zone(player));
        this.data.set(p + ".seller", player.getUniqueId().toString());
        this.data.set(p + ".name", player.getName());
        this.data.set(p + ".price", price);
        this.data.set(p + ".expires", System.currentTimeMillis() + this.hours() * 3600000L);
        this.data.set(p + ".item", listed);
        // Erst das Item aus dem Inventar reservieren, dann das Listing dauerhaft speichern.
        // So kann ein Listing nach einem harten Neustart nicht zusammen mit dem Originalitem
        // im Inventar weiterexistieren.
        if (amount >= inSlot.getAmount()) {
            player.getInventory().setItem(slot, null);
        } else {
            inSlot.setAmount(inSlot.getAmount() - amount);
            player.getInventory().setItem(slot, inSlot);
        }
        this.save();
        Store.flush();
        this.signal(listed, price, false);
        Chat.coins(player);
        player.sendMessage("§aAngebot erstellt: §f" + amount + "x " + Gui.prettyName(listed.getType()) + " §7für §a" + Gui.money(price) + "§7 (" + this.hours() + " Std).");
        return true;
    }

    private void claim(Player player) {
        UUID id = player.getUniqueId();
        String zone = this.zone(player);
        List<ItemStack> list = this.claims(id, zone);
        if (list.isEmpty()) {
            player.sendMessage("§7Nichts abzuholen.");
            return;
        }
        List<ItemStack> rest = new ArrayList<ItemStack>();
        int given = 0;
        for (ItemStack stack : list) {
            int left = Gui.give(player, stack.clone());
            if (left > 0) {
                ItemStack remainder = stack.clone();
                remainder.setAmount(left);
                rest.add(remainder);
            }
            if (left < stack.getAmount()) {
                given++;
            }
        }
        this.data.set(this.claimKey(id, zone), rest.isEmpty() ? null : rest);
        this.save();
        player.sendMessage(rest.isEmpty() ? "§aAlle Items abgeholt." : "§eNicht alles passte ins Inventar. Mach Platz und nutze /ah claim erneut.");
    }

    /* ---------- Menue ---------- */

    /** Suchfilter: Teil des Item- oder Anzeigenamens (leer = alles). */
    private boolean matches(String id, String filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        ItemStack stack = this.data.getItemStack("listings." + id + ".item");
        if (stack == null) {
            return false;
        }
        String name = Gui.prettyName(stack.getType()).toLowerCase(Locale.ROOT) + " " + stack.getType().name().toLowerCase(Locale.ROOT);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            name += " " + meta.getDisplayName().toLowerCase(Locale.ROOT);
        }
        return name.contains(filter);
    }

    private void search(Player player, String text) {
        View previous = this.views.get(player.getUniqueId());
        View view = new View();
        view.sort = previous == null ? 0 : previous.sort;
        view.filter = text.toLowerCase(Locale.ROOT).trim();
        this.views.put(player.getUniqueId(), view);
        player.sendMessage(view.filter.isEmpty() ? "§7Suche zurückgesetzt." : "§7Suche: §f" + view.filter + " §8(§f/ah search §8ohne Text setzt zurück)");
        this.open(player, false, 0);
    }

    private double unitPrice(String id) {
        ItemStack stack = this.data.getItemStack("listings." + id + ".item");
        return this.data.getLong("listings." + id + ".price", 0L) / (double) Math.max(1, stack == null ? 1 : stack.getAmount());
    }

    void show(Player player, boolean mine) {
        this.open(player, mine, 0);
    }

    private void open(Player player, boolean mine, int page) {
        UUID me = player.getUniqueId();
        View view = new View();
        view.mine = mine;
        View previous = this.views.get(me);
        view.sort = previous == null ? 0 : previous.sort;
        view.filter = previous == null ? "" : previous.filter;
        List<String> all = new ArrayList<String>();
        String myZone = this.zone(player);
        for (String id : this.listingIds()) {
            if (!myZone.equals(this.data.getString("listings." + id + ".zone", "smp"))) {
                continue;
            }
            boolean own = me.toString().equals(this.data.getString("listings." + id + ".seller"));
            if (mine == own && this.matches(id, view.filter)) {
                all.add(id);
            }
        }
        if (view.sort == 1 || view.sort == 2) {
            // Stueckpreis: guenstigste bzw. teuerste zuerst
            all.sort((a, b) -> {
                int c = Double.compare(this.unitPrice(a), this.unitPrice(b));
                return view.sort == 1 ? c : -c;
            });
        }
        int pages = Math.max(1, (all.size() + PAGE - 1) / PAGE);
        view.page = Math.max(0, Math.min(page, pages - 1));
        Inventory inv = Gui.frame(this.plugin, TAG, (mine ? "Meine Angebote" : "Angebote") + ("bau".equals(myZone) ? " (Bau)" : ""), 54);
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, null);
        }
        for (int i = 0; i < PAGE; i++) {
            int index = view.page * PAGE + i;
            if (index >= all.size()) {
                break;
            }
            String id = all.get(index);
            String p = "listings." + id;
            ItemStack stack = this.data.getItemStack(p + ".item");
            if (stack == null) {
                view.ids.add(null);
                continue;
            }
            ItemStack shown = stack.clone();
            ItemMeta meta = shown.getItemMeta();
            List<String> lore = new ArrayList<String>();
            if (meta != null && meta.getLore() != null) {
                lore.addAll(meta.getLore());
            }
            lore.add("");
            lore.add("§7Preis: §a" + Gui.money(this.data.getLong(p + ".price", 0L)));
            if (shown.getAmount() > 1) {
                lore.add("§7Pro Stück: §a" + Gui.money(Math.round(this.data.getLong(p + ".price", 0L) / (double) shown.getAmount())));
            }
            lore.add("§7Verkäufer: §f" + this.data.getString(p + ".name", "?"));
            long leftMin = Math.max(0L, (this.data.getLong(p + ".expires", 0L) - System.currentTimeMillis()) / 60000L);
            lore.add("§8Läuft ab in " + leftMin / 60L + " Std " + leftMin % 60L + " Min");
            lore.add(mine ? "§e▶ Klicken: zurückholen" : "§e▶ Klicken: kaufen");
            if (meta != null) {
                meta.setLore(lore);
                shown.setItemMeta(meta);
            }
            inv.setItem(i, shown);
            view.ids.add(id);
        }
        // Platzhalter, damit Listenpositionen stimmen
        while (view.ids.size() < PAGE) {
            view.ids.add(null);
        }
        inv.setItem(45, Gui.item(Material.ARROW, "§7« Vorherige Seite"));
        inv.setItem(46, Gui.item(Material.PAPER, "§dSeite " + (view.page + 1) + "/" + pages));
        inv.setItem(47, Gui.item(Material.GOLD_INGOT, "§6§lDein Geld", "§a" + Gui.money(Economy.balance(me))));
        inv.setItem(49, mine ? Gui.item(Material.CHEST, "§d§lAlle Angebote", "§e▶ Klicken") : Gui.item(Material.ENDER_CHEST, "§d§lMeine Angebote", "§7" + this.countOf(me) + "/" + this.maxListings() + " aktiv", "§e▶ Klicken"));
        inv.setItem(50, Gui.item(Material.COMPARATOR, "§e§lSortierung", "§7Aktuell: §f" + (view.sort == 1 ? "günstigster Stückpreis" : view.sort == 2 ? "teuerster Stückpreis" : "neueste zuerst"), "§e▶ Klicken: wechseln", view.filter.isEmpty() ? "§8Suche: §f/ah search <Text>" : "§7Suche: §f" + view.filter));
        inv.setItem(51, Gui.item(Material.HOPPER, "§a§lAbholen", "§7Abgelaufene Items: §f" + this.claims(me, this.zone(player)).size(), "§e▶ Klicken"));
        inv.setItem(52, Gui.item(Material.BOOK, "§f§lVerkaufen", "§7Item einlegen, Preis wählen,", "§7bestätigen – ganz ohne Befehl.", "§e▶ Klicken"));
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
                    this.claim(player);
                }
                case 50 -> {
                    view.sort = (view.sort + 1) % 3;
                    this.open(player, view.mine, 0);
                }
                case 52 -> {
                    if (Wizard.instance != null) {
                        Wizard.instance.start(player, Wizard.Mode.AH);
                    }
                }
                default -> { }
            }
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || slot >= view.ids.size() || view.ids.get(slot) == null) {
            return;
        }
        String id = view.ids.get(slot);
        String p = "listings." + id;
        if (!this.data.contains(p)) {
            player.sendMessage("§cDieses Angebot gibt es nicht mehr.");
            this.open(player, view.mine, view.page);
            return;
        }
        ItemStack stack = this.data.getItemStack(p + ".item");
        if (stack == null) {
            return;
        }
        UUID me = player.getUniqueId();
        String sellerText = this.data.getString(p + ".seller", "");
        if (view.mine) {
            if (!sellerText.equals(me.toString())) {
                return;
            }
            this.data.set(p, null);
            int left = Gui.give(player, stack.clone());
            if (left > 0) {
                ItemStack remainder = stack.clone();
                remainder.setAmount(left);
                this.addClaim(me, remainder, this.zone(player));
                player.sendMessage("§eInventar voll - Rest mit /ah claim abholen.");
            } else {
                player.sendMessage("§aAngebot zurückgeholt.");
            }
            this.save();
            this.open(player, true, view.page);
            return;
        }
        if (sellerText.equals(me.toString())) {
            player.sendMessage("§cDas ist dein eigenes Angebot (siehe „Meine Angebote“).");
            return;
        }
        long price = this.data.getLong(p + ".price", 0L);
        String sellerName = this.data.getString(p + ".name", null);
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("§cDein Inventar ist voll.");
            return;
        }
        UUID seller;
        try {
            seller = UUID.fromString(sellerText);
        }
        catch (IllegalArgumentException ex) {
            player.sendMessage("§cDieses Angebot hat einen ungültigen Verkäufer und wurde gesperrt.");
            return;
        }
        long tax = Math.max(0L, Math.min(50L, this.plugin.getConfig().getLong("ah.tax-percent", 0L)));
        long proceeds = Rules.proceeds(price, tax);
        boolean bauListing = "bau".equals(this.data.getString(p + ".zone", "smp"));
        String tx = Economy.beginJournal("AH_BUY", "listing=" + id + ";buyer=" + me + ";seller=" + seller + ";amount=" + price + ";proceeds=" + proceeds + ";bau=" + (bauListing ? "1" : "0"));
        if (tx == null) {
            player.sendMessage("§cDie Economy-Datenbank ist momentan nicht verfügbar.");
            return;
        }
        if (!Economy.debitOnce(Economy.key(me, bauListing), price, tx + ":debit")) {
            Chat.no(player);
            player.sendMessage("§cDu hast nicht genug Geld (§f" + Gui.money(price) + "§c).");
            return;
        }
        // Reihenfolge: Angebot entfernen und speichern, dann ausliefern, dann Verkaeufer bezahlen
        this.data.set(p, null);
        this.save();
        Store.flush();
        this.signal(stack, price, true);
        int notFit = Gui.give(player, stack.clone());
        if (notFit > 0) {
            // darf eigentlich nicht passieren (Platz wurde geprueft) - Rest geht sicher in die Abholung statt verloren
            ItemStack rest = stack.clone();
            rest.setAmount(notFit);
            this.addClaim(me, rest, this.zone(player));
            player.sendMessage("§eNicht alles passte ins Inventar - Rest mit /ah claim abholen.");
            this.save();
        }
        Audit.log("AH-BUY", player.getName() + " (" + me + ") kauft " + stack.getAmount() + "x " + stack.getType() + " von " + sellerText + " fuer " + price + " (Verkaeufer erhaelt " + proceeds + ")");
        if (!Economy.creditOnce(Economy.key(seller, bauListing), sellerName, proceeds, tx + ":credit")) {
            player.sendMessage("§eVerkäufergutschrift wartet auf Recovery. Bitte nicht erneut kaufen.");
            return;
        }
        Economy.completeJournal(tx);
        String note = "§a" + player.getName() + " hat §f" + stack.getAmount() + "x " + Gui.prettyName(stack.getType()) + " §afür " + Gui.money(price) + " gekauft!"
                + (tax > 0L ? " §7(Gebühr " + tax + "%: Du erhältst " + Gui.money(proceeds) + ")" : "");
        Player online = this.plugin.getServer().getPlayer(seller);
        if (online != null) {
            online.sendMessage(note);
        } else {
            // Verkaeufer offline: Meldung merken und beim naechsten Beitritt zeigen
            List<String> notes = new ArrayList<String>(this.data.getStringList("notify." + seller));
            if (notes.size() < 20) {
                notes.add(note);
            }
            this.data.set("notify." + seller, notes);
            this.save();
        }
        Chat.coins(player);
        player.sendMessage("§7Gekauft: §f" + stack.getAmount() + "x " + Gui.prettyName(stack.getType()) + " §7für §a" + Gui.money(price));
        this.open(player, false, view.page);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        final Player joined = event.getPlayer();
        if (!this.data.contains("notify." + joined.getUniqueId())) {
            return;
        }
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            List<String> notes = this.data.getStringList("notify." + joined.getUniqueId());
            if (joined.isOnline() && !notes.isEmpty()) {
                joined.sendMessage("§6Auktionshaus: §7Während du weg warst:");
                for (String note : notes) {
                    joined.sendMessage(note);
                }
            }
            this.data.set("notify." + joined.getUniqueId(), null);
            this.save();
        }, 60L);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            out.add("sell");
            out.add("mine");
            out.add("claim");
        }
        return out;
    }
}
