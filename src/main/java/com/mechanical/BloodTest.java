package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * B6: livestock crowded in a small pen, worn below the bleed line by cramming, hunger and a wall, draw no blood: no Blood
 * blocks on the ground and none of them bleeding. A cow bitten by another bleeds (the check isn't vacuous).
 */
@EventBusSubscriber(modid = MODID)
public final class BloodTest {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(MODID, "blood_only_from_wounds");
    private static final Identifier KETTLE_ID = Identifier.fromNamespaceAndPath(MODID, "blood_kettle");

    @SubscribeEvent
    static void registerFunction(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, ID, () -> (Consumer<GameTestHelper>) BloodTest::test);
        event.register(Registries.TEST_FUNCTION, KETTLE_ID, () -> (Consumer<GameTestHelper>) BloodTest::kettle);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(ID, new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(ID, new FunctionGameTestInstance(net.minecraft.resources.ResourceKey.create(Registries.TEST_FUNCTION, ID), TestArea.of(env, 200, true)));
        event.registerTest(KETTLE_ID, new FunctionGameTestInstance(net.minecraft.resources.ResourceKey.create(Registries.TEST_FUNCTION, KETTLE_ID), TestArea.of(env, 100, true)));
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x <= 8; x++) for (int z = 0; z <= 8; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
            for (int y = 1; y <= 3; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        // a 2 x 2 pen, fenced
        for (int x = 1; x <= 4; x++) for (int z = 1; z <= 4; z++)
            if (x == 1 || x == 4 || z == 1 || z == 4) h.setBlock(new BlockPos(x, 1, z), Blocks.OAK_FENCE);
        List<Animal> herd = new ArrayList<>();
        // packed in, though short of vanilla's cramming limit so none dies of it (that's the hurt below)
        for (int i = 0; i < 20; i++) {
            BlockPos at = new BlockPos(2 + i % 2, 1, 2 + i / 2 % 2);
            herd.add(i % 2 == 0 ? h.spawn(EntityTypes.COW, at) : h.spawn(EntityTypes.SHEEP, at));
        }
        // worn down without a wound: below half health, which used to start the bleeding
        var sources = level.damageSources();
        for (int i = 0; i < herd.size(); i++) {
            Animal a = herd.get(i);
            a.hurtServer(level, i % 3 == 0 ? sources.cramming() : i % 3 == 1 ? sources.starve() : sources.inWall(), a.getMaxHealth() * 0.6F);
        }
        Animal bitten = h.spawn(EntityTypes.COW, new BlockPos(7, 1, 7));
        bitten.hurtServer(level, sources.mobAttack(herd.get(0)), bitten.getMaxHealth() * 0.6F);
        h.assertTrue(Blood.isBleeding(bitten), "a bitten cow below half health bleeds");

        h.runAfterDelay(100, () -> {
            for (Animal a : herd) h.assertFalse(a.isAlive() && Blood.isBleeding(a), "a penned " + a.getType().getDescriptionId() + " bleeds without a wound");
            for (BlockPos p : BlockPos.betweenClosed(h.absolutePos(new BlockPos(2, 1, 2)), h.absolutePos(new BlockPos(3, 2, 3)))) // inside the fence: the bitten cow outside may drip beside it
                h.assertFalse(level.getBlockState(p).is(Mechanical.BLOOD.get()), "blood on the ground of the pen at " + p);
            h.succeed();
        });
    }

    /**
     * A kettle under a Hanging Rack catches the blood: a bucket's worth (FULL steps) goes in with no pool on the ground or
     * around the rack; only once it is full does the blood run onto the ground again.
     */
    private static void kettle(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x <= 4; x++) for (int z = 0; z <= 4; z++) {
            h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            for (int y = 1; y <= 4; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        }
        var rack = CarcassHauling.HANGING_RACK.get().defaultBlockState();
        for (HangingRackBlock.Part part : HangingRackBlock.Part.values())
            h.setBlock(new BlockPos(2, 1 + part.ordinal(), 2), rack.setValue(HangingRackBlock.PART, part));
        BlockPos base = h.absolutePos(new BlockPos(2, 1, 2));
        h.assertValueEqual(HangingRackBlock.catchBlood(level, base, 3), 3, "no kettle: all the blood is left over");
        level.setBlock(base, level.getBlockState(base).setValue(HangingRackBlock.KETTLE, HangingRackBlock.Pot.BRONZE), 3);
        for (int i = 0; i < HangingRackBlock.FULL; i++) HangingRackBlock.bleedOnto(level, base);
        h.assertValueEqual(level.getBlockState(base).getValue(HangingRackBlock.KETTLE_BLOOD), HangingRackBlock.FULL, "kettle fill");
        h.assertValueEqual(level.getBlockState(base).getValue(HangingRackBlock.BLOOD), 0, "pool under a rack with a kettle");
        for (BlockPos p : BlockPos.betweenClosed(h.absolutePos(new BlockPos(0, 1, 0)), h.absolutePos(new BlockPos(4, 1, 4))))
            h.assertFalse(level.getBlockState(p).is(Mechanical.BLOOD.get()), "blood on the ground beside the kettle at " + p);
        h.assertValueEqual(HangingRackBlock.catchBlood(level, base, 2), 2, "a full kettle takes no more");
        HangingRackBlock.bleedOnto(level, base);
        h.assertValueEqual(level.getBlockState(base).getValue(HangingRackBlock.BLOOD), 1, "a full kettle runs over into the pool");
        h.succeed();
    }

    private BloodTest() {}
}
