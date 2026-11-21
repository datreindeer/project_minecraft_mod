package com.mechanical.people;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.mechanical.BronzeOres;
import com.mechanical.Coins;
import com.mechanical.IronOres;
import com.mechanical.Mechanical;
import com.mechanical.WorldAge;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * The Miner (mechanical:miner, docs/underground-plan.md step 2): a rare lone {@link TribeRole#MINER} below y {@link #TOP},
 * often at a small camp (worldgen MinerCamp). He is a Tribesperson of his own trading tribe (as a caravan's, so he trades
 * with strangers), hunched under a sack of ore, with a pickaxe and a lit torch, and flees from monsters (unarmed). He buys
 * raw ores and stone, and sells the ores that are missing around him (he looks {@link #SCAN_R} blocks round), torches,
 * food and now and then a map to an ore vein further off. Stone Age: barter; from the Bronze Age (his tribe's age,
 * WorldAge.npcAge, as a caravan's): coins. Offers are made the first time someone trades, from the world as it is then.
 */
public final class Miners {
    /** Miners live below this y. */
    public static final int TOP = 30;
    static final int SCAN_R = 16, SCAN_H = 8, MAP_MIN = 24, MAP_MAX = 64, MAP_ODDS = 3;

    /** A trading tribe for a lone miner (as a caravan's): friendly to strangers. */
    public static UUID tribe(RandomSource r) {
        UUID tribe;
        do tribe = new UUID(r.nextLong(), r.nextLong()); while (Tribe.of(tribe).temperament != Tribe.Temperament.TRADERS);
        return tribe;
    }

    /** An ore he may sell where it is missing: its blocks, what he sells, its price in barter and in coin, the age it belongs to. */
    private record Ore(Predicate<BlockState> block, Supplier<ItemStack> goods, Supplier<ItemCost> barter, Supplier<ItemCost> coin, int tier) {}

    private static List<Ore> ores() {
        return List.of(
                new Ore(s -> s.is(net.minecraft.tags.BlockItemTags.COAL_ORES.block()), () -> new ItemStack(Items.COAL, 6), () -> cost(Mechanical.CORDAGE.get(), 2), () -> cost(Coins.COPPER_COIN.get(), 3), 0),
                new Ore(s -> s.is(Mechanical.TIN_ORE.get()), () -> new ItemStack(BronzeOres.CASSITERITE.get(), 3), () -> cost(Items.LEATHER, 2), () -> cost(Coins.COPPER_COIN.get(), 8), 1),
                new Ore(s -> s.is(BlockTags.COPPER_ORES), () -> new ItemStack(Items.RAW_COPPER, 3), () -> cost(Items.LEATHER, 2), () -> cost(Coins.COPPER_COIN.get(), 6), 1),
                new Ore(s -> s.is(BlockTags.IRON_ORES), () -> new ItemStack(Items.RAW_IRON, 2), () -> cost(Items.LEATHER, 4), () -> cost(Coins.SILVER_COIN.get(), 3), 2));
    }

    /** Veins a map can lead to (not coal). */
    private static boolean vein(BlockState s) {
        return s.is(Mechanical.TIN_ORE.get()) || s.is(BlockTags.COPPER_ORES) || s.is(BlockTags.IRON_ORES);
    }

    private static ItemCost cost(ItemLike item, int n) {
        return new ItemCost(item, n);
    }

    /** What he buys: (raw ore or stone, how many, torches for it in barter, copper coins for it). */
    private record Buy(Supplier<Item> item, int count, int torches, int copper) {}

    private static List<Buy> buys(RandomSource r) {
        Item[] stones = {Items.GRANITE, Items.DIORITE, Items.ANDESITE, Items.TUFF};
        Item stone = stones[r.nextInt(stones.length)];
        return List.of(
                new Buy(() -> Items.RAW_COPPER, 4, 4, 3), new Buy(() -> Items.RAW_IRON, 3, 6, 4), new Buy(() -> Items.RAW_GOLD, 2, 8, 6),
                new Buy(BronzeOres.MALACHITE, 4, 3, 3), new Buy(BronzeOres.CASSITERITE, 3, 4, 4), new Buy(() -> Items.COAL, 8, 4, 2),
                new Buy(() -> Items.COBBLESTONE, 32, 2, 1), new Buy(() -> Items.COBBLED_DEEPSLATE, 32, 3, 2),
                new Buy(IronOres.LIMESTONE_ITEM::get, 8, 3, 2), new Buy(() -> stone, 16, 2, 1));
    }

    /** One sale: item for item in the Stone Age, else for coin. */
    private static MerchantOffer sale(ItemStack goods, ItemCost barter, ItemCost coin, boolean coins, int uses) {
        return new MerchantOffer(coins ? coin : barter, goods, uses, 0, 0);
    }

    /** The miner's offers for the world as it is now (server side). */
    static MerchantOffers offers(Tribesperson miner) {
        ServerLevel level = (ServerLevel) miner.level();
        RandomSource r = miner.getRandom();
        int age = miner.age();
        boolean coins = age > WorldAge.STONE;
        BlockPos at = miner.home() != null ? miner.home() : miner.blockPosition();
        MerchantOffers offers = new MerchantOffers();
        for (Ore ore : missingOres(level, at)) {
            if (ore.tier <= age + 1) offers.add(sale(ore.goods.get(), ore.barter.get(), ore.coin.get(), coins, 4));
        }
        offers.add(sale(new ItemStack(Items.TORCH, 8), cost(Mechanical.RAW_HIDE.get(), 1), cost(Coins.COPPER_COIN.get(), 2), coins, 6));
        offers.add(sale(new ItemStack(Wildlife.COOKED_VENISON_SCRAPS.get(), 4), cost(Mechanical.PLANT_FIBRE.get(), 8), cost(Coins.COPPER_COIN.get(), 4), coins, 6));
        BlockPos vein = r.nextInt(MAP_ODDS) == 0 ? findVein(level, at, r) : null;
        if (vein != null) offers.add(sale(veinMap(level, vein), cost(Items.LEATHER, 4), cost(Coins.SILVER_COIN.get(), 1), coins, 1));
        for (Buy b : buys(r)) {
            ItemStack pay = coins ? new ItemStack(Coins.COPPER_COIN.get(), b.copper) : new ItemStack(Items.TORCH, b.torches);
            offers.add(new MerchantOffer(cost(b.item.get(), b.count), pay, 8, 0, 0));
        }
        return offers;
    }

    /** The ores with no block in the box round {@code at} ({@link #SCAN_R} across, {@link #SCAN_H} up and down). */
    private static List<Ore> missingOres(ServerLevel level, BlockPos at) {
        List<Ore> all = ores();
        Set<Ore> found = new HashSet<>();
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-SCAN_R, -SCAN_H, -SCAN_R), at.offset(SCAN_R, SCAN_H, SCAN_R))) {
            if (!level.isLoaded(p)) continue;
            BlockState s = level.getBlockState(p);
            for (Ore ore : all) if (ore.block.test(s)) found.add(ore);
            if (found.size() == all.size()) break;
        }
        return all.stream().filter(o -> !found.contains(o)).toList();
    }

    /** A copper, tin or iron vein {@link #MAP_MIN}-{@link #MAP_MAX} blocks off in loaded chunks (sampled columns), the nearest; or null. */
    static BlockPos findVein(ServerLevel level, BlockPos at, RandomSource r) {
        BlockPos best = null;
        for (int i = 0; i < 64; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = MAP_MIN + r.nextDouble() * (MAP_MAX - MAP_MIN);
            BlockPos col = at.offset((int) (Math.cos(a) * d), 0, (int) (Math.sin(a) * d));
            if (!level.isLoaded(col)) continue;
            for (int dy = -12; dy <= 12; dy++) {
                BlockPos p = col.above(dy);
                if (vein(level.getBlockState(p)) && (best == null || p.distSqr(at) < best.distSqr(at))) best = p;
            }
        }
        return best;
    }

    /** An explorer-style map centred on the vein, marked with a red X. */
    static ItemStack veinMap(ServerLevel level, BlockPos vein) {
        ItemStack map = MapItem.create(level, vein.getX(), vein.getZ(), (byte) 1, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, vein, "+", MapDecorationTypes.RED_X);
        map.set(DataComponents.ITEM_NAME, Component.translatable("filled_map.mechanical.ore_vein"));
        return map;
    }

    private Miners() {}
}
