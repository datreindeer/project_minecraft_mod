package com.mechanical.client;

import com.mechanical.KilnMenu;
import com.mechanical.Mechanical;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Furnace-style screen: the flame shows fuel left while lit, the arrow shows baking progress. */
public class KilnScreen extends AbstractContainerScreen<KilnMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/gui/container/kiln.png");
    private static final Identifier FLAME = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier ARROW = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final int FLAME_X = 56, FLAME_Y = 36;

    public KilnScreen(KilnMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BG, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        if (menu.lit()) {
            int h = (int) Math.ceil(menu.burn() * 13) + 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FLAME, 14, 14, 0, 14 - h, leftPos + FLAME_X, topPos + FLAME_Y + 14 - h, 14, h);
        }
        int arrow = (int) Math.ceil(menu.progress() * 24);
        if (arrow > 0) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ARROW, 24, 16, 0, 0, leftPos + 79, topPos + 34, arrow, 16);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + FLAME_X && mouseX < leftPos + FLAME_X + 14 && mouseY >= topPos + FLAME_Y && mouseY < topPos + FLAME_Y + 14) {
            graphics.setTooltipForNextFrame(font, Component.translatable(menu.lit() ? "gui.mechanical.kiln.lit" : "gui.mechanical.kiln.unlit"), mouseX, mouseY);
        }
    }
}
