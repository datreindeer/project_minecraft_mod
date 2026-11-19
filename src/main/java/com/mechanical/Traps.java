package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Traps: food without hunting yourself. A {@link SnareBlock} on the ground catches small game (the animal dies and
 * leaves a normal Carcass); a {@link FishTrapBlock} in a stream fills up with whole fish. Snares lure small animals
 * walking nearby (this class gives them the {@link LureGoal}). Data: scripts/gen_traps.py, textures:
 * scripts/gen_trap_textures.py, numbers in docs/ages/01-stone-age.md.
 */
@EventBusSubscriber(modid = MODID)
public final class Traps {
    /** Laid on a snare to draw small animals from {@link SnareBlock#BAIT_RANGE} blocks. */
    public static final TagKey<Item> SNARE_BAIT = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "snare_bait"));
    /** Small game: neither side of the animal's hitbox is bigger than this (hare 0.5, pheasant and badger 0.7, fox 0.7). */
    public static final float SMALL_GAME = 0.75F;

    public static final DeferredBlock<SnareBlock> SNARE = BLOCKS.registerBlock("snare", SnareBlock::new, p -> p
            .mapColor(MapColor.WOOD).noCollision().strength(0.3F).sound(SoundType.GRASS).noOcclusion().pushReaction(PushReaction.POPPED));
    public static final DeferredItem<BlockItem> SNARE_ITEM = ITEMS.registerSimpleBlockItem("snare", SNARE);
    public static final DeferredBlock<FishTrapBlock> FISH_TRAP = BLOCKS.registerBlock("fish_trap", FishTrapBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(0.6F).sound(SoundType.WOOD).noOcclusion().randomTicks().pushReaction(PushReaction.POPPED));
    public static final DeferredItem<BlockItem> FISH_TRAP_ITEM = ITEMS.registerSimpleBlockItem("fish_trap", FISH_TRAP);

    public static void init() {}

    /** Wild, grown small animals that would leave a carcass (birds of the flocks, pets, babies and big game don't count). */
    public static boolean isSmallGame(net.minecraft.world.entity.Entity entity) {
        return entity instanceof Animal animal && Butchery.leavesCarcass(animal) && AnimalFear.isWild(animal) && !animal.isInWater()
                && Math.max(animal.getBbWidth(), animal.getBbHeight()) <= SMALL_GAME;
    }

    /** Server side: the snare each lured animal is walking to, and until when it keeps trying. */
    private record Lure(BlockPos snare, long until) {}
    private static final Map<Animal, Lure> LURES = new WeakHashMap<>();

    static boolean isLured(Animal animal) {
        Lure l = LURES.get(animal);
        return l != null && l.until > animal.level().getGameTime();
    }

    /** Sends the animal to the snare at {@code pos} for up to 30 seconds. */
    static void lure(Animal animal, BlockPos pos) {
        LURES.put(animal, new Lure(pos.immutable(), animal.level().getGameTime() + 600));
    }

    static void forget(Animal animal) {
        LURES.remove(animal);
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        // every animal gets the goal (size can't be read reliably before it is added); it only runs once a snare lured it
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Animal animal && !(animal instanceof com.mechanical.wildlife.FlockBird)) {
            animal.goalSelector.addGoal(2, new LureGoal(animal));
        }
    }

    /** Walks a lured animal onto its snare. Fear (priority 0) still wins: an animal that notices a player runs off instead. */
    static class LureGoal extends Goal {
        private final Animal animal;
        private int repath;

        LureGoal(Animal animal) {
            this.animal = animal;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private BlockPos target() {
            Lure l = LURES.get(animal);
            return l == null ? null : l.snare;
        }

        @Override
        public boolean canUse() {
            if (!isLured(animal) || AnimalFear.isPanicking(animal)) return false;
            BlockPos pos = target();
            if (animal.level().getBlockState(pos).is(SNARE.get()) && !animal.level().getBlockState(pos).getValue(SnareBlock.SPRUNG)) return true;
            forget(animal); // sprung or gone meanwhile
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            repath = 0;
        }

        @Override
        public void stop() {
            animal.getNavigation().stop();
        }

        @Override
        public void tick() {
            BlockPos pos = target();
            animal.getLookControl().setLookAt(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            if (--repath > 0) return;
            repath = 20;
            if (animal.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 0.6) {
                // close but not quite inside the snare cell: step the last bit
                animal.getMoveControl().setWantedPosition(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.8);
            } else {
                animal.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.8);
            }
        }
    }
}
