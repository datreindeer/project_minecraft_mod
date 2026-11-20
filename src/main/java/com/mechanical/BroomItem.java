package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeafLitterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Broom (Tool Station: Straw + Twine + Tool Rod): each sweep thins the {@link SootBlock} soot in the 3x3 around the
 * clicked spot by one stage (the lightest goes), on the surface clicked, and sweeps leaf litter a leaf thinner too.
 * One durability per sweep that moved anything.
 */
public class BroomItem extends Item {
    private static final int COOLDOWN = 8;
    static final net.minecraft.resources.Identifier ADVANCEMENT = net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "stone_age/broom");

    public BroomItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null && context.getPlayer().getCooldowns().isOnCooldown(context.getItemInHand())) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        // clicking the soot (or litter) itself sweeps around it; clicking the wall or floor it lies on sweeps in front of that face
        BlockPos centre = sweepable(level.getBlockState(clicked)) ? clicked : clicked.relative(face);
        int swept = 0;
        boolean soot = false;
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                BlockPos p = switch (face.getAxis()) {
                    case X -> centre.offset(0, a, b);
                    case Y -> centre.offset(a, 0, b);
                    case Z -> centre.offset(a, b, 0);
                };
                BlockState state = level.getBlockState(p);
                if (!sweep(level, p, state)) continue;
                soot |= state.is(Mechanical.SOOT.get());
                swept++;
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5,
                        4, 0.3, 0.3, 0.3, 0.02);
            }
        }
        level.playSound(null, centre, SoundEvents.BRUSH_GENERIC, SoundSource.PLAYERS, 0.8F, 0.8F + level.getRandom().nextFloat() * 0.3F);
        if (context.getPlayer() != null) {
            context.getPlayer().getCooldowns().addCooldown(context.getItemInHand(), COOLDOWN);
            if (soot && context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer sp) Kettle.award(sp, ADVANCEMENT);
            if (swept > 0) context.getItemInHand().hurtAndBreak(1, context.getPlayer(), context.getHand());
        }
        return swept > 0 ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
    }

    private static boolean sweepable(BlockState state) {
        return state.is(Mechanical.SOOT.get()) || state.getBlock() instanceof LeafLitterBlock;
    }

    /** One stage of soot, or one leaf of litter, off {@code pos}. */
    static boolean sweep(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof LeafLitterBlock) {
            int leaves = state.getValue(BlockStateProperties.SEGMENT_AMOUNT);
            if (leaves > 1) level.setBlock(pos, state.setValue(BlockStateProperties.SEGMENT_AMOUNT, leaves - 1), Block.UPDATE_ALL);
            else level.removeBlock(pos, false);
            return true;
        }
        return SootBlock.sweep(level, pos);
    }
}
