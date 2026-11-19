package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import java.util.function.Supplier;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Late Bronze Age: the Smelting Furnace, a 3x3x3 shaft of {@link #FURNACE_BRICKS} with a {@link FurnaceTapBlock} in the
 * bottom front wall (the controller, {@link FurnaceTapBlockEntity}). Ore, ingots and charcoal dropped down the shaft go
 * in; bellows on a wall heat it; opened, the tap pours along {@link ClayChannelBlock Clay Channels} into Clay Molds.
 * Melt and yield rules: {@link FurnaceMelt}. Data: scripts/gen_smelter.py, textures: scripts/gen_smelter_textures.py.
 */
public final class Smelting {
    public static final DeferredItem<Item> UNFIRED_FURNACE_BRICK = ITEMS.registerSimpleItem("unfired_furnace_brick");
    public static final DeferredItem<Item> FURNACE_BRICK = ITEMS.registerSimpleItem("furnace_brick");
    public static final DeferredItem<Item> UNFIRED_CLAY_CHANNEL = ITEMS.registerSimpleItem("unfired_clay_channel");

    public static final DeferredBlock<Block> FURNACE_BRICKS = BLOCKS.registerBlock("furnace_bricks", Block::new, p -> p
            .mapColor(MapColor.SAND).strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> FURNACE_BRICKS_ITEM = ITEMS.registerSimpleBlockItem("furnace_bricks", FURNACE_BRICKS);

    public static final DeferredBlock<FurnaceTapBlock> FURNACE_TAP = BLOCKS.registerBlock("furnace_tap", FurnaceTapBlock::new, p -> p
            .mapColor(MapColor.SAND).strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion()
            .lightLevel(s -> s.getValue(FurnaceTapBlock.POURING) ? 12 : s.getValue(FurnaceTapBlock.LIT) ? 9 : 0));
    public static final DeferredItem<BlockItem> FURNACE_TAP_ITEM = ITEMS.registerSimpleBlockItem("furnace_tap", FURNACE_TAP);
    public static final Supplier<BlockEntityType<FurnaceTapBlockEntity>> FURNACE_TAP_ENTITY = Mechanical.BLOCK_ENTITIES.register("furnace_tap",
            () -> new BlockEntityType<>(FurnaceTapBlockEntity::new, FURNACE_TAP.get()));
    public static final Supplier<MenuType<FurnaceMenu>> FURNACE_MENU = Mechanical.MENUS.register("smelting_furnace",
            () -> new MenuType<>(FurnaceMenu::new, FeatureFlags.VANILLA_SET));

    public static final DeferredBlock<ClayChannelBlock> CLAY_CHANNEL = BLOCKS.registerBlock("clay_channel", ClayChannelBlock::new, p -> p
            .mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.6F).sound(SoundType.DECORATED_POT).noOcclusion().pushReaction(PushReaction.POPPED));
    public static final DeferredItem<BlockItem> CLAY_CHANNEL_ITEM = ITEMS.registerSimpleBlockItem("clay_channel", CLAY_CHANNEL);
    public static final Supplier<BlockEntityType<ClayChannelBlockEntity>> CLAY_CHANNEL_ENTITY = Mechanical.BLOCK_ENTITIES.register("clay_channel",
            () -> new BlockEntityType<>(ClayChannelBlockEntity::new, CLAY_CHANNEL.get()));

    /** Advancements granted from code (their criterion is minecraft:impossible), see scripts/gen_advancements.py. */
    public static final Identifier FORMED_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "bronze_age/smelting_furnace");
    public static final Identifier TAPPED_ADVANCEMENT = Identifier.fromNamespaceAndPath(MODID, "bronze_age/tap_metal");

    /** Grants a code-driven advancement to every player within {@code radius} blocks. */
    static void award(ServerLevel level, BlockPos pos, Identifier id, double radius) {
        var holder = level.getServer().getAdvancements().get(id);
        if (holder == null) return;
        for (ServerPlayer player : level.players())
            if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= radius * radius) player.getAdvancements().award(holder, "done");
    }

    public static void init() {
        FurnaceMelt.selfCheck();
    }

    private Smelting() {}
}
