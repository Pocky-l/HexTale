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
import net.minecraft.world.level.block.entity.BlockEntityType;
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
 * The essence tank's contents; it drains downwards into pipes. The contents and the sticker travel with the item
 * (components {@code hextale:essence} and {@code hextale:sticker}): a broken jar keeps them, a full jar can be placed
 * from the creative tab. A Creative Essence
 * Tank (same entity) holds one essence and is always full: whatever is drawn or let out is back the next tick, and it
 * takes nothing in.
 */
public class EssenceTankBlockEntity extends BlockEntity implements EssenceNetwork.Receiver {
    /** Millibuckets: sixteen bottles. */
    public static final int CAPACITY = 256;
    /** Ticks between pushes, and millibuckets per push. */
    static final int FLOW_EVERY = 5;
    static final int FLOW = 4;

    /** What a Creative Essence Tank holds: a full tank of every essence at once. */
    public static final EssenceMix CREATIVE = EssenceMix.all(CAPACITY);

    private final EssenceTank tank;
    /** The essence sticker on the jar: it then takes only that essence. */
    private @Nullable Essence sticker;

    public EssenceTankBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.ESSENCE_TANK.get(), pos, state, CAPACITY);
    }

    /** A jar of another kind and size (the Big Essence Jar). */
    protected EssenceTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        tank = new EssenceTank(capacity);
    }

    /** The jar at this block: a jar itself, or the Big Essence Jar a block of which stands there; else null. */
    @Nullable
    public static EssenceTankBlockEntity at(net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof com.pockyl.hextale.multiblock.MultiblockPartBlockEntity part) {
            entity = level.getBlockEntity(part.controller());
        }
        return entity instanceof EssenceTankBlockEntity jar ? jar : null;
    }

    public EssenceTank tank() {
        return tank;
    }

    /** The essence on the jar's sticker, or null. */
    public @Nullable Essence sticker() {
        return sticker;
    }

    public void setSticker(@Nullable Essence essence) {
        sticker = essence;
        setChanged();
    }

    /** Whether the jar takes this essence: anything without a sticker, else only the sticker's essence, pure. */
    public boolean accepts(EssenceMix offer) {
        return sticker == null || offer.unknown() == 0 && offer.get(sticker) == offer.total();
    }

    /** Block event: someone pushed the jar (the parameter is the direction they faced); clients rock it. */
    public static final int PUSHED = 1;
    /** Client side: the game time of the last push (-1 never) and the direction it pushed towards. */
    private float pushedAt = -1;
    private int pushedTowards;

    public float pushedAt() {
        return pushedAt;
    }

    public int pushedTowards() {
        return pushedTowards;
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == PUSHED) {
            if (level != null && level.isClientSide()) {
                pushedAt = level.getGameTime();
                pushedTowards = param;
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }

    /** Whether this is a Creative Essence Tank. */
    public boolean isCreative() {
        return getBlockState().getBlock() instanceof CreativeEssenceTankBlock;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        EssenceMix contents = components.get(ModDataComponents.ESSENCE.get());
        if (contents != null && !isCreative()) {
            tank.set(contents.part(tank.capacity()));
        }
        sticker = components.get(ModDataComponents.STICKER.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!tank.isEmpty() && !isCreative()) {
            components.set(ModDataComponents.ESSENCE.get(), tank.contents());
        }
        if (sticker != null) {
            components.set(ModDataComponents.STICKER.get(), sticker);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("essence");
        output.discard("sticker");
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssenceTankBlockEntity tank) {
        if (tank.isCreative()) {
            // Every essence, always full; it pours nothing into pipes (machines and the table draw what they need).
            if (!tank.tank.contents().equals(CREATIVE)) {
                tank.tank.set(CREATIVE);
                tank.setChanged();
            }
            return;
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
        if (side == Direction.DOWN || isCreative() || !accepts(offer)) {
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
        sticker = Essence.byId(input.getStringOr("sticker", ""));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.save(output, "essence");
        if (sticker != null) {
            output.putString("sticker", sticker.id());
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
