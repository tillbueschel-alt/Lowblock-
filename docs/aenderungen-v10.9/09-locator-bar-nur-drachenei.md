# Locator-Bar zeigt nur das Drachenei

- Alle Spieler bekommen `waypoint_transmit_range` = 0 (sie sind auf der Leiste unsichtbar), nur wer das Drachenei trägt, sendet (Wert 6.0E7).
- Liegt das Ei am Schrein oder als Item herum, wird dort ein unsichtbarer Marker-Rüstungsständer (`lb_locator`, nicht persistent) mit Sendebereich gespawnt.
- Gibt es kein Ei, ist die Leiste leer. Aktualisierung alle 2 s und beim Join. Schalter `locator.dragon-egg-only` (Standard true).
- Alles per Reflection (`Attribute.WAYPOINT_TRANSMIT_RANGE`), fällt ohne Absturz zurück, falls die Server-Version das Attribut nicht hat. Der Marker-Teil ist ungetestet: kann ein Rüstungsständer das Attribut nicht haben, wirkt nur das Signal des Eiträgers.

Dateien: neue `Locator.java`, `DragonEgg.java` (`carries` ist package-private, `waypointSpot()`, `droppedEgg`), Registrierung in `DuelNetworkPlugin` (im `patch.diff`, der Registrierungs-Hunk enthält auch den Vein-Miner, siehe Feature 08).
