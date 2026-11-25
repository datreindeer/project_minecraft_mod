package com.mechanical;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import com.mechanical.worldgen.ModBiomes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Body temperature, 0 (freezing) .. 20 (warm), 12 is comfortable. It drifts towards a target set by the biome, the time
 * of day, rain/water, altitude, nearby burning fires and Magma Blocks (up to comfortable); leather armour and fur (also a worn cloak) insulate against the cold,
 * and so does a house: walls and roof round you ({@link #shelter}; turf, thatch and daub best, wood next, stone least), which also keeps a fire's heat in. Cold makes you
 * hungry; very cold freezes you (vanilla frost overlay and freeze damage). Shown by a bar above the hunger bar.
 * <p>
 * Heat: two cave biomes set their own air ({@link #cave(Holder, int)}): the Ice Caves near freezing, the Magma Vents far
 * above {@link #MAX}. Only the vents' air can push the body above {@link #OVERHEAT} (sun, the Nether and fires stop there,
 * and so does being in water or rain). Above it you tire (food exhaustion), above {@link #SCORCHING} you burn
 * (fire damage, so Fire Resistance protects). Clothes neither help nor hurt in the heat.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Temperature {
    public static final float MAX = 20, COMFORT = 12, HUNGRY_BELOW = 8, FREEZING_BELOW = 3;
    /** Overheating: hungry above OVERHEAT, burning above SCORCHING (one heart every 2 s). */
    public static final float OVERHEAT = 17, SCORCHING = 19;
    /** The air of the Ice Caves (any depth) and of the Magma Vents. */
    public static final float ICE_CAVE_AIR = 2, MAGMA_VENT_AIR = 30;
    /** Granted from code: cooling off in water in the Magma Vents. */
    public static final Identifier HOT_SPRING_ADVANCEMENT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "wilderness/magma_vents_cool_off");
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
        return Jewelry.warmth(item); // a ring or necklace set with a warm gem
    }

    /** What is worn that can keep the cold out: the four armour pieces, the cloak and the jewellery (equipment.Accessory). */
    public static java.util.List<ItemStack> wornForWarmth(Player player) {
        return java.util.List.of(player.getItemBySlot(EquipmentSlot.HEAD), player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS), player.getItemBySlot(EquipmentSlot.FEET),
                com.mechanical.equipment.Equipment.get(player, com.mechanical.equipment.Accessory.CLOAK),
                com.mechanical.equipment.Equipment.get(player, com.mechanical.equipment.Accessory.NECK),
                com.mechanical.equipment.Equipment.get(player, com.mechanical.equipment.Accessory.RING_1),
                com.mechanical.equipment.Equipment.get(player, com.mechanical.equipment.Accessory.RING_2));
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

    /** Below this Y and out of sight of the sky you are in a cave: only the depth counts, not biome, season, night or rain. */
    public static final int CAVE_TOP = 50;

    /** A cave's air: 10 at CAVE_TOP, 0 (freezing) at the bottom of the world (Y -64); unclamped, so the deep end stays cold. */
    public static float cave(int y) {
        return 10F * (y + 64) / (CAVE_TOP + 64);
    }

    /** A cave's air in this biome: the Ice Caves and Magma Vents set their own, any other cave goes by depth. */
    public static float cave(Holder<Biome> biome, int y) {
        if (biome.is(ModBiomes.ICE_CAVES)) return ICE_CAVE_AIR;
        if (biome.is(ModBiomes.MAGMA_VENTS)) return MAGMA_VENT_AIR;
        return cave(y);
    }

    /** Air temperature at a spot (same scale as the body): biome (shifted by the season, Seasons), night, rain and altitude; in caves the depth. */
    public static float ambient(Level level, BlockPos pos) {
        if (level.dimension() == Level.NETHER) return MAX;
        if (level.dimension() == Level.OVERWORLD && pos.getY() < CAVE_TOP && !level.canSeeSky(pos)) return cave(level.getBiome(pos), pos.getY());
        // biome: plains ~0.8 -> 12.4, taiga 0.25 -> 8, snowy -0.5..0 -> 2..6, desert 2.0 -> hot
        float base = level.getBiome(pos).value().getBaseTemperature();
        float t = 6 + (base + Seasons.offset(base, Seasons.curve(level.getOverworldClockTime()))) * 8;
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

    /** A house at its best (every ray hits warm walls and roof) counts as this much insulation, like clothes. */
    public static final float SHELTER_MAX = 0.6F;
    /** How far the walls and roof may be from your head. */
    public static final int SHELTER_RANGE = 8;
    /** Block tags for how well a wall or roof keeps the warmth in; any other solid block (stone) counts as {@link #STONE_WARMTH}. */
    public static final TagKey<Block> INSULATION_HIGH = blockTag("insulation/high"), INSULATION_MEDIUM = blockTag("insulation/medium"),
            INSULATION_POOR = blockTag("insulation/poor");
    public static final float HIGH_WARMTH = 1, MEDIUM_WARMTH = 0.7F, STONE_WARMTH = 0.4F, POOR_WARMTH = 0.1F;
    private static final int[][] WALLS = {{1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}};

    private static TagKey<Block> blockTag(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Mechanical.MODID, path));
    }

    /** How well one block keeps warmth in: by tag, else stone for anything solid, else 0 (open). */
    public static float warmth(BlockState state) {
        if (state.is(INSULATION_HIGH)) return HIGH_WARMTH;
        if (state.is(INSULATION_MEDIUM)) return MEDIUM_WARMTH;
        if (state.is(INSULATION_POOR)) return POOR_WARMTH;
        return state.isSolid() ? STONE_WARMTH : 0;
    }

    /** The first wall or roof along a ray from head, or 0 when it reaches open air. */
    private static float ray(Level level, BlockPos from, int dx, int dy, int dz) {
        BlockPos.MutableBlockPos p = from.mutable();
        for (int i = 0; i < SHELTER_RANGE; i++) {
            BlockState state = level.getBlockState(p.move(dx, dy, dz));
            if (state.isAir()) continue;
            float w = warmth(state);
            if (w > 0) return w;
        }
        return 0;
    }

    /**
     * 0..1: how sheltered the spot is. Rays go up and to 8 sides from the head; without a roof it is 0, otherwise the
     * mean warmth of what the rays hit (a gap or open door counts 0). A turf or thatch house is 1, stone walls 0.4.
     */
    public static float shelter(Level level, BlockPos head) {
        float roof = ray(level, head, 0, 1, 0);
        if (roof == 0) return 0;
        float sum = roof;
        for (int[] d : WALLS) sum += ray(level, head, d[0], 0, d[1]);
        return sum / (WALLS.length + 1);
    }

    /** Where the body temperature is heading, given the surroundings. */
    static float target(Player player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        float air = ambient(level, pos), t = air;
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
        float shelter = shelter(level, BlockPos.containing(player.getEyePosition()));
        // walls round a fire keep its heat in: up to half as much again in a tight house
        if (fire != null) t += FIRE_WARMTH * 0.5F * shelter * (float) Math.max(0, 1 - Math.sqrt(Vec3.atCenterOf(fire).distanceToSqr(player.position())) / FIRE_RANGE);
        // a Magma Block takes the chill off, but never warms you past comfortable; walls keep its heat in like a fire's
        if (t < COMFORT) t = Math.min(COMFORT, t + Magma.warmth(level, pos) * (1 + 0.5F * shelter));
        if (t < COMFORT) {
            float insulation = shelter * SHELTER_MAX;
            for (ItemStack worn : wornForWarmth(player)) insulation += insulation(worn);
            if (warmBedrollUntil(player) > level.getGameTime()) insulation += FURRED_BED_WARMTH;
            if (player.hasEffect(Kitchen.TIPSY)) insulation += Kitchen.TIPSY_WARMTH; // a warm belly
            if (player.hasEffect(Kitchen.FROST_WARD)) insulation += Kitchen.FROST_WARD_WARMTH;
            insulation = Math.min(insulation, 1);
            t = COMFORT - (COMFORT - t) * (1 - insulation);
        }
        return Math.clamp(heatCap(t, air, player.isInWaterOrRain()), 0, MAX);
    }

    /** Only the Magma Vents' air heats you past OVERHEAT; anything else (sun, Nether, fires) and being wet stop there. */
    public static float heatCap(float t, float air, boolean wet) {
        return air < MAGMA_VENT_AIR || wet ? Math.min(t, OVERHEAT) : t;
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.isCreative() || player.isSpectator()) return;
        float body = of(player);
        if (player.tickCount % 20 == 0) {
            float target = target(player);
            float step = Math.min(DRIFT_PER_SECOND, Math.abs(target - body));
            body += Math.signum(target - body) * step;
            player.setData(BODY, body);
            if (body < HUNGRY_BELOW) player.causeFoodExhaustion(0.5F * (HUNGRY_BELOW - body) / HUNGRY_BELOW);
            if (body > OVERHEAT) player.causeFoodExhaustion(0.5F * (body - OVERHEAT) / (MAX - OVERHEAT));
            if (body > SCORCHING && player.tickCount % 40 == 0) player.hurtServer(level, level.damageSources().onFire(), 1);
            if (player.isInWater() && player instanceof ServerPlayer sp && ambient(level, player.blockPosition()) >= MAGMA_VENT_AIR)
                Kettle.award(sp, HOT_SPRING_ADVANCEMENT);
        }
        // freezing: keep the vanilla frozen state topped up -> frost overlay, slowness and freeze damage
        if (body < FREEZING_BELOW) player.setTicksFrozen(Math.min(player.getTicksFrozen() + 3, player.getTicksRequiredToFreeze() + 20));
    }

    private Temperature() {}
}
