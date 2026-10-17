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
import com.pockyl.hextale.rune.Essence;

import org.jspecify.annotations.Nullable;

/**
 * The Separator, read from left to right: a bottle of mixture to pour in and glass bottles, the mixture's tank, the
 * chosen essence over the blaze powder, and the two products - the concentrate (top) and the rest (bottom) - each with the slot it is bottled into.
 * Data = the powder's charge, the chosen essence, the step progress and the mode of each face (the port panel). Buttons:
 * 0-3 cycle a face, {@link #CHOOSE} + essence ordinal chooses the essence ({@link #CHOOSE} + the number of essences: the
 * dark unknown essence), {@link #CHOOSE} - 1 clears it.
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
    public static final int CHOOSE = 100;

    public SeparatorMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainer(5), new SimpleContainerData(SeparatorBlockEntity.DATA_COUNT), buf.readBlockPos());
    }

    public SeparatorMenu(int containerId, Inventory inventory, Container container, ContainerData data, BlockPos pos) {
        super(ModMenus.SEPARATOR.get(), containerId, inventory, container, data, pos);
    }

    @Override
    protected void addMachineSlots(Player player) {
        addSlot(machineSlot(SeparatorBlockEntity.POUR, POUR_X, POUR_Y));
        addSlot(machineSlot(SeparatorBlockEntity.FUEL, REAGENT_X, REAGENT_Y));
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

    /** The chosen essence, or null. */
    @Nullable
    public Essence chosen() {
        int index = data(1);
        return index >= 0 && index < Essence.COUNT ? Essence.values()[index] : null;
    }

    /** Whether the dark unknown essence is chosen. */
    public boolean chosenUnknown() {
        return data(1) == SeparatorBlockEntity.UNKNOWN_CHOICE;
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
        if (!(container instanceof SeparatorBlockEntity separator)) {
            return false;
        }
        if (id >= 0 && id < SeparatorBlock.Side.values().length) {
            separator.cycle(SeparatorBlock.Side.values()[id]);
            return true;
        }
        if (id == CHOOSE + SeparatorBlockEntity.UNKNOWN_CHOICE) {
            separator.chooseUnknown();
            return true;
        }
        if (id >= CHOOSE - 1 && id < CHOOSE + Essence.COUNT) {
            separator.choose(id < CHOOSE ? null : Essence.values()[id - CHOOSE]);
            return true;
        }
        return false;
    }
}
