package com.mechanical.client;

import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.mechanical.Mechanical;
import com.mechanical.Smoke;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** A smoky room (see Smoke): grey fog closing in and smoke drifting in the air, thicker the more smoke there is. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class SmokeClient {
    private static final Vector3fc FOG = new Vector3f(0.32F, 0.30F, 0.28F);
    private static float target, strength;

    @SubscribeEvent
    static void registerHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(Smoke.Payload.TYPE, (payload, context) -> target = payload.smoke());
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            target = strength = 0;
            return;
        }
        strength = Mth.approach(strength, target, 0.01F); // smooth steps between the once-a-second updates
        if (strength < 0.1F || mc.isPaused()) return;
        RandomSource random = mc.level.getRandom();
        for (int i = 0; i < strength * 4; i++) {
            double x = mc.player.getX() + random.nextGaussian() * 3, y = mc.player.getEyeY() + random.nextGaussian() * 1.5, z = mc.player.getZ() + random.nextGaussian() * 3;
            if (!mc.level.getBlockState(BlockPos.containing(x, y, z)).isAir()) continue;
            mc.level.addParticle(random.nextInt(4) == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, x, y, z, 0, 0.005, 0);
        }
    }

    /** Called with BloodMoonClient.addLayers (ClientLevel mixin). Fog end falls to 4 blocks at full smoke. */
    public static void addLayers(EnvironmentAttributeSystem.Builder builder) {
        builder.addTimeBasedLayer(EnvironmentAttributes.FOG_END_DISTANCE, (d, tick) -> strength > 0 ? Mth.lerp(Math.min(1, strength * 4), d, Math.min(d, 4 + 60 * (1 - strength) * (1 - strength))) : d);
        builder.addTimeBasedLayer(EnvironmentAttributes.FOG_START_DISTANCE, (d, tick) -> strength > 0 ? Mth.lerp(Math.min(1, strength * 4), d, 0) : d);
        builder.addTimeBasedLayer(EnvironmentAttributes.FOG_COLOR, (c, tick) -> strength > 0 ? ARGB.srgbLerp(strength * 0.8F, c, FOG) : c);
    }

    private SmokeClient() {}
}
