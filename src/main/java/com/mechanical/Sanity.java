package com.mechanical;

import java.util.function.Supplier;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Fear of the dark: sanity 0..100 (saved, synced, reset on death). It drains while you stand in darkness (block light
 * at the eyes, or a light someone near you holds, at most {@link #DARK_LIGHT}, and no daylight), faster underground,
 * alone and under a blood moon; it comes back by a burning fire, in daylight, in lit places, with company, after a rest
 * in a Straw Bed and from a hot meal. Low sanity only plays tricks (client.SanityClient: whispers, shadows, fake mobs,
 * vignette); nothing attacks. The real effects here: Mining Fatigue and Weakness below {@link #MAD}, nausea bouts at 0.
 * Only in dimensions with skylight (the Nether and End leave it alone).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Sanity {
    public static final float MAX = 100, UNEASY = 70, AFRAID = 40, MAD = 15;
    /** At or below this light (0..15) it counts as dark; 4..7 is dim (no change), 8+ is lit. */
    public static final int DARK_LIGHT = 3, LIT_LIGHT = 8;
    static final float DARK_DRAIN = 0.10F, UNDERGROUND = 1.5F, NIGHT = 1.25F, COMPANY = 0.5F, BLOOD_MOON = 2;
    static final float FIRE = 0.5F, DAYLIGHT = 0.3F, LIT = 0.05F, COMPANY_BONUS = 0.1F, BLOOD_DIM_DRAIN = 0.05F;
    static final double FIRE_RANGE = 6, COMPANY_RANGE = 12, HELD_LIGHT_RANGE = 8;
    static final float REST = 20, REST_MAD = 8, HOT_MEAL = 3;
    static final int REST_COOLDOWN = 2400; // ticks since the last rest before a Straw Bed helps again
    static final int NAUSEA_CHANCE = 30; // 1 in this many seconds at 0 sanity starts a bout

    public static final Supplier<AttachmentType<Float>> SANITY = Mechanical.ATTACHMENTS.register("sanity", () -> AttachmentType
            .builder(() -> MAX)
            .serialize(Codec.FLOAT.fieldOf("value"))
            .sync(ByteBufCodecs.FLOAT)
            .build());

    public static void init() {
        selfCheck();
    }

    public static float of(Player player) {
        return player.getData(SANITY);
    }

    public static void set(Player player, float value) {
        float old = of(player), now = Math.clamp(value, 0, MAX);
        if (now == old) return;
        player.setData(SANITY, now);
        if (player instanceof ServerPlayer sp) {
            String stage = now < MAD && old >= MAD ? "mad" : now < AFRAID && old >= AFRAID ? "afraid" : now < UNEASY && old >= UNEASY ? "uneasy" : null;
            if (stage != null) sp.sendOverlayMessage(Component.translatable("message.mechanical.sanity." + stage).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    /** Light a held item gives off (same rule as client.DynamicLights). */
    public static int heldLight(ItemStack stack) {
        return stack.getItem() instanceof BlockItem block ? block.getBlock().defaultBlockState().getLightEmission() : AncientOak.lightOf(stack);
    }

    /**
     * Sanity change per second. {@code light} is the light at the player's eyes (block light or a nearby held light),
     * {@code daylight} open sky by day. Pure so it can be checked without a world.
     */
    static float rate(int light, boolean daylight, boolean fire, boolean underground, boolean night, boolean company, boolean bloodMoon) {
        if (fire) return FIRE + (company ? COMPANY_BONUS : 0);
        if (light <= DARK_LIGHT && !daylight) {
            float drain = DARK_DRAIN * (underground ? UNDERGROUND : night ? NIGHT : 1) * (company ? COMPANY : 1) * (bloodMoon ? BLOOD_MOON : 1);
            return -drain;
        }
        float gain = daylight ? DAYLIGHT : light >= LIT_LIGHT ? LIT : 0;
        if (gain == 0 && bloodMoon) return -BLOOD_DIM_DRAIN;
        if (company) gain += COMPANY_BONUS;
        return bloodMoon ? gain / 2 : gain;
    }

    private static void selfCheck() {
        check(near(rate(0, false, false, false, true, false, false), -0.125F), "dark night alone");
        check(near(rate(0, false, false, true, true, false, false), -0.15F), "underground");
        check(near(rate(0, false, false, false, true, true, true), -0.125F), "company halves, blood moon doubles");
        check(near(rate(14, false, false, false, true, false, false), LIT), "held torch is light");
        check(near(rate(0, true, false, false, false, false, false), DAYLIGHT), "daylight");
        check(near(rate(0, false, true, true, true, false, true), FIRE), "fire beats everything");
        check(near(rate(5, false, false, false, true, false, false), 0), "dim is neutral");
        check(near(rate(5, false, false, false, true, false, true), -BLOOD_DIM_DRAIN), "blood moon dim drains");
        check(MAX / -rate(0, false, false, false, true, false, false) / 60 > 13, "a full night alone in the dark does not empty it");
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 1e-6F;
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new IllegalStateException("Sanity self-check failed: " + what);
    }

    /** Light at the player's eyes: block light, or a light held by anyone close (falls off 1 per block like DynamicLights). */
    static int lightAt(Player player) {
        Level level = player.level();
        int light = level.getBrightness(LightLayer.BLOCK, BlockPos.containing(player.getEyePosition()));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(HELD_LIGHT_RANGE))) {
            int held = Math.max(heldLight(e.getMainHandItem()), heldLight(e.getOffhandItem()));
            if (held > light) light = Math.max(light, held - (int) e.distanceTo(player));
        }
        return light;
    }

    static boolean hasCompany(Player player) {
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(COMPANY_RANGE))) {
            if (e != player && e instanceof Player other && !other.isSpectator()) return true;
            if (e instanceof com.mechanical.people.Tribesperson person && !person.isAngryAt(player)) return true;
        }
        return false;
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 20 != 0 || player.isCreative() || player.isSpectator()) return;
        if (!level.dimensionType().hasSkyLight()) return;
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        boolean daylight = !level.isDarkOutside() && level.getBrightness(LightLayer.SKY, eye) >= 8;
        boolean fire = NightDangers.nearFire(level, player.position(), FIRE_RANGE);
        boolean underground = !level.canSeeSky(eye) && eye.getY() < level.getSeaLevel();
        float sanity = of(player);
        set(player, sanity + rate(lightAt(player), daylight, fire, underground, level.isDarkOutside(), hasCompany(player), BloodMoon.isActive(level)));

        sanity = of(player);
        if (sanity < MAD) {
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 60, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false, true));
        }
        if (sanity <= 0 && !player.hasEffect(MobEffects.NAUSEA) && player.getRandom().nextInt(NAUSEA_CHANCE) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0, true, false, true));
        }
    }

    /** Straw Bed rest (called before the rest timer is reset): a good rest restores sanity, a mad mind rests badly. */
    static void rest(ServerPlayer player) {
        if (player.getStats().getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)) < REST_COOLDOWN) return;
        float sanity = of(player);
        set(player, sanity + (sanity < MAD ? REST_MAD : REST));
        if (sanity < MAD) player.sendSystemMessage(Component.translatable("message.mechanical.sanity.restless").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    /** A hot meal (cooked food, stews and soups) calms the mind a little. */
    @SubscribeEvent
    static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getItem().has(DataComponents.FOOD)) return;
        // shortcut: "hot" is judged by the item id; upgrade to an item tag if foods without these words should count
        String id = BuiltInRegistries.ITEM.getKey(event.getItem().getItem()).getPath();
        if (id.contains("cooked") || id.contains("roasted") || id.endsWith("_stew") || id.endsWith("_soup") || id.equals("baked_potato")) {
            set(player, of(player) + HOT_MEAL);
        }
    }

    /** /sanity [value [targets]]: shows or sets sanity (op, for testing). */
    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sanity").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ctx.getSource().sendSuccess(() -> Component.literal("Sanity: " + Math.round(of(player))), false);
                    return Math.round(of(player));
                })
                .then(Commands.argument("value", FloatArgumentType.floatArg(0, MAX))
                        .executes(ctx -> setCommand(ctx.getSource(), java.util.List.of(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "value")))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> setCommand(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "value"))))));
    }

    private static int setCommand(CommandSourceStack source, java.util.Collection<ServerPlayer> players, float value) {
        for (ServerPlayer p : players) set(p, value);
        source.sendSuccess(() -> Component.literal("Set sanity to " + value + " for " + players.size() + " player(s)"), true);
        return players.size();
    }

    private Sanity() {}
}
