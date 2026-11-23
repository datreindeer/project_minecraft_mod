package com.mechanical.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.entity.WalkAnimationState;

/** Lets a wagon's second horse stride in step with the lead horse (WagonEntity#keepPair): the walk state's private fields. */
@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {
    @Accessor("speedOld")
    float mechanical$speedOld();

    @Accessor("speedOld")
    void mechanical$setSpeedOld(float value);

    @Accessor("position")
    float mechanical$position();

    @Accessor("position")
    void mechanical$setPosition(float value);
}
