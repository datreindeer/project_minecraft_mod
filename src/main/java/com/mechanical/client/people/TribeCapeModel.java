package com.mechanical.client.people;

import java.util.EnumSet;
import java.util.Set;

import com.mechanical.Mechanical;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * The 3D pieces of the capes (TribeCapeLayer draws them, and the flowing cloth itself): per style a "collar" that follows
 * the body (the raised neck wings and the clasps on the chest) and a "right" and "left" shoulder ornament in arm
 * coordinates (the arm box runs from y -2 to 10 round its pivot). The right side is described here (outward is -x); the
 * left is its mirror. The elder wears fur, bone plates, antler tines and amber; the caravan leader embroidered cloth,
 * braid, tassels and brass; the Oak Keeper bark, moss, oak leaves and acorns. Nothing stands inside x -4.5..4.5 above
 * y 0, where the head turns. The tile places on the 128 x 128 sheet mirror scripts/gen_capes.py.
 */
public final class TribeCapeModel {
    public static final ModelLayerLocation ELDER = layer("elder"), TRADER = layer("trader"), OAK = layer("oak");

    private static final int[] PAD = {64, 0}, TOP = {64, 16}, TRIM = {64, 32}, HARD = {96, 0}, JEWEL = {96, 16}, FRINGE = {96, 32};

    private static final Set<Direction> FLAT = EnumSet.of(Direction.NORTH);

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "tribe_cape_" + name), "main");
    }

    /** Builds one side: boxes and child parts are given for the right shoulder and mirrored for the left. */
    private record Side(PartDefinition part, float s, int[] count) {
        Side box(int[] tile, float x, float y, float z, float w, float h, float d) {
            return part("b" + count[0]++, 0, 0, 0, 0, 0, 0).add(tile, x, y, z, w, h, d).back(this);
        }

        /** A child part at a pivot, turned (z positive lifts the outer end of what points outward, and tips what points up outward... mirrored on the left). */
        Child part(String name, float px, float py, float pz, float xr, float yr, float zr) {
            return new Child(this, name, PartPose.offsetAndRotation(px * s, py, pz, xr, yr * s, zr * s));
        }

        /** A flat hanging or standing sprite (one of the five at the bottom of the sheet), 5 x 6, hung from its top middle. */
        Side leaf(int sprite, float px, float py, float pz, float xr, float yr, float zr) {
            part.addOrReplaceChild("l" + count[0]++, CubeListBuilder.create().texOffs(sprite * 5, 48).addBox(-2.5F, 0, 0, 5, 6, 0, FLAT),
                    PartPose.offsetAndRotation(px * s, py, pz, xr, yr * s, zr * s));
            return this;
        }
    }

    private static final class Child {
        private final Side owner;
        private final String name;
        private final PartPose pose;
        private final CubeListBuilder cubes = CubeListBuilder.create();

        Child(Side owner, String name, PartPose pose) {
            this.owner = owner;
            this.name = name;
            this.pose = pose;
        }

        /** A hanging row of fringe: one face, seen from both sides (a box's two faces mirror each other and would z-fight). */
        Child sheet(float w, float h) {
            cubes.texOffs(FRINGE[0], FRINGE[1]).addBox(-w / 2, 0, 0, w, h, 0, FLAT);
            return this;
        }

        Child add(int[] tile, float x, float y, float z, float w, float h, float d) {
            cubes.texOffs(tile[0], tile[1]).addBox(owner.s > 0 ? x : -x - w, y, z, w, h, d, CubeDeformation.NONE);
            return this;
        }

        /** Adds the part and returns it, to hang further parts from. */
        Side done() {
            return new Side(owner.part.addOrReplaceChild(name, cubes, pose), owner.s, owner.count);
        }

        private Side back(Side s) {
            done();
            return s;
        }
    }

    private interface Builder {
        void build(Side shoulder, Side collar);
    }

    private static LayerDefinition create(Builder builder) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition collar = root.addOrReplaceChild("collar", CubeListBuilder.create(), PartPose.ZERO);
        int[] count = {0};
        PartDefinition right = root.addOrReplaceChild("right", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition left = root.addOrReplaceChild("left", CubeListBuilder.create(), PartPose.ZERO);
        builder.build(new Side(right, 1, count), new Side(collar, 1, count));
        builder.build(new Side(left, -1, count), new Side(collar, -1, count));
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition createElder() {
        return create((arm, collar) -> {
            // the fur pad over the shoulder, hanging down outside the arm
            arm.box(PAD, -4.1F, -2.9F, -2.9F, 4.6F, 2.2F, 5.8F);
            arm.box(PAD, -4.4F, -1.9F, -2.5F, 1.3F, 2.4F, 5.0F);
            // a carved bone plate on top, tipped up at its outer edge, a second smaller one over it
            Side plate = arm.part("plate", -1.2F, -2.9F, 0, 0, 0, 0.30F).add(TOP, -3.6F, -1.0F, -2.5F, 4.0F, 1.1F, 5.0F).done();
            plate.box(TRIM, -3.75F, -0.45F, -2.65F, 4.3F, 0.6F, 5.3F);
            plate.box(TOP, -3.0F, -1.7F, -1.7F, 2.8F, 0.8F, 3.4F);
            plate.box(JEWEL, -2.2F, -2.2F, -0.8F, 1.4F, 0.7F, 1.6F); // the amber
            // an antler rising from the plate: a beam out and up, a brow tine forward, one back, the tip turning in
            Side beam = plate.part("antler", -2.9F, -1.4F, 0.7F, -0.12F, 0, -0.62F).add(HARD, -0.65F, -4.2F, -0.65F, 1.3F, 4.4F, 1.3F).done();
            beam.part("brow", 0, -1.7F, -0.3F, 0.95F, 0, 0.25F).add(HARD, -0.4F, -2.8F, -0.4F, 0.8F, 2.9F, 0.8F).done();
            Side tip = beam.part("tip", 0, -4.0F, 0, -0.1F, 0, 0.75F).add(HARD, -0.5F, -3.2F, -0.5F, 1.0F, 3.4F, 1.0F).done();
            tip.part("point", 0, -3.0F, 0, 0, 0, 0.45F).add(HARD, -0.35F, -2.0F, -0.35F, 0.7F, 2.2F, 0.7F).done();
            beam.part("back", 0, -3.9F, 0.2F, -0.75F, 0, -0.45F).add(HARD, -0.4F, -2.4F, -0.4F, 0.8F, 2.6F, 0.8F).done();
            // fangs and bone beads along the pad's edge, thongs hanging under it
            for (int i = 0; i < 3; i++) arm.box(HARD, -4.75F, -0.1F + (i == 1 ? 0.3F : 0), -2.0F + i * 1.65F, 0.7F, 1.5F + (i == 1 ? 0.5F : 0), 0.7F);
            arm.part("thongs", -4.2F, 0.4F, 0, 0, 1.5708F, 0).sheet(5, 3.0F).done();
            // fur tufts spilling over the edges
            arm.leaf(0, -4.45F, -2.6F, -0.2F, 0, 1.5708F, 0.1F).leaf(1, -2.0F, -2.0F, -3.0F, -0.15F, 0, 0)
                    .leaf(3, -4.0F, -2.2F, 1.8F, 0.1F, 1.0F, 0.2F).leaf(4, -4.0F, -2.2F, -1.8F, -0.1F, -1.0F, 0.2F);
            // the collar: a roll of fur across the back of the neck, rising at the sides
            collar.box(PAD, -4.9F, 0.4F, 2.1F, 5.0F, 1.5F, 2.2F);
            collar.part("wing", -4.7F, 0.9F, 2.3F, -0.16F, 0.30F, -0.22F).add(PAD, -1.6F, -3.7F, -0.6F, 2.4F, 4.0F, 1.7F).done()
                    .box(TOP, -1.3F, -4.4F, -0.4F, 1.5F, 0.8F, 1.2F).leaf(2, -0.4F, -3.4F, 1.25F, 0.2F, 0, 0).leaf(0, -1.7F, -2.6F, 0.3F, 0, 1.5708F, 0.15F);
            // the clasp on the chest: an amber set in a bone toggle, a fang under it
            collar.box(TOP, -5.1F, 0.1F, -3.1F, 2.2F, 2.2F, 0.8F).box(JEWEL, -4.55F, 0.65F, -3.4F, 1.1F, 1.1F, 0.4F)
                    .box(HARD, -4.35F, 2.3F, -2.95F, 0.7F, 1.5F, 0.6F).box(TRIM, -4.95F, -0.35F, -2.9F, 1.9F, 0.5F, 5.4F);
        });
    }

    public static LayerDefinition createTrader() {
        return create((arm, collar) -> {
            // a broad padded epaulet of embroidered cloth with a braid edge
            arm.box(PAD, -4.0F, -2.9F, -2.8F, 4.5F, 1.7F, 5.6F);
            arm.box(TRIM, -4.2F, -1.5F, -3.0F, 4.8F, 0.7F, 6.0F);
            // the peaked wing over it, sweeping up and out, braid along its edges and a gem on top
            Side wing = arm.part("wing", -1.4F, -2.8F, 0, 0, 0, 0.36F).add(PAD, -3.4F, -1.0F, -2.5F, 4.0F, 1.0F, 5.0F).done();
            wing.box(TRIM, -3.9F, -1.15F, -2.7F, 0.7F, 1.3F, 5.4F).box(TRIM, -3.4F, -1.2F, -2.75F, 3.8F, 0.5F, 0.6F)
                    .box(TRIM, -3.4F, -1.2F, 2.15F, 3.8F, 0.5F, 0.6F).box(HARD, -2.5F, -1.55F, -1.0F, 2.0F, 0.6F, 2.0F)
                    .box(JEWEL, -2.0F, -2.0F, -0.5F, 1.0F, 0.6F, 1.0F);
            wing.part("peak", -3.5F, -0.8F, 0, 0, 0, 0.55F).add(PAD, -2.0F, -0.4F, -1.6F, 2.2F, 0.8F, 3.2F).done()
                    .box(TRIM, -2.5F, -0.55F, -1.2F, 0.7F, 0.9F, 2.4F).box(HARD, -3.05F, -0.45F, -0.45F, 0.7F, 0.7F, 0.9F);
            // tassels hanging from the epaulet: outside, in front and behind
            arm.part("fringe", -4.3F, -0.9F, 0, 0, 1.5708F, 0).sheet(6, 4).done();
            arm.part("fringefront", -2.2F, -0.9F, -3.05F, 0, 0, 0).sheet(4, 3).done();
            arm.part("fringeback", -2.2F, -0.9F, 3.05F, 0, 0, 0).sheet(4, 3).done();
            // the standing collar: rich cloth rising behind the neck at the sides, braid on its top edge
            collar.box(PAD, -4.9F, 0.2F, 2.2F, 5.0F, 1.7F, 1.8F).box(TRIM, -4.9F, 0.0F, 3.9F, 5.0F, 0.6F, 0.6F);
            collar.part("stand", -4.7F, 0.8F, 2.4F, -0.20F, 0.34F, -0.26F).add(PAD, -1.5F, -3.9F, -0.4F, 2.2F, 4.2F, 1.2F).done()
                    .box(TRIM, -1.65F, -4.5F, -0.55F, 2.5F, 0.7F, 1.5F).box(TRIM, -2.0F, -3.9F, -0.5F, 0.6F, 4.1F, 1.4F)
                    .box(HARD, -1.3F, -5.2F, -0.1F, 0.9F, 0.8F, 0.7F);
            // the brass clasp on the chest with its gem, and a short tassel under it
            collar.box(HARD, -5.2F, 0.1F, -3.1F, 2.4F, 2.4F, 0.8F).box(JEWEL, -4.5F, 0.8F, -3.45F, 1.0F, 1.0F, 0.4F)
                    .box(TRIM, -5.0F, -0.35F, -2.9F, 1.9F, 0.5F, 5.4F);
            collar.part("tassel", -4.0F, 2.5F, -2.8F, 0, 0, 0).sheet(2, 4).done();
        });
    }

    public static LayerDefinition createOak() {
        return create((arm, collar) -> {
            // slabs of bark lapped over each other, moss grown on top
            arm.box(PAD, -4.1F, -2.8F, -2.8F, 4.6F, 1.8F, 5.6F);
            arm.box(PAD, -4.6F, -1.8F, -2.4F, 1.3F, 3.0F, 4.8F);
            Side slab = arm.part("slab", -1.0F, -2.8F, 0, 0, 0, 0.30F).add(PAD, -3.9F, -1.1F, -2.4F, 4.0F, 1.2F, 4.8F).done();
            slab.box(TOP, -3.0F, -1.6F, -1.7F, 3.0F, 0.6F, 3.0F).box(TOP, -4.1F, -0.7F, 0.4F, 1.2F, 0.7F, 2.2F);
            Side crest = slab.part("crest", -3.3F, -1.0F, 0.2F, 0, 0, 0.55F).add(PAD, -2.4F, -0.6F, -1.7F, 2.8F, 0.9F, 3.4F).done();
            // a young oak twig growing out of the crest, leaves on it
            Side twig = crest.part("twig", -1.4F, -0.4F, 0.6F, -0.15F, 0, -0.65F).add(PAD, -0.4F, -2.6F, -0.4F, 0.8F, 2.8F, 0.8F).done();
            twig.part("fork", 0, -1.4F, 0, 0.7F, 0, 0.7F).add(PAD, -0.3F, -1.5F, -0.3F, 0.6F, 1.6F, 0.6F).done()
                    .leaf(1, 0, -1.0F, 0, 3.1416F + 0.2F, 0.5F, 0);
            twig.leaf(0, 0, -2.2F, 0, 3.1416F, 0, -0.25F).leaf(3, -0.2F, -2.0F, 0.1F, 3.1416F, 1.5708F, 0.6F);
            // layers of leaves spilling over the shoulder
            for (int i = 0; i < 3; i++) {
                float z = -1.9F + i * 1.9F;
                arm.leaf(i, -3.9F, -2.9F, z, 0, 1.5708F, 0.62F).leaf((i + 2) % 5, -4.7F, -1.2F, z + 0.9F, 0, 1.5708F, 0.30F);
            }
            arm.leaf(3, -2.4F, -2.4F, -2.95F, -0.35F, 0, 0.15F).leaf(4, -0.9F, -2.4F, -3.0F, -0.25F, 0, -0.2F)
                    .leaf(1, -2.4F, -2.4F, 3.0F, 0.4F, 0, 0.15F).leaf(2, -0.8F, -2.3F, 3.05F, 0.3F, 0, -0.2F);
            // acorns hanging under the bark: scaly cups and gold-brown nuts
            for (int i = 0; i < 2; i++) {
                float z = -1.5F + i * 2.4F, drop = i * 0.5F;
                arm.box(TRIM, -4.95F, 1.0F, z + 0.4F, 0.3F, 0.8F + drop, 0.3F).box(HARD, -5.4F, 1.8F + drop, z - 0.1F, 1.3F, 0.8F, 1.3F)
                        .box(JEWEL, -5.25F, 2.6F + drop, z + 0.05F, 1.0F, 1.1F, 1.0F);
            }
            // the collar: a gnarled bark roll behind the neck rising into two leafy boughs
            collar.box(PAD, -4.9F, 0.2F, 2.1F, 5.0F, 1.8F, 2.0F).box(TOP, -3.8F, 0.0F, 3.0F, 2.6F, 0.5F, 1.3F);
            collar.part("bough", -4.7F, 0.9F, 2.4F, -0.2F, 0.32F, -0.30F).add(PAD, -1.0F, -3.6F, -0.4F, 1.4F, 3.8F, 1.2F).done()
                    .leaf(0, -0.4F, -3.4F, 0.95F, 0.25F, 0, 0).leaf(1, -0.6F, -3.4F, 0.2F, 3.1416F, 0.3F, -0.35F)
                    .leaf(2, -1.1F, -3.4F, 0.1F, 0, 1.5708F, 0.45F).leaf(3, -0.2F, -2.2F, 1.1F, 0.35F, 0.25F, -0.3F);
            collar.leaf(4, -2.6F, 0.4F, 4.3F, 0.3F, 0, 0.35F).leaf(2, -0.6F, 0.6F, 4.35F, 0.3F, 0, -0.2F);
            // an acorn clasp on the chest, two leaves behind it
            collar.leaf(1, -5.0F, -0.1F, -2.75F, 0, 0, 0.75F).leaf(3, -3.4F, -0.1F, -2.8F, 0, 0, -0.65F)
                    .box(HARD, -5.0F, 0.5F, -3.3F, 1.9F, 1.0F, 1.0F).box(JEWEL, -4.75F, 1.5F, -3.2F, 1.4F, 1.6F, 0.8F)
                    .box(TRIM, -4.95F, -0.35F, -2.9F, 1.9F, 0.5F, 5.4F);
        });
    }

    private TribeCapeModel() {}
}
