package com.mechanical;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The metals of the bronze-age Crucible ({@link CrucibleContents}), Smelting Furnace ({@link FurnaceMelt}) and Clay Molds:
 * one unit is one Crushed Copper Ore. Everything about them (melting point, casts, ore units, the alloy rule) is in the
 * metal table ({@link Molten}, scripts/gen_metals.py); these constants exist because the Clay Mold's METAL block-state
 * property and the Smelting Furnace's saved arrays need a fixed set. A table metal that goes in a crucible or casts in
 * clay needs its constant here (start-up says so); add new ones at the end (the Smelting Furnace saves by this order).
 * POOR_BRONZE is bronze from the edges of the alloy's interval: it casts the same items, marked {@link Bronze#POOR}.
 */
public enum Metal implements StringRepresentable {
    COPPER, TIN, BRONZE, POOR_BRONZE;

    private final String id = name().toLowerCase(Locale.ROOT);

    /** Its row in the metal table. */
    public Molten molten() {
        return Molten.of(this);
    }

    /** Degrees the kiln (or Smelting Furnace) must reach to melt it. */
    public int melt() {
        return molten().melt;
    }

    public boolean casts(ClayMoldBlock.Shape shape) {
        return molten().cast(shape) != null;
    }

    /** Does it cast in any clay mold (the Clay Mold's METAL states)? */
    public boolean castsAnything() {
        return molten().castsAnything();
    }

    /** What a mold of this shape gives when this metal has set in it (empty if this metal can't be cast in it). */
    public ItemStack cast(ClayMoldBlock.Shape shape) {
        Item item = molten().cast(shape);
        if (item == null) return ItemStack.EMPTY;
        ItemStack out = new ItemStack(item);
        if (molten().poor) Bronze.markPoor(out);
        return out;
    }

    /** Is this an alloy that is already melted together (it can't be mixed with ore)? */
    public boolean isAlloy() {
        Molten.Crucible c = Molten.crucibleAlloy();
        return molten() == c.result() || molten() == c.poorResult();
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
