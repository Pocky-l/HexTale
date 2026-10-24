package com.pockyl.hextale.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.server.level.ServerLevel;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.registry.ModBlockEntities;

import org.jspecify.annotations.Nullable;

/**
 * A block of a formed structure: it remembers the block it was and its controller. Essence that pipes bring to it goes
 * to the controller, if that takes essence; taking it away (breaking it) takes the whole structure apart.
 */
public class MultiblockPartBlockEntity extends BlockEntity implements EssenceNetwork.Receiver, com.pockyl.hextale.port.Removal.SideEffects {
    private BlockPos controller = BlockPos.ZERO;
    private BlockState original = Blocks.AIR.defaultBlockState();

    public MultiblockPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MULTIBLOCK_PART.get(), pos, state);
    }

    void bind(BlockPos controller, BlockState original) {
        this.controller = controller;
        this.original = original;
        setChanged();
    }

    public BlockPos controller() {
        return controller;
    }

    /** The block that stood here before the structure was formed. */
    public BlockState original() {
        return original;
    }

    @Nullable
    public BlockEntity core() {
        return level == null ? null : level.getBlockEntity(controller);
    }

    /** The controller's container when its structure shares items through its blocks, else null. */
    public net.minecraft.world.@Nullable WorldlyContainer sharedContainer() {
        if (core() instanceof MultiblockCore core && core instanceof net.minecraft.world.WorldlyContainer container && core.formed() != null) {
            Multiblock multiblock = Multiblocks.byId(core.formed().multiblock());
            return multiblock != null && multiblock.sharesItems() ? container : null;
        }
        return null;
    }

    @Override
    public int receive(Direction side, EssenceMix offer, boolean simulate) {
        return core() instanceof EssenceNetwork.Receiver receiver ? receiver.receive(side, offer, simulate) : 0;
    }

    /** Removed by anything but the structure itself coming apart: the rest of the structure comes apart too. */
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        com.pockyl.hextale.port.Removal.vanilla(this, pos, state);
        Level level = this.level;
        if (level instanceof ServerLevel serverLevel) {
            Multiblocks.breakApart(serverLevel, controller, pos);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        controller = input.read("controller", BlockPos.CODEC).orElse(BlockPos.ZERO);
        original = input.read("original", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        output.store("controller", BlockPos.CODEC, controller);
        output.store("original", BlockState.CODEC, original);
    }
}
