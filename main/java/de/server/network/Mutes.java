package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

/**
 * Stummschaltung (/mute, /unmute) - dauerhaft oder zeitlich begrenzt, gespeichert in mutes.yml.
 * Stumme Spieler koennen weder im Chat schreiben noch private Nachrichten senden.
 */
final class Mutes implements Listener, CommandExecutor, TabCompleter {
    static Mutes instance;

    private record Entry(String name, long until, String reason, String by) {
    }

    /** Befehle, die ein stummer Spieler nicht benutzen darf. */
    private static final Set<String> BLOCKED_COMMANDS = Set.of("msg", "m", "tell", "w", "whisper", "r", "reply", "me", "say");

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final Map<UUID, Entry> mutes = new ConcurrentHashMap<UUID, Entry>();

    Mutes(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "mutes.yml");
        instance = this;
        YamlConfiguration data = YamlConfiguration.loadConfiguration(this.file);
        ConfigurationSection section = data.getConfigurationSection("mutes");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    String base = "mutes." + key;
                    this.mutes.put(id, new Entry(data.getString(base + ".name", key), data.getLong(base + ".until", 0L),
                            data.getString(base + ".reason", ""), data.getString(base + ".by", "?")));
                }
                catch (IllegalArgumentException ex) {
                    // kaputter Eintrag: ueberspringen
                }
            }
        }
    }

    boolean isMuted(UUID id) {
        return this.active(id) != null;
    }

    /** Liefert den Eintrag, solange die Stummschaltung gilt; abgelaufene Eintraege werden entfernt. */
    private Entry active(UUID id) {
        Entry entry = this.mutes.get(id);
        if (entry == null) {
            return null;
        }
        if (entry.until() > 0L && System.currentTimeMillis() > entry.until()) {
            this.mutes.remove(id);
            // Speichern im Haupt-Thread, auch wenn die Pruefung aus dem asynchronen Chat kommt
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> this.persist());
            return null;
        }
        return entry;
    }

    private void persist() {
        YamlConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, Entry> e : this.mutes.entrySet()) {
            String base = "mutes." + e.getKey();
            data.set(base + ".name", e.getValue().name());
            data.set(base + ".until", e.getValue().until());
            data.set(base + ".reason", e.getValue().reason());
            data.set(base + ".by", e.getValue().by());
        }
        try {
            Store.save(data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("mutes.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private String remaining(Entry entry) {
        return entry.until() <= 0L ? "dauerhaft" : Durations.describe(Math.max(60_000L, entry.until() - System.currentTimeMillis()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Entry entry = this.active(event.getPlayer().getUniqueId());
        if (entry != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Messages.get("mute.blocked", this.remaining(entry), entry.reason()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage().trim();
        if (!raw.startsWith("/") || raw.length() < 2) {
            return;
        }
        String first = raw.substring(1).split("\\s+")[0].toLowerCase(Locale.ROOT);
        int colon = first.indexOf(':');
        if (colon >= 0) {
            first = first.substring(colon + 1);
        }
        if (!BLOCKED_COMMANDS.contains(first)) {
            return;
        }
        Entry entry = this.active(event.getPlayer().getUniqueId());
        if (entry != null) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Messages.get("mute.blocked", this.remaining(entry), entry.reason()));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("unmute")) {
            this.unmute(sender, args);
        } else {
            // /timeout: ohne Dauer standardmaessig mute.default-timeout-minutes (10) Minuten
            long fallback = name.equals("timeout") ? Math.max(1L, this.plugin.getConfig().getLong("mute.default-timeout-minutes", 10L)) * 60000L : 0L;
            this.mute(sender, args, fallback);
        }
        return true;
    }

    private void mute(CommandSender sender, String[] args, long fallbackMs) {
        if (args.length < 1) {
            sender.sendMessage(Messages.get("mute.usage"));
            return;
        }
        long duration = fallbackMs;
        int reasonFrom = 1;
        if (args.length >= 2) {
            long parsed = Durations.parse(args[1]);
            if (parsed > 0L) {
                duration = parsed;
                reasonFrom = 2;
            }
        }
        String reason = args.length > reasonFrom ? String.join(" ", Arrays.copyOfRange(args, reasonFrom, args.length)).trim() : "";
        if (reason.isEmpty()) {
            reason = Messages.get("mute.default-reason");
        }
        final long ms = duration;
        final String why = reason;
        Moderation moderation = this.plugin.moderation;
        moderation.resolve(sender, args[0], target -> {
            if (this.plugin.isProtectedStaff(target)) {
                sender.sendMessage(Messages.get("mod.protected-mute"));
                return;
            }
            String targetName = Moderation.nameOf(target, args[0]);
            long until = ms <= 0L ? 0L : System.currentTimeMillis() + ms;
            this.mutes.put(target.getUniqueId(), new Entry(targetName, until, why, sender.getName()));
            this.persist();
            moderation.log("MUTE " + targetName + " (" + target.getUniqueId() + ") durch " + sender.getName() + " [" + Durations.describe(ms) + "] Grund: " + why);
            moderation.notifyStaff(sender.getName() + " hat " + targetName + " stummgeschaltet (" + Durations.describe(ms) + "): " + why);
            Player online = target.getPlayer();
            if (online != null) {
                online.sendMessage(Messages.get("mute.target", Durations.describe(ms), why));
            }
            sender.sendMessage(Messages.get("mute.done", targetName, Durations.describe(ms), why));
        });
    }

    private void unmute(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Messages.get("mute.unmute-usage"));
            return;
        }
        this.plugin.moderation.resolve(sender, args[0], target -> {
            String targetName = Moderation.nameOf(target, args[0]);
            if (this.mutes.remove(target.getUniqueId()) == null) {
                sender.sendMessage(Messages.get("mute.not-muted", targetName));
                return;
            }
            this.persist();
            this.plugin.moderation.log("UNMUTE " + targetName + " (" + target.getUniqueId() + ") durch " + sender.getName());
            Player online = target.getPlayer();
            if (online != null) {
                online.sendMessage(Messages.get("mute.target-unmuted"));
            }
            sender.sendMessage(Messages.get("mute.unmuted", targetName));
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            if (command.getName().equalsIgnoreCase("unmute")) {
                for (Entry entry : this.mutes.values()) {
                    out.add(entry.name());
                }
            } else {
                for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                    out.add(online.getName());
                }
            }
        } else if (args.length == 2 && command.getName().equalsIgnoreCase("mute")) {
            out.addAll(List.of("10m", "1h", "12h", "1d", "7d"));
        }
        out.removeIf(entry -> !entry.toLowerCase(Locale.ROOT).startsWith(typed));
        return out;
    }
}
