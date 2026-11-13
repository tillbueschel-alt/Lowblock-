package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Zusatzfunktionen, die das Hauptplugin nicht veraendern: private Nachrichten (/msg, /r), /ignore,
 * /ping, /report + /reports, Beitritts-/Austrittsmeldungen mit Willkommensgruss und automatische Ankuendigungen.
 * Alles ist in config.yml unter "extras" abschaltbar.
 */
final class Extras implements Listener, CommandExecutor, TabCompleter {
    private static final String PREFIX = "§5§lLowBlock §8» §7";

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<UUID, UUID> lastPartner = new ConcurrentHashMap<UUID, UUID>();
    private final Map<UUID, Long> lastReport = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Set<UUID>> ignoring = new ConcurrentHashMap<UUID, Set<UUID>>();
    private int announceIndex;

    Extras(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "extras.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        List<String> stored = this.data.getStringList("ignore-all");
        for (String line : stored) {
            String[] parts = line.split(":", 2);
            if (parts.length < 2) {
                continue;
            }
            try {
                UUID owner = UUID.fromString(parts[0]);
                Set<UUID> set = ConcurrentHashMap.newKeySet();
                for (String id : parts[1].split(",")) {
                    if (!id.isBlank()) {
                        set.add(UUID.fromString(id.trim()));
                    }
                }
                this.ignoring.put(owner, set);
            }
            catch (IllegalArgumentException ignored) {
            }
        }
        long seconds = Math.max(30, plugin.getConfig().getInt("extras.announcements.interval-seconds", 300));
        if (plugin.getConfig().getBoolean("extras.announcements.enabled", true)) {
            plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, () -> this.announce(), seconds * 20L, seconds * 20L);
        }
    }

    private boolean on(String key) {
        return this.plugin.getConfig().getBoolean("extras." + key, true);
    }

    private void save() {
        List<String> lines = new ArrayList<String>();
        for (Map.Entry<UUID, Set<UUID>> entry : this.ignoring.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            StringBuilder ids = new StringBuilder();
            for (UUID id : entry.getValue()) {
                if (ids.length() > 0) {
                    ids.append(',');
                }
                ids.append(id);
            }
            lines.add(entry.getKey() + ":" + ids);
        }
        this.data.set("ignore-all", lines);
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("extras.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    /* ---------- Ankuendigungen ---------- */

    private void announce() {
        List<String> messages = this.plugin.getConfig().getStringList("extras.announcements.messages");
        if (messages.isEmpty()) {
            messages = List.of(
                    "Fordere jemanden heraus: §d/duel <Spieler> §7– Ranglisten: §d/rangliste",
                    "Spiele mit Freunden: §d/party §7oder das Party-Item in der Lobby.",
                    "Parkour-Bestzeiten findest du unter §d/parkour records",
                    "Cheater gesehen? Melde sie mit §d/report <Spieler> <Grund>");
        }
        String line = messages.get(this.announceIndex++ % messages.size()).replace('&', '§');
        for (Player online : this.plugin.getServer().getOnlinePlayers()) {
            online.sendMessage("");
            online.sendMessage(PREFIX + line);
            online.sendMessage("");
        }
    }

    /* ---------- Join / Quit ---------- */

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        if (!this.on("join-messages")) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            event.setJoinMessage("§8» §d✦ §fWillkommen auf §5§lLowBlock§f, §d" + player.getName() + "§f! §8(neu)");
            player.sendTitle("§d§lWillkommen!", "§7Starte mit §d/duel §7oder dem Navigator", 10, 70, 20);
        } else {
            event.setJoinMessage("§8[§a+§8] §7" + player.getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        this.lastPartner.remove(event.getPlayer().getUniqueId());
        if (this.on("join-messages")) {
            event.setQuitMessage("§8[§c-§8] §7" + event.getPlayer().getName());
        }
    }

    /* ---------- Chat: Ignorierte Spieler ---------- */

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID sender = event.getPlayer().getUniqueId();
        event.getRecipients().removeIf(recipient -> {
            Set<UUID> set = this.ignoring.get(recipient.getUniqueId());
            return set != null && set.contains(sender) && !recipient.getUniqueId().equals(sender);
        });
    }

    private boolean ignores(UUID owner, UUID other) {
        Set<UUID> set = this.ignoring.get(owner);
        return set != null && set.contains(other);
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
            case "msg": {
                if (args.length < 2) {
                    player.sendMessage("§cNutze: /msg <Spieler> <Nachricht>");
                    return true;
                }
                Player target = this.plugin.getServer().getPlayerExact(args[0]);
                this.whisper(player, target, String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
                return true;
            }
            case "r": {
                UUID partner = this.lastPartner.get(player.getUniqueId());
                Player target = partner == null ? null : this.plugin.getServer().getPlayer(partner);
                if (args.length < 1) {
                    player.sendMessage("§cNutze: /r <Nachricht>");
                    return true;
                }
                if (target == null) {
                    player.sendMessage("§cNiemand, dem du antworten könntest.");
                    return true;
                }
                this.whisper(player, target, String.join(" ", args));
                return true;
            }
            case "ignore": {
                this.handleIgnore(player, args);
                return true;
            }
            case "ping": {
                Player target = args.length >= 1 ? this.plugin.getServer().getPlayerExact(args[0]) : player;
                if (target == null) {
                    player.sendMessage("§cSpieler nicht online.");
                    return true;
                }
                int ping = target.getPing();
                String color = ping < 60 ? "§a" : ping < 130 ? "§e" : "§c";
                player.sendMessage(PREFIX + (target.equals(player) ? "Dein Ping: " : target.getName() + ": ") + color + ping + " ms");
                return true;
            }
            case "report": {
                this.handleReport(player, args);
                return true;
            }
            case "reports": {
                this.handleReports(player, args);
                return true;
            }
            default:
                return false;
        }
    }

    private void whisper(Player from, Player to, String message) {
        if (to == null || !to.isOnline()) {
            from.sendMessage("§cSpieler nicht online.");
            return;
        }
        if (to.equals(from)) {
            from.sendMessage("§cDu kannst dir nicht selbst schreiben.");
            return;
        }
        if (this.ignores(from.getUniqueId(), to.getUniqueId())) {
            from.sendMessage("§cDu ignorierst " + to.getName() + " – hebe es mit /ignore " + to.getName() + " auf.");
            return;
        }
        if (this.ignores(to.getUniqueId(), from.getUniqueId())) {
            from.sendMessage("§c" + to.getName() + " möchte keine Nachrichten von dir.");
            return;
        }
        from.sendMessage("§8[§dDu §8→ §f" + to.getName() + "§8] §7" + message);
        to.sendMessage("§8[§f" + from.getName() + " §8→ §dDu§8] §7" + message);
        this.lastPartner.put(from.getUniqueId(), to.getUniqueId());
        this.lastPartner.put(to.getUniqueId(), from.getUniqueId());
        Chat.ping(to);
    }

    private void handleIgnore(Player player, String[] args) {
        UUID id = player.getUniqueId();
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            Set<UUID> set = this.ignoring.get(id);
            if (set == null || set.isEmpty()) {
                player.sendMessage(PREFIX + "Du ignorierst niemanden. §d/ignore <Spieler>");
                return;
            }
            List<String> names = new ArrayList<String>();
            for (UUID other : set) {
                String n = this.plugin.getServer().getOfflinePlayer(other).getName();
                names.add(n == null ? other.toString().substring(0, 8) : n);
            }
            player.sendMessage(PREFIX + "Ignoriert: §f" + String.join("§7, §f", names));
            return;
        }
        Player target = this.plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage("§cSpieler nicht online.");
            return;
        }
        if (target.equals(player)) {
            player.sendMessage("§cDu kannst dich nicht selbst ignorieren.");
            return;
        }
        if (target.hasPermission("duelnetwork.admin")) {
            player.sendMessage("§cTeammitglieder können nicht ignoriert werden.");
            return;
        }
        Set<UUID> set = this.ignoring.computeIfAbsent(id, key -> ConcurrentHashMap.newKeySet());
        if (set.remove(target.getUniqueId())) {
            player.sendMessage(PREFIX + "Du ignorierst §f" + target.getName() + " §7nicht mehr.");
        } else {
            set.add(target.getUniqueId());
            player.sendMessage(PREFIX + "Du ignorierst §f" + target.getName() + " §7jetzt (Chat und Nachrichten).");
        }
        this.save();
    }

    private void handleReport(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cNutze: /report <Spieler> <Grund>");
            return;
        }
        long now = System.currentTimeMillis();
        Long last = this.lastReport.get(player.getUniqueId());
        if (last != null && now - last < 60000L) {
            player.sendMessage("§cBitte warte kurz, bevor du die nächste Meldung sendest.");
            return;
        }
        Player target = this.plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage("§cSpieler nicht online.");
            return;
        }
        if (target.equals(player)) {
            player.sendMessage("§cDu kannst dich nicht selbst melden.");
            return;
        }
        String reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        if (reason.length() > 120) {
            reason = reason.substring(0, 120);
        }
        this.lastReport.put(player.getUniqueId(), now);
        List<String> reports = this.data.getStringList("reports");
        reports.add(new java.text.SimpleDateFormat("dd.MM. HH:mm").format(new java.util.Date()) + "|" + player.getName() + "|" + target.getName() + "|" + reason);
        while (reports.size() > 100) {
            reports.remove(0);
        }
        this.data.set("reports", reports);
        this.save();
        Webhook.send(this.plugin, "report", "📣 Report: " + player.getName() + " meldet " + target.getName() + " – " + reason);
        player.sendMessage(PREFIX + "Danke, §f" + target.getName() + " §7wurde dem Team gemeldet.");
        for (Player online : this.plugin.getServer().getOnlinePlayers()) {
            if (online.hasPermission("duelnetwork.admin")) {
                online.sendMessage("§4§l[Report] §f" + player.getName() + " §7meldet §c" + target.getName() + "§7: §f" + reason);
            }
        }
    }

    private void handleReports(Player player, String[] args) {
        if (!player.hasPermission("duelnetwork.admin")) {
            player.sendMessage("§cDafür brauchst du die Permission duelnetwork.admin.");
            return;
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("clear")) {
            this.data.set("reports", new ArrayList<String>());
            this.save();
            player.sendMessage(PREFIX + "Alle Meldungen gelöscht.");
            return;
        }
        List<String> reports = this.data.getStringList("reports");
        if (reports.isEmpty()) {
            player.sendMessage(PREFIX + "Keine offenen Meldungen.");
            return;
        }
        player.sendMessage(PREFIX + "Letzte Meldungen (" + reports.size() + "):");
        for (int i = Math.max(0, reports.size() - 10); i < reports.size(); i++) {
            String[] parts = reports.get(i).split("\\|", 4);
            if (parts.length == 4) {
                player.sendMessage("§8" + parts[0] + " §f" + parts[1] + " §7→ §c" + parts[2] + "§7: " + parts[3]);
            }
        }
        player.sendMessage("§7Löschen: §d/reports clear");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (args.length == 1 && (name.equals("msg") || name.equals("ignore") || name.equals("ping") || name.equals("report"))) {
            for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)) && !online.getName().equals(sender.getName())) {
                    out.add(online.getName());
                }
            }
        } else if (args.length == 1 && name.equals("reports")) {
            out.add("clear");
        }
        return out;
    }
}
