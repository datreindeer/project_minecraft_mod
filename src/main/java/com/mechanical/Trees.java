package com.mechanical;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.mechanical.mixin.FireBlockInvoker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * Aspen, alder, beech, linden, willow, ash, hazel, pine and yew: vanilla-like wood sets (no signs or boats yet). Sounds and door/button behaviour are oak's.
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

    public static final Map<String, Wood> WOODS = Map.of(
            "aspen", wood("aspen", MapColor.SAND, MapColor.COLOR_LIGHT_GRAY),
            "alder", wood("alder", MapColor.TERRACOTTA_ORANGE, MapColor.COLOR_BROWN),
            "beech", wood("beech", MapColor.WOOD, MapColor.STONE),
            "linden", wood("linden", MapColor.TERRACOTTA_WHITE, MapColor.PODZOL),
            "willow", wood("willow", MapColor.SAND, MapColor.COLOR_GRAY),
            "ash", wood("ash", MapColor.TERRACOTTA_WHITE, MapColor.COLOR_LIGHT_GRAY),
            "hazel", wood("hazel", MapColor.TERRACOTTA_PINK, MapColor.COLOR_BROWN),
            "pine", wood("pine", MapColor.WOOD, MapColor.TERRACOTTA_ORANGE),
            "yew", wood("yew", MapColor.TERRACOTTA_RED, MapColor.TERRACOTTA_BROWN));

    public static final DeferredItem<Item> BEECHNUTS = Mechanical.ITEMS.registerSimpleItem("beechnuts",
            p -> p.food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.2F).build()));
    public static final DeferredItem<Item> ROASTED_BEECHNUTS = Mechanical.ITEMS.registerSimpleItem("roasted_beechnuts",
            p -> p.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.5F).build()));
    public static final DeferredItem<Item> PINE_RESIN = Mechanical.ITEMS.registerSimpleItem("pine_resin");

    private static Wood wood(String w, MapColor planksColor, MapColor barkColor) {
        var B = Mechanical.BLOCKS;
        ResourceKey<Feature> feature = ResourceKey.create(Registries.FEATURE, Identifier.fromNamespaceAndPath(Mechanical.MODID, w));
        TreeGrower grower = new TreeGrower(Mechanical.MODID + "_" + w, WeightedList.of(feature), WeightedList.of(), WeightedList.of(), feature);

        var planks = block(w + "_planks", Block::new, () -> Properties.of().mapColor(planksColor).instrument(NoteBlockInstrument.BASS)
                .strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
        var sapling = block(w + "_sapling", p -> new SaplingBlock(grower, p), () -> Properties.of().mapColor(MapColor.PLANT).noCollision()
                .randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.POPPED));
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
                // plain vanilla leaves (distance/persistent, decay) with untinted, pre-coloured textures
                block(w + "_leaves", p -> new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), p), () -> Properties.of()
                        .mapColor(MapColor.PLANT).strength(0.2F).randomTicks().sound(SoundType.GRASS).noOcclusion()
                        .isValidSpawn((s, l, pos, e) -> false).isSuffocating((s, l, pos) -> false)
                        .ignitedByLava().pushReaction(PushReaction.POPPED).isRedstoneConductor((s, l, pos) -> false)),
                sapling,
                B.registerBlock("potted_" + w + "_sapling", p -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, sapling, p),
                        () -> Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.POPPED)));
    }

    private static Properties logProperties(MapColor top, MapColor side) {
        return Properties.of().mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? top : side)
                .instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava();
    }

    /** Registers the block and its block item (doors get the two-block-high item). */
    private static <B extends Block> DeferredBlock<B> block(String name, Function<Properties, B> factory, Supplier<Properties> properties) {
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

    private Trees() {}
}
