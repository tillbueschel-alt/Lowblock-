package de.server.network;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/** Team-Notizen pro Spieler: /note &lt;Spieler&gt; &lt;Text&gt; und /notes &lt;Spieler&gt; [clear] (Permission duelnetwork.admin). */
final class Notes implements CommandExecutor, TabCompleter {
    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    Notes(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "notes.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("notes.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("duelnetwork.admin")) {
            sender.sendMessage("§cDafür hast du keine Rechte.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§d/note <Spieler> <Text> §7– Notiz anlegen   §d/notes <Spieler> [clear] §7– Notizen anzeigen");
            return true;
        }
        OfflinePlayer target = this.plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            target = this.plugin.getServer().getOfflinePlayerIfCached(args[0]);
        }
        if (target == null) {
            sender.sendMessage("§cSpieler unbekannt.");
            return true;
        }
        UUID id = target.getUniqueId();
        String key = "players." + id;
        if (command.getName().equalsIgnoreCase("note")) {
            if (args.length < 2) {
                sender.sendMessage("§c/note <Spieler> <Text>");
                return true;
            }
            List<String> notes = new ArrayList<String>(this.data.getStringList(key));
            notes.add(new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date()) + " " + sender.getName() + ": " + String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)));
            this.data.set(key, notes);
            this.save();
            sender.sendMessage("§aNotiz gespeichert.");
            return true;
        }
        if (args.length >= 2 && args[1].equalsIgnoreCase("clear")) {
            this.data.set(key, null);
            this.save();
            sender.sendMessage("§7Notizen gelöscht.");
            return true;
        }
        List<String> notes = this.data.getStringList(key);
        sender.sendMessage("§d✦ §fNotizen zu " + (target.getName() == null ? args[0] : target.getName()) + " §7(" + notes.size() + ")");
        for (String line : notes) {
            sender.sendMessage(" §8» §7" + line);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1 && sender.hasPermission("duelnetwork.admin")) {
            for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(online.getName());
                }
            }
        }
        return out;
    }
}
