package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Pan for washing gravel and sand. Empty: right-click gravel or sand to scoop the block up. Filled (gravel / sand):
 * hold use while standing in or aiming at water for 3 s, then the loot table gameplay/panning/&lt;fill&gt; decides what is
 * left in the pan (often nothing). Each wash costs one use; the wear carries across the three items (transmuteCopy).
 */
public class PanItem extends Item {
    public static final int USES = 64;
    private static final int WASH_TICKS = 60;
    private final String fill; // null = empty pan

    public PanItem(Properties properties, String fill) {
        super(properties);
        this.fill = fill;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (fill != null) return InteractionResult.PASS; // falls through to use(): washing
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Item filled = state.is(Blocks.GRAVEL) ? BronzeOres.PAN_WITH_GRAVEL.get()
                : state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) ? BronzeOres.PAN_WITH_SAND.get() : null;
        Player player = context.getPlayer();
        if (filled == null || player == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.levelEvent(2001, pos, Block.getId(state)); // break particles and sound
            level.removeBlock(pos, false);
            player.setItemInHand(context.getHand(), context.getItemInHand().transmuteCopy(filled, 1));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (fill == null) return InteractionResult.PASS;
        if (!nearWater(level, player)) {
            if (level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.mechanical.pan.need_water"));
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    private static boolean nearWater(Level level, Player player) {
        if (player.isInWater()) return true;
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        return hit.getType() == HitResult.Type.BLOCK && level.getFluidState(hit.getBlockPos()).is(FluidTags.WATER);
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BRUSH;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return fill == null ? 0 : WASH_TICKS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (!(entity instanceof Player player) || !nearWater(level, player)) {
            entity.releaseUsingItem();
            return;
        }
        Vec3 at = swirl(entity);
        if (ticksRemaining % 4 == 0)
            for (int i = 0; i < 3; i++)
                level.addParticle(ParticleTypes.SPLASH, at.x + (level.getRandom().nextDouble() - 0.5) * 0.4, at.y,
                        at.z + (level.getRandom().nextDouble() - 0.5) * 0.4, 0, 0.1, 0);
        if (ticksRemaining % 10 == 0)
            level.playSound(player, at.x, at.y, at.z, fill.equals("gravel") ? SoundEvents.BRUSH_GRAVEL : SoundEvents.BRUSH_SAND, SoundSource.PLAYERS, 0.6F, 0.8F);
        if (ticksRemaining % 20 == 5)
            level.playSound(player, at.x, at.y, at.z, SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.15F, 1.4F);
    }

    /** Where the pan is: a little in front of the player, about waist high. */
    private static Vec3 swirl(LivingEntity entity) {
        Vec3 look = entity.getLookAngle();
        return entity.position().add(look.x * 0.7, 0.6, look.z * 0.7);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (fill == null || !(entity instanceof Player player) || !(level instanceof ServerLevel server)) return stack;
        LootTable table = server.getServer().reloadableRegistries().getLootTable(
                ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Mechanical.MODID, "gameplay/panning/" + fill)));
        LootParams params = new LootParams.Builder(server)
                .withParameter(LootContextParams.ORIGIN, player.position())
                .withLuck(player.getLuck())
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .withParameter(LootContextParams.TOOL, stack)
                .create(LootContextParamSets.ARCHAEOLOGY);
        Vec3 at = swirl(player);
        server.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 15, 0.3, 0.1, 0.3, 0.1);
        server.playSound(null, at.x, at.y, at.z, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.7F, 1.2F);
        var loot = table.getRandomItems(params);
        if (!loot.isEmpty()) server.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F, 1.5F);
        for (ItemStack found : loot) player.getInventory().placeItemBackInInventory(found, net.minecraft.util.Prediction.SERVER_ONLY);

        ItemStack empty = stack.transmuteCopy(BronzeOres.PAN.get(), 1);
        if (player.hasInfiniteMaterials()) return empty;
        empty.setDamageValue(stack.getDamageValue() + 1);
        if (empty.getDamageValue() >= empty.getMaxDamage()) {
            server.playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
            return ItemStack.EMPTY;
        }
        return empty;
    }
}
