package com.pockyl.hextale.knowledge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import com.pockyl.hextale.network.KnowledgeSyncPayload;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModTriggers;
import com.pockyl.hextale.rune.ComplexRune;

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

    /**
     * Records a finished sheet: the rune is learned (or its stars improve).
     *
     * @return whether the rune was new to the player
     */
    public static boolean learn(ServerPlayer player, ComplexRune rune, int stars, int symbols) {
        RuneKnowledge knowledge = of(player);
        int before = knowledge.runeCount();
        boolean fresh = knowledge.learn(rune, stars, symbols);
        progress(player, before, knowledge.runeCount());
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
        runes.forEach(rune -> knowledge.learn(rune, stars, 0));
        int added = knowledge.runeCount() - before;
        progress(player, before, knowledge.runeCount());
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

    public static void sync(ServerPlayer player) {
        ModNetwork.sendToPlayer(player, new KnowledgeSyncPayload(of(player).copy()));
        ModTriggers.RUNES_LEARNED.get().trigger(player, of(player).runeCount());
    }

    private static void progress(ServerPlayer player, int before, int after) {
        for (Milestone milestone : Milestone.values()) {
            if (before < milestone.count() && after >= milestone.count()) {
                player.sendSystemMessage(Component.translatable("diary.hextale.milestone.reached",
                        Component.translatable(milestone.translationKey()).withStyle(ChatFormatting.GOLD), milestone.count()));
            }
        }
    }
}
