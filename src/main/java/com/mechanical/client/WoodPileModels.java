package com.mechanical.client;

import java.util.List;

import com.mechanical.FirewoodPileBlock;
import com.mechanical.Mechanical;
import com.mechanical.RoughLogPileBlock;
import com.mechanical.WoodPileEntity;
import com.mojang.math.OctahedralGroup;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.DelegateBlockStateModel;
import net.neoforged.neoforge.client.model.standalone.SimpleUnbakedStandaloneModel;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

/**
 * Mixed wood piles, each piece in its own wood: the woods come from the {@link WoodPileEntity} as model data, and every
 * piece is a standalone model part (block/&lt;wood&gt;_firewood_pile_piece_&lt;layer&gt;, block/&lt;wood&gt;_rough_log_piece_&lt;log&gt;,
 * turned y:90 for axis x like the blockstates). A Firewood Pile without model data (one wood) keeps its plain model.
 * Meshed with the chunk, so a pile costs nothing per frame.
 */
@EventBusSubscriber(modid = Mechanical.MODID, value = Dist.CLIENT)
public final class WoodPileModels {
    private static final int WOODS = Mechanical.WOODS.size();
    /** [wood][layer - 1][axis: 0 z, 1 x] */
    @SuppressWarnings("unchecked")
    private static final StandaloneModelKey<BlockStateModelPart>[][][] FIREWOOD = new StandaloneModelKey[WOODS][FirewoodPileBlock.MAX][2];
    /** [wood][log][axis] */
    @SuppressWarnings("unchecked")
    private static final StandaloneModelKey<BlockStateModelPart>[][][] ROUGH = new StandaloneModelKey[WOODS][RoughLogPileBlock.PER_BLOCK][2];

    static {
        for (int w = 0; w < WOODS; w++) {
            String wood = Mechanical.WOODS.get(w);
            for (int a = 0; a < 2; a++) {
                String axis = a == 0 ? "z" : "x";
                for (int k = 0; k < FirewoodPileBlock.MAX; k++) {
                    String n = wood + "_firewood_pile_piece_" + (k + 1) + "_" + axis;
                    FIREWOOD[w][k][a] = new StandaloneModelKey<>(() -> Mechanical.MODID + ":" + n);
                }
                for (int i = 0; i < RoughLogPileBlock.PER_BLOCK; i++) {
                    String n = wood + "_rough_log_piece_" + i + "_" + axis;
                    ROUGH[w][i][a] = new StandaloneModelKey<>(() -> Mechanical.MODID + ":" + n);
                }
            }
        }
    }

    private WoodPileModels() {}

    @SubscribeEvent
    static void registerModels(ModelEvent.RegisterStandalone event) {
        BlockModelRotation turned = BlockModelRotation.get(OctahedralGroup.BLOCK_ROT_Y_90);
        for (int w = 0; w < WOODS; w++) {
            String wood = Mechanical.WOODS.get(w);
            for (int a = 0; a < 2; a++) {
                BlockModelRotation rotation = a == 0 ? BlockModelRotation.IDENTITY : turned;
                for (int k = 0; k < FirewoodPileBlock.MAX; k++) event.register(FIREWOOD[w][k][a], SimpleUnbakedStandaloneModel.simpleModelWrapper(
                        Identifier.fromNamespaceAndPath(Mechanical.MODID, "block/" + wood + "_firewood_pile_piece_" + (k + 1)), rotation));
                for (int i = 0; i < RoughLogPileBlock.PER_BLOCK; i++) event.register(ROUGH[w][i][a], SimpleUnbakedStandaloneModel.simpleModelWrapper(
                        Identifier.fromNamespaceAndPath(Mechanical.MODID, "block/" + wood + "_rough_log_piece_" + i), rotation));
            }
        }
    }

    @SubscribeEvent
    static void wrap(ModelEvent.ModifyBakingResult event) {
        var baked = event.getBakingResult();
        BlockStateModelPart[][][] firewood = new BlockStateModelPart[WOODS][FirewoodPileBlock.MAX][2];
        BlockStateModelPart[][][] rough = new BlockStateModelPart[WOODS][RoughLogPileBlock.PER_BLOCK][2];
        for (int w = 0; w < WOODS; w++) {
            for (int a = 0; a < 2; a++) {
                for (int k = 0; k < FirewoodPileBlock.MAX; k++) firewood[w][k][a] = baked.standaloneModels().get(FIREWOOD[w][k][a]);
                for (int i = 0; i < RoughLogPileBlock.PER_BLOCK; i++) rough[w][i][a] = baked.standaloneModels().get(ROUGH[w][i][a]);
            }
        }
        for (var pile : Mechanical.FIREWOOD_PILES.values()) {
            int own = Mechanical.WOODS.indexOf(pile.get().wood());
            wrapAll(baked, pile.get(), m -> new Pile(m, firewood, own, FirewoodPileBlock.LAYERS));
        }
        wrapAll(baked, Mechanical.ROUGH_LOG_PILE.get(), m -> new Pile(m, rough, 0, RoughLogPileBlock.LOGS));
    }

    private static void wrapAll(net.minecraft.client.resources.model.ModelBakery.BakingResult baked, Block block,
            java.util.function.Function<BlockStateModel, BlockStateModel> wrapper) {
        for (BlockState state : block.getStateDefinition().getPossibleStates()) baked.blockStateModels().computeIfPresent(state, (s, m) -> wrapper.apply(m));
    }

    /** A pile drawn piece by piece in the woods of its model data; {@code fallback} for pieces it doesn't name. */
    static final class Pile extends DelegateBlockStateModel {
        private final BlockStateModelPart[][][] pieces;
        private final int fallback;
        private final net.minecraft.world.level.block.state.properties.IntegerProperty count;

        Pile(BlockStateModel plain, BlockStateModelPart[][][] pieces, int fallback, net.minecraft.world.level.block.state.properties.IntegerProperty count) {
            super(plain);
            this.pieces = pieces;
            this.fallback = fallback;
            this.count = count;
        }

        @Override
        public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
            int[] woods = level.getModelData(pos).get(WoodPileEntity.WOODS);
            if (woods == null) {
                delegate.collectParts(level, pos, state, random, parts);
                return;
            }
            int axis = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_AXIS) == Direction.Axis.X ? 1 : 0;
            int n = state.getValue(count);
            // a rough log that rolled in from the east lies alone on the east half: drawn as the row's second piece
            boolean flipped = state.hasProperty(RoughLogPileBlock.FLIPPED) && state.getValue(RoughLogPileBlock.FLIPPED) && n % 2 == 1;
            for (int k = 0; k < n; k++) {
                int w = k < woods.length && woods[k] >= 0 && woods[k] < pieces.length ? woods[k] : fallback;
                BlockStateModelPart part = pieces[w][flipped && k == n - 1 ? k + 1 : k][axis];
                if (part != null) parts.add(part);
            }
        }
    }
}
