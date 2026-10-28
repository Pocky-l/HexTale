package com.pockyl.hextale.knowledge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Locale;
import java.util.Optional;

/**
 * Fires for the players by an altar when its ritual finishes. Conditions: optional {@code player}, {@code item} (an item
 * predicate on what lies on the altar afterwards) and {@code kind} ({@code infuse}, {@code engrave} or {@code write}).
 */
public final class RitualFinishedTrigger extends SimpleCriterionTrigger<RitualFinishedTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    /** @param kind the ritual's kind in lower case: infuse, engrave or write */
    public void trigger(ServerPlayer player, ItemStack result, String kind) {
        trigger(player, instance -> instance.matches(result, kind));
    }

    public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<ItemPredicate> item, Optional<String> kind)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item),
                Codec.STRING.optionalFieldOf("kind").forGetter(TriggerInstance::kind)
        ).apply(instance, TriggerInstance::new));

        public boolean matches(ItemStack result, String fired) {
            return item.map(predicate -> predicate.test(result)).orElse(true)
                    && kind.map(wanted -> wanted.toLowerCase(Locale.ROOT).equals(fired)).orElse(true);
        }
    }
}
