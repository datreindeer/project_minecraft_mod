package com.mechanical.client;

import org.jspecify.annotations.Nullable;

import com.mechanical.Glider;
import com.mechanical.LeatherSatchelItem;
import com.mechanical.Mechanical;
import com.mechanical.equipment.Accessory;
import com.mechanical.equipment.CloakItem;
import com.mechanical.equipment.Equipment;
import com.mechanical.equipment.EquipmentInventory;
import com.mechanical.equipment.OpenPackPayload;
import com.mechanical.equipment.Quiver;
import com.mechanical.equipment.QuiverItem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.renderstate.AvatarRenderStateModifier;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Client side of the accessory slots ({@link Equipment}): the B key (open the worn satchel and quiver, also from the
 * inventory screen), the worn items on every player's model ({@link EquipmentLayer}, fed by a render-state modifier),
 * and a tooltip line telling which slot an item is worn in.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class EquipmentClient {
    public static final KeyMapping OPEN_PACK = new KeyMapping("key.mechanical.open_pack", com.mojang.blaze3d.platform.InputConstants.KEY_B, KeyMapping.Category.INVENTORY);
    /** A cloak's humanoid layer: the chest-armour mesh, a size larger so it lies over real chest armour. */
    static final ModelLayerLocation CLOAK_BODY = new ModelLayerLocation(Identifier.fromNamespaceAndPath(Mechanical.MODID, "cloak"), "main");

    private static final ContextKey<ItemStack> CLOAK = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "worn_cloak"));
    private static final ContextKey<Boolean> SATCHEL = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "worn_satchel"));
    private static final ContextKey<Boolean> POUCH = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "worn_pouch"));
    private static final ContextKey<Integer> QUIVER =new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "worn_quiver"));
    private static final ContextKey<Boolean> MINER_SACK = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "miner_sack"));
    private static final ContextKey<Identifier> TRIBE_GEAR = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "tribe_gear"));
    private static final ContextKey<Boolean> ARMORED = new ContextKey<>(Identifier.fromNamespaceAndPath(Mechanical.MODID, "chest_armored"));

    // ---------------------------------------------------------------- B

    @EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
    static final class ModBus {
        @SubscribeEvent
        static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(OPEN_PACK);
        }

        @SubscribeEvent
        static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(CLOAK_BODY, () -> LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1.15F), 0.0F), 64, 32));
            event.registerLayerDefinition(WornSatchelModel.LAYER, WornSatchelModel::createLayer);
            event.registerLayerDefinition(WornQuiverModel.LAYER, WornQuiverModel::createLayer);
            event.registerLayerDefinition(WornPouchModel.LAYER, WornPouchModel::createLayer);
            event.registerLayerDefinition(MinerSackModel.LAYER, MinerSackModel::createLayer);
            event.registerLayerDefinition(TribeGearModel.LAYER, TribeGearModel::createLayer);
        }

        @SubscribeEvent
        static void addLayers(EntityRenderersEvent.AddLayers event) {
            for (var skin : event.getSkins()) {
                AvatarRenderer<?> renderer = event.getPlayerRenderer(skin);
                if (renderer != null) renderer.addLayer(new EquipmentLayer(renderer, event.getEntityModels(), event.getContext().getEquipmentRenderer()));
            }
        }

        @SubscribeEvent
        static void renderStateModifiers(RegisterRenderStateModifiersEvent event) {
            event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
                @Override
                public <T extends Avatar & ClientAvatarEntity> void accept(T avatar, AvatarRenderState state) {
                    if (avatar instanceof Player player) extract(player, state);
                }
            });
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN_PACK.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null) ClientPacketDistributor.sendToServer(new OpenPackPayload());
        }
    }

    /** B in the inventory screen switches to the worn satchel and quiver. */
    @SubscribeEvent
    static void onScreenKey(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() instanceof InventoryScreen && OPEN_PACK.matches(event.getKeyEvent()) && Minecraft.getInstance().player != null) {
            ClientPacketDistributor.sendToServer(new OpenPackPayload());
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- the worn items on the model

    private static void extract(Player player, AvatarRenderState state) {
        EquipmentInventory worn = Equipment.of(player);
        ItemStack cloak = worn.get(Accessory.CLOAK);
        if (cloak.getItem() instanceof CloakItem) {
            state.setRenderData(CLOAK, cloak);
            state.showCape = false; // the skin's own cape would hang through it
            if (!cloak.isEmpty() && Boolean.TRUE.equals(player.getData(Glider.GLIDING))) {
                // the glider cloak spread wide
                state.elytraRotX = 0.35F;
                state.elytraRotY = 0.0F;
                state.elytraRotZ = -1.45F;
            }
        }
        if (worn.get(Accessory.BACK).getItem() instanceof LeatherSatchelItem) state.setRenderData(SATCHEL, true);
        ItemStack quiver = worn.get(Accessory.HIP);
        if (quiver.getItem() instanceof QuiverItem) state.setRenderData(QUIVER, Quiver.filledSlots(quiver));
        if (quiver.getItem() instanceof com.mechanical.LeatherPouchItem) state.setRenderData(POUCH, true);
        state.setRenderData(ARMORED, isChestArmor(state.chestEquipment));
    }

    /** Armour drawn on the chest (not an elytra-like glider). */
    private static boolean isChestArmor(ItemStack chest) {
        Equippable equippable = chest.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST && equippable.assetId().isPresent() && !chest.has(DataComponents.GLIDER);
    }

    static @Nullable ItemStack cloak(AvatarRenderState state) {
        return state.getRenderData(CLOAK);
    }

    static boolean satchel(AvatarRenderState state) {
        return state.getRenderData(SATCHEL) != null;
    }

    static boolean pouch(AvatarRenderState state) {
        return state.getRenderData(POUCH) != null;
    }

    /** Quiver slots holding arrows, or -1 without a quiver. */
    static int quiverArrows(AvatarRenderState state) {
        Integer n = state.getRenderData(QUIVER);
        return n == null ? -1 : n;
    }

    /** The Miner's big ore sack on his back (people.TribespersonRenderer sets it). */
    public static void wearMinerSack(AvatarRenderState state) {
        state.setRenderData(MINER_SACK, true);
    }

    static boolean minerSack(AvatarRenderState state) {
        return state.getRenderData(MINER_SACK) != null;
    }

    /** A tribesperson's gear (TribeGearModel) with this texture (people.TribeIdlePoses sets it). */
    public static void wearTribeGear(AvatarRenderState state, Identifier texture) {
        state.setRenderData(TRIBE_GEAR, texture);
    }

    static Identifier tribeGear(AvatarRenderState state) {
        return state.getRenderData(TRIBE_GEAR);
    }

    static boolean chestArmored(AvatarRenderState state) {
        return Boolean.TRUE.equals(state.getRenderData(ARMORED));
    }

    // ---------------------------------------------------------------- tooltip

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        for (Accessory slot : Accessory.values()) {
            if (slot == Accessory.RING_2 || !Equipment.fits(slot, stack)) continue;
            event.getToolTip().add(1, Component.translatable("tooltip.mechanical.worn_in",
                    Component.translatable("slot.mechanical." + slot.tag.location().getPath().substring("equipment/".length()))).withStyle(ChatFormatting.GRAY));
            return;
        }
    }

    private EquipmentClient() {}
}
