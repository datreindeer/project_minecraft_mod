package com.mechanical.client.wildlife.savanna;

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
 * Ostrich: a big rounded body (back at y 4) with drooping wing plumes and a bushy tail plume, a long bare neck in two
 * segments carrying a small flat head with a broad beak, and two long legs - thick feathered thighs, bare shanks, two-toed
 * feet. About 2 blocks to the top of the head. Strides with the neck swinging, runs with the wings half open, kicks
 * forward with one leg (state.kick), sits on its folded legs to sleep, and lies limp as a carcass. Chicks are the adult
 * scaled down. Texture 64x64: textures/entity/wildlife/ostrich_*.png, painted by scripts/gen_fauna_savanna_textures.py
 * whose OSTRICH table mirrors the cubes below (keep them in sync).
 */
public class OstrichModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "ostrich"), "main");
    private static final float NECK_REST = 0.15F, UPPER_REST = -0.1F, HEAD_REST = -0.05F;

    private final ModelPart body, neck, upperNeck, head, rightWing, leftWing, tail, rightLeg, leftLeg;

    public OstrichModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.upperNeck = neck.getChild("upper_neck");
        this.head = upperNeck.getChild("head");
        this.rightWing = body.getChild("right_wing");
        this.leftWing = body.getChild("left_wing");
        this.tail = body.getChild("tail");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 11.0F),
                PartPose.offset(0.0F, 8.0F, 1.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(38, 0).addBox(-3.0F, -2.0F, 0.0F, 6.0F, 4.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, 4.5F, 0.35F, 0.0F, 0.0F));
        for (float x : new float[] {-4.0F, 4.0F}) {
            boolean left = x > 0;
            body.addOrReplaceChild(left ? "left_wing" : "right_wing",
                    CubeListBuilder.create().texOffs(38, 7).mirror(left).addBox(left ? 0.0F : -1.0F, 0.0F, -4.0F, 1.0F, 5.0F, 8.0F),
                    PartPose.offset(x, -3.0F, 0.5F));
        }
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 19).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -2.5F, -5.0F, NECK_REST, 0.0F, 0.0F));
        PartDefinition upper = neck.addOrReplaceChild("upper_neck", CubeListBuilder.create().texOffs(8, 19).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -5.8F, 0.0F, UPPER_REST, 0.0F, 0.0F));
        upper.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(16, 19).addBox(-1.5F, -2.5F, -2.5F, 3.0F, 3.0F, 4.0F)
                        .texOffs(40, 20).addBox(-1.0F, -1.0F, -5.5F, 2.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, HEAD_REST, 0.0F, 0.0F));
        for (float x : new float[] {-2.0F, 2.0F}) {
            boolean left = x > 0;
            PartDefinition leg = root.addOrReplaceChild(left ? "left_leg" : "right_leg",
                    CubeListBuilder.create().texOffs(0, 27).mirror(left).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 5.0F, 3.0F),
                    PartPose.offset(x, 11.0F, 1.0F));
            leg.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left)
                            .texOffs(12, 27).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F)
                            .texOffs(16, 27).addBox(-1.0F, 8.0F, -3.0F, 2.0F, 1.0F, 4.0F),
                    PartPose.offset(0.0F, 4.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        ModelPart[] legs = {rightLeg, leftLeg};

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[0], legs);
            neck.xRot = 1.6F; // the long neck hangs straight down, head limp
            upperNeck.xRot = 0.0F;
            head.xRot = 0.3F;
            rightWing.zRot = 0.4F;
            leftWing.zRot = -0.4F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            neck.xRot = 1.3F; // neck along the ground, sagging
            neck.zRot = -CarcassRenderer.LIMB_SAG * 0.8F;
            upperNeck.xRot = 0.2F;
            head.xRot = 0.1F;
            leftWing.zRot = -0.6F;
            return;
        }
        if (state.dozing) {
            // sitting on the folded legs: body on the ground, neck curved up, head drooping
            body.y += 8.0F + Mth.sin(age * 0.07F) * 0.15F;
            for (ModelPart leg : legs) {
                leg.y += 8.0F;
                leg.xRot = -1.45F;
                leg.getChild("lower").xRot = 2.9F;
            }
            neck.xRot = 0.5F;
            upperNeck.xRot = -0.9F;
            head.xRot = 0.7F;
            return;
        }

        float pos = state.walkAnimationPos, speed = Math.min(state.walkAnimationSpeed, 1.4F);
        float run = Mth.clamp((speed - 0.5F) / 0.5F, 0.0F, 1.0F);
        float phase = pos * 0.55F;
        // two-legged stride: the thigh swings, the shank trails and snaps forward
        for (int i = 0; i < 2; i++) {
            float c = Mth.cos(phase + i * Mth.PI);
            legs[i].xRot = c * speed * 0.9F;
            legs[i].getChild("lower").xRot = Math.max(0.0F, -Mth.sin(phase + i * Mth.PI)) * speed * 1.1F;
        }
        body.y -= Mth.abs(Mth.sin(phase)) * 1.2F * speed;
        body.xRot = 0.08F * run;
        // the neck pumps with each step, reaching forward when running
        neck.xRot = NECK_REST + Mth.sin(phase * 2.0F) * 0.06F * speed + run * 0.55F;
        upperNeck.xRot = UPPER_REST - run * 0.35F;
        head.xRot = HEAD_REST - run * 0.25F + Mth.clamp(state.xRot, -30.0F, 30.0F) * 0.5F * Mth.DEG_TO_RAD;
        neck.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * 0.5F * Mth.DEG_TO_RAD;
        head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * 0.5F * Mth.DEG_TO_RAD;
        // wings out for balance at a run, and a lazy flap now and then
        float flap = run * 0.7F + Mth.sin(age * 0.05F) * 0.04F;
        rightWing.zRot = flap;
        leftWing.zRot = -flap;
        tail.xRot = 0.35F + run * 0.3F + Mth.sin(age * 0.09F) * 0.05F;

        // pecking at the ground
        if (state.grazeAmount > 0) {
            float g = state.grazeAmount;
            neck.xRot = Mth.lerp(g, neck.xRot, 1.9F);
            upperNeck.xRot = Mth.lerp(g, upperNeck.xRot, 0.6F);
            head.xRot = Mth.lerp(g, head.xRot, -0.8F + Mth.sin(age * 0.9F) * 0.1F);
        }
        // the forward kick: one leg thrown out in front, shank straight, the body rocking back
        float kick = state instanceof SavannaAnimalRenderer.KickState k ? k.kick : 0.0F;
        if (kick > 0) {
            rightLeg.xRot = Mth.lerp(kick, rightLeg.xRot, -1.5F);
            rightLeg.getChild("lower").xRot = Mth.lerp(kick, rightLeg.getChild("lower").xRot, -0.2F);
            body.xRot -= 0.2F * kick;
            neck.xRot -= 0.3F * kick;
            rightWing.zRot = leftWing.zRot * -1 + 0.6F * kick;
            leftWing.zRot = -rightWing.zRot;
        }
    }
}
