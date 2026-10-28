package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;

import com.pockyl.hextale.registry.ModBlockEntities;

/**
 * A pedestal of the ritual circle: holds one item, which floats over it. Hoppers may fill and empty it. When the altar
 * draws its item, the pedestal flares ({@link #drawnAt}) and the item rises away.
 */
public class PedestalBlockEntity extends BlockEntity implements Container {
    /** Block event: the altar starts drawing this pedestal's item. */
    public static final int EVENT_DRAW = 1;
    /**
     * Height over the block's foot where the item floats: just over the brass dish (its top is at 15 px). The altar's
     * ghosts, the drawn item's fragments and the book's circle diorama follow it.
     */
    public static final double ITEM_Y = 1.2;

    private ItemStack item = ItemStack.EMPTY;
    /** Client side: game time the altar began to draw the item. */
    private long drawnAt = Long.MIN_VALUE;

    public PedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PEDESTAL.get(), pos, state);
    }

    public ItemStack item() {
        return item;
    }

    public long drawnAt() {
        return drawnAt;
    }

    /** Puts one item of {@code stack} on the pedestal (if it is empty). */
    public boolean place(ItemStack stack) {
        if (!item.isEmpty() || stack.isEmpty()) {
            return false;
        }
        item = stack.copyWithCount(1);
        setChanged();
        return true;
    }

    public ItemStack take() {
        ItemStack out = item;
        item = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == EVENT_DRAW && level != null) {
            drawnAt = level.getGameTime();
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

    // ---- Container: one slot of one item, for hoppers --------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return item.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return item;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return count > 0 ? take() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack out = item;
        item = ItemStack.EMPTY;
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        item = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        item = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        item = input.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        if (!item.isEmpty()) {
            output.store("item", ItemStack.OPTIONAL_CODEC, item);
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
