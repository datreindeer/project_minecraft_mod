package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.mechanical.skills.Skill;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Gold and jewellery (docs/living-world-plan.md F10, end of the Iron Age). The Clay Kiln still melts gold (Raw Gold or 9
 * Gold Nuggets -> Gold Ingot at 1100°). The <b>Steel Casting Table</b> is the casting multiblock: a steel table that must
 * stand beside a Clay Kiln at {@link IronOres#GOLD_TEMP}° or more; a Gold Ingot laid on it melts from the kiln's heat into
 * its mould and sets as two Gold Ring Blanks or one Gold Chain (empty hand switches the mould). The <b>Jeweler's Bench</b>
 * sets a cut gemstone in a blank with a chisel: Gold Ring (ring slots) or Gold Necklace (neck slot). Each gem gives a
 * small bonus while worn, a Flawless one a real one ({@link #BONUSES}, table in docs/ages/03-iron-age.md): attribute
 * modifiers for the ARMOR slot group (equipment.Equipment applies them in accessory slots), or cold insulation
 * ({@link #warmth}, Temperature#insulation). Blocks: {@link JewelryTableBlock}; data: scripts/gen_p8smith.py.
 */
public final class Jewelry {
    /** The gem set in a piece: its name (as in Gemstones, "diamond", "amethyst" ...) and whether it is Flawless. */
    public record Jewel(String gem, boolean flawless) {
        public static final Codec<Jewel> CODEC = RecordCodecBuilder.create(i -> i.group(Codec.STRING.fieldOf("gem").forGetter(Jewel::gem),
                Codec.BOOL.fieldOf("flawless").forGetter(Jewel::flawless)).apply(i, Jewel::new));
        public static final StreamCodec<io.netty.buffer.ByteBuf, Jewel> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Jewel::gem,
                ByteBufCodecs.BOOL, Jewel::flawless, Jewel::new);
    }

    /** What a gem gives while worn: an attribute modifier (normal / flawless amount), or cold insulation when attribute is null. */
    public record Bonus(Holder<Attribute> attribute, AttributeModifier.Operation op, double normal, double flawless, int colour) {
        double amount(boolean f) {
            return f ? flawless : normal;
        }
    }

    private static final AttributeModifier.Operation ADD = AttributeModifier.Operation.ADD_VALUE, MUL = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
    /** Cold insulation from a warm gem (garnet, amber, citrine), as a share like an armour piece's (Temperature#insulation). */
    public static final float WARM_NORMAL = 0.05F, WARM_FLAWLESS = 0.15F;
    /** Per gem: decided 2026-11-22 (night run, may be changed): normal = small, Flawless = about four times as much. */
    public static final Map<String, Bonus> BONUSES = new LinkedHashMap<>();

    static {
        b("diamond", Attributes.ARMOR_TOUGHNESS, ADD, 0.5, 2, 0x9CF0E8);
        b("emerald", Attributes.LUCK, ADD, 0.5, 2, 0x5CE07A);
        b("amethyst", Attributes.BLOCK_BREAK_SPEED, MUL, 0.05, 0.2, 0xC89CF0);
        b("garnet", null, ADD, WARM_NORMAL, WARM_FLAWLESS, 0xC4343E);
        b("ruby", Attributes.MAX_HEALTH, ADD, 1, 4, 0xF64668);
        b("sapphire", Attributes.ARMOR, ADD, 0.5, 2, 0x5482F0);
        b("topaz", Attributes.MINING_EFFICIENCY, ADD, 1, 4, 0xFAC456);
        b("jade", Attributes.KNOCKBACK_RESISTANCE, ADD, 0.02, 0.1, 0x6EC284);
        b("opal", Attributes.ENTITY_INTERACTION_RANGE, ADD, 0.25, 1, 0xF8E4EC);
        b("turquoise", Attributes.SAFE_FALL_DISTANCE, ADD, 0.5, 2, 0x6EDCCE);
        b("peridot", Attributes.OXYGEN_BONUS, ADD, 0.5, 2, 0xB0D63C);
        b("amber", null, ADD, WARM_NORMAL, WARM_FLAWLESS, 0xECA028);
        b("clear_quartz", Attributes.BLOCK_INTERACTION_RANGE, ADD, 0.25, 1, 0xF0F4F8);
        b("rose_quartz", Attributes.SNEAKING_SPEED, ADD, 0.05, 0.2, 0xF8C8D6);
        b("citrine", null, ADD, WARM_NORMAL, WARM_FLAWLESS, 0xFCD85A);
        b("smoky_quartz", Attributes.BURNING_TIME, ADD, -0.1, -0.4, 0x928070);
        b("aquamarine", Attributes.WATER_MOVEMENT_EFFICIENCY, ADD, 0.1, 0.4, 0xA0E4EC);
        b("fluorite", Attributes.SUBMERGED_MINING_SPEED, ADD, 0.2, 0.8, 0x5AC8AA);
        b("celestine", Attributes.JUMP_STRENGTH, ADD, 0.02, 0.08, 0xBED6F6);
        b("moonstone", Attributes.STEP_HEIGHT, ADD, 0.1, 0.5, 0xE8EEFC);
        b("tourmaline", Attributes.ATTACK_SPEED, ADD, 0.1, 0.4, 0xF07896);
        b("agate", Attributes.EXPLOSION_KNOCKBACK_RESISTANCE, ADD, 0.05, 0.25, 0xE6D2BE);
        b("jasper", Attributes.ATTACK_DAMAGE, ADD, 0.25, 1, 0xC85A38);
        b("carnelian", Attributes.MOVEMENT_SPEED, MUL, 0.02, 0.1, 0xF49646);
        b("chalcedony", Attributes.MOVEMENT_EFFICIENCY, ADD, 0.05, 0.25, 0xC8D4E0);
        b("bloodstone", Attributes.MAX_HEALTH, ADD, 0.5, 2, 0x3C6E4C);
    }

    /** The skill each gem raises while worn (spec "Smycken"); garnet, amber and citrine are the builder's pick by theme. */
    public static final Map<String, Skill> SKILLS = Map.ofEntries(
            Map.entry("topaz", Skill.MINING), Map.entry("amethyst", Skill.MINING), Map.entry("fluorite", Skill.MINING),
            Map.entry("diamond", Skill.SMITHING), Map.entry("garnet", Skill.SMITHING),
            Map.entry("jade", Skill.WOODCUTTING), Map.entry("moonstone", Skill.WOODCUTTING), Map.entry("amber", Skill.WOODCUTTING),
            Map.entry("peridot", Skill.FARMING), Map.entry("chalcedony", Skill.FARMING),
            Map.entry("aquamarine", Skill.FISHING), Map.entry("turquoise", Skill.FISHING),
            Map.entry("bloodstone", Skill.SLAUGHTER), Map.entry("ruby", Skill.SLAUGHTER),
            Map.entry("jasper", Skill.HUNTING), Map.entry("tourmaline", Skill.HUNTING), Map.entry("carnelian", Skill.HUNTING),
            Map.entry("smoky_quartz", Skill.COOKING), Map.entry("citrine", Skill.COOKING),
            Map.entry("agate", Skill.MASONRY), Map.entry("sapphire", Skill.MASONRY),
            Map.entry("clear_quartz", Skill.GOLD_SMITHING), Map.entry("opal", Skill.GOLD_SMITHING), Map.entry("emerald", Skill.GOLD_SMITHING),
            Map.entry("rose_quartz", Skill.LEATHERWORKING), Map.entry("celestine", Skill.FLETCHING));

    /** The skill a worn piece raises, or null. */
    public static Skill skillOf(ItemStack stack) {
        Jewel jewel = stack.get(JEWEL.get());
        return jewel == null ? null : SKILLS.get(jewel.gem());
    }

    /** Levels a piece gives its skill: gold 3, electrum 2, silver 1, doubled when Flawless; 0 for other items. */
    public static int skillLevels(ItemStack stack) {
        Jewel jewel = stack.get(JEWEL.get());
        if (jewel == null) return 0;
        double s = strength(stack.getItem());
        return (s == 1.0 ? 3 : s == ELECTRUM_STRENGTH ? 2 : 1) * (jewel.flawless() ? 2 : 1);
    }

    private static void b(String gem, Holder<Attribute> attribute, AttributeModifier.Operation op, double normal, double flawless, int colour) {
        BONUSES.put(gem, new Bonus(attribute, op, normal, flawless, colour));
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Jewel>> JEWEL = Casting.COMPONENTS.registerComponentType(
            "jewel", b -> b.persistent(Jewel.CODEC).networkSynchronized(Jewel.STREAM_CODEC));

    public static final DeferredItem<Item> GOLD_RING_BLANK = ITEMS.registerSimpleItem("gold_ring_blank");
    public static final DeferredItem<Item> GOLD_CHAIN = ITEMS.registerSimpleItem("gold_chain");
    public static final DeferredItem<Item> GOLD_RING = ITEMS.registerSimpleItem("gold_ring", p -> p.stacksTo(1));
    public static final DeferredItem<Item> GOLD_NECKLACE = ITEMS.registerSimpleItem("gold_necklace", p -> p.stacksTo(1));
    /** The same pieces in silver (v0.211.0): cast from a Silver Ingot at silver's heat, set the same way, the gem's bonus at {@link #SILVER_STRENGTH}. */
    public static final DeferredItem<Item> SILVER_RING_BLANK = ITEMS.registerSimpleItem("silver_ring_blank");
    public static final DeferredItem<Item> SILVER_CHAIN = ITEMS.registerSimpleItem("silver_chain");
    public static final DeferredItem<Item> SILVER_RING = ITEMS.registerSimpleItem("silver_ring", p -> p.stacksTo(1));
    public static final DeferredItem<Item> SILVER_NECKLACE = ITEMS.registerSimpleItem("silver_necklace", p -> p.stacksTo(1));
    /** The same in electrum: between silver and gold. */
    public static final DeferredItem<Item> ELECTRUM_RING_BLANK = ITEMS.registerSimpleItem("electrum_ring_blank");
    public static final DeferredItem<Item> ELECTRUM_CHAIN = ITEMS.registerSimpleItem("electrum_chain");
    public static final DeferredItem<Item> ELECTRUM_RING = ITEMS.registerSimpleItem("electrum_ring", p -> p.stacksTo(1));
    public static final DeferredItem<Item> ELECTRUM_NECKLACE = ITEMS.registerSimpleItem("electrum_necklace", p -> p.stacksTo(1));
    /** A gem set in silver gives this share of what it gives in gold (the user's choice: same effects, weaker). */
    public static final double SILVER_STRENGTH = 0.5, ELECTRUM_STRENGTH = 0.75;
    /** Ticks a Gold Ingot takes to melt and set on a Steel Casting Table beside a hot kiln. */
    public static final int CAST_TICKS = 200, TABLE_MAX_INGOTS = 8;

    public static final DeferredBlock<JewelryTableBlock> STEEL_CASTING_TABLE = BLOCKS.registerBlock("steel_casting_table",
            p -> new JewelryTableBlock(p, true), p -> p.mapColor(MapColor.METAL).strength(4.0F, 6.0F).sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> STEEL_CASTING_TABLE_ITEM = ITEMS.registerSimpleBlockItem("steel_casting_table", STEEL_CASTING_TABLE);
    public static final DeferredBlock<JewelryTableBlock> JEWELERS_BENCH = BLOCKS.registerBlock("jewelers_bench",
            p -> new JewelryTableBlock(p, false), p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> JEWELERS_BENCH_ITEM = ITEMS.registerSimpleBlockItem("jewelers_bench", JEWELERS_BENCH);
    public static final Supplier<BlockEntityType<JewelryTableBlockEntity>> TABLE_ENTITY = Mechanical.BLOCK_ENTITIES.register("jewelry_table",
            () -> new BlockEntityType<>(JewelryTableBlockEntity::new, STEEL_CASTING_TABLE.get(), JEWELERS_BENCH.get()));

    public static void init() {}

    /** The gem a cut or Flawless gemstone item is (null for anything else, rough and flawed stones included). */
    public static Jewel gemOf(ItemStack stack) {
        if (stack.is(Items.DIAMOND)) return new Jewel("diamond", false);
        if (stack.is(Items.EMERALD)) return new Jewel("emerald", false);
        if (stack.is(Items.AMETHYST_SHARD)) return new Jewel("amethyst", false);
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals(MODID)) return null;
        String path = id.getPath();
        boolean flawless = path.startsWith("flawless_");
        String gem = flawless ? path.substring("flawless_".length()) : path;
        return BONUSES.containsKey(gem) ? new Jewel(gem, flawless) : null;
    }

    /** The gemstone item a jewel was cut as. */
    public static Item gemItem(Jewel jewel) {
        if (!jewel.flawless()) {
            switch (jewel.gem()) {
                case "diamond": return Items.DIAMOND;
                case "emerald": return Items.EMERALD;
                case "amethyst": return Items.AMETHYST_SHARD;
                default: break;
            }
        }
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MODID, (jewel.flawless() ? "flawless_" : "") + jewel.gem()));
    }

    /** What a blank becomes on the Jeweler's Bench: Ring Blank -> Ring, Chain -> Necklace, gold or silver; else null. */
    public static Item jewelFor(ItemStack blank) {
        if (blank.is(GOLD_RING_BLANK.get())) return GOLD_RING.get();
        if (blank.is(GOLD_CHAIN.get())) return GOLD_NECKLACE.get();
        if (blank.is(SILVER_RING_BLANK.get())) return SILVER_RING.get();
        if (blank.is(SILVER_CHAIN.get())) return SILVER_NECKLACE.get();
        if (blank.is(ELECTRUM_RING_BLANK.get())) return ELECTRUM_RING.get();
        if (blank.is(ELECTRUM_CHAIN.get())) return ELECTRUM_NECKLACE.get();
        return null;
    }

    /** How much of a gem's bonus this piece gives: all in gold, {@link #ELECTRUM_STRENGTH} in electrum, {@link #SILVER_STRENGTH} in silver. */
    public static double strength(Item piece) {
        if (piece == ELECTRUM_RING.get() || piece == ELECTRUM_NECKLACE.get()) return ELECTRUM_STRENGTH;
        return piece == SILVER_RING.get() || piece == SILVER_NECKLACE.get() ? SILVER_STRENGTH : 1.0;
    }

    /** A finished piece with {@code jewel} set: its name, the gem's colour on the setting, and the bonus it gives worn. */
    public static ItemStack make(Item piece, Jewel jewel) {
        ItemStack stack = new ItemStack(piece);
        Bonus bonus = BONUSES.get(jewel.gem());
        stack.set(JEWEL.get(), jewel);
        stack.set(DataComponents.ITEM_NAME, Component.translatable(piece.getDescriptionId() + ".with", gemItem(jewel).getName(new ItemStack(gemItem(jewel)))));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(bonus.colour())));
        if (bonus.attribute() != null) {
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder().add(bonus.attribute(),
                    new AttributeModifier(Identifier.fromNamespaceAndPath(MODID, "jewel/" + jewel.gem()), bonus.amount(jewel.flawless()) * strength(piece), bonus.op()),
                    EquipmentSlotGroup.ARMOR).build());
        }
        if (jewel.flawless()) {
            stack.set(DataComponents.RARITY, Rarity.RARE);
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return stack;
    }

    /** Cold insulation of a worn piece set with a warm gem (Temperature#insulation), else 0. */
    public static float warmth(ItemStack stack) {
        Jewel jewel = stack.get(JEWEL.get());
        if (jewel == null) return 0;
        Bonus bonus = BONUSES.get(jewel.gem());
        return bonus != null && bonus.attribute() == null ? (float) (bonus.amount(jewel.flawless()) * strength(stack.getItem())) : 0;
    }

    private Jewelry() {}
}
