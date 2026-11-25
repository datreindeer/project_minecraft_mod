package com.mechanical.client;

import com.mechanical.Mechanical;
import com.mechanical.equipment.Quiver;

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
 * The Quiver worn in a Hip slot: a hide tube hanging from a waist belt behind the right hip (behind the left, mirrored, from
 * the second Hip slot), tilted so the fletchings
 * come up towards the right side; one arrow shows for each quiver slot holding arrows. Parts of the body, so it follows
 * crouching; moved out over chest armour and behind a cloak. Textured by scripts/gen_equipment_textures.py
 * (entity/worn/quiver.png, 32x32).
 */
public class WornQuiverModel extends HumanoidModel<AvatarRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "worn_quiver"), "main");
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/worn/quiver.png");
    private static final float QUIVER_X = -2.5F, QUIVER_Z = 2.0F, QUIVER_Z_ROT = 0.45F;

    private final ModelPart quiver, belt, beltArmored;
    private final ModelPart[] arrows = new ModelPart[Quiver.SIZE];

    public WornQuiverModel(ModelPart root) {
        super(root);
        this.quiver = body.getChild("quiver");
        this.belt = body.getChild("belt");
        this.beltArmored = body.getChild("belt_armored");
        for (int i = 0; i < Quiver.SIZE; i++) arrows[i] = quiver.getChild("arrow" + i);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        // the waist belt (over leggings, or over chest armour)
        body.addOrReplaceChild("belt", CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, 10.0F, -2.0F, 8, 1, 4, new CubeDeformation(0.6F)), PartPose.ZERO);
        body.addOrReplaceChild("belt_armored", CubeListBuilder.create().texOffs(0, 20).addBox(-4.0F, 10.0F, -2.0F, 8, 1, 4, new CubeDeformation(1.1F)), PartPose.ZERO);

        // the tube hangs from the belt behind the right hip (x < 0 is the right side), top tilted out to the right
        PartDefinition quiver = body.addOrReplaceChild("quiver", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.5F, -4.0F, 0.0F, 3, 9, 3)
                .texOffs(0, 14).addBox(-2.0F, -4.5F, -0.5F, 4, 1, 4), PartPose.offsetAndRotation(QUIVER_X, 7.0F, QUIVER_Z, 0.12F, 0.0F, QUIVER_Z_ROT));
        float[][] spots = {{-0.75F, 0.6F}, {0.6F, 0.9F}, {-0.2F, 2.1F}, {0.8F, 2.2F}};
        for (int i = 0; i < Quiver.SIZE; i++) {
            quiver.addOrReplaceChild("arrow" + i, CubeListBuilder.create().texOffs(24, 0).addBox(-0.5F, -4.0F, -0.5F, 1, 4, 1),
                    PartPose.offsetAndRotation(spots[i][0], -4.0F + (i % 2) * 0.5F, spots[i][1], (i - 1.5F) * 0.06F, 0.0F, (i % 2 == 0 ? -1 : 1) * 0.08F));
        }

        for (String limb : new String[] {"right_arm", "left_arm", "right_leg", "left_leg"}) {
            root.addOrReplaceChild(limb, CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    /**
     * One arrow per filled quiver slot; moved out over chest armour and behind a cloak's cape; mirrored to the left hip
     * when worn in the second Hip slot (render data from EquipmentClient).
     */
    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        int filled = EquipmentClient.quiverArrows(state);
        boolean armored = EquipmentClient.chestArmored(state), cloak = EquipmentClient.cloak(state) != null;
        quiver.z = QUIVER_Z + (armored ? 1.0F : 0.0F) + (cloak ? 1.25F : 0.0F);
        float side = EquipmentClient.quiverLeft(state) ? -1.0F : 1.0F;
        quiver.x = QUIVER_X * side;
        quiver.zRot = QUIVER_Z_ROT * side;
        for (ModelPart arrow : arrows) { // the arrows mirrored inside the tube too
            arrow.x = arrow.getInitialPose().x() * side;
            arrow.zRot = arrow.getInitialPose().zRot() * side;
        }
        belt.visible = !armored;
        beltArmored.visible = armored;
        for (int i = 0; i < arrows.length; i++) arrows[i].visible = i < filled;
    }
}
