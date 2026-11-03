package com.pockyl.hextale.block;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemFuel;
import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.chem.TankFuel;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

/**
 * A placed Chem Tank's fuel ({@link TankFuel}). Hoppers pour fuel vials in through one slot that empties at once. The fuel
 * goes with the tank when it is broken (item component {@code hextale:chem_tank}). The client keeps the time of the last
 * pump for the hose animation.
 */
public class ChemTankBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final int[] SLOTS = {0};
    private TankFuel contents = TankFuel.EMPTY;
    private long pumpedAt = -1000;
    private int pumpedTo = -1;

    public ChemTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEM_TANK.get(), pos, state);
    }

    public TankFuel contents() {
        return contents;
    }

    public void setContents(TankFuel contents) {
        this.contents = contents.isEmpty() ? TankFuel.EMPTY : contents;
        sync();
    }

    /** Remembers a pump into this player's tank (for the hose on the client). */
    public void pumped(Player player) {
        if (level != null) {
            pumpedAt = level.getGameTime();
            pumpedTo = player.getId();
            sync();
        }
    }

    public long pumpedAt() {
        return pumpedAt;
    }

    public int pumpedTo() {
        return pumpedTo;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Hoppers: one slot that pours at once -------------------------------------------------------------------------

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        Brew brew = ChemVialItem.brew(stack);
        return brew != null && ChemFuel.of(brew) != null && ChemTank.room(contents, brew) >= stack.getCount();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        Brew brew = ChemVialItem.brew(stack);
        if (brew != null && ChemFuel.of(brew) != null) {
            int vials = Math.min(stack.getCount(), ChemTank.room(contents, brew));
            if (vials > 0) {
                setContents(ChemTank.poured(contents, brew, vials));
            }
        }
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        contents = input.read("fuel", TankFuel.CODEC).orElse(TankFuel.EMPTY);
        pumpedAt = input.getLongOr("pumped_at", -1000);
        pumpedTo = input.getIntOr("pumped_to", -1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!contents.isEmpty()) {
            output.store("fuel", TankFuel.CODEC, contents);
        }
        output.putLong("pumped_at", pumpedAt);
        output.putInt("pumped_to", pumpedTo);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        contents = components.getOrDefault(ModDataComponents.CHEM_TANK.get(), TankFuel.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!contents.isEmpty()) {
            components.set(ModDataComponents.CHEM_TANK.get(), contents);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("fuel");
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
