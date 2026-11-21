package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * End of the Bronze Age, toward iron: Fire Clay (Clay Balls + Grog from crushed Brick) fired hot into Fire Bricks, the
 * Bloomery (a 1x1x3 shaft: {@link BloomeryHearthBlock} + two {@link BloomeryStackBlock}s, controller
 * {@link BloomeryBlockEntity}) that reduces Raw Iron to a glowing Hot Iron Bloom at 1350° (only with bellows), and the
 * Bronze Smithing Hammer and Bronze Anvil ({@link BronzeAnvilBlock}) the hot bloom is hammered on into Wrought Iron Ingots
 * and Slag. Raw Iron smelts nowhere else (vanilla's iron smelting recipes are disabled in scripts/gen_bloomery.py).
 * Data: scripts/gen_bloomery.py, textures: scripts/gen_bloomery_textures.py.
 */
@EventBusSubscriber(modid = MODID)
public final class Ironworking {
    /** How many times a hot bloom has been struck (it keeps them through cooling and reheating). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BLOOM_HITS = Casting.COMPONENTS.registerComponentType(
            "bloom_hits", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Seconds a bloom stays hot enough to work out of the fire. */
    public static final int HOT_SECONDS = 30;
    /** Blows that work a bloom into iron; each costs the hammer {@link #HAMMER_WEAR}. */
    public static final int HITS = 8, HAMMER_WEAR = 2, HAMMER_COOLDOWN = 10;
    /**
     * A worked bloom is {@link #BLOOM_PARTS} parts, iron and slag: its {@link #BLOOM_YIELD} are Wrought Iron Ingots and the
     * rest Slag. Unroasted ore gives {@link #RAW_YIELD}, roasted {@link #ROASTED_YIELD}, a charge of flux in the hearth one more.
     */
    public static final int BLOOM_PARTS = 3, RAW_YIELD = 1, ROASTED_YIELD = 2;
    /** Wrought Iron Ingots a bloom will give (blooms from before roasting existed count as roasted). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BLOOM_YIELD = Casting.COMPONENTS.registerComponentType(
            "bloom_yield", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** Flux charges the Bloomery hearth holds at most. */
    public static final int MAX_FLUX = 4;
    /** Limestone flux (Iron Age ores): laid in the hearth, the next bloom gives one more ingot and one less slag. */
    public static final net.minecraft.tags.TagKey<Item> FLUX = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
            Identifier.fromNamespaceAndPath(MODID, "flux"));

    // --- Iron Age: roasting drives water and sulphur out of the ore (campfire or Fire Pit); roasted ore blooms richer ---
    public static final DeferredItem<Item> ROASTED_IRON_ORE = ITEMS.registerSimpleItem("roasted_iron_ore");
    public static final DeferredItem<Item> ROASTED_BOG_IRON = ITEMS.registerSimpleItem("roasted_bog_iron");

    // --- bog iron: rusty lumps in the floors of fen and swamp pools, dug with any shovel (scripts/gen_bog_iron.py) ---
    public static final DeferredBlock<net.minecraft.world.level.block.Block> BOG_IRON_ORE = BLOCKS.registerSimpleBlock("bog_iron_ore", p -> p
            .mapColor(MapColor.TERRACOTTA_BROWN).strength(0.8F).sound(SoundType.MUD).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> BOG_IRON_ORE_ITEM = ITEMS.registerSimpleBlockItem("bog_iron_ore", BOG_IRON_ORE);
    public static final DeferredItem<Item> RAW_BOG_IRON = ITEMS.registerSimpleItem("raw_bog_iron");

    // --- refractory ---
    public static final DeferredItem<Item> GROG = ITEMS.registerSimpleItem("grog");
    public static final DeferredItem<Item> FIRE_CLAY = ITEMS.registerSimpleItem("fire_clay");
    public static final DeferredItem<Item> FIRE_BRICK = ITEMS.registerSimpleItem("fire_brick");

    // --- the bloomery ---
    public static final DeferredBlock<BloomeryHearthBlock> BLOOMERY_HEARTH = BLOCKS.registerBlock("bloomery_hearth", BloomeryHearthBlock::new, p -> p
            .mapColor(MapColor.TERRACOTTA_BROWN).strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()
            .lightLevel(s -> s.getValue(BloomeryHearthBlock.LIT) ? 10 : 0));
    public static final DeferredItem<BlockItem> BLOOMERY_HEARTH_ITEM = ITEMS.registerSimpleBlockItem("bloomery_hearth", BLOOMERY_HEARTH);
    public static final DeferredBlock<BloomeryStackBlock> BLOOMERY_STACK = BLOCKS.registerBlock("bloomery_stack", BloomeryStackBlock::new, p -> p
            .mapColor(MapColor.TERRACOTTA_BROWN).strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()
            .lightLevel(s -> s.getValue(BloomeryStackBlock.LIT) && s.getValue(BloomeryStackBlock.TOP) ? 9 : 0));
    public static final DeferredItem<BlockItem> BLOOMERY_STACK_ITEM = ITEMS.registerSimpleBlockItem("bloomery_stack", BLOOMERY_STACK);
    public static final Supplier<BlockEntityType<BloomeryBlockEntity>> BLOOMERY_ENTITY = Mechanical.BLOCK_ENTITIES.register("bloomery",
            () -> new BlockEntityType<>(BloomeryBlockEntity::new, BLOOMERY_HEARTH.get()));
    public static final Supplier<MenuType<KilnMenu>> BLOOMERY_MENU = Mechanical.MENUS.register("bloomery",
            () -> new MenuType<>(KilnMenu::bloomery, FeatureFlags.VANILLA_SET));

    public static final DeferredItem<IronBloomItem> HOT_IRON_BLOOM = ITEMS.registerItem("hot_iron_bloom", p -> new IronBloomItem(p, true),
            p -> p.durability(HOT_SECONDS));
    public static final DeferredItem<IronBloomItem> IRON_BLOOM = ITEMS.registerItem("iron_bloom", p -> new IronBloomItem(p, false), p -> p.stacksTo(16));
    public static final DeferredItem<Item> WROUGHT_IRON_INGOT = ITEMS.registerSimpleItem("wrought_iron_ingot");
    public static final DeferredItem<Item> SLAG = ITEMS.registerSimpleItem("slag");

    // --- smithing hammer and anvil, cast in bronze ---
    public static final DeferredItem<Item> UNFIRED_CLAY_HAMMER_HEAD_MOLD = ITEMS.registerSimpleItem("unfired_clay_hammer_head_mold");
    public static final DeferredItem<Item> UNFIRED_CLAY_ANVIL_MOLD = ITEMS.registerSimpleItem("unfired_clay_anvil_mold");
    public static final DeferredBlock<ClayMoldBlock> CLAY_HAMMER_HEAD_MOLD = Casting.mold("clay_hammer_head_mold", ClayMoldBlock.Shape.HAMMER_HEAD);
    public static final DeferredBlock<ClayMoldBlock> CLAY_ANVIL_MOLD = Casting.mold("clay_anvil_mold", ClayMoldBlock.Shape.ANVIL);
    public static final DeferredItem<BlockItem> CLAY_HAMMER_HEAD_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_hammer_head_mold", CLAY_HAMMER_HEAD_MOLD);
    public static final DeferredItem<BlockItem> CLAY_ANVIL_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_anvil_mold", CLAY_ANVIL_MOLD);
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_HAMMER_HEAD = ITEMS.registerSimpleItem("rough_cast_bronze_hammer_head");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_ANVIL = ITEMS.registerSimpleItem("rough_cast_bronze_anvil");
    public static final DeferredItem<Item> BRONZE_HAMMER_HEAD = ITEMS.registerSimpleItem("bronze_hammer_head");
    /** Works hot blooms on the Bronze Anvil ({@link #HAMMER_WEAR} a blow: iron wears bronze fast) and is a hammerstone on the Stone Anvil. */
    public static final DeferredItem<Item> BRONZE_SMITHING_HAMMER = ITEMS.registerSimpleItem("bronze_smithing_hammer", p -> p.durability(160));

    public static final DeferredBlock<BronzeAnvilBlock> BRONZE_ANVIL = BLOCKS.registerBlock("bronze_anvil", BronzeAnvilBlock::new, p -> p
            .mapColor(MapColor.COLOR_ORANGE).strength(5.0F, 1200.0F).sound(SoundType.ANVIL).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredItem<BlockItem> BRONZE_ANVIL_ITEM = ITEMS.registerSimpleBlockItem("bronze_anvil", BRONZE_ANVIL);
    public static final Supplier<BlockEntityType<BronzeAnvilBlockEntity>> BRONZE_ANVIL_ENTITY = Mechanical.BLOCK_ENTITIES.register("bronze_anvil",
            () -> new BlockEntityType<>(BronzeAnvilBlockEntity::new, BRONZE_ANVIL.get()));

    /** Granted from code (criterion minecraft:impossible, scripts/gen_advancements.py). */
    public static final Identifier BLOOMERY_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "bronze_age/bloomery");

    /** Rough cast -> what the Whetstone grinds it into (the anvil keeps no Poor Bronze mark: a block carries none). */
    private static final Map<Supplier<Item>, Supplier<? extends Item>> GROUND = Map.of(
            ROUGH_CAST_BRONZE_HAMMER_HEAD, BRONZE_HAMMER_HEAD, ROUGH_CAST_BRONZE_ANVIL, BRONZE_ANVIL_ITEM);

    static ItemStack ground(ItemStack rough) {
        for (var e : GROUND.entrySet()) {
            if (!rough.is(e.getKey().get())) continue;
            ItemStack out = new ItemStack(e.getValue().get());
            if (Bronze.isPoor(rough) && !out.is(BRONZE_ANVIL_ITEM.get())) Bronze.markPoor(out);
            return out;
        }
        return ItemStack.EMPTY;
    }

    /** A bloom at full heat (keeping its blows), cooling from {@code now}. */
    public static ItemStack hotBloom(ItemStack from, long now) {
        ItemStack out = new ItemStack(HOT_IRON_BLOOM.get());
        out.set(BLOOM_HITS.get(), hits(from));
        if (from.has(BLOOM_YIELD.get())) out.set(BLOOM_YIELD.get(), from.get(BLOOM_YIELD.get()));
        out.set(Casting.COOLS_AT.get(), now + HOT_SECONDS * 20L);
        return out;
    }

    public static int bloomYield(ItemStack bloom) {
        return bloom.getOrDefault(BLOOM_YIELD.get(), ROASTED_YIELD);
    }

    public static int hits(ItemStack bloom) {
        return bloom.getOrDefault(BLOOM_HITS.get(), 0);
    }

    /** Is it a hot bloom, or hot iron or steel ({@link HotMetalItem}), that hasn't cooled by {@code now}? */
    public static boolean isHot(ItemStack stack, long now) {
        return hotItem(stack) && !CrucibleItem.isCold(stack, now);
    }

    /** A hot bloom or hot iron/steel item, whether or not its time is up. */
    public static boolean hotItem(ItemStack stack) {
        return stack.is(HOT_IRON_BLOOM.get()) || stack.getItem() instanceof HotMetalItem;
    }

    /** {@code from} (a cold bloom or ingot, or the ore) heated into {@code hot}: full heat from {@code now}, its blows and yield kept. */
    public static ItemStack heated(ItemStack from, Item hot, long now) {
        if (hot == HOT_IRON_BLOOM.get()) return hotBloom(from, now);
        if (!(hot instanceof HotMetalItem)) return new ItemStack(hot); // not hot metal (a cemented pot): just the result
        ItemStack out = new ItemStack(hot);
        if (hits(from) > 0) out.set(BLOOM_HITS.get(), hits(from));
        out.set(Casting.COOLS_AT.get(), now + HOT_SECONDS * 20L);
        return out;
    }

    // ---------------------------------------------------------------- game test

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "bloomery"));

    static {
        TEST_FUNCTIONS.register("bloomery", () -> Ironworking::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "bloomery"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "bloomery"),
                new FunctionGameTestInstance(TEST, TestArea.of(env, 40, true)));
    }

    private static void test(GameTestHelper h) {
        try {
            run(h);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    private static void run(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        // the 1x1x3 shaft: hearth + two stacks with an open top is a bloomery; one stack, three stacks or a lid are not
        BlockPos hearth = new BlockPos(1, 1, 1);
        h.setBlock(hearth, BLOOMERY_HEARTH.get());
        BloomeryBlockEntity bloomery = (BloomeryBlockEntity) level.getBlockEntity(h.absolutePos(hearth));
        h.assertTrue(bloomery != null, "the hearth has its block entity");
        h.setBlock(hearth.above(), BLOOMERY_STACK.get());
        h.assertTrue(bloomery.check(level) != null, "a hearth with one stack is not a bloomery");
        h.setBlock(hearth.above(2), BLOOMERY_STACK.get());
        h.assertTrue(bloomery.check(level) == null, "hearth + 2 stacks is a bloomery (" + bloomery.check(level) + ")");
        h.assertTrue(level.getBlockState(h.absolutePos(hearth.above(2))).getValue(BloomeryStackBlock.TOP), "the upper stack is the open top");
        h.assertFalse(level.getBlockState(h.absolutePos(hearth.above())).getValue(BloomeryStackBlock.TOP), "the lower stack is not");
        h.setBlock(hearth.above(3), Blocks.STONE);
        h.assertTrue(bloomery.check(level) != null, "a lid on the shaft: not a bloomery");
        h.setBlock(hearth.above(3), BLOOMERY_STACK.get());
        h.assertTrue(bloomery.check(level) != null, "three stacks: too tall");
        h.setBlock(hearth.above(3), Blocks.AIR);

        // it smelts Raw Iron only at 1350° (above the kiln's 1300° ceiling), so only with bellows
        h.assertTrue(BloomeryBlockEntity.canSmelt(new ItemStack(Items.RAW_IRON)), "the bloomery takes Raw Iron");
        h.assertTrue(BloomeryBlockEntity.SMELT_TEMP > KilnBlockEntity.CEILING && BloomeryBlockEntity.SMELT_TEMP > BloomeryBlockEntity.NATURAL_MAX
                && BloomeryBlockEntity.SMELT_TEMP <= BloomeryBlockEntity.CEILING, "1350° needs the bloomery and bellows");
        bloomery.setItem(BloomeryBlockEntity.INPUT, new ItemStack(Items.RAW_IRON, 4));
        bloomery.setItem(BloomeryBlockEntity.FUEL, new ItemStack(Items.CHARCOAL, 4));
        h.assertTrue(bloomery.tryLight(level), "a whole bloomery with Charcoal lights");
        bloomery.forceTemperature(1300);
        for (int i = 0; i < 40; i++) BloomeryBlockEntity.serverTick(level, bloomery);
        h.assertTrue(bloomery.progress() == 0, "at 1300° nothing smelts");
        bloomery.forceTemperature(1500);
        for (int i = 0; i < BloomeryBlockEntity.SMELT_TICKS; i++) BloomeryBlockEntity.serverTick(level, bloomery);
        h.assertTrue(bloomery.getItem(BloomeryBlockEntity.OUTPUT).is(HOT_IRON_BLOOM.get()), "4 Raw Iron at 1500° make a Hot Iron Bloom (got "
                + bloomery.getItem(BloomeryBlockEntity.OUTPUT) + ")");

        // bog iron: any shovel digs it (not a pickaxe or a bare hand), and 6 Raw Bog Iron make a bloom
        var bog = BOG_IRON_ORE.get().defaultBlockState();
        h.assertTrue(new ItemStack(Mechanical.FLINT_SHOVEL.get()).isCorrectToolForDrops(bog), "a Flint Shovel digs Bog Iron Ore");
        h.assertFalse(new ItemStack(Bronze.BRONZE_PICKAXE.get()).isCorrectToolForDrops(bog), "a pickaxe gets nothing from Bog Iron Ore");
        h.assertFalse(ItemStack.EMPTY.isCorrectToolForDrops(bog), "nor a bare hand");
        h.assertTrue(BloomeryBlockEntity.canSmelt(new ItemStack(RAW_BOG_IRON.get())), "the bloomery takes Raw Bog Iron");
        h.assertTrue(BloomeryBlockEntity.recipeFor(new ItemStack(RAW_BOG_IRON.get())).orElseThrow().count() == BloomeryBlockEntity.BOG_ORE_PER_BLOOM,
                "6 Raw Bog Iron per bloom");
        h.assertTrue(KilnBlockEntity.bakeFor(new ItemStack(RAW_BOG_IRON.get())) == null && Casting.oreUnits(new ItemStack(RAW_BOG_IRON.get())) == null,
                "Raw Bog Iron smelts nowhere else");

        // Raw Iron smelts nowhere else: not in the Clay Kiln, the crucible, the Smelting Furnace, on the Stone Anvil or in vanilla furnaces
        ItemStack raw = new ItemStack(Items.RAW_IRON);
        h.assertFalse(KilnBlockEntity.canBake(raw), "the Clay Kiln won't take Raw Iron");
        h.assertTrue(Casting.oreUnits(raw) == null, "Raw Iron doesn't go in a crucible (nor the Smelting Furnace)");
        h.assertTrue(BronzeOres.crushed(raw).isEmpty(), "Raw Iron isn't crushed on the Stone Anvil");
        // (a campfire only roasts it, into Roasted Iron Ore: IronAgeTest)
        for (RecipeType<?> type : List.of(RecipeType.SMELTING, RecipeType.BLASTING, RecipeType.SMOKING)) {
            @SuppressWarnings("unchecked")
            var cooking = (RecipeType<net.minecraft.world.item.crafting.AbstractCookingRecipe>) type;
            h.assertTrue(level.recipeAccess().getRecipeFor(cooking, new SingleRecipeInput(raw), level).isEmpty(), "no " + type + " recipe for Raw Iron");
        }

        // iron ore needs a bronze pickaxe
        var ore = Blocks.IRON_ORE.defaultBlockState();
        h.assertFalse(new ItemStack(Items.COPPER_PICKAXE).isCorrectToolForDrops(ore), "a Copper Pickaxe gets nothing from Iron Ore");
        h.assertFalse(new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(ore), "nor a Stone Pickaxe");
        h.assertTrue(new ItemStack(Bronze.BRONZE_PICKAXE.get()).isCorrectToolForDrops(ore), "a Bronze Pickaxe mines Iron Ore");

        // a hot bloom hammered on the Bronze Anvil gives wrought iron and slag; a cold one doesn't give
        BlockPos anvilAt = new BlockPos(4, 1, 1);
        h.setBlock(anvilAt, BRONZE_ANVIL.get());
        BronzeAnvilBlockEntity anvil = (BronzeAnvilBlockEntity) level.getBlockEntity(h.absolutePos(anvilAt));
        Player smith = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack hammer = new ItemStack(BRONZE_SMITHING_HAMMER.get());
        smith.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        smith.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(BronzeTools.BRONZE_TONGS.get()));
        anvil.place(level, IRON_BLOOM.get().getDefaultInstance());
        for (int i = 0; i < HITS; i++) anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertItemEntityNotPresent(WROUGHT_IRON_INGOT.get(), anvilAt, 2);
        h.assertTrue(hits(anvil.item()) == 0 && hammer.getDamageValue() == 0, "a cold bloom takes no blows and doesn't wear the hammer");
        anvil.take();
        anvil.place(level, bloomery.removeItemNoUpdate(BloomeryBlockEntity.OUTPUT));
        for (int i = 0; i < HITS; i++) anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertItemEntityPresent(WROUGHT_IRON_INGOT.get(), anvilAt, 2);
        h.assertItemEntityPresent(SLAG.get(), anvilAt, 2);
        h.assertTrue(anvil.item().isEmpty() && hammer.getDamageValue() == HITS * HAMMER_WEAR, "the bloom is used up and the hammer worn " + HITS * HAMMER_WEAR);

        // a Copper Ingot is struck cold into Copper Coins
        anvil.place(level, new ItemStack(Items.COPPER_INGOT));
        for (int i = 0; i < Coins.MINT_HITS - 1; i++) anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertItemEntityNotPresent(Coins.COPPER_COIN.get(), anvilAt, 2);
        anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertItemEntityPresent(Coins.COPPER_COIN.get(), anvilAt, 2);
        h.assertTrue(anvil.item().isEmpty(), "the ingot is used up");

        // three Bronze Ingots struck cold make a Bronze Fire Grate; two take no blows
        anvil.place(level, Bronze.BRONZE_INGOT.get().getDefaultInstance());
        anvil.add(level, Bronze.BRONZE_INGOT.get().getDefaultInstance());
        int worn = hammer.getDamageValue();
        anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertTrue(anvil.item().getCount() == 2 && hammer.getDamageValue() == worn, "two ingots: no grate and no wear");
        anvil.add(level, Bronze.BRONZE_INGOT.get().getDefaultInstance());
        for (int i = 0; i < FirePit.GRATE_HITS - 1; i++) anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertItemEntityNotPresent(FirePit.FIRE_GRATE.get(), anvilAt, 2);
        anvil.strike(level, smith, hammer, InteractionHand.MAIN_HAND);
        h.assertItemEntityPresent(FirePit.FIRE_GRATE.get(), anvilAt, 2);
        h.assertTrue(anvil.item().isEmpty() && hammer.getDamageValue() == worn + FirePit.GRATE_HITS, "the ingots are used up, 1 wear per blow");

        // a cooled bloom keeps its blows and is reheated (1100°) in the Clay Kiln
        ItemStack half = IRON_BLOOM.get().getDefaultInstance();
        half.set(BLOOM_HITS.get(), 3);
        KilnBlockEntity.Bake reheat = KilnBlockEntity.bakeFor(half);
        h.assertTrue(reheat != null && reheat.temp == BloomeryBlockEntity.FORGE_TEMP && reheat.result.is(HOT_IRON_BLOOM.get()) && hits(reheat.result) == 3,
                "the kiln reheats a bloom at 1100° and it keeps its blows");
        h.assertTrue(BloomeryBlockEntity.canSmelt(half), "the bloomery reheats it too");
        h.succeed();
    }

    private Ironworking() {}
}
