package com.mechanical.wildlife.jungle;

import com.mechanical.wildlife.CaveLion;
import com.mechanical.wildlife.RoeDeer;
import com.mechanical.wildlife.WildBoar;
import com.mechanical.wildlife.warm.JungleFowl;
import com.mechanical.wildlife.warm.Peafowl;
import com.mechanical.wildlife.warm.Tapir;
import com.mechanical.wildlife.warm.WaterBuffalo;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tiger: the jungle's biggest hunter, alone. Hunts like the cave lion it inherits from (creeps up crouched, freezes when
 * watched, pounces, roars when found out, fears fire), but bigger, and a cat of the thicket: lying in bamboo, ferns, tall
 * grass or leaves it hunts by day too, and while it creeps through such cover it can't be seen at all
 * ({@link #isHidden}: invisible) until it is within {@link #REVEAL} blocks of its quarry or pounces. Swims well and
 * readily. Gives the Raw Tiger Pelt (fat-tanned into a Tiger Pelt: rug, mantle), meat, sinew and bones.
 */
public class Tiger extends CaveLion {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("mechanical", "textures/entity/wildlife/tiger.png");
    /** Hidden in cover until its quarry is this close. */
    static final double REVEAL = 6.0;

    public Tiger(EntityType<? extends Tiger> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 36.0).add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public WaterHabit waterHabit() {
        return WaterHabit.SWIMMER;
    }

    @Override
    protected boolean isPrey(LivingEntity e) {
        return !e.isBaby() && (e instanceof Tapir || e instanceof WaterBuffalo || e instanceof JungleFowl || e instanceof Peafowl
                || e instanceof Orangutan || e instanceof ProboscisMonkey || e instanceof MonitorLizard || e instanceof WildBoar
                || e instanceof RoeDeer || e instanceof com.mechanical.people.Tribesperson) || e instanceof WaterBuffalo && e.isBaby();
    }

    /** Cover a tiger vanishes in: bamboo, ferns, tall grass, leaves, vines, saplings round its feet and shoulders. */
    public boolean inThicket() {
        BlockPos me = blockPosition();
        int cover = 0;
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-1, 0, -1), me.offset(1, 1, 1))) {
            BlockState s = level().getBlockState(p);
            if (s.is(Blocks.BAMBOO) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN) || s.is(BlockTags.LEAVES)
                    || s.is(Blocks.VINE) || s.is(BlockTags.SAPLINGS) || s.is(Blocks.SUGAR_CANE)) cover++;
        }
        return cover >= 3;
    }

    /** In cover the tiger hunts by day as well. */
    @Override
    protected boolean huntingTime() {
        return super.huntingTime() || inThicket();
    }

    /** Creeping through cover, still out of {@link #REVEAL} of its quarry. */
    public boolean isHidden() {
        LivingEntity t = getTarget();
        return isCrouching() && !isLeaping() && inThicket() && (t == null || distanceTo(t) > REVEAL);
    }

    /** Crouches to creep (the stalking pose; normally set by the hunt). */
    public void stalk(boolean crouch) {
        setCrouching(crouch);
    }

    /** Keeps the invisible flag in step with {@link #isHidden} (server; synced like a potion's invisibility). */
    public void updateCamouflage() {
        boolean hidden = isHidden();
        if (hidden != isInvisible()) setInvisible(hidden);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide() && (tickCount + getId()) % 5 == 0) updateCamouflage();
    }

    @Override
    public Identifier texture() {
        return TEXTURE;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.1F;
    }
}
