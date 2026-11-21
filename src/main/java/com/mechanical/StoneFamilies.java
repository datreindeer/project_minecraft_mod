package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.CREATIVE_MODE_TABS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Stone families (docs/world-materials-plan.md 1.1): each stone copies a vanilla block family (Tuff, Deepslate, Stone,
 * Sandstone ...) block for block. The same rows as scripts/gen_stone_families.py, which writes the data (blockstates,
 * models, loot, recipes, tags, textures, worldgen). Adding a stone is one {@link #STONES} row here and one there. Every
 * family block sits in the Building Blocks creative tab; the Bronze Chisel walks each row's chains ({@link #chisel()}).
 */
public final class StoneFamilies {
    /** The vanilla families: name patterns ("{}" = the stone) of every block besides the base, in registration order. */
    public enum Template {
        TUFF(false, "{}_slab", "{}_stairs", "{}_wall", "polished_{}", "polished_{}_slab", "polished_{}_stairs", "polished_{}_wall",
                "{}_bricks", "{}_brick_slab", "{}_brick_stairs", "{}_brick_wall", "chiseled_{}", "chiseled_{}_bricks"),
        DEEPSLATE(true, "cobbled_{}", "cobbled_{}_slab", "cobbled_{}_stairs", "cobbled_{}_wall", "polished_{}", "polished_{}_slab",
                "polished_{}_stairs", "polished_{}_wall", "{}_bricks", "{}_brick_slab", "{}_brick_stairs", "{}_brick_wall", "{}_tiles",
                "{}_tile_slab", "{}_tile_stairs", "{}_tile_wall", "cracked_{}_bricks", "cracked_{}_tiles", "chiseled_{}"),
        STONE(false, "{}_slab", "{}_stairs", "smooth_{}", "smooth_{}_slab", "{}_bricks", "{}_brick_slab", "{}_brick_stairs", "{}_brick_wall",
                "cracked_{}_bricks", "chiseled_{}_bricks", "mossy_{}_bricks", "mossy_{}_brick_slab", "mossy_{}_brick_stairs", "mossy_{}_brick_wall"),
        SANDSTONE(false, "{}_slab", "{}_stairs", "{}_wall", "cut_{}", "cut_{}_slab", "smooth_{}", "smooth_{}_slab", "smooth_{}_stairs", "chiseled_{}"),
        QUARTZ(false, "{}_slab", "{}_stairs", "{}_bricks", "{}_pillar", "chiseled_{}", "smooth_{}", "smooth_{}_slab", "smooth_{}_stairs"),
        /** polished_{} turns like the base (vanilla polished basalt). */
        BASALT(true, "polished_{}", "smooth_{}"),
        // phase 3 (earth): packed mud's family (base + mud bricks), and plain bricks (base = the row's "<name>s" + slab/stairs/wall)
        MUD(false, "{}_bricks", "{}_brick_slab", "{}_brick_stairs", "{}_brick_wall"),
        BRICKS(false, "{}_slab", "{}_stairs", "{}_wall");

        /** The base block turns like a log (deepslate's axis). */
        final boolean pillarBase;
        final List<String> patterns;

        Template(boolean pillarBase, String... patterns) {
            this.pillarBase = pillarBase;
            this.patterns = List.of(patterns);
        }
    }

    /**
     * One stone. {@code existing}: a base block registered elsewhere (Limestone, IronOres), else a new one with
     * {@code color}/{@code strength}/{@code sound}. {@code hard}: needs a Bronze Pickaxe (tag written by the script).
     */
    public record Stone(String name, String base, Template template, boolean hard, MapColor color, float strength, SoundType sound,
                        DeferredBlock<Block> existing, List<List<String>> chains) {
        static Stone of(String name, String base, Template template, boolean hard, MapColor color, float strength, SoundType sound, List<List<String>> chains) {
            return new Stone(name, base, template, hard, color, strength, sound, null, chains);
        }
    }

    public static final List<Stone> STONES = List.of(
            new Stone("limestone", "limestone", Template.TUFF, false, null, 0, null, IronOres.LIMESTONE,
                    List.of(List.of("limestone", "polished_limestone", "limestone_bricks", "chiseled_limestone_bricks"))),
            Stone.of("slate", "slate", Template.DEEPSLATE, false, MapColor.COLOR_GRAY, 2.0F, SoundType.DEEPSLATE,
                    List.of(List.of("slate", "polished_slate", "slate_bricks", "slate_tiles", "chiseled_slate"), List.of("cobbled_slate", "polished_slate"))),
            Stone.of("gneiss", "gneiss", Template.STONE, true, MapColor.STONE, 3.0F, SoundType.STONE,
                    List.of(List.of("gneiss", "smooth_gneiss", "gneiss_bricks", "chiseled_gneiss_bricks"))),
            Stone.of("chalk", "chalk_block", Template.SANDSTONE, false, MapColor.QUARTZ, 0.8F, SoundType.CALCITE,
                    List.of(List.of("chalk_block", "cut_chalk", "chiseled_chalk"))),
            Stone.of("soapstone", "soapstone", Template.TUFF, false, MapColor.COLOR_LIGHT_GREEN, 1.2F, SoundType.TUFF,
                    List.of(List.of("soapstone", "polished_soapstone", "soapstone_bricks", "chiseled_soapstone_bricks"))),
            Stone.of("shale", "shale", Template.TUFF, false, MapColor.COLOR_BROWN, 1.2F, SoundType.TUFF,
                    List.of(List.of("shale", "polished_shale", "shale_bricks", "chiseled_shale_bricks"))),
            Stone.of("conglomerate", "conglomerate", Template.TUFF, false, MapColor.DIRT, 1.5F, SoundType.STONE,
                    List.of(List.of("conglomerate", "polished_conglomerate", "conglomerate_bricks", "chiseled_conglomerate_bricks"))),
            // phase 2: lime stones (they burn to Quicklime in the Clay Kiln)
            Stone.of("dolomite", "dolomite", Template.TUFF, false, MapColor.TERRACOTTA_WHITE, 1.5F, SoundType.TUFF,
                    List.of(List.of("dolomite", "polished_dolomite", "dolomite_bricks", "chiseled_dolomite_bricks"))),
            Stone.of("travertine", "travertine", Template.SANDSTONE, false, MapColor.SAND, 1.2F, SoundType.STONE,
                    List.of(List.of("travertine", "cut_travertine", "chiseled_travertine"))),
            Stone.of("shell_bank", "shell_bank", Template.SANDSTONE, false, MapColor.SAND, 0.8F, SoundType.CALCITE,
                    List.of(List.of("shell_bank", "cut_shell_bank", "chiseled_shell_bank"))),
            // phase 6: the remaining rock types
            Stone.of("marble", "marble", Template.QUARTZ, true, MapColor.QUARTZ, 2.5F, SoundType.STONE,
                    List.of(List.of("marble", "marble_bricks", "chiseled_marble"), List.of("smooth_marble", "marble_pillar"))),
            Stone.of("quartzite", "quartzite", Template.TUFF, true, MapColor.TERRACOTTA_PINK, 3.0F, SoundType.STONE,
                    List.of(List.of("quartzite", "polished_quartzite", "quartzite_bricks", "chiseled_quartzite_bricks"))),
            Stone.of("red_porphyry", "red_porphyry", Template.TUFF, true, MapColor.COLOR_RED, 3.0F, SoundType.STONE,
                    List.of(List.of("red_porphyry", "polished_red_porphyry", "red_porphyry_bricks", "chiseled_red_porphyry_bricks"))),
            Stone.of("green_porphyry", "green_porphyry", Template.TUFF, true, MapColor.TERRACOTTA_GREEN, 3.0F, SoundType.STONE,
                    List.of(List.of("green_porphyry", "polished_green_porphyry", "green_porphyry_bricks", "chiseled_green_porphyry_bricks"))),
            Stone.of("rapakivi_granite", "rapakivi_granite", Template.STONE, true, MapColor.COLOR_ORANGE, 3.0F, SoundType.STONE,
                    List.of(List.of("rapakivi_granite", "smooth_rapakivi_granite", "rapakivi_granite_bricks", "chiseled_rapakivi_granite_bricks"))),
            Stone.of("coastal_basalt", "coastal_basalt", Template.BASALT, true, MapColor.COLOR_BLACK, 2.5F, SoundType.BASALT,
                    List.of(List.of("coastal_basalt", "polished_coastal_basalt"))),
            Stone.of("pumice", "pumice", Template.TUFF, false, MapColor.COLOR_LIGHT_GRAY, 0.8F, SoundType.TUFF,
                    List.of(List.of("pumice", "polished_pumice", "pumice_bricks", "chiseled_pumice_bricks"))),
            Stone.of("scoria", "scoria", Template.BASALT, false, MapColor.TERRACOTTA_BLACK, 1.0F, SoundType.BASALT,
                    List.of(List.of("scoria", "polished_scoria"))),
            Stone.of("serpentine", "serpentine", Template.TUFF, true, MapColor.COLOR_GREEN, 2.5F, SoundType.STONE,
                    List.of(List.of("serpentine", "polished_serpentine", "serpentine_bricks", "chiseled_serpentine_bricks"))),
            Stone.of("jasper", "jasper_stone", Template.TUFF, false, MapColor.TERRACOTTA_RED, 1.2F, SoundType.TUFF,
                    List.of(List.of("jasper_stone", "polished_jasper", "jasper_bricks", "chiseled_jasper_bricks"))),
            Stone.of("gypsum", "gypsum", Template.SANDSTONE, false, MapColor.SAND, 0.8F, SoundType.CALCITE,
                    List.of(List.of("gypsum", "cut_gypsum", "chiseled_gypsum"))),
            Stone.of("alabaster", "alabaster", Template.QUARTZ, true, MapColor.TERRACOTTA_WHITE, 2.0F, SoundType.CALCITE,
                    List.of(List.of("alabaster", "alabaster_bricks", "chiseled_alabaster"), List.of("smooth_alabaster", "alabaster_pillar"))),
            Stone.of("alum_shale", "alum_shale", Template.TUFF, false, MapColor.DEEPSLATE, 1.2F, SoundType.TUFF,
                    List.of(List.of("alum_shale", "polished_alum_shale", "alum_shale_bricks", "chiseled_alum_shale_bricks"))),
            // phase 3 (earth; EarthBlocks): no chisel chains
            Stone.of("peat_brick", "peat_bricks", Template.BRICKS, false, MapColor.COLOR_BROWN, 1.5F, SoundType.MUD_BRICKS, List.of()),
            Stone.of("adobe", "mudstone", Template.MUD, false, MapColor.TERRACOTTA_ORANGE, 1.0F, SoundType.PACKED_MUD, List.of()),
            new Stone("loess", "loess", Template.MUD, false, null, 0, null, EarthBlocks.LOESS, List.of()),
            Stone.of("rammed_earth", "rammed_earth", Template.BRICKS, false, MapColor.DIRT, 1.5F, SoundType.PACKED_MUD, List.of()),
            Stone.of("kaolin_tile", "kaolin_tiles", Template.BRICKS, false, MapColor.SNOW, 2.0F, SoundType.STONE, List.of()),
            Stone.of("red_ochre", "red_ochre_block", Template.MUD, false, MapColor.TERRACOTTA_RED, 1.0F, SoundType.PACKED_MUD, List.of()),
            Stone.of("yellow_ochre", "yellow_ochre_block", Template.MUD, false, MapColor.TERRACOTTA_YELLOW, 1.0F, SoundType.PACKED_MUD, List.of()),
            // phase 10 (Masonry): Mosaic Tiles of four stones and Fired Roof Tiles, plain and coloured with pigment
            Stone.of("warm_mosaic_tile", "warm_mosaic_tiles", Template.BRICKS, false, MapColor.TERRACOTTA_ORANGE, 1.5F, SoundType.STONE, List.of()),
            Stone.of("cool_mosaic_tile", "cool_mosaic_tiles", Template.BRICKS, false, MapColor.WARPED_NYLIUM, 1.5F, SoundType.STONE, List.of()),
            Stone.of("checkered_mosaic_tile", "checkered_mosaic_tiles", Template.BRICKS, false, MapColor.QUARTZ, 1.5F, SoundType.STONE, List.of()),
            Stone.of("roof_tile", "roof_tiles", Template.BRICKS, false, MapColor.TERRACOTTA_RED, 2.0F, SoundType.DECORATED_POT, List.of()),
            Stone.of("red_roof_tile", "red_roof_tiles", Template.BRICKS, false, MapColor.COLOR_RED, 2.0F, SoundType.DECORATED_POT, List.of()),
            Stone.of("black_roof_tile", "black_roof_tiles", Template.BRICKS, false, MapColor.COLOR_BLACK, 2.0F, SoundType.DECORATED_POT, List.of()),
            Stone.of("yellow_roof_tile", "yellow_roof_tiles", Template.BRICKS, false, MapColor.TERRACOTTA_YELLOW, 2.0F, SoundType.DECORATED_POT, List.of()),
            Stone.of("white_roof_tile", "white_roof_tiles", Template.BRICKS, false, MapColor.TERRACOTTA_WHITE, 2.0F, SoundType.DECORATED_POT, List.of()));

    /**
     * Phase 10 (weathering, {@link Masonry}): every rock family with bricks also gets these where its template lacks them
     * (scripts/gen_stone_families.py WEATHERED), and its bricks block random-ticks to age into them.
     */
    public static final List<String> WEATHERED = List.of("cracked_{}_bricks", "mossy_{}_bricks", "mossy_{}_brick_slab", "mossy_{}_brick_stairs", "mossy_{}_brick_wall");
    private static final java.util.Set<Template> WEATHERS = java.util.EnumSet.of(Template.TUFF, Template.DEEPSLATE, Template.STONE, Template.QUARTZ);

    /** The template's patterns, plus the weathered ones it lacks for a rock family with bricks. */
    static List<String> patterns(Stone s) {
        if (!WEATHERS.contains(s.template())) return s.template().patterns;
        List<String> all = new java.util.ArrayList<>(s.template().patterns);
        WEATHERED.stream().filter(p -> !all.contains(p)).forEach(all::add);
        return all;
    }

    /** Every stone family block by id path, base blocks included. */
    public static final Map<String, DeferredBlock<? extends Block>> BLOCKS_BY_NAME = new LinkedHashMap<>();

    static {
        for (Stone s : STONES) {
            DeferredBlock<Block> base = s.existing();
            if (base == null) {
                base = register(s.base(), s.template().pillarBase ? RotatedPillarBlock::new : Block::new, () -> BlockBehaviour.Properties.of()
                        .mapColor(s.color()).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(s.strength(), 6.0F).sound(s.sound()));
            }
            BLOCKS_BY_NAME.put(s.base(), base);
            DeferredBlock<Block> props = base;
            for (String pattern : patterns(s)) {
                String name = pattern.replace("{}", s.name());
                Function<BlockBehaviour.Properties, ? extends Block> factory;
                if (pattern.equals("{}_bricks") && WEATHERS.contains(s.template())) {
                    register(name, Masonry.BricksBlock::new, () -> BlockBehaviour.Properties.ofLegacyCopy(props.get()).randomTicks());
                    continue;
                }
                if (name.endsWith("_slab")) factory = SlabBlock::new;
                else if (name.endsWith("_wall")) factory = p -> new WallBlock(p.forceSolidOn());
                else if (name.endsWith("_pillar") || s.template() == Template.BASALT && name.startsWith("polished_")) factory = RotatedPillarBlock::new;
                else if (name.endsWith("_stairs")) {
                    String of = stairsOf(s, name.substring(0, name.length() - "_stairs".length()));
                    factory = p -> new StairBlock(BLOCKS_BY_NAME.get(of).get().defaultBlockState(), p);
                } else factory = Block::new;
                register(name, factory, () -> BlockBehaviour.Properties.ofLegacyCopy(props.get()));
            }
        }
    }

    /** "tuff_brick" -> tuff_bricks, "polished_tuff" -> itself, "chalk" -> the base (chalk_block). */
    private static String stairsOf(Stone s, String block) {
        if (block.equals(s.name())) return s.base();
        return s.template().patterns.contains(block.replace(s.name(), "{}")) ? block : block + "s";
    }

    private static <B extends Block> DeferredBlock<B> register(String name, Function<BlockBehaviour.Properties, B> factory,
                                                               java.util.function.Supplier<BlockBehaviour.Properties> props) {
        DeferredBlock<B> block = BLOCKS.registerBlock(name, factory, props);
        ITEMS.registerSimpleBlockItem(name, block);
        BLOCKS_BY_NAME.put(name, block);
        return block;
    }

    public static Block block(String name) {
        return BLOCKS_BY_NAME.get(name).get();
    }

    /** Bronze Chisel steps from every row's chains (merged into BronzeTools.ChiselItem.carved). */
    public static Map<Block, Block> chisel() {
        Map<Block, Block> map = new HashMap<>();
        for (Stone s : STONES)
            for (List<String> chain : s.chains())
                for (int i = 0; i + 1 < chain.size(); i++) map.put(block(chain.get(i)), block(chain.get(i + 1)));
        return map;
    }

    public static boolean isFamilyItem(Identifier id) {
        return id.getNamespace().equals(MODID) && (BLOCKS_BY_NAME.containsKey(id.getPath()) || EarthBlocks.isBuilding(id.getPath()));
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BUILDING_BLOCKS_TAB = CREATIVE_MODE_TABS.register("building_blocks_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mechanical.building_blocks"))
            .withTabsBefore(Mechanical.MECHANICAL_TAB.getKey())
            .icon(() -> block("slate_tiles").asItem().getDefaultInstance())
            .displayItems((parameters, output) -> {
                BLOCKS_BY_NAME.values().forEach(b -> output.accept(b.get()));
                EarthBlocks.BUILDING.forEach(b -> output.accept(b.get()));
            })
            .build());

    /** Loads the class (and so registers everything) from the Mechanical constructor. */
    public static void init() {}

    private StoneFamilies() {}
}
