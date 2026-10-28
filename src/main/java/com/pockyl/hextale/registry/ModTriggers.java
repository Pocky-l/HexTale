package com.pockyl.hextale.registry;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.knowledge.NamedTrigger;
import com.pockyl.hextale.knowledge.RitualFinishedTrigger;
import com.pockyl.hextale.knowledge.RunesLearnedTrigger;

public final class ModTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, Hextale.MOD_ID);

    /** {@code hextale:runes_learned} with {@code "count": n}: the codex holds at least n complex runes. */
    public static final DeferredHolder<CriterionTrigger<?>, RunesLearnedTrigger> RUNES_LEARNED =
            TRIGGERS.register("runes_learned", RunesLearnedTrigger::new);
    /** {@code hextale:research_completed} with an optional {@code "topic"}: a topic was researched (any without it). */
    public static final DeferredHolder<CriterionTrigger<?>, NamedTrigger> RESEARCH_COMPLETED =
            TRIGGERS.register("research_completed", () -> new NamedTrigger("topic"));
    /** {@code hextale:guide_step} with an optional {@code "step"}: a step of the Researcher's Guide was done. */
    public static final DeferredHolder<CriterionTrigger<?>, NamedTrigger> GUIDE_STEP =
            TRIGGERS.register("guide_step", () -> new NamedTrigger("step"));
    /**
     * {@code hextale:guide_event} with an optional {@code "event"}: the game reported an event to the guide
     * ({@code breach_opened}, {@code hexcore_awakened}, {@code awakened/<block>}, ...).
     */
    public static final DeferredHolder<CriterionTrigger<?>, NamedTrigger> GUIDE_EVENT =
            TRIGGERS.register("guide_event", () -> new NamedTrigger("event"));
    /** {@code hextale:ritual_finished} with optional {@code "item"} (item predicate) and {@code "kind"}: an altar ritual ended. */
    public static final DeferredHolder<CriterionTrigger<?>, RitualFinishedTrigger> RITUAL_FINISHED =
            TRIGGERS.register("ritual_finished", RitualFinishedTrigger::new);

    private ModTriggers() {
    }

    public static void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
    }
}
