package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Water power (docs/flora-and-food-plan.md section 7, phase 5a; Iron Age). A <b>Water Wheel</b> (5 blocks across, its
 * hub the only real block, {@link WaterWheelBlock}) turns when water moves past its paddles ({@link WaterWheelBlockEntity#measure}):
 * every paddle cell (20 round the hub) with water flowing round the wheel counts 1, falling water (overshot) 2, up to {@link #MAX_POWER}.
 * Every second each axle line ({@link #line}) is worked out from one of its wheels: straight through <b>Wooden Shafts</b>
 * along the line (lying or standing), through other wheels on the same axle, and through a <b>Gearbox</b> on into shafts
 * at a right angle, up and down too (the bevel gears turn the other way round). All the wheels on a line add their power
 * (opposed wheels take from each other; one jammed wheel stops the axle), up to {@link #MAX_POWER}. A machine at the end
 * of a line (the Saw Bench, {@link Sawmill}) takes the power. Parts lose their turn {@link #LAPSE_TICKS} after no wheel
 * feeds them. The wheel's solid body: {@link WaterWheelHitbox}. Data: scripts/gen_waterpower.py.
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

    /** The wheel's invisible solid body: two per wheel (see {@link WaterWheelHitbox}); its box comes from its shape, not this size. */
    public static final DeferredHolder<EntityType<?>, EntityType<WaterWheelHitbox>> WHEEL_HITBOX = Mechanical.ENTITY_TYPES.registerEntityType("water_wheel_body",
            WaterWheelHitbox::new, MobCategory.MISC, b -> b.noLootTable().sized(1.0F, 1.0F).clientTrackingRange(8).updateInterval(200).noSummon());

    public static final Identifier WHEEL_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "iron_age/water_wheel");
    public static final Identifier FULL_POWER_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "iron_age/full_power");
    public static final Identifier TWIN_WHEELS_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "iron_age/twin_wheels");
    public static final Identifier POWER_CLIMBS_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "iron_age/power_climbs");

    public static void init() {}

    static void tellPower(net.minecraft.world.entity.player.Player player, int power) {
        player.sendOverlayMessage(power > 0 ? net.minecraft.network.chat.Component.translatable("message.mechanical.water_power.turning", power, MAX_POWER)
                : net.minecraft.network.chat.Component.translatable("message.mechanical.water_power.still"));
    }

    /** A part turned by a line, and which way it turns about its own +axis compared with the wheel that walked the line. */
    record Part(RotorBlockEntity rotor, int sign) {}

    /**
     * Everything one wheel's axle turns: the wheel itself, then down its axle both ways through shafts, other wheels on
     * the same axle, gearboxes (which branch to all six sides) and into a machine.
     */
    static List<Part> line(ServerLevel level, WaterWheelBlockEntity wheel) {
        List<Part> parts = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        parts.add(new Part(wheel, 1));
        seen.add(wheel.getBlockPos());
        for (Direction.AxisDirection sign : Direction.AxisDirection.values())
            walk(level, wheel.getBlockPos(), Direction.fromAxisAndDirection(wheel.axis(), sign), 1, parts, seen);
        return parts;
    }

    /**
     * Which way a gearbox's gear on this axis turns compared with the gearbox's own turn (kept about X): the gears sit on
     * the +X, +Z and +Y sides, and a bevel gear meshing at a right angle turns the other way (client/WaterPowerRenderers).
     */
    static int gear(Direction.Axis axis) {
        return axis == Direction.Axis.X ? 1 : -1;
    }

    /** Down one line from {@code from} towards {@code dir}: shafts lying along it, wheels on it, a gearbox (which branches), a machine. */
    private static void walk(ServerLevel level, BlockPos from, Direction dir, int sign, List<Part> parts, Set<BlockPos> seen) {
        BlockPos p = from.relative(dir);
        for (int i = 0; i < MAX_LINE && seen.size() < 4 * MAX_LINE && seen.add(p); i++, p = p.relative(dir)) {
            BlockState s = level.getBlockState(p);
            BlockEntity be = level.getBlockEntity(p);
            if (s.is(WOODEN_SHAFT.get()) && s.getValue(ShaftBlock.AXIS) == dir.getAxis()
                    || s.is(WATER_WHEEL.get()) && s.getValue(WaterWheelBlock.AXIS) == dir.getAxis()) { // the axle runs on through a hub
                if (be instanceof RotorBlockEntity r) parts.add(new Part(r, sign));
                continue;
            }
            if (s.is(GEARBOX.get())) {
                int g = sign * gear(dir.getAxis());
                if (be instanceof RotorBlockEntity r) parts.add(new Part(r, g));
                for (Direction out : Direction.values())
                    if (out != dir.getOpposite()) walk(level, p, out, g * gear(out.getAxis()), parts, seen);
                return;
            }
            if (s.getBlock() instanceof SawBenchBlock && s.getValue(SawBenchBlock.FACING).getAxis() != dir.getAxis() && dir.getAxis().isHorizontal()
                    && be instanceof RotorBlockEntity r) parts.add(new Part(r, sign));
            return;
        }
    }

    private WaterPower() {}
}
