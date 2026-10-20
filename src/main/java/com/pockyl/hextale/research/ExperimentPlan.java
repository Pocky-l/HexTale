package com.pockyl.hextale.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import com.pockyl.hextale.chem.Reactions;
import com.pockyl.hextale.crafting.DataRecipes;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * What the Lab Bench asks to work out a chemtech topic, read from {@code data/<namespace>/hextale_experiment/<name>.json}:
 * make {@code amount} parts of the {@code target} compound in the flask, at least {@code purity} of the liquid, and seal
 * it with the {@code sample} lying on the bench (a brew's catalyst; it goes into the sealed flask). A topic without a
 * file asks for the compound of its two theme essences.
 */
public record ExperimentPlan(ResearchTopic topic, Essence target, int amount, float purity, Optional<Ingredient> sample) {
    public static final Codec<ExperimentPlan> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResearchTopics.CODEC.fieldOf("topic").forGetter(ExperimentPlan::topic),
            Essence.ID_CODEC.fieldOf("target").forGetter(ExperimentPlan::target),
            Codec.intRange(1, 24).optionalFieldOf("amount", 6).forGetter(ExperimentPlan::amount),
            Codec.floatRange(0.1F, 1).optionalFieldOf("purity", 0.6F).forGetter(ExperimentPlan::purity),
            Ingredient.CODEC.optionalFieldOf("sample").forGetter(ExperimentPlan::sample)
    ).apply(instance, ExperimentPlan::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExperimentPlan> STREAM_CODEC = StreamCodec.composite(
            ResearchTopics.STREAM_CODEC.cast(), ExperimentPlan::topic,
            Essence.STREAM_CODEC.cast(), ExperimentPlan::target,
            ByteBufCodecs.VAR_INT, ExperimentPlan::amount,
            ByteBufCodecs.FLOAT, ExperimentPlan::purity,
            ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC), ExperimentPlan::sample,
            ExperimentPlan::new);

    /** Every experiment, server and client. */
    public static final DataRecipes<ExperimentPlan> ALL = new DataRecipes<>("hextale_experiment", CODEC);

    /** The experiment of a Lab Bench topic: its file, or the compound of its two theme essences. */
    public static ExperimentPlan of(ResearchTopic topic, boolean clientSide) {
        for (ExperimentPlan plan : ALL.get(clientSide).values()) {
            if (plan.topic() == topic) {
                return plan;
            }
        }
        Essence pair = Essence.pair(topic.action(), topic.target());
        return new ExperimentPlan(topic, pair == null ? Essence.STEAM : pair, 6, 0.6F, Optional.empty());
    }

    public boolean needsSample() {
        return sample.isPresent();
    }

    public boolean sampleFits(ItemStack stack) {
        return sample.isEmpty() || !stack.isEmpty() && sample.get().test(stack);
    }

    /** The reactions that build the target from primals, the first to make first, the target last. */
    public List<Essence> chain() {
        List<Essence> out = new ArrayList<>();
        walk(target, out);
        return out;
    }

    private static void walk(Essence essence, List<Essence> out) {
        if (essence.isPrimal() || out.contains(essence)) {
            return;
        }
        for (Essence parent : essence.parents()) {
            walk(parent, out);
        }
        out.add(essence);
    }

    /** The primals the chain starts from, in the order they first appear. */
    public List<Essence> reagents() {
        List<Essence> out = new ArrayList<>();
        for (Essence compound : chain()) {
            for (Essence parent : compound.parents()) {
                if (parent.isPrimal() && !out.contains(parent)) {
                    out.add(parent);
                }
            }
        }
        return out;
    }

    /** The zone the target forms in. */
    public int zone() {
        return Reactions.zone(target);
    }

    /** The plan for a topic id, or null when the topic is unknown. */
    public static @Nullable ExperimentPlan byTopicId(String id, boolean clientSide) {
        ResearchTopic topic = ResearchTopics.byId(id);
        return topic == null || !topic.lab() ? null : of(topic, clientSide);
    }
}
