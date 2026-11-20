package com.mechanical.people;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/** Stone Age people: the tribesperson entity, its spawn egg, and the camp / hunting band worldgen features. */
@EventBusSubscriber(modid = "mechanical")
public final class People {
    public static final DeferredHolder<EntityType<?>, EntityType<Tribesperson>> TRIBESPERSON = ENTITY_TYPES.registerEntityType(
            "tribesperson", Tribesperson::new, MobCategory.MISC, b -> b.sized(0.6F, 1.8F).eyeHeight(1.62F).clientTrackingRange(10));
    public static final DeferredItem<SpawnEggItem> TRIBESPERSON_SPAWN_EGG = ITEMS.registerItem("tribesperson_spawn_egg", SpawnEggItem::new,
            p -> p.spawnEgg(TRIBESPERSON.get()));

    /** Palisade stake tops for tribe villages (items come from Mechanical.registerMissingBlockItems). */
    public static final DeferredBlock<SharpenedLogBlock> SHARPENED_OAK_LOG = sharpened("sharpened_oak_log", MapColor.WOOD);
    public static final DeferredBlock<SharpenedLogBlock> SHARPENED_SPRUCE_LOG = sharpened("sharpened_spruce_log", MapColor.PODZOL);

    /** Sewn hides: tent walls, and the slopes and ridge of an A-frame tent (tribe camps build them, see Neighbours). */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> HIDE_CANVAS = BLOCKS.registerSimpleBlock("hide_canvas",
            p -> p.mapColor(MapColor.TERRACOTTA_BROWN).strength(0.8F).sound(SoundType.WOOL).ignitedByLava());
    public static final DeferredBlock<HideTentBlock> HIDE_TENT_SLOPE = BLOCKS.registerBlock("hide_tent_slope", HideTentBlock::new,
            p -> p.mapColor(MapColor.TERRACOTTA_BROWN).strength(0.8F).sound(SoundType.WOOL).ignitedByLava().noOcclusion());
    public static final DeferredBlock<HideTentBlock.Ridge> HIDE_TENT_RIDGE = BLOCKS.registerBlock("hide_tent_ridge", HideTentBlock.Ridge::new,
            p -> p.mapColor(MapColor.TERRACOTTA_BROWN).strength(0.8F).sound(SoundType.WOOL).ignitedByLava().noOcclusion());

    private static DeferredBlock<SharpenedLogBlock> sharpened(String name, MapColor color) {
        return BLOCKS.registerBlock(name, SharpenedLogBlock::new, p -> p.mapColor(color).strength(2.0F).sound(SoundType.WOOD).ignitedByLava().noOcclusion());
    }

    /** Call from the mod constructor: loads the registrations and registers the worldgen features. */
    public static void init(IEventBus modEventBus) {
        com.mechanical.worldgen.PeopleFeatures.FEATURE_TYPES.register(modEventBus);
        Tribe.init();
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(TRIBESPERSON.get(), Tribesperson.createAttributes().build());
    }

    private People() {}
}
