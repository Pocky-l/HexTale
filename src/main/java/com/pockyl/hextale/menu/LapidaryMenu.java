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

import com.pockyl.hextale.block.LapidaryBlockEntity;
import com.pockyl.hextale.registry.ModMenus;

/** The Hex Lapidary: the crystal, the disc and the finished stone in a column at the right of the cutting field. */
public class LapidaryMenu extends AbstractContainerMenu {
    public static final int WIDTH = 236;
    public static final int SLOT_X = 208;
    public static final int INPUT_Y = 26;
    public static final int DISC_Y = 62;
    public static final int OUTPUT_Y = 112;
    public static final int INVENTORY_X = (WIDTH - 162) / 2;
    public static final int INVENTORY_Y = 162;
    private static final int SLOTS = LapidaryBlockEntity.SIZE;

    private final Container lapidary;
    private final BlockPos pos;

    public LapidaryMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainer(SLOTS), buf.readBlockPos());
    }

    public LapidaryMenu(int containerId, Inventory inventory, Container lapidary, BlockPos pos) {
        super(ModMenus.LAPIDARY.get(), containerId);
        this.lapidary = lapidary;
        this.pos = pos;
        checkContainerSize(lapidary, SLOTS);
        lapidary.startOpen(inventory.player);
        addSlot(new Slot(lapidary, LapidaryBlockEntity.INPUT, SLOT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return LapidaryBlockEntity.accepts(LapidaryBlockEntity.INPUT, stack);
            }
        });
        addSlot(new Slot(lapidary, LapidaryBlockEntity.DISC, SLOT_X, DISC_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return LapidaryBlockEntity.accepts(LapidaryBlockEntity.DISC, stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(lapidary, LapidaryBlockEntity.OUTPUT, SLOT_X, OUTPUT_Y) {
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
            boolean moved = false;
            for (int i = 0; i < SLOTS && !moved; i++) {
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
        return lapidary.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        lapidary.stopOpen(player);
    }
}
