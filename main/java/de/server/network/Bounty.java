package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

/**
 * Kopfgeld (SMP und Bau-SMP, jeweils mit dem Geld des eigenen Modus):
 * /bounty set <Spieler> <Betrag> - setzt Geld auf einen Spieler (mehrere Spender addieren sich),
 * /bounty [list] - Menue mit den hoechsten Kopfgeldern.
 * Wer den Spieler im selben Modus toetet, bekommt den ganzen Topf. Kein Kopfgeld bei: Duell/Event/Parkour, Selbstmord,
 * Spendern des Kopfgelds, Freunden und demselben Opfer innerhalb von bounty.repeat-cooldown-minutes.
 * Gebuehr (bounty.fee-percent) wird beim Setzen abgezogen; nach bounty.expire-days ohne Erhoehung gibt es das Geld zurueck.
 */
final class Bounty implements CommandExecutor, TabCompleter, Listener {
    private static final String TAG = "Kopfgeld";
    private static final int[] SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<String, Long> recentKills = new HashMap<String, Long>();

    Bounty(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "bounty.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("Bounty", this::expire), 6000L, 12000L);
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("bounty.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private String zone(Player player) {
        return this.plugin.inBauMode(player.getUniqueId()) ? "bau" : "smp";
    }

    private boolean inSmpWorld(Player player) {
        UUID id = player.getUniqueId();
        return "smp".equals(this.plugin.profiles.getOrDefault(id, "lobby")) && !this.plugin.activeDuelPlayers.contains(id)
                && !this.plugin.activePartyMatches.containsKey(id) && !this.plugin.activeParkourMaps.containsKey(id);
    }

    private String path(String zone, UUID target) {
        return "b." + zone + "." + target;
    }

    private long pot(String zone, UUID target) {
        ConfigurationSection section = this.data.getConfigurationSection(this.path(zone, target) + ".pot");
        long total = 0L;
        if (section != null) {
            for (String key : section.getKeys(false)) {
                total += section.getLong(key);
            }
        }
        return total;
    }

    private String name(UUID id) {
        OfflinePlayer off = this.plugin.getServer().getOfflinePlayer(id);
        return off.getName() == null ? id.toString().substring(0, 8) : off.getName();
    }

    /* ---------- Befehle ---------- */

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        if (!this.inSmpWorld(player)) {
            player.sendMessage("§cKopfgeld gibt es nur im SMP und Bau-SMP.");
            return true;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("set")) {
            this.set(player, args);
        } else if (args.length >= 1 && !args[0].equalsIgnoreCase("list")) {
            player.sendMessage("§d/bounty §7(Liste) §8| §d/bounty set <Spieler> <Betrag>");
        } else {
            this.openList(player);
        }
        return true;
    }

    private void set(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("§cNutze: /bounty set <Spieler> <Betrag>");
            return;
        }
        OfflinePlayer target = this.plugin.getServer().getPlayerExact(args[1]);
        if (target == null) {
            target = this.plugin.getServer().getOfflinePlayer(args[1]);
        }
        if (target == null || (!target.isOnline() && !target.hasPlayedBefore())) {
            player.sendMessage("§cSpieler nicht gefunden.");
            return;
        }
        UUID targetId = target.getUniqueId();
        if (targetId.equals(player.getUniqueId())) {
            player.sendMessage("§cAuf dich selbst kannst du kein Kopfgeld setzen.");
            return;
        }
        long amount = Gui.parseAmount(args[2]);
        long min = Math.max(1L, this.plugin.getConfig().getLong("bounty.min", 500L));
        if (amount < min) {
            player.sendMessage("§cMindestbetrag: " + Gui.money(min) + ".");
            return;
        }
        if (!Economy.has(player.getUniqueId(), amount)) {
            player.sendMessage("§cDafür hast du nicht genug Geld.");
            Chat.no(player);
            return;
        }
        if (!Economy.take(player.getUniqueId(), amount)) {
            player.sendMessage("§cZahlung fehlgeschlagen.");
            return;
        }
        int fee = Math.max(0, Math.min(90, this.plugin.getConfig().getInt("bounty.fee-percent", 10)));
        long net = amount - amount * fee / 100L;
        String zone = this.zone(player);
        String base = this.path(zone, targetId);
        String key = base + ".pot." + player.getUniqueId();
        this.data.set(key, this.data.getLong(key, 0L) + net);
        this.data.set(base + ".since", System.currentTimeMillis());
        this.save();
        long total = this.pot(zone, targetId);
        player.sendMessage("§aKopfgeld gesetzt: §f" + Gui.money(net) + " §7(Gebühr " + fee + " %) auf §f" + this.name(targetId) + "§7. Gesamt: §6" + Gui.money(total));
        Chat.coins(player);
        String targetName = this.name(targetId);
        for (Player online : this.plugin.getServer().getOnlinePlayers()) {
            if (this.inSmpWorld(online) && this.zone(online).equals(zone) && Prefs.on(online.getUniqueId(), "bounty")) {
                online.sendMessage("§6✦ Kopfgeld §f" + Gui.money(total) + " §7auf §c" + targetName + " §7ausgesetzt!");
            }
        }
        Webhook.send(this.plugin, "bounty", "Kopfgeld: " + Gui.money(total) + " auf " + targetName + " (" + zone + ")");
    }

    /* ---------- Menue ---------- */

    private void openList(Player player) {
        String zone = this.zone(player);
        List<UUID> targets = new ArrayList<UUID>();
        ConfigurationSection section = this.data.getConfigurationSection("b." + zone);
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    targets.add(UUID.fromString(key));
                }
                catch (IllegalArgumentException ignored) {
                    // kaputter Eintrag
                }
            }
        }
        targets.sort((a, b) -> Long.compare(this.pot(zone, b), this.pot(zone, a)));
        Inventory inv = Gui.frame(this.plugin, TAG, "Kopfgelder" + ("bau".equals(zone) ? " (Bau)" : ""), 54);
        inv.setItem(4, Gui.item(Material.GOLD_INGOT, "§6§lKopfgeld", "§7Töte einen Spieler mit Kopfgeld und", "§7erhalte den ganzen Betrag.", "§7Setzen: §d/bounty set <Spieler> <Betrag>", "§8Mindestbetrag " + Gui.money(Math.max(1L, this.plugin.getConfig().getLong("bounty.min", 500L)))));
        for (int i = 0; i < targets.size() && i < SLOTS.length; i++) {
            UUID id = targets.get(i);
            OfflinePlayer off = this.plugin.getServer().getOfflinePlayer(id);
            ConfigurationSection pot = this.data.getConfigurationSection(this.path(zone, id) + ".pot");
            ItemStack head = Gui.item(Material.PLAYER_HEAD, "§c§l" + this.name(id), "§7Kopfgeld: §6" + Gui.money(this.pot(zone, id)),
                    "§7Spender: §f" + (pot == null ? 0 : pot.getKeys(false).size()), off.isOnline() ? "§aOnline" : "§8Offline");
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(off);
                head.setItemMeta(meta);
            }
            inv.setItem(SLOTS[i], head);
        }
        if (targets.isEmpty()) {
            inv.setItem(22, Gui.item(Material.BARRIER, "§7Aktuell kein Kopfgeld", "§8Setze das erste mit §f/bounty set"));
        }
        player.openInventory(inv);
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

    /* ---------- Auszahlung ---------- */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim) || !this.inSmpWorld(victim) || !this.inSmpWorld(killer)) {
            return;
        }
        String zone = this.zone(victim);
        if (!zone.equals(this.zone(killer))) {
            return;
        }
        long total = this.pot(zone, victim.getUniqueId());
        if (total <= 0L) {
            return;
        }
        String base = this.path(zone, victim.getUniqueId());
        if (this.data.contains(base + ".pot." + killer.getUniqueId())) {
            killer.sendMessage("§7Du hast selbst Kopfgeld auf §f" + victim.getName() + " §7gesetzt – keine Auszahlung.");
            return;
        }
        if (Friends.instance != null && Friends.instance.areFriends(killer.getUniqueId(), victim.getUniqueId())) {
            killer.sendMessage("§7Freunde bekommen kein Kopfgeld voneinander.");
            return;
        }
        String pair = killer.getUniqueId() + ">" + victim.getUniqueId();
        long now = System.currentTimeMillis();
        long cooldown = Math.max(1L, this.plugin.getConfig().getLong("bounty.repeat-cooldown-minutes", 30L)) * 60000L;
        Long last = this.recentKills.get(pair);
        this.recentKills.put(pair, now);
        if (this.recentKills.size() > 500) {
            this.recentKills.values().removeIf(t -> now - t > cooldown);
        }
        if (last != null && now - last < cooldown) {
            killer.sendMessage("§7Du hast §f" + victim.getName() + " §7erst kürzlich getötet – keine Auszahlung.");
            return;
        }
        this.data.set(base, null);
        this.save();
        Economy.add(killer.getUniqueId(), killer.getName(), total, "bau".equals(zone));
        for (Player online : this.plugin.getServer().getOnlinePlayers()) {
            if (this.inSmpWorld(online) && this.zone(online).equals(zone) && Prefs.on(online.getUniqueId(), "bounty")) {
                online.sendMessage("§6✦ §f" + killer.getName() + " §7hat das Kopfgeld auf §c" + victim.getName() + " §7kassiert: §6" + Gui.money(total));
            }
        }
        Chat.coins(killer);
        Webhook.send(this.plugin, "bounty", "Kopfgeld kassiert: " + killer.getName() + " -> " + victim.getName() + " (" + Gui.money(total) + ")");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            long total = this.pot("smp", player.getUniqueId()) + this.pot("bau", player.getUniqueId());
            if (total > 0L) {
                player.sendMessage("§6✦ §7Auf dich ist ein Kopfgeld von §6" + Gui.money(total) + " §7ausgesetzt. Pass auf!");
            }
        }, 120L);
    }

    /* ---------- Ablauf: Geld zurueck ---------- */

    private void expire() {
        long limit = Math.max(1L, this.plugin.getConfig().getLong("bounty.expire-days", 7L)) * 86400000L;
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (String zone : new String[]{"smp", "bau"}) {
            ConfigurationSection section = this.data.getConfigurationSection("b." + zone);
            if (section == null) {
                continue;
            }
            for (String key : new ArrayList<String>(section.getKeys(false))) {
                String base = "b." + zone + "." + key;
                if (now - this.data.getLong(base + ".since", now) < limit) {
                    continue;
                }
                ConfigurationSection pot = this.data.getConfigurationSection(base + ".pot");
                if (pot != null) {
                    for (String setter : pot.getKeys(false)) {
                        try {
                            UUID id = UUID.fromString(setter);
                            Economy.add(id, this.name(id), pot.getLong(setter), "bau".equals(zone));
                        }
                        catch (IllegalArgumentException ignored) {
                            // kaputter Eintrag
                        }
                    }
                }
                this.data.set(base, null);
                changed = true;
            }
        }
        if (changed) {
            this.save();
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1 && "set".startsWith(args[0].toLowerCase(Locale.ROOT))) {
            out.add("set");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(online.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            out.addAll(List.of("500", "1k", "5k", "10k"));
        }
        return out;
    }
}
