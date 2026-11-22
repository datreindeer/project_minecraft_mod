package com.mechanical.wildlife.elkwight;

import com.mechanical.HeavyLoad;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The Elkwight's double-bladed axe, left where he fell: far too heavy to pick up, a {@link HeavyLoad} like a carcass.
 * One player can't budge it, two drag it by the haft ({@link #pullersToBudge}); it goes on a Travois, Cart or Wagon
 * (2 room). Right-click the top of a block while dragging it to stand it up there as a trophy ({@link ElkwightAxeBlock}):
 * "Heavy Burden". Its runes glow (the renderer draws the trophy block's model, lit). Never despawns.
 */
public class ElkwightAxeEntity extends HeavyLoad {
    static final Identifier HEAVY_BURDEN = Identifier.fromNamespaceAndPath(com.mechanical.Mechanical.MODID, "wilderness/elkwight_axe_trophy");

    public ElkwightAxeEntity(EntityType<? extends ElkwightAxeEntity> type, Level level) {
        super(type, level);
    }

    /** Server: the axe comes down at {@code at}, haft pointing {@code yaw}. */
    public static ElkwightAxeEntity dropAt(ServerLevel level, Vec3 at, float yaw) {
        ElkwightAxeEntity axe = new ElkwightAxeEntity(Elkwights.AXE.get(), level);
        axe.setPos(at);
        axe.setYRot(yaw);
        level.addFreshEntity(axe);
        return axe;
    }

    @Override
    public int pullersNeeded() {
        return 2;
    }

    @Override
    public int pullersToBudge() {
        return 2;
    }

    @Override
    public int loadUnits() {
        return 2;
    }

    /** Both hands on the haft, near its end ({@link #getYRot} points from the head to the haft end), one player each side. */
    @Override
    public Vec3 gripPoint(int slot, float partialTicks) {
        Vec3 haft = Vec3.directionFromRotation(0, getViewYRot(partialTicks)), side = new Vec3(-haft.z, 0, haft.x);
        return getPosition(partialTicks).add(haft.scale(1.0 + 0.2 * (slot / 2))).add(side.scale(slot % 2 == 0 ? 0.35 : -0.35));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (random.nextInt(8) == 0) // the runes glow faintly
                level().addParticle(net.minecraft.core.particles.ParticleTypes.GLOW, getX() + random.nextGaussian() * 0.4, getY() + 0.3,
                        getZ() + random.nextGaussian() * 0.4, 0, 0.015, 0);
            return;
        }
        if (tickHaulers()) return;
        if (isPassenger()) return; // on a travois, which places it
        settle();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) return InteractionResult.PASS;
        if (getVehicle() instanceof com.mechanical.TravoisEntity bed && slotOf(player) < 0 && !player.isShiftKeyDown())
            return bed.interact(player, hand, location); // clicks meant for the cart it lies on
        if (player.isShiftKeyDown() || slotOf(player) >= 0) return com.mechanical.CarcassHauling.pickUpOrLetGo(player, this);
        if (!level().isClientSide()) player.sendOverlayMessage(Component.translatable("message.mechanical.elkwight_axe.heavy"));
        return InteractionResult.SUCCESS;
    }

    /** Stands it up as the trophy on the block clicked (on its top face, with room). */
    @Override
    public boolean putDownOn(ServerLevel level, BlockPos pos, Direction face, Player player) {
        BlockPos at = pos.relative(face);
        BlockState trophy = Elkwights.AXE_TROPHY.get().defaultBlockState().setValue(ElkwightAxeBlock.FACING, player.getDirection().getOpposite());
        if (face != Direction.UP || !level.getBlockState(at).canBeReplaced() || !level.getBlockState(at.above()).canBeReplaced()
                || !trophy.canSurvive(level, at)) return false;
        level.setBlockAndUpdate(at, trophy);
        level.playSound(null, at, SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 1.0F, 0.6F);
        if (player instanceof ServerPlayer sp) Elkwights.award(sp, HEAVY_BURDEN);
        // everyone else on the haft lets go with it
        for (int i = 0; i < MAX_HAULERS; i++) if (hauler(i) instanceof ServerPlayer other && other != player) Elkwights.award(other, HEAVY_BURDEN);
        discard();
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
