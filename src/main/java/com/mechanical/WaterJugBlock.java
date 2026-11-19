package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Fired clay Water Jug, standing on the ground: holds {@link #MAX} pours of water (block state {@link #WATER}, kept on
 * the item when it's picked up). Clay Bucket / Bucket / Glass Bottle take one pour out, a full one pours one in, and
 * standing under open sky it slowly fills in the rain. Held, see {@link WaterJugItem}.
 */
public class WaterJugBlock extends Block {
    public static final int MAX = 6;
    public static final IntegerProperty WATER = IntegerProperty.create("water", 0, MAX);
    /** Chance per random tick (about every 68 s) that rain adds a pour. */
    public static final float RAIN_CHANCE = 0.5F;
    private static final VoxelShape SHAPE = Shapes.or(box(3, 0, 3, 13, 9, 13), box(5, 9, 5, 11, 14, 11));

    public WaterJugBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(WATER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int water = state.getValue(WATER);
        ItemStack out;
        int change;
        if (water > 0 && stack.is(Mechanical.CLAY_BUCKET.get())) {
            out = stack.transmuteCopy(Mechanical.CLAY_WATER_BUCKET.get(), 1);
            change = -1;
        } else if (water > 0 && stack.is(Items.BUCKET)) {
            out = new ItemStack(Items.WATER_BUCKET);
            change = -1;
        } else if (water > 0 && stack.is(Items.GLASS_BOTTLE)) {
            out = PotionContents.createItemStack(Items.POTION, Potions.WATER);
            change = -1;
        } else if (water < MAX && stack.is(Mechanical.CLAY_WATER_BUCKET.get())) {
            out = ClayBucketItem.emptied(stack);
            change = 1;
        } else if (water < MAX && stack.is(Items.WATER_BUCKET)) {
            out = new ItemStack(Items.BUCKET);
            change = 1;
        } else {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel server) {
            SoundEvent sound = change < 0 ? (stack.is(Items.GLASS_BOTTLE) ? SoundEvents.BOTTLE_FILL : SoundEvents.BUCKET_FILL) : SoundEvents.BUCKET_EMPTY;
            server.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.1F);
            server.setBlock(pos, state.setValue(WATER, water + change), Block.UPDATE_ALL);
            if (out.isEmpty() && !player.hasInfiniteMaterials()) player.playSound(SoundEvents.DECORATED_POT_SHATTER, 0.8F, 1.1F); // a worn clay bucket cracked
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, out));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(WATER) < MAX;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isRainingAt(pos.above()) && random.nextFloat() < RAIN_CHANCE) level.setBlock(pos, state.setValue(WATER, state.getValue(WATER) + 1), Block.UPDATE_ALL);
    }
}
