package com.mechanical;

import static com.mechanical.Mechanical.ITEMS;

import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Faster field work (docs/farming.md "Fältredskap"): the Seed Bag (Bronze Age, sows 3x3) and Linen Seed Bag (Iron Age,
 * 5x5) hold seed in a 3x3 inventory and sow one seed per empty farmland; the Bronze Ard tills 3x3 per use, through the
 * hoe's own block transformer (so what a hoe can till, and only with air above). Data: scripts/gen_farm_tools.py,
 * textures: scripts/gen_farm_tools_textures.py.
 */
public final class FarmTools {
    /** What a seed bag takes: anything planted as an item that becomes a crop or a stem. */
    public static final Predicate<ItemStack> SEEDS = s -> s.is(Farming.GRAINS)
            || s.getItem() instanceof BlockItem b && (b.getBlock() instanceof CropBlock || b.getBlock() instanceof StemBlock);
    public static final int BAG_RADIUS = 1, LINEN_BAG_RADIUS = 2, ARD_RADIUS = 1, ARD_DURABILITY = 300;

    public static final DeferredItem<SeedBagItem> SEED_BAG = ITEMS.registerItem("seed_bag", p -> new SeedBagItem(BAG_RADIUS, p), p -> p.stacksTo(1));
    public static final DeferredItem<SeedBagItem> LINEN_SEED_BAG = ITEMS.registerItem("linen_seed_bag", p -> new SeedBagItem(LINEN_BAG_RADIUS, p), p -> p.stacksTo(1));
    public static final Supplier<MenuType<BarkBasketMenu>> SEED_BAG_MENU = Mechanical.MENUS.register("seed_bag",
            () -> new MenuType<>(BarkBasketMenu::seedBag, FeatureFlags.VANILLA_SET));
    /** Heavy and slow to swing: a bronze share on a long beam. */
    public static final DeferredItem<ArdItem> BRONZE_ARD = ITEMS.registerItem("bronze_ard", ArdItem::new,
            p -> p.hoe(Bronze.BRONZE, -1.0F, -3.2F).durability(ARD_DURABILITY));

    public static void init() {}

    /** Seed bag: right-click farmland (or a crop on it) to sow the empty farmland around it; elsewhere, or sneaking, opens it. */
    public static class SeedBagItem extends Item {
        public final int radius;

        public SeedBagItem(int radius, Properties properties) {
            super(properties);
            this.radius = radius;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!level.isClientSide()) {
                ItemStack stack = player.getItemInHand(hand);
                int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().getSelectedSlot() : 40;
                player.openMenu(new SimpleMenuProvider((id, inventory, p) -> BarkBasketMenu.open(SEED_BAG_MENU.get(), SEEDS, id, inventory, slot, stack),
                        stack.getHoverName()));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Player player = ctx.getPlayer();
            if (player != null && player.isSecondaryUseActive()) return InteractionResult.PASS;
            Level level = ctx.getLevel();
            BlockPos center = ctx.getClickedPos();
            if (!(level.getBlockState(center).getBlock() instanceof FarmlandBlock)) center = center.below();
            if (!(level.getBlockState(center).getBlock() instanceof FarmlandBlock)) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            int sown = sow(level, center, ctx.getItemInHand(), radius);
            if (sown == 0 && player != null) player.sendOverlayMessage(Component.translatable("message.mechanical.seed_bag.nothing"));
            return InteractionResult.SUCCESS;
        }
    }

    /** Sows one seed from the bag on each empty farmland within {@code radius} at the level of {@code center}; returns how many. */
    public static int sow(Level level, BlockPos center, ItemStack bag, int radius) {
        NonNullList<ItemStack> items = NonNullList.withSize(BarkBasketMenu.SIZE, ItemStack.EMPTY);
        bag.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        int sown = 0;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, 0, -radius), center.offset(radius, 0, radius))) {
            if (!(level.getBlockState(p).getBlock() instanceof FarmlandBlock) || !level.getBlockState(p.above()).isAir()) continue;
            for (ItemStack seed : items) {
                if (seed.isEmpty() || !(seed.getItem() instanceof BlockItem b)) continue;
                BlockState crop = b.getBlock().defaultBlockState();
                if (!crop.canSurvive(level, p.above())) continue;
                level.setBlock(p.above(), crop, Block.UPDATE_ALL);
                level.gameEvent(GameEvent.BLOCK_PLACE, p.above(), GameEvent.Context.of(crop));
                seed.shrink(1);
                sown++;
                break;
            }
        }
        if (sown > 0) {
            bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
            level.playSound(null, center, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return sown;
    }

    /** Bronze Ard: tills the clicked block like a hoe, then the 8 around it (sneak: just the one); 1 durability per block. */
    public static class ArdItem extends Item {
        public ArdItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            ItemStack stack = ctx.getItemInHand();
            Holder<BlockTransformer> hoe = stack.get(DataComponents.BLOCK_TRANSFORMER);
            if (hoe == null) return InteractionResult.PASS;
            InteractionResult first = hoe.value().transformBlock(ctx, hoe);
            Player player = ctx.getPlayer();
            if (!first.consumesAction() || player == null || player.isSecondaryUseActive()) return first;
            BlockPos center = ctx.getClickedPos();
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-ARD_RADIUS, 0, -ARD_RADIUS), center.offset(ARD_RADIUS, 0, ARD_RADIUS))) {
                if (p.equals(center) || player.getItemInHand(ctx.getHand()) != stack || stack.isEmpty()) continue;
                BlockPos at = p.immutable();
                hoe.value().transformBlock(new UseOnContext(player, ctx.getHand(), new BlockHitResult(Vec3.atCenterOf(at).add(0, 0.5, 0), Direction.UP, at, false)), hoe);
            }
            return first;
        }
    }

    private FarmTools() {}
}
