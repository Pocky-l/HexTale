package com.pockyl.hextale.quest;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
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
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechPickaxeItem;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.RuneHolder;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Server side of the researcher's guide: notices met goals and marks their item quests done. */
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
        for (Quest quest : Guide.all()) {
            if (quest.kind() == Quest.Kind.ITEM && !knowledge.questDone(quest) && quest.isMet(player)) {
                knowledge.completeQuest(quest);
                changed = true;
                player.sendSystemMessage(Component.translatable("book.hextale.quest.completed",
                        Component.translatable(quest.translationKey()).withStyle(ChatFormatting.GOLD)));
            }
        }
        if (changed) {
            // Only the researcher hears their path.
            player.connection.send(new ClientboundSoundPacket(ModSounds.QUEST_COMPLETE, SoundSource.PLAYERS, player.getX(), player.getY(),
                    player.getZ(), 0.6F, 1.0F, player.getRandom().nextLong()));
            Research.sync(player);
        }
    }

    /** Marks a quest done that has no goal to check (a structure formed, say): tells the player like any other. */
    public static void complete(ServerPlayer player, Quest quest) {
        RuneKnowledge knowledge = Research.of(player);
        if (knowledge.questDone(quest)) {
            return;
        }
        knowledge.completeQuest(quest);
        player.sendSystemMessage(Component.translatable("book.hextale.quest.completed",
                Component.translatable(quest.translationKey()).withStyle(ChatFormatting.GOLD)));
        player.connection.send(new ClientboundSoundPacket(ModSounds.QUEST_COMPLETE, SoundSource.PLAYERS, player.getX(), player.getY(),
                player.getZ(), 0.6F, 1.0F, player.getRandom().nextLong()));
        Research.sync(player);
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

    /** A hextech pickaxe of at least this tier (1 plain, 2 refined, 3 radiant). */
    static Predicate<ServerPlayer> pickaxe(int tier) {
        return player -> player.getInventory().contains(stack ->
                stack.getItem() instanceof HextechPickaxeItem pickaxe && pickaxe.kind().tier() >= tier);
    }

    /** A hextech pickaxe carrying at least this many runes. */
    static Predicate<ServerPlayer> pickaxeRunes(int runes) {
        return player -> player.getInventory().contains(stack -> HextechPickaxeItem.is(stack) && PickaxeRunes.of(stack).count() >= runes);
    }

    /** A Hex Smelter item carrying at least this many runes. */
    static Predicate<ServerPlayer> pylonRunes(int runes) {
        return player -> player.getInventory().contains(stack -> ModItems.isPylon(stack) && RuneHolder.PYLON.runes(stack).count() >= runes);
    }

    static Predicate<ServerPlayer> smelterRunes(int runes) {
        return player -> player.getInventory().contains(stack -> stack.is(ModItems.HEX_SMELTER.get())
                && RuneHolder.SMELTER.runes(stack).count() >= runes);
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
