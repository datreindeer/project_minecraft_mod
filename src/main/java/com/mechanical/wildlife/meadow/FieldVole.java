package com.mechanical.wildlife.meadow;

import java.util.EnumSet;
import java.util.function.BiPredicate;

import com.mechanical.AnimalFear;
import com.mechanical.wildlife.WildAnimal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Field vole: a small, blunt-nosed, short-tailed grey-brown rodent of grassland and fields, living in loose colonies.
 * A crop and store pest, but only at night and only close to fields:
 * <ul>
 * <li>it nibbles crops on farmland: a growing crop is set back two stages, a fresh seedling is eaten whole;</li>
 * <li>near farmland it gets into wooden Chests and Barrels and eats grain from them (item tag {@code mechanical:vole_food}),
 * one or two at a time. Clay Storage Jars, the Root Cellar and Bark Baskets keep it out;</li>
 * <li>every three meals, if the colony nearby is still small (fewer than {@link #CROWDED}), it has a litter of one or
 * two young: unchecked voles on a farm multiply.</li>
 * </ul>
 * Respects the mobGriefing game rule. Prey for wild cats, foxes, cats and ocelots, which it runs from (a cat on the farm keeps voles off), and snared like
 * other small game. Leaves a tiny carcass (just the Raw Vole).
 */
public class FieldVole extends WildAnimal {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/field_vole.png");
    /** Meals (crops or grain) before a litter, and the colony size within 16 blocks that stops litters. */
    static final int MEALS_PER_LITTER = 3, CROWDED = 6;
    /** Farmland this close counts as "by the fields" (store raids). */
    static final int FIELD_RANGE = 12, CROP_RANGE = 8, STORE_RANGE = 8;
    private int meals;
    private long nextRaid;

    public FieldVole(EntityType<? extends FieldVole> type, Level level) {
        super(type, level);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.POOR;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 3.0).add(Attributes.MOVEMENT_SPEED, 0.28);
    }

    /** The hunters a vole runs from. */
    static boolean isHunter(LivingEntity e) {
        return e instanceof WildCat || e instanceof Cat || e instanceof Ocelot || e instanceof Fox;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Animal.class, FieldVole::isHunter, 8.0F, 1.2, 1.6, e -> true));
        goalSelector.addGoal(4, new EatCropGoal(this));
        goalSelector.addGoal(4, new RaidStoreGoal(this));
    }

    /** Night-time pest work: dusk to dawn, not while asleep, frightened or carried off by the panic. */
    boolean pestTime() {
        return level().isDarkOutside() && !isBaby() && !isDozing() && !AnimalFear.isPanicking(this)
                && level() instanceof ServerLevel server && server.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
    }

    @Override
    protected boolean sleepTime() {
        return level().isBrightOutside(); // voles are about at night and dusk; they rest in their runs by day
    }

    /** One more meal; every few meals a litter, while the colony is small. */
    void hadMeal() {
        if (++meals < MEALS_PER_LITTER || !(level() instanceof ServerLevel level)) return;
        meals = 0;
        if (level.getEntitiesOfClass(FieldVole.class, getBoundingBox().inflate(16)).size() >= CROWDED) return;
        int young = 1 + random.nextInt(2);
        for (int i = 0; i < young; i++) {
            FieldVole baby = MeadowFauna.FIELD_VOLE.get().create(level, EntitySpawnReason.BREEDING);
            if (baby == null) continue;
            baby.setAge(-24000);
            baby.snapTo(getX(), getY(), getZ(), random.nextFloat() * 360, 0);
            level.addFreshEntity(baby);
        }
    }

    /** Nearest position within (r, dy) matching {@code test}, or null. Only on the rare goal checks. */
    BlockPos find(int r, int dy, BiPredicate<BlockPos, BlockState> test) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(blockPosition().offset(-r, -dy, -r), blockPosition().offset(r, dy, r))) {
            BlockState s = level().getBlockState(p);
            if (!test.test(p, s)) continue;
            double d = p.distSqr(blockPosition());
            if (d < bestDist) {
                bestDist = d;
                best = p.immutable();
            }
        }
        return best;
    }

    static boolean unprotectedStore(BlockEntity be) {
        return be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity;
    }

    static int foodSlot(Container c, net.minecraft.util.RandomSource random) {
        int found = -1, seen = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).is(MeadowFauna.VOLE_FOOD) && random.nextInt(++seen) == 0) found = i; // a random one of them
        }
        return found;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Meals", meals);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        meals = input.getIntOr("Meals", 0);
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    protected float babyChance() {
        return 0.2F;
    }

    @Override
    public double fearMultiplier() {
        return 0.8;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return random.nextInt(3) == 0 ? SoundEvents.RABBIT_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RABBIT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RABBIT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.9F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    /** Walks to a crop on farmland and nibbles it. */
    static class EatCropGoal extends Goal {
        private final FieldVole vole;
        private BlockPos crop;
        private int time;

        EatCropGoal(FieldVole vole) {
            this.vole = vole;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private static boolean isCrop(BlockState s) {
            return s.getBlock() instanceof CropBlock;
        }

        @Override
        public boolean canUse() {
            if (!vole.pestTime() || vole.getRandom().nextInt(120) != 0) return false;
            crop = vole.find(CROP_RANGE, 2, (p, s) -> isCrop(s));
            return crop != null;
        }

        @Override
        public void start() {
            time = 0;
            vole.getNavigation().moveTo(crop.getX() + 0.5, crop.getY(), crop.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return crop != null && time < 300 && vole.pestTime() && isCrop(vole.level().getBlockState(crop));
        }

        @Override
        public void tick() {
            time++;
            vole.getLookControl().setLookAt(Vec3.atCenterOf(crop));
            if (vole.distanceToSqr(Vec3.atBottomCenterOf(crop)) > 1.2) {
                if ((vole.tickCount + vole.getId()) % 20 == 0) vole.getNavigation().moveTo(crop.getX() + 0.5, crop.getY(), crop.getZ() + 0.5, 1.0);
                return;
            }
            Level level = vole.level();
            BlockState s = level.getBlockState(crop);
            CropBlock block = (CropBlock) s.getBlock();
            int age = block.getAge(s);
            if (age == 0) level.destroyBlock(crop, false, vole); // a seedling: gone
            else {
                level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK, crop, Block.getId(s));
                level.setBlock(crop, block.getStateForAge(Math.max(0, age - 2)), Block.UPDATE_ALL);
            }
            level.playSound(null, crop, SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.4F, 1.8F);
            vole.hadMeal();
            crop = null;
        }

        @Override
        public void stop() {
            crop = null;
            vole.getNavigation().stop();
        }
    }

    /** By the fields: gets into a chest or barrel and eats grain from it. */
    static class RaidStoreGoal extends Goal {
        /** Between raids, per vole. */
        private static final int COOLDOWN = 2400;
        private final FieldVole vole;
        private BlockPos store;
        private int time;

        RaidStoreGoal(FieldVole vole) {
            this.vole = vole;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private Container container() {
            return store != null && vole.level().getBlockEntity(store) instanceof Container c && unprotectedStore((BlockEntity) c) ? c : null;
        }

        @Override
        public boolean canUse() {
            if (!vole.pestTime() || vole.level().getGameTime() < vole.nextRaid || vole.getRandom().nextInt(200) != 0) return false;
            if (vole.find(FIELD_RANGE, 3, (p, s) -> s.is(Blocks.FARMLAND)) == null) return false;
            store = vole.find(STORE_RANGE, 3, (p, s) -> s.hasBlockEntity() && unprotectedStore(vole.level().getBlockEntity(p))
                    && vole.level().getBlockEntity(p) instanceof Container c && foodSlot(c, vole.getRandom()) >= 0);
            return store != null;
        }

        @Override
        public void start() {
            time = 0;
            vole.nextRaid = vole.level().getGameTime() + COOLDOWN;
            vole.getNavigation().moveTo(store.getX() + 0.5, store.getY(), store.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return time < 300 && vole.pestTime() && container() != null;
        }

        @Override
        public void tick() {
            time++;
            vole.getLookControl().setLookAt(Vec3.atCenterOf(store));
            if (vole.distanceToSqr(Vec3.atCenterOf(store)) > 2.6) {
                if ((vole.tickCount + vole.getId()) % 20 == 0) vole.getNavigation().moveTo(store.getX() + 0.5, store.getY(), store.getZ() + 0.5, 1.0);
                return;
            }
            Container c = container();
            int slot = foodSlot(c, vole.getRandom());
            if (slot >= 0 && vole.level() instanceof ServerLevel level) {
                ItemStack eaten = c.removeItem(slot, 1 + vole.getRandom().nextInt(2));
                c.setChanged();
                Vec3 at = Vec3.atCenterOf(store);
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, eaten.getItem()), at.x, at.y + 0.5, at.z, 8, 0.25, 0.2, 0.25, 0.05);
                level.playSound(null, store, SoundEvents.GENERIC_EAT.value(), SoundSource.NEUTRAL, 0.5F, 1.8F);
                level.playSound(null, store, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.4F, 1.6F); // gnawing at the wood
                vole.hadMeal();
            }
            store = null;
        }

        @Override
        public void stop() {
            store = null;
            vole.getNavigation().stop();
        }
    }
}
