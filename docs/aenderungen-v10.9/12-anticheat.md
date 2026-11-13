# Anti-Cheat: Fixes, Befehle und Anti-X-Ray

**Fehlalarme behoben**
- FastBreak ignoriert die Extra-Blöcke von Hammer-Spitzhacke, Hammer-Schaufel, Fäller-Axt und Vein Miner (`AntiCheat.skipBreak`, nutzt `Gems.busyFor` und `VeinMiner.busyFor`). Standardgrenze 16 Blöcke/s (mit Eile +8).
- Explosionen (TNT, Creeper, Betten, Anker, Kristalle ...): `EntityExplodeEvent`/`BlockExplodeEvent` und Explosionsschaden setzen für alle Spieler im Umkreis von 16 Blöcken 6 s "Start" (`markLaunch`), damit Fly/Speed/Velocity/NoFall nicht anschlagen. Wind-Charges und Mace-Schübe wie bisher.
- Der "Gelandet"-Reset nach einem Start greift erst nach 1,5 s (verhindert, dass der Schutz am Absprung endet).

**Befehle** (`/anticheat`, Alias `/ac`, nur Admins)
- `on` / `off`: Anti-Cheat komplett ein-/ausschalten (wird in der Config gespeichert und im Log vermerkt, alle Admins werden informiert).
- `alerts [on|off]`: Warnungen für dich selbst. Warnungen erscheinen jetzt zusätzlich in der Actionbar der Admins, damit man sie ständig sieht.
- `xray`: Status und Hinweis zur Paper-Engine (`config/paper-world-defaults.yml` -> `anticheat.anti-xray`).

**Anti-X-Ray** (neue Datei `XRay.java`, Check "XRay"): zählt wertvolle Erze (Diamant, Smaragd, Antiker Schutt, Gold), die beim Abbau keine natürlich freie Seite hatten (selbst gegrabene Luft zählt nicht), im Verhältnis zu den abgebauten Steinen im Zeitfenster. Eine Ader zählt nur einmal. Konfig `anticheat.xray.*`. Im Bau-SMP, im Creative und mit Bypass-Recht wird nicht geprüft.

Dateien: `AntiCheat.java`, `XRay.java`, `Gems.java`, `VeinMiner.java`, `config.yml`, `plugin.yml`; Registrierung in `DuelNetworkPlugin` (Patch von Ordner 01 bzw. 08/09).
