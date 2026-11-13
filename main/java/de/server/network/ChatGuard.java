package de.server.network;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

/**
 * Leichter Chat-Schutz (Spam und Wiederholungen macht bereits der Anti-Cheat):
 * Links blockieren (chat.block-links), GROSSBUCHSTABEN abschwaechen (chat.caps-limit),
 * Wortliste (chat.filter-words) und @Name-Erwaehnung mit Ping. Alles in config.yml unter chat.*.
 * Umgehen: duelnetwork.chat.bypass (oder duelnetwork.admin); Links: duelnetwork.chat.links.
 */
final class ChatGuard implements Listener {
    private static final Pattern LINK = Pattern.compile("(?i)(https?://|www\\.|\\b[a-z0-9-]{2,}\\.(com|net|org|de|gg|io|tv|xyz|ru|me|to|co)\\b|\\b\\d{1,3}(\\.\\d{1,3}){3}\\b)");

    private final DuelNetworkPlugin plugin;

    ChatGuard(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean bypass(Player player) {
        return player.hasPermission("duelnetwork.chat.bypass") || player.hasPermission("duelnetwork.admin");
    }

    private void tell(Player player, String text) {
        this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
            if (player.isOnline()) {
                player.sendMessage(text);
                Chat.no(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (this.bypass(player) || !this.plugin.getConfig().getBoolean("chat.guard-enabled", true)) {
            return;
        }
        String message = event.getMessage();
        if (this.plugin.getConfig().getBoolean("chat.block-links", true) && !player.hasPermission("duelnetwork.chat.links") && LINK.matcher(message).find()) {
            event.setCancelled(true);
            this.tell(player, "§cLinks und IP-Adressen sind im Chat nicht erlaubt.");
            return;
        }
        List<String> words = this.plugin.getConfig().getStringList("chat.filter-words");
        if (!words.isEmpty()) {
            String lower = message.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9äöüß]", "");
            for (String word : words) {
                String w = word.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9äöüß]", "");
                if (!w.isEmpty() && lower.contains(w)) {
                    event.setCancelled(true);
                    this.tell(player, "§cBitte achte auf deine Wortwahl.");
                    return;
                }
            }
        }
        int capsLimit = this.plugin.getConfig().getInt("chat.caps-limit-percent", 70);
        if (capsLimit > 0 && message.length() >= 10) {
            int letters = 0;
            int upper = 0;
            for (int i = 0; i < message.length(); i++) {
                char c = message.charAt(i);
                if (Character.isLetter(c)) {
                    letters++;
                    if (Character.isUpperCase(c)) {
                        upper++;
                    }
                }
            }
            if (letters >= 8 && upper * 100 / letters > capsLimit) {
                event.setMessage(message.toLowerCase(Locale.ROOT));
            }
        }
    }

    /** Erwaehnung: "@Name" pingt den Spieler (Actionbar + Ton), sofern er die Nachricht wirklich empfaengt. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMention(AsyncPlayerChatEvent event) {
        if (!this.plugin.getConfig().getBoolean("chat.mentions", true) || event.getMessage().indexOf('@') < 0) {
            return;
        }
        String lower = event.getMessage().toLowerCase(Locale.ROOT);
        String from = event.getPlayer().getName();
        for (Player recipient : event.getRecipients()) {
            if (recipient.equals(event.getPlayer()) || !Prefs.on(recipient.getUniqueId(), "mention") || !lower.contains("@" + recipient.getName().toLowerCase(Locale.ROOT))) {
                continue;
            }
            this.plugin.getServer().getScheduler().runTask((Plugin)this.plugin, () -> {
                if (recipient.isOnline()) {
                    recipient.sendActionBar(net.kyori.adventure.text.Component.text("§d✦ §f" + from + " §7hat dich erwähnt"));
                    Chat.ping(recipient);
                }
            });
        }
    }
}
