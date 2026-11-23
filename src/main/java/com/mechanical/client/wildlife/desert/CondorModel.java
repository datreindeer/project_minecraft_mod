package com.mechanical.client.wildlife.desert;

import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.forestbirds.ForestBirdModel;

import net.minecraft.client.model.geom.ModelPart;

/**
 * Condor: the forest birds' upright carrion-bird animation (ForestBirdModel) on its own build from
 * scripts/gen_desert_models.py (DesertGeometry): a big black body, the white ruff round the bare red-grey neck, a bald
 * head with its comb, the white panel on each folded wing, and a 3-block span in flight with the primaries spread like
 * fingers at the wing tips (hidden on the folded wing, where they would stick out behind).
 */
public class CondorModel extends ForestBirdModel {
    private final ModelPart leftFingers, rightFingers;

    public CondorModel(ModelPart root) {
        super(root, 0.7F, 0.4F, 0.5F);
        ModelPart body = root.getChild("body");
        leftFingers = body.getChild("left_wing").getChild("left_wing_tip").getChild("fingers");
        rightFingers = body.getChild("right_wing").getChild("right_wing_tip").getChild("fingers");
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        leftFingers.visible = rightFingers.visible = state.flying;
    }
}
