package com.mechanical;

import static com.mechanical.StationRecipe.Part.of;

import java.util.List;

import com.mechanical.wildlife.Wildlife;

import net.minecraft.world.item.Items;

/**
 * Fletching table recipes: tip + shaft + fletching (or, for bows: string + bow + grip) -> result. All unlocked from the start.
 * shortcut: hardcoded like the Tool Station recipes; move to datapack recipes if modpacks need to edit them.
 */
public final class FletchingRecipes {
    public static final List<StationRecipe> ALL = List.of(
            StationRecipe.of(() -> Items.ARROW, 4, of(() -> Items.FLINT, 1), of(() -> Items.STICK, 1), of(() -> Items.FEATHER, 1), null),
            StationRecipe.of(() -> Mechanical.BONE_ARROW.get(), 4, of(() -> Mechanical.FISH_BONES.get(), 1), of(() -> Items.STICK, 1), of(() -> Items.FEATHER, 1), null),
            StationRecipe.of(() -> Mechanical.BONE_ARROW.get(), 4, of(() -> Items.BONE, 1), of(() -> Items.STICK, 1), of(() -> Items.FEATHER, 1), null),
            StationRecipe.of(() -> Mechanical.BARBED_ARROW.get(), 4, of(() -> Wildlife.BEAR_CLAW.get(), 1), of(() -> Items.STICK, 1), of(() -> Items.FEATHER, 1), null),
            StationRecipe.of(() -> Mechanical.FIRE_ARROW.get(), 1, of(() -> Mechanical.TALLOW.get(), 1), of(() -> Items.ARROW, 1), of(() -> Mechanical.TWINE.get(), 1), null),
            StationRecipe.of(() -> Mechanical.SINEW_BOW.get(), 1, of(() -> Wildlife.SINEW.get(), 1), of(() -> Mechanical.CRUDE_BOW.get(), 1), of(() -> Mechanical.CORDAGE.get(), 1, () -> Kettle.HIDE_GLUE.get()), null));

    private FletchingRecipes() {}
}
