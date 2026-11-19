package com.mechanical.client;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.KilnBlockEntity;
import com.mechanical.KilnMenu;
import com.mechanical.Mechanical;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/**
 * Furnace-style screen: the flame shows fuel left while lit, the arrow shows baking progress. On the left a
 * thermometer (glass tube and marks for 600/950/1085° are in kiln.png, gen_copper_textures.py) filled dark red to
 * white-yellow, a white notch at the heat the current bake needs, and beside it the red overheating strain bar.
 */
public class KilnScreen extends AbstractContainerScreen<KilnMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/gui/container/kiln.png");
    private static final Identifier FLAME = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier ARROW = Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final int FLAME_X = 56, FLAME_Y = 36;
    /** Inside of the thermometer tube (in kiln.png): x 22..27, y 18 (CEILING) .. 62 (0°); the bulb is below it. */
    public static final int TUBE_X = 22, TUBE_W = 6, TUBE_TOP = 18, TUBE_BOTTOM = 62;
    private static final int STRAIN_X = 34, STRAIN_W = 4;

    public KilnScreen(KilnMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    /** Row (screen-relative) for a temperature on the tube. */
    public static int rowFor(int degrees) {
        return TUBE_BOTTOM - Math.round(Math.clamp(degrees / (float) KilnBlockEntity.CEILING, 0, 1) * (TUBE_BOTTOM - TUBE_TOP));
    }

    /** Glow colour of hot clay: dark red, red at 600°, orange at 950°, white-yellow at the ceiling. */
    public static int heatColour(int degrees) {
        int[][] stops = {{0, 0x4A1A14}, {600, 0xB0200C}, {950, 0xF07A10}, {1085, 0xFFB030}, {1300, 0xFFF4C0}};
        for (int i = 1; i < stops.length; i++) {
            if (degrees > stops[i][0] && i < stops.length - 1) continue;
            float f = Math.clamp((degrees - stops[i - 1][0]) / (float) (stops[i][0] - stops[i - 1][0]), 0, 1);
            int a = stops[i - 1][1], b = stops[i][1], c = 0xFF000000;
            for (int sh = 0; sh <= 16; sh += 8) c |= Math.round(((a >> sh) & 255) * (1 - f) + ((b >> sh) & 255) * f) << sh;
            return c;
        }
        return 0xFFFFF4C0;
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

        // mercury: each row coloured by the temperature it stands for, up to the current temperature; the bulb glows with it
        int t = menu.temperature();
        for (int y = rowFor(t); y < TUBE_BOTTOM; y++) {
            int deg = (TUBE_BOTTOM - y) * KilnBlockEntity.CEILING / (TUBE_BOTTOM - TUBE_TOP);
            graphics.fill(leftPos + TUBE_X, topPos + y, leftPos + TUBE_X + TUBE_W, topPos + y + 1, heatColour(deg));
        }
        int bulb = heatColour(Math.max(t, 0));
        graphics.fill(leftPos + TUBE_X, topPos + TUBE_BOTTOM, leftPos + TUBE_X + TUBE_W, topPos + TUBE_BOTTOM + 6, bulb);
        graphics.fill(leftPos + TUBE_X - 1, topPos + TUBE_BOTTOM + 1, leftPos + TUBE_X + TUBE_W + 1, topPos + TUBE_BOTTOM + 5, bulb);
        int need = menu.needed();
        if (need > 0) { // notch left of the tube at the heat the bake needs: white, green once reached
            int y = topPos + rowFor(need);
            graphics.fill(leftPos + TUBE_X - 4, y, leftPos + TUBE_X - 1, y + 1, t >= need ? 0xFF80FF80 : 0xFFFFFFFF);
        }
        float strain = menu.cracked() ? 1 : menu.strain();
        if (strain > 0) {
            int x = leftPos + STRAIN_X, y0 = topPos + TUBE_TOP - 1, y1 = topPos + TUBE_BOTTOM + 1;
            graphics.fill(x - 1, y0, x + STRAIN_W + 1, y1, 0xFF373737);
            graphics.fill(x, y0 + 1, x + STRAIN_W, y1 - 1, 0xFF1E1E1E);
            int h = Math.round(strain * (y1 - y0 - 2));
            graphics.fill(x, y1 - 1 - h, x + STRAIN_W, y1 - 1, menu.cracked() ? 0xFF6E1010 : 0xFFE03020);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + FLAME_X && mouseX < leftPos + FLAME_X + 14 && mouseY >= topPos + FLAME_Y && mouseY < topPos + FLAME_Y + 14) {
            graphics.setTooltipForNextFrame(font, Component.translatable(menu.lit() ? "gui.mechanical.kiln.lit" : "gui.mechanical.kiln.unlit"), mouseX, mouseY);
        }
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= TUBE_X - 4 && rx < STRAIN_X + STRAIN_W + 1 && ry >= TUBE_TOP - 2 && ry < TUBE_BOTTOM + 8) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.mechanical.kiln.temperature", menu.temperature()));
            if (menu.needed() > 0) lines.add(Component.translatable("gui.mechanical.kiln.needs", menu.needed())
                    .withStyle(menu.temperature() >= menu.needed() ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            if (menu.needed() > 0 && menu.temperature() >= menu.needed()) lines.add(Component.translatable("gui.mechanical.kiln.speed",
                    String.format(java.util.Locale.ROOT, "%.1f", KilnBlockEntity.speed(menu.temperature(), menu.needed()))).withStyle(ChatFormatting.GOLD));
            if (menu.cracked()) lines.add(Component.translatable("gui.mechanical.kiln.cracked", KilnBlockEntity.REPAIR_CLAY).withStyle(ChatFormatting.RED));
            else if (menu.strain() > 0) lines.add(Component.translatable("gui.mechanical.kiln.strain", Math.round(menu.strain() * 100)).withStyle(ChatFormatting.RED));
            List<FormattedCharSequence> seq = lines.stream().map(Component::getVisualOrderText).toList();
            graphics.setTooltipForNextFrame(seq, mouseX, mouseY);
        }
    }
}
