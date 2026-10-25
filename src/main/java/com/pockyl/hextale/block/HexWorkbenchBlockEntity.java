package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.menu.HexWorkbenchMenu;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModSounds;

import java.util.List;

/**
 * The Hex Workbench's contents: the 3x3 grid (the items stay on the worktop when nobody works at it) and the crystal
 * socket. Every craft spends charge, and the crystal in the socket gives it: an unstable orb {@value #UNSTABLE_CHARGE},
 * a crystal grown in shimmer {@value #SYNTHETIC_CHARGE}, a cut crystal {@value #CRYSTAL_CHARGE}, a stabilised gemstone
 * {@value #GEMSTONE_CHARGE}. Crystals in the socket soak
 * into the bench one by one while there is room under {@value #MAX_CHARGE}; a craft short of charge uses them up at once.
 */
public class HexWorkbenchBlockEntity extends BaseContainerBlockEntity {
    public static final int GRID = 9;
    public static final int CRYSTAL = 9;
    public static final int SIZE = 10;
    public static final int UNSTABLE_CHARGE = 5;
    public static final int SYNTHETIC_CHARGE = 8;
    public static final int CRYSTAL_CHARGE = 12;
    public static final int GEMSTONE_CHARGE = 30;
    /** The most charge the bench soaks up from its socket by itself. */
    public static final int MAX_CHARGE = 60;
    private static final int SOAK_TICKS = 10;
    /** Block event: a craft was made (clients flash the worktop). */
    public static final int CRAFTED = 1;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int charge;
    /** Bumped on every change, so open menus know when to look for a new result. */
    private int revision;
    /** Client side: game time of the last craft, for the flash. */
    public long craftedAt = -1000;

    public HexWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEX_WORKBENCH.get(), pos, state);
    }

    /** Charge a crystal gives, 0 for anything else. */
    public static int chargeOf(ItemStack stack) {
        if (stack.is(ModItems.UNSTABLE_HEXTECH.get())) {
            return UNSTABLE_CHARGE;
        }
        if (stack.is(ModItems.SYNTHETIC_CRYSTAL.get())) {
            return SYNTHETIC_CHARGE;
        }
        if (stack.is(ModItems.HEXTECH_CRYSTAL.get())) {
            return CRYSTAL_CHARGE;
        }
        return stack.is(ModItems.HEXTECH_GEMSTONE.get()) ? GEMSTONE_CHARGE : 0;
    }

    public int charge() {
        return charge;
    }

    /** Takes charge from outside (a Crystal Battery) while there is room under {@value #MAX_CHARGE}; returns what it took. */
    public int receive(int amount) {
        int taken = Math.max(0, Math.min(amount, MAX_CHARGE - charge));
        if (taken > 0) {
            charge += taken;
            setChanged();
        }
        return taken;
    }

    public int revision() {
        return revision;
    }

    /** The charge left plus what the crystals in the socket would give. */
    public int chargeAvailable() {
        ItemStack crystal = items.get(CRYSTAL);
        return charge + chargeOf(crystal) * crystal.getCount();
    }

    /** Spends {@code cost} charge, using up crystals from the socket as needed; false (nothing spent) when short. */
    public boolean spend(int cost) {
        if (chargeAvailable() < cost) {
            return false;
        }
        ItemStack crystal = items.get(CRYSTAL);
        while (charge < cost) {
            charge += chargeOf(crystal);
            crystal.shrink(1);
        }
        charge -= cost;
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            com.pockyl.hextale.thevoid.Thinning.add(server, worldPosition, cost * com.pockyl.hextale.thevoid.Thinning.CHARGE);
        }
        setChanged();
        return true;
    }

    public CraftingInput.Positioned input() {
        return CraftingInput.ofPositioned(3, 3, items.subList(0, GRID));
    }

    public List<ItemStack> grid() {
        return items.subList(0, GRID);
    }

    /** Soaks one crystal from the socket into charge when there is room for it (every {@value #SOAK_TICKS} ticks). */
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, HexWorkbenchBlockEntity bench) {
        ItemStack crystal = bench.items.get(CRYSTAL);
        int value = chargeOf(crystal);
        if (value <= 0 || bench.charge + value > MAX_CHARGE || level.getGameTime() % SOAK_TICKS != 0) {
            return;
        }
        crystal.shrink(1);
        bench.charge += value;
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.5F, 1.6F);
        bench.setChanged();
    }

    /** Effects of a craft on the server: a soft spend sound. */
    public void crafted(ServerLevel level) {
        level.blockEvent(worldPosition, getBlockState().getBlock(), CRAFTED, 0);
        Vec3 top = Vec3.atLowerCornerOf(worldPosition).add(0.5, 1.02, 0.5);
        level.playSound(null, top.x, top.y, top.z, ModSounds.WORKBENCH_CRAFT.get(), SoundSource.BLOCKS, 0.7F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        level.playSound(null, top.x, top.y, top.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.4F, 1.4F);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == CRAFTED && level != null && level.isClientSide()) {
            craftedAt = level.getGameTime();
            return true;
        }
        return super.triggerEvent(id, param);
    }

    // ---- Container ----------------------------------------------------------------------------------------------------

    @Override
    public void setChanged() {
        revision++;
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hextale.hex_workbench");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new HexWorkbenchMenu(containerId, inventory, this, worldPosition);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != CRYSTAL || chargeOf(stack) > 0;
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        charge = input.getIntOr("charge", 0);
        revision++;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("charge", charge);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** Client: whether a craft flashed within the last {@code ticks}. */
    public float flash(Level level, float partialTick, int ticks) {
        float age = level.getGameTime() - craftedAt + partialTick;
        return age < 0 || age > ticks ? 0 : 1 - age / ticks;
    }
}
