package com.mechanical.client.wildlife;

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
 * The flocking birds (crow, goose, rock dove, ptarmigan, seagull): one part tree, different proportions per species.
 * On the ground: walks with a head bob, pecks, roosts with the head sunk in. In the air: the inner wings swing out
 * level and the outer wings (folded back along the body at rest) swing out in line, beating through the flap phase or
 * held still for gliding; legs tucked, neck stretched (geese all the way), body pitched with the climb or dive.
 * The geometry and the textures come from scripts/gen_bird_textures.py, which writes the createXxxLayer methods below.
 */
public class BirdModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation CROW = layer("crow");
    public static final ModelLayerLocation GOOSE = layer("goose");
    public static final ModelLayerLocation ROCK_DOVE = layer("rock_dove");
    public static final ModelLayerLocation PTARMIGAN = layer("ptarmigan");
    public static final ModelLayerLocation SEAGULL = layer("seagull");

    private final ModelPart body, neck, head, leftWing, rightWing, leftTip, rightTip, tail, leftLeg, rightLeg;
    /** How far the neck leans forward in flight (geese: stretched straight out). */
    private final float flightNeck;
    /** Wing-beat amplitude (radians). */
    private final float beat;

    public BirdModel(ModelPart root, float flightNeck, float beat) {
        super(root);
        this.body = root.getChild("body");
        this.neck = body.getChild("neck");
        this.head = neck.getChild("head");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
        this.leftTip = leftWing.getChild("left_wing_tip");
        this.rightTip = rightWing.getChild("right_wing_tip");
        this.tail = body.getChild("tail");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.flightNeck = flightNeck;
        this.beat = beat;
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", name), "main");
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        BirdRenderState bird = (BirdRenderState) state;
        leftLeg.visible = rightLeg.visible = true;
        float t = state.ageInTicks;

        if (state.flying) {
            flying(bird);
            return;
        }
        head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.8F;
        head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;

        if (state.dozing) {
            // roosting: body down on the ground (or water), legs folded away, head sunk onto the shoulders
            body.y += 23.0F - leftLeg.y; // the body's underside (one pixel below the leg pivot) on the ground
            leftLeg.visible = rightLeg.visible = false;
            neck.y += 1.5F;
            boolean longNeck = flightNeck > 1.0F; // geese lay the neck back along the body
            neck.xRot = longNeck ? -1.1F : -0.25F;
            head.xRot = longNeck ? 1.3F : 0.3F;
            head.yRot = 0.0F;
            return;
        }

        if (bird.swimming) {
            // paddling: legs stroke slowly under the water, body rocks gently
            leftLeg.xRot = Mth.cos(t * 0.25F) * 0.6F;
            rightLeg.xRot = -leftLeg.xRot;
            body.zRot = Mth.sin(t * 0.1F) * 0.04F;
        } else {
            // walking: alternating legs, the head thrusts forward and back with each step, slight waddle
            float speed = Math.min(state.walkAnimationSpeed, 1.0F);
            float phase = state.walkAnimationPos * 0.6662F * 1.4F;
            leftLeg.xRot = Mth.cos(phase) * 1.1F * speed;
            rightLeg.xRot = Mth.cos(phase + Mth.PI) * 1.1F * speed;
            neck.z += Mth.sin(phase * 2.0F) * 0.7F * speed;
            body.zRot = Mth.sin(phase) * 0.07F * speed;
        }

        // pecking / grazing: body tips forward, the neck dips to the ground in quick jabs
        float graze = state.grazeAmount;
        float peck = Math.max(0.0F, Mth.sin(t * 0.6F)) * 0.4F;
        body.xRot += 0.3F * graze;
        neck.xRot += (1.1F + peck) * graze;
        head.xRot += 0.3F * graze;

        // idle: tail flicks, the odd wing shuffle
        tail.xRot += 0.08F * Math.max(0.0F, Mth.sin(t * 0.13F) * 4.0F - 3.0F);
        float shuffle = Math.max(0.0F, Mth.sin(t * 0.05F + state.entityId) * 8.0F - 7.0F);
        leftWing.zRot = -0.15F * shuffle;
        rightWing.zRot = 0.15F * shuffle;
    }

    private void flying(BirdRenderState state) {
        float glide = state.glide;
        float wave = Mth.sin(state.flap) * (1.0F - glide);
        // wings level out to the sides (left wing: negative zRot lifts it), beating around just above level
        float lift = Mth.HALF_PI - 0.05F + beat * wave;
        leftWing.zRot = -lift;
        rightWing.zRot = lift;
        leftWing.yRot = 0.08F;
        rightWing.yRot = -0.08F;
        // outer wings: swung out in line with the inner wings, trailing the beat a little, swept back when gliding
        for (ModelPart tip : new ModelPart[] {leftTip, rightTip}) {
            tip.x = 0.0F;
            tip.xRot = 0.0F;
        }
        float lag = 0.45F * Mth.sin(state.flap - 0.9F) * (1.0F - glide);
        leftTip.zRot = -lag + glide * 0.06F;
        rightTip.zRot = lag - glide * 0.06F;
        leftTip.yRot = 0.12F * glide;
        rightTip.yRot = -0.12F * glide;

        float bob = 0.5F * Mth.cos(state.flap) * (1.0F - glide);
        body.xRot = -Mth.clamp(state.pitch, -0.9F, 0.6F);
        body.y += bob;
        neck.xRot = flightNeck;
        head.xRot = -flightNeck; // head level with the body
        tail.xRot = 0.05F;
        tail.xScale = 1.5F; // fanned
        leftLeg.xRot = rightLeg.xRot = 1.4F; // tucked back
        leftLeg.y += bob - 0.5F;
        rightLeg.y += bob - 0.5F;
    }

    // BEGIN GENERATED by scripts/gen_bird_textures.py
    public static LayerDefinition createCrowLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -3.5F, 4.0F, 4.0F, 7.0F),
            PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
            CubeListBuilder.create().texOffs(24, 11).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, -1.0F, -2.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(36, 0).addBox(-1.5F, -2.5F, -1.5F, 3.0F, 3.0F, 3.0F).texOffs(36, 11).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 1.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightWing = body.addOrReplaceChild("right_wing",
            CubeListBuilder.create().texOffs(22, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 6.0F),
            PartPose.offsetAndRotation(-2.0F, -1.5F, -2.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing",
            CubeListBuilder.create().texOffs(22, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 6.0F),
            PartPose.offsetAndRotation(2.0F, -1.5F, -2.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightWingTip = rightWing.addOrReplaceChild("right_wing_tip",
            CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 4.0F, 2.0F),
            PartPose.offsetAndRotation(-0.5F, 3.0F, 0.0F, 1.5708F, 0.0F, 0.0F));
        PartDefinition leftWingTip = leftWing.addOrReplaceChild("left_wing_tip",
            CubeListBuilder.create().texOffs(48, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 4.0F, 2.0F),
            PartPose.offsetAndRotation(0.5F, 3.0F, 0.0F, 1.5708F, 0.0F, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail",
            CubeListBuilder.create().texOffs(0, 11).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 1.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, -1.0F, 3.0F, -0.15F, 0.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(32, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(14, 11).addBox(-1.0F, 3.0F, -2.5F, 2.0F, 1.0F, 3.0F),
            PartPose.offsetAndRotation(-1.0F, 20.0F, 0.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(32, 11).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(14, 11).mirror().addBox(-1.0F, 3.0F, -2.5F, 2.0F, 1.0F, 3.0F),
            PartPose.offsetAndRotation(1.0F, 20.0F, 0.5F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createGooseLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.5F, -4.5F, 5.0F, 5.0F, 9.0F),
            PartPose.offsetAndRotation(0.0F, 18.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
            CubeListBuilder.create().texOffs(46, 0).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 7.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, -1.5F, -3.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(0, 14).addBox(-1.5F, -2.5F, -2.5F, 3.0F, 3.0F, 4.0F).texOffs(42, 14).addBox(-1.0F, -1.5F, -4.5F, 2.0F, 1.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightWing = body.addOrReplaceChild("right_wing",
            CubeListBuilder.create().texOffs(28, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 4.0F, 8.0F),
            PartPose.offsetAndRotation(-2.5F, -2.0F, -3.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing",
            CubeListBuilder.create().texOffs(28, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 4.0F, 8.0F),
            PartPose.offsetAndRotation(2.5F, -2.0F, -3.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightWingTip = rightWing.addOrReplaceChild("right_wing_tip",
            CubeListBuilder.create().texOffs(54, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 5.0F, 3.0F),
            PartPose.offsetAndRotation(-0.5F, 4.0F, 0.0F, 1.5708F, 0.0F, 0.0F));
        PartDefinition leftWingTip = leftWing.addOrReplaceChild("left_wing_tip",
            CubeListBuilder.create().texOffs(54, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 5.0F, 3.0F),
            PartPose.offsetAndRotation(0.5F, 4.0F, 0.0F, 1.5708F, 0.0F, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail",
            CubeListBuilder.create().texOffs(26, 14).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 1.0F, 3.0F),
            PartPose.offsetAndRotation(0.0F, -1.5F, 4.0F, -0.1F, 0.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(38, 14).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(14, 14).addBox(-1.5F, 3.0F, -2.5F, 3.0F, 1.0F, 3.0F),
            PartPose.offsetAndRotation(-1.5F, 20.0F, 0.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(38, 14).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(14, 14).mirror().addBox(-1.5F, 3.0F, -2.5F, 3.0F, 1.0F, 3.0F),
            PartPose.offsetAndRotation(1.5F, 20.0F, 0.5F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createRockDoveLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.5F, -3.0F, 3.0F, 3.0F, 6.0F),
            PartPose.offsetAndRotation(0.0F, 20.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
            CubeListBuilder.create().texOffs(0, 9).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 3.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, -0.5F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, -1.5F, -1.5F, 2.0F, 2.0F, 3.0F).texOffs(18, 9).addBox(-0.5F, -0.5F, -2.5F, 1.0F, 1.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightWing = body.addOrReplaceChild("right_wing",
            CubeListBuilder.create().texOffs(18, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 2.0F, 5.0F),
            PartPose.offsetAndRotation(-1.5F, -1.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing",
            CubeListBuilder.create().texOffs(18, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 5.0F),
            PartPose.offsetAndRotation(1.5F, -1.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightWingTip = rightWing.addOrReplaceChild("right_wing_tip",
            CubeListBuilder.create().texOffs(30, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 4.0F, 2.0F),
            PartPose.offsetAndRotation(-0.5F, 2.0F, 0.0F, 1.5708F, 0.0F, 0.0F));
        PartDefinition leftWingTip = leftWing.addOrReplaceChild("left_wing_tip",
            CubeListBuilder.create().texOffs(30, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 4.0F, 2.0F),
            PartPose.offsetAndRotation(0.5F, 2.0F, 0.0F, 1.5708F, 0.0F, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail",
            CubeListBuilder.create().texOffs(36, 0).addBox(-1.0F, 0.0F, 0.0F, 2.0F, 1.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, -0.5F, 2.5F, -0.1F, 0.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(14, 9).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F).texOffs(8, 9).addBox(-0.5F, 2.0F, -1.5F, 1.0F, 1.0F, 2.0F),
            PartPose.offsetAndRotation(-0.5F, 21.0F, 0.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(14, 9).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 2.0F, 1.0F).texOffs(8, 9).mirror().addBox(-0.5F, 2.0F, -1.5F, 1.0F, 1.0F, 2.0F),
            PartPose.offsetAndRotation(0.5F, 21.0F, 0.5F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createPtarmiganLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 6.0F),
            PartPose.offsetAndRotation(0.0F, 20.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
            CubeListBuilder.create().texOffs(8, 10).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, -1.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(32, 0).addBox(-1.5F, -2.5F, -1.5F, 3.0F, 3.0F, 3.0F).texOffs(24, 10).addBox(-0.5F, -1.5F, -2.5F, 1.0F, 1.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightWing = body.addOrReplaceChild("right_wing",
            CubeListBuilder.create().texOffs(20, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 5.0F),
            PartPose.offsetAndRotation(-2.0F, -1.5F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing",
            CubeListBuilder.create().texOffs(20, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 5.0F),
            PartPose.offsetAndRotation(2.0F, -1.5F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightWingTip = rightWing.addOrReplaceChild("right_wing_tip",
            CubeListBuilder.create().texOffs(44, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F),
            PartPose.offsetAndRotation(-0.5F, 3.0F, 0.0F, 1.5708F, 0.0F, 0.0F));
        PartDefinition leftWingTip = leftWing.addOrReplaceChild("left_wing_tip",
            CubeListBuilder.create().texOffs(44, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 2.0F),
            PartPose.offsetAndRotation(0.5F, 3.0F, 0.0F, 1.5708F, 0.0F, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail",
            CubeListBuilder.create().texOffs(50, 0).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 1.0F, 3.0F),
            PartPose.offsetAndRotation(0.0F, -1.0F, 2.5F, -0.2F, 0.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(0, 10).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F).texOffs(16, 10).addBox(-1.0F, 2.0F, -1.5F, 2.0F, 1.0F, 2.0F),
            PartPose.offsetAndRotation(-1.0F, 21.0F, 0.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(0, 10).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 2.0F, 2.0F).texOffs(16, 10).mirror().addBox(-1.0F, 2.0F, -1.5F, 2.0F, 1.0F, 2.0F),
            PartPose.offsetAndRotation(1.0F, 21.0F, 0.5F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createSeagullLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body",
            CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -2.0F, -3.5F, 4.0F, 4.0F, 7.0F),
            PartPose.offsetAndRotation(0.0F, 19.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition neck = body.addOrReplaceChild("neck",
            CubeListBuilder.create().texOffs(22, 11).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, -1.0F, -2.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition head = neck.addOrReplaceChild("head",
            CubeListBuilder.create().texOffs(44, 0).addBox(-1.5F, -2.5F, -1.5F, 3.0F, 3.0F, 3.0F).texOffs(34, 11).addBox(-0.5F, -1.5F, -3.5F, 1.0F, 1.0F, 2.0F),
            PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightWing = body.addOrReplaceChild("right_wing",
            CubeListBuilder.create().texOffs(30, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 3.0F, 6.0F),
            PartPose.offsetAndRotation(-2.0F, -1.5F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftWing = body.addOrReplaceChild("left_wing",
            CubeListBuilder.create().texOffs(30, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 3.0F, 6.0F),
            PartPose.offsetAndRotation(2.0F, -1.5F, -2.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition rightWingTip = rightWing.addOrReplaceChild("right_wing_tip",
            CubeListBuilder.create().texOffs(22, 0).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 7.0F, 3.0F),
            PartPose.offsetAndRotation(-0.5F, 3.0F, 0.0F, 1.5708F, 0.0F, 0.0F));
        PartDefinition leftWingTip = leftWing.addOrReplaceChild("left_wing_tip",
            CubeListBuilder.create().texOffs(22, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 1.0F, 7.0F, 3.0F),
            PartPose.offsetAndRotation(0.5F, 3.0F, 0.0F, 1.5708F, 0.0F, 0.0F));
        PartDefinition tail = body.addOrReplaceChild("tail",
            CubeListBuilder.create().texOffs(0, 11).addBox(-1.5F, 0.0F, 0.0F, 3.0F, 1.0F, 3.0F),
            PartPose.offsetAndRotation(0.0F, -1.0F, 3.0F, -0.1F, 0.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg",
            CubeListBuilder.create().texOffs(30, 11).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(12, 11).addBox(-1.0F, 3.0F, -2.5F, 2.0F, 1.0F, 3.0F),
            PartPose.offsetAndRotation(-1.0F, 20.0F, 0.5F, 0.0F, 0.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg",
            CubeListBuilder.create().texOffs(30, 11).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F).texOffs(12, 11).mirror().addBox(-1.0F, 3.0F, -2.5F, 2.0F, 1.0F, 3.0F),
            PartPose.offsetAndRotation(1.0F, 20.0F, 0.5F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
    // END GENERATED
}
