package com.mechanical.client;

import com.mechanical.Mechanical;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.boat.AbstractBoatModel;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.resources.Identifier;

/**
 * Log Raft and Dugout Canoe. Model space as vanilla boats: x along the craft (+x the bow), z across, y down; y = 6 is the
 * craft's bottom, y = -3 its top (0.5625 above the entity). The texture layouts are painted by scripts/gen_watercraft_textures.py,
 * which repeats these boxes, so keep the two in step.
 */
public class WatercraftModel extends AbstractBoatModel {
    public static final ModelLayerLocation RAFT = layer("log_raft", "main");
    public static final ModelLayerLocation CANOE = layer("dugout_canoe", "main");
    public static final ModelLayerLocation CANOE_WATER_PATCH = layer("dugout_canoe", "water_patch");
    private final ModelPart leftPaddle, rightPaddle;

    public WatercraftModel(ModelPart root) {
        super(root);
        leftPaddle = root.getChild("left_paddle");
        rightPaddle = root.getChild("right_paddle");
    }

    private static ModelLayerLocation layer(String name, String part) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, name), part);
    }

    @Override
    public void setupAnim(BoatRenderState state) {
        super.setupAnim(state);
        boolean paddles = !(state instanceof WatercraftRenderer.State s) || s.paddles;
        leftPaddle.visible = paddles;
        rightPaddle.visible = paddles;
    }

    /** Vanilla's paddle movement, on a single-bladed paddle; z is where it rests on the side. */
    private static void paddles(PartDefinition root, int u, int v, float y, float z) {
        root.addOrReplaceChild("left_paddle", CubeListBuilder.create()
                        .texOffs(u, v).addBox(-1.0F, 0.0F, -5.0F, 2.0F, 2.0F, 18.0F)
                        .texOffs(u + 40, v).addBox(-1.001F, -3.0F, 8.0F, 1.0F, 6.0F, 7.0F),
                PartPose.offsetAndRotation(3.0F, y, z, 0.0F, 0.0F, (float) (Math.PI / 16)));
        root.addOrReplaceChild("right_paddle", CubeListBuilder.create()
                        .texOffs(u + 56, v).addBox(-1.0F, 0.0F, -5.0F, 2.0F, 2.0F, 18.0F)
                        .texOffs(u + 96, v).addBox(0.001F, -3.0F, 8.0F, 1.0F, 6.0F, 7.0F),
                PartPose.offsetAndRotation(3.0F, y, -z, 0.0F, (float) Math.PI, (float) (Math.PI / 16)));
    }

    /** Five bark-on logs side by side, two cross poles lashed on top. 128 x 96. */
    public static LayerDefinition createRaft() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder logs = CubeListBuilder.create();
        for (int i = 0; i < 5; i++) {
            // slightly uneven logs: alternate ones stick out a little and sit a little lower
            float len = i % 2 == 0 ? 30.0F : 28.0F;
            logs.texOffs(0, 10 * i).addBox(-len / 2, i % 2 == 0 ? -3.0F : -2.5F, -12.5F + 5 * i, len, 5.0F, 5.0F);
        }
        root.addOrReplaceChild("logs", logs, PartPose.ZERO);
        root.addOrReplaceChild("poles", CubeListBuilder.create()
                .texOffs(72, 0).addBox(-10.0F, -5.0F, -13.0F, 2.0F, 2.0F, 26.0F)
                .texOffs(72, 28).addBox(8.0F, -5.0F, -13.0F, 2.0F, 2.0F, 26.0F), PartPose.ZERO);
        paddles(root, 0, 56, -7.0F, 11.0F);
        return LayerDefinition.create(mesh, 128, 96);
    }

    /** A hollowed log: rounded floor, two walls, solid tapering ends. 128 x 64. */
    public static LayerDefinition createCanoe() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("hull", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-15.0F, 4.0F, -5.0F, 30.0F, 2.0F, 10.0F)   // floor
                .texOffs(0, 12).addBox(-15.0F, -3.0F, 5.0F, 30.0F, 8.0F, 2.0F)   // left wall
                .texOffs(64, 12).addBox(-15.0F, -3.0F, -7.0F, 30.0F, 8.0F, 2.0F) // right wall
                .texOffs(0, 22).addBox(15.0F, -3.0F, -6.0F, 3.0F, 8.0F, 12.0F)   // bow, solid end of the log
                .texOffs(30, 22).addBox(18.0F, -3.0F, -3.0F, 2.0F, 7.0F, 6.0F)
                .texOffs(46, 22).addBox(20.0F, -3.0F, -1.0F, 1.0F, 5.0F, 2.0F)
                .texOffs(52, 22).addBox(-18.0F, -3.0F, -6.0F, 3.0F, 8.0F, 12.0F) // stern
                .texOffs(82, 22).addBox(-20.0F, -3.0F, -3.0F, 2.0F, 7.0F, 6.0F)
                .texOffs(98, 22).addBox(-21.0F, -3.0F, -1.0F, 1.0F, 5.0F, 2.0F), PartPose.ZERO);
        paddles(root, 0, 42, -5.0F, 7.0F);
        return LayerDefinition.create(mesh, 128, 64);
    }

    /** Hides the water surface inside the canoe's hull (as vanilla's boat water patch). */
    public static LayerDefinition createCanoeWaterPatch() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("water_patch", CubeListBuilder.create().addBox(-15.0F, -3.0F, -5.0F, 30.0F, 3.0F, 10.0F), PartPose.ZERO);
        return LayerDefinition.create(mesh, 0, 0);
    }
}
