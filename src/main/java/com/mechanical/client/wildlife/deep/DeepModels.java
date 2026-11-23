package com.mechanical.client.wildlife.deep;

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
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Models of the deep fauna: Cave Salamander, Blind Cave Fish, Bat Swarm, Cavern Spider. Textures and UVs:
 * scripts/gen_fauna_deep_textures.py (its --java prints the parts below). None of them has an emissive layer: in a
 * pitch-black cave they are as dark as the rock.
 */
public final class DeepModels {
    private DeepModels() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", name), "main");
    }

    private static final CubeDeformation PLANE = new CubeDeformation(0.001F);

    /** Olm: long thin body, a flat snout, red gill fronds, tiny legs, a finned tail; the body snakes as it swims. */
    public static class SalamanderModel extends EntityModel<LivingEntityRenderState> {
        public static final ModelLayerLocation LAYER = layer("cave_salamander");
        private final ModelPart body, head, tail, rightGill, leftGill;
        private final ModelPart[] legs;

        public SalamanderModel(ModelPart root) {
            super(root);
            body = root.getChild("body");
            head = body.getChild("head");
            tail = body.getChild("tail");
            rightGill = head.getChild("right_gill");
            leftGill = head.getChild("left_gill");
            legs = new ModelPart[] {body.getChild("right_front_leg"), body.getChild("left_front_leg"), body.getChild("right_hind_leg"),
                    body.getChild("left_hind_leg")};
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 1.0F, 6.0F), PartPose.offset(0.0F, 24.0F, 0.0F));
            PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(8, 7).addBox(-1.0F, -1.0F, -3.0F, 2.0F, 1.0F, 3.0F), PartPose.offset(0.0F, 0.0F, -3.0F));
            head.addOrReplaceChild("right_gill", CubeListBuilder.create().texOffs(22, 7).addBox(-1.0F, -1.5F, 0.0F, 1.0F, 2.0F, 0.0F, PLANE), PartPose.offset(-1.0F, -0.5F, -0.5F));
            head.addOrReplaceChild("left_gill", CubeListBuilder.create().texOffs(22, 7).mirror().addBox(0.0F, -1.5F, 0.0F, 1.0F, 2.0F, 0.0F, PLANE).mirror(false), PartPose.offset(1.0F, -0.5F, -0.5F));
            body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 0).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 1.0F, 5.0F).texOffs(0, 7).addBox(0.0F, -2.0F, 1.0F, 0.0F, 1.0F, 4.0F, PLANE), PartPose.offset(0.0F, 0.0F, 3.0F));
            body.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(18, 7).addBox(-1.0F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-1.0F, -1.0F, -2.0F));
            body.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(18, 7).mirror().addBox(0.0F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F).mirror(false), PartPose.offset(1.0F, -1.0F, -2.0F));
            body.addOrReplaceChild("right_hind_leg", CubeListBuilder.create().texOffs(18, 7).addBox(-1.0F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F), PartPose.offset(-1.0F, -1.0F, 2.0F));
            body.addOrReplaceChild("left_hind_leg", CubeListBuilder.create().texOffs(18, 7).mirror().addBox(0.0F, 0.0F, -0.5F, 1.0F, 1.0F, 1.0F).mirror(false), PartPose.offset(1.0F, -1.0F, 2.0F));
            return LayerDefinition.create(mesh, 32, 32);
        }

        @Override
        public void setupAnim(LivingEntityRenderState state) {
            super.setupAnim(state);
            float t = state.ageInTicks, rate = state.isInWater ? 0.25F + Math.min(state.walkAnimationSpeed, 1.0F) * 0.5F : 1.2F;
            head.yRot = Mth.sin(t * rate) * 0.15F;
            tail.yRot = -Mth.sin(t * rate - 1.0F) * 0.45F;
            rightGill.yRot = -0.3F - Mth.sin(t * 0.3F) * 0.25F; // the fronds stir
            leftGill.yRot = 0.3F + Mth.sin(t * 0.3F) * 0.25F;
            for (int i = 0; i < 4; i++) legs[i].xRot = Mth.sin(t * rate * 1.5F + i * Mth.PI / 2) * 0.5F;
        }
    }

    /** Blind cave fish: a small pale body, a dorsal fin, a beating tail. */
    public static class CaveFishModel extends EntityModel<LivingEntityRenderState> {
        public static final ModelLayerLocation LAYER = layer("blind_cave_fish");
        private final ModelPart tail;

        public CaveFishModel(ModelPart root) {
            super(root);
            tail = root.getChild("body").getChild("tail");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -1.5F, -2.5F, 2.0F, 3.0F, 5.0F).texOffs(20, 0).addBox(0.0F, -2.5F, -1.0F, 0.0F, 1.0F, 3.0F, PLANE), PartPose.offset(0.0F, 22.0F, 0.0F));
            body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(14, 0).addBox(0.0F, -1.5F, 0.0F, 0.0F, 3.0F, 3.0F, PLANE), PartPose.offset(0.0F, 0.0F, 2.5F));
            return LayerDefinition.create(mesh, 32, 32);
        }

        @Override
        public void setupAnim(LivingEntityRenderState state) {
            super.setupAnim(state);
            float rate = state.isInWater ? 0.6F : 1.7F, beat = state.isInWater ? 1.0F : 1.4F;
            tail.yRot = -Mth.sin(state.ageInTicks * rate) * 0.45F * beat;
        }
    }

    /** Bat swarm: {@link #BATS} little bats wheeling in a loose cloud, wings flapping out of step. */
    public static class BatSwarmModel extends EntityModel<LivingEntityRenderState> {
        public static final ModelLayerLocation LAYER = layer("bat_swarm");
        static final int BATS = 18;
        /** Centre of the cloud in model space (the swarm is 1 block tall; the feet are at y 24). */
        private static final float CY = 16.0F;
        private final ModelPart[] bats = new ModelPart[BATS], right = new ModelPart[BATS], left = new ModelPart[BATS];

        public BatSwarmModel(ModelPart root) {
            super(root);
            for (int i = 0; i < BATS; i++) {
                bats[i] = root.getChild("bat_" + i);
                right[i] = bats[i].getChild("right_wing");
                left[i] = bats[i].getChild("left_wing");
            }
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            for (int i = 0; i < BATS; i++) {
                PartDefinition bat = root.addOrReplaceChild("bat_" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -0.5F, -1.0F, 1.0F, 1.0F, 2.0F), PartPose.offset(0.0F, CY, 0.0F));
                bat.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(6, 0).addBox(-3.0F, 0.0F, -1.0F, 3.0F, 0.0F, 2.0F, PLANE), PartPose.offset(-0.5F, 0.0F, 0.0F));
                bat.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(6, 0).mirror().addBox(0.0F, 0.0F, -1.0F, 3.0F, 0.0F, 2.0F, PLANE).mirror(false), PartPose.offset(0.5F, 0.0F, 0.0F));
            }
            return LayerDefinition.create(mesh, 16, 16);
        }

        @Override
        public void setupAnim(LivingEntityRenderState state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            for (int i = 0; i < BATS; i++) {
                ModelPart b = bats[i];
                float a = i * 2.39996F, r = 3.0F + (i * 7 % 5) * 1.2F, w = 0.2F + (i % 6) * 0.04F;
                float ang = t * w + a;
                b.x = Mth.cos(ang) * r + Mth.sin(t * 0.21F + a * 3) * 1.5F;
                b.z = Mth.sin(ang) * r + Mth.cos(t * 0.19F + a * 2) * 1.5F;
                b.y = CY + Mth.sin(t * (0.17F + (i % 5) * 0.03F) + a * 5) * 4.0F;
                b.yRot = -ang; // flying round the cloud, nose first
                float flap = Mth.sin(t * 1.6F + a * 7) * 0.9F;
                right[i].zRot = flap;
                left[i].zRot = -flap;
            }
        }
    }

    /** Cavern spider: thorax, head with fangs, a big tilted abdomen, eight long legs that step in two alternating sets. */
    public static class CavernSpiderModel extends EntityModel<WildAnimalRenderState> {
        public static final ModelLayerLocation LAYER = layer("cavern_spider");
        /** Resting (zRot, yRot) of the right legs, front to back (the left ones mirrored): as in the texture script. */
        private static final float[][] REST = {{-0.5F, -0.6F}, {-0.45F, -0.25F}, {-0.45F, 0.25F}, {-0.5F, 0.6F}};
        private final ModelPart head;
        private final ModelPart[] right = new ModelPart[4], left = new ModelPart[4];

        public CavernSpiderModel(ModelPart root) {
            super(root);
            head = root.getChild("body").getChild("head");
            for (int i = 0; i < 4; i++) {
                right[i] = root.getChild("right_leg" + i);
                left[i] = root.getChild("left_leg" + i);
            }
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(30, 0).addBox(-2.5F, -2.0F, -3.0F, 5.0F, 4.0F, 5.0F), PartPose.offset(0.0F, 20.0F, 0.0F));
            body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(50, 0).addBox(-2.0F, -1.5F, -3.0F, 4.0F, 3.0F, 3.0F).texOffs(20, 14).addBox(-1.5F, 1.5F, -3.0F, 1.0F, 1.0F, 1.0F).texOffs(20, 14).mirror().addBox(0.5F, 1.5F, -3.0F, 1.0F, 1.0F, 1.0F).mirror(false), PartPose.offset(0.0F, 0.0F, -3.0F));
            body.addOrReplaceChild("abdomen", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -3.5F, 0.0F, 7.0F, 6.0F, 8.0F), PartPose.offsetAndRotation(0.0F, -0.5F, 2.0F, 0.25F, 0.0F, 0.0F));
            float[] zs = {-2.0F, -1.0F, 0.0F, 1.0F};
            for (int i = 0; i < 4; i++) {
                root.addOrReplaceChild("right_leg" + i, CubeListBuilder.create().texOffs(0, 14).addBox(-9.0F, -0.5F, -0.5F, 9.0F, 1.0F, 1.0F),
                        PartPose.offsetAndRotation(-2.5F, 19.5F, zs[i], 0.0F, REST[i][1], REST[i][0]));
                root.addOrReplaceChild("left_leg" + i, CubeListBuilder.create().texOffs(0, 14).mirror().addBox(0.0F, -0.5F, -0.5F, 9.0F, 1.0F, 1.0F).mirror(false),
                        PartPose.offsetAndRotation(2.5F, 19.5F, zs[i], 0.0F, -REST[i][1], -REST[i][0]));
            }
            return LayerDefinition.create(mesh, 64, 32);
        }

        @Override
        public void setupAnim(WildAnimalRenderState state) {
            super.setupAnim(state);
            if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return; // legs curled limp: rest pose
            head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
            head.xRot = state.xRot * Mth.DEG_TO_RAD;
            float p = state.walkAnimationPos * 1.33F, s = Math.min(state.walkAnimationSpeed, 1.0F);
            for (int i = 0; i < 4; i++) {
                // legs 0 and 2 of one side step with 1 and 3 of the other (vanilla spider gait)
                float phase = (i % 2 == 0 ? 0 : Mth.PI);
                float swing = Mth.cos(p * 2 + phase) * 0.4F * s, lift = Math.abs(Mth.sin(p + phase)) * 0.4F * s;
                right[i].yRot += swing;
                left[i].yRot -= swing;
                right[i].zRot += lift;
                left[i].zRot -= lift;
            }
        }
    }
}
