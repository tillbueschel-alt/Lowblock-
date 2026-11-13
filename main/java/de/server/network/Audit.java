package de.server.network;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Protokoll aller Geld- und Handelsvorgaenge (plugins/DuelNetwork/audit.log), damit man bei Streit oder
 * Verdacht auf Exploits nachvollziehen kann, wer wann was bezahlt oder gekauft hat.
 * Bei jedem Start wird eine zu grosse Datei (ueber 5 MB) archiviert.
 */
final class Audit {
    private static File file;

    private Audit() {
    }

    static void init(File dataFolder) {
        file = new File(dataFolder, "audit.log");
        try {
            if (file.exists() && file.length() > 5L * 1024L * 1024L) {
                String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
                File archive = new File(dataFolder, "audit-" + stamp + ".log");
                if (!file.renameTo(archive)) {
                    Store.problem("Audit:rotate", new java.io.IOException("Umbenennen fehlgeschlagen"));
                }
            }
        }
        catch (Throwable t) {
            Store.problem("Audit:init", t);
        }
    }

    static void log(String category, String message) {
        if (file == null) {
            return;
        }
        Store.append(file, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(new Date()) + " [" + category + "] " + message);
    }
}
