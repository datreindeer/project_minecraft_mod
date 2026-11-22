package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.wildlife.WildNature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Living-world plan D5, D6, A10. husbandry_ground: hooves churn grass to dirt, coarse dirt, mud in the rain and packed mud
 * when dry; a hungry cow grazing on grass is fed; a boar roots grass up to dirt; a deer browses a sapling; troughs side
 * by side join, fill one after the other and share fodder; a water bucket fills a trough. husbandry_trough: a cow out of food
 * and water is tired (slow, no love); boxed in at a joined trough it drinks and eats from it and is not tired after.
 */
@EventBusSubscriber(modid = MODID)
public final class HusbandryTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> GROUND = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "husbandry_ground"));
    private static final ResourceKey<Consumer<GameTestHelper>> TROUGH = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "husbandry_trough"));

    static {
        TEST_FUNCTIONS.register("husbandry_ground", () -> HusbandryTest::ground);
        TEST_FUNCTIONS.register("husbandry_trough", () -> HusbandryTest::trough);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "husbandry"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "husbandry_ground"), new FunctionGameTestInstance(GROUND, TestArea.of(env, 100, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "husbandry_trough"), new FunctionGameTestInstance(TROUGH, TestArea.of(env, 400, true)));
    }

    /** Places a trough already joined to the troughs beside it. */
    static void trough(GameTestHelper h, BlockPos rel) {
        h.setBlock(rel, Block.updateFromNeighbourShapes(Husbandry.FEEDING_TROUGH.get().defaultBlockState(), h.getLevel(), h.absolutePos(rel)));
    }

    private static FeedingTroughBlockEntity at(GameTestHelper h, BlockPos rel) {
        return (FeedingTroughBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(rel));
    }

    private static void ground(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x <= 12; x++) for (int z = 0; z <= 12; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);

        // D5: hoof stages
        BlockPos g = h.absolutePos(new BlockPos(1, 0, 1));
        level.setBlockAndUpdate(g, Blocks.GRASS_BLOCK.defaultBlockState());
        h.assertTrue(TrampledPaths.churn(level, g, false) && level.getBlockState(g).is(Blocks.DIRT), "grass churns to dirt");
        h.assertTrue(TrampledPaths.churn(level, g, false) && level.getBlockState(g).is(Blocks.COARSE_DIRT), "dirt churns to coarse dirt");
        h.assertFalse(TrampledPaths.churn(level, g, false), "dry coarse dirt stays");
        h.assertTrue(TrampledPaths.churn(level, g, true) && level.getBlockState(g).is(Blocks.MUD), "coarse dirt in the rain turns to mud");
        h.assertFalse(TrampledPaths.churn(level, g, true), "mud in the rain stays mud");
        h.assertTrue(TrampledPaths.churn(level, g, false) && level.getBlockState(g).is(Blocks.PACKED_MUD), "mud trodden dry packs");
        h.assertTrue(TrampledPaths.churn(level, g, true) && level.getBlockState(g).is(Blocks.MUD), "rain softens packed mud again");

        // A10 / D6: grazing feeds a hungry cow, nothing to graze on stone
        h.setBlock(new BlockPos(3, 0, 3), Blocks.GRASS_BLOCK);
        Animal cow = h.spawn(EntityTypes.COW, new BlockPos(3, 1, 3));
        long now = level.getGameTime();
        Husbandry.setNeeds(cow, now + 10, now + Husbandry.WATER_TICKS);
        h.assertTrue(Husbandry.needs(cow).hungry(now), "set hungry");
        h.assertTrue(Husbandry.graze(cow), "a cow on grass grazes");
        h.assertFalse(Husbandry.needs(cow).hungry(now), "grazing fed it");
        h.assertTrue(Husbandry.needs(cow).eating(now), "head down while it grazes");
        BlockState under = level.getBlockState(h.absolutePos(new BlockPos(3, 0, 3)));
        h.assertTrue(under.is(Blocks.GRASS_BLOCK) || under.is(Blocks.DIRT), "grass grazed or bared to dirt, got " + under);
        cow.teleportTo(h.absolutePos(new BlockPos(5, 1, 3)).getX() + 0.5, h.absolutePos(new BlockPos(5, 1, 3)).getY(), h.absolutePos(new BlockPos(5, 1, 3)).getZ() + 0.5);
        h.assertFalse(Husbandry.graze(cow), "nothing to graze on stone");
        cow.discard();

        // A10: boar rooting, deer browsing
        BlockPos r = h.absolutePos(new BlockPos(1, 0, 5));
        level.setBlockAndUpdate(r, Blocks.GRASS_BLOCK.defaultBlockState());
        h.assertTrue(WildNature.root(level, r) && level.getBlockState(r).is(Blocks.DIRT), "a boar roots grass up to dirt");
        h.setBlock(new BlockPos(1, 0, 8), Blocks.DIRT);
        h.setBlock(new BlockPos(1, 1, 8), Blocks.OAK_SAPLING);
        h.assertTrue(WildNature.browse(level, h.absolutePos(new BlockPos(1, 1, 7))) && h.getBlockState(new BlockPos(1, 1, 8)).isAir(), "a deer eats a sapling");

        // D6: troughs side by side join and fill one after the other
        BlockPos a = new BlockPos(6, 1, 6), b = new BlockPos(7, 1, 6);
        trough(h, a);
        trough(h, b);
        h.assertTrue(h.getBlockState(a).getValue(FeedingTroughBlock.EAST) && h.getBlockState(b).getValue(FeedingTroughBlock.WEST), "troughs side by side join");
        h.assertTrue(FeedingTroughBlock.group(level, h.absolutePos(b)).size() == 2, "one trough of two");
        h.assertTrue(FeedingTroughBlock.fillWater(level, h.absolutePos(a)) && FeedingTroughBlock.fillWater(level, h.absolutePos(a)), "two buckets go in");
        h.assertFalse(FeedingTroughBlock.fillWater(level, h.absolutePos(b)), "the third bucket finds it full");
        h.assertTrue(at(h, b).water() == FeedingTroughBlockEntity.WATER_CAP && h.getBlockState(b).getValue(FeedingTroughBlock.WATER) == 3, "the second trough got the second bucket and shows it");
        ItemStack wheat = new ItemStack(Items.WHEAT, 64);
        FeedingTroughBlock.addFood(level, h.absolutePos(b), wheat);
        FeedingTroughBlock.addFood(level, h.absolutePos(b), new ItemStack(Items.WHEAT, 6));
        h.assertTrue(at(h, b).food().getCount() == 64 && at(h, a).food().getCount() == 6 && h.getBlockState(a).getValue(FeedingTroughBlock.FOOD), "fodder spills into the next trough");
        h.assertTrue(FeedingTroughBlock.addFood(level, h.absolutePos(a), new ItemStack(Items.CARROT, 5)) == 0, "a full trough of wheat takes no carrots");

        // a water bucket from a player's hand
        BlockPos c = new BlockPos(10, 1, 10);
        trough(h, c);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        BlockPos ac = h.absolutePos(c);
        h.getBlockState(c).useItemOn(player.getMainHandItem(), level, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(ac), Direction.UP, ac, false));
        h.assertTrue(at(h, c).water() == FeedingTroughBlockEntity.WATER_CAP, "a water bucket fills the trough");
        h.assertTrue(player.getMainHandItem().is(Items.BUCKET), "and comes back empty");
        h.succeed();
    }

    private static void trough(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x <= 12; x++) for (int z = 0; z <= 12; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
        // a joined trough of two with water in the far one and wheat in the near one
        BlockPos a = new BlockPos(5, 1, 5), b = new BlockPos(6, 1, 5);
        trough(h, a);
        trough(h, b);
        at(h, b).setWater(FeedingTroughBlockEntity.WATER_CAP);
        at(h, a).insert(new ItemStack(Items.WHEAT, 10));
        // a cow boxed in right at the trough, out of food and water
        for (BlockPos f : new BlockPos[] {new BlockPos(4, 1, 4), new BlockPos(6, 1, 4), new BlockPos(5, 1, 3)}) h.setBlock(f, Blocks.OAK_FENCE);
        Animal cow = h.spawn(EntityTypes.COW, new BlockPos(5, 1, 4));
        long now = level.getGameTime();
        Husbandry.setNeeds(cow, now - 1, now - 1);
        cow.setInLove(null);
        // a second cow, fed and watered but far from any trough
        Animal fine = h.spawn(EntityTypes.COW, new BlockPos(11, 1, 11));
        Husbandry.setNeeds(fine, now + Husbandry.FOOD_TICKS, now + Husbandry.WATER_TICKS);
        h.assertTrue(Husbandry.tired(cow) && !Husbandry.tired(fine), "out of food and water is tired");
        Husbandry.update(cow);
        h.assertTrue(cow.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(Husbandry.TIRED_ID), "a tired cow walks slowly");
        h.assertFalse(cow.isInLove(), "a tired cow won't breed");
        h.succeedWhen(() -> {
            long t = level.getGameTime();
            Husbandry.Needs n = Husbandry.needs(cow);
            h.assertFalse(n.thirsty(t), "drank at the trough");
            h.assertFalse(n.hungry(t), "ate at the trough");
            h.assertTrue(at(h, b).water() == FeedingTroughBlockEntity.WATER_CAP - 1, "one drink gone");
            h.assertTrue(at(h, a).food().getCount() == 9, "one wheat gone");
            Husbandry.update(cow);
            h.assertFalse(cow.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(Husbandry.TIRED_ID), "fed and watered it walks at full speed again");
            h.assertFalse(Husbandry.tired(fine), "the fed cow stays fine");
        });
    }

    private HusbandryTest() {}
}
