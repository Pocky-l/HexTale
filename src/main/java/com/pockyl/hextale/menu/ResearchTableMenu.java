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
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModMenus;

import org.jetbrains.annotations.Nullable;

/**
 * The Research Table. Its screen has two tabs - the research book and the desk (the parchment with the table's slots
 * and the player's inventory beside it); slots exist on both tabs but are only shown and clickable on the desk.
 */
public class ResearchTableMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 468;
    public static final int IMAGE_HEIGHT = 262;
    /** The materials row on the side panel: diary, orb, paper, then the inkwell and the item to study. */
    public static final int ROW_Y = 116;
    public static final int DIARY_X = 258;
    public static final int HEXTECH_X = 278;
    public static final int PAPER_X = 298;
    public static final int INK_X = 326;
    public static final int STUDY_X = 354;
    public static final int TOOLS_X = 258;
    public static final int TOOLS_Y = 138;
    public static final int TOOL_SPACING = 20;
    public static final int OUTPUT_X = 434;
    public static final int OUTPUT_Y = 127;
    public static final int INVENTORY_X = 277;
    public static final int INVENTORY_Y = 166;

    private static final int TABLE_SLOTS = ResearchTableBlockEntity.SLOTS;
    private static final int INVENTORY_START = TABLE_SLOTS;
    private static final int INVENTORY_END = INVENTORY_START + 36;

    private final Container container;
    private final BlockPos pos;
    private final Player player;
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
        this.player = inventory.player;
        container.startOpen(inventory.player);

        addSlot(new TableSlot(container, ResearchTableBlockEntity.DIARY, DIARY_X, ROW_Y));
        addSlot(new TableSlot(container, ResearchTableBlockEntity.HEXTECH, HEXTECH_X, ROW_Y));
        addSlot(new TableSlot(container, ResearchTableBlockEntity.PAPER, PAPER_X, ROW_Y));
        addSlot(new TableSlot(container, ResearchTableBlockEntity.INK, INK_X, ROW_Y));
        addSlot(new TableSlot(container, ResearchTableBlockEntity.STUDY, STUDY_X, ROW_Y));
        addSlot(new TableSlot(container, ResearchTableBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y));
        for (int i = 0; i < ResearchTableBlockEntity.STUDY - ResearchTableBlockEntity.TOOLS; i++) {
            addSlot(new TableSlot(container, ResearchTableBlockEntity.TOOLS + i, TOOLS_X + i * TOOL_SPACING, TOOLS_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new HiddenSlot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new HiddenSlot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 60));
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

    /** The table on the server side: what it does when the researcher changes its inkwell or study slot. */
    private void tableSlotChanged(int index) {
        if (container instanceof ResearchTableBlockEntity table && player instanceof ServerPlayer researcher) {
            if (index == ResearchTableBlockEntity.INK) {
                table.dissolve(researcher);
            } else if (index == ResearchTableBlockEntity.STUDY) {
                table.studySlotChanged(researcher);
            }
        }
    }

    /** Called once the menu is open on the server: items left in the inkwell dissolve for this researcher. */
    public void opened() {
        tableSlotChanged(ResearchTableBlockEntity.INK);
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
        if (player instanceof ServerPlayer researcher) {
            // The fog the thread lifted is kept on the player without a sync per section: send it once now.
            Research.sync(researcher);
        }
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
        public void setChanged() {
            super.setChanged();
            tableSlotChanged(getContainerSlot());
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            int index = getContainerSlot();
            return index >= ResearchTableBlockEntity.TOOLS && index < ResearchTableBlockEntity.STUDY ? 1 : super.getMaxStackSize(stack);
        }
    }
}
