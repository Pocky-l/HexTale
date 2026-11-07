package com.pockyl.hextale.thevoid.boss.attack;

import net.minecraft.sounds.SoundEvent;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.boss.Prosector;
import com.pockyl.hextale.thevoid.boss.ProsectorConfig;
import com.pockyl.hextale.thevoid.boss.ProsectorConfig.AttackData;
import com.pockyl.hextale.thevoid.boss.ProsectorState;
import com.pockyl.hextale.thevoid.boss.StudyKind;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The attack table: each move's state, its numbers (from {@link ProsectorConfig}), its class, the sound that starts its
 * telegraph and that sound's pitch, so an ear learns the set. A mimic is only there while its kind is written down.
 */
public enum AttackType {
    INCISION(ProsectorState.INCISION, ProsectorConfig.INCISION, Incision::new, ModSounds.PROSECTOR_PREPARE_INCISION::get, 1.0F, null),
    PROBE(ProsectorState.PROBE, ProsectorConfig.PROBE, Probe::new, ModSounds.PROSECTOR_PREPARE_PROBE::get, 1.2F, null),
    RETRACTOR(ProsectorState.RETRACTOR, ProsectorConfig.RETRACTOR, Retractor::new, ModSounds.PROSECTOR_PREPARE_RETRACTOR::get, 0.9F, null),
    EXCISION(ProsectorState.EXCISION, ProsectorConfig.EXCISION, Excision::new, ModSounds.PROSECTOR_PREPARE_EXCISION::get, 0.7F, null),
    SECTION(ProsectorState.SECTION, ProsectorConfig.SECTION, FloorSection::new, ModSounds.PROSECTOR_PREPARE_SECTION::get, 0.8F, null),
    RIPOSTE(ProsectorState.RIPOSTE, ProsectorConfig.RIPOSTE, Riposte::new, ModSounds.PROSECTOR_PREPARE_RIPOSTE::get, 1.1F, StudyKind.BLADE),
    VOLLEY(ProsectorState.VOLLEY, ProsectorConfig.VOLLEY, ReturnVolley::new, ModSounds.PROSECTOR_PREPARE_VOLLEY::get, 1.3F, StudyKind.SHOT),
    CHARGE(ProsectorState.CHARGE, ProsectorConfig.CHARGE, ReturnedCharge::new, ModSounds.PROSECTOR_PREPARE_CHARGE::get, 0.75F, StudyKind.BLAST),
    CIRCLE(ProsectorState.CIRCLE, ProsectorConfig.CIRCLE, InvertedCircle::new, ModSounds.PROSECTOR_PREPARE_CIRCLE::get, 1.4F, StudyKind.RUNE);

    private final ProsectorState state;
    private final AttackData data;
    private final Function<Prosector, Attack> factory;
    private final Supplier<SoundEvent> prepare;
    private final float pitch;
    private final @Nullable StudyKind mimicOf;

    AttackType(ProsectorState state, AttackData data, Function<Prosector, Attack> factory, Supplier<SoundEvent> prepare, float pitch,
               @Nullable StudyKind mimicOf) {
        this.state = state;
        this.data = data;
        this.factory = factory;
        this.prepare = prepare;
        this.pitch = pitch;
        this.mimicOf = mimicOf;
    }

    public ProsectorState state() {
        return state;
    }

    public AttackData data() {
        return data;
    }

    public Attack create(Prosector boss) {
        return factory.apply(boss);
    }

    public SoundEvent prepare() {
        return prepare.get();
    }

    public float pitch() {
        return pitch;
    }

    public @Nullable StudyKind mimicOf() {
        return mimicOf;
    }

    /** Whatever the move needs apart from time and distance: a Specimen for a mimic. */
    public boolean ready(Prosector boss) {
        return mimicOf == null || boss.study().written(mimicOf);
    }
}
