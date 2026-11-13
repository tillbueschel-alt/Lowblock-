package de.server.network;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Team-Werkzeuge (Berechtigung duelnetwork.admin): /vanish (unsichtbar fuer normale Spieler), /freeze <Spieler> (Spieler wird festgehalten,
 * Schaden aus), /invsee <Spieler> (Inventar eines Online-Spielers ansehen/bearbeiten), /staffchat bzw. /sc [Text] (Team-Chat; ohne Text
 * wird der Modus fuer normale Chat-Nachrichten umgeschaltet). Zustand (Vanish/Freeze) gilt bis zum Neustart.
 */
final class Staff implements CommandExecutor, TabCompleter, Listener {
    private final DuelNetworkPlugin plugin;
    private final Set<UUID> vanished = new HashSet<UUID>();
    private final Set<UUID> frozen = new HashSet<UUID>();
    private final Set<UUID> staffMode = new HashSet<UUID>();

    Staff(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        for (String name : new String[]{"vanish", "freeze", "invsee", "staffchat"}) {
            if (plugin.getCommand(name) != null) {
                plugin.getCommand(name).setExecutor((CommandExecutor)this);
                plugin.getCommand(name).setTabCompleter((TabCompleter)this);
            }
        }
    }

    private boolean isStaff(CommandSender sender) {
        return sender.isOp() || sender.hasPermission("duelnetwork.admin");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!this.isStaff(sender)) {
            sender.sendMessage("§cDafür fehlt dir die Berechtigung.");
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "vanish" -> this.vanish(player);
            case "freeze" -> this.freeze(player, args);
            case "invsee" -> {
                Player target = args.length == 0 ? null : this.plugin.getServer().getPlayerExact(args[0]);
                if (target == null) {
                    player.sendMessage("§cNutze: /invsee <Spieler> §7(muss online sein)");
                } else {
                    player.openInventory(target.getInventory());
                }
            }
            default -> this.staffChat(player, args);
        }
        return true;
    }

    private void vanish(Player player) {
        UUID id = player.getUniqueId();
        if (this.vanished.remove(id)) {
            for (Player other : this.plugin.getServer().getOnlinePlayers()) {
                other.showPlayer((Plugin)this.plugin, player);
            }
            player.sendMessage("§7Vanish: §caus §7– du bist wieder sichtbar.");
        } else {
            this.vanished.add(id);
            for (Player other : this.plugin.getServer().getOnlinePlayers()) {
                if (!this.isStaff(other)) {
                    other.hidePlayer((Plugin)this.plugin, player);
                }
            }
            player.sendMessage("§7Vanish: §aan §7– normale Spieler sehen dich nicht. §8(/vanish zum Beenden)");
        }
    }

    private void freeze(Player staff, String[] args) {
        Player target = args.length == 0 ? null : this.plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            staff.sendMessage("§cNutze: /freeze <Spieler>");
            return;
        }
        UUID id = target.getUniqueId();
        if (this.frozen.remove(id)) {
            target.sendMessage("§aDu wurdest wieder freigegeben.");
            staff.sendMessage("§7" + target.getName() + " ist nicht mehr eingefroren.");
        } else {
            this.frozen.add(id);
            target.sendTitle("§c§lEINGEFROREN", "§7Bitte melde dich im Chat beim Team.", 5, 100, 20);
            target.sendMessage("§cDu wurdest vom Team eingefroren. Bitte antworte im Chat, verlasse den Server nicht.");
            staff.sendMessage("§7" + target.getName() + " ist jetzt eingefroren. §8(/freeze " + target.getName() + " zum Freigeben)");
        }
    }

    private void staffChat(Player player, String[] args) {
        if (args.length == 0) {
            boolean on = this.staffMode.add(player.getUniqueId());
            if (!on) {
                this.staffMode.remove(player.getUniqueId());
            }
            player.sendMessage("§7Team-Chat-Modus: " + (on ? "§aan §7(alle Nachrichten gehen ans Team)" : "§caus"));
            return;
        }
        this.broadcast(player, String.join(" ", args));
    }

    private void broadcast(Player from, String text) {
        String line = "§8[§cTeam§8] §f" + from.getName() + "§7: §f" + text;
        for (Player online : this.plugin.getServer().getOnlinePlayers()) {
            if (this.isStaff(online)) {
                online.sendMessage(line);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (this.staffMode.contains(player.getUniqueId()) && this.isStaff(player)) {
            event.setCancelled(true);
            String text = event.getMessage();
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> this.broadcast(player, text));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();
        if (!this.isStaff(joined)) {
            for (UUID id : this.vanished) {
                Player hidden = this.plugin.getServer().getPlayer(id);
                if (hidden != null) {
                    joined.hidePlayer((Plugin)this.plugin, hidden);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (this.frozen.contains(event.getPlayer().getUniqueId()) && event.getTo() != null
                && (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ() || event.getFrom().getY() < event.getTo().getY())) {
            event.setTo(event.getFrom().clone().setDirection(event.getTo().getDirection()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player && this.frozen.contains(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (this.frozen.remove(player.getUniqueId())) {
            for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                if (this.isStaff(online)) {
                    online.sendMessage("§c⚠ §f" + player.getName() + " §7hat den Server verlassen, während er eingefroren war.");
                }
            }
        }
        this.staffMode.remove(player.getUniqueId());
        if (this.vanished.remove(player.getUniqueId())) {
            for (Player other : this.plugin.getServer().getOnlinePlayers()) {
                other.showPlayer((Plugin)this.plugin, player);
            }
        }
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        if (this.isStaff(sender) && args.length == 1 && !command.getName().equalsIgnoreCase("vanish") && !command.getName().equalsIgnoreCase("staffchat")) {
            for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(online.getName());
                }
            }
        }
        return out;
    }
}
