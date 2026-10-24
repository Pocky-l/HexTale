package com.pockyl.hextale.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModMenus;
import com.pockyl.hextale.transport.ItemFilter;

/**
 * The Pylon Filter's screen: nine ghost slots above the player's inventory. Nothing goes into the ghost slots - clicking
 * one with an item on the cursor copies it in, with an empty cursor clears it; the screen sends those clicks and the
 * switches as menu buttons, and the server writes the result onto the filter in the player's hand (the client reads it
 * back from there). The filter's own slot in the hotbar cannot be clicked away.
 */
public class ItemFilterMenu extends AbstractContainerMenu {
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 116;
    /** Button ids: 0-8 ghost slot click; 20+ / 40+ / 60+ / 80+ slot: count +1 / -1 / +10 / -10. */
    public static final int SET = 0;
    public static final int PLUS = 20;
    public static final int MINUS = 40;
    public static final int PLUS_TEN = 60;
    public static final int MINUS_TEN = 80;
    public static final int TOGGLE_LIST = 100;
    public static final int TOGGLE_EXACT = 101;
    public static final int CYCLE_RULE = 102;

    private final Player player;
    private final InteractionHand hand;

    public ItemFilterMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readEnum(InteractionHand.class));
    }

    public ItemFilterMenu(int containerId, Inventory inventory, InteractionHand hand) {
        super(ModMenus.ITEM_FILTER.get(), containerId);
        this.player = inventory.player;
        this.hand = hand;
        int locked = hand == InteractionHand.MAIN_HAND ? inventory.selected : -1;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            int index = col;
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, INVENTORY_Y + 58) {
                @Override
                public boolean mayPickup(Player who) {
                    return index != locked;
                }
            });
        }
    }

    public InteractionHand hand() {
        return hand;
    }

    public ItemStack filterItem() {
        return player.getItemInHand(hand);
    }

    public ItemFilter filter() {
        return filterItem().getOrDefault(ModDataComponents.ITEM_FILTER.get(), ItemFilter.EMPTY);
    }

    @Override
    public boolean clickMenuButton(Player who, int id) {
        ItemStack stack = filterItem();
        if (!stack.is(ModItems.PYLON_FILTER.get())) {
            return false;
        }
        ItemFilter filter = filter();
        ItemFilter changed;
        if (id >= SET && id < SET + ItemFilter.SLOTS) {
            changed = filter.with(id - SET, getCarried());
        } else if (id >= PLUS && id < PLUS + ItemFilter.SLOTS) {
            changed = filter.add(id - PLUS, 1);
        } else if (id >= MINUS && id < MINUS + ItemFilter.SLOTS) {
            changed = filter.add(id - MINUS, -1);
        } else if (id >= PLUS_TEN && id < PLUS_TEN + ItemFilter.SLOTS) {
            changed = filter.add(id - PLUS_TEN, 10);
        } else if (id >= MINUS_TEN && id < MINUS_TEN + ItemFilter.SLOTS) {
            changed = filter.add(id - MINUS_TEN, -10);
        } else if (id == TOGGLE_LIST) {
            changed = filter.withWhitelist(!filter.whitelist());
        } else if (id == TOGGLE_EXACT) {
            changed = filter.withExact(!filter.exact());
        } else if (id == CYCLE_RULE) {
            changed = filter.withRule(filter.rule().next());
        } else {
            return false;
        }
        stack.set(ModDataComponents.ITEM_FILTER.get(), changed);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player who, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player who) {
        return filterItem().is(ModItems.PYLON_FILTER.get());
    }
}
