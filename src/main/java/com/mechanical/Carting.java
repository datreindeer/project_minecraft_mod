package com.mechanical;

import static com.mechanical.Mechanical.ENTITY_TYPES;
import static com.mechanical.Mechanical.ITEMS;
import static com.mechanical.Mechanical.MODID;

import com.mechanical.wildlife.WildHorse;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Bronze Age transport: the {@link CartEntity} (Wooden Wheels cut with the Bronze Saw) and horse taming. Wild Horses are
 * won over by hand-feeding ({@link WildHorse#feed}), led on a Rope Halter and become a {@link com.mechanical.wildlife.TameHorse}
 * that is ridden bareback, better with a Hide Saddle Blanket. Data: scripts/gen_cart.py, textures: scripts/gen_cart_textures.py.
 */
public final class Carting {
    /** Hand feed for wild and tame horses: grain, sheaves, wheat, hay, apples, carrots. */
    public static final TagKey<Item> HORSE_FEED = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "horse_feed"));
    /** Puts a tamed adult horse in the mood: sheaves and hay bales (vanilla's golden food too). */
    public static final TagKey<Item> HORSE_BREEDING_FEED = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "horse_breeding_feed"));
    /** Who wears the Hide Saddle Blanket (only the tamed wild horse has a model for it). */
    public static final TagKey<EntityType<?>> WEARS_HIDE_BLANKET = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MODID, "wears_hide_saddle_blanket"));

    public static final DeferredHolder<EntityType<?>, EntityType<CartEntity>> CART = ENTITY_TYPES.registerEntityType("cart",
            CartEntity::new, MobCategory.MISC, b -> b.noLootTable().sized(1.5F, 1.0F).clientTrackingRange(10));
    public static final DeferredItem<TravoisEntity.Placer> CART_ITEM = ITEMS.registerItem("cart", p -> new TravoisEntity.Placer(p, CART), p -> p.stacksTo(1));
    public static final DeferredItem<Item> WOODEN_WHEEL = ITEMS.registerSimpleItem("wooden_wheel", p -> p.stacksTo(16));
    public static final DeferredItem<RopeHalterItem> ROPE_HALTER = ITEMS.registerItem("rope_halter", RopeHalterItem::new, p -> p.stacksTo(16));
    /** Goes in the horse's saddle slot (vanilla equippable): full speed and full jumps instead of bareback's. */
    public static final DeferredItem<Item> HIDE_SADDLE_BLANKET = ITEMS.registerSimpleItem("hide_saddle_blanket", p -> p.stacksTo(1)
            .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.SADDLE)
                    .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
                    .setAllowedEntities(BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE).getOrThrow(WEARS_HIDE_BLANKET))
                    .setEquipOnInteract(true)
                    .setCanBeSheared(true)
                    .setShearingSound(SoundEvents.SADDLE_UNEQUIP)
                    .build()));

    public static void init() {
    }

    /** Rope Halter: put it on a Wild Horse that trusts you enough ({@link WildHorse#HALTER_TRUST}) to lead it. */
    public static class RopeHalterItem extends Item {
        public RopeHalterItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
            return target instanceof WildHorse horse && !horse.isBaby() ? horse.halter(player, stack) : InteractionResult.PASS;
        }
    }

    private Carting() {}
}
