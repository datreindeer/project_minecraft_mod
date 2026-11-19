package com.mechanical.client.wildlife;

import com.mechanical.client.CarcassRenderer;

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
 * Common pheasant (cock): copper body, green head with red wattle and white collar, long barred tail. Walks with a
 * head bob, pecks when grazing, and in the air spreads and flaps its wings with the tail fanned and legs tucked.
 * Texture: textures/entity/wildlife/pheasant.png (64x32), painted by scripts/gen_small_wildlife_textures.py, which
 * mirrors the cubes below.
 */
public class PheasantModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "pheasant"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public PheasantModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.tail = this.body.getChild("tail");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // breast slightly raised
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 8.0F),
            PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F, -0.15F, 0.0F, 0.0F));
        // neck with the white collar, head, beak; the pitch cancels the body's so the neck stands upright
        body.addOrReplaceChild("head",
            CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F)
                .texOffs(32, 0).addBox(-1.5F, -6.5F, -2.5F, 3.0F, 3.0F, 3.0F)
                .texOffs(44, 0).addBox(-0.5F, -5.5F, -3.5F, 1.0F, 1.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, -1.5F, -3.5F, 0.15F, 0.0F, 0.0F));
        body.addOrReplaceChild("left_wing",
            CubeListBuilder.create().texOffs(0, 12).addBox(0.0F, 0.0F, 0.0F, 1.0F, 4.0F, 7.0F),
            PartPose.offset(2.0F, -1.5F, -3.0F));
        body.addOrReplaceChild("right_wing",
            CubeListBuilder.create().texOffs(16, 12).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 4.0F, 7.0F),
            PartPose.offset(-2.0F, -1.5F, -3.0F));
        // long tail in two tapering pieces, held a little raised
        body.addOrReplaceChild("tail",
            CubeListBuilder.create()
                .texOffs(32, 6).addBox(-1.5F, -0.5F, 0.0F, 3.0F, 1.0F, 6.0F)
                .texOffs(32, 13).addBox(-1.0F, -0.5F, 6.0F, 2.0F, 1.0F, 7.0F),
            PartPose.offsetAndRotation(0.0F, -1.0F, 4.0F, 0.45F, 0.0F, 0.0F));
        // legs on the root so pecking (body pitch) leaves them standing
        root.addOrReplaceChild("left_leg",
            CubeListBuilder.create()
                .texOffs(50, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F)
                .texOffs(50, 4).addBox(-1.0F, 3.0F, -2.5F, 2.0F, 1.0F, 3.0F),
            PartPose.offset(1.0F, 20.0F, 0.5F));
        root.addOrReplaceChild("right_leg",
            CubeListBuilder.create()
                .texOffs(54, 0).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F)
                .texOffs(50, 8).addBox(-1.0F, 3.0F, -2.5F, 2.0F, 1.0F, 3.0F),
            PartPose.offset(-1.0F, 20.0F, 0.5F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
        this.head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.6F;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[0], new ModelPart[] {leftLeg, rightLeg});
            head.xRot = 1.45F; // the upright neck flops down
            leftWing.xRot = rightWing.xRot = 3.0F; // wings fall open, hanging from the shoulders towards the ground
            leftWing.zRot = 0.6F;
            rightWing.zRot = -0.6F;
            return;
        }

        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart limb : new ModelPart[] {this.rightLeg, this.leftLeg, this.rightWing, this.leftWing}) {
                limb.zRot = CarcassRenderer.LIMB_SAG;
            }
            this.head.xRot += 0.6F; // neck limp
            return;
        }

        if (state.dozing) {
            // roosting on the ground: body levelled and dropped so the wing bottoms (lowest point) rest on y = 24;
            // legs folded forward and pivots lowered so they sit hidden inside the body; head sunk onto the shoulders
            this.body.xRot = 0.0F;
            this.body.y += 3.5F;
            this.leftLeg.xRot = -1.5F;
            this.rightLeg.xRot = -1.5F;
            this.leftLeg.y += 2.7F;
            this.rightLeg.y += 2.7F;
            this.head.xRot = 0.0F;
            this.head.yRot = 0.0F;
            this.head.y += 2.5F;
            this.head.z += 1.0F;
            return;
        }

        float t = state.ageInTicks;
        if (state.flying) {
            // wings spread out level and flap through the phase; body level, neck stretched forward, tail fanned
            float beat = Mth.sin(state.flap);
            float bob = 0.6F * Mth.cos(state.flap);
            this.leftWing.zRot = -1.35F - 0.9F * beat;
            this.rightWing.zRot = 1.35F + 0.9F * beat;
            this.leftWing.yRot = 0.15F;
            this.rightWing.yRot = -0.15F;
            this.body.xRot += 0.15F;
            this.body.y += bob;
            this.head.xRot += 0.45F;
            this.tail.xRot -= 0.4F;
            this.tail.xScale = 1.9F;
            this.leftLeg.xRot = 1.3F;
            this.rightLeg.xRot = 1.3F;
            this.leftLeg.y += bob - 0.5F;
            this.rightLeg.y += bob - 0.5F;
            return;
        }

        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * 0.6662F;
        float graze = state.grazeAmount;

        // walking: alternating legs, the head thrusts forward and back once per step, slight waddle
        this.leftLeg.xRot = Mth.cos(phase) * 1.2F * speed;
        this.rightLeg.xRot = Mth.cos(phase + Mth.PI) * 1.2F * speed;
        this.head.z += Mth.sin(phase * 2.0F) * 0.8F * speed;
        this.head.xRot += Mth.sin(phase * 2.0F) * 0.06F * speed;
        this.body.zRot = Mth.sin(phase) * 0.06F * speed;
        this.tail.xRot += Mth.cos(phase * 2.0F) * 0.05F * speed;

        // pecking: body tips forward, neck dips to the ground in quick jabs
        float peck = Math.max(0.0F, Mth.sin(t * 0.5F)) * 0.35F;
        this.body.xRot += 0.35F * graze;
        this.head.y += 1.0F * graze;
        this.head.xRot += (1.2F + peck) * graze;

        // idle: small head bob and tail sway, the odd wing shuffle
        this.head.xRot += 0.04F * Mth.sin(t * 0.09F) * (1.0F - graze);
        this.tail.yRot += 0.06F * Mth.sin(t * 0.05F);
        float shuffle = Math.max(0.0F, Mth.sin(t * 0.07F) * 8.0F - 7.0F);
        this.leftWing.zRot = -0.12F * shuffle;
        this.rightWing.zRot = 0.12F * shuffle;
    }
}
