package com.mechanical.wildlife.forestbirds;

import com.mechanical.wildlife.FlockBird;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Great bustard: the heaviest flying bird of the open plains, wary beyond any other. Walks the grass in small droves and
 * notices you from far off; rather than fly it runs, long-legged and fast, and only takes to the air (heavily, low and
 * not far) when you get close, when it is hit, or when the drove is shot at. Leaves a carcass with a lot of meat
 * (Birds), nests on the ground in the grass (BirdNestBlock) with a big egg.
 */
public class Bustard extends FlockBird {
    static final Kind KIND = new Kind("great_bustard", 0.4, 3, 7, 80, 200, 900, 2000, false, false, false, 1.4,
            () -> SoundEvents.CHICKEN_AMBIENT_BABY.value(), 0.3F);
    /** Closer than this it flies; further off it runs. */
    static final double FLUSH = 6.0;
    static final double RUN_SPEED = 1.9;
    private int runCooldown;

    public Bustard(EntityType<? extends Bustard> type, Level level) {
        super(type, level, KIND);
    }

    @Override
    public void scare(Vec3 from) {
        if (!isFlying() && onGround() && hurtTime == 0 && from != null && from.distanceTo(position()) > FLUSH) {
            runFrom(from);
            return;
        }
        super.scare(from);
    }

    private void runFrom(Vec3 from) {
        if (runCooldown > tickCount && !getNavigation().isDone()) return;
        runCooldown = tickCount + 20;
        wake();
        Vec3 to = DefaultRandomPos.getPosAway(this, 16, 6, from);
        if (to != null) getNavigation().moveTo(to.x, to.y, to.z, RUN_SPEED);
    }

    @Override
    public int getAmbientSoundInterval() {
        return 600; // mostly silent
    }
}
