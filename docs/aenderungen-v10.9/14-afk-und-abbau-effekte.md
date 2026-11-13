# AFK-Schutz fürs Einkommen und echte Abbau-Effekte (V10.8)

- **AFK:** `Economy.payIncome` vergleicht die Position mit der vom letzten Einkommen (vor 5 Minuten). Gleicher Bereich (unter 1,5 Blöcke) und gleiche Blickrichtung = AFK: kein Geld, kein Gem. Schalter `economy.afk-check` (Standard true).
- **Gems nur im echten SMP:** Im Bau-SMP gibt es kein Gem (`!plugin.inBauMode`), Geld gibt es weiter.
- **Hammer, Schaufel, Axt, Vein Miner:** `breakNaturally(tool, true, true)`: Abbau-Partikel/-Ton und Erz-Erfahrung; Rückfall auf die alte Methode bei `NoSuchMethodError`.

Dateien (komplett): `Economy.java`, `Gems.java`, `VeinMiner.java`, `DuelNetworkPlugin.java`, `config.yml`.
