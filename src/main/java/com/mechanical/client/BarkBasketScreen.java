package com.mechanical.client;

import com.mechanical.BarkBasketMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** 3x3 screen for the Bark Basket, on vanilla's dispenser background. */
public class BarkBasketScreen extends AbstractContainerScreen<BarkBasketMenu> {
    private static final Identifier BG = Identifier.withDefaultNamespace("textures/gui/container/dispenser.png");

    public BarkBasketScreen(BarkBasketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BG, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
    }
}
