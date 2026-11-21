package com.mechanical;

import static com.mechanical.StationRecipe.Part.of;
import static com.mechanical.StationRecipe.Part.tool;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Armor Station: smithed armour. Ground bronze plates are riveted onto a leather lining with a hammer (#mechanical:hammerstones: any pickaxe
 * or the Bronze Smithing Hammer; loses 1 durability per piece), unlocked by carrying a Bronze Plate. Leather and fur pieces are sewn at the
 * Leatherworking Bench ({@link LeatherworkingRecipes}).
 */
public final class ArmorStationRecipes {
    private static final Predicate<ItemStack> BRONZE_PLATE = s -> s.is(Bronze.BRONZE_PLATE.get());
    private static final Predicate<ItemStack> IRON_PLATE = s -> s.is(Smithing.IRON_PLATE.get());

    public static final List<StationRecipe> ALL = List.of(
            StationRecipe.of(Bronze.BRONZE_HELMET::get, 1, of(Bronze.BRONZE_PLATE, 2), tool(BronzeOres.HAMMERSTONES, Bronze.BRONZE_PICKAXE), of(() -> Items.LEATHER, 2), BRONZE_PLATE),
            StationRecipe.of(Bronze.BRONZE_GREAVES::get, 1, of(Bronze.BRONZE_PLATE, 3), tool(BronzeOres.HAMMERSTONES, Bronze.BRONZE_PICKAXE), of(() -> Items.LEATHER, 3), BRONZE_PLATE),
            // forged Iron Plates (Smithing): vanilla's full iron armour, the chestplate the bronze age lacked
            StationRecipe.of(() -> Items.IRON_HELMET, 1, of(Smithing.IRON_PLATE, 3), tool(BronzeOres.HAMMERSTONES, Bronze.BRONZE_PICKAXE), of(() -> Items.LEATHER, 2), IRON_PLATE),
            StationRecipe.of(() -> Items.IRON_CHESTPLATE, 1, of(Smithing.IRON_PLATE, 6), tool(BronzeOres.HAMMERSTONES, Bronze.BRONZE_PICKAXE), of(() -> Items.LEATHER, 4), IRON_PLATE),
            StationRecipe.of(() -> Items.IRON_LEGGINGS, 1, of(Smithing.IRON_PLATE, 5), tool(BronzeOres.HAMMERSTONES, Bronze.BRONZE_PICKAXE), of(() -> Items.LEATHER, 3), IRON_PLATE),
            StationRecipe.of(() -> Items.IRON_BOOTS, 1, of(Smithing.IRON_PLATE, 2), tool(BronzeOres.HAMMERSTONES, Bronze.BRONZE_PICKAXE), of(() -> Items.LEATHER, 2), IRON_PLATE));

    private ArmorStationRecipes() {}
}
