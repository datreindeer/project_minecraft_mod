package com.mechanical.client;

import com.mechanical.Mechanical;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;

/**
 * The Miner's ore sack (people.Miners): the Leather Satchel grown big and slung on the back, wider than the shoulders and
 * up past them, lumps of ore heaped in its open mouth, held by two straps over the shoulders. Part of the body, so it
 * leans with his hunched (sneaking) pose. Textured by scripts/gen_equipment_textures.py (entity/worn/miner_sack.png, 64x32).
 */
public class MinerSackModel extends HumanoidModel<AvatarRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "miner_sack"), "main");
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/worn/miner_sack.png");

    public MinerSackModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        CubeDeformation thin = new CubeDeformation(0.0F, 0.0F, -0.3F);
        body.addOrReplaceChild("sack", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.5F, -2.0F, 2.0F, 9, 11, 6)
                // ore heaped in the mouth
                .texOffs(0, 18).addBox(-3.0F, -4.0F, 3.0F, 3, 2, 3)
                .texOffs(14, 18).addBox(0.5F, -3.5F, 4.0F, 2, 2, 2)
                .texOffs(24, 18).addBox(-1.0F, -3.0F, 5.5F, 2, 1, 2)
                // straps: down the chest, and over each shoulder to the sack
                .texOffs(32, 0).addBox(-3.0F, 0.0F, -2.5F, 1, 7, 1, thin)
                .texOffs(32, 0).addBox(2.0F, 0.0F, -2.5F, 1, 7, 1, thin)
                .texOffs(40, 0).addBox(-3.0F, -0.5F, -2.5F, 1, 1, 5)
                .texOffs(40, 0).addBox(2.0F, -0.5F, -2.5F, 1, 1, 5), PartPose.ZERO);
        for (String limb : new String[] {"right_arm", "left_arm", "right_leg", "left_leg"}) {
            root.addOrReplaceChild(limb, CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 32);
    }
}
