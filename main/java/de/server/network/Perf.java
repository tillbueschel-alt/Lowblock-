package de.server.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Misst, wie lange die Timer des Plugins im Hauptthread brauchen. Ohne Profiler sieht man sonst nicht,
 * ob das Plugin ueberhaupt Last verursacht. Anzeige: /dnwartung perf. Wer laenger als perf.warn-ms braucht, wird
 * (hoechstens einmal pro Minute und Task) im Log gemeldet.
 */
final class Perf {
    private static final class Stat {
        long runs;
        long totalNs;
        long maxNs;
        long lastWarn;
    }

    private static final Map<String, Stat> STATS = new ConcurrentHashMap<String, Stat>();

    private Perf() { }

    /** Fuehrt den Task aus und misst ihn. Exceptions werden nicht verschluckt. */
    static void time(String name, Runnable task) {
        long start = System.nanoTime();
        try {
            task.run();
        }
        finally {
            long took = System.nanoTime() - start;
            Stat stat = STATS.computeIfAbsent(name, k -> new Stat());
            synchronized (stat) {
                stat.runs++;
                stat.totalNs += took;
                if (took > stat.maxNs) {
                    stat.maxNs = took;
                }
                DuelNetworkPlugin plugin = DuelNetworkPlugin.getPlugin(DuelNetworkPlugin.class);
                long warnMs = plugin == null ? 50L : plugin.getConfig().getLong("perf.warn-ms", 40L);
                if (warnMs > 0L && took / 1000000L >= warnMs && System.currentTimeMillis() - stat.lastWarn > 60000L) {
                    stat.lastWarn = System.currentTimeMillis();
                    if (plugin != null) {
                        plugin.getLogger().warning("Langsamer Task '" + name + "': " + (took / 1000000L) + " ms (ein Tick hat 50 ms)");
                    }
                }
            }
        }
    }

    static Runnable wrap(String name, Runnable task) {
        return () -> time(name, task);
    }

    /** Zeilen fuer /dnwartung perf: die langsamsten Tasks nach Durchschnitt. */
    static List<String> report(int limit) {
        List<Map.Entry<String, Stat>> rows = new ArrayList<Map.Entry<String, Stat>>(STATS.entrySet());
        rows.sort((x, y) -> Double.compare(avg(y.getValue()), avg(x.getValue())));
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < Math.min(limit, rows.size()); i++) {
            Stat stat = rows.get(i).getValue();
            synchronized (stat) {
                out.add("§8▪ §f" + rows.get(i).getKey() + " §7Ø §f" + String.format(java.util.Locale.ROOT, "%.2f", avg(stat)) + " ms §7max §f"
                        + String.format(java.util.Locale.ROOT, "%.1f", stat.maxNs / 1000000.0) + " ms §8(" + stat.runs + "x)");
            }
        }
        return out;
    }

    private static double avg(Stat stat) {
        return stat.runs == 0L ? 0.0 : stat.totalNs / 1000000.0 / stat.runs;
    }
}
