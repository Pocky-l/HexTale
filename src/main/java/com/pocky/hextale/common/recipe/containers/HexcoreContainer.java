package com.pocky.hextale.common.recipe.containers;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HexcoreContainer implements Container {

    private final List<ItemStack> STACKS = new ArrayList<>(16);

    @Override
    public int getContainerSize() {
        return 16;
    }

    @Override
    public boolean isEmpty() {
        return STACKS.isEmpty();
    }

    @Override
    public ItemStack getItem(int index) {
        if (!isEmpty() && index < STACKS.size() && index >= 0) {
            return STACKS.get(index);
        }
        return null;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (!isEmpty() && index < STACKS.size() && index >= 0) {
            ItemStack stack = STACKS.get(index);
            stack.shrink(count);
            return stack;
        }
        return null;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (!isEmpty() && index < STACKS.size() && index >= 0) {
            return STACKS.remove(index);
        }
        return null;
    }

    @Override
    public void setItem(int index, ItemStack itemStack) {
        STACKS.add(index, itemStack);
    }

    @Override
    public void setChanged() {
        // Реализация для уведомления об изменениях в контейнере
        // ...
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
        STACKS.clear(); // Очищает содержимое контейнера
    }

    public boolean contains(ItemStack stack) {
        return STACKS.contains(stack);
    }

    public int size() {
        return STACKS.size();
    }

    public void forEach(Consumer<? super ItemStack> consumer) {
        STACKS.forEach(consumer);
    }

    public void remove(ItemStack stack) {
        STACKS.remove(stack);
    }
}
