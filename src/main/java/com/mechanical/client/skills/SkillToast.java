package com.mechanical.client.skills;

import com.mechanical.Mechanical;
import com.mechanical.client.ui.HideUi;
import com.mechanical.skills.Skill;
import com.mechanical.skills.Skills;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;

/**
 * Above the hotbar: "+10 Mining" on a small faded hide tag with the way to the next level beneath, fading out; gains of
 * the same skill while it shows add up. A new level shows "Mining 11!" in gold, a while longer.
 */
public final class SkillToast {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Mechanical.MODID, "skill_toast");
    private static final long SHOW = 2000, LEVEL_SHOW = 3500, FADE = 600;

    private static Skill skill;
    private static int amount, level;
    private static long since;

    private SkillToast() {}

    static void add(Skill s, int xp, int newLevel) {
        long now = Util.getMillis();
        boolean merge = s == skill && now - since < (level > 0 ? LEVEL_SHOW : SHOW) - FADE;
        amount = merge ? amount + xp : xp;
        if (!merge) level = 0;
        if (newLevel > 0) level = newLevel;
        skill = s;
        since = now;
    }

    static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (skill == null || mc.player == null || mc.gui.hud.isHidden()) return;
        long age = Util.getMillis() - since, show = level > 0 ? LEVEL_SHOW : SHOW;
        if (age >= show) {
            skill = null;
            return;
        }
        float alpha = Math.min(1, Math.min(age / 120F, (show - age) / (float) FADE));
        if (alpha < 0.05F) return;
        Component name = Component.translatable(skill.langKey());
        Component text = level > 0 ? Component.translatable("gui.mechanical.skills.level_up", name, level)
                : Component.translatable("gui.mechanical.skills.gain", amount, name);
        int tw = mc.font.width(text), w = Math.max(tw + 24, 80), h = 25;
        int x = (g.guiWidth() - w) / 2, y = g.guiHeight() - 100 + Math.round((1 - alpha) * 4);
        HideUi.panel(g, x, y, w, h, alpha * 0.92F);
        g.text(mc.font, text, x + (w - tw) / 2, y + 6, ARGB.multiplyAlpha(level > 0 ? HideUi.GOLD : HideUi.TEXT, alpha), true);
        // the way to the next level, from the synced XP
        int xp = Skills.xp(mc.player, skill), l = Skills.level(xp);
        int from = Skills.xpForLevel(l), to = Skills.xpForLevel(l + 1);
        int bx = x + 8, by = y + 17, bw = w - 16;
        int fill = l >= Skills.MAX_LEVEL ? bw : (int) Math.ceil(bw * (double) (xp - from) / (to - from));
        g.fill(bx, by, bx + bw, by + 2, ARGB.multiplyAlpha(HideUi.DARK, alpha));
        g.fill(bx, by, bx + fill, by + 2, ARGB.multiplyAlpha(HideUi.GOLD, alpha));
    }
}
