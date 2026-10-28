package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import org.jspecify.annotations.Nullable;

/**
 * A rune stele (its lower half): carries the rune a researcher chose on it, with that researcher's stars. Its
 * hieroglyph turns in the window of the stele's head ({@link #GLYPH_Y}); while a ritual of the altar uses it, the stele
 * burns brighter ({@link #activeUntil}).
 */
public class RuneSteleBlockEntity extends BlockEntity {
    /** Block event: a ritual draws on this stele; param = ticks it lasts. */
    public static final int EVENT_ACTIVE = 1;
    /** Height of the rune's middle over the stele's foot (blocks): the middle of the window in its head (25 px). */
    public static final float GLYPH_Y = 25F / 16;

    @Nullable
    private ComplexRune rune;
    private int stars = 1;
    /** Client side: game time until which a ritual draws on the stele. */
    private long activeUntil = Long.MIN_VALUE;
    /** Client side: game time the rune was chosen, for the glyph's entrance. */
    private long chosenAt = Long.MIN_VALUE;

    public RuneSteleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUNE_STELE.get(), pos, state);
    }

    @Nullable
    public ComplexRune rune() {
        return rune;
    }

    public int stars() {
        return stars;
    }

    public long activeUntil() {
        return activeUntil;
    }

    public long chosenAt() {
        return chosenAt;
    }

    public void choose(@Nullable ComplexRune rune, int stars) {
        this.rune = rune;
        this.stars = Math.max(1, Math.min(3, stars));
        if (level != null && !level.isClientSide()) {
            level.setBlock(worldPosition, getBlockState().setValue(RuneSteleBlock.LIT, rune != null), Block.UPDATE_ALL);
            BlockState head = level.getBlockState(worldPosition.above());
            if (head.is(getBlockState().getBlock()) && RuneSteleBlock.upper(head)) {
                level.setBlock(worldPosition.above(), head.setValue(RuneSteleBlock.LIT, rune != null), Block.UPDATE_ALL);
            }
        }
        setChanged();
    }

    /** A stele from before steles stood two blocks tall grows its upper half (see {@link RuneSteleBlock}). */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server && !server.getBlockState(worldPosition.above()).is(getBlockState().getBlock())) {
            server.scheduleTick(worldPosition, getBlockState().getBlock(), 20);
        }
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == EVENT_ACTIVE && level != null) {
            activeUntil = level.getGameTime() + param;
            return true;
        }
        return super.triggerEvent(id, param);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        ComplexRune before = rune;
        rune = input.read("rune", ComplexRunes.CODEC).orElse(null);
        stars = input.getIntOr("stars", 1);
        if (rune != before && level != null && level.isClientSide()) {
            chosenAt = level.getGameTime();
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        if (rune != null) {
            output.store("rune", ComplexRunes.CODEC, rune);
            output.putInt("stars", stars);
        }
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
