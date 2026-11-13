package de.server.network;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Duell-Seasons: Jede Season dauert seasons.length-days Tage (Standard 30). Am Ende bekommen die besten Spieler Gems
 * (seasons.rewards, Standard 500/300/150, danach seasons.rewards-top10 fuer Platz 4-10), die Elo wird weich zurueckgesetzt
 * (seasons.elo-keep, Standard 0.5) und die Gewinner landen in der Ruhmeshalle (/season).
 */
final class Seasons implements CommandExecutor {
    static Seasons instance;

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    Seasons(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.file = new File(plugin.getDataFolder(), "seasons.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        if (!this.data.contains("number")) {
            this.data.set("number", 1);
            this.data.set("start", System.currentTimeMillis());
            this.save();
        }
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("Seasons", this::check), 20L * 30L, 20L * 60L * 5L);
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("seasons.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    int number() {
        return this.data.getInt("number", 1);
    }

    private long lengthMs() {
        return Math.max(1L, this.plugin.getConfig().getLong("seasons.length-days", 30L)) * 86400000L;
    }

    long endsAt() {
        return this.data.getLong("start", System.currentTimeMillis()) + this.lengthMs();
    }

    private void check() {
        try {
            if (System.currentTimeMillis() >= this.endsAt()) {
                this.rollover();
            }
        }
        catch (Throwable t) {
            Store.problem("Seasons:check", t);
        }
    }

    /** Beendet die laufende Season: Belohnungen, Ruhmeshalle, Elo-Reset. */
    void rollover() {
        int number = this.number();
        List<Object[]> leaders = DuelStats.eloLeaders(10);
        List<Integer> top = this.plugin.getConfig().getIntegerList("seasons.rewards");
        if (top.isEmpty()) {
            top = List.of(500, 300, 150);
        }
        int rest = this.plugin.getConfig().getInt("seasons.rewards-top10", 50);
        List<String> hall = new ArrayList<String>();
        for (int i = 0; i < leaders.size(); i++) {
            Object[] row = leaders.get(i);
            UUID id = (UUID)row[0];
            int reward = i < top.size() ? top.get(i) : rest;
            if (reward > 0) {
                Gems.give(id, reward);
            }
            if (i == 0) {
                Cosmetics.grantChampion(id);
            }
            if (i < 3) {
                hall.add((i + 1) + ". " + row[2] + " (" + row[1] + " Elo)");
            }
            Player online = this.plugin.getServer().getPlayer(id);
            if (online != null) {
                online.sendMessage("§d✦ §fSeason " + number + " §7beendet: Platz §f" + (i + 1) + " §7– §b+" + reward + " Gems§7!");
            }
        }
        if (Gems.instance != null) {
            Gems.instance.save();
        }
        this.data.set("history." + number, hall);
        this.data.set("number", number + 1);
        this.data.set("start", System.currentTimeMillis());
        this.save();
        DuelStats.softResetElo(this.plugin.getConfig().getDouble("seasons.elo-keep", 0.5));
        this.plugin.getServer().broadcastMessage("§d✦ §fSeason " + number + " ist vorbei! §7Season §f" + (number + 1) + " §7beginnt – die Elo wurde zurückgesetzt. §d/season");
        Webhook.send(this.plugin, "season", "🏆 Season " + number + " beendet. Sieger: " + hall);
        this.plugin.getLogger().info("Season " + number + " beendet, Gewinner: " + hall);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("end") && sender.hasPermission("duelnetwork.admin")) {
            this.rollover();
            sender.sendMessage(Messages.get("season.season-wurde-beendet"));
            return true;
        }
        long left = Math.max(0L, this.endsAt() - System.currentTimeMillis());
        long days = left / 86400000L;
        long hours = left / 3600000L % 24L;
        sender.sendMessage("§d✦ §fSeason " + this.number() + " §7– endet in §f" + days + "d " + hours + "h §8(" + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(this.endsAt())) + ")");
        List<String[]> top = DuelStats.top("elo", 10);
        sender.sendMessage(Messages.get("season.aktuelle-top-10-elo"));
        for (int i = 0; i < top.size(); i++) {
            sender.sendMessage(" §8" + (i + 1) + ". §f" + top.get(i)[0] + " §7– §d" + top.get(i)[1]);
        }
        if (top.isEmpty()) {
            sender.sendMessage(Messages.get("season.noch-keine-duelle-in"));
        }
        List<Integer> rewards = this.plugin.getConfig().getIntegerList("seasons.rewards");
        sender.sendMessage("§7Belohnung: §b" + (rewards.isEmpty() ? "500/300/150" : rewards.toString()) + " Gems §7für die Besten.");
        int last = this.number() - 1;
        if (last >= 1) {
            List<String> hall = this.data.getStringList("history." + last);
            if (!hall.isEmpty()) {
                sender.sendMessage("§7Season " + last + " – Sieger:");
                for (String line : hall) {
                    sender.sendMessage(" §6" + line);
                }
            }
        }
        return true;
    }
}
