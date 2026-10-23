package com.pockyl.hextale.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.block.HexFoundryBlockEntity;
import com.pockyl.hextale.registry.ModMenus;

/**
 * The Hex Foundry: nine inputs in a square at the left, nine outputs at the right, the fuel slot between them under the
 * heat gauge; data = heat, fire gauge (0-1000), then each input's melt in percent.
 */
public class HexFoundryMenu extends MachineMenu {
    public static final int INPUT_X = 8;
    public static final int OUTPUT_X = 116;
    public static final int GRID_Y = 22;
    public static final int FUEL_X = 80;
    public static final int FUEL_Y = 62;

    public HexFoundryMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainer(HexFoundryBlockEntity.SIZE), new SimpleContainerData(2 + HexFoundryBlockEntity.INPUTS),
                buf.readBlockPos());
    }

    public HexFoundryMenu(int containerId, Inventory inventory, Container container, ContainerData data, BlockPos pos) {
        super(ModMenus.HEX_FOUNDRY.get(), containerId, inventory, container, data, pos);
    }

    @Override
    protected void addMachineSlots(Player player) {
        for (int i = 0; i < HexFoundryBlockEntity.INPUTS; i++) {
            addSlot(new Slot(container, i, INPUT_X + i % 3 * 18, GRID_Y + i / 3 * 18));
        }
        for (int i = 0; i < HexFoundryBlockEntity.OUTPUTS; i++) {
            addSlot(new Slot(container, HexFoundryBlockEntity.OUTPUT_START + i, OUTPUT_X + i % 3 * 18, GRID_Y + i / 3 * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }

                @Override
                public void onTake(Player taker, ItemStack stack) {
                    super.onTake(taker, stack);
                    if (container instanceof HexFoundryBlockEntity foundry) {
                        foundry.awardExperience(taker);
                    }
                }
            });
        }
        addSlot(new Slot(container, HexFoundryBlockEntity.FUEL, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return HexFoundryBlockEntity.accepts(HexFoundryBlockEntity.FUEL, stack);
            }
        });
    }

    public int heat() {
        return data(0);
    }

    /** How much fire is left, 0-1. */
    public float fire() {
        return data(1) / 1000F;
    }

    /** How far the piece in an input has melted, 0-1. */
    public float melt(int slot) {
        return data(2 + slot) / 100F;
    }

    /** Shift-click from the inventory: fuel goes to the fuel slot, anything else is spread over the inputs. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        int machine = HexFoundryBlockEntity.SIZE;
        Slot slot = slots.get(index);
        if (index < machine || !slot.hasItem()) {
            return super.quickMoveStack(player, index);
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean fuel = HexFoundryBlockEntity.accepts(HexFoundryBlockEntity.FUEL, stack)
                && moveItemStackTo(stack, HexFoundryBlockEntity.FUEL, HexFoundryBlockEntity.FUEL + 1, false);
        if (!fuel && !moveItemStackTo(stack, 0, HexFoundryBlockEntity.INPUTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return stack.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }
}
