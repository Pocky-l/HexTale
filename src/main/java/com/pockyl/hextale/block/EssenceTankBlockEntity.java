package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.registry.ModBlockEntities;

/** The essence tank's contents; it drains downwards into pipes. */
public class EssenceTankBlockEntity extends BlockEntity implements EssenceNetwork.Receiver {
    public static final int CAPACITY = 256;
    /** Ticks between pushes, and drops per push. */
    static final int FLOW_EVERY = 5;
    static final int FLOW = 4;

    private final EssenceTank tank = new EssenceTank(CAPACITY);

    public EssenceTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENCE_TANK.get(), pos, state);
    }

    public EssenceTank tank() {
        return tank;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssenceTankBlockEntity tank) {
        if (level.getGameTime() % FLOW_EVERY != 0 || tank.tank.isEmpty()) {
            return;
        }
        EssenceMix offer = tank.tank.drain(FLOW, true);
        int moved = EssenceNetwork.push(level, pos, Direction.DOWN, offer);
        if (moved > 0) {
            tank.tank.remove(offer.part(moved));
            tank.setChanged();
        }
    }

    @Override
    public boolean isOutlet(Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public int receive(Direction side, EssenceMix offer, boolean simulate) {
        if (side == Direction.DOWN) {
            return 0;
        }
        int taken = tank.fill(offer, simulate);
        if (taken > 0 && !simulate) {
            setChanged();
        }
        return taken;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tank.load(input, "essence");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.save(output, "essence");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
