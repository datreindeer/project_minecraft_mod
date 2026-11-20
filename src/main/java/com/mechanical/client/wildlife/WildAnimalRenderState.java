package com.mechanical.client.wildlife;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/**
 * What wildlife models get, on top of the vanilla living state (walkAnimationPos/Speed, ageInTicks, head yRot/xRot ...).
 * Models should also call {@code CarcassRenderer.isCarcass(state)} and let the limbs sag when it is true, and
 * {@code CarcassRenderer.isHung(state)} (head down on a Hanging Rack, model -z is world down): {@code CarcassRenderer.hangLegs}
 * for the legs, neck and head hanging straight down in line with the body, ears and tail drooping.
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
    /** Arrows stuck in the body (ArrowWounds; synced through vanilla's arrow count). */
    public int arrowCount;
    /** Entity id: seeds where the stuck arrows sit, so they stay put. */
    public int entityId;
    /** Predators: crouched stalking, 0..1 head thrown back howling/roaring, in the air pouncing. */
    public boolean crouching, leaping;
    public float calling;
    /** Giant Deer: 0..1 progress of the antler sweep (its rear-up stomp uses {@link #stand}). */
    public float sweep;
    /** Tamed horse (and Giant Deer stomp): 0..1 reared up on the hind legs, and the Hide Saddle Blanket on. */
    public float stand;
    public boolean saddled;
    /** Tamed horse: satchels on its flanks (0-2), and the horse armour's tint (0 = none). */
    public int satchels;
    public int armorColor;
    /**
     * Swimming (WildAnimal.isSwimmingNow; not birds or snakes). For the land animals and the big water animals (moose,
     * buffalo, tapir) the renderer already sinks the body ({@code swimSink}, blocks), tilts it nose-up, lifts the head
     * (xRot) and turns the walk cycle into a steady paddle, so every quadruped model swims with no code of its own; a
     * model only checks this flag to swim in its own way.
     */
    public boolean swimming;
    public float swimSink;
}
