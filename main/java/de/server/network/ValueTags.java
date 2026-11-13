package de.server.network;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Zeigt im SMP unter dem Item-Namen den Verkaufswert pro Stueck. Die Anzeige bleibt unabhaengig von der Stackgroesse,
 * damit gleichartige Items in Inventaren und auf dem Boden weiterhin stapelbar sind. Ausserhalb des SMP (Lobby, Duel,
 * Parkour ...) wird die Zeile wieder entfernt. Dazu: stellt sicher, dass unsere Befehle (/shop, /ah ...) nicht von
 * Befehlen anderer Plugins ueberdeckt werden.
 */
final class ValueTags implements Listener {
    private static final Set<String> OWN = Set.of("shop", "sell", "sale", "ah", "auction", "auctionhouse", "order", "orders", "money", "bal", "balance",
            "pay", "baltop", "balancetop", "rtp", "wild", "sidebar");

    private final DuelNetworkPlugin plugin;

    ValueTags(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("ValueTags", () -> this.tick()), 40L, 20L);
    }

    /** Stapel ohne unsere Wert-Zeile und mit Menge 1 (zum Vergleichen, ob zwei Staepel dasselbe Item sind). */
    private static ItemStack clean(ItemStack stack) {
        ItemStack c = stack.clone();
        c.setAmount(1);
        if (c.hasItemMeta()) {
            ItemMeta meta = c.getItemMeta();
            if (Gui.hasMark(meta)) {
                List<String> lore = Gui.loreWithoutMark(meta);
                meta.setLore(lore.isEmpty() ? null : lore);
                c.setItemMeta(meta);
            }
        }
        return c;
    }

    /**
     * Aufheben im SMP: Die Wert-Zeile im Item verhindert, dass Vanilla neue Items auf einen vorhandenen Stapel legt.
     * Darum wird NUR beim Aufheben das aufgesammelte Item direkt auf vorhandene, noch nicht volle Staepel desselben Items gelegt
     * (kein Zusammenfuehren anderer Staepel, nichts beim Rausnehmen aus Kisten oder beim Verschieben im Inventar).
     * Was nicht auf einen Stapel passt, nimmt Vanilla wie gewohnt in einen freien Slot auf.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player) || !"smp".equals(this.plugin.profiles.getOrDefault(player.getUniqueId(), "lobby"))) {
            return;
        }
        org.bukkit.entity.Item entity = event.getItem();
        ItemStack incoming = entity.getItemStack();
        int max = incoming.getMaxStackSize();
        if (max > 1) {
            int remaining = incoming.getAmount();
            ItemStack probe = clean(incoming);
            org.bukkit.inventory.PlayerInventory inv = player.getInventory();
            // Hotbar zuerst, dann der Rest (wie Vanilla)
            int[] order = new int[36];
            for (int i = 0; i < 36; i++) {
                order[i] = i;
            }
            for (int i = 0; i < 36 && remaining > 0; i++) {
                int slot = order[i];
                ItemStack stack = inv.getItem(slot);
                if (stack == null || stack.getAmount() >= max || !clean(stack).isSimilar(probe)) {
                    continue;
                }
                int put = Math.min(max - stack.getAmount(), remaining);
                ItemStack updated = stack.clone();
                updated.setAmount(stack.getAmount() + put);
                inv.setItem(slot, updated);
                remaining -= put;
            }
            if (remaining < incoming.getAmount()) {
                event.setCancelled(true);
                if (remaining <= 0) {
                    try {
                        player.playPickupItemAnimation(entity, incoming.getAmount());
                    }
                    catch (Throwable ignored) {
                        // nur Optik
                    }
                    entity.remove();
                } else {
                    ItemStack rest = incoming.clone();
                    rest.setAmount(remaining);
                    entity.setItemStack(rest);
                }
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.2f, 1.6f);
            }
        }
        // Wert-Zeile sofort aktualisieren (nicht erst beim naechsten Durchlauf)
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            try {
                for (int i = 0; i <= 40; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack != null) {
                        this.apply(stack, true);
                    }
                }
            }
            catch (Throwable t) {
                Store.problem("ValueTags:pickup", t);
            }
        });
    }

    private int round;

    /** Jeder Spieler kommt nur alle performance.value-tags-seconds Sekunden dran (Standard 3), dafuer pro Tick nur ein Teil der Spieler. */
    private void tick() {
        int div = Math.max(1, this.plugin.getConfig().getInt("performance.value-tags-seconds", 3));
        int turn = this.round++ % div;
        int index = 0;
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            if (index++ % div != turn) {
                continue;
            }
            try {
                boolean smp = "smp".equals(this.plugin.profiles.getOrDefault(player.getUniqueId(), "lobby"));
                for (int i = 0; i <= 40; i++) {
                    ItemStack stack = player.getInventory().getItem(i);
                    if (stack == null || (!smp && !stack.hasItemMeta())) {
                        continue;
                    }
                    this.apply(stack, smp);
                }
            }
            catch (Throwable ignored) {
                // nur Anzeige
                Store.problem("ValueTags:56", ignored);
            }
        }
    }

    private void apply(ItemStack stack, boolean show) {
        Economy economy = Economy.instance;
        if (economy == null) {
            return;
        }
        boolean has = false;
        ItemMeta meta = stack.hasItemMeta() ? stack.getItemMeta() : null;
        if (meta != null) {
            has = Gui.hasMark(meta);
        }
        boolean want = show && economy.sellable(stack);
        if (want) {
            List<String> add = new ArrayList<String>();
            if (Economy.isEmptyShulker(stack)) {
                add.add(Gui.MARK + Gui.money(economy.worth(stack, false, null)) + " §8leere Box (wird verkauft)");
            } else if (Economy.isShulker(stack)) {
                add.add(Gui.MARK + Gui.money(economy.worth(stack, false, null)) + " §8Inhalt (wird verkauft, Box bleibt)");
            } else {
                long single = economy.worthOf(stack, 1);
                boolean special = !stack.getEnchantments().isEmpty() || (meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta);
                add.add(Gui.MARK + Gui.money(single) + (special ? " §8pro Stück §d✦ verzaubert" : " §8pro Stück"));
            }
            if (meta == null) {
                meta = stack.getItemMeta();
            }
            if (meta == null) {
                return;
            }
            List<String> lore = Gui.loreWithoutMark(meta);
            List<String> current = meta.getLore();
            if (has && current != null && current.size() >= add.size() && current.subList(current.size() - add.size(), current.size()).equals(add)
                    && current.size() - add.size() == lore.size()) {
                return;
            }
            lore.addAll(add);
            meta.setLore(lore);
            stack.setItemMeta(meta);
        } else if (has) {
            List<String> lore = Gui.loreWithoutMark(meta);
            meta.setLore(lore.isEmpty() ? null : lore);
            stack.setItemMeta(meta);
        }
    }

    private static boolean realContainer(String title) {
        return title == null || title.indexOf('\u00bb') < 0;
    }

    private void markTop(Inventory top, boolean show) {
        if (top == null) {
            return;
        }
        for (int i = 0; i < top.getSize(); i++) {
            ItemStack stack = top.getItem(i);
            if (stack == null || (!show && !stack.hasItemMeta())) {
                continue;
            }
            this.apply(stack, show);
        }
    }

    /** Dezenter Klick-Ton in allen Plugin-Menues (Titel enthaelt den "»"-Trenner). */
    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        try {
            String title = event.getView().getTitle();
            if (title == null || title.indexOf('\u00bb') < 0 || event.getCurrentItem() == null || event.getRawSlot() >= event.getView().getTopInventory().getSize()
                    || !(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            Chat.click(player);
        }
        catch (Throwable ignored) {
            // nur Ton
            Store.problem("ValueTags:182", ignored);
        }
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        try {
            if (!realContainer(event.getView().getTitle()) || !"smp".equals(this.plugin.profiles.getOrDefault(event.getPlayer().getUniqueId(), "lobby"))) {
                return;
            }
            this.markTop(event.getView().getTopInventory(), true);
        }
        catch (Throwable ignored) {
            // nur Anzeige
            Store.problem("ValueTags:195", ignored);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        try {
            if (!realContainer(event.getView().getTitle())) {
                return;
            }
            this.markTop(event.getView().getTopInventory(), false);
        }
        catch (Throwable ignored) {
            // nur Anzeige
            Store.problem("ValueTags:208", ignored);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || message.length() < 2 || message.charAt(0) != '/') {
            return;
        }
        String body = message.substring(1);
        int space = body.indexOf(' ');
        String label = (space < 0 ? body : body.substring(0, space)).toLowerCase(Locale.ROOT);
        if (label.indexOf(':') >= 0 || !OWN.contains(label)) {
            return;
        }
        // Namensraum erzwingen, damit kein anderes Plugin denselben Befehl uebernimmt
        event.setMessage("/duelnetwork:" + label + (space < 0 ? "" : body.substring(space)));
    }
}
