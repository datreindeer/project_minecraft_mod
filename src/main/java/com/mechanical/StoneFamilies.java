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
        SANDSTONE(false, "{}_slab", "{}_stairs", "{}_wall", "cut_{}", "cut_{}_slab", "smooth_{}", "smooth_{}_slab", "smooth_{}_stairs", "chiseled_{}");

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
                    List.of(List.of("conglomerate", "polished_conglomerate", "conglomerate_bricks", "chiseled_conglomerate_bricks"))));

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
            for (String pattern : s.template().patterns) {
                String name = pattern.replace("{}", s.name());
                Function<BlockBehaviour.Properties, ? extends Block> factory;
                if (name.endsWith("_slab")) factory = SlabBlock::new;
                else if (name.endsWith("_wall")) factory = p -> new WallBlock(p.forceSolidOn());
                else if (name.endsWith("_pillar")) factory = RotatedPillarBlock::new;
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
        return id.getNamespace().equals(MODID) && BLOCKS_BY_NAME.containsKey(id.getPath());
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BUILDING_BLOCKS_TAB = CREATIVE_MODE_TABS.register("building_blocks_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mechanical.building_blocks"))
            .withTabsBefore(Mechanical.MECHANICAL_TAB.getKey())
            .icon(() -> block("slate_tiles").asItem().getDefaultInstance())
            .displayItems((parameters, output) -> BLOCKS_BY_NAME.values().forEach(b -> output.accept(b.get())))
            .build());

    /** Loads the class (and so registers everything) from the Mechanical constructor. */
    public static void init() {}

    private StoneFamilies() {}
}
