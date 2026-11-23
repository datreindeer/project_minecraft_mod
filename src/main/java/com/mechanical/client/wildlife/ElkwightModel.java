package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.wildlife.elkwight.Elkwight;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The Elkwight: a giant's body (hairy legs ending in hooves, a broad torso under a fur mantle, a hide belt) with an elk's
 * head (long drooping muzzle, the dewlap, small ears) and a crown of two great palmate antlers, each a broad upright palm
 * with tines along its rim, about 40 px across (4 blocks at the renderer's 1.8 scale). The double-bladed axe hangs from the
 * right hand ({@code axe}, hidden while thrown and on the carcass), the left hand on the haft too. Every attack has its
 * pose ({@link Elkwight#action}, 0..1 progress): the telegraph wind-ups read from afar. Texture 128x128
 * (textures/entity/wildlife/elkwight.png, glow layer elkwight_glow.png: eyes and axe runes), painted by
 * scripts/gen_elkwight_boss_textures.py whose box table mirrors the texOffs here.
 */
public class ElkwightModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "elkwight"), "main");

    private final ModelPart root, body, head, rightArm, leftArm, rightLeg, leftLeg, axe;

    public ElkwightModel(ModelPart root) {
        super(root);
        this.root = root;
        body = root.getChild("body");
        head = body.getChild("head");
        rightArm = body.getChild("right_arm");
        leftArm = body.getChild("left_arm");
        axe = rightArm.getChild("right_forearm").getChild("axe");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    /** The parts and boxes come from scripts/gen_elkwight_model.py, which paints the 256x256 skin for them. */
    public static LayerDefinition createBodyLayer() {
        return ElkwightGeometry.create();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        Integer actionData = state.getRenderData(ElkwightRenderer.ACTION);
        int action = actionData == null ? Elkwight.IDLE : actionData;
        Float progressData = state.getRenderData(ElkwightRenderer.PROGRESS);
        float p = progressData == null ? 0 : progressData;
        boolean awake = Boolean.TRUE.equals(state.getRenderData(ElkwightRenderer.AWAKE));
        axe.visible = Boolean.TRUE.equals(state.getRenderData(ElkwightRenderer.HAS_AXE)) && !CarcassRenderer.isCarcass(state) && !CarcassRenderer.isHung(state);
        if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) { // on his back: limbs fallen aside, head rolled over
            rightArm.zRot = 0.9F;
            leftArm.zRot = -0.7F;
            rightLeg.zRot = 0.25F;
            leftLeg.zRot = -0.2F;
            head.yRot = 0.5F;
            head.xRot = -0.3F;
            return;
        }
        float pos = state.walkAnimationPos, speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float swing = Mth.cos(pos * 0.5F) * 0.9F * speed;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        body.xRot = 0.1F;
        head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
        head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
        // the ready stance: both hands on the haft, the axe across the body pointing up and forward
        rightArm.xRot = -0.9F + swing * 0.15F;
        rightArm.yRot = 0.2F;
        leftArm.xRot = -1.0F - swing * 0.15F; // the left hand reaches across to the haft
        leftArm.yRot = 0.55F;
        leftArm.zRot = 0.0F;
        axe.yRot = 0.35F;
        float breathe = Mth.sin(state.ageInTicks * 0.06F) * 0.03F;
        body.xRot += breathe;
        if (!awake) { // asleep on his feet: head bowed, the axe planted before him
            head.xRot = 0.7F;
            rightArm.xRot = leftArm.xRot = -0.35F;
            rightArm.yRot = 0.35F;
            leftArm.yRot = -0.45F;
            axe.xRot = 1.25F;
            return;
        }
        switch (action) {
            case Elkwight.CHOP_WIND, Elkwight.BREAK_WIND -> { // axe raised high behind the head, then down
                float raise = p < 0.75F ? ease(p / 0.75F) : 1 - ease((p - 0.75F) / 0.25F) * 1.3F;
                rightArm.xRot = leftArm.xRot = -0.9F - raise * 2.1F;
                leftArm.yRot = -0.35F;
                body.xRot = 0.1F - raise * 0.25F;
            }
            case Elkwight.SPIN_WIND -> { // the axe drawn back to the right, a stomp at 60 %
                float back = ease(Math.min(1, p / 0.5F));
                body.yRot = back * 0.9F;
                rightArm.xRot = -0.5F;
                rightArm.yRot = 0.2F + back * 0.9F;
                leftArm.xRot = -0.9F;
                leftArm.yRot = -0.2F + back * 0.6F;
                float stomp = p > 0.45F && p < 0.65F ? Mth.sin((p - 0.45F) / 0.2F * Mth.PI) : 0;
                leftLeg.xRot = -stomp * 1.0F;
                body.y = 8.0F + stomp * 0.8F;
            }
            case Elkwight.SPIN -> { // two turns with the axe out level
                body.yRot = 0.9F - p * Mth.TWO_PI * 2;
                rightArm.xRot = -1.5F;
                rightArm.zRot = 0.5F;
                leftArm.xRot = -1.5F;
                leftArm.yRot = -0.9F;
                axe.yRot = 0.9F;
            }
            case Elkwight.SWEEP_WIND -> { // the crown lowered, a hoof scraping the ground
                head.xRot = 0.95F;
                body.xRot = 0.45F;
                rightLeg.xRot = Mth.sin(p * Mth.PI * 6) * 0.5F + 0.3F;
                leftLeg.xRot = -0.2F;
                rightArm.xRot = leftArm.xRot = -0.4F;
            }
            case Elkwight.CHARGE -> {
                head.xRot = 1.0F;
                body.xRot = 0.55F;
                float run = Mth.cos(state.ageInTicks * 1.2F) * 1.1F;
                rightLeg.xRot = run;
                leftLeg.xRot = -run;
                rightArm.xRot = leftArm.xRot = -0.3F;
            }
            case Elkwight.STAGGER -> { // reeling back, the crown shaken
                body.xRot = -0.35F;
                head.xRot = -0.4F;
                head.zRot = Mth.sin(state.ageInTicks * 0.9F) * 0.25F;
                rightArm.xRot = -0.3F;
                rightArm.zRot = 0.6F;
                leftArm.xRot = -0.3F;
                leftArm.zRot = -0.7F;
            }
            case Elkwight.THROW_WIND -> { // the axe drawn far back over the right shoulder, the left arm aiming
                float wind = ease(Math.min(1, p / 0.8F));
                rightArm.xRot = -0.9F - wind * 2.3F;
                rightArm.yRot = 0.2F;
                leftArm.xRot = -1.5F;
                leftArm.yRot = 0.1F;
                body.yRot = wind * 0.4F;
            }
            case Elkwight.WAVE_WIND -> { // both hands high over the head
                float raise = ease(p);
                rightArm.xRot = leftArm.xRot = -0.9F - raise * 2.3F;
                leftArm.yRot = -0.2F;
                body.xRot = -raise * 0.3F;
                head.xRot = -raise * 0.3F;
            }
            case Elkwight.WAVE_SLAM -> { // the axe driven into the ground, bent over it
                rightArm.xRot = leftArm.xRot = -0.25F;
                leftArm.yRot = -0.2F;
                body.xRot = 0.65F;
                head.xRot = 0.3F;
                axe.xRot = 0.9F;
            }
            case Elkwight.LEAP_WIND -> { // crouched to spring
                float crouch = ease(p);
                body.xRot = 0.1F + crouch * 0.5F;
                rightLeg.xRot = leftLeg.xRot = -crouch * 0.8F;
                root.y = crouch * 3.0F;
            }
            case Elkwight.LEAP -> {
                rightLeg.xRot = -0.9F;
                leftLeg.xRot = 0.4F;
                rightArm.xRot = leftArm.xRot = -2.2F;
            }
            case Elkwight.EAT, Elkwight.PICKUP -> { // on his knees, bent over the dead
                rightLeg.xRot = leftLeg.xRot = -1.45F;
                root.y = 5.0F;
                body.xRot = 0.75F;
                head.xRot = 0.6F + (action == Elkwight.EAT ? Mth.sin(state.ageInTicks * 0.8F) * 0.15F : 0);
                rightArm.xRot = leftArm.xRot = -0.6F;
                axe.xRot = 1.0F;
            }
            default -> {}
        }
    }

    private static float ease(float t) {
        t = Mth.clamp(t, 0, 1);
        return t * t * (3 - 2 * t);
    }
}
