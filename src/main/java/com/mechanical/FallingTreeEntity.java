package com.mechanical;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.SynchedEntityData;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/**
 * A felled tree tipping over. Sits at the bottom-centre of the cut log; blocks are stored as offsets from it.
 * After FALL_TICKS each block drops its normal loot where it lands.
 * shortcut: not saved (noSave), so a tree mid-fall when the server stops loses its drops; persist offsets if that ever matters.
 */
public class FallingTreeEntity extends Entity implements IEntityWithComplexSpawn {
    public static final int FALL_TICKS = 25;

    private final List<BlockPos> offsets = new ArrayList<>();
    private final List<BlockState> states = new ArrayList<>();
    private Direction direction = Direction.NORTH;
    // server only: who felled it, with what (for loot conditions like fortune)
    private Player feller;
    private ItemStack tool = ItemStack.EMPTY;

    public FallingTreeEntity(EntityType<? extends FallingTreeEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public static FallingTreeEntity create(Level level, BlockPos base, Direction direction, List<BlockPos> blocks, Player feller, ItemStack tool) {
        FallingTreeEntity tree = new FallingTreeEntity(Mechanical.FALLING_TREE.get(), level);
        tree.setPos(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        tree.direction = direction;
        tree.feller = feller;
        tree.tool = tool.copy();
        for (BlockPos pos : blocks) {
            tree.offsets.add(pos.subtract(base));
            tree.states.add(level.getBlockState(pos));
        }
        return tree;
    }

    public List<BlockPos> offsets() {
        return offsets;
    }

    public List<BlockState> states() {
        return states;
    }

    public Direction direction() {
        return direction;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level && tickCount >= FALL_TICKS) land(level);
    }

    private void land(ServerLevel level) {
        BlockPos base = blockPosition();
        for (int i = 0; i < offsets.size(); i++) {
            BlockPos o = offsets.get(i);
            // rotating 90 degrees away from the feller turns height into distance along the fall direction
            BlockPos column = base.offset(o.getX() + direction.getStepX() * o.getY(), 0, o.getZ() + direction.getStepZ() * o.getY());
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            Block.dropResources(states.get(i), level, ground, null, feller, tool);
        }
        level.playSound(null, base.relative(direction, 3), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 2.0F, 0.5F);
        discard();
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(direction.get3DDataValue());
        buf.writeVarInt(offsets.size());
        for (int i = 0; i < offsets.size(); i++) {
            buf.writeBlockPos(offsets.get(i));
            buf.writeVarInt(Block.getId(states.get(i)));
        }
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        direction = Direction.from3DDataValue(buf.readVarInt());
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            offsets.add(buf.readBlockPos());
            states.add(Block.stateById(buf.readVarInt()));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {}
}
