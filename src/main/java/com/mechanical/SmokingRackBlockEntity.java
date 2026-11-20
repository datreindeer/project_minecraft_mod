package com.mechanical;

import java.util.ArrayList;
import java.util.List;

import com.mechanical.SmokingRackBlock.Content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** What hangs on a {@link SmokingRackBlock}: the strips (keeping their spoil time) and how long they have been in smoke. */
public class SmokingRackBlockEntity extends BlockEntity {
    /** Seconds of smoke until the strips are smoked: 4 minutes. */
    public static final int SMOKE_SECONDS = 240;

    private List<ItemStack> items = new ArrayList<>();
    private int smoked;

    public SmokingRackBlockEntity(BlockPos pos, BlockState state) {
        super(Preserving.SMOKING_RACK_ENTITY.get(), pos, state);
    }

    /** One more raw strip; the smoking starts over (the new strip has to be smoked all the way too). */
    public void hang(ItemStack one, long now) {
        Preserving.check(one, now); // stamp it, so it keeps spoiling on the rack
        items.add(one);
        smoked = 0;
        setChanged();
    }

    public List<ItemStack> takeAll() {
        List<ItemStack> out = items;
        items = new ArrayList<>();
        smoked = 0;
        setChanged();
        return out;
    }

    void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if ((level.getGameTime() + pos.asLong()) % 20 != 0) return;
        Content content = state.getValue(SmokingRackBlock.CONTENT);
        if (content != Content.RAW_MEAT && content != Content.RAW_FISH) return;
        if (SmokingRackBlock.inSmoke(level, pos)) {
            if (level.getRandom().nextInt(4) == 0)
                level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 2, 0.25, 0.2, 0.25, 0.005);
            if (++smoked < SMOKE_SECONDS) {
                setChanged();
                return;
            }
            boolean meat = content == Content.RAW_MEAT;
            // a whole cut (Haunch, Ribs, Loin) smokes whole; a knife slices it into Smoked Meat later
            items.replaceAll(s -> new ItemStack(meat ? Preserving.smoked(s) : Preserving.SMOKED_FISH.get(), s.getCount()));
            finish(level, pos, state, meat ? Content.SMOKED_MEAT : Content.SMOKED_FISH);
            return;
        }
        // no smoke: the strips spoil on their own time; the first to go spoils the lot (flies)
        long now = level.getGameTime();
        if (items.stream().anyMatch(s -> Preserving.freshness(s, now) == 0)) {
            items.replaceAll(s -> new ItemStack(Items.ROTTEN_FLESH, s.getCount()));
            finish(level, pos, state, Content.ROTTEN);
        }
    }

    private void finish(ServerLevel level, BlockPos pos, BlockState state, Content content) {
        smoked = 0;
        setChanged();
        level.setBlock(pos, state.setValue(SmokingRackBlock.CONTENT, content), Block.UPDATE_ALL);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) for (ItemStack s : items) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), s);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("items", ItemStack.CODEC.listOf(), items);
        output.putInt("smoked", smoked);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = new ArrayList<>(input.read("items", ItemStack.CODEC.listOf()).orElse(List.of()));
        smoked = input.getIntOr("smoked", 0);
    }
}
