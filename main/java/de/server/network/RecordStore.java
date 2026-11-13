package de.server.network;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Parkour-Bestzeiten in einer eigenen Datei (records.yml) statt in der config.yml.
 * Beim ersten Start werden vorhandene Rekorde aus der config.yml uebernommen.
 */
final class RecordStore {
    /** Ein Eintrag der Bestenliste. */
    record Entry(String id, String name, long timeMs) { }

    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    RecordStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "records.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
        this.migrateFromConfig();
    }

    private void migrateFromConfig() {
        ConfigurationSection maps = this.plugin.getConfig().getConfigurationSection("parkour.maps");
        if (maps == null) {
            return;
        }
        boolean changed = false;
        for (String map : maps.getKeys(false)) {
            String old = "parkour.maps." + map + ".records";
            ConfigurationSection section = this.plugin.getConfig().getConfigurationSection(old);
            if (section == null) {
                continue;
            }
            for (String id : section.getKeys(false)) {
                long time = this.plugin.getConfig().getLong(old + "." + id + ".time-ms", Long.MAX_VALUE);
                if (time == Long.MAX_VALUE) {
                    continue;
                }
                String path = path(map, id);
                if (time < this.data.getLong(path + ".time-ms", Long.MAX_VALUE)) {
                    this.data.set(path + ".time-ms", time);
                    this.data.set(path + ".name", this.plugin.getConfig().getString(old + "." + id + ".name", id));
                }
            }
            this.plugin.getConfig().set(old, null);
            changed = true;
        }
        if (changed) {
            this.save();
            this.plugin.saveConfig();
            this.plugin.getLogger().info("Parkour-Rekorde wurden nach records.yml verschoben.");
        }
    }

    private static String path(String map, String id) {
        return "records." + map.toLowerCase() + "." + id;
    }

    long best(String map, UUID id) {
        return this.data.getLong(path(map, id.toString()) + ".time-ms", Long.MAX_VALUE);
    }

    /** Speichert den Namen immer und die Zeit nur, wenn sie besser ist. @return true bei neuer Bestzeit */
    boolean submit(String map, UUID id, String name, long timeMs) {
        String path = path(map, id.toString());
        this.data.set(path + ".name", name);
        boolean better = timeMs < this.data.getLong(path + ".time-ms", Long.MAX_VALUE);
        if (better) {
            this.data.set(path + ".time-ms", timeMs);
        }
        this.save();
        return better;
    }

    /** Alle Eintraege einer Map, schnellste zuerst. */
    List<Entry> top(String map) {
        List<Entry> out = new ArrayList<Entry>();
        ConfigurationSection section = this.data.getConfigurationSection("records." + map.toLowerCase());
        if (section != null) {
            for (String id : section.getKeys(false)) {
                long time = this.data.getLong(path(map, id) + ".time-ms", Long.MAX_VALUE);
                if (time == Long.MAX_VALUE) {
                    continue;
                }
                out.add(new Entry(id, this.data.getString(path(map, id) + ".name", id), time));
            }
        }
        out.sort(Comparator.comparingLong(Entry::timeMs));
        return out;
    }

    /** Ein Rekord eines Spielers auf einer Map (fuer die Admin-Verwaltung). */
    record Row(String map, String id, String name, long timeMs) { }

    private boolean matches(String id, String name, String who) {
        return id.equalsIgnoreCase(who) || name.equalsIgnoreCase(who);
    }

    /** Alle Rekorde eines Spielers (Name oder UUID) ueber alle Maps. */
    List<Row> find(String who) {
        List<Row> out = new ArrayList<Row>();
        ConfigurationSection maps = this.data.getConfigurationSection("records");
        if (maps == null) {
            return out;
        }
        for (String map : maps.getKeys(false)) {
            ConfigurationSection section = this.data.getConfigurationSection("records." + map);
            if (section == null) {
                continue;
            }
            for (String id : section.getKeys(false)) {
                String name = this.data.getString(path(map, id) + ".name", id);
                long time = this.data.getLong(path(map, id) + ".time-ms", Long.MAX_VALUE);
                if (time != Long.MAX_VALUE && this.matches(id, name, who)) {
                    out.add(new Row(map, id, name, time));
                }
            }
        }
        out.sort(Comparator.comparing(Row::map).thenComparingLong(Row::timeMs));
        return out;
    }

    /** Loescht die Rekorde eines Spielers auf einer Map (map == null: auf allen Maps). @return Anzahl geloeschter Rekorde */
    int remove(String who, String map) {
        int removed = 0;
        for (Row row : this.find(who)) {
            if (map != null && !row.map().equalsIgnoreCase(map)) {
                continue;
            }
            this.data.set(path(row.map(), row.id()), null);
            ++removed;
        }
        if (removed > 0) {
            this.save();
        }
        return removed;
    }

    void deleteMap(String map) {
        this.data.set("records." + map.toLowerCase(), null);
        this.save();
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("records.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }
}
