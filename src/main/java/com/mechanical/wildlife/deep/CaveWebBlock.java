package com.mechanical.wildlife.deep;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Cave Web: the Cavern Spider's great web across a narrow passage. Whoever walks in is held far harder than by a cobweb
 * (a tenth of the speed, no jumping out) and stays slowed for a moment after (Slowness II); the struggling tugs the web,
 * and every Cavern Spider within {@link #ALERT_RANGE} blocks comes for the catch. The spiders themselves walk through it.
 * Broken with shears or a knife it comes away whole (loot table) and can be set again as a trap; otherwise only String.
 */
public class CaveWebBlock extends WebBlock {
    static final double ALERT_RANGE = 16;
    static final int SLOW_TICKS = 40;

    public CaveWebBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (entity instanceof CavernSpider) return;
        entity.makeStuckInBlock(state, new Vec3(0.1, 0.02, 0.1));
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;
        if (living instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, 1));
        if (living.tickCount % 10 == 0) alert(level, pos, living);
    }

    /** The web shakes: the spiders round about make for whatever is caught in it. */
    static void alert(Level level, BlockPos pos, LivingEntity caught) {
        for (CavernSpider spider : level.getEntitiesOfClass(CavernSpider.class, new AABB(pos).inflate(ALERT_RANGE))) {
            if (spider.getTarget() == null) spider.setTarget(caught);
        }
    }
}
