package com.mechanical.people;

import com.mechanical.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Crimes against a camp: breaking its campfire angers the people living by it (for a day, like hitting one of them), and
 * opening or breaking a chest, barrel or pot near their fire while one of them sees it counts as stealing. Both lower the
 * player's standing with the tribe.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class CampFireTheft {
    private static final double CAMP_RANGE = 48, STORAGE_RANGE = 24, WITNESS_RANGE = 16;

    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        if (event.getLevel().isClientSide() || event.getPlayer().isCreative()) return;
        BlockPos pos = event.getPos();
        if (event.getState().is(Blocks.CAMPFIRE)) {
            event.getLevel().getEntitiesOfClass(Tribesperson.class, new AABB(pos).inflate(CAMP_RANGE), t -> pos.equals(t.home()))
                    .stream().findFirst().ifPresent(t -> t.fireTakenBy(event.getPlayer()));
        } else if (isStorage(event.getLevel().getBlockEntity(pos))) {
            stealing(event.getLevel(), pos, event.getPlayer());
        }
    }

    @SubscribeEvent
    static void onOpen(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide() || event.getEntity().isCreative() || event.getEntity().isSecondaryUseActive()) return;
        if (isStorage(level.getBlockEntity(event.getPos()))) stealing(level, event.getPos(), event.getEntity());
    }

    private static boolean isStorage(BlockEntity be) {
        return be instanceof Container || be instanceof DecoratedPotBlockEntity;
    }

    /** Someone living near the container who can see the player calls them a thief. */
    private static void stealing(LevelAccessor level, BlockPos pos, Player player) {
        level.getEntitiesOfClass(Tribesperson.class, new AABB(pos).inflate(STORAGE_RANGE), t -> t.isAlive() && t.home() != null
                        && t.home().closerThan(pos, STORAGE_RANGE) && t.distanceToSqr(player) < WITNESS_RANGE * WITNESS_RANGE && t.hasLineOfSight(player))
                .stream().findFirst().ifPresent(t -> t.caughtStealing(player));
    }

    private CampFireTheft() {}
}
