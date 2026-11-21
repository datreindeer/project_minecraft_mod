package com.mechanical;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.mechanical.mixin.FireBlockInvoker;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HangingRootsBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Bog and earth (docs/world-materials-plan.md phase 3), the blocks that aren't stone families: Peat (fuel), Turf, Bog Oak
 * (a wood set without tree: buried logs, worldgen/BogOakLogs), Reed (grows like sugar cane) and Reed Thatch, Loess, Wattle,
 * Wattle & Daub, Kaolin; Beach Pebbles and Fieldstone + Dry Stone Wall (phases 4 and 7). The families built on them (Peat Bricks, Adobe/Mudstone, Loess Bricks, Rammed Earth, Kaolin Tiles,
 * Ochre blocks) are {@link StoneFamilies} rows. Also the Glowworm of the Glowworm Caves (scripts/gen_caves.py). Data, textures and worldgen: scripts/gen_earth.py.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class EarthBlocks {
    /** Shown in the Building Blocks tab (StoneFamilies) instead of the main one. */
    public static final List<DeferredBlock<? extends Block>> BUILDING = new ArrayList<>();
    /** Burn time of one Peat on a campfire: twice a firewood's. */
    public static final int PEAT_CAMPFIRE_TICKS = 2 * Campfires.FIREWOOD_TICKS;
    /** A Kaolin block fires into Kaolin Tiles at this heat in the Clay Kiln: charcoal with a bellows. */
    public static final int KAOLIN_TEMP = 1000;

    private static Supplier<Properties> soil(MapColor color, float strength, SoundType sound) {
        return () -> Properties.of().mapColor(color).strength(strength).sound(sound);
    }

    public static final DeferredBlock<Block> PEAT = block("peat", Block::new, soil(MapColor.COLOR_BROWN, 0.6F, SoundType.ROOTED_DIRT));
    public static final DeferredBlock<Block> LOESS = Mechanical.BLOCKS.registerBlock("loess", Block::new, soil(MapColor.SAND, 0.5F, SoundType.GRAVEL)); // tab: its family row
    public static final DeferredBlock<Block> KAOLIN = block("kaolin", Block::new, () -> Properties.of().mapColor(MapColor.SNOW)
            .instrument(NoteBlockInstrument.FLUTE).strength(0.6F).sound(SoundType.GRAVEL));
    public static final DeferredItem<Item> KAOLIN_CLAY = Mechanical.ITEMS.registerSimpleItem("kaolin_clay");
    // dried on the Drying Rack (DryingRackBlockEntity.TABLE): adobe from clay, peat cut from the block, reed for thatch
    public static final DeferredItem<Item> WET_ADOBE_BRICK = Mechanical.ITEMS.registerSimpleItem("wet_adobe_brick");
    public static final DeferredItem<Item> ADOBE_BRICK = Mechanical.ITEMS.registerSimpleItem("adobe_brick");
    public static final DeferredItem<Item> CUT_PEAT = Mechanical.ITEMS.registerSimpleItem("cut_peat");
    public static final DeferredItem<Item> DRIED_PEAT = Mechanical.ITEMS.registerSimpleItem("dried_peat");
    public static final DeferredItem<Item> DRIED_REED = Mechanical.ITEMS.registerSimpleItem("dried_reed");

    public static final DeferredBlock<Block> TURF = block("turf", Block::new, soil(MapColor.GRASS, 0.6F, SoundType.GRASS));
    public static final DeferredBlock<SlabBlock> TURF_SLAB = block("turf_slab", SlabBlock::new, () -> Properties.ofLegacyCopy(TURF.get()));
    public static final DeferredBlock<StairBlock> TURF_STAIRS = block("turf_stairs", p -> new StairBlock(TURF.get().defaultBlockState(), p),
            () -> Properties.ofLegacyCopy(TURF.get()));

    public static final DeferredBlock<SugarCaneBlock> REED = block("reed", SugarCaneBlock::new, () -> Properties.of().mapColor(MapColor.PLANT)
            .noCollision().randomTicks().instabreak().sound(SoundType.GRASS).ignitedByLava().pushReaction(PushReaction.POPPED));
    private static final Supplier<Properties> THATCH = () -> Properties.of().mapColor(MapColor.COLOR_YELLOW).instrument(NoteBlockInstrument.BANJO)
            .strength(0.5F).sound(SoundType.GRASS).ignitedByLava();
    public static final DeferredBlock<Block> REED_THATCH = block("reed_thatch", Block::new, THATCH);
    public static final DeferredBlock<SlabBlock> REED_THATCH_SLAB = block("reed_thatch_slab", SlabBlock::new, THATCH);
    public static final DeferredBlock<StairBlock> REED_THATCH_STAIRS = block("reed_thatch_stairs", p -> new StairBlock(REED_THATCH.get().defaultBlockState(), p), THATCH);
    /** Same slopes as the Bark Roof. */
    public static final DeferredBlock<BarkRoofBlock> REED_THATCH_ROOF = block("reed_thatch_roof", BarkRoofBlock::new, () -> THATCH.get().noOcclusion());

    public static final DeferredBlock<IronBarsBlock> WATTLE = block("wattle", IronBarsBlock::new, () -> Properties.of().mapColor(MapColor.WOOD)
            .strength(1.0F).sound(SoundType.WOOD).ignitedByLava().noOcclusion());
    public static final DeferredBlock<Block> WATTLE_AND_DAUB = block("wattle_and_daub", Block::new, soil(MapColor.TERRACOTTA_WHITE, 1.5F, SoundType.PACKED_MUD));
    public static final DeferredBlock<Block> FRAMED_WATTLE_AND_DAUB = block("framed_wattle_and_daub", Block::new, soil(MapColor.TERRACOTTA_WHITE, 1.5F, SoundType.PACKED_MUD));

    // phases 4 and 7 (surface biomes): Beach Pebbles on stony beaches, Fieldstone lying loose on moraines and shingle
    public static final DeferredBlock<Block> BEACH_PEBBLES = block("beach_pebbles", Block::new, soil(MapColor.STONE, 0.6F, SoundType.GRAVEL));
    public static final DeferredBlock<SlabBlock> BEACH_PEBBLE_SLAB = block("beach_pebble_slab", SlabBlock::new, () -> Properties.ofLegacyCopy(BEACH_PEBBLES.get()));
    private static final Supplier<Properties> FIELDSTONE_LIKE = () -> Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
            .requiresCorrectToolForDrops().strength(1.5F, 6.0F).sound(SoundType.STONE);
    public static final DeferredBlock<Block> FIELDSTONE = block("fieldstone", Block::new, FIELDSTONE_LIKE);
    public static final DeferredBlock<SlabBlock> FIELDSTONE_SLAB = block("fieldstone_slab", SlabBlock::new, FIELDSTONE_LIKE);
    public static final DeferredBlock<StairBlock> FIELDSTONE_STAIRS = block("fieldstone_stairs", p -> new StairBlock(FIELDSTONE.get().defaultBlockState(), p), FIELDSTONE_LIKE);
    /** Laid without mortar from Fieldstone. */
    public static final DeferredBlock<WallBlock> DRY_STONE_WALL = block("dry_stone_wall", WallBlock::new, () -> FIELDSTONE_LIKE.get().forceSolidOn());
    /** Glowworm Caves (docs/caves.md): threads hanging from a cave ceiling, dotted with faint blue light. Drops nothing. */
    public static final int GLOWWORM_LIGHT = 6;
    public static final DeferredBlock<HangingRootsBlock> GLOWWORM = Mechanical.BLOCKS.registerBlock("glowworm", HangingRootsBlock::new,
            () -> Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).noCollision().instabreak().sound(SoundType.HANGING_ROOTS)
                    .lightLevel(s -> GLOWWORM_LIGHT).pushReaction(PushReaction.POPPED));

    // Bog Oak: a wood set like Trees' but with no leaves, sapling or tree; black oak preserved in peat
    private static final MapColor BOG_PLANKS = MapColor.TERRACOTTA_BLACK, BOG_BARK = MapColor.COLOR_BLACK;
    private static final Supplier<Properties> BOG_PLANK_LIKE = () -> Properties.of().mapColor(BOG_PLANKS).instrument(NoteBlockInstrument.BASS).ignitedByLava();
    public static final DeferredBlock<RotatedPillarBlock> BOG_OAK_LOG = wood("bog_oak_log", RotatedPillarBlock::new, () -> Trees.logProperties(BOG_PLANKS, BOG_BARK));
    public static final DeferredBlock<RotatedPillarBlock> BOG_OAK_WOOD = wood("bog_oak_wood", RotatedPillarBlock::new, () -> Trees.logProperties(BOG_BARK, BOG_BARK));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_BOG_OAK_LOG = wood("stripped_bog_oak_log", RotatedPillarBlock::new, () -> Trees.logProperties(BOG_PLANKS, BOG_PLANKS));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_BOG_OAK_WOOD = wood("stripped_bog_oak_wood", RotatedPillarBlock::new, () -> Trees.logProperties(BOG_PLANKS, BOG_PLANKS));
    public static final DeferredBlock<Block> BOG_OAK_PLANKS = wood("bog_oak_planks", Block::new, () -> BOG_PLANK_LIKE.get().strength(2.0F, 3.0F).sound(SoundType.WOOD));
    public static final DeferredBlock<SlabBlock> BOG_OAK_SLAB = wood("bog_oak_slab", SlabBlock::new, () -> Properties.ofLegacyCopy(BOG_OAK_PLANKS.get()));
    public static final DeferredBlock<StairBlock> BOG_OAK_STAIRS = wood("bog_oak_stairs", p -> new StairBlock(BOG_OAK_PLANKS.get().defaultBlockState(), p),
            () -> Properties.ofLegacyCopy(BOG_OAK_PLANKS.get()));
    public static final DeferredBlock<FenceBlock> BOG_OAK_FENCE = wood("bog_oak_fence", FenceBlock::new, () -> BOG_PLANK_LIKE.get().strength(2.0F, 3.0F).sound(SoundType.WOOD));
    public static final DeferredBlock<FenceGateBlock> BOG_OAK_FENCE_GATE = wood("bog_oak_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p),
            () -> BOG_PLANK_LIKE.get().forceSolidOn().strength(2.0F, 3.0F));
    public static final DeferredBlock<DoorBlock> BOG_OAK_DOOR = wood("bog_oak_door", p -> new DoorBlock(BlockSetType.OAK, p),
            () -> BOG_PLANK_LIKE.get().strength(3.0F).noOcclusion().pushReaction(PushReaction.POPPED));
    public static final DeferredBlock<TrapDoorBlock> BOG_OAK_TRAPDOOR = wood("bog_oak_trapdoor", p -> new TrapDoorBlock(BlockSetType.OAK, p),
            () -> BOG_PLANK_LIKE.get().strength(3.0F).noOcclusion().isValidSpawn((s, l, pos, e) -> false));
    public static final DeferredBlock<ButtonBlock> BOG_OAK_BUTTON = wood("bog_oak_button", p -> new ButtonBlock(BlockSetType.OAK, 30, p),
            () -> Properties.of().noCollision().strength(0.5F).pushReaction(PushReaction.POPPED));
    public static final DeferredBlock<PressurePlateBlock> BOG_OAK_PRESSURE_PLATE = wood("bog_oak_pressure_plate", p -> new PressurePlateBlock(BlockSetType.OAK, p),
            () -> BOG_PLANK_LIKE.get().forceSolidOn().noCollision().strength(0.5F).pushReaction(PushReaction.POPPED));

    private static <B extends Block> DeferredBlock<B> block(String name, Function<Properties, B> factory, Supplier<Properties> properties) {
        DeferredBlock<B> block = Mechanical.BLOCKS.registerBlock(name, factory, properties); // item: Mechanical.registerMissingBlockItems
        BUILDING.add(block);
        return block;
    }

    /** Through Trees' helper, so the door gets its two-high item. */
    private static <B extends Block> DeferredBlock<B> wood(String name, Function<Properties, B> factory, Supplier<Properties> properties) {
        DeferredBlock<B> block = Trees.block(name, factory, properties);
        BUILDING.add(block);
        return block;
    }

    public static boolean isBuilding(String path) {
        return BUILDING.stream().anyMatch(b -> b.getId().getPath().equals(path));
    }

    public static boolean isPeat(ItemStack stack) {
        return stack.is(PEAT.get().asItem());
    }

    /** Campfire burn ticks of Peat (a block) or Dried Peat (a quarter block, dried: half a block's burn each), else 0. */
    public static int peatCampfireTicks(ItemStack stack) {
        return isPeat(stack) ? PEAT_CAMPFIRE_TICKS : stack.is(DRIED_PEAT.get()) ? Campfires.FIREWOOD_TICKS : 0;
    }

    /** Fire odds: thatch and reed like hay, wattle and bog oak like wood. */
    @SubscribeEvent
    static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var fire = (FireBlockInvoker) Blocks.FIRE;
            for (var b : List.of(REED_THATCH, REED_THATCH_SLAB, REED_THATCH_STAIRS, REED_THATCH_ROOF, REED)) fire.mechanical$setFlammable(b.get(), 60, 20);
            for (var b : List.of(BOG_OAK_PLANKS, BOG_OAK_SLAB, BOG_OAK_STAIRS, BOG_OAK_FENCE, BOG_OAK_FENCE_GATE, WATTLE)) fire.mechanical$setFlammable(b.get(), 5, 20);
            for (var b : List.of(BOG_OAK_LOG, BOG_OAK_WOOD, STRIPPED_BOG_OAK_LOG, STRIPPED_BOG_OAK_WOOD)) fire.mechanical$setFlammable(b.get(), 5, 5);
        });
    }

    /** Loads the class (and so registers everything); call from the mod constructor before StoneFamilies. */
    public static void init() {}

    private EarthBlocks() {}
}
