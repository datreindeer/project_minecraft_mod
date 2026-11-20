package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.equipment.PackMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/**
 * {@link PackMenu}: the satchel's 3x3 and/or the quiver's 2x2, each under its own (item) name, on a background drawn
 * for that layout by scripts/gen_equipment_textures.py. The B key closes it again, like E closes the inventory.
 */
public class PackScreen extends AbstractContainerScreen<PackMenu> {
    private static final int LABEL_COLOR = -12566464;
    private final Identifier background;

    public PackScreen(PackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        String layout = menu.hasSatchel && menu.hasQuiver ? "both" : menu.hasSatchel ? "satchel" : "quiver";
        background = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/gui/container/pack_" + layout + ".png");
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, background, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (menu.hasSatchel && menu.hasQuiver) {
            label(graphics, menu.satchelName, 8, 104);
            label(graphics, menu.quiverName, 115, imageWidth - 8 - 115);
        } else {
            label(graphics, menu.hasSatchel ? menu.satchelName : menu.quiverName, 8, imageWidth - 16);
        }
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
    }

    /** A name at the top, cut to fit its column (a renamed bag can have a long name). */
    private void label(GuiGraphicsExtractor graphics, Component name, int x, int maxWidth) {
        FormattedCharSequence text = font.width(name) <= maxWidth ? name.getVisualOrderText()
                : net.minecraft.locale.Language.getInstance().getVisualOrder(font.substrByWidth(name, maxWidth - font.width("...")));
        graphics.text(font, text, x, titleLabelY, LABEL_COLOR, false);
        if (font.width(name) > maxWidth) graphics.text(font, "...", x + font.width(text), titleLabelY, LABEL_COLOR, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (EquipmentClient.OPEN_PACK.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
