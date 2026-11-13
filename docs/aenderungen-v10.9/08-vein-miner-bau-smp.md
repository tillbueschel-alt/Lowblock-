# Vein Miner nur im Bau-SMP (braucht Feature 01 wegen `inBauMode`)

Wer im Bau-SMP (Survival, nicht schleichend) ein Erz mit einer Spitzhacke abbaut, baut die ganze zusammenhängende Ader ab.
- Gleiches Erz, 26er-Nachbarschaft (auch diagonal); `X_ORE` und `DEEPSLATE_X_ORE` zählen als dasselbe, dazu `ANCIENT_DEBRIS`.
- Maximal `bausmp.veinminer.max-blocks` (Standard 64), Schalter `bausmp.veinminer.enabled`.
- Pro Extra-Block 1 Haltbarkeit; ist die Spitzhacke fast kaputt, hört es mit Meldung auf.
- Pro Block wird ein `BlockBreakEvent` ausgelöst (Schutz-Plugins), Rekursionsschutz per `busy`-Menge.
- Die Gem-Hammer-Spitzhacke wird übersprungen (hat ihr eigenes 3x3).
- Kein XP-Drop für die Extra-Blöcke (`breakNaturally`).

Umsetzung: neue Datei `VeinMiner.java` (komplett in `ganze-dateien/`), Registrierung in `DuelNetworkPlugin.onEnable` (`patch.diff`), Config-Kommentar in `config.yml`.
