package com.mechanical.client;

import com.mechanical.Mechanical;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * Travois: two long poles crossing in a V at the front (towards the one pulling it, -z), lashed where they cross, their
 * rear ends dragging on the ground; three crossbars and a stretched hide make the platform. Model space: y down, the
 * ground at y = 0, px units. The texture (128 x 64) is painted by scripts/gen_carcass_hauling_textures.py, which uses the
 * same UV boxes: poles (0,0) and (8,0), hide (16,0), crossbars (16,28), lashings (64,28).
 */
public final class TravoisModel {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "travois"), "main");
    /** Rear end of a pole (on the ground) and front end (raised to the hands), for the left pole; the right one mirrors x. */
    private static final float REAR_X = 10, REAR_Y = -1, REAR_Z = 24, FRONT_X = -1, FRONT_Y = -17, FRONT_Z = -32;
    /** The poles' height at z: a straight line from the rear end to the front end. */
    private static float poleY(float z) {
        return REAR_Y + (FRONT_Y - REAR_Y) * (REAR_Z - z) / (REAR_Z - FRONT_Z);
    }

    private static float poleX(float z) {
        return REAR_X + (FRONT_X - REAR_X) * (REAR_Z - z) / (REAR_Z - FRONT_Z);
    }

    /** A pole is a 2 x len x 2 box standing up (-y) from its rear end, turned to point at its front end. */
    private static void pole(PartDefinition root, String name, int u, float side) {
        float dx = (FRONT_X - REAR_X) * side, dy = FRONT_Y - REAR_Y, dz = FRONT_Z - REAR_Z;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= len; dy /= len; dz /= len;
        // (0,-1,0) turned by x then y: (-sin a sin b, -cos a, -sin a cos b)
        float xRot = (float) Math.atan2(Math.sqrt(dx * dx + dz * dz), -dy), yRot = (float) Math.atan2(-dx, -dz);
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, 0).addBox(-1, -len - 2, -1, 2, len + 2, 2),
                PartPose.offsetAndRotation(REAR_X * side, REAR_Y, REAR_Z, xRot, yRot, 0));
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        pole(root, "left_pole", 0, 1);
        pole(root, "right_pole", 8, -1);
        // the platform follows the poles' slope, lying on top of them
        float slope = (float) Math.atan2(FRONT_Y - REAR_Y, REAR_Z - FRONT_Z); // negative: the front end is higher
        float deckZ = 6;
        CubeListBuilder deck = CubeListBuilder.create().texOffs(16, 0).addBox(-9, -2, -13, 18, 1, 26);
        for (float z : new float[] {-12, 0, 11}) {
            float half = poleX(deckZ + z) + 2;
            deck.texOffs(16, 28).addBox(-half, -1, z - 1, half * 2, 2, 2);
            // lashings where each crossbar meets a pole
            deck.texOffs(64, 28).addBox(half - 3.5F, -1.5F, z - 1.5F, 3, 3, 3);
            deck.texOffs(64, 28).addBox(-half + 0.5F, -1.5F, z - 1.5F, 3, 3, 3);
        }
        root.addOrReplaceChild("deck", deck, PartPose.offsetAndRotation(0, poleY(deckZ) - 1, deckZ, slope, 0, 0));
        // the lashing where the poles cross
        float crossZ = REAR_Z - REAR_X * (REAR_Z - FRONT_Z) / (REAR_X - FRONT_X);
        root.addOrReplaceChild("cross_lashing", CubeListBuilder.create().texOffs(64, 28).addBox(-1.5F, -1.5F, -1.5F, 3, 3, 3),
                PartPose.offset(0, poleY(crossZ), crossZ));
        return LayerDefinition.create(mesh, 128, 64);
    }

    private TravoisModel() {}
}
