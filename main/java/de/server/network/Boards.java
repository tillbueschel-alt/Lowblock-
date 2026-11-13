package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.Plugin;

/**
 * Hologramm-Ranglisten (Text-Displays, ganz ohne Texture-Pack): Duell-Elo, Siege, Kills, Spielzeit,
 * Geld (SMP und Bau), Gems und Parkour-Rekorde. Aufstellen mit /board add &lt;Typ&gt; an der aktuellen Position.
 * Die Listen aktualisieren sich selbst (boards.refresh-seconds, Standard 60) und schreiben nur bei Aenderungen.
 */
final class Boards implements CommandExecutor, TabCompleter, Listener {
    static Boards instance;

    private static final List<String> TYPES = List.of("elo", "wins", "streak", "kills", "playtime", "money", "bau-money", "gems", "parkour");

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<String, UUID> entities = new LinkedHashMap<String, UUID>();
    private final Map<String, String> lastText = new LinkedHashMap<String, String>();

    Boards(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.file = new File(plugin.getDataFolder(), "boards.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        long ticks = Math.max(10L, plugin.getConfig().getLong("boards.refresh-seconds", 60L)) * 20L;
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("Boards", this::refreshAll), 100L, ticks);
    }

    void shutdown() {
        for (UUID id : this.entities.values()) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
        this.entities.clear();
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("boards.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    /* ---------- Inhalt ---------- */

    private static String name(UUID id) {
        String name = Economy.nameOf(id);
        if (name == null || name.equals("?")) {
            name = Bukkit.getOfflinePlayer(id).getName();
        }
        return name == null ? "?" : name;
    }

    private static String medal(int place) {
        return switch (place) {
            case 1 -> "§6§l#1";
            case 2 -> "§f§l#2";
            case 3 -> "§c§l#3";
            default -> "§7#" + place;
        };
    }

    private String title(String type, String map) {
        return switch (type) {
            case "elo" -> "§d§l⚔ Duell-Elo";
            case "wins" -> "§d§l⚔ Duell-Siege";
            case "streak" -> "§d§l⚔ Beste Siegesserie";
            case "kills" -> "§c§l☠ SMP-Kills";
            case "playtime" -> "§b§l⌚ Spielzeit";
            case "money" -> "§6§l$ Reichste Spieler (SMP)";
            case "bau-money" -> "§6§l$ Reichste Spieler (Bau-SMP)";
            case "gems" -> "§d§l◆ Meiste Gems";
            case "parkour" -> "§d§l⚑ Parkour: §f" + map;
            default -> type;
        };
    }

    /** Zeilen {Name, Wert} der Rangliste (max. 10). */
    private List<String[]> rows(String type, String map) {
        List<String[]> rows = new ArrayList<String[]>();
        switch (type) {
            case "elo", "wins" -> rows.addAll(DuelStats.top(type, 10));
            case "streak" -> rows.addAll(DuelStats.top("best-streak", 10));
            case "kills" -> {
                for (Map.Entry<UUID, Long> e : SmpTop.topKills(10)) {
                    rows.add(new String[]{name(e.getKey()), String.valueOf(e.getValue())});
                }
            }
            case "playtime" -> {
                for (Map.Entry<UUID, Long> e : SmpTop.topMinutes(10)) {
                    rows.add(new String[]{name(e.getKey()), SmpTop.time(e.getValue())});
                }
            }
            case "money" -> {
                List<UUID> order = Economy.order();
                for (int i = 0; i < order.size() && rows.size() < 10; i++) {
                    rows.add(new String[]{name(order.get(i)), Gui.shortMoney(Economy.balanceIn(order.get(i), false))});
                }
            }
            case "bau-money" -> {
                List<UUID> ids = new ArrayList<UUID>(Economy.order());
                ids.removeIf(id -> Economy.balanceIn(id, true) <= 0L);
                ids.sort((a, b) -> Long.compare(Economy.balanceIn(b, true), Economy.balanceIn(a, true)));
                for (int i = 0; i < ids.size() && i < 10; i++) {
                    rows.add(new String[]{name(ids.get(i)), Gui.shortMoney(Economy.balanceIn(ids.get(i), true))});
                }
            }
            case "gems" -> {
                for (Map.Entry<UUID, Integer> e : Gems.top(10)) {
                    rows.add(new String[]{name(e.getKey()), String.valueOf(e.getValue())});
                }
            }
            case "parkour" -> {
                if (this.plugin.records != null) {
                    List<RecordStore.Entry> top = this.plugin.records.top(map);
                    for (int i = 0; i < top.size() && i < 10; i++) {
                        rows.add(new String[]{top.get(i).name(), ParkourEnhancements.formatTime(top.get(i).timeMs())});
                    }
                }
            }
            default -> {
            }
        }
        return rows;
    }

    private String render(String type, String map) {
        StringBuilder sb = new StringBuilder(this.title(type, map)).append("\n§8§m                    ");
        List<String[]> rows = this.rows(type, map);
        if (rows.isEmpty()) {
            sb.append("\n§7Noch keine Einträge");
        }
        for (int i = 0; i < rows.size(); i++) {
            sb.append("\n").append(medal(i + 1)).append(" §f").append(rows.get(i)[0]).append(" §8» §a").append(rows.get(i)[1]);
        }
        return sb.toString();
    }

    /* ---------- Entities ---------- */

    private Location where(String id) {
        String base = "boards." + id;
        World world = Bukkit.getWorld(this.data.getString(base + ".world", ""));
        if (world == null) {
            return null;
        }
        return new Location(world, this.data.getDouble(base + ".x"), this.data.getDouble(base + ".y"), this.data.getDouble(base + ".z"));
    }

    private void refresh(String id) {
        Location loc = this.where(id);
        if (loc == null || !loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            return;
        }
        String text = this.render(this.data.getString("boards." + id + ".type", ""), this.data.getString("boards." + id + ".map", ""));
        UUID known = this.entities.get(id);
        Entity entity = known == null ? null : Bukkit.getEntity(known);
        if (entity instanceof TextDisplay display && display.isValid()) {
            if (!text.equals(this.lastText.get(id))) {
                display.setText(text);
                this.lastText.put(id, text);
            }
            return;
        }
        TextDisplay spawned = loc.getWorld().spawn(loc, TextDisplay.class, display -> {
            display.setPersistent(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(false);
            display.setBackgroundColor(Color.fromARGB(150, 20, 0, 30));
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setText(text);
        });
        this.entities.put(id, spawned.getUniqueId());
        this.lastText.put(id, text);
    }

    void refreshAll() {
        ConfigurationSection section = this.data.getConfigurationSection("boards");
        if (section == null) {
            return;
        }
        for (String id : new ArrayList<String>(section.getKeys(false))) {
            try {
                this.refresh(id);
            }
            catch (RuntimeException ex) {
                Store.problem("Boards:" + id, ex);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        ConfigurationSection section = this.data.getConfigurationSection("boards");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            Location loc = this.where(id);
            if (loc != null && loc.getWorld().equals(event.getWorld()) && (loc.getBlockX() >> 4) == event.getChunk().getX()
                    && (loc.getBlockZ() >> 4) == event.getChunk().getZ()) {
                this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> this.refresh(id));
            }
        }
    }

    /* ---------- Befehl ---------- */

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("duelnetwork.admin")) {
            sender.sendMessage("§cDafür fehlt dir die Berechtigung.");
            return true;
        }
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "types" -> sender.sendMessage("§dTypen: §f" + String.join(", ", TYPES) + " §8(parkour braucht eine Map: /board add parkour <Map>)");
            case "list" -> {
                ConfigurationSection section = this.data.getConfigurationSection("boards");
                if (section == null || section.getKeys(false).isEmpty()) {
                    sender.sendMessage("§7Keine Ranglisten aufgestellt.");
                    return true;
                }
                for (String id : section.getKeys(false)) {
                    sender.sendMessage("§d" + id + " §8• §f" + this.data.getString("boards." + id + ".type") + " "
                            + this.data.getString("boards." + id + ".map", "") + " §8@ §7" + this.data.getString("boards." + id + ".world")
                            + " " + (int)this.data.getDouble("boards." + id + ".x") + " " + (int)this.data.getDouble("boards." + id + ".y") + " " + (int)this.data.getDouble("boards." + id + ".z"));
                }
            }
            case "refresh" -> {
                this.refreshAll();
                sender.sendMessage("§aRanglisten aktualisiert.");
            }
            case "add" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Nur im Spiel.");
                    return true;
                }
                if (args.length < 2 || !TYPES.contains(args[1].toLowerCase(Locale.ROOT))) {
                    sender.sendMessage("§cNutze: /board add <" + String.join("|", TYPES) + "> [Map]");
                    return true;
                }
                String type = args[1].toLowerCase(Locale.ROOT);
                String map = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "";
                if (type.equals("parkour") && map.isEmpty()) {
                    sender.sendMessage("§cNutze: /board add parkour <Map>");
                    return true;
                }
                String id = type + (map.isEmpty() ? "" : "-" + map) + "-" + (System.currentTimeMillis() % 100000L);
                Location loc = player.getLocation().add(0.0, 2.0, 0.0);
                this.data.set("boards." + id + ".type", type);
                this.data.set("boards." + id + ".map", map);
                this.data.set("boards." + id + ".world", loc.getWorld().getName());
                this.data.set("boards." + id + ".x", loc.getX());
                this.data.set("boards." + id + ".y", loc.getY());
                this.data.set("boards." + id + ".z", loc.getZ());
                this.save();
                this.refresh(id);
                sender.sendMessage("§a✔ Rangliste aufgestellt: §f" + id + " §7(entfernen: /board remove in der Nähe)");
            }
            case "remove" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Nur im Spiel.");
                    return true;
                }
                ConfigurationSection section = this.data.getConfigurationSection("boards");
                String best = null;
                double bestDistance = 8.0;
                if (section != null) {
                    for (String id : section.getKeys(false)) {
                        Location loc = this.where(id);
                        if (loc != null && loc.getWorld().equals(player.getWorld()) && loc.distance(player.getLocation()) < bestDistance) {
                            bestDistance = loc.distance(player.getLocation());
                            best = id;
                        }
                    }
                }
                if (best == null) {
                    sender.sendMessage("§cKeine Rangliste im Umkreis von 8 Blöcken.");
                    return true;
                }
                UUID known = this.entities.remove(best);
                Entity entity = known == null ? null : Bukkit.getEntity(known);
                if (entity != null) {
                    entity.remove();
                }
                this.lastText.remove(best);
                this.data.set("boards." + best, null);
                this.save();
                sender.sendMessage("§aRangliste entfernt: §f" + best);
            }
            default -> sender.sendMessage("§d/board §fadd <Typ> [Map] §8| §fremove §8| §flist §8| §frefresh §8| §ftypes");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            for (String sub : List.of("add", "remove", "list", "refresh", "types")) {
                if (sub.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            for (String type : TYPES) {
                if (type.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(type);
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("add") && args[1].equalsIgnoreCase("parkour")) {
            ConfigurationSection maps = this.plugin.getConfig().getConfigurationSection("parkour.maps");
            if (maps != null) {
                out.addAll(maps.getKeys(false));
            }
        }
        return out;
    }
}
