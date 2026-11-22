package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Mold Chest (living-world plan F7): a clay-daubed plank chest of 27 slots that only takes molds (tag mechanical:molds), from hand or hopper. */
public class MoldChestBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Every clay and soapstone mold, fired or not (scripts/gen_mold_chest.py). */
    public static final TagKey<Item> MOLDS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "molds"));
    public static final int SIZE = 27;
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 14, 15);

    public MoldChestBlock(Properties properties) {
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof Entity chest) player.openMenu(chest);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    /** The 27 slots; {@link #canPlaceItem} keeps hoppers to molds. */
    public static class Entity extends BaseContainerBlockEntity {
        private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

        public Entity(BlockPos pos, BlockState state) {
            super(MoldBench.MOLD_CHEST_ENTITY.get(), pos, state);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.is(MOLDS);
        }

        @Override
        public void startOpen(ContainerUser user) {
            if (!remove && level != null && !user.getLivingEntity().isSpectator())
                level.playSound(null, worldPosition, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.5F, 0.9F + level.getRandom().nextFloat() * 0.1F);
        }

        @Override
        public void stopOpen(ContainerUser user) {
            if (!remove && level != null && !user.getLivingEntity().isSpectator())
                level.playSound(null, worldPosition, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.5F, 0.9F + level.getRandom().nextFloat() * 0.1F);
        }

        @Override
        protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
            return new Menu(containerId, inventory, this);
        }

        @Override
        protected Component getDefaultName() {
            return Component.translatable("block.mechanical.mold_chest");
        }

        @Override
        public int getContainerSize() {
            return SIZE;
        }

        @Override
        protected NonNullList<ItemStack> getItems() {
            return items;
        }

        @Override
        protected void setItems(NonNullList<ItemStack> items) {
            this.items = items;
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            ContainerHelper.saveAllItems(output, items);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, items);
        }
    }

    /** Vanilla's 9x3 chest menu (so the vanilla chest screen draws it) whose chest slots only take molds. */
    public static class Menu extends ChestMenu {
        public Menu(int containerId, Inventory inventory) {
            this(containerId, inventory, new SimpleContainer(SIZE));
        }

        public Menu(int containerId, Inventory inventory, Container container) {
            super(MoldBench.MOLD_CHEST_MENU.get(), containerId, inventory, container, 3);
            for (int i = 0; i < SIZE; i++) {
                Slot old = slots.get(i);
                Slot filtered = new Slot(container, i, old.x, old.y) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.is(MOLDS);
                    }
                };
                filtered.index = i;
                slots.set(i, filtered);
            }
        }
    }
}
