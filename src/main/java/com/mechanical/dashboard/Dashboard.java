package com.mechanical.dashboard;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * Serves the page from assets/mechanical/dashboard and the JSON {@link Snapshot} rebuilt once a second on the server thread.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Dashboard {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    private static HttpServer http;
    private static volatile Snapshot.Json latest = new Snapshot.Json("{}", "[]", "[]", "{}");

    private Dashboard() {}

    @SubscribeEvent
    static void onStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        if (!server.isDedicatedServer() || server instanceof net.minecraft.gametest.framework.GameTestServer || !ServerConfig.DASHBOARD.get()) return;
        bind(ServerConfig.DASHBOARD_PORT.get());
    }

    @SubscribeEvent
    static void onStopping(ServerStoppingEvent event) {
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
            HttpServer s = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
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

    public static synchronized void stop() {
        if (http == null) return;
        http.stop(0);
        ((ExecutorService) http.getExecutor()).shutdownNow();
        http = null;
    }

    private static void handle(HttpExchange ex) throws IOException {
        try (ex) {
            String path = ex.getRequestURI().getPath();
            if (!ex.getRequestMethod().equals("GET")) {
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
            } else {
                String file = path.equals("/") ? "index.html" : path.substring(1);
                if (file.contains("..") || file.contains("\\")) {
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
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(code, body.length);
        ex.getResponseBody().write(body);
    }
}
