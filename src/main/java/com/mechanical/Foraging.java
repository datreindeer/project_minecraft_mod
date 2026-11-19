package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Wild food to forage: rare berry bushes (pick ripe berries, they grow back) and mushrooms (pick them; they spread slowly
 * in the shade). Neither can be planted yet. Worldgen is in data (gen_data.py), the blocks in {@link WildBerryBushBlock}
 * and {@link WildMushroomBlock}.
 */
public final class Foraging {
    /** bush name -> berries item name */
    public static final Map<String, String> BUSHES = new LinkedHashMap<>();
    static {
        BUSHES.put("raspberry", "raspberries");
        BUSHES.put("blueberry", "blueberries");
        BUSHES.put("lingonberry", "lingonberries");
        BUSHES.put("wild_strawberry", "wild_strawberries");
        BUSHES.put("elderberry", "elderberries");
        BUSHES.put("cloudberry", "cloudberries");
        BUSHES.put("blackberry", "blackberries");
    }
    public static final List<String> MUSHROOMS = List.of("chanterelle", "porcini", "morel", "oyster_mushroom", "fly_agaric", "puffball");

    public static final Map<String, DeferredItem<Item>> BERRIES = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<WildBerryBushBlock>> BUSH_BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> MUSHROOM_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredBlock<WildMushroomBlock>> MUSHROOM_BLOCKS = new LinkedHashMap<>();

    private static final FoodProperties BERRY = new FoodProperties.Builder().nutrition(1).saturationModifier(0.3F).build();
    private static final FoodProperties RAW_MUSHROOM = new FoodProperties.Builder().nutrition(1).saturationModifier(0.2F).build();
    /** Fly agaric: poisonous, and makes you sick. */
    private static final Consumable FLY_AGARIC = Consumables.defaultFood()
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.POISON, 200, 0),
                    new MobEffectInstance(MobEffects.NAUSEA, 300, 0)))).build();

    static {
        BUSHES.forEach((bush, berries) -> {
            DeferredItem<Item> item = ITEMS.registerSimpleItem(berries, p -> p.food(BERRY));
            BERRIES.put(bush, item);
            BUSH_BLOCKS.put(bush, BLOCKS.registerBlock(bush + "_bush", p -> new WildBerryBushBlock(item, p), p -> p
                    .mapColor(MapColor.PLANT).noCollision().randomTicks().strength(0.3F).sound(SoundType.SWEET_BERRY_BUSH)
                    .pushReaction(PushReaction.POPPED)));
        });
        for (String name : MUSHROOMS) {
            DeferredItem<Item> item = name.equals("fly_agaric")
                    ? ITEMS.registerSimpleItem(name, p -> p.food(RAW_MUSHROOM, FLY_AGARIC))
                    : ITEMS.registerSimpleItem(name, p -> p.food(RAW_MUSHROOM));
            MUSHROOM_ITEMS.put(name, item);
            MUSHROOM_BLOCKS.put(name, BLOCKS.registerBlock(name, p -> new WildMushroomBlock(item, p), p -> p
                    .mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.GRASS)
                    .pushReaction(PushReaction.POPPED)));
        }
    }

    public static final DeferredItem<Item> ROASTED_MUSHROOMS = ITEMS.registerSimpleItem("roasted_mushrooms",
            p -> p.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6F).build()));
    public static final TagKey<net.minecraft.world.level.block.Block> WILD_BERRY_BUSHES =
            TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Mechanical.MODID, "wild_berry_bushes"));

    public static void init() {}

    /** Is there a wild berry bush within {@code radius} blocks (checked every other block; bushes grow in groups)? */
    public static boolean nearBerryBush(LevelReader level, BlockPos pos, int radius) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx += 2) {
            for (int dz = -radius; dz <= radius; dz += 2) {
                for (int dy = -4; dy <= 4; dy++) {
                    if (level.getBlockState(p.setWithOffset(pos, dx, dy, dz)).is(WILD_BERRY_BUSHES)) return true;
                }
            }
        }
        return false;
    }

    private Foraging() {}
}
