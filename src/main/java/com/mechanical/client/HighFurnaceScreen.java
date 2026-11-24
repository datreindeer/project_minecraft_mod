package com.mechanical.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mechanical.HighFurnaceBlockEntity;
import com.mechanical.HighFurnaceMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The High Furnace's screen (phase 2): a thermometer up to {@link #MAX}° with notches where things happen (bronze 950°,
 * iron ore 1450°, steel 1500°, charcoal's ceiling 1600°) and the target's notch, the blast per volume, each Firebox's
 * fuel, blast and fire, the drying out of a new furnace and the pool's size. Phase 3 adds the layer column and the
 * melting places. Drawn without a texture: a vanilla-like panel with a dark inset.
 */
public class HighFurnaceScreen extends AbstractContainerScreen<HighFurnaceMenu> {
    public static final int MAX = 1800, W = 262, H = 118;
    private static final int TUBE_X = 16, TUBE_W = 7, TUBE_TOP = 20, TUBE_BOTTOM = 96, TEXT_X = 44;
    private static final int[] MARKS = {950, 1450, 1500, 1600};
    private static final Identifier FLAME = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final int DARK = 0xFF404040, GREY = 0xFF6A6A6A, GREEN = 0xFF2E7D32, RED = 0xFFB71C1C, GOLD = 0xFF8A5A00;
    private final ItemStack charcoal = new ItemStack(Items.CHARCOAL);

    public HighFurnaceScreen(HighFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, W, H);
    }

    private static int row(int degrees) {
        return TUBE_BOTTOM - Math.round(Math.clamp(degrees / (float) MAX, 0, 1) * (TUBE_BOTTOM - TUBE_TOP));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(g, mouseX, mouseY, partialTick);
        int x = leftPos, y = topPos;
        // panel with a bevel, like vanilla's
        g.fill(x, y, x + W, y + H, 0xFF000000);
        g.fill(x + 1, y + 1, x + W - 1, y + H - 1, 0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + W - 2, y + 3, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 3, y + H - 2, 0xFFFFFFFF);
        g.fill(x + 3, y + H - 3, x + W - 1, y + H - 1, 0xFF555555);
        g.fill(x + W - 3, y + 3, x + W - 1, y + H - 1, 0xFF555555);
        // thermometer: a dark inset tube filled with the heat's colour
        g.fill(x + TUBE_X - 1, y + TUBE_TOP - 1, x + TUBE_X + TUBE_W + 1, y + TUBE_BOTTOM + 8, 0xFF373737);
        g.fill(x + TUBE_X, y + TUBE_TOP, x + TUBE_X + TUBE_W, y + TUBE_BOTTOM + 7, 0xFF1E1A18);
        int t = menu.temperature();
        for (int r = row(t); r < TUBE_BOTTOM; r++) {
            int deg = (TUBE_BOTTOM - r) * MAX / (TUBE_BOTTOM - TUBE_TOP);
            g.fill(x + TUBE_X, y + r, x + TUBE_X + TUBE_W, y + r + 1, KilnScreen.heatColour(deg));
        }
        int bulb = KilnScreen.heatColour(Math.max(t, 0));
        g.fill(x + TUBE_X, y + TUBE_BOTTOM, x + TUBE_X + TUBE_W, y + TUBE_BOTTOM + 7, bulb);
        g.fill(x + TUBE_X - 1, y + TUBE_BOTTOM + 1, x + TUBE_X + TUBE_W + 1, y + TUBE_BOTTOM + 6, bulb);
        for (int m : MARKS) {
            int r = y + row(m);
            g.fill(x + TUBE_X + TUBE_W + 1, r, x + TUBE_X + TUBE_W + 4, r + 1, t >= m ? 0xFF2E7D32 : 0xFF555555);
        }
        int tr = y + row(menu.target());
        g.fill(x + TUBE_X - 5, tr, x + TUBE_X - 1, tr + 1, 0xFFFFFFFF); // where the heat is heading
        // fireboxes: a flame while it burns, a charcoal icon
        int fy = y + 56;
        for (int i = 0; i < menu.fireboxes(); i++, fy += 14) {
            if (menu.burning(i)) g.blitSprite(RenderPipelines.GUI_TEXTURED, FLAME, 14, 14, 0, 0, x + TEXT_X, fy - 2, 14, 14);
            else g.item(charcoal, x + TEXT_X - 1, fy - 3);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.text(font, title, 8, 6, DARK, false);
        int t = menu.temperature(), target = menu.target();
        g.text(font, Component.translatable("gui.mechanical.high_furnace.heat", t, target), TEXT_X, 20, t >= 1450 ? GOLD : DARK, false);
        g.text(font, Component.translatable("gui.mechanical.high_furnace.blast", String.format(Locale.ROOT, "%.1f", menu.effectiveBlast()),
                HighFurnaceBlockEntity.FULL_BLAST, menu.totalBlast(), menu.volume()), TEXT_X, 31, DARK, false);
        Component state = menu.dryingSeconds() > 0
                ? Component.translatable("gui.mechanical.high_furnace.drying", menu.dryingSeconds() / 60, String.format(Locale.ROOT, "%02d", menu.dryingSeconds() % 60), HighFurnaceBlockEntity.DRY_MAX)
                : Component.translatable(menu.lit() ? "gui.mechanical.high_furnace.lit" : "gui.mechanical.high_furnace.out");
        g.text(font, state, TEXT_X, 42, menu.dryingSeconds() > 0 ? GOLD : menu.lit() ? GREEN : GREY, false);
        int fy = 56;
        for (int i = 0; i < menu.fireboxes(); i++, fy += 14)
            g.text(font, Component.translatable("gui.mechanical.high_furnace.firebox", i + 1, menu.fuel(i), HighFurnaceBlockEntity.FUEL_MAX, menu.blast(i)),
                    TEXT_X + 17, fy, menu.fuel(i) == 0 ? RED : DARK, false);
        g.text(font, Component.translatable("gui.mechanical.high_furnace.pool", menu.pool(), menu.capacity()), TEXT_X, H - 14, GREY, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        if (rx >= TUBE_X - 5 && rx < TUBE_X + TUBE_W + 5 && ry >= TUBE_TOP - 2 && ry < TUBE_BOTTOM + 8) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.mechanical.high_furnace.heat", menu.temperature(), menu.target()));
            for (int m : MARKS)
                lines.add(Component.translatable("gui.mechanical.high_furnace.mark." + m, m).withStyle(menu.temperature() >= m ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            List<FormattedCharSequence> seq = lines.stream().map(Component::getVisualOrderText).toList();
            g.setTooltipForNextFrame(font, seq, mouseX, mouseY);
        }
    }
}
