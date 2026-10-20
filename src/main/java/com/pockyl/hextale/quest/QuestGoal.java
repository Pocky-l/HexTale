package com.pockyl.hextale.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.menu.SeparatorMenu;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.runework.HextechPickaxeItem;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.RuneHolder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * What an item step of the guide asks of the player, as data: {@code {"type": "has_item", "item": "hextale:brass_ingot"}}.
 * Most goals are checked every few ticks ({@link Quests#check}); an {@code event} goal is met when the game reports that
 * event ({@link Quests#fire}) - a structure formed, a block woken, a scroll written. New kinds of goals are added here,
 * not per step.
 */
public interface QuestGoal {
    Map<String, MapCodec<? extends QuestGoal>> TYPES = Map.ofEntries(
            Map.entry("has_item", HasItem.CODEC),
            Map.entry("has_brew", HasBrew.CODEC),
            Map.entry("studied", Studied.CODEC),
            Map.entry("runes_known", RunesKnown.CODEC),
            Map.entry("pickaxe", Pickaxe.CODEC),
            Map.entry("pickaxe_runes", PickaxeRunesCount.CODEC),
            Map.entry("pylon_runes", PylonRunes.CODEC),
            Map.entry("concentrate", Concentrate.CODEC),
            Map.entry("ink_at_table", InkAtTable.CODEC),
            Map.entry("event", Event.CODEC),
            Map.entry("any_of", AnyOf.CODEC));

    Codec<QuestGoal> CODEC = Codec.STRING.dispatch("type", QuestGoal::type, type -> {
        MapCodec<? extends QuestGoal> codec = TYPES.get(type);
        if (codec == null) {
            throw new IllegalArgumentException("Unknown quest goal type " + type);
        }
        return codec;
    });

    /** The goal's type name in the data. */
    String type();

    /** Whether the player meets the goal now (event goals never do: they wait for their event). */
    boolean isMet(ServerPlayer player);

    /** Whether this event meets the goal. */
    default boolean answers(String event) {
        return false;
    }

    /** An item (of this kind) in the inventory. */
    record HasItem(Item item) implements QuestGoal {
        static final MapCodec<HasItem> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(HasItem::item)).apply(i, HasItem::new));

        @Override
        public String type() {
            return "has_item";
        }

        @Override
        public boolean isMet(ServerPlayer player) {
            return player.getInventory().contains(stack -> stack.is(item));
        }
    }

    /** A Chem Vial of this brew in the inventory. */
    record HasBrew(com.pockyl.hextale.chem.Brew brew) implements QuestGoal {
        static final MapCodec<HasBrew> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                com.pockyl.hextale.chem.Brew.CODEC.fieldOf("brew").forGetter(HasBrew::brew)).apply(i, HasBrew::new));

        @Override
        public String type() {
            return "has_brew";
        }

        @Override
        public boolean isMet(ServerPlayer player) {
            return player.getInventory().contains(stack -> com.pockyl.hextale.chem.ChemVialItem.brew(stack) == brew);
        }
    }

    /** At least this many items studied at the table. */
    record Studied(int items) implements QuestGoal {
        static final MapCodec<Studied> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("items", 1).forGetter(Studied::items)).apply(i, Studied::new));

        @Override
        public String type() {
            return "studied";
        }

        @Override
        public boolean isMet(ServerPlayer player) {
            return Research.of(player).studied().size() >= items;
        }
    }

    /** At least this many runes found. */
    record RunesKnown(int runes) implements QuestGoal {
        static final MapCodec<RunesKnown> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("runes", 1).forGetter(RunesKnown::runes)).apply(i, RunesKnown::new));

        @Override
        public String type() {
            return "runes_known";
        }

        @Override
        public boolean isMet(ServerPlayer player) {
            return Research.of(player).runeCount() >= runes;
        }
    }

    /** A hextech pickaxe of at least this tier (1 plain, 2 refined, 3 radiant). */
    record Pickaxe(int tier) implements QuestGoal {
        static final MapCodec<Pickaxe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("tier", 1).forGetter(Pickaxe::tier)).apply(i, Pickaxe::new));

        @Override
        public String type() {
            return "pickaxe";
        }

        @Override
        public boolean isMet(ServerPlayer player) {
            return player.getInventory().contains(stack -> stack.getItem() instanceof HextechPickaxeItem pickaxe && pickaxe.kind().tier() >= tier);
        }
    }

    /** A hextech pickaxe carrying at least this many runes. */
    record PickaxeRunesCount(int runes) implements QuestGoal {
        static final MapCodec<PickaxeRunesCount> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("runes", 1).forGetter(PickaxeRunesCount::runes)).apply(i, PickaxeRunesCount::new));

        @Override
        public String type() {
            return "pickaxe_runes";
        }

        @Override
        public boolean isMet(ServerPlayer player) {
            return player.getInventory().contains(stack -> HextechPickaxeItem.is(stack) && PickaxeRunes.of(stack).count() >= runes);
        }
    }

    /** A hex pylon item carrying at least this many runes. */
    record PylonRunes(int runes) implements QuestGoal {
        static final MapCodec<PylonRunes> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.INT.optionalFieldOf("runes", 1).forGetter(PylonRunes::runes)).apply(i, PylonRunes::new));

        @Override
        public String type() {
            return "pylon_runes";
        }

        @Override
        public boolean isMet(ServerPlayer player) {
            return player.getInventory().contains(stack -> ModItems.isPylon(stack) && RuneHolder.PYLON.runes(stack).count() >= runes);
        }
    }

    /** A bottle of concentrate in the inventory, or a Separator open on the screen with concentrate in it. */
    record Concentrate() implements QuestGoal {
        static final MapCodec<Concentrate> CODEC = MapCodec.unit(new Concentrate());

        @Override
        public String type() {
            return "concentrate";
        }

        @Override
        public boolean isMet(ServerPlayer player) {
            if (player.getInventory().contains(stack -> stack.is(ModItems.ESSENCE_FLASK.get())
                    && EssenceFlaskItem.contents(stack).pureEssence() != null)) {
                return true;
            }
            return player.containerMenu instanceof SeparatorMenu menu
                    && player.level().getBlockEntity(menu.pos()) instanceof SeparatorBlockEntity separator
                    && !separator.extract().isEmpty();
        }
    }

    /** A Research Table open on the screen with ink in the tanks around it. */
    record InkAtTable() implements QuestGoal {
        static final MapCodec<InkAtTable> CODEC = MapCodec.unit(new InkAtTable());

        @Override
        public String type() {
            return "ink_at_table";
        }

        @Override
        public boolean isMet(ServerPlayer player) {
            return player.containerMenu instanceof ResearchTableMenu menu
                    && player.level().getBlockEntity(menu.pos()) instanceof ResearchTableBlockEntity table
                    && Arrays.stream(table.inkSupply().available()).anyMatch(amount -> amount > 0);
        }
    }

    /** Met when the game reports this event (see {@link Quests} for the events it reports). */
    record Event(String event) implements QuestGoal {
        static final MapCodec<Event> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.fieldOf("event").forGetter(Event::event)).apply(i, Event::new));

        @Override
        public String type() {
            return "event";
        }

        @Override
        public boolean isMet(ServerPlayer player) {
            return false;
        }

        @Override
        public boolean answers(String reported) {
            return event.equals(reported);
        }
    }

    /** Met when any of the goals is. */
    record AnyOf(List<QuestGoal> goals) implements QuestGoal {
        static final MapCodec<AnyOf> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.lazyInitialized(() -> QuestGoal.CODEC).listOf().fieldOf("goals").forGetter(AnyOf::goals)).apply(i, AnyOf::new));

        @Override
        public String type() {
            return "any_of";
        }

        @Override
        public boolean isMet(ServerPlayer player) {
            return goals.stream().anyMatch(goal -> goal.isMet(player));
        }

        @Override
        public boolean answers(String event) {
            return goals.stream().anyMatch(goal -> goal.answers(event));
        }
    }
}
