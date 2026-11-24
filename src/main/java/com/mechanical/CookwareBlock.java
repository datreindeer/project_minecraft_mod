package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cookware that holds a few things and turns them into a dish (Kitchen.DISHES): the Bronze Griddle and Iron Skillet
 * stand on their legs over a campfire and cook while it burns (food left on too long burns); the Fermenting Barrel and
 * the Cheese Press need no fire, only time. Right-click with an ingredient to put it in, an empty hand to take out
 * what is done (or, while it works, to see how long it has left), sneaking to take the raw things back out.
 * The contents: {@link CookwareBlockEntity}; pans draw what lies on them (client.CookwareRenderer).
 */
public class CookwareBlock extends Block implements EntityBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final BooleanProperty PRESSING = BooleanProperty.create("pressing");

    /** Kind: which dishes (tool, tier), how many things it holds, whether it stands over a campfire. */
    public enum Kind {
        GRIDDLE(Kitchen.Tool.PAN, 1, 4, true), SKILLET(Kitchen.Tool.PAN, 2, 4, true), BARREL(Kitchen.Tool.BARREL, 2, 8, false), PRESS(Kitchen.Tool.PRESS, 2, 4, false);

        public final Kitchen.Tool tool;
        public final int tier, capacity;
        public final boolean fire;

        Kind(Kitchen.Tool tool, int tier, int capacity, boolean fire) {
            this.tool = tool;
            this.tier = tier;
            this.capacity = capacity;
            this.fire = fire;
        }
    }

    private static final VoxelShape PAN_SHAPE = box(1, -9, 1, 15, 3.5, 15), BARREL_SHAPE = box(1, 0, 1, 15, 15, 15), PRESS_SHAPE = box(1, 0, 1, 15, 16, 15);
    public final Kind kind;

    protected CookwareBlock(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        BlockState s = stateDefinition.any();
        if (s.hasProperty(HorizontalDirectionalBlock.FACING)) s = s.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH);
        if (s.hasProperty(OPEN)) s = s.setValue(OPEN, true);
        if (s.hasProperty(PRESSING)) s = s.setValue(PRESSING, false);
        registerDefaultState(s);
    }

    /** The block for this kind, with its own state property (createBlockStateDefinition runs before the kind is known). */
    public static CookwareBlock create(Kind kind, Properties p) {
        return switch (kind) {
            case GRIDDLE, SKILLET -> new CookwareBlock(kind, p) {
                @Override
                protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
                    builder.add(HorizontalDirectionalBlock.FACING);
                }
            };
            case BARREL -> new CookwareBlock(kind, p) {
                @Override
                protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
                    builder.add(OPEN);
                }
            };
            case PRESS -> new CookwareBlock(kind, p) {
                @Override
                protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
                    builder.add(PRESSING);
                }
            };
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (kind.fire && !context.getLevel().getBlockState(context.getClickedPos().below()).is(BlockTags.CAMPFIRES)) {
            if (context.getPlayer() != null && context.getLevel().isClientSide())
                context.getPlayer().sendOverlayMessage(Component.translatable("message.mechanical.pan.no_fire"));
            return null;
        }
        BlockState s = defaultBlockState();
        return s.hasProperty(HorizontalDirectionalBlock.FACING) ? s.setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection()) : s;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !kind.fire || level.getBlockState(pos.below()).is(BlockTags.CAMPFIRES);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) ticks.scheduleTick(pos, this, 1);
        return super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) level.destroyBlock(pos, true);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind.fire ? PAN_SHAPE : kind == Kind.BARREL ? BARREL_SHAPE : PRESS_SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof CookwareBlockEntity pan)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (stack.is(Mechanical.FIREWOOD_TAG) || stack.getItem() instanceof FireDrillItem) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level instanceof ServerLevel server) pan.add(server, player, hand, stack);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CookwareBlockEntity pan) || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) pan.takeOut(server, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CookwareBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != Kitchen.COOKWARE_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<CookwareBlockEntity>) (l, p, s, t) -> t.serverTick((ServerLevel) l, p, s);
    }
}
