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
    /** The Lab Bench: a drop into the flask, a reaction taking off, the vent, a sealed flask, a burst one. */
    public static final DeferredHolder<SoundEvent, SoundEvent> LAB_DROP = register("lab_bench.drop");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAB_REACT = register("lab_bench.react");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAB_VENT = register("lab_bench.vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAB_SEAL = register("lab_bench.seal");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAB_BURST = register("lab_bench.burst");
    /** Shimmer: the axolotl sheds slime, the still drips and mutters, the brazier catches and breathes. */
    public static final DeferredHolder<SoundEvent, SoundEvent> AXOLOTL_TANK_SHED = register("axolotl_tank.shed");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHEM_STILL_DRIP = register("chem_still.drip");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHEM_STILL_BOIL = register("chem_still.boil");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHEM_BOILER_KNOCK = register("chem_boiler.knock");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHEM_BOILER_VENT = register("chem_boiler.vent");
    public static final DeferredHolder<SoundEvent, SoundEvent> PINK_BRAZIER_IGNITE = register("pink_brazier.ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> PINK_BRAZIER_BREATH = register("pink_brazier.breath");
    /** The Hextech Skypack: its jets catching, their steady roar (looped while they burn), a cough when the air is gone. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SKYPACK_IGNITE = register("skypack.ignite");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKYPACK_THRUST = register("skypack.thrust");
    public static final DeferredHolder<SoundEvent, SoundEvent> SKYPACK_SPUTTER = register("skypack.sputter");
    /** The Void: the world breaking open, the depth breathing behind it (looped while it hangs), the breach shutting. */
    public static final DeferredHolder<SoundEvent, SoundEvent> THE_VOID_OPEN = register("the_void.open");
    public static final DeferredHolder<SoundEvent, SoundEvent> THE_VOID_HUM = register("the_void.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> THE_VOID_CLOSE = register("the_void.close");
    /** The ground groaning and a riser climbing before a breach tears open. */
    public static final DeferredHolder<SoundEvent, SoundEvent> THE_VOID_OMEN = register("the_void.omen");
    /** A breach throwing back what touched it. */
    public static final DeferredHolder<SoundEvent, SoundEvent> THE_VOID_REPEL = register("the_void.repel");
    /** A voidling pushing out of a breach and dropping into the world. */
    public static final DeferredHolder<SoundEvent, SoundEvent> VOIDLING_EMERGE = register("voidling.emerge");
    /** A voidling molting into a bigger one. */
    public static final DeferredHolder<SoundEvent, SoundEvent> VOIDLING_GROW = register("voidling.grow");
    /** The anchors' ring closing round a breach: it is held. */
    public static final DeferredHolder<SoundEvent, SoundEvent> THE_VOID_SUBDUE = register("the_void.subdue");
    /** A held breach swallowing what it is fed. */
    public static final DeferredHolder<SoundEvent, SoundEvent> THE_VOID_EAT = register("the_void.eat");
    /** A Void Anchor's thread tied to a breach, and snapping when its crystal clouds through. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ANCHOR_TIE = register("void_anchor.tie");
    public static final DeferredHolder<SoundEvent, SoundEvent> ANCHOR_SNAP = register("void_anchor.snap");
    /** A held breach folding into its heart. */
    public static final DeferredHolder<SoundEvent, SoundEvent> THE_VOID_FOLD = register("the_void.fold");
    /** A Breach Heart beating, and letting go as it unfolds. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HEART_BEAT = register("breach_heart.beat");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEART_UNFOLD = register("breach_heart.unfold");
    /** Void ground healing back into earth. */
    public static final DeferredHolder<SoundEvent, SoundEvent> LAND_HEAL = register("land.heal");
    /** A nullstone drinking the Thinning out of the ground. */
    public static final DeferredHolder<SoundEvent, SoundEvent> NULLSTONE_DRINK = register("nullstone.drink");
    /** A Purifier drawing the Thinning out of the land into its crystal. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFIER_DRAW = register("purifier.draw");
    /** A Purifier's crystal clouding over completely. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFIER_CLOUDED = register("purifier.clouded");
    /** A hextech lamp's gem choking on sick land. */
    public static final DeferredHolder<SoundEvent, SoundEvent> LAMP_FLICKER = register("hextech_lamp.flicker");
    /** The Thinning: a distant murmur of the land where the world wore thin. */
    public static final DeferredHolder<SoundEvent, SoundEvent> THINNING_MURMUR = register("thinning.murmur");
    /** The orb recognises a simple rune. */
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
    /** A step of the Void's branch is done: a slow pen stroke over a dark breath. */
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEST_VOID = register("quest.void");
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
    /** A creative scroll unrolling: everything learned, or everything forgotten. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SCROLL_KNOWLEDGE = register("scroll.knowledge");
    /** Essence siphoned out of a jar into a machine that draws from the jars around it. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ESSENCE_DRAW = register("essence.draw");
    /** A crucible at a rolling boil: heavy bubbles breaking in an iron pot. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUCIBLE_BOIL = register("crucible.boil");
    /** Something sinks into boiling water and dissolves: a hiss and a low gulp. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUCIBLE_DISSOLVE = register("crucible.dissolve");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUCIBLE_POUR = register("crucible.pour");
    /** The water in a crucible turns into a brew: a deep gulp, a rising fizz, a soft settling bloom. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUCIBLE_BREW = register("crucible.brew");
    /** A Chem Burner catches: a soft whump of gas and a breathy rise. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHEM_BURNER_IGNITE = register("chem_burner.ignite");
    /** A lit Chem Burner: a low, throaty gas roar. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHEM_BURNER_ROAR = register("chem_burner.roar");
    /** A thrown Chem Vial splashes: a wet slap and a spreading fizz. */
    public static final DeferredHolder<SoundEvent, SoundEvent> VIAL_SPLASH = register("chem_vial.splash");
    /** Acid eats stone: a long seething hiss with crackles. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ACID_HISS = register("acid.hiss");
    /** The loupe arm is lifted off its rest (lower: set back); a lens swings in; the last lens has seen an item's essences. */
    public static final DeferredHolder<SoundEvent, SoundEvent> LOUPE_GRAB = register("loupe.grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOUPE_LENS = register("loupe.lens");
    public static final DeferredHolder<SoundEvent, SoundEvent> LOUPE_STUDIED = register("loupe.studied");
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
    /** A whirl leaves a Whirl Reaper's crystal. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WHIRL_LAUNCH = register("whirl.launch");
    /** A whirl reaps a plant. */
    public static final DeferredHolder<SoundEvent, SoundEvent> WHIRL_REAP = register("whirl.reap");
    /** A Hex Sawmill's saw flies out. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SAW_LAUNCH = register("saw.launch");
    /** The saw bites into a trunk. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SAW_CUT = register("saw.cut");
    /** The sawn tree creaks and starts to fall. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SAW_FELL = register("saw.fell");
    /** The tree hits the ground. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SAW_LAND = register("saw.land");
    /** A Greedy Funnel draws a death in. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FUNNEL_SWALLOW = register("funnel.swallow");
    /** A Greedy Funnel's crystal lets its experience go to a player. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FUNNEL_RELEASE = register("funnel.release");
    /** A Hex Arc Spire's arc strikes. */
    public static final DeferredHolder<SoundEvent, SoundEvent> ARC_STRIKE = register("arc.strike");
    /** Rune Shackles take hold of a creature. */
    public static final DeferredHolder<SoundEvent, SoundEvent> SHACKLE_BIND = register("shackle.bind");
    /** The Grapple Gauntlet: the claw thrown, biting into a block, the rope reeling, the claw springing back. */
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAPPLE_FIRE = register("grapple.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAPPLE_ATTACH = register("grapple.attach");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAPPLE_REEL = register("grapple.reel");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAPPLE_RELEASE = register("grapple.release");
    /** The Impulse Plate fires; what it threw at a target lands softly. */
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPULSE_LAUNCH = register("impulse.launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPULSE_LAND = register("impulse.land");
    /** Lesser Rift Gates: two tied together, something drawn in, something coming out, a gate left alone. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_LINK = register("rift_gate.link");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_SEND = register("rift_gate.send");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_ARRIVE = register("rift_gate.arrive");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFT_BREAK = register("rift_gate.break");
    /** The Levitation Plate: something taken up, the column humming while it carries (a loop), something let go. */
    public static final DeferredHolder<SoundEvent, SoundEvent> LEVITATION_LIFT = register("levitation_plate.lift");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEVITATION_HUM = register("levitation_plate.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEVITATION_RELEASE = register("levitation_plate.release");
    /** Binding Chalk: a stroke on stone, and a drawn figure closing. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHALK_SCRATCH = register("binding_chalk.scratch");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHALK_CLOSE = register("binding_chalk.close");
    /** A Miasma Vent breathes out its fumes. */
    public static final DeferredHolder<SoundEvent, SoundEvent> MIASMA_HISS = register("miasma.hiss");
    /** A rune doing its work (every proc, played by ToolEffects.puff; throttled per place). */
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNE_PROC = register("rune.proc");
    /** A rune learned from a scroll or notes. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNE_LEARN = register("rune.learn");
    /** A charged crystal answering: a slow glassy hum (replaces the vanilla amethyst resonance). */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRYSTAL_RESONATE = register("crystal.resonate");
    /** Choosing something in a book or on a board. */
    public static final DeferredHolder<SoundEvent, SoundEvent> UI_SELECT = register("ui.select");
    /** A device, board or ritual refuses: a dull muffled knock. */
    public static final DeferredHolder<SoundEvent, SoundEvent> DEVICE_REFUSE = register("device.refuse");
    /** Unstable hextech thrown. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HEXTECH_THROW = register("hextech.throw");
    /** A burner's or brazier's flame going out. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FLAME_OUT = register("flame.out");
    /** Water flashing into steam: a crucible boiled dry, a coolant brew quenching fire. */
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUCIBLE_EVAPORATE = register("crucible.evaporate");
    /** The Hexcore lashing out (and waking), a chain lightning rune leaping. */
    public static final DeferredHolder<SoundEvent, SoundEvent> HEXCORE_STRIKE = register("hexcore.strike");
    /** A stuck Dust Charge sparking up to its burst (as long as its fuse). */
    public static final DeferredHolder<SoundEvent, SoundEvent> DUST_CHARGE_FUSE = register("dust_charge.fuse");
    /** A voidling's own voice: ambient, hurt, death, attack, steps. */
    public static final DeferredHolder<SoundEvent, SoundEvent> VOIDLING_AMBIENT = register("voidling.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOIDLING_HURT = register("voidling.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOIDLING_DEATH = register("voidling.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOIDLING_ATTACK = register("voidling.attack");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOIDLING_STEP = register("voidling.step");
    /** A void mite's own voice: ambient, hurt, death, crawling; mites latching on and shaken off. */
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_MITE_AMBIENT = register("void_mite.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_MITE_HURT = register("void_mite.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_MITE_DEATH = register("void_mite.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_MITE_STEP = register("void_mite.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_MITE_LATCH = register("void_mite.latch");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_MITE_SHAKE = register("void_mite.shake");
    /** A melt settling in the Hex Furnace or the Hex Foundry, and the foundry catching fire when it is built. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_MELT = register("forge.melt");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_CATCH = register("forge.catch");
    /** Loops of working machines (client `MachineLoops`): a ritual, the Hex Furnace, the Hex Foundry, the separator. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RITUAL_LOOP = register("ritual.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORGE_BURN = register("forge.burn");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOUNDRY_ROAR = register("foundry.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> SEPARATOR_SIMMER = register("separator.simmer");
    /** Loops of a pylon moving things and of a Hextech Purifier drawing the Thinning in. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PYLON_HUM = register("pylon.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> PURIFIER_LOOP = register("purifier.loop");
    /** Quiet sounds of the rune effect families (`RuneEffectPayload`): lightning, fire, crumbling stone, rising light, wind. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNE_CRACKLE = register("rune.crackle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNE_KINDLE = register("rune.kindle");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNE_CRUMBLE = register("rune.crumble");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNE_BLOOM = register("rune.bloom");
    public static final DeferredHolder<SoundEvent, SoundEvent> RUNE_GUST = register("rune.gust");
    /** The Thinning eating into the ground (its own voice instead of the vanilla sculk spread). */
    public static final DeferredHolder<SoundEvent, SoundEvent> THINNING_BLIGHT = register("thinning.blight");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Hextale.id(name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
