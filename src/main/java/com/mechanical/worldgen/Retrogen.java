package com.mechanical.worldgen;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import com.mechanical.Mechanical;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.FeaturePlacer;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Retrogen: old chunks get the small features of newer versions. Each overworld chunk carries a marker (an index into the
 * world's saved list of feature sets) saying which {@link #FEATURES} it already has; a chunk generated now is marked with the
 * current set. A loaded chunk with an older or missing marker is queued and, a couple per tick, gets the features it lacks,
 * placed with their own placement rules and biome lists, seeded per chunk. Only when its 8 neighbours are loaded (features
 * may reach into them) and only on land players have barely touched: every chunk of the 3x3 must have under a minute of
 * InhabitedTime and no player-made blocks at or just below the surface (see {@link #playerMade}). A skipped chunk is
 * marked too and never tried again. Structures and large or destructive features (huts, dens, camps, burial mounds, trees,
 * trails) are never retrogenned.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Retrogen {
    /** Placed features (mechanical:...) that may be added to old chunks. Append new small features here. */
    static final List<String> FEATURES = List.of(
            "ore_tin",
            "abandoned_campsite", "animal_carving", "balanced_rock", "cairn", "fishing_camp", "flint_quarry", "glacial_erratic",
            "hunters_lean_to", "mossy_boulder", "rune_stone", "sandstone_boulder", "shrine_statue", "standing_stones", "stone_head",
            "blackberry_bush", "blueberry_bush", "bramble", "bramble_in_plains", "bustard_nest", "capercaillie_nest", "chanterelle",
            "cloudberry_bush", "common_crane_nest", "elderberry_bush", "fly_agaric", "goose_nest", "grey_heron_nest", "hazel_shrub",
            "juniper", "juniper_in_hills", "lingonberry_bush", "loose_chalk", "loose_red_ochre", "loose_yellow_ochre", "loose_stone",
            "loose_granite", "loose_diorite", "loose_andesite", "loose_flint", "mallard_nest", "morel", "mute_swan_nest",
            "ore_indicators", "oyster_mushroom", "pheasant_nest", "porcini", "ptarmigan_nest", "puffball", "raspberry_bush",
            "rock_dove_nest", "sapling_thicket", "sea_beet", "stinging_nettle_forest", "stinging_nettle_riverbank", "twigs",
            "undergrowth", "wild_carrot", "wild_einkorn", "wild_emmer", "wild_emmer_forest", "wild_pea", "wild_strawberry_bush",
            // gemstones (gen_gemstones.py)
            "gem_garnet", "gem_ruby", "gem_sapphire", "gem_topaz", "gem_jade", "gem_opal", "gem_turquoise", "gem_peridot", "gem_amber",
            "gem_clear_quartz_floor", "gem_clear_quartz_ceiling", "gem_rose_quartz_floor", "gem_rose_quartz_ceiling", "gem_citrine_floor",
            "gem_citrine_ceiling", "gem_smoky_quartz_floor", "gem_smoky_quartz_ceiling", "gem_aquamarine_floor", "gem_aquamarine_ceiling",
            "gem_fluorite_floor", "gem_fluorite_ceiling", "gem_celestine_floor", "gem_celestine_ceiling", "gem_moonstone_floor",
            "gem_moonstone_ceiling", "gem_tourmaline_floor", "gem_tourmaline_ceiling", "gem_agate", "gem_jasper", "gem_carnelian",
            "gem_chalcedony", "gem_bloodstone",
            // loose rocks of the stone families (gen_stone_rocks.py)
            "loose_limestone", "loose_slate", "loose_gneiss", "loose_soapstone", "loose_shale", "loose_conglomerate", "loose_dolomite",
            "loose_travertine", "loose_shell_bank", "loose_marble", "loose_quartzite", "loose_red_porphyry", "loose_green_porphyry",
            "loose_rapakivi_granite", "loose_coastal_basalt", "loose_pumice", "loose_scoria", "loose_serpentine", "loose_gypsum",
            "loose_alabaster", "loose_alum_shale",
            // forest floor and waterside (gen_nature_decor.py; beaver dams and springs add water and are left out)
            "fallen_log", "fallen_log_old", "snag", "snag_old", "root_plate", "root_plate_old", "mossy_stones", "mossy_stones_heath",
            "log_bridge", "driftwood", "gravel_bar", "reed_belt",
            // mountains, open ground, dry and cold land (gen_land_decor.py; waterfalls add water and lone trees are trees, both left
            // out, and so is everything switched off there)
            "bone_remains", "fire_ring", "sand_skeleton", "mammoth_tusks", "rock_arch", "dead_tree", "rock_outcrop",
            "dry_creek", "snow_patch", "anthill", "anthill_open", "ice_blocks", "root_cellar",
            // flowers and berry thickets (gen_flora.py)
            "pasque_flower", "harebell", "lady_slipper", "sea_thrift", "mountain_avens", "edelweiss", "sand_verbena", "desert_marigold",
            "great_mullein", "desert_candle", "fern_flower", "frost_lily", "blackthorn_thicket", "sea_buckthorn_thicket", "jujube_thicket",
            // wild vegetables and herbs (gen_herbs.py)
            "wild_garlic_patch", "wild_onion_patch", "wild_cabbage_patch", "wild_leek_patch", "wild_parsnip_patch", "wild_celery_patch",
            "wild_turnip_patch", "wild_lettuce_patch", "wild_thyme_patch", "wild_marjoram_patch", "yarrow_patch", "chamomile_patch",
            "water_mint_patch", "sorrel_patch", "sage_patch", "rosemary_patch", "bog_myrtle_patch", "meadowsweet_patch", "hops_patch",
            "wild_mustard_patch",
            // flora phases 2-3 (gen_flora.py; fruit trees in gen_trees.py are trees, left out)
            "foxglove", "yellow_adonis", "bog_asphodel", "marsh_marigold", "wood_anemone", "bluebells", "purple_saxifrage", "purple_loosestrife",
            "spiny_thrift", "cats_foot", "will_o_wisp_lily", "moonwort", "rosehip_thicket", "crowberry_thicket", "bearberry_thicket",
            "barberry_thicket", "caper_thicket", "red_currant_thicket", "black_currant_thicket",
            // half steps on 1-block steps (SmoothGround), last so it sees the decor; never near camps, Fire Pits or roads
            "smooth_ground");
    /** Ticks players may have spent near a chunk (its InhabitedTime) for it to count as untouched: 1 minute. */
    static final long MAX_INHABITED = 1200;
    /** How far below the ground of its column a player-made block still counts. */
    static final int DEPTH = 3;
    private static final int PER_TICK = 2, LOOK_PER_TICK = 256;
    private static final Set<Heightmap.Types> WG_HEIGHTMAPS = EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG);

    /** Index into {@link Sets} of the feature set the chunk has; absent = generated before retrogen existed. */
    public static final Supplier<AttachmentType<Integer>> MARKER = Mechanical.ATTACHMENTS.register("retrogen", () -> AttachmentType
            .builder(() -> -1)
            .serialize(Codec.INT.fieldOf("value"))
            .build());

    private static final LinkedHashSet<Long> QUEUE = new LinkedHashSet<>();
    static int updated, skipped, placedFeatures;
    private static int logged;

    public static void init() {
        GemRegen.init();
    }

    enum Result { CURRENT, NOT_READY, SKIPPED, UPDATED }

    /** Every feature set this world has seen, so a marker tells which features its chunk already has. */
    static final class Sets extends SavedData {
        private static final Codec<Sets> CODEC = Codec.STRING.listOf().listOf().xmap(l -> new Sets(new ArrayList<>(l)), s -> s.sets);
        private static final SavedDataType<Sets> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "retrogen_sets"),
                () -> new Sets(new ArrayList<>()), CODEC);
        final List<List<String>> sets;
        private int current = -1;

        private Sets(List<List<String>> sets) {
            this.sets = sets;
        }

        int current() {
            if (current < 0) {
                Set<String> now = new HashSet<>(FEATURES);
                for (int i = 0; i < sets.size() && current < 0; i++) if (new HashSet<>(sets.get(i)).equals(now)) current = i;
                if (current < 0) {
                    sets.add(FEATURES);
                    current = sets.size() - 1;
                    setDirty();
                }
            }
            return current;
        }

        List<String> get(Integer marker) {
            return marker != null && marker >= 0 && marker < sets.size() ? sets.get(marker) : List.of();
        }
    }

    static Sets sets(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(Sets.TYPE);
    }

    static void mark(LevelChunk chunk, int set) {
        chunk.setData(MARKER, set);
        chunk.markUnsaved();
    }

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        LevelChunk chunk = event.getChunk();
        if (event.isNewChunk()) {
            mark(chunk, sets(level).current()); // generated now: already has every current feature
            GemRegen.markHasGems(chunk);
            GemRegen.markHasStones(chunk);
            return;
        }
        // this chunk, and neighbours that were only waiting for it to load
        ChunkPos pos = chunk.getPos();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) QUEUE.add(ChunkPos.pack(pos.x() + dx, pos.z() + dz));
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
        int done = 0, looked = 0;
        for (Iterator<Long> it = QUEUE.iterator(); it.hasNext() && done < PER_TICK && looked++ < LOOK_PER_TICK; ) {
            long pos = it.next();
            it.remove();
            Result result = process(level, ChunkPos.getX(pos), ChunkPos.getZ(pos));
            if (result == Result.UPDATED || result == Result.SKIPPED) done++;
        }
        if (level.getGameTime() % 600 == 0 && updated + skipped != logged) {
            logged = updated + skipped;
            Mechanical.LOGGER.info("Retrogen: {} chunks updated ({} features placed), {} skipped as players' land, {} queued",
                    updated, placedFeatures, skipped, QUEUE.size());
        }
    }

    /** Brings one loaded chunk up to the current feature set, if it is stale, ready and untouched. */
    static Result process(ServerLevel level, int cx, int cz) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
        if (chunk == null) return Result.NOT_READY;
        Sets sets = sets(level);
        int current = sets.current();
        Integer marker = chunk.getExistingDataOrNull(MARKER);
        if (marker != null && marker == current) return Result.CURRENT;
        LevelChunk[] around = new LevelChunk[9];
        for (int i = 0; i < 9; i++) {
            around[i] = level.getChunkSource().getChunkNow(cx + i % 3 - 1, cz + i / 3 - 1);
            if (around[i] == null) return Result.NOT_READY; // re-queued when that neighbour loads
        }
        if (!untouched(around)) {
            mark(chunk, current);
            skipped++;
            return Result.SKIPPED;
        }
        List<String> have = sets.get(marker);
        placedFeatures += place(level, chunk, around, FEATURES.stream().filter(f -> !have.contains(f)).toList());
        mark(chunk, current);
        GemRegen.markHasGems(chunk); // /regengems must not add them again
        for (LevelChunk c : around) c.markUnsaved(); // ores write sections directly, so make sure every touched chunk saves
        updated++;
        return Result.UPDATED;
    }

    /**
     * No chunk of the 3x3 has been lived in or holds player-made blocks near the surface (no more than {@link #DEPTH} below the
     * ground of their column, so mineshafts and dungeons don't count). Sections whose palette can't hold such a block are skipped.
     */
    static boolean untouched(LevelChunk[] around) {
        for (LevelChunk c : around) if (c.getInhabitedTime() > MAX_INHABITED || !noPlayerBlocks(c)) return false;
        return true;
    }

    /** No player-made blocks near the surface of the chunk (see {@link #untouched}); NatureClock asks this too. */
    static boolean noPlayerBlocks(LevelChunk c) {
        int[] ground = new int[256];
        int floor = Integer.MAX_VALUE;
        for (int i = 0; i < 256; i++) floor = Math.min(floor, ground[i] = c.getHeight(Heightmap.Types.OCEAN_FLOOR, i & 15, i >> 4) - DEPTH);
        LevelChunkSection[] sections = c.getSections();
        for (int i = 0; i < sections.length; i++) {
            int y0 = SectionPos.sectionToBlockCoord(c.getSectionYFromSectionIndex(i));
            if (y0 + 15 < floor || sections[i].hasOnlyAir() || !sections[i].maybeHas(Retrogen::playerMade)) continue;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = Math.max(0, ground[x | z << 4] - y0); y < 16; y++)
                if (playerMade(sections[i].getBlockState(x, y, z))) return false;
        }
        return true;
    }

    /**
     * Blocks that suggest players: any block entity except the ones worldgen makes (bee nests, spawners, sculk, suspicious
     * sand...), and building blocks (planks, wooden slabs/stairs, doors, trapdoors, fences, beds, wool, glass, torches, ladders,
     * crafting tables). Villages, ruins and our own camps count too, which only means fewer chunks are touched.
     */
    static boolean playerMade(BlockState s) {
        if (s.hasBlockEntity()) return !(s.is(Blocks.BEE_NEST) || s.is(Blocks.SPAWNER) || s.is(Blocks.TRIAL_SPAWNER) || s.is(Blocks.VAULT)
                || s.is(Blocks.SUSPICIOUS_SAND) || s.is(Blocks.SUSPICIOUS_GRAVEL) || s.is(Blocks.SCULK_SENSOR) || s.is(Blocks.SCULK_SHRIEKER)
                || s.is(Blocks.SCULK_CATALYST) || s.is(Blocks.CREAKING_HEART));
        return s.is(BlockTags.PLANKS) || s.is(BlockTags.WOODEN_SLABS) || s.is(BlockTags.WOODEN_STAIRS) || s.is(BlockTags.DOORS)
                || s.is(BlockTags.TRAPDOORS) || s.is(BlockTags.FENCES) || s.is(BlockTags.BEDS) || s.is(BlockTags.WOOL) || s.is(BlockTags.WOOL_CARPETS)
                || s.is(Blocks.GLASS) || s.is(Blocks.GLASS_PANE) || s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.LADDER)
                || s.is(Blocks.CRAFTING_TABLE) || s.is(Mechanical.UNLIT_TORCH.get()) || s.is(Mechanical.UNLIT_WALL_TORCH.get());
    }

    /** Places the given features in the chunk as worldgen would (seeded per chunk); returns how many placed something. */
    static int place(ServerLevel level, LevelChunk chunk, LevelChunk[] around, List<String> features) {
        var registry = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        Set<Holder<Biome>> biomes = new HashSet<>();
        for (LevelChunkSection section : chunk.getSections()) section.getBiomes().getAll(biomes::add);
        ChunkPos pos = chunk.getPos();
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0));
        long seed = random.setDecorationSeed(level.getSeed(), pos.getMinBlockX(), pos.getMinBlockZ());
        BlockPos origin = new BlockPos(pos.getMinBlockX(), level.getMinY(), pos.getMinBlockZ());
        FeaturePlacer placer = new FeaturePlacer(level, level.getChunkSource().getGenerator());
        boolean prime = true;
        int placed = 0;
        for (String name : features) {
            int i = FEATURES.indexOf(name); // a feature's own seed index; regen-only features (GemRegen stones) get one after the list
            if (i < 0) i = FEATURES.size() + (name.hashCode() & 0xffff);
            PlacedFeature feature = registry.getValue(Identifier.fromNamespaceAndPath(Mechanical.MODID, name));
            if (feature == null || biomes.stream().noneMatch(b -> b.value().getGenerationSettings().hasFeature(feature))) continue;
            if (name.equals("smooth_ground") && SmoothGround.protectedChunk(level, pos)) continue;
            // a live chunk doesn't keep the worldgen heightmaps up to date: rebuild them before the first feature and after each change
            if (prime) for (LevelChunk c : around) Heightmap.primeHeightmaps(c, WG_HEIGHTMAPS);
            random.setFeatureSeed(seed, i, 99);
            prime = placer.place(ownBiomeCheck(feature), random, origin);
            if (prime) placed++;
        }
        return placed;
    }

    /** The same feature, its biome filter asking the biome instead of the chunk generator (a flat world's generator leaves features out). */
    private static PlacedFeature ownBiomeCheck(PlacedFeature feature) {
        return new PlacedFeature(feature.feature(), feature.placement().stream()
                .map(m -> m instanceof BiomeFilter ? (PlacementModifier) new InBiome(feature) : m).toList());
    }

    private record InBiome(PlacedFeature feature) implements PlacementFilter {
        @Override
        public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
            return context.getLevel().getBiome(pos).value().getGenerationSettings().hasFeature(feature);
        }

        @Override
        public MapCodec<? extends PlacementFilter> codec() {
            throw new UnsupportedOperationException("retrogen only, never saved");
        }
    }
}
