package com.mechanical;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import com.mechanical.wildlife.FlockBird;
import com.mechanical.wildlife.Wildlife;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A nest of twigs lined with straw and down. Found in the wild where its birds live (scripts/gen_birds2.py) or built from
 * sticks and straw. Everything happens on the block's random tick (about once a minute), so nothing scans every tick:
 * <ul>
 * <li>An empty, unclaimed nest is claimed by the first nesting bird (goose, pheasant, ptarmigan, rock dove, chicken) within
 * {@link #CLAIM_RANGE} blocks.</li>
 * <li>A claimed nest keeps its birds near: flock birds within {@link #HOME_RANGE} roam around it and land by it now and then
 * ({@link FlockBird#setNest}), ground birds that strayed walk back. With one of them about, it lays an egg now and then
 * ({@link #LAY_CHANCE}), up to {@link #MAX_EGGS}.</li>
 * <li>Right-click with an empty hand takes an egg. Each time the nest is emptied the birds grow more uneasy (ROBBED), and
 * they settle down only slowly (a step about every hour, {@link #SETTLE_CHANCE}); emptied {@link #ABANDON_AFTER} times
 * before that, the nest is abandoned. Leaving an egg in it is always safe. An abandoned nest can be claimed again after
 * a day or so (ROBBED counts back down).</li>
 * </ul>
 */
public class BirdNestBlock extends Block {
    public enum Bird implements StringRepresentable {
        NONE(null, null), GOOSE(Wildlife.GOOSE::get, Birds.GOOSE_EGG), PHEASANT(Wildlife.PHEASANT::get, Birds.PHEASANT_EGG),
        PTARMIGAN(Wildlife.PTARMIGAN::get, Birds.PTARMIGAN_EGG), ROCK_DOVE(Wildlife.ROCK_DOVE::get, Birds.PIGEON_EGG),
        CHICKEN(() -> EntityTypes.CHICKEN, () -> Items.EGG),
        // waterfowl (wildlife.waterfowl.WaterfowlFauna)
        MALLARD(com.mechanical.wildlife.waterfowl.WaterfowlFauna.MALLARD::get, com.mechanical.wildlife.waterfowl.WaterfowlFauna.MALLARD_EGG),
        MUTE_SWAN(com.mechanical.wildlife.waterfowl.WaterfowlFauna.MUTE_SWAN::get, com.mechanical.wildlife.waterfowl.WaterfowlFauna.SWAN_EGG),
        GREY_HERON(com.mechanical.wildlife.waterfowl.WaterfowlFauna.GREY_HERON::get, com.mechanical.wildlife.waterfowl.WaterfowlFauna.HERON_EGG),
        COMMON_CRANE(com.mechanical.wildlife.waterfowl.WaterfowlFauna.COMMON_CRANE::get, com.mechanical.wildlife.waterfowl.WaterfowlFauna.CRANE_EGG);

        private final Supplier<EntityType<?>> type;
        private final Supplier<? extends Item> egg;

        Bird(Supplier<EntityType<?>> type, Supplier<? extends Item> egg) {
            this.type = type;
            this.egg = egg;
        }

        public EntityType<?> type() {
            return type == null ? null : type.get();
        }

        public Item egg() {
            return egg.get();
        }

        static Bird of(EntityType<?> type) {
            for (Bird b : values()) if (b != NONE && b.type() == type) return b;
            return NONE;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Bird> BIRD = EnumProperty.create("bird", Bird.class);
    public static final int MAX_EGGS = 3, ABANDON_AFTER = 3;
    public static final IntegerProperty EGGS = IntegerProperty.create("eggs", 0, MAX_EGGS);
    /** How uneasy the birds are (times emptied, settling slowly); on an abandoned (NONE) nest, how long until birds take it again. */
    public static final IntegerProperty ROBBED = IntegerProperty.create("robbed", 0, ABANDON_AFTER - 1);
    /**
     * Random-tick odds (a block gets one about every 68 s): an egg (1 in 6, one every ~7 minutes while a bird is about),
     * uneasy birds settling a step (1 in 48, ~an hour), an abandoned nest calming down a step (1 in 8, two steps ~a day).
     */
    static final int LAY_CHANCE = 6, SETTLE_CHANCE = 48, CALM_CHANCE = 8;
    static final double CLAIM_RANGE = 12, HOME_RANGE = 24, STRAY = 10;
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 4, 14);

    public BirdNestBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BIRD, Bird.NONE).setValue(EGGS, 0).setValue(ROBBED, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BIRD, EGGS, ROBBED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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

    /** The block at {@code pos} is a nest claimed by birds of {@code type}. */
    public static boolean isNestOf(Level level, BlockPos pos, EntityType<?> type) {
        BlockState s = level.getBlockState(pos);
        return s.is(Birds.BIRD_NEST.get()) && s.getValue(BIRD).type() == type;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int eggs = state.getValue(EGGS);
        if (eggs == 0) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            Bird bird = state.getValue(BIRD);
            player.getInventory().placeItemBackInInventory(new ItemStack(bird == Bird.NONE ? Items.EGG : bird.egg()), net.minecraft.util.Prediction.SERVER_ONLY);
            BlockState next = state.setValue(EGGS, eggs - 1);
            if (eggs == 1 && bird != Bird.NONE) {
                int robbed = state.getValue(ROBBED) + 1;
                if (robbed >= ABANDON_AFTER) {
                    next = next.setValue(BIRD, Bird.NONE); // ROBBED stays at its top: it calms down on random ticks
                    player.sendOverlayMessage(Component.translatable("message.mechanical.bird_nest.abandoned"));
                } else {
                    next = next.setValue(ROBBED, robbed);
                    if (robbed == ABANDON_AFTER - 1) player.sendOverlayMessage(Component.translatable("message.mechanical.bird_nest.uneasy"));
                }
            }
            server.setBlock(pos, next, Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.5F, 1.4F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Bird bird = state.getValue(BIRD);
        Vec3 center = Vec3.atBottomCenterOf(pos);
        if (bird == Bird.NONE) {
            if (state.getValue(ROBBED) > 0) {
                if (random.nextInt(CALM_CHANCE) == 0) level.setBlock(pos, state.setValue(ROBBED, state.getValue(ROBBED) - 1), Block.UPDATE_CLIENTS);
                return;
            }
            List<Animal> near = level.getEntitiesOfClass(Animal.class, new AABB(pos).inflate(CLAIM_RANGE),
                    a -> a.isAlive() && !a.isBaby() && Bird.of(a.getType()) != Bird.NONE);
            if (!near.isEmpty()) level.setBlock(pos, state.setValue(BIRD, Bird.of(near.get(0).getType())), Block.UPDATE_CLIENTS);
            return;
        }
        List<Animal> birds = level.getEntitiesOfClass(Animal.class, new AABB(pos).inflate(HOME_RANGE, 12, HOME_RANGE),
                a -> a.isAlive() && !a.isBaby() && a.getType() == bird.type());
        if (birds.isEmpty()) return; // nobody home to lay
        for (Animal a : birds) {
            if (a instanceof FlockBird flock) flock.setNest(pos);
            else if (a.onGround() && a.distanceToSqr(center) > STRAY * STRAY && a.getNavigation().isDone())
                a.getNavigation().moveTo(center.x, center.y, center.z, 1.0); // a ground bird that strayed walks back
        }
        if (state.getValue(ROBBED) > 0 && random.nextInt(SETTLE_CHANCE) == 0) {
            level.setBlock(pos, state = state.setValue(ROBBED, state.getValue(ROBBED) - 1), Block.UPDATE_CLIENTS); // they settle down again
        }
        int eggs = state.getValue(EGGS);
        if (eggs < MAX_EGGS && random.nextInt(LAY_CHANCE) == 0) {
            level.setBlock(pos, state.setValue(EGGS, eggs + 1), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.CHICKEN_EGG, SoundSource.NEUTRAL, 0.6F, 1.1F);
        }
    }
}
