package com.mechanical.wildlife.desert;

import com.mechanical.wildlife.forestbirds.Vulture;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Condor: the giant black carrion bird of the badlands, with a white ruff and white wing panels and a bare red-grey head.
 * A vulture ({@link Vulture}) on a grander scale: it soars higher than any other bird ({@link #KIND}: 24-44 blocks up) and
 * spots a carcass from {@link #SIGHT} blocks; the condors then wheel over it in a slow, wide column that shows from far
 * off where there is a carcass, lure the night hunters to it, and come down to strip it to bones. No carcass of its own:
 * feathers and Condor Feathers (loot table).
 */
public class Condor extends Vulture {
    public static final Kind KIND = new Kind("condor", 0.42, 24, 44, 1600, 3000, 400, 1200, false, false, false, 1.0,
            () -> SoundEvents.CAT_HISS_BABY.value(), 0.35F);
    public static final double SIGHT = 128;

    public Condor(EntityType<? extends Condor> type, Level level) {
        super(type, level, KIND);
    }

    @Override
    protected double sight() {
        return SIGHT;
    }

    /** Wider circles than the vulture's. */
    @Override
    protected double radius() {
        return 10 + getId() % 8;
    }
}
