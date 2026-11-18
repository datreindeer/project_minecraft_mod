package com.mechanical;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A recipe for a {@link RecipeStationMenu} (fletching table, armor station): three parts, each a number of an item, or a
 * tool that only loses durability (the bone needle). {@code unlockedBy}: the recipe unlocks the first time the player
 * carries a matching item (null = always unlocked).
 */
public record StationRecipe(Supplier<Item> result, int count, List<Part> parts, Predicate<ItemStack> unlockedBy) {
    public record Part(Supplier<Item> item, int count, boolean tool) {
        public static Part of(Supplier<Item> item, int count) {
            return new Part(item, count, false);
        }

        public static Part tool(Supplier<Item> item) {
            return new Part(item, 1, true);
        }

        public boolean matches(ItemStack stack) {
            return stack.is(item.get()) && stack.getCount() >= count;
        }
    }

    public static StationRecipe of(Supplier<Item> result, int count, Part a, Part b, Part c, Predicate<ItemStack> unlockedBy) {
        return new StationRecipe(result, count, List.of(a, b, c), unlockedBy);
    }

    public String id() {
        return BuiltInRegistries.ITEM.getKey(result.get()).toString();
    }

    public ItemStack assemble() {
        return new ItemStack(result.get(), count);
    }

    public boolean matches(ItemStack a, ItemStack b, ItemStack c) {
        return parts.get(0).matches(a) && parts.get(1).matches(b) && parts.get(2).matches(c);
    }

    public boolean isUnlocked(Player player) {
        return unlockedBy == null || player.getData(Mechanical.UNLOCKED_TOOLS).contains(id());
    }

    /** The player carries every part (enough of each). */
    public boolean hasParts(Player player) {
        return parts.stream().allMatch(p -> player.getInventory().countItem(p.item().get()) >= p.count());
    }

    /** All station recipes that unlock by material (the Tool Station has its own list in ToolStationRecipes). */
    static List<StationRecipe> lockable() {
        List<StationRecipe> all = new ArrayList<>(ArmorStationRecipes.ALL);
        all.addAll(FletchingRecipes.ALL);
        all.removeIf(r -> r.unlockedBy() == null);
        return all;
    }

    @EventBusSubscriber(modid = Mechanical.MODID)
    static final class Unlocks {
        @SubscribeEvent
        static void onPlayerTick(PlayerTickEvent.Post event) {
            Player player = event.getEntity();
            if (player.level().isClientSide() || player.tickCount % 20 != 10) return;
            List<String> unlocked = player.getData(Mechanical.UNLOCKED_TOOLS);
            List<String> updated = null;
            for (StationRecipe recipe : lockable()) {
                if (unlocked.contains(recipe.id()) || (updated != null && updated.contains(recipe.id()))) continue;
                if (player.getInventory().getNonEquipmentItems().stream().noneMatch(recipe.unlockedBy())) continue;
                if (updated == null) updated = new ArrayList<>(unlocked);
                updated.add(recipe.id());
            }
            if (updated != null) player.setData(Mechanical.UNLOCKED_TOOLS, List.copyOf(updated));
        }

        private Unlocks() {}
    }
}
