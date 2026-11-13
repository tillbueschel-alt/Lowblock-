package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

/**
 * Freundesliste: /friend (Menue), add/accept/deny/remove/list, Online-Meldungen, Teleport-Anfrage und
 * Duell- bzw. Party-Einladung direkt aus dem Menue.
 * Menue: Linksklick = Teleport-Anfrage, Rechtsklick = Duell, Shift+Linksklick = Party-Einladung, Shift+Rechtsklick = entfernen.
 */
final class Friends implements CommandExecutor, TabCompleter, Listener {
    static Friends instance;

    private static final String TAG = "Freunde";
    private static final int MAX_FRIENDS = 45;
    private static final long TP_TIMEOUT_MS = 60000L;

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    /** Empfaenger -> Absender einer Teleport-Anfrage. */
    private final Map<UUID, UUID> tpRequests = new HashMap<UUID, UUID>();
    private final Map<UUID, Long> tpTimes = new HashMap<UUID, Long>();

    Friends(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.file = new File(plugin.getDataFolder(), "friends.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("friends.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    /* ---------- Daten ---------- */

    private List<UUID> list(UUID id, String key) {
        List<UUID> out = new ArrayList<UUID>();
        for (String raw : this.data.getStringList("players." + id + "." + key)) {
            try {
                out.add(UUID.fromString(raw));
            }
            catch (IllegalArgumentException ignored) {
                // kaputter Eintrag
            }
        }
        return out;
    }

    private void store(UUID id, String key, List<UUID> values) {
        List<String> raw = new ArrayList<String>();
        for (UUID value : values) {
            raw.add(value.toString());
        }
        this.data.set("players." + id + "." + key, raw);
    }

    /** Alle gerade online befindlichen Freunde. */
    List<Player> onlineFriends(UUID id) {
        List<Player> out = new ArrayList<Player>();
        for (UUID friend : this.list(id, "friends")) {
            Player online = this.plugin.getServer().getPlayer(friend);
            if (online != null) {
                out.add(online);
            }
        }
        return out;
    }

    private boolean notifyOn(UUID id) {
        return this.data.getBoolean("players." + id + ".notify", true);
    }

    /** Freunde eines Spielers (fuer das Claim-Menue). */
    List<UUID> friendsOf(UUID id) {
        return this.list(id, "friends");
    }

    boolean areFriends(UUID a, UUID b) {
        return this.list(a, "friends").contains(b);
    }

    private String nameOf(UUID id) {
        Player online = this.plugin.getServer().getPlayer(id);
        if (online != null) {
            return online.getName();
        }
        String stored = this.data.getString("names." + id);
        if (stored != null) {
            return stored;
        }
        OfflinePlayer off = this.plugin.getServer().getOfflinePlayer(id);
        return off.getName() == null ? id.toString().substring(0, 8) : off.getName();
    }

    private OfflinePlayer lookup(String name) {
        Player online = this.plugin.getServer().getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return this.plugin.getServer().getOfflinePlayerIfCached(name);
    }

    private void addFriend(UUID a, UUID b) {
        List<UUID> la = this.list(a, "friends");
        if (!la.contains(b)) {
            la.add(b);
        }
        this.store(a, "friends", la);
        List<UUID> lb = this.list(b, "friends");
        if (!lb.contains(a)) {
            lb.add(a);
        }
        this.store(b, "friends", lb);
        this.dropRequest(a, b);
        this.dropRequest(b, a);
    }

    private void dropRequest(UUID owner, UUID from) {
        List<UUID> in = this.list(owner, "requests");
        if (in.remove(from)) {
            this.store(owner, "requests", in);
        }
    }

    /* ---------- Befehle ---------- */

    private void request(Player player, String name) {
        OfflinePlayer target = this.lookup(name);
        if (target == null || target.getUniqueId() == null) {
            player.sendMessage(Messages.get("friends.spieler-nicht-gefunden-er"));
            return;
        }
        UUID me = player.getUniqueId();
        UUID other = target.getUniqueId();
        if (me.equals(other)) {
            player.sendMessage(Messages.get("friends.du-kannst-dich-nicht"));
            return;
        }
        if (this.areFriends(me, other)) {
            player.sendMessage(Messages.get("friends.ihr-seid-schon-befreundet"));
            return;
        }
        if (this.list(me, "friends").size() >= MAX_FRIENDS) {
            player.sendMessage("§cDeine Freundesliste ist voll (" + MAX_FRIENDS + ").");
            return;
        }
        // Hat der andere mir schon eine Anfrage geschickt? Dann direkt annehmen.
        if (this.list(me, "requests").contains(other)) {
            this.addFriend(me, other);
            this.save();
            player.sendMessage("§a" + this.nameOf(other) + " ist jetzt dein Freund.");
            Player online = target.getPlayer();
            if (online != null) {
                online.sendMessage("§a" + player.getName() + " ist jetzt dein Freund.");
            }
            return;
        }
        List<UUID> theirs = this.list(other, "requests");
        if (theirs.contains(me)) {
            player.sendMessage(Messages.get("friends.die-anfrage-ist-schon"));
            return;
        }
        theirs.add(me);
        this.store(other, "requests", theirs);
        this.data.set("names." + me, player.getName());
        this.data.set("names." + other, target.getName() == null ? name : target.getName());
        this.save();
        player.sendMessage("§aFreundschaftsanfrage an §f" + this.nameOf(other) + " §agesendet.");
        Player online = target.getPlayer();
        if (online != null) {
            online.sendMessage("§d✦ §f" + player.getName() + " §7möchte dein Freund sein. §a/friend accept " + player.getName() + " §7oder §c/friend deny " + player.getName() + "§7.");
            Chat.ping(online);
        }
    }

    private UUID resolveKnown(Player player, String name, String key) {
        for (UUID id : this.list(player.getUniqueId(), key)) {
            if (this.nameOf(id).equalsIgnoreCase(name)) {
                return id;
            }
        }
        return null;
    }

    private void accept(Player player, String name) {
        UUID from = this.resolveKnown(player, name, "requests");
        if (from == null) {
            player.sendMessage("§cKeine Anfrage von " + name + ".");
            return;
        }
        this.addFriend(player.getUniqueId(), from);
        this.save();
        player.sendMessage("§a" + this.nameOf(from) + " ist jetzt dein Freund.");
        Player online = this.plugin.getServer().getPlayer(from);
        if (online != null) {
            online.sendMessage("§a" + player.getName() + " hat deine Anfrage angenommen.");
        }
    }

    private void deny(Player player, String name) {
        UUID from = this.resolveKnown(player, name, "requests");
        if (from == null) {
            player.sendMessage("§cKeine Anfrage von " + name + ".");
            return;
        }
        this.dropRequest(player.getUniqueId(), from);
        this.save();
        player.sendMessage("§7Anfrage von " + this.nameOf(from) + " abgelehnt.");
    }

    private void remove(Player player, UUID other) {
        UUID me = player.getUniqueId();
        List<UUID> mine = this.list(me, "friends");
        List<UUID> theirs = this.list(other, "friends");
        mine.remove(other);
        theirs.remove(me);
        this.store(me, "friends", mine);
        this.store(other, "friends", theirs);
        this.save();
        player.sendMessage("§7" + this.nameOf(other) + " ist nicht mehr auf deiner Freundesliste.");
    }

    /** Wo der Freund gerade ist (fuer die Freundesliste). */
    private String where(Player friend) {
        UUID id = friend.getUniqueId();
        if (this.plugin.activeDuelPlayers.contains(id) || this.plugin.activePartyMatches.containsKey(id)) {
            return "im Duell";
        }
        if (this.plugin.activeParkourMaps.containsKey(id)) {
            return "Parkour";
        }
        if (this.plugin.inBauMode(id)) {
            return "Bau-SMP";
        }
        return switch (this.plugin.profiles.getOrDefault(id, "lobby")) {
            case "smp" -> "SMP";
            default -> "Lobby";
        };
    }

    private void teleportRequest(Player player, Player target) {
        if (!this.areFriends(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(Messages.get("friends.nur-freunde-konnen-sich"));
            return;
        }
        if (this.plugin.inActiveMatch(player.getUniqueId()) || this.plugin.inActiveMatch(target.getUniqueId())) {
            player.sendMessage(Messages.get("friends.wahrend-eines-duells-geht"));
            return;
        }
        this.tpRequests.put(target.getUniqueId(), player.getUniqueId());
        this.tpTimes.put(target.getUniqueId(), System.currentTimeMillis());
        player.sendMessage("§aTeleport-Anfrage an §f" + target.getName() + " §agesendet.");
        target.sendMessage("§d✦ §f" + player.getName() + " §7möchte zu dir. §a/friend tpaccept §7(60 Sekunden).");
        Chat.ping(target);
    }

    private void tpAccept(Player target) {
        UUID from = this.tpRequests.remove(target.getUniqueId());
        Long time = this.tpTimes.remove(target.getUniqueId());
        Player sender = from == null ? null : this.plugin.getServer().getPlayer(from);
        if (sender == null || time == null || System.currentTimeMillis() - time > TP_TIMEOUT_MS) {
            target.sendMessage(Messages.get("friends.keine-gultige-teleport-anfrage"));
            return;
        }
        if (this.plugin.inActiveMatch(sender.getUniqueId()) || this.plugin.inActiveMatch(target.getUniqueId())) {
            target.sendMessage(Messages.get("friends.wahrend-eines-duells-geht-2"));
            return;
        }
        sender.teleport(target.getLocation());
        sender.sendMessage("§aTeleportiert zu " + target.getName() + ".");
        target.sendMessage("§a" + sender.getName() + " wurde zu dir teleportiert.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.get("friends.nur-im-spiel"));
            return true;
        }
        if (args.length == 0) {
            this.open(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "add", "hinzufuegen" -> {
                if (args.length < 2) {
                    player.sendMessage(Messages.get("friends.friend-add-spieler"));
                } else {
                    this.request(player, args[1]);
                }
            }
            case "accept", "annehmen" -> {
                if (args.length < 2) {
                    player.sendMessage(Messages.get("friends.friend-accept-spieler"));
                } else {
                    this.accept(player, args[1]);
                }
            }
            case "deny", "ablehnen" -> {
                if (args.length < 2) {
                    player.sendMessage(Messages.get("friends.friend-deny-spieler"));
                } else {
                    this.deny(player, args[1]);
                }
            }
            case "remove", "entfernen" -> {
                UUID other = args.length < 2 ? null : this.resolveKnown(player, args[1], "friends");
                if (other == null) {
                    player.sendMessage(Messages.get("friends.das-ist-kein-freund"));
                } else {
                    this.remove(player, other);
                }
            }
            case "tp" -> {
                Player target = args.length < 2 ? null : this.plugin.getServer().getPlayerExact(args[1]);
                if (target == null) {
                    player.sendMessage(Messages.get("friends.friend-tp-freund-muss"));
                } else {
                    this.teleportRequest(player, target);
                }
            }
            case "tpaccept" -> this.tpAccept(player);
            case "notify", "meldungen" -> {
                boolean now = !this.notifyOn(player.getUniqueId());
                this.data.set("players." + player.getUniqueId() + ".notify", now);
                this.save();
                player.sendMessage("§7Online-Meldungen deiner Freunde: " + (now ? "§aan" : "§caus"));
            }
            case "list", "liste" -> this.sendList(player);
            default -> this.open(player);
        }
        return true;
    }

    private void sendList(Player player) {
        List<UUID> friends = this.list(player.getUniqueId(), "friends");
        if (friends.isEmpty()) {
            player.sendMessage(Messages.get("friends.du-hast-noch-keine"));
            return;
        }
        player.sendMessage("§d✦ §fFreunde §7(" + friends.size() + ")");
        for (UUID id : friends) {
            boolean online = this.plugin.getServer().getPlayer(id) != null;
            player.sendMessage((online ? " §a● §f" : " §8● §7") + this.nameOf(id));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            for (String sub : List.of("add", "accept", "deny", "remove", "tp", "tpaccept", "list", "notify")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2 && sender instanceof Player player) {
            String key = args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny") ? "requests"
                    : args[0].equalsIgnoreCase("add") ? "" : "friends";
            if (key.isEmpty()) {
                for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                    if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)) && !online.equals(player)) {
                        out.add(online.getName());
                    }
                }
            } else {
                for (UUID id : this.list(player.getUniqueId(), key)) {
                    String name = this.nameOf(id);
                    if (name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                        out.add(name);
                    }
                }
            }
        }
        return out;
    }

    /* ---------- Menue ---------- */

    void open(Player player) {
        UUID me = player.getUniqueId();
        Inventory inv = Gui.frame(this.plugin, TAG, "Freunde", 54);
        List<UUID> friends = this.list(me, "friends");
        friends.sort((x, y) -> {
            boolean ox = this.plugin.getServer().getPlayer(x) != null;
            boolean oy = this.plugin.getServer().getPlayer(y) != null;
            return ox != oy ? (ox ? -1 : 1) : this.nameOf(x).compareToIgnoreCase(this.nameOf(y));
        });
        int slot = 10;
        for (UUID id : friends) {
            if (slot > 43) {
                break;
            }
            if (slot % 9 == 8) {
                slot += 2;
            }
            Player online = this.plugin.getServer().getPlayer(id);
            ItemStack head;
            if (online != null) {
                head = Gui.item(Material.PLAYER_HEAD, "§a● §f" + online.getName(), "§7Online §8• §f" + this.where(online), "",
                        "§eLinksklick: §7Teleport-Anfrage", "§eRechtsklick: §7Duell", "§eShift+Links: §7Party-Einladung", "§cShift+Rechts: §7entfernen");
            } else {
                head = Gui.item(Material.PLAYER_HEAD, "§8● §7" + this.nameOf(id), "§8Offline", "", "§cShift+Rechtsklick: §7entfernen");
            }
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(this.plugin.getServer().getOfflinePlayer(id));
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }
        if (friends.isEmpty()) {
            inv.setItem(22, Gui.item(Material.BARRIER, "§7Noch keine Freunde", "§8Mit §d/friend add <Spieler> §8Anfragen senden."));
        }
        int pending = this.list(me, "requests").size();
        inv.setItem(49, Gui.item(Material.WRITABLE_BOOK, "§d§lAnfragen §7(" + pending + ")", "§7Offene Freundschaftsanfragen.", "", "§e▶ Klicken"));
        inv.setItem(4, Gui.item(Material.PLAYER_HEAD, "§d§lDeine Freunde §7(" + friends.size() + "/" + MAX_FRIENDS + ")", "§8Hinzufügen: /friend add <Spieler>"));
        player.openInventory(inv);
    }

    private void openRequests(Player player) {
        Inventory inv = Gui.frame(this.plugin, TAG, "Anfragen", 54);
        int slot = 10;
        for (UUID id : this.list(player.getUniqueId(), "requests")) {
            if (slot > 43) {
                break;
            }
            if (slot % 9 == 8) {
                slot += 2;
            }
            ItemStack head = Gui.item(Material.PLAYER_HEAD, "§f" + this.nameOf(id), "§aLinksklick: §7annehmen", "§cRechtsklick: §7ablehnen");
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(this.plugin.getServer().getOfflinePlayer(id));
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }
        inv.setItem(49, Gui.item(Material.ARROW, "§7« Zurück"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!Gui.isTitle(title, TAG)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        boolean requests = title.contains("Anfragen");
        int slot = event.getRawSlot();
        if (slot == 49) {
            if (requests) {
                this.open(player);
            } else {
                this.openRequests(player);
            }
            return;
        }
        ItemStack stack = event.getCurrentItem();
        if (stack == null || stack.getType() != Material.PLAYER_HEAD || !stack.hasItemMeta() || slot == 4) {
            return;
        }
        String name = org.bukkit.ChatColor.stripColor(stack.getItemMeta().getDisplayName()).replace("●", "").trim();
        ClickType click = event.getClick();
        if (requests) {
            if (click.isLeftClick()) {
                this.accept(player, name);
            } else if (click.isRightClick()) {
                this.deny(player, name);
            }
            this.openRequests(player);
            return;
        }
        UUID other = this.resolveKnown(player, name, "friends");
        if (other == null) {
            return;
        }
        Player target = this.plugin.getServer().getPlayer(other);
        if (click == ClickType.SHIFT_RIGHT) {
            this.remove(player, other);
            this.open(player);
            return;
        }
        if (target == null) {
            player.sendMessage("§7" + name + " ist gerade offline.");
            return;
        }
        player.closeInventory();
        if (click == ClickType.SHIFT_LEFT) {
            this.plugin.getServer().dispatchCommand(player, "party invite " + target.getName());
        } else if (click.isRightClick()) {
            this.plugin.getServer().dispatchCommand(player, "duel " + target.getName());
        } else if (click.isLeftClick()) {
            this.teleportRequest(player, target);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (Gui.isTitle(event.getView().getTitle(), TAG)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.data.set("names." + player.getUniqueId(), player.getName());
        int pending = this.list(player.getUniqueId(), "requests").size();
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (pending > 0) {
                player.sendMessage("§d✦ §7Du hast §f" + pending + " §7offene Freundschaftsanfrage" + (pending == 1 ? "" : "n") + ". §d/friend");
            }
            for (UUID friend : this.list(player.getUniqueId(), "friends")) {
                Player online = this.plugin.getServer().getPlayer(friend);
                if (online != null && this.notifyOn(friend)) {
                    online.sendMessage("§a● §f" + player.getName() + " §7ist jetzt online.");
                }
            }
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        this.tpRequests.remove(id);
        this.tpTimes.remove(id);
        for (UUID friend : this.list(id, "friends")) {
            Player online = this.plugin.getServer().getPlayer(friend);
            if (online != null && this.notifyOn(friend)) {
                online.sendMessage("§8● §7" + event.getPlayer().getName() + " ist jetzt offline.");
            }
        }
        this.save();
    }
}
