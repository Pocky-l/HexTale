package com.pockyl.hextale.gametest;

import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.event.KnowledgeCommands;
import com.pockyl.hextale.knowledge.NamedTrigger;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.knowledge.RitualFinishedTrigger;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModTriggers;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.List;
import java.util.UUID;

/**
 * Hooks for quest makers and pack makers: the criteria triggers fire for the right names and only for them, the
 * commands grant and revoke research and guide steps.
 */
public final class QuestHookGameTests {
    private QuestHookGameTests() {
    }

    /**
     * An advancement made on the spot with one criterion, listened for by the player. Awarding and revoking the criterion
     * once is the public way to make the player's advancements listen for it.
     */
    private static <T extends CriterionTriggerInstance> AdvancementHolder listen(ServerPlayer player, CriterionTrigger<T> trigger, String json) {
        Codec<T> codec = trigger.codec();
        T instance = codec.parse(player.level().registryAccess().createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json))
                .getOrThrow(error -> new IllegalStateException("bad conditions " + json + ": " + error));
        AdvancementHolder holder = Advancement.Builder.advancement().addCriterion("met", trigger.createCriterion(instance))
                .build(Hextale.id("test/" + UUID.randomUUID()));
        PlayerAdvancements advancements = player.getAdvancements();
        advancements.award(holder, "met");
        advancements.revoke(holder, "met");
        return holder;
    }

    private static boolean met(ServerPlayer player, AdvancementHolder holder) {
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static CommandSourceStack source(GameTestHelper helper) {
        return helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput();
    }

    /** Research granted by the command fires research_completed for that topic only; revoking locks it again. */
    @HexTest
    public static void researchCommandsAndTheirTrigger(GameTestHelper helper) throws CommandSyntaxException {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        AdvancementHolder battery = listen(player, ModTriggers.RESEARCH_COMPLETED.get(), "{\"topic\": \"crystal_battery\"}");
        AdvancementHolder other = listen(player, ModTriggers.RESEARCH_COMPLETED.get(), "{\"topic\": \"hexcore\"}");
        AdvancementHolder any = listen(player, ModTriggers.RESEARCH_COMPLETED.get(), "{}");
        int granted = KnowledgeCommands.grantResearch(source(helper), List.of(player), "crystal_battery");
        RuneKnowledge knowledge = Research.of(player);
        helper.assertTrue(granted == 1 && knowledge.researched(ResearchTopics.CRYSTAL_BATTERY), "the command researches the topic");
        helper.assertTrue(met(player, battery) && met(player, any), "research_completed fires for it");
        helper.assertTrue(!met(player, other), "and not for another topic");
        helper.assertTrue(KnowledgeCommands.grantResearch(source(helper), List.of(player), "crystal_battery") == 0, "granting twice changes nothing");
        helper.assertTrue(KnowledgeCommands.revokeResearch(source(helper), List.of(player), "crystal_battery") == 1
                && !knowledge.researched(ResearchTopics.CRYSTAL_BATTERY), "revoke forgets it");
        int all = KnowledgeCommands.grantResearch(source(helper), List.of(player), "all");
        helper.assertTrue(all == ResearchTopics.all().size() && met(player, other), "all researches every topic");
        helper.assertTrue(KnowledgeCommands.revokeResearch(source(helper), List.of(player), "all") == all, "and all forgets them");
        helper.succeed();
    }

    /** Granting a step marks it and every step it waits for; guide_step fires; resetting a parent resets its children. */
    @HexTest
    public static void guideCommandsAndTheirTrigger(GameTestHelper helper) throws CommandSyntaxException {
        Guide guide = Guide.server();
        Quest step = guide.all().stream()
                .filter(quest -> quest.kind() == Quest.Kind.ITEM && quest.topic() == null && !quest.parents().isEmpty()
                        && quest.parents().getFirst().kind() == Quest.Kind.ITEM)
                .findFirst().orElseThrow();
        Quest parent = step.parents().getFirst();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        AdvancementHolder wanted = listen(player, ModTriggers.GUIDE_STEP.get(), "{\"step\": \"" + step.id() + "\"}");
        AdvancementHolder before = listen(player, ModTriggers.GUIDE_STEP.get(), "{\"step\": \"hextale:" + parent.id() + "\"}");
        KnowledgeCommands.grantSteps(source(helper), List.of(player), step.id());
        RuneKnowledge knowledge = Research.of(player);
        helper.assertTrue(knowledge.questDone(step) && knowledge.questDone(parent), "the step and the one it waits for are done");
        helper.assertTrue(met(player, wanted) && met(player, before), "guide_step fires for both (with or without the namespace)");
        KnowledgeCommands.resetSteps(source(helper), List.of(player), parent.id());
        helper.assertTrue(!knowledge.questDone(parent) && !knowledge.questDone(step), "resetting a step resets the steps after it");
        helper.assertTrue(Quests.grant(player, parent) && !Quests.grant(player, parent), "a step is granted once");
        helper.succeed();
    }

    /** Events reported to the guide fire guide_event with their name. */
    @HexTest
    public static void guideEventsFireTheirTrigger(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        AdvancementHolder opened = listen(player, ModTriggers.GUIDE_EVENT.get(), "{\"event\": \"" + Quests.BREACH_OPENED + "\"}");
        AdvancementHolder core = listen(player, ModTriggers.GUIDE_EVENT.get(), "{\"event\": \"" + Quests.HEXCORE_AWAKENED + "\"}");
        Quests.fire(player, Quests.BREACH_OPENED);
        helper.assertTrue(met(player, opened) && !met(player, core), "only the event that happened fires");
        helper.succeed();
    }

    /** ritual_finished matches by the item on the altar and the ritual's kind. */
    @HexTest
    public static void ritualFinishedMatchesItemAndKind(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        RitualFinishedTrigger trigger = ModTriggers.RITUAL_FINISHED.get();
        AdvancementHolder diamond = listen(player, trigger, "{\"item\": {\"items\": \"minecraft:diamond\"}, \"kind\": \"infuse\"}");
        AdvancementHolder written = listen(player, trigger, "{\"kind\": \"write\"}");
        trigger.trigger(player, new ItemStack(Items.EMERALD), "infuse");
        helper.assertTrue(!met(player, diamond) && !met(player, written), "an emerald infusion matches neither");
        trigger.trigger(player, new ItemStack(Items.DIAMOND), "infuse");
        helper.assertTrue(met(player, diamond) && !met(player, written), "a diamond infusion matches the item condition");
        NamedTrigger.TriggerInstance plain = new NamedTrigger.TriggerInstance(java.util.Optional.empty(), java.util.Optional.empty());
        helper.assertTrue(plain.matches("anything"), "a named trigger without a name matches any");
        helper.succeed();
    }
}
