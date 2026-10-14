package com.pockyl.hextale.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.registry.ModMenus;

import org.jetbrains.annotations.Nullable;

/**
 * The Research Table: diary, parchment, orb, ink and brush in a column on the left of the board, the drawing canvas in
 * the middle (client only, see the screen), the finished scroll on the right and the player's inventory below. The
 * diary's rune reference page lies left of the board.
 */
public class ResearchTableMenu extends AbstractContainerMenu {
    /** The table board starts here; left of it lies the diary's rune reference page (client only). */
    public static final int BOARD_X = 100;
    public static final int IMAGE_WIDTH = BOARD_X + 276;
    public static final int IMAGE_HEIGHT = 246;
    public static final int COLUMN_X = BOARD_X + 10;
    public static final int[] COLUMN_Y = {20, 42, 64, 86, 108};
    public static final int OUTPUT_X = BOARD_X + 250;
    public static final int OUTPUT_Y = 65;
    public static final int INVENTORY_X = BOARD_X + 57;
    public static final int INVENTORY_Y = 166;

    private static final int TABLE_SLOTS = ResearchTableBlockEntity.SLOTS;
    private static final int INVENTORY_START = TABLE_SLOTS;
    private static final int INVENTORY_END = INVENTORY_START + 36;

    private final Container container;
    private final BlockPos pos;

    public ResearchTableMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainer(TABLE_SLOTS), buf.readBlockPos());
    }

    public ResearchTableMenu(int containerId, Inventory inventory, Container container, BlockPos pos) {
        super(ModMenus.RESEARCH_TABLE.get(), containerId);
        checkContainerSize(container, TABLE_SLOTS);
        this.container = container;
        this.pos = pos;
        container.startOpen(inventory.player);

        int[] column = {ResearchTableBlockEntity.DIARY, ResearchTableBlockEntity.PAPER, ResearchTableBlockEntity.HEXTECH,
                ResearchTableBlockEntity.INK, ResearchTableBlockEntity.BRUSH};
        for (int i = 0; i < column.length; i++) {
            addSlot(new TableSlot(container, column[i], COLUMN_X, COLUMN_Y[i]));
        }
        addSlot(new TableSlot(container, ResearchTableBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
        }
    }

    /** The table whose screen this player has open at {@code pos}, or null (stale or forged packets). */
    @Nullable
    public static ResearchTableBlockEntity openTable(ServerPlayer player, BlockPos pos) {
        if (player.containerMenu instanceof ResearchTableMenu menu && menu.pos.equals(pos) && menu.stillValid(player)
                && player.level().getBlockEntity(pos) instanceof ResearchTableBlockEntity table) {
            return table;
        }
        return null;
    }

    public BlockPos pos() {
        return pos;
    }

    /** The table's slots in container order (on the client: the synced copy). */
    public Container table() {
        return container;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < TABLE_SLOTS) {
            if (!moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveIntoTable(stack)) {
            return ItemStack.EMPTY;
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

    private boolean moveIntoTable(ItemStack stack) {
        for (int i = 0; i < TABLE_SLOTS; i++) {
            if (slots.get(i).mayPlace(stack) && moveItemStackTo(stack, i, i + 1, false)) {
                return true;
            }
        }
        return false;
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

    /** A table slot: accepts only what the table accepts there (nothing in the output slot). */
    private static class TableSlot extends Slot {
        TableSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ResearchTableBlockEntity.accepts(getContainerSlot(), stack);
        }
    }
}
