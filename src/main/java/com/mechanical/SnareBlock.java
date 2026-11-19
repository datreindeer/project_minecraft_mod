package com.mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A twine noose on a stake, set low over the ground. A small animal ({@link Traps#isSmallGame}) that walks into a set
 * snare is caught most of the time: it dies there and leaves a normal Carcass. Anything else that blunders through
 * (a player, a deer) just springs it; a player trips (Slowness II, 2 s) unless sneaking. Right-click a sprung snare to
 * set it again; right-click a set one with bait (#mechanical:snare_bait) to bait it.
 * Every {@link #CHECK} ticks a set snare may lure a small animal nearby (Traps.LureGoal): baited ones draw from
 * {@link #BAIT_RANGE} blocks at {@link #BAIT_CHANCE}, plain ones from {@link #RANGE} blocks at {@link #PLAIN_CHANCE},
 * doubled on a trail or by berry bushes and x2.5 from dusk to dawn.
 */
public class SnareBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty SPRUNG = BooleanProperty.create("sprung");
    public static final BooleanProperty BAITED = BooleanProperty.create("baited");
    static final int CHECK = 200, RANGE = 6, BAIT_RANGE = 8;
    static final float PLAIN_CHANCE = 0.04F, BAIT_CHANCE = 0.5F, GOOD_SPOT = 2.0F, NIGHT = 2.5F;
    /** A small animal walking in is caught at this chance (else it slips the noose and the snare is sprung empty). */
    static final float CATCH_CHANCE = 0.8F;
    /** Chance the twine snaps on a catch: the snare is gone (a stick is left). */
    static final float BREAK_CHANCE = 0.15F;
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 8, 14), SPRUNG_SHAPE = box(2, 0, 2, 14, 3, 14);

    public SnareBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SPRUNG, false).setValue(BAITED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SPRUNG, BAITED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(SPRUNG) ? SPRUNG_SHAPE : SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return direction == Direction.DOWN && !canSurvive(state, level, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!state.getValue(SPRUNG) && !level.isClientSide()) level.scheduleTick(pos, this, CHECK);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(SPRUNG) || state.getValue(BAITED) || !stack.is(Traps.SNARE_BAIT)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            stack.consume(1, player);
            level.setBlock(pos, state.setValue(BAITED, true), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PLACE, SoundSource.BLOCKS, 0.8F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(SPRUNG)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(SPRUNG, false).setValue(BAITED, false), Block.UPDATE_ALL); // onPlace restarts the checks
            level.playSound(null, pos, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    // ---------------------------------------------------------------- luring

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(SPRUNG)) return;
        level.scheduleTick(pos, this, CHECK);
        boolean baited = state.getValue(BAITED);
        float chance = baited ? BAIT_CHANCE : PLAIN_CHANCE * (goodSpot(level, pos) ? GOOD_SPOT : 1) * (twilightOrNight(level) ? NIGHT : 1);
        if (random.nextFloat() >= chance) return;
        int r = baited ? BAIT_RANGE : RANGE;
        // a random one in range, so a sleeping hedgehog next to the snare doesn't block the rest
        var near = level.getEntitiesOfClass(Animal.class, new AABB(pos).inflate(r, 3, r),
                a -> Traps.isSmallGame(a) && !Traps.isLured(a) && !AnimalFear.isPanicking(a));
        if (!near.isEmpty()) Traps.lure(near.get(random.nextInt(near.size())), pos);
    }

    /** On a trail (path or coarse dirt) or within 3 blocks of berry bushes, where small animals go anyway. */
    static boolean goodSpot(LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(Blocks.DIRT_PATH) || below.getBlock() instanceof TrampledPathBlock || below.is(Blocks.COARSE_DIRT) || Foraging.nearBerryBush(level, pos, 3)
                || BlockPos.betweenClosedStream(pos.offset(-2, -1, -2), pos.offset(2, 1, 2)).anyMatch(p -> level.getBlockState(p).is(Blocks.SWEET_BERRY_BUSH));
    }

    /** From dusk (12000) to just after dawn (1000), when small game is about. */
    static boolean twilightOrNight(ServerLevel level) {
        long t = level.getOverworldClockTime() % 24000;
        return t >= 12000 || t < 1000;
    }

    // ---------------------------------------------------------------- springing

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!(level instanceof ServerLevel server) || state.getValue(SPRUNG) || !(entity instanceof LivingEntity living)) return;
        if (living instanceof Player player) {
            if (player.isSteppingCarefully() || player.isSpectator()) return; // stepped over it
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
            spring(server, pos, state, false);
            return;
        }
        if (!Traps.isSmallGame(living)) {
            // too big to hold: tramples the noose, unless it is a bird or something tiny that slips by
            if (Math.max(living.getBbWidth(), living.getBbHeight()) > Traps.SMALL_GAME) spring(server, pos, state, false);
            return;
        }
        Animal animal = (Animal) living;
        Traps.forget(animal);
        boolean caught = server.getRandom().nextFloat() < CATCH_CHANCE;
        if (caught) {
            animal.kill(server); // dies in the noose and leaves a carcass (Butchery)
        } else {
            AnimalFear.panic(animal, net.minecraft.world.phys.Vec3.atCenterOf(pos));
        }
        spring(server, pos, state, caught);
    }

    private static void spring(ServerLevel level, BlockPos pos, BlockState state, boolean caught) {
        level.playSound(null, pos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 1.0F, 0.6F);
        if (caught && level.getRandom().nextFloat() < BREAK_CHANCE) {
            level.playSound(null, pos, SoundEvents.TRIPWIRE_DETACH, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.removeBlock(pos, false);
            popResource(level, pos, new ItemStack(Items.STICK));
            return;
        }
        level.setBlock(pos, state.setValue(SPRUNG, true).setValue(BAITED, false), Block.UPDATE_ALL);
    }
}
