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
            new Flower("desert_candle", SPRING | SUMMER, true, 16, true),
            // phase 2
            new Flower("foxglove", SUMMER, false, 16, true),
            new Flower("yellow_adonis", SPRING, false, 8, false),
            new Flower("bog_asphodel", SUMMER, false, 10, false),
            new Flower("marsh_marigold", SPRING, false, 8, false),
            new Flower("wood_anemone", SPRING, false, 7, false),
            new Flower("bluebells", SPRING, false, 10, false),
            new Flower("purple_saxifrage", SPRING | SUMMER, false, 4, false),
            new Flower("purple_loosestrife", SUMMER | AUTUMN, false, 16, true),
            new Flower("spiny_thrift", SPRING | SUMMER, true, 6, false),
            new Flower("cats_foot", SUMMER, false, 6, false));

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
    /** Will-o'-Wisp Lily: a ghostly lily of the Peat Bog that opens and glows only at night (phase 3). */
    public static final DeferredBlock<WildFlowerBlock> WILL_O_WISP_LILY = BLOCKS.registerBlock("will_o_wisp_lily",
            p -> new WildFlowerBlock(new WildFlowerBlock.Bloom(SPRING | SUMMER | AUTUMN, true), false, 12, new DustParticleOptions(0x9CF0D8, 0.55F), p),
            p -> flower(p).lightLevel(s -> s.getValue(WildFlowerBlock.BLOOMING) ? 7 : 0));
    /** Moonwort: the folklore key-herb of the Limestone Karst; its spike shines silver only on full-moon nights (phase 3). */
    public static final DeferredBlock<WildFlowerBlock> MOONWORT = BLOCKS.registerBlock("moonwort",
            p -> new WildFlowerBlock(new WildFlowerBlock.Bloom(SPRING | SUMMER | AUTUMN, true, true), false, 8, new DustParticleOptions(0xE8EEFF, 0.4F), p),
            p -> flower(p).lightLevel(s -> s.getValue(WildFlowerBlock.BLOOMING) ? 5 : 0));
    /** 10 minutes of Luck for picking a blooming Fern Flower. */
    static final int LUCK_TICKS = 12000;

    /** A thicket kind: berries, stage wanted per season (spring..winter), biome base temperature window, grows on sand too,
     *  thorny, a low mat/mound, keeps its leaves in winter (see {@link ThicketBushBlock}). */
    record Thicket(String name, String berries, int[] target, float minTemp, float maxTemp, boolean sand, FoodProperties food,
                   boolean thorns, boolean low, boolean evergreen) {
        Thicket(String name, String berries, int[] target, float minTemp, float maxTemp, boolean sand, FoodProperties food) {
            this(name, berries, target, minTemp, maxTemp, sand, food, true, false, false);
        }
    }

    private static FoodProperties berry(int nutrition, float saturation) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
    }

    static final List<Thicket> THICKET_KINDS = List.of(
            new Thicket("blackthorn", "sloes", new int[] {1, 2, 3, 3}, 0.5F, 0.9F, false,
                    new FoodProperties.Builder().nutrition(1).saturationModifier(0.1F).build()),
            new Thicket("sea_buckthorn", "sea_buckthorn_berries", new int[] {0, 2, 3, 3}, 0.0F, 0.5F, true,
                    new FoodProperties.Builder().nutrition(1).saturationModifier(0.4F).build()),
            new Thicket("jujube", "jujubes", new int[] {0, 2, 3, 0}, 1.0F, 2.0F, true,
                    new FoodProperties.Builder().nutrition(2).saturationModifier(0.4F).build()),
            // phase 2 (stages: 0 leaves, 1 flowers, 2 unripe, 3 ripe; caper: 3 = buds ready to pick)
            new Thicket("rosehip", "rosehips", new int[] {0, 1, 3, 3}, 0.4F, 0.8F, false, berry(1, 0.3F), true, false, false),
            new Thicket("crowberry", "crowberries", new int[] {1, 2, 3, 3}, -0.5F, 0.4F, false, berry(1, 0.2F), false, true, true),
            new Thicket("bearberry", "bearberries", new int[] {1, 2, 3, 3}, 0.15F, 0.45F, true, berry(1, 0.1F), false, true, true),
            new Thicket("barberry", "barberries", new int[] {1, 2, 3, 3}, 0.6F, 1.3F, false, berry(1, 0.2F), true, false, false),
            new Thicket("caper", "capers", new int[] {0, 3, 2, 0}, 1.0F, 1.6F, true, berry(1, 0.2F), false, true, false),
            new Thicket("red_currant", "red_currants", new int[] {1, 2, 3, 0}, 0.6F, 0.8F, false, berry(1, 0.3F), false, false, false),
            new Thicket("black_currant", "black_currants", new int[] {1, 2, 3, 0}, 0.25F, 0.6F, false, berry(1, 0.3F), false, false, false));

    public static final Map<String, DeferredBlock<ThicketBushBlock>> THICKETS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> THICKET_BERRIES = new LinkedHashMap<>();

    static {
        for (Thicket t : THICKET_KINDS) {
            DeferredItem<Item> berries = ITEMS.registerSimpleItem(t.berries(), p -> p.food(t.food()));
            THICKET_BERRIES.put(t.name(), berries);
            THICKETS.put(t.name(), BLOCKS.registerBlock(t.name(), p -> new ThicketBushBlock(berries, t.target(), t.minTemp(), t.maxTemp(), t.sand(), t.thorns(), t.low(), t.evergreen(), p),
                    p -> p.mapColor(MapColor.PLANT).noCollision().randomTicks().strength(0.4F).sound(SoundType.SWEET_BERRY_BUSH)
                            .pushReaction(PushReaction.POPPED).noOcclusion()));
        }
    }

    /** Raw elderberries make you sick (as real ones do); cooked or dried they are safe. */
    public static Consumable rawElderberries() {
        return Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.POISON, 80, 0),
                new MobEffectInstance(MobEffects.NAUSEA, 200, 0)))).build();
    }

    /** Raw rowan berries are bitter and upset the stomach; cooked or dried they are fine (phase 2). */
    public static Consumable rawRowanBerries() {
        return Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(List.of(new MobEffectInstance(MobEffects.POISON, 60, 0),
                new MobEffectInstance(MobEffects.NAUSEA, 160, 0)))).build();
    }

    public static final DeferredItem<Item> COOKED_ROWAN_BERRIES = ITEMS.registerSimpleItem("cooked_rowan_berries",
            p -> p.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.5F).build()));

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
