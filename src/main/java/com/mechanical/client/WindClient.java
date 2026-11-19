package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.Wind;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Windy nights on the client: gusts you can hear, and ash and bits of leaf streaming past on the wind outdoors. */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class WindClient {
    private static boolean windy;
    /** Wind direction for the night, so everything blows the same way. */
    private static double dirX = 1, dirZ = 0.3;

    @SubscribeEvent
    static void registerHandlers(RegisterClientPayloadHandlersEvent event) {
        event.register(Wind.Payload.TYPE, (payload, context) -> {
            windy = payload.windy();
            if (windy) {
                double a = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getRandom().nextDouble() * Math.PI * 2 : 0;
                dirX = Math.cos(a);
                dirZ = Math.sin(a);
            }
        });
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            windy = false;
            return;
        }
        if (!windy || mc.isPaused()) return;
        var player = mc.player;
        RandomSource random = mc.level.getRandom();
        boolean outside = mc.level.canSeeSky(player.blockPosition().above());
        // a gust now and then (quieter indoors)
        if (random.nextInt(140) == 0) {
            mc.level.playLocalSound(player.getX() - dirX * 6, player.getEyeY() + 2, player.getZ() - dirZ * 6, SoundEvents.BREEZE_IDLE_AIR,
                    SoundSource.WEATHER, outside ? 0.5F : 0.15F, 0.4F + random.nextFloat() * 0.2F, false);
        }
        if (!outside) return;
        for (int i = 0; i < 3; i++) {
            double x = player.getX() - dirX * 10 + random.nextGaussian() * 8, y = player.getY() + random.nextDouble() * 6 - 1;
            double z = player.getZ() - dirZ * 10 + random.nextGaussian() * 8;
            double speed = 0.35 + random.nextDouble() * 0.25;
            mc.level.addParticle(random.nextInt(3) == 0 ? ParticleTypes.WHITE_ASH : ParticleTypes.ASH, x, y, z, dirX * speed, -0.01, dirZ * speed);
        }
    }

    private WindClient() {}
}
