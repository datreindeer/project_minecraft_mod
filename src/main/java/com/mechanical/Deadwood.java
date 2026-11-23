package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;

import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The pieces of a snag, a dead standing tree (worldgen ForestDecor.snag, docs/biomes.md): Deadwood, the silver-grey,
 * cracked wood left when the bark has fallen off (a log, any axis; burns like one); Broken Deadwood, the jagged top where
 * the trunk snapped; Deadwood Branch, a short dead stub that joins up like a chorus plant, and the thinner Deadwood
 * Twig (the dry land's dead trees); Shelf Fungus, brackets on dead
 * wood; and Fallen Bark, flakes of one wood's bark at the foot (gives that wood's Bark). Data, models and textures:
 * scripts/gen_deadwood.py.
 */
public final class Deadwood {
    public static final DeferredBlock<RotatedPillarBlock> DEADWOOD = BLOCKS.registerBlock("deadwood", RotatedPillarBlock::new,
            p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.5F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<BrokenTop> BROKEN_DEADWOOD = BLOCKS.registerBlock("broken_deadwood", BrokenTop::new,
            p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.5F).sound(SoundType.WOOD).ignitedByLava().noOcclusion());
    public static final DeferredBlock<AncientOak.Branch> DEADWOOD_BRANCH = BLOCKS.registerBlock("deadwood_branch", p -> new AncientOak.Branch(6, p),
            p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.8F).sound(SoundType.WOOD).ignitedByLava().noOcclusion());
    /** The thin, 4 px ends of a dead tree's limbs (worldgen DryColdDecor.deadTree), joining like Deadwood Branch. */
    public static final DeferredBlock<AncientOak.Branch> DEADWOOD_TWIG = BLOCKS.registerBlock("deadwood_twig", p -> new AncientOak.Branch(4, p),
            p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.5F).sound(SoundType.WOOD).ignitedByLava().noOcclusion());
    public static final DeferredBlock<ShelfFungus> SHELF_FUNGUS = BLOCKS.registerBlock("shelf_fungus", ShelfFungus::new,
            p -> p.mapColor(MapColor.TERRACOTTA_BROWN).strength(0.4F).sound(SoundType.FUNGUS).noCollision().noOcclusion().pushReaction(PushReaction.POPPED));
    /** Fallen Bark, one per wood (Mechanical.WOODS): a flat litter of bark flakes like Twigs. */
    public static final Map<String, DeferredBlock<TwigsBlock>> FALLEN_BARKS = Mechanical.WOODS.stream().collect(Collectors.toMap(w -> w, w ->
            BLOCKS.registerBlock(w + "_fallen_bark", TwigsBlock::new, p -> p.mapColor(MapColor.WOOD).noCollision().instabreak().replaceable()
                    .noOcclusion().sound(SoundType.GRASS).pushReaction(PushReaction.POPPED))));

    public static void init() {}

    /** The snapped-off top of a dead trunk: half a block of wood with jagged shards and a splinter standing up from it. */
    public static class BrokenTop extends Block {
        private static final VoxelShape SHAPE = Shapes.or(box(0, 0, 0, 16, 8, 16), box(2, 8, 2, 14, 13, 14));

        public BrokenTop(Properties p) {
            super(p);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }
    }

    /** Brackets of a polypore on the side of dead wood; FACING points away from the wood it grows on. */
    public static class ShelfFungus extends HorizontalDirectionalBlock {
        private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(box(2, 4, 9, 14, 13, 16));

        public ShelfFungus(Properties p) {
            super(p);
            registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPES.get(state.getValue(FACING));
        }

        /** Grows on wood: a log, Deadwood or a Grub Log behind it. */
        @Override
        protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            BlockState behind = level.getBlockState(pos.relative(state.getValue(FACING).getOpposite()));
            return behind.is(BlockTags.LOGS) || behind.is(DEADWOOD.get()) || behind.is(BROKEN_DEADWOOD.get())
                    || behind.is(com.mechanical.wildlife.temperate.TemperateFauna.GRUB_LOG.get());
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            Direction face = context.getClickedFace();
            if (face.getAxis().isVertical()) return null;
            BlockState state = defaultBlockState().setValue(FACING, face);
            return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
        }

        @Override
        protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
            return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                    ? Blocks.AIR.defaultBlockState()
                    : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
        }
    }

    private Deadwood() {}
}
