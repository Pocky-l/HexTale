package com.pockyl.hextale.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.block.AlembicBlockEntity;
import com.pockyl.hextale.registry.ModMenus;

/** The Alembic: the item to boil, an empty flask and the filled flask; data = boiling progress and heat. */
public class AlembicMenu extends MachineMenu {
    public static final int INPUT_X = 44;
    public static final int INPUT_Y = 35;
    public static final int FLASK_X = 134;
    public static final int FLASK_IN_Y = 17;
    public static final int FLASK_OUT_Y = 53;

    public AlembicMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainer(3), new SimpleContainerData(2), buf.readBlockPos());
    }

    public AlembicMenu(int containerId, Inventory inventory, Container container, ContainerData data, BlockPos pos) {
        super(ModMenus.ALEMBIC.get(), containerId, inventory, container, data, pos);
    }

    @Override
    protected void addMachineSlots(Player player) {
        addSlot(new Slot(container, AlembicBlockEntity.INPUT, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AlembicBlockEntity.accepts(AlembicBlockEntity.INPUT, stack);
            }

            @Override
            public void setByPlayer(ItemStack stack, ItemStack previous) {
                super.setByPlayer(stack, previous);
                // Unstudied items boil into unknown essence: the player who puts the item in is its boiler.
                if (player instanceof ServerPlayer && container instanceof AlembicBlockEntity alembic && !stack.isEmpty()) {
                    alembic.boiledBy(player);
                }
            }
        });
        addSlot(new Slot(container, AlembicBlockEntity.FLASK_IN, FLASK_X, FLASK_IN_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return AlembicBlockEntity.accepts(AlembicBlockEntity.FLASK_IN, stack);
            }
        });
        addSlot(new Slot(container, AlembicBlockEntity.FLASK_OUT, FLASK_X, FLASK_OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    public int progress() {
        return data(0);
    }

    public boolean heated() {
        return data(1) != 0;
    }
}
