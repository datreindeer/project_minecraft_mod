package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * <ul>
 * <li>slab_full_drop: Dirt, Grass, Dirt Path and Podzol Slabs drop one full Dirt, half or double alike (a double never 2:
 * the recipe is 1 Dirt per slab), Silk Touch the slab itself (2 for a double).</li>
 * <li>chest_mod_loot: vanilla structure chests hold mod items by depth (gen_data.py mod_loot): early gear and copper in a
 * shipwreck's supply chest, never gold or gems; tin, bronze and rough gems in a dungeon; gems, gold and jewellery in a
 * stronghold's library.</li>
 * </ul>
 */
@EventBusSubscriber(modid = MODID)
public final class LootDropsTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] TESTS = {"slab_full_drop", "chest_mod_loot"};

    static {
        TEST_FUNCTIONS.register("slab_full_drop", () -> LootDropsTest::slabs);
        TEST_FUNCTIONS.register("chest_mod_loot", () -> LootDropsTest::chests);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "loot_drops"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String name : TESTS) {
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, name));
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, name), new FunctionGameTestInstance(key, TestArea.of(env, 100, true)));
        }
    }

    private static List<ItemStack> drops(GameTestHelper h, BlockState state, ItemStack tool) {
        BlockPos rel = new BlockPos(4, 1, 4);
        h.setBlock(rel, state);
        return Block.getDrops(state, h.getLevel(), h.absolutePos(rel), null, null, tool);
    }

    private static void slabs(GameTestHelper h) {
        ItemStack silk = new ItemStack(Items.DIAMOND_SHOVEL);
        silk.enchant(h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), 1);
        for (Block slab : new Block[] {EarthBlocks.DIRT_SLAB.get(), EarthBlocks.GRASS_SLAB.get(), EarthBlocks.DIRT_PATH_SLAB.get(),
                BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(MODID, "podzol_slab"))}) {
            for (SlabType type : new SlabType[] {SlabType.BOTTOM, SlabType.DOUBLE}) {
                BlockState state = slab.defaultBlockState().setValue(SlabBlock.TYPE, type);
                List<ItemStack> plain = drops(h, state, new ItemStack(Items.STONE_SHOVEL));
                h.assertTrue(plain.size() == 1 && plain.get(0).is(Items.DIRT) && plain.get(0).getCount() == 1, state + " drops one Dirt: " + plain);
                List<ItemStack> kept = drops(h, state, silk);
                int want = type == SlabType.DOUBLE ? 2 : 1;
                h.assertTrue(kept.size() == 1 && kept.get(0).is(slab.asItem()) && kept.get(0).getCount() == want, state + " with Silk Touch: itself x" + want + ": " + kept);
            }
        }
        h.succeed();
    }

    /** Every item from {@code rolls} openings of chest table {@code name}. */
    private static Set<Item> roll(ServerLevel level, String name, int rolls) {
        var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.withDefaultNamespace(name)));
        Set<Item> got = new HashSet<>();
        for (int i = 0; i < rolls; i++) {
            LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO).create(LootContextParamSets.CHEST);
            table.getRandomItems(params).forEach(s -> got.add(s.getItem()));
        }
        return got;
    }

    private static Item mod(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MODID, id));
    }

    private static void chests(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Set<Item> supply = roll(level, "chests/shipwreck_supply", 200);
        h.assertTrue(supply.contains(Coins.COPPER_COIN.get()) && supply.contains(Mechanical.FLINT_KNIFE.get()), "a shipwreck's supplies: copper and a Flint Knife");
        h.assertFalse(supply.contains(Coins.GOLD_COIN.get()) || supply.contains(mod("ruby")) || supply.contains(Items.EMERALD), "no gold, gems or emeralds in common chests");
        Set<Item> dungeon = roll(level, "chests/simple_dungeon", 200);
        h.assertTrue(dungeon.contains(mod("tin_nugget")) && dungeon.contains(mod("bronze_nugget")) && dungeon.contains(mod("rough_garnet")), "a dungeon: tin, bronze, rough gems");
        h.assertFalse(dungeon.contains(mod("gold_ring")), "no jewellery in a dungeon");
        Set<Item> library = roll(level, "chests/stronghold_library", 300);
        h.assertTrue(library.contains(mod("gold_ring")) && library.contains(Coins.GOLD_COIN.get()) && library.contains(mod("garnet")), "a stronghold: gold and gems");
        Set<Item> village = roll(level, "chests/village/village_plains_house", 200);
        h.assertTrue(village.contains(Coins.COPPER_COIN.get()) && !village.contains(Items.EMERALD), "village chests: coins where emeralds were");
        h.succeed();
    }

    private LootDropsTest() {}
}
