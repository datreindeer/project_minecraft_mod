package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Farming (docs/farming.md). Grain is the seed: a ripe grain crop gives a Sheaf, threshed with the {@link FlailItem}
 * into Grain + Straw. Every crop, vanilla ones too, grows only by day in its temperature range on moist farmland, and
 * slower on poor soil ({@link Fertility}); frost kills young plants at night, drought and root rot wither them (Withered
 * Crop). Harvests use up fertility. Hooded Crows eat young crops unless a Scarecrow stands near (FlockBird). Crop Stakes on farmland: right-click
 * harvest and replant, for a smaller yield. Data
 * (loot, recipes, worldgen, models): scripts/gen_farming.py. Vanilla wheat: the Wheat item is the Wheat Sheaf, Wheat
 * Seeds are Wheat Grain (renamed in lang).
 */
@EventBusSubscriber(modid = MODID)
public final class Farming {
    // ---- tags ----
    public static final TagKey<Item> GRAINS = itemTag("grains");
    public static final TagKey<Item> SHEAVES = itemTag("sheaves");
    public static final TagKey<Item> COMPOSTABLE = itemTag("compostable");
    public static final TagKey<Item> SICKLES = itemTag("sickles");
    public static final TagKey<Block> THRESHING_FLOORS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "threshing_floors"));
    /** Livestock, aurochs and horses: adults drop Manure now and then. */
    public static final TagKey<EntityType<?>> DROPS_MANURE = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MODID, "drops_manure"));
    public static final TagKey<Block> SICKLE_MINEABLE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "mineable/sickle"));

    private static TagKey<Item> itemTag(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, name));
    }

    // ---- crops: barley, einkorn, emmer, oats (grain -> crop -> sheaf), peas ----
    public static final List<String> GRAIN_CROPS = List.of("barley", "einkorn", "emmer", "oat");
    public static final Map<String, DeferredBlock<FarmCropBlock>> CROPS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> GRAIN_ITEMS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> SHEAF_ITEMS = new LinkedHashMap<>();
    static {
        for (String g : GRAIN_CROPS) {
            DeferredBlock<FarmCropBlock> crop = BLOCKS.registerBlock(g + "_crop", p -> new FarmCropBlock(() -> Farming.GRAIN_ITEMS.get(g).get(), p), Farming::cropProperties);
            CROPS.put(g, crop);
            GRAIN_ITEMS.put(g, ITEMS.registerItem(g + "_grain", p -> new BlockItem(crop.get(), p)));
            SHEAF_ITEMS.put(g, ITEMS.registerSimpleItem(g + "_sheaf"));
        }
    }
    public static final DeferredBlock<FarmCropBlock> PEA_CROP = BLOCKS.registerBlock("pea_crop", p -> new FarmCropBlock(() -> Farming.PEAS.get(), p), Farming::cropProperties);
    public static final DeferredItem<BlockItem> PEAS = ITEMS.registerItem("peas", p -> new BlockItem(PEA_CROP.get(), p),
            p -> p.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.3F).build()));

    static Block.Properties cropProperties(Block.Properties p) {
        return p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.POPPED);
    }

    // ---- wild plants (the first seed), and what frost, drought and rot leave ----
    /**
     * Wild grains in their natural stands (WildGrainBlock; worldgen: worldgen.WildGrainFeature): ears, ripening and shattering
     * days of the year (late summer, barley first, rice last) and height in px. Each gives the grain of its crop.
     */
    public static final Map<String, DeferredBlock<WildGrainBlock>> WILD_GRAINS = new LinkedHashMap<>();
    static {
        wildGrain("einkorn", new WildGrainBlock.Timing(92, 132, 144), 13);
        wildGrain("emmer", new WildGrainBlock.Timing(95, 136, 148), 15);
        wildGrain("barley", new WildGrainBlock.Timing(80, 122, 133), 12);
        wildGrain("oats", new WildGrainBlock.Timing(102, 144, 155), 14);
        wildGrain("rice", new WildGrainBlock.Timing(112, 156, 168), 16);
    }
    public static final DeferredBlock<WildGrainBlock> WILD_EINKORN = WILD_GRAINS.get("einkorn"), WILD_EMMER = WILD_GRAINS.get("emmer");

    private static void wildGrain(String kind, WildGrainBlock.Timing timing, int height) {
        WILD_GRAINS.put(kind, BLOCKS.registerBlock("wild_" + kind, kind.equals("rice") ? p -> new WildGrainBlock.Rice(timing, height, p)
                : p -> new WildGrainBlock(timing, height, p), p -> p.mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.GRASS)
                .offsetType(Block.OffsetType.XZ).randomTicks().pushReaction(PushReaction.POPPED)));
    }
    public static final DeferredBlock<WildPlantBlock> WILD_PEA = wild("wild_pea");
    public static final DeferredBlock<WildPlantBlock> WITHERED_CROP = BLOCKS.registerBlock("withered_crop", WildPlantBlock::new, p -> p
            .mapColor(MapColor.COLOR_BROWN).noCollision().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.POPPED));

    private static DeferredBlock<WildPlantBlock> wild(String name) {
        return BLOCKS.registerBlock(name, WildPlantBlock::new, p -> p.mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.GRASS)
                .offsetType(Block.OffsetType.XZ).pushReaction(PushReaction.POPPED));
    }

    // ---- items ----
    public static final DeferredItem<Item> STRAW = ITEMS.registerSimpleItem("straw");
    public static final DeferredItem<Item> RAW_PORRIDGE = ITEMS.registerSimpleItem("raw_porridge", p -> p.stacksTo(1));
    public static final DeferredItem<Item> PORRIDGE = ITEMS.registerSimpleItem("porridge", p -> p.stacksTo(1)
            .food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6F).build()).usingConvertsTo(Items.BOWL));
    public static final DeferredItem<Item> FLINT_SICKLE = ITEMS.registerSimpleItem("flint_sickle", p -> p.tool(Mechanical.FLINT, SICKLE_MINEABLE, 1.0F, -2.2F, 0.0F));
    public static final DeferredItem<FlailItem> FLAIL = ITEMS.registerItem("flail", FlailItem::new, p -> p.durability(64));
    public static final DeferredItem<CropStakesItem> CROP_STAKES = ITEMS.registerItem("crop_stakes", CropStakesItem::new);

    // ---- Bronze Age (recipes: bronze phase 3, scripts/gen_farming3.py) ----
    public static final DeferredItem<Item> FLOUR = ITEMS.registerSimpleItem("flour");
    public static final DeferredItem<Item> DOUGH = ITEMS.registerSimpleItem("dough");
    public static final DeferredItem<FertilizerItem> COMPOST = ITEMS.registerItem("compost", p -> new FertilizerItem(1, p));
    public static final DeferredItem<FertilizerItem> MANURE = ITEMS.registerItem("manure", p -> new FertilizerItem(2, p));
    public static final DeferredBlock<QuernBlock> QUERN = BLOCKS.registerBlock("quern", QuernBlock::new, p -> p
            .mapColor(MapColor.STONE).strength(2.0F).sound(SoundType.STONE).noOcclusion());
    public static final DeferredItem<BlockItem> QUERN_ITEM = ITEMS.registerSimpleBlockItem("quern", QUERN);
    public static final DeferredBlock<BakingStoneBlock> BAKING_STONE = BLOCKS.registerBlock("baking_stone", BakingStoneBlock::new, p -> p
            .mapColor(MapColor.STONE).strength(1.5F).sound(SoundType.STONE).noOcclusion());
    public static final DeferredItem<BlockItem> BAKING_STONE_ITEM = ITEMS.registerSimpleBlockItem("baking_stone", BAKING_STONE);
    public static final DeferredBlock<CompostHeapBlock> COMPOST_HEAP = BLOCKS.registerBlock("compost_heap", CompostHeapBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> COMPOST_HEAP_ITEM = ITEMS.registerSimpleBlockItem("compost_heap", COMPOST_HEAP);
    public static final DeferredBlock<ScarecrowBlock> SCARECROW = BLOCKS.registerBlock("scarecrow", ScarecrowBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> SCARECROW_ITEM = ITEMS.registerSimpleBlockItem("scarecrow", SCARECROW);

    public static void init() {}

    // ---------------------------------------------------------------- growth rules

    /**
     * Temperature range (Temperature.ambient scale): grows in min..max, full speed in bestLo..bestHi; young plants die below
     * frost at night. On top of that, growth speed per season ({@link #season}). One row per crop in {@link #climate}.
     */
    public record Climate(float min, float bestLo, float bestHi, float max, float frost, boolean root, float[] seasons) {
        Climate(float min, float bestLo, float bestHi, float max, float frost, boolean root) {
            this(min, bestLo, bestHi, max, frost, root, DEFAULT_SEASONS);
        }

        /** Growth factor in a season (0 spring .. 3 winter, Calendar.season); 0 = rests for the season. */
        public float season(int season) {
            return seasons[season];
        }

        float factor(float t) {
            if (t <= min || t >= max) return 0;
            if (t < bestLo) return (t - min) / (bestLo - min);
            if (t > bestHi) return (max - t) / (max - bestHi);
            return 1;
        }
    }

    /** Spring, summer, autumn, winter. Crops without their own profile: sown in spring, best in summer, slow in autumn, rest in winter. */
    public static final float[] DEFAULT_SEASONS = {1.0F, 1.2F, 0.6F, 0};
    private static final Climate WHEAT = new Climate(7, 10, 14, 17, 2, false); // the default profile
    private static Map<Block, Climate> climates;

    public static @Nullable Climate climate(Block block) {
        if (climates == null) {
            Map<Block, Climate> m = new HashMap<>();
            m.put(Blocks.WHEAT, WHEAT);
            m.put(CROPS.get("einkorn").get(), WHEAT);
            m.put(CROPS.get("emmer").get(), WHEAT);
            m.put(CROPS.get("barley").get(), new Climate(5, 8, 12, 16, 0, false, new float[] {1.2F, 1.0F, 0.7F, 0})); // quick, early
            m.put(CROPS.get("oat").get(), new Climate(4, 7, 12, 16, -1, false, new float[] {1.1F, 1.1F, 0.6F, 0})); // cool and damp, like the meadows it comes from
            Climate carrot = new Climate(4, 7, 12, 15, -1, true, new float[] {1.0F, 1.0F, 0.8F, 0}); // roots fill out in autumn
            m.put(Blocks.CARROTS, carrot);
            m.put(Roots.WILD_CARROT_CROP.get(), carrot); // the wild form, same needs
            m.put(Blocks.POTATOES, new Climate(6, 9, 13, 16, 2, true, new float[] {0.9F, 1.2F, 0.5F, 0}));
            m.put(Blocks.BEETROOTS, new Climate(5, 8, 13, 16, 1, true, new float[] {1.0F, 1.1F, 0.8F, 0}));
            m.put(PEA_CROP.get(), new Climate(6, 9, 13, 16, 2, false, new float[] {1.2F, 1.0F, 0.4F, 0})); // early, done by autumn
            m.put(Spinning.FLAX_CROP.get(), new Climate(5, 8, 13, 16, 0, false, new float[] {1.1F, 1.2F, 0.4F, 0})); // a cool-climate crop, like barley
            // max 25 (spec 22): savanna and desert are 22, the only biomes this warm; needs the summer heat
            Climate warm = new Climate(12, 15, 20, 25, 6, false, new float[] {0.5F, 1.3F, 0.6F, 0});
            m.put(Blocks.MELON_STEM, warm);
            m.put(Blocks.PUMPKIN_STEM, warm);
            Maize.climates(m); // teosinte and maize (flora 4)
            Herbs.climates(m); // wild and tamed vegetables, bought crops (flora and food plan phase 4)
            climates = m;
        }
        return climates.get(block);
    }

    /** Per random tick that reaches the growth check (sky light >= 9 by vanilla), when the conditions hold. */
    public static final float FROST_KILL = 0.1F, DROUGHT_KILL = 0.05F, ROOT_ROT = 0.01F;
    /**
     * Growth speed, all in one place (docs/farming.md "Växtvillkor"): a crop grows at vanilla's per-tick chance x
     * GROWTH_SPEED x temperature factor x season factor x {@link #FERTILITY_GROWTH}. Above 1 the extra becomes extra
     * stages after a vanilla growth step ({@link #onCropGrown}), so the rate is kept exactly, not capped at vanilla's.
     */
    public static final float GROWTH_SPEED = 2.0F;
    /** Growth factor per fertility 0..3: poor soil halves it, composted or manured soil (3) grows 1.5x. */
    public static final float[] FERTILITY_GROWTH = {0.5F, 0.75F, 1.0F, 1.5F};
    /** Crop Stakes hold the plant up: it grows this much faster. */
    public static final float STAKED_GROWTH = 1.25F;
    /** The best soil's chance of one extra item at harvest. */
    public static final float RICH_SOIL_BONUS = 0.3F;
    public static final int DAYLIGHT = 9;
    static final Identifier FALLOW = Identifier.fromNamespaceAndPath(MODID, "food/fallow_season");

    public static boolean isYoungCrop(BlockState state) {
        if (MaizeCropBlock.isUpper(state)) return false; // only the lower half is the plant
        if (state.getBlock() instanceof CropBlock crop) return !crop.isMaxAge(state);
        return state.getBlock() instanceof StemBlock && state.getValue(StemBlock.AGE) < StemBlock.MAX_AGE;
    }

    /** Growth rate relative to vanilla's chance when nothing blocks it ({@link #blocker}): 1 = vanilla speed. */
    public static float growthRate(Climate c, float temp, int season, int fertility) {
        return GROWTH_SPEED * c.factor(temp) * c.season(season) * FERTILITY_GROWTH[Math.clamp(fertility, 0, Fertility.MAX)];
    }

    /** This crop's growth rate where it stands, 0 when something blocks it. */
    public static float growthRate(Level level, BlockPos pos, Climate c) {
        BlockState soil = level.getBlockState(pos.below());
        float temp = Temperature.ambient(level, pos);
        return blocker(level, pos, c, temp, soil) != null ? 0
                : growthRate(c, temp, Calendar.season(level.getOverworldClockTime()), Fertility.get(soil)) * stakeFactor(soil);
    }

    /** {@link #STAKED_GROWTH} on staked farmland, else 1. */
    public static float stakeFactor(BlockState soil) {
        return soil.hasProperty(Fertility.STAKED) && soil.getValue(Fertility.STAKED) ? STAKED_GROWTH : 1;
    }

    /** Stages a crop gains for one vanilla growth step at this rate (> 1: the rest as extra stages, the fraction by chance). */
    public static int stagesFor(float rate, RandomSource random) {
        int stages = (int) rate;
        return stages + (random.nextFloat() < rate - stages ? 1 : 0);
    }

    /** Why a crop isn't growing right now, or null when it can. Shared by the growth check and the field info. */
    private static @Nullable String blocker(Level level, BlockPos pos, Climate c, float temp, BlockState soil) {
        if (soil.hasProperty(FarmlandBlock.MOISTURE) && soil.getValue(FarmlandBlock.MOISTURE) == 0) return "dry";
        if (!Herbs.paddyOk(level, pos)) return "paddy";
        if (level.isDarkOutside() || level.getBrightness(LightLayer.SKY, pos) < DAYLIGHT) return "dark";
        if (c.season(Calendar.season(level.getOverworldClockTime())) == 0) return "season";
        if (temp <= c.min()) return "cold";
        if (temp >= c.max()) return "hot";
        return null;
    }

    @SubscribeEvent
    static void onCropGrow(CropGrowEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockState state = event.getState();
        Climate c = climate(state.getBlock());
        if (c == null) return;
        BlockPos pos = event.getPos();
        RandomSource random = level.getRandom();
        BlockState soil = level.getBlockState(pos.below());
        float temp = Temperature.ambient(level, pos);
        int moisture = soil.hasProperty(FarmlandBlock.MOISTURE) ? soil.getValue(FarmlandBlock.MOISTURE) : FarmlandBlock.MAX_MOISTURE;
        if (isYoungCrop(state)) {
            boolean frost = level.isDarkOutside() && temp < c.frost() && level.canSeeSky(pos) && random.nextFloat() < FROST_KILL;
            boolean drought = moisture == 0 && random.nextFloat() < DROUGHT_KILL;
            boolean rot = c.root() && moisture == FarmlandBlock.MAX_MOISTURE
                    && (level.getBiome(pos).is(Tags.Biomes.IS_SWAMP) || level.isRainingAt(pos.above())) && random.nextFloat() < ROOT_ROT;
            if (frost || drought || rot) {
                level.setBlock(pos, WITHERED_CROP.get().defaultBlockState(), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.6F, 0.6F);
                event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
                return;
            }
        }
        // per real minute, not per (3x longer) day: divided by Calendar.SLOWDOWN a field took ~5 hours of play
        if (random.nextFloat() >= growthRate(level, pos, c)) event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
    }

    /** After a growth step: a rate above 1 ({@link #growthRate}) adds its extra stages. */
    @SubscribeEvent
    static void onCropGrown(CropGrowEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockState state = event.getState();
        Climate c = climate(state.getBlock());
        if (c == null || !isYoungCrop(state)) return;
        BlockPos pos = event.getPos();
        int extra = stagesFor(growthRate(level, pos, c), level.getRandom()) - 1;
        if (extra <= 0) return;
        if (state.getBlock() instanceof CropBlock crop) {
            level.setBlock(pos, crop.getStateForAge(Math.min(crop.getMaxAge(), crop.getAge(state) + extra)), Block.UPDATE_CLIENTS);
        } else {
            level.setBlock(pos, state.setValue(StemBlock.AGE, Math.min(StemBlock.MAX_AGE, state.getValue(StemBlock.AGE) + extra)), Block.UPDATE_CLIENTS);
        }
    }

    // ---------------------------------------------------------------- manure

    /** Ticks between manure rolls (spread by entity id) and the chance per roll: one Manure per ~10 minutes per adult. */
    public static final int MANURE_CHECK = 200, MANURE_CHANCE = 60;

    @SubscribeEvent
    static void dropManure(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Animal animal) || (animal.tickCount + animal.getId()) % MANURE_CHECK != 0
                || !(animal.level() instanceof ServerLevel level) || animal.isBaby() || !animal.isAlive() || !animal.typeHolder().is(DROPS_MANURE)
                || level.getRandom().nextInt(MANURE_CHANCE) != 0) return;
        animal.spawnAtLocation(level, new ItemStack(MANURE.get()));
        level.playSound(null, animal.getX(), animal.getY(), animal.getZ(), SoundEvents.MUD_PLACE, SoundSource.NEUTRAL, 0.6F, 0.7F);
    }

    // ---------------------------------------------------------------- harvest

    /** A ripe crop is harvested: poor soil halves the yield, rich soil may add one; the soil loses 1 (peas give 1 back). */
    @SubscribeEvent
    static void onDrops(BlockDropsEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state) || climate(crop) == null) return;
        if (event.getLevel() instanceof ServerLevel server) Herbs.domesticate(server, event.getPos(), state, event.getDrops());
        addSickleBonus(event.getDrops().stream().map(ItemEntity::getItem).toList(), event.getTool(), event.getLevel().getRandom());
        if (!harvestSoil(event.getLevel(), event.getPos().below(), crop, event.getDrops().stream().map(ItemEntity::getItem).toList())) return;
        event.getDrops().removeIf(e -> e.getItem().isEmpty());
        if (event.getBreaker() instanceof LivingEntity breaker && event.getTool().is(SICKLES)) {
            event.getTool().hurtAndBreak(1, breaker, EquipmentSlot.MAINHAND); // crops break instantly, so tools don't wear by themselves
        }
    }

    static final Identifier WILD_HARVEST = Identifier.fromNamespaceAndPath(MODID, "food/wild_harvest"),
            TOO_LATE = Identifier.fromNamespaceAndPath(MODID, "food/too_late");

    /**
     * A wild grain plant cut (stage drops: its loot table): ripe, a sickle's chance of one more grain, as a crop's extra sheaf;
     * a sickle wears 1 (the plant breaks at once). First Harvest for a ripe one cut with a sickle, Too Late for shattered ears.
     */
    @SubscribeEvent
    static void onWildGrainDrops(BlockDropsEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof WildGrainBlock)) return;
        ItemStack tool = event.getTool();
        int stage = state.getValue(WildGrainBlock.STAGE);
        if (stage == WildGrainBlock.RIPE && event.getLevel().getRandom().nextFloat() < sickleBonus(tool)) {
            event.getDrops().stream().map(ItemEntity::getItem).filter(s -> s.is(GRAINS)).findFirst().ifPresent(s -> s.grow(1));
        }
        if (event.getBreaker() instanceof LivingEntity breaker && tool.is(SICKLES)) tool.hurtAndBreak(1, breaker, EquipmentSlot.MAINHAND);
        if (event.getBreaker() instanceof net.minecraft.server.level.ServerPlayer sp) {
            if (stage == WildGrainBlock.RIPE && tool.is(SICKLES)) Kettle.award(sp, WILD_HARVEST);
            if (stage == WildGrainBlock.SHATTERED) Kettle.award(sp, TOO_LATE);
        }
    }

    /** Days from today until this plant reaches {@code day} of the year (its own ripening offset added), 0..359. */
    static int daysUntil(Level level, BlockPos pos, int day) {
        return Math.floorMod(day + WildGrainBlock.jitter(pos) - Calendar.dayOfYear(level.getOverworldClockTime()), Calendar.YEAR_DAYS);
    }

    /** Field info for a wild grain plant: its stage and how long till the next one (the harvest window). */
    static Component wildGrainInfo(Level level, BlockPos pos, BlockState state) {
        WildGrainBlock.Timing t = ((WildGrainBlock) state.getBlock()).timing();
        int stage = state.getValue(WildGrainBlock.STAGE);
        int next = switch (stage) {
            case WildGrainBlock.YOUNG -> t.head();
            case WildGrainBlock.HEADING, WildGrainBlock.SHATTERED -> t.ripe();
            default -> t.shatter();
        };
        return Component.translatable("message.mechanical.wild_grain." + stage, state.getBlock().getName(), daysUntil(level, pos, next));
    }

    /** Chance of one extra sheaf from a ripe grain crop cut with this sickle (Straw comes from the loot tables): better metal, better yield. */
    public static float sickleBonus(ItemStack tool) {
        if (tool.is(Smithing.STEEL_SICKLE.get())) return 1.0F;
        if (tool.is(Smithing.IRON_SICKLE.get())) return 0.9F;
        if (tool.is(BronzeTools.BRONZE_SICKLE.get())) return 0.8F;
        if (tool.is(FLINT_SICKLE.get())) return 0.5F;
        return 0;
    }

    /** The sickle's chance of one more sheaf: grows the first sheaf in {@code drops}. */
    private static void addSickleBonus(List<ItemStack> drops, ItemStack tool, RandomSource random) {
        float bonus = sickleBonus(tool);
        if (bonus > 0 && random.nextFloat() < bonus) drops.stream().filter(s -> s.is(SHEAVES)).findFirst().ifPresent(s -> s.grow(1));
    }

    @EventBusSubscriber(modid = MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    static final class SickleTooltip {
        @SubscribeEvent
        static void onTooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (event.getToolTip().isEmpty()) return;
            List<Component> tip = event.getToolTip();
            WILD_GRAINS.forEach((kind, wild) -> {
                if (!stack.is(wildGrainItem(kind))) return;
                WildGrainBlock.Timing t = wild.get().timing();
                tip.add(1, Component.translatable("tooltip.mechanical.wild_grain_window", wild.get().getName(), t.ripe() % Calendar.MONTH_DAYS + 1,
                        Component.translatable("calendar.mechanical.month." + Calendar.MONTHS[t.ripe() / Calendar.MONTH_DAYS]), t.shatter() - t.ripe())
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            });
            float bonus = sickleBonus(stack);
            if (bonus == 0) return;
            tip.add(1, Component.translatable("tooltip.mechanical.sickle_yield", Math.round(bonus * 100)).withStyle(net.minecraft.ChatFormatting.GRAY));
            int side = 2 * BronzeTools.sweepRadius(stack) + 1;
            if (side > 1) tip.add(2, Component.translatable("tooltip.mechanical.sickle_swath", side).withStyle(net.minecraft.ChatFormatting.GRAY));
        }

        private SickleTooltip() {}
    }

    /** Soil rules for a ripe harvest, changing {@code drops} in place (may leave empty stacks); false if not on farmland. */
    private static boolean harvestSoil(ServerLevel level, BlockPos soilPos, CropBlock crop, List<ItemStack> drops) {
        BlockState soil = level.getBlockState(soilPos);
        if (!soil.hasProperty(Fertility.FERTILITY)) return false;
        int fertility = Fertility.get(soil);
        RandomSource random = level.getRandom();
        if (fertility == 0) {
            for (ItemStack s : drops) s.setCount(s.getCount() / 2 + (s.getCount() % 2 == 1 && random.nextBoolean() ? 1 : 0));
        } else if (fertility == Fertility.MAX && !drops.isEmpty() && random.nextFloat() < RICH_SOIL_BONUS) {
            drops.getFirst().grow(1);
        }
        Fertility.add(level, soilPos, crop == PEA_CROP.get() ? 1 : -1);
        return true;
    }

    /** Each item of a staked harvest is kept with this chance: the free replant costs some of the yield. */
    public static final float STAKED_YIELD = 0.6F;

    /**
     * Right-click a ripe crop on staked farmland with an empty hand or a sickle: harvested and replanted at age 0, a smaller
     * yield. A sickle adds its Straw (loot tables) and, after the yield cut, its chance of an extra sheaf, and wears 1.
     */
    @SubscribeEvent
    static void onStakedHarvest(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isShiftKeyDown() || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        ItemStack tool = event.getItemStack();
        if (!tool.isEmpty() && !tool.is(SICKLES)) return;
        // an empty hand with a sickle in a worn bag reaps with it (equipment.WornTools)
        com.mechanical.equipment.WornTools.Found bagSickle = tool.isEmpty() ? com.mechanical.equipment.WornTools.forEmptyHand(event.getEntity(), event.getHand(), s -> s.is(SICKLES)) : null;
        if (bagSickle != null) tool = bagSickle.tool();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        BlockState soil = level.getBlockState(pos.below());
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state) || climate(crop) == null
                || !soil.hasProperty(Fertility.STAKED) || !soil.getValue(Fertility.STAKED)) return;
        if (level instanceof ServerLevel server) {
            List<ItemStack> drops = Block.getDrops(state, server, pos, null, event.getEntity(), tool);
            harvestSoil(server, pos.below(), crop, drops);
            RandomSource random = server.getRandom();
            Item sheaf = drops.stream().filter(s -> s.is(SHEAVES)).findFirst().map(ItemStack::getItem).orElse(null); // before the cut empties it
            for (ItemStack s : drops) {
                int kept = 0;
                for (int i = 0; i < s.getCount(); i++) if (random.nextFloat() < STAKED_YIELD) kept++;
                s.setCount(kept);
            }
            if (!tool.isEmpty()) {
                if (sheaf != null && random.nextFloat() < sickleBonus(tool)) drops.add(new ItemStack(sheaf));
                if (bagSickle != null) bagSickle.hurt(1, event.getEntity());
                else tool.hurtAndBreak(1, event.getEntity(), EquipmentSlot.MAINHAND);
            }
            for (ItemStack s : drops) if (!s.isEmpty()) Block.popResource(server, pos, s);
            server.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /** The grain a wild stand of this kind gives: its crop's grain (oats: Oat Grain, rice: Rice). */
    public static Item wildGrainItem(String kind) {
        return kind.equals("rice") ? Herbs.ITEMS_BY_NAME.get("rice").get() : GRAIN_ITEMS.get(kind.equals("oats") ? "oat" : kind).get();
    }

    /** Sheaf -> its grain (threshing); empty if the stack isn't a sheaf. */
    public static ItemStack grainOf(ItemStack sheaf) {
        if (sheaf.is(Items.WHEAT)) return new ItemStack(Items.WHEAT_SEEDS);
        for (String g : GRAIN_CROPS) {
            if (sheaf.is(SHEAF_ITEMS.get(g).get())) return new ItemStack(GRAIN_ITEMS.get(g).get());
        }
        return ItemStack.EMPTY;
    }

    // ---------------------------------------------------------------- crows and scarecrows

    public static final int SCARECROW_RANGE = 12, CROW_SEARCH = 24;

    public static boolean scarecrowNear(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = -4; dy <= 4; dy += 2) { // two blocks tall: every other layer finds it
            for (int dx = -SCARECROW_RANGE; dx <= SCARECROW_RANGE; dx++) {
                for (int dz = -SCARECROW_RANGE; dz <= SCARECROW_RANGE; dz++) {
                    if (level.getBlockState(p.setWithOffset(pos, dx, dy, dz)).is(SCARECROW.get())) return true;
                }
            }
        }
        return false;
    }

    /** A young crop within {@link #CROW_SEARCH} blocks that no scarecrow guards, for a crow flock to land on; null if none. */
    public static @Nullable BlockPos cropForCrows(Level level, Vec3 around) {
        BlockPos center = BlockPos.containing(around);
        for (int dx = -CROW_SEARCH; dx <= CROW_SEARCH; dx += 2) {
            for (int dz = -CROW_SEARCH; dz <= CROW_SEARCH; dz += 2) {
                BlockPos col = center.offset(dx, 0, dz);
                if (!level.hasChunkAt(col)) continue;
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, col);
                if (isYoungCrop(level.getBlockState(top))) return scarecrowNear(level, top) ? null : top;
            }
        }
        return null;
    }

    /** A crow on the ground pecks at a young crop within 2 blocks: one growth stage gone, a seedling eaten up. */
    public static boolean crowPeck(ServerLevel level, BlockPos at) {
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-2, -1, -2), at.offset(2, 1, 2))) {
            BlockState state = level.getBlockState(p);
            if (!isYoungCrop(state)) continue;
            if (scarecrowNear(level, p)) return false;
            int age = state.getBlock() instanceof CropBlock crop ? crop.getAge(state) : state.getValue(StemBlock.AGE);
            if (age == 0) level.removeBlock(p, false);
            else level.setBlock(p, state.getBlock() instanceof CropBlock crop ? crop.getStateForAge(age - 1) : state.setValue(StemBlock.AGE, age - 1), Block.UPDATE_ALL);
            level.playSound(null, p, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.5F, 1.3F);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), p.getX() + 0.5, p.getY() + 0.3, p.getZ() + 0.5, 5, 0.2, 0.1, 0.2, 0);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- field info

    /** Sneak + right-click farmland or a crop with an empty hand: fertility, moisture and, for a crop, its climate; a wild grain: its harvest window. */
    @SubscribeEvent
    static void onInspect(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isShiftKeyDown() || !event.getItemStack().isEmpty()) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState clicked = level.getBlockState(pos);
        if (clicked.getBlock() instanceof WildGrainBlock) {
            if (!level.isClientSide()) event.getEntity().sendOverlayMessage(wildGrainInfo(level, pos, clicked));
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }
        BlockPos soilPos = clicked.hasProperty(Fertility.FERTILITY) ? pos : pos.below();
        BlockState soil = level.getBlockState(soilPos);
        if (!soil.hasProperty(Fertility.FERTILITY)) return;
        if (!level.isClientSide()) {
            Component msg = Component.translatable("message.mechanical.field_info", Fertility.get(soil), soil.getValue(FarmlandBlock.MOISTURE));
            BlockState crop = level.getBlockState(soilPos.above());
            Climate c = climate(crop.getBlock());
            if (c != null) {
                float temp = Temperature.ambient(level, soilPos.above());
                String why = isYoungCrop(crop) ? blocker(level, soilPos.above(), c, temp, soil) : "ripe";
                if ("season".equals(why) && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) Kettle.award(sp, FALLOW);
                msg = Component.translatable("message.mechanical.crop_info", crop.getBlock().getName(), String.format("%.1f", temp),
                        (int) c.min(), (int) c.max(), msg, Component.translatable("message.mechanical.crop." + (why == null ? "growing" : why)));
            }
            event.getEntity().sendOverlayMessage(msg);
        }
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private Farming() {}
}
