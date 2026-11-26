package com.mechanical.client.viewer;

import static com.mechanical.client.ui.HideUi.DIM;
import static com.mechanical.client.ui.HideUi.LABEL;
import static com.mechanical.client.ui.HideUi.TEXT;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mechanical.client.ui.HideUi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The item list to the right of a container screen: a search box, a page of the items this player knows (seen, or made by
 * an unlocked recipe) filtered by name, and page buttons. Left click shows an item's recipes, right click its uses.
 */
final class SearchPanel {
    private static final int GAP = 4, MAX_COLS = 9, MIN_COLS = 4;
    /** Room left for the compact effect icons beside the player inventory. */
    private static final int EFFECTS = 34;
    /** From the panel's top to the grid, and from the grid to the panel's bottom. */
    private static final int HEAD = 32, FOOT = 32;
    /** The search, kept between screens for the session. */
    private static String text = "";
    private static int page;

    final AbstractContainerScreen<?> screen;
    final EditBox box;
    private final Button prev, next;
    private final Font font = Minecraft.getInstance().font;
    private int x, y, w, h, cols, rows;
    private boolean shown, swallowRelease;
    private @Nullable Item hovered;
    private List<Item> source = List.of(), found = List.of();
    private String query = "";

    SearchPanel(AbstractContainerScreen<?> screen, Consumer<GuiEventListener> add) {
        this.screen = screen;
        Component search = Component.translatable("viewer.mechanical.search");
        box = new EditBox(font, 0, 0, 0, 16, search);
        box.setHint(search.copy().withColor(DIM));
        box.setMaxLength(50);
        box.setTextColor(TEXT);
        box.setValue(text);
        box.setResponder(s -> {
            text = s;
            page = 0;
        });
        prev = Button.builder(Component.literal("◀"), b -> turn(-1)).size(16, 16).build();
        next = Button.builder(Component.literal("▶"), b -> turn(1)).size(16, 16).build();
        add.accept(box);
        add.accept(prev);
        add.accept(next);
        layout();
    }

    /** Every frame: the recipe book can move the screen, and effects can come and go. */
    private void layout() {
        boolean effects = screen instanceof InventoryScreen && !Minecraft.getInstance().player.getActiveEffects().isEmpty();
        x = screen.getLeftPos() + screen.getImageWidth() + GAP + (effects ? EFFECTS : 0);
        cols = Math.min(MAX_COLS, (screen.width - GAP - x - 14) / 18);
        rows = (screen.height - 2 * GAP - HEAD - FOOT) / 18;
        shown = cols >= MIN_COLS && rows >= 1;
        w = cols * 18 + 14;
        h = HEAD + rows * 18 + FOOT;
        y = (screen.height - h) / 2;
        box.setX(x + 7);
        box.setY(y + 7);
        box.setWidth(w - 14);
        box.visible = shown;
        if (!shown && box.isFocused()) screen.setFocused(null);
        int by = y + h - 7 - 16;
        prev.setPosition(x + 7, by);
        next.setPosition(x + w - 7 - 16, by);
        prev.visible = next.visible = shown && pages() > 1;
    }

    private List<Item> found() {
        List<Item> all = ClientCatalog.searchable();
        String q = text.trim().toLowerCase(Locale.ROOT);
        if (all != source || !q.equals(query)) {
            source = all;
            query = q;
            found = q.isEmpty() ? all : all.stream().filter(i -> i.getDefaultInstance().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)).toList();
        }
        return found;
    }

    private int pages() {
        int per = Math.max(1, cols * rows);
        return Math.max(1, (found().size() + per - 1) / per);
    }

    private void turn(int by) {
        page = Math.floorMod(page + by, pages());
    }

    private boolean contains(double mx, double my) {
        return shown && mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** The item in the grid under the mouse. */
    private @Nullable Item itemAt(double mx, double my) {
        int gx = x + 7, gy = y + HEAD;
        if (!shown || mx < gx || my < gy) return null;
        int col = (int) (mx - gx) / 18, row = (int) (my - gy) / 18;
        if (col >= cols || row >= rows) return null;
        int i = page * cols * rows + row * cols + col;
        return i < found().size() ? found().get(i) : null;
    }

    boolean shown() {
        return shown;
    }

    /** The grid item under the mouse in the last frame. */
    @Nullable
    Item hovered() {
        return hovered;
    }

    void draw(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        layout();
        hovered = null;
        if (!shown) return;
        page = Math.min(page, pages() - 1);
        HideUi.panel(g, x, y, w, h);
        int gx = x + 7, gy = y + HEAD;
        HideUi.seam(g, x + 7, y + 27, w - 14);
        HideUi.inset(g, gx - 1, gy - 1, cols * 18 + 2, rows * 18 + 2);
        hovered = itemAt(mouseX, mouseY);
        List<Item> list = found();
        for (int i = 0, first = page * cols * rows; i < cols * rows && first + i < list.size(); i++) {
            int cx = gx + i % cols * 18, cy = gy + i / cols * 18;
            Item item = list.get(first + i);
            if (item == hovered) g.fill(cx, cy, cx + 18, cy + 18, 0x40F0D8A8);
            g.item(item.getDefaultInstance(), cx + 1, cy + 1);
        }
        int sy = gy + rows * 18 + 4;
        HideUi.seam(g, x + 7, sy, w - 14);
        g.centeredText(font, Component.translatable("viewer.mechanical.page", page + 1, pages()), x + w / 2, y + h - 7 - 12, pages() > 1 ? LABEL : DIM);
        if (hovered != null) g.setTooltipForNextFrame(font, hovered.getDefaultInstance(), mouseX, mouseY);
    }

    /** A press on the panel: true when it is taken (a grid item opens the viewer); the box and buttons get theirs from the screen. */
    boolean click(double mx, double my, int button) {
        if (box.isFocused() && !box.isMouseOver(mx, my)) screen.setFocused(null); // a click anywhere else lets go of the box
        if (!contains(mx, my) || box.isMouseOver(mx, my) || prev.visible && prev.isMouseOver(mx, my) || next.visible && next.isMouseOver(mx, my))
            return false;
        swallowRelease = true; // the screen never saw the press: its release would throw a carried stack "outside"
        Item item = itemAt(mx, my);
        if (item != null && (button == 0 || button == 1)) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            RecipeViewerClient.open(screen, item, button == 1);
        }
        return true;
    }

    boolean release() {
        boolean swallow = swallowRelease;
        swallowRelease = false;
        return swallow;
    }

    boolean scroll(double mx, double my, double dy) {
        if (!contains(mx, my) || dy == 0) return false;
        turn(dy > 0 ? -1 : 1);
        return true;
    }
}
