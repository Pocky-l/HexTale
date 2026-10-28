package com.pockyl.hextale.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Optional;

/**
 * A trigger for quest makers that fires with a name: a researched topic, a guide step done, a guide event. Its
 * conditions hold an optional {@code player} predicate and an optional name field ({@code topic}, {@code step} or
 * {@code event}); without the name it is met by any.
 */
public final class NamedTrigger extends SimpleCriterionTrigger<NamedTrigger.TriggerInstance> {
    private final Codec<TriggerInstance> codec;

    public NamedTrigger(String field) {
        codec = RecordCodecBuilder.create(instance -> instance.group(
                LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Codec.STRING.optionalFieldOf(field).forGetter(TriggerInstance::name)
        ).apply(instance, TriggerInstance::new));
    }

    @Override
    public Codec<TriggerInstance> codec() {
        return codec;
    }

    public void trigger(ServerPlayer player, String name) {
        trigger(player, instance -> instance.matches(name));
    }

    public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<String> name)
            implements SimpleCriterionTrigger.SimpleInstance {
        /** The name as written or with the mod's namespace: {@code "hexcore"} and {@code "hextale:hexcore"} both match. */
        public boolean matches(String fired) {
            return name.isEmpty() || name.get().equals(fired) || name.get().equals("hextale:" + fired);
        }
    }
}
