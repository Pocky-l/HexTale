package com.pockyl.hextale.knowledge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import com.pockyl.hextale.network.KnowledgeSyncPayload;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModTriggers;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.research.ItemEssenceTable;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;

import java.util.Collection;

/**
 * Server-side changes to a researcher's knowledge. Every change is synced to the player's client and updates the codex
 * advancements.
 */
public final class Research {
    private Research() {
    }

    public static RuneKnowledge of(Player player) {
        return player.getData(ModAttachments.KNOWLEDGE);
    }

    /**
     * Records a studied rune scroll: the rune is learned (or its stars improve).
     *
     * @return whether the rune was new to the player
     */
    public static boolean learn(ServerPlayer player, ComplexRune rune, int stars) {
        RuneKnowledge knowledge = of(player);
        boolean fresh = knowledge.learn(rune, stars);
        sync(player);
        return fresh;
    }

    /**
     * Records a researched topic: its items may be made from now on.
     *
     * @return whether the topic was new to the player
     */
    public static boolean research(ServerPlayer player, ResearchTopic topic) {
        boolean fresh = of(player).research(topic);
        sync(player);
        return fresh;
    }

    /** Records a studied item: its tooltip shows its elements from now on. */
    public static boolean study(ServerPlayer player, Item item) {
        boolean fresh = of(player).study(BuiltInRegistries.ITEM.getKey(item));
        sync(player);
        return fresh;
    }

    /** @return how many of the runes were new */
    public static int learnAll(ServerPlayer player, Collection<ComplexRune> runes, int stars) {
        RuneKnowledge knowledge = of(player);
        int before = knowledge.runeCount();
        runes.forEach(rune -> knowledge.learn(rune, stars));
        int added = knowledge.runeCount() - before;
        sync(player);
        return added;
    }

    /** @return how many of the runes were known */
    public static int forget(ServerPlayer player, Collection<ComplexRune> runes) {
        RuneKnowledge knowledge = of(player);
        int removed = 0;
        for (ComplexRune rune : runes) {
            if (knowledge.forget(rune)) {
                removed++;
            }
        }
        sync(player);
        return removed;
    }

    /**
     * The Scroll of Omniscience: every rune with three stars, every topic researched, every item with essences studied,
     * every item quest done.
     */
    public static void learnEverything(ServerPlayer player) {
        RuneKnowledge knowledge = of(player);
        ComplexRunes.all().forEach(rune -> knowledge.learn(rune, 3));
        ResearchTopics.all().forEach(knowledge::research);
        ItemEssenceTable.byId().keySet().forEach(knowledge::study);
        for (Quest quest : Guide.all()) {
            if (quest.kind() == Quest.Kind.ITEM) {
                knowledge.completeQuest(quest);
            }
        }
        sync(player);
    }

    /** The Scroll of Oblivion: runes, research, studied items, quests and every parchment's memory are gone. */
    public static void forgetEverything(ServerPlayer player) {
        RuneKnowledge knowledge = of(player);
        ComplexRunes.all().forEach(knowledge::forget);
        knowledge.forgetResearch();
        knowledge.forgetItems();
        knowledge.forgetQuests();
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, new KnowledgeSyncPayload(of(player).copy()));
        ModTriggers.RUNES_LEARNED.get().trigger(player, of(player).runeCount());
    }
}
