package com.mechanical.people;

import com.mechanical.Mechanical;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Tribespeople's own voice (living-world-plan B8): murmur, hurt, death, greeting and trade. Their sounds.json entries
 * have empty sound lists for now, so the tribes are silent (no villager sounds) until the chosen CC0 recordings are
 * added as .ogg files (docs/tribe-sounds.md lists them and the entries to fill).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class TribeSounds {
    public static final SoundEvent AMBIENT = event("entity.tribesperson.ambient"), HURT = event("entity.tribesperson.hurt"),
            DEATH = event("entity.tribesperson.death"), GREET = event("entity.tribesperson.greet"), TRADE = event("entity.tribesperson.trade");

    private static SoundEvent event(String name) {
        return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Mechanical.MODID, name));
    }

    @SubscribeEvent
    static void register(RegisterEvent event) {
        for (SoundEvent s : new SoundEvent[]{AMBIENT, HURT, DEATH, GREET, TRADE}) event.register(Registries.SOUND_EVENT, s.location(), () -> s);
    }

    private TribeSounds() {}
}
