package com.mechanical.people;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.BrownBear;
import com.mechanical.wildlife.GiantDeer;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.underground.GiantCentipede;
import com.mechanical.wildlife.underground.Lindworm;
import com.mechanical.wildlife.underground.Troll;
import com.mechanical.wildlife.underground.Vittra;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Hostile monsters go after tribespeople like zombies go after villagers. Neutral mobs (endermen, piglins) leave them be;
 * Mushroom Men creep up on them too. The mod's own man-eaters that are animals rather than Monsters (trolls, vittra,
 * lindworms, centipedes, giant deer, bears) take tribespeople too. Predators and Gloson pick them as prey themselves.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class MonstersHuntPeople {
    private static final double RANGE = 16;

    /** A mod creature that attacks players on sight, and so tribespeople too. */
    static boolean huntsPeople(Entity e) {
        return e instanceof Troll || e instanceof Vittra || e instanceof Lindworm || e instanceof GiantCentipede || e instanceof GiantDeer || e instanceof BrownBear;
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Monster monster && !(monster instanceof NeutralMob))
            monster.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(monster, Tribesperson.class, true));
        else if (huntsPeople(event.getEntity()) && event.getEntity() instanceof WildAnimal animal)
            animal.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(animal, Tribesperson.class, 10, true, false,
                    (t, l) -> !animal.isBaby() && !animal.isDozing() && !(animal instanceof GiantDeer d && d.isBroken()) && t.distanceToSqr(animal) < RANGE * RANGE));
    }

    private MonstersHuntPeople() {}
}
