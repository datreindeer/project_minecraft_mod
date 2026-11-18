package com.mechanical;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Tool Station recipes: head + binding + rod = tool. A recipe is unlocked for a player the first time
 * they hold its material (rock chunks for stone, the ingot for metals).
 * shortcut: recipes are hardcoded so client and server agree without syncing; move to datapack recipes when modpacks need to edit them.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class ToolStationRecipes {
    public record Recipe(Supplier<Item> result, Supplier<Item> head, Supplier<Item> binding, Supplier<Item> rod, Predicate<ItemStack> material) {
        public String id() {
            return BuiltInRegistries.ITEM.getKey(result.get()).toString();
        }

        public List<Item> parts() {
            return List.of(head.get(), binding.get(), rod.get());
        }

        public boolean matches(ItemStack head, ItemStack binding, ItemStack rod) {
            return head.is(this.head.get()) && binding.is(this.binding.get()) && rod.is(this.rod.get());
        }
    }

    private static final Predicate<ItemStack> STONE = s -> s.is(Mechanical.ROCK_CHUNKS);
    private static final Predicate<ItemStack> FLINT = s -> s.is(Items.FLINT);

    /** Order matters: the screen sends the index of the clicked recipe to the server. */
    public static final List<Recipe> ALL = List.of(
            new Recipe(Mechanical.CRUDE_STONE_PICKAXE, Mechanical.STONE_PICKAXE_HEAD, Mechanical.TWINE, () -> Items.STICK, STONE),
            new Recipe(() -> Items.STONE_PICKAXE, Mechanical.DOUBLE_STONE_PICKAXE_HEAD, Mechanical.CORDAGE, Mechanical.TOOL_ROD, STONE),
            new Recipe(Mechanical.CRUDE_STONE_AXE, Mechanical.STONE_AXE_HEAD, Mechanical.TWINE, () -> Items.STICK, STONE),
            new Recipe(() -> Items.STONE_AXE, Mechanical.DOUBLE_STONE_AXE_HEAD, Mechanical.CORDAGE, Mechanical.TOOL_ROD, STONE),
            new Recipe(Mechanical.FLINT_SPEAR, () -> Items.FLINT, Mechanical.CORDAGE, Mechanical.TOOL_ROD, FLINT),
            // ranged: "head" = bowstring / hook, "binding" = grip wrap / line
            new Recipe(() -> Mechanical.CRUDE_BOW.get(), Mechanical.CORDAGE, Mechanical.TWINE, Mechanical.TOOL_ROD, s -> s.is(Mechanical.CORDAGE.get())),
            new Recipe(() -> Mechanical.CRUDE_FISHING_ROD.get(), () -> Items.FLINT, Mechanical.TWINE, Mechanical.TOOL_ROD, s -> s.is(Mechanical.TWINE.get())),
            new Recipe(() -> Mechanical.TUSK_DAGGER.get(), () -> com.mechanical.wildlife.Wildlife.TUSK.get(), Mechanical.CORDAGE, () -> Items.LEATHER,
                    s -> s.is(com.mechanical.wildlife.Wildlife.TUSK.get())),
            new Recipe(() -> Mechanical.BONE_HOOK_FISHING_ROD.get(), () -> Mechanical.BONE_FISH_HOOK.get(), Mechanical.TWINE, Mechanical.TOOL_ROD,
                    s -> s.is(Mechanical.BONE_FISH_HOOK.get())));

    public static boolean isUnlocked(Player player, Recipe recipe) {
        return player.getData(Mechanical.UNLOCKED_TOOLS).contains(recipe.id());
    }

    /** True if the player's inventory holds every part of the recipe. */
    public static boolean hasParts(Player player, Recipe recipe) {
        return recipe.parts().stream().allMatch(part -> player.getInventory().contains(new ItemStack(part)));
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) return;
        List<String> unlocked = player.getData(Mechanical.UNLOCKED_TOOLS);
        List<String> updated = null;
        for (Recipe recipe : ALL) {
            if (unlocked.contains(recipe.id()) || !holdsMaterial(player, recipe)) continue;
            if (updated == null) updated = new ArrayList<>(unlocked);
            updated.add(recipe.id());
        }
        if (updated != null) player.setData(Mechanical.UNLOCKED_TOOLS, List.copyOf(updated));
    }

    private static boolean holdsMaterial(Player player, Recipe recipe) {
        return player.getInventory().getNonEquipmentItems().stream().anyMatch(recipe.material());
    }

    private ToolStationRecipes() {}
}
