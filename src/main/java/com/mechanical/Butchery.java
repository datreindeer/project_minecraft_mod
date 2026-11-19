package com.mechanical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Passive animals don't drop loot when killed: they leave a {@link CarcassEntity} that must be butchered with a knife.
 * This table says what each animal yields. Counts and chances are for a flint knife; better knives multiply them.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Butchery {
    public record Extra(Supplier<Item> item, float chance, int min, int max) {}
    /** {@code outer} comes off in the first pass (wool, hide, feathers), then meat and {@code inner} in the second. */
    public record Yield(Supplier<Item> meat, int minMeat, int maxMeat, List<Extra> outer, List<Extra> inner) {}

    private static final String CARCASS_TAG = "mechanical.carcass";
    private static final Extra FEATHERS = new Extra(() -> Items.FEATHER, 0.8F, 1, 2);
    private static Extra feathers(int min, int max) {
        return new Extra(() -> Items.FEATHER, 1.0F, min, max);
    }
    private static Extra hide(float chance) {
        return new Extra(Mechanical.RAW_HIDE, chance, 1, 1);
    }

    // shortcut: hardcoded table; move to data (e.g. a data map) when modpacks need to change it.
    private static Map<EntityType<?>, Yield> yields;

    private static Map<EntityType<?>, Yield> createYields() {
        return Map.ofEntries(
            Map.entry(EntityTypes.COW, new Yield(Mechanical.BEEF_SCRAPS, 1, 3, List.of(hide(0.3F)), List.of())),
            Map.entry(EntityTypes.MOOSHROOM, new Yield(Mechanical.BEEF_SCRAPS, 1, 3, List.of(hide(0.3F)), List.of())),
            Map.entry(EntityTypes.PIG, new Yield(Mechanical.PORKCHOP_SCRAPS, 1, 3, List.of(hide(0.25F)), List.of())),
            // sheep: shear the wool first, then the hide comes off with the meat
            Map.entry(EntityTypes.SHEEP, new Yield(Mechanical.MUTTON_SCRAPS, 1, 2,
                    List.of(new Extra(Mechanical.WOOL_THREAD, 0.3F, 1, 2)), List.of(hide(0.25F)))),
            // birds: pluck the feathers first, then each gives its own meat (Birds; the plucked bird can also be taken whole)
            Map.entry(EntityTypes.CHICKEN, new Yield(() -> Items.CHICKEN, 1, 1, List.of(FEATHERS), List.of())),
            Map.entry(Wildlife.GOOSE.get(), new Yield(Birds.RAW_GOOSE, 2, 2, List.of(feathers(3, 5)), List.of())),
            Map.entry(Wildlife.ROCK_DOVE.get(), new Yield(Birds.RAW_PIGEON, 1, 1, List.of(feathers(1, 2)), List.of())),
            Map.entry(Wildlife.PTARMIGAN.get(), new Yield(Birds.RAW_PTARMIGAN, 1, 1, List.of(feathers(2, 3)), List.of())),
            Map.entry(EntityTypes.PARROT, new Yield(Mechanical.GAME_MEAT_SCRAPS, 1, 1, List.of(FEATHERS), List.of())),
            Map.entry(Wildlife.ROE_DEER.get(), new Yield(Wildlife.VENISON_SCRAPS, 1, 2, List.of(hide(0.3F)),
                    List.of(new Extra(Wildlife.SINEW, 0.4F, 1, 1)))),
            Map.entry(Wildlife.RED_DEER.get(), new Yield(Wildlife.VENISON_SCRAPS, 2, 4, List.of(hide(0.45F)),
                    List.of(new Extra(Wildlife.SINEW, 0.5F, 1, 2)))),
            Map.entry(Wildlife.WILD_BOAR.get(), new Yield(Mechanical.PORKCHOP_SCRAPS, 2, 3, List.of(hide(0.35F)),
                    List.of(new Extra(Wildlife.LARD, 0.5F, 1, 2), new Extra(Wildlife.TUSK, 0.4F, 1, 2)))),
            Map.entry(Wildlife.HARE.get(), new Yield(Mechanical.RABBIT_SCRAPS, 1, 1, List.of(new Extra(() -> Items.RABBIT_HIDE, 0.35F, 1, 1)),
                    List.of(new Extra(() -> Items.RABBIT_FOOT, 0.08F, 1, 1)))),
            Map.entry(Wildlife.DESERT_HARE.get(), new Yield(Mechanical.RABBIT_SCRAPS, 1, 1, List.of(new Extra(() -> Items.RABBIT_HIDE, 0.35F, 1, 1)),
                    List.of(new Extra(() -> Items.RABBIT_FOOT, 0.08F, 1, 1)))),
            Map.entry(Wildlife.PHEASANT.get(), new Yield(Birds.RAW_PHEASANT, 1, 1, List.of(feathers(2, 3)), List.of())),
            Map.entry(Wildlife.AUROCHS.get(), new Yield(Mechanical.BEEF_SCRAPS, 3, 5, List.of(hide(0.6F)),
                    List.of(new Extra(Wildlife.SINEW, 0.5F, 1, 2), new Extra(Wildlife.HORN, 0.5F, 1, 2)))),
            Map.entry(Wildlife.WILD_HORSE.get(), new Yield(Mechanical.GAME_MEAT_SCRAPS, 2, 4, List.of(hide(0.5F)),
                    List.of(new Extra(Wildlife.SINEW, 0.5F, 1, 2)))),
            // mouflon: like the sheep, the wool comes off first, then the hide with the meat
            Map.entry(Wildlife.MOUFLON.get(), new Yield(Mechanical.MUTTON_SCRAPS, 1, 3,
                    List.of(new Extra(Mechanical.WOOL_THREAD, 0.4F, 1, 2)),
                    List.of(hide(0.3F), new Extra(Wildlife.SINEW, 0.3F, 1, 1), new Extra(Wildlife.HORN, 0.3F, 1, 2)))),
            Map.entry(Wildlife.BROWN_BEAR.get(), new Yield(Mechanical.GAME_MEAT_SCRAPS, 4, 6,
                    List.of(new Extra(Wildlife.RAW_BEAR_PELT, 1.0F, 1, 1), hide(0.4F)),
                    List.of(new Extra(Wildlife.BEAR_FAT, 0.8F, 1, 3), new Extra(Wildlife.BEAR_CLAW, 0.6F, 1, 3),
                            new Extra(Wildlife.SINEW, 0.6F, 1, 2)))),
            // night predators: the wolf's pelt comes off first; bones come out with the meat
            Map.entry(Wildlife.GREY_WOLF.get(), new Yield(Mechanical.GAME_MEAT_SCRAPS, 1, 2, List.of(new Extra(Wildlife.RAW_WOLF_PELT, 1.0F, 1, 1)),
                    List.of(new Extra(Wildlife.SINEW, 0.5F, 1, 1), new Extra(() -> Items.BONE, 0.6F, 1, 2)))),
            Map.entry(Wildlife.CAVE_LION.get(), new Yield(Mechanical.GAME_MEAT_SCRAPS, 3, 5, List.of(hide(0.7F), hide(0.4F)),
                    List.of(new Extra(Wildlife.SINEW, 0.6F, 1, 2), new Extra(() -> Items.BONE, 0.7F, 1, 3)))),
            Map.entry(Wildlife.MOOSE.get(), new Yield(Wildlife.VENISON_SCRAPS, 3, 6, List.of(hide(0.7F)),
                    List.of(new Extra(Wildlife.SINEW, 0.6F, 1, 3)))),
            Map.entry(Wildlife.REINDEER.get(), new Yield(Wildlife.VENISON_SCRAPS, 2, 3, List.of(hide(0.45F)),
                    List.of(new Extra(Wildlife.SINEW, 0.5F, 1, 2)))),
            // small forest animals: little meat; the squirrel's pelt comes off first
            Map.entry(Wildlife.SQUIRREL.get(), new Yield(Mechanical.GAME_MEAT_SCRAPS, 1, 1, List.of(new Extra(Wildlife.SQUIRREL_PELT, 0.7F, 1, 1)), List.of())),
            Map.entry(Wildlife.HEDGEHOG.get(), new Yield(Mechanical.GAME_MEAT_SCRAPS, 0, 1, List.of(), List.of())),
            Map.entry(Wildlife.BADGER.get(), new Yield(Mechanical.GAME_MEAT_SCRAPS, 1, 2, List.of(hide(0.35F)),
                    List.of(new Extra(Wildlife.LARD, 0.4F, 1, 1)))),
            // giant deer: a mountain of meat, and always the giant antler (exactly one: chance 1 is never scaled into a count)
            Map.entry(Wildlife.GIANT_DEER.get(), new Yield(Mechanical.GAME_MEAT_SCRAPS, 6, 9, List.of(hide(1.0F), hide(0.6F)),
                    List.of(new Extra(Wildlife.GIANT_ANTLER, 1.0F, 1, 1), new Extra(Wildlife.SINEW, 0.8F, 2, 3)))),
            // phantom: undead, so no meat; the membrane hide comes off first, then rotten flesh and maybe a membrane
            Map.entry(EntityTypes.PHANTOM, new Yield(() -> Items.ROTTEN_FLESH, 1, 2, List.of(new Extra(Mechanical.RAW_PHANTOM_HIDE, 0.6F, 1, 1)),
                    List.of(new Extra(() -> Items.PHANTOM_MEMBRANE, 0.5F, 1, 1)))),
            Map.entry(EntityTypes.RABBIT, new Yield(Mechanical.RABBIT_SCRAPS, 1, 1,
                    List.of(new Extra(() -> Items.RABBIT_HIDE, 0.3F, 1, 1)), List.of(new Extra(() -> Items.RABBIT_FOOT, 0.05F, 1, 1)))));
    }

    /** Animals not in the table: game meat and hide scaled by body size. */
    private static Yield bySize(EntityType<?> type) {
        float size = Math.max(type.getWidth(), type.getHeight());
        if (size < 0.7F) return new Yield(Mechanical.GAME_MEAT_SCRAPS, 1, 1, List.of(hide(0.1F)), List.of());
        if (size < 1.3F) return new Yield(Mechanical.GAME_MEAT_SCRAPS, 1, 2, List.of(hide(0.25F)), List.of());
        return new Yield(Mechanical.GAME_MEAT_SCRAPS, 2, 3, List.of(hide(0.4F)), List.of());
    }

    public static Yield yieldFor(EntityType<?> type) {
        if (yields == null) {
            Map<EntityType<?>, Yield> all = new HashMap<>(createYields());
            for (var e : REGISTERED) all.put(e.type().get(), e.yield());
            yields = all;
        }
        return yields.getOrDefault(type, bySize(type));
    }

    private record Registered(Supplier<? extends EntityType<?>> type, Yield yield) {}
    private static final List<Registered> REGISTERED = new ArrayList<>();
    private static final List<Supplier<? extends EntityType<?>>> ALWAYS_CARCASS = new ArrayList<>(), NEVER_CARCASS = new ArrayList<>();

    /**
     * Adds an animal's yield from its own feature class (call during mod construction, e.g. from an init()), so new animal
     * groups don't have to edit the table above. Registered yields override the table.
     */
    public static void register(Supplier<? extends EntityType<?>> type, Yield yield) {
        REGISTERED.add(new Registered(type, yield));
    }

    /** A creature that isn't an {@link Animal} (a fish-like water mob, a snake ...) that should still leave a carcass. */
    public static void alwaysCarcass(Supplier<? extends EntityType<?>> type) {
        ALWAYS_CARCASS.add(type);
    }

    /** An animal that should not leave a carcass (insect swarms, tiny critters that just drop items). */
    public static void neverCarcass(Supplier<? extends EntityType<?>> type) {
        NEVER_CARCASS.add(type);
    }

    private static boolean listed(List<Supplier<? extends EntityType<?>>> list, EntityType<?> type) {
        for (var s : list) if (s.get() == type) return true;
        return false;
    }

    /** Wild, grown passive animals (and phantoms) become carcasses; pets, babies and undead horses keep vanilla behaviour. */
    static boolean leavesCarcass(LivingEntity entity) {
        // phantoms only when a player killed them (like the vanilla membrane); those burnt by the sun just vanish
        if (entity.getType() == EntityTypes.PHANTOM) return entity.getLastHurtByPlayer() != null;
        if (listed(NEVER_CARCASS, entity.getType())) return false;
        if (listed(ALWAYS_CARCASS, entity.getType())) return !entity.isBaby();
        if (!(entity instanceof Animal animal) || animal.isBaby()) return false;
        // crows and gulls just drop their feathers (loot tables); the edible flock birds leave a carcass
        if (animal instanceof com.mechanical.wildlife.FlockBird && !Birds.isBird(animal.getType())) return false;
        if (animal instanceof TamableAnimal tame && tame.isTame()) return false;
        if (animal instanceof AbstractHorse horse && horse.isTamed()) return false;
        return entity.getType() != EntityTypes.SKELETON_HORSE && entity.getType() != EntityTypes.ZOMBIE_HORSE;
    }

    @SubscribeEvent
    static void onDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !leavesCarcass(entity)) return;
        event.setCanceled(true);
        // the carcass appears when the death animation ends, where the body came to rest (see onLeave)
        entity.addTag(CARCASS_TAG);
    }

    @SubscribeEvent
    static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof LivingEntity entity
                && entity.getRemovalReason() == Entity.RemovalReason.KILLED && entity.entityTags().contains(CARCASS_TAG)) {
            level.addFreshEntity(CarcassEntity.of(level, entity));
        }
    }

    private record KnifeTier(Supplier<? extends EntityType<?>> type, int tier) {}
    private static final List<KnifeTier> KNIFE_TIERS = new ArrayList<>();

    /** From a feature class: this animal's hide is too thick for less than knife {@code tier} (e.g. {@link KnifeItem#TUSK}). */
    public static void requireKnife(Supplier<? extends EntityType<?>> type, int tier) {
        KNIFE_TIERS.add(new KnifeTier(type, tier));
    }

    /** Knife tier needed to butcher this animal at all (thick hides like the bear's need iron). */
    public static int requiredKnifeTier(EntityType<?> animal) {
        for (var e : KNIFE_TIERS) if (e.type().get() == animal) return e.tier();
        return animal == Wildlife.BROWN_BEAR.get() || animal == Wildlife.GIANT_DEER.get() ? KnifeItem.TUSK : KnifeItem.FLINT;
    }

    /**
     * First pass: wool / hide / feathers; a skinning knife ({@code hideMultiplier} > 1) also gets proportionally more of
     * each piece. On a Hanging Rack ({@code hung}) the skin comes off whole: every piece drops, no chance roll.
     */
    static void dropOuter(ServerLevel level, Entity at, EntityType<?> type, float hideMultiplier, boolean hung) {
        RandomSource random = level.getRandom();
        for (Extra e : yieldFor(type).outer()) {
            if (!hung && random.nextFloat() >= e.chance() * hideMultiplier) continue;
            float count = (e.min() + random.nextInt(e.max() - e.min() + 1)) * Math.max(1, hideMultiplier);
            int whole = (int) count;
            drop(level, at, e.item().get(), whole + (random.nextFloat() < count - whole ? 1 : 0));
        }
    }

    /**
     * Second pass: the meat and what is inside; {@code trophy} = the animal carried antlers (stag), which come off too.
     * On a Hanging Rack ({@code hung}) the meat is twice the scraps (x1.25 more if {@code aged}), cut into Haunches, Ribs and
     * Loins ({@link #cuts}), and fat, sinew and bone always come out, one more of each. A {@code spoiled} body gives
     * Rotten Flesh for its meat.
     */
    static void dropInner(ServerLevel level, Entity at, EntityType<?> type, float yieldMultiplier, boolean trophy, boolean hung, boolean aged, boolean spoiled) {
        if (trophy) drop(level, at, Wildlife.ANTLER.get(), 1 + level.getRandom().nextInt(2));
        RandomSource random = level.getRandom();
        Yield y = yieldFor(type);
        int scraps = Math.round((y.minMeat() + random.nextInt(y.maxMeat() - y.minMeat() + 1)) * yieldMultiplier);
        Item meat = y.meat().get();
        if (Birds.isBird(type)) {
            // a bird has no big cuts and no bones worth taking: hung game just gains a piece once it has aged
            drop(level, at, spoiled ? Items.ROTTEN_FLESH : meat, scraps + (hung && aged ? 1 : 0));
            return;
        }
        if (spoiled || meat == Items.ROTTEN_FLESH) drop(level, at, Items.ROTTEN_FLESH, hung ? scraps * 2 : scraps);
        else if (!hung) drop(level, at, meat, scraps);
        else {
            int[] cuts = cuts(Math.round(scraps * 2 * (aged ? 1.25F : 1.0F)));
            drop(level, at, CarcassHauling.RAW_HAUNCH.get(), cuts[0]);
            drop(level, at, CarcassHauling.RAW_RIBS.get(), cuts[1]);
            drop(level, at, CarcassHauling.RAW_LOIN.get(), cuts[2]);
            drop(level, at, meat, cuts[3]);
        }
        if (!hung) {
            dropExtras(level, at, y.inner(), yieldMultiplier);
            return;
        }
        for (Extra e : y.inner()) {
            Item item = e.item().get();
            boolean body = item == Wildlife.SINEW.get() || item == Wildlife.LARD.get() || item == Wildlife.BEAR_FAT.get() || item == Items.BONE;
            if (body) drop(level, at, item, e.min() + 1 + random.nextInt(e.max() - e.min() + 1));
            else if (random.nextFloat() < e.chance() * yieldMultiplier) drop(level, at, item, e.min() + random.nextInt(e.max() - e.min() + 1));
        }
        // the skeleton comes apart on the rack: bones for every body (big ones give more)
        if (y.inner().stream().noneMatch(e -> e.item().get() == Items.BONE)) drop(level, at, Items.BONE, 1 + Math.round(type.getWidth()) + random.nextInt(2));
    }

    /** Cost in meat units (1 unit = 1 scrap = 1 Smoked Meat) of a Haunch, Ribs and Loin, and how many of each one body has. */
    static final int[] CUT_UNITS = {3, 2, 2}, CUT_MAX = {4, 2, 2};

    /** Meat {@code units} as {haunches, ribs, loins, leftover scraps}: taken in turn, each while it fits and the body has one left. */
    static int[] cuts(int units) {
        int[] out = new int[4];
        for (boolean took = true; took; ) {
            took = false;
            for (int i = 0; i < 3; i++) {
                if (units >= CUT_UNITS[i] && out[i] < CUT_MAX[i]) {
                    out[i]++;
                    units -= CUT_UNITS[i];
                    took = true;
                }
            }
        }
        out[3] = units;
        return out;
    }

    static {
        // self-check: units are never lost; small bodies give ribs/loin, big ones the full set plus scraps
        assert java.util.Arrays.equals(cuts(2), new int[] {0, 1, 0, 0});
        assert java.util.Arrays.equals(cuts(7), new int[] {1, 1, 1, 0});
        assert java.util.Arrays.equals(cuts(25), new int[] {4, 2, 2, 5});
    }

    private static void dropExtras(ServerLevel level, Entity at, List<Extra> extras, float yieldMultiplier) {
        RandomSource random = level.getRandom();
        for (Extra e : extras) {
            if (random.nextFloat() < e.chance() * yieldMultiplier) drop(level, at, e.item().get(), e.min() + random.nextInt(e.max() - e.min() + 1));
        }
    }

    private static void drop(ServerLevel level, Entity at, Item item, int count) {
        if (count > 0) at.spawnAtLocation(level, new ItemStack(item, count));
    }

    private Butchery() {}
}
