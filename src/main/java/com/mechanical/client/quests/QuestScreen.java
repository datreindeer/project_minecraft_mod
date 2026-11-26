package com.mechanical.client.quests;

import static com.mechanical.client.ui.HideUi.BAD;
import static com.mechanical.client.ui.HideUi.DARK;
import static com.mechanical.client.ui.HideUi.DIM;
import static com.mechanical.client.ui.HideUi.EDGE_DARK;
import static com.mechanical.client.ui.HideUi.GOLD;
import static com.mechanical.client.ui.HideUi.GOOD;
import static com.mechanical.client.ui.HideUi.NAME;
import static com.mechanical.client.ui.HideUi.TEXT;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mechanical.Mechanical;
import com.mechanical.client.ui.HideUi;
import com.mechanical.quests.QuestLayout;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * The quest screen, in place of vanilla's advancements screen (every way of opening that one opens this). Chapters (the
 * advancement tabs) on the left; the chapter's quests as a graph that is dragged and zoomed, joined from each quest to
 * those it leads to, green when done, gold when it can be done now, dull with a padlock when something else comes first;
 * and a pane on the right: the quest clicked (what to do, a checklist, what comes before and after), or with none clicked
 * the list of what can be done now. On narrow screens the pane lies over the graph and only shows for a clicked quest.
 */
public class QuestScreen extends Screen implements ClientAdvancements.Listener {
    private static final float[] ZOOMS = {0.25F, 0.35F, 0.5F, 0.67F, 0.75F, 1F, 1.25F, 1.5F, 2F};
    private static final int ONE = 5; // ZOOMS[ONE] == 1
    private static final int MARGIN = 4, PAD = 7, HEADER = 22, WIDE_SIDEBAR = 126, NARROW_SIDEBAR = 28;
    private static final int LINE_DONE = 0xFF5F8A3C, LINE_READY = 0xFFD8A845, LINE_LOCKED = 0xFF4C3D2E, LINE_PICKED = 0xFFF0E4C4;

    /** Where a chapter was left: the pan (graph pixels), the zoom and the clicked quest. */
    private static final class View {
        double panX, panY;
        int zoom = ONE;
        Identifier picked;
        /** The graph's size when last shown. */
        int w, h;
    }

    // shortcut: kept for the game session only; write them to a file if players want them across restarts
    private static final Map<Identifier, View> VIEWS = new HashMap<>();
    private static Identifier lastChapter;

    /** Something clickable (and maybe with a tooltip) drawn this frame. */
    private record Hit(int x, int y, int w, int h, Runnable click, List<Component> tip, ItemStack item) {
        boolean has(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final Screen parent;
    private ClientAdvancements advancements;
    private QuestBook book;
    private boolean stale;
    private QuestBook.Chapter chapter;
    private View view;

    // areas, set by init
    private boolean wide, docked;
    private int sideX, sideW, top, bottom, mainX, right, bodyY, paneW;
    private Button nextButton, zoomOut, zoomIn, fitButton;

    private final List<Hit> hits = new ArrayList<>();
    private double paneScroll, paneHeight;
    private String paneShows;
    private boolean pressedInGraph, dragging;
    private double pressX, pressY;
    /** Where a "go to quest" is gliding the pan to, or NaN. */
    private double glideX = Double.NaN, glideY;
    private long lastFrame;

    public QuestScreen(Screen parent) {
        super(Component.translatable("gui.mechanical.quests.title"));
        this.parent = parent;
    }

    // ------------------------------------------------------------------ data

    @Override
    public void onAdvancementsUpdated() {
        stale = true;
    }

    @Override
    public void onAdvancementsCleared() {
        stale = true;
    }

    @Override
    public void onSelectedTabChanged(net.minecraft.advancements.AdvancementHolder tab) {}

    private void rebuild() {
        stale = false;
        book = new QuestBook(advancements);
        Identifier want = chapter != null ? chapter.id() : lastChapter;
        chapter = book.chapters.stream().filter(c -> c.id().equals(want)).findFirst().orElse(book.chapters.isEmpty() ? null : book.chapters.get(0));
        if (chapter != null) view = VIEWS.get(chapter.id());
    }

    private void open(QuestBook.Chapter c) {
        chapter = c;
        lastChapter = c.id();
        view = VIEWS.get(c.id());
        glideX = Double.NaN;
        if (view == null) {
            VIEWS.put(c.id(), view = new View());
            // all of it if it fits; else at full size by the quest already begun, or at the chapter's start
            if (c.layout.width() + 30 <= graphW() && c.layout.height() + 30 <= graphH()) {
                fit();
            } else {
                List<QuestBook.Quest> ready = c.readyNow();
                QuestBook.Quest start = !ready.isEmpty() && ready.get(0).partial() ? ready.get(0) : c.root;
                view.panX = firstLook(graphW() * 0.35 - start.at.x(), c.layout.width(), graphW());
                view.panY = firstLook(graphH() / 2.0 - start.at.y() - QuestLayout.SIZE / 2.0, c.layout.height(), graphH());
            }
        }
        settle();
    }

    /** If the graph's size has changed since the view was last shown, the same spot stays in its middle. */
    private void settle() {
        if (view.w != 0) {
            view.panX += (graphW() - view.w) / 2.0;
            view.panY += (graphH() - view.h) / 2.0;
        }
        view.w = graphW();
        view.h = graphH();
        clampPan();
    }

    @Override
    protected void init() {
        if (advancements == null) {
            advancements = minecraft.player.connection.getAdvancements();
            advancements.setListener(this);
        }
        rebuild();
        wide = width >= 500;
        sideW = wide ? WIDE_SIDEBAR : NARROW_SIDEBAR;
        sideX = MARGIN + PAD;
        top = MARGIN + PAD;
        bottom = height - MARGIN - PAD;
        right = width - MARGIN - PAD;
        mainX = sideX + sideW + 5;
        bodyY = top + HEADER + 3;
        paneW = Math.clamp(width * 3 / 10, 148, 204);
        docked = right - mainX - paneW >= 218;
        if (!docked) paneW = Math.min(Math.max(paneW, 164), right - mainX - 40);

        addRenderableWidget(Button.builder(Component.literal("✕"), b -> onClose()).bounds(right - 20, top + 1, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.mechanical.quests.close"))).build());
        Component next = Component.translatable("gui.mechanical.quests.next").append(" ▶");
        int nw = font.width(next) + 12;
        nextButton = addRenderableWidget(Button.builder(next, b -> next()).bounds(right - 24 - nw, top + 1, nw, 20).build());
        zoomOut = zoomButton("−", "zoom_out", () -> zoom(-1, graphW() / 2.0, graphH() / 2.0));
        zoomIn = zoomButton("+", "zoom_in", () -> zoom(1, graphW() / 2.0, graphH() / 2.0));
        fitButton = zoomButton("⛶", "fit", this::fit);

        if (chapter != null) {
            if (view == null) open(chapter);
            lastChapter = chapter.id();
            settle();
        }
    }

    private Button zoomButton(String label, String tip, Runnable press) {
        return addRenderableWidget(Button.builder(Component.literal(label), b -> press.run()).bounds(0, 0, 16, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.mechanical.quests." + tip))).build());
    }

    @Override
    public void removed() {
        advancements.setListener(null);
        advancements = null; // a screen that comes back listens again
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    // ------------------------------------------------------------------ areas and camera

    private QuestBook.Quest picked() {
        QuestBook.Quest q = view == null || view.picked == null ? null : book.byId.get(view.picked);
        return q;
    }

    private boolean paneOpen() {
        return docked || picked() != null;
    }

    private int paneX() {
        return right - paneW;
    }

    private int graphX() {
        return mainX + 2;
    }

    private int graphY() {
        return bodyY + 2;
    }

    private int graphW() {
        return (docked ? paneX() - 4 : right) - 2 - graphX();
    }

    private int graphH() {
        return bottom - 2 - graphY();
    }

    /** The part of the graph not under the pane. */
    private int clearW() {
        return !docked && picked() != null ? Math.max(60, paneX() - graphX()) : graphW();
    }

    private float scale() {
        return ZOOMS[view.zoom];
    }

    private void clampPan() {
        float s = scale();
        view.panX = clamp(view.panX, chapter.layout.width() * s, graphW());
        view.panY = clamp(view.panY, chapter.layout.height() * s, graphH());
    }

    /** A pan that keeps the chapter in sight: free until it is a third of the view past either end. */
    private static double clamp(double pan, double content, int view) {
        double slack = view / 3.0;
        return Math.clamp(pan, Math.min(0, view - content) - slack, Math.max(0, view - content) + slack);
    }

    /** The first look along one axis: toward {@code pan}, with no empty space past the chapter's edges. */
    private static double firstLook(double pan, int content, int view) {
        return content + 24 <= view ? (view - content) / 2.0 : Math.clamp(pan, view - content - 12, 12);
    }

    /** Zooms a step in or out, keeping the graph point at (x, y) of the graph area where it is. */
    private void zoom(int step, double x, double y) {
        int to = Math.clamp(view.zoom + step, 0, ZOOMS.length - 1);
        float before = scale(), after = ZOOMS[to];
        view.panX = x - (x - view.panX) * after / before;
        view.panY = y - (y - view.panY) * after / before;
        view.zoom = to;
        glideX = Double.NaN;
        clampPan();
    }

    /** The whole chapter, as large as it fits (the smallest zoom for the biggest). */
    private void fit() {
        int z = ONE;
        while (z > 0 && (chapter.layout.width() * ZOOMS[z] > graphW() - 20 || chapter.layout.height() * ZOOMS[z] > graphH() - 20)) z--;
        view.zoom = z;
        view.panX = Math.max(8, (graphW() - chapter.layout.width() * ZOOMS[z]) / 2.0);
        view.panY = Math.max(8, (graphH() - chapter.layout.height() * ZOOMS[z]) / 2.0); // too tall: from the top
        glideX = Double.NaN;
        clampPan();
    }

    /** Picks a quest and glides to it (zooming back in if it was too small to see). */
    private void go(QuestBook.Quest q) {
        view.picked = q.id();
        if (view.zoom < 3) zoom(ONE - view.zoom, clearW() / 2.0, graphH() / 2.0);
        glideX = clearW() / 2.0 - (q.at.x() + QuestLayout.SIZE / 2.0) * scale();
        glideY = graphH() / 2.0 - (q.at.y() + QuestLayout.SIZE / 2.0) * scale();
    }

    /** The next of what can be done now, after the clicked one. */
    private void next() {
        List<QuestBook.Quest> ready = chapter.readyNow();
        if (ready.isEmpty()) { // on to the next chapter with something to do
            int at = book.chapters.indexOf(chapter);
            for (int i = 1; i < book.chapters.size(); i++) {
                QuestBook.Chapter c = book.chapters.get((at + i) % book.chapters.size());
                if (c.ready > 0) {
                    open(c);
                    go(c.readyNow().get(0));
                    return;
                }
            }
            return;
        }
        go(ready.get((ready.indexOf(picked()) + 1) % ready.size()));
    }

    /** Turns to the quest's chapter and, with {@code pick}, clicks and glides to the quest (the dev shots use it). */
    public void show(Identifier id, boolean pick) {
        QuestBook.Quest q = book.byId.get(id);
        if (q == null) return;
        book.chapters.stream().filter(c -> c.quests.contains(q)).findFirst().ifPresent(this::open);
        if (pick) go(q);
        else view.picked = null;
    }

    /** The clicked quest, or null. */
    public Identifier pickedId() {
        QuestBook.Quest q = picked();
        return q == null ? null : q.id();
    }

    /** The pan and zoom, for the dev shots' report. */
    public String camera() {
        return Math.round(view.panX) + "," + Math.round(view.panY) + " x" + scale() + " in " + chapter.id();
    }

    /** The middle of a quest's node on screen. */
    public int[] screenPos(Identifier id) {
        QuestBook.Quest q = book.byId.get(id);
        return new int[] {(int) (graphX() + view.panX + (q.at.x() + 13) * scale()), (int) (graphY() + view.panY + (q.at.y() + 13) * scale())};
    }

    // ------------------------------------------------------------------ input

    private boolean inGraph(double x, double y) {
        return x >= graphX() && x < graphX() + graphW() && y >= graphY() && y < graphY() + graphH() && !inPane(x, y);
    }

    private boolean inPane(double x, double y) {
        return paneOpen() && x >= paneX() && x < right && y >= bodyY && y < bottom;
    }

    private QuestBook.Quest questAt(double mx, double my) {
        if (chapter == null || !inGraph(mx, my)) return null;
        double x = (mx - graphX() - view.panX) / scale(), y = (my - graphY() - view.panY) / scale();
        for (QuestBook.Quest q : chapter.quests)
            if (x >= q.at.x() - 1 && x < q.at.x() + QuestLayout.SIZE + 1 && y >= q.at.y() - 1 && y < q.at.y() + QuestLayout.SIZE + 1) return q;
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) return true;
        if (event.button() != 0) return false;
        for (Hit hit : hits)
            if (hit.click != null && hit.has(event.x(), event.y())) {
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance
                        .forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                hit.click.run();
                return true;
            }
        if (inGraph(event.x(), event.y())) {
            pressedInGraph = true;
            dragging = false;
            pressX = event.x();
            pressY = event.y();
            glideX = Double.NaN;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (!pressedInGraph) return super.mouseDragged(event, dx, dy);
        if (Math.abs(event.x() - pressX) + Math.abs(event.y() - pressY) > 3) dragging = true;
        view.panX += dx;
        view.panY += dy;
        clampPan();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (pressedInGraph) {
            pressedInGraph = false;
            if (!dragging) {
                QuestBook.Quest q = questAt(event.x(), event.y());
                view.picked = q == null ? null : q.id();
            }
            dragging = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (inPane(x, y)) {
            paneScroll -= scrollY * 18;
            return true;
        }
        if (inGraph(x, y) && scrollY != 0) {
            zoom(scrollY > 0 ? 1 : -1, x - graphX(), y - graphY());
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minecraft.options.keyAdvancements.matches(event)) {
            if (parent == null) minecraft.mouseHandler.grabMouse();
            onClose();
            return true;
        }
        if (chapter != null) {
            int key = event.key();
            if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_UP || key == InputConstants.KEY_DOWN) {
                view.panX += key == InputConstants.KEY_LEFT ? 40 : key == InputConstants.KEY_RIGHT ? -40 : 0;
                view.panY += key == InputConstants.KEY_UP ? 40 : key == InputConstants.KEY_DOWN ? -40 : 0;
                glideX = Double.NaN;
                clampPan();
                return true;
            }
            if (key == InputConstants.KEY_EQUALS || key == InputConstants.KEY_ADD) {
                zoom(1, clearW() / 2.0, graphH() / 2.0);
                return true;
            }
            if (key == InputConstants.KEY_MINUS) {
                zoom(-1, clearW() / 2.0, graphH() / 2.0);
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        HideUi.panel(g, MARGIN, MARGIN, width - 2 * MARGIN, height - 2 * MARGIN);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (stale) rebuild();
        hits.clear();
        long now = Util.getMillis();
        float dt = Math.min(100, now - lastFrame);
        lastFrame = now;
        if (chapter == null) {
            super.extractRenderState(g, mouseX, mouseY, a);
            return;
        }
        if (view == null) open(chapter);
        if (!Double.isNaN(glideX)) {
            double k = 1 - Math.exp(-dt / 70.0);
            view.panX += (glideX - view.panX) * k;
            view.panY += (glideY - view.panY) * k;
            if (Math.abs(glideX - view.panX) + Math.abs(glideY - view.panY) < 0.5) {
                view.panX = glideX;
                view.panY = glideY;
                glideX = Double.NaN;
            }
        }
        sidebar(g);
        header(g);
        graph(g, mouseX, mouseY, now);

        // the graph's own buttons, in its bottom right corner (hidden where the pane lies over them)
        int bx = graphX() + clearW() - 52, by = graphY() + graphH() - 19;
        zoomOut.setPosition(bx, by);
        zoomIn.setPosition(bx + 17, by);
        fitButton.setPosition(bx + 34, by);
        zoomOut.active = view.zoom > 0;
        zoomIn.active = view.zoom < ZOOMS.length - 1;
        nextButton.active = chapter.ready > 0 || book.chapters.stream().anyMatch(c -> c.ready > 0);
        super.extractRenderState(g, mouseX, mouseY, a);

        if (paneOpen()) {
            g.nextStratum();
            pane(g, mouseX, mouseY);
        }

        for (Hit hit : hits)
            if (hit.has(mouseX, mouseY)) {
                if (hit.item != null) g.setTooltipForNextFrame(font, hit.item, mouseX, mouseY);
                else if (hit.tip != null) g.setTooltipForNextFrame(font, wrapped(hit.tip, 180), mouseX, mouseY);
                break;
            }
    }

    private List<FormattedCharSequence> wrapped(List<Component> lines, int w) {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (Component line : lines) out.addAll(font.split(line, w));
        return out;
    }

    /** One line of {@code text} no wider than {@code w}, cut with an ellipsis. */
    private FormattedCharSequence cut(FormattedText text, int w) {
        if (font.width(text) <= w) return net.minecraft.locale.Language.getInstance().getVisualOrder(text);
        return net.minecraft.locale.Language.getInstance().getVisualOrder(
                FormattedText.composite(font.substrByWidth(text, w - font.width("...")), FormattedText.of("...")));
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, int value, int max, int colour) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE_DARK);
        g.fill(x, y, x + w, y + h, DARK);
        int fill = max == 0 ? 0 : (int) Math.ceil(w * (double) value / max);
        g.fill(x, y, x + fill, y + h, colour);
        if (fill > 0 && h > 2) g.fill(x, y, x + fill, y + 1, 0x50FFFFFF);
    }

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(Mechanical.MODID, "quest/" + name);
    }

    private static void blit(GuiGraphicsExtractor g, String name, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(name), x, y, w, h);
    }

    private static int colour(QuestBook.Quest q) {
        return switch (q.state) {
            case DONE -> GOOD;
            case READY -> GOLD;
            case LOCKED -> DIM;
        };
    }

    private static Component stateText(QuestBook.Quest q) {
        String state = q.state == QuestBook.State.READY && q.partial() ? "progress" : q.state.name().toLowerCase(java.util.Locale.ROOT);
        return Component.translatable("gui.mechanical.quests.state." + state);
    }

    /** A quest's node, 26x26 at (x, y): its shape in its state's colours, its item, and the tick or padlock. */
    private void node(GuiGraphicsExtractor g, QuestBook.Quest q, int x, int y, boolean badges) {
        blit(g, q.shape() + "_" + q.state.name().toLowerCase(java.util.Locale.ROOT), x, y, 26, 26);
        g.item(q.icon, x + 5, y + 5);
        if (q.state == QuestBook.State.LOCKED) g.fill(x + 5, y + 5, x + 21, y + 21, 0x8C1C150F);
        if (!badges) return;
        if (q.state == QuestBook.State.DONE) blit(g, "check", x + 18, y - 3, 11, 11);
        if (q.state == QuestBook.State.LOCKED) blit(g, "lock", x + 18, y + 17, 9, 11);
        if (q.partial()) bar(g, x + 4, y + 25, 18, 2, q.met, q.total, GOLD);
    }

    private void halo(GuiGraphicsExtractor g, QuestBook.Quest q, int x, int y, int argb) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite(q.shape() + "_halo"), x - 4, y - 4, 34, 34, argb);
    }

    private void sidebar(GuiGraphicsExtractor g) {
        int n = book.chapters.size(), footer = wide ? 26 : 0;
        int step = Math.min(30, (bottom - top - footer) / Math.max(1, n));
        int h = step - 2;
        for (int i = 0; i < n; i++) {
            QuestBook.Chapter c = book.chapters.get(i);
            int y = top + i * step;
            boolean selected = c == chapter, complete = c.done == c.quests.size();
            Hit hit = new Hit(sideX, y, sideW, h, () -> open(c), wide ? null : List.of(c.root.title().copy().withColor(NAME),
                    Component.translatable("gui.mechanical.quests.chapter_count", c.done, c.quests.size()).withColor(DIM),
                    Component.translatable("gui.mechanical.quests.ready_count", c.ready).withColor(GOLD)), null);
            hits.add(hit);
            boolean hover = hit.has(mouseX(), mouseY());
            HideUi.row(g, sideX, y, sideW, h, hover || selected, true);
            if (selected) {
                g.outline(sideX, y, sideW, h, GOLD);
                g.fill(sideX + sideW, y + h / 2 - 3, sideX + sideW + 2, y + h / 2 + 3, GOLD);
                g.fill(sideX + sideW + 2, y + h / 2 - 1, sideX + sideW + 4, y + h / 2 + 1, GOLD);
            }
            int iy = y + (h - 16) / 2 - (wide ? 0 : 1);
            g.item(c.root.icon, sideX + (wide ? 3 : 6), iy);
            if (wide) {
                int tx = sideX + 22, tw = sideW - 26;
                boolean two = h >= 20;
                g.text(font, cut(c.root.title(), two ? tw : tw - 30), tx, two ? y + (h - 17) / 2 : y + (h - 8) / 2, selected ? NAME : TEXT, true);
                if (two) {
                    String count = c.done + "/" + c.quests.size();
                    int cw = font.width(count), by = y + (h - 17) / 2 + 11;
                    bar(g, tx + 1, by + 2, tw - cw - 6, 3, c.done, c.quests.size(), complete ? GOOD : GOLD);
                    g.text(font, count, tx + tw - cw, by, complete ? GOOD : DIM, false);
                }
            } else if (h >= 14) {
                bar(g, sideX + 4, y + h - 3, sideW - 8, 1, c.done, c.quests.size(), complete ? GOOD : GOLD);
            }
            if (complete) blit(g, "check", sideX + sideW - (wide ? 9 : 8), y - 1, 9, 9);
        }
        if (wide) {
            int y = bottom - 22;
            HideUi.seam(g, sideX + 2, y, sideW - 4);
            g.text(font, Component.translatable("gui.mechanical.quests.total"), sideX + 3, y + 5, DIM, false);
            String count = book.done + "/" + book.total;
            g.text(font, count, sideX + sideW - 3 - font.width(count), y + 5, NAME, false);
            bar(g, sideX + 3, y + 16, sideW - 6, 4, book.done, book.total, GOOD);
        }
    }

    private int mouseX() {
        return (int) minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
    }

    private int mouseY() {
        return (int) minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());
    }

    private void header(GuiGraphicsExtractor g) {
        int x = mainX, w = nextButton.getX() - 6 - x;
        g.item(chapter.root.icon, x + 1, top + 3);
        x += 21;
        w -= 21;
        Component name = chapter.root.title().copy().withStyle(ChatFormatting.BOLD);
        g.text(font, cut(name, w), x, top + 2, NAME, true);
        boolean complete = chapter.done == chapter.quests.size();
        Component count = Component.translatable("gui.mechanical.quests.chapter_count", chapter.done, chapter.quests.size());
        Component ready = Component.translatable("gui.mechanical.quests.ready_count", chapter.ready);
        g.text(font, cut(count, w), x, top + 13, complete ? GOOD : TEXT, false);
        int used = font.width(count) + 8;
        if (!complete && used + font.width(ready) <= w) {
            g.text(font, ready, x + used, top + 13, GOLD, false);
            used += font.width(ready) + 8;
        }
        int barW = Math.min(150, w - used - 2);
        if (barW >= 24) bar(g, x + used, top + 14, barW, 5, chapter.done, chapter.quests.size(), GOOD);
    }

    private void graph(GuiGraphicsExtractor g, int mouseX, int mouseY, long now) {
        int gx = graphX(), gy = graphY(), gw = graphW(), gh = graphH();
        HideUi.inset(g, gx - 2, gy - 2, gw + 4, gh + 4);
        g.enableScissor(gx, gy, gx + gw, gy + gh);
        float s = scale();
        // the pan snapped to whole screen pixels, so the pixel art stays still and sharp
        float gui = minecraft.getWindow().getGuiScale();
        float ox = gx + Math.round(view.panX * gui) / gui, oy = gy + Math.round(view.panY * gui) / gui;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) (gx + view.panX % 64 - 64), (float) (gy + view.panY % 64 - 64));
        blit(g, "board", 0, 0, gw + 128, gh + 128);
        pose.popMatrix();
        pose.pushMatrix();
        pose.translate(ox, oy);
        pose.scale(s, s);
        // what is visible, in graph coordinates
        int x0 = (int) ((gx - ox) / s) - 40, y0 = (int) ((gy - oy) / s) - 40;
        int x1 = (int) ((gx + gw - ox) / s) + 40, y1 = (int) ((gy + gh - oy) / s) + 40;

        QuestBook.Quest picked = picked(), hover = dragging ? null : questAt(mouseX, mouseY);
        QuestBook.Quest lit = picked != null ? picked : hover;
        int thick = s < 0.6F ? 3 : 2;
        // dim lines first, so shared trunks show the brightest way
        List<QuestLayout.Line<QuestBook.Quest>> lines = chapter.layout.lines().stream()
                .sorted(Comparator.comparingInt(l -> lineRank(l, lit))).toList();
        for (var line : lines) {
            int rank = lineRank(line, lit);
            int colour = rank == 3 ? LINE_PICKED : rank == 2 ? LINE_READY : rank == 1 ? LINE_DONE : LINE_LOCKED;
            int[] xy = line.xy();
            for (int i = 0; i + 3 < xy.length; i += 2) {
                int lx = Math.min(xy[i], xy[i + 2]), hx = Math.max(xy[i], xy[i + 2]);
                int ly = Math.min(xy[i + 1], xy[i + 3]), hy = Math.max(xy[i + 1], xy[i + 3]);
                if (hx < x0 || lx > x1 || hy < y0 || ly > y1) continue;
                g.fill(lx - thick / 2, ly - thick / 2, hx - thick / 2 + thick, hy - thick / 2 + thick, colour);
            }
        }
        float pulse = (float) (0.5 + 0.5 * Math.sin(now / 320.0));
        for (QuestBook.Quest q : chapter.quests) {
            int x = q.at.x(), y = q.at.y();
            if (x < x0 || x > x1 || y < y0 || y > y1) continue;
            if (q == picked) halo(g, q, x, y, 0xFFFFFFFF);
            else if (q == hover) halo(g, q, x, y, 0xA0FFFFFF);
            else if (q.state == QuestBook.State.READY) halo(g, q, x, y, (int) (50 + 150 * pulse) << 24 | 0xFFD060);
            node(g, q, x, y, true);
        }
        pose.popMatrix();
        legend(g);
        g.disableScissor();

        if (hover != null) {
            List<Component> tip = new ArrayList<>();
            tip.add(hover.title().copy().withColor(NAME));
            tip.add(stateText(hover).copy().withColor(colour(hover)));
            if (hover.total > 1) tip.add(Component.literal(hover.met + " / " + hover.total).withColor(TEXT));
            if (hover.state == QuestBook.State.LOCKED)
                tip.add(Component.translatable("gui.mechanical.quests.first").append(": ").append(hover.parent.title()).withColor(BAD));
            hits.add(new Hit((int) mouseX, (int) mouseY, 1, 1, null, tip, null));
        }
    }

    private static int lineRank(QuestLayout.Line<QuestBook.Quest> line, QuestBook.Quest lit) {
        if (lit != null && (line.child() == lit || line.parent() == lit)) return 3;
        return switch (line.child().state) {
            case READY -> 2;
            case DONE -> 1;
            case LOCKED -> 0;
        };
    }

    /** What the colours mean, in the graph's bottom left corner. */
    private void legend(GuiGraphicsExtractor g) {
        String[] keys = {"done", "ready", "locked"};
        int w = 4;
        for (String key : keys) w += 17 + font.width(Component.translatable("gui.mechanical.quests." + key)) + 4;
        if (w > clearW() - 60) return;
        int x = graphX() + 3, y = graphY() + graphH() - 19;
        HideUi.row(g, x, y, w, 16, false, true);
        x += 4;
        for (String key : keys) {
            var pose = g.pose();
            pose.pushMatrix();
            pose.translate(x, y + 1.5F);
            pose.scale(0.5F, 0.5F);
            blit(g, "task_" + key, 0, 0, 26, 26);
            pose.popMatrix();
            if (key.equals("done")) blit(g, "check", x + 6, y + 1, 9, 9);
            if (key.equals("locked")) blit(g, "lock", x + 7, y + 5, 7, 9);
            Component label = Component.translatable("gui.mechanical.quests." + key);
            g.text(font, label, x + 17, y + 4, key.equals("done") ? GOOD : key.equals("ready") ? GOLD : DIM, false);
            x += 17 + font.width(label) + 4;
        }
    }

    // ------------------------------------------------------------------ the pane

    private void pane(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int x = paneX(), y = bodyY, w = paneW, h = bottom - bodyY;
        QuestBook.Quest q = picked();
        String shows = q == null ? chapter.id() + "#overview" : q.id().toString();
        if (!shows.equals(paneShows)) {
            paneShows = shows;
            paneScroll = 0;
        }
        HideUi.panel(g, x, y, w, h);
        paneScroll = Math.clamp(paneScroll, 0, Math.max(0, paneHeight - (h - 12)));
        int cx = x + 7, cw = w - 15, cy = y + 6 - (int) paneScroll;
        g.enableScissor(x + 5, y + 6, x + w - 5, y + h - 6);
        int before = hits.size();
        int end = q == null ? overview(g, cx, cy, cw, mouseX, mouseY) : details(g, q, cx, cy, cw, mouseX, mouseY);
        g.disableScissor();
        paneHeight = end - cy;
        // rows scrolled out of sight can't be clicked
        for (int i = hits.size() - 1; i >= before; i--) {
            Hit hit = hits.get(i);
            int hy = Math.max(hit.y, y + 6), hb = Math.min(hit.y + hit.h, y + h - 6);
            if (hb <= hy) hits.remove(i);
            else hits.set(i, new Hit(hit.x, hy, hit.w, hb - hy, hit.click, hit.tip, hit.item));
        }
        if (paneHeight > h - 12) { // a scrollbar
            int track = h - 12, thumb = Math.max(10, (int) (track * track / paneHeight));
            int ty = y + 6 + (int) ((track - thumb) * paneScroll / (paneHeight - track));
            g.fill(x + w - 7, y + 6, x + w - 5, y + h - 6, DARK);
            g.fill(x + w - 7, ty, x + w - 5, ty + thumb, HideUi.EDGE);
        }
        if (q != null) { // close: back to the overview (or, on a narrow screen, the graph)
            Hit close = new Hit(x + w - 20, y + 5, 12, 12, () -> view.picked = null,
                    List.of(Component.translatable("gui.mechanical.quests.close")), null);
            hits.add(0, close);
            g.nextStratum();
            HideUi.row(g, close.x, close.y, 12, 12, close.has(mouseX, mouseY), true);
            g.text(font, "✕", close.x + 3, close.y + 2, TEXT, false);
        }
    }

    private int wrap(GuiGraphicsExtractor g, FormattedText text, int x, int y, int w, int colour, boolean shadow) {
        for (FormattedCharSequence line : font.split(text, w)) {
            g.text(font, line, x, y, colour, shadow);
            y += 10;
        }
        return y;
    }

    private int heading(GuiGraphicsExtractor g, Component text, int x, int y, int w) {
        HideUi.seam(g, x, y + 3, w);
        g.text(font, text.copy().withStyle(ChatFormatting.BOLD), x, y + 9, GOLD, true);
        return y + 21;
    }

    /** A quest as a clickable row: its node, small, and its title on up to two lines. */
    private int questRow(GuiGraphicsExtractor g, QuestBook.Quest q, int x, int y, int w, int mouseX, int mouseY) {
        Hit hit = new Hit(x, y, w, 23, () -> go(q), List.of(q.title().copy().withColor(NAME), stateText(q).copy().withColor(colour(q))), null);
        hits.add(hit);
        HideUi.row(g, x, y, w, 23, hit.has(mouseX, mouseY), q.state != QuestBook.State.LOCKED);
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x + 2, y + 1.5F);
        pose.scale(0.77F, 0.77F);
        node(g, q, 0, 0, false);
        pose.popMatrix();
        if (q.state == QuestBook.State.DONE) blit(g, "check", x + 14, y, 9, 9);
        if (q.state == QuestBook.State.LOCKED) blit(g, "lock", x + 16, y + 12, 7, 9);
        int tx = x + 26, tw = w - 29;
        List<FormattedCharSequence> lines = font.split(q.title(), tw);
        int colour = q.state == QuestBook.State.LOCKED ? DIM : TEXT;
        if (q.partial()) {
            g.text(font, cut(q.title(), tw), tx, y + 3, colour, false);
            String count = q.met + "/" + q.total;
            g.text(font, count, tx + tw - font.width(count), y + 13, GOLD, false);
            bar(g, tx + 1, y + 15, tw - font.width(count) - 6, 3, q.met, q.total, GOLD);
        } else if (lines.size() == 1) {
            g.text(font, lines.get(0), tx, y + 8, colour, false);
        } else {
            g.text(font, lines.get(0), tx, y + 3, colour, false);
            g.text(font, lines.size() == 2 ? lines.get(1) : cut(Component.literal(font.getSplitter()
                    .splitLines(q.title(), tw, net.minecraft.network.chat.Style.EMPTY).get(1).getString() + "..."), tw), tx, y + 13, colour, false);
        }
        return y + 25;
    }

    /** With no quest picked: what can be done in this chapter now. */
    private int overview(GuiGraphicsExtractor g, int x, int y, int w, int mouseX, int mouseY) {
        List<QuestBook.Quest> ready = chapter.readyNow();
        g.text(font, Component.translatable("gui.mechanical.quests.ready_now").withStyle(ChatFormatting.BOLD), x, y + 2, GOLD, true);
        y += 15;
        for (QuestBook.Quest q : ready) y = questRow(g, q, x, y, w, mouseX, mouseY);
        if (ready.isEmpty())
            y = wrap(g, Component.translatable(chapter.done == chapter.quests.size() ? "gui.mechanical.quests.chapter_done"
                    : "gui.mechanical.quests.nothing_ready"), x, y, w, GOOD, false);
        HideUi.seam(g, x, y + 3, w);
        return wrap(g, Component.translatable("gui.mechanical.quests.hint"), x, y + 10, w, DIM, false) + 2;
    }

    private int details(GuiGraphicsExtractor g, QuestBook.Quest q, int x, int y, int w, int mouseX, int mouseY) {
        // the node twice its size, beside it the state and kind
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x + 1, y + 2);
        pose.scale(2F, 2F);
        node(g, q, 0, 0, false);
        pose.popMatrix();
        if (q.state == QuestBook.State.DONE) blit(g, "check", x + 40, y, 15, 15);
        if (q.state == QuestBook.State.LOCKED) blit(g, "lock", x + 42, y + 36, 12, 15);
        int tx = x + 60, tw = w - 60 - 12;
        int ty = wrap(g, stateText(q).copy().withStyle(ChatFormatting.BOLD), tx, y + 14, w - 60, colour(q), true);
        g.text(font, cut(Component.translatable("gui.mechanical.quests.frame." + q.shape()), tw + 12), tx, ty + 2, DIM, false);
        y += 59;
        y = wrap(g, q.title().copy().withStyle(ChatFormatting.BOLD), x, y, w, NAME, true);

        if (q.state == QuestBook.State.LOCKED) {
            y = heading(g, Component.translatable("gui.mechanical.quests.first"), x, y, w);
            y = questRow(g, q.parent, x, y, w, mouseX, mouseY) - 2;
        }
        y = heading(g, Component.translatable("gui.mechanical.quests.what"), x, y, w);
        y = wrap(g, q.display.description(), x, y, w, TEXT, false) + 1;

        List<QuestBook.Check> checks = q.checklist();
        if (!checks.isEmpty()) {
            boolean all = q.total > 1;
            y = heading(g, Component.translatable(all ? "gui.mechanical.quests.progress" : "gui.mechanical.quests.any_of"), x, y, w);
            if (all) {
                String count = q.met + " / " + q.total;
                int colour = q.met == q.total ? GOOD : GOLD;
                g.text(font, count, x + w - font.width(count), y - 12, colour, true);
                bar(g, x + 1, y, w - 2, 5, q.met, q.total, colour);
                y += 10;
            }
            // what is left first
            if (all && q.state != QuestBook.State.DONE) checks = checks.stream().sorted(Comparator.comparing(QuestBook.Check::done)).toList();
            for (QuestBook.Check check : checks) y = checkRow(g, check, x, y, w);
        }

        if (q.state != QuestBook.State.LOCKED && q.parent != null) {
            y = heading(g, Component.translatable("gui.mechanical.quests.follows"), x, y, w);
            y = questRow(g, q.parent, x, y, w, mouseX, mouseY) - 2;
        }
        if (!q.next.isEmpty()) {
            y = heading(g, Component.translatable("gui.mechanical.quests.leads_to").append(" (" + q.next.size() + ")"), x, y, w);
            for (QuestBook.Quest next : q.next) y = questRow(g, next, x, y, w, mouseX, mouseY);
        }
        return y + 2;
    }

    private int checkRow(GuiGraphicsExtractor g, QuestBook.Check check, int x, int y, int w) {
        List<FormattedCharSequence> lines = font.split(check.name(), w - 35);
        int h = Math.max(19, lines.size() * 9 + 3);
        ItemStack icon = check.icons().isEmpty() ? null : check.icons().get((int) (Util.getMillis() / 1000 % check.icons().size()));
        HideUi.slot(g, x, y);
        if (icon != null) {
            g.item(icon, x + 1, y + 1);
            hits.add(new Hit(x, y, 18, 18, null, null, icon));
        }
        int ty = y + (h - lines.size() * 9) / 2;
        for (FormattedCharSequence line : lines) {
            g.text(font, line, x + 21, ty, check.done() ? GOOD : TEXT, false);
            ty += 9;
        }
        if (check.done()) {
            blit(g, "check", x + w - 11, y + (h - 11) / 2, 11, 11);
        } else {
            g.fill(x + w - 10, y + (h - 9) / 2, x + w - 1, y + (h - 9) / 2 + 9, EDGE_DARK);
            g.fill(x + w - 9, y + (h - 9) / 2 + 1, x + w - 2, y + (h - 9) / 2 + 8, DARK);
        }
        return y + h + 1;
    }

    // ------------------------------------------------------------------ opening

    /** However the advancements screen is opened (the key, the pause menu), this one comes up instead. */
    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class Opener {
        @SubscribeEvent
        static void onOpen(ScreenEvent.Opening event) {
            if (event.getNewScreen() instanceof AdvancementsScreen) event.setNewScreen(new QuestScreen(event.getCurrentScreen()));
        }
    }
}
