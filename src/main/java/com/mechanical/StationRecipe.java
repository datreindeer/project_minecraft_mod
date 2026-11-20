package com.mechanical;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A recipe for a {@link RecipeStationMenu} (fletching table, armor station, mold bench): up to three parts, each a number
 * of an item, or a tool that only loses durability (the bone needle). {@code unlockedBy}: the recipe unlocks the first time the player
 * carries a matching item (null = always unlocked).
 */
public record StationRecipe(Supplier<Item> result, int count, List<Part> parts, Predicate<ItemStack> unlockedBy) {
    /**
     * {@code alt}: an item that may stand in for {@code item}, or null. {@code tag}: any item of this tag will do (e.g.
     * #mechanical:threads); {@code item} is then only what the screen shows. One kind per slot.
     */
    public record Part(Supplier<Item> item, int count, boolean tool, @Nullable Supplier<Item> alt, @Nullable TagKey<Item> tag) {
        public static Part of(Supplier<Item> item, int count) {
            return new Part(item, count, false, null, null);
        }

        public static Part of(Supplier<Item> item, int count, Supplier<Item> alt) {
            return new Part(item, count, false, alt, null);
        }

        public static Part of(TagKey<Item> tag, Supplier<Item> shown, int count) {
            return new Part(shown, count, false, null, tag);
        }

        public static Part tool(Supplier<Item> item) {
            return new Part(item, 1, true, null, null);
        }

        public boolean matches(ItemStack stack) {
            boolean kind = tag != null ? stack.is(tag) : stack.is(item.get()) || alt != null && stack.is(alt.get());
            return kind && stack.getCount() >= count;
        }

        /** The item to use from this inventory: the main one if there is enough of it, else the stand-in (or a tag item) that suffices. */
        public Item pick(Inventory inventory) {
            if (tag != null) {
                for (ItemStack s : inventory.getNonEquipmentItems()) {
                    if (s.is(tag) && inventory.countItem(s.getItem()) >= count) return s.getItem();
                }
                return item.get();
            }
            if (alt == null || inventory.countItem(item.get()) >= count || inventory.countItem(alt.get()) < count) return item.get();
            return alt.get();
        }

        public boolean has(Inventory inventory) {
            return inventory.countItem(pick(inventory)) >= count;
        }
    }

    public static StationRecipe of(Supplier<Item> result, int count, Part a, Part b, Part c, Predicate<ItemStack> unlockedBy) {
        return new StationRecipe(result, count, List.of(a, b, c), unlockedBy);
    }

    /** A one-part recipe (the Mold Bench: clay balls only). */
    public static StationRecipe of(Supplier<Item> result, int count, Part a, Predicate<ItemStack> unlockedBy) {
        return new StationRecipe(result, count, List.of(a), unlockedBy);
    }

    public String id() {
        return BuiltInRegistries.ITEM.getKey(result.get()).toString();
    }

    public ItemStack assemble() {
        return new ItemStack(result.get(), count);
    }

    /** Each input fits its part; inputs past the recipe's parts must be empty. */
    public boolean matches(ItemStack... inputs) {
        for (int i = 0; i < inputs.length; i++) {
            if (i < parts.size() ? !parts.get(i).matches(inputs[i]) : !inputs[i].isEmpty()) return false;
        }
        return true;
    }

    public boolean isUnlocked(Player player) {
        return unlockedBy == null || player.getData(Mechanical.UNLOCKED_TOOLS).contains(id());
    }

    /** The player carries every part (enough of each). */
    public boolean hasParts(Player player) {
        return parts.stream().allMatch(p -> p.has(player.getInventory()));
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
