package com.mechanical.client.wildlife.warm;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Desert lizard (an agama): low flat body, wedge head with a jaw, ringed eye, ear opening and a throat pouch, sprawled legs
 * (upper leg out to the side, lower leg down to a flat long-toed foot) and a long whip tail in four tapering segments. Scurries with the sprawling
 * lizard S-wave through body and tail, does push-up head bobs when idle; basking it lies pressed flat with the legs
 * splayed and the throat pulsing; a shed tail ({@link WarmRenderState#tailless}) leaves only the stub.
 * Babies use this model scaled down. Parts, boxes and the 64x64 skin (textures/entity/wildlife/desert_lizard.png plus the
 * skinned carcass skin) come from one table in scripts/gen_owarm_reptiles_models.py ({@link WarmDryGeometry}).
 */
public class DesertLizardModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "desert_lizard"), "main");
    /** Sprawl: the upper leg's roll out to the side (right leg; the left mirrors it). The lower leg rolls back to vertical. */
    private static final float SPRAWL = 1.2F;
    private static final float[] TAIL_REST = {-0.45F, 0.25F, 0.15F, 0.05F};

    private final ModelPart body, head, throat;
    private final ModelPart[] tail = new ModelPart[4];
    private final ModelPart rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;
    private final ModelPart[] legs;

    public DesertLizardModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.throat = head.getChild("throat");
        ModelPart t = body;
        for (int i = 0; i < 4; i++) tail[i] = t = t.getChild("tail" + i);
        this.rightFrontLeg = body.getChild("right_front_leg");
        this.leftFrontLeg = body.getChild("left_front_leg");
        this.rightHindLeg = body.getChild("right_hind_leg");
        this.leftHindLeg = body.getChild("left_hind_leg");
        this.legs = new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};
    }

    public static LayerDefinition createBodyLayer() {
        return WarmDryGeometry.desertLizard();
    }

    private static ModelPart lower(ModelPart leg) {
        return leg.getChild("lower");
    }

    /** +1 for the left legs, -1 for the right: signs the side rolls and swings. */
    private static float side(ModelPart leg) {
        return Math.signum(leg.x);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        WarmRenderState w = state instanceof WarmRenderState ws ? ws : null;
        tail[1].visible = w == null || !w.tailless; // resetPose doesn't restore visibility

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            for (ModelPart leg : legs) lower(leg).zRot = 0.0F;
            head.xRot = 0.2F;
            tail[0].xRot = 2.8F; // the limp tail flops down over the back
            for (int i = 1; i < 4; i++) tail[i].xRot = 0.0F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) {
                leg.zRot = CarcassRenderer.LIMB_SAG;
                lower(leg).zRot = 0.0F;
            }
            head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.5F;
            tail[0].xRot = -0.1F;
            for (int i = 1; i < 4; i++) {
                tail[i].xRot = 0.0F;
                tail[i].yRot = 0.25F; // a loose curl
            }
            return;
        }

        if (w != null && w.basking) {
            // pressed flat to the warm ground, legs splayed out sideways, head up, throat pulsing slowly
            body.y += 1.9F;
            for (ModelPart leg : legs) {
                boolean hind = leg == rightHindLeg || leg == leftHindLeg;
                leg.zRot = -side(leg) * 1.5F;
                lower(leg).zRot = side(leg) * 0.25F;
                leg.xRot = hind ? 0.5F : -0.45F; // front feet reach forward, hind feet back
            }
            head.xRot = -0.3F + Mth.clamp(state.xRot, -20.0F, 20.0F) * Mth.DEG_TO_RAD * 0.3F;
            head.yRot = Mth.clamp(state.yRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD * 0.5F;
            throat.yScale = 1.0F + Math.max(0.0F, Mth.sin(age * 0.12F)) * 0.6F;
            tail[0].xRot = -0.05F;
            tail[1].xRot = tail[2].xRot = tail[3].xRot = 0.0F;
            tail[2].yRot = Mth.sin(age * 0.02F) * 0.15F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed * 1.4F, 1.0F);
        float phase = state.walkAnimationPos * 1.5F; // quick little strides
        // trot of a sprawler: diagonal pairs; the upper leg swings the knee fore-aft (pitch comes before the sideways roll)
        // and lifts it on the way forward
        for (ModelPart leg : legs) {
            float p = (leg == rightFrontLeg || leg == leftHindLeg) ? phase : phase + Mth.PI;
            float fwd = Mth.cos(p) * 0.8F * speed, lift = Math.max(0.0F, -Mth.sin(p)) * 0.5F * speed;
            leg.xRot = -fwd;
            leg.zRot -= side(leg) * lift;
            lower(leg).zRot += side(leg) * lift * 0.6F;
        }
        // the S-wave: the body swings to the side of the hind leg that is pushing, head kept steady, the tail following behind
        float wave = Mth.sin(phase) * speed;
        body.yRot = wave * 0.3F;
        head.yRot = -wave * 0.25F + Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(state.xRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD * 0.6F;
        for (int i = 0; i < 4; i++) tail[i].yRot = Mth.sin(phase - 1.0F - i * 0.9F) * (0.3F + i * 0.08F) * speed + Mth.sin(age * 0.05F + i) * 0.04F;
        tail[0].xRot += speed * 0.25F; // tail lifted clear of the sand when running

        // idle: a quick double push-up, the front of the body pumped up on straightened forelegs
        float still = 1.0F - speed;
        float c = (age + state.entityId * 37) % 140.0F;
        float bob = c < 24.0F ? Math.max(0.0F, Mth.sin(c / 24.0F * Mth.PI * 3.0F)) * still : 0.0F;
        if (bob > 0) {
            body.xRot -= 0.22F * bob;
            body.y -= 0.6F * bob;
            head.xRot -= 0.2F * bob;
            for (ModelPart leg : new ModelPart[] {rightFrontLeg, leftFrontLeg}) {
                leg.zRot += side(leg) * 0.5F * bob; // less sprawl: the forelegs straighten under the chest
                lower(leg).zRot -= side(leg) * 0.5F * bob;
                leg.xRot += 0.22F * bob;
            }
            for (ModelPart leg : new ModelPart[] {rightHindLeg, leftHindLeg}) leg.xRot += 0.22F * bob;
            throat.yScale = 1.0F + 0.4F * bob;
        }

        // snapping up an insect: head dips and jerks
        float g = state.grazeAmount;
        if (g > 0) {
            head.xRot = Mth.lerp(g, head.xRot, 0.45F + Mth.sin(age * 0.9F) * 0.1F);
            head.y += g * 0.4F;
        }
    }
}
