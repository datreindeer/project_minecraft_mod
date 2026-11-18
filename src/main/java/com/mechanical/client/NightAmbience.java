package com.mechanical.client;

import java.util.ArrayDeque;

import com.mechanical.Mechanical;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Night unease, client-side only: out in the dark (no fire close by) you now and then hear something out there:
 * footsteps that come closer and stop, a twig snapping, bushes rustling, a distant groan, a scream, rattling bones.
 * Purely sounds, nothing is actually there (the real monsters are in NightDangers).
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class NightAmbience {
    private record Scheduled(int tick, Vec3 pos, SoundEvent sound, float volume, float pitch) {}

    private static final int MIN_GAP = 300, MAX_GAP = 900; // 15-45 s between events
    private static final ArrayDeque<Scheduled> QUEUE = new ArrayDeque<>();
    private static int ticks, next = MIN_GAP;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (mc.level == null || player == null || mc.isPaused()) return;
        ticks++;
        while (!QUEUE.isEmpty() && QUEUE.peek().tick() <= ticks) {
            Scheduled s = QUEUE.poll();
            mc.level.playLocalSound(s.pos().x, s.pos().y, s.pos().z, s.sound(), SoundSource.AMBIENT, s.volume(), s.pitch(), false);
        }
        if (ticks < next) return;
        RandomSource random = player.getRandom();
        next = ticks + MIN_GAP + random.nextInt(MAX_GAP - MIN_GAP);
        if (!mc.level.isDarkOutside() || !mc.level.dimensionType().hasSkyLight()) return;
        BlockPos at = player.blockPosition();
        if (mc.level.getBrightness(LightLayer.BLOCK, at) > 7 || nearCampfire(mc, at)) return;
        scheduleEvent(player, random);
    }

    private static void scheduleEvent(LocalPlayer player, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = 8 + random.nextDouble() * 12;
        Vec3 from = player.position().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
        switch (random.nextInt(6)) {
            case 0 -> { // footsteps coming closer, then stopping
                int steps = 4 + random.nextInt(4);
                Vec3 towards = player.position().subtract(from).normalize().scale(0.7);
                for (int i = 0; i < steps; i++) {
                    QUEUE.add(new Scheduled(ticks + i * 9, from.add(towards.scale(i)), SoundEvents.GRASS_STEP, 0.35F, 0.8F + random.nextFloat() * 0.2F));
                }
            }
            case 1 -> QUEUE.add(new Scheduled(ticks, from, SoundEvents.SCAFFOLDING_BREAK, 0.5F, 0.6F)); // twig snapping
            case 2 -> { // rustling in the bushes
                for (int i = 0; i < 3; i++) QUEUE.add(new Scheduled(ticks + i * 6, from, SoundEvents.SWEET_BERRY_BUSH_BREAK, 0.3F, 0.5F + random.nextFloat() * 0.2F));
            }
            case 3 -> QUEUE.add(new Scheduled(ticks, from, SoundEvents.ZOMBIE_AMBIENT, 0.25F, 0.6F)); // distant groan
            case 4 -> QUEUE.add(new Scheduled(ticks, from.add(from.subtract(player.position())), SoundEvents.FOX_SCREECH, 0.4F, 0.5F)); // far scream
            default -> {
                for (int i = 0; i < 3; i++) QUEUE.add(new Scheduled(ticks + i * 7, from, SoundEvents.SKELETON_STEP, 0.3F, 0.7F));
            }
        }
    }

    private static boolean nearCampfire(Minecraft mc, BlockPos at) {
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-8, -3, -8), at.offset(8, 3, 8))) {
            var state = mc.level.getBlockState(p);
            if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) return true;
        }
        return false;
    }

    private NightAmbience() {}
}
