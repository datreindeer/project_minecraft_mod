package com.mechanical.wildlife.river;

import com.mechanical.wildlife.WildAnimal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/**
 * A wild animal at home both on the bank and in the water (beaver, otter): paths through water like the axolotl, swims
 * under the surface at its own speed ({@link #swimSpeed}) and holds its breath for minutes.
 */
public abstract class SemiAquatic extends WildAnimal {
    protected SemiAquatic(EntityType<? extends SemiAquatic> type, Level level, float swimSpeed) {
        super(type, level);
        setPathfindingMalus(PathType.WATER, 0.0F);
        // speed attribute x modifier: on land 1, in water swimSpeed
        moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, swimSpeed, 1.0F, false);
        lookControl = new SmoothSwimmingLookControl(this, 20);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.removeAllGoals(g -> g instanceof FloatGoal); // dives instead of bobbing at the surface
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    protected void travelInWater(Vec3 input, double baseGravity, boolean isFalling, double oldY) {
        moveRelative(getSpeed(), input);
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.9));
    }

    @Override
    public int getMaxAirSupply() {
        return 4800;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    /** Swimming (for models): in water and not standing on the bottom. */
    public boolean isSwimmingNow() {
        return isInWater() && !onGround();
    }
}
