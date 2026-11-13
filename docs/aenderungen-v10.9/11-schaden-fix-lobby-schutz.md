# Fix: SMP-Spieler nehmen keinen Schaden (Explosionen u.a.)

**Ursache:** `onLobbyDamage`, `onLobbyCombat`, `onLobbyBlockBreak/Place` und `ProtectionListener.inLobby` prüften nur "liegt die Position im Lobby-Quadrat (Radius 200)". Liegt das SMP/Bau-SMP in derselben Welt innerhalb dieses Quadrats, waren auch SMP-Spieler geschützt (kein Schaden, kein Bauen).

**Fix:** neue Methode `DuelNetworkPlugin.lobbyProtected(player, location)` = Profil ist nicht `smp`, nicht im Duell/Party-Match und Position im Lobby-Quadrat. Alle genannten Handler benutzen sie; Schaden an Nicht-Spielern wird nicht mehr blockiert. `DuelFixes.onDamageGuard` macht ausserhalb eines Duells nie unverwundbar.

Dateien: `DuelNetworkPlugin.java`, `ProtectionListener.java`, `DuelFixes.java` (komplett in `ganze-dateien/`).
