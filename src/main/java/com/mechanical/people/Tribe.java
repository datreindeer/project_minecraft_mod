package com.mechanical.people;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.mechanical.Mechanical;
import com.mojang.serialization.Codec;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.neoforged.neoforge.attachment.AttachmentType;

/**
 * A tribe ("lag"): every camp, hunting band and village is one, identified by its group id. Name (a totem animal), banner
 * and temperament all follow from that id, so nothing about the tribe itself needs saving. What players did to a tribe is
 * saved per player ({@link #REPUTATION}, -100..100, starting at the temperament's default), or shared by a player tribe's
 * members ({@link #standings}); every member reacts to it, also members the player never met.
 */
public final class Tribe {
    /** How a tribe meets strangers: default standing, whether they guard their land, and how keen they are to trade. */
    public enum Temperament {
        PEACEFUL(15, false, 0, 1.0), WARY(-5, false, 10, 0.8), RAIDERS(-20, true, 30, 0.6), TRADERS(10, false, -100, 1.5);

        public final int start, minTrade;
        public final boolean territorial;
        public final double discount;

        Temperament(int start, boolean territorial, int minTrade, double discount) {
            this.start = start;
            this.territorial = territorial;
            this.minTrade = minTrade;
            this.discount = discount;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final int HOSTILE = -40, FRIENDLY = 30, MIN = -100, MAX = 100;
    public static final int HIT = -8, KILL = -60, FIRE = -15, THEFT = -10, TRADE = 2, GIFT = 2;
    /** Letting a visiting band stay the night, or sending it away (TribeVisits); standing of a best friend (advancement). */
    public static final int STAY = 4, REFUSE = -3, BEST_FRIENDS = 80;

    static final String[] TOTEMS = {"raven", "wolf", "bear", "elk", "salmon", "owl", "fox", "boar", "lynx", "eagle", "otter", "badger",
            "stag", "heron", "mammoth", "aurochs", "hare", "crane", "wolverine", "bison"};
    private static final DyeColor[][] COLOURS = {
            {DyeColor.GREEN, DyeColor.LIME, DyeColor.WHITE, DyeColor.LIGHT_BLUE},  // peaceful
            {DyeColor.BROWN, DyeColor.GRAY, DyeColor.GREEN, DyeColor.CYAN},        // wary
            {DyeColor.BLACK, DyeColor.RED, DyeColor.GRAY, DyeColor.BROWN},         // raiders
            {DyeColor.YELLOW, DyeColor.ORANGE, DyeColor.BLUE, DyeColor.PURPLE}};   // traders
    private static final List<ResourceKey<BannerPattern>> PATTERNS = List.of(BannerPatterns.STRIPE_BOTTOM, BannerPatterns.STRIPE_TOP,
            BannerPatterns.STRIPE_CENTER, BannerPatterns.STRIPE_MIDDLE, BannerPatterns.STRIPE_DOWNRIGHT, BannerPatterns.STRIPE_DOWNLEFT,
            BannerPatterns.CROSS, BannerPatterns.STRAIGHT_CROSS, BannerPatterns.TRIANGLE_BOTTOM, BannerPatterns.TRIANGLE_TOP,
            BannerPatterns.TRIANGLES_BOTTOM, BannerPatterns.TRIANGLES_TOP, BannerPatterns.CIRCLE_MIDDLE, BannerPatterns.RHOMBUS_MIDDLE,
            BannerPatterns.HALF_HORIZONTAL, BannerPatterns.HALF_VERTICAL, BannerPatterns.CURLY_BORDER, BannerPatterns.BORDER,
            BannerPatterns.STRIPE_SMALL, BannerPatterns.DIAGONAL_LEFT);

    /** Each player's standing with each tribe they have dealt with (tribe id -> -100..100). */
    public static final Supplier<AttachmentType<Map<String, Integer>>> REPUTATION = Mechanical.ATTACHMENTS.register("tribe_reputation",
            () -> AttachmentType.<Map<String, Integer>>builder(() -> Map.of())
                    .serialize(Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("tribes"))
                    .copyOnDeath()
                    .build());

    public final UUID id;
    public final Temperament temperament;
    public final String totem;
    private final DyeColor base;
    private final ResourceKey<?>[] layers;
    private final DyeColor[] layerColours;

    private Tribe(UUID id) {
        this.id = id;
        java.util.SplittableRandom r = new java.util.SplittableRandom(id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 32));
        int t = r.nextInt(10);
        temperament = t < 3 ? Temperament.PEACEFUL : t < 6 ? Temperament.WARY : t < 8 ? Temperament.TRADERS : Temperament.RAIDERS;
        totem = TOTEMS[r.nextInt(TOTEMS.length)];
        DyeColor[] palette = COLOURS[temperament.ordinal()];
        base = palette[r.nextInt(palette.length)];
        int n = 2 + r.nextInt(2);
        layers = new ResourceKey<?>[n];
        layerColours = new DyeColor[n];
        for (int i = 0; i < n; i++) {
            layers[i] = PATTERNS.get(r.nextInt(PATTERNS.size()));
            DyeColor c;
            do c = DyeColor.byId(r.nextInt(16)); while (c == base);
            layerColours[i] = c;
        }
    }

    /** How many ages this tribe lags behind the world (WorldAge.npcAge): 0 or 1, fixed by its id. */
    public int ageLag() {
        return (int) Math.floorMod(id.getMostSignificantBits() ^ id.getLeastSignificantBits(), 2L);
    }

    public static Tribe of(UUID id) {
        return new Tribe(id);
    }

    /** "Raven Tribe". */
    public Component name() {
        return Component.translatable("tribe.mechanical.name", Component.translatable("tribe.mechanical.totem." + totem));
    }

    public DyeColor baseColour() {
        return base;
    }

    /** The tribe's flag as a banner item (base colour plus 2-3 patterns). */
    @SuppressWarnings("unchecked")
    public ItemStack banner(HolderLookup.Provider registries) {
        ItemStack stack = new ItemStack(Items.BANNER.pick(base));
        HolderGetter<BannerPattern> patterns = registries.lookupOrThrow(Registries.BANNER_PATTERN);
        BannerPatternLayers.Builder b = new BannerPatternLayers.Builder();
        for (int i = 0; i < layers.length; i++) b.addIfRegistered(patterns, (ResourceKey<BannerPattern>) layers[i], layerColours[i]);
        stack.set(DataComponents.BANNER_PATTERNS, b.build());
        stack.set(DataComponents.ITEM_NAME, Component.translatable("tribe.mechanical.banner", name()));
        return stack;
    }

    // ---- reputation ----

    /**
     * The standings a player goes by (tribe id -> -100..100): their player tribe's, shared by all its members (A4,
     * clan.Clans#sharedStandings), or their own outside one. Every read of a standing goes through here.
     */
    public static Map<String, Integer> standings(Player player) {
        Map<String, Integer> shared = com.mechanical.clan.Clans.sharedStandings(player);
        return shared != null ? shared : player.getData(REPUTATION);
    }

    public int reputation(Player player) {
        return standings(player).getOrDefault(id.toString(), temperament.start);
    }

    /** Changes the player's standing (their player tribe's if they have one; clamped) and tells them in the action bar. */
    public void addReputation(Player player, int delta) {
        int old = reputation(player), now = Mth.clamp(old + delta, MIN, MAX);
        if (now == old) return;
        if (!com.mechanical.clan.Clans.setShared(player, id.toString(), now)) {
            Map<String, Integer> map = new java.util.HashMap<>(player.getData(REPUTATION));
            map.put(id.toString(), now);
            player.setData(REPUTATION, Map.copyOf(map));
        }
        if (player instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("message.mechanical.tribe.standing", name(), standing(now)));
            if (now >= BEST_FRIENDS) TribeVisits.award(sp, "tribe_friends");
        }
    }

    /** Hostile, unfriendly, neutral, friendly or honoured. */
    public static Component standing(int rep) {
        String key = rep <= HOSTILE ? "hostile" : rep < 0 ? "unfriendly" : rep < FRIENDLY ? "neutral" : rep < 70 ? "friendly" : "honoured";
        return Component.translatable("tribe.mechanical.standing." + key);
    }

    public boolean hostileTo(Player player) {
        return reputation(player) <= HOSTILE;
    }

    public boolean trades(Player player) {
        return reputation(player) >= temperament.minTrade;
    }

    /** Price change for a cost of {@code count}: up to half off for honoured friends (more from traders), dearer when disliked. */
    public int priceDiff(Player player, int count) {
        int rep = reputation(player);
        double pct = rep >= 0 ? -Math.min(0.6, rep / 200.0 * temperament.discount) : -rep / 100.0;
        return (int) Math.round(count * pct);
    }

    public static void init() {}
}
