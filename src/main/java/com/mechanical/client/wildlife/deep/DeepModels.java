package com.mechanical.client.wildlife.deep;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Models of the deep fauna: Cave Salamander, Blind Cave Fish, Bat Swarm, Cavern Spider. Parts, boxes and skins come from
 * scripts/gen_fauna_deep_textures.py (DeepGeometry); the motion is here. None of them has an emissive layer: in a
 * pitch-black cave they are as dark as the rock.
 */
public final class DeepModels {
    private DeepModels() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", name), "main");
    }

    /** Olm: a long body in three pieces that snakes as it swims, a flat snout, red gill fronds that stir, four tiny legs. */
    public static class SalamanderModel extends EntityModel<LivingEntityRenderState> {
        public static final ModelLayerLocation LAYER = layer("cave_salamander");
        private final ModelPart head, rear, tail, tip;
        private final ModelPart[] gills = new ModelPart[6], legs;

        public SalamanderModel(ModelPart root) {
            super(root);
            ModelPart body = root.getChild("body");
            head = body.getChild("head");
            rear = body.getChild("rear");
            tail = rear.getChild("tail");
            tip = tail.getChild("tail_tip");
            for (int k = 0; k < 3; k++) {
                gills[k] = head.getChild("right_gill" + k);
                gills[k + 3] = head.getChild("left_gill" + k);
            }
            legs = new ModelPart[] {body.getChild("right_front_leg"), body.getChild("left_front_leg"), rear.getChild("right_hind_leg"),
                    rear.getChild("left_hind_leg")};
        }

        public static LayerDefinition createBodyLayer() {
            return DeepGeometry.caveSalamander();
        }

        @Override
        public void setupAnim(LivingEntityRenderState state) {
            super.setupAnim(state);
            float t = state.ageInTicks, rate = state.isInWater ? 0.25F + Math.min(state.walkAnimationSpeed, 1.0F) * 0.5F : 1.2F;
            head.yRot = Mth.sin(t * rate) * 0.15F;
            rear.yRot = -Mth.sin(t * rate - 0.8F) * 0.25F;
            tail.yRot = -Mth.sin(t * rate - 1.6F) * 0.35F;
            tip.yRot = -Mth.sin(t * rate - 2.4F) * 0.45F;
            for (int k = 0; k < 6; k++) gills[k].zRot += Mth.sin(t * 0.3F + k) * 0.2F * (k < 3 ? 1 : -1); // the fronds stir
            for (int i = 0; i < 4; i++) legs[i].xRot = Mth.sin(t * rate * 1.5F + i * Mth.PI / 2) * 0.5F;
        }
    }

    /** Blind cave fish: a deep little body that bends as the forked tail beats, fins that flutter. */
    public static class CaveFishModel extends EntityModel<LivingEntityRenderState> {
        public static final ModelLayerLocation LAYER = layer("blind_cave_fish");
        private final ModelPart rear, tail, rightFin, leftFin;

        public CaveFishModel(ModelPart root) {
            super(root);
            ModelPart body = root.getChild("body");
            rear = body.getChild("rear");
            tail = rear.getChild("tail");
            rightFin = body.getChild("right_fin");
            leftFin = body.getChild("left_fin");
        }

        public static LayerDefinition createBodyLayer() {
            return DeepGeometry.blindCaveFish();
        }

        @Override
        public void setupAnim(LivingEntityRenderState state) {
            super.setupAnim(state);
            float t = state.ageInTicks, rate = state.isInWater ? 0.6F : 1.7F, beat = state.isInWater ? 1.0F : 1.4F;
            rear.yRot = -Mth.sin(t * rate) * 0.2F * beat;
            tail.yRot = -Mth.sin(t * rate - 0.7F) * 0.45F * beat;
            rightFin.yRot += Mth.sin(t * 0.4F) * 0.25F;
            leftFin.yRot -= Mth.sin(t * 0.4F) * 0.25F;
        }
    }

    /** Bat swarm: {@link #BATS} little bats (drawn at {@link #SIZE}) wheeling in a loose cloud, two-piece wings flapping out of step. */
    public static class BatSwarmModel extends EntityModel<LivingEntityRenderState> {
        public static final ModelLayerLocation LAYER = layer("bat_swarm");
        static final int BATS = 18;
        private static final float SIZE = 0.6F;
        /** Centre of the cloud in model space (the swarm is 1 block tall; the feet are at y 24). */
        private static final float CY = 16.0F;
        private final ModelPart[] bats = new ModelPart[BATS], right = new ModelPart[BATS], left = new ModelPart[BATS],
                rightTip = new ModelPart[BATS], leftTip = new ModelPart[BATS];

        public BatSwarmModel(ModelPart root) {
            super(root);
            for (int i = 0; i < BATS; i++) {
                bats[i] = root.getChild("bat_" + i);
                right[i] = bats[i].getChild("right_wing");
                left[i] = bats[i].getChild("left_wing");
                rightTip[i] = right[i].getChild("right_wing_tip");
                leftTip[i] = left[i].getChild("left_wing_tip");
            }
        }

        public static LayerDefinition createBodyLayer() {
            return DeepGeometry.bat();
        }

        @Override
        public void setupAnim(LivingEntityRenderState state) {
            super.setupAnim(state);
            float t = state.ageInTicks;
            for (int i = 0; i < BATS; i++) {
                ModelPart b = bats[i];
                float a = i * 2.39996F, r = 3.5F + (i * 7 % 5) * 1.3F, w = 0.2F + (i % 6) * 0.04F;
                float ang = t * w + a;
                b.x = Mth.cos(ang) * r + Mth.sin(t * 0.21F + a * 3) * 1.5F;
                b.z = Mth.sin(ang) * r + Mth.cos(t * 0.19F + a * 2) * 1.5F;
                b.y = CY + Mth.sin(t * (0.17F + (i % 5) * 0.03F) + a * 5) * 4.0F;
                b.yRot = -ang; // flying round the cloud, nose first
                b.xScale = b.yScale = b.zScale = SIZE;
                float flap = Mth.sin(t * 1.6F + a * 7);
                right[i].zRot = flap * 0.8F;
                left[i].zRot = -flap * 0.8F;
                rightTip[i].zRot = flap * 0.5F + 0.1F;
                leftTip[i].zRot = -flap * 0.5F - 0.1F;
            }
        }
    }

    /**
     * Cavern spider: a hairy cephalothorax, a head with fangs and pedipalps, a big tilted abdomen, eight legs whose knees
     * stand high over the body (a femur up from the body, a long shin down to the ground), stepping in two alternating sets.
     */
    public static class CavernSpiderModel extends EntityModel<WildAnimalRenderState> {
        public static final ModelLayerLocation LAYER = layer("cavern_spider");
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
            return DeepGeometry.cavernSpider();
        }

        @Override
        public void setupAnim(WildAnimalRenderState state) {
            super.setupAnim(state);
            if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return; // legs limp: rest pose
            head.yRot = Mth.clamp(state.yRot, -40.0F, 40.0F) * Mth.DEG_TO_RAD;
            head.xRot = state.xRot * Mth.DEG_TO_RAD;
            float p = state.walkAnimationPos * 1.33F, s = Math.min(state.walkAnimationSpeed, 1.0F);
            for (int i = 0; i < 4; i++) {
                // legs 0 and 2 of one side step with 1 and 3 of the other (vanilla spider gait)
                float phase = (i % 2 == 0 ? 0 : Mth.PI);
                float swing = Mth.cos(p * 2 + phase) * 0.4F * s, lift = Math.abs(Mth.sin(p + phase)) * 0.3F * s;
                right[i].yRot += swing;
                left[i].yRot -= swing;
                right[i].zRot += lift;
                left[i].zRot -= lift;
            }
        }
    }
}
