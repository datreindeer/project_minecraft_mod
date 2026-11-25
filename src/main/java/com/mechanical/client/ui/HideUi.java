package com.mechanical.client.ui;

import com.mechanical.Mechanical;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * The hide-and-bark look for screens drawn in code, the same leather as the container textures: a stitched panel, a seam,
 * rows, wells and item pockets, and the text colours that read on them. The sprites (gui/sprites/hide/) and vanilla's
 * re-skinned slot come from scripts/gen_gui_textures.py, which has the same palette.
 */
public final class HideUi {
    /** Text: {@code NAME} for titles and headings, {@code LABEL} where vanilla writes dark grey on its containers. */
    public static final int NAME = 0xFFF0D8A8, LABEL = 0xFFE6CE9E, TEXT = 0xFFE8E0D0, DIM = 0xFFB39F80, GOLD = 0xFFE8C060,
            GOOD = 0xFFA6D67E, BAD = 0xFFEC8C68;
    /** Lines and bars: the tan leather edge, its darker side, and the near-black behind a bar. */
    public static final int EDGE = 0xFF8A6A44, EDGE_DARK = 0xFF6A4F31, DARK = 0xFF160F09, STITCH = 0xFF8F714A;
    /** Vanilla's container label colour, which is unreadable on dark hide (mixin GuiTextColorMixin swaps it). */
    public static final int VANILLA_LABEL = 0xFF404040;

    private static final Identifier PANEL = sprite("panel"), ROW = sprite("row"), ROW_HOVER = sprite("row_hover"), ROW_OFF = sprite("row_off"),
            INSET = sprite("inset");
    private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(Mechanical.MODID, "hide/" + name);
    }

    /** The stitched hide panel; keep content at least 6 px inside it. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, x, y, w, h);
    }

    /** The panel faded to {@code alpha} (0-1), for overlays. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, float alpha) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, PANEL, x, y, w, h, alpha);
    }

    /** A running stitch across: divides a panel's sections. */
    public static void seam(GuiGraphicsExtractor g, int x, int y, int w) {
        for (int k = 0; k < w; k += 6) {
            g.fill(x + k, y, x + Math.min(w, k + 3), y + 1, STITCH);
            g.fill(x + k, y + 1, x + Math.min(w, k + 3), y + 2, DARK);
        }
    }

    /** A list row: lit under the mouse, dull when it can't be used. */
    public static void row(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hover, boolean enabled) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, !enabled ? ROW_OFF : hover ? ROW_HOVER : ROW, x, y, w, h);
    }

    /** A sunken dark well (lists, bars, pictures). */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, INSET, x, y, w, h);
    }

    /** An 18x18 item pocket; draw the item at x + 1, y + 1. */
    public static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, x, y, 18, 18);
    }

    private HideUi() {}
}
