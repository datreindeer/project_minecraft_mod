package com.mechanical;

import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Bronze Age phase 3b: bronze. Copper + tin melt together in the Crucible ({@link CrucibleContents#alloy}); bronze
 * casts the Bronze Ingot, Rough Cast heads, a Saw Blade and armour Plates, ground on the Whetstone and assembled in the
 * Tool Station (tools, Bronze Saw) or Armor Station (Bronze Helmet, Bronze Greaves). Bronze from the edges of the tin
 * interval is Poor Bronze: everything cast from it carries {@link #POOR}, and the tool or armour made from it gets
 * {@link #POOR_DURABILITY} of its durability. Data: scripts/gen_bronze.py, textures: scripts/gen_bronze_textures.py.
 */
public final class Bronze {
    public static final float POOR_DURABILITY = 0.7F;
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> POOR = Casting.COMPONENTS.registerComponentType(
            "poor_bronze", b -> b.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC));

    /** Mines like stone plus #mechanical:needs_bronze_tool (iron, coal, lapis ore); not gold, diamond, redstone, emerald. */
    public static final TagKey<Block> INCORRECT_FOR_BRONZE_TOOL = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "incorrect_for_bronze_tool"));
    public static final TagKey<Item> BRONZE_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "bronze_tool_materials"));
    /** Iron speed and damage, durability 460 (copper 190, iron 250): bronze tools last. */
    public static final ToolMaterial BRONZE = new ToolMaterial(INCORRECT_FOR_BRONZE_TOOL, 460, 6.0F, 2.0F, 12, BRONZE_TOOL_MATERIALS);
    /** Iron's armour values, about 70 % of its durability (multiplier 10 vs 15). Only helmet and greaves exist. */
    public static final ArmorMaterial BRONZE_ARMOR = new ArmorMaterial(10,
            Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 6, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 2, ArmorType.BODY, 5),
            9, SoundEvents.ARMOR_EQUIP_IRON, 0.0F, 0.0F, BRONZE_TOOL_MATERIALS,
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(MODID, "bronze")));

    public static final DeferredItem<Item> BRONZE_INGOT = ITEMS.registerSimpleItem("bronze_ingot");

    // --- molds for the new shapes (see Casting for the others) ---
    public static final DeferredItem<Item> UNFIRED_CLAY_SAW_BLADE_MOLD = ITEMS.registerSimpleItem("unfired_clay_saw_blade_mold");
    public static final DeferredItem<Item> UNFIRED_CLAY_PLATE_MOLD = ITEMS.registerSimpleItem("unfired_clay_plate_mold");
    public static final DeferredBlock<ClayMoldBlock> CLAY_SAW_BLADE_MOLD = Casting.mold("clay_saw_blade_mold", ClayMoldBlock.Shape.SAW_BLADE);
    public static final DeferredBlock<ClayMoldBlock> CLAY_PLATE_MOLD = Casting.mold("clay_plate_mold", ClayMoldBlock.Shape.PLATE);
    public static final DeferredItem<BlockItem> CLAY_SAW_BLADE_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_saw_blade_mold", CLAY_SAW_BLADE_MOLD);
    public static final DeferredItem<BlockItem> CLAY_PLATE_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_plate_mold", CLAY_PLATE_MOLD);
    public static final DeferredItem<Item> UNFIRED_CLAY_SPEAR_HEAD_MOLD = ITEMS.registerSimpleItem("unfired_clay_spear_head_mold");
    public static final DeferredItem<Item> UNFIRED_CLAY_SWORD_BLADE_MOLD = ITEMS.registerSimpleItem("unfired_clay_sword_blade_mold");
    public static final DeferredBlock<ClayMoldBlock> CLAY_SPEAR_HEAD_MOLD = Casting.mold("clay_spear_head_mold", ClayMoldBlock.Shape.SPEAR_HEAD);
    public static final DeferredBlock<ClayMoldBlock> CLAY_SWORD_BLADE_MOLD = Casting.mold("clay_sword_blade_mold", ClayMoldBlock.Shape.SWORD_BLADE);
    public static final DeferredItem<BlockItem> CLAY_SPEAR_HEAD_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_spear_head_mold", CLAY_SPEAR_HEAD_MOLD);
    public static final DeferredItem<BlockItem> CLAY_SWORD_BLADE_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_sword_blade_mold", CLAY_SWORD_BLADE_MOLD);
    /** The kettle takes 8 units (a pickaxe or sword head takes 6): most of a full crucible, see {@link Kettle}. */
    public static final DeferredItem<Item> UNFIRED_CLAY_KETTLE_MOLD = ITEMS.registerSimpleItem("unfired_clay_kettle_mold");
    public static final DeferredBlock<ClayMoldBlock> CLAY_KETTLE_MOLD = Casting.mold("clay_kettle_mold", ClayMoldBlock.Shape.KETTLE);
    public static final DeferredItem<BlockItem> CLAY_KETTLE_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_kettle_mold", CLAY_KETTLE_MOLD);
    /** A gang mold: four arrowheads on one sprue (2 units), ground apart on the Whetstone into {@link #ARROWHEADS_PER_CAST}. */
    public static final DeferredItem<Item> UNFIRED_CLAY_ARROWHEADS_MOLD = ITEMS.registerSimpleItem("unfired_clay_arrowheads_mold");
    public static final DeferredBlock<ClayMoldBlock> CLAY_ARROWHEADS_MOLD = Casting.mold("clay_arrowheads_mold", ClayMoldBlock.Shape.ARROWHEADS);
    public static final DeferredItem<BlockItem> CLAY_ARROWHEADS_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_arrowheads_mold", CLAY_ARROWHEADS_MOLD);
    public static final int ARROWHEADS_PER_CAST = 4;

    // --- castings, rough and ground ---
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_PICKAXE_HEAD = ITEMS.registerSimpleItem("rough_cast_bronze_pickaxe_head");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_AXE_HEAD = ITEMS.registerSimpleItem("rough_cast_bronze_axe_head");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_SHOVEL_HEAD = ITEMS.registerSimpleItem("rough_cast_bronze_shovel_head");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_HOE_HEAD = ITEMS.registerSimpleItem("rough_cast_bronze_hoe_head");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_KNIFE_BLADE = ITEMS.registerSimpleItem("rough_cast_bronze_knife_blade");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_SAW_BLADE = ITEMS.registerSimpleItem("rough_cast_bronze_saw_blade");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_PLATE = ITEMS.registerSimpleItem("rough_cast_bronze_plate");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_SPEAR_HEAD = ITEMS.registerSimpleItem("rough_cast_bronze_spear_head");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_SWORD_BLADE = ITEMS.registerSimpleItem("rough_cast_bronze_sword_blade");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_ARROWHEADS = ITEMS.registerSimpleItem("rough_cast_bronze_arrowheads");
    public static final DeferredItem<Item> BRONZE_ARROWHEAD = ITEMS.registerSimpleItem("bronze_arrowhead");
    /** Fletched from Bronze Arrowheads: 3.0 damage (flint 2.0), and it seldom breaks (ArrowWounds). Poor Bronze: {@link #POOR_ARROW_DAMAGE} less. */
    public static final DeferredItem<SpecialArrowItem> BRONZE_ARROW = ITEMS.registerItem("bronze_arrow", p -> new SpecialArrowItem(p, 3.0, false));
    public static final double POOR_ARROW_DAMAGE = 0.5;
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_KETTLE = ITEMS.registerSimpleItem("rough_cast_bronze_kettle", p -> p.stacksTo(1));
    public static final DeferredItem<Item> BRONZE_PICKAXE_HEAD = ITEMS.registerSimpleItem("bronze_pickaxe_head");
    public static final DeferredItem<Item> BRONZE_AXE_HEAD = ITEMS.registerSimpleItem("bronze_axe_head");
    public static final DeferredItem<Item> BRONZE_SHOVEL_HEAD = ITEMS.registerSimpleItem("bronze_shovel_head");
    public static final DeferredItem<Item> BRONZE_HOE_HEAD = ITEMS.registerSimpleItem("bronze_hoe_head");
    public static final DeferredItem<Item> BRONZE_KNIFE_BLADE = ITEMS.registerSimpleItem("bronze_knife_blade");
    public static final DeferredItem<Item> BRONZE_SAW_BLADE = ITEMS.registerSimpleItem("bronze_saw_blade");
    public static final DeferredItem<Item> BRONZE_PLATE = ITEMS.registerSimpleItem("bronze_plate");
    public static final DeferredItem<Item> BRONZE_SPEAR_HEAD = ITEMS.registerSimpleItem("bronze_spear_head");
    public static final DeferredItem<Item> BRONZE_SWORD_BLADE = ITEMS.registerSimpleItem("bronze_sword_blade");
    /** Hung in a Tripod over a fire (Kettle). A Poor Bronze kettle holds less ({@link Kettle#POOR_CAPACITY}). */
    public static final DeferredItem<Item> BRONZE_KETTLE = ITEMS.registerSimpleItem("bronze_kettle", p -> p.stacksTo(1));

    // --- tools (attack values as vanilla iron) ---
    public static final DeferredItem<Item> BRONZE_PICKAXE = ITEMS.registerSimpleItem("bronze_pickaxe", p -> p.pickaxe(BRONZE, 1.0F, -2.8F));
    public static final DeferredItem<Item> BRONZE_AXE = ITEMS.registerSimpleItem("bronze_axe", p -> p.axe(BRONZE, 6.0F, -3.1F));
    public static final DeferredItem<Item> BRONZE_SHOVEL = ITEMS.registerSimpleItem("bronze_shovel", p -> p.shovel(BRONZE, 1.5F, -3.0F));
    public static final DeferredItem<Item> BRONZE_HOE = ITEMS.registerSimpleItem("bronze_hoe", p -> p.hoe(BRONZE, -2.0F, -1.0F));
    /** Butchers 1.75x as fast, 1.35x the yield, and is tough enough for the Brown Bear. */
    public static final DeferredItem<KnifeItem> BRONZE_KNIFE = ITEMS.registerItem("bronze_knife", p -> new KnifeItem(p, 1.75F, 1.35F, KnifeItem.BRONZE),
            p -> p.sword(BRONZE, 1.5F, -2.0F).durability(700));
    /** Log + saw in the crafting grid = 4 planks; the saw stays and loses 1 (same remainder rule as the Flint Pickaxe). */
    public static final DeferredItem<SawItem> BRONZE_SAW = ITEMS.registerItem("bronze_saw", SawItem::new, p -> p.durability(480));

    /** Vanilla spear handling (charge and jab) with iron spear's timings on bronze: the Flint Spear (stone's) made better. */
    public static final DeferredItem<Item> BRONZE_SPEAR = ITEMS.registerSimpleItem("bronze_spear",
            p -> p.spear(BRONZE, 0.95F, 0.95F, 0.6F, 2.5F, 11.0F, 6.75F, 5.1F, 11.25F, 4.6F));
    /** A vanilla sword, half a point below iron's (5.5 damage), 460 durability. */
    public static final DeferredItem<Item> BRONZE_SWORD = ITEMS.registerSimpleItem("bronze_sword", p -> p.sword(BRONZE, 2.5F, -2.4F));

    public static final DeferredItem<Item> BRONZE_HELMET = ITEMS.registerSimpleItem("bronze_helmet", p -> p.humanoidArmor(BRONZE_ARMOR, ArmorType.HELMET));
    public static final DeferredItem<Item> BRONZE_GREAVES = ITEMS.registerSimpleItem("bronze_greaves", p -> p.humanoidArmor(BRONZE_ARMOR, ArmorType.LEGGINGS));

    /** Rough cast -> what the Whetstone grinds it into. */
    private static final Map<Supplier<Item>, Supplier<Item>> GROUND = Map.of(
            ROUGH_CAST_BRONZE_PICKAXE_HEAD, BRONZE_PICKAXE_HEAD, ROUGH_CAST_BRONZE_AXE_HEAD, BRONZE_AXE_HEAD,
            ROUGH_CAST_BRONZE_SHOVEL_HEAD, BRONZE_SHOVEL_HEAD, ROUGH_CAST_BRONZE_HOE_HEAD, BRONZE_HOE_HEAD,
            ROUGH_CAST_BRONZE_KNIFE_BLADE, BRONZE_KNIFE_BLADE, ROUGH_CAST_BRONZE_SAW_BLADE, BRONZE_SAW_BLADE,
            ROUGH_CAST_BRONZE_PLATE, BRONZE_PLATE, ROUGH_CAST_BRONZE_SPEAR_HEAD, BRONZE_SPEAR_HEAD,
            ROUGH_CAST_BRONZE_SWORD_BLADE, BRONZE_SWORD_BLADE, ROUGH_CAST_BRONZE_KETTLE, BRONZE_KETTLE); // Map.of's limit: 10

    /** The ground piece for a rough bronze casting (Poor Bronze stays poor), or empty. */
    static ItemStack ground(ItemStack rough) {
        if (rough.is(ROUGH_CAST_BRONZE_ARROWHEADS.get())) {
            ItemStack heads = new ItemStack(BRONZE_ARROWHEAD.get(), ARROWHEADS_PER_CAST);
            if (isPoor(rough)) markPoor(heads);
            return heads;
        }
        for (var e : GROUND.entrySet()) {
            if (!rough.is(e.getKey().get())) continue;
            ItemStack out = new ItemStack(e.getValue().get());
            if (isPoor(rough)) markPoor(out);
            return out;
        }
        return ItemStack.EMPTY;
    }

    public static boolean isPoor(ItemStack stack) {
        return stack.has(POOR.get());
    }

    /** Marks a Poor Bronze piece; a tool or armour piece also loses 30 % of its durability. */
    public static void markPoor(ItemStack stack) {
        if (isPoor(stack)) return;
        stack.set(POOR.get(), Unit.INSTANCE);
        if (stack.isDamageableItem()) stack.set(DataComponents.MAX_DAMAGE, Math.max(1, Math.round(stack.getMaxDamage() * POOR_DURABILITY)));
    }

    /** A station's result is poor if any part put into it is Poor Bronze. */
    static ItemStack inheritPoor(ItemStack result, Container inputs) {
        for (int i = 0; i < inputs.getContainerSize(); i++) {
            if (isPoor(inputs.getItem(i))) {
                markPoor(result);
                break;
            }
        }
        return result;
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    static final class Tooltip {
        @SubscribeEvent
        static void onTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (isPoor(stack) && !event.getToolTip().isEmpty()) event.getToolTip().add(1, Component.translatable(
                    stack.is(BRONZE_KETTLE.get()) || stack.is(ROUGH_CAST_BRONZE_KETTLE.get()) ? "tooltip.mechanical.poor_bronze_kettle"
                            : stack.is(BRONZE_ARROW.get()) ? "tooltip.mechanical.poor_bronze_arrow" : "tooltip.mechanical.poor_bronze",
                    Kettle.POOR_CAPACITY, Kettle.CAPACITY).withStyle(ChatFormatting.YELLOW));
        }

        private Tooltip() {}
    }

    public static void init() {
        CrucibleContents.selfCheck();
    }

    private Bronze() {}
}
