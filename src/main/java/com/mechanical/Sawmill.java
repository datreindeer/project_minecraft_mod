package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jspecify.annotations.Nullable;

/**
 * The Sawmill (docs/flora-and-food-plan.md section 7, phase 5b; steel, end of the Iron Age): a 7-block bed, three
 * <b>Log Flumes</b> ({@link LogFlumeBlock}) on each side of a <b>Saw Bench</b> ({@link SawBenchBlock}) whose steel frame
 * saw goes up and down, worked by a crank from a Water Wheel's shaft. Water let into the flume's upstream end carries the
 * logs laid in it to the saw ({@link SawBenchBlockEntity}); the saw needs {@link #NEED_POWER} power and a Steel Saw
 * Blade (worn by one per log, {@link #BLADE_USES} in all). What a log gives: {@link #products}. Sawdust: short fuel,
 * compost, and pressed with clay into Sawdust Briquettes that a Clay Kiln chars into Charcoal. Data: scripts/gen_waterpower.py.
 */
public final class Sawmill {
    public static final int NEED_POWER = 8;
    public static final int BLADE_USES = 512;
    /** Flumes on each side of the saw. */
    public static final int FLUMES = 3;

    public static final DeferredBlock<LogFlumeBlock> LOG_FLUME = BLOCKS.registerBlock("log_flume", LogFlumeBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion().forceSolidOn().ignitedByLava());
    public static final DeferredItem<BlockItem> LOG_FLUME_ITEM = ITEMS.registerSimpleBlockItem("log_flume", LOG_FLUME);
    public static final DeferredBlock<SawBenchBlock> SAW_BENCH = BLOCKS.registerBlock("saw_bench", SawBenchBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD).noOcclusion().forceSolidOn());
    public static final DeferredItem<BlockItem> SAW_BENCH_ITEM = ITEMS.registerSimpleBlockItem("saw_bench", SAW_BENCH);
    public static final Supplier<BlockEntityType<SawBenchBlockEntity>> SAW_BENCH_ENTITY = Mechanical.BLOCK_ENTITIES.register("saw_bench",
            () -> new BlockEntityType<>(SawBenchBlockEntity::new, SAW_BENCH.get()));

    public static final DeferredItem<Item> SAWDUST = ITEMS.registerSimpleItem("sawdust");
    /** Sawdust pressed with clay: charred in a Clay Kiln into Charcoal. */
    public static final DeferredItem<Item> SAWDUST_BRIQUETTE = ITEMS.registerSimpleItem("sawdust_briquette");

    public static final Identifier SAWN_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "iron_age/sawmill");

    public static void init() {}

    /** The wood of a log the saw takes and whether it still has its bark, or null. */
    public record Wood(String wood, boolean bark) {}

    /** Logs and wood blocks of the mod's woods (bark on or stripped), and Rough Logs (hewn: no bark). */
    public static @Nullable Wood woodOf(ItemStack stack) {
        if (stack.getItem() instanceof RoughLogItem rough) return new Wood(rough.wood(), false);
        if (!(stack.getItem() instanceof BlockItem block) || !block.getBlock().defaultBlockState().is(BlockTags.LOGS)) return null;
        String path = BuiltInRegistries.BLOCK.getKey(block.getBlock()).getPath();
        boolean stripped = path.startsWith("stripped_");
        if (stripped) path = path.substring("stripped_".length());
        String wood = path.endsWith("_log") ? path.substring(0, path.length() - 4) : path.endsWith("_wood") ? path.substring(0, path.length() - 5) : null;
        return wood != null && Mechanical.WOODS.contains(wood) ? new Wood(wood, !stripped) : null;
    }

    /**
     * What the saw makes of one log (plan table): with bark 2 Rough Logs, 2 Firewood, 2 Bark and 2 Sawdust; without bark
     * (stripped, or a Rough Log) 6 Planks, 2 Sticks and 1 Sawdust. Hand work gets 2 Firewood (axe) or 2 Planks.
     */
    public static List<ItemStack> products(Wood w) {
        if (w.bark()) return List.of(new ItemStack(Mechanical.ROUGH_LOGS.get(w.wood()).get(), 2), new ItemStack(Mechanical.FIREWOOD.get(w.wood()).get(), 2),
                new ItemStack(Mechanical.BARKS.get(w.wood()).get(), 2), new ItemStack(SAWDUST.get(), 2));
        return List.of(new ItemStack(planks(w.wood()), 6), new ItemStack(Items.STICK, 2), new ItemStack(SAWDUST.get(), 1));
    }

    /** Vanilla's planks for a vanilla wood, else the mod's (Trees). */
    public static Item planks(String wood) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace(wood + "_planks"))
                .orElseGet(() -> BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MODID, wood + "_planks")));
    }

    /** The stripped log of a wood, as an item (what a Rough Log or a sawn log looks like lying in the flume). */
    public static Item strippedLog(String wood) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.withDefaultNamespace("stripped_" + wood + "_log"))
                .orElseGet(() -> BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MODID, "stripped_" + wood + "_log")));
    }

    private Sawmill() {}
}
