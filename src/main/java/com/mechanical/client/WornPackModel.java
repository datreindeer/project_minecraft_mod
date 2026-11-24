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
 * The Steel-Frame Pack worn in the Back slot: a big rucksack on a steel frame, a bedroll under it, shoulder straps and a hip
 * belt. Parts of the body, so it follows crouching; pushed out over chest armour and a cloak. Textured by
 * scripts/gen_equipment_textures.py (entity/worn/steel_frame_pack.png, 64x64).
 */
public class WornPackModel extends HumanoidModel<AvatarRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "worn_pack"), "main");
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/worn/steel_frame_pack.png");

    private final ModelPart front, back;

    public WornPackModel(ModelPart root) {
        super(root);
        this.front = body.getChild("front");
        this.back = body.getChild("back");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        // chest: two shoulder straps with their pads over the shoulders, and the hip belt
        CubeListBuilder front = CubeListBuilder.create()
                .texOffs(0, 24).addBox(-3.5F, 0.0F, -2.75F, 2, 6, 1, new CubeDeformation(0.0F, 0.0F, -0.125F))
                .texOffs(0, 24).addBox(1.5F, 0.0F, -2.75F, 2, 6, 1, new CubeDeformation(0.0F, 0.0F, -0.125F))
                .texOffs(0, 32).addBox(-4.0F, 10.0F, -2.0F, 8, 1, 4, new CubeDeformation(0.25F))
                .texOffs(8, 24).addBox(-3.5F, -0.75F, -2.25F, 2, 1, 4, new CubeDeformation(0.0F, 0.0F, 0.25F))
                .texOffs(8, 24).addBox(1.5F, -0.75F, -2.25F, 2, 1, 4, new CubeDeformation(0.0F, 0.0F, 0.25F));
        body.addOrReplaceChild("front", front, PartPose.ZERO);

        // back: steel frame (rails, top and bottom bar), the big bag with its flap, a bedroll slung under it
        CubeListBuilder back = CubeListBuilder.create()
                .texOffs(28, 8).addBox(-5.0F, -2.0F, 2.0F, 1, 14, 1)
                .texOffs(28, 8).addBox(4.0F, -2.0F, 2.0F, 1, 14, 1)
                .texOffs(28, 8).addBox(-5.0F, -2.0F, 2.0F, 10, 1, 1)
                .texOffs(28, 8).addBox(-5.0F, 11.0F, 2.0F, 10, 1, 1)
                .texOffs(0, 0).addBox(-4.5F, -1.0F, 3.0F, 9, 11, 5)
                .texOffs(0, 17).addBox(-4.5F, -1.5F, 3.0F, 9, 2, 5, new CubeDeformation(0.25F))
                .texOffs(28, 0).addBox(-5.0F, 10.0F, 3.0F, 10, 3, 4);
        body.addOrReplaceChild("back", back, PartPose.ZERO);

        for (String limb : new String[] {"right_arm", "left_arm", "right_leg", "left_leg"}) {
            root.addOrReplaceChild(limb, CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Over chest armour the straps sit on it; a cloak pushes the whole pack out behind it. */
    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        float armor = EquipmentClient.chestArmored(state) ? 1.0F : 0.0F;
        front.z = -armor;
        back.z = armor + (EquipmentClient.cloak(state) != null ? 1.0F : 0.0F);
    }
}
