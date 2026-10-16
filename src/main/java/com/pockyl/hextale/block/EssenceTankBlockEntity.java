package com.pockyl.hextale.block;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
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
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.rune.Essence;

import org.jspecify.annotations.Nullable;

/**
 * The essence tank's contents; it drains downwards into pipes. The contents travel with the item (component
 * {@code hextale:essence}): a broken tank keeps them, a full tank can be placed from the creative tab. A Creative Essence
 * Tank (same entity) holds one essence and is always full: whatever is drawn or let out is back the next tick, and it
 * takes nothing in.
 */
public class EssenceTankBlockEntity extends BlockEntity implements EssenceNetwork.Receiver {
    /** Millibuckets: sixteen bottles. */
    public static final int CAPACITY = 256;
    /** Ticks between pushes, and millibuckets per push. */
    static final int FLOW_EVERY = 5;
    static final int FLOW = 4;

    private final EssenceTank tank = new EssenceTank(CAPACITY);
    /** A sticker per face (by {@link Direction} index): 0 none, else essence ordinal + 1. Only a sign for the player. */
    private final int[] labels = new int[Direction.values().length];

    public EssenceTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENCE_TANK.get(), pos, state);
    }

    public EssenceTank tank() {
        return tank;
    }

    /** The essence on the sticker of this face, or null. */
    @Nullable
    public Essence label(Direction face) {
        int label = labels[face.get3DDataValue()];
        return label == 0 ? null : Essence.values()[label - 1];
    }

    /** Puts the next essence's sticker on this face: none, then every essence in turn, then none again. */
    public void cycleLabel(Direction face) {
        int index = face.get3DDataValue();
        labels[index] = (labels[index] + 1) % (Essence.values().length + 1);
        setChanged();
    }

    /** Whether this is a Creative Essence Tank. */
    public boolean isCreative() {
        return getBlockState().getBlock() instanceof CreativeEssenceTankBlock;
    }

    /** Makes a creative tank hold {@code essence} (always full). */
    public void setCreativeEssence(Essence essence) {
        tank.set(EssenceMix.pure(essence, CAPACITY));
        setChanged();
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        EssenceMix contents = components.get(ModDataComponents.ESSENCE.get());
        if (contents != null) {
            tank.set(contents.part(CAPACITY));
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!tank.isEmpty()) {
            components.set(ModDataComponents.ESSENCE.get(), tank.contents());
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("essence");
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssenceTankBlockEntity tank) {
        if (tank.isCreative()) {
            Essence essence = tank.tank.contents().pureEssence();
            if (essence != null && tank.tank.amount() < CAPACITY) {
                tank.tank.set(EssenceMix.pure(essence, CAPACITY));
            }
        }
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
        if (side == Direction.DOWN || isCreative()) {
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
        int[] stored = input.getIntArray("labels").orElse(new int[0]);
        for (int i = 0; i < labels.length; i++) {
            labels[i] = i < stored.length && stored[i] >= 0 && stored[i] <= Essence.values().length ? stored[i] : 0;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.save(output, "essence");
        output.putIntArray("labels", labels.clone());
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
