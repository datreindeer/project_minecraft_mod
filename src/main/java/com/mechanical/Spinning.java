package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

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
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * More thread than wool (docs/ages/01-stone-age.md "Tråd", 02-bronze-age.md "Lin"). Stinging Nettle grows wild in forests
 * and on riverbanks and stings bare legs; its stalks are stripped with a knife into Nettle Fiber. Flax is a Bronze Age crop
 * (seeds from the village Farmer at trade level 3); its stalks are retted in a Clay Water Bucket and scutched with a knife
 * into Flax Fiber. The Drop Spindle spins fibre (and wool) into thread in the crafting grid; Sinew is split with a knife
 * into Sinew Thread. Every thread is in {@link #THREADS}, which the Armor Station sews with.
 * Data: scripts/gen_thread.py, textures: scripts/gen_thread_textures.py.
 */
@EventBusSubscriber(modid = MODID)
public final class Spinning {
    public static final TagKey<Item> THREADS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "threads"));
    public static final ResourceKey<DamageType> NETTLE_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(MODID, "nettle"));
    /** A sting: half a heart, about once a second while you push through without leggings. */
    public static final float STING_DAMAGE = 1.0F;
    public static final int STING_ONE_IN = 12, GROW_ONE_IN = 8;

    public static final DeferredBlock<NettleBlock> STINGING_NETTLE = BLOCKS.registerBlock("stinging_nettle", NettleBlock::new, p -> p
            .mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.GRASS).offsetType(Block.OffsetType.XZ)
            .pushReaction(PushReaction.POPPED).randomTicks());
    public static final DeferredItem<BlockItem> NETTLE_SEEDS = ITEMS.registerItem("nettle_seeds", p -> new BlockItem(STINGING_NETTLE.get(), p));
    public static final DeferredBlock<FarmCropBlock> FLAX_CROP = BLOCKS.registerBlock("flax_crop",
            p -> new FarmCropBlock(() -> Spinning.FLAX_SEEDS.get(), p), Farming::cropProperties);
    public static final DeferredItem<BlockItem> FLAX_SEEDS = ITEMS.registerItem("flax_seeds", p -> new BlockItem(FLAX_CROP.get(), p));

    public static final DeferredItem<Item> NETTLE_STALKS = ITEMS.registerSimpleItem("nettle_stalks");
    public static final DeferredItem<Item> NETTLE_FIBER = ITEMS.registerSimpleItem("nettle_fiber");
    public static final DeferredItem<Item> FLAX_STALKS = ITEMS.registerSimpleItem("flax_stalks");
    public static final DeferredItem<Item> FLAX_FIBER = ITEMS.registerSimpleItem("flax_fiber");
    public static final DeferredItem<Item> NETTLE_THREAD = ITEMS.registerSimpleItem("nettle_thread");
    public static final DeferredItem<Item> LINEN_THREAD = ITEMS.registerSimpleItem("linen_thread");
    public static final DeferredItem<Item> SINEW_THREAD = ITEMS.registerSimpleItem("sinew_thread");
    public static final DeferredItem<GridToolItem> DROP_SPINDLE = ITEMS.registerItem("drop_spindle", GridToolItem::new, p -> p.durability(64));

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "thread"));

    static {
        TEST_FUNCTIONS.register("thread", () -> Spinning::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    /** Stings the player unless they wear something on their legs; true if it hurt. */
    public static boolean sting(ServerLevel level, Player player) {
        if (!player.getItemBySlot(EquipmentSlot.LEGS).isEmpty()) return false;
        return player.hurtServer(level, level.damageSources().source(NETTLE_DAMAGE), STING_DAMAGE);
    }

    /** Wild nettle: age 1 grown (worldgen), age 0 a seedling from Nettle Seeds that grows up in the light and doesn't sting yet. */
    public static class NettleBlock extends WildPlantBlock {
        public static final IntegerProperty AGE = BlockStateProperties.AGE_1;

        public NettleBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(AGE, 1));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AGE);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(AGE, 0);
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return state.getValue(AGE) == 0;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextInt(GROW_ONE_IN) == 0 && level.getRawBrightness(pos, 0) >= 9) level.setBlock(pos, state.setValue(AGE, 1), Block.UPDATE_CLIENTS);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean isPrecise) {
            // players only: wild animals wander through nettle beds all day
            if (state.getValue(AGE) == 0 || !(entity instanceof Player player) || !(level instanceof ServerLevel server)
                    || player.isSteppingCarefully() || level.getRandom().nextInt(STING_ONE_IN) != 0) return;
            Vec3 move = player.isClientAuthoritative() ? player.getKnownMovement() : player.oldPosition().subtract(player.position());
            if (move.horizontalDistanceSqr() > 1.0E-5) sting(server, player);
        }
    }

    // ---- game test: the sting, and the spindle recipe keeps its tool ----

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "default"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "thread"),
                new FunctionGameTestInstance(TEST, new TestData<>(env, Identifier.withDefaultNamespace("empty"), 20, 0, true)));
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Player bare = h.makeMockPlayer(GameType.SURVIVAL);
        float before = bare.getHealth();
        h.assertTrue(sting(level, bare) && bare.getHealth() < before, "a nettle stings bare legs");
        Player dressed = h.makeMockPlayer(GameType.SURVIVAL);
        dressed.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        h.assertFalse(sting(level, dressed), "leggings keep the sting off");

        CraftingInput in = CraftingInput.of(2, 1, List.of(new ItemStack(FLAX_FIBER.get()), new ItemStack(DROP_SPINDLE.get())));
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, in, level)
                .orElseThrow(() -> h.assertionException("no recipe for Flax Fiber + Drop Spindle"));
        h.assertTrue(recipe.value().assemble(in).is(LINEN_THREAD.get()), "Flax Fiber spins into Linen Thread");
        ItemStack left = recipe.value().getRemainingItems(in).get(1);
        h.assertTrue(left.is(DROP_SPINDLE.get()) && left.getDamageValue() == 1, "the Drop Spindle stays in the grid with 1 damage");
        h.succeed();
    }

    private Spinning() {}
}
