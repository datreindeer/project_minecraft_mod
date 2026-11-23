package com.mechanical.wildlife.desert;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.Butchery;
import com.mechanical.HorseWork;
import com.mechanical.Husbandry;
import com.mechanical.TestArea;
import com.mechanical.wildlife.forestbirds.Vulture;
import com.mechanical.wildlife.meadow.MeadowFauna;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Game tests for the desert fauna: each species spawns only in its biomes (the biome modifiers), the butchering yields,
 * and each one's key behaviour: the scorpion's sting (Weakness) and day hiding, the fennec taken from the hand into a tame
 * fennec that marks a monster, the camel tamed into the hardy tame camel (half the stamina drain, four times the drink),
 * the sand viper burying itself in sand, the rattlesnake rattling before it strikes, the puma's ledge leap and soft
 * landing, the bighorn ram's charge, and the condor's long sight.
 */
@EventBusSubscriber(modid = MODID)
public final class DesertFaunaTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] TESTS = {"desert_spawns", "desert_yields", "desert_scorpion", "desert_fennec", "desert_camel",
            "desert_sand_viper", "desert_rattlesnake", "desert_puma", "desert_bighorn", "desert_condor"};

    static {
        TEST_FUNCTIONS.register(TESTS[0], () -> DesertFaunaTest::spawns);
        TEST_FUNCTIONS.register(TESTS[1], () -> DesertFaunaTest::yields);
        TEST_FUNCTIONS.register(TESTS[2], () -> DesertFaunaTest::scorpion);
        TEST_FUNCTIONS.register(TESTS[3], () -> DesertFaunaTest::fennec);
        TEST_FUNCTIONS.register(TESTS[4], () -> DesertFaunaTest::camel);
        TEST_FUNCTIONS.register(TESTS[5], () -> DesertFaunaTest::sandViper);
        TEST_FUNCTIONS.register(TESTS[6], () -> DesertFaunaTest::rattlesnake);
        TEST_FUNCTIONS.register(TESTS[7], () -> DesertFaunaTest::puma);
        TEST_FUNCTIONS.register(TESTS[8], () -> DesertFaunaTest::bighorn);
        TEST_FUNCTIONS.register(TESTS[9], () -> DesertFaunaTest::condor);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        for (String name : TESTS) {
            var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, name), new TestEnvironmentDefinition.AllOf(List.of()));
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name)), TestArea.of(env, 1400, true)));
        }
    }

    private static void floor(GameTestHelper h, net.minecraft.world.level.block.Block block) {
        for (int x = 0; x <= 10; x++) for (int z = 0; z <= 10; z++) h.setBlock(x, 0, z, block);
    }

    private static <T extends Mob> T spawn(GameTestHelper h, EntityType<T> type, double x, double y, double z) {
        return h.spawn(type, new net.minecraft.world.phys.Vec3(x, y, z));
    }

    /** Each species' biome modifier adds that species, in its home biome and not in the other dry country (or the plains). */
    private static void spawns(GameTestHelper h) {
        var access = h.getLevel().registryAccess();
        var modifiers = access.lookupOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        var biomes = access.lookupOrThrow(Registries.BIOME);
        Holder<Biome> desert = biomes.getOrThrow(Biomes.DESERT), badlands = biomes.getOrThrow(Biomes.BADLANDS), plains = biomes.getOrThrow(Biomes.PLAINS);
        Holder<Biome> gypsum = biomes.getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(MODID, "gypsum_flats")));
        Holder<Biome> ochre = biomes.getOrThrow(ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath(MODID, "ochre_badlands")));
        record Case(String name, EntityType<?> type, boolean desert, boolean badlands) {}
        List<Case> cases = List.of(new Case("desert_scorpion", DesertFauna.DESERT_SCORPION.get(), true, true),
                new Case("fennec_fox", DesertFauna.FENNEC_FOX.get(), true, false), new Case("wild_camel", DesertFauna.WILD_CAMEL.get(), true, false),
                new Case("sand_viper", DesertFauna.SAND_VIPER.get(), true, false), new Case("puma", DesertFauna.PUMA.get(), false, true),
                new Case("bighorn_sheep", DesertFauna.BIGHORN_SHEEP.get(), false, true), new Case("rattlesnake", DesertFauna.RATTLESNAKE.get(), false, true),
                new Case("condor", DesertFauna.CONDOR.get(), false, true));
        for (Case c : cases) {
            BiomeModifier m = modifiers.getOrThrow(ResourceKey.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                    Identifier.fromNamespaceAndPath(MODID, "add_" + c.name() + "_desert"))).value();
            h.assertTrue(m instanceof BiomeModifiers.AddSpawnsBiomeModifier, c.name() + ": an add_spawns modifier");
            var add = (BiomeModifiers.AddSpawnsBiomeModifier) m;
            h.assertTrue(add.spawners().unwrap().stream().allMatch(w -> w.value().type() == c.type()), c.name() + ": spawns its own type");
            h.assertTrue(add.biomes().contains(desert) == c.desert() && add.biomes().contains(gypsum) == c.desert(),
                    c.name() + (c.desert() ? ": spawns in the desert and the Gypsum Flats" : ": not in the desert"));
            h.assertTrue(add.biomes().contains(badlands) == c.badlands() && add.biomes().contains(ochre) == c.badlands(),
                    c.name() + (c.badlands() ? ": spawns in the badlands and the Ochre Badlands" : ": not in the badlands"));
            h.assertFalse(add.biomes().contains(plains), c.name() + ": not on the plains");
        }
        h.succeed();
    }

    private static boolean has(List<Butchery.Extra> extras, Item item) {
        return extras.stream().anyMatch(e -> e.item().get() == item);
    }

    private static void yields(GameTestHelper h) {
        var s = Butchery.yieldFor(DesertFauna.DESERT_SCORPION.get());
        h.assertTrue(s.meat().get() == DesertFauna.RAW_SCORPION.get() && has(s.inner(), DesertFauna.SCORPION_VENOM.get()), "scorpion: meat and Scorpion Venom");
        var f = Butchery.yieldFor(DesertFauna.FENNEC_FOX.get());
        h.assertTrue(f.meat().get() == DesertFauna.RAW_FENNEC.get() && has(f.outer(), DesertFauna.RAW_FENNEC_PELT.get()), "fennec: meat and pelt");
        var c = Butchery.yieldFor(DesertFauna.WILD_CAMEL.get());
        h.assertTrue(c.meat().get() == DesertFauna.CAMEL_MEAT.get() && has(c.outer(), DesertFauna.RAW_CAMEL_HIDE.get())
                && has(c.inner(), DesertFauna.CAMEL_HUMP_FAT.get()), "camel: meat, hide, hump fat");
        var v = Butchery.yieldFor(DesertFauna.SAND_VIPER.get());
        h.assertTrue(v.meat().get() == MeadowFauna.RAW_SNAKE.get() && has(v.outer(), MeadowFauna.SNAKESKIN.get())
                && has(v.inner(), MeadowFauna.VENOM_GLAND.get()) && has(v.inner(), DesertFauna.VIPER_FANG.get()), "sand viper: snake, skin, venom gland, fangs");
        var r = Butchery.yieldFor(DesertFauna.RATTLESNAKE.get());
        h.assertTrue(r.meat().get() == MeadowFauna.RAW_SNAKE.get() && has(r.outer(), DesertFauna.RATTLESNAKE_RATTLE.get())
                && has(r.inner(), MeadowFauna.VENOM_GLAND.get()), "rattlesnake: snake, rattle, venom gland");
        var p = Butchery.yieldFor(DesertFauna.PUMA.get());
        h.assertTrue(p.meat().get() == DesertFauna.PUMA_MEAT.get() && has(p.outer(), DesertFauna.RAW_PUMA_PELT.get()) && has(p.inner(), Items.BONE),
                "puma: meat, pelt, bones");
        var b = Butchery.yieldFor(DesertFauna.BIGHORN_SHEEP.get());
        h.assertTrue(b.meat().get() == DesertFauna.BIGHORN_MUTTON.get() && has(b.outer(), DesertFauna.RAW_BIGHORN_HIDE.get())
                && has(b.inner(), DesertFauna.BIGHORN_HORN.get()), "bighorn: mutton, hide, horn");
        h.succeed();
    }

    private static void scorpion(GameTestHelper h) {
        floor(h, Blocks.SAND);
        DesertScorpion s = spawn(h, DesertFauna.DESERT_SCORPION.get(), 3.5, 1, 3.5);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        s.sting(p);
        h.assertTrue(p.hasEffect(MobEffects.WEAKNESS) && p.hasEffect(MobEffects.POISON), "the sting gives Weakness and Poison");
        h.assertTrue(s.sleepTime() == h.getLevel().isBrightOutside(), "it hides (sleeps) by day and is out at night");
        h.assertTrue(s.fearMultiplier() == 0.0, "it doesn't run from players");
        h.succeed();
    }

    private static void fennec(GameTestHelper h) {
        floor(h, Blocks.SAND);
        FennecFox fox = spawn(h, DesertFauna.FENNEC_FOX.get(), 3.5, 1, 3.5);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setPose(Pose.CROUCHING);
        ItemStack meat = new ItemStack(Items.MUTTON, 4);
        fox.feed(p, meat);
        h.assertTrue(fox.trust() == 1 && meat.getCount() == 3, "a crouching player feeds it meat from the hand (trust 1)");
        p.setPose(Pose.STANDING);
        fox.feed(p, meat);
        h.assertTrue(fox.trust() == 1 && meat.getCount() == 3, "standing up close it won't eat");
        TameFennec tame = fox.tame(p);
        h.assertTrue(tame != null && tame.isTame() && tame.isOwnedBy(p), "tamed it becomes a tame fennec owned by the feeder");
        Mob zombie = spawn(h, EntityTypes.ZOMBIE, 8.5, 1, 8.5);
        Mob found = tame.listen(h.getLevel());
        h.assertTrue(found == zombie && zombie.hasEffect(MobEffects.GLOWING), "it hears the zombie and marks it (Glowing)");
        h.assertTrue(tame.listen(h.getLevel()) == null, "it doesn't warn twice of the same one");
        zombie.discard();
        h.succeed();
    }

    private static void camel(GameTestHelper h) {
        floor(h, Blocks.SAND);
        WildCamel wild = spawn(h, DesertFauna.WILD_CAMEL.get(), 3.5, 1, 3.5);
        h.assertTrue(wild.tameType() == DesertFauna.TAME_CAMEL.get(), "a wild camel tames into a tame camel");
        TameCamel camel = spawn(h, DesertFauna.TAME_CAMEL.get(), 6.5, 1, 6.5);
        Horse horse = spawn(h, EntityTypes.HORSE, 8.5, 1, 3.5);
        camel.setTamed(true);
        horse.setTamed(true);
        h.assertTrue(HorseWork.hardy(camel) && !HorseWork.hardy(horse), "the camel is hardy, the horse isn't");
        h.assertTrue(Math.abs(HorseWork.drain(camel, 0.5) - HorseWork.drain(horse, 0.5) * 0.5F) < 1e-6, "a camel's stamina drains at half the horse's rate");
        long now = h.getLevel().getGameTime();
        Husbandry.feed(camel, false);
        Husbandry.feed(horse, false);
        h.assertTrue(camel.getData(Husbandry.NEEDS).watered() - now >= Husbandry.WATER_TICKS * Husbandry.CAMEL_WATER, "a camel's drink lasts four times as long");
        h.assertTrue(horse.getData(Husbandry.NEEDS).watered() - now < Husbandry.WATER_TICKS + 5, "a horse's drink lasts the usual time");
        h.assertFalse(camel.canMate(horse), "camels don't breed with horses");
        h.succeed();
    }

    private static void sandViper(GameTestHelper h) {
        floor(h, Blocks.STONE);
        for (int x = 0; x <= 10; x++) for (int z = 0; z <= 10; z++) h.setBlock(x, 1, z, Blocks.SAND); // on stone, so it doesn't fall
        SandViper viper = spawn(h, DesertFauna.SAND_VIPER.get(), 5.5, 2, 5.5);
        h.succeedWhen(() -> h.assertTrue(viper.isBuried(), "on sand the viper buries itself"));
    }

    private static void rattlesnake(GameTestHelper h) {
        floor(h, Blocks.RED_SAND);
        Rattlesnake snake = spawn(h, DesertFauna.RATTLESNAKE.get(), 3.5, 1, 3.5);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setPos(snake.getX() + 1.5, snake.getY(), snake.getZ());
        h.runAfterDelay(5, () -> {
            for (int i = 0; i < Rattlesnake.WARN_TICKS - 1; i++) snake.rattleAt(p);
            h.assertTrue(snake.isRattling() && snake.getTarget() == null, "it rattles first and doesn't strike yet");
            snake.rattleAt(p);
            h.assertTrue(snake.getTarget() == p, "after the warning, someone still close is struck");
            h.succeed();
        });
    }

    private static void puma(GameTestHelper h) {
        floor(h, Blocks.RED_SANDSTONE);
        for (int y = 1; y <= 4; y++) h.setBlock(2, y, 2, Blocks.RED_SANDSTONE);
        Puma puma = spawn(h, DesertFauna.PUMA.get(), 2.5, 5, 2.5);
        BighornSheep prey = spawn(h, DesertFauna.BIGHORN_SHEEP.get(), 6.5, 1, 2.5);
        h.runAfterDelay(3, () -> {
            h.assertTrue(puma.isLedge(puma.blockPosition()), "the pillar top is a ledge (a drop beside it)");
            h.assertTrue(puma.canLedgeLeap(prey), "prey below and in reach: it can leap down on it");
            puma.ledgeLeap(prey);
            h.assertTrue(puma.isLeaping() && puma.getDeltaMovement().x > 0, "it leaps out and down towards the prey");
            float before = puma.getHealth();
            puma.causeFallDamage(10.0, 1.0F, puma.damageSources().fall());
            h.assertTrue(puma.getHealth() == before, "a 10-block drop doesn't hurt it");
            prey.discard();
            h.succeed();
        });
    }

    private static void bighorn(GameTestHelper h) {
        floor(h, Blocks.STONE);
        BighornSheep ram = spawn(h, DesertFauna.BIGHORN_SHEEP.get(), 3.5, 1, 3.5);
        ram.setMale(true);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        p.setPos(ram.getX() + 4, ram.getY(), ram.getZ());
        h.runAfterDelay(5, () -> ram.hurtServer(h.getLevel(), ram.damageSources().playerAttack(p), 1.0F));
        h.succeedWhen(() -> h.assertTrue(ram.isChargingPlayer() && ram.isCharging(), "the hurt ram lowers its horns and charges"));
    }

    private static void condor(GameTestHelper h) {
        floor(h, Blocks.RED_SANDSTONE);
        Condor condor = spawn(h, DesertFauna.CONDOR.get(), 3.5, 1, 3.5);
        h.assertTrue(condor instanceof Vulture, "a carrion bird: circles carcasses like the vulture");
        h.assertTrue(condor.sight() > 80 && Condor.KIND.maxAlt() > 34, "it sees farther and soars higher than the vulture");
        h.succeed();
    }

    private DesertFaunaTest() {}
}
