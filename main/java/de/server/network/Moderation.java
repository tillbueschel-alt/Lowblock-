package de.server.network;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Ban/Kick/Unban/Modlog mit Dauer, Moderations-Log und ohne blockierende Namens-Abfragen im Haupt-Thread.
 * Nutzung: /ban &lt;Spieler&gt; [Dauer] &lt;Grund&gt;   (Dauer z. B. 30m, 12h, 7d, 2w - ohne Angabe dauerhaft)
 */
final class Moderation implements TabCompleter {
    static Moderation instance;

    private final DuelNetworkPlugin plugin;
    private final File logFile;

    Moderation(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        this.logFile = new File(plugin.getDataFolder(), "moderation.log");
        instance = this;
    }

    /** Gibt die Dauer in Millisekunden zurueck, oder -1 wenn der Text keine Dauer ist. */
    static long parseDuration(String token) {
        return Durations.parse(token);
    }

    static String describe(long ms) {
        return Durations.describe(ms);
    }

    /** Nur was sofort bekannt ist (online oder im Namens-Cache) - blockiert nie. */
    OfflinePlayer cached(String name) {
        Player online = this.plugin.getServer().getPlayerExact(name);
        if (online != null) {
            return online;
        }
        return this.plugin.getServer().getOfflinePlayerIfCached(name);
    }

    /** Loest einen Namen auf; bei unbekannten Spielern laeuft die (eventuell langsame) Suche in einem Hintergrund-Thread. */
    void resolve(CommandSender sender, String name, Consumer<OfflinePlayer> then) {
        OfflinePlayer known = this.cached(name);
        if (known != null) {
            then.accept(known);
            return;
        }
        sender.sendMessage(Messages.get("common.searching", name));
        this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            OfflinePlayer found = null;
            try {
                found = Bukkit.getOfflinePlayer(name);
            }
            catch (Throwable t) {
                this.plugin.getLogger().warning("Spieler-Suche fehlgeschlagen (" + name + "): " + t.getMessage());
            }
            final OfflinePlayer result = found;
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
                if (result == null) {
                    sender.sendMessage(Messages.get("common.player-not-found"));
                } else {
                    then.accept(result);
                }
            });
        });
    }

    void ban(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Messages.get("mod.ban.usage"));
            return;
        }
        long duration = 0L;
        int reasonFrom = 1;
        if (args.length >= 3) {
            long parsed = Durations.parse(args[1]);
            if (parsed > 0L) {
                duration = parsed;
                reasonFrom = 2;
            }
        }
        String reason = String.join(" ", Arrays.copyOfRange(args, reasonFrom, args.length)).trim();
        if (reason.isEmpty()) {
            sender.sendMessage(Messages.get("mod.ban.usage"));
            return;
        }
        final long ms = duration;
        final String why = reason;
        this.resolve(sender, args[0], target -> {
            if (this.plugin.isProtectedStaff(target)) {
                sender.sendMessage(Messages.get("mod.protected-ban"));
                return;
            }
            this.banPlayer(target, args[0], ms, why, sender.getName());
            sender.sendMessage(Messages.get("mod.ban.done", nameOf(target, args[0]), Durations.describe(ms), why));
        });
    }

    /** Wird auch vom Anti-Cheat benutzt (statt den Befehl als Konsole auszufuehren). */
    void banPlayer(OfflinePlayer target, String fallbackName, long durationMs, String reason, String source) {
        Instant until = durationMs <= 0L ? Instant.MAX : Instant.now().plusMillis(durationMs);
        target.ban(reason, until, source);
        Player online = target.getPlayer();
        if (online != null) {
            online.kickPlayer(Messages.get("mod.ban.kick-screen", Durations.describe(durationMs), reason));
        }
        this.log("BAN " + nameOf(target, fallbackName) + " (" + target.getUniqueId() + ") durch " + source + " [" + Durations.describe(durationMs) + "] Grund: " + reason);
        this.notifyStaff(source + " hat " + nameOf(target, fallbackName) + " gebannt (" + Durations.describe(durationMs) + "): " + reason);
    }

    void kick(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Messages.get("mod.kick.usage"));
            return;
        }
        Player target = this.plugin.getServer().getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(Messages.get("mod.kick.not-online"));
            return;
        }
        if (this.plugin.isProtectedStaff(target)) {
            sender.sendMessage(Messages.get("mod.protected-kick"));
            return;
        }
        String reason = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : Messages.get("mod.kick.default-reason");
        target.kickPlayer(Messages.get("mod.kick.screen", reason));
        this.log("KICK " + target.getName() + " (" + target.getUniqueId() + ") durch " + sender.getName() + " Grund: " + reason);
        sender.sendMessage(Messages.get("mod.kick.done", target.getName()));
    }

    void unban(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Messages.get("mod.unban.usage"));
            return;
        }
        this.resolve(sender, args[0], target -> {
            String name = nameOf(target, args[0]);
            // alte Namens-Liste und Profil-Liste (dort landet OfflinePlayer#ban) beide bereinigen
            Bukkit.getBanList(BanList.Type.NAME).pardon(name);
            try {
                this.plugin.getServer().dispatchCommand(this.plugin.getServer().getConsoleSender(), "minecraft:pardon " + name);
            }
            catch (Throwable t) {
                this.plugin.getLogger().warning("pardon fehlgeschlagen: " + t.getMessage());
            }
            this.log("UNBAN " + name + " (" + target.getUniqueId() + ") durch " + sender.getName());
            sender.sendMessage(Messages.get("mod.unban.done", name));
        });
    }

    /** /modlog &lt;Spieler&gt;: die letzten Eintraege (Bans, Kicks, Mutes ...) zu einem Spieler. Das Lesen passiert im Hintergrund. */
    void modlog(CommandSender sender, String[] args) {
        if (args.length != 1) {
            sender.sendMessage(Messages.get("mod.log.usage"));
            return;
        }
        final String needle = args[0].toLowerCase(Locale.ROOT);
        OfflinePlayer known = this.cached(args[0]);
        final String uuid = known == null ? "" : known.getUniqueId().toString();
        this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            List<String> hits = new ArrayList<String>();
            try {
                if (this.logFile.exists()) {
                    for (String line : Files.readAllLines(this.logFile.toPath(), StandardCharsets.UTF_8)) {
                        String lower = line.toLowerCase(Locale.ROOT);
                        if (lower.contains(" " + needle + " ") || (!uuid.isEmpty() && lower.contains(uuid))) {
                            hits.add(line);
                        }
                    }
                }
            }
            catch (IOException e) {
                Store.problem("Moderation:modlog", e);
            }
            final List<String> last = hits.size() > 10 ? hits.subList(hits.size() - 10, hits.size()) : hits;
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
                if (last.isEmpty()) {
                    sender.sendMessage(Messages.get("mod.log.none", args[0]));
                    return;
                }
                sender.sendMessage(Messages.get("mod.log.header", args[0], last.size()));
                for (String line : last) {
                    sender.sendMessage("§8» §7" + line);
                }
            });
        });
    }

    void notifyStaff(String message) {
        for (Player online : this.plugin.getServer().getOnlinePlayers()) {
            if (online.hasPermission("duelnetwork.admin")) {
                online.sendMessage(Messages.get("mod.staff-notice", message));
            }
        }
    }

    void log(String line) {
        Store.append(this.logFile, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date()) + " " + line);
    }

    static String nameOf(OfflinePlayer player, String fallback) {
        return player.getName() == null ? fallback : player.getName();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<String>();
        String typed = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            if (name.equals("unban")) {
                for (OfflinePlayer banned : this.plugin.getServer().getBannedPlayers()) {
                    if (banned.getName() != null) {
                        out.add(banned.getName());
                    }
                }
            } else {
                for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                    out.add(online.getName());
                }
            }
        } else if (args.length == 2 && (name.equals("ban") || name.equals("mute") || name.equals("timeout"))) {
            out.addAll(List.of("30m", "1h", "12h", "1d", "7d", "30d"));
        } else if (args.length == 2 && name.equals("rank")) {
            out.addAll(List.of("Media", "Owner", "Admin", "Premium", "Tester", "reset"));
        }
        out.removeIf(entry -> !entry.toLowerCase(Locale.ROOT).startsWith(typed));
        return out;
    }
}
