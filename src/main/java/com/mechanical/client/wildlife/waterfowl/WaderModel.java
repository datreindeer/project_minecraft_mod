package com.mechanical.client.wildlife.waterfowl;

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
 * The long-legged waders, Grey Heron and Common Crane. The neck is two pieces (an S at rest), the legs are the bare tibia
 * and the tarsus with its long toes, bending backwards at the ankle as the bird steps.
 * <ul>
 * <li>Walking/wading: slow stilt strides, the ankle folding as each foot comes forward, a nod of the neck per step.</li>
 * <li>Heron hunting: leaning out over the water, neck forward and bill down, then the lightning spear-thrust (strike).</li>
 * <li>Crane foraging (graze): the neck sweeps down to the ground. Dancing: wings half spread and flicking, bowing.</li>
 * <li>Asleep: standing on one leg, the other drawn up, neck sunk into the shoulders.</li>
 * <li>Flight: wings beating slowly, legs trailing straight out behind; the crane flies with its neck outstretched, the
 * heron with it folded back into the shoulders.</li>
 * <li>Carcass: legs and wings sag; hung by the feet, the neck dangles straight down.</li>
 * </ul>
 * Geometry and textures: scripts/gen_small_models.py (SmallGeometry); the createXxxLayer methods below delegate there
 * (that script rewrites them; the older texture script named in the marker runs it at its end).
 */
public class WaderModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation GREY_HERON = layer("grey_heron");
    public static final ModelLayerLocation COMMON_CRANE = layer("common_crane");

    private final ModelPart body, neck, neck2, head, leftWing, rightWing, leftTip, rightTip, tail, leftLeg, rightLeg, leftShin, rightShin;
    /** Flies with the neck stretched out (crane) or folded back (heron). */
    private final boolean neckOut;
    /** Wing-beat amplitude (radians). */
    private final float beat;

    public WaderModel(ModelPart root, boolean neckOut, float beat) {
        super(root);
        body = root.getChild("body");
        neck = body.getChild("neck");
        neck2 = neck.getChild("neck2");
        head = neck2.getChild("head");
        leftWing = body.getChild("left_wing");
        rightWing = body.getChild("right_wing");
        leftTip = leftWing.getChild("left_wing_tip");
        rightTip = rightWing.getChild("right_wing_tip");
        tail = body.getChild("tail");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
        leftShin = leftLeg.getChild("left_shin");
        rightShin = rightLeg.getChild("right_shin");
        this.neckOut = neckOut;
        this.beat = beat;
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", name), "main");
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        WaterfowlRenderState s = (WaterfowlRenderState) state;
        float t = state.ageInTicks;

        if (CarcassRenderer.isHung(state)) {
            // hung by the feet (model -z is world down): legs straight up to the hook, body in line, neck dangling, wings open
            CarcassRenderer.hangLegs(new ModelPart[0], new ModelPart[] {leftLeg, rightLeg});
            leftShin.xRot = rightShin.xRot = 0.0F;
            body.xRot = 0.0F;
            neck.xRot = 1.5F;
            neck2.xRot = 0.0F;
            head.xRot = 0.0F;
            leftWing.xRot = rightWing.xRot = 3.0F;
            leftWing.zRot = 0.6F;
            rightWing.zRot = -0.6F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            // lying on its side: legs and wings sag to the ground, the long neck limp
            for (ModelPart limb : new ModelPart[] {leftLeg, rightLeg, leftWing, rightWing}) limb.zRot = CarcassRenderer.LIMB_SAG;
            leftShin.xRot = rightShin.xRot = 0.35F;
            neck.xRot = 1.1F;
            neck2.xRot = 0.5F;
            head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.8F;
            return;
        }
        if (state.flying) {
            flying(s);
            return;
        }
        head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.7F;
        head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.4F;

        if (state.dozing) {
            // on one leg: the right one drawn up under the belly, neck sunk back between the shoulders
            rightLeg.xRot = -0.9F;
            rightShin.xRot = 2.3F;
            neck.xRot -= 0.9F;
            neck2.xRot += 1.4F;
            head.xRot -= 0.5F;
            head.yRot = 0.0F;
            return;
        }

        // stilt strides, slower in the water: the swinging leg folds at the ankle as its foot comes forward
        float speed = Math.min(state.walkAnimationSpeed, 1.0F) * (s.wading ? 0.7F : 1.0F);
        float phase = state.walkAnimationPos * 0.6662F;
        float swing = Mth.cos(phase) * 0.55F * speed;
        leftLeg.xRot -= swing;
        rightLeg.xRot += swing;
        leftShin.xRot += Math.max(0.0F, Mth.sin(phase)) * 0.9F * speed;
        rightShin.xRot += Math.max(0.0F, -Mth.sin(phase)) * 0.9F * speed;
        neck.xRot += Mth.sin(phase * 2.0F) * 0.1F * speed;
        head.xRot -= Mth.sin(phase * 2.0F) * 0.1F * speed;

        if (s.hunting) {
            // leaning out over the water, neck forward and low, bill pointing down at the fish
            body.xRot += 0.3F;
            neck.xRot += 0.9F;
            neck2.xRot += 0.3F;
            head.xRot -= 0.9F;
        }
        float k = s.strike;
        if (k > 0.0F) {
            // the spear-thrust: the S snaps straight and the bill stabs down
            neck.xRot += 0.7F * k;
            neck2.xRot += 0.8F * k;
            head.xRot -= 0.9F * k;
        }

        // foraging (crane; heron rarely): the neck sweeps down to the ground, quick pecks
        float graze = state.grazeAmount;
        float peck = Math.max(0.0F, Mth.sin(t * 0.5F)) * 0.25F;
        body.xRot += 0.25F * graze;
        neck.xRot += (1.5F + peck) * graze;
        neck2.xRot += 0.5F * graze;
        head.xRot -= 0.5F * graze;

        float d = s.dance;
        if (d > 0.0F) {
            // the crane dance: wings half spread and flicking, the outer wings swung out, deep bows
            float p = t * 0.3F;
            float spread = (0.95F + 0.45F * Mth.sin(p)) * d;
            leftWing.zRot = -spread;
            rightWing.zRot = spread;
            leftTip.xRot = rightTip.xRot = Mth.HALF_PI * (1.0F - 0.8F * d);
            float bow = Math.max(0.0F, Mth.sin(p * 0.5F)) * d;
            body.xRot += 0.35F * bow;
            neck.xRot += 1.0F * bow;
            head.xRot -= 0.5F * bow;
            leftShin.xRot += 0.4F * d * Math.max(0.0F, Mth.cos(p));
            rightShin.xRot += 0.4F * d * Math.max(0.0F, Mth.cos(p));
            return;
        }

        // idle: the odd wing shuffle and tail flick
        float shuffle = Math.max(0.0F, Mth.sin(t * 0.04F + state.entityId) * 8.0F - 7.0F);
        leftWing.zRot = -0.15F * shuffle;
        rightWing.zRot = 0.15F * shuffle;
        tail.xRot += 0.1F * Math.max(0.0F, Mth.sin(t * 0.11F) * 4.0F - 3.0F);
    }

    private void flying(WaterfowlRenderState state) {
        float glide = state.glide;
        float wave = Mth.sin(state.flap) * (1.0F - glide);
        // wings level out to the sides, beating slow and deep around just above level
        float lift = Mth.HALF_PI - 0.05F + beat * wave;
        leftWing.zRot = -lift;
        rightWing.zRot = lift;
        leftWing.yRot = 0.08F;
        rightWing.yRot = -0.08F;
        for (ModelPart tip : new ModelPart[] {leftTip, rightTip}) {
            tip.x = 0.0F;
            tip.xRot = 0.0F;
        }
        float lag = 0.4F * Mth.sin(state.flap - 0.9F) * (1.0F - glide);
        leftTip.zRot = -lag + glide * 0.08F;
        rightTip.zRot = lag - glide * 0.08F;
        leftTip.yRot = 0.1F * glide;
        rightTip.yRot = -0.1F * glide;

        float bob = 0.6F * Mth.cos(state.flap) * (1.0F - glide);
        body.xRot = -Mth.clamp(state.pitch, -0.9F, 0.6F);
        body.y += bob;
        if (neckOut) {
            neck.xRot = 1.5F; // crane: neck stretched straight out ahead
            neck2.xRot = 0.0F;
            head.xRot = -1.5F;
        } else {
            neck.xRot = -0.3F; // heron: neck folded back, head drawn in between the shoulders, bill forward
            neck2.xRot = 2.0F;
            head.xRot = -1.7F;
        }
        tail.xRot = 0.0F;
        // legs trailing straight out behind, past the tail
        leftLeg.xRot = rightLeg.xRot = 1.45F;
        leftShin.xRot = rightShin.xRot = 0.0F;
        leftLeg.y += bob;
        rightLeg.y += bob;
    }

    // BEGIN GENERATED by scripts/gen_fauna_waterfowl_textures.py
    public static LayerDefinition createGreyHeronLayer() {
        return com.mechanical.client.wildlife.SmallGeometry.greyHeron();
    }

    public static LayerDefinition createCommonCraneLayer() {
        return com.mechanical.client.wildlife.SmallGeometry.commonCrane();
    }
    // END GENERATED
}
