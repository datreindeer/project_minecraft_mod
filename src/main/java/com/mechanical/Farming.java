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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Farming (docs/farming.md). Grain is the seed: a ripe grain crop gives a Sheaf, threshed with the {@link FlailItem}
 * into Grain + Straw. Every crop, vanilla ones too, grows only by day in its temperature range on moist farmland, and
 * slower on poor soil ({@link Fertility}); frost kills young plants at night, drought and root rot wither them (Withered
 * Crop). Harvests use up fertility. Hooded Crows eat young crops unless a Scarecrow stands near (FlockBird). Data
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
    public static final TagKey<Block> SICKLE_MINEABLE = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "mineable/sickle"));

    private static TagKey<Item> itemTag(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, name));
    }

    // ---- crops: barley, einkorn, emmer (grain -> crop -> sheaf), peas ----
    public static final List<String> GRAIN_CROPS = List.of("barley", "einkorn", "emmer");
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

    private static Block.Properties cropProperties(Block.Properties p) {
        return p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.POPPED);
    }

    // ---- wild plants (the first seed), and what frost, drought and rot leave ----
    public static final DeferredBlock<WildPlantBlock> WILD_EINKORN = wild("wild_einkorn");
    public static final DeferredBlock<WildPlantBlock> WILD_EMMER = wild("wild_emmer");
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

    // ---- Bronze Age (complete, but no survival recipe until bronze phase 3) ----
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

    /** Temperature range (Temperature.ambient scale): grows in min..max, full speed in bestLo..bestHi; young plants die below frost at night. */
    public record Climate(float min, float bestLo, float bestHi, float max, float frost, boolean root) {
        float factor(float t) {
            if (t <= min || t >= max) return 0;
            if (t < bestLo) return (t - min) / (bestLo - min);
            if (t > bestHi) return (max - t) / (max - bestHi);
            return 1;
        }
    }

    private static final Climate WHEAT = new Climate(7, 10, 14, 17, 2, false);
    private static Map<Block, Climate> climates;

    public static @Nullable Climate climate(Block block) {
        if (climates == null) {
            Map<Block, Climate> m = new HashMap<>();
            m.put(Blocks.WHEAT, WHEAT);
            m.put(CROPS.get("einkorn").get(), WHEAT);
            m.put(CROPS.get("emmer").get(), WHEAT);
            m.put(CROPS.get("barley").get(), new Climate(5, 8, 12, 16, 0, false));
            m.put(Blocks.CARROTS, new Climate(4, 7, 12, 15, -1, true));
            m.put(Blocks.POTATOES, new Climate(6, 9, 13, 16, 2, true));
            m.put(Blocks.BEETROOTS, new Climate(5, 8, 13, 16, 1, true));
            m.put(PEA_CROP.get(), new Climate(6, 9, 13, 16, 2, false));
            Climate warm = new Climate(12, 15, 20, 25, 6, false); // max 25 (spec 22): savanna and desert are 22, the only biomes this warm
            m.put(Blocks.MELON_STEM, warm);
            m.put(Blocks.PUMPKIN_STEM, warm);
            climates = m;
        }
        return climates.get(block);
    }

    /** Per random tick that reaches the growth check (sky light >= 9 by vanilla), when the conditions hold. */
    public static final float FROST_KILL = 0.1F, DROUGHT_KILL = 0.05F, ROOT_ROT = 0.01F;
    /** Extra growth on the best soil: chance of a second stage after a growth step (~1.3x speed). */
    public static final float RICH_SOIL_BONUS = 0.3F;
    public static final int DAYLIGHT = 9;

    public static boolean isYoungCrop(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) return !crop.isMaxAge(state);
        return state.getBlock() instanceof StemBlock && state.getValue(StemBlock.AGE) < StemBlock.MAX_AGE;
    }

    private static float fertilityFactor(int fertility) {
        return switch (fertility) { case 0 -> 0.5F; case 1 -> 0.75F; default -> 1.0F; };
    }

    /** Why a crop isn't growing right now, or null when it can. Shared by the growth check and the field info. */
    private static @Nullable String blocker(Level level, BlockPos pos, Climate c, float temp, BlockState soil) {
        if (soil.hasProperty(FarmlandBlock.MOISTURE) && soil.getValue(FarmlandBlock.MOISTURE) == 0) return "dry";
        if (level.isDarkOutside() || level.getBrightness(LightLayer.SKY, pos) < DAYLIGHT) return "dark";
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
        float chance = blocker(level, pos, c, temp, soil) != null ? 0 : c.factor(temp) * fertilityFactor(Fertility.get(soil));
        if (random.nextFloat() >= chance) event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
    }

    @SubscribeEvent
    static void onCropGrown(CropGrowEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getState().getBlock() instanceof CropBlock crop) || climate(crop) == null) return;
        BlockPos pos = event.getPos();
        if (Fertility.get(level.getBlockState(pos.below())) == Fertility.MAX && !crop.isMaxAge(event.getState())
                && level.getRandom().nextFloat() < RICH_SOIL_BONUS) {
            level.setBlock(pos, crop.getStateForAge(crop.getAge(event.getState()) + 1), Block.UPDATE_CLIENTS);
        }
    }

    // ---------------------------------------------------------------- harvest

    /** A ripe crop is harvested: poor soil halves the yield, rich soil may add one; the soil loses 1 (peas give 1 back). */
    @SubscribeEvent
    static void onDrops(BlockDropsEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state) || climate(crop) == null) return;
        ServerLevel level = event.getLevel();
        BlockPos soilPos = event.getPos().below();
        BlockState soil = level.getBlockState(soilPos);
        if (!soil.hasProperty(Fertility.FERTILITY)) return;
        int fertility = Fertility.get(soil);
        RandomSource random = level.getRandom();
        List<ItemEntity> drops = event.getDrops();
        if (fertility == 0) {
            for (ItemEntity e : drops) {
                ItemStack s = e.getItem();
                s.setCount(s.getCount() / 2 + (s.getCount() % 2 == 1 && random.nextBoolean() ? 1 : 0));
            }
            drops.removeIf(e -> e.getItem().isEmpty());
        } else if (fertility == Fertility.MAX && !drops.isEmpty() && random.nextFloat() < RICH_SOIL_BONUS) {
            drops.getFirst().getItem().grow(1);
        }
        Fertility.add(level, soilPos, crop == PEA_CROP.get() ? 1 : -1);
        if (event.getBreaker() instanceof LivingEntity breaker && event.getTool().is(SICKLES)) {
            event.getTool().hurtAndBreak(1, breaker, EquipmentSlot.MAINHAND); // crops break instantly, so tools don't wear by themselves
        }
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

    /** Sneak + right-click farmland or a crop with an empty hand: fertility, moisture and, for a crop, its climate. */
    @SubscribeEvent
    static void onInspect(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isShiftKeyDown() || !event.getItemStack().isEmpty()) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState clicked = level.getBlockState(pos);
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
