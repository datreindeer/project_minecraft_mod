package com.mechanical.client;

import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import com.mechanical.BloodMoon;
import com.mechanical.Mechanical;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** The blood moon's look: red sky, fog and light, a full red moon, fewer stars. Fades in and out over 5 seconds. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class BloodMoonClient {
    private static final Vector3fc SKY = new Vector3f(0.42F, 0.04F, 0.03F), FOG = new Vector3f(0.28F, 0.03F, 0.02F),
            LIGHT = new Vector3f(1.0F, 0.35F, 0.3F);
    private static boolean active;
    private static float strength;

    @SubscribeEvent
    static void registerHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(BloodMoon.Payload.TYPE, (payload, context) -> active = payload.active());
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) {
            active = false;
            strength = 0;
            return;
        }
        strength = Mth.approach(strength, active ? 1.0F : 0.0F, 0.01F);
    }

    /** 0 = no blood moon, 1 = full blood moon. */
    public static float strength() {
        return strength;
    }

    /** Called from ClientLevelMixin when the level builds its environment attribute layers. */
    public static void addLayers(EnvironmentAttributeSystem.Builder builder) {
        builder.addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR, (c, tick) -> strength > 0 ? ARGB.srgbLerp(strength * 0.85F, c, SKY) : c);
        builder.addTimeBasedLayer(EnvironmentAttributes.FOG_COLOR, (c, tick) -> strength > 0 ? ARGB.srgbLerp(strength * 0.8F, c, FOG) : c);
        builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_COLOR, (c, tick) -> strength > 0 ? ARGB.srgbLerp(strength * 0.7F, c, LIGHT) : c);
        builder.addTimeBasedLayer(EnvironmentAttributes.MOON_PHASE, (phase, tick) -> strength > 0 ? MoonPhase.FULL_MOON : phase);
        builder.addTimeBasedLayer(EnvironmentAttributes.STAR_BRIGHTNESS, (b, tick) -> b * (1 - 0.7F * strength));
    }

    /** Colour the moon is drawn with: blood red at full strength. */
    public static Vector4f moonTint(Vector4f colour) {
        if (strength <= 0) return colour;
        return new Vector4f(colour.x(), Mth.lerp(strength, colour.y(), 0.28F), Mth.lerp(strength, colour.z(), 0.22F), colour.w());
    }

    private BloodMoonClient() {}
}
