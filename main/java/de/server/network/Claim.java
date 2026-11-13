package de.server.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Ein zweidimensionaler X/Z-Claim innerhalb einer Welt. */
final class Claim {
    final long id;
    final String world;
    final int minX;
    final int maxX;
    final int minZ;
    final int maxZ;
    final UUID owner;
    final String name;
    final Map<UUID, String> roles = new HashMap<>();

    Claim(long id, String world, int minX, int maxX, int minZ, int maxZ, UUID owner, String name) {
        this.id = id;
        this.world = world;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.owner = owner;
        this.name = name;
    }

    boolean contains(org.bukkit.Location location) {
        return location != null && location.getWorld() != null && this.world.equals(location.getWorld().getName())
                && location.getBlockX() >= this.minX && location.getBlockX() <= this.maxX
                && location.getBlockZ() >= this.minZ && location.getBlockZ() <= this.maxZ;
    }

    String role(UUID player) {
        if (this.owner.equals(player)) return "owner";
        return this.roles.get(player);
    }

    int area() {
        long width = (long) this.maxX - this.minX + 1L;
        long depth = (long) this.maxZ - this.minZ + 1L;
        return (int) Math.min(Integer.MAX_VALUE, width * depth);
    }
}
