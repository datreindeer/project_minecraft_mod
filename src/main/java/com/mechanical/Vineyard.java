package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import net.minecraft.resources.Identifier;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Flora 4 (docs/flora-and-food-plan.md): the wild grape vine ({@link GrapeVineBlock}) and what grapes become: Raisins on
 * the Drying Rack (DryingRackBlockEntity.TABLE), Wine in the Fermenting Barrel (Kitchen.DISHES, Tipsy like cider and ale).
 * Data: scripts/gen_flora.py, textures: scripts/gen_vines_textures.py.
 */
public final class Vineyard {
    static final Identifier PICK_GRAPES = Identifier.fromNamespaceAndPath(MODID, "food/pick_grapes");

    public static final DeferredItem<Item> GRAPES = ITEMS.registerSimpleItem("grapes",
            p -> p.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build()));
    public static final DeferredBlock<GrapeVineBlock> GRAPE_VINE = BLOCKS.registerBlock("grape_vine", p -> new GrapeVineBlock(GRAPES, p), p -> p
            .mapColor(MapColor.PLANT).replaceable().noCollision().randomTicks().strength(0.3F).sound(SoundType.VINE).ignitedByLava()
            .pushReaction(PushReaction.POPPED));
    public static final DeferredItem<BlockItem> GRAPE_VINE_ITEM = ITEMS.registerSimpleBlockItem("grape_vine", GRAPE_VINE);
    public static final DeferredItem<Item> RAISINS = ITEMS.registerSimpleItem("raisins",
            p -> p.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.5F).build()));
    public static final DeferredItem<Item> WINE = ITEMS.registerSimpleItem("wine", p -> p.stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.6F).alwaysEdible().build(), Consumables.defaultDrink().build()));

    public static void init() {}

    private Vineyard() {}
}
