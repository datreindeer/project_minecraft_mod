package com.mechanical.dashboard;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;

import net.minecraft.server.MinecraftServer;

/**
 * Players' faces for the web map (/faces/&lt;uuid&gt;.png): cut here from the skin Mojang hosts, so the page never has to
 * ask another site and no player id leaves for one. No skin (offline mode, download failed): no face, the page keeps its dot.
 */
final class Faces {
    /** Where Mojang keeps skins; nothing else is ever fetched. */
    private static final String SKIN_HOST = "textures.minecraft.net";
    private static final int MAX_SKIN = 256 * 1024;
    private static final long RETRY_MS = 5 * 60_000;

    private record Face(String url, byte[] png, long tried) {}

    private static final Map<UUID, Face> FACES = new ConcurrentHashMap<>();
    private static volatile HttpClient client;

    private Faces() {}

    /** The face's PNG, or null while there is none. Any thread. */
    static byte[] png(UUID player) {
        Face face = FACES.get(player);
        return face == null ? null : face.png;
    }

    /** Server thread: fetches this player's face in the background unless it is here already or just failed. */
    static void want(MinecraftServer server, GameProfile profile) {
        var skin = server.services().sessionService().getTextures(profile).skin();
        if (skin == null) return;
        String url = skin.getUrl();
        UUID id = profile.id();
        long now = System.currentTimeMillis();
        Face had = FACES.get(id);
        if (had != null && had.url.equals(url) && (had.png != null || now - had.tried < RETRY_MS)) return;
        URI uri = skinUri(url);
        if (uri == null) return;
        // until the new one is here (or if it fails) the old face stays
        FACES.put(id, new Face(url, had == null ? null : had.url.equals(url) ? had.png : null, now));
        if (client == null) client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        client.sendAsync(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).build(), HttpResponse.BodyHandlers.ofInputStream())
                .thenAccept(response -> {
                    try (InputStream in = response.body()) {
                        byte[] bytes = in.readNBytes(MAX_SKIN + 1);
                        if (response.statusCode() != 200 || bytes.length > MAX_SKIN) return;
                        byte[] png = cut(bytes);
                        if (png != null) FACES.put(id, new Face(url, png, now));
                    } catch (IOException | RuntimeException e) {
                        LogUtils.getLogger().warn("Mechanical map: no face for {} ({})", profile.name(), e.toString());
                    }
                });
    }

    /** The skin's address over https, or null when it is not on Mojang's skin server. */
    static URI skinUri(String url) {
        try {
            URI uri = URI.create(url);
            return SKIN_HOST.equals(uri.getHost()) && uri.getRawPath().startsWith("/texture/") ? URI.create("https://" + SKIN_HOST + uri.getRawPath()) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** The 8x8 face with the hat layer over it, as the game shows it; null when the bytes are not a skin. */
    static byte[] cut(byte[] skinPng) throws IOException {
        BufferedImage skin = ImageIO.read(new ByteArrayInputStream(skinPng));
        if (skin == null || skin.getWidth() != 64 || skin.getHeight() != 64 && skin.getHeight() != 32) return null;
        BufferedImage face = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = face.createGraphics();
        g.drawImage(skin.getSubimage(8, 8, 8, 8), 0, 0, null);
        g.drawImage(skin.getSubimage(40, 8, 8, 8), 0, 0, null);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(face, "png", out);
        return out.toByteArray();
    }
}
