package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Kleine Komfort-Funktionen:
 * /tpatoggle - Teleport-Anfragen (/tpa, /tpahere) ein- oder ausschalten,
 * /pc &lt;Text&gt; - Party-Chat, /fc &lt;Text&gt; - Chat an alle Online-Freunde,
 * Duell-Zusammenfassung (Dauer, Treffer, Schaden) nach jedem gewerteten Duell (config duels.summary).
 */
final class Social implements CommandExecutor, Listener {
    static Social instance;

    private static final class Fight {
        long start = System.currentTimeMillis();
        int hits;
        double damage;
    }

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private final Set<UUID> tpOff = new HashSet<UUID>();
    private final Map<UUID, Fight> fights = new HashMap<UUID, Fight>();

    Social(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        this.file = new File(plugin.getDataFolder(), "social.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        for (String raw : this.data.getStringList("tp-off")) {
            try {
                this.tpOff.add(UUID.fromString(raw));
            }
            catch (IllegalArgumentException ignored) {
                // kaputter Eintrag
            }
        }
    }

    private void save() {
        java.util.List<String> raw = new java.util.ArrayList<String>();
        for (UUID id : this.tpOff) {
            raw.add(id.toString());
        }
        this.data.set("tp-off", raw);
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("social.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    /** true, wenn der Spieler keine Teleport-Anfragen annehmen will. */
    static boolean tpOff(UUID id) {
        return instance != null && instance.tpOff.contains(id);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        switch (command.getName().toLowerCase(java.util.Locale.ROOT)) {
            case "tpatoggle" -> {
                if (this.tpOff.remove(player.getUniqueId())) {
                    player.sendMessage("§7Teleport-Anfragen: §aan");
                } else {
                    this.tpOff.add(player.getUniqueId());
                    player.sendMessage("§7Teleport-Anfragen: §caus §8(§f/tpatoggle §8zum Einschalten)");
                }
                this.save();
            }
            case "pc" -> {
                if (args.length == 0) {
                    player.sendMessage("§c/pc <Nachricht>");
                    return true;
                }
                Set<UUID> members = this.plugin.partyMembersOf(player.getUniqueId());
                if (members.isEmpty()) {
                    player.sendMessage("§cDu bist in keiner Party.");
                    return true;
                }
                if (this.muted(player)) {
                    return true;
                }
                String line = "§5[Party] §f" + player.getName() + "§8: §7" + String.join(" ", args);
                for (UUID id : members) {
                    Player member = this.plugin.getServer().getPlayer(id);
                    if (member != null) {
                        member.sendMessage(line);
                    }
                }
            }
            case "fc" -> {
                if (args.length == 0) {
                    player.sendMessage("§c/fc <Nachricht>");
                    return true;
                }
                if (this.muted(player)) {
                    return true;
                }
                String line = "§d[Freunde] §f" + player.getName() + "§8: §7" + String.join(" ", args);
                player.sendMessage(line);
                int reached = 0;
                if (Friends.instance != null) {
                    for (Player friend : Friends.instance.onlineFriends(player.getUniqueId())) {
                        friend.sendMessage(line);
                        reached++;
                    }
                }
                if (reached == 0) {
                    player.sendMessage("§8Gerade ist kein Freund online.");
                }
            }
            default -> { }
        }
        return true;
    }

    private boolean muted(Player player) {
        if (this.plugin.mutes != null && this.plugin.mutes.isMuted(player.getUniqueId())) {
            player.sendMessage("§cDu bist stummgeschaltet.");
            return true;
        }
        return false;
    }

    /* ---------- Duell-Zusammenfassung ---------- */

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !this.plugin.getConfig().getBoolean("duels.summary", true)) {
            return;
        }
        Player attacker = event.getDamager() instanceof Player p ? p
                : event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player shooter ? shooter : null;
        if (attacker == null || attacker.equals(victim) || !this.plugin.inActiveMatch(attacker.getUniqueId()) || !this.plugin.inActiveMatch(victim.getUniqueId())) {
            return;
        }
        Fight fight = this.fights.computeIfAbsent(attacker.getUniqueId(), k -> new Fight());
        fight.hits++;
        fight.damage += event.getFinalDamage();
        this.fights.computeIfAbsent(victim.getUniqueId(), k -> new Fight());
    }

    /** Wird vom Duell-Ergebnis aufgerufen: schickt beiden die Zusammenfassung und setzt die Zaehler zurueck. */
    static void summary(Player winner, Player loser) {
        if (instance == null || winner == null || loser == null) {
            return;
        }
        try {
            instance.show(winner, loser);
        }
        catch (Throwable t) {
            Store.problem("Social:summary", t);
        }
    }

    private void show(Player winner, Player loser) {
        Fight w = this.fights.remove(winner.getUniqueId());
        Fight l = this.fights.remove(loser.getUniqueId());
        if ((w == null && l == null) || !this.plugin.getConfig().getBoolean("duels.summary", true)) {
            return;
        }
        long start = Math.min(w == null ? Long.MAX_VALUE : w.start, l == null ? Long.MAX_VALUE : l.start);
        long seconds = Math.max(1L, (System.currentTimeMillis() - start) / 1000L);
        String time = seconds / 60L + ":" + String.format("%02d", seconds % 60L);
        for (Player viewer : new Player[]{winner, loser}) {
            Fight mine = viewer.equals(winner) ? w : l;
            Fight theirs = viewer.equals(winner) ? l : w;
            viewer.sendMessage("§8§m        §r §d⚔ Duell-Zusammenfassung §8§m        ");
            viewer.sendMessage(" §7Dauer: §f" + time);
            viewer.sendMessage(" §7Du: §f" + (mine == null ? 0 : mine.hits) + " Treffer §8• §f" + String.format(java.util.Locale.ROOT, "%.1f", (mine == null ? 0.0 : mine.damage) / 2.0) + " ♥ Schaden");
            viewer.sendMessage(" §7Gegner: §f" + (theirs == null ? 0 : theirs.hits) + " Treffer §8• §f" + String.format(java.util.Locale.ROOT, "%.1f", (theirs == null ? 0.0 : theirs.damage) / 2.0) + " ♥ Schaden");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.fights.remove(event.getPlayer().getUniqueId());
    }
}
