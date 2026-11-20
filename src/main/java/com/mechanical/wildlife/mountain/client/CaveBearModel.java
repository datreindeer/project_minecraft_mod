package com.mechanical.wildlife.mountain.client;

import com.mechanical.client.wildlife.BrownBearModel;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * Cave bear: the brown bear's build (same parts, pivots and animation - rearing included - from {@link BrownBearModel}),
 * rendered a fifth bigger (MountainClient), with the cave bear's tell-tale skull: the forehead rises in a steep, high dome
 * straight up from the muzzle instead of the brown bear's low stop. Texture 128x64: textures/entity/wildlife/cave_bear.png,
 * the brown bear's layout (scripts/gen_fauna_mountain_textures.py repaints it darker and fills the dome).
 */
public class CaveBearModel extends BrownBearModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath("mechanical", "cave_bear"), "main");
    private static final float HEAD_X_ROT = 0.12F, TAIL_X_ROT = 0.5F, HIP_Y = 12.0F, HIP_Z = 8.0F, SHOULDER_Y = 14.0F, SHOULDER_Z = -7.5F;

    public CaveBearModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation plane = new CubeDeformation(0.001F);

        // Body pivot at the hips (0, 12, 8). Forequarters: x -6..6, y 3..16, z -12..0 in model space; hump on top of the
        // withers (y 0..3); hindquarters a bit lower and narrower (y 5..16, z -1..12). Shaggy belly fringe planes hang
        // 3 px below the chest on both sides.
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.0F, -9.0F, -20.0F, 12.0F, 13.0F, 12.0F)
                        .texOffs(48, 0).addBox(-4.5F, -11.0F, -18.0F, 9.0F, 3.0F, 7.0F)
                        .texOffs(48, 10).addBox(-3.0F, -12.0F, -16.5F, 6.0F, 1.0F, 4.0F)
                        .texOffs(0, 25).addBox(-5.5F, -7.0F, -9.0F, 11.0F, 11.0F, 13.0F)
                        .texOffs(48, 25).addBox(-6.2F, 3.0F, -19.0F, 0.0F, 4.0F, 10.0F, plane)
                        .texOffs(48, 25).mirror().addBox(6.2F, 3.0F, -19.0F, 0.0F, 4.0F, 10.0F, plane),
                PartPose.offset(0.0F, HIP_Y, HIP_Z));
        body.addOrReplaceChild("tail",
                CubeListBuilder.create().texOffs(114, 20).addBox(-1.5F, -1.0F, 0.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -5.0F, 3.5F, TAIL_X_ROT, 0.0F, 0.0F));

        // Head in front of the shoulders, carried below the hump. Skull 8 wide, cheek ruffs 10 wide at the back,
        // forehead stop, muzzle 5 wide, nose pad sticking up half a pixel above the bridge (the "dish").
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(80, 0).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 7.0F, 6.0F)
                        .texOffs(48, 15).addBox(-5.0F, -2.0F, -2.5F, 10.0F, 6.0F, 3.0F)
                        .texOffs(108, 0).addBox(-3.5F, -5.0F, -7.5F, 7.0F, 4.0F, 2.0F)
                        .texOffs(80, 13).addBox(-2.5F, -1.0F, -10.0F, 5.0F, 4.0F, 4.0F)
                        .texOffs(114, 13).addBox(-1.5F, -1.5F, -11.0F, 3.0F, 2.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, -5.0F, -20.0F, HEAD_X_ROT, 0.0F, 0.0F));
        head.addOrReplaceChild("jaw",
                CubeListBuilder.create().texOffs(98, 13).addBox(-2.0F, 0.0F, -3.5F, 4.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 2.5F, -6.0F));
        // small round ears (corners transparent in the texture), 1 px sunk into the skull
        head.addOrReplaceChild("right_ear",
                CubeListBuilder.create().texOffs(114, 16).addBox(-1.5F, -2.5F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-3.0F, -3.5F, -2.0F, 0.0F, 0.0F, -0.25F));
        head.addOrReplaceChild("left_ear",
                CubeListBuilder.create().mirror().texOffs(114, 16).addBox(-1.5F, -2.5F, -0.5F, 3.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(3.0F, -3.5F, -2.0F, 0.0F, 0.0F, 0.25F));

        // Pillar legs, ground at y = 24: front leg 5x8x5 over a broad paw with a row of long claws in front;
        // hind leg a thick 6x8x7 haunch over a long flat-footed paw.
        CubeListBuilder front = CubeListBuilder.create()
                .texOffs(0, 49).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 8.0F, 5.0F)
                .texOffs(20, 49).addBox(-2.5F, 7.0F, -3.5F, 5.0F, 3.0F, 6.0F)
                .texOffs(68, 59).addBox(-2.0F, 9.0F, -4.5F, 4.0F, 1.0F, 1.0F);
        CubeListBuilder frontMirror = CubeListBuilder.create().mirror()
                .texOffs(0, 49).addBox(-2.5F, -1.0F, -2.5F, 5.0F, 8.0F, 5.0F)
                .texOffs(20, 49).addBox(-2.5F, 7.0F, -3.5F, 5.0F, 3.0F, 6.0F)
                .texOffs(68, 59).addBox(-2.0F, 9.0F, -4.5F, 4.0F, 1.0F, 1.0F);
        CubeListBuilder hind = CubeListBuilder.create()
                .texOffs(42, 49).addBox(-3.0F, -2.0F, -3.5F, 6.0F, 8.0F, 7.0F)
                .texOffs(68, 49).addBox(-2.5F, 6.0F, -3.5F, 5.0F, 4.0F, 6.0F);
        CubeListBuilder hindMirror = CubeListBuilder.create().mirror()
                .texOffs(42, 49).addBox(-3.0F, -2.0F, -3.5F, 6.0F, 8.0F, 7.0F)
                .texOffs(68, 49).addBox(-2.5F, 6.0F, -3.5F, 5.0F, 4.0F, 6.0F);
        root.addOrReplaceChild("right_front_leg", front, PartPose.offset(-3.5F, SHOULDER_Y, SHOULDER_Z));
        root.addOrReplaceChild("left_front_leg", frontMirror, PartPose.offset(3.5F, SHOULDER_Y, SHOULDER_Z));
        root.addOrReplaceChild("right_hind_leg", hind, PartPose.offset(-3.5F, 14.0F, 7.0F));
        root.addOrReplaceChild("left_hind_leg", hindMirror, PartPose.offset(3.5F, 14.0F, 7.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }
}
