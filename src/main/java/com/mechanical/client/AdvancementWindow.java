package com.mechanical.client;

/**
 * Size of the advancements window: vanilla's fixed 252x140 is stretched to fill the screen (leaving room for the tabs above
 * and the Done button below), so the age trees can be seen whole. Used by AdvancementsScreenMixin and AdvancementTabMixin.
 */
public final class AdvancementWindow {
    public static final int VANILLA_W = 252, VANILLA_H = 140, BORDER_X = 18, BORDER_Y = 27;

    public static int width(int screenWidth) {
        return Math.max(VANILLA_W, screenWidth - 24);
    }

    public static int height(int screenHeight) {
        return Math.max(VANILLA_H, screenHeight - 72);
    }

    public static int insideWidth(int screenWidth) {
        return width(screenWidth) - BORDER_X;
    }

    public static int insideHeight(int screenHeight) {
        return height(screenHeight) - BORDER_Y;
    }
}
