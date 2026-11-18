package com.mechanical.client;

import com.mechanical.AncientOak;
import com.mechanical.Mechanical;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Mythic item names shimmer in the tooltip: the green-to-gold fade slowly slides along the name. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class MythicNames {
    private static final long CYCLE_MS = 2400;

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        var id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        String name = id.getNamespace().equals(Mechanical.MODID) ? AncientOak.MYTHIC_NAMES.get(id.getPath()) : null;
        if (name == null || event.getToolTip().isEmpty() || event.getItemStack().has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) return;
        float phase = (Util.getMillis() % CYCLE_MS) / (float) CYCLE_MS;
        event.getToolTip().set(0, AncientOak.mythicName(name, phase));
    }

    private MythicNames() {}
}
