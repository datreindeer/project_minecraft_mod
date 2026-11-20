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
 * Steppe bison: massive woolly forequarters under a tall hump, slim hindquarters, a big shaggy head hung low with a beard,
 * and horns that curve out and up (long and thick on the bull, slim on the cow, whose hump is lower). Shaggy "chaps" on
 * the forelegs, a tufted tail. The calf is the model scaled down with a bigger head and no horns.
 * Textures: textures/entity/wildlife/giants/steppe_bison_{bull,cow,calf}.png (128x64), painted by
 * scripts/gen_fauna_giants_textures.py whose BISON table mirrors the cubes below.
 */
public class BisonModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "steppe_bison"), "main");
    private static final float HEAD_X = 0.5F, GRAZE_HEAD_X = 1.05F, CHARGE_HEAD_X = 1.2F, TAIL_X = 0.1F;
    private static final float LIE_DROP = 7.0F, LIE_LEG_Y = 22.0F;

    private final ModelPart body, hump, head, bullHorns, cowHorns, tail, leftEar, rightEar, beard;
    private final ModelPart leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg;

    public BisonModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        hump = body.getChild("hump");
        head = body.getChild("head");
        bullHorns = head.getChild("bull_horns");
        cowHorns = head.getChild("cow_horns");
        beard = head.getChild("beard");
        tail = body.getChild("tail");
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
        // big woolly forequarters, slimmer hindquarters behind
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.0F, -8.0F, -12.0F, 14.0F, 15.0F, 11.0F)
                .texOffs(50, 0).addBox(-5.5F, -4.0F, -2.0F, 11.0F, 10.0F, 12.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        body.addOrReplaceChild("hump", CubeListBuilder.create().texOffs(0, 26).addBox(-5.0F, -4.0F, -4.0F, 10.0F, 5.0F, 8.0F),
                PartPose.offset(0.0F, -8.0F, -7.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                .texOffs(74, 26).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 7.0F, 1.0F)
                .texOffs(120, 26).addBox(-1.0F, 6.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 10.0F, TAIL_X, 0.0F, 0.0F));
        // the head hangs low in front of the hump: woolly skull, muzzle in front, beard under the chin
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(96, 0).addBox(-4.5F, -4.5F, -6.0F, 9.0F, 9.0F, 7.0F)
                .texOffs(78, 26).addBox(-2.5F, -1.5F, -9.0F, 5.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 3.0F, -12.0F, HEAD_X, 0.0F, 0.0F));
        head.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(106, 26).addBox(-2.0F, 0.0F, -1.5F, 4.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 4.0F, -5.5F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(16, 39).mirror().addBox(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(5.0F, -0.5F, -2.0F, 0.0F, 0.0F, 0.4F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(16, 39).addBox(-1.5F, -0.5F, -1.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(-5.0F, -0.5F, -2.0F, 0.0F, 0.0F, -0.4F));
        PartDefinition bull = head.addOrReplaceChild("bull_horns", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition cow = head.addOrReplaceChild("cow_horns", CubeListBuilder.create(), PartPose.ZERO);
        for (float side : new float[] {1.0F, -1.0F}) {
            String p = side > 0 ? "left_" : "right_";
            // built along -y and bent at the root: out sideways from the poll, then up and a little forward
            PartDefinition b = bull.addOrReplaceChild(p + "horn", CubeListBuilder.create().texOffs(0, 39).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                    PartPose.offsetAndRotation(4.0F * side, -3.0F, -3.0F, 0.0F, 0.0F, 1.35F * side));
            b.addOrReplaceChild(p + "horn_tip", CubeListBuilder.create().texOffs(12, 39).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, -2.5F, 0.0F, 0.35F, 0.0F, -1.2F * side));
            PartDefinition c = cow.addOrReplaceChild(p + "horn", CubeListBuilder.create().texOffs(8, 39).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                    PartPose.offsetAndRotation(3.8F * side, -3.0F, -3.0F, 0.0F, 0.0F, 1.2F * side));
            c.addOrReplaceChild(p + "horn_tip", CubeListBuilder.create().texOffs(12, 39).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, -2.5F, 0.0F, 0.25F, 0.0F, -1.0F * side));
        }
        addLeg(root, "left_front_leg", 3.5F, -7.0F, false);
        addLeg(root, "right_front_leg", -3.5F, -7.0F, false);
        addLeg(root, "left_hind_leg", 3.0F, 7.5F, true);
        addLeg(root, "right_hind_leg", -3.0F, 7.5F, true);
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void addLeg(PartDefinition root, String name, float x, float z, boolean hind) {
        boolean left = x > 0;
        CubeListBuilder upper = hind
                ? CubeListBuilder.create().mirror(left).texOffs(56, 26).addBox(-2.0F, -1.0F, -2.5F, 4.0F, 6.0F, 5.0F)
                : CubeListBuilder.create().mirror(left).texOffs(36, 26).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 6.0F, 5.0F);
        PartDefinition leg = root.addOrReplaceChild(name, upper, PartPose.offset(x, 14.0F, z));
        leg.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(94, 26).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 5.0F, 3.0F),
                PartPose.offset(0.0F, 5.0F, 0.0F));
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks, seed = state.entityId * 0.37F;
        ModelPart[] front = {leftFrontLeg, rightFrontLeg}, hind = {leftHindLeg, rightHindLeg};
        bullHorns.visible = state.showsHorns;
        cowHorns.visible = !state.showsHorns && !state.isBaby;
        if (!state.showsHorns) hump.yScale = 0.7F; // the cow's hump is lower
        if (state.isBaby) {
            GiantPose.scale(head, 1.35F);
            beard.visible = false;
            hump.yScale = 0.5F;
        }

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(front, hind);
            head.xRot = 0.15F;
            leftEar.yRot = 1.0F;
            rightEar.yRot = -1.0F;
            tail.xRot = -1.9F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            GiantPose.sag(leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
            head.xRot = 0.5F;
            head.yRot = state.yRot * DEG * 0.7F;
            leftEar.zRot = 1.2F;
            rightEar.zRot = -1.2F;
            tail.xRot = 0.6F;
            return;
        }
        if (state.dozing) {
            GiantPose.lie(body, LIE_DROP, LIE_LEG_Y, age, front, hind);
            head.xRot = 0.85F;
            head.yRot = 0.3F;
            head.zRot = 0.12F;
            leftEar.zRot = 0.8F;
            rightEar.zRot = -0.8F;
            tail.xRot = 0.5F;
            tail.zRot = 0.4F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = GiantPose.walk(state.walkAnimationPos, speed, 0.6F, 0.85F, leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
        body.y += Mth.cos(phase * 2.0F) * 0.4F * speed;
        body.xRot = Mth.sin(phase * 2.0F) * 0.02F * speed; // the heavy front rocks over the forelegs

        float yaw = Mth.clamp(state.yRot, -45.0F, 45.0F) * DEG;
        float pitch = Mth.clamp(state.xRot, -20.0F, 20.0F) * DEG;
        float headX = HEAD_X + pitch * 0.5F + Mth.cos(phase * 2.0F) * 0.07F * speed;
        float graze = state.grazeAmount;
        if (state.charging) {
            headX = CHARGE_HEAD_X + Mth.sin(age * 0.5F) * 0.08F; // forehead down, horns hooking
            yaw *= 0.3F;
        } else if (graze > 0.0F) {
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.5F) * 0.05F);
            yaw *= 1.0F - graze;
        }
        head.xRot = headX;
        head.yRot = yaw;
        beard.xRot = -headX * 0.4F + Mth.sin(phase) * 0.1F * speed; // hangs down whatever the head does

        float pinned = state.charging ? 0.6F : 0.0F;
        leftEar.zRot = 0.4F - GiantPose.pulse(age * 0.05F + seed) * Mth.sin(age * 1.6F) * 0.4F - pinned;
        rightEar.zRot = -0.4F + GiantPose.pulse(age * 0.041F + seed + 2.0F) * Mth.sin(age * 1.8F) * 0.4F + pinned;
        tail.zRot = GiantPose.pulse(age * 0.07F + seed * 1.3F) * Mth.sin(age * 0.9F) * 0.55F + Mth.sin(phase) * 0.1F * speed;
        tail.xRot = TAIL_X + speed * 0.6F + (state.charging ? 0.9F : 0.0F); // tail up when running or charging
    }
}
