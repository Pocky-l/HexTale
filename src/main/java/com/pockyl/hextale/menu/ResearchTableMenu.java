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
 * The Research Table. Its screen has three tabs - the research book, the parchment and the table itself; slots (the
 * materials, the research tools, the output and the player's inventory) exist on every tab but are only shown and
 * clickable on the table tab.
 */
public class ResearchTableMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 468;
    public static final int IMAGE_HEIGHT = 262;
    /** Materials in a row: diary, paper, orb, ink, brush; the scroll comes out right of them. */
    public static final int MATERIALS_X = 154;
    public static final int MATERIALS_Y = 50;
    public static final int OUTPUT_X = 298;
    public static final int TOOLS_X = 182;
    public static final int TOOLS_Y = 104;
    public static final int SLOT_SPACING = 22;
    public static final int INVENTORY_X = 153;
    public static final int INVENTORY_Y = 164;

    private static final int TABLE_SLOTS = ResearchTableBlockEntity.SLOTS;
    private static final int INVENTORY_START = TABLE_SLOTS;
    private static final int INVENTORY_END = INVENTORY_START + 36;
    private static final int[] MATERIALS = {ResearchTableBlockEntity.DIARY, ResearchTableBlockEntity.PAPER,
            ResearchTableBlockEntity.HEXTECH, ResearchTableBlockEntity.INK, ResearchTableBlockEntity.BRUSH};

    private final Container container;
    private final BlockPos pos;
    /** Client side: whether the table tab is open (slots are hidden on the other tabs). */
    private boolean slotsVisible = true;

    public ResearchTableMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainer(TABLE_SLOTS), buf.readBlockPos());
    }

    public ResearchTableMenu(int containerId, Inventory inventory, Container container, BlockPos pos) {
        super(ModMenus.RESEARCH_TABLE.get(), containerId);
        checkContainerSize(container, TABLE_SLOTS);
        this.container = container;
        this.pos = pos;
        container.startOpen(inventory.player);

        for (int i = 0; i < MATERIALS.length; i++) {
            addSlot(new TableSlot(container, MATERIALS[i], MATERIALS_X + i * SLOT_SPACING, MATERIALS_Y));
        }
        addSlot(new TableSlot(container, ResearchTableBlockEntity.OUTPUT, OUTPUT_X, MATERIALS_Y));
        for (int i = 0; i < TABLE_SLOTS - ResearchTableBlockEntity.TOOLS; i++) {
            addSlot(new TableSlot(container, ResearchTableBlockEntity.TOOLS + i, TOOLS_X + i * SLOT_SPACING, TOOLS_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new HiddenSlot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new HiddenSlot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58));
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

    public void setSlotsVisible(boolean visible) {
        slotsVisible = visible;
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

    /** A player inventory slot shown only on the table tab. */
    private class HiddenSlot extends Slot {
        HiddenSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean isActive() {
            return slotsVisible;
        }
    }

    /** A table slot: accepts only what the table accepts there (nothing in the output slot). */
    private class TableSlot extends HiddenSlot {
        TableSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return ResearchTableBlockEntity.accepts(getContainerSlot(), stack);
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return getContainerSlot() >= ResearchTableBlockEntity.TOOLS ? 1 : super.getMaxStackSize(stack);
        }
    }
}
