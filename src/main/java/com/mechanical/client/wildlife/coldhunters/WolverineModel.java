package com.mechanical.client.wildlife.coldhunters;

import com.mechanical.client.CarcassRenderer;
import com.mechanical.client.wildlife.PredatorModel;
import com.mechanical.client.wildlife.WildAnimalRenderState;

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
 * Wolverine: a small bear of a weasel - heavy, low and humped (the back rises to the rump), short thick legs on broad
 * clawed paws, a broad head with a short muzzle and small round ears, and a long bushy tail. Animation in
 * {@link PredatorModel}, plus the rolling lope of a mustelid (the body rocks). Texture 64x64:
 * textures/entity/wildlife/wolverine.png, painted by scripts/gen_fauna_coldhunters_textures.py (WOLVERINE table).
 */
public class WolverineModel extends PredatorModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "wolverine"), "main");

    public WolverineModel(ModelPart root) {
        super(root, 0.9F, 2.5F, 4.5F, 1.0F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -3.5F, -6.0F, 7.0F, 7.0F, 7.0F)
                        .texOffs(0, 14).addBox(-3.5F, -4.0F, 1.0F, 7.0F, 7.0F, 6.0F),
                PartPose.offset(0.0F, 15.0F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(28, 14).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -3.0F, 6.5F, 1.0F, 0.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(28, 0).addBox(-3.0F, -2.5F, -4.0F, 6.0F, 5.0F, 4.0F)
                        .texOffs(48, 0).addBox(-1.5F, -0.5F, -6.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, -0.5F, -6.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(48, 4).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 1.0F, 2.0F),
                PartPose.offset(0.0F, 1.5F, -4.0F));
        for (float x : new float[] {-2.2F, 2.2F}) {
            boolean left = x > 0;
            head.addOrReplaceChild(left ? "left_ear" : "right_ear", CubeListBuilder.create().mirror(left)
                            .texOffs(58, 0).addBox(-1.0F, -1.5F, -0.5F, 2.0F, 2.0F, 1.0F),
                    PartPose.offsetAndRotation(x * 1.05F, -2.3F, -1.5F, 0.0F, 0.0F, left ? 0.3F : -0.3F));
            PartDefinition front = root.addOrReplaceChild(left ? "left_front_leg" : "right_front_leg",
                    CubeListBuilder.create().mirror(left).texOffs(0, 28).addBox(-1.5F, -1.0F, -1.5F, 3.0F, 4.0F, 3.0F),
                    PartPose.offset(x, 17.0F, -3.5F));
            front.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 3.0F, 0.0F));
            PartDefinition hind = root.addOrReplaceChild(left ? "left_hind_leg" : "right_hind_leg",
                    CubeListBuilder.create().mirror(left).texOffs(38, 28).addBox(-2.0F, -1.0F, -2.0F, 4.0F, 5.0F, 4.0F),
                    PartPose.offset(x, 16.0F, 5.0F));
            hind.addOrReplaceChild("lower", lowerLeg(left), PartPose.offset(0.0F, 4.0F, 0.5F));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Short thick shank and the broad paw with its pale claws. */
    private static CubeListBuilder lowerLeg(boolean mirror) {
        return CubeListBuilder.create().mirror(mirror)
                .texOffs(12, 28).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F)
                .texOffs(24, 28).addBox(-1.5F, 3.0F, -2.5F, 3.0F, 1.0F, 4.0F);
    }

    @Override
    public void setupAnim(WildAnimalRenderState state) {
        super.setupAnim(state);
        if (!state.dozing && !CarcassRenderer.isCarcass(state) && !CarcassRenderer.isHung(state))
            body.zRot += Mth.sin(state.walkAnimationPos * 0.9F) * 0.06F * Math.min(state.walkAnimationSpeed, 1.0F);
    }
}
