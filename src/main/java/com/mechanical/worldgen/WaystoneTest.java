package com.mechanical.worldgen;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.TestArea;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * waystone_teleport: travel to a column with no stone builds one and lands the traveller on its feet beside it (air at feet
 * and head, solid under), and a second trip with the front spot blocked still lands safely. waystone_unbreakable: no
 * hardness, no mining progress, blast proof (an explosion beside it leaves all three parts), pistons can't push it, no drops.
 * waystone_pairs: the seed's pairs round the origin are 1000-10000 apart and each end finds the other.
 */
@EventBusSubscriber(modid = MODID)
public final class WaystoneTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TELEPORT = key("waystone_teleport"), UNBREAKABLE = key("waystone_unbreakable"),
            PAIRS = key("waystone_pairs");

    static {
        TEST_FUNCTIONS.register("waystone_teleport", () -> WaystoneTest::teleport);
        TEST_FUNCTIONS.register("waystone_unbreakable", () -> WaystoneTest::unbreakable);
        TEST_FUNCTIONS.register("waystone_pairs", () -> WaystoneTest::pairs);
    }

    private static ResourceKey<Consumer<GameTestHelper>> key(String name) {
        return ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name));
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "waystone"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(TELEPORT.identifier(), new FunctionGameTestInstance(TELEPORT, TestArea.of(env, 40, true)));
        event.registerTest(UNBREAKABLE.identifier(), new FunctionGameTestInstance(UNBREAKABLE, TestArea.of(env, 40, true)));
        event.registerTest(PAIRS.identifier(), new FunctionGameTestInstance(PAIRS, TestArea.of(env, 20, true)));
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) h.setBlock(x, 0, z, Blocks.STONE);
    }

    private static void checkSafe(GameTestHelper h, Pig pig, BlockPos column, String what) {
        ServerLevel level = h.getLevel();
        BlockPos feet = pig.blockPosition();
        if (Math.abs(feet.getX() - column.getX()) > 2 || Math.abs(feet.getZ() - column.getZ()) > 2)
            throw h.assertionException(what + ": landed at " + feet + ", not beside the stone at " + column);
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty())
            throw h.assertionException(what + ": landed inside blocks at " + feet);
        if (level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty())
            throw h.assertionException(what + ": nothing to stand on at " + feet);
        if (pig.isInWater() || pig.isInLava()) throw h.assertionException(what + ": landed in a fluid");
    }

    private static void teleport(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        BlockPos column = h.absolutePos(new BlockPos(11, 1, 11));
        Pig pig = h.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(3, 1, 3));
        pig.setNoAi(true);
        Waystones.teleport(pig, level, column.getX(), column.getZ(), Direction.WEST);
        BlockPos base = Waystones.findStone(level, column.getX(), column.getZ());
        if (base == null) throw h.assertionException("no waystone was built at the far end");
        for (int i = 1; i <= 2; i++)
            if (!level.getBlockState(base.above(i)).is(Waystones.WAYSTONE.get()) || level.getBlockState(base.above(i)).getValue(Waystones.PART).ordinal() != i)
                throw h.assertionException("the far waystone is missing part " + i);
        checkSafe(h, pig, column, "first trip");
        // block the spot in front of the runes: the second trip must find another
        BlockPos front = base.relative(level.getBlockState(base).getValue(Waystones.WaystoneBlock.FACING));
        level.setBlockAndUpdate(front, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(front.above(), Blocks.STONE.defaultBlockState());
        pig.teleportTo(h.absolutePos(new BlockPos(3, 1, 3)).getX() + 0.5, h.absolutePos(new BlockPos(3, 1, 3)).getY(), h.absolutePos(new BlockPos(3, 1, 3)).getZ() + 0.5);
        Waystones.teleport(pig, level, column.getX(), column.getZ(), Direction.WEST);
        if (!base.equals(Waystones.findStone(level, column.getX(), column.getZ()))) throw h.assertionException("the second trip built another stone");
        checkSafe(h, pig, column, "second trip");
        h.succeed();
    }

    private static void unbreakable(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        BlockState lower = Waystones.WAYSTONE.get().defaultBlockState(), middle = lower.setValue(Waystones.PART, Waystones.Part.MIDDLE),
                upper = lower.setValue(Waystones.PART, Waystones.Part.UPPER);
        BlockPos rel = new BlockPos(8, 1, 8), pos = h.absolutePos(rel);
        level.setBlock(pos, lower, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
        level.setBlock(pos.above(), middle, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        level.setBlock(pos.above(2), upper, Block.UPDATE_ALL);
        if (lower.getDestroySpeed(level, pos) >= 0) throw h.assertionException("waystone has a hardness");
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        if (lower.getDestroyProgress(player, level, pos) > 0) throw h.assertionException("a survival player makes mining progress");
        if (lower.getBlock().getExplosionResistance() < 3_000_000) throw h.assertionException("waystone is not blast proof");
        if (lower.getPistonPushReaction() != PushReaction.IMMOVEABLE) throw h.assertionException("pistons can push a waystone");
        if (!Block.getDrops(lower, level, pos, null).isEmpty()) throw h.assertionException("waystone drops something");
        level.explode(null, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() - 1.5, 5.0F, Level.ExplosionInteraction.TNT);
        h.runAfterDelay(2, () -> {
            if (!level.getBlockState(pos).is(Waystones.WAYSTONE.get()) || !level.getBlockState(pos.above()).is(Waystones.WAYSTONE.get())
                    || !level.getBlockState(pos.above(2)).is(Waystones.WAYSTONE.get()))
                throw h.assertionException("an explosion broke the waystone");
            h.succeed();
        });
    }

    private static void pairs(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        int found = 0;
        for (int rx = -2; rx <= 2; rx++) {
            for (int rz = -2; rz <= 2; rz++) {
                int[] p = Waystones.pair(level, rx, rz);
                if (p.length == 0) continue;
                found++;
                double d = 16 * Math.hypot(p[2] - p[0], p[3] - p[1]);
                if (d < Waystones.MIN || d > Waystones.MAX) throw h.assertionException("pair " + rx + "," + rz + " is " + d + " blocks apart");
                int[] b = Waystones.partner(level, p[0], p[1]), a = Waystones.partner(level, p[2], p[3]);
                if (b == null || b[0] != p[2] || b[1] != p[3] || a == null || a[0] != p[0] || a[1] != p[1])
                    throw h.assertionException("ends of pair " + rx + "," + rz + " don't find each other");
            }
        }
        if (found == 0) throw h.assertionException("no waystone pairs round the origin");
        h.succeed();
    }

    private WaystoneTest() {}
}
