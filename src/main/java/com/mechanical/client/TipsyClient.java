package com.mechanical.client;

import com.mechanical.Kitchen;
import com.mechanical.Mechanical;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Tipsy (ale, cider; {@link Kitchen#drinkAlcohol}): nothing to see after a drink or two; from the third in a row
 * ({@link Kitchen#TIPSY_SWAY_FROM}) the view leans slowly from side to side, a little more at the fourth, fading out over
 * the effect's last 10 s. Much softer than Nausea: no warping, only a slow roll of a degree or two.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class TipsyClient {
    @SubscribeEvent
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        var player = Minecraft.getInstance().player;
        MobEffectInstance e = player == null ? null : player.getEffect(Kitchen.TIPSY);
        if (e == null || e.getAmplifier() < Kitchen.TIPSY_SWAY_FROM) return;
        float k = (e.getAmplifier() - Kitchen.TIPSY_SWAY_FROM + 1) * Math.min(1, e.getDuration() / 200F); // 1 at the third drink, 2 at the fourth
        float t = (float) ((player.tickCount + event.getPartialTick()) * 0.025);
        event.setRoll(event.getRoll() + Mth.sin(t) * 1.1F * k);
        event.setPitch(event.getPitch() + Mth.sin(t * 1.7F + 1) * 0.35F * k);
    }

    private TipsyClient() {}
}
