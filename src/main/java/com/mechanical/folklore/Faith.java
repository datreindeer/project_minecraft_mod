package com.mechanical.folklore;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.mechanical.Calendar;
import com.mechanical.Mechanical;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Faith 0..100 per player (saved, synced, kept on death). Only those who believe see the folklore beings: sight =
 * Faith + {@link #SECOND_SIGHT_BONUS} under Second Sight (Fly Agaric). Each being or glimmer has a threshold
 * ({@link #STUMP_SIGHT}, {@link #WATCHER_SIGHT}). Faith grows by gifts at a Glimmering Stump (+{@link #GIFT}, once a
 * day) and the first sight of a new kind of being (+{@link #NEW_KIND}); felling a Glimmering Stump costs {@link #FELL_STUMP}.
 * A gift left at a stump is answered the next morning: a small present appears on top (loot table gameplay/stump_gift).
 * docs/folklore-plan.md.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Faith {
    public static final float MAX = 100, SECOND_SIGHT_BONUS = 50, GIFT = 3, NEW_KIND = 5, FELL_STUMP = -25;
    public static final float STUMP_SIGHT = 10, WATCHER_SIGHT = 40;
    static final ResourceKey<LootTable> GIFT_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "gameplay/stump_gift"));

    public static final Supplier<AttachmentType<Float>> FAITH = Mechanical.ATTACHMENTS.register("faith", () -> AttachmentType
            .builder(() -> 0F)
            .serialize(Codec.FLOAT.fieldOf("value"))
            .sync(ByteBufCodecs.FLOAT)
            .copyOnDeath()
            .build());

    /** Server-only bookkeeping: kinds of beings seen, the last gift (day, and the stump while it waits), the year's Watcher Feather. */
    public record Folk(List<String> seen, long giftDay, Optional<BlockPos> giftAt, long featherYear) {
        static final Folk NONE = new Folk(List.of(), -1, Optional.empty(), -1);
        static final Codec<Folk> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.listOf().fieldOf("seen").forGetter(Folk::seen),
                Codec.LONG.fieldOf("gift_day").forGetter(Folk::giftDay),
                BlockPos.CODEC.optionalFieldOf("gift_at").forGetter(Folk::giftAt),
                Codec.LONG.fieldOf("feather_year").forGetter(Folk::featherYear)).apply(i, Folk::new));
    }

    public static final Supplier<AttachmentType<Folk>> FOLK = Mechanical.ATTACHMENTS.register("folk", () -> AttachmentType
            .builder(() -> Folk.NONE)
            .serialize(Folk.CODEC.fieldOf("folk"))
            .copyOnDeath()
            .build());

    /** The local player on the client (set by client.folklore.FolkloreClient); null on a server. */
    public static Supplier<Player> localPlayer = () -> null;

    public static float of(Player player) {
        return player.getData(FAITH);
    }

    public static void set(Player player, float value) {
        player.setData(FAITH, Math.clamp(value, 0, MAX));
    }

    public static void add(Player player, float delta) {
        set(player, of(player) + delta);
    }

    /** Pure: how much a player sees. */
    public static float sight(float faith, boolean secondSight) {
        return faith + (secondSight ? SECOND_SIGHT_BONUS : 0);
    }

    public static float sight(Player player) {
        return sight(of(player), player.hasEffect(Folklore.SECOND_SIGHT));
    }

    public static boolean canSee(Player player, float threshold) {
        return player != null && !player.isSpectator() && sight(player) >= threshold;
    }

    /** Sight of the local client player (0 on a server or before joining). */
    public static float localSight() {
        Player p = localPlayer.get();
        return p == null ? 0 : sight(p);
    }

    /** First sight of this kind of being: +{@link #NEW_KIND} Faith and the advancement. False if seen before. */
    public static boolean seen(ServerPlayer player, String kind, String advancement) {
        Folk f = player.getData(FOLK);
        if (f.seen().contains(kind)) return false;
        List<String> seen = new ArrayList<>(f.seen());
        seen.add(kind);
        player.setData(FOLK, new Folk(List.copyOf(seen), f.giftDay(), f.giftAt(), f.featherYear()));
        add(player, NEW_KIND);
        award(player, advancement);
        return true;
    }

    /** True (and marks it) if this player has not had a Watcher Feather this calendar year. */
    public static boolean takeFeather(ServerPlayer player, long year) {
        Folk f = player.getData(FOLK);
        if (f.featherYear() >= year) return false;
        player.setData(FOLK, new Folk(f.seen(), f.giftDay(), f.giftAt(), year));
        return true;
    }

    /** A gift laid at the stump at pos on calendar day {@code day}: one a day per player. False if one was given today. */
    public static boolean offer(ServerPlayer player, BlockPos pos, long day) {
        Folk f = player.getData(FOLK);
        if (f.giftDay() >= day) return false;
        player.setData(FOLK, new Folk(f.seen(), day, Optional.of(pos.immutable()), f.featherYear()));
        add(player, GIFT);
        award(player, "folklore/small_folk");
        return true;
    }

    /**
     * The answer to a waiting gift: from the morning after (not night, a later day) a present lies on the stump. Waits
     * while the stump's chunk is not loaded; a felled stump answers nothing. Returns the present, or null.
     */
    public static ItemEntity answerGift(ServerPlayer player, ServerLevel level, long day, boolean night) {
        Folk f = player.getData(FOLK);
        if (f.giftAt().isEmpty() || day <= f.giftDay() || night) return null;
        BlockPos pos = f.giftAt().get();
        if (!level.isLoaded(pos)) return null;
        player.setData(FOLK, new Folk(f.seen(), f.giftDay(), Optional.empty(), f.featherYear()));
        if (!level.getBlockState(pos).is(Folklore.GLIMMERING_STUMP.get())) return null;
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.THIS_ENTITY, player).create(LootContextParamSets.GIFT);
        ItemEntity present = null;
        for (ItemStack stack : level.getServer().reloadableRegistries().getLootTable(GIFT_LOOT).getRandomItems(params)) {
            present = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack, 0, 0, 0);
            present.setUnlimitedLifetime(); // lies there until it is found
            level.addFreshEntity(present);
        }
        return present;
    }

    static void award(ServerPlayer player, String path) {
        var holder = player.level().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(Mechanical.MODID, path));
        if (holder != null) player.getAdvancements().award(holder, "done");
    }

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 100 != 0) return;
        ServerLevel level = player.level();
        answerGift(player, level, Calendar.day(level.getOverworldClockTime()), level.isDarkOutside());
    }

    /** /faith [value]: shows or sets your Faith (op, for testing). */
    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("faith").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ctx.getSource().sendSuccess(() -> Component.literal("Faith: " + Math.round(of(player)) + ", sight: " + Math.round(sight(player))), false);
                    return Math.round(of(player));
                })
                .then(Commands.argument("value", FloatArgumentType.floatArg(0, MAX)).executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    set(player, FloatArgumentType.getFloat(ctx, "value"));
                    ctx.getSource().sendSuccess(() -> Component.literal("Faith set to " + Math.round(of(player))).withStyle(ChatFormatting.GRAY), true);
                    return 1;
                })));
    }

    static void selfCheck() {
        check(sight(0, false) == 0 && sight(0, true) == SECOND_SIGHT_BONUS, "second sight adds 50");
        check(sight(0, true) >= WATCHER_SIGHT && sight(0, false) < STUMP_SIGHT, "the mushroom alone opens the eyes, nothing without it");
        check(sight(4 * GIFT, false) >= STUMP_SIGHT && sight(4 * GIFT, false) < WATCHER_SIGHT, "four gifts show the glimmer, not yet the watcher");
        check(sight(WATCHER_SIGHT, false) >= WATCHER_SIGHT && -FELL_STUMP > GIFT * 7, "faith alone can see, felling undoes a week of gifts");
    }

    private static void check(boolean ok, String what) {
        if (!ok) throw new IllegalStateException("Faith self-check failed: " + what);
    }

    private Faith() {}
}
