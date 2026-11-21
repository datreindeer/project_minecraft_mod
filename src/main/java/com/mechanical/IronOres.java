package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Iron Age's new ores (docs/ages/03-iron-age.md): Galena (Raw Galena -> Lead Ingot in the Clay Kiln, Lead Ingot +
 * Bone Meal -> Leaded Cupel -> Silver Nugget by cupellation), gold (Raw Gold or 9 panned Gold Nuggets -> Gold Ingot in the
 * kiln), Limestone (crushed on the Stone Anvil into Flux, item tag {@link #FLUX}) and Rock Salt (Salt: salted meat and fish
 * that keep, salt-cured leather). Silver and Gold Ingots mint coins on the Bronze Anvil ({@link Coins}). The kiln recipes
 * live in {@link KilnBlockEntity#RECIPES}. Data: scripts/gen_iron_ores.py, textures: scripts/gen_iron_ores_textures.py.
 */
@EventBusSubscriber(modid = MODID)
public final class IronOres {
    /** Kiln heat: lead smelts on Charcoal alone (800° is its natural max); cupellation and gold need the bellows. */
    public static final int LEAD_TEMP = 800, CUPEL_TEMP = 1000, GOLD_TEMP = 1100;
    /** Limestone on the Stone Anvil. */
    public static final int FLUX_PER_LIMESTONE = 2;

    public static final DeferredBlock<Block> GALENA_ORE = BLOCKS.registerSimpleBlock("galena_ore", p -> p
            .mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F));
    public static final DeferredBlock<Block> DEEPSLATE_GALENA_ORE = BLOCKS.registerSimpleBlock("deepslate_galena_ore", p -> p
            .mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(4.5F, 3.0F)
            .sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<Block> LIMESTONE = BLOCKS.registerSimpleBlock("limestone", p -> p
            .mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(1.5F, 6.0F));
    public static final DeferredBlock<Block> ROCK_SALT = BLOCKS.registerSimpleBlock("rock_salt", p -> p
            .mapColor(MapColor.TERRACOTTA_WHITE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(2.0F, 3.0F)
            .sound(SoundType.CALCITE));
    public static final DeferredItem<BlockItem> GALENA_ORE_ITEM = ITEMS.registerSimpleBlockItem("galena_ore", GALENA_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_GALENA_ORE_ITEM = ITEMS.registerSimpleBlockItem("deepslate_galena_ore", DEEPSLATE_GALENA_ORE);
    public static final DeferredItem<BlockItem> LIMESTONE_ITEM = ITEMS.registerSimpleBlockItem("limestone", LIMESTONE);
    public static final DeferredItem<BlockItem> ROCK_SALT_ITEM = ITEMS.registerSimpleBlockItem("rock_salt", ROCK_SALT);

    public static final DeferredItem<Item> RAW_GALENA = ITEMS.registerSimpleItem("raw_galena");
    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.registerSimpleItem("lead_ingot");
    /** Lead Ingot pressed into a bone-ash cupel (crafted with Bone Meal); fired at {@link #CUPEL_TEMP}° it leaves a Silver Nugget. */
    public static final DeferredItem<Item> LEADED_CUPEL = ITEMS.registerSimpleItem("leaded_cupel");
    public static final DeferredItem<Item> SILVER_NUGGET = ITEMS.registerSimpleItem("silver_nugget");
    public static final DeferredItem<Item> SILVER_INGOT = ITEMS.registerSimpleItem("silver_ingot");
    public static final DeferredItem<Item> FLUX = ITEMS.registerSimpleItem("flux");
    public static final DeferredItem<Item> SALT = ITEMS.registerSimpleItem("salt");
    /** Salt-cured: no perishes_* tag, so they keep; a little less food than smoked. */
    public static final DeferredItem<Item> SALTED_MEAT = food("salted_meat", 3, 0.6F);
    public static final DeferredItem<Item> SALTED_FISH = food("salted_fish", 2, 0.6F);

    /** What the Bloomery can take as flux (the Bloomery side is wired up separately). */
    public static final TagKey<Item> FLUX_TAG = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "flux"));

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        return ITEMS.registerSimpleItem(name, p -> p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build()));
    }

    /** Stone Anvil crushing (called from {@link BronzeOres#crushed}). */
    static ItemStack crushed(ItemStack stack) {
        return stack.is(LIMESTONE_ITEM.get()) ? new ItemStack(FLUX.get(), FLUX_PER_LIMESTONE) : ItemStack.EMPTY;
    }

    // ---------------------------------------------------------------- game test

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "iron_ores"));

    static {
        TEST_FUNCTIONS.register("iron_ores", () -> IronOres::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "iron_ores"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "iron_ores"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 40, 0, true)));
    }

    private static void test(GameTestHelper h) {
        try {
            run(h);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw h.assertionException(e + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(e.getStackTrace(), 6)));
        }
    }

    private static List<ItemStack> drops(GameTestHelper h, BlockState state, Player miner, ItemStack tool) {
        return Block.getDrops(state, h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)), null, miner, tool);
    }

    private static boolean dropsOnly(List<ItemStack> drops, Item item) {
        return !drops.isEmpty() && drops.stream().allMatch(s -> s.is(item));
    }

    private static TransientCraftingContainer grid(ItemStack... items) {
        var grid = new TransientCraftingContainer(new AbstractContainerMenu(null, 0) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean stillValid(Player player) {
                return true;
            }
        }, 3, 3);
        for (int i = 0; i < items.length; i++) grid.setItem(i, items[i]);
        return grid;
    }

    private static ItemStack craft(GameTestHelper h, String what, ItemStack... items) {
        var input = grid(items).asCraftInput();
        return h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .orElseThrow(() -> h.assertionException("no recipe for " + what)).value().assemble(input);
    }

    private static void run(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Player miner = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack bronzePick = new ItemStack(Bronze.BRONZE_PICKAXE.get());

        // galena and rock salt need a Bronze Pickaxe; limestone any pickaxe; gold ore still an iron one
        for (Block ore : List.of(GALENA_ORE.get(), DEEPSLATE_GALENA_ORE.get(), ROCK_SALT.get())) {
            h.assertFalse(new ItemStack(Items.COPPER_PICKAXE).isCorrectToolForDrops(ore.defaultBlockState()), "a Copper Pickaxe gets nothing from " + ore);
            h.assertTrue(bronzePick.isCorrectToolForDrops(ore.defaultBlockState()), "a Bronze Pickaxe mines " + ore);
        }
        h.assertTrue(new ItemStack(Mechanical.FLINT_PICKAXE.get()).isCorrectToolForDrops(LIMESTONE.get().defaultBlockState()), "a Flint Pickaxe mines Limestone");
        h.assertFalse(bronzePick.isCorrectToolForDrops(Blocks.GOLD_ORE.defaultBlockState()), "Gold Ore still needs an iron pickaxe");

        // drops
        h.assertTrue(dropsOnly(drops(h, GALENA_ORE.get().defaultBlockState(), miner, bronzePick), RAW_GALENA.get()), "Galena Ore drops Raw Galena");
        h.assertTrue(dropsOnly(drops(h, DEEPSLATE_GALENA_ORE.get().defaultBlockState(), miner, bronzePick), RAW_GALENA.get()), "Deepslate Galena Ore drops Raw Galena");
        List<ItemStack> salt = drops(h, ROCK_SALT.get().defaultBlockState(), miner, bronzePick);
        h.assertTrue(dropsOnly(salt, SALT.get()) && salt.get(0).getCount() >= 2, "Rock Salt drops 2-3 Salt (got " + salt + ")");
        h.assertTrue(dropsOnly(drops(h, LIMESTONE.get().defaultBlockState(), miner, new ItemStack(Items.STONE_PICKAXE)), LIMESTONE_ITEM.get()), "Limestone drops itself");

        // limestone crushes into flux
        ItemStack flux = BronzeOres.crushed(new ItemStack(LIMESTONE_ITEM.get()));
        h.assertTrue(flux.is(FLUX.get()) && flux.getCount() == FLUX_PER_LIMESTONE && flux.is(FLUX_TAG), "Limestone crushes into 2 Flux, tagged mechanical:flux");
        // stone families (StoneFamilies): tool, drops, chisel chains, and data for every block the Java registers
        ItemStack flintPick = new ItemStack(Mechanical.FLINT_PICKAXE.get());
        for (StoneFamilies.Stone stone : StoneFamilies.STONES) {
            BlockState base = StoneFamilies.block(stone.base()).defaultBlockState();
            h.assertTrue(flintPick.isCorrectToolForDrops(base) != stone.hard(), "a Flint Pickaxe " + (stone.hard() ? "can't mine " : "mines ") + stone.base());
            h.assertTrue(bronzePick.isCorrectToolForDrops(base), "a Bronze Pickaxe mines " + stone.base());
            List<ItemStack> got = drops(h, base, miner, bronzePick);
            if (stone.name().equals("chalk"))
                h.assertTrue(dropsOnly(got, Pigments.CHALK.get()) && got.get(0).getCount() == 4, "Chalk Block drops 4 Chalk (got " + got + ")");
            else if (stone.name().equals("slate"))
                h.assertTrue(dropsOnly(got, StoneFamilies.block("cobbled_slate").asItem()), "Slate drops Cobbled Slate like deepslate (got " + got + ")");
            else h.assertTrue(dropsOnly(got, base.getBlock().asItem()), stone.base() + " drops itself (got " + got + ")");
            for (List<String> chain : stone.chains())
                for (int i = 0; i + 1 < chain.size(); i++)
                    h.assertTrue(BronzeTools.ChiselItem.carved(StoneFamilies.block(chain.get(i))) == StoneFamilies.block(chain.get(i + 1)),
                            "the chisel carves " + chain.get(i) + " -> " + chain.get(i + 1));
        }
        for (var block : StoneFamilies.BLOCKS_BY_NAME.values()) {
            var key = block.get().getLootTable().orElseThrow();
            h.assertTrue(level.getServer().reloadableRegistries().getLootTable(key) != net.minecraft.world.level.storage.loot.LootTable.EMPTY,
                    block.getId() + " has a loot table (gen_stone_families.py and StoneFamilies.java agree)");
        }
        // pillars turn like logs: quartz-template {}_pillar, basalt-template base and polished_{}
        for (String pillar : List.of("marble_pillar", "alabaster_pillar", "coastal_basalt", "polished_coastal_basalt", "scoria", "polished_scoria"))
            h.assertTrue(StoneFamilies.block(pillar).defaultBlockState().hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS),
                    pillar + " has an axis");
        h.assertFalse(StoneFamilies.block("smooth_coastal_basalt").defaultBlockState().hasProperty(net.minecraft.world.level.block.RotatedPillarBlock.AXIS),
                "Smooth Coastal Basalt has no axis (like vanilla)");

        // the kiln: lead at 800°, cupellation at 1000°, gold at 1100° (9 nuggets per ingot)
        KilnBlockEntity.Bake lead = KilnBlockEntity.bakeFor(new ItemStack(RAW_GALENA.get()));
        h.assertTrue(lead != null && lead.result.is(LEAD_INGOT.get()) && lead.temp == LEAD_TEMP && lead.count == 1, "Raw Galena -> Lead Ingot at 800°");
        KilnBlockEntity.Bake cupel = KilnBlockEntity.bakeFor(new ItemStack(LEADED_CUPEL.get()));
        h.assertTrue(cupel != null && cupel.result.is(SILVER_NUGGET.get()) && cupel.temp == CUPEL_TEMP, "Leaded Cupel -> Silver Nugget at 1000°");
        h.assertTrue(CUPEL_TEMP > KilnBlockEntity.FUELS.getLast().maxTemp() && GOLD_TEMP > KilnBlockEntity.FUELS.getLast().maxTemp(),
                "cupellation and gold need the bellows");
        KilnBlockEntity.Bake gold = KilnBlockEntity.bakeFor(new ItemStack(Items.RAW_GOLD));
        h.assertTrue(gold != null && gold.result.is(Items.GOLD_INGOT) && gold.temp == GOLD_TEMP, "Raw Gold -> Gold Ingot at 1100°");
        KilnBlockEntity.Bake nuggets = KilnBlockEntity.bakeFor(new ItemStack(Items.GOLD_NUGGET, 9));
        h.assertTrue(nuggets != null && nuggets.result.is(Items.GOLD_INGOT) && nuggets.count == 9, "9 Gold Nuggets -> Gold Ingot");
        // and nowhere else: no vanilla furnace recipe for Raw Gold, no crafting 9 nuggets into an ingot
        ItemStack rawGold = new ItemStack(Items.RAW_GOLD);
        for (RecipeType<?> type : List.of(RecipeType.SMELTING, RecipeType.BLASTING)) {
            @SuppressWarnings("unchecked")
            var cooking = (RecipeType<AbstractCookingRecipe>) type;
            h.assertTrue(level.recipeAccess().getRecipeFor(cooking, new SingleRecipeInput(rawGold), level).isEmpty(), "no " + type + " recipe for Raw Gold");
        }
        ItemStack[] nine = new ItemStack[9];
        for (int i = 0; i < 9; i++) nine[i] = new ItemStack(Items.GOLD_NUGGET);
        h.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, grid(nine).asCraftInput(), level).isEmpty(), "9 Gold Nuggets don't craft into an ingot");

        // crafting: the cupel, silver nuggets <-> ingot, salting
        h.assertTrue(craft(h, "Lead Ingot + Bone Meal", new ItemStack(LEAD_INGOT.get()), new ItemStack(Items.BONE_MEAL)).is(LEADED_CUPEL.get()),
                "Lead Ingot + Bone Meal make a Leaded Cupel");
        ItemStack[] silver = new ItemStack[9];
        for (int i = 0; i < 9; i++) silver[i] = new ItemStack(SILVER_NUGGET.get());
        h.assertTrue(craft(h, "9 Silver Nuggets", silver).is(SILVER_INGOT.get()), "9 Silver Nuggets make a Silver Ingot");
        ItemStack back = craft(h, "Silver Ingot", new ItemStack(SILVER_INGOT.get()));
        h.assertTrue(back.is(SILVER_NUGGET.get()) && back.getCount() == 9, "a Silver Ingot makes 9 Silver Nuggets");
        ItemStack meat = craft(h, "Mutton + Salt", new ItemStack(Items.MUTTON), new ItemStack(SALT.get()));
        h.assertTrue(meat.is(SALTED_MEAT.get()) && Preserving.shelfLife(meat) == 0, "Mutton + Salt make Salted Meat, which keeps");
        h.assertTrue(craft(h, "Cod + Salt", new ItemStack(Items.COD), new ItemStack(SALT.get())).is(SALTED_FISH.get()), "Cod + Salt make Salted Fish");
        h.assertTrue(craft(h, "Scraped Hide + Salt", new ItemStack(Mechanical.SCRAPED_HIDE.get()), new ItemStack(SALT.get())).is(Items.LEATHER),
                "a Scraped Hide salt-cured is Leather");

        // minting on the Bronze Anvil: Silver Ingot -> Silver Coins, Gold Ingot -> Gold Coins
        BlockPos anvilAt = new BlockPos(4, 1, 1);
        h.setBlock(anvilAt, Ironworking.BRONZE_ANVIL.get());
        BronzeAnvilBlockEntity anvil = (BronzeAnvilBlockEntity) level.getBlockEntity(h.absolutePos(anvilAt));
        ItemStack hammer = new ItemStack(Ironworking.BRONZE_SMITHING_HAMMER.get());
        miner.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        for (var mint : List.of(new ItemStack[] {new ItemStack(SILVER_INGOT.get()), new ItemStack(Coins.SILVER_COIN.get())},
                new ItemStack[] {new ItemStack(Items.GOLD_INGOT), new ItemStack(Coins.GOLD_COIN.get())})) {
            h.assertTrue(Coins.mintable(mint[0]), mint[0] + " is mintable");
            anvil.place(level, mint[0]);
            for (int i = 0; i < Coins.MINT_HITS; i++) anvil.strike(level, miner, hammer, InteractionHand.MAIN_HAND);
            h.assertItemEntityPresent(mint[1].getItem(), anvilAt, 2);
            h.assertTrue(anvil.item().isEmpty(), "the ingot is used up");
        }

        // panning: gold nuggets now and then (gravel: about 1 pan in 85, more by rivers and shores)
        var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE,
                Identifier.fromNamespaceAndPath(MODID, "gameplay/panning/gravel")));
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(h.absolutePos(BlockPos.ZERO)))
                .withParameter(LootContextParams.THIS_ENTITY, miner).withParameter(LootContextParams.TOOL, new ItemStack(BronzeOres.PAN_WITH_GRAVEL.get()))
                .create(LootContextParamSets.ARCHAEOLOGY);
        boolean found = false;
        for (int i = 0; i < 3000 && !found; i++) found = table.getRandomItems(params).stream().anyMatch(s -> s.is(Items.GOLD_NUGGET));
        h.assertTrue(found, "panning gravel finds Gold Nuggets");
        h.succeed();
    }

    private IronOres() {}
}
