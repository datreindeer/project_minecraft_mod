package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.Preserving;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Freshness of perishable food (Preserving): Fresh / Stale / Rotting in the tooltip, and a bar like durability's once it is stamped. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class PreservingClient {
    private static long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        float fresh = Preserving.freshness(event.getItemStack(), now());
        if (fresh < 0 || event.getToolTip().isEmpty()) return;
        Component line = fresh > Preserving.STALE ? Component.translatable("tooltip.mechanical.fresh").withStyle(ChatFormatting.GREEN)
                : fresh > Preserving.ROTTING ? Component.translatable("tooltip.mechanical.stale").withStyle(ChatFormatting.YELLOW)
                : Component.translatable("tooltip.mechanical.rotting").withStyle(ChatFormatting.RED);
        event.getToolTip().add(1, line);
    }

    private static final IItemDecorator BAR = (graphics, font, stack, x, y) -> {
        if (!stack.has(Preserving.SPOILS_AT.get())) return false;
        float fresh = Preserving.freshness(stack, now());
        if (fresh < 0) return false;
        int left = x + 2, top = y + 13;
        graphics.fill(RenderPipelines.GUI, left, top, left + 13, top + 2, 0xFF000000);
        // green when fresh, through yellow, to red when rotting (like the durability bar)
        graphics.fill(RenderPipelines.GUI, left, top, left + Math.round(13 * fresh), top + 1, ARGB.opaque(Mth.hsvToRgb(fresh / 3.0F, 1.0F, 1.0F)));
        return false;
    };

    @SubscribeEvent
    static void registerDecorators(RegisterItemDecorationsEvent event) {
        // tags are not loaded yet, so every item gets it; the bar checks the stack
        for (var item : BuiltInRegistries.ITEM) event.register(item, BAR);
    }

    private PreservingClient() {}
}
