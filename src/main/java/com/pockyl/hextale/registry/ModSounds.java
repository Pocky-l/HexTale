package com.pockyl.hextale.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;

/** The mod's sounds (synthesized or CC0, see tools/sound-gen/hextale_sounds.py in the workspace). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Hextale.MOD_ID);

    /** A soft brush stroke on parchment. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BRUSH_STROKE = register("brush.stroke");
    /** The orb recognises a simple rune; played at the element's pitch. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNE_RECOGNISED = register("rune.recognised");
    /** The orb does not know the drawing. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNE_FAILED = register("rune.failed");
    /** Two simple runes fuse into a complex one. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNE_FUSED = register("rune.fused");
    /** The orb cracks under the same element twice. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ORB_CRACK = register("orb.crack");
    /** Lightning awakens amethyst into unstable hextech. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ORB_AWAKEN = register("orb.awaken");
    /** The alembic bubbles while it boils (CC0 samples, see the sound generator). */
    public static final DeferredHolder<SoundEvent, SoundEvent> ALEMBIC_BUBBLE = register("alembic.bubble");
    /** A drop of concentrate falls in the separator. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SEPARATOR_DRIP = register("separator.drip");
    /** One stroke of the pump. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PUMP_STROKE = register("pump.stroke");
    /** The drain valve's handwheel turns. */
    public static final DeferredHolder<SoundEvent, SoundEvent> VALVE_TURN = register("valve.turn");
    /** A section of a rune's thread soaks up its ink (screen sound, pitched by the essence). */
    public static final DeferredHolder<SoundEvent, SoundEvent> INK_STEP = register("ink.step");
    /** A quest of the researcher's path is done. */
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEST_COMPLETE = register("quest.complete");
    /** Essence poured into or out of a tank. */
    public static final DeferredHolder<SoundEvent, SoundEvent> TANK_GLUG = register("tank.glug");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Hextale.id(name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
