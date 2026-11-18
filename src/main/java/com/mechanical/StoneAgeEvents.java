package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.CampfireBlock;

/** Stone age rules that can't be expressed as data: logs need an axe and drop firewood, extra sticks and fibre. */
@EventBusSubscriber(modid = Mechanical.MODID)
public class StoneAgeEvents {
    private static final float LEAF_STICK_CHANCE = 0.08F; // on top of vanilla's ~2%
    private static final float GRASS_FIBRE_CHANCE = 0.2F;
    private static final float GRASS_FIBRE_CHANCE_KNIFE = 0.6F;
    private static final int FIREWOOD_PER_LOG = 2;

    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getState().is(BlockTags.LOGS) && !event.getEntity().getMainHandItem().is(ItemTags.AXES)) {
            event.setCanceled(true);
        }
    }

    // Campfires are placed unlit (light them with a fire drill). A default block_state component on the item applies
    // lit=false on both client and server, so there is no lit-then-unlit flicker.
    @SubscribeEvent
    static void unlitCampfires(ModifyDefaultComponentsEvent event) {
        event.modify(Items.CAMPFIRE, (components, context, item) ->
                components.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(CampfireBlock.LIT, false)));
    }

    /**
     * Stone Axe (Tool Station, double head + cordage + tool rod): a clear step up in durability from the Crude Stone Axe
     * (80), but no great weapon: 6.5 damage (vanilla 9, crude 5.5), 250 durability (vanilla 131).
     */
    @SubscribeEvent
    static void stoneAxe(ModifyDefaultComponentsEvent event) {
        event.modify(Items.STONE_AXE, (components, context, item) -> {
            components.set(DataComponents.MAX_DAMAGE, 250);
            components.set(DataComponents.ATTRIBUTE_MODIFIERS, net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                    .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            net.minecraft.world.item.Item.BASE_ATTACK_DAMAGE_ID, 5.5, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                            net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                    .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED, new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                            net.minecraft.world.item.Item.BASE_ATTACK_SPEED_ID, -3.2, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                            net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                    .build());
        });
    }

    @SubscribeEvent
    static void onBlockDrops(BlockDropsEvent event) {
        BlockState state = event.getState();
        if (state.is(BlockTags.LOGS)) {
            logsToFirewood(event, state);
        } else if (state.is(BlockTags.LEAVES)) {
            maybeDrop(event, Items.STICK, LEAF_STICK_CHANCE);
        } else if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN)) {
            boolean knife = event.getTool().getItem() instanceof KnifeItem;
            maybeDrop(event, Mechanical.PLANT_FIBRE, knife ? GRASS_FIBRE_CHANCE_KNIFE : GRASS_FIBRE_CHANCE);
            // cutting fibre wears the knife (vanilla doesn't damage tools on instant-break blocks)
            if (event.getBreaker() instanceof Player player && player.getMainHandItem().getItem() instanceof KnifeItem) {
                player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
        }
    }

    /** A log (or stripped log / wood block) drops 2 firewood of its wood type instead of itself. */
    private static void logsToFirewood(BlockDropsEvent event, BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        String wood = path.replace("stripped_", "").replace("_log", "").replace("_wood", "");
        var firewood = Mechanical.FIREWOOD.get(wood);
        if (firewood == null) return; // e.g. nether stems, bamboo: keep vanilla drops
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.is(state.getBlock().asItem())) drop.setItem(new ItemStack(firewood.get(), stack.getCount() * FIREWOOD_PER_LOG));
        }
    }

    private static void maybeDrop(BlockDropsEvent event, ItemLike item, float chance) {
        ServerLevel level = event.getLevel();
        if (level.getRandom().nextFloat() >= chance) return;
        BlockPos pos = event.getPos();
        event.getDrops().add(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(item)));
    }

    /** The fletching table opens its GUI (arrows and bow upgrades, see FletchingRecipes). */
    @SubscribeEvent
    static void openFletchingTable(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isSecondaryUseActive() || !event.getLevel().getBlockState(event.getPos()).is(Blocks.FLETCHING_TABLE)) return;
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if (!event.getLevel().isClientSide()) {
            var access = net.minecraft.world.inventory.ContainerLevelAccess.create(event.getLevel(), event.getPos());
            event.getEntity().openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, p) -> new FletchingMenu(id, inventory, access),
                    net.minecraft.network.chat.Component.translatable("container.mechanical.fletching_table")));
        }
    }

    /** Gutting a whole fish (whole fish + knife) also gives Fish Bones; peeling a boiled egg gives an Eggshell. */
    @SubscribeEvent
    static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        for (int i = 0; i < event.getInventory().getContainerSize(); i++) {
            if (event.getInventory().getItem(i).is(Mechanical.WHOLE_FISH)) {
                event.getEntity().getInventory().placeItemBackInInventory(new ItemStack(Mechanical.FISH_BONES.get()), net.minecraft.util.Prediction.SERVER_ONLY);
                return;
            }
            if (event.getInventory().getItem(i).is(Mechanical.BOILED_EGG.get())) { // peeling also gives the shell
                event.getEntity().getInventory().placeItemBackInInventory(new ItemStack(Mechanical.EGGSHELL.get()), net.minecraft.util.Prediction.SERVER_ONLY);
                return;
            }
        }
    }
}
