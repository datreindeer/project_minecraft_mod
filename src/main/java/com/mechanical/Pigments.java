package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Pigments and cave paintings: ochre and chalk finds (loose rocks placed by worldgen), ground into pigments on the Stone
 * Anvil ({@link #ground}) or with a Flint Pickaxe in the grid, mixed into {@link PaintItem paint}, painted with the
 * {@link PaintbrushItem} as {@link CavePaintingBlock}. Data and worldgen: scripts/gen_pigments.py.
 */
public final class Pigments {
    public static final DeferredItem<Item> RED_OCHRE = ITEMS.registerSimpleItem("red_ochre");
    public static final DeferredItem<Item> YELLOW_OCHRE = ITEMS.registerSimpleItem("yellow_ochre");
    public static final DeferredItem<Item> CHALK = ITEMS.registerSimpleItem("chalk");
    public static final DeferredItem<Item> BLACK_PIGMENT = ITEMS.registerSimpleItem("black_pigment");
    public static final DeferredItem<Item> RED_PIGMENT = ITEMS.registerSimpleItem("red_pigment");
    public static final DeferredItem<Item> YELLOW_PIGMENT = ITEMS.registerSimpleItem("yellow_pigment");
    public static final DeferredItem<Item> WHITE_PIGMENT = ITEMS.registerSimpleItem("white_pigment");
    public static final DeferredItem<PaintItem> BLACK_PAINT = paint("black_paint", CavePaintingBlock.Colour.BLACK);
    public static final DeferredItem<PaintItem> RED_PAINT = paint("red_paint", CavePaintingBlock.Colour.RED);
    public static final DeferredItem<PaintItem> YELLOW_PAINT = paint("yellow_paint", CavePaintingBlock.Colour.YELLOW);
    public static final DeferredItem<PaintItem> WHITE_PAINT = paint("white_paint", CavePaintingBlock.Colour.WHITE);
    public static final DeferredItem<PaintbrushItem> PAINTBRUSH = ITEMS.registerItem("paintbrush", PaintbrushItem::new, p -> p.durability(64));

    public static final DeferredBlock<RockBlock> LOOSE_RED_OCHRE = find("loose_red_ochre", MapColor.COLOR_RED);
    public static final DeferredBlock<RockBlock> LOOSE_YELLOW_OCHRE = find("loose_yellow_ochre", MapColor.COLOR_YELLOW);
    public static final DeferredBlock<RockBlock> LOOSE_CHALK = find("loose_chalk", MapColor.QUARTZ);

    public static final DeferredBlock<CavePaintingBlock> CAVE_PAINTING = BLOCKS.registerBlock("cave_painting", CavePaintingBlock::new, p -> p
            .mapColor(MapColor.TERRACOTTA_RED).replaceable().noCollision().noOcclusion().strength(0.5F).randomTicks()
            .sound(SoundType.SAND).pushReaction(PushReaction.POPPED).noLootTable());

    /** The motif the paintbrush paints, as an index into its list (PaintbrushItem). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BRUSH_MOTIF = Casting.COMPONENTS.registerComponentType(
            "brush_motif", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private static DeferredItem<PaintItem> paint(String name, CavePaintingBlock.Colour colour) {
        return ITEMS.registerItem(name, p -> new PaintItem(p, colour), p -> p.durability(PaintItem.USES));
    }

    private static DeferredBlock<RockBlock> find(String name, MapColor color) {
        return BLOCKS.registerBlock(name, RockBlock::new, p -> p
                .mapColor(color).strength(1.0F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.POPPED));
    }

    /** What one item on the Stone Anvil grinds into (twice what grinding by hand gives), or empty. */
    public static ItemStack ground(ItemStack stack) {
        if (stack.is(RED_OCHRE.get())) return new ItemStack(RED_PIGMENT.get(), 2);
        if (stack.is(YELLOW_OCHRE.get())) return new ItemStack(YELLOW_PIGMENT.get(), 2);
        if (stack.is(CHALK.get())) return new ItemStack(WHITE_PIGMENT.get(), 2);
        if (stack.is(Items.CALCITE)) return new ItemStack(WHITE_PIGMENT.get(), 4);
        if (stack.is(Items.CHARCOAL)) return new ItemStack(BLACK_PIGMENT.get(), 2);
        return Chemistry.ground(stack); // gypsum, chalk and ochre blocks
    }

    public static void init() {}

    private Pigments() {}
}
