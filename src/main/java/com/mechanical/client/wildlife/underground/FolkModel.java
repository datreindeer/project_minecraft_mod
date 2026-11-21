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
 * The two-legged folk of the dark: the Vittra (slender, a big head with long pointed ears and huge eyes) and the Troll
 * (hunched and massive, long arms, a great nose, tusks and the cow's tail of the old tales). Same parts for both: the body
 * pivots at the hips and carries head and arms, so the troll's hunch brings them forward. Walking the legs and arms swing;
 * hunting, the arms reach forward and claw. Textures 64x64 (textures/entity/wildlife/vittra.png, troll.png), painted by
 * scripts/gen_fauna_underground_textures.py, whose UV tables mirror the texOffs here.
 */
public class FolkModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation VITTRA = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "vittra"), "main");
    public static final ModelLayerLocation TROLL = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "troll"), "main");

    private final ModelPart body, head, rightArm, leftArm, rightLeg, leftLeg;
    private final float hunch;
    /** Gaunt (the Vittra): a longer torso, a smaller head and arms to the knees, drawn by scaling the parts (UVs unchanged). */
    private final boolean gaunt;

    public FolkModel(ModelPart root, float hunch) {
        this(root, hunch, false);
    }

    public FolkModel(ModelPart root, float hunch, boolean gaunt) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        rightArm = body.getChild("right_arm");
        leftArm = body.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        this.hunch = hunch;
        this.gaunt = gaunt;
    }

    /** Vittra: 6x12x3 body, 8x8x8 head with 1x4x2 ears, 3x12x3 limbs. 2 blocks tall before the renderer's scale. */
    public static LayerDefinition createVittraLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-3.0F, -12.0F, -1.5F, 6.0F, 12.0F, 3.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -12.0F, 0.0F));
        head.addOrReplaceChild("right_ear", CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, -4.0F, -1.0F, 1.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(-4.0F, -4.0F, 0.0F, -0.3F, 0.0F, -0.7F));
        head.addOrReplaceChild("left_ear", CubeListBuilder.create().texOffs(32, 0).mirror().addBox(0.0F, -4.0F, -1.0F, 1.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(4.0F, -4.0F, 0.0F, -0.3F, 0.0F, 0.7F));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16).addBox(-2.0F, -1.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(-4.0F, -11.0F, 0.0F));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -1.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(4.0F, -11.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(-1.5F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F),
                PartPose.offset(1.5F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /**
     * Troll: 10x14x6 body, 8x8x8 head with a 2x4x3 nose and two 1x2x1 tusks, 4x16x4 arms (to the knees), 5x12x5 legs, a
     * 1x10x1 tail with a 2x3x2 tuft.
     */
    public static LayerDefinition createTrollLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-5.0F, -14.0F, -3.0F, 10.0F, 14.0F, 6.0F),
                PartPose.offset(0.0F, 12.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -7.0F, -6.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, -14.0F, -1.0F));
        head.addOrReplaceChild("nose", CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 4.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -4.0F, -6.0F, 0.25F, 0.0F, 0.0F));
        head.addOrReplaceChild("right_tusk", CubeListBuilder.create().texOffs(44, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(-2.5F, 0.0F, -5.5F));
        head.addOrReplaceChild("left_tusk", CubeListBuilder.create().texOffs(44, 0).mirror().addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.offset(2.5F, 0.0F, -5.5F));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-3.0F, -1.0F, -2.0F, 4.0F, 16.0F, 4.0F),
                PartPose.offset(-6.0F, -13.0F, 0.0F));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 16).mirror().addBox(-1.0F, -1.0F, -2.0F, 4.0F, 16.0F, 4.0F),
                PartPose.offset(6.0F, -13.0F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(48, 16).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 10.0F, 1.0F)
                        .texOffs(52, 16).addBox(-1.0F, 10.0F, -1.0F, 2.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, 3.0F, 0.5F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 36).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.offset(-2.6F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 36).mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.offset(2.6F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        if (gaunt) {
            // body x1.2 tall, head 0.7 and arms 1.4 of their size (children inherit the body's stretch); same 2 blocks overall
            body.yScale = 1.2F;
            body.xScale = 0.85F;
            head.xScale = head.zScale = 0.8F;
            head.yScale = 0.7F / 1.2F;
            rightArm.yScale = leftArm.yScale = 1.4F / 1.2F;
            rightArm.xScale = leftArm.xScale = rightArm.zScale = leftArm.zScale = 0.8F;
            rightLeg.xScale = leftLeg.xScale = rightLeg.zScale = leftLeg.zScale = 0.85F;
        }
        if (CarcassRenderer.isHung(state)) { // hung by the feet (upside down): the arms dangle past the head
            rightArm.xRot = leftArm.xRot = Mth.PI;
            rightArm.zRot = -0.15F;
            leftArm.zRot = 0.15F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) { // on its back (CarcassRenderer.ON_BACK): arms and legs fallen aside, head rolled over
            rightArm.zRot = 0.6F;
            leftArm.zRot = -0.45F;
            rightLeg.zRot = 0.25F;
            leftLeg.zRot = -0.15F;
            head.yRot = 0.6F;
            return;
        }
        float pos = state.walkAnimationPos, speed = Math.min(state.walkAnimationSpeed, 1.0F);
        // the gaunt stalk with short slow strides, the long arms hanging near still
        float swing = Mth.cos(pos * (gaunt ? 0.45F : 0.6662F)) * (gaunt ? 0.7F : 1.2F) * speed;
        float armSwing = gaunt ? 0.3F : 0.8F;
        rightLeg.xRot = swing;
        leftLeg.xRot = -swing;
        body.xRot = hunch;
        rightArm.xRot = -swing * armSwing - hunch;
        leftArm.xRot = swing * armSwing - hunch;
        float sway = Mth.sin(state.ageInTicks * 0.067F) * 0.05F;
        rightArm.zRot = 0.05F + sway;
        leftArm.zRot = -0.05F - sway;
        if (state.charging) { // reaching for its prey, the claws raking in turn
            float rake = Mth.sin(state.ageInTicks * 0.35F) * 0.4F;
            rightArm.xRot = -1.4F - hunch + rake;
            leftArm.xRot = -1.4F - hunch - rake;
        }
        head.xRot = state.xRot * Mth.DEG_TO_RAD - hunch;
        head.yRot = Mth.clamp(state.yRot, -45.0F, 45.0F) * Mth.DEG_TO_RAD;
    }
}
