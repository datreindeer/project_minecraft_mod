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
 * The Leather Pouch worn in the Hip slot: a small drawstring bag hanging from the waist belt at the front of the right
 * hip (the satchel hangs on the left). Parts of the body, so it follows crouching; moved out over chest armour. Textured
 * by scripts/gen_equipment_textures.py (entity/worn/leather_pouch.png, 32x32; belt as the quiver's).
 */
public class WornPouchModel extends HumanoidModel<AvatarRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "worn_pouch"), "main");
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/worn/leather_pouch.png");
    private static final float POUCH_Z = -2.6F;

    private final ModelPart pouch, belt, beltArmored;

    public WornPouchModel(ModelPart root) {
        super(root);
        this.pouch = body.getChild("pouch");
        this.belt = body.getChild("belt");
        this.beltArmored = body.getChild("belt_armored");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, 10.0F, -2.0F, 8, 1, 4, new CubeDeformation(0.6F)), PartPose.ZERO);
        body.addOrReplaceChild("belt_armored", CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, 10.0F, -2.0F, 8, 1, 4, new CubeDeformation(1.1F)), PartPose.ZERO);

        // the bag (3x3x2) under its gathered, tied neck (2x1x1), hanging from the belt (x < 0 is the right side)
        body.addOrReplaceChild("pouch", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, 1.0F, -1.0F, 3, 3, 2)
                .texOffs(0, 6).addBox(-1.0F, 0.0F, -0.5F, 2, 1, 1), PartPose.offsetAndRotation(-2.3F, 10.4F, POUCH_Z, -0.08F, 0.0F, 0.06F));

        for (String limb : new String[] {"right_arm", "left_arm", "right_leg", "left_leg"}) {
            root.addOrReplaceChild(limb, CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        boolean armored = EquipmentClient.chestArmored(state);
        pouch.z = POUCH_Z - (armored ? 1.0F : 0.0F);
        belt.visible = !armored;
        beltArmored.visible = armored;
        if (HoldPoses.rummagingWorn(state)) { // lifted off the belt towards the hands, its neck tipped open
            pouch.z -= 2.0F;
            pouch.y -= 1.5F;
            pouch.xRot = -0.5F;
        }
    }
}
