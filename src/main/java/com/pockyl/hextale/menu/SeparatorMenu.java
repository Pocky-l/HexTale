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

import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.registry.ModMenus;

/**
 * The Separator: a flask to pour in, empty flasks, the reagent and the two products; data = the reagent's charge, its
 * element and the step progress.
 */
public class SeparatorMenu extends MachineMenu {
    public static final int POUR_X = 12;
    public static final int POUR_Y = 17;
    public static final int SUPPLY_X = 12;
    public static final int SUPPLY_Y = 53;
    public static final int REAGENT_X = 80;
    public static final int REAGENT_Y = 26;
    public static final int MIX_OUT_X = 58;
    public static final int EXTRACT_OUT_X = 128;
    public static final int OUT_Y = 53;

    public SeparatorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainer(5), new SimpleContainerData(3), buf.readBlockPos());
    }

    public SeparatorMenu(int containerId, Inventory inventory, Container container, ContainerData data, BlockPos pos) {
        super(ModMenus.SEPARATOR.get(), containerId, inventory, container, data, pos);
    }

    @Override
    protected void addMachineSlots(Player player) {
        addSlot(machineSlot(SeparatorBlockEntity.POUR, POUR_X, POUR_Y));
        addSlot(machineSlot(SeparatorBlockEntity.REAGENT, REAGENT_X, REAGENT_Y));
        addSlot(machineSlot(SeparatorBlockEntity.SUPPLY, SUPPLY_X, SUPPLY_Y));
        addSlot(machineSlot(SeparatorBlockEntity.MIX_OUT, MIX_OUT_X, OUT_Y));
        addSlot(machineSlot(SeparatorBlockEntity.EXTRACT_OUT, EXTRACT_OUT_X, OUT_Y));
    }

    private Slot machineSlot(int index, int x, int y) {
        return new Slot(container, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SeparatorBlockEntity.accepts(index, stack);
            }
        };
    }

    public int charge() {
        return data(0);
    }

    public int chargeElement() {
        return data(1);
    }

    public int step() {
        return data(2);
    }
}
