# Ein Kill beendet den SMP-Combat des Killers

Wer im SMP einen Spieler tötet, ist sofort nicht mehr im Combat (Meldung "Kill! Du bist nicht mehr im Combat.").
Umsetzung: `DuelNetworkPlugin.onDuelDeath`: `player.getKiller()` ermitteln, `clearSmpCombat(slayer)` aufrufen, wenn der Killer im Combat war.
