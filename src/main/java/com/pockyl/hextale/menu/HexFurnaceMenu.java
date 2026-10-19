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

import com.pockyl.hextale.block.HexFurnaceBlockEntity;
import com.pockyl.hextale.registry.ModMenus;

/** The Hex Furnace: three metal inputs in a row, fuel under them, the alloy out; data = progress, total, lit time, lit duration. */
public class HexFurnaceMenu extends MachineMenu {
    public static final int INPUT_X = 26;
    public static final int INPUT_Y = 22;
    public static final int FUEL_X = 44;
    public static final int FUEL_Y = 62;
    public static final int OUTPUT_X = 130;
    public static final int OUTPUT_Y = 40;

    public HexFurnaceMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainer(HexFurnaceBlockEntity.SIZE), new SimpleContainerData(4), buf.readBlockPos());
    }

    public HexFurnaceMenu(int containerId, Inventory inventory, Container container, ContainerData data, BlockPos pos) {
        super(ModMenus.HEX_FURNACE.get(), containerId, inventory, container, data, pos);
    }

    @Override
    protected void addMachineSlots(Player player) {
        for (int i = 0; i < HexFurnaceBlockEntity.INPUTS; i++) {
            addSlot(new Slot(container, i, INPUT_X + i * 18, INPUT_Y));
        }
        addSlot(new Slot(container, HexFurnaceBlockEntity.FUEL, FUEL_X, FUEL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return HexFurnaceBlockEntity.accepts(HexFurnaceBlockEntity.FUEL, stack);
            }
        });
        addSlot(new Slot(container, HexFurnaceBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player taker, ItemStack stack) {
                super.onTake(taker, stack);
                if (container instanceof HexFurnaceBlockEntity furnace) {
                    furnace.awardExperience(taker);
                }
            }
        });
    }

    public int progress() {
        return data(0);
    }

    public int total() {
        return Math.max(1, data(1));
    }

    /** How much of the current fuel is left, 0-1. */
    public float fuel() {
        return data(3) <= 0 ? 0 : data(2) / (float) data(3);
    }
}
