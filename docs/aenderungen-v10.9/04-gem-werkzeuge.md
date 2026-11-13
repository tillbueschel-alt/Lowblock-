# Gem-Werkzeuge: Hammer-Spitzhacke, Fäller-Axt, neu Hammer-Schaufel (+ Admin-Funktionen)

**Zuverlässigkeit (Fix für "funktioniert mal ja, mal nein")**
- Erkennung: Name ohne Farbcodes (`strip`) plus Lore-Zeile "Gem-Item" - nicht mehr der exakte farbige Name (Paper gibt Namen nicht immer identisch zurück).
- Richtung: die angeschaute Fläche wird per Strahlverfolgung (`faceNormal`) bestimmt, die 3x3-Ebene liegt immer senkrecht zur Fläche (vorher: nur grob nach Blickwinkel).
- Baumfällen: Laub darf bis 2 Blöcke von irgendeinem Stamm entfernt sein (vorher nur direkt am Stamm, deshalb oft nichts), bis zu 256 Stämme, auch Nether-Bäume.
- Schutz für Bauten: Treppen, Stufen, Mauern, Ziegel, polierte/glatte/gemeißelte Blöcke, Redstone-Zeug usw. werden vom Hammer nie mitgenommen; Obsidian/Antiker Schutt nur, wenn man genau das abbaut.
- Fehler in der Logik werden gefangen und geloggt statt still zu scheitern; Warnungen höchstens alle 10 s.

**Neu: Hammer-Schaufel** (`gems.price.shovel`, Standard 600): 3x3 für Erde, Gras, Sand, Kies, Ton, Seelensand usw.
Gem-Shop-Layout: Spitzhacke Slot 10, Axt 12, Schaufel 14, Admin-Button 16.

**Admins** (OP oder `duelnetwork.admin`): Werkzeuge kostenlos, "+1000 Gems"-Button, `/gems give <Anzahl>` bzw. `/gems give <Spieler> <Anzahl>`.
Schleichen = nur ein Block.

Dateien: `Gems.java` (komplett), Config-Eintrag `gems.price.shovel`. Für den Compiler braucht man `Material.DIAMOND_SHOVEL`, `Block.getX/Y/Z` (normale Paper-API).
