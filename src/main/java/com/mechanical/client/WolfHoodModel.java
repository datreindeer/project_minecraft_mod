package com.mechanical.client;

import com.mechanical.Mechanical;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * The Wolf Hood as worn: a hunter's hood made from a wolf's head. The hood over the head (face left open), the upper jaw
 * with its fangs jutting over the brow, the pointed ears on top, and the pelt over the shoulders and down the back to the
 * tail. Textured by scripts/gen_wolf_pelt_textures.py (equipment layer humanoid/wolf_hood), which repeats these boxes.
 * Swapped in for the plain helmet model through IClientItemExtensions; the armour layer poses it like any humanoid.
 */
public class WolfHoodModel extends HumanoidModel<HumanoidRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "wolf_hood"), "main");

    public WolfHoodModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(1.0F)) // the hood
                .texOffs(32, 0).addBox(-2, -10, -9, 4, 3, 4) // upper jaw over the brow
                .texOffs(48, 0).addBox(-1.75F, -7, -8.5F, 1, 2, 1) // fangs
                .texOffs(48, 0).mirror().addBox(0.75F, -7, -8.5F, 1, 2, 1).mirror(false)
                .texOffs(56, 0).addBox(-4, -12, -3, 2, 3, 1) // ears
                .texOffs(56, 0).mirror().addBox(2, -12, -3, 2, 3, 1), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-8, 0, -3, 16, 2, 6, new CubeDeformation(0.5F)) // over the shoulders
                .texOffs(0, 24).addBox(-5, 2.5F, 3, 10, 9, 1, new CubeDeformation(0.25F)) // down the back
                .texOffs(24, 24).addBox(-1, 11.5F, 3.25F, 2, 5, 2), PartPose.ZERO); // the tail
        for (String limb : new String[] {"right_arm", "left_arm", "right_leg", "left_leg"}) {
            root.addOrReplaceChild(limb, CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Registered for the Wolf Hood item (MechanicalClient). */
    static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
        private WolfHoodModel model;

        @Override
        public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
            if (layerType != EquipmentClientInfo.LayerType.HUMANOID) return original; // babies keep the plain hood
            if (model == null) model = new WolfHoodModel(Minecraft.getInstance().getEntityModels().bakeLayer(LAYER));
            return model;
        }
    };
}
