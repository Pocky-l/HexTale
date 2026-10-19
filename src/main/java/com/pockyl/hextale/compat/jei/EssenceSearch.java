package com.pockyl.hextale.compat.jei;

import mezz.jei.api.search.ISearchStorage;
import mezz.jei.api.search.ISearchStorageBuilder;
import mezz.jei.api.search.ISearchStorageBuilderFactory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.pockyl.hextale.client.EssenceLookup;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * JEI search by essence: typing an essence's name ("magic") also finds every item the player has studied that carries
 * it. Wraps JEI's own search storage; the storage only knows its entries by the strings they were put under, so the
 * studied items are found again by their names. Unstudied items stay hidden from such a search, as their essences are.
 */
final class EssenceSearch {
    /** Shorter search text matches too many essence names to mean one. */
    private static final int MIN_TOKEN = 3;

    private EssenceSearch() {
    }

    static ISearchStorageBuilderFactory wrap(ISearchStorageBuilderFactory jei) {
        return new ISearchStorageBuilderFactory() {
            @Override
            public <T> ISearchStorageBuilder<T> create() {
                return new Builder<>(jei.create());
            }

            @Override
            public <T> ISearchStorageBuilder<T> create(String id) {
                return new Builder<>(jei.create(id));
            }
        };
    }

    private record Builder<T>(ISearchStorageBuilder<T> inner, Map<String, List<T>> byKey) implements ISearchStorageBuilder<T> {
        Builder(ISearchStorageBuilder<T> inner) {
            this(inner, new HashMap<>());
        }

        @Override
        public void put(String key, T value) {
            inner.put(key, value);
            byKey.computeIfAbsent(normal(key), k -> new ArrayList<>()).add(value);
        }

        @Override
        public ISearchStorage<T> build() {
            return new Storage<>(inner.build(), byKey);
        }
    }

    private record Storage<T>(ISearchStorage<T> inner, Map<String, List<T>> byKey) implements ISearchStorage<T> {
        @Override
        public void getSearchResults(String token, Consumer<Collection<T>> results) {
            inner.getSearchResults(token, results);
            String wanted = normal(token);
            if (wanted.length() < MIN_TOKEN) {
                return;
            }
            List<T> extra = new ArrayList<>();
            for (Item item : EssenceLookup.studiedWith(wanted)) {
                List<T> found = byKey.get(normal(new ItemStack(item).getHoverName().getString()));
                if (found != null) {
                    extra.addAll(found);
                }
            }
            if (!extra.isEmpty()) {
                results.accept(extra);
            }
        }

        @Override
        public void getAllElements(Consumer<Collection<T>> results) {
            inner.getAllElements(results);
        }

        @Override
        public void put(String key, T value) {
            inner.put(key, value);
            byKey.computeIfAbsent(normal(key), k -> new ArrayList<>()).add(value);
        }

        @Override
        public String statistics() {
            return inner.statistics();
        }
    }

    private static String normal(String text) {
        return EssenceLookup.normal(text);
    }
}
