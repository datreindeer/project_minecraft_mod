package com.mechanical.wildlife.temperate;

import com.mechanical.Calendar;
import com.mechanical.wildlife.waterfowl.WadingBird;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Eurasian bittern: a stocky, streaked brown heron of the reed beds, hard to see and harder to put up. It stalks the
 * shallows like the heron, but trusts its camouflage: it lets you come close (low fear in its kind) before it flies.
 * In spring, and at dusk and dawn, the male "booms": a deep, foghorn note heard far across the fen ({@link #BOOM_VOLUME}).
 * Eaten in the old days: a carcass to pluck.
 */
public class Bittern extends WadingBird {
    public static final Kind KIND = new Kind("bittern", 0.32, 4, 10, 120, 300, 1400, 3000, false, true, true, 0.4,
            Bittern::boom, 0.35F);
    /** 16 blocks per unit: heard about 80 blocks off. */
    static final float BOOM_VOLUME = 5.0F;

    public Bittern(EntityType<? extends Bittern> type, Level level) {
        super(type, level, KIND);
    }

    /** The boom: the moody cow's low, deep note, pitched far down (Kind pitch). */
    static SoundEvent boom() {
        return SoundEvents.COW_SOUNDS.get(net.minecraft.world.entity.animal.cow.CowSoundVariants.SoundSet.MOODY).ambientSound().value();
    }

    /** Booms at dusk and dawn (19:00 to 06:59), and at any hour in spring. */
    static boolean boomingTime(long clock) {
        int hour = Calendar.hour(clock);
        return hour >= 19 || hour <= 6 || Calendar.season(clock) == 0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return boomingTime(level().getOverworldClockTime()) ? boom() : null;
    }

    @Override
    protected float getSoundVolume() {
        return BOOM_VOLUME;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }
}
