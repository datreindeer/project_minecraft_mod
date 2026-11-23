package com.mechanical.wildlife.cold;

import com.mechanical.wildlife.FlockBird;
import com.mechanical.wildlife.Hare;
import com.mechanical.wildlife.Ptarmigan;
import com.mechanical.wildlife.coldhunters.Lemming;
import com.mechanical.wildlife.forestbirds.Owl;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Snowy owl: the white owl of the tundra. Unlike the eagle-owl it hunts by day - the arctic summer has no night - low
 * over the snow on silent wings, and drops on lemmings above all, ptarmigan and hares; it sits out the night on the
 * ground or a rock, not in a tree. Everything else (silent flight, the hoot, sitting by its kill) is the eagle-owl's
 * ({@link Owl}). No carcass of its own: feathers and its Snowy Owl Feathers (loot table).
 */
public class SnowyOwl extends Owl {
    static final FlockBird.Kind KIND = new FlockBird.Kind("snowy_owl", 0.36, 2, 6, 200, 500, 300, 900, false, false, false, 0.9,
            () -> SoundEvents.NOTE_BLOCK_FLUTE.value(), 0.6F);

    public SnowyOwl(EntityType<? extends SnowyOwl> type, Level level) {
        super(type, level, KIND);
    }

    @Override
    protected boolean huntingTime(Level level) {
        return level.isBrightOutside();
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        return e instanceof Lemming || e instanceof Ptarmigan || e instanceof Hare && e.isBaby();
    }
}
