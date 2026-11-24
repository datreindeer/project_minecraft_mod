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
 * Wagon (WagonEntity): a long plank bed (24 x 48 px) with side and end boards and six stakes, a fixed rear axle and a
 * front axle on a turntable that swings with the pole; four six-spoked wheels with iron tyres; the pole reaches forward
 * (-z) and up to a yoke bar across the pair of horses. Model space: y down, ground at y = 0, px units. Texture 256 x 128,
 * painted by scripts/gen_wagon_textures.py with the same UV boxes (its BOXES table).
 */
public class WagonModel extends Model<WagonModel.Pose> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "wagon"), "main");
    /** Wheels: radius 8 px (WagonEntity.WHEEL_RADIUS), axle height, axles' z, hubs' x. */
    private static final float AXLE_Y = -8, FRONT_Z = -16, REAR_Z = 16, WHEEL_X = 16;
    private static final int RIM_SEGMENTS = 12, SPOKES = 3;
    /** Pole: 40 px from the front axle, tilted up 14 degrees to the horses' collar height at the yoke. */
    private static final float POLE_TILT = -0.245F;
    private final ModelPart front, pole, frontLeft, frontRight, rearLeft, rearRight, backBoard;

    /**
     * Wheel spin (radians, about x), the front axle's swing (radians, about y), the pole raised (radians, + up) from its
     * rest tilt to reach the horses, and the back board off (a log on the bed).
     */
    public record Pose(float spin, float steer, float pole, boolean open) {}

    public WagonModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        front = root.getChild("front");
        pole = front.getChild("pole");
        frontLeft = front.getChild("left_wheel");
        frontRight = front.getChild("right_wheel");
        rearLeft = root.getChild("left_wheel");
        rearRight = root.getChild("right_wheel");
        backBoard = root.getChild("back_board");
    }

    @Override
    public void setupAnim(Pose pose) {
        super.setupAnim(pose);
        front.yRot = pose.steer();
        pole.xRot = POLE_TILT - pose.pole();
        backBoard.visible = !pose.open();
        frontLeft.xRot = frontRight.xRot = rearLeft.xRot = rearRight.xRot = pose.spin();
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("floor", CubeListBuilder.create().texOffs(0, 0).addBox(-12, -14, -24, 24, 2, 48), PartPose.ZERO);
        root.addOrReplaceChild("left_board", CubeListBuilder.create().texOffs(0, 50).addBox(12, -20, -24, 2, 8, 48), PartPose.ZERO);
        root.addOrReplaceChild("right_board", CubeListBuilder.create().texOffs(0, 50).mirror().addBox(-14, -20, -24, 2, 8, 48), PartPose.ZERO);
        root.addOrReplaceChild("front_board", CubeListBuilder.create().texOffs(100, 50).addBox(-12, -19, -24, 24, 5, 2), PartPose.ZERO);
        root.addOrReplaceChild("back_board", CubeListBuilder.create().texOffs(100, 50).addBox(-12, -19, 22, 24, 5, 2), PartPose.ZERO);
        CubeListBuilder stake = CubeListBuilder.create().texOffs(228, 0).addBox(-1, -23, -1, 2, 10, 2);
        for (int i = 0; i < 6; i++) {
            float x = i % 2 == 0 ? 13 : -13, z = (i / 2 - 1) * 23;
            root.addOrReplaceChild("stake" + i, stake, PartPose.offset(x, 0, z));
        }
        root.addOrReplaceChild("axle", CubeListBuilder.create().texOffs(100, 60).addBox(-17, -1, -1, 34, 2, 2), PartPose.offset(0, AXLE_Y, REAR_Z));
        wheel(root, "left_wheel", WHEEL_X, AXLE_Y, REAR_Z);
        wheel(root, "right_wheel", -WHEEL_X, AXLE_Y, REAR_Z);

        // the front axle turns about its middle, the pole fixed to it
        PartDefinition front = root.addOrReplaceChild("front", CubeListBuilder.create().texOffs(100, 60).addBox(-17, -1, -1, 34, 2, 2)
                .texOffs(100, 66).addBox(-10, -4, -1.5F, 20, 3, 3), PartPose.offset(0, AXLE_Y, FRONT_Z));
        PartDefinition pole = front.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(144, 0).addBox(-1, -1, -40, 2, 2, 40),
                PartPose.rotation(POLE_TILT, 0, 0));
        pole.addOrReplaceChild("yoke", CubeListBuilder.create().texOffs(100, 72).addBox(-14, -1, -1, 28, 2, 2), PartPose.offset(0, -1.5F, -38));
        wheel(front, "left_wheel", WHEEL_X, 0, 0);
        wheel(front, "right_wheel", -WHEEL_X, 0, 0);
        return LayerDefinition.create(mesh, 256, 128);
    }

    /** Iron-capped hub, three spokes through it (six arms), twelve wooden felloes, an iron tyre ring around them. */
    private static void wheel(PartDefinition parent, String name, float x, float y, float z) {
        PartDefinition wheel = parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(172, 50).addBox(-2, -2, -2, 4, 4, 4),
                PartPose.offset(x, y, z));
        CubeListBuilder spoke = CubeListBuilder.create().texOffs(188, 50).addBox(-0.5F, -7, -0.5F, 1, 14, 1);
        for (int i = 0; i < SPOKES; i++) wheel.addOrReplaceChild("spoke" + i, spoke, PartPose.rotation(i * Mth.PI / SPOKES, 0, 0));
        CubeListBuilder felloe = CubeListBuilder.create().texOffs(192, 50).addBox(-1, -7.5F, -2, 2, 2, 4);
        CubeListBuilder tyre = CubeListBuilder.create().texOffs(208, 50).addBox(-1.25F, -8.5F, -2.25F, 2.5F, 1, 4.5F);
        for (int i = 0; i < RIM_SEGMENTS; i++) {
            PartPose turn = PartPose.rotation(i * Mth.TWO_PI / RIM_SEGMENTS, 0, 0);
            wheel.addOrReplaceChild("rim" + i, felloe, turn);
            wheel.addOrReplaceChild("tyre" + i, tyre, turn);
        }
    }
}
