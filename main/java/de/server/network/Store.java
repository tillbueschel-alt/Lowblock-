package de.server.network;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Zentrale Dateispeicherung.
 * <p>
 * Die YAML-Daten werden im Haupt-Thread nur in einen String umgewandelt (schnell, konsistenter Zustand),
 * das eigentliche Schreiben auf die Platte passiert in einem eigenen Thread. Geschrieben wird atomar
 * (temporaere Datei, dann verschieben), damit bei einem Absturz keine halbe Datei zurueckbleibt.
 * Alle Schreibvorgaenge laufen ueber einen einzigen Thread und behalten so ihre Reihenfolge.
 */
final class Store {
    private static ExecutorService io;
    private static Logger log;
    private static volatile boolean closed = true;
    /** true = ausfuehrliche Fehlerausgabe mit Stacktrace (config: debug: true) */
    static volatile boolean verbose;
    /** Anzahl unerwarteter Fehler seit dem Start (fuer /duelnetwork info). */
    static volatile int problemCount;
    /** Die letzten 30 Fehler mit Kurz-Stacktrace (fuer /duelnetwork report). */
    static final java.util.List<String> RECENT = new java.util.ArrayList<String>();
    static volatile String lastProblem = "keiner";

    private Store() {
    }

    static synchronized void init(Logger logger) {
        log = logger;
        io = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "DuelNetwork-IO");
            t.setDaemon(true);
            return t;
        });
        closed = false;
    }

    /** Wartet (hoechstens 10 s), bis alle bisher angeforderten Schreibvorgaenge auf der Platte sind. */
    static void flush() {
        flushSuccessfully();
    }

    /** Wie flush(), aber false, wenn der IO-Thread in 10 Sekunden nicht bestaetigt hat. */
    static boolean flushSuccessfully() {
        ExecutorService exec;
        synchronized (Store.class) {
            exec = closed ? null : io;
        }
        if (exec == null) {
            return true;
        }
        try {
            exec.submit(() -> { }).get(10L, TimeUnit.SECONDS);
            return true;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            if (log != null) {
                log.warning("Warten auf ausstehende Schreibvorgaenge fehlgeschlagen: " + e);
            }
            return false;
        }
    }

    /** Wartet, bis alle ausstehenden Schreibvorgaenge fertig sind. Danach wird synchron geschrieben. */
    static synchronized void shutdown() {
        closed = true;
        if (io == null) {
            return;
        }
        io.shutdown();
        try {
            if (!io.awaitTermination(15L, TimeUnit.SECONDS) && log != null) {
                log.warning("Speichern dauert ungewoehnlich lange, einige Daten wurden eventuell nicht geschrieben.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        io = null;
    }

    /**
     * Speichert die Konfiguration asynchron. Die Signatur deklariert IOException, damit bestehende
     * try/catch-Bloecke unveraendert bleiben koennen.
     */
    static void save(FileConfiguration yaml, File file) throws IOException {
        String text = yaml.saveToString();
        ExecutorService exec;
        synchronized (Store.class) {
            exec = closed ? null : io;
        }
        if (exec == null) {
            write(file, text);
            return;
        }
        exec.execute(() -> {
            try {
                write(file, text);
            } catch (IOException e) {
                if (log != null) {
                    log.warning(file.getName() + " konnte nicht gespeichert werden: " + e.getMessage());
                }
            }
        });
    }

    /** Haengt eine Zeile an eine Textdatei an (asynchron), z. B. fuer das Moderations-Log. */
    static void append(File file, String line) {
        ExecutorService exec;
        synchronized (Store.class) {
            exec = closed ? null : io;
        }
        Runnable job = () -> {
            try {
                File parent = file.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                Files.writeString(file.toPath(), line + System.lineSeparator(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException e) {
                if (log != null) {
                    log.warning(file.getName() + " konnte nicht beschrieben werden: " + e.getMessage());
                }
            }
        };
        if (exec == null) {
            job.run();
        } else {
            exec.execute(job);
        }
    }

    private static void write(File file, String text) throws IOException {
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        Path target = file.toPath();
        Path tmp = target.resolveSibling(file.getName() + ".tmp");
        Files.writeString(tmp, text, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Fuer Stellen, an denen Fehler nicht stillschweigend verschwinden sollen. */
    static void debug(Logger logger, String where, Throwable t) {
        if (logger != null && t != null) {
            logger.log(java.util.logging.Level.FINE, where + ": " + t, t);
        }
    }

    private static final java.util.Map<String, Long> SEEN = new java.util.concurrent.ConcurrentHashMap<String, Long>();

    /**
     * Meldet einen unerwarteten Fehler, der frueher stillschweigend verschluckt wurde. Pro Stelle und
     * Fehlerart hoechstens einmal pro Minute, damit die Konsole auch bei wiederholten Fehlern lesbar bleibt.
     */
    static void problem(String where, Throwable t) {
        if (log == null || t == null) {
            return;
        }
        problemCount++;
        synchronized (RECENT) {
            StringBuilder sb = new StringBuilder(new java.text.SimpleDateFormat("dd.MM. HH:mm:ss").format(new java.util.Date())).append("  ").append(where).append("\n  ").append(t);
            StackTraceElement[] st = t.getStackTrace();
            for (int i = 0; i < Math.min(6, st.length); i++) {
                sb.append("\n    at ").append(st[i]);
            }
            RECENT.add(sb.toString());
            while (RECENT.size() > 30) {
                RECENT.remove(0);
            }
        }
        lastProblem = where + ": " + t.getClass().getSimpleName();
        long now = System.currentTimeMillis();
        if (SEEN.size() > 500) {
            SEEN.clear();
        }
        String key = where + "|" + t.getClass().getName();
        Long last = SEEN.get(key);
        if (last != null && now - last < 60_000L) {
            return;
        }
        SEEN.put(key, now);
        if (verbose) {
            log.log(java.util.logging.Level.WARNING, "Unerwarteter Fehler bei " + where + ": " + t, t);
        } else {
            log.warning("Unerwarteter Fehler bei " + where + ": " + t + " (Details mit debug: true in der config.yml)");
        }
    }

    static boolean debugEnabled() {
        return verbose;
    }

    static void setDebug(boolean on) {
        verbose = on;
    }

    /** Ausgabe nur bei aktiviertem Debug-Modus (config: debug oder /duelnetwork debug on). */
    static void debug(String message) {
        if (verbose && log != null) {
            log.info("[debug] " + message);
        }
    }
}
