package com.mechanical.client.wildlife.coldhunters;

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
 * Lemming: a round little ball of fur - stout body, blunt head sunk into it with the ears hidden in the fur, a stub of a
 * tail, tiny legs. Scurries with quick short steps and a bobbing head, sits up nibbling when it grazes, lies flat asleep.
 * Texture 64x32: textures/entity/wildlife/lemming.png, painted by scripts/gen_fauna_coldhunters_textures.py (LEMMING table).
 */
public class LemmingModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "lemming"), "main");

    private final ModelPart body, head, tail;
    private final ModelPart rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg;
    private final ModelPart[] legs;

    public LemmingModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.tail = body.getChild("tail");
        this.rightFrontLeg = root.getChild("right_front_leg");
        this.leftFrontLeg = root.getChild("left_front_leg");
        this.rightHindLeg = root.getChild("right_hind_leg");
        this.leftHindLeg = root.getChild("left_hind_leg");
        this.legs = new ModelPart[] {rightFrontLeg, leftFrontLeg, rightHindLeg, leftHindLeg};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -2.5F, 4.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, 21.0F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 6).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 0.5F, 3.5F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(20, 0).addBox(-1.5F, -1.5F, -2.5F, 3.0F, 3.0F, 3.0F)
                        .texOffs(32, 0).addBox(-0.5F, 0.0F, -3.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(0.0F, 20.5F, -2.5F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(36, 0).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(-1.2F, -1.2F, -0.5F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(36, 0).mirror().addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(1.2F, -1.2F, -0.5F));
        for (float x : new float[] {-1.2F, 1.2F}) {
            boolean left = x > 0;
            root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(0, 10).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(x, 23.0F, -1.5F));
            root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(4, 10).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(x, 23.0F, 2.0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            head.xRot = 0.3F; // tucked into the chest, as the head of a hung rodent flops
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            head.zRot = 0.2F;
            return;
        }
        if (state.dozing) {
            // flattened into a fur ball, legs hidden, a slow breath
            body.y += 1.0F + Mth.sin(t * 0.1F) * 0.1F;
            head.y += 1.2F;
            head.xRot = 0.2F;
            for (ModelPart leg : legs) leg.y -= 0.5F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.5F);
        float phase = state.walkAnimationPos * 2.2F; // quick, short scurrying steps
        float swing = Mth.cos(phase) * 1.0F * speed;
        rightFrontLeg.xRot = leftHindLeg.xRot = swing;
        leftFrontLeg.xRot = rightHindLeg.xRot = -swing;
        float bob = Math.abs(Mth.sin(phase)) * 0.5F * speed;
        body.y -= bob;
        head.y -= bob;
        head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(state.xRot, -20.0F, 20.0F) * Mth.DEG_TO_RAD;
        tail.yRot = Mth.sin(t * 0.3F) * 0.2F;

        // nibbling: sits back on its haunches, head and forepaws up, jaws working
        float graze = state.grazeAmount;
        if (graze > 0.0F) {
            body.xRot = -0.5F * graze;
            body.y += 0.5F * graze;
            head.y -= 1.2F * graze;
            head.z += 1.0F * graze;
            head.xRot += graze * (Mth.sin(t * 1.6F) * 0.12F);
            rightFrontLeg.y -= 1.5F * graze;
            leftFrontLeg.y -= 1.5F * graze;
            rightFrontLeg.z += graze;
            leftFrontLeg.z += graze;
            rightFrontLeg.xRot = leftFrontLeg.xRot = -0.8F * graze;
        }
    }
}
