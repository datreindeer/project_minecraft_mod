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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The Smelting Furnace's status: the kiln's thermometer (white notch at the melting point the charge needs), a tank
 * showing the molten pool in the metal's colour with the unmelted charge above it (darker), the fuel (flame, Charcoal,
 * count) and, on the right, rows with an item icon: the molten metal (alloy, amount, quality: Good / Poor bronze, pure
 * copper or slag), each kind of ore / ingots still melting, then the copper : tin mix and what the tap is doing.
 * Background: textures/gui/container/smelting_furnace.png (gen_smelter_textures.py).
 */
public class FurnaceScreen extends AbstractContainerScreen<FurnaceMenu> {
    private static final Identifier BG = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/gui/container/smelting_furnace.png");
    private static final Identifier FLAME = Identifier.withDefaultNamespace("container/furnace/lit_progress");
    /** Inside of the tank: x 40..51, rows 18..61 (= {@link FurnaceMelt#CAPACITY} quarters). */
    public static final int TANK_X = 40, TANK_W = 12, FLAME_X = 22, FLAME_Y = 70, FUEL_X = 38, TEXT_X = 62, ROW_H = 21;
    private static final int DARK = 0xFF404040, GREEN = 0xFF2E7D32, RED = 0xFFB71C1C, GOLD = 0xFF8A6A00, GREY = 0xFF6A6A6A;

    // built when the screen opens (never in a static initializer: items must be registered first)
    private final ItemStack charcoal = new ItemStack(Items.CHARCOAL);

    public FurnaceScreen(FurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 220, 112);
    }

    /** Molten colour of a metal: the metal table's, as in the High Furnace (also the channel strip's tint, client/MoltenRenderer). */
    public static int colour(Metal metal) {
        return metal.molten().color;
    }

    static String units(int quarters) {
        return String.format(Locale.ROOT, "%.2f", quarters / 4F);
    }

    private static Component metalName(Metal m) {
        return Component.translatable("metal.mechanical." + m.getSerializedName());
    }

    /** Its crushed ore while melting (an alloy: its ingot), its ingot once molten. */
    private static ItemStack icon(Metal m, boolean ore) {
        net.minecraft.world.item.Item crushed = ore ? m.molten().crucibleOre(1) : null;
        return crushed != null ? new ItemStack(crushed) : m.molten().ingot();
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
        graphics.item(charcoal, leftPos + FUEL_X, topPos + FLAME_Y - 1);
        // the icon of each row on the right (the text is drawn in extractLabels)
        int y = topPos + 18;
        if (metal != null && melt.poolTotal() > 0) {
            graphics.item(icon(metal, false), leftPos + TEXT_X, y + 2);
            y += ROW_H;
        }
        for (Metal m : Metal.values()) {
            if (melt.charge(m) == 0) continue;
            graphics.item(icon(m, true), leftPos + TEXT_X, y + 2);
            y += ROW_H;
        }
    }

    /** The heat the notch marks: the charge's melting point, or the pool's while nothing is left to melt. */
    private int needed(FurnaceMelt melt) {
        if (menu.meltTemp() > 0) return menu.meltTemp();
        return melt.alloy() != null ? melt.alloy().melt() : 0;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        graphics.text(font, title, titleLabelX, titleLabelY, DARK, false);
        FurnaceMelt melt = menu.melt();
        int x = TEXT_X + 19, y = 18;
        Metal alloy = melt.alloy();
        // the molten metal: what it pours as, how much, and its quality
        if (melt.poolTotal() > 0 && (alloy != null || melt.pool(Metal.TIN) > 0)) {
            Component quality;
            int colour;
            if (alloy == null) {
                quality = Component.translatable("gui.mechanical.furnace.quality.needs_copper");
                colour = GREY;
            } else if (melt.settling()) {
                quality = Component.translatable("gui.mechanical.furnace.quality.mixing");
                colour = GOLD;
            } else if (melt.slags()) {
                quality = Component.translatable("gui.mechanical.furnace.quality.slag");
                colour = RED;
            } else {
                quality = Component.translatable("gui.mechanical.furnace.quality." + switch (alloy) {
                    case BRONZE -> "good";
                    case POOR_BRONZE -> "poor";
                    default -> "pure";
                });
                colour = alloy == Metal.POOR_BRONZE ? GOLD : GREEN;
            }
            graphics.text(font, Component.translatable("gui.mechanical.furnace.molten", metalName(alloy != null ? alloy : Metal.TIN)), x, y + 1, DARK, false);
            int shown = alloy != null && !melt.settling() ? melt.pourable() : melt.poolTotal();
            graphics.text(font, Component.translatable("gui.mechanical.furnace.amount", units(shown), quality), x, y + 11, colour, false);
            y += ROW_H;
        }
        // what is still melting, per kind
        for (Metal m : Metal.values()) {
            if (melt.charge(m) == 0) continue;
            graphics.text(font, Component.translatable("gui.mechanical.furnace.unmelted", metalName(m)), x, y + 1, DARK, false);
            graphics.text(font, Component.translatable("gui.mechanical.furnace.units", units(melt.charge(m))), x, y + 11, GREY, false);
            y += ROW_H;
        }
        int cu = melt.pool(Metal.COPPER) + melt.charge(Metal.COPPER), sn = melt.pool(Metal.TIN) + melt.charge(Metal.TIN);
        if (cu > 0 && sn > 0) {
            String pct = String.format(Locale.ROOT, "%.1f", sn * 100F / (cu + sn));
            Metal mix = melt.chargeTotal() > 0 ? null : melt.alloy();
            graphics.text(font, Component.translatable("gui.mechanical.furnace.mix", pct, metalName(mix != null ? mix : CrucibleContents.alloy(cu, sn))),
                    TEXT_X, y + 1, DARK, false);
            y += 11;
        }
        if (melt.poolTotal() + melt.chargeTotal() == 0) {
            graphics.text(font, Component.translatable("gui.mechanical.furnace.empty"), TEXT_X, y + 1, DARK, false);
            y += 11;
        }
        Component status;
        int colour;
        if (alloy == null) {
            status = Component.translatable(menu.open() ? "gui.mechanical.furnace.open" : "gui.mechanical.furnace.closed");
            colour = DARK;
        } else if (melt.settling()) {
            status = Component.translatable("gui.mechanical.furnace.settling");
            colour = GOLD;
        } else if (menu.temperature() < alloy.melt()) {
            status = Component.translatable("gui.mechanical.furnace.too_cold", alloy.melt());
            colour = RED;
        } else if (melt.slags()) {
            status = Component.translatable("gui.mechanical.furnace.slag");
            colour = RED;
        } else {
            status = Component.translatable(menu.open() ? "gui.mechanical.furnace.pouring" : "gui.mechanical.furnace.closed");
            colour = menu.open() ? GREEN : DARK;
        }
        graphics.text(font, status, TEXT_X, Math.max(y + 3, 98), colour, false);
        graphics.text(font, menu.fuel() + "/" + FurnaceTapBlockEntity.FUEL_MAX, FLAME_X, FLAME_Y + 18, menu.lit() ? DARK : RED, false);
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
        } else if (rx >= FLAME_X && rx < FUEL_X + 16 && ry >= FLAME_Y - 1 && ry < FLAME_Y + 27) {
            lines.add(Component.translatable("gui.mechanical.furnace.fuel", menu.fuel(), FurnaceTapBlockEntity.FUEL_MAX));
            lines.add(Component.translatable(menu.lit() ? "gui.mechanical.kiln.lit" : "gui.mechanical.furnace.unlit").withStyle(ChatFormatting.GRAY));
        }
        if (lines.isEmpty()) return;
        List<FormattedCharSequence> seq = lines.stream().map(Component::getVisualOrderText).toList();
        graphics.setTooltipForNextFrame(seq, mouseX, mouseY);
    }
}
