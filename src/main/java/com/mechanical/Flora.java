package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Flora phase 1 (docs/flora-and-food-plan.md): wild flowers for the sparse biomes, two magic ones, and thorny berry
 * bushes that grow into thickets. Blocks in {@link WildFlowerBlock}, {@link TallWildFlowerBlock} and
 * {@link ThicketBushBlock}; data and worldgen (climate windows) in scripts/gen_flora.py, textures in
 * scripts/gen_flora_textures.py.
 */
public final class Flora {
    static final int SPRING = 1, SUMMER = 2, AUTUMN = 4, WINTER = 8;

    /** A flower kind: bloom seasons (mask), grows on sand/dry ground too, height in px (tall = two blocks). */
    record Flower(String name, int seasons, boolean dry, int height, boolean tall) {}

    static final List<Flower> FLOWERS = List.of(
            new Flower("pasque_flower", SPRING, false, 8, false),
            new Flower("harebell", SUMMER | AUTUMN, false, 11, false),
            new Flower("lady_slipper", SPRING | SUMMER, false, 10, false),
            new Flower("sea_thrift", SPRING | SUMMER, true, 8, false),
            new Flower("mountain_avens", SUMMER, false, 5, false),
            new Flower("edelweiss", SUMMER, false, 7, false),
            new Flower("sand_verbena", SPRING | SUMMER, true, 7, false),
            new Flower("desert_marigold", SPRING | SUMMER | AUTUMN, true, 9, false),
            new Flower("great_mullein", SUMMER, true, 16, true),
            new Flower("desert_candle", SPRING | SUMMER, true, 16, true));

    public static final Map<String, DeferredBlock<? extends Block>> FLOWER_BLOCKS = new LinkedHashMap<>();

    private static BlockBehaviour.Properties flower(BlockBehaviour.Properties p) {
        return p.mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.POPPED);
    }

    static {
        for (Flower f : FLOWERS) {
            var bloom = new WildFlowerBlock.Bloom(f.seasons(), false);
            FLOWER_BLOCKS.put(f.name(), f.tall()
                    ? BLOCKS.registerBlock(f.name(), p -> new TallWildFlowerBlock(bloom, f.dry(), p), Flora::flower)
                    : BLOCKS.registerBlock(f.name(), p -> new WildFlowerBlock(bloom, f.dry(), f.height(), null, p), Flora::flower));
        }
    }

    /** Fern Flower: a fern that blooms only on summer nights, glowing gold; picked in bloom it brings luck (folklore). */
    public static final DeferredBlock<WildFlowerBlock> FERN_FLOWER = BLOCKS.registerBlock("fern_flower",
            p -> new WildFlowerBlock(new WildFlowerBlock.Bloom(SUMMER, true), false, 12, new DustParticleOptions(0xFFD45A, 0.5F), p) {
                @Override
                public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
                    if (!level.isClientSide() && state.getValue(BLOOMING)) player.addEffect(new MobEffectInstance(MobEffects.LUCK, LUCK_TICKS));
                    return super.playerWillDestroy(level, pos, state, player);
                }
            }, p -> flower(p).lightLevel(s -> s.getValue(WildFlowerBlock.BLOOMING) ? 9 : 0));
    /** Frost Lily: pale, icy, blooms only in winter in the snow of cold highlands, with a faint glow. */
    public static final DeferredBlock<WildFlowerBlock> FROST_LILY = BLOCKS.registerBlock("frost_lily",
            p -> new WildFlowerBlock(new WildFlowerBlock.Bloom(WINTER, false), false, 9, new DustParticleOptions(0xD8F2FF, 0.45F), p),
            p -> flower(p).lightLevel(s -> s.getValue(WildFlowerBlock.BLOOMING) ? 6 : 0));
    /** 10 minutes of Luck for picking a blooming Fern Flower. */
    static final int LUCK_TICKS = 12000;

    /** A thicket kind: berries, stage wanted per season (spring..winter), biome base temperature window, grows on sand too. */
    record Thicket(String name, String berries, int[] target, float minTemp, float maxTemp, boolean sand, FoodProperties food) {}

    static final List<Thicket> THICKET_KINDS = List.of(
            new Thicket("blackthorn", "sloes", new int[] {1, 2, 3, 3}, 0.5F, 0.9F, false,
                    new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).build()),
            new Thicket("sea_buckthorn", "sea_buckthorn_berries", new int[] {0, 2, 3, 3}, 0.0F, 0.5F, true,
                    new FoodProperties.Builder().nutrition(1).saturationModifier(0.4F).build()),
            new Thicket("jujube", "jujubes", new int[] {0, 2, 3, 0}, 1.0F, 2.0F, true,
                    new FoodProperties.Builder().nutrition(2).saturationModifier(0.4F).build()));

    public static final Map<String, DeferredBlock<ThicketBushBlock>> THICKETS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> THICKET_BERRIES = new LinkedHashMap<>();

    static {
        for (Thicket t : THICKET_KINDS) {
            DeferredItem<Item> berries = ITEMS.registerSimpleItem(t.berries(), p -> p.food(t.food()));
            THICKET_BERRIES.put(t.name(), berries);
            THICKETS.put(t.name(), BLOCKS.registerBlock(t.name(), p -> new ThicketBushBlock(berries, t.target(), t.minTemp(), t.maxTemp(), t.sand(), p),
                    p -> p.mapColor(MapColor.PLANT).noCollision().randomTicks().strength(0.4F).sound(SoundType.SWEET_BERRY_BUSH)
                            .pushReaction(PushReaction.POPPED).noOcclusion()));
        }
    }

    /** Raw elderberries make you sick (as real ones do); cooked or dried they are safe. */
    public static Consumable rawElderberries() {
        return Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.POISON, 80, 0),
                new MobEffectInstance(MobEffects.NAUSEA, 200, 0)))).build();
    }

    public static final DeferredItem<Item> COOKED_ELDERBERRIES = ITEMS.registerSimpleItem("cooked_elderberries",
            p -> p.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.4F).build()));

    /** Sand, terracotta, gravel and shingle: where the desert and coast kinds also take root. */
    public static boolean dryGround(BlockState state) {
        return state.is(net.minecraft.tags.BlockTags.SAND) || state.is(net.minecraft.tags.BlockTags.SUPPORTS_DRY_VEGETATION)
                || state.is(net.minecraft.world.level.block.Blocks.GRAVEL) || state.is(EarthBlocks.BEACH_PEBBLES.get());
    }

    public static void init() {}

    private Flora() {}
}
