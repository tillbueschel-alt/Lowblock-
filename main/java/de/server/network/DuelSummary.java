package de.server.network;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/** Kurze Zusammenfassung nach einem 1v1-Duell: ausgeteilter und erhaltener Schaden (in Herzen) und Treffer. */
final class DuelSummary implements Listener, org.bukkit.command.CommandExecutor {
    private static DuelSummary instance;
    private final DuelNetworkPlugin plugin;
    private final Map<UUID, double[]> stats = new HashMap<UUID, double[]>(); // {dealt, taken, hits, bester Treffer, Startzeit}
    private final Map<UUID, java.util.List<String>> last = new HashMap<UUID, java.util.List<String>>();

    DuelSummary(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }
        if (attacker == null) {
            return;
        }
        UUID a = attacker.getUniqueId();
        UUID v = victim.getUniqueId();
        if (!this.plugin.activeDuelPlayers.contains(a) || !v.equals(this.plugin.duelPartners.get(a))) {
            return;
        }
        double dmg = event.getFinalDamage();
        double[] sa = this.stats.computeIfAbsent(a, k -> new double[5]);
        sa[0] += dmg;
        sa[2] += 1.0;
        sa[3] = Math.max(sa[3], dmg);
        double[] sv = this.stats.computeIfAbsent(v, k -> new double[5]);
        sv[1] += dmg;
        long now = System.currentTimeMillis();
        if (sa[4] == 0.0) sa[4] = now;
        if (sv[4] == 0.0) sv[4] = now;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.stats.remove(event.getPlayer().getUniqueId());
        this.last.remove(event.getPlayer().getUniqueId());
    }

    private static String hearts(double hp) {
        return String.format(Locale.ROOT, "%.1f", hp / 2.0) + " ❤";
    }

    private static String line(String name, double[] d) {
        return "§f" + name + " §8» §7Schaden §a" + hearts(d[0]) + " §8• §7kassiert §c" + hearts(d[1]) + " §8• §7Treffer §f" + (int)d[2] + " §8• §7stärkster Treffer §f" + hearts(d[3]);
    }

    /** Vor dem Aufraeumen des Duells aufrufen: zeigt die Statistik beiden Spielern und merkt sie fuer /matchstats. */
    static void show(Player winner, Player loser) {
        DuelSummary s = instance;
        if (s == null || winner == null || loser == null) {
            return;
        }
        double[] w = s.stats.remove(winner.getUniqueId());
        double[] l = s.stats.remove(loser.getUniqueId());
        w = w == null ? new double[5] : w;
        l = l == null ? new double[5] : l;
        String kit = s.plugin.duelKitLabel(winner.getUniqueId());
        Boolean ranked = s.plugin.queueRankedFlag(winner.getUniqueId());
        long start = (long)Math.max(w[4], l[4]) == 0L ? 0L : (long)Math.min(w[4] == 0.0 ? l[4] : w[4], l[4] == 0.0 ? w[4] : l[4]);
        long secs = start == 0L ? 0L : Math.max(0L, (System.currentTimeMillis() - start) / 1000L);
        java.util.List<String> out = new java.util.ArrayList<String>();
        out.add("§8§m                    §r §d§lDuell-Statistik §8§m                    ");
        out.add("§7Kit: §f" + (kit.isBlank() ? "-" : kit) + " §8• §7Modus: " + (Boolean.TRUE.equals(ranked) ? "§aRanked" : "§eUnranked") + " §8• §7Dauer: §f" + (secs / 60) + ":" + String.format("%02d", secs % 60));
        out.add("§a✔ " + winner.getName() + " §7hat gegen §c✘ " + loser.getName() + " §7gewonnen §8(§f" + hearts(winner.getHealth()) + " §7übrig§8)");
        out.add(line(winner.getName(), w));
        out.add(line(loser.getName(), l));
        s.last.put(winner.getUniqueId(), new java.util.ArrayList<String>(out));
        s.last.put(loser.getUniqueId(), new java.util.ArrayList<String>(out));
    }

    /** Haengt eine Zeile (z. B. Elo-Aenderung) an die gemerkte Statistik an und zeigt sie dem Spieler. */
    static void note(Player player, String text) {
        DuelSummary s = instance;
        if (s == null || player == null) {
            return;
        }
        s.last.computeIfAbsent(player.getUniqueId(), k -> new java.util.ArrayList<String>()).add(text);
        // nur merken; angezeigt wird die Statistik auf Klick
    }

    static void forget(UUID id) {
        DuelSummary s = instance;
        if (s != null) {
            s.stats.remove(id);
        }
    }

    /** Kurze Ergebnis-Zeile mit klickbarem [Statistiken]-Knopf; die volle Statistik erscheint erst nach dem Klick. */
    static void endNote(Player player, boolean won, String shortInfo) {
        if (instance == null || player == null) {
            return;
        }
        Chat.buttons(player, (won ? "§a§lDu hast gewonnen!" : "§c§lDu hast verloren.") + (shortInfo == null || shortInfo.isBlank() ? "" : " §8• " + shortInfo),
                new String[]{"§d§l[Statistiken]", "/duelnetwork:matchstats", "§7Klicken: Schaden, Treffer und Elo dieses Duells"});
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        java.util.List<String> lines = this.last.get(player.getUniqueId());
        if (lines == null || lines.isEmpty()) {
            player.sendMessage("§7Du hast noch kein Duell beendet. Nach jedem Duell siehst du hier die Statistik.");
            return true;
        }
        for (String text : lines) {
            player.sendMessage(text);
        }
        return true;
    }
}
