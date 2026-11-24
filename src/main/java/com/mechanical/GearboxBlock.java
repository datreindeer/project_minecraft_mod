package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Gearbox (see {@link WaterPower}): a timber frame with bevel gears on the X and Z lines (and on the upright line
 * when a shaft stands on it or hangs under it) that passes a turn straight on and round the corner to every side, up
 * and down too. Its {@link RotorBlockEntity} keeps the turn about X; the Z and Y gears turn the other way
 * ({@link WaterPower#gear}; drawn by client/WaterPowerRenderers).
 */
public class GearboxBlock extends Block implements EntityBlock {
    public GearboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof RotorBlockEntity r) WaterPower.tellPower(player, r.power());
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RotorBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != WaterPower.ROTOR_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<RotorBlockEntity>) (l, p, s, r) -> {
            if (l.getGameTime() % 20 == 7) r.lapse();
        };
    }
}
