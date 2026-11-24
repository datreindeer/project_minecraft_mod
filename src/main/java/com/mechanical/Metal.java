package com.mechanical;

import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Metals in a Crucible (see {@link CrucibleContents}); one unit is one Crushed Copper Ore. Tin is only an ore here:
 * copper + 8-15 % tin melts into bronze ({@link CrucibleContents#alloy}). POOR_BRONZE is bronze from the edges of that
 * interval: it casts the same items, marked {@link Bronze#POOR} (tools and armour from it get 70 % durability).
 * Also the METAL block-state property of a {@link ClayMoldBlock}.
 */
public enum Metal implements StringRepresentable {
    COPPER("copper", 1085, false, Map.of(
            ClayMoldBlock.Shape.INGOT, () -> Items.COPPER_INGOT,
            ClayMoldBlock.Shape.PICKAXE_HEAD, () -> Casting.ROUGH_CAST_COPPER_PICKAXE_HEAD.get(),
            ClayMoldBlock.Shape.AXE_HEAD, () -> Casting.ROUGH_CAST_COPPER_AXE_HEAD.get(),
            ClayMoldBlock.Shape.SHOVEL_HEAD, () -> Casting.ROUGH_CAST_COPPER_SHOVEL_HEAD.get(),
            ClayMoldBlock.Shape.HOE_HEAD, () -> Casting.ROUGH_CAST_COPPER_HOE_HEAD.get(),
            ClayMoldBlock.Shape.KNIFE_BLADE, () -> Casting.ROUGH_CAST_COPPER_KNIFE_BLADE.get())),
    TIN("tin", 232, false, Map.of()),
    BRONZE("bronze", 950, false, bronzeCasts()),
    POOR_BRONZE("poor_bronze", 950, true, bronzeCasts());

    private final String id;
    /** Degrees the kiln must reach to melt it. */
    public final int melt;
    public final boolean poor;
    private final Map<ClayMoldBlock.Shape, Supplier<Item>> casts;

    Metal(String id, int melt, boolean poor, Map<ClayMoldBlock.Shape, Supplier<Item>> casts) {
        this.id = id;
        this.melt = melt;
        this.poor = poor;
        this.casts = casts;
    }

    private static Map<ClayMoldBlock.Shape, Supplier<Item>> bronzeCasts() {
        return Map.ofEntries(
                Map.entry(ClayMoldBlock.Shape.INGOT, () -> Bronze.BRONZE_INGOT.get()),
                Map.entry(ClayMoldBlock.Shape.PICKAXE_HEAD, () -> Bronze.ROUGH_CAST_BRONZE_PICKAXE_HEAD.get()),
                Map.entry(ClayMoldBlock.Shape.AXE_HEAD, () -> Bronze.ROUGH_CAST_BRONZE_AXE_HEAD.get()),
                Map.entry(ClayMoldBlock.Shape.SHOVEL_HEAD, () -> Bronze.ROUGH_CAST_BRONZE_SHOVEL_HEAD.get()),
                Map.entry(ClayMoldBlock.Shape.HOE_HEAD, () -> Bronze.ROUGH_CAST_BRONZE_HOE_HEAD.get()),
                Map.entry(ClayMoldBlock.Shape.KNIFE_BLADE, () -> Bronze.ROUGH_CAST_BRONZE_KNIFE_BLADE.get()),
                Map.entry(ClayMoldBlock.Shape.SAW_BLADE, () -> Bronze.ROUGH_CAST_BRONZE_SAW_BLADE.get()),
                Map.entry(ClayMoldBlock.Shape.PLATE, () -> Bronze.ROUGH_CAST_BRONZE_PLATE.get()),
                Map.entry(ClayMoldBlock.Shape.SPEAR_HEAD, () -> Bronze.ROUGH_CAST_BRONZE_SPEAR_HEAD.get()),
                Map.entry(ClayMoldBlock.Shape.SWORD_BLADE, () -> Bronze.ROUGH_CAST_BRONZE_SWORD_BLADE.get()),
                Map.entry(ClayMoldBlock.Shape.KETTLE, () -> Bronze.ROUGH_CAST_BRONZE_KETTLE.get()),
                Map.entry(ClayMoldBlock.Shape.ARROWHEADS, () -> Bronze.ROUGH_CAST_BRONZE_ARROWHEADS.get()),
                Map.entry(ClayMoldBlock.Shape.BUCKET, () -> BronzeTools.ROUGH_CAST_BRONZE_BUCKET.get()),
                Map.entry(ClayMoldBlock.Shape.SICKLE_BLADE, () -> BronzeTools.ROUGH_CAST_BRONZE_SICKLE_BLADE.get()),
                Map.entry(ClayMoldBlock.Shape.SHEARS, () -> BronzeTools.ROUGH_CAST_BRONZE_SHEARS.get()),
                Map.entry(ClayMoldBlock.Shape.FISH_HOOKS, () -> BronzeTools.ROUGH_CAST_BRONZE_FISH_HOOKS.get()),
                Map.entry(ClayMoldBlock.Shape.NEEDLES, () -> BronzeTools.ROUGH_CAST_BRONZE_NEEDLES.get()),
                Map.entry(ClayMoldBlock.Shape.TONGS, () -> BronzeTools.ROUGH_CAST_BRONZE_TONGS.get()),
                Map.entry(ClayMoldBlock.Shape.CHISEL_BLADE, () -> BronzeTools.ROUGH_CAST_BRONZE_CHISEL_BLADE.get()),
                Map.entry(ClayMoldBlock.Shape.HAMMER_HEAD, () -> Ironworking.ROUGH_CAST_BRONZE_HAMMER_HEAD.get()),
                Map.entry(ClayMoldBlock.Shape.ANVIL, () -> Ironworking.ROUGH_CAST_BRONZE_ANVIL.get()),
                Map.entry(ClayMoldBlock.Shape.TUYERE, () -> HighFurnace.BRONZE_TUYERE_NOZZLE.get()));
    }

    public boolean casts(ClayMoldBlock.Shape shape) {
        return casts.containsKey(shape);
    }

    /** What a mold of this shape gives when this metal has set in it (empty if this metal can't be cast in it). */
    public ItemStack cast(ClayMoldBlock.Shape shape) {
        Supplier<Item> item = casts.get(shape);
        if (item == null) return ItemStack.EMPTY;
        ItemStack out = new ItemStack(item.get());
        if (poor) Bronze.markPoor(out);
        return out;
    }

    /** Is this an alloy that is already melted together (it can't be mixed with ore)? */
    public boolean isAlloy() {
        return this == BRONZE || this == POOR_BRONZE;
    }

    public static Metal byId(String id) {
        for (Metal m : values()) if (m.id.equals(id)) return m;
        return null;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
