package de.server.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

/**
 * Claim-Menue (Bau-SMP): Chunk claimen, Mitglieder (Freunde per Klick), Grenze mit Partikeln anzeigen,
 * Betreten-/Verlassen-Hinweis in der Actionbar.
 * Mitglieds-Klick wechselt die Rolle: keine -> "Bauen" (member) -> "Truhen & Tueren" (trusted) -> keine.
 */
final class ClaimMenu implements Listener {
    static ClaimMenu instance;
    private static final String TAG = "Claim";
    private static final String TAG_MEMBERS = "Claim-Rechte";
    private static final int[] MEMBER_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};

    private final DuelNetworkPlugin plugin;
    private final ClaimManager claims;
    private final Map<UUID, Long> lastClaim = new HashMap<UUID, Long>();
    private final Map<UUID, Long> deleteArmed = new HashMap<UUID, Long>();
    private final Map<UUID, List<UUID>> memberView = new HashMap<UUID, List<UUID>>();
    private final Set<UUID> showing = new HashSet<UUID>();

    ClaimMenu(DuelNetworkPlugin plugin, ClaimManager claims) {
        this.plugin = plugin;
        this.claims = claims;
        instance = this;
    }

    private Claim here(Player player) {
        return this.claims.find(player.getLocation());
    }

    private String ownerName(Claim claim) {
        org.bukkit.OfflinePlayer owner = this.plugin.getServer().getOfflinePlayer(claim.owner);
        return owner.getName() == null ? claim.owner.toString().substring(0, 8) : owner.getName();
    }

    void open(Player player) {
        Claim claim = this.here(player);
        boolean mine = claim != null && claim.owner.equals(player.getUniqueId());
        Inventory inv = Gui.frame(this.plugin, TAG, "Land-Claim", 36);
        inv.setItem(10, Gui.item(Material.GRASS_BLOCK, "§a§lChunk claimen", "§7Schützt den Chunk (16×16), in dem du stehst.",
                "§7Deine Claims: §f" + this.claims.owned(player.getUniqueId()).size() + "§7/§f" + this.plugin.getConfig().getInt("claims.max-per-player", 3), "", "§e▶ Klicken"));
        if (claim == null) {
            inv.setItem(12, Gui.item(Material.PAPER, "§7Hier ist kein Claim", "§8Stell dich in einen Claim für Details."));
        } else {
            List<String> lore = new ArrayList<String>();
            lore.add("§7Besitzer: §f" + this.ownerName(claim));
            lore.add("§7Größe: §f" + claim.area() + " Blöcke");
            lore.add("§7Mitglieder: §f" + Math.max(0, claim.roles.size() - 1));
            inv.setItem(12, Gui.item(Material.PAPER, "§d§lClaim §f" + claim.name, lore.toArray(new String[0])));
        }
        inv.setItem(14, Gui.item(Material.PLAYER_HEAD, mine ? "§b§lMitglieder & Rechte" : "§7Mitglieder & Rechte",
                mine ? new String[]{"§7Füge Freunde hinzu oder entferne sie.", "", "§e▶ Klicken"} : new String[]{"§8Nur der Besitzer kann das ändern."}));
        inv.setItem(16, Gui.item(Material.LANTERN, "§e§lGrenze anzeigen", "§7Zeigt den Rand deines Claims", "§7zehn Sekunden mit Partikeln.", "", "§e▶ Klicken"));
        boolean open = claim != null && this.claims.visitorInteract(claim);
        inv.setItem(20, Gui.item(open ? Material.LIME_DYE : Material.GRAY_DYE, open ? "§aBesucher-Modus: an" : "§7Besucher-Modus: aus",
                "§7An: Fremde dürfen Türen, Knöpfe und Hebel", "§7benutzen. Bauen und Kisten bleiben gesperrt.", mine ? "§e▶ Klicken zum Umschalten" : "§8Nur der Besitzer."));
        inv.setItem(22, Gui.item(Material.WRITABLE_BOOK, "§b§lEinladen", "§7Lade jemanden per Befehl ein:", "§d/claim invite <Spieler> [truhen]", "§7Er bekommt Buttons zum Annehmen."));
        inv.setItem(24, Gui.item(Material.TNT, mine ? "§c§lClaim löschen" : "§7Claim löschen", mine ? new String[]{"§7Zweimal klicken zum Bestätigen."} : new String[]{"§8Nur der Besitzer."}));
        player.openInventory(inv);
    }

    void claimChunk(Player player) {
        Claim claim = this.claims.createChunk(player);
        if (claim == null) {
            player.sendMessage("§cHier kann kein Claim erstellt werden. §7Nur im Bau-SMP, nicht überlappend und im Rahmen deines Limits.");
            Chat.no(player);
            return;
        }
        player.sendMessage("§aChunk geclaimt: §f" + claim.name + " §7(" + claim.minX + "," + claim.minZ + " bis " + claim.maxX + "," + claim.maxZ + ")");
        Chat.yes(player);
        this.showBorder(player);
    }

    void showBorder(Player player) {
        Claim claim = this.here(player);
        if (claim == null) {
            player.sendMessage("§7Hier ist kein Claim.");
            return;
        }
        UUID id = player.getUniqueId();
        if (!this.showing.add(id)) {
            return;
        }
        final Claim target = claim;
        new org.bukkit.scheduler.BukkitRunnable() {
            int runs;
            @Override
            public void run() {
                if (!player.isOnline() || ++this.runs > 20) {
                    ClaimMenu.this.showing.remove(id);
                    this.cancel();
                    return;
                }
                double y = player.getLocation().getY() + 1.0;
                int step = Math.max(1, (int)Math.ceil((target.maxX - target.minX + 1) / 24.0));
                for (int x = target.minX; x <= target.maxX + 1; x += step) {
                    player.spawnParticle(Particle.END_ROD, x, y, target.minZ, 1, 0, 0, 0, 0);
                    player.spawnParticle(Particle.END_ROD, x, y, target.maxZ + 1, 1, 0, 0, 0, 0);
                }
                for (int z = target.minZ; z <= target.maxZ + 1; z += step) {
                    player.spawnParticle(Particle.END_ROD, target.minX, y, z, 1, 0, 0, 0, 0);
                    player.spawnParticle(Particle.END_ROD, target.maxX + 1, y, z, 1, 0, 0, 0, 0);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 10L);
    }

    private void openMembers(Player player) {
        Claim claim = this.here(player);
        if (claim == null || !claim.owner.equals(player.getUniqueId())) {
            player.sendMessage("§cNur der Besitzer kann Mitglieder ändern.");
            Chat.no(player);
            return;
        }
        List<UUID> people = new ArrayList<UUID>();
        if (Friends.instance != null) {
            people.addAll(Friends.instance.friendsOf(player.getUniqueId()));
        }
        for (UUID member : claim.roles.keySet()) {
            if (!member.equals(claim.owner) && !people.contains(member)) {
                people.add(member);
            }
        }
        this.memberView.put(player.getUniqueId(), people);
        Inventory inv = Gui.frame(this.plugin, TAG_MEMBERS, "Mitglieder", 54);
        inv.setItem(4, Gui.item(Material.PAPER, "§d§lClaim §f" + claim.name, "§7Klick auf einen Kopf wechselt:", "§7keine §8→ §aBauen §8→ §6Truhen & Türen §8→ §7keine"));
        for (int i = 0; i < people.size() && i < MEMBER_SLOTS.length; i++) {
            UUID id = people.get(i);
            org.bukkit.OfflinePlayer off = this.plugin.getServer().getOfflinePlayer(id);
            String role = claim.roles.get(id);
            String state = "member".equals(role) ? "§aBauen & alles" : "trusted".equals(role) ? "§6Truhen & Türen" : "§7kein Zugriff";
            ItemStack head = Gui.item(Material.PLAYER_HEAD, "§f" + (off.getName() == null ? id.toString().substring(0, 8) : off.getName()), "§7Recht: " + state, "", "§e▶ Klicken zum Wechseln");
            if (head.getItemMeta() instanceof SkullMeta meta) {
                meta.setOwningPlayer(off);
                head.setItemMeta(meta);
            }
            inv.setItem(MEMBER_SLOTS[i], head);
        }
        if (people.isEmpty()) {
            inv.setItem(22, Gui.item(Material.BARRIER, "§7Noch keine Freunde", "§8Füge Freunde mit §f/friend §8hinzu."));
        }
        inv.setItem(49, Gui.item(Material.ARROW, "§7« Zurück"));
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        boolean main = Gui.isTitle(title, TAG);
        boolean members = Gui.isTitle(title, TAG_MEMBERS);
        if (!main && !members) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        int slot = event.getSlot();
        if (main) {
            switch (slot) {
                case 10 -> { player.closeInventory(); this.claimChunk(player); }
                case 14 -> this.openMembers(player);
                case 16 -> { player.closeInventory(); this.showBorder(player); }
                case 20 -> {
                    Claim c = this.here(player);
                    if (c != null && this.claims.setVisitorInteract(player, c, !this.claims.visitorInteract(c))) {
                        Chat.click(player);
                    } else {
                        Chat.no(player);
                    }
                    this.open(player);
                }
                case 24 -> this.tryDelete(player);
                default -> { }
            }
            return;
        }
        if (slot == 49) {
            this.open(player);
            return;
        }
        Claim claim = this.here(player);
        List<UUID> people = this.memberView.get(player.getUniqueId());
        if (claim == null || people == null) {
            return;
        }
        for (int i = 0; i < MEMBER_SLOTS.length && i < people.size(); i++) {
            if (MEMBER_SLOTS[i] != slot) {
                continue;
            }
            UUID target = people.get(i);
            String role = claim.roles.get(target);
            boolean ok = role == null ? this.claims.setRole(player, claim, target, "member")
                    : "member".equals(role) ? this.claims.setRole(player, claim, target, "trusted")
                    : this.claims.removeRole(player, claim, target);
            if (ok) {
                Chat.click(player);
            } else {
                Chat.no(player);
            }
            this.openMembers(player);
            return;
        }
    }

    private void tryDelete(Player player) {
        Claim claim = this.here(player);
        if (claim == null || !claim.owner.equals(player.getUniqueId())) {
            Chat.no(player);
            return;
        }
        long now = System.currentTimeMillis();
        Long armed = this.deleteArmed.get(player.getUniqueId());
        if (armed != null && now - armed < 8000L) {
            this.deleteArmed.remove(player.getUniqueId());
            if (this.claims.delete(player, claim)) {
                player.sendMessage("§aClaim gelöscht.");
                Chat.yes(player);
            }
            player.closeInventory();
            return;
        }
        this.deleteArmed.put(player.getUniqueId(), now);
        player.sendMessage("§cNoch einmal klicken, um den Claim §f" + claim.name + " §cendgültig zu löschen.");
        Chat.no(player);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        String title = event.getView().getTitle();
        if (Gui.isTitle(title, TAG) || Gui.isTitle(title, TAG_MEMBERS)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getTo() == null || (event.getFrom().getBlockX() >> 2 == event.getTo().getBlockX() >> 2
                && event.getFrom().getBlockZ() >> 2 == event.getTo().getBlockZ() >> 2)) {
            return;
        }
        Player player = event.getPlayer();
        if (!Prefs.on(player.getUniqueId(), "claim")) {
            return;
        }
        Claim now = this.claims.find(event.getTo());
        long id = now == null ? -1L : now.id;
        Long before = this.lastClaim.get(player.getUniqueId());
        if (before != null && before == id) {
            return;
        }
        this.lastClaim.put(player.getUniqueId(), id);
        if (before == null && now == null) {
            return;
        }
        if (now != null) {
            String mine = now.owner.equals(player.getUniqueId()) ? "§adein Claim" : "§7Claim von §f" + this.ownerName(now);
            player.sendActionBar(net.kyori.adventure.text.Component.text("§d✦ §fClaim §d" + now.name + " §8• " + mine));
        } else {
            player.sendActionBar(net.kyori.adventure.text.Component.text("§7Du verlässt den Claim."));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        this.lastClaim.remove(id);
        this.deleteArmed.remove(id);
        this.memberView.remove(id);
        this.showing.remove(id);
    }
}
