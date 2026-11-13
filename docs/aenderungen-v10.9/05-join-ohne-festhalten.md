# Neue Spieler werden beim Join nicht mehr festgehalten

**Problem:** Neue Spieler konnten sich am Anfang nicht bewegen (Warten aufs Texture-Pack, Standard 20 Sekunden).

**Änderung:** `PackHost.onJoin` sendet nur noch das Pack (nach 5 Ticks) und hält niemanden fest. Die Config-Einträge `resourcepack.hold-seconds` und `resourcepack.kick-on-missing` wurden gelöscht (alte Configs können sie behalten, sie werden ignoriert). `PACK_VERSION` 86 -> 87 (siehe Feature 06). Die Sperr-Handler (`holdMove` usw.) bleiben im Code, haben aber nie etwas zu tun, weil die Menge `pending` leer bleibt.
