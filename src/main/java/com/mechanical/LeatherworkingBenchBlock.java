package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Leatherworker's bench: right-click to sew clothes, armour and bags (see {@link LeatherworkingMenu}). */
public class LeatherworkingBenchBlock extends Block {
    private static final Component TITLE = Component.translatable("container.mechanical.leatherworking_bench");

    public LeatherworkingBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) -> new LeatherworkingMenu(id, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
    }
}
