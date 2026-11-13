package de.server.network;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drachenei wie bei Craft Attack: Platziert schwebt es als Schrein ueber dem Boden, darueber steht die Zahl der Spieler im Radius.
 * Nur wenn genau EIN Spieler im Radius ist und dort durchgehend wartet, bekommt er das Ei. Wer das Ei im Inventar traegt, leuchtet (Glowing).
 */
final class DragonEgg implements Listener {
    static DragonEgg instance;

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final Set<UUID> glowing = ConcurrentHashMap.newKeySet();
    private Location shrine;
    private Entity eggDisplay;
    private Entity textDisplay;
    private UUID capturer;
    private int progress;
    private float spin;
    private boolean full;

    DragonEgg(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.file = new File(plugin.getDataFolder(), "dragonegg.yml");
        YamlConfiguration data = YamlConfiguration.loadConfiguration(this.file);
        String world = data.getString("world", "");
        if (!world.isEmpty() && plugin.getServer().getWorld(world) != null) {
            this.shrine = new Location(plugin.getServer().getWorld(world), data.getDouble("x"), data.getDouble("y"), data.getDouble("z"));
        }
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("DragonEgg", () -> this.tick()), 40L, 20L);
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("DragonEgg-Spin", () -> this.spinTick()), 40L, 2L);
    }

    private double radius() {
        return Math.max(2.0d, this.plugin.getConfig().getDouble("dragonegg.radius", 20.0d));
    }

    private int captureSeconds() {
        return Math.max(5, this.plugin.getConfig().getInt("dragonegg.capture-seconds", 30));
    }

    private void saveShrine() {
        try {
            YamlConfiguration data = YamlConfiguration.loadConfiguration(this.file);
            if (this.shrine == null) {
                data.set("world", null);
                data.set("x", null);
                data.set("y", null);
                data.set("z", null);
            } else {
                data.set("world", this.shrine.getWorld().getName());
                data.set("x", this.shrine.getX());
                data.set("y", this.shrine.getY());
                data.set("z", this.shrine.getZ());
            }
            Store.save(data, this.file);
        }
        catch (Exception ignored) {
            // naechstes Mal
            Store.problem("DragonEgg:85", ignored);
        }
    }

    boolean hasShrine() {
        return this.shrine != null;
    }

    /** Stellt das Ei als Schrein an diese Stelle (Boden-Position, das Ei schwebt darueber). */
    void place(Location where) {
        this.removeEntities();
        this.shrine = where.clone();
        this.capturer = null;
        this.progress = 0;
        this.saveShrine();
        this.ensureEntities();
    }

    /** Beim Herunterfahren: Anzeigen entfernen (werden beim Start neu erzeugt), Schrein bleibt gespeichert. */
    void shutdown() {
        this.removeEntities();
    }

    void remove() {
        this.removeEntities();
        this.shrine = null;
        this.capturer = null;
        this.progress = 0;
        this.saveShrine();
    }

    private void removeEntities() {
        for (Entity e : new Entity[]{this.eggDisplay, this.textDisplay}) {
            try {
                if (e != null && e.isValid()) {
                    e.remove();
                }
            }
            catch (Throwable ignored) {
                // egal
                Store.problem("DragonEgg:124", ignored);
            }
        }
        this.eggDisplay = null;
        this.textDisplay = null;
    }

    /** Dropped-Ei (zuletzt gespawnt) - fuer die Locator-Bar. */
    static volatile Entity droppedEgg;

    /** Wo das Drachenei gerade liegt (Schrein oder herumliegendes Item), sonst null. */
    Location waypointSpot() {
        if (this.shrine != null && this.shrine.getWorld() != null) {
            return this.eggLocation();
        }
        Entity dropped = droppedEgg;
        if (dropped != null && dropped.isValid()) {
            return dropped.getLocation();
        }
        return null;
    }

    private Location eggLocation() {
        return this.shrine.clone().add(0.0d, 1.4d, 0.0d);
    }

    private boolean playersNear() {
        World w = this.shrine.getWorld();
        for (Player p : this.plugin.getServer().getOnlinePlayers()) {
            if (p.getWorld().equals(w) && p.getLocation().distanceSquared(this.shrine) <= 80.0d * 80.0d) {
                return true;
            }
        }
        return false;
    }

    private void ensureEntities() {
        if (this.shrine == null || this.shrine.getWorld() == null) {
            return;
        }
        try {
            boolean eggOk = this.eggDisplay != null && this.eggDisplay.isValid();
            boolean textOk = this.textDisplay != null && this.textDisplay.isValid();
            if (eggOk && textOk) {
                return;
            }
            if (!this.playersNear()) {
                return;
            }
            World world = this.shrine.getWorld();
            // Reste einer frueheren Sitzung entfernen (Schrein-Tag)
            for (Entity e : world.getNearbyEntities(this.shrine, 4.0d, 6.0d, 4.0d)) {
                if (e.getScoreboardTags().contains("lb_dragonegg") && e != this.eggDisplay && e != this.textDisplay) {
                    e.remove();
                }
            }
            if (!eggOk) {
                Entity egg = world.spawnEntity(this.eggLocation(), EntityType.ITEM_DISPLAY);
                if ((Object)egg instanceof org.bukkit.entity.ItemDisplay display) {
                    display.setItemStack(new ItemStack(Material.DRAGON_EGG));
                    display.setBillboard(org.bukkit.entity.Display.Billboard.FIXED);
                }
                egg.addScoreboardTag("lb_dragonegg");
                egg.setGlowing(true);
                this.eggDisplay = egg;
            }
            if (!textOk) {
                Entity text = world.spawnEntity(this.shrine.clone().add(0.0d, 2.5d, 0.0d), EntityType.TEXT_DISPLAY);
                if ((Object)text instanceof org.bukkit.entity.TextDisplay display) {
                    display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
                    display.setSeeThrough(true);
                }
                text.addScoreboardTag("lb_dragonegg");
                this.textDisplay = text;
            }
        }
        catch (Throwable t) {
            this.plugin.getLogger().warning("Drachenei-Anzeige: " + t);
        }
    }

    private void setText(String legacy) {
        try {
            if (this.textDisplay != null && this.textDisplay.isValid() && (Object)this.textDisplay instanceof org.bukkit.entity.TextDisplay display) {
                display.text(LegacyComponentSerializer.legacySection().deserialize(legacy));
            }
        }
        catch (Throwable ignored) {
            // nur Anzeige
            Store.problem("DragonEgg:197", ignored);
        }
    }

    private void spinTick() {
        try {
            if (this.shrine == null || this.eggDisplay == null || !this.eggDisplay.isValid()) {
                return;
            }
            this.spin = (this.spin + 8.0f) % 360.0f;
            Location at = this.eggLocation();
            at.setYaw(this.spin);
            at.setY(at.getY() + Math.sin(Math.toRadians(this.spin)) * 0.08d);
            this.eggDisplay.teleport(at);
        }
        catch (Throwable ignored) {
            // nur Optik
            Store.problem("DragonEgg:213", ignored);
        }
    }

    /* ---------- Sekundentakt ---------- */

    /** Wer den SMP-Bereich verlaesst (Lobby, Bau-SMP), laesst das Drachenei fallen - sonst waere es im gespeicherten SMP-Inventar "versteckt". */
    static void dropCarried(Player player) {
        try {
            int dropped = 0;
            ItemStack[] contents = player.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                ItemStack stack = contents[i];
                if (stack != null && stack.getType() == Material.DRAGON_EGG) {
                    player.getInventory().setItem(i, null);
                    player.getWorld().dropItemNaturally(player.getLocation(), stack);
                    dropped++;
                }
            }
            ItemStack off = player.getInventory().getItemInOffHand();
            if (off != null && off.getType() == Material.DRAGON_EGG) {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
                player.getWorld().dropItemNaturally(player.getLocation(), off);
                dropped++;
            }
            if (dropped > 0) {
                player.sendMessage("§5✦ §7Du hast das Drachenei fallen gelassen - es kann nicht in die Lobby oder ins Bau-SMP mitgenommen werden.");
            }
        }
        catch (Throwable t) {
            Store.problem("DragonEgg:dropCarried", t);
        }
    }

    static boolean carries(Player player) {
        try {
            for (ItemStack stack : player.getInventory().getContents()) {
                if (stack != null && stack.getType() == Material.DRAGON_EGG) {
                    return true;
                }
            }
            ItemStack off = player.getInventory().getItemInOffHand();
            return off != null && off.getType() == Material.DRAGON_EGG;
        }
        catch (Throwable t) {
            return false;
        }
    }

    private void tick() {
        try {
            this.tickCarriers();
            if (this.shrine == null || this.shrine.getWorld() == null) {
                return;
            }
            this.ensureEntities();
            double radius = this.radius();
            double r2 = radius * radius;
            Player only = null;
            int count = 0;
            for (Player p : this.plugin.getServer().getOnlinePlayers()) {
                GameMode mode = p.getGameMode();
                if (!p.getWorld().equals(this.shrine.getWorld()) || p.isDead() || (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE)
                        || !"smp".equals(this.plugin.profiles.getOrDefault(p.getUniqueId(), "lobby"))) {
                    continue;
                }
                if (p.getLocation().distanceSquared(this.eggLocation()) <= r2) {
                    count++;
                    only = p;
                }
            }
            int need = this.captureSeconds();
            String status;
            if (count == 0) {
                this.capturer = null;
                this.progress = 0;
                status = "§8Niemand in Reichweite";
            } else if (count >= 2) {
                this.capturer = null;
                this.progress = 0;
                status = "§c⚔ Kampf! §7Nur ein Spieler darf im Radius sein";
            } else if (this.plugin.isInSmpCombat(only)) {
                this.capturer = null;
                this.progress = 0;
                status = "§c⚔ " + only.getName() + " §7ist im Kampf – Timer pausiert";
                only.sendActionBar(Component.text("§cIm Kampf – das Drachenei kann jetzt nicht gesichert werden"));
            } else {
                if (!only.getUniqueId().equals(this.capturer)) {
                    this.capturer = only.getUniqueId();
                    this.progress = 0;
                    this.plugin.getServer().broadcastMessage("§5✦ §f" + only.getName() + " §7sichert das Drachenei! §8(" + need + " s ohne Kampf und ohne andere Spieler im Radius)");
                }
                this.progress++;
                int left = Math.max(0, need - this.progress);
                status = "§a" + only.getName() + " §7sichert das Ei: §e" + left + " s";
                only.sendActionBar(Component.text("§5Drachenei §8• §7noch §e" + left + " s §7im Radius bleiben"));
                if (this.progress >= need) {
                    this.award(only);
                    return;
                }
            }
            this.setText("§5§l✦ DRACHENEI ✦\n§7Im Radius: §f" + count + " Spieler\n" + status);
            this.ring(radius);
        }
        catch (Throwable t) {
            this.plugin.getLogger().warning("Drachenei: " + t);
        }
    }

    private void ring(double radius) {
        World world = this.shrine.getWorld();
        Location c = this.eggLocation();
        int points = (int)Math.min(120.0d, Math.max(24.0d, radius * 6.0d));
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2.0d * i / points;
            world.spawnParticle(Particle.END_ROD, c.getX() + Math.cos(a) * radius, this.shrine.getY() + 0.2d, c.getZ() + Math.sin(a) * radius, 1, 0.0d, 0.0d, 0.0d, 0.0d);
        }
        // vier Saeulen am Rand als "Rahmen"
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 2.0d * k;
            for (int h = 0; h < 6; h++) {
                world.spawnParticle(Particle.END_ROD, c.getX() + Math.cos(a) * radius, this.shrine.getY() + 0.3d + h * 0.5d, c.getZ() + Math.sin(a) * radius, 1, 0.0d, 0.0d, 0.0d, 0.0d);
            }
        }
    }

    private void award(Player winner) {
        ItemStack egg = new ItemStack(Material.DRAGON_EGG);
        if (Gui.give(winner, egg) > 0) {
            this.progress = Math.max(0, this.captureSeconds() - 1);
            winner.sendMessage("§cDein Inventar ist voll – mach einen Platz frei, um das Ei zu bekommen!");
            return;
        }
        this.removeEntities();
        this.shrine = null;
        this.capturer = null;
        this.progress = 0;
        this.saveShrine();
        this.plugin.getServer().broadcastMessage("§5§l✦ §d" + winner.getName() + " §7hat das Drachenei erobert! Er leuchtet für alle sichtbar – holt es euch!");
        for (Player p : this.plugin.getServer().getOnlinePlayers()) {
            Chat.sound(p, org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.6f);
        }
        winner.sendTitle("§5§lDRACHENEI", "§7Du trägst es jetzt – verteidige es!", 10, 60, 20);
    }

    private void tickCarriers() {
        for (Player p : this.plugin.getServer().getOnlinePlayers()) {
            UUID id = p.getUniqueId();
            boolean has = carries(p);
            if (has && this.glowing.add(id)) {
                p.setGlowing(true);
            } else if (!has && this.glowing.remove(id)) {
                p.setGlowing(false);
            }
            if (has) {
                p.sendActionBar(Component.text("§5✦ Du trägst das Drachenei §8– §7alle sehen dich leuchten"));
            }
        }
    }

    /* ---------- Ereignisse ---------- */

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent event) {
        try {
            if (event.getBlockPlaced().getType() != Material.DRAGON_EGG) {
                return;
            }
            Player player = event.getPlayer();
            boolean admin = player.isOp();
            event.setCancelled(true);
            if (!admin && !"smp".equals(this.plugin.profiles.getOrDefault(player.getUniqueId(), "lobby"))) {
                player.sendMessage("§cDas Drachenei kann nur im SMP platziert werden.");
                return;
            }
            if (this.plugin.inBauMode(player.getUniqueId())) {
                player.sendMessage("§cIm Bau-SMP kann das Drachenei nicht platziert werden.");
                return;
            }
            if (this.shrine != null) {
                player.sendMessage("§cEs gibt schon ein platziertes Drachenei.");
                return;
            }
            player.getInventory().removeItem(new ItemStack(Material.DRAGON_EGG, 1));
            Location at = event.getBlockPlaced().getLocation().add(0.5d, 0.0d, 0.5d);
            this.place(at);
            this.plugin.getServer().broadcastMessage("§5§l✦ §d" + player.getName() + " §7hat das Drachenei platziert bei §f" + at.getBlockX() + " " + at.getBlockY() + " " + at.getBlockZ()
                    + "§7! Nur ein einzelner Spieler im Radius kann es sich holen.");
        }
        catch (Throwable ignored) {
            // Standardverhalten
            Store.problem("DragonEgg:371", ignored);
        }
    }

    @EventHandler
    public void onItem(ItemSpawnEvent event) {
        try {
            if (event.getEntity().getItemStack().getType() == Material.DRAGON_EGG) {
                droppedEgg = event.getEntity();
                event.getEntity().setGlowing(true);
                event.getEntity().setInvulnerable(true);
                event.getEntity().setUnlimitedLifetime(true);
            }
        }
        catch (Throwable ignored) {
            // nur Komfort
            Store.problem("DragonEgg:385", ignored);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            Player player = event.getPlayer();
            this.glowing.remove(player.getUniqueId());
            // Mit dem Ei ausloggen ist erlaubt: es bleibt im Inventar, beim naechsten Beitritt leuchtet der Spieler wieder.
            if (carries(player)) {
                player.setGlowing(false);
            }
        }
        catch (Throwable ignored) {
            // egal
            Store.problem("DragonEgg:400", ignored);
        }
    }

    /** Das Ei bleibt am Koerper: nicht in Kisten, Ender-Kisten oder andere Behaelter legen. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player player) || !carries(player) && !isEgg(event.getCursor())) {
                return;
            }
            int top = event.getView().getTopInventory().getSize();
            String type = event.getView().getTopInventory().getType().name();
            if (type.equals("CRAFTING") || type.equals("PLAYER")) {
                return;
            }
            int raw = event.getRawSlot();
            boolean intoTop = raw >= 0 && raw < top;
            boolean block = false;
            if (intoTop && isEgg(event.getCursor())) {
                block = true;
            } else if (event.isShiftClick() && !intoTop && isEgg(event.getCurrentItem())) {
                block = true;
            } else if (intoTop && event.getClick().isKeyboardClick()) {
                int hot = event.getHotbarButton();
                if (hot >= 0 && isEgg(player.getInventory().getItem(hot))) {
                    block = true;
                }
                if (event.getClick().name().equals("SWAP_OFFHAND") && isEgg(player.getInventory().getItemInOffHand())) {
                    block = true;
                }
            }
            if (block) {
                event.setCancelled(true);
                player.sendMessage("§cDas Drachenei kannst du nicht in Behältern lagern.");
            }
        }
        catch (Throwable ignored) {
            // nur Komfort
            Store.problem("DragonEgg:438", ignored);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        try {
            if (!isEgg(event.getOldCursor())) {
                return;
            }
            int top = event.getView().getTopInventory().getSize();
            String type = event.getView().getTopInventory().getType().name();
            if (type.equals("CRAFTING") || type.equals("PLAYER")) {
                return;
            }
            for (int slot : event.getRawSlots()) {
                if (slot < top) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        catch (Throwable ignored) {
            // nur Komfort
            Store.problem("DragonEgg:461", ignored);
        }
    }

    private static boolean isEgg(ItemStack stack) {
        return stack != null && stack.getType() == Material.DRAGON_EGG;
    }

    /* ---------- Admin: /duelladmin egg ... ---------- */

    static void admin(DuelNetworkPlugin plugin, Player admin, String[] args) {
        DragonEgg egg = instance;
        if (egg == null) {
            return;
        }
        String sub = args.length >= 2 ? args[1].toLowerCase(java.util.Locale.ROOT) : "";
        switch (sub) {
            case "place" -> {
                egg.place(admin.getLocation().getBlock().getLocation().add(0.5d, 0.0d, 0.5d));
                admin.sendMessage("§aDrachenei-Schrein gesetzt.");
            }
            case "remove" -> {
                egg.remove();
                admin.sendMessage("§aDrachenei-Schrein entfernt.");
            }
            case "give" -> {
                Gui.give(admin, new ItemStack(Material.DRAGON_EGG));
                admin.sendMessage("§aDrachenei im Inventar.");
            }
            case "status" -> admin.sendMessage(egg.shrine == null ? "§7Kein Schrein aktiv." : "§dSchrein bei " + egg.shrine.getBlockX() + " " + egg.shrine.getBlockY() + " " + egg.shrine.getBlockZ()
                    + " §7• Sicherer: §f" + (egg.capturer == null ? "-" : plugin.getServer().getOfflinePlayer(egg.capturer).getName()) + " §7(" + egg.progress + "/" + egg.captureSeconds() + " s)");
            default -> admin.sendMessage("§d/duelladmin egg place §7(an deiner Position) | §dremove | give | status");
        }
    }
}
