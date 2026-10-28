package com.pockyl.hextale.quest;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import com.pockyl.hextale.port.ItemStackTemplate;

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
 * the player holds its (first) icon's item. {@code research} names a research topic the step's item needs first. Without
 * {@code pos} the step finds a place around its first parent by itself. A {@code secret} step stays in the fog until it
 * is done: something that happens to the researcher rather than something to aim for. Texts live in the language files:
 * {@code quest.<namespace>.<id>}, {@code .desc}, {@code .goal}.
 * <p>
 * {@code icon} may list several items: the book switches between them (one step for a set of things, such as the four
 * pieces of the researcher's clothes). {@code display} may list several pictures too, one per icon: the step's plate
 * then shows the icons as a row to pick from, and the picture of the one picked.
 */
public record QuestDefinition(Quest.Kind kind, Region region, Quest.Stage stage, List<ItemStackTemplate> icons, List<String> parents,
                              Optional<QuestGoal> goal, List<QuestDisplay> displays, Optional<ResearchTopic> research, Quest.Aura aura,
                              boolean station, boolean landmark, boolean secret, Optional<Quest.Scene> scene, Optional<Pos> pos) {
    public static final Codec<QuestDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Quest.Kind.CODEC.optionalFieldOf("kind", Quest.Kind.ITEM).forGetter(QuestDefinition::kind),
            Region.CODEC.fieldOf("region").forGetter(QuestDefinition::region),
            Quest.Stage.CODEC.optionalFieldOf("stage", Quest.Stage.EARLY).forGetter(QuestDefinition::stage),
            oneOrMany(ItemStackTemplate.CODEC).fieldOf("icon").forGetter(QuestDefinition::icons),
            Codec.STRING.listOf().optionalFieldOf("parents", List.of()).forGetter(QuestDefinition::parents),
            QuestGoal.CODEC.optionalFieldOf("goal").forGetter(QuestDefinition::goal),
            oneOrMany(QuestDisplay.CODEC).optionalFieldOf("display", List.of()).forGetter(QuestDefinition::displays),
            ResearchTopics.CODEC.optionalFieldOf("research").forGetter(QuestDefinition::research),
            Quest.Aura.CODEC.optionalFieldOf("aura", Quest.Aura.NONE).forGetter(QuestDefinition::aura),
            Codec.BOOL.optionalFieldOf("station", false).forGetter(QuestDefinition::station),
            Codec.BOOL.optionalFieldOf("landmark", false).forGetter(QuestDefinition::landmark),
            Codec.BOOL.optionalFieldOf("secret", false).forGetter(QuestDefinition::secret),
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

    /** A single value or a list of them; written back as a single value when there is only one. */
    private static <T> Codec<List<T>> oneOrMany(Codec<T> codec) {
        return Codec.either(codec, codec.listOf(1, Integer.MAX_VALUE)).xmap(
                either -> either.map(List::of, List::copyOf),
                list -> list.size() == 1 ? Either.left(list.getFirst()) : Either.right(list));
    }

    public QuestDefinition withParents(List<String> next) {
        return new QuestDefinition(kind, region, stage, icons, List.copyOf(next), goal, displays, research, aura, station, landmark, secret, scene, pos);
    }

    /** The first icon changed; the others stay. */
    public QuestDefinition withIcon(ItemStackTemplate next) {
        List<ItemStackTemplate> changed = new java.util.ArrayList<>(icons);
        changed.set(0, next);
        return new QuestDefinition(kind, region, stage, List.copyOf(changed), parents, goal, displays, research, aura, station, landmark, secret, scene, pos);
    }

    public QuestDefinition withAura(Quest.Aura next) {
        return new QuestDefinition(kind, region, stage, icons, parents, goal, displays, research, next, station, landmark, secret, scene, pos);
    }

    public QuestDefinition withStation(boolean next) {
        return new QuestDefinition(kind, region, stage, icons, parents, goal, displays, research, aura, next, landmark, secret, scene, pos);
    }

    public QuestDefinition withPos(Optional<Pos> next) {
        return new QuestDefinition(kind, region, stage, icons, parents, goal, displays, research, aura, station, landmark, secret, scene, next);
    }
}
