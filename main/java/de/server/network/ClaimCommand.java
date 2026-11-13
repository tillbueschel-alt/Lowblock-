package de.server.network;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class ClaimCommand implements CommandExecutor, TabCompleter {
    private final DuelNetworkPlugin plugin;
    private final ClaimManager claims;

    ClaimCommand(DuelNetworkPlugin plugin, ClaimManager claims) { this.plugin = plugin; this.claims = claims; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Dieser Befehl ist nur im Spiel verfügbar."); return true; }
        if (!player.hasPermission("duelnetwork.claim.use") && !player.hasPermission("duelnetwork.claim.admin")) { player.sendMessage("§cDu darfst Claims nicht nutzen."); return true; }
        String sub = args.length == 0 ? "menu" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "menu" -> { if (ClaimMenu.instance != null) ClaimMenu.instance.open(player); else info(player); }
            case "chunk" -> { if (ClaimMenu.instance != null) ClaimMenu.instance.claimChunk(player); }
            case "map", "karte" -> {
                org.bukkit.Location base = player.getLocation();
                int cx = base.getBlockX() >> 4;
                int cz = base.getBlockZ() >> 4;
                player.sendMessage("§d§l✦ Claim-Karte §8(Norden oben, jedes Feld = 1 Chunk)");
                for (int dz = -4; dz <= 4; dz++) {
                    StringBuilder row = new StringBuilder();
                    for (int dx = -7; dx <= 7; dx++) {
                        if (dx == 0 && dz == 0) {
                            row.append("§f◆");
                            continue;
                        }
                        Claim c = claims.find(new org.bukkit.Location(base.getWorld(), ((cx + dx) << 4) + 8, base.getY(), ((cz + dz) << 4) + 8));
                        if (c == null) row.append("§8■");
                        else if (c.owner.equals(player.getUniqueId())) row.append("§a■");
                        else if (c.roles.containsKey(player.getUniqueId())) row.append("§b■");
                        else row.append("§c■");
                    }
                    player.sendMessage(row.toString());
                }
                player.sendMessage("§f◆ §7du  §a■ §7deins  §b■ §7du bist Mitglied  §c■ §7fremd  §8■ §7frei");
            }
            case "show", "border" -> { if (ClaimMenu.instance != null) ClaimMenu.instance.showBorder(player); }
            case "log", "protokoll" -> { if (ClaimLog.instance != null) ClaimLog.instance.show(player); }
            case "rollback" -> {
                if (ClaimLog.instance == null || args.length < 2) { player.sendMessage("§cNutze: /claim rollback <Spieler> [Minuten]"); }
                else {
                    int minutes = 60;
                    if (args.length >= 3) { try { minutes = Integer.parseInt(args[2]); } catch (NumberFormatException ex) { player.sendMessage("§cMinuten müssen eine Zahl sein."); return true; } }
                    ClaimLog.instance.rollback(player, args[1], minutes);
                }
            }
            case "invite", "einladen" -> invite(player, args);
            case "accept" -> accept(player, args);
            case "tp", "home" -> teleport(player, args);
            case "visitors", "besucher" -> visitors(player);
            case "create" -> create(player, args);
            case "info" -> info(player);
            case "list" -> list(player);
            case "trust", "member" -> role(player, args, "trusted");
            case "untrust", "unmember" -> unrole(player, args);
            case "delete", "abandon" -> delete(player);
            case "reload" -> { if (!player.hasPermission("duelnetwork.claim.admin")) player.sendMessage("§cKeine Berechtigung."); else { try { claims.reload(); player.sendMessage("§aClaims neu geladen."); } catch (Exception ex) { player.sendMessage("§cClaims konnten nicht geladen werden."); } } }
            default -> help(player);
        }
        return true;
    }

    private void create(Player player, String[] args) {
        String name = args.length >= 2 ? args[1].replaceAll("[^A-Za-z0-9_-]", "").substring(0, Math.min(24, args[1].replaceAll("[^A-Za-z0-9_-]", "").length())) : "home";
        if (name.isEmpty()) name = "home";
        int radius = plugin.getConfig().getInt("claims.default-radius", 8);
        if (args.length >= 3) try { radius = Integer.parseInt(args[2]); } catch (NumberFormatException ignored) { }
        Claim claim = claims.create(player, name, radius);
        if (claim == null) { player.sendMessage("§cClaim konnte nicht erstellt werden. Prüfe SMP-Zone, Überschneidungen, Größe und dein Limit."); return; }
        player.sendMessage("§aClaim §f" + claim.name + " §aerstellt: §7" + claim.minX + "," + claim.minZ + " bis " + claim.maxX + "," + claim.maxZ + " §8(" + claim.area() + " Blöcke)");
    }

    private record Invite(long claimId, String role, long expires) { }
    private final java.util.Map<UUID, Invite> invites = new java.util.HashMap<>();

    /** /claim invite <Spieler> [truhen]: Einladung mit Klick-Buttons; Standard ist "Bauen & alles". */
    private void invite(Player player, String[] args) {
        Claim claim = current(player);
        if (claim == null || !claim.owner.equals(player.getUniqueId()) || args.length < 2) {
            player.sendMessage("§cStell dich in deinen Claim und nutze §d/claim invite <Spieler> [truhen]§c.");
            return;
        }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null || target.equals(player)) { player.sendMessage("§cSpieler muss online sein."); return; }
        String role = args.length >= 3 && args[2].equalsIgnoreCase("truhen") ? "trusted" : "member";
        invites.put(target.getUniqueId(), new Invite(claim.id, role, System.currentTimeMillis() + 300000L));
        player.sendMessage("§aEinladung an §f" + target.getName() + " §agesendet §7(" + ("trusted".equals(role) ? "Truhen & Türen" : "Bauen & alles") + ").");
        target.sendMessage("§d✦ §f" + player.getName() + " §7lädt dich in den Claim §f" + claim.name + " §7ein (" + ("trusted".equals(role) ? "Truhen & Türen" : "Bauen & alles") + ").");
        Chat.buttons(target, " ", new String[]{"§a§l[Annehmen]", "/claim accept " + player.getName(), "§7Einladung annehmen"});
        Chat.ping(target);
    }

    private void accept(Player player, String[] args) {
        Invite invite = invites.remove(player.getUniqueId());
        if (invite == null || invite.expires() < System.currentTimeMillis()) { player.sendMessage("§cKeine offene Einladung (sie läuft nach 5 Minuten ab)."); return; }
        Claim claim = claims.byId(invite.claimId());
        if (claim != null && claims.acceptInvite(claim, player.getUniqueId(), invite.role())) {
            player.sendMessage("§aDu bist jetzt im Claim §f" + claim.name + "§a dabei.");
            Chat.yes(player);
            Player owner = plugin.getServer().getPlayer(claim.owner);
            if (owner != null) owner.sendMessage("§a" + player.getName() + " hat deine Einladung angenommen.");
        } else {
            player.sendMessage("§cDer Claim existiert nicht mehr.");
        }
    }

    private void teleport(Player player, String[] args) {
        if (!plugin.inBauMode(player.getUniqueId()) || plugin.activeDuelPlayers.contains(player.getUniqueId())) {
            player.sendMessage("§cDas geht nur im Bau-SMP.");
            return;
        }
        List<Claim> own = claims.owned(player.getUniqueId());
        if (own.isEmpty()) { player.sendMessage("§7Du besitzt keine Claims."); return; }
        Claim target = null;
        if (args.length >= 2) {
            for (Claim claim : own) if (claim.name.equalsIgnoreCase(args[1])) target = claim;
        } else if (own.size() == 1) {
            target = own.get(0);
        }
        if (target == null) {
            StringBuilder names = new StringBuilder();
            for (Claim claim : own) names.append(claim.name).append(' ');
            player.sendMessage("§7Nutze §d/claim tp <Name> §7– deine Claims: §f" + names.toString().trim());
            return;
        }
        org.bukkit.World world = plugin.getServer().getWorld(target.world);
        if (world == null) { player.sendMessage("§cDie Welt dieses Claims existiert nicht mehr."); return; }
        int x = (target.minX + target.maxX) / 2;
        int z = (target.minZ + target.maxZ) / 2;
        org.bukkit.Location where = new org.bukkit.Location(world, x + 0.5, world.getHighestBlockYAt(x, z) + 1.0, z + 0.5);
        player.teleportAsync(where);
        player.sendMessage("§aTeleport zu deinem Claim §f" + target.name + "§a.");
    }

    private void visitors(Player player) {
        Claim claim = current(player);
        if (claim == null) { player.sendMessage("§7Hier ist kein Claim."); return; }
        StringBuilder out = new StringBuilder();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.equals(player) && claim.contains(online.getLocation())) out.append(online.getName()).append(' ');
        }
        player.sendMessage(out.length() == 0 ? "§7Gerade ist niemand sonst im Claim." : "§7Im Claim §f" + claim.name + "§7: §f" + out.toString().trim());
    }

    private Claim current(Player player) {
        return claims.find(player.getLocation());
    }

    private void info(Player player) {
        Claim claim = current(player);
        if (claim == null) { player.sendMessage("§7Hier ist kein Claim."); return; }
        player.sendMessage("§dClaim §f" + claim.name + " §8#" + claim.id + " §7von §f" + name(claim.owner));
        player.sendMessage("§7Bereich: §f" + claim.minX + "," + claim.minZ + " §7bis §f" + claim.maxX + "," + claim.maxZ + " §8(" + claim.area() + " Blöcke)");
        if (!claim.roles.isEmpty()) {
            StringBuilder roles = new StringBuilder("§7Rollen: ");
            claim.roles.forEach((id, role) -> roles.append("§f").append(name(id)).append(" §8(").append(role).append(") "));
            player.sendMessage(roles.toString());
        }
    }

    private void list(Player player) {
        List<Claim> own = claims.owned(player.getUniqueId());
        if (own.isEmpty()) { player.sendMessage("§7Du besitzt keine Claims."); return; }
        player.sendMessage("§dDeine Claims:");
        for (Claim claim : own) player.sendMessage("§7- §f" + claim.name + " §8#" + claim.id + " §7(" + claim.world + ", " + claim.area() + " Blöcke)");
    }

    private void role(Player player, String[] args, String role) {
        Claim claim = current(player);
        if (claim == null || !claim.owner.equals(player.getUniqueId()) || args.length < 2) { player.sendMessage("§cDu musst Besitzer sein und einen Spielernamen angeben."); return; }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) { player.sendMessage("§cSpieler muss online sein."); return; }
        if (claims.setRole(player, claim, target.getUniqueId(), role)) player.sendMessage("§a" + target.getName() + " ist jetzt " + role + " im Claim."); else player.sendMessage("§cRolle konnte nicht gesetzt werden.");
    }

    private void unrole(Player player, String[] args) {
        Claim claim = current(player);
        if (claim == null || !claim.owner.equals(player.getUniqueId()) || args.length < 2) { player.sendMessage("§cDu musst Besitzer sein und einen Spielernamen angeben."); return; }
        Player target = plugin.getServer().getPlayerExact(args[1]);
        if (target == null) { player.sendMessage("§cSpieler muss online sein."); return; }
        if (claims.removeRole(player, claim, target.getUniqueId())) player.sendMessage("§aRolle von " + target.getName() + " entfernt."); else player.sendMessage("§cRolle konnte nicht entfernt werden.");
    }

    private void delete(Player player) {
        Claim claim = current(player);
        if (claims.delete(player, claim)) player.sendMessage("§aClaim gelöscht."); else player.sendMessage("§cNur der Besitzer kann den Claim löschen.");
    }

    private String name(UUID id) {
        Player player = plugin.getServer().getPlayer(id);
        return player == null ? id.toString().substring(0, 8) : player.getName();
    }

    private void help(Player player) { player.sendMessage("§d/claim §7(Menü) §7| §dchunk §7| §dshow §7| §dcreate <Name> [Radius] §7| §dinfo §7| §dlist §7| §dtrust/untrust <Spieler> §7| §ddelete"); }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) for (String value : List.of("menu", "chunk", "map", "show", "log", "rollback", "invite", "accept", "tp", "visitors", "create", "info", "list", "trust", "untrust", "delete", "abandon", "reload")) if (value.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(value);
        else if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("trust") || args[0].equalsIgnoreCase("untrust") || args[0].equalsIgnoreCase("member") || args[0].equalsIgnoreCase("unmember"))) for (Player p : plugin.getServer().getOnlinePlayers()) out.add(p.getName());
        return out;
    }
}
