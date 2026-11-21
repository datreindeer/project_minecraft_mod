package com.mechanical.client.wildlife.underground;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.wildlife.underground.Lindworm;

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
 * Lindworm: a horned, long-jawed head, {@link #BODY} thick body segments with a spine ridge (the second carries the two
 * clawed forelegs), {@link #TAIL} thinner tail segments and a whip-thin tip, each hinged on the one before. It slithers
 * with a wave running down the body; hunting it rears its head and gapes; lunging it stretches out straight, jaws wide;
 * coiled round its prey every hinge bends the same way and the body winds into a ring. As a carcass it lies in a loose
 * S. Texture 128x64: textures/entity/wildlife/lindworm.png, painted by scripts/gen_fauna_underground_textures.py.
 */
public class LindwormModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "lindworm"), "main");
    static final int BODY = 7, TAIL = 4, ALL = BODY + TAIL + 1;

    private final ModelPart head, jaw, rightLeg, leftLeg;
    private final ModelPart[] seg = new ModelPart[ALL];

    public LindwormModel(ModelPart root) {
        super(root);
        seg[0] = root.getChild("seg0");
        for (int i = 1; i < ALL; i++) seg[i] = seg[i - 1].getChild("seg" + i);
        head = seg[0].getChild("head");
        jaw = head.getChild("jaw");
        rightLeg = seg[1].getChild("right_leg");
        leftLeg = seg[1].getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parent = mesh.getRoot();
        PartDefinition first = null, second = null;
        for (int i = 0; i < ALL; i++) {
            CubeListBuilder box;
            PartPose pose;
            if (i < BODY) {
                box = CubeListBuilder.create().texOffs(0, 20).addBox(-3.0F, -2.5F, 0.0F, 6.0F, 5.0F, 6.0F)
                        .texOffs(28, 20).addBox(-0.5F, -4.5F, 1.0F, 1.0F, 2.0F, 4.0F);
                pose = i == 0 ? PartPose.offset(0.0F, 21.5F, -12.0F) : PartPose.offset(0.0F, 0.0F, 6.0F);
            } else if (i < BODY + TAIL) {
                box = CubeListBuilder.create().texOffs(0, 34).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 6.0F);
                pose = PartPose.offset(0.0F, i == BODY ? 0.5F : 0.0F, 6.0F);
            } else {
                box = CubeListBuilder.create().texOffs(0, 46).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F);
                pose = PartPose.offset(0.0F, 1.0F, 6.0F);
            }
            PartDefinition s = parent.addOrReplaceChild("seg" + i, box, pose);
            if (i == 0) first = s;
            if (i == 1) second = s;
            parent = s;
        }
        PartDefinition head = first.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -2.5F, -8.0F, 6.0F, 4.0F, 8.0F),
                PartPose.offset(0.0F, -0.5F, 0.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(30, 0).addBox(-2.5F, 0.0F, -7.0F, 5.0F, 2.0F, 7.0F),
                PartPose.offset(0.0F, 1.5F, -1.0F));
        head.addOrReplaceChild("right_horn", CubeListBuilder.create().texOffs(56, 0).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(-2.0F, -2.5F, -2.0F, 0.45F, -0.25F, 0.0F));
        head.addOrReplaceChild("left_horn", CubeListBuilder.create().texOffs(56, 0).mirror().addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(2.0F, -2.5F, -2.0F, 0.45F, 0.25F, 0.0F));
        second.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(72, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, -1.0F, 2.0F, 0.0F, 0.0F, 0.6F));
        second.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(72, 0).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, -1.0F, 2.0F, 0.0F, 0.0F, -0.6F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        if (CarcassRenderer.isHung(state)) return;
        if (CarcassRenderer.isCarcass(state)) {
            for (int i = 1; i < ALL; i++) seg[i].yRot = (i / 3 % 2 == 0 ? 0.3F : -0.3F);
            rightLeg.zRot = 1.2F;
            leftLeg.zRot = -1.2F;
            jaw.xRot = 0.3F;
            return;
        }
        Integer pose = state.getRenderData(UndergroundClient.LINDWORM_POSE);
        float t = state.ageInTicks;
        if (pose != null && pose == Lindworm.COILING) { // wound round its prey, squeezing
            for (int i = 1; i < ALL; i++) seg[i].yRot = 0.62F + Mth.sin(t * 0.3F + i) * 0.04F;
            seg[0].xRot = -0.3F;
            jaw.xRot = 0.5F;
            return;
        }
        if (pose != null && pose == Lindworm.LUNGING) { // straight as a spear, jaws wide, claws forward
            jaw.xRot = 0.8F;
            seg[0].xRot = -0.1F;
            rightLeg.xRot = leftLeg.xRot = -1.0F;
            return;
        }
        float speed = Math.min(state.walkAnimationSpeed * 1.5F, 1.0F);
        float phase = state.walkAnimationPos * 0.9F + t * 0.04F;
        float amp = 0.05F + 0.22F * speed, prev = 0.0F;
        for (int i = 0; i < ALL; i++) {
            float theta = amp * Mth.sin(phase - i * 0.7F);
            if (i > 0) seg[i].yRot = theta - prev;
            prev = theta;
        }
        float step = Mth.sin(state.walkAnimationPos * 1.6F) * 0.8F * speed;
        rightLeg.xRot = step;
        leftLeg.xRot = -step;
        head.yRot = Mth.clamp(state.yRot, -35.0F, 35.0F) * Mth.DEG_TO_RAD - seg[0].yRot;
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        jaw.xRot = 0.05F + Mth.abs(Mth.sin(t * 0.05F)) * 0.08F;
        if (state.charging) { // reared, gaping
            seg[0].xRot = -0.35F;
            seg[1].xRot = 0.15F;
            head.xRot += 0.3F;
            jaw.xRot = 0.3F + Mth.abs(Mth.sin(t * 0.25F)) * 0.4F;
        }
    }
}
