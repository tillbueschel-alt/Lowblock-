package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * Persoenliche Benachrichtigungs-Einstellungen: /notify [claim|mention|bounty] [on|off].
 * claim = Claim-Hinweis in der Actionbar, mention = @Name-Ping, bounty = Kopfgeld-Meldungen im Chat.
 * Standard ist fuer alle "an". Gespeichert in prefs.yml (nur bei Aenderung).
 */
final class Prefs implements CommandExecutor, TabCompleter {
    private static final List<String> KEYS = List.of("claim", "mention", "bounty", "hints");
    static Prefs instance;

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    Prefs(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "prefs.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        instance = this;
        if (plugin.getCommand("notify") != null) {
            plugin.getCommand("notify").setExecutor((CommandExecutor)this);
            plugin.getCommand("notify").setTabCompleter((TabCompleter)this);
        }
    }

    /** true, wenn die Meldung fuer den Spieler aktiv ist (Standard). */
    static boolean on(UUID id, String key) {
        return instance == null || instance.data.getBoolean("off." + id + "." + key, false) == false;
    }

    /** Schaltet eine Einstellung um und liefert den neuen Zustand. */
    static boolean flip(UUID id, String key) {
        if (instance == null) {
            return true;
        }
        boolean enabled = !on(id, key);
        instance.set(id, key, enabled);
        return enabled;
    }

    private void set(UUID id, String key, boolean enabled) {
        this.data.set("off." + id + "." + key, enabled ? null : Boolean.TRUE);
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("prefs.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        UUID id = player.getUniqueId();
        if (args.length == 0) {
            player.sendMessage("§d✦ §fBenachrichtigungen §8(§d/notify <Name> on|off§8)");
            player.sendMessage("§7claim §8– §7Claim-Hinweis in der Actionbar: " + (on(id, "claim") ? "§aan" : "§coff"));
            player.sendMessage("§7mention §8– §7Ping bei @Name: " + (on(id, "mention") ? "§aan" : "§coff"));
            player.sendMessage("§7bounty §8– §7Kopfgeld-Meldungen im Chat: " + (on(id, "bounty") ? "§aan" : "§coff"));
            return true;
        }
        String key = args[0].toLowerCase(Locale.ROOT);
        if (!KEYS.contains(key)) {
            player.sendMessage("§cMögliche Einstellungen: §f" + String.join(", ", KEYS));
            return true;
        }
        boolean enabled = args.length >= 2 ? args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("an") : !on(id, key);
        this.set(id, key, enabled);
        player.sendMessage("§7" + key + ": " + (enabled ? "§aan" : "§caus"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1) {
            out.addAll(KEYS);
        } else if (args.length == 2) {
            out.addAll(List.of("on", "off"));
        }
        return out;
    }
}
