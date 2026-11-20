package com.mechanical.wildlife.mountain.client;

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
 * Alpine marmot: a plump, low ground squirrel (9 px long) on short legs, a broad blunt head with tiny round ears and a
 * pale muzzle, and a medium bushy tail. The body pivots at its rear underside, so the sentinel pose (state.charging, the
 * synced "alert") sits it up on its haunches with the forepaws held to the chest; the forelegs are children of the body
 * for that. Gallops in short bounds; curls up asleep. Babies: the same model scaled.
 * Texture 64x32: textures/entity/wildlife/marmot{,_pup}.png (scripts/gen_fauna_mountain_textures.py, MARMOT table).
 */
public class MarmotModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "marmot"), "main");
    private static final float TAIL_X = 0.5F;

    private final ModelPart body, head, tail, leftEar, rightEar;
    private final ModelPart leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg;

    public MarmotModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        tail = body.getChild("tail");
        leftEar = head.getChild("left_ear");
        rightEar = head.getChild("right_ear");
        leftFrontLeg = body.getChild("left_front_leg");
        rightFrontLeg = body.getChild("right_front_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightHindLeg = root.getChild("right_hind_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // pivot at the rear underside (y 22, z 3): body spans y 16..22, z -4.5..4.5
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -6.0F, -7.5F, 6.0F, 6.0F, 9.0F),
                PartPose.offset(0.0F, 22.0F, 3.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(30, 0).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 3.0F)
                        .texOffs(44, 0).addBox(-1.5F, -0.5F, -4.0F, 3.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, -4.0F, -7.5F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(52, 0).mirror().addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(1.6F, -2.0F, -1.2F, 0.0F, 0.0F, 0.3F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(52, 0).addBox(-0.5F, -1.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-1.6F, -2.0F, -1.2F, 0.0F, 0.0F, -0.3F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(30, 7).addBox(-1.0F, -0.5F, 0.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -4.5F, 1.5F, TAIL_X, 0.0F, 0.0F));
        for (float x : new float[] {-2.0F, 2.0F}) {
            boolean left = x > 0;
            body.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(0, 15).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(x, -1.0F, -6.0F));
            root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(0, 15).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F), PartPose.offset(x, 21.0F, 2.0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            head.xRot = 0.2F;
            tail.xRot = -2.0F; // flops down over the back
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : new ModelPart[] {leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg}) leg.zRot = CarcassRenderer.LIMB_SAG;
            head.yRot = state.yRot * Mth.DEG_TO_RAD;
            head.xRot = 0.3F;
            tail.xRot = 1.1F;
            return;
        }
        if (state.dozing) {
            // curled up flat: belly on the ground, legs tucked, nose down, the tail wrapped round
            body.y += 2.0F + Mth.sin(age * 0.08F) * 0.1F;
            leftFrontLeg.xRot = rightFrontLeg.xRot = -1.4F;
            leftHindLeg.y = rightHindLeg.y = 22.5F;
            leftHindLeg.xRot = rightHindLeg.xRot = 1.4F;
            head.xRot = 0.45F;
            head.yRot = 0.3F;
            tail.xRot = 1.2F;
            tail.yRot = 0.9F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.4F);
        float pos = state.walkAnimationPos;
        // bounding gallop: forelegs together, hind legs together, the body rocking
        float f = Mth.cos(pos * 0.9F) * speed * 1.1F;
        leftFrontLeg.xRot = f;
        rightFrontLeg.xRot = f * 0.85F;
        leftHindLeg.xRot = -f;
        rightHindLeg.xRot = -f * 0.85F;
        body.xRot = Mth.sin(pos * 0.9F) * 0.08F * speed;
        head.yRot = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(state.xRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD * 0.7F;
        tail.xRot = TAIL_X + Mth.sin(pos * 0.9F) * 0.2F * speed;
        // grazing: nose down nibbling
        if (state.grazeAmount > 0) {
            head.xRot = Mth.lerp(state.grazeAmount, head.xRot, 0.7F + Mth.sin(age * 1.1F) * 0.08F);
            head.yRot *= 1.0F - state.grazeAmount;
        }
        // sentinel: up on the haunches, forepaws at the chest, head level and watching, the tail flat behind
        if (state.charging) {
            body.xRot = -1.25F;
            head.xRot = 1.15F + Mth.sin(age * 0.2F) * 0.05F;
            leftFrontLeg.xRot = rightFrontLeg.xRot = 0.9F;
            tail.xRot = 1.6F;
            leftEar.zRot += 0.15F;
            rightEar.zRot -= 0.15F;
        }
        // whiskery nose twitch
        head.zRot = Mth.sin(age * 0.6F) > 0.85F ? Mth.sin(age * 3.0F) * 0.03F : 0.0F;
    }
}
