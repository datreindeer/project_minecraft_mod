package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Sea salt (flora and food plan, phase 5): a shallow clay pan by the sea. Pour in seawater (any water bucket, in a beach
 * or ocean biome or the mod's coasts) and the sun dries it, a stage at a time ({@link #DRY} per random tick, by day under
 * open sky, not in rain), to a white crust: an empty hand scrapes up 2-3 Salt.
 */
public class SaltPanBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 4);
    public static final int SALT = 4;
    public static final float DRY = 0.3F;
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 3, 16);

    public SaltPanBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Seawater here: a beach, an ocean or one of the mod's coasts. */
    public static boolean seaside(Level level, BlockPos pos) {
        var biome = level.getBiome(pos);
        return biome.is(BiomeTags.IS_BEACH) || biome.is(BiomeTags.IS_OCEAN) || biome.unwrapKey().map(k -> k.identifier().getPath())
                .filter(p -> p.equals("shingle_beach") || p.equals("basalt_coast") || p.equals("stony_shore")).isPresent();
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!TripodBlockEntity.isWater(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (state.getValue(STAGE) != 0) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            if (!seaside(server, pos)) {
                player.sendOverlayMessage(Component.translatable("message.mechanical.salt_pan.fresh"));
                return InteractionResult.SUCCESS;
            }
            TripodBlockEntity.pourOut(player, hand, stack);
            server.setBlock(pos, state.setValue(STAGE, 1), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(STAGE) != SALT) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            popResource(server, pos.above(), new ItemStack(IronOres.SALT.get(), 2 + server.getRandom().nextInt(2)));
            server.setBlock(pos, state.setValue(STAGE, 0), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.8F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int stage = state.getValue(STAGE);
        if (stage == 0 || stage == SALT || level.isRaining() || level.isDarkOutside() || !level.canSeeSky(pos.above())) return;
        if (random.nextFloat() < DRY) level.setBlock(pos, state.setValue(STAGE, stage + 1), Block.UPDATE_CLIENTS);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        int stage = state.getValue(STAGE);
        return stage > 0 && stage < SALT;
    }
}
