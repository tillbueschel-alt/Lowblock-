package de.server.network;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Prueft die config.yml beim Start und nach /duelnetwork reload: falsche Datentypen, unsinnige Zahlenwerte und unbekannte
 * Hauptabschnitte (Tippfehler). Es wird nur gemeldet, nichts veraendert; fehlende Werte nimmt das Plugin aus den Standardwerten.
 */
final class ConfigCheck {
    private ConfigCheck() {
    }

    /** {Schluessel, min, max} fuer Zahlen, die sonst Unsinn anrichten. */
    private static final Object[][] RANGES = new Object[][]{
        {"sleep.percent", 1, 100}, {"afk.minutes", 1, 1440}, {"backup.interval-hours", 1, 168}, {"duels.snapshot-blocks-per-tick", 100, 20000},
        {"backup.keep", 1, 200}, {"claims.max-per-player", 1, 1000}, {"webhook.status-minutes", 1, 1440}
    };

    static List<String> run(DuelNetworkPlugin plugin) {
        List<String> found = new ArrayList<String>();
        FileConfiguration config = plugin.getConfig();
        Configuration defaults = config.getDefaults();
        if (defaults != null) {
            for (String key : defaults.getKeys(true)) {
                Object def = defaults.get(key);
                Object val = config.get(key);
                if (val == null || def == null || config.isConfigurationSection(key)) {
                    continue;
                }
                boolean wrong = (def instanceof Number && !(val instanceof Number)) || (def instanceof Boolean && !(val instanceof Boolean));
                if (wrong) {
                    found.add("'" + key + "' hat den falschen Typ (erwartet: " + (def instanceof Boolean ? "true/false" : "Zahl") + ", steht: " + val + ")");
                }
            }
            List<String> missing = new ArrayList<String>();
            for (String key : defaults.getKeys(true)) {
                if (!defaults.isConfigurationSection(key) && !config.contains(key, true)) {
                    missing.add(key);
                }
            }
            if (!missing.isEmpty()) {
                found.add(missing.size() + " neue Einstellung(en) fehlen in deiner config.yml (Standardwerte gelten): " + String.join(", ", missing.subList(0, Math.min(5, missing.size()))) + (missing.size() > 5 ? " …" : ""));
            }
            for (String top : config.getKeys(false)) {
                if (!defaults.contains(top) && !defaults.isConfigurationSection(top)) {
                    found.add("Unbekannter Abschnitt '" + top + "' (Tippfehler?)");
                }
            }
        }
        for (Object[] r : RANGES) {
            String key = (String)r[0];
            if (!config.contains(key)) {
                continue;
            }
            double v = config.getDouble(key);
            if (v < ((Integer)r[1]).doubleValue() || v > ((Integer)r[2]).doubleValue()) {
                found.add("'" + key + "' = " + config.get(key) + " liegt außerhalb von " + r[1] + " bis " + r[2]);
            }
        }
        if (found.size() > 12) {
            int more = found.size() - 12;
            found = new ArrayList<String>(found.subList(0, 12));
            found.add("… und " + more + " weitere");
        }
        for (String f : found) {
            plugin.getLogger().warning("[config] " + f);
        }
        return found;
    }
}
