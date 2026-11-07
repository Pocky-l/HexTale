package com.pockyl.hextale.block;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.CarboyContents;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * A Chem Carboy's brew ({@link CarboyContents}). Hoppers pour vials in through one slot that empties at once; the crucible
 * beside it pours its brew straight in; devices draw doses ({@code CarboyReach}). The brew goes with the carboy when it
 * is broken (item component {@code hextale:chem_carboy}). A creative carboy never runs down and takes nothing in.
 */
public class ChemCarboyBlockEntity extends BlockEntity implements WorldlyContainer {
    private static final int[] SLOTS = {0};
    /** The mouth of the tap and the stopper's top, in the north-facing model. */
    private static final double TAP_OUT = 7.5 / 16;
    private static final double TAP_Y = 2.0 / 16;
    private static final double STOPPER_Y = 31.0 / 16;

    private CarboyContents contents = CarboyContents.EMPTY;

    public ChemCarboyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEM_CARBOY.get(), pos, state);
    }

    /** The carboy at this cell, its upper half included, or null. */
    public static @Nullable ChemCarboyBlockEntity at(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        BlockPos device = state.getBlock() instanceof ShapePartBlock ? ShapePartBlock.device(state, pos) : pos;
        return level.getBlockEntity(device) instanceof ChemCarboyBlockEntity carboy ? carboy : null;
    }

    public CarboyContents contents() {
        return contents;
    }

    public void setContents(CarboyContents contents) {
        this.contents = contents.isEmpty() ? CarboyContents.EMPTY : contents;
        sync();
    }

    public boolean isCreative() {
        return getBlockState().getBlock() instanceof CreativeChemCarboyBlock;
    }

    /** Doses of this brew that can be poured in. */
    public int room(Brew brew) {
        return isCreative() ? 0 : contents.room(brew);
    }

    /** Pours in up to {@code count} doses of the brew; returns how many went in. */
    public int fill(Brew brew, int count) {
        int taken = Math.min(count, room(brew));
        if (taken > 0) {
            setContents(contents.plus(brew, taken));
        }
        return taken;
    }

    /** One dose leaves; a creative carboy does not notice. */
    public void takeDose() {
        if (!isCreative()) {
            setContents(contents.minusDose());
        }
    }

    /** At the tap: a dose into a vial, with the sound and the drop; empty when the carboy is. */
    public ItemStack bottle() {
        if (contents.isEmpty() || !(level instanceof ServerLevel server)) {
            return ItemStack.EMPTY;
        }
        Brew brew = contents.brew();
        takeDose();
        Vec3 tap = tap();
        server.sendParticles(ModParticles.drop(brew.color()), tap.x, tap.y - 0.05, tap.z, 3, 0.01, 0.02, 0.01, 0.01);
        server.playSound(null, tap.x, tap.y, tap.z, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 0.4F, 1.0F);
        return ChemVialItem.of(brew);
    }

    /** Where a dose leaves: the mouth of the tap, in front of the base. */
    public Vec3 tap() {
        Direction front = getBlockState().getValue(ChemCarboyBlock.FACING);
        return Vec3.atLowerCornerOf(worldPosition).add(0.5 + front.getStepX() * TAP_OUT, TAP_Y, 0.5 + front.getStepZ() * TAP_OUT);
    }

    /** Where a stream comes in: the stopper's top. */
    public Vec3 stopper() {
        return Vec3.atLowerCornerOf(worldPosition).add(0.5, STOPPER_Y, 0.5);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
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
        return brew != null && room(brew) >= stack.getCount();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        Brew brew = ChemVialItem.brew(stack);
        if (brew != null) {
            fill(brew, stack.getCount());
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
        contents = input.read("brew", CarboyContents.CODEC).orElse(CarboyContents.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!contents.isEmpty()) {
            output.store("brew", CarboyContents.CODEC, contents);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        contents = components.getOrDefault(ModDataComponents.CHEM_CARBOY.get(), CarboyContents.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!contents.isEmpty()) {
            components.set(ModDataComponents.CHEM_CARBOY.get(), contents);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("brew");
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
