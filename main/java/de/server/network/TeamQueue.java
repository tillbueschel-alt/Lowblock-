package de.server.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

/**
 * Team-Duelle per Warteschlange (NPC "team" oder /teamduel): Eine Party mit genau 2, 3 oder 4 Mitgliedern waehlt
 * die Teamgroesse und ein Kit. Sobald eine zweite Party gleicher Groesse und gleichen Kits wartet, startet ein Match
 * Party gegen Party (Team 1 = zuerst wartende Party). Teamschaden ist aus; das Match laeuft ueber das Party-Match-System.
 */
final class TeamQueue implements Listener, CommandExecutor {
    private static final String TAG = "Team";
    private static final String TAG_KIT = "Team-Kit";
    private static final long MAX_WAIT_MS = 15L * 60L * 1000L;

    private record Entry(String party, UUID leader, long since) { }

    private final DuelNetworkPlugin plugin;
    /** Schluessel "<Groesse>:<kit>" -> wartende Parties (aelteste zuerst). */
    private final Map<String, List<Entry>> queues = new LinkedHashMap<String, List<Entry>>();
    private final Map<UUID, Integer> pendingSize = new ConcurrentHashMap<UUID, Integer>();

    TeamQueue(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("TeamQueue", this::pump), 100L, 40L);
        if (plugin.getCommand("teamduel") != null) {
            plugin.getCommand("teamduel").setExecutor((CommandExecutor)this);
        }
    }

    static TeamQueue instance;

    /** Anzahl wartender Teams (fuer die NPC-Anzeige). */
    static int waitingTotal() {
        if (instance == null) {
            return 0;
        }
        int total = 0;
        for (List<Entry> list : instance.queues.values()) {
            total += list.size();
        }
        return total;
    }

    static void open(DuelNetworkPlugin plugin, Player player) {
        if (instance != null) {
            instance.openMenu(player);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        if (args.length >= 1 && (args[0].equalsIgnoreCase("leave") || args[0].equalsIgnoreCase("verlassen"))) {
            this.leave(player);
        } else {
            this.openMenu(player);
        }
        return true;
    }

    private boolean idle(Player player) {
        UUID id = player.getUniqueId();
        return "lobby".equals(this.plugin.profiles.getOrDefault(id, "lobby")) && !this.plugin.activeDuelPlayers.contains(id)
                && !this.plugin.activePartyMatches.containsKey(id) && !this.plugin.activeParkourMaps.containsKey(id);
    }

    private int waiting(int size) {
        int total = 0;
        for (Map.Entry<String, List<Entry>> e : this.queues.entrySet()) {
            if (e.getKey().startsWith(size + ":")) {
                total += e.getValue().size();
            }
        }
        return total;
    }

    private String queuedFor(String party) {
        for (Map.Entry<String, List<Entry>> e : this.queues.entrySet()) {
            for (Entry entry : e.getValue()) {
                if (entry.party().equals(party)) {
                    return e.getKey();
                }
            }
        }
        return null;
    }

    /* ---------- Menues ---------- */

    private void openMenu(Player player) {
        if (!this.idle(player)) {
            player.sendMessage("§cTeam-Duelle gibt es nur in der Lobby.");
            return;
        }
        String party = this.plugin.partyOf(player.getUniqueId());
        int members = this.plugin.partyOnline(party).size();
        Inventory inv = Gui.frame(this.plugin, TAG, "Team-Duelle", 27);
        String[] names = {"§a§lDuo §f2 gegen 2", "§e§lTrio §f3 gegen 3", "§c§lQuad §f4 gegen 4"};
        Material[] icons = {Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD};
        int[] slots = {10, 12, 14};
        for (int i = 0; i < 3; i++) {
            int size = i + 2;
            String status = party == null ? "§cDu brauchst eine Party (/party)." : members == size ? "§aDeine Party passt (" + size + " Spieler)." : "§cDeine Party hat " + members + " Spieler online, nötig: " + size + ".";
            inv.setItem(slots[i], Gui.item(icons[i], names[i], "§7Deine Party tritt gegen eine andere", "§7Party der gleichen Größe an.", "§7Wartende Teams: §f" + this.waiting(size), "", status, "§e▶ Klicken"));
        }
        String queued = party == null ? null : this.queuedFor(party);
        inv.setItem(16, Gui.item(queued == null ? Material.BARRIER : Material.REDSTONE, queued == null ? "§7Nicht in der Warteschlange" : "§c§lWarteschlange verlassen",
                queued == null ? new String[]{"§8Wähle links eine Teamgröße."} : new String[]{"§7Gesucht: §f" + queued.split(":")[0] + "v" + queued.split(":")[0] + " §7Kit §f" + queued.substring(queued.indexOf(':') + 1), "§e▶ Klicken zum Verlassen"}));
        inv.setItem(22, Gui.item(Material.BOOK, "§d§lSo geht's", "§71. §d/party create §7und Freunde einladen", "§72. Teamgröße wählen (Party-Größe = Teamgröße)", "§73. Kit wählen und auf ein Gegner-Team warten", "§8Nur der Party-Leader kann starten."));
        player.openInventory(inv);
    }

    private void openKits(Player player, int size) {
        this.pendingSize.put(player.getUniqueId(), size);
        Inventory inv = Gui.frame(this.plugin, TAG_KIT, "Kit für " + size + "v" + size, 27);
        List<String> kits = this.plugin.getConfig().getStringList("kits.available");
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < kits.size() && i < slots.length; i++) {
            String kit = kits.get(i);
            inv.setItem(slots[i], Gui.item(this.plugin.iconForKit(kit), "§d" + this.plugin.displayKitName(kit), "§7Kit für das Team-Duell", "§e▶ Klicken zum Anmelden"));
        }
        inv.setItem(22, Gui.item(Material.ARROW, "§7« Zurück"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        boolean menu = Gui.isTitle(title, TAG);
        boolean kits = Gui.isTitle(title, TAG_KIT);
        if (!menu && !kits) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        int slot = event.getSlot();
        if (menu) {
            if (slot == 10 || slot == 12 || slot == 14) {
                int size = slot == 10 ? 2 : slot == 12 ? 3 : 4;
                String problem = this.check(player, size);
                if (problem != null) {
                    player.sendMessage("§c" + problem);
                    Chat.no(player);
                    return;
                }
                this.openKits(player, size);
            } else if (slot == 16) {
                this.leave(player);
                player.closeInventory();
            }
            return;
        }
        if (slot == 22) {
            this.openMenu(player);
            return;
        }
        Integer size = this.pendingSize.get(player.getUniqueId());
        List<String> kitList = this.plugin.getConfig().getStringList("kits.available");
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < kitList.size() && i < slots.length; i++) {
            if (slots[i] == slot && size != null) {
                player.closeInventory();
                this.queue(player, size, kitList.get(i));
                return;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (Gui.isTitle(title, TAG) || Gui.isTitle(title, TAG_KIT)) {
            event.setCancelled(true);
        }
    }

    /* ---------- Warteschlange ---------- */

    /** @return Fehlertext oder null, wenn der Spieler diese Teamgroesse anmelden darf. */
    private String check(Player leader, int size) {
        if (!this.idle(leader)) {
            return "Team-Duelle gibt es nur in der Lobby.";
        }
        String party = this.plugin.partyOf(leader.getUniqueId());
        if (party == null) {
            return "Du brauchst eine Party (/party create) mit genau " + size + " Spielern.";
        }
        if (!this.plugin.partyLeader(leader)) {
            return "Nur der Party-Leader kann ein Team-Duell starten.";
        }
        List<Player> members = this.plugin.partyOnline(party);
        if (members.size() != size) {
            return "Für " + size + "v" + size + " braucht deine Party genau " + size + " Spieler online (jetzt: " + members.size() + ").";
        }
        for (Player member : members) {
            if (!this.idle(member)) {
                return member.getName() + " ist gerade nicht in der Lobby.";
            }
        }
        return null;
    }

    private void queue(Player leader, int size, String kit) {
        String problem = this.check(leader, size);
        if (problem != null) {
            leader.sendMessage("§c" + problem);
            return;
        }
        String party = this.plugin.partyOf(leader.getUniqueId());
        this.remove(party);
        String key = size + ":" + kit.toLowerCase(Locale.ROOT);
        this.queues.computeIfAbsent(key, k -> new ArrayList<Entry>()).add(new Entry(party, leader.getUniqueId(), System.currentTimeMillis()));
        for (Player member : this.plugin.partyOnline(party)) {
            member.sendMessage("§d⚔ §fTeam-Duell " + size + "v" + size + " §7(Kit §f" + this.plugin.displayKitName(kit) + "§7): Warteschlange – wir suchen ein Gegner-Team. §8(/teamduel leave)");
        }
        this.pump();
    }

    private void remove(String party) {
        for (List<Entry> list : this.queues.values()) {
            list.removeIf(entry -> entry.party().equals(party));
        }
    }

    private void leave(Player player) {
        String party = this.plugin.partyOf(player.getUniqueId());
        if (party == null || this.queuedFor(party) == null) {
            player.sendMessage("§7Deine Party ist in keiner Team-Warteschlange.");
            return;
        }
        this.remove(party);
        for (Player member : this.plugin.partyOnline(party)) {
            member.sendMessage("§7Eure Party hat die Team-Warteschlange verlassen.");
        }
    }

    private boolean valid(Entry entry, int size) {
        if (System.currentTimeMillis() - entry.since() > MAX_WAIT_MS) {
            return false;
        }
        Player leader = this.plugin.getServer().getPlayer(entry.leader());
        if (leader == null || !entry.party().equals(this.plugin.partyOf(leader.getUniqueId())) || !this.plugin.partyLeader(leader)) {
            return false;
        }
        List<Player> members = this.plugin.partyOnline(entry.party());
        if (members.size() != size) {
            return false;
        }
        for (Player member : members) {
            if (!this.idle(member)) {
                return false;
            }
        }
        return true;
    }

    /** Alle 2 Sekunden: ungueltige Eintraege entfernen, passende Teams zusammenfuehren. */
    private void pump() {
        if (this.queues.isEmpty() || this.plugin.findFreeArena() == null) {
            // ohne freie Arena nur aufraeumen
            this.purge();
            return;
        }
        this.purge();
        for (Map.Entry<String, List<Entry>> e : new ArrayList<Map.Entry<String, List<Entry>>>(this.queues.entrySet())) {
            List<Entry> list = e.getValue();
            if (list.size() < 2) {
                continue;
            }
            int size = Integer.parseInt(e.getKey().split(":")[0]);
            String kit = e.getKey().substring(e.getKey().indexOf(':') + 1);
            Entry first = list.get(0);
            Entry second = list.get(1);
            List<Player> players = new ArrayList<Player>(this.plugin.partyOnline(first.party()));
            int split = players.size();
            players.addAll(this.plugin.partyOnline(second.party()));
            Map<UUID, Integer> teams = new HashMap<UUID, Integer>();
            for (int i = 0; i < players.size(); i++) {
                teams.put(players.get(i).getUniqueId(), i < split ? 0 : 1);
            }
            Player starter = this.plugin.getServer().getPlayer(second.leader());
            if (starter == null) {
                continue;
            }
            this.plugin.startPartyMatch(starter, players, kit, false, new ConcurrentHashMap<UUID, Integer>(teams));
            if (this.plugin.activePartyMatches.containsKey(starter.getUniqueId())) {
                list.remove(first);
                list.remove(second);
                StringBuilder one = new StringBuilder();
                StringBuilder two = new StringBuilder();
                for (int i = 0; i < players.size(); i++) {
                    (i < split ? one : two).append(players.get(i).getName()).append(' ');
                }
                for (Player player : players) {
                    player.sendMessage("§d⚔ §fTeam-Duell gefunden! §7Team 1: §f" + one.toString().trim() + " §8| §7Team 2: §f" + two.toString().trim());
                }
            }
        }
    }

    private void purge() {
        for (Map.Entry<String, List<Entry>> e : this.queues.entrySet()) {
            int size = Integer.parseInt(e.getKey().split(":")[0]);
            Iterator<Entry> it = e.getValue().iterator();
            while (it.hasNext()) {
                Entry entry = it.next();
                if (!this.valid(entry, size)) {
                    it.remove();
                    Player leader = this.plugin.getServer().getPlayer(entry.leader());
                    if (leader != null) {
                        leader.sendMessage("§7Eure Team-Anmeldung ist abgelaufen oder ungültig geworden.");
                    }
                }
            }
        }
    }
}
