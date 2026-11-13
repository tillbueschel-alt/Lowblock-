package de.server.network;

import java.lang.reflect.Method;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

/**
 * Locator-Bar nur fuers Drachenei: Spieler senden kein Signal (waypoint_transmit_range 0), nur das Drachenei zeigt sich -
 * als Signal des Spielers, der es traegt, sonst ueber einen unsichtbaren Marker am Schrein bzw. am herumliegenden Ei.
 * Gibt es kein Drachenei, ist die Leiste leer. Alles per Reflection, damit es auf jeder Paper-Version nur im Zweifel still ausfaellt.
 * Die Leiste ist ausserhalb des SMP komplett aus (Empfangsreichweite 0), im SMP zeigt sie nur das Drachenei, falls es existiert.
 * Config: locator.dragon-egg-only (true).
 */
final class Locator implements Listener {
    private static final double FAR = 6.0E7;
    private final DuelNetworkPlugin plugin;
    private Entity marker;
    private boolean warned;

    Locator(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("Locator", () -> this.update()), 40L, 40L);
    }

    private boolean enabled() {
        return this.plugin.getConfig().getBoolean("locator.dragon-egg-only", true);
    }

    private boolean transmit(Entity entity, double range) {
        return this.attribute(entity, "WAYPOINT_TRANSMIT_RANGE", range);
    }

    /** waypoint_receive_range: 0 = der Spieler sieht keine Punkte in der Leiste. */
    private boolean receive(Entity entity, double range) {
        return this.attribute(entity, "WAYPOINT_RECEIVE_RANGE", range);
    }

    /** true nur im SMP (nicht Lobby, Duelle, Events, Parkour, Bau-SMP). */
    private boolean inSmp(Player player) {
        return "smp".equals(this.plugin.profiles.getOrDefault(player.getUniqueId(), "lobby")) && !this.plugin.inBauMode(player.getUniqueId());
    }

    /** Setzt ein Waypoint-Attribut (Spieler oder Marker). false, wenn das Attribut nicht existiert. */
    private boolean attribute(Entity entity, String field, double range) {
        try {
            Class<?> attributeClass = Class.forName("org.bukkit.attribute.Attribute");
            Object attribute = attributeClass.getField(field).get(null);
            Method getAttribute = Class.forName("org.bukkit.attribute.Attributable").getMethod("getAttribute", attributeClass);
            Object instance = getAttribute.invoke(entity, attribute);
            if (instance == null) {
                return false;
            }
            Class.forName("org.bukkit.attribute.AttributeInstance").getMethod("setBaseValue", double.class).invoke(instance, range);
            return true;
        }
        catch (Throwable t) {
            if (!this.warned) {
                this.warned = true;
                this.plugin.getLogger().warning("Locator-Bar: waypoint_transmit_range nicht verfuegbar (" + t + "). Die Leiste bleibt dann wie in Vanilla.");
            }
            return false;
        }
    }

    private static void call(Object target, String name, boolean value) {
        try {
            target.getClass().getMethod(name, boolean.class).invoke(target, value);
        }
        catch (Throwable ignored) {
            // nicht jede Version hat jede Methode
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (this.enabled()) {
            this.transmit((Entity)event.getPlayer(), 0.0);
            this.receive((Entity)event.getPlayer(), 0.0);
        }
    }

    private void update() {
        try {
            if (!this.enabled()) {
                return;
            }
            boolean carried = false;
            for (Player p : this.plugin.getServer().getOnlinePlayers()) {
                // Leiste ist ueberall aus; nur im SMP sieht man das Drachenei (Traeger sendet, SMP-Spieler empfangen)
                boolean smp = this.inSmp(p);
                boolean has = smp && DragonEgg.carries(p);
                carried |= has;
                this.transmit((Entity)p, has ? FAR : 0.0);
                this.receive((Entity)p, smp ? FAR : 0.0);
            }
            Location spot = carried || DragonEgg.instance == null ? null : DragonEgg.instance.waypointSpot();
            if (spot == null || spot.getWorld() == null) {
                this.removeMarker();
                return;
            }
            if (this.marker == null || !this.marker.isValid() || !this.marker.getWorld().equals(spot.getWorld())) {
                this.removeMarker();
                Class<?> type = Class.forName("org.bukkit.entity.EntityType");
                Object armorStand = type.getField("ARMOR_STAND").get(null);
                Method spawn = spot.getWorld().getClass().getMethod("spawnEntity", Location.class, type);
                Entity e = (Entity)spawn.invoke(spot.getWorld(), spot, armorStand);
                call(e, "setInvisible", true);
                call(e, "setMarker", true);
                call(e, "setGravity", false);
                call(e, "setSilent", true);
                call(e, "setPersistent", false);
                e.addScoreboardTag("lb_locator");
                e.setInvulnerable(true);
                if (!this.transmit(e, FAR)) {
                    e.remove();
                    return;
                }
                this.marker = e;
            } else if (this.marker.getLocation().distanceSquared(spot) > 1.0) {
                this.marker.teleport(spot);
            }
        }
        catch (Throwable t) {
            Store.problem("Locator:update", t);
        }
    }

    private void removeMarker() {
        try {
            if (this.marker != null && this.marker.isValid()) {
                this.marker.remove();
            }
        }
        catch (Throwable ignored) {
            // egal
        }
        this.marker = null;
    }

    void shutdown() {
        this.removeMarker();
    }
}
