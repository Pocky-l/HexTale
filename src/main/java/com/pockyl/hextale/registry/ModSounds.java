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
    /** The alembic simmers while it boils. */
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
    /** The ritual circle of a Hextech Altar wakes. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_START = register("ritual.start");
    /** The working circle throbs (every two seconds of a ritual). */
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_HUM = register("ritual.hum");
    /** A pedestal item dissolves into the ritual; pitched up as the ritual goes on. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_ABSORB = register("ritual.absorb");
    /** A ritual closes and its result appears. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_COMPLETE = register("ritual.complete");
    /** A ritual waits for something missing. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_STALL = register("ritual.stall");
    /** A ritual breaks. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_COLLAPSE = register("ritual.collapse");
    /** A rune settles into a rune stele. */
    public static final DeferredHolder<SoundEvent, SoundEvent> STELE_SET = register("stele.set");
    /** The Hex Workbench spends a little of its crystal's charge on a craft. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WORKBENCH_CRAFT = register("workbench.craft");
    /** Hextech dust sinks into a block and wakes it. */
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_AWAKEN = register("block.awaken");
    /** The Hex Furnace's low roar while it burns. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FURNACE_ROAR = register("furnace.roar");
    /** A creative scroll unrolling: everything learned, or everything forgotten. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SCROLL_KNOWLEDGE = register("scroll.knowledge");
    /** Essence siphoned out of a jar into a machine that draws from the jars around it. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ESSENCE_DRAW = register("essence.draw");
    /** A gathering pylon pulls items off the ground into its gem. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PYLON_GATHER = register("pylon.gather");
    /** A sending pylon throws items out of its gem. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PYLON_SEND = register("pylon.send");
    /** A flying item lands where it was sent. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PYLON_ARRIVE = register("pylon.arrive");
    /** A link between pylons is tied. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PYLON_LINK = register("pylon.link");
    /** A link between pylons is untied. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PYLON_UNLINK = register("pylon.unlink");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Hextale.id(name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
