package com.mechanical.worldgen;

import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.mechanical.wildlife.WildlifeRestock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for retrogen (fresh chunks marked current; an old chunk gets features and is re-marked; a chunk by a chest or
 * long lived in is skipped) and for the wildlife restock preferring a species missing from the area.
 */
@EventBusSubscriber(modid = MODID)
public final class RetrogenTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> RETROGEN = key("retrogen"), RESTOCK = key("restock_missing_species");
    /** Far from the test grid, so features placed here can't land in another test. */
    private static final int FAR = 400;

    static {
        TEST_FUNCTIONS.register("retrogen", () -> RetrogenTest::retrogen);
        TEST_FUNCTIONS.register("restock_missing_species", () -> RetrogenTest::restock);
    }

    private static ResourceKey<Consumer<GameTestHelper>> key(String name) {
        return ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name));
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "retrogen"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "retrogen"),
                new FunctionGameTestInstance(RETROGEN, com.mechanical.TestArea.of(env, 20, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "restock_missing_species"),
                new FunctionGameTestInstance(RESTOCK, com.mechanical.TestArea.of(env, 20, true)));
    }

    /** Generates (or loads) the 3x3 around a chunk and returns its middle. */
    private static LevelChunk load(ServerLevel level, int cx, int cz) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.getChunk(cx + dx, cz + dz);
        return level.getChunk(cx, cz);
    }

    private static void retrogen(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int current = Retrogen.sets(level).current();

        // freshly generated chunks already have every feature
        LevelChunk fresh = load(level, FAR, FAR);
        h.assertTrue(fresh.getExistingDataOrNull(Retrogen.MARKER) instanceof Integer m && m == current, "a freshly generated chunk is marked current");
        h.assertTrue(Retrogen.process(level, FAR, FAR) == Retrogen.Result.CURRENT, "a current chunk is left alone");

        // old chunks (no marker) get features and are re-marked. The test world is flat sandstone desert: a stone floor lets
        // loose stones (every overworld biome, about every other chunk) place, so over 12 chunks something must
        int placedBefore = Retrogen.placedFeatures;
        for (int i = 0; i < 12; i++) {
            int cx = FAR + 3 + 3 * i;
            LevelChunk old = load(level, cx, FAR);
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    int bx = cx * 16 + x, bz = FAR * 16 + z;
                    level.setBlock(new BlockPos(bx, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) - 1, bz), Blocks.STONE.defaultBlockState(), 2);
                }
            }
            old.removeData(Retrogen.MARKER);
            h.assertTrue(Retrogen.process(level, cx, FAR) == Retrogen.Result.UPDATED, "an old chunk is retrogenned");
            h.assertTrue(old.getExistingDataOrNull(Retrogen.MARKER) instanceof Integer m && m == current, "a retrogenned chunk is re-marked");
        }
        h.assertTrue(Retrogen.placedFeatures > placedBefore, "retrogen placed features in old chunks");

        // a player's chest in a neighbouring chunk: skipped (but marked, so it isn't tried again)
        LevelChunk byChest = load(level, FAR, FAR + 6);
        byChest.removeData(Retrogen.MARKER);
        int x = (FAR + 1) * 16 + 4, z = (FAR + 6) * 16 + 4;
        level.setBlock(new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z), Blocks.CHEST.defaultBlockState(), 3);
        h.assertTrue(Retrogen.process(level, FAR, FAR + 6) == Retrogen.Result.SKIPPED, "a chunk next to a chest is skipped");
        h.assertTrue(byChest.getExistingDataOrNull(Retrogen.MARKER) instanceof Integer m && m == current, "a skipped chunk is marked");

        // long lived in: skipped
        LevelChunk lived = load(level, FAR, FAR + 9);
        lived.removeData(Retrogen.MARKER);
        lived.setInhabitedTime(Retrogen.MAX_INHABITED * 10);
        h.assertTrue(Retrogen.process(level, FAR, FAR + 9) == Retrogen.Result.SKIPPED, "a chunk players lived in is skipped");
        h.succeed();
    }

    private static void restock(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos at = h.absolutePos(new BlockPos(7, 1, 7));
        AABB area = new AABB(at).inflate(64); // the area pick() looks at
        List<EntityType<?>> types = WildlifeRestock.candidates(level, at).stream().<EntityType<?>>map(w -> w.value().type()).distinct().toList();
        h.assertTrue(types.size() >= 2, "the test biome has at least two species (" + types + ")");
        EntityType<?> missing = types.stream().filter(t -> level.getEntities(t, area, Entity::isAlive).isEmpty()).reduce((a, b) -> b).orElse(null);
        h.assertTrue(missing != null, "some species is not around yet");
        // every other species gets a living member right here
        List<Entity> added = new ArrayList<>();
        for (EntityType<?> type : types) {
            if (type == missing || !level.getEntities(type, area, Entity::isAlive).isEmpty()) continue;
            Entity e = type.create(level, EntitySpawnReason.COMMAND);
            if (e == null) continue;
            if (e instanceof Mob mob) mob.setNoAi(true);
            e.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            level.addFreshEntity(e);
            added.add(e);
        }
        for (int i = 0; i < 20; i++) {
            EntityType<?> picked = WildlifeRestock.pick(level, at, level.getRandom()).map(d -> d.type()).orElse(null);
            h.assertTrue(picked == missing, "restock picks the missing species " + missing + ", not " + picked);
        }
        added.forEach(Entity::discard);
        h.succeed();
    }
}
