package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StrawBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Iron Age weaving (docs/ages/03-iron-age.md "Vävstol och Cloth Bed"). The Warp-weighted Loom weaves
 * {@value #THREAD_PER_CLOTH} thread into a Cloth: Wool, Linen or Silk (Silk Thread comes only from Trader Caravans). Three
 * of any Cloth over Straw and Planks make a Cloth Bed: it rests like a Furred Straw Bed (StrawBedRest) but, unlike straw,
 * isn't used up when you get out of it. Data: scripts/gen_weaving.py, textures: scripts/gen_weaving_textures.py.
 */
@EventBusSubscriber(modid = MODID)
public final class Weaving {
    public static final int THREAD_PER_CLOTH = 3;
    public static final TagKey<Item> CLOTH = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "cloth"));

    public static final DeferredItem<Item> SILK_THREAD = ITEMS.registerSimpleItem("silk_thread");
    public static final DeferredItem<Item> WOOL_CLOTH = ITEMS.registerSimpleItem("wool_cloth");
    public static final DeferredItem<Item> LINEN_CLOTH = ITEMS.registerSimpleItem("linen_cloth");
    public static final DeferredItem<Item> SILK_CLOTH = ITEMS.registerSimpleItem("silk_cloth");

    public static final DeferredBlock<LoomBlock> WARP_WEIGHTED_LOOM = BLOCKS.registerBlock("warp_weighted_loom", LoomBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredItem<BlockItem> WARP_WEIGHTED_LOOM_ITEM = ITEMS.registerSimpleBlockItem("warp_weighted_loom", WARP_WEIGHTED_LOOM);
    public static final DeferredBlock<ClothBedBlock> CLOTH_BED = BLOCKS.registerBlock("cloth_bed", ClothBedBlock::new,
            p -> p.mapColor(MapColor.WOOL).sound(SoundType.WOOL).strength(0.2F).noOcclusion().ignitedByLava());
    public static final DeferredItem<BlockItem> CLOTH_BED_ITEM = ITEMS.registerSimpleBlockItem("cloth_bed", CLOTH_BED, p -> p.stacksTo(1));

    /** Thread → the cloth the loom weaves of it. */
    static Map<Item, Supplier<Item>> cloths() {
        return Map.of(Mechanical.WOOL_THREAD.get(), WOOL_CLOTH, Spinning.LINEN_THREAD.get(), LINEN_CLOTH, SILK_THREAD.get(), SILK_CLOTH);
    }

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "weaving"));

    static {
        TEST_FUNCTIONS.register("weaving", () -> Weaving::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    /** Upright frame, warp hanging from the top beam and held taut by clay weights. Right-click with thread to weave. */
    public static class LoomBlock extends Block {
        public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
        private static final VoxelShape SHAPE_NS = box(0, 0, 5, 16, 16, 11), SHAPE_EW = box(5, 0, 0, 11, 16, 16);

        public LoomBlock(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected BlockState rotate(BlockState state, Rotation rotation) {
            return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
        }

        @Override
        protected BlockState mirror(BlockState state, Mirror mirror) {
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!cloths().containsKey(stack.getItem())) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (level instanceof ServerLevel server) weave(server, pos, player, stack);
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.mechanical.loom.how", THREAD_PER_CLOTH));
            return InteractionResult.SUCCESS;
        }
    }

    /** Weaves {@value #THREAD_PER_CLOTH} of the held thread into a cloth (into the hand, or dropped); false if too little. */
    static boolean weave(ServerLevel level, BlockPos pos, Player player, ItemStack thread) {
        Supplier<Item> cloth = cloths().get(thread.getItem());
        if (cloth == null) return false;
        if (thread.getCount() < THREAD_PER_CLOTH && !player.hasInfiniteMaterials()) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.loom.more", THREAD_PER_CLOTH));
            return false;
        }
        thread.consume(THREAD_PER_CLOTH, player);
        ItemStack out = new ItemStack(cloth.get());
        if (!player.getInventory().add(out)) Block.popResourceFromFace(level, pos, Direction.UP, out);
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.3F);
        level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6F, 1.4F); // the weaving sword beats the weft up
        return true;
    }

    /** A Straw Bed's rest and fast night in a bed that keeps: vanilla's bed rule (no straw "destroy on leave"). */
    public static class ClothBedBlock extends StrawBedBlock {
        public ClothBedBlock(Properties properties) {
            super(properties);
        }

        @Override
        protected EnvironmentAttribute<BedRule> getBedEnvironmentAttribute() {
            return EnvironmentAttributes.BED_RULE;
        }
    }

    // ---- game test: each thread weaves its cloth, too little does nothing, the Cloth Bed outlasts getting up ----

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "weaving"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "weaving"), new FunctionGameTestInstance(TEST, TestArea.of(env, 20, true)));
    }

    private static void test(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos loom = h.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(loom, WARP_WEIGHTED_LOOM.get().defaultBlockState());
        Player player = h.makeMockPlayer(GameType.SURVIVAL);
        for (var e : cloths().entrySet()) {
            ItemStack thread = new ItemStack(e.getKey(), THREAD_PER_CLOTH + 1);
            h.assertTrue(weave(level, loom, player, thread), "the loom weaves " + e.getKey());
            h.assertTrue(thread.getCount() == 1, "weaving takes " + THREAD_PER_CLOTH + " thread");
            h.assertTrue(player.getInventory().countItem(e.getValue().get()) == 1, e.getKey() + " becomes " + e.getValue().get());
            h.assertFalse(weave(level, loom, player, thread), "one thread is too little");
        }
        h.assertTrue(new ItemStack(SILK_CLOTH.get()).is(CLOTH), "Silk Cloth is cloth (for the bed)");

        BlockPos foot = h.absolutePos(new BlockPos(5, 1, 5)), head = foot.north();
        BlockState bed = CLOTH_BED.get().defaultBlockState().setValue(AbstractBedBlock.FACING, Direction.NORTH);
        level.setBlockAndUpdate(foot, bed.setValue(AbstractBedBlock.PART, BedPart.FOOT));
        level.setBlockAndUpdate(head, bed.setValue(AbstractBedBlock.PART, BedPart.HEAD));
        CLOTH_BED.get().onStopSleeping(level, head);
        h.assertTrue(level.getBlockState(head).is(CLOTH_BED.get()) && level.getBlockState(foot).is(CLOTH_BED.get()), "the Cloth Bed is still there after getting up");
        h.assertFalse(CLOTH_BED.get().getBedRule(level, head).destroyOnLeave(), "the Cloth Bed's rule doesn't destroy it");
        h.succeed();
    }

    private Weaving() {}
}
