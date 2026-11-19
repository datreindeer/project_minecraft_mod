package com.mechanical.wildlife.forestbirds;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.Sanity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Bone Flute: a hollow vulture wing bone with finger holes cut into it, like the oldest instruments ever found. Played
 * (use) it sounds a short tune of a few rising notes; everyone within {@link #RANGE} blocks feels less alone in the dark
 * ({@link #CHEER} sanity). A rest of {@link #COOLDOWN} ticks between tunes.
 */
@EventBusSubscriber(modid = com.mechanical.Mechanical.MODID)
public class BoneFluteItem extends Item {
    static final double RANGE = 12;
    static final float CHEER = 4.0F;
    static final int COOLDOWN = 200;
    /** The tune: note-block pitches (each a semitone step is 2^(1/12)). */
    private static final float[] TUNE = {0.84F, 1.0F, 1.12F, 0.94F, 1.26F};

    private record Note(ServerLevel level, Player player, long at, float pitch) {}
    /** Notes still to sound (server). */
    private static final List<Note> NOTES = new ArrayList<>();

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        NOTES.removeIf(n -> {
            if (n.level().getGameTime() < n.at()) return false;
            n.level().playSound(null, n.player().getX(), n.player().getY(), n.player().getZ(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 1.0F, n.pitch());
            return true;
        });
    }

    public BoneFluteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            float base = 0.9F + level.getRandom().nextFloat() * 0.2F;
            long now = server.getGameTime();
            for (int i = 0; i < TUNE.length; i++) NOTES.add(new Note(server, player, now + i * 6L, TUNE[i] * base));
            for (Player p : level.getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(RANGE))) Sanity.set(p, Sanity.of(p) + CHEER);
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        return InteractionResult.SUCCESS;
    }
}
