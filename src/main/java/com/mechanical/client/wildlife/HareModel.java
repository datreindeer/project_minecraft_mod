package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;

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

/**
 * European brown hare: lean body, long black-tipped ears, long hind feet. Bounds when moving, sits up a little when
 * still, nibbles with the head down when grazing. Texture: textures/entity/wildlife/hare.png, painted by
 * scripts/gen_small_models.py (with the geometry, SmallGeometry).
 */
public class HareModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "hare"), "main");
    /** Body pitch (radians, nose up) when sitting up. */
    private static final float SIT_PITCH = 0.12F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart nose;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart tail;
    private final ModelPart leftFrontLeg;
    private final ModelPart rightFrontLeg;
    private final ModelPart leftHaunch;
    private final ModelPart rightHaunch;
    private final ModelPart leftHindFoot;
    private final ModelPart rightHindFoot;

    public HareModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.nose = this.head.getChild("nose");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.tail = this.body.getChild("tail");
        this.leftFrontLeg = this.body.getChild("left_front_leg");
        this.rightFrontLeg = this.body.getChild("right_front_leg");
        this.leftHaunch = this.body.getChild("left_haunch");
        this.rightHaunch = this.body.getChild("right_haunch");
        this.leftHindFoot = this.leftHaunch.getChild("left_hind_foot");
        this.rightHindFoot = this.rightHaunch.getChild("right_hind_foot");
    }

    public static LayerDefinition createBodyLayer() {
        return SmallGeometry.hare(); // scripts/gen_small_models.py
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot = state.xRot * Mth.DEG_TO_RAD;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHaunch, rightHaunch});
            leftEar.xRot = rightEar.xRot = 1.4F; // long ears dangle below the head
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart limb : new ModelPart[] {this.rightFrontLeg, this.leftFrontLeg, this.rightHaunch, this.leftHaunch}) {
                limb.zRot = CarcassRenderer.LIMB_SAG;
            }
            this.leftEar.xRot = -1.3F; // ears flat along the neck
            this.rightEar.xRot = -1.3F;
            return;
        }

        if (state.dozing) {
            // pressed flat in its form: body (bottom 19) dropped onto y = 24, forepaws stretched forward flat under the
            // chin, hind feet flat along the flanks (the rigid shin+foot part is raised so the shin hides in the haunch)
            this.body.y += 5.0F;
            this.leftFrontLeg.xRot = -1.5F;
            this.rightFrontLeg.xRot = -1.5F;
            this.leftFrontLeg.y -= 1.4F;
            this.rightFrontLeg.y -= 1.4F;
            this.leftHindFoot.y -= 5.0F;
            this.rightHindFoot.y -= 5.0F;
            this.head.xRot = 0.0F;
            this.head.yRot = 0.0F;
            this.head.y += 2.0F;
            // ears laid back flat along the back
            this.leftEar.xRot = -1.45F;
            this.rightEar.xRot = -1.45F;
            return;
        }

        float t = state.ageInTicks;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * 0.6662F;
        float graze = state.grazeAmount;
        float sit = (1.0F - Mth.clamp(speed * 4.0F, 0.0F, 1.0F)) * (1.0F - graze);

        // Sitting up: the body pitches up around the hips and sinks onto the haunches; the forelegs stay vertical and
        // straighten (stretched a little) to keep the front paws on the ground, the hind legs keep their pose.
        this.body.y += 0.5F * sit;
        this.body.xRot -= SIT_PITCH * sit;
        this.leftHaunch.xRot += SIT_PITCH * sit;
        this.rightHaunch.xRot += SIT_PITCH * sit;
        this.leftFrontLeg.xRot += SIT_PITCH * sit;
        this.rightFrontLeg.xRot += SIT_PITCH * sit;
        this.leftFrontLeg.yScale = 1.0F + 0.13F * sit; // keeps the forepaws on the ground at SIT_PITCH
        this.rightFrontLeg.yScale = 1.0F + 0.13F * sit;
        this.head.xRot += 0.15F * sit;

        // Bounding: hind legs push off together, the body flies with the forelegs reaching forward, lands on the
        // forelegs while the hind legs swing forward past them. sin(phase) > 0 is the airborne half.
        float air = Math.max(0.0F, Mth.sin(phase));
        float pitch = -0.15F * Mth.cos(phase) * speed;
        this.body.y -= air * 1.5F * speed;
        this.body.xRot += pitch;
        this.head.xRot -= pitch * 0.7F;
        float hind = (0.15F + 0.65F * Mth.cos(phase - 0.6F)) * speed;
        float ankle = 0.4F * Mth.cos(phase - 0.3F) * speed;
        float fore = -0.85F * Mth.sin(phase) * speed;
        this.leftHaunch.xRot += hind;
        this.rightHaunch.xRot += hind;
        this.leftHindFoot.xRot += ankle;
        this.rightHindFoot.xRot += ankle;
        this.leftFrontLeg.xRot += fore;
        this.rightFrontLeg.xRot += fore;
        this.tail.xRot += 0.3F * air * speed;

        // Nibbling: head down to the grass, chewing.
        this.head.y += 2.0F * graze;
        this.head.xRot += (1.2F + 0.06F * Mth.sin(t * 1.6F)) * graze;

        // Ears: laid back when running or grazing, plus the odd quick twitch; nose always wiggling.
        float earsBack = 0.5F * speed + 0.6F * graze;
        float twitchLeft = Math.max(0.0F, Mth.sin(t * 0.11F) * 6.0F - 5.0F);
        float twitchRight = Math.max(0.0F, Mth.sin(t * 0.083F + 2.0F) * 6.0F - 5.0F);
        this.leftEar.xRot -= earsBack + 0.3F * twitchLeft;
        this.leftEar.zRot += 0.2F * twitchLeft * Mth.sin(t * 1.7F);
        this.rightEar.xRot -= earsBack + 0.3F * twitchRight;
        this.rightEar.zRot -= 0.2F * twitchRight * Mth.sin(t * 1.9F);
        this.nose.y += 0.3F * Mth.sin(t * (1.1F + 1.3F * graze));
        this.tail.xRot += 0.08F * Mth.sin(t * 0.25F);
    }
}
