package de.server.network;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * LowBlock-Zusaetze fuer Parkour: Rekorde mit Podium und Seiten, Hilfe, /leave,
 * Tab-Vervollstaendigung, automatisches Verlassen beim Disconnect und der
 * LowBlock-Schriftzug in der Tab-Liste.
 */
public final class ParkourEnhancements implements Listener {
    private static final List<String> SUBS = List.of("help", "list", "join", "leave", "restart", "records");
    private static final Set<String> MAP_SUBS = Set.of("join", "start", "records", "top", "stats", "rekorde");
    private static final int PAGE_SIZE = 10;

    static final String LINE = "§5§m                                        §r";
    static final String BANNER = "§5§m      §r §d§l✦ LowBlock ✦ §5§m      §r";

    private final DuelNetworkPlugin plugin;

    private static ParkourEnhancements instance;

    public ParkourEnhancements(DuelNetworkPlugin plugin) {
        instance = this;
        this.plugin = plugin;
    }

    static List<String> mapNames(DuelNetworkPlugin plugin) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("parkour.maps");
        return section == null ? List.of() : new ArrayList<String>(section.getKeys(false));
    }

    private static List<String> filter(Collection<String> options, String typed) {
        String prefix = typed == null ? "" : typed.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<String>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(option);
            }
        }
        return out;
    }

    /** Wird von DuelNetworkPlugin.onTabComplete fuer /parkour aufgerufen. */
    public static List<String> complete(DuelNetworkPlugin plugin, String[] args) {
        if (args.length == 1) {
            return filter(SUBS, args[0]);
        }
        if (args.length == 2 && MAP_SUBS.contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(mapNames(plugin), args[1]);
        }
        return new ArrayList<String>();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        try {
            event.getPlayer().setPlayerListHeaderFooter(
                    "\n" + BANNER + "\n§7Dein Server für Duels, Parkour & mehr\n",
                    "\n§7Viel Spaß auf §d§lLowBlock§r§7!\n");
        }
        catch (Throwable ignored) {
            // Tab-Header ist nur Kosmetik
            Store.problem("ParkourEnhancements:74", ignored);
        }
    }

    /** Wer den Server im Parkour verlaesst, wird automatisch aus dem Parkour entfernt. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (this.plugin.activeParkourMaps.containsKey(player.getUniqueId())) {
            this.plugin.leaveParkour(player, true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null) {
            return;
        }
        Player player = event.getPlayer();
        if (message.trim().equalsIgnoreCase("/leave") && this.plugin.activeParkourMaps.containsKey(player.getUniqueId())) {
            event.setMessage("/parkour leave");
            return;
        }
        String[] parts = message.trim().split("\\s+");
        String label = parts[0].toLowerCase(Locale.ROOT);
        if (label.startsWith("/")) {
            label = label.substring(1);
        }
        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }
        if (!label.equals("parkour") || parts.length < 2) {
            return;
        }
        switch (parts[1].toLowerCase(Locale.ROOT)) {
            case "help", "hilfe", "?" -> {
                event.setCancelled(true);
                this.sendHelp(player);
            }
            case "records", "rekorde", "top", "stats" -> {
                event.setCancelled(true);
                this.handleRecords(player, parts);
            }
            default -> { }
        }
    }

    private void handleRecords(Player player, String[] parts) {
        int page = 1;
        String wanted = null;
        for (int i = 2; i < parts.length; i++) {
            if (parts[i].matches("\\d{1,3}")) {
                page = Math.max(1, Integer.parseInt(parts[i]));
            } else if (wanted == null) {
                wanted = parts[i];
            }
        }
        if (wanted == null) {
            wanted = this.plugin.activeParkourMaps.get(player.getUniqueId());
        }
        List<String> maps = mapNames(this.plugin);
        if (wanted == null) {
            player.sendMessage(BANNER);
            player.sendMessage("§dNutze: §f/parkour records <Map> [Seite]");
            player.sendMessage("§7Maps: §f" + String.join("§7, §f", maps));
            return;
        }
        String key = null;
        for (String map : maps) {
            if (map.equalsIgnoreCase(wanted)) {
                key = map;
            }
        }
        if (key == null) {
            player.sendMessage("§cDiese Parkour-Map gibt es nicht. §7Maps: §f" + String.join("§7, §f", maps));
            return;
        }
        this.sendRecords(player, key, page);
    }

    static String formatTime(long ms) {
        ms = Math.max(0L, ms);
        return String.format("%02d:%02d.%03d", ms / 60000L, ms / 1000L % 60L, ms % 1000L);
    }

    /** Wird auch vom Plugin genutzt (z.B. nach dem Zieleinlauf). */
    static void showRecords(Player player, String map) {
        ParkourEnhancements self = instance;
        if (self != null) {
            self.sendRecords(player, map, 1);
        }
    }

    private void sendRecords(Player player, String map, int requestedPage) {
        List<RecordStore.Entry> rows = this.plugin.records.top(map);
        int pages = Math.max(1, (rows.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(requestedPage, pages);
        String me = player.getUniqueId().toString();
        int myRank = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).id().equals(me)) {
                myRank = i + 1;
            }
        }
        long best = rows.isEmpty() ? 0L : rows.get(0).timeMs();
        player.sendMessage("");
        player.sendMessage(BANNER);
        player.sendMessage("§d§l⚑ Parkour-Rekorde §8» §f§l" + map);
        if (rows.isEmpty()) {
            player.sendMessage("");
            player.sendMessage("§7Noch keine Zeiten – sei der Erste! §d/parkour join " + map);
        } else {
            String mine = myRank < 0 ? "§7noch keine Zeit" : "§d#" + myRank + " §8(§f" + formatTime(rows.get(myRank - 1).timeMs()) + "§8)";
            player.sendMessage("§7Läufer: §f" + rows.size() + " §8• §7Dein Platz: " + mine);
            player.sendMessage("");
        }
        int from = (page - 1) * PAGE_SIZE;
        for (int i = from; i < Math.min(rows.size(), from + PAGE_SIZE); i++) {
            player.sendMessage(this.recordLine(i, rows.get(i), best, rows.get(i).id().equals(me)));
        }
        if (myRank > from + PAGE_SIZE) {
            player.sendMessage("§8 ...");
            player.sendMessage(this.recordLine(myRank - 1, rows.get(myRank - 1), best, true));
        }
        player.sendMessage("");
        if (pages > 1) {
            player.sendMessage("§7Seite §f" + page + "§7/§f" + pages + " §8• §d/parkour records " + map + " <Seite>");
        }
        player.sendMessage(LINE);
    }

    private String recordLine(int index, RecordStore.Entry row, long best, boolean you) {
        String time = formatTime(row.timeMs());
        String gap = index == 0 ? "" : " §8(+" + formatTime(row.timeMs() - best) + ")";
        String mark = you ? " §d◀ Du" : "";
        return switch (index) {
            case 0 -> " §6§l♛ 1. §e§l" + row.name() + " §8» §6§l" + time + mark;
            case 1 -> " §f§l★ 2. §f" + row.name() + " §8» §f" + time + gap + mark;
            case 2 -> " §c§l✦ 3. §c" + row.name() + " §8» §c" + time + gap + mark;
            default -> " §8▪ §7" + (index + 1) + ". §7" + row.name() + " §8» §d" + time + gap + mark;
        };
    }

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage(BANNER);
        player.sendMessage("§d§l⚑ Parkour – Hilfe");
        player.sendMessage("");
        player.sendMessage(" §d/parkour §8» §7Parkour-Lobby öffnen");
        player.sendMessage(" §d/parkour list §8» §7alle Strecken anzeigen");
        player.sendMessage(" §d/parkour join <Map> §8» §7eine Strecke starten");
        player.sendMessage(" §d/parkour restart §8» §7Strecke neu starten");
        player.sendMessage(" §d/parkour leave §8» §7Parkour verlassen §8(auch /leave)");
        player.sendMessage(" §d/parkour records [Map] [Seite] §8» §7Bestenliste mit Podium");
        player.sendMessage(" §d/parkour help §8» §7diese Hilfe");
        player.sendMessage("");
        player.sendMessage(LINE);
    }
}
