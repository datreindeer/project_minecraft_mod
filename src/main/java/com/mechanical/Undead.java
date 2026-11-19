package com.mechanical;

import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.DropChances;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.SpearUseGoal;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Varied undead. Every adult zombie has one of eight stone-age looks and every skeleton one of four; the look is picked
 * (weighted) from the entity's UUID, so it is saved with the entity and known to clients without any extra data.
 * Three zombie looks carry traits (attribute modifiers, added once and saved): the big Brute, the fast Gaunt and the
 * club-carrying Clubber. Skeletons get a stone-age weapon instead of the bow most of the time and fight in melee;
 * spear holders charge with the spear like vanilla zombies do.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Undead {
    public enum ZombieLook {
        HIDE_MAN(20), FUR_WOMAN(20), BONE_ELDER(14), PAINTED(14), BOG_BODY(12), BRUTE(7), GAUNT(7), CLUBBER(6);
        final int weight;
        ZombieLook(int weight) { this.weight = weight; }
    }

    public enum SkeletonLook {
        BONE(40), MOSSY(25), HIDE(20), BOG(15);
        final int weight;
        SkeletonLook(int weight) { this.weight = weight; }
    }

    /** Set once an undead got its traits/weapon, so reloading doesn't roll again. */
    static final String SETUP_TAG = "mechanical.undead";
    /** Marks the Brute for the "kill a Brute" advancement (entity_tags predicate). */
    public static final String BRUTE_TAG = "mechanical.zombie_brute";
    private static final Identifier TRAIT = Identifier.fromNamespaceAndPath(Mechanical.MODID, "undead_variant");

    public static ZombieLook zombieLook(Entity zombie) {
        ZombieLook[] all = ZombieLook.values();
        int[] w = new int[all.length];
        for (int i = 0; i < all.length; i++) w[i] = all[i].weight;
        return all[pick(zombie.getUUID(), 0x5A3B1E, w)];
    }

    public static SkeletonLook skeletonLook(Entity skeleton) {
        SkeletonLook[] all = SkeletonLook.values();
        int[] w = new int[all.length];
        for (int i = 0; i < all.length; i++) w[i] = all[i].weight;
        return all[pick(skeleton.getUUID(), 0x2C9F47, w)];
    }

    /** Weighted index from a UUID: same answer on server and client, forever. */
    static int pick(UUID id, long salt, int[] weights) {
        long h = (id.getMostSignificantBits() ^ id.getLeastSignificantBits() ^ salt) * 0x9E3779B97F4A7C15L;
        h ^= h >>> 31;
        int total = 0;
        for (int w : weights) total += w;
        int roll = (int) Math.floorMod(h, (long) total);
        for (int i = 0; i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0) return i;
        }
        return weights.length - 1;
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        Entity entity = event.getEntity();
        if (entity.getType() == EntityTypes.ZOMBIE && entity instanceof Zombie zombie && !zombie.isBaby()) {
            if (zombie.entityTags().contains(SETUP_TAG)) return;
            zombie.addTag(SETUP_TAG);
            setUpZombie(zombie, zombieLook(zombie));
        } else if (entity.getType() == EntityTypes.SKELETON && entity instanceof Skeleton skeleton) {
            // goals aren't saved: add the spear charge on every load (it only runs while a spear is held)
            skeleton.goalSelector.addGoal(2, new SpearUseGoal<>(skeleton, 1.0, 1.0, 10.0F, 2.0F));
            if (skeleton.entityTags().contains(SETUP_TAG)) return;
            skeleton.addTag(SETUP_TAG);
            armSkeleton(skeleton);
        }
    }

    static void setUpZombie(Zombie zombie, ZombieLook look) {
        switch (look) {
            case BRUTE -> {
                zombie.addTag(BRUTE_TAG);
                trait(zombie, Attributes.MAX_HEALTH, 0.6, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                trait(zombie, Attributes.KNOCKBACK_RESISTANCE, 0.6, AttributeModifier.Operation.ADD_VALUE);
                trait(zombie, Attributes.MOVEMENT_SPEED, -0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                trait(zombie, Attributes.ATTACK_DAMAGE, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                trait(zombie, Attributes.SCALE, 0.25, AttributeModifier.Operation.ADD_VALUE);
            }
            case GAUNT -> {
                trait(zombie, Attributes.MAX_HEALTH, -0.35, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                trait(zombie, Attributes.MOVEMENT_SPEED, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
                trait(zombie, Attributes.SCALE, 0.05, AttributeModifier.Operation.ADD_VALUE); // tall and thin (renderer narrows it)
            }
            case CLUBBER -> equip(zombie, new ItemStack(Mechanical.CRUDE_STONE_AXE.get()));
            default -> { }
        }
        zombie.setHealth(zombie.getMaxHealth());
    }

    private static void trait(Mob mob, Holder<Attribute> attribute, double amount, AttributeModifier.Operation op) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null && !instance.hasModifier(TRAIT)) instance.addPermanentModifier(new AttributeModifier(TRAIT, amount, op));
    }

    /** Skeleton weapons: 35 % keep the bow, 25 % Flint Spear, 10 % Crude Stone Axe, 10 % Flint Axe, 20 % bare bones. */
    static void armSkeleton(Skeleton skeleton) {
        // only a fresh vanilla bow is swapped; picked-up or given items stay
        if (!skeleton.getMainHandItem().is(Items.BOW) || skeleton.getDropChances().isPreserved(EquipmentSlot.MAINHAND)) return;
        int roll = skeleton.getRandom().nextInt(100);
        if (roll < 35) return;
        equip(skeleton, roll < 60 ? new ItemStack(Mechanical.FLINT_SPEAR.get())
                : roll < 70 ? new ItemStack(Mechanical.CRUDE_STONE_AXE.get())
                : roll < 80 ? new ItemStack(Mechanical.FLINT_AXE.get())
                : ItemStack.EMPTY);
    }

    /** Puts the weapon in hand (skeletons switch to melee in setItemSlot) with the normal vanilla drop chance. */
    private static void equip(Mob mob, ItemStack weapon) {
        mob.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        mob.setDropChance(EquipmentSlot.MAINHAND, DropChances.DEFAULT_EQUIPMENT_DROP_CHANCE);
    }

    private Undead() {}
}
