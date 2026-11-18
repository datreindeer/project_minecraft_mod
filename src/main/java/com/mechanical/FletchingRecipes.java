package com.mechanical;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.mechanical.wildlife.Wildlife;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Fletching table recipes: tip + shaft + fletching (or, for bows: string + bow + grip) -> result.
 * shortcut: hardcoded like the Tool Station recipes; move to datapack recipes if modpacks need to edit them.
 */
public final class FletchingRecipes {
    public record Recipe(Supplier<Item> result, int count, Supplier<Item> first, Supplier<Item> second, Supplier<Item> third) {
        public boolean matches(ItemStack a, ItemStack b, ItemStack c) {
            return a.is(first.get()) && b.is(second.get()) && c.is(third.get());
        }

        public ItemStack assemble() {
            return new ItemStack(result.get(), count);
        }
    }

    public static final List<Recipe> ALL = List.of(
            new Recipe(() -> Items.ARROW, 4, () -> Items.FLINT, () -> Items.STICK, () -> Items.FEATHER),
            new Recipe(() -> Mechanical.BONE_ARROW.get(), 4, () -> Mechanical.FISH_BONES.get(), () -> Items.STICK, () -> Items.FEATHER),
            new Recipe(() -> Mechanical.BONE_ARROW.get(), 4, () -> Items.BONE, () -> Items.STICK, () -> Items.FEATHER),
            new Recipe(() -> Mechanical.BARBED_ARROW.get(), 4, () -> Wildlife.BEAR_CLAW.get(), () -> Items.STICK, () -> Items.FEATHER),
            new Recipe(() -> Mechanical.FIRE_ARROW.get(), 1, () -> Mechanical.TALLOW.get(), () -> Items.ARROW, () -> Mechanical.TWINE.get()),
            new Recipe(() -> Mechanical.SINEW_BOW.get(), 1, () -> Wildlife.SINEW.get(), () -> Mechanical.CRUDE_BOW.get(), () -> Mechanical.CORDAGE.get()));

    public static Optional<Recipe> find(ItemStack a, ItemStack b, ItemStack c) {
        return ALL.stream().filter(r -> r.matches(a, b, c)).findFirst();
    }

    private FletchingRecipes() {}
}
