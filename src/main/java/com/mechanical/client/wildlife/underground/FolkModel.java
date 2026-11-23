package com.mechanical.client.wildlife.underground;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The two-legged folk of the dark: the Vittra (slender, a big head with long pointed ears and huge eyes) and the Troll
 * (hunched and massive, long arms, a great nose, tusks and the cow's tail of the old tales). Same parts for both: the body
 * pivots at the hips and carries head and arms, so the troll's hunch brings them forward. Walking the legs and arms swing;
 * hunting, the arms reach forward and claw. Geometry and textures (vittra.png 64x64, troll.png 128x64) from one table each in
 * scripts/gen_hunt_models.py (UndergroundGeometry).
 */
public class FolkModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation VITTRA = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "vittra"), "main");
    public static final ModelLayerLocation TROLL = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "troll"), "main");

    private final ModelPart body, head, rightArm, leftArm, rightLeg, leftLeg;
    private final float hunch;
    /** Gaunt (the Vittra): a slow stalking stride, the long arms hanging near still. */
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

    /** Vittra: slender, a big head with huge eyes, long pointed ears and long hair, a ragged wadmal tunic, arms to the knees. */
    public static LayerDefinition createVittraLayer() {
        return UndergroundGeometry.vittra();
    }

    /** Troll: hunched and massive, a moss-grown hump, a great nose and tusks, huge hands, a hide kilt and the cow's tail. */
    public static LayerDefinition createTrollLayer() {
        return UndergroundGeometry.troll();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
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
