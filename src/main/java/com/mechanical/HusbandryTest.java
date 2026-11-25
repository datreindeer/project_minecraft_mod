package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.wildlife.WildNature;
import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.Taming;
import com.mechanical.wildlife.Mouflon;
import com.mechanical.wildlife.Hare;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntitySpawnReason;
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
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Living-world plan D5, D6, A10. husbandry_ground: hooves churn grass to dirt, coarse dirt, mud in the rain and packed mud
 * when dry; a hungry cow grazing on grass is fed; a boar roots grass up to dirt; a deer browses a sapling; troughs side
 * by side join, fill one after the other and share fodder; a water bucket fills a trough. husbandry_trough: a cow out of food
 * and water is tired (slow, no love); boxed in at a joined trough it drinks and eats from it and is not tired after.
 * husbandry_taming: a joined row shares one pool of water and shows one level, through breaking and rejoining, losing
 * none; a wild animal is tamed with its favourite food by a crouching player. husbandry_breed: a content tamed pair with a
 * stocked trough has a tame young by itself; feeding from the hand never puts anything in love.
 */
@EventBusSubscriber(modid = MODID)
public final class HusbandryTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> GROUND = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "husbandry_ground"));
    private static final ResourceKey<Consumer<GameTestHelper>> TROUGH = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "husbandry_trough"));

    private static final ResourceKey<Consumer<GameTestHelper>> TAMING = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "husbandry_taming"));
    private static final ResourceKey<Consumer<GameTestHelper>> BREED = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "husbandry_breed"));

    static {
        TEST_FUNCTIONS.register("husbandry_taming", () -> HusbandryTest::taming);
        TEST_FUNCTIONS.register("husbandry_breed", () -> HusbandryTest::breed);
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
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "husbandry_taming"), new FunctionGameTestInstance(TAMING, TestArea.of(env, 600, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "husbandry_breed"), new FunctionGameTestInstance(BREED, TestArea.of(env, 600, true)));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "husbandry_trough"), new FunctionGameTestInstance(TROUGH, TestArea.of(env, 400, true)));
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    /** Every trough of the row shows this water level, and holds no more than its share of {@code total}. */
    private static void shows(GameTestHelper h, int level, int total, BlockPos... row) {
        int sum = 0;
        for (BlockPos p : row) {
            h.assertTrue(h.getBlockState(p).getValue(FeedingTroughBlock.WATER) == level, "level " + level + " at " + p + ", got " + h.getBlockState(p).getValue(FeedingTroughBlock.WATER));
            h.assertTrue(at(h, p).water() <= (total + row.length - 1) / row.length, "an even share at " + p);
            sum += at(h, p).water();
        }
        h.assertTrue(sum == total, total + " drinks in the row, got " + sum);
    }

    private static void taming(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        BlockPos a = new BlockPos(1, 1, 1), b = new BlockPos(2, 1, 1), c = new BlockPos(3, 1, 1);
        trough(h, a);
        trough(h, b);
        trough(h, c);
        h.assertTrue(FeedingTroughBlock.fillWater(level, h.absolutePos(a)), "a bucket goes in");
        shows(h, 1, FeedingTroughBlockEntity.WATER_CAP, a, b, c); // one bucket over three troughs
        h.assertTrue(FeedingTroughBlock.fillWater(level, h.absolutePos(c)) && FeedingTroughBlock.fillWater(level, h.absolutePos(b))
                && !FeedingTroughBlock.fillWater(level, h.absolutePos(a)), "a row of three takes three buckets");
        shows(h, 3, 48, a, b, c);
        for (int i = 0; i < 32; i++) h.assertTrue(FeedingTroughBlock.drink(level, h.absolutePos(a)), "drinks");
        shows(h, 1, 16, a, b, c);
        FeedingTroughBlock.addFood(level, h.absolutePos(a), new ItemStack(Items.WHEAT, 3));
        for (BlockPos p : new BlockPos[] {a, b, c}) h.assertTrue(h.getBlockState(p).getValue(FeedingTroughBlock.FOOD), "the whole row shows fodder");
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos far = h.absolutePos(c);
        h.getBlockState(c).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(far), Direction.UP, far, false));
        h.assertTrue(player.getInventory().countItem(Items.WHEAT) == 3 && !h.getBlockState(a).getValue(FeedingTroughBlock.FOOD), "an empty hand takes it back from either end");

        // break the middle one: its water stays in the row's ends
        h.setBlock(b, Blocks.AIR);
        h.assertTrue(at(h, a).water() + at(h, c).water() == 16, "no water lost or made by breaking, got " + (at(h, a).water() + at(h, c).water()));
        h.runAfterDelay(3, () -> {
            trough(h, b);
            h.runAfterDelay(3, () -> shows(h, 1, 16, a, b, c));
        });

        // taming: standing up it shies, crouching it eats; hare takes three feedings with chewing between
        Hare hare = h.spawn(Wildlife.HARE.get(), new BlockPos(8, 1, 8));
        hare.setNoAi(true);
        Taming.Diet diet = Taming.diet(hare.getType());
        ItemStack carrots = new ItemStack(Items.CARROT, 10);
        h.assertTrue(diet != null && carrots.is(diet.food()) && Husbandry.fodder(carrots), "a hare likes carrots, and a trough takes them");
        h.assertTrue(Husbandry.fodder(new ItemStack(Items.BROWN_MUSHROOM)), "a trough takes reindeer food");
        h.assertFalse(Husbandry.livestock(hare), "a wild hare is not livestock");
        player.setItemInHand(InteractionHand.MAIN_HAND, carrots);
        hare.tameFeed(player, carrots, diet);
        h.assertTrue(hare.tameTrust() == 0 && carrots.getCount() == 10, "it shies from someone standing");
        player.setPose(Pose.CROUCHING);
        h.assertTrue(hare.calmAround(player), "calm round a crouching player with carrots");
        hare.mobInteract(player, InteractionHand.MAIN_HAND);
        hare.mobInteract(player, InteractionHand.MAIN_HAND);
        h.assertTrue(hare.tameTrust() == 1 && carrots.getCount() == 9 && !hare.isInLove(), "one feeding, then it chews");

        Animal boar = h.spawn(Wildlife.WILD_BOAR.get(), new BlockPos(12, 1, 12));
        boar.setNoAi(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.POTATO));
        boar.setTarget(player);
        h.assertTrue(boar.getTarget() == null, "a boar doesn't charge someone crouching with its food");
        player.setPose(Pose.STANDING);
        boar.setTarget(player);
        h.assertTrue(boar.getTarget() == player, "a wild one may charge");
        ((WildAnimal) boar).setTamed();
        boar.setTarget(player);
        h.assertTrue(boar.getTarget() == null && Husbandry.livestock(boar) && !Husbandry.grazer(boar) && boar.isPersistenceRequired(), "a tamed boar turns on no one");
        AnimalFear.panic(boar, boar.position());
        h.assertFalse(AnimalFear.isPanicking(boar), "nor does it panic");

        for (String id : Taming.species()) {
            var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.fromNamespaceAndPath(MODID, id));
            var made = type.create(level, EntitySpawnReason.COMMAND);
            h.assertTrue(made instanceof WildAnimal w && BuiltInRegistries.ITEM.getTagOrEmpty(Taming.diet(w.getType()).food()).iterator().hasNext(), id + " exists and has food");
        }

        int[] fed = {1};
        h.succeedWhen(() -> {
            if (!hare.isTamed()) {
                player.setPose(Pose.CROUCHING);
                player.setItemInHand(InteractionHand.MAIN_HAND, carrots);
                if (h.getTick() % 10 == 0) hare.mobInteract(player, InteractionHand.MAIN_HAND);
            }
            h.assertTrue(hare.isTamed() && Husbandry.livestock(hare) && carrots.getCount() == 7, "tamed after three carrots");
            h.assertTrue(h.getTick() >= 2 * Taming.FEED_COOLDOWN, "with chewing between");
            TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            hare.saveWithoutId(out);
            Hare loaded = Wildlife.HARE.get().create(level, EntitySpawnReason.LOAD);
            loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
            h.assertTrue(loaded.isTamed(), "stays tame after saving");
        });
    }

    private static void breed(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h);
        for (int i = 4; i <= 11; i++) {
            for (BlockPos f : new BlockPos[] {new BlockPos(i, 1, 4), new BlockPos(i, 1, 11), new BlockPos(4, 1, i), new BlockPos(11, 1, i)}) h.setBlock(f, Blocks.OAK_FENCE);
        }
        WildAnimal ewe = h.spawn(Wildlife.MOUFLON.get(), new BlockPos(6, 1, 7)), ram = h.spawn(Wildlife.MOUFLON.get(), new BlockPos(8, 1, 7));
        ewe.setAge(0);
        ram.setAge(0);
        ewe.setTamed();
        ram.setTamed();
        long now = level.getGameTime();
        Husbandry.setNeeds(ewe, now + Husbandry.FOOD_TICKS, now + Husbandry.WATER_TICKS);
        Husbandry.setNeeds(ram, now + Husbandry.FOOD_TICKS, now + Husbandry.WATER_TICKS);
        h.assertTrue("no_water".equals(Husbandry.lack(ewe)) && !Husbandry.tryBreed(ewe), "no trough, no young: " + Husbandry.lack(ewe));
        BlockPos t = new BlockPos(7, 1, 9);
        trough(h, t);
        FeedingTroughBlock.fillWater(level, h.absolutePos(t));
        h.assertTrue("no_food".equals(Husbandry.lack(ewe)), "an empty trough of fodder: " + Husbandry.lack(ewe));
        at(h, t).insert(new ItemStack(Items.WHEAT, 20));
        h.assertTrue(Husbandry.lack(ewe) == null, "content: " + Husbandry.lack(ewe));

        // from the hand: nothing ever falls in love
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        Animal cow = h.spawn(EntityTypes.COW, new BlockPos(14, 1, 14));
        Husbandry.setNeeds(cow, now + Husbandry.FOOD_TICKS, now + Husbandry.WATER_TICKS);
        for (Animal animal : new Animal[] {ewe, cow}) {
            ItemStack wheat = new ItemStack(Items.WHEAT, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, wheat);
            h.assertTrue(CommonHooks.onInteractEntity(player, animal, InteractionHand.MAIN_HAND, animal.position()) != null && !animal.isInLove() && wheat.getCount() == 2, "a fed animal leaves the wheat");
            Husbandry.setNeeds(animal, now + 100, now + Husbandry.WATER_TICKS);
            CommonHooks.onInteractEntity(player, animal, InteractionHand.MAIN_HAND, animal.position());
            h.assertTrue(!animal.isInLove() && wheat.getCount() == 1 && !Husbandry.needs(animal).hungry(now), "a hungry one just eats it");
        }
        cow.discard();

        h.assertTrue(Husbandry.tryBreed(ewe) && ewe.isInLove() && ram.isInLove(), "a content pair falls in love by itself");
        h.succeedWhen(() -> {
            var young = level.getEntitiesOfClass(Mouflon.class, ewe.getBoundingBox().inflate(10), Mouflon::isBaby);
            h.assertTrue(young.size() == 1 && young.get(0).isTamed(), "a tame lamb is born");
            h.assertTrue(ewe.getData(Husbandry.RESTING) > level.getGameTime() && "resting".equals(Husbandry.lack(ewe)), "the parents rest: " + Husbandry.lack(ewe));
        });
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

        // D6: troughs side by side join and share one pool
        BlockPos a = new BlockPos(6, 1, 6), b = new BlockPos(7, 1, 6);
        trough(h, a);
        trough(h, b);
        h.assertTrue(h.getBlockState(a).getValue(FeedingTroughBlock.EAST) && h.getBlockState(b).getValue(FeedingTroughBlock.WEST), "troughs side by side join");
        h.assertTrue(FeedingTroughBlock.group(level, h.absolutePos(b)).size() == 2, "one trough of two");
        h.assertTrue(FeedingTroughBlock.fillWater(level, h.absolutePos(a)) && FeedingTroughBlock.fillWater(level, h.absolutePos(a)), "two buckets go in");
        h.assertFalse(FeedingTroughBlock.fillWater(level, h.absolutePos(b)), "the third bucket finds it full");
        h.assertTrue(at(h, b).water() == FeedingTroughBlockEntity.WATER_CAP && h.getBlockState(b).getValue(FeedingTroughBlock.WATER) == 3, "two buckets fill a row of two");
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
            h.assertTrue(FeedingTroughBlock.water(level, h.absolutePos(a)) == FeedingTroughBlockEntity.WATER_CAP - 1, "one drink gone");
            h.assertTrue(at(h, a).food().getCount() == 9, "one wheat gone");
            Husbandry.update(cow);
            h.assertFalse(cow.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(Husbandry.TIRED_ID), "fed and watered it walks at full speed again");
            h.assertFalse(Husbandry.tired(fine), "the fed cow stays fine");
        });
    }

    private HusbandryTest() {}
}
