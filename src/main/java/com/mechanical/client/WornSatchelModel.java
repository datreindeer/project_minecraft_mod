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
 * The Leather Satchel worn in the Back slot: a shoulder bag at the front of the left hip, its strap from the bag up across
 * the chest, over the right shoulder and down the back. Parts of the body, so it follows crouching; pushed out a pixel
 * over chest armour, and the back strap is left out under a cloak. Textured by scripts/gen_equipment_textures.py
 * (entity/worn/satchel.png, 32x32).
 */
public class WornSatchelModel extends HumanoidModel<AvatarRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "worn_satchel"), "main");
    public static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/worn/satchel.png");
    /** Strap steps across the chest (right shoulder to the bag) and down the back (shoulder to the left hip). */
    private static final int FRONT_STEPS = 8, BACK_STEPS = 10;

    private final ModelPart front, back, shoulder;

    public WornSatchelModel(ModelPart root) {
        super(root);
        this.front = body.getChild("front");
        this.back = body.getChild("back");
        this.shoulder = body.getChild("shoulder");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        // in front of the belly: the bag (x 0.5..4.5 is the left half), its flap over the top, the strap up to the right shoulder
        CubeListBuilder front = CubeListBuilder.create()
                .texOffs(0, 0).addBox(0.5F, 8.0F, -4.0F, 4, 4, 2)
                .texOffs(0, 8).addBox(0.5F, 7.5F, -4.5F, 4, 2, 1, new CubeDeformation(0.25F));
        for (int i = 0; i < FRONT_STEPS; i++) {
            float t = i / (float) (FRONT_STEPS - 1);
            front.texOffs(0, 12).addBox(-4.0F + t * 5.0F, i, -2.75F, 2, 1, 1, new CubeDeformation(-0.125F, 0.0F, -0.125F));
        }
        body.addOrReplaceChild("front", front, PartPose.ZERO);

        CubeListBuilder back = CubeListBuilder.create();
        for (int i = 0; i < BACK_STEPS; i++) {
            float t = i / (float) (BACK_STEPS - 1);
            back.texOffs(0, 12).addBox(-4.0F + t * 6.5F, i, 1.75F, 2, 1, 1, new CubeDeformation(-0.125F, 0.0F, -0.125F));
        }
        body.addOrReplaceChild("back", back, PartPose.ZERO);
        body.addOrReplaceChild("shoulder", CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -0.75F, -2.25F, 2, 1, 4, new CubeDeformation(0.0F, 0.0F, 0.25F)),
                PartPose.ZERO);

        for (String limb : new String[] {"right_arm", "left_arm", "right_leg", "left_leg"}) {
            root.addOrReplaceChild(limb, CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    /** Over chest armour the satchel sits on it; a cloak hides the back strap (render data from EquipmentClient). */
    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        boolean armored = EquipmentClient.chestArmored(state), cloak = EquipmentClient.cloak(state) != null;
        float out = armored ? 1.0F : 0.0F;
        front.z = -out;
        back.z = out;
        back.visible = !cloak;
        shoulder.y = -out;
        shoulder.zScale = armored ? 1.5F : 1.0F;
    }
}
