package com.mechanical;

import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Metals that melt in a Crucible and cast in a Clay Mold. A crucible holds units of each metal (see
 * {@link CrucibleContents}); one unit is one Crushed Copper Ore. Bronze (9 copper : 1 tin, melts at ~950°) goes here
 * next, together with a tin entry in {@link Casting#oreUnits} and an alloy rule in {@link CrucibleContents#alloy}.
 */
public enum Metal implements StringRepresentable {
    COPPER("copper", 1085, Map.of(
            ClayMoldBlock.Shape.INGOT, () -> Items.COPPER_INGOT,
            ClayMoldBlock.Shape.PICKAXE_HEAD, () -> Casting.ROUGH_CAST_COPPER_PICKAXE_HEAD.get(),
            ClayMoldBlock.Shape.AXE_HEAD, () -> Casting.ROUGH_CAST_COPPER_AXE_HEAD.get(),
            ClayMoldBlock.Shape.SHOVEL_HEAD, () -> Casting.ROUGH_CAST_COPPER_SHOVEL_HEAD.get(),
            ClayMoldBlock.Shape.HOE_HEAD, () -> Casting.ROUGH_CAST_COPPER_HOE_HEAD.get(),
            ClayMoldBlock.Shape.KNIFE_BLADE, () -> Casting.ROUGH_CAST_COPPER_KNIFE_BLADE.get()));

    private final String id;
    /** Degrees the kiln must reach to melt it. */
    public final int melt;
    private final Map<ClayMoldBlock.Shape, Supplier<Item>> casts;

    Metal(String id, int melt, Map<ClayMoldBlock.Shape, Supplier<Item>> casts) {
        this.id = id;
        this.melt = melt;
        this.casts = casts;
    }

    /** What a mold of this shape gives when this metal has set in it. */
    public Item cast(ClayMoldBlock.Shape shape) {
        return casts.get(shape).get();
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
