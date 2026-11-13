package de.server.network;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Persistente Economy-Schicht.
 *
 * Kontenänderungen und der Ledger-Eintrag werden in einer SQLite-Transaktion
 * geschrieben. Die eindeutige Transaktions-ID macht Wiederholungen nach einem
 * Crash idempotent. Das Journal hält zusätzlich externe Abläufe wie Orders und
 * Auktionen bis zum endgültigen Abschluss fest.
 */
final class EconomyDatabase implements AutoCloseable {
    record Account(UUID id, String name, long balance) { }
    record JournalEntry(String id, String type, String payload, String status) { }

    private final Logger logger;
    private final Connection connection;

    EconomyDatabase(java.io.File file, java.io.File legacyFile, Logger logger) throws SQLException {
        this.logger = logger;
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("SQLite-Treiber fehlt", ex);
        }
        file.getParentFile().mkdirs();
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        this.connection.setAutoCommit(true);
        try (Statement statement = this.connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout=5000");
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=FULL");
            statement.execute("CREATE TABLE IF NOT EXISTS accounts (uuid TEXT PRIMARY KEY, name TEXT NOT NULL, balance INTEGER NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS ledger (tx_id TEXT PRIMARY KEY, kind TEXT NOT NULL, from_uuid TEXT, to_uuid TEXT, amount INTEGER NOT NULL, created_at INTEGER NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS journal (id TEXT PRIMARY KEY, type TEXT NOT NULL, payload TEXT NOT NULL, status TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
        }
        migrateLegacy(legacyFile);
    }

    private void migrateLegacy(java.io.File legacyFile) throws SQLException {
        try (PreparedStatement count = this.connection.prepareStatement("SELECT COUNT(*) FROM accounts"); ResultSet rs = count.executeQuery()) {
            if (!rs.next() || rs.getLong(1) != 0L) {
                return;
            }
        }
        if (!legacyFile.isFile()) {
            return;
        }
        org.bukkit.configuration.file.YamlConfiguration yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(legacyFile);
        org.bukkit.configuration.ConfigurationSection section = yaml.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        this.connection.setAutoCommit(false);
        try (PreparedStatement insert = this.connection.prepareStatement("INSERT OR IGNORE INTO accounts(uuid,name,balance) VALUES(?,?,?)")) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    insert.setString(1, id.toString());
                    insert.setString(2, section.getString(key + ".name", key));
                    insert.setLong(3, Math.max(0L, section.getLong(key + ".money", 0L)));
                    insert.addBatch();
                } catch (IllegalArgumentException ignored) {
                    // Ungültige Legacy-UUID wird ignoriert und bleibt in der YAML sichtbar.
                }
            }
            insert.executeBatch();
            this.connection.commit();
        } catch (SQLException ex) {
            this.connection.rollback();
            throw ex;
        } finally {
            this.connection.setAutoCommit(true);
        }
        this.logger.info("Economy: economy.yml wurde in die transaktionale SQLite-Datenbank übernommen.");
    }

    synchronized List<Account> accounts() throws SQLException {
        List<Account> result = new ArrayList<>();
        try (PreparedStatement statement = this.connection.prepareStatement("SELECT uuid,name,balance FROM accounts"); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                try {
                    result.add(new Account(UUID.fromString(rs.getString(1)), rs.getString(2), rs.getLong(3)));
                } catch (IllegalArgumentException ignored) {
                    // Kaputter Datensatz wird nicht in die Laufzeit-Map übernommen.
                }
            }
        }
        return result;
    }

    synchronized boolean ensureAccount(UUID id, String name, long startBalance) throws SQLException {
        try (PreparedStatement statement = this.connection.prepareStatement("INSERT OR IGNORE INTO accounts(uuid,name,balance) VALUES(?,?,?)")) {
            statement.setString(1, id.toString());
            statement.setString(2, name == null ? "?" : name);
            statement.setLong(3, Math.max(0L, startBalance));
            boolean inserted = statement.executeUpdate() > 0;
            if (!inserted && name != null) {
                try (PreparedStatement update = this.connection.prepareStatement("UPDATE accounts SET name=? WHERE uuid=?")) {
                    update.setString(1, name);
                    update.setString(2, id.toString());
                    update.executeUpdate();
                }
            }
            return inserted;
        }
    }

    synchronized long balance(UUID id) throws SQLException {
        try (PreparedStatement statement = this.connection.prepareStatement("SELECT balance FROM accounts WHERE uuid=?")) {
            statement.setString(1, id.toString());
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private boolean hasLedger(String txId) throws SQLException {
        try (PreparedStatement statement = this.connection.prepareStatement("SELECT 1 FROM ledger WHERE tx_id=?")) {
            statement.setString(1, txId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private long safeAdd(long current, long amount) {
        try {
            return Math.addExact(current, amount);
        } catch (ArithmeticException overflow) {
            return amount >= 0L ? Long.MAX_VALUE : Long.MIN_VALUE;
        }
    }

    synchronized boolean credit(UUID id, String name, long amount, String txId) throws SQLException {
        if (amount <= 0L) return amount == 0L;
        this.connection.setAutoCommit(false);
        try {
            if (hasLedger(txId)) {
                this.connection.commit();
                return true;
            }
            ensureAccount(id, name, 0L);
            long next = safeAdd(balance(id), amount);
            try (PreparedStatement update = this.connection.prepareStatement("UPDATE accounts SET name=?,balance=? WHERE uuid=?"); PreparedStatement ledger = this.connection.prepareStatement("INSERT INTO ledger(tx_id,kind,to_uuid,amount,created_at) VALUES(?,?,?,?,?)")) {
                update.setString(1, name == null ? "?" : name);
                update.setLong(2, next);
                update.setString(3, id.toString());
                update.executeUpdate();
                ledger.setString(1, txId);
                ledger.setString(2, "CREDIT");
                ledger.setString(3, id.toString());
                ledger.setLong(4, amount);
                ledger.setLong(5, System.currentTimeMillis());
                ledger.executeUpdate();
            }
            this.connection.commit();
            return true;
        } catch (SQLException ex) {
            this.connection.rollback();
            throw ex;
        } finally {
            this.connection.setAutoCommit(true);
        }
    }

    synchronized boolean debit(UUID id, long amount, String txId) throws SQLException {
        if (amount < 0L) return false;
        if (amount == 0L) return true;
        this.connection.setAutoCommit(false);
        try {
            if (hasLedger(txId)) {
                this.connection.commit();
                return true;
            }
            long current = balance(id);
            if (current < amount) {
                this.connection.rollback();
                return false;
            }
            try (PreparedStatement update = this.connection.prepareStatement("UPDATE accounts SET balance=balance-? WHERE uuid=? AND balance>=?"); PreparedStatement ledger = this.connection.prepareStatement("INSERT INTO ledger(tx_id,kind,from_uuid,amount,created_at) VALUES(?,?,?,?,?)")) {
                update.setLong(1, amount);
                update.setString(2, id.toString());
                update.setLong(3, amount);
                if (update.executeUpdate() != 1) {
                    this.connection.rollback();
                    return false;
                }
                ledger.setString(1, txId);
                ledger.setString(2, "DEBIT");
                ledger.setString(3, id.toString());
                ledger.setLong(4, amount);
                ledger.setLong(5, System.currentTimeMillis());
                ledger.executeUpdate();
            }
            this.connection.commit();
            return true;
        } catch (SQLException ex) {
            this.connection.rollback();
            throw ex;
        } finally {
            this.connection.setAutoCommit(true);
        }
    }

    synchronized boolean set(UUID id, String name, long amount, String txId) throws SQLException {
        this.connection.setAutoCommit(false);
        try {
            if (hasLedger(txId)) {
                this.connection.commit();
                return true;
            }
            ensureAccount(id, name, 0L);
            try (PreparedStatement update = this.connection.prepareStatement("UPDATE accounts SET name=?,balance=? WHERE uuid=?"); PreparedStatement ledger = this.connection.prepareStatement("INSERT INTO ledger(tx_id,kind,to_uuid,amount,created_at) VALUES(?,?,?,?,?)")) {
                update.setString(1, name == null ? "?" : name);
                update.setLong(2, Math.max(0L, amount));
                update.setString(3, id.toString());
                update.executeUpdate();
                ledger.setString(1, txId);
                ledger.setString(2, "SET");
                ledger.setString(3, id.toString());
                ledger.setLong(4, Math.max(0L, amount));
                ledger.setLong(5, System.currentTimeMillis());
                ledger.executeUpdate();
            }
            this.connection.commit();
            return true;
        } catch (SQLException ex) {
            this.connection.rollback();
            throw ex;
        } finally {
            this.connection.setAutoCommit(true);
        }
    }

    synchronized void beginJournal(String id, String type, String payload) throws SQLException {
        long now = System.currentTimeMillis();
        try (PreparedStatement statement = this.connection.prepareStatement("INSERT OR IGNORE INTO journal(id,type,payload,status,created_at,updated_at) VALUES(?,?,?,?,?,?)")) {
            statement.setString(1, id);
            statement.setString(2, type);
            statement.setString(3, payload == null ? "" : payload);
            statement.setString(4, "PENDING");
            statement.setLong(5, now);
            statement.setLong(6, now);
            statement.executeUpdate();
        }
    }

    synchronized void completeJournal(String id) throws SQLException {
        try (PreparedStatement statement = this.connection.prepareStatement("UPDATE journal SET status='COMMITTED',updated_at=? WHERE id=?")) {
            statement.setLong(1, System.currentTimeMillis());
            statement.setString(2, id);
            statement.executeUpdate();
        }
    }

    synchronized List<JournalEntry> pendingJournals() throws SQLException {
        List<JournalEntry> result = new ArrayList<>();
        try (PreparedStatement statement = this.connection.prepareStatement("SELECT id,type,payload,status FROM journal WHERE status='PENDING' ORDER BY created_at"); ResultSet rs = statement.executeQuery()) {
            while (rs.next()) result.add(new JournalEntry(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
        }
        return result;
    }

    synchronized void pruneLedgerAndJournal(long olderThan) throws SQLException {
        try (PreparedStatement ledger = this.connection.prepareStatement("DELETE FROM ledger WHERE created_at<?"); PreparedStatement journal = this.connection.prepareStatement("DELETE FROM journal WHERE status='COMMITTED' AND updated_at<?")) {
            ledger.setLong(1, olderThan);
            ledger.executeUpdate();
            journal.setLong(1, olderThan);
            journal.executeUpdate();
        }
    }

    @Override
    public synchronized void close() {
        try {
            this.connection.close();
        } catch (SQLException ex) {
            this.logger.warning("economy.db konnte nicht geschlossen werden: " + ex.getMessage());
        }
    }
}
