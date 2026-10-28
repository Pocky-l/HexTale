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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        if (fresh) {
            ModTriggers.RESEARCH_COMPLETED.get().trigger(player, topic.id());
        }
        return fresh;
    }

    /** Researches every topic given at once (an operator's command); returns how many were new. */
    public static int researchAll(ServerPlayer player, Collection<ResearchTopic> topics) {
        RuneKnowledge knowledge = of(player);
        List<ResearchTopic> fresh = new ArrayList<>();
        for (ResearchTopic topic : topics) {
            if (knowledge.research(topic)) {
                fresh.add(topic);
            }
        }
        sync(player);
        fresh.forEach(topic -> ModTriggers.RESEARCH_COMPLETED.get().trigger(player, topic.id()));
        return fresh.size();
    }

    /**
     * Forgets every topic given at once (an operator's command): their items are locked again, their parchments start
     * afresh. Returns how many were researched.
     */
    public static int unresearchAll(ServerPlayer player, Collection<ResearchTopic> topics) {
        RuneKnowledge knowledge = of(player);
        int forgotten = 0;
        for (ResearchTopic topic : topics) {
            if (knowledge.forgetResearch(topic)) {
                forgotten++;
            }
        }
        sync(player);
        return forgotten;
    }

    /**
     * Fires the quest makers' triggers again for everything the player already knows: an advancement or quest added to
     * a pack after the fact is met at the next login.
     */
    public static void catchUp(ServerPlayer player) {
        RuneKnowledge knowledge = of(player);
        for (String topic : knowledge.researchedIds()) {
            ModTriggers.RESEARCH_COMPLETED.get().trigger(player, topic);
        }
        for (Quest quest : Guide.server().all()) {
            if (knowledge.questDone(quest)) {
                ModTriggers.GUIDE_STEP.get().trigger(player, quest.id());
            }
        }
    }

    /** Records compounds seen forming in a Lab Bench flask: the journal shows their zones from now on. */
    public static boolean observe(ServerPlayer player, java.util.Collection<com.pockyl.hextale.rune.Essence> compounds) {
        boolean fresh = false;
        for (com.pockyl.hextale.rune.Essence compound : compounds) {
            fresh |= of(player).observe(compound);
        }
        if (fresh) {
            sync(player);
        }
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
        List<ResearchTopic> topics = new ArrayList<>();
        for (ResearchTopic topic : ResearchTopics.all()) {
            if (knowledge.research(topic)) {
                topics.add(topic);
            }
        }
        ItemEssenceTable.byId().keySet().forEach(knowledge::study);
        List<Quest> steps = new ArrayList<>();
        for (Quest quest : Guide.server().all()) {
            if (quest.kind() == Quest.Kind.ITEM) {
                if (!knowledge.questDone(quest)) {
                    steps.add(quest);
                }
                knowledge.completeQuest(quest);
                knowledge.notice(quest.id());
            }
        }
        sync(player);
        topics.forEach(topic -> ModTriggers.RESEARCH_COMPLETED.get().trigger(player, topic.id()));
        steps.forEach(quest -> ModTriggers.GUIDE_STEP.get().trigger(player, quest.id()));
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
