package com.mechanical;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * of day, rain/water, altitude and nearby burning fires; leather armour and fur (also a worn cloak) insulate against the cold. Cold makes you
 * hungry; very cold freezes you (vanilla frost overlay and freeze damage). Shown by a bar above the hunger bar.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Temperature {
    public static final float MAX = 20, COMFORT = 12, HUNGRY_BELOW = 8, FREEZING_BELOW = 3;
    private static final float DRIFT_PER_SECOND = 0.25F;
    private static final float NIGHT = 5, RAIN = 2, WATER = 5;
    private static final double FIRE_RANGE = 6;
    private static final float FIRE_WARMTH = 10;
    /** Armour pieces that keep out the cold like Fur armour (FUR_PER_PIECE), so new fur goods need no code here. */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> FUR_INSULATION = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "fur_insulation"));
    public static final float LEATHER_PER_PIECE = 0.15F, FUR_PER_PIECE = 0.25F, WOLF_FUR_PER_PIECE = 0.2F;
    /** A rest in a Furred Straw Bed (StrawBedRest) keeps you this much warmer for FURRED_BED_TICKS. */
    public static final float FURRED_BED_WARMTH = 0.3F;
    public static final int FURRED_BED_TICKS = 6000;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = Mechanical.ATTACHMENTS;
    public static final Supplier<AttachmentType<Float>> BODY = ATTACHMENTS.register("body_temperature", () -> AttachmentType
            .builder(() -> COMFORT)
            .serialize(Codec.FLOAT.fieldOf("value"))
            .sync(ByteBufCodecs.FLOAT)
            .build());

    /** Game time until which a Furred Straw Bed's warmth lasts (not saved: a relog ends it early). */
    private static final Supplier<AttachmentType<Long>> WARM_BEDROLL = ATTACHMENTS.register("warm_bedroll", () -> AttachmentType.builder(() -> 0L).build());

    public static void init() {}

    /** Share of the cold an armour piece keeps out. */
    public static float insulation(ItemStack item) {
        if (item.is(Items.LEATHER_HELMET) || item.is(Items.LEATHER_CHESTPLATE) || item.is(Items.LEATHER_LEGGINGS) || item.is(Items.LEATHER_BOOTS)) return LEATHER_PER_PIECE;
        if (item.is(Mechanical.FUR_HOOD.get()) || item.is(Mechanical.FUR_COAT.get()) || item.is(Mechanical.FUR_LEGGINGS.get()) || item.is(Mechanical.FUR_BOOTS.get())) return FUR_PER_PIECE;
        if (item.is(Mechanical.WOLF_HOOD.get()) || item.is(Mechanical.WOLF_FUR_COAT.get()) || item.is(Mechanical.WOLF_FUR_LEGGINGS.get()) || item.is(Mechanical.WOLF_FUR_BOOTS.get())) return WOLF_FUR_PER_PIECE;
        if (item.is(FUR_INSULATION)) return FUR_PER_PIECE; // fur pieces added by other features (data tag)
        if (item.is(com.mechanical.wildlife.coast.CoastFauna.SEALSKIN_PARKA.get()) || item.is(com.mechanical.wildlife.coast.CoastFauna.SEALSKIN_BOOTS.get()))
            return com.mechanical.wildlife.coast.CoastFauna.SEALSKIN_PER_PIECE;
        if (item.is(com.mechanical.wildlife.river.RiverFauna.WATERPROOF_FUR)) return com.mechanical.wildlife.river.RiverFauna.FUR_INSULATION;
        return 0;
    }

    /** What is worn that can keep the cold out: the four armour pieces and the cloak (equipment.Accessory.CLOAK). */
    public static java.util.List<ItemStack> wornForWarmth(Player player) {
        return java.util.List.of(player.getItemBySlot(EquipmentSlot.HEAD), player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS), player.getItemBySlot(EquipmentSlot.FEET),
                com.mechanical.equipment.Equipment.get(player, com.mechanical.equipment.Accessory.CLOAK));
    }

    /** Share of the water's chill kept out: each Sealskin piece worn. */
    static float waterproofing(Player player) {
        float w = 0;
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.CHEST, EquipmentSlot.FEET}) {
            ItemStack item = player.getItemBySlot(slot);
            if (item.is(com.mechanical.wildlife.coast.CoastFauna.SEALSKIN_PARKA.get()) || item.is(com.mechanical.wildlife.coast.CoastFauna.SEALSKIN_BOOTS.get()))
                w += com.mechanical.wildlife.coast.CoastFauna.SEALSKIN_WATERPROOF;
        }
        return w;
    }

    static long warmBedrollUntil(Player player) {
        return player.getData(WARM_BEDROLL);
    }

    /** Called after a rest in a Furred Straw Bed. */
    static void warmFromBedroll(Player player) {
        player.setData(WARM_BEDROLL, player.level().getGameTime() + FURRED_BED_TICKS);
    }

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
        // waterproof sealskin and fur (beaver, otter) keep the wet's chill off
        float furDry = com.mechanical.wildlife.river.RiverFauna.waterproofing(player);
        if (player.isInWater()) t -= WATER * (1 - Math.min(1, waterproofing(player) + furDry));
        if (level.isRainingAt(pos.above())) t += RAIN * furDry;

        BlockPos fire = NightDangers.nearestFire(level, player.position(), FIRE_RANGE);
        if (fire != null) {
            double d = Math.sqrt(Vec3.atCenterOf(fire).distanceToSqr(player.position()));
            double reach = NightDangers.reach(level, fire); // a Fire Pit warms half as much again, half as far again
            t += FIRE_WARMTH * (float) (reach * Math.max(0, 1 - d / (FIRE_RANGE * reach)));
        }
        if (t < COMFORT) {
            float insulation = 0;
            for (ItemStack worn : wornForWarmth(player)) insulation += insulation(worn);
            if (warmBedrollUntil(player) > level.getGameTime()) insulation += FURRED_BED_WARMTH;
            insulation = Math.min(insulation, 1);
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
