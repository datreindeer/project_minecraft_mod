package com.mechanical;

import com.mechanical.skills.Skill;
import com.mechanical.skills.Skills;
import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * World-materials plan phase 10. <b>Weathering:</b> the bricks of every rock family ({@link StoneFamilies#WEATHERED})
 * random-tick and slowly age: open to the sky with water, moss or rain on them they turn Mossy, through frost they turn
 * Cracked; under a roof nothing happens. A Pumice Chunk scrubs moss off, Lime Mortar repoints cracks (vanilla's mossy and
 * cracked blocks too). <b>Mosaic Tiles</b> and <b>Fired Roof Tiles</b> are {@link StoneFamilies} rows; here are the roof
 * tile items (fired in the Clay Kiln, {@link KilnBlockEntity#RECIPES}) and the sloped Tiled Roofs. Data and textures:
 * scripts/gen_masonry.py (and the weathered rows in gen_stone_families.py).
 */
@EventBusSubscriber(modid = MODID)
public final class Masonry {
    /** Random ticks one block gets per game day at the default randomTickSpeed (3 picks per 16^3 section per tick): ~53. */
    static final float TICKS_PER_DAY = Calendar.DAY * 3F / 4096;
    /** Wet and open: Mossy after a year on average (the mod's year, 360 one-hour days). */
    public static final float MOSS_CHANCE = 1F / (TICKS_PER_DAY * Calendar.YEAR_DAYS);
    /** Per random tick while freezing: Cracked after about two seasons of frost, i.e. a few winters in a temperate land. */
    public static final float CRACK_CHANCE = 1F / (TICKS_PER_DAY * 2 * Calendar.SEASON_DAYS);
    /** Kiln heat for roof tiles: hotter than a brick (600), so a bellows or charcoal (Bronze Age). */
    public static final int ROOF_TILE_TEMP = 800;
    public static final List<String> ROOF_COLORS = List.of("", "red_", "black_", "yellow_", "white_");

    public static final DeferredItem<Item> UNFIRED_ROOF_TILE = Mechanical.ITEMS.registerSimpleItem("unfired_roof_tile");
    public static final DeferredItem<Item> FIRED_ROOF_TILE = Mechanical.ITEMS.registerSimpleItem("fired_roof_tile");
    /** Sloped like the Bark Roof, one per colour: tiled_roof, red_tiled_roof ... Stone: never burns. */
    public static final List<DeferredBlock<BarkRoofBlock>> TILED_ROOFS = new ArrayList<>();

    static {
        for (String c : ROOF_COLORS) {
            DeferredBlock<BarkRoofBlock> roof = Mechanical.BLOCKS.registerBlock(c + "tiled_roof", BarkRoofBlock::new, () -> BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_RED).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F).sound(SoundType.DECORATED_POT).noOcclusion()); // item: Mechanical.registerMissingBlockItems
            TILED_ROOFS.add(roof);
            EarthBlocks.BUILDING.add(roof);
        }
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    /** A rock family's bricks: ages on random ticks ({@link #age}). */
    public static class BricksBlock extends Block {
        public BricksBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            float roll = random.nextFloat();
            if (roll >= Math.max(MOSS_CHANCE, CRACK_CHANCE)) return; // nearly every tick ends here, before any world lookup
            age(level, pos, state, roll, level.dimension() == Level.OVERWORLD && Temperature.ambient(level, pos) < Temperature.FREEZING_BELOW);
        }
    }

    /** mossy_x / cracked_x <-> x for every block id that has both (ours and vanilla's), built once from the registry. */
    private static final class Maps {
        static final Map<Block, Block> CLEAN = new HashMap<>(), MOSSY = new HashMap<>(), CRACKED = new HashMap<>();
        static final Set<Block> MOSSY_BLOCKS = new HashSet<>();

        static {
            for (Block b : BuiltInRegistries.BLOCK) {
                Identifier id = BuiltInRegistries.BLOCK.getKey(b);
                for (String prefix : new String[] {"mossy_", "cracked_"}) {
                    if (!id.getPath().startsWith(prefix)) continue;
                    Identifier plain = id.withPath(id.getPath().substring(prefix.length()));
                    if (!BuiltInRegistries.BLOCK.containsKey(plain)) continue;
                    Block clean = BuiltInRegistries.BLOCK.getValue(plain);
                    CLEAN.put(b, clean);
                    (prefix.equals("mossy_") ? MOSSY : CRACKED).put(clean, b);
                    if (prefix.equals("mossy_")) MOSSY_BLOCKS.add(b);
                }
            }
        }
    }

    /** What a Pumice Chunk scrubs this block back to (mossy -> clean), or null. */
    public static Block scrubbed(Block block) {
        return Maps.MOSSY_BLOCKS.contains(block) ? Maps.CLEAN.get(block) : null;
    }

    /** What Lime Mortar repoints this block back to (cracked -> whole), or null. */
    public static Block repointed(Block block) {
        Block clean = Maps.CLEAN.get(block);
        return clean != null && !Maps.MOSSY_BLOCKS.contains(block) ? clean : null;
    }

    /** Open to the weather: the top or a side touches open air with nothing above it (heightmap, so it holds the moment a roof goes on). */
    static boolean exposed(Level level, BlockPos pos) {
        for (Direction d : new Direction[] {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos n = pos.relative(d);
            if (!level.getBlockState(n).isSolid() && level.getFluidState(n).isEmpty()
                    && n.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, n.getX(), n.getZ())) return true;
        }
        return false;
    }

    /** Rain on it, or water, moss, vines or mossy stone touching it (the 3x3x3 around). */
    static boolean wet(Level level, BlockPos pos) {
        if (level.isRainingAt(pos.above())) return true;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            BlockState s = level.getBlockState(p);
            if (s.getFluidState().is(FluidTags.WATER) || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.MOSS_CARPET) || s.is(Blocks.VINE)
                    || Maps.MOSSY_BLOCKS.contains(s.getBlock())) return true;
        }
        return false;
    }

    /**
     * One ageing step for the bricks at {@code pos}, given a random roll (0..1) and whether it is freezing there: indoors
     * nothing; frost cracks at {@link #CRACK_CHANCE}, else wet moss grows at {@link #MOSS_CHANCE}. True when it changed.
     */
    public static boolean age(ServerLevel level, BlockPos pos, BlockState state, float roll, boolean frost) {
        if (!exposed(level, pos)) return false;
        Block to = frost && roll < CRACK_CHANCE ? Maps.CRACKED.get(state.getBlock())
                : roll < MOSS_CHANCE && wet(level, pos) ? Maps.MOSSY.get(state.getBlock()) : null;
        if (to == null) return false;
        level.setBlock(pos, to.withPropertiesOf(state), Block.UPDATE_ALL);
        return true;
    }

    /** Lime Mortar on a cracked block: repointed, one mortar used. */
    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(IronOres.LIME_MORTAR.get())) return;
        if (repoint(event.getLevel(), event.getPos(), event.getEntity(), event.getItemStack())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    static boolean repoint(Level level, BlockPos pos, Player player, ItemStack mortar) {
        BlockState state = level.getBlockState(pos);
        Block to = repointed(state.getBlock());
        if (to == null) return false;
        if (level instanceof ServerLevel server) {
            server.setBlock(pos, to.withPropertiesOf(state), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.MUD_BRICKS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (player == null || !player.getAbilities().instabuild) mortar.shrink(1);
            if (player instanceof ServerPlayer p) {
                Kettle.award(p, Identifier.fromNamespaceAndPath(MODID, "iron_age/repointed"));
                Skills.addXp(p, Skill.MASONRY, 5);
            }
        }
        return true;
    }

    /** Pumice Chunk on a mossy block (StoneCrafts.PumiceItem): the "Weathered" advancement. */
    static void scrubbedBy(Player player) {
        if (!(player instanceof ServerPlayer p)) return;
        Kettle.award(p, Identifier.fromNamespaceAndPath(MODID, "bronze_age/weathered"));
        Skills.addXp(p, Skill.MASONRY, 5);
    }

    // ---------------------------------------------------------------- game test

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "masonry"));

    static {
        TEST_FUNCTIONS.register("masonry", () -> Masonry::test);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "masonry"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "masonry"),
                new FunctionGameTestInstance(TEST, TestArea.of(env, 40, true)));
    }

    private static void test(GameTestHelper h) {
        try {
            run(h);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    private static ItemStack craft(GameTestHelper h, int w, int ht, ItemStack... items) {
        CraftingInput in = CraftingInput.of(w, ht, List.of(items));
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, in, h.getLevel()).map(r -> r.value().assemble(in)).orElse(ItemStack.EMPTY);
    }

    private static ItemStack stack(String id) {
        return new ItemStack(StoneFamilies.block(id));
    }

    private static void run(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        Block bricks = StoneFamilies.block("limestone_bricks"), mossy = StoneFamilies.block("mossy_limestone_bricks"),
                cracked = StoneFamilies.block("cracked_limestone_bricks");
        BlockPos at = new BlockPos(2, 1, 2), water = at.east(), abs = h.absolutePos(at);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);

        // 1. every rock family's bricks random-tick and have both aged forms; earth bricks don't
        for (StoneFamilies.Stone s : StoneFamilies.STONES) {
            if (!StoneFamilies.patterns(s).contains("mossy_{}_bricks")) continue;
            Block b = StoneFamilies.block(s.name() + "_bricks");
            h.assertTrue(b.defaultBlockState().isRandomlyTicking() && Maps.MOSSY.get(b) != null && Maps.CRACKED.get(b) != null, s.name() + " bricks weather");
        }
        h.assertTrue(!StoneFamilies.block("adobe_bricks").defaultBlockState().isRandomlyTicking(), "adobe bricks don't weather");

        // 2. open sky + water beside: moss (only when the roll is under the chance); dry and mild: nothing
        h.setBlock(at, bricks);
        h.assertTrue(!age(level, abs, h.getBlockState(at), 0, false), "dry, mild bricks stay clean");
        h.setBlock(water, Blocks.WATER);
        h.assertTrue(!age(level, abs, h.getBlockState(at), MOSS_CHANCE * 1.01F, false), "a roll over the chance does nothing");
        h.assertTrue(age(level, abs, h.getBlockState(at), 0, false) && h.getBlockState(at).is(mossy), "wet, open bricks turn mossy");

        // 3. frost cracks them (water or not)
        h.setBlock(water, Blocks.AIR);
        h.setBlock(at, bricks);
        h.assertTrue(age(level, abs, h.getBlockState(at), 0, true) && h.getBlockState(at).is(cracked), "frost cracks open bricks");

        // 4. indoors (a roof over it and its sides): nothing, even wet and freezing
        for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) h.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
        h.setBlock(at, bricks);
        h.setBlock(water, Blocks.WATER);
        h.assertTrue(!age(level, abs, h.getBlockState(at), 0, true) && h.getBlockState(at).is(bricks), "under a roof bricks don't age");
        for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) h.setBlock(new BlockPos(x, 3, z), Blocks.AIR);
        h.setBlock(water, Blocks.AIR);

        // 5. Pumice scrubs moss off (1 wear), keeping slab halves; Lime Mortar repoints cracks (1 used), vanilla's too
        ItemStack pumice = new ItemStack(StoneCrafts.PUMICE_CHUNK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, pumice);
        h.setBlock(at, mossy);
        pumice.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
        h.assertTrue(h.getBlockState(at).is(bricks) && pumice.getDamageValue() == 1, "Pumice Chunk: Mossy -> Limestone Bricks, 1 wear");
        h.assertTrue(scrubbed(Blocks.MOSSY_STONE_BRICKS) == Blocks.STONE_BRICKS && scrubbed(StoneFamilies.block("mossy_limestone_brick_slab")) == StoneFamilies.block("limestone_brick_slab"),
                "pumice scrubs vanilla and slab moss");
        ItemStack mortar = new ItemStack(IronOres.LIME_MORTAR.get(), 2);
        h.setBlock(at, cracked);
        h.assertTrue(repoint(level, abs, player, mortar) && h.getBlockState(at).is(bricks) && mortar.getCount() == 1, "Lime Mortar: Cracked -> Limestone Bricks, 1 used");
        h.assertTrue(!repoint(level, abs, player, mortar) && mortar.getCount() == 1, "mortar does nothing on whole bricks");
        h.assertTrue(repointed(Blocks.CRACKED_STONE_BRICKS) == Blocks.STONE_BRICKS && repointed(Blocks.MOSSY_STONE_BRICKS) == null, "vanilla cracks repoint, moss doesn't");

        // 6. crafting the aged forms like vanilla: bricks + vine -> mossy
        h.assertTrue(craft(h, 2, 1, stack("limestone_bricks"), new ItemStack(Items.VINE)).is(mossy.asItem()), "Limestone Bricks + Vine = Mossy");

        // 7. Mosaic Tiles: four different polished stones -> 4
        ItemStack mosaic = craft(h, 2, 2, stack("polished_jasper"), stack("polished_quartzite"), stack("cut_travertine"), stack("yellow_ochre_bricks"));
        h.assertTrue(mosaic.is(StoneFamilies.block("warm_mosaic_tiles").asItem()) && mosaic.getCount() == 4, "4 stones = 4 Warm Mosaic Tiles, got " + mosaic);

        // 8. roof tiles: clay -> unfired, fired in the Clay Kiln, laid as tiles and roofs; pigment colours them
        h.assertTrue(craft(h, 2, 1, new ItemStack(Items.CLAY_BALL), new ItemStack(Items.CLAY_BALL)).is(UNFIRED_ROOF_TILE.get()), "2 Clay Balls = Unfired Roof Tile");
        var bake = KilnBlockEntity.recipeFor(new ItemStack(UNFIRED_ROOF_TILE.get())).orElseThrow();
        h.assertTrue(bake.result().get() == FIRED_ROOF_TILE.get() && bake.temp() == ROOF_TILE_TEMP, "kiln: Unfired -> Fired Roof Tile at " + ROOF_TILE_TEMP);
        ItemStack tile = new ItemStack(FIRED_ROOF_TILE.get());
        ItemStack roof = craft(h, 2, 2, tile, ItemStack.EMPTY, tile.copy(), tile.copy());
        h.assertTrue(roof.is(TILED_ROOFS.get(0).get().asItem()) && roof.getCount() == 4, "3 Fired Roof Tiles = 4 Tiled Roof, got " + roof);
        ItemStack r = new ItemStack(TILED_ROOFS.get(0).get());
        ItemStack black = craft(h, 3, 3, r, r.copy(), r.copy(), r.copy(), new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MODID, "black_pigment"))),
                r.copy(), r.copy(), r.copy(), r.copy());
        h.assertTrue(black.is(TILED_ROOFS.get(2).get().asItem()) && black.getCount() == 8, "8 Tiled Roof + Black Pigment = 8 Black Tiled Roof, got " + black);
        h.assertTrue(!TILED_ROOFS.get(0).get().defaultBlockState().ignitedByLava(), "roof tiles don't burn");

        h.succeed();
    }

    private Masonry() {}
}
