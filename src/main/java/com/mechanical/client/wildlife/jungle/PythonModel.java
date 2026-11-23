package com.mechanical.client.wildlife.jungle;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.meadow.SnakeModel;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Python: a broad squared head over a lower jaw that gapes, a flicking forked tongue, then a chain of a neck, ten body
 * segments thickest in the middle and a two-part tail, each hinged on the one before. It glides in a slow travelling wave
 * (as SnakeModel, with more and shorter links); coiled ({@link SnakeModel#COILED}) it lies in a heaped spiral with the
 * head resting on top; striking it rears the front up with the jaws open. Geometry and skin: scripts/gen_jungle_models.py
 * (python_snake(), JungleGeometry.python()).
 */
public class PythonModel extends EntityModel<WildAnimalRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "python"), "main");
    private final ModelPart neck, head, jaw, tongue;
    /** neck, seg0..seg9, tail0, tail1: each a child of the one before. */
    private final ModelPart[] chain = new ModelPart[13];

    public PythonModel(ModelPart root) {
        super(root);
        neck = root.getChild("neck");
        head = neck.getChild("head");
        jaw = head.getChild("jaw");
        tongue = head.getChild("tongue");
        chain[0] = neck;
        for (int i = 1; i < chain.length; i++) chain[i] = chain[i - 1].getChild(i <= 10 ? "seg" + (i - 1) : "tail" + (i - 11));
    }

    public static LayerDefinition createBodyLayer() {
        return JungleGeometry.python();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        tongue.visible = false;
        if (CarcassRenderer.isHung(state)) return; // straight down from the tail
        if (CarcassRenderer.isCarcass(state)) {
            for (int i = 0; i < chain.length; i++) chain[i].yRot = 0.22F * Mth.sin(i * 0.9F);
            jaw.xRot = 0.25F;
            tongue.visible = true;
            return;
        }
        if (Boolean.TRUE.equals(state.getRenderData(SnakeModel.COILED))) {
            // a heaped spiral: each link turns a little more, the inner turns riding up on the outer; head on top
            for (int i = 1; i < chain.length; i++) {
                chain[i].yRot = 0.62F + 0.035F * i;
                if (i == 6 || i == 10) chain[i].y -= 2.6F;
            }
            float breathe = Mth.sin(t * 0.05F) * 0.03F;
            neck.y -= 3.2F;
            head.yRot = -0.9F;
            head.xRot = 0.1F + breathe;
            tongue.visible = Mth.sin(t * 0.19F) > 0.94F;
            return;
        }
        float speed = Math.min(state.walkAnimationSpeed * 1.5F, 1.0F);
        float phase = state.walkAnimationPos * 0.8F + t * 0.03F;
        float amp = 0.1F + 0.3F * speed;
        float prev = 0.0F;
        for (int i = 0; i < chain.length; i++) {
            float theta = amp * Mth.sin(phase - i * 0.7F);
            chain[i].yRot = theta - prev;
            prev = theta;
        }
        head.yRot = -chain[0].yRot * 0.8F + Mth.clamp(state.yRot, -35.0F, 35.0F) * Mth.DEG_TO_RAD;
        head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.3F;
        tongue.visible = Mth.sin(t * 0.4F) > 0.7F;
        tongue.z = tongue.visible ? -Mth.abs(Mth.sin(t * 2.3F)) * 0.8F : 0.0F;
        if (state.charging) {
            // reared in an S, the head drawn back, jaws parted
            neck.y -= 3.0F;
            neck.xRot = 0.5F;
            chain[1].xRot = -0.35F;
            chain[2].xRot = -0.15F;
            head.xRot = -0.6F;
            head.z += 0.6F + Mth.sin(t * 0.8F) * 0.3F;
            jaw.xRot = 0.35F;
            tongue.visible = true;
        }
    }
}
