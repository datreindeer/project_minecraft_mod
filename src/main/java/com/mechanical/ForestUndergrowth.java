package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Forest undergrowth: hazel shrubs (pick hazelnuts), juniper (pick berries), thorny bramble, low undergrowth and sapling
 * thickets. All walk-through (no collision); bramble slows and pricks. Wild only, no block items. Data + worldgen in
 * scripts/gen_undergrowth.py, textures in scripts/gen_undergrowth_textures.py.
 */
public final class ForestUndergrowth {
    public static final DeferredItem<Item> HAZELNUTS = ITEMS.registerSimpleItem("hazelnuts",
            p -> p.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build()));
    public static final DeferredItem<Item> ROASTED_HAZELNUTS = ITEMS.registerSimpleItem("roasted_hazelnuts",
            p -> p.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.7F).build()));
    public static final DeferredItem<Item> JUNIPER_BERRIES = ITEMS.registerSimpleItem("juniper_berries",
            p -> p.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.2F).build()));

    private static BlockBehaviour.Properties plant(BlockBehaviour.Properties p, SoundType sound, float strength) {
        return p.mapColor(MapColor.PLANT).noCollision().strength(strength).sound(sound).pushReaction(PushReaction.POPPED);
    }

    public static final DeferredBlock<HazelShrubBlock> HAZEL_SHRUB = BLOCKS.registerBlock("hazel_shrub",
            p -> new HazelShrubBlock(HAZELNUTS, p), p -> plant(p, SoundType.AZALEA, 0.4F).randomTicks());
    public static final DeferredBlock<PickableShrubBlock> JUNIPER = BLOCKS.registerBlock("juniper",
            p -> new PickableShrubBlock(JUNIPER_BERRIES, VegetationBlock.column(14, 0, 12), p),
            p -> plant(p, SoundType.AZALEA, 0.4F).randomTicks());
    public static final DeferredBlock<BrambleBlock> BRAMBLE = BLOCKS.registerBlock("bramble",
            p -> new BrambleBlock(VegetationBlock.column(16, 0, 10), p), p -> plant(p, SoundType.SWEET_BERRY_BUSH, 0.3F));
    public static final DeferredBlock<ShrubBlock> UNDERGROWTH = BLOCKS.registerBlock("undergrowth",
            p -> new ShrubBlock(VegetationBlock.column(14, 0, 7), p), p -> plant(p, SoundType.GRASS, 0F).replaceable());
    public static final DeferredBlock<DoublePlantBlock> SAPLING_THICKET = BLOCKS.registerBlock("sapling_thicket",
            DoublePlantBlock::new, p -> plant(p, SoundType.AZALEA, 0.4F));
    /** Low flowering heather of the Pine Heath (scripts/gen_biomes.py). */
    public static final DeferredBlock<ShrubBlock> HEATHER = BLOCKS.registerBlock("heather",
            p -> new ShrubBlock(VegetationBlock.column(14, 0, 9), p), p -> plant(p, SoundType.GRASS, 0F).replaceable());

    public static void init() {}

    /** Light enough to grow fruit? Same rule as the wild berry bushes. */
    static boolean bright(ServerLevel level, BlockPos pos) {
        return level.getRawBrightness(pos.above(), 0) >= 9;
    }

    static void pickSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);
    }

    /** A plain one-block shrub with its own outline. */
    public static class ShrubBlock extends VegetationBlock {
        private final VoxelShape shape;

        public ShrubBlock(VoxelShape shape, Properties properties) {
            super(properties);
            this.shape = shape;
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return shape;
        }
    }

    /** Thorny thicket: slows anything walking through and pricks it, unless it sneaks through carefully. */
    public static class BrambleBlock extends ShrubBlock {
        public BrambleBlock(VoxelShape shape, Properties properties) {
            super(shape, properties);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean isPrecise) {
            if (!(entity instanceof LivingEntity)) return;
            entity.makeStuckInBlock(state, new Vec3(0.8F, 0.75, 0.8F));
            if (level instanceof ServerLevel server && !entity.isSteppingCarefully() && level.getRandom().nextInt(8) == 0) {
                Vec3 move = entity.isClientAuthoritative() ? entity.getKnownMovement() : entity.oldPosition().subtract(entity.position());
                if (move.horizontalDistanceSqr() > 1.0E-5) entity.hurtServer(server, level.damageSources().sweetBerryBush(), 1.0F);
            }
        }
    }

    /** One-block shrub that carries fruit (juniper berries): pick them, they come back slowly in the light. */
    public static class PickableShrubBlock extends ShrubBlock {
        public static final BooleanProperty BERRIES = BooleanProperty.create("berries");
        /** ~68 s between random ticks per block, so ~14 min to regrow. */
        private static final int GROW_ONE_IN = 12;
        private final Supplier<Item> fruit;

        public PickableShrubBlock(Supplier<Item> fruit, VoxelShape shape, Properties properties) {
            super(shape, properties);
            this.fruit = fruit;
            registerDefaultState(defaultBlockState().setValue(BERRIES, true));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(BERRIES);
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return !state.getValue(BERRIES);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextInt(GROW_ONE_IN) == 0 && bright(level, pos)) level.setBlock(pos, state.setValue(BERRIES, true), Block.UPDATE_CLIENTS);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!state.getValue(BERRIES)) return InteractionResult.PASS;
            if (level instanceof ServerLevel server) {
                level.setBlock(pos, state.setValue(BERRIES, false), Block.UPDATE_CLIENTS);
                pickSound(level, pos);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
                Block.popResource(server, pos, new ItemStack(fruit.get(), 1 + level.getRandom().nextInt(3)));
                // juniper is winter-hardy, like lingonberry
                if (player instanceof net.minecraft.server.level.ServerPlayer sp && Calendar.season(server.getOverworldClockTime()) == 3) {
                    Kettle.award(sp, WildBerryBushBlock.WINTER_BERRIES);
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** Two-block hazel shrub. Both halves carry NUTS; pick 1-2 hazelnuts from either half, they ripen again slowly (~23 min). */
    public static class HazelShrubBlock extends DoublePlantBlock {
        public static final BooleanProperty NUTS = BooleanProperty.create("nuts");
        private static final int GROW_ONE_IN = 20;
        private static final VoxelShape LOWER = column(14, 0, 16), UPPER = column(14, 0, 14);
        private final Supplier<Item> nuts;

        public HazelShrubBlock(Supplier<Item> nuts, Properties properties) {
            super(properties);
            this.nuts = nuts;
            registerDefaultState(defaultBlockState().setValue(NUTS, true));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(NUTS);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return !state.getValue(NUTS) && state.getValue(HALF) == DoubleBlockHalf.LOWER;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextInt(GROW_ONE_IN) == 0 && bright(level, pos.above())) setNuts(level, pos, true);
        }

        private void setNuts(Level level, BlockPos lower, boolean has) {
            for (BlockPos p : new BlockPos[] {lower, lower.above()}) {
                BlockState s = level.getBlockState(p);
                if (s.is(this)) level.setBlock(p, s.setValue(NUTS, has), Block.UPDATE_CLIENTS);
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!state.getValue(NUTS)) return InteractionResult.PASS;
            if (level instanceof ServerLevel server) {
                setNuts(level, state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below(), false);
                pickSound(level, pos);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
                Block.popResource(server, pos, new ItemStack(nuts.get(), 1 + level.getRandom().nextInt(2)));
            }
            return InteractionResult.SUCCESS;
        }
    }

    private ForestUndergrowth() {}
}
