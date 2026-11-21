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
 * A Copper, Silver or Gold Ingot is struck cold: {@link Coins#MINT_HITS} blows mint {@link Coins#COINS_PER_INGOT} coins of its metal.
 * Three Bronze Ingots, also cold, become a Bronze Fire Grate ({@link FirePit#FIRE_GRATE}). Hot iron and steel are forged
 * into parts ({@link Smithing.Pattern}); bigger parts take more hot ingots stacked on the first.
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
        if (Smithing.forgeable(item)) {
            forge(level, player, hammer, hand, x, y, z);
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
        struck();
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
        struck();
        if (++hits < Coins.MINT_HITS) {
            sync();
            return;
        }
        ItemStack coins = Coins.minted(item);
        item = ItemStack.EMPTY;
        hits = 0;
        Block.popResource(level, worldPosition.above(), coins);
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
        struck();
        if (++hits < FirePit.GRATE_HITS) {
            sync();
            return;
        }
        item = ItemStack.EMPTY;
        hits = 0;
        level.playSound(null, worldPosition, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6F, 1.2F);
        Block.popResource(level, worldPosition.above(), new ItemStack(FirePit.FIRE_GRATE.get()));
        sync();
    }

    // ---------------------------------------------------------------- forging iron and steel (Smithing)

    /** What hot iron or steel on this anvil is forged into. */
    private Smithing.Pattern pattern = Smithing.Pattern.PLATE;

    public Smithing.Pattern pattern() {
        return pattern;
    }

    public void setPattern(Smithing.Pattern pattern) {
        this.pattern = pattern;
        setChanged();
    }

    /** Sneak-use with a hammer: the next pattern. */
    void nextPattern(Player player) {
        setPattern(pattern.next());
        tellPattern(player);
    }

    void tellPattern(Player player) {
        if (item.is(Smithing.BLISTER_STEEL.get()) || item.is(Smithing.HOT_BLISTER_STEEL.get())) {
            player.sendOverlayMessage(Component.translatable("message.mechanical.forge.blister", Smithing.BLISTER_HITS));
            return;
        }
        player.sendOverlayMessage(Component.translatable("message.mechanical.forge.pattern",
                Component.translatable("pattern.mechanical." + pattern.id), pattern.ingots, pattern.hits, 1 + extra));
    }

    /** Hot ingots stacked on the first one (it holds the heat and the blows), to be welded into a bigger part. */
    private int extra;

    /** Another hot ingot of the same metal onto the pile; false when it holds {@link Smithing#MAX_PILE}. */
    boolean addIngot(ServerLevel level, Player player) {
        if (1 + extra >= Smithing.MAX_PILE) {
            if (player != null) player.sendOverlayMessage(Component.translatable("message.mechanical.forge.pile_full", Smithing.MAX_PILE));
            return false;
        }
        extra++;
        level.playSound(null, worldPosition, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.6F, 1.0F);
        if (player != null) tellPattern(player);
        sync();
        return true;
    }

    /** One piece at a time: a stacked ingot first. */
    @Override
    public ItemStack take() {
        if (extra == 0) return super.take();
        extra--;
        ItemStack one = item.copyWithCount(1);
        one.remove(Ironworking.BLOOM_HITS.get()); // the blows were on the pile
        sync();
        return one;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) for (int i = 0; i < extra; i++) net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), item.copyWithCount(1));
    }

    @Override
    public int pile() {
        if (item.isEmpty()) return 0;
        if (item.is(Bronze.BRONZE_INGOT.get())) return item.getCount();
        return 1 + extra;
    }

    @Override
    public float progress() {
        if (item.isEmpty()) return 0;
        if (Coins.mintable(item)) return hits / (float) Coins.MINT_HITS;
        if (item.is(Bronze.BRONZE_INGOT.get())) return hits / (float) FirePit.GRATE_HITS;
        boolean blister = item.is(Smithing.BLISTER_STEEL.get()) || item.is(Smithing.HOT_BLISTER_STEEL.get());
        if (Smithing.forgeable(item)) return Ironworking.hits(item) / (float) (blister ? Smithing.BLISTER_HITS : pattern.hits);
        return Ironworking.hits(item) / (float) Ironworking.HITS;
    }

    /** One blow on hot iron or steel: after enough the part (or, from Blister Steel, a Steel Ingot) pops out. */
    private void forge(ServerLevel level, Player player, ItemStack hammer, InteractionHand hand, double x, double y, double z) {
        if (!Ironworking.isHot(item, level.getGameTime())) {
            cool(level);
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4F, 1.6F);
            player.sendOverlayMessage(Component.translatable("message.mechanical.forge.cold", BloomeryBlockEntity.FORGE_TEMP));
            return;
        }
        if (Smithing.needsIronHammer(item) && !Smithing.worksSteel(hammer)) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4F, 1.6F);
            player.sendOverlayMessage(Component.translatable("message.mechanical.forge.steel_hammer"));
            return;
        }
        boolean blister = item.is(Smithing.HOT_BLISTER_STEEL.get());
        ItemStack result = blister ? new ItemStack(Smithing.STEEL_INGOT.get()) : pattern.result(item);
        if (result.isEmpty()) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4F, 1.6F);
            player.sendOverlayMessage(Component.translatable("message.mechanical.forge.no_part",
                    Component.translatable("pattern.mechanical." + pattern.id)));
            return;
        }
        int ingots = blister ? 1 : pattern.ingots;
        if (1 + extra < ingots) {
            level.playSound(null, worldPosition, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4F, 1.6F);
            player.sendOverlayMessage(Component.translatable("message.mechanical.forge.ingots", ingots, 1 + extra));
            return;
        }
        int needed = blister ? Smithing.BLISTER_HITS : pattern.hits;
        level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.7F, 0.9F + level.getRandom().nextFloat() * 0.3F);
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 8, 0.15, 0.05, 0.15, 0.3);
        level.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 2, 0.1, 0.02, 0.1, 0.01);
        hammer.hurtAndBreak(Smithing.wear(hammer), player, hand);
        struck();
        int done = Ironworking.hits(item) + 1;
        if (done < needed) {
            item.set(Ironworking.BLOOM_HITS.get(), done);
            sync();
            return;
        }
        int left = 1 + extra - ingots; // ingots the part didn't take stay on the anvil, hot, unworked
        if (left > 0) {
            item.remove(Ironworking.BLOOM_HITS.get());
            extra = left - 1;
        } else {
            item = ItemStack.EMPTY;
            extra = 0;
        }
        level.playSound(null, worldPosition, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.6F, 1.3F);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 3, 0.1, 0.05, 0.1, 0.01);
        Block.popResource(level, worldPosition.above(), result);
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) Kettle.award(sp, Smithing.FORGE_ADVANCEMENT);
        sync();
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        if (pattern != Smithing.Pattern.PLATE) output.putString("pattern", pattern.id);
        if (extra > 0) output.putInt("extra", extra);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        String id = input.getStringOr("pattern", Smithing.Pattern.PLATE.id);
        pattern = java.util.Arrays.stream(Smithing.Pattern.values()).filter(p -> p.id.equals(id)).findFirst().orElse(Smithing.Pattern.PLATE);
        extra = input.getIntOr("extra", 0);
    }

    /** A hot bloom, ingot or steel whose time is up turns into the cold one (with its blows). */
    void cool(ServerLevel level) {
        if (!Ironworking.hotItem(item) || Ironworking.isHot(item, level.getGameTime())) return;
        item = item.getItem() instanceof HotMetalItem ? HotMetalItem.cooled(item) : IronBloomItem.cooled(item);
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.4F);
        sync();
    }
}
