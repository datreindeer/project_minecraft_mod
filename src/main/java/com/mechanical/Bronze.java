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
    /** Iron speed and damage, durability between copper (190) and iron (250). */
    public static final ToolMaterial BRONZE = new ToolMaterial(INCORRECT_FOR_BRONZE_TOOL, 230, 6.0F, 2.0F, 12, BRONZE_TOOL_MATERIALS);
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

    // --- castings, rough and ground ---
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_PICKAXE_HEAD = ITEMS.registerSimpleItem("rough_cast_bronze_pickaxe_head");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_AXE_HEAD = ITEMS.registerSimpleItem("rough_cast_bronze_axe_head");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_SHOVEL_HEAD = ITEMS.registerSimpleItem("rough_cast_bronze_shovel_head");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_HOE_HEAD = ITEMS.registerSimpleItem("rough_cast_bronze_hoe_head");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_KNIFE_BLADE = ITEMS.registerSimpleItem("rough_cast_bronze_knife_blade");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_SAW_BLADE = ITEMS.registerSimpleItem("rough_cast_bronze_saw_blade");
    public static final DeferredItem<Item> ROUGH_CAST_BRONZE_PLATE = ITEMS.registerSimpleItem("rough_cast_bronze_plate");
    public static final DeferredItem<Item> BRONZE_PICKAXE_HEAD = ITEMS.registerSimpleItem("bronze_pickaxe_head");
    public static final DeferredItem<Item> BRONZE_AXE_HEAD = ITEMS.registerSimpleItem("bronze_axe_head");
    public static final DeferredItem<Item> BRONZE_SHOVEL_HEAD = ITEMS.registerSimpleItem("bronze_shovel_head");
    public static final DeferredItem<Item> BRONZE_HOE_HEAD = ITEMS.registerSimpleItem("bronze_hoe_head");
    public static final DeferredItem<Item> BRONZE_KNIFE_BLADE = ITEMS.registerSimpleItem("bronze_knife_blade");
    public static final DeferredItem<Item> BRONZE_SAW_BLADE = ITEMS.registerSimpleItem("bronze_saw_blade");
    public static final DeferredItem<Item> BRONZE_PLATE = ITEMS.registerSimpleItem("bronze_plate");

    // --- tools (attack values as vanilla iron) ---
    public static final DeferredItem<Item> BRONZE_PICKAXE = ITEMS.registerSimpleItem("bronze_pickaxe", p -> p.pickaxe(BRONZE, 1.0F, -2.8F));
    public static final DeferredItem<Item> BRONZE_AXE = ITEMS.registerSimpleItem("bronze_axe", p -> p.axe(BRONZE, 6.0F, -3.1F));
    public static final DeferredItem<Item> BRONZE_SHOVEL = ITEMS.registerSimpleItem("bronze_shovel", p -> p.shovel(BRONZE, 1.5F, -3.0F));
    public static final DeferredItem<Item> BRONZE_HOE = ITEMS.registerSimpleItem("bronze_hoe", p -> p.hoe(BRONZE, -2.0F, -1.0F));
    /** Butchers 1.75x as fast, 1.35x the yield, and is tough enough for the Brown Bear. */
    public static final DeferredItem<KnifeItem> BRONZE_KNIFE = ITEMS.registerItem("bronze_knife", p -> new KnifeItem(p, 1.75F, 1.35F, KnifeItem.BRONZE),
            p -> p.sword(BRONZE, 1.5F, -2.0F).durability(350));
    /** Log + saw in the crafting grid = 4 planks; the saw stays and loses 1 (same remainder rule as the Flint Pickaxe). */
    public static final DeferredItem<FlintPickaxeItem> BRONZE_SAW = ITEMS.registerItem("bronze_saw", FlintPickaxeItem::new, p -> p.durability(240));

    public static final DeferredItem<Item> BRONZE_HELMET = ITEMS.registerSimpleItem("bronze_helmet", p -> p.humanoidArmor(BRONZE_ARMOR, ArmorType.HELMET));
    public static final DeferredItem<Item> BRONZE_GREAVES = ITEMS.registerSimpleItem("bronze_greaves", p -> p.humanoidArmor(BRONZE_ARMOR, ArmorType.LEGGINGS));

    /** Rough cast -> what the Whetstone grinds it into. */
    private static final Map<Supplier<Item>, Supplier<Item>> GROUND = Map.of(
            ROUGH_CAST_BRONZE_PICKAXE_HEAD, BRONZE_PICKAXE_HEAD, ROUGH_CAST_BRONZE_AXE_HEAD, BRONZE_AXE_HEAD,
            ROUGH_CAST_BRONZE_SHOVEL_HEAD, BRONZE_SHOVEL_HEAD, ROUGH_CAST_BRONZE_HOE_HEAD, BRONZE_HOE_HEAD,
            ROUGH_CAST_BRONZE_KNIFE_BLADE, BRONZE_KNIFE_BLADE, ROUGH_CAST_BRONZE_SAW_BLADE, BRONZE_SAW_BLADE,
            ROUGH_CAST_BRONZE_PLATE, BRONZE_PLATE);

    /** The ground piece for a rough bronze casting (Poor Bronze stays poor), or empty. */
    static ItemStack ground(ItemStack rough) {
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
            if (isPoor(event.getItemStack()) && !event.getToolTip().isEmpty())
                event.getToolTip().add(1, Component.translatable("tooltip.mechanical.poor_bronze").withStyle(ChatFormatting.YELLOW));
        }

        private Tooltip() {}
    }

    public static void init() {
        CrucibleContents.selfCheck();
    }

    private Bronze() {}
}
