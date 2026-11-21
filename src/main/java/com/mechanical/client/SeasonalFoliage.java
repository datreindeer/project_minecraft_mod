package com.mechanical.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mechanical.Calendar;
import com.mechanical.EarthBlocks;
import com.mechanical.ForestUndergrowth;
import com.mechanical.Mechanical;
import com.mechanical.SeasonLook;
import com.mechanical.TrampledPaths;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * Seasons in the overworld's grass and leaves (phase 7, docs/seasons-and-tribes.md, maths in {@link SeasonLook}). Render
 * only: no block or saved data changes. Grass, oak/dark oak/vine and birch take their colour through our own colour
 * resolvers (biome colour moved toward the month's colour, blended across biomes and cached like vanilla's). Deciduous
 * leaves get a wrapped model that draws bare branches or buds instead of the leaves on the days {@link SeasonLook#stage}
 * says, each block on its own day (threshold from its position), so a tree thins out over a few days. Untinted
 * deciduous leaves (cherry, our woods) draw a grey copy of their texture tinted in autumn. When anything drawn changes
 * (a new month, a day of falling or budding) the tint caches are cleared and the sections round the player rebuilt.
 * Textures and models: scripts/gen_seasonal_leaves_textures.py.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class SeasonalFoliage {
    /** Deciduous leaves (namespace:name, block name minus "_leaves"). Spruce, pine, yew, jungle, acacia, mangrove, azalea, pale oak and the Ancient Oak keep theirs. */
    static final List<String> DECIDUOUS = List.of("minecraft:oak", "minecraft:dark_oak", "minecraft:birch", "minecraft:cherry",
            "minecraft:red_poplar", "minecraft:orange_poplar", "minecraft:yellow_poplar", "mechanical:aspen", "mechanical:alder",
            "mechanical:beech", "mechanical:linden", "mechanical:willow", "mechanical:ash", "mechanical:hazel");
    /** Untinted leaves that get a grey, tinted autumn model (poplars are autumn-coloured already). */
    static final Set<String> AUTUMN_MODEL = Set.of("minecraft:cherry", "mechanical:aspen", "mechanical:alder", "mechanical:beech",
            "mechanical:linden", "mechanical:willow", "mechanical:ash", "mechanical:hazel");
    /** Turn yellow in autumn; the rest go orange and red. */
    static final Set<String> YELLOW = Set.of("minecraft:birch", "mechanical:aspen", "mechanical:alder", "mechanical:linden",
            "mechanical:willow", "mechanical:ash", "mechanical:hazel");
    /** Standalone model keys by "name_look" (bare, buds, autumn). */
    private static final Map<String, StandaloneModelKey<BlockStateModelPart>> MODELS = new HashMap<>();
    static {
        for (String leaves : DECIDUOUS) {
            for (String look : looks(leaves)) {
                String n = path(leaves) + "_" + look;
                MODELS.put(n, new StandaloneModelKey<>(() -> Mechanical.MODID + ":seasonal/" + n));
            }
        }
    }

    /** Day of the year and month in the overworld; -1 elsewhere (vanilla look). Read on meshing threads. */
    private static volatile int day = -1, month = -1;
    private static int key = Integer.MIN_VALUE;

    static final ColorResolver GRASS = (biome, x, z) -> SeasonLook.grass(biome.getGrassColor(x, z), biome.getBaseTemperature(), month);
    static final ColorResolver FOLIAGE = (biome, x, z) -> SeasonLook.foliage(biome.getFoliageColor(), biome.getBaseTemperature(), month, false);
    static final ColorResolver REED = (biome, x, z) -> SeasonLook.reed(biome.getBaseTemperature(), month);
    static final ColorResolver HEATHER = (biome, x, z) -> SeasonLook.heather(biome.getBaseTemperature(), month);
    static final ColorResolver BIRCH =(biome, x, z) -> SeasonLook.foliage(FoliageColor.FOLIAGE_BIRCH, biome.getBaseTemperature(), month, true);

    private SeasonalFoliage() {}

    private static String path(String leaves) {
        return leaves.substring(leaves.indexOf(':') + 1);
    }

    private static StandaloneModelKey<BlockStateModelPart> model(String leaves, String look) {
        return MODELS.get(path(leaves) + "_" + look);
    }

    private static List<String> looks(String leaves) {
        return AUTUMN_MODEL.contains(leaves) ? List.of("bare", "buds", "autumn") : List.of("bare", "buds");
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        int d = mc.level.dimension() == Level.OVERWORLD ? Calendar.dayOfYear(mc.level.getOverworldClockTime()) : -1;
        int k = d < 0 ? -1 : SeasonLook.key(d);
        if (k == key) return;
        key = k;
        day = d;
        month = d < 0 ? -1 : d / Calendar.MONTH_DAYS;
        mc.level.clearTintCaches();
        // rebuild in place (no blank world like allChanged), a few times a month at most
        int r = mc.options.getEffectiveRenderDistance() + 1;
        SectionPos s = SectionPos.of(mc.player.blockPosition());
        mc.levelExtractor.setSectionRangeDirty(s.x() - r, mc.level.getMinSectionY(), s.z() - r, s.x() + r, mc.level.getMaxSectionY(), s.z() + r);
    }

    /** Base temperature of the biome at pos; warm when there is no level. Vanilla's tint cache calls getBiome from meshing threads too. */
    private static float temperature(BlockPos pos) {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? 2.0F : level.getBiome(pos).value().getBaseTemperature();
    }

    static int stage(BlockPos pos) {
        int d = day;
        if (d < 0) return SeasonLook.LEAVES;
        int stage = SeasonLook.stage(d, (int) Math.floorMod(Mth.getSeed(pos) >> 8, 100L));
        return stage == SeasonLook.LEAVES || !SeasonLook.seasonal(temperature(pos)) ? SeasonLook.LEAVES : stage;
    }

    static int autumn(BlockPos pos, boolean yellow) {
        int m = month;
        return m < SeasonLook.AUTUMN ? -1 : SeasonLook.autumn(temperature(pos), m, yellow);
    }

    @SubscribeEvent
    static void registerResolvers(RegisterColorHandlersEvent.ColorResolvers event) {
        event.register(GRASS);
        event.register(FOLIAGE);
        event.register(BIRCH);
    }

    /** Vanilla's grass and foliage tints with our resolvers (also Worn Grass and Trodden Path, like a Grass Block). */
    @SubscribeEvent
    static void registerTints(RegisterColorHandlersEvent.BlockTintSources event) {
        BlockTintSource grass = tint(GrassColor.getDefaultColor(), GRASS, false);
        event.register(List.of(grass), Blocks.FERN, Blocks.SHORT_GRASS, Blocks.POTTED_FERN, Blocks.BUSH, Blocks.TALL_GRASS, Blocks.LARGE_FERN);
        event.register(List.of(tint(GrassColor.getDefaultColor(), GRASS, true)), Blocks.GRASS_BLOCK,
                TrampledPaths.WORN_GRASS.get(), TrampledPaths.TRODDEN_PATH.get(),
                EarthBlocks.TURF.get(), EarthBlocks.TURF_SLAB.get(), EarthBlocks.TURF_STAIRS.get());
        event.register(List.of(tint(SeasonLook.REED_SUMMER, REED, false)), EarthBlocks.REED.get());
        event.register(List.of(tint(-1, HEATHER, false)), ForestUndergrowth.HEATHER.get());
        event.register(List.of(BlockTintSources.constant(-1), grass), Blocks.PINK_PETALS, Blocks.WILDFLOWERS);
        event.register(List.of(tint(FoliageColor.FOLIAGE_DEFAULT, FOLIAGE, false)), Blocks.OAK_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.VINE);
        event.register(List.of(tint(FoliageColor.FOLIAGE_BIRCH, BIRCH, false)), Blocks.BIRCH_LEAVES);
        for (String leaves : AUTUMN_MODEL) {
            boolean yellow = YELLOW.contains(leaves);
            event.register(List.of(new BlockTintSource() {
                @Override
                public int color(BlockState state) {
                    return -1;
                }

                @Override
                public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                    return autumn(pos, yellow);
                }
            }), block(leaves));
        }
    }

    /** {@code inHand} for items; in the world the resolver's colour (the lower half's for a double plant); no tint on a Grass Block's particles. */
    private static BlockTintSource tint(int inHand, ColorResolver resolver, boolean untintedParticles) {
        return new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return inHand;
            }

            @Override
            public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                boolean upper = state.hasProperty(DoublePlantBlock.HALF) && state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.UPPER;
                return level.getBlockTint(upper ? pos.below() : pos, resolver);
            }

            @Override
            public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
                return untintedParticles ? -1 : colorInWorld(state, level, pos);
            }
        };
    }

    private static Block block(String leaves) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.parse(leaves + "_leaves"));
    }

    @SubscribeEvent
    static void registerModels(ModelEvent.RegisterStandalone event) {
        for (String leaves : DECIDUOUS) {
            for (String look : looks(leaves)) {
                event.register(model(leaves, look), SimpleUnbakedStandaloneModel.simpleModelWrapper(
                        Identifier.fromNamespaceAndPath(Mechanical.MODID, "block/seasonal/" + path(leaves) + "_" + look)));
            }
        }
    }

    @SubscribeEvent
    static void wrapLeaves(ModelEvent.ModifyBakingResult event) {
        var baked = event.getBakingResult();
        for (String leaves : DECIDUOUS) {
            BlockStateModelPart bare = baked.standaloneModels().get(model(leaves, "bare")), buds = baked.standaloneModels().get(model(leaves, "buds"));
            BlockStateModelPart autumn = AUTUMN_MODEL.contains(leaves) ? baked.standaloneModels().get(model(leaves, "autumn")) : null;
            if (bare == null || buds == null) continue;
            boolean yellow = YELLOW.contains(leaves);
            for (BlockState state : block(leaves).getStateDefinition().getPossibleStates()) {
                baked.blockStateModels().computeIfPresent(state, (s, m) -> new Leaves(m, bare, buds, autumn, yellow));
            }
        }
    }

    /** Leaves, buds or bare branches by season and position; collision, drops and decay stay the leaves'. */
    static final class Leaves extends DelegateBlockStateModel {
        private final BlockStateModelPart bare, buds, autumn;
        private final boolean yellow;

        Leaves(BlockStateModel leaves, BlockStateModelPart bare, BlockStateModelPart buds, BlockStateModelPart autumn, boolean yellow) {
            super(leaves);
            this.bare = bare;
            this.buds = buds;
            this.autumn = autumn;
            this.yellow = yellow;
        }

        @Override
        public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
            switch (stage(pos)) {
                case SeasonLook.BARE -> parts.add(bare);
                case SeasonLook.BUDS -> parts.add(buds);
                default -> {
                    if (autumn != null && autumn(pos, yellow) != -1) parts.add(autumn);
                    else delegate.collectParts(level, pos, state, random, parts);
                }
            }
        }
    }
}
