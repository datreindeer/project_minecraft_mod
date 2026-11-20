package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;

import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Gemstones: ore gems in full ore blocks, crystal gems growing in caves (small / medium / large), pebble gems lying on
 * cave floors. Only blocks and items here (block items come from Mechanical.registerMissingBlockItems); everything else,
 * worldgen included, is data from scripts/gen_gemstones.py, whose table (scripts/gem_table.py) must list the same names.
 */
public final class Gemstones {
    /** Ore gems and the rock their ore sits in. */
    static final String[][] ORES = {{"garnet", "stone", "deepslate"}, {"ruby", "deepslate"}, {"sapphire", "stone", "deepslate"},
            {"topaz", "stone"}, {"jade", "stone"}, {"opal", "sandstone"}, {"turquoise", "stone"}, {"peridot", "deepslate"}, {"amber", "stone"}};
    static final String[] CRYSTALS = {"clear_quartz", "rose_quartz", "citrine", "smoky_quartz", "aquamarine", "fluorite", "celestine",
            "moonstone", "tourmaline"};
    static final String[] PEBBLES = {"agate", "jasper", "carnelian", "chalcedony", "bloodstone"};

    static {
        for (String[] ore : ORES) {
            ITEMS.registerSimpleItem(ore[0]);
            for (int i = 1; i < ore.length; i++) {
                String host = ore[i];
                BLOCKS.registerSimpleBlock(host.equals("stone") ? ore[0] + "_ore" : host + "_" + ore[0] + "_ore", p -> switch (host) {
                    case "deepslate" -> p.mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                            .strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE);
                    case "sandstone" -> p.mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                            .strength(1.6F);
                    default -> p.mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                            .strength(3.0F, 3.0F);
                });
            }
        }
        for (String gem : CRYSTALS) {
            ITEMS.registerSimpleItem(gem);
            boolean moon = gem.equals("moonstone"); // moonstone glows brighter than the others' faint glimmer
            crystal("small_" + gem + "_crystal", 3, 8, moon ? 3 : 1, SoundType.SMALL_AMETHYST_BUD);
            crystal("medium_" + gem + "_crystal", 5, 10, moon ? 5 : 2, SoundType.LARGE_AMETHYST_BUD);
            crystal("large_" + gem + "_crystal", 7, 10, moon ? 7 : 4, SoundType.AMETHYST_CLUSTER);
        }
        for (String gem : PEBBLES) {
            ITEMS.registerSimpleItem(gem);
            BLOCKS.registerBlock("loose_" + gem, RockBlock::new, p -> p
                    .mapColor(MapColor.STONE).strength(1.5F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.POPPED));
        }
    }

    private static void crystal(String id, float height, float width, int light, SoundType sound) {
        BLOCKS.registerBlock(id, p -> new AmethystClusterBlock(height, width, p), p -> p
                .mapColor(MapColor.COLOR_PURPLE).forceSolidOn().noOcclusion().strength(1.5F).sound(sound)
                .lightLevel(s -> light).requiresCorrectToolForDrops().pushReaction(PushReaction.POPPED));
    }

    public static void init() {}

    private Gemstones() {}
}
