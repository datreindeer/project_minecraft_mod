package com.mechanical.client.wildlife.river;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Mosquito Swarm: {@link #SPECKS} tiny gnats (a third of a pixel each) dancing in a loose cloud, each on its own
 * wobbling orbit (phases spread by the golden angle, so the cloud has no visible pattern).
 * Texture: textures/entity/wildlife/mosquito_swarm.png (16x16): one dark speck with grey wings on top.
 */
public class MosquitoSwarmModel extends EntityModel<LivingEntityRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "mosquito_swarm"), "main");
    static final int SPECKS = 28;
    /** Centre of the cloud in model space (the swarm is 0.7 blocks tall; the feet are at y 24). */
    private static final float CY = 18.0F;
    private final ModelPart[] specks = new ModelPart[SPECKS];

    public MosquitoSwarmModel(ModelPart root) {
        super(root);
        for (int i = 0; i < SPECKS; i++) specks[i] = root.getChild("speck_" + i);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < SPECKS; i++) {
            root.addOrReplaceChild("speck_" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offset(0.0F, CY, 0.0F));
        }
        return LayerDefinition.create(mesh, 16, 16);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float t = state.ageInTicks;
        for (int i = 0; i < SPECKS; i++) {
            ModelPart s = specks[i];
            float a = i * 2.39996F, r = 2.0F + (i * 7 % 5) * 0.9F, w = 0.18F + (i % 7) * 0.035F;
            float ang = t * w + a;
            s.x = Mth.cos(ang) * r + Mth.sin(t * 0.31F + a * 3) * 1.2F;
            s.z = Mth.sin(ang) * r + Mth.cos(t * 0.27F + a * 2) * 1.2F;
            s.y = CY + Mth.sin(t * (0.23F + (i % 5) * 0.04F) + a * 5) * 3.5F;
            s.xScale = s.yScale = s.zScale = 0.35F;
        }
    }
}
