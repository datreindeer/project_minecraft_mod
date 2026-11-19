package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A straw-lined plank bin with a lid, opened like a Storage Jar; dug into the ground it keeps produce longer ({@link RootCellarBlockEntity}). */
public class RootCellarBlock extends StorageJarBlock {
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 14, 16);

    public RootCellarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RootCellarBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && !Roots.dugIn(level, pos)) player.sendOverlayMessage(Component.translatable("message.mechanical.root_cellar.not_dug_in"));
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
