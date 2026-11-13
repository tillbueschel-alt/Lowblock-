# Änderungen seit deiner V97 (Stand V10.9)

Jeder Ordner ist ein Feature: `ANLEITUNG.md`, `patch.diff` (gegen die V97, `patch -p1`) und `ganze-dateien/` (kompletter aktueller Stand der Dateien).
Falls ein Patch nicht sauber passt, nimm die Dateien aus `ganze-dateien/`. `DuelNetworkPlugin.java` ist überall dieselbe Gesamtdatei.

1. 01-bau-smp-eigenes-inventar (Basis für 02, 08, 10)
2. 02-bau-smp-eigenes-auktionshaus (braucht 01)
3. 03-kill-beendet-combat
4. 04-gem-werkzeuge (Zuverlässigkeits-Fix, neue Schaufel, Admin-Funktionen)
5. 05-join-ohne-festhalten
6. 06-item-texturen-vanilla
7. 07-texture-pack
8. 08-vein-miner-bau-smp (neue Datei VeinMiner.java)
9. 09-locator-bar-nur-drachenei (neue Datei Locator.java)
10. 10-bau-shop
11. 11-schaden-fix-lobby-schutz
12. 12-anticheat (Fehlalarm-Fixes, /anticheat on|off, Anti-X-Ray)

Nur gegen API-Stubs kompiliert, nicht auf einem echten Server getestet. Dateien, die in keinem Ordner stehen, sind gegenüber der V97 unverändert.
13. 13-kleinigkeiten (Drachenei nicht versteckbar, Config-Defaults, /gems Tab-Completion; nur ganze Dateien)
14. 14-afk-und-abbau-effekte (AFK-Schutz, Abbau-Effekte/XP; nur ganze Dateien)
15. 15-zonen-und-combat (Combat-Flucht ins Bau-SMP, Zonen-Sperre für Teleports, Respawn, Combat-Log-Meldung; nur ganze Dateien)
