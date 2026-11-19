package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.Map;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Bronze Age phases 2-3 for copper: Bellows and Clay Tuyere (kiln heat, see {@link KilnBlockEntity}), melting ore in
 * the Crucible ({@link CrucibleItem}), Clay Molds ({@link ClayMoldBlock}), grinding Rough Cast heads on the
 * {@link WhetstoneBlock}, and the copper tools (vanilla copper pickaxe/axe/shovel/hoe from the Tool Station, plus our
 * Copper Knife). Data: scripts/gen_copper.py, textures: scripts/gen_copper_textures.py.
 */
public final class Casting {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CrucibleContents>> CRUCIBLE_CONTENTS = COMPONENTS.registerComponentType(
            "crucible_contents", b -> b.persistent(CrucibleContents.CODEC).networkSynchronized(CrucibleContents.STREAM_CODEC));
    /** Game time when a Crucible of Molten Metal sets. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> COOLS_AT = COMPONENTS.registerComponentType(
            "cools_at", b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    // --- heat ---
    public static final DeferredItem<Item> UNFIRED_CLAY_TUYERE = ITEMS.registerSimpleItem("unfired_clay_tuyere");
    public static final DeferredItem<Item> CLAY_TUYERE = ITEMS.registerSimpleItem("clay_tuyere");
    public static final DeferredBlock<BellowsBlock> BELLOWS = BLOCKS.registerBlock("bellows", BellowsBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> BELLOWS_ITEM = ITEMS.registerSimpleBlockItem("bellows", BELLOWS);

    // --- casting ---
    public static final DeferredItem<CrucibleItem> MOLTEN_CRUCIBLE = ITEMS.registerItem("molten_crucible", p -> new CrucibleItem(p, CrucibleItem.Kind.MOLTEN),
            p -> p.durability(CrucibleItem.COOL_SECONDS));
    public static final DeferredItem<CrucibleItem> SOLID_CRUCIBLE = ITEMS.registerItem("solid_crucible", p -> new CrucibleItem(p, CrucibleItem.Kind.SOLID),
            p -> p.stacksTo(1));

    public static final DeferredItem<Item> UNFIRED_CLAY_INGOT_MOLD = ITEMS.registerSimpleItem("unfired_clay_ingot_mold");
    public static final DeferredItem<Item> UNFIRED_CLAY_PICKAXE_HEAD_MOLD = ITEMS.registerSimpleItem("unfired_clay_pickaxe_head_mold");
    public static final DeferredItem<Item> UNFIRED_CLAY_AXE_HEAD_MOLD = ITEMS.registerSimpleItem("unfired_clay_axe_head_mold");
    public static final DeferredItem<Item> UNFIRED_CLAY_SHOVEL_HEAD_MOLD = ITEMS.registerSimpleItem("unfired_clay_shovel_head_mold");
    public static final DeferredItem<Item> UNFIRED_CLAY_HOE_HEAD_MOLD = ITEMS.registerSimpleItem("unfired_clay_hoe_head_mold");
    public static final DeferredItem<Item> UNFIRED_CLAY_KNIFE_BLADE_MOLD = ITEMS.registerSimpleItem("unfired_clay_knife_blade_mold");

    public static final DeferredBlock<ClayMoldBlock> CLAY_INGOT_MOLD = mold("clay_ingot_mold", ClayMoldBlock.Shape.INGOT);
    public static final DeferredBlock<ClayMoldBlock> CLAY_PICKAXE_HEAD_MOLD = mold("clay_pickaxe_head_mold", ClayMoldBlock.Shape.PICKAXE_HEAD);
    public static final DeferredBlock<ClayMoldBlock> CLAY_AXE_HEAD_MOLD = mold("clay_axe_head_mold", ClayMoldBlock.Shape.AXE_HEAD);
    public static final DeferredBlock<ClayMoldBlock> CLAY_SHOVEL_HEAD_MOLD = mold("clay_shovel_head_mold", ClayMoldBlock.Shape.SHOVEL_HEAD);
    public static final DeferredBlock<ClayMoldBlock> CLAY_HOE_HEAD_MOLD = mold("clay_hoe_head_mold", ClayMoldBlock.Shape.HOE_HEAD);
    public static final DeferredBlock<ClayMoldBlock> CLAY_KNIFE_BLADE_MOLD = mold("clay_knife_blade_mold", ClayMoldBlock.Shape.KNIFE_BLADE);
    public static final DeferredItem<BlockItem> CLAY_INGOT_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_ingot_mold", CLAY_INGOT_MOLD);
    public static final DeferredItem<BlockItem> CLAY_PICKAXE_HEAD_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_pickaxe_head_mold", CLAY_PICKAXE_HEAD_MOLD);
    public static final DeferredItem<BlockItem> CLAY_AXE_HEAD_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_axe_head_mold", CLAY_AXE_HEAD_MOLD);
    public static final DeferredItem<BlockItem> CLAY_SHOVEL_HEAD_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_shovel_head_mold", CLAY_SHOVEL_HEAD_MOLD);
    public static final DeferredItem<BlockItem> CLAY_HOE_HEAD_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_hoe_head_mold", CLAY_HOE_HEAD_MOLD);
    public static final DeferredItem<BlockItem> CLAY_KNIFE_BLADE_MOLD_ITEM = ITEMS.registerSimpleBlockItem("clay_knife_blade_mold", CLAY_KNIFE_BLADE_MOLD);

    // cast heads come out rough and are ground on the Whetstone before the Tool Station takes them
    public static final DeferredItem<Item> ROUGH_CAST_COPPER_PICKAXE_HEAD = ITEMS.registerSimpleItem("rough_cast_copper_pickaxe_head");
    public static final DeferredItem<Item> ROUGH_CAST_COPPER_AXE_HEAD = ITEMS.registerSimpleItem("rough_cast_copper_axe_head");
    public static final DeferredItem<Item> ROUGH_CAST_COPPER_SHOVEL_HEAD = ITEMS.registerSimpleItem("rough_cast_copper_shovel_head");
    public static final DeferredItem<Item> ROUGH_CAST_COPPER_HOE_HEAD = ITEMS.registerSimpleItem("rough_cast_copper_hoe_head");
    public static final DeferredItem<Item> ROUGH_CAST_COPPER_KNIFE_BLADE = ITEMS.registerSimpleItem("rough_cast_copper_knife_blade");
    public static final DeferredItem<Item> COPPER_PICKAXE_HEAD = ITEMS.registerSimpleItem("copper_pickaxe_head");
    public static final DeferredItem<Item> COPPER_AXE_HEAD = ITEMS.registerSimpleItem("copper_axe_head");
    public static final DeferredItem<Item> COPPER_SHOVEL_HEAD = ITEMS.registerSimpleItem("copper_shovel_head");
    public static final DeferredItem<Item> COPPER_HOE_HEAD = ITEMS.registerSimpleItem("copper_hoe_head");
    public static final DeferredItem<Item> COPPER_KNIFE_BLADE = ITEMS.registerSimpleItem("copper_knife_blade");

    public static final DeferredBlock<WhetstoneBlock> WHETSTONE = BLOCKS.registerBlock("whetstone", WhetstoneBlock::new, p -> p
            .mapColor(MapColor.SAND).strength(1.5F).sound(SoundType.STONE).noOcclusion());
    public static final DeferredItem<BlockItem> WHETSTONE_ITEM = ITEMS.registerSimpleBlockItem("whetstone", WHETSTONE);

    // --- copper tools: the pickaxe/axe/shovel/hoe are vanilla's copper tools (mine like stone here, see the
    // incorrect_for_copper_tool tag), assembled in the Tool Station; the knife is ours ---
    public static final DeferredItem<KnifeItem> COPPER_KNIFE = ITEMS.registerItem("copper_knife", p -> new KnifeItem(p, 1.5F, 1.2F, KnifeItem.FLINT), // not tough enough for bears (the Tusk Dagger is)
            p -> p.sword(ToolMaterial.COPPER, 1.5F, -2.0F).durability(200));

    /** Rough cast item -> what the Whetstone grinds it into. */
    private static final Map<Supplier<Item>, Supplier<Item>> GROUND = Map.of(
            ROUGH_CAST_COPPER_PICKAXE_HEAD, COPPER_PICKAXE_HEAD, ROUGH_CAST_COPPER_AXE_HEAD, COPPER_AXE_HEAD,
            ROUGH_CAST_COPPER_SHOVEL_HEAD, COPPER_SHOVEL_HEAD, ROUGH_CAST_COPPER_HOE_HEAD, COPPER_HOE_HEAD,
            ROUGH_CAST_COPPER_KNIFE_BLADE, COPPER_KNIFE_BLADE);

    public record OreUnits(Metal metal, int units) {}

    /** What one ore item adds to a crucible, or null if it can't go in. A Bronze Ingot melts back into its 2 units. */
    public static OreUnits oreUnits(ItemStack stack) {
        if (stack.is(BronzeOres.CRUSHED_COPPER_ORE.get())) return new OreUnits(Metal.COPPER, 1);
        if (stack.is(BronzeOres.CRUSHED_TIN_ORE.get())) return new OreUnits(Metal.TIN, 1);
        if (stack.is(BronzeOres.COPPER_CONCENTRATE.get())) return new OreUnits(Metal.COPPER, CrucibleContents.CONCENTRATE); // washed: no gangue
        if (stack.is(BronzeOres.TIN_CONCENTRATE.get())) return new OreUnits(Metal.TIN, CrucibleContents.CONCENTRATE);
        if (stack.is(Bronze.BRONZE_INGOT.get())) return new OreUnits(Bronze.isPoor(stack) ? Metal.POOR_BRONZE : Metal.BRONZE, 2);
        if (stack.is(BronzeOres.NATIVE_COPPER.get())) return new OreUnits(Metal.COPPER, 2); // nearly pure metal
        return null;
    }

    /** The ground head for a rough cast one (keeping Poor Bronze), or empty. */
    public static ItemStack ground(ItemStack rough) {
        for (var e : GROUND.entrySet()) if (rough.is(e.getKey().get())) return new ItemStack(e.getValue().get());
        return Bronze.ground(rough);
    }

    static DeferredBlock<ClayMoldBlock> mold(String name, ClayMoldBlock.Shape shape) {
        return BLOCKS.registerBlock(name, p -> new ClayMoldBlock(p, shape), p -> p
                .mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.6F).sound(SoundType.DECORATED_POT).noOcclusion().pushReaction(PushReaction.POPPED)
                .lightLevel(s -> s.getValue(ClayMoldBlock.FILL) == ClayMoldBlock.Fill.MOLTEN ? 9 : 0));
    }

    public static void init() {}

    private Casting() {}
}
