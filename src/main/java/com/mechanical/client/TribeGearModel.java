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
 * What a tribesperson wears that stands out from the body (people.Tribesperson): a fur hood with a thick ruff round the
 * face, a top-knot, two braids over the shoulders, a beard down the chest, a feather crest or one feather on the head; a fur
 * mantle round the shoulders, a hide cloak down the back and a big necklace standing off the chest on the body. Every box
 * is always there: each person's gear texture (tribesperson/.../&lt;role&gt;_&lt;skin&gt;_gear.png, scripts/gen_tribe_skins.py,
 * whose GEAR_BOXES table mirrors the boxes here) paints only what they wear and leaves the rest see-through. Follows the
 * head and body of the player model, so it walks, turns and crouches with them. Drawn by EquipmentLayer.
 */
public class TribeGearModel extends HumanoidModel<AvatarRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "tribe_gear"), "main");

    public TribeGearModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8, new CubeDeformation(0.75F))   // fur hood, open face
                .texOffs(32, 0).addBox(-5.0F, -9.0F, -4.9F, 10, 10, 1)                          // the ruff round the face
                .texOffs(0, 16).addBox(-1.5F, -11.0F, -0.5F, 3, 3, 3)                            // top-knot
                .texOffs(12, 16).addBox(-3.0F, -1.0F, -4.7F, 6, 5, 1)                            // beard
                .texOffs(26, 16).addBox(-5.0F, -4.0F, -2.0F, 1, 9, 1)                            // braids
                .texOffs(30, 16).addBox(4.0F, -4.0F, -2.0F, 1, 9, 1)
                .texOffs(34, 16).addBox(-2.5F, -13.0F, 3.0F, 1, 6, 1)                            // feather crest
                .texOffs(38, 16).addBox(-0.5F, -15.0F, 3.2F, 1, 8, 1)
                .texOffs(42, 16).addBox(1.5F, -13.0F, 3.0F, 1, 6, 1)
                .texOffs(46, 16).addBox(3.6F, -12.0F, 1.0F, 1, 5, 1),                            // one feather
                PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-6.0F, -0.5F, -3.0F, 12, 4, 6, new CubeDeformation(0.25F)) // fur mantle
                .texOffs(36, 32).addBox(-4.5F, 0.5F, 2.3F, 9, 13, 1)                              // hide cloak
                .texOffs(0, 48).addBox(-4.0F, 0.0F, -2.6F, 8, 5, 1),                              // necklace
                PartPose.ZERO);
        for (String limb : new String[] {"right_arm", "left_arm", "right_leg", "left_leg"}) {
            root.addOrReplaceChild(limb, CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }
}
