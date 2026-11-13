package de.server.network;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Claims-Fassade: authoritative Datenbank, speicherfreundlicher Laufzeit-Cache und Rollenregeln. */
final class ClaimManager {
    enum Action { BUILD, INTERACT, CONTAINER, PVP }
    private final DuelNetworkPlugin plugin;
    private final ClaimDatabase database;
    private final List<Claim> claims = new ArrayList<>();
    /** Chunk-Index: "welt:cx:cz" -> Claims, die diesen Chunk beruehren (find() prueft nur diese statt aller Claims). */
    private final java.util.Map<String, List<Claim>> chunkIndex = new java.util.HashMap<>();

    private static String chunkKey(String world, int cx, int cz) {
        return world + ":" + cx + ":" + cz;
    }

    private void indexAdd(Claim claim) {
        for (int cx = claim.minX >> 4; cx <= claim.maxX >> 4; cx++)
            for (int cz = claim.minZ >> 4; cz <= claim.maxZ >> 4; cz++)
                chunkIndex.computeIfAbsent(chunkKey(claim.world, cx, cz), k -> new ArrayList<>()).add(claim);
    }

    private void indexRemove(Claim claim) {
        for (int cx = claim.minX >> 4; cx <= claim.maxX >> 4; cx++)
            for (int cz = claim.minZ >> 4; cz <= claim.maxZ >> 4; cz++) {
                List<Claim> list = chunkIndex.get(chunkKey(claim.world, cx, cz));
                if (list != null) { list.remove(claim); if (list.isEmpty()) chunkIndex.remove(chunkKey(claim.world, cx, cz)); }
            }
    }
    private final java.io.File flagFile;
    private final org.bukkit.configuration.file.YamlConfiguration flags;

    ClaimManager(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        this.flagFile = new java.io.File(plugin.getDataFolder(), "claim-flags.yml");
        this.flags = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(this.flagFile);
        try {
            this.database = new ClaimDatabase(new java.io.File(plugin.getDataFolder(), plugin.getConfig().getString("claims.database", "claims.db")), plugin.getLogger());
            reload();
        } catch (Exception ex) {
            throw new IllegalStateException("Claims-Datenbank konnte nicht gestartet werden", ex);
        }
    }

    synchronized void reload() throws SQLException {
        this.claims.clear();
        this.chunkIndex.clear();
        for (ClaimDatabase.Row row : database.claims()) {
            Claim claim = new Claim(row.id(), row.world(), row.minX(), row.maxX(), row.minZ(), row.maxZ(), row.owner(), row.name());
            this.claims.add(claim);
            this.indexAdd(claim);
        }
        for (ClaimDatabase.Member member : database.members()) {
            for (Claim claim : claims) if (claim.id == member.claimId()) claim.roles.put(member.player(), member.role());
        }
    }

    Claim find(Location location) {
        if (location == null || location.getWorld() == null) return null;
        // Claims gelten ausschließlich im Bau-SMP. Alte Claims aus einem früheren
        // Build bleiben in der Datenbank erhalten, sind außerhalb dieser Zone aber inaktiv.
        try { if (!plugin.insideBauBorder(location)) return null; }
        catch (Throwable ignored) { return null; }
        synchronized (this) {
            List<Claim> near = chunkIndex.get(chunkKey(location.getWorld().getName(), location.getBlockX() >> 4, location.getBlockZ() >> 4));
            if (near != null) for (Claim claim : near) if (claim.contains(location)) return claim;
        }
        return null;
    }

    /** Besucher-Modus: Fremde duerfen Tueren, Knoepfe und Hebel benutzen (Kisten und Bauen bleiben gesperrt). */
    boolean visitorInteract(Claim claim) {
        return claim != null && this.flags.getBoolean("open." + claim.id, false);
    }

    boolean setVisitorInteract(Player actor, Claim claim, boolean on) {
        if (claim == null || actor == null || !claim.owner.equals(actor.getUniqueId())) return false;
        this.flags.set("open." + claim.id, on ? Boolean.TRUE : null);
        try { Store.save(this.flags, this.flagFile); return true; }
        catch (java.io.IOException ex) { plugin.getLogger().warning("claim-flags.yml konnte nicht gespeichert werden: " + ex.getMessage()); return false; }
    }

    Claim byId(long id) {
        synchronized (this) { for (Claim claim : claims) if (claim.id == id) return claim; }
        return null;
    }

    /** Einladung wurde angenommen: Rolle ohne Besitzer-Aktion setzen. */
    boolean acceptInvite(Claim claim, UUID player, String role) {
        if (claim == null || player.equals(claim.owner)) return false;
        try { database.setMember(claim.id, player, role, claim.owner); claim.roles.put(player, role); return true; }
        catch (SQLException ex) { plugin.getLogger().warning("Claim-Einladung konnte nicht gespeichert werden: " + ex.getMessage()); return false; }
    }

    boolean enabled() { return plugin.getConfig().getBoolean("claims.enabled", true); }

    boolean eligible(Player player, Location location) {
        if (!enabled() || player == null || location == null || location.getWorld() == null) return false;
        UUID id = player.getUniqueId();
        if (!"smp".equals(plugin.profiles.getOrDefault(id, "lobby")) || plugin.activeDuelPlayers.contains(id)
                || plugin.activePartyMatches.containsKey(id) || plugin.activeParkourMaps.containsKey(id) || !plugin.inBauMode(id)) return false;
        try { return !plugin.lobbyProtected(player, location) && plugin.insideBauBorder(location); }
        catch (Throwable ignored) { return false; }
    }

    boolean bypass(Player player) {
        return player != null && (player.hasPermission(plugin.getConfig().getString("claims.admin-bypass-permission", "duelnetwork.claim.bypass"))
                || player.hasPermission("duelnetwork.claim.admin"));
    }

    boolean allowed(Player player, Location location, Action action) {
        if (!enabled() || player == null || bypass(player)) return true;
        Claim claim = find(location);
        if (claim == null) return true;
        String role = claim.role(player.getUniqueId());
        if (action == Action.PVP) return !plugin.getConfig().getBoolean("claims.protect-pvp", true);
        if ("owner".equals(role) || "member".equals(role)) return true;
        if (role == null && action == Action.INTERACT && visitorInteract(claim)) return true;
        return action == Action.INTERACT || action == Action.CONTAINER ? "trusted".equals(role) : false;
    }

    /** Wie denyMessage(action), nennt aber den Besitzer des Claims. */
    String denyMessage(Action action, Claim claim) {
        if (claim == null) return denyMessage(action);
        String owner = plugin.getServer().getOfflinePlayer(claim.owner).getName();
        String who = owner == null ? "einem anderen Spieler" : owner;
        return action == Action.CONTAINER ? "§cDiese Kiste gehört zum Claim von §f" + who + "§c." : "§cDieser Claim gehört §f" + who + "§c. Du darfst hier nichts verändern.";
    }

    String denyMessage(Action action) {
        return action == Action.CONTAINER ? "§cDiese Kiste gehört zu einem geschützten Claim." : "§cDu darfst hier im Claim nichts verändern.";
    }

    boolean protectedExplosions() {
        return plugin.getConfig().getBoolean("claims.protect-explosions", true);
    }

    Claim create(Player player, String name, int radius) {
        if (!eligible(player, player.getLocation())) return null;
        radius = Math.max(this.plugin.getConfig().getInt("claims.min-radius", 2), radius);
        int maxRadius = (int) Math.sqrt(Math.max(1, this.plugin.getConfig().getInt("claims.max-area", 10000)));
        radius = Math.min(radius, maxRadius / 2);
        int cx = player.getLocation().getBlockX(), cz = player.getLocation().getBlockZ();
        int minX = cx - radius, maxX = cx + radius, minZ = cz - radius, maxZ = cz + radius;
        return this.createBounds(player, name, minX, maxX, minZ, maxZ);
    }

    /** Claimt den kompletten Chunk (16x16), in dem der Spieler steht. */
    Claim createChunk(Player player) {
        if (!eligible(player, player.getLocation())) return null;
        org.bukkit.Chunk chunk = player.getLocation().getChunk();
        int minX = chunk.getX() * 16, minZ = chunk.getZ() * 16;
        return this.createBounds(player, "chunk-" + chunk.getX() + "_" + chunk.getZ(), minX, minX + 15, minZ, minZ + 15);
    }

    private Claim createBounds(Player player, String name, int minX, int maxX, int minZ, int maxZ) {
        int area = (maxX - minX + 1) * (maxZ - minZ + 1);
        if (area > plugin.getConfig().getInt("claims.max-area", 10000)) return null;
        synchronized (this) {
            int owned = 0;
            for (Claim claim : claims) {
                if (claim.owner.equals(player.getUniqueId())) owned++;
                if (claim.world.equals(player.getWorld().getName()) && overlaps(minX, maxX, minZ, maxZ, claim)) return null;
            }
            if (owned >= Math.max(1, plugin.getConfig().getInt("claims.max-per-player", 3))) return null;
            try {
                long id = database.create(player.getWorld().getName(), minX, maxX, minZ, maxZ, player.getUniqueId(), name);
                Claim claim = new Claim(id, player.getWorld().getName(), minX, maxX, minZ, maxZ, player.getUniqueId(), name);
                claim.roles.put(player.getUniqueId(), "owner");
                claims.add(claim);
                indexAdd(claim);
                return claim;
            } catch (SQLException ex) { plugin.getLogger().warning("Claim konnte nicht erstellt werden: " + ex.getMessage()); return null; }
        }
    }

    private boolean overlaps(int minX, int maxX, int minZ, int maxZ, Claim other) {
        return minX <= other.maxX && maxX >= other.minX && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    boolean delete(Player actor, Claim claim) {
        if (claim == null || actor == null || (!claim.owner.equals(actor.getUniqueId()) && !bypass(actor))) return false;
        try { database.delete(claim.id, actor.getUniqueId(), "DELETE"); synchronized (this) { claims.remove(claim); indexRemove(claim); } return true; }
        catch (SQLException ex) { plugin.getLogger().warning("Claim konnte nicht gelöscht werden: " + ex.getMessage()); return false; }
    }

    boolean setRole(Player actor, Claim claim, UUID player, String role) {
        if (claim == null || actor == null || !claim.owner.equals(actor.getUniqueId()) || player.equals(claim.owner)) return false;
        try { database.setMember(claim.id, player, role, actor.getUniqueId()); claim.roles.put(player, role); return true; }
        catch (SQLException ex) { plugin.getLogger().warning("Claim-Rolle konnte nicht gespeichert werden: " + ex.getMessage()); return false; }
    }

    boolean removeRole(Player actor, Claim claim, UUID player) {
        if (claim == null || actor == null || !claim.owner.equals(actor.getUniqueId()) || player.equals(claim.owner)) return false;
        try { database.removeMember(claim.id, player, actor.getUniqueId()); claim.roles.remove(player); return true; }
        catch (SQLException ex) { plugin.getLogger().warning("Claim-Rolle konnte nicht entfernt werden: " + ex.getMessage()); return false; }
    }

    List<Claim> owned(UUID owner) {
        List<Claim> result = new ArrayList<>();
        synchronized (this) { for (Claim claim : claims) if (claim.owner.equals(owner)) result.add(claim); }
        return result;
    }

    List<Claim> all() { synchronized (this) { return new ArrayList<>(claims); } }

    void close() { database.close(); }
}
