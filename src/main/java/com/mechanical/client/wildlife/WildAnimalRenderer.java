package com.mechanical.client.wildlife;

import com.mechanical.wildlife.Pheasant;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.coast.Pinniped;
import com.mechanical.wildlife.river.SemiAquatic;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;

/**
 * One renderer for all wildlife: adult and baby models are passed in, the texture comes from the entity.
 * Baby models are built at baby size; {@code babyScale} scales them further (or the adult model, when an animal has no
 * baby model of its own, like the hare).
 */
public class WildAnimalRenderer<T extends WildAnimal> extends MobRenderer<T, WildAnimalRenderState, EntityModel<WildAnimalRenderState>> {
    private final EntityModel<WildAnimalRenderState> adult, baby;
    private final float babyScale;
    /** Swimming: water up to this share of the body height, nose-up tilt (degrees), walk-cycle steps per tick while paddling. */
    public static final float LAND_DEPTH = 0.6F, SWIMMER_DEPTH = 0.72F;
    private static final float SWIM_TILT = 8.0F, PADDLE_RATE = 0.6F;

    public WildAnimalRenderer(EntityRendererProvider.Context context, EntityModel<WildAnimalRenderState> adult,
            EntityModel<WildAnimalRenderState> baby, float babyScale, float shadow) {
        super(context, adult, shadow);
        this.adult = adult;
        this.baby = baby;
        this.babyScale = babyScale;
        addLayer(new StuckArrowLayer(this, context));
    }

    @Override
    public WildAnimalRenderState createRenderState() {
        return new WildAnimalRenderState();
    }

    @Override
    public void extractRenderState(T entity, WildAnimalRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.texture = entity.texture();
        state.grazeAmount = entity.grazeAmount(partialTicks);
        state.showsHorns = entity.showsHorns();
        state.charging = entity.isCharging();
        state.dozing = entity.isDozing();
        state.arrowCount = entity.getArrowCount();
        state.entityId = entity.getId();
        if (entity instanceof com.mechanical.wildlife.Predator predator) {
            state.crouching = predator.isCrouching();
            state.leaping = predator.isLeaping();
            state.calling = predator.callAmount();
        }
        if (entity instanceof com.mechanical.wildlife.GiantDeer deer) {
            state.sweep = deer.sweepAmount(partialTicks);
            state.stand = deer.stompAmount(partialTicks);
        }
        if (entity instanceof Pheasant pheasant) {
            state.flying = !pheasant.onGround();
            state.flap = pheasant.flapPhase(partialTicks);
        }
        WildAnimal.WaterHabit habit = entity.waterHabit();
        state.swimming = habit != WildAnimal.WaterHabit.OWN && entity.isSwimmingNow() && entity.isAlive() && !state.dozing;
        state.swimSink = 0.0F;
        // otters, beavers, seals and walruses have their own depth (they dive) and strokes
        if (state.swimming && !(entity instanceof SemiAquatic) && !(entity instanceof Pinniped)) {
            swimPose(entity, state, habit == WildAnimal.WaterHabit.SWIMMER ? SWIMMER_DEPTH : LAND_DEPTH);
        }
    }

    /**
     * A quadruped swimming: the float goal only keeps the feet under, so the body sinks until the water reaches
     * {@code depth} of its height (measured from the surface so the float bobbing doesn't show), head up, the walk cycle a
     * steady paddle. Also the tamed horse's (TameHorseRenderer).
     */
    static void swimPose(net.minecraft.world.entity.LivingEntity entity, WildAnimalRenderState state, float depth) {
        double surface = entity.getY() + entity.getFluidHeight(FluidTags.WATER);
        float h = entity.getBbHeight();
        state.swimSink = Mth.clamp((float) (state.y + depth * h - surface), 0.0F, 0.7F * h);
        state.xRot = -20.0F; // head up, muzzle clear of the water
        state.walkAnimationPos = state.ageInTicks * PADDLE_RATE;
        state.walkAnimationSpeed = Math.max(state.walkAnimationSpeed, 0.7F);
    }

    /** The swimming body sunk to {@code swimSink} and tilted nose-up (after the body rotation). */
    static void swimRotations(WildAnimalRenderState state, PoseStack poseStack, float entityScale) {
        if (state.swimSink > 0.0F) {
            poseStack.translate(0.0F, -state.swimSink / entityScale, 0.0F);
            poseStack.rotateDegrees(Axis.XP, SWIM_TILT); // chest down, rump lower still: nose up
        }
    }

    @Override
    protected void setupRotations(WildAnimalRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        swimRotations(state, poseStack, entityScale);
    }

    @Override
    public void submit(WildAnimalRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        model = state.isBaby ? baby : adult;
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    protected void scale(WildAnimalRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        if (state.isBaby && babyScale != 1.0F) poseStack.scale(babyScale, babyScale, babyScale);
    }

    @Override
    public Identifier getTextureLocation(WildAnimalRenderState state) {
        return state.texture;
    }
}
