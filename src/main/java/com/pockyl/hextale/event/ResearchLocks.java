package com.pockyl.hextale.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;

import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Devices and tools must be researched before they can be crafted: a crafting grid (the inventory's or a crafting
 * table's) whose result is an unresearched item of the mod shows no result. The server recomputes the result after
 * every change of the grid and this listener clears it right after, before the next click can take it; the researcher
 * is told what to research. Altar rituals and structures check the same knowledge where they start.
 */
public final class ResearchLocks {
    /** Menus already watched (the inventory menu is joined again on every change of dimension). */
    private static final Set<AbstractContainerMenu> WATCHED = Collections.newSetFromMap(new WeakHashMap<>());

    private ResearchLocks() {
    }

    public static void register(IEventBus bus) {
        bus.addListener((PlayerContainerEvent.Open event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                watch(player, event.getContainer());
            }
        });
        bus.addListener((EntityJoinLevelEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                watch(player, player.inventoryMenu);
            }
        });
    }

    private static void watch(ServerPlayer player, AbstractContainerMenu menu) {
        boolean crafts = menu.slots.stream().anyMatch(slot -> slot instanceof ResultSlot);
        if (!crafts || !WATCHED.add(menu)) {
            return;
        }
        menu.addSlotListener(new ContainerListener() {
            @Override
            public void slotChanged(AbstractContainerMenu changed, int index, ItemStack stack) {
                if (!(changed.getSlot(index) instanceof ResultSlot slot) || ResearchTopics.mayMake(player, stack, Research.of(player))) {
                    return;
                }
                slot.container.setItem(slot.getContainerSlot(), ItemStack.EMPTY);
                ResearchTopic topic = ResearchTopics.of(stack.getItem());
                if (topic != null) {
                    player.displayClientMessage(Component.translatable("research.hextale.locked",
                            Component.translatable(topic.translationKey()).withColor(topic.color())), true);
                }
            }

            @Override
            public void dataChanged(AbstractContainerMenu changed, int id, int value) {
            }
        });
    }
}
