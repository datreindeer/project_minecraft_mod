package com.mechanical.client.people;

import com.mechanical.people.OakQuests;
import com.mechanical.people.Tribe;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The Oak Folk keeper's quest line (people.OakQuests): a moss-green panel listing every quest, the finished ones ticked,
 * the current one with its item, how many are still wanted and its reward, the later ones hidden. "Hand in" gives what you
 * carry of the item (the server answers with a fresh screen); a closed line shows only the keeper's refusal.
 */
public class OakQuestScreen extends Screen {
    private static final int W = 300, PAD = 6, ROW = 10, BUTTON_H = 20;
    private static final int PANEL = 0xE81C2A16, BORDER = 0xFF6FA64A, NAME = 0xFFD8F0A8, TEXT = 0xFFE4ECD8, DONE = 0xFF8CD06A,
            CURRENT = 0xFFF2D46A, HIDDEN = 0xFF7C8A70, CLOSED = 0xFFE07A5A;
    private final OakQuests.Open open;
    private int top, panelH;

    public OakQuestScreen(OakQuests.Open open) {
        super(open.name());
        this.open = open;
    }

    private boolean finished() {
        return open.stage() >= OakQuests.QUESTS.size();
    }

    @Override
    protected void init() {
        panelH = PAD + 22 + OakQuests.QUESTS.size() * ROW + 4 + 20 + 4 + BUTTON_H + PAD;
        top = Math.max(2, (this.height - panelH) / 2);
        int y = top + panelH - PAD - BUTTON_H, cx = width / 2;
        if (!open.closed() && !finished()) {
            addRenderableWidget(Button.builder(Component.translatable("gui.mechanical.oak_quests.hand_in"), b ->
                    ClientPacketDistributor.sendToServer(new OakQuests.HandIn(open.entity()))).bounds(cx - 102, y, 100, BUTTON_H).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mechanical.tribe_dialog.farewell"), b -> onClose()).bounds(cx + 2, y, 100, BUTTON_H).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("gui.mechanical.tribe_dialog.farewell"), b -> onClose()).bounds(cx - 50, y, 100, BUTTON_H).build());
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int left = (width - W) / 2;
        graphics.fill(left - 1, top - 1, left + W + 1, top + panelH + 1, BORDER);
        graphics.fill(left, top, left + W, top + panelH, PANEL);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int left = (width - W) / 2, x = left + PAD, y = top + PAD;
        graphics.text(font, open.name(), x, y, NAME);
        graphics.text(font, Component.translatable("gui.mechanical.oak_quests.standing", Tribe.standing(open.standing()), open.standing()), x, y + 11, TEXT);
        y += 22;
        for (int i = 0; i < OakQuests.QUESTS.size(); i++, y += ROW) {
            OakQuests.Quest q = OakQuests.QUESTS.get(i);
            Component title = Component.translatable("quest.mechanical.oak." + q.id());
            if (i < open.stage()) graphics.text(font, Component.literal("✔ ").append(title), x, y, DONE);
            else if (i == open.stage() && !open.closed()) graphics.text(font, Component.literal("▶ ").append(title)
                    .append(Component.literal("  " + open.delivered() + " / " + q.count())), x, y, CURRENT);
            else graphics.text(font, Component.literal((i + 1) + ". ").append(Component.translatable("gui.mechanical.oak_quests.hidden")), x, y, HIDDEN);
        }
        y += 4;
        if (open.closed()) {
            graphics.textWithWordWrap(font, Component.translatable("gui.mechanical.oak_quests.closed"), x, y, W - PAD * 2, CLOSED);
        } else if (finished()) {
            graphics.textWithWordWrap(font, Component.translatable("gui.mechanical.oak_quests.all_done"), x, y, W - PAD * 2, DONE);
        } else {
            OakQuests.Quest q = OakQuests.QUESTS.get(open.stage());
            ItemStack wanted = new ItemStack(q.item().get(), q.count() - open.delivered());
            // one line: Bring [item] name   Reward [items] name
            graphics.text(font, Component.translatable("gui.mechanical.oak_quests.bring"), x, y + 5, TEXT);
            int bx = x + font.width(Component.translatable("gui.mechanical.oak_quests.bring")) + 3;
            graphics.item(wanted, bx, y);
            graphics.itemDecorations(font, wanted, bx, y);
            graphics.text(font, wanted.getHoverName(), bx + 18, y + 5, TEXT);
            int rx = left + W / 2 + 4;
            graphics.text(font, Component.translatable("gui.mechanical.oak_quests.reward"), rx, y + 5, TEXT);
            rx += font.width(Component.translatable("gui.mechanical.oak_quests.reward")) + 3;
            for (var r : q.rewards()) {
                ItemStack s = r.get();
                graphics.item(s, rx, y);
                graphics.itemDecorations(font, s, rx, y);
                rx += 17;
            }
            graphics.text(font, q.rewards().get(0).get().getHoverName(), rx + 1, y + 5, TEXT);
        }
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
