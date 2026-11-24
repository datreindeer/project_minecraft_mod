package com.mechanical.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mechanical.HighFurnaceBlockEntity;
import com.mechanical.HighFurnaceMelting;
import com.mechanical.HighFurnaceMenu;
import com.mechanical.MeltPool;
import com.mechanical.Molten;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The High Furnace's screen: a thermometer up to {@link #MAX}° (notches at bronze 950°, iron ore 1450°, steel 1500°,
 * charcoal's ceiling 1600°, the target's white notch), the pool as a column of layers in their own colours by density
 * (click one to have the tapholes pour it, click it again for the bottom layer; a frame marks the choice), the blast,
 * each Firebox's fuel and blast, and the melting places: each item with its own bar, red while it waits for more heat.
 * Drawn without a texture: a vanilla-like panel with dark insets.
 */
public class HighFurnaceScreen extends AbstractContainerScreen<HighFurnaceMenu> {
    public static final int MAX = 1800, W = 300, H = 134;
    private static final int TUBE_X = 14, TUBE_W = 7, TUBE_TOP = 20, TUBE_BOTTOM = 112;
    private static final int COL_X = 30, COL_W = 18, COL_TOP = 20, COL_BOTTOM = 119, TEXT_X = 56, GRID_X = 206, GRID_Y = 20, CELL = 22, GRID_COLS = 4, GRID_ROWS = 5;
    private static final int[] MARKS = {950, 1450, 1500, 1600};
    private static final Identifier FLAME = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final int DARK = 0xFF404040, GREY = 0xFF6A6A6A, GREEN = 0xFF2E7D32, RED = 0xFFB71C1C, GOLD = 0xFF8A5A00;
    private final ItemStack charcoal = new ItemStack(Items.CHARCOAL);

    public HighFurnaceScreen(HighFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    private static int row(int degrees) {
        return TUBE_BOTTOM - Math.round(Math.clamp(degrees / (float) MAX, 0, 1) * (TUBE_BOTTOM - TUBE_TOP));
    }

    /** The pool's layers as bands from the bottom of the column: each at least 3 px so a thin one can still be clicked. */
    private record Band(Molten molten, int mb, int y0, int y1) {}

    private List<Band> bands() {
        List<Band> out = new ArrayList<>();
        HighFurnaceBlockEntity f = menu.furnace();
        if (f == null) return out;
        MeltPool pool = f.pool();
        int cap = Math.max(1, pool.capacity()), y = COL_BOTTOM, h = COL_BOTTOM - COL_TOP;
        for (Map.Entry<Molten, Integer> e : pool.layers()) {
            int px = Math.max(3, Math.round(e.getValue() * h / (float) cap));
            out.add(new Band(e.getKey(), e.getValue(), Math.max(COL_TOP, y - px), y));
            y -= px;
        }
        return out;
    }

    private static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, 0xFF000000);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + w - 2, y + 3, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 3, y + h - 2, 0xFFFFFFFF);
        g.fill(x + 3, y + h - 3, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 3, y + 3, x + w - 1, y + h - 1, 0xFF555555);
    }

    private static void inset(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF373737);
        g.fill(x0, y0, x1 + 1, y1 + 1, 0xFFFFFFFF);
        g.fill(x0, y0, x1, y1, 0xFF1E1A18);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        panel(g, x, y, W, H);
        // thermometer
        inset(g, x + TUBE_X, y + TUBE_TOP, x + TUBE_X + TUBE_W, y + TUBE_BOTTOM + 7);
        int t = menu.temperature();
        for (int r = row(t); r < TUBE_BOTTOM; r++) {
            int deg = (TUBE_BOTTOM - r) * MAX / (TUBE_BOTTOM - TUBE_TOP);
            g.fill(x + TUBE_X, y + r, x + TUBE_X + TUBE_W, y + r + 1, KilnScreen.heatColour(deg));
        }
        int bulb = KilnScreen.heatColour(Math.max(t, 0));
        g.fill(x + TUBE_X, y + TUBE_BOTTOM, x + TUBE_X + TUBE_W, y + TUBE_BOTTOM + 7, bulb);
        for (int m : MARKS) {
            int r = y + row(m);
            g.fill(x + TUBE_X + TUBE_W + 1, r, x + TUBE_X + TUBE_W + 4, r + 1, t >= m ? 0xFF2E7D32 : 0xFF555555);
        }
        int tr = y + row(menu.target());
        g.fill(x + TUBE_X - 4, tr, x + TUBE_X, tr + 1, 0xFFFFFFFF);
        // the pool's column: layers in their colours, a lighter top line each, the chosen one framed
        inset(g, x + COL_X, y + COL_TOP, x + COL_X + COL_W, y + COL_BOTTOM);
        HighFurnaceBlockEntity f = menu.furnace();
        Molten chosen = f == null ? null : f.selected();
        for (Band b : bands()) {
            g.fill(x + COL_X, y + b.y0, x + COL_X + COL_W, y + b.y1, b.molten.color);
            g.fill(x + COL_X, y + b.y0, x + COL_X + COL_W, y + b.y0 + 1, 0x60FFFFFF);
            if (b.molten == chosen) {
                g.fill(x + COL_X - 2, y + b.y0 - 1, x + COL_X + COL_W + 2, y + b.y0, 0xFFFFFFFF);
                g.fill(x + COL_X - 2, y + b.y1, x + COL_X + COL_W + 2, y + b.y1 + 1, 0xFFFFFFFF);
            }
        }
        // fireboxes
        int fy = y + 60;
        for (int i = 0; i < menu.fireboxes(); i++, fy += 13) {
            if (menu.burning(i)) g.blitSprite(RenderPipelines.GUI_TEXTURED, FLAME, 14, 14, 0, 0, x + TEXT_X, fy - 3, 14, 14);
            else g.item(charcoal, x + TEXT_X - 1, fy - 4);
        }
        // melting places: the items in them with a bar each (red: waiting for heat or room)
        inset(g, x + GRID_X, y + GRID_Y, x + GRID_X + GRID_COLS * CELL, y + GRID_Y + GRID_ROWS * CELL);
        if (f != null) {
            int n = 0;
            for (int i = 0; i < f.slots().size() && n < GRID_COLS * GRID_ROWS; i++) {
                ItemStack it = f.slots().get(i);
                if (it.isEmpty()) continue;
                int cx = x + GRID_X + (n % GRID_COLS) * CELL + 3, cy = y + GRID_Y + (n / GRID_COLS) * CELL + 2;
                g.item(it, cx, cy);
                HighFurnaceMelting.Melt m = HighFurnaceMelting.of(it);
                boolean waits = m == null || t < m.heat();
                g.fill(cx, cy + 17, cx + 16, cy + 19, 0xFF3A3A3A);
                g.fill(cx, cy + 17, cx + Math.max(1, Math.round(16 * f.progress(i))), cy + 19, waits ? 0xFFC03020 : 0xFF40C040);
                n++;
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.text(font, title, 8, 6, DARK, false);
        int t = menu.temperature(), target = menu.target();
        g.text(font, Component.translatable("gui.mechanical.high_furnace.heat", t, target), TEXT_X, 20, t >= 1450 ? GOLD : DARK, false);
        g.text(font, Component.translatable("gui.mechanical.high_furnace.blast", String.format(Locale.ROOT, "%.1f", menu.effectiveBlast()),
                HighFurnaceBlockEntity.FULL_BLAST), TEXT_X, 31, DARK, false);
        Component state = menu.dryingSeconds() > 0
                ? Component.translatable("gui.mechanical.high_furnace.drying", menu.dryingSeconds() / 60, String.format(Locale.ROOT, "%02d", menu.dryingSeconds() % 60))
                : Component.translatable(menu.lit() ? "gui.mechanical.high_furnace.lit" : "gui.mechanical.high_furnace.out");
        g.text(font, state, TEXT_X, 42, menu.dryingSeconds() > 0 ? GOLD : menu.lit() ? GREEN : GREY, false);
        int fy = 60;
        for (int i = 0; i < menu.fireboxes(); i++, fy += 13)
            g.text(font, Component.translatable("gui.mechanical.high_furnace.firebox", i + 1, menu.fuel(i), menu.blast(i)),
                    TEXT_X + 17, fy, menu.fuel(i) == 0 ? RED : DARK, false);
        g.text(font, Component.translatable("gui.mechanical.high_furnace.pool", menu.pool(), menu.capacity()), TEXT_X, H - 14, GREY, false);
        HighFurnaceBlockEntity f = menu.furnace();
        Molten pouring = f == null ? null : f.pouring();
        g.text(font, pouring == null ? Component.translatable("gui.mechanical.high_furnace.pours_nothing")
                : Component.translatable("gui.mechanical.high_furnace.pours", Component.translatable("molten.mechanical." + pouring.id)), GRID_X, 8, DARK, false);
    }

    private @Nullable Band bandAt(double mx, double my) {
        int rx = (int) mx - leftPos, ry = (int) my - topPos;
        if (rx < COL_X - 2 || rx >= COL_X + COL_W + 2) return null;
        for (Band b : bands()) if (ry >= b.y0 && ry < b.y1) return b;
        return null;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Band b = bandAt(event.x(), event.y());
        HighFurnaceBlockEntity f = menu.furnace();
        if (b != null && minecraft != null && minecraft.gameMode != null) {
            int id = f != null && f.selected() == b.molten ? HighFurnaceMenu.BOTTOM : b.molten.ordinal();
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
            if (f != null) f.select(id == HighFurnaceMenu.BOTTOM ? null : b.molten); // show it at once; the server's copy follows
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        List<Component> lines = new ArrayList<>();
        if (rx >= TUBE_X - 5 && rx < TUBE_X + TUBE_W + 5 && ry >= TUBE_TOP - 2 && ry < TUBE_BOTTOM + 8) {
            lines.add(Component.translatable("gui.mechanical.high_furnace.heat", menu.temperature(), menu.target()));
            for (int m : MARKS)
                lines.add(Component.translatable("gui.mechanical.high_furnace.mark." + m, m).withStyle(menu.temperature() >= m ? ChatFormatting.GREEN : ChatFormatting.GRAY));
        }
        Band b = bandAt(mouseX, mouseY);
        if (b != null) {
            lines.add(Component.translatable("molten.mechanical." + b.molten.id).withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable("gui.mechanical.high_furnace.layer", b.mb, String.format(Locale.ROOT, "%.1f", b.mb / (float) Molten.INGOT), b.molten.melt));
            lines.add(Component.translatable("gui.mechanical.high_furnace.layer_click").withStyle(ChatFormatting.GRAY));
        }
        HighFurnaceBlockEntity f = menu.furnace();
        int gx = rx - GRID_X, gy = ry - GRID_Y;
        if (f != null && gx >= 0 && gy >= 0 && gx < GRID_COLS * CELL && gy < GRID_ROWS * CELL) {
            int want = gy / CELL * GRID_COLS + gx / CELL, n = 0;
            for (int i = 0; i < f.slots().size(); i++) {
                ItemStack it = f.slots().get(i);
                if (it.isEmpty()) continue;
                if (n++ != want) continue;
                HighFurnaceMelting.Melt m = HighFurnaceMelting.of(it);
                lines.add(it.getHoverName());
                if (m != null) {
                    lines.add(Component.translatable("gui.mechanical.high_furnace.slot", m.mb(), Component.translatable("molten.mechanical." + m.molten().id)));
                    if (menu.temperature() < m.heat())
                        lines.add(Component.translatable("gui.mechanical.high_furnace.needs", m.heat()).withStyle(ChatFormatting.RED));
                }
                break;
            }
        }
        if (!lines.isEmpty()) {
            List<FormattedCharSequence> seq = lines.stream().map(Component::getVisualOrderText).toList();
            g.setTooltipForNextFrame(font, seq, mouseX, mouseY);
        }
    }
}
