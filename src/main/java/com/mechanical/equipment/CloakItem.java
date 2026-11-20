package com.mechanical.equipment;

import com.mechanical.Mechanical;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.level.Level;

/**
 * A cloak for the Cloak slot (tag mechanical:equipment/cloak): worn over the armour, it keeps the stats it had as a
 * chest piece of its {@link ArmorMaterial}: armour ("When worn:", applied by {@link Equipment}), durability (worn down
 * like armour), enchantability and repair material. Its look: the material's humanoid equipment layer over the body and
 * shoulders, plus a cape (textures/entity/cloak/<id>.png) or, for the Glider Cloak, the wings layer (client.EquipmentLayer).
 * Right-click in hand to put it on.
 */
public class CloakItem extends Item {
    private final ArmorMaterial material;
    private final boolean cape;

    /** {@code cape}: hangs a cape down the back (fur cloaks); false for a cloak drawn by its wings layer. */
    public CloakItem(Properties properties, ArmorMaterial material, boolean cape) {
        super(properties);
        this.material = material;
        this.cape = cape;
    }

    /** What humanoidArmor(material, CHESTPLATE) gives, minus being wearable on the chest. */
    public static Properties properties(Properties properties, ArmorMaterial material) {
        Identifier id = Identifier.fromNamespaceAndPath(Mechanical.MODID, "cloak");
        ItemAttributeModifiers.Builder modifiers = ItemAttributeModifiers.builder();
        int defense = material.defense().getOrDefault(ArmorType.CHESTPLATE, 0);
        if (defense > 0) modifiers.add(Attributes.ARMOR, new AttributeModifier(id, defense, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ARMOR);
        if (material.toughness() > 0) modifiers.add(Attributes.ARMOR_TOUGHNESS,
                new AttributeModifier(id, material.toughness(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ARMOR);
        if (material.knockbackResistance() > 0) modifiers.add(Attributes.KNOCKBACK_RESISTANCE,
                new AttributeModifier(id, material.knockbackResistance(), AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ARMOR);
        return properties.durability(ArmorType.CHESTPLATE.getDurability(material.durability()))
                .attributes(modifiers.build())
                .enchantable(material.enchantmentValue())
                .repairable(material.repairIngredient());
    }

    public ResourceKey<EquipmentAsset> asset() {
        return material.assetId();
    }

    public Holder<SoundEvent> equipSound() {
        return material.equipSound();
    }

    public boolean hasCape() {
        return cape;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return Equipment.equipFromHand(player, player.getItemInHand(hand), hand) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }
}
