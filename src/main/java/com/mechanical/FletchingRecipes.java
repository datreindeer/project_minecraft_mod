package com.mechanical;

import static com.mechanical.StationRecipe.Part.of;

import java.util.List;

import com.mechanical.wildlife.Wildlife;
import com.mechanical.wildlife.forestbirds.ForestBirdsFauna;

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
            StationRecipe.of(() -> Mechanical.BARBED_ARROW.get(), 4, of(() -> Wildlife.BEAR_CLAW.get(), 1, () -> ForestBirdsFauna.EAGLE_TALON.get()), of(() -> Items.STICK, 1), of(() -> Items.FEATHER, 1), null),
            // fauna: mountain - hyena fangs barb arrows like bear claws; ibex horn glued over a sinew bow makes the composite Horn Bow
            StationRecipe.of(() -> Mechanical.BARBED_ARROW.get(), 4, of(() -> com.mechanical.wildlife.mountain.MountainFauna.HYENA_FANG.get(), 1), of(() -> Items.STICK, 1), of(() -> Items.FEATHER, 1), null),
            // fauna: underground - the giant centipede's hooked venom claws barb arrows too
            StationRecipe.of(() -> Mechanical.BARBED_ARROW.get(), 4, of(() -> com.mechanical.wildlife.underground.UndergroundFauna.CENTIPEDE_FANG.get(), 1), of(() -> Items.STICK, 1), of(() -> Items.FEATHER, 1), null),
            StationRecipe.of(() -> com.mechanical.wildlife.mountain.MountainFauna.HORN_BOW.get(), 1, of(() -> Wildlife.SINEW.get(), 2), of(() -> Mechanical.SINEW_BOW.get(), 1),
                    of(() -> com.mechanical.wildlife.mountain.MountainFauna.IBEX_HORN.get(), 2), null),
            // fauna: desert - sand viper fangs barb arrows; one great Bighorn Horn does for the two ibex horns; a condor feather fletches more eagle arrows
            StationRecipe.of(() -> Mechanical.BARBED_ARROW.get(), 4, of(() -> com.mechanical.wildlife.desert.DesertFauna.VIPER_FANG.get(), 1), of(() -> Items.STICK, 1), of(() -> Items.FEATHER, 1), null),
            StationRecipe.of(() -> com.mechanical.wildlife.mountain.MountainFauna.HORN_BOW.get(), 1, of(() -> Wildlife.SINEW.get(), 2), of(() -> Mechanical.SINEW_BOW.get(), 1),
                    of(() -> com.mechanical.wildlife.desert.DesertFauna.BIGHORN_HORN.get(), 1), null),
            StationRecipe.of(() -> ForestBirdsFauna.EAGLE_FLETCHED_ARROW.get(), 6, of(() -> Items.FLINT, 1), of(() -> Items.STICK, 1), of(() -> com.mechanical.wildlife.desert.DesertFauna.CONDOR_FEATHER.get(), 1), null),
            // fauna: forestbirds (broad capercaillie tail feathers fletch more arrows; eagle feathers fly hardest; owl feathers fly silent)
            StationRecipe.of(() -> Items.ARROW, 6, of(() -> Items.FLINT, 1), of(() -> Items.STICK, 1), of(() -> ForestBirdsFauna.CAPERCAILLIE_TAIL_FEATHER.get(), 1,
                    () -> com.mechanical.wildlife.cold.ColdFauna.BLACK_GROUSE_TAIL_FEATHER.get()), null),
            StationRecipe.of(() -> ForestBirdsFauna.EAGLE_FLETCHED_ARROW.get(), 4, of(() -> Items.FLINT, 1), of(() -> Items.STICK, 1), of(() -> ForestBirdsFauna.EAGLE_FEATHER.get(), 1,
                    () -> com.mechanical.wildlife.cold.ColdFauna.LAMMERGEIER_FEATHER.get()), null),
            StationRecipe.of(() -> ForestBirdsFauna.SILENT_ARROW.get(), 4, of(() -> Items.FLINT, 1), of(() -> Items.STICK, 1), of(() -> ForestBirdsFauna.OWL_FEATHER.get(), 1,
                    () -> com.mechanical.wildlife.cold.ColdFauna.SNOWY_OWL_FEATHER.get()), null),
            // bronze: four ground Bronze Arrowheads (one cast) on four shafts; a Poor Bronze head makes poor arrows (Bronze.inheritPoor)
            StationRecipe.of(() -> Bronze.BRONZE_ARROW.get(), 4, of(() -> Bronze.BRONZE_ARROWHEAD.get(), 4), of(() -> Items.STICK, 1), of(() -> Items.FEATHER, 1), null),
            // iron: four forged Iron Bolt Heads (one ingot) on four short shafts, crossbow bolts
            StationRecipe.of(() -> Smithing.IRON_BOLT.get(), 4, of(() -> Smithing.IRON_BOLT_HEAD.get(), 4), of(() -> Items.STICK, 1), of(() -> Items.FEATHER, 1), null),
            StationRecipe.of(() -> Mechanical.FIRE_ARROW.get(), 1, of(() -> Mechanical.TALLOW.get(), 1), of(() -> Items.ARROW, 1), of(() -> Mechanical.TWINE.get(), 1), null),
            StationRecipe.of(() -> Mechanical.SINEW_BOW.get(), 1, of(() -> Wildlife.SINEW.get(), 1), of(() -> Mechanical.CRUDE_BOW.get(), 1), of(() -> Mechanical.CORDAGE.get(), 1, () -> Kettle.HIDE_GLUE.get()), null));

    private FletchingRecipes() {}
}
