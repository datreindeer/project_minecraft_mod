package com.mechanical;

import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.function.Supplier;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Iron Age smithing (docs/ages/03-iron-age.md): iron is forged, not cast. A Wrought Iron Ingot heated to forging heat
 * (1100°, Bloomery or Clay Kiln) is a Hot Wrought Iron Ingot; on the Bronze Anvil the smithing hammer works it into the
 * part the anvil is set to ({@link Pattern}, sneak-use the anvil with a hammer to choose), {@link Pattern#hits} blows,
 * before it cools (it keeps its blows). The parts make vanilla's iron tools and armour at the Tool Station and Armor
 * Station; vanilla's Iron Ingot recipes stay off.
 * <p>
 * Steel the same way: Blister Steel (cementation) worked hot {@link #BLISTER_HITS} blows into a Steel Ingot, hot Steel
 * Ingots into steel parts, steel tools a tier above iron.
 */
@EventBusSubscriber(modid = MODID)
public final class Smithing {
    // ---- hot metal (HotMetalItem): the bar is the heat left
    public static final DeferredItem<HotMetalItem> HOT_WROUGHT_IRON_INGOT = hot("hot_wrought_iron_ingot", Ironworking.WROUGHT_IRON_INGOT);
    /** Wrought iron cemented with charcoal and soot: carbon in its skin, still full of slag streaks. */
    public static final DeferredItem<Item> BLISTER_STEEL = ITEMS.registerSimpleItem("blister_steel");
    public static final DeferredItem<HotMetalItem> HOT_BLISTER_STEEL = hot("hot_blister_steel", BLISTER_STEEL);
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    public static final DeferredItem<HotMetalItem> HOT_STEEL_INGOT = hot("hot_steel_ingot", STEEL_INGOT);
    /** Blows that fold hot Blister Steel into an even Steel Ingot. */
    public static final int BLISTER_HITS = 6;

    // ---- forged parts
    public static final DeferredItem<Item> IRON_PLATE = ITEMS.registerSimpleItem("iron_plate");
    public static final DeferredItem<Item> IRON_TOOL_ROD = ITEMS.registerSimpleItem("iron_tool_rod");
    public static final DeferredItem<Item> IRON_PICKAXE_HEAD = ITEMS.registerSimpleItem("iron_pickaxe_head");
    public static final DeferredItem<Item> IRON_AXE_HEAD = ITEMS.registerSimpleItem("iron_axe_head");
    public static final DeferredItem<Item> IRON_SHOVEL_HEAD = ITEMS.registerSimpleItem("iron_shovel_head");
    public static final DeferredItem<Item> IRON_HOE_HEAD = ITEMS.registerSimpleItem("iron_hoe_head");
    public static final DeferredItem<Item> IRON_KNIFE_BLADE = ITEMS.registerSimpleItem("iron_knife_blade");
    public static final DeferredItem<Item> IRON_SWORD_BLADE = ITEMS.registerSimpleItem("iron_sword_blade");
    public static final DeferredItem<Item> IRON_SPEAR_HEAD = ITEMS.registerSimpleItem("iron_spear_head");
    public static final DeferredItem<Item> IRON_HAMMER_HEAD = ITEMS.registerSimpleItem("iron_hammer_head");
    public static final DeferredItem<Item> STEEL_PLATE = ITEMS.registerSimpleItem("steel_plate");
    public static final DeferredItem<Item> STEEL_PICKAXE_HEAD = ITEMS.registerSimpleItem("steel_pickaxe_head");
    public static final DeferredItem<Item> STEEL_AXE_HEAD = ITEMS.registerSimpleItem("steel_axe_head");
    public static final DeferredItem<Item> STEEL_SHOVEL_HEAD = ITEMS.registerSimpleItem("steel_shovel_head");
    public static final DeferredItem<Item> STEEL_HOE_HEAD = ITEMS.registerSimpleItem("steel_hoe_head");
    public static final DeferredItem<Item> STEEL_KNIFE_BLADE = ITEMS.registerSimpleItem("steel_knife_blade");
    public static final DeferredItem<Item> STEEL_SWORD_BLADE = ITEMS.registerSimpleItem("steel_sword_blade");
    public static final DeferredItem<Item> STEEL_SPEAR_HEAD = ITEMS.registerSimpleItem("steel_spear_head");

    /** Iron wears less than bronze: 1 a blow instead of {@link Ironworking#HAMMER_WEAR}. */
    public static final DeferredItem<Item> IRON_SMITHING_HAMMER = ITEMS.registerSimpleItem("iron_smithing_hammer", p -> p.durability(500));

    // ---- steel tools: diamond's reach (obsidian), more durable, faster and harder-hitting than iron
    public static final TagKey<Item> STEEL_TOOL_MATERIALS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "steel_tool_materials"));
    public static final ToolMaterial STEEL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1100, 7.5F, 2.5F, 12, STEEL_TOOL_MATERIALS);
    public static final DeferredItem<Item> STEEL_PICKAXE = ITEMS.registerSimpleItem("steel_pickaxe", p -> p.pickaxe(STEEL, 1.0F, -2.8F));
    public static final DeferredItem<Item> STEEL_AXE = ITEMS.registerSimpleItem("steel_axe", p -> p.axe(STEEL, 6.0F, -3.1F));
    public static final DeferredItem<Item> STEEL_SHOVEL = ITEMS.registerSimpleItem("steel_shovel", p -> p.shovel(STEEL, 1.5F, -3.0F));
    public static final DeferredItem<Item> STEEL_HOE = ITEMS.registerSimpleItem("steel_hoe", p -> p.hoe(STEEL, -2.5F, 0.0F));
    public static final DeferredItem<Item> STEEL_SWORD = ITEMS.registerSimpleItem("steel_sword", p -> p.sword(STEEL, 3.0F, -2.4F));
    public static final DeferredItem<Item> STEEL_SPEAR = ITEMS.registerSimpleItem("steel_spear",
            p -> p.spear(STEEL, 1.0F, 1.05F, 0.55F, 2.25F, 10.0F, 6.5F, 5.1F, 10.5F, 4.6F));
    public static final DeferredItem<KnifeItem> STEEL_KNIFE = ITEMS.registerItem("steel_knife", p -> new KnifeItem(p, 2.4F, 1.6F, KnifeItem.IRON),
            p -> p.sword(STEEL, 1.5F, -2.0F));

    /** Granted for the first part forged on the anvil (criterion minecraft:impossible, scripts/gen_advancements.py). */
    public static final Identifier FORGE_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "iron_age/forged");

    /** Vanilla's iron tools last longer than bronze (460): forged iron, {@value #IRON_TOOL_DURABILITY}. */
    public static final int IRON_TOOL_DURABILITY = 600;

    /** What the anvil makes of a hot ingot, and in how many blows; null where that metal has no such part. */
    public enum Pattern {
        PLATE("plate", 4, () -> IRON_PLATE.get(), () -> STEEL_PLATE.get()),
        TOOL_ROD("tool_rod", 3, () -> IRON_TOOL_ROD.get(), null),
        PICKAXE_HEAD("pickaxe_head", 6, () -> IRON_PICKAXE_HEAD.get(), () -> STEEL_PICKAXE_HEAD.get()),
        AXE_HEAD("axe_head", 6, () -> IRON_AXE_HEAD.get(), () -> STEEL_AXE_HEAD.get()),
        SHOVEL_HEAD("shovel_head", 5, () -> IRON_SHOVEL_HEAD.get(), () -> STEEL_SHOVEL_HEAD.get()),
        HOE_HEAD("hoe_head", 5, () -> IRON_HOE_HEAD.get(), () -> STEEL_HOE_HEAD.get()),
        KNIFE_BLADE("knife_blade", 5, () -> IRON_KNIFE_BLADE.get(), () -> STEEL_KNIFE_BLADE.get()),
        SWORD_BLADE("sword_blade", 7, () -> IRON_SWORD_BLADE.get(), () -> STEEL_SWORD_BLADE.get()),
        SPEAR_HEAD("spear_head", 6, () -> IRON_SPEAR_HEAD.get(), () -> STEEL_SPEAR_HEAD.get()),
        HAMMER_HEAD("hammer_head", 6, () -> IRON_HAMMER_HEAD.get(), null);

        public final String id;
        public final int hits;
        private final Supplier<Item> iron, steel;

        Pattern(String id, int hits, Supplier<Item> iron, Supplier<Item> steel) {
            this.id = id;
            this.hits = hits;
            this.iron = iron;
            this.steel = steel;
        }

        /** The part this pattern makes from {@code metal} (a hot ingot), or empty. */
        public ItemStack result(ItemStack metal) {
            Supplier<Item> part = metal.is(HOT_STEEL_INGOT.get()) ? steel : metal.is(HOT_WROUGHT_IRON_INGOT.get()) ? iron : null;
            return part == null ? ItemStack.EMPTY : new ItemStack(part.get());
        }

        public Pattern next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static DeferredItem<HotMetalItem> hot(String name, Supplier<? extends Item> cold) {
        return ITEMS.registerItem(name, p -> new HotMetalItem(p, cold), p -> p.durability(Ironworking.HOT_SECONDS));
    }

    public static boolean isHammer(ItemStack stack) {
        return stack.is(Ironworking.BRONZE_SMITHING_HAMMER.get()) || stack.is(IRON_SMITHING_HAMMER.get());
    }

    /** Durability a blow on hot iron or steel costs this hammer. */
    public static int wear(ItemStack hammer) {
        return hammer.is(IRON_SMITHING_HAMMER.get()) ? 1 : Ironworking.HAMMER_WEAR;
    }

    /** Iron and steel that go on the anvil to be forged (hot, or cold to be told it must be heated). */
    public static boolean forgeable(ItemStack stack) {
        return stack.is(Ironworking.WROUGHT_IRON_INGOT.get()) || stack.is(HOT_WROUGHT_IRON_INGOT.get()) || stack.is(BLISTER_STEEL.get())
                || stack.is(HOT_BLISTER_STEEL.get()) || stack.is(STEEL_INGOT.get()) || stack.is(HOT_STEEL_INGOT.get());
    }

    /** Vanilla's iron tools: {@link #IRON_TOOL_DURABILITY} instead of 250. */
    @SubscribeEvent
    static void ironTools(ModifyDefaultComponentsEvent event) {
        for (Item tool : new Item[] {Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_SWORD, Items.IRON_SPEAR}) {
            event.modify(tool, (components, context, item) -> components.set(DataComponents.MAX_DAMAGE, IRON_TOOL_DURABILITY));
        }
    }

    public static void init() {}

    private Smithing() {}
}
