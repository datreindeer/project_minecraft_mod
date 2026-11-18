package com.mechanical;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Body temperature, 0 (freezing) .. 20 (warm), 12 is comfortable. It drifts towards a target set by the biome, the time
 * of day, rain/water, altitude and nearby burning fires; leather armour insulates against the cold. Cold makes you
 * hungry; very cold freezes you (vanilla frost overlay and freeze damage). Shown by a bar above the hunger bar.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Temperature {
    public static final float MAX = 20, COMFORT = 12, HUNGRY_BELOW = 8, FREEZING_BELOW = 3;
    private static final float DRIFT_PER_SECOND = 0.25F;
    private static final float NIGHT = 5, RAIN = 2, WATER = 5;
    private static final double FIRE_RANGE = 6;
    private static final float FIRE_WARMTH = 10;
    private static final float LEATHER_PER_PIECE = 0.15F, FUR_PER_PIECE = 0.25F;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = Mechanical.ATTACHMENTS;
    public static final Supplier<AttachmentType<Float>> BODY = ATTACHMENTS.register("body_temperature", () -> AttachmentType
            .builder(() -> COMFORT)
            .serialize(Codec.FLOAT.fieldOf("value"))
            .sync(ByteBufCodecs.FLOAT)
            .build());

    public static void init() {}

    public static float of(Player player) {
        return player.getData(BODY);
    }

    /** Air temperature at a spot (same scale as the body): biome, night, rain and altitude. */
    public static float ambient(Level level, BlockPos pos) {
        if (level.dimension() == Level.NETHER) return MAX;
        // biome: plains ~0.8 -> 12.4, taiga 0.25 -> 8, snowy -0.5..0 -> 2..6, desert 2.0 -> hot
        float t = 6 + level.getBiome(pos).value().getBaseTemperature() * 8;
        if (level.isDarkOutside()) t -= level.canSeeSky(pos) ? NIGHT : NIGHT / 2;
        if (level.isRainingAt(pos.above())) t -= RAIN;
        if (pos.getY() > 100) t -= (pos.getY() - 100) * 0.05F;
        return t;
    }

    /**
     * Burn ticks a fire (campfire, kiln) uses this tick: 1 when it is mild, up to 3 on a snowy night; rain on it costs
     * another 1. Fractions are rounded randomly so the average is right.
     */
    public static int fireDrain(Level level, BlockPos pos, RandomSource random) {
        float rate = 1 + 1.5F * Math.clamp((COMFORT - ambient(level, pos)) / COMFORT, 0, 1);
        if (level.isRainingAt(pos.above())) rate += 1;
        int whole = (int) rate;
        return whole + (random.nextFloat() < rate - whole ? 1 : 0);
    }

    /** Where the body temperature is heading, given the surroundings. */
    static float target(Player player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        float t = ambient(level, pos);
        if (player.isInWater()) t -= WATER;

        BlockPos fire = NightDangers.nearestFire(level, player.position(), FIRE_RANGE);
        if (fire != null) {
            double d = Math.sqrt(Vec3.atCenterOf(fire).distanceToSqr(player.position()));
            t += FIRE_WARMTH * (float) (1 - d / FIRE_RANGE);
        }
        if (t < COMFORT) {
            float insulation = 0;
            for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                var item = player.getItemBySlot(slot);
                if (item.is(Items.LEATHER_HELMET) || item.is(Items.LEATHER_CHESTPLATE) || item.is(Items.LEATHER_LEGGINGS) || item.is(Items.LEATHER_BOOTS)) {
                    insulation += LEATHER_PER_PIECE;
                } else if (item.is(Mechanical.FUR_HOOD.get()) || item.is(Mechanical.FUR_COAT.get()) || item.is(Mechanical.FUR_LEGGINGS.get()) || item.is(Mechanical.FUR_BOOTS.get())) {
                    insulation += FUR_PER_PIECE;
                }
            }
            t = COMFORT - (COMFORT - t) * (1 - insulation);
        }
        return Math.clamp(t, 0, MAX);
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel) || player.isCreative() || player.isSpectator()) return;
        float body = of(player);
        if (player.tickCount % 20 == 0) {
            float target = target(player);
            float step = Math.min(DRIFT_PER_SECOND, Math.abs(target - body));
            body += Math.signum(target - body) * step;
            player.setData(BODY, body);
            if (body < HUNGRY_BELOW) player.causeFoodExhaustion(0.5F * (HUNGRY_BELOW - body) / HUNGRY_BELOW);
        }
        // freezing: keep the vanilla frozen state topped up -> frost overlay, slowness and freeze damage
        if (body < FREEZING_BELOW) player.setTicksFrozen(Math.min(player.getTicksFrozen() + 3, player.getTicksRequiredToFreeze() + 20));
    }

    private Temperature() {}
}
