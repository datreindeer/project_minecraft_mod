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
 * Seal: a spindle-shaped body lying on its belly (chest, barrel, tapering hips), a round head with a short whiskered
 * muzzle and big eyes, small fore flippers splayed under the chest and the paired hind flippers trailing behind. On land
 * it humps along (body arching, hips lifting, forefeet pulling); in the water the hips and hind flippers sweep side to
 * side; basking (graze) it lifts head and tail in a banana curve. Pup: same model, scaled, white texture.
 * Parts, boxes and skins (seal.png / seal_pup.png, 128x64, and the skinned seal.png): {@link CoastGeometry#seal()} from
 * scripts/gen_owarm_coast_models.py (a spotted harbour-seal coat with pale rings on the back; the pup white).
 */
public class SealModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "seal"), "main");
    private static final float FLIPPER_X = 0.3F, FLIPPER_Z = 0.5F;

    private final ModelPart body, head, hips, tail, rightFlipper, leftFlipper;

    public SealModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        hips = body.getChild("hips");
        tail = hips.getChild("tail");
        rightFlipper = root.getChild("right_flipper");
        leftFlipper = root.getChild("left_flipper");
    }

    public static LayerDefinition createBodyLayer() {
        return CoastGeometry.seal();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;

        if (CarcassRenderer.isHung(state)) {
            // hung by the hind flippers: they already point up along the body; the forefeet dangle, the head hangs in line
            CarcassRenderer.hangLegs(new ModelPart[] {rightFlipper, leftFlipper}, new ModelPart[0]);
            head.xRot = 0.15F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            rightFlipper.zRot = leftFlipper.zRot = CarcassRenderer.LIMB_SAG;
            head.yRot = state.yRot * Mth.DEG_TO_RAD;
            tail.xRot = 0.15F;
            return;
        }

        float lookYaw = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD, lookPitch = Mth.clamp(state.xRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
        float speed = Math.min(state.walkAnimationSpeed, 1.2F);
        if (state.isInWater) {
            // swimming: hips and hind flippers sweep side to side, forefeet pressed to the flanks, head leads the dive
            float sweep = 0.35F + speed * 0.5F, phase = t * (0.25F + speed * 0.35F);
            hips.yRot = Mth.sin(phase) * 0.3F * sweep;
            tail.yRot = Mth.sin(phase - 1.1F) * 0.7F * sweep;
            body.yRot = -Mth.sin(phase) * 0.06F * sweep;
            rightFlipper.xRot = leftFlipper.xRot = 1.2F;
            rightFlipper.zRot = 0.2F;
            leftFlipper.zRot = -0.2F;
            head.yRot = lookYaw * 0.5F;
            head.xRot = lookPitch;
            return;
        }
        if (state.dozing) {
            // asleep on the shore: chin on the ground, flippers flat out, slow breathing
            head.xRot = 0.35F;
            head.y += 0.8F;
            rightFlipper.zRot = 1.2F;
            leftFlipper.zRot = -1.2F;
            body.y += Mth.sin(t * 0.07F) * 0.15F;
            tail.xRot = 0.1F;
            return;
        }

        // humping along on land: the body arches and pulls forward on the forefeet, the hips heave after it
        float phase = state.walkAnimationPos * 0.8F;
        body.y -= Math.max(0.0F, Mth.sin(phase)) * 1.2F * speed;
        body.xRot = Mth.sin(phase) * 0.08F * speed;
        hips.xRot = -Mth.sin(phase - 0.8F) * 0.25F * speed;
        tail.xRot = -Mth.sin(phase - 1.6F) * 0.2F * speed;
        rightFlipper.xRot = leftFlipper.xRot = FLIPPER_X + Mth.cos(phase) * 0.7F * speed;
        head.yRot = lookYaw;
        head.xRot = lookPitch - Mth.sin(phase) * 0.1F * speed;

        // basking (the graze idle): head and tail flippers raised in a banana curve
        float bask = state.grazeAmount;
        if (bask > 0.0F) {
            head.xRot -= 0.55F * bask;
            hips.xRot -= 0.22F * bask;
            tail.xRot -= 0.45F * bask + Mth.sin(t * 0.15F) * 0.05F * bask;
        }
    }
}
