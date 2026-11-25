package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Living-world plan F6, F7, F11: every stone family rock drops its chunk (a rock chunk) or Pumice Chunk; the Mold Chest
 * takes a mold from a hopper and its menu, and refuses anything else; a Dirt Slab grows grass from a grass block next to
 * it, the Grass Slab dies back under stone, gets snowy under snow, flattens to a path slab with a shovel and drops a full Dirt.
 */
@EventBusSubscriber(modid = MODID)
public final class LivingWorldBlocksTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "living_world_blocks"));

    static {
        TEST_FUNCTIONS.register("living_world_blocks", () -> LivingWorldBlocksTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "living_world_blocks"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "living_world_blocks"),
                new FunctionGameTestInstance(TEST, TestArea.of(env, 100, true)));
    }

    private static List<ItemStack> drops(GameTestHelper h, BlockPos rel, BlockState state) {
        h.setBlock(rel, state);
        return Block.getDrops(state, h.getLevel(), h.absolutePos(rel), null);
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);

        // F6: every new loose rock gives its own chunk, which counts as a rock chunk (pumice: one Pumice Chunk)
        BlockPos rockAt = new BlockPos(0, 1, 0);
        for (var e : StoneFamilies.LOOSE_ROCKS.entrySet()) {
            List<ItemStack> got = drops(h, rockAt, e.getValue().get().defaultBlockState().setValue(RockBlock.SIZE, 2));
            boolean pumice = e.getKey().equals("pumice");
            ItemStack want = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MODID, pumice ? "pumice_chunk" : e.getKey() + "_chunk")));
            h.assertTrue(got.size() == 1 && got.get(0).is(want.getItem()) && got.get(0).getCount() == (pumice ? 1 : 2)
                    && (pumice || want.is(Mechanical.ROCK_CHUNKS)), "loose " + e.getKey() + " drops " + got);
        }
        h.setBlock(rockAt, Blocks.AIR);

        // F7: hopper insertion and the menu take molds only
        BlockPos chestAt = new BlockPos(2, 1, 0);
        h.setBlock(chestAt, MoldBench.MOLD_CHEST.get());
        MoldChestBlock.Entity chest = (MoldChestBlock.Entity) level.getBlockEntity(h.absolutePos(chestAt));
        ItemStack dirt = HopperBlockEntity.addItem(null, chest, new ItemStack(Items.DIRT, 5), Direction.DOWN);
        h.assertTrue(dirt.getCount() == 5 && chest.isEmpty(), "a hopper can't put dirt in the Mold Chest");
        ItemStack mold = HopperBlockEntity.addItem(null, chest, new ItemStack(Casting.UNFIRED_CLAY_INGOT_MOLD.get()), Direction.DOWN);
        h.assertTrue(mold.isEmpty() && chest.getItem(0).is(Casting.UNFIRED_CLAY_INGOT_MOLD.get()), "a hopper puts a mold in");
        h.assertTrue(new ItemStack(Casting.CLAY_INGOT_MOLD_ITEM.get()).is(MoldChestBlock.MOLDS), "fired molds count");
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = new MoldChestBlock.Menu(0, p.getInventory(), chest);
        h.assertTrue(menu.slots.size() == 27 + 36 && !menu.getSlot(1).mayPlace(new ItemStack(Items.DIRT))
                && menu.getSlot(1).mayPlace(new ItemStack(Casting.CLAY_INGOT_MOLD_ITEM.get())), "the chest's slots only take molds");

        // F11: grass creeps onto a dirt slab from a grass block beside it. It needs light 9 above it: at noon (as the troll test
        // does; restored after) the sky gives it. Not a glowstone at night: a busy test run leaves the light engine's queue so
        // far behind that a new block light may not be worked out within the test's ticks.
        BlockPos slab = new BlockPos(4, 1, 4), abs = h.absolutePos(slab);
        h.setBlock(new BlockPos(5, 1, 4), Blocks.GRASS_BLOCK);
        h.setBlock(slab, EarthBlocks.DIRT_SLAB.get());
        var server = level.getServer();
        long saved = level.getOverworldClockTime();
        h.startSequence()
                .thenExecute(() -> Calendar.overworldClock(server).ifPresent(c -> server.clockManager().setTotalTicks(c, Math.floorDiv(saved, 24000L) * 24000L + 6000L)))
                .thenWaitUntil(() -> { // retried until the sky has brightened to noon
                    h.assertTrue(level.getMaxLocalRawBrightness(abs.above()) >= 9, "daylight on the dirt slab: " + level.getMaxLocalRawBrightness(abs.above()));
                    for (int i = 0; i < 400 && level.getBlockState(abs).is(EarthBlocks.DIRT_SLAB); i++) level.getBlockState(abs).randomTick(level, abs, level.getRandom());
                    h.assertTrue(level.getBlockState(abs).is(EarthBlocks.GRASS_SLAB), "grass spreads to the dirt slab: " + level.getBlockState(abs));
                })
                .thenExecute(() -> {
                    Calendar.overworldClock(server).ifPresent(c -> server.clockManager().setTotalTicks(c, saved));
                    h.assertTrue(drops(h, slab, level.getBlockState(abs)).stream().allMatch(s -> s.is(Items.DIRT) && s.getCount() == 1), "a grass slab drops a Dirt");

                    // snow on a top slab makes it snowy; under stone it dies back to a dirt slab
                    BlockPos top = new BlockPos(2, 1, 4);
                    h.setBlock(top, EarthBlocks.GRASS_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE));
                    h.setBlock(top.above(), Blocks.SNOW);
                    h.assertTrue(h.getBlockState(top).getValue(DirtSlabBlock.Grass.SNOWY), "snow on it makes the grass slab snowy");
                    h.setBlock(top.above(), Blocks.STONE);
                    BlockPos topAbs = h.absolutePos(top);
                    level.getBlockState(topAbs).randomTick(level, topAbs, level.getRandom());
                    h.assertTrue(level.getBlockState(topAbs).is(EarthBlocks.DIRT_SLAB) && level.getBlockState(topAbs).getValue(SlabBlock.TYPE) == SlabType.DOUBLE,
                            "under stone the grass slab turns back to a (double) dirt slab: " + level.getBlockState(topAbs));
                    List<ItemStack> two = drops(h, top, level.getBlockState(topAbs));
                    h.assertTrue(two.size() == 1 && two.get(0).is(Items.DIRT) && two.get(0).getCount() == 1, "a double dirt slab drops one Dirt: " + two);

                    // a shovel flattens the grass slab into a path slab, keeping its type
                    ItemStack shovel = new ItemStack(Items.STONE_SHOVEL);
                    p.setItemInHand(InteractionHand.MAIN_HAND, shovel);
                    shovel.useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false)));
                    h.assertTrue(level.getBlockState(abs).is(EarthBlocks.DIRT_PATH_SLAB) && level.getBlockState(abs).getValue(SlabBlock.TYPE) == SlabType.BOTTOM,
                            "the shovel makes a dirt path slab: " + level.getBlockState(abs));
                })
                .thenSucceed();
    }

    private LivingWorldBlocksTest() {}
}
