package de.server.network;

import com.sun.net.httpserver.HttpServer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.MessageDigest;

/**
 * Liefert das mitgelieferte LowBlock-Texture-Pack: eingebauter kleiner Webserver (oder eigene URL) und schickt es
 * beim Beitritt an jeden Spieler. Der SHA1-Hash wird automatisch berechnet.
 */
final class PackHost implements Listener, org.bukkit.command.CommandExecutor {
    /** Muss zur Version in pack/gen.py (pack.mcmeta-Beschreibung) passen. */
    static final int PACK_VERSION = 96;
    static final String PLUGIN_PACK_TARGET = "DuelNetwork 1.3.1";
    private boolean manual = true;
    /** true = Spieler koennen sich beim Beitritt nicht bewegen/interagieren, bis das Pack geladen ist (resourcepack.freeze-on-join, Standard aus). */
    private boolean freeze;
    private final java.util.Set<java.util.UUID> optIn = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private java.io.File optFile;
    private final DuelNetworkPlugin plugin;
    private byte[] data;
    private byte[] sha1;
    private String url;
    private boolean force;
    private HttpServer server;
    private java.util.concurrent.ExecutorService httpExecutor;
    private String host = "";
    private int port;
    private volatile String publicIp = "";
    private final java.util.Set<java.util.UUID> scheduledJoins = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // Download-Begrenzung: hoechstens 12 Anfragen pro Minute und Adresse (Schutz vor Missbrauch des offenen Ports)
    private final java.util.Map<String, long[]> rate = new java.util.concurrent.ConcurrentHashMap<String, long[]>();

    private boolean allowRequest(java.net.InetAddress address) {
        long now = System.currentTimeMillis();
        if (this.rate.size() > 5000) {
            this.rate.clear();
        }
        String key = address == null ? "?" : address.getHostAddress();
        long[] slot = this.rate.computeIfAbsent(key, k -> new long[]{now, 0L});
        synchronized (slot) {
            if (now - slot[0] > 60_000L) {
                slot[0] = now;
                slot[1] = 0L;
            }
            return ++slot[1] <= 12L;
        }
    }

    private void lookupPublicIp() {
        try {
            java.net.HttpURLConnection c = (java.net.HttpURLConnection)new java.net.URL("https://api.ipify.org").openConnection();
            c.setConnectTimeout(4000);
            c.setReadTimeout(4000);
            InputStream in = c.getInputStream();
            this.publicIp = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
            in.close();
        }
        catch (Throwable t) {
            // dann gilt die Adresse, mit der sich der Spieler verbunden hat
            this.plugin.getLogger().info("Oeffentliche IP konnte nicht ermittelt werden (" + t.getClass().getSimpleName() + "). Setze resourcepack.host, falls Spieler das Pack nicht laden.");
        }
    }

    PackHost(DuelNetworkPlugin plugin) {
        this.plugin = plugin;
        try {
            if (!plugin.getConfig().getBoolean("resourcepack.enabled", true)) {
                return;
            }
            InputStream in = plugin.getResource("LowBlock-Pack.zip");
            if (in == null) {
                plugin.getLogger().warning("Texture-Pack nicht in der Jar gefunden.");
                return;
            }
            this.data = in.readAllBytes();
            in.close();
            this.sha1 = MessageDigest.getInstance("SHA-1").digest(this.data);
            try {
                StringBuilder hex = new StringBuilder();
                for (byte b : this.sha1) {
                    hex.append(String.format("%02x", b));
                }
                plugin.getDataFolder().mkdirs();
                java.nio.file.Files.write(new java.io.File(plugin.getDataFolder(), "LowBlock-Pack.zip").toPath(), this.data);
                plugin.getLogger().info("Texture-Pack fuer " + PLUGIN_PACK_TARGET + " (SHA-1 " + hex + ") liegt in plugins/DuelNetwork/LowBlock-Pack.zip - fuer eigenes Hosting dort hochladen und die URL bei resourcepack.url eintragen.");
            }
            catch (Throwable ignored) {
                // nur Komfort
                Store.problem("PackHost:95", ignored);
            }
            this.optFile = new java.io.File(plugin.getDataFolder(), "textures.yml");
            org.bukkit.configuration.file.YamlConfiguration saved = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(this.optFile);
            for (String s : saved.getStringList("on")) {
                try {
                    this.optIn.add(java.util.UUID.fromString(s));
                }
                catch (Exception ignored) {
                    // kaputter Eintrag
                    Store.problem("PackHost:104", ignored);
                }
            }
            // neue Pack-Version: alte /texturen-Zusagen verfallen (Spieler muessen das neue Pack laden und neu bestaetigen)
            if (saved.getInt("version", 0) != PACK_VERSION) {
                this.optIn.clear();
                try {
                    saved.set("on", new java.util.ArrayList<String>());
                    saved.set("version", PACK_VERSION);
                    Store.save(saved, this.optFile);
                }
                catch (Exception ignored) {
                    // egal
                    Store.problem("PackHost:116", ignored);
                }
            }
            this.manual = !"server".equalsIgnoreCase(plugin.getConfig().getString("resourcepack.mode", "server"));
            if (this.manual) {
                // Manuell-Modus (z.B. Minekeep verbietet Server-Packs): Pack zum Verteilen in den Plugin-Ordner legen
                try {
                    plugin.getDataFolder().mkdirs();
                    java.nio.file.Files.write(new java.io.File(plugin.getDataFolder(), "LowBlock-Pack.zip").toPath(), this.data);
                }
                catch (Throwable ignored) {
                    // nur Komfort
                    Store.problem("PackHost:127", ignored);
                }
                plugin.getLogger().info("Texture-Pack im Manuell-Modus: Datei liegt in plugins/DuelNetwork/LowBlock-Pack.zip, Spieler aktivieren mit /texturen an.");
                return;
            }
            this.force = plugin.getConfig().getBoolean("resourcepack.force", true);
            String custom = plugin.getConfig().getString("resourcepack.url", "");
            String host = plugin.getConfig().getString("resourcepack.host", "");
            int port = plugin.getConfig().getInt("resourcepack.port", 8123);
            if (custom != null && !custom.trim().isEmpty()) {
                this.url = custom.trim();
            } else {
                this.host = host == null ? "" : host.trim();
                final byte[] bytes = this.data;
                HttpServer created = null;
                for (int p = port; p < port + 10 && created == null; p++) {
                    try {
                        created = HttpServer.create(new InetSocketAddress(p), 0);
                        this.port = p;
                    }
                    catch (Exception busy) {
                        created = null;
                    }
                }
                if (created == null) {
                    throw new IllegalStateException("kein freier Port ab " + port);
                }
                this.server = created;
                // eigener kleiner Thread-Pool: ein langsamer Download blockiert so keine anderen Spieler
            this.httpExecutor = java.util.concurrent.Executors.newFixedThreadPool(3, r -> {
                Thread t = new Thread(r, "LowBlock-Pack-HTTP");
                t.setDaemon(true);
                return t;
            });
            this.server.setExecutor(this.httpExecutor);
            this.server.createContext("/LowBlock-Pack.zip", exchange -> {
                    try {
                        String method = exchange.getRequestMethod();
                        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                            exchange.sendResponseHeaders(405, -1L);
                            return;
                        }
                        if (!this.allowRequest(exchange.getRemoteAddress().getAddress())) {
                            exchange.sendResponseHeaders(429, -1L);
                            return;
                        }
                        exchange.getResponseHeaders().add("Content-Type", "application/zip");
                        if ("HEAD".equalsIgnoreCase(method)) {
                            exchange.getResponseHeaders().add("Content-Length", String.valueOf(bytes.length));
                            exchange.sendResponseHeaders(200, -1L);
                            return;
                        }
                        exchange.sendResponseHeaders(200, bytes.length);
                        OutputStream out = exchange.getResponseBody();
                        out.write(bytes);
                        out.close();
                    }
                    catch (Throwable t) {
                        // meist abgebrochene Verbindung - nur auf FINE, damit die Konsole sauber bleibt
                        Store.debug(plugin.getLogger(), "Pack-Download", t);
                    }
                    finally {
                        exchange.close();
                    }
                });
            this.server.start();
                plugin.getLogger().info("Texture-Pack-Server laeuft auf Port " + this.port + " (muss von aussen erreichbar sein).");
                if (this.host.isEmpty() && plugin.getConfig().getBoolean("resourcepack.detect-ip", true)) {
                    Thread t = new Thread(() -> this.lookupPublicIp(), "lowblock-ip");
                    t.setDaemon(true);
                    t.start();
                }
            }
        }
        catch (Throwable t) {
            this.url = null;
            plugin.getLogger().warning("Texture-Pack konnte nicht gestartet werden: " + t);
        }
    }

    void stop() {
        try {
            if (this.server != null) {
                this.server.stop(0);
            }
            if (this.httpExecutor != null) {
                this.httpExecutor.shutdownNow();
                this.httpExecutor = null;
            }
        }
        catch (Throwable ignored) {
            // egal
            Store.problem("PackHost:213", ignored);
        }
    }

    private String linkFor(Player player) {
        if (this.url != null) {
            return this.url;
        }
        String h = this.host;
        if (h.isEmpty()) {
            try {
                java.net.InetSocketAddress virtual = player.getVirtualHost();
                if (virtual != null && virtual.getHostString() != null && !virtual.getHostString().isEmpty()) {
                    h = virtual.getHostString();
                }
            }
            catch (Throwable ignored) {
                // aeltere API
                Store.problem("PackHost:230", ignored);
            }
        }
        if (h.isEmpty()) {
            h = this.publicIp;
        }
        return h.isEmpty() ? null : "http://" + h + ":" + this.port + "/LowBlock-Pack.zip";
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (this.manual) {
            final Player joined = event.getPlayer();
            this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                try {
                    if (!joined.isOnline()) {
                        return;
                    }
                    if (this.optIn.contains(joined.getUniqueId())) {
                        ItemSkins.LOADED.add(joined.getUniqueId());
                        MenuItems.refreshNow(joined);
                    } else {
                        joined.sendMessage("§7Du hast das aktuelle LowBlock-Pack für " + PLUGIN_PACK_TARGET + " installiert? Mit §d/texturen an §7bekommst du die Spezial-Texturen.");
                    }
                }
                catch (Throwable ignored) {
                    // nur Optik
                    Store.problem("PackHost:256", ignored);
                }
            }, 40L);
            return;
        }
        if (this.sha1 == null || (this.url == null && this.server == null)) {
            this.plugin.getLogger().warning("Texture-Pack kann beim Join nicht gesendet werden: kein aktiver Host bzw. keine resourcepack.url.");
            return;
        }
        final Player player = event.getPlayer();
        this.scheduledJoins.add(player.getUniqueId());
        // Die öffentliche IP kann erst nach dem Join eintreffen. Mehrere Versuche
        // verhindern, dass der einmalige frühe Join-Versand ohne URL ins Leere läuft.
        this.sendOnJoin(player, 0);
    }
    private void sendOnJoin(Player player, int attempt) {
        long delay = attempt == 0 ? 5L : 20L;
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            try {
                if (!player.isOnline() || !this.scheduledJoins.contains(player.getUniqueId())) return;
                String link = this.linkFor(player);
                if (link != null) {
                    player.setResourcePack(link, this.sha1, this.force);
                    this.scheduledJoins.remove(player.getUniqueId());
                    this.plugin.getLogger().info("Texture-Pack für " + PLUGIN_PACK_TARGET + " an " + player.getName() + " gesendet: " + link);
                } else if (attempt < 20) {
                    this.sendOnJoin(player, attempt + 1);
                } else {
                    this.scheduledJoins.remove(player.getUniqueId());
                    player.sendMessage("§eDas Texture-Pack konnte nicht automatisch gesendet werden. §7Bitte resourcepack.host oder resourcepack.url konfigurieren.");
                    this.plugin.getLogger().warning("Keine erreichbare Texture-Pack-URL fuer " + player.getName() + ". Setze resourcepack.host oder resourcepack.url.");
                }
            }
            catch (Throwable t) {
                Store.problem("PackHost:296", t);
                if (attempt < 20 && player.isOnline()) this.sendOnJoin(player, attempt + 1);
            }
        }, delay);
    }

    /** Bis das Pflicht-Pack geladen ist: nicht bewegen, nichts anfassen, nicht schreiben. */
    private final java.util.Set<java.util.UUID> pending = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private boolean freezing(java.util.UUID id) {
        return this.freeze && !this.pending.isEmpty() && this.pending.contains(id);
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void holdMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (this.freezing(event.getPlayer().getUniqueId()) && event.getTo() != null
                && (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ() || event.getFrom().getY() != event.getTo().getY())) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void holdInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (this.freezing(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void holdCommand(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        if (this.freezing(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void holdDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!this.pending.isEmpty() && event.getEntity() instanceof Player p && this.pending.contains(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private final java.util.Map<java.util.UUID, String> lastStatus = new java.util.concurrent.ConcurrentHashMap<java.util.UUID, String>();

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        java.util.UUID id = player.getUniqueId();
        String arg = args.length >= 1 ? args[0].toLowerCase(java.util.Locale.ROOT) : "";
        if (arg.equals("status")) {
            player.sendMessage("§dPack-Status §8» §7Modus: §f" + (this.manual ? "manuell" : "Server") + " §8• §7Ziel: §f" + PLUGIN_PACK_TARGET);
            player.sendMessage("§7Geladen (Plugin-Sicht): " + (ItemSkins.LOADED.contains(id) ? "§aja" : "§cnein") + " §8• §7letzter Status: §f" + this.lastStatus.getOrDefault(id, "keiner"));
            if (!this.manual) {
                String link = this.linkFor(player);
                player.sendMessage("§7Download-Link: §f" + (link == null ? "kein Link (resourcepack.url oder -host setzen)" : link));
                if (!ItemSkins.LOADED.contains(id)) {
                    player.sendMessage("§eKein Pack geladen: Ist der Port §f" + this.port + "§e von aussen offen? Sonst Pack extern hosten und resourcepack.url eintragen.");
                }
            }
            return true;
        }
        boolean on = arg.equals("an") || arg.equals("on") ? true : arg.equals("aus") || arg.equals("off") ? false : !this.optIn.contains(id);
        if (!this.manual && arg.isEmpty()) {
            player.sendMessage("§7Das Texture-Pack wird automatisch geladen.");
            return true;
        }
        if (on) {
            this.optIn.add(id);
            ItemSkins.LOADED.add(id);
            player.sendMessage("§aSpezial-Texturen sind an. §7Du brauchst dafür das aktuelle LowBlock-Pack für " + PLUGIN_PACK_TARGET + ", sonst siehst du Fehler-Texturen (dann §d/texturen aus§7).");
            MenuItems.refreshNow(player);
        } else {
            this.optIn.remove(id);
            ItemSkins.LOADED.remove(id);
            player.sendMessage("§7Spezial-Texturen sind aus. §8(Items sind ab jetzt normal; vorhandene Hotbar-Items aktualisieren sich beim Neuladen.)");
        }
        try {
            org.bukkit.configuration.file.YamlConfiguration out = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(this.optFile);
            java.util.List<String> ids = new java.util.ArrayList<String>();
            for (java.util.UUID u : this.optIn) {
                ids.add(u.toString());
            }
            out.set("on", ids);
            out.set("version", PACK_VERSION);
            Store.save(out, this.optFile);
        }
        catch (Throwable ignored) {
            // naechstes Mal
            Store.problem("PackHost:381", ignored);
        }
        return true;
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {
        try {
            Player player = event.getPlayer();
            String status = String.valueOf(event.getStatus());
            this.plugin.getLogger().info("Texture-Pack " + player.getName() + ": " + status);
            this.lastStatus.put(player.getUniqueId(), status);
            if (status.equals("SUCCESSFULLY_LOADED")) {
                ItemSkins.LOADED.add(player.getUniqueId());
                this.pending.remove(player.getUniqueId());
                MenuItems.refreshNow(player);
            } else if (status.equals("DECLINED") || status.equals("FAILED_DOWNLOAD") || status.equals("FAILED_RELOAD") || status.equals("INVALID_URL")) {
                ItemSkins.LOADED.remove(player.getUniqueId());
                if (!status.equals("DECLINED") || !this.force) {
                    // Download fehlgeschlagen: sofort freigeben statt festhalten
                    if (this.pending.remove(player.getUniqueId())) {
                        player.sendMessage("§eDas Texture-Pack konnte nicht geladen werden (" + status + ") - du kannst trotzdem spielen.");
                        this.plugin.getLogger().warning("Texture-Pack fuer " + player.getName() + ": " + status + " - Link: " + this.linkFor(player));
                    }
                }
            }
        }
        catch (Throwable ignored) {
            // nur Optik
            Store.problem("PackHost:409", ignored);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.lastStatus.remove(event.getPlayer().getUniqueId());
        this.pending.remove(event.getPlayer().getUniqueId());
        this.scheduledJoins.remove(event.getPlayer().getUniqueId());
        ItemSkins.LOADED.remove(event.getPlayer().getUniqueId());
    }

    private static boolean isPluginMenu(String title) {
        return title != null && title.indexOf('\u00a7') >= 0;
    }

    /** Leises Klick-Geraeusch beim Klicken in Plugin-Menues (Vanilla-Kisten bleiben still). */
    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onMenuClickSound(org.bukkit.event.inventory.InventoryClickEvent event) {
        try {
            if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() == null
                    || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
                return;
            }
            org.bukkit.inventory.ItemStack stack = event.getCurrentItem();
            if (stack == null || stack.getType().isAir() || !stack.hasItemMeta() || !stack.getItemMeta().hasDisplayName()
                    || !isPluginMenu(event.getView().getTitle())) {
                return;
            }
            if (stack.getItemMeta().getDisplayName().replaceAll("\u00a7.", "").trim().isEmpty()) {
                return;
            }
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.3f);
        }
        catch (Throwable ignored) {
            // nur Ton
            Store.problem("PackHost:443", ignored);
        }
    }

    /** Leises Blaettern beim Oeffnen eines Plugin-Menues; namenlose Fuell-Items ohne Tooltip. */
    @EventHandler
    public void onMenuOpen(InventoryOpenEvent event) {
        try {
            if (!(event.getPlayer() instanceof Player player)) {
                return;
            }
            ItemSkins.hideBlanks(event.getView().getTopInventory());
            if (isPluginMenu(event.getView().getTitle())) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1.1f);
                this.applyMenuBackground(player, event.getView());
            }
        }
        catch (Throwable ignored) {
            // nur Optik
            Store.problem("PackHost:menuOpen", ignored);
        }
    }

    /**
     * Lila Hintergrund nur fuer Plugin-Menues (Inventar ohne Besitzer, 3-6 Reihen) und nur fuer Spieler mit geladenem Pack.
     * Der Hintergrund ist ein Titel-Zeichen aus dem Pack (Schrift minecraft:default, Zeichen U+F8E0-F8E6). Vanilla-Kisten bleiben unberuehrt.
     * Wird einen Tick nach dem Oeffnen gesetzt; der Titel behaelt seinen Text, die Menue-Erkennung per contains() funktioniert weiter.
     */
    private void applyMenuBackground(Player player, org.bukkit.inventory.InventoryView view) {
        try {
            if (!ItemSkins.LOADED.contains(player.getUniqueId())) {
                return;
            }
            org.bukkit.inventory.Inventory top = view.getTopInventory();
            int size = top.getSize();
            if (top.getHolder() != null || size % 9 != 0 || size < 27 || size > 54) {
                return;
            }
            String title = view.getTitle();
            if (title == null || title.indexOf('\uf8e0') >= 0) {
                return;
            }
            String prefix = "\u00a7f\uf8e0" + (char)(0xf8e0 + size / 9) + "\uf8e1\u00a7r";
            this.plugin.getServer().getScheduler().runTask((org.bukkit.plugin.Plugin)this.plugin, () -> {
                try {
                    if (player.isOnline() && player.getOpenInventory() == view && view.getTitle().indexOf('\uf8e0') < 0) {
                        view.setTitle(prefix + view.getTitle());
                    }
                }
                catch (Throwable ignored) {
                    Store.problem("PackHost:menuBg", ignored);
                }
            });
        }
        catch (Throwable ignored) {
            Store.problem("PackHost:menuBg0", ignored);
        }
    }
}
