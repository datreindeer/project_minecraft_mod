package com.mechanical;

import java.util.Arrays;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;

/**
 * Which wood each piece of a wood pile is, bottom piece first, as indices into {@link Mechanical#WOODS}: the layers of a
 * mixed {@link FirewoodPileBlock} (empty while every layer is the block's own wood, which its plain models draw) and the
 * logs of a {@link RoughLogPileBlock}. The client gets it as model data, so each piece is drawn in its own wood
 * (client/WoodPileModels).
 */
public class WoodPileEntity extends BlockEntity {
    public static final ModelProperty<int[]> WOODS = new ModelProperty<>();
    private int[] woods = new int[0];

    public WoodPileEntity(BlockPos pos, BlockState state) {
        super(Mechanical.WOOD_PILE_ENTITY.get(), pos, state);
    }

    public int[] woods() {
        return woods.clone();
    }

    public void setWoods(int[] woods) {
        if (Arrays.equals(this.woods, woods)) return;
        this.woods = woods.clone();
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (woods.length > 0) output.putIntArray("woods", woods);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        woods = input.getIntArray("woods").orElse(new int[0]);
        if (level != null && level.isClientSide()) {
            // the new woods only show once the section is meshed again
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public ModelData getModelData() {
        return woods.length == 0 ? ModelData.EMPTY : ModelData.of(WOODS, woods.clone());
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
