package de.server.network;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Bestenliste fuers SMP als Podest-Menue (Geld + Spielzeit), gleicher Stil wie die Duel-Rangliste. */
final class SmpTop implements Listener {
    static SmpTop instance;
    private static final String TAG = "Top";
    private static final int[] HEAD_SLOT = {13, 21, 23};
    private static final int[][] PILLAR_SLOTS = {{22, 31}, {30}, {32}};
    private static final int[] REST = {37, 38, 39, 40, 41, 42, 43};
    private static final Material[] PILLAR = {Material.YELLOW_STAINED_GLASS_PANE, Material.LIGHT_GRAY_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE};
    private static final String[] PILLAR_NAME = {"§6§l♕ 1. PLATZ", "§f§l✦ 2. PLATZ", "§c§l✦ 3. PLATZ"};
    private static final String SEP = "§8§m                         ";

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<UUID, Long> minutes = new HashMap<UUID, Long>();
    private final Map<UUID, String> names = new HashMap<UUID, String>();
    private final Map<UUID, Long> kills = new HashMap<UUID, Long>();
    private final Map<UUID, Long> deaths = new HashMap<UUID, Long>();

    SmpTop(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.file = new File(plugin.getDataFolder(), "smpstats.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        if (this.data.getConfigurationSection("play") != null) {
            for (String key : this.data.getConfigurationSection("play").getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    this.minutes.put(id, this.data.getLong("play." + key + ".min", 0L));
                    this.names.put(id, this.data.getString("play." + key + ".name", "?"));
                    this.kills.put(id, this.data.getLong("play." + key + ".kills", 0L));
                    this.deaths.put(id, this.data.getLong("play." + key + ".deaths", 0L));
                }
                catch (Exception ignored) {
                    Store.problem("SmpTop:56", ignored);
                }
            }
        }
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, () -> this.tick(), 1200L, 1200L);
    }

    private void tick() {
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            if ("smp".equals(this.plugin.profiles.getOrDefault(player.getUniqueId(), "lobby"))) {
                this.minutes.merge(player.getUniqueId(), 1L, Long::sum);
                this.names.put(player.getUniqueId(), player.getName());
            }
        }
        this.save();
    }

    void save() {
        try {
            this.data.set("play", null);
            java.util.Set<UUID> all = new java.util.HashSet<UUID>(this.minutes.keySet());
            all.addAll(this.kills.keySet());
            all.addAll(this.deaths.keySet());
            for (UUID id : all) {
                this.data.set("play." + id + ".min", this.minutes.getOrDefault(id, 0L));
                this.data.set("play." + id + ".kills", this.kills.getOrDefault(id, 0L));
                this.data.set("play." + id + ".deaths", this.deaths.getOrDefault(id, 0L));
                this.data.set("play." + id + ".name", this.names.getOrDefault(id, "?"));
            }
            Store.save(this.data, this.file);
        }
        catch (Exception ignored) {
            Store.problem("SmpTop:87", ignored);
        }
    }

    static List<Map.Entry<UUID, Long>> topKills(int limit) {
        return instance == null ? new ArrayList<Map.Entry<UUID, Long>>() : instance.topOf(instance.kills, limit);
    }

    static List<Map.Entry<UUID, Long>> topMinutes(int limit) {
        return instance == null ? new ArrayList<Map.Entry<UUID, Long>>() : instance.topOf(instance.minutes, limit);
    }

    private synchronized List<Map.Entry<UUID, Long>> topOf(Map<UUID, Long> source, int limit) {
        List<Map.Entry<UUID, Long>> list = new ArrayList<Map.Entry<UUID, Long>>();
        for (Map.Entry<UUID, Long> e : source.entrySet()) {
            if (e.getValue() > 0L) {
                list.add(new java.util.AbstractMap.SimpleEntry<UUID, Long>(e.getKey(), e.getValue()));
            }
        }
        list.sort((x, y) -> Long.compare(y.getValue(), x.getValue()));
        return new ArrayList<Map.Entry<UUID, Long>>(list.subList(0, Math.min(limit, list.size())));
    }

    static long killsOf(UUID id) {
        return instance == null ? 0L : instance.kills.getOrDefault(id, 0L);
    }

    static long deathsOf(UUID id) {
        return instance == null ? 0L : instance.deaths.getOrDefault(id, 0L);
    }

    static int killRank(UUID id) {
        if (instance == null) {
            return 0;
        }
        List<UUID> order = instance.killOrder();
        int index = order.indexOf(id);
        return index < 0 ? order.size() + 1 : index + 1;
    }

    private List<UUID> killOrder() {
        List<UUID> list = new ArrayList<UUID>(this.kills.keySet());
        list.sort((a, b) -> Long.compare(this.kills.getOrDefault(b, 0L), this.kills.getOrDefault(a, 0L)));
        return list;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        try {
            Player victim = event.getEntity();
            if (!"smp".equals(this.plugin.profiles.getOrDefault(victim.getUniqueId(), "lobby"))) {
                return;
            }
            this.deaths.merge(victim.getUniqueId(), 1L, Long::sum);
            this.names.put(victim.getUniqueId(), victim.getName());
            Player killer = victim.getKiller();
            if (killer != null && !killer.equals(victim) && "smp".equals(this.plugin.profiles.getOrDefault(killer.getUniqueId(), "lobby"))) {
                this.kills.merge(killer.getUniqueId(), 1L, Long::sum);
                this.names.put(killer.getUniqueId(), killer.getName());
            }
        }
        catch (Throwable ignored) {
            // nur Statistik
            Store.problem("SmpTop:129", ignored);
        }
    }

    static long minutesOf(UUID id) {
        return instance == null ? 0L : instance.minutes.getOrDefault(id, 0L);
    }

    static int playRank(UUID id) {
        if (instance == null) {
            return 0;
        }
        List<UUID> order = instance.playOrder();
        int index = order.indexOf(id);
        return index < 0 ? order.size() + 1 : index + 1;
    }

    static String time(long minutes) {
        long h = minutes / 60L;
        return h >= 1L ? h + "h " + (minutes % 60L) + "m" : minutes + "m";
    }

    private List<UUID> playOrder() {
        List<UUID> list = new ArrayList<UUID>(this.minutes.keySet());
        list.sort((a, b) -> Long.compare(this.minutes.getOrDefault(b, 0L), this.minutes.getOrDefault(a, 0L)));
        return list;
    }

    void open(Player player, String mode) {
        String m = mode.equals("zeit") ? "zeit" : mode.equals("kills") ? "kills" : "geld";
        Inventory inv = Gui.frame(this.plugin, TAG, "Bestenliste · " + (m.equals("zeit") ? "Spielzeit" : m.equals("kills") ? "Kills" : "Geld"), 54);
        for (int i = 10; i < 44; i++) {
            if (i % 9 != 0 && i % 9 != 8) {
                inv.setItem(i, Gui.item(Material.BLACK_STAINED_GLASS_PANE, " "));
            }
        }
        DuelStats.categories(inv, "smp");
        inv.setItem(45, Gui.item(Material.ARROW, "§7« Zurück", "§7Zur Duel-Rangliste"));
        List<UUID> order = m.equals("geld") ? Economy.order() : m.equals("zeit") ? this.playOrder() : this.killOrder();
        UUID me = player.getUniqueId();
        for (int i = 0; i < Math.min(10, order.size()); i++) {
            UUID id = order.get(i);
            String name = m.equals("geld") ? Economy.nameOf(id) : this.names.getOrDefault(id, "?");
            this.place(inv, i, id, name, id.equals(me), this.line(m, id));
        }
        int myRank = m.equals("geld") ? Economy.rank(me) : m.equals("zeit") ? playRank(me) : killRank(me);
        inv.setItem(50, Gui.item(Material.NETHER_STAR, "§d§l✦ Dein Platz: §f#" + myRank, SEP, this.line(m, me), SEP));
        inv.setItem(46, Gui.item(Material.GOLD_INGOT, (m.equals("geld") ? "§a▶ " : "§f") + "§lGeld", "§7Die reichsten Spieler", m.equals("geld") ? "§aAusgewählt" : "§eKlicken zum Anzeigen"));
        inv.setItem(47, Gui.item(Material.CLOCK, (m.equals("zeit") ? "§a▶ " : "§f") + "§lSpielzeit", "§7Meiste Zeit im SMP", m.equals("zeit") ? "§aAusgewählt" : "§eKlicken zum Anzeigen"));
        inv.setItem(48, Gui.item(Material.IRON_INGOT, (m.equals("kills") ? "§a▶ " : "§f") + "§lKills", "§7Die meisten SMP-Kills", m.equals("kills") ? "§aAusgewählt" : "§eKlicken zum Anzeigen"));
        player.openInventory(inv);
    }

    private String line(String mode, UUID id) {
        if (mode.equals("geld")) {
            return "§7Geld: §a" + Gui.money(Economy.balance(id));
        }
        if (mode.equals("zeit")) {
            return "§7Spielzeit: §b" + time(this.minutes.getOrDefault(id, 0L));
        }
        return "§7Kills: §a" + this.kills.getOrDefault(id, 0L) + " §8• §7Tode: §c" + this.deaths.getOrDefault(id, 0L);
    }

    private void place(Inventory inv, int index, UUID id, String name, boolean you, String line) {
        String color = index == 0 ? "§6§l" : index == 1 ? "§f§l" : index == 2 ? "§c§l" : "§7";
        String title = (index < 3 ? color + "#" + (index + 1) + " §f§l" : "§7#" + (index + 1) + " §f") + name + (you ? " §d◀ Du" : "");
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = head.getItemMeta();
        meta.setDisplayName(title);
        List<String> lore = new ArrayList<String>();
        lore.add(SEP);
        lore.add(line);
        lore.add(SEP);
        meta.setLore(lore);
        if (meta instanceof org.bukkit.inventory.meta.SkullMeta skull) {
            try {
                skull.setOwningPlayer(this.plugin.getServer().getOfflinePlayer(id));
            }
            catch (Exception ignored) {
                Store.problem("SmpTop:208", ignored);
            }
        }
        head.setItemMeta(meta);
        if (index < 3) {
            inv.setItem(HEAD_SLOT[index], head);
            for (int pillar : PILLAR_SLOTS[index]) {
                inv.setItem(pillar, Gui.item(PILLAR[index], PILLAR_NAME[index], "§7" + name));
            }
        } else if (index - 3 < REST.length) {
            inv.setItem(REST[index - 3], head);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!Gui.isTitle(event.getView().getTitle(), TAG) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        DuelStats stats = DuelStats.get();
        if (slot == 2 || slot == 45) {
            if (stats != null) {
                stats.openDuelGui(player, "kills");
            }
        } else if (slot == 6) {
            if (stats != null) {
                stats.openParkourPicker(player);
            }
        } else if (slot == 46) {
            this.open(player, "geld");
        } else if (slot == 47) {
            this.open(player, "zeit");
        } else if (slot == 48) {
            this.open(player, "kills");
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (Gui.isTitle(event.getView().getTitle(), TAG)) {
            event.setCancelled(true);
        }
    }
}
