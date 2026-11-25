package com.mechanical;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.mechanical.mixin.FireBlockInvoker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Aspen, alder, beech, linden, willow, ash, hazel, pine, yew and the fruit trees (rowan, bird cherry, wild apple, plum,
 * olive, date palm; leaves in {@link FruitLeavesBlock}): vanilla-like wood sets (no signs or boats yet). Sounds and door/button behaviour are oak's.
 * Models, loot, tags, recipes, stripping (block_transformer/axe) and the tree features are data (scripts/gen_data.py).
 * They are also in {@link Mechanical#WOODS}, so felling, stumps, fallen logs, rough logs and firewood work for them.
 * Willow and hazel use their own tree shapes ({@link TreeShapes}); beech leaves drop Beechnuts (loot, scripts/gen_trees.py)
 * and a knife on a pine log cuts it open for Pine Resin (once per log: the log is left stripped).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Trees {
    public record Wood(DeferredBlock<RotatedPillarBlock> log, DeferredBlock<RotatedPillarBlock> wood, DeferredBlock<RotatedPillarBlock> strippedLog,
                       DeferredBlock<RotatedPillarBlock> strippedWood, DeferredBlock<Block> planks, DeferredBlock<SlabBlock> slab,
                       DeferredBlock<StairBlock> stairs, DeferredBlock<FenceBlock> fence, DeferredBlock<FenceGateBlock> fenceGate,
                       DeferredBlock<DoorBlock> door, DeferredBlock<TrapDoorBlock> trapdoor, DeferredBlock<ButtonBlock> button,
                       DeferredBlock<PressurePlateBlock> pressurePlate, DeferredBlock<LeavesBlock> leaves, DeferredBlock<SaplingBlock> sapling,
                       DeferredBlock<FlowerPotBlock> pottedSapling) {}

    /** A fruit tree's fruit: item name, the leaves' stage wanted per season (spring..winter, {@link FruitLeavesBlock}) and its food. */
    record Fruit(String item, int[] target, FoodProperties food) {}

    /** Fruit trees (docs/flora-and-food-plan.md, section 4); Rowan Berries are bitter and a little poisonous raw. */
    static final Map<String, Fruit> FRUIT_KINDS = new LinkedHashMap<>();
    static {
        FRUIT_KINDS.put("rowan", new Fruit("rowan_berries", new int[] {1, 2, 3, 0}, food(1, 0.1F)));
        FRUIT_KINDS.put("bird_cherry", new Fruit("bird_cherries", new int[] {1, 2, 3, 0}, food(1, 0.2F)));
        FRUIT_KINDS.put("wild_apple", new Fruit("crab_apple", new int[] {1, 2, 3, 0}, food(2, 0.2F)));
        FRUIT_KINDS.put("plum", new Fruit("wild_plums", new int[] {1, 3, 3, 0}, food(2, 0.3F)));
        FRUIT_KINDS.put("olive", new Fruit("olives", new int[] {0, 1, 2, 3}, food(1, 0.2F)));
        FRUIT_KINDS.put("date_palm", new Fruit("dates", new int[] {1, 2, 3, 3}, food(3, 0.5F)));
        // evergreen: white blossom in spring, green all summer, orange from autumn and hanging on through winter
        FRUIT_KINDS.put("bitter_orange", new Fruit("bitter_oranges", new int[] {1, 2, 3, 3}, food(1, 0.1F)));
        // ---- the orchard (docs/flora-and-food-plan.md, "Orchard"): stages per season as above ----
        FRUIT_KINDS.put("pear", new Fruit("pears", new int[] {1, 2, 3, 0}, food(3, 0.3F)));
        FRUIT_KINDS.put("sweet_cherry", new Fruit("sweet_cherries", new int[] {1, 3, 0, 0}, food(2, 0.2F)));      // early summer
        FRUIT_KINDS.put("lemon", new Fruit("lemons", new int[] {1, 2, 3, 3}, food(1, 0.1F)));                     // evergreen, sour
        FRUIT_KINDS.put("clementine", new Fruit("clementines", new int[] {1, 2, 2, 3}, food(3, 0.4F)));          // evergreen, ripe in winter
        FRUIT_KINDS.put("walnut", new Fruit("walnuts", new int[] {1, 2, 3, 0}, food(2, 0.6F)));
        FRUIT_KINDS.put("chestnut", new Fruit("chestnuts", new int[] {0, 1, 3, 0}, food(1, 0.2F)));              // catkins in high summer
        FRUIT_KINDS.put("quince", new Fruit("quinces", new int[] {1, 2, 3, 0}, food(1, 0.2F)));                  // hard and sour raw
        FRUIT_KINDS.put("mulberry", new Fruit("mulberries", new int[] {1, 3, 0, 0}, food(2, 0.2F)));
        FRUIT_KINDS.put("medlar", new Fruit("medlars", new int[] {1, 2, 3, 0}, food(2, 0.3F)));                  // soft only late in autumn
        FRUIT_KINDS.put("peach", new Fruit("peaches", new int[] {1, 3, 3, 0}, food(3, 0.3F)));
        FRUIT_KINDS.put("apricot", new Fruit("apricots", new int[] {1, 3, 0, 0}, food(2, 0.3F)));
        FRUIT_KINDS.put("almond", new Fruit("almonds", new int[] {1, 2, 3, 0}, food(2, 0.6F)));
        FRUIT_KINDS.put("pomegranate", new Fruit("pomegranates", new int[] {0, 1, 3, 0}, food(3, 0.3F)));       // red flowers in summer
    }
    public static final Map<String, DeferredItem<Item>> FRUITS = new LinkedHashMap<>();
    static {
        FRUIT_KINDS.forEach((tree, f) -> FRUITS.put(tree, f.item().equals("rowan_berries")
                ? Mechanical.ITEMS.registerSimpleItem(f.item(), p -> p.food(f.food(), Flora.rawRowanBerries()))
                : Mechanical.ITEMS.registerSimpleItem(f.item(), p -> p.food(f.food()))));
    }

    private static FoodProperties food(int nutrition, float saturation) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
    }

    /** Saplings that take only in a warm biome (citrus and pomegranate: frost-tender); almond, peach and apricot grow anywhere. Before WOODS: wood() reads it. */
    public static final java.util.Set<String> WARM_ONLY = java.util.Set.of("bitter_orange", "lemon", "clementine", "pomegranate");

    public static final Map<String, Wood> WOODS = Map.ofEntries(
            Map.entry("aspen", wood("aspen", MapColor.SAND, MapColor.COLOR_LIGHT_GRAY)),
            Map.entry("alder", wood("alder", MapColor.TERRACOTTA_ORANGE, MapColor.COLOR_BROWN)),
            Map.entry("beech", wood("beech", MapColor.WOOD, MapColor.STONE)),
            Map.entry("linden", wood("linden", MapColor.TERRACOTTA_WHITE, MapColor.PODZOL)),
            Map.entry("willow", wood("willow", MapColor.SAND, MapColor.COLOR_GRAY)),
            Map.entry("ash", wood("ash", MapColor.TERRACOTTA_WHITE, MapColor.COLOR_LIGHT_GRAY)),
            Map.entry("hazel", wood("hazel", MapColor.TERRACOTTA_PINK, MapColor.COLOR_BROWN)),
            Map.entry("pine", wood("pine", MapColor.WOOD, MapColor.TERRACOTTA_ORANGE)),
            Map.entry("yew", wood("yew", MapColor.TERRACOTTA_RED, MapColor.TERRACOTTA_BROWN)),
            Map.entry("rowan", wood("rowan", MapColor.TERRACOTTA_WHITE, MapColor.COLOR_LIGHT_GRAY)),
            Map.entry("bird_cherry", wood("bird_cherry", MapColor.SAND, MapColor.COLOR_GRAY)),
            Map.entry("wild_apple", wood("wild_apple", MapColor.TERRACOTTA_PINK, MapColor.TERRACOTTA_GRAY)),
            Map.entry("plum", wood("plum", MapColor.TERRACOTTA_RED, MapColor.TERRACOTTA_PURPLE)),
            Map.entry("olive", wood("olive", MapColor.SAND, MapColor.STONE)),
            Map.entry("date_palm", wood("date_palm", MapColor.SAND, MapColor.COLOR_BROWN)),
            Map.entry("bitter_orange", wood("bitter_orange", MapColor.TERRACOTTA_YELLOW, MapColor.TERRACOTTA_GRAY)),
            // the orchard
            Map.entry("pear", wood("pear", MapColor.TERRACOTTA_PINK, MapColor.TERRACOTTA_BLACK)),
            Map.entry("sweet_cherry", wood("sweet_cherry", MapColor.TERRACOTTA_RED, MapColor.TERRACOTTA_BROWN)),
            Map.entry("lemon", wood("lemon", MapColor.SAND, MapColor.TERRACOTTA_GRAY)),
            Map.entry("clementine", wood("clementine", MapColor.TERRACOTTA_YELLOW, MapColor.TERRACOTTA_GRAY)),
            Map.entry("walnut", wood("walnut", MapColor.TERRACOTTA_BROWN, MapColor.COLOR_LIGHT_GRAY)),
            Map.entry("chestnut", wood("chestnut", MapColor.TERRACOTTA_ORANGE, MapColor.TERRACOTTA_BROWN)),
            Map.entry("quince", wood("quince", MapColor.TERRACOTTA_PINK, MapColor.TERRACOTTA_GRAY)),
            Map.entry("mulberry", wood("mulberry", MapColor.TERRACOTTA_YELLOW, MapColor.TERRACOTTA_BROWN)),
            Map.entry("medlar", wood("medlar", MapColor.TERRACOTTA_PINK, MapColor.COLOR_GRAY)),
            Map.entry("peach", wood("peach", MapColor.TERRACOTTA_RED, MapColor.TERRACOTTA_BROWN)),
            Map.entry("apricot", wood("apricot", MapColor.TERRACOTTA_ORANGE, MapColor.TERRACOTTA_BROWN)),
            Map.entry("almond", wood("almond", MapColor.TERRACOTTA_WHITE, MapColor.COLOR_GRAY)),
            Map.entry("pomegranate", wood("pomegranate", MapColor.TERRACOTTA_YELLOW, MapColor.COLOR_GRAY)));

    /** A Bitter Orange sapling grows only where the biome is warm (base temperature at least this: savanna, desert, Marble Gorge, jungle). */
    public static final float CITRUS_MIN_TEMPERATURE = 0.95F;

    public static boolean warmEnoughForCitrus(Level level, BlockPos pos) {
        return level.getBiome(pos).value().getBaseTemperature() >= CITRUS_MIN_TEMPERATURE;
    }

    // ---- the cultivated Apple: a Wild Apple sapling grafted with an Apple Scion (knife in the other hand); same wood, own leaves ----
    public static final DeferredItem<Item> APPLE_SCION = Mechanical.ITEMS.registerSimpleItem("apple_scion");
    public static final DeferredBlock<LeavesBlock> APPLE_LEAVES = block("apple_leaves",
            p -> new FruitLeavesBlock(() -> net.minecraft.world.item.Items.APPLE, new int[] {1, 2, 3, 0}, p), Trees::leafProperties);
    public static final DeferredBlock<SaplingBlock> APPLE_SAPLING = block("apple_sapling", p -> new SaplingBlock(grower("apple"), p), Trees::saplingProperties);
    public static final DeferredBlock<FlowerPotBlock> POTTED_APPLE_SAPLING = Mechanical.BLOCKS.registerBlock("potted_apple_sapling",
            p -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, APPLE_SAPLING, p),
            () -> Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.POPPED));

    /** Yew Arils (from yew leaves, gen_trees.py): the red flesh is sweet, the seed inside deadly; eaten whole they poison. */
    public static final DeferredItem<Item> YEW_ARILS = Mechanical.ITEMS.registerSimpleItem("yew_arils",
            p -> p.food(food(1, 0.1F), net.minecraft.world.item.component.Consumables.defaultFood().onConsume(
                    new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(List.of(
                            new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 140, 1),
                            new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NAUSEA, 200, 0)))).build()));

    public static final DeferredItem<Item> BEECHNUTS = Mechanical.ITEMS.registerSimpleItem("beechnuts",
            p -> p.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.2F).build()));
    public static final DeferredItem<Item> ROASTED_BEECHNUTS = Mechanical.ITEMS.registerSimpleItem("roasted_beechnuts",
            p -> p.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.5F).build()));
    /** Chestnuts roasted on a campfire or a fire pit (gen_trees.py): sweet and floury, far better than raw. */
    public static final DeferredItem<Item> ROASTED_CHESTNUTS = Mechanical.ITEMS.registerSimpleItem("roasted_chestnuts",
            p -> p.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6F).build()));
    public static final DeferredItem<Item> PINE_RESIN = Mechanical.ITEMS.registerSimpleItem("pine_resin");

    private static Wood wood(String w, MapColor planksColor, MapColor barkColor) {
        var B = Mechanical.BLOCKS;
        TreeGrower grower = grower(w);

        var planks = block(w + "_planks", Block::new, () -> Properties.of().mapColor(planksColor).instrument(NoteBlockInstrument.BASS)
                .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
        Function<Properties, SaplingBlock> saplingBlock = w.equals("date_palm") ? p -> new SaplingBlock(grower, p) { // a desert tree: takes root in sand
            @Override
            protected boolean mayPlaceOn(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
                return super.mayPlaceOn(state, level, pos) || state.is(net.minecraft.tags.BlockTags.SAND);
            }
        } : WARM_ONLY.contains(w) ? p -> new SaplingBlock(grower, p) { // citrus, pomegranate: grow only in a warm biome
            @Override
            public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, net.minecraft.util.RandomSource random) {
                if (warmEnoughForCitrus(level, pos)) super.advanceTree(level, pos, state, random);
            }
        } : p -> new SaplingBlock(grower, p);
        var sapling = block(w + "_sapling", saplingBlock, Trees::saplingProperties);
        Supplier<Properties> wood = () -> Properties.of().mapColor(barkColor).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava();
        Supplier<Properties> strippedWood = () -> Properties.of().mapColor(planksColor).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava();
        Supplier<Properties> plankLike = () -> Properties.of().mapColor(planksColor).instrument(NoteBlockInstrument.BASS).ignitedByLava();
        return new Wood(
                block(w + "_log", RotatedPillarBlock::new, () -> logProperties(planksColor, barkColor)),
                block(w + "_wood", RotatedPillarBlock::new, wood),
                block("stripped_" + w + "_log", RotatedPillarBlock::new, () -> logProperties(planksColor, planksColor)),
                block("stripped_" + w + "_wood", RotatedPillarBlock::new, strippedWood),
                planks,
                block(w + "_slab", SlabBlock::new, () -> Properties.ofLegacyCopy(planks.get())),
                block(w + "_stairs", p -> new StairBlock(planks.get().defaultBlockState(), p), () -> Properties.ofLegacyCopy(planks.get())),
                block(w + "_fence", FenceBlock::new, () -> plankLike.get().strength(2.0F, 3.0F).sound(SoundType.WOOD)),
                block(w + "_fence_gate", p -> new FenceGateBlock(WoodType.OAK, p), () -> plankLike.get().forceSolidOn().strength(2.0F, 3.0F)),
                block(w + "_door", p -> new DoorBlock(BlockSetType.OAK, p), () -> plankLike.get().strength(3.0F).noOcclusion().pushReaction(PushReaction.POPPED)),
                block(w + "_trapdoor", p -> new TrapDoorBlock(BlockSetType.OAK, p), () -> plankLike.get().strength(3.0F).noOcclusion()
                        .isValidSpawn((s, l, pos, e) -> false)),
                block(w + "_button", p -> new ButtonBlock(BlockSetType.OAK, 30, p), () -> Properties.of().noCollision().strength(0.5F).pushReaction(PushReaction.POPPED)),
                block(w + "_pressure_plate", p -> new PressurePlateBlock(BlockSetType.OAK, p), () -> plankLike.get().forceSolidOn().noCollision()
                        .strength(0.5F).pushReaction(PushReaction.POPPED)),
                // plain vanilla leaves (distance/persistent, decay) with untinted, pre-coloured textures; fruit trees' follow the season
                block(w + "_leaves", leaves(w), Trees::leafProperties),
                sapling,
                B.registerBlock("potted_" + w + "_sapling", p -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, sapling, p),
                        () -> Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.POPPED)));
    }

    private static TreeGrower grower(String w) {
        ResourceKey<Feature> feature = ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(Mechanical.MODID, w));
        return new TreeGrower(Mechanical.MODID + "_" + w, WeightedList.of(feature), WeightedList.of(), WeightedList.of(), feature);
    }

    private static Properties leafProperties() {
        return Properties.of().mapColor(MapColor.PLANT).strength(0.2F).randomTicks().sound(SoundType.GRASS).noOcclusion()
                .isValidSpawn((s, l, pos, e) -> false).isSuffocating((s, l, pos) -> false)
                .ignitedByLava().pushReaction(PushReaction.POPPED).isRedstoneConductor((s, l, pos) -> false);
    }

    private static Properties saplingProperties() {
        return Properties.of().mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.POPPED);
    }

    private static Function<Properties, LeavesBlock> leaves(String w) {
        Fruit f = FRUIT_KINDS.get(w);
        return f == null ? p -> new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), p) : p -> new FruitLeavesBlock(FRUITS.get(w), f.target(), p);
    }

    static Properties logProperties(MapColor top, MapColor side) {
        return Properties.of().mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? top : side)
                .instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava();
    }

    /** Registers the block and its block item (doors get the two-block-high item). */
    static <B extends Block> DeferredBlock<B> block(String name, Function<Properties, B> factory, Supplier<Properties> properties) {
        DeferredBlock<B> block = Mechanical.BLOCKS.registerBlock(name, factory, properties);
        if (name.endsWith("_door")) Mechanical.ITEMS.registerItem(name, p -> new DoubleHighBlockItem(block.get(), p), p -> p.useBlockDescriptionPrefix());
        else Mechanical.ITEMS.registerSimpleBlockItem(name, block);
        return block;
    }

    /** Loads this class so its registrations happen; call from the mod constructor. */
    public static void init() {}

    /** Same fire odds as vanilla wood. */
    @SubscribeEvent
    static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var fire = (FireBlockInvoker) Blocks.FIRE;
            for (Wood w : WOODS.values()) {
                for (var b : List.of(w.planks(), w.slab(), w.stairs(), w.fence(), w.fenceGate())) fire.mechanical$setFlammable(b.get(), 5, 20);
                for (var b : List.of(w.log(), w.wood(), w.strippedLog(), w.strippedWood())) fire.mechanical$setFlammable(b.get(), 5, 5);
                fire.mechanical$setFlammable(w.leaves().get(), 30, 60);
            }
            fire.mechanical$setFlammable(APPLE_LEAVES.get(), 30, 60);
        });
    }

    /** A knife on a pine log cuts the bark open: 1 Pine Resin, and the log is left stripped (so once per log). */
    @SubscribeEvent
    static void tapResin(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Wood pine = WOODS.get("pine");
        if (!(event.getItemStack().getItem() instanceof KnifeItem) || !state.is(pine.log().get())) return;
        if (!level.isClientSide()) {
            level.setBlock(pos, pine.strippedLog().get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS)),
                    Block.UPDATE_ALL);
            Block.popResourceFromFace(level, pos, event.getFace() == null ? Direction.UP : event.getFace(), new ItemStack(PINE_RESIN.get()));
            event.getItemStack().hurtAndBreak(1, event.getEntity(), event.getHand());
            level.playSound(null, pos, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 1.0F, 1.3F);
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    /**
     * Grafting (docs/flora-and-food-plan.md, section 5): an Apple Scion used on a Wild Apple sapling, with a knife in the
     * other hand, turns it into an Apple sapling (same growth stage). A knife on the leaves of a grown Apple tree cuts a
     * scion off (the twig and its leaves come away), so one tree gives the scions for the next.
     */
    @SubscribeEvent
    static void graft(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        var player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (held.is(APPLE_SCION.get()) && state.is(WOODS.get("wild_apple").sapling().get())) {
            var otherHand = event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? net.minecraft.world.InteractionHand.OFF_HAND
                    : net.minecraft.world.InteractionHand.MAIN_HAND;
            ItemStack knife = player.getItemInHand(otherHand);
            if (!level.isClientSide()) {
                if (!(knife.getItem() instanceof KnifeItem)) {
                    player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mechanical.graft.no_knife"));
                } else {
                    graftApple(level, pos, state);
                    knife.hurtAndBreak(1, player, otherHand);
                    held.consume(1, player);
                }
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (held.getItem() instanceof KnifeItem && state.is(APPLE_LEAVES.get())) {
            if (!level.isClientSide()) {
                level.destroyBlock(pos, false, player);
                Block.popResourceFromFace(level, pos, event.getFace() == null ? Direction.UP : event.getFace(), new ItemStack(APPLE_SCION.get()));
                held.hurtAndBreak(1, player, event.getHand());
            }
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    /** The Wild Apple sapling at pos becomes an Apple sapling at the same stage. */
    static void graftApple(Level level, BlockPos pos, BlockState wild) {
        level.setBlock(pos, APPLE_SAPLING.get().defaultBlockState().setValue(SaplingBlock.STAGE, wild.getValue(SaplingBlock.STAGE)), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.2F);
        if (level instanceof ServerLevel server)
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 6, 0.25, 0.3, 0.25, 0);
    }

    private Trees() {}
}
