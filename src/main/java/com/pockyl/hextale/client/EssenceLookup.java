package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.CreativeModeTabSearchRegistry;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Finding items by their essences: typing an essence's name ("magic") in the creative search or in JEI also finds the
 * items the player has studied that carry it. Unstudied items are not found that way - their essences are a secret.
 *
 * <p>The creative search indexes the tooltip lines items give without a player (that is how the search tree asks for
 * them, a real tooltip always names the player), so studied items add their essence names there only; the index is
 * rebuilt whenever the studied items change.
 */
public final class EssenceLookup {
    private static Set<String> indexed = Set.of();

    private EssenceLookup() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EssenceLookup::searchLines);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> refreshCreativeSearch());
    }

    /** Studied items carrying an essence whose name (in the language being played) contains {@code wanted}. */
    public static List<Item> studiedWith(String wanted) {
        String text = normal(wanted);
        List<Essence> essences = new ArrayList<>();
        for (Essence essence : Essence.values()) {
            if (normal(Component.translatable(essence.translationKey()).getString()).contains(text)) {
                essences.add(essence);
            }
        }
        List<Item> items = new ArrayList<>();
        if (essences.isEmpty()) {
            return items;
        }
        for (String id : ClientKnowledge.get().studied()) {
            ResourceLocation key = ResourceLocation.tryParse(id);
            Item item = key == null ? null : BuiltInRegistries.ITEM.getOptional(key).orElse(null);
            ItemEssences carried = item == null ? null : ResearchItems.of(item);
            if (carried != null && essences.stream().anyMatch(carried.essences()::containsKey)) {
                items.add(item);
            }
        }
        return items;
    }

    public static String normal(String text) {
        return text.toLowerCase(Locale.ROOT).trim();
    }

    /** For the creative search tree only (no player): a studied item's essence names. */
    private static void searchLines(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (event.getEntity() != null || stack.isEmpty()
                || !ClientKnowledge.get().hasStudied(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            return;
        }
        ItemEssences essences = ResearchItems.of(stack);
        if (essences != null) {
            essences.essences().keySet().forEach(essence -> event.getToolTip().add(Component.translatable(essence.translationKey())));
        }
    }

    /** Rebuilds the creative search trees when the studied items changed since they were last built. */
    private static void refreshCreativeSearch() {
        Minecraft minecraft = Minecraft.getInstance();
        Set<String> studied = ClientKnowledge.get().studied();
        if (studied.equals(indexed) || minecraft.getConnection() == null || minecraft.level == null) {
            return;
        }
        if (CreativeModeTabs.searchTab().getDisplayItems().isEmpty()) {
            // The tabs are built the first time the creative screen opens, and their search with them.
            return;
        }
        indexed = Set.copyOf(studied);
        var trees = minecraft.getConnection().searchTrees();
        var registries = minecraft.level.registryAccess();
        CreativeModeTabs.allTabs().stream().filter(CreativeModeTab::hasSearchBar).forEach(tab ->
                trees.updateCreativeTooltips(registries, List.copyOf(tab.getDisplayItems()), CreativeModeTabSearchRegistry.getNameSearchKey(tab)));
    }
}
