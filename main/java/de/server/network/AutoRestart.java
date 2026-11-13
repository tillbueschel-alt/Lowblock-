package de.server.network;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Geplanter Neustart mit Vorwarnung (config restart.*). Zum angegebenen Zeitpunkt (restart.at, z. B. ["04:00"]) wird
 * 10, 5, 1 Minute sowie 30, 10, 5-1 Sekunden vorher angesagt, dann werden alle Spieler mit Hinweis gekickt, gespeichert
 * und der Server sauber heruntergefahren. WICHTIG: Der Host muss den Server danach wieder starten (Neustart-Skript oder
 * Panel-Auto-Restart), sonst bleibt er aus.
 */
final class AutoRestart {
    private static final int[] WARN_SECONDS = {600, 300, 60, 30, 10, 5, 4, 3, 2, 1};

    private final DuelNetworkPlugin plugin;
    private final Set<String> announced = new HashSet<String>();
    private boolean stopping;

    AutoRestart(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        if (plugin.getConfig().getBoolean("restart.enabled", false)) {
            plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("AutoRestart", this::tick), 40L, 20L);
            plugin.getLogger().info("Geplanter Neustart aktiv: " + plugin.getConfig().getStringList("restart.at"));
        }
    }

    private void tick() {
        if (this.stopping) {
            return;
        }
        List<String> times = this.plugin.getConfig().getStringList("restart.at");
        LocalTime now = LocalTime.now();
        for (String raw : times) {
            LocalTime target;
            try {
                target = LocalTime.parse(raw.trim(), DateTimeFormatter.ofPattern("H:mm"));
            }
            catch (RuntimeException ex) {
                continue;
            }
            long left = java.time.Duration.between(now, target).getSeconds();
            if (left < 0L) {
                left += 86400L;
            }
            for (int warn : WARN_SECONDS) {
                if (left == warn && this.announced.add(raw + "@" + warn + "@" + java.time.LocalDate.now())) {
                    this.announce(warn);
                }
            }
            if (left <= 1L && this.announced.add(raw + "@go@" + java.time.LocalDate.now())) {
                this.stop();
                return;
            }
        }
        if (this.announced.size() > 200) {
            this.announced.clear();
        }
    }

    private void announce(int seconds) {
        String text = seconds >= 60 ? (seconds / 60) + " Minute" + (seconds >= 120 ? "n" : "") : seconds + " Sekunde" + (seconds == 1 ? "" : "n");
        this.plugin.getServer().broadcastMessage("§d✦ §fServer-Neustart in §d" + text + "§f. §7Bitte bring dich in Sicherheit und beende laufende Duelle.");
        for (Player player : this.plugin.getServer().getOnlinePlayers()) {
            Chat.ping(player);
        }
    }

    private void stop() {
        this.stopping = true;
        this.plugin.getLogger().info("Geplanter Neustart: Server faehrt herunter.");
        String message = this.plugin.getConfig().getString("restart.kick-message", "§dServer-Neustart\n§7Wir sind gleich wieder da!");
        for (Player player : new java.util.ArrayList<Player>(this.plugin.getServer().getOnlinePlayers())) {
            player.kickPlayer(message);
        }
        // onDisable speichert alles (Wirtschaft, Statistik, Profile)
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> this.plugin.getServer().shutdown(), 20L);
    }
}
