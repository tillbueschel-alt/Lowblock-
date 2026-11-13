package de.server.network;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Verkaufswerte aller Items (fuer /sale). Explizite Werte fuer wichtige Items, Formeln fuer Werkzeug/Ruestung,
 * Regeln fuer den Rest - jedes handelbare Item hat mindestens einen kleinen Wert. 0 = nicht verkaufbar.
 */
final class Values {
    private static final Map<String, Long> BASE = new HashMap<String, Long>();
    private static final Map<String, Long> TIER = new HashMap<String, Long>();

    private Values() { }

    private static void v(String name, long value) {
        BASE.put(name, value);
    }

    static {
        // Rohstoffe & Erze
        v("COAL", 2); v("CHARCOAL", 2); v("COPPER_INGOT", 6); v("RAW_COPPER", 4); v("IRON_INGOT", 10); v("RAW_IRON", 8);
        v("GOLD_INGOT", 16); v("RAW_GOLD", 13); v("GOLD_NUGGET", 2); v("IRON_NUGGET", 1); v("REDSTONE", 3); v("LAPIS_LAZULI", 3);
        v("QUARTZ", 6); v("AMETHYST_SHARD", 8); v("DIAMOND", 200); v("EMERALD", 60); v("NETHERITE_SCRAP", 2000);
        v("NETHERITE_INGOT", 8000); v("ANCIENT_DEBRIS", 2200); v("COAL_ORE", 4); v("IRON_ORE", 9); v("GOLD_ORE", 14);
        v("COPPER_ORE", 5); v("REDSTONE_ORE", 8); v("LAPIS_ORE", 12); v("DIAMOND_ORE", 210); v("EMERALD_ORE", 45);
        v("NETHER_QUARTZ_ORE", 8); v("NETHER_GOLD_ORE", 7);
        v("COAL_BLOCK", 18); v("IRON_BLOCK", 90); v("GOLD_BLOCK", 144); v("COPPER_BLOCK", 54); v("REDSTONE_BLOCK", 27);
        v("LAPIS_BLOCK", 27); v("QUARTZ_BLOCK", 24); v("DIAMOND_BLOCK", 1800); v("EMERALD_BLOCK", 360); v("NETHERITE_BLOCK", 72000);
        v("AMETHYST_BLOCK", 32);
        // Kampf & Spezial (Totem/Kristall usw. entsprechen 40 % vom Shop-Preis)
        v("END_CRYSTAL", 500); v("TOTEM_OF_UNDYING", 2500); v("ENDER_PEARL", 120); v("OBSIDIAN", 40); v("CRYING_OBSIDIAN", 40);
        v("GOLDEN_APPLE", 100); v("ENCHANTED_GOLDEN_APPLE", 6000); v("NETHER_STAR", 12000); v("BEACON", 15000);
        v("DRAGON_EGG", 300000); v("DRAGON_HEAD", 10000); v("ELYTRA", 60000); v("TRIDENT", 6000); v("HEART_OF_THE_SEA", 2500);
        v("NAUTILUS_SHELL", 80); v("SHULKER_SHELL", 1500); v("SHULKER_BOX", 3500); v("WITHER_SKELETON_SKULL", 3000);
        v("ENCHANTING_TABLE", 90); v("ANVIL", 80); v("EXPERIENCE_BOTTLE", 12); v("BLAZE_ROD", 16); v("BLAZE_POWDER", 8);
        v("GHAST_TEAR", 60); v("MAGMA_CREAM", 10); v("SLIME_BALL", 5); v("PHANTOM_MEMBRANE", 20); v("PRISMARINE_SHARD", 4);
        v("PRISMARINE_CRYSTALS", 6); v("ECHO_SHARD", 200); v("DISC_FRAGMENT_5", 80); v("SCULK_CATALYST", 60);
        v("WARDEN_SPAWN_EGG", 0); v("ENCHANTED_BOOK", 0); v("WRITTEN_BOOK", 0); v("WIND_CHARGE", 16); v("BREEZE_ROD", 20);
        v("HEAVY_CORE", 3000); v("MACE", 12000); v("OMINOUS_TRIAL_KEY", 400); v("TRIAL_KEY", 80);
        v("ENDER_EYE", 70); v("RESPAWN_ANCHOR", 120); v("GLOWSTONE", 16); v("GLOWSTONE_DUST", 4); v("GUNPOWDER", 4); v("TNT", 20);
        v("ARROW", 1); v("SHIELD", 48); v("FIREWORK_ROCKET", 6); v("COBWEB", 12); v("TURTLE_SCUTE", 20); v("ARMADILLO_SCUTE", 20);
        // Essen
        v("COOKED_BEEF", 5); v("COOKED_PORKCHOP", 5); v("COOKED_CHICKEN", 3); v("COOKED_SALMON", 4); v("COOKED_COD", 3);
        v("COOKED_MUTTON", 4); v("COOKED_RABBIT", 3); v("BREAD", 2); v("BAKED_POTATO", 2); v("PUMPKIN_PIE", 4);
        v("GOLDEN_CARROT", 6); v("BEEF", 2); v("PORKCHOP", 2); v("CHICKEN", 1); v("WHEAT", 1); v("CARROT", 1); v("POTATO", 1);
        v("CAKE", 8); v("COOKIE", 1); v("HONEY_BOTTLE", 6); v("MUSHROOM_STEW", 3); v("RABBIT_STEW", 6); v("SUSPICIOUS_STEW", 8);
        // Sonstiges
        v("STRING", 1); v("LEATHER", 3); v("FEATHER", 1); v("BONE", 1); v("SPIDER_EYE", 2); v("ROTTEN_FLESH", 1); v("INK_SAC", 1);
        v("GLOW_INK_SAC", 6); v("SUGAR", 1); v("PAPER", 1); v("BOOK", 4); v("STICK", 1); v("CLAY_BALL", 1); v("BRICK", 2);
        v("NETHER_BRICK", 2); v("NETHERRACK", 1); v("END_STONE", 2); v("SOUL_SAND", 2); v("CHORUS_FRUIT", 4); v("CHORUS_FLOWER", 12);
        v("SPONGE", 40); v("WET_SPONGE", 20); v("SCAFFOLDING", 2); v("BUCKET", 22); v("WATER_BUCKET", 24); v("LAVA_BUCKET", 30);
        v("SADDLE", 150); v("NAME_TAG", 80); v("LEAD", 20); v("COMPASS", 40); v("CLOCK", 60); v("SPYGLASS", 30); v("MAP", 12);
        v("FISHING_ROD", 6); v("BOW", 40); v("CROSSBOW", 48); v("SHEARS", 20); v("FLINT_AND_STEEL", 12); v("ENDER_CHEST", 2500);
        v("HOPPER", 30); v("PISTON", 14); v("STICKY_PISTON", 20); v("OBSERVER", 10); v("DISPENSER", 10); v("DROPPER", 8);
        v("RAIL", 2); v("POWERED_RAIL", 14); v("DETECTOR_RAIL", 6); v("ACTIVATOR_RAIL", 6); v("MINECART", 20);
        // Material-Stufen (Wert pro "Einheit" bei Werkzeug/Ruestung)
        TIER.put("WOODEN", 1L); TIER.put("STONE", 1L); TIER.put("LEATHER", 3L); TIER.put("CHAINMAIL", 6L);
        TIER.put("IRON", 10L); TIER.put("GOLDEN", 16L); TIER.put("DIAMOND", 200L); TIER.put("NETHERITE", 6000L);
        TIER.put("TURTLE", 20L); TIER.put("COPPER", 6L);
    }

    private static int units(String piece) {
        return switch (piece) {
            case "HELMET" -> 5;
            case "CHESTPLATE" -> 8;
            case "LEGGINGS" -> 7;
            case "BOOTS" -> 4;
            case "SWORD" -> 2;
            case "PICKAXE" -> 3;
            case "AXE" -> 3;
            case "SHOVEL" -> 1;
            case "HOE" -> 2;
            default -> 0;
        };
    }

    /** Verkaufswert pro Item (0 = nicht verkaufbar). */
    static long of(Material material) {
        if (material == null || Gui.blocked(material)) {
            return 0L;
        }
        String name = material.name();
        Long base = BASE.get(name);
        if (base != null) {
            return base;
        }
        int us = name.indexOf('_');
        if (us > 0) {
            String tier = name.substring(0, us);
            String piece = name.substring(us + 1);
            Long tierValue = TIER.get(tier);
            if (tierValue != null && units(piece) > 0) {
                long value = tierValue * units(piece);
                if (tier.equals("NETHERITE")) {
                    value += 3000L;
                }
                return Math.max(1L, value);
            }
        }
        String n = name.toLowerCase(Locale.ROOT);
        if (name.endsWith("_SPAWN_EGG") || name.contains("COMMAND")) {
            return 0L;
        }
        if (name.startsWith("MUSIC_DISC")) {
            return 250L;
        }
        if (name.endsWith("_SMITHING_TEMPLATE")) {
            return name.startsWith("NETHERITE") ? 6000L : 300L;
        }
        if (name.endsWith("_BANNER_PATTERN")) {
            return 60L;
        }
        if (name.endsWith("_POTTERY_SHERD")) {
            return 40L;
        }
        if (name.endsWith("_HORSE_ARMOR")) {
            return name.startsWith("DIAMOND") ? 400L : name.startsWith("GOLDEN") ? 200L : 120L;
        }
        if (name.endsWith("_SAPLING") || name.endsWith("_SEEDS") || name.endsWith("_PROPAGULE")) {
            return 1L;
        }
        if (name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.endsWith("_HYPHAE")) {
            return 2L;
        }
        if (name.endsWith("_PLANKS") || name.endsWith("_SLAB") || name.endsWith("_STAIRS") || name.endsWith("_FENCE") || name.endsWith("_WALL")) {
            return 1L;
        }
        if (name.endsWith("_WOOL") || name.endsWith("_CARPET") || name.endsWith("_CONCRETE") || name.endsWith("_TERRACOTTA") || name.endsWith("_GLASS") || name.endsWith("_GLASS_PANE")) {
            return 2L;
        }
        if (name.endsWith("_CONCRETE_POWDER") || name.endsWith("_DYE") || name.endsWith("_BED") || name.endsWith("_BANNER")) {
            return 3L;
        }
        if (name.endsWith("_SHULKER_BOX")) {
            return 3500L;
        }
        if (name.endsWith("_ORE")) {
            return 12L;
        }
        if (name.endsWith("_BLOCK") && (name.startsWith("RAW_"))) {
            return 60L;
        }
        if (name.endsWith("_HEAD") || name.endsWith("_SKULL")) {
            return 150L;
        }
        if (name.endsWith("_COPPER") || name.contains("COPPER")) {
            return 5L;
        }
        if (n.contains("potion") || n.contains("tipped")) {
            return 12L;
        }
        if (n.contains("boat") || n.contains("raft")) {
            return 8L;
        }
        return material.isSolid() ? 1L : 2L;
    }
}
