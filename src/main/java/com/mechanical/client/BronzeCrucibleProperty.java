package com.mechanical.client;

import com.mechanical.CrucibleItem;
import com.mechanical.Mechanical;
import com.mechanical.Metal;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;

/** Item model condition {@code mechanical:bronze_crucible}: the crucible holds (Poor) Bronze, so it shows bronze, not copper. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public record BronzeCrucibleProperty() implements ConditionalItemModelProperty {
    public static final MapCodec<BronzeCrucibleProperty> MAP_CODEC = MapCodec.unit(new BronzeCrucibleProperty());

    @Override
    public boolean get(ItemStack stack, ClientLevel level, LivingEntity owner, int seed, ItemDisplayContext context) {
        Metal metal = CrucibleItem.contents(stack).alloy();
        return metal != null && metal.isAlloy();
    }

    @Override
    public MapCodec<BronzeCrucibleProperty> type() {
        return MAP_CODEC;
    }

    @SubscribeEvent
    static void register(RegisterConditionalItemModelPropertyEvent event) {
        event.register(Identifier.fromNamespaceAndPath(Mechanical.MODID, "bronze_crucible"), MAP_CODEC);
    }
}
