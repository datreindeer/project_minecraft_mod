package com.mechanical;

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
            // birds: pluck the feathers first
            Map.entry(EntityTypes.CHICKEN, new Yield(Mechanical.CHICKEN_SCRAPS, 1, 1, List.of(FEATHERS), List.of())),
            Map.entry(EntityTypes.PARROT, new Yield(Mechanical.GAME_MEAT_SCRAPS, 1, 1, List.of(FEATHERS), List.of())),
            Map.entry(Wildlife.ROE_DEER.get(), new Yield(Wildlife.VENISON_SCRAPS, 1, 2, List.of(hide(0.3F)),
                    List.of(new Extra(Wildlife.SINEW, 0.4F, 1, 1)))),
            Map.entry(Wildlife.RED_DEER.get(), new Yield(Wildlife.VENISON_SCRAPS, 2, 4, List.of(hide(0.45F)),
                    List.of(new Extra(Wildlife.SINEW, 0.5F, 1, 2)))),
            Map.entry(Wildlife.WILD_BOAR.get(), new Yield(Mechanical.PORKCHOP_SCRAPS, 2, 3, List.of(hide(0.35F)),
                    List.of(new Extra(Wildlife.LARD, 0.5F, 1, 2), new Extra(Wildlife.TUSK, 0.4F, 1, 2)))),
            Map.entry(Wildlife.HARE.get(), new Yield(Mechanical.RABBIT_SCRAPS, 1, 1, List.of(new Extra(() -> Items.RABBIT_HIDE, 0.35F, 1, 1)),
                    List.of(new Extra(() -> Items.RABBIT_FOOT, 0.08F, 1, 1)))),
            Map.entry(Wildlife.PHEASANT.get(), new Yield(Mechanical.CHICKEN_SCRAPS, 1, 1, List.of(new Extra(() -> Items.FEATHER, 1.0F, 2, 3)), List.of())),
            Map.entry(Wildlife.AUROCHS.get(), new Yield(Mechanical.BEEF_SCRAPS, 3, 5, List.of(hide(0.6F)),
                    List.of(new Extra(Wildlife.SINEW, 0.5F, 1, 2), new Extra(Wildlife.HORN, 0.5F, 1, 2)))),
            Map.entry(Wildlife.WILD_HORSE.get(), new Yield(Mechanical.GAME_MEAT_SCRAPS, 2, 4, List.of(hide(0.5F)),
                    List.of(new Extra(Wildlife.SINEW, 0.5F, 1, 2)))),
            // mouflon: like the sheep, the wool comes off first, then the hide with the meat
            Map.entry(Wildlife.MOUFLON.get(), new Yield(Mechanical.MUTTON_SCRAPS, 1, 3,
                    List.of(new Extra(Mechanical.WOOL_THREAD, 0.4F, 1, 2)),
                    List.of(hide(0.3F), new Extra(Wildlife.SINEW, 0.3F, 1, 1), new Extra(Wildlife.HORN, 0.3F, 1, 2)))),
            Map.entry(Wildlife.BROWN_BEAR.get(), new Yield(Mechanical.GAME_MEAT_SCRAPS, 4, 6,
                    List.of(new Extra(Wildlife.BEAR_PELT, 1.0F, 1, 1), hide(0.4F)),
                    List.of(new Extra(Wildlife.BEAR_FAT, 0.8F, 1, 3), new Extra(Wildlife.BEAR_CLAW, 0.6F, 1, 3),
                            new Extra(Wildlife.SINEW, 0.6F, 1, 2)))),
            Map.entry(Wildlife.MOOSE.get(), new Yield(Wildlife.VENISON_SCRAPS, 3, 6, List.of(hide(0.7F)),
                    List.of(new Extra(Wildlife.SINEW, 0.6F, 1, 3)))),
            Map.entry(Wildlife.REINDEER.get(), new Yield(Wildlife.VENISON_SCRAPS, 2, 3, List.of(hide(0.45F)),
                    List.of(new Extra(Wildlife.SINEW, 0.5F, 1, 2)))),
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
        if (yields == null) yields = createYields();
        return yields.getOrDefault(type, bySize(type));
    }

    /** Wild, grown passive animals become carcasses; pets, babies and undead horses keep vanilla behaviour. */
    static boolean leavesCarcass(LivingEntity entity) {
        if (!(entity instanceof Animal animal) || animal.isBaby()) return false;
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

    /** Knife tier needed to butcher this animal at all (thick hides like the bear's need iron). */
    public static int requiredKnifeTier(EntityType<?> animal) {
        return animal == Wildlife.BROWN_BEAR.get() ? KnifeItem.IRON : KnifeItem.FLINT;
    }

    /** First pass: wool / hide / feathers. {@code yieldMultiplier} comes from the knife (flint = 1). */
    static void dropOuter(ServerLevel level, Entity at, EntityType<?> type, float yieldMultiplier) {
        dropExtras(level, at, yieldFor(type).outer(), yieldMultiplier);
    }

    /** Second pass: the meat and what is inside; {@code trophy} = the animal carried antlers (stag), which come off too. */
    static void dropInner(ServerLevel level, Entity at, EntityType<?> type, float yieldMultiplier, boolean trophy) {
        if (trophy) drop(level, at, Wildlife.ANTLER.get(), 1 + level.getRandom().nextInt(2));
        RandomSource random = level.getRandom();
        Yield y = yieldFor(type);
        drop(level, at, y.meat().get(), Math.round((y.minMeat() + random.nextInt(y.maxMeat() - y.minMeat() + 1)) * yieldMultiplier));
        dropExtras(level, at, y.inner(), yieldMultiplier);
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
