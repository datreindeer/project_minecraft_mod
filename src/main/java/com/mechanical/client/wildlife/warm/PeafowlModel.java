package com.mechanical.client.wildlife.warm;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Peafowl, peacock and peahen in one model: long upright neck with the fan crest, long legs. {@code showsHorns} (the
 * peacock) shows the train: seven eyed feather panels hinged at the rump, trailing behind him on the ground. With
 * {@link WarmRenderState#display} they swing up and spread into the upright fan, which shivers when fully raised (the
 * rattle); the wings droop and quiver with it. Otherwise moves like the pheasant. Geometry ({@link WarmBirdGeometry}, 128x64:
 * blue neck and breast, white face stripes, wire crest, scaled green back, barred wings, eyed 9x20 panels) and skins
 * (wildlife/peacock.png, peahen.png and the plucked ones) come from scripts/gen_owarm_birds_models.py.
 */
public class PeafowlModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "peafowl"), "main");
    private static final int PANELS = 7;
    /** Panels trailing behind (as in the layer): pitch, yaw and roll steps between neighbours; in the fan: angle between neighbours. */
    private static final float TRAIL = -Mth.HALF_PI - 0.3F, TRAIL_SPREAD = 0.05F, TRAIL_ROLL = 0.2F, FAN_STEP = 0.45F;

    private final ModelPart body, head, leftWing, rightWing, tail, train, leftLeg, rightLeg;
    private final ModelPart[] panels = new ModelPart[PANELS];

    public PeafowlModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = body.getChild("head");
        this.leftWing = body.getChild("left_wing");
        this.rightWing = body.getChild("right_wing");
        this.tail = body.getChild("tail");
        this.train = body.getChild("train");
        for (int i = 0; i < PANELS; i++) panels[i] = train.getChild("train_" + i);
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    /** Every panel at the same pitch, fanned by {@code spread} per step from the middle one. */
    private void trainPose(float pitch, float spread) {
        for (int i = 0; i < PANELS; i++) {
            panels[i].xRot = pitch;
            panels[i].yRot = (i - PANELS / 2) * spread;
        }
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        train.visible = state.showsHorns;
        leftLeg.visible = rightLeg.visible = true;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.6F;
        float t = state.ageInTicks;

        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[0], new ModelPart[] {leftLeg, rightLeg});
            head.xRot = 1.45F; // the long neck hangs straight down
            leftWing.xRot = rightWing.xRot = 3.0F;
            leftWing.zRot = 0.6F;
            rightWing.zRot = -0.6F;
            trainPose(Mth.HALF_PI - 0.15F, 0.03F); // the train falls down along the back
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart limb : new ModelPart[] {leftLeg, rightLeg, leftWing, rightWing}) limb.zRot = CarcassRenderer.LIMB_SAG;
            head.xRot += 0.6F;
            trainPose(-Mth.HALF_PI - 0.15F, 0.08F);
            return;
        }

        if (state.dozing) {
            // roosting on the ground: body levelled onto y = 24, legs tucked away, neck drawn in, the train lying flat behind
            body.xRot = 0.0F;
            body.y += 4.0F;
            leftLeg.visible = rightLeg.visible = false;
            head.xRot = 0.0F;
            head.yRot = 0.0F;
            head.y += 3.0F;
            head.z += 1.0F;
            trainPose(-Mth.HALF_PI - 0.28F, TRAIL_SPREAD);
            return;
        }

        if (state.flying) {
            // like the pheasant: wings spread and flapping, body level, neck forward, the train streaming out behind
            float beat = Mth.sin(state.flap);
            float bob = 0.6F * Mth.cos(state.flap);
            leftWing.zRot = -1.35F - 0.9F * beat;
            rightWing.zRot = 1.35F + 0.9F * beat;
            leftWing.yRot = 0.15F;
            rightWing.yRot = -0.15F;
            body.xRot += 0.15F;
            body.y += bob;
            head.xRot += 0.5F;
            tail.xRot -= 0.25F;
            trainPose(-Mth.HALF_PI + 0.05F * beat, 0.02F);
            leftLeg.xRot = rightLeg.xRot = 1.3F;
            leftLeg.y += bob - 0.5F;
            rightLeg.y += bob - 0.5F;
            return;
        }

        float display = state instanceof WarmRenderState w ? w.display : 0.0F;
        float calm = 1.0F - display;
        float speed = Math.min(state.walkAnimationSpeed, 1.0F);
        float phase = state.walkAnimationPos * 0.6662F;
        float graze = state.grazeAmount * calm;

        // walking: long strides, the head thrusts forward and back, the train swings a little behind
        leftLeg.xRot = Mth.cos(phase) * 1.0F * speed;
        rightLeg.xRot = Mth.cos(phase + Mth.PI) * 1.0F * speed;
        head.z += Mth.sin(phase * 2.0F) * 1.0F * speed;
        head.xRot += Mth.sin(phase * 2.0F) * 0.05F * speed;
        body.zRot = Mth.sin(phase) * 0.05F * speed;

        // pecking: body tips forward, the long neck dips to the ground
        float peck = Math.max(0.0F, Mth.sin(t * 0.5F)) * 0.3F;
        body.xRot += 0.3F * graze;
        head.y += 1.0F * graze;
        head.xRot += (1.3F + peck) * graze;

        // idle: small head bob and tail sway, the odd wing shuffle
        head.xRot += 0.04F * Mth.sin(t * 0.09F) * (1.0F - graze);
        float shuffle = Math.max(0.0F, Mth.sin(t * 0.07F) * 8.0F - 7.0F) * calm;
        leftWing.zRot = -0.12F * shuffle;
        rightWing.zRot = 0.12F * shuffle;

        // the display: the train lifts first, then spreads into the fan; at full height it shivers, in rattling bursts
        float lift = smoothstep(Math.min(1.0F, display * 1.6F));
        float spread = smoothstep((display - 0.3F) / 0.7F);
        float full = Mth.clamp((display - 0.85F) / 0.15F, 0.0F, 1.0F);
        float rattle = full * (0.012F + 0.045F * Math.max(0.0F, Mth.sin(t * 0.25F) * 3.0F - 2.0F));
        float pitch = Mth.lerp(lift, TRAIL + 0.04F * Mth.sin(phase) * speed, -0.12F);
        for (int i = 0; i < PANELS; i++) {
            int k = i - PANELS / 2;
            panels[i].xRot = pitch + rattle * Mth.sin(t * 3.1F + i * 1.3F);
            panels[i].yRot = k * TRAIL_SPREAD * (1.0F - lift);
            panels[i].zRot = k * Mth.lerp(spread, TRAIL_ROLL, FAN_STEP) + rattle * 0.6F * Mth.sin(t * 2.7F + i * 2.1F);
        }
        train.xRot = rattle * 0.5F * Mth.sin(t * 4.3F);
        tail.xRot -= 0.9F * lift; // the short tail feathers prop the fan from behind
        // head up and drawn back, wings lowered and quivering
        head.xRot -= 0.15F * display;
        head.z += 0.8F * display;
        float droop = 0.25F * display + rattle * 2.0F * Mth.sin(t * 3.7F);
        leftWing.zRot -= droop;
        rightWing.zRot += droop;
        leftWing.xRot = rightWing.xRot = -0.2F * display;
    }

    private static float smoothstep(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    public static LayerDefinition createBodyLayer() {
        return WarmBirdGeometry.peafowl();
    }
}
