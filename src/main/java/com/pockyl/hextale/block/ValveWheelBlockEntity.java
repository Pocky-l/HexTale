package com.pockyl.hextale.block;

import com.pockyl.hextale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** How many quarter turns a Wall Valve Wheel was given. The client eases the wheel to its new angle when the count changes. */
public class ValveWheelBlockEntity extends BlockEntity {
    private static final float STEP = 90;
    private static final float TURN_TICKS = 14;

    private int turns;
    /** The angle the wheel left from and when, on the client. */
    private float turnedFrom;
    private long turnedAt = Long.MIN_VALUE;

    public ValveWheelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VALVE_WHEEL.get(), pos, state);
    }

    public int turns() {
        return turns;
    }

    public void turn() {
        turns++;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The wheel's angle in degrees now: clockwise seen from the front, slow at both ends of a turn. */
    public float angle(float partialTick) {
        float target = turns * STEP;
        if (level == null || turnedAt == Long.MIN_VALUE) {
            return target;
        }
        float done = (level.getGameTime() - turnedAt + partialTick) / TURN_TICKS;
        return done >= 1 ? target : Mth.lerp(done * done * (3 - 2 * done), turnedFrom, target);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int loaded = input.getIntOr("turns", 0);
        if (level != null && level.isClientSide() && loaded != turns) {
            turnedFrom = angle(0);
            turnedAt = level.getGameTime();
        }
        turns = loaded;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("turns", turns);
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
