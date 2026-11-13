package de.server.network;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

/**
 * Einsteiger-Hilfe und Profil:
 * /tutorial [Seite] - kurze Erklaerung der Modi (beim ersten Beitritt wird darauf hingewiesen),
 * /stats [Spieler] - Profil-Menue mit Elo, Platz, Siegen, Serien, Season, Gems und Geld,
 * /ranked - zwischen Ranked (Elo zaehlt) und Casual (Elo bleibt) umschalten.
 * Beim ersten Beitritt gibt es zusaetzlich einen Tipp zu FPS-Mods (config welcome.client-tip).
 */
final class Guide implements CommandExecutor, TabCompleter, Listener {
    private static final String TAG = "Profil";

    private static final String[][] PAGES = {
        {"Willkommen", "§7Hier gibt es drei Bereiche: §dDuelle§7, §aSMP §7und §bBau-SMP§7.",
            "§7Wechseln: §d/lobby§7, §a/smp§7, §b/bausmp§7. Mit dem §fKompass §7in der Lobby geht es über das Menü."},
        {"Duelle", "§7Fordere jemanden mit §d/duel <Spieler> §7heraus. Du kannst Kit und Best-of (1/3/5/7) wählen.",
            "§7Mit §d/party §7spielst du zusammen. §d/spectate §7zeigt laufende Duelle, §d/ranked §7schaltet Elo an/aus."},
        {"SMP & Geld", "§7Im SMP verdienst du Geld mit §d/sell§7, §d/ah §7(Auktionshaus) und §d/order§7.",
            "§7Dein Geld gilt nur im jeweiligen Modus (SMP / Bau-SMP). Gems sind überall gleich."},
        {"Gems", "§7Alle 10 Minuten im SMP gibt es 1 Gem, dazu §d/daily§7.",
            "§7Im §d/gems§7-Shop: Gem-Spitzhacke, Gem-Axt, Kosmetik (Armor-Trims mit Mustern und Materialien, Kill-Effekte, Projektil-Spuren)."},
        {"Freunde & Handel", "§d/friend §7öffnet deine Freundesliste: Teleport-Anfrage, Duell und Party per Klick.",
            "§d/trade <Spieler> §7ist der sichere Handel (SMP). §d/tpmenu §7hilft beim Teleportieren."},
        {"Parkour & Seasons", "§d/parkour §7startet eine Strecke. Dein Bestzeit-Geist und der Rekord-Geist laufen mit.",
            "§d/season §7zeigt die laufende Duell-Season, die Top 10 und die Belohnungen."},
        {"FPS-Tipp", "§7Für flüssiges Spielen empfehlen wir die Mods §fSodium§7, §fLithium §7und §fIris §7(Fabric).",
            "§7Im Menü §fVideo-Einstellungen §7helfen auch Renderdistanz 8–10 und Partikel auf »Minimal«."},
    };

    private final DuelNetworkPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    Guide(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "guide.yml");
        this.data = YamlConfiguration.loadConfiguration(this.file);
    }

    private void save() {
        try {
            Store.save(this.data, this.file);
        }
        catch (IOException ex) {
            this.plugin.getLogger().warning("guide.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        if (!this.plugin.getConfig().getBoolean("welcome.tutorial-hint", true) || this.data.getBoolean("seen." + id, false)) {
            return;
        }
        this.data.set("seen." + id, true);
        this.save();
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.sendMessage("§d✦ §fWillkommen, " + player.getName() + "! §7Eine kurze Einführung gibt es mit §d/tutorial§7.");
            if (this.plugin.getConfig().getBoolean("welcome.client-tip", true)) {
                player.sendMessage("§7FPS-Tipp: Mit §fSodium§7, §fLithium §7und §fIris §7läuft der Server noch flüssiger.");
            }
        }, 100L);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "tutorial" -> this.tutorial(player, args.length == 0 ? 1 : this.parse(args[0]));
            case "ranked" -> {
                boolean casual = DuelStats.toggleCasual(player.getUniqueId());
                this.plugin.leaveDuelQueueQuiet(player);
                player.sendMessage(casual ? "§7Modus: §eUnranked §8– §7zufälliger Gegner, deine Elo ändert sich nicht."
                        : "§7Modus: §aRanked §8– §7eigene Elo je Kit, Gegner mit ähnlicher Elo.");
            }
            default -> {
                Player target = args.length == 0 ? player : this.plugin.getServer().getPlayerExact(args[0]);
                if (target == null) {
                    player.sendMessage("§cSpieler nicht online.");
                } else {
                    this.openProfile(player, target);
                }
            }
        }
        return true;
    }

    private int parse(String text) {
        try {
            return Integer.parseInt(text);
        }
        catch (NumberFormatException ex) {
            return 1;
        }
    }

    private void tutorial(Player player, int page) {
        int index = Math.max(1, Math.min(PAGES.length, page));
        String[] content = PAGES[index - 1];
        player.sendMessage("§8§m                    §r §d✦ §fTutorial §7" + index + "/" + PAGES.length + " §8– §d" + content[0] + " §8§m                    ");
        for (int i = 1; i < content.length; i++) {
            player.sendMessage(content[i]);
        }
        List<String[]> buttons = new ArrayList<String[]>();
        if (index > 1) {
            buttons.add(new String[]{"§e§l[« Zurück]", "/tutorial " + (index - 1), "§7Vorherige Seite"});
        }
        if (index < PAGES.length) {
            buttons.add(new String[]{"§a§l[Weiter »]", "/tutorial " + (index + 1), "§7Nächste Seite"});
        }
        if (!buttons.isEmpty()) {
            Chat.buttons(player, " ", buttons.toArray(new String[0][]));
        }
    }

    private void openProfile(Player viewer, Player target) {
        UUID id = target.getUniqueId();
        int[] stats = DuelStats.snapshot(id);
        int games = stats[1] + stats[2];
        String rate = games == 0 ? "–" : String.format(Locale.ROOT, "%.0f%%", 100.0 * stats[1] / games);
        Inventory inv = Gui.frame(this.plugin, TAG, target.getName(), 27);
        ItemStack head = Gui.item(Material.PLAYER_HEAD, "§d§l" + target.getName(), DuelStats.casual(id) ? "§eUnranked-Modus" : "§aRanked-Modus");
        if (head.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(target);
            head.setItemMeta(meta);
        }
        inv.setItem(4, head);
        inv.setItem(10, Gui.item(Material.NETHERITE_SWORD, "§d§lDuell-Elo §f" + stats[0],
                stats[5] > 0 ? "§7Platz §f#" + stats[5] : "§7Noch keine Wertung", "§7Season §f" + (Seasons.instance == null ? 1 : Seasons.instance.number())));
        inv.setItem(12, Gui.item(Material.IRON_SWORD, "§a§lSiege §f" + stats[1], "§7Niederlagen: §f" + stats[2], "§7Siegquote: §f" + rate));
        inv.setItem(14, Gui.item(Material.BLAZE_POWDER, "§6§lSerie §f" + stats[3], "§7Beste Serie: §f" + stats[4]));
        inv.setItem(16, Gui.item(Material.AMETHYST_SHARD, "§b§lGems §f" + Gems.balance(id), "§7Geld (aktueller Modus): §f" + Gui.money(Economy.balance(id))));
        List<String> kits = DuelStats.kitLines(id, 6);
        inv.setItem(22, Gui.item(Material.BOOK, "§d§lKit-Statistik", kits.isEmpty() ? new String[]{"§7Noch keine Kit-Duelle."} : kits.toArray(new String[0])));
        viewer.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (Gui.isTitle(event.getView().getTitle(), TAG)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (Gui.isTitle(event.getView().getTitle(), TAG)) {
            event.setCancelled(true);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();
        if (args.length == 1 && command.getName().equalsIgnoreCase("stats")) {
            for (Player online : this.plugin.getServer().getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(online.getName());
                }
            }
        }
        return out;
    }
}
