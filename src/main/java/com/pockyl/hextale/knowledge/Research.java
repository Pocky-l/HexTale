package com.pockyl.hextale.knowledge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import com.pockyl.hextale.network.KnowledgeSyncPayload;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModTriggers;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.Collection;

/**
 * Server-side changes to a researcher's knowledge. Every change is synced to the player's client; learning runes
 * also writes the first-rune and milestone notes and updates the codex advancements.
 */
public final class Research {
    private Research() {
    }

    public static RuneKnowledge of(Player player) {
        return player.getData(ModAttachments.KNOWLEDGE);
    }

    /** @return whether the rune was new to the player */
    public static boolean learn(ServerPlayer player, ComplexRune rune) {
        RuneKnowledge knowledge = of(player);
        int before = knowledge.runeCount();
        if (!knowledge.learn(rune)) {
            return false;
        }
        progress(player, before, knowledge.runeCount());
        sync(player);
        return true;
    }

    /** @return how many of the runes were new */
    public static int learnAll(ServerPlayer player, Collection<ComplexRune> runes) {
        RuneKnowledge knowledge = of(player);
        int before = knowledge.runeCount();
        runes.forEach(knowledge::learn);
        int added = knowledge.runeCount() - before;
        if (added > 0) {
            progress(player, before, knowledge.runeCount());
            sync(player);
        }
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
        if (removed > 0) {
            sync(player);
        }
        return removed;
    }

    public static void recordDrawn(ServerPlayer player, SimpleRune element) {
        of(player).recordDrawn(element);
    }

    /** A drawing the orb did not recognise: the first one is worth a note with drawing tips. */
    public static void recordFailure(ServerPlayer player) {
        RuneKnowledge knowledge = of(player);
        knowledge.recordFailure();
        unlockNote(player, DiaryNote.FIRST_FAILURE);
    }

    public static void recordCrack(ServerPlayer player) {
        of(player).recordCrack();
        unlockNote(player, DiaryNote.ORB_CRACKED);
    }

    /** Writes a note into the diary (once); the player sees a hint and hears a page turn. */
    public static boolean unlockNote(ServerPlayer player, DiaryNote note) {
        if (!of(player).addNote(note)) {
            return false;
        }
        player.displayClientMessage(Component.translatable("diary.hextale.note.unlocked",
                Component.translatable(note.translationKey()).withStyle(ChatFormatting.GOLD)), true);
        player.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        sync(player);
        return true;
    }

    public static void markNotesRead(ServerPlayer player) {
        if (of(player).markNotesRead()) {
            sync(player);
        }
    }

    public static void sync(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, new KnowledgeSyncPayload(of(player).copy()));
        ModTriggers.RUNES_LEARNED.get().trigger(player, of(player).runeCount());
    }

    private static void progress(ServerPlayer player, int before, int after) {
        if (before == 0 && after > 0) {
            unlockNote(player, DiaryNote.FIRST_RUNE);
        }
        for (Milestone milestone : Milestone.values()) {
            if (before < milestone.count() && after >= milestone.count()) {
                unlockNote(player, milestone.note());
                player.sendSystemMessage(Component.translatable("diary.hextale.milestone.reached",
                        Component.translatable(milestone.translationKey()).withStyle(ChatFormatting.GOLD), milestone.count()));
            }
        }
    }
}
