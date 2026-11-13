package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Kurze Erstes-Mal-Hinweise je Modus (SMP, Bau-SMP, Lobby, Duell, Event, Parkour). Jeder Spieler sieht jeden Hinweis nur einmal;
 * gemerkt wird das in hints.yml. Abschaltbar mit hints.enabled: false.
 */
final class Hints {
    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Map<UUID, String> lastMode = new HashMap<UUID, String>();

    Hints(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "hints.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("Hints", this::tick), 100L, 100L);
    }

    private String modeOf(Player p) {
        UUID id = p.getUniqueId();
        if (this.plugin.activeDuelPlayers.contains(id) || this.plugin.activePartyMatches.containsKey(id)) {
            return "duel";
        }
        if (this.plugin.activeParkourMaps.containsKey(id)) {
            return "parkour";
        }
        if (EventGames.participant(id)) {
            return "event";
        }
        String profile = this.plugin.profiles.getOrDefault(id, "lobby");
        if (profile.equals("smp")) {
            return this.plugin.inBauMode(id) ? "bau" : "smp";
        }
        return "lobby";
    }

    private static String text(String mode) {
        return switch (mode) {
            case "smp" -> "§d✦ SMP §7– Überleben, Handeln und Claims. §f/claim menu §7schützt dein Gebiet, §f/shop §7und §f/ah §7zum Handeln.";
            case "bau" -> "§d✦ Bau-SMP §7– frei bauen ohne PvP. Dein Inventar bleibt hier getrennt vom SMP.";
            case "duel" -> "§d✦ Duell §7– mit §f/duel §7forderst du jemanden heraus; Elo-Duelle zählen für die Rangliste.";
            case "event" -> "§d✦ Event §7– das Bracket siehst du mit §f/events bracket§7, verlassen mit §f/events leave§7.";
            case "parkour" -> "§d✦ Parkour §7– Rekorde und Lauf-Rang stehen in der Sidebar.";
            default -> "§d✦ Lobby §7– über die NPCs kommst du in SMP, Duelle, Events und Parkour.";
        };
    }

    private void tick() {
        if (!this.plugin.getConfig().getBoolean("hints.enabled", true)) {
            return;
        }
        boolean changed = false;
        for (Player p : this.plugin.getServer().getOnlinePlayers()) {
            if (!Prefs.on(p.getUniqueId(), "hints")) {
                continue;
            }
            String mode = this.modeOf(p);
            if (mode.equals(this.lastMode.put(p.getUniqueId(), mode))) {
                continue;
            }
            String key = p.getUniqueId() + "." + mode;
            if (this.data.getBoolean(key, false)) {
                continue;
            }
            this.data.set(key, true);
            changed = true;
            p.sendMessage(text(mode));
        }
        this.lastMode.keySet().removeIf(id -> this.plugin.getServer().getPlayer(id) == null);
        if (changed) {
            try {
                Store.save(this.data, this.file);
            }
            catch (IOException e) {
                Store.problem("Hints:save", e);
            }
        }
    }
}
