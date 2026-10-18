package com.pockyl.hextale.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * A small machine screen of the essence works (alembic, separator): the machine's slots, the player's inventory below
 * and a few synced numbers. Tank contents come from the block entity itself, which syncs them to clients.
 */
public abstract class MachineMenu extends AbstractContainerMenu {
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 102;

    protected final Container container;
    protected final ContainerData data;
    private final BlockPos pos;
    private final int machineSlots;

    protected MachineMenu(MenuType<?> type, int containerId, Inventory inventory, Container container, ContainerData data, BlockPos pos) {
        super(type, containerId);
        this.container = container;
        this.data = data;
        this.pos = pos;
        this.machineSlots = container.getContainerSize();
        checkContainerSize(container, machineSlots);
        container.startOpen(inventory.player);
        addMachineSlots(inventory.player);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }
        addDataSlots(data);
    }

    /** Adds the machine's own slots, in container order. */
    protected abstract void addMachineSlots(Player player);

    public BlockPos pos() {
        return pos;
    }

    public int data(int index) {
        return data.get(index);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int inventoryEnd = machineSlots + 36;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, inventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < machineSlots && !moved; i++) {
                moved = slots.get(i).mayPlace(stack) && moveItemStackTo(stack, i, i + 1, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }
}
