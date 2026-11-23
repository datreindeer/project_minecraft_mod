package com.mechanical.client.wildlife.jungle;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The three little mangrove and jungle-floor creatures: Poison Dart Frog, Mudskipper and Fiddler Crab. Built at twice
 * their size in pixels and drawn at half scale (JungleFaunaClient), for finer boxes and texels. Geometry and skins:
 * scripts/gen_jungle_models.py (dart_frog(), mudskipper(), fiddler_crab(); JungleGeometry).
 */
public final class SmallJungleModels {
    private SmallJungleModels() {}

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", name), "main");
    }

    /** Frog sitting up on braced forelegs, hind legs folded; the throat pulsing; hopping it kicks the hind legs out behind. */
    public static class DartFrogModel extends EntityModel<WildAnimalRenderState> {
        public static final ModelLayerLocation LAYER = layer("poison_dart_frog");
        private final ModelPart body, head, throat, rightHind, leftHind, rightFront, leftFront;

        public DartFrogModel(ModelPart root) {
            super(root);
            body = root.getChild("body");
            head = body.getChild("head");
            throat = head.getChild("throat");
            rightHind = root.getChild("right_hind_leg");
            leftHind = root.getChild("left_hind_leg");
            rightFront = root.getChild("right_front_leg");
            leftFront = root.getChild("left_front_leg");
        }

        public static LayerDefinition createBodyLayer() {
            return JungleGeometry.dartFrog();
        }

        @Override
        public void setupAnim(WildAnimalRenderState state) {
            super.setupAnim(state);
            if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return;
            boolean air = state.walkAnimationSpeed > 0.15F;
            float kick = air ? Mth.abs(Mth.sin(state.walkAnimationPos * 0.8F)) : 0.0F;
            rightHind.xRot += kick * 0.9F; // the folded legs straighten out behind
            leftHind.xRot += kick * 0.9F;
            rightHind.z += kick * 2.0F;
            leftHind.z += kick * 2.0F;
            rightFront.xRot -= kick * 0.7F;
            leftFront.xRot -= kick * 0.7F;
            body.xRot -= kick * 0.25F;
            throat.yScale = 1.0F + Math.max(0.0F, Mth.sin(state.ageInTicks * 0.3F)) * 1.2F;
            head.yRot = Mth.clamp(state.yRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD * 0.5F;
        }
    }

    /** Mudskipper propped on its pectoral fins: crutches along on them, flicks the dorsal fin up, the tail swishing. */
    public static class MudskipperModel extends EntityModel<WildAnimalRenderState> {
        public static final ModelLayerLocation LAYER = layer("mudskipper");
        private final ModelPart body, head, tail, rightFin, leftFin;

        public MudskipperModel(ModelPart root) {
            super(root);
            body = root.getChild("body");
            head = body.getChild("head");
            tail = body.getChild("tail");
            rightFin = body.getChild("right_fin");
            leftFin = body.getChild("left_fin");
        }

        public static LayerDefinition createBodyLayer() {
            return JungleGeometry.mudskipper();
        }

        @Override
        public void setupAnim(WildAnimalRenderState state) {
            super.setupAnim(state);
            if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return;
            float speed = Math.min(state.walkAnimationSpeed * 1.5F, 1.0F);
            float p = state.walkAnimationPos * 1.6F;
            rightFin.xRot = Mth.cos(p) * 0.8F * speed; // "crutching" on the fins, the body lurching up with each push
            leftFin.xRot = Mth.cos(p) * 0.8F * speed;
            body.y -= Math.max(0.0F, Mth.sin(p)) * 1.2F * speed;
            body.xRot = -Math.max(0.0F, Mth.sin(p)) * 0.12F * speed;
            tail.yRot = Mth.sin(state.ageInTicks * (state.swimming ? 0.8F : 0.15F)) * (state.swimming ? 0.6F : 0.2F);
            head.yRot = Mth.clamp(state.yRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD * 0.4F;
        }
    }

    /**
     * Fiddler crab: a flat carapace on eye stalks, four pairs of jointed legs; the male (showsHorns) has one huge claw that
     * it waves in courtship, the hinged finger snapping, the female two small ones. Scuttles sideways; hiding it sits down
     * its hole with only the eyes on their stalks above the mud.
     */
    public static class FiddlerCrabModel extends EntityModel<WildAnimalRenderState> {
        public static final ModelLayerLocation LAYER = layer("fiddler_crab");
        private final ModelPart body, bigClaw, dactyl, femaleClaw;
        private final ModelPart[] legs = new ModelPart[8];

        public FiddlerCrabModel(ModelPart root) {
            super(root);
            body = root.getChild("body");
            bigClaw = body.getChild("big_claw");
            dactyl = bigClaw.getChild("dactyl");
            femaleClaw = body.getChild("female_claw");
            for (int i = 0; i < 4; i++) {
                legs[i] = body.getChild("right_leg" + i);
                legs[i + 4] = body.getChild("left_leg" + i);
            }
        }

        public static LayerDefinition createBodyLayer() {
            return JungleGeometry.fiddlerCrab();
        }

        @Override
        public void setupAnim(WildAnimalRenderState state) {
            super.setupAnim(state);
            bigClaw.visible = state.showsHorns;
            femaleClaw.visible = !state.showsHorns;
            if (CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return;
            if (Boolean.TRUE.equals(state.getRenderData(JungleRenderer.HIDING))) {
                body.y += 6.5F; // down its hole: only the eyes on their stalks show above the mud
                return;
            }
            float speed = Math.min(state.walkAnimationSpeed * 1.6F, 1.0F);
            float p = state.walkAnimationPos * 2.0F;
            for (int i = 0; i < 8; i++) legs[i].zRot += Mth.sin(p + i * 1.7F) * 0.3F * speed;
            body.zRot = Mth.sin(p) * 0.04F * speed;
            // the courtship wave: the great claw raised and lowered, the finger snapping at the top
            float c = (state.ageInTicks + state.entityId * 17) % 60.0F;
            float wave = c < 20 ? Mth.sin(c / 20.0F * Mth.PI) : 0.0F;
            bigClaw.xRot -= wave * 0.9F;
            bigClaw.zRot -= wave * 0.3F;
            dactyl.xRot -= wave * 0.5F;
        }
    }
}
