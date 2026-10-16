package com.pockyl.hextale.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.block.RuneEngraverBlockEntity;
import com.pockyl.hextale.registry.ModMenus;
import com.pockyl.hextale.runework.HextechTools;

/**
 * The Rune Engraver: tool, scroll and bottle slots; the tool's rune slots are plaques on the screen. Button ids:
 * {@code slot} engraves the scroll into that rune slot, {@link #GRIND} + {@code slot} grinds that rune off.
 */
public class RuneEngraverMenu extends MachineMenu {
    public static final int GRIND = 10;
    public static final int TOOL_X = 44;
    public static final int TOOL_Y = 35;
    public static final int SCROLL_X = 14;
    public static final int SCROLL_Y = 17;
    public static final int BOTTLE_X = 14;
    public static final int BOTTLE_Y = 53;

    public RuneEngraverMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, new SimpleContainer(3), buf.readBlockPos());
    }

    public RuneEngraverMenu(int containerId, Inventory inventory, Container container, BlockPos pos) {
        super(ModMenus.RUNE_ENGRAVER.get(), containerId, inventory, container, new SimpleContainerData(0), pos);
    }

    @Override
    protected void addMachineSlots(Player player) {
        addSlot(new Slot(container, RuneEngraverBlockEntity.TOOL, TOOL_X, TOOL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return RuneEngraverBlockEntity.accepts(RuneEngraverBlockEntity.TOOL, stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new Slot(container, RuneEngraverBlockEntity.SCROLL, SCROLL_X, SCROLL_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return RuneEngraverBlockEntity.accepts(RuneEngraverBlockEntity.SCROLL, stack);
            }
        });
        addSlot(new Slot(container, RuneEngraverBlockEntity.BOTTLE, BOTTLE_X, BOTTLE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return RuneEngraverBlockEntity.accepts(RuneEngraverBlockEntity.BOTTLE, stack);
            }
        });
    }

    public ItemStack tool() {
        return slots.get(RuneEngraverBlockEntity.TOOL).getItem();
    }

    public ItemStack scroll() {
        return slots.get(RuneEngraverBlockEntity.SCROLL).getItem();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(container instanceof RuneEngraverBlockEntity engraver)) {
            return false;
        }
        if (id >= 0 && id < HextechTools.SLOTS) {
            engraver.engrave(serverPlayer, id);
            return true;
        }
        if (id >= GRIND && id < GRIND + HextechTools.SLOTS) {
            engraver.grind(serverPlayer, id - GRIND);
            return true;
        }
        return false;
    }
}
