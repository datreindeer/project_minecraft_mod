package com.mechanical.people;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.mechanical.AncientOak;
import com.mechanical.Coins;
import com.mechanical.Foraging;
import com.mechanical.Mechanical;
import com.mechanical.wildlife.Wildlife;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The Oak Folk's quest line (F4): {@link #QUESTS}, handed out one at a time by the keeper (right-click, screen
 * client.people.OakQuestScreen). Each asks for items, brought in any number of hand-ins; the progress belongs to the
 * player's tribe (clan.Clans#sharerOf: shared by all members) or to the player outside one, saved here. Each finished
 * quest gives its reward to whoever handed in the last of it, and raises the standing with the Oak Folk. Falling to a
 * hostile standing ({@link #close}, from Tribe#addReputation) closes the line for good. Rewards: docs/ages (F4 table).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class OakQuests extends SavedData {
    /** One quest: bring {@code count} of {@code item}; get {@code rewards}. */
    public record Quest(String id, Supplier<? extends Item> item, int count, List<Supplier<ItemStack>> rewards) {}

    public static final int STANDING_PER_QUEST = 4;

    private static Supplier<ItemStack> give(Supplier<? extends Item> item, int n) {
        return () -> new ItemStack(item.get(), n);
    }

    /** The line, easy to hard: Stone Age gathering, hides and antlers, then bronze, a bear, iron, the Giant Antler and steel. */
    public static final List<Quest> QUESTS = List.of(
            new Quest("berries", () -> Foraging.BERRIES.get("raspberry").get(), 16, List.of(give(() -> AncientOak.GLOWCAP.get().asItem(), 4))),
            new Quest("mushrooms", () -> Foraging.MUSHROOM_ITEMS.get("chanterelle").get(), 8, List.of(give(() -> AncientOak.SPIRIT_BLOOM.get().asItem(), 2))),
            new Quest("fibre", Mechanical.PLANT_FIBRE, 32, List.of(give(Mechanical.BARK_BASKET, 1))),
            new Quest("bark", () -> Mechanical.BARKS.get("oak").get(), 16, List.of(give(() -> AncientOak.GLOW_MOSS.get().asItem(), 8))),
            new Quest("hides", Mechanical.RAW_HIDE, 6, List.of(give(Mechanical.TUSK_DAGGER, 1))),
            new Quest("sinew", Wildlife.SINEW, 8, List.of(give(Coins.SILVER_COIN, 2))),
            new Quest("antlers", Wildlife.ANTLER, 4, List.of(give(OakFolk.VERDANT_STAFF, 1))),
            new Quest("bronze", com.mechanical.Bronze.BRONZE_INGOT, 6, List.of(give(Coins.SILVER_COIN, 4), give(() -> AncientOak.SPIRIT_BLOOM.get().asItem(), 4))),
            new Quest("bear", Wildlife.RAW_BEAR_PELT, 1, List.of(give(AncientOak.MISTLETOE_CHARM, 1))),
            new Quest("iron", com.mechanical.Ironworking.WROUGHT_IRON_INGOT, 8, List.of(give(Coins.GOLD_COIN, 1))),
            new Quest("giant_antler", Wildlife.GIANT_ANTLER, 1, List.of(give(Coins.GOLD_COIN, 3))),
            new Quest("steel", com.mechanical.Smithing.STEEL_INGOT, 4, List.of(give(OakFolk.ANCIENT_RITUAL_KNIFE, 1))));

    /** A tribe's (or lone player's) place in the line: quests done, items brought for the current one, closed for good. */
    public record Progress(int stage, int delivered, boolean closed) {
        static final Codec<Progress> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("stage").forGetter(Progress::stage), Codec.INT.fieldOf("delivered").forGetter(Progress::delivered),
                Codec.BOOL.fieldOf("closed").forGetter(Progress::closed)).apply(i, Progress::new));
        static final Progress START = new Progress(0, 0, false);

        public boolean finished() {
            return stage >= QUESTS.size();
        }
    }

    private static final Codec<OakQuests> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Progress.CODEC).xmap(OakQuests::new, q -> q.progress);
    private static final SavedDataType<OakQuests> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "oak_quests"),
            () -> new OakQuests(Map.of()), CODEC);

    private final Map<UUID, Progress> progress;

    private OakQuests(Map<UUID, Progress> progress) {
        this.progress = new HashMap<>(progress);
    }

    static OakQuests get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** The progress {@code player} shares with their tribe. */
    public static Progress of(Player player) {
        if (!(player.level() instanceof ServerLevel level)) return Progress.START;
        return get(level.getServer()).progress.getOrDefault(com.mechanical.clan.Clans.sharerOf(player), Progress.START);
    }

    /** For game tests: put the player's tribe at this point of the line (null: forget it). */
    static void set(Player player, Progress p) {
        if (p == null) {
            get(((ServerLevel) player.level()).getServer()).progress.remove(com.mechanical.clan.Clans.sharerOf(player));
            return;
        }
        OakQuests data = get(((ServerLevel) player.level()).getServer());
        data.progress.put(com.mechanical.clan.Clans.sharerOf(player), p);
        data.setDirty();
    }

    /** A hostile standing with the Oak Folk: their quest line is closed to this player's tribe for good. */
    static void close(Player player) {
        if (!(player.level() instanceof ServerLevel)) return;
        Progress p = of(player);
        if (!p.closed()) set(player, new Progress(p.stage(), p.delivered(), true));
    }

    /** Closed for good, or hostile right now. */
    public static boolean closed(Player player) {
        return of(player).closed() || Tribe.of(Tribe.OAK).hostileTo(player);
    }

    /**
     * Brings what the player carries of the current quest's item (up to what is still missing). Finishing it gives the
     * rewards to this player and moves the line on. Returns how many items were taken.
     */
    public static int handIn(Player player) {
        Progress p = of(player);
        if (closed(player) || p.finished()) return 0;
        Quest q = QUESTS.get(p.stage());
        int taken = take(player.getInventory(), q.item().get(), q.count() - p.delivered());
        if (taken == 0) return 0;
        int delivered = p.delivered() + taken;
        if (delivered < q.count()) {
            set(player, new Progress(p.stage(), delivered, false));
            return taken;
        }
        set(player, new Progress(p.stage() + 1, 0, false));
        for (Supplier<ItemStack> r : q.rewards()) player.getInventory().placeItemBackInInventory(r.get(), net.minecraft.util.Prediction.SERVER_ONLY);
        Tribe.of(Tribe.OAK).addReputation(player, STANDING_PER_QUEST);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.sendSystemMessage(Component.translatable("message.mechanical.oak_quest.done", Component.translatable("quest.mechanical.oak." + q.id())));
        if (player instanceof ServerPlayer sp) TribeVisits.award(sp, "oak_quest");
        return taken;
    }

    private static int take(Inventory inv, Item item, int max) {
        int taken = 0;
        for (int i = 0; i < inv.getContainerSize() && taken < max; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(item)) continue;
            int n = Math.min(s.getCount(), max - taken);
            s.shrink(n);
            taken += n;
        }
        if (taken > 0) inv.setChanged();
        return taken;
    }

    // ---- the screen ----

    /** Server -> client: open (or refresh) the quest screen for the keeper {@code entity}. */
    public record Open(int entity, Component name, int stage, int delivered, boolean closed, int standing) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "oak_quests"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::entity, net.minecraft.network.chat.ComponentSerialization.TRUSTED_STREAM_CODEC, Open::name,
                ByteBufCodecs.VAR_INT, Open::stage, ByteBufCodecs.VAR_INT, Open::delivered, ByteBufCodecs.BOOL, Open::closed,
                ByteBufCodecs.VAR_INT, Open::standing, Open::new);

        @Override
        public Type<Open> type() {
            return TYPE;
        }
    }

    /** Client -> server: hand in to the keeper {@code entity}. */
    public record HandIn(int entity) implements CustomPacketPayload {
        public static final Type<HandIn> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "oak_hand_in"));
        public static final StreamCodec<RegistryFriendlyByteBuf, HandIn> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, HandIn::entity, HandIn::new);

        @Override
        public Type<HandIn> type() {
            return TYPE;
        }
    }

    @SubscribeEvent
    static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(Open.TYPE, Open.CODEC).playToServer(HandIn.TYPE, HandIn.CODEC, OakQuests::onHandIn);
    }

    /** The keeper was right-clicked: the screen, or (hostile) a curt word. */
    static void open(ServerPlayer player, Tribesperson keeper) {
        TribeVisits.award(player, "oak_folk");
        if (closed(player)) close(player);
        Progress p = of(player);
        PacketDistributor.sendToPlayer(player, new Open(keeper.getId(), keeper.getDisplayName(), p.stage(), p.delivered(), p.closed(),
                Tribe.of(Tribe.OAK).reputation(player)));
    }

    private static void onHandIn(HandIn msg, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) return;
        if (!(player.level().getEntity(msg.entity()) instanceof Tribesperson keeper) || !keeper.isAlive() || keeper.role() != TribeRole.OAK_KEEPER
                || keeper.distanceToSqr(player) > TribeDialog.REACH * TribeDialog.REACH) return;
        handIn(player);
        open(player, keeper);
    }
}
