package com.mechanical.wildlife.waterfowl;

import com.mechanical.Sanity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * Bone Flute: a hollow swan or crane wing bone with finger holes (like the oldest flutes ever found). Right-click plays a
 * short phrase on a five-note scale. Music lifts the spirits: every player within {@link #RANGE} blocks (the player too)
 * gains {@link #SANITY} sanity, but only up to {@link #SANITY_CAP} (it eases unease, it doesn't replace rest and fire).
 */
public class BoneFluteItem extends Item {
    static final double RANGE = 12;
    static final float SANITY = 3, SANITY_CAP = 75;
    static final int COOLDOWN = 60;
    /** A minor pentatonic scale as note-block pitches (2^(n/12)). */
    private static final int[] SCALE = {0, 3, 5, 7, 10, 12};

    public BoneFluteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            var random = server.getRandom();
            float pitch = (float) Math.pow(2.0, (SCALE[random.nextInt(SCALE.length)] - 6) / 12.0);
            server.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.0F, pitch);
            server.sendParticles(ParticleTypes.NOTE, player.getX(), player.getEyeY() + 0.4, player.getZ(), 3, 0.3, 0.2, 0.3, random.nextInt(25) / 24.0);
            server.gameEvent(player, GameEvent.INSTRUMENT_PLAY, player.position());
            for (Player near : server.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(RANGE))) {
                float now = Sanity.of(near);
                if (now < SANITY_CAP) Sanity.set(near, Math.min(SANITY_CAP, now + SANITY));
            }
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        return InteractionResult.SUCCESS;
    }
}
