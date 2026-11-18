package com.mechanical.people;

import com.mechanical.Mechanical;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** Breaking a camp's campfire angers the people living by it (for a day, like hitting one of them). */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class CampFireTheft {
    private static final double CAMP_RANGE = 48;

    @SubscribeEvent
    static void onBreak(BreakBlockEvent event) {
        if (event.getLevel().isClientSide() || !event.getState().is(Blocks.CAMPFIRE) || event.getPlayer().isCreative()) return;
        var pos = event.getPos();
        event.getLevel().getEntitiesOfClass(Tribesperson.class, new AABB(pos).inflate(CAMP_RANGE), t -> pos.equals(t.home()))
                .stream().findFirst().ifPresent(t -> t.fireTakenBy(event.getPlayer()));
    }

    private CampFireTheft() {}
}
