package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.SaddlebagMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * {@link SaddlebagMenu}: saddle and armour slots, the left satchel, the horse (follows the mouse) and the right
 * satchel, on a background drawn per satchel count by scripts/gen_equipment_textures.py (saddlebags_0/1/2.png).
 */
public class SaddlebagScreen extends AbstractContainerScreen<SaddlebagMenu> {
    private final Identifier background;
    private float xMouse, yMouse;

    public SaddlebagScreen(SaddlebagMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, SaddlebagMenu.WIDTH, 166);
        background = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/gui/container/saddlebags_" + menu.satchels + ".png");
        inventoryLabelX = SaddlebagMenu.INV_X;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, background, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, leftPos + 85, topPos + 18, leftPos + 127, topPos + 70, 14, 0.25F, xMouse, yMouse, menu.horse);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        xMouse = mouseX;
        yMouse = mouseY;
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
