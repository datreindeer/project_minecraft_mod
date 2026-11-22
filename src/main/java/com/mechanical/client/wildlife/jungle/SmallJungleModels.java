package com.mechanical.client.wildlife.jungle;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The three little mangrove and jungle-floor creatures: Poison Dart Frog, Mudskipper and Fiddler Crab. Textures 32x32:
 * scripts/gen_fauna_jungle_textures.py (dart_frog(), mudskipper(), fiddler_crab()), the parts below printed by its --java.
 */
public final class SmallJungleModels {
    private SmallJungleModels() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", name), "main");
    }

    /** Frog: squat body, a head with bulging eyes, folded hind legs; sits, throat pulsing; hops (legs kick back in the air). */
    public static class DartFrogModel extends EntityModel<WildAnimalRenderState> {
        public static final ModelLayerLocation LAYER = layer("poison_dart_frog");
        private final ModelPart body, head, rightHind, leftHind, rightFront, leftFront;

        public DartFrogModel(ModelPart root) {
            super(root);
            body = root.getChild("body");
            head = body.getChild("head");
            rightHind = root.getChild("right_hind_leg");
            leftHind = root.getChild("left_hind_leg");
            rightFront = root.getChild("right_front_leg");
            leftFront = root.getChild("left_front_leg");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 2.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 22.5F, 0.0F, -0.2F, 0.0F, 0.0F));
            body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(14, 0).addBox(-1.5F, -1.0F, -2.0F, 3.0F, 2.0F, 2.0F).texOffs(4, 6).addBox(-1.5F, -1.5F, -1.5F, 1.0F, 1.0F, 1.0F).texOffs(4, 6).mirror().addBox(0.5F, -1.5F, -1.5F, 1.0F, 1.0F, 1.0F).mirror(false), PartPose.offsetAndRotation(0.0F, -0.5F, -1.5F, 0.2F, 0.0F, 0.0F));
            root.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(0, 6).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-1.0F, 23.0F, -1.5F));
            root.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(0, 6).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F).mirror(false), PartPose.offset(1.0F, 23.0F, -1.5F));
            root.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(24, 0).addBox(-1.0F, 0.0F, -2.0F, 1.0F, 1.0F, 3.0F), PartPose.offset(-1.5F, 23.0F, 1.5F));
            root.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(24, 0).mirror().addBox(0.0F, 0.0F, -2.0F, 1.0F, 1.0F, 3.0F).mirror(false), PartPose.offset(1.5F, 23.0F, 1.5F));
            return LayerDefinition.create(mesh, 32, 32);
        }

        @Override
        public void setupAnim(WildAnimalRenderState state) {
            super.setupAnim(state);
            if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return;
            boolean air = state.walkAnimationSpeed > 0.15F;
            float kick = air ? Mth.abs(Mth.sin(state.walkAnimationPos * 0.8F)) : 0.0F;
            rightHind.xRot = leftHind.xRot = -kick * 0.9F;
            rightHind.z += kick * 1.2F;
            leftHind.z += kick * 1.2F;
            rightFront.xRot = leftFront.xRot = -kick * 0.6F;
            body.yScale = 1.0F + Math.max(0.0F, Mth.sin(state.ageInTicks * 0.25F)) * 0.08F; // throat pulsing
            head.yRot = Mth.clamp(state.yRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD * 0.5F;
        }
    }

    /** Mudskipper: a goggle-eyed head, a long body with a sail-like dorsal fin, a tail fin, pectoral fins it walks on. */
    public static class MudskipperModel extends EntityModel<WildAnimalRenderState> {
        public static final ModelLayerLocation LAYER = layer("mudskipper");
        private final ModelPart body, tail, rightFin, leftFin;

        public MudskipperModel(ModelPart root) {
            super(root);
            body = root.getChild("body");
            tail = body.getChild("tail");
            rightFin = body.getChild("right_fin");
            leftFin = body.getChild("left_fin");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 5.0F).texOffs(14, 0).addBox(0.0F, -3.0F, -1.0F, 0.0F, 2.0F, 4.0F, new CubeDeformation(0.001F)), PartPose.offset(0.0F, 22.5F, 0.0F));
            body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 7).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 3.0F).texOffs(12, 7).addBox(-1.5F, -2.0F, -2.0F, 1.0F, 1.0F, 1.0F).texOffs(12, 7).mirror().addBox(0.5F, -2.0F, -2.0F, 1.0F, 1.0F, 1.0F).mirror(false), PartPose.offset(0.0F, 0.0F, -2.0F));
            body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(22, 0).addBox(0.0F, -1.5F, 0.0F, 0.0F, 3.0F, 3.0F, new CubeDeformation(0.001F)), PartPose.offset(0.0F, 0.0F, 3.0F));
            body.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(16, 7).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-1.0F, 0.5F, -1.5F, 0.0F, 0.0F, 0.5F));
            body.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(16, 7).mirror().addBox(-0.5F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F).mirror(false), PartPose.offsetAndRotation(1.0F, 0.5F, -1.5F, 0.0F, 0.0F, -0.5F));
            return LayerDefinition.create(mesh, 32, 32);
        }

        @Override
        public void setupAnim(WildAnimalRenderState state) {
            super.setupAnim(state);
            if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return;
            float speed = Math.min(state.walkAnimationSpeed * 1.5F, 1.0F);
            float p = state.walkAnimationPos * 1.6F;
            rightFin.xRot = Mth.cos(p) * 0.8F * speed; // "crutching" on the fins
            leftFin.xRot = Mth.cos(p) * 0.8F * speed;
            body.y -= Math.max(0.0F, Mth.sin(p)) * 0.6F * speed;
            tail.yRot = Mth.sin(state.ageInTicks * (state.swimming ? 0.8F : 0.15F)) * (state.swimming ? 0.6F : 0.2F);
        }
    }

    /** Fiddler crab: a flat shell on eye stalks, one huge claw, a little one, six legs; scuttles sideways, waves the big claw. */
    public static class FiddlerCrabModel extends EntityModel<WildAnimalRenderState> {
        public static final ModelLayerLocation LAYER = layer("fiddler_crab");
        private final ModelPart body, bigClaw;
        private final ModelPart[] legs = new ModelPart[6];

        public FiddlerCrabModel(ModelPart root) {
            super(root);
            body = root.getChild("body");
            bigClaw = body.getChild("big_claw");
            for (int i = 0; i < 3; i++) {
                legs[i] = body.getChild("right_leg" + i);
                legs[i + 3] = body.getChild("left_leg" + i);
            }
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -1.0F, -1.5F, 4.0F, 2.0F, 3.0F).texOffs(0, 5).addBox(-1.5F, -3.0F, -1.5F, 1.0F, 2.0F, 1.0F).texOffs(0, 5).mirror().addBox(0.5F, -3.0F, -1.5F, 1.0F, 2.0F, 1.0F).mirror(false), PartPose.offset(0.0F, 22.0F, 0.0F));
            body.addOrReplaceChild("big_claw", CubeListBuilder.create().texOffs(14, 0).addBox(-2.0F, -1.0F, -3.0F, 2.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(-2.5F, 0.0F, -1.5F, 0.0F, 0.3F, 0.0F));
            body.addOrReplaceChild("small_claw", CubeListBuilder.create().texOffs(24, 0).addBox(0.0F, -0.5F, -2.0F, 1.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(2.0F, 0.0F, -1.5F, 0.0F, -0.3F, 0.0F));
            float[] zs = {-0.5F, 0.5F, 1.5F};
            for (int i = 0; i < 3; i++) {
                body.addOrReplaceChild("right_leg" + i, CubeListBuilder.create().texOffs(4, 5).addBox(-3.0F, 0.0F, -0.5F, 3.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, 0.5F, zs[i], 0.0F, 0.0F, 0.6F));
                body.addOrReplaceChild("left_leg" + i, CubeListBuilder.create().texOffs(4, 5).mirror().addBox(0.0F, 0.0F, -0.5F, 3.0F, 1.0F, 1.0F).mirror(false), PartPose.offsetAndRotation(2.0F, 0.5F, zs[i], 0.0F, 0.0F, -0.6F));
            }
            return LayerDefinition.create(mesh, 32, 32);
        }

        @Override
        public void setupAnim(WildAnimalRenderState state) {
            super.setupAnim(state);
            if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return;
            if (Boolean.TRUE.equals(state.getRenderData(JungleRenderer.HIDING))) {
                body.y += 2.6F; // down its hole: only the eyes on their stalks show above the mud
                return;
            }
            float speed = Math.min(state.walkAnimationSpeed * 1.6F, 1.0F);
            float p = state.walkAnimationPos * 2.0F;
            for (int i = 0; i < 6; i++) legs[i].zRot += Mth.sin(p + i * 1.7F) * 0.35F * speed;
            body.zRot = Mth.sin(p) * 0.05F * speed;
            // the courtship wave of the big claw
            float c = (state.ageInTicks + state.entityId * 17) % 60.0F;
            bigClaw.xRot = c < 20 ? -Mth.sin(c / 20.0F * Mth.PI) * 1.1F : 0.0F;
        }
    }
}
