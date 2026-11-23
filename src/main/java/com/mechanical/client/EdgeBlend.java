package com.mechanical.client;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mechanical.ClientConfig;
import com.mechanical.Mechanical;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.quad.BakedColors;

/**
 * Edge blending: where two related blocks meet on their top faces, the stronger one frays a few pixels over the weaker
 * one instead of a straight seam (grass over dirt, sand over gravel, bricks toothed into cobblestone, oak boards running
 * on into spruce). Render only, at chunk meshing. The table (strength, groups, mask style) lives in
 * scripts/gen_blend_edges.py, which also draws each fraying block's sheet of overlay tiles (its top texture cut out by
 * an edge mask) and writes blend/edges.json. Blocks are matched by the sprite of their top face, so slabs and every
 * random rotation of a block come along. At bake time every block state whose model has one full-size top face in the
 * table is wrapped; when a chunk is meshed the wrapper looks at the 8 neighbours on the same level and adds, for each
 * stronger neighbour in a shared group with its top face level with ours (or up to 1/16 higher, a path next to grass), one
 * cutout quad on our top face: the neighbour's tile for that edge or corner. The quads copy the top face's corners, so
 * they lie exactly on it (no z-fighting) and are culled with it. A tinted neighbour (grass, seasonal) lends its colour as
 * computed at its own position. Toggle: blendEdges in mechanical-client.toml.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class EdgeBlend {
    private static final Identifier TABLE = Identifier.fromNamespaceAndPath(Mechanical.MODID, "blend/edges.json");
    private static final int[] DX = {0, 1, 0, -1}, DZ = {-1, 0, 1, 0}; // N E S W
    private static final int[] CX = {1, 1, -1, -1}, CZ = {-1, 1, 1, -1}; // NE SE SW NW
    private static final float STEP = 1 / 16F + 1E-4F, UV_INSET = 1 / 128F;

    /** One row of the table. {@code sheet} null: takes overlays but never frays over anything. */
    record Entry(int strength, int groups, @Nullable TextureAtlasSprite sheet, int cols, int rows, int k, int m, boolean corners, boolean parity, boolean join) {}

    /** A wrapped block state: its row, its top face (the overlays copy its corners) and whether that face is culled. */
    record Top(Entry entry, BakedQuad face, boolean culled, boolean snowy) {
        float y() {
            return face.position0().y();
        }
    }

    private static Map<BlockState, Top> tops = Map.of();
    /** Dev measurement (people.BlendShots bench): time spent in the neighbour lookups and overlay quads, when {@link #profile}. */
    public static final java.util.concurrent.atomic.LongAdder NANOS = new java.util.concurrent.atomic.LongAdder(),
            COMPILE_NANOS = new java.util.concurrent.atomic.LongAdder(), COMPILES = new java.util.concurrent.atomic.LongAdder();
    public static volatile boolean profile;

    private EdgeBlend() {}

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        ClientConfig.onChange = () -> Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().level != null) Minecraft.getInstance().levelExtractor.allChanged();
        });
        ModList.get().getModContainerById(Mechanical.MODID).ifPresent(c -> c.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new));
    }

    private static Map<Identifier, JsonObject> table() {
        Map<Identifier, JsonObject> out = new HashMap<>();
        var res = Minecraft.getInstance().getResourceManager().getResource(TABLE);
        if (res.isEmpty()) return out;
        try (Reader r = res.get().openAsReader()) {
            for (JsonElement el : JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("blocks")) {
                JsonObject o = el.getAsJsonObject();
                for (JsonElement k : o.getAsJsonArray("keys")) out.putIfAbsent(Identifier.parse(k.getAsString()), o);
            }
        } catch (Exception e) {
            Mechanical.LOGGER.error("edge blending: could not read {}", TABLE, e);
        }
        return out;
    }

    @SubscribeEvent
    static void wrap(ModelEvent.ModifyBakingResult event) {
        Map<Identifier, JsonObject> table = table();
        Map<JsonObject, Entry> entries = new IdentityHashMap<>();
        Map<BlockState, Top> found = new IdentityHashMap<>();
        var atlas = event.getBlockAtlasPreparations();
        RandomSource random = RandomSource.create(0);
        List<BlockStateModelPart> parts = new ArrayList<>();
        event.getBakingResult().blockStateModels().replaceAll((state, model) -> {
            parts.clear();
            model.collectParts(random, parts);
            BakedQuad face = null;
            boolean culled = false;
            for (BlockStateModelPart part : parts) {
                for (BakedQuad q : part.getQuads(Direction.UP)) if (fullTop(q)) { face = q; culled = true; }
                for (BakedQuad q : part.getQuads(null)) if (fullTop(q)) { face = q; culled = false; }
            }
            if (face == null) return model;
            JsonObject row = table.get(face.materialInfo().sprite().contents().name());
            if (row == null) return model;
            Entry entry = entries.computeIfAbsent(row, o -> {
                TextureAtlasSprite sheet = o.has("sheet") ? atlas.getSprite(Identifier.parse(o.get("sheet").getAsString())) : null;
                return sheet == null ? new Entry(o.get("strength").getAsInt(), o.get("groups").getAsInt(), null, 1, 1, 1, 1, false, false, false)
                        : new Entry(o.get("strength").getAsInt(), o.get("groups").getAsInt(), sheet, o.get("cols").getAsInt(), o.get("rows").getAsInt(),
                                o.get("k").getAsInt(), o.get("m").getAsInt(), o.get("corners").getAsBoolean(), o.get("select").getAsString().equals("parity"),
                                o.has("join") && o.get("join").getAsBoolean());
            });
            Top top = new Top(entry, face, culled, state.hasProperty(BlockStateProperties.SNOWY) && state.getValue(BlockStateProperties.SNOWY));
            found.put(state, top);
            return new Blended(model, top);
        });
        tops = found;
        Mechanical.LOGGER.info("edge blending: {} block states, {} table rows", found.size(), entries.size());
    }

    /** An upward quad covering the whole block face. */
    private static boolean fullTop(BakedQuad q) {
        if (q.direction() != Direction.UP) return false;
        float minX = 1, maxX = 0, minZ = 1, maxZ = 0, y = q.position0().y();
        for (int i = 0; i < 4; i++) {
            Vector3fc p = q.position(i);
            if (Math.abs(p.y() - y) > 1E-4F) return false;
            minX = Math.min(minX, p.x()); maxX = Math.max(maxX, p.x());
            minZ = Math.min(minZ, p.z()); maxZ = Math.max(maxZ, p.z());
        }
        return minX < 1E-4F && minZ < 1E-4F && maxX > 1 - 1E-4F && maxZ > 1 - 1E-4F;
    }

    /** Same as hash3 in gen_blend_edges.py. */
    static int hash(int x, int z, int salt) {
        int h = x * 0x27D4EB2D ^ z * 0x165667B1 ^ salt * 0x61C88647;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return h & 0x7FFFFFFF;
    }

    /** Class of a lattice corner: picks the edge depth there, so both edges meeting at it agree. */
    private static int corner(int x, int z, int k) {
        return k == 1 ? 0 : hash(x, z, 1) % k;
    }

    /** Can {@code over} fray over {@code base}: stronger, shares a group, its top level with ours or a step (1/16) higher. */
    private static boolean frays(@Nullable Top over, Top base) {
        if (over == null || over.snowy || over.entry.sheet == null || over.entry.strength <= base.entry.strength
                || (over.entry.groups & base.entry.groups) == 0) return false;
        float dy = over.y() - base.y();
        return dy > -1E-4F && dy < STEP;
    }

    private record Overlay(Top over, int tile, BlockState state, BlockPos pos) {}

    static final class Blended extends DelegateBlockStateModel {
        private final Top self;

        Blended(BlockStateModel model, Top self) {
            super(model);
            this.self = self;
        }

        @Override
        public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
            int before = parts.size();
            delegate.collectParts(level, pos, state, random, parts);
            if (!ClientConfig.blendEdges || self.snowy || parts.size() == before) return;
            if (!profile) {
                overlay(level, pos, parts, before);
                return;
            }
            long t = System.nanoTime();
            overlay(level, pos, parts, before);
            NANOS.add(System.nanoTime() - t);
        }

        private void overlay(BlockAndTintGetter level, BlockPos pos, List<BlockStateModelPart> parts, int before) {
            if (self.culled && level.getBlockState(pos.above()).isSolidRender()) return; // top face hidden: underground, walls
            Map<BlockState, Top> map = tops;
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            BlockPos.MutableBlockPos n = new BlockPos.MutableBlockPos();
            Top[] edge = new Top[4];
            BlockState[] edgeState = new BlockState[4];
            for (int d = 0; d < 4; d++) {
                BlockState ns = level.getBlockState(n.set(x + DX[d], y, z + DZ[d]));
                Top t = map.get(ns);
                if (frays(t, self)) {
                    edge[d] = t;
                    edgeState[d] = ns;
                }
            }
            List<Overlay> found = null;
            for (int d = 0; d < 4; d++) {
                Top t = edge[d];
                if (t == null) continue;
                Entry e = t.entry;
                int a, b;
                switch (d) {
                    case 0 -> { a = corner(x, z, e.k); b = corner(x + 1, z, e.k); }
                    case 1 -> { a = corner(x + 1, z, e.k); b = corner(x + 1, z + 1, e.k); }
                    case 2 -> { a = corner(x, z + 1, e.k); b = corner(x + 1, z + 1, e.k); }
                    default -> { a = corner(x, z, e.k); b = corner(x, z + 1, e.k); }
                }
                if (e.join && d % 2 == 0) { // planks: a board running along the edge carries on into the same wood beside us
                    if (edge[3] != null && edge[3].entry == e) a = 1;
                    if (edge[1] != null && edge[1].entry == e) b = 1;
                }
                int v = e.parity ? Math.floorMod(d % 2 == 0 ? x : z, e.m) : hash(x, z, 2 + d) % e.m; // bricks: position along the edge
                if (found == null) found = new ArrayList<>(4);
                found.add(new Overlay(t, d * e.k * e.k * e.m + (a * e.k + b) * e.m + v, edgeState[d], new BlockPos(x + DX[d], y, z + DZ[d])));
            }
            for (int c = 0; c < 4; c++) {
                BlockState ns = level.getBlockState(n.set(x + CX[c], y, z + CZ[c]));
                Top t = map.get(ns);
                // the two edges beside this corner already cover it when either is the same block
                if (!frays(t, self) || !t.entry.corners || edge[c] != null && edge[c].entry == t.entry || edge[(c + 1) % 4] != null && edge[(c + 1) % 4].entry == t.entry) continue;
                Entry e = t.entry;
                int px = x + (CX[c] > 0 ? 1 : 0), pz = z + (CZ[c] > 0 ? 1 : 0);
                if (found == null) found = new ArrayList<>(4);
                found.add(new Overlay(t, 4 * e.k * e.k * e.m + c * e.k + corner(px, pz, e.k), ns, n.immutable()));
            }
            if (found == null) return;
            found.sort((o1, o2) -> Integer.compare(o1.over.entry.strength, o2.over.entry.strength)); // the strongest drawn last, on top
            List<BakedQuad> quads = new ArrayList<>(found.size());
            for (Overlay o : found) quads.add(quad(self.face, o, level));
            parts.add(new Part(quads, self.culled, parts.get(before)));
        }
    }

    /** The overlay tile laid on our top face: same corners, the neighbour's sheet, its tint baked in. */
    private static BakedQuad quad(BakedQuad face, Overlay o, BlockAndTintGetter level) {
        Entry e = o.over.entry;
        TextureAtlasSprite s = e.sheet;
        int col = o.tile % e.cols, row = o.tile / e.cols;
        long[] uv = new long[4];
        for (int i = 0; i < 4; i++) {
            Vector3fc p = face.position(i);
            uv[i] = UVPair.pack(s.getU((col + inset(p.x())) / e.cols), s.getV((row + inset(p.z())) / e.rows));
        }
        BakedQuad.MaterialInfo m = face.materialInfo();
        int tintIndex = o.over.face.materialInfo().tintIndex();
        int color = -1;
        if (tintIndex >= 0) {
            List<BlockTintSource> sources = Minecraft.getInstance().getBlockColors().getTintSources(o.state);
            if (tintIndex < sources.size() && sources.get(tintIndex) != null) color = 0xFF000000 | sources.get(tintIndex).colorInWorld(o.state, level, o.pos);
        }
        return new BakedQuad(face.position0(), face.position1(), face.position2(), face.position3(), uv[0], uv[1], uv[2], uv[3], Direction.UP,
                new BakedQuad.MaterialInfo(s, ChunkSectionLayer.CUTOUT, m.itemRenderType(), m.itemGlintRenderType(), m.itemGlintSpecialRenderType(), -1,
                        m.shadeDirectionOverride(), m.lightEmission(), m.ambientOcclusion()),
                face.bakedNormals(), BakedColors.of(color));
    }

    /** A corner 1/8 texel inside the tile, so the next tile on the sheet never bleeds in along the block's edge. */
    private static float inset(float f) {
        return UV_INSET + f * (1 - 2 * UV_INSET);
    }

    /** The overlay quads, culled with the top face they lie on. */
    private record Part(List<BakedQuad> quads, boolean culled, BlockStateModelPart base) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction direction) {
            return (culled ? direction == Direction.UP : direction == null) ? quads : List.of();
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean useAmbientOcclusion() {
            return base.useAmbientOcclusion();
        }

        @Override
        public net.minecraft.util.TriState ambientOcclusion() {
            return base.ambientOcclusion();
        }

        @Override
        public Material.Baked particleMaterial() {
            return base.particleMaterial();
        }

        @Override
        public int materialFlags() {
            return 0;
        }
    }
}
