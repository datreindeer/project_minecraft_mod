package com.mechanical.dashboard;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mechanical.Mechanical;
import com.mechanical.ServerConfig;
import com.mojang.logging.LogUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The admin web dashboard: a small HTTP server inside the dedicated server, on 127.0.0.1 only (a login proxy sits in front, never the mod itself).
 * Serves the page from assets/mechanical/dashboard, the JSON {@link Snapshot} rebuilt once a second on the server thread, and the {@link MapTiles} PNGs.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Dashboard {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static volatile HttpServer http;
    /** Signed integers only, so a tile name can never become a path. */
    static final Pattern TILE = Pattern.compile("/tiles/(-?\\d{1,6})\\.(-?\\d{1,6})\\.png");
    private static volatile Snapshot.Json latest = new Snapshot.Json("{}", "[]", "[]", "{}");

    private Dashboard() {}

    @SubscribeEvent
    static void onStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer() || server instanceof net.minecraft.gametest.framework.GameTestServer || !ServerConfig.DASHBOARD.get()) return;
        Snapshot.started();
        bind(ServerConfig.DASHBOARD_PORT.get());
        if (isRunning()) MapTiles.start(server);
    }

    @SubscribeEvent
    static void onStopping(ServerStoppingEvent event) {
        MapTiles.stop();
        stop();
    }

    @SubscribeEvent
    static void onTick(ServerTickEvent.Post event) {
        if (http != null && event.getServer().getTickCount() % 20 == 0) latest = Snapshot.build(event.getServer());
    }

    /** Starts listening; a port in use is one warning, never a crash. Public for the game test (the test server isn't dedicated). */
    public static synchronized void bind(int port) {
        stop();
        try {
            // 127.0.0.1 itself: getLoopbackAddress() is ::1 when the JVM prefers IPv6, and the proxy in front dials 127.0.0.1
            HttpServer s = HttpServer.create(new InetSocketAddress(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), port), 0);
            s.createContext("/", Dashboard::handle);
            s.setExecutor(Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "mechanical-dashboard");
                t.setDaemon(true);
                return t;
            }));
            s.start();
            http = s;
            LOGGER.info("Mechanical dashboard on http://127.0.0.1:{}", port);
        } catch (IOException e) {
            LOGGER.warn("Mechanical dashboard could not listen on 127.0.0.1:{} ({}), it stays off", port, e.getMessage());
        }
    }

    static boolean isRunning() {
        return http != null;
    }

    /** Only our own loopback names may talk to us: a web page that rebinds its domain to 127.0.0.1 sends its own Host and gets 403 (DNS rebinding). */
    static boolean allowedHost(String host, int port) {
        return host != null && (host.equals("127.0.0.1:" + port) || host.equals("localhost:" + port) || host.equals("[::1]:" + port));
    }

    public static synchronized void stop() {
        if (http == null) return;
        http.stop(0);
        ((ExecutorService) http.getExecutor()).shutdownNow();
        http = null;
    }

    private static void handle(HttpExchange ex) throws IOException {
        try (ex) {
            String path = ex.getRequestURI().getPath();
            if (!allowedHost(ex.getRequestHeaders().getFirst("Host"), ex.getLocalAddress().getPort())) {
                send(ex, 403, "text/plain", "forbidden");
            } else if (!ex.getRequestMethod().equals("GET")) {
                send(ex, 405, "text/plain", "method not allowed");
            } else if (path.startsWith("/api/")) {
                Snapshot.Json j = latest;
                String body = switch (path) {
                    case "/api/status" -> j.status();
                    case "/api/players" -> j.players();
                    case "/api/tribes" -> j.tribes();
                    case "/api/map" -> j.map();
                    default -> null;
                };
                if (body == null) send(ex, 404, "text/plain", "not found");
                else send(ex, 200, "application/json", body);
            } else if (path.startsWith("/tiles/")) {
                Matcher m = TILE.matcher(path);
                byte[] png = m.matches() ? MapTiles.tile(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))) : null;
                if (png == null) send(ex, 404, "text/plain", "not found");
                else send(ex, 200, "image/png", png, "no-cache");
            } else {
                String file = path.equals("/") ? "index.html" : path.substring(1);
                if (file.contains("..") || file.contains("\\") || file.endsWith("/")) {
                    send(ex, 404, "text/plain", "not found");
                    return;
                }
                try (InputStream in = Dashboard.class.getResourceAsStream("/assets/mechanical/dashboard/" + file)) {
                    if (in == null) send(ex, 404, "text/plain", "not found");
                    else send(ex, 200, type(file), in.readAllBytes());
                }
            }
        }
    }

    private static String type(String file) {
        return switch (file.substring(file.lastIndexOf('.') + 1)) {
            case "html" -> "text/html; charset=utf-8";
            case "css" -> "text/css; charset=utf-8";
            case "js" -> "text/javascript; charset=utf-8";
            case "png" -> "image/png";
            case "svg" -> "image/svg+xml";
            case "json" -> "application/json";
            default -> "application/octet-stream";
        };
    }

    private static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        send(ex, code, type, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        send(ex, code, type, body, "no-store");
    }

    private static void send(HttpExchange ex, int code, String type, byte[] body, String cache) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", cache);
        ex.sendResponseHeaders(code, body.length);
        ex.getResponseBody().write(body);
    }
}
