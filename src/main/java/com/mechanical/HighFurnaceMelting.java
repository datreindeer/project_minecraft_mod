package com.mechanical;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * What melts in a High Furnace and into how much (docs/steel-furnace-plan.md section 5, "Utbyte"): base-metal ore gives
 * twice the crucible's (Crushed Ore 200 mB, Concentrate and Native Copper 400), precious-metal ore 1.5x (Raw Gold 300,
 * Raw Galena 300 Lead), ingots exactly 200 and nuggets 22 (rounded down, so 9 nuggets never make an ingot), blocks 1800.
 * Iron ore (phase 5) is reduced at {@link #IRON_ORE_HEAT}° into Pig Iron, twice the Bloomery's: Raw Iron 100 mB (200 with
 * flux), Roasted Iron Ore 200 (300), bog iron two thirds of that; each ore leaves {@link #SLAG} mB Slag ({@link #SLAG_FLUX}
 * with flux) and burns half a Charcoal out of the Fireboxes. Blister Steel melts into 200 mB Steel (no folding).
 * Each item melts in {@link #TICKS_PER_100} ticks per 100 mB at its heat, faster when hotter (as the kiln).
 */
public final class HighFurnaceMelting {
    public static final int TICKS_PER_100 = 40, IRON_ORE_HEAT = 1450, SLAG = 75, SLAG_FLUX = 25;
    /** Iron ores one Flux serves. */
    public static final int ORES_PER_FLUX = 4;

    /** One item's worth of melt: into what, how much, at what heat, and (iron ore) how much with flux. */
    public record Melt(Molten molten, int mb, int heat, int withFlux) {
        Melt(Molten molten, int mb) {
            this(molten, mb, molten.melt, 0);
        }

        public boolean ironOre() {
            return withFlux > 0;
        }

        public int ticks() {
            return Math.max(1, Math.max(mb, withFlux) * TICKS_PER_100 / 100);
        }
    }

    public static @Nullable Melt of(ItemStack s) {
        if (s.isEmpty()) return null;
        if (s.is(BronzeOres.CRUSHED_COPPER_ORE.get())) return new Melt(Molten.COPPER, 200);
        if (s.is(BronzeOres.CRUSHED_TIN_ORE.get())) return new Melt(Molten.TIN, 200);
        if (s.is(BronzeOres.COPPER_CONCENTRATE.get()) || s.is(BronzeOres.NATIVE_COPPER.get()) || s.is(Items.RAW_COPPER)) return new Melt(Molten.COPPER, 400);
        if (s.is(BronzeOres.TIN_CONCENTRATE.get())) return new Melt(Molten.TIN, 400);
        if (s.is(Items.COPPER_INGOT)) return new Melt(Molten.COPPER, Molten.INGOT);
        if (s.is(Mechanical.TIN_INGOT.get())) return new Melt(Molten.TIN, Molten.INGOT);
        if (s.is(Bronze.BRONZE_INGOT.get())) return new Melt(Bronze.isPoor(s) ? Molten.POOR_BRONZE : Molten.BRONZE, Molten.INGOT);
        if (s.is(Items.RAW_GOLD)) return new Melt(Molten.GOLD, 300);
        if (s.is(Items.GOLD_INGOT)) return new Melt(Molten.GOLD, Molten.INGOT);
        if (s.is(Items.GOLD_NUGGET)) return new Melt(Molten.GOLD, Molten.NUGGET);
        if (s.is(Items.GOLD_BLOCK)) return new Melt(Molten.GOLD, Molten.BLOCK);
        if (s.is(Items.COPPER_BLOCK.weathering().unaffected())) return new Melt(Molten.COPPER, Molten.BLOCK);
        if (s.is(IronOres.RAW_GALENA.get())) return new Melt(Molten.LEAD, 300);
        if (s.is(IronOres.LEAD_INGOT.get())) return new Melt(Molten.LEAD, Molten.INGOT);
        if (s.is(IronOres.SILVER_INGOT.get())) return new Melt(Molten.SILVER, Molten.INGOT);
        if (s.is(IronOres.SILVER_NUGGET.get())) return new Melt(Molten.SILVER, Molten.NUGGET);
        // glass, basalt and obsidian (phase 6)
        if (s.is(HighFurnace.GLASS_BATCH.get())) return new Melt(Molten.GLASS, FoundryBasinBlock.STONE);
        if (s.is(Items.BASALT) || s.is(Items.POLISHED_BASALT) || s.is(Items.SMOOTH_BASALT)) return new Melt(Molten.BASALT, FoundryBasinBlock.STONE);
        if (s.is(Items.OBSIDIAN)) return new Melt(Molten.OBSIDIAN, FoundryBasinBlock.STONE);
        // iron and steel (phase 5)
        if (s.is(Items.RAW_IRON)) return new Melt(Molten.PIG_IRON, 100, IRON_ORE_HEAT, 200);
        if (s.is(Ironworking.ROASTED_IRON_ORE.get())) return new Melt(Molten.PIG_IRON, 200, IRON_ORE_HEAT, 300);
        if (s.is(Ironworking.RAW_BOG_IRON.get())) return new Melt(Molten.PIG_IRON, 65, IRON_ORE_HEAT, 130);
        if (s.is(Ironworking.ROASTED_BOG_IRON.get())) return new Melt(Molten.PIG_IRON, 130, IRON_ORE_HEAT, 200);
        if (s.is(HighFurnace.PIG_IRON_INGOT.get())) return new Melt(Molten.PIG_IRON, Molten.INGOT);
        if (s.is(Ironworking.WROUGHT_IRON_INGOT.get())) return new Melt(Molten.WROUGHT_IRON, Molten.INGOT);
        if (s.is(Smithing.STEEL_INGOT.get()) || s.is(Smithing.BLISTER_STEEL.get())) return new Melt(Molten.STEEL, Molten.INGOT);
        return null;
    }

    /** The nugget of a melt, or null (knocked out of set metal: 22 mB each). */
    public static @Nullable ItemStack nugget(Molten m) {
        return switch (m) {
            case GOLD -> new ItemStack(Items.GOLD_NUGGET);
            case SILVER -> new ItemStack(IronOres.SILVER_NUGGET.get());
            case PIG_IRON, WROUGHT_IRON, STEEL -> new ItemStack(Items.IRON_NUGGET);
            default -> null;
        };
    }

    /** Flux goes on the furnace's flux count, not in a place. */
    public static boolean isFlux(ItemStack s) {
        return s.is(IronOres.FLUX.get());
    }

    private HighFurnaceMelting() {}
}
