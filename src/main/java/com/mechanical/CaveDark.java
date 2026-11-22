package com.mechanical;

/**
 * Pitch-black caves (living-world plan F3), pure rule: client.CaveDarkness feeds it each tick, DarkNightsMixin applies it to
 * the lightmap, CalendarTest tests it. You are underground when the sky light at your eyes is (almost) gone and there is solid
 * ground well above you (the world surface at your column, so a roofed hut does not count). Leaving needs clearly more sky
 * light, so standing at a cave mouth does not flicker, and the change fades in over {@link #FADE_TICKS}.
 */
public final class CaveDark {
    /** Enter at sky light 2 or less with 8+ blocks of world above the eyes; stay until sky light 7+ or under 4 blocks. */
    public static final int ENTER_SKY = 2, LEAVE_SKY = 7, ENTER_DEPTH = 8, LEAVE_DEPTH = 4, FADE_TICKS = 40;

    /** {@code depth} = world-surface height at the column minus the eye's block y. */
    public static boolean underground(boolean was, int skyLight, int depth) {
        return was ? skyLight < LEAVE_SKY && depth >= LEAVE_DEPTH : skyLight <= ENTER_SKY && depth >= ENTER_DEPTH;
    }

    /** One tick of the 2-second fade towards 1 (underground) or 0. */
    public static float step(float fade, boolean underground) {
        return Math.clamp(fade + (underground ? 1F : -1F) / FADE_TICKS, 0F, 1F);
    }

    /** The fade eased (smoothstep), 0 = surface lighting, 1 = no gamma and no ambient floor. */
    public static float strength(float fade) {
        return fade * fade * (3 - 2 * fade);
    }

    private CaveDark() {}
}
