package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.mechanical.worldgen.ModBiomes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Game test for phase 9C: cave air (Ice Caves, Magma Vents), overheating, cold storage and peat smoke. */
@EventBusSubscriber(modid = MODID)
public final class EnvironmentTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "environment"));

    static {
        TEST_FUNCTIONS.register("environment", () -> EnvironmentTest::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "environment"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "environment"),
                new FunctionGameTestInstance(TEST, TestArea.of(env, 20, true)));
    }

    private static void check(GameTestHelper h, boolean ok, String what) {
        if (!ok) throw h.assertionException("environment: " + what);
    }

    private static void test(GameTestHelper h) {
        var biomes = h.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        // cave air: the Ice Caves freeze at any depth, the Magma Vents are far above body heat, other caves go by depth
        float ice = Temperature.cave(biomes.getOrThrow(ModBiomes.ICE_CAVES), 40), magma = Temperature.cave(biomes.getOrThrow(ModBiomes.MAGMA_VENTS), -40);
        check(h, ice < Temperature.FREEZING_BELOW && ice == Temperature.cave(biomes.getOrThrow(ModBiomes.ICE_CAVES), 0), "Ice Caves near freezing, got " + ice);
        check(h, magma > Temperature.MAX, "Magma Vents hotter than MAX, got " + magma);
        check(h, Temperature.cave(biomes.getOrThrow(ModBiomes.LIMESTONE_CAVERNS), 0) == Temperature.cave(0), "other caves by depth");

        // overheating: only the vents scorch; desert sun, Nether, fires and being wet stop at OVERHEAT; the cold is untouched
        check(h, Temperature.COMFORT < Temperature.OVERHEAT && Temperature.OVERHEAT < Temperature.SCORCHING && Temperature.SCORCHING < Temperature.MAX, "heat thresholds in order");
        check(h, Temperature.heatCap(Temperature.MAX, magma, false) > Temperature.SCORCHING, "the vents scorch");
        check(h, Temperature.heatCap(Temperature.MAX, magma, true) <= Temperature.OVERHEAT, "water cools you in the vents");
        check(h, Temperature.heatCap(Temperature.MAX, 22, false) <= Temperature.OVERHEAT && Temperature.heatCap(Temperature.MAX, Temperature.MAX, false) <= Temperature.OVERHEAT,
                "desert and Nether never scorch");
        check(h, Temperature.heatCap(4, 4, false) == 4 && Temperature.heatCap(4, 4, true) == 4, "cold untouched");

        // cold storage: warm air full speed, cool half, near freezing a quarter; ice beside the store a quarter anywhere
        check(h, Preserving.coldRate(Temperature.COMFORT) == 1 && Preserving.coldRate(6) == Preserving.COOL_RATE && Preserving.coldRate(ice) == Preserving.COLD_RATE,
                "cold rates");
        BlockPos chest = h.absolutePos(new BlockPos(1, 1, 1));
        h.setBlock(1, 1, 1, Blocks.CHEST);
        h.setBlock(2, 1, 1, Blocks.AIR);
        float plain = Preserving.coldRate(h.getLevel(), chest);
        h.setBlock(2, 1, 1, Blocks.PACKED_ICE);
        check(h, Preserving.coldRate(h.getLevel(), chest) == Preserving.COLD_RATE, "packed ice beside a chest is a cold cellar (without: " + plain + ")");
        h.setBlock(2, 1, 1, Blocks.BLUE_ICE);
        check(h, Preserving.coldRate(h.getLevel(), chest) == Preserving.COLD_RATE, "blue ice too");
        // a day shut in at a quarter rate counts as a quarter day: the spoil time moves 3/4 day later
        ItemStack beef = new ItemStack(Items.BEEF);
        beef.set(Preserving.SPOILS_AT.get(), 1000L);
        beef.set(Preserving.BASKET_SINCE.get(), 0L);
        ItemStack out = Preserving.outOfBasket(beef, Preserving.DAY, Preserving.COLD_RATE);
        check(h, out.get(Preserving.SPOILS_AT.get()) == 1000 + Preserving.DAY * 3 / 4 && !out.has(Preserving.BASKET_SINCE.get()), "cold settles the spoil time");
        check(h, Preserving.outOfBasket(beef, Preserving.DAY, 1).get(Preserving.SPOILS_AT.get()) == 1000, "warm: no credit");
        h.setBlock(1, 1, 1, Blocks.AIR);
        h.setBlock(2, 1, 1, Blocks.AIR);

        // peat smoke: a lit campfire fed peat under a Smoking Rack smokes it twice as fast; plain wood does not
        h.setBlock(3, 1, 3, Blocks.CAMPFIRE);
        h.setBlock(3, 2, 3, Preserving.SMOKING_RACK.get());
        BlockPos rack = h.absolutePos(new BlockPos(3, 2, 3));
        check(h, SmokingRackBlock.inSmoke(h.getLevel(), rack) && !SmokingRackBlock.peatSmoke(h.getLevel(), rack), "wood fire: smoke, not peat smoke");
        ((CampfireAccess) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(3, 1, 3)))).mechanical$addPeat(EarthBlocks.PEAT_CAMPFIRE_TICKS);
        check(h, SmokingRackBlock.peatSmoke(h.getLevel(), rack) && SmokingRackBlockEntity.PEAT_SPEED > 1, "peat fire: peat smoke");
        h.setBlock(3, 1, 3, Blocks.AIR);
        h.setBlock(3, 2, 3, Blocks.AIR);
        h.succeed();
    }

    private EnvironmentTest() {}
}
