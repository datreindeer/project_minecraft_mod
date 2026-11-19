package com.mechanical;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Blood moon: about one night in seven the moon rises full and red, the sky and fog turn red, and the zombie hordes
 * come (see NightDangers). It lasts from dusk to dawn. Which nights is fixed by the world seed, so a restart keeps it.
 * The client only learns on/off through {@link Payload}; the look is in client.BloodMoonClient.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class BloodMoon {
    private static final float CHANCE = 1.0F / 7.0F;
    private static final long START = 12000, END = 23500; // dusk to just before dawn, in day time
    private static final Map<Level, Boolean> ACTIVE = new WeakHashMap<>();
    /** Night forced to a blood moon by /bloodmoon (for testing), per level. */
    private static final Map<Level, Long> FORCED = new WeakHashMap<>();

    public record Payload(boolean active) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "blood_moon"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = ByteBufCodecs.BOOL.map(Payload::new, Payload::active).cast();

        @Override
        public Type<Payload> type() {
            return TYPE;
        }
    }

    /** Is night number {@code night} (game time / 24000) a blood moon night? */
    public static boolean isBloodNight(ServerLevel level, long night) {
        if (Long.valueOf(night).equals(FORCED.get(level))) return true;
        return RandomSource.create(level.getSeed() ^ night * 0x9E3779B97F4A7C15L).nextFloat() < CHANCE;
    }

    /** Blood moon over this level right now (overworld only). */
    public static boolean isActive(Level level) {
        return Boolean.TRUE.equals(ACTIVE.get(level));
    }

    private static boolean computeActive(ServerLevel level) {
        long time = level.getOverworldClockTime();
        long dayTime = time % 24000;
        return dayTime >= START && dayTime < END && isBloodNight(level, time / 24000);
    }

    @SubscribeEvent
    static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Payload.TYPE, Payload.CODEC);
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 20 != 0) return;
        boolean active = computeActive(level);
        if (active == isActive(level)) return;
        ACTIVE.put(level, active);
        PacketDistributor.sendToPlayersInDimension(level, new Payload(active));
        if (!active) return;
        for (ServerPlayer player : level.players()) {
            player.sendSystemMessage(Component.translatable("message.mechanical.blood_moon").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.8F, 0.6F);
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity());
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getEntity());
    }

    private static void sync(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer sp) PacketDistributor.sendToPlayer(sp, new Payload(isActive(sp.level())));
    }

    /** /bloodmoon: makes tonight a blood moon (it shows from dusk; set the time to night to see it at once). */
    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bloodmoon").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
            ServerLevel level = ctx.getSource().getServer().overworld();
            FORCED.put(level, level.getOverworldClockTime() / 24000);
            ctx.getSource().sendSuccess(() -> Component.literal("Tonight is a blood moon"), true);
            return 1;
        }));
    }

    private BloodMoon() {}
}
