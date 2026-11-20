package com.mechanical.equipment;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.mechanical.Mechanical;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Accessory slots in the player inventory ({@link Accessory}): Cloak, Neck, Back, two Rings, Hip and Charm, beside and
 * under the player model (InventoryMenuMixin adds them to the inventory menu after the vanilla slots, from index
 * {@link #MENU_START}; InventoryScreenMixin draws the taller screen, CreativeModeInventoryScreenMixin places them in the
 * creative inventory tab).
 *
 * <ul>
 * <li>Worn items are saved with the player and synced to every client that sees them (the worn models, client.EquipmentLayer).</li>
 * <li>Attribute modifiers an item has for slot group ARMOR ("When worn:") apply while it is in its accessory slot.
 * Cloaks ({@link CloakItem}) use this for their armour.</li>
 * <li>A cloak counts as a fifth armour piece: it wears down when armour does (PlayerMixin), and its enchantments work as
 * if it were on the chest (EnchantmentHelperMixin).</li>
 * <li>On death the slots drop like the inventory, unless keepInventory (Curse of Vanishing items are destroyed).</li>
 * <li>Back and Hip hold the Leather Satchel and the {@link QuiverItem}; B opens them ({@link PackMenu}).</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class Equipment {
    /** Inventory-menu index of the first accessory slot (after the off hand, InventoryMenu.SHIELD_SLOT). */
    public static final int MENU_START = InventoryMenu.SHIELD_SLOT + 1;
    /** How much taller the survival inventory screen is: the row of accessory slots under the player model. */
    public static final int SCREEN_EXTRA_HEIGHT = 22;

    public static final Supplier<AttachmentType<EquipmentInventory>> INVENTORY = Mechanical.ATTACHMENTS.register("equipment", () -> AttachmentType
            .builder(EquipmentInventory::new)
            .serialize(new IAttachmentSerializer<EquipmentInventory>() {
                @Override
                public EquipmentInventory read(IAttachmentHolder holder, net.minecraft.world.level.storage.ValueInput input) {
                    EquipmentInventory inventory = new EquipmentInventory();
                    inventory.load(input);
                    return inventory;
                }

                @Override
                public boolean write(EquipmentInventory inventory, net.minecraft.world.level.storage.ValueOutput output) {
                    if (inventory.isEmpty()) return false;
                    inventory.save(output);
                    return true;
                }
            })
            .copyOnDeath()
            .sync(new AttachmentSyncHandler<EquipmentInventory>() {
                @Override
                public void write(RegistryFriendlyByteBuf buf, EquipmentInventory inventory, boolean initialSync) {
                    for (int i = 0; i < Accessory.COUNT; i++) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, inventory.getItem(i));
                }

                /** Into the client's existing copy where there is one, so its inventory menu keeps showing the same object. */
                @Override
                public EquipmentInventory read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, @Nullable EquipmentInventory previous) {
                    EquipmentInventory inventory = previous != null ? previous : new EquipmentInventory();
                    for (int i = 0; i < Accessory.COUNT; i++) inventory.setItem(i, ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                    return inventory;
                }
            })
            .build());

    /** Loads the registrations (attachment here, the B menu type in PackMenu); call from the mod constructor. */
    public static void init() {
        PackMenu.init();
    }

    public static EquipmentInventory of(Player player) {
        return player.getData(INVENTORY);
    }

    public static ItemStack get(Player player, Accessory slot) {
        return of(player).get(slot);
    }

    /** Can this item be worn in that slot? */
    public static boolean fits(Accessory slot, ItemStack stack) {
        return stack.is(slot.tag);
    }

    /** The empty accessory slot this item would go to on a shift-click, or null. */
    public static @Nullable Accessory freeSlotFor(Player player, ItemStack stack) {
        EquipmentInventory inventory = of(player);
        for (Accessory slot : Accessory.values()) {
            if (fits(slot, stack) && inventory.get(slot).isEmpty()) return slot;
        }
        return null;
    }

    /** A player put an item into (or took one out of) an accessory slot: the equip sound, heard by everyone near. */
    static void onEquip(Player player, Accessory slot, ItemStack previous, ItemStack stack) {
        if (player.level().isClientSide() || player.isSpectator() || stack.isEmpty() || ItemStack.isSameItemSameComponents(previous, stack)) return;
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), equipSound(slot, stack), player.getSoundSource(), 1.0F, 1.0F);
    }

    private static SoundEvent equipSound(Accessory slot, ItemStack stack) {
        if (stack.getItem() instanceof CloakItem cloak) return cloak.equipSound().value();
        return switch (slot) {
            case NECK, RING_1, RING_2, CHARM -> SoundEvents.ARMOR_EQUIP_CHAIN.value();
            default -> SoundEvents.ARMOR_EQUIP_LEATHER.value();
        };
    }

    /**
     * Right-clicking a wearable item in the hand: swaps it with what is in its slot (the first free one, else the first
     * of its kind), like armour. Runs on both sides; false when it can't (nothing fits, or the worn one is cursed on).
     */
    static boolean equipFromHand(Player player, ItemStack held, net.minecraft.world.InteractionHand hand) {
        Accessory target = freeSlotFor(player, held);
        if (target == null) {
            for (Accessory slot : Accessory.values()) {
                if (fits(slot, held)) {
                    target = slot;
                    break;
                }
            }
        }
        if (target == null) return false;
        EquipmentInventory inventory = of(player);
        ItemStack worn = inventory.get(target);
        if (!worn.isEmpty() && !player.isCreative() && EnchantmentHelper.has(worn, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) return false;
        onEquip(player, target, worn, held);
        inventory.set(target, held.copyWithCount(1));
        if (held.getCount() > 1) {
            held.shrink(1);
            if (!worn.isEmpty() && !player.level().isClientSide()) player.getInventory().placeItemBackInInventory(worn, net.minecraft.util.Prediction.SERVER_ONLY);
        } else {
            player.setItemInHand(hand, worn);
        }
        return true;
    }

    // ---------------------------------------------------------------- change tracking: attributes, sync, advancements

    @SubscribeEvent
    static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) update(player);
    }

    /** Each server tick: a slot whose item changed swaps its attribute modifiers, grants its advancement and is synced. */
    static void update(Player player) {
        EquipmentInventory inventory = of(player);
        boolean changed = false;
        for (Accessory slot : Accessory.values()) {
            int i = slot.ordinal();
            ItemStack now = inventory.getItem(i);
            ItemStack last = inventory.lastSeen[i];
            if (ItemStack.matches(now, last)) continue;
            if (!ItemStack.isSameItemSameComponents(now, last)) applyModifiers(player, inventory, slot, now);
            if (!now.isEmpty() && !ItemStack.isSameItem(now, last) && player instanceof ServerPlayer sp) onWorn(sp, slot, now);
            inventory.lastSeen[i] = now.copy();
            changed = true;
        }
        if (changed && player instanceof ServerPlayer) player.syncData(INVENTORY);
    }

    /** Swaps the attribute modifiers of the slot's old item for those of the new one ("When worn:" ones). */
    private static void applyModifiers(Player player, EquipmentInventory inventory, Accessory slot, ItemStack stack) {
        List<EquipmentInventory.AppliedModifier> applied = inventory.applied[slot.ordinal()];
        for (EquipmentInventory.AppliedModifier old : applied) {
            AttributeInstance instance = player.getAttribute(old.attribute());
            if (instance != null) instance.removeModifier(old.id());
        }
        applied.clear();
        if (stack.isEmpty()) return;
        stack.forEachModifier(EquipmentSlotGroup.ARMOR, (attribute, modifier, display) -> {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) return;
            // a slot's own id, so it never clashes with the same modifier on real armour or in another slot
            Identifier id = Identifier.fromNamespaceAndPath(Mechanical.MODID, "accessory/" + slot.name().toLowerCase(Locale.ROOT) + "/"
                    + modifier.id().getNamespace() + "/" + modifier.id().getPath());
            instance.removeModifier(id);
            instance.addTransientModifier(new AttributeModifier(id, modifier.amount(), modifier.operation()));
            applied.add(new EquipmentInventory.AppliedModifier(attribute, id));
        });
    }

    /** A new item went into a slot: its advancement (granted from code, see scripts/gen_advancements.py). */
    private static void onWorn(ServerPlayer player, Accessory slot, ItemStack stack) {
        if (slot == Accessory.BACK && stack.is(Mechanical.LEATHER_SATCHEL.get())) award(player, "wilderness/worn_satchel");
        if (slot == Accessory.HIP && stack.getItem() instanceof QuiverItem) award(player, "wilderness/quiver");
        if (slot == Accessory.CLOAK && stack.getItem() instanceof CloakItem) award(player, "wilderness/worn_cloak");
    }

    private static void award(ServerPlayer player, String path) {
        var holder = player.level().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath(Mechanical.MODID, path));
        if (holder != null) player.getAdvancements().award(holder, "done");
    }

    // ---------------------------------------------------------------- death (PlayerMixin#dropEquipment)

    /** After the inventory dropped on death: the worn items drop too, unless keepInventory. */
    public static void dropOnDeath(Player player, ServerLevel level) {
        if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) return;
        EquipmentInventory inventory = of(player);
        for (int i = 0; i < Accessory.COUNT; i++) {
            ItemStack stack = inventory.removeItemNoUpdate(i);
            if (stack.isEmpty() || EnchantmentHelper.has(stack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP)) continue;
            ItemEntity drop = player.createItemStackToDrop(stack, true, false);
            if (drop == null) continue;
            if (player.captureDrops() != null) player.captureDrops().add(drop);
            else level.addFreshEntity(drop);
        }
        inventory.setChanged();
    }

    // ---------------------------------------------------------------- the cloak as a fifth armour piece

    /** PlayerMixin#hurtArmor: the cloak takes the same wear as each armour piece from a hit armour stops. */
    public static void hurtCloak(Player player, DamageSource source, float damage) {
        if (!(player.level() instanceof ServerLevel level) || damage <= 0 || source.is(DamageTypeTags.BYPASSES_ARMOR)) return;
        ItemStack cloak = get(player, Accessory.CLOAK);
        if (!(cloak.getItem() instanceof CloakItem) || !cloak.isDamageableItem() || !cloak.canBeHurtBy(source)) return;
        damageCloak(player, level, (int) Math.max(1.0F, damage / 4.0F));
    }

    /** Wears the cloak down by {@code amount}; a worn-out cloak breaks with the armour-break sound and is gone. */
    public static void damageCloak(Player player, ServerLevel level, int amount) {
        ItemStack cloak = get(player, Accessory.CLOAK);
        if (cloak.isEmpty()) return;
        cloak.hurtAndBreak(amount, level, player, broken -> {
            Holder<SoundEvent> sound = broken.get(DataComponents.BREAK_SOUND);
            if (sound != null) level.playSound(null, player.getX(), player.getY(), player.getZ(), sound.value(), player.getSoundSource(), 0.8F,
                    0.8F + player.getRandom().nextFloat() * 0.4F);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, broken.getItem()), player.getX(), player.getY(1.0 / 3), player.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
        });
        if (cloak.isEmpty()) of(player).set(Accessory.CLOAK, ItemStack.EMPTY);
    }

    /**
     * The worn cloak's enchantments as if it were on the chest (EnchantmentHelperMixin): Protection, Thorns and the like
     * count, and its wear (Thorns) goes to the cloak itself.
     */
    public static void forEachCloakEnchantment(net.minecraft.world.entity.LivingEntity owner, EnchantmentHelper.EnchantmentInSlotVisitor visitor) {
        if (!(owner instanceof Player player)) return;
        ItemStack cloak = get(player, Accessory.CLOAK);
        if (cloak.isEmpty()) return;
        ItemEnchantments enchantments = cloak.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return;
        EnchantedItemInUse inUse = cloakInUse(player, cloak);
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().value().matchingSlot(EquipmentSlot.CHEST)) visitor.accept(entry.getKey(), entry.getIntValue(), inUse);
        }
    }

    /** The worn cloak if it carries an enchantment with this effect (Mending picks among these), see EnchantmentHelperMixin. */
    public static Optional<EnchantedItemInUse> cloakWith(net.minecraft.world.entity.LivingEntity owner,
            net.minecraft.core.component.DataComponentType<?> effect, java.util.function.Predicate<ItemStack> predicate) {
        if (!(owner instanceof Player player)) return Optional.empty();
        ItemStack cloak = get(player, Accessory.CLOAK);
        if (cloak.isEmpty() || !predicate.test(cloak)) return Optional.empty();
        for (var entry : cloak.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {
            if (entry.getKey().value().effects().has(effect) && entry.getKey().value().matchingSlot(EquipmentSlot.CHEST))
                return Optional.of(cloakInUse(player, cloak));
        }
        return Optional.empty();
    }

    private static EnchantedItemInUse cloakInUse(Player player, ItemStack cloak) {
        // breaking goes through our slot, not the chest (whose real armour must keep its effects)
        return new EnchantedItemInUse(cloak, EquipmentSlot.CHEST, player, broken -> {
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, broken.getItem()), player.getX(), player.getY(1.0 / 3), player.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
                of(player).set(Accessory.CLOAK, ItemStack.EMPTY);
            }
        });
    }

    private Equipment() {}
}
