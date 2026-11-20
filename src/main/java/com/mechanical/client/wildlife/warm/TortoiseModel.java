package com.mechanical.client.wildlife.warm;

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
 * Tortoise: a high-domed carapace stepped up from the marginal rim in four tiers, a flat plastron underneath, a scaly head
 * on a long neck that reaches out of the shell, four stumpy elephantine legs and a tiny tail. Plods with one leg lifted at
 * a time and the shell rocking; withdrawn ({@link WarmRenderState#shell}) head, neck, legs and tail slide into the shell
 * and it settles flat on the ground. Babies use this model scaled down.
 * Texture 128x64: textures/entity/wildlife/tortoise.png, painted by scripts/gen_fauna_warm_tex_dry.py whose TORTOISE
 * table mirrors the cubes below (keep them in sync).
 */
public class TortoiseModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "tortoise"), "main");
    /** How far (px) the shell sinks when withdrawn: the plastron (bottom at y 21.5) comes to rest on the ground. */
    private static final float SHELL_DROP = 2.5F;
    private static final float NECK_LIFT = -0.25F;

    private final ModelPart body, neck, head, tail;
    private final ModelPart rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;
    private final ModelPart[] legs;

    public TortoiseModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.tail = body.getChild("tail");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        // walking order of a slow four-beat walk: right hind, right front, left hind, left front
        this.legs = new ModelPart[] {rightHindLeg, rightFrontLeg, leftHindLeg, leftFrontLeg};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -0.5F, -6.5F, 10.0F, 1.0F, 13.0F)    // plastron
                        .texOffs(0, 14).addBox(-6.0F, -3.0F, -7.5F, 12.0F, 3.0F, 15.0F)   // marginal rim
                        .texOffs(0, 32).addBox(-5.5F, -5.0F, -7.0F, 11.0F, 2.0F, 14.0F)   // costal tier
                        .texOffs(0, 48).addBox(-4.5F, -7.0F, -6.0F, 9.0F, 2.0F, 12.0F)    // upper dome
                        .texOffs(54, 14).addBox(-3.0F, -8.0F, -4.5F, 6.0F, 1.0F, 9.0F),   // crown (vertebral scutes)
                PartPose.offset(0.0F, 21.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(46, 0).addBox(-1.5F, -1.5F, -4.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, -6.5F, NECK_LIFT, 0.0F, 0.0F));
        neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(60, 0).addBox(-1.5F, -2.0F, -4.0F, 3.0F, 3.0F, 4.0F)
                        .texOffs(74, 0).addBox(-1.0F, -1.5F, -5.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -0.5F, -4.0F, -NECK_LIFT, 0.0F, 0.0F));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(104, 0).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, 6.5F, -0.5F, 0.0F, 0.0F));
        for (float x : new float[] {-4.0F, 4.0F}) {
            boolean left = x > 0;
            // front legs splay out a little, hind legs stand like columns
            root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(80, 0).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 4.0F, 3.0F),
                    PartPose.offsetAndRotation(x, 20.5F, -4.5F, 0.0F, 0.0F, left ? -0.12F : 0.12F));
            root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(92, 0).addBox(-1.5F, -0.5F, -1.5F, 3.0F, 4.0F, 3.0F),
                    PartPose.offset(x * 0.95F, 20.5F, 4.5F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 0.0F; // neck and head dangle straight down out of the shell
            head.xRot = 0.15F;
            tail.xRot = -0.2F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            // shell upright, the limbs limp and splayed, the neck lolling out to the side
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            neck.xRot = 0.35F;
            neck.yRot = state.yRot * Mth.DEG_TO_RAD * 0.6F;
            head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.4F;
            head.xRot = 0.3F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed * 2.5F, 1.0F);
        float phase = state.walkAnimationPos * 0.9F;
        // one leg at a time: each swings forward while lifted, then pushes back on the ground
        for (int i = 0; i < 4; i++) {
            float p = phase + i * Mth.HALF_PI;
            ModelPart leg = legs[i];
            leg.xRot = -Mth.sin(p) * 0.45F * speed;
            leg.y -= Math.max(0.0F, Mth.cos(p)) * 1.0F * speed;
        }
        // the heavy shell rocks onto the supporting side and bobs at each footfall
        body.zRot = Mth.sin(phase) * 0.045F * speed;
        body.yRot = Mth.sin(phase * 0.5F) * 0.03F * speed;
        body.y += Mth.abs(Mth.sin(phase * 2.0F)) * 0.25F * speed;

        // head: follows the look, bobs with the stride, and now and then a slow look around
        float look = Mth.sin(age * 0.011F + state.entityId) > 0.55F ? Mth.sin(age * 0.045F) * 0.6F : 0.0F;
        neck.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD * 0.6F + look * (1.0F - speed);
        head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD * 0.4F;
        head.xRot += Mth.clamp(state.xRot, -25.0F, 25.0F) * Mth.DEG_TO_RAD * 0.5F;
        neck.xRot += Mth.sin(phase * 2.0F) * 0.06F * speed;
        neck.z -= Mth.sin(phase * 2.0F + 0.6F) * 0.3F * speed; // the neck reaching with each step

        // nibbling: neck stretched down, head working at the plants
        float g = state.grazeAmount;
        if (g > 0) {
            neck.xRot = Mth.lerp(g, neck.xRot, 0.55F);
            head.xRot = Mth.lerp(g, head.xRot, 0.35F + Math.max(0.0F, Mth.sin(age * 0.5F)) * 0.15F);
            neck.yRot *= 1.0F - g * 0.7F;
        }

        // withdrawn into the shell: everything slides in, the shell settles flat on the ground
        float s = state instanceof WarmRenderState w ? w.shell : 0.0F;
        if (s > 0) {
            body.y += SHELL_DROP * s;
            body.zRot *= 1.0F - s;
            neck.z += 6.0F * s;
            neck.y += 0.5F * s;
            neck.xRot = Mth.lerp(s, neck.xRot, 0.0F);
            neck.yRot *= 1.0F - s;
            head.z += 2.5F * s;
            head.xRot = Mth.lerp(s, head.xRot, 0.0F);
            head.yRot *= 1.0F - s;
            tail.z -= 2.0F * s;
            for (ModelPart leg : legs) {
                leg.y += (SHELL_DROP - 3.5F) * s; // up into the shell as it sinks
                leg.x *= 1.0F - 0.25F * s;
                leg.xRot *= 1.0F - s;
                leg.zRot *= 1.0F - s;
            }
        }
    }
}
