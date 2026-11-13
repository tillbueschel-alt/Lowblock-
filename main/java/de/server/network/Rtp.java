package de.server.network;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * /rtp - zufaellige Position in Overworld, Nether und End (im SMP). Nether und End sind erst nach
 * /duelladmin unlock sichtbar und betretbar (auch Portale sind bis dahin gesperrt).
 */
final class Rtp implements Listener, CommandExecutor, TabCompleter {
    private static final String TAG = "Wilde Welt";
    private static final List<String> DIMENSIONS = List.of("overworld", "nether", "end");

    private final DuelNetworkPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> cooldown = new HashMap<UUID, Long>();
    private final Set<UUID> pending = new HashSet<UUID>();
    private final Map<String, Long> previousRemaining = new HashMap<String, Long>();
    private static final long[] ANNOUNCEMENT_MILESTONES = {12L * 3600000L, 6L * 3600000L, 3L * 3600000L, 3600000L,
            30L * 60000L, 10L * 60000L, 5L * 60000L, 60000L, 30000L, 10000L};

    Rtp(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        for (String dim : List.of("nether", "end")) {
            long unlockAt = plugin.getConfig().getLong("rtp.unlock-at." + dim, 0L);
            this.previousRemaining.put(dim, unlockAt > 0L ? Math.max(0L, unlockAt - System.currentTimeMillis()) : -1L);
        }
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, this::tickUnlockSchedules, 20L, 20L);
    }

    /* ---------- Welten ---------- */

    private World world(String dim) {
        if (dim.equals("overworld")) {
            Location smp = this.plugin.readConfiguredLocation("locations.spawns.smp.default");
            if (smp != null && smp.getWorld() != null) {
                return smp.getWorld();
            }
        }
        World.Environment wanted = dim.equals("nether") ? World.Environment.NETHER : dim.equals("end") ? World.Environment.THE_END : World.Environment.NORMAL;
        for (World world : this.plugin.getServer().getWorlds()) {
            if (world.getEnvironment() == wanted) {
                return world;
            }
        }
        return null;
    }

    boolean unlocked(String dim) {
        return dim.equals("overworld") || (this.plugin.getConfig().getBoolean("rtp.unlocked." + dim, false)
                && (!dim.equals("end") || this.enderDragonDefeated()));
    }

    private boolean normalSmp(Player player) {
        return "smp".equals(this.plugin.profiles.getOrDefault(player.getUniqueId(), "lobby"))
                && !this.plugin.inBauMode(player.getUniqueId());
    }

    private boolean enderDragonDefeated() {
        World end = this.world("end");
        if (end == null) return false;
        try {
            return end.getEnderDragonBattle() != null && end.getEnderDragonBattle().hasBeenPreviouslyKilled();
        } catch (RuntimeException ex) {
            Store.problem("RTP:Enderdrachenstatus", ex);
            return false;
        }
    }

    private void broadcastSmp(String message) {
        for (Player online : this.plugin.getServer().getOnlinePlayers()) {
            if (this.normalSmp(online)) online.sendMessage(message);
        }
    }

    private static String remaining(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long days = seconds / 86400L;
        long hours = seconds % 86400L / 3600L;
        long minutes = seconds % 3600L / 60L;
        long secs = seconds % 60L;
        if (days > 0L) return days + " Tag" + (days == 1L ? "" : "e") + " " + hours + " Std " + minutes + " Min";
        if (hours > 0L) return hours + " Std " + minutes + " Min";
        if (minutes > 0L) return minutes + " Min " + secs + " Sek";
        return secs + " Sek";
    }

    private void tickUnlockSchedules() {
        long now = System.currentTimeMillis();
        for (String dim : List.of("nether", "end")) {
            long unlockAt = this.plugin.getConfig().getLong("rtp.unlock-at." + dim, 0L);
            if (unlockAt <= 0L) {
                this.previousRemaining.put(dim, -1L);
                continue;
            }
            long left = unlockAt - now;
            long previous = this.previousRemaining.getOrDefault(dim, left);
            if (left > 0L) {
                for (long milestone : ANNOUNCEMENT_MILESTONES) {
                    if (previous > milestone && left <= milestone) {
                        this.broadcastSmp("§d✦ §f" + label(dim) + " §dwird in §e" + remaining(left) + " §dfreigeschaltet. (Nur normales SMP)");
                        break;
                    }
                }
                this.previousRemaining.put(dim, left);
                continue;
            }
            this.plugin.getConfig().set("rtp.unlock-at." + dim, null);
            this.plugin.getConfig().set("rtp.unlocked." + dim, true);
            this.plugin.saveConfig();
            this.previousRemaining.put(dim, -1L);
            if (dim.equals("end") && !this.enderDragonDefeated()) {
                this.plugin.getConfig().set("rtp.end-countdown-finished", true);
                this.plugin.saveConfig();
                this.broadcastSmp("§d✦ §fEnd: §7Der Countdown ist abgelaufen. Es bleibt bis zum Sieg über den Enderdrachen gesperrt.");
            } else if (!dim.equals("end")) {
                this.broadcastSmp("§d✦ §f" + label(dim) + " §dwurde freigeschaltet! §7Nutze §d/rtp §7oder die Portale. (Nur normales SMP)");
            }
        }
        if (this.plugin.getConfig().getBoolean("rtp.unlocked.end", false) && this.enderDragonDefeated()
                && !this.plugin.getConfig().getBoolean("rtp.end-open-announced", false)) {
            this.plugin.getConfig().set("rtp.end-countdown-finished", false);
            this.plugin.getConfig().set("rtp.end-open-announced", true);
            this.plugin.saveConfig();
            this.broadcastSmp("§d✦ §fDer Enderdrache ist besiegt – das End ist jetzt offen! §7RTP und Portale sind im normalen SMP verfügbar.");
        }
    }

    private boolean lockedEnvironment(World.Environment environment) {
        return environment == World.Environment.NETHER && !this.unlocked("nether")
                || environment == World.Environment.THE_END && !this.unlocked("end");
    }

    private boolean available(String dim) {
        return this.unlocked(dim) && this.world(dim) != null;
    }

    private static String label(String dim) {
        return switch (dim) {
            case "nether" -> "Nether";
            case "end" -> "End";
            default -> "Overworld";
        };
    }

    /* ---------- Befehl & Menue ---------- */

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Befehl ist nur im Spiel verfügbar.");
            return true;
        }
        if (args.length >= 1) {
            String dim = args[0].toLowerCase(Locale.ROOT);
            if (dim.equals("end") || dim.equals("nether") || dim.equals("overworld")) {
                if (!this.available(dim)) {
                    player.sendMessage("§cDiese Welt ist nicht verfügbar.");
                    return true;
                }
                this.start(player, dim);
                return true;
            }
        }
        this.openMenu(player);
        return true;
    }

    private void openMenu(Player player) {
        List<String> dims = new ArrayList<String>();
        for (String dim : DIMENSIONS) {
            if (this.available(dim)) {
                dims.add(dim);
            }
        }
        Inventory inv = Gui.frame(this.plugin, TAG, "Zufällig teleportieren", 27);
        int[] slots = dims.size() == 1 ? new int[]{13} : dims.size() == 2 ? new int[]{11, 15} : new int[]{11, 13, 15};
        for (int i = 0; i < dims.size(); i++) {
            String dim = dims.get(i);
            Material icon = dim.equals("nether") ? Material.NETHERRACK : dim.equals("end") ? Material.END_STONE : Material.GRASS_BLOCK;
            String color = dim.equals("nether") ? "§c" : dim.equals("end") ? "§e" : "§a";
            String desc = dim.equals("nether") ? "§7Heiße Höhlen und Festungen" : dim.equals("end") ? "§7Äußere Inseln mit Städten" : "§7Die große SMP-Welt";
            inv.setItem(slots[i], Gui.item(icon, color + "§l" + label(dim), desc, "", "§e▶ Klicken zum Teleportieren", "§8Wartezeit 3 Sekunden – nicht bewegen"));
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!Gui.isTitle(title, TAG) || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || event.getRawSlot() >= 27) {
            return;
        }
        String dim = switch (clicked.getType()) {
            case NETHERRACK -> "nether";
            case END_STONE -> "end";
            case GRASS_BLOCK -> "overworld";
            default -> null;
        };
        if (dim == null || !this.available(dim)) {
            return;
        }
        player.closeInventory();
        this.start(player, dim);
    }

    /* ---------- Teleport ---------- */

    private void start(Player player, String dim) {
        UUID id = player.getUniqueId();
        if (!this.normalSmp(player)) {
            player.sendMessage("§c/rtp funktioniert nur im normalen SMP, nicht in Lobby, Event, Duel oder Bau-SMP.");
            return;
        }
        if (this.lockedEnvironment(player.getWorld().getEnvironment())) {
            String dimension = player.getWorld().getEnvironment() == World.Environment.NETHER ? "Nether" : "End";
            player.sendMessage("§c/rtp ist im gesperrten " + dimension + " nicht verfügbar.");
            return;
        }
        if (this.plugin.isInSmpCombat(player)) {
            player.sendMessage("§cWegen des Combats kannst du dich nicht teleportieren.");
            return;
        }
        if (this.pending.contains(id)) {
            player.sendMessage("§cDu wirst bereits teleportiert.");
            return;
        }
        long wait = Math.max(0L, this.plugin.getConfig().getLong("rtp.cooldown-seconds", 30L)) * 1000L;
        Long last = this.cooldown.get(id);
        if (!player.hasPermission("duelnetwork.admin") && last != null && System.currentTimeMillis() - last < wait) {
            player.sendMessage("§cBitte warte noch " + ((wait - (System.currentTimeMillis() - last)) / 1000L + 1L) + " Sekunden.");
            return;
        }
        this.pending.add(id);
        this.step(player, dim, player.getLocation().clone(), 3);
    }

    private void step(Player player, String dim, Location origin, int left) {
        UUID id = player.getUniqueId();
        if (!player.isOnline()) {
            this.pending.remove(id);
            return;
        }
        Location now = player.getLocation();
        if (now.getWorld() != origin.getWorld() || now.distance(origin) > 1.5) {
            this.pending.remove(id);
            player.sendTitle("§cAbgebrochen", "§7Du hast dich bewegt", 0, 30, 10);
            Chat.no(player);
            return;
        }
        if (left <= 0) {
            this.pending.remove(id);
            this.finish(player, dim);
            return;
        }
        player.sendTitle("§d" + left, "§7Wilde Welt: §f" + label(dim), 0, 22, 0);
        player.playSound(now, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f + (3 - left) * 0.25f);
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> this.step(player, dim, origin, left - 1), 20L);
    }

    private void finish(Player player, String dim) {
        World world = this.world(dim);
        if (world == null) {
            player.sendMessage("§cDiese Welt ist nicht verfügbar.");
            return;
        }
        UUID id = player.getUniqueId();
        this.pending.add(id);
        player.sendMessage("§7Suche einen sicheren Ort ...");
        this.findAsync(world, dim, 40, target -> {
            this.pending.remove(id);
            if (!player.isOnline()) {
                return;
            }
            if (!this.normalSmp(player) || !this.available(dim)) {
                player.sendMessage("§cDer Teleport wurde abgebrochen: Diese Dimension ist nicht mehr verfügbar.");
                return;
            }
            if (target == null) {
                player.sendMessage("§cEs wurde kein sicherer Ort gefunden. Versuche es noch einmal.");
                return;
            }
            this.plugin.saveLastSmpLocation(player);
            target.setYaw(player.getLocation().getYaw());
            Chat.warp(player);
            player.teleport(target);
            Chat.warp(player);
            this.cooldown.put(id, System.currentTimeMillis());
            player.playSound(target, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.sendTitle("§a§lTeleportiert", "§7" + label(dim) + " §8• §f" + target.getBlockX() + ", " + target.getBlockZ(), 5, 40, 10);
        });
    }

    private static boolean unsafe(Material material) {
        String n = material.name();
        return !material.isSolid() || n.equals("MAGMA_BLOCK") || n.equals("CACTUS") || n.contains("CAMPFIRE") || n.equals("SWEET_BERRY_BUSH")
                || n.equals("POWDER_SNOW") || n.contains("FIRE") || n.equals("BEDROCK") || n.equals("LAVA") || n.equals("WATER");
    }

    /**
     * Sucht einen sicheren Ort, ohne den Server zu blockieren: pro Versuch wird der Ziel-Chunk
     * asynchron geladen (Paper), erst danach wird im Haupt-Thread kurz geprueft.
     */
    private void findAsync(World world, String dim, int attemptsLeft, java.util.function.Consumer<Location> done) {
        if (attemptsLeft <= 0) {
            done.accept(null);
            return;
        }
        double cx = 0.0;
        double cz = 0.0;
        int radius;
        Location smp = this.plugin.readConfiguredLocation("locations.spawns.smp.default");
        double border = Math.max(100.0, this.plugin.borderRadius(world.getEnvironment()));
        if (dim.equals("overworld")) {
            radius = (int)Math.max(50.0, border - 100.0);
            if (smp != null && smp.getWorld() != null && smp.getWorld().equals((Object)world)) {
                cx = smp.getX();
                cz = smp.getZ();
            }
        } else if (dim.equals("nether")) {
            radius = (int)Math.min(border - 100.0, 6000.0);
            if (smp != null) {
                cx = this.plugin.borderCenterX(World.Environment.NETHER, smp);
                cz = this.plugin.borderCenterZ(World.Environment.NETHER, smp);
            }
        } else {
            radius = 0;
        }
        int x;
        int z;
        if (dim.equals("end")) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double dist = 1200.0 + this.random.nextDouble() * Math.max(500.0, Math.min(border - 1400.0, 6000.0));
            x = (int)(Math.cos(angle) * dist);
            z = (int)(Math.sin(angle) * dist);
        } else {
            x = (int)cx + this.random.nextInt(radius * 2 + 1) - radius;
            z = (int)cz + this.random.nextInt(radius * 2 + 1) - radius;
        }
        final int fx = x;
        final int fz = z;
        world.getChunkAtAsync(fx >> 4, fz >> 4).whenComplete((chunk, error) ->
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
                Location found = error == null ? this.check(world, dim, fx, fz) : null;
                if (found != null) {
                    done.accept(found);
                } else {
                    this.findAsync(world, dim, attemptsLeft - 1, done);
                }
            }));
    }

    /** Prueft eine Position in einem bereits geladenen Chunk (billig, nur Haupt-Thread). */
    private Location check(World world, String dim, int x, int z) {
        if (dim.equals("nether")) {
            for (int y = 120; y >= 33; y--) {
                Block ground = world.getBlockAt(x, y, z);
                if (unsafe(ground.getType())) {
                    continue;
                }
                if (world.getBlockAt(x, y + 1, z).getType().isAir() && world.getBlockAt(x, y + 2, z).getType().isAir()) {
                    return new Location(world, x + 0.5, y + 1.0, z + 0.5);
                }
            }
            return null;
        }
        int y = world.getHighestBlockYAt(x, z);
        Block ground = world.getBlockAt(x, y, z);
        if (unsafe(ground.getType()) || y <= world.getMinHeight() + 1) {
            return null;
        }
        if (world.getBlockAt(x, y + 1, z).getType().isAir() && world.getBlockAt(x, y + 2, z).getType().isAir()) {
            return new Location(world, x + 0.5, y + 1.0, z + 0.5);
        }
        return null;
    }

    /* ---------- Portale sperren ---------- */

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || to.getWorld() == null || from == null || from.getWorld() == null) {
            return;
        }
        World.Environment env = to.getWorld().getEnvironment();
        Player player = event.getPlayer();
        World.Environment fromEnvironment = from.getWorld().getEnvironment();
        boolean crossingDimension = fromEnvironment == World.Environment.NETHER || fromEnvironment == World.Environment.THE_END
                || env == World.Environment.NETHER || env == World.Environment.THE_END;
        if (crossingDimension && !this.normalSmp(player)) {
            event.setCancelled(true);
            player.sendMessage("§cNether und End sind ausschließlich im normalen SMP verfügbar.");
            return;
        }
        if (this.lockedEnvironment(env)) {
            event.setCancelled(true);
            String blocked = label(env == World.Environment.NETHER ? "nether" : "end");
            if (blocked.equals("End") && !this.enderDragonDefeated()) {
                player.sendMessage("§cDas End bleibt gesperrt, bis der Enderdrache besiegt wurde.");
            } else {
                player.sendMessage("§cPortale in den " + blocked + " sind noch nicht freigeschaltet. Zurückreisen bleibt möglich.");
            }
            return;
        }
        // Nether -> Overworld: Ziel liegt wegen 1:8 oft weit ausserhalb der SMP-Zone, dann wird es an den Rand gesetzt
        Location smp = this.plugin.readConfiguredLocation("locations.spawns.smp.default");
        if (env == World.Environment.NORMAL && from != null && from.getWorld() != null && from.getWorld().getEnvironment() == World.Environment.NETHER
                && smp != null && smp.getWorld() != null && smp.getWorld().equals((Object)to.getWorld())) {
            double limit = Math.max(100.0, this.plugin.borderRadius(World.Environment.NORMAL) - 300.0);
            double x = Math.max(smp.getX() - limit, Math.min(smp.getX() + limit, to.getX()));
            double z = Math.max(smp.getZ() - limit, Math.min(smp.getZ() + limit, to.getZ()));
            if (x != to.getX() || z != to.getZ()) {
                event.setTo(new Location(to.getWorld(), x, to.getY(), z, to.getYaw(), to.getPitch()));
                player.sendMessage("§7Dein Portal liegt am Rand der SMP-Zone.");
            }
        }
    }

    /* ---------- Admin: /duelladmin unlock|lock <nether|end> ---------- */

    private static boolean validDimension(String dim) {
        return "nether".equalsIgnoreCase(dim) || "end".equalsIgnoreCase(dim);
    }

    private static long parseDelay(String raw) {
        Matcher matcher = Pattern.compile("(?i)^(\\d+)(s|m|h|d)$").matcher(raw);
        if (!matcher.matches()) throw new IllegalArgumentException("Zeitformat: z.B. 24h, 1d, 30m.");
        long amount = Long.parseLong(matcher.group(1));
        if (amount < 1L) throw new IllegalArgumentException("Die Dauer muss mindestens 1 betragen.");
        long multiplier = switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
            case "s" -> 1000L;
            case "m" -> 60000L;
            case "h" -> 3600000L;
            default -> 86400000L;
        };
        long millis = Math.multiplyExact(amount, multiplier);
        if (millis > 30L * 86400000L) throw new IllegalArgumentException("Der Countdown darf höchstens 30 Tage dauern.");
        return millis;
    }

    private static void schedule(DuelNetworkPlugin plugin, Player admin, Rtp rtp, String dim, String rawDelay) {
        long delay;
        try {
            delay = rawDelay == null ? 86400000L : parseDelay(rawDelay);
        } catch (RuntimeException ex) {
            admin.sendMessage("§c" + ex.getMessage());
            return;
        }
        long at = System.currentTimeMillis() + delay;
        plugin.getConfig().set("rtp.unlocked." + dim, false);
        plugin.getConfig().set("rtp.unlock-at." + dim, at);
        if (dim.equals("end")) {
            plugin.getConfig().set("rtp.end-open-announced", false);
            plugin.getConfig().set("rtp.end-countdown-finished", false);
        }
        plugin.saveConfig();
        rtp.previousRemaining.put(dim, delay);
        admin.sendMessage("§a" + label(dim) + "-Freischaltung geplant: in " + remaining(delay) + ".");
        rtp.broadcastSmp("§d✦ §f" + label(dim) + " §dwird in §e" + remaining(delay) + " §dfreigeschaltet. (Nur normales SMP)");
        if (rtp.world(dim) == null) admin.sendMessage("§eHinweis: Es gibt derzeit keine " + label(dim) + "-Welt auf dem Server.");
    }

    static void admin(DuelNetworkPlugin plugin, Player admin, String[] args, Rtp rtp) {
        String action = args[0].toLowerCase(Locale.ROOT);
        if (args.length >= 2 && args[1].equalsIgnoreCase("status")) {
            for (String dim : List.of("nether", "end")) {
                long at = plugin.getConfig().getLong("rtp.unlock-at." + dim, 0L);
                String status = at > 0L ? "Countdown: noch " + remaining(at - System.currentTimeMillis())
                        : (rtp.unlocked(dim) ? "offen" : "gesperrt");
                admin.sendMessage("§7" + label(dim) + ": §f" + status);
            }
            if (!rtp.enderDragonDefeated()) admin.sendMessage("§7End-Bedingung: §cEnderdrache noch nicht besiegt.");
            admin.sendMessage("§7Zeitplan: §f/duelladmin unlock start <nether|end> [24h]");
            return;
        }
        if (action.equals("unlock") && args.length >= 3 && args[1].equalsIgnoreCase("start")) {
            if (!validDimension(args[2])) {
                admin.sendMessage("§d/duelladmin unlock start <nether|end> [24h]");
                return;
            }
            schedule(plugin, admin, rtp, args[2].toLowerCase(Locale.ROOT), args.length >= 4 ? args[3] : null);
            return;
        }
        if (args.length >= 3 && validDimension(args[1]) && args[2].equalsIgnoreCase("start")) {
            if (!action.equals("unlock")) {
                admin.sendMessage("§cEin Countdown kann nur mit /duelladmin unlock gestartet werden.");
                return;
            }
            schedule(plugin, admin, rtp, args[1].toLowerCase(Locale.ROOT), args.length >= 4 ? args[3] : null);
            return;
        }
        if (args.length < 2 || !validDimension(args[1])) {
            admin.sendMessage("§d/duelladmin unlock <nether|end> §7(sofort)");
            admin.sendMessage("§d/duelladmin unlock start <nether|end> [24h]");
            admin.sendMessage("§d/duelladmin lock <nether|end> §8| §d/duelladmin unlock status");
            for (String dim : List.of("nether", "end")) {
                long at = plugin.getConfig().getLong("rtp.unlock-at." + dim, 0L);
                admin.sendMessage("§7Status " + label(dim) + ": " + (at > 0L ? "§eCountdown " + remaining(at - System.currentTimeMillis()) : rtp.unlocked(dim) ? "§aoffen" : "§cgesperrt"));
            }
            return;
        }
        String dim = args[1].toLowerCase(Locale.ROOT);
        boolean unlock = action.equals("unlock");
        if (unlock) {
            plugin.getConfig().set("rtp.unlocked." + dim, true);
            plugin.getConfig().set("rtp.unlock-at." + dim, null);
            if (dim.equals("end")) {
                plugin.getConfig().set("rtp.end-open-announced", false);
                plugin.getConfig().set("rtp.end-countdown-finished", false);
            }
            rtp.previousRemaining.put(dim, -1L);
        } else {
            plugin.getConfig().set("rtp.unlocked." + dim, false);
            plugin.getConfig().set("rtp.unlock-at." + dim, null);
            if (dim.equals("end")) {
                plugin.getConfig().set("rtp.end-open-announced", false);
                plugin.getConfig().set("rtp.end-countdown-finished", false);
            }
            rtp.previousRemaining.put(dim, -1L);
        }
        plugin.saveConfig();
        admin.sendMessage(ChatColor.GREEN + label(dim) + (unlock ? " freigeschaltet." : " gesperrt."));
        if (unlock && dim.equals("end") && !rtp.enderDragonDefeated()) {
            admin.sendMessage("§eDas End bleibt bis zum Sieg über den Enderdrachen gesperrt (auch RTP).");
        }
        if (unlock && dim.equals("nether")) {
            rtp.broadcastSmp("§d✦ §f" + label(dim) + " §dwurde freigeschaltet! §7Nutze §d/rtp §7oder die Portale. (Nur normales SMP)");
        }
        if (rtp.world(dim) == null) admin.sendMessage("§eHinweis: Es gibt keine " + label(dim) + "-Welt auf dem Server.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            for (String dim : DIMENSIONS) {
                if (this.available(dim)) {
                    out.add(dim);
                }
            }
        }
        return out;
    }
}
