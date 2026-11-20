package com.mechanical;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What a Smelting Furnace holds, in quarter units (4 quarters = 1 crucible unit = 1 Crushed Copper Ore's worth of
 * metal), per {@link Metal}: the CHARGE (ore and ingots dropped into the shaft, not melted yet) and the POOL (molten).
 * <p>Yield: ore is worth {@link #ORE_QUARTERS} quarters per unit (+25 % over the crucible), ingots {@link #INGOT_QUARTERS}
 * (their exact metal). Quarters are only made when an item goes in ({@link #quarters}) and only leave when a mold is
 * filled ({@link #pour}, the mold's units x 4) or as slag. Nothing turns metal back into ore, so ore -> metal -> ingot ->
 * remelt can't gain: an ingot costs 8 quarters to cast and gives 8 back.
 * <p>Alloy: the same rules as the crucible ({@link CrucibleContents#alloy(int, int)}), on the pool's copper : tin.
 */
public final class FurnaceMelt {
    /** 40 units: four crucibles, a full bronze tool kit (26 units) with ingots to spare. */
    public static final int CAPACITY = 160;
    public static final int ORE_QUARTERS = 5, INGOT_QUARTERS = 4;
    /**
     * Tin is taken only up to the most a full furnace can hold and still be good bronze (11.5 % of 160), and copper
     * without tin only up to the rest, so a furnace filled from whole stacks always has room for the other ore.
     * (Before 0.58 the first stack thrown in filled it: 32 copper and the tin bounced out, or tin that never poured.)
     */
    public static final int TIN_MAX = 18, COPPER_ALONE = CAPACITY - TIN_MAX;
    /** Metals by ordinal: copper, tin, bronze, poor bronze. */
    static final Metal[] METALS = Metal.values();

    /**
     * The pool is kept in thousandths of a quarter, so a pour can take copper and tin in their exact share: whole
     * quarters would round the same way on every pour and drift the mix (bronze turning poor after a few ingots).
     */
    static final int SCALE = 1000;
    final int[] charge = new int[METALS.length], pool = new int[METALS.length];

    public record Amount(Metal metal, int quarters) {}

    /** What one item adds (its metal and quarters), or null if it doesn't melt. */
    public static Amount quarters(ItemStack stack) {
        if (stack.is(Items.COPPER_INGOT)) return new Amount(Metal.COPPER, 2 * INGOT_QUARTERS);
        Casting.OreUnits u = Casting.oreUnits(stack);
        if (u == null) return null;
        return new Amount(u.metal(), u.units() * (stack.is(Bronze.BRONZE_INGOT.get()) ? INGOT_QUARTERS : ORE_QUARTERS));
    }

    /** Quarters in use (a part-quarter left in the pool counts whole). */
    public int total() {
        return chargeTotal() + (Arrays.stream(pool).sum() + SCALE - 1) / SCALE;
    }

    /** Molten quarters of this metal (whole ones). */
    public int pool(Metal m) {
        return pool[m.ordinal()] / SCALE;
    }

    public int charge(Metal m) {
        return charge[m.ordinal()];
    }

    public int chargeTotal() {
        return Arrays.stream(charge).sum();
    }

    /** Molten quarters in all (whole ones). */
    public int poolTotal() {
        return Arrays.stream(pool).sum() / SCALE;
    }

    /** Sets the molten amount from whole quarters (the screen's copy, the self-check). */
    void setPool(Metal m, int quarters) {
        pool[m.ordinal()] = quarters * SCALE;
    }

    /** Everything, charge and pool, as crucible contents (in quarters): for the mixing and melting-point rules. */
    CrucibleContents all() {
        Map<String, Integer> m = new HashMap<>();
        for (Metal metal : METALS) {
            int q = charge(metal) + (pool[metal.ordinal()] + SCALE - 1) / SCALE;
            if (q > 0) m.put(metal.getSerializedName(), q);
        }
        return new CrucibleContents(m);
    }

    /** How many of these items fit (0 if they don't mix: melted bronze only takes the same bronze, as in the crucible). */
    public int fits(Amount a, int count) {
        CrucibleContents all = all();
        if (!all.canAdd(a.metal())) return 0;
        int room = CAPACITY - total();
        if (a.metal() == Metal.TIN) room = Math.min(room, TIN_MAX - all.units(Metal.TIN));
        else if (a.metal() == Metal.COPPER && all.units(Metal.TIN) == 0) room = Math.min(room, COPPER_ALONE - all.units(Metal.COPPER));
        return Math.max(0, Math.min(count, room / a.quarters()));
    }

    public void add(Amount a, int count) {
        charge[a.metal().ordinal()] += a.quarters() * count;
    }

    /** Degrees the charge needs to melt: the melting point of what it all makes (tin alone: tin's). 0 when nothing is left to melt. */
    public int meltTemp() {
        if (chargeTotal() == 0) return 0;
        Metal m = all().alloy();
        return m == null ? Metal.TIN.melt : m.melt;
    }

    /** Melts one quarter: of the metal most under-represented in the pool, so the pool keeps the charge's mix. */
    public void meltQuarter() {
        double ct = chargeTotal(), pt = Arrays.stream(pool).sum() / (double) SCALE;
        Metal best = null;
        double bestDeficit = -2;
        for (Metal m : METALS) {
            if (charge(m) == 0) continue;
            double inPool = pool[m.ordinal()] / (double) SCALE;
            double deficit = (charge(m) + inPool) / (ct + pt) - (pt == 0 ? 0 : inPool / pt);
            if (deficit > bestDeficit) {
                bestDeficit = deficit;
                best = m;
            }
        }
        if (best == null) return;
        charge[best.ordinal()]--;
        pool[best.ordinal()] += SCALE;
    }

    /** What the pool pours as, or null (empty, or tin alone). */
    public Metal alloy() {
        if (pool[Metal.BRONZE.ordinal()] > 0) return Metal.BRONZE;
        if (pool[Metal.POOR_BRONZE.ordinal()] > 0) return Metal.POOR_BRONZE;
        int cu = pool[Metal.COPPER.ordinal()], sn = pool[Metal.TIN.ordinal()];
        return cu == 0 ? null : CrucibleContents.alloy(cu, sn);
    }

    /** Still melting into a different metal than the pool is now: wait, so a half-melted mix isn't tapped as slag. */
    public boolean settling() {
        return chargeTotal() > 0 && alloy() != all().alloy();
    }

    /** Copper is tapped while there is tin in it: the tin is lost as slag (as in the crucible). */
    public boolean slags() {
        return alloy() == Metal.COPPER && pool[Metal.TIN.ordinal()] > 0;
    }

    /** Whole quarters the pool can pour as {@link #alloy()}. */
    public int pourable() {
        Metal m = alloy();
        if (m == null) return 0;
        if (m == Metal.COPPER) return pool(Metal.COPPER);
        if (pool[m.ordinal()] > 0) return pool(m);
        return (pool[Metal.COPPER.ordinal()] + pool[Metal.TIN.ordinal()]) / SCALE;
    }

    /** Takes {@code q} quarters of the alloy out of the pool (copper and tin in their exact share); false if there isn't that much. */
    public boolean pour(int q) {
        Metal m = alloy();
        if (m == null || pourable() < q) return false;
        int take = q * SCALE;
        if (m == Metal.COPPER) {
            pool[Metal.COPPER.ordinal()] -= take;
            pool[Metal.TIN.ordinal()] = 0; // slag
        } else if (pool[m.ordinal()] > 0) {
            pool[m.ordinal()] -= take;
        } else {
            int cu = pool[Metal.COPPER.ordinal()], sn = pool[Metal.TIN.ordinal()];
            int tin = (int) Math.min(sn, Math.round(take * (double) sn / (cu + sn)));
            int copper = take - tin;
            if (copper > cu) {
                tin += copper - cu;
                copper = cu;
            }
            pool[Metal.COPPER.ordinal()] -= copper;
            pool[Metal.TIN.ordinal()] -= tin;
        }
        return true;
    }

    int[] save() {
        int[] out = new int[METALS.length * 2];
        System.arraycopy(charge, 0, out, 0, METALS.length);
        System.arraycopy(pool, 0, out, METALS.length, METALS.length);
        return out;
    }

    void load(int[] in) {
        Arrays.fill(charge, 0);
        Arrays.fill(pool, 0);
        if (in.length != METALS.length * 2) return;
        System.arraycopy(in, 0, charge, 0, METALS.length);
        System.arraycopy(in, METALS.length, pool, 0, METALS.length);
    }

    /** Thrown on load if the furnace's yield or alloy rules drift from what the docs promise. */
    static void selfCheck() {
        FurnaceMelt f = new FurnaceMelt();
        f.add(new Amount(Metal.COPPER, ORE_QUARTERS), 36);
        f.add(new Amount(Metal.TIN, ORE_QUARTERS), 4);
        check(f.total() == 200 && f.fits(new Amount(Metal.COPPER, ORE_QUARTERS), 1) == 0, "capacity is counted in quarters");
        f = new FurnaceMelt();
        f.add(new Amount(Metal.COPPER, ORE_QUARTERS), 27);
        f.add(new Amount(Metal.TIN, ORE_QUARTERS), 3); // 9:1 crushed ore, 30 units of ore
        check(f.meltTemp() == 950, "copper + tin melts at bronze's 950");
        for (int i = 0; i < 60; i++) f.meltQuarter();
        check(f.settling() || f.alloy() == Metal.BRONZE, "a half-melted mix pours as bronze or waits");
        while (f.chargeTotal() > 0) f.meltQuarter();
        check(f.alloy() == Metal.BRONZE && f.pourable() == 150, "30 ore units melt into 37.5 units of bronze (+25 %)");
        int ingots = 0;
        while (f.pour(ClayMoldBlock.Shape.INGOT.units * 4)) ingots++;
        check(ingots == 18 && f.poolTotal() == 6, "18 ingots (crucible: 15) and 1.5 units left over");
        // remelting gives exactly what casting took: no gain on a loop
        FurnaceMelt r = new FurnaceMelt();
        r.add(new Amount(Metal.BRONZE, 2 * INGOT_QUARTERS), 18);
        while (r.chargeTotal() > 0) r.meltQuarter();
        int again = 0;
        while (r.pour(ClayMoldBlock.Shape.INGOT.units * 4)) again++;
        check(again == 18 && r.poolTotal() == 0, "18 ingots remelt into 18 ingots");
        // the mix keeps the crucible's quality rules
        FurnaceMelt poor = new FurnaceMelt();
        poor.add(new Amount(Metal.COPPER, ORE_QUARTERS), 7);
        poor.add(new Amount(Metal.TIN, ORE_QUARTERS), 1);
        while (poor.chargeTotal() > 0) poor.meltQuarter();
        check(poor.alloy() == Metal.POOR_BRONZE, "7:1 is poor bronze");
        FurnaceMelt slag = new FurnaceMelt();
        slag.add(new Amount(Metal.COPPER, ORE_QUARTERS), 5);
        slag.add(new Amount(Metal.TIN, ORE_QUARTERS), 1);
        while (slag.chargeTotal() > 0) slag.meltQuarter();
        check(slag.alloy() == Metal.COPPER && slag.slags() && slag.pour(8) && slag.pool(Metal.TIN) == 0 && slag.pool(Metal.COPPER) == 17, "slag: the tin is lost");
        check(sameAllTheWay(135, 15) && sameAllTheWay(36, 4) && sameAllTheWay(130, 16) && sameAllTheWay(7, 1), "tapping keeps the mix: every ingot of a pool is the same metal");
        check(poor.fits(new Amount(Metal.BRONZE, 8), 1) == 0 && r.fits(new Amount(Metal.COPPER, 5), 1) > 0, "bronze doesn't mix with ore");
        // whole stacks, either ore first: there is always room for the other, and it comes out good bronze
        check(stacks(Metal.COPPER, Metal.TIN) == Metal.BRONZE && stacks(Metal.TIN, Metal.COPPER) == Metal.BRONZE, "a stack of each ore makes bronze");
    }

    /** Throws a stack of 64 crushed ore of {@code first}, then 64 of {@code second}, as the tap's intake would; melts it all. */
    private static Metal stacks(Metal first, Metal second) {
        FurnaceMelt f = new FurnaceMelt();
        for (Metal m : new Metal[] {first, second}) {
            Amount a = new Amount(m, ORE_QUARTERS);
            f.add(a, f.fits(a, 64));
        }
        while (f.chargeTotal() > 0) f.meltQuarter();
        return f.alloy();
    }

    /** Pours ingots from a copper : tin pool (quarters) until it can't; true if every one came out the same metal. */
    private static boolean sameAllTheWay(int cu, int sn) {
        FurnaceMelt f = new FurnaceMelt();
        f.setPool(Metal.COPPER, cu);
        f.setPool(Metal.TIN, sn);
        Metal first = f.alloy();
        while (f.pourable() >= 8) {
            if (f.alloy() != first) return false;
            f.pour(8);
        }
        return true;
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new IllegalStateException("Smelting Furnace self-check failed: " + what);
    }
}
