package com.mechanical.client.wildlife.river;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Beaver: a heavy, hunched body (narrow shoulders, a big round rump), a blunt head with small ears and the orange
 * incisors showing, short front legs with clawed hands, big webbed hind feet, and the flat scaly tail trailing on the
 * ground. Waddles on land; swimming it lies flat with the hind feet kicking, hands tucked, tail sculling. Gnawing,
 * it cocks its head and chews; carrying, a stick sits crosswise in its mouth.
 * Geometry: {@link RiverGeometry#beaver}; texture textures/entity/wildlife/beaver.png (64x64) and its skinned carcass skin,
 * both generated with the boxes by scripts/gen_owarm_river_models.py.
 */
public class BeaverModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "beaver"), "main");
    private static final float TAIL_X = -0.3F;

    private final ModelPart body, tail, head, stick;
    private final ModelPart rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;
    private final ModelPart[] legs;

    public BeaverModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        tail = body.getChild("tail");
        head = body.getChild("head");
        stick = head.getChild("stick");
        rightFrontLeg = root.getChild("right_front_leg");
        leftFrontLeg = root.getChild("left_front_leg");
        rightHindLeg = root.getChild("right_hind_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        legs = new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};
    }

    public static LayerDefinition createBodyLayer() {
        return RiverGeometry.beaver();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        stick.visible = RiverFaunaClient.flag(state, RiverFaunaClient.CARRYING);

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            head.xRot = 0.2F;
            tail.xRot = 2.5F; // sticks up (+z) past the gambrel: the heavy tail folds down over the rump
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            head.yRot = state.yRot * Mth.DEG_TO_RAD;
            tail.xRot = 0.0F;
            tail.zRot = 0.3F;
            return;
        }
        if (state.dozing) {
            // curled flat on its belly, nose tucked towards the tail
            body.y += 2.0F;
            for (ModelPart leg : legs) leg.y += 1.8F;
            rightFrontLeg.xRot = leftFrontLeg.xRot = -1.3F;
            rightHindLeg.xRot = leftHindLeg.xRot = 1.3F;
            head.xRot = 0.25F;
            head.yRot = 0.35F;
            tail.xRot = -0.05F;
            tail.yRot = 0.5F;
            body.y += Mth.sin(t * 0.07F) * 0.1F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.2F);
        float phase = state.walkAnimationPos;
        if (RiverFaunaClient.flag(state, RiverFaunaClient.SWIMMING)) {
            // flat in the water: hands tucked to the chest, the webbed hind feet kicking in turn, the tail sculling
            float kick = Mth.sin(t * 0.35F) * (0.5F + speed);
            rightFrontLeg.xRot = leftFrontLeg.xRot = -1.2F;
            rightHindLeg.xRot = 0.6F + kick;
            leftHindLeg.xRot = 0.6F - kick;
            tail.xRot = 0.1F;
            tail.yRot = Mth.sin(t * 0.35F) * 0.3F;
            head.xRot = -0.25F; // nose up out of the water
            head.yRot = Mth.clamp(state.yRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
            return;
        }

        // a heavy, short-legged waddle with the rump rolling
        float swing = Mth.cos(phase * 1.1F) * 0.9F * speed;
        rightFrontLeg.xRot = leftHindLeg.xRot = swing;
        leftFrontLeg.xRot = rightHindLeg.xRot = -swing;
        body.zRot = Mth.sin(phase * 1.1F) * 0.08F * speed;
        body.y -= Math.abs(Mth.sin(phase * 1.1F)) * 0.5F * speed;
        tail.xRot = TAIL_X + Math.abs(Mth.sin(phase * 1.1F)) * 0.1F * speed;
        tail.yRot = Mth.sin(phase * 0.55F) * 0.12F * speed;
        head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(state.xRot, -25.0F, 25.0F) * Mth.DEG_TO_RAD;

        if (RiverFaunaClient.flag(state, RiverFaunaClient.GNAWING)) {
            // up on the haunches at the trunk: head cocked, jaw working fast
            body.xRot = -0.35F;
            body.y -= 0.8F;
            rightFrontLeg.xRot = leftFrontLeg.xRot = -0.9F;
            rightFrontLeg.y -= 1.5F;
            leftFrontLeg.y -= 1.5F;
            head.zRot = 0.45F + Mth.sin(t * 0.25F) * 0.1F;
            head.xRot = 0.35F + Mth.sin(t * 1.6F) * 0.08F;
            head.yRot = Mth.sin(t * 0.1F) * 0.2F;
            return;
        }
        float graze = state.grazeAmount;
        if (graze > 0.0F) {
            // nibbling bark and shoots held in the hands
            head.xRot = Mth.lerp(graze, head.xRot, 0.6F + Mth.sin(t * 1.2F) * 0.05F);
            rightFrontLeg.xRot = Mth.lerp(graze, rightFrontLeg.xRot, -0.8F);
            leftFrontLeg.xRot = Mth.lerp(graze, leftFrontLeg.xRot, -0.8F);
        }
    }
}
