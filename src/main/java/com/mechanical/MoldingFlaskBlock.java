package com.mechanical;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Molding Flask (docs/steel-furnace-plan.md section 6, sand casting): an iron frame in two halves (cope and drag).
 * Right-click it with 4 Casting Sand to ram it full, then with a Wooden Pattern to press the shape in ({@link FlaskShape}):
 * the flask closes with a pouring hole on top. A spout or trough fills it like a mold ({@link Sink}); once set
 * ({@link #setTicks}, half on a Foundry Table) a pickaxe or hammer knocks it out: the casting, finished (no grinding),
 * and 3 Burnt Casting Sand fall out, and the empty frame stays. Tool heads take two thirds of a Clay Mold's metal.
 */
public class MoldingFlaskBlock extends Block implements EntityBlock {
    public enum Stage implements StringRepresentable {
        EMPTY, SAND, READY, MOLTEN, SET;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 12, 15);

    /** What a pattern presses into the sand: the cast it takes (mB) and what it gives of a melt (null: that melt can't). */
    public enum FlaskShape {
        PICKAXE_HEAD(ClayMoldBlock.Shape.PICKAXE_HEAD), AXE_HEAD(ClayMoldBlock.Shape.AXE_HEAD), SHOVEL_HEAD(ClayMoldBlock.Shape.SHOVEL_HEAD),
        HOE_HEAD(ClayMoldBlock.Shape.HOE_HEAD), KNIFE_BLADE(ClayMoldBlock.Shape.KNIFE_BLADE), SAW_BLADE(ClayMoldBlock.Shape.SAW_BLADE),
        SPEAR_HEAD(ClayMoldBlock.Shape.SPEAR_HEAD), SWORD_BLADE(ClayMoldBlock.Shape.SWORD_BLADE), HAMMER_HEAD(ClayMoldBlock.Shape.HAMMER_HEAD),
        PLATE(ClayMoldBlock.Shape.PLATE),
        INGOT_MOLD(1000), PLATE_MOLD(1000), NUGGET_MOLD(600), GEAR(200), POT(800);

        public final String id;
        public final ClayMoldBlock.@Nullable Shape clay;
        public final int mb;

        FlaskShape(ClayMoldBlock.Shape clay) {
            this.id = name().toLowerCase(Locale.ROOT);
            this.clay = clay;
            this.mb = clay.units * Molten.UNIT * 2 / 3; // fine sand: thinner walls than clay
        }

        FlaskShape(int mb) {
            this.id = name().toLowerCase(Locale.ROOT);
            this.clay = null;
            this.mb = mb;
        }

        public static @Nullable FlaskShape byId(String id) {
            for (FlaskShape s : values()) if (s.id.equals(id)) return s;
            return null;
        }

        /** The casting: a finished head of bronze or copper (the rough cast's finished item), or the cast-iron pieces. */
        public @Nullable ItemStack result(Molten m) {
            if (clay != null) {
                if (m.metal == null || !m.metal.casts(clay)) return null;
                ItemStack rough = m.metal.cast(clay);
                Identifier id = BuiltInRegistries.ITEM.getKey(rough.getItem());
                String done = id.getPath().replace("rough_cast_", "");
                Item item = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath(id.getNamespace(), done)).orElse(rough.getItem());
                ItemStack out = new ItemStack(item, rough.getCount());
                if (m == Molten.POOR_BRONZE) Bronze.markPoor(out);
                return out;
            }
            if (m != Molten.PIG_IRON) return null;
            return switch (this) {
                case INGOT_MOLD -> new ItemStack(HighFurnace.IRON_INGOT_MOLD_ITEM.get());
                case PLATE_MOLD -> new ItemStack(HighFurnace.IRON_PLATE_MOLD_ITEM.get());
                case NUGGET_MOLD -> new ItemStack(HighFurnace.IRON_NUGGET_MOLD_ITEM.get());
                case GEAR -> new ItemStack(HighFurnace.CAST_IRON_GEAR.get());
                case POT -> new ItemStack(HighFurnace.CAST_IRON_POT.get());
                default -> null;
            };
        }
    }

    public MoldingFlaskBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, Stage.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public static int setTicks(Level level, BlockPos pos, int mb) {
        return FoundryTableBlock.setTicks(level, pos, 100 + mb / 10); // 5 s small, about 15 s for the biggest
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof Entity f)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        Stage stage = state.getValue(STAGE);
        if (stage == Stage.EMPTY && stack.is(HighFurnace.CASTING_SAND_ITEM.get())) {
            if (stack.getCount() < 4) {
                if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.mechanical.molding_flask.sand"));
                return InteractionResult.FAIL;
            }
            if (level instanceof ServerLevel server) {
                if (!player.getAbilities().instabuild) stack.shrink(4);
                server.setBlock(pos, state.setValue(STAGE, Stage.SAND), Block.UPDATE_ALL);
                server.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            return InteractionResult.SUCCESS;
        }
        FlaskShape shape = patternShape(stack);
        if (stage == Stage.SAND && shape != null) {
            if (level instanceof ServerLevel server) {
                f.shape = shape;
                f.setChanged();
                stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                server.setBlock(pos, state.setValue(STAGE, Stage.READY), Block.UPDATE_ALL);
                server.playSound(null, pos, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
            }
            return InteractionResult.SUCCESS;
        }
        if (stage == Stage.SET && (stack.is(ItemTags.PICKAXES) || stack.getItem() instanceof net.minecraft.world.item.Item i && BuiltInRegistries.ITEM.getKey(i).getPath().contains("hammer"))) {
            if (level instanceof ServerLevel server) knockOut(server, pos, state, f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof Entity f) {
            Stage s = state.getValue(STAGE);
            if (s == Stage.MOLTEN && f.molten != null && server.getGameTime() >= f.setAt) {
                server.setBlock(pos, state.setValue(STAGE, Stage.SET), Block.UPDATE_ALL);
                s = Stage.SET;
            }
            player.sendOverlayMessage(Component.translatable("message.mechanical.molding_flask." + s.getSerializedName(),
                    f.shape == null ? "-" : Component.translatable("pattern.mechanical.flask." + f.shape.id)));
        }
        return InteractionResult.SUCCESS;
    }

    /** The shape a Wooden Pattern presses, or null. */
    static @Nullable FlaskShape patternShape(ItemStack stack) {
        for (var e : HighFurnace.PATTERNS.entrySet()) if (stack.is(e.getValue().get())) return e.getKey();
        return null;
    }

    static void knockOut(ServerLevel level, BlockPos pos, BlockState state, Entity f) {
        ItemStack out = f.shape == null || f.molten == null ? null : f.shape.result(f.molten);
        if (out != null) Block.popResource(level, pos.above(), out);
        Block.popResource(level, pos.above(), new ItemStack(HighFurnace.BURNT_CASTING_SAND_ITEM.get(), 3));
        f.shape = null;
        f.molten = null;
        f.setChanged();
        level.setBlock(pos, state.setValue(STAGE, Stage.EMPTY), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.4F, 1.4F);
        level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.getValue(STAGE) == Stage.MOLTEN) level.setBlock(pos, state.setValue(STAGE, Stage.SET), Block.UPDATE_ALL);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    /** The pattern pressed into the sand, the melt poured in and when it has set. */
    public static class Entity extends BlockEntity {
        @Nullable FlaskShape shape;
        @Nullable Molten molten;
        long setAt;
        /** While the stream runs: how much is in (the sprue shows it glowing). */
        int filled;

        public Entity(BlockPos pos, BlockState state) {
            super(HighFurnace.MOLDING_FLASK_ENTITY.get(), pos, state);
        }

        public @Nullable Molten molten() {
            return molten;
        }

        public int filled() {
            return filled;
        }

        public @Nullable FlaskShape shape() {
            return shape;
        }

        void sync() {
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            shape = FlaskShape.byId(input.getStringOr("shape", ""));
            molten = Molten.byId(input.getStringOr("molten", ""));
            setAt = input.getLongOr("set_at", 0);
            filled = input.getIntOr("filled", 0);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            if (shape != null) output.putString("shape", shape.id);
            if (molten != null) output.putString("molten", molten.id);
            output.putLong("set_at", setAt);
            output.putInt("filled", filled);
        }

        @Override
        public Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            return saveCustomOnly(registries);
        }
    }

    /** As a sink: a ready flask takes a whole cast of what its pattern can be cast in, through its pouring hole. */
    static final class Sink implements PouringSpoutBlockEntity.Sink {
        @Override
        public int need(ServerLevel level, BlockPos at, Molten m) {
            BlockState s = level.getBlockState(at);
            if (!s.is(HighFurnace.MOLDING_FLASK.get()) || s.getValue(STAGE) != Stage.READY || !(level.getBlockEntity(at) instanceof Entity f) || f.shape == null) return 0;
            return f.shape.result(m) != null ? f.shape.mb : 0;
        }

        @Override
        public void fill(ServerLevel level, BlockPos at, Molten m, int mb) {
            if (!(level.getBlockEntity(at) instanceof Entity f)) return;
            f.molten = m;
            f.filled = 0;
            f.setAt = level.getGameTime() + setTicks(level, at, mb);
            f.sync();
            BlockState s = level.getBlockState(at);
            level.setBlock(at, s.setValue(STAGE, Stage.MOLTEN), Block.UPDATE_ALL);
            level.scheduleTick(at, s.getBlock(), (int) (f.setAt - level.getGameTime()));
        }

        @Override
        public void progress(ServerLevel level, BlockPos at, Molten m, int poured, int need) {
            if (level.getBlockEntity(at) instanceof Entity f) {
                f.filled = poured;
                f.molten = m;
                f.sync();
            }
        }

        @Override
        public void abort(ServerLevel level, BlockPos at) {
            if (level.getBlockEntity(at) instanceof Entity f && level.getBlockState(at).getValue(STAGE) == Stage.READY) {
                f.filled = 0;
                f.molten = null;
                f.sync();
            }
        }

        /** The pouring hole in the middle of the top. */
        @Override
        public net.minecraft.world.phys.Vec3 target(ServerLevel level, BlockPos at, int poured, int need) {
            return new net.minecraft.world.phys.Vec3(0.5, 12.05 / 16, 0.5);
        }
    }
}
