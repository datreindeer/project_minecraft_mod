package com.mechanical.client.wildlife.coast;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Walrus: a massive barrel of a body (deep chest, broad hips), a small round head on a thick neck with a broad bristly
 * whisker pad and two ivory tusks hanging from the upper jaw, long fore flippers it walks on, hind flippers trailing
 * flat. Lumbers on land (heavy rolling gait), sculls with the hind flippers in the water, rears its head and tusks when
 * it warns or charges. The calf has no tusks. Rendered at 1.5x (the renderer). Parts, boxes and skins (walrus.png /
 * walrus_calf.png, 128x128, and the skinned walrus.png): {@link CoastGeometry#walrus()} from scripts/gen_owarm_coast_models.py.
 */
public class WalrusModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "walrus"), "main");
    private static final float TUSK_X = 0.15F, TUSK_Z = 0.06F, FRONT_Z = 0.2F;

    private final ModelPart body, head, rightTusk, leftTusk, hips, rightHind, leftHind, rightFront, leftFront;

    public WalrusModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        rightTusk = head.getChild("right_tusk");
        leftTusk = head.getChild("left_tusk");
        hips = body.getChild("hips");
        rightHind = hips.getChild("right_hind");
        leftHind = hips.getChild("left_hind");
        rightFront = root.getChild("right_front");
        leftFront = root.getChild("left_front");
    }

    public static LayerDefinition createBodyLayer() {
        return CoastGeometry.walrus();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        rightTusk.visible = leftTusk.visible = state.showsHorns;

        if (CarcassRenderer.isHung(state)) { // too big for the rack, but a command can still hang it
            CarcassRenderer.hangLegs(new ModelPart[] {rightFront, leftFront}, new ModelPart[0]);
            head.xRot = 0.2F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            rightFront.zRot = leftFront.zRot = CarcassRenderer.LIMB_SAG;
            head.yRot = state.yRot * Mth.DEG_TO_RAD;
            return;
        }

        float lookYaw = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD, lookPitch = Mth.clamp(state.xRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
        float speed = Math.min(state.walkAnimationSpeed, 1.2F);
        if (state.isInWater) {
            // sculling with the hind flippers, forefeet steering, head up
            float phase = t * (0.2F + speed * 0.3F), sweep = 0.4F + speed * 0.5F;
            hips.yRot = Mth.sin(phase) * 0.18F * sweep;
            rightHind.yRot = Mth.sin(phase - 1.0F) * 0.6F * sweep;
            leftHind.yRot = Mth.sin(phase - 1.0F) * 0.6F * sweep;
            rightFront.xRot = leftFront.xRot = 0.9F + Mth.sin(phase * 0.5F) * 0.15F;
            head.yRot = lookYaw * 0.5F;
            head.xRot = lookPitch - 0.15F;
            return;
        }
        if (state.dozing) {
            // asleep in a heap: head sunk forward, tusks on the ground, flippers out flat
            head.xRot = 0.3F;
            head.y += 1.0F;
            rightFront.zRot = 1.1F;
            leftFront.zRot = -1.1F;
            body.y += Mth.sin(t * 0.06F) * 0.2F;
            return;
        }

        // heavy rolling gait on the forefeet; the hind end drags and heaves
        float phase = state.walkAnimationPos * 0.6F;
        rightFront.xRot = Mth.cos(phase) * 0.6F * speed;
        leftFront.xRot = -Mth.cos(phase) * 0.6F * speed;
        body.zRot = Mth.sin(phase) * 0.05F * speed;
        body.y -= Math.abs(Mth.sin(phase)) * 0.8F * speed;
        hips.xRot = -Math.abs(Mth.sin(phase)) * 0.12F * speed;
        rightHind.xRot = leftHind.xRot = Mth.sin(phase) * 0.15F * speed;
        head.yRot = lookYaw;
        head.xRot = lookPitch + Mth.sin(phase * 2.0F) * 0.04F * speed;

        // resting upright (graze idle): head lifted, tusks out
        float rest = state.grazeAmount;
        if (rest > 0.0F) head.xRot -= 0.35F * rest;
        // warning or charging: head thrown up and back, tusks jabbed forward
        if (state.charging) {
            head.xRot = -0.55F + Math.max(0.0F, Mth.sin(t * 0.5F)) * 0.35F;
            rightTusk.xRot = leftTusk.xRot = TUSK_X - 0.1F;
        }
    }
}
