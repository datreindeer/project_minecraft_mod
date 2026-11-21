package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The bloom lying on a Bronze Anvil. A hot one takes {@link Ironworking#HITS} blows of the Bronze Smithing Hammer
 * (counted on the bloom, so a cooled one keeps them), then it is wrought iron and slag; a cold one takes none.
 * A Copper Ingot is struck cold: {@link Coins#MINT_HITS} blows mint {@link Coins#COINS_PER_INGOT} Copper Coins.
 * Three Bronze Ingots, also cold, become a Bronze Fire Grate ({@link FirePit#FIRE_GRATE}).
 */
public class BronzeAnvilBlockEntity extends StoneAnvilBlockEntity {
    public BronzeAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(Ironworking.BRONZE_ANVIL_ENTITY.get(), pos, state);
    }

    @Override
    public float top() {
        return 13 / 16.0F;
    }

    /** One blow of the smithing hammer on the bloom (the block keeps the hammer's cooldown). */
    @Override
    public void strike(ServerLevel level, Player player, ItemStack hammer, InteractionHand hand) {
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + top() + 0.05, z = worldPosition.getZ() + 0.5;
        if (Coins.mintable(item)) {
            mint(level, player, hammer, hand, x, y, z);
            return;
        }
        if (item.is(Bronze.BRONZE_INGOT.get())) {
            smithGrate(level, player, hammer, hand, x, y, z);
            return;
        }
        if (!Ironworking.isHot(item, level.getGameTime())) {
            cool(level);
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4F, 1.6F);
            player.sendOverlayMessage(Component.translatable("message.mechanical.bloom.cold", BloomeryBlockEntity.FORGE_TEMP));
            return;
        }
        level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 0.8F + level.getRandom().nextFloat() * 0.3F);
        level.sendParticles(ParticleTypes.LAVA, x, y, z, 2, 0.1, 0.02, 0.1, 0.0);
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 8, 0.15, 0.05, 0.15, 0.3);
        hammer.hurtAndBreak(Ironworking.HAMMER_WEAR, player, hand);
        int done = Ironworking.hits(item) + 1;
        if (done < Ironworking.HITS) {
            item.set(Ironworking.BLOOM_HITS.get(), done);
            sync();
            return;
        }
        int ingots = Ironworking.bloomYield(item);
        item = ItemStack.EMPTY;
        level.playSound(null, worldPosition, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 4, 0.1, 0.05, 0.1, 0.01);
        Block.popResource(level, worldPosition.above(), new ItemStack(Ironworking.WROUGHT_IRON_INGOT.get(), ingots));
        if (ingots < Ironworking.BLOOM_PARTS) Block.popResource(level, worldPosition.above(), new ItemStack(Ironworking.SLAG.get(), Ironworking.BLOOM_PARTS - ingots));
        sync();
    }

    private void mint(ServerLevel level, Player player, ItemStack hammer, InteractionHand hand, double x, double y, double z) {
        level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.4F + level.getRandom().nextFloat() * 0.2F);
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 6, 0.15, 0.05, 0.15, 0.3);
        hammer.hurtAndBreak(1, player, hand);
        if (++hits < Coins.MINT_HITS) {
            setChanged();
            return;
        }
        item = ItemStack.EMPTY;
        hits = 0;
        Block.popResource(level, worldPosition.above(), new ItemStack(Coins.COPPER_COIN.get(), Coins.COINS_PER_INGOT));
        sync();
    }

    /** Another Bronze Ingot onto the pile (any quality: the grate keeps none). */
    void add(ServerLevel level, ItemStack one) {
        item.grow(one.getCount());
        hits = 0;
        level.playSound(null, worldPosition, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
        sync();
    }

    /** Bronze Ingots struck cold: {@link FirePit#GRATE_HITS} blows on {@link FirePit#GRATE_INGOTS} of them make a Bronze Fire Grate. */
    private void smithGrate(ServerLevel level, Player player, ItemStack hammer, InteractionHand hand, double x, double y, double z) {
        if (item.getCount() < FirePit.GRATE_INGOTS) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4F, 1.6F);
            player.sendOverlayMessage(Component.translatable("message.mechanical.fire_grate.ingots", item.getCount(), FirePit.GRATE_INGOTS));
            return;
        }
        level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.1F + level.getRandom().nextFloat() * 0.2F);
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 6, 0.15, 0.05, 0.15, 0.3);
        hammer.hurtAndBreak(1, player, hand);
        if (++hits < FirePit.GRATE_HITS) {
            setChanged();
            return;
        }
        item = ItemStack.EMPTY;
        hits = 0;
        level.playSound(null, worldPosition, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
        Block.popResource(level, worldPosition.above(), new ItemStack(FirePit.FIRE_GRATE.get()));
        sync();
    }

    /** A hot bloom whose time is up turns into a cold one (with its blows). */
    void cool(ServerLevel level) {
        if (!item.is(Ironworking.HOT_IRON_BLOOM.get()) || Ironworking.isHot(item, level.getGameTime())) return;
        item = IronBloomItem.cooled(item);
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.4F);
        sync();
    }
}
