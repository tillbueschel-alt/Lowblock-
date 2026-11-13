# SMP/Bau-SMP-Zonen und Combat (V10.9)

**Lücken, die geschlossen wurden**
- **Combat-Flucht:** Ins Bau-SMP wechseln löschte den Combat (`enterBauMode` -> `clearSmpCombat`). `BauSmp.enter` lehnt jetzt im Combat ab.
- **Zonen-Sperre für Teleports:** Neuer Handler `onZoneTeleport` (`PlayerTeleportEvent`, HIGH): Wer im SMP-Profil ist, darf nur dann in das Bau-Quadrat teleportieren, wenn er im Bau-Modus ist - und umgekehrt (`insideBauBorder(to) != bauMode.contains(id)` -> abgebrochen). Das betrifft /home, /tpa, /rtp, Befehle und Plugin-Teleports. Admins/Creative sind ausgenommen. `BauSmp.enter` setzt dafür `plugin.bauTransit`.
- **Respawn:** `onZoneRespawn`: Bau-SMP-Spieler respawnen am Bau-Spawn, SMP-Spieler nie im Bau-Quadrat (z. B. wegen eines dort gesetzten Betts).
- **Combat-Log:** Wer im Combat ausloggt, stirbt wie bisher; zusätzlich gibt es eine Meldung an alle.

Dateien (komplett): `DuelNetworkPlugin.java`, `BauSmp.java` (siehe auch den Patch von Ordner 01).
