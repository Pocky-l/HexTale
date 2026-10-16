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

/**
 * The essence tank's contents; it drains downwards into pipes. The contents travel with the item (component
 * {@code hextale:essence}): a broken tank keeps them, a full tank can be placed from the creative tab. A Creative Essence
 * Tank (same entity) holds one essence and is always full: whatever is drawn or let out is back the next tick, and it
 * takes nothing in.
 */
public class EssenceTankBlockEntity extends BlockEntity implements EssenceNetwork.Receiver {
    /** Millibuckets: sixteen bottles. */
    public static final int CAPACITY = 4000;
    /** Ticks between pushes, and millibuckets per push. */
    static final int FLOW_EVERY = 5;
    static final int FLOW = 60;

    private final EssenceTank tank = new EssenceTank(CAPACITY);

    public EssenceTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENCE_TANK.get(), pos, state);
    }

    public EssenceTank tank() {
        return tank;
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
