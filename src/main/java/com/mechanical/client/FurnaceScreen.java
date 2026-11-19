package com.mechanical.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mechanical.CrucibleContents;
import com.mechanical.FurnaceMelt;
import com.mechanical.FurnaceMenu;
import com.mechanical.FurnaceTapBlockEntity;
import com.mechanical.KilnBlockEntity;
import com.mechanical.Mechanical;
import com.mechanical.Metal;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Smelting Furnace's status: the kiln's thermometer (white notch at the melting point the charge needs), a tank
 * showing the molten pool in the metal's colour with the unmelted charge above it (darker), the fuel flame and
 * text lines: molten metal per kind, ore still melting, the copper : tin mix, and what the tap is doing.
 * Background: textures/gui/container/smelting_furnace.png (gen_smelter_textures.py).
 */
public class FurnaceScreen extends AbstractContainerScreen<FurnaceMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/gui/container/smelting_furnace.png");
    private static final Identifier FLAME = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    /** Inside of the tank: x 40..51, rows 18..61 (= {@link FurnaceMelt#CAPACITY} quarters). */
    public static final int TANK_X = 40, TANK_W = 12, FLAME_X = 22, FLAME_Y = 76, TEXT_X = 62;
    private static final int DARK = 0xFF404040, GREEN = 0xFF2E7D32, RED = 0xFFB71C1C, GOLD = 0xFF8A6A00;

    public FurnaceScreen(FurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 220, 96);
    }

    /** Molten colour of a metal (also the channel strip's tint, client/MoltenRenderer). */
    public static int colour(Metal metal) {
        return switch (metal) {
            case COPPER -> 0xFFFF8A48;
            case TIN -> 0xFFE4E8F0;
            case BRONZE -> 0xFFFFC85A;
            case POOR_BRONZE -> 0xFFE8A850;
        };
    }

    static String units(int quarters) {
        return String.format(Locale.ROOT, "%.2f", quarters / 4F);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BG, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        FurnaceMelt melt = menu.melt();
        KilnScreen.thermometer(graphics, leftPos, topPos, menu.temperature(), needed(melt));
        // tank: pool from the bottom in its colour (a lighter surface row), the charge stacked on top, dark
        int bottom = topPos + KilnScreen.TUBE_BOTTOM, rows = KilnScreen.TUBE_BOTTOM - KilnScreen.TUBE_TOP, x0 = leftPos + TANK_X;
        int pool = Math.round(melt.poolTotal() * rows / (float) FurnaceMelt.CAPACITY);
        int charge = Math.round(melt.chargeTotal() * rows / (float) FurnaceMelt.CAPACITY);
        Metal metal = melt.alloy() != null ? melt.alloy() : melt.pool(Metal.TIN) > 0 ? Metal.TIN : null;
        if (pool > 0 && metal != null) {
            graphics.fill(x0, bottom - pool, x0 + TANK_W, bottom, colour(metal));
            graphics.fill(x0, bottom - pool, x0 + TANK_W, bottom - pool + 1, 0xFFFFF4C0);
        }
        for (int y = 0; y < charge; y++) {
            int row = bottom - pool - 1 - y;
            graphics.fill(x0, row, x0 + TANK_W, row + 1, (y & 1) == 0 ? 0xFF5A4636 : 0xFF6E5844);
        }
        if (menu.lit()) {
            int h = (int) Math.ceil(menu.burn() * 13) + 1;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FLAME, 14, 14, 0, 14 - h, leftPos + FLAME_X, topPos + FLAME_Y + 14 - h, 14, h);
        }
    }

    /** The heat the notch marks: the charge's melting point, or the pool's while nothing is left to melt. */
    private int needed(FurnaceMelt melt) {
        if (menu.meltTemp() > 0) return menu.meltTemp();
        return melt.alloy() != null ? melt.alloy().melt : 0;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(font, title, titleLabelX, titleLabelY, DARK, false);
        FurnaceMelt melt = menu.melt();
        int y = 18;
        for (Metal m : Metal.values()) {
            if (melt.pool(m) == 0) continue;
            graphics.text(font, Component.translatable("gui.mechanical.furnace.molten", Component.translatable("metal.mechanical." + m.getSerializedName()),
                    units(melt.pool(m))), TEXT_X, y, DARK, false);
            y += 10;
        }
        if (melt.chargeTotal() > 0) {
            graphics.text(font, Component.translatable("gui.mechanical.furnace.charge", units(melt.chargeTotal())), TEXT_X, y, DARK, false);
            y += 10;
        }
        int cu = melt.pool(Metal.COPPER) + melt.charge(Metal.COPPER), sn = melt.pool(Metal.TIN) + melt.charge(Metal.TIN);
        if (cu > 0 && sn > 0) {
            String pct = String.format(Locale.ROOT, "%.1f", sn * 100F / (cu + sn));
            Metal mix = melt.chargeTotal() > 0 ? null : melt.alloy();
            Component result = Component.translatable("metal.mechanical." + (mix != null ? mix : CrucibleContents.alloy(cu, sn)).getSerializedName());
            graphics.text(font, Component.translatable("gui.mechanical.furnace.mix", pct, result), TEXT_X, y, DARK, false);
            y += 10;
        }
        if (melt.poolTotal() + melt.chargeTotal() == 0) {
            graphics.text(font, Component.translatable("gui.mechanical.furnace.empty"), TEXT_X, y, DARK, false);
            y += 10;
        }
        Component status;
        int colour;
        Metal alloy = melt.alloy();
        if (alloy == null) {
            status = Component.translatable(menu.open() ? "gui.mechanical.furnace.open" : "gui.mechanical.furnace.closed");
            colour = DARK;
        } else if (melt.settling()) {
            status = Component.translatable("gui.mechanical.furnace.settling");
            colour = GOLD;
        } else if (menu.temperature() < alloy.melt) {
            status = Component.translatable("gui.mechanical.furnace.too_cold", alloy.melt);
            colour = RED;
        } else if (melt.slags()) {
            status = Component.translatable("gui.mechanical.furnace.slag");
            colour = RED;
        } else {
            status = Component.translatable(menu.open() ? "gui.mechanical.furnace.pouring" : "gui.mechanical.furnace.closed");
            colour = menu.open() ? GREEN : DARK;
        }
        graphics.text(font, status, TEXT_X, Math.max(y, 68), colour, false);
        graphics.text(font, Component.translatable("gui.mechanical.furnace.fuel", menu.fuel(), FurnaceTapBlockEntity.FUEL_MAX), FLAME_X + 18, FLAME_Y + 3,
                menu.lit() ? DARK : RED, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        int rx = mouseX - leftPos, ry = mouseY - topPos;
        List<Component> lines = new ArrayList<>();
        if (rx >= KilnScreen.TUBE_X - 4 && rx < KilnScreen.TUBE_X + KilnScreen.TUBE_W + 4 && ry >= KilnScreen.TUBE_TOP - 2 && ry < KilnScreen.TUBE_BOTTOM + 8) {
            int t = menu.temperature(), need = needed(menu.melt());
            lines.add(Component.translatable("gui.mechanical.kiln.temperature", t));
            if (need > 0) lines.add(Component.translatable("gui.mechanical.kiln.needs", need).withStyle(t >= need ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            if (menu.meltTemp() > 0 && t >= menu.meltTemp()) lines.add(Component.translatable("gui.mechanical.furnace.speed",
                    String.format(Locale.ROOT, "%.1f", KilnBlockEntity.speed(t, menu.meltTemp()))).withStyle(ChatFormatting.GOLD));
        } else if (rx >= TANK_X - 1 && rx < TANK_X + TANK_W + 1 && ry >= KilnScreen.TUBE_TOP - 1 && ry <= KilnScreen.TUBE_BOTTOM) {
            FurnaceMelt melt = menu.melt();
            lines.add(Component.translatable("gui.mechanical.furnace.tank", units(melt.poolTotal() + melt.chargeTotal()), FurnaceMelt.CAPACITY / 4));
        } else if (rx >= FLAME_X && rx < FLAME_X + 14 && ry >= FLAME_Y && ry < FLAME_Y + 14) {
            lines.add(Component.translatable(menu.lit() ? "gui.mechanical.kiln.lit" : "gui.mechanical.furnace.unlit"));
        }
        if (lines.isEmpty()) return;
        List<FormattedCharSequence> seq = lines.stream().map(Component::getVisualOrderText).toList();
        graphics.setTooltipForNextFrame(seq, mouseX, mouseY);
    }
}
