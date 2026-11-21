package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Gemstones: ore gems in full ore blocks, crystal gems growing in caves (small / medium / large), pebble gems lying on
 * cave floors. Everything found is a Rough gem; the {@link LapidaryWheelBlock} cuts it into the finished gem (id = the
 * gem's name) with an abrasive at least as hard as the gem (Mohs tiers 1-4, item tags mechanical:gem_hardness/N and
 * mechanical:abrasives/N), and an abrasive only just hard enough may crack it into a Flawed gem. Grit is crushed on the
 * Stone Anvil. Block items come from Mechanical.registerMissingBlockItems; worldgen, tags and the rest are data from
 * scripts/gen_gemstones.py, whose table (scripts/gem_table.py) must list the same names.
 */
@EventBusSubscriber(modid = MODID)
public final class Gemstones {
    /** Ore gems and the rock their ore sits in. */
    static final String[][] ORES = {{"garnet", "stone", "deepslate"}, {"ruby", "deepslate"}, {"sapphire", "stone", "deepslate"},
            {"topaz", "stone"}, {"jade", "stone"}, {"opal", "sandstone"}, {"turquoise", "stone"}, {"peridot", "deepslate"}, {"amber", "stone"}};
    static final String[] CRYSTALS = {"clear_quartz", "rose_quartz", "citrine", "smoky_quartz", "aquamarine", "fluorite", "celestine",
            "moonstone", "tourmaline"};
    static final String[] PEBBLES = {"agate", "jasper", "carnelian", "chalcedony", "bloodstone"};
    /** Vanilla gems that are found rough too (their ores and the Amethyst Cluster drop the Rough gem) and what they cut into. */
    static final java.util.Map<String, Item> VANILLA = java.util.Map.of("diamond", Items.DIAMOND, "emerald", Items.EMERALD, "amethyst", Items.AMETHYST_SHARD);
    /** Chance that an abrasive of the gem's own tier (only just hard enough) cracks it into a Flawed gem. */
    static final float FLAW_CHANCE = 0.35F;
    /** Chance of a Flawless cut with a grit one tier harder than the gem, and two or more tiers harder; doubled for a fine
     *  rough stone (from a Large Crystal, marked by custom data {@link #FINE}). */
    static final float FLAWLESS_ONE_UP = 0.03F, FLAWLESS_TWO_UP = 0.06F;
    static final String FINE = "mechanical_fine";

    public static final DeferredItem<Item> FLINT_GRIT = ITEMS.registerSimpleItem("flint_grit");
    public static final DeferredItem<Item> GARNET_GRIT = ITEMS.registerSimpleItem("garnet_grit");
    public static final DeferredItem<Item> CORUNDUM_GRIT = ITEMS.registerSimpleItem("corundum_grit");
    public static final DeferredItem<Item> DIAMOND_DUST = ITEMS.registerSimpleItem("diamond_dust");
    public static final DeferredBlock<LapidaryWheelBlock> LAPIDARY_WHEEL = BLOCKS.registerBlock("lapidary_wheel", LapidaryWheelBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());

    static {
        for (String gem : List.of("diamond", "emerald", "amethyst")) { // the finished gem is the vanilla item
            ITEMS.registerSimpleItem("rough_" + gem);
            ITEMS.registerSimpleItem("flawed_" + gem);
            flawless(gem);
        }
        for (String[] ore : ORES) {
            gem(ore[0]);
            for (int i = 1; i < ore.length; i++) {
                String host = ore[i];
                BLOCKS.registerSimpleBlock(host.equals("stone") ? ore[0] + "_ore" : host + "_" + ore[0] + "_ore", p -> switch (host) {
                    case "deepslate" -> p.mapColor(MapColor.DEEPSLATE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                            .strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE);
                    case "sandstone" -> p.mapColor(MapColor.SAND).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                            .strength(1.6F);
                    default -> p.mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                            .strength(3.0F, 3.0F);
                });
            }
        }
        for (String gem : CRYSTALS) {
            gem(gem);
            boolean moon = gem.equals("moonstone"); // moonstone glows brighter than the others' faint glimmer
            crystal("small_" + gem + "_crystal", 3, 8, moon ? 3 : 1, SoundType.SMALL_AMETHYST_BUD);
            crystal("medium_" + gem + "_crystal", 5, 10, moon ? 5 : 2, SoundType.LARGE_AMETHYST_BUD);
            crystal("large_" + gem + "_crystal", 7, 10, moon ? 7 : 4, SoundType.AMETHYST_CLUSTER);
        }
        for (String gem : PEBBLES) {
            gem(gem);
            BLOCKS.registerBlock("loose_" + gem, RockBlock::new, p -> p
                    .mapColor(MapColor.STONE).strength(1.5F).sound(SoundType.STONE).noOcclusion().pushReaction(PushReaction.POPPED));
        }
    }

    private static void gem(String name) {
        ITEMS.registerSimpleItem(name);
        ITEMS.registerSimpleItem("rough_" + name);
        ITEMS.registerSimpleItem("flawed_" + name);
        flawless(name);
    }

    /** Flawless gems shine like enchanted items and carry the rare (aqua) name colour. */
    private static void flawless(String name) {
        ITEMS.registerSimpleItem("flawless_" + name, p -> p.rarity(net.minecraft.world.item.Rarity.RARE)
                .component(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    }

    /** A rough stone from a Large Crystal: double the chance of a Flawless cut. */
    static boolean isFine(ItemStack stack) {
        var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(FINE);
    }

    private static void crystal(String id, float height, float width, int light, SoundType sound) {
        BLOCKS.registerBlock(id, p -> new AmethystClusterBlock(height, width, p), p -> p
                .mapColor(MapColor.COLOR_PURPLE).forceSolidOn().noOcclusion().strength(1.5F).sound(sound)
                .lightLevel(s -> light).requiresCorrectToolForDrops().pushReaction(PushReaction.POPPED));
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, path));
    }

    /** Mohs tier 1-5 of a Rough gem (0 = not a rough gem). */
    static int hardness(ItemStack stack) {
        return tier(stack, "gem_hardness/");
    }

    /** Tier 1-5 of an abrasive: Sand, Flint Grit, Garnet Grit, Corundum Grit, Diamond Dust (0 = not an abrasive). */
    static int abrasive(ItemStack stack) {
        return tier(stack, "abrasives/");
    }

    private static int tier(ItemStack stack, String prefix) {
        for (int t = 5; t >= 1; t--) if (stack.is(tag(prefix + t))) return t;
        return 0;
    }

    /** What a Rough gem becomes on a wheel with grit of the given tier: empty if the grit is too soft for it. */
    static ItemStack cut(ItemStack rough, int grit, RandomSource random) {
        int hard = hardness(rough);
        if (hard == 0 || grit < hard) return ItemStack.EMPTY;
        String name = BuiltInRegistries.ITEM.getKey(rough.getItem()).getPath().substring("rough_".length());
        int margin = grit - hard;
        // nothing is harder than Diamond Dust, so a diamond gets the one-up chance on its own tier (after the crack check)
        float flawless = (margin >= 2 ? FLAWLESS_TWO_UP : margin == 1 || hard == 5 ? FLAWLESS_ONE_UP : 0) * (isFine(rough) ? 2 : 1);
        String prefix = margin == 0 && random.nextFloat() < FLAW_CHANCE ? "flawed_" : random.nextFloat() < flawless ? "flawless_" : "";
        if (prefix.isEmpty() && VANILLA.containsKey(name)) return new ItemStack(VANILLA.get(name));
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MODID, prefix + name)));
    }

    /** Grit crushed from one item on the Stone Anvil, or empty. */
    static ItemStack grit(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.is(Items.FLINT)) return new ItemStack(FLINT_GRIT.get(), 2);
        if (id.equals("mechanical:rough_garnet") || id.equals("mechanical:flawed_garnet")) return new ItemStack(GARNET_GRIT.get(), 2);
        if (id.matches("mechanical:(rough|flawed)_(ruby|sapphire)")) return new ItemStack(CORUNDUM_GRIT.get(), 2);
        if (id.matches("mechanical:(rough|flawed)_diamond")) return new ItemStack(DIAMOND_DUST.get(), 2);
        return ItemStack.EMPTY;
    }

    // ---------------------------------------------------------------- game test

    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final ResourceKey<Consumer<GameTestHelper>> TEST = ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, "gemstones"));

    static {
        TEST_FUNCTIONS.register("gemstones", () -> Gemstones::test);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "gemstones"), new TestEnvironmentDefinition.AllOf(List.of()));
        event.registerTest(Identifier.fromNamespaceAndPath(MODID, "gemstones"),
                new FunctionGameTestInstance(TEST, TestArea.of(env, 40, true)));
    }

    private static Item item(String name) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(MODID, name));
    }

    private static void test(GameTestHelper h) {
        RandomSource random = RandomSource.create(1);
        ItemStack garnet = new ItemStack(item("rough_garnet")), citrine = new ItemStack(item("rough_citrine")), ruby = new ItemStack(item("rough_ruby"));
        h.assertTrue(hardness(garnet) == 3 && hardness(citrine) == 2 && hardness(ruby) == 4, "Mohs tiers: garnet 3, citrine 2, ruby 4");
        h.assertTrue(abrasive(new ItemStack(Items.SAND)) == 1 && abrasive(new ItemStack(GARNET_GRIT.get())) == 3, "Sand is tier 1, Garnet Grit tier 3");
        h.assertTrue(cut(garnet, 2, random).isEmpty(), "Flint Grit is too soft for Garnet");
        for (int i = 0; i < 40; i++) {
            ItemStack c = cut(citrine, 3, random);
            h.assertTrue(c.is(item("citrine")) || c.is(item("flawless_citrine")), "a harder grit never cracks Citrine (got " + c + ")");
        }
        boolean fine = false, flawed = false;
        for (int i = 0; i < 60; i++) {
            ItemStack out = cut(garnet, 3, random);
            fine |= out.is(item("garnet"));
            flawed |= out.is(item("flawed_garnet"));
        }
        h.assertTrue(fine && flawed, "a grit only just hard enough gives fine and Flawed Garnets");
        ItemStack diamond = new ItemStack(item("rough_diamond"));
        h.assertTrue(hardness(diamond) == 5 && cut(diamond, 4, random).isEmpty(), "Corundum Grit is too soft for a Rough Diamond");
        for (int i = 0; i < 20; i++) {
            ItemStack d = cut(diamond, 5, random);
            h.assertTrue(d.is(Items.DIAMOND) || d.is(item("flawed_diamond")), "Diamond Dust cuts a Rough Diamond into a vanilla Diamond (got " + d + ")");
        }
        boolean flawlessDiamond = false;
        for (int i = 0; i < 2000 && !flawlessDiamond; i++) flawlessDiamond = cut(diamond, 5, random).is(item("flawless_diamond"));
        h.assertTrue(flawlessDiamond, "Diamond Dust can give a Flawless Diamond");
        h.assertTrue(BronzeOres.crushed(diamond).is(DIAMOND_DUST.get()), "a Rough Diamond crushes into Diamond Dust");
        h.assertTrue(cut(new ItemStack(item("rough_amethyst")), 3, random).is(Items.AMETHYST_SHARD), "Rough Amethyst cuts into an Amethyst Shard");
        // Flawless: never with a grit of the gem's own tier, ~6 % two tiers up, doubled for a fine stone from a Large Crystal
        int plain = 0, fineCount = 0, same = 0;
        ItemStack fineCitrine = citrine.copy();
        var tag = new net.minecraft.nbt.CompoundTag();
        tag.putBoolean(FINE, true);
        fineCitrine.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        for (int i = 0; i < 4000; i++) {
            if (cut(citrine, 5, random).is(item("flawless_citrine"))) plain++;
            if (cut(fineCitrine, 5, random).is(item("flawless_citrine"))) fineCount++;
            if (cut(citrine, 2, random).is(item("flawless_citrine"))) same++;
        }
        h.assertTrue(same == 0, "no Flawless cut with a grit only just hard enough");
        h.assertTrue(plain > 140 && plain < 340, "about 6 % Flawless with a much harder grit (got " + plain + "/4000)");
        h.assertTrue(fineCount > 340 && fineCount < 640, "about 12 % for a fine stone (got " + fineCount + "/4000)");
        BlockPos crystalAt = h.absolutePos(new BlockPos(3, 1, 1));
        h.setBlock(new BlockPos(3, 0, 1), net.minecraft.world.level.block.Blocks.STONE);
        h.setBlock(new BlockPos(3, 1, 1), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(MODID, "large_citrine_crystal")));
        var drops = net.minecraft.world.level.block.Block.getDrops(h.getLevel().getBlockState(crystalAt), h.getLevel(), crystalAt, null, null,
                new ItemStack(Items.IRON_PICKAXE));
        h.assertTrue(!drops.isEmpty() && drops.stream().allMatch(d -> d.is(item("rough_citrine")) && isFine(d)),
                "a Large Citrine Crystal drops fine Rough Citrine (got " + drops + ")");
        // /regengems: "ores" picks exactly the 9 ore veins; a chunk gets its gems once, a second run leaves it alone
        var oreFeatures = com.mechanical.worldgen.GemRegen.features(h.getLevel(), false);
        h.assertTrue(oreFeatures.size() == ORES.length && com.mechanical.worldgen.GemRegen.features(h.getLevel(), true).size() == 32,
                "regengems ores = the 9 ore veins, all = 32 gem features (got " + oreFeatures + ")");
        var chunk = new net.minecraft.world.level.ChunkPos(net.minecraft.core.SectionPos.blockToSectionCoord(h.absolutePos(BlockPos.ZERO).getX()) + 4,
                net.minecraft.core.SectionPos.blockToSectionCoord(h.absolutePos(BlockPos.ZERO).getZ()) + 4);
        h.getLevel().getChunk(chunk.x(), chunk.z()).setData(com.mechanical.worldgen.GemRegen.HAS_GEMS, false);
        int[] first = com.mechanical.worldgen.GemRegen.regen(h.getLevel(), chunk, 0, true);
        int[] second = com.mechanical.worldgen.GemRegen.regen(h.getLevel(), chunk, 0, true);
        h.assertTrue(first[0] == 1 && second[0] == 0 && second[1] == 1, "regengems does a chunk once (got " + first[0] + ", then " + second[0] + ")");
        // /regenstones: the stone-only veins (marble, galena...), never dirt-replacing ones (peat) or retrogenned ones (tin); once per chunk
        var stones = com.mechanical.worldgen.GemRegen.stoneFeatures(h.getLevel());
        h.assertTrue(stones.contains("ore_marble") && !stones.contains("ore_peat")
                && !stones.contains("ore_tin") && !stones.contains("ore_kaolin"), "regenstones veins (got " + stones + ")");
        h.getLevel().getChunk(chunk.x(), chunk.z()).setData(com.mechanical.worldgen.GemRegen.HAS_STONES, false);
        int[] s1 = com.mechanical.worldgen.GemRegen.regenStones(h.getLevel(), chunk, 0);
        int[] s2 = com.mechanical.worldgen.GemRegen.regenStones(h.getLevel(), chunk, 0);
        h.assertTrue(s1[0] + s1[1] == 1 && s2[0] == 0 && s2[1] == 1, "regenstones does a chunk once");
        h.assertTrue(BronzeOres.crushed(garnet).is(GARNET_GRIT.get()) && BronzeOres.crushed(ruby).is(CORUNDUM_GRIT.get())
                && BronzeOres.crushed(new ItemStack(Items.FLINT)).is(FLINT_GRIT.get()), "grit is crushed on the Stone Anvil");

        // the wheel: grit in, then four passes with the rough gem
        BlockPos at = new BlockPos(1, 1, 1);
        h.setBlock(at, LAPIDARY_WHEEL.get());
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos pos = h.absolutePos(at);
        LapidaryWheelBlock.addGrit(h.getLevel(), pos, h.getLevel().getBlockState(pos), player, new ItemStack(Items.SAND));
        ItemStack amber = new ItemStack(item("rough_amber"), 2);
        for (int i = 0; i < LapidaryWheelBlock.PASSES; i++)
            LapidaryWheelBlock.grind(h.getLevel(), pos, h.getLevel().getBlockState(pos), player, amber);
        h.assertTrue(amber.getCount() == 1, "four passes use up one Rough Amber (left " + amber.getCount() + ")");
        h.assertTrue(player.getInventory().contains(s -> s.is(item("amber")) || s.is(item("flawed_amber"))), "the cut Amber goes to the player");
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(LapidaryWheelBlock.CHARGES) == LapidaryWheelBlock.PER_GRIT - 1, "one charge of sand used");
        h.succeed();
    }

    private Gemstones() {}
}
