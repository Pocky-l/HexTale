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

import com.pockyl.hextale.block.HexSmelterBlockEntity;
import com.pockyl.hextale.registry.ModMenus;

/** The Hex Smelter: input, output, a bottle of fiery essence and the emptied bottles; data = progress, ticks per item, heat. */
public class HexSmelterMenu extends MachineMenu {
    public static final int INPUT_X = 56;
    public static final int INPUT_Y = 26;
    public static final int OUTPUT_X = 112;
    public static final int OUTPUT_Y = 35;
    public static final int BOTTLE_X = 8;
    public static final int BOTTLE_IN_Y = 17;
    public static final int BOTTLE_OUT_Y = 53;

    public HexSmelterMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainer(HexSmelterBlockEntity.SIZE), new SimpleContainerData(3), buf.readBlockPos());
    }

    public HexSmelterMenu(int containerId, Inventory inventory, Container container, ContainerData data, BlockPos pos) {
        super(ModMenus.HEX_SMELTER.get(), containerId, inventory, container, data, pos);
    }

    @Override
    protected void addMachineSlots(Player player) {
        addSlot(new Slot(container, HexSmelterBlockEntity.INPUT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return HexSmelterBlockEntity.accepts(HexSmelterBlockEntity.INPUT, stack);
            }
        });
        addSlot(new Slot(container, HexSmelterBlockEntity.OUTPUT, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player taker, ItemStack stack) {
                super.onTake(taker, stack);
                if (container instanceof HexSmelterBlockEntity smelter) {
                    smelter.awardExperience(taker);
                }
            }
        });
        addSlot(new Slot(container, HexSmelterBlockEntity.BOTTLE_IN, BOTTLE_X, BOTTLE_IN_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return HexSmelterBlockEntity.accepts(HexSmelterBlockEntity.BOTTLE_IN, stack);
            }
        });
        addSlot(new Slot(container, HexSmelterBlockEntity.BOTTLE_OUT, BOTTLE_X, BOTTLE_OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    public int progress() {
        return data(0);
    }

    public int cookTicks() {
        return Math.max(1, data(1));
    }

    /** Heat drawn and not yet spent, in hundredths. */
    public int heat() {
        return data(2);
    }
}
