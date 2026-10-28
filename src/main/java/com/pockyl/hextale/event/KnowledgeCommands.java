package com.pockyl.hextale.event;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.quest.Quest;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Operator commands for admins and quest rewards (part of {@code /hextale}, permission level 2):
 * {@code research grant|revoke <targets> <topic|all>} and {@code guide grant|reset <targets> <step|all>}. Granting a
 * step grants every step it waits for too (and researches a research step's topic); resetting one resets every step
 * that waits for it. Steps whose goal is still met come back at the next check, as with {@code quests reset}.
 */
public final class KnowledgeCommands {
    private static final String ALL = "all";
    private static final DynamicCommandExceptionType UNKNOWN_TOPIC =
            new DynamicCommandExceptionType(id -> Component.translatable("commands.hextale.research.unknown", id));
    private static final DynamicCommandExceptionType UNKNOWN_STEP =
            new DynamicCommandExceptionType(id -> Component.translatable("commands.hextale.guide.unknown", id));
    private static final SuggestionProvider<CommandSourceStack> TOPICS = (context, builder) ->
            SharedSuggestionProvider.suggest(Stream.concat(Stream.of(ALL), ResearchTopics.all().stream().map(ResearchTopic::id)), builder);
    private static final SuggestionProvider<CommandSourceStack> STEPS = (context, builder) ->
            SharedSuggestionProvider.suggest(Stream.concat(Stream.of(ALL),
                    Guide.server().all().stream().filter(quest -> quest.kind() == Quest.Kind.ITEM).map(Quest::id)), builder);

    private KnowledgeCommands() {
    }

    /** {@code research grant|revoke <targets> <topic|all>}. */
    static LiteralArgumentBuilder<CommandSourceStack> research() {
        return Commands.literal("research")
                .then(Commands.literal("grant")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("topic", StringArgumentType.word()).suggests(TOPICS)
                                        .executes(context -> grantResearch(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                StringArgumentType.getString(context, "topic"))))))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("topic", StringArgumentType.word()).suggests(TOPICS)
                                        .executes(context -> revokeResearch(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                StringArgumentType.getString(context, "topic"))))));
    }

    /** {@code guide grant|reset <targets> <step|all>}. */
    static LiteralArgumentBuilder<CommandSourceStack> guide() {
        return Commands.literal("guide")
                .then(Commands.literal("grant")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("step", StringArgumentType.word()).suggests(STEPS)
                                        .executes(context -> grantSteps(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                StringArgumentType.getString(context, "step"))))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("step", StringArgumentType.word()).suggests(STEPS)
                                        .executes(context -> resetSteps(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                                StringArgumentType.getString(context, "step"))))));
    }

    private static List<ResearchTopic> topics(String id) throws CommandSyntaxException {
        if (ALL.equals(id)) {
            return ResearchTopics.all();
        }
        ResearchTopic topic = ResearchTopics.byId(id);
        if (topic == null) {
            throw UNKNOWN_TOPIC.create(id);
        }
        return List.of(topic);
    }

    private static Quest step(String id) throws CommandSyntaxException {
        Quest quest = Guide.server().byId(id);
        if (quest == null || quest.kind() != Quest.Kind.ITEM) {
            throw UNKNOWN_STEP.create(id);
        }
        return quest;
    }

    public static int grantResearch(CommandSourceStack source, Collection<ServerPlayer> targets, String id) throws CommandSyntaxException {
        List<ResearchTopic> topics = topics(id);
        int changed = 0;
        for (ServerPlayer player : targets) {
            changed += Research.researchAll(player, topics);
        }
        int done = changed;
        source.sendSuccess(() -> Component.translatable("commands.hextale.research.granted", done, targets.size()), true);
        return changed;
    }

    public static int revokeResearch(CommandSourceStack source, Collection<ServerPlayer> targets, String id) throws CommandSyntaxException {
        List<ResearchTopic> topics = topics(id);
        int changed = 0;
        for (ServerPlayer player : targets) {
            changed += Research.unresearchAll(player, topics);
        }
        int done = changed;
        source.sendSuccess(() -> Component.translatable("commands.hextale.research.revoked", done, targets.size()), true);
        return changed;
    }

    /** The step and every step it waits for, in the guide's order (parents first); every item step for {@code all}. */
    private static List<Quest> withAncestors(String id) throws CommandSyntaxException {
        Guide guide = Guide.server();
        Set<Quest> wanted = new HashSet<>();
        if (ALL.equals(id)) {
            wanted.addAll(guide.all());
        } else {
            List<Quest> todo = new ArrayList<>(List.of(step(id)));
            while (!todo.isEmpty()) {
                Quest next = todo.removeLast();
                if (wanted.add(next)) {
                    todo.addAll(next.parents());
                }
            }
        }
        return guide.all().stream().filter(wanted::contains).filter(quest -> quest.kind() == Quest.Kind.ITEM).toList();
    }

    public static int grantSteps(CommandSourceStack source, Collection<ServerPlayer> targets, String id) throws CommandSyntaxException {
        List<Quest> steps = withAncestors(id);
        int changed = 0;
        for (ServerPlayer player : targets) {
            for (Quest quest : steps) {
                if (Quests.grant(player, quest)) {
                    changed++;
                }
            }
            Research.sync(player);
        }
        int done = changed;
        source.sendSuccess(() -> Component.translatable("commands.hextale.guide.granted", done, targets.size()), true);
        return changed;
    }

    public static int resetSteps(CommandSourceStack source, Collection<ServerPlayer> targets, String id) throws CommandSyntaxException {
        Guide guide = Guide.server();
        Set<Quest> steps = new HashSet<>();
        if (ALL.equals(id)) {
            steps.addAll(guide.all());
        } else {
            Quest quest = step(id);
            steps.add(quest);
            steps.addAll(guide.descendants(quest));
        }
        int changed = 0;
        for (ServerPlayer player : targets) {
            RuneKnowledge knowledge = Research.of(player);
            for (Quest quest : steps) {
                if (knowledge.forgetQuest(quest)) {
                    changed++;
                }
            }
            Research.sync(player);
        }
        int done = changed;
        source.sendSuccess(() -> Component.translatable("commands.hextale.guide.reset", done, targets.size()), true);
        return changed;
    }
}
