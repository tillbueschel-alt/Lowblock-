package de.server.network;

import org.bukkit.ChatColor;
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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Geld (economy.yml): /money, /pay, /baltop, /sell und Admin-Befehle. Dazu das Preis-Verzeichnis fuer Shop und /sell. */
final class Economy implements Listener, CommandExecutor, TabCompleter {
    static Economy instance;

    /** Eintrag im Shop-Verzeichnis. */
    record Entry(String category, Material material, long buy, long sell) { }

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final EconomyDatabase database;
    private final Map<UUID, Long> money = new HashMap<UUID, Long>();
    private final Map<UUID, String> names = new HashMap<UUID, String>();
    private final List<Entry> catalog = new ArrayList<Entry>();
    private List<UUID> rankCache = new ArrayList<UUID>();
    private long rankCacheTime;

    Economy(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.file = new File(plugin.getDataFolder(), "economy.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection section = this.data.getConfigurationSection("players");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    this.money.put(id, this.data.getLong("players." + key + ".money", 0L));
                    this.names.put(id, this.data.getString("players." + key + ".name", key));
                }
                catch (IllegalArgumentException ignored) {
                    // kaputter Eintrag
                }
            }
        }
        try {
            this.database = new EconomyDatabase(new File(plugin.getDataFolder(), "economy.db"), this.file, plugin.getLogger());
            for (EconomyDatabase.Account account : this.database.accounts()) {
                this.money.put(account.id(), account.balance());
                this.names.put(account.id(), account.name());
            }
        }
        catch (java.sql.SQLException ex) {
            throw new IllegalStateException("Transaktionale Economy-Datenbank konnte nicht gestartet werden", ex);
        }
        this.recoverPendingJournals();
        this.buildCatalog();
        long income = Math.max(0L, plugin.getConfig().getLong("economy.income-per-5min", 250L));
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, () -> this.payIncome(income), 6000L, 6000L);
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, () -> this.save(), 1200L, 1200L);
    }

    private long startBalance() {
        return Math.max(0L, this.plugin.getConfig().getLong("economy.start-balance", 1000L));
    }

    /* ---------- Katalog (Shop + /sell) ---------- */

    /** Nie im Shop kaeuflich (selten/besonders/stark) - gibt es nur durch Spielen und Handel. */
    private static final java.util.Set<String> SHOP_BLOCKED = java.util.Set.of("ELYTRA", "NETHER_STAR", "BEACON", "DRAGON_EGG", "ENCHANTED_GOLDEN_APPLE",
            "TRIDENT", "MACE", "HEAVY_CORE", "NETHERITE_INGOT", "NETHERITE_SCRAP", "NETHERITE_BLOCK", "ANCIENT_DEBRIS", "WITHER_SKELETON_SKULL",
            "DRAGON_HEAD", "ECHO_SHARD", "HEART_OF_THE_SEA", "TOTEM_OF_UNDYING_PLACEHOLDER", "ENCHANTED_BOOK", "SPONGE", "TRIAL_KEY", "OMINOUS_TRIAL_KEY");

    private void add(String category, String material, long buy) {
        Material m = Material.matchMaterial(material);
        if (m == null || Gui.blocked(m) || SHOP_BLOCKED.contains(m.name())) {
            return;
        }
        long sell = Math.max(0L, Math.round(buy * 0.4d));
        long customBuy = this.plugin.getConfig().getLong("shop.prices." + material, buy);
        if (customBuy <= 0L) {
            return;
        }
        this.catalog.add(new Entry(category, m, customBuy, customBuy == buy ? sell : Math.round(customBuy * 0.4d)));
    }

    /** Shop wie bei DonutSMP: End, Nether, Gear und Food (Preise ueber shop.prices.<MATERIAL> aenderbar). */
    private void buildCatalog() {
        String[][] end = {{"ENDER_CHEST", "7500"}, {"ENDER_PEARL", "200"}, {"END_STONE", "10"}, {"DRAGON_BREATH", "300"}, {"CHORUS_FRUIT", "25"}, {"SHULKER_SHELL", "4000"}};
        String[][] nether = {{"BLAZE_ROD", "200"}, {"NETHER_WART", "35"}, {"GLOWSTONE_DUST", "25"}, {"GLOWSTONE", "100"}, {"MAGMA_CREAM", "70"}, {"GHAST_TEAR", "900"}, {"QUARTZ", "25"}};
        String[][] gear = {{"OBSIDIAN", "100"}, {"END_CRYSTAL", "1200"}, {"RESPAWN_ANCHOR", "1800"}, {"TOTEM_OF_UNDYING", "6000"}, {"GOLDEN_APPLE", "350"}, {"ENCHANTED_GOLDEN_APPLE", "3500"}, {"ARROW", "5"},
            {"IRON_SWORD", "250"}, {"DIAMOND_SWORD", "4500"}, {"NETHERITE_SWORD", "18000"}, {"IRON_AXE", "300"}, {"DIAMOND_AXE", "5000"}, {"NETHERITE_AXE", "20000"},
            {"BOW", "1200"}, {"CROSSBOW", "1800"}, {"SHIELD", "500"}, {"TRIDENT", "30000"}, {"MACE", "50000"}, {"WIND_CHARGE", "80"}, {"ENDER_PEARL", "200"},
            {"IRON_HELMET", "400"}, {"IRON_CHESTPLATE", "700"}, {"IRON_LEGGINGS", "600"}, {"IRON_BOOTS", "350"}, {"DIAMOND_HELMET", "4500"}, {"DIAMOND_CHESTPLATE", "7500"}, {"DIAMOND_LEGGINGS", "6500"}, {"DIAMOND_BOOTS", "4000"},
            {"NETHERITE_HELMET", "18000"}, {"NETHERITE_CHESTPLATE", "30000"}, {"NETHERITE_LEGGINGS", "26000"}, {"NETHERITE_BOOTS", "16000"}, {"LEATHER_HELMET", "100"}, {"LEATHER_CHESTPLATE", "160"}, {"LEATHER_LEGGINGS", "140"}, {"LEATHER_BOOTS", "80"}};
        String[][] food = {{"POTATO", "5"}, {"SWEET_BERRIES", "5"}, {"CARROT", "5"}, {"APPLE", "10"}, {"COOKED_CHICKEN", "20"}, {"COOKED_BEEF", "35"}, {"GOLDEN_CARROT", "40"}};
        String[][] blocks = {{"STONE", "8"}, {"COBBLESTONE", "4"}, {"DEEPSLATE", "12"}, {"DEEPSLATE_BRICKS", "20"}, {"DEEPSLATE_TILES", "20"}, {"TUFF", "8"}, {"CALCITE", "18"}, {"GRANITE", "8"}, {"DIORITE", "8"}, {"ANDESITE", "8"}, {"OAK_LOG", "12"}, {"SPRUCE_LOG", "12"}, {"BIRCH_LOG", "12"}, {"JUNGLE_LOG", "12"}, {"ACACIA_LOG", "12"}, {"DARK_OAK_LOG", "12"}, {"CHERRY_LOG", "15"}, {"OAK_PLANKS", "4"}, {"BRICKS", "20"}, {"MUD_BRICKS", "20"}, {"SANDSTONE", "12"}, {"RED_SANDSTONE", "16"}, {"GLASS", "12"}, {"GLOWSTONE", "100"}, {"WHITE_CONCRETE", "16"}, {"BLACK_CONCRETE", "16"}, {"TERRACOTTA", "16"}, {"WHITE_WOOL", "12"}, {"CLAY", "16"}, {"QUARTZ_BLOCK", "32"}, {"PRISMARINE", "50"}, {"AMETHYST_BLOCK", "70"}};
        for (String[] f : end) {
            this.add("End", f[0], Long.parseLong(f[1]));
        }
        for (String[] f : nether) {
            this.add("Nether", f[0], Long.parseLong(f[1]));
        }
        for (String[] f : gear) {
            this.add("Gear", f[0], Long.parseLong(f[1]));
        }
        for (String[] f : food) {
            this.add("Food", f[0], Long.parseLong(f[1]));
        }
        for (String[] f : blocks) {
            this.add("Blocks", f[0], Long.parseLong(f[1]));
        }
    }

    List<Entry> catalog(String category) {
        List<Entry> out = new ArrayList<Entry>();
        for (Entry entry : this.catalog) {
            if (entry.category().equals(category)) {
                out.add(entry);
            }
        }
        return out;
    }

    Entry find(Material material) {
        for (Entry entry : this.catalog) {
            if (entry.material() == material) {
                return entry;
            }
        }
        return null;
    }

    /* ---------- Konten ---------- */

    /**
     * Normales Geld ist pro Modus getrennt: SMP nutzt die echte Spieler-UUID, das Bau-SMP ein
     * daraus abgeleitetes Konto. Gems (Gems.java) sind davon NICHT betroffen und gelten ueberall.
     */
    static UUID bauKey(UUID id) {
        return UUID.nameUUIDFromBytes(("duelnetwork-bau:" + id).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /** Konto-Schluessel fuer die angegebene Zone. */
    static UUID key(UUID id, boolean bau) {
        return bau ? bauKey(id) : id;
    }

    /** Konto-Schluessel passend zum aktuellen Modus des Spielers. */
    static UUID key(UUID id) {
        return instance != null && instance.plugin.inBauMode(id) ? bauKey(id) : id;
    }

    static long balance(UUID id) {
        UUID k = key(id);
        return instance == null ? 0L : instance.money.getOrDefault(k, 0L);
    }

    /** Kontostand im angegebenen Modus (fuer Exporte/Admin), unabhaengig vom aktuellen Modus des Spielers. */
    static long balanceIn(UUID id, boolean bau) {
        return instance == null ? 0L : instance.money.getOrDefault(key(id, bau), 0L);
    }

    static boolean hasAccount(UUID id) {
        return instance != null && instance.money.containsKey(key(id));
    }

    /** Legt ein Konto mit Startguthaben an, falls es noch keins gibt (z. B. durch Vault). */
    static void ensureAccount(UUID id, String name) {
        if (instance == null) {
            return;
        }
        id = key(id);
        try {
            instance.database.ensureAccount(id, name, instance.startBalance());
            instance.money.put(id, instance.database.balance(id));
            if (name != null) {
                instance.names.put(id, name);
            }
            instance.rankCacheTime = 0L;
        }
        catch (java.sql.SQLException ex) {
            Store.problem("Economy.ensureAccount", ex);
        }
    }

    static boolean has(UUID id, long amount) {
        return balance(id) >= amount;
    }

    /** Grosse Betraege werden im audit.log festgehalten (economy.audit-threshold, 0 = aus). */
    private static void audit(String kind, UUID id, long amount) {
        if (instance == null) {
            return;
        }
        long threshold = instance.plugin.getConfig().getLong("economy.audit-threshold", 100000L);
        if (threshold > 0L && Math.abs(amount) >= threshold) {
            Audit.log("MONEY", kind + " " + instance.names.getOrDefault(id, String.valueOf(id)) + " (" + id + ") " + amount);
        }
    }

    static void add(UUID id, String name, long amount) {
        addKey(key(id), name, amount);
    }

    /** Gutschrift im Konto der angegebenen Zone (z. B. /pay: Zone des Absenders). */
    static void add(UUID id, String name, long amount, boolean bau) {
        addKey(key(id, bau), name, amount);
    }

    private static void addKey(UUID id, String name, long amount) {
        if (instance == null || amount <= 0L) {
            return;
        }
        try {
            if (!instance.database.credit(id, name, amount, "money-credit-" + UUID.randomUUID())) {
                return;
            }
            audit("+", id, amount);
            instance.money.put(id, instance.database.balance(id));
            if (name != null) {
                instance.names.put(id, name);
            }
            instance.rankCacheTime = 0L;
        }
        catch (java.sql.SQLException ex) {
            Store.problem("Economy.add", ex);
        }
    }

    /** Zieht Geld ab, falls genug da ist. */
    static boolean take(UUID id, long amount) {
        if (instance == null || amount < 0L || balance(id) < amount) {
            return false;
        }
        id = key(id);
        try {
            if (!instance.database.debit(id, amount, "money-debit-" + UUID.randomUUID())) {
                return false;
            }
            audit("-", id, amount);
            instance.money.put(id, instance.database.balance(id));
            instance.rankCacheTime = 0L;
            return true;
        }
        catch (java.sql.SQLException ex) {
            Store.problem("Economy.take", ex);
            return false;
        }
    }

    static void set(UUID id, String name, long amount) {
        if (instance == null) {
            return;
        }
        id = key(id);
        try {
            if (instance.database.set(id, name, Math.max(0L, amount), "money-set-" + UUID.randomUUID())) {
                instance.money.put(id, instance.database.balance(id));
                instance.names.put(id, name);
                instance.rankCacheTime = 0L;
            }
        }
        catch (java.sql.SQLException ex) {
            Store.problem("Economy.set", ex);
        }
    }

    /** Beginnt einen externen Ablauf; derselbe txId-Aufruf ist nach einem Crash sicher wiederholbar. */
    static String beginJournal(String type, String payload) {
        if (instance == null) return null;
        String id = "journal-" + UUID.randomUUID();
        try {
            instance.database.beginJournal(id, type, payload);
            return id;
        }
        catch (java.sql.SQLException ex) {
            Store.problem("Economy.beginJournal", ex);
            return null;
        }
    }

    static void completeJournal(String id) {
        if (instance == null || id == null) return;
        try {
            instance.database.completeJournal(id);
        }
        catch (java.sql.SQLException ex) {
            Store.problem("Economy.completeJournal", ex);
        }
    }

    private void recoverPendingJournals() {
        try {
            for (EconomyDatabase.JournalEntry entry : this.database.pendingJournals()) {
                Map<String, String> values = new HashMap<>();
                for (String token : entry.payload().split(";")) {
                    int equals = token.indexOf('=');
                    if (equals > 0) values.put(token.substring(0, equals), token.substring(equals + 1));
                }
                long amount = Math.max(0L, Long.parseLong(values.getOrDefault("amount", "0")));
                boolean bau = "1".equals(values.get("bau"));
                if (entry.type().equals("ORDER_CREATE")) {
                    debitOnce(key(UUID.fromString(values.get("buyer")), bau), amount, entry.id() + ":debit");
                }
                else if (entry.type().equals("ORDER_CANCEL") || entry.type().equals("ORDER_DELIVERY")) {
                    String owner = values.get(entry.type().equals("ORDER_CANCEL") ? "buyer" : "supplier");
                    creditOnce(key(UUID.fromString(owner), bau), null, amount, entry.id() + (entry.type().equals("ORDER_CANCEL") ? ":refund" : ":pay"));
                }
                else if (entry.type().equals("AH_BUY")) {
                    debitOnce(key(UUID.fromString(values.get("buyer")), bau), amount, entry.id() + ":debit");
                    long proceeds = Math.max(0L, Long.parseLong(values.getOrDefault("proceeds", String.valueOf(amount))));
                    creditOnce(key(UUID.fromString(values.get("seller")), bau), null, proceeds, entry.id() + ":credit");
                }
                this.database.completeJournal(entry.id());
                this.plugin.getLogger().info("Economy-Recovery: " + entry.type() + " " + entry.id() + " abgeschlossen.");
            }
        }
        catch (Exception ex) {
            this.plugin.getLogger().warning("Economy-Recovery konnte nicht vollständig ausgeführt werden: " + ex.getMessage());
        }
    }

    static boolean creditOnce(UUID id, String name, long amount, String txId) {
        if (instance == null || txId == null) return false;
        try {
            boolean result = instance.database.credit(id, name, amount, txId);
            if (result) {
                instance.money.put(id, instance.database.balance(id));
                if (name != null) instance.names.put(id, name);
                instance.rankCacheTime = 0L;
            }
            return result;
        }
        catch (java.sql.SQLException ex) {
            Store.problem("Economy.creditOnce", ex);
            return false;
        }
    }

    static boolean debitOnce(UUID id, long amount, String txId) {
        if (instance == null || txId == null) return false;
        try {
            boolean result = instance.database.debit(id, amount, txId);
            if (result) {
                instance.money.put(id, instance.database.balance(id));
                instance.rankCacheTime = 0L;
            }
            return result;
        }
        catch (java.sql.SQLException ex) {
            Store.problem("Economy.debitOnce", ex);
            return false;
        }
    }

    /** Platz in der Geld-Rangliste (1 = reichster). */
    static int rank(UUID id) {
        if (instance == null) {
            return 0;
        }
        List<UUID> order = instance.ranking();
        int index = order.indexOf(id);
        return index < 0 ? order.size() + 1 : index + 1;
    }

    static int accounts() {
        return instance == null ? 0 : instance.money.size();
    }

    static String nameOf(UUID id) {
        return instance == null ? String.valueOf(id) : instance.names.getOrDefault(id, "?");
    }

    static List<UUID> order() {
        return instance == null ? new ArrayList<UUID>() : instance.ranking();
    }

    private List<UUID> ranking() {
        long now = System.currentTimeMillis();
        if (now - this.rankCacheTime > 5000L) {
            java.util.Set<UUID> bauKeys = new java.util.HashSet<UUID>();
            for (UUID k : this.money.keySet()) {
                bauKeys.add(bauKey(k));
            }
            List<UUID> list = new ArrayList<UUID>();
            for (UUID k : this.money.keySet()) {
                if (!bauKeys.contains(k)) {
                    list.add(k);
                }
            }
            list.sort((a, b) -> Long.compare(this.money.getOrDefault(b, 0L), this.money.getOrDefault(a, 0L)));
            this.rankCache = list;
            this.rankCacheTime = now;
        }
        return this.rankCache;
    }

    /** Position beim letzten Einkommen: wer sich 5 Minuten nicht bewegt hat (AFK), bekommt nichts. */
    private final Map<UUID, org.bukkit.Location> lastIncomeSpot = new java.util.concurrent.ConcurrentHashMap<UUID, org.bukkit.Location>();

    private boolean afk(Player player) {
        if (!this.plugin.getConfig().getBoolean("economy.afk-check", true)) {
            return false;
        }
        org.bukkit.Location now = player.getLocation();
        org.bukkit.Location before = this.lastIncomeSpot.put(player.getUniqueId(), now.clone());
        return before != null && before.getWorld() != null && before.getWorld().equals(now.getWorld()) && before.distanceSquared(now) < 2.25
                && Math.abs(before.getYaw() - now.getYaw()) < 1.0f && Math.abs(before.getPitch() - now.getPitch()) < 1.0f;
    }

    private void payIncome(long amount) {
        this.lastIncomeSpot.keySet().removeIf(id -> this.plugin.getServer().getPlayer(id) == null);
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            if ("smp".equals(this.plugin.profiles.getOrDefault(player.getUniqueId(), "lobby"))) {
                if (this.afk(player)) {
                    player.sendMessage("§7AFK erkannt - diesmal kein Geld-Einkommen. §8(Bewege dich, dann geht es weiter.)");
                    continue;
                }
                if (amount > 0L) {
                    add(player.getUniqueId(), player.getName(), amount);
                    player.sendMessage("§a+" + Gui.money(amount) + " §7für 5 Minuten im SMP.");
                }
            }
        }
        if (Gems.instance != null) {
            Gems.instance.save();
        }
    }

    void save() {
        try {
            for (Map.Entry<UUID, Long> e : this.money.entrySet()) {
                String key = "players." + e.getKey();
                this.data.set(key + ".money", e.getValue());
                this.data.set(key + ".name", this.names.getOrDefault(e.getKey(), "?"));
            }
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("economy.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    void shutdown() {
        this.save();
        this.database.close();
        if (instance == this) {
            instance = null;
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        ensureAccount(id, player.getName());
        this.names.put(id, player.getName());
        this.rankCacheTime = 0L;
    }

    /* ---------- Befehle ---------- */

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl ist nur im Spiel verfügbar.");
            return true;
        }
        switch (name) {
            case "money": {
                if (args.length >= 1) {
                    Player target = this.plugin.getServer().getPlayerExact(args[0]);
                    if (target == null) {
                        player.sendMessage(ChatColor.RED + "Spieler nicht online.");
                        return true;
                    }
                    player.sendMessage("§d" + target.getName() + "§7 hat §a" + Gui.money(balance(target.getUniqueId())) + "§7 (Platz §d#" + rank(target.getUniqueId()) + "§7).");
                    return true;
                }
                player.sendMessage("§7Dein Geld: §a" + Gui.money(balance(player.getUniqueId())) + " §7• Platz §d#" + rank(player.getUniqueId()) + "§7 von " + accounts());
                return true;
            }
            case "pay": {
                if (args.length < 2) {
                    player.sendMessage("§cNutze: /pay <Spieler> <Betrag>");
                    return true;
                }
                Player target = this.plugin.getServer().getPlayerExact(args[0]);
                long amount = Gui.parseAmount(args[1]);
                // OPs/Admins: duerfen sich selbst bezahlen und mehr zahlen, als sie haben (das Geld wird dann neu erschaffen)
                boolean admin = player.isOp() || player.hasPermission("duelnetwork.admin");
                if (target == null || (target.equals(player) && !admin)) {
                    player.sendMessage(ChatColor.RED + "Spieler nicht online (oder du selbst).");
                    return true;
                }
                if (amount < 1L) {
                    player.sendMessage(ChatColor.RED + "Ungültiger Betrag.");
                    return true;
                }
                if (admin) {
                    add(target.getUniqueId(), target.getName(), amount, this.plugin.inBauMode(player.getUniqueId()));
                    // erschaffenes Geld nachvollziehbar machen (moderation.log)
                    Audit.log("PAY-ADMIN", player.getName() + " -> " + target.getName() + " +" + amount);
                    if (target.equals(player)) {
                        player.sendMessage("§7Du hast dir §a" + Gui.money(amount) + "§7 gegeben. §8(Admin) §7Kontostand: §a" + Gui.money(balance(player.getUniqueId())));
                    } else {
                        player.sendMessage("§7Du hast §d" + target.getName() + " §a" + Gui.money(amount) + "§7 gesendet. §8(Admin)");
                        target.sendMessage("§d" + player.getName() + " §7hat dir §a" + Gui.money(amount) + "§7 gesendet.");
                    }
                    Chat.coins(target);
                    return true;
                }
                if (!take(player.getUniqueId(), amount)) {
                    player.sendMessage(ChatColor.RED + "Du hast nicht genug Geld.");
                    Chat.no(player);
                    return true;
                }
                add(target.getUniqueId(), target.getName(), amount, this.plugin.inBauMode(player.getUniqueId()));
                Audit.log("PAY", player.getName() + " (" + player.getUniqueId() + ") -> " + target.getName() + " (" + target.getUniqueId() + ") " + amount);
                player.sendMessage("§7Du hast §d" + target.getName() + " §a" + Gui.money(amount) + "§7 gesendet.");
                target.sendMessage("§d" + player.getName() + " §7hat dir §a" + Gui.money(amount) + "§7 gesendet.");
                Chat.coins(target);
                Chat.click(player);
                return true;
            }
            case "baltop": {
                if (SmpTop.instance != null) {
                    SmpTop.instance.open(player, args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "geld");
                }
                return true;
            }
            case "market": {
                Market.open(this.plugin, player);
                return true;
            }
            case "worth": {
                ItemStack hand = player.getInventory().getItemInMainHand();
                if (hand == null || hand.getType() == Material.AIR) {
                    player.sendMessage("§cHalte ein Item in der Hand.");
                    return true;
                }
                if (isShulker(hand) && isEmptyShulker(hand)) {
                    player.sendMessage("§7Leere Shulkerbox: §a" + Gui.money(this.worth(hand, false, null)) + " §8(wird mit verkauft)");
                    return true;
                }
                if (isShulker(hand)) {
                    player.sendMessage("§7Inhalt der Shulkerbox: §a" + Gui.money(this.worth(hand, false, null)) + " §8(Box bleibt bei dir)");
                    return true;
                }
                long one = this.worthOf(hand, 1);
                if (one <= 0L) {
                    player.sendMessage("§cDieses Item ist nicht verkaufbar.");
                    return true;
                }
                player.sendMessage("§7Wert: §a" + Gui.money(one) + " §7pro Stück §8• §7Stack (" + hand.getAmount() + "×): §a" + Gui.money(this.worthOf(hand, hand.getAmount())) + Market.trend(player, hand.getType()));
                return true;
            }
            case "sell": {
                this.sell(player, args.length >= 1 ? args[0].toLowerCase(Locale.ROOT) : "");
                return true;
            }
            default:
                return false;
        }
    }

    /* ---------- Verkaufen (/sale) ---------- */

    private static final String SALE_TAG = "Verkaufen";
    private static final int SALE_SLOTS = 45;

    /** Grundwert eines Items ohne Marktschwankung (0 = nicht verkaufbar). */
    long basePrice(Material material) {
        Entry entry = this.find(material);
        if (entry != null) {
            return entry.sell();
        }
        long custom = this.plugin.getConfig().getLong("sell.prices." + material.name(), -1L);
        if (custom >= 0L) {
            return custom;
        }
        return Values.of(material);
    }

    /** Aktueller Marktpreis pro Stueck (0 = nicht verkaufbar). */
    long sellPrice(Material material) {
        return Market.unit(material, this.basePrice(material), false);
    }

    private static boolean container(Material material) {
        String n = material.name();
        return n.contains("SHULKER_BOX") || n.equals("BUNDLE") || n.endsWith("_BUNDLE") || n.equals("CHEST") || n.equals("TRAPPED_CHEST")
                || n.equals("BARREL") || n.equals("FURNACE") || n.equals("BLAST_FURNACE") || n.equals("SMOKER") || n.equals("HOPPER")
                || n.equals("DROPPER") || n.equals("DISPENSER") || n.equals("BREWING_STAND") || n.equals("CHISELED_BOOKSHELF");
    }

    /* ---------- Bewertung (Verzauberungen, Haltbarkeit, Shulker) ---------- */

    private static final Map<String, Long> ENCHANT = new HashMap<String, Long>();

    static {
        String[] table = {"mending=900", "unbreaking=130", "efficiency=160", "fortune=450", "silk_touch=700", "sharpness=170", "smite=60", "bane_of_arthropods=50",
                "fire_aspect=220", "knockback=110", "looting=350", "sweeping_edge=130", "protection=170", "fire_protection=80", "blast_protection=100",
                "projectile_protection=80", "feather_falling=120", "respiration=90", "aqua_affinity=130", "thorns=90", "depth_strider=130", "frost_walker=450",
                "soul_speed=350", "swift_sneak=650", "power=130", "punch=90", "flame=150", "infinity=800", "luck_of_the_sea=160", "lure=110", "loyalty=220",
                "impaling=110", "riptide=350", "channeling=350", "multishot=320", "quick_charge=170", "piercing=90", "density=320", "breach=320", "wind_burst=450",
                "binding_curse=0", "vanishing_curse=0"};
        for (String entry : table) {
            int eq = entry.indexOf('=');
            ENCHANT.put(entry.substring(0, eq), Long.parseLong(entry.substring(eq + 1)));
        }
    }

    private static boolean shulker(Material material) {
        return material != null && material.name().endsWith("SHULKER_BOX");
    }

    private static long enchantBonus(Map<org.bukkit.enchantments.Enchantment, Integer> map) {
        long bonus = 0L;
        if (map == null) {
            return 0L;
        }
        for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> e : map.entrySet()) {
            String key;
            try {
                key = e.getKey().getKey().getKey();
            }
            catch (Throwable t) {
                key = "";
            }
            long per = ENCHANT.getOrDefault(key, 100L) * 5L / 2L;
            int level = e.getValue() == null ? 1 : e.getValue();
            // hoehere Stufen sind ueberproportional wertvoll
            bonus += per * level + per * Math.max(0, level - 1) * level / 4;
        }
        return bonus;
    }

    /** Wert eines einzelnen Stuecks inkl. Haltbarkeit und Verzauberungen (ohne Marktschwankung). */
    private long unitBase(ItemStack stack) {
        Material type = stack.getType();
        long base = this.basePrice(type);
        long bonus = 0L;
        if (type.name().equals("ENCHANTED_BOOK")) {
            base = Math.max(base, 100L);
        }
        try {
            org.bukkit.inventory.meta.ItemMeta meta = stack.hasItemMeta() ? stack.getItemMeta() : null;
            if (meta instanceof org.bukkit.inventory.meta.EnchantmentStorageMeta storage) {
                bonus += enchantBonus(storage.getStoredEnchants());
            }
            if (meta instanceof org.bukkit.inventory.meta.Damageable damageable && type.getMaxDurability() > 0 && damageable.hasDamage()) {
                double left = Math.max(0.0d, 1.0d - (double)damageable.getDamage() / (double)type.getMaxDurability());
                base = Math.round(base * (0.35d + 0.65d * left));
            }
        }
        catch (Throwable ignored) {
            // nur Bewertung
            Store.problem("Economy:450", ignored);
        }
        bonus += enchantBonus(stack.getEnchantments());
        return base + bonus;
    }

    /** Hat das Item einen eigenen Namen? (solche Items gelten als "Spezial" und sind nicht verkaufbar - ausser Shulkerboxen). */
    private static boolean named(ItemStack stack) {
        if (!stack.hasItemMeta()) {
            return false;
        }
        org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.hasDisplayName();
    }

    /**
     * Auszahlung fuer das Item (bei Shulkerboxen: Wert des Inhalts). Mit commit=true wird der Markt bewegt und bei Shulkerboxen
     * der verkaufte Inhalt entfernt - die Box selbst bleibt dem Spieler.
     * @param count zaehlt die verkauften Items (optional)
     */
    long worth(ItemStack stack, boolean commit, int[] count) {
        if (stack == null || stack.getType() == Material.AIR) {
            return 0L;
        }
        Material type = stack.getType();
        try {
            if (shulker(type)) {
                org.bukkit.inventory.meta.ItemMeta meta = stack.hasItemMeta() ? stack.getItemMeta() : null;
                if (!(meta instanceof org.bukkit.inventory.meta.BlockStateMeta blockMeta)) {
                    return 0L;
                }
                org.bukkit.block.BlockState state = blockMeta.getBlockState();
                if (!(state instanceof org.bukkit.block.ShulkerBox box)) {
                    return 0L;
                }
                Inventory contents = box.getInventory();
                if (isEmptyShulker(stack)) {
                    // leere Box: die Box selbst wird verkauft
                    long boxBase = this.basePrice(type);
                    if (boxBase <= 0L || named(stack)) {
                        return 0L;
                    }
                    if (commit && count != null) {
                        count[0] += 1;
                    }
                    return commit ? Market.sell(type, boxBase, 1) : Market.quoteSell(type, boxBase, 1);
                }
                long total = 0L;
                for (int i = 0; i < contents.getSize(); i++) {
                    ItemStack inner = contents.getItem(i);
                    if (inner == null || shulker(inner.getType()) || named(inner)) {
                        continue;
                    }
                    long value = this.worth(inner, commit, count);
                    if (value > 0L) {
                        total += value;
                        if (commit) {
                            contents.setItem(i, null);
                        }
                    }
                }
                if (commit && total > 0L) {
                    blockMeta.setBlockState(state);
                    stack.setItemMeta(blockMeta);
                }
                return total;
            }
            if (named(stack) || (container(type) && stack.hasItemMeta())) {
                return 0L;
            }
            long base = this.unitBase(stack);
            if (base <= 0L) {
                return 0L;
            }
            int n = Math.max(1, stack.getAmount());
            long value = commit ? Market.sell(type, base, n) : Market.quoteSell(type, base, n);
            if (commit && count != null) {
                count[0] += n;
            }
            return value;
        }
        catch (Throwable t) {
            return 0L;
        }
    }

    /** Wert von {@code n} Stueck dieses Items zum aktuellen Marktpreis (fuer die Tooltips). */
    long worthOf(ItemStack stack, int n) {
        if (stack == null || shulker(stack.getType()) || named(stack)) {
            return 0L;
        }
        long base = this.unitBase(stack);
        return base <= 0L ? 0L : Market.quoteSell(stack.getType(), base, Math.max(1, n));
    }

    boolean sellable(ItemStack stack) {
        return stack != null && stack.getType() != Material.AIR && this.worth(stack, false, null) > 0L;
    }

    /** Shulkerbox ohne jeden Inhalt? */
    static boolean isEmptyShulker(ItemStack stack) {
        try {
            if (!isShulker(stack)) {
                return false;
            }
            org.bukkit.inventory.meta.ItemMeta meta = stack.hasItemMeta() ? stack.getItemMeta() : null;
            if (!(meta instanceof org.bukkit.inventory.meta.BlockStateMeta blockMeta)) {
                return true;
            }
            org.bukkit.block.BlockState state = blockMeta.getBlockState();
            if (!(state instanceof org.bukkit.block.ShulkerBox box)) {
                return true;
            }
            Inventory contents = box.getInventory();
            for (int i = 0; i < contents.getSize(); i++) {
                ItemStack inner = contents.getItem(i);
                if (inner != null && inner.getType() != Material.AIR) {
                    return false;
                }
            }
            return true;
        }
        catch (Throwable t) {
            return false;
        }
    }

    static boolean isShulker(ItemStack stack) {
        return stack != null && shulker(stack.getType());
    }

    private void openSale(Player player) {
        UUID id = player.getUniqueId();
        if (this.plugin.activeDuelPlayers.contains(id) || this.plugin.activePartyMatches.containsKey(id)) {
            player.sendMessage("\u00a7cIm Duel kannst du nichts verkaufen.");
            return;
        }
        Inventory inv = this.plugin.getServer().createInventory(null, 54, "\u00a78" + SALE_TAG + " \u00a78\u00bb \u00a7aItems hineinlegen");
        ItemStack border = Gui.item(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = SALE_SLOTS; i < 54; i++) {
            inv.setItem(i, border.clone());
        }
        this.updateSaleInfo(inv, player);
        player.openInventory(inv);
        player.sendMessage("\u00a77Lege Items hinein und klicke auf \u00a7a\u00a7lVerkaufen\u00a77. Schlie\u00dfen gibt alles zur\u00fcck.");
    }

    private long saleTotal(Inventory inv) {
        long total = 0L;
        for (int i = 0; i < SALE_SLOTS; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack != null) {
                total += this.worth(stack, false, null);
            }
        }
        return total;
    }

    private void updateSaleInfo(Inventory inv, Player viewer) {
        long total = this.saleTotal(inv);
        inv.setItem(49, Gui.item(Material.EMERALD, "\u00a7a\u00a7l\u2714 Verkaufen", "\u00a77Auszahlung: \u00a7a" + Gui.money(total),
                total > 0L ? "\u00a7eKlicke zum Verkaufen" : "\u00a78Lege zuerst Items hinein"));
        inv.setItem(53, Gui.item(Material.RED_DYE, "\u00a7c\u00a7l\u2716 Abbrechen", "\u00a77Schlie\u00dft das Men\u00fc,", "\u00a77alle Items kommen zur\u00fcck."));
        inv.setItem(45, Gui.item(Material.BOOK, "\u00a7d\u00a7lSo geht\u2019s", "\u00a77Items hineinlegen \u2192 \u00a7aVerkaufen\u00a77 klicken", "\u00a77Verzauberte Items sind mehr wert,", "\u00a77Shulker: Inhalt wird verkauft, Box bleibt – leere Box wird selbst verkauft.", "\u00a77Preise folgen Angebot und Nachfrage."));
    }

    private void processSale(Player player, Inventory inv) {
        long total = 0L;
        int items = 0;
        for (int i = 0; i < SALE_SLOTS; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            int[] sold = new int[1];
            boolean boxWasEmpty = isEmptyShulker(stack);
            long value = this.worth(stack, true, sold);
            if (value <= 0L) {
                continue;
            }
            total += value;
            items += sold[0];
            inv.setItem(i, isShulker(stack) && !boxWasEmpty ? stack : null);
        }
        if (total <= 0L) {
            player.sendMessage("\u00a7cNichts verkaufbar im Men\u00fc.");
            Chat.no(player);
            return;
        }
        add(player.getUniqueId(), player.getName(), total);
        player.sendMessage("\u00a7a\u2714 Verkauft: \u00a7f" + items + " Items \u00a77f\u00fcr \u00a7a" + Gui.money(total) + " \u00a78\u2022 \u00a77Kontostand: \u00a7a" + Gui.money(balance(player.getUniqueId())));
        Chat.coins(player);
    }

    @EventHandler
    public void onSaleClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!Gui.isTitle(title, SALE_TAG) || !(event.getWhoClicked() instanceof Player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot >= SALE_SLOTS && slot < 54) {
            event.setCancelled(true);
            Player seller = (Player)event.getWhoClicked();
            if (slot == 49) {
                this.processSale(seller, event.getView().getTopInventory());
                this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> seller.closeInventory());
            } else if (slot == 53) {
                this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> seller.closeInventory());
            }
            return;
        }
        Inventory top = event.getView().getTopInventory();
        final Player viewer = (Player)event.getWhoClicked();
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> this.updateSaleInfo(top, viewer));
    }

    @EventHandler
    public void onSaleClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (!Gui.isTitle(title, SALE_TAG) || !(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory inv = event.getInventory();
        int returned = 0;
        for (int i = 0; i < SALE_SLOTS; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            returned += stack.getAmount();
            int left = Gui.give(player, stack.clone());
            if (left > 0) {
                ItemStack rest = stack.clone();
                rest.setAmount(left);
                player.getWorld().dropItemNaturally(player.getLocation(), rest);
            }
            inv.setItem(i, null);
        }
        if (returned > 0) {
            player.sendMessage("\u00a77" + returned + " Items zur\u00fcckgegeben.");
        }
    }

    private void sell(Player player, String mode) {
        if (!mode.equals("hand") && !mode.equals("all")) {
            this.openSale(player);
            return;
        }
        ItemStack[] contents = player.getInventory().getContents();
        long total = 0L;
        int items = 0;
        for (int i = 0; i < Math.min(36, contents.length); i++) {
            ItemStack stack = contents[i];
            if (stack == null) {
                continue;
            }
            boolean hand = mode.equals("hand") && i == player.getInventory().getHeldItemSlot();
            if (!(mode.equals("all") || hand)) {
                continue;
            }
            int[] sold = new int[1];
            boolean boxWasEmpty = isEmptyShulker(stack);
            long value = this.worth(stack, true, sold);
            if (value <= 0L) {
                continue;
            }
            total += value;
            items += sold[0];
            if (isShulker(stack) && !boxWasEmpty) {
                player.getInventory().setItem(i, stack);
            } else {
                player.getInventory().setItem(i, null);
            }
        }
        if (items == 0) {
            player.sendMessage("\u00a7cNichts verkaufbar. \u00a7f/sale \u00a7cer\u00f6ffnet das Verkaufsmen\u00fc.");
            Chat.no(player);
            return;
        }
        add(player.getUniqueId(), player.getName(), total);
        player.sendMessage("\u00a7a\u2714 Verkauft: \u00a7f" + items + " Items \u00a77f\u00fcr \u00a7a" + Gui.money(total));
        Chat.coins(player);
    }

    /** /duelladmin eco <give|take|set> <Spieler> <Betrag> | check <Spieler> */
    static void admin(DuelNetworkPlugin plugin, Player admin, String[] args) {
        if (instance == null) {
            return;
        }
        if (args.length < 3) {
            admin.sendMessage("§d/duelladmin eco <give|take|set|check> <Spieler> [Betrag]");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[2]);
        if (target == null) {
            admin.sendMessage(ChatColor.RED + "Spieler muss online sein.");
            return;
        }
        UUID id = target.getUniqueId();
        String sub = args[1].toLowerCase(Locale.ROOT);
        if (sub.equals("check")) {
            admin.sendMessage("§d" + target.getName() + "§7: §a" + Gui.money(balance(id)));
            return;
        }
        long amount = args.length >= 4 ? Gui.parseAmount(args[3]) : -1L;
        if (amount < 0L && !(sub.equals("set") && args.length >= 4 && args[3].equals("0"))) {
            admin.sendMessage(ChatColor.RED + "Betrag fehlt oder ungültig.");
            return;
        }
        switch (sub) {
            case "give" -> add(id, target.getName(), amount);
            case "take" -> set(id, target.getName(), Math.max(0L, balance(id) - amount));
            case "set" -> set(id, target.getName(), Math.max(0L, amount));
            default -> {
                admin.sendMessage(ChatColor.RED + "give, take, set oder check.");
                return;
            }
        }
        admin.sendMessage("§a" + target.getName() + " hat jetzt " + Gui.money(balance(id)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<String>();
        if ((name.equals("money") || name.equals("pay")) && args.length == 1) {
            for (Player p : this.plugin.getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        } else if (name.equals("sell") && args.length == 1) {
            out.add("hand");
            out.add("all");
        }
        return out;
    }
}
