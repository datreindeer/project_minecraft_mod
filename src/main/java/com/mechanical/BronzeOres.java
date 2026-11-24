package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.function.Supplier;

import com.mechanical.worldgen.OreIndicators;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Bronze Age phase 1, ores: surface finds over copper and tin veins ({@link OreIndicators}), panning gravel and sand
 * ({@link PanItem}) and crushing ore on the {@link StoneAnvilBlock}. Data: scripts/gen_bronze_ores.py.
 */
public final class BronzeOres {
    public static final DeferredItem<Item> MALACHITE = ITEMS.registerSimpleItem("malachite");
    public static final DeferredItem<Item> NATIVE_COPPER = ITEMS.registerSimpleItem("native_copper");
    public static final DeferredItem<Item> CASSITERITE = ITEMS.registerSimpleItem("cassiterite");
    public static final DeferredItem<Item> CRUSHED_COPPER_ORE = ITEMS.registerSimpleItem("crushed_copper_ore");
    public static final DeferredItem<Item> CRUSHED_TIN_ORE = ITEMS.registerSimpleItem("crushed_tin_ore");
    /** Crushed ore washed in the Pan (the gangue rinsed off): 2 crucible units instead of 1. */
    public static final DeferredItem<Item> COPPER_CONCENTRATE = ITEMS.registerSimpleItem("copper_concentrate");
    public static final DeferredItem<Item> TIN_CONCENTRATE = ITEMS.registerSimpleItem("tin_concentrate");

    public static final DeferredItem<PanItem> PAN = ITEMS.registerItem("pan", p -> new PanItem(p, null), p -> p.durability(PanItem.USES));
    public static final DeferredItem<PanItem> PAN_WITH_GRAVEL = ITEMS.registerItem("pan_with_gravel", p -> new PanItem(p, "gravel"), p -> p.durability(PanItem.USES));
    public static final DeferredItem<PanItem> PAN_WITH_SAND = ITEMS.registerItem("pan_with_sand", p -> new PanItem(p, "sand"), p -> p.durability(PanItem.USES));

    // Surface finds: loose rocks (same block class, sizes 1-2 from worldgen, picked by hand), placed by OreIndicators.
    public static final DeferredBlock<RockBlock> LOOSE_MALACHITE = find("loose_malachite", MapColor.COLOR_GREEN);
    public static final DeferredBlock<RockBlock> LOOSE_NATIVE_COPPER = find("loose_native_copper", MapColor.COLOR_ORANGE);
    public static final DeferredBlock<RockBlock> LOOSE_CASSITERITE = find("loose_cassiterite", MapColor.COLOR_BLACK);

    public static final DeferredBlock<StoneAnvilBlock> STONE_ANVIL = BLOCKS.registerBlock("stone_anvil", StoneAnvilBlock::new, p -> p
            .mapColor(MapColor.STONE).strength(2.0F, 6.0F).sound(SoundType.STONE).noOcclusion());
    public static final DeferredItem<BlockItem> STONE_ANVIL_ITEM = ITEMS.registerSimpleBlockItem("stone_anvil", STONE_ANVIL);
    public static final Supplier<BlockEntityType<StoneAnvilBlockEntity>> STONE_ANVIL_ENTITY = Mechanical.BLOCK_ENTITIES.register("stone_anvil",
            () -> new BlockEntityType<>(StoneAnvilBlockEntity::new, STONE_ANVIL.get(), StoneCrafts.BASALT_ANVIL.get()));

    /** Tools that work as a hammerstone on the Stone Anvil: Flint Pickaxe, Crude Stone Pickaxe, Stone Pickaxe. */
    public static final TagKey<Item> HAMMERSTONES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "hammerstones"));

    static {
        com.mechanical.worldgen.Structures.FEATURE_TYPES.register("ore_indicators", () -> OreIndicators.CODEC);
    }

    private static DeferredBlock<RockBlock> find(String name, MapColor color) {
        return BLOCKS.registerBlock(name, RockBlock::new, p -> p
                .mapColor(color).strength(1.5F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.POPPED));
    }

    /** What one ore item on the Stone Anvil crushes into, or empty if it cannot be crushed. */
    public static ItemStack crushed(ItemStack ore) {
        if (ore.is(Items.RAW_COPPER)) return new ItemStack(CRUSHED_COPPER_ORE.get(), 2);
        if (ore.is(MALACHITE.get())) return new ItemStack(CRUSHED_COPPER_ORE.get());
        if (ore.is(CASSITERITE.get())) return new ItemStack(CRUSHED_TIN_ORE.get());
        if (ore.is(Items.BRICK)) return new ItemStack(Ironworking.GROG.get(), 2); // grog for Fire Clay
        if (ore.is(StoneFamilies.block("quartzite").asItem())) return new ItemStack(HighFurnace.CRUSHED_QUARTZITE.get(), 2); // for Silica Bricks
        ItemStack flux = IronOres.crushed(ore); // Limestone -> Flux
        if (!flux.isEmpty()) return flux;
        ItemStack rubble = StoneCrafts.crushed(ore); // Conglomerate, Shale, Pumice
        if (!rubble.isEmpty()) return rubble;
        ItemStack grit = Gemstones.grit(ore); // Flint Grit, Garnet Grit, Corundum Grit for the Lapidary Wheel
        if (!grit.isEmpty()) return grit;
        return Pigments.ground(ore); // ochre, chalk, calcite, charcoal
    }

    /** What one crushed ore washes into in the Pan, or null if it can't be washed. */
    public static Item washed(ItemStack ore) {
        if (ore.is(CRUSHED_COPPER_ORE.get())) return COPPER_CONCENTRATE.get();
        if (ore.is(CRUSHED_TIN_ORE.get())) return TIN_CONCENTRATE.get();
        return null;
    }

    public static void init() {}

    private BronzeOres() {}
}
