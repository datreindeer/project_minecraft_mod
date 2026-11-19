package com.mechanical;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Windy nights: about one night in five (fixed by the world seed, like the blood moon) a strong wind blows from dusk to
 * dawn. Campfires and torches out in the open can blow out: a campfire sinks to embers, a torch becomes an Unlit Torch.
 * A roof over them, or walls on three sides, keeps the wind off. Rain under open sky puts them out too, at any time of
 * day (more slowly; twice as fast in a thunderstorm). Both get random ticks (mixins) which call in here.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Wind {
    private static final float CHANCE = 1.0F / 5.0F;
    private static final long START = 12000, END = 23500;
    /** Chance per random tick (~ every 68 s per block) that an exposed fire blows out: a torch lasts ~2-3 min, a campfire ~3 min. */
    private static final float TORCH_BLOW = 0.45F, CAMPFIRE_BLOW = 0.35F;
    /** Rain on an exposed fire (any time of day): a torch lasts ~4-5 min, a campfire ~6 min; a thunderstorm doubles it. */
    private static final float TORCH_RAIN = 0.25F, CAMPFIRE_RAIN = 0.18F;
    /** The Fire Pit (one roll per pit): wind ~9 min, rain ~19 min. */
    private static final float FIRE_PIT_BLOW = 0.12F, FIRE_PIT_RAIN = 0.06F;
    private static final Map<Level, Boolean> ACTIVE = new WeakHashMap<>();
    /** Night forced windy by /windynight (testing). */
    private static final Map<Level, Long> FORCED = new WeakHashMap<>();

    public record Payload(boolean windy) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "wind"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = ByteBufCodecs.BOOL.map(Payload::new, Payload::windy).cast();

        @Override
        public Type<Payload> type() {
            return TYPE;
        }
    }

    /** Is night number {@code night} a windy one? (different salt from the blood moon, so they don't always coincide) */
    public static boolean isWindyNight(ServerLevel level, long night) {
        if (Long.valueOf(night).equals(FORCED.get(level))) return true;
        return RandomSource.create(level.getSeed() * 31 + night * 0x2545F4914F6CDD1DL + 7).nextFloat() < CHANCE;
    }

    public static boolean isWindy(Level level) {
        return Boolean.TRUE.equals(ACTIVE.get(level.dimension() == Level.OVERWORLD ? level : null));
    }

    private static boolean computeWindy(ServerLevel level) {
        long time = level.getOverworldClockTime();
        long dayTime = time % 24000;
        return dayTime >= START && dayTime < END && isWindyNight(level, time / 24000);
    }

    /** Out in the wind: open sky above and not walled in on three sides. */
    public static boolean exposed(Level level, BlockPos pos) {
        if (!level.canSeeSky(pos.above())) return false;
        int walls = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(pos.relative(d)).isSolid() || level.getBlockState(pos.relative(d).above()).isSolid()) walls++;
        }
        return walls < 3;
    }

    /** Chance this random tick that wind or rain puts out an exposed fire (0 if neither is blowing/falling on it). */
    private static float douse(ServerLevel level, BlockPos pos, float wind, float rain) {
        float chance = isWindy(level) ? wind : 0;
        if (level.isRainingAt(pos.above())) chance = Math.max(chance, rain * (level.isThundering() ? 2 : 1));
        return chance;
    }

    /** Random tick of a lit campfire (CampfireBlockMixin). */
    public static void campfireTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= douse(level, pos, CAMPFIRE_BLOW, CAMPFIRE_RAIN) || !exposed(level, pos)) return;
        if (level.getBlockEntity(pos) instanceof CampfireBlockEntity fire) ((CampfireAccess) fire).mechanical$blowOut();
    }

    /** Random tick of a lit Fire Pit's corner (FirePitBlock): a big fire holds out about three times as long as a campfire. */
    public static void firePitTick(ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= douse(level, pos, FIRE_PIT_BLOW, FIRE_PIT_RAIN) || !exposed(level, pos)) return;
        if (level.getBlockEntity(pos) instanceof FirePitBlockEntity pit) pit.blowOut();
    }

    /** Random tick of a torch (TorchBlockMixin): torch and wall torch become their unlit versions. */
    public static void torchTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= douse(level, pos, TORCH_BLOW, TORCH_RAIN) || !exposed(level, pos)) return;
        BlockState unlit;
        if (state.is(Blocks.TORCH)) unlit = Mechanical.UNLIT_TORCH.get().defaultBlockState();
        else if (state.is(Blocks.WALL_TORCH)) unlit = Mechanical.UNLIT_WALL_TORCH.get().defaultBlockState().setValue(WallTorchBlock.FACING, state.getValue(WallTorchBlock.FACING));
        else return;
        level.setBlock(pos, unlit, Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.6F);
    }

    @SubscribeEvent
    static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Payload.TYPE, Payload.CODEC);
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD || level.getGameTime() % 20 != 0) return;
        boolean windy = computeWindy(level);
        if (windy == Boolean.TRUE.equals(ACTIVE.get(level))) return;
        ACTIVE.put(level, windy);
        PacketDistributor.sendToPlayersInDimension(level, new Payload(windy));
        if (windy) {
            for (ServerPlayer player : level.players()) {
                player.sendSystemMessage(Component.translatable("message.mechanical.wind").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) PacketDistributor.sendToPlayer(sp, new Payload(isWindy(sp.level())));
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) PacketDistributor.sendToPlayer(sp, new Payload(isWindy(sp.level())));
    }

    /** /windynight: tonight is windy (from dusk; set the time to night to see it at once). */
    @SubscribeEvent
    static void registerCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("windynight")
                .requires(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                    ServerLevel level = ctx.getSource().getServer().overworld();
                    FORCED.put(level, level.getOverworldClockTime() / 24000);
                    ctx.getSource().sendSuccess(() -> Component.literal("Tonight is windy"), true);
                    return 1;
                }));
    }

    private Wind() {}
}
