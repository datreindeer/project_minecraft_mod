package com.mechanical.wildlife.temperate;

import com.mechanical.wildlife.FlockBird;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Skylark and lapwing: the small birds of open meadow and plain. They feed on the ground in loose little flocks and nest
 * in the grass (Bird Nests with their eggs, generated in the plains). The skylark rises steeply and hangs singing high
 * over the meadow (it sings often while up, {@link #getAmbientSoundInterval}); the lapwing tumbles about low, calling its
 * "pee-wit". Small: a carcass to pluck (feathers and a little meat).
 */
public class Skylark extends FlockBird {
    public static final Kind SKYLARK = new Kind("skylark", 0.38, 10, 20, 300, 700, 600, 1500, false, false, false, 0.9,
            () -> SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, 1.9F);
    public static final Kind LAPWING = new Kind("lapwing", 0.42, 3, 8, 200, 500, 600, 1500, false, false, false, 1.1,
            () -> SoundEvents.PARROT_AMBIENT, 1.8F);

    public Skylark(EntityType<? extends Skylark> type, Level level, Kind kind) {
        super(type, level, kind);
    }

    /** The skylark's song flight: up in the air it sings every few seconds; on the ground it keeps quiet. */
    @Override
    public int getAmbientSoundInterval() {
        return kind == SKYLARK && isFlying() ? 60 : 400;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return kind == SKYLARK && !isFlying() ? null : super.getAmbientSound();
    }
}
