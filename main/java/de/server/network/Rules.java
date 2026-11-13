package de.server.network;

/** Reine Rechenregeln (Gebuehren, Elo) - ohne Bukkit und damit testbar. */
final class Rules {
    private Rules() {
    }

    /** Erloes des Verkaeufers nach Abzug der Gebuehr; die Gebuehr wird auf 0-50 % begrenzt. */
    static long proceeds(long price, long taxPercent) {
        long tax = Math.max(0L, Math.min(50L, taxPercent));
        if (price <= 0L) {
            return 0L;
        }
        return price - price * tax / 100L;
    }

    /** Begrenzt einen angeforderten Listenbereich auf 0..size, auch bei fehlerhaften negativen Limits. */
    static int boundedLimit(int requested, int size) {
        return Math.max(0, Math.min(Math.max(0, requested), Math.max(0, size)));
    }

    /**
     * Elo-Berechnung fuer einen Sieg. Rueckgabe: {neuer Wert Sieger, neuer Wert Verlierer, Punkte Sieger, Punkte Verlierer}.
     * Der Verlierer faellt nie unter 100 (dann verliert er entsprechend weniger oder nichts).
     */
    static int[] elo(int winner, int loser, int k) {
        double expected = 1.0 / (1.0 + Math.pow(10.0, (loser - winner) / 400.0));
        int delta = (int)Math.round(Math.max(1, k) * (1.0 - expected));
        delta = Math.max(1, delta);
        int newLoser = Math.max(100, loser - delta);
        return new int[]{winner + delta, newLoser, delta, loser - newLoser};
    }
}
