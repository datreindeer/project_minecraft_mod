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

/**
 * A Trader Caravan's canvas tilt (people.Caravans): short upright side panels from the boards' top, then a round arch of
 * {@link #SEGMENTS} panels over the bed, every other one in the second stripe of the cloth (UV regions A and B), and a
 * bent wooden hoop at each open end so the load shows front and back. One for the cart, one for the wagon; textures by
 * scripts/gen_cart_canopy_textures.py with the same layout (cart 128 x 128, wagon 256 x 128), dyed per caravan kind.
 * Model space as the cart's and wagon's: y down, px.
 */
public class TiltModel extends Model<Float> {
    public static final ModelLayerLocation CART = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "cart"), "canopy"),
            WAGON = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "wagon"), "canopy");
    static final int SEGMENTS = 6;

    public TiltModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
    }

    public static LayerDefinition cart() {
        // bed 20 px wide inside 2 px boards whose top is at y -16; 30 px long; the arch's crown at y -32
        return create(11, -16, 5, 30, new int[] {0, 0}, new int[] {0, 32}, new int[] {0, 64}, new int[] {64, 64}, 128, 128);
    }

    public static LayerDefinition wagon() {
        // bed 24 px wide inside its boards (top at y -20); 48 px long
        return create(13, -20, 7, 48, new int[] {0, 0}, new int[] {128, 0}, new int[] {0, 50}, new int[] {128, 60}, 256, 128);
    }

    /**
     * {@code radius}: the arch's (and the side panels' x); {@code top}: the boards' top; {@code rise}: the side panels'
     * height up to where the arch starts; UV corners for stripe A, stripe B, the sides and the hoops.
     */
    static LayerDefinition create(float radius, float top, float rise, float length, int[] a, int[] b, int[] sides, int[] hoops, int tw, int th) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float centre = top - rise, half = length / 2;
        int width = segmentWidth(radius);
        root.addOrReplaceChild("left_side", CubeListBuilder.create().texOffs(sides[0], sides[1]).addBox(-0.5F, -rise, -half, 1, rise, length),
                PartPose.offset(-radius, top, 0));
        root.addOrReplaceChild("right_side", CubeListBuilder.create().texOffs(sides[0], sides[1]).mirror().addBox(-0.5F, -rise, -half, 1, rise, length),
                PartPose.offset(radius, top, 0));
        for (int k = 0; k < SEGMENTS; k++) {
            double ang = Math.PI - (k + 0.5) * Math.PI / SEGMENTS; // round from the left side over to the right
            float x = (float) (radius * Math.cos(ang)), y = (float) (centre - radius * Math.sin(ang)), roll = (float) (Math.PI / 2 - ang);
            int[] uv = k % 2 == 0 ? a : b;
            root.addOrReplaceChild("arch" + k, CubeListBuilder.create().texOffs(uv[0], uv[1]).addBox(-width / 2.0F, -0.5F, -half, width, 1, length),
                    PartPose.offsetAndRotation(x, y, 0, 0, 0, roll));
            for (int end = -1; end <= 1; end += 2) {
                float r = radius + 0.4F, hx = (float) (r * Math.cos(ang)), hy = (float) (centre - r * Math.sin(ang));
                root.addOrReplaceChild("hoop" + k + (end < 0 ? "f" : "b"), CubeListBuilder.create().texOffs(hoops[0], hoops[1])
                        .addBox(-width / 2.0F - 0.2F, -0.6F, -0.6F, width + 0.4F, 1.2F, 1.2F), PartPose.offsetAndRotation(hx, hy, end * (half - 0.4F), 0, 0, roll));
            }
        }
        return LayerDefinition.create(mesh, tw, th);
    }

    /** A panel's width: the chord of its share of the half circle, rounded up so the panels overlap. */
    static int segmentWidth(float radius) {
        return (int) Math.ceil(2 * radius * Math.sin(Math.PI / SEGMENTS / 2) + 0.3);
    }
}
