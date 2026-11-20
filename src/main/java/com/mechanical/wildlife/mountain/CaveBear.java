package com.mechanical.wildlife.mountain;

import com.mechanical.wildlife.BrownBear;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;

/**
 * Cave bear: the brown bear's bigger, heavier Ice Age cousin (all of {@link BrownBear}'s behaviour: rears and swipes,
 * charges in short bursts, never flees, a mother guards her cubs). It dens in caves under the mountains (spawns only in
 * the dark, out of the sky) and lies dormant there day and night - hibernating - until something comes close: woken, it
 * goes for whoever woke it. Out in the open it sleeps at night like the brown bear. Only a Tusk Dagger or better cuts
 * its hide (Butchery.requireKnife); the pelt needs a whole tall Drying Rack. Always leaves its skull, a trophy.
 */
public class CaveBear extends BrownBear {
    private static final Identifier TEXTURE = Ibex.tex("cave_bear"), CUB_TEXTURE = Ibex.tex("cave_bear_cub");
    /** Sky light at or below this counts as inside the den. */
    private static final int DEN_SKYLIGHT = 4;

    public CaveBear(EntityType<? extends CaveBear> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 90.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 11.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.85).add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public boolean inDen() {
        return level().getBrightness(LightLayer.SKY, blockPosition()) <= DEN_SKYLIGHT;
    }

    /** Dormant all the time in its den; outside, asleep at night. */
    @Override
    protected boolean sleepTime() {
        return inDen() || level().isDarkOutside();
    }

    /** Roused from its winter sleep: a deep, long roar, then it charges. */
    @Override
    protected void onWoken(Player near) {
        super.onWoken(near);
        if (!isBaby()) playSound(net.minecraft.sounds.SoundEvents.POLAR_BEAR_WARNING, 2.5F, 0.55F);
    }

    @Override
    public Identifier texture() {
        return isBaby() ? CUB_TEXTURE : TEXTURE;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.82F; // deeper still than the brown bear
    }
}
