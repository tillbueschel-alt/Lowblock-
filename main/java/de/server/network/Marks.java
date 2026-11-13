package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Persoenliche Markierungen (nur SMP und Bau-SMP, getrennt pro Modus): /mark set <Name> | del <Name> | list | <Name>.
 * Es wird nicht teleportiert: /mark <Name> zeigt 60 Sekunden lang Richtung und Entfernung in der Actionbar. Maximal marks.max (5).
 */
final class Marks implements CommandExecutor, TabCompleter {
    private static final String[] ARROWS = {"⬆", "⬈", "➡", "⬊", "⬇", "⬋", "⬅", "⬉"};

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final java.util.Set<java.util.UUID> guiding = new java.util.HashSet<java.util.UUID>();

    Marks(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "marks.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        if (plugin.getCommand("mark") != null) {
            plugin.getCommand("mark").setExecutor((CommandExecutor)this);
            plugin.getCommand("mark").setTabCompleter((TabCompleter)this);
        }
    }

    private String base(Player player) {
        return "marks." + Zone.key(this.plugin, player) + "." + player.getUniqueId();
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("marks.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private List<String> names(Player player) {
        ConfigurationSection section = this.data.getConfigurationSection(this.base(player));
        return section == null ? new ArrayList<String>() : new ArrayList<String>(section.getKeys(false));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        if (!Zone.survival(this.plugin, player)) {
            player.sendMessage("§cMarkierungen gibt es nur im SMP und Bau-SMP.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            List<String> names = this.names(player);
            player.sendMessage(names.isEmpty() ? "§7Keine Markierungen. §d/mark set <Name>" : "§7Deine Markierungen: §f" + String.join("§7, §f", names));
            player.sendMessage("§8/mark <Name> zeigt dir den Weg • /mark set <Name> • /mark del <Name>");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("set") && args.length >= 2) {
            String name = args[1].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_äöüß-]", "");
            if (name.isEmpty() || name.length() > 16) {
                player.sendMessage("§cName: 1-16 Zeichen (Buchstaben, Zahlen, _ -).");
                return true;
            }
            if (!this.names(player).contains(name) && this.names(player).size() >= Math.max(1, this.plugin.getConfig().getInt("marks.max", 5))) {
                player.sendMessage("§cDu hast schon die maximale Anzahl Markierungen. §7Lösche eine mit /mark del <Name>.");
                return true;
            }
            Location at = player.getLocation();
            this.data.set(this.base(player) + "." + name, at.getWorld().getName() + "," + at.getBlockX() + "," + at.getBlockY() + "," + at.getBlockZ());
            this.save();
            player.sendMessage("§aMarkierung §f" + name + " §agesetzt.");
            return true;
        }
        if (sub.equals("del") && args.length >= 2) {
            this.data.set(this.base(player) + "." + args[1].toLowerCase(Locale.ROOT), null);
            this.save();
            player.sendMessage("§7Markierung gelöscht.");
            return true;
        }
        this.guide(player, args[0].toLowerCase(Locale.ROOT));
        return true;
    }

    private void guide(Player player, String name) {
        String raw = this.data.getString(this.base(player) + "." + name, "");
        if (raw.isEmpty()) {
            player.sendMessage("§cMarkierung nicht gefunden. §7/mark list");
            return;
        }
        String[] p = raw.split(",");
        Location target = new Location(this.plugin.getServer().getWorld(p[0]), Integer.parseInt(p[1]) + 0.5, Integer.parseInt(p[2]), Integer.parseInt(p[3]) + 0.5);
        if (target.getWorld() == null || !this.guiding.add(player.getUniqueId())) {
            return;
        }
        new BukkitRunnable() {
            int ticks;
            @Override
            public void run() {
                Location here = player.getLocation();
                if (!player.isOnline() || (this.ticks += 10) > 1200 || !Zone.survival(Marks.this.plugin, player)) {
                    Marks.this.guiding.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                if (!here.getWorld().equals(target.getWorld())) {
                    player.sendActionBar(net.kyori.adventure.text.Component.text("§d➤ " + name + " §7liegt in einer anderen Welt"));
                    return;
                }
                double dx = target.getX() - here.getX();
                double dz = target.getZ() - here.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance < 4.0) {
                    player.sendActionBar(net.kyori.adventure.text.Component.text("§a✔ §fDu bist bei §d" + name));
                    Marks.this.guiding.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                double angleToTarget = Math.toDegrees(Math.atan2(-dx, dz));
                double relative = ((angleToTarget - here.getYaw()) % 360.0 + 540.0) % 360.0 - 180.0;
                int index = (int)Math.round(relative / 45.0);
                index = ((index % 8) + 8) % 8;
                player.sendActionBar(net.kyori.adventure.text.Component.text("§d" + ARROWS[index] + " §f" + name + " §7• §f" + (int)distance + " m"));
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 10L);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (!(sender instanceof Player player)) {
            return out;
        }
        if (args.length == 1) {
            out.addAll(List.of("set", "del", "list"));
            out.addAll(this.names(player));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("del")) {
            out.addAll(this.names(player));
        }
        return out;
    }
}
