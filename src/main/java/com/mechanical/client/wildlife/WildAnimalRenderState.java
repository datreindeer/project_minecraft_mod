package com.mechanical.client.wildlife;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * What wildlife models get, on top of the vanilla living state (walkAnimationPos/Speed, ageInTicks, head yRot/xRot ...).
 * Models should also call {@code CarcassRenderer.isCarcass(state)} and let the limbs sag when it is true.
 */
public class WildAnimalRenderState extends LivingEntityRenderState {
    public Identifier texture;
    /** 0..1: head lowered to the ground for grazing. */
    public float grazeAmount;
    /** Antlers / tusks visible. */
    public boolean showsHorns;
    /** Head down, charging a target (boar, stag). */
    public boolean charging;
    /** Pheasant: in the air, and a wing-flap phase in radians. */
    public boolean flying;
    public float flap;
    /** Asleep, lying down: legs folded under the body, body on the ground, head resting low. */
    public boolean dozing;
}
