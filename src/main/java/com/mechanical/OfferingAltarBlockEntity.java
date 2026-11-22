package com.mechanical;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.wildlife.BrownBear;
import com.mechanical.wildlife.WildAnimal;
import com.mechanical.wildlife.WildBoar;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * The Offering Altar holds one item on display. A Giant Antler (tag mechanical:altar_offerings) starts a ritual: the server
 * counts {@link #RITUAL_TICKS}, calls deer to the tree's entrance and finally swaps the antler for a mythic item; clients
 * play the animation from the synced counter (particles here, the floating item in AltarRenderer).
 */
public class OfferingAltarBlockEntity extends BlockEntity {
    public static final int RITUAL_TICKS = 140;
    /** How long called animals stay calm and gathered after the ritual. */
    private static final int LINGER_TICKS = 900;

    private ItemStack item = ItemStack.EMPTY;
    /** 0 = idle, else ticks into the ritual. */
    private int ritual;
    /** Where animals gather: just outside the hollow's opening (set by worldgen; defaults to the altar). */
    private BlockPos entrance;
    private long herdUntil;
    /** The Oak Folk (people.OakFolk) have been settled round this oak. Only a worldgen altar (entrance set) gets them. */
    private boolean folk;
    private final List<Mob> herd = new ArrayList<>();

    public OfferingAltarBlockEntity(BlockPos pos, BlockState state) {
        super(AncientOak.ALTAR_ENTITY.get(), pos, state);
    }

    public ItemStack item() {
        return item;
    }

    public int ritual() {
        return ritual;
    }

    public void setEntrance(BlockPos entrance) {
        this.entrance = entrance.immutable();
        setChanged();
    }

    public boolean busy() {
        return ritual > 0;
    }

    /** Puts one of the stack on the altar (starting the ritual for an offering). */
    public void place(ServerLevel level, ItemStack one) {
        item = one;
        level.playSound(null, worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 0.8F);
        if (one.is(AncientOak.ALTAR_OFFERINGS)) startRitual(level);
        sync();
    }

    public ItemStack take() {
        ItemStack out = item;
        item = ItemStack.EMPTY;
        sync();
        return out;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    private void startRitual(ServerLevel level) {
        ritual = 1;
        level.setBlock(worldPosition, getBlockState().setValue(AncientOak.Altar.ACTIVE, true), Block.UPDATE_ALL);
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.2F, 1.5F);
        herdUntil = level.getGameTime() + RITUAL_TICKS + LINGER_TICKS;
        callAnimals(level);
    }

    // ---- ticking ----

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, OfferingAltarBlockEntity altar) {
        if (altar.ritual > 0) {
            int t = ++altar.ritual;
            if (t % 20 == 0) level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.5F, 0.6F + t / (float) RITUAL_TICKS);
            if (t == 60) level.playSound(null, pos, SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.BLOCKS, 1.5F, 0.6F);
            if (t >= RITUAL_TICKS) altar.finish(level);
        }
        if (level.getGameTime() % 20 == 0 && !altar.herd.isEmpty()) altar.herd(level);
        if (!altar.folk && altar.entrance != null && level.getGameTime() % 100 == 0
                && level.hasChunksAt(pos.offset(-20, 0, -20), pos.offset(20, 0, 20))) {
            altar.folk = true;
            altar.setChanged();
            com.mechanical.people.OakFolk.spawnGroup(level, pos, altar.entrance);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, OfferingAltarBlockEntity altar) {
        if (altar.ritual > 0 && altar.ritual < RITUAL_TICKS) altar.ritualParticles(level, ++altar.ritual);
    }

    private void finish(ServerLevel level) {
        ritual = 0;
        item = new ItemStack(AncientOak.mythics().get(level.getRandom().nextInt(5)).get());
        level.setBlock(worldPosition, getBlockState().setValue(AncientOak.Altar.ACTIVE, false), Block.UPDATE_ALL);
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.8, z = worldPosition.getZ() + 0.5;
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 80, 0.3, 0.3, 0.3, 0.45);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 25, 0.8, 0.6, 0.8, 0);
        level.sendParticles(ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0xFF5E9A32), x, y + 1.5, z, 30, 1.5, 0.5, 1.5, 0);
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.5F, 0.7F);
        level.playSound(null, worldPosition, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.5F, 0.6F);
        sync();
    }

    /** Client: motes spiral in, spores rise, leaves drift down, the ground pulses. t runs 1..RITUAL_TICKS. */
    private void ritualParticles(Level level, int t) {
        RandomSource r = level.getRandom();
        double cx = worldPosition.getX() + 0.5, cz = worldPosition.getZ() + 0.5, floor = worldPosition.getY() + 0.05;
        double hover = worldPosition.getY() + 1.0 + floatHeight(t);
        float build = Math.min(1.0F, t / 60.0F); // the swell grows for three seconds
        // enchant motes fly from a turning ring around the hollow into the antler
        for (int i = 0; i < 1 + (int) (build * 3); i++) {
            double a = t * 0.15 + i * Math.PI * 2 / 3 + r.nextDouble() * 0.4, rad = 2.2 + r.nextDouble();
            level.addParticle(ParticleTypes.ENCHANT, cx, hover, cz, Math.cos(a) * rad, -0.6 + r.nextDouble() * 1.4, Math.sin(a) * rad);
        }
        // green and gold dust climbing a slow spiral
        double a = t * 0.25;
        double sr = 1.6 - build * 0.8;
        level.addParticle(new DustParticleOptions(r.nextBoolean() ? 0x7FD14A : 0xE8C547, 0.9F),
                cx + Math.cos(a) * sr, floor + (t % 40) / 20.0, cz + Math.sin(a) * sr, 0, 0.02, 0);
        level.addParticle(new DustParticleOptions(0x9AE05C, 0.7F),
                cx - Math.cos(a) * sr, floor + ((t + 20) % 40) / 20.0, cz - Math.sin(a) * sr, 0, 0.02, 0);
        // little glowing spores drift up off the floor
        if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.GLOW, cx + (r.nextDouble() - 0.5) * 4, floor + 0.1, cz + (r.nextDouble() - 0.5) * 4,
                0, 0.03 + r.nextDouble() * 0.03, 0);
        if (r.nextInt(4) == 0) level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, cx + (r.nextDouble() - 0.5) * 4, floor + r.nextDouble() * 2.5,
                cz + (r.nextDouble() - 0.5) * 4, 0, 0, 0);
        // leaves drifting down the hollow
        if (r.nextInt(5) == 0) level.addParticle(ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0xFF4F8A2B),
                cx + (r.nextDouble() - 0.5) * 3, floor + 3.2, cz + (r.nextDouble() - 0.5) * 3, 0, 0, 0);
        // the roots pulse: a ring of green sparkles runs out across the floor every second
        if (t % 20 == 0) {
            for (int i = 0; i < 16; i++) {
                double b = i * Math.PI / 8;
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, cx + Math.cos(b) * 0.8, floor + 0.1, cz + Math.sin(b) * 0.8,
                        Math.cos(b) * 0.25, 0, Math.sin(b) * 0.25);
            }
        }
        // the antler starts to dissolve
        if (t > RITUAL_TICKS - 25) level.addParticle(ParticleTypes.END_ROD, cx, hover, cz,
                (r.nextDouble() - 0.5) * 0.15, (r.nextDouble() - 0.5) * 0.15, (r.nextDouble() - 0.5) * 0.15);
    }

    /** How high above the altar top the offering floats at ritual tick t (rises over 2 s, then hovers). */
    public static float floatHeight(float t) {
        float rise = Mth.clamp(t / 40.0F, 0, 1);
        return 0.15F + 0.75F * rise * rise * (3 - 2 * rise);
    }

    // ---- the animals ----

    private BlockPos gather() {
        return entrance != null ? entrance : worldPosition;
    }

    private static boolean gentle(Mob mob) {
        return mob instanceof WildAnimal && !(mob instanceof BrownBear) && !(mob instanceof WildBoar)
                && !BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath().equals("giant_deer") && AnimalFear.isWild(mob);
    }

    /** 3-6 deer appear 24-40 blocks away, and every gentle wild animal within 48 blocks is called to the tree. */
    private void callAnimals(ServerLevel level) {
        RandomSource r = level.getRandom();
        List<EntityType<? extends Mob>> kinds = new ArrayList<>(List.of(Wildlife.RED_DEER.get(), Wildlife.ROE_DEER.get(), Wildlife.ROE_DEER.get()));
        if (level.getBiome(worldPosition).is(BiomeTags.IS_TAIGA)) {
            kinds.add(Wildlife.REINDEER.get());
            kinds.add(Wildlife.MOOSE.get());
        }
        int count = 3 + r.nextInt(4);
        for (int i = 0, tries = 0; i < count && tries < 30; tries++) {
            double a = r.nextDouble() * Math.PI * 2, d = 24 + r.nextDouble() * 16;
            int x = worldPosition.getX() + (int) (Math.cos(a) * d), z = worldPosition.getZ() + (int) (Math.sin(a) * d);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
            BlockPos at = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            if (!level.getFluidState(at.below()).isEmpty() || !level.getBlockState(at.below()).isSolid()) continue;
            Mob mob = kinds.get(r.nextInt(kinds.size())).create(level, EntitySpawnReason.EVENT);
            if (mob == null) continue;
            mob.snapTo(x + 0.5, at.getY(), z + 0.5, r.nextFloat() * 360, 0);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
            level.addFreshEntityWithPassengers(mob);
            herd.add(mob);
            i++;
        }
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(worldPosition).inflate(48), OfferingAltarBlockEntity::gentle))
            if (!herd.contains(mob)) herd.add(mob);
        herd(level);
    }

    /** Once a second: keep them calm, walk them to the entrance, and once there let them stand and look at the altar. */
    private void herd(ServerLevel level) {
        long now = level.getGameTime();
        if (now > herdUntil) {
            herd.clear();
            return;
        }
        herd.removeIf(m -> !m.isAlive() || m.isRemoved());
        BlockPos g = gather();
        for (int i = 0; i < herd.size(); i++) {
            Mob mob = herd.get(i);
            AncientOak.calm(mob, herdUntil);
            // each animal has its own spot in a loose half-ring around the entrance
            double a = i * 2.4, rad = 2.5 + (i % 3);
            double tx = g.getX() + 0.5 + Math.cos(a) * rad, tz = g.getZ() + 0.5 + Math.sin(a) * rad;
            if (mob.distanceToSqr(tx, mob.getY(), tz) > 4) mob.getNavigation().moveTo(tx, g.getY(), tz, 0.9);
            else {
                mob.getNavigation().stop();
                mob.getLookControl().setLookAt(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5);
            }
        }
    }

    // ---- saving and syncing ----

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!item.isEmpty()) output.store("item", ItemStack.CODEC, item);
        output.putInt("ritual", ritual);
        if (entrance != null) output.putLong("entrance", entrance.asLong());
        output.putBoolean("folk", folk);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        item = input.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        ritual = input.getIntOr("ritual", 0);
        long e = input.getLongOr("entrance", Long.MIN_VALUE);
        entrance = e == Long.MIN_VALUE ? null : BlockPos.of(e);
        folk = input.getBooleanOr("folk", false);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }
}
