package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.StationRecipe.Part.of;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The Mold Bench: every unfired clay mold (and the Unfired Crucible) is shaped here from Clay Balls instead of in the
 * crafting grid, where their clay-ball patterns kept colliding. One input slot; pick the mold from the list. The
 * recipes are always unlocked (the bench itself unlocks with Clay Balls). Data: scripts/gen_moldbench.py, textures:
 * scripts/gen_moldbench_textures.py.
 */
public final class MoldBench {
    public static final DeferredBlock<MoldBenchBlock> MOLD_BENCH = BLOCKS.registerBlock("mold_bench", MoldBenchBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredItem<BlockItem> MOLD_BENCH_ITEM = ITEMS.registerSimpleBlockItem("mold_bench", MOLD_BENCH);
    public static final Supplier<MenuType<Menu>> MENU = Mechanical.MENUS.register("mold_bench",
            () -> new MenuType<>(Menu::new, FeatureFlags.VANILLA_SET));
    /** Mold Chest: storage for molds only ({@link MoldChestBlock}). */
    public static final DeferredBlock<MoldChestBlock> MOLD_CHEST = BLOCKS.registerBlock("mold_chest", MoldChestBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredItem<BlockItem> MOLD_CHEST_ITEM = ITEMS.registerSimpleBlockItem("mold_chest", MOLD_CHEST);
    public static final Supplier<BlockEntityType<MoldChestBlock.Entity>> MOLD_CHEST_ENTITY = Mechanical.BLOCK_ENTITIES.register("mold_chest",
            () -> new BlockEntityType<>(MoldChestBlock.Entity::new, MOLD_CHEST.get()));
    public static final Supplier<MenuType<MoldChestBlock.Menu>> MOLD_CHEST_MENU = Mechanical.MENUS.register("mold_chest",
            () -> new MenuType<>(MoldChestBlock.Menu::new, FeatureFlags.VANILLA_SET));

    /** Clay Balls, costs as the old crafting recipes (gen_registry.py reads these calls); the kettle mold took 7 Clay blocks, so 28 balls.
     *  Then one soapstone mold per shape from Polished Soapstone (StoneCrafts.moldRecipes). */
    public static final List<StationRecipe> ALL = java.util.stream.Stream.concat(java.util.stream.Stream.of(
            StationRecipe.of(Mechanical.UNFIRED_CRUCIBLE::get, 1, of(() -> Items.CLAY_BALL, 7), null),
            StationRecipe.of(Casting.UNFIRED_CLAY_INGOT_MOLD::get, 1, of(() -> Items.CLAY_BALL, 5), null),
            StationRecipe.of(Casting.UNFIRED_CLAY_PICKAXE_HEAD_MOLD::get, 1, of(() -> Items.CLAY_BALL, 7), null),
            StationRecipe.of(Casting.UNFIRED_CLAY_AXE_HEAD_MOLD::get, 1, of(() -> Items.CLAY_BALL, 7), null),
            StationRecipe.of(Casting.UNFIRED_CLAY_SHOVEL_HEAD_MOLD::get, 1, of(() -> Items.CLAY_BALL, 5), null),
            StationRecipe.of(Casting.UNFIRED_CLAY_HOE_HEAD_MOLD::get, 1, of(() -> Items.CLAY_BALL, 6), null),
            StationRecipe.of(Casting.UNFIRED_CLAY_KNIFE_BLADE_MOLD::get, 1, of(() -> Items.CLAY_BALL, 4), null),
            StationRecipe.of(Casting.UNFIRED_CLAY_TOOL_ROD_MOLD::get, 1, of(() -> Items.CLAY_BALL, 4), null),
            StationRecipe.of(Bronze.UNFIRED_CLAY_SAW_BLADE_MOLD::get, 1, of(() -> Items.CLAY_BALL, 5), null),
            StationRecipe.of(Bronze.UNFIRED_CLAY_PLATE_MOLD::get, 1, of(() -> Items.CLAY_BALL, 8), null),
            StationRecipe.of(Bronze.UNFIRED_CLAY_SPEAR_HEAD_MOLD::get, 1, of(() -> Items.CLAY_BALL, 4), null),
            StationRecipe.of(Bronze.UNFIRED_CLAY_SWORD_BLADE_MOLD::get, 1, of(() -> Items.CLAY_BALL, 6), null),
            StationRecipe.of(Bronze.UNFIRED_CLAY_KETTLE_MOLD::get, 1, of(() -> Items.CLAY_BALL, 28), null),
            StationRecipe.of(Bronze.UNFIRED_CLAY_ARROWHEADS_MOLD::get, 1, of(() -> Items.CLAY_BALL, 3), null),
            StationRecipe.of(Bronze.UNFIRED_CLAY_BOLT_HEADS_MOLD::get, 1, of(() -> Items.CLAY_BALL, 3), null),
            StationRecipe.of(BronzeTools.UNFIRED_CLAY_BUCKET_MOLD::get, 1, of(() -> Items.CLAY_BALL, 8), null),
            StationRecipe.of(BronzeTools.UNFIRED_CLAY_SICKLE_BLADE_MOLD::get, 1, of(() -> Items.CLAY_BALL, 5), null),
            StationRecipe.of(BronzeTools.UNFIRED_CLAY_SHEARS_MOLD::get, 1, of(() -> Items.CLAY_BALL, 5), null),
            StationRecipe.of(BronzeTools.UNFIRED_CLAY_FISH_HOOKS_MOLD::get, 1, of(() -> Items.CLAY_BALL, 3), null),
            StationRecipe.of(BronzeTools.UNFIRED_CLAY_NEEDLES_MOLD::get, 1, of(() -> Items.CLAY_BALL, 3), null),
            StationRecipe.of(BronzeTools.UNFIRED_CLAY_TONGS_MOLD::get, 1, of(() -> Items.CLAY_BALL, 6), null),
            StationRecipe.of(BronzeTools.UNFIRED_CLAY_CHISEL_BLADE_MOLD::get, 1, of(() -> Items.CLAY_BALL, 4), null),
            StationRecipe.of(Ironworking.UNFIRED_CLAY_HAMMER_HEAD_MOLD::get, 1, of(() -> Items.CLAY_BALL, 5), null),
            StationRecipe.of(Ironworking.UNFIRED_CLAY_ANVIL_MOLD::get, 1, of(() -> Items.CLAY_BALL, 32), null),
            StationRecipe.of(HighFurnace.UNFIRED_CLAY_TUYERE_MOLD::get, 1, of(() -> Items.CLAY_BALL, 5), null)),
            StoneCrafts.moldRecipes()).toList(); // then the soapstone molds, carved from Polished Soapstone

    public static void init() {}

    /** One clay slot; several molds take the same number of balls, so the one picked in the list wins. */
    public static class Menu extends RecipeStationMenu {
        public Menu(int containerId, Inventory inventory) {
            this(containerId, inventory, ContainerLevelAccess.NULL);
        }

        public Menu(int containerId, Inventory inventory, ContainerLevelAccess access) {
            super(MENU.get(), containerId, inventory, access, MOLD_BENCH, ALL, 1);
        }
    }

    private MoldBench() {}
}
