package com.mechanical.client.people;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.people.OakFolk;
import com.mechanical.people.OakQuests;
import com.mechanical.people.Tribe;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The Oak Folk keeper's quest line (people.OakQuests) on a moss-green framed panel. The quest in focus fills the panel:
 * its item large in a medallion, number, title, the keeper's words, then two cards: what to bring (with a progress bar)
 * and the reward. Under it a strip of tokens, one per quest (done ticked, current glowing, later ones hidden); clicking
 * a done or the current one puts it in focus. "Hand in" gives what you carry of the item (the server answers with a
 * fresh screen); a closed or finished line puts the keeper's last words in focus instead.
 */
public class OakQuestScreen extends Screen {
    private static final int W = 340, H = 208, PAD = 6, BUTTON_H = 20, TOKEN = 18, TOKEN_GAP = 4, MEDAL = 56;
    private static final int BOX_TOP = 22, BOX_H = 128, CARD_TOP = 98, CARD_H = 46, STRIP_TOP = 158;
    private static final int PANEL = 0xEC1C2A16, BORDER = 0xFF6FA64A, CORNER = 0xFFC9B46A, SHINE = 0x30E8FFC0,
            INSET = 0xFF131D0E, INSET_EDGE = 0xFF3C5A2A, CARD = 0xFF1A2814, NAME = 0xFFD8F0A8, TEXT = 0xFFE4ECD8,
            STORY = 0xFFC6D2B6, DIM = 0xFF8A9A7C, DONE = 0xFF8CD06A, CURRENT = 0xFFF2D46A, HIDDEN = 0xFF56634C,
            CLOSED = 0xFFE07A5A, BAR_BG = 0xFF0C130A;
    private final OakQuests.Open open;
    private final int count = OakQuests.QUESTS.size();
    /** The quest in focus, or -1: the keeper's last words (line closed or finished). */
    private int focus;
    private int left, top;

    public OakQuestScreen(OakQuests.Open open) {
        super(open.name());
        this.open = open;
        focus = open.closed() || finished() ? -1 : open.stage();
    }

    private boolean finished() {
        return open.stage() >= count;
    }

    private boolean current(int i) {
        return i == open.stage() && !open.closed();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(2, (height - H) / 2);
        int y = top + H - PAD - BUTTON_H, cx = width / 2;
        if (!open.closed() && !finished()) {
            addRenderableWidget(Button.builder(Component.translatable("gui.mechanical.oak_quests.hand_in"), b ->
                    ClientPacketDistributor.sendToServer(new OakQuests.HandIn(open.entity()))).bounds(cx - 102, y, 100, BUTTON_H).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mechanical.tribe_dialog.farewell"), b -> onClose()).bounds(cx + 2, y, 100, BUTTON_H).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("gui.mechanical.tribe_dialog.farewell"), b -> onClose()).bounds(cx - 50, y, 100, BUTTON_H).build());
        }
    }

    private int tokenX(int i) {
        return left + (W - (count * (TOKEN + TOKEN_GAP) - TOKEN_GAP)) / 2 + i * (TOKEN + TOKEN_GAP);
    }

    private int tokenAt(double mx, double my) {
        int ty = top + STRIP_TOP;
        if (my < ty || my >= ty + TOKEN) return -1;
        for (int i = 0; i < count; i++) if (mx >= tokenX(i) && mx < tokenX(i) + TOKEN) return i;
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int i = tokenAt(event.x(), event.y());
        if (i >= 0 && (i < open.stage() || current(i))) {
            focus = i;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.fill(left - 1, top - 1, left + W + 1, top + H + 1, BORDER);
        graphics.fill(left, top, left + W, top + H, PANEL);
        graphics.outline(left + 1, top + 1, W - 2, H - 2, SHINE);
        corners(graphics, left - 1, top - 1, W + 2, H + 2);
    }

    private static void corners(GuiGraphicsExtractor graphics, int x, int y, int w, int h) {
        for (int[] c : new int[][] {{x, y}, {x + w - 3, y}, {x, y + h - 3}, {x + w - 3, y + h - 3}}) graphics.fill(c[0], c[1], c[0] + 3, c[1] + 3, CORNER);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // header: the keeper, and how the Oak Folk see your tribe
        graphics.text(font, open.name(), left + PAD + 2, top + PAD + 1, NAME, true);
        Component standing = Component.translatable("gui.mechanical.oak_quests.standing", Tribe.standing(open.standing()), open.standing());
        graphics.text(font, standing, left + W - PAD - 2 - font.width(standing), top + PAD + 1, DIM, false);

        int bx0 = left + PAD, by0 = top + BOX_TOP, bx1 = left + W - PAD, by1 = by0 + BOX_H;
        graphics.fill(bx0 - 1, by0 - 1, bx1 + 1, by1 + 1, INSET_EDGE);
        graphics.fill(bx0, by0, bx1, by1, INSET);
        if (focus >= 0) featured(graphics, focus, mouseX, mouseY);
        else lastWords(graphics, mouseX, mouseY);
        strip(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    /** The medallion: a framed, stepped square with {@code item} drawn three times its size. */
    private void medallion(GuiGraphicsExtractor graphics, ItemStack item, int x, int y, int ring) {
        graphics.fill(x, y, x + MEDAL, y + MEDAL, ring);
        graphics.fill(x + 1, y + 1, x + MEDAL - 1, y + MEDAL - 1, 0xFF0E160A);
        graphics.fill(x + 3, y + 3, x + MEDAL - 3, y + MEDAL - 3, 0xFF22341A);
        graphics.fill(x + 6, y + 6, x + MEDAL - 6, y + MEDAL - 6, 0xFF2A3F20);
        corners(graphics, x, y, MEDAL, MEDAL);
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x + 4, y + 4);
        pose.scale(3F, 3F);
        graphics.item(item, 0, 0);
        pose.popMatrix();
    }

    /** Right of the medallion: the kicker line with a tag at its end, the title (half again as big) and the keeper's words. */
    private void heading(GuiGraphicsExtractor graphics, Component kicker, Component tag, int accent, Component title, Component story, int storyColour) {
        int tx = left + PAD + 6 + MEDAL + 10, tw = left + W - PAD - 6 - tx, y = top + BOX_TOP + 6;
        graphics.text(font, kicker, tx, y, DIM, false);
        graphics.text(font, tag, tx + tw - font.width(tag), y, accent, false);
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(tx, y + 12);
        pose.scale(1.5F, 1.5F);
        graphics.text(font, title, 0, 0, accent, true);
        pose.popMatrix();
        List<FormattedCharSequence> lines = font.split(story, tw);
        for (int i = 0; i < Math.min(3, lines.size()); i++) graphics.text(font, lines.get(i), tx, y + 30 + i * 10, storyColour, false);
    }

    private void featured(GuiGraphicsExtractor graphics, int i, int mouseX, int mouseY) {
        OakQuests.Quest q = OakQuests.QUESTS.get(i);
        boolean done = i < open.stage();
        int delivered = done ? q.count() : open.delivered(), accent = done ? DONE : CURRENT;
        medallion(graphics, new ItemStack(q.item().get()), left + PAD + 6, top + BOX_TOP + 6, accent);
        heading(graphics, Component.translatable("gui.mechanical.oak_quests.number", i + 1, count),
                Component.translatable(done ? "gui.mechanical.oak_quests.completed" : "gui.mechanical.oak_quests.in_progress"), accent,
                Component.translatable("quest.mechanical.oak." + q.id()),
                Component.translatable("quest.mechanical.oak." + q.id() + ".story").withStyle(ChatFormatting.ITALIC), STORY);

        // two cards: what to bring (with progress) and the reward
        int cy = top + CARD_TOP, cw = (W - PAD * 2 - 18) / 2, c1 = left + PAD + 6, c2 = c1 + cw + 6;
        card(graphics, c1, cy, cw, Component.translatable("gui.mechanical.oak_quests.bring"));
        card(graphics, c2, cy, cw, Component.translatable("gui.mechanical.oak_quests.reward"));

        Component tally = Component.literal(delivered + " / " + q.count());
        graphics.text(font, tally, c1 + cw - 5 - font.width(tally), cy + 4, accent, false);
        ItemStack wanted = new ItemStack(q.item().get(), q.count());
        slot(graphics, wanted, c1 + 5, cy + 15, mouseX, mouseY);
        graphics.text(font, wanted.getHoverName(), c1 + 27, cy + 20, TEXT, false);
        bar(graphics, c1 + 5, cy + CARD_H - 8, cw - 10, delivered, q.count(), accent);

        int rx = c2 + 5;
        for (var r : q.rewards()) {
            slot(graphics, r.get(), rx, cy + 15, mouseX, mouseY);
            rx += 20;
        }
        int nw = c2 + cw - rx - 6;
        if (q.rewards().size() == 1) {
            Component name = q.rewards().get(0).get().getHoverName();
            graphics.textWithWordWrap(font, name, rx + 2, cy + (font.width(name) > nw ? 15 : 20), nw, TEXT);
        } else { // several: one short name a line beside the slots
            for (int n = 0; n < Math.min(2, q.rewards().size()); n++)
                graphics.text(font, font.substrByWidth(q.rewards().get(n).get().getHoverName(), nw).getString(), rx + 2, cy + 15 + n * 9, TEXT, false);
        }
        if (done) graphics.text(font, Component.translatable("gui.mechanical.oak_quests.received"), c2 + 5, cy + CARD_H - 10, DONE, false);
    }

    /** A progress bar {@code value} of {@code max}, with a notch between each step while they fit. */
    private static void bar(GuiGraphicsExtractor graphics, int x, int y, int w, int value, int max, int accent) {
        int fill = w * Math.min(value, max) / max;
        graphics.fill(x - 1, y - 1, x + w + 1, y + 5, INSET_EDGE);
        graphics.fill(x, y, x + w, y + 4, BAR_BG);
        graphics.fill(x, y, x + fill, y + 4, accent);
        if (fill > 0) graphics.fill(x, y, x + fill, y + 1, 0x60FFFFFF);
        if (max <= 16) for (int n = 1; n < max; n++) graphics.fill(x + w * n / max, y, x + w * n / max + 1, y + 4, 0x90000000);
    }

    /** A card in the featured box: a slightly lighter panel with a bold label. */
    private void card(GuiGraphicsExtractor graphics, int x, int y, int w, Component label) {
        graphics.fill(x - 1, y - 1, x + w + 1, y + CARD_H + 1, INSET_EDGE);
        graphics.fill(x, y, x + w, y + CARD_H, CARD);
        graphics.text(font, label.copy().withStyle(ChatFormatting.BOLD), x + 5, y + 4, NAME, false);
    }

    /** An inventory-like slot with {@code stack} and its count; its tooltip when hovered. */
    private void slot(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0A1007);
        graphics.fill(x, y, x + 17, y + 17, INSET_EDGE);
        graphics.fill(x, y, x + 16, y + 16, 0xFF26381C);
        graphics.item(stack, x, y);
        graphics.itemDecorations(font, stack, x, y);
        if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
    }

    /** The line closed or finished: the keeper's staff, what they say, and the gifts the line has given. */
    private void lastWords(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        boolean closed = open.closed();
        int accent = closed ? CLOSED : DONE;
        medallion(graphics, new ItemStack(OakFolk.VERDANT_STAFF.get()), left + PAD + 6, top + BOX_TOP + 6, accent);
        heading(graphics, Component.translatable("gui.mechanical.oak_quests.line"), Component.literal(Math.min(open.stage(), count) + " / " + count), accent,
                Component.translatable(closed ? "gui.mechanical.oak_quests.closed_title" : "gui.mechanical.oak_quests.done_title"),
                Component.translatable(closed ? "gui.mechanical.oak_quests.closed" : "gui.mechanical.oak_quests.all_done").withStyle(ChatFormatting.ITALIC),
                closed ? 0xFFE8B8A4 : STORY);
        // what the line has given so far
        int x = left + PAD + 6, y = top + CARD_TOP, w = W - PAD * 2 - 12, done = Math.min(open.stage(), count), sx = x + 5;
        card(graphics, x, y, w, Component.translatable("gui.mechanical.oak_quests.gifts"));
        for (int i = 0; i < done; i++)
            for (var r : OakQuests.QUESTS.get(i).rewards()) {
                slot(graphics, r.get(), sx, y + 15, mouseX, mouseY);
                sx += 20;
            }
        if (done == 0) graphics.text(font, Component.translatable("gui.mechanical.oak_quests.no_gifts"), sx, y + 20, DIM, false);
        bar(graphics, x + 5, y + CARD_H - 8, w - 10, done, count, accent);
    }

    /** One token per quest, joined by a vine; the one in focus framed in gold, with a notch pointing up at the box. */
    private void strip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int ty = top + STRIP_TOP, vine = ty + TOKEN / 2;
        graphics.fill(tokenX(0) + TOKEN / 2, vine, tokenX(count - 1) + TOKEN / 2, vine + 1, HIDDEN);
        if (open.stage() > 0) graphics.fill(tokenX(0) + TOKEN / 2, vine, tokenX(Math.min(open.stage(), count - 1)) + TOKEN / 2, vine + 1, DONE);
        float pulse = (float) (0.5 + 0.5 * Math.sin(Util.getMillis() / 300.0));
        int hovered = tokenAt(mouseX, mouseY);
        for (int i = 0; i < count; i++) {
            OakQuests.Quest q = OakQuests.QUESTS.get(i);
            int tx = tokenX(i);
            boolean done = i < open.stage(), cur = current(i), shown = done || cur;
            if (i == focus) {
                graphics.fill(tx - 2, ty - 2, tx + TOKEN + 2, ty + TOKEN + 2, CORNER);
                for (int n = 0; n < 3; n++) graphics.fill(tx + TOKEN / 2 - 3 + n, ty - 3 - n, tx + TOKEN / 2 + 3 - n, ty - 2 - n, CORNER);
            }
            graphics.fill(tx - 1, ty - 1, tx + TOKEN + 1, ty + TOKEN + 1, done ? DONE : cur ? lerp(0xFFB89A44, CURRENT, pulse) : HIDDEN);
            graphics.fill(tx, ty, tx + TOKEN, ty + TOKEN, shown ? (i == hovered ? 0xFF33492A : 0xFF22341A) : 0xFF141C10);
            if (shown) {
                graphics.item(new ItemStack(q.item().get()), tx + 1, ty + 1);
                if (done) {
                    graphics.fill(tx + TOKEN - 7, ty + TOKEN - 7, tx + TOKEN, ty + TOKEN, 0xFF1C2A16);
                    graphics.text(font, "✔", tx + TOKEN - 6, ty + TOKEN - 8, DONE, false);
                }
            } else {
                graphics.text(font, "?", tx + (TOKEN - font.width("?")) / 2 + 1, ty + 5, HIDDEN, false);
            }
            if (i == hovered) {
                List<Component> tip = new ArrayList<>();
                if (shown) {
                    tip.add(Component.translatable("quest.mechanical.oak." + q.id()).withColor(done ? DONE : CURRENT));
                    tip.add(done ? Component.translatable("gui.mechanical.oak_quests.completed").withStyle(ChatFormatting.GRAY)
                            : Component.literal(open.delivered() + " / " + q.count()).withStyle(ChatFormatting.GRAY));
                } else {
                    tip.add(Component.translatable("gui.mechanical.oak_quests.hidden").withStyle(ChatFormatting.GRAY));
                }
                graphics.setTooltipForNextFrame(tip.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
            }
        }
    }

    private static int lerp(int from, int to, float t) {
        int r = (int) (((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * t);
        int g = (int) (((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * t);
        int b = (int) ((from & 255) + ((to & 255) - (from & 255)) * t);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
