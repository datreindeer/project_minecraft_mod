package com.mechanical.wildlife.cold;

import java.util.Comparator;

import com.mechanical.Mechanical;
import com.mechanical.wildlife.FlockBird;
import com.mechanical.wildlife.forestbirds.SoaringBird;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Lammergeier (bearded vulture): soars the mountains looking for bones. A Bone lying on the ground or a Bone Pile (what
 * a predator left of a kill) within {@link #SEARCH} blocks it comes down for and takes in its talons, climbs at least
 * {@link #DROP_HEIGHT} blocks over the ground and lets it fall: the bone cracks open below and leaves Bone Marrow (food)
 * and a little Bone Meal. A bone pile gives it two bones, so two drops. No carcass of its own (feathers, its
 * Lammergeier Feathers: loot table).
 */
public class Lammergeier extends SoaringBird {
    static final FlockBird.Kind KIND = new FlockBird.Kind("lammergeier", 0.5, 14, 26, 900, 1800, 300, 900, false, false, true, 1.4,
            () -> SoundEvents.PARROT_AMBIENT, 0.6F);
    static final int SEARCH = 24, DROP_HEIGHT = 12;
    static final double GRAB = 1.6;
    private int bones;
    /** Where the bone it is going for lies (package access for ColdTest). */
    Vec3 bone;

    public Lammergeier(EntityType<? extends Lammergeier> type, Level level) {
        super(type, level, KIND);
    }

    public int bones() {
        return bones;
    }

    /** Where a bone lies: a Bone item on the ground or a Bone Pile on top of the ground, the nearest; or null. */
    Vec3 findBone() {
        ItemEntity item = level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(SEARCH, 32, SEARCH),
                e -> e.getItem().is(Items.BONE) && e.onGround()).stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);
        if (item != null) return item.position();
        BlockPos at = blockPosition();
        Vec3 best = null;
        for (int dx = -SEARCH; dx <= SEARCH; dx += 2) for (int dz = -SEARCH; dz <= SEARCH; dz += 2) {
            int x = at.getX() + dx, z = at.getZ() + dz;
            BlockPos top = new BlockPos(x, level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z);
            if (!level().getBlockState(top).is(Mechanical.BONE_PILE.get())) top = top.above(); // a pile without collision sits above the heightmap
            if (!level().getBlockState(top).is(Mechanical.BONE_PILE.get())) continue;
            Vec3 c = Vec3.atBottomCenterOf(top);
            if (best == null || c.distanceToSqr(position()) < best.distanceToSqr(position())) best = c;
        }
        return best;
    }

    @Override
    protected Vec3 cruise() {
        if (bones > 0) {
            double ground = surfaceY(getX(), getZ());
            if (getY() - ground >= DROP_HEIGHT) {
                dropBone((ServerLevel) level(), ground);
                return super.cruise();
            }
            return seek(new Vec3(getX() + getDeltaMovement().x * 8, ground + DROP_HEIGHT + 4, getZ() + getDeltaMovement().z * 8), speed());
        }
        if (bone == null && (tickCount + getId()) % 40 == 0) bone = findBone();
        if (bone == null) return super.cruise();
        if (position().distanceToSqr(bone) < GRAB * GRAB) {
            take();
            return super.cruise();
        }
        return seek(bone.add(0, 0.3, 0), speed() * 1.2);
    }

    /** Talons round it: one Bone off the ground, or two from a Bone Pile (the pile is gone). */
    void take() {
        BlockPos at = BlockPos.containing(bone);
        ItemEntity item = level().getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(at).inflate(1.5), e -> e.getItem().is(Items.BONE))
                .stream().findFirst().orElse(null);
        if (item != null) {
            item.getItem().shrink(1);
            if (item.getItem().isEmpty()) item.discard();
            bones = 1;
        } else if (level().getBlockState(at).is(Mechanical.BONE_PILE.get())) {
            level().setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
            bones = 2;
        }
        bone = null;
        if (bones > 0) playSound(SoundEvents.SKELETON_STEP, 0.8F, 1.4F);
    }

    /** Lets a bone fall from high up: it cracks on the ground below into Bone Marrow and a little Bone Meal. */
    void dropBone(ServerLevel level, double ground) {
        bones--;
        Vec3 hit = new Vec3(getX(), ground, getZ());
        for (ItemStack s : new ItemStack[] {new ItemStack(ColdFauna.BONE_MARROW.get()), new ItemStack(Items.BONE_MEAL, 1 + random.nextInt(2))}) {
            ItemEntity e = new ItemEntity(level, getX(), getY() - 0.5, getZ(), s);
            e.setDeltaMovement(0, -0.4, 0);
            level.addFreshEntity(e);
        }
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.BONE_BLOCK_BREAK, SoundSource.NEUTRAL, 1.5F, 0.7F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("bones", bones);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        bones = input.getIntOr("bones", 0);
    }
}
