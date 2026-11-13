package de.server.network;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/** Persistenter, atomarer Claim-Speicher mit versioniertem Schema. */
final class ClaimDatabase implements AutoCloseable {
    record Row(long id, String world, int minX, int maxX, int minZ, int maxZ, UUID owner, String name) { }
    record Member(long claimId, UUID player, String role) { }

    private static final int SCHEMA = 1;
    private final Connection connection;
    private final Logger logger;

    ClaimDatabase(java.io.File file, Logger logger) throws SQLException {
        this.logger = logger;
        try { Class.forName("org.sqlite.JDBC"); }
        catch (ClassNotFoundException ex) { throw new SQLException("SQLite-Treiber fehlt", ex); }
        file.getParentFile().mkdirs();
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
        try (Statement s = this.connection.createStatement()) {
            s.execute("PRAGMA busy_timeout=5000");
            s.execute("PRAGMA journal_mode=WAL");
            s.execute("PRAGMA synchronous=FULL");
            s.execute("PRAGMA foreign_keys=ON");
            s.execute("CREATE TABLE IF NOT EXISTS schema_version (component TEXT PRIMARY KEY, version INTEGER NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS claims (id INTEGER PRIMARY KEY AUTOINCREMENT, world TEXT NOT NULL, min_x INTEGER NOT NULL, max_x INTEGER NOT NULL, min_z INTEGER NOT NULL, max_z INTEGER NOT NULL, owner_uuid TEXT NOT NULL, name TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS claim_members (claim_id INTEGER NOT NULL, uuid TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('owner','member','trusted')), added_by TEXT NOT NULL, added_at INTEGER NOT NULL, PRIMARY KEY(claim_id,uuid), FOREIGN KEY(claim_id) REFERENCES claims(id) ON DELETE CASCADE)");
            s.execute("CREATE TABLE IF NOT EXISTS claim_audit (id INTEGER PRIMARY KEY AUTOINCREMENT, claim_id INTEGER, actor_uuid TEXT NOT NULL, action TEXT NOT NULL, payload TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_claims_world_xz ON claims(world,min_x,max_x,min_z,max_z)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_claim_members_uuid ON claim_members(uuid)");
            s.execute("INSERT OR IGNORE INTO schema_version(component,version) VALUES('claims'," + SCHEMA + ")");
        }
        logger.info("Claims: SQLite-Datenbank bereit (Schema " + SCHEMA + ").");
    }

    synchronized List<Row> claims() throws SQLException {
        List<Row> result = new ArrayList<>();
        try (PreparedStatement s = connection.prepareStatement("SELECT id,world,min_x,max_x,min_z,max_z,owner_uuid,name FROM claims ORDER BY id"); ResultSet r = s.executeQuery()) {
            while (r.next()) {
                try { result.add(new Row(r.getLong(1), r.getString(2), r.getInt(3), r.getInt(4), r.getInt(5), r.getInt(6), UUID.fromString(r.getString(7)), r.getString(8))); }
                catch (IllegalArgumentException ignored) { }
            }
        }
        return result;
    }

    synchronized List<Member> members() throws SQLException {
        List<Member> result = new ArrayList<>();
        try (PreparedStatement s = connection.prepareStatement("SELECT claim_id,uuid,role FROM claim_members"); ResultSet r = s.executeQuery()) {
            while (r.next()) {
                try { result.add(new Member(r.getLong(1), UUID.fromString(r.getString(2)), r.getString(3))); }
                catch (IllegalArgumentException ignored) { }
            }
        }
        return result;
    }

    synchronized long create(String world, int minX, int maxX, int minZ, int maxZ, UUID owner, String name) throws SQLException {
        connection.setAutoCommit(false);
        try {
            long now = System.currentTimeMillis();
            long id;
            try (PreparedStatement s = connection.prepareStatement("INSERT INTO claims(world,min_x,max_x,min_z,max_z,owner_uuid,name,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                s.setString(1, world); s.setInt(2, minX); s.setInt(3, maxX); s.setInt(4, minZ); s.setInt(5, maxZ); s.setString(6, owner.toString()); s.setString(7, name); s.setLong(8, now); s.setLong(9, now); s.executeUpdate();
                try (ResultSet keys = s.getGeneratedKeys()) { if (!keys.next()) throw new SQLException("Claim-ID fehlt"); id = keys.getLong(1); }
            }
            memberInsert(id, owner, "owner", owner);
            audit(id, owner, "CREATE", name);
            connection.commit();
            return id;
        } catch (SQLException ex) { connection.rollback(); throw ex; } finally { connection.setAutoCommit(true); }
    }

    synchronized void delete(long id, UUID actor, String action) throws SQLException {
        connection.setAutoCommit(false);
        try (PreparedStatement s = connection.prepareStatement("DELETE FROM claims WHERE id=?")) {
            s.setLong(1, id); s.executeUpdate(); audit(id, actor, action, ""); connection.commit();
        } catch (SQLException ex) { connection.rollback(); throw ex; } finally { connection.setAutoCommit(true); }
    }

    synchronized void setMember(long claimId, UUID player, String role, UUID actor) throws SQLException {
        if (!role.equals("member") && !role.equals("trusted")) throw new SQLException("Ungültige Claim-Rolle");
        connection.setAutoCommit(false);
        try { memberInsert(claimId, player, role, actor); audit(claimId, actor, "SET_ROLE", player + ";" + role); connection.commit(); }
        catch (SQLException ex) { connection.rollback(); throw ex; } finally { connection.setAutoCommit(true); }
    }

    synchronized void removeMember(long claimId, UUID player, UUID actor) throws SQLException {
        connection.setAutoCommit(false);
        try (PreparedStatement s = connection.prepareStatement("DELETE FROM claim_members WHERE claim_id=? AND uuid=? AND role<>'owner'")) {
            s.setLong(1, claimId); s.setString(2, player.toString()); s.executeUpdate(); audit(claimId, actor, "REMOVE_ROLE", player.toString()); connection.commit();
        } catch (SQLException ex) { connection.rollback(); throw ex; } finally { connection.setAutoCommit(true); }
    }

    private void memberInsert(long claimId, UUID player, String role, UUID actor) throws SQLException {
        try (PreparedStatement s = connection.prepareStatement("INSERT INTO claim_members(claim_id,uuid,role,added_by,added_at) VALUES(?,?,?,?,?) ON CONFLICT(claim_id,uuid) DO UPDATE SET role=excluded.role,added_by=excluded.added_by,added_at=excluded.added_at")) {
            s.setLong(1, claimId); s.setString(2, player.toString()); s.setString(3, role); s.setString(4, actor.toString()); s.setLong(5, System.currentTimeMillis()); s.executeUpdate();
        }
    }

    private void audit(long claimId, UUID actor, String action, String payload) throws SQLException {
        try (PreparedStatement s = connection.prepareStatement("INSERT INTO claim_audit(claim_id,actor_uuid,action,payload,created_at) VALUES(?,?,?,?,?)")) {
            s.setLong(1, claimId); s.setString(2, actor.toString()); s.setString(3, action); s.setString(4, payload == null ? "" : payload); s.setLong(5, System.currentTimeMillis()); s.executeUpdate();
        }
    }

    @Override public synchronized void close() {
        try { connection.close(); } catch (SQLException ex) { logger.warning("claims.db konnte nicht geschlossen werden: " + ex.getMessage()); }
    }
}
