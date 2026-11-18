package com.mechanical.client;

import com.mechanical.FletchingMenu;
import com.mechanical.Mechanical;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Fletching table screen: tip, shaft and fletching slots, then the result. */
public class FletchingScreen extends AbstractContainerScreen<FletchingMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/gui/fletching_table.png");

    public FletchingScreen(FletchingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BG, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }
}
