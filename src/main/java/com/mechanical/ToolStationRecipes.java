package com.mechanical;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.mechanical.tools.ToolParts;

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
    public record Recipe(Supplier<Item> result, Supplier<Item> head, Supplier<Item> binding, Supplier<Item> rod, Predicate<ItemStack> material,
                         ToolParts.Kind kind) {
        public Recipe(Supplier<Item> result, Supplier<Item> head, Supplier<Item> binding, Supplier<Item> rod, Predicate<ItemStack> material) {
            this(result, head, binding, rod, material, ToolParts.Kind.TOOL);
        }

        public String id() {
            return BuiltInRegistries.ITEM.getKey(result.get()).toString();
        }

        /** Tools, bows, crossbows and fishing rods take any allowed parts; the broom and buckets stay as they are. */
        public boolean dynamic() {
            return kind != ToolParts.Kind.FIXED;
        }

        /** How well the placed binding and rod fit this recipe's own: both 3, binding 2, rod 1, neither 0. */
        public int score(ItemStack binding, ItemStack rod) {
            return (binding.is(this.binding.get()) ? 2 : 0) + (rod.is(this.rod.get()) ? 1 : 0);
        }

        public List<Item> parts() {
            return List.of(head.get(), binding.get(), rod.get());
        }

        /** What slot 0-2 takes: the allowed parts of a dynamic recipe, else its own part (and Hide Glue for a Cordage lashing). */
        public List<Item> options(int slot) {
            Item own = parts().get(slot);
            if (dynamic()) return ToolParts.options(kind, slot, head.get(), own);
            return slot == 1 && gluable() ? List.of(own, Kettle.HIDE_GLUE.get()) : List.of(own);
        }

        /** Whether the slot is a choice: a bow's string, binding and rod; never a tool's or fishing rod's head. */
        public boolean variable(int slot) {
            return ToolParts.variable(kind, slot);
        }

        /** The parts this inventory would use: its own where it has them, else the first allowed one it has (Hide Glue for Cordage). */
        public List<Item> parts(Inventory inventory) {
            return List.of(pick(inventory, head.get(), options(0)), pick(inventory, binding.get(), options(1)), pick(inventory, rod.get(), options(2)));
        }

        private static Item pick(Inventory inventory, Item preferred, List<Item> allowed) {
            if (inventory.countItem(preferred) > 0) return preferred;
            return allowed.stream().filter(i -> inventory.countItem(i) > 0).findFirst().orElse(preferred);
        }

        /** Hafting with Hide Glue (Kettle) instead of a Cordage lashing. */
        public boolean gluable() {
            return binding.get() == Mechanical.CORDAGE.get();
        }

        public boolean matches(ItemStack head, ItemStack binding, ItemStack rod) {
            return takesHead(head) && options(1).contains(binding.getItem()) && options(2).contains(rod.getItem());
        }

        /** Whether this head goes in: the recipe's own, or any allowed bowstring. */
        public boolean takesHead(ItemStack head) {
            return !head.isEmpty() && options(0).contains(head.getItem());
        }

        /** Writes the chosen parts (and the stats they give) on a fresh result; head is only kept where it is a choice. */
        public void apply(ItemStack tool, Item head, Item binding, Item rod, int smithing) {
            ToolParts.apply(tool, kind, parts(), new ToolParts.Parts(binding, rod, smithing, head));
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
            new Recipe(() -> Mechanical.CRUDE_BOW.get(), Mechanical.CORDAGE, Mechanical.TWINE, Mechanical.TOOL_ROD, s -> s.is(Mechanical.CORDAGE.get()), ToolParts.Kind.BOW),
            new Recipe(() -> Mechanical.CRUDE_FISHING_ROD.get(), () -> Items.FLINT, Mechanical.TWINE, Mechanical.TOOL_ROD, s -> s.is(Mechanical.TWINE.get()),
                    ToolParts.Kind.FISHING_ROD),
            new Recipe(() -> Mechanical.TUSK_DAGGER.get(), () -> com.mechanical.wildlife.Wildlife.TUSK.get(), Mechanical.CORDAGE, () -> Items.LEATHER,
                    s -> s.is(com.mechanical.wildlife.Wildlife.TUSK.get())),
            new Recipe(() -> Mechanical.BONE_HOOK_FISHING_ROD.get(), () -> Mechanical.BONE_FISH_HOOK.get(), Mechanical.TWINE, Mechanical.TOOL_ROD,
                    s -> s.is(Mechanical.BONE_FISH_HOOK.get()), ToolParts.Kind.FISHING_ROD),
            new Recipe(() -> Farming.FLINT_SICKLE.get(), () -> Items.FLINT, Mechanical.TWINE, () -> Items.STICK, FLINT),
            // the Broom: "head" = a bundle of Straw, bound with Twine on a Tool Rod
            new Recipe(() -> Mechanical.BROOM.get(), () -> Farming.STRAW.get(), Mechanical.TWINE, Mechanical.TOOL_ROD, s -> s.is(Farming.STRAW.get()), ToolParts.Kind.FIXED),
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
            new Recipe(() -> BronzeTools.BRONZE_BUCKET.get(), BronzeTools.BRONZE_BUCKET_BODY, Mechanical.CORDAGE, () -> Items.STICK, s -> s.is(BronzeTools.BRONZE_BUCKET_BODY.get()),
                    ToolParts.Kind.FIXED),
            new Recipe(BronzeTools.BRONZE_SICKLE, BronzeTools.BRONZE_SICKLE_BLADE, Mechanical.TWINE, () -> Items.STICK, s -> s.is(BronzeTools.BRONZE_SICKLE_BLADE.get())),
            new Recipe(() -> BronzeTools.BRONZE_HOOK_FISHING_ROD.get(), BronzeTools.BRONZE_FISH_HOOK, Mechanical.TWINE, Mechanical.TOOL_ROD, s -> s.is(BronzeTools.BRONZE_FISH_HOOK.get()),
                    ToolParts.Kind.FISHING_ROD),
            new Recipe(BronzeTools.BRONZE_TONGS, BronzeTools.BRONZE_TONG_ARMS, Mechanical.TWINE, () -> Items.LEATHER, s -> s.is(BronzeTools.BRONZE_TONG_ARMS.get())),
            new Recipe(() -> BronzeTools.BRONZE_CHISEL.get(), BronzeTools.BRONZE_CHISEL_BLADE, Mechanical.TWINE, () -> Items.STICK, s -> s.is(BronzeTools.BRONZE_CHISEL_BLADE.get())),
            new Recipe(Ironworking.BRONZE_SMITHING_HAMMER, Ironworking.BRONZE_HAMMER_HEAD, Mechanical.CORDAGE, Mechanical.TOOL_ROD, s -> s.is(Ironworking.BRONZE_HAMMER_HEAD.get())),
            // the quarry hammer: the same head, a Bronze Plate wrapped round the haft as its binding for the heavy blows
            new Recipe(BronzeTools.BRONZE_QUARRY_HAMMER, Ironworking.BRONZE_HAMMER_HEAD, Bronze.BRONZE_PLATE, Mechanical.TOOL_ROD, s -> s.is(Ironworking.BRONZE_HAMMER_HEAD.get())),
            // the Bronze Ard: a hoe head as its share, a Bronze Plate shoe under it, on a Tool Rod beam
            new Recipe(() -> FarmTools.BRONZE_ARD.get(), Bronze.BRONZE_HOE_HEAD, Bronze.BRONZE_PLATE, Mechanical.TOOL_ROD, s -> s.is(Bronze.BRONZE_HOE_HEAD.get())),
            // the Yew Longbow: "head" = a Sinew bowstring, "binding" = the leather grip, "rod" = the Yew Stave (unlocked by holding one)
            new Recipe(() -> BronzeTools.YEW_LONGBOW.get(), () -> com.mechanical.wildlife.Wildlife.SINEW.get(), () -> Items.LEATHER, BronzeTools.YEW_STAVE,
                    s -> s.is(BronzeTools.YEW_STAVE.get()), ToolParts.Kind.BOW),
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
            new Recipe(() -> Smithing.IRON_HOOK_FISHING_ROD.get(), Smithing.IRON_FISH_HOOK, Mechanical.TWINE, Mechanical.TOOL_ROD, s -> s.is(Smithing.IRON_FISH_HOOK.get()),
                    ToolParts.Kind.FISHING_ROD),
            // hammers: the iron quarry hammer as the bronze one (an Iron Plate round the haft), the steel smithing hammer as the steel tools
            new Recipe(Smithing.IRON_QUARRY_HAMMER, Smithing.IRON_HAMMER_HEAD, Smithing.IRON_PLATE, Mechanical.TOOL_ROD, s -> s.is(Smithing.IRON_HAMMER_HEAD.get())),
            new Recipe(Smithing.STEEL_SMITHING_HAMMER, Smithing.STEEL_HAMMER_HEAD, Smithing.IRON_TOOL_ROD, Mechanical.TOOL_ROD, s -> s.is(Smithing.STEEL_HAMMER_HEAD.get())),
            // crossbows: "head" = the prod (with its trigger), "binding" = the string, "rod" = the stock; unlocked by holding the prod.
            // The Iron Crossbow is vanilla's: Sinew on a Tool Rod stock gives exactly its stats.
            new Recipe(() -> Items.CROSSBOW, Smithing.IRON_CROSSBOW_PROD, () -> com.mechanical.wildlife.Wildlife.SINEW.get(), Mechanical.TOOL_ROD,
                    s -> s.is(Smithing.IRON_CROSSBOW_PROD.get()), ToolParts.Kind.CROSSBOW),
            new Recipe(() -> Smithing.STEEL_CROSSBOW.get(), Smithing.STEEL_CROSSBOW_PROD, () -> com.mechanical.wildlife.Wildlife.SINEW.get(), Mechanical.TOOL_ROD,
                    s -> s.is(Smithing.STEEL_CROSSBOW_PROD.get()), ToolParts.Kind.CROSSBOW));

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
