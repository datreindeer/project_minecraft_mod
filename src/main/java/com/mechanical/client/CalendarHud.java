package com.mechanical.client;

import com.mechanical.Calendar;
import com.mechanical.Mechanical;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.gui.GuiLayer;

/** Top left: season, date, year and time of day, in the overworld (the only world with a sky clock). */
public class CalendarHud implements GuiLayer {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Mechanical.MODID, "calendar");
    public static final int[] SEASON_COLOURS = {0xFF9BE07A, 0xFFF2D35B, 0xFFE0894A, 0xFFBFE3F5};

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getDebugOverlay().showDebugScreen() || !mc.level.dimensionType().hasSkyLight()) return;
        long clock = mc.level.getOverworldClockTime();
        graphics.text(mc.font, Calendar.describe(clock), 4, 4, SEASON_COLOURS[Calendar.season(clock)], true);
    }
}
