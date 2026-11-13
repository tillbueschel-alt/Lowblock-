package de.server.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.plugin.Plugin;

/**
 * Haelt die Welten schlank: zu viele Boden-Items und Tiere in einem Chunk sind die haeufigste Lag-Ursache.
 * Alles ueber dem Limit wird entfernt (aelteste Items zuerst; Tiere mit Namen, zahme, angeleinte und
 * Pferde/Haustiere bleiben immer). Config: cleanup.*. Laeuft im Hauptthread, aber nur ueber bereits geladene Chunks.
 */
final class Cleanup {
    static Cleanup instance;

    private final DuelNetworkPlugin plugin;
    private long removedItems;
    private long removedAnimals;

    Cleanup(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        if (plugin.getConfig().getBoolean("cleanup.enabled", true)) {
            long period = 20L * Math.max(10L, plugin.getConfig().getLong("cleanup.interval-seconds", 60L));
            plugin.getServer().getScheduler().runTaskTimer((Plugin)plugin, Perf.wrap("Cleanup", this::run), period, period);
        }
    }

    String summary() {
        return "§7Aufgeräumt seit Start: §f" + this.removedItems + " §7Items, §f" + this.removedAnimals + " §7Tiere";
    }

    private void run() {
        int maxItems = Math.max(10, this.plugin.getConfig().getInt("cleanup.max-items-per-chunk", 60));
        int maxAnimals = Math.max(8, this.plugin.getConfig().getInt("cleanup.max-animals-per-chunk", 40));
        for (World world : this.plugin.getServer().getWorlds()) {
            // Spielerwelten durchgehen; Duell-Arenen und Lobby enthalten kaum Items/Tiere, kosten aber nichts
            for (Chunk chunk : world.getLoadedChunks()) {
                Entity[] entities = chunk.getEntities();
                if (entities.length <= 12) {
                    continue;
                }
                List<Item> items = new ArrayList<Item>();
                List<LivingEntity> animals = new ArrayList<LivingEntity>();
                for (Entity entity : entities) {
                    if (entity instanceof Item item) {
                        items.add(item);
                    } else if (entity instanceof Animals animal && this.removable(animal)) {
                        animals.add(animal);
                    }
                }
                if (items.size() > maxItems) {
                    // aelteste zuerst entfernen (hoechstes Alter)
                    items.sort((x, y) -> Integer.compare(y.getTicksLived(), x.getTicksLived()));
                    for (int i = 0; i < items.size() - maxItems; i++) {
                        items.get(i).remove();
                        this.removedItems++;
                    }
                }
                if (animals.size() > maxAnimals) {
                    Map<String, Integer> kept = new HashMap<String, Integer>();
                    int excess = animals.size() - maxAnimals;
                    for (LivingEntity animal : animals) {
                        if (excess <= 0) {
                            break;
                        }
                        // aeltere Tiere zuerst; Babys bleiben, damit Zucht nicht kaputtgeht
                        if (animal instanceof org.bukkit.entity.Ageable ageable && !ageable.isAdult()) {
                            continue;
                        }
                        kept.merge(animal.getType().name(), 1, Integer::sum);
                        animal.remove();
                        this.removedAnimals++;
                        excess--;
                    }
                }
            }
        }
    }

    private boolean removable(Animals animal) {
        if (animal.getCustomName() != null || animal.isLeashed() || animal.isInsideVehicle() || !animal.getPassengers().isEmpty()) {
            return false;
        }
        if (animal instanceof Tameable tameable && tameable.isTamed()) {
            return false;
        }
        return !(animal instanceof org.bukkit.entity.AbstractHorse);
    }
}
