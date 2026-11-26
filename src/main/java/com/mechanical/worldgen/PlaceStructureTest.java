package com.mechanical.worldgen;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.AncientOak;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** /structure: both kinds stand and are known to the game; a second one on the same spot and one over a player block are refused. */
@EventBusSubscriber(modid = MODID)
public final class PlaceStructureTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final Identifier ID = Identifier.fromNamespaceAndPath(MODID, "place_structure");
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, ID);

    static {
        TEST_FUNCTIONS.register("place_structure", () -> PlaceStructureTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(ID, new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(ID, new FunctionGameTestInstance(TEST, com.mechanical.TestArea.of(env, 20, true)));
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        if (!ok) throw h.assertionException("place_structure: " + what);
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // far from every test area
        BlockPos from = h.absolutePos(BlockPos.ZERO);
        int x = (from.getX() >> 4 << 4) + 8 + 3200, z = (from.getZ() >> 4 << 4) + 8 + 3200;

        String r = PlaceStructure.place(level, "tribe_village", x, z);
        check(h, r == null, "village refused: " + r);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        var village = TribeVillageStructure.villageAt(level, new BlockPos(x, y, z));
        check(h, village != null, "the game does not know the village");
        var fire = level.getBlockState(village.centre());
        check(h, fire.is(Blocks.CAMPFIRE), "no campfire at the centre, but " + fire);
        check(h, PlaceStructure.place(level, "tribe_village", x, z) != null, "a second village on the same spot was allowed");

        int ox = x + 320;
        r = PlaceStructure.place(level, "ancient_oak", ox, z);
        check(h, r == null, "oak refused: " + r);
        var start = level.getChunk(ox >> 4, z >> 4).getAllStarts().values().stream()
                .filter(s -> s.isValid() && s.getPieces().getFirst() instanceof AncientOakStructure.Piece).findFirst().orElse(null);
        check(h, start != null, "the oak's start was not saved");
        check(h, level.getBlockState(PlaceStructure.centre(start)).is(AncientOak.ALTAR.get()), "no altar at the oak's foot");

        int tx = x + 640;
        level.getChunk(tx >> 4, z >> 4);
        BlockPos table = new BlockPos(tx, level.getHeight(Heightmap.Types.MOTION_BLOCKING, tx, z), z);
        level.setBlock(table, Blocks.CRAFTING_TABLE.defaultBlockState(), 2);
        var beside = level.getBlockState(table.offset(3, -1, 3));
        r = PlaceStructure.place(level, "tribe_village", tx, z);
        check(h, r != null && r.startsWith("Buildings"), "built over a player block: " + r);
        check(h, level.getBlockState(table).is(Blocks.CRAFTING_TABLE), "the player block is gone");
        check(h, level.getBlockState(table.offset(3, -1, 3)).equals(beside), "the ground changed after a refusal");
        // a chest under a stone roof: the roof is the surface, the chest far below it
        int hx = x + 960;
        level.getChunk(hx >> 4, z >> 4);
        BlockPos chest = new BlockPos(hx, level.getHeight(Heightmap.Types.MOTION_BLOCKING, hx, z), z);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
        level.setBlock(chest.above(6), Blocks.STONE.defaultBlockState(), 2);
        r = PlaceStructure.place(level, "tribe_village", hx, z);
        check(h, r != null && r.startsWith("Buildings"), "built over a chest under a roof: " + r);
        check(h, level.getBlockState(chest).is(Blocks.CHEST), "the chest is gone");
        h.succeed();
    }
}
