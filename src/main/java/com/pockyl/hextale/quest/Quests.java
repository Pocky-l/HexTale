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
    public static final String RUNE_SCROLL_WRITTEN = "rune_scroll_written";

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
        boolean changed = false;
        for (Quest quest : guide.all()) {
            if (quest.kind() == Quest.Kind.ITEM && !knowledge.questDone(quest) && Guide.open(quest, knowledge) && quest.isMet(player, guide)) {
                knowledge.completeQuest(quest);
                announce(player, quest);
                changed = true;
            }
        }
        if (changed) {
            done(player);
        }
    }

    /** Reports an event (one of the names above): every open step whose goal waits for it is done for this player. */
    public static void fire(ServerPlayer player, String event) {
        RuneKnowledge knowledge = Research.of(player);
        boolean changed = false;
        for (Quest quest : Guide.server().all()) {
            if (!knowledge.questDone(quest) && Guide.open(quest, knowledge) && quest.answers(event)) {
                knowledge.completeQuest(quest);
                announce(player, quest);
                changed = true;
            }
        }
        if (changed) {
            done(player);
        }
    }

    private static void announce(ServerPlayer player, Quest quest) {
        player.sendSystemMessage(Component.translatable("book.hextale.quest.completed",
                Component.translatable(quest.translationKey()).withStyle(ChatFormatting.GOLD)));
    }

    /** Only the researcher hears their path; the client learns what is done. */
    private static void done(ServerPlayer player) {
        player.connection.send(new ClientboundSoundPacket(ModSounds.QUEST_COMPLETE, SoundSource.PLAYERS, player.getX(), player.getY(),
                player.getZ(), 0.6F, 1.0F, player.getRandom().nextLong()));
        Research.sync(player);
    }
}
