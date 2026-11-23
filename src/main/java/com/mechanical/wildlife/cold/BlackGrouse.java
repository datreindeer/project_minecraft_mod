package com.mechanical.wildlife.cold;

import com.mechanical.Calendar;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.FlockBird;
import com.mechanical.wildlife.forestbirds.Capercaillie;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Black grouse: the smaller grouse of the bogs and the open taiga edge. The cock is glossy blue-black with a white wing
 * bar, red brows and a lyre-shaped tail over white undertail feathers; the hen is mottled brown. Like the capercaillie
 * ({@link Capercaillie}: same flight, same deaf grinding while he sings) but the black cock leks only in spring, at dawn
 * on the open bog, bubbling and hissing - and does it often. Leaves a carcass (Birds), with its Lyre Feathers.
 */
public class BlackGrouse extends Capercaillie {
    static final FlockBird.Kind KIND = new FlockBird.Kind("black_grouse", 0.45, 2, 6, 60, 160, 700, 1800, false, false, true, 0.9,
            () -> SoundEvents.CHICKEN_AMBIENT_BABY.value(), 0.5F);
    private static final Identifier COCK = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/black_grouse.png");
    private static final Identifier HEN = Identifier.fromNamespaceAndPath(Mechanical.MODID, "textures/entity/wildlife/black_grouse_hen.png");

    public BlackGrouse(EntityType<? extends BlackGrouse> type, Level level) {
        super(type, level, KIND);
    }

    /** Spring dawns only (season 0). */
    @Override
    protected boolean lekTime() {
        return Calendar.season(level().getOverworldClockTime()) == 0 && super.lekTime();
    }

    @Override
    public Identifier texture() {
        return isHen() ? HEN : COCK;
    }
}
