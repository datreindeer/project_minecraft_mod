package com.mechanical.client;

import java.util.Map;
import java.util.WeakHashMap;

import com.mechanical.FlintAndSteelItem;
import com.mechanical.Mechanical;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The moment the flint meets the steel ({@link FlintAndSteelItem#IMPACT} of a strike), on every client for every player near
 * it: a spray of sparks from where the hands meet, flying forward and down, and the scrape. Done here rather than by the
 * server so it lands on the frame the animation strikes (the server only knows the use, at the start of the swing).
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class FlintSparks {
    private static final Map<LivingEntity, Float> LAST = new WeakHashMap<>();

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) return;
        for (Player player : mc.level.players()) {
            var swing = player.getCurrentSwing();
            if (swing == null || !(player.getItemInHand(swing.hand()).getItem() instanceof FlintAndSteelItem) || player.distanceToSqr(mc.gameRenderer.mainCamera().position()) > 48 * 48) {
                LAST.remove(player);
                continue;
            }
            // two ticks ahead (measured in FlintShots): spawned now, the sparks show up as the strike reaches the steel
            float now = player.getSwingAnimation(1.0F) + 2.0F / Math.max(1, swing.durationTicks()), before = LAST.getOrDefault(player, -1.0F);
            LAST.put(player, now);
            if (before < FlintAndSteelItem.IMPACT && now >= FlintAndSteelItem.IMPACT) strike(mc, player, swing.hand() == net.minecraft.world.InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite());
        }
    }

    private static void strike(Minecraft mc, Player player, HumanoidArm steelArm) {
        RandomSource r = player.getRandom();
        boolean firstPerson = player == mc.player && mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == player;
        Vec3 at, forward;
        if (firstPerson) { // where the first-person hands meet: low in front of the eye, a little to the steel's side
            Vec3 look = player.getViewVector(1.0F), up = player.getUpVector(1.0F), right = look.cross(up).normalize();
            forward = look;
            at = player.getEyePosition(1.0F).add(look.scale(1.0)).add(up.scale(-0.3)).add(right.scale((steelArm == HumanoidArm.RIGHT ? 1 : -1) * 0.18));
        } else { // in front of the chest, where poseStrike brings the flint down across the steel
            float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
            forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
            Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
            at = player.position().add(0.0, player.getBbHeight() * (player.isCrouching() ? 0.5 : 0.58), 0.0).add(forward.scale(0.42))
                    .add(right.scale((steelArm == HumanoidArm.RIGHT ? 1 : -1) * 0.04));
        }
        int n = 9 + r.nextInt(7);
        for (int i = 0; i < n; i++) {
            double speed = 0.06 + r.nextDouble() * 0.12;
            Vec3 v = forward.scale(speed).add((r.nextDouble() - 0.5) * 0.14, -0.02 + r.nextDouble() * 0.1, (r.nextDouble() - 0.5) * 0.14);
            mc.level.addParticle(Mechanical.FLINT_SPARK.get(), at.x, at.y, at.z, v.x, v.y, v.z);
        }
        mc.level.playLocalSound(at.x, at.y, at.z, SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.9F, 0.9F + r.nextFloat() * 0.3F, false);
    }

    private FlintSparks() {}
}
