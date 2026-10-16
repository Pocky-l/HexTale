package com.pockyl.hextale.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;

import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.menu.SeparatorMenu;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Server side of the researcher's path: notices met goals and marks their quests done. */
public final class Quests {
    /** How often a player's open quests are checked. */
    public static final int CHECK_TICKS = 10;
    /** What the bottle quest's icon holds. */
    static final EssenceMix SAMPLE_MIX = EssenceMix.of(Map.of(Essence.FIRE, 120, Essence.EARTH, 80, Essence.LIGHT, 50));

    private Quests() {
    }

    /** Marks every quest whose goal the player meets now; tells the player and syncs the knowledge if any. */
    public static void check(ServerPlayer player) {
        RuneKnowledge knowledge = Research.of(player);
        boolean changed = false;
        for (Quest quest : Quest.values()) {
            if (!knowledge.questDone(quest) && quest.isMet(player)) {
                knowledge.completeQuest(quest);
                changed = true;
                player.sendSystemMessage(Component.translatable("book.hextale.quest.completed",
                        Component.translatable(quest.translationKey()).withStyle(ChatFormatting.GOLD)));
            }
        }
        if (changed) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.3F);
            Research.sync(player);
        }
    }

    static Predicate<ServerPlayer> has(Supplier<? extends Item> item) {
        return player -> player.getInventory().contains(stack -> stack.is(item.get()));
    }

    static Predicate<ServerPlayer> studied(int items) {
        return player -> Research.of(player).studied().size() >= items;
    }

    static Predicate<ServerPlayer> knowledge(Predicate<RuneKnowledge> test) {
        return player -> test.test(Research.of(player));
    }

    /** A bottle of concentrate in the inventory, or a Separator open on the screen with concentrate in it. */
    static boolean concentrate(ServerPlayer player) {
        if (player.getInventory().contains(stack -> stack.is(ModItems.ESSENCE_FLASK.get())
                && EssenceFlaskItem.contents(stack).pureEssence() != null)) {
            return true;
        }
        return player.containerMenu instanceof SeparatorMenu menu
                && player.level().getBlockEntity(menu.pos()) instanceof SeparatorBlockEntity separator
                && !separator.extract().isEmpty();
    }

    /** A Research Table open on the screen with ink in the tanks around it. */
    static boolean inkAtTable(ServerPlayer player) {
        return player.containerMenu instanceof ResearchTableMenu menu
                && player.level().getBlockEntity(menu.pos()) instanceof ResearchTableBlockEntity table
                && Arrays.stream(table.inkSupply().available()).anyMatch(amount -> amount > 0);
    }
}
