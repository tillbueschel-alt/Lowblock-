package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * Alle Texte der neuen Funktionen (Moderation, Mutes, Warps, Admin) stehen in plugins/DuelNetwork/messages.yml
 * und koennen dort angepasst werden. Farben mit &amp;, Platzhalter als {0}, {1} ...
 * Fehlende Eintraege werden beim Start automatisch mit dem Standardtext ergaenzt.
 */
final class Messages {
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<String, String>();
    private static YamlConfiguration cfg = new YamlConfiguration();
    private static File file;
    private static Plugin plugin;

    private Messages() {
    }

    private static void def(String key, String text) {
        DEFAULTS.put(key, text);
    }

    static {
        def("common.no-permission", "&cDafür fehlt dir die Berechtigung.");
        def("common.player-not-found", "&cSpieler konnte nicht gefunden werden.");
        def("common.searching", "&7Suche Spieler {0} ...");
        def("common.only-smp", "&cDas geht nur im SMP.");
        def("common.in-combat", "&cWegen des Combats kannst du dich nicht teleportieren.");

        def("mod.admin-only", "&cDafür brauchst du Admin- oder Owner-Rechte.");
        def("mod.ban.usage", "&dNutze: /ban <Spieler> [Dauer, z. B. 7d] <Grund>");
        def("mod.ban.done", "&a{0} wurde gebannt ({1}). Grund: {2}");
        def("mod.ban.kick-screen", "&cGebannt ({0}): {1}");
        def("mod.protected-ban", "&cOwner und Admins können nicht gebannt werden.");
        def("mod.protected-kick", "&cOwner und Admins können nicht gekickt werden.");
        def("mod.protected-mute", "&cOwner und Admins können nicht stummgeschaltet werden.");
        def("mod.kick.usage", "&dNutze: /kick <Spieler> [Grund]");
        def("mod.kick.not-online", "&cDieser Spieler ist nicht online.");
        def("mod.kick.default-reason", "Vom Server gekickt.");
        def("mod.kick.screen", "&cGekickt: {0}");
        def("mod.kick.done", "&a{0} wurde gekickt.");
        def("mod.unban.usage", "&dNutze: /unban <Spieler>");
        def("mod.unban.done", "&a{0} wurde entbannt.");
        def("mod.staff-notice", "&8[Mod] &7{0}");
        def("mod.log.usage", "&dNutze: /modlog <Spieler>");
        def("mod.log.none", "&7Keine Einträge für {0} gefunden.");
        def("mod.log.header", "&dModerations-Verlauf von {0} &8(letzte {1})");

        def("mute.usage", "&dNutze: /mute <Spieler> [Dauer, z. B. 2h] [Grund]");
        def("mute.unmute-usage", "&dNutze: /unmute <Spieler>");
        def("mute.default-reason", "Verstoß gegen die Chat-Regeln");
        def("mute.done", "&a{0} wurde stummgeschaltet ({1}). Grund: {2}");
        def("mute.target", "&cDu wurdest stummgeschaltet ({0}). Grund: {1}");
        def("mute.blocked", "&cDu bist stummgeschaltet (noch {0}). Grund: {1}");
        def("mute.unmuted", "&a{0} ist wieder freigeschaltet.");
        def("mute.not-muted", "&7{0} ist nicht stummgeschaltet.");
        def("mute.target-unmuted", "&aDu darfst wieder schreiben.");

        def("warp.usage", "&dNutze: /warp <Name> &8| &7Alle Warps: &d/warps");
        def("warp.unknown", "&cDen Warp \"{0}\" gibt es nicht.");
        def("warp.list", "&dWarps: &f{0}");
        def("warp.list-empty", "&7Es sind noch keine Warps gesetzt.");
        def("warp.set", "&aWarp \"{0}\" wurde gesetzt.");
        def("warp.deleted", "&aWarp \"{0}\" wurde gelöscht.");
        def("warp.bad-name", "&cErlaubt sind 1-24 Zeichen: a-z, 0-9, _ und -.");
        def("warp.limit", "&cEs sind höchstens 50 Warps möglich.");
        def("warp.setwarp-usage", "&dNutze: /setwarp <Name>");
        def("warp.delwarp-usage", "&dNutze: /delwarp <Name>");
        def("back.none", "&cEs gibt keinen Ort, zu dem du zurück kannst.");
        def("back.disabled", "&c/back ist ausgeschaltet.");

        def("admin.help", "&d/duelnetwork &7reload &8| &7info &8| &7backup &8| &7restore [list|<Datei> confirm] &8| &7debug [on|off]");
        def("admin.reloaded", "&aKonfiguration und Nachrichten neu geladen. &7(Pack-Modus und Ports brauchen einen Neustart.)");
        def("admin.backup-started", "&7Backup wird erstellt ...");
        def("admin.backup-done", "&aBackup erstellt: &f{0}");
        def("admin.backup-failed", "&cBackup fehlgeschlagen: {0}");
        def("admin.debug", "&7Debug-Ausgabe: &f{0}");
    }

    static {
        // Neue Funktionen (Freunde, Handel, Kosmetik, Seasons): Texte in messages.yml anpassbar
        def("friends.spieler-nicht-gefunden-er", "&cSpieler nicht gefunden (er muss online oder schon einmal da gewesen sein).");
        def("friends.du-kannst-dich-nicht", "&cDu kannst dich nicht selbst hinzufügen.");
        def("friends.ihr-seid-schon-befreundet", "&7Ihr seid schon befreundet.");
        def("friends.die-anfrage-ist-schon", "&7Die Anfrage ist schon unterwegs.");
        def("friends.nur-freunde-konnen-sich", "&cNur Freunde können sich teleportieren.");
        def("friends.wahrend-eines-duells-geht", "&cWährend eines Duells geht das nicht.");
        def("friends.keine-gultige-teleport-anfrage", "&cKeine (gültige) Teleport-Anfrage.");
        def("friends.wahrend-eines-duells-geht-2", "&cWährend eines Duells geht das nicht.");
        def("friends.nur-im-spiel", "Nur im Spiel.");
        def("friends.friend-add-spieler", "&c/friend add <Spieler>");
        def("friends.friend-accept-spieler", "&c/friend accept <Spieler>");
        def("friends.friend-deny-spieler", "&c/friend deny <Spieler>");
        def("friends.das-ist-kein-freund", "&cDas ist kein Freund von dir.");
        def("friends.friend-tp-freund-muss", "&c/friend tp <Freund> (muss online sein)");
        def("friends.du-hast-noch-keine", "&7Du hast noch keine Freunde. &d/friend add <Spieler>");
        def("trade.nur-im-spiel", "Nur im Spiel.");
        def("trade.trade-spieler-handel-anfragen", "&d/trade <Spieler> &7– Handel anfragen   &d/trade accept &7– annehmen   &d/trade money <Betrag> &7– Geld anbieten");
        def("trade.spieler-nicht-gefunden", "&cSpieler nicht gefunden.");
        def("trade.handeln-geht-nur-im", "&cHandeln geht nur im SMP / Bau-SMP und nicht im Duell.");
        def("trade.ihr-musst-im-gleichen", "&cIhr müsst im gleichen Modus sein (SMP oder Bau-SMP).");
        def("trade.einer-von-euch-handelt", "&cEiner von euch handelt gerade.");
        def("trade.keine-gultige-handelsanfrage", "&cKeine (gültige) Handelsanfrage.");
        def("trade.der-handel-ist-gerade", "&cDer Handel ist gerade nicht möglich.");
        def("trade.du-handelst-gerade-nicht", "&cDu handelst gerade nicht.");
        def("trade.trade-money-betrag", "&c/trade money <Betrag>");
        def("trade.so-viel-geld-hast", "&cSo viel Geld hast du nicht.");
        def("trade.ein-geldbetrag-ist-nicht", "&cEin Geldbetrag ist nicht mehr gedeckt.");
        def("trade.ein-geldbetrag-ist-nicht-2", "&cEin Geldbetrag ist nicht mehr gedeckt.");
        def("trade.ein-inventar-hat-nicht", "&cEin Inventar hat nicht genug Platz.");
        def("trade.ein-inventar-hat-nicht-2", "&cEin Inventar hat nicht genug Platz.");
        def("cosmetics.dein-inventar-ist-voll", "&cDein Inventar ist voll.");
        def("season.season-wurde-beendet", "&aSeason wurde beendet.");
        def("season.aktuelle-top-10-elo", "&7Aktuelle Top 10 (Elo):");
        def("season.noch-keine-duelle-in", " &8Noch keine Duelle in dieser Season.");
    }

    /** Beim Start aufrufen: laedt messages.yml und ergaenzt fehlende Eintraege. */
    static void init(Plugin owner) {
        plugin = owner;
        file = new File(owner.getDataFolder(), "messages.yml");
        reload();
    }

    static void reload() {
        if (file == null) {
            return;
        }
        cfg = YamlConfiguration.loadConfiguration(file);
        boolean changed = false;
        for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
            if (!cfg.contains(entry.getKey())) {
                cfg.set(entry.getKey(), entry.getValue());
                changed = true;
            }
        }
        if (changed) {
            try {
                Store.save(cfg, file);
            }
            catch (IOException e) {
                plugin.getLogger().warning("messages.yml konnte nicht gespeichert werden: " + e.getMessage());
            }
        }
    }

    /** Text holen, Farben umwandeln und {0}, {1} ... ersetzen. */
    static String get(String key, Object... args) {
        String text = cfg.getString(key, DEFAULTS.getOrDefault(key, key));
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
