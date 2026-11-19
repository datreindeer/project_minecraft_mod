package com.mechanical;

import static com.mechanical.StationRecipe.Part.of;
import static com.mechanical.StationRecipe.Part.tool;

import java.util.List;
import java.util.function.Predicate;

import com.mechanical.wildlife.Wildlife;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Armor station: hides sewn into armour with a Bone Needle (loses 1 durability per piece) and thread. Leather pieces use
 * Wool Thread and unlock with Leather; fur pieces are heavy pelts sewn with Sinew and unlock with a Bear Pelt. The Glider
 * Cloak sews Phantom Leather onto a Glider Frame and unlocks with Phantom Leather.
 */
public final class ArmorStationRecipes {
    private static final Predicate<ItemStack> LEATHER = s -> s.is(Items.LEATHER);
    private static final Predicate<ItemStack> PELT = s -> s.is(Wildlife.BEAR_PELT.get());
    private static final Predicate<ItemStack> PHANTOM_LEATHER = s -> s.is(Mechanical.PHANTOM_LEATHER.get());

    public static final List<StationRecipe> ALL = List.of(
            StationRecipe.of(() -> Items.LEATHER_HELMET, 1, of(() -> Items.LEATHER, 5), tool(Mechanical.BONE_NEEDLE), of(Mechanical.WOOL_THREAD, 2), LEATHER),
            StationRecipe.of(() -> Items.LEATHER_CHESTPLATE, 1, of(() -> Items.LEATHER, 8), tool(Mechanical.BONE_NEEDLE), of(Mechanical.WOOL_THREAD, 4), LEATHER),
            StationRecipe.of(() -> Items.LEATHER_LEGGINGS, 1, of(() -> Items.LEATHER, 7), tool(Mechanical.BONE_NEEDLE), of(Mechanical.WOOL_THREAD, 3), LEATHER),
            StationRecipe.of(() -> Items.LEATHER_BOOTS, 1, of(() -> Items.LEATHER, 4), tool(Mechanical.BONE_NEEDLE), of(Mechanical.WOOL_THREAD, 2), LEATHER),
            StationRecipe.of(Mechanical.LEATHER_SATCHEL::get, 1, of(() -> Items.LEATHER, 4), tool(Mechanical.BONE_NEEDLE), of(Mechanical.WOOL_THREAD, 2), LEATHER),
            StationRecipe.of(Mechanical.FUR_HOOD::get, 1, of(Wildlife.BEAR_PELT, 1), tool(Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 1), PELT),
            StationRecipe.of(Mechanical.FUR_COAT::get, 1, of(Wildlife.BEAR_PELT, 3), tool(Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 3), PELT),
            StationRecipe.of(Mechanical.FUR_LEGGINGS::get, 1, of(Wildlife.BEAR_PELT, 2), tool(Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 2), PELT),
            StationRecipe.of(Mechanical.FUR_BOOTS::get, 1, of(Wildlife.BEAR_PELT, 1), tool(Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 1), PELT),
            // the membrane is sewn onto a lashed frame of rods and bones (crafted): a late stone age piece
            StationRecipe.of(Mechanical.GLIDER_CLOAK::get, 1, of(Mechanical.PHANTOM_LEATHER, 6), tool(Mechanical.BONE_NEEDLE), of(Mechanical.GLIDER_FRAME, 1),
                    PHANTOM_LEATHER));

    private ArmorStationRecipes() {}
}
