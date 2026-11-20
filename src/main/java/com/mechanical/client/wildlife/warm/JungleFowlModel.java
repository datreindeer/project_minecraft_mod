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
 * Red jungle fowl, rooster and hen in one model: upright body, the hackle cape on the neck, small head with the bare face.
 * {@code showsHorns} (the rooster) shows the comb and wattles and the arched sickle feathers on top of the tail fan; the hen
 * has just the short tail. Walks with a head bob, pecks, roosts, bursts into the air like a pheasant. The chick is
 * PheasantChickModel with its own texture. Geometry and textures (wildlife/jungle_fowl_rooster.png, _hen.png) come from
 * scripts/gen_fauna_warm_tex_birds.py, which writes createBodyLayer() below.
 */
public class JungleFowlModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "jungle_fowl"), "main");

    private final ModelPart body, head, comb, leftWing, rightWing, tail, sickle, sickleTip, leftLeg, rightLeg;

    public JungleFowlModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.comb = head.getChild("comb");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
        this.tail = body.getChild("tail");
        this.sickle = tail.getChild("sickle");
        this.sickleTip = sickle.getChild("sickle_tip");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        comb.visible = sickle.visible = state.showsHorns;
        leftLeg.visible = rightLeg.visible = true;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.6F;
        float t = state.ageInTicks;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[0], new ModelPart[] {leftLeg, rightLeg});
            head.xRot = 1.45F; // the upright neck flops down
            leftWing.xRot = rightWing.xRot = 3.0F; // wings fall open, hanging from the shoulders towards the ground
            leftWing.zRot = 0.6F;
            rightWing.zRot = -0.6F;
            sickle.xRot = 3.9F; // the sickles fall down over the back
            sickleTip.xRot = 0.0F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart limb : new ModelPart[] {leftLeg, rightLeg, leftWing, rightWing}) limb.zRot = CarcassRenderer.LIMB_SAG;
            head.xRot += 0.6F; // neck limp
            tail.xRot -= 0.6F; // tail fan laid back
            sickleTip.xRot = -0.6F;
            return;
        }

        if (state.dozing) {
            // roosting on the ground: body levelled onto y = 24, legs tucked away, head sunk onto the shoulders, tail lowered
            body.xRot = 0.0F;
            body.y += 4.0F;
            leftLeg.visible = rightLeg.visible = false;
            head.xRot = 0.0F;
            head.yRot = 0.0F;
            head.y += 1.5F;
            head.z += 0.8F;
            tail.xRot -= 0.3F;
            return;
        }

        if (state.flying) {
            // wings spread and flapping, body level, neck stretched forward, the tail fanned out behind, sickles streaming
            float beat = Mth.sin(state.flap);
            float bob = 0.6F * Mth.cos(state.flap);
            leftWing.zRot = -1.35F - 0.9F * beat;
            rightWing.zRot = 1.35F + 0.9F * beat;
            leftWing.yRot = 0.15F;
            rightWing.yRot = -0.15F;
            body.xRot += 0.25F;
            body.y += bob;
            head.xRot += 0.5F;
            tail.xRot = -1.35F;
            tail.xScale = 1.8F;
            sickle.xRot = 1.35F;
            sickleTip.xRot = -0.25F + 0.1F * beat;
            leftLeg.xRot = rightLeg.xRot = 1.3F;
            leftLeg.y += bob - 0.5F;
            rightLeg.y += bob - 0.5F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * 0.6662F * 1.2F;
        float graze = state.grazeAmount;

        // walking: alternating legs, the head thrusts forward and back with each step, the sickles bounce
        leftLeg.xRot = Mth.cos(phase) * 1.2F * speed;
        rightLeg.xRot = Mth.cos(phase + Mth.PI) * 1.2F * speed;
        head.z += Mth.sin(phase * 2.0F) * 0.9F * speed;
        head.xRot += Mth.sin(phase * 2.0F) * 0.06F * speed;
        body.zRot = Mth.sin(phase) * 0.06F * speed;
        sickleTip.xRot += Mth.cos(phase * 2.0F) * 0.12F * speed;

        // scratching and pecking: body tips forward, quick jabs at the ground
        float peck = Math.max(0.0F, Mth.sin(t * 0.55F)) * 0.35F;
        body.xRot += 0.4F * graze;
        head.y += 1.0F * graze;
        head.xRot += (1.25F + peck) * graze;

        // idle: head cocks and jerks, the tail sways, the odd wing shuffle
        head.yRot += 0.25F * Math.max(0.0F, Mth.sin(t * 0.07F + state.entityId) * 4.0F - 3.0F) * (1.0F - graze);
        tail.zRot += 0.05F * Mth.sin(t * 0.06F);
        sickleTip.xRot += 0.04F * Mth.sin(t * 0.09F);
        float shuffle = Math.max(0.0F, Mth.sin(t * 0.07F) * 8.0F - 7.0F);
        leftWing.zRot = -0.12F * shuffle;
        rightWing.zRot = 0.12F * shuffle;
    }

    // BEGIN GENERATED by scripts/gen_fauna_warm_tex_birds.py
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -3.5F, 4.0F, 4.0F, 7.0F),
            PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F, -0.25F, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 11).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 4.0F, 3.0F).texOffs(12, 11).addBox(-1.0F, -4.5F, -2.5F, 2.0F, 2.0F, 3.0F).texOffs(56, 11).addBox(-0.5F, -4.0F, -3.5F, 1.0F, 1.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, -1.5F, -3.0F, 0.25F, 0.0F, 0.0F));
        PartDefinition comb = head.addOrReplaceChild("comb",
            CubeListBuilder.create().texOffs(22, 11).addBox(-0.5F, -6.5F, -2.5F, 1.0F, 2.0F, 3.0F).texOffs(52, 11).addBox(-0.5F, -3.0F, -2.5F, 1.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing",
            CubeListBuilder.create().texOffs(22, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 6.0F),
            PartPose.offsetAndRotation(2.0F, -1.5F, -2.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightWing = body.addOrReplaceChild("right_wing",
            CubeListBuilder.create().texOffs(22, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 6.0F),
            PartPose.offsetAndRotation(-2.0F, -1.5F, -2.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail",
            CubeListBuilder.create().texOffs(30, 11).addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, -1.5F, 3.0F, -0.6F, 0.0F, 0.0F));
        PartDefinition sickle = tail.addOrReplaceChild("sickle",
            CubeListBuilder.create().texOffs(36, 0).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 2.0F, 6.0F),
            PartPose.offsetAndRotation(0.0F, -3.0F, 1.0F, 1.25F, 0.0F, 0.0F));
        PartDefinition sickleTip = sickle.addOrReplaceChild("sickle_tip",
            CubeListBuilder.create().texOffs(50, 0).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 2.0F, 6.0F),
            PartPose.offsetAndRotation(0.0F, 0.0F, 5.5F, -1.35F, 0.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(48, 11).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(38, 11).mirror().addBox(-1.0F, 3.0F, -2.5F, 2.0F, 1.0F, 3.0F),
            PartPose.offsetAndRotation(1.0F, 20.0F, 0.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(48, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(38, 11).addBox(-1.0F, 3.0F, -2.5F, 2.0F, 1.0F, 3.0F),
            PartPose.offsetAndRotation(-1.0F, 20.0F, 0.5F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
    // END GENERATED
}
