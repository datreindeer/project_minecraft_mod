package com.mechanical.viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

/** The JSON (datapack) recipes of the six kinds the viewer shows, as {@link ViewerRecipe}s. */
public final class JsonSources {
    private JsonSources() {}

    public static List<ViewerRecipe> from(RecipeManager rm, HolderLookup.Provider regs) {
        List<ViewerRecipe> out = new ArrayList<>();
        for (RecipeHolder<?> h : rm.getRecipes()) convert(h).ifPresent(out::add);
        return out;
    }

    /** Empty when the type is not shown, or an ingredient has no items (an empty tag) so the recipe could never be made. */
    static Optional<ViewerRecipe> convert(RecipeHolder<?> holder) {
        var r = holder.value();
        if (r instanceof ShapedRecipe s) {
            List<Slot> slots = new ArrayList<>();
            for (Optional<Ingredient> ing : s.getIngredients()) {
                if (ing.isEmpty()) slots.add(new Slot(List.of(), 0, false));
                else if (!add(slots, ing.get())) return Optional.empty();
            }
            return make(Station.CRAFTING_SHAPED, slots, s.assemble(CraftingInput.EMPTY), s.getWidth());
        }
        if (r instanceof ShapelessRecipe s) {
            List<Slot> slots = new ArrayList<>();
            for (Ingredient ing : s.placementInfo().ingredients()) if (!add(slots, ing)) return Optional.empty();
            // an ingredient with no items makes the placement info empty, which is how it shows here
            return s.result() == null || slots.isEmpty() ? Optional.empty() : make(Station.CRAFTING_SHAPELESS, slots, s.result().create(), 0);
        }
        if (r instanceof SingleItemRecipe s) {
            Station st = r instanceof StonecutterRecipe ? Station.STONECUTTING : r instanceof CampfireCookingRecipe ? Station.CAMPFIRE
                    : r instanceof SmokingRecipe ? Station.SMOKING : r instanceof SmeltingRecipe ? Station.SMELTING : null;
            if (st == null) return Optional.empty();
            List<Slot> slots = new ArrayList<>();
            if (!add(slots, s.input())) return Optional.empty();
            return make(st, slots, s.assemble(new SingleRecipeInput(ItemStack.EMPTY)), 0);
        }
        return Optional.empty();
    }

    private static boolean add(List<Slot> slots, Ingredient ing) {
        List<Item> options = ing.items().map(Holder::value).toList();
        if (options.isEmpty()) return false;
        slots.add(new Slot(options, 1, false));
        return true;
    }

    private static Optional<ViewerRecipe> make(Station st, List<Slot> slots, ItemStack out, int width) {
        return out.isEmpty() ? Optional.empty() : Optional.of(new ViewerRecipe(st, slots, out, width, UnlockKind.ALL_SEEN, ""));
    }
}
