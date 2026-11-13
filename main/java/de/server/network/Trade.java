package de.server.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Sicherer Handel zwischen zwei Spielern: /trade &lt;Spieler&gt;, der andere nimmt mit /trade accept an.
 * Links legt man Items (und per /trade money &lt;Betrag&gt; Geld) ab, rechts sieht man das Angebot des anderen.
 * Beide muessen bestaetigen; jede Aenderung nimmt beide Bestaetigungen zurueck. Beim Schliessen wird alles zurueckgegeben.
 * Nur im SMP bzw. Bau-SMP und nur im gleichen Modus (Geld ist modusgebunden).
 */
final class Trade implements CommandExecutor, TabCompleter, Listener {
    static Trade instance;

    private static final String TAG = "Handel";
    private static final long REQUEST_MS = 60000L;
    private static final int[] OWN = slots(0);
    private static final int[] THEIRS = slots(5);
    private static final int READY = 39;
    private static final int CANCEL = 36;

    private static int[] slots(int firstCol) {
        int[] out = new int[16];
        int i = 0;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                out[i++] = row * 9 + firstCol + col;
            }
        }
        return out;
    }

    private static final class Session {
        final UUID a;
        final UUID b;
        Inventory invA;
        Inventory invB;
        long moneyA;
        long moneyB;
        boolean readyA;
        boolean readyB;
        boolean done;

        Session(UUID a, UUID b) {
            this.a = a;
            this.b = b;
        }

        UUID other(UUID me) {
            return me.equals(this.a) ? this.b : this.a;
        }
    }

    private final DuelNetworkPlugin plugin;
    private final Map<UUID, Session> sessions = new HashMap<UUID, Session>();
    /** Empfaenger -> Anfrage-Absender. */
    private final Map<UUID, UUID> requests = new HashMap<UUID, UUID>();
    private final Map<UUID, Long> requestTimes = new HashMap<UUID, Long>();

    Trade(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    private boolean eligible(Player player) {
        return "smp".equals(this.plugin.profiles.getOrDefault(player.getUniqueId(), "lobby")) && !this.plugin.inActiveMatch(player.getUniqueId());
    }

    /* ---------- Befehl ---------- */

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.get("trade.nur-im-spiel"));
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(Messages.get("trade.trade-spieler-handel-anfragen"));
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("accept")) {
            this.accept(player);
        } else if (sub.equals("money")) {
            this.setMoney(player, args.length < 2 ? "" : args[1]);
        } else if (sub.equals("cancel")) {
            Session session = this.sessions.get(player.getUniqueId());
            if (session != null) {
                this.cancel(session, "§c" + player.getName() + " hat den Handel abgebrochen.");
            }
        } else {
            Player target = this.plugin.getServer().getPlayerExact(args[0]);
            if (target == null || target.equals(player)) {
                player.sendMessage(Messages.get("trade.spieler-nicht-gefunden"));
            } else {
                this.request(player, target);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            for (String sub : List.of("accept", "money", "cancel")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
            for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)) && !online.equals(sender)) {
                    out.add(online.getName());
                }
            }
        }
        return out;
    }

    private void request(Player player, Player target) {
        if (!this.eligible(player) || !this.eligible(target)) {
            player.sendMessage(Messages.get("trade.handeln-geht-nur-im"));
            return;
        }
        if (this.plugin.inBauMode(player.getUniqueId()) != this.plugin.inBauMode(target.getUniqueId())) {
            player.sendMessage(Messages.get("trade.ihr-musst-im-gleichen"));
            return;
        }
        if (this.sessions.containsKey(player.getUniqueId()) || this.sessions.containsKey(target.getUniqueId())) {
            player.sendMessage(Messages.get("trade.einer-von-euch-handelt"));
            return;
        }
        this.requests.put(target.getUniqueId(), player.getUniqueId());
        this.requestTimes.put(target.getUniqueId(), System.currentTimeMillis());
        player.sendMessage("§aHandelsanfrage an §f" + target.getName() + " §agesendet.");
        target.sendMessage("§d✦ §f" + player.getName() + " §7möchte mit dir handeln. §a/trade accept §7(60 Sekunden)");
        Chat.ping(target);
    }

    private void accept(Player target) {
        UUID from = this.requests.remove(target.getUniqueId());
        Long time = this.requestTimes.remove(target.getUniqueId());
        Player sender = from == null ? null : this.plugin.getServer().getPlayer(from);
        if (sender == null || time == null || System.currentTimeMillis() - time > REQUEST_MS) {
            target.sendMessage(Messages.get("trade.keine-gultige-handelsanfrage"));
            return;
        }
        if (!this.eligible(sender) || !this.eligible(target) || this.plugin.inBauMode(sender.getUniqueId()) != this.plugin.inBauMode(target.getUniqueId())
                || this.sessions.containsKey(sender.getUniqueId()) || this.sessions.containsKey(target.getUniqueId())) {
            target.sendMessage(Messages.get("trade.der-handel-ist-gerade"));
            return;
        }
        Session session = new Session(sender.getUniqueId(), target.getUniqueId());
        this.sessions.put(session.a, session);
        this.sessions.put(session.b, session);
        session.invA = this.build(session, sender, target);
        session.invB = this.build(session, target, sender);
        sender.openInventory(session.invA);
        target.openInventory(session.invB);
        this.refresh(session);
    }

    private void setMoney(Player player, String text) {
        Session session = this.sessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage(Messages.get("trade.du-handelst-gerade-nicht"));
            return;
        }
        long amount = Gui.parseAmount(text);
        if (amount < 0L) {
            player.sendMessage(Messages.get("trade.trade-money-betrag"));
            return;
        }
        if (amount > 0L && !Economy.has(player.getUniqueId(), amount)) {
            player.sendMessage(Messages.get("trade.so-viel-geld-hast"));
            return;
        }
        if (player.getUniqueId().equals(session.a)) {
            session.moneyA = amount;
        } else {
            session.moneyB = amount;
        }
        session.readyA = false;
        session.readyB = false;
        this.refresh(session);
    }

    /* ---------- Menue ---------- */

    private Inventory build(Session session, Player me, Player other) {
        Inventory inv = this.plugin.getServer().createInventory(null, 54, "§8" + TAG + " §8» §d" + me.getName() + " §8↔ §d" + other.getName());
        ItemStack divider = Gui.item(Material.PURPLE_STAINED_GLASS_PANE, " ");
        ItemStack filler = Gui.item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, i % 9 == 4 ? divider.clone() : filler.clone());
        }
        for (int slot : OWN) {
            inv.setItem(slot, null);
        }
        for (int slot : THEIRS) {
            inv.setItem(slot, null);
        }
        inv.setItem(CANCEL, Gui.item(Material.BARRIER, "§c§lAbbrechen"));
        return inv;
    }

    private Inventory invOf(Session session, UUID id) {
        return id.equals(session.a) ? session.invA : session.invB;
    }

    private ItemStack[] offer(Session session, UUID id) {
        Inventory inv = this.invOf(session, id);
        ItemStack[] out = new ItemStack[OWN.length];
        for (int i = 0; i < OWN.length; i++) {
            out[i] = inv == null ? null : inv.getItem(OWN[i]);
        }
        return out;
    }

    /** Spiegelt die Angebote in beide Menues und setzt Geld-/Bestaetigungsanzeigen. */
    private void refresh(Session session) {
        for (UUID id : new UUID[]{session.a, session.b}) {
            UUID other = session.other(id);
            Inventory inv = this.invOf(session, id);
            ItemStack[] theirs = this.offer(session, other);
            for (int i = 0; i < THEIRS.length; i++) {
                inv.setItem(THEIRS[i], theirs[i] == null ? null : theirs[i].clone());
            }
            boolean mine = id.equals(session.a) ? session.readyA : session.readyB;
            boolean his = id.equals(session.a) ? session.readyB : session.readyA;
            long myMoney = id.equals(session.a) ? session.moneyA : session.moneyB;
            long hisMoney = id.equals(session.a) ? session.moneyB : session.moneyA;
            inv.setItem(READY, Gui.item(mine ? Material.LIME_CONCRETE : Material.RED_CONCRETE, mine ? "§a§lBestätigt" : "§c§lNoch nicht bestätigt",
                    "§7Klicken zum " + (mine ? "Zurücknehmen" : "Bestätigen")));
            inv.setItem(38, Gui.item(Material.GOLD_INGOT, "§6Dein Geld: §f" + Gui.money(myMoney), "§8/trade money <Betrag>"));
            inv.setItem(42, Gui.item(Material.GOLD_INGOT, "§6Sein Geld: §f" + Gui.money(hisMoney)));
            inv.setItem(41, Gui.item(his ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE, his ? "§aGegenüber hat bestätigt" : "§7Gegenüber wartet noch"));
            Player online = this.plugin.getServer().getPlayer(id);
            if (online != null) {
                online.updateInventory();
            }
        }
    }

    private boolean isOwn(int slot) {
        for (int s : OWN) {
            if (s == slot) {
                return true;
            }
        }
        return false;
    }

    private Session sessionOf(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !Gui.isTitle(event.getView().getTitle(), TAG)) {
            return null;
        }
        return this.sessions.get(player.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!Gui.isTitle(event.getView().getTitle(), TAG)) {
            return;
        }
        Player player = (Player)event.getWhoClicked();
        Session session = this.sessions.get(player.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            return;
        }
        ClickType click = event.getClick();
        if (click == ClickType.DOUBLE_CLICK || event.getAction() == InventoryAction.COLLECT_TO_CURSOR || click == ClickType.CREATIVE) {
            event.setCancelled(true);
            return;
        }
        boolean top = event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory());
        int slot = event.getRawSlot();
        if (!top) {
            // Klick im eigenen Inventar: Shift-Klick selbst in das Angebot verschieben, sonst normal
            if (click.isShiftClick()) {
                event.setCancelled(true);
                ItemStack stack = event.getCurrentItem();
                if (stack != null && !stack.getType().isAir()) {
                    Inventory inv = this.invOf(session, player.getUniqueId());
                    for (int s : OWN) {
                        if (inv.getItem(s) == null) {
                            inv.setItem(s, stack.clone());
                            event.setCurrentItem(null);
                            this.changed(session);
                            break;
                        }
                    }
                }
            }
            return;
        }
        if (slot == CANCEL) {
            event.setCancelled(true);
            this.cancel(session, "§c" + player.getName() + " hat den Handel abgebrochen.");
            return;
        }
        if (slot == READY) {
            event.setCancelled(true);
            boolean mine = player.getUniqueId().equals(session.a);
            if (mine) {
                session.readyA = !session.readyA;
            } else {
                session.readyB = !session.readyB;
            }
            this.refresh(session);
            if (session.readyA && session.readyB) {
                this.complete(session);
            }
            return;
        }
        if (!this.isOwn(slot)) {
            event.setCancelled(true);
            return;
        }
        // normale Platzierung im eigenen Angebotsbereich, danach synchronisieren
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
            if (!session.done) {
                this.changed(session);
            }
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (Gui.isTitle(event.getView().getTitle(), TAG)) {
            event.setCancelled(true);
        }
    }

    private void changed(Session session) {
        session.readyA = false;
        session.readyB = false;
        this.refresh(session);
    }

    /* ---------- Abschluss ---------- */

    private int freeSlots(Player player) {
        int free = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType().isAir()) {
                free++;
            }
        }
        return free;
    }

    private static int count(ItemStack[] items) {
        int n = 0;
        for (ItemStack stack : items) {
            if (stack != null && !stack.getType().isAir()) {
                n++;
            }
        }
        return n;
    }

    private void complete(Session session) {
        Player pa = this.plugin.getServer().getPlayer(session.a);
        Player pb = this.plugin.getServer().getPlayer(session.b);
        if (pa == null || pb == null) {
            this.cancel(session, "§cDer Handel wurde abgebrochen.");
            return;
        }
        ItemStack[] offerA = this.offer(session, session.a);
        ItemStack[] offerB = this.offer(session, session.b);
        if (!Economy.has(session.a, session.moneyA) || !Economy.has(session.b, session.moneyB)) {
            this.changed(session);
            pa.sendMessage(Messages.get("trade.ein-geldbetrag-ist-nicht"));
            pb.sendMessage(Messages.get("trade.ein-geldbetrag-ist-nicht-2"));
            return;
        }
        // Platz: die eigenen Angebotsitems kommen nicht zurueck, also zaehlt nur, was dazukommt
        if (count(offerB) > this.freeSlots(pa) || count(offerA) > this.freeSlots(pb)) {
            this.changed(session);
            pa.sendMessage(Messages.get("trade.ein-inventar-hat-nicht"));
            pb.sendMessage(Messages.get("trade.ein-inventar-hat-nicht-2"));
            return;
        }
        session.done = true;
        this.sessions.remove(session.a);
        this.sessions.remove(session.b);
        Inventory invA = session.invA;
        Inventory invB = session.invB;
        for (int s : OWN) {
            invA.setItem(s, null);
            invB.setItem(s, null);
        }
        if (session.moneyA > 0L && Economy.take(session.a, session.moneyA)) {
            Economy.add(session.b, pb.getName(), session.moneyA);
        }
        if (session.moneyB > 0L && Economy.take(session.b, session.moneyB)) {
            Economy.add(session.a, pa.getName(), session.moneyB);
        }
        for (ItemStack stack : offerB) {
            if (stack != null && !stack.getType().isAir()) {
                pa.getInventory().addItem(stack);
            }
        }
        for (ItemStack stack : offerA) {
            if (stack != null && !stack.getType().isAir()) {
                pb.getInventory().addItem(stack);
            }
        }
        Store.append(new java.io.File(this.plugin.getDataFolder(), "trades.log"), new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new java.util.Date())
                + " " + pa.getName() + " gibt [" + describe(offerA, session.moneyA) + "] an " + pb.getName() + " und erhaelt [" + describe(offerB, session.moneyB) + "]");
        this.plugin.getLogger().info("Handel: " + pa.getName() + " (" + session.moneyA + ") <-> " + pb.getName() + " (" + session.moneyB + ") abgeschlossen.");
        pa.closeInventory();
        pb.closeInventory();
        pa.sendMessage("§a✔ Handel mit " + pb.getName() + " abgeschlossen.");
        pb.sendMessage("§a✔ Handel mit " + pa.getName() + " abgeschlossen.");
        Chat.yes(pa);
        Chat.yes(pb);
    }

    private static String describe(ItemStack[] items, long money) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack stack : items) {
            if (stack != null && !stack.getType().isAir()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(stack.getAmount()).append("x ").append(stack.getType().name());
            }
        }
        if (money > 0L) {
            sb.append(sb.length() > 0 ? ", " : "").append(money).append(" Geld");
        }
        return sb.length() == 0 ? "nichts" : sb.toString();
    }

    /** Bricht ab und gibt jedem seine Items zurueck. */
    private void cancel(Session session, String message) {
        if (session.done) {
            return;
        }
        session.done = true;
        this.sessions.remove(session.a);
        this.sessions.remove(session.b);
        for (UUID id : new UUID[]{session.a, session.b}) {
            Player player = this.plugin.getServer().getPlayer(id);
            Inventory inv = this.invOf(session, id);
            for (int s : OWN) {
                ItemStack stack = inv.getItem(s);
                inv.setItem(s, null);
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }
                if (player != null) {
                    for (ItemStack left : player.getInventory().addItem(stack).values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), left);
                    }
                } else {
                    this.plugin.getLogger().warning("Handel abgebrochen, Spieler offline - Items verloren: " + id + " " + stack);
                }
            }
            if (player != null) {
                player.sendMessage(message);
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player) || !Gui.isTitle(event.getView().getTitle(), TAG)) {
            return;
        }
        Session session = this.sessions.get(player.getUniqueId());
        if (session != null) {
            this.cancel(session, "§c" + player.getName() + " hat den Handel geschlossen.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        this.requests.remove(id);
        this.requestTimes.remove(id);
        Session session = this.sessions.get(id);
        if (session != null) {
            this.cancel(session, "§c" + event.getPlayer().getName() + " hat das Spiel verlassen.");
        }
    }

    void shutdown() {
        for (Session session : new ArrayList<Session>(this.sessions.values())) {
            this.cancel(session, "§cDer Handel wurde wegen eines Neustarts abgebrochen.");
        }
    }
}
