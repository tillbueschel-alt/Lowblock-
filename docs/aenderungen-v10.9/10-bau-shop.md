# Eigener Shop im Bau-SMP (braucht Feature 01 wegen `inBauMode`)

`/shop` im Bau-SMP öffnet den "Bau-Shop" statt des normalen Shops. Feste, günstige Preise, kein PvP-Zubehör (keine Kristalle, Anker, TNT, Waffen).
Kategorien: **Overworld** (Stein, Holz, Glas, Beton, Wolle ...), **Nether** (Netherblöcke, Glowstone, Stämme, Krimson/Wirrwarr ...),
**End** (End-Stein, Purpur, End-Stäbe, Chorus, Obsidian, Enderkiste, Shulkerbox - als Ersatz, weil das End im Bau-SMP nicht offen ist),
**Flug-Shop** (Elytra 900, Raketen Flugdauer 3 für 8, Flugdauer 1 für 5; 900 liegt unter dem Startgeld 1000, also von Anfang an kaufbar) und **Essen**.
Preise stehen in der Konstante `BAU` in `Shop.java` (Format `MATERIAL:Preis[:Flugstärke]`).
Raketen werden per `FireworkMeta.setPower` erzeugt. Das normale SMP-Sortiment bleibt unverändert.

Datei: `Shop.java` (komplett).
