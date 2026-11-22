package com.mechanical.client.wildlife.desert;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

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
 * Desert Scorpion: a flat banded body, a small head with two arms ending in pincers, four pairs of thin legs splayed out
 * and down, and a tail of four segments curling up over the back to the sting. Walking: the legs ripple in alternating
 * groups and the pincers sway; idle the pincers open and close now and then; hunting (charging) the tail arches further and
 * jabs forward; hidden by day (dozing) it lies flat with legs drawn in and the tail laid along the back; a carcass lies on
 * its back, legs curled. Texture 64x32: textures/entity/wildlife/desert_scorpion.png, painted by
 * scripts/gen_fauna_desert_textures.py whose scorpion() part tree mirrors the cubes below (keep them in sync).
 */
public class ScorpionModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "desert_scorpion"), "main");
    private static final float[] TAIL_X = {-0.75F, -0.75F, -0.75F, -0.7F};
    private static final float STING_X = -0.9F, ARM_Y = 0.6F, CLAW_Y = -0.9F, LEG_Z = 0.7F;
    private static final float[] LEG_Y = {0.5F, 0.17F, -0.16F, -0.49F};

    private final ModelPart body, head, leftArm, rightArm, leftClaw, rightClaw, sting;
    private final ModelPart[] tail = new ModelPart[4], leftLegs = new ModelPart[4], rightLegs = new ModelPart[4];

    public ScorpionModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        leftArm = head.getChild("left_arm");
        rightArm = head.getChild("right_arm");
        leftClaw = leftArm.getChild("left_claw");
        rightClaw = rightArm.getChild("right_claw");
        ModelPart t = body;
        for (int i = 0; i < 4; i++) tail[i] = t = t.getChild("tail" + (i + 1));
        sting = tail[3].getChild("sting");
        for (int i = 0; i < 4; i++) {
            leftLegs[i] = body.getChild("left_leg" + i);
            rightLegs[i] = body.getChild("right_leg" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -1.0F, -3.5F, 5.0F, 2.0F, 7.0F), PartPose.offset(0.0F, 21.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(24, 0).addBox(-2.0F, -1.0F, -2.5F, 4.0F, 2.0F, 3.0F), PartPose.offset(0.0F, 0.0F, -3.5F));
        PartDefinition leftArm = head.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(30, 9).mirror().addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F).mirror(false), PartPose.offsetAndRotation(1.5F, 0.3F, -2.0F, 0.0F, ARM_Y, 0.0F));
        leftArm.addOrReplaceChild("left_claw", CubeListBuilder.create().texOffs(20, 9).mirror().addBox(-1.0F, -0.5F, -3.0F, 2.0F, 1.0F, 3.0F).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.0F, CLAW_Y, 0.0F));
        PartDefinition rightArm = head.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(30, 9).addBox(-0.5F, -0.5F, -3.0F, 1.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(-1.5F, 0.3F, -2.0F, 0.0F, -ARM_Y, 0.0F));
        rightArm.addOrReplaceChild("right_claw", CubeListBuilder.create().texOffs(20, 9).addBox(-1.0F, -0.5F, -3.0F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -3.0F, 0.0F, -CLAW_Y, 0.0F));
        float[] legZ = {-2.5F, -1.0F, 0.5F, 2.0F};
        for (int i = 0; i < 4; i++) {
            body.addOrReplaceChild("left_leg" + i, CubeListBuilder.create().texOffs(44, 9).mirror().addBox(0.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F).mirror(false),
                    PartPose.offsetAndRotation(2.5F, 0.4F, legZ[i], 0.0F, LEG_Y[i], LEG_Z));
            body.addOrReplaceChild("right_leg" + i, CubeListBuilder.create().texOffs(44, 9).addBox(-4.0F, -0.5F, -0.5F, 4.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(-2.5F, 0.4F, legZ[i], 0.0F, -LEG_Y[i], -LEG_Z));
        }
        PartDefinition tail1 = body.addOrReplaceChild("tail1", CubeListBuilder.create().texOffs(38, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -0.3F, 3.5F, TAIL_X[0], 0.0F, 0.0F));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -0.3F, 2.5F, TAIL_X[1], 0.0F, 0.0F));
        PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(0, 9).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -0.3F, 2.5F, TAIL_X[2], 0.0F, 0.0F));
        PartDefinition tail4 = tail3.addOrReplaceChild("tail4", CubeListBuilder.create().texOffs(10, 9).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -0.3F, 2.5F, TAIL_X[3], 0.0F, 0.0F));
        tail4.addOrReplaceChild("sting", CubeListBuilder.create().texOffs(38, 9).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 2.5F, STING_X, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        if (CarcassRenderer.isHung(state)) {
            for (ModelPart p : tail) p.xRot = 0.0F; // hanging by the tail, straight
            sting.xRot = 0.0F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            body.zRot = Mth.PI; // on its back
            body.y -= 2.0F;
            for (int i = 0; i < 4; i++) {
                leftLegs[i].zRot = -0.9F;
                rightLegs[i].zRot = 0.9F;
                tail[i].xRot = -0.25F;
            }
            return;
        }
        if (state.dozing) {
            // hidden: flat on the ground, legs drawn in, tail laid along the back
            body.y += 0.8F;
            for (int i = 0; i < 4; i++) {
                leftLegs[i].zRot = 0.25F;
                rightLegs[i].zRot = -0.25F;
                leftLegs[i].yRot *= 1.6F;
                rightLegs[i].yRot *= 1.6F;
                tail[i].xRot = i == 0 ? -0.3F : -0.45F;
            }
            leftArm.yRot = ARM_Y + 0.5F;
            rightArm.yRot = -ARM_Y - 0.5F;
            return;
        }
        float speed = Math.min(state.walkAnimationSpeed * 1.4F, 1.0F);
        float phase = state.walkAnimationPos * 1.6F;
        for (int i = 0; i < 4; i++) {
            float s = Mth.sin(phase + i * Mth.PI * 0.5F) * 0.45F * speed;
            float lift = Math.max(0.0F, Mth.cos(phase + i * Mth.PI * 0.5F)) * 0.3F * speed;
            leftLegs[i].yRot = LEG_Y[i] + s;
            rightLegs[i].yRot = -LEG_Y[i] + s;
            leftLegs[i].zRot = LEG_Z - lift;
            rightLegs[i].zRot = -LEG_Z + (speed > 0 ? Math.max(0.0F, -Mth.cos(phase + i * Mth.PI * 0.5F)) * 0.3F * speed : 0);
        }
        // pincers: sway when walking, open and snap now and then
        float snap = Mth.sin(t * 0.07F) > 0.85F ? Mth.abs(Mth.sin(t * 0.9F)) * 0.5F : 0.0F;
        leftArm.yRot = ARM_Y + Mth.sin(phase) * 0.15F * speed;
        rightArm.yRot = -ARM_Y + Mth.sin(phase) * 0.15F * speed;
        leftClaw.yRot = CLAW_Y + snap;
        rightClaw.yRot = -CLAW_Y - snap;
        head.yRot = Mth.clamp(state.yRot, -25.0F, 25.0F) * Mth.DEG_TO_RAD;
        // the tail sways a little; arched and jabbing when it hunts
        float sway = Mth.sin(t * 0.05F) * 0.08F;
        for (int i = 0; i < 4; i++) {
            tail[i].xRot = TAIL_X[i] + sway * (i + 1) * 0.3F;
            tail[i].yRot = Mth.sin(t * 0.04F + i * 0.6F) * 0.05F;
        }
        if (state.charging) {
            float jab = Math.max(0.0F, Mth.sin(t * 0.5F));
            for (int i = 1; i < 4; i++) tail[i].xRot = TAIL_X[i] - 0.15F - jab * 0.2F;
            sting.xRot = STING_X - 0.4F - jab * 0.6F;
            leftArm.yRot = ARM_Y - 0.3F;
            rightArm.yRot = -ARM_Y + 0.3F;
            leftClaw.yRot = CLAW_Y + 0.5F;
            rightClaw.yRot = -CLAW_Y - 0.5F;
        }
    }
}
