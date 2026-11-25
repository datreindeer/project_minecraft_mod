package com.mechanical;

import static com.mechanical.Mechanical.MODID;

import java.util.List;
import java.util.function.Consumer;

import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The metal table (docs/metals.md, {@link Molten}):
 * <ul>
 * <li>{@code metals_table}: every item it names exists; the layers lie by density; every metal melts at its table heat in
 * each furnace that takes it (Clay Kiln crucible, Smelting Furnace, High Furnace) and the furnace can reach it; the alloy
 * ratios (crucible 9-11.5 % / 8-15 % tin, High Furnace 9:1 and 1:2); what each metal casts in every clay mold shape, iron
 * mold, the Foundry Basin and the Molding Flask; coins; the 0.208.0 fixes; old saves (pool, salamander, smelter) load.</li>
 * <li>{@code metals_new_metal}: a metal added by one table entry (a test-only Zinc and Brass, made of vanilla items) melts,
 * layers, alloys, casts and saves through the same code, and is gone again after.</li>
 * </ul>
 */
@EventBusSubscriber(modid = MODID)
public final class MetalsTest {
    private static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, MODID);
    private static final String[] TESTS = {"metals_table", "metals_new_metal"};

    static {
        TEST_FUNCTIONS.register("metals_table", () -> MetalsTest::table);
        TEST_FUNCTIONS.register("metals_new_metal", () -> MetalsTest::newMetal);
    }

    public static void init(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    @SubscribeEvent
    static void registerTests(RegisterGameTestsEvent event) {
        var env = event.registerEnvironment(Identifier.fromNamespaceAndPath(MODID, "metals"), new TestEnvironmentDefinition.AllOf(List.of()));
        for (String t : TESTS)
            event.registerTest(Identifier.fromNamespaceAndPath(MODID, t), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(MODID, t)), TestArea.of(env, 100, true)));
    }

    private static ItemStack crucibleOf(Metal metal, int units) {
        ItemStack c = new ItemStack(Mechanical.CRUCIBLE.get());
        c.set(Casting.CRUCIBLE_CONTENTS.get(), CrucibleContents.EMPTY.add(metal, units));
        return c;
    }

    private static void table(GameTestHelper h) {
        // every item the table names is registered (a typo would only show when that metal is used)
        for (Molten m : Molten.values())
            for (Identifier id : m.items()) h.assertTrue(BuiltInRegistries.ITEM.containsKey(id), m + " names a missing item " + id);
        // layers by density, heaviest at the bottom, slag on top; the Clay Mold states stay what saved worlds hold
        List<Molten> all = Molten.values();
        for (int i = 1; i < all.size(); i++) h.assertTrue(all.get(i - 1).density >= all.get(i).density, "density order: " + all);
        h.assertTrue(all.getFirst() == Molten.GOLD && all.getLast() == Molten.SLAG, "gold at the bottom, slag on top: " + all);
        h.assertTrue(all.indexOf(Molten.STEEL) < all.indexOf(Molten.TIN) && all.indexOf(Molten.TIN) < all.indexOf(Molten.PIG_IRON),
                "steel (7.85) under tin (7.29) under pig iron (7.1): " + all);
        h.assertTrue(ClayMoldBlock.METAL.getPossibleValues().equals(List.of(Metal.COPPER, Metal.BRONZE, Metal.POOR_BRONZE)), "clay mold metals: "
                + ClayMoldBlock.METAL.getPossibleValues());

        // melting points per furnace: the crucible in the Clay Kiln and the Smelting Furnace melt at the table's heat, which
        // their 1300° ceiling reaches; everything melts in the High Furnace at its heat, under its 1800° top
        for (Metal m : Metal.values()) {
            h.assertTrue(m.melt() == m.molten().melt && m.melt() <= KilnBlockEntity.CEILING, m + ": the kiln reaches " + m.melt());
            if (!m.castsAnything()) continue;
            KilnBlockEntity.Bake bake = KilnBlockEntity.bakeFor(crucibleOf(m, 2));
            h.assertTrue(bake != null && bake.temp == m.melt(), m + " melts in the kiln at its table heat");
        }
        h.assertTrue(KilnBlockEntity.bakeFor(crucibleOf(Metal.TIN, 2)) == null, "tin alone is not poured from a crucible");
        FurnaceMelt smelter = new FurnaceMelt();
        smelter.add(new FurnaceMelt.Amount(Metal.COPPER, FurnaceMelt.ORE_QUARTERS), 4);
        h.assertTrue(smelter.meltTemp() == Molten.COPPER.melt, "the Smelting Furnace melts copper at " + Molten.COPPER.melt);
        for (Molten m : all) {
            h.assertTrue(m.melt <= HighFurnaceBlockEntity.MAX_HEAT, m + " can melt in a High Furnace");
            ItemStack ingot = m.ingot();
            if (!ingot.isEmpty()) {
                HighFurnaceMelting.Melt melt = HighFurnaceMelting.of(ingot);
                h.assertTrue(melt != null && melt.molten() == m && melt.mb() == Molten.INGOT && melt.heat() == m.melt, m + "'s ingot melts into 200 mB at " + m.melt);
            }
        }
        HighFurnaceMelting.Melt ore = HighFurnaceMelting.of(new ItemStack(Items.RAW_IRON));
        h.assertTrue(ore != null && ore.molten() == Molten.PIG_IRON && ore.heat() == 1450 && ore.mb() == 100 && ore.withFlux() == 200, "raw iron: 100 / 200 mB pig iron at 1450°");
        h.assertTrue(HighFurnaceMelting.of(new ItemStack(Items.GOLD_BLOCK)).mb() == Molten.BLOCK, "a block melts into 1800 mB");
        h.assertTrue(IronOres.GOLD_TEMP == Molten.GOLD.melt && IronOres.CUPEL_TEMP == Molten.SILVER.melt, "the kiln's gold and cupel heats are the table's");

        // alloys: the crucible by share, the High Furnace in exact parts
        h.assertTrue(CrucibleContents.alloy(9, 1) == Metal.BRONZE && CrucibleContents.alloy(7, 1) == Metal.POOR_BRONZE && CrucibleContents.alloy(5, 1) == Metal.COPPER,
                "crucible: 10 % tin bronze, 12.5 % poor, 16.7 % copper and slag");
        MeltPool p = new MeltPool(8000);
        p.add(Molten.COPPER, 900);
        p.add(Molten.TIN, 200);
        p.add(Molten.PIG_IRON, 300);
        p.add(Molten.WROUGHT_IRON, 600);
        for (int t = 0; t < 200; t++) p.mix(1600);
        h.assertTrue(p.get(Molten.BRONZE) == 1000 && p.get(Molten.TIN) == 100 && p.get(Molten.STEEL) == 900 && p.total() == 2000, "9:1 bronze, 1:2 steel: " + p.layers());

        // casts: clay molds (copper 6 shapes, bronze and poor bronze all, poor marked; tin none) ...
        for (Metal m : Metal.values())
            for (ClayMoldBlock.Shape s : ClayMoldBlock.Shape.values()) {
                ItemStack c = m.cast(s);
                h.assertTrue(c.isEmpty() != m.casts(s), m + " in " + s);
                if (!c.isEmpty()) h.assertTrue(Bronze.isPoor(c) == (m == Metal.POOR_BRONZE), m + " " + s + " is marked poor only for poor bronze");
            }
        h.assertTrue(Metal.BRONZE.cast(ClayMoldBlock.Shape.TUYERE).is(HighFurnace.BRONZE_TUYERE_NOZZLE.get()) && Metal.COPPER.cast(ClayMoldBlock.Shape.SAW_BLADE).isEmpty()
                && !Metal.TIN.castsAnything(), "the bronze tuyere, no copper saw, no tin casts");
        // ... the iron molds, the basin and the flask
        for (Molten m : all) {
            ItemStack ingots = IngotMoldBlock.Kind.INGOT.result(m), pig = IngotMoldBlock.Kind.PIG.result(m);
            h.assertTrue(m.nonMetal == (ingots == null) && (ingots == null || ingots.getCount() == 4 && pig.getCount() == 2), m + ": 4 ingots / 2 pigs");
            ItemStack nuggets = IngotMoldBlock.Kind.NUGGET.result(m);
            h.assertTrue(nuggets == null || nuggets.getCount() == 18, m + ": 18 nuggets");
        }
        h.assertTrue(IngotMoldBlock.Kind.NUGGET.result(Molten.COPPER).is(Items.COPPER_NUGGET) && IngotMoldBlock.Kind.NUGGET.result(Molten.STEEL).is(Items.IRON_NUGGET)
                && IngotMoldBlock.Kind.NUGGET.result(Molten.LEAD) == null, "nuggets: copper's, iron's for steel, none of lead");
        ItemStack poorPlate = IngotMoldBlock.Kind.PLATE.result(Molten.POOR_BRONZE);
        h.assertTrue(poorPlate != null && poorPlate.is(Bronze.BRONZE_PLATE.get()) && Bronze.isPoor(poorPlate), "poor bronze casts a poor Bronze Plate (fix 8)");
        h.assertTrue(IngotMoldBlock.Kind.PLATE.result(Molten.STEEL).is(Smithing.STEEL_PLATE.get()) && IngotMoldBlock.Kind.PLATE.result(Molten.GOLD) == null, "steel plate, no gold plate");
        h.assertTrue(FoundryBasinBlock.block(Molten.POOR_BRONZE) == null && FoundryBasinBlock.block(Molten.BRONZE) == HighFurnace.BRONZE_BLOCK_ITEM.get(),
                "poor bronze casts no block (fix 7), bronze does");
        h.assertTrue(FoundryBasinBlock.block(Molten.TIN) == Mechanical.TIN_BLOCK_ITEM.get() && FoundryBasinBlock.block(Molten.SLAG) == HighFurnace.SLAG_STONE_ITEM.get()
                && FoundryBasinBlock.cast(Molten.GLASS) == FoundryBasinBlock.STONE, "tin casts a Tin Block (fix 9); slag stone; glass 250 mB");
        h.assertTrue(MoldingFlaskBlock.FlaskShape.GEAR.result(Molten.PIG_IRON).is(HighFurnace.CAST_IRON_GEAR.get()) && MoldingFlaskBlock.FlaskShape.GEAR.result(Molten.STEEL) == null
                && MoldingFlaskBlock.FlaskShape.PICKAXE_HEAD.result(Molten.BRONZE).is(Bronze.BRONZE_PICKAXE_HEAD.get()), "flask: cast iron gear, a finished bronze head");
        h.assertTrue(Coins.minted(new ItemStack(IronOres.SILVER_INGOT.get())).is(Coins.SILVER_COIN.get()) && Coins.minted(new ItemStack(Items.IRON_INGOT)).isEmpty(), "coins");

        // remelting what the molds give: copper and iron nuggets (fix 6), tin ingots in the Smelting Furnace (fix 10), no copper ingot in a crucible
        HighFurnaceMelting.Melt cn = HighFurnaceMelting.of(new ItemStack(Items.COPPER_NUGGET)), in = HighFurnaceMelting.of(new ItemStack(Items.IRON_NUGGET));
        h.assertTrue(cn != null && cn.molten() == Molten.COPPER && cn.mb() == Molten.NUGGET && in != null && in.molten() == Molten.PIG_IRON, "nuggets remelt: 22 mB copper, iron as pig iron");
        ItemStack poor = new ItemStack(Bronze.BRONZE_INGOT.get());
        Bronze.markPoor(poor);
        h.assertTrue(HighFurnaceMelting.of(poor).molten() == Molten.POOR_BRONZE && HighFurnaceMelting.of(new ItemStack(Bronze.BRONZE_INGOT.get())).molten() == Molten.BRONZE, "a poor ingot melts as poor bronze");
        FurnaceMelt.Amount tin = FurnaceMelt.quarters(new ItemStack(Mechanical.TIN_INGOT.get()));
        h.assertTrue(tin != null && tin.metal() == Metal.TIN && tin.quarters() == 8, "a Tin Ingot is 2 units in the Smelting Furnace");
        h.assertTrue(Casting.oreUnits(new ItemStack(Items.COPPER_INGOT)) == null && Casting.oreUnits(poor).metal() == Metal.POOR_BRONZE
                && Casting.oreUnits(new ItemStack(BronzeOres.NATIVE_COPPER.get())).units() == 2, "crucible: no copper ingot, poor bronze ingot, native copper 2");
        List<ItemStack> back = CrucibleItem.emptied(CrucibleContents.EMPTY.add(Metal.COPPER, 3), CrucibleItem.Kind.ORE);
        h.assertTrue(back.size() == 2 && back.get(0).is(BronzeOres.COPPER_CONCENTRATE.get()) && back.get(1).is(BronzeOres.CRUSHED_COPPER_ORE.get()), "3 units of ore back: concentrate + crushed: " + back);

        // old saves: a pool, a salamander and a smelter from before 0.208.0
        MeltPool old = new MeltPool(8000);
        old.loadLegacy(new int[] {0, 200, 9, 100, 13, 25});
        h.assertTrue(old.get(Molten.GOLD) == 200 && old.get(Molten.TIN) == 100 && old.get(Molten.SLAG) == 25, "an old pool loads: " + old.layers());
        ItemStack salamander = new ItemStack(HighFurnace.FURNACE_SALAMANDER.get());
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("pool", new int[] {4, 500, 0, 200});
        salamander.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        MeltPool held = new MeltPool(Integer.MAX_VALUE);
        FurnaceSalamander.load(salamander, held);
        h.assertTrue(held.get(Molten.BRONZE) == 500 && held.get(Molten.GOLD) == 200, "an old salamander loads: " + held.layers());
        FurnaceMelt f = new FurnaceMelt();
        f.load(new int[] {4, 0, 0, 0, 8000, 0, 0, 0}); // charge, then pool in thousandths of a quarter
        h.assertTrue(f.charge(Metal.COPPER) == 4 && f.pool(Metal.COPPER) == 8, "a smelter saved with these 4 metals loads");
        f.load(new int[] {4, 0, 8000, 0}); // a save with fewer metals than now: what it has loads
        h.assertTrue(f.charge(Metal.COPPER) == 4 && f.pool(Metal.COPPER) == 8 && f.charge(Metal.BRONZE) == 0, "a shorter smelter save loads what it has");
        h.succeed();
    }

    /** A test-only Zinc and Brass, written as the table rows gen_metals.py writes (docs/metals.md, "Så lägger du till en metall"). */
    static final String TEST_TABLE = """
            {"metals": [
              {"id": "test_zinc", "melt": 420, "density": 7.14, "color": "B4C0C8", "ingot": "minecraft:brick", "nugget": "minecraft:flint",
               "block": "minecraft:bricks", "melts": {"minecraft:clay_ball": 100, "minecraft:brick": 200, "minecraft:flint": 22, "minecraft:bricks": 1800}},
              {"id": "test_brass", "melt": 930, "density": 8.5, "color": "E8C050", "ingot": "minecraft:nether_brick", "plate": "minecraft:paper",
               "melts": {"minecraft:nether_brick": 200}}],
             "alloys": [{"result": "test_brass", "parts": [["copper", 2], ["test_zinc", 1]], "heat": 950}]}
            """;

    private static void newMetal(GameTestHelper h) {
        Molten.Added added = Molten.load(JsonParser.parseString(TEST_TABLE).getAsJsonObject());
        try {
            Molten zinc = Molten.byId("test_zinc"), brass = Molten.byId("test_brass");
            h.assertTrue(zinc != null && brass != null && zinc.metal() == null, "both rows are in, neither is a crucible metal");
            Molten.selfCheck();
            List<Molten> all = Molten.values();
            h.assertTrue(all.indexOf(Molten.POOR_BRONZE) < all.indexOf(brass) && all.indexOf(brass) < all.indexOf(Molten.STEEL)
                    && all.indexOf(Molten.TIN) < all.indexOf(zinc) && all.indexOf(zinc) < all.indexOf(Molten.PIG_IRON), "they lie by density: " + all);
            // melting: the ore and the ingot by the table, at its heat
            HighFurnaceMelting.Melt ore = HighFurnaceMelting.of(new ItemStack(Items.CLAY_BALL)), ingot = HighFurnaceMelting.of(new ItemStack(Items.BRICK));
            h.assertTrue(ore != null && ore.molten() == zinc && ore.mb() == 100 && ore.heat() == 420 && ingot.mb() == Molten.INGOT, "zinc melts at 420°: ore 100, ingot 200 mB");
            // the pool layers it and mixes the alloy, exactly 2:1
            MeltPool p = new MeltPool(8000);
            p.add(zinc, 300);
            p.add(Molten.COPPER, 400);
            p.add(Molten.SLAG, 50);
            h.assertTrue(p.bottom() == Molten.COPPER && p.layers().get(1).getKey() == zinc && p.layers().getLast().getKey() == Molten.SLAG, "copper, zinc, slag: " + p.layers());
            for (int t = 0; t < 100; t++) p.mix(900);
            h.assertTrue(p.get(brass) == 0, "no brass below 950°");
            for (int t = 0; t < 100; t++) p.mix(1000);
            h.assertTrue(p.get(brass) == 600 && p.get(zinc) == 100 && p.get(Molten.COPPER) == 0 && p.total() == 750, "2 copper + 1 zinc -> 3 brass: " + p.layers());
            // it casts what the table says, and saves by id
            ItemStack bars = IngotMoldBlock.Kind.INGOT.result(brass), plate = IngotMoldBlock.Kind.PLATE.result(brass), nuggets = IngotMoldBlock.Kind.NUGGET.result(zinc);
            h.assertTrue(bars.is(Items.NETHER_BRICK) && bars.getCount() == 4 && plate.is(Items.PAPER) && nuggets.is(Items.FLINT) && nuggets.getCount() == 18,
                    "4 brass ingots, a plate, 18 zinc nuggets");
            Item block = FoundryBasinBlock.block(zinc);
            h.assertTrue(block == Items.BRICKS && FoundryBasinBlock.block(brass) == null, "a zinc block, no brass block");
            MeltPool l = new MeltPool(8000);
            CompoundTag saved = p.save();
            l.load(saved);
            h.assertTrue(l.get(brass) == 600 && l.get(zinc) == 100 && l.total() == 750, "saved and loaded by id");
            Molten.unload(added);
            added = null;
            h.assertTrue(Molten.byId("test_zinc") == null && HighFurnaceMelting.of(new ItemStack(Items.CLAY_BALL)) == null, "unloaded: gone");
            l.load(saved);
            h.assertTrue(l.total() == 50 && l.get(Molten.SLAG) == 50, "a melt no longer in the table drops out of a save, the rest loads");
        } finally {
            if (added != null) Molten.unload(added);
        }
        h.succeed();
    }

    private MetalsTest() {}
}
