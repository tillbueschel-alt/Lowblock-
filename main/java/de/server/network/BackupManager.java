package de.server.network;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/** Rotierende Daten-Backups mit SQLite-Snapshots und sicherer Wiederherstellung beim naechsten Start. */
final class BackupManager {
    private static final String MANIFEST = "backup-manifest.properties";
    private static final String RESTORE_MARKER = "restore-pending.txt";
    private static final long MAX_ARCHIVE_BYTES = 4L * 1024L * 1024L * 1024L;
    private static final long MAX_ENTRY_BYTES = 2L * 1024L * 1024L * 1024L;
    private static final int MAX_ENTRIES = 256;

    private final DuelNetworkPlugin plugin;
    private final File dir;
    private volatile String last = "noch keins";

    String last() {
        return this.last;
    }

    BackupManager(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "backups");
    }

    void start() {
        if (!this.plugin.getConfig().getBoolean("backup.enabled", true)) return;
        long hours = Math.max(1L, this.plugin.getConfig().getLong("backup.interval-hours", 6L));
        long ticks = hours * 60L * 60L * 20L;
        this.plugin.getServer().getScheduler().runTaskTimerAsynchronously((Plugin)this.plugin, this::backup, 20L * 30L, ticks);
    }

    private void backup() {
        try {
            File made = this.createNow();
            if (made != null) this.plugin.getLogger().info("Backup erstellt: " + made.getName());
        } catch (IOException | RuntimeException e) {
            Store.problem("Backup", e);
        }
    }

    /** Erstellt ein Backup. Nur in einem asynchronen Task aufrufen. */
    synchronized File createNow() throws IOException {
        return this.createNow(null);
    }

    private synchronized File createNow(String preserveName) throws IOException {
        if (!Store.flushSuccessfully()) throw new IOException("Ausstehende Schreibvorgaenge konnten nicht bestaetigt werden; Backup abgebrochen.");
        File data = this.plugin.getDataFolder();
        File[] yamlFiles = data.listFiles((d, name) -> name.endsWith(".yml"));
        if (yamlFiles == null || yamlFiles.length == 0) return null;
        Arrays.sort(yamlFiles, Comparator.comparing(File::getName));
        this.dir.mkdirs();
        try {
            Files.setPosixFilePermissions(this.dir.toPath(), java.nio.file.attribute.PosixFilePermissions.fromString("rwx------"));
        } catch (UnsupportedOperationException ignored) {
            // Dateisystem ohne POSIX-Rechte; der Backup-Ordner bleibt unter dem Plugin-Datenordner.
        }
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).format(new Date());
        Path staging = Files.createTempDirectory(this.dir.toPath(), "snapshot-");
        Path tmpZip = this.dir.toPath().resolve("backup-" + stamp + "-" + UUID.randomUUID() + ".zip.tmp");
        File target = this.dir.toPath().resolve("backup-" + stamp + ".zip").toFile();
        try {
            List<File> files = new ArrayList<File>(List.of(yamlFiles));
            Set<String> names = new HashSet<String>();
            for (File file : files) names.add(file.getName());
            for (File db : this.databaseFiles()) {
                if (!db.isFile()) continue;
                if (!names.add(db.getName())) throw new IOException("Doppelter Backup-Dateiname: " + db.getName());
                Path snapshot = staging.resolve(db.getName());
                this.snapshotSqlite(db, snapshot.toFile());
                files.add(snapshot.toFile());
            }
            files.sort(Comparator.comparing(File::getName));
            StringBuilder manifest = new StringBuilder("created=").append(System.currentTimeMillis()).append('\n');
            try (OutputStream out = Files.newOutputStream(tmpZip); ZipOutputStream zip = new ZipOutputStream(out)) {
                zip.setLevel(Deflater.BEST_SPEED);
                byte[] buffer = new byte[64 * 1024];
                for (File file : files) {
                    MessageDigest digest = sha256();
                    long size = 0L;
                    zip.putNextEntry(new ZipEntry(file.getName()));
                    try (InputStream in = Files.newInputStream(file.toPath())) {
                        int n;
                        while ((n = in.read(buffer)) >= 0) {
                            if (n == 0) continue;
                            zip.write(buffer, 0, n);
                            digest.update(buffer, 0, n);
                            size += n;
                        }
                    }
                    zip.closeEntry();
                    manifest.append("file=").append(file.getName()).append('|').append(size).append('|').append(hex(digest.digest())).append('\n');
                }
                zip.putNextEntry(new ZipEntry(MANIFEST));
                zip.write(manifest.toString().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
            try {
                Files.move(tmpZip, target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmpZip, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            this.last = target.getName();
            this.prune(preserveName);
            return target;
        } finally {
            Files.deleteIfExists(tmpZip);
            deleteTree(staging);
        }
    }

    private List<File> databaseFiles() throws IOException {
        Path base = this.plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        List<File> result = new ArrayList<File>();
        result.add(base.resolve("economy.db").toFile());
        String configured = this.plugin.getConfig().getString("claims.database", "claims.db");
        if (configured != null && !configured.isBlank()) {
            Path raw = Path.of(configured);
            Path claim = (raw.isAbsolute() ? raw : base.resolve(raw)).toAbsolutePath().normalize();
            if (!claim.startsWith(base)) throw new IOException("claims.database muss fuer Backups innerhalb des DuelNetwork-Datenordners liegen.");
            if (!result.contains(claim.toFile())) result.add(claim.toFile());
        }
        return result;
    }

    /** VACUUM INTO erzeugt einen konsistenten SQLite-Snapshot inklusive WAL-Daten, ohne rohe WAL-Kopien. */
    private void snapshotSqlite(File source, File destination) throws IOException {
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + source.getAbsolutePath());
                 Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA busy_timeout=10000");
                String escaped = destination.getAbsolutePath().replace("'", "''");
                statement.execute("VACUUM INTO '" + escaped + "'");
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new IOException("SQLite-Snapshot fuer " + source.getName() + " fehlgeschlagen", e);
        }
    }

    private void prune(String preserveName) {
        int keep = Math.max(1, this.plugin.getConfig().getInt("backup.keep", 10));
        File[] old = this.dir.listFiles((d, name) -> name.startsWith("backup-") && name.endsWith(".zip"));
        if (old == null || old.length <= keep) return;
        Arrays.sort(old, Comparator.comparing(File::getName));
        int remaining = old.length;
        for (File file : old) {
            if (remaining <= keep) break;
            if (file.getName().equals(preserveName)) continue;
            if (file.delete()) remaining--;
            else this.plugin.getLogger().warning("Altes Backup konnte nicht geloescht werden: " + file.getName());
        }
    }

    List<File> listBackups() {
        File[] files = this.dir.listFiles((d, name) -> name.startsWith("backup-") && name.endsWith(".zip"));
        if (files == null) return List.of();
        Arrays.sort(files, Comparator.comparing(File::getName).reversed());
        return List.of(files);
    }

    /** Validiert ZIP-Pfade, YAML-Inhalt und SQLite-Integritaet; laeuft im Hintergrund. */
    static List<String> validateArchive(File archive, File tempDir) {
        List<String> problems = new ArrayList<String>();
        Set<String> names = new HashSet<String>();
        long total = 0L;
        int dataFiles = 0;
        try (ZipFile zip = new ZipFile(archive)) {
            if (zip.size() == 0 || zip.size() > MAX_ENTRIES) return List.of("Ungueltige Anzahl an ZIP-Eintraegen.");
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (entry.isDirectory() || name.isBlank() || name.contains("/") || name.contains("\\") || name.equals(".") || name.equals("..")) {
                    problems.add("Unsicherer ZIP-Eintrag: " + name);
                    continue;
                }
                if (!names.add(name)) {
                    problems.add("Doppelter ZIP-Eintrag: " + name);
                    continue;
                }
                boolean yaml = name.endsWith(".yml");
                boolean sqlite = isSqliteName(name);
                if (name.equals(MANIFEST)) continue;
                if (!yaml && !sqlite) {
                    problems.add("Nicht erlaubter ZIP-Eintrag: " + name);
                    continue;
                }
                dataFiles++;
                if (entry.getSize() > MAX_ENTRY_BYTES || entry.getSize() < -1L || (entry.getSize() >= 0 && total + entry.getSize() > MAX_ARCHIVE_BYTES)) {
                    problems.add("ZIP-Groesse unzulaessig bei " + name);
                    continue;
                }
                tempDir.mkdirs();
                Path temp = Files.createTempFile(tempDir.toPath(), "dn-verify-", yaml ? ".yml" : ".db");
                try {
                    long allowance = Math.min(MAX_ENTRY_BYTES, MAX_ARCHIVE_BYTES - total);
                    if (allowance <= 0L) throw new IOException("Gesamtgroesse des Backups ueberschreitet das Sicherheitslimit");
                    try (InputStream in = zip.getInputStream(entry); OutputStream out = Files.newOutputStream(temp)) {
                        total += copyLimited(in, out, allowance);
                    }
                    if (yaml) {
                        if (Files.size(temp) > 32L * 1024L * 1024L) throw new IOException("YAML-Datei groesser als 32 MiB");
                        YamlConfiguration config = new YamlConfiguration();
                        config.loadFromString(Files.readString(temp, StandardCharsets.UTF_8));
                    } else {
                        verifySqlite(temp);
                    }
                } catch (Exception e) {
                    problems.add(name + " ist beschaedigt: " + e.getMessage());
                } finally {
                    Files.deleteIfExists(temp);
                }
            }
        } catch (IOException e) {
            problems.add("ZIP kann nicht geoeffnet werden: " + e.getMessage());
        }
        if (dataFiles == 0) problems.add("Backup enthaelt keine wiederherstellbaren YAML-/SQLite-Dateien.");
        return problems;
    }

    private static void verifySqlite(Path file) throws IOException {
        try {
            Class.forName("org.sqlite.JDBC");
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
                 Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("PRAGMA integrity_check")) {
                if (!result.next() || !"ok".equalsIgnoreCase(result.getString(1))) throw new IOException("SQLite integrity_check meldet einen Fehler");
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new IOException("SQLite-Pruefung fehlgeschlagen: " + e.getMessage(), e);
        }
    }

    /** Prueft das Zielarchiv und erstellt zuerst ein frisches Rueckfall-Backup; Anwendung erst beim naechsten Start. */
    synchronized File queueRestore(String backupName) throws IOException {
        if (new File(this.dir, RESTORE_MARKER).exists()) throw new IOException("Es ist bereits eine Wiederherstellung fuer den naechsten Start vorgemerkt.");
        File selected = this.resolveBackup(backupName);
        List<String> problems = validateArchive(selected, this.dir);
        if (!problems.isEmpty()) throw new IOException(String.join("; ", problems));
        File safety = this.createNow(selected.getName());
        if (safety == null) throw new IOException("Vor dem Restore konnte kein Sicherheits-Backup erstellt werden.");
        Path marker = new File(this.dir, RESTORE_MARKER).toPath();
        Path tmp = marker.resolveSibling(RESTORE_MARKER + ".tmp");
        Files.writeString(tmp, selected.getName() + System.lineSeparator() + safety.getName() + System.lineSeparator(), StandardCharsets.UTF_8);
        try {
            Files.move(tmp, marker, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, marker, StandardCopyOption.REPLACE_EXISTING);
        }
        return safety;
    }

    private File resolveBackup(String name) throws IOException {
        if (name == null || !name.matches("backup-[A-Za-z0-9._-]+\\.zip")) throw new IOException("Ungueltiger Backup-Dateiname.");
        File candidate = new File(this.dir, name).getCanonicalFile();
        if (!candidate.getParentFile().equals(this.dir.getCanonicalFile()) || !candidate.isFile()) throw new IOException("Backup wurde nicht gefunden: " + name);
        return candidate;
    }

    /** Wird als erste Aktion in onEnable aufgerufen und wendet einen bestaetigten Restore offline an. */
    static void applyPendingRestore(DuelNetworkPlugin plugin) {
        new BackupManager(plugin).applyPendingRestore();
    }

    private void applyPendingRestore() {
        File marker = new File(this.dir, RESTORE_MARKER);
        if (!marker.isFile()) return;
        Path staging = null;
        try {
            List<String> lines = Files.readAllLines(marker.toPath(), StandardCharsets.UTF_8);
            if (lines.size() < 2) throw new IOException("Restore-Marker ist unvollstaendig.");
            File selected = this.resolveBackup(lines.get(0).trim());
            File safety = this.resolveBackup(lines.get(1).trim());
            List<String> problems = validateArchive(selected, this.dir);
            if (!problems.isEmpty()) throw new IOException("Ziel-Backup ist nicht mehr gueltig: " + String.join("; ", problems));
            if (!safety.isFile()) throw new IOException("Rueckfall-Backup fehlt; Restore abgebrochen.");
            Path data = this.plugin.getDataFolder().toPath().toAbsolutePath().normalize();
            Files.createDirectories(data);
            staging = Files.createTempDirectory(data, ".restore-");
            Set<String> restoredDatabases = new HashSet<String>();
            try (ZipFile zip = new ZipFile(selected)) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.equals(MANIFEST)) continue;
                    if (entry.isDirectory() || name.contains("/") || name.contains("\\") || (!name.endsWith(".yml") && !isSqliteName(name))) {
                        throw new IOException("Unsicherer ZIP-Eintrag: " + name);
                    }
                    Path staged = staging.resolve(name).normalize();
                    if (!staged.getParent().equals(staging)) throw new IOException("Unsicherer Dateipfad im Backup.");
                    try (InputStream in = zip.getInputStream(entry); OutputStream out = Files.newOutputStream(staged)) {
                        copyLimited(in, out, MAX_ENTRY_BYTES);
                    }
                    if (isSqliteName(name)) restoredDatabases.add(name);
                }
            }
            // Alle Dateien liegen zuerst vollstaendig im Staging. Erst danach werden sie atomar eingesetzt.
            try (var paths = Files.list(staging)) {
                for (Path staged : paths.toList()) {
                    Path target = data.resolve(staged.getFileName()).normalize();
                    if (!target.getParent().equals(data)) throw new IOException("Restore-Ziel liegt ausserhalb des Pluginordners.");
                    try {
                        Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException e) {
                        Files.move(staged, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
            for (String db : restoredDatabases) {
                Files.deleteIfExists(data.resolve(db + "-wal"));
                Files.deleteIfExists(data.resolve(db + "-shm"));
                Files.deleteIfExists(data.resolve(db + "-journal"));
            }
            Files.deleteIfExists(marker.toPath());
            this.plugin.getLogger().warning("DuelNetwork-Daten aus " + selected.getName() + " wiederhergestellt. Sicherheitskopie: " + safety.getName());
        } catch (Exception e) {
            this.plugin.getLogger().severe("Vorgemerkter DuelNetwork-Restore wurde nicht angewendet: " + e.getMessage() + ". Der Marker bleibt fuer einen erneuten Versuch erhalten.");
        } finally {
            if (staging != null) deleteTree(staging);
        }
    }

    private static boolean isSqliteName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".db") || lower.endsWith(".sqlite") || lower.endsWith(".sqlite3");
    }

    private static long copyLimited(InputStream in, OutputStream out, long maxBytes) throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long copied = 0L;
        int n;
        while ((n = in.read(buffer)) >= 0) {
            if (n == 0) continue;
            copied += n;
            if (copied > maxBytes) throw new IOException("Eintrag ueberschreitet das Sicherheitslimit.");
            out.write(buffer, 0, n);
        }
        return copied;
    }

    private static MessageDigest sha256() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 ist nicht verfuegbar", e);
        }
    }

    private static String hex(byte[] data) {
        StringBuilder out = new StringBuilder(data.length * 2);
        for (byte b : data) out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        return out.toString();
    }

    private static void deleteTree(Path directory) {
        if (directory == null || !Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }
}
