package de.server.network;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/** Klickbare Chat-Nachrichten (Annehmen/Ablehnen-Buttons) und kleine Sound-Helfer. */
final class Chat {
    private Chat() { }

    /** Anfrage-Nachricht mit Annehmen- und Ablehnen-Button; die Befehle laufen ueber das Plugin-Praefix. */
    static void request(Player target, String text, String acceptCommand, String acceptHover, String denyCommand, String denyHover) {
        buttons(target, text,
                new String[]{"§a§l[✔ Annehmen]", "/duelnetwork:" + acceptCommand, acceptHover},
                new String[]{"§c§l[✖ Ablehnen]", "/duelnetwork:" + denyCommand, denyHover});
    }

    /** Text gefolgt von klickbaren Buttons; jeder Button = {Beschriftung, Befehl mit /, Hover-Text}. */
    static void buttons(Player player, String text, String[]... buttons) {
        try {
            LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
            Component line = serializer.deserialize(text);
            for (String[] b : buttons) {
                Component button = serializer.deserialize(b[0]).clickEvent(ClickEvent.runCommand(b[1])).hoverEvent((net.kyori.adventure.text.event.HoverEventSource<?>)(Object)HoverEvent.showText(serializer.deserialize(b[2])));
                line = line.append(Component.text(" ")).append(button);
            }
            ((net.kyori.adventure.audience.Audience)(Object)player).sendMessage(line);
        }
        catch (Throwable t) {
            StringBuilder sb = new StringBuilder(text);
            for (String[] b : buttons) {
                sb.append(" §8").append(b[1].replace("/duelnetwork:", "/"));
            }
            player.sendMessage(sb.toString());
        }
    }

    static void sound(Player player, Sound sound, float volume, float pitch) {
        try {
            if (player != null && player.isOnline()) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }
        catch (Throwable ignored) {
            // nur Ton
            Store.problem("Chat:47", ignored);
        }
    }

    static void ping(Player player) {
        sound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
    }

    static void click(Player player) {
        sound(player, Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    static void yes(Player player) {
        sound(player, Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.6f);
    }

    static void no(Player player) {
        sound(player, Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
    }

    static void warp(Player player) {
        sound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.0f);
    }

    static void coins(Player player) {
        sound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
        sound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.8f);
    }
}
