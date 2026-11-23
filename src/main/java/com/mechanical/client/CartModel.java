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
 * Wheeled Cart: a plank bed with side boards, end boards and corner stakes, on an axle with two six-spoked wheels (a
 * bronze-capped hub, twelve felloe segments), and two shafts reaching forward (-z) and a little up towards whoever pulls
 * it. Model space: y down, ground at y = 0, px units. The state is the wheels' spin in radians (they turn about x).
 * Texture 128 x 128 painted by scripts/gen_cart_textures.py with the same UV boxes (its BOXES table).
 */
public class CartModel extends Model<Float> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "cart"), "main");
    /** Wheel: radius 7 px (CartEntity.WHEEL_RADIUS), axle height, axle z, hub x. */
    private static final float AXLE_Y = -7, AXLE_Z = 2, WHEEL_X = 13;
    private static final int RIM_SEGMENTS = 12, SPOKES = 3;
    /** The axle in blocks, y up and z back: CartRenderer tips the cart about it. */
    static final float AXLE_HEIGHT = -AXLE_Y / 16.0F, AXLE_BACK = AXLE_Z / 16.0F;
    /** Shafts: from under the front of the bed, tilted up 9.5 degrees to about hand height at the tips. */
    private static final float SHAFT_TILT = -0.165F;
    private final ModelPart leftWheel, rightWheel;

    public CartModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        leftWheel = root.getChild("left_wheel");
        rightWheel = root.getChild("right_wheel");
    }

    @Override
    public void setupAnim(Float spin) {
        super.setupAnim(spin);
        leftWheel.xRot = spin;
        rightWheel.xRot = spin;
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("floor", CubeListBuilder.create().texOffs(0, 0).addBox(-10, -11, -14, 20, 2, 28), PartPose.ZERO);
        root.addOrReplaceChild("left_board", CubeListBuilder.create().texOffs(0, 30).addBox(10, -16, -14, 2, 7, 28), PartPose.ZERO);
        root.addOrReplaceChild("right_board", CubeListBuilder.create().texOffs(0, 30).mirror().addBox(-12, -16, -14, 2, 7, 28), PartPose.ZERO);
        root.addOrReplaceChild("front_board", CubeListBuilder.create().texOffs(0, 66).addBox(-10, -15, -14, 20, 4, 2), PartPose.ZERO);
        root.addOrReplaceChild("back_board", CubeListBuilder.create().texOffs(0, 66).addBox(-10, -15, 12, 20, 4, 2), PartPose.ZERO);
        CubeListBuilder stake = CubeListBuilder.create().texOffs(44, 66).addBox(-1, -19, -1, 2, 10, 2);
        for (int i = 0; i < 4; i++) {
            float x = i % 2 == 0 ? 11 : -11, z = i < 2 ? -13 : 13;
            root.addOrReplaceChild("stake" + i, stake, PartPose.offset(x, 0, z));
        }
        root.addOrReplaceChild("axle", CubeListBuilder.create().texOffs(52, 66).addBox(-14, -1, -1, 28, 2, 2), PartPose.offset(0, AXLE_Y, AXLE_Z));
        for (int side = -1; side <= 1; side += 2) {
            root.addOrReplaceChild(side > 0 ? "left_shaft" : "right_shaft", CubeListBuilder.create().texOffs(60, 34).mirror(side < 0).addBox(-1, -1, -30, 2, 2, 30),
                    PartPose.offsetAndRotation(7 * side, -8, -12, SHAFT_TILT, 0, 0));
        }
        wheel(root, "left_wheel", WHEEL_X);
        wheel(root, "right_wheel", -WHEEL_X);
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Hub on the axle end, spokes through it, a ring of felloe segments; spins about its x axis. */
    private static void wheel(PartDefinition root, String name, float x) {
        PartDefinition wheel = root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(0, 72).addBox(-2, -2, -2, 4, 4, 4),
                PartPose.offset(x, AXLE_Y, AXLE_Z));
        CubeListBuilder spoke = CubeListBuilder.create().texOffs(16, 72).addBox(-0.5F, -5, -0.5F, 1, 10, 1);
        for (int i = 0; i < SPOKES; i++) wheel.addOrReplaceChild("spoke" + i, spoke, PartPose.rotation(i * Mth.PI / SPOKES, 0, 0));
        CubeListBuilder felloe = CubeListBuilder.create().texOffs(20, 72).addBox(-1, -7, -2, 2, 2, 4);
        for (int i = 0; i < RIM_SEGMENTS; i++) wheel.addOrReplaceChild("rim" + i, felloe, PartPose.rotation(i * Mth.TWO_PI / RIM_SEGMENTS, 0, 0));
    }
}
