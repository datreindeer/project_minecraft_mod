package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Smithing Hearth (Smithing): a small fire-brick forge set beside the anvil. Right-click it with Charcoal to stock it
 * ({@link SmithingHearthBlockEntity#MAX_FUEL}), light it with a Fire Drill or Flint and Steel, lay up to four blooms or
 * ingots of iron or steel in the coals: cold ones come to forging heat, hot ones stay hot as long as it burns. An empty
 * hand takes the last piece laid in (hot: hold Bronze Tongs).
 */
public class SmithingHearthBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    static final net.minecraft.resources.Identifier ADVANCEMENT = net.minecraft.resources.Identifier.fromNamespaceAndPath(Mechanical.MODID, "iron_age/smithing_hearth");

    public SmithingHearthBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmithingHearthBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != Smithing.SMITHING_HEARTH_ENTITY.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<SmithingHearthBlockEntity>) SmithingHearthBlockEntity::serverTick;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SmithingHearthBlockEntity hearth)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (stack.getItem() instanceof FireDrillItem) return InteractionResult.PASS; // the drill lights it (FireDrillItem)
        if (!(level instanceof ServerLevel server)) return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.SUCCESS;
        if (stack.is(Items.CHARCOAL)) {
            if (!hearth.addFuel()) player.sendOverlayMessage(Component.translatable("message.mechanical.hearth.full", SmithingHearthBlockEntity.MAX_FUEL));
            else {
                stack.consume(1, player);
                server.playSound(null, pos, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 0.6F, 1.4F);
                player.sendOverlayMessage(Component.translatable("message.mechanical.hearth.fuel", hearth.fuel(), SmithingHearthBlockEntity.MAX_FUEL));
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(Items.FLINT_AND_STEEL)) {
            if (lightUp(server, pos, hearth, player)) stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            return InteractionResult.SUCCESS;
        }
        if (Smithing.hotOf(stack) != null || Ironworking.hotItem(stack)) {
            if (!hearth.put(stack.copyWithCount(1))) player.sendOverlayMessage(Component.translatable("message.mechanical.hearth.no_room"));
            else {
                stack.consume(1, player);
                server.playSound(null, pos, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.6F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return stack.isEmpty() ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    /** Lights it (with a message when there is no charcoal); true if it is burning now. */
    static boolean lightUp(ServerLevel level, BlockPos pos, SmithingHearthBlockEntity hearth, Player player) {
        if (hearth.light(level)) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) Kettle.award(sp, ADVANCEMENT);
            return true;
        }
        if (!hearth.lit() && player != null) player.sendOverlayMessage(Component.translatable("message.mechanical.hearth.no_fuel"));
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SmithingHearthBlockEntity hearth)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            ItemStack out = hearth.take();
            if (!out.isEmpty()) player.getInventory().placeItemBackInInventory(out, Prediction.SERVER_ONLY);
            else player.sendOverlayMessage(Component.translatable("message.mechanical.hearth.state", hearth.fuel(), SmithingHearthBlockEntity.MAX_FUEL));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        double x = pos.getX() + 0.2 + random.nextDouble() * 0.6, z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, x, pos.getY() + 1.02, z, 0, 0.01, 0);
        if (random.nextInt(10) == 0) level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + 1.05, z, 0, 0.03, 0);
        if (random.nextInt(20) == 0) level.playLocalSound(pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6F, 1.0F, false);
    }
}
