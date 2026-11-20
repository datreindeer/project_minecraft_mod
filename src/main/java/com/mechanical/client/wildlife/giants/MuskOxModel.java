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
 * Musk ox: a squat, humped body almost hidden under a curtain of long guard hair that hangs nearly to the ground on all
 * sides, short pale-stockinged legs below it, and a low head with a shaggy chin. The horns meet in a broad boss across
 * the forehead, sweep down beside the face and hook forward and up at the tips. The calf is the model scaled down with a
 * bigger head, shorter hair and no horns.
 * Texture: textures/entity/wildlife/giants/musk_ox.png (128x64), painted by scripts/gen_fauna_giants_textures.py whose
 * MUSK_OX table mirrors the cubes below.
 */
public class MuskOxModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "musk_ox"), "main");
    private static final float HEAD_X = 0.6F, GRAZE_HEAD_X = 1.1F, GUARD_HEAD_X = 0.95F;
    private static final float LIE_DROP = 6.0F, LIE_LEG_Y = 22.0F;

    private final ModelPart body, head, horns, leftSkirt, rightSkirt, rearSkirt, frontSkirt, chin;
    private final ModelPart leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg;

    public MuskOxModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        horns = head.getChild("horns");
        chin = head.getChild("chin");
        leftSkirt = body.getChild("left_skirt");
        rightSkirt = body.getChild("right_skirt");
        rearSkirt = body.getChild("rear_skirt");
        frontSkirt = body.getChild("front_skirt");
        leftFrontLeg = root.getChild("left_front_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightHindLeg = root.getChild("right_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(42, 0).addBox(-6.0F, -5.0F, -10.0F, 12.0F, 10.0F, 20.0F)
                .texOffs(0, 31).addBox(-5.0F, -8.0F, -10.0F, 10.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, 10.0F, 0.0F));
        // the curtain of guard hair, hanging from mid-flank nearly to the ground on all four sides
        body.addOrReplaceChild("left_skirt", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, -10.0F, 1.0F, 11.0F, 20.0F),
                PartPose.offset(6.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("right_skirt", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -10.0F, 1.0F, 11.0F, 20.0F),
                PartPose.offset(-6.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("rear_skirt", CubeListBuilder.create().texOffs(36, 31).addBox(-6.0F, 0.0F, 0.0F, 12.0F, 11.0F, 1.0F),
                PartPose.offset(0.0F, 0.0F, 10.0F));
        body.addOrReplaceChild("front_skirt", CubeListBuilder.create().texOffs(86, 31).addBox(-5.0F, 0.0F, -1.0F, 10.0F, 9.0F, 1.0F),
                PartPose.offset(0.0F, 2.0F, -10.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(62, 31).addBox(-3.0F, -3.0F, -6.0F, 6.0F, 6.0F, 6.0F)
                .texOffs(0, 43).addBox(-2.0F, -1.0F, -9.0F, 4.0F, 4.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, -10.0F, HEAD_X, 0.0F, 0.0F));
        head.addOrReplaceChild("chin", CubeListBuilder.create().texOffs(108, 31).addBox(-2.5F, 0.0F, -2.0F, 5.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 2.0F, -6.0F));
        // the horn boss across the forehead; each horn runs down beside the face (+y) and hooks forward and up
        PartDefinition horns = head.addOrReplaceChild("horns", CubeListBuilder.create().texOffs(38, 43).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 2.0F, 4.0F),
                PartPose.ZERO);
        for (float side : new float[] {1.0F, -1.0F}) {
            String p = side > 0 ? "left_" : "right_";
            PartDefinition a = horns.addOrReplaceChild(p + "horn", CubeListBuilder.create().texOffs(62, 43).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                    PartPose.offsetAndRotation(3.5F * side, -3.0F, -2.0F, -0.2F, 0.0F, -0.45F * side));
            a.addOrReplaceChild(p + "horn_tip", CubeListBuilder.create().texOffs(70, 43).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, 3.5F, 0.0F, -2.2F, 0.0F, -0.5F * side));
        }
        addLeg(root, "left_front_leg", 3.5F, -6.0F);
        addLeg(root, "right_front_leg", -3.5F, -6.0F);
        addLeg(root, "left_hind_leg", 3.5F, 6.0F);
        addLeg(root, "right_hind_leg", -3.5F, 6.0F);
        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void addLeg(PartDefinition root, String name, float x, float z) {
        boolean left = x > 0;
        PartDefinition leg = root.addOrReplaceChild(name, CubeListBuilder.create().mirror(left).texOffs(14, 43).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(x, 17.0F, z));
        leg.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(26, 43).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 3.0F, 0.0F));
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        ModelPart[] front = {leftFrontLeg, rightFrontLeg}, hind = {leftHindLeg, rightHindLeg};
        ModelPart[] skirts = {leftSkirt, rightSkirt, rearSkirt, frontSkirt};
        horns.visible = state.showsHorns;
        if (state.isBaby) {
            GiantPose.scale(head, 1.3F);
            for (ModelPart s : skirts) s.yScale = 0.6F;
            chin.visible = false;
        }

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(front, hind);
            head.xRot = 0.1F;
            // the long hair falls towards the ground (-z here)
            leftSkirt.xRot = rightSkirt.xRot = -1.2F;
            frontSkirt.xRot = -0.4F;
            rearSkirt.xRot = -1.5F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            GiantPose.sag(leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
            head.xRot = 0.5F;
            head.yRot = state.yRot * DEG * 0.7F;
            leftSkirt.zRot = -0.6F;
            rightSkirt.zRot = 0.6F;
            return;
        }
        if (state.dozing) {
            GiantPose.lie(body, LIE_DROP, LIE_LEG_Y, age, front, hind);
            head.xRot = 0.9F;
            head.yRot = 0.25F;
            // the hair spreads out on the ground around it
            for (ModelPart s : skirts) s.yScale *= 0.5F;
            leftSkirt.zRot = -0.9F;
            rightSkirt.zRot = 0.9F;
            frontSkirt.xRot = 0.8F;
            rearSkirt.xRot = -0.8F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = GiantPose.walk(state.walkAnimationPos, speed, 0.8F, 0.8F, leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg);
        body.y += Mth.cos(phase * 2.0F) * 0.3F * speed;
        // the curtain swings with every step
        float swing = Mth.sin(phase * 2.0F) * 0.07F * speed;
        leftSkirt.zRot = -Math.abs(swing);
        rightSkirt.zRot = Math.abs(swing);
        frontSkirt.xRot = swing - speed * 0.15F;
        rearSkirt.xRot = -swing + speed * 0.2F;

        float yaw = Mth.clamp(state.yRot, -40.0F, 40.0F) * DEG;
        float pitch = Mth.clamp(state.xRot, -20.0F, 20.0F) * DEG;
        float headX = HEAD_X + pitch * 0.5F + Mth.cos(phase * 2.0F) * 0.06F * speed;
        float graze = state.grazeAmount;
        if (state.charging) {
            // in the ring: head down, horns out, swinging now and then
            headX = GUARD_HEAD_X + GiantPose.pulse(age * 0.08F + state.entityId) * 0.25F;
            yaw *= 0.4F;
            head.zRot = Mth.sin(age * 0.15F + state.entityId) * 0.08F;
        } else if (graze > 0.0F) {
            headX = Mth.lerp(graze, headX, GRAZE_HEAD_X + Mth.sin(age * 0.5F) * 0.05F);
            yaw *= 1.0F - graze;
        }
        head.xRot = headX;
        head.yRot = yaw;
        chin.xRot = -headX * 0.5F;
    }
}
