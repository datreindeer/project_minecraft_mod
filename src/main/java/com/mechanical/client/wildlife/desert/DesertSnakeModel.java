package com.mechanical.client.wildlife.desert;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.WildAnimalRenderState;
import com.mechanical.client.wildlife.meadow.SnakeModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The desert snakes on the adder's part chain and animation (SnakeModel), each with its own build from
 * scripts/gen_desert_models.py (DesertGeometry): the Rattlesnake heavy-bodied with a broad arrow head, a brow over each
 * eye and the segmented rattle on the tail; the Sand Viper short and stout with a horn over each eye. Rattling, the
 * rattlesnake coils with its front raised in an S, the head drawn back over the coil and the tail held up in the middle,
 * the rattle a blur.
 */
public class DesertSnakeModel extends SnakeModel {
    public static final ModelLayerLocation RATTLESNAKE = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "rattlesnake"), "main");
    public static final ModelLayerLocation SAND_VIPER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "sand_viper"), "main");
    /** How far the neck is tipped up in the defensive S (radians), its length and the chain's pivot height (px, gen_desert_models.py). */
    private static final float REAR = 1.2F, NECK_LEN = 3.0F, PIVOT_Y = 22.5F;

    private final ModelPart neck, head;
    private final ModelPart[] chain = new ModelPart[7];

    public DesertSnakeModel(ModelPart root) {
        super(root);
        neck = root.getChild("neck");
        head = neck.getChild("head");
        chain[0] = neck;
        String[] names = {"seg2", "seg3", "seg4", "seg5", "seg6", "tail"};
        for (int i = 0; i < names.length; i++) chain[i + 1] = chain[i].getChild(names[i]);
    }

    public static LayerDefinition createRattlesnakeLayer() {
        return DesertGeometry.rattlesnake();
    }

    public static LayerDefinition createSandViperLayer() {
        return DesertGeometry.sandViper();
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        if (!Boolean.TRUE.equals(state.getRenderData(RATTLING)) || CarcassRenderer.isCarcass(state) || CarcassRenderer.isHung(state)) return;
        float t = state.ageInTicks;
        // the neck tipped up from its back end (which stays on the ground), the body behind it level again and coiled
        neck.y = PIVOT_Y - NECK_LEN * Mth.sin(REAR);
        neck.xRot = -REAR;
        neck.yRot = 0.0F;
        chain[1].xRot = REAR; // level again; the coil turns only behind it (a turn here would tilt the coil)
        chain[1].yRot = 0.0F;
        for (int i = 2; i < 6; i++) {
            chain[i].xRot = 0.0F;
            chain[i].yRot = 1.25F;
        }
        // the tail end raised up out of the coil, buzzing (+xRot lifts the segments, which run back along +z)
        chain[5].xRot = 0.45F;
        chain[6].xRot = 1.0F;
        chain[6].yRot = Mth.sin(t * 3.1F) * 0.3F;
        // the head drawn back, level, watching
        head.xRot = REAR - 0.2F + Mth.sin(t * 0.3F) * 0.05F;
        head.yRot = Mth.clamp(state.yRot, -35.0F, 35.0F) * Mth.DEG_TO_RAD;
        head.z = 0.5F;
    }
}
