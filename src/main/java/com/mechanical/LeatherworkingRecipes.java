package com.mechanical;

import static com.mechanical.StationRecipe.Part.of;
import static com.mechanical.StationRecipe.Part.tool;

import java.util.List;
import java.util.function.Predicate;

import com.mechanical.wildlife.Wildlife;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Leatherworking Bench: hides sewn into clothes, armour and bags with a needle (#mechanical:needles: Bone or Bronze Needle; loses 1 durability per piece) and thread. Leather pieces use
 * any thread (#mechanical:threads: wool, nettle, linen, sinew thread, string) and unlock with Leather; fur pieces are heavy pelts sewn with Sinew and unlock with a Bear Pelt
 * (Wolf Fur and the Wolf Hood: with a Wolf Pelt). The Glider
 * Cloak sews Phantom Leather onto a Glider Frame and unlocks with Phantom Leather. The Satchel and the Quiver unlock with Leather,
 * Leather Horse Armor (for the tamed horse) too.
 * Smithed (metal) armour is made at the Armor Station, see {@link ArmorStationRecipes}.
 */
public final class LeatherworkingRecipes {
    private static final Predicate<ItemStack> LEATHER = s -> s.is(Items.LEATHER);
    private static final Predicate<ItemStack> PELT = s -> s.is(Wildlife.BEAR_PELT.get());
    private static final Predicate<ItemStack> WOLF_PELT = s -> s.is(Wildlife.WOLF_PELT.get());
    private static final Predicate<ItemStack> PHANTOM_LEATHER = s -> s.is(Mechanical.PHANTOM_LEATHER.get());

    public static final List<StationRecipe> ALL = List.of(
            StationRecipe.of(() -> Items.LEATHER_HELMET, 1, of(() -> Items.LEATHER, 5), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Spinning.THREADS, Mechanical.WOOL_THREAD, 2), LEATHER),
            StationRecipe.of(() -> Items.LEATHER_CHESTPLATE, 1, of(() -> Items.LEATHER, 8), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Spinning.THREADS, Mechanical.WOOL_THREAD, 4), LEATHER),
            StationRecipe.of(() -> Items.LEATHER_LEGGINGS, 1, of(() -> Items.LEATHER, 7), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Spinning.THREADS, Mechanical.WOOL_THREAD, 3), LEATHER),
            StationRecipe.of(() -> Items.LEATHER_BOOTS, 1, of(() -> Items.LEATHER, 4), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Spinning.THREADS, Mechanical.WOOL_THREAD, 2), LEATHER),
            StationRecipe.of(Mechanical.LEATHER_SATCHEL::get, 1, of(() -> Items.LEATHER, 4), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Spinning.THREADS, Mechanical.WOOL_THREAD, 2), LEATHER),
            // end of the Iron Age (Satchels): loops for tools with steel buckles, and the 5x5 pack on a steel frame; unlock with Steel Plate
            StationRecipe.of(Satchels.TOOL_LOOPS::get, 1, of(() -> Items.LEATHER, 2), of(Smithing.STEEL_PLATE, 1), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE),
                    s -> s.is(Smithing.STEEL_PLATE.get())),
            StationRecipe.of(Satchels.STEEL_FRAME_PACK::get, 1, of(() -> Items.LEATHER, 10), of(Smithing.STEEL_PLATE, 3), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE),
                    s -> s.is(Smithing.STEEL_PLATE.get())),
            StationRecipe.of(Mechanical.FUR_HOOD::get, 1, of(Wildlife.BEAR_PELT, 1), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 1), PELT),
            StationRecipe.of(Mechanical.FUR_COAT::get, 1, of(Wildlife.BEAR_PELT, 3), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 3), PELT),
            StationRecipe.of(Mechanical.FUR_LEGGINGS::get, 1, of(Wildlife.BEAR_PELT, 2), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 2), PELT),
            StationRecipe.of(Mechanical.FUR_BOOTS::get, 1, of(Wildlife.BEAR_PELT, 1), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 1), PELT),
            // wolf pelts are smaller than a bear's: more of them per piece
            StationRecipe.of(Mechanical.WOLF_HOOD::get, 1, of(Wildlife.WOLF_PELT, 2), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 1), WOLF_PELT),
            StationRecipe.of(Mechanical.WOLF_FUR_COAT::get, 1, of(Wildlife.WOLF_PELT, 4), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 3), WOLF_PELT),
            StationRecipe.of(Mechanical.WOLF_FUR_LEGGINGS::get, 1, of(Wildlife.WOLF_PELT, 3), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 2), WOLF_PELT),
            StationRecipe.of(Mechanical.WOLF_FUR_BOOTS::get, 1, of(Wildlife.WOLF_PELT, 2), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 1), WOLF_PELT),
            // fauna: mountain - Chamois Boots; marmot pelts and the big cave bear pelt as fur (fewer pelts per piece than the bear's)
            StationRecipe.of(com.mechanical.wildlife.mountain.MountainFauna.CHAMOIS_BOOTS::get, 1, of(com.mechanical.wildlife.mountain.MountainFauna.CHAMOIS_LEATHER, 3), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 1),
                    s -> s.is(com.mechanical.wildlife.mountain.MountainFauna.CHAMOIS_LEATHER.get())),
            StationRecipe.of(Mechanical.FUR_HOOD::get, 1, of(com.mechanical.wildlife.mountain.MountainFauna.MARMOT_PELT, 3), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 1),
                    s -> s.is(com.mechanical.wildlife.mountain.MountainFauna.MARMOT_PELT.get())),
            StationRecipe.of(Mechanical.FUR_BOOTS::get, 1, of(com.mechanical.wildlife.mountain.MountainFauna.MARMOT_PELT, 3), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 1),
                    s -> s.is(com.mechanical.wildlife.mountain.MountainFauna.MARMOT_PELT.get())),
            StationRecipe.of(Mechanical.FUR_COAT::get, 1, of(com.mechanical.wildlife.mountain.MountainFauna.CAVE_BEAR_PELT, 2), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 3),
                    s -> s.is(com.mechanical.wildlife.mountain.MountainFauna.CAVE_BEAR_PELT.get())),
            StationRecipe.of(Mechanical.FUR_LEGGINGS::get, 1, of(com.mechanical.wildlife.mountain.MountainFauna.CAVE_BEAR_PELT, 1), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 2),
                    s -> s.is(com.mechanical.wildlife.mountain.MountainFauna.CAVE_BEAR_PELT.get())),
            // coast fauna: sealskin (warm and waterproof) and layered walrus hide; unlocked by carrying the cured skin
            StationRecipe.of(com.mechanical.wildlife.coast.CoastFauna.SEALSKIN_PARKA::get, 1, of(com.mechanical.wildlife.coast.CoastFauna.SEALSKIN, 4),
                    tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 3), s -> s.is(com.mechanical.wildlife.coast.CoastFauna.SEALSKIN.get())),
            StationRecipe.of(com.mechanical.wildlife.coast.CoastFauna.SEALSKIN_BOOTS::get, 1, of(com.mechanical.wildlife.coast.CoastFauna.SEALSKIN, 2),
                    tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 1), s -> s.is(com.mechanical.wildlife.coast.CoastFauna.SEALSKIN.get())),
            StationRecipe.of(com.mechanical.wildlife.coast.CoastFauna.WALRUS_HIDE_CUIRASS::get, 1, of(com.mechanical.wildlife.coast.CoastFauna.WALRUS_HIDE, 2),
                    tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Wildlife.SINEW, 4), s -> s.is(com.mechanical.wildlife.coast.CoastFauna.WALRUS_HIDE.get())),
            // the membrane is sewn onto a lashed frame of rods and bones (crafted): a late stone age piece
            StationRecipe.of(Mechanical.GLIDER_CLOAK::get, 1, of(Mechanical.PHANTOM_LEATHER, 6), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Mechanical.GLIDER_FRAME, 1),
                    PHANTOM_LEATHER),
            // the Quiver: a leather tube stitched shut
            StationRecipe.of(() -> Mechanical.QUIVER.get(), 1, of(() -> Items.LEATHER, 2), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Spinning.THREADS, Mechanical.WOOL_THREAD, 2), LEATHER),
            // barding for the tamed horse (vanilla's crafting recipe stays off)
            StationRecipe.of(() -> Items.LEATHER_HORSE_ARMOR, 1, of(() -> Items.LEATHER, 7), tool(BronzeTools.NEEDLES, Mechanical.BONE_NEEDLE), of(Spinning.THREADS, Mechanical.WOOL_THREAD, 4),
                    LEATHER));

    private LeatherworkingRecipes() {}
}
