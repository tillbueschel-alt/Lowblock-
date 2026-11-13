package de.server.network;

import java.io.File;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;

/** /duelnetwork reload | info | backup | debug - Verwaltung fuer Admins (auch von der Konsole aus nutzbar). */
final class AdminCommand implements CommandExecutor, TabCompleter {
    private final DuelNetworkPlugin plugin;

    AdminCommand(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                this.plugin.reloadAll();
                this.plugin.configProblems = ConfigCheck.run(this.plugin);
                sender.sendMessage(Messages.get("admin.reloaded"));
                if (!this.plugin.configProblems.isEmpty()) {
                    sender.sendMessage("§e⚠ Config: " + this.plugin.configProblems.size() + " Hinweis(e):");
                    for (String hint : this.plugin.configProblems) {
                        sender.sendMessage("§7 - §f" + hint);
                    }
                }
            }
            case "info" -> this.info(sender);
            case "matches" -> this.matches(sender);
            case "maintenance" -> {
                boolean on = args.length >= 2 ? args[1].equalsIgnoreCase("on") : !this.plugin.maintenance;
                this.plugin.maintenance = on;
                sender.sendMessage("§e⚙ Wartungsmodus: " + (on ? "§aAN §7(neue Duelle gesperrt, laufende enden normal)" : "§cAUS"));
            }
            case "report" -> this.report(sender);
            case "backup" -> this.backup(sender);
            case "restore" -> this.restore(sender, args);
            case "debug" -> {
                boolean on = args.length >= 2 ? args[1].equalsIgnoreCase("on") : !Store.debugEnabled();
                Store.setDebug(on);
                sender.sendMessage(Messages.get("admin.debug", on ? "an" : "aus"));
            }
            default -> sender.sendMessage(Messages.get("admin.help"));
        }
        return true;
    }

    private void info(CommandSender sender) {
        long bytes = 0L;
        File[] files = this.plugin.getDataFolder().listFiles((d, n) -> n.endsWith(".yml"));
        if (files != null) {
            for (File f : files) {
                bytes += f.length();
            }
        }
        double tps = 20.0;
        try {
            double[] t = this.plugin.getServer().getTPS();
            if (t != null && t.length > 0) {
                tps = Math.min(20.0, t[0]);
            }
        }
        catch (Throwable t) {
            Store.problem("AdminCommand:tps", t);
        }
        sender.sendMessage("§dDuelNetwork §f" + this.plugin.getDescription().getVersion());
        sender.sendMessage("§7Spieler online: §f" + this.plugin.getServer().getOnlinePlayers().size() + " §8• §7TPS: §f" + String.format(Locale.ROOT, "%.1f", tps));
        sender.sendMessage("§7Konten: §f" + Economy.accounts() + " §8• §7Daten: §f" + (bytes / 1024L) + " KB");
        sender.sendMessage("§7Letztes Backup: §f" + (this.plugin.backups == null ? "aus" : this.plugin.backups.last()));
        int bau = 0;
        int smp = 0;
        for (org.bukkit.entity.Player p : this.plugin.getServer().getOnlinePlayers()) {
            if (this.plugin.inBauMode(p.getUniqueId())) {
                bau++;
            } else if (Zone.survival(this.plugin, p)) {
                smp++;
            }
        }
        StringBuilder worlds = new StringBuilder();
        for (org.bukkit.World w : this.plugin.getServer().getWorlds()) {
            worlds.append(worlds.length() == 0 ? "" : ", ").append(w.getName()).append(" (").append(w.getPlayers().size()).append(")");
        }
        sender.sendMessage("§7Modi: §fSMP " + smp + " §8• §fBau-SMP " + bau + " §8• §fDuelle " + this.plugin.activeDuelPlayers.size() + " §8• §fWelten: " + worlds);
        sender.sendMessage("§7Bau-SMP: §f" + (this.plugin.bauHasOwnBorder() ? "eigene Welt" : "geteilte Welt mit SMP (eigene Bau-Uhr)"));
        sender.sendMessage("§7Fehler seit Start: §f" + Store.problemCount + " §8• §7zuletzt: §f" + Store.lastProblem);
        java.util.List<String> cfg = this.plugin.configProblems;
        sender.sendMessage("§7Config: " + (cfg.isEmpty() ? "§aOK" : "§e" + cfg.size() + " Hinweis(e) – siehe Konsole [config]"));
        sender.sendMessage("§7Vault: §f" + (this.plugin.vaultActive ? "aktiv" : "nicht aktiv") + " §8• §7PlaceholderAPI: §f" + (this.plugin.papiActive ? "aktiv" : "nicht aktiv"));
    }

    /** Ueberblick fuer Admins: laufende Duelle, Warteschlangen, Event, Parkour (hilft, wenn etwas haengt). */
    private void matches(CommandSender sender) {
        sender.sendMessage("§dLaufende Aktivitäten");
        sender.sendMessage("§7Duelle: §f" + this.plugin.activeDuelPlayers.size() + " Spieler §8• §7Party-Matches: §f" + new java.util.HashSet<String>(this.plugin.activePartyMatches.values()).size() + " §8(" + this.plugin.activePartyMatches.size() + " Spieler)");
        int waiting = 0;
        StringBuilder queues = new StringBuilder();
        for (java.util.Map.Entry<String, java.util.List<java.util.UUID>> e : this.plugin.duelQueues.entrySet()) {
            if (!e.getValue().isEmpty()) {
                waiting += e.getValue().size();
                queues.append(queues.length() == 0 ? "" : ", ").append(e.getKey()).append(" ").append(e.getValue().size());
            }
        }
        sender.sendMessage("§7Warteschlange Einzel: §f" + waiting + (queues.length() == 0 ? "" : " §8(" + queues + ")") + " §8• §7Team: §f" + TeamQueue.waitingTotal());
        String ev = EventGames.describe();
        sender.sendMessage("§7Event: §f" + (ev.isEmpty() ? "keins" : ev) + " §8• §7Parkour-Läufe: §f" + this.plugin.activeParkourMaps.size());
        sender.sendMessage("§7Tipp: Hängt jemand fest, hilft §f/duelnetwork reload §7nicht – der Spieler kann sich neu einloggen und landet sicher in der Lobby.");
    }

    /** Schreibt fehlerbericht.txt (Version, Server, Config-Hinweise, letzte Fehler) zum Weiterschicken. */
    private void report(CommandSender sender) {
        StringBuilder sb = new StringBuilder();
        sb.append("DuelNetwork ").append(this.plugin.getDescription().getVersion()).append("\n");
        sb.append("Server: ").append(this.plugin.getServer().getVersion()).append("\n");
        sb.append("Java: ").append(System.getProperty("java.version")).append("\n");
        sb.append("Spieler online: ").append(this.plugin.getServer().getOnlinePlayers().size()).append("\n");
        sb.append("Bau-SMP: ").append(this.plugin.bauHasOwnBorder() ? "eigene Welt" : "geteilte Welt").append("\n");
        sb.append("Fehler seit Start: ").append(Store.problemCount).append("\n\nConfig-Hinweise:\n");
        if (this.plugin.configProblems.isEmpty()) {
            sb.append("  keine\n");
        }
        for (String c : this.plugin.configProblems) {
            sb.append("  - ").append(c).append("\n");
        }
        sb.append("\nLetzte Fehler:\n");
        synchronized (Store.RECENT) {
            if (Store.RECENT.isEmpty()) {
                sb.append("  keine\n");
            }
            for (String e : Store.RECENT) {
                sb.append(e).append("\n\n");
            }
        }
        try {
            java.nio.file.Files.writeString(new java.io.File(this.plugin.getDataFolder(), "fehlerbericht.txt").toPath(), sb.toString(), java.nio.charset.StandardCharsets.UTF_8);
            sender.sendMessage("§aFehlerbericht geschrieben: §fplugins/DuelNetwork/fehlerbericht.txt §7(Datei weiterschicken)");
        }
        catch (java.io.IOException e) {
            sender.sendMessage("§cBericht konnte nicht geschrieben werden: " + e.getMessage());
        }
    }

    private void backup(CommandSender sender) {
        if (this.plugin.backups == null) {
            sender.sendMessage(Messages.get("admin.backup-failed", "Backups sind ausgeschaltet (backup.enabled)."));
            return;
        }
        sender.sendMessage(Messages.get("admin.backup-started"));
        this.plugin.flushConfig();
        this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            String result;
            boolean ok = true;
            try {
                java.io.File made = this.plugin.backups.createNow();
                result = made == null ? "nichts zu sichern" : made.getName();
            }
            catch (Throwable t) {
                ok = false;
                result = String.valueOf(t.getMessage());
            }
            final String text = result;
            final boolean success = ok;
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () ->
                sender.sendMessage(success ? Messages.get("admin.backup-done", text) : Messages.get("admin.backup-failed", text)));
        });
    }

    private void restore(CommandSender sender, String[] args) {
        if (this.plugin.backups == null) {
            sender.sendMessage("§cBackups sind ausgeschaltet oder nicht initialisiert.");
            return;
        }
        if (args.length == 1 || args[1].equalsIgnoreCase("list")) {
            List<File> backups = this.plugin.backups.listBackups();
            if (backups.isEmpty()) {
                sender.sendMessage("§eKeine Backups gefunden. Erstelle zuerst eins mit /duelnetwork backup.");
                return;
            }
            sender.sendMessage("§dBackups (neueste zuerst):");
            for (int i = 0; i < Math.min(10, backups.size()); i++) sender.sendMessage("§7• §f" + backups.get(i).getName());
            sender.sendMessage("§8Restore: /duelnetwork restore <Dateiname> confirm — Anwendung beim nächsten Serverstart.");
            return;
        }
        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            sender.sendMessage("§eSicherheitsabfrage: /duelnetwork restore " + args[1] + " confirm");
            sender.sendMessage("§7Das Backup wird geprüft, ein Rückfall-Backup erstellt und erst beim nächsten Start angewendet.");
            return;
        }
        sender.sendMessage("§7Backup und Sicherheitskopie werden im Hintergrund geprüft/erstellt ...");
        this.plugin.flushConfig();
        this.plugin.getServer().getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            String result;
            try {
                File safety = this.plugin.backups.queueRestore(args[1]);
                result = "§aRestore vorgemerkt. Beim nächsten Start wird das Backup angewendet. Sicherheitskopie: §f" + safety.getName();
            } catch (Exception e) {
                result = "§cRestore nicht vorgemerkt: " + e.getMessage();
            }
            String message = result;
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> sender.sendMessage(message));
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "info", "matches", "maintenance", "report", "backup", "restore", "debug").stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("restore") && this.plugin.backups != null) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return this.plugin.backups.listBackups().stream().map(File::getName).filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("restore")) return List.of("confirm");
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            return List.of("on", "off");
        }
        return List.of();
    }
}
