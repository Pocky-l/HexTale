package com.pockyl.hextale.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.block.LabBenchBlockEntity;
import com.pockyl.hextale.registry.ModMenus;

/** The Lab Bench: the flask bottle, the sample and the notes in a column at the right of the bench's field. */
public class LabBenchMenu extends AbstractContainerMenu {
    public static final int WIDTH = 276;
    public static final int SLOT_X = 252;
    public static final int FLASK_Y = 24;
    public static final int SAMPLE_Y = 60;
    public static final int OUTPUT_Y = 140;
    public static final int INVENTORY_X = (WIDTH - 162) / 2;
    public static final int INVENTORY_Y = 180;
    private static final int SLOTS = LabBenchBlockEntity.SIZE;

    private final Container bench;
    private final BlockPos pos;

    public LabBenchMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainer(SLOTS), buf.readBlockPos());
    }

    public LabBenchMenu(int containerId, Inventory inventory, Container bench, BlockPos pos) {
        super(ModMenus.LAB_BENCH.get(), containerId);
        this.bench = bench;
        this.pos = pos;
        checkContainerSize(bench, SLOTS);
        bench.startOpen(inventory.player);
        addSlot(new Slot(bench, LabBenchBlockEntity.FLASK, SLOT_X, FLASK_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return LabBenchBlockEntity.accepts(LabBenchBlockEntity.FLASK, stack);
            }
        });
        addSlot(new Slot(bench, LabBenchBlockEntity.SAMPLE, SLOT_X, SAMPLE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return LabBenchBlockEntity.accepts(LabBenchBlockEntity.SAMPLE, stack);
            }
        });
        addSlot(new Slot(bench, LabBenchBlockEntity.OUTPUT, SLOT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < SLOTS) {
            if (!moveItemStackTo(stack, SLOTS, SLOTS + 36, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // A bottle goes to the flask, anything else to the sample.
            boolean bottle = slots.get(LabBenchBlockEntity.FLASK).mayPlace(stack);
            boolean moved = bottle
                    ? moveItemStackTo(stack, LabBenchBlockEntity.FLASK, LabBenchBlockEntity.FLASK + 1, false)
                    : moveItemStackTo(stack, LabBenchBlockEntity.SAMPLE, LabBenchBlockEntity.SAMPLE + 1, false);
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
        return bench.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        bench.stopOpen(player);
    }
}
