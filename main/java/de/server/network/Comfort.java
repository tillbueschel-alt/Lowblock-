package de.server.network;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
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
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

/**
 * Komfort und Wartung: /daily (Tagesbelohnung mit Streak), /tpmenu (Teleport-Menue)
 * und /dnwartung (CSV-Export, Backup-Test, Konfig-Pruefung).
 * Daten liegen in comfort.yml; Belohnungen sind Gems (global) und Geld im aktuellen Modus.
 */
final class Comfort implements CommandExecutor, TabCompleter, Listener {
    static Comfort instance;

    private static final String TP_TAG = "Teleport";
    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private boolean dirty;

    Comfort(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.file = new File(plugin.getDataFolder(), "comfort.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        // geaenderte Daten alle 60 s sichern (Hauptthread, kleine Datei)
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, this::save, 1200L, 1200L);
        // Konfig-Pruefung einmal nach dem Start
        plugin.getServer().getScheduler().runTaskLater((Plugin)plugin, () -> {
            for (String line : this.validateConfig()) {
                plugin.getLogger().warning("Konfig: " + line);
            }
        }, 40L);
    }

    void save() {
        if (!this.dirty) {
            return;
        }
        try {
            Store.save(this.data, this.file);
            this.dirty = false;
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("comfort.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private LocalDate today() {
        String zone = this.plugin.getConfig().getString("daily.timezone", "Europe/Berlin");
        try {
            return LocalDate.now(ZoneId.of(zone));
        }
        catch (RuntimeException ex) {
            return LocalDate.now();
        }
    }

    /* ---------- Taegliche Belohnung ---------- */

    private boolean dailyClaimed(UUID id) {
        return this.today().toString().equals(this.data.getString("daily." + id + ".last", ""));
    }

    private void daily(Player player) {
        if (!this.plugin.getConfig().getBoolean("daily.enabled", true)) {
            player.sendMessage("§cDie tägliche Belohnung ist ausgeschaltet.");
            return;
        }
        UUID id = player.getUniqueId();
        if (this.dailyClaimed(id)) {
            player.sendMessage("§7Du hast deine Tagesbelohnung heute schon geholt. Aktuelle Serie: §d" + this.data.getInt("daily." + id + ".streak", 1) + " Tag(e)§7. Komm morgen wieder!");
            Chat.no(player);
            return;
        }
        LocalDate today = this.today();
        boolean continued = today.minusDays(1).toString().equals(this.data.getString("daily." + id + ".last", ""));
        int streak = continued ? this.data.getInt("daily." + id + ".streak", 0) + 1 : 1;
        int bonusDays = Math.min(Math.max(0, streak - 1), Math.max(0, this.plugin.getConfig().getInt("daily.streak-max", 6)));
        int gems = Gems.boosted(Math.max(0, this.plugin.getConfig().getInt("daily.gems", 2)) + bonusDays * Math.max(0, this.plugin.getConfig().getInt("daily.streak-bonus", 1)));
        long money = Math.max(0L, this.plugin.getConfig().getLong("daily.money", 250L));
        this.data.set("daily." + id + ".last", today.toString());
        this.data.set("daily." + id + ".streak", streak);
        this.dirty = true;
        Gems.give(id, gems);
        if (money > 0L) {
            Economy.add(id, player.getName(), money);
        }
        player.sendMessage("§d§l✦ Tagesbelohnung! §b+" + gems + " Gems" + (money > 0L ? " §8• §a+" + Gui.money(money) : "") + " §8• §7Serie: §d" + streak + " Tag(e)");
        Chat.coins(player);
        this.save();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            if (this.plugin.getConfig().getBoolean("daily.enabled", true) && !this.dailyClaimed(player.getUniqueId())) {
                Chat.buttons(player, "§d✦ §7Deine Tagesbelohnung wartet!", new String[]{"§a§l[Abholen]", "/daily", "§7Gems und Geld abholen"});
            }
        }, 100L);
    }

    /* ---------- Teleport-Menue ---------- */

    private void tpMenu(Player player) {
        Inventory inv = Gui.frame(this.plugin, TP_TAG, "Teleport-Menü", 54);
        inv.setItem(10, Gui.item(Material.RED_BED, "§d§lHomes", "§7Öffnet deine Homes", "§e▶ Klicken"));
        inv.setItem(12, Gui.item(Material.ENDER_PEARL, "§d§lZurück", "§7Zum letzten Ort (/back)", "§e▶ Klicken"));
        inv.setItem(14, Gui.item(Material.GRASS_BLOCK, "§a§lSMP", "§7Zum SMP-Spawn", "§e▶ Klicken"));
        inv.setItem(16, Gui.item(Material.NETHER_STAR, "§b§lLobby", "§7Zur Lobby", "§e▶ Klicken"));
        int[] warpSlots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        File warpFile = new File(this.plugin.getDataFolder(), "warps.yml");
        ConfigurationSection warps = YamlConfiguration.loadConfiguration(warpFile).getConfigurationSection("warps");
        if (warps != null) {
            List<String> names = new ArrayList<String>(warps.getKeys(false));
            Collections.sort(names);
            for (int i = 0; i < names.size() && i < warpSlots.length; i++) {
                inv.setItem(warpSlots[i], Gui.item(Material.ENDER_EYE, "§d" + names.get(i), "§7Warp §f" + names.get(i), "§e▶ Klicken"));
            }
        }
        int slot = 37;
        for (Player other : this.plugin.getServer().getOnlinePlayers()) {
            if (other.equals(player) || slot > 43) {
                continue;
            }
            ItemStack head = Gui.item(Material.PLAYER_HEAD, "§f" + other.getName(), "§7Teleport-Anfrage senden", "§e▶ Klicken (/tpa)");
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(other);
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!Gui.isTitle(title, TP_TAG)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        ItemStack stack = event.getCurrentItem();
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta() || !stack.getItemMeta().hasDisplayName()) {
            return;
        }
        String name = org.bukkit.ChatColor.stripColor(stack.getItemMeta().getDisplayName());
        if (name.isBlank()) {
            return;
        }
        player.closeInventory();
        switch (stack.getType()) {
            case RED_BED -> player.performCommand("home");
            case ENDER_PEARL -> player.performCommand("back");
            case GRASS_BLOCK -> player.performCommand("smp");
            case NETHER_STAR -> player.performCommand("lobby");
            case ENDER_EYE -> player.performCommand("warp " + name);
            case PLAYER_HEAD -> player.performCommand("tpa " + name);
            default -> {
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (Gui.isTitle(event.getView().getTitle(), TP_TAG)) {
            event.setCancelled(true);
        }
    }

    /* ---------- Wartung ---------- */

    private void maintenance(CommandSender sender, String[] args) {
        String sub = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "export" -> this.exportCsv(sender);
            case "backup" -> {
                sender.sendMessage("§7Backup wird erstellt ...");
                this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
                    String result;
                    try {
                        File made = this.plugin.backups.createNow();
                        result = made == null ? "§eNichts zu sichern." : "§aBackup erstellt: §f" + made.getName();
                    }
                    catch (IOException | RuntimeException ex) {
                        result = "§cBackup fehlgeschlagen: " + ex.getMessage();
                    }
                    this.reply(sender, result);
                });
            }
            case "backuptest" -> {
                sender.sendMessage("§7Prüfe das neueste Backup ...");
                this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
                    for (String line : this.testBackup()) {
                        this.reply(sender, line);
                    }
                });
            }
            case "perf" -> {
                double[] tps = this.plugin.getServer().getTPS();
                sender.sendMessage("\u00a7d\u00a7lPerformance \u00a78\u00bb \u00a77TPS (1m/5m/15m): \u00a7f" + String.format(Locale.ROOT, "%.1f / %.1f / %.1f", Math.min(20.0, tps[0]), Math.min(20.0, tps[1]), Math.min(20.0, tps[2]))
                        + " \u00a78\u2022 \u00a77MSPT: \u00a7f" + String.format(Locale.ROOT, "%.1f", this.plugin.getServer().getAverageTickTime()) + " ms");
                Runtime rt = Runtime.getRuntime();
                sender.sendMessage("\u00a77RAM: \u00a7f" + ((rt.totalMemory() - rt.freeMemory()) >> 20) + " / " + (rt.maxMemory() >> 20) + " MB \u00a78\u2022 \u00a77Spieler: \u00a7f" + this.plugin.getServer().getOnlinePlayers().size());
                for (org.bukkit.World world : this.plugin.getServer().getWorlds()) {
                    sender.sendMessage("\u00a78\u25aa \u00a7f" + world.getName() + " \u00a77Chunks: \u00a7f" + world.getLoadedChunks().length + " \u00a77Entities: \u00a7f" + world.getEntityCount());
                }
                if (Cleanup.instance != null) {
                    sender.sendMessage(Cleanup.instance.summary());
                }
                List<String> slow = Perf.report(8);
                if (!slow.isEmpty()) {
                    sender.sendMessage("\u00a77Plugin-Tasks (Hauptthread, langsamste zuerst):");
                    for (String line : slow) {
                        sender.sendMessage(line);
                    }
                }
                sender.sendMessage("\u00a77Tipp: F\u00fcr Details \u00a7f/spark profiler start \u00a77benutzen.");
            }
            case "config" -> {
                List<String> problems = this.validateConfig();
                if (problems.isEmpty()) {
                    sender.sendMessage("§a✔ Die config.yml sieht gut aus.");
                } else {
                    sender.sendMessage("§e" + problems.size() + " Hinweis(e) zur config.yml:");
                    for (String p : problems) {
                        sender.sendMessage("§7▪ " + p);
                    }
                }
            }
            default -> {
                sender.sendMessage("§d§lWartung §8» §7/dnwartung §fexport §8| §fbackup §8| §fbackuptest §8| §fconfig §8| §fperf");
                sender.sendMessage("§7Letztes Backup: §f" + this.plugin.backups.last() + " §8• §7Spieler online: §f" + this.plugin.getServer().getOnlinePlayers().size());
            }
        }
    }

    /** Nachricht aus einem Hintergrund-Thread sicher an den Absender schicken. */
    private void reply(CommandSender sender, String message) {
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> sender.sendMessage(message));
    }

    private static String csv(String value) {
        String v = value == null ? "" : value.replace("\"", "\"\"");
        return v.contains(",") || v.contains("\"") || v.contains("\n") ? "\"" + v + "\"" : v;
    }

    private void exportCsv(CommandSender sender) {
        List<String> rows = new ArrayList<String>();
        rows.add("name,uuid,smp_geld,bau_geld,gems,kills,tode,spielzeit_min");
        for (UUID id : Economy.order()) {
            rows.add(csv(Economy.nameOf(id)) + "," + id + "," + Economy.balanceIn(id, false) + "," + Economy.balanceIn(id, true) + ","
                    + Gems.balance(id) + "," + SmpTop.killsOf(id) + "," + SmpTop.deathsOf(id) + "," + SmpTop.minutesOf(id));
        }
        this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            try {
                File dir = new File(this.plugin.getDataFolder(), "export");
                dir.mkdirs();
                File out = new File(dir, "stats-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date()) + ".csv");
                Files.write(out.toPath(), rows, StandardCharsets.UTF_8);
                this.reply(sender, "§a✔ Export: §fexport/" + out.getName() + " §7(" + (rows.size() - 1) + " Spieler)");
            }
            catch (IOException ex) {
                this.reply(sender, "§cExport fehlgeschlagen: " + ex.getMessage());
            }
        });
    }

    /** Prueft das neueste Backup: YAML-Syntax, SQLite-Integritaet und fehlende YAML-Dateien. Laeuft asynchron. */
    private List<String> testBackup() {
        List<String> out = new ArrayList<String>();
        File dir = new File(this.plugin.getDataFolder(), "backups");
        File[] zips = dir.listFiles((d, n) -> n.startsWith("backup-") && n.endsWith(".zip"));
        if (zips == null || zips.length == 0) {
            out.add("§cKein Backup gefunden. Erstelle eins mit §f/dnwartung backup§c.");
            return out;
        }
        Arrays.sort(zips, Comparator.comparing(File::getName));
        File newest = zips[zips.length - 1];
        List<String> problems = BackupManager.validateArchive(newest, dir);
        int yamlCount = 0;
        int dbCount = 0;
        List<String> inZip = new ArrayList<String>();
        try (ZipFile zip = new ZipFile(newest)) {
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                inZip.add(entry.getName());
                if (entry.getName().endsWith(".yml")) yamlCount++;
                if (entry.getName().endsWith(".db") || entry.getName().endsWith(".sqlite") || entry.getName().endsWith(".sqlite3")) dbCount++;
            }
        }
        catch (IOException ex) {
            out.add("§c✖ " + newest.getName() + " lässt sich nicht öffnen: " + ex.getMessage());
            return out;
        }
        File[] live = this.plugin.getDataFolder().listFiles((d, n) -> n.endsWith(".yml"));
        int missing = 0;
        if (live != null) {
            for (File f : live) {
                if (!inZip.contains(f.getName())) {
                    missing++;
                    out.add("§e▪ " + f.getName() + " fehlt im Backup (neu seit dem Backup?)");
                }
            }
        }
        long ageMin = (System.currentTimeMillis() - newest.lastModified()) / 60000L;
        for (int i = 0; i < Math.min(8, problems.size()); i++) out.add("§c✖ " + problems.get(i));
        if (problems.size() > 8) out.add("§8… weitere Prüfhinweise: " + (problems.size() - 8));
        out.add((problems.isEmpty() && missing == 0 ? "§a✔ " : "§c✖ ") + "Backup §f" + newest.getName() + "§7: " + yamlCount + " YAML, " + dbCount + " SQLite, " + problems.size() + " Fehler, " + missing + " YAML-Datei(en) fehlen §8(" + ageMin + " Min alt)");
        if (problems.isEmpty()) {
            out.add("§7Sicherer Restore: §f/duelnetwork restore " + newest.getName() + " confirm §7(geprüft, Rueckfall-Backup, Anwendung beim nächsten Serverstart).");
        }
        return out;
    }

    /** Vergleicht die config.yml mit den Standardwerten im Plugin: fehlende Schluessel, falsche Typen, negative Zahlen. */
    List<String> validateConfig() {
        List<String> problems = new ArrayList<String>();
        try (java.io.InputStream in = this.plugin.getResource("config.yml")) {
            if (in == null) {
                return problems;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
            org.bukkit.configuration.file.FileConfiguration cfg = this.plugin.getConfig();
            for (String key : defaults.getKeys(true)) {
                Object def = defaults.get(key);
                if (def instanceof ConfigurationSection) {
                    continue;
                }
                if (!cfg.isSet(key)) {
                    problems.add("Fehlt: §f" + key + " §7(Standard: " + def + ")");
                    continue;
                }
                Object now = cfg.get(key);
                if (now == null) {
                    continue;
                }
                boolean sameKind = (def instanceof Boolean && now instanceof Boolean) || (def instanceof Number && now instanceof Number)
                        || (def instanceof String && now instanceof String) || (def instanceof List && now instanceof List)
                        || (!(def instanceof Boolean) && !(def instanceof Number) && !(def instanceof String) && !(def instanceof List));
                if (!sameKind) {
                    problems.add("Falscher Typ: §f" + key + " §7ist '" + now + "', erwartet " + def.getClass().getSimpleName());
                } else if (now instanceof Number number && number.doubleValue() < 0.0 && def instanceof Number d && d.doubleValue() >= 0.0) {
                    problems.add("Negativer Wert: §f" + key + " §7= " + now);
                }
            }
            for (String top : cfg.getKeys(false)) {
                if (!defaults.contains(top) && !top.equals("players")) {
                    problems.add("Unbekannter Abschnitt: §f" + top + " §7(Tippfehler?)");
                }
            }
        }
        catch (IOException | RuntimeException ex) {
            problems.add("Prüfung nicht möglich: " + ex.getMessage());
        }
        return problems;
    }

    /* ---------- Befehle ---------- */

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("dnwartung")) {
            if (!sender.hasPermission("duelnetwork.admin")) {
                sender.sendMessage("§cDafür fehlt dir die Berechtigung.");
                return true;
            }
            this.maintenance(sender, args);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler.");
            return true;
        }
        switch (name) {
            case "daily" -> this.daily(player);
            case "tpmenu" -> this.tpMenu(player);
            default -> {
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (command.getName().equalsIgnoreCase("dnwartung") && args.length == 1) {
            for (String sub : List.of("status", "export", "backup", "backuptest", "config", "perf")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        }
        return out;
    }
}
