package com.mechanical.client.wildlife;

import com.google.common.reflect.TypeToken;
import com.mechanical.client.CarcassRenderer;
import com.mechanical.wildlife.Squirrel;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

/**
 * Red squirrel: slim body, round head with tufted ears, long hind feet, and a bushy tail in three segments curling up
 * over the back. Bounds when it runs, sits up nibbling when it grazes, and clings upright to a trunk when climbing
 * (head first on the way down). Texture: textures/entity/wildlife/squirrel.png, painted by
 * scripts/gen_small_models.py (with the geometry, SmallGeometry).
 */
public class SquirrelModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "squirrel"), "main");
    /** 0 on the ground, 1 clinging head up, 2 coming down head first. */
    public static final ContextKey<Integer> CLIMB = new ContextKey<>(Identifier.fromNamespaceAndPath("mechanical", "squirrel_climb"));
    private static final float TAIL_X = 1.0F, TAIL_MID_X = 0.9F, TAIL_TIP_X = 0.9F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightEar;
    private final ModelPart leftEar;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightHaunch;
    private final ModelPart leftHaunch;
    private final ModelPart rightHindFoot;
    private final ModelPart leftHindFoot;
    private final ModelPart tail;
    private final ModelPart tailMid;
    private final ModelPart tailTip;

    public SquirrelModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.rightEar = head.getChild("right_ear");
        this.leftEar = head.getChild("left_ear");
        this.rightFrontLeg = body.getChild("right_front_leg");
        this.leftFrontLeg = body.getChild("left_front_leg");
        this.rightHaunch = body.getChild("right_haunch");
        this.leftHaunch = body.getChild("left_haunch");
        this.rightHindFoot = rightHaunch.getChild("right_hind_foot");
        this.leftHindFoot = leftHaunch.getChild("left_hind_foot");
        this.tail = body.getChild("tail");
        this.tailMid = tail.getChild("tail_mid");
        this.tailTip = tailMid.getChild("tail_tip");
    }

    public static void registerRenderState(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<WildAnimalRenderer<WildAnimal>>() {}, (WildAnimal animal, WildAnimalRenderState state) -> {
            if (animal instanceof Squirrel squirrel && squirrel.isClimbing()) state.setRenderData(CLIMB, squirrel.isDescending() ? 2 : 1);
        });
    }

    public static LayerDefinition createBodyLayer() {
        return SmallGeometry.squirrel(); // scripts/gen_small_models.py
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        ModelPart[] limbs = {rightFrontLeg, leftFrontLeg, rightHaunch, leftHaunch};

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHaunch, rightHaunch});
            leftEar.xRot = rightEar.xRot = 1.4F;
            tail.xRot = 2.8F; // the bushy tail falls down along the back, past the head
            tailMid.xRot = 0.15F;
            tailTip.xRot = 0.15F;
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart limb : limbs) limb.zRot = CarcassRenderer.LIMB_SAG;
            head.yRot = state.yRot * Mth.DEG_TO_RAD;
            tail.xRot = 0.2F; // tail lies out straight behind
            tailMid.xRot = 0.1F;
            tailTip.xRot = 0.1F;
            return;
        }

        Integer climbData = state.getRenderData(CLIMB);
        int climb = climbData == null ? 0 : climbData;
        if (climb != 0) {
            // clinging to the bark: body upright, belly against the trunk (in front, or behind when coming down head
            // first), legs gripping forward into the bark, tail hanging down the trunk with a lazy flick
            boolean down = climb == 2;
            body.xRot = down ? 1.5F : -1.5F;
            body.y -= down ? 4.0F : 2.0F;
            body.z += down ? 0.0F : -4.0F;
            for (ModelPart limb : limbs) limb.xRot = -0.2F;
            float scurry = Math.min(state.walkAnimationSpeed * 3.0F, 1.0F);
            rightFrontLeg.xRot += Mth.sin(t * 1.4F) * 0.6F * scurry;
            leftFrontLeg.xRot -= Mth.sin(t * 1.4F) * 0.6F * scurry;
            rightHaunch.xRot -= Mth.sin(t * 1.4F) * 0.5F * scurry;
            leftHaunch.xRot += Mth.sin(t * 1.4F) * 0.5F * scurry;
            head.xRot = down ? -0.9F : 0.6F; // looks out from the trunk
            head.yRot = Mth.sin(t * 0.07F) * 0.5F;
            tail.xRot = -0.4F + Mth.sin(t * 0.2F) * 0.1F;
            tailMid.xRot = -0.5F;
            tailTip.xRot = -0.3F + Mth.sin(t * 0.3F) * 0.2F;
            return;
        }

        if (state.dozing) {
            // curled up on the ground, tail laid forward over the back like a blanket
            body.y += 2.0F;
            rightFrontLeg.xRot = leftFrontLeg.xRot = -1.5F;
            rightFrontLeg.y -= 0.5F;
            leftFrontLeg.y -= 0.5F;
            rightHindFoot.y -= 1.5F;
            leftHindFoot.y -= 1.5F;
            head.xRot = 0.5F;
            head.y += 1.0F;
            tail.xRot = 2.9F;
            tailMid.xRot = 0.05F + Mth.sin(t * 0.08F) * 0.02F;
            tailTip.xRot = 0.5F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * 0.8F;
        float graze = state.grazeAmount;
        float still = 1.0F - Mth.clamp(speed * 4.0F, 0.0F, 1.0F);

        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;

        // Bounding run: hind feet push off together, forepaws reach and land together, back arches and stretches.
        float air = Math.max(0.0F, Mth.sin(phase));
        body.y -= air * 1.2F * speed;
        body.xRot += -0.15F * Mth.cos(phase) * speed;
        float hind = (0.1F + 0.7F * Mth.cos(phase - 0.6F)) * speed;
        float fore = -0.9F * Mth.sin(phase) * speed;
        rightHaunch.xRot += hind;
        leftHaunch.xRot += hind;
        rightHindFoot.xRot += 0.4F * Mth.cos(phase - 0.3F) * speed;
        leftHindFoot.xRot += 0.4F * Mth.cos(phase - 0.3F) * speed;
        rightFrontLeg.xRot += fore;
        leftFrontLeg.xRot += fore;

        // Sitting up to eat (graze): front lifted on the haunches, forepaws at the mouth, quick nibbling.
        float sit = graze;
        body.xRot -= 0.65F * sit;
        body.y += 0.3F * sit;
        rightHaunch.xRot += 0.65F * sit;
        leftHaunch.xRot += 0.65F * sit;
        rightFrontLeg.xRot -= 1.1F * sit;
        leftFrontLeg.xRot -= 1.1F * sit;
        head.xRot += (0.65F + 0.08F * Mth.sin(t * 2.2F)) * sit;

        // Alert stillness: quick head darts.
        head.yRot += still * (1.0F - sit) * (Mth.sin(t * 0.21F) > 0.6F ? 0.35F : 0.0F) * Math.signum(Mth.sin(t * 0.05F));
        rightEar.zRot += -0.1F * Mth.sin(t * 0.13F) * still;
        leftEar.zRot += 0.1F * Mth.sin(t * 0.11F) * still;

        // Tail: arched over the back at rest, streaming out behind at speed, flicking when still.
        float flick = still * Math.max(0.0F, Mth.sin(t * 0.09F) * 4.0F - 3.0F);
        tail.xRot = TAIL_X - 0.7F * speed + 0.3F * flick * Mth.sin(t * 1.5F);
        tailMid.xRot = TAIL_MID_X - 0.6F * speed + 0.1F * Mth.sin(t * 0.15F);
        tailTip.xRot = TAIL_TIP_X - 0.6F * speed + 0.2F * flick * Mth.sin(t * 1.5F + 1.0F);
    }
}
