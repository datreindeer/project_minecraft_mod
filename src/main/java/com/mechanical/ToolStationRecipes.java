package com.mechanical;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
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

        /** The parts this inventory would use: Hide Glue stands in for Cordage when there is no Cordage. */
        public List<Item> parts(Inventory inventory) {
            if (!gluable() || inventory.countItem(Mechanical.CORDAGE.get()) > 0 || inventory.countItem(Kettle.HIDE_GLUE.get()) == 0) return parts();
            return List.of(head.get(), Kettle.HIDE_GLUE.get(), rod.get());
        }

        /** Hafting with Hide Glue (Kettle) instead of a Cordage lashing. */
        public boolean gluable() {
            return binding.get() == Mechanical.CORDAGE.get();
        }

        public boolean matches(ItemStack head, ItemStack binding, ItemStack rod) {
            return head.is(this.head.get()) && (binding.is(this.binding.get()) || gluable() && binding.is(Kettle.HIDE_GLUE.get())) && rod.is(this.rod.get());
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
                    s -> s.is(Mechanical.BONE_FISH_HOOK.get())),
            new Recipe(() -> Farming.FLINT_SICKLE.get(), () -> Items.FLINT, Mechanical.TWINE, () -> Items.STICK, FLINT),
            // the Broom: "head" = a bundle of Straw, bound with Twine on a Tool Rod
            new Recipe(() -> Mechanical.BROOM.get(), () -> Farming.STRAW.get(), Mechanical.TWINE, Mechanical.TOOL_ROD, s -> s.is(Farming.STRAW.get())),
            // copper: cast heads, ground on the Whetstone; unlocked by holding the ground head
            new Recipe(() -> Items.COPPER_PICKAXE, Casting.COPPER_PICKAXE_HEAD, Mechanical.CORDAGE, Mechanical.TOOL_ROD, s -> s.is(Casting.COPPER_PICKAXE_HEAD.get())),
            new Recipe(() -> Items.COPPER_AXE, Casting.COPPER_AXE_HEAD, Mechanical.CORDAGE, Mechanical.TOOL_ROD, s -> s.is(Casting.COPPER_AXE_HEAD.get())),
            new Recipe(() -> Items.COPPER_SHOVEL, Casting.COPPER_SHOVEL_HEAD, Mechanical.CORDAGE, Mechanical.TOOL_ROD, s -> s.is(Casting.COPPER_SHOVEL_HEAD.get())),
            new Recipe(() -> Items.COPPER_HOE, Casting.COPPER_HOE_HEAD, Mechanical.CORDAGE, Mechanical.TOOL_ROD, s -> s.is(Casting.COPPER_HOE_HEAD.get())),
            new Recipe(() -> Casting.COPPER_KNIFE.get(), Casting.COPPER_KNIFE_BLADE, Mechanical.TWINE, () -> Items.STICK, s -> s.is(Casting.COPPER_KNIFE_BLADE.get())),
            // bronze: like copper (a Poor Bronze head makes a tool with 70 % durability, see Bronze.inheritPoor)
            new Recipe(Bronze.BRONZE_PICKAXE, Bronze.BRONZE_PICKAXE_HEAD, Mechanical.CORDAGE, Mechanical.TOOL_ROD, s -> s.is(Bronze.BRONZE_PICKAXE_HEAD.get())),
            new Recipe(Bronze.BRONZE_AXE, Bronze.BRONZE_AXE_HEAD, Mechanical.CORDAGE, Mechanical.TOOL_ROD, s -> s.is(Bronze.BRONZE_AXE_HEAD.get())),
            new Recipe(Bronze.BRONZE_SHOVEL, Bronze.BRONZE_SHOVEL_HEAD, Mechanical.CORDAGE, Mechanical.TOOL_ROD, s -> s.is(Bronze.BRONZE_SHOVEL_HEAD.get())),
            new Recipe(Bronze.BRONZE_HOE, Bronze.BRONZE_HOE_HEAD, Mechanical.CORDAGE, Mechanical.TOOL_ROD, s -> s.is(Bronze.BRONZE_HOE_HEAD.get())),
            new Recipe(() -> Bronze.BRONZE_KNIFE.get(), Bronze.BRONZE_KNIFE_BLADE, Mechanical.TWINE, () -> Items.STICK, s -> s.is(Bronze.BRONZE_KNIFE_BLADE.get())),
            new Recipe(() -> Bronze.BRONZE_SAW.get(), Bronze.BRONZE_SAW_BLADE, Mechanical.TWINE, () -> Items.STICK, s -> s.is(Bronze.BRONZE_SAW_BLADE.get())),
            new Recipe(Bronze.BRONZE_SPEAR, Bronze.BRONZE_SPEAR_HEAD, Mechanical.CORDAGE, Mechanical.TOOL_ROD, s -> s.is(Bronze.BRONZE_SPEAR_HEAD.get())),
            // the sword's "rod" is the leather-wrapped grip (as the Tusk Dagger's)
            new Recipe(Bronze.BRONZE_SWORD, Bronze.BRONZE_SWORD_BLADE, Mechanical.CORDAGE, () -> Items.LEATHER, s -> s.is(Bronze.BRONZE_SWORD_BLADE.get())),
            // more bronze tools (BronzeTools): the bucket's "binding" is its cordage bail, the tongs' "rod" a leather grip against the heat
            new Recipe(() -> BronzeTools.BRONZE_BUCKET.get(), BronzeTools.BRONZE_BUCKET_BODY, Mechanical.CORDAGE, () -> Items.STICK, s -> s.is(BronzeTools.BRONZE_BUCKET_BODY.get())),
            new Recipe(BronzeTools.BRONZE_SICKLE, BronzeTools.BRONZE_SICKLE_BLADE, Mechanical.TWINE, () -> Items.STICK, s -> s.is(BronzeTools.BRONZE_SICKLE_BLADE.get())),
            new Recipe(() -> BronzeTools.BRONZE_HOOK_FISHING_ROD.get(), BronzeTools.BRONZE_FISH_HOOK, Mechanical.TWINE, Mechanical.TOOL_ROD, s -> s.is(BronzeTools.BRONZE_FISH_HOOK.get())),
            new Recipe(BronzeTools.BRONZE_TONGS, BronzeTools.BRONZE_TONG_ARMS, Mechanical.TWINE, () -> Items.LEATHER, s -> s.is(BronzeTools.BRONZE_TONG_ARMS.get())),
            new Recipe(() -> BronzeTools.BRONZE_CHISEL.get(), BronzeTools.BRONZE_CHISEL_BLADE, Mechanical.TWINE, () -> Items.STICK, s -> s.is(BronzeTools.BRONZE_CHISEL_BLADE.get())),
            new Recipe(Ironworking.BRONZE_SMITHING_HAMMER, Ironworking.BRONZE_HAMMER_HEAD, Mechanical.CORDAGE, Mechanical.TOOL_ROD, s -> s.is(Ironworking.BRONZE_HAMMER_HEAD.get())),
            // the quarry hammer: the same head, a Bronze Plate wrapped round the haft as its binding for the heavy blows
            new Recipe(BronzeTools.BRONZE_QUARRY_HAMMER, Ironworking.BRONZE_HAMMER_HEAD, Bronze.BRONZE_PLATE, Mechanical.TOOL_ROD, s -> s.is(Ironworking.BRONZE_HAMMER_HEAD.get())),
            // the Yew Longbow: "head" = a Sinew bowstring, "binding" = the leather grip, "rod" = the Yew Stave (unlocked by holding one)
            new Recipe(() -> BronzeTools.YEW_LONGBOW.get(), () -> com.mechanical.wildlife.Wildlife.SINEW.get(), () -> Items.LEATHER, BronzeTools.YEW_STAVE,
                    s -> s.is(BronzeTools.YEW_STAVE.get())),
            // forged iron (Smithing): vanilla's iron tools, a leather wrap on a wooden haft; each unlocks with its forged part
            new Recipe(() -> Items.IRON_PICKAXE, Smithing.IRON_PICKAXE_HEAD, () -> Items.LEATHER, Mechanical.TOOL_ROD, s -> s.is(Smithing.IRON_PICKAXE_HEAD.get())),
            new Recipe(() -> Items.IRON_AXE, Smithing.IRON_AXE_HEAD, () -> Items.LEATHER, Mechanical.TOOL_ROD, s -> s.is(Smithing.IRON_AXE_HEAD.get())),
            new Recipe(() -> Items.IRON_SHOVEL, Smithing.IRON_SHOVEL_HEAD, () -> Items.LEATHER, Mechanical.TOOL_ROD, s -> s.is(Smithing.IRON_SHOVEL_HEAD.get())),
            new Recipe(() -> Items.IRON_HOE, Smithing.IRON_HOE_HEAD, () -> Items.LEATHER, Mechanical.TOOL_ROD, s -> s.is(Smithing.IRON_HOE_HEAD.get())),
            new Recipe(() -> Mechanical.IRON_KNIFE.get(), Smithing.IRON_KNIFE_BLADE, () -> Items.LEATHER, () -> Items.STICK, s -> s.is(Smithing.IRON_KNIFE_BLADE.get())),
            new Recipe(() -> Items.IRON_SWORD, Smithing.IRON_SWORD_BLADE, Mechanical.CORDAGE, () -> Items.LEATHER, s -> s.is(Smithing.IRON_SWORD_BLADE.get())),
            new Recipe(() -> Items.IRON_SPEAR, Smithing.IRON_SPEAR_HEAD, Mechanical.CORDAGE, Mechanical.TOOL_ROD, s -> s.is(Smithing.IRON_SPEAR_HEAD.get())),
            new Recipe(Smithing.IRON_SMITHING_HAMMER, Smithing.IRON_HAMMER_HEAD, () -> Items.LEATHER, Mechanical.TOOL_ROD, s -> s.is(Smithing.IRON_HAMMER_HEAD.get())),
            // steel: forged heads with an eye on a wooden haft, locked by an Iron Tool Rod driven in as wedge and ferrule (no lashing)
            new Recipe(Smithing.STEEL_PICKAXE, Smithing.STEEL_PICKAXE_HEAD, Smithing.IRON_TOOL_ROD, Mechanical.TOOL_ROD, s -> s.is(Smithing.STEEL_PICKAXE_HEAD.get())),
            new Recipe(Smithing.STEEL_AXE, Smithing.STEEL_AXE_HEAD, Smithing.IRON_TOOL_ROD, Mechanical.TOOL_ROD, s -> s.is(Smithing.STEEL_AXE_HEAD.get())),
            new Recipe(Smithing.STEEL_SHOVEL, Smithing.STEEL_SHOVEL_HEAD, Smithing.IRON_TOOL_ROD, Mechanical.TOOL_ROD, s -> s.is(Smithing.STEEL_SHOVEL_HEAD.get())),
            new Recipe(Smithing.STEEL_HOE, Smithing.STEEL_HOE_HEAD, Smithing.IRON_TOOL_ROD, Mechanical.TOOL_ROD, s -> s.is(Smithing.STEEL_HOE_HEAD.get())),
            new Recipe(() -> Smithing.STEEL_KNIFE.get(), Smithing.STEEL_KNIFE_BLADE, () -> Items.LEATHER, () -> Items.STICK, s -> s.is(Smithing.STEEL_KNIFE_BLADE.get())),
            // the sword: an Iron Tool Rod forged into guard and tang, a leather grip
            new Recipe(Smithing.STEEL_SWORD, Smithing.STEEL_SWORD_BLADE, Smithing.IRON_TOOL_ROD, () -> Items.LEATHER, s -> s.is(Smithing.STEEL_SWORD_BLADE.get())),
            new Recipe(Smithing.STEEL_SPEAR, Smithing.STEEL_SPEAR_HEAD, Smithing.IRON_TOOL_ROD, Mechanical.TOOL_ROD, s -> s.is(Smithing.STEEL_SPEAR_HEAD.get())),
            // the quarry hammer in steel: a Steel Plate round the haft for the heavy blows, the haft an Iron Tool Rod
            new Recipe(Smithing.STEEL_QUARRY_HAMMER, Smithing.STEEL_HAMMER_HEAD, Smithing.STEEL_PLATE, Smithing.IRON_TOOL_ROD, s -> s.is(Smithing.STEEL_HAMMER_HEAD.get())),
            // forged saws: the blade riveted into a wooden back, a leather-wrapped grip
            new Recipe(() -> Smithing.IRON_SAW.get(), Smithing.IRON_SAW_BLADE, () -> Items.LEATHER, () -> Items.STICK, s -> s.is(Smithing.IRON_SAW_BLADE.get())),
            new Recipe(() -> Smithing.STEEL_SAW.get(), Smithing.STEEL_SAW_BLADE, () -> Items.LEATHER, () -> Items.STICK, s -> s.is(Smithing.STEEL_SAW_BLADE.get())),
            // forged versions of the bronze tools: small blades on a leather-wrapped wooden handle, as the forged knives
            new Recipe(Smithing.IRON_SICKLE, Smithing.IRON_SICKLE_BLADE, () -> Items.LEATHER, () -> Items.STICK, s -> s.is(Smithing.IRON_SICKLE_BLADE.get())),
            new Recipe(Smithing.STEEL_SICKLE, Smithing.STEEL_SICKLE_BLADE, () -> Items.LEATHER, () -> Items.STICK, s -> s.is(Smithing.STEEL_SICKLE_BLADE.get())),
            new Recipe(() -> Smithing.IRON_CHISEL.get(), Smithing.IRON_CHISEL_BLADE, () -> Items.LEATHER, () -> Items.STICK, s -> s.is(Smithing.IRON_CHISEL_BLADE.get())),
            new Recipe(() -> Smithing.STEEL_CHISEL.get(), Smithing.STEEL_CHISEL_BLADE, () -> Items.LEATHER, () -> Items.STICK, s -> s.is(Smithing.STEEL_CHISEL_BLADE.get())),
            // the tongs: forged arms riveted with an Iron Tool Rod pin, a leather grip against the heat
            new Recipe(Smithing.IRON_TONGS, Smithing.IRON_TONG_ARMS, Smithing.IRON_TOOL_ROD, () -> Items.LEATHER, s -> s.is(Smithing.IRON_TONG_ARMS.get())),
            new Recipe(() -> Smithing.IRON_HOOK_FISHING_ROD.get(), Smithing.IRON_FISH_HOOK, Mechanical.TWINE, Mechanical.TOOL_ROD, s -> s.is(Smithing.IRON_FISH_HOOK.get())),
            // hammers: the iron quarry hammer as the bronze one (an Iron Plate round the haft), the steel smithing hammer as the steel tools
            new Recipe(Smithing.IRON_QUARRY_HAMMER, Smithing.IRON_HAMMER_HEAD, Smithing.IRON_PLATE, Mechanical.TOOL_ROD, s -> s.is(Smithing.IRON_HAMMER_HEAD.get())),
            new Recipe(Smithing.STEEL_SMITHING_HAMMER, Smithing.STEEL_HAMMER_HEAD, Smithing.IRON_TOOL_ROD, Mechanical.TOOL_ROD, s -> s.is(Smithing.STEEL_HAMMER_HEAD.get())));

    public static boolean isUnlocked(Player player, Recipe recipe) {
        return player.getData(Mechanical.UNLOCKED_TOOLS).contains(recipe.id());
    }

    /** True if the player's inventory holds every part of the recipe. */
    public static boolean hasParts(Player player, Recipe recipe) {
        return recipe.parts(player.getInventory()).stream().allMatch(part -> player.getInventory().countItem(part) > 0); // by item: Poor Bronze parts count too
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
