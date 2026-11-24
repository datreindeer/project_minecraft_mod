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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Finery Hearth (docs/steel-furnace-plan.md section 5; the Walloon forge of Uppland): a low open hearth of fire brick
 * blown by a Great Bellows. Charcoal (up to {@link #FUEL_MAX}) and Pig Iron Ingots (up to {@link #CAPACITY}) go in with a
 * right-click; lit with a Fire Drill or Flint and Steel it heats toward 800° + 50° per point of blast (a Great Bellows
 * with its nozzle at the hearth); from {@link #HEAT}° each pig iron is fined into a Wrought Iron Ingot in
 * {@link #FINE_TICKS} ticks, burning a Charcoal. An empty hand takes the wrought iron out.
 */
public class FineryHearthBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final int FUEL_MAX = 16, CAPACITY = 4, HEAT = 1300, FINE_TICKS = 400;
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 10, 16);

    public FineryHearthBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Entity h)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        boolean fuel = stack.is(Items.CHARCOAL), pig = stack.is(HighFurnace.PIG_IRON_INGOT.get()), fire = stack.getItem() instanceof FireDrillItem || stack.is(Items.FLINT_AND_STEEL);
        if (!fuel && !pig && !fire) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (fuel && h.fuel < FUEL_MAX) {
            int n = Math.min(stack.getCount(), FUEL_MAX - h.fuel);
            h.fuel += n;
            if (!player.getAbilities().instabuild) stack.shrink(n);
        } else if (pig && h.pig < CAPACITY) {
            h.pig++;
            if (!player.getAbilities().instabuild) stack.shrink(1);
        } else if (fire && h.fuel > 0 && !state.getValue(LIT)) {
            server.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        } else return InteractionResult.FAIL;
        h.sync();
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel) || !(level.getBlockEntity(pos) instanceof Entity h)) return InteractionResult.SUCCESS;
        if (h.wrought > 0) {
            ItemStack out = new ItemStack(Ironworking.WROUGHT_IRON_INGOT.get(), h.wrought);
            if (!player.getInventory().add(out)) Block.popResource(level, pos.above(), out);
            h.wrought = 0;
            h.sync();
        } else player.sendOverlayMessage(Component.translatable("message.mechanical.finery_hearth", Math.round(h.temp), h.pig, h.fuel, h.blast));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (!state.getValue(LIT)) return;
        level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.3 + r.nextDouble() * 0.4, pos.getY() + 0.7, pos.getZ() + 0.3 + r.nextDouble() * 0.4, 0, 0.03, 0);
        if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, (r.nextDouble() - 0.5) * 0.2, 0.2, (r.nextDouble() - 0.5) * 0.2);
        if (r.nextInt(2) == 0) level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0, 0.05, 0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != HighFurnace.FINERY_HEARTH_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<Entity>) (l, p, s, e) -> e.tick((ServerLevel) l, s);
    }

    public static class Entity extends BlockEntity {
        int fuel, pig, wrought, blast;
        float temp = 20, progress, burn;

        public Entity(BlockPos pos, BlockState state) {
            super(HighFurnace.FINERY_HEARTH_ENTITY.get(), pos, state);
        }

        public int wrought() {
            return wrought;
        }

        public float temperature() {
            return temp;
        }

        /** The blast of a Great Bellows beside it with its nozzle at the hearth. */
        int blast(ServerLevel level) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockState s = level.getBlockState(worldPosition.relative(d));
                if (s.is(HighFurnace.GREAT_BELLOWS.get()) && s.getValue(GreatBellowsBlock.FACING) == d.getOpposite()
                        && level.getBlockEntity(worldPosition.relative(d)) instanceof GreatBellowsBlockEntity b) return Math.min(16, b.power());
            }
            return 0;
        }

        void tick(ServerLevel level, BlockState state) {
            boolean lit = state.getValue(LIT);
            blast = blast(level);
            if (lit) {
                burn -= 1 + blast / 8F;
                if (burn <= 0) {
                    if (fuel > 0) {
                        fuel--;
                        burn += 1200;
                    } else {
                        level.setBlock(worldPosition, state.setValue(LIT, false), Block.UPDATE_ALL);
                        lit = false;
                    }
                }
            }
            float target = lit ? Math.min(1600, 800 + 50 * blast) : 20;
            temp += Math.clamp(target - temp, -1F, 0.5F) * 1; // 10° a second up, 20° a second down
            if (temp < target) temp = Math.min(target, temp + 0.5F);
            if (lit && pig > 0 && temp >= HEAT) {
                progress += KilnBlockEntity.speed(temp, HEAT);
                if (progress >= FINE_TICKS) {
                    progress = 0;
                    pig--;
                    wrought++;
                    if (fuel > 0) fuel--; // the charcoal the fining eats
                    level.playSound(null, worldPosition, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.2F);
                    Smelting.award(level, worldPosition, HighFurnace.WALLOON_ADVANCEMENT, 12);
                    sync();
                }
            }
            setChanged();
        }

        void sync() {
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }

        void setForTest(float t) {
            temp = t;
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            fuel = input.getIntOr("fuel", 0);
            pig = input.getIntOr("pig", 0);
            wrought = input.getIntOr("wrought", 0);
            temp = input.getFloatOr("temp", 20);
            progress = input.getFloatOr("progress", 0);
            burn = input.getFloatOr("burn", 0);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putInt("fuel", fuel);
            output.putInt("pig", pig);
            output.putInt("wrought", wrought);
            output.putFloat("temp", temp);
            output.putFloat("progress", progress);
            output.putFloat("burn", burn);
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
