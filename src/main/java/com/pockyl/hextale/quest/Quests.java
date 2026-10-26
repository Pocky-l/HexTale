package com.pockyl.hextale.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.registry.ModSounds;

/** Server side of the researcher's guide: notices met goals and reported events and marks their item steps done. */
public final class Quests {
    /** Events the game reports to the guide ({@link #fire}); event goals in the data name them. */
    public static final String BIG_ESSENCE_JAR_FORMED = "big_essence_jar_formed";
    public static final String HEX_FOUNDRY_LIT = "hex_foundry_lit";
    public static final String RUNE_SCROLL_WRITTEN = "rune_scroll_written";
    public static final String HEXCORE_AWAKENED = "hexcore_awakened";
    /** A Void Mite latched onto the player. */
    public static final String VOID_MITE_LATCHED = "void_mite_latched";
    /** A breach tore open near the player (within the distance it is watched from). */
    public static final String BREACH_OPENED = "breach_opened";

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
                complete(knowledge, quest);
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
    private static void complete(RuneKnowledge knowledge, Quest quest) {
        knowledge.completeQuest(quest);
        if (!quest.secret()) {
            knowledge.notice(quest.id());
        }
    }

    /** The reader looked at a step in the book (sent by the client): it is no longer shown as new. */
    public static void noticed(ServerPlayer player, String step) {
        if (Guide.server().byId(step) != null) {
            Research.of(player).notice(step);
        }
    }

    /** Reports an event (one of the names above): every open step whose goal waits for it is done for this player. */
    public static void fire(ServerPlayer player, String event) {
        RuneKnowledge knowledge = Research.of(player);
        Quest last = null;
        for (Quest quest : Guide.server().all()) {
            if (!knowledge.questDone(quest) && Guide.open(quest, knowledge) && quest.answers(event)) {
                complete(knowledge, quest);
                announce(player, quest);
                last = quest;
            }
        }
        if (last != null) {
            done(player, last);
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
