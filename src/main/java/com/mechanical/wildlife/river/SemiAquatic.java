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
import net.minecraft.world.phys.Vec3;

/**
 * A wild animal at home both on the bank and in the water (beaver, otter): paths through water like the axolotl, swims
 * under the surface at its own speed ({@link #swimSpeed}) and holds its breath for minutes.
 */
public abstract class SemiAquatic extends WildAnimal {
    protected SemiAquatic(EntityType<? extends SemiAquatic> type, Level level, float swimSpeed) {
        super(type, level);
        // speed attribute x modifier: on land 1, in water swimSpeed
        moveControl = new SmoothSwimmingMoveControl<>(this, 85, 10, swimSpeed, 1.0F, false);
        lookControl = new SmoothSwimmingLookControl(this, 20);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.SWIMMER;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // swims at the surface (so it can haul out onto the bank), except while it means to dive
        goalSelector.removeAllGoals(g -> g instanceof FloatGoal);
        goalSelector.addGoal(0, new FloatGoal(this) {
            @Override
            public boolean canUse() {
                return !diving() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !diving() && super.canContinueToUse();
            }
        });
    }

    /** Under water on purpose (the otter fishing): no floating up. */
    protected boolean diving() {
        return false;
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
}
