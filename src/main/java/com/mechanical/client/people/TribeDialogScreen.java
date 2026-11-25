package com.mechanical.client.people;

import com.mechanical.client.ui.HideUi;
import com.mechanical.people.TribeDialog;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * What a tribe's envoy says, on a hide-coloured panel, with a row of buttons: Trade, Yes / No when they ask to stay the
 * night, and Farewell. A click goes to the server (TribeDialog.Answer) and closes the screen; the server decides.
 */
public class TribeDialogScreen extends Screen {
    private static final int W = 280, PAD = 10, BUTTON_H = 20;
    private final TribeDialog.Open open;
    private int top, textHeight;

    public TribeDialogScreen(TribeDialog.Open open) {
        super(open.name());
        this.open = open;
    }

    @Override
    protected void init() {
        textHeight = font.split(open.text(), W - PAD * 2).size() * 9;
        int height = PAD + 12 + textHeight + PAD + BUTTON_H + PAD;
        top = (this.height - height) / 2;
        int y = top + height - PAD - BUTTON_H;
        java.util.List<Button> buttons = new java.util.ArrayList<>();
        if ((open.buttons() & TribeDialog.TRADE) != 0) buttons.add(button("gui.mechanical.tribe_dialog.trade", TribeDialog.CHOOSE_TRADE));
        if ((open.buttons() & TribeDialog.YES_NO) != 0) {
            buttons.add(button("gui.mechanical.tribe_dialog.yes", TribeDialog.CHOOSE_YES));
            buttons.add(button("gui.mechanical.tribe_dialog.no", TribeDialog.CHOOSE_NO));
        }
        if ((open.buttons() & TribeDialog.ROAD) != 0) buttons.add(button("gui.mechanical.tribe_dialog.road", TribeDialog.CHOOSE_ROAD));
        if ((open.buttons() & TribeDialog.UPGRADE) != 0) buttons.add(button("gui.mechanical.tribe_dialog.upgrade", TribeDialog.CHOOSE_UPGRADE));
        buttons.add(Button.builder(Component.translatable("gui.mechanical.tribe_dialog.farewell"), b -> onClose()).build());
        int bw = Math.min(80, (W - PAD * 2 - 4 * (buttons.size() - 1)) / buttons.size());
        int x = (this.width - (bw * buttons.size() + 4 * (buttons.size() - 1))) / 2;
        for (Button b : buttons) {
            b.setRectangle(bw, BUTTON_H, x, y);
            addRenderableWidget(b);
            x += bw + 4;
        }
    }

    private Button button(String key, int choice) {
        return Button.builder(Component.translatable(key), b -> {
            ClientPacketDistributor.sendToServer(new TribeDialog.Answer(open.entity(), choice));
            onClose();
        }).build();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int left = (width - W) / 2, height = PAD + 12 + textHeight + PAD + BUTTON_H + PAD;
        HideUi.panel(graphics, left, top, W, height);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        int left = (width - W) / 2;
        graphics.text(font, open.name(), left + PAD, top + PAD, HideUi.NAME);
        graphics.textWithWordWrap(font, open.text(), left + PAD, top + PAD + 12, W - PAD * 2, HideUi.TEXT);
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
