package com.mechanical.people;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;

/**
 * Road Marker (RoadBuilding, A5): a tribe's leader or trader hands it out at Friendly or better. Set down where the road
 * should end, at most RoadBuilding.MAX_LEN blocks from their camp, it becomes the tribe's banner on the spot and they start
 * building toward it. Past RoadBuilding.FREE_LEN blocks it costs Silver Coins (RoadBuilding.price), taken as it goes down.
 */
public class RoadMarkerItem extends Item {
    public RoadMarkerItem(Properties properties) {
        super(properties);
    }

    /** A marker for this tribe, whose camp is centred on {@code base}. */
    static ItemStack of(Tribe tribe, BlockPos base) {
        ItemStack stack = new ItemStack(People.ROAD_MARKER.get());
        CompoundTag tag = new CompoundTag();
        tag.putString("tribe", tribe.id.toString());
        tag.putLong("base", base.asLong());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.ITEM_NAME, Component.translatable("item.mechanical.road_marker.of", tribe.name()));
        return stack;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel level) || !(ctx.getPlayer() instanceof ServerPlayer player)) return InteractionResult.SUCCESS;
        CustomData data = ctx.getItemInHand().get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains("tribe")) return InteractionResult.FAIL;
        CompoundTag tag = data.copyTag();
        UUID tribeId = UUID.fromString(tag.getStringOr("tribe", ""));
        BlockPos base = BlockPos.of(tag.getLongOr("base", 0L));
        BlockPos at = ctx.getClickedPos().relative(ctx.getClickedFace());
        Component fail = place(level, player, Tribe.of(tribeId), base, at);
        if (fail != null) {
            player.sendOverlayMessage(fail);
            return InteractionResult.FAIL;
        }
        ctx.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Sets the marker down at {@code at}: the road is paid for and starts. Returns why not, or null. */
    static Component place(ServerLevel level, ServerPlayer player, Tribe tribe, BlockPos base, BlockPos at) {
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD || Neighbours.get(level).settlement(tribe.id) == null)
            return Component.translatable("message.mechanical.road.no_camp", tribe.name());
        if (!level.getBlockState(at).canBeReplaced() || !level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP))
            return Component.translatable("message.mechanical.road.bad_spot");
        RoadBuilding data = RoadBuilding.get(level.getServer());
        if (data.building(tribe.id) != null) return Component.translatable("message.mechanical.road.busy", tribe.name());
        int length = (int) RoadBuilding.length(base, at), cost = RoadBuilding.price(length, RoadBuilding.PATH);
        if (cost < 0) return Component.translatable("message.mechanical.road.too_far", length, RoadBuilding.MAX_LEN);
        if (!RoadBuilding.pay(player, cost)) return Component.translatable("message.mechanical.road.need_silver", cost);
        ItemStack banner = tribe.banner(level.registryAccess());
        BlockState state = ((BannerItem) banner.getItem()).getBlock().defaultBlockState()
                .setValue(BannerBlock.ROTATION, RotationSegment.convertToSegment(player.getYRot() + 180));
        level.setBlock(at, state, Block.UPDATE_ALL);
        if (level.getBlockEntity(at) instanceof BannerBlockEntity be) be.applyComponentsFromItemStack(banner);
        data.add(new RoadBuilding.Road(tribe.id, player.getUUID(), base, at, RoadBuilding.PATH, List.of(), 0, false, List.of()));
        player.sendSystemMessage(Component.translatable("message.mechanical.road.placed", tribe.name(), length, cost));
        TribeVisits.award(player, "tribe_road");
        return null;
    }
}
