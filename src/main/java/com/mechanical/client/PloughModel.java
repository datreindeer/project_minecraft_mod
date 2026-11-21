package com.mechanical.client;

import com.mechanical.Mechanical;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Mouldboard Plough (PloughEntity): a hewn beam (30 px) resting at the front on a small fork-mounted wheel, a swingletree
 * on an iron clevis ahead of it for the horse's traces; under the beam's rear an iron coulter, the wooden standard, the
 * iron share and landside, and a two-plate twisted mouldboard on the right; two handles rising back from the standard
 * with a rung between them. Model space: y down, ground at y = 0, px units, front -z. Texture 128 x 64, painted by
 * scripts/gen_plough_textures.py with the same UV boxes (its BOXES table).
 */
public class PloughModel extends Model<Float> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "mouldboard_plough"), "main");
    /** The wheel: radius 5 px (PloughEntity.WHEEL_RADIUS), its axle height and z (the renderer tilts the plough about it). */
    public static final float AXLE_Y = -5, AXLE_Z = -22;
    private static final int RIM_SEGMENTS = 8;
    /** Handles: rise 34 degrees towards the back, splayed 8 degrees apart. */
    private static final float HANDLE_RISE = 0.6F, HANDLE_SPLAY = 0.14F;
    private final ModelPart wheel;

    public PloughModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        wheel = root.getChild("wheel");
    }

    /** {@code spin}: the wheel's angle (radians). */
    @Override
    public void setupAnim(Float spin) {
        super.setupAnim(spin);
        wheel.xRot = spin;
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("beam", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.5F, -26, 3, 3, 30), PartPose.offset(0, -13, 0));
        root.addOrReplaceChild("clevis", CubeListBuilder.create().texOffs(62, 33).addBox(-1, -1, -2, 2, 2, 2), PartPose.offset(0, -13, -26));
        root.addOrReplaceChild("swingletree", CubeListBuilder.create().texOffs(62, 47).addBox(-7, -1, -1, 14, 2, 2), PartPose.offset(0, -13, -29));
        // under the beam's rear: coulter ahead, then the standard on the share, landside left, mouldboard right
        root.addOrReplaceChild("coulter", CubeListBuilder.create().texOffs(56, 33).addBox(-0.5F, 0, -1, 1, 12, 2), PartPose.offset(0, -12, -8));
        root.addOrReplaceChild("standard", CubeListBuilder.create().texOffs(116, 0).addBox(-1, -11.5F, -1.5F, 2, 9.5F, 3), PartPose.ZERO);
        root.addOrReplaceChild("share", CubeListBuilder.create().texOffs(66, 0).addBox(-2, -2, -5, 4, 1.5F, 7)
                .texOffs(88, 0).addBox(-1, -1.5F, -7, 2, 1, 2), PartPose.ZERO);
        root.addOrReplaceChild("landside", CubeListBuilder.create().texOffs(66, 9).addBox(-2.5F, -3, -4, 1, 3, 9), PartPose.ZERO);
        root.addOrReplaceChild("mouldboard_low", CubeListBuilder.create().texOffs(86, 9).addBox(0, -3, 0, 1, 3, 7),
                PartPose.offsetAndRotation(2, -0.5F, -3, 0, 0.45F, 0));
        root.addOrReplaceChild("mouldboard_high", CubeListBuilder.create().texOffs(102, 9).addBox(0, -3, 0, 1, 3, 6),
                PartPose.offsetAndRotation(2.5F, -3.5F, -1, 0, 0.7F, 0.35F));
        CubeListBuilder handle = CubeListBuilder.create().texOffs(0, 33).addBox(-1, -1, 0, 2, 2, 26);
        root.addOrReplaceChild("left_handle", handle, PartPose.offsetAndRotation(2, -6, 2, HANDLE_RISE, HANDLE_SPLAY, 0));
        root.addOrReplaceChild("right_handle", handle, PartPose.offsetAndRotation(-2, -6, 2, HANDLE_RISE, -HANDLE_SPLAY, 0));
        // 18 px up the handles
        float up = 18 * Mth.sin(HANDLE_RISE), back = 18 * Mth.cos(HANDLE_RISE);
        root.addOrReplaceChild("rung", CubeListBuilder.create().texOffs(62, 43).addBox(-4, -0.5F, -0.5F, 8, 1, 1),
                PartPose.offset(0, -6 - up, 2 + back * Mth.cos(HANDLE_SPLAY)));
        // fore-carriage: a fork down from the beam to the axle, the wheel spinning between its arms
        CubeListBuilder fork = CubeListBuilder.create().texOffs(70, 33).addBox(-0.5F, -8, -0.5F, 1, 8, 1);
        root.addOrReplaceChild("left_fork", fork, PartPose.offset(2, AXLE_Y, AXLE_Z));
        root.addOrReplaceChild("right_fork", fork, PartPose.offset(-2, AXLE_Y, AXLE_Z));
        root.addOrReplaceChild("axle", CubeListBuilder.create().texOffs(74, 33).addBox(-3, -0.5F, -0.5F, 6, 1, 1), PartPose.offset(0, AXLE_Y, AXLE_Z));
        PartDefinition wheel = root.addOrReplaceChild("wheel", CubeListBuilder.create().texOffs(88, 33).addBox(-1, -1.5F, -1.5F, 2, 3, 3),
                PartPose.offset(0, AXLE_Y, AXLE_Z));
        CubeListBuilder spoke = CubeListBuilder.create().texOffs(98, 33).addBox(-0.5F, -4.5F, -0.5F, 1, 9, 1);
        for (int i = 0; i < 2; i++) wheel.addOrReplaceChild("spoke" + i, spoke, PartPose.rotation(i * Mth.HALF_PI, 0, 0));
        CubeListBuilder rim = CubeListBuilder.create().texOffs(102, 33).addBox(-1, -5, -2.1F, 2, 1, 4.2F);
        for (int i = 0; i < RIM_SEGMENTS; i++) wheel.addOrReplaceChild("rim" + i, rim, PartPose.rotation(i * Mth.TWO_PI / RIM_SEGMENTS, 0, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }
}
