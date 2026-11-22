package com.mechanical.client.clan;

import com.mechanical.Mechanical;
import com.mechanical.clan.Lands;
import com.mechanical.people.Tribe;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

/**
 * "You are now entering X's land": a bark panel in the tribe page's style ({@link ClanScreen}) a quarter down the screen,
 * the tribe's pennant and name in its colour, and under it your standing ("Friendly 45"), or "Home" on your own.
 * Shown once per {@link Lands.Entered} for {@link #SHOW_MS}, fading in and out.
 */
public final class LandTitle {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Mechanical.MODID, "land_title");
    static final int PANEL = 0xE0302418, BORDER = 0xFF8A6A44, TITLE = 0xFFF0D8A8, TEXT = 0xFFE8E0D0, PAD = 8;
    static final long SHOW_MS = 5000, FADE_MS = 600;
    /** Standing colours from hostile to honoured (also the Relations tab's bar). */
    static final int[] STANDING = {0xFFE05A4A, 0xFFE0A050, 0xFFD8D0B8, 0xFFA8E07A, 0xFF7AD8E0};

    private static Lands.Entered shown;
    private static long since;

    static void show(Lands.Entered land) {
        shown = land;
        since = Util.getMillis();
    }

    /** 0-4: hostile, unfriendly, neutral, friendly, honoured (the thresholds of {@link Tribe#standing}). */
    static int tier(int rep) {
        return rep <= Tribe.HOSTILE ? 0 : rep < 0 ? 1 : rep < Tribe.FRIENDLY ? 2 : rep < 70 ? 3 : 4;
    }

    /** The land's name: a player tribe's own, an NPC tribe's from its id. */
    static Component name(Lands.Entered land) {
        if (land.kind() != Lands.TRIBE) return Component.literal(land.name());
        try {
            return Tribe.of(java.util.UUID.fromString(land.name())).name();
        } catch (IllegalArgumentException e) {
            return Component.literal("?");
        }
    }

    static Component standing(Lands.Entered land) {
        if (land.kind() == Lands.HOME) return Component.translatable("gui.mechanical.land.home");
        if (land.kind() == Lands.CLAN) return null;
        return Component.translatable("gui.mechanical.land.standing", Tribe.standing(land.rep()), land.rep());
    }

    /** The name in the tribe's colour, lifted toward white so dark dyes (black, brown, blue) read on the bark. */
    static int lighten(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return (r + (255 - r) * 2 / 5) << 16 | (g + (255 - g) * 2 / 5) << 8 | (b + (255 - b) * 2 / 5);
    }

    private static int alpha(int argb, float a) {
        return ((int) ((argb >>> 24) * a) << 24) | (argb & 0xFFFFFF);
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (shown == null || mc.gui.hud.isHidden()) return;
        long t = Util.getMillis() - since;
        if (t > SHOW_MS) {
            shown = null;
            return;
        }
        float a = Math.min(1, Math.min(t, SHOW_MS - t) / (float) FADE_MS);
        if (a < 0.05F) return;
        var font = mc.font;
        int colour = 0xFF000000 | TribeTabList.dye(shown.colour());
        Component title = Component.translatable("gui.mechanical.land.entering", name(shown).copy().withColor(lighten(colour)));
        Component sub = standing(shown);
        int w = Math.max(font.width(title) + 14, sub == null ? 0 : font.width(sub)) + 2 * PAD;
        int h = PAD + 10 + (sub == null ? 0 : 12) + PAD - 2;
        int x = (g.guiWidth() - w) / 2, y = g.guiHeight() / 4;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, alpha(BORDER, a));
        g.fill(x, y, x + w, y + h, alpha(PANEL, a));
        g.fill(x, y + h - 2, x + w, y + h, alpha(colour, a)); // a stripe of the tribe's colour along the bottom
        TribeTabList.pennant(g, x + PAD, y + PAD - 2, alpha(colour, a));
        g.text(font, title, x + PAD + 14, y + PAD, alpha(TITLE, a));
        if (sub != null) {
            int subColour = shown.kind() == Lands.HOME ? TEXT : STANDING[tier(shown.rep())];
            g.centeredText(font, sub, x + w / 2, y + PAD + 12, alpha(subColour, a));
        }
    }

    private LandTitle() {}
}
