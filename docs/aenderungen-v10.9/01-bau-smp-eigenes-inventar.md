# Bau-SMP hat ein eigenes Inventar

**Ziel:** Das Bau-SMP ist ein anderer Bereich als das normale SMP und braucht ein eigenes Inventar (inkl. Rüstung und Zweithand).

**Verhalten**
- Betritt man das Bau-SMP, wird das SMP-Inventar gesichert und das Bau-Inventar geladen (beim ersten Mal leer).
- Wechsel ins SMP, in die Lobby, Duell-Start oder Server verlassen: Bau-Inventar sichern, SMP-Inventar wiederherstellen.
- Das Bau-Inventar wird wie die anderen Profile in der Config gespeichert (`persistProfile`/`loadPersistedProfiles`, Profil "bau").
- Bau-SMP bleibt ein Unterzustand des Profils `smp` (Geld, Rang bleiben gemeinsam). Die Menge `bauMode` (UUIDs) sagt, wer gerade im Bau-SMP ist.
- Seitenleiste: "BAU-SMP" wird nach `inBauMode(id)` gezeigt, nicht nach der Position.

**Umsetzung (`patch.diff`, gegen die V97):**
- `DuelNetworkPlugin`: Maps `bauItems/bauArmor/bauOffhand`, Menge `bauMode`, Methoden `offhandMap`, `inBauMode`, `enterBauMode`, `leaveBauMode`; `switchProfile` ruft zuerst `leaveBauMode`; Quit und Duell-Start rufen `leaveBauMode(player,false)`; `loadPersistedProfiles` lädt auch "bau".
- `BauSmp.enter`: nach dem Teleport `plugin.enterBauMode(player)`.
- `Sidebar`: Bedingung auf `inBauMode(id)`.

`ganze-dateien/` enthält die kompletten aktuellen Dateien (DuelNetworkPlugin.java enthält zusätzlich Feature 03).
