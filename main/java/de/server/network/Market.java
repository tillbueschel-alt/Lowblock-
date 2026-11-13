package de.server.network;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamischer Markt: Jeder Verkauf drueckt den Preis eines Items, jeder Kauf treibt ihn hoch.
 * Ohne Handel pendelt sich der Preis langsam wieder bei 100 % ein (wie bei DonutSMP/Hugo).
 */
final class Market implements org.bukkit.event.Listener {
    static Market instance;
    static final double MIN = 0.25d;
    static final double MAX = 2.0d;

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<Material, Double> factor = new HashMap<Material, Double>();
    /** Zielwert, zu dem der Preis zurueckpendelt - wird vom Auktionshaus (tatsaechliche Verkaufspreise) verschoben. */
    private final Map<Material, Double> anchor = new HashMap<Material, Double>();
    private boolean dirty;

    Market(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.file = new File(plugin.getDataFolder(), "market.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        if (this.data.getConfigurationSection("factor") != null) {
            for (String key : this.data.getConfigurationSection("factor").getKeys(false)) {
                Material m = Material.matchMaterial(key);
                if (m != null) {
                    this.factor.put(m, this.data.getDouble("factor." + key, 1.0d));
                }
            }
        }
        if (this.data.getConfigurationSection("anchor") != null) {
            for (String key : this.data.getConfigurationSection("anchor").getKeys(false)) {
                Material m = Material.matchMaterial(key);
                if (m != null) {
                    this.anchor.put(m, this.data.getDouble("anchor." + key, 1.0d));
                }
            }
        }
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, () -> this.recover(), 1200L, 1200L);
    }

    private double step() {
        return Math.max(0.0001d, Math.min(0.05d, this.plugin.getConfig().getDouble("market.step", 0.006d)));
    }

    static double factor(Material material) {
        return instance == null ? 1.0d : instance.factor.getOrDefault(material, 1.0d);
    }

    /** Preis fuer n Stueck bei aktuellem Markt (ohne etwas zu veraendern). */
    static long quoteSell(Material material, long base, int amount) {
        return instance == null ? base * amount : instance.quote(material, base, amount, false, false);
    }

    static long quoteBuy(Material material, long base, int amount) {
        return instance == null ? base * amount : instance.quote(material, base, amount, true, false);
    }

    static long sell(Material material, long base, int amount) {
        return instance == null ? base * amount : instance.quote(material, base, amount, false, true);
    }

    static long buy(Material material, long base, int amount) {
        return instance == null ? base * amount : instance.quote(material, base, amount, true, true);
    }

    private long quote(Material material, long base, int amount, boolean buying, boolean commit) {
        if (amount <= 0 || base <= 0L) {
            return 0L;
        }
        double f = this.factor.getOrDefault(material, 1.0d);
        double q = buying ? 1.0d + this.step() : 1.0d - this.step();
        // Summe der Einzelpreise base*f*q^i (i = 0..n-1): jedes weitere Stueck wird teurer bzw. billiger
        double sum = base * f * (Math.pow(q, amount) - 1.0d) / (q - 1.0d);
        double next = Math.max(MIN, Math.min(MAX, f * Math.pow(q, amount)));
        if (commit) {
            this.factor.put(material, next);
            this.dirty = true;
        }
        long total = Math.round(sum);
        if (buying) {
            return Math.max(amount, total);
        }
        return Math.max(1L, total);
    }

    /** Aktueller Einzelpreis (gerundet, mindestens 1 wenn die Basis > 0 ist). */
    static long unit(Material material, long base, boolean buying) {
        if (base <= 0L) {
            return 0L;
        }
        return Math.max(1L, Math.round(base * factor(material)));
    }

    /** Trend-Anzeige nur fuer Admins im Creative-Modus, alle anderen sehen keine Preisbewegung. */
    static boolean seeTrend(Player player) {
        try {
            return player != null && player.isOp() && player.getGameMode() == org.bukkit.GameMode.CREATIVE;
        }
        catch (Throwable t) {
            return false;
        }
    }

    static String trend(Player player, Material material) {
        return seeTrend(player) ? " " + rawTrend(material) : "";
    }

    static String rawTrend(Material material) {
        double f = factor(material);
        int pct = (int)Math.round((f - 1.0d) * 100.0d);
        if (pct == 0) {
            return "§7● 0%";
        }
        return pct > 0 ? "§a▲ +" + pct + "%" : "§c▼ " + pct + "%";
    }

    /** Die am staerksten bewegten Items (gesenkt oder gestiegen). */
    static List<Material> movers(boolean rising, int limit) {
        List<Material> list = new ArrayList<Material>();
        if (instance == null) {
            return list;
        }
        for (Map.Entry<Material, Double> e : instance.factor.entrySet()) {
            if (rising ? e.getValue() > 1.01d : e.getValue() < 0.99d) {
                list.add(e.getKey());
            }
        }
        list.sort((a, b) -> rising ? Double.compare(factor(b), factor(a)) : Double.compare(factor(a), factor(b)));
        return list.size() > limit ? new ArrayList<Material>(list.subList(0, limit)) : list;
    }

    /**
     * Das Auktionshaus meldet einen Preis. Verkauft (sale=true) zaehlt stark, ein neues Angebot nur leicht.
     * Der Marktpreis springt Richtung Auktionspreis und pendelt sich dort ein.
     */
    static void auction(Material material, long base, long unitPrice, boolean sale) {
        if (instance != null) {
            instance.signal(material, base, unitPrice, sale);
        }
    }

    private void signal(Material material, long base, long unitPrice, boolean sale) {
        if (base <= 0L || unitPrice <= 0L) {
            return;
        }
        double ratio = Math.max(MIN, Math.min(MAX, (double)unitPrice / (double)base));
        double w = sale ? Math.max(0.05d, Math.min(0.9d, this.plugin.getConfig().getDouble("market.auction-weight", 0.45d))) : 0.08d;
        double a = this.anchor.getOrDefault(material, 1.0d);
        this.anchor.put(material, Math.max(MIN, Math.min(MAX, a + (ratio - a) * w)));
        double f = this.factor.getOrDefault(material, 1.0d);
        this.factor.put(material, Math.max(MIN, Math.min(MAX, f + (ratio - f) * w)));
        this.dirty = true;
    }

    private void recover() {
        double rate = Math.max(0.0d, Math.min(0.5d, this.plugin.getConfig().getDouble("market.recovery-per-minute", 0.02d)));
        List<Material> done = new ArrayList<Material>();
        // Auktions-Ziel driftet langsam wieder zu 100 %
        List<Material> anchorDone = new ArrayList<Material>();
        for (Map.Entry<Material, Double> e : this.anchor.entrySet()) {
            double a = e.getValue();
            double next = a + (1.0d - a) * 0.01d;
            if (Math.abs(next - 1.0d) < 0.01d) {
                anchorDone.add(e.getKey());
            } else {
                e.setValue(next);
            }
            this.dirty = true;
        }
        for (Material m : anchorDone) {
            this.anchor.remove(m);
        }
        for (Map.Entry<Material, Double> e : this.factor.entrySet()) {
            double f = e.getValue();
            double target = this.anchor.getOrDefault(e.getKey(), 1.0d);
            double next = f + (target - f) * rate + (f < target ? 0.001d : -0.001d);
            if (Math.abs(next - target) < 0.004d || (f < target) != (next < target)) {
                if (Math.abs(target - 1.0d) < 0.004d) {
                    done.add(e.getKey());
                } else {
                    e.setValue(target);
                }
            } else {
                e.setValue(next);
            }
            this.dirty = true;
        }
        for (Material m : done) {
            this.factor.remove(m);
        }
        for (Map.Entry<Material, Double> e : this.anchor.entrySet()) {
            if (!this.factor.containsKey(e.getKey())) {
                this.factor.put(e.getKey(), e.getValue());
            }
        }
        if (this.dirty) {
            this.save();
        }
    }

    void save() {
        try {
            this.data.set("factor", null);
            this.data.set("anchor", null);
            for (Map.Entry<Material, Double> e : this.anchor.entrySet()) {
                this.data.set("anchor." + e.getKey().name(), Math.round(e.getValue() * 10000.0d) / 10000.0d);
            }
            for (Map.Entry<Material, Double> e : this.factor.entrySet()) {
                this.data.set("factor." + e.getKey().name(), Math.round(e.getValue() * 10000.0d) / 10000.0d);
            }
            Store.save(this.data, this.file);
            this.dirty = false;
        }
        catch (Exception ignored) {
            // naechster Versuch in einer Minute
            Store.problem("Market:233", ignored);
        }
    }

    private static final String TAG = "Markt";
    private static final int[] LEFT = {10, 11, 12, 19, 20, 21, 28, 29, 30, 37, 38, 39};
    private static final int[] RIGHT = {14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};

    /** Marktuebersicht: was gerade gefragt (teuer) und was im Ueberfluss (billig) ist. */
    static void open(DuelNetworkPlugin plugin, Player player) {
        if (!seeTrend(player)) {
            player.sendMessage("§cDie Marktübersicht ist nur für Admins im Creative-Modus sichtbar.");
            return;
        }
        Inventory inv = Gui.frame(plugin, TAG, "Marktpreise", 54);
        inv.setItem(4, Gui.item(Material.NETHER_STAR, "§d§l✦ Markt", "§7Kaufen treibt den Preis hoch,", "§7Verkaufen drückt ihn. Ohne Handel", "§7pendelt er sich wieder ein."));
        fill(inv, LEFT, movers(true, LEFT.length), true);
        fill(inv, RIGHT, movers(false, RIGHT.length), false);
        inv.setItem(1, Gui.item(Material.GOLD_INGOT, "§a§l▲ Gefragt", "§7Links: Preis gestiegen – lohnt sich zu verkaufen."));
        inv.setItem(7, Gui.item(Material.IRON_INGOT, "§c§l▼ Im Überfluss", "§7Rechts: Preis gefallen – lieber noch warten."));
        inv.setItem(46, Gui.item(Material.PAPER, "§e§lTipp", "§7/worth zeigt den Wert deines Items,", "§7/sell verkauft zum Marktpreis."));
        player.openInventory(inv);
    }

    private static void fill(Inventory inv, int[] slots, List<Material> list, boolean rising) {
        Economy economy = Economy.instance;
        for (int i = 0; i < slots.length; i++) {
            if (i >= list.size() || economy == null) {
                inv.setItem(slots[i], Gui.item(Material.BLACK_STAINED_GLASS_PANE, " "));
                continue;
            }
            Material m = list.get(i);
            inv.setItem(slots[i], Gui.item(m, 1, "§f" + Gui.prettyName(m), List.of("§7Verkaufspreis: §a" + Gui.money(economy.sellPrice(m)), rawTrend(m), "§8Grundpreis: " + Gui.money(economy.basePrice(m)))));
        }
    }

    private static final java.util.Set<String> SMP_ONLY = java.util.Set.of("sell", "shop", "ah", "order", "market", "worth", "rtp", "home", "sethome", "delhome", "homes", "tpa", "tpaccept", "tpdeny", "tpahere", "money", "pay", "baltop");

    /** Wirtschafts-Befehle (/sell, /shop, /ah ...) gehen nur im SMP, nicht in Lobby/Duel/Parkour. */
    @EventHandler
    public void onCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        try {
            Player player = event.getPlayer();
            String msg = event.getMessage();
            if (msg.length() < 2) {
                return;
            }
            String label = msg.substring(1).split(" ")[0].toLowerCase(java.util.Locale.ROOT);
            int colon = label.indexOf(':');
            if (colon >= 0) {
                label = label.substring(colon + 1);
            }
            org.bukkit.command.PluginCommand cmd = this.plugin.getCommand(label);
            String name = cmd != null ? cmd.getName().toLowerCase(java.util.Locale.ROOT) : label;
            if (!SMP_ONLY.contains(name) || (player.isOp() && player.getGameMode() == org.bukkit.GameMode.CREATIVE)) {
                return;
            }
            if (!"smp".equals(this.plugin.profiles.getOrDefault(player.getUniqueId(), "lobby"))) {
                event.setCancelled(true);
                player.sendMessage("§c/" + name + " geht nur im SMP.");
            }
        }
        catch (Throwable ignored) {
            // nur Komfort
            Store.problem("Market:296", ignored);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (Gui.isTitle(event.getView().getTitle(), TAG)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (Gui.isTitle(event.getView().getTitle(), TAG)) {
            event.setCancelled(true);
        }
    }
}
