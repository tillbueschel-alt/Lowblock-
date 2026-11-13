package de.server.network;

import java.util.Locale;
import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.command.UnknownCommandEvent;

/** Bei einem unbekannten Befehl: "Meintest du /duel?" (nur Befehle dieses Plugins). */
final class HintCommands implements Listener {
    private final DuelNetworkPlugin plugin;

    HintCommands(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
    }

    private static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            int[] cur = new int[b.length() + 1];
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            }
            prev = cur;
        }
        return prev[b.length()];
    }

    @EventHandler
    public void onUnknown(UnknownCommandEvent event) {
        String line = event.getCommandLine().trim();
        String typed = (line.startsWith("/") ? line.substring(1) : line).split(" ")[0].toLowerCase(Locale.ROOT);
        if (typed.length() < 3) {
            return;
        }
        Set<String> names = this.plugin.getDescription().getCommands().keySet();
        String best = null;
        int bestDistance = 3;
        for (String name : names) {
            int d = name.startsWith(typed) || typed.startsWith(name) && name.length() >= 4 ? 1 : distance(typed, name);
            if (d < bestDistance) {
                bestDistance = d;
                best = name;
            }
        }
        if (best != null) {
            event.setMessage("§cUnbekannter Befehl. §7Meintest du §f/" + best + "§7?");
        }
    }
}
