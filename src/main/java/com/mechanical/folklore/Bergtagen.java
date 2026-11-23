package com.mechanical.folklore;

import com.mechanical.Calendar;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.underground.UndergroundFauna;
import com.mechanical.wildlife.underground.Vittra;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

/**
 * Bergtagen: sleep alone in the mountains (a mountain biome, height {@link #MIN_Y}+, nobody else within
 * {@link #ALONE_RANGE} blocks) and one morning in {@link #CHANCE} you may wake inside the mountain, at most once a calendar
 * year: a dark hall {@link #DEPTH} blocks down, {@link #VITTRA} Vittra about you, a chest of their things (loot
 * chests/bergtagen) and a ladder shaft up. You are their guest (Mountain Guest, {@link #GUEST_TICKS}): no vittra hears you.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Bergtagen {
    static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "chests/bergtagen"));
    static final int MIN_Y = 100, ALONE_RANGE = 64, DEPTH = 18, VITTRA = 3, GUEST_TICKS = 20 * 300;
    static final float CHANCE = 0.25F;
    static final String YEAR_KEY = "mechanical_bergtagen_year";

    @SubscribeEvent
    static void onWake(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.wakeImmediately()) return;
        ServerLevel level = player.level();
        if (!level.isBrightOutside()) return;
        boolean alone = level.getPlayers(p -> p != player && !p.isSpectator() && p.distanceTo(player) < ALONE_RANGE).isEmpty();
        long year = Calendar.year(level.getOverworldClockTime());
        if (!canTake(level.getBiome(player.blockPosition()).is(BiomeTags.IS_MOUNTAIN), player.getBlockY(), alone,
                player.getPersistentData().getLongOr(YEAR_KEY, -1) >= year, level.getRandom().nextFloat())) return;
        BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, player.blockPosition());
        player.getPersistentData().putLong(YEAR_KEY, year);
        // after vanilla has stood the sleeper up beside the bed
        var server = level.getServer();
        server.schedule(new net.minecraft.server.TickTask(server.getTickCount() + 1, () -> take(level, player, top.below(DEPTH), top.getY())));
    }

    /** Pure: may the mountain take this sleeper? */
    static boolean canTake(boolean mountain, int y, boolean alone, boolean takenThisYear, float roll) {
        return mountain && y >= MIN_Y && alone && !takenThisYear && roll < CHANCE;
    }

    /** Wakes the player in a hall whose floor centre is {@code floor}, with a ladder shaft up to {@code surfaceY}. */
    static void take(ServerLevel level, ServerPlayer player, BlockPos floor, int surfaceY) {
        // the hall: 7x4x7 of air on a stone floor
        for (BlockPos p : BlockPos.betweenClosed(floor.offset(-3, -1, -3), floor.offset(3, 4, 3))) {
            boolean shell = p.getY() == floor.getY() - 1 || p.getY() == floor.getY() + 4 || Math.abs(p.getX() - floor.getX()) == 3 || Math.abs(p.getZ() - floor.getZ()) == 3;
            if (shell) {
                if (level.getBlockState(p).isAir() || !level.getFluidState(p).isEmpty()) level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
            } else {
                level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            }
        }
        // the shaft: in the north-west corner, a ladder on its north wall
        BlockPos shaft = floor.offset(-2, 0, -2);
        for (int y = floor.getY(); y <= surfaceY; y++) {
            BlockPos p = new BlockPos(shaft.getX(), y, shaft.getZ());
            if (y > floor.getY() + 3) level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            var ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
            if (ladder.canSurvive(level, p)) level.setBlockAndUpdate(p, ladder);
        }
        BlockPos chest = floor.offset(2, 0, 2);
        level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(chest) instanceof ChestBlockEntity c) c.setLootTable(LOOT, level.getRandom().nextLong());
        for (int i = 0; i < VITTRA; i++) {
            Vittra v = UndergroundFauna.VITTRA.get().create(level, EntitySpawnReason.EVENT);
            if (v == null) continue;
            v.snapTo(floor.getX() + 0.5 + (i - 1) * 2, floor.getY(), floor.getZ() + 2.5, 180, 0);
            level.addFreshEntity(v);
        }
        player.addEffect(new MobEffectInstance(Beings.MOUNTAIN_GUEST, GUEST_TICKS, 0));
        player.teleportTo(level, floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5, java.util.Set.of(), player.getYRot(), 0, true);
        Faith.add(player, Faith.NEW_KIND);
        Faith.award(player, "folklore/bergtagen");
        player.sendSystemMessage(Component.translatable("message.mechanical.bergtagen").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }

    private Bergtagen() {}
}
