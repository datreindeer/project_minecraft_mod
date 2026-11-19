package com.mechanical.people;

import com.mechanical.Mechanical;

import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Hostile monsters go after tribespeople like zombies go after villagers. Creepers and neutral mobs (endermen, piglins) leave them be. */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class MonstersHuntPeople {
    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Monster monster)
                || monster instanceof NeutralMob || monster instanceof Creeper) return;
        monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, Tribesperson.class, true));
    }

    private MonstersHuntPeople() {}
}
