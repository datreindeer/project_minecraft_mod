package com.mechanical;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Working felled wood by hand, holding right-click with an axe: one heavy chop every {@link #CHOP_TICKS}.
 * - Fallen log: bark comes off (Bark), it is hewn thinner twice, then it becomes a Rough Log (or only sticks if it fails).
 *   A block of a felled tree ({@link FelledLogEntity}) goes through all four stages in one go, with the same drops.
 *   A Bronze Saw (held right-click, see {@link SawItem}) instead cuts the whole log across into 3 Rough Logs (fewer if an
 *   axe already hewed it) plus its bark, in {@link FallenLogBlock#SAW_STROKES} strokes.
 * - Rough Log stood on a stump (right-click the stump with it): split into 2-3 Firewood (or 1 Firewood + sticks if it fails).
 * A better axe needs fewer chops per stage and fails less often.
 */
@EventBusSubscriber(modid = Mechanical.MODID)
public final class TreeChopping {
    /** Ticks between chops: a slow two-handed overhead blow (client/AxeChopClient draws it from this swing's length). */
    public static final int CHOP_TICKS = 16; // a multiple of 4: a held use key repeats every 4 ticks, so the blows come evenly
    private static final Map<Player, Long> LAST_CHOP = new WeakHashMap<>();
    /** Chops done on a block towards its next stage. Lost if the server restarts, which only costs a few chops. */
    private static final Map<Level, Map<BlockPos, Integer>> PROGRESS = new WeakHashMap<>();

    /** Rough Logs a whole (unhewn) fallen log gives when sawn up. */
    public static final int SAWN_LOGS = 3;

    /** Chops per stage: flint axe 3, stone 2, iron 2. */
    public static int chopsNeeded(ItemStack axe) {
        return Math.max(2, (int) Math.ceil(7 / axeSpeed(axe)));
    }

    /** Chance a finished piece comes out whole: flint axe 60 %, stone 75 %, iron 95 %. */
    public static float successChance(ItemStack axe) {
        return Math.min(0.95F, 0.35F + 0.1F * axeSpeed(axe));
    }

    /** Extra firewood per successful split: flint / crude stone 0, stone and copper +1, iron +2, diamond +3. */
    public static int firewoodBonus(ItemStack axe) {
        return Math.max(0, (int) ((axeSpeed(axe) - 2) / 2));
    }

    private static float axeSpeed(ItemStack axe) {
        return Math.max(1, axe.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()));
    }

    @SubscribeEvent
    static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        boolean axe = stack.is(ItemTags.AXES);

        com.mechanical.equipment.WornTools.Found bagSaw;
        if (state.getBlock() instanceof FallenLogBlock && stack.getItem() instanceof SawItem) {
            take(event, InteractionResult.CONSUME);
            SawItem.startSawing(player, event.getHand(), pos);
        } else if (state.getBlock() instanceof FallenLogBlock
                && (bagSaw = com.mechanical.equipment.WornTools.forEmptyHand(player, event.getHand(), s -> s.getItem() instanceof SawItem)) != null) {
            // a saw from a worn bag and an empty hand: a stroke for every stroke-time the button is held on the log
            take(event, InteractionResult.SUCCESS);
            if (level instanceof ServerLevel server) {
                int strokeTicks = ((SawItem) bagSaw.tool().getItem()).strokeTicks;
                com.mechanical.equipment.WornTools.Hold hold = com.mechanical.equipment.WornTools.held(player, pos.immutable(), server.getGameTime());
                int strokes = hold.total() / strokeTicks, before = (hold.total() - hold.delta()) / strokeTicks;
                com.mechanical.equipment.WornTools.Found saw = bagSaw;
                for (int n = before + 1; n <= strokes && level.getBlockState(pos).getBlock() instanceof FallenLogBlock; n++)
                    sawStroke(server, player, pos, level.getBlockState(pos), n % 2 == 1, () -> saw.hurt(1, player));
            }
        } else if (state.getBlock() instanceof FallenLogBlock && axe) {
            take(event, InteractionResult.CONSUME);
            if (level instanceof ServerLevel server && chop(server, player, event.getHand(), pos, state, stack)) finishFallenLog(server, pos, state, stack, player);
        } else if (state.getBlock() instanceof StumpBlock) {
            int log = state.getValue(StumpBlock.LOG);
            String wood = roughLogWood(stack);
            if (log == 0 && wood != null) {
                take(event, InteractionResult.SUCCESS);
                if (!level.isClientSide()) {
                    level.setBlock(pos, state.setValue(StumpBlock.LOG, Mechanical.WOODS.indexOf(wood) + 1), Block.UPDATE_ALL);
                    level.playSound(null, pos, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
                    stack.consume(1, player);
                }
            } else if (log > 0 && axe) {
                take(event, InteractionResult.CONSUME);
                if (level instanceof ServerLevel server && chop(server, player, event.getHand(), pos.above(), state, stack)) splitOnStump(server, pos, state, stack, player);
            } else if (log > 0 && stack.isEmpty()) { // take the log back off
                take(event, InteractionResult.SUCCESS);
                if (!level.isClientSide()) {
                    level.setBlock(pos, state.setValue(StumpBlock.LOG, 0), Block.UPDATE_ALL);
                    player.getInventory().placeItemBackInInventory(new ItemStack(Mechanical.ROUGH_LOGS.get(Mechanical.WOODS.get(log - 1)).get()), net.minecraft.util.Prediction.SERVER_ONLY);
                }
            }
        }
    }

    /** The left button on a Fallen Log with an axe or saw doesn't break it: it chops or saws like the right one (client/AxeChopClient holds the use key). */
    @SubscribeEvent
    static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack held = event.getEntity().getMainHandItem();
        if ((held.is(ItemTags.AXES) || held.getItem() instanceof SawItem) && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof FallenLogBlock)
            event.setCanceled(true);
    }

    private static void take(PlayerInteractEvent.RightClickBlock event, InteractionResult result) {
        event.setCanceled(true);
        event.setCancellationResult(result);
    }

    static String roughLogWood(ItemStack stack) {
        for (var e : Mechanical.ROUGH_LOGS.entrySet()) if (stack.is(e.getValue().get())) return e.getKey();
        return null;
    }

    /** Chops done so far on the stage at {@code at}. */
    static int progress(Level level, BlockPos at) {
        Map<BlockPos, Integer> progress = PROGRESS.get(level);
        return progress == null ? 0 : progress.getOrDefault(at, 0);
    }

    /** One chop (if the cooldown allows). Returns true when this chop completes the stage. {@code at} is where the chopping shows. */
    private static boolean chop(ServerLevel level, Player player, InteractionHand hand, BlockPos at, BlockState state, ItemStack axe) {
        return chop(level, player, hand, at, state, axe, chopsNeeded(axe));
    }

    /** As above with a given number of chops for the stage (the Dugout Blank's stages are longer; see {@link DugoutBlankBlock}). */
    static boolean chop(ServerLevel level, Player player, InteractionHand hand, BlockPos at, BlockState state, ItemStack axe, int needed) {
        return chop(level, player, hand, at, state, needed, () -> axe.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
    }

    /** As above, {@code wear} wearing the tool when the stage completes (a held tool, or a knife from a worn bag). */
    static boolean chop(ServerLevel level, Player player, InteractionHand hand, BlockPos at, BlockState state, int needed, Runnable wear) {
        if (!swing(level, player, hand, at, state)) return false;
        Map<BlockPos, Integer> progress = PROGRESS.computeIfAbsent(level, l -> new HashMap<>());
        int done = progress.merge(at.immutable(), 1, Integer::sum);
        if (done < needed) {
            level.destroyBlockProgress(player.getId(), at, Math.min(9, done * 10 / needed));
            return false;
        }
        progress.remove(at);
        level.destroyBlockProgress(player.getId(), at, -1);
        wear.run();
        return true;
    }

    /** One chop's blow if the cooldown allows: the heavy swing, the sound and chips at {@code at}. No progress is kept; true if it struck. */
    static boolean swing(ServerLevel level, Player player, InteractionHand hand, BlockPos at, BlockState state) {
        long now = level.getGameTime();
        Long last = LAST_CHOP.get(player);
        if (last != null && now - last < CHOP_TICKS) return false;
        LAST_CHOP.put(player, now);

        RandomSource random = level.getRandom();
        // slow, heavy swing
        player.swing(hand, new net.minecraft.world.item.component.SwingAnimation(net.minecraft.world.item.SwingAnimationType.WHACK, CHOP_TICKS), true);
        level.playSound(null, at, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 0.9F, 0.6F + random.nextFloat() * 0.2F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state.getBlock() instanceof StumpBlock ? Blocks.OAK_LOG.defaultBlockState() : state),
                at.getX() + 0.5, at.getY() + 0.6, at.getZ() + 0.5, 6, 0.25, 0.2, 0.25, 0.05);
        return true;
    }

    /** Stripping a standing log or wood block with an axe peels off that wood's bark. */
    @SubscribeEvent
    static void stripBark(net.neoforged.neoforge.event.level.BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated() || !"axe_strip".equals(event.getItemAbility().name())
                || !(event.getLevel() instanceof ServerLevel level)) return;
        String wood = barkWood(event.getState());
        if (wood != null) Block.popResourceFromFace(level, event.getPos(), event.getContext().getClickedFace(), new ItemStack(Mechanical.BARKS.get(wood).get()));
    }

    /** The wood of an unstripped log or wood block ("birch" for minecraft:birch_log), or null. */
    static String barkWood(BlockState state) {
        if (!state.is(net.minecraft.tags.BlockTags.LOGS)) return null;
        String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (path.startsWith("stripped_")) return null;
        String wood = path.endsWith("_log") ? path.substring(0, path.length() - 4) : path.endsWith("_wood") ? path.substring(0, path.length() - 5) : null;
        return wood != null && Mechanical.WOODS.contains(wood) ? wood : null;
    }

    static void finishFallenLog(ServerLevel level, BlockPos pos, BlockState state, ItemStack axe, Player player) {
        int stage = state.getValue(FallenLogBlock.STAGE);
        hewDrops(level, pos, ((FallenLogBlock) state.getBlock()).wood(), stage, axe, player);
        if (stage < FallenLogBlock.LAST_STAGE) {
            level.setBlock(pos, state.setValue(FallenLogBlock.STAGE, stage + 1), Block.UPDATE_ALL);
            return;
        }
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** All four hewing stages of a log at once (a block cut off a {@link FelledLogEntity}): what a Fallen Log gives, stage by stage. */
    static void hewWhole(ServerLevel level, BlockPos pos, String wood, ItemStack axe, Player player) {
        for (int stage = 0; stage <= FallenLogBlock.LAST_STAGE; stage++) hewDrops(level, pos, wood, stage, axe, player);
        level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /** What finishing hewing stage {@code stage} of a {@code wood} log drops: bark, then sticks and maybe a Rough Log, last a Rough Log (or sticks). */
    static void hewDrops(ServerLevel level, BlockPos pos, String wood, int stage, ItemStack axe, Player player) {
        int wood2 = AncientOak.woodMultiplier(player); // Heart of the Oak doubles the wood
        RandomSource random = level.getRandom();
        if (stage == 0) Block.popResource(level, pos, new ItemStack(Mechanical.BARKS.get(wood).get(), 1 + random.nextInt(2)));
        else if (stage < FallenLogBlock.LAST_STAGE) {
            // hewn thinner: chips of sticks, and sometimes a usable piece of rough log
            Block.popResource(level, pos, new ItemStack(Items.STICK, 1 + random.nextInt(2)));
            if (random.nextFloat() < successChance(axe) * 0.5F) Block.popResource(level, pos, new ItemStack(Mechanical.ROUGH_LOGS.get(wood).get(), wood2));
        } else if (random.nextFloat() < successChance(axe)) Block.popResource(level, pos, new ItemStack(Mechanical.ROUGH_LOGS.get(wood).get(), wood2));
        else Block.popResource(level, pos, new ItemStack(Items.STICK, 2 + random.nextInt(2)));
    }

    /**
     * One Bronze Saw stroke across a fallen log (push or pull, see {@link SawItem}): rasp, sawdust from the cut, and the
     * kerf one stroke deeper; the last stroke cuts the log up.
     */
    static void sawStroke(ServerLevel level, Player player, InteractionHand hand, BlockPos pos, BlockState state, ItemStack saw, boolean push) {
        sawStroke(level, player, pos, state, push, () -> saw.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND));
    }

    /** As above, {@code wear} wearing the saw on the last stroke (a held saw, or one in a worn bag). */
    static void sawStroke(ServerLevel level, Player player, BlockPos pos, BlockState state, boolean push, Runnable wear) {
        RandomSource random = level.getRandom();
        int sawn = state.getValue(FallenLogBlock.SAWN) + 1;
        double top = state.getShape(level, pos).max(net.minecraft.core.Direction.Axis.Y);
        double y = pos.getY() + top * (1 - (double) sawn / FallenLogBlock.SAW_STROKES); // the floor of the kerf
        boolean alongX = state.getValue(FallenLogBlock.AXIS) == net.minecraft.core.Direction.Axis.X; // the kerf runs across the log
        level.playSound(null, pos.getX() + 0.5, y, pos.getZ() + 0.5, SoundEvents.AXE_SCRAPE.value(), SoundSource.BLOCKS,
                0.6F, (push ? 1.35F : 1.1F) + random.nextFloat() * 0.1F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + top, pos.getZ() + 0.5,
                3, alongX ? 0.02 : 0.3, 0.02, alongX ? 0.3 : 0.02, 0.02);
        if (sawn < FallenLogBlock.SAW_STROKES) {
            level.setBlock(pos, state.setValue(FallenLogBlock.SAWN, sawn), Block.UPDATE_CLIENTS);
            return;
        }
        sawFallenLog(level, pos, state, player);
        wear.run();
        player.stopUsingItem();
    }

    /** A saw cuts the whole fallen log at once, with no failures: its bark, and a Rough Log for every piece not yet hewn away. */
    private static void sawFallenLog(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 1.1F);
        sawDrops(level, pos, ((FallenLogBlock) state.getBlock()).wood(), state.getValue(FallenLogBlock.STAGE), player);
    }

    /** What sawing up a {@code wood} log hewn to {@code stage} drops (also a block sawn off a {@link FelledLogEntity}, stage 0). */
    static void sawDrops(ServerLevel level, BlockPos pos, String wood, int stage, Player player) {
        if (stage == 0) Block.popResource(level, pos, new ItemStack(Mechanical.BARKS.get(wood).get(), 1 + level.getRandom().nextInt(2)));
        int logs = Math.max(1, SAWN_LOGS - Math.max(0, stage - 1));
        Block.popResource(level, pos, new ItemStack(Mechanical.ROUGH_LOGS.get(wood).get(), logs * AncientOak.woodMultiplier(player)));
    }

    private static void splitOnStump(ServerLevel level, BlockPos pos, BlockState state, ItemStack axe, Player player) {
        int wood2 = AncientOak.woodMultiplier(player); // Heart of the Oak doubles the wood
        RandomSource random = level.getRandom();
        String wood = Mechanical.WOODS.get(state.getValue(StumpBlock.LOG) - 1);
        level.setBlock(pos, state.setValue(StumpBlock.LOG, 0), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        var firewood = Mechanical.FIREWOOD.get(wood).get();
        if (random.nextFloat() < successChance(axe)) {
            Block.popResource(level, pos.above(), new ItemStack(firewood, (2 + random.nextInt(2) + firewoodBonus(axe)) * wood2));
        } else {
            Block.popResource(level, pos.above(), new ItemStack(firewood, wood2));
            Block.popResource(level, pos.above(), new ItemStack(Items.STICK, 1 + random.nextInt(2)));
        }
    }

    private TreeChopping() {}
}
