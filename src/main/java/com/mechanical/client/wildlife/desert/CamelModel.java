package com.mechanical.client.wildlife.desert;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.LyingPose;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.warm.WarmRenderState;

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
 * Camel (dromedary; the wild one and the tamed one): a deep body high on long two-jointed legs with broad pads, one big
 * hump, a long neck that swings forward and low from the chest (neck) and then up (neck2) to a long-muzzled head with
 * small ears, and a thin tufted tail. Paces (both legs of a side together), the neck nodding with the stride; grazing it
 * lowers the whole neck; lying it folds its legs under it (LyingPose) with the neck curved and the head up. The tamed one
 * can carry Pack Baskets on its sides, the strap over the hump ({@link WarmRenderState#packed}). Calves: the same model
 * drawn smaller. Texture 128x64: textures/entity/wildlife/camel.png, painted by scripts/gen_fauna_desert_textures.py
 * whose camel() part tree mirrors the cubes below (keep them in sync).
 */
public class CamelModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "camel"), "main");
    private static final float DEG = Mth.DEG_TO_RAD;
    private static final float NECK_X = 1.9F, NECK2_X = -1.75F, HEAD_X = 0.15F, TAIL_X = 0.3F;
    private static final float GRAZE_NECK_X = 2.6F, GRAZE_NECK2_X = -1.1F, GRAZE_HEAD_X = 0.5F;
    private static final float LIE_DROP = 11.0F, LIE_LEG_Y = 22.5F;

    private final ModelPart body, packs, neck, neck2, head, leftEar, rightEar, tail;
    private final ModelPart leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg;
    private final ModelPart[] legs;

    public CamelModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        packs = body.getChild("packs");
        neck = body.getChild("neck");
        neck2 = neck.getChild("neck2");
        head = neck2.getChild("head");
        leftEar = head.getChild("left_ear");
        rightEar = head.getChild("right_ear");
        tail = body.getChild("tail");
        leftFrontLeg = root.getChild("left_front_leg");
        rightFrontLeg = root.getChild("right_front_leg");
        leftHindLeg = root.getChild("left_hind_leg");
        rightHindLeg = root.getChild("right_hind_leg");
        legs = new ModelPart[] {leftFrontLeg, rightFrontLeg, leftHindLeg, rightHindLeg};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -9.0F, 8.0F, 8.0F, 18.0F)
                .texOffs(66, 0).addBox(-3.0F, -9.0F, -5.0F, 6.0F, 5.0F, 8.0F), PartPose.offset(0.0F, 4.0F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(66, 26).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F)
                .texOffs(106, 26).addBox(-1.0F, 7.0F, -1.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -3.0F, 9.0F, TAIL_X, 0.0F, 0.0F));
        PartDefinition packs = body.addOrReplaceChild("packs", CubeListBuilder.create().texOffs(86, 26).addBox(-3.5F, -9.5F, -2.0F, 7.0F, 1.0F, 3.0F), PartPose.ZERO);
        packs.addOrReplaceChild("left_basket", CubeListBuilder.create().texOffs(94, 0).mirror().addBox(0.0F, 0.0F, -3.5F, 3.0F, 6.0F, 7.0F).mirror(false),
                PartPose.offsetAndRotation(4.0F, -3.0F, -0.5F, 0.0F, 0.0F, -0.08F));
        packs.addOrReplaceChild("right_basket", CubeListBuilder.create().texOffs(94, 0).addBox(-3.0F, 0.0F, -3.5F, 3.0F, 6.0F, 7.0F),
                PartPose.offsetAndRotation(-4.0F, -3.0F, -0.5F, 0.0F, 0.0F, 0.08F));
        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 26).addBox(-2.0F, -9.0F, -2.0F, 4.0F, 9.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, -8.0F, NECK_X, 0.0F, 0.0F));
        PartDefinition neck2 = neck.addOrReplaceChild("neck2", CubeListBuilder.create().texOffs(28, 26).addBox(-1.5F, -8.0F, -1.5F, 3.0F, 8.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -8.5F, 0.0F, NECK2_X, 0.0F, 0.0F));
        PartDefinition head = neck2.addOrReplaceChild("head", CubeListBuilder.create().texOffs(48, 26).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 5.0F)
                .texOffs(70, 26).addBox(-1.5F, -1.0F, -9.0F, 3.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, -7.5F, 0.0F, HEAD_X, 0.0F, 0.0F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(114, 26).mirror().addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F).mirror(false),
                PartPose.offsetAndRotation(1.5F, -2.0F, -0.5F, 0.0F, 0.0F, 0.4F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(114, 26).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(-1.5F, -2.0F, -0.5F, 0.0F, 0.0F, -0.4F));
        for (float x : new float[] {2.5F, -2.5F}) {
            boolean left = x > 0;
            PartDefinition front = root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(16, 26).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 10.0F, 3.0F), PartPose.offset(x, 7.0F, -6.5F));
            front.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(40, 26).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offset(0.0F, 9.0F, 0.0F));
            PartDefinition hind = root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(52, 0).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 10.0F, 4.0F), PartPose.offset(x, 7.0F, 6.5F));
            hind.addOrReplaceChild("lower", CubeListBuilder.create().mirror(left).texOffs(40, 26).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F), PartPose.offset(0.0F, 9.0F, 0.5F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        packs.visible = state instanceof WarmRenderState w && w.packed && !state.isBaby;
        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 3.0F; // straight down below the chest
            neck2.xRot = 0.0F;
            head.xRot = 0.0F;
            tail.xRot = -1.9F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart leg : legs) leg.zRot = CarcassRenderer.LIMB_SAG;
            neck.xRot = 1.6F;
            neck2.xRot = -0.3F;
            neck.zRot = -CarcassRenderer.LIMB_SAG * 0.6F;
            head.xRot = 0.0F;
            tail.xRot = 0.5F;
            return;
        }
        float age = state.ageInTicks;
        float seed = (float) (state.x * 3.7 + state.z * 5.3);
        if (state.dozing) {
            LyingPose.apply(body, LIE_DROP, LIE_LEG_Y, age, new ModelPart[] {leftFrontLeg, rightFrontLeg}, new ModelPart[] {leftHindLeg, rightHindLeg});
            neck.xRot = 1.5F;
            neck2.xRot = -1.6F;
            head.xRot = 0.2F;
            head.yRot = 0.15F;
            tail.xRot = 0.2F;
            tail.zRot = 0.3F;
            return;
        }
        // the camel paces: both legs of a side swing together; the neck nods with each stride
        float speed = Math.min(state.walkAnimationSpeed, 1.1F);
        float phase = state.walkAnimationPos * 0.55F;
        float amp = Math.min(0.9F, speed * 1.0F);
        float a = Mth.cos(phase) * amp;
        leftFrontLeg.xRot = leftHindLeg.xRot = a;
        rightFrontLeg.xRot = rightHindLeg.xRot = -a;
        for (ModelPart leg : new ModelPart[] {leftFrontLeg, rightFrontLeg}) leg.getChild("lower").xRot = Math.max(0.0F, -leg.xRot) * 1.2F;
        for (ModelPart leg : new ModelPart[] {leftHindLeg, rightHindLeg}) leg.getChild("lower").xRot = Math.max(0.0F, leg.xRot) * 0.7F;
        body.zRot = Mth.sin(phase) * 0.04F * speed; // the rolling gait
        body.y -= Mth.abs(Mth.sin(phase)) * 0.4F * speed;

        float yaw = Mth.clamp(state.yRot, -50.0F, 50.0F) * DEG;
        float pitch = Mth.clamp(state.xRot, -30.0F, 30.0F) * DEG;
        float nod = Mth.cos(phase * 2.0F) * 0.06F * speed;
        float nx = NECK_X + nod, n2x = NECK2_X - nod * 0.5F, hx = HEAD_X + pitch * 0.6F - state.stand * 0.3F;
        float graze = state.grazeAmount;
        if (graze > 0.0F) {
            nx = Mth.lerp(graze, nx, GRAZE_NECK_X);
            n2x = Mth.lerp(graze, n2x, GRAZE_NECK2_X);
            hx = Mth.lerp(graze, hx, GRAZE_HEAD_X + Mth.sin(age * 0.6F) * 0.05F);
            yaw *= 1.0F - graze;
        }
        neck.xRot = nx;
        neck.yRot = yaw * 0.3F;
        neck2.xRot = n2x;
        head.xRot = hx;
        head.yRot = yaw * 0.7F;
        float flick = Mth.sin(age * 0.05F + seed) > 0.95F ? Mth.sin(age * 0.9F) * 0.4F : 0.0F;
        leftEar.zRot = 0.4F + flick;
        rightEar.zRot = -0.4F - flick;
        tail.zRot = Mth.sin(age * 0.08F + seed) * 0.25F;
        tail.xRot = TAIL_X + speed * 0.2F;
    }
}
