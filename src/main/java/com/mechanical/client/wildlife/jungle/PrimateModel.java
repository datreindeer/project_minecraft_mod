package com.mechanical.client.wildlife.jungle;

import java.util.function.Function;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Ape and monkey on all fours, two part trees with the same names (scripts/gen_jungle_models.py: orangutan(),
 * proboscis(); JungleGeometry): a body leaning forward from the hips, the head turned back up to face ahead, arms of upper
 * arm and forearm reaching the ground, legs of thigh and shin. The Orangutan adds the cheek flanges and a hanging skirt of
 * hair; the Proboscis Monkey a long tail and the male's great nose ("nose", shown when showsHorns; the others get the
 * small "snub"). Walks in the diagonal gait, arms and legs swinging, the forearms and shins bending through the step;
 * looks about, scratches now and then; asleep it sits hunched, arms folded round the knees, head sunk.
 */
public class PrimateModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation ORANGUTAN = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "orangutan"), "main");
    public static final ModelLayerLocation PROBOSCIS = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "proboscis_monkey"), "main");
    private final ModelPart body, head, rightArm, leftArm, rightForearm, leftForearm, rightLeg, leftLeg, rightShin, leftShin;
    /** Proboscis monkey only (null on the orangutan). */
    private final ModelPart nose, snub, tail, tailTip;
    /** The body's rest lean (radians) and how far it drops (px) to sit on its rump. */
    private final float lean, sitDrop;

    public PrimateModel(ModelPart root, float lean, float sitDrop) {
        super(root);
        this.lean = lean;
        this.sitDrop = sitDrop;
        Function<String, ModelPart> part = root.createPartLookup();
        body = part.apply("body");
        head = part.apply("head");
        rightArm = part.apply("right_arm");
        leftArm = part.apply("left_arm");
        rightForearm = part.apply("right_forearm");
        leftForearm = part.apply("left_forearm");
        rightLeg = part.apply("right_leg");
        leftLeg = part.apply("left_leg");
        rightShin = part.apply("right_shin");
        leftShin = part.apply("left_shin");
        nose = part.apply("nose");
        snub = part.apply("snub");
        tail = part.apply("tail");
        tailTip = part.apply("tail_tip");
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        if (nose != null) {
            nose.visible = state.showsHorns;
            snub.visible = !state.showsHorns;
        }
        float age = state.ageInTicks;
        if (CarcassRenderer.isHung(state)) {
            CarcassRenderer.hangLegs(new ModelPart[] {leftArm, rightArm}, new ModelPart[] {leftLeg, rightLeg});
            rightArm.xRot -= lean; // the arms hang from the leaning body
            leftArm.xRot -= lean;
            head.xRot += 0.4F;
            return;
        }
        if (CarcassRenderer.isCarcass(state)) {
            for (ModelPart limb : new ModelPart[] {rightArm, leftArm, rightLeg, leftLeg}) limb.zRot = CarcassRenderer.LIMB_SAG;
            rightForearm.xRot = leftForearm.xRot = -0.3F;
            head.xRot += 0.35F;
            head.yRot = 0.4F;
            if (tail != null) tail.xRot += 0.9F;
            return;
        }
        if (state.dozing) {
            // sitting up on the rump, thighs forward and shins down, arms folded round the knees, head sunk
            float sit = lean - 0.2F;
            body.xRot -= sit;
            body.y += sitDrop;
            head.xRot += sit + 0.3F + Mth.sin(age * 0.06F) * 0.03F;
            rightArm.xRot += sit - 0.75F;
            leftArm.xRot += sit - 0.75F;
            rightForearm.xRot = leftForearm.xRot = -1.0F;
            rightArm.zRot = 0.3F;
            leftArm.zRot = -0.3F;
            rightLeg.xRot = leftLeg.xRot = -1.45F;
            rightShin.xRot = leftShin.xRot = 1.35F;
            rightLeg.y += sitDrop - 1.0F;
            leftLeg.y += sitDrop - 1.0F;
            if (tail != null) tail.xRot += sit + 0.5F;
            return;
        }
        float speed = Math.min(state.walkAnimationSpeed * 1.3F, 1.0F);
        float phase = state.walkAnimationPos * 0.7F;
        float swing = Mth.cos(phase) * 0.6F * speed;
        rightArm.xRot += swing;
        leftLeg.xRot += swing;
        leftArm.xRot -= swing;
        rightLeg.xRot -= swing;
        // the limb coming forward bends: forearm folds back, shin folds back
        rightForearm.xRot += Math.max(0.0F, -Mth.sin(phase)) * 0.6F * speed;
        leftForearm.xRot += Math.max(0.0F, Mth.sin(phase)) * 0.6F * speed;
        leftShin.xRot += Math.max(0.0F, -Mth.sin(phase)) * 0.7F * speed;
        rightShin.xRot += Math.max(0.0F, Mth.sin(phase)) * 0.7F * speed;
        body.y -= Math.abs(Mth.sin(phase)) * 0.6F * speed;
        body.zRot = Mth.sin(phase) * 0.04F * speed;
        head.yRot = Mth.clamp(state.yRot, -50.0F, 50.0F) * Mth.DEG_TO_RAD;
        head.xRot += Mth.clamp(state.xRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD;
        if (tail != null) {
            tail.xRot += Mth.sin(age * 0.07F) * 0.06F - speed * 0.4F;
            tail.zRot = Mth.sin(age * 0.05F) * 0.1F;
            tailTip.xRot += Mth.sin(age * 0.07F - 1.0F) * 0.1F;
        }
        // idle: scratching now and then, one arm raised to the head
        float c = (age + state.entityId * 31) % 160.0F;
        if (speed < 0.1F && c < 30) {
            rightArm.xRot -= 1.6F + Mth.sin(c * 0.9F) * 0.2F;
            rightForearm.xRot = -1.4F;
        }
    }
}
