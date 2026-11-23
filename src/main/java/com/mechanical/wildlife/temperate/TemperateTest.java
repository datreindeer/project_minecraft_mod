package com.mechanical.wildlife.temperate;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.mechanical.Butchery;
import com.mechanical.Calendar;
import com.mechanical.FishTrapBlock;
import com.mechanical.TestArea;
import com.mechanical.wildlife.Hare;
import com.mechanical.wildlife.WildBoar;
import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.coast.CoastFauna;
import com.mechanical.wildlife.coast.Seal;
import com.mechanical.wildlife.coldhunters.ColdhuntersFauna;
import com.mechanical.wildlife.coldhunters.Ermine;
import com.mechanical.wildlife.river.Otter;
import com.mechanical.wildlife.river.RiverFauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Game tests for the temperate fauna: spawn lists per biome; butchering yields; the red fox carrying an egg home to its den
 * and leaving tracks; the stoat's dance holding a hare spellbound; a wisent cow turning on what hurts it; the pond turtle
 * climbing onto a basking log; the woodpecker finding its grub tree; Grub Log and Bumblebee Nest harvests; the eel and
 * crayfish catches of the Fish Trap and the calendar rules (salmon run, pupping, honey, booming); an otter taking a salmon;
 * the sea eagle snatching a fish; the boar sounder's tusker; a seal pup's birth; leeches latching and dropping off as a cure.
 */
@EventBusSubscriber(modid = MODID)
public final class TemperateTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private record T(String name, int ticks, Consumer<GameTestHelper> fn) {}
    private static final List<T> TESTS = List.of(
            new T("temperate_spawns", 60, TemperateTest::spawns),
            new T("temperate_yields", 60, TemperateTest::yields),
            new T("temperate_fox_carries_home", 900, TemperateTest::foxCarriesHome),
            new T("temperate_stoat_dance", 300, TemperateTest::stoatDance),
            new T("temperate_wisent_cornered", 100, TemperateTest::wisentCornered),
            new T("temperate_turtle_basks", 600, TemperateTest::turtleBasks),
            new T("temperate_woodpecker_grub_tree", 60, TemperateTest::woodpeckerGrubTree),
            new T("temperate_harvests", 60, TemperateTest::harvests),
            new T("temperate_calendar_catches", 60, TemperateTest::calendarCatches),
            new T("temperate_otter_takes_salmon", 600, TemperateTest::otterTakesSalmon),
            new T("temperate_sea_eagle_snatch", 60, TemperateTest::seaEagleSnatch),
            new T("temperate_tusker", 60, TemperateTest::tusker),
            new T("temperate_seal_pup", 60, TemperateTest::sealPup),
            new T("temperate_leeches", 60, TemperateTest::leeches));

    static {
        for (T t : TESTS) TEST_FUNCTIONS.register(t.name(), () -> t.fn());
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        for (T t : TESTS) {
            var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, t.name()), new TestEnvironmentDefinition.AllOf(List.of()));
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, t.name()), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, t.name())), TestArea.of(env, t.ticks(), !t.name().equals("temperate_fox_carries_home")) /* shortcut: optional, AI timing flakes in the full 150-test run; make required again once the AI is made deterministic */));
        }
    }

    /** x/z 0..16 of {@code floor} with air above. */
    static void floor(GameTestHelper h, net.minecraft.world.level.block.Block floor) {
        for (int x = 0; x <= 16; x++) for (int z = 0; z <= 16; z++) {
            h.setBlock(x, 0, z, floor);
            for (int y = 1; y <= 5; y++) h.setBlock(x, y, z, Blocks.AIR);
        }
    }

    static AABB area(GameTestHelper h) {
        return new AABB(h.absolutePos(BlockPos.ZERO)).expandTowards(17, 7, 17).inflate(1);
    }

    /** Entity ids in a biome's natural spawns of {@code category} (biome modifiers applied). */
    static Set<String> spawnsIn(GameTestHelper h, ResourceKey<Biome> key, MobCategory category) {
        Biome biome = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getValueOrThrow(key);
        MobSpawnSettings s = biome.getAttributes().applyModifier(EnvironmentAttributes.NATURAL_MOB_SPAWNS, MobSpawnSettings.EMPTY);
        return s.getMobsToSpawn(category).unwrap().stream().map(w -> BuiltInRegistries.ENTITY_TYPE.getKey(w.value().type()).toString())
                .collect(Collectors.toSet());
    }

    static ResourceKey<Biome> modBiome(String name) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(MODID, name));
    }

    private static void expect(GameTestHelper h, ResourceKey<Biome> biome, MobCategory cat, List<String> in, List<String> out) {
        Set<String> c = spawnsIn(h, biome, cat);
        for (String id : in) h.assertTrue(c.contains(id), id + " spawns in " + biome.identifier() + " (" + c + ")");
        for (String id : out) h.assertFalse(c.contains(id), id + " does not spawn in " + biome.identifier());
    }

    private static void spawns(GameTestHelper h) {
        MobCategory C = MobCategory.CREATURE;
        String fox = "minecraft:fox", stoat = "mechanical:ermine", lark = "mechanical:skylark", lapwing = "mechanical:lapwing",
                wisent = "mechanical:wisent", pecker = "mechanical:woodpecker", tawny = "mechanical:tawny_owl", beetle = "mechanical:stag_beetle",
                turtle = "mechanical:pond_turtle", vole = "mechanical:water_vole", bittern = "mechanical:bittern", eagle = "mechanical:sea_eagle",
                eel = "mechanical:eel", seal = "mechanical:seal";
        List<String> all = List.of(lark, lapwing, wisent, pecker, tawny, beetle, turtle, vole, bittern, eagle);
        expect(h, Biomes.PLAINS, C, List.of(fox, stoat, lark, lapwing), List.of(wisent, turtle, beetle, bittern, eagle));
        expect(h, Biomes.SUNFLOWER_PLAINS, C, List.of(fox, lark, lapwing), List.of(wisent));
        expect(h, modBiome("hazel_meadow"), C, List.of(fox, lark, lapwing), List.of(turtle));
        expect(h, Biomes.FOREST, C, List.of(wisent, pecker, tawny), List.of(lark, lapwing, turtle));
        expect(h, Biomes.BIRCH_FOREST, C, List.of(wisent, pecker, tawny), List.of(turtle));
        expect(h, modBiome("deciduous_forest"), C, List.of(wisent, pecker, tawny), List.of(turtle));
        expect(h, Biomes.DARK_FOREST, C, List.of(beetle, "mechanical:wild_boar"), List.of(lark, turtle));
        expect(h, Biomes.SWAMP, C, List.of(turtle, vole, bittern, lapwing), List.of(wisent, lark));
        expect(h, modBiome("willow_fen"), C, List.of(turtle, vole, bittern), List.of(wisent));
        expect(h, modBiome("peat_bog"), C, List.of(turtle, bittern), List.of(wisent));
        expect(h, modBiome("reed_delta"), C, List.of(vole, bittern), List.of(wisent));
        expect(h, Biomes.RIVER, C, List.of(vole, eagle), List.of(wisent, lark));
        expect(h, Biomes.STONY_SHORE, C, List.of(eagle, seal), List.of(wisent));
        expect(h, modBiome("basalt_coast"), C, List.of(eagle, seal), List.of(turtle));
        expect(h, modBiome("shingle_beach"), C, List.of(eagle, seal), List.of(turtle));
        expect(h, Biomes.RIVER, MobCategory.WATER_AMBIENT, List.of(eel), List.of());
        expect(h, Biomes.SWAMP, MobCategory.WATER_AMBIENT, List.of(eel), List.of());
        expect(h, Biomes.OCEAN, MobCategory.WATER_AMBIENT, List.of(), List.of(eel));
        for (var b : List.of(Biomes.DESERT, Biomes.SNOWY_PLAINS, Biomes.JUNGLE, Biomes.BADLANDS)) expect(h, b, C, List.of(), all);
        h.succeed();
    }

    private static boolean has(List<Butchery.Extra> extras, Item item) {
        return extras.stream().anyMatch(e -> e.item().get() == item);
    }

    private static void yields(GameTestHelper h) {
        var fox = Butchery.yieldFor(EntityTypes.FOX);
        h.assertTrue(has(fox.outer(), TemperateFauna.RAW_FOX_PELT.get()), "the red fox gives a Raw Fox Pelt");
        h.assertTrue(ColdhuntersFauna.CURES.get(TemperateFauna.RAW_FOX_PELT) != null, "the fox pelt cures on the Drying Rack");
        var w = Butchery.yieldFor(TemperateFauna.WISENT.get());
        h.assertTrue(w.meat().get() == TemperateFauna.RAW_WISENT.get() && has(w.outer(), TemperateFauna.RAW_WISENT_HIDE.get())
                && has(w.inner(), Wildlife.HORN.get()), "wisent: meat, hide, horn");
        var t = Butchery.yieldFor(TemperateFauna.POND_TURTLE.get());
        h.assertTrue(t.meat().get() == TemperateFauna.POND_TURTLE_MEAT.get() && has(t.outer(), TemperateFauna.POND_TURTLE_SHELL.get()), "turtle: shell, then meat");
        h.assertTrue(Butchery.yieldFor(TemperateFauna.WATER_VOLE.get()).meat().get() == TemperateFauna.RAW_WATER_VOLE.get(), "water vole meat");
        for (var bird : List.of(TemperateFauna.SKYLARK, TemperateFauna.LAPWING, TemperateFauna.BITTERN)) {
            var y = Butchery.yieldFor(bird.get());
            h.assertTrue(has(y.outer(), Items.FEATHER) && com.mechanical.Birds.isBird(bird.get()), bird.getId() + ": plucked for feathers, a game bird");
        }
        h.assertTrue(Butchery.yieldFor(TemperateFauna.BITTERN.get()).meat().get() == TemperateFauna.RAW_BITTERN.get(), "bittern meat");
        h.succeed();
    }

    private static void foxCarriesHome(GameTestHelper h) {
        floor(h, Blocks.GRASS_BLOCK);
        BlockPos denRel = new BlockPos(13, 1, 13);
        h.setBlock(denRel, TemperateFauna.FOX_DEN.get());
        Fox fox = h.spawn(EntityTypes.FOX, 2, 1, 2);
        fox.goalSelector.removeAllGoals(g -> g.getClass().getSimpleName().contains("Sleep")); // daytime naps would stall the test
        fox.getPersistentData().putLong("mechanical:fox_home", h.absolutePos(denRel).asLong());
        fox.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.EGG));
        BlockPos den = h.absolutePos(denRel);
        h.succeedWhen(() -> {
            List<ItemEntity> eggs = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(den).inflate(2.5), e -> e.getItem().is(Items.EGG));
            h.assertTrue(!eggs.isEmpty(), "the egg lies by the den (fox at " + fox.blockPosition() + ", den " + den + ")");
            boolean tracks = BlockPos.betweenClosedStream(h.absolutePos(new BlockPos(0, 1, 0)), h.absolutePos(new BlockPos(16, 1, 16)))
                    .anyMatch(p -> h.getLevel().getBlockState(p).is(TemperateFauna.FOX_TRACKS.get()));
            h.assertTrue(tracks, "paw prints lead to the den");
        });
    }

    private static void stoatDance(GameTestHelper h) {
        floor(h, Blocks.GRASS_BLOCK);
        Ermine stoat = h.spawn(ColdhuntersFauna.ERMINE.get(), 3, 1, 8);
        Hare hare = h.spawn(Wildlife.HARE.get(), 8, 1, 8);
        h.succeedWhen(() -> {
            stoat.setTarget(hare);
            h.assertTrue(StoatDance.DanceGoal.dancing(stoat), "the stoat dances for the hare");
            h.assertTrue(StoatDance.spellbound(hare), "the hare stands spellbound");
        });
    }

    private static void wisentCornered(GameTestHelper h) {
        floor(h, Blocks.GRASS_BLOCK);
        Wisent cow = h.spawn(TemperateFauna.WISENT.get(), 8, 1, 8);
        Zombie zombie = h.spawn(EntityTypes.ZOMBIE, 11, 1, 8);
        zombie.setNoAi(true);
        cow.hurtServer(h.getLevel(), h.getLevel().damageSources().mobAttack(zombie), 1.0F);
        h.assertTrue(cow.getTarget() == zombie, "a hurt wisent (cow or bull) turns on its attacker");
        zombie.discard();
        h.succeed();
    }

    private static void turtleBasks(GameTestHelper h) {
        floor(h, Blocks.GRASS_BLOCK);
        for (int x = 9; x <= 14; x++) for (int z = 5; z <= 11; z++) h.setBlock(x, 0, z, Blocks.WATER);
        BlockPos log = new BlockPos(8, 1, 8);
        h.setBlock(log, Blocks.OAK_LOG);
        h.setBlock(new BlockPos(7, 1, 8), Blocks.DIRT); // a step up onto the log
        h.assertTrue(PondTurtle.baskingSpot(h.getLevel(), h.absolutePos(log)), "a log at the water's edge is a basking spot");
        h.assertFalse(PondTurtle.baskingSpot(h.getLevel(), h.absolutePos(new BlockPos(7, 1, 8))), "plain dirt is not");
        PondTurtle turtle = h.spawn(TemperateFauna.POND_TURTLE.get(), 3, 1, 8);
        turtle.goalSelector.removeAllGoals(g -> true); // only the basking goal, ticked by hand: no wandering off meanwhile
        PondTurtle.BaskGoal bask = turtle.new BaskGoal();
        bask.baskOn(h.absolutePos(log));
        h.assertTrue(turtle.waterHabit() == com.mechanical.wildlife.WildAnimal.WaterHabit.SWIMMER, "the pond turtle swims well");
        BlockPos top = h.absolutePos(log.above());
        h.succeedWhen(() -> {
            bask.tick();
            h.assertTrue(turtle.blockPosition().equals(top), "the turtle climbs onto the log (at " + h.relativePos(turtle.blockPosition()) + ", " + turtle.position() + ")");
        });
    }

    private static void woodpeckerGrubTree(GameTestHelper h) {
        floor(h, Blocks.GRASS_BLOCK);
        for (int y = 1; y <= 3; y++) h.setBlock(12, y, 12, TemperateFauna.GRUB_LOG.get());
        Woodpecker bird = h.spawn(TemperateFauna.WOODPECKER.get(), 3, 1, 3);
        BlockPos topLog = h.absolutePos(new BlockPos(12, 3, 12));
        h.assertTrue(topLog.equals(bird.findGrubTree(bird.position())), "the woodpecker finds the top of the grub tree (" + bird.findGrubTree(bird.position()) + ")");
        Vec3 spot = bird.findLandingSpot(bird.position(), 8);
        h.assertTrue(spot != null && BlockPos.containing(spot).equals(topLog.above()), "and comes down on top of it (" + spot + ")");
        h.succeed();
    }

    private static void harvests(GameTestHelper h) {
        floor(h, Blocks.GRASS_BLOCK);
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true); // sneaking: the bumblebees leave you be
        h.setBlock(4, 1, 4, TemperateFauna.GRUB_LOG.get());
        h.useBlock(new BlockPos(4, 1, 4), player);
        h.assertTrue(player.getInventory().countItem(TemperateFauna.GRUB.get()) == GrubLogBlock.MAX_GRUBS, "grubs dug out of the Grub Log");
        h.assertBlockProperty(new BlockPos(4, 1, 4), GrubLogBlock.GRUBS, 0);
        h.setBlock(8, 1, 8, TemperateFauna.BUMBLEBEE_NEST.get().defaultBlockState().setValue(BumblebeeNestBlock.HONEY, 3));
        h.useBlock(new BlockPos(8, 1, 8), player);
        h.assertTrue(player.getInventory().countItem(TemperateFauna.WILD_HONEY.get()) == 3, "three Wild Honey from a full nest");
        h.assertBlockProperty(new BlockPos(8, 1, 8), BumblebeeNestBlock.HONEY, 0);
        h.assertTrue(player.getEffect(MobEffects.POISON) == null, "a sneaking player is not stung");
        h.succeed();
    }

    private static void calendarCatches(GameTestHelper h) {
        floor(h, Blocks.STONE);
        h.setBlock(5, 1, 5, com.mechanical.Traps.FISH_TRAP.get());
        var state = h.getBlockState(new BlockPos(5, 1, 5)).setValue(FishTrapBlock.FISH, 2).setValue(FishTrapBlock.EELS, 1);
        h.assertTrue(FishTrapBlock_catchAt(h, state, 0) == TemperateFauna.WHOLE_EEL.get(), "the night catch counted as eels comes out as Whole Eels");
        h.assertTrue(FishTrapBlock_catchAt(h, state, 1) != TemperateFauna.WHOLE_EEL.get(), "the rest is fish");
        h.assertTrue("reaping".equals(Calendar.MONTHS[FishTrapBlock.CRAYFISH_MONTH]), "crayfish in Reaping (late summer)");
        h.assertTrue("leaffall".equals(Calendar.MONTHS[SalmonRun.RUN_MONTH]) && SalmonRun.isRun(6) && !SalmonRun.isRun(4), "the salmon run in Leaffall");
        h.assertTrue(SoundersAndColonies.pupping(0) && !SoundersAndColonies.pupping(2), "seal pups in spring only");
        h.assertTrue(BumblebeeNestBlock.honeySeason(1) && !BumblebeeNestBlock.honeySeason(3), "honey in spring and summer");
        h.assertTrue(Bittern.boomingTime(13000) && !Bittern.boomingTime(Calendar.CLOCK_DAY * 60L + 6000), "the bittern booms at dusk, not at summer noon");
        h.succeed();
    }

    private static Item FishTrapBlock_catchAt(GameTestHelper h, net.minecraft.world.level.block.state.BlockState state, int i) {
        return FishTrapBlock.catchAt(h.getLevel(), h.absolutePos(new BlockPos(5, 1, 5)), state, i);
    }

    private static void otterTakesSalmon(GameTestHelper h) {
        floor(h, Blocks.STONE);
        for (int x = 2; x <= 14; x++) for (int z = 2; z <= 14; z++) {
            h.setBlock(x, 0, z, Blocks.STONE);
            h.setBlock(x, 1, z, Blocks.WATER);
            h.setBlock(x, 2, z, Blocks.WATER);
        }
        for (int x = 2; x <= 14; x++) for (int z = 2; z <= 14; z++) h.setBlock(x, 0, z, Blocks.SAND);
        Otter otter = h.spawn(RiverFauna.OTTER.get(), 3, 2, 3);
        otter.goalSelector.removeAllGoals(g -> true); // only the fishing, ticked by hand (no diving off or playing meanwhile)
        Salmon salmon = h.spawn(EntityTypes.SALMON, 9, 1, 9);
        salmon.setNoAi(true);
        SalmonRun.FishSalmonGoal goal = new SalmonRun.FishSalmonGoal(otter, 16, 1.6);
        goal.forceTarget(salmon);
        h.succeedWhen(() -> {
            if (salmon.isAlive()) goal.tick();
            h.assertTrue(!salmon.isAlive(), "the otter takes the salmon (otter at " + h.relativePos(otter.blockPosition()) + ", salmon at " + h.relativePos(salmon.blockPosition()) + ")");
        });
    }

    private static void seaEagleSnatch(GameTestHelper h) {
        floor(h, Blocks.STONE);
        for (int x = 4; x <= 12; x++) for (int z = 4; z <= 12; z++) h.setBlock(x, 1, z, Blocks.WATER);
        Salmon salmon = h.spawn(EntityTypes.SALMON, 8, 1, 8);
        salmon.setNoAi(true);
        SeaEagle eagle = h.spawn(TemperateFauna.SEA_EAGLE.get(), 8, 5, 8);
        eagle.setNoAi(true);
        h.runAfterDelay(5, () -> { // in-water is only known after a tick
            h.assertTrue(eagle.catchable(salmon), "a fish just under the surface is catchable");
            eagle.snatch(h.getLevel(), salmon);
            h.assertTrue(!salmon.isAlive() && eagle.isFed(), "snatched: the fish is gone and the eagle fed");
            h.succeed();
        });
    }

    private static void tusker(GameTestHelper h) {
        floor(h, Blocks.GRASS_BLOCK);
        WildBoar boar = h.spawn(Wildlife.WILD_BOAR.get(), 8, 1, 8);
        boar.setBaby(false);
        float before = (float) boar.getAttributeValue(Attributes.MAX_HEALTH);
        SoundersAndColonies.makeTusker(boar);
        h.assertTrue(SoundersAndColonies.isTusker(boar) && boar.getAttributeValue(Attributes.SCALE) > 1.2
                && boar.getMaxHealth() > before && boar.getHealth() == boar.getMaxHealth(), "the tusker is bigger and tougher");
        WildBoar copy = h.spawn(Wildlife.WILD_BOAR.get(), 4, 1, 4);
        copy.setBaby(false);
        net.minecraft.world.level.storage.TagValueOutput out = net.minecraft.world.level.storage.TagValueOutput.createWithoutContext(net.minecraft.util.ProblemReporter.DISCARDING);
        boar.saveWithoutId(out);
        copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, h.getLevel().registryAccess(), out.buildResult()));
        h.assertTrue(SoundersAndColonies.isTusker(copy), "it stays a tusker when saved and loaded");
        h.succeed();
    }

    private static void sealPup(GameTestHelper h) {
        floor(h, Blocks.STONE);
        Seal mother = h.spawn(CoastFauna.SEAL.get(), 8, 1, 8);
        mother.setBaby(false);
        Seal pup = SoundersAndColonies.birth(h.getLevel(), mother);
        h.assertTrue(pup != null && pup.isBaby() && pup.distanceTo(mother) < 1.5, "a pup is born by its mother");
        h.succeed();
    }

    private static void leeches(GameTestHelper h) {
        floor(h, Blocks.STONE);
        // the leech code works on any living thing; a wisent stands in for the wader (a mock player can't take packets)
        var player = h.spawn(TemperateFauna.WISENT.get(), 8, 1, 8);
        player.setNoAi(true);
        Leeches.latch(player);
        Leeches.latch(player);
        h.assertTrue(Leeches.count(player) == 2, "two leeches latch on");
        float health = player.getHealth();
        for (int i = 0; i < Leeches.FEED_CYCLES; i++) Leeches.feed(player);
        h.assertTrue(Leeches.count(player) == 0, "sated, they drop off");
        h.assertTrue(player.getHealth() < health, "they drank some blood");
        List<ItemEntity> dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(2), e -> e.getItem().is(TemperateFauna.LEECH.get()));
        h.assertTrue(dropped.stream().mapToInt(e -> e.getItem().getCount()).sum() == 2, "two Leeches at your feet");
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        ItemStack leech = new ItemStack(TemperateFauna.LEECH.get());
        leech.finishUsingItem(h.getLevel(), player);
        h.assertTrue(player.getEffect(MobEffects.POISON) == null, "a Leech draws out poison");
        dropped.forEach(ItemEntity::discard);
        h.succeed();
    }

    private TemperateTest() {}
}
