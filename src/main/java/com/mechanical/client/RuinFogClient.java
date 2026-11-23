package com.mechanical.client;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.mechanical.Mechanical;
import com.mechanical.worldgen.ElkwightRuins;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * Fog in an Elkwight ruin (worldgen.ElkwightRuins): dense and dark, about 10 blocks of sight, while its boss lives; a thin
 * haze once it is dead. Fades over a few seconds when you walk in or out.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class RuinFogClient {
    private static final Vector3fc FOG = new Vector3f(0.07F, 0.08F, 0.07F);
    private static final float CLEARED = 0.2F;
    private static float target, strength;

    @SubscribeEvent
    static void registerHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(ElkwightRuins.Payload.TYPE, (payload, context) ->
                target = payload.state() == ElkwightRuins.FOG ? 1 : payload.state() == ElkwightRuins.CLEARED ? CLEARED : 0);
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) target = strength = 0;
        else strength = Mth.approach(strength, target, 0.008F);
    }

    /** For screenshots (ElkShots): skip the fade. */
    public static void snap() {
        strength = target;
    }

    /** Called with BloodMoonClient.addLayers (ClientLevel mixin). */
    public static void addLayers(EnvironmentAttributeSystem.Builder builder) {
        builder.addTimeBasedLayer(EnvironmentAttributes.FOG_END_DISTANCE, (d, tick) -> strength > 0 ? Mth.lerp(strength, d, Math.min(d, 10)) : d);
        builder.addTimeBasedLayer(EnvironmentAttributes.FOG_START_DISTANCE, (d, tick) -> strength > 0 ? Mth.lerp(strength, d, 0) : d);
        builder.addTimeBasedLayer(EnvironmentAttributes.FOG_COLOR, (c, tick) -> strength > 0 ? ARGB.srgbLerp(strength * 0.9F, c, FOG) : c);
        builder.addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR, (c, tick) -> strength > 0 ? ARGB.srgbLerp(strength * 0.8F, c, FOG) : c);
    }

    private RuinFogClient() {}
}
