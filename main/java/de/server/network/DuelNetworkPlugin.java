/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.BanList$Type
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.GameMode
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Server
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.WorldBorder
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.boss.BarColor
 *  org.bukkit.boss.BarFlag
 *  org.bukkit.boss.BarStyle
 *  org.bukkit.boss.BossBar
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Projectile
 *  org.bukkit.entity.Villager
 *  org.bukkit.entity.Villager$Profession
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.event.block.BlockFromToEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.CreatureSpawnEvent
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.PlayerDeathEvent
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerDropItemEvent
 *  org.bukkit.event.player.PlayerInteractEntityEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerKickEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.PotionMeta
 *  org.bukkit.permissions.PermissionAttachment
 *  org.bukkit.persistence.PersistentDataType
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.projectiles.ProjectileSource
 *  org.bukkit.scoreboard.Scoreboard
 *  org.bukkit.scoreboard.Team
 */
package de.server.network;

import de.server.network.DuelFixes;
import de.server.network.ParkourEnhancements;
import de.server.network.ProtectionListener;
import java.lang.invoke.CallSite;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class DuelNetworkPlugin
extends JavaPlugin
implements Listener,
CommandExecutor,
TabCompleter {
    private final Map<UUID, UUID> duelInvites = new ConcurrentHashMap<UUID, UUID>();
    /** Serien (Best of N): Auswahl des Herausforderers/Leaders (1, 3, 5, 7), wer bestimmt, Vorgabe fuer den naechsten Start, letzte Serie (Revanche), laufende Serien. */
    private final Map<UUID, Integer> duelBestOf = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, UUID> duelHosts = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, Integer> pendingBestOf = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> lastBestOf = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Series> duelSeries = new ConcurrentHashMap<UUID, Series>();

    private static final class Series {
        final UUID a;
        final UUID b;
        final int best;
        final int needed;
        final String arena;
        final String kit;
        final boolean party;
        int winsA;
        int winsB;

        Series(UUID a, UUID b, int best, String arena, String kit, boolean party) {
            this.a = a;
            this.b = b;
            this.best = best;
            this.needed = best / 2 + 1;
            this.arena = arena;
            this.kit = kit;
            this.party = party;
        }
    }
    private final Map<UUID, UUID> outgoingDuelInvites = new ConcurrentHashMap<UUID, UUID>();
    final Map<UUID, UUID> duelPartners = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, String> selectedDuelKits = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, String> duelArenas = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, Double> duelDamage = new ConcurrentHashMap<UUID, Double>();
    private final Map<UUID, Integer> duelTimeoutTasks = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Location> pendingDuelRespawns = new ConcurrentHashMap<UUID, Location>();
    final Map<String, List<UUID>> duelQueues = new ConcurrentHashMap<String, List<UUID>>();
    private final Map<UUID, String> kitEditors = new ConcurrentHashMap<UUID, String>();
    final Map<UUID, String> duelModes = new ConcurrentHashMap<UUID, String>();
    private final Map<String, UUID> partyLeaders = new ConcurrentHashMap<String, UUID>();
    private final Map<UUID, String> playerParties = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, UUID> tpaRequests = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, UUID> outgoingTpaRequests = new ConcurrentHashMap<UUID, UUID>();
    /** /tpahere: Ziel (der teleportiert wird) -> Anfragender (zu dem teleportiert wird). */
    private final Map<UUID, UUID> tpahereRequests = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, Integer> tpaExpiryTasks = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> pendingHomeSlots = new ConcurrentHashMap<UUID, Integer>();
    final Map<UUID, String> activeParkourMaps = new ConcurrentHashMap<UUID, String>();
    final Map<UUID, Location> parkourCheckpoints = new ConcurrentHashMap<UUID, Location>();
    private final Map<UUID, Long> parkourStartTimes = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Integer> parkourTimerTasks = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Long> parkourFallCooldowns = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Integer> delayedTeleportTasks = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Long> smpCombatUntil = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Integer> smpCombatTasks = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> combatXpLevels = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Float> combatXpProgress = new ConcurrentHashMap<UUID, Float>();
    private final Map<UUID, BossBar> duelBossBars = new ConcurrentHashMap<UUID, BossBar>();
    private final Map<UUID, Integer> duelHudTasks = new ConcurrentHashMap<UUID, Integer>();
    final Map<UUID, String> activePartyMatches = new ConcurrentHashMap<UUID, String>();
    final Map<String, Set<UUID>> partyMatchPlayers = new ConcurrentHashMap<String, Set<UUID>>();
    private final Map<String, Boolean> partyMatchFfa = new ConcurrentHashMap<String, Boolean>();
    /** Team-Duelle: Match -> (Spieler -> Team 0/1). */
    private final Map<String, Map<UUID, Integer>> partyMatchTeams = new ConcurrentHashMap<String, Map<UUID, Integer>>();
    private final Map<String, String> partyMatchKits = new ConcurrentHashMap<String, String>();
    private final Map<String, String> partyMatchArenas = new ConcurrentHashMap<String, String>();
    private final Map<String, Long> partyWarmupUntil = new ConcurrentHashMap<String, Long>();
    private final Map<String, Integer> partyMatchTasks = new ConcurrentHashMap<String, Integer>();
    private final Map<UUID, String> partyPendingModes = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, UUID> partyPendingOpponents = new ConcurrentHashMap<UUID, UUID>();
    private final Map<String, Set<UUID>> partyMembers = new ConcurrentHashMap<String, Set<UUID>>();
    private final Map<UUID, String> partyInvites = new ConcurrentHashMap<UUID, String>();
    final Map<UUID, String> profiles = new ConcurrentHashMap<UUID, String>();
    BackupManager backups;
    volatile java.util.List<String> configProblems = new java.util.ArrayList<String>();
    Mutes mutes;
    Warps warps;
    boolean vaultActive;
    boolean papiActive;
    Moderation moderation;
    ClaimManager claimManager;

    private boolean configSavePending;

    /**
     * Gebuendeltes, asynchrones Speichern der config.yml: viele Aenderungen kurz hintereinander
     * ergeben nur noch einen Schreibvorgang (im Haupt-Thread wird nur serialisiert).
     */
    @Override
    public void saveConfig() {
        if (!this.isEnabled()) {
            this.flushConfig();
            return;
        }
        if (this.configSavePending) {
            return;
        }
        this.configSavePending = true;
        this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
            this.configSavePending = false;
            this.flushConfig();
        }, 20L);
    }

    void flushConfig() {
        this.configSavePending = false;
        try {
            Store.save(this.getConfig(), new java.io.File(this.getDataFolder(), "config.yml"));
        }
        catch (java.io.IOException e) {
            this.getLogger().warning("config.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        this.flushConfig();
        if (this.packHost != null) {
            this.packHost.stop();
        }
        if (Comfort.instance != null) {
            Comfort.instance.save();
        }
        if (Boards.instance != null) {
            Boards.instance.shutdown();
        }
        if (Trade.instance != null) {
            Trade.instance.shutdown();
        }
        if (Ghosts.instance != null) {
            Ghosts.instance.shutdown();
        }
        if (Watch.instance != null) {
            Watch.instance.shutdown();
        }
        if (SmpTop.instance != null) {
            SmpTop.instance.save();
        }
        if (Market.instance != null) {
            Market.instance.save();
        }
        if (Economy.instance != null) {
            Economy.instance.shutdown();
        }
        if (this.claimManager != null) {
            this.claimManager.close();
        }
        if (Gems.instance != null) {
            Gems.instance.save();
        }
        if (EventGames.instance != null) {
            EventGames.instance.shutdown();
        }
        if (DragonEgg.instance != null) {
            DragonEgg.instance.shutdown();
        }
        if (this.locator != null) {
            this.locator.shutdown();
        }
        // wartet, bis alle ausstehenden Schreibvorgaenge auf der Platte sind
        Store.shutdown();
    }
    private final Map<UUID, ItemStack[]> lobbyItems = new ConcurrentHashMap<UUID, ItemStack[]>();
    private final Map<UUID, ItemStack[]> lobbyArmor = new ConcurrentHashMap<UUID, ItemStack[]>();
    private final Map<UUID, ItemStack[]> smpItems = new ConcurrentHashMap<UUID, ItemStack[]>();
    Locator locator;
    private final Map<UUID, ItemStack[]> bauItems = new ConcurrentHashMap<UUID, ItemStack[]>();
    private final Map<UUID, ItemStack[]> bauArmor = new ConcurrentHashMap<UUID, ItemStack[]>();
    private final Map<UUID, ItemStack> bauOffhand = new ConcurrentHashMap<UUID, ItemStack>();
    /** Spieler, die gerade im Bau-SMP sind (eigenes Inventar, eigenes Auktionshaus). */
    private final java.util.Set<UUID> bauMode = ConcurrentHashMap.newKeySet();
    private final Map<UUID, ItemStack[]> smpArmor = new ConcurrentHashMap<UUID, ItemStack[]>();
    private final Map<UUID, ItemStack> lobbyOffhand = new ConcurrentHashMap<UUID, ItemStack>();
    private final Map<UUID, ItemStack> smpOffhand = new ConcurrentHashMap<UUID, ItemStack>();
    /** Eigenes XP-Level je Bereich: {Level, Fortschritt}. In Lobby, Duellen, Events und Parkour gibt es kein XP. */
    private final Map<UUID, float[]> smpXp = new ConcurrentHashMap<UUID, float[]>();
    private final Map<UUID, float[]> bauXp = new ConcurrentHashMap<UUID, float[]>();

    private Map<UUID, float[]> xpMap(Map<UUID, ItemStack[]> items) {
        return items == this.smpItems ? this.smpXp : items == this.bauItems ? this.bauXp : null;
    }

    private void saveXp(UUID id, Player player, Map<UUID, ItemStack[]> items) {
        Map<UUID, float[]> map = this.xpMap(items);
        if (map == null) {
            return; // Lobby: kein XP
        }
        Integer combatLevel = this.combatXpLevels.get(id);
        if (combatLevel != null) {
            // Im SMP-Kampf zeigt die XP-Leiste den Kampf-Timer: gesichert wird der echte Wert von vorher
            Float progress = this.combatXpProgress.get(id);
            map.put(id, new float[]{combatLevel.intValue(), progress == null ? 0.0f : progress.floatValue()});
        } else {
            map.put(id, new float[]{player.getLevel(), player.getExp()});
        }
    }

    private void restoreXp(UUID id, Player player, Map<UUID, ItemStack[]> items) {
        Map<UUID, float[]> map = this.xpMap(items);
        float[] xp = map == null ? null : map.get(id);
        player.setLevel(xp == null ? 0 : (int)xp[0]);
        player.setExp(xp == null ? 0.0f : Math.max(0.0f, Math.min(0.999f, xp[1])));
    }

    /** Kein XP ausserhalb von SMP und Bau-SMP (Lobby, Duelle, Events, Parkour). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onXpGain(org.bukkit.event.player.PlayerExpChangeEvent event) {
        if (!Zone.survival(this, event.getPlayer())) {
            event.setAmount(0);
        }
    }
    private final Map<UUID, String> duelBaseProfiles = new ConcurrentHashMap<UUID, String>();
    final Set<UUID> activeDuelPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> duelCountdownPlayers = ConcurrentHashMap.newKeySet();
    final Set<UUID> pendingDuelLosers = ConcurrentHashMap.newKeySet();
    final Set<UUID> duelResultPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, UUID> duelDeathWinners = new ConcurrentHashMap<UUID, UUID>();
    final Map<UUID, Location> duelDeathLocations = new ConcurrentHashMap<UUID, Location>();
    final Map<UUID, Integer> duelDeathFinishTasks = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, Integer> duelLeaveTasks = new ConcurrentHashMap<UUID, Integer>();
    private final Map<UUID, String> personalKitEditors = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, String> playerRanks = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, PermissionAttachment> staffAttachments = new ConcurrentHashMap<UUID, PermissionAttachment>();
    private final Map<UUID, ItemStack[]> personalKitOriginalItems = new ConcurrentHashMap<UUID, ItemStack[]>();
    private final Map<UUID, ItemStack[]> personalKitOriginalArmor = new ConcurrentHashMap<UUID, ItemStack[]>();
    private final Map<UUID, ItemStack> personalKitOriginalOffhand = new ConcurrentHashMap<UUID, ItemStack>();
    private final Set<String> pendingNpcSpawns = ConcurrentHashMap.newKeySet();
    private final Map<String, Map<String, BlockData>> arenaSnapshots = new ConcurrentHashMap<String, Map<String, BlockData>>();
    private final Set<String> snapshottingArenas = ConcurrentHashMap.newKeySet();
    NamespacedKey npcKey;
    RecordStore records;
    PackHost packHost;
    Rtp rtp;
    final Map<UUID, Long> lastLobbyReturn = new ConcurrentHashMap<UUID, Long>();
    KitLayouts kitLayouts;

    /** Einmalige Anpassung alter config.yml-Dateien an neue Standardwerte (Markt, Drachenei, Pack-Pflicht). */
    private void migrateConfig() {
        try {
            java.io.File raw = new java.io.File(this.getDataFolder(), "config.yml");
            int applied = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(raw).getInt("config-version", 0);
            if (applied >= 9) {
                return;
            }
            org.bukkit.configuration.file.FileConfiguration cfg = this.getConfig();
            if (applied < 4) {
                cfg.set("market.step", 0.006);
                cfg.set("market.recovery-per-minute", 0.02);
                cfg.set("dragonegg.radius", 20);
                cfg.set("dragonegg.capture-seconds", 30);
                cfg.set("protection.lobby-radius", 200.0);
                cfg.set("bausmp.border-radius", 30000.0);
                this.getLogger().info("config.yml auf Version 4 angehoben (Markt, Drachenei, Pack-Pflicht).");
            }
            // Version 5 (Plugin 1.0): neue Einstellungen sichtbar machen, bestehende Werte bleiben unveraendert
            for (String[] entry : new String[][]{{"ah.tax-percent", "0"}, {"duels.elo-k", "32"}, {"economy.audit-threshold", "100000"}}) {
                if (!cfg.contains(entry[0])) {
                    cfg.set(entry[0], Integer.parseInt(entry[1]));
                }
            }
            if (!cfg.contains("back.enabled")) {
                cfg.set("back.enabled", true);
            }
            if (!cfg.contains("back.death")) {
                cfg.set("back.death", true);
            }
            if (!cfg.contains("claims.enabled")) cfg.set("claims.enabled", true);
            if (!cfg.contains("claims.database")) cfg.set("claims.database", "claims.db");
            if (!cfg.contains("claims.max-per-player")) cfg.set("claims.max-per-player", 3);
            if (!cfg.contains("claims.max-area")) cfg.set("claims.max-area", 10000);
            if (!cfg.contains("claims.default-radius")) cfg.set("claims.default-radius", 8);
            if (!cfg.contains("claims.min-radius")) cfg.set("claims.min-radius", 2);
            if (!cfg.contains("claims.protect-pvp")) cfg.set("claims.protect-pvp", true);
            if (!cfg.contains("claims.protect-explosions")) cfg.set("claims.protect-explosions", true);
            if (!cfg.contains("claims.admin-bypass-permission")) cfg.set("claims.admin-bypass-permission", "duelnetwork.claim.bypass");
            // Version 9: keine kuenstliche Volumengrenze fuer Duel-Arenen mehr.
            cfg.set("duels.max-snapshot-blocks", null);
            cfg.set("config-version", 9);
            this.saveConfig();
            this.getLogger().info("config.yml auf Version 9 angehoben (Duel-Arenen ohne feste Groessengrenze).");
        }
        catch (Throwable t) {
            this.getLogger().warning("config.yml konnte nicht migriert werden: " + t);
        }
    }

    public void onEnable() {
        BackupManager.applyPendingRestore(this);
        Store.init(this.getLogger());
        this.saveDefaultConfig();
        Store.verbose = this.getConfig().getBoolean("debug", false);
        this.moderation = new Moderation(this);
        Messages.init(this);
        Audit.init(this.getDataFolder());
        DuelFixes.install(this);
        this.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ProtectionListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ParkourEnhancements(this), (Plugin)this);
        this.saveDefaultConfig();
        this.migrateConfig();
        this.configProblems = ConfigCheck.run(this);
        new Hints(this);
        this.getServer().getScheduler().runTaskTimer((Plugin)this, Perf.wrap("PartyTab", () -> this.refreshPartyTab()), 40L, 40L);
        this.getServer().getScheduler().runTaskTimer((Plugin)this, Perf.wrap("BorderView", () -> this.refreshBorderView()), 20L, 20L);
        this.backups = new BackupManager(this);
        this.backups.start();
        this.records = new RecordStore(this);
        this.kitLayouts = new KitLayouts(this);
        this.getConfig().addDefault("smp.border-radius", (Object)40000.0);
        this.getConfig().addDefault("smp.nether-radius", (Object)20000.0);
        this.getConfig().addDefault("smp.end-radius", (Object)50000.0);
        this.getConfig().addDefault("protection.lobby-radius", (Object)200.0);
        this.getConfig().addDefault("gems.price.shovel", (Object)600);
        this.getConfig().addDefault("economy.afk-check", (Object)true);
        this.getConfig().addDefault("locator.dragon-egg-only", (Object)true);
        this.getConfig().addDefault("bausmp.veinminer.enabled", (Object)true);
        this.getConfig().addDefault("bausmp.veinminer.max-blocks", (Object)64);
        this.getConfig().addDefault("anticheat.max-breaks-per-second", (Object)16);
        this.getConfig().addDefault("anticheat.xray.enabled", (Object)true);
        this.getConfig().addDefault("anticheat.xray.window-minutes", (Object)15);
        this.getConfig().addDefault("anticheat.xray.min-hidden", (Object)5);
        this.getConfig().addDefault("anticheat.xray.min-ratio", (Object)0.04);
        this.getConfig().addDefault("parkour.maps", Map.of());
        this.getConfig().addDefault("claims.enabled", (Object)true);
        this.getConfig().addDefault("claims.database", (Object)"claims.db");
        this.getConfig().addDefault("claims.max-per-player", (Object)3);
        this.getConfig().addDefault("claims.max-area", (Object)10000);
        this.getConfig().addDefault("claims.default-radius", (Object)8);
        this.getConfig().addDefault("claims.min-radius", (Object)2);
        this.getConfig().addDefault("claims.protect-pvp", (Object)true);
        this.getConfig().addDefault("claims.protect-explosions", (Object)true);
        this.getConfig().addDefault("claims.admin-bypass-permission", (Object)"duelnetwork.claim.bypass");
        this.getConfig().addDefault("smp.home-teleport-delay", (Object)3);
        this.getConfig().addDefault("smp.tpa-expire-seconds", (Object)60);
        this.getConfig().addDefault("smp.combat.base-seconds", (Object)15);
        this.getConfig().addDefault("smp.combat.seconds-per-heart", (Object)5);
        this.getConfig().addDefault("smp.combat.seconds-per-hit", (Object)2);
        if (this.getConfig().getInt("smp.combat.base-seconds", 15) == 30) {
            this.getConfig().set("smp.combat.base-seconds", (Object)15);
        }
        if (this.getConfig().getInt("smp.combat.seconds-per-heart", 5) == 10) {
            this.getConfig().set("smp.combat.seconds-per-heart", (Object)5);
        }
        if (this.getConfig().getInt("smp.combat.seconds-per-hit", 2) == 3) {
            this.getConfig().set("smp.combat.seconds-per-hit", (Object)2);
        }
        this.getConfig().set("resourcepack.hold-seconds", null);
        this.getConfig().set("resourcepack.kick-on-missing", null);
        this.getConfig().options().copyDefaults(true);
        if (!this.getConfig().getBoolean("smp.border-v3", false)) {
            // SMP-Border: 20.000 Bloecke in jede Richtung (Overworld/Nether), End etwas groesser
            double old = this.getConfig().getDouble("smp.border-radius", 5000.0);
            if (old == 5000.0 || old == 10000.0) {
                this.getConfig().set("smp.border-radius", 20000.0);
            }
            this.getConfig().set("smp.border-v3", true);
        }
        if (!this.getConfig().getBoolean("smp.border-v4", false)) {
            // Groessere SMP-Zone: Overworld 40.000, Nether 20.000, End 50.000 Bloecke in jede Richtung
            double over = this.getConfig().getDouble("smp.border-radius", 40000.0);
            if (over == 5000.0 || over == 10000.0 || over == 20000.0) {
                this.getConfig().set("smp.border-radius", 40000.0);
            }
            double end = this.getConfig().getDouble("smp.end-radius", 50000.0);
            if (end == 24000.0) {
                this.getConfig().set("smp.end-radius", 50000.0);
            }
            this.getConfig().set("smp.border-v4", true);
        }
        this.saveConfig();
        if (this.getConfig().getBoolean("claims.enabled", true)) {
            this.claimManager = new ClaimManager(this);
            this.getServer().getPluginManager().registerEvents((Listener)new ClaimProtectionListener(this.claimManager), (Plugin)this);
            this.getServer().getPluginManager().registerEvents((Listener)new ClaimLog(this, this.claimManager), (Plugin)this);
        }
        this.npcKey = new NamespacedKey((Plugin)this, "menu_npc");
        for (String command : List.of("navigator", "duel", "lobby", "smp", "events", "parkour", "bausmp", "home", "sethome", "delhome", "tpa", "tpahere", "party", "kit", "rank", "ban", "kick", "unban", "duelladmin")) {
            if (this.getCommand(command) == null) continue;
            this.getCommand(command).setExecutor((CommandExecutor)this);
            this.getCommand(command).setTabCompleter((TabCompleter)this);
        }
        if (this.claimManager != null && this.getCommand("claim") != null) {
            this.getServer().getPluginManager().registerEvents((Listener)new ClaimMenu(this, this.claimManager), (Plugin)this);
            ClaimCommand claimCommand = new ClaimCommand(this, this.claimManager);
            this.getCommand("claim").setExecutor((CommandExecutor)claimCommand);
            this.getCommand("claim").setTabCompleter((TabCompleter)claimCommand);
        }
        // Team-Befehle: eigene Vorschlaege (Spielernamen, Bann-Dauer, Raenge, gebannte Spieler)
        for (String command : List.of("ban", "kick", "unban", "rank")) {
            if (this.getCommand(command) != null) {
                this.getCommand(command).setTabCompleter((TabCompleter)this.moderation);
            }
        }
        this.getServer().getPluginManager().registerEvents((Listener)new BauSmp(this), (Plugin)this);
        AntiCheat antiCheat = new AntiCheat(this);
        this.getServer().getPluginManager().registerEvents((Listener)antiCheat, (Plugin)this);
        if (this.getCommand("anticheat") != null) {
            this.getCommand("anticheat").setExecutor((CommandExecutor)antiCheat);
            this.getCommand("anticheat").setTabCompleter((TabCompleter)antiCheat);
        }
        Extras extras = new Extras(this);
        this.getServer().getPluginManager().registerEvents((Listener)extras, (Plugin)this);
        for (String extraCommand : List.of("msg", "r", "ignore", "ping", "report", "reports")) {
            if (this.getCommand(extraCommand) != null) {
                this.getCommand(extraCommand).setExecutor((CommandExecutor)extras);
                this.getCommand(extraCommand).setTabCompleter((TabCompleter)extras);
            }
        }
        Market market = new Market(this);
        this.getServer().getPluginManager().registerEvents((Listener)market, (Plugin)this);
        Economy economy = new Economy(this);
        this.getServer().getPluginManager().registerEvents((Listener)economy, (Plugin)this);
        Gems gems = new Gems(this);
        this.getServer().getPluginManager().registerEvents((Listener)gems, (Plugin)this);
        Locator locator = new Locator(this);
        this.getServer().getPluginManager().registerEvents((Listener)locator, (Plugin)this);
        this.locator = locator;
        XRay xray = new XRay(this);
        this.getServer().getPluginManager().registerEvents((Listener)xray, (Plugin)this);
        VeinMiner veinMiner = new VeinMiner(this);
        this.getServer().getPluginManager().registerEvents((Listener)veinMiner, (Plugin)this);
        Shop shop = new Shop(this);
        this.getServer().getPluginManager().registerEvents((Listener)shop, (Plugin)this);
        AuctionHouse auctionHouse = new AuctionHouse(this);
        this.getServer().getPluginManager().registerEvents((Listener)auctionHouse, (Plugin)this);
        Orders orders = new Orders(this);
        this.getServer().getPluginManager().registerEvents((Listener)orders, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new Wizard(this, auctionHouse, orders), (Plugin)this);
        SmpTop smpTop = new SmpTop(this);
        this.getServer().getPluginManager().registerEvents((Listener)smpTop, (Plugin)this);
        DragonEgg dragonEgg = new DragonEgg(this);
        this.getServer().getPluginManager().registerEvents((Listener)dragonEgg, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new AdminMenu(this), (Plugin)this);
        Webhook.startStatus(this);
        new Prefs(this);
        this.getServer().getPluginManager().registerEvents((Listener)new SettingsMenu(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new HintCommands(this), (Plugin)this);
        DuelSummary duelSummary = new DuelSummary(this);
        this.getServer().getPluginManager().registerEvents((Listener)duelSummary, (Plugin)this);
        if (this.getCommand("matchstats") != null) {
            this.getCommand("matchstats").setExecutor((org.bukkit.command.CommandExecutor)duelSummary);
        }
        new LagWatch(this);
        this.getServer().getScheduler().runTaskTimer((Plugin)this, Perf.wrap("Matchmaker", this::matchmakeRanked), 40L, 20L);
        new Share(this);
        this.getServer().getPluginManager().registerEvents((Listener)new Afk(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new Sleep(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new WorldTime(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new Grave(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new DuelExtras(this), (Plugin)this);
        Staff staffTools = new Staff(this);
        this.getServer().getPluginManager().registerEvents((Listener)staffTools, (Plugin)this);
        new Marks(this);
        // Anzeige an den Duell-NPCs (wartende Spieler/laufende Duelle) alle 10 Sekunden, nur in Welten mit Spielern
        this.getServer().getScheduler().runTaskTimer((Plugin)this, Perf.wrap("NpcBoards", () -> {
            for (World world : this.getServer().getWorlds()) {
                if (world.getPlayers().isEmpty()) {
                    continue;
                }
                for (Entity entity : world.getEntities()) {
                    String npcType = (String)entity.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING);
                    if (npcType != null && List.of("duell", "ranked", "unranked", "team").contains(npcType)) {
                        entity.customName(this.npcName(npcType));
                    }
                }
            }
        }), 200L, 200L);
        TeamQueue teamQueue = new TeamQueue(this);
        this.getServer().getPluginManager().registerEvents((Listener)teamQueue, (Plugin)this);
        EventGames eventGames = new EventGames(this);
        this.getServer().getPluginManager().registerEvents((Listener)eventGames, (Plugin)this);
        ValueTags valueTags = new ValueTags(this);
        this.getServer().getPluginManager().registerEvents((Listener)valueTags, (Plugin)this);
        Rtp rtp = new Rtp(this);
        this.rtp = rtp;
        this.getServer().getPluginManager().registerEvents((Listener)rtp, (Plugin)this);
        if (this.getCommand("rtp") != null) {
            this.getCommand("rtp").setExecutor((CommandExecutor)rtp);
            this.getCommand("rtp").setTabCompleter((TabCompleter)rtp);
        }
        Sidebar sidebar = new Sidebar(this);
        this.getServer().getPluginManager().registerEvents((Listener)sidebar, (Plugin)this);
        for (String economyCommand : List.of("money", "pay", "baltop", "sell", "market", "worth")) {
            if (this.getCommand(economyCommand) != null) {
                this.getCommand(economyCommand).setExecutor((CommandExecutor)economy);
                this.getCommand(economyCommand).setTabCompleter((TabCompleter)economy);
            }
        }
        if (this.getCommand("shop") != null) {
            this.getCommand("shop").setExecutor((CommandExecutor)shop);
        }
        if (this.getCommand("ah") != null) {
            this.getCommand("ah").setExecutor((CommandExecutor)auctionHouse);
            this.getCommand("ah").setTabCompleter((TabCompleter)auctionHouse);
        }
        if (this.getCommand("order") != null) {
            this.getCommand("order").setExecutor((CommandExecutor)orders);
            this.getCommand("order").setTabCompleter((TabCompleter)orders);
        }
        if (this.getCommand("sidebar") != null) {
            this.getCommand("sidebar").setExecutor((CommandExecutor)sidebar);
        }
        Events events = new Events(this);
        this.getServer().getPluginManager().registerEvents((Listener)events, (Plugin)this);
        DuelStats duelStats = new DuelStats(this);
        this.getServer().getPluginManager().registerEvents((Listener)duelStats, (Plugin)this);
        if (this.getCommand("rangliste") != null) {
            this.getCommand("rangliste").setExecutor((CommandExecutor)duelStats);
            this.getCommand("rangliste").setTabCompleter((TabCompleter)duelStats);
        }
        if (this.getCommand("duelstats") != null) {
            this.getCommand("duelstats").setExecutor((CommandExecutor)duelStats);
            this.getCommand("duelstats").setTabCompleter((TabCompleter)duelStats);
        }
        this.ensureDefaultKits();
        this.configureSmpWorldBorder();
        // Alte Villager-NPCs automatisch in Mannequins (echte Skins) umwandeln
        this.getServer().getScheduler().runTaskLater((Plugin)this, () -> { this.convertVillagerNpcs(); this.refreshNpcNames(); this.styleAllNpcs(); }, 60L);
        this.setupModules();
        this.getLogger().info("DuelNetwork aktiviert: Ein-Server/Eine-Welt-GUI bereit.");
    }

    /** Neue Module (ab 1.0): Mutes, Warps und /back, /duelnetwork sowie die optionalen Anbindungen an Vault und PlaceholderAPI. */
    private void setupModules() {
        this.getServer().getPluginManager().registerEvents((Listener)new ChatGuard(this), (Plugin)this);
        Bounty bounty = new Bounty(this);
        this.getServer().getPluginManager().registerEvents((Listener)bounty, (Plugin)this);
        if (this.getCommand("bounty") != null) {
            this.getCommand("bounty").setExecutor((CommandExecutor)bounty);
            this.getCommand("bounty").setTabCompleter((TabCompleter)bounty);
        }
        this.mutes = new Mutes(this);
        this.getServer().getPluginManager().registerEvents((Listener)this.mutes, (Plugin)this);
        for (String name : List.of("mute", "unmute", "timeout")) {
            if (this.getCommand(name) != null) {
                this.getCommand(name).setExecutor((CommandExecutor)this.mutes);
                this.getCommand(name).setTabCompleter((TabCompleter)this.mutes);
            }
        }
        if (this.getCommand("modlog") != null) {
            this.getCommand("modlog").setExecutor((sender, command, label, args) -> {
                this.moderation.modlog(sender, args);
                return true;
            });
            this.getCommand("modlog").setTabCompleter((TabCompleter)this.moderation);
        }
        PackHost packHost = new PackHost(this);
        this.packHost = packHost;
        this.getServer().getPluginManager().registerEvents((Listener)packHost, (Plugin)this);
        if (this.getCommand("texturen") != null) {
            this.getCommand("texturen").setExecutor((CommandExecutor)packHost);
        }
        Boards boards = new Boards(this);
        this.getServer().getPluginManager().registerEvents((Listener)boards, (Plugin)this);
        if (this.getCommand("board") != null) {
            this.getCommand("board").setExecutor((CommandExecutor)boards);
            this.getCommand("board").setTabCompleter((TabCompleter)boards);
        }
        Watch watch = new Watch(this);
        this.getServer().getPluginManager().registerEvents((Listener)watch, (Plugin)this);
        if (this.getCommand("spectate") != null) {
            this.getCommand("spectate").setExecutor((CommandExecutor)watch);
            this.getCommand("spectate").setTabCompleter((TabCompleter)watch);
        }
        Cosmetics cosmetics = new Cosmetics(this);
        this.getServer().getPluginManager().registerEvents((Listener)cosmetics, (Plugin)this);
        if (this.getCommand("cosmetics") != null) {
            this.getCommand("cosmetics").setExecutor((CommandExecutor)cosmetics);
        }
        Friends friends = new Friends(this);
        this.getServer().getPluginManager().registerEvents((Listener)friends, (Plugin)this);
        if (this.getCommand("friend") != null) {
            this.getCommand("friend").setExecutor((CommandExecutor)friends);
            this.getCommand("friend").setTabCompleter((TabCompleter)friends);
        }
        Trade trade = new Trade(this);
        this.getServer().getPluginManager().registerEvents((Listener)trade, (Plugin)this);
        if (this.getCommand("trade") != null) {
            this.getCommand("trade").setExecutor((CommandExecutor)trade);
            this.getCommand("trade").setTabCompleter((TabCompleter)trade);
        }
        Seasons seasons = new Seasons(this);
        if (this.getCommand("season") != null) {
            this.getCommand("season").setExecutor((CommandExecutor)seasons);
        }
        Social social = new Social(this);
        this.getServer().getPluginManager().registerEvents((Listener)social, (Plugin)this);
        for (String socialCommand : List.of("tpatoggle", "pc", "fc")) {
            if (this.getCommand(socialCommand) != null) {
                this.getCommand(socialCommand).setExecutor((CommandExecutor)social);
            }
        }
        new AutoRestart(this);
        Notes notes = new Notes(this);
        for (String noteCommand : List.of("note", "notes")) {
            if (this.getCommand(noteCommand) != null) {
                this.getCommand(noteCommand).setExecutor((CommandExecutor)notes);
                this.getCommand(noteCommand).setTabCompleter((TabCompleter)notes);
            }
        }
        new Cleanup(this);
        Guide guide = new Guide(this);
        this.getServer().getPluginManager().registerEvents((Listener)guide, (Plugin)this);
        for (String guideCommand : List.of("tutorial", "stats", "ranked")) {
            if (this.getCommand(guideCommand) != null) {
                this.getCommand(guideCommand).setExecutor((CommandExecutor)guide);
                this.getCommand(guideCommand).setTabCompleter((TabCompleter)guide);
            }
        }
        Ghosts ghosts = new Ghosts(this);
        this.getServer().getPluginManager().registerEvents((Listener)ghosts, (Plugin)this);
        Comfort comfort = new Comfort(this);
        this.getServer().getPluginManager().registerEvents((Listener)comfort, (Plugin)this);
        for (String comfortCommand : List.of("daily", "quests", "tpmenu", "dnwartung")) {
            if (this.getCommand(comfortCommand) != null) {
                this.getCommand(comfortCommand).setExecutor((CommandExecutor)comfort);
                this.getCommand(comfortCommand).setTabCompleter((TabCompleter)comfort);
            }
        }
        this.warps = new Warps(this);
        this.getServer().getPluginManager().registerEvents((Listener)this.warps, (Plugin)this);
        for (String name : List.of("warp", "warps", "setwarp", "delwarp", "back")) {
            if (this.getCommand(name) != null) {
                this.getCommand(name).setExecutor((CommandExecutor)this.warps);
                this.getCommand(name).setTabCompleter((TabCompleter)this.warps);
            }
        }
        AdminCommand admin = new AdminCommand(this);
        if (this.getCommand("duelnetwork") != null) {
            this.getCommand("duelnetwork").setExecutor((CommandExecutor)admin);
            this.getCommand("duelnetwork").setTabCompleter((TabCompleter)admin);
        }
        // Optionale Anbindungen: jeweils nur, wenn das andere Plugin installiert ist. Fehler stoppen das Plugin nicht.
        if (this.getServer().getPluginManager().getPlugin("Vault") != null) {
            try {
                this.hookVault();
                this.vaultActive = true;
                this.getLogger().info("Vault gefunden: DuelNetwork-Wirtschaft ist als Vault-Economy registriert.");
            }
            catch (Throwable t) {
                Store.problem("Vault", t);
            }
        }
        if (this.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                this.papiActive = this.hookPapi();
                if (this.papiActive) {
                    this.getLogger().info("PlaceholderAPI gefunden: Platzhalter %duelnetwork_...% sind verfuegbar.");
                }
            }
            catch (Throwable t) {
                Store.problem("PlaceholderAPI", t);
            }
        }
    }

    /** In eigenen Methoden, damit die Vault-/PAPI-Klassen nur geladen werden, wenn das Plugin wirklich da ist. */
    private void hookVault() {
        VaultHook.register(this);
    }

    private boolean hookPapi() {
        return new PapiHook(this).register();
    }

    /** Laedt config.yml und messages.yml neu (/duelnetwork reload und /duelladmin reload). */
    void reloadAll() {
        this.flushConfig();
        Store.flush(); // erst fertig schreiben, dann neu lesen - sonst gehen ungespeicherte Aenderungen verloren
        this.reloadConfig();
        Store.verbose = this.getConfig().getBoolean("debug", false);
        this.migrateConfig();
        Messages.reload();
    }

    String rankOf(UUID id) {
        String rank = this.playerRanks.get(id);
        return rank == null ? "" : rank;
    }

    private ItemStack enchanted(Material material, Enchantment enchantment, int level) {
        ItemStack item = new ItemStack(material);
        item.addUnsafeEnchantment(enchantment, level);
        return item;
    }

    private ItemStack durable(ItemStack item) {
        item.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
        item.addUnsafeEnchantment(Enchantment.MENDING, 1);
        return item;
    }

    private ItemStack potion(PotionEffectType effect, int seconds, int amplifier, int amount, String name) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION, amount);
        PotionMeta meta = (PotionMeta)potion.getItemMeta();
        if (meta != null) {
            meta.addCustomEffect(new PotionEffect(effect, seconds * 20, amplifier), true);
            meta.setDisplayName(String.valueOf(ChatColor.LIGHT_PURPLE) + name);
            potion.setItemMeta((ItemMeta)meta);
        }
        return potion;
    }

    private ItemStack tippedArrow(PotionEffectType effect, int seconds, int amount) {
        ItemStack arrows = new ItemStack(Material.TIPPED_ARROW, amount);
        PotionMeta meta = (PotionMeta)arrows.getItemMeta();
        if (meta != null) {
            meta.addCustomEffect(new PotionEffect(effect, seconds * 20, 0), true);
            arrows.setItemMeta((ItemMeta)meta);
        }
        return arrows;
    }

    private ItemStack slowFallingArrows(int n) {
        return new ItemStack(Material.ARROW, n);
    }

    private ItemStack totem() {
        return new ItemStack(Material.TOTEM_OF_UNDYING, 1);
    }

    private ItemStack maceReach() {
        return this.durable(this.enchanted(Material.MACE, Enchantment.BREACH, 4));
    }

    private ItemStack maceWind() {
        ItemStack mace = this.durable(this.enchanted(Material.MACE, Enchantment.DENSITY, 5));
        mace.addUnsafeEnchantment(Enchantment.WIND_BURST, 1);
        return mace;
    }

    private List<ItemStack> kit(ItemStack weapon, ItemStack[] armor, ItemStack ... extras) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        items.add(weapon);
        items.addAll(Arrays.asList(armor));
        items.addAll(Arrays.asList(extras));
        return items;
    }

    private ItemStack[] armor(String material, int protection) {
        Material legs;
        Material chest;
        Material helmet;
        boolean netherite = material.equalsIgnoreCase("netherite");
        boolean diamond = material.equalsIgnoreCase("diamond");
        helmet = netherite ? Material.NETHERITE_HELMET : (diamond ? Material.DIAMOND_HELMET : Material.IRON_HELMET);
        chest = netherite ? Material.NETHERITE_CHESTPLATE : (diamond ? Material.DIAMOND_CHESTPLATE : Material.IRON_CHESTPLATE);
        legs = netherite ? Material.NETHERITE_LEGGINGS : (diamond ? Material.DIAMOND_LEGGINGS : Material.IRON_LEGGINGS);
        Material boots = netherite ? Material.NETHERITE_BOOTS : (diamond ? Material.DIAMOND_BOOTS : Material.IRON_BOOTS);
        ItemStack[] result = new ItemStack[]{this.enchanted(helmet, Enchantment.PROTECTION, protection), this.enchanted(chest, Enchantment.PROTECTION, protection), this.enchanted(legs, Enchantment.PROTECTION, protection), this.enchanted(boots, Enchantment.PROTECTION, protection)};
        for (ItemStack piece : result) {
            piece.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
            piece.addUnsafeEnchantment(Enchantment.MENDING, 1);
        }
        result[0].addUnsafeEnchantment(Enchantment.RESPIRATION, 3);
        result[0].addUnsafeEnchantment(Enchantment.AQUA_AFFINITY, 1);
        result[3].addUnsafeEnchantment(Enchantment.FEATHER_FALLING, 4);
        result[3].addUnsafeEnchantment(Enchantment.DEPTH_STRIDER, 3);
        return result;
    }

    private void ensureItemTotal(List<ItemStack> items, Material material, int minimum) {
        int amount;
        int maxStack;
        int n = maxStack = material == Material.ENDER_PEARL ? 16 : material.getMaxStackSize();
        for (int total = items.stream().filter(item -> item.getType() == material).mapToInt(ItemStack::getAmount).sum(); total < minimum; total += amount) {
            amount = Math.min(maxStack, minimum - total);
            items.add(new ItemStack(material, amount));
        }
    }

    private List<ItemStack> normalizeStoredKit(String kitName, List<?> rawItems) {
        String kit = kitName.toLowerCase();
        ArrayList<ItemStack> result = new ArrayList<ItemStack>();
        for (Object value : rawItems) {
            ItemMeta meta;
            if (!(value instanceof ItemStack)) continue;
            ItemStack item2 = ((ItemStack)value).clone();
            if (item2.getType() == Material.AIR) {
                result.add(item2);
                continue;
            }
            if (kit.equals("axe pvp")) {
                for (Enchantment enchantment : new ArrayList<Enchantment>(item2.getEnchantments().keySet())) {
                }
            } else if (item2.getType() == Material.SHIELD) {
            }
            if (item2.getType() == Material.MACE && (meta = item2.getItemMeta()) != null) {
                meta.setDisplayName(null);
                meta.removeAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE);
                item2.setItemMeta(meta);
            }
            if (kit.equals("op netherite") && item2.getType() == Material.NETHERITE_AXE) {
            }
            if (kit.equals("op netherite") && item2.getType() == Material.NETHERITE_SWORD) {
            }
            if (item2.getType() == Material.ENDER_PEARL) {
                int stackAmount;
                for (int amount = Math.max(1, item2.getAmount()); amount > 0; amount -= stackAmount) {
                    stackAmount = Math.min(16, amount);
                    result.add(new ItemStack(Material.ENDER_PEARL, stackAmount));
                }
                continue;
            }
            result.add(item2);
        }
        if (kit.equals("mace pvp")) {
            int keptMaces = 0;
            for (int i = result.size() - 1; i >= 0; --i) {
                if (((ItemStack)result.get(i)).getType() != Material.MACE || ++keptMaces <= 2) continue;
                result.remove(i);
            }
            while (result.stream().filter(item -> item.getType() == Material.MACE).count() < 2L) {
                long count = result.stream().filter(item -> item.getType() == Material.MACE).count();
                result.add(count % 2L == 0L ? this.maceReach() : this.maceWind());
            }
            this.ensureItemTotal(result, Material.BREEZE_ROD, 64);
        }
        if (kit.equals("op netherite")) {
            for (int potionAmount = result.stream().filter(item -> item.getType() == Material.SPLASH_POTION).mapToInt(ItemStack::getAmount).sum(); potionAmount < 12; ++potionAmount) {
                result.add(this.potion(PotionEffectType.INSTANT_HEALTH, 1, 1, 1, "Heilung"));
            }
            this.ensureItemTotal(result, Material.EXPERIENCE_BOTTLE, 64);
            if (result.stream().noneMatch(item -> item.getType() == Material.MACE)) {
                result.add(this.maceReach());
            }
        }
        if (kit.equals("smp")) {
            this.ensureItemTotal(result, Material.ENDER_PEARL, 16);
            this.ensureItemTotal(result, Material.GOLDEN_APPLE, 8);
            for (int potionAmount = result.stream().filter(item -> item.getType() == Material.SPLASH_POTION).mapToInt(ItemStack::getAmount).sum(); potionAmount < 8; ++potionAmount) {
                result.add(this.potion(PotionEffectType.INSTANT_HEALTH, 1, 1, 1, "Heilung"));
            }
        }
        if (kit.equals("crystal pvp")) {
            this.ensureItemTotal(result, Material.TOTEM_OF_UNDYING, 12);
            this.ensureItemTotal(result, Material.GOLDEN_APPLE, 1);
            this.ensureItemTotal(result, Material.ENDER_PEARL, 32);
            for (int potionAmount = result.stream().filter(item -> item.getType() == Material.SPLASH_POTION).mapToInt(ItemStack::getAmount).sum(); potionAmount < 12; ++potionAmount) {
                result.add(this.potion(PotionEffectType.INSTANT_HEALTH, 1, 1, 1, "Heilung"));
            }
        }
        return result;
    }

    private void ensureDefaultKits() {
        Map<String, List<ItemStack>> defaults = Map.of("axe pvp", this.kit(this.durable(this.enchanted(Material.DIAMOND_AXE, Enchantment.SHARPNESS, 5)), this.armor("diamond", 4), this.durable(this.enchanted(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 3)), this.enchanted(Material.BOW, Enchantment.POWER, 3), this.durable(this.enchanted(Material.CROSSBOW, Enchantment.QUICK_CHARGE, 3)), new ItemStack(Material.ARROW, 16), new ItemStack(Material.SHIELD), new ItemStack(Material.GOLDEN_APPLE, 6)), "sword pvp", this.kit(this.durable(this.enchanted(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 5)), this.armor("diamond", 4), new ItemStack[0]), "mace pvp", this.kit(this.maceReach(), this.armor("diamond", 4), this.maceWind(), this.durable(this.enchanted(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 5)), this.durable(this.enchanted(Material.DIAMOND_AXE, Enchantment.SHARPNESS, 4)), new ItemStack(Material.ELYTRA), new ItemStack(Material.SHIELD), new ItemStack(Material.WIND_CHARGE, 64), new ItemStack(Material.WIND_CHARGE, 64), this.totem(), this.totem(), this.totem(), this.potion(PotionEffectType.STRENGTH, 90, 1, 6, "St\u00e4rke II"), this.potion(PotionEffectType.SPEED, 480, 0, 3, "Speed I"), this.potion(PotionEffectType.FIRE_RESISTANCE, 480, 0, 3, "Feuerresistenz I"), new ItemStack(Material.ENDER_PEARL, 64), new ItemStack(Material.ENDER_PEARL, 64), new ItemStack(Material.ENDER_PEARL, 64), new ItemStack(Material.ENDER_PEARL, 64), new ItemStack(Material.COBWEB, 16), new ItemStack(Material.LAVA_BUCKET), new ItemStack(Material.WATER_BUCKET), new ItemStack(Material.GOLDEN_APPLE, 64)), "uhc", this.kit(this.durable(this.enchanted(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 3)), this.armor("diamond", 2), this.durable(this.enchanted(Material.DIAMOND_AXE, Enchantment.SHARPNESS, 3)), this.durable(this.enchanted(Material.DIAMOND_PICKAXE, Enchantment.EFFICIENCY, 3)), this.enchanted(Material.BOW, Enchantment.POWER, 2), this.enchanted(Material.CROSSBOW, Enchantment.PIERCING, 1), new ItemStack(Material.ARROW, 32), new ItemStack(Material.GOLDEN_APPLE, 6), new ItemStack(Material.LAVA_BUCKET), new ItemStack(Material.LAVA_BUCKET), new ItemStack(Material.WATER_BUCKET), new ItemStack(Material.WATER_BUCKET), new ItemStack(Material.WATER_BUCKET), new ItemStack(Material.COBWEB, 16), new ItemStack(Material.COBBLESTONE, 64), new ItemStack(Material.OAK_PLANKS, 64), new ItemStack(Material.CRAFTING_TABLE), new ItemStack(Material.SHIELD)), "op netherite", this.kit(this.enchanted(Material.NETHERITE_SWORD, Enchantment.SHARPNESS, 5), this.armor("netherite", 4), new ItemStack(Material.NETHERITE_AXE), new ItemStack(Material.SHIELD), new ItemStack(Material.WIND_CHARGE, 16), this.totem(), this.totem(), this.potion(PotionEffectType.STRENGTH, 90, 1, 2, "St\u00e4rke"), this.potion(PotionEffectType.SPEED, 90, 1, 2, "Speed"), this.potion(PotionEffectType.FIRE_RESISTANCE, 180, 0, 2, "Feuerresistenz"), new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 4), new ItemStack(Material.GOLDEN_APPLE, 64), new ItemStack(Material.COBWEB, 16), new ItemStack(Material.LAVA_BUCKET), new ItemStack(Material.WATER_BUCKET), new ItemStack(Material.ENDER_PEARL, 16)), "smp", this.kit(this.durable(this.enchanted(Material.IRON_SWORD, Enchantment.SHARPNESS, 2)), this.armor("iron", 2), this.durable(this.enchanted(Material.IRON_AXE, Enchantment.SHARPNESS, 2)), new ItemStack(Material.SHIELD), this.enchanted(Material.BOW, Enchantment.POWER, 1), new ItemStack(Material.ARROW, 32), new ItemStack(Material.COOKED_BEEF, 8), new ItemStack(Material.GOLDEN_APPLE, 4), new ItemStack(Material.ENDER_PEARL, 8), this.potion(PotionEffectType.STRENGTH, 90, 0, 2, "St\u00e4rke"), this.potion(PotionEffectType.SPEED, 90, 0, 2, "Speed")), "crystal pvp", this.kit(this.durable(this.enchanted(Material.NETHERITE_SWORD, Enchantment.SHARPNESS, 5)), this.armor("netherite", 4), this.durable(this.enchanted(Material.NETHERITE_AXE, Enchantment.SHARPNESS, 5)), this.durable(this.enchanted(Material.NETHERITE_PICKAXE, Enchantment.EFFICIENCY, 5)), this.enchanted(Material.CROSSBOW, Enchantment.QUICK_CHARGE, 3), new ItemStack(Material.SHIELD), this.slowFallingArrows(64), new ItemStack(Material.OBSIDIAN, 64), new ItemStack(Material.END_CRYSTAL, 64), new ItemStack(Material.RESPAWN_ANCHOR, 16), new ItemStack(Material.GLOWSTONE, 64), this.totem(), this.totem(), this.totem(), this.totem(), this.totem(), this.totem(), this.totem(), this.totem(), this.potion(PotionEffectType.STRENGTH, 90, 1, 1, "St\u00e4rke"), this.potion(PotionEffectType.SPEED, 90, 1, 1, "Speed"), this.potion(PotionEffectType.FIRE_RESISTANCE, 180, 0, 1, "Feuerresistenz"), this.potion(PotionEffectType.INSTANT_HEALTH, 1, 1, 1, "Heilung"), new ItemStack(Material.ENDER_PEARL, 16), new ItemStack(Material.GOLDEN_APPLE, 32)));
        ArrayList<String> available = new ArrayList<String>(this.getConfig().getStringList("kits.available"));
        available.removeIf(value -> List.of("classic", "maze", "maze pvp", "spear maze", "bow", "bow pvp", "sumo", "boxing", "potion pvp").contains(value.toLowerCase()));
        available = new ArrayList<String>(new LinkedHashSet(available));
        boolean migrateDefaults = this.getConfig().getInt("kits.defaultVersion", 0) < 17;
        for (Map.Entry<String, List<ItemStack>> entry : defaults.entrySet()) {
            String path = "kits.data." + entry.getKey();
            if (migrateDefaults || !this.getConfig().contains(path) || this.getConfig().getList(path + ".items", List.of()).isEmpty()) {
                this.getConfig().set(path + ".items", entry.getValue());
            }
            if (available.contains(entry.getKey())) continue;
            available.add(entry.getKey());
        }
        for (String kitName : new ArrayList<String>(available)) {
            List stored = this.getConfig().getList("kits.data." + kitName + ".items", List.of());
            this.getConfig().set("kits.data." + kitName + ".items", this.normalizeStoredKit(kitName, stored));
        }
        this.getConfig().set("kits.defaultVersion", (Object)18);
        this.getConfig().set("kits.available", available);
        this.saveConfig();
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Dieser Befehl ist nur fuer Spieler.");
            return true;
        }
        Player player = (Player)sender;
        switch (command.getName().toLowerCase()) {
            case "navigator": {
                this.openNavigator(player);
                break;
            }
            case "lobby": {
                if (this.activeDuelPlayers.contains(player.getUniqueId())) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Verlasse zuerst deine laufende Runde.");
                    return true;
                }
                if (this.profiles.getOrDefault(player.getUniqueId(), "lobby").equals("smp")) {
                    if (this.isInSmpCombat(player)) {
                        player.sendMessage(String.valueOf(ChatColor.RED) + "Du bist noch im Combat. Warte, bis der Combat-Timer abgelaufen ist.");
                        break;
                    }
                    this.scheduleSmpLobbyTeleport(player);
                    break;
                }
                this.switchProfile(player, "lobby");
                if (this.teleportToLobby(player)) break;
                player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Noch kein Lobby-Spawn gesetzt. Nutze /duelladmin setspawn haupt.");
                break;
            }
            case "smp": {
                if (this.activeDuelPlayers.contains(player.getUniqueId())) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Verlasse zuerst deine laufende Runde.");
                    return true;
                }
                this.switchProfile(player, "smp");
                BauSmp.title(player, "smp");
                if (this.teleportToLastSmpLocation(player)) break;
                this.teleportToSpawn(player, "smp");
                break;
            }
            case "combat": {
                long remaining = this.combatRemainingSeconds(player);
                if (remaining > 0L) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Du bist noch " + remaining + " Sekunden im Combat.");
                    break;
                }
                player.sendMessage(String.valueOf(ChatColor.GREEN) + "Du bist nicht im Combat.");
                break;
            }
            case "home": {
                this.handleHome(player, args);
                break;
            }
            case "sethome": {
                if (args.length == 0) {
                    player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Nutze: /sethome <Name>");
                    break;
                }
                this.handleHome(player, new String[]{args[0], "set"});
                break;
            }
            case "delhome": {
                if (args.length == 0) {
                    player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Nutze: /delhome <Name>");
                    break;
                }
                this.handleHome(player, new String[]{args[0], "delete"});
                break;
            }
            case "tpa": {
                this.handleTpa(player, args);
                break;
            }
            case "tpahere": {
                this.handleTpaHere(player, args);
                break;
            }
            case "party": {
                this.handleParty(player, args);
                break;
            }
            case "kit": {
                this.handleKitCommand(player, args);
                break;
            }
            case "ban": {
                this.handleBan(player, args);
                break;
            }
            case "kick": {
                this.handleKick(player, args);
                break;
            }
            case "unban": {
                this.handleUnban(player, args);
                break;
            }
            case "rank": {
                this.handleRank(player, args);
                break;
            }
            case "battlepass": {
                if (args.length > 0 && args[0].equalsIgnoreCase("rewards")) {
                }
                break;
            }
            case "duel": {
                this.handleDuel(player, args);
                break;
            }
            case "events": {
                if (!EventGames.command(player, args)) {
                    Events.open(this, player);
                }
                break;
            }
            case "parkour": {
                this.handleParkourCommand(player, args);
                break;
            }
            case "bausmp": {
                BauSmp.enter(this, player);
                break;
            }
            case "duelladmin": {
                this.handleAdmin(player, args);
            }
        }
        return true;
    }

    private boolean isOwner(Player player) {
        return player != null && "owner".equalsIgnoreCase(this.playerRanks.getOrDefault(player.getUniqueId(), ""));
    }

    boolean isProtectedStaff(OfflinePlayer player) {
        if (player == null) {
            return false;
        }
        String rank = this.playerRanks.getOrDefault(player.getUniqueId(), this.getConfig().getString("ranks." + String.valueOf(player.getUniqueId()), ""));
        return rank.equalsIgnoreCase("owner") || rank.equalsIgnoreCase("admin");
    }

    boolean mayModerate(Player player) {
        return player != null && (player.isOp() || this.hasAdminRank(player) || player.hasPermission("duelnetwork.admin"));
    }

    private void handleBan(Player sender, String[] args) {
        if (!this.mayModerate(sender)) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Daf\u00fcr brauchst du Admin- oder Owner-Rechte.");
            return;
        }
        this.moderation.ban((CommandSender)sender, args);
    }

    private void handleKick(Player sender, String[] args) {
        if (!this.mayModerate(sender)) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Daf\u00fcr brauchst du Admin- oder Owner-Rechte.");
            return;
        }
        this.moderation.kick((CommandSender)sender, args);
    }

    private void handleUnban(Player sender, String[] args) {
        if (!this.mayModerate(sender)) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Daf\u00fcr brauchst du Admin- oder Owner-Rechte.");
            return;
        }
        this.moderation.unban((CommandSender)sender, args);
    }

    @EventHandler
    public void onModerationCommandGuard(PlayerCommandPreprocessEvent event) {
        OfflinePlayer target;
        String raw = event.getMessage().trim();
        if (!raw.startsWith("/")) {
            return;
        }
        String[] parts = raw.substring(1).split("\\s+");
        if (parts.length == 0) {
            return;
        }
        String command = parts[0].toLowerCase().replace("minecraft:", "");
        if (!command.equals("ban") && !command.equals("kick")) {
            return;
        }
        if (command.equals("ban") && parts.length < 3) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Nutze: /ban <Spieler> [Dauer] <Grund>");
            return;
        }
        // nur Cache-Abfrage: getOfflinePlayer(String) kann im Haupt-Thread eine Mojang-Anfrage ausloesen
        target = parts.length >= 2 ? this.moderation.cached(parts[1]) : null;
        if (target != null && this.isProtectedStaff(target)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(String.valueOf(ChatColor.RED) + "Owner und Admins k\u00f6nnen nicht " + (command.equals("ban") ? "gebannt" : "gekickt") + " werden.");
        }
    }

    @EventHandler
    public void onProtectedKick(PlayerKickEvent event) {
        if (this.isProtectedStaff((OfflinePlayer)event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(String.valueOf(ChatColor.RED) + "Owner und Admins k\u00f6nnen nicht gekickt werden.");
        }
    }

    private void handleRank(Player sender, String[] args) {
        String rank;
        if (!this.mayManageRanks(sender)) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Nur OPs oder Spieler mit Admin-/Owner-Rang d\u00fcrfen R\u00e4nge vergeben.");
            return;
        }
        if (args.length != 2) {
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Nutze: /rank <Spieler> <Media|Owner|Admin|Premium|Tester|reset>");
            return;
        }
        // nur Cache-Abfrage: getOfflinePlayer(String) kann im Haupt-Thread eine Mojang-Anfrage ausloesen
        OfflinePlayer target = this.getServer().getOfflinePlayerIfCached(args[0]);
        if (target == null) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Spieler unbekannt (er muss schon einmal auf dem Server gewesen sein).");
            return;
        }
        UUID id = target.getUniqueId();
        String path = "ranks." + String.valueOf(id);
        String currentRank = this.playerRanks.getOrDefault(id, this.getConfig().getString(path, ""));
        if (args[1].equalsIgnoreCase("reset")) {
            if (currentRank.equalsIgnoreCase("owner")) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Der Owner-Rang kann nicht entfernt werden.");
                return;
            }
            if (currentRank.equalsIgnoreCase("admin") && !this.isOwner(sender)) {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Nur der Owner darf einem Admin den Rang entfernen.");
                return;
            }
            this.playerRanks.remove(id);
            this.getConfig().set(path, null);
            this.saveConfig();
            Player online = this.getServer().getPlayer(id);
            if (online != null) {
                this.applyRankDisplay(online);
                this.applyStaffPermissions(online);
            }
            this.updateTabOrder();
            sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Rang von " + args[0] + " wurde entfernt.");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "media": rank = "Media"; break;
            case "owner": rank = "Owner"; break;
            case "admin": rank = "Admin"; break;
            case "premium": rank = "Premium"; break;
            case "tester": rank = "Tester"; break;
            default: rank = "";
        }
        if (rank.isBlank()) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Erlaubt sind: Media, Owner, Admin, Premium, Tester oder reset.");
            return;
        }
        this.playerRanks.put(id, rank);
        this.getConfig().set(path, (Object)rank);
        this.saveConfig();
        Player online = this.getServer().getPlayer(id);
        if (online != null) {
            this.applyRankDisplay(online);
            this.applyStaffPermissions(online);
        }
        for (Player viewer : this.getServer().getOnlinePlayers()) {
            this.applyRankDisplay(viewer);
        }
        this.updateTabOrder();
        sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Rang gesetzt: " + args[0] + " -> " + rank);
    }

    private String rankPrefix(UUID id) {
        String rank = this.playerRanks.get(id);
        if (rank == null) {
            return "";
        }
        return switch (rank.toLowerCase()) {
            case "media" -> String.valueOf(ChatColor.DARK_GRAY) + "[" + String.valueOf(ChatColor.LIGHT_PURPLE) + String.valueOf(ChatColor.BOLD) + "MEDIA" + String.valueOf(ChatColor.DARK_GRAY) + "] " + String.valueOf(ChatColor.RESET);
            case "owner" -> String.valueOf(ChatColor.DARK_GRAY) + "[" + String.valueOf(ChatColor.AQUA) + String.valueOf(ChatColor.BOLD) + "OWNER" + String.valueOf(ChatColor.DARK_GRAY) + "] " + String.valueOf(ChatColor.RESET);
            case "admin" -> String.valueOf(ChatColor.DARK_GRAY) + "[" + String.valueOf(ChatColor.RED) + String.valueOf(ChatColor.BOLD) + "ADMIN" + String.valueOf(ChatColor.DARK_GRAY) + "] " + String.valueOf(ChatColor.RESET);
            case "premium" -> String.valueOf(ChatColor.DARK_GRAY) + "[" + String.valueOf(ChatColor.GOLD) + String.valueOf(ChatColor.BOLD) + "PREMIUM" + String.valueOf(ChatColor.DARK_GRAY) + "] " + String.valueOf(ChatColor.RESET);
            case "tester" -> String.valueOf(ChatColor.DARK_GRAY) + "[" + String.valueOf(ChatColor.GREEN) + String.valueOf(ChatColor.BOLD) + "TESTER" + String.valueOf(ChatColor.DARK_GRAY) + "] " + String.valueOf(ChatColor.RESET);
            default -> "";
        };
    }

    void applyRankDisplay(Player player) {
        String rank;
        String prefix = this.rankPrefix(player.getUniqueId());
        String legacyName = prefix + String.valueOf(ChatColor.WHITE) + player.getName();
        player.setDisplayName(legacyName);
        player.setPlayerListName(this.partyTabSymbol(player) + legacyName);
        player.customName((Component)LegacyComponentSerializer.legacySection().deserialize(legacyName));
        player.setCustomNameVisible(true);
        String teamName = switch (rank = this.playerRanks.getOrDefault(player.getUniqueId(), "default").toLowerCase()) {
            case "owner" -> "dn_owner";
            case "admin" -> "dn_admin";
            case "media" -> "dn_media";
            case "premium" -> "dn_premium";
            case "tester" -> "dn_tester";
            default -> "dn_default";
        };
        ArrayList<Scoreboard> scoreboards = new ArrayList<Scoreboard>();
        scoreboards.add(Bukkit.getScoreboardManager().getMainScoreboard());
        for (Player viewer : this.getServer().getOnlinePlayers()) {
            if (scoreboards.contains(viewer.getScoreboard())) continue;
            scoreboards.add(viewer.getScoreboard());
        }
        for (Scoreboard scoreboard : scoreboards) {
            for (String oldTeamName : List.of("dn_owner", "dn_admin", "dn_media", "dn_premium", "dn_tester", "dn_default")) {
                Team oldTeam = scoreboard.getTeam(oldTeamName);
                if (oldTeam == null) continue;
                oldTeam.removeEntry(player.getName());
            }
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            team.setPrefix(prefix);
            team.setColor(ChatColor.WHITE);
            team.addEntry(player.getName());
        }
    }

    private int rankPriority(UUID id) {
        String rank = this.playerRanks.get(id);
        if (rank == null) {
            return 400;
        }
        return switch (rank.toLowerCase()) {
            case "owner" -> 0;
            case "admin" -> 100;
            case "media" -> 200;
            case "premium" -> 300;
            case "tester" -> 350;
            default -> 400;
        };
    }

    private final Map<UUID, String> partyFooterCache = new ConcurrentHashMap<UUID, String>();
    private final Map<UUID, String> tabNameCache = new ConcurrentHashMap<UUID, String>();

    /** Kleines Symbol vor dem Namen in der Tab-Liste, wenn der Spieler in einer Party ist. */
    private String partyTabSymbol(Player player) {
        return this.playerParties.containsKey(player.getUniqueId()) ? "\u00a7d\u2726 " : "";
    }

    /** Aktualisiert Tab-Namen (Symbol) und Party-Zeile im Tab-Footer fuer alle Spieler. */
    private void refreshPartyTab() {
        try {
            this.partyFooterCache.keySet().removeIf(known -> this.getServer().getPlayer(known) == null);
            this.tabNameCache.keySet().removeIf(known -> this.getServer().getPlayer(known) == null);
            for (Player player : this.getServer().getOnlinePlayers()) {
                UUID id = player.getUniqueId();
                String party = this.playerParties.get(id);
                String legacy = this.rankPrefix(id) + String.valueOf(ChatColor.WHITE) + player.getName();
                String tabName = this.partyTabSymbol(player) + legacy;
                if (!tabName.equals(this.tabNameCache.put(id, tabName))) {
                    player.setPlayerListName(tabName);
                }
                String names = "";
                if (party != null) {
                    StringBuilder sb = new StringBuilder();
                    for (UUID member : this.partyMembers.getOrDefault(party, Set.of())) {
                        Player online = this.getServer().getPlayer(member);
                        if (online == null) continue;
                        if (sb.length() > 0) {
                            sb.append("\u00a77, ");
                        }
                        sb.append("\u00a7f").append(online.getName());
                        if (member.equals(this.partyLeaders.get(party))) {
                            sb.append(" \u00a76\u2605");
                        }
                    }
                    names = sb.toString();
                }
                int count = this.getServer().getOnlinePlayers().size();
                String key = names + "|" + count;
                String last = this.partyFooterCache.get(id);
                if (key.equals(last)) continue;
                this.partyFooterCache.put(id, key);
                String footer = "\n\u00a77Viel Spa\u00df auf \u00a7d\u00a7lLowBlock\u00a7r\u00a77!\n";
                if (!names.isEmpty()) {
                    footer = "\n\u00a7d\u2726 Party: " + names + "\n" + footer.substring(1);
                }
                player.setPlayerListHeaderFooter("\n" + ParkourEnhancements.BANNER + "\n\u00a77Dein Server f\u00fcr Duels, Parkour & mehr\n\u00a77Spieler online: \u00a7d" + count + "\n", footer);
            }
        }
        catch (Throwable ignored) {
            // Tab-Anzeige ist nur Kosmetik
            Store.problem("DuelNetworkPlugin:1043", ignored);
        }
    }

    private void updateTabOrder() {
        ArrayList<Player> players = new ArrayList<Player>(this.getServer().getOnlinePlayers());
        players.sort(Comparator.comparingInt((Player p) -> this.rankPriority(p.getUniqueId())).thenComparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        for (int index = 0; index < players.size(); ++index) {
            ((Player)players.get(index)).setPlayerListOrder(index);
        }
    }

    private boolean hasAdminRank(Player player) {
        String rank = this.playerRanks.get(player.getUniqueId());
        return rank != null && List.of("admin", "owner").contains(rank.toLowerCase());
    }

    private boolean mayManageRanks(Player player) {
        return player.isOp() || this.hasAdminRank(player);
    }

    private void applyStaffPermissions(Player player) {
        UUID id = player.getUniqueId();
        PermissionAttachment previous = this.staffAttachments.remove(id);
        if (previous != null) {
            player.removeAttachment(previous);
        }
        boolean owner = "owner".equalsIgnoreCase(this.playerRanks.getOrDefault(id, ""));
        boolean admin = this.hasAdminRank(player);
        // ranks.owner-grants-op: false = Owner bekommt kein OP, sondern nur die Plugin-Rechte (sicherer)
        boolean grantOp = this.getConfig().getBoolean("ranks.owner-grants-op", false);
        if (owner && grantOp && !player.isOp()) {
            player.setOp(true);
            this.getConfig().set("players." + String.valueOf(id) + ".pluginGrantedOp", (Object)true);
            this.saveConfig();
        } else if ((!owner || !grantOp) && this.getConfig().getBoolean("players." + String.valueOf(id) + ".pluginGrantedOp", false)) {
            player.setOp(false);
            this.getConfig().set("players." + String.valueOf(id) + ".pluginGrantedOp", null);
            this.saveConfig();
            this.getLogger().info("OP-Status von " + player.getName() + " entfernt (vom Plugin vergeben, ranks.owner-grants-op ist aus). Team-Rechte laufen jetzt ueber Permissions.");
        }
        if (admin) {
            PermissionAttachment attachment = player.addAttachment((Plugin)this);
            attachment.setPermission("duelnetwork.admin", true);
            attachment.setPermission("minecraft.command.kick", true);
            attachment.setPermission("minecraft.command.ban", true);
            attachment.setPermission("bukkit.command.kick", true);
            attachment.setPermission("bukkit.command.ban", true);
            if (owner) {
                // Owner ist nicht mehr automatisch OP - die haeufigsten Vanilla-Befehle gibt es deshalb ueber Permissions
                for (String node : List.of("gamemode", "give", "tp", "teleport", "time", "weather", "kill", "effect", "enchant", "xp", "clear", "summon", "setblock", "fill", "whitelist", "difficulty", "gamerule", "say", "list", "pardon", "banlist")) {
                    attachment.setPermission("minecraft.command." + node, true);
                }
            }
            this.staffAttachments.put(id, attachment);
        }
    }

    void saveLastSmpLocation(Player player) {
        if (!this.profiles.getOrDefault(player.getUniqueId(), "lobby").equals("smp")) {
            return;
        }
        if (this.insideBauBorder(player.getLocation())) {
            return; // im Bau-SMP: der letzte SMP-Ort bleibt der aus dem normalen SMP
        }
        this.saveLocation("players." + String.valueOf(player.getUniqueId()) + ".lastSmp", player.getLocation());
    }

    boolean teleportToLastSmpLocation(Player player) {
        String path = "players." + String.valueOf(player.getUniqueId()) + ".lastSmp";
        if (!this.getConfig().contains(path + ".world")) {
            return false;
        }
        Location location = this.readLocation(path);
        if (location.getWorld() == null || this.insideBauBorder(location)) {
            return false; // alter Eintrag aus dem Bau-SMP -> normalen SMP-Spawn nehmen
        }
        player.teleport(location);
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Du bist wieder an deinem letzten SMP-Ort.");
        return true;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private boolean hasFiveHomes(Player player) {
        String rank = this.playerRanks.getOrDefault(player.getUniqueId(), "");
        if (player.hasPermission("duelnetwork.admin")) return true;
        if (rank.equalsIgnoreCase("premium")) return true;
        if (rank.equalsIgnoreCase("admin")) return true;
        if (rank.equalsIgnoreCase("owner")) return true;
        if (rank.equalsIgnoreCase("media")) return true;
        if (rank.equalsIgnoreCase("tester")) return true;
        if (!false) return false;
        return true;
    }

    /** Migriert alte, gemeinsame Homes genau einmal in den normalen SMP-Bereich. */
    private void migrateLegacyHomes(Player player) {
        String playerPath = "players." + player.getUniqueId();
        String versionPath = playerPath + ".homes-data-version";
        if (this.getConfig().getInt(versionPath, 0) >= 1) {
            return;
        }
        String legacyRoot = playerPath + ".homes";
        String smpRoot = playerPath + ".mode-homes.smp";
        List<String> legacyNames = new ArrayList<String>(this.getConfig().getStringList(legacyRoot + ".list"));
        List<String> smpNames = new ArrayList<String>(this.getConfig().getStringList(smpRoot + ".list"));
        List<String> remainingLegacyNames = new ArrayList<String>();
        for (String storedName : legacyNames) {
            String name = storedName.toLowerCase(java.util.Locale.ROOT);
            if (!name.matches("[a-z0-9_-]{1,16}")) {
                remainingLegacyNames.add(storedName);
                continue;
            }
            if (!smpNames.contains(name)) {
                smpNames.add(name);
            }
            String destination = smpRoot + "." + name;
            ConfigurationSection oldHome = this.getConfig().getConfigurationSection(legacyRoot + "." + storedName);
            if (oldHome != null && !this.getConfig().contains(destination + ".world")) {
                for (String key : oldHome.getKeys(true)) {
                    if (!oldHome.isConfigurationSection(key)) {
                        this.getConfig().set(destination + "." + key, oldHome.get(key));
                    }
                }
            }
            this.getConfig().set(legacyRoot + "." + storedName, null);
        }
        this.getConfig().set(smpRoot + ".list", smpNames);
        this.getConfig().set(legacyRoot + ".list", remainingLegacyNames.isEmpty() ? null : remainingLegacyNames);
        this.getConfig().set(versionPath, 1);
        this.saveConfig();
    }

    /** Mode-spezifischer Speicherpfad; alte Homes werden bei der ersten Nutzung dem SMP zugeordnet. */
    private String homeDataPath(Player player) {
        this.migrateLegacyHomes(player);
        String mode = this.inBauMode(player.getUniqueId()) ? "bau" : "smp";
        return "players." + player.getUniqueId() + ".mode-homes." + mode;
    }

    private String homeListPath(Player player) {
        return this.homeDataPath(player) + ".list";
    }

    private boolean homeLocationAllowed(Player player, Location location) {
        return this.inBauMode(player.getUniqueId()) ? this.insideBauBorder(location) : this.isInsideSmpBorder(location);
    }

    private static Material mat(String name, Material fallback) {
        Material m = Material.matchMaterial(name);
        return m == null ? fallback : m;
    }

    private static String dimensionLabel(Location location) {
        String env = String.valueOf(location.getWorld().getEnvironment());
        return env.equals("NETHER") ? "§cNether" : env.equals("THE_END") ? "§5End" : "§aOberwelt";
    }

    private void openHomeMenu(Player player) {
        if (!this.profiles.getOrDefault(player.getUniqueId(), "lobby").equals("smp")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Homes kannst du nur im SMP benutzen.");
            return;
        }
        Inventory inv = Gui.frame(this, "Homes", this.inBauMode(player.getUniqueId()) ? "Bau-SMP" : "SMP", 27);
        List homes = this.getConfig().getStringList(this.homeListPath(player));
        int max = this.hasFiveHomes(player) ? 5 : 3;
        int[] slots = new int[]{11, 12, 13, 14, 15};
        String[] beds = {"RED_BED", "ORANGE_BED", "YELLOW_BED", "LIME_BED", "LIGHT_BLUE_BED"};
        inv.setItem(4, Gui.item(mat("NETHER_STAR", Material.BOOK), "§d§l✦ Deine Homes", "§7Genutzt: §f" + homes.size() + "§7/§f" + max, "§8Mehr Homes mit Premium, Media, Admin, Owner oder Tester."));
        for (int i = 0; i < slots.length; ++i) {
            if (i < homes.size()) {
                String name = (String)homes.get(i);
                String path = this.homeDataPath(player) + "." + name;
                List<String> lore = new ArrayList<String>();
                try {
                    Location at = this.readLocation(path);
                    if (at.getWorld() != null) {
                        lore.add("§7Welt: " + dimensionLabel(at));
                        lore.add("§7Position: §f" + at.getBlockX() + "§7, §f" + at.getBlockY() + "§7, §f" + at.getBlockZ());
                    }
                }
                catch (Throwable ignored) {
                    // Anzeige
                    Store.problem("DuelNetworkPlugin:1165", ignored);
                }
                lore.add("");
                lore.add("§aLinksklick: §fteleportieren");
                lore.add("§cRechtsklick: §flöschen");
                inv.setItem(slots[i], Gui.item(mat(beds[i], Material.BOOK), 1, "§f§l" + name, lore));
                continue;
            }
            if (i < max) {
                inv.setItem(slots[i], Gui.item(mat("LIGHT_GRAY_BED", Material.BOOK), "§7Freier Home-Slot", "§dKlicken §7und Namen in den Chat schreiben", "§8oder: §f/sethome <Name>"));
                continue;
            }
            inv.setItem(slots[i], Gui.item(mat("BARRIER", Material.BOOK), "§8Gesperrter Slot", "§7Für Premium, Media, Admin, Owner oder Tester"));
        }
        player.openInventory(inv);
    }

    private void openTpaMenu(Player player) {
        if (!this.profiles.getOrDefault(player.getUniqueId(), "lobby").equals("smp")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "TPA kannst du nur im SMP benutzen.");
            return;
        }
        Inventory inv = Gui.frame(this, "TPA", "Teleport", 54);
        UUID incoming = this.tpaRequests.get(player.getUniqueId());
        UUID outgoing = this.outgoingTpaRequests.get(player.getUniqueId());
        boolean busy = outgoing != null || this.delayedTeleportTasks.containsKey(player.getUniqueId());
        inv.setItem(10, incoming == null ? Gui.item(mat("GRAY_DYE", Material.BOOK), "§7Annehmen", "§8Keine Anfrage")
                : Gui.item(mat("LIME_DYE", Material.BOOK), "§a§lAnnehmen", "§7Anfrage von §f" + this.getServer().getOfflinePlayer(incoming).getName()));
        inv.setItem(12, incoming == null ? Gui.item(mat("GRAY_DYE", Material.BOOK), "§7Ablehnen", "§8Keine Anfrage")
                : Gui.item(mat("RED_DYE", Material.BOOK), "§c§lAblehnen", "§7Anfrage ablehnen"));
        inv.setItem(14, busy ? Gui.item(mat("YELLOW_DYE", Material.BOOK), "§e§lAbbrechen", "§7Anfrage oder Teleport abbrechen")
                : Gui.item(mat("GRAY_DYE", Material.BOOK), "§7Abbrechen", "§8Nichts abzubrechen"));
        inv.setItem(16, Gui.item(mat("NETHER_STAR", Material.BOOK), "§d§l✦ Teleport-Anfrage", "§7Klicke unten auf einen Spieler,", "§7um ihm eine Anfrage zu senden."));
        int[] free = new int[]{19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int index = 0;
        for (Player other : this.getServer().getOnlinePlayers()) {
            if (other.equals(player) || !this.profiles.getOrDefault(other.getUniqueId(), "lobby").equals("smp") || index >= free.length) {
                continue;
            }
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.ItemMeta meta = head.getItemMeta();
            meta.setDisplayName("§f" + other.getName());
            meta.setLore(List.of("§7Klicken: §dTeleport-Anfrage senden"));
            if (meta instanceof org.bukkit.inventory.meta.SkullMeta skull) {
                skull.setOwningPlayer(other);
            }
            head.setItemMeta(meta);
            inv.setItem(free[index++], head);
        }
        if (index == 0) {
            inv.setItem(31, Gui.item(mat("BARRIER", Material.BOOK), "§7Kein anderer Spieler im SMP", "§8Hier erscheinen alle Spieler, die gerade im SMP sind."));
        }
        player.openInventory(inv);
    }

    private void handleHome(Player player, String[] args) {
        String name;
        if (!this.profiles.getOrDefault(player.getUniqueId(), "lobby").equals("smp")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Homes kannst du nur im SMP benutzen.");
            return;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            this.openHomeMenu(player);
            return;
        }
        if (args.length >= 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("delete"))) {
            args = new String[]{args[1], args[0]};
        }
        if (!(name = args[0].toLowerCase()).matches("[a-z0-9_-]{1,16}")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Ung\u00fcltiger Home-Name. Erlaubt: 1\u201316 Zeichen (a-z, 0-9, _, -).");
            return;
        }
        String path = this.homeDataPath(player) + "." + name;
        if (args.length >= 2 && args[1].equalsIgnoreCase("delete")) {
            this.getConfig().set(path, null);
            List homes = this.getConfig().getStringList(this.homeListPath(player));
            homes.remove(name);
            this.getConfig().set(this.homeListPath(player), (Object)homes);
            this.saveConfig();
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Home gel\u00f6scht: " + name);
            return;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("set")) {
            if (!this.homeLocationAllowed(player, player.getLocation())) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Homes koennen nur innerhalb des zugehoerigen SMP-Bereichs gesetzt werden.");
                return;
            }
            List homes = this.getConfig().getStringList(this.homeListPath(player));
            int maxHomes = this.hasFiveHomes(player) ? 5 : 3;
            int n = maxHomes;
            if (!homes.contains(name) && homes.size() >= maxHomes) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Du kannst maximal " + maxHomes + " Homes haben.");
                return;
            }
            if (!homes.contains(name)) {
                homes.add(name);
            }
            this.getConfig().set(this.homeListPath(player), (Object)homes);
            this.saveLocation(path, player.getLocation());
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Home gesetzt: " + name);
            return;
        }
        if (!this.getConfig().contains(path + ".world")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Home nicht gefunden. Nutze /home " + name + " set.");
            return;
        }
        Location location = this.readLocation(path);
        if (location.getWorld() == null) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Die Welt dieses Homes existiert nicht mehr.");
            return;
        }
        if (!this.homeLocationAllowed(player, location)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Dieses Home liegt ausserhalb des zugehoerigen SMP-Bereichs.");
            return;
        }
        this.scheduleDelayedTeleport(player, location, "Home: " + name);
    }

    private void scheduleSmpLobbyTeleport(Player player) {
        this.cancelDelayedTeleport(player, false);
        int delaySeconds = Math.max(1, this.getConfig().getInt("smp.home-teleport-delay", 3));
        player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Lobby-Teleport in " + delaySeconds + " Sekunden. Bewege dich nicht.");
        int task = this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
            this.delayedTeleportTasks.remove(player.getUniqueId());
            if (!player.isOnline() || !this.profiles.getOrDefault(player.getUniqueId(), "lobby").equals("smp") || this.isInSmpCombat(player)) {
                if (player.isOnline()) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Lobby-Teleport abgebrochen.");
                }
                return;
            }
            this.saveLastSmpLocation(player);
            this.switchProfile(player, "lobby");
            if (!this.teleportToLobby(player)) {
                player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Noch kein Lobby-Spawn gesetzt.");
            }
        }, (long)delaySeconds * 20L).getTaskId();
        this.delayedTeleportTasks.put(player.getUniqueId(), task);
    }

    void scheduleDelayedTeleport(Player player, Location location, String label) {
        this.cancelDelayedTeleport(player, false);
        int delaySeconds = Math.max(0, this.getConfig().getInt("smp.home-teleport-delay", 3));
        player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + label + " in " + delaySeconds + " Sekunden. Bewege dich nicht.");
        Chat.click(player);
        int task = this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
            this.delayedTeleportTasks.remove(player.getUniqueId());
            if (!this.profiles.getOrDefault(player.getUniqueId(), "lobby").equals("smp")) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Teleport abgebrochen: Du bist nicht mehr im SMP.");
                return;
            }
            if (this.isInSmpCombat(player)) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Teleport abgebrochen: Du bist noch im Combat.");
                return;
            }
            if (location.getWorld() == null || !this.isInsideSmpBorder(location)) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Teleport abgebrochen: Das Ziel liegt au\u00dferhalb der SMP-WorldBorder.");
                return;
            }
            Chat.warp(player);
            player.teleport(location);
            Chat.warp(player);
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Teleportiert zu " + label + ".");
        }, (long)delaySeconds * 20L).getTaskId();
        this.delayedTeleportTasks.put(player.getUniqueId(), task);
    }

    private void cancelDelayedTeleport(Player player, boolean notify) {
        Integer task = this.delayedTeleportTasks.remove(player.getUniqueId());
        if (task != null) {
            this.getServer().getScheduler().cancelTask(task.intValue());
            if (notify) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Teleport abgebrochen.");
                Chat.no(player);
            }
        }
    }

    private void clearTpaExpiry(UUID targetId) {
        Integer task = this.tpaExpiryTasks.remove(targetId);
        if (task != null) {
            this.getServer().getScheduler().cancelTask(task.intValue());
        }
    }

    private void handleTpa(Player player, String[] args) {
        if (!this.profiles.getOrDefault(player.getUniqueId(), "lobby").equals("smp")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "TPA kannst du nur im SMP benutzen.");
            return;
        }
        if (args.length == 0) {
            this.openTpaMenu(player);
            return;
        }
        if ((args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny")) && !this.tpaRequests.containsKey(player.getUniqueId()) && this.tpahereRequests.containsKey(player.getUniqueId())) {
            this.answerTpaHere(player, args[0].equalsIgnoreCase("accept"));
            return;
        }
        if (args[0].equalsIgnoreCase("deny")) {
            UUID requesterId = this.tpaRequests.remove(player.getUniqueId());
            this.clearTpaExpiry(player.getUniqueId());
            if (requesterId == null) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Du hast keine offene TPA-Anfrage.");
                return;
            }
            this.outgoingTpaRequests.remove(requesterId, player.getUniqueId());
            Player requester = this.getServer().getPlayer(requesterId);
            if (requester != null) {
                requester.sendMessage(String.valueOf(ChatColor.RED) + player.getName() + " hat deine TPA-Anfrage abgelehnt.");
            }
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "TPA-Anfrage abgelehnt.");
            return;
        }
        if (args[0].equalsIgnoreCase("cancel")) {
            UUID requesterId;
            UUID targetId = this.outgoingTpaRequests.remove(player.getUniqueId());
            if (targetId != null) {
                this.tpaRequests.remove(targetId, player.getUniqueId());
                this.clearTpaExpiry(targetId);
            }
            if ((requesterId = this.tpaRequests.remove(player.getUniqueId())) != null) {
                this.clearTpaExpiry(player.getUniqueId());
                this.outgoingTpaRequests.remove(requesterId, player.getUniqueId());
                Player requester = this.getServer().getPlayer(requesterId);
                if (requester != null) {
                    requester.sendMessage(String.valueOf(ChatColor.RED) + player.getName() + " hat die TPA-Anfrage abgelehnt.");
                    Chat.no(requester);
                }
            }
            this.cancelDelayedTeleport(player, false);
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "TPA-Anfrage oder Teleport abgebrochen.");
            return;
        }
        if (args[0].equalsIgnoreCase("accept")) {
            UUID requesterId = this.tpaRequests.remove(player.getUniqueId());
            this.clearTpaExpiry(player.getUniqueId());
            if (requesterId == null) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Du hast keine offene TPA-Anfrage.");
                return;
            }
            this.outgoingTpaRequests.remove(requesterId, player.getUniqueId());
            Player requester = this.getServer().getPlayer(requesterId);
            if (requester == null || !this.profiles.getOrDefault(requesterId, "lobby").equals("smp")) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Der anfragende Spieler ist nicht mehr im SMP.");
                return;
            }
            if (this.isInSmpCombat(requester)) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Die TPA wurde abgelehnt, weil der anfragende Spieler noch im Combat ist.");
                requester.sendMessage(String.valueOf(ChatColor.RED) + "TPA abgebrochen: Du bist noch im Combat.");
                return;
            }
            this.scheduleDelayedTeleport(requester, player.getLocation(), "TPA zu " + player.getName());
            requester.sendMessage(String.valueOf(ChatColor.GREEN) + player.getName() + " hat deine TPA-Anfrage angenommen.");
            Chat.yes(requester);
            Chat.yes(player);
            return;
        }
        Player target = this.getServer().getPlayerExact(args[0]);
        if (target == null || !target.isOnline() || !this.profiles.getOrDefault(target.getUniqueId(), "lobby").equals("smp")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Dieser Spieler ist nicht online oder nicht im SMP.");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Du kannst dich nicht selbst anfragen.");
            return;
        }
        if (Social.tpOff(target.getUniqueId())) {
            player.sendMessage(String.valueOf(ChatColor.RED) + target.getName() + " nimmt gerade keine Teleport-Anfragen an.");
            return;
        }
        UUID oldTarget = this.outgoingTpaRequests.put(player.getUniqueId(), target.getUniqueId());
        if (oldTarget != null) {
            this.tpaRequests.remove(oldTarget, player.getUniqueId());
            this.clearTpaExpiry(oldTarget);
        }
        this.clearTpaExpiry(target.getUniqueId());
        this.tpaRequests.put(target.getUniqueId(), player.getUniqueId());
        int expiry = Math.max(5, this.getConfig().getInt("smp.tpa-expire-seconds", 60));
        int expiryTask = this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
            UUID requesterId = this.tpaRequests.remove(target.getUniqueId(), player.getUniqueId()) ? player.getUniqueId() : null;
            this.tpaExpiryTasks.remove(target.getUniqueId());
            if (requesterId != null) {
                this.outgoingTpaRequests.remove(requesterId, target.getUniqueId());
                if (player.isOnline()) {
                    player.sendMessage(String.valueOf(ChatColor.GRAY) + "Deine TPA-Anfrage an " + target.getName() + " ist abgelaufen.");
                }
                if (target.isOnline()) {
                    target.sendMessage(String.valueOf(ChatColor.GRAY) + "Die TPA-Anfrage von " + player.getName() + " ist abgelaufen.");
                }
            }
        }, (long)expiry * 20L).getTaskId();
        this.tpaExpiryTasks.put(target.getUniqueId(), expiryTask);
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "TPA-Anfrage an " + target.getName() + " gesendet.");
        Chat.request(target, "§5✦ §d" + player.getName() + " §7möchte sich zu dir teleportieren.", "tpa accept", "§7Anfrage annehmen", "tpa deny", "§7Anfrage ablehnen");
        Chat.ping(target);
        Chat.click(player);
    }

    /** Mitglieder der Party des Spielers (leer, wenn in keiner Party). */
    Set<UUID> partyMembersOf(UUID id) {
        String party = this.playerParties.get(id);
        return party == null ? Set.of() : this.partyMembers.getOrDefault(party, Set.of());
    }

    /** /tpahere <Spieler>: bittet einen anderen SMP-Spieler, sich zu dir zu teleportieren (Annahme mit /tpa accept). */
    private void handleTpaHere(Player player, String[] args) {
        if (!this.profiles.getOrDefault(player.getUniqueId(), "lobby").equals("smp")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "TPAHERE kannst du nur im SMP benutzen.");
            return;
        }
        if (args.length == 0) {
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Nutze: /tpahere <Spieler> " + String.valueOf(ChatColor.GRAY) + "(der Spieler teleportiert sich dann zu dir)");
            return;
        }
        Player target = this.getServer().getPlayerExact(args[0]);
        if (target == null || !target.isOnline() || !this.profiles.getOrDefault(target.getUniqueId(), "lobby").equals("smp")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Dieser Spieler ist nicht online oder nicht im SMP.");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Du kannst dich nicht selbst anfragen.");
            return;
        }
        if (Social.tpOff(target.getUniqueId())) {
            player.sendMessage(String.valueOf(ChatColor.RED) + target.getName() + " nimmt gerade keine Teleport-Anfragen an.");
            return;
        }
        if (this.isInSmpCombat(player)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Im Combat kannst du niemanden zu dir holen.");
            return;
        }
        if (this.inBauMode(player.getUniqueId()) != this.inBauMode(target.getUniqueId())) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Ihr seid nicht im gleichen Modus (SMP / Bau-SMP).");
            return;
        }
        UUID requesterId = player.getUniqueId();
        this.tpahereRequests.put(target.getUniqueId(), requesterId);
        int expiry = Math.max(5, this.getConfig().getInt("smp.tpa-expire-seconds", 60));
        this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
            if (this.tpahereRequests.remove(target.getUniqueId(), requesterId)) {
                if (player.isOnline()) {
                    player.sendMessage(String.valueOf(ChatColor.GRAY) + "Deine TPAHERE-Anfrage an " + target.getName() + " ist abgelaufen.");
                }
                if (target.isOnline()) {
                    target.sendMessage(String.valueOf(ChatColor.GRAY) + "Die TPAHERE-Anfrage von " + player.getName() + " ist abgelaufen.");
                }
            }
        }, (long)expiry * 20L);
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "TPAHERE-Anfrage an " + target.getName() + " gesendet.");
        Chat.request(target, "§5✦ §d" + player.getName() + " §7möchte, dass du dich zu ihm teleportierst.", "tpa accept", "§7Anfrage annehmen", "tpa deny", "§7Anfrage ablehnen");
        Chat.ping(target);
        Chat.click(player);
    }

    private void answerTpaHere(Player target, boolean accept) {
        UUID requesterId = this.tpahereRequests.remove(target.getUniqueId());
        Player requester = requesterId == null ? null : this.getServer().getPlayer(requesterId);
        if (requester == null) {
            target.sendMessage(String.valueOf(ChatColor.RED) + "Der Spieler ist nicht mehr online.");
            return;
        }
        if (!accept) {
            requester.sendMessage(String.valueOf(ChatColor.RED) + target.getName() + " hat deine TPAHERE-Anfrage abgelehnt.");
            target.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "TPAHERE-Anfrage abgelehnt.");
            return;
        }
        if (!this.profiles.getOrDefault(requesterId, "lobby").equals("smp") || this.inBauMode(requester.getUniqueId()) != this.inBauMode(target.getUniqueId())) {
            target.sendMessage(String.valueOf(ChatColor.RED) + "Der anfragende Spieler ist nicht mehr im gleichen Modus.");
            return;
        }
        if (this.isInSmpCombat(target)) {
            target.sendMessage(String.valueOf(ChatColor.RED) + "Du bist noch im Combat und kannst dich nicht teleportieren.");
            requester.sendMessage(String.valueOf(ChatColor.RED) + "TPAHERE abgebrochen: " + target.getName() + " ist noch im Combat.");
            return;
        }
        this.scheduleDelayedTeleport(target, requester.getLocation(), "TPA zu " + requester.getName());
        requester.sendMessage(String.valueOf(ChatColor.GREEN) + target.getName() + " hat deine TPAHERE-Anfrage angenommen.");
        Chat.yes(requester);
        Chat.yes(target);
    }

    private void handleKitCommand(Player player, String[] args) {
        if (!player.hasPermission("duelnetwork.admin")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Daf\u00fcr brauchst du die Permission duelnetwork.admin.");
            return;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Verf\u00fcgbare Kits: " + this.getConfig().getStringList("kits.available").stream().map(this::displayKitName).collect(Collectors.joining(", ")));
            player.sendMessage(String.valueOf(ChatColor.GRAY) + "Vorschau: /kit <name> | Inventar erhalten: /kit give <name>");
            return;
        }
        boolean give = args[0].equalsIgnoreCase("give");
        int start = give ? 1 : 0;
        int n = start;
        if (args.length <= start) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Nutze: /kit <name> oder /kit give <name>");
            return;
        }
        String kitName = String.join((CharSequence)" ", Arrays.copyOfRange(args, start, args.length)).toLowerCase().replace('_', ' ');
        if (!this.getConfig().getStringList("kits.available").stream().anyMatch(kit -> kit.equalsIgnoreCase(kitName))) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Kit nicht gefunden. Nutze /kit list.");
            return;
        }
        List<ItemStack> stored = this.normalizeStoredKit(kitName, this.getConfig().getList("kits.data." + kitName + ".items", List.of()));
        if (stored == null) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "F\u00fcr dieses Kit sind keine Items gespeichert.");
            return;
        }
        if (give) {
            int added = 0;
            for (ItemStack value : stored) {
                ItemStack stack;
                if (!(value instanceof ItemStack) || (stack = value).getType() == Material.AIR) continue;
                player.getInventory().addItem(new ItemStack[]{stack.clone()});
                ++added;
            }
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Kit " + kitName + " ins Inventar gelegt (" + added + " Itemstapel).");
            return;
        }
        Inventory preview = this.getServer().createInventory(null, 54, String.valueOf(ChatColor.DARK_RED) + "Kit Vorschau: " + kitName);
        for (int i = 0; i < Math.min(stored.size(), preview.getSize()); ++i) {
            ItemStack stack;
            ItemStack value = stored.get(i);
            if (!(value instanceof ItemStack) || (stack = value).getType() == Material.AIR) continue;
            preview.setItem(i, stack.clone());
        }
        player.openInventory(preview);
    }

    private static final String PARTY_MENU = "Party-Menü";
    private static final String PARTY_PICK = "Party-Einladen";

    private String partyPrefix() {
        return String.valueOf(ChatColor.DARK_PURPLE) + "[Party] " + String.valueOf(ChatColor.GRAY);
    }

    /** Fuer TeamQueue: Party-ID des Spielers (oder null). */
    String partyOf(UUID id) {
        return this.playerParties.get(id);
    }

    /** Fuer TeamQueue: online befindliche Mitglieder einer Party. */
    List<Player> partyOnline(String party) {
        List<Player> out = new ArrayList<Player>();
        if (party == null) {
            return out;
        }
        for (UUID member : this.partyMembers.getOrDefault(party, Set.of())) {
            Player online = this.getServer().getPlayer(member);
            if (online != null && online.isOnline()) {
                out.add(online);
            }
        }
        return out;
    }

    boolean partyLeader(Player player) {
        return this.isPartyLeader(player);
    }

    private boolean isPartyLeader(Player player) {
        String party = this.playerParties.get(player.getUniqueId());
        return party != null && player.getUniqueId().equals(this.partyLeaders.get(party));
    }

    private void partyBroadcast(String party, String message, UUID except) {
        Set<UUID> members = this.partyMembers.get(party);
        if (members == null) {
            return;
        }
        for (UUID member : members) {
            Player online = this.getServer().getPlayer(member);
            if (online != null && !member.equals(except)) {
                online.sendMessage(this.partyPrefix() + message);
            }
        }
    }

    private String createParty(Player player) {
        String existing = this.playerParties.get(player.getUniqueId());
        if (existing != null) {
            return existing;
        }
        String party = "party-" + player.getUniqueId().toString().substring(0, 8);
        this.partyLeaders.put(party, player.getUniqueId());
        Set<UUID> members = ConcurrentHashMap.newKeySet();
        members.add(player.getUniqueId());
        this.partyMembers.put(party, members);
        this.playerParties.put(player.getUniqueId(), party);
        return party;
    }

    private void partyInvite(Player player, String targetName) {
        Player target = this.getServer().getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Spieler " + targetName + " ist nicht online.");
            return;
        }
        this.partyInvite(player, target);
    }

    private void partyInvite(Player player, Player target) {
        if (target.equals((Object)player)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Du kannst dich nicht selbst einladen.");
            return;
        }
        String party = this.playerParties.get(player.getUniqueId());
        if (party != null && !this.isPartyLeader(player)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Nur der Party-Leader kann einladen.");
            return;
        }
        if (party != null && this.playerParties.get(target.getUniqueId()) != null && party.equals(this.playerParties.get(target.getUniqueId()))) {
            player.sendMessage(String.valueOf(ChatColor.RED) + target.getName() + " ist schon in deiner Party.");
            return;
        }
        if (this.playerParties.containsKey(target.getUniqueId())) {
            player.sendMessage(String.valueOf(ChatColor.RED) + target.getName() + " ist bereits in einer anderen Party.");
            return;
        }
        String created = this.createParty(player);
        UUID targetId = target.getUniqueId();
        this.partyInvites.put(targetId, created);
        player.sendMessage(this.partyPrefix() + String.valueOf(ChatColor.GREEN) + target.getName() + " wurde eingeladen (60 Sekunden gültig).");
        target.sendMessage(this.partyPrefix() + String.valueOf(ChatColor.LIGHT_PURPLE) + player.getName() + String.valueOf(ChatColor.GRAY) + " lädt dich in seine Party ein.");
        Chat.request(target, this.partyPrefix() + "§7Einladung annehmen?", "party accept", "§7Party beitreten", "party deny", "§7Einladung ablehnen");
        Chat.ping(target);
        Chat.click(player);
        this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
            if (this.partyInvites.remove(targetId, created)) {
                Player t = this.getServer().getPlayer(targetId);
                if (t != null) {
                    t.sendMessage(this.partyPrefix() + "Die Party-Einladung ist abgelaufen.");
                }
            }
        }, 1200L);
    }

    private void partyAccept(Player player) {
        String party = this.partyInvites.remove(player.getUniqueId());
        if (party == null || !this.partyMembers.containsKey(party)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Du hast keine gültige Party-Einladung.");
            return;
        }
        if (this.playerParties.containsKey(player.getUniqueId())) {
            this.leaveParty(player);
            if (this.playerParties.containsKey(player.getUniqueId())) {
                return;
            }
        }
        this.partyBroadcast(party, String.valueOf(ChatColor.GREEN) + player.getName() + " ist der Party beigetreten.", null);
        this.partyMembers.get(party).add(player.getUniqueId());
        this.playerParties.put(player.getUniqueId(), party);
        player.sendMessage(this.partyPrefix() + String.valueOf(ChatColor.GREEN) + "Du bist der Party beigetreten.");
        Chat.yes(player);
        this.refreshMenus(player);
    }

    private void refreshMenus(Player player) {
        MenuItems.refreshNow(player);
    }

    private void partyList(Player player) {
        String party = this.playerParties.get(player.getUniqueId());
        if (party == null) {
            player.sendMessage(this.partyPrefix() + "Du bist in keiner Party. Erstelle eine mit " + String.valueOf(ChatColor.LIGHT_PURPLE) + "/party create" + String.valueOf(ChatColor.GRAY) + " oder nutze das Party-Item.");
            return;
        }
        UUID leader = this.partyLeaders.get(party);
        Set<UUID> members = this.partyMembers.getOrDefault(party, Set.of());
        player.sendMessage(this.partyPrefix() + String.valueOf(ChatColor.LIGHT_PURPLE) + "Party (" + members.size() + "):");
        for (UUID member : members) {
            Player online = this.getServer().getPlayer(member);
            String name = online == null ? this.getServer().getOfflinePlayer(member).getName() : online.getName();
            player.sendMessage(String.valueOf(ChatColor.GRAY) + " • " + String.valueOf(ChatColor.WHITE) + name + (member.equals(leader) ? String.valueOf(ChatColor.GOLD) + " ★ Leader" : "") + (online == null ? String.valueOf(ChatColor.DARK_GRAY) + " (offline)" : ""));
        }
    }

    private Player partyMember(Player leader, String name) {
        String party = this.playerParties.get(leader.getUniqueId());
        Player target = this.getServer().getPlayerExact(name);
        if (party == null || target == null || !this.partyMembers.getOrDefault(party, Set.of()).contains(target.getUniqueId())) {
            leader.sendMessage(String.valueOf(ChatColor.RED) + name + " ist nicht in deiner Party.");
            return null;
        }
        return target;
    }

    private void partyHelp(Player player) {
        String p = String.valueOf(ChatColor.LIGHT_PURPLE);
        String g = String.valueOf(ChatColor.GRAY);
        player.sendMessage(p + "── Party-Hilfe ──");
        player.sendMessage(p + "/party" + g + " – Party-Menü öffnen (oder Party-Item)");
        player.sendMessage(p + "/party create" + g + " – Party erstellen");
        player.sendMessage(p + "/party invite <Spieler>" + g + " – Spieler einladen");
        player.sendMessage(p + "/party accept | deny" + g + " – Einladung annehmen / ablehnen");
        player.sendMessage(p + "/party list" + g + " – Mitglieder anzeigen");
        player.sendMessage(p + "/party kick <Spieler>" + g + " – Mitglied entfernen (Leader)");
        player.sendMessage(p + "/party promote <Spieler>" + g + " – Leader übergeben (Leader)");
        player.sendMessage(p + "/party disband" + g + " – Party auflösen (Leader)");
        player.sendMessage(p + "/party duel <Mitglied> [Kit]" + g + " – Party-1v1 (Leader)");
        player.sendMessage(p + "/party ffa [Kit]" + g + " – Party-FFA (Leader)");
        player.sendMessage(p + "/party team [Kit]" + g + " – Teamduell (Party wird in 2 Teams geteilt, gerade Zahl ab 4)");
        player.sendMessage(p + "/party leave" + g + " – Party verlassen");
        player.sendMessage(g + "Duel gegen einen beliebigen Spieler: " + p + "/duel <Spieler>");
    }

    private void handleParty(Player player, String[] args) {
        if (args.length == 0) {
            this.openPartyMenu(player);
            return;
        }
        String action = args[0].toLowerCase();
        switch (action) {
            case "menu": case "gui": {
                this.openPartyMenu(player);
                return;
            }
            case "help": case "?": {
                this.partyHelp(player);
                return;
            }
            case "create": case "new": {
                if (this.playerParties.containsKey(player.getUniqueId())) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Du bist schon in einer Party.");
                    return;
                }
                this.createParty(player);
                player.sendMessage(this.partyPrefix() + String.valueOf(ChatColor.GREEN) + "Party erstellt. Lade Spieler mit /party invite <Spieler> ein.");
                this.refreshMenus(player);
                return;
            }
            case "list": case "info": {
                this.partyList(player);
                return;
            }
            case "invite": case "add": {
                if (args.length < 2) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Nutze: /party invite <Spieler>");
                    return;
                }
                this.partyInvite(player, args[1]);
                return;
            }
            case "accept": case "join": {
                this.partyAccept(player);
                return;
            }
            case "deny": case "decline": {
                String party = this.partyInvites.remove(player.getUniqueId());
                player.sendMessage(this.partyPrefix() + (party == null ? "Du hast keine Einladung." : "Einladung abgelehnt."));
                Chat.no(player);
                return;
            }
            case "leave": {
                this.leaveParty(player);
                return;
            }
            case "disband": {
                if (!this.isPartyLeader(player)) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Nur der Party-Leader kann die Party auflösen.");
                    return;
                }
                this.disbandParty(player);
                return;
            }
            case "kick": {
                if (!this.isPartyLeader(player)) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Nur der Party-Leader kann Mitglieder entfernen.");
                    return;
                }
                if (args.length < 2) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Nutze: /party kick <Spieler>");
                    return;
                }
                Player target = this.partyMember(player, args[1]);
                if (target == null) {
                    return;
                }
                if (target.equals((Object)player)) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Du kannst dich nicht selbst entfernen. Nutze /party leave oder /party disband.");
                    return;
                }
                if (this.activePartyMatches.containsKey(target.getUniqueId())) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Der Spieler ist gerade in einem Party-Match.");
                    return;
                }
                String party = this.playerParties.get(player.getUniqueId());
                this.partyMembers.get(party).remove(target.getUniqueId());
                this.playerParties.remove(target.getUniqueId());
                target.sendMessage(this.partyPrefix() + String.valueOf(ChatColor.RED) + "Du wurdest aus der Party entfernt.");
                this.partyBroadcast(party, target.getName() + " wurde aus der Party entfernt.", null);
                this.refreshMenus(target);
                return;
            }
            case "promote": case "leader": {
                if (!this.isPartyLeader(player)) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Nur der Party-Leader kann die Leitung übergeben.");
                    return;
                }
                if (args.length < 2) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Nutze: /party promote <Spieler>");
                    return;
                }
                Player target = this.partyMember(player, args[1]);
                if (target == null || target.equals((Object)player)) {
                    return;
                }
                String party = this.playerParties.get(player.getUniqueId());
                this.partyLeaders.put(party, target.getUniqueId());
                this.partyBroadcast(party, String.valueOf(ChatColor.GOLD) + target.getName() + " ist jetzt Party-Leader.", null);
                return;
            }
            case "duel": case "1v1": {
                if (args.length < 2) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Nutze: /party duel <Mitglied> [Kit]  – oder /duel <Spieler> für ein normales Duel.");
                    return;
                }
                String kit = args.length >= 3 ? String.join((CharSequence)" ", Arrays.copyOfRange(args, 2, args.length)).toLowerCase() : "";
                this.startPartyOneVsOne(player, args[1], kit);
                return;
            }
            case "team": case "teamduell": case "2v2": {
                String teamKit = args.length >= 2 ? String.join((CharSequence)" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase() : "";
                this.startPartyTeams(player, teamKit);
                return;
            }
            case "ffa": {
                String kit = args.length >= 2 ? String.join((CharSequence)" ", Arrays.copyOfRange(args, 1, args.length)).toLowerCase() : "";
                this.startPartyFfa(player, kit);
                return;
            }
            default: {
                // /party <Spielername> = schnell einladen
                if (this.getServer().getPlayerExact(args[0]) != null) {
                    this.partyInvite(player, args[0]);
                    return;
                }
                this.partyHelp(player);
            }
        }
    }

    private void disbandParty(Player leader) {
        String party = this.playerParties.get(leader.getUniqueId());
        if (party == null) {
            return;
        }
        for (UUID member : new ArrayList<UUID>(this.partyMembers.getOrDefault(party, Set.of()))) {
            if (this.activePartyMatches.containsKey(member)) {
                leader.sendMessage(String.valueOf(ChatColor.RED) + "Während eines Party-Matches kann die Party nicht aufgelöst werden.");
                return;
            }
        }
        Set<UUID> members = this.partyMembers.remove(party);
        this.partyLeaders.remove(party);
        if (members != null) {
            for (UUID member : members) {
                this.playerParties.remove(member);
                Player online = this.getServer().getPlayer(member);
                if (online != null) {
                    online.sendMessage(this.partyPrefix() + String.valueOf(ChatColor.RED) + "Die Party wurde aufgelöst.");
                    this.refreshMenus(online);
                }
            }
        }
    }

    /** Party-Menü: je nach Zustand Erstellen/Einladung annehmen oder Verwaltung. */
    void openPartyMenu(Player player) {
        UUID id = player.getUniqueId();
        String party = this.playerParties.get(id);
        Inventory inv = this.getServer().createInventory(null, 27, String.valueOf(ChatColor.DARK_PURPLE) + PARTY_MENU);
        ItemStack filler = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.setDisplayName(" ");
        filler.setItemMeta(fm);
        for (int i = 0; i < 27; ++i) {
            inv.setItem(i, filler);
        }
        if (party == null) {
            inv.setItem(11, this.menuItem(Material.NETHER_STAR, "§d§lParty erstellen", "§7Klicke, um eine eigene Party zu erstellen.", "§7Danach kannst du Spieler einladen."));
            String inviteParty = this.partyInvites.get(id);
            if (inviteParty != null && this.partyLeaders.get(inviteParty) != null) {
                Player inviter = this.getServer().getPlayer(this.partyLeaders.get(inviteParty));
                String from = inviter == null ? "einem Spieler" : inviter.getName();
                inv.setItem(14, this.menuItem(Material.LIME_DYE, "§a§lEinladung annehmen", "§7Einladung von §f" + from));
                inv.setItem(15, this.menuItem(Material.RED_DYE, "§c§lEinladung ablehnen", "§7Einladung von §f" + from));
            } else {
                inv.setItem(15, this.menuItem(Material.BOOK, "§f§lHilfe", "§7Alle Party-Befehle anzeigen."));
            }
        } else {
            boolean leader = this.isPartyLeader(player);
            Set<UUID> members = this.partyMembers.getOrDefault(party, Set.of());
            List<String> names = new ArrayList<String>();
            for (UUID member : members) {
                Player online = this.getServer().getPlayer(member);
                String name = online == null ? String.valueOf(this.getServer().getOfflinePlayer(member).getName()) : online.getName();
                names.add("§7• §f" + name + (member.equals(this.partyLeaders.get(party)) ? " §6★" : ""));
            }
            inv.setItem(10, this.menuItem(Material.PAPER, "§d§lMitglieder (" + members.size() + ")", names.toArray(new String[0])));
            inv.setItem(12, this.menuItem(Material.NAME_TAG, "§a§lSpieler einladen", leader ? "§7Klicke und wähle einen Spieler." : "§cNur der Leader kann einladen."));
            if (leader) {
                inv.setItem(13, this.bestOfItem(this.duelBestOf.getOrDefault(id, 1)));
            }
            inv.setItem(14, this.menuItem(Material.IRON_SWORD, "§c§lParty-Duel (1v1)", leader ? "§7Befehl: §f/party duel <Mitglied> [Kit]" : "§cNur der Leader kann starten."));
            inv.setItem(15, this.menuItem(Material.DIAMOND_SWORD, "§6§lParty-FFA", leader ? "§7Klicke, um ein Kit zu wählen und zu starten." : "§cNur der Leader kann starten."));
            inv.setItem(16, this.menuItem(Material.BOOK, "§f§lHilfe", "§7Alle Party-Befehle anzeigen."));
            inv.setItem(22, this.menuItem(Material.RED_DYE, leader ? "§c§lParty auflösen" : "§c§lParty verlassen", "§7Klicke zum Bestätigen."));
        }
        player.openInventory(inv);
    }

    private ItemStack menuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private void openPartyPicker(Player player) {
        if (!this.isPartyLeader(player) && this.playerParties.containsKey(player.getUniqueId())) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Nur der Party-Leader kann einladen.");
            return;
        }
        Inventory inv = this.getServer().createInventory(null, 54, String.valueOf(ChatColor.DARK_PURPLE) + PARTY_PICK);
        int slot = 0;
        for (Player online : this.getServer().getOnlinePlayers()) {
            if (online.equals((Object)player) || slot >= 54 || this.playerParties.containsKey(online.getUniqueId())) {
                continue;
            }
            inv.setItem(slot++, this.menuItem(Material.NAME_TAG, "§f" + online.getName(), "§7Klicke, um §f" + online.getName() + "§7 einzuladen."));
        }
        if (slot == 0) {
            player.sendMessage(this.partyPrefix() + "Gerade ist niemand online, den du einladen könntest.");
            return;
        }
        player.openInventory(inv);
    }

    private void handlePartyMenuClick(InventoryClickEvent event, Player player, String title) {
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.PURPLE_STAINED_GLASS_PANE) {
            return;
        }
        if (title.contains(PARTY_PICK)) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                player.closeInventory();
                this.partyInvite(player, ChatColor.stripColor((String)meta.getDisplayName()));
            }
            return;
        }
        boolean inParty = this.playerParties.containsKey(player.getUniqueId());
        int slot = event.getRawSlot();
        player.closeInventory();
        if (!inParty) {
            if (slot == 11) {
                this.handleParty(player, new String[]{"create"});
                this.openPartyMenu(player);
            } else if (slot == 14) {
                this.partyAccept(player);
            } else if (slot == 15 && clicked.getType() == Material.RED_DYE) {
                this.handleParty(player, new String[]{"deny"});
            } else if (slot == 15) {
                this.partyHelp(player);
            }
            return;
        }
        if (slot == 13 && this.isPartyLeader(player) && clicked.getType() == Material.TARGET) {
            this.duelHosts.put(player.getUniqueId(), player.getUniqueId());
            this.cycleBestOf(player, null);
            this.openPartyMenu(player);
        } else if (slot == 12) {
            this.openPartyPicker(player);
        } else if (slot == 14) {
            player.sendMessage(this.partyPrefix() + "Starte ein Party-Duel mit " + String.valueOf(ChatColor.LIGHT_PURPLE) + "/party duel <Mitglied> [Kit]");
        } else if (slot == 15) {
            this.startPartyFfa(player, "");
        } else if (slot == 16) {
            this.partyHelp(player);
        } else if (slot == 22) {
            if (this.isPartyLeader(player)) {
                this.disbandParty(player);
            } else {
                this.leaveParty(player);
            }
        }
    }

    private void leaveParty(Player player) {
        UUID id = player.getUniqueId();
        if (this.activePartyMatches.containsKey(id)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Du kannst eine Party während eines Matches nicht verlassen.");
            return;
        }
        String party = this.playerParties.remove(id);
        if (party == null) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Du bist in keiner Party.");
            return;
        }
        Set<UUID> members = this.partyMembers.get(party);
        if (members != null) {
            members.remove(id);
        }
        if (members == null || members.isEmpty()) {
            this.partyMembers.remove(party);
            this.partyLeaders.remove(party);
        } else {
            if (id.equals(this.partyLeaders.get(party))) {
                UUID newLeader = members.iterator().next();
                this.partyLeaders.put(party, newLeader);
                Player nl = this.getServer().getPlayer(newLeader);
                this.partyBroadcast(party, String.valueOf(ChatColor.GOLD) + (nl == null ? "Ein Mitglied" : nl.getName()) + " ist jetzt Party-Leader.", null);
            }
            this.partyBroadcast(party, player.getName() + " hat die Party verlassen.", null);
        }
        player.sendMessage(this.partyPrefix() + "Du hast die Party verlassen.");
        this.refreshMenus(player);
    }

    private boolean validPartyKit(String kit) {
        return kit != null && !kit.isBlank() && this.getConfig().getStringList("kits.available").stream().anyMatch(value -> value.equalsIgnoreCase(kit));
    }

    private void openPartyKitMenu(Player player, String mode, UUID opponent) {
        if (!"lobby".equals(this.profiles.getOrDefault(player.getUniqueId(), "lobby"))) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Party-Matches sind nur in der Lobby verf\u00fcgbar.");
            return;
        }
        this.partyPendingModes.put(player.getUniqueId(), mode);
        if (opponent != null) {
            this.partyPendingOpponents.put(player.getUniqueId(), opponent);
        }
        Inventory inv = this.getServer().createInventory(null, 27, String.valueOf(ChatColor.DARK_PURPLE) + "Party-Kits");
        List kits = this.getConfig().getStringList("kits.available");
        int[] slots = new int[]{10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < kits.size() && i < slots.length; ++i) {
            String kit = (String)kits.get(i);
            inv.setItem(slots[i], this.item(this.iconForKit(kit), String.valueOf(ChatColor.LIGHT_PURPLE) + this.displayKitName(kit), String.valueOf(ChatColor.GRAY) + "Party-" + mode + " starten\n" + String.valueOf(ChatColor.LIGHT_PURPLE) + "Klicke zur Auswahl"));
        }
        if ("1v1".equalsIgnoreCase(mode)) {
            inv.setItem(22, this.bestOfItem(this.duelBestOf.getOrDefault(player.getUniqueId(), 1)));
        }
        this.decorateMenu(inv);
        player.openInventory(inv);
    }

    private void startPartyOneVsOne(Player leader, String targetName, String kit) {
        String party = this.playerParties.get(leader.getUniqueId());
        if (party == null || !this.partyLeaders.getOrDefault(party, leader.getUniqueId()).equals(leader.getUniqueId())) {
            leader.sendMessage(String.valueOf(ChatColor.RED) + "Nur der Party-Leader kann Party-Matches starten.");
            return;
        }
        Player target = this.getServer().getPlayerExact(targetName);
        if (target == null || !this.partyMembers.getOrDefault(party, Set.of()).contains(target.getUniqueId()) || target.equals((Object)leader)) {
            leader.sendMessage(String.valueOf(ChatColor.RED) + "Der Zielspieler muss online und in deiner Party sein.");
            return;
        }
        if (kit.isBlank()) {
            this.openPartyKitMenu(leader, "1v1", target.getUniqueId());
            return;
        }
        if (!this.validPartyKit(kit)) {
            leader.sendMessage(String.valueOf(ChatColor.RED) + "Kit nicht gefunden. Nutze /party duel " + targetName + " <Kit>.");
            return;
        }
        this.startPartyMatch(leader, List.of(leader, target), kit, false);
    }

    /** Teamduell: die Party wird zufaellig in zwei gleich grosse Teams geteilt (gerade Zahl, mindestens 4 online). */
    private void startPartyTeams(Player leader, String kit) {
        String party = this.playerParties.get(leader.getUniqueId());
        if (party == null || !this.partyLeaders.getOrDefault(party, leader.getUniqueId()).equals(leader.getUniqueId())) {
            leader.sendMessage(String.valueOf(ChatColor.RED) + "Nur der Party-Leader kann ein Teamduell starten.");
            return;
        }
        List<Player> players = new ArrayList<Player>();
        for (UUID member : this.partyMembers.getOrDefault(party, Set.of())) {
            Player online = this.getServer().getPlayer(member);
            if (online != null && online.isOnline()) {
                players.add(online);
            }
        }
        if (players.size() < 4 || players.size() % 2 != 0) {
            leader.sendMessage(String.valueOf(ChatColor.RED) + "Für ein Teamduell braucht die Party eine gerade Spielerzahl (mindestens 4 online). Jetzt online: " + players.size());
            return;
        }
        if (kit.isBlank()) {
            this.openPartyKitMenu(leader, "TEAM", null);
            return;
        }
        if (!this.validPartyKit(kit)) {
            leader.sendMessage(String.valueOf(ChatColor.RED) + "Kit nicht gefunden. Nutze /party team <Kit>.");
            return;
        }
        java.util.Collections.shuffle(players);
        Map<UUID, Integer> teams = new ConcurrentHashMap<UUID, Integer>();
        StringBuilder one = new StringBuilder();
        StringBuilder two = new StringBuilder();
        for (int i = 0; i < players.size(); ++i) {
            teams.put(players.get(i).getUniqueId(), i % 2);
            (i % 2 == 0 ? one : two).append(one.length() + two.length() == 0 ? "" : " ").append(players.get(i).getName());
        }
        this.startPartyMatch(leader, players, kit, false, teams);
        if (this.activePartyMatches.containsKey(leader.getUniqueId())) {
            for (Player player : players) {
                player.sendMessage("§d⚔ §7Team 1: §f" + one.toString().trim() + " §8| §7Team 2: §f" + two.toString().trim());
                player.sendMessage("§7Du bist in §d" + (teams.get(player.getUniqueId()) == 0 ? "Team 1" : "Team 2") + "§7. Teamkollegen können sich nicht verletzen.");
            }
        }
    }

    private void startPartyFfa(Player leader, String kit) {
        Set<UUID> ids;
        String party = this.playerParties.get(leader.getUniqueId());
        Set<UUID> set = ids = party == null ? Set.of() : this.partyMembers.getOrDefault(party, Set.of());
        if (party == null || !this.partyLeaders.getOrDefault(party, leader.getUniqueId()).equals(leader.getUniqueId())) {
            leader.sendMessage(String.valueOf(ChatColor.RED) + "Nur der Party-Leader kann ein Party-FFA starten.");
            return;
        }
        if (ids.size() < 2) {
            leader.sendMessage(String.valueOf(ChatColor.RED) + "F\u00fcr ein FFA brauchst du mindestens zwei Party-Mitglieder.");
            return;
        }
        if (kit.isBlank()) {
            this.openPartyKitMenu(leader, "FFA", null);
            return;
        }
        if (!this.validPartyKit(kit)) {
            leader.sendMessage(String.valueOf(ChatColor.RED) + "Kit nicht gefunden. Nutze /party ffa <Kit>.");
            return;
        }
        List<Player> players = ids.stream().map(arg_0 -> ((Server)this.getServer()).getPlayer(arg_0)).filter(Objects::nonNull).filter(OfflinePlayer::isOnline).toList();
        if (players.size() < 2) {
            leader.sendMessage(String.valueOf(ChatColor.RED) + "Mindestens zwei Party-Mitglieder m\u00fcssen online sein.");
            return;
        }
        this.startPartyMatch(leader, players, kit, true);
    }

    void startPartyMatch(Player leader, List<Player> players, String kit, boolean ffa) {
        this.startPartyMatch(leader, players, kit, ffa, null);
    }

    void startPartyMatch(Player leader, List<Player> players, String kit, boolean ffa, Map<UUID, Integer> teams) {
        if (this.blockedByMaintenance(leader)) {
            return;
        }
        String arena = this.findFreeArena();
        if (arena == null) {
            leader.sendMessage(String.valueOf(ChatColor.RED) + "Keine freie, vollst\u00e4ndige Party-Arena verf\u00fcgbar.");
            return;
        }
        for (Player player : players) {
            UUID id = player.getUniqueId();
            if ("lobby".equals(this.profiles.getOrDefault(id, "lobby")) && !this.activeDuelPlayers.contains(id) && !this.activePartyMatches.containsKey(id)) continue;
            leader.sendMessage(String.valueOf(ChatColor.RED) + player.getName() + " kann gerade nicht am Party-Match teilnehmen.");
            return;
        }
        if (this.arenaSnapshots.containsKey(arena)) {
            this.startPreparedPartyMatch(leader, players, kit, ffa, teams, arena);
            return;
        }
        leader.sendMessage("§7Die Arena wird schrittweise vorbereitet; das Party-Match startet gleich.");
        this.snapshotArena(arena, success -> {
            if (!success) {
                if (leader.isOnline()) leader.sendMessage("§cDie Arena konnte nicht sicher vorbereitet werden. Bitte einen Admin um /duelladmin diagnose.");
                return;
            }
            boolean participantsReady = players.stream().allMatch(p -> p.isOnline()
                    && "lobby".equals(this.profiles.getOrDefault(p.getUniqueId(), "lobby"))
                    && !this.activeDuelPlayers.contains(p.getUniqueId())
                    && !this.activePartyMatches.containsKey(p.getUniqueId()));
            if (!participantsReady) {
                if (leader.isOnline()) leader.sendMessage("§cParty-Match abgebrochen: Mindestens ein Spieler ist nicht mehr verfügbar.");
                return;
            }
            this.startPreparedPartyMatch(leader, players, kit, ffa, teams, arena);
        });
    }

    private void startPreparedPartyMatch(Player leader, List<Player> players, String kit, boolean ffa, Map<UUID, Integer> teams, String arena) {
        if (this.blockedByMaintenance(leader)) {
            this.getConfig().set("duels.arenas." + arena + ".inUse", (Object)false);
            this.saveConfig();
            return;
        }
        boolean participantsReady = players.stream().allMatch(p -> p.isOnline()
                && "lobby".equals(this.profiles.getOrDefault(p.getUniqueId(), "lobby"))
                && !this.activeDuelPlayers.contains(p.getUniqueId())
                && !this.activePartyMatches.containsKey(p.getUniqueId()));
        if (!participantsReady) {
            this.getConfig().set("duels.arenas." + arena + ".inUse", (Object)false);
            this.saveConfig();
            if (leader.isOnline()) leader.sendMessage("§cParty-Match abgebrochen: Mindestens ein Spieler ist nicht mehr verfügbar.");
            return;
        }
        String match = UUID.randomUUID().toString();
        ConcurrentHashMap.KeySetView ids = ConcurrentHashMap.newKeySet();
        for (Player player : players) {
            UUID id = player.getUniqueId();
            ids.add(id);
            this.activePartyMatches.put(id, match);
            this.duelArenas.put(id, arena);
            this.beginDuelKitNow(player, kit);
        }
        this.partyMatchPlayers.put(match, ids);
        if (teams != null) {
            this.partyMatchTeams.put(match, teams);
        }
        this.partyMatchFfa.put(match, ffa);
        this.partyMatchKits.put(match, kit);
        this.partyMatchArenas.put(match, arena);
        if (!ffa && teams == null && players.size() == 2) {
            int best = this.duelBestOf.getOrDefault(leader.getUniqueId(), 1);
            if (best > 1) {
                Series series = new Series(players.get(0).getUniqueId(), players.get(1).getUniqueId(), best, arena, kit, true);
                this.duelSeries.put(series.a, series);
                this.duelSeries.put(series.b, series);
                for (Player player : players) {
                    player.sendMessage("\u00a7d\u2694 \u00a77Serie: \u00a7fBest of " + best + " \u00a78(erster mit " + series.needed + " Siegen)");
                }
            }
        }
        this.partyWarmupUntil.put(match, System.currentTimeMillis() + 10000L);
        this.getConfig().set("duels.arenas." + arena + ".inUse", (Object)true);
        this.saveConfig();
        Location spawn1 = this.readLocation("duels.arenas." + arena + ".spawn1");
        Location spawn2 = this.readLocation("duels.arenas." + arena + ".spawn2");
        if (spawn2 == null || spawn2.getWorld() == null) {
            spawn2 = spawn1;
        }
        int[] teamSlot = new int[2];
        for (int i = 0; i < players.size(); ++i) {
            Player player = players.get(i);
            Location target;
            if (ffa) {
                target = spawn1.clone();
            } else {
                int side = teams != null ? teams.getOrDefault(player.getUniqueId(), 0) : (i == 0 ? 0 : 1);
                target = (side == 0 ? spawn1 : spawn2).clone();
                if (teams != null) {
                    // Team-Duell: Spieler des Teams auf eigene Spawns (spawn<Team>_<Platz>) verteilen, sonst Hauptspawn
                    int slot = teamSlot[side]++;
                    if (slot > 0) {
                        Location extra = this.readLocation("duels.arenas." + arena + ".spawn" + (side + 1) + "_" + (slot + 1));
                        if (extra != null && extra.getWorld() != null) {
                            target = extra.clone();
                        }
                    }
                }
            }
            player.teleport(target);
            this.duelCountdownPlayers.add(player.getUniqueId());
            player.sendTitle(String.valueOf(ChatColor.LIGHT_PURPLE) + "PARTY " + (ffa ? "FFA" : teams != null ? (players.size() / 2) + "v" + (players.size() / 2) : "1v1"), String.valueOf(ChatColor.LIGHT_PURPLE) + "Kit: " + this.displayKitName(kit), 0, 40, 10);
        }
        this.partyCountdownStep(match, 10);
        String modeName = ffa ? "Party-FFA" : teams != null ? "Party-Teamduell " + (players.size() / 2) + "v" + (players.size() / 2) : "Party-1v1";
        for (Player player : players) {
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + modeName + " gestartet! Kit: " + this.displayKitName(kit) + ". Countdown: 10 Sekunden, bis dahin kann niemand angreifen.");
        }
    }

    /** Countdown fuer Party-Matches: jede Sekunde Titel + Sound, am Ende LOS! */
    private void partyCountdownStep(String match, int left) {
        Set<UUID> ids = this.partyMatchPlayers.get(match);
        if (ids == null) {
            this.partyMatchTasks.remove(match);
            return;
        }
        if (left <= 0) {
            this.partyMatchTasks.remove(match);
            this.partyWarmupUntil.remove(match);
            for (UUID id : ids) {
                this.duelCountdownPlayers.remove(id);
                Player player = this.getServer().getPlayer(id);
                if (player == null) continue;
                player.sendTitle(String.valueOf(ChatColor.GREEN) + "K\u00c4MPFT!", String.valueOf(ChatColor.GRAY) + "Viel Erfolg", 0, 25, 10);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            }
            return;
        }
        for (UUID id : ids) {
            Player player = this.getServer().getPlayer(id);
            if (player == null) continue;
            if (left <= 5) {
                player.sendTitle(String.valueOf(ChatColor.LIGHT_PURPLE) + left, String.valueOf(ChatColor.GRAY) + "Bereit machen...", 0, 15, 5);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, left <= 3 ? 1.5f : 1.0f);
            } else {
                player.sendTitle(String.valueOf(ChatColor.LIGHT_PURPLE) + left, String.valueOf(ChatColor.GRAY) + "Das Match startet gleich", 0, 15, 5);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 0.8f);
            }
        }
        int task = this.getServer().getScheduler().runTaskLater((Plugin)this, () -> this.partyCountdownStep(match, left - 1), 20L).getTaskId();
        this.partyMatchTasks.put(match, task);
    }

    void finishPartyMatch(String match, UUID winnerId, String reason) {
        EventGames.partyFinished(match, winnerId);
        HashSet<UUID> ids = new HashSet<UUID>(this.partyMatchPlayers.getOrDefault(match, Set.of()));
        Integer task = this.partyMatchTasks.remove(match);
        if (task != null) {
            this.getServer().getScheduler().cancelTask(task.intValue());
        }
        this.partyWarmupUntil.remove(match);
        String arena = this.partyMatchArenas.remove(match);
        this.partyMatchPlayers.remove(match);
        this.partyMatchFfa.remove(match);
        this.partyMatchKits.remove(match);
        boolean teamMatch = this.partyMatchTeams.remove(match) != null;
        for (UUID id : ids) {
            this.activePartyMatches.remove(id);
            this.duelCountdownPlayers.remove(id);
            this.duelArenas.remove(id);
            Player player = this.getServer().getPlayer(id);
            if (player == null) continue;
            if (winnerId != null && (winnerId.equals(id) || teamMatch)) {
                player.sendTitle(String.valueOf(ChatColor.GREEN) + "SIEG!", String.valueOf(ChatColor.LIGHT_PURPLE) + (reason == null ? "Letzter Spieler" : reason), 0, 50, 10);
            } else if (reason != null) {
                player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + reason);
            }
            this.finishDuel(player);
        }
        if (arena != null) {
            this.getConfig().set("duels.arenas." + arena + ".inUse", (Object)false);
            this.saveConfig();
            this.resetArena(arena);
        }
    }

    void handlePartyDeath0(Player player) {
        UUID id = player.getUniqueId();
        String match = this.activePartyMatches.remove(id);
        if (match == null) {
            return;
        }
        Set<UUID> remaining = this.partyMatchPlayers.getOrDefault(match, ConcurrentHashMap.newKeySet());
        remaining.remove(id);
        this.pendingDuelRespawns.put(id, this.lobbyCenter());
        this.duelArenas.remove(id);
        this.finishDuel(player);
        Map<UUID, Integer> teams = this.partyMatchTeams.get(match);
        if (teams != null) {
            int side0 = 0;
            int side1 = 0;
            for (UUID other : remaining) {
                if (teams.getOrDefault(other, 0) == 0) {
                    side0++;
                } else {
                    side1++;
                }
            }
            if (side0 == 0 || side1 == 0) {
                this.finishPartyMatch(match, remaining.stream().findFirst().orElse(null), "Team " + (side0 == 0 ? 2 : 1) + " gewinnt!");
                return;
            }
            for (UUID other : remaining) {
                Player online = this.getServer().getPlayer(other);
                if (online != null) {
                    online.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + player.getName() + " ist ausgeschieden. §7Team 1: §f" + side0 + " §7• Team 2: §f" + side1);
                }
            }
            return;
        }
        if (remaining.size() <= 1) {
            UUID winner = remaining.stream().findFirst().orElse(null);
            this.finishPartyMatch(match, winner, "Party-Match beendet.");
        } else {
            for (UUID other : remaining) {
                Player online = this.getServer().getPlayer(other);
                if (online == null) continue;
                online.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + player.getName() + " ist ausgeschieden. Noch " + remaining.size() + " Spieler im Party-FFA.");
            }
        }
    }

    /** Verlaesst alle Warteschlangen ohne Meldung (z. B. beim Wechsel zwischen Ranked und Unranked). */
    void leaveDuelQueueQuiet(Player player) {
        for (List<UUID> queue : this.duelQueues.values()) {
            queue.remove(player.getUniqueId());
        }
        this.queuedSince.remove(player.getUniqueId());
    }

    void leaveDuelQueue(Player player) {
        UUID id = player.getUniqueId();
        boolean removed = false;
        for (List<UUID> queue : this.duelQueues.values()) {
            removed |= queue.remove(id);
        }
        if (removed) {
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Duel-Warteschlange verlassen.");
        } else {
            player.sendMessage(String.valueOf(ChatColor.GRAY) + "Du bist in keiner Duel-Warteschlange.");
        }
    }

    private boolean canUseDuels(Player player) {
        return "lobby".equals(this.profiles.getOrDefault(player.getUniqueId(), "lobby")) && !this.activeDuelPlayers.contains(player.getUniqueId());
    }

    boolean requireLobbyForDuels(Player player) {
        if (this.canUseDuels(player)) {
            return true;
        }
        player.sendMessage(String.valueOf(ChatColor.RED) + "Duels sind nur in der Lobby verf\u00fcgbar. Wechsle zuerst mit /lobby zur\u00fcck.");
        return false;
    }

    private void handleDuel(Player player, String[] args) {
        if (args.length == 1 && (args[0].equalsIgnoreCase("last") || args[0].equalsIgnoreCase("nochmal"))) {
            String last = this.lastKit.get(player.getUniqueId());
            if (last == null) {
                player.sendMessage("§cDu hast noch kein Kit gespielt. §7Wähle eins über die Duell-NPCs oder /duel.");
            } else {
                player.sendMessage("§7Zuletzt gespielt: §f" + last);
                this.joinRandomDuel(player, last);
            }
            return;
        }
        if (args.length == 1 && (args[0].equalsIgnoreCase("leave") || args[0].equalsIgnoreCase("quit"))) {
            if (this.activeDuelPlayers.contains(player.getUniqueId())) {
                this.duelLeaveCheck(player, this.duelModes.getOrDefault(player.getUniqueId(), ""));
            } else {
                this.leaveDuelQueue(player);
            }
            return;
        }
        if (args.length >= 1 && (args[0].equalsIgnoreCase("top") || args[0].equalsIgnoreCase("stats") || args[0].equalsIgnoreCase("rangliste"))) {
            player.performCommand("rangliste");
            return;
        }
        if (!this.requireLobbyForDuels(player)) {
            return;
        }
        if (args.length == 0) {
            this.openDuelKits(player, null);
            return;
        }
        if (args.length == 1 && (args[0].equalsIgnoreCase("history") || args[0].equalsIgnoreCase("verlauf"))) {
            DuelStats.showHistory(player);
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("rematch")) {
            DuelFixes fixes = DuelFixes.get();
            if (fixes != null) {
                fixes.action(player, "rematch");
            }
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("accept")) {
            UUID challengerId = this.duelInvites.remove(player.getUniqueId());
            if (challengerId == null) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Du hast keine offene Duel-Anfrage.");
                return;
            }
            Player challenger = this.getServer().getPlayer(challengerId);
            if (challenger == null || !challenger.isOnline()) {
                this.cancelDuelInvite(challengerId, player.getUniqueId());
                player.sendMessage(String.valueOf(ChatColor.RED) + "Der Herausforderer ist nicht mehr online.");
                return;
            }
            if (!this.canUseDuels(challenger) || !this.canUseDuels(player)) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Duels k\u00f6nnen nur gestartet werden, wenn beide Spieler in der Lobby sind.");
                this.cancelDuelInvite(challengerId, player.getUniqueId());
                return;
            }
            this.outgoingDuelInvites.remove(challengerId);
            this.duelPartners.put(player.getUniqueId(), challengerId);
            this.duelPartners.put(challengerId, player.getUniqueId());
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Duel-Anfrage angenommen.");
            challenger.sendMessage(String.valueOf(ChatColor.GREEN) + player.getName() + " hat angenommen.");
            Chat.yes(player);
            Chat.yes(challenger);
            this.openDuelKits(player, challenger);
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("deny")) {
            UUID challengerId = this.duelInvites.remove(player.getUniqueId());
            if (challengerId != null) {
                this.cancelDuelInvite(challengerId, player.getUniqueId());
                Player challenger = this.getServer().getPlayer(challengerId);
                if (challenger != null) {
                    challenger.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + player.getName() + " hat die Duel-Anfrage abgelehnt.");
                    Chat.no(challenger);
                }
            }
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Duel-Anfrage abgelehnt.");
            Chat.no(player);
            return;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
            UUID targetId = this.outgoingDuelInvites.get(player.getUniqueId());
            if (targetId == null) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Du hast keine ausgehende Duel-Anfrage.");
                return;
            }
            this.cancelDuelInvite(player.getUniqueId(), targetId);
            Player target = this.getServer().getPlayer(targetId);
            if (target != null) {
                target.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + player.getName() + " hat die Duel-Anfrage zur\u00fcckgezogen.");
            }
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Duel-Anfrage zur\u00fcckgezogen.");
            return;
        }
        if (args.length != 1) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Nutze: /duel <Spieler>, /duel accept, /duel deny oder /duel cancel");
            return;
        }
        if (this.activeDuelPlayers.contains(player.getUniqueId()) || this.duelPartners.containsKey(player.getUniqueId()) || this.outgoingDuelInvites.containsKey(player.getUniqueId())) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Du bist bereits in einer Runde oder einer Duel-Anfrage.");
            return;
        }
        Player target = this.getServer().getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Dieser Spieler ist nicht online.");
            return;
        }
        if (!this.canUseDuels(target)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Du kannst nur Spieler herausfordern, die sich in der Lobby befinden.");
            return;
        }
        if (target.equals((Object)player)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Du kannst dich nicht selbst herausfordern.");
            return;
        }
        if (this.activeDuelPlayers.contains(target.getUniqueId()) || this.duelPartners.containsKey(target.getUniqueId()) || this.duelInvites.containsKey(target.getUniqueId())) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Dieser Spieler ist bereits besch\u00e4ftigt.");
            return;
        }
        this.duelHosts.put(player.getUniqueId(), player.getUniqueId());
        this.duelHosts.put(target.getUniqueId(), player.getUniqueId());
        this.duelInvites.put(target.getUniqueId(), player.getUniqueId());
        this.outgoingDuelInvites.put(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Du hast " + target.getName() + " herausgefordert.");
        player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "W\u00e4hle jetzt dein Kit. Der Kampf startet erst, wenn " + target.getName() + " annimmt.");
        this.openDuelKits(player, null);
        target.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + player.getName() + " fordert dich zu einem Duel heraus.");
        Chat.request(target, "§7Die Anfrage ist 60 Sekunden gültig.", "duel accept", "§7Duel annehmen", "duel deny", "§7Duel ablehnen");
        Chat.ping(target);
        Chat.click(player);
        UUID targetId = target.getUniqueId();
        UUID challengerId = player.getUniqueId();
        this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
            if (this.cancelDuelInvite(challengerId, targetId)) {
                Player onlineTarget = this.getServer().getPlayer(targetId);
                Player onlineChallenger = this.getServer().getPlayer(challengerId);
                if (onlineTarget != null) {
                    onlineTarget.sendMessage(String.valueOf(ChatColor.GRAY) + "Die Duel-Anfrage ist abgelaufen.");
                }
                if (onlineChallenger != null) {
                    onlineChallenger.sendMessage(String.valueOf(ChatColor.GRAY) + "Die Duel-Anfrage an " + args[0] + " ist abgelaufen.");
                }
            }
        }, 1200L);
    }

    void openParkourMenu(Player player) {
        ConfigurationSection section = this.getConfig().getConfigurationSection("parkour.maps");
        if (section == null || section.getKeys(false).isEmpty()) {
            if (!this.teleportToSpawn(player, "parkour")) {
                player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Noch keine Parkour-Strecken eingerichtet.");
            }
            this.sendParkourHelp(player);
            return;
        }
        Inventory inv = this.getServer().createInventory(null, 27, String.valueOf(ChatColor.LIGHT_PURPLE) + "Parkour-Strecken");
        this.decorateMenu(inv);
        int slot = 10;
        for (String map : section.getKeys(false)) {
            if (slot > 16) break;
            String status = DuelFixes.parkourStatus(this, map, player);
            boolean ready = status.startsWith("OK");
            String recordLine = "\u00a78\u2503 \u00a77Bestzeit: \u00a78noch keine";
            String mineLine = "\u00a78\u2503 \u00a77Deine Zeit: \u00a78noch keine";
            List<RecordStore.Entry> top = this.records.top(map);
            if (!top.isEmpty()) {
                recordLine = "\u00a78\u2503 \u00a77Bestzeit: \u00a76" + ParkourEnhancements.formatTime(top.get(0).timeMs()) + " \u00a77von \u00a7f" + top.get(0).name();
            }
            for (int rank = 0; rank < top.size(); ++rank) {
                if (top.get(rank).id().equals(player.getUniqueId().toString())) {
                    mineLine = "\u00a78\u2503 \u00a77Deine Zeit: \u00a7e" + ParkourEnhancements.formatTime(top.get(rank).timeMs()) + " \u00a78(Platz #" + (rank + 1) + ")";
                }
            }
            String sep = "\u00a78\u00a7m                         ";
            String notReady = player.hasPermission("duelnetwork.admin") ? "\u00a77" + status + "\n\u00a7cNoch nicht spielbar" : "\u00a7cDerzeit nicht verf\u00fcgbar";
            inv.setItem(slot++, this.item(ready ? this.icon("map." + map.toLowerCase(), Material.FEATHER) : Material.BARRIER, (ready ? "\u00a7d" : "\u00a7c") + this.displayParkourName(map), (ready ? recordLine + "\n" + mineLine + "\n" + sep + "\n\u00a7e\u25b6 Klicken zum Starten" : notReady)));
        }
        player.openInventory(inv);
    }

    private void fillParkourLavaArea(String map) {
        String base = "parkour.maps." + map;
        Location first = this.readLocation(base + ".fall1");
        Location second = this.readLocation(base + ".fall2");
        if (first == null || second == null || first.getWorld() == null || !first.getWorld().equals((Object)second.getWorld())) {
            return;
        }
        long width = Math.abs((long)first.getBlockX() - second.getBlockX()) + 1L;
        long height = Math.abs((long)first.getBlockY() - second.getBlockY()) + 1L;
        long depth = Math.abs((long)first.getBlockZ() - second.getBlockZ()) + 1L;
        long volume;
        try {
            volume = Math.multiplyExact(Math.multiplyExact(width, height), depth);
        }
        catch (ArithmeticException overflow) {
            return;
        }
        if (volume > 500000L) {
            this.getLogger().warning("Parkour-Lavafl\u00e4che " + map + " ist zu gro\u00df: " + volume + " Bl\u00f6cke.");
            return;
        }
        for (int x = Math.min(first.getBlockX(), second.getBlockX()); x <= Math.max(first.getBlockX(), second.getBlockX()); ++x) {
            for (int y = Math.min(first.getBlockY(), second.getBlockY()); y <= Math.max(first.getBlockY(), second.getBlockY()); ++y) {
                for (int z = Math.min(first.getBlockZ(), second.getBlockZ()); z <= Math.max(first.getBlockZ(), second.getBlockZ()); ++z) {
                    first.getWorld().getBlockAt(x, y, z).setType(Material.LAVA, false);
                }
            }
        }
    }

    private boolean isParkourLavaBlock(Location location) {
        ConfigurationSection maps = this.getConfig().getConfigurationSection("parkour.maps");
        if (maps == null || location == null) {
            return false;
        }
        for (String map : maps.getKeys(false)) {
            String base = "parkour.maps." + map;
            if (!this.getConfig().contains(base + ".fall1.world") || !this.getConfig().contains(base + ".fall2.world") || !this.inParkourArea(location, this.readLocation(base + ".fall1"), this.readLocation(base + ".fall2"))) continue;
            return true;
        }
        return false;
    }

    private void sendParkourHelp(Player player) {
        player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501 PARKOUR \u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501");
        player.sendMessage(String.valueOf(ChatColor.WHITE) + "Verf\u00fcgbare Befehle:");
        player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/parkour" + String.valueOf(ChatColor.GRAY) + " \u2013 Parkour-Lobby und diese Hilfe \u00f6ffnen");
        player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/parkour list" + String.valueOf(ChatColor.GRAY) + " \u2013 alle Strecken anzeigen");
        player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/parkour join <Map>" + String.valueOf(ChatColor.GRAY) + " \u2013 eine Strecke starten");
        player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/parkour restart" + String.valueOf(ChatColor.GRAY) + " \u2013 laufende Strecke neu starten");
        player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/parkour leave" + String.valueOf(ChatColor.GRAY) + " \u2013 Parkour verlassen und zur Lobby zur\u00fcck");
        player.sendMessage(String.valueOf(ChatColor.GRAY) + "Checkpoints: einfach dar\u00fcber laufen. Bei Lava/Fallzone wirst du zum letzten Checkpoint zur\u00fcckgesetzt.");
        player.sendMessage(String.valueOf(ChatColor.GRAY) + "Die Zeit l\u00e4uft ab dem Start und wird am Ziel automatisch gespeichert.");
    }

    private void handleParkourCommand(Player player, String[] args) {
        String map;
        if (args.length == 0) {
            this.sendParkourHelp(player);
            if (!this.teleportToSpawn(player, "parkour")) {
                player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Noch keine Parkour-Lobby eingerichtet.");
            }
            this.openParkourMenu(player);
            return;
        }
        if (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("hilfe") || args[0].equals("?")) {
            this.sendParkourHelp(player);
            return;
        }
        if (args[0].equalsIgnoreCase("leave") || args[0].equalsIgnoreCase("quit")) {
            this.leaveParkour(player, true);
            return;
        }
        if (args[0].equalsIgnoreCase("restart") || args[0].equalsIgnoreCase("reset")) {
            String active = this.activeParkourMaps.get(player.getUniqueId());
            if (active == null) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Du bist gerade auf keiner Parkour-Strecke.");
                return;
            }
            this.joinParkour(player, active);
            return;
        }
        map = args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("start") ? (args.length >= 2 ? args[1] : "") : args[0];
        if (map.isBlank()) {
            this.sendParkourHelp(player);
            return;
        }
        if (map.equalsIgnoreCase("list")) {
            ConfigurationSection section = this.getConfig().getConfigurationSection("parkour.maps");
            if (section == null || section.getKeys(false).isEmpty()) {
                player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Es sind noch keine Parkour-Maps eingerichtet.");
                return;
            }
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "\u2501\u2501\u2501\u2501 Parkour-Maps \u2501\u2501\u2501\u2501");
            for (String name : section.getKeys(false)) {
                player.sendMessage(String.valueOf(ChatColor.WHITE) + name + String.valueOf(ChatColor.DARK_GRAY) + " \u2022 " + String.valueOf(ChatColor.GRAY) + DuelFixes.parkourStatus(this, name, player));
            }
            return;
        }
        this.joinParkour(player, map.toLowerCase());
    }

    private String formatParkourTime(long milliseconds) {
        long seconds = Math.max(0L, (milliseconds + 999L) / 1000L);
        return String.format("%02d:%02d", seconds / 60L, seconds % 60L);
    }

    private void saveParkourRecord(Player player, String map, long elapsedMilliseconds) {
        if (!AntiCheat.allowRecord(player, map, elapsedMilliseconds)) {
            return;
        }
        boolean personalBest = this.records.submit(map, player.getUniqueId(), player.getName(), elapsedMilliseconds);
        List<RecordStore.Entry> top = this.records.top(map);
        int rank = -1;
        for (int i = 0; i < top.size(); ++i) {
            if (top.get(i).id().equals(player.getUniqueId().toString())) {
                rank = i + 1;
            }
        }
        player.sendMessage("");
        player.sendMessage("\u00a7d\u2691 \u00a7fZeit: \u00a7d\u00a7l" + ParkourEnhancements.formatTime(elapsedMilliseconds) + (personalBest ? " \u00a78\u2022 \u00a7d\u2605 Neue pers\u00f6nliche Bestzeit!" : ""));
        if (!top.isEmpty()) {
            player.sendMessage("\u00a77Bestzeit: \u00a7f" + top.get(0).name() + " \u00a78\u00bb \u00a76" + ParkourEnhancements.formatTime(top.get(0).timeMs()) + (rank > 0 ? " \u00a78\u2022 \u00a77Dein Platz: \u00a7d#" + rank : ""));
        }
        player.sendMessage("\u00a77Alle Rekorde: \u00a7d/parkour records " + map);
    }

    private void sendParkourRecords(Player player, String map) {
        ParkourEnhancements.showRecords(player, map);
    }

    private void joinParkour(Player player, String map) {
        String path = "parkour.maps." + map;
        if (!this.getConfig().contains(path + ".start.world") || !this.getConfig().contains(path + ".end.world")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Diese Parkour-Map ist noch nicht vollst\u00e4ndig eingerichtet.");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Status: " + DuelFixes.parkourStatus(this, map, player));
            player.sendMessage(String.valueOf(ChatColor.GRAY) + "Ein Admin muss mindestens Start und Ende setzen: /duelladmin parkour start/end " + map);
            return;
        }
        Location start = this.readLocation(path + ".start");
        if (start.getWorld() == null) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Die Welt der Parkour-Map wurde nicht gefunden.");
            return;
        }
        if (this.activeDuelPlayers.contains(player.getUniqueId())) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Verlasse zuerst deine laufende Runde.");
            return;
        }
        this.leaveParkour(player, false);
        if (this.profiles.getOrDefault(player.getUniqueId(), "lobby").equals("smp")) {
            if (this.isInSmpCombat(player)) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Du bist noch im Combat.");
                return;
            }
            this.saveLastSmpLocation(player);
            this.switchProfile(player, "lobby");
        }
        player.teleport(start);
        UUID id = player.getUniqueId();
        this.activeParkourMaps.put(id, map);
        this.parkourCheckpoints.put(id, start.clone());
        this.parkourStartTimes.put(id, System.currentTimeMillis());
        Ghosts.begin(player, map);
        this.parkourFallCooldowns.remove(id);
        Integer oldTimer = this.parkourTimerTasks.remove(id);
        if (oldTimer != null) {
            this.getServer().getScheduler().cancelTask(oldTimer.intValue());
        }
        int timerTask = this.getServer().getScheduler().runTaskTimer((Plugin)this, () -> {
            if (!player.isOnline() || !this.activeParkourMaps.containsKey(id)) {
                Integer task = this.parkourTimerTasks.remove(id);
                if (task != null) {
                    this.getServer().getScheduler().cancelTask(task.intValue());
                }
                return;
            }
            long elapsed = System.currentTimeMillis() - this.parkourStartTimes.getOrDefault(id, System.currentTimeMillis());
            player.sendActionBar((Component)Component.text((String)(String.valueOf(ChatColor.LIGHT_PURPLE) + "\u23f1 " + this.formatParkourTime(elapsed))));
        }, 0L, 20L).getTaskId();
        this.parkourTimerTasks.put(id, timerTask);
        player.setGameMode(GameMode.ADVENTURE);
        player.sendTitle(String.valueOf(ChatColor.LIGHT_PURPLE) + "PARKOUR", String.valueOf(ChatColor.LIGHT_PURPLE) + this.displayParkourName(map), 0, 35, 10);
        player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "\u2726 Parkour gestartet: " + String.valueOf(ChatColor.WHITE) + this.displayParkourName(map) + String.valueOf(ChatColor.GRAY) + " \u2022 Erreiche das Ende!");
        player.sendMessage(String.valueOf(ChatColor.GRAY) + "Laufe durch die Checkpoints und zum Ziel. Bei einem Fall wirst du zum letzten Checkpoint zur\u00fcckgesetzt. /parkour help");
    }

    void leaveParkour(Player player, boolean teleportLobby) {
        UUID id = player.getUniqueId();
        String map = this.activeParkourMaps.remove(id);
        Ghosts.stop(id);
        this.parkourCheckpoints.remove(id);
        this.parkourStartTimes.remove(id);
        this.parkourFallCooldowns.remove(id);
        Integer timerTask = this.parkourTimerTasks.remove(id);
        if (timerTask != null) {
            this.getServer().getScheduler().cancelTask(timerTask.intValue());
        }
        if (map != null && player.isOnline()) {
            player.setGameMode(GameMode.SURVIVAL);
            if (teleportLobby) {
                this.resetPlayerState(player);
                this.teleportToLobby(player);
            }
            player.sendMessage(String.valueOf(ChatColor.GRAY) + "Parkour verlassen.");
        }
    }

    private static final String RECORDS_GUI = "Rekorde: ";

    private void openRecordsGui(Player admin, String playerName) {
        List<RecordStore.Row> rows = this.records.find(playerName);
        if (rows.isEmpty()) {
            admin.sendMessage(String.valueOf(ChatColor.GRAY) + "Keine Parkour-Rekorde f\u00fcr " + playerName + " gefunden.");
            return;
        }
        String name = rows.get(0).name();
        Inventory inv = this.getServer().createInventory(null, 54, String.valueOf(ChatColor.DARK_RED) + RECORDS_GUI + name);
        int slot = 0;
        for (RecordStore.Row row : rows) {
            if (slot >= 45) {
                break;
            }
            inv.setItem(slot++, this.menuItem(Material.CLOCK, "\u00a7f" + row.map(), "\u00a77Zeit: \u00a7e" + ParkourEnhancements.formatTime(row.timeMs()), "\u00a7cKlicken: diesen Rekord l\u00f6schen"));
        }
        inv.setItem(49, this.menuItem(Material.TNT, "\u00a74\u00a7lALLE Rekorde von " + name + " l\u00f6schen", "\u00a77" + rows.size() + " Rekord(e)", "\u00a7cKlicken zum L\u00f6schen"));
        inv.setItem(53, this.menuItem(Material.BARRIER, "\u00a77Schlie\u00dfen"));
        admin.openInventory(inv);
    }

    private void handleRecordsGuiClick(InventoryClickEvent event, Player admin, String title) {
        event.setCancelled(true);
        if (!admin.hasPermission("duelnetwork.admin") || event.getRawSlot() < 0 || event.getRawSlot() >= 54) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        String name = ChatColor.stripColor((String)title).substring(RECORDS_GUI.length()).trim();
        if (clicked.getType() == Material.BARRIER) {
            admin.closeInventory();
            return;
        }
        if (clicked.getType() == Material.TNT) {
            int removed = this.records.remove(name, null);
            admin.sendMessage(String.valueOf(ChatColor.GREEN) + removed + " Rekord(e) von " + name + " gel\u00f6scht.");
            this.getLogger().info(admin.getName() + " hat " + removed + " Parkour-Rekord(e) von " + name + " zur\u00fcckgesetzt (alle Maps).");
            admin.closeInventory();
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }
        String map = ChatColor.stripColor((String)meta.getDisplayName());
        int removed = this.records.remove(name, map);
        admin.sendMessage(String.valueOf(removed > 0 ? ChatColor.GREEN : ChatColor.GRAY) + (removed > 0 ? "Rekord von " + name + " auf " + map + " gel\u00f6scht." : "Dieser Rekord existiert nicht mehr."));
        if (removed > 0) {
            this.getLogger().info(admin.getName() + " hat den Parkour-Rekord von " + name + " auf " + map + " zur\u00fcckgesetzt.");
        }
        this.getServer().getScheduler().runTask((Plugin)this, () -> this.openRecordsGui(admin, name));
    }

    /** /duelladmin icon <Eintrag> <MATERIAL|hand|reset> - Icons der Menues anpassen. */
    private void handleIconAdmin(Player admin, String[] args) {
        String g = String.valueOf(ChatColor.GRAY);
        String p = String.valueOf(ChatColor.LIGHT_PURPLE);
        if (args.length < 3) {
            admin.sendMessage(p + "Nutze: /duelladmin icon <Eintrag> <MATERIAL|hand|reset>");
            admin.sendMessage(g + "Eintraege: " + p + "smp, duell, creative, events, parkour" + g + " (Navigator)");
            admin.sendMessage(g + "          " + p + "parkour:<Map>" + g + ", " + p + "kit:<Kit>");
            admin.sendMessage(g + "Beispiel: " + p + "/duelladmin icon parkour:city DIAMOND_BOOTS" + g + " oder " + p + "... hand" + g + " (Item in der Hand)");
            return;
        }
        String key = args[1].toLowerCase().trim();
        String path;
        if (key.startsWith("parkour:")) {
            path = "map." + key.substring(8);
        } else if (key.startsWith("kit:")) {
            path = "kit." + key.substring(4).replace(' ', '_');
        } else {
            String type = key.equals("duel") ? "duell" : key.equals("bau") || key.equals("bausmp") || key.equals("bau-smp") ? "creative" : key;
            if (!List.of("smp", "duell", "creative", "events", "parkour").contains(type)) {
                admin.sendMessage(String.valueOf(ChatColor.RED) + "Unbekannter Eintrag. /duelladmin icon zeigt die Liste.");
                return;
            }
            path = "nav." + type;
        }
        String value = args[2];
        if (value.equalsIgnoreCase("reset")) {
            this.getConfig().set("icons." + path, null);
            this.saveConfig();
            admin.sendMessage(String.valueOf(ChatColor.GREEN) + "Icon zur\u00fcckgesetzt: " + key);
            return;
        }
        Material material;
        if (value.equalsIgnoreCase("hand")) {
            material = admin.getInventory().getItemInMainHand().getType();
        } else {
            material = Material.matchMaterial(value);
        }
        if (material == null || material.isAir() || material == Material.BARRIER) {
            admin.sendMessage(String.valueOf(ChatColor.RED) + "Unbekanntes Material: " + value + " (z.B. DIAMOND_SWORD, oder 'hand').");
            return;
        }
        this.getConfig().set("icons." + path, material.name());
        this.saveConfig();
        admin.sendMessage(String.valueOf(ChatColor.GREEN) + "Icon f\u00fcr " + key + ": " + material.name() + " (Barrier-Block bleibt f\u00fcr nicht verf\u00fcgbare Eintr\u00e4ge).");
    }

    /** Admin: Parkour-Rekorde ansehen und (z.B. bei Cheatern) zuruecksetzen. */
    private void handleRecordsAdmin(Player admin, String[] args) {
        String usage = String.valueOf(ChatColor.LIGHT_PURPLE) + "Nutze: /duelladmin records list <Spieler> | reset <Spieler> <Map|all> | map <Map>";
        if (args.length == 2 && !List.of("list", "reset", "map", "gui").contains(args[1].toLowerCase())) {
            this.openRecordsGui(admin, args[1]);
            return;
        }
        if (args.length < 3) {
            admin.sendMessage(usage);
            admin.sendMessage(String.valueOf(ChatColor.GRAY) + "Am einfachsten: /duelladmin records <Spieler>  \u2013 \u00f6ffnet ein Men\u00fc, Klick auf einen Rekord l\u00f6scht ihn.");
            return;
        }
        String mode = args[1].toLowerCase();
        if (mode.equals("gui")) {
            this.openRecordsGui(admin, args[2]);
            return;
        }
        if (mode.equals("list")) {
            List<RecordStore.Row> rows = this.records.find(args[2]);
            if (rows.isEmpty()) {
                admin.sendMessage(String.valueOf(ChatColor.GRAY) + "Keine Parkour-Rekorde f\u00fcr " + args[2] + " gefunden.");
                return;
            }
            admin.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Parkour-Rekorde von " + rows.get(0).name() + " (" + rows.size() + "):");
            for (RecordStore.Row row : rows) {
                admin.sendMessage(String.valueOf(ChatColor.GRAY) + " \u2022 " + row.map() + ": " + String.valueOf(ChatColor.WHITE) + ParkourEnhancements.formatTime(row.timeMs()));
            }
            admin.sendMessage(String.valueOf(ChatColor.GRAY) + "Zur\u00fccksetzen: /duelladmin records reset " + rows.get(0).name() + " <Map|all>");
        } else if (mode.equals("map")) {
            List<RecordStore.Entry> top = this.records.top(args[2]);
            if (top.isEmpty()) {
                admin.sendMessage(String.valueOf(ChatColor.GRAY) + "Keine Rekorde auf der Map " + args[2] + ".");
                return;
            }
            admin.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Rekorde auf " + args[2] + " (" + top.size() + "):");
            int place = 1;
            for (RecordStore.Entry entry : top) {
                admin.sendMessage(String.valueOf(ChatColor.GRAY) + " " + place++ + ". " + String.valueOf(ChatColor.WHITE) + entry.name() + String.valueOf(ChatColor.GRAY) + " \u2013 " + ParkourEnhancements.formatTime(entry.timeMs()));
            }
        } else if (mode.equals("reset")) {
            if (args.length < 4) {
                admin.sendMessage(usage);
                return;
            }
            String map = args[3].equalsIgnoreCase("all") ? null : args[3];
            int removed = this.records.remove(args[2], map);
            if (removed == 0) {
                admin.sendMessage(String.valueOf(ChatColor.GRAY) + "Nichts zu l\u00f6schen: keine passenden Rekorde von " + args[2] + (map == null ? "." : " auf " + map + "."));
                return;
            }
            admin.sendMessage(String.valueOf(ChatColor.GREEN) + removed + " Rekord(e) von " + args[2] + (map == null ? " auf allen Maps" : " auf " + map) + " zur\u00fcckgesetzt.");
            this.getLogger().info(admin.getName() + " hat " + removed + " Parkour-Rekord(e) von " + args[2] + " zur\u00fcckgesetzt (" + (map == null ? "alle Maps" : map) + ").");
        } else {
            admin.sendMessage(usage);
        }
    }

    String parkourName(String map) {
        return this.displayParkourName(map);
    }

    Long parkourStart(UUID id) {
        return this.parkourStartTimes.get(id);
    }

    void refreshAllRanks() {
        for (Player online : this.getServer().getOnlinePlayers()) {
            this.applyRankDisplay(online);
        }
    }

    /** Server-Rang als Text fuer die Sidebar. */
    String rankLabel(UUID id) {
        String rank = this.playerRanks.get(id);
        if (rank == null) {
            return "\u00a77Spieler";
        }
        return switch (rank.toLowerCase()) {
            case "owner" -> "\u00a7b\u00a7lOwner";
            case "admin" -> "\u00a7c\u00a7lAdmin";
            case "media" -> "\u00a7d\u00a7lMedia";
            case "premium" -> "\u00a76\u00a7lPremium";
            case "tester" -> "\u00a7a\u00a7lTester";
            default -> "\u00a77Spieler";
        };
    }

    private String displayParkourName(String map) {
        return map.replace('-', ' ').replace('_', ' ');
    }

    private ConfigurationSection parkourMap(String map) {
        return this.getConfig().getConfigurationSection("parkour.maps." + map.toLowerCase());
    }

    private void handleParkourAdmin(Player player, String action, String rawMap, String rawNumber) {
        String map = rawMap.toLowerCase();
        if (action.equalsIgnoreCase("icon")) {
            // /duelladmin parkour icon <Map> <MATERIAL|hand|reset>
            if (map.isEmpty() || rawNumber.isEmpty()) {
                player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin parkour icon <Map> <MATERIAL|hand|reset> " + String.valueOf(ChatColor.GRAY) + "- Symbol der Strecke im Men\u00fc");
                return;
            }
            this.handleIconAdmin(player, new String[]{"icon", "parkour:" + map, rawNumber});
            return;
        }
        if (action.equalsIgnoreCase("list")) {
            ConfigurationSection section = this.getConfig().getConfigurationSection("parkour.maps");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Parkour-Maps: " + (section == null || section.getKeys(false).isEmpty() ? "keine" : String.join((CharSequence)", ", section.getKeys(false))));
            return;
        }
        if (action.equalsIgnoreCase("records") || action.equalsIgnoreCase("top") || action.equalsIgnoreCase("stats")) {
            if (map.isBlank()) {
                player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Nutze: /duelladmin parkour records <map>");
                return;
            }
            this.sendParkourRecords(player, map);
            return;
        }
        if (map.isBlank()) {
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Nutze: /duelladmin parkour <aktion> <map> [nummer]");
            return;
        }
        String base = "parkour.maps." + map;
        if (action.equalsIgnoreCase("create")) {
            this.getConfig().set(base + ".created", (Object)true);
            this.saveConfig();
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Parkour-Map erstellt: " + map);
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "N\u00e4chste Schritte: start, end, checkpoint, fall1, fall2 und danach check setzen.");
        } else if (action.equalsIgnoreCase("delete")) {
            this.getConfig().set(base, null);
            this.saveConfig();
            this.records.deleteMap(map);
            for (Player online : this.getServer().getOnlinePlayers()) {
                if (!map.equals(this.activeParkourMaps.get(online.getUniqueId()))) continue;
                this.leaveParkour(online, true);
            }
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Parkour-Map gel\u00f6scht: " + map);
        } else if (action.equalsIgnoreCase("start") || action.equalsIgnoreCase("end") || action.equalsIgnoreCase("fall1") || action.equalsIgnoreCase("fall2")) {
            if (!this.getConfig().contains(base)) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Map nicht gefunden. Erstelle sie zuerst mit create.");
                return;
            }
            String key = action.equalsIgnoreCase("start") ? "start" : (action.equalsIgnoreCase("end") ? "end" : action);
            this.saveLocation(base + "." + key, player.getLocation());
            if (key.equals("fall2")) {
                this.fillParkourLavaArea(map);
                player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Die Fallzone wurde mit nicht flie\u00dfender Lava gef\u00fcllt.");
            }
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Parkour-" + key + " f\u00fcr " + map + " gespeichert.");
            player.sendMessage(String.valueOf(ChatColor.GRAY) + "Status: " + this.parkourValidationMessage(map));
        } else if (action.equalsIgnoreCase("checkpoint")) {
            int number;
            if (!this.getConfig().contains(base)) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Map nicht gefunden. Erstelle sie zuerst mit create.");
                return;
            }
            try {
                number = rawNumber.isBlank() ? this.nextParkourCheckpoint(map) : Integer.parseInt(rawNumber);
            }
            catch (NumberFormatException ex) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Die Checkpoint-Nummer muss eine Zahl sein.");
                return;
            }
            if (number < 1) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Die Checkpoint-Nummer muss mindestens 1 sein.");
                return;
            }
            this.saveLocation(base + ".checkpoints." + number, player.getLocation());
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Checkpoint " + number + " f\u00fcr " + map + " gespeichert.");
        } else if (action.equalsIgnoreCase("check")) {
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Parkour " + map + ": " + this.parkourValidationMessage(map));
        } else {
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Aktionen: create, delete, list, start, end, checkpoint, fall1, fall2, check");
        }
    }

    private int nextParkourCheckpoint(String map) {
        ConfigurationSection section = this.getConfig().getConfigurationSection("parkour.maps." + map + ".checkpoints");
        return section == null ? 1 : section.getKeys(false).stream().mapToInt(key -> {
            try {
                return Integer.parseInt(key);
            }
            catch (NumberFormatException ex) {
                return 0;
            }
        }).max().orElse(0) + 1;
    }

    String parkourValidationMessage(String map) {
        String base = "parkour.maps." + map;
        if (!this.getConfig().contains(base)) {
            return "NICHT GEFUNDEN";
        }
        boolean start = this.getConfig().contains(base + ".start.world");
        boolean end = this.getConfig().contains(base + ".end.world");
        boolean fall = this.getConfig().contains(base + ".fall1.world") && this.getConfig().contains(base + ".fall2.world");
        ConfigurationSection checkpoints = this.getConfig().getConfigurationSection(base + ".checkpoints");
        return (start && end ? "OK" : "UNVOLLST\u00c4NDIG") + " \u2022 Start=" + (start ? "OK" : "FEHLT") + " \u2022 Ende=" + (end ? "OK" : "FEHLT") + " \u2022 Checkpoints=" + (checkpoints == null ? 0 : checkpoints.getKeys(false).size()) + " \u2022 Fallzone=" + (fall ? "OK" : "FEHLT");
    }

    private boolean inParkourArea(Location location, Location first, Location second) {
        return first != null && second != null && first.getWorld() != null && location != null && location.getWorld() != null && first.getWorld().equals((Object)location.getWorld()) && Math.abs(location.getX() - (first.getX() + second.getX()) / 2.0) <= Math.abs(first.getX() - second.getX()) / 2.0 + 1.0 && Math.abs(location.getY() - (first.getY() + second.getY()) / 2.0) <= Math.abs(first.getY() - second.getY()) / 2.0 + 2.0 && Math.abs(location.getZ() - (first.getZ() + second.getZ()) / 2.0) <= Math.abs(first.getZ() - second.getZ()) / 2.0 + 1.0;
    }

    @EventHandler
    public void onParkourLavaFlow(BlockFromToEvent event) {
        if (event.getBlock().getType() == Material.LAVA && this.isParkourLavaBlock(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    /** Zwischengespeicherte Punkte einer Parkour-Strecke (20 Sekunden), damit das Move-Event keine Config-Strings aufbaut. */
    private static final class ParkourPoints {
        Location end;
        Location fall1;
        Location fall2;
        Location start;
        final java.util.LinkedHashMap<String, Location> checkpoints = new java.util.LinkedHashMap<String, Location>();
        long loaded;
    }

    private final Map<String, ParkourPoints> parkourPointCache = new ConcurrentHashMap<String, ParkourPoints>();

    private ParkourPoints parkourPoints(String map) {
        ParkourPoints cached = this.parkourPointCache.get(map);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.loaded < 20000L) {
            return cached;
        }
        String base = "parkour.maps." + map;
        ParkourPoints fresh = new ParkourPoints();
        fresh.loaded = now;
        fresh.end = this.getConfig().contains(base + ".end.world") ? this.readLocation(base + ".end") : null;
        fresh.fall1 = this.getConfig().contains(base + ".fall1.world") ? this.readLocation(base + ".fall1") : null;
        fresh.fall2 = this.getConfig().contains(base + ".fall2.world") ? this.readLocation(base + ".fall2") : null;
        fresh.start = this.readLocation(base + ".start");
        ConfigurationSection cps = this.getConfig().getConfigurationSection(base + ".checkpoints");
        if (cps != null) {
            for (String key : cps.getKeys(false)) {
                fresh.checkpoints.put(key, this.readLocation(base + ".checkpoints." + key));
            }
        }
        this.parkourPointCache.put(map, fresh);
        return fresh;
    }

    @EventHandler
    public void onParkourMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        String map = this.activeParkourMaps.get(id);
        if (map == null || event.getTo() == null) {
            return;
        }
        Location to = event.getTo();
        Location from = event.getFrom();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        ParkourPoints points = this.parkourPoints(map);
        Location end = points.end;
        if (end != null && end.getWorld() != null && end.getWorld().equals((Object)to.getWorld()) && end.distanceSquared(to) <= 9.0) {
            long elapsed = Math.max(0L, System.currentTimeMillis() - this.parkourStartTimes.getOrDefault(id, System.currentTimeMillis()));
            long seconds = (elapsed + 999L) / 1000L;
            this.saveParkourRecord(player, map, elapsed);
            Ghosts.finish(player, map, elapsed);
            this.leaveParkour(player, false);
            player.setGameMode(GameMode.SURVIVAL);
            this.resetPlayerState(player);
            this.teleportToLobby(player);
            player.sendTitle(String.valueOf(ChatColor.GREEN) + "PARKOUR GESCHAFFT!", String.valueOf(ChatColor.LIGHT_PURPLE) + "Zeit: " + seconds / 60L + ":" + String.format("%02d", seconds % 60L), 0, 60, 15);
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "\u2726 Ziel erreicht! " + String.valueOf(ChatColor.WHITE) + "Deine Zeit: " + seconds / 60L + ":" + String.format("%02d", seconds % 60L));
            return;
        }
        if (this.inParkourArea(to, points.fall1, points.fall2)) {
            long now = System.currentTimeMillis();
            if (now - this.parkourFallCooldowns.getOrDefault(id, 0L) < 700L) {
                return;
            }
            this.parkourFallCooldowns.put(id, now);
            Location checkpoint = this.parkourCheckpoints.getOrDefault(id, points.start);
            player.teleport(checkpoint);
            player.setFallDistance(0.0f);
            player.setFireTicks(0);
            player.setNoDamageTicks(10);
            player.sendActionBar((Component)Component.text((String)(String.valueOf(ChatColor.LIGHT_PURPLE) + "\u21a9 Zur\u00fcck zum letzten Checkpoint")));
        }
        for (Map.Entry<String, Location> entry : points.checkpoints.entrySet()) {
            Location checkpoint = entry.getValue();
            if (checkpoint.getWorld() == null || !checkpoint.getWorld().equals((Object)to.getWorld()) || !(checkpoint.distanceSquared(to) <= 2.25)) continue;
            this.parkourCheckpoints.put(id, checkpoint.clone());
            player.sendActionBar((Component)Component.text((String)(String.valueOf(ChatColor.GREEN) + "\u2713 Checkpoint " + entry.getKey())));
            break;
        }
    }

    private String diagnoseLocation(String label, String setupCommand, String... paths) {
        for (String path : paths) {
            String worldName = this.getConfig().getString(path + ".world");
            if (worldName == null || worldName.isBlank()) continue;
            for (String axis : new String[]{"x", "y", "z"}) {
                Object coordinate = this.getConfig().get(path + "." + axis);
                if (!(coordinate instanceof Number)) return "§cFEHLER §7" + label + ": Koordinate " + axis + " fehlt oder ist keine Zahl (" + path + ")";
            }
            return this.getServer().getWorld(worldName) == null
                    ? "§cFEHLER §7" + label + ": Welt '" + worldName + "' ist nicht geladen"
                    : "§aOK §7" + label + " in " + worldName;
        }
        return "§cFEHLT §7" + label + ": Einrichten mit " + setupCommand;
    }

    /** Schnelle Setup-Pruefung ohne Welt-, Chunk-, Block- oder Entity-Scans. */
    private void diagnoseSetup(Player player) {
        player.sendMessage("§d§lDuelNetwork Diagnose §8— §7Setup- und Config-Prüfung");
        player.sendMessage(this.diagnoseLocation("Hauptlobby", "/duelladmin setspawn haupt", "locations.lobbies.haupt", "locations.spawns.haupt.default", "locations.spawns.haupt"));
        player.sendMessage(this.diagnoseLocation("SMP-Spawn", "/duelladmin setspawn smp", "locations.spawns.smp.default", "locations.spawns.smp"));
        player.sendMessage(this.diagnoseLocation("Bau-SMP-Spawn", "/duelladmin setspawn creative", "locations.spawns.creative.default", "locations.spawns.creative"));
        player.sendMessage(this.diagnoseLocation("Duell-Lobby", "/duelladmin setlobby duell", "locations.lobbies.duell", "locations.spawns.duell.default"));

        List<String> kits = this.getConfig().getStringList("kits.available");
        player.sendMessage(kits.isEmpty() ? "§cFEHLER §7Keine Duell-Kits in kits.available eingerichtet."
                : "§aOK §7" + kits.size() + " Duell-Kit(s) konfiguriert.");

        ConfigurationSection arenas = this.getConfig().getConfigurationSection("duels.arenas");
        if (arenas == null || arenas.getKeys(false).isEmpty()) {
            player.sendMessage("§cFEHLER §7Keine Duell-Arena eingerichtet: /duelladmin arena create <name>");
        } else {
            List<String> names = new ArrayList<String>(arenas.getKeys(false));
            player.sendMessage("§7Duell-Arenen: §f" + names.size());
            for (int i = 0; i < Math.min(8, names.size()); i++) {
                String name = names.get(i);
                String status = this.arenaValidationMessage(name);
                if (status.startsWith("FEHLER: inUse=true")) {
                    player.sendMessage("§ePRÜFEN §7Arena " + name + ": belegt/gesperrt (Status inUse=true)");
                } else {
                    player.sendMessage((status.startsWith("OK") ? "§a✔ " : "§c✖ ") + "§7Arena " + name + ": " + status);
                }
            }
            if (names.size() > 8) player.sendMessage("§8… weitere Arenen mit /duelladmin arena list prüfen.");
        }

        List<String> problems = ConfigCheck.run(this);
        this.configProblems = new ArrayList<String>(problems);
        if (problems.isEmpty()) {
            player.sendMessage("§a✔ config.yml: keine bekannten Hinweise.");
        } else {
            player.sendMessage("§eConfig: " + problems.size() + " Hinweis(e):");
            for (int i = 0; i < Math.min(6, problems.size()); i++) player.sendMessage("§7• " + problems.get(i));
            if (problems.size() > 6) player.sendMessage("§8… Details stehen in der Serverkonsole mit [config].");
        }
        player.sendMessage("§7Backups: §f" + (this.backups == null ? "aus" : this.backups.last())
                + " §8• §7Vault: §f" + (this.vaultActive ? "aktiv" : "nicht aktiv")
                + " §8• §7PlaceholderAPI: §f" + (this.papiActive ? "aktiv" : "nicht aktiv"));
        player.sendMessage("§8Die Diagnose liest nur Config/Status; es werden keine Chunks oder Blöcke gescannt.");
    }

    private void handleAdmin(Player player, String[] args) {
        if (!player.hasPermission("duelnetwork.admin")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Daf\u00fcr brauchst du die Permission duelnetwork.admin (oder OP).");
            return;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin npc <navigator|smp|duell(=Duelle, führt in die Duell-Lobby)|ranked|unranked|team|dummy|events|parkour|creative(=Bau-SMP)>");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin diagnose  (Setup, Config, Welten, Arenen und Kits prüfen)");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin setlobby <haupt|duell|events|parkour|creative>");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin setspawn <smp|duell|events|parkour|creative> [name]");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin setduelspawn <arena> <1|2> | seteventspawn <event> | event <add|remove|list|...>");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin unlock <nether|end> §7sofort §8| §dunlock start <nether|end> [24h] §7Countdown §8| §dlock <nether|end> §8| §dunlock status");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin kit <create|save|delete|list> <name>");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin kitedit <name>  (Inventar sortieren)");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin arena <create|delete|list|check> <name>");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin parkour <create|delete|list|start|end|checkpoint|fall1|fall2|check> <map> [nummer]");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin npcskin <typ> <MinecraftName>");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin npclock <typ> <on|off>  (NPC sperren/freigeben)");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin records <Spieler>  (Men\u00fc: Klick l\u00f6scht Rekord) | records list|reset|map ...");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin npcfix  (NPCs neu stylen, alte Villager umwandeln)");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "/duelladmin icon <Eintrag> <MATERIAL|hand|reset>  (Menue-Icons)");
            return;
        }
        if (args[0].equalsIgnoreCase("diagnose")) {
            this.diagnoseSetup(player);
            return;
        }
        if (args[0].equalsIgnoreCase("icon")) {
            this.handleIconAdmin(player, args);
        } else if (args[0].equalsIgnoreCase("npc") && args.length >= 2) {
            this.spawnMenuNpc(player, args[1]);
        } else if (args[0].equalsIgnoreCase("npcfix")) {
            int converted = this.convertVillagerNpcs();
            player.sendMessage(String.valueOf(ChatColor.GREEN) + converted + " Villager-NPC(s) in Skin-NPCs umgewandelt.");
        } else if (args[0].equalsIgnoreCase("setlobby") && args.length >= 2) {
            this.saveLocation("locations.lobbies." + args[1].toLowerCase(), player.getLocation());
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Lobby-Spawn gespeichert: " + args[1]);
        } else if (args[0].equalsIgnoreCase("setspawn") && args.length >= 2) {
            String name = args.length >= 3 ? args[2] : "default";
            this.saveLocation("locations.spawns." + args[1].toLowerCase() + "." + name.toLowerCase(), player.getLocation());
            if (args[1].equalsIgnoreCase("smp") && name.equalsIgnoreCase("default")) {
                this.configureSmpWorldBorder();
            }
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Spielmodus-Spawn gespeichert: " + args[1] + " / " + name);
        } else if (args[0].equalsIgnoreCase("setduelspawn") && args.length >= 3) {
            // /duelladmin setduelspawn <Arena> <1|2> [Platz 2-4]: Platz 1 = Hauptspawn des Teams, 2-4 = weitere Spawns fuer Team-Duelle
            String suffix = args[2];
            if (args.length >= 4 && (args[2].equals("1") || args[2].equals("2")) && args[3].matches("[2-4]")) {
                suffix = args[2] + "_" + args[3];
            }
            this.saveLocation("duels.arenas." + args[1] + ".spawn" + suffix, player.getLocation());
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Duel-Spawn gespeichert: " + args[1] + " / " + (suffix.contains("_") ? "Team " + args[2] + ", Platz " + args[3] : "Spieler " + args[2]));
        } else if (args[0].equalsIgnoreCase("event")) {
            Events.admin(this, player, args);
        } else if (args[0].equalsIgnoreCase("reload")) {
            this.reloadAll();
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "config.yml neu geladen. " + String.valueOf(ChatColor.GRAY) + "(Pack-Modus und Ports brauchen einen Neustart.)");
            this.adminSetupHints(player);
        } else if (args[0].equalsIgnoreCase("egg")) {
            DragonEgg.admin(this, player, args);
        } else if (args[0].equalsIgnoreCase("game")) {
            EventGames.admin(this, player, args);
        } else if ((args[0].equalsIgnoreCase("unlock") || args[0].equalsIgnoreCase("lock")) && this.rtp != null) {
            Rtp.admin(this, player, args, this.rtp);
        } else if (args[0].equalsIgnoreCase("eco")) {
            Economy.admin(this, player, args);
        } else if (args[0].equalsIgnoreCase("seteventspawn") && args.length >= 2) {
            this.saveLocation("events." + args[1] + ".spawn", player.getLocation());
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Event-Spawn gespeichert: " + args[1]);
        } else if (args[0].equalsIgnoreCase("finish") && args.length >= 2) {
            Player target = this.getServer().getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Spieler nicht gefunden.");
            } else {
                this.finishDuel(target);
            }
        } else if (args[0].equalsIgnoreCase("kit") && args.length >= 2) {
            this.handleKitAdmin(player, args[1], args.length >= 3 ? args[2] : "");
        } else if (args[0].equalsIgnoreCase("kitedit") && args.length >= 2) {
            this.openKitEditor(player, args[1]);
        } else if (args[0].equalsIgnoreCase("arena") && args.length >= 2) {
            this.handleArenaAdmin(player, args[1], args.length >= 3 ? args[2] : "");
        } else if (args[0].equalsIgnoreCase("records")) {
            this.handleRecordsAdmin(player, args);
        } else if (args[0].equalsIgnoreCase("parkour") && args.length >= 2) {
            this.handleParkourAdmin(player, args[1], args.length >= 3 ? args[2] : "", args.length >= 4 ? args[3] : "");
        } else if (args[0].equalsIgnoreCase("npcskin")) {
            this.handleNpcSkinAdmin(player, args);
        } else if (args[0].equalsIgnoreCase("npclock") && args.length >= 3) {
            this.setNpcLock(player, args[1], args[2]);
        } else {
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Nutze /duelladmin fuer die Setup-Hilfe.");
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static final List<String> NPC_TYPES = List.of("smp", "duell", "ranked", "unranked", "team", "dummy", "events", "parkour", "creative");

    private void spawnMenuNpc(Player player, String type) {
        String normalized = switch (type.toLowerCase()) {
            case "duelle", "duel", "duell-lobby" -> "duell";
            case "teams", "teamduell", "teamduelle" -> "team";
            case "bausmp", "bau-smp" -> "creative";
            default -> type.toLowerCase();
        };
        List<String> allowed = NPC_TYPES;
        if (!allowed.contains(normalized)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Unbekannter NPC. Erlaubt: " + String.join((CharSequence)", ", allowed));
            return;
        }
        Location playerLocation = player.getLocation();
        Location spawnLocation = new Location(player.getWorld(), (double)playerLocation.getBlockX() + 0.5, (double)playerLocation.getBlockY(), (double)playerLocation.getBlockZ() + 0.5, playerLocation.getYaw(), 0.0f);
        String spawnKey = this.locationKey(spawnLocation);
        this.pendingNpcSpawns.add(spawnKey);
        Entity npc = null;
        boolean mannequin = false;
        try {
            try {
                // Mannequin (Minecraft 1.21.9+): Spieler-Figur mit echtem Skin
                npc = player.getWorld().spawnEntity(spawnLocation, EntityType.valueOf("MANNEQUIN"));
                mannequin = true;
            }
            catch (Throwable t) {
                npc = null;
            }
            if (npc == null || !npc.isValid()) {
                npc = player.getWorld().spawnEntity(spawnLocation, EntityType.VILLAGER);
                mannequin = false;
            }
        }
        finally {
            this.pendingNpcSpawns.remove(spawnKey);
        }
        if (npc == null || !npc.isValid()) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Der NPC konnte nicht gespawnt werden. Pr\u00fcfe die Server-Konsole.");
            this.getLogger().warning("NPC-Spawn wurde trotz internem Spawnmarker abgebrochen: " + normalized + " bei " + String.valueOf(spawnLocation));
            return;
        }
        npc.setInvulnerable(!normalized.equals("dummy"));
        npc.setSilent(true);
        npc.setGravity(false);
        npc.setRotation(spawnLocation.getYaw(), 0.0f);
        if (npc instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)npc;
            living.setCollidable(false);
            if (normalized.equals("dummy") && living.getAttribute(Attribute.MAX_HEALTH) != null) {
                living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(2048.0);
                living.setHealth(2048.0);
            }
            if (living.getAttribute(Attribute.SCALE) != null && !mannequin) {
                living.getAttribute(Attribute.SCALE).setBaseValue(2.0);
            }
        }
        if (npc instanceof Villager) {
            Villager villager = (Villager)npc;
            villager.setAI(false);
            villager.setProfession(Villager.Profession.NONE);
        }
        npc.customName(this.npcName(normalized));
        npc.setCustomNameVisible(true);
        npc.getPersistentDataContainer().set(this.npcKey, PersistentDataType.STRING, normalized);
        if (mannequin) {
            this.applyNpcSkin(npc, normalized);
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "NPC mit Skin \"" + this.npcSkinName(normalized) + "\" gespawnt: " + normalized);
        } else {
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Sichtbarer NPC gespawnt (Villager, dieser Server kennt keine Mannequins): " + normalized);
        }
    }

    /** Wandelt alle geladenen Villager-Menue-NPCs in Mannequins mit Skin um. */
    int convertVillagerNpcs() {
        int count = 0;
        for (World world : this.getServer().getWorlds()) {
            count += this.convertVillagerNpcs(new ArrayList<Entity>(world.getEntities()));
        }
        return count;
    }

    int convertVillagerNpcs(List<Entity> entities) {
        int count = 0;
        for (Entity old : entities) {
            if (!(old instanceof Villager)) {
                continue;
            }
            String type = (String)old.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING);
            if (type == null || !old.isValid()) {
                continue;
            }
            Location at = old.getLocation();
            String key = this.locationKey(at);
            Entity fresh = null;
            this.pendingNpcSpawns.add(key);
            try {
                fresh = at.getWorld().spawnEntity(at, EntityType.valueOf("MANNEQUIN"));
            }
            catch (Throwable t) {
                this.getLogger().warning("Mannequin-NPC konnte nicht erstellt werden (alter Server?): " + t);
                return count;
            }
            finally {
                this.pendingNpcSpawns.remove(key);
            }
            if (fresh == null || !fresh.isValid()) {
                continue;
            }
            fresh.setInvulnerable(true);
            fresh.setSilent(true);
            fresh.setGravity(false);
            fresh.setRotation(at.getYaw(), 0.0f);
            fresh.customName(this.npcName(type));
            fresh.setCustomNameVisible(true);
            fresh.getPersistentDataContainer().set(this.npcKey, PersistentDataType.STRING, type);
            this.applyNpcSkin(fresh, type);
            old.remove();
            ++count;
        }
        return count;
    }

    /** Neu geladene Chunks: alte Villager-NPCs ebenfalls umwandeln. */
    @EventHandler
    public void onNpcEntitiesLoad(EntitiesLoadEvent event) {
        List<Entity> loaded = new ArrayList<Entity>(event.getEntities());
        boolean any = false;
        for (Entity e : loaded) {
            if (e.getPersistentDataContainer().has(this.npcKey, PersistentDataType.STRING)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        this.getServer().getScheduler().runTask((Plugin)this, () -> {
            this.convertVillagerNpcs(loaded);
            boolean removeLegacy = this.getConfig().getBoolean("npcs.remove-legacy", true);
            for (Entity e : loaded) {
                String type = (String)e.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING);
                if (type == null || !e.isValid() || e instanceof Villager) {
                    continue;
                }
                if (!NPC_TYPES.contains(type) && removeLegacy) {
                    this.removeNpcLabel(e);
                    e.remove();
                } else if (NPC_TYPES.contains(type)) {
                    this.styleNpc(e, type);
                }
            }
        });
    }

    private static final Map<String, String> DEFAULT_NPC_SKINS = Map.of("navigator", "MHF_Villager", "smp", "MHF_Steve", "duell", "MHF_Golem", "events", "MHF_Question", "parkour", "MHF_Alex", "creative", "MHF_Chest", "ranked", "MHF_Blaze", "unranked", "MHF_Chicken", "team", "MHF_Zombie", "dummy", "MHF_Skeleton");

    private void handleNpcSkinAdmin(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.YELLOW + "Nutze: /duelladmin npcskin <typ|all> <Spielername|import|clear>");
            player.sendMessage(ChatColor.GRAY + "Sammelimport: Dateien in plugins/DuelNetwork/npc-skins/ legen und /duelladmin npcskin all import ausführen.");
            return;
        }
        String type = args[1].toLowerCase(java.util.Locale.ROOT);
        if (type.equals("all") && args[2].equalsIgnoreCase("import")) {
            this.importAllNpcSkins(player);
            return;
        }
        if (!DEFAULT_NPC_SKINS.containsKey(type)) {
            player.sendMessage(ChatColor.RED + "Unbekannter NPC-Typ. Erlaubt: " + String.join(", ", DEFAULT_NPC_SKINS.keySet()));
            return;
        }
        String path = "npc-skins." + type;
        String action = args[2];
        if (action.equalsIgnoreCase("import")) {
            java.io.File importFile = new java.io.File(new java.io.File(this.getDataFolder(), "npc-skins"), type + ".yml");
            if (!importFile.isFile()) {
                player.sendMessage(ChatColor.RED + "Importdatei fehlt: plugins/DuelNetwork/npc-skins/" + type + ".yml");
                player.sendMessage(ChatColor.GRAY + "Dateiinhalt: texture-value: '…' und optional texture-signature: '…'.");
                return;
            }
            org.bukkit.configuration.file.YamlConfiguration imported = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(importFile);
            String value = imported.getString("texture-value", "");
            String signature = imported.getString("texture-signature", "");
            if (!this.isValidNpcTexture(value, signature)) {
                player.sendMessage(ChatColor.RED + "Import abgebrochen: texture-value oder texture-signature ist ungültig.");
                return;
            }
            this.getConfig().set(path + ".texture-value", value);
            this.getConfig().set(path + ".texture-signature", signature);
            this.saveConfig();
            this.refreshNpcSkins(type, player, "Skin-Texture importiert und gespeichert.");
            return;
        }
        if (action.equalsIgnoreCase("clear")) {
            this.getConfig().set(path + ".texture-value", "");
            this.getConfig().set(path + ".texture-signature", "");
            this.saveConfig();
            this.refreshNpcSkins(type, player, "Importierte Texture entfernt; Spielername-Skin wird verwendet.");
            return;
        }
        if (!action.matches("[A-Za-z0-9_]{1,16}")) {
            player.sendMessage(ChatColor.RED + "Ungültiger Spielername. Erlaubt sind 1–16 Zeichen aus A-Z, 0-9 und _.");
            return;
        }
        this.getConfig().set(path + ".name", action);
        this.getConfig().set(path + ".texture-value", "");
        this.getConfig().set(path + ".texture-signature", "");
        this.saveConfig();
        this.refreshNpcSkins(type, player, "Spieler-Skin gespeichert.");
    }

    /** Importiert alle vorhandenen, gueltigen <typ>.yml-Dateien und scannt geladene Chunks nur einmal. */
    private void importAllNpcSkins(Player player) {
        java.io.File directory = new java.io.File(this.getDataFolder(), "npc-skins");
        if (!directory.isDirectory()) {
            player.sendMessage(ChatColor.RED + "Skin-Ordner fehlt: plugins/DuelNetwork/npc-skins/");
            player.sendMessage(ChatColor.GRAY + "Lege dort je NPC-Typ eine Datei wie duell.yml mit texture-value und texture-signature ab.");
            return;
        }
        List<String> importedTypes = new ArrayList<>();
        List<String> rejectedTypes = new ArrayList<>();
        List<String> supportedTypes = new ArrayList<>(DEFAULT_NPC_SKINS.keySet());
        java.util.Collections.sort(supportedTypes);
        for (String type : supportedTypes) {
            java.io.File file = new java.io.File(directory, type + ".yml");
            if (!file.isFile()) continue;
            org.bukkit.configuration.file.YamlConfiguration imported = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            String value = imported.getString("texture-value", "");
            String signature = imported.getString("texture-signature", "");
            if (!this.isValidNpcTexture(value, signature)) {
                rejectedTypes.add(type);
                continue;
            }
            this.getConfig().set("npc-skins." + type + ".texture-value", value);
            this.getConfig().set("npc-skins." + type + ".texture-signature", signature);
            importedTypes.add(type);
        }
        if (importedTypes.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Keine gültigen Skin-Dateien zum Importieren gefunden.");
            player.sendMessage(ChatColor.GRAY + "Dateinamen müssen einem NPC-Typ entsprechen, z. B. duell.yml oder smp.yml.");
            if (!rejectedTypes.isEmpty()) player.sendMessage(ChatColor.RED + "Ungültige Dateien: " + String.join(", ", rejectedTypes));
            return;
        }
        this.saveConfig();
        player.sendMessage(ChatColor.GREEN + "Skin-Sammelimport: " + importedTypes.size() + " Skin(s) gespeichert: " + String.join(", ", importedTypes) + ".");
        if (!rejectedTypes.isEmpty()) player.sendMessage(ChatColor.RED + "Übersprungen (ungültige Texture-Daten): " + String.join(", ", rejectedTypes));
        this.refreshAllNpcSkins(player);
    }

    /** Aktualisiert alle importierten NPC-Typen in einem einzigen Chunk-Batch-Lauf. */
    private void refreshAllNpcSkins(Player requester) {
        List<org.bukkit.Chunk> loadedChunks = new ArrayList<>();
        for (World world : this.getServer().getWorlds()) java.util.Collections.addAll(loadedChunks, world.getLoadedChunks());
        if (loadedChunks.isEmpty()) {
            requester.sendMessage(ChatColor.GRAY + "Keine Chunks geladen; importierte Skins greifen beim nächsten NPC-Spawn.");
            return;
        }
        final int[] chunkIndex = {0};
        final int[] updated = {0};
        final org.bukkit.scheduler.BukkitTask[] task = new org.bukkit.scheduler.BukkitTask[1];
        task[0] = this.getServer().getScheduler().runTaskTimer(this, () -> {
            int batchSize = 0;
            while (chunkIndex[0] < loadedChunks.size() && batchSize++ < 4) {
                org.bukkit.Chunk chunk = loadedChunks.get(chunkIndex[0]++);
                if (!chunk.isLoaded()) continue;
                for (Entity entity : chunk.getEntities()) {
                    String type = entity.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING);
                    if (type == null || entity instanceof Villager || !DEFAULT_NPC_SKINS.containsKey(type)) continue;
                    String texture = this.getConfig().getString("npc-skins." + type + ".texture-value", "");
                    if (texture == null || texture.isBlank()) continue;
                    this.applyNpcSkin(entity, type);
                    ++updated[0];
                }
            }
            if (chunkIndex[0] >= loadedChunks.size()) {
                task[0].cancel();
                if (requester.isOnline()) requester.sendMessage(ChatColor.GREEN + "Skin-Import abgeschlossen; " + updated[0] + " vorhandene NPC(s) aktualisiert.");
            }
        }, 1L, 1L);
    }

    private boolean isValidNpcTexture(String value, String signature) {
        if (value == null || value.isBlank() || value.length() > 16384 || signature == null || signature.length() > 8192) return false;
        try {
            String decoded = new String(java.util.Base64.getDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8);
            if (!decoded.contains("\"textures\"") || !decoded.contains("\"SKIN\"") || !decoded.contains("textures.minecraft.net")) return false;
            return signature.isBlank() || java.util.Base64.getDecoder().decode(signature).length > 0;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /** Skin-Name (Minecraft-Spielername) fuer einen NPC-Typ; per /duelladmin npcskin <typ> <Name> aenderbar. */
    private String npcSkinName(String type) {
        String name = this.getConfig().getString("npc-skins." + type.toLowerCase() + ".name", "");
        if (name == null || name.isBlank()) {
            name = DEFAULT_NPC_SKINS.getOrDefault(type.toLowerCase(), "Steve");
        }
        return name;
    }

    /** Setzt den importierten Skin oder Spielername-Skin sowie Mannequin-Flags. */
    private void applyNpcSkin(Entity npc, String type) {
        String path = "npc-skins." + type.toLowerCase(java.util.Locale.ROOT);
        String textureValue = this.getConfig().getString(path + ".texture-value", "");
        String signature = this.getConfig().getString(path + ".texture-signature", "");
        boolean customApplied = false;
        if (textureValue != null && !textureValue.isBlank()) {
            try {
                customApplied = this.applyImportedNpcTexture(npc, type, textureValue, signature == null ? "" : signature);
            } catch (Throwable t) {
                this.getLogger().warning("Importierter NPC-Skin für " + type + " konnte nicht angewendet werden: " + t.getMessage());
            }
        }
        if (!customApplied) {
            String name = this.npcSkinName(type).replaceAll("[^A-Za-z0-9_]", "");
            if (name.isEmpty()) name = "Steve";
            String command = "data merge entity " + npc.getUniqueId() + " {profile:{name:\"" + name + "\"},immovable:1b,hide_description:1b,Invulnerable:1b}";
            try {
                this.getServer().dispatchCommand((CommandSender)this.getServer().getConsoleSender(), command);
            } catch (Throwable t) {
                this.getLogger().warning("NPC-Skin konnte nicht gesetzt werden: " + t);
            }
        } else {
            String command = "data merge entity " + npc.getUniqueId() + " {immovable:1b,hide_description:1b,Invulnerable:1b}";
            this.getServer().dispatchCommand((CommandSender)this.getServer().getConsoleSender(), command);
        }
        this.styleNpc(npc, type);
    }

    /** Baut ein Paper-ResolvableProfile zur Laufzeit per Reflection, damit ältere 1.21-Server ladbar bleiben. */
    private boolean applyImportedNpcTexture(Entity npc, String type, String value, String signature) throws ReflectiveOperationException {
        if (!this.isValidNpcTexture(value, signature)) return false;
        Class<?> propertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
        Object property = signature == null || signature.isBlank()
            ? propertyClass.getConstructor(String.class, String.class).newInstance("textures", value)
            : propertyClass.getConstructor(String.class, String.class, String.class).newInstance("textures", value, signature);
        Class<?> profileClass = Class.forName("io.papermc.paper.datacomponent.item.ResolvableProfile");
        Class<?> builderClass = Class.forName("io.papermc.paper.datacomponent.item.ResolvableProfile$Builder");
        Object builder = profileClass.getMethod("resolvableProfile").invoke(null);
        builderClass.getMethod("name", String.class).invoke(builder, "DuelNet");
        builderClass.getMethod("uuid", UUID.class).invoke(builder, npc.getUniqueId());
        builderClass.getMethod("addProperty", propertyClass).invoke(builder, property);
        Object profile = builderClass.getMethod("build").invoke(builder);
        npc.getClass().getMethod("setProfile", profileClass).invoke(npc, profile);
        return true;
    }

    /** Aktualisiert nur geladene Chunks und verteilt die Entity-Prüfung über mehrere Ticks. */
    private void refreshNpcSkins(String type, Player requester, String successMessage) {
        List<org.bukkit.Chunk> loadedChunks = new ArrayList<>();
        for (World world : this.getServer().getWorlds()) {
            java.util.Collections.addAll(loadedChunks, world.getLoadedChunks());
        }
        if (loadedChunks.isEmpty()) {
            requester.sendMessage(ChatColor.GREEN + successMessage + " Kein Chunk ist geladen; der Skin greift beim nächsten Spawn.");
            return;
        }
        final int[] chunkIndex = {0};
        final int[] updated = {0};
        final org.bukkit.scheduler.BukkitTask[] task = new org.bukkit.scheduler.BukkitTask[1];
        task[0] = this.getServer().getScheduler().runTaskTimer(this, () -> {
            int batchSize = 0;
            while (chunkIndex[0] < loadedChunks.size() && batchSize++ < 4) {
                org.bukkit.Chunk chunk = loadedChunks.get(chunkIndex[0]++);
                if (!chunk.isLoaded()) continue;
                for (Entity entity : chunk.getEntities()) {
                    if (entity instanceof Villager || !type.equals(entity.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING))) continue;
                    this.applyNpcSkin(entity, type);
                    ++updated[0];
                }
            }
            if (chunkIndex[0] >= loadedChunks.size()) {
                task[0].cancel();
                if (requester.isOnline()) {
                    requester.sendMessage(ChatColor.GREEN + successMessage + " " + updated[0] + " NPC(s) aktualisiert.");
                    if (updated[0] == 0) requester.sendMessage(ChatColor.GRAY + "Der Skin wird bei neu gespawnten NPCs dieses Typs verwendet.");
                }
            }
        }, 1L, 1L);
    }

    private String locationKey(Location location) {
        return String.valueOf(location.getWorld().getUID()) + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private boolean isNpcLocked(String type) {
        return this.getConfig().getBoolean("npcs.locked." + type.toLowerCase(), false);
    }

    private char npcColorCode() {
        String configured = this.getConfig().getString("npcs.name-color", "d");
        return configured == null || configured.isEmpty() ? 'd' : configured.charAt(configured.startsWith("\u00a7") && configured.length() > 1 ? 1 : 0);
    }

    private String npcDisplayName(String type) {
        if (this.isNpcLocked(type)) {
            return "\u00a7c\u00a7lGesperrt";
        }
        String base = "\u00a7" + this.npcColorCode() + "\u00a7l" + this.textForNpc(type);
        if (type.equals("duell") || type.equals("ranked") || type.equals("unranked")) {
            int waiting = this.duelQueues.entrySet().stream().filter(e -> type.equals("duell") || (type.equals("ranked") == isRankedKey(e.getKey()))).mapToInt(e -> e.getValue().size()).sum();
            return base + " \u00a78(\u00a77" + waiting + " warten \u00a78\u2022 \u00a77" + this.activeDuelPlayers.size() / 2 + " laufen\u00a78)";
        }
        if (type.equals("team")) {
            return base + " \u00a78(\u00a77" + TeamQueue.waitingTotal() + " Teams warten\u00a78)";
        }
        return base;
    }

    private static String vanillaColor(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "black"; case '1' -> "dark_blue"; case '2' -> "dark_green"; case '3' -> "dark_aqua";
            case '4' -> "dark_red"; case '5' -> "dark_purple"; case '6' -> "gold"; case '7' -> "gray";
            case '8' -> "dark_gray"; case '9' -> "blue"; case 'a' -> "green"; case 'b' -> "aqua";
            case 'c' -> "red"; case 'e' -> "yellow"; case 'f' -> "white"; default -> "light_purple";
        };
    }

    private boolean console(String command) {
        try {
            return this.getServer().dispatchCommand((CommandSender)this.getServer().getConsoleSender(), command);
        }
        catch (Throwable t) {
            this.getLogger().warning("Befehl fehlgeschlagen (" + command + "): " + t);
            return false;
        }
    }

    /** Entfernt das grosse Text-Schild eines NPC. */
    private void removeNpcLabel(Entity npc) {
        String tag = "dnl_" + npc.getUniqueId();
        for (Entity near : npc.getNearbyEntities(3.0, 6.0, 3.0)) {
            if (near.getScoreboardTags().contains(tag)) {
                near.remove();
            }
        }
    }

    /** Skaliert den NPC und setzt ein einheitlich gefaerbtes, groesseres Namensschild (Text-Display) ueber ihn. */
    private void styleNpc(Entity npc, String type) {
        double scale = this.getConfig().getDouble("npcs.scale", 1.5);
        this.console("attribute " + npc.getUniqueId() + " minecraft:scale base set " + String.format(java.util.Locale.ROOT, "%.2f", scale));
        this.removeNpcLabel(npc);
        String tag = "dnl_" + npc.getUniqueId();
        String text = this.isNpcLocked(type) ? "Gesperrt" : this.textForNpc(type);
        String color = this.isNpcLocked(type) ? "red" : vanillaColor(this.npcColorCode());
        double size = this.getConfig().getDouble("npcs.label-scale", 1.8);
        String sz = String.format(java.util.Locale.ROOT, "%.2f", size);
        String height = String.format(java.util.Locale.ROOT, "%.2f", 1.8 * scale + 0.45);
        String command = "execute as " + npc.getUniqueId() + " at @s run summon minecraft:text_display ~ ~" + height + " ~ {text:{text:\"" + text + "\",color:\"" + color + "\",bold:true},billboard:\"center\",transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],translation:[0f,0f,0f],scale:[" + sz + "f," + sz + "f," + sz + "f]},Tags:[\"dn_label\",\"" + tag + "\"]}";
        boolean ok = this.console(command);
        npc.customName(this.npcName(type));
        npc.setCustomNameVisible(!ok); // klappt das Schild nicht, bleibt das normale (einheitlich gefaerbte) Namensschild
    }

    /** Stylt alle NPCs, entfernt NPC-Typen, die es nicht mehr gibt, und verwaiste Text-Schilder. */
    int styleAllNpcs() {
        List<Entity> npcs = new ArrayList<Entity>();
        List<Entity> labels = new ArrayList<Entity>();
        int removed = 0;
        boolean removeLegacy = this.getConfig().getBoolean("npcs.remove-legacy", true);
        for (World world : this.getServer().getWorlds()) {
            for (Entity entity : new ArrayList<Entity>(world.getEntities())) {
                String type = (String)entity.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING);
                if (type != null) {
                    if (!NPC_TYPES.contains(type) && removeLegacy) {
                        entity.remove();
                        ++removed;
                    } else {
                        npcs.add(entity);
                    }
                } else if (entity.getScoreboardTags().contains("dn_label")) {
                    labels.add(entity);
                }
            }
        }
        for (Entity label : labels) {
            label.remove();
        }
        for (Entity npc : npcs) {
            String type = (String)npc.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING);
            if ("dummy".equals(type)) {
                npc.setInvulnerable(false);
                if (npc instanceof LivingEntity living && living.getAttribute(Attribute.MAX_HEALTH) != null) {
                    living.getAttribute(Attribute.MAX_HEALTH).setBaseValue(2048.0);
                    if (living.getHealth() < 2048.0) living.setHealth(2048.0);
                }
            }
            if (type != null && !(npc instanceof Villager)) {
                this.styleNpc(npc, type);
            }
        }
        if (removed > 0) {
            this.getLogger().info(removed + " alte NPC(s) entfernt (es gibt nur noch SMP, Duel, Bau-SMP, Events und Parkour).");
        }
        return npcs.size();
    }

    private Component npcName(String type) {
        return LegacyComponentSerializer.legacySection().deserialize(this.npcDisplayName(type));
    }

    /** Setzt bei allen NPCs den aktuellen (farbigen) Namen, z.B. nach einem Update. */
    int refreshNpcNames() {
        int count = 0;
        for (World world : this.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                String type = (String)entity.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING);
                if (type == null) continue;
                entity.customName(this.npcName(type));
                entity.setCustomNameVisible(true);
                ++count;
            }
        }
        return count;
    }

    private void setNpcLock(Player admin, String rawType, String rawState) {
        boolean locked;
        String type = rawType.toLowerCase();
        List<String> allowed = NPC_TYPES;
        if (!allowed.contains(type)) {
            admin.sendMessage(String.valueOf(ChatColor.RED) + "Unbekannter NPC-Typ. Erlaubt: " + String.join((CharSequence)", ", allowed));
            return;
        }
        if (rawState.equalsIgnoreCase("on") || rawState.equalsIgnoreCase("lock") || rawState.equalsIgnoreCase("sperren")) {
            locked = true;
        } else if (rawState.equalsIgnoreCase("off") || rawState.equalsIgnoreCase("unlock") || rawState.equalsIgnoreCase("freigeben")) {
            locked = false;
        } else {
            admin.sendMessage(String.valueOf(ChatColor.RED) + "Nutze on/lock/sperren oder off/unlock/freigeben.");
            return;
        }
        this.getConfig().set("npcs.locked." + type, (Object)locked);
        this.saveConfig();
        for (World world : this.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!type.equals(entity.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING))) continue;
                entity.customName(this.npcName(type));
                if (!(entity instanceof Villager)) {
                    this.styleNpc(entity, type);
                }
            }
        }
        admin.sendMessage(String.valueOf(locked ? ChatColor.RED : ChatColor.GREEN) + "NPC " + type + (locked ? " ist jetzt gesperrt." : " ist wieder freigegeben."));
    }

    /*
     * WARNING - void declaration
     */
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        block28: {
            String name;
            block29: {
                block27: {
                    name = command.getName().toLowerCase();
                    if (name.equals("navigator") || name.equals("lobby") || name.equals("smp")) break block27;
                    if (name.equals("events")) {
                        return args.length == 1 ? List.of("join", "leave", "bracket") : List.of();
                    }
                    if (name.equals("parkour")) break block28;
                    if (!name.equals("bausmp")) break block29;
                }
                return List.of();
            }
            if (name.equals("combat")) {
                return List.of();
            }
            if (name.equals("home")) {
                return args.length == 1 ? List.of("list") : List.of();
            }
            if (name.equals("sethome") || name.equals("delhome")) {
                return List.of();
            }
            if (name.equals("tpahere")) {
                return args.length != 1 ? List.of() : this.getServer().getOnlinePlayers().stream().filter(p -> !p.equals((Object)sender) && this.profiles.getOrDefault(p.getUniqueId(), "lobby").equals("smp")).map(Player::getName).collect(Collectors.toList());
            }
            if (name.equals("tpa")) {
                if (args.length != 1) {
                    return List.of();
                }
                ArrayList<String> suggestions = new ArrayList<String>(List.of("accept", "deny", "cancel"));
                this.getServer().getOnlinePlayers().stream().filter(p -> !p.equals((Object)sender) && this.profiles.getOrDefault(p.getUniqueId(), "lobby").equals("smp")).map(Player::getName).forEach(suggestions::add);
                return suggestions;
            }
            if (name.equals("party")) {
                if (args.length == 1) {
                    List<String> subs = new ArrayList<String>(List.of("create", "invite", "accept", "deny", "leave", "list", "kick", "promote", "disband", "duel", "ffa", "team", "help"));
                    String typed0 = args[0].toLowerCase();
                    subs.removeIf(entry -> !entry.startsWith(typed0));
                    return subs;
                }
                if (args.length == 2 && List.of("invite", "add", "kick", "promote", "duel", "1v1").contains(args[0].toLowerCase())) {
                    List<String> names = new ArrayList<String>();
                    String typed1 = args[1].toLowerCase();
                    for (Player online : this.getServer().getOnlinePlayers()) {
                        if (!online.getName().equals(sender.getName()) && online.getName().toLowerCase().startsWith(typed1)) {
                            names.add(online.getName());
                        }
                    }
                    return names;
                }
                if (args.length >= 3 && (args[0].equalsIgnoreCase("duel") || args[0].equalsIgnoreCase("1v1")) || args.length == 2 && (args[0].equalsIgnoreCase("ffa") || args[0].equalsIgnoreCase("team"))) {
                    return this.getConfig().getStringList("kits.available");
                }
                return List.of();
            }
            if (name.equals("kit")) {
                return args.length == 1 ? List.of("list", "give", "axe", "sword", "mace", "uhc", "op", "smp", "crystal") : List.of();
            }
            if (name.equals("rank")) {
                if (args.length == 1) {
                    return this.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
                }
                if (args.length == 2) {
                    return List.of("Media", "Owner", "Admin", "Premium", "Tester", "reset");
                }
                return List.of();
            }
            if (name.equals("duel")) {
                if (args.length != 1) {
                    return List.of();
                }
                ArrayList<String> duelSuggestions = new ArrayList<String>(List.of("accept", "deny", "cancel", "leave", "top", "stats"));
                for (Player online : this.getServer().getOnlinePlayers()) {
                    if (!online.getName().equals(sender.getName())) {
                        duelSuggestions.add(online.getName());
                    }
                }
                String typed = args[0].toLowerCase();
                duelSuggestions.removeIf(entry -> !entry.toLowerCase().startsWith(typed));
                return duelSuggestions;
            }
            if (!name.equals("duelladmin")) {
                return List.of();
            }
            if (args.length == 1) {
                return List.of("help", "diagnose", "reload", "icon", "npc", "npcskin", "npcfix", "records", "npclock", "setlobby", "setspawn", "setduelspawn", "seteventspawn", "kit", "kitedit", "arena", "event", "game", "egg", "eco", "unlock", "lock", "parkour", "finish");
            }
            String root = args[0].toLowerCase();
            if (root.equals("icon")) {
                List<String> out = new ArrayList<String>();
                if (args.length == 2) {
                    out.addAll(List.of("smp", "duell", "creative", "events", "parkour"));
                    ConfigurationSection pk = this.getConfig().getConfigurationSection("parkour.maps");
                    if (pk != null) {
                        for (String map : pk.getKeys(false)) {
                            out.add("parkour:" + map);
                        }
                    }
                    for (String kit : this.getConfig().getStringList("kits.available")) {
                        out.add("kit:" + kit.replace(' ', '_'));
                    }
                } else if (args.length == 3) {
                    out.addAll(List.of("hand", "reset"));
                    String typedMaterial = args[2].toUpperCase();
                    if (typedMaterial.length() >= 2) {
                        for (Material material : Material.values()) {
                            if (material.name().startsWith(typedMaterial) && out.size() < 40) {
                                out.add(material.name());
                            }
                        }
                    }
                }
                String typedKey = args[args.length - 1].toLowerCase();
                out.removeIf(entry -> !entry.toLowerCase().startsWith(typedKey));
                return out;
            }
            if (args.length == 2 && root.equals("npc")) {
                return NPC_TYPES;
            }
            if (args.length == 2 && root.equals("npcskin")) {
                List<String> skinTargets = new ArrayList<String>(DEFAULT_NPC_SKINS.keySet());
                skinTargets.add("all");
                return skinTargets;
            }
            if (args.length == 3 && root.equals("npcskin")) {
                return List.of("import", "clear");
            }
            if (args.length == 2 && root.equals("npclock")) {
                return NPC_TYPES;
            }
            if (args.length == 3 && root.equals("npclock")) {
                return List.of("on", "off", "lock", "unlock", "sperren", "freigeben");
            }
            if (args.length == 2 && root.equals("kit")) {
                return List.of("create", "save", "delete", "list");
            }
            if (args.length == 2 && root.equals("arena")) {
                return List.of("create", "delete", "list", "check", "pos1", "pos2");
            }
            if (args.length == 2 && (root.equals("unlock") || root.equals("lock"))) {
                return root.equals("unlock") ? List.of("start", "status", "nether", "end") : List.of("nether", "end");
            }
            if (root.equals("unlock") && args.length == 3 && args[1].equalsIgnoreCase("start")) {
                return List.of("nether", "end");
            }
            if (root.equals("unlock") && args.length == 3 && (args[1].equalsIgnoreCase("nether") || args[1].equalsIgnoreCase("end"))) {
                return List.of("start");
            }
            if (root.equals("unlock") && args.length == 4) {
                return List.of("24h", "1d", "12h", "6h", "60m");
            }
            if (args.length == 2 && root.equals("eco")) {
                return List.of("give", "take", "set", "check");
            }
            if (args.length == 2 && root.equals("event")) {
                return Events.SUBS;
            }
            if (args.length == 3 && root.equals("event") && !args[1].equalsIgnoreCase("template") && !args[1].equalsIgnoreCase("templates") && !args[1].equalsIgnoreCase("add") && !args[1].equalsIgnoreCase("list") && !args[1].equalsIgnoreCase("stop")) {
                return Events.ids();
            }
            if (args.length == 4 && root.equals("event") && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("time"))) {
                return List.of("heute", "morgen");
            }
            if (root.equals("event") && args.length >= 3) {
                String sub = args[1].toLowerCase(java.util.Locale.ROOT);
                List<String> times = List.of("18:00", "19:00", "20:00", "21:00");
                List<String> minutes = List.of("20", "30", "45", "60", "90", "120");
                if (sub.equals("template")) {
                    if (args.length == 3) {
                        return Events.templateIds();
                    }
                    return args.length == 4 ? List.of("heute", "morgen") : args.length == 5 ? times : List.of();
                }
                if (sub.equals("game") && args.length == 4) {
                    return List.of("ffa", "turnier", "none");
                }
                if (sub.equals("game") && args.length >= 5) {
                    return this.kitTokens(args, 4);
                }
                if (sub.equals("quick") && args.length == 4) {
                    return minutes;
                }
                if (sub.equals("duration") && args.length == 4) {
                    return minutes;
                }
                if ((sub.equals("add") || sub.equals("time")) && args.length == 5) {
                    return times;
                }
            }
            if (root.equals("game")) {
                if (args.length == 2) {
                    return List.of("ffa", "turnier", "startnow", "stop", "status");
                }
                String type = args[1].toLowerCase(java.util.Locale.ROOT);
                if ((type.equals("ffa") || type.equals("turnier")) && args.length >= 3) {
                    List<String> kitWords = this.kitTokens(args, 2);
                    if (kitWords.isEmpty() || args.length >= 4) {
                        ArrayList<String> merged = new ArrayList<String>(kitWords);
                        merged.addAll(List.of("5", "10", "15", "30"));
                        return merged;
                    }
                    return kitWords;
                }
            }
            if (root.equals("setduelspawn")) {
                if (args.length == 2) {
                    ConfigurationSection arenaSection = this.getConfig().getConfigurationSection("duels.arenas");
                    return arenaSection == null ? List.of() : new ArrayList<String>(arenaSection.getKeys(false));
                }
                if (args.length == 3) {
                    return List.of("1", "2");
                }
                if (args.length == 4) {
                    return List.of("2", "3", "4");
                }
            }
            if (args.length == 2 && root.equals("setspawn")) {
                return List.of("smp", "duell", "creative", "events", "parkour");
            }
            if (args.length == 2 && root.equals("parkour")) {
                return List.of("create", "delete", "list", "icon", "start", "end", "checkpoint", "fall1", "fall2", "check", "records", "top", "stats");
            }
            if (args.length == 4 && root.equals("parkour") && args[1].equalsIgnoreCase("icon")) {
                return List.of("hand", "reset", "DIAMOND_BOOTS", "FEATHER", "ELYTRA", "SLIME_BLOCK", "ENDER_PEARL", "GOLDEN_BOOTS");
            }
            if (args.length == 3 && root.equals("parkour")) {
                ConfigurationSection section = this.getConfig().getConfigurationSection("parkour.maps");
                return section == null ? List.of() : new ArrayList(section.getKeys(false));
            }
            if (root.equals("records")) {
                if (args.length == 2) {
                    ArrayList<String> first = new ArrayList<String>(List.of("list", "reset", "map", "gui"));
                    this.getServer().getOnlinePlayers().forEach(online -> first.add(online.getName()));
                    return first;
                }
                String mode = args[1].toLowerCase();
                if (args.length == 3 && (mode.equals("list") || mode.equals("reset") || mode.equals("gui"))) {
                    return this.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
                }
                if (args.length == 3 && mode.equals("map") || args.length == 4 && mode.equals("reset")) {
                    ConfigurationSection mapSection = this.getConfig().getConfigurationSection("parkour.maps");
                    ArrayList<String> names = new ArrayList<String>();
                    if (mapSection != null) {
                        names.addAll(mapSection.getKeys(false));
                    }
                    if (mode.equals("reset")) {
                        names.add("all");
                    }
                    return names;
                }
                return List.of();
            }
            return List.of();
        }
        return ParkourEnhancements.complete(this, args);
    }

    private void handleKitAdmin(Player player, String action, String name) {
        String kitName = this.internalKitName(name);
        if (action.equalsIgnoreCase("list")) {
            List kits = this.getConfig().getStringList("kits.available");
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Kits: " + (kits.isEmpty() ? "keine" : String.join((CharSequence)", ", kits)));
            return;
        }
        if (name.isBlank()) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Nutze: /duelladmin kit <create|save|delete|list> <name>");
            return;
        }
        String path = "kits.data." + kitName;
        if (action.equalsIgnoreCase("create")) {
            this.getConfig().set(path + ".items", List.of());
            this.addToList("kits.available", name.toLowerCase());
            this.saveConfig();
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Kit erstellt. R\u00fcste dich aus und nutze /duelladmin kit save " + kitName + ".");
        } else if (action.equalsIgnoreCase("save")) {
            if (!this.getConfig().contains(path)) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Dieses Kit gibt es nicht. Erstelle es zuerst.");
                return;
            }
            ArrayList<ItemStack> items = new ArrayList<ItemStack>();
            for (ItemStack item : player.getInventory().getContents()) {
                items.add(item == null ? new ItemStack(Material.AIR) : item.clone());
            }
            this.getConfig().set(path + ".items", items);
            this.saveConfig();
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Ausr\u00fcstung f\u00fcr Kit " + kitName + " gespeichert.");
        } else if (action.equalsIgnoreCase("delete")) {
            this.getConfig().set(path, null);
            List<String> kits = new ArrayList<String>(this.getConfig().getStringList("kits.available"));
            kits.removeIf(value -> value.equalsIgnoreCase(kitName));
            this.getConfig().set("kits.available", (Object)kits);
            this.saveConfig();
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Kit gel\u00f6scht: " + kitName);
        }
    }

    private void handleArenaAdmin(Player player, String action, String name) {
        if (action.equalsIgnoreCase("list")) {
            ConfigurationSection section = this.getConfig().getConfigurationSection("duels.arenas");
            if (section == null || section.getKeys(false).isEmpty()) {
                player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Arenen: keine");
                return;
            }
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Arenen und Status:");
            for (String arena : section.getKeys(false)) {
                String status = this.arenaValidationMessage(arena);
                player.sendMessage(String.valueOf(status.startsWith("OK") ? ChatColor.GREEN : ChatColor.RED) + arena + ": " + status);
            }
            return;
        }
        if (name.isBlank()) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Nutze: /duelladmin arena <create|delete|list|check> <name>");
            return;
        }
        String path = "duels.arenas." + name.toLowerCase();
        if (action.equalsIgnoreCase("create")) {
            this.getConfig().set(path + ".enabled", (Object)true);
            this.getConfig().set(path + ".inUse", (Object)false);
            this.arenaSnapshots.remove(name.toLowerCase());
            this.saveConfig();
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Arena erstellt. Setze jetzt pos1, pos2, spawn1 und spawn2.");
        } else if (action.equalsIgnoreCase("pos1") || action.equalsIgnoreCase("pos2")) {
            this.saveLocation(path + "." + action.toLowerCase(), player.getLocation());
            this.arenaSnapshots.remove(name.toLowerCase());
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Arena-Resetposition " + action.toUpperCase() + " f\u00fcr " + name + " gespeichert.");
        } else if (action.equalsIgnoreCase("check")) {
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Arena " + name + ": " + this.arenaValidationMessage(name));
        } else if (action.equalsIgnoreCase("delete")) {
            this.getConfig().set(path, null);
            this.arenaSnapshots.remove(name.toLowerCase());
            this.saveConfig();
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Arena gel\u00f6scht: " + name);
        } else {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Nutze: /duelladmin arena <create|delete|list|check> <name>");
        }
    }

    private String arenaValidationMessage(String arena) {
        String[] fields;
        String path = "duels.arenas." + arena.toLowerCase();
        if (!this.getConfig().contains(path)) {
            return "FEHLER: Arena existiert nicht";
        }
        if (!this.getConfig().getBoolean(path + ".enabled", true)) {
            return "FEHLER: enabled=false";
        }
        if (this.getConfig().getBoolean(path + ".inUse", false)) {
            return "FEHLER: inUse=true (alte Runde h\u00e4ngt fest; auf false setzen)";
        }
        for (String field : fields = new String[]{"pos1", "pos2", "spawn1", "spawn2"}) {
            if (!this.getConfig().contains(path + "." + field + ".world")) {
                return "FEHLER: " + field + " fehlt";
            }
            for (String coordinate : new String[]{"x", "y", "z"}) {
                if (!this.getConfig().contains(path + "." + field + "." + coordinate)) {
                    return "FEHLER: Koordinate " + coordinate + " von " + field + " fehlt";
                }
            }
            Location location = this.readLocation(path + "." + field);
            if (location != null && location.getWorld() != null) continue;
            return "FEHLER: Welt von " + field + " ist nicht geladen";
        }
        Location first = this.readLocation(path + ".pos1");
        Location second = this.readLocation(path + ".pos2");
        Location spawn1 = this.readLocation(path + ".spawn1");
        Location spawn2 = this.readLocation(path + ".spawn2");
        if (!first.getWorld().equals((Object)second.getWorld())) {
            return "FEHLER: pos1 und pos2 sind in verschiedenen Welten";
        }
        if (!first.getWorld().equals((Object)spawn1.getWorld()) || !first.getWorld().equals((Object)spawn2.getWorld())) {
            return "FEHLER: Spawns und Resetbereich sind in verschiedenen Welten";
        }
        long width = Math.abs((long)first.getBlockX() - second.getBlockX()) + 1L;
        long height = Math.abs((long)first.getBlockY() - second.getBlockY()) + 1L;
        long depth = Math.abs((long)first.getBlockZ() - second.getBlockZ()) + 1L;
        long volume;
        try {
            volume = Math.multiplyExact(Math.multiplyExact(width, height), depth);
        }
        catch (ArithmeticException overflow) {
            return "FEHLER: Resetbereich kann wegen Koordinaten-Ueberlauf nicht berechnet werden";
        }
        return "OK: vollst\u00e4ndig (" + volume + " Resetbl\u00f6cke, kein Gr\u00f6\u00dfenlimit)";
    }

    private void addToList(String path, String value) {
        List values = this.getConfig().getStringList(path);
        if (!values.contains(value)) {
            values.add(value);
        }
        this.getConfig().set(path, (Object)values);
    }

    /** Reiner Anzeigename eines NPC-Typs (ohne Farbe). */
    private String textForNpc(String type) {
        return switch (type) {
            case "smp" -> "SMP";
            case "duell" -> "Duelle";
            case "ranked" -> "Ranked";
            case "unranked" -> "Unranked";
            case "team" -> "Teams";
            case "dummy" -> "Trainings-Dummy";
            case "events" -> "Events";
            case "parkour" -> "Parkour";
            case "creative" -> "Bau-SMP";
            default -> "Men\u00fc";
        };
    }

    private void saveLocation(String path, Location location) {
        this.getConfig().set(path + ".world", (Object)location.getWorld().getName());
        this.getConfig().set(path + ".x", (Object)location.getX());
        this.getConfig().set(path + ".y", (Object)location.getY());
        this.getConfig().set(path + ".z", (Object)location.getZ());
        this.getConfig().set(path + ".yaw", (Object)Float.valueOf(location.getYaw()));
        this.getConfig().set(path + ".pitch", (Object)Float.valueOf(location.getPitch()));
        this.saveConfig();
    }

    private void clearTpaState(Player player) {
        UUID id = player.getUniqueId();
        this.tpahereRequests.remove(id);
        this.tpahereRequests.values().removeIf(id::equals);
        UUID target = this.outgoingTpaRequests.remove(id);
        if (target != null) {
            this.tpaRequests.remove(target, id);
            this.clearTpaExpiry(target);
        }
        UUID requester = this.tpaRequests.remove(id);
        this.clearTpaExpiry(id);
        if (requester != null) {
            this.outgoingTpaRequests.remove(requester, id);
        }
        this.cancelDelayedTeleport(player, false);
    }

    private void resetPlayerState(Player player) {
        if (player == null) {
            return;
        }
        Attribute attribute = Attribute.MAX_HEALTH;
        if (player.getAttribute(attribute) != null) {
            player.setHealth(Math.max(1.0, player.getAttribute(attribute).getValue()));
        }
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setExhaustion(0.0f);
        player.setAbsorptionAmount(0.0);
        player.setFireTicks(0);
        player.setFreezeTicks(0);
        player.setFallDistance(0.0f);
        player.setNoDamageTicks(0);
        player.setInvulnerable(false);
        for (PotionEffect effect : new ArrayList<PotionEffect>(player.getActivePotionEffects())) {
            player.removePotionEffect(effect.getType());
        }
    }

    /** true, wenn der Spieler gerade eben (letzte 3 Sekunden) schon zur Lobby zurueckgebracht wurde. */
    boolean recentlyReturned(UUID id) {
        Long at = this.lastLobbyReturn.get(id);
        return at != null && System.currentTimeMillis() - at < 3000L;
    }

    private void guaranteeDuelLobbyReturn(Player player) {
        Location center;
        if (player == null || !player.isOnline()) {
            return;
        }
        UUID id = player.getUniqueId();
        // Sicherheitsnetz: nur eingreifen, wenn der Spieler noch im alten Duel-Ende haengt - nicht noch einmal teleportieren
        boolean stuck = player.isDead() || this.pendingDuelLosers.contains(id) || this.duelResultPlayers.contains(id);
        if (!stuck) {
            return;
        }
        this.pendingDuelLosers.remove(id);
        this.duelResultPlayers.remove(id);
        this.duelDeathLocations.remove(id);
        this.pendingDuelRespawns.put(id, this.lobbyCenter());
        if (player.isDead()) {
            return;
        }
        player.setGameMode(GameMode.SURVIVAL);
        this.profiles.put(id, "lobby");
        this.resetPlayerState(player);
        if (!this.teleportToLobby(player) && (center = this.lobbyCenter()) != null && center.getWorld() != null) {
            player.teleport(center);
        }
        this.pendingDuelRespawns.remove(id);
    }

    private void scheduleGuaranteedDuelLobbyReturn(Player player) {
        if (player == null) {
            return;
        }
        this.getServer().getScheduler().runTaskLater((Plugin)this, () -> this.guaranteeDuelLobbyReturn(player), 60L);
    }

    void switchProfile(Player player, String profile) {
        UUID id = player.getUniqueId();
        if (this.activeDuelPlayers.contains(id)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Du kannst w\u00e4hrend einer laufenden Runde nicht die Lobby wechseln.");
            return;
        }
        String current = this.profiles.getOrDefault(id, "lobby");
        if (this.bauMode.contains(id)) {
            // Bau-SMP verlassen: zuerst das normale SMP-Inventar zurueck
            this.leaveBauMode(player, profile.equals("smp"));
            if (profile.equals("smp")) {
                return;
            }
        }
        if (current.equals(profile)) {
            if (profile.equals("lobby")) {
                player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Du bist bereits in der Lobby.");
            } else {
                player.sendMessage(String.valueOf(ChatColor.GREEN) + "Du bist bereits im SMP-Profil.");
            }
            return;
        }
        if (current.equals("smp") && !profile.equals("smp")) {
            DragonEgg.dropCarried(player);
            this.clearSmpCombat(player);
            this.clearTpaState(player);
        }
        if (current.equals("smp")) {
            this.saveInventory(id, player, this.smpItems, this.smpArmor);
            this.persistProfile(id, "smp", this.smpItems.get(id), this.smpArmor.get(id));
        } else {
            this.saveInventory(id, player, this.lobbyItems, this.lobbyArmor);
            this.persistProfile(id, "lobby", this.lobbyItems.get(id), this.lobbyArmor.get(id));
        }
        this.restoreInventory(id, player, profile.equals("smp") ? this.smpItems : this.lobbyItems, profile.equals("smp") ? this.smpArmor : this.lobbyArmor);
        this.profiles.put(id, profile);
        if (profile.equals("lobby")) {
            this.resetPlayerState(player);
        }
        this.getConfig().set("players." + String.valueOf(id) + ".active", (Object)profile);
        this.saveConfig();
        player.sendMessage(profile.equals("smp") ? String.valueOf(ChatColor.GREEN) + "SMP-Inventar geladen." : String.valueOf(ChatColor.LIGHT_PURPLE) + "Lobby-Inventar geladen.");
    }

    private Map<UUID, ItemStack> offhandMap(Map<UUID, ItemStack[]> items) {
        return items == this.smpItems ? this.smpOffhand : items == this.bauItems ? this.bauOffhand : this.lobbyOffhand;
    }

    boolean inBauMode(UUID id) {
        return this.bauMode.contains(id);
    }

    /** SMP -> Bau-SMP: eigenes Inventar (das SMP-Inventar wird gesichert). */
    void enterBauMode(Player player) {
        UUID id = player.getUniqueId();
        if (this.bauMode.contains(id) || !"smp".equals(this.profiles.getOrDefault(id, "lobby")) || this.activeDuelPlayers.contains(id)) {
            return;
        }
        this.clearSmpCombat(player);
        this.saveInventory(id, player, this.smpItems, this.smpArmor);
        this.persistProfile(id, "smp", this.smpItems.get(id), this.smpArmor.get(id));
        this.restoreInventory(id, player, this.bauItems, this.bauArmor);
        this.bauMode.add(id);
        Economy.ensureAccount(id, player.getName());
        player.sendMessage(String.valueOf(ChatColor.YELLOW) + "Bau-SMP-Inventar geladen. " + String.valueOf(ChatColor.GRAY) + "(Dein SMP-Inventar ist sicher gespeichert.)");
    }

    /** Bau-SMP -> SMP (oder Lobby/Quit): Bau-Inventar sichern, SMP-Inventar zurueck. */
    void leaveBauMode(Player player, boolean message) {
        UUID id = player.getUniqueId();
        if (!this.bauMode.remove(id)) {
            return;
        }
        this.saveInventory(id, player, this.bauItems, this.bauArmor);
        this.persistProfile(id, "bau", this.bauItems.get(id), this.bauArmor.get(id));
        this.restoreInventory(id, player, this.smpItems, this.smpArmor);
        if (message) {
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "SMP-Inventar geladen.");
        }
    }

    private void saveInventory(UUID id, Player player, Map<UUID, ItemStack[]> items, Map<UUID, ItemStack[]> armor) {
        items.put(id, this.cloneItems(player.getInventory().getStorageContents()));
        armor.put(id, this.cloneItems(player.getInventory().getArmorContents()));
        this.offhandMap(items).put(id, player.getInventory().getItemInOffHand().clone());
        this.saveXp(id, player, items);
    }

    private void restoreInventory(UUID id, Player player, Map<UUID, ItemStack[]> items, Map<UUID, ItemStack[]> armor) {
        player.getInventory().clear();
        ItemStack[] storedItems = items.get(id);
        ItemStack[] storedArmor = armor.get(id);
        if (storedItems != null) {
            player.getInventory().setStorageContents(this.cloneItems(storedItems));
        }
        if (storedArmor != null) {
            player.getInventory().setArmorContents(this.cloneItems(storedArmor));
        }
        ItemStack offhand = this.offhandMap(items).get(id);
        player.getInventory().setItemInOffHand(offhand == null ? new ItemStack(Material.AIR) : offhand.clone());
        this.restoreXp(id, player, items);
    }

    private ItemStack[] cloneItems(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; ++i) {
            copy[i] = source[i] == null ? null : source[i].clone();
        }
        return copy;
    }

    private String profilePath(UUID id, String profile) {
        return "players." + String.valueOf(id) + "." + profile;
    }

    private void persistProfile(UUID id, String profile, ItemStack[] items, ItemStack[] armor) {
        String path = this.profilePath(id, profile);
        this.getConfig().set(path + ".items", Arrays.asList(items == null ? new ItemStack[]{} : items));
        this.getConfig().set(path + ".armor", Arrays.asList(armor == null ? new ItemStack[]{} : armor));
        float[] xp = profile.equals("smp") ? this.smpXp.get(id) : profile.equals("bau") ? this.bauXp.get(id) : null;
        if (xp != null) {
            this.getConfig().set(path + ".level", (Object)Integer.valueOf((int)xp[0]));
            this.getConfig().set(path + ".exp", (Object)Double.valueOf(xp[1]));
        }
        this.saveConfig();
    }

    private void loadPersistedProfiles(UUID id) {
        for (String profile : List.of("lobby", "smp", "bau")) {
            String path = this.profilePath(id, profile);
            List items = this.getConfig().getList(path + ".items");
            List armor = this.getConfig().getList(path + ".armor");
            if (this.getConfig().contains(path + ".level") && (profile.equals("smp") || profile.equals("bau"))) {
                (profile.equals("smp") ? this.smpXp : this.bauXp).put(id, new float[]{this.getConfig().getInt(path + ".level"), (float)this.getConfig().getDouble(path + ".exp", 0.0)});
            }
            if (items != null) {
                this.lobbyOrSmpItems(id, profile, items, false);
            }
            if (armor == null) continue;
            this.lobbyOrSmpItems(id, profile, armor, true);
        }
    }

    private void lobbyOrSmpItems(UUID id, String profile, List<?> entries, boolean armor) {
        ItemStack[] result = new ItemStack[entries.size()];
        for (int i = 0; i < entries.size(); ++i) {
            ItemStack itemStack;
            Object obj = entries.get(i);
            if (obj instanceof ItemStack) {
                ItemStack stack = (ItemStack)obj;
                itemStack = stack.clone();
            } else {
                itemStack = new ItemStack(Material.AIR);
            }
            result[i] = itemStack;
        }
        if (profile.equals("bau")) {
            (armor ? this.bauArmor : this.bauItems).put(id, result);
        } else if (profile.equals("smp")) {
            if (armor) {
                this.smpArmor.put(id, result);
            } else {
                this.smpItems.put(id, result);
            }
        } else if (armor) {
            this.lobbyArmor.put(id, result);
        } else {
            this.lobbyItems.put(id, result);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        this.loadPersistedProfiles(id);
        // Grenzansicht sofort (nicht erst im Sekundentakt) setzen, damit beim Beitritt kein roter Rand aufblitzt
        this.getServer().getScheduler().runTaskLater((Plugin)this, () -> this.refreshBorderView(), 2L);
        this.getServer().getScheduler().runTaskLater((Plugin)this, () -> this.refreshBorderView(), 10L);
        String savedRank = this.getConfig().getString("ranks." + String.valueOf(id), "");
        if (savedRank.isBlank()) {
            this.playerRanks.remove(id);
        } else {
            this.playerRanks.put(id, savedRank);
        }
        this.applyRankDisplay(event.getPlayer());
        this.applyStaffPermissions(event.getPlayer());
        this.updateTabOrder();
        if (event.getPlayer().hasPermission("duelnetwork.admin")) {
            final Player adminJoined = event.getPlayer();
            this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
                if (adminJoined.isOnline()) {
                    this.adminSetupHints(adminJoined);
                }
            }, 60L);
        }
        Player joining = event.getPlayer();
        // Einmalige Uebernahme: bisheriges XP (vor dem getrennten XP-System) gehoert dem SMP
        if (!this.getConfig().contains(this.profilePath(id, "smp") + ".level") && !this.smpXp.containsKey(id)) {
            this.smpXp.put(id, new float[]{joining.getLevel(), joining.getExp()});
            this.persistProfile(id, "smp", this.smpItems.get(id), this.smpArmor.get(id));
        }
        this.profiles.put(id, "lobby");
        this.restoreInventory(id, joining, this.lobbyItems, this.lobbyArmor);
        this.resetPlayerState(joining);
        // Ein gespeichertes SMP-Profil bleibt fuer den naechsten bewussten Wechsel erhalten;
        // der Einstiegspunkt nach jedem Join ist unabhaengig davon immer die Lobby.
        this.getConfig().set("players." + String.valueOf(id) + ".active", (Object)"lobby");
        this.saveConfig();
        this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
            if (!joining.isOnline()) return;
            if (!this.teleportToLobby(joining)) {
                joining.sendMessage(String.valueOf(ChatColor.RED) + "Die Lobby ist noch nicht eingerichtet. Bitte einen Admin um /duelladmin diagnose.");
            }
        }, 1L);
    }

    @EventHandler
    public void onRankChat(AsyncPlayerChatEvent event) {
        Player chatPlayer = event.getPlayer();
        if (this.pendingHomeSlots.remove(chatPlayer.getUniqueId()) != null) {
            event.setCancelled(true);
            String requested = event.getMessage().trim();
            if (requested.equalsIgnoreCase("cancel")) {
                this.getServer().getScheduler().runTask((Plugin)this, () -> {
                    if (chatPlayer.isOnline()) {
                        chatPlayer.sendMessage(String.valueOf(ChatColor.YELLOW) + "Home-Erstellung abgebrochen.");
                    }
                });
                return;
            }
            this.getServer().getScheduler().runTask((Plugin)this, () -> this.handleHome(chatPlayer, new String[]{requested, "set"}));
            return;
        }
        String prefix = this.rankPrefix(event.getPlayer().getUniqueId());
        if (!prefix.isBlank()) {
            event.setFormat(prefix + String.valueOf(ChatColor.WHITE) + event.getPlayer().getName().replace("%", "%%") + String.valueOf(ChatColor.GRAY) + ": " + String.valueOf(ChatColor.WHITE) + "%2$s");
        }
    }

    @EventHandler
    public void onDelayedTeleportMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }
        if (this.pendingDuelLosers.contains(event.getPlayer().getUniqueId())) {
            Location destination = event.getTo();
            boolean blocked = destination.getBlock().getType().isSolid() || destination.clone().add(0.0, 1.0, 0.0).getBlock().getType().isSolid();
            boolean bl = blocked;
            if (blocked) {
                event.setCancelled(true);
                return;
            }
        }
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() && event.getFrom().getBlockY() == event.getTo().getBlockY() && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        this.cancelDelayedTeleport(event.getPlayer(), true);
    }

    private long combatRemainingSeconds(Player player) {
        Long until = this.smpCombatUntil.get(player.getUniqueId());
        if (until == null) {
            return 0L;
        }
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0L) {
            this.clearSmpCombat(player);
            return 0L;
        }
        return (remaining + 999L) / 1000L;
    }

    boolean isInSmpCombat(Player player) {
        Long until = this.smpCombatUntil.get(player.getUniqueId());
        if (until == null) {
            return false;
        }
        if (until <= System.currentTimeMillis()) {
            this.clearSmpCombat(player);
            return false;
        }
        return true;
    }

    private void clearSmpCombat(Player player) {
        UUID id = player.getUniqueId();
        this.smpCombatUntil.remove(id);
        Integer task = this.smpCombatTasks.remove(id);
        Integer oldLevel = this.combatXpLevels.remove(id);
        Float oldProgress = this.combatXpProgress.remove(id);
        if (oldLevel != null && player.isOnline()) {
            player.setLevel(oldLevel.intValue());
            player.setExp(oldProgress == null ? 0.0f : oldProgress.floatValue());
        }
        if (task != null) {
            this.getServer().getScheduler().cancelTask(task.intValue());
        }
        if (player.isOnline()) {
            player.sendActionBar((Component)Component.text((String)""));
        }
    }

    private void markSmpCombat(Player player, double damage, boolean hit) {
        if (!player.isOnline() || !this.profiles.getOrDefault(player.getUniqueId(), "lobby").equals("smp") || this.activeDuelPlayers.contains(player.getUniqueId())) {
            return;
        }
        long now = System.currentTimeMillis();
        UUID combatId = player.getUniqueId();
        long current = Math.max(now, this.smpCombatUntil.getOrDefault(combatId, now));
        long base = Math.max(1, this.getConfig().getInt("smp.combat.base-seconds", 30));
        long perHeart = Math.max(0, this.getConfig().getInt("smp.combat.seconds-per-heart", 10));
        long perHit = hit ? (long)Math.max(0, this.getConfig().getInt("smp.combat.seconds-per-hit", 3)) : 0L;
        long fullHearts = (long)Math.floor(Math.max(0.0, damage) / 2.0);
        long extension = (perHit + fullHearts * perHeart) * 1000L;
        if (current == now) {
            current += base * 1000L;
        }
        this.smpCombatUntil.put(player.getUniqueId(), current + extension);
        Integer oldTask = this.smpCombatTasks.remove(player.getUniqueId());
        if (oldTask != null) {
            this.getServer().getScheduler().cancelTask(oldTask.intValue());
        }
        int task = this.getServer().getScheduler().runTaskTimer((Plugin)this, () -> {
            long remaining = this.smpCombatUntil.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis();
            if (remaining <= 0L || !player.isOnline() || !this.profiles.getOrDefault(player.getUniqueId(), "lobby").equals("smp")) {
                this.clearSmpCombat(player);
                return;
            }
            long seconds = (remaining + 999L) / 1000L;
            player.sendActionBar((Component)Component.text((String)(String.valueOf(ChatColor.RED) + "\u2694 COMBAT " + String.valueOf(ChatColor.DARK_GRAY) + "\u2022 " + String.valueOf(ChatColor.WHITE) + seconds + "s")));
        }, 0L, 20L).getTaskId();
        this.smpCombatTasks.put(player.getUniqueId(), task);
    }

    @EventHandler
    public void onSmpCombat(EntityDamageEvent event) {
        Projectile projectile;
        ProjectileSource projectileSource;
        Entity entity;
        EntityDamageByEntityEvent byEntity;
        block8: {
            block7: {
                if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent)) break block7;
                byEntity = (EntityDamageByEntityEvent)event;
                entity = event.getEntity();
                if (entity instanceof Player) break block8;
            }
            return;
        }
        Player victim = (Player)entity;
        Player attacker = null;
        Entity damager = byEntity.getDamager();
        if (damager instanceof Player) {
            Player direct;
            attacker = direct = (Player)damager;
        } else if (damager instanceof Projectile && (projectileSource = (projectile = (Projectile)damager).getShooter()) instanceof Player) {
            Player shooter;
            attacker = shooter = (Player)projectileSource;
        }
        if (attacker == null || !this.profiles.getOrDefault(victim.getUniqueId(), "lobby").equals("smp") || !this.profiles.getOrDefault(attacker.getUniqueId(), "lobby").equals("smp") || this.activeDuelPlayers.contains(victim.getUniqueId()) || this.activeDuelPlayers.contains(attacker.getUniqueId())) {
            return;
        }
        this.markSmpCombat(victim, event.getFinalDamage(), true);
        this.markSmpCombat(attacker, 0.0, true);
    }

    /** Entfernt eine Einladung nur fuer das passende Herausforderer/Ziel-Paar. */
    private boolean cancelDuelInvite(UUID challengerId, UUID targetId) {
        boolean removed = this.duelInvites.remove(targetId, challengerId);
        removed |= this.outgoingDuelInvites.remove(challengerId, targetId);
        if (removed) {
            this.selectedDuelKits.remove(challengerId);
            this.selectedDuelKits.remove(targetId);
            this.duelHosts.remove(challengerId, challengerId);
            this.duelHosts.remove(targetId, challengerId);
        }
        return removed;
    }

    private void cancelDuelInvitesFor(UUID id, String leaverName) {
        UUID challengerId = this.duelInvites.get(id);
        if (challengerId != null && this.cancelDuelInvite(challengerId, id)) {
            Player challenger = this.getServer().getPlayer(challengerId);
            if (challenger != null) challenger.sendMessage("§7Die Duel-Anfrage wurde aufgehoben, weil " + leaverName + " den Server verlassen hat.");
        }
        UUID targetId = this.outgoingDuelInvites.get(id);
        if (targetId != null && this.cancelDuelInvite(id, targetId)) {
            Player target = this.getServer().getPlayer(targetId);
            if (target != null) target.sendMessage("§7Die Duel-Anfrage wurde aufgehoben, weil " + leaverName + " den Server verlassen hat.");
        }
        this.selectedDuelKits.remove(id);
        this.duelHosts.remove(id);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID target;
        Player player = event.getPlayer();
        MenuItems.removeTrainingItems(player);
        if (this.personalKitEditors.containsKey(player.getUniqueId())) {
            this.savePersonalKitEditor(player, player.getOpenInventory().getTopInventory());
        }
        UUID id = player.getUniqueId();
        this.cancelDuelInvitesFor(id, player.getName());
        boolean combatLogout = this.isInSmpCombat(player);
        if (combatLogout) {
            player.setHealth(0.0);
            this.getServer().broadcastMessage(String.valueOf(ChatColor.RED) + "\u2694 " + player.getName() + " hat sich im Combat ausgeloggt und ist gestorben.");
        }
        this.clearSmpCombat(player);
        this.saveLastSmpLocation(player);
        String partyMatch = this.activePartyMatches.get(id);
        if (partyMatch != null) {
            this.finishPartyMatch(partyMatch, null, player.getName() + " hat das Party-Match verlassen.");
        }
        if (this.activeDuelPlayers.contains(id)) {
            Player partner;
            // Wer schon verloren hat oder im Ergebnis ist, muss nicht mehr aufgeben - aufraeumen und speichern muss trotzdem laufen
            if (!this.duelResultPlayers.contains(id) && !this.pendingDuelLosers.contains(id)) {
                UUID partnerId = this.duelPartners.get(id);
                Player player2 = partner = partnerId == null ? null : this.getServer().getPlayer(partnerId);
                if (partner != null && partner.isOnline()) {
                    this.scheduleDuelForfeit(player, partner);
                } else {
                    this.finishDuel(player);
                }
            }
        }
        this.leaveBauMode(player, false);
        boolean duelLogout = this.activeDuelPlayers.contains(id);
        String profile = this.profiles.getOrDefault(id, "lobby");
        if (duelLogout) profile = this.duelBaseProfiles.getOrDefault(id, profile);
        if (profile.equals("smp")) {
            if (!duelLogout) this.saveInventory(id, player, this.smpItems, this.smpArmor);
            this.persistProfile(id, "smp", this.smpItems.get(id), this.smpArmor.get(id));
        } else if (profile.equals("bau")) {
            if (!duelLogout) this.saveInventory(id, player, this.bauItems, this.bauArmor);
            this.persistProfile(id, "bau", this.bauItems.get(id), this.bauArmor.get(id));
        } else {
            if (!duelLogout) this.saveInventory(id, player, this.lobbyItems, this.lobbyArmor);
            this.persistProfile(id, "lobby", this.lobbyItems.get(id), this.lobbyArmor.get(id));
        }
        // Nur das aktive Profil zuruecksetzen: SMP-/Bau-Inventare bleiben getrennt gespeichert.
        this.getConfig().set("players." + String.valueOf(id) + ".active", (Object)"lobby");
        this.saveConfig();
        this.duelQueues.values().forEach(queue -> queue.remove(id));
        this.queuedSince.remove(id);
        this.duelRankedFlags.remove(id);
        this.partyInvites.remove(id);
        this.tpahereRequests.remove(id);
        this.tpahereRequests.values().removeIf(id::equals);
        UUID requester = this.tpaRequests.remove(id);
        if (requester != null) {
            this.outgoingTpaRequests.remove(requester, id);
        }
        if ((target = this.outgoingTpaRequests.remove(id)) != null) {
            this.tpaRequests.remove(target, id);
        }
        this.cancelDelayedTeleport(player, false);
        this.clearTpaExpiry(id);
        if (this.playerParties.containsKey(id)) {
            this.leaveParty(player);
        }
        this.profiles.remove(id);
    }

    private void normalizeDuelKitItem(String string, ItemStack itemStack) {
    }

    private void enforceActiveKitRules(Player player, String kitName) {
        String kit = kitName.toLowerCase();
        ArrayList<ItemStack> all = new ArrayList<ItemStack>();
        all.addAll(Arrays.asList(player.getInventory().getContents()));
        all.addAll(Arrays.asList(player.getInventory().getArmorContents()));
        all.add(player.getInventory().getItemInOffHand());
        for (ItemStack item : all) {
            ItemMeta meta;
            if (item == null || item.getType() == Material.AIR) continue;
            if (kit.equals("axe pvp")) {
                for (Enchantment enchantment : new ArrayList<Enchantment>(item.getEnchantments().keySet())) {
                }
            } else if (item.getType() == Material.SHIELD) {
            }
            if (kit.equals("op netherite") && item.getType() == Material.NETHERITE_AXE) {
            }
            if (kit.equals("op netherite") && item.getType() == Material.NETHERITE_SWORD) {
            }
            if (item.getType() != Material.MACE || (meta = item.getItemMeta()) == null) continue;
            meta.setDisplayName(null);
            meta.removeAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE);
            item.setItemMeta(meta);
        }
    }

    private void beginDuelKit(Player player, String kitName) {
        if (!this.requireLobbyForDuels(player)) {
            return;
        }
        if (this.duelLeaveCheck(player, kitName)) {
            return;
        }
        UUID id = player.getUniqueId();
        this.selectedDuelKits.put(id, kitName);
        UUID partnerId = this.duelPartners.get(id);
        if (partnerId == null) {
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Kit vorgemerkt: " + kitName + ". Das Duel startet nach Annahme.");
            return;
        }
        Player partner = this.getServer().getPlayer(partnerId);
        if (partner == null || !partner.isOnline()) {
            this.clearPendingDuelStart(id, partnerId);
            player.sendMessage(String.valueOf(ChatColor.RED) + "Der andere Spieler ist nicht mehr online.");
            return;
        }
        String partnerKit = this.selectedDuelKits.get(partnerId);
        if (partnerKit == null) {
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Warte, bis " + partner.getName() + " ebenfalls ein Kit ausw\u00e4hlt.");
            partner.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + player.getName() + " hat das Kit " + kitName + " gew\u00e4hlt. W\u00e4hle dasselbe Kit.");
            return;
        }
        if (!partnerKit.equals(kitName)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Beide Spieler m\u00fcssen dasselbe Kit w\u00e4hlen.");
            partner.sendMessage(String.valueOf(ChatColor.RED) + "Beide Spieler m\u00fcssen dasselbe Kit w\u00e4hlen.");
            return;
        }
        this.selectedDuelKits.remove(id);
        this.selectedDuelKits.remove(partnerId);
        UUID host = this.duelHosts.getOrDefault(id, id);
        int chosen = this.duelBestOf.getOrDefault(host, 1);
        this.pendingBestOf.put(id, chosen);
        this.pendingBestOf.put(partnerId, chosen);
        this.startDuelMatch(player, partner, kitName);
    }

    /** Wartungsmodus: neue Duelle/Matches werden nicht mehr gestartet (laufende enden normal). */
    volatile boolean maintenance;

    private void clearPendingDuelStart(UUID first, UUID second) {
        if (first != null) {
            this.duelPartners.remove(first);
            this.selectedDuelKits.remove(first);
            this.pendingBestOf.remove(first);
            this.duelHosts.remove(first);
        }
        if (second != null) {
            this.duelPartners.remove(second);
            this.selectedDuelKits.remove(second);
            this.pendingBestOf.remove(second);
            this.duelHosts.remove(second);
        }
    }

    private void abortDuelStart(Player first, Player second, String message) {
        this.clearPendingDuelStart(first == null ? null : first.getUniqueId(), second == null ? null : second.getUniqueId());
        if (first != null && first.isOnline()) first.sendMessage(message);
        if (second != null && second.isOnline()) second.sendMessage(message);
    }

    private boolean blockedByMaintenance(Player... who) {
        if (!this.maintenance) {
            return false;
        }
        for (Player p : who) {
            if (p != null) {
                p.sendMessage("§e⚙ Wartungsmodus: Neue Duelle sind kurz pausiert, bald geht es weiter.");
            }
        }
        return true;
    }

    void startDuelMatch(Player player, Player partner, String kitName) {
        if (this.blockedByMaintenance(player, partner)) {
            this.clearPendingDuelStart(player.getUniqueId(), partner.getUniqueId());
            return;
        }
        String arena = this.findFreeArena();
        if (arena == null) {
            this.abortDuelStart(player, partner, "§cZurzeit ist keine freie, vollständig eingerichtete Duell-Arena verfügbar. Bitte einen Admin um /duelladmin diagnose.");
            return;
        }
        if (this.arenaSnapshots.containsKey(arena)) {
            this.startPreparedDuelMatch(player, partner, kitName, arena);
            return;
        }
        // Reservierung bleibt nur im Arbeitsspeicher, bis der Snapshot fertig ist.
        player.sendMessage("§7Die Arena wird schrittweise vorbereitet; das Duell startet gleich.");
        partner.sendMessage("§7Die Arena wird schrittweise vorbereitet; das Duell startet gleich.");
        this.snapshotArena(arena, success -> {
            if (!success) {
                this.abortDuelStart(player, partner, "§cDie Arena konnte nicht sicher vorbereitet werden. Bitte einen Admin um /duelladmin diagnose.");
                return;
            }
            if (!player.isOnline() || !partner.isOnline() || !partner.getUniqueId().equals(this.duelPartners.get(player.getUniqueId()))) {
                this.clearPendingDuelStart(player.getUniqueId(), partner.getUniqueId());
                if (player.isOnline()) player.sendMessage("§7Duell abgebrochen, weil ein Spieler nicht mehr verfügbar ist.");
                if (partner.isOnline()) partner.sendMessage("§7Duell abgebrochen, weil ein Spieler nicht mehr verfügbar ist.");
                return;
            }
            this.startPreparedDuelMatch(player, partner, kitName, arena);
        });
    }

    private void startPreparedDuelMatch(Player player, Player partner, String kitName, String arena) {
        if (this.blockedByMaintenance(player, partner)) {
            this.getConfig().set("duels.arenas." + arena + ".inUse", (Object)false);
            this.saveConfig();
            this.clearPendingDuelStart(player.getUniqueId(), partner.getUniqueId());
            return;
        }
        this.getConfig().set("duels.arenas." + arena + ".inUse", (Object)true);
        this.saveConfig();
        this.duelArenas.put(player.getUniqueId(), arena);
        this.duelArenas.put(partner.getUniqueId(), arena);
        this.duelModes.put(player.getUniqueId(), kitName.toLowerCase());
        this.duelModes.put(partner.getUniqueId(), kitName.toLowerCase());
        this.duelDamage.put(player.getUniqueId(), 0.0);
        this.duelDamage.put(partner.getUniqueId(), 0.0);
        int best = Math.max(this.pendingBestOf.getOrDefault(player.getUniqueId(), 1), this.pendingBestOf.getOrDefault(partner.getUniqueId(), 1));
        this.pendingBestOf.remove(player.getUniqueId());
        this.pendingBestOf.remove(partner.getUniqueId());
        this.duelHosts.remove(player.getUniqueId());
        this.duelHosts.remove(partner.getUniqueId());
        this.lastBestOf.put(player.getUniqueId(), best);
        this.lastBestOf.put(partner.getUniqueId(), best);
        if (best > 1) {
            Series series = new Series(player.getUniqueId(), partner.getUniqueId(), best, arena, kitName, false);
            this.duelSeries.put(player.getUniqueId(), series);
            this.duelSeries.put(partner.getUniqueId(), series);
            player.sendMessage("\u00a7d\u2694 \u00a77Serie: \u00a7fBest of " + best + " \u00a78(erster mit " + series.needed + " Siegen)");
            partner.sendMessage("\u00a7d\u2694 \u00a77Serie: \u00a7fBest of " + best + " \u00a78(erster mit " + series.needed + " Siegen)");
        }
        final long limitSeconds = 600L * Math.max(1, best);
        this.beginDuelKitNow(player, kitName);
        this.beginDuelKitNow(partner, kitName);
        Location first = this.readLocation("duels.arenas." + arena + ".spawn1");
        Location second = this.readLocation("duels.arenas." + arena + ".spawn2");
        player.teleport(first);
        partner.teleport(second);
        this.duelCountdownPlayers.add(player.getUniqueId());
        this.duelCountdownPlayers.add(partner.getUniqueId());
        long duelStartMillis = System.currentTimeMillis();
        BossBar duelBar = this.getServer().createBossBar(String.valueOf(ChatColor.RED) + "\u2694 DUELL " + String.valueOf(ChatColor.DARK_GRAY) + "\u2022 " + String.valueOf(ChatColor.WHITE) + this.displayKitName(kitName) + String.valueOf(ChatColor.DARK_GRAY) + " \u2022 " + String.valueOf(ChatColor.GRAY) + "10:00", BarColor.RED, BarStyle.SEGMENTED_10, new BarFlag[0]);
        duelBar.setProgress(1.0);
        duelBar.addPlayer(player);
        duelBar.addPlayer(partner);
        this.duelBossBars.put(player.getUniqueId(), duelBar);
        this.duelBossBars.put(partner.getUniqueId(), duelBar);
        int hudTask = this.getServer().getScheduler().runTaskTimer((Plugin)this, () -> {
            if (!this.activeDuelPlayers.contains(player.getUniqueId()) || !this.activeDuelPlayers.contains(partner.getUniqueId())) {
                return;
            }
            long remainingSeconds = Math.max(0L, limitSeconds - (System.currentTimeMillis() - duelStartMillis) / 1000L);
            long minutes = remainingSeconds / 60L;
            long seconds = remainingSeconds % 60L;
            duelBar.setTitle(String.valueOf(ChatColor.RED) + "\u2694 DUELL " + String.valueOf(ChatColor.DARK_GRAY) + "\u2022 " + String.valueOf(ChatColor.WHITE) + this.displayKitName(kitName) + String.valueOf(ChatColor.DARK_GRAY) + " \u2022 " + String.valueOf(ChatColor.GRAY) + String.format("%d:%02d", minutes, seconds));
            duelBar.setTitle(duelBar.getTitle() + this.seriesTag(player.getUniqueId()));
            duelBar.setProgress(Math.max(0.0, Math.min(1.0, (double)remainingSeconds / (double)limitSeconds)));
        }, 20L, 20L).getTaskId();
        this.duelHudTasks.put(player.getUniqueId(), hudTask);
        this.duelHudTasks.put(partner.getUniqueId(), hudTask);
        player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "\u2694 Arena " + arena + String.valueOf(ChatColor.GRAY) + " \u2022 Match wird vorbereitet.");
        partner.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "\u2694 Arena " + arena + String.valueOf(ChatColor.GRAY) + " \u2022 Match wird vorbereitet.");
        int timeoutTask = this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
            Player firstPlayer = this.getServer().getPlayer(player.getUniqueId());
            Player secondPlayer = this.getServer().getPlayer(partner.getUniqueId());
            if (firstPlayer == null || secondPlayer == null || !this.activeDuelPlayers.contains(firstPlayer.getUniqueId()) || !this.activeDuelPlayers.contains(secondPlayer.getUniqueId())) {
                return;
            }
            double firstDamage = this.duelDamage.getOrDefault(firstPlayer.getUniqueId(), 0.0);
            double secondDamage = this.duelDamage.getOrDefault(secondPlayer.getUniqueId(), 0.0);
            if (Math.abs(firstDamage - secondDamage) < 0.01) {
                this.finishDuelDraw(firstPlayer, secondPlayer, "Zeitlimit erreicht \u2013 Gleichstand.");
            } else if (firstDamage > secondDamage) {
                this.finishDuelResult(firstPlayer, secondPlayer, "Zeitlimit erreicht \u2013 mehr Schaden verursacht.");
            } else {
                this.finishDuelResult(secondPlayer, firstPlayer, "Zeitlimit erreicht \u2013 mehr Schaden verursacht.");
            }
        }, limitSeconds * 20L).getTaskId();
        this.duelTimeoutTasks.put(player.getUniqueId(), timeoutTask);
        this.duelTimeoutTasks.put(partner.getUniqueId(), timeoutTask);
        for (int i = 3; i >= 1; --i) {
            int count = i;
            this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
                if (!this.activeDuelPlayers.contains(player.getUniqueId()) || !this.activeDuelPlayers.contains(partner.getUniqueId())) {
                    return;
                }
                String message = String.valueOf(ChatColor.LIGHT_PURPLE) + count;
                player.sendTitle(message, String.valueOf(ChatColor.LIGHT_PURPLE) + "Kit laden \u2022 Bereit machen...", 0, 15, 5);
                partner.sendTitle(message, String.valueOf(ChatColor.LIGHT_PURPLE) + "Kit laden \u2022 Bereit machen...", 0, 15, 5);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                partner.playSound(partner.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
            }, (3L - (long)i) * 20L);
        }
        this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
            if (!this.activeDuelPlayers.contains(player.getUniqueId()) || !this.activeDuelPlayers.contains(partner.getUniqueId())) {
                return;
            }
            this.duelCountdownPlayers.remove(player.getUniqueId());
            this.duelCountdownPlayers.remove(partner.getUniqueId());
            player.sendTitle(String.valueOf(ChatColor.GREEN) + "K\u00c4MPFT!", "", 0, 25, 10);
            partner.sendTitle(String.valueOf(ChatColor.GREEN) + "K\u00c4MPFT!", "", 0, 25, 10);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            partner.playSound(partner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        }, 60L);
    }


    /** Legt das Duel-Kit an (leert vorher das Inventar). Wird zu Beginn und vor jeder weiteren Runde einer Serie genutzt. */
    private void applyDuelKit(Player player, String kitName) {
        UUID id = player.getUniqueId();
        player.setLevel(0);
        player.setExp(0.0f);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        @SuppressWarnings("unchecked") List<ItemStack> stored = (List<ItemStack>)this.getConfig().getList("kits.data." + kitName.toLowerCase() + ".items");
        if (stored != null && !stored.isEmpty()) {
            KitSet kitSet = this.buildKitSet(kitName, stored);
            this.arrangeKit(id, kitName, kitSet);
            ItemStack[] kitContents = kitSet.storage;
            ItemStack[] kitArmor = kitSet.armor;
            ItemStack offhand = kitSet.offhand;
            player.getInventory().setStorageContents(kitContents);
            player.getInventory().setArmorContents(kitArmor);
            player.getInventory().setItemInOffHand(offhand);
            this.enforceActiveKitRules(player, kitName);
            Cosmetics.decorate(player);
        } else {
            player.getInventory().addItem(new ItemStack[]{this.item(Material.IRON_SWORD, "\u00a7f" + kitName, "\u00a77Tempor\u00e4res Duel-Kit")});
        }
        player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "\u2694 Kit geladen: " + kitName + String.valueOf(ChatColor.GRAY) + " \u2022 Items werden nach dem Duel entfernt.");
    }

    /* ---------- Serien (Best of 1/3/5/7) ---------- */

    private static int nextBestOf(int current) {
        return current >= 7 ? 1 : current < 3 ? 3 : current + 2;
    }

    private ItemStack bestOfItem(int best) {
        int needed = best / 2 + 1;
        return this.item(Material.TARGET, "\u00a7e\u00a7lBest of " + best,
                "\u00a77Wer zuerst \u00a7f" + needed + " Runde" + (needed == 1 ? "" : "n") + "\u00a77 gewinnt, siegt.\n"
                + "\u00a77Der Herausforderer / Party-Leader bestimmt.\n\u00a7aKlick: \u00e4ndern (1 / 3 / 5 / 7)");
    }

    /** Klick auf das Best-of-Item: Auswahl weiterschalten. */
    private void cycleBestOf(Player player, Inventory inv) {
        UUID me = player.getUniqueId();
        UUID host = this.duelHosts.getOrDefault(me, me);
        if (!host.equals(me)) {
            Player hostPlayer = this.getServer().getPlayer(host);
            player.sendMessage("\u00a7cNur " + (hostPlayer == null ? "der Herausforderer" : hostPlayer.getName()) + " kann das einstellen.");
            Chat.no(player);
            return;
        }
        int next = nextBestOf(this.duelBestOf.getOrDefault(me, 1));
        this.duelBestOf.put(me, next);
        if (inv != null) {
            inv.setItem(22, this.bestOfItem(next));
        }
        player.sendMessage("\u00a7d\u2694 \u00a77Serie: \u00a7fBest of " + next + " \u00a78(erster mit " + (next / 2 + 1) + ")");
        Chat.click(player);
        UUID partnerId = this.duelPartners.get(me);
        Player partner = partnerId == null ? null : this.getServer().getPlayer(partnerId);
        if (partner != null) {
            partner.sendMessage("\u00a7d\u2694 \u00a7f" + player.getName() + " \u00a77hat \u00a7fBest of " + next + " \u00a77gew\u00e4hlt.");
        }
    }

    /** Fuer die Revanche: dieselbe Serienlaenge wie zuletzt. */
    void prepareRematchSeries(Player first, Player second) {
        int best = Math.max(this.lastBestOf.getOrDefault(first.getUniqueId(), 1), this.lastBestOf.getOrDefault(second.getUniqueId(), 1));
        this.pendingBestOf.put(first.getUniqueId(), best);
        this.pendingBestOf.put(second.getUniqueId(), best);
    }

    private String seriesTag(UUID id) {
        Series series = this.duelSeries.get(id);
        if (series == null) {
            return "";
        }
        Player a = this.getServer().getPlayer(series.a);
        Player b = this.getServer().getPlayer(series.b);
        return " \u00a78\u2022 \u00a7fBo" + series.best + " \u00a7d" + (a == null ? "?" : a.getName()) + " " + series.winsA + ":" + series.winsB + " " + (b == null ? "?" : b.getName());
    }

    /**
     * Ein Spieler wurde besiegt. @return true, wenn die Serie weiterlaeuft (naechste Runde wird vorbereitet),
     * false, wenn es keine Serie gibt oder sie damit entschieden ist (normaler Ablauf).
     */
    private boolean seriesRound(Player loser, Player winner) {
        Series series = this.duelSeries.get(winner.getUniqueId());
        if (series == null || !this.duelSeries.containsKey(loser.getUniqueId())) {
            return false;
        }
        boolean winnerIsA = winner.getUniqueId().equals(series.a);
        if (winnerIsA) {
            series.winsA++;
        } else {
            series.winsB++;
        }
        int winnerWins = winnerIsA ? series.winsA : series.winsB;
        Player pa = this.getServer().getPlayer(series.a);
        Player pb = this.getServer().getPlayer(series.b);
        String score = "\u00a7f" + series.winsA + "\u00a78:\u00a7f" + series.winsB;
        if (winnerWins >= series.needed) {
            for (Player p : new Player[]{pa, pb}) {
                if (p != null) {
                    p.sendMessage("\u00a7d\u00a7l\u2694 Serie entschieden! \u00a7f" + winner.getName() + " \u00a77gewinnt \u00a7f" + series.winsA + ":" + series.winsB + " \u00a78(Best of " + series.best + ")");
                }
            }
            return false;
        }
        winner.sendTitle("\u00a7aRunde gewonnen", score, 0, 35, 10);
        loser.sendTitle("\u00a7cRunde verloren", score, 0, 35, 10);
        for (Player p : new Player[]{pa, pb}) {
            if (p != null) {
                p.sendMessage("\u00a7d\u2694 \u00a77Stand: \u00a7f" + (pa == null ? "?" : pa.getName()) + " " + series.winsA + "\u00a78:\u00a7f" + series.winsB + " " + (pb == null ? "?" : pb.getName()) + " \u00a78(Best of " + series.best + ", erster mit " + series.needed + ")");
                this.duelCountdownPlayers.add(p.getUniqueId()); // niemand kann mehr Schaden nehmen
                DuelFixes.forget(p.getUniqueId()); // keine verspaetete Ergebnis-Phase aus der Todes-Behandlung
            }
        }
        this.getServer().getScheduler().runTaskLater((Plugin)this, () -> this.nextRound(series), 50L);
        return true;
    }

    private void nextRound(Series series) {
        Player pa = this.getServer().getPlayer(series.a);
        Player pb = this.getServer().getPlayer(series.b);
        if (pa == null || pb == null || !this.activeDuelPlayers.contains(series.a) || !this.activeDuelPlayers.contains(series.b)
                || !this.duelSeries.containsKey(series.a)) {
            this.duelCountdownPlayers.remove(series.a);
            this.duelCountdownPlayers.remove(series.b);
            return;
        }
        this.resetArena(series.arena, () -> this.nextRoundStart(series));
    }

    private void nextRoundStart(Series series) {
        Player pa = this.getServer().getPlayer(series.a);
        Player pb = this.getServer().getPlayer(series.b);
        if (pa == null || pb == null || !this.activeDuelPlayers.contains(series.a) || !this.activeDuelPlayers.contains(series.b)
                || !this.duelSeries.containsKey(series.a)) {
            this.duelCountdownPlayers.remove(series.a);
            this.duelCountdownPlayers.remove(series.b);
            return;
        }
        try {
            Location first = this.readLocation("duels.arenas." + series.arena + ".pos1");
            Location second = this.readLocation("duels.arenas." + series.arena + ".pos2");
            if (first.getWorld() != null && first.getWorld().equals(second.getWorld())) {
                for (org.bukkit.entity.Entity entity : first.getWorld().getNearbyEntities(org.bukkit.util.BoundingBox.of(first, second).expand(1.0))) {
                    if (entity instanceof org.bukkit.entity.Item || entity instanceof org.bukkit.entity.Projectile
                            || entity instanceof org.bukkit.entity.EnderCrystal || entity instanceof org.bukkit.entity.ExperienceOrb
                            || entity instanceof org.bukkit.entity.AreaEffectCloud) {
                        entity.remove();
                    }
                }
            }
        }
        catch (RuntimeException ex) {
            Store.problem("Duel-Serie:Aufraeumen", ex);
        }
        Location spawn1 = this.readLocation("duels.arenas." + series.arena + ".spawn1");
        Location spawn2 = this.readLocation("duels.arenas." + series.arena + ".spawn2");
        if (spawn2 == null || spawn2.getWorld() == null) {
            spawn2 = spawn1;
        }
        Player[] both = {pa, pb};
        for (int i = 0; i < 2; i++) {
            Player p = both[i];
            p.setGameMode(GameMode.SURVIVAL);
            for (org.bukkit.potion.PotionEffect effect : new ArrayList<org.bukkit.potion.PotionEffect>(p.getActivePotionEffects())) {
                p.removePotionEffect(effect.getType());
            }
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.setSaturation(20.0f);
            p.setFireTicks(0);
            p.setFallDistance(0.0f);
            p.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            this.applyDuelKit(p, series.kit);
            p.teleport((i == 0 ? spawn1 : spawn2).clone());
            this.duelCountdownPlayers.add(p.getUniqueId());
        }
        for (int i = 3; i >= 1; --i) {
            int count = i;
            this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
                if (!this.duelSeries.containsKey(series.a)) {
                    return;
                }
                for (Player p : both) {
                    p.sendTitle("\u00a7d" + count, "\u00a77Runde " + (series.winsA + series.winsB + 1), 0, 15, 5);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                }
            }, (3L - (long)i) * 20L);
        }
        this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
            this.duelCountdownPlayers.remove(series.a);
            this.duelCountdownPlayers.remove(series.b);
            if (!this.duelSeries.containsKey(series.a)) {
                return;
            }
            for (Player p : both) {
                p.sendTitle("\u00a7aK\u00c4MPFT!", "", 0, 25, 10);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            }
        }, 60L);
    }

    String findFreeArena() {
        ConfigurationSection section = this.getConfig().getConfigurationSection("duels.arenas");
        if (section == null) {
            return null;
        }
        for (String name : section.getKeys(false)) {
            String path = "duels.arenas." + name;
            if (this.resettingArenas.contains(name) || this.snapshottingArenas.contains(name) || !this.arenaValidationMessage(name).startsWith("OK:")) continue;
            Location first = this.readLocation(path + ".pos1");
            Location second = this.readLocation(path + ".pos2");
            if (first.getWorld() == null || !first.getWorld().equals((Object)second.getWorld())) continue;
            return name;
        }
        return null;
    }

    private void snapshotArena(String arena, java.util.function.Consumer<Boolean> completed) {
        if (this.arenaSnapshots.containsKey(arena)) {
            completed.accept(true);
            return;
        }
        if (!this.snapshottingArenas.add(arena)) {
            completed.accept(false);
            return;
        }
        String path = "duels.arenas." + arena;
        if (!this.getConfig().contains(path + ".pos1.world") || !this.getConfig().contains(path + ".pos2.world")) {
            this.snapshottingArenas.remove(arena);
            completed.accept(false);
            return;
        }
        Location first = this.readLocation(path + ".pos1");
        Location second = this.readLocation(path + ".pos2");
        if (first.getWorld() == null || second.getWorld() == null || !first.getWorld().equals((Object)second.getWorld())) {
            this.snapshottingArenas.remove(arena);
            completed.accept(false);
            return;
        }
        final World world = first.getWorld();
        final int minX = Math.min(first.getBlockX(), second.getBlockX());
        final int maxX = Math.max(first.getBlockX(), second.getBlockX());
        final int minY = Math.min(first.getBlockY(), second.getBlockY());
        final int maxY = Math.max(first.getBlockY(), second.getBlockY());
        final int minZ = Math.min(first.getBlockZ(), second.getBlockZ());
        final int maxZ = Math.max(first.getBlockZ(), second.getBlockZ());
        final long width = (long)maxX - minX + 1L;
        final long height = (long)maxY - minY + 1L;
        final long depth = (long)maxZ - minZ + 1L;
        final long plane;
        final long volume;
        try {
            plane = Math.multiplyExact(height, depth);
            volume = Math.multiplyExact(width, plane);
        }
        catch (ArithmeticException overflow) {
            this.getLogger().warning("Duel-Arena " + arena + " hat ein übergroßes Snapshot-Volumen und wurde abgelehnt.");
            this.snapshottingArenas.remove(arena);
            completed.accept(false);
            return;
        }
        final int perTick = Math.max(100, Math.min(20000, this.getConfig().getInt("duels.snapshot-blocks-per-tick", 1500)));
        final Map<String, BlockData> snapshot = new HashMap<String, BlockData>();
        final long[] index = {0L};
        final int[] taskId = {-1};
        Runnable capture = () -> {
            try {
                long end = Math.min(volume, index[0] + perTick);
                for (; index[0] < end; index[0]++) {
                    long current = index[0];
                    int x = (int)(minX + current / plane);
                    int y = (int)(minY + (current / depth) % height);
                    int z = (int)(minZ + current % depth);
                    snapshot.put(x + ":" + y + ":" + z, world.getBlockAt(x, y, z).getBlockData().clone());
                }
            } catch (RuntimeException ex) {
                this.getServer().getScheduler().cancelTask(taskId[0]);
                this.snapshottingArenas.remove(arena);
                Store.problem("Duel-Arena Snapshot " + arena, ex);
                completed.accept(false);
                return;
            }
            if (index[0] >= volume) {
                this.getServer().getScheduler().cancelTask(taskId[0]);
                this.arenaSnapshots.put(arena, snapshot);
                this.snapshottingArenas.remove(arena);
                this.getLogger().info("Duel-Arena " + arena + " wurde schrittweise für den automatischen Reset aufgenommen (" + snapshot.size() + " Blöcke).");
                completed.accept(true);
            }
        };
        taskId[0] = this.getServer().getScheduler().runTaskTimer((Plugin)this, capture, 1L, 1L).getTaskId();
    }

    /** Arenen, die gerade schrittweise zurueckgesetzt werden (nicht neu belegen). */
    private final Set<String> resettingArenas = ConcurrentHashMap.newKeySet();

    private void resetArena(String arena) {
        this.resetArena(arena, null);
    }

    /**
     * Setzt die Arena auf den Snapshot zurueck - verteilt auf mehrere Ticks und nur dort, wo sich wirklich
     * etwas geaendert hat (kein Lagspike mehr bei grossen Arenen). Danach wird {@code done} ausgefuehrt.
     */
    private void lockArenaAfterResetFailure(String arena, RuntimeException cause) {
        this.resettingArenas.add(arena);
        this.getConfig().set("duels.arenas." + arena + ".inUse", (Object)true);
        this.saveConfig();
        Store.problem("Arena-Reset gesperrt: " + arena, cause);
        this.getLogger().severe("Duell-Arena " + arena + " bleibt wegen eines Resetfehlers gesperrt. Bitte Arena pruefen, bevor sie manuell wieder freigegeben wird.");
    }

    private void resetArena(String arena, Runnable done) {
        Map<String, BlockData> snapshot = this.arenaSnapshots.get(arena);
        Location first = snapshot == null ? null : this.readLocation("duels.arenas." + arena + ".pos1");
        if (snapshot == null || first == null || first.getWorld() == null) {
            this.lockArenaAfterResetFailure(arena, new IllegalStateException("Arena-Snapshot oder Resetwelt fehlt"));
            return;
        }
        if (!this.resettingArenas.add(arena)) {
            if (done != null) {
                this.getServer().getScheduler().runTaskLater((Plugin)this, done, 40L);
            }
            return;
        }
        final List<Map.Entry<String, BlockData>> entries = new ArrayList<Map.Entry<String, BlockData>>(snapshot.entrySet());
        final World world = first.getWorld();
        final int perTick = Math.max(500, this.getConfig().getInt("duels.reset-blocks-per-tick", 4000));
        final int[] index = {0};
        final int[] taskId = {-1};
        taskId[0] = this.getServer().getScheduler().runTaskTimer((Plugin)this, () -> {
            try {
                int end = Math.min(entries.size(), index[0] + perTick);
                for (; index[0] < end; index[0]++) {
                    Map.Entry<String, BlockData> entry = entries.get(index[0]);
                    String[] c = entry.getKey().split(":");
                    org.bukkit.block.Block block = world.getBlockAt(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]));
                    if (!block.getBlockData().equals(entry.getValue())) {
                        block.setBlockData(entry.getValue().clone(), false);
                    }
                }
            }
            catch (RuntimeException ex) {
                this.getServer().getScheduler().cancelTask(taskId[0]);
                this.lockArenaAfterResetFailure(arena, ex);
                return;
            }
            if (index[0] >= entries.size()) {
                this.getServer().getScheduler().cancelTask(taskId[0]);
                this.resettingArenas.remove(arena);
                this.getLogger().info("Duel-Arena " + arena + " wurde nach dem Duel zur\u00fcckgesetzt.");
                if (done != null) {
                    done.run();
                }
            }
        }, 1L, 1L).getTaskId();
    }

    private boolean isWindChargeKit(String kitName) {
        return kitName != null && (kitName.equalsIgnoreCase("mace pvp") || kitName.equalsIgnoreCase("op netherite"));
    }

    private int countItem(Player player, Material material) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType() != material) continue;
            count += stack.getAmount();
        }
        return count;
    }

    private void ensureBreezeRodReserve(Player player, String kitName) {
        if (!this.isWindChargeKit(kitName) || this.countItem(player, Material.WIND_CHARGE) > 0 || this.countItem(player, Material.BREEZE_ROD) > 0) {
            return;
        }
        if (player.getInventory().firstEmpty() >= 0) {
            player.getInventory().addItem(new ItemStack[]{new ItemStack(Material.BREEZE_ROD, 64)});
            player.sendActionBar((Component)Component.text((String)(String.valueOf(ChatColor.LIGHT_PURPLE) + "Breeze-Rod-Reserve bereit")));
        }
    }

    @EventHandler
    public void onWindChargeReserve(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        String kit = this.duelModes.get(player.getUniqueId());
        String partyMatch = this.activePartyMatches.get(player.getUniqueId());
        if (partyMatch != null) {
            kit = this.partyMatchKits.get(partyMatch);
        }
        if (this.activeDuelPlayers.contains(player.getUniqueId())) {
            this.ensureBreezeRodReserve(player, kit);
        }
    }

    private void beginDuelKitNow(Player player, String kitName) {
        UUID id = player.getUniqueId();
        if (this.activeDuelPlayers.contains(id)) {
            return;
        }
        this.leaveBauMode(player, false);
        String base = this.profiles.getOrDefault(id, "lobby");
        if (base.equals("smp")) {
            this.saveLastSmpLocation(player);
            this.saveInventory(id, player, this.smpItems, this.smpArmor);
        } else {
            this.saveInventory(id, player, this.lobbyItems, this.lobbyArmor);
        }
        this.duelBaseProfiles.put(id, base);
        this.activeDuelPlayers.add(id);
        this.applyDuelKit(player, kitName);
    }

    private void scheduleDuelForfeit(Player leaver, Player winner) {
        Location result;
        UUID leaverId;
        if (winner == null || !winner.isOnline()) {
            return;
        }
        UUID winnerId = winner.getUniqueId();
        UUID uUID = leaverId = leaver == null ? null : leaver.getUniqueId();
        if (this.duelLeaveTasks.containsKey(winnerId) || leaverId != null && this.duelLeaveTasks.containsKey(leaverId)) {
            return;
        }
        String reason = (leaver == null ? "Der Gegner" : leaver.getName()) + " hat das Duel verlassen.";
        winner.sendTitle(String.valueOf(ChatColor.GREEN) + "GEWONNEN!", String.valueOf(ChatColor.LIGHT_PURPLE) + reason, 0, 50, 10);
        winner.sendMessage(String.valueOf(ChatColor.GREEN) + "SIEGER! " + reason + " " + String.valueOf(ChatColor.GRAY) + "R\u00fcckkehr zum Spawn in 3 Sekunden.");
        if (leaver != null && leaver.isOnline()) {
            leaver.sendTitle(String.valueOf(ChatColor.RED) + "VERLOREN!", String.valueOf(ChatColor.GRAY) + reason, 0, 50, 10);
            leaver.sendMessage(String.valueOf(ChatColor.RED) + "VERLIERER! " + reason + " " + String.valueOf(ChatColor.GRAY) + "R\u00fcckkehr zum Spawn in 3 Sekunden.");
        }
        if ((result = this.lobbyCenter()) != null && result.getWorld() != null) {
            this.pendingDuelRespawns.put(winnerId, result);
            if (leaverId != null) {
                this.pendingDuelRespawns.put(leaverId, result);
            }
        }
        int task = this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
            this.duelLeaveTasks.remove(winnerId);
            if (leaverId != null) {
                this.duelLeaveTasks.remove(leaverId);
            }
            if (leaver != null && this.activeDuelPlayers.contains(winnerId) && this.activeDuelPlayers.contains(leaverId)) {
                DuelStats.record(winner, leaver);
            }
            if (this.activeDuelPlayers.contains(winnerId)) {
                this.finishDuel(winner);
                this.teleportDuelResult(winner);
            }
            if (leaver != null && this.activeDuelPlayers.contains(leaverId)) {
                this.finishDuel(leaver);
                this.teleportDuelResult(leaver);
                if (!leaver.isOnline()) {
                    this.profiles.remove(leaverId);
                }
            }
        }, 60L).getTaskId();
        this.duelLeaveTasks.put(winnerId, task);
        if (leaverId != null) {
            this.duelLeaveTasks.put(leaverId, task);
        }
    }

    void finishDuel(Player player) {
        String arena;
        UUID currentPartner;
        Integer hudTask;
        Integer timeoutTask;
        UUID id = player.getUniqueId();
        DuelFixes.forget(id);
        DuelSummary.forget(id);
        this.duelSeries.remove(id);
        this.duelRankedFlags.remove(id);
        this.queuedSince.remove(id);
        Integer leaveTask = this.duelLeaveTasks.remove(id);
        if (leaveTask != null) {
            this.getServer().getScheduler().cancelTask(leaveTask.intValue());
        }
        if (!this.activeDuelPlayers.remove(id)) {
            return;
        }
        this.duelCountdownPlayers.remove(id);
        this.pendingDuelLosers.remove(id);
        this.duelResultPlayers.remove(id);
        this.duelDeathWinners.remove(id);
        this.duelDeathLocations.remove(id);
        Integer deathFinishTask = this.duelDeathFinishTasks.remove(id);
        if (deathFinishTask != null) {
            this.getServer().getScheduler().cancelTask(deathFinishTask.intValue());
        }
        if ((timeoutTask = this.duelTimeoutTasks.remove(id)) != null) {
            this.getServer().getScheduler().cancelTask(timeoutTask.intValue());
        }
        this.duelDamage.remove(id);
        BossBar duelBar = this.duelBossBars.remove(id);
        if (duelBar != null) {
            duelBar.removeAll();
        }
        if ((hudTask = this.duelHudTasks.remove(id)) != null) {
            this.getServer().getScheduler().cancelTask(hudTask.intValue());
        }
        if ((currentPartner = this.duelPartners.get(id)) != null) {
            this.duelBossBars.remove(currentPartner);
            this.duelHudTasks.remove(currentPartner);
        }
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        String baseProfile = this.duelBaseProfiles.remove(id);
        boolean returnToSmp = "smp".equals(baseProfile);
        this.restoreInventory(id, player, returnToSmp ? this.smpItems : this.lobbyItems, returnToSmp ? this.smpArmor : this.lobbyArmor);
        this.profiles.put(id, returnToSmp ? "smp" : "lobby");
        UUID partner = this.duelPartners.remove(id);
        if (partner != null) {
            this.duelPartners.remove(partner, id);
        }
        if (partner != null) {
            this.duelModes.remove(partner);
        }
        if ((arena = this.duelArenas.remove(id)) != null) {
            this.duelArenas.remove(partner);
            this.getConfig().set("duels.arenas." + arena + ".inUse", (Object)false);
            this.saveConfig();
            this.resetArena(arena);
        }
        this.selectedDuelKits.remove(id);
        this.duelModes.remove(id);
        player.setGameMode(GameMode.SURVIVAL);
        this.resetPlayerState(player);
        if (!player.isDead()) {
            if (returnToSmp) {
                if (!this.teleportToLastSmpLocation(player)) {
                    this.teleportToSpawn(player, "smp");
                }
            } else {
                this.teleportToLobby(player);
            }
            this.lastLobbyReturn.put(id, System.currentTimeMillis());
        }
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "\u2726 Duel beendet " + String.valueOf(ChatColor.DARK_GRAY) + "\u2022 " + String.valueOf(ChatColor.GRAY) + "Dein " + (returnToSmp ? "SMP-" : "Lobby-") + "Inventar wurde wiederhergestellt.");
    }

    private void finishDuelResult(Player winner, Player loser, String reason) {
        if (winner == null || loser == null) {
            return;
        }
        this.pendingDuelRespawns.put(winner.getUniqueId(), this.duelResultLocation("winner"));
        this.pendingDuelRespawns.put(loser.getUniqueId(), this.duelResultLocation("loser"));
        winner.sendMessage(String.valueOf(ChatColor.GREEN) + "SIEGER! " + reason);
        loser.sendMessage(String.valueOf(ChatColor.RED) + "VERLIERER! " + reason);
        winner.sendTitle(String.valueOf(ChatColor.GREEN) + "GEWONNEN!", String.valueOf(ChatColor.LIGHT_PURPLE) + reason, 0, 40, 10);
        winner.playSound(winner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        loser.sendTitle(String.valueOf(ChatColor.RED) + "VERLOREN!", String.valueOf(ChatColor.GRAY) + reason, 0, 40, 10);
        loser.playSound(loser.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.7f);
        DuelSummary.show(winner, loser);
        DuelStats.record(winner, loser);
        this.finishDuel(winner);
        this.finishDuel(loser);
        this.teleportDuelResult(winner);
        this.teleportDuelResult(loser);
    }

    private void finishDuelDraw(Player first, Player second, String reason) {
        EventGames.duelDraw(first, second);
        Location drawLocation = this.duelResultLocation("draw");
        this.pendingDuelRespawns.put(first.getUniqueId(), drawLocation);
        this.pendingDuelRespawns.put(second.getUniqueId(), drawLocation);
        first.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + reason);
        second.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + reason);
        this.finishDuel(first);
        this.finishDuel(second);
        this.teleportDuelResult(first);
        this.teleportDuelResult(second);
    }

    private Location duelResultLocation(String result) {
        return this.lobbyCenter();
    }

    private void teleportDuelResult(Player player) {
        if (player.isDead()) {
            return;
        }
        Location location = this.pendingDuelRespawns.remove(player.getUniqueId());
        if (this.recentlyReturned(player.getUniqueId())) {
            return; // finishDuel hat den Spieler gerade schon zurueckgebracht
        }
        if (location != null && location.getWorld() != null) {
            player.teleport(location);
        }
    }

    /**
     * Tod im SMP: zurueck zum eigenen Bett (nur wenn es im SMP-Gebiet liegt) oder zum SMP-Spawn.
     * Sonst (Lobby, Events, ...) immer zum Lobby-Spawn. Duel-/Party-/Parkour-Logik darf danach noch ueberschreiben.
     */
    @EventHandler(priority=EventPriority.LOWEST)
    public void onSmpRespawn(PlayerRespawnEvent event) {
        try {
            Player player = event.getPlayer();
            UUID id = player.getUniqueId();
            if (this.activeDuelPlayers.contains(id) || this.activePartyMatches.containsKey(id) || this.activeParkourMaps.containsKey(id)) {
                return;
            }
            if ("smp".equals(this.profiles.getOrDefault(id, "lobby"))) {
                Location bed = event.isBedSpawn() || event.isAnchorSpawn() ? event.getRespawnLocation() : null;
                if (bed != null && bed.getWorld() != null && this.isInsideSmpBorder(bed)) {
                    return;
                }
                Location spawn = this.readConfiguredLocation("locations.spawns.smp.default");
                if (spawn != null && spawn.getWorld() != null) {
                    event.setRespawnLocation(spawn);
                }
                return;
            }
            Location lobby = this.readConfiguredLocation("locations.lobbies.haupt");
            if (lobby == null || lobby.getWorld() == null) {
                lobby = this.readConfiguredLocation("locations.spawns.haupt.default");
            }
            if (lobby != null && lobby.getWorld() != null) {
                event.setRespawnLocation(lobby);
            }
        }
        catch (Throwable ignored) {
            // Standard-Respawn
            Store.problem("DuelNetworkPlugin:4616", ignored);
        }
    }

    @EventHandler
    public void onDuelRespawn(PlayerRespawnEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (this.pendingDuelLosers.contains(id)) {
            Location deathLocation = this.duelDeathLocations.get(id);
            if (deathLocation != null && deathLocation.getWorld() != null) {
                event.setRespawnLocation(deathLocation);
            }
            this.getServer().getScheduler().runTask((Plugin)this, () -> {
                Player loser = event.getPlayer();
                if (!this.pendingDuelLosers.contains(id)) {
                    return;
                }
                loser.setGameMode(GameMode.SURVIVAL);
                Location target = this.duelDeathLocations.get(id);
                if (target != null && target.getWorld() != null) {
                    loser.teleport(target);
                }
            });
            return;
        }
        Location location = this.pendingDuelRespawns.remove(event.getPlayer().getUniqueId());
        if (location != null && location.getWorld() != null) {
            event.setRespawnLocation(location);
        }
    }

    /** Zeigt Admins, was noch fehlt (Spawns, Arenen). */
    void adminSetupHints(Player admin) {
        List<String> missing = new ArrayList<String>();
        if (!this.getConfig().contains("locations.lobbies.haupt.world")) {
            missing.add("Lobby (/duelladmin setlobby)");
        }
        for (String mode : List.of("smp", "creative")) {
            if (!this.spawnReady(mode)) {
                missing.add((mode.equals("smp") ? "SMP" : "Bau-SMP") + "-Spawn (/duelladmin setspawn " + mode + ")");
            }
        }
        if (!this.duelReady()) {
            missing.add("Duell-Arena (/duelladmin arena)");
        }
        Location bauSpawn = this.readConfiguredLocation("locations.spawns.creative.default");
        Location smpSpawn = this.readConfiguredLocation("locations.spawns.smp.default");
        if (bauSpawn != null && smpSpawn != null && bauSpawn.getWorld() != null && bauSpawn.getWorld().equals((Object)smpSpawn.getWorld())
                && Math.max(Math.abs(bauSpawn.getX() - smpSpawn.getX()), Math.abs(bauSpawn.getZ() - smpSpawn.getZ())) < 200.0) {
            missing.add("Bau-Spawn liegt weniger als 200 Bloecke vom SMP-Spawn - bitte weiter weg setzen (/duelladmin setspawn creative)");
        }
        if (!missing.isEmpty()) {
            admin.sendMessage(String.valueOf(ChatColor.YELLOW) + "Noch einzurichten: " + String.valueOf(ChatColor.GRAY) + String.join(", ", missing));
        }
    }

    boolean teleportToSpawn(Player player, String mode) {
        String path = "locations.spawns." + mode.toLowerCase() + ".default";
        if (!this.getConfig().contains(path + ".world")) {
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "F\u00fcr " + mode + " wurde noch kein Spawn eingerichtet.");
            return false;
        }
        String worldName = this.getConfig().getString(path + ".world");
        if (worldName == null || this.getServer().getWorld(worldName) == null) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Die Welt f\u00fcr den Spawn von " + mode + " wurde nicht gefunden.");
            return false;
        }
        Location location = new Location(this.getServer().getWorld(worldName), this.getConfig().getDouble(path + ".x"), this.getConfig().getDouble(path + ".y"), this.getConfig().getDouble(path + ".z"), (float)this.getConfig().getDouble(path + ".yaw"), (float)this.getConfig().getDouble(path + ".pitch"));
        player.teleport(location);
        Chat.sound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.4f, 1.2f);
        return true;
    }

    boolean teleportToLobby(Player player) {
        this.resetPlayerState(player);
        String lobbyPath = "locations.lobbies.haupt";
        if (this.getConfig().contains(lobbyPath + ".world")) {
            player.teleport(this.readLocation(lobbyPath));
            return true;
        }
        return this.teleportToSpawn(player, "haupt");
    }

    private Location lobbyCenter() {
        return DuelFixes.lobbyCenter(this);
    }

    boolean isInLobby(Location location) {
        Location center = this.lobbyCenter();
        double radius = this.getConfig().getDouble("protection.lobby-radius", 200.0);
        return this.isWithinSquare(location, center, radius);
    }

    private boolean isInSmpArea(Location location) {
        return this.isInsideSmpBorder(location);
    }

    /** true, wenn die Position in der Arena des Spielers liegt (mit kleinem Rand). */
    boolean insideDuelArena(Player player, Location location) {
        String arena = this.duelArenas.get(player.getUniqueId());
        if (arena == null || location == null || location.getWorld() == null) {
            return true;
        }
        String path = "duels.arenas." + arena;
        if (!this.getConfig().contains(path + ".pos1.world") || !this.getConfig().contains(path + ".pos2.world")) {
            return true;
        }
        Location a = this.readLocation(path + ".pos1");
        Location b = this.readLocation(path + ".pos2");
        if (a.getWorld() == null || !a.getWorld().equals((Object)location.getWorld())) {
            return true;
        }
        double margin = 2.0;
        return location.getX() >= Math.min(a.getX(), b.getX()) - margin && location.getX() <= Math.max(a.getX(), b.getX()) + 1.0 + margin
                && location.getZ() >= Math.min(a.getZ(), b.getZ()) - margin && location.getZ() <= Math.max(a.getZ(), b.getZ()) + 1.0 + margin
                && location.getY() >= Math.min(a.getY(), b.getY()) - 20.0;
    }

    private boolean confinedLobbyPlayer(Player player) {
        UUID id = player.getUniqueId();
        return player.getGameMode() != GameMode.CREATIVE && "lobby".equals(this.profiles.getOrDefault(id, "lobby"))
                && !this.activeDuelPlayers.contains(id) && !this.activePartyMatches.containsKey(id) && !this.activeParkourMaps.containsKey(id);
    }

    /** Unsichtbare Grenzen: Lobby/Spawn und Arenen kann man nicht zu Fuss verlassen. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBoundaryMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || from == null || (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ() && from.getBlockY() == to.getBlockY())) {
            return;
        }
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        boolean duel = this.activeDuelPlayers.contains(id) || this.activePartyMatches.containsKey(id);
        boolean blocked = false;
        if (duel) {
            blocked = this.insideDuelArena(player, from) && !this.insideDuelArena(player, to);
        } else if (this.confinedLobbyPlayer(player)) {
            blocked = this.isInLobby(from) && !this.isInLobby(to);
        }
        if (blocked) {
            event.setTo(from);
            long now = System.currentTimeMillis();
            Long last = this.boundaryMessage.get(id);
            if (last == null || now - last > 2000L) {
                this.boundaryMessage.put(id, now);
                player.sendMessage(String.valueOf(ChatColor.RED) + "Hier endet der Bereich.");
            }
        }
    }

    private final Map<UUID, Long> boundaryMessage = new ConcurrentHashMap<UUID, Long>();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBoundaryTeleport(PlayerTeleportEvent event) {
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL && cause != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            return;
        }
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        boolean duel = this.activeDuelPlayers.contains(id) || this.activePartyMatches.containsKey(id);
        if (duel ? !this.insideDuelArena(player, event.getTo()) : (this.confinedLobbyPlayer(player) && this.isInLobby(event.getFrom()) && !this.isInLobby(event.getTo()))) {
            event.setCancelled(true);
        }
    }

    /** SMP-Grenze (Quadrat um den SMP-Spawn, Radius smp.border-radius) - keine echte WorldBorder, damit ueberall gebaut werden kann. */
    boolean isInsideSmpBorder(Location location) {
        Location center = this.readConfiguredLocation("locations.spawns.smp.default");
        if (center == null || location == null || location.getWorld() == null) {
            return false;
        }
        World.Environment env = location.getWorld().getEnvironment();
        if (env == World.Environment.NORMAL && (center.getWorld() == null || !center.getWorld().equals((Object)location.getWorld()))) {
            return false;
        }
        double cx = this.borderCenterX(env, center);
        double cz = this.borderCenterZ(env, center);
        double radius = this.borderRadius(env);
        return Math.abs(location.getX() - cx) <= radius && Math.abs(location.getZ() - cz) <= radius;
    }

    /** Mittelpunkt der Grenze: Overworld = SMP-Spawn, Nether = SMP-Spawn / 8 (gleiche Stelle), End = 0/0. */
    double borderCenterX(World.Environment env, Location smp) {
        return env == World.Environment.NORMAL ? smp.getX() : env == World.Environment.NETHER ? smp.getX() / 8.0 : 0.0;
    }

    double borderCenterZ(World.Environment env, Location smp) {
        return env == World.Environment.NORMAL ? smp.getZ() : env == World.Environment.NETHER ? smp.getZ() / 8.0 : 0.0;
    }

    /** Radius der SMP-Grenze je Dimension. */
    double borderRadius(World.Environment env) {
        if (env == World.Environment.NETHER) {
            return Math.max(1.0, this.getConfig().getDouble("smp.nether-radius", 20000.0));
        }
        if (env == World.Environment.THE_END) {
            return Math.max(1.0, this.getConfig().getDouble("smp.end-radius", 50000.0));
        }
        return Math.max(1.0, this.getConfig().getDouble("smp.border-radius", 40000.0));
    }


    /** SMP-Spieler koennen die SMP-Grenze nicht zu Fuss oder per Perle durchqueren. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSmpBorderMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        Player player = event.getPlayer();
        if (to == null || from == null || (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ())) {
            return;
        }
        if (!"smp".equals(this.profiles.getOrDefault(player.getUniqueId(), "lobby")) || player.getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (this.isInsideSmpBorder(from) && !this.isInsideSmpBorder(to)) {
            event.setTo(from);
            long now = System.currentTimeMillis();
            Long last = this.boundaryMessage.get(player.getUniqueId());
            if (last == null || now - last > 2000L) {
                this.boundaryMessage.put(player.getUniqueId(), now);
                player.sendMessage(String.valueOf(ChatColor.RED) + "Das ist die Grenze der SMP-Welt.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSmpBorderTeleport(PlayerTeleportEvent event) {
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL && cause != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            return;
        }
        Player player = event.getPlayer();
        if ("smp".equals(this.profiles.getOrDefault(player.getUniqueId(), "lobby")) && this.isInsideSmpBorder(event.getFrom()) && !this.isInsideSmpBorder(event.getTo())) {
            event.setCancelled(true);
        }
    }


    private boolean isWithinSquare(Location location, Location center, double radius) {
        if (center == null || center.getWorld() == null || location == null || location.getWorld() == null || !center.getWorld().equals((Object)location.getWorld())) {
            return false;
        }
        return Math.abs(location.getX() - center.getX()) <= radius && Math.abs(location.getZ() - center.getZ()) <= radius;
    }

    Location readConfiguredLocation(String path) {
        World world;
        if (!this.getConfig().contains(path + ".world")) {
            return null;
        }
        String worldName = this.getConfig().getString(path + ".world");
        World world2 = world = worldName == null ? null : this.getServer().getWorld(worldName);
        if (world == null) {
            return null;
        }
        return new Location(world, this.getConfig().getDouble(path + ".x"), this.getConfig().getDouble(path + ".y"), this.getConfig().getDouble(path + ".z"));
    }

    private final Map<World.Environment, WorldBorder> visualBorders = new java.util.EnumMap<World.Environment, WorldBorder>(World.Environment.class);
    private final Map<UUID, String> borderShown = new ConcurrentHashMap<UUID, String>();

    /**
     * Die echte WorldBorder ist riesig (sonst kann man ausserhalb nicht bauen). Die SMP-Grenze (Overworld, Nether,
     * End) wird nur SMP-Spielern als rote Umrandung gezeigt (pro Spieler) und per Bewegungs-Check erzwungen.
     */
    private final Map<String, WorldBorder> zoneBorders = new ConcurrentHashMap<String, WorldBorder>();

    private WorldBorder zoneBorder(String key, double cx, double cz, double radius) {
        WorldBorder border = this.zoneBorders.get(key);
        if (border == null) {
            border = this.getServer().createWorldBorder();
            border.setWarningTime(0);
            this.zoneBorders.put(key, border);
        }
        // Lobby: kein roter Rand-Schimmer; SMP/Bau behalten die Warnung nahe der Grenze
        border.setWarningDistance("lobby".equals(key) ? 0 : 5);
        border.setCenter(cx, cz);
        border.setSize(radius * 2.0);
        return border;
    }

    /** Bau-SMP: eigene Welt (nicht die SMP-/Lobby-Welt) -> eigene Grenze rund um den Bau-Spawn. */
    boolean bauHasOwnBorder() {
        Location c = this.readConfiguredLocation("locations.spawns.creative.default");
        if (c == null || c.getWorld() == null) {
            return false;
        }
        String w = c.getWorld().getName();
        return !w.equals(this.getConfig().getString("locations.spawns.smp.default.world"))
                && !w.equals(this.getConfig().getString("locations.lobbies.haupt.world"));
    }

    /** Radius des Bau-SMP-Quadrats: bausmp.radius (falls gesetzt), sonst eigene Welt = bausmp.border-radius, geteilte Welt = bausmp.shared-radius. */
    double bauBorderRadius() {
        double explicit = this.getConfig().getDouble("bausmp.radius", 0.0);
        double radius;
        if (explicit > 0.0) {
            radius = Math.max(50.0, explicit);
        } else if (this.bauHasOwnBorder()) {
            return Math.max(50.0, this.getConfig().getDouble("bausmp.border-radius", 30000.0));
        } else {
            radius = Math.max(50.0, this.getConfig().getDouble("bausmp.shared-radius", 1500.0));
        }
        // In einer geteilten Welt darf das Bau-Quadrat weder den SMP-Spawn noch die Lobby verschlucken
        Location bau = this.readConfiguredLocation("locations.spawns.creative.default");
        if (bau != null) {
            Location smp = this.readConfiguredLocation("locations.spawns.smp.default");
            if (smp != null && smp.getWorld() != null && smp.getWorld().equals((Object)bau.getWorld())) {
                double d = Math.max(Math.abs(smp.getX() - bau.getX()), Math.abs(smp.getZ() - bau.getZ()));
                radius = Math.min(radius, Math.max(15.0, d - 60.0));
            }
            Location lobby = this.lobbyCenter();
            if (lobby != null && lobby.getWorld() != null && lobby.getWorld().equals((Object)bau.getWorld())) {
                double d = Math.max(Math.abs(lobby.getX() - bau.getX()), Math.abs(lobby.getZ() - bau.getZ()));
                radius = Math.min(radius, Math.max(15.0, d - this.getConfig().getDouble("protection.lobby-radius", 200.0) - 20.0));
            }
        }
        return radius;
    }

    /** true, wenn die Position im Bau-SMP-Quadrat liegt (auch in einer mit SMP/Lobby geteilten Welt). */
    boolean insideBauSmp(Location l) {
        return this.insideBauBorder(l);
    }

    boolean insideBauBorder(Location l) {
        Location c = this.readConfiguredLocation("locations.spawns.creative.default");
        if (c == null || l == null || l.getWorld() == null || !c.getWorld().equals((Object)l.getWorld())) {
            return false;
        }
        double r = this.bauBorderRadius();
        return Math.abs(l.getX() - c.getX()) <= r && Math.abs(l.getZ() - c.getZ()) <= r;
    }

    private void refreshBorderView() {
        try {
            Location center = this.readConfiguredLocation("locations.spawns.smp.default");
            Location bau = this.readConfiguredLocation("locations.spawns.creative.default");
            Location lobby = this.lobbyCenter();
            double lobbyRadius = Math.max(20.0, this.getConfig().getDouble("protection.lobby-radius", 200.0));
            // echte Welt-Grenze der Lobby-Welt: keine rote Warnung (sonst blitzt sie beim Beitritt kurz auf)
            if (lobby != null && lobby.getWorld() != null && lobby.getWorld().getWorldBorder().getWarningDistance() != 0) {
                lobby.getWorld().getWorldBorder().setWarningDistance(0);
            }
            if (center != null) {
                for (World.Environment env : List.of(World.Environment.NORMAL, World.Environment.NETHER, World.Environment.THE_END)) {
                    WorldBorder border = this.visualBorders.get(env);
                    if (border == null) {
                        border = this.getServer().createWorldBorder();
                        border.setWarningDistance(5);
                        border.setWarningTime(0);
                        this.visualBorders.put(env, border);
                    }
                    border.setCenter(this.borderCenterX(env, center), this.borderCenterZ(env, center));
                    border.setSize(this.borderRadius(env) * 2.0);
                }
            }
            WorldBorder bauBorder = bau == null ? null : this.zoneBorder("bau", bau.getX(), bau.getZ(), this.bauBorderRadius());
            WorldBorder lobbyBorder = lobby == null ? null : this.zoneBorder("lobby", lobby.getX(), lobby.getZ(), lobbyRadius);
            this.borderShown.keySet().removeIf(known -> this.getServer().getPlayer(known) == null);
            for (Player player : this.getServer().getOnlinePlayers()) {
                UUID id = player.getUniqueId();
                World.Environment env = player.getWorld().getEnvironment();
                String profile = this.profiles.getOrDefault(id, "lobby");
                String key = null;
                WorldBorder wb = null;
                if ("smp".equals(profile)) {
                    if (bau != null && this.insideBauBorder(player.getLocation())) {
                        key = "bau";
                        wb = bauBorder;
                    } else if (center != null && (env != World.Environment.NORMAL || (center.getWorld() != null && center.getWorld().equals((Object)player.getWorld())))) {
                        key = "smp:" + env;
                        wb = this.visualBorders.get(env);
                    }
                } else if ("lobby".equals(profile) && lobbyBorder != null && this.confinedLobbyPlayer(player) && this.isInLobby(player.getLocation())) {
                    key = "lobby";
                    wb = lobbyBorder;
                }
                String shown = this.borderShown.get(id);
                if (key != null && wb != null) {
                    if (!key.equals(shown)) {
                        this.borderShown.put(id, key);
                        player.setWorldBorder(wb);
                    }
                } else if (shown != null) {
                    this.borderShown.remove(id);
                    player.setWorldBorder(null);
                }
            }
        }
        catch (Throwable ignored) {
            // nur Optik
            Store.problem("DuelNetworkPlugin:5005", ignored);
        }
    }

    /** Waehrend Betreten/Verlassen des Bau-SMP darf die Zonen-Sperre nicht greifen. */
    final java.util.Set<UUID> bauTransit = ConcurrentHashMap.newKeySet();

    /**
     * SMP und Bau-SMP haben getrennte Inventare: Teleports (/home, /tpa, /rtp, Befehle, Plugins) duerfen nicht ueber die Grenze gehen,
     * sonst waere man mit SMP-Ausruestung im Bau-Bereich (oder mit Bau-Inventar im SMP). Admins sind ausgenommen.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onZoneTeleport(PlayerTeleportEvent event) {
        try {
            Player player = event.getPlayer();
            UUID id = player.getUniqueId();
            Location to = event.getTo();
            if (to == null || !"smp".equals(this.profiles.getOrDefault(id, "lobby")) || this.bauTransit.contains(id) || this.activeDuelPlayers.contains(id)
                    || player.getGameMode() == GameMode.CREATIVE || player.hasPermission("duelnetwork.admin")) {
                return;
            }
            if (this.insideBauBorder(to) != this.bauMode.contains(id)) {
                event.setCancelled(true);
                player.sendMessage(String.valueOf(ChatColor.RED) + (this.bauMode.contains(id)
                        ? "Du bist im Bau-SMP - ins SMP kommst du über das Menü (/spawn oder /lobby)."
                        : "Das Ziel liegt im Bau-SMP - das erreichst du nur über das Menü (/bausmp)."));
            }
        }
        catch (Throwable t) {
            Store.problem("DuelNetworkPlugin:onZoneTeleport", t);
        }
    }

    /** Wiedereinstieg nach dem Tod: Bau-SMP-Spieler im Bau-Spawn, SMP-Spieler nie im Bau-Bereich (z. B. durch ein dort gesetztes Bett). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onZoneRespawn(PlayerRespawnEvent event) {
        try {
            Player player = event.getPlayer();
            UUID id = player.getUniqueId();
            if (!"smp".equals(this.profiles.getOrDefault(id, "lobby")) || this.activeDuelPlayers.contains(id)) {
                return;
            }
            if (this.bauMode.contains(id)) {
                Location bau = this.readConfiguredLocation("locations.spawns.creative.default");
                if (bau != null) {
                    event.setRespawnLocation(bau);
                }
            } else if (event.getRespawnLocation() != null && this.insideBauBorder(event.getRespawnLocation())) {
                Location smp = this.readConfiguredLocation("locations.spawns.smp.default");
                if (smp != null) {
                    event.setRespawnLocation(smp);
                }
            }
        }
        catch (Throwable t) {
            Store.problem("DuelNetworkPlugin:onZoneRespawn", t);
        }
    }

    /** Bau-SMP-Grenze: nicht zu Fuss oder per Perle verlassbar. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBauBorderMove(PlayerMoveEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        Player player = event.getPlayer();
        if (to == null || from == null || (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ())
                || player.getGameMode() == GameMode.CREATIVE || !"smp".equals(this.profiles.getOrDefault(player.getUniqueId(), "lobby"))) {
            return;
        }
        boolean wasIn = this.insideBauBorder(from);
        boolean nowIn = this.insideBauBorder(to);
        if (wasIn == nowIn) {
            return;
        }
        event.setTo(from);
        long now = System.currentTimeMillis();
        Long last = this.boundaryMessage.get(player.getUniqueId());
        if (last == null || now - last > 2000L) {
            this.boundaryMessage.put(player.getUniqueId(), now);
            player.sendMessage(String.valueOf(ChatColor.RED) + (wasIn ? "Das ist die Grenze des Bau-SMP." : "Das Bau-SMP erreichst du nur \u00fcber das Men\u00fc (/bausmp)."));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBauBorderTeleport(PlayerTeleportEvent event) {
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL && cause != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            return;
        }
        Player player = event.getPlayer();
        if ("smp".equals(this.profiles.getOrDefault(player.getUniqueId(), "lobby")) && event.getTo() != null
                && this.insideBauBorder(event.getFrom()) != this.insideBauBorder(event.getTo())) {
            event.setCancelled(true);
        }
    }

    private void configureSmpWorldBorder() {
        Location center = this.readConfiguredLocation("locations.spawns.smp.default");
        if (center == null) {
            this.getLogger().warning("SMP-Spawn ist noch nicht eingerichtet; die SMP-Grenze konnte nicht gesetzt werden.");
            return;
        }
        double radius = Math.max(1.0, this.getConfig().getDouble("smp.border-radius", 40000.0));
        WorldBorder border = center.getWorld().getWorldBorder();
        border.setCenter(0.0, 0.0);
        border.setSize(5.9999968E7);
        border.setDamageBuffer(0.0);
        border.setDamageAmount(0.0);
        this.getLogger().info("SMP-Grenze: Zentrum " + center.getBlockX() + ", " + center.getBlockZ() + ", Radius " + radius + " Bl\u00f6cke (echte WorldBorder bleibt riesig, damit ueberall gebaut werden kann).");
    }

    /** Lobby-Schutz gilt nur fuer Spieler ohne SMP-Profil (und nicht im Duell) - SMP-/Bau-SMP-Spieler nehmen auch innerhalb des Lobby-Radius normal Schaden und bauen normal. */
    boolean lobbyProtected(Player player, Location location) {
        UUID id = player.getUniqueId();
        return !"smp".equals(this.profiles.getOrDefault(id, "lobby")) && !this.activeDuelPlayers.contains(id)
                && !this.activePartyMatches.containsKey(id) && this.isInLobby(location);
    }

    @EventHandler
    public void onLobbyDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && this.lobbyProtected(player, player.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDuelDamage(EntityDamageByEntityEvent event) {
        Player victim;
        Entity entity = event.getEntity();
        if (!(entity instanceof Player) || !this.activeDuelPlayers.contains((victim = (Player)entity).getUniqueId())) {
            return;
        }
        if (this.duelLeaveTasks.containsKey(victim.getUniqueId()) || this.duelResultPlayers.contains(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        String partyMatch = this.activePartyMatches.get(victim.getUniqueId());
        if (partyMatch != null) {
            Projectile projectile;
            ProjectileSource projectileSource;
            Long warmup = this.partyWarmupUntil.get(partyMatch);
            if (warmup != null && warmup > System.currentTimeMillis()) {
                event.setCancelled(true);
                return;
            }
            Player partyAttacker = null;
            Entity damager = event.getDamager();
            if (damager instanceof Player) {
                Player direct;
                partyAttacker = direct = (Player)damager;
            } else if (damager instanceof Projectile && (projectileSource = (projectile = (Projectile)damager).getShooter()) instanceof Player) {
                Player shooter;
                partyAttacker = shooter = (Player)projectileSource;
            }
            if (partyAttacker == null || !partyMatch.equals(this.activePartyMatches.get(partyAttacker.getUniqueId()))) {
                event.setCancelled(true);
            } else {
                Map<UUID, Integer> teamMap = this.partyMatchTeams.get(partyMatch);
                Integer own = teamMap == null ? null : teamMap.get(partyAttacker.getUniqueId());
                if (own != null && own.equals(teamMap.get(victim.getUniqueId())) && !partyAttacker.equals(victim)) {
                    event.setCancelled(true);
                }
            }
            return;
        }
        if (this.duelCountdownPlayers.contains(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        Player attacker = null;
        Entity entity2 = event.getDamager();
        if (entity2 instanceof Player) {
            Player direct;
            attacker = direct = (Player)entity2;
        } else {
            Projectile projectile;
            Entity entity3 = event.getDamager();
            if (entity3 instanceof Projectile && (projectile = (Projectile)entity3).getShooter() instanceof Player) {
                Player shooter;
                attacker = shooter = (Player)projectile.getShooter();
            }
        }
        UUID partnerId = this.duelPartners.get(victim.getUniqueId());
        if (attacker != null && (this.duelLeaveTasks.containsKey(attacker.getUniqueId()) || this.duelResultPlayers.contains(attacker.getUniqueId()))) {
            event.setCancelled(true);
            return;
        }
        // Crystal-, TNT-, Bett- und Respawn-Anker-Explosionen haben keinen
        // Spieler/Projectile als direkten Angreifer. Sie sind im Crystal-PvP
        // trotzdem echter Duellschaden und dürfen nicht als Fremdschaden
        // verworfen werden.
        if (attacker == null && this.duelExplosionDamageAllowed(victim, event.getCause())) {
            return;
        }
        if (attacker == null || partnerId == null || !partnerId.equals(attacker.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        this.duelDamage.merge(attacker.getUniqueId(), event.getFinalDamage(), Double::sum);
    }

    /** Bukkit liefert Explosionen je nach Quelle als EntityDamageEvent oder als EntityDamageByEntityEvent. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDuelEnvironmentalDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent || !(event.getEntity() instanceof Player victim)
                || !this.activeDuelPlayers.contains(victim.getUniqueId())) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                && event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) return;
        if (this.duelLeaveTasks.containsKey(victim.getUniqueId()) || this.duelResultPlayers.contains(victim.getUniqueId())
                || this.duelCountdownPlayers.contains(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (!this.duelExplosionDamageAllowed(victim, event.getCause())) event.setCancelled(true);
    }

    private boolean duelExplosionDamageAllowed(Player victim, EntityDamageEvent.DamageCause cause) {
        return (cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)
                && this.activeDuelPlayers.contains(victim.getUniqueId())
                && !this.duelLeaveTasks.containsKey(victim.getUniqueId())
                && !this.duelResultPlayers.contains(victim.getUniqueId())
                && !this.duelCountdownPlayers.contains(victim.getUniqueId())
                && "crystal pvp".equalsIgnoreCase(this.duelModes.getOrDefault(victim.getUniqueId(), ""));
    }

    @EventHandler
    public void onDuelCountdownDamage(EntityDamageEvent event) {
        Player player;
        Entity entity = event.getEntity();
        if (entity instanceof Player && this.duelCountdownPlayers.contains((player = (Player)entity).getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLobbyCombat(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity damager = event.getDamager();
        if ("dummy".equals(victim.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING))) {
            return;
        }
        if (damager instanceof org.bukkit.entity.Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }
        if ((victim instanceof Player v && this.lobbyProtected(v, v.getLocation())) || (damager instanceof Player d && this.lobbyProtected(d, d.getLocation()))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onLobbyBlockBreak(BlockBreakEvent event) {
        if (this.lobbyProtected(event.getPlayer(), event.getBlock().getLocation()) && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(String.valueOf(ChatColor.RED) + "In der Lobby darf nur im Kreativmodus gebaut werden.");
        }
    }

    @EventHandler
    public void onLobbyBlockPlace(BlockPlaceEvent event) {
        if (this.lobbyProtected(event.getPlayer(), event.getBlock().getLocation()) && event.getPlayer().getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(String.valueOf(ChatColor.RED) + "In der Lobby darf nur im Kreativmodus gebaut werden.");
        }
    }

    @EventHandler
    public void onLobbyCreatureSpawn(CreatureSpawnEvent event) {
        if (this.pendingNpcSpawns.contains(this.locationKey(event.getLocation()))) {
            return;
        }
        if (!this.isInSmpArea(event.getLocation())) {
            event.setCancelled(true);
        }
    }

    private Location readLocation(String path) {
        String worldName = this.getConfig().getString(path + ".world");
        return new Location(this.getServer().getWorld(worldName), this.getConfig().getDouble(path + ".x"), this.getConfig().getDouble(path + ".y"), this.getConfig().getDouble(path + ".z"), (float)this.getConfig().getDouble(path + ".yaw"), (float)this.getConfig().getDouble(path + ".pitch"));
    }

    private void decorateMenu(Inventory inventory) {
        ItemStack border = this.item(Material.PURPLE_STAINED_GLASS_PANE, "\u00a7r ");
        ItemStack background = this.item(Material.BLACK_STAINED_GLASS_PANE, "\u00a7r ");
        int rows = inventory.getSize() / 9;
        for (int slot = 0; slot < inventory.getSize(); ++slot) {
            if (inventory.getItem(slot) != null) continue;
            int row = slot / 9;
            int column = slot % 9;
            inventory.setItem(slot, row == 0 || row == rows - 1 || column == 0 || column == 8 ? border.clone() : background.clone());
        }
    }

    /* ---------- Icons (anpassbar) und Verfuegbarkeit ---------- */

    /** Material fuer einen Menue-Eintrag: config icons.<pfad>, sonst der Standard. */
    Material icon(String path, Material fallback) {
        String value = this.getConfig().getString("icons." + path, "");
        if (value == null || value.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(value.trim());
        return material == null || material.isAir() ? fallback : material;
    }

    private boolean spawnReady(String mode) {
        String path = "locations.spawns." + mode + ".default";
        if (!this.getConfig().contains(path + ".world")) {
            return false;
        }
        String worldName = this.getConfig().getString(path + ".world");
        return worldName != null && this.getServer().getWorld(worldName) != null;
    }

    private boolean duelReady() {
        ConfigurationSection arenas = this.getConfig().getConfigurationSection("duels.arenas");
        return arenas != null && !arenas.getKeys(false).isEmpty();
    }

    private boolean parkourReady(Player player) {
        ConfigurationSection maps = this.getConfig().getConfigurationSection("parkour.maps");
        if (maps == null) {
            return false;
        }
        for (String map : maps.getKeys(false)) {
            if (DuelFixes.parkourStatus(this, map, player).startsWith("OK")) {
                return true;
            }
        }
        return false;
    }

    /** Navigator-Eintrag: nicht verfuegbar (gesperrt/nicht eingerichtet) wird zum Barrier-Block. */
    private ItemStack navEntry(String type, Material fallback, String color, String label, String info, boolean ready) {
        boolean locked = this.isNpcLocked(type);
        if (locked || !ready) {
            return this.item(Material.BARRIER, "\u00a7c\u00a7l" + label, "\u00a77" + info + "\n\u00a78\u00a7m                         \n" + (locked ? "\u00a7cDerzeit gesperrt" : "\u00a7cNoch nicht eingerichtet"));
        }
        return this.item(this.icon("nav." + type, fallback), color + "\u00a7l" + label, info);
    }

    void openNavigator(Player player) {
        Inventory inv = this.getServer().createInventory(null, 36, String.valueOf(ChatColor.DARK_PURPLE) + "Netzwerk-Navigator" + String.valueOf(ChatColor.DARK_GRAY) + " \u2022 " + String.valueOf(ChatColor.WHITE) + "Spielmodi");
        String sep = "\u00a78\u00a7m                         ";
        String hint = "\n" + sep + "\n\u00a7e\u25b6 Klicken zum Betreten";
        int smpPlayers = 0;
        for (String profile : this.profiles.values()) {
            if (profile.equals("smp")) {
                ++smpPlayers;
            }
        }
        int duelQueued = 0;
        for (List<UUID> queue : this.duelQueues.values()) {
            duelQueued += queue.size();
        }
        inv.setItem(11, this.navEntry("smp", Material.GRASS_BLOCK, "\u00a7a", "SMP", "\u00a77Survival mit Homes, TPA und Handel\n" + sep + "\n\u00a78\u2503 \u00a77Spieler im SMP: \u00a7a" + smpPlayers + hint, this.spawnReady("smp")));
        inv.setItem(12, this.navEntry("duell", Material.IRON_SWORD, "\u00a7c", "Duelle", "\u00a77Duell-Lobby mit Ranked, Unranked und Teams\n" + sep + "\n\u00a78\u2503 \u00a77Laufende Duels: \u00a7c" + this.activeDuelPlayers.size() / 2 + "\n\u00a78\u2503 \u00a77In der Warteschlange: \u00a7c" + duelQueued + hint, true));
        inv.setItem(14, this.navEntry("events", Material.NETHER_STAR, "\u00a76", "Events", "\u00a77Laufende und geplante Events\n" + sep + "\n\u00a78\u2503 \u00a77Auch ohne laufendes Event \u00f6ffnen" + hint, true));
        inv.setItem(15, this.navEntry("parkour", Material.FEATHER, "\u00a7f", "Parkour", "\u00a77Strecken, Checkpoints und Bestzeiten\n" + sep + "\n\u00a78\u2503 \u00a77Spieler im Parkour: \u00a7f" + this.activeParkourMaps.size() + hint, this.parkourReady(player)));
        inv.setItem(13, this.navEntry("creative", Material.BRICKS, "\u00a7e", "Bau-SMP", "\u00a77Frei bauen im Survival\n\u00a7aKein PvP \u00a78\u2022 \u00a77Inventar bleibt" + hint, this.spawnReady("creative")));
        inv.setItem(20, this.item(Material.BOOK, "\u00a76\u00a7lRangliste", "\u00a77Beste Duel-Spieler und Parkour-Zeiten\n" + sep + "\n\u00a7e\u25b6 Klicken zum \u00d6ffnen"));
        inv.setItem(22, this.item(Material.PLAYER_HEAD, "\u00a7d\u00a7l" + player.getName(), "\u00a77Online: \u00a7f" + this.getServer().getOnlinePlayers().size() + "\u00a77 Spieler\n" + sep + "\n\u00a77Befehle: \u00a7d/duel \u00a77\u2022 \u00a7d/party \u00a77\u2022 \u00a7d/rangliste"));
        inv.setItem(24, this.item(Material.NETHER_STAR, "\u00a75\u00a7lParty", "\u00a77Spiele mit Freunden zusammen\n" + sep + "\n\u00a7e\u25b6 Klicken zum \u00d6ffnen"));
        this.decorateMenu(inv);
        player.openInventory(inv);
    }

    /**
     * Duell-Lobby: Duelle-NPC und Navigator bringen dich dorthin; dort stehen die NPCs Ranked, Unranked und Teams.
     * Ort setzen mit /duelladmin setlobby duell. Ohne gesetzte Lobby wird eine Einrichtungsmeldung angezeigt.
     */
    void enterDuelLobby(Player player) {
        if (!this.requireLobbyForDuels(player)) {
            return;
        }
        Location lobby = this.readConfiguredLocation("locations.lobbies.duell");
        if (lobby == null || lobby.getWorld() == null) {
            player.closeInventory();
            player.sendMessage("§cDie Duell-Lobby ist noch nicht eingerichtet.");
            if (player.hasPermission("duelnetwork.admin") || player.isOp()) {
                player.sendMessage("§7Zum Einrichten: §f/duelladmin setlobby duell");
            }
            return;
        }
        player.closeInventory();
        player.teleport(lobby);
        player.sendTitle("§c§lDuelle", "§7Ranked • Unranked • Teams", 5, 30, 10);
        Chat.sound(player, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.2f);
    }

    void enterEventLobby(Player player) {
        Location lobby = this.readConfiguredLocation("locations.lobbies.events");
        if (lobby == null || lobby.getWorld() == null) {
            Events.open(this, player);
            return;
        }
        UUID id = player.getUniqueId();
        if (!"lobby".equals(this.profiles.getOrDefault(id, "lobby"))) {
            this.switchProfile(player, "lobby");
            if (!"lobby".equals(this.profiles.getOrDefault(id, "lobby"))) return;
        }
        player.closeInventory();
        player.teleport(lobby);
        player.sendTitle("§d§lEVENTS", "§7Events, Termine und Party", 5, 30, 10);
        Chat.sound(player, org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.2f);
        player.sendMessage("§7Event-Menü: §d/events §8• §7Party: §d/party");
    }

    void openDuelKits(Player player, Player opponent) {
        if (!this.requireLobbyForDuels(player)) {
            return;
        }
        Inventory inv = this.getServer().createInventory(null, 27, String.valueOf(ChatColor.RED) + "Duel-Kits" + String.valueOf(ChatColor.DARK_GRAY) + " \u2022 " + String.valueOf(ChatColor.WHITE) + "W\u00e4hle dein Kit");
        List<String> kits = this.getConfig().getStringList("kits.available");
        if (kits.isEmpty()) {
            kits = List.of("axe pvp", "sword pvp", "mace pvp", "uhc", "op netherite", "smp", "crystal pvp");
        }
        boolean direct = opponent != null || this.outgoingDuelInvites.containsKey(player.getUniqueId()) || this.duelPartners.containsKey(player.getUniqueId());
        this.renderKitItems(inv, player, kits, direct);
        UUID me = player.getUniqueId();
        if (opponent != null || this.outgoingDuelInvites.containsKey(me) || this.duelPartners.containsKey(me)) {
            UUID host = this.duelHosts.getOrDefault(me, me);
            inv.setItem(22, this.bestOfItem(this.duelBestOf.getOrDefault(host, 1)));
        }
        this.decorateMenu(inv);
        player.openInventory(inv);
        if (opponent != null && opponent.isOnline() && this.canUseDuels(opponent)) {
            opponent.openInventory(inv);
            opponent.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Warte auf die Kit-Auswahl von " + player.getName() + ".");
        }
    }

    /** Fuellt die Kit-Slots: Elo nur in der Ranked-Ansicht; das Kit, in dem man wartet, ist rot und leuchtet. */
    private void renderKitItems(Inventory inv, Player player, List<String> kits, boolean direct) {
        UUID id = player.getUniqueId();
        boolean rankedView = !direct && !DuelStats.casual(id);
        int[] kitSlots = new int[]{10, 11, 12, 13, 14, 15, 16, 19};
        String queuedKey = this.queuedKit(id);
        if (!direct) {
            inv.setItem(4, this.item(rankedView ? Material.NETHER_STAR : Material.PAPER,
                    rankedView ? "\u00a7aModus: Ranked" : "\u00a7eModus: Unranked",
                    rankedView ? "\u00a77Elo z\u00e4hlt, Gegner mit \u00e4hnlicher Elo.\n\u00a7aSieg gegen St\u00e4rkere: viele Punkte\n\u00a7cSieg gegen Schw\u00e4chere: wenige Punkte\n\u00a7eKlick: zu Unranked wechseln" : "\u00a77Zuf\u00e4lliger Gegner, keine Elo.\n\u00a7eKlick: zu Ranked wechseln"));
        }
        for (int i = 0; i < kits.size() && i < kitSlots.length; ++i) {
            String kit = kits.get(i);
            String label = this.displayKitName(kit);
            boolean queued = !queuedKey.isEmpty() && queueKitName(queuedKey).equalsIgnoreCase(kit) && isRankedKey(queuedKey) == rankedView;
            boolean other = !queuedKey.isEmpty() && !queued;
            StringBuilder lore = new StringBuilder();
            if (queued) {
                lore.append("\u00a7c\u00a7lDu spielst dieses Kit \u00a77\u2013 Suche l\u00e4uft ...\n");
            }
            if (rankedView) {
                lore.append("\u00a7bDeine Elo: \u00a7f").append(DuelStats.kitElo(id, label)).append("\n");
                lore.append("\u00a77Gegner mit \u00e4hnlicher Elo\n");
            } else if (!direct) {
                lore.append("\u00a77Zuf\u00e4lliger Gegner (ohne Elo)\n");
            }
            if (!direct) {
                lore.append("\u00a7dWartend: ").append(this.queueSize(kit, rankedView)).append(" Spieler\n");
            }
            lore.append(queued ? "\u00a7cLinksklick: Suche abbrechen" : other ? "\u00a7eLinksklick: zu diesem Kit wechseln" : "\u00a7aLinksklick: Beitreten");
            lore.append("\n\u00a7dRechtsklick: Kit ansehen");
            ItemStack stack = this.item(queued ? Material.RED_CONCRETE : this.iconForKit(kit), (queued ? "\u00a7c" : "\u00a7f") + label, lore.toString());
            if (queued) {
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    meta.setEnchantmentGlintOverride(Boolean.TRUE);
                    stack.setItemMeta(meta);
                }
            }
            inv.setItem(kitSlots[i], stack);
        }
    }

    private List<String> availableKits() {
        List<String> kits = this.getConfig().getStringList("kits.available");
        return kits.isEmpty() ? List.of("axe pvp", "sword pvp", "mace pvp", "uhc", "op netherite", "smp", "crystal pvp") : kits;
    }

    /** Zeichnet das offene Kit-Menue nach einem Klick neu (Beitreten/Verlassen/Wechseln), ohne es zu schliessen. */
    private void refreshKitMenu(Player player) {
        this.getServer().getScheduler().runTask((Plugin)this, () -> {
            if (!player.isOnline() || !player.getOpenInventory().getTitle().contains("Duel-Kits")) {
                return;
            }
            boolean direct = this.duelPartners.containsKey(player.getUniqueId()) || this.outgoingDuelInvites.containsKey(player.getUniqueId());
            this.renderKitItems(player.getOpenInventory().getTopInventory(), player, this.availableKits(), direct);
        });
    }

    Material iconForKit(String kit) {
        Material custom = this.icon("kit." + kit.toLowerCase().replace(' ', '_'), Material.AIR);
        if (custom != Material.AIR) {
            return custom;
        }
        return switch (kit.toLowerCase()) {
            case "axe pvp" -> Material.DIAMOND_AXE;
            case "sword pvp" -> Material.DIAMOND_SWORD;
            case "mace pvp" -> Material.MACE;
            case "uhc" -> Material.GOLDEN_APPLE;
            case "op netherite" -> Material.NETHERITE_CHESTPLATE;
            case "smp" -> Material.SHIELD;
            case "crystal pvp" -> Material.END_CRYSTAL;
            default -> Material.NETHER_STAR;
        };
    }

    /** Lesbarer Kit-Name des laufenden Duells dieses Spielers (fuer Statistik/Verlauf). */
    String duelKitLabel(UUID id) {
        String mode = this.duelModes.get(id);
        return mode == null ? "" : this.displayKitName(mode);
    }

    /** Laeuft fuer den Spieler gerade ein Duel oder Party-Match? */
    boolean inActiveMatch(UUID id) {
        return this.activeDuelPlayers.contains(id) || this.activePartyMatches.containsKey(id);
    }

    /** Spieler, die aktuell in einem 1v1-Duel kaempfen (fuer die Zuschauerliste). */
    List<UUID> activeDuelFighters() {
        return new ArrayList<UUID>(this.activeDuelPlayers);
    }

    /** Ist der Spieler frei (Lobby, nicht im Duel/Match)? */
    boolean isIdleInLobby(Player player) {
        return this.canUseDuels(player) && !this.activePartyMatches.containsKey(player.getUniqueId());
    }

    String displayKitName(String kit) {
        return switch (kit.toLowerCase()) {
            case "axe pvp" -> "Axe";
            case "sword pvp" -> "Sword";
            case "mace pvp" -> "Mace";
            case "op netherite" -> "OP Netherite";
            case "crystal pvp" -> "Crystal";
            default -> kit.equalsIgnoreCase("uhc") ? "UHC" : kit.toUpperCase();
        };
    }

    private String internalKitName(String name) {
        return switch (name.toLowerCase()) {
            case "axe", "axe pvp" -> "axe pvp";
            case "sword", "sword pvp" -> "sword pvp";
            case "mace", "mace pvp" -> "mace pvp";
            case "crystal", "crystal pvp" -> "crystal pvp";
            case "op", "op netherite" -> "op netherite";
            default -> name.toLowerCase();
        };
    }

    private int queueSize(String kit, boolean ranked) {
        int waiting = this.duelQueues.getOrDefault(queueKey(kit, ranked), List.of()).size();
        int active = (int)this.duelModes.values().stream().filter(kit.toLowerCase()::equals).count();
        return waiting + active;
    }

    /** Vom Duel-Ende-Button "Nächster Gegner": gleiche Warteschlange wie zuvor. */
    void requeueDuel(Player player, String kitName) {
        this.joinRandomDuel(player, kitName);
    }

    /** Zuletzt gewaehltes Warteschlangen-Kit pro Spieler (fuer /duel last). */
    private final Map<UUID, String> lastKit = new ConcurrentHashMap<UUID, String>();

    private void joinRandomDuel(Player player, String kitName) {
        if (!this.requireLobbyForDuels(player)) {
            return;
        }
        if (this.duelLeaveCheck(player, kitName)) {
            return;
        }
        UUID id = player.getUniqueId();
        if (this.activeDuelPlayers.contains(id) || this.duelPartners.containsKey(id) || this.outgoingDuelInvites.containsKey(id)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Du bist bereits besch\u00e4ftigt.");
            return;
        }
        String kit = kitName.toLowerCase();
        this.lastKit.put(id, kitName);
        boolean ranked = !DuelStats.casual(id);
        String key = queueKey(kit, ranked);
        List<UUID> queue = this.duelQueues.computeIfAbsent(key, ignored -> new ArrayList<UUID>());
        queue.removeIf(uuid -> this.getServer().getPlayer(uuid) == null);
        if (queue.contains(id)) {
            queue.remove(id);
            this.queuedSince.remove(id);
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Du hast die " + (ranked ? "Ranked" : "Unranked") + "-Warteschlange f\u00fcr " + kitName + " verlassen.");
            return;
        }
        String previousKey = this.queuedKit(id);
        if (!previousKey.isBlank()) {
            ((List)this.duelQueues.getOrDefault(previousKey, new ArrayList())).remove(id);
            this.queuedSince.remove(id);
            player.sendMessage(String.valueOf(ChatColor.GRAY) + "Du wurdest aus der Warteschlange f\u00fcr " + queueLabel(previousKey) + " entfernt.");
        }
        if (!ranked) {
            // Unranked: zuf\u00e4lliger (erster wartender) Gegner, keine Elo-Wertung
            if (!queue.isEmpty()) {
                UUID opponentId = (UUID)queue.remove(0);
                Player opponent = this.getServer().getPlayer(opponentId);
                if (opponent != null && opponent.isOnline()) {
                    this.queuedSince.remove(opponentId);
                    this.pairQueued(player, opponent, kit, false);
                    return;
                }
            }
            queue.add(id);
            this.queuedSince.put(id, System.currentTimeMillis());
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Du bist f\u00fcr " + kitName + " (Unranked) in der Warteschlange.");
            player.sendMessage(String.valueOf(ChatColor.GRAY) + "Aktuell warten " + queue.size() + " Spieler auf diesen Modus. Deine Elo \u00e4ndert sich nicht.");
            return;
        }
        // Ranked: Gegner mit \u00e4hnlicher Elo in diesem Kit; der Suchbereich wird mit der Wartezeit gr\u00f6\u00dfer
        int myElo = DuelStats.kitElo(id, this.displayKitName(kit));
        UUID best = this.bestRankedOpponent(queue, kit, myElo, id, 0L);
        if (best != null) {
            Player opponent = this.getServer().getPlayer(best);
            queue.remove(best);
            this.queuedSince.remove(best);
            if (opponent != null && opponent.isOnline()) {
                this.pairQueued(player, opponent, kit, true);
                return;
            }
        }
        queue.add(id);
        this.queuedSince.put(id, System.currentTimeMillis());
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Du bist f\u00fcr " + kitName + " (Ranked) in der Warteschlange. \u00a77Deine Elo: \u00a7f" + myElo);
        player.sendMessage(String.valueOf(ChatColor.GRAY) + "Gesucht wird ein Gegner mit \u00e4hnlicher Elo (\u00b1" + this.rankedRange(0L) + "); der Bereich wird mit der Zeit gr\u00f6\u00dfer.");
    }

    /* ---------- Warteschlangen: Ranked und Unranked getrennt, Elo pro Kit ---------- */

    private final Map<UUID, Boolean> duelRankedFlags = new ConcurrentHashMap<UUID, Boolean>();
    private final Map<UUID, Long> queuedSince = new ConcurrentHashMap<UUID, Long>();

    static String queueKey(String kit, boolean ranked) {
        return (ranked ? "ranked:" : "casual:") + kit.toLowerCase();
    }

    private static String queueKitName(String key) {
        int i = key.indexOf(':');
        return i < 0 ? key : key.substring(i + 1);
    }

    private static boolean isRankedKey(String key) {
        return key.startsWith("ranked:");
    }

    private String queueLabel(String key) {
        return this.displayKitName(queueKitName(key)) + (isRankedKey(key) ? " (Ranked)" : " (Unranked)");
    }

    /** Gewertet (true), ungewertet (false) oder unbekannt (null, z. B. direkte Herausforderung): Das Duell merkt sich, aus welcher Warteschlange es kam. */
    Boolean queueRankedFlag(UUID id) {
        return this.duelRankedFlags.get(id);
    }

    /** Suchbereich in Elo-Punkten nach Wartezeit: Start, pro 5 Sekunden mehr, nach der Maximalzeit unbegrenzt. */
    private int rankedRange(long waitedMs) {
        int start = Math.max(25, this.getConfig().getInt("duels.match-range-start", 100));
        int step = Math.max(10, this.getConfig().getInt("duels.match-range-step", 75));
        long unlimitedAfter = Math.max(10L, this.getConfig().getLong("duels.match-unlimited-after-seconds", 30L)) * 1000L;
        if (waitedMs >= unlimitedAfter) {
            return Integer.MAX_VALUE;
        }
        return start + (int)(waitedMs / 5000L) * step;
    }

    private UUID bestRankedOpponent(List<UUID> queue, String kit, int myElo, UUID self, long myWaitedMs) {
        UUID best = null;
        int bestDiff = Integer.MAX_VALUE;
        long now = System.currentTimeMillis();
        String label = this.displayKitName(kit);
        for (UUID other : queue) {
            if (other.equals(self) || this.getServer().getPlayer(other) == null) {
                continue;
            }
            int diff = Math.abs(DuelStats.kitElo(other, label) - myElo);
            long theirWait = now - this.queuedSince.getOrDefault(other, now);
            int range = Math.max(this.rankedRange(myWaitedMs), this.rankedRange(theirWait));
            if (diff <= range && diff < bestDiff) {
                best = other;
                bestDiff = diff;
            }
        }
        return best;
    }

    private void pairQueued(Player a, Player b, String kit, boolean ranked) {
        this.duelRankedFlags.put(a.getUniqueId(), ranked);
        this.duelRankedFlags.put(b.getUniqueId(), ranked);
        this.duelPartners.put(a.getUniqueId(), b.getUniqueId());
        this.duelPartners.put(b.getUniqueId(), a.getUniqueId());
        this.startDuelMatch(a, b, kit);
    }

    /** L\u00e4uft jede Sekunde: Wartende Ranked-Spieler werden mit dem n\u00e4chstliegenden Gegner gepaart, sobald der Suchbereich reicht. */
    private void matchmakeRanked() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, List<UUID>> entry : this.duelQueues.entrySet()) {
            if (!isRankedKey(entry.getKey())) {
                continue;
            }
            List<UUID> queue = entry.getValue();
            queue.removeIf(uuid -> this.getServer().getPlayer(uuid) == null);
            String kit = queueKitName(entry.getKey());
            String label = this.displayKitName(kit);
            boolean paired = true;
            while (paired && queue.size() >= 2) {
                paired = false;
                // L\u00e4ngste Wartezeit zuerst
                List<UUID> order = new ArrayList<UUID>(queue);
                order.sort((x, y) -> Long.compare(this.queuedSince.getOrDefault(x, now), this.queuedSince.getOrDefault(y, now)));
                for (UUID first : order) {
                    UUID second = this.bestRankedOpponent(queue, kit, DuelStats.kitElo(first, label), first, now - this.queuedSince.getOrDefault(first, now));
                    Player pa = this.getServer().getPlayer(first);
                    Player pb = second == null ? null : this.getServer().getPlayer(second);
                    if (pa != null && pb != null) {
                        queue.remove(first);
                        queue.remove(second);
                        this.queuedSince.remove(first);
                        this.queuedSince.remove(second);
                        this.pairQueued(pa, pb, kit, true);
                        paired = true;
                        break;
                    }
                }
            }
        }
    }

    private String queuedKit(UUID playerId) {
        for (Map.Entry<String, List<UUID>> entry : this.duelQueues.entrySet()) {
            if (!entry.getValue().contains(playerId)) continue;
            return entry.getKey();
        }
        return "";
    }

    @EventHandler
    public void onMenuNpcClick(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        String type = (String)entity.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING);
        if (type == null) {
            return;
        }
        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // Klick mit der Zweithand wuerde das Menue/die Aktion doppelt ausloesen
        }
        if (this.isNpcLocked(type)) {
            return;
        }
        Player player = event.getPlayer();
        switch (type) {
            case "navigator": {
                this.openNavigator(player);
                break;
            }
            case "smp": {
                this.switchProfile(player, "smp");
                BauSmp.title(player, "smp");
                if (this.teleportToLastSmpLocation(player)) break;
                this.teleportToSpawn(player, "smp");
                break;
            }
            case "duell": {
                this.enterDuelLobby(player);
                break;
            }
            case "ranked":
            case "unranked": {
                if (!this.requireLobbyForDuels(player)) break;
                boolean wantRanked = type.equals("ranked");
                if (DuelStats.casual(player.getUniqueId()) == wantRanked) {
                    DuelStats.toggleCasual(player.getUniqueId());
                    // Moduswechsel: alte Warteschlange verlassen, sonst wartet man im falschen Modus weiter
                    boolean wasQueued = false;
                    for (List<UUID> q : this.duelQueues.values()) {
                        wasQueued |= q.remove(player.getUniqueId());
                    }
                    this.queuedSince.remove(player.getUniqueId());
                    if (wasQueued) {
                        player.sendMessage("§7Du hast die alte Warteschlange verlassen, weil du den Modus gewechselt hast.");
                    }
                }
                player.sendMessage(wantRanked ? "§7Modus: §aRanked §8– §7eigene Elo je Kit, Gegner mit ähnlicher Elo."
                        : "§7Modus: §eUnranked §8– §7deine Elo ändert sich nicht (auch nicht die deines Gegners).");
                this.openDuelKits(player, null);
                break;
            }
            case "team": {
                TeamQueue.open(this, player);
                break;
            }
            case "dummy": {
                player.sendMessage("§7Schlag auf mich ein – über der Hotbar siehst du Schaden, Treffer und DPS.");
                break;
            }
            case "events": {
                this.enterEventLobby(player);
                break;
            }
            case "parkour": {
                this.openParkourMenu(player);
                break;
            }
            case "creative": {
                BauSmp.enter(this, player);
                break;
            }
            case "battlepass": {
                break;
            }
            case "battlepassrewards": {
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPluginNpcDamage(EntityDamageEvent event) {
        String type = (String)event.getEntity().getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING);
        if (type == null) return;
        if (type.equals("dummy")) {
            if (event.getEntity() instanceof LivingEntity living && event.getFinalDamage() >= living.getHealth()) {
                event.setDamage(Math.max(0.1, living.getHealth() - 1.0));
            }
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onDuelNpcLeftClick(EntityDamageByEntityEvent event) {
        Entity entity = event.getDamager();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        Entity entity2 = event.getEntity();
        String type = (String)entity2.getPersistentDataContainer().get(this.npcKey, PersistentDataType.STRING);
        if (type == null) {
            return;
        }
        if (this.isNpcLocked(type) && (player.getGameMode() != GameMode.CREATIVE || !player.hasPermission("duelnetwork.admin"))) {
            event.setCancelled(true);
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE && player.hasPermission("duelnetwork.admin")) {
            event.setCancelled(true);
            this.removePluginNpc(entity2);
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "NPC entfernt: " + type);
            return;
        }
        if (!"duell".equals(type)) {
            return;
        }
        event.setCancelled(true);
        this.enterDuelLobby(player);
    }

    private void removePluginNpc(Entity entity) {
        entity.remove();
    }

    @EventHandler
    public void onDuelDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        this.clearSmpCombat(player);
        Player slayer = player.getKiller();
        if (slayer != null && !slayer.equals(player) && this.isInSmpCombat(slayer)) {
            // Kill = Combat sofort beendet
            this.clearSmpCombat(slayer);
            slayer.sendMessage(String.valueOf(ChatColor.GREEN) + "Kill! Du bist nicht mehr im Combat.");
        }
        if (this.activePartyMatches.containsKey(player.getUniqueId())) {
            event.getDrops().clear();
            this.getServer().getScheduler().runTask((Plugin)this, () -> this.handlePartyDeath(player));
            return;
        }
        if (!this.activeDuelPlayers.contains(player.getUniqueId())) {
            return;
        }
        event.getDrops().clear();
        UUID partnerId = this.duelPartners.get(player.getUniqueId());
        this.getServer().getScheduler().runTask((Plugin)this, () -> {
            Player partner = partnerId == null ? null : this.getServer().getPlayer(partnerId);
            Player player2 = partner;
            if (partner != null && this.activeDuelPlayers.contains(partner.getUniqueId())) {
                if (this.seriesRound(player, partner)) {
                    return;
                }
                UUID loserId = player.getUniqueId();
                UUID winnerId = partner.getUniqueId();
                this.pendingDuelLosers.add(loserId);
                this.duelResultPlayers.add(loserId);
                this.duelResultPlayers.add(winnerId);
                this.duelDeathWinners.put(loserId, winnerId);
                Location deathLocation = player.getLocation().clone();
                this.duelDeathLocations.put(loserId, deathLocation);
                partner.sendTitle(String.valueOf(ChatColor.GREEN) + "GEWONNEN!", String.valueOf(ChatColor.LIGHT_PURPLE) + "R\u00fcckkehr zur Lobby l\u00e4uft...", 0, 40, 10);
                partner.playSound(partner.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                player.sendTitle(String.valueOf(ChatColor.RED) + "VERLOREN!", String.valueOf(ChatColor.GRAY) + "R\u00fcckkehr zur Lobby l\u00e4uft...", 0, 40, 10);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.7f);
                int finishTask = this.getServer().getScheduler().runTaskLater((Plugin)this, () -> {
                    this.duelDeathFinishTasks.remove(loserId);
                    this.pendingDuelLosers.remove(loserId);
                    this.duelResultPlayers.remove(loserId);
                    this.duelResultPlayers.remove(winnerId);
                    this.duelDeathWinners.remove(loserId);
                    this.duelDeathLocations.remove(loserId);
                    Player currentWinner = this.getServer().getPlayer(winnerId);
                    Player currentLoser = this.getServer().getPlayer(loserId);
                    if (currentWinner != null && currentLoser != null && this.activeDuelPlayers.contains(winnerId) && this.activeDuelPlayers.contains(loserId)) {
                        this.finishDuelResult(currentWinner, currentLoser, "Der Gegner ist gestorben.");
                    } else {
                        if (currentWinner != null) {
                            this.finishDuel(currentWinner);
                        }
                        if (currentLoser != null) {
                            this.finishDuel(currentLoser);
                        }
                    }
                    if (currentWinner != null) {
                        this.scheduleGuaranteedDuelLobbyReturn(currentWinner);
                    }
                    if (currentLoser != null) {
                        this.scheduleGuaranteedDuelLobbyReturn(currentLoser);
                    }
                }, 60L).getTaskId();
                this.duelDeathFinishTasks.put(loserId, finishTask);
            } else {
                this.finishDuel(player);
            }
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        String title = event.getView().getTitle();
        if (title.contains("Parkour-Strecken")) {
            ItemMeta meta;
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR && event.getRawSlot() < event.getView().getTopInventory().getSize() && (meta = clicked.getItemMeta()) != null && meta.hasDisplayName() && clicked.getType() == Material.BARRIER) {
                player.sendMessage(String.valueOf(ChatColor.RED) + "Diese Strecke ist derzeit nicht verf\u00fcgbar.");
            } else if (clicked != null && clicked.getType() != Material.AIR && event.getRawSlot() < event.getView().getTopInventory().getSize() && (meta = clicked.getItemMeta()) != null && meta.hasDisplayName() && !ChatColor.stripColor((String)meta.getDisplayName()).isBlank()) {
                String shown = ChatColor.stripColor((String)meta.getDisplayName());
                String map = null;
                ConfigurationSection maps = this.getConfig().getConfigurationSection("parkour.maps");
                if (maps != null) {
                    for (String candidate : maps.getKeys(false)) {
                        if (!this.displayParkourName(candidate).equalsIgnoreCase(shown)) continue;
                        map = candidate;
                        break;
                    }
                }
                if (map == null) {
                    map = shown.toLowerCase().replace(' ', '_');
                }
                player.closeInventory();
                this.joinParkour(player, map);
            }
            return;
        }
        if (title.contains(RECORDS_GUI)) {
            this.handleRecordsGuiClick(event, player, title);
            return;
        }
        if (title.contains(PARTY_MENU) || title.contains(PARTY_PICK)) {
            this.handlePartyMenuClick(event, player, title);
            return;
        }
        if (title.contains("Kit-Editor:")) {
            this.handleKitEditorClick(event, player, true);
            return;
        }
        if (title.contains("Kit Vorschau:")) {
            this.handleKitEditorClick(event, player, false);
            return;
        }
        if (title.contains("Battle Pass") || title.contains("Battle-Pass-Belohnungen")) {
            event.setCancelled(true);
            return;
        }
        if (!(title.contains("Netzwerk-Navigator") || title.contains("Duel-Kits") || title.contains("Party-Kits") || Gui.isTitle(title, "Homes") || Gui.isTitle(title, "TPA"))) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        String name = ChatColor.stripColor((String)clicked.getItemMeta().getDisplayName());
        if (name == null || name.isBlank()) {
            return; // Rahmen/Hintergrund des Menues
        }
        if (Gui.isTitle(title, "Homes")) {
            int[] slots = new int[]{11, 12, 13, 14, 15};
            int homeSlot = -1;
            for (int i = 0; i < slots.length; ++i) {
                if (slots[i] != event.getRawSlot()) continue;
                homeSlot = i;
            }
            if (homeSlot < 0) {
                return;
            }
            List homes = this.getConfig().getStringList(this.homeListPath(player));
            if (homeSlot < homes.size()) {
                String homeName = (String)homes.get(homeSlot);
                player.closeInventory();
                if (event.isRightClick()) {
                    this.handleHome(player, new String[]{homeName, "delete"});
                    this.getServer().getScheduler().runTask((Plugin)this, () -> this.openHomeMenu(player));
                } else {
                    this.handleHome(player, new String[]{homeName});
                }
            } else {
                int max;
                int n = max = this.hasFiveHomes(player) ? 5 : 3;
                if (homeSlot >= max) {
                    player.sendMessage(String.valueOf(ChatColor.RED) + "Dieser Home-Slot ist f\u00fcr deinen Rang gesperrt.");
                } else {
                    this.pendingHomeSlots.put(player.getUniqueId(), homeSlot);
                    player.closeInventory();
                    player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Gib jetzt den gew\u00fcnschten Home-Namen in den Chat ein (1\u201316 Zeichen: a-z, 0-9, _, -). Oder schreibe cancel.");
                }
            }
            return;
        }
        if (Gui.isTitle(title, "TPA")) {
            int raw = event.getRawSlot();
            player.closeInventory();
            if (raw == 10 && this.tpaRequests.containsKey(player.getUniqueId())) {
                this.handleTpa(player, new String[]{"accept"});
            } else if (raw == 12 && this.tpaRequests.containsKey(player.getUniqueId())) {
                this.handleTpa(player, new String[]{"deny"});
            } else if (raw == 14 && (this.outgoingTpaRequests.containsKey(player.getUniqueId()) || this.delayedTeleportTasks.containsKey(player.getUniqueId()))) {
                this.handleTpa(player, new String[]{"cancel"});
            } else if (raw >= 19 && clicked.getType() == Material.PLAYER_HEAD) {
                this.handleTpa(player, new String[]{name});
            }
            return;
        }
        if (title.contains("Netzwerk-Navigator")) {
            if (name.equals("SMP")) {
                this.saveLastSmpLocation(player);
                this.switchProfile(player, "smp");
                if (!this.teleportToLastSmpLocation(player)) {
                    this.teleportToSpawn(player, "smp");
                }
            } else if (name.equals("Duelle")) {
                player.closeInventory();
                this.enterDuelLobby(player);
            } else if (name.equals("Rangliste")) {
                player.closeInventory();
                player.performCommand("rangliste");
            } else if (name.equals("Party")) {
                player.closeInventory();
                player.performCommand("party");
            } else if (name.equals("Events")) {
                Events.open(this, player);
            }
            return;
        }
        if (title.contains("Party-Kits")) {
            if (name.startsWith("Best of")) {
                this.duelHosts.put(player.getUniqueId(), player.getUniqueId());
                this.cycleBestOf(player, event.getView().getTopInventory());
                return;
            }
            if (event.isLeftClick() && !name.equals("Party-Kits")) {
                String mode = this.partyPendingModes.remove(player.getUniqueId());
                UUID opponent = this.partyPendingOpponents.remove(player.getUniqueId());
                String kitKey = this.internalKitName(name);
                if (mode != null && this.validPartyKit(kitKey)) {
                    if (mode.equalsIgnoreCase("FFA")) {
                        this.startPartyFfa(player, kitKey);
                    } else if (mode.equalsIgnoreCase("TEAM")) {
                        this.startPartyTeams(player, kitKey);
                    } else if (opponent != null) {
                        this.startPartyOneVsOne(player, this.getServer().getPlayer(opponent) == null ? "" : this.getServer().getPlayer(opponent).getName(), kitKey);
                    }
                }
                player.closeInventory();
            }
            return;
        }
        if (title.contains("Duel-Kits")) {
            if (name.startsWith("Best of")) {
                this.cycleBestOf(player, event.getView().getTopInventory());
                return;
            }
            if (name.startsWith("Modus:")) {
                DuelStats.toggleCasual(player.getUniqueId());
                this.leaveDuelQueueQuiet(player);
                this.openDuelKits(player, null);
                return;
            }
            if (!name.equals("Duel-Kits")) {
                String kitKey = this.internalKitName(name);
                if (event.isRightClick()) {
                    this.openKitPreview(player, kitKey);
                } else if (event.isLeftClick()) {
                    // Eine direkte /duel-Spieler-Einladung reserviert den Herausforderer bereits,
                    // bevor der Gegner annimmt. In diesem Zustand Kit vormerken, nicht Queue joinen.
                    if (this.duelPartners.containsKey(player.getUniqueId()) || this.outgoingDuelInvites.containsKey(player.getUniqueId())) {
                        this.beginDuelKit(player, kitKey);
                    } else {
                        this.joinRandomDuel(player, kitKey);
                        this.refreshKitMenu(player);
                    }
                }
            }
            return;
        }
        if (clicked.getType() == Material.BARRIER && title.contains("Netzwerk-Navigator")) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Dieser Bereich ist derzeit nicht verf\u00fcgbar.");
        }
    }

    /* ---------- Kit-Editor (Anordnung pro Spieler, Items nur durch Admins) ---------- */

    /** Inhalt eines Kits: 36 Inventar-Slots (0-8 Hotbar), Ruestung (0=Schuhe..3=Helm) und Offhand. */
    static final class KitSet {
        final ItemStack[] storage = new ItemStack[36];
        final ItemStack[] armor = new ItemStack[4];
        ItemStack offhand = new ItemStack(Material.AIR);
    }

    private static int armorIndex(Material material) {
        String name = material.name();
        boolean tier = name.startsWith("LEATHER_") || name.startsWith("CHAINMAIL_") || name.startsWith("IRON_") || name.startsWith("GOLDEN_") || name.startsWith("DIAMOND_") || name.startsWith("NETHERITE_");
        if (!tier) {
            return -1;
        }
        if (name.endsWith("_BOOTS")) {
            return 0;
        }
        if (name.endsWith("_LEGGINGS")) {
            return 1;
        }
        if (name.endsWith("_CHESTPLATE")) {
            return 2;
        }
        if (name.endsWith("_HELMET")) {
            return 3;
        }
        return -1;
    }

    KitSet buildKitSet(String kitName, List<?> rawStored) {
        KitSet set = new KitSet();
        if (rawStored == null) {
            return set;
        }
        List<Object> stored = new ArrayList<Object>(rawStored);
        String normalizedKit = kitName.toLowerCase();
        if (normalizedKit.equals("crystal pvp")) {
            long totemCount = stored.stream().filter(value -> value instanceof ItemStack && ((ItemStack)value).getType() == Material.TOTEM_OF_UNDYING).count();
            for (int i = (int)totemCount; i < 12; ++i) {
                stored.add(this.totem());
            }
        }
        if (normalizedKit.equals("op netherite")) {
            long potionCount = stored.stream().filter(value -> value instanceof ItemStack && ((ItemStack)value).getType() == Material.SPLASH_POTION).count();
            for (int i = (int)potionCount; i < 16; ++i) {
                stored.add(this.potion(PotionEffectType.INSTANT_HEALTH, 1, 1, 1, "Heilung"));
            }
        }
        int slot = 0;
        for (Object e : stored) {
            if (!(e instanceof ItemStack) || ((ItemStack)e).getType() == Material.AIR) {
                continue;
            }
            ItemStack copy = ((ItemStack)e).clone();
            if (this.isWindChargeKit(kitName) && copy.getType() == Material.MACE && copy.containsEnchantment(Enchantment.WIND_BURST)) {
                copy.removeEnchantment(Enchantment.WIND_BURST);
                copy.addUnsafeEnchantment(Enchantment.WIND_BURST, 1);
            }
            this.normalizeDuelKitItem(kitName, copy);
            int armorSlot = armorIndex(copy.getType());
            if (armorSlot >= 0) {
                set.armor[armorSlot] = copy;
                continue;
            }
            if ((copy.getType() == Material.SHIELD || copy.getType() == Material.TOTEM_OF_UNDYING) && set.offhand.getType() == Material.AIR) {
                set.offhand = copy;
                continue;
            }
            if (slot < set.storage.length) {
                set.storage[slot++] = copy;
            }
        }
        return set;
    }

    /** Ordnet die Kit-Items nach der gespeicherten Anordnung (persoenlich, sonst Standard). Es kommen nie neue Items dazu. */
    void arrangeKit(UUID id, String kitName, KitSet set) {
        String kit = this.internalKitName(kitName);
        ItemStack[] layout = id == null ? null : this.kitLayouts.personal(id, kit);
        if (layout == null) {
            layout = this.kitLayouts.standard(kit);
        }
        if (layout == null) {
            return;
        }
        boolean layoutHasOffhand = layout.length > 36;
        List<ItemStack> pool = new ArrayList<ItemStack>();
        for (ItemStack stack : set.storage) {
            if (stack != null && stack.getType() != Material.AIR) {
                pool.add(stack);
            }
        }
        if (layoutHasOffhand && set.offhand.getType() != Material.AIR) {
            pool.add(set.offhand);
            set.offhand = new ItemStack(Material.AIR);
        }
        ItemStack[] out = new ItemStack[set.storage.length];
        for (int i = 0; i < layout.length; ++i) {
            ItemStack want = layout[i];
            if (want == null || want.getType() == Material.AIR) {
                continue;
            }
            if (i == 36 && want.getType() != Material.SHIELD && want.getType() != Material.TOTEM_OF_UNDYING) {
                continue;
            }
            int pick = -1;
            for (int j = 0; j < pool.size(); ++j) {
                if (!pool.get(j).isSimilar(want)) {
                    continue;
                }
                if (pick < 0) {
                    pick = j;
                }
                if (pool.get(j).getAmount() == want.getAmount()) {
                    pick = j;
                    break;
                }
            }
            if (pick < 0) {
                continue;
            }
            if (i == 36) {
                set.offhand = pool.remove(pick);
            } else if (i < out.length) {
                out[i] = pool.remove(pick);
            }
        }
        int next = 0;
        for (ItemStack rest : pool) {
            while (next < out.length && out[next] != null) {
                ++next;
            }
            if (next < out.length) {
                out[next++] = rest;
            } else if (set.offhand.getType() == Material.AIR) {
                set.offhand = rest;
            }
        }
        System.arraycopy(out, 0, set.storage, 0, out.length);
    }

    /** Inventar-Slot (0-8 Hotbar, 9-35 Inventar) -> Slot im Editor: Inventar oben, Trennlinie, Hotbar unten. */
    private static int editorSlot(int storageIndex) {
        return storageIndex >= 9 ? storageIndex : 45 + storageIndex;
    }

    private static int storageIndex(int editorSlot) {
        if (editorSlot >= 9 && editorSlot <= 35) {
            return editorSlot;
        }
        if (editorSlot >= 45 && editorSlot <= 53) {
            return editorSlot - 45;
        }
        return -1;
    }

    private boolean isKitAdmin(Player player) {
        return player.isOp() || player.hasPermission("duelnetwork.admin");
    }

    /** Vorschlaege fuer Kit-Namen mit Leerzeichen: schon getippte Woerter (ab Index from) werden beruecksichtigt. */
    private List<String> kitTokens(String[] args, int from) {
        String prefix = String.join(" ", java.util.Arrays.copyOfRange(args, from, args.length - 1)).toLowerCase(java.util.Locale.ROOT);
        ArrayList<String> out = new ArrayList<String>();
        for (String kit : this.getConfig().getStringList("kits.available")) {
            String lower = kit.toLowerCase(java.util.Locale.ROOT);
            String rest;
            if (prefix.isEmpty()) {
                rest = kit;
            } else if (lower.startsWith(prefix + " ")) {
                rest = kit.substring(prefix.length() + 1);
            } else {
                continue;
            }
            String token = rest.split(" ")[0];
            if (!out.contains(token)) {
                out.add(token);
            }
        }
        return out;
    }

    private void openKitPreview(Player player, String kitName) {
        this.openKitEditorGui(player, kitName, false);
    }

    private void openKitEditor(Player player, String name) {
        if (!this.isKitAdmin(player)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Dazu hast du keine Berechtigung.");
            return;
        }
        if (!this.getConfig().contains("kits.data." + this.internalKitName(name))) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Dieses Kit gibt es nicht. Nutze zuerst /duelladmin kit create " + name + ".");
            return;
        }
        this.openKitEditorGui(player, name, true);
    }

    private void openKitEditorGui(Player player, String kitName, boolean admin) {
        String kit = this.internalKitName(kitName);
        List<?> stored = this.getConfig().getList("kits.data." + kit + ".items");
        if (stored == null) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Für dieses Kit sind keine Items gespeichert.");
            return;
        }
        player.closeInventory();
        player.setItemOnCursor(new ItemStack(Material.AIR));
        String title = String.valueOf(ChatColor.DARK_RED) + (admin ? "Kit-Editor: " : "Kit Vorschau: ") + kit;
        Inventory inv = this.getServer().createInventory(null, 54, title);
        KitSet set = this.buildKitSet(kit, stored);
        this.arrangeKit(admin ? null : player.getUniqueId(), kit, set);
        ItemStack[] storage = set.storage;
        for (int i = 0; i < storage.length; ++i) {
            if (storage[i] != null) {
                inv.setItem(editorSlot(i), storage[i].clone());
            }
        }
        inv.setItem(0, set.armor[3]);
        inv.setItem(1, set.armor[2]);
        inv.setItem(2, set.armor[1]);
        inv.setItem(3, set.armor[0]);
        inv.setItem(4, set.offhand.getType() == Material.AIR ? null : set.offhand);
        inv.setItem(7, this.item(Material.GRAY_STAINED_GLASS_PANE, "§7 ", "§7Rüstung / Offhand (oben links)"));
        inv.setItem(5, this.item(Material.LIME_STAINED_GLASS_PANE, "§aSpeichern", admin ? "§7Kit und Anordnung speichern" : "§7Deine Anordnung speichern"));
        inv.setItem(6, this.item(Material.YELLOW_STAINED_GLASS_PANE, "§dNeu laden", admin ? "§7Gespeicherten Stand wiederherstellen" : "§7Standard-Anordnung wiederherstellen"));
        inv.setItem(8, this.item(Material.RED_STAINED_GLASS_PANE, "§cZurück", "§7Änderungen verwerfen"));
        for (int slot = 36; slot < 45; ++slot) {
            inv.setItem(slot, this.item(Material.PURPLE_STAINED_GLASS_PANE, "§5▼ Hotbar", "§7Alles darunter ist deine Hotbar"));
        }
        if (admin) {
            this.kitEditors.put(player.getUniqueId(), kit);
        } else {
            this.personalKitEditors.put(player.getUniqueId(), kit);
        }
        player.openInventory(inv);
        player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Kit-Editor geöffnet: oben Inventar, unter der Trennlinie deine Hotbar.");
        player.sendMessage(String.valueOf(ChatColor.GRAY) + (admin ? "Du bist Admin: Items können aus deinem Inventar hinzugefügt und durch Klick außerhalb entfernt werden." : "Du kannst Items nur verschieben, nicht hinzufügen oder entfernen. Speichern: grüner Button."));
    }

    private void handleKitEditorClick(InventoryClickEvent event, Player player, boolean admin) {
        event.setCancelled(true);
        UUID id = player.getUniqueId();
        String kit = admin ? this.kitEditors.get(id) : this.personalKitEditors.get(id);
        if (kit == null || (admin && !this.isKitAdmin(player))) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        int raw = event.getRawSlot();
        if (raw == -999) {
            if (admin) {
                player.setItemOnCursor(new ItemStack(Material.AIR));
            }
            return;
        }
        if (raw < 0) {
            return;
        }
        if (raw >= top.getSize()) {
            if (admin && event.getClickedInventory() == player.getInventory() && !event.isShiftClick()) {
                int playerSlot = event.getSlot();
                ItemStack cursor = event.getCursor();
                ItemStack current = player.getInventory().getItem(playerSlot);
                player.getInventory().setItem(playerSlot, cursor == null || cursor.getType() == Material.AIR ? null : cursor.clone());
                player.setItemOnCursor(current == null ? new ItemStack(Material.AIR) : current.clone());
            }
            return;
        }
        if (raw == 5) {
            if (admin) {
                this.mergeKitCursor(player, top, true);
                this.kitEditors.remove(id);
                this.saveEditedKit(player, kit, top);
            } else {
                this.mergeKitCursor(player, top, false);
                this.savePersonalKitEditor(player, top);
            }
            player.closeInventory();
            return;
        }
        if (raw == 6) {
            if (admin) {
                this.kitEditors.remove(id);
            } else {
                this.personalKitEditors.remove(id);
                this.kitLayouts.clearPersonal(id, kit);
            }
            player.closeInventory();
            this.getServer().getScheduler().runTask((Plugin)this, () -> this.openKitEditorGui(player, kit, admin));
            return;
        }
        if (raw == 8) {
            if (admin) {
                this.kitEditors.remove(id);
            } else {
                this.personalKitEditors.remove(id);
            }
            player.closeInventory();
            player.sendMessage(String.valueOf(ChatColor.LIGHT_PURPLE) + "Änderungen verworfen.");
            return;
        }
        boolean editable = storageIndex(raw) >= 0 || raw == 4 || admin && raw >= 0 && raw <= 3;
        if (!editable || event.isShiftClick() || !event.isLeftClick() && !event.isRightClick()) {
            return;
        }
        ItemStack cursor = event.getCursor();
        if (raw == 4 && !admin && cursor != null && cursor.getType() != Material.AIR && cursor.getType() != Material.SHIELD && cursor.getType() != Material.TOTEM_OF_UNDYING) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "In die Offhand passen nur Schild und Totem.");
            return;
        }
        ItemStack current = top.getItem(raw);
        top.setItem(raw, cursor == null || cursor.getType() == Material.AIR ? null : cursor.clone());
        player.setItemOnCursor(current == null ? new ItemStack(Material.AIR) : current.clone());
    }

    /** Ein Item auf dem Cursor wird beim Schliessen/Speichern in den ersten freien Slot gelegt. */
    private void mergeKitCursor(Player player, Inventory top, boolean admin) {
        ItemStack cursor = player.getItemOnCursor();
        if (cursor == null || cursor.getType() == Material.AIR) {
            return;
        }
        boolean placed = false;
        for (int s = 0; s < 36; ++s) {
            int slot = editorSlot(s);
            if (top.getItem(slot) == null) {
                top.setItem(slot, cursor.clone());
                placed = true;
                break;
            }
        }
        if (!placed && top.getItem(4) == null) {
            top.setItem(4, cursor.clone());
        }
        player.setItemOnCursor(new ItemStack(Material.AIR));
    }

    private ItemStack[] readEditorStorage(Inventory top) {
        ItemStack[] layout = new ItemStack[37];
        for (int s = 0; s < 36; ++s) {
            ItemStack stack = top.getItem(editorSlot(s));
            layout[s] = stack == null || stack.getType() == Material.AIR ? null : stack.clone();
        }
        ItemStack offhand = top.getItem(4);
        layout[36] = offhand == null || offhand.getType() == Material.AIR ? null : offhand.clone();
        return layout;
    }

    /** Admin: speichert Items (neu/entfernt) und die Standard-Anordnung des Kits. */
    private void saveEditedKit(Player player, String kitName, Inventory inventory) {
        if (!this.isKitAdmin(player)) {
            return;
        }
        ItemStack[] layout = this.readEditorStorage(inventory);
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        ItemStack offhand = inventory.getItem(4);
        if (offhand != null && offhand.getType() != Material.AIR) {
            items.add(offhand.clone());
        }
        for (int slot = 0; slot < 4; ++slot) {
            ItemStack piece = inventory.getItem(slot);
            if (piece != null && piece.getType() != Material.AIR) {
                items.add(piece.clone());
            }
        }
        for (int s = 0; s < 36; ++s) {
            if (layout[s] != null) {
                items.add(layout[s].clone());
            }
        }
        List<ItemStack> normalized = this.normalizeStoredKit(kitName, items);
        this.getConfig().set("kits.data." + kitName + ".items", normalized);
        this.saveConfig();
        this.kitLayouts.setStandard(kitName, layout);
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Kit " + kitName + " gespeichert (Items und Anordnung von Hotbar und Inventar).");
    }

    /** Spieler: speichert nur die eigene Anordnung, das Kit selbst bleibt unveraendert. */
    private void savePersonalKitEditor(Player player, Inventory inventory) {
        UUID id = player.getUniqueId();
        String kit = this.personalKitEditors.remove(id);
        if (kit == null || inventory.getSize() < 54) {
            return;
        }
        this.kitLayouts.setPersonal(id, kit, this.readEditorStorage(inventory));
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Deine Anordnung für " + kit + " wurde gespeichert (Hotbar und Inventar).");
    }

    @EventHandler
    public void onKitEditorClose(InventoryCloseEvent event) {
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        String title = event.getView().getTitle();
        UUID id = player.getUniqueId();
        if (title.contains("Kit Vorschau:") && this.personalKitEditors.containsKey(id)) {
            this.mergeKitCursor(player, event.getInventory(), false);
            this.savePersonalKitEditor(player, event.getInventory());
        } else if (title.contains("Kit-Editor:") && this.kitEditors.containsKey(id)) {
            this.mergeKitCursor(player, event.getInventory(), true);
            String kit = this.kitEditors.remove(id);
            this.saveEditedKit(player, kit, event.getInventory());
        }
    }

    @EventHandler
    public void onKitEditorDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (title.contains("Kit-Editor:") || title.contains("Kit Vorschau:")) {
            event.setCancelled(true);
        }
    }

    private ItemStack item(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack item(Material material, String name, String lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(List.of(lore.split("\\n")));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private boolean inKit(Player player, String string) {
        UUID uUID = player.getUniqueId();
        String queued = this.queuedKit(uUID);
        if (!queued.isEmpty() && queueKitName(queued).equalsIgnoreCase(string)) {
            return true;
        }
        return this.activeDuelPlayers.contains(uUID) && string.equalsIgnoreCase(this.duelModes.get(uUID));
    }

    boolean duelLeaveCheck(Player player, String string) {
        UUID uUID = player.getUniqueId();
        UUID uUID2 = null;
        Player player2 = null;
        if (this.activeDuelPlayers.contains(uUID) && DuelFixes.leaveResult(player)) {
            return true;
        }
        if (this.activeDuelPlayers.contains(uUID) && string.equalsIgnoreCase(this.duelModes.get(uUID))) {
            if (this.duelResultPlayers.contains(uUID) || this.pendingDuelLosers.contains(uUID)) {
                player.sendMessage(String.valueOf(ChatColor.GRAY) + "Das Duel ist bereits beendet. Die R\u00fcckkehr zur Lobby l\u00e4uft.");
                return true;
            }
            if (this.duelLeaveTasks.containsKey(uUID)) {
                return true;
            }
            uUID2 = this.duelPartners.get(uUID);
            if (uUID2 != null && (player2 = this.getServer().getPlayer(uUID2)) != null) {
                this.scheduleDuelForfeit(player, player2);
                return true;
            }
            this.finishDuel(player);
            return true;
        }
        return false;
    }

    private static String lobbyLore(int n, int n2, boolean bl) {
        if (bl) {
            return "\u00a77Wartend: ".concat(String.valueOf(n)).concat("\n\u00a77Maximal: ").concat(String.valueOf(n2)).concat("\n\u00a7cLinksklick: Verlassen");
        }
        return "\u00a77Wartend: ".concat(String.valueOf(n)).concat("\n\u00a77Maximal: ").concat(String.valueOf(n2)).concat("\n\u00a7aKlicke zum Beitreten");
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent playerDropItemEvent) {
        Player player = playerDropItemEvent.getPlayer();
        if (player != null && this.profiles.getOrDefault(player.getUniqueId(), "lobby").equals("lobby")) {
            playerDropItemEvent.setCancelled(true);
        }
    }

    private void handlePartyDeath(Player player) {
        Series series = this.duelSeries.get(player.getUniqueId());
        if (series != null && series.party) {
            UUID otherId = player.getUniqueId().equals(series.a) ? series.b : series.a;
            Player other = this.getServer().getPlayer(otherId);
            if (other != null && this.activePartyMatches.containsKey(otherId) && this.seriesRound(player, other)) {
                return;
            }
        }
        DuelFixes.partyDeath(this, player);
    }
}
