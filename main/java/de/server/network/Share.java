package de.server.network;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * /share [Text]: schickt allen Freunden einen anklickbaren Teleport zu deinem aktuellen Ort (SMP oder Bau-SMP),
 * 10 Minuten gueltig. Klick fuehrt /sharetp <id> aus. Teleport nur, wenn der Freund im selben Modus ist.
 * Kein Galerie- oder Wettbewerbssystem, nur ein schnelles "Schau mal, was ich gebaut habe".
 */
final class Share implements CommandExecutor {
    private record Spot(Location location, String owner, String zone, long until) { }

    private final DuelNetworkPlugin plugin;
    private final Map<String, Spot> spots = new HashMap<String, Spot>();
    private final Map<UUID, Long> cooldown = new HashMap<UUID, Long>();
    private int counter;

    Share(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        for (String name : List.of("share", "sharetp")) {
            if (plugin.getCommand(name) != null) {
                plugin.getCommand(name).setExecutor((CommandExecutor)this);
            }
        }
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, () -> {
            long now = System.currentTimeMillis();
            this.spots.values().removeIf(s -> s.until() < now);
        }, 1200L, 1200L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        if (command.getName().equalsIgnoreCase("sharetp")) {
            this.go(player, args.length > 0 ? args[0] : "");
            return true;
        }
        if (!Zone.survival(this.plugin, player)) {
            player.sendMessage("§c/share geht nur im SMP und Bau-SMP.");
            return true;
        }
        long now = System.currentTimeMillis();
        if (now - this.cooldown.getOrDefault(player.getUniqueId(), 0L) < 30_000L) {
            player.sendMessage("§cBitte kurz warten (30 s zwischen zwei Freigaben).");
            return true;
        }
        this.cooldown.put(player.getUniqueId(), now);
        String text = String.join(" ", args).trim();
        if (text.length() > 60) {
            text = text.substring(0, 60);
        }
        String id = Integer.toString(++this.counter, 36);
        String zone = Zone.key(this.plugin, player);
        this.spots.put(id, new Spot(player.getLocation().clone(), player.getName(), zone, now + 600_000L));
        int sent = 0;
        Friends friends = Friends.instance;
        if (friends != null) {
            for (UUID fid : friends.friendsOf(player.getUniqueId())) {
                Player f = this.plugin.getServer().getPlayer(fid);
                if (f == null) {
                    continue;
                }
                f.sendMessage(Component.text("§d✦ §f" + player.getName() + " §7zeigt dir etwas" + (text.isEmpty() ? "" : ": §f" + text) + " §8(" + (zone.equals("bau") ? "Bau-SMP" : "SMP") + ") §a[Hinkommen]")
                        .clickEvent(ClickEvent.runCommand("/sharetp " + id))
                        .hoverEvent(HoverEvent.showText(Component.text("§7Klicken zum Teleportieren (10 Min. gültig)"))));
                sent++;
            }
        }
        player.sendMessage(sent == 0 ? "§7Keine Freunde online, die das sehen können. §8(Freunde: /friend)" : "§aAn " + sent + " Freund(e) gesendet. §8Gültig für 10 Minuten.");
        return true;
    }

    private void go(Player player, String id) {
        Spot spot = this.spots.get(id);
        if (spot == null || spot.until() < System.currentTimeMillis()) {
            player.sendMessage("§cDiese Freigabe ist abgelaufen.");
            return;
        }
        Friends friends = Friends.instance;
        boolean friend = friends != null && this.plugin.getServer().getPlayerExact(spot.owner()) != null
                && friends.friendsOf(this.plugin.getServer().getPlayerExact(spot.owner()).getUniqueId()).contains(player.getUniqueId());
        if (!friend) {
            player.sendMessage("§cDas darfst du nicht (nur Freunde, und der Besitzer muss online sein).");
            return;
        }
        if (!Zone.survival(this.plugin, player) || !Zone.key(this.plugin, player).equals(spot.zone())) {
            player.sendMessage("§cDafür musst du im " + (spot.zone().equals("bau") ? "Bau-SMP" : "SMP") + " sein.");
            return;
        }
        player.teleport(spot.location());
        player.sendMessage("§aDu bist bei §f" + spot.owner() + "§a.");
    }
}
