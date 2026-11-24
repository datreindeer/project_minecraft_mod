package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Coke Oven (docs/steel-furnace-plan.md phase 7): a beehive oven of silica brick. Right-click it with Coal: once it
 * holds {@link #BATCH}, it closes and bakes them without fuel for {@link #BAKE_TICKS} (10 minutes) into {@link #YIELD} Coke;
 * an empty hand takes the Coke. Coke burns hotter in a High Furnace's Fireboxes (950° natural, 1750° ceiling).
 */
public class CokeOvenBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int BATCH = 16, YIELD = 12, BAKE_TICKS = 12000;

    public CokeOvenBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(Items.COAL) || !(level.getBlockEntity(pos) instanceof Entity o)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (state.getValue(LIT) || o.coke > 0) return InteractionResult.FAIL;
        int n = Math.min(stack.getCount(), BATCH - o.coal);
        o.coal += n;
        if (!player.getAbilities().instabuild) stack.shrink(n);
        if (o.coal >= BATCH) o.start(server);
        o.setChanged();
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel) || !(level.getBlockEntity(pos) instanceof Entity o)) return InteractionResult.SUCCESS;
        if (o.coke > 0) {
            ItemStack out = new ItemStack(HighFurnace.COKE.get(), o.coke);
            if (!player.getInventory().add(out)) Block.popResource(level, pos.relative(state.getValue(FACING)), out);
            o.coke = 0;
            o.setChanged();
        } else player.sendOverlayMessage(Component.translatable(state.getValue(LIT) ? "message.mechanical.coke_oven.baking" : "message.mechanical.coke_oven.coal",
                state.getValue(LIT) ? (o.left + 19) / 20 : o.coal, BATCH));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(LIT)) return;
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0.06, 0);
        if (r.nextInt(5) == 0) level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0, 0.02, 0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != HighFurnace.COKE_OVEN_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<Entity>) (l, p, s, e) -> e.tick((ServerLevel) l, s);
    }

    public static class Entity extends BlockEntity {
        int coal, coke, left;

        public Entity(BlockPos pos, BlockState state) {
            super(HighFurnace.COKE_OVEN_ENTITY.get(), pos, state);
        }

        void start(ServerLevel level) {
            coal = 0;
            left = BAKE_TICKS;
            level.setBlock(worldPosition, getBlockState().setValue(LIT, true), Block.UPDATE_ALL);
            level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 0.7F);
        }

        void tick(ServerLevel level, BlockState state) {
            if (!state.getValue(LIT)) return;
            if (--left <= 0) {
                coke = YIELD;
                level.setBlock(worldPosition, state.setValue(LIT, false), Block.UPDATE_ALL);
                Smelting.award(level, worldPosition, HighFurnace.COKE_ADVANCEMENT, 12);
            }
            setChanged();
        }

        void finishForTest() {
            left = 1;
        }

        public int coke() {
            return coke;
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            coal = input.getIntOr("coal", 0);
            coke = input.getIntOr("coke", 0);
            left = input.getIntOr("left", 0);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putInt("coal", coal);
            output.putInt("coke", coke);
            output.putInt("left", left);
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            return saveCustomOnly(registries);
        }
    }
}
