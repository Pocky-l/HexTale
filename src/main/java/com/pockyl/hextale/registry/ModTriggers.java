package com.pockyl.hextale.registry;

import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.knowledge.RunesLearnedTrigger;

public final class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, Hextale.MOD_ID);

    /** {@code hextale:runes_learned} with {@code "count": n}: the codex holds at least n complex runes. */
    public static final DeferredHolder<CriterionTrigger<?>, RunesLearnedTrigger> RUNES_LEARNED =
            TRIGGERS.register("runes_learned", RunesLearnedTrigger::new);

    private ModTriggers() {
    }

    public static void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
    }
}
