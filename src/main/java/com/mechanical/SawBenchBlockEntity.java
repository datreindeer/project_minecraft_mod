package com.mechanical;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Saw Bench's work (see {@link Sawmill}): the logs in its in-side flume, the blade, and the crank's turn (a
 * {@link RotorBlockEntity} fed by a Water Wheel's shaft). Logs lie at {@code s} blocks from the blade along the line
 * (negative: upstream). While the mill is whole and the water runs towards the saw ({@link #problem}) they float on at
 * {@link #FEED} a tick and queue a block apart; the first one waits at the blade until the saw has power and a blade,
 * then is fed through it over {@link #CUT_TICKS}; what it gives ({@link Sawmill#products}) drops into the out-side flume,
 * whose water carries it away. The same tick runs on the client, so the logs move smoothly between syncs.
 */
public class SawBenchBlockEntity extends RotorBlockEntity {
    public static final float FEED = 0.05F;
    public static final int CUT_TICKS = 80;
    /** Half a log's length, blocks: a log touches the blade at s = -HALF and is through at +HALF. */
    public static final float HALF = 0.45F;
    public static final float SPACING = 1.0F;
    /** The crank turns this many times faster than the wheel (a gear inside the bench). */
    public static final float CRANK_RATIO = 9;

    public enum Problem {
        NONE, FLUMES_IN, FLUMES_OUT, DRY, STILL, WRONG_WAY, NO_BLADE, LOW_POWER;

        /** The logs can float to the saw (only the saw itself is not ready). */
        public boolean flows() {
            return this == NONE || this == NO_BLADE || this == LOW_POWER;
        }
    }

    public static final class Log {
        public final ItemStack item;
        public float s;

        Log(ItemStack item, float s) {
            this.item = item;
            this.s = s;
        }
    }

    private final List<Log> logs = new ArrayList<>();
    private int blade;
    /** Client: the first log is being sawn this tick (sash and dust). */
    public boolean cutting;

    public SawBenchBlockEntity(BlockPos pos, BlockState state) {
        super(Sawmill.SAW_BENCH_ENTITY.get(), pos, state);
    }

    public List<Log> logs() {
        return logs;
    }

    public int blade() {
        return blade;
    }

    public Direction front() {
        return getBlockState().getValue(SawBenchBlock.FACING);
    }

    /** What keeps the mill from working, in the order a builder meets it, or NONE. */
    public Problem problem() {
        Direction front = front();
        for (Direction side : new Direction[] {front, front.getOpposite()})
            for (int i = 1; i <= Sawmill.FLUMES; i++) {
                BlockState s = level.getBlockState(worldPosition.relative(side, i));
                if (!s.is(Sawmill.LOG_FLUME.get()) || s.getValue(LogFlumeBlock.AXIS) != front.getAxis()) return side == front ? Problem.FLUMES_IN : Problem.FLUMES_OUT;
            }
        LogFlumeBlock.Water water = getBlockState().getValue(LogFlumeBlock.WATER);
        if (water == LogFlumeBlock.Water.DRY) return Problem.DRY;
        if (water == LogFlumeBlock.Water.STILL) return Problem.STILL;
        if (!water.runs(front.getOpposite())) return Problem.WRONG_WAY;
        if (blade <= 0) return Problem.NO_BLADE;
        if (power < Sawmill.NEED_POWER) return Problem.LOW_POWER;
        return Problem.NONE;
    }

    /** Both sides, every tick: float the logs on, feed the first through the blade. */
    void tick(Level level) {
        if (level.getGameTime() % 20 == 7 && !level.isClientSide()) lapse();
        cutting = false;
        if (logs.isEmpty()) return;
        Problem problem = problem();
        if (!problem.flows()) return;
        Log head = logs.get(0);
        if (head.s < -HALF) head.s = Math.min(-HALF, head.s + FEED);
        else if (problem == Problem.NONE) {
            cutting = true;
            head.s += 2 * HALF / CUT_TICKS;
            if (head.s >= HALF) {
                head.s = HALF;
                if (level instanceof ServerLevel server) finishCut(server, head);
            }
            if (level.isClientSide()) dust(level, head);
            else if (level.getGameTime() % 10 == 0)
                level.playSound(null, worldPosition, SoundEvents.AXE_SCRAPE.value(), SoundSource.BLOCKS, 0.7F, 0.55F + level.getRandom().nextFloat() * 0.15F);
        }
        for (int i = 1; i < logs.size(); i++) logs.get(i).s = Math.min(logs.get(i).s + FEED, logs.get(i - 1).s - SPACING);
    }

    private void finishCut(ServerLevel level, Log log) {
        Sawmill.Wood wood = Sawmill.woodOf(log.item);
        logs.remove(log);
        if (wood != null) {
            Direction out = front().getOpposite();
            double x = worldPosition.getX() + 0.5 + out.getStepX() * 0.9, y = worldPosition.getY() + 0.35, z = worldPosition.getZ() + 0.5 + out.getStepZ() * 0.9;
            for (ItemStack product : Sawmill.products(wood)) {
                ItemEntity item = new ItemEntity(level, x, y, z, product, out.getStepX() * 0.08, 0.02, out.getStepZ() * 0.08);
                item.setDefaultPickUpDelay();
                level.addFreshEntity(item);
            }
        }
        level.playSound(null, worldPosition, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.9F, 0.7F);
        if (--blade <= 0) {
            blade = 0;
            level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        Smelting.award(level, worldPosition, Sawmill.SAWN_ADVANCEMENT, 16);
        sync();
    }

    /** Sawdust spraying from the kerf and falling under the bench. */
    private void dust(Level level, Log log) {
        RandomSource r = level.getRandom();
        Direction front = front();
        double x = worldPosition.getX() + 0.5 + front.getStepX() * 0.1, y = worldPosition.getY() + 0.62, z = worldPosition.getZ() + 0.5 + front.getStepZ() * 0.1;
        if (r.nextInt(2) == 0) level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, Sawmill.SAWDUST.get()), x, y, z,
                (r.nextDouble() - 0.5) * 0.15, 0.08 + r.nextDouble() * 0.06, (r.nextDouble() - 0.5) * 0.15);
        Sawmill.Wood wood = Sawmill.woodOf(log.item);
        if (wood != null && r.nextInt(3) == 0) level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST,
                Block.byItem(Sawmill.planks(wood.wood())).defaultBlockState()), x + (r.nextDouble() - 0.5) * 0.3, y - 0.2, z + (r.nextDouble() - 0.5) * 0.3, 0, 0, 0);
    }

    /** Lays one log at {@code s} (-1, -2, -3: the in-side flume) if there is room for it there. */
    void lay(ServerLevel level, Player player, ItemStack stack, float s) {
        if (logs.size() >= Sawmill.FLUMES) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.saw.full"));
            return;
        }
        for (Log log : logs) if (Math.abs(log.s - s) < SPACING) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.saw.occupied"));
            return;
        }
        logs.add(new Log(stack.consumeAndReturn(1, player), s));
        logs.sort((a, b) -> Float.compare(b.s, a.s));
        level.playSound(null, worldPosition.relative(front(), (int) -s), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8F, 0.8F);
        level.playSound(null, worldPosition.relative(front(), (int) -s), SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.4F, 1.2F);
        Problem p = problem();
        if (p != Problem.NONE) tell(player, p);
        sync();
    }

    /** A new blade; the old one (if it has uses left) comes back with its uses on it. */
    void fitBlade(ServerLevel level, Player player, ItemStack stack) {
        if (blade >= Sawmill.BLADE_USES) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.saw.blade_new"));
            return;
        }
        ItemStack fresh = stack.consumeAndReturn(1, player);
        if (blade > 0) player.getInventory().placeItemBackInInventory(bladeItem(blade), net.minecraft.util.Prediction.SERVER_ONLY);
        blade = usesOf(fresh);
        level.playSound(null, worldPosition, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.7F, 1.3F);
        player.sendOverlayMessage(Component.translatable("message.mechanical.saw.blade_fitted", blade, Sawmill.BLADE_USES));
        sync();
    }

    static ItemStack bladeItem(int uses) {
        ItemStack s = new ItemStack(Smithing.STEEL_SAW_BLADE.get());
        if (uses < Sawmill.BLADE_USES) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("saw_uses", uses);
            s.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return s;
    }

    static int usesOf(ItemStack blade) {
        CustomData data = blade.get(DataComponents.CUSTOM_DATA);
        return data == null ? Sawmill.BLADE_USES : data.copyTag().getIntOr("saw_uses", Sawmill.BLADE_USES);
    }

    void tellStatus(Player player) {
        Problem p = problem();
        if (p == Problem.NONE) player.sendOverlayMessage(Component.translatable("message.mechanical.saw.ready", power, Sawmill.NEED_POWER, blade, Sawmill.BLADE_USES));
        else tell(player, p);
    }

    private void tell(Player player, Problem p) {
        player.sendOverlayMessage(switch (p) {
            case LOW_POWER -> Component.translatable("message.mechanical.saw.low_power", power, Sawmill.NEED_POWER);
            case FLUMES_IN, FLUMES_OUT -> Component.translatable("message.mechanical.saw." + p.name().toLowerCase(java.util.Locale.ROOT), Sawmill.FLUMES);
            default -> Component.translatable("message.mechanical.saw." + p.name().toLowerCase(java.util.Locale.ROOT));
        });
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;
        for (Log log : logs) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), log.item);
        if (blade > 0) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), bladeItem(blade));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("blade", blade);
        output.store("logs", ItemStack.CODEC.listOf(), logs.stream().map(l -> l.item).toList());
        output.store("log_s", Codec.FLOAT.listOf(), logs.stream().map(l -> l.s).toList());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        blade = input.getIntOr("blade", 0);
        List<ItemStack> items = input.read("logs", ItemStack.CODEC.listOf()).orElse(List.of());
        List<Float> s = input.read("log_s", Codec.FLOAT.listOf()).orElse(List.of());
        logs.clear();
        for (int i = 0; i < Math.min(items.size(), s.size()); i++) logs.add(new Log(items.get(i), s.get(i)));
    }

    /** For tests: lays a log without a player. */
    void layForTest(ItemStack stack, float s) {
        logs.add(new Log(stack.copyWithCount(1), s));
        logs.sort((a, b) -> Float.compare(b.s, a.s));
    }

    void setBladeForTest(int uses) {
        blade = uses;
    }
}
