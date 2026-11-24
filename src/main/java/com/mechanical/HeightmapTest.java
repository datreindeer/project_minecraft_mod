package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * In 26.x the live heightmaps count only blocks in #minecraft:blocks_motion_in_heightmap(_no_leaves) (scripts/gen_heightmap.py).
 * heightmap_tags: every mod block that blocks motion by the old rule (BlockState#isSolid with a collision shape) is in the
 * tags, and none without collision is (unless forced solid, like pressure plates). heightmap_rain: on a column of Limestone the heightmaps report its top, rain falls
 * on it (not through it) and snow would settle on it (vanilla lays snow at the MOTION_BLOCKING height).
 */
@EventBusSubscriber(modid = MODID)
public final class HeightmapTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TAGS = key("heightmap_tags"), RAIN = key("heightmap_rain");

    static {
        TEST_FUNCTIONS.register("heightmap_tags", () -> HeightmapTest::tags);
        TEST_FUNCTIONS.register("heightmap_rain", () -> HeightmapTest::rain);
    }

    private static ResourceKey<Consumer<GameTestHelper>> key(String name) {
        return ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name));
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "heightmap"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(TAGS.identifier(), new FunctionGameTestInstance(TAGS, TestArea.of(env, 20, true)));
        event.registerTest(RAIN.identifier(), new FunctionGameTestInstance(RAIN, TestArea.of(env, 40, true)));
    }

    private static void tags(GameTestHelper h) {
        List<String> missing = new ArrayList<>(), extra = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(b);
            if (!id.getNamespace().equals(MODID)) continue;
            BlockState s = b.defaultBlockState();
            boolean collides = !s.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).isEmpty();
            boolean leaves = s.is(BlockTags.LEAVES);
            if (s.isSolid() && collides && !s.is(leaves ? BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP : BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP_NO_LEAVES))
                missing.add(id.getPath());
            // forced solid without collision (pressure plates) may be in, as vanilla's #pressure_plates are
            if (!collides && !s.isSolid() && s.is(BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP_NO_LEAVES)) extra.add(id.getPath());
        }
        if (!missing.isEmpty() || !extra.isEmpty())
            throw h.assertionException("heightmap tags (gen_heightmap.py): " + missing.size() + " solid blocks missing " + missing + ", "
                    + extra.size() + " without collision in them " + extra);
        h.succeed();
    }

    private static void rain(GameTestHelper h) {
        Block limestone = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(MODID, "limestone"));
        BlockPos rel = new BlockPos(8, 1, 8);
        for (int y = 0; y < 3; y++) h.setBlock(rel.above(y), limestone);
        var level = h.getLevel();
        BlockPos top = h.absolutePos(rel.above(2));
        for (Heightmap.Types t : new Heightmap.Types[] {Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR}) {
            int y = level.getHeight(t, top.getX(), top.getZ());
            if (y != top.getY() + 1) throw h.assertionException(t + " is " + y + " over the Limestone column, its top is " + top.getY());
        }
        // vanilla's snow goes on the block at the MOTION_BLOCKING height, rain stops on the block under it
        if (!level.getBlockState(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, top).below()).is(limestone))
            throw h.assertionException("snow would not settle on the Limestone");
        float rainWas = level.getRainLevel(1.0F);
        try {
            level.setRainLevel(1.0F);
            Biome.Precipitation over = level.precipitationAt(top.above()), in = level.precipitationAt(top);
            boolean wet = level.getBiome(top).value().getPrecipitationAt(top.above(), level.getSeaLevel()) != Biome.Precipitation.NONE;
            if (wet && over == Biome.Precipitation.NONE) throw h.assertionException("no rain or snow falls on top of the Limestone");
            if (in != Biome.Precipitation.NONE) throw h.assertionException("rain falls into the Limestone (" + in + ")");
        } finally {
            level.setRainLevel(rainWas);
        }
        h.succeed();
    }

    private HeightmapTest() {}
}
