package com.mechanical.client.skills;

import static com.mechanical.client.ui.HideUi.DARK;
import static com.mechanical.client.ui.HideUi.DIM;
import static com.mechanical.client.ui.HideUi.EDGE_DARK;
import static com.mechanical.client.ui.HideUi.GOLD;
import static com.mechanical.client.ui.HideUi.GOOD;
import static com.mechanical.client.ui.HideUi.LABEL;
import static com.mechanical.client.ui.HideUi.NAME;
import static com.mechanical.client.ui.HideUi.TEXT;

import java.util.List;
import java.util.Locale;

import com.mechanical.Mechanical;
import com.mechanical.client.ui.HideUi;
import com.mechanical.skills.Skill;
import com.mechanical.skills.Skills;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The K screen: the twelve skills down the left (icon, name, level, the way to the next level, and the jewelry bonus in
 * gold), and on the right the picked skill: its level and XP, and its perks, lit when unlocked with the chance they give
 * now, dim with the level they unlock at while locked.
 */
public class SkillsScreen extends Screen {
    /** The skill picked when the screen was last open, for the next time. */
    static Skill last = Skill.MINING;
    private static final Skill[] SKILLS = Skill.values();
    private static final int PAD = 8, HEAD = 28, LIST_W = 150, GAP = 6, LOCKED = 0xFF7E6E58;
    private static final Identifier CHECK = Identifier.fromNamespaceAndPath(Mechanical.MODID, "quest/check"),
            LOCK = Identifier.fromNamespaceAndPath(Mechanical.MODID, "quest/lock");

    private final ItemStack[] icons = new ItemStack[SKILLS.length];
    private Skill picked;
    private int left, top, w, h, step, paneW;

    public SkillsScreen(Skill picked) {
        super(Component.translatable("gui.mechanical.skills.title"));
        this.picked = picked;
        for (int i = 0; i < SKILLS.length; i++) icons[i] = SKILLS[i].icon();
    }

    @Override
    protected void init() {
        step = Math.clamp((height - 8 - HEAD - PAD) / SKILLS.length, 14, 22);
        paneW = Math.clamp(width - 8 - 2 * PAD - LIST_W - GAP, 120, 176);
        w = 2 * PAD + LIST_W + GAP + paneW;
        h = HEAD + SKILLS.length * step - 2 + PAD;
        left = (width - w) / 2;
        top = (height - h) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void pick(Skill s) {
        if (s == picked) return;
        picked = last = s;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (SkillsClient.KEY.matches(event)) {
            onClose();
            return true;
        }
        int key = event.key();
        if (key == InputConstants.KEY_UP || key == InputConstants.KEY_DOWN) {
            pick(SKILLS[Math.floorMod(picked.ordinal() + (key == InputConstants.KEY_UP ? -1 : 1), SKILLS.length)]);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int row = rowAt(event.x(), event.y());
        if (event.button() == 0 && row >= 0) {
            pick(SKILLS[row]);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY != 0) pick(SKILLS[Math.clamp(picked.ordinal() + (scrollY > 0 ? -1 : 1), 0, SKILLS.length - 1)]);
        return true;
    }

    private int rowAt(double mx, double my) {
        int x = left + PAD, y0 = top + HEAD;
        if (mx < x || mx >= x + LIST_W || my < y0) return -1;
        int i = (int) ((my - y0) / step);
        return i < SKILLS.length && my - y0 - i * step < step - 2 ? i : -1;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        HideUi.panel(g, left, top, w, h);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        Player p = minecraft.player;
        if (p == null) return;
        header(g, p);
        Component tip = null;
        for (int i = 0; i < SKILLS.length; i++) {
            Component t = row(g, p, i, mouseX, mouseY);
            if (t != null) tip = t;
        }
        pane(g, p);
        super.extractRenderState(g, mouseX, mouseY, a);
        if (tip != null) g.setTooltipForNextFrame(font, tip, mouseX, mouseY);
    }

    private void header(GuiGraphicsExtractor g, Player p) {
        g.text(font, title.copy().withStyle(ChatFormatting.BOLD), left + PAD, top + 8, NAME, true);
        int total = 0;
        for (Skill s : SKILLS) total += Skills.level(p, s);
        String n = String.valueOf(total);
        int nx = left + w - PAD - font.width(n);
        g.text(font, n, nx, top + 8, NAME, true);
        Component label = Component.translatable("gui.mechanical.skills.total");
        g.text(font, label, nx - 4 - font.width(label), top + 8, DIM, false);
        HideUi.seam(g, left + 7, top + 21, w - 14);
    }

    /** One skill's row; returns the jewelry tooltip when the mouse is on its "+N". */
    private Component row(GuiGraphicsExtractor g, Player p, int i, int mouseX, int mouseY) {
        Skill s = SKILLS[i];
        int x = left + PAD, y = top + HEAD + i * step, rh = step - 2;
        boolean selected = s == picked;
        HideUi.row(g, x, y, LIST_W, rh, selected || rowAt(mouseX, mouseY) == i, true);
        if (selected) {
            g.outline(x, y, LIST_W, rh, GOLD);
            g.fill(x + LIST_W, y + rh / 2 - 3, x + LIST_W + 2, y + rh / 2 + 3, GOLD);
            g.fill(x + LIST_W + 2, y + rh / 2 - 1, x + LIST_W + 4, y + rh / 2 + 1, GOLD);
        }
        g.item(icons[i], x + 3, y + (rh - 16) / 2);

        int xp = Skills.xp(p, s), level = Skills.level(xp), bonus = Skills.bonus(p, s);
        boolean two = rh >= 20;
        int ty = two ? y + 3 : y + (rh - 9) / 2, right = x + LIST_W - 5;
        String plus = bonus > 0 ? "+" + bonus : "", lv = String.valueOf(level);
        int plusW = bonus > 0 ? font.width(plus) + 2 : 0, lvX = right - plusW - font.width(lv);
        if (bonus > 0) g.text(font, plus, right - plusW + 2, ty, GOLD, true);
        g.text(font, lv, lvX, ty, NAME, true);
        g.text(font, cut(Component.translatable(s.langKey()), lvX - 5 - (x + 22)), x + 22, ty, selected ? NAME : TEXT, true);
        int colour = level >= Skills.MAX_LEVEL ? GOOD : GOLD;
        if (two) {
            bar(g, x + 23, y + rh - 6, LIST_W - 29, 2, progress(xp), colour);
        } else { // no frame: on a short row a framed bar would underline the name
            int bw = LIST_W - 27, by = y + rh - 2;
            g.fill(x + 22, by, x + 22 + bw, by + 1, DARK);
            g.fill(x + 22, by, x + 22 + (int) Math.ceil(bw * progress(xp)), by + 1, colour);
        }
        boolean onPlus = bonus > 0 && mouseX >= right - plusW && mouseX < right + 2 && mouseY >= ty - 1 && mouseY < ty + 9;
        return onPlus ? Component.translatable("gui.mechanical.skills.bonus_tip", bonus) : null;
    }

    private void pane(GuiGraphicsExtractor g, Player p) {
        int px = left + PAD + LIST_W + GAP, py = top + HEAD, ph = SKILLS.length * step - 2;
        HideUi.inset(g, px, py, paneW, ph);
        int x = px + 6, iw = paneW - 12, y = py + 6;
        Skill s = picked;
        int xp = Skills.xp(p, s), level = Skills.level(xp), bonus = Skills.bonus(p, s), eff = Skills.effectiveLevel(p, s);

        HideUi.slot(g, x, y);
        g.item(icons[s.ordinal()], x + 1, y + 1);
        g.text(font, cut(Component.translatable(s.langKey()).withStyle(ChatFormatting.BOLD), iw - 23), x + 23, y + 1, NAME, true);
        Component lv = Component.translatable("gui.mechanical.skills.level", level);
        if (bonus > 0) lv = lv.copy().append(Component.literal(" +" + bonus).withColor(GOLD));
        g.text(font, lv, x + 23, y + 11, TEXT, false);
        y += 24;

        boolean max = level >= Skills.MAX_LEVEL;
        bar(g, x + 1, y, iw - 2, 4, progress(xp), max ? GOOD : GOLD);
        y += 8;
        if (max) {
            g.text(font, Component.translatable("gui.mechanical.skills.max"), x, y, GOOD, false);
        } else {
            int from = Skills.xpForLevel(level), to = Skills.xpForLevel(level + 1);
            g.text(font, Component.translatable("gui.mechanical.skills.xp", num(xp - from), num(to - from)), x, y, DIM, false);
            Component go = Component.translatable("gui.mechanical.skills.to_go", num(to - xp));
            g.text(font, go, x + iw - font.width(go), y, DIM, false);
        }
        y += 11;
        if (bonus > 0) {
            for (FormattedCharSequence line : font.split(Component.translatable("gui.mechanical.skills.jewelry", bonus, eff), iw)) {
                g.text(font, line, x, y, GOLD, false);
                y += 10;
            }
        }
        HideUi.seam(g, px + 3, y + 1, paneW - 6);
        y += 7;
        g.text(font, Component.translatable("gui.mechanical.skills.perks"), x, y, LABEL, false);
        y += 13;

        for (Skills.Perk perk : Skills.perks(s)) {
            boolean on = eff >= perk.unlockLevel();
            double value = on ? perk.value(p) : perk.flat() > 0 ? perk.flat() : perk.unlockLevel() * perk.perLevel();
            Component text = Component.translatable(perk.langKey(), Component.literal(percent(value)).withColor(on ? GOLD : LOCKED));
            if (on) g.blitSprite(RenderPipelines.GUI_TEXTURED, CHECK, x, y, 9, 9);
            else g.blitSprite(RenderPipelines.GUI_TEXTURED, LOCK, x, y - 1, 9, 11);
            List<FormattedCharSequence> lines = font.split(text, iw - 13);
            for (FormattedCharSequence line : lines) {
                g.text(font, line, x + 13, y, on ? TEXT : LOCKED, on);
                y += 10;
            }
            if (!on) {
                g.text(font, Component.translatable("gui.mechanical.skills.unlocks", perk.unlockLevel()), x + 13, y, DIM, false);
                y += 10;
            }
            y += 5;
        }

        // how the skill is trained, at the foot of the pane when there is room
        List<FormattedCharSequence> how = font.split(Component.translatable("gui.mechanical.skills.how." + s.key()), iw);
        int hy = py + ph - 6 - how.size() * 10 + 1;
        if (hy - 7 > y) {
            HideUi.seam(g, px + 3, hy - 6, paneW - 6);
            for (FormattedCharSequence line : how) {
                g.text(font, line, x, hy, DIM, false);
                hy += 10;
            }
        }
    }

    /** How far through its current level {@code xp} is, 0-1 (1 at the top level). */
    private static double progress(int xp) {
        int level = Skills.level(xp);
        if (level >= Skills.MAX_LEVEL) return 1;
        int from = Skills.xpForLevel(level);
        return (xp - from) / (double) (Skills.xpForLevel(level + 1) - from);
    }

    private static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, double part, int colour) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE_DARK);
        g.fill(x, y, x + w, y + h, DARK);
        int fill = (int) Math.ceil(w * part);
        g.fill(x, y, x + fill, y + h, colour);
        if (fill > 0 && h > 2) g.fill(x, y, x + fill, y + 1, 0x50FFFFFF);
    }

    /** A chance or bonus fraction as a percent, with one decimal when it has one: 0.052 is "5.2%", 0.25 is "25%". */
    static String percent(double v) {
        long tenths = Math.round(v * 1000);
        return (tenths % 10 == 0 ? String.valueOf(tenths / 10) : tenths / 10 + "." + tenths % 10) + "%";
    }

    private static String num(int n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    /** One line of {@code text} no wider than {@code w}, cut with an ellipsis. */
    private FormattedCharSequence cut(Component text, int w) {
        if (font.width(text) <= w) return text.getVisualOrderText();
        return net.minecraft.locale.Language.getInstance().getVisualOrder(
                FormattedText.composite(font.substrByWidth(text, w - font.width("...")), FormattedText.of("...")));
    }
}
