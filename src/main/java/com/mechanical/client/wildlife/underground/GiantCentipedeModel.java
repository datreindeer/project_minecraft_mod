package com.mechanical.client.wildlife.underground;

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
 * Giant centipede: a flat armoured head with long antennae and a pair of hooked venom claws (forcipules), then a chain of
 * {@link #SEGMENTS} plated segments, each hinged on the one before and carrying a pair of legs, and a short tail with two
 * trailing cerci. Walking, a gentle wave runs down the body and the legs ripple front to back (each pair a little behind
 * the one before); attacking, the head rears and the claws open. As a carcass it lies curled with the legs folded in; hung
 * it hangs straight. Wall and ceiling poses are whole-body turns in the renderer (UndergroundClient).
 * Texture 64x32: textures/entity/wildlife/giant_centipede.png, painted by scripts/gen_fauna_underground_textures.py
 * (its UV table mirrors the texOffs here).
 */
public class GiantCentipedeModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "giant_centipede"), "main");
    static final int SEGMENTS = 8;

    private final ModelPart head, leftClaw, rightClaw, leftAntenna, rightAntenna, tail;
    private final ModelPart[] seg = new ModelPart[SEGMENTS], leftLeg = new ModelPart[SEGMENTS], rightLeg = new ModelPart[SEGMENTS];

    public GiantCentipedeModel(ModelPart root) {
        super(root);
        seg[0] = root.getChild("seg0");
        for (int i = 1; i < SEGMENTS; i++) seg[i] = seg[i - 1].getChild("seg" + i);
        for (int i = 0; i < SEGMENTS; i++) {
            leftLeg[i] = seg[i].getChild("left_leg");
            rightLeg[i] = seg[i].getChild("right_leg");
        }
        head = seg[0].getChild("head");
        leftClaw = head.getChild("left_claw");
        rightClaw = head.getChild("right_claw");
        leftAntenna = head.getChild("left_antenna");
        rightAntenna = head.getChild("right_antenna");
        tail = seg[SEGMENTS - 1].getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parent = mesh.getRoot();
        PartDefinition first = null;
        for (int i = 0; i < SEGMENTS; i++) {
            PartDefinition s = parent.addOrReplaceChild("seg" + i, CubeListBuilder.create().texOffs(0, 6).addBox(-2.0F, -1.0F, 0.0F, 4.0F, 2.0F, 2.0F),
                    i == 0 ? PartPose.offset(0.0F, 21.0F, -8.0F) : PartPose.offset(0.0F, 0.0F, 2.0F));
            s.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 0).addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(2.0F, 0.5F, 1.0F, 0.0F, 0.0F, 0.85F));
            s.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 0).mirror().addBox(-3.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(-2.0F, 0.5F, 1.0F, 0.0F, 0.0F, -0.85F));
            if (i == 0) first = s;
            parent = s;
        }
        parent.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 11).addBox(-1.5F, -0.5F, 0.0F, 3.0F, 1.0F, 2.0F)
                        .texOffs(40, 0).addBox(-1.5F, -0.5F, 2.0F, 1.0F, 1.0F, 3.0F)
                        .texOffs(40, 0).mirror().addBox(0.5F, -0.5F, 2.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 2.0F));
        PartDefinition head = first.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -1.0F, -3.0F, 4.0F, 2.0F, 3.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("left_antenna", CubeListBuilder.create().texOffs(28, 0).addBox(-0.5F, -0.5F, -5.0F, 1.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(1.2F, -0.5F, -3.0F, 0.3F, -0.45F, 0.0F));
        head.addOrReplaceChild("right_antenna", CubeListBuilder.create().texOffs(28, 0).mirror().addBox(-0.5F, -0.5F, -5.0F, 1.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(-1.2F, -0.5F, -3.0F, 0.3F, 0.45F, 0.0F));
        head.addOrReplaceChild("left_claw", CubeListBuilder.create().texOffs(16, 3).addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(1.2F, 0.5F, -2.5F, 0.0F, 0.5F, 0.0F));
        head.addOrReplaceChild("right_claw", CubeListBuilder.create().texOffs(16, 3).mirror().addBox(-0.5F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F),
                PartPose.offsetAndRotation(-1.2F, 0.5F, -2.5F, 0.0F, -0.5F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        if (CarcassRenderer.isHung(state)) return; // straight down from the tail
        if (CarcassRenderer.isCarcass(state)) {
            // curled on its side, legs folded in
            for (int i = 1; i < SEGMENTS; i++) seg[i].yRot = 0.32F;
            for (int i = 0; i < SEGMENTS; i++) {
                leftLeg[i].zRot = -0.4F;
                rightLeg[i].zRot = 0.4F;
            }
            tail.yRot = 0.4F;
            return;
        }
        float t = state.ageInTicks;
        float speed = Math.min(state.walkAnimationSpeed * 1.5F, 1.0F);
        float phase = state.walkAnimationPos * 1.2F + t * 0.03F;
        float amp = 0.04F + 0.14F * speed, prev = 0.0F;
        for (int i = 0; i < SEGMENTS; i++) {
            float theta = amp * Mth.sin(phase - i * 0.9F);
            if (i > 0) seg[i].yRot = theta - prev;
            prev = theta;
            // the legs ripple front to back: each pair a beat behind the one before, left and right opposite
            float step = Mth.sin(state.walkAnimationPos * 2.4F - i * 0.8F) * 0.6F * speed;
            leftLeg[i].yRot = step;
            rightLeg[i].yRot = step;
            float lift = Math.max(0.0F, Mth.cos(state.walkAnimationPos * 2.4F - i * 0.8F)) * 0.35F * speed;
            leftLeg[i].zRot = 0.85F - lift;
            rightLeg[i].zRot = -0.85F + lift;
        }
        tail.yRot = -prev * 0.5F;
        // antennae feel about; head follows the gaze
        leftAntenna.yRot = -0.45F + Mth.sin(t * 0.17F) * 0.15F;
        rightAntenna.yRot = 0.45F + Mth.sin(t * 0.17F + 1.3F) * 0.15F;
        leftAntenna.xRot = rightAntenna.xRot = 0.3F + Mth.sin(t * 0.11F) * 0.1F;
        head.yRot = Mth.clamp(state.yRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
        float open = state.charging ? 0.35F + Mth.abs(Mth.sin(t * 0.5F)) * 0.35F : 0.0F;
        leftClaw.yRot = 0.5F + open;
        rightClaw.yRot = -0.5F - open;
        if (state.charging) {
            seg[0].xRot = -0.25F; // fore-body reared a little, head up
            seg[0].y -= 0.5F;
            head.xRot = 0.15F;
        }
    }
}
