package com.mechanical.folklore;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import java.util.List;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.FlockBird;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Folklore, steps 0-2 of docs/folklore-plan.md: the Second Sight effect and the Fly Agaric that brings it (raw in
 * Foraging, Boiled Fly Agaric here), the Glimmering Stump and the Mountain Watcher. Faith and sight: {@link Faith}.
 * Data: scripts/gen_folklore.py, textures: scripts/gen_folklore_textures.py, client: client.folklore.FolkloreClient.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Folklore {
    static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Mechanical.MODID);
    /** +50 sight while it lasts; the client draws a soft coloured vignette and a gentle sway. */
    public static final DeferredHolder<MobEffect, MobEffect> SECOND_SIGHT = MOB_EFFECTS.register("second_sight",
            () -> new MobEffect(MobEffectCategory.NEUTRAL, 0xB070E0) {});

    /** Raw Fly Agaric (Foraging): poison and nausea as before, plus 20 s Second Sight. */
    public static Consumable rawFlyAgaric() {
        return Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.POISON, 200, 0),
                new MobEffectInstance(MobEffects.NAUSEA, 300, 0), new MobEffectInstance(SECOND_SIGHT, 400, 0)))).build();
    }

    /** Boiled on a campfire (the poison boils out): a short spell of nausea and 3 min Second Sight. */
    public static final DeferredItem<Item> BOILED_FLY_AGARIC = ITEMS.registerSimpleItem("boiled_fly_agaric", p -> p.food(
            new FoodProperties.Builder().nutrition(1).saturationModifier(0.2F).alwaysEdible().build(),
            Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.NAUSEA, 100, 0),
                    new MobEffectInstance(SECOND_SIGHT, 3600, 0)))).build()));

    public static final DeferredBlock<GlimmeringStumpBlock> GLIMMERING_STUMP = BLOCKS.registerBlock("glimmering_stump", GlimmeringStumpBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> GLIMMERING_STUMP_ITEM = ITEMS.registerSimpleBlockItem("glimmering_stump", GLIMMERING_STUMP);
    /** What a Glimmering Stump takes as a gift: berries and nuts. */
    public static final TagKey<Item> OFFERINGS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Mechanical.MODID, "stump_offerings"));

    public static final DeferredHolder<EntityType<?>, EntityType<MountainWatcher>> MOUNTAIN_WATCHER = ENTITY_TYPES.registerEntityType(
            "mountain_watcher", MountainWatcher::new, MobCategory.CREATURE, b -> b.sized(0.9F, 1.1F).clientTrackingRange(16));
    public static final DeferredItem<SpawnEggItem> MOUNTAIN_WATCHER_SPAWN_EGG = ITEMS.registerItem("mountain_watcher_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(MOUNTAIN_WATCHER.get()));
    /** Dropped once a year per player by a Mountain Watcher: a trophy. */
    public static final DeferredItem<Item> WATCHER_FEATHER = ITEMS.registerSimpleItem("watcher_feather", p -> p.stacksTo(16).rarity(Rarity.RARE));

    public static void init(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
        Faith.selfCheck();
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(MOUNTAIN_WATCHER.get(), FlockBird.createAttributes(20.0).build());
    }

    @SubscribeEvent
    static void spawnRules(RegisterSpawnPlacementsEvent event) {
        event.register(MOUNTAIN_WATCHER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> MountainWatcher.canSpawn(level, pos), RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private Folklore() {}
}
