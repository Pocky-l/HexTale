package com.pockyl.hextale.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.registry.ModTriggers;
import com.pockyl.hextale.research.ResearchTopic;

/** Server side of the researcher's guide: notices met goals and reported events and marks their item steps done. */
public final class Quests {
    /** Events the game reports to the guide ({@link #fire}); event goals in the data name them. */
    public static final String BIG_ESSENCE_JAR_FORMED = "big_essence_jar_formed";
    public static final String HEX_FOUNDRY_LIT = "hex_foundry_lit";
    public static final String RUNE_SCROLL_WRITTEN = "rune_scroll_written";
    public static final String HEXCORE_AWAKENED = "hexcore_awakened";
    /** A figure drawn in Binding Chalk closed near the player. */
    public static final String BINDING_FIGURE = "binding_figure";
    /** A Void Mite latched onto the player. */
    public static final String VOID_MITE_LATCHED = "void_mite_latched";
    /** A breach tore open near the player (within the distance it is watched from). */
    public static final String BREACH_OPENED = "breach_opened";
    /** A nullstone near the player drank the Thinning out of sick land. */
    public static final String NULL_WARD_DRANK = "null_ward_drank";
    /** Void ground near the player healed back into earth or rock. */
    public static final String LAND_HEALED = "land_healed";
    /** A breach near the player starved and closed. */
    public static final String BREACH_FADED = "breach_faded";
    /** The anchors near the player closed their ring round a breach. */
    public static final String BREACH_SUBDUED = "breach_subdued";
    /** A subdued breach near the player ate essence or things thrown into it. */
    public static final String VOID_ATE = "void_ate";
    /** A fed breach near the player pushed out a voidling. */
    public static final String VOID_BIRTH = "void_birth";
    /** A subdued breach near the player stands on land healed below the point where it would starve. */
    public static final String BREACH_KEPT = "breach_kept";
    /** A voidling near the player grew. */
    public static final String VOIDLING_GREW = "voidling_grew";
    /** A held breach near the player folded into its heart. */
    public static final String BREACH_FOLDED = "breach_folded";
    /** The player let a Breach Heart unfold into a breach again. */
    public static final String BREACH_UNFOLDED = "breach_unfolded";
    /** A player stepped through a held breach into the Void. */
    public static final String VOID_ENTERED = "void_entered";

    /** The event of a block woken with hextech dust (its id path), for the one who woke it. */
    public static String awakened(String block) {
        return "awakened/" + block;
    }

    /** How often a player's open quests are checked. */
    public static final int CHECK_TICKS = 10;
    private Quests() {
    }

    /**
     * Marks every open step whose goal the player meets now; tells the player and syncs the knowledge if any. The path is
     * walked in order: a step counts only once every step it waits for is done (the guide lists parents first, so a
     * chain the player has already gone through completes in one check).
     */
    public static void check(ServerPlayer player) {
        RuneKnowledge knowledge = Research.of(player);
        Guide guide = Guide.server();
        if (!knowledge.noticing()) {
            startNoticing(player, knowledge, guide);
        }
        Quest last = null;
        for (Quest quest : guide.all()) {
            if (quest.kind() == Quest.Kind.ITEM && !knowledge.questDone(quest) && Guide.open(quest, knowledge) && quest.isMet(player, guide)) {
                complete(player, knowledge, quest);
                announce(player, quest);
                last = quest;
            }
        }
        if (last != null) {
            done(player, last);
        }
    }

    /**
     * Knowledge saved before the book marked new steps: whatever the reader can already see counts as taken in, so an old
     * path does not light up all at once. A researcher who has done nothing yet keeps the first steps new.
     */
    private static void startNoticing(ServerPlayer player, RuneKnowledge knowledge, Guide guide) {
        java.util.List<String> seen = new java.util.ArrayList<>();
        if (knowledge.anyQuestDone()) {
            for (Quest quest : guide.all()) {
                if (Guide.visible(quest, knowledge)) {
                    seen.add(quest.id());
                }
            }
        }
        knowledge.startNoticing(seen);
        Research.sync(player);
    }

    /** A step done on purpose is taken in as it is done; a secret one happened by itself and waits to be looked at. */
    private static void complete(ServerPlayer player, RuneKnowledge knowledge, Quest quest) {
        knowledge.completeQuest(quest);
        if (!quest.secret()) {
            knowledge.notice(quest.id());
        }
        ModTriggers.GUIDE_STEP.get().trigger(player, quest.id());
    }

    /**
     * Marks a step done for an operator's command, quietly: no message or sound; the caller syncs the knowledge. A
     * research step researches its topic too, else the step would be done with its item still locked.
     *
     * @return whether it was not done before
     */
    public static boolean grant(ServerPlayer player, Quest quest) {
        RuneKnowledge knowledge = Research.of(player);
        if (quest.kind() != Quest.Kind.ITEM || knowledge.questDone(quest)) {
            return false;
        }
        complete(player, knowledge, quest);
        ResearchTopic topic = quest.topic();
        if (topic != null && Guide.server().forTopic(topic) == quest && knowledge.research(topic)) {
            ModTriggers.RESEARCH_COMPLETED.get().trigger(player, topic.id());
        }
        return true;
    }

    /** The reader looked at a step in the book (sent by the client): it is no longer shown as new. */
    public static void noticed(ServerPlayer player, String step) {
        if (Guide.server().byId(step) != null) {
            Research.of(player).notice(step);
        }
    }

    /** Reports an event (one of the names above): every open step whose goal waits for it is done for this player. */
    public static void fire(ServerPlayer player, String event) {
        ModTriggers.GUIDE_EVENT.get().trigger(player, event);
        RuneKnowledge knowledge = Research.of(player);
        Quest last = null;
        for (Quest quest : Guide.server().all()) {
            if (!knowledge.questDone(quest) && Guide.open(quest, knowledge) && quest.answers(event)) {
                complete(player, knowledge, quest);
                announce(player, quest);
                last = quest;
            }
        }
        if (last != null) {
            done(player, last);
        }
    }

    /** Reports an event to every player within {@code radius} blocks of {@code pos}. */
    public static void fireNear(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 pos, double radius, String event) {
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator() && player.position().distanceToSqr(pos) <= radius * radius) {
                fire(player, event);
            }
        }
    }

    /** The Void's steps are written in a darker ink. */
    private static final int VOID_INK = 0xB0407E;

    private static void announce(ServerPlayer player, Quest quest) {
        Component name = quest.region() == Region.VOID ? Component.translatable(quest.translationKey()).withColor(VOID_INK)
                : Component.translatable(quest.translationKey()).withStyle(ChatFormatting.GOLD);
        player.sendSystemMessage(Component.translatable("book.hextale.quest.completed", name));
    }

    /** Only the researcher hears their path (the Void's steps in a sound of their own); the client learns what is done. */
    private static void done(ServerPlayer player, Quest last) {
        var sound = last.region() == Region.VOID ? ModSounds.QUEST_VOID : ModSounds.QUEST_COMPLETE;
        player.connection.send(new ClientboundSoundPacket(sound, SoundSource.PLAYERS, player.getX(), player.getY(),
                player.getZ(), 0.6F, 1.0F, player.getRandom().nextLong()));
        Research.sync(player);
    }
}
