package com.mechanical;

import static com.mechanical.Mechanical.BLOCKS;
import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Stone-age watercraft in place of vanilla boats (their recipes are off, see gen_age_recipes.py):
 * - Log Raft: logs lashed with cordage (crafting table). One seat, slow (about 47 % of a boat), wobbles.
 * - Dugout Canoe: a log hollowed in the world ({@link DugoutBlankBlock}): score a log lying on its side with a knife, then
 *   axe / fire / axe / fire / axe / knife. Two seats; with a Paddle in hand about 87 % of a boat, without one as slow as the raft.
 * Both are vanilla boat entities underneath; the speed is capped by bleeding off a share of the speed every tick
 * (steady speed = 0.04 / (1 - 0.9 * keep) blocks per tick; vanilla boat 0.36). Data: scripts/gen_watercraft.py.
 */
@EventBusSubscriber(modid = MODID)
public final class Watercraft {
    public static final TagKey<Block> DUGOUT_LOGS = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "dugout_logs"));
    public static final TagKey<Item> FIRE_STARTERS = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "dugout_fire_starters"));
    public static final TagKey<Item> KNIVES = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "knives"));

    /** Share of the speed kept each tick: raft 0.85 (0.17 b/t), canoe with a paddle 0.97 (0.31 b/t), canoe by hand 0.85. */
    public static final double RAFT_KEEP = 0.85, CANOE_PADDLE_KEEP = 0.97, CANOE_HAND_KEEP = 0.85;

    public static final DeferredBlock<DugoutBlankBlock> DUGOUT_BLANK = BLOCKS.registerBlock("dugout_blank", DugoutBlankBlock::new, p -> p
            .mapColor(MapColor.WOOD).strength(4.0F).sound(SoundType.WOOD).noOcclusion()
            .lightLevel(s -> s.getValue(DugoutBlankBlock.BURNING) ? 11 : 0));

    public static final DeferredHolder<EntityType<?>, EntityType<LogRaft>> LOG_RAFT = ENTITY_TYPES.registerEntityType("log_raft",
            LogRaft::new, MobCategory.MISC, b -> b.noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<DugoutCanoe>> DUGOUT_CANOE = ENTITY_TYPES.registerEntityType("dugout_canoe",
            DugoutCanoe::new, MobCategory.MISC, b -> b.noLootTable().sized(1.375F, 0.5625F).eyeHeight(0.5625F).clientTrackingRange(10));

    public static final DeferredItem<BoatItem> LOG_RAFT_ITEM = ITEMS.registerItem("log_raft", p -> new BoatItem(LOG_RAFT.get(), p), p -> p.stacksTo(1));
    public static final DeferredItem<BoatItem> DUGOUT_CANOE_ITEM = ITEMS.registerItem("dugout_canoe", p -> new BoatItem(DUGOUT_CANOE.get(), p), p -> p.stacksTo(1));
    public static final DeferredItem<Item> PADDLE = ITEMS.registerSimpleItem("paddle", p -> p.stacksTo(1));

    private Watercraft() {
    }

    public static void init() {
    }

    /** Bleeds off speed while someone steers (a drifting craft behaves like a vanilla boat). */
    static void cap(AbstractBoat boat, double keep) {
        if (!boat.isLocalInstanceAuthoritative() || !boat.isVehicle()) return;
        Vec3 v = boat.getDeltaMovement();
        boat.setDeltaMovement(v.x * keep, v.y, v.z * keep);
    }

    public static class LogRaft extends AbstractBoat {
        public LogRaft(EntityType<? extends LogRaft> type, Level level) {
            super(type, level, () -> Watercraft.LOG_RAFT_ITEM.get());
        }

        @Override
        protected double rideHeight(EntityDimensions dimensions) {
            return dimensions.height(); // on top of the logs
        }

        @Override
        protected int getMaxPassengers() {
            return 1;
        }

        @Override
        public void tick() {
            super.tick();
            cap(this, RAFT_KEEP);
        }
    }

    public static class DugoutCanoe extends AbstractBoat {
        public DugoutCanoe(EntityType<? extends DugoutCanoe> type, Level level) {
            super(type, level, () -> Watercraft.DUGOUT_CANOE_ITEM.get());
        }

        @Override
        protected double rideHeight(EntityDimensions dimensions) {
            return dimensions.height() / 3.0F; // sitting low inside the hull
        }

        public boolean riderHasPaddle() {
            return getControllingPassenger() instanceof Player player && player.isHolding(PADDLE.get());
        }

        @Override
        public void tick() {
            super.tick();
            cap(this, riderHasPaddle() ? CANOE_PADDLE_KEEP : CANOE_HAND_KEEP);
        }
    }

    /** Scoring the outline with a knife turns a debarked fallen log, or any log lying on its side, into a Dugout Blank. */
    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(KNIVES)) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Direction.Axis axis;
        if (state.getBlock() instanceof FallenLogBlock && state.getValue(FallenLogBlock.STAGE) <= 1) axis = state.getValue(FallenLogBlock.AXIS);
        else if (state.is(DUGOUT_LOGS) && state.hasProperty(BlockStateProperties.AXIS) && state.getValue(BlockStateProperties.AXIS).isHorizontal())
            axis = state.getValue(BlockStateProperties.AXIS);
        else return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(level instanceof ServerLevel server)) return;
        Player player = event.getEntity();
        server.setBlock(pos, DUGOUT_BLANK.get().defaultBlockState().setValue(DugoutBlankBlock.AXIS, axis), Block.UPDATE_ALL);
        server.playSound(null, pos, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 0.7F, 1.4F);
        event.getItemStack().hurtAndBreak(1, player, event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
    }
}
