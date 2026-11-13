# Bau-SMP hat ein eigenes Auktionshaus (braucht Feature 01)

- Jedes Angebot bekommt `.zone` (`smp` oder `bau`). Alte Angebote ohne Zone zählen als `smp`.
- "Angebote" und "Meine Angebote" zeigen nur die eigene Zone; im Bau-SMP steht " (Bau)" im Titel.
- Abholungen getrennt: `claims.<uuid>` (SMP) und `claimsbau.<uuid>` (Bau).
- Zone = `plugin.inBauMode(uuid)`. Geld bleibt gemeinsam.

Umsetzung: nur `AuctionHouse.java` (`zone`, `claimKey`, `addClaim/claims` mit Zone, Filter in der Listenansicht).
