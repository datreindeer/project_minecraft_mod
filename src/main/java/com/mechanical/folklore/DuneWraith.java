package com.mechanical.folklore;

import com.mechanical.WaterJugBlock;
import com.mechanical.WaterJugItem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

/**
 * Dune Wraith: in a desert sandstorm (thunder over the desert) a shape of sand drifts up to a player and, at arm's
 * length, steals water: a Water Bottle turns empty, a Water Jug runs dry. Then it is gone. The swirling sand around it
 * everyone sees; the shape itself only sight {@link Faith#WRAITH_SIGHT}, and only while the storm lasts. A blow scatters
 * it and leaves a piece of Dune Glass. When the storm ends, so does the wraith.
 */
public class DuneWraith extends Spirit {
    static final double TOUCH = 1.6, HUNT = 24;

    public DuneWraith(EntityType<? extends DuneWraith> type, Level level) {
        super(type, level);
    }

    @Override
    public float sight() {
        return Faith.WRAITH_SIGHT;
    }

    @Override
    String kind() {
        return "dune_wraith";
    }

    /** On sand under open sky in a thunderstorm (the desert's sandstorm); rare. */
    static boolean canSpawn(LevelAccessor level, BlockPos pos, RandomSource random) {
        return random.nextInt(4) == 0 && level instanceof ServerLevelAccessor s
                && rule(s.getLevel().isThundering(), level.getBlockState(pos.below()).is(BlockTags.SAND), level.canSeeSky(pos));
    }

    static boolean rule(boolean storm, boolean sand, boolean sky) {
        return storm && sand && sky;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) return;
        if (tickCount % 4 == 0) level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()),
                getX(), getY() + 1, getZ(), 6, 0.5, 0.9, 0.5, 0.02);
        if (!level.isThundering()) {
            vanish();
            return;
        }
        Player p = level.getNearestPlayer(this, HUNT);
        if (p == null || p.isCreative() || p.isSpectator()) return;
        if (distanceTo(p) > TOUCH) {
            if (tickCount % 10 == 0) getNavigation().moveTo(p, 1.1);
        } else if (p instanceof ServerPlayer sp) {
            boolean took = steal(sp);
            sp.sendOverlayMessage(Component.translatable(took ? "message.mechanical.dune_wraith.stole" : "message.mechanical.dune_wraith.touch")
                    .withStyle(ChatFormatting.YELLOW));
            level.playSound(null, blockPosition(), SoundEvents.SAND_BREAK, SoundSource.HOSTILE, 1.0F, 0.6F);
            vanish();
        }
    }

    /** Takes the first water it finds: a Water Bottle empties, a Water Jug runs dry. True if it took any. */
    static boolean steal(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.POTION) && s.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER)) {
                inv.setItem(i, new ItemStack(Items.GLASS_BOTTLE, s.getCount()));
                return true;
            }
            if (s.getItem() instanceof WaterJugItem && WaterJugItem.water(s) > 0) {
                s.set(DataComponents.BLOCK_STATE, s.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).with(WaterJugBlock.WATER, 0));
                return true;
            }
        }
        return false;
    }

    @Override
    void struck(ServerLevel level, ServerPlayer player) {
        spawnAtLocation(level, Beings.DUNE_GLASS.toStack());
        vanish();
    }
}
