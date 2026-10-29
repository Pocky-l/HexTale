package com.pockyl.hextale.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.rune.ComplexRune;

import java.util.List;
import java.util.Optional;

/**
 * Rune scrolls in the world's treasure chests - the only place runes are found. One modifier per kind of place
 * ({@code data/hextale/loot_modifiers/rune_scrolls_*.json}): the chest loot tables it applies to, the chance a chest holds
 * a scroll, how often the scroll is of each chapter (plain dungeons hold chapter I runes, the deepest places chapter III)
 * and how often it carries one, two or three stars - pure luck.
 *
 * @param tables   the chest loot tables (as queried: a vault's table, a dungeon's chest)
 * @param chance   the chance one opened chest holds a scroll
 * @param chapters weights of chapters I, II and III
 * @param stars    weights of one, two and three stars
 */
public final class RuneScrollLoot extends LootModifier {
    private static final Codec<List<Integer>> THREE_WEIGHTS = Codec.intRange(0, 10_000).listOf(3, 3);
    public static final MapCodec<RuneScrollLoot> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).and(instance.group(
            Identifier.CODEC.listOf().fieldOf("tables").forGetter(loot -> loot.tables),
            Codec.floatRange(0, 1).fieldOf("chance").forGetter(loot -> loot.chance),
            THREE_WEIGHTS.fieldOf("chapters").forGetter(loot -> loot.chapters),
            THREE_WEIGHTS.optionalFieldOf("stars", List.of(60, 30, 10)).forGetter(loot -> loot.stars)
    )).apply(instance, RuneScrollLoot::new));

    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Hextale.MOD_ID);

    static {
        SERIALIZERS.register("rune_scrolls", () -> CODEC);
    }

    private final List<Identifier> tables;
    private final float chance;
    private final List<Integer> chapters;
    private final List<Integer> stars;

    public RuneScrollLoot(LootItemCondition[] conditions, int priority, List<Identifier> tables, float chance,
                          List<Integer> chapters, List<Integer> stars) {
        super(conditions, priority);
        this.tables = List.copyOf(tables);
        this.chance = chance;
        this.chapters = List.copyOf(chapters);
        this.stars = List.copyOf(stars);
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!tables.contains(context.getQueriedLootTableId())) {
            return loot;
        }
        RandomSource random = context.getRandom();
        if (random.nextFloat() >= chance) {
            return loot;
        }
        ItemStack scroll = roll(random, chapters, stars);
        if (!scroll.isEmpty()) {
            loot.add(scroll);
        }
        return loot;
    }

    /** A scroll of a random rune of a chapter picked by weight, with stars picked by weight; empty when no chapter weighs. */
    public static ItemStack roll(RandomSource random, List<Integer> chapters, List<Integer> stars) {
        int chapter = pick(random, chapters) + 1;
        if (chapter <= 0) {
            return ItemStack.EMPTY;
        }
        List<ComplexRune> runes = ResearchBook.findable(chapter);
        ComplexRune rune = runes.get(random.nextInt(runes.size()));
        return RuneScrollItem.of(rune, Math.max(0, pick(random, stars)) + 1, ModItems.RUNE_SCROLL.get());
    }

    /** An index picked by weight, or -1 when all weights are zero. */
    private static int pick(RandomSource random, List<Integer> weights) {
        int total = weights.stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return -1;
        }
        int roll = random.nextInt(total);
        for (int i = 0; i < weights.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
                return i;
            }
        }
        return weights.size() - 1;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
