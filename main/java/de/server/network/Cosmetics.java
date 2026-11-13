package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

/**
 * Gem-Shop-Kosmetik: Armor-Trims (werden im Duell automatisch auf die Kit-Ruestung gelegt), Kill-Effekte und
 * Trim-Materialien. Alles wird mit Gems bezahlt, Kosmetik gehoert dem Spieler dauerhaft.
 */
final class Cosmetics implements Listener, CommandExecutor {
    static Cosmetics instance;

    private static final String TAG = "Kosmetik";

    private record Offer(String key, String label, int price) { }

    private static final List<Offer> PATTERNS = List.of(
            new Offer("sentry", "Wächter", 80), new Offer("dune", "Düne", 80), new Offer("coast", "Küste", 80),
            new Offer("wild", "Wildnis", 80), new Offer("ward", "Hüter", 150), new Offer("eye", "Auge", 150),
            new Offer("vex", "Plagegeist", 150), new Offer("tide", "Gezeiten", 120), new Offer("snout", "Rüssel", 120),
            new Offer("rib", "Rippe", 120), new Offer("spire", "Turmspitze", 180), new Offer("wayfinder", "Wegfinder", 180),
            new Offer("shaper", "Former", 180), new Offer("silence", "Stille", 250),
            new Offer("raiser", "Plünderer", 180), new Offer("host", "Gastgeber", 180), new Offer("flow", "Strömung", 200),
            new Offer("bolt", "Blitzschlag", 200));
    private static final List<Offer> MATERIALS = List.of(
            new Offer("quartz", "Quarz", 25), new Offer("iron", "Eisen", 25), new Offer("copper", "Kupfer", 25),
            new Offer("gold", "Gold", 40), new Offer("redstone", "Redstone", 40), new Offer("lapis", "Lapis", 40),
            new Offer("amethyst", "Amethyst", 50), new Offer("emerald", "Smaragd", 60), new Offer("diamond", "Diamant", 80),
            new Offer("netherite", "Netherit", 100));
    private static final List<Offer> EFFECTS = List.of(
            new Offer("flames", "Flammen-Explosion", 80), new Offer("hearts", "Herzregen", 60), new Offer("souls", "Seelenwirbel", 100),
            new Offer("firework", "Feuerwerk", 150), new Offer("lightning", "Blitz (nur Optik)", 180),
            new Offer("totem", "Totem-Funken", 120), new Offer("notes", "Notenregen", 60),
            new Offer("snow", "Schneewirbel", 70), new Offer("magic", "Zauberfunken", 90), new Offer("sparks", "Funkenregen", 100),
            new Offer("cherry", "Kirschblüten", 80), new Offer("dragon", "Drachenhauch", 130), new Offer("electric", "Elektro-Funken", 110),
            new Offer("portal", "Portal-Wirbel", 100));
    /** Projektil-Spuren (Pfeile, Enderperlen) im Duell. */
    private static final List<Offer> TRAILS = List.of(
            new Offer("dust", "Lila Funken", 50), new Offer("end_rod", "Sternenstaub", 80), new Offer("flame", "Flammenspur", 70),
            new Offer("heart", "Herzchen", 60), new Offer("soul", "Seelenspur", 100), new Offer("note", "Noten", 60),
            new Offer("totem", "Totem-Glitzer", 120),
            new Offer("snow", "Schneeflocken", 70), new Offer("magic", "Zauberspur", 90), new Offer("cherry", "Kirschblüten", 80),
            new Offer("electric", "Elektro-Funken", 110), new Offer("portal", "Portal-Spur", 100), new Offer("lava", "Lava-Tropfen", 90),
            new Offer("dragon", "Drachenhauch", 130));
    private static final int[] PATTERN_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31};
    private static final int[] MATERIAL_SLOTS = {32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    /** Raster fuer Kill-Effekte und Projektil-Spuren (14 Plaetze); Zurueck/Aus/Champion liegen in der unteren Reihe. */
    private static final int[] GRID_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<UUID, Long> effectCooldown = new HashMap<UUID, Long>();

    Cosmetics(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.file = new File(plugin.getDataFolder(), "cosmetics.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("cosmetics.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    /* ---------- Daten ---------- */

    private boolean owns(UUID id, String key) {
        return this.data.getStringList("players." + id + ".owned").contains(key);
    }

    private void grant(UUID id, String key) {
        List<String> owned = new ArrayList<String>(this.data.getStringList("players." + id + ".owned"));
        if (!owned.contains(key)) {
            owned.add(key);
            this.data.set("players." + id + ".owned", owned);
        }
    }

    private String selected(UUID id, String slot) {
        return this.data.getString("players." + id + "." + slot, "");
    }

    private boolean trimOn(UUID id) {
        return this.data.getBoolean("players." + id + ".trim-on", true);
    }

    private boolean pay(Player player, int price) {
        UUID id = player.getUniqueId();
        if (Gems.balance(id) < price) {
            player.sendMessage("§cDir fehlen " + (price - Gems.balance(id)) + " Gems.");
            Chat.no(player);
            return false;
        }
        Gems.give(id, -price);
        if (Gems.instance != null) {
            Gems.instance.save();
        }
        return true;
    }

    private static Offer find(List<Offer> list, String key) {
        for (Offer offer : list) {
            if (offer.key().equals(key)) {
                return offer;
            }
        }
        return null;
    }

    /* ---------- Duell: Trims auf die Ruestung ---------- */

    /** Wird von applyDuelKit nach dem Anlegen der Ruestung aufgerufen. Nur Optik, Fehler werden verschluckt. */
    static void decorate(Player player) {
        if (instance == null || player == null) {
            return;
        }
        try {
            instance.applyTrim(player);
        }
        catch (Throwable ignored) {
            Store.problem("Cosmetics:decorate", ignored);
        }
    }

    private void applyTrim(Player player) {
        UUID id = player.getUniqueId();
        String pattern = this.selected(id, "trim-pattern");
        String material = this.selected(id, "trim-material");
        if (!this.trimOn(id) || pattern.isEmpty() || material.isEmpty()) {
            return;
        }
        ArmorTrim trim = trimOf(pattern, material);
        if (trim == null) {
            return;
        }
        ItemStack[] armor = player.getInventory().getArmorContents();
        boolean changed = false;
        for (int i = 0; i < armor.length; i++) {
            ItemStack piece = armor[i];
            if (piece == null || !(piece.getItemMeta() instanceof ArmorMeta meta) || meta.getTrim() != null) {
                continue;
            }
            meta.setTrim(trim);
            piece.setItemMeta(meta);
            armor[i] = piece;
            changed = true;
        }
        if (changed) {
            player.getInventory().setArmorContents(armor);
        }
    }

    private static ArmorTrim trimOf(String pattern, String material) {
        TrimPattern p = Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(pattern));
        TrimMaterial m = Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft(material));
        return p == null || m == null ? null : new ArmorTrim(m, p);
    }

    /* ---------- Duell: Kill-Effekt ---------- */

    /** Wird vom Duell-Ergebnis aufgerufen (Gewinner, Verlierer). */
    static void killEffect(Player winner, Player loser) {
        if (instance == null || winner == null || loser == null) {
            return;
        }
        try {
            instance.playEffect(winner, loser.getLocation());
        }
        catch (Throwable ignored) {
            Store.problem("Cosmetics:killEffect", ignored);
        }
    }

    private void playEffect(Player winner, Location at) {
        String effect = this.selected(winner.getUniqueId(), "effect");
        if (!this.plugin.getConfig().getBoolean("performance.particles", true) || effect.isEmpty() || at == null || at.getWorld() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - this.effectCooldown.getOrDefault(winner.getUniqueId(), 0L) < 4000L) {
            return;
        }
        this.effectCooldown.put(winner.getUniqueId(), now);
        Location loc = at.clone().add(0.0, 1.0, 0.0);
        switch (effect) {
            case "flames" -> {
                loc.getWorld().spawnParticle(Particle.FLAME, loc, 60, 0.4, 0.6, 0.4, 0.08);
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.4f, 1.6f);
            }
            case "hearts" -> loc.getWorld().spawnParticle(Particle.HEART, loc, 18, 0.5, 0.7, 0.5, 0.0);
            case "souls" -> {
                loc.getWorld().spawnParticle(Particle.SOUL, loc, 40, 0.4, 0.8, 0.4, 0.05);
                loc.getWorld().playSound(loc, Sound.PARTICLE_SOUL_ESCAPE, 1.0f, 1.0f);
            }
            case "firework" -> {
                Firework fw = loc.getWorld().spawn(loc, Firework.class);
                org.bukkit.inventory.meta.FireworkMeta meta = fw.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(Color.PURPLE, Color.FUCHSIA).withFade(Color.WHITE).build());
                fw.setFireworkMeta(meta);
                fw.detonate();
            }
            case "lightning" -> loc.getWorld().strikeLightningEffect(loc);
            case "totem" -> {
                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 50, 0.4, 0.7, 0.4, 0.3);
                loc.getWorld().playSound(loc, Sound.ITEM_TOTEM_USE, 0.5f, 1.4f);
            }
            case "notes" -> {
                loc.getWorld().spawnParticle(Particle.NOTE, loc, 16, 0.6, 0.8, 0.6, 1.0);
                loc.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
            }
            case "snow" -> {
                loc.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 60, 0.6, 0.8, 0.6, 0.02);
                loc.getWorld().playSound(loc, Sound.BLOCK_POWDER_SNOW_BREAK, 1.0f, 1.0f);
            }
            case "magic" -> {
                loc.getWorld().spawnParticle(Particle.ENCHANT, loc, 80, 0.6, 0.9, 0.6, 0.8);
                loc.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.2f);
            }
            case "sparks" -> {
                loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 50, 0.4, 0.8, 0.4, 0.15);
                loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f);
            }
            case "cherry" -> loc.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc, 50, 0.7, 0.9, 0.7, 0.0);
            case "dragon" -> {
                loc.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 50, 0.4, 0.6, 0.4, 0.03);
                loc.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.4f);
            }
            case "electric" -> {
                loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 60, 0.5, 0.8, 0.5, 0.2);
                loc.getWorld().playSound(loc, Sound.BLOCK_LODESTONE_PLACE, 0.8f, 1.6f);
            }
            case "portal" -> {
                loc.getWorld().spawnParticle(Particle.PORTAL, loc, 80, 0.5, 0.9, 0.5, 0.8);
                loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.0f);
            }
            case "champion" -> {
                loc.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 80, 0.5, 0.9, 0.5, 0.35);
                loc.getWorld().spawnParticle(Particle.END_ROD, loc, 30, 0.4, 1.0, 0.4, 0.05);
                loc.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
            }
            default -> { }
        }
    }

    /** Exklusive Belohnung (z. B. Season-Sieger): schaltet die Champion-Aura frei. */
    static void grantChampion(UUID id) {
        if (instance != null) {
            instance.grant(id, "effect:champion");
            instance.save();
        }
    }

    /* ---------- Menues ---------- */

    void open(Player player) {
        Inventory inv = Gui.frame(this.plugin, TAG, "Gem-Kosmetik", 27);
        inv.setItem(4, Gui.item(Material.AMETHYST_SHARD, "§b§lDeine Gems: §f" + Gems.balance(player.getUniqueId())));
        inv.setItem(10, Gui.item(Material.NETHERITE_CHESTPLATE, "§d§lArmor-Trim", "§7Kaufe Muster und Trim-Materialien.", "§7Dein Trim liegt im Duell automatisch", "§7auf deiner Kit-Rüstung.", "", "§e▶ Klicken"));
        inv.setItem(12, Gui.item(Material.FIREWORK_ROCKET, "§d§lKill-Effekte", "§7Ein Effekt am Ort deines Siegs.", "", "§e▶ Klicken"));
        inv.setItem(14, Gui.item(Material.ARROW, "§d§lProjektil-Spuren", "§7Partikel hinter Pfeilen und Enderperlen", "§7(nur in Duellen).", "", "§e▶ Klicken"));
        inv.setItem(22, Gui.item(Material.ARROW, "§7« Zurück"));
        player.openInventory(inv);
    }

    private ItemStack patternIcon(Offer offer) {
        Material icon = Material.matchMaterial(offer.key().toUpperCase(Locale.ROOT) + "_ARMOR_TRIM_SMITHING_TEMPLATE");
        return new ItemStack(icon == null ? Material.PAPER : icon);
    }

    private ItemStack materialIcon(Offer offer) {
        Material icon = switch (offer.key()) {
            case "quartz" -> Material.QUARTZ;
            case "iron" -> Material.IRON_INGOT;
            case "copper" -> Material.COPPER_INGOT;
            case "gold" -> Material.GOLD_INGOT;
            case "redstone" -> Material.REDSTONE;
            case "lapis" -> Material.LAPIS_LAZULI;
            case "amethyst" -> Material.AMETHYST_SHARD;
            case "emerald" -> Material.EMERALD;
            case "diamond" -> Material.DIAMOND;
            default -> Material.NETHERITE_INGOT;
        };
        return new ItemStack(icon);
    }

    private ItemStack offerItem(UUID id, ItemStack icon, Offer offer, String slot, String keyPrefix) {
        boolean owned = this.owns(id, keyPrefix + offer.key());
        boolean chosen = this.selected(id, slot).equals(offer.key());
        List<String> lore = new ArrayList<String>();
        if (chosen) {
            lore.add("§a✔ Ausgewählt");
        } else if (owned) {
            lore.add("§7Im Besitz");
            lore.add("§e▶ Klicken zum Auswählen");
        } else {
            lore.add("§7Preis: §b" + offer.price() + " Gems");
            lore.add("§e▶ Klicken zum Kaufen");
        }
        ItemStack stack = Gui.item(icon.getType(), (chosen ? "§a§l" : "§d") + offer.label(), lore.toArray(new String[0]));
        if (chosen) {
            stack.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
            org.bukkit.inventory.meta.ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                stack.setItemMeta(meta);
            }
        }
        return stack;
    }

    private void openTrims(Player player) {
        UUID id = player.getUniqueId();
        Inventory inv = Gui.frame(this.plugin, TAG, "Armor-Trim", 54);
        ItemStack preview = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ArmorTrim trim = trimOf(this.selected(id, "trim-pattern"), this.selected(id, "trim-material"));
        if (trim != null && preview.getItemMeta() instanceof ArmorMeta meta) {
            meta.setTrim(trim);
            meta.setDisplayName("§d§lVorschau");
            preview.setItemMeta(meta);
        } else {
            org.bukkit.inventory.meta.ItemMeta meta = preview.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§d§lVorschau");
                meta.setLore(List.of("§7Wähle ein Muster und ein Material."));
                preview.setItemMeta(meta);
            }
        }
        inv.setItem(4, preview);
        for (int i = 0; i < PATTERNS.size() && i < PATTERN_SLOTS.length; i++) {
            inv.setItem(PATTERN_SLOTS[i], this.offerItem(id, this.patternIcon(PATTERNS.get(i)), PATTERNS.get(i), "trim-pattern", "pattern:"));
        }
        for (int i = 0; i < MATERIALS.size() && i < MATERIAL_SLOTS.length; i++) {
            inv.setItem(MATERIAL_SLOTS[i], this.offerItem(id, this.materialIcon(MATERIALS.get(i)), MATERIALS.get(i), "trim-material", "material:"));
        }
        inv.setItem(8, Gui.item(Material.AMETHYST_SHARD, "§b§lGems: §f" + Gems.balance(id)));
        inv.setItem(49, Gui.item(this.trimOn(id) ? Material.LIME_DYE : Material.GRAY_DYE, this.trimOn(id) ? "§aTrim im Duell: an" : "§7Trim im Duell: aus", "§e▶ Klicken zum Umschalten"));
        inv.setItem(45, Gui.item(Material.ARROW, "§7« Zurück"));
        player.openInventory(inv);
    }

    private void openEffects(Player player) {
        UUID id = player.getUniqueId();
        Inventory inv = Gui.frame(this.plugin, TAG, "Kill-Effekte", 36);
        inv.setItem(4, Gui.item(Material.AMETHYST_SHARD, "§b§lGems: §f" + Gems.balance(id)));
        int[] slots = GRID_SLOTS;
        for (int i = 0; i < EFFECTS.size() && i < slots.length; i++) {
            Offer offer = EFFECTS.get(i);
            Material icon = switch (offer.key()) {
                case "flames" -> Material.BLAZE_POWDER;
                case "hearts" -> Material.PINK_DYE;
                case "souls" -> Material.SOUL_LANTERN;
                case "firework" -> Material.FIREWORK_STAR;
                case "totem" -> Material.TOTEM_OF_UNDYING;
                case "notes" -> Material.NOTE_BLOCK;
                case "snow" -> Material.SNOWBALL;
                case "magic" -> Material.ENCHANTED_BOOK;
                case "sparks" -> Material.FIREWORK_ROCKET;
                case "cherry" -> Material.PINK_PETALS;
                case "dragon" -> Material.DRAGON_BREATH;
                case "portal" -> Material.OBSIDIAN;
                default -> Material.LIGHTNING_ROD;
            };
            inv.setItem(slots[i], this.offerItem(id, new ItemStack(icon), offer, "effect", "effect:"));
        }
        if (this.owns(id, "effect:champion")) {
            ItemStack champ = Gui.item(Material.NETHER_STAR, this.selected(id, "effect").equals("champion") ? "§a§lChampion-Aura §7(Season-Sieger)" : "§6§lChampion-Aura §7(Season-Sieger)", "§e▶ Klicken zum Auswählen");
            inv.setItem(29, champ);
        }
        inv.setItem(33, Gui.item(Material.BARRIER, "§7Kein Effekt", "§e▶ Klicken zum Ausschalten"));
        inv.setItem(31, Gui.item(Material.ARROW, "§7« Zurück"));
        player.openInventory(inv);
    }

    /** Spieler, die im Spuren-Menue gerade die Enderperlen-Spur bearbeiten (sonst: Pfeil-Spur). */
    private final java.util.Set<UUID> pearlTarget = new java.util.HashSet<UUID>();

    private String trailSlot(UUID id) {
        return this.pearlTarget.contains(id) ? "trail-pearl" : "trail";
    }

    private void openTrails(Player player) {
        UUID id = player.getUniqueId();
        String slotKey = this.trailSlot(id);
        boolean pearl = slotKey.equals("trail-pearl");
        Inventory inv = Gui.frame(this.plugin, TAG, "Projektil-Spuren", 36);
        inv.setItem(4, Gui.item(Material.AMETHYST_SHARD, "§b§lGems: §f" + Gems.balance(id)));
        Material[] icons = {Material.AMETHYST_SHARD, Material.END_ROD, Material.BLAZE_POWDER, Material.PINK_DYE, Material.SOUL_LANTERN, Material.NOTE_BLOCK, Material.TOTEM_OF_UNDYING,
            Material.SNOWBALL, Material.ENCHANTED_BOOK, Material.PINK_PETALS, Material.LIGHTNING_ROD, Material.OBSIDIAN, Material.LAVA_BUCKET, Material.DRAGON_BREATH};
        int[] slots = GRID_SLOTS;
        for (int i = 0; i < TRAILS.size() && i < slots.length; i++) {
            inv.setItem(slots[i], this.offerItem(id, new ItemStack(icons[i]), TRAILS.get(i), slotKey, "trail:"));
        }
        inv.setItem(30, Gui.item(pearl ? Material.ENDER_PEARL : Material.ARROW, "§dSpur für: §f" + (pearl ? "Enderperlen" : "Pfeile"),
                "§7Pfeile und Enderperlen können", "§7verschiedene Spuren haben.", "§7Gekaufte Spuren gelten für beide.", "", "§e▶ Klicken zum Wechseln"));
        inv.setItem(33, Gui.item(Material.BARRIER, "§7Keine Spur (" + (pearl ? "Enderperlen" : "Pfeile") + ")", "§e▶ Klicken zum Ausschalten"));
        inv.setItem(31, Gui.item(Material.ARROW, "§7« Zurück"));
        player.openInventory(inv);
    }

    /** /cosmetics (/kosmetik): Kosmetik ansehen und kaufen, ueberall. */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            this.open(player);
        } else {
            sender.sendMessage("Nur im Spiel.");
        }
        return true;
    }

    /* ---------- Projektil-Spuren ---------- */

    private final Map<Projectile, String> tracked = new java.util.LinkedHashMap<Projectile, String>();
    private final Map<Projectile, Integer> trackedAge = new HashMap<Projectile, Integer>();
    private BukkitTask trailTask;

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile.getShooter() instanceof Player shooter) || !(projectile instanceof AbstractArrow || projectile instanceof EnderPearl)) {
            return;
        }
        String trailKey = projectile instanceof EnderPearl ? "trail-pearl" : "trail";
        // Enderperle ohne eigene Auswahl: wie bisher die Pfeil-Spur verwenden
        String trail = this.data.contains("players." + shooter.getUniqueId() + "." + trailKey) ? this.selected(shooter.getUniqueId(), trailKey) : this.selected(shooter.getUniqueId(), "trail");
        if (trail.isEmpty() || !this.plugin.inActiveMatch(shooter.getUniqueId()) || this.tracked.size() >= 40) {
            return;
        }
        this.tracked.put(projectile, trail);
        this.trackedAge.put(projectile, 0);
        if (this.trailTask == null) {
            this.trailTask = this.plugin.getServer().getScheduler().runTaskTimer((org.bukkit.plugin.Plugin)this.plugin, Perf.wrap("Spuren", this::tickTrails), 1L, 1L);
        }
    }

    private final Map<Projectile, Location> lastPos = new HashMap<Projectile, Location>();

    private void emit(org.bukkit.World world, Location at, String trail) {
        switch (trail) {
            case "dust" -> world.spawnParticle(Particle.DUST, at, 3, 0.05, 0.05, 0.05, 0.0, new Particle.DustOptions(Color.fromRGB(170, 80, 255), 1.1f));
            case "end_rod" -> world.spawnParticle(Particle.END_ROD, at, 2, 0.03, 0.03, 0.03, 0.0);
            case "flame" -> world.spawnParticle(Particle.FLAME, at, 2, 0.03, 0.03, 0.03, 0.0);
            case "heart" -> world.spawnParticle(Particle.HEART, at, 1, 0.05, 0.05, 0.05, 0.0);
            case "soul" -> world.spawnParticle(Particle.SOUL, at, 1, 0.03, 0.03, 0.03, 0.0);
            case "note" -> world.spawnParticle(Particle.NOTE, at, 1, 0.05, 0.05, 0.05, 1.0);
            case "totem" -> world.spawnParticle(Particle.TOTEM_OF_UNDYING, at, 2, 0.05, 0.05, 0.05, 0.1);
            case "snow" -> world.spawnParticle(Particle.SNOWFLAKE, at, 2, 0.05, 0.05, 0.05, 0.0);
            case "magic" -> world.spawnParticle(Particle.ENCHANT, at, 3, 0.1, 0.1, 0.1, 0.3);
            case "cherry" -> world.spawnParticle(Particle.CHERRY_LEAVES, at, 1, 0.08, 0.08, 0.08, 0.0);
            case "electric" -> world.spawnParticle(Particle.ELECTRIC_SPARK, at, 2, 0.05, 0.05, 0.05, 0.0);
            case "portal" -> world.spawnParticle(Particle.PORTAL, at, 3, 0.05, 0.05, 0.05, 0.3);
            case "lava" -> world.spawnParticle(Particle.DRIPPING_LAVA, at, 1, 0.05, 0.05, 0.05, 0.0);
            case "dragon" -> world.spawnParticle(Particle.DRAGON_BREATH, at, 1, 0.05, 0.05, 0.05, 0.0);
            default -> { }
        }
    }

    private void tickTrails() {
        java.util.Iterator<Map.Entry<Projectile, String>> it = this.tracked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Projectile, String> entry = it.next();
            Projectile projectile = entry.getKey();
            int age = this.trackedAge.merge(projectile, 1, Integer::sum);
            boolean stuck = projectile instanceof AbstractArrow arrow && arrow.isInBlock();
            if (!projectile.isValid() || projectile.isDead() || stuck || age > 200) {
                it.remove();
                this.trackedAge.remove(projectile);
                this.lastPos.remove(projectile);
                continue;
            }
            Location at = projectile.getLocation();
            org.bukkit.World world = at.getWorld();
            if (world == null) {
                continue;
            }
            Location previous = this.lastPos.put(projectile, at.clone());
            if (previous != null && previous.getWorld() == world) {
                double gap = previous.distance(at);
                if (gap > 0.8 && gap < 12.0) {
                    // schnelle Projektile (Enderperlen): Luecken zwischen zwei Ticks mit Partikeln fuellen
                    int extra = (int)Math.min(6.0, Math.floor(gap / 0.6));
                    for (int k = 1; k <= extra; k++) {
                        double t = (double)k / (extra + 1);
                        this.emit(world, previous.clone().add(at.clone().subtract(previous).multiply(t)), entry.getValue());
                    }
                }
            }
            this.emit(world, at, entry.getValue());
        }
        if (this.tracked.isEmpty() && this.trailTask != null) {
            this.trailTask.cancel();
            this.trailTask = null;
        }
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
        int slot = event.getRawSlot();
        UUID id = player.getUniqueId();
        boolean main = title.contains("Gem-Kosmetik");
        boolean trims = title.contains("Armor-Trim");
        boolean effects = title.contains("Kill-Effekte");
        boolean trails = title.contains("Projektil-Spuren");
        if (main) {
            switch (slot) {
                case 10 -> this.openTrims(player);
                case 12 -> this.openEffects(player);
                case 14 -> this.openTrails(player);
                case 22 -> {
                    if (Gems.instance != null) {
                        Gems.instance.open(player);
                    }
                }
                default -> { }
            }
            return;
        }
        if ((trims && slot == 45) || ((effects || trails) && slot == 31)) {
            this.open(player);
            return;
        }
        if (trims) {
            if (slot == 49) {
                this.data.set("players." + id + ".trim-on", !this.trimOn(id));
                this.save();
                this.openTrims(player);
                return;
            }
            for (int i = 0; i < PATTERN_SLOTS.length && i < PATTERNS.size(); i++) {
                if (PATTERN_SLOTS[i] == slot) {
                    this.choose(player, PATTERNS.get(i), "pattern:", "trim-pattern");
                    this.openTrims(player);
                    return;
                }
            }
            for (int i = 0; i < MATERIAL_SLOTS.length && i < MATERIALS.size(); i++) {
                if (MATERIAL_SLOTS[i] == slot) {
                    this.choose(player, MATERIALS.get(i), "material:", "trim-material");
                    this.openTrims(player);
                    return;
                }
            }
            return;
        }
        if (trails) {
            if (slot == 30) {
                if (!this.pearlTarget.remove(id)) {
                    this.pearlTarget.add(id);
                }
                this.openTrails(player);
                return;
            }
            if (slot == 33) {
                this.data.set("players." + id + "." + this.trailSlot(id), "");
                this.save();
                this.openTrails(player);
                return;
            }
            int[] tslots = GRID_SLOTS;
            for (int i = 0; i < TRAILS.size() && i < tslots.length; i++) {
                if (tslots[i] == slot) {
                    this.choose(player, TRAILS.get(i), "trail:", this.trailSlot(id));
                    this.openTrails(player);
                    return;
                }
            }
            return;
        }
        if (effects) {
            if (slot == 29 && this.owns(id, "effect:champion")) {
                this.data.set("players." + id + ".effect", "champion");
                this.save();
                this.openEffects(player);
                return;
            }
            if (slot == 33) {
                this.data.set("players." + id + ".effect", "");
                this.save();
                this.openEffects(player);
                return;
            }
            int[] slots = GRID_SLOTS;
            for (int i = 0; i < EFFECTS.size() && i < slots.length; i++) {
                if (slots[i] == slot) {
                    this.choose(player, EFFECTS.get(i), "effect:", "effect");
                    this.openEffects(player);
                    return;
                }
            }
        }
    }

    /** Kaufen (falls noch nicht im Besitz) und auswaehlen. */
    private void choose(Player player, Offer offer, String keyPrefix, String slot) {
        UUID id = player.getUniqueId();
        if (!this.owns(id, keyPrefix + offer.key())) {
            if (!this.pay(player, offer.price())) {
                return;
            }
            this.grant(id, keyPrefix + offer.key());
            player.sendMessage("§aGekauft: §f" + offer.label() + " §7für §b" + offer.price() + " Gems§7.");
            Chat.yes(player);
        }
        this.data.set("players." + id + "." + slot, offer.key());
        this.save();
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (Gui.isTitle(title, TAG)) {
            event.setCancelled(true);
        }
    }
}
