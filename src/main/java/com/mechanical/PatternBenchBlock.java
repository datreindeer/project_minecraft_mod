package com.mechanical;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Pattern Bench (docs/steel-furnace-plan.md section 6): a joiner's bench where Wooden Patterns are cut from planks,
 * built like the Mold Bench ({@link RecipeStationMenu}): planks in, pick the pattern from the list. Every flask shape
 * ({@link MoldingFlaskBlock.FlaskShape}) and the Pig Pattern for a Pig Bed.
 */
public class PatternBenchBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    private static final Component TITLE = Component.translatable("container.mechanical.pattern_bench");

    public static final List<StationRecipe> RECIPES = recipes();

    private static List<StationRecipe> recipes() {
        List<StationRecipe> r = new ArrayList<>();
        r.add(StationRecipe.of(() -> HighFurnace.PIG_PATTERN.get(), 1, StationRecipe.Part.of(ItemTags.PLANKS, () -> Items.OAK_PLANKS, 2), null));
        for (var e : HighFurnace.PATTERNS.entrySet()) {
            int planks = e.getKey().mb >= 800 ? 4 : e.getKey().mb >= 400 ? 3 : 2;
            r.add(StationRecipe.of(() -> e.getValue().get(), 1, StationRecipe.Part.of(ItemTags.PLANKS, () -> Items.OAK_PLANKS, planks), null));
        }
        return r;
    }

    public PatternBenchBlock(Properties properties) {
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) player.openMenu(state.getMenuProvider(level, pos));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider((id, inventory, player) -> new Menu(id, inventory, ContainerLevelAccess.create(level, pos)), TITLE);
    }

    public static class Menu extends RecipeStationMenu {
        public Menu(int containerId, Inventory inventory) {
            this(containerId, inventory, ContainerLevelAccess.NULL);
        }

        public Menu(int containerId, Inventory inventory, ContainerLevelAccess access) {
            super(HighFurnace.PATTERN_BENCH_MENU.get(), containerId, inventory, access, HighFurnace.PATTERN_BENCH, RECIPES, 1);
        }
    }
}
