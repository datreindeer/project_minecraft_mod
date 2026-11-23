package com.mechanical.folklore;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import com.mechanical.Coins;
import com.mechanical.Farming;
import com.mechanical.Pottery;
import com.mechanical.TestArea;
import com.mechanical.WaterJugBlock;
import com.mechanical.WaterJugItem;
import com.mechanical.wildlife.underground.Troll;
import com.mechanical.wildlife.underground.Vittra;
import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for the folklore beings (Beings): where each spawns, the spawn rules, Huldra's gift taken and refused, the
 * Tomte's Yule porridge, crops and pinches, Näcken's pull and lesson, the forest troll waking and sleeping again, the
 * Frost Heart and the real beings' drops, Bergtagen's hall and the Vittra's guest, the Dune Wraith's theft and the wisp's
 * trail end.
 */
@EventBusSubscriber(modid = MODID)
public final class BeingsTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new java.util.LinkedHashMap<>();

    static {
        TESTS.put("folklore_spawn_biomes", BeingsTest::spawnBiomes);
        TESTS.put("folklore_spawn_rules", BeingsTest::spawnRules);
        TESTS.put("folklore_huldra", BeingsTest::huldra);
        TESTS.put("folklore_tomte", BeingsTest::tomte);
        TESTS.put("folklore_nacken", BeingsTest::nacken);
        TESTS.put("folklore_forest_troll", BeingsTest::forestTroll);
        TESTS.put("folklore_frost_heart", BeingsTest::frostHeart);
        TESTS.put("folklore_real_beings", BeingsTest::realBeings);
        TESTS.put("folklore_bergtagen", BeingsTest::bergtagen);
        TESTS.put("folklore_dune_wraith", BeingsTest::duneWraith);
        TESTS.put("folklore_wisp", BeingsTest::wisp);
        TESTS.forEach((name, fn) -> TEST_FUNCTIONS.register(name, () -> fn));
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        for (String name : TESTS.keySet()) {
            var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, name), new TestEnvironmentDefinition.AllOf(List.of()));
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name)), TestArea.of(env, 200, true)));
        }
    }

    private static FakePlayer believer(GameTestHelper h, Vec3 rel) {
        FakePlayer p = FakePlayerFactory.get(h.getLevel(), new GameProfile(UUID.randomUUID(), "folk_test"));
        Faith.set(p, Faith.MAX);
        p.setInvulnerable(false); // a fake player is invulnerable, so no mob could target it
        Vec3 at = h.absoluteVec(rel);
        p.setPos(at.x, at.y, at.z);
        return p;
    }

    private static <T extends Mob> T spawn(GameTestHelper h, EntityType<T> type, Vec3 rel) {
        T mob = type.create(h.getLevel(), EntitySpawnReason.COMMAND);
        Vec3 at = h.absoluteVec(rel);
        mob.snapTo(at.x, at.y, at.z, 0, 0);
        mob.setNoAi(true);
        h.getLevel().addFreshEntity(mob);
        return mob;
    }

    private static void floor(GameTestHelper h) {
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) h.setBlock(x, 0, z, Blocks.STONE);
    }

    private static int count(GameTestHelper h, Item item) {
        AABB box = new AABB(Vec3.atCenterOf(h.absolutePos(new BlockPos(-2, -2, -2))), Vec3.atCenterOf(h.absolutePos(new BlockPos(18, 12, 18))));
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(item)).stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static boolean has(FakePlayer p, Item item) {
        return p.getInventory().countItem(item) > 0;
    }

    // ---------------------------------------------------------------- spawns

    private static ResourceKey<Biome> mod(String name) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(MODID, name));
    }

    private static boolean spawnsIn(ServerLevel level, ResourceKey<Biome> key, EntityType<?> type) {
        Biome biome = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(key).value();
        MobSpawnSettings s = biome.getModifiedEnvironmentAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
        return s.getMobsToSpawn(MobCategory.MONSTER).unwrap().stream().anyMatch(w -> w.value().type() == type);
    }

    private static void spawnBiomes(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Map<EntityType<?>, List<ResourceKey<Biome>>> where = Map.of(
                Beings.HULDRA.get(), List.of(Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.DARK_FOREST, mod("deciduous_forest")),
                Beings.WILL_O_WISP.get(), List.of(Biomes.SWAMP, Biomes.DARK_FOREST, mod("peat_bog"), mod("willow_fen")),
                Beings.JUNGLE_SPIRIT_MASK.get(), List.of(Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE, Biomes.SPARSE_JUNGLE),
                Beings.NACKEN.get(), List.of(Biomes.RIVER),
                Beings.BOG_BODY.get(), List.of(mod("peat_bog")),
                Beings.FROST_GIANT.get(), List.of(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES, Biomes.FROZEN_PEAKS),
                Beings.DUNE_WRAITH.get(), List.of(Biomes.DESERT));
        List<ResourceKey<Biome>> never = List.of(Biomes.PLAINS, Biomes.MUSHROOM_FIELDS, Biomes.OCEAN);
        where.forEach((type, biomes) -> {
            for (ResourceKey<Biome> b : biomes) h.assertTrue(spawnsIn(level, b, type), type.getDescriptionId() + " spawns in " + b.identifier());
            for (ResourceKey<Biome> b : never) h.assertFalse(spawnsIn(level, b, type), type.getDescriptionId() + " never in " + b.identifier());
        });
        h.assertFalse(spawnsIn(level, Biomes.DESERT, Beings.HULDRA.get()) || spawnsIn(level, Biomes.JUNGLE, Beings.WILL_O_WISP.get())
                || spawnsIn(level, Biomes.FOREST, Beings.BOG_BODY.get()) || spawnsIn(level, Biomes.DESERT, Beings.FROST_GIANT.get()), "each in its own land");
        h.succeed();
    }

    private static void spawnRules(GameTestHelper h) {
        h.assertTrue(Huldra.rule(true, true, true) && !Huldra.rule(false, true, true) && !Huldra.rule(true, false, true), "huldra: night, on forest soil");
        h.assertTrue(Wisp.rule(true, true, false, 0.1F) && !Wisp.rule(false, true, true, 0.0F) && !Wisp.rule(true, true, false, 0.2F)
                && Wisp.rule(true, true, true, 0.2F), "wisp: night, more in autumn");
        h.assertTrue(Nacken.rule(true, false, true) && !Nacken.rule(true, true, true) && !Nacken.rule(false, false, true) && !Nacken.rule(true, false, false),
                "näcken: moonlit night at the rapids");
        h.assertTrue(BogBody.rule(true, true) && !BogBody.rule(false, true) && !BogBody.rule(true, false), "bog body: night, on peat");
        h.assertTrue(FrostGiant.rule(true, true) && !FrostGiant.rule(false, true), "frost giant: only in a snowstorm");
        h.assertTrue(DuneWraith.rule(true, true, true) && !DuneWraith.rule(false, true, true) && !DuneWraith.rule(true, false, true), "dune wraith: sandstorm, sand");
        h.assertTrue(Bergtagen.canTake(true, 120, true, false, 0.1F) && !Bergtagen.canTake(true, 80, true, false, 0.1F)
                && !Bergtagen.canTake(true, 120, false, false, 0.1F) && !Bergtagen.canTake(true, 120, true, true, 0.1F)
                && !Bergtagen.canTake(false, 120, true, false, 0.1F) && !Bergtagen.canTake(true, 120, true, false, 0.9F), "bergtagen: alone, high, once a year");
        // rapids: still water is not enough, flowing water beside it is
        floor(h);
        h.setBlock(2, 1, 2, Blocks.WATER);
        ServerLevel level = h.getLevel();
        h.assertFalse(Nacken.rapids(level, h.absolutePos(new BlockPos(2, 1, 2))), "still water is no rapids");
        h.setBlock(3, 1, 2, Fluids.FLOWING_WATER.getFlowing(5, false).createLegacyBlock());
        h.assertTrue(Nacken.rapids(level, h.absolutePos(new BlockPos(2, 1, 2))), "flowing water beside: rapids");
        h.succeed();
    }

    // ---------------------------------------------------------------- Huldra

    private static void huldra(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        FakePlayer p = believer(h, new Vec3(3.5, 1, 2.5));
        Huldra her = spawn(h, Beings.HULDRA.get(), new Vec3(2.5, 1, 2.5));
        h.assertFalse(her.hurtServer(level, level.damageSources().generic(), 10) || her.isRemoved(), "nothing hurts her");
        h.assertFalse(her.accept(p), "no gift before she has led you off");
        her.skipLure();
        h.assertTrue(her.accept(p) && has(p, Beings.HULDRA_COMB.get()) && p.hasEffect(MobEffects.LUCK) && her.isRemoved(), "her gift taken: comb and luck");

        FakePlayer q = believer(h, new Vec3(3.5, 1, 4.5));
        Huldra other = spawn(h, Beings.HULDRA.get(), new Vec3(2.5, 1, 4.5));
        other.skipLure();
        other.offerTo(q);
        h.assertFalse(other.walkedAway(q), "still by her");
        Vec3 far = h.absoluteVec(new Vec3(15.5, 1, 15.5));
        q.setPos(far.x, far.y, far.z);
        h.assertTrue(other.walkedAway(q), "walked off from the offered gift");
        other.refused(q);
        h.assertTrue(q.hasEffect(Beings.LOST) && !has(q, Beings.HULDRA_COMB.get()), "refused: lost, no gift");
        FakePlayer blind = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "folk_blind"));
        h.assertFalse(other.seenBy(blind), "an unbeliever does not see her");
        h.succeed();
    }

    // ---------------------------------------------------------------- Tomte

    private static void tomte(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        FakePlayer p = believer(h, new Vec3(2.5, 1, 2.5));
        Tomte t = spawn(h, Beings.TOMTE.get(), new Vec3(8.5, 1, 8.5));
        t.setHomeTo(h.absolutePos(new BlockPos(8, 1, 8)), Tomte.HOME_RADIUS);
        ItemStack porridge = new ItemStack(Farming.PORRIDGE.get());
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, porridge);
        h.assertFalse(t.feed(p, porridge, Tomte.YULE - 2, 5) || porridge.isEmpty(), "porridge outside Yule is left alone");
        h.assertTrue(t.feed(p, porridge, Tomte.YULE, 5) && porridge.isEmpty() && t.fedYear == 5, "Yule porridge eaten");
        h.assertTrue(has(p, Beings.STRAW_YULE_GOAT.get()) && has(p, Items.BOWL), "a straw goat and the bowl back");
        h.assertTrue(Tomte.mood(Tomte.YULE + 1, 5, 0, t.fedYear, 4, -1) == Tomte.Mood.PLEASED, "pleased after Yule");
        // pleased: a crop on the farm grows
        for (int x = 4; x <= 6; x++) {
            h.setBlock(x, 0, 4, Blocks.FARMLAND);
            h.setBlock(x, 1, 4, Blocks.WHEAT);
        }
        h.assertTrue(t.tendCrop(level), "he tends a crop");
        int grown = 0;
        for (int x = 4; x <= 6; x++) grown += ((CropBlock) Blocks.WHEAT).getAge(h.getBlockState(new BlockPos(x, 1, 4)));
        h.assertTrue(grown == 1, "one wheat a step taller, was " + grown);
        // angry: a farm animal is pinched
        Animal cow = spawn(h, EntityTypes.COW, new Vec3(10.5, 1, 10.5));
        float before = cow.getHealth();
        h.assertTrue(t.pinch(level) && cow.getHealth() < before, "an angry tomte pinches the animals");
        h.assertTrue(Tomte.farm(level, h.absolutePos(new BlockPos(5, 1, 5))) == null, "three farmland is no farm");
        for (int x = 2; x <= 7; x++) h.setBlock(x, 0, 6, Blocks.FARMLAND);
        h.assertTrue(Tomte.farm(level, h.absolutePos(new BlockPos(5, 1, 5))) != null, "nine farmland is a farm");
        h.succeed();
    }

    // ---------------------------------------------------------------- Näcken

    private static void nacken(GameTestHelper h) {
        floor(h);
        FakePlayer p = believer(h, new Vec3(2.5, 1, 2.5));
        Nacken n = spawn(h, Beings.NACKEN.get(), new Vec3(10.5, 1, 2.5));
        for (int i = 1; i < Nacken.LISTEN_TICKS; i++) n.listen(p);
        h.assertTrue(p.getDeltaMovement().x <= 0.0001, "listening a while: nothing yet");
        n.listen(p);
        h.assertTrue(p.getDeltaMovement().x > 0.01, "listening too long: drawn toward him, " + p.getDeltaMovement());
        p.setDeltaMovement(Vec3.ZERO);
        ItemStack coin = new ItemStack(Coins.GOLD_COIN.get(), 2);
        h.assertTrue(n.gift(p, coin) && coin.getCount() == 1, "a gift taken");
        h.assertTrue(p.hasEffect(Beings.FIDDLERS_BLESSING) && has(p, Beings.NACKEN_STRING.get()), "his lesson: the blessing and a string");
        for (int i = 0; i < Nacken.LISTEN_TICKS + 5; i++) n.listen(p);
        h.assertTrue(p.getDeltaMovement().x <= 0.0001, "his music pulls a pupil no more");
        h.assertFalse(n.gift(p, coin), "one lesson a year");
        h.assertFalse(n.gift(p, new ItemStack(Items.DIRT)), "dirt is no gift");
        h.succeed();
    }

    // ---------------------------------------------------------------- forest troll

    private static void forestTroll(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        FakePlayer p = believer(h, new Vec3(2.5, 1, 2.5));
        h.setBlock(8, 1, 8, Beings.MOSSY_TROLL_STONE.get());
        h.setBlock(8, 2, 8, Beings.MOSSY_TROLL_STONE.get());
        h.setBlock(4, 1, 4, Blocks.SPRUCE_LOG);
        List<Troll> woke = TrollStone.wakeNear(level, h.absolutePos(new BlockPos(4, 1, 4)), p);
        h.assertTrue(woke.size() == 1 && woke.get(0).entityTags().contains(Troll.FOREST_TAG), "chopping nearby wakes one troll");
        h.assertTrue(h.getBlockState(new BlockPos(8, 1, 8)).isAir() && h.getBlockState(new BlockPos(8, 2, 8)).isAir(), "the stone is gone");
        Troll t = woke.get(0);
        h.assertTrue(t.getTargetUnchecked() == p, "after whoever woke it");
        t.petrify(Beings.MOSSY_TROLL_STONE.get().defaultBlockState());
        BlockPos feet = t.blockPosition();
        h.assertTrue(t.isRemoved() && level.getBlockState(feet).is(Beings.MOSSY_TROLL_STONE.get())
                && level.getBlockState(feet.above()).is(Beings.MOSSY_TROLL_STONE.get()), "sleeps again as mossy stone");
        h.assertTrue(TrollStone.wakeNear(level, h.absolutePos(new BlockPos(14, 1, 14)), p).size() == 1, "and can be woken again");
        h.succeed();
    }

    // ---------------------------------------------------------------- Frost Heart, real beings

    private static void frostHeart(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        h.setBlock(3, 1, 3, Beings.FROST_HEART.get());
        h.assertTrue(Beings.frostHeartNear(level, h.absolutePos(new BlockPos(6, 0, 6))), "ice by the heart stays frozen");
        h.assertFalse(Beings.frostHeartNear(level, h.absolutePos(new BlockPos(16, 0, 16))), "ice far away thaws as usual");
        h.setBlock(3, 1, 3, Blocks.AIR);
        h.assertFalse(Beings.frostHeartNear(level, h.absolutePos(new BlockPos(6, 0, 6))), "the heart taken away, the ice thaws");
        h.succeed();
    }

    private static void realBeings(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        FrostGiant giant = spawn(h, Beings.FROST_GIANT.get(), new Vec3(4.5, 1, 4.5));
        giant.kill(level);
        BogBody body = spawn(h, Beings.BOG_BODY.get(), new Vec3(12.5, 1, 12.5));
        body.kill(level);
        h.runAfterDelay(5, () -> {
            h.assertTrue(count(h, Beings.FROST_HEART_ITEM.get()) == 1, "the frost giant leaves its heart");
            h.assertTrue(count(h, Beings.PEAT_TANNED_HIDE.get()) >= 1, "a bog body leaves peat-tanned hide");
            // no snowstorm: a frost giant goes back into the snow
            FrostGiant g2 = spawn(h, Beings.FROST_GIANT.get(), new Vec3(8.5, 1, 8.5));
            g2.setNoAi(false);
            h.assertFalse(com.mechanical.Thaw.storm(level), "no storm in the test world");
            h.succeedWhen(() -> h.assertTrue(g2.isRemoved(), "the giant leaves with the storm"));
        });
    }

    // ---------------------------------------------------------------- Bergtagen

    private static void bergtagen(GameTestHelper h) {
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) for (int y = 0; y <= 12; y++) h.setBlock(x, y, z, Blocks.STONE);
        ServerLevel level = h.getLevel();
        FakePlayer p = believer(h, new Vec3(1.5, 13, 1.5));
        BlockPos floor = h.absolutePos(new BlockPos(8, 3, 8));
        Bergtagen.take(level, p, floor, h.absolutePos(new BlockPos(0, 12, 0)).getY());
        h.assertTrue(level.getBlockState(floor).isAir() && level.getBlockState(floor.above(3)).isAir() && level.getBlockState(floor.below()).isSolid(),
                "a hall carved in the rock"); // (a fake player is not moved by teleportTo, so its position is not checked)
        h.assertTrue(p.hasEffect(Beings.MOUNTAIN_GUEST), "a guest of the vittra");
        h.assertTrue(level.getBlockEntity(floor.offset(2, 0, 2)) instanceof ChestBlockEntity, "their chest");
        h.assertTrue(level.getBlockState(floor.offset(-2, 6, -2)).is(Blocks.LADDER), "a ladder up the shaft");
        List<Vittra> vittra = level.getEntitiesOfClass(Vittra.class, new AABB(floor).inflate(5));
        h.assertTrue(vittra.size() == Bergtagen.VITTRA, "the vittra about, " + vittra.size());
        vittra.get(0).hear(p);
        h.assertTrue(vittra.get(0).getTargetUnchecked() == null, "no vittra hears its guest");
        p.removeEffect(Beings.MOUNTAIN_GUEST);
        vittra.get(0).hear(p);
        h.assertTrue(vittra.get(0).getTargetUnchecked() == p, "but it would hear anyone else");
        h.succeed();
    }

    // ---------------------------------------------------------------- Dune Wraith

    private static void duneWraith(GameTestHelper h) {
        FakePlayer p = believer(h, new Vec3(2.5, 1, 2.5));
        p.getInventory().setItem(0, PotionContents.createItemStack(Items.POTION, Potions.WATER));
        ItemStack jug = new ItemStack(Pottery.WATER_JUG_ITEM.get());
        jug.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(WaterJugBlock.WATER, WaterJugBlock.MAX));
        p.getInventory().setItem(1, jug);
        h.assertTrue(DuneWraith.steal(p) && p.getInventory().getItem(0).is(Items.GLASS_BOTTLE), "the water bottle stolen");
        h.assertTrue(DuneWraith.steal(p) && WaterJugItem.water(p.getInventory().getItem(1)) == 0, "the jug drunk dry");
        h.assertFalse(DuneWraith.steal(p), "nothing left to steal");
        DuneWraith w = spawn(h, Beings.DUNE_WRAITH.get(), new Vec3(6.5, 1, 6.5));
        w.struck(h.getLevel(), p);
        h.runAfterDelay(2, () -> {
            h.assertTrue(w.isRemoved() && count(h, Beings.DUNE_GLASS.get()) == 1, "struck, it scatters into Dune Glass");
            h.succeed();
        });
    }

    // ---------------------------------------------------------------- wisp and mask

    private static void wisp(GameTestHelper h) {
        floor(h);
        ServerLevel level = h.getLevel();
        FakePlayer p = believer(h, new Vec3(2.5, 1, 2.5));
        Wisp astray = spawn(h, Beings.WILL_O_WISP.get(), new Vec3(4.5, 2, 4.5));
        astray.choose(false, astray.position());
        astray.arrive(level, List.of(p));
        h.assertTrue(p.hasEffect(Beings.LOST) && astray.isRemoved(), "led astray: lost");
        BlockPos chest = Wisp.bogGrave(level, h.absolutePos(new BlockPos(10, 1, 10)));
        h.assertTrue(level.getBlockEntity(chest) instanceof ChestBlockEntity c && Wisp.BOG_GRAVE.equals(c.getLootTable()), "a bog grave with its chest");
        Wisp mask = spawn(h, Beings.JUNGLE_SPIRIT_MASK.get(), new Vec3(6.5, 2, 12.5));
        h.assertTrue(mask.mask() && mask.sight() > astray.sight(), "the mask is harder to see");
        mask.choose(true, mask.position());
        mask.arrive(level, List.of(p));
        h.runAfterDelay(2, () -> {
            h.assertTrue(count(h, Beings.SPIRIT_MASK.get()) == 1, "the mask's treasure: a Spirit Mask");
            FakePlayer q = believer(h, new Vec3(1.5, 1, 1.5));
            Faith.set(q, 0);
            float bare = Faith.sight(q);
            q.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, Beings.SPIRIT_MASK.toStack());
            h.assertTrue(Faith.sight(q) == bare + Faith.MASK_SIGHT, "the worn mask sharpens sight");
            h.succeed();
        });
    }

    private BeingsTest() {}
}
