package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.bukkit.plugin.Plugin;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Gems: 1 Gem pro 10 Minuten Onlinezeit in jedem Modus, Gem-Shop mit Hammer-Spitzhacke (3x3) und Faeller-Axt (ganzer Baum).
 * Config: gems.minutes-per-gem (10), gems.price.pickaxe (1000), gems.price.axe (1000).
 */
final class Gems implements Listener, CommandExecutor, org.bukkit.command.TabCompleter {
    static Gems instance;
    private static final String TAG = "Gem-Shop";
    static final String PICK = "§b§lHammer-Spitzhacke";
    static final String AXE = "§a§lFäller-Axt";
    private static final int TOOL_EFFICIENCY = 5;
    private static final int MAX_TREE = 1200;
    static final String SHOVEL = "§e§lHammer-Schaufel";

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<UUID, Integer> gems = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> minutes = new ConcurrentHashMap<UUID, Integer>();
    private final Set<UUID> busy = ConcurrentHashMap.newKeySet();

    Gems(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.startHaste();
        this.file = new File(plugin.getDataFolder(), "gems.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        org.bukkit.configuration.ConfigurationSection players = this.data.getConfigurationSection("players");
        if (players != null) {
            for (String key : players.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    this.gems.put(id, this.data.getInt("players." + key + ".gems", 0));
                    this.minutes.put(id, this.data.getInt("players." + key + ".minutes", 0));
                }
                catch (IllegalArgumentException ignored) {
                    // kaputter Eintrag
                }
            }
        }
        if (plugin.getCommand("gemboost") != null) {
            plugin.getCommand("gemboost").setExecutor((CommandExecutor)this);
            plugin.getCommand("gemboost").setTabCompleter((org.bukkit.command.TabCompleter)this);
        }
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("GemBoost", this::boostTick), 400L, 400L);
        // Onlinezeit laeuft unabhaengig vom Geld-Timer: Lobby, SMP, Bau-SMP, Duelle und Events zaehlen.
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("GemOnlineTime", this::onlineMinuteTick), 1200L, 1200L);
        if (plugin.getCommand("gems") != null) {
            plugin.getCommand("gems").setExecutor((CommandExecutor)this);
            plugin.getCommand("gems").setTabCompleter((org.bukkit.command.TabCompleter)this);
        }
    }

    /** true, solange fuer diesen Spieler gerade Extra-Bloecke eines Gem-Werkzeugs abgebaut werden (Anti-Cheat ignoriert diese Events). */
    static boolean busyFor(UUID id) {
        return instance != null && instance.busy.contains(id);
    }

    static int balance(UUID id) {
        return instance == null ? 0 : instance.gems.getOrDefault(id, 0);
    }

    void save() {
        try {
            Set<UUID> players = new HashSet<UUID>(this.gems.keySet());
            players.addAll(this.minutes.keySet());
            for (UUID id : players) {
                this.data.set("players." + id + ".gems", this.gems.getOrDefault(id, 0));
                this.data.set("players." + id + ".minutes", this.minutes.getOrDefault(id, 0));
            }
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("gems.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    /** Die Spieler mit den meisten Gems (fuer Ranglisten). */
    static List<Map.Entry<UUID, Integer>> top(int limit) {
        List<Map.Entry<UUID, Integer>> list = new java.util.ArrayList<Map.Entry<UUID, Integer>>();
        if (instance != null) {
            for (Map.Entry<UUID, Integer> e : instance.gems.entrySet()) {
                if (e.getValue() > 0) {
                    list.add(new java.util.AbstractMap.SimpleEntry<UUID, Integer>(e.getKey(), e.getValue()));
                }
            }
        }
        list.sort((x, y) -> Integer.compare(y.getValue(), x.getValue()));
        return new java.util.ArrayList<Map.Entry<UUID, Integer>>(list.subList(0, Math.min(limit, list.size())));
    }

    /* ---------- Gem-Boost (/gemboost) ---------- */

    private int boostFactor = 1;
    private long boostUntil;
    private boolean remind10;
    private boolean remind1;

    private int factor() {
        return System.currentTimeMillis() < this.boostUntil ? this.boostFactor : 1;
    }

    /** Multipliziert verdiente Gems (Spielzeit, Daily, Quests) mit dem aktuellen Boost. */
    static int boosted(int amount) {
        return instance == null ? amount : amount * instance.factor();
    }

    private String boostLeft() {
        long minutes = Math.max(1L, (this.boostUntil - System.currentTimeMillis() + 59999L) / 60000L);
        return minutes >= 60L ? (minutes / 60L) + " Std " + (minutes % 60L) + " Min" : minutes + " Min";
    }

    private boolean boostCommand(CommandSender sender, String[] args) {
        boolean admin = sender.isOp() || sender.hasPermission("duelnetwork.admin");
        if (args.length == 0) {
            sender.sendMessage(this.factor() > 1 ? "§b✦ Gem-Boost §fx" + this.factor() + " §7aktiv, noch §f" + this.boostLeft() + "§7." : "§7Gerade ist kein Gem-Boost aktiv." + (admin ? " §8Starten: §d/gemboost <2-10> [Minuten]" : ""));
            return true;
        }
        if (!admin) {
            sender.sendMessage("§cDafür hast du keine Berechtigung.");
            return true;
        }
        if (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("stop")) {
            this.boostUntil = 0L;
            this.boostFactor = 1;
            this.plugin.getServer().broadcastMessage("§b✦ Der Gem-Boost wurde beendet.");
            return true;
        }
        int factor;
        long minutes = 60L;
        try {
            factor = Integer.parseInt(args[0].replace("x", "").replace("X", ""));
            if (args.length >= 2) {
                minutes = Long.parseLong(args[1]);
            }
        }
        catch (NumberFormatException ex) {
            sender.sendMessage("§cNutze: /gemboost <2-10> [Minuten] §7oder §d/gemboost off");
            return true;
        }
        if (factor < 2 || factor > 10 || minutes < 1L || minutes > 1440L) {
            sender.sendMessage("§cFaktor 2 bis 10, Dauer 1 bis 1440 Minuten.");
            return true;
        }
        this.boostFactor = factor;
        this.boostUntil = System.currentTimeMillis() + minutes * 60000L;
        this.remind10 = minutes > 10L;
        this.remind1 = minutes > 1L;
        this.plugin.getServer().broadcastMessage("§b✦ §lGem-Boost x" + factor + "§r §7für §f" + this.boostLeft() + "§7! Alle Gems aus Spielzeit und §d/daily §7zählen mehrfach.");
        for (Player online : this.plugin.getServer().getOnlinePlayers()) {
            online.sendTitle("§b§lGEM-BOOST x" + factor, "§7für " + this.boostLeft(), 10, 60, 20);
            Chat.ping(online);
        }
        sender.sendMessage("§aGem-Boost x" + factor + " gestartet (" + minutes + " Min). §7Er endet bei einem Neustart des Servers.");
        return true;
    }

    private void boostTick() {
        if (this.boostUntil <= 0L) {
            return;
        }
        long left = this.boostUntil - System.currentTimeMillis();
        if (left <= 0L) {
            this.boostUntil = 0L;
            this.boostFactor = 1;
            this.plugin.getServer().broadcastMessage("§b✦ Der Gem-Boost ist vorbei. Danke fürs Mitspielen!");
        } else if (this.remind10 && left <= 600000L) {
            this.remind10 = false;
            this.plugin.getServer().broadcastMessage("§b✦ Gem-Boost x" + this.boostFactor + " noch §f10 Minuten§b!");
        } else if (this.remind1 && left <= 60000L) {
            this.remind1 = false;
            this.plugin.getServer().broadcastMessage("§b✦ Gem-Boost x" + this.boostFactor + " noch §f1 Minute§b!");
        }
    }

    /** Gems gutschreiben (statisch, fuer Belohnungen aus anderen Systemen). */
    static void give(UUID id, int amount) {
        if (instance != null) {
            instance.add(id, amount);
        }
    }

    private final Map<UUID, java.util.LinkedList<String>> history = new java.util.concurrent.ConcurrentHashMap<UUID, java.util.LinkedList<String>>();

    void add(UUID id, int amount) {
        this.gems.merge(id, amount, (a, b) -> Math.max(0, a + b));
        if (amount != 0) {
            this.remember(id, amount);
        }
    }

    /** Merkt die letzten 10 Aenderungen (seit dem letzten Neustart) mit Herkunft fuer /gems history. */
    private void remember(UUID id, int amount) {
        String source = StackWalker.getInstance().walk(frames -> frames.map(f -> f.getClassName().substring(f.getClassName().lastIndexOf('.') + 1))
                .filter(n -> !n.equals("Gems") && !n.startsWith("Perf") && !n.contains("$$Lambda") && !n.startsWith("Craft") && !n.startsWith("Scheduler"))
                .findFirst().orElse("?"));
        String label = switch (source) {
            case "Comfort" -> "Tagesbelohnung";
            case "Seasons" -> "Season-Belohnung";
            case "Shop", "Market" -> "Shop";
            case "Cosmetics" -> "Cosmetics";
            case "DuelNetworkPlugin" -> "Plugin";
            default -> source;
        };
        java.util.LinkedList<String> list = this.history.computeIfAbsent(id, k -> new java.util.LinkedList<String>());
        synchronized (list) {
            list.addFirst(new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()) + "|" + amount + "|" + label);
            while (list.size() > 10) {
                list.removeLast();
            }
        }
    }

    void showHistory(Player player) {
        java.util.LinkedList<String> list = this.history.get(player.getUniqueId());
        if (list == null || list.isEmpty()) {
            player.sendMessage("§7Noch keine Gem-Änderungen seit dem letzten Neustart.");
            return;
        }
        player.sendMessage("§b§l✦ Gem-Verlauf §8(letzte 10, seit dem letzten Neustart)");
        synchronized (list) {
            for (String row : list) {
                String[] p = row.split("\\|", 3);
                int n = Integer.parseInt(p[1]);
                player.sendMessage("§8" + p[0] + " " + (n >= 0 ? "§a+" : "§c") + n + " §7" + p[2]);
            }
        }
    }

    /** Jede Online-Minute zaehlt in jedem Spielmodus; AFK-Status und Profil sind fuer Gems egal. */
    private void onlineMinuteTick() {
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            UUID id = player.getUniqueId();
            int per = Math.max(1, this.plugin.getConfig().getInt("gems.minutes-per-gem", 10));
            int total = this.minutes.getOrDefault(id, 0) + 1;
            int earned = 0;
            while (total >= per) {
                total -= per;
                earned++;
            }
            this.minutes.put(id, total);
            if (earned > 0) {
                int factor = this.factor();
                int got = earned * factor;
                this.add(id, got);
                player.sendMessage("§b+" + got + (got == 1 ? " Gem" : " Gems §d(Boost x" + factor + ")") + " §7für " + (earned * per) + " Minuten auf dem Server. §8(§b" + balance(id) + " §8• §d/gems§8)");
                Chat.sound(player, org.bukkit.Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.4f);
                // Die verdienten Gems sofort sichern; der Restfortschritt wird beim normalen Economy-Save gesichert.
                this.save();
            }
        }
    }

    private int price(String key) {
        return Math.max(1, this.plugin.getConfig().getInt("gems.price." + key, 1000));
    }

    private static boolean isAdmin(Player p) {
        return p.isOp() || p.hasPermission("duelnetwork.admin");
    }

    /** Hebt Gem-Werkzeuge, die noch mit Effizienz IV verkauft wurden, auf Effizienz V an. */
    private void upgradeTools(Player player) {
        boolean changed = false;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || (!isTool(stack, PICK) && !isTool(stack, AXE) && !isTool(stack, SHOVEL))) {
                continue;
            }
            if (stack.getEnchantmentLevel(Enchantment.EFFICIENCY) >= TOOL_EFFICIENCY) {
                continue;
            }
            stack.addUnsafeEnchantment(Enchantment.EFFICIENCY, TOOL_EFFICIENCY);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> lore = new ArrayList<String>();
                for (String line : meta.getLore()) {
                    lore.add(line.replace("Effizienz IV", "Effizienz V"));
                }
                meta.setLore(lore);
                stack.setItemMeta(meta);
            }
            changed = true;
        }
        if (changed) {
            player.sendMessage("§bDeine Gem-Werkzeuge wurden auf §fEffizienz V §bverbessert.");
        }
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        final Player joined = event.getPlayer();
        this.plugin.getServer().getScheduler().runTaskLater((org.bukkit.plugin.Plugin)this.plugin, () -> {
            if (joined.isOnline()) {
                this.upgradeTools(joined);
            }
        }, 60L);
    }

    /** true, wenn der Spieler im SMP/Bau-SMP ist (dort gibt es die Werkzeuge, sonst Kosmetik). */
    private boolean smpMode(Player player) {
        return "smp".equals(this.plugin.profiles.getOrDefault(player.getUniqueId(), "lobby"));
    }

    void open(Player player) {
        this.upgradeTools(player);
        Inventory inv = Gui.frame(this.plugin, TAG, "Gem-Shop", 27);
        int have = balance(player.getUniqueId());
        inv.setItem(4, Gui.item(Material.AMETHYST_SHARD, "§b§lDeine Gems: §f" + have, "§7Alle " + Math.max(1, this.plugin.getConfig().getInt("gems.minutes-per-gem", 10)) + " Minuten auf dem Server gibt es 1 Gem."));
        if (this.smpMode(player)) {
            // SMP: nur Werkzeuge
            inv.setItem(11, this.pickaxe());
            inv.setItem(15, this.axe());
        } else {
            // Lobby, Duelle, Events, Parkour: Kosmetik
            inv.setItem(13, Gui.item(Material.NETHERITE_CHESTPLATE, "§d§lKosmetik", "§7Armor-Trims mit Materialien, Kill-Effekte", "§7und Projektil-Spuren fürs Duell.", "", "§e▶ Klicken zum Ansehen & Kaufen"));
            inv.setItem(22, Gui.item(Material.BARRIER, "§7Gem-Werkzeuge", "§8Spitzhacke und Axt gibt es im SMP."));
        }
        if (this.smpMode(player)) {
            inv.setItem(22, Gui.item(Material.ARROW, "§7« Schließen"));
        } else {
            inv.setItem(26, Gui.item(Material.ARROW, "§7« Schließen"));
        }
        player.openInventory(inv);
    }

    private ItemStack pickaxe() {
        ItemStack s = Gui.item(Material.DIAMOND_PICKAXE, "§b§lHammer-Spitzhacke", "§7Baut 3x3 Blöcke auf einmal ab.", "§7Schleichen: nur ein Block.", "§7Effizienz V, Haltbarkeit III, Reparatur", "§8Gem-Item", "", "§7Preis: §b" + this.price("pickaxe") + " Gems", "§eKlicken zum Kaufen");
        return this.enchantTool(s, TOOL_EFFICIENCY);
    }

    private ItemStack axe() {
        ItemStack s = Gui.item(Material.DIAMOND_AXE, "§a§lFäller-Axt", "§7Fällt einen ganzen Baum mit einem Schlag.", "§7Schleichen: nur ein Block.", "§7Effizienz V, Haltbarkeit III, Reparatur", "§8Gem-Item", "", "§7Preis: §b" + this.price("axe") + " Gems", "§eKlicken zum Kaufen");
        return this.enchantTool(s, TOOL_EFFICIENCY);
    }

    private ItemStack enchantTool(ItemStack s, int eff) {
        s.addUnsafeEnchantment(Enchantment.EFFICIENCY, eff);
        // Die Hammer-Aktionen verbrauchen bei 3x3/ganzen Bäumen viele Haltbarkeitspunkte.
        // Unbreaking X hält die Werkzeuge deutlich länger, ohne die normale Minecraft-Durability zu umgehen.
        s.addUnsafeEnchantment(Enchantment.UNBREAKING, 10);
        s.addUnsafeEnchantment(Enchantment.MENDING, 1);
        return s;
    }

    /** Fertiges Item ohne Kauf-Lore (fuer die Ausgabe): 0 = Spitzhacke, 1 = Axt, 2 = Schaufel. */
    private ItemStack product(int kind) {
        ItemStack s = kind == 0 ? Gui.item(Material.DIAMOND_PICKAXE, PICK, "§7Baut 3x3 Blöcke auf einmal ab.", "§7Schleichen: nur ein Block.", "§8Gem-Item")
                : kind == 1 ? Gui.item(Material.DIAMOND_AXE, AXE, "§7Fällt einen ganzen Baum mit einem Schlag.", "§7Schleichen: nur ein Block.", "§8Gem-Item")
                : Gui.item(Material.DIAMOND_SHOVEL, SHOVEL, "§7Gräbt 3x3 Blöcke auf einmal (Erde, Sand, Kies ...).", "§7Schleichen: nur ein Block.", "§8Gem-Item");
        return this.enchantTool(s, TOOL_EFFICIENCY);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("gemboost")) {
            return this.boostCommand(sender, args);
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        if (args.length == 1 && (args[0].equalsIgnoreCase("history") || args[0].equalsIgnoreCase("verlauf"))) {
            this.showHistory(player);
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give") && isAdmin(player)) {
            try {
                int n = Integer.parseInt(args[1]);
                this.add(player.getUniqueId(), n);
                this.save();
                player.sendMessage("§a" + n + " Gems für dich (jetzt " + balance(player.getUniqueId()) + ").");
            }
            catch (NumberFormatException e) {
                player.sendMessage("§c/gems give <Anzahl> oder /gems give <Spieler> <Anzahl>");
            }
            return true;
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase("give") && isAdmin(player)) {
            Player target = this.plugin.getServer().getPlayerExact(args[1]);
            try {
                int n = Integer.parseInt(args[2]);
                if (target == null) {
                    player.sendMessage("§cSpieler nicht gefunden.");
                    return true;
                }
                this.add(target.getUniqueId(), n);
                this.save();
                player.sendMessage("§a" + n + " Gems an " + target.getName() + " (jetzt " + balance(target.getUniqueId()) + ").");
            }
            catch (NumberFormatException e) {
                player.sendMessage("§c/gems give <Spieler> <Anzahl>");
            }
            return true;
        }
        if (this.factor() > 1) {
            player.sendMessage("§b✦ Gem-Boost §fx" + this.factor() + " §7aktiv, noch §f" + this.boostLeft() + "§7.");
        }
        player.sendMessage("§bDu hast §f" + balance(player.getUniqueId()) + " Gems§7. Alle " + Math.max(1, this.plugin.getConfig().getInt("gems.minutes-per-gem", 10)) + " Minuten auf dem Server gibt es 1 Gem.");
        this.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new java.util.ArrayList<String>();
        if (command.getName().equalsIgnoreCase("gemboost")) {
            if (!(sender.isOp() || sender.hasPermission("duelnetwork.admin"))) {
                return out;
            }
            if (args.length == 1) {
                out.addAll(List.of("2", "3", "5", "off"));
            } else if (args.length == 2) {
                out.addAll(List.of("30", "60", "120"));
            }
            return out;
        }
        if (!(sender instanceof Player player) || !isAdmin(player)) {
            return out;
        }
        if (args.length == 1 && "give".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            out.add("give");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(online.getName());
                }
            }
        }
        return out;
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
        int slot = event.getRawSlot();
        if (slot == 22 && this.smpMode(player) || slot == 26 && !this.smpMode(player)) {
            player.closeInventory();
            return;
        }
        if (!this.smpMode(player)) {
            if (Cosmetics.instance != null) {
                if (slot == 13) {
                    Cosmetics.instance.open(player);
                }
            }
            return;
        }
        if (slot != 11 && slot != 15) {
            return;
        }
        int kind = slot == 11 ? 0 : 1;
        String label = kind == 0 ? PICK : AXE;
        int cost = this.price(kind == 0 ? "pickaxe" : "axe");
        UUID id = player.getUniqueId();
        if (balance(id) < cost) {
            player.sendMessage("§cDir fehlen " + (cost - balance(id)) + " Gems.");
            Chat.no(player);
            return;
        }
        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage("§cDein Inventar ist voll.");
            Chat.no(player);
            return;
        }
        this.add(id, -cost);
        ItemStack bought = this.product(kind);
        player.getInventory().addItem(new ItemStack[]{bought});
        this.save();
        player.sendMessage("§aGekauft: " + label + " §7für §b" + cost + " Gems§7.");
        Chat.sound(player, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
        this.open(player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (Gui.isTitle(event.getView().getTitle(), TAG)) {
            event.setCancelled(true);
        }
    }

    private static String strip(String text) {
        return text == null ? "" : text.replaceAll("§.", "").replaceAll("&[0-9a-fk-or]", "").trim();
    }

    /** Erkennt Gem-Werkzeuge am Namen ohne Farbcodes und an der Lore-Zeile "Gem-Item" (robust gegen Formatierungs-Unterschiede). */
    private static boolean isTool(ItemStack stack, String name) {
        if (stack == null || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasDisplayName() || !strip(meta.getDisplayName()).equals(strip(name))) {
            return false;
        }
        List<String> lore = meta.getLore();
        if (lore == null) {
            return false;
        }
        for (String line : lore) {
            if (strip(line).equals("Gem-Item")) {
                return true;
            }
        }
        return false;
    }

    static boolean isHammer(ItemStack stack) {
        return isTool(stack, PICK);
    }

    /** Welche Flaeche schaut der Spieler an? Normale (Richtung zum Spieler) als {x,y,z}, per Strahlverfolgung - Fallback: Blickrichtung. */
    private static int[] faceNormal(Player player, Block origin) {
        Location loc = player.getLocation();
        double yaw = Math.toRadians(loc.getYaw());
        double pitch = Math.toRadians(loc.getPitch());
        double dx = -Math.sin(yaw) * Math.cos(pitch);
        double dy = -Math.sin(pitch);
        double dz = Math.cos(yaw) * Math.cos(pitch);
        double ex = loc.getX();
        double ey = loc.getY() + 1.62;
        double ez = loc.getZ();
        int px = (int)Math.floor(ex);
        int py = (int)Math.floor(ey);
        int pz = (int)Math.floor(ez);
        for (double t = 0.0; t <= 8.0; t += 0.02) {
            int bx = (int)Math.floor(ex + dx * t);
            int by = (int)Math.floor(ey + dy * t);
            int bz = (int)Math.floor(ez + dz * t);
            if (bx == px && by == py && bz == pz) {
                continue;
            }
            if (bx == origin.getX() && by == origin.getY() && bz == origin.getZ()) {
                int nx = px - bx;
                int ny = py - by;
                int nz = pz - bz;
                int sum = Math.abs(nx) + Math.abs(ny) + Math.abs(nz);
                if (sum == 1) {
                    return new int[]{nx, ny, nz};
                }
                break;
            }
            px = bx;
            py = by;
            pz = bz;
        }
        double ax = Math.abs(dx);
        double ay = Math.abs(dy);
        double az = Math.abs(dz);
        if (ay >= ax && ay >= az) {
            return new int[]{0, dy > 0 ? -1 : 1, 0};
        }
        return ax >= az ? new int[]{dx > 0 ? -1 : 1, 0, 0} : new int[]{0, 0, dz > 0 ? -1 : 1};
    }

    private static boolean hardBlock(Material m) {
        String n = m.name();
        return n.equals("OBSIDIAN") || n.equals("CRYING_OBSIDIAN") || n.equals("ANCIENT_DEBRIS") || n.equals("RESPAWN_ANCHOR");
    }

    private static boolean shovelable(Material m) {
        String n = m.name();
        return n.equals("DIRT") || n.equals("GRASS_BLOCK") || n.equals("COARSE_DIRT") || n.equals("PODZOL") || n.equals("MYCELIUM") || n.equals("ROOTED_DIRT")
                || n.equals("DIRT_PATH") || n.equals("FARMLAND") || n.equals("MUD") || n.equals("CLAY") || n.equals("GRAVEL") || n.equals("SAND") || n.equals("RED_SAND")
                || n.equals("SOUL_SAND") || n.equals("SOUL_SOIL") || n.equals("SNOW_BLOCK") || n.endsWith("_CONCRETE_POWDER");
    }

    private static boolean minable(Material m) {
        String n = m.name();
        if (m.isAir() || n.contains("CHEST") || n.contains("SPAWNER") || n.contains("BEDROCK") || n.contains("SHULKER") || n.contains("FURNACE")
                || n.contains("BARREL") || n.contains("HOPPER") || n.contains("BEACON") || n.contains("PORTAL") || n.contains("REINFORCED")
                || n.endsWith("_STAIRS") || n.endsWith("_SLAB") || n.endsWith("_WALL") || n.endsWith("_BUTTON") || n.endsWith("_PRESSURE_PLATE") || n.contains("BRICKS")
                || n.equals("GLOWSTONE") || n.equals("LODESTONE") || n.equals("STONECUTTER") || n.equals("GRINDSTONE") || (n.contains("REDSTONE") && !n.endsWith("_ORE"))
                || n.startsWith("CHISELED") || n.startsWith("POLISHED") || n.startsWith("SMOOTH") || n.startsWith("CUT_")) {
            return false;
        }
        return n.contains("STONE") || n.contains("DEEPSLATE") || n.endsWith("_ORE") || n.equals("DIRT") || n.equals("GRASS_BLOCK") || n.contains("SAND")
                || n.equals("GRAVEL") || n.equals("NETHERRACK") || n.contains("TERRACOTTA") || n.equals("GRANITE") || n.equals("DIORITE") || n.equals("ANDESITE")
                || n.equals("TUFF") || n.equals("CALCITE") || n.contains("BASALT") || n.contains("BLACKSTONE") || n.equals("OBSIDIAN") || n.equals("CLAY")
                || n.contains("MUD") || n.equals("COARSE_DIRT") || n.equals("PODZOL") || n.equals("MYCELIUM") || n.equals("ROOTED_DIRT") || n.contains("COBBLE")
                || n.equals("DRIPSTONE_BLOCK") || n.equals("MAGMA_BLOCK") || n.equals("SOUL_SOIL") || n.equals("SOUL_SAND") || n.equals("AMETHYST_BLOCK")
                || n.equals("ANCIENT_DEBRIS");
    }

    private static boolean isLog(Material m) {
        String n = m.name();
        return n.endsWith("_LOG") || n.endsWith("_WOOD") || n.endsWith("_STEM") || n.endsWith("_HYPHAE");
    }

    private static boolean isLeaves(Material m) {
        String n = m.name();
        return n.endsWith("_LEAVES") || n.endsWith("_WART_BLOCK") || n.equals("SHROOMLIGHT");
    }

    /** true, wenn nach dem Schaden noch Haltbarkeit uebrig ist; wendet den Schaden an. */
    /** true, wenn die Haltbarkeit nicht mehr fuer weitere Extra-Bloecke reicht. */
    static boolean nearlyBroken(ItemStack tool) {
        try {
            ItemMeta meta = tool.getItemMeta();
            int max = tool.getType().getMaxDurability();
            return (Object)meta instanceof Damageable d && max > 0 && d.getDamage() + 1 >= max - 1;
        }
        catch (Throwable t) {
            return false;
        }
    }

    static boolean wear(ItemStack tool, int amount) {
        try {
            ItemMeta meta = tool.getItemMeta();
            if (!((Object)meta instanceof Damageable d)) {
                return true;
            }
            int max = tool.getType().getMaxDurability();
            if (max <= 0 || amount <= 0) {
                return true;
            }
            // Die 3x3-/Baum-Aktion baut viele Blöcke über breakNaturally ab.
            // Dieser manuelle Verschleiß muss Unbreaking genauso respektieren wie
            // der normale Minecraft-Abbau, sonst gehen die Gem-Werkzeuge trotz
            // Unbreaking X genauso schnell kaputt.
            int unbreaking = Math.max(0, meta.getEnchantLevel(Enchantment.UNBREAKING));
            int applied = 0;
            java.util.concurrent.ThreadLocalRandom random = java.util.concurrent.ThreadLocalRandom.current();
            for (int i = 0; i < amount; i++) {
                if (unbreaking == 0 || random.nextInt(unbreaking + 1) == 0) {
                    applied++;
                }
            }
            if (applied == 0) {
                return true;
            }
            if (d.getDamage() + applied >= max - 1) {
                return false;
            }
            d.setDamage(d.getDamage() + applied);
            tool.setItemMeta(meta);
        }
        catch (Throwable ignored) {
            // egal
            Store.problem("Gems:272", ignored);
        }
        return true;
    }

    private boolean breakExtra(Player player, Block block, ItemStack tool) {
        BlockBreakEvent test = new BlockBreakEvent(block, player);
        this.plugin.getServer().getPluginManager().callEvent(test);
        if (test.isCancelled()) {
            return false;
        }
        // mit Abbau-Effekt (Partikel, Ton) und Erfahrung (Erze)
        try {
            block.breakNaturally(tool, true, true);
        }
        catch (NoSuchMethodError old) {
            block.breakNaturally(tool);
        }
        return true;
    }

    /** Kleiner Hinweis, hoechstens alle 10 Sekunden pro Spieler. */
    private final Map<UUID, Long> lastWarn = new ConcurrentHashMap<UUID, Long>();

    private void warn(Player player, String text) {
        long now = System.currentTimeMillis();
        Long last = this.lastWarn.get(player.getUniqueId());
        if (last == null || now - last > 10000L) {
            this.lastWarn.put(player.getUniqueId(), now);
            try {
                player.sendActionBar(net.kyori.adventure.text.Component.text(text));
            }
            catch (Throwable t) {
                player.sendMessage(text);
            }
        }
    }

    @EventHandler
    public void onQuitCleanup(org.bukkit.event.player.PlayerQuitEvent event) {
        this.lastWarn.remove(event.getPlayer().getUniqueId());
        this.busy.remove(event.getPlayer().getUniqueId());
    }

    /** 3x3 in der Ebene der angeschauten Flaeche (Hammer-Spitzhacke und Hammer-Schaufel). */
    private void hammer(Player player, Block origin, ItemStack tool, boolean shovel) {
        int[] n = faceNormal(player, origin);
        Material base = origin.getType();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                int dx;
                int dy;
                int dz;
                if (n[1] != 0) {
                    dx = a;
                    dy = 0;
                    dz = b;
                } else if (n[0] != 0) {
                    dx = 0;
                    dy = b;
                    dz = a;
                } else {
                    dx = a;
                    dy = b;
                    dz = 0;
                }
                Block target = origin.getRelative(dx, dy, dz);
                Material type = target.getType();
                boolean ok = shovel ? shovelable(type) : minable(type) && (!hardBlock(type) || type == base);
                if (!ok) {
                    continue;
                }
                if (nearlyBroken(tool)) {
                    this.warn(player, "§cDein Werkzeug ist fast kaputt - repariere es, sonst baut es nur noch einzeln ab.");
                    return;
                }
                if (this.breakExtra(player, target, tool)) {
                    wear(tool, 1);
                }
            }
        }
    }

    /**
     * Gem-Werkzeuge in der Hand geben Eile (gems.haste-level, Standard 2 = Eile II; 0 = aus): zusammen mit Effizienz V baut man
     * damit praktisch sofort ab. Der Effekt wird alle 2 Sekunden erneuert und verschwindet kurz nach dem Wegstecken.
     */
    private void startHaste() {
        this.plugin.getServer().getScheduler().runTaskTimer((org.bukkit.plugin.Plugin)this.plugin, () -> {
            try {
                int level = this.plugin.getConfig().getInt("gems.haste-level", 2);
                if (level <= 0) {
                    return;
                }
                for (Player player : this.plugin.getServer().getOnlinePlayers()) {
                    if (player.getGameMode() != GameMode.SURVIVAL || !"smp".equals(this.plugin.profiles.getOrDefault(player.getUniqueId(), "lobby"))) {
                        continue;
                    }
                    ItemStack tool = player.getInventory().getItemInMainHand();
                    if (!isTool(tool, PICK) && !isTool(tool, AXE) && !isTool(tool, SHOVEL)) {
                        continue;
                    }
                    org.bukkit.potion.PotionEffect now = player.getPotionEffect(org.bukkit.potion.PotionEffectType.HASTE);
                    if (now != null && now.getAmplifier() >= level - 1 && now.getDuration() > 50) {
                        continue;
                    }
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.HASTE, 60, level - 1, true, false, false));
                }
            }
            catch (Throwable t) {
                Store.problem("Gems:haste", t);
            }
        }, 40L, 40L);
    }

    /** Baeume werden in Schueben gefaellt (kein Tick-Ruckler bei sehr grossen Baeumen). */
    private static final int FELL_BATCH = 250;

    private void fellBatch(Player player, List<Block> logs, int from, ItemStack tool) {
        if (!player.isOnline() || from >= logs.size() || !isTool(player.getInventory().getItemInMainHand(), AXE)) {
            return;
        }
        UUID id = player.getUniqueId();
        int end = Math.min(logs.size(), from + FELL_BATCH);
        this.busy.add(id);
        try {
            for (int i = from; i < end; i++) {
                Block log = logs.get(i);
                if (!isLog(log.getType())) {
                    continue;
                }
                if (nearlyBroken(tool)) {
                    this.warn(player, "§cDein Werkzeug ist fast kaputt - repariere es, sonst fällt nur noch ein Stamm.");
                    return;
                }
                if (this.breakExtra(player, log, tool)) {
                    wear(tool, 1);
                }
            }
        }
        finally {
            this.busy.remove(id);
        }
        if (end < logs.size()) {
            final int next = end;
            this.plugin.getServer().getScheduler().runTaskLater((org.bukkit.plugin.Plugin)this.plugin, () -> this.fellBatch(player, logs, next, player.getInventory().getItemInMainHand()), 1L);
        }
    }

    /** Ganzer Baum: zusammenhaengende Staemme, nur wenn Laub in der Naehe ist (kein Abriss von Holzhaeusern). */
    private void fell(Player player, Block origin, ItemStack tool) {
        Set<Block> found = new HashSet<Block>();
        ArrayDeque<Block> queue = new ArrayDeque<Block>();
        found.add(origin);
        queue.add(origin);
        while (!queue.isEmpty() && found.size() < MAX_TREE) {
            Block c = queue.poll();
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block nb = c.getRelative(x, y, z);
                        if (found.size() < MAX_TREE && isLog(nb.getType()) && found.add(nb)) {
                            queue.add(nb);
                        }
                    }
                }
            }
        }
        if (found.size() < 2) {
            return;
        }
        boolean leaves = false;
        for (Block log : found) {
            for (int x = -2; x <= 2 && !leaves; x++) {
                for (int y = -2; y <= 2 && !leaves; y++) {
                    for (int z = -2; z <= 2 && !leaves; z++) {
                        leaves = isLeaves(log.getRelative(x, y, z).getType());
                    }
                }
            }
            if (leaves) {
                break;
            }
        }
        if (!leaves) {
            return;
        }
        List<Block> logs = new ArrayList<Block>(found);
        logs.remove(origin);
        logs.sort(java.util.Comparator.comparingInt(Block::getY));
        this.fellBatch(player, logs, 0, tool);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (this.busy.contains(id) || player.getGameMode() != GameMode.SURVIVAL || player.isSneaking()) {
            return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        boolean hammer = isTool(tool, PICK);
        boolean axe = isTool(tool, AXE);
        boolean shovel = isTool(tool, SHOVEL);
        if (!hammer && !axe && !shovel) {
            return;
        }
        if (!"smp".equals(this.plugin.profiles.getOrDefault(id, "lobby"))) {
            return;
        }
        Block origin = event.getBlock();
        this.busy.add(id);
        try {
            if (hammer && minable(origin.getType())) {
                this.hammer(player, origin, tool, false);
            } else if (shovel && shovelable(origin.getType())) {
                this.hammer(player, origin, tool, true);
            } else if (axe && isLog(origin.getType())) {
                this.fell(player, origin, tool);
            }
        }
        catch (Throwable t) {
            Store.problem("Gems:onBreak", t);
        }
        finally {
            this.busy.remove(id);
        }
    }
}
