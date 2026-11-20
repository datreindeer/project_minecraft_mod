package com.mechanical;

import java.util.Set;
import java.util.UUID;

import com.mechanical.people.Tribe;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.Codec;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * The ages (docs/seasons-and-tribes.md) and the world's age: the latest age any player tribe, or any player on their own,
 * has reached. It is kept as a high-water mark, so it never drops. An age is reached with its marker advancement
 * (Bronze Age: made Bronze; Iron Age: made Wrought Iron). NPC tribes follow the world 0 or 1 age behind
 * ({@link #npcAge}); monsters may keep gear of ages the world has reached (LaterAgeGear). /worldage for ops.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class WorldAge extends SavedData {
    public static final int STONE = 0, BRONZE = 1, IRON = 2;
    public static final String[] AGES = {"stone", "bronze", "iron"};
    /** The advancement that starts each age (none for the Stone Age). */
    public static final Identifier[] MARKERS = {null, id("bronze_age/bronze"), id("bronze_age/wrought_iron")};

    private static final Codec<WorldAge> CODEC = Codec.INT.xmap(WorldAge::new, w -> w.age);
    private static final SavedDataType<WorldAge> TYPE = new SavedDataType<>(id("world_age"), () -> new WorldAge(STONE), CODEC);

    private int age;

    private WorldAge(int age) {
        this.age = age;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Mechanical.MODID, path);
    }

    public static WorldAge get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static int of(MinecraftServer server) {
        return get(server).age;
    }

    /** The age of a set of finished advancements (a tribe's). */
    public static int of(Set<Identifier> done) {
        for (int a = AGES.length - 1; a > 0; a--) if (done.contains(MARKERS[a])) return a;
        return STONE;
    }

    /** The age one player has reached on their own. */
    public static int of(ServerPlayer player) {
        var manager = player.level().getServer().getAdvancements();
        for (int a = AGES.length - 1; a > 0; a--) {
            var holder = manager.get(MARKERS[a]);
            if (holder != null && player.getAdvancements().getOrStartProgress(holder).isDone()) return a;
        }
        return STONE;
    }

    /** An NPC tribe's age: the world's, less the tribe's own lag (0 or 1, fixed by its id). */
    public static int npcAge(MinecraftServer server, UUID tribe) {
        return Math.max(STONE, of(server) - (tribe == null ? 1 : Tribe.of(tribe).ageLag()));
    }

    /** Raise the world's age if {@code reached} is later (never lowers it), and tell everyone. */
    public static void reach(MinecraftServer server, int reached) {
        WorldAge w = get(server);
        if (reached <= w.age) return;
        w.age = reached;
        w.setDirty();
        server.getPlayerList().broadcastSystemMessage(Component.translatable("message.mechanical.world_age.entered",
                Component.translatable("gui.mechanical.clan.age." + AGES[reached])).withStyle(ChatFormatting.GOLD), false);
    }

    /** Set the age outright, also back (ops and tests). */
    public static void set(MinecraftServer server, int age) {
        WorldAge w = get(server);
        w.age = age;
        w.setDirty();
    }

    @SubscribeEvent
    static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (int a = 1; a < AGES.length; a++) if (MARKERS[a].equals(event.getAdvancement().id())) reach(player.level().getServer(), a);
    }

    /** Players who reached an age before the world kept count (or on their own while it wasn't looking) count when they log in. */
    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) reach(player.level().getServer(), of(player));
    }

    @SubscribeEvent
    static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("worldage").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx -> {
                    int a = of(ctx.getSource().getServer());
                    ctx.getSource().sendSuccess(() -> Component.translatable("message.mechanical.world_age.is",
                            Component.translatable("gui.mechanical.clan.age." + AGES[a])), false);
                    return a;
                })
                .then(Commands.literal("set").then(Commands.argument("age", IntegerArgumentType.integer(STONE, AGES.length - 1)).executes(ctx -> {
                    int a = IntegerArgumentType.getInteger(ctx, "age");
                    set(ctx.getSource().getServer(), a);
                    ctx.getSource().sendSuccess(() -> Component.translatable("message.mechanical.world_age.is",
                            Component.translatable("gui.mechanical.clan.age." + AGES[a])), true);
                    return a;
                }))));
    }
}
