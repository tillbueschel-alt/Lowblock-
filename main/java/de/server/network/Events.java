package de.server.network;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Event-Plan: Admins legen Events mit Datum und Uhrzeit fest, jeder kann das Menue (NPC, Navigator, /events)
 * auch dann oeffnen, wenn gerade kein Event laeuft, und sieht dort die anstehenden Events.
 */
final class Events implements Listener {
    static final String GUI = "Events";
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy", Locale.GERMAN);
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final long DEFAULT_MINUTES = 60L;
    private static Events instance;

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final java.util.Map<java.util.UUID, List<String>> menuRunning = new java.util.concurrent.ConcurrentHashMap<java.util.UUID, List<String>>();

    /** Ein Event aus der Datei. */
    private record Ev(String id, String name, long start, long durationMin, String desc) {
        long end() {
            return this.start + this.durationMin * 60000L;
        }
    }

    Events(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "events.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        instance = this;
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, () -> { this.scheduleTick(); this.tick(); }, 100L, 200L);
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("events.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    /* ---------- Daten ---------- */

    private List<Ev> all() {
        List<Ev> out = new ArrayList<Ev>();
        ConfigurationSection section = this.data.getConfigurationSection("events");
        if (section == null) {
            return out;
        }
        for (String id : section.getKeys(false)) {
            String p = "events." + id;
            out.add(new Ev(id, this.data.getString(p + ".name", id), this.data.getLong(p + ".start", 0L), this.data.getLong(p + ".duration", DEFAULT_MINUTES), this.data.getString(p + ".desc", "")));
        }
        out.sort(Comparator.comparingLong(Ev::start));
        return out;
    }

    private List<Ev> running() {
        long now = System.currentTimeMillis();
        List<Ev> out = new ArrayList<Ev>();
        for (Ev ev : this.all()) {
            if (ev.start() <= now && now < ev.end() && !this.data.getBoolean("events." + ev.id() + ".stopped", false)) {
                out.add(ev);
            }
        }
        return out;
    }

    private List<Ev> upcoming() {
        long now = System.currentTimeMillis();
        List<Ev> out = new ArrayList<Ev>();
        for (Ev ev : this.all()) {
            if (ev.start() > now) {
                out.add(ev);
            }
        }
        return out;
    }

    static String when(long millis) {
        ZonedDateTime time = Instant.ofEpochMilli(millis).atZone(ZONE);
        return DAY.format(time) + " um " + CLOCK.format(time) + " Uhr";
    }

    static String relative(long millis) {
        long minutes = Math.max(0L, (millis - System.currentTimeMillis()) / 60000L);
        long days = minutes / 1440L;
        long hours = minutes % 1440L / 60L;
        long mins = minutes % 60L;
        if (days > 0L) {
            return "in " + days + " Tag" + (days == 1L ? "" : "en") + (hours > 0L ? " " + hours + " Std" : "");
        }
        if (hours > 0L) {
            return "in " + hours + " Std " + mins + " Min";
        }
        return "in " + Math.max(1L, mins) + " Min";
    }

    /* ---------- Erinnerungen / Start ---------- */

    private void tick() {
        try {
            long now = System.currentTimeMillis();
            boolean changed = false;
            for (Ev ev : this.all()) {
                String p = "events." + ev.id();
                if (now > ev.end() + 86400000L) {
                    this.data.set(p, null);
                    changed = true;
                    continue;
                }
                if (!this.data.getBoolean(p + ".reminded1h", false) && now >= ev.start() - 3600000L && now < ev.start() - 900000L) {
                    this.data.set(p + ".reminded1h", true);
                    changed = true;
                    this.broadcast("§d✦ Event §f" + ev.name() + "§d startet " + relative(ev.start()) + "§d! §7Details im Event-Menü: §d/events", false);
                }
                if (!this.data.getBoolean(p + ".reminded", false) && now >= ev.start() - 600000L && now < ev.start()) {
                    this.data.set(p + ".reminded", true);
                    changed = true;
                    this.broadcast("§d✦ Event §f" + ev.name() + "§d startet " + relative(ev.start()) + "§d! §7Öffne das Event-Menü mit §d/events", false);
                }
                if (!this.data.getBoolean(p + ".started", false) && now >= ev.start() && now < ev.end()) {
                    this.data.set(p + ".started", true);
                    changed = true;
                    this.broadcast("§d✦ Das Event §f" + ev.name() + "§d läuft jetzt! §7Betritt es über §d/events", true);
                    String game = this.data.getString(p + ".game", "");
                    if (!game.isEmpty()) {
                        EventGames.autoStart(game.equalsIgnoreCase("ffa"), this.data.getString(p + ".kit", ""));
                    }
                }
            }
            if (changed) {
                this.save();
            }
        }
        catch (Throwable t) {
            this.plugin.getLogger().warning("Event-Pruefung fehlgeschlagen: " + t);
        }
    }

    private void broadcast(String message, boolean title) {
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            player.sendMessage(message);
            if (title) {
                player.sendTitle("§d§lEVENT", "§fEin Event hat begonnen!", 10, 60, 20);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
        }
    }

    /* ---------- Menue ---------- */

    private static ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        stack.setItemMeta(meta);
        return stack;
    }

    static void open(DuelNetworkPlugin plugin, Player player) {
        if (instance == null) {
            player.sendMessage("§cEvents sind gerade nicht verfügbar.");
            return;
        }
        instance.openMenu(player);
    }

    private void openMenu(Player player) {
        Inventory inv = this.plugin.getServer().createInventory(null, 45, "§8" + GUI + " §8» §dEvent-Plan");
        ItemStack border = item(Material.PURPLE_STAINED_GLASS_PANE, " ");
        ItemStack filler = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) {
            boolean edge = i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8;
            inv.setItem(i, edge ? border.clone() : filler.clone());
        }
        List<Ev> running = this.running();
        List<String> runningIds = new ArrayList<String>();
        if (EventGames.joinable()) {
            Material eventSword = EventGames.isFfa() ? Material.DIAMOND_SWORD : Material.IRON_SWORD;
            inv.setItem(4, item(eventSword, "§a§l" + EventGames.describe() + " §7• §aAnmeldung offen", "§7" + (EventGames.isFfa() ? "FFA: alle gegen alle" : "1v1-Turnier: Duelle im K.-o.-System") + " –", "§7du bleibst in der Lobby, es geht automatisch los.", "§e▶ Klicken zum Mitmachen"));
        } else if (running.isEmpty()) {
            inv.setItem(4, item(Material.BARRIER, "§c§lKein Event läuft", "§7Zurzeit läuft kein Event.", "§7Unten siehst du die nächsten Events."));
        } else {
            inv.setItem(4, item(Material.NETHER_STAR, "§d§l" + running.size() + " Event" + (running.size() == 1 ? "" : "s") + " laufen", "§7Alle laufenden Events stehen unten", "§7in der grünen Reihe."));
        }
        int[] runSlots = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < running.size() && i < runSlots.length; i++) {
            Ev ev = running.get(i);
            runningIds.add(ev.id());
            inv.setItem(runSlots[i], item(Material.FIREWORK_ROCKET, "§a§l" + ev.name() + " §7• §aLäuft!", "§7Seit §f" + CLOCK.format(Instant.ofEpochMilli(ev.start()).atZone(ZONE)) + " Uhr", ev.desc().isEmpty() ? "" : "§7" + ev.desc(), "§e▶ Klicken zum Betreten"));
        }
        this.menuRunning.put(player.getUniqueId(), runningIds);
        List<Ev> upcoming = this.upcoming();
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        if (upcoming.isEmpty()) {
            inv.setItem(31, item(Material.PAPER, "§7Keine Events geplant", "§7Schau später wieder vorbei!"));
        }
        for (int i = 0; i < upcoming.size() && i < slots.length; i++) {
            Ev ev = upcoming.get(i);
            List<String> lore = new ArrayList<String>();
            lore.add("§7" + when(ev.start()));
            lore.add("§d" + relative(ev.start()));
            lore.add("§8Dauer: ca. " + ev.durationMin() + " Min");
            if (!ev.desc().isEmpty()) {
                lore.add("§7" + ev.desc());
            }
            ItemStack stack = new ItemStack(Material.CLOCK);
            ItemMeta meta = stack.getItemMeta();
            meta.setDisplayName("§6§l" + ev.name());
            meta.setLore(lore);
            stack.setItemMeta(meta);
            inv.setItem(slots[i], stack);
        }
        boolean canJoin = !running.isEmpty() || player.hasPermission("duelnetwork.admin");
        inv.setItem(40, canJoin ? item(Material.ENDER_PEARL, "§d§lEvent-Lobby betreten", "§e▶ Klicken zum Teleportieren")
                : item(Material.BARRIER, "§c§lEvent-Lobby geschlossen", "§7Nur während eines Events offen."));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (title == null || !title.contains(GUI + " §8»") || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (slot < 0 || slot >= 45 || clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        if (slot == 4 && (clicked.getType() == Material.DIAMOND_SWORD || clicked.getType() == Material.IRON_SWORD)) {
            player.closeInventory();
            EventGames.join(player);
            return;
        }
        if (clicked.getType() == Material.BARRIER) {
            player.sendMessage("§cZurzeit läuft kein Event.");
            return;
        }
        if (clicked.getType() == Material.FIREWORK_ROCKET && slot >= 10 && slot <= 16) {
            player.closeInventory();
            List<String> ids = this.menuRunning.getOrDefault(player.getUniqueId(), List.of());
            String id = slot - 10 < ids.size() ? ids.get(slot - 10) : null;
            for (Ev ev : this.running()) {
                if (ev.id().equals(id)) {
                    this.join(player, ev);
                    return;
                }
            }
            player.sendMessage("§cDieses Event läuft nicht mehr.");
            return;
        }
        if (slot == 40) {
            player.closeInventory();
            List<Ev> running = this.running();
            if (running.isEmpty() && !player.hasPermission("duelnetwork.admin")) {
                player.sendMessage("§cZurzeit läuft kein Event.");
                return;
            }
            this.join(player, running.isEmpty() ? null : running.get(0));
        }
    }

    private void join(Player player, Ev ev) {
        if (this.plugin.activeDuelPlayers.contains(player.getUniqueId()) || this.plugin.activePartyMatches.containsKey(player.getUniqueId()) || this.plugin.activeParkourMaps.containsKey(player.getUniqueId())) {
            player.sendMessage("§cVerlasse zuerst deine laufende Runde (/lobby).");
            return;
        }
        org.bukkit.Location own = ev == null ? null : this.plugin.readConfiguredLocation("events." + ev.id() + ".spawn");
        if (own != null) {
            player.teleport(own);
            return;
        }
        if (this.plugin.teleportToSpawn(player, "events")) {
            BauSmp.title(player, "events");
        } else {
            player.sendMessage("§dEvent-Lobby: Der Spawn ist noch nicht eingerichtet.");
        }
    }

    /* ---------- Admin: /duelladmin event ... ---------- */

    static final List<String> SUBS = List.of("template", "templates", "add", "quick", "remove", "list", "time", "desc", "duration", "game", "spawn", "announce", "start", "stop");

    static void admin(DuelNetworkPlugin plugin, Player player, String[] args) {
        if (instance == null) {
            return;
        }
        instance.handleAdmin(player, args);
    }

    private static LocalDate parseDate(String text) {
        String t = text.toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now(ZONE);
        if (t.equals("heute")) {
            return today;
        }
        if (t.equals("morgen")) {
            return today.plusDays(1);
        }
        if (t.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
            String[] p = t.split("-");
            return LocalDate.of(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
        }
        String[] p = t.split("\\.");
        if (p.length >= 2) {
            int day = Integer.parseInt(p[0]);
            int month = Integer.parseInt(p[1]);
            if (p.length >= 3 && !p[2].isEmpty()) {
                int year = Integer.parseInt(p[2]);
                return LocalDate.of(year < 100 ? 2000 + year : year, month, day);
            }
            LocalDate date = LocalDate.of(today.getYear(), month, day);
            return date.isBefore(today) ? date.plusYears(1) : date;
        }
        throw new IllegalArgumentException("Datum");
    }

    private static long parseStart(String date, String time) {
        LocalDate d = parseDate(date);
        String[] t = time.replace('.', ':').split(":");
        LocalTime clock = LocalTime.of(Integer.parseInt(t[0]), t.length > 1 ? Integer.parseInt(t[1]) : 0);
        return LocalDateTime.of(d, clock).atZone(ZONE).toInstant().toEpochMilli();
    }

    /* ---------- Vorlagen und Wochenplan ---------- */

    private record Template(String name, String game, String kit, int minutes, String desc) { }

    private static final java.util.Map<String, Template> DEFAULT_TEMPLATES = new java.util.LinkedHashMap<String, Template>();
    static {
        DEFAULT_TEMPLATES.put("ffa-abend", new Template("FFA-Abend", "ffa", "", 60, "Jeder gegen jeden – der letzte Überlebende gewinnt."));
        DEFAULT_TEMPLATES.put("ffa-schnell", new Template("Schnell-FFA", "ffa", "", 20, "Kurze FFA-Runde zwischendurch."));
        DEFAULT_TEMPLATES.put("turnier-abend", new Template("Turnier-Abend", "turnier", "", 90, "1v1-Turnier mit Turnierbaum und Preisen."));
        DEFAULT_TEMPLATES.put("mini-turnier", new Template("Mini-Turnier", "turnier", "", 45, "Kleines Turnier für Zwischendurch."));
        DEFAULT_TEMPLATES.put("treffen", new Template("Community-Treffen", "", "", 60, "Wir treffen uns am Event-Spawn."));
    }

    /** Vorlagen aus config (event-templates.<id>.name/game/kit/duration/desc) plus die eingebauten. */
    private java.util.Map<String, Template> templates() {
        java.util.Map<String, Template> out = new java.util.LinkedHashMap<String, Template>(DEFAULT_TEMPLATES);
        org.bukkit.configuration.ConfigurationSection section = this.plugin.getConfig().getConfigurationSection("event-templates");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String p = key + ".";
                out.put(key.toLowerCase(Locale.ROOT), new Template(section.getString(p + "name", key), section.getString(p + "game", ""),
                        section.getString(p + "kit", ""), Math.max(1, Math.min(1440, section.getInt(p + "duration", 60))), section.getString(p + "desc", "")));
            }
        }
        return out;
    }

    static List<String> templateIds() {
        return instance == null ? new ArrayList<String>() : new ArrayList<String>(instance.templates().keySet());
    }

    /** Legt aus einer Vorlage ein Event an; start = Startzeit (Millisekunden). @return Event-ID oder null. */
    private String createFromTemplate(String templateId, long start) {
        Template t = this.templates().get(templateId.toLowerCase(Locale.ROOT));
        if (t == null) {
            return null;
        }
        String id = templateId.toLowerCase(Locale.ROOT) + "-" + java.time.Instant.ofEpochMilli(start).atZone(ZONE).format(DateTimeFormatter.ofPattern("ddMM-HHmm"));
        String p = "events." + id;
        this.data.set(p + ".name", t.name());
        this.data.set(p + ".start", start);
        this.data.set(p + ".duration", t.minutes());
        this.data.set(p + ".desc", t.desc().isEmpty() ? null : t.desc());
        this.data.set(p + ".reminded", false);
        this.data.set(p + ".started", false);
        this.data.set(p + ".stopped", false);
        if (t.game().equals("ffa") || t.game().equals("turnier")) {
            this.data.set(p + ".game", t.game());
            this.data.set(p + ".kit", t.kit());
        }
        this.save();
        return id;
    }

    /**
     * Woechentlicher Plan: config event-schedule: ["FRIDAY 20:00 ffa-abend", "SUNDAY 18:00 turnier-abend"] (Berliner Zeit).
     * Zwei Stunden vor Beginn wird das Event angelegt, damit die Erinnerungen laufen. Pro Tag und Eintrag nur einmal.
     */
    private void scheduleTick() {
        List<String> entries = this.plugin.getConfig().getStringList("event-schedule");
        if (entries.isEmpty()) {
            return;
        }
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        for (String entry : entries) {
            String[] part = entry.trim().split("\\s+");
            if (part.length < 3) {
                continue;
            }
            try {
                java.time.DayOfWeek day = java.time.DayOfWeek.valueOf(part[0].toUpperCase(Locale.ROOT));
                String[] clock = part[1].split(":");
                ZonedDateTime start = now.toLocalDate().atTime(Integer.parseInt(clock[0]), Integer.parseInt(clock[1])).atZone(ZONE);
                String done = "sched." + entry.replaceAll("[^A-Za-z0-9]", "_");
                if (day != now.getDayOfWeek() || now.isBefore(start.minusHours(2)) || now.isAfter(start.plusMinutes(30))
                        || now.toLocalDate().toString().equals(this.data.getString(done, ""))) {
                    continue;
                }
                this.data.set(done, now.toLocalDate().toString());
                String id = this.createFromTemplate(part[2], start.toInstant().toEpochMilli());
                if (id == null) {
                    this.plugin.getLogger().warning("event-schedule: Vorlage '" + part[2] + "' unbekannt.");
                }
            }
            catch (RuntimeException ex) {
                this.plugin.getLogger().warning("event-schedule: Eintrag '" + entry + "' ungueltig (Format: FRIDAY 20:00 ffa-abend).");
            }
        }
    }

    private void handleAdmin(Player player, String[] args) {
        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";
        try {
            switch (sub) {
                case "add": {
                    if (args.length < 5) {
                        break;
                    }
                    String id = args[2].toLowerCase(Locale.ROOT);
                    long start = parseStart(args[3], args[4]);
                    String name = args.length > 5 ? String.join(" ", Arrays.copyOfRange(args, 5, args.length)) : args[2];
                    String p = "events." + id;
                    this.data.set(p + ".name", name);
                    this.data.set(p + ".start", start);
                    this.data.set(p + ".duration", DEFAULT_MINUTES);
                    this.data.set(p + ".reminded", false);
                    this.data.set(p + ".started", false);
                    this.data.set(p + ".stopped", false);
                    this.save();
                    player.sendMessage(ChatColor.GREEN + "Event gespeichert: " + name + " – " + when(start));
                    return;
                }
                case "templates": {
                    player.sendMessage("§d§lEvent-Vorlagen §7(/duelladmin event template <Vorlage> [Datum] [HH:mm])");
                    for (java.util.Map.Entry<String, Template> e : this.templates().entrySet()) {
                        Template t = e.getValue();
                        player.sendMessage("§d" + e.getKey() + " §8• §f" + t.name() + " §7" + t.minutes() + " Min" + (t.game().isEmpty() ? "" : " • " + t.game().toUpperCase(Locale.ROOT)));
                    }
                    return;
                }
                case "template": {
                    // /duelladmin event template <Vorlage>              -> startet sofort
                    // /duelladmin event template <Vorlage> <Datum> <HH:mm> -> plant das Event
                    if (args.length < 3) {
                        player.sendMessage("§cNutze: /duelladmin event template <Vorlage> [Datum] [HH:mm] §7(Liste: /duelladmin event templates)");
                        return;
                    }
                    long start = args.length >= 5 ? parseStart(args[3], args[4]) : System.currentTimeMillis();
                    String id = this.createFromTemplate(args[2], start);
                    if (id == null) {
                        player.sendMessage("§cUnbekannte Vorlage. §7Liste: /duelladmin event templates");
                        return;
                    }
                    if (args.length < 5) {
                        this.data.set("events." + id + ".reminded", true);
                        this.save();
                        this.tick();
                    }
                    player.sendMessage(ChatColor.GREEN + "Event aus Vorlage angelegt: " + id + " – " + when(start));
                    return;
                }
                case "quick": {
                    // /duelladmin event quick <id> <Minuten> <Name...>  - Event startet sofort
                    if (args.length < 4) {
                        break;
                    }
                    String id = args[2].toLowerCase(Locale.ROOT);
                    int minutes = args[3].matches("\\d{1,6}") ? Math.max(1, Math.min(1440, Integer.parseInt(args[3]))) : 60;
                    String name = args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : args[2];
                    String p = "events." + id;
                    this.data.set(p + ".name", name);
                    this.data.set(p + ".start", System.currentTimeMillis());
                    this.data.set(p + ".duration", minutes);
                    this.data.set(p + ".reminded", true);
                    this.data.set(p + ".started", false);
                    this.data.set(p + ".stopped", false);
                    this.save();
                    this.tick();
                    player.sendMessage(ChatColor.GREEN + "Event gestartet: " + name + " (" + minutes + " Min). Mehrere Events können gleichzeitig laufen.");
                    return;
                }
                case "game": {
                    // /duelladmin event game <id> <ffa|turnier|none> [Kit...] - startet beim Event-Start automatisch die Anmeldung
                    if (args.length < 4 || !this.data.contains("events." + args[2].toLowerCase(Locale.ROOT))) {
                        break;
                    }
                    String p = "events." + args[2].toLowerCase(Locale.ROOT);
                    String type = args[3].toLowerCase(Locale.ROOT);
                    if (type.equals("none")) {
                        this.data.set(p + ".game", null);
                        this.data.set(p + ".kit", null);
                    } else if (type.equals("ffa") || type.equals("turnier")) {
                        this.data.set(p + ".game", type);
                        this.data.set(p + ".kit", args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : "");
                    } else {
                        break;
                    }
                    this.save();
                    player.sendMessage(ChatColor.GREEN + (type.equals("none") ? "Automatisches Spiel entfernt." : "Beim Start öffnet automatisch die " + type.toUpperCase(Locale.ROOT) + "-Anmeldung."));
                    return;
                }
                case "spawn": {
                    if (args.length < 3 || !this.data.contains("events." + args[2].toLowerCase(Locale.ROOT))) {
                        break;
                    }
                    this.plugin.getConfig().set("events." + args[2].toLowerCase(Locale.ROOT) + ".spawn", null);
                    this.plugin.getServer().dispatchCommand((org.bukkit.command.CommandSender)player, "duelladmin seteventspawn " + args[2].toLowerCase(Locale.ROOT));
                    return;
                }
                case "announce": {
                    if (args.length < 3 || !this.data.contains("events." + args[2].toLowerCase(Locale.ROOT))) {
                        break;
                    }
                    String p = "events." + args[2].toLowerCase(Locale.ROOT);
                    this.broadcast("§d✦ Event §f" + this.data.getString(p + ".name", args[2]) + "§d – " + when(this.data.getLong(p + ".start", 0L)) + "§d! §7Details: §d/events", true);
                    return;
                }
                case "time": {
                    if (args.length < 5 || !this.data.contains("events." + args[2].toLowerCase(Locale.ROOT))) {
                        break;
                    }
                    String p = "events." + args[2].toLowerCase(Locale.ROOT);
                    long start = parseStart(args[3], args[4]);
                    this.data.set(p + ".start", start);
                    this.data.set(p + ".reminded", false);
                    this.data.set(p + ".started", false);
                    this.data.set(p + ".stopped", false);
                    this.save();
                    player.sendMessage(ChatColor.GREEN + "Neue Zeit: " + when(start));
                    return;
                }
                case "desc": {
                    if (args.length < 4 || !this.data.contains("events." + args[2].toLowerCase(Locale.ROOT))) {
                        break;
                    }
                    this.data.set("events." + args[2].toLowerCase(Locale.ROOT) + ".desc", String.join(" ", Arrays.copyOfRange(args, 3, args.length)));
                    this.save();
                    player.sendMessage(ChatColor.GREEN + "Beschreibung gespeichert.");
                    return;
                }
                case "duration": {
                    if (args.length < 4 || !this.data.contains("events." + args[2].toLowerCase(Locale.ROOT))) {
                        break;
                    }
                    int minutes = args[3].matches("\\d{1,6}") ? Integer.parseInt(args[3]) : -1;
                    if (minutes < 1 || minutes > 1440) {
                        break;
                    }
                    this.data.set("events." + args[2].toLowerCase(Locale.ROOT) + ".duration", minutes);
                    this.save();
                    player.sendMessage(ChatColor.GREEN + "Dauer: " + minutes + " Minuten.");
                    return;
                }
                case "remove": {
                    if (args.length < 3 || !this.data.contains("events." + args[2].toLowerCase(Locale.ROOT))) {
                        break;
                    }
                    this.data.set("events." + args[2].toLowerCase(Locale.ROOT), null);
                    this.save();
                    player.sendMessage(ChatColor.GREEN + "Event gelöscht.");
                    return;
                }
                case "start": {
                    if (args.length < 3 || !this.data.contains("events." + args[2].toLowerCase(Locale.ROOT))) {
                        break;
                    }
                    String p = "events." + args[2].toLowerCase(Locale.ROOT);
                    this.data.set(p + ".start", System.currentTimeMillis());
                    this.data.set(p + ".started", false);
                    this.data.set(p + ".stopped", false);
                    this.save();
                    this.tick();
                    player.sendMessage(ChatColor.GREEN + "Event gestartet.");
                    return;
                }
                case "stop": {
                    List<Ev> running = this.running();
                    for (Ev ev : running) {
                        this.data.set("events." + ev.id() + ".stopped", true);
                    }
                    this.save();
                    player.sendMessage(ChatColor.GREEN + String.valueOf(running.size()) + " laufende(s) Event(s) beendet.");
                    return;
                }
                case "list": {
                    List<Ev> events = this.all();
                    if (events.isEmpty()) {
                        player.sendMessage(ChatColor.YELLOW + "Keine Events eingetragen.");
                        return;
                    }
                    for (Ev ev : events) {
                        player.sendMessage("§d" + ev.id() + " §8• §f" + ev.name() + " §7" + when(ev.start()) + " (" + ev.durationMin() + " Min)");
                    }
                    return;
                }
                default:
                    break;
            }
        }
        catch (RuntimeException ex) {
            player.sendMessage(ChatColor.RED + "Ungültige Angabe (Datum: heute|morgen|TT.MM.|TT.MM.JJJJ, Uhrzeit: HH:mm).");
            return;
        }
        player.sendMessage("§d§lEvents planen §7(mehrere können gleichzeitig laufen)");
        player.sendMessage("§d/duelladmin event template <Vorlage> [Datum] [HH:mm] §7(fertige Events, ohne Zeit = sofort) §8| §dtemplates §7(Liste)");
        player.sendMessage("§d/duelladmin event add <id> <Datum> <HH:mm> <Name...> §8z.B. add ffa1 heute 20:00 FFA-Abend");
        player.sendMessage("§d/duelladmin event quick <id> <Minuten> <Name...> §7(startet sofort)");
        player.sendMessage("§d/duelladmin event game <id> <ffa|turnier|none> [Kit] §7(öffnet beim Start automatisch die Anmeldung)");
        player.sendMessage("§d/duelladmin event spawn <id> §7(eigener Treffpunkt) §8| §dannounce <id> §7(sofort ankündigen)");
        player.sendMessage("§d/duelladmin event time <id> <Datum> <HH:mm> §7| §ddesc <id> <Text> §7| §dduration <id> <Minuten>");
        player.sendMessage("§d/duelladmin event remove <id> §7| §dlist §7| §dstart <id> §7| §dstop");
        player.sendMessage("§7Datum: heute, morgen, 24.12. oder 24.12.2026 – Zeit in Berliner Zeit.");
    }

    static List<String> ids() {
        List<String> out = new ArrayList<String>();
        if (instance != null) {
            for (Ev ev : instance.all()) {
                out.add(ev.id());
            }
        }
        return out;
    }
}
