package de.server.network;

import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * PlaceholderAPI-Erweiterung "duelnetwork". Verfuegbar (jeweils als %duelnetwork_...%):
 * money, money_formatted, money_rank, gems, wins, losses, elo, streak, rank, muted
 */
final class PapiHook extends PlaceholderExpansion {
    private final DuelNetworkPlugin plugin;

    PapiHook(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
    }

    public String getIdentifier() {
        return "duelnetwork";
    }

    public String getAuthor() {
        return "ServerTeam";
    }

    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    public boolean persist() {
        return true;
    }

    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return "";
        }
        UUID id = player.getUniqueId();
        switch (params.toLowerCase()) {
            case "money":
                return String.valueOf(Economy.balance(id));
            case "money_formatted":
                return Gui.money(Economy.balance(id));
            case "money_rank":
                return String.valueOf(Economy.rank(id));
            case "gems":
                return String.valueOf(Gems.instance == null ? 0 : Gems.balance(id));
            case "wins":
                return String.valueOf(DuelStats.stat(id, "wins", 0));
            case "losses":
                return String.valueOf(DuelStats.stat(id, "losses", 0));
            case "elo":
                return String.valueOf(DuelStats.stat(id, "elo", 1000));
            case "streak":
                return String.valueOf(DuelStats.stat(id, "streak", 0));
            case "rank":
                return this.plugin.rankOf(id);
            case "muted":
                return Mutes.instance != null && Mutes.instance.isMuted(id) ? "true" : "false";
            default:
                return null;
        }
    }
}
