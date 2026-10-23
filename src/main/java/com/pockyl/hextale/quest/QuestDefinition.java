package com.pockyl.hextale.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;

import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.List;
import java.util.Optional;

/**
 * One step of the researcher's guide as data: a file {@code data/<namespace>/hextale_guide/<id>.json}. Example:
 * <pre>{@code
 * {
 *   "region": "workshop",
 *   "icon": "hextale:research_table",
 *   "parents": ["hex_workbench", "brass"],
 *   "display": {"kind": "hex_crafting", "result": "hextale:research_table"},
 *   "station": true,
 *   "pos": [0, -130]
 * }
 * }</pre>
 * {@code stage} is the part of the game the step belongs to: {@code early} (default), {@code mid} or {@code late}.
 * {@code kind} is {@code item} (default: something to make or obtain) or {@code wip} (a device still being worked out,
 * never done). The first parent is the one the step's link comes from. Without a {@code goal} an item step is done once
 * the player holds its icon's item. {@code research} names a research topic the step's item needs first. Without
 * {@code pos} the step finds a place around its first parent by itself. Texts live in the language files:
 * {@code quest.<namespace>.<id>}, {@code .desc}, {@code .goal}.
 */
public record QuestDefinition(Quest.Kind kind, Region region, Quest.Stage stage, ItemStackTemplate icon, List<String> parents, Optional<QuestGoal> goal,
                              Optional<QuestDisplay> display, Optional<ResearchTopic> research, Quest.Aura aura, boolean station,
                              boolean landmark, Optional<Quest.Scene> scene, Optional<Pos> pos) {
    public static final Codec<QuestDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Quest.Kind.CODEC.optionalFieldOf("kind", Quest.Kind.ITEM).forGetter(QuestDefinition::kind),
            Region.CODEC.fieldOf("region").forGetter(QuestDefinition::region),
            Quest.Stage.CODEC.optionalFieldOf("stage", Quest.Stage.EARLY).forGetter(QuestDefinition::stage),
            ItemStackTemplate.CODEC.fieldOf("icon").forGetter(QuestDefinition::icon),
            Codec.STRING.listOf().optionalFieldOf("parents", List.of()).forGetter(QuestDefinition::parents),
            QuestGoal.CODEC.optionalFieldOf("goal").forGetter(QuestDefinition::goal),
            QuestDisplay.CODEC.optionalFieldOf("display").forGetter(QuestDefinition::display),
            ResearchTopics.CODEC.optionalFieldOf("research").forGetter(QuestDefinition::research),
            Quest.Aura.CODEC.optionalFieldOf("aura", Quest.Aura.NONE).forGetter(QuestDefinition::aura),
            Codec.BOOL.optionalFieldOf("station", false).forGetter(QuestDefinition::station),
            Codec.BOOL.optionalFieldOf("landmark", false).forGetter(QuestDefinition::landmark),
            Quest.Scene.CODEC.optionalFieldOf("scene").forGetter(QuestDefinition::scene),
            Pos.CODEC.optionalFieldOf("pos").forGetter(QuestDefinition::pos)
    ).apply(i, QuestDefinition::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, QuestDefinition> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /** A place on the book's map, in map pixels: {@code [x, y]}. */
    public record Pos(int x, int y) {
        public static final Codec<Pos> CODEC = Codec.INT.listOf().comapFlatMap(
                list -> list.size() == 2 ? DataResult.success(new Pos(list.get(0), list.get(1))) : DataResult.error(() -> "pos needs [x, y]"),
                pos -> List.of(pos.x(), pos.y()));
    }

    public QuestDefinition withParents(List<String> next) {
        return new QuestDefinition(kind, region, stage, icon, List.copyOf(next), goal, display, research, aura, station, landmark, scene, pos);
    }

    public QuestDefinition withIcon(ItemStackTemplate next) {
        return new QuestDefinition(kind, region, stage, next, parents, goal, display, research, aura, station, landmark, scene, pos);
    }

    public QuestDefinition withAura(Quest.Aura next) {
        return new QuestDefinition(kind, region, stage, icon, parents, goal, display, research, next, station, landmark, scene, pos);
    }

    public QuestDefinition withStation(boolean next) {
        return new QuestDefinition(kind, region, stage, icon, parents, goal, display, research, aura, next, landmark, scene, pos);
    }

    public QuestDefinition withPos(Optional<Pos> next) {
        return new QuestDefinition(kind, region, stage, icon, parents, goal, display, research, aura, station, landmark, scene, next);
    }
}
