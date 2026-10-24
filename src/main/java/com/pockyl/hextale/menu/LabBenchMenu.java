package com.pockyl.hextale.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.LabBenchBlockEntity;
import com.pockyl.hextale.registry.ModMenus;

/**
 * The Lab Bench: the flask bottle and the sample left of the inventory, the notes right of it; each empty slot shows a
 * silhouette of what goes in.
 */
public class LabBenchMenu extends AbstractContainerMenu {
    public static final int WIDTH = 300;
    public static final int INVENTORY_X = (WIDTH - 162) / 2;
    public static final int INVENTORY_Y = 188;
    public static final int LEFT_SLOT_X = 28;
    public static final int FLASK_Y = INVENTORY_Y + 12;
    public static final int SAMPLE_Y = INVENTORY_Y + 46;
    public static final int OUTPUT_X = WIDTH - 44;
    public static final int OUTPUT_Y = INVENTORY_Y + 12;
    private static final ResourceLocation BOTTLE_ICON = Hextale.id("lab_bench/slot_bottle");
    private static final ResourceLocation SAMPLE_ICON = Hextale.id("lab_bench/slot_sample");
    private static final ResourceLocation NOTES_ICON = Hextale.id("lab_bench/slot_notes");
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
        addSlot(new Slot(bench, LabBenchBlockEntity.FLASK, LEFT_SLOT_X, FLASK_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return LabBenchBlockEntity.accepts(LabBenchBlockEntity.FLASK, stack);
            }

            @Override
            public ResourceLocation getNoItemIcon() {
                return BOTTLE_ICON;
            }
        });
        addSlot(new Slot(bench, LabBenchBlockEntity.SAMPLE, LEFT_SLOT_X, SAMPLE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return LabBenchBlockEntity.accepts(LabBenchBlockEntity.SAMPLE, stack);
            }

            @Override
            public ResourceLocation getNoItemIcon() {
                return SAMPLE_ICON;
            }
        });
        addSlot(new Slot(bench, LabBenchBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public ResourceLocation getNoItemIcon() {
                return NOTES_ICON;
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
