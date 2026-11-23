package com.mechanical.wildlife.temperate;

import com.mechanical.wildlife.FlockBird;
import com.mechanical.wildlife.forestbirds.Owl;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Tawny owl: the common brown owl of the broadleaf woods, smaller than the eagle-owl and without its ear tufts, a warm
 * russet with dark streaks. Everything else is the eagle-owl's ({@link Owl}): it dozes by day, hunts small animals at
 * night on silent wings, and its hooting carries (a higher, wavering "hoo-hoo-hooo"). Drops what the eagle-owl does.
 */
public class TawnyOwl extends Owl {
    static final FlockBird.Kind KIND = new FlockBird.Kind("tawny_owl", 0.34, 3, 7, 160, 400, 300, 900, false, false, true, 0.8,
            () -> SoundEvents.NOTE_BLOCK_FLUTE.value(), 0.75F);

    public TawnyOwl(EntityType<? extends TawnyOwl> type, Level level) {
        super(type, level, KIND);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.25F;
    }
}
