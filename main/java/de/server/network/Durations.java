package de.server.network;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reine Hilfsfunktionen fuer Zeitangaben (ohne Bukkit, damit sie sich einfach testen lassen). */
final class Durations {
    private static final Pattern DURATION = Pattern.compile("^(\\d{1,6})([smhdw])$", Pattern.CASE_INSENSITIVE);

    private Durations() {
    }

    /** Dauer in Millisekunden ("30m", "12h", "7d", "2w", "45s") oder -1, wenn der Text keine Dauer ist. */
    static long parse(String token) {
        if (token == null) {
            return -1L;
        }
        Matcher m = DURATION.matcher(token.trim());
        if (!m.matches()) {
            return -1L;
        }
        long amount = Long.parseLong(m.group(1));
        return switch (Character.toLowerCase(m.group(2).charAt(0))) {
            case 's' -> amount * 1000L;
            case 'm' -> amount * 60_000L;
            case 'h' -> amount * 3_600_000L;
            case 'd' -> amount * 86_400_000L;
            default -> amount * 7L * 86_400_000L;
        };
    }

    /** Lesbare Darstellung, 0 oder weniger = "dauerhaft". */
    static String describe(long ms) {
        if (ms <= 0L) {
            return "dauerhaft";
        }
        long minutes = ms / 60_000L;
        if (minutes < 120L) {
            return Math.max(1L, minutes) + " Min.";
        }
        long hours = minutes / 60L;
        if (hours < 72L) {
            return hours + " Std.";
        }
        return (hours / 24L) + " Tage";
    }
}
