package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Water power (docs/flora-and-food-plan.md section 7, phase 5a; Iron Age). A <b>Water Wheel</b> (5 blocks across, its
 * hub the only real block, {@link WaterWheelBlock}) turns when water moves past its paddles ({@link WaterWheelBlockEntity#measure}):
 * every paddle cell (20 round the hub) with water flowing round the wheel counts 1, falling water (overshot) 2, up to {@link #MAX_POWER}.
 * Every second the wheel hands its turn down its axle in both directions ({@link #spread}): straight through <b>Wooden
 * Shafts</b> lying along the line, and through a <b>Gearbox</b> on into shafts at a right angle (the bevel gears turn
 * the other way round). A machine at the end of a line (the Saw Bench, {@link Sawmill}) takes the power. Parts lose
 * their turn {@link #LAPSE_TICKS} after the wheel stops feeding them. Data: scripts/gen_waterpower.py.
 */
public final class WaterPower {
    public static final int MAX_POWER = 16;
    /** How fast the wheel turns per point of power: 16 power is one turn in 4.5 s. */
    public static final float DEG_PER_POWER = 0.25F;
    /** Longest line of shafts a wheel turns. */
    public static final int MAX_LINE = 24;
    public static final int LAPSE_TICKS = 50;

    public static final DeferredBlock<WaterWheelBlock> WATER_WHEEL = BLOCKS.registerBlock("water_wheel", WaterWheelBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion().forceSolidOn().ignitedByLava());
    public static final DeferredItem<BlockItem> WATER_WHEEL_ITEM = ITEMS.registerSimpleBlockItem("water_wheel", WATER_WHEEL);
    public static final DeferredBlock<ShaftBlock> WOODEN_SHAFT = BLOCKS.registerBlock("wooden_shaft", ShaftBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion().forceSolidOn().ignitedByLava());
    public static final DeferredItem<BlockItem> WOODEN_SHAFT_ITEM = ITEMS.registerSimpleBlockItem("wooden_shaft", WOODEN_SHAFT);
    public static final DeferredBlock<GearboxBlock> GEARBOX = BLOCKS.registerBlock("gearbox", GearboxBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion().forceSolidOn().ignitedByLava());
    public static final DeferredItem<BlockItem> GEARBOX_ITEM = ITEMS.registerSimpleBlockItem("gearbox", GEARBOX);

    public static final Supplier<BlockEntityType<WaterWheelBlockEntity>> WATER_WHEEL_ENTITY = Mechanical.BLOCK_ENTITIES.register("water_wheel",
            () -> new BlockEntityType<>(WaterWheelBlockEntity::new, WATER_WHEEL.get()));
    public static final Supplier<BlockEntityType<RotorBlockEntity>> ROTOR_ENTITY = Mechanical.BLOCK_ENTITIES.register("rotor",
            () -> new BlockEntityType<>(RotorBlockEntity::new, WOODEN_SHAFT.get(), GEARBOX.get()));

    public static final Identifier WHEEL_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "iron_age/water_wheel");
    public static final Identifier FULL_POWER_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "iron_age/full_power");

    public static void init() {}

    static void tellPower(net.minecraft.world.entity.player.Player player, int power) {
        player.sendOverlayMessage(power > 0 ? net.minecraft.network.chat.Component.translatable("message.mechanical.water_power.turning", power, MAX_POWER)
                : net.minecraft.network.chat.Component.translatable("message.mechanical.water_power.still"));
    }

    /** Hands a wheel's turn down its axle both ways. {@code speed}/{@code base} are about the wheel's +axis. */
    static void spread(ServerLevel level, BlockPos wheel, Direction.Axis axis, float speed, float base, long t0, int power) {
        Set<BlockPos> seen = new HashSet<>();
        seen.add(wheel);
        for (Direction.AxisDirection sign : Direction.AxisDirection.values())
            walk(level, wheel, Direction.fromAxisAndDirection(axis, sign), speed, base, t0, power, seen);
    }

    /** Down one line from {@code from} towards {@code dir}: shafts lying along it, a gearbox (which branches), a machine. */
    private static void walk(ServerLevel level, BlockPos from, Direction dir, float speed, float base, long t0, int power, Set<BlockPos> seen) {
        BlockPos p = from.relative(dir);
        for (int i = 0; i < MAX_LINE && seen.size() < 4 * MAX_LINE && seen.add(p); i++, p = p.relative(dir)) {
            BlockState s = level.getBlockState(p);
            if (s.is(WOODEN_SHAFT.get()) && s.getValue(BlockStateProperties.HORIZONTAL_AXIS) == dir.getAxis()) {
                if (level.getBlockEntity(p) instanceof RotorBlockEntity r) r.feed(speed, base, t0, power);
                continue;
            }
            if (s.is(GEARBOX.get())) {
                // the gearbox keeps its turn about X; a bevel gear on the other axis turns the other way (GearboxBlock)
                float sx = dir.getAxis() == Direction.Axis.X ? 1 : -1;
                if (level.getBlockEntity(p) instanceof RotorBlockEntity r) r.feed(speed * sx, base * sx, t0, power);
                for (Direction out : Direction.Plane.HORIZONTAL) {
                    if (out == dir.getOpposite()) continue;
                    float m = out.getAxis() == dir.getAxis() ? 1 : -1;
                    walk(level, p, out, speed * m, base * m, t0, power, seen);
                }
                return;
            }
            if (s.getBlock() instanceof SawBenchBlock && s.getValue(SawBenchBlock.FACING).getAxis() != dir.getAxis()
                    && level.getBlockEntity(p) instanceof RotorBlockEntity r) r.feed(speed, base, t0, power);
            return;
        }
    }

    private WaterPower() {}
}
