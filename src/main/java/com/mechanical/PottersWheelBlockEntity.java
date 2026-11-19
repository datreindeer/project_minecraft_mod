package com.mechanical;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * What sits on a Potter's Wheel: the chosen {@link Shape}, how many Clay Balls are on it and how far the throwing has
 * come. Every use with an empty hand (holding right-click repeats it) spins the wheel and adds one step; after
 * {@link #THROW_STEPS} the greenware is finished and waits on the wheel to be taken off. Drawn by client/PottersWheelRenderer.
 */
public class PottersWheelBlockEntity extends BlockEntity {
    /** Steps to throw a piece: holding right-click repeats every 4 ticks, so about 5 s. */
    public static final int THROW_STEPS = 24;
    /** How long the wheel keeps turning after the last step. */
    public static final int SPIN_TICKS = 10;

    public enum Shape {
        STORAGE_JAR(4, Pottery.UNFIRED_STORAGE_JAR),
        WATER_JUG(3, Pottery.UNFIRED_WATER_JUG);

        public final int clay;
        public final Supplier<? extends Item> greenware;

        Shape(int clay, Supplier<? extends Item> greenware) {
            this.clay = clay;
            this.greenware = greenware;
        }

        Shape next() {
            return values()[(ordinal() + 1) % values().length];
        }

        Component title() {
            return Component.translatable(greenware.get().getDescriptionId());
        }
    }

    private Shape shape = Shape.STORAGE_JAR;
    private int clay, steps;
    private long spinUntil;
    /** Client only: where the wheel stopped, so it doesn't jump back when it stops spinning. */
    public float lastAngle;

    public PottersWheelBlockEntity(BlockPos pos, BlockState state) {
        super(Pottery.POTTERS_WHEEL_ENTITY.get(), pos, state);
    }

    public Shape shape() {
        return shape;
    }

    public int clay() {
        return clay;
    }

    public int steps() {
        return steps;
    }

    public boolean done() {
        return steps >= THROW_STEPS;
    }

    public boolean spinning(long now) {
        return now < spinUntil;
    }

    /** One more Clay Ball, if the shape still needs it and throwing hasn't started. */
    public boolean addClay(ServerLevel level) {
        if (steps > 0 || clay >= shape.clay) return false;
        clay++;
        level.playSound(null, worldPosition, SoundEvents.MUD_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
        sync();
        return true;
    }

    /** Sneak + empty hand before throwing: the next shape; clay beyond what it needs goes back to the player. */
    public void cycle(ServerLevel level, Player player) {
        shape = shape.next();
        if (clay > shape.clay) {
            player.getInventory().placeItemBackInInventory(new ItemStack(Items.CLAY_BALL, clay - shape.clay), net.minecraft.util.Prediction.SERVER_ONLY);
            clay = shape.clay;
        }
        level.playSound(null, worldPosition, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.6F, 1.4F);
        player.sendOverlayMessage(Component.translatable("message.mechanical.wheel.shape", shape.title(), shape.clay));
        sync();
    }

    /** One step of throwing: the wheel spins, wet clay flies, and the last step finishes the piece. */
    public void throwStep(ServerLevel level) {
        steps++;
        spinUntil = level.getGameTime() + SPIN_TICKS;
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.85, z = worldPosition.getZ() + 0.5;
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, Items.CLAY_BALL), x, y, z, 3, 0.15, 0.08, 0.15, 0.05);
        if (steps % 3 == 1) level.playSound(null, worldPosition, SoundEvents.MUD_STEP, SoundSource.BLOCKS, 0.7F, 0.8F + level.getRandom().nextFloat() * 0.3F);
        if (done()) {
            level.playSound(null, worldPosition, SoundEvents.MUD_BRICKS_PLACE, SoundSource.BLOCKS, 0.9F, 1.1F);
            level.sendParticles(ParticleTypes.SPLASH, x, y + 0.3, z, 8, 0.2, 0.1, 0.2, 0.0);
        }
        sync();
    }

    /** Takes the finished greenware off the wheel. */
    public ItemStack take() {
        ItemStack out = new ItemStack(shape.greenware.get());
        clay = steps = 0;
        sync();
        return out;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** Broken: a finished piece drops as it is, unfinished clay as Clay Balls. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null || clay == 0) return;
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), done() ? new ItemStack(shape.greenware.get()) : new ItemStack(Items.CLAY_BALL, clay));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("shape", shape.name());
        output.putInt("clay", clay);
        output.putInt("steps", steps);
        output.putLong("spin_until", spinUntil);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        try {
            shape = Shape.valueOf(input.getStringOr("shape", Shape.STORAGE_JAR.name()));
        } catch (IllegalArgumentException e) {
            shape = Shape.STORAGE_JAR;
        }
        clay = input.getIntOr("clay", 0);
        steps = input.getIntOr("steps", 0);
        spinUntil = input.getLongOr("spin_until", 0);
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
