package com.mechanical.client.wildlife.giants;

import static com.mechanical.client.wildlife.giants.GiantPose.DEG;

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
 * Woolly rhinoceros: a long, low, barrel body on short thick legs, a big shoulder hump, a thick neck and a long head
 * carried low. The great front horn sweeps forward and curves up from the snout (three segments), a short second horn
 * stands behind it. Shaggy hair hangs along the flanks. The calf is the model scaled down with a bigger head, no horns.
 * Texture: textures/entity/wildlife/giants/woolly_rhino.png (128x64), painted by scripts/gen_fauna_giants_textures.py
 * whose RHINO table mirrors the cubes below.
 */
public class RhinoModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "woolly_rhino"), "main");
    private static final float NECK_X = 0.25F, HEAD_X = 0.55F, GRAZE_NECK_X = 0.75F, GRAZE_HEAD_X = 0.75F, TAIL_X = 0.2F;
    private static final float LIE_DROP = 6.0F, LIE_LEG_Y = 22.0F;

    private final ModelPart body, neck, head, horns, tail, leftSkirt, rightSkirt, leftEar, rightEar;
    private final ModelPart leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg;

    public RhinoModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        neck = body.getChild("neck");
        head = neck.getChild("head");
        horns = head.getChild("horns");
        tail = body.getChild("tail");
        leftSkirt = body.getChild("left_skirt");
        rightSkirt = body.getChild("right_skirt");
        leftEar = head.getChild("left_ear");
        rightEar = head.getChild("right_ear");
        leftFrontLeg = root.getChild("left_front_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightHindLeg = root.getChild("right_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.0F, -5.0F, -12.0F, 14.0F, 13.0F, 24.0F)
                .texOffs(32, 37).addBox(-6.0F, -9.0F, -13.0F, 12.0F, 5.0F, 11.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        body.addOrReplaceChild("left_skirt", CubeListBuilder.create().texOffs(76, 0).mirror().addBox(0.0F, 0.0F, -11.0F, 1.0F, 6.0F, 21.0F),
                PartPose.offset(7.0F, 4.0F, 0.0F));
        body.addOrReplaceChild("right_skirt", CubeListBuilder.create().texOffs(76, 0).addBox(-1.0F, 0.0F, -11.0F, 1.0F, 6.0F, 21.0F),
                PartPose.offset(-7.0F, 4.0F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(56, 54).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 6.0F, 1.0F)
                .texOffs(96, 54).addBox(-1.0F, 5.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 12.0F, TAIL_X, 0.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 37).addBox(-4.5F, -4.0F, -6.0F, 9.0F, 10.0F, 7.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, -12.0F, NECK_X, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(78, 37).addBox(-3.5F, -3.5F, -8.0F, 7.0F, 7.0F, 9.0F)
                .texOffs(0, 54).addBox(-3.0F, -2.0F, -12.0F, 6.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 2.0F, -5.5F, HEAD_X, 0.0F, 0.0F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(90, 54).mirror().addBox(-0.5F, -3.0F, -1.0F, 1.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, -3.5F, -1.0F, -0.4F, 0.0F, 0.4F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(90, 54).addBox(-0.5F, -3.0F, -1.0F, 1.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, -3.5F, -1.0F, -0.4F, 0.0F, -0.4F));
        // horns built along -y (up), each segment bent forward / back at its root
        PartDefinition horns = head.addOrReplaceChild("horns", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition h1 = horns.addOrReplaceChild("front_horn", CubeListBuilder.create().texOffs(60, 54).addBox(-1.0F, -4.0F, -1.5F, 2.0F, 4.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, -10.5F, 0.45F, 0.0F, 0.0F));
        PartDefinition h2 = h1.addOrReplaceChild("front_horn_mid", CubeListBuilder.create().texOffs(70, 54).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -3.5F, 0.0F, -0.2F, 0.0F, 0.0F));
        h2.addOrReplaceChild("front_horn_tip", CubeListBuilder.create().texOffs(78, 54).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -3.5F, 0.0F, -0.25F, 0.0F, 0.0F));
        horns.addOrReplaceChild("rear_horn", CubeListBuilder.create().texOffs(82, 54).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, -6.5F, 0.1F, 0.0F, 0.0F));
        addLeg(root, "left_front_leg", 4.5F, -8.0F);
        addLeg(root, "right_front_leg", -4.5F, -8.0F);
        addLeg(root, "left_hind_leg", 4.5F, 8.0F);
        addLeg(root, "right_hind_leg", -4.5F, 8.0F);
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void addLeg(PartDefinition root, String name, float x, float z) {
        boolean left = x > 0;
        PartDefinition leg = root.addOrReplaceChild(name, CubeListBuilder.create().mirror(left).texOffs(20, 54).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(x, 16.0F, z));
        leg.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(40, 54).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 4.0F, 0.0F));
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks, seed = state.entityId * 0.37F;
        ModelPart[] front = {leftFrontLeg, rightFrontLeg}, hind = {leftHindLeg, rightHindLeg};
        horns.visible = state.showsHorns;
        if (state.isBaby) {
            GiantPose.scale(head, 1.35F);
            leftSkirt.yScale = rightSkirt.yScale = 0.6F;
        }

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(front, hind);
            neck.xRot = 0.05F;
            head.xRot = 0.1F;
            leftSkirt.xRot = rightSkirt.xRot = -1.2F;
            leftEar.xRot = rightEar.xRot = 0.6F;
            tail.xRot = -1.9F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            GiantPose.sag(leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
            neck.xRot = 0.3F;
            neck.yRot = -0.35F;
            head.xRot = 0.35F;
            head.yRot = state.yRot * DEG * 0.6F;
            leftSkirt.zRot = -0.5F;
            rightSkirt.zRot = 0.5F;
            leftEar.zRot = 1.1F;
            rightEar.zRot = -1.1F;
            tail.xRot = 0.6F;
            return;
        }
        if (state.dozing) {
            GiantPose.lie(body, LIE_DROP, LIE_LEG_Y, age, front, hind);
            neck.xRot = 0.55F;
            neck.yRot = 0.25F;
            head.xRot = 0.6F; // chin and horn resting on the ground
            leftSkirt.zRot = -1.1F;
            rightSkirt.zRot = 1.1F;
            leftEar.zRot = 0.8F;
            rightEar.zRot = -0.8F;
            tail.xRot = 0.5F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = GiantPose.walk(state.walkAnimationPos, speed, 0.65F, 0.75F, leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
        body.y += Mth.cos(phase * 2.0F) * 0.35F * speed;
        body.zRot = Mth.sin(phase) * 0.03F * speed;
        leftSkirt.xRot = rightSkirt.xRot = Mth.sin(phase * 2.0F) * 0.05F * speed;

        float yaw = Mth.clamp(state.yRot, -40.0F, 40.0F) * DEG;
        float pitch = Mth.clamp(state.xRot, -20.0F, 20.0F) * DEG;
        float neckX = NECK_X + pitch * 0.3F + Mth.cos(phase * 2.0F) * 0.06F * speed, headX = HEAD_X + pitch * 0.4F;
        float graze = state.grazeAmount;
        if (state.charging) {
            // horn levelled at the target, head tossing
            neckX = 0.35F;
            headX = 0.9F + Mth.sin(age * 0.6F) * 0.1F;
            yaw *= 0.3F;
        } else if (graze > 0.0F) {
            neckX = Mth.lerp(graze, neckX, GRAZE_NECK_X);
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.5F) * 0.05F);
            yaw *= 1.0F - graze;
        }
        neck.xRot = neckX;
        neck.yRot = yaw * 0.4F;
        head.xRot = headX;
        head.yRot = yaw * 0.6F;

        float pinned = state.charging ? 0.7F : 0.0F;
        leftEar.xRot = -0.4F - pinned + GiantPose.pulse(age * 0.05F + seed) * Mth.sin(age * 1.5F) * 0.4F;
        rightEar.xRot = -0.4F - pinned + GiantPose.pulse(age * 0.043F + seed + 2.0F) * Mth.sin(age * 1.7F) * 0.4F;
        tail.zRot = GiantPose.pulse(age * 0.07F + seed * 1.3F) * Mth.sin(age * 0.9F) * 0.5F;
        tail.xRot = TAIL_X + speed * 0.4F + (state.charging ? 0.9F : 0.0F);
    }
}
