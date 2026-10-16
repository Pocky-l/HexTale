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

import com.pockyl.hextale.block.PortMode;
import com.pockyl.hextale.block.SeparatorBlock;
import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.registry.ModMenus;

/**
 * The Separator, read from left to right: a bottle of mixture to pour in and glass bottles, the mixture's tank, the
 * reagent, and the two products - the concentrate (top) and the rest (bottom) - each with the slot it is bottled into.
 * Data = the reagent's charge, its element, the step progress and the mode of each face (the port panel).
 */
public class SeparatorMenu extends MachineMenu {
    public static final int POUR_X = 12;
    public static final int POUR_Y = 17;
    public static final int SUPPLY_X = 12;
    public static final int SUPPLY_Y = 53;
    public static final int REAGENT_X = 68;
    public static final int REAGENT_Y = 35;
    public static final int OUT_X = 130;
    public static final int EXTRACT_OUT_Y = 20;
    public static final int REST_OUT_Y = 50;

    public SeparatorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainer(5), new SimpleContainerData(SeparatorBlockEntity.DATA_COUNT), buf.readBlockPos());
    }

    public SeparatorMenu(int containerId, Inventory inventory, Container container, ContainerData data, BlockPos pos) {
        super(ModMenus.SEPARATOR.get(), containerId, inventory, container, data, pos);
    }

    @Override
    protected void addMachineSlots(Player player) {
        addSlot(machineSlot(SeparatorBlockEntity.POUR, POUR_X, POUR_Y));
        addSlot(machineSlot(SeparatorBlockEntity.REAGENT, REAGENT_X, REAGENT_Y));
        addSlot(machineSlot(SeparatorBlockEntity.SUPPLY, SUPPLY_X, SUPPLY_Y));
        addSlot(machineSlot(SeparatorBlockEntity.REST_OUT, OUT_X, REST_OUT_Y));
        addSlot(machineSlot(SeparatorBlockEntity.EXTRACT_OUT, OUT_X, EXTRACT_OUT_Y));
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

    public PortMode mode(SeparatorBlock.Side side) {
        return PortMode.values()[data(3 + side.ordinal())];
    }

    /** A click on a face of the port panel: the server cycles that face's mode. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= 0 && id < SeparatorBlock.Side.values().length && container instanceof SeparatorBlockEntity separator) {
            separator.cycle(SeparatorBlock.Side.values()[id]);
            return true;
        }
        return false;
    }
}
