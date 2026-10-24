package com.pockyl.hextale.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/** Fires whenever a player's codex changes; met when it holds at least {@code count} complex runes. */
public final class RunesLearnedTrigger extends SimpleCriterionTrigger<RunesLearnedTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, int learned) {
        trigger(player, instance -> learned >= instance.count());
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, int count) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.intRange(1, 1000).optionalFieldOf("count", 1).forGetter(TriggerInstance::count)
        ).apply(instance, TriggerInstance::new));
    }
}
