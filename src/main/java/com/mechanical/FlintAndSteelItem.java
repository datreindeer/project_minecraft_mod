package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

/**
 * The Fire Drill's steel successor (it replaces vanilla's Flint and Steel, whose recipes now make this): lights everything the
 * drill lights (campfires, Fire Pits, kilns, furnace taps, bloomeries, hearths, charcoal clamps, candles) every time and
 * lasts far longer. Everything else does what vanilla's does: fire on a block face, nether portals, TNT, creepers (item
 * tag), dispensers. A use is a {@link #STRIKE_TICKS}-tick strike (own pose, HoldPoses.poseStrike / FirstPersonHolds); the
 * flint meets the steel at {@link #IMPACT} of it, where the clients throw the sparks and play the scrape (FlintSparks).
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public class FlintAndSteelItem extends FireDrillItem {
    public static final int DURABILITY = 500;
    /** Length of one strike (the item's interact swing); a held use key repeats it as fast as the swing allows. */
    public static final int STRIKE_TICKS = 10;
    /** Where in the strike (0..1) the flint scrapes the steel. */
    public static final float IMPACT = 0.45F;

    public FlintAndSteelItem(Properties properties) {
        super(properties);
    }

    @Override
    public float chance(boolean tinder) {
        return 1.0F;
    }

    @Override
    protected float candleChance() {
        return 1.0F;
    }

    @Override
    public boolean canPerformAction(net.minecraft.world.item.ItemInstance stack, ItemAbility ability) {
        return ItemAbilities.DEFAULT_FLINT_ACTIONS.contains(ability);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        // TNT only listens for vanilla's item, so light it here as TntBlock does
        if (state.getBlock() instanceof TntBlock) {
            if (!level.isClientSide() && player != null) {
                if (state.onCaughtFire(level, pos, context.getClickedFace(), player, context.getItemInHand())) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
                    context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
                    player.awardStat(Stats.ITEM_USED.get(this));
                }
            }
            return InteractionResult.SUCCESS;
        }
        boolean wasLit = lit(state);
        InteractionResult result = super.useOn(context);
        if (result != InteractionResult.PASS) {
            if (level instanceof ServerLevel server && !wasLit && lit(level.getBlockState(pos))) embers(server, pos, context.getClickedFace());
            return result;
        }
        // not one of the drill's fires: a lit one is left alone (no fire set on top of it), anything else as vanilla's
        if (firePlace(state)) return InteractionResult.PASS;
        return Items.FLINT_AND_STEEL.useOn(context);
    }

    /** One of the fires the drill lights (lit or not). */
    private static boolean firePlace(BlockState state) {
        return state.getBlock() instanceof CampfireBlock || state.getBlock() instanceof KilnBlock || state.getBlock() instanceof FurnaceTapBlock
                || state.getBlock() instanceof FirePitBlock || state.getBlock() instanceof BloomeryHearthBlock
                || state.getBlock() instanceof SmithingHearthBlock || CharcoalClamp.isPile(state);
    }

    private static boolean lit(BlockState state) {
        return state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT);
    }

    /** The tinder catching: a few sparks lifting off the fire and a soft whoosh. */
    private static void embers(ServerLevel server, BlockPos pos, Direction face) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        for (int i = 0; i < 6; i++) {
            server.sendParticles(Mechanical.FLINT_SPARK.get(), x + (server.getRandom().nextDouble() - 0.5) * 0.4, y, z + (server.getRandom().nextDouble() - 0.5) * 0.4,
                    0, (server.getRandom().nextDouble() - 0.5) * 0.06, 0.12 + server.getRandom().nextDouble() * 0.1, (server.getRandom().nextDouble() - 0.5) * 0.06, 1.0);
        }
        server.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.8F, 1.3F);
    }

    /** Dispensers use it as vanilla's (fire in front, TNT, portals, campfires and candles). */
    @SubscribeEvent
    static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> DispenserBlock.registerBehavior(Mechanical.FLINT_AND_STEEL.get(), DispenserBlock.DISPENSER_REGISTRY.get(Items.FLINT_AND_STEEL)));
    }
}
