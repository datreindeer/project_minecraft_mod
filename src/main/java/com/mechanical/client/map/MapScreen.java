package com.mechanical.client.map;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mechanical.Mechanical;
import com.mechanical.client.ui.HideUi;
import com.mechanical.map.MapNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerSkin;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The fullscreen world map (M): what you and your tribe have explored, inked on parchment, with you and your tribe
 * mates on it, and over the land what the dashboard map shows of what you have explored: tribe lands, villages, fires, Ancient
 * Oaks and waystones ({@link MapMarks}), with a legend that jumps to them and a readout of what the cursor is on. Drag to pan, scroll to zoom towards the cursor, Space or the button to follow yourself again. While open
 * it asks the server for the regions in view every second (sooner when new ones come into view); the server decides
 * what may be seen ({@link MapNetwork}). The pictures are dropped when the screen closes.
 */
public final class MapScreen extends Screen {
    private static final int MARGIN = 6, PAD = 8, HEADER = 24, FOOTER = 18, REGION = 512;
    private static final int REFRESH_TICKS = 20, SOONEST_TICKS = MapNetwork.MIN_TICKS + 2;
    private static final int INK = 0xFF3A2A18, INK_SOFT = 0x803A2A18, PLATE = 0xD0201509, CARD = 0xC8E8D6AC;
    private static final float MAX_ZOOM = 8;
    /** GUI pixels per block, kept between openings. */
    private static float zoom = 1;

    private record Picture(Identifier id, int stamp) {}

    private record Box(int x, int y, int w, int h) {
        boolean overlaps(Box o) {
            return x < o.x + o.w && o.x < x + w && y < o.y + o.h && o.y < y + h;
        }
    }

    private record Mark(MapNetwork.Person who, double x, double y, boolean off) {}

    private final Map<Long, Picture> pictures = new HashMap<>();
    /** The stamp the server gave each region, also those with nothing to show. */
    private final Map<Long, Integer> stamps = new HashMap<>();
    private final Set<Long> asked = new HashSet<>();
    private MapNetwork.People people = new MapNetwork.People(-1, List.of());
    private MapMarks marks = MapMarks.NONE;
    private String marksJson = "";
    private List<Row> rows = List.of();
    private static boolean legendOpen = true;
    private int legendScroll;
    /** What the cursor is on this frame, on the map or in the legend; either may be null. */
    private MapMarks.Land hoverLand;
    private MapMarks.Place hoverPlace;
    private double centreX, centreZ;
    private boolean follow = true, dragging, answered;
    private int sinceRequest = REFRESH_TICKS, age;

    public MapScreen() {
        super(Component.translatable("gui.mechanical.map.title"));
    }

    // ------------------------------------------------------------------ data

    public void people(MapNetwork.People now) {
        people = now;
        answered = true;
    }

    public void features(String json) {
        if (json.equals(marksJson)) return;
        try {
            marks = MapMarks.parse(json);
            marksJson = json;
            rows = rows(marks);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("Mechanical map: unreadable marks", e);
        }
    }

    public void tile(MapNetwork.Tile tile) {
        long key = ChunkPos.pack(tile.x(), tile.z());
        if (tile.stamp() == 0) stamps.remove(key);
        else stamps.put(key, tile.stamp());
        if (tile.png().length == 0) {
            Picture old = pictures.remove(key);
            if (old != null) minecraft.getTextureManager().release(old.id);
            return;
        }
        try {
            NativeImage image = NativeImage.read(tile.png());
            MapArt.ink(image, tile.x() * REGION, tile.z() * REGION);
            Identifier id = Identifier.fromNamespaceAndPath(Mechanical.MODID, "map_region/" + tile.x() + "_" + tile.z());
            MapArt.register(id, image); // replaces and closes an older picture under the same id
            pictures.put(key, new Picture(id, tile.stamp()));
        } catch (IOException e) {
            LogUtils.getLogger().warn("Mechanical map: unreadable region {} {}", tile.x(), tile.z(), e);
        }
    }

    private boolean overworld() {
        return minecraft.level != null && minecraft.level.dimension() == Level.OVERWORLD;
    }

    /** The regions in view, those nearest the middle first. */
    private List<int[]> visible() {
        double halfW = mapW() / 2.0 / zoom, halfH = mapH() / 2.0 / zoom;
        List<int[]> out = new ArrayList<>();
        for (int x = Math.floorDiv((int) Math.floor(centreX - halfW), REGION); x * (double) REGION < centreX + halfW; x++)
            for (int z = Math.floorDiv((int) Math.floor(centreZ - halfH), REGION); z * (double) REGION < centreZ + halfH; z++) out.add(new int[] {x, z});
        out.sort(java.util.Comparator.comparingDouble(r -> Math.hypot((r[0] + 0.5) * REGION - centreX, (r[1] + 0.5) * REGION - centreZ)));
        return out.size() > MapNetwork.MAX_WANTS ? out.subList(0, MapNetwork.MAX_WANTS) : out;
    }

    @Override
    public void tick() {
        age++;
        sinceRequest++;
        if (!overworld()) return;
        followPlayer(1);
        if (sinceRequest < SOONEST_TICKS) return;
        List<int[]> view = visible();
        boolean fresh = view.stream().anyMatch(r -> !asked.contains(ChunkPos.pack(r[0], r[1])));
        if (!fresh && sinceRequest < REFRESH_TICKS) return;
        sinceRequest = 0;
        asked.clear();
        List<MapNetwork.Want> wants = new ArrayList<>();
        for (int[] r : view) {
            long key = ChunkPos.pack(r[0], r[1]);
            asked.add(key);
            wants.add(new MapNetwork.Want(r[0], r[1], stamps.getOrDefault(key, 0)));
        }
        ClientPacketDistributor.sendToServer(new MapNetwork.Request(wants));
    }

    @Override
    public void removed() {
        pictures.values().forEach(p -> minecraft.getTextureManager().release(p.id));
        pictures.clear();
        stamps.clear();
        asked.clear();
    }

    @Override
    public boolean isPauseScreen() {
        return false; // a paused singleplayer world would never answer
    }

    // ------------------------------------------------------------------ layout and input

    private int mapX() {
        return MARGIN + PAD;
    }

    private int mapY() {
        return MARGIN + HEADER;
    }

    private int mapW() {
        return width - 2 * (MARGIN + PAD);
    }

    private int mapH() {
        return height - mapY() - MARGIN - FOOTER - 4;
    }

    private boolean inMap(double x, double y) {
        return x >= mapX() && x < mapX() + mapW() && y >= mapY() && y < mapY() + mapH();
    }

    /** Far out is limited so the regions in view always fit in one request (at most 7 x 6). */
    private float minZoom() {
        return Math.max(0.25F, Math.max(mapW() / (REGION * 5F), mapH() / (REGION * 4F)));
    }

    private double screenX(double worldX) {
        return mapX() + mapW() / 2.0 + (worldX - centreX) * zoom;
    }

    private double screenY(double worldZ) {
        return mapY() + mapH() / 2.0 + (worldZ - centreZ) * zoom;
    }

    private void followPlayer(float partial) {
        if (!follow || minecraft.player == null) return;
        var at = minecraft.player.getPosition(partial);
        centreX = at.x;
        centreZ = at.z;
    }

    private void recentre() {
        follow = true;
        followPlayer(1);
    }

    @Override
    protected void init() {
        MapArt.make();
        zoom = Math.clamp(zoom, minZoom(), MAX_ZOOM);
        followPlayer(1);
        Component label = Component.translatable("gui.mechanical.map.centre");
        int w = font.width(label) + 14;
        Button centre = addRenderableWidget(Button.builder(label, b -> recentre()).bounds(width - MARGIN - PAD - w, MARGIN + 4, w, 16).build());
        centre.active = overworld();
        int right = width - MARGIN - PAD - w - 3;
        right = button(Component.literal("+"), 16, right, () -> zoomAt(2, mapX() + mapW() / 2.0, mapY() + mapH() / 2.0));
        right = button(Component.literal("-"), 16, right, () -> zoomAt(0.5F, mapX() + mapW() / 2.0, mapY() + mapH() / 2.0));
        Component fit = Component.translatable("gui.mechanical.map.fit");
        right = button(fit, font.width(fit) + 10, right, this::fitTribes);
        Component legend = Component.translatable("gui.mechanical.map.legend");
        button(legend, font.width(legend) + 10, right, () -> legendOpen = !legendOpen);
    }

    /** A header button ending at {@code right}; returns where the next one ends. */
    private int button(Component label, int w, int right, Runnable press) {
        addRenderableWidget(Button.builder(label, b -> press.run()).bounds(right - w, MARGIN + 4, w, 16).build()).active = overworld();
        return right - w - 3;
    }

    private void fitTribes() {
        int[] all = null;
        for (MapMarks.Land l : marks.lands()) {
            if (l.kind() != MapMarks.Kind.TRIBE) continue;
            int[] b = l.shape().bounds();
            all = all == null ? b : new int[] {Math.min(all[0], b[0]), Math.min(all[1], b[1]), Math.max(all[2], b[2]), Math.max(all[3], b[3])};
        }
        if (all != null) fit(all);
        else recentre();
    }

    private void fit(int[] b) {
        follow = false;
        centreX = (b[0] + b[2]) / 2.0;
        centreZ = (b[1] + b[3]) / 2.0;
        zoom = Math.clamp(Math.min(mapW() / (b[2] - b[0] + 96F), mapH() / (b[3] - b[1] + 96F)), minZoom(), MAX_ZOOM);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (overworld() && inLegend(event.x(), event.y())) {
            Row row = rowAt(event.y());
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && row != null && row.land != null) fit(row.land.shape().bounds());
            return true;
        }
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT || !inMap(event.x(), event.y()) || !overworld()) return false;
        setFocused(null);
        dragging = true;
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!dragging) return super.mouseDragged(event, dx, dy);
        follow = false;
        centreX -= dx / zoom;
        centreZ -= dy / zoom;
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!dragging) return super.mouseReleased(event);
        dragging = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (!inMap(x, y) || !overworld() || scrollY == 0) return super.mouseScrolled(x, y, scrollX, scrollY);
        if (inLegend(x, y)) {
            legendScroll -= (int) Math.signum(scrollY) * 2; // clamped when drawn
            return true;
        }
        zoomAt((float) Math.pow(1.25, scrollY), x, y);
        return true;
    }

    /** Zooms so the spot under (x, y) stays there. */
    private void zoomAt(float by, double x, double y) {
        float next = Math.clamp(zoom * by, minZoom(), MAX_ZOOM);
        if (next == zoom) return;
        double offX = x - mapX() - mapW() / 2.0, offY = y - mapY() - mapH() / 2.0;
        centreX += offX / zoom - offX / next;
        centreZ += offY / zoom - offY / next;
        zoom = next;
        follow = false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (MapClient.OPEN.matches(event)) {
            onClose();
            return true;
        }
        if (event.key() == InputConstants.KEY_SPACE && overworld()) {
            recentre();
            return true;
        }
        return super.keyPressed(event);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        HideUi.panel(g, MARGIN, MARGIN, width - 2 * MARGIN, height - 2 * MARGIN);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        int x = mapX(), y = mapY(), w = mapW(), h = mapH();
        boolean here = overworld() && minecraft.player != null;
        if (here) followPlayer(a);
        g.text(font, title, x + 2, MARGIN + 8, HideUi.NAME);
        if (here) {
            Component at = Component.translatable("gui.mechanical.map.position", minecraft.player.getBlockX(), minecraft.player.getBlockZ());
            g.text(font, at, x + font.width(title) + 14, MARGIN + 8, HideUi.DIM);
        }
        Component hint = Component.translatable("gui.mechanical.map.hint", MapClient.OPEN.getTranslatedKeyMessage());
        g.centeredText(font, hint, width / 2, y + h + 5, HideUi.DIM);

        HideUi.inset(g, x - 2, y - 2, w + 4, h + 4);
        g.enableScissor(x, y, x + w, y + h);
        paper(g, here);
        if (here) {
            land(g);
            hover(mouseX, mouseY);
            grid(g);
            lands(g);
            frame(g);
            compass(g, x + w - 26, y + h - 28);
            scaleBar(g, x + 13, y + h - 15);
            places(g);
            List<Box> taken = new ArrayList<>();
            // names keep off the compass, the scale and the legend
            taken.add(new Box(mapX() + mapW() - 42, mapY() + mapH() - 56, 42, 56));
            taken.add(new Box(mapX(), mapY() + mapH() - 30, 140, 30));
            if (legendShown()) taken.add(legendBox());
            markers(g, a, taken);
            landNames(g, taken);
            legend(g, mouseX, mouseY);
            readout(g, mouseX, mouseY);
            if (pictures.isEmpty()) note(g, Component.translatable(answered && age > 60 ? "gui.mechanical.map.empty" : "gui.mechanical.map.drawing"), y + h / 4);
        } else {
            frame(g);
            compass(g, width / 2, y + h / 2 - 22);
            note(g, Component.translatable("gui.mechanical.map.overworld_only"), y + h / 2 + 12);
        }
        g.disableScissor();
        super.extractRenderState(g, mouseX, mouseY, a);
    }

    /** The sheet itself, sliding with the land when you pan. */
    private void paper(GuiGraphicsExtractor g, boolean slide) {
        int size = MapArt.PARCHMENT_SIZE;
        int ox = slide ? Math.floorMod((int) Math.round(-centreX * zoom), size) : 0, oy = slide ? Math.floorMod((int) Math.round(-centreZ * zoom), size) : 0;
        for (int px = mapX() + ox - size; px < mapX() + mapW(); px += size)
            for (int py = mapY() + oy - size; py < mapY() + mapH(); py += size)
                g.blit(RenderPipelines.GUI_TEXTURED, MapArt.PAPER, px, py, 0, 0, size, size, size, size);
    }

    /** The regions, laid out in whole screen pixels so neighbours meet without seams. */
    private void land(GuiGraphicsExtractor g) {
        float gui = minecraft.getWindow().getGuiScale();
        var pose = g.pose();
        pose.pushMatrix();
        pose.scale(1 / gui, 1 / gui);
        for (int[] r : visible()) {
            Picture picture = pictures.get(ChunkPos.pack(r[0], r[1]));
            if (picture == null) continue;
            int x0 = (int) Math.round(screenX(r[0] * (double) REGION) * gui), x1 = (int) Math.round(screenX((r[0] + 1) * (double) REGION) * gui);
            int y0 = (int) Math.round(screenY(r[1] * (double) REGION) * gui), y1 = (int) Math.round(screenY((r[1] + 1) * (double) REGION) * gui);
            g.blit(RenderPipelines.GUI_TEXTURED, picture.id, x0, y0, 0, 0, x1 - x0, y1 - y0, x1 - x0, y1 - y0);
        }
        pose.popMatrix();
    }

    /** Aged edges and a double ink rule round the sheet. */
    private void frame(GuiGraphicsExtractor g) {
        int x = mapX(), y = mapY(), r = x + mapW(), b = y + mapH();
        for (int i = 0; i < 10; i++) {
            int shade = (10 - i) * 7 << 24 | 0x3A2408;
            g.fill(x + i, y + i, r - i, y + i + 1, shade);
            g.fill(x + i, b - i - 1, r - i, b - i, shade);
            g.fill(x + i, y + i + 1, x + i + 1, b - i - 1, shade);
            g.fill(r - i - 1, y + i + 1, r - i, b - i - 1, shade);
        }
        g.outline(x + 3, y + 3, mapW() - 6, mapH() - 6, INK);
        g.outline(x + 5, y + 5, mapW() - 10, mapH() - 10, INK_SOFT);
    }

    /** A four-pointed compass star with north marked, centred on (cx, cy). */
    private void compass(GuiGraphicsExtractor g, int cx, int cy) {
        int disc = 13;
        for (int dy = -disc; dy <= disc; dy++) {
            int half = (int) Math.sqrt(disc * disc - dy * dy);
            g.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, CARD);
        }
        for (int i = 0; i <= 13; i++) {
            int half = (13 - i) / 4;
            // each point is split down the middle, lit on one side
            g.fill(cx - half, cy - i, cx + 1, cy - i + 1, 0xFFB0382A);
            g.fill(cx + 1, cy - i, cx + half + 1, cy - i + 1, 0xFF6E1E16);
            g.fill(cx - half, cy + i, cx + 1, cy + i + 1, INK);
            g.fill(cx + 1, cy + i, cx + half + 1, cy + i + 1, 0xFF7A6040);
            g.fill(cx - i, cy - half, cx - i + 1, cy + 1, 0xFF7A6040);
            g.fill(cx - i, cy + 1, cx - i + 1, cy + half + 1, INK);
            g.fill(cx + i, cy - half, cx + i + 1, cy + 1, INK);
            g.fill(cx + i, cy + 1, cx + i + 1, cy + half + 1, 0xFF7A6040);
        }
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFE8C060);
        Component north = Component.translatable("gui.mechanical.map.north");
        int tw = font.width(north);
        g.fill(cx - tw / 2 - 2, cy - 25, cx - tw / 2 + tw + 2, cy - 15, CARD);
        g.text(font, north, cx - tw / 2, cy - 24, INK, false);
    }

    /** A checkered rule a round number of blocks long, at most 90 pixels. */
    private void scaleBar(GuiGraphicsExtractor g, int x, int y) {
        int blocks = 1000;
        for (int n : new int[] {500, 200, 100, 50, 20, 10})
            if (blocks * zoom > 90) blocks = n;
        int len = Math.round(blocks * zoom);
        Component label = Component.translatable("gui.mechanical.map.zoom", blocks, String.format(zoom >= 1 ? "%.1f" : "%.2f", zoom));
        g.fill(x - 4, y - 13, x + Math.max(len, font.width(label)) + 4, y + 7, CARD);
        g.text(font, label, x, y - 10, INK, false);
        g.fill(x - 1, y, x + len + 1, y + 4, INK);
        for (int i = 1; i < 4; i += 2) g.fill(x + len * i / 4, y + 1, x + len * (i + 1) / 4, y + 3, 0xFFF0E2BC);
    }

    // ------------------------------------------------------------------ what is marked on the land

    private void hover(int mouseX, int mouseY) {
        hoverLand = null;
        hoverPlace = null;
        if (inLegend(mouseX, mouseY)) {
            Row row = rowAt(mouseY);
            if (row != null) hoverLand = row.land;
            return;
        }
        if (!inMap(mouseX, mouseY)) return;
        for (MapMarks.Place p : marks.places())
            if (Math.hypot(screenX(p.x() + 0.5) - mouseX, screenY(p.z() + 0.5) - mouseY) < 9) hoverPlace = p;
        if (hoverPlace != null) return;
        double x = worldX(mouseX), z = worldZ(mouseY);
        for (MapMarks.Land l : marks.lands()) // the last is drawn on top
            if (l.shape().contains(x, z)) hoverLand = l;
    }

    private double worldX(double screenX) {
        return centreX + (screenX - mapX() - mapW() / 2.0) / zoom;
    }

    private double worldZ(double screenY) {
        return centreZ + (screenY - mapY() - mapH() / 2.0) / zoom;
    }

    /** Region borders, and every chunk when close. */
    private void grid(GuiGraphicsExtractor g) {
        int step = zoom >= 3 ? 16 : REGION, colour = step == 16 ? 0x20000000 : 0x383A2A18;
        for (double x = Math.ceil(worldX(mapX()) / step) * step; screenX(x) < mapX() + mapW(); x += step)
            g.fill((int) screenX(x), mapY(), (int) screenX(x) + 1, mapY() + mapH(), colour);
        for (double z = Math.ceil(worldZ(mapY()) / step) * step; screenY(z) < mapY() + mapH(); z += step)
            g.fill(mapX(), (int) screenY(z), mapX() + mapW(), (int) screenY(z) + 1, colour);
    }

    /** Washes and inked edges, in whole screen pixels like the regions. The one under the cursor is lit and edged in gold. */
    private void lands(GuiGraphicsExtractor g) {
        float gui = minecraft.getWindow().getGuiScale();
        var pose = g.pose();
        pose.pushMatrix();
        pose.scale(1 / gui, 1 / gui);
        int px = Math.max(1, Math.round(gui / 2));
        for (MapMarks.Land l : marks.lands()) {
            boolean village = l.kind() == MapMarks.Kind.VILLAGE, lit = l == hoverLand;
            wash(g, l.shape(), (lit ? 0x70 : village ? 0x2C : 0x48) << 24 | l.colour(), gui);
            // every edge is drawn inwards, so two lands side by side each keep their own band
            edges(g, l.shape(), gui, (lit ? 7 : village ? 3 : 5) * px, lit ? 0xFF000000 | HideUi.GOLD : 0xE0120D09, 0);
            edges(g, l.shape(), gui, (village ? 2 : lit ? 5 : 3) * px, 0xFF000000 | l.colour(), village ? 7 * px : 0);
            if (l.loaded() != null) {
                wash(g, l.loaded(), 0x30FFFFFF, gui);
                edges(g, l.loaded(), gui, px, 0xC0000000 | paler(l.colour()), 5 * px);
            }
        }
        pose.popMatrix();
    }

    private static int paler(int c) {
        int r = (c >> 16 & 255) * 3 / 10 + 178, green = (c >> 8 & 255) * 3 / 10 + 178, b = (c & 255) * 3 / 10 + 178;
        return r << 16 | green << 8 | b;
    }

    private int pixelX(double worldX, float gui) {
        return (int) Math.round(screenX(worldX) * gui);
    }

    private int pixelY(double worldZ, float gui) {
        return (int) Math.round(screenY(worldZ) * gui);
    }

    private void wash(GuiGraphicsExtractor g, MapMarks.Shape shape, int colour, float gui) {
        for (MapMarks.Rect r : shape.fill()) g.fill(pixelX(r.x0(), gui), pixelY(r.z0(), gui), pixelX(r.x1(), gui), pixelY(r.z1(), gui), colour);
    }

    /** Bands {@code thick} pixels wide just inside the outline, dashed when {@code dash} is above 0. */
    private void edges(GuiGraphicsExtractor g, MapMarks.Shape shape, float gui, int thick, int colour, int dash) {
        int left = (int) (mapX() * gui), right = (int) ((mapX() + mapW()) * gui), top = (int) (mapY() * gui), bottom = (int) ((mapY() + mapH()) * gui);
        for (MapMarks.Edge e : shape.edges()) {
            boolean flat = e.z0() == e.z1();
            int x0 = pixelX(e.x0(), gui), y0 = pixelY(e.z0(), gui), x1 = pixelX(e.x1(), gui), y1 = pixelY(e.z1(), gui);
            if (x1 < left || x0 > right || y1 < top || y0 > bottom) continue;
            int from = flat ? x0 : y0, to = flat ? x1 : y1, line = flat ? y0 : x0;
            int across = e.in() > 0 ? line : line - thick;
            int first = from, step = Math.max(1, to - from), length = to - from;
            if (dash > 0) { // dashes stay put on the land, and only those in view are drawn
                step = dash * 2;
                length = dash + dash / 3;
                first = from + Math.max(0, ((flat ? left : top) - from) / step) * step;
                to = Math.min(to, flat ? right : bottom);
            }
            for (int at = first; at < to; at += step) {
                int end = Math.min(to, at + length);
                if (flat) g.fill(at, across, end, across + thick, colour);
                else g.fill(across, at, across + thick, end, colour);
            }
        }
    }

    /** Fires, then the places; the one under the cursor stands in a glow. */
    private void places(GuiGraphicsExtractor g) {
        float k = Math.clamp((float) Math.sqrt(zoom), 0.75F, 1.25F);
        for (MapMarks.Fire f : marks.fires()) {
            double x = screenX(f.x() + 0.5), y = screenY(f.z() + 0.5);
            if (!inMap(x, y)) continue;
            boolean big = f.kind().equals("big_fire_pit"), pit = big || f.kind().equals("fire_pit");
            int size = Math.round((big ? 28 : pit ? 22 : 18) * k);
            icon(g, x, y, size, MapArt.icon(f.kind()), -1);
            if (pit && f.colour() >= 0) icon(g, x, y, size, MapArt.PIT_RING, 0xFF000000 | f.colour());
        }
        for (MapMarks.Place p : marks.places()) {
            double x = screenX(p.x() + 0.5), y = screenY(p.z() + 0.5);
            if (!inMap(x, y)) continue;
            boolean lit = p == hoverPlace;
            if (lit) icon(g, x, y, Math.round(36 * k), MapArt.GLOW, -1);
            icon(g, x, y, Math.round((lit ? 26 : 21) * k), MapArt.icon(p.kind()), -1);
        }
    }

    private void icon(GuiGraphicsExtractor g, double x, double y, int size, Identifier texture, int colour) {
        int full = MapArt.ICON_SIZE;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.scale(size / (float) full, size / (float) full);
        g.blit(RenderPipelines.GUI_TEXTURED, texture, -full / 2, -full / 2, 0, 0, full, full, full, full, colour);
        pose.popMatrix();
    }

    /** Villages in italic sand, tribes' names behind a pennant in their colour; tribes (last in the list) get first pick of the room. */
    private void landNames(GuiGraphicsExtractor g, List<Box> taken) {
        for (MapMarks.Land l : marks.lands().reversed()) {
            double x = screenX(l.labelX()), y = screenY(l.labelZ());
            if (!inMap(x, y)) continue;
            boolean village = l.kind() == MapMarks.Kind.VILLAGE;
            Component name = village ? Component.literal(l.name()).withStyle(ChatFormatting.ITALIC) : Component.literal(l.name());
            int flag = village ? 0 : 9, w = font.width(name) + flag + 8;
            Box box = null;
            // under the land's middle (often a fire or a tent), else further off
            for (int dy : new int[] {12, -24, 26, -38}) {
                Box at = new Box((int) x - w / 2, (int) y + dy, w, 12);
                if (taken.stream().noneMatch(at::overlaps)) {
                    box = at;
                    break;
                }
            }
            if (box == null) continue; // the legend and the readout still name it
            taken.add(box);
            g.fill(box.x, box.y, box.x + box.w, box.y + box.h, l == hoverLand ? 0xF0201509 : 0xB0201509);
            if (l == hoverLand) g.outline(box.x - 1, box.y - 1, box.w + 2, box.h + 2, 0xFF000000 | HideUi.GOLD);
            if (!village) pennant(g, box.x + 4, box.y + 2, l.colour());
            g.text(font, name, box.x + 4 + flag, box.y + 2, village ? 0xFFE8DCC0 : HideUi.TEXT);
        }
    }

    private void pennant(GuiGraphicsExtractor g, int x, int y, int colour) {
        g.fill(x, y, x + 6, y + 5, 0xFF000000 | colour);
        g.fill(x, y + 5, x + 2, y + 8, 0xFF000000 | colour);
        g.fill(x + 4, y + 5, x + 6, y + 8, 0xFF000000 | colour);
        g.fill(x + 2, y + 5, x + 4, y + 6, 0xFF000000 | colour);
    }

    // ------------------------------------------------------------------ legend and readout

    private static final int LEGEND_W = 138, LINE = 11;

    /** A line of the legend: a heading, a land you can click to go to, a count of places or the kept-loaded key. */
    private record Row(Component text, Component count, MapMarks.Land land, Identifier icon, int swatch) {}

    private static List<Row> rows(MapMarks marks) {
        List<Row> out = new ArrayList<>();
        for (MapMarks.Land l : marks.lands())
            if (l.kind() == MapMarks.Kind.TRIBE) out.add(new Row(Component.literal(l.name()), Component.translatable("gui.mechanical.map.chunks", l.chunks()), l, null, l.colour()));
        if (marks.lands().stream().anyMatch(l -> l.loaded() != null))
            out.add(new Row(Component.translatable("gui.mechanical.map.loaded_key"), null, null, null, 0xD8D8D8));
        section(out, "tribe_villages", marks.lands().stream().filter(l -> l.kind() == MapMarks.Kind.TRIBE_VILLAGE || l.kind() == MapMarks.Kind.TRIBE_CAMP).toList());
        section(out, "villages", marks.lands().stream().filter(l -> l.kind() == MapMarks.Kind.VILLAGE).toList());
        List<Row> found = new ArrayList<>();
        for (String kind : new String[] {"ancient_oak", "waystone"}) {
            long n = marks.places().stream().filter(p -> p.kind().equals(kind)).count();
            if (n > 0) found.add(new Row(Component.translatable("gui.mechanical.map.places." + kind), Component.literal(Long.toString(n)), null, MapArt.icon(kind), -1));
        }
        if (!found.isEmpty()) out.add(heading("places"));
        out.addAll(found);
        return out;
    }

    private static Row heading(String key) {
        return new Row(Component.translatable("gui.mechanical.map." + key), null, null, null, -1);
    }

    private static void section(List<Row> out, String key, List<MapMarks.Land> lands) {
        if (!lands.isEmpty()) out.add(heading(key));
        for (MapMarks.Land l : lands) out.add(new Row(Component.literal(l.name()), null, l, null, l.colour()));
    }

    private boolean legendShown() {
        return legendOpen && !rows.isEmpty();
    }

    private int legendLines() {
        return Math.min(rows.size(), Math.max(1, (mapH() - 52) / LINE));
    }

    private Box legendBox() {
        return new Box(mapX() + 8, mapY() + 8, Math.min(LEGEND_W, mapW() / 2), legendLines() * LINE + 7);
    }

    private boolean inLegend(double x, double y) {
        if (!legendShown()) return false;
        Box b = legendBox();
        return x >= b.x && x < b.x + b.w && y >= b.y && y < b.y + b.h;
    }

    private Row rowAt(double y) {
        int i = (int) Math.floor((y - legendBox().y - 4) / LINE);
        int at = i + Math.clamp(legendScroll, 0, rows.size() - legendLines());
        return i >= 0 && i < legendLines() ? rows.get(at) : null;
    }

    private void legend(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!legendShown()) return;
        Box b = legendBox();
        int lines = legendLines();
        legendScroll = Math.clamp(legendScroll, 0, rows.size() - lines);
        g.fill(b.x, b.y, b.x + b.w, b.y + b.h, 0xF4201509);
        g.outline(b.x, b.y, b.w, b.h, INK);
        Row under = inLegend(mouseX, mouseY) ? rowAt(mouseY) : null;
        for (int i = 0; i < lines; i++) {
            Row row = rows.get(i + legendScroll);
            int y = b.y + 4 + i * LINE, x = b.x + 5, right = b.x + b.w - 7;
            boolean heading = row.swatch < 0 && row.icon == null;
            if (row.land != null && row == under) g.fill(b.x + 1, y - 1, b.x + b.w - 1, y + LINE - 1, 0x40FFD870);
            if (row.icon != null) icon(g, x + 4, y + 4, 12, row.icon, -1);
            else if (!heading) {
                g.fill(x, y, x + 8, y + 8, 0xFF000000);
                g.fill(x + 1, y + 1, x + 7, y + 7, 0xFF000000 | row.swatch);
            }
            if (row.count != null) {
                g.text(font, row.count, right - font.width(row.count), y, HideUi.DIM);
                right -= font.width(row.count) + 4;
            }
            int tx = heading ? x : x + 12;
            g.enableScissor(tx, y - 1, right, y + LINE);
            g.text(font, row.text, tx, y, heading ? HideUi.GOLD : HideUi.TEXT);
            g.disableScissor();
        }
        if (rows.size() > lines) { // a thumb showing how far down you are
            int track = lines * LINE, thumb = Math.max(8, track * lines / rows.size()), at = (track - thumb) * legendScroll / (rows.size() - lines);
            g.fill(b.x + b.w - 4, b.y + 4 + at, b.x + b.w - 2, b.y + 4 + at + thumb, 0xA0E8D6AC);
        }
    }

    /** Where the cursor is, and what is there, in the top right corner. */
    private void readout(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        if (!inMap(mouseX, mouseY) || inLegend(mouseX, mouseY)) return;
        int bx = (int) Math.floor(worldX(mouseX)), bz = (int) Math.floor(worldZ(mouseY));
        Component where = Component.translatable("gui.mechanical.map.coords", bx, bz), what, note = null;
        int swatch = -1;
        if (hoverPlace != null) {
            String kind = hoverPlace.kind();
            what = Component.translatable(kind.equals("tribe_village") ? "gui.mechanical.map.tribe_village" : "gui.mechanical.map.place." + kind);
            note = Component.translatable("gui.mechanical.map.coords", hoverPlace.x(), hoverPlace.z());
        } else if (hoverLand != null) {
            what = Component.literal(hoverLand.name());
            swatch = hoverLand.colour();
            note = switch (hoverLand.kind()) {
                case TRIBE -> hoverLand.loaded() != null && hoverLand.loaded().contains(bx + 0.5, bz + 0.5) ? Component.translatable("gui.mechanical.map.kept") : null;
                case TRIBE_VILLAGE -> Component.translatable("gui.mechanical.map.tribe_village");
                case TRIBE_CAMP -> Component.translatable("gui.mechanical.map.tribe_camp");
                case VILLAGE -> Component.translatable("gui.mechanical.map.village");
            };
        } else what = Component.translatable("gui.mechanical.map.unclaimed");
        int flag = swatch < 0 ? 0 : 9;
        int w = Math.max(font.width(where), Math.max(font.width(what) + flag, note == null ? 0 : font.width(note))) + 12, h = note == null ? 28 : 39;
        int x = mapX() + mapW() - 8 - w, y = mapY() + 8;
        g.fill(x, y, x + w, y + h, PLATE);
        g.outline(x, y, w, h, INK);
        g.text(font, where, x + 6, y + 5, HideUi.DIM);
        if (swatch >= 0) pennant(g, x + 6, y + 16, swatch);
        g.text(font, what, x + 6 + flag, y + 16, HideUi.TEXT);
        if (note != null) g.text(font, note, x + 6, y + 27, HideUi.DIM);
    }

    // ------------------------------------------------------------------ people

    private void markers(GuiGraphicsExtractor g, float partial, List<Box> taken) {
        var player = minecraft.player;
        var me = player.getPosition(partial);
        double sx = screenX(me.x), sy = screenY(me.z);
        boolean selfShown = inMap(sx, sy);
        if (selfShown) taken.add(new Box((int) sx - 9, (int) sy - 9, 18, 18));
        int colour = people.colour() < 0 ? 0xFFFFFFFF : 0xFF000000 | DyeColor.byId(people.colour()).getTextureDiffuseColor();
        List<Mark> shown = new ArrayList<>();
        for (MapNetwork.Person p : people.mates()) {
            double px = screenX(p.x() + 0.5), py = screenY(p.z() + 0.5);
            // a mate outside the sheet waits at its edge, towards them, with how far they are
            double cx = Math.clamp(px, mapX() + 12, mapX() + mapW() - 12), cy = Math.clamp(py, mapY() + 12, mapY() + mapH() - 12);
            // and no pin hides the compass or the scale in the bottom corners
            if (cx < mapX() + 120 || cx > mapX() + mapW() - 48) cy = Math.min(cy, mapY() + mapH() - 64);
            shown.add(new Mark(p, cx, cy, !inMap(px, py)));
            taken.add(new Box((int) cx - 7, (int) cy - 7, 14, 14));
        }
        for (Mark m : shown) {
            if (!m.off) { // which way they face
                float turn = (float) Math.toRadians(m.who.yaw());
                sprite(g, m.x - Math.sin(turn) * 12, m.y + Math.cos(turn) * 12, 9, turn + (float) Math.PI, MapArt.ARROW, 0xFFFFFFFF);
            }
            var info = minecraft.getConnection().getPlayerInfo(m.who.name());
            if (info != null) {
                face(g, m.x, m.y, info.getSkin(), info.showHat(), colour);
            } else {
                sprite(g, m.x, m.y, 12, 0, MapArt.PIN_RIM, 0xFFFFFFFF);
                sprite(g, m.x, m.y, 12, 0, MapArt.PIN_CORE, colour);
            }
        }
        // yaw 0 faces south (down the map), and the arrow is painted pointing north
        if (selfShown) {
            // your face, with the arrow riding round it the way you look
            float yaw = (float) Math.toRadians(player.getViewYRot(partial));
            face(g, sx, sy, player.getSkin(), player.isModelPartShown(PlayerModelPart.HAT), HideUi.GOLD);
            sprite(g, sx - Math.sin(yaw) * 13, sy + Math.cos(yaw) * 13, 11, yaw + (float) Math.PI, MapArt.ARROW, 0xFFFFFFFF);
        }
        if (selfShown) label(g, taken, Component.translatable("gui.mechanical.map.you"), sx, sy, HideUi.GOLD, HideUi.GOLD);
        for (Mark m : shown) {
            Component name = m.off ? Component.translatable("gui.mechanical.map.far", m.who.name(), (int) Math.hypot(m.who.x() - me.x, m.who.z() - me.z))
                    : Component.literal(m.who.name());
            label(g, taken, name, m.x, m.y, colour, HideUi.TEXT);
        }
    }

    /** A player's face in a frame of the given colour, inked round, centred on (x, y). */
    private void face(GuiGraphicsExtractor g, double x, double y, PlayerSkin skin, boolean hat, int frame) {
        int left = (int) Math.round(x) - 5, top = (int) Math.round(y) - 5;
        g.fill(left - 2, top - 2, left + 12, top + 12, INK);
        g.fill(left - 1, top - 1, left + 11, top + 11, frame);
        PlayerFaceExtractor.extractRenderState(g, skin.body().texturePath(), left, top, 10, hat, false, -1);
    }

    private void sprite(GuiGraphicsExtractor g, double x, double y, int size, float turn, Identifier texture, int colour) {
        int full = MapArt.MARKER_SIZE;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) x, (float) y);
        pose.rotate(turn);
        pose.scale(size / (float) full, size / (float) full);
        g.blit(RenderPipelines.GUI_TEXTURED, texture, -full / 2, -full / 2, 0, 0, full, full, full, full, colour);
        pose.popMatrix();
    }

    /** A name plate over its marker, or under it, or further off: the first place no other plate or marker is, kept on the sheet. */
    private void label(GuiGraphicsExtractor g, List<Box> taken, Component text, double x, double y, int underline, int colour) {
        int w = font.width(text) + 8, h = 13;
        int left = (int) Math.clamp(x - w / 2.0, mapX() + 7, Math.max(mapX() + 7, mapX() + mapW() - 7 - w));
        Box box = null;
        for (int dy : new int[] {-22, 10, -36, 24, -50, 38, -64, 52, -78}) {
            Box at = new Box(left, (int) Math.clamp(y + dy, mapY() + 7, mapY() + mapH() - 7 - h), w, h);
            if (box == null) box = at;
            if (taken.stream().noneMatch(at::overlaps)) {
                box = at;
                break;
            }
        }
        taken.add(box);
        g.fill(box.x, box.y, box.x + box.w, box.y + box.h, PLATE);
        g.fill(box.x, box.y + box.h - 1, box.x + box.w, box.y + box.h, underline);
        g.text(font, text, box.x + 4, box.y + 3, colour);
    }

    /** A message on its own plate in the middle of the sheet. */
    private void note(GuiGraphicsExtractor g, Component text, int y) {
        var lines = font.split(text, Math.min(260, mapW() - 40));
        int w = 0;
        for (var line : lines) w = Math.max(w, font.width(line));
        HideUi.panel(g, width / 2 - w / 2 - 10, y, w + 20, lines.size() * 11 + 14);
        for (int i = 0; i < lines.size(); i++) g.text(font, lines.get(i), width / 2 - font.width(lines.get(i)) / 2, y + 8 + i * 11, HideUi.TEXT);
    }
}
