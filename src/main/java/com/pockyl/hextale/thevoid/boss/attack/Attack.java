package com.pockyl.hextale.thevoid.boss.attack;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.thevoid.boss.Prosector;
import com.pockyl.hextale.thevoid.boss.ProsectorConfig;
import com.pockyl.hextale.thevoid.boss.ProsectorDamage;
import com.pockyl.hextale.thevoid.boss.ProsectorState.Section;

/**
 * One move, in the three sections every move has: startup (the telegraph, its sound first), active, recovery. The base
 * class counts the ticks and moves the Prosector's synced state along; a move only says what happens in each section.
 * A subclass stays a screen long: that is the point of the split.
 */
public abstract class Attack {
    protected final Prosector boss;
    protected final AttackType type;
    protected LivingEntity target;
    /** Where the move aims, kept when the target moves on or dies. */
    protected Vec3 aim = Vec3.ZERO;
    private Section section = Section.STARTUP;
    private int ticks;
    private int length;

    protected Attack(Prosector boss, AttackType type) {
        this.boss = boss;
        this.type = type;
    }

    protected ProsectorConfig.AttackData data() {
        return type.data();
    }

    protected float damage() {
        return data().damage().get().floatValue();
    }

    /** The move's width, radius or strength, as the table has it. */
    protected float size() {
        return data().size().get().floatValue();
    }

    /** The tick of the startup at which the move stops following its target. */
    protected int lockTick() {
        return Math.max(0, section(Section.STARTUP) - boss.timed(data().lock().get()));
    }

    protected boolean strike(ServerLevel level, LivingEntity victim) {
        return ProsectorDamage.strike(level, victim, boss, damage());
    }

    /** Whether this move still watches its target: past the look lock it no longer turns. */
    protected boolean targetGone() {
        return target == null || !target.isAlive();
    }

    protected int section(Section section) {
        return boss.timed(switch (section) {
            case STARTUP -> data().startup().get();
            case ACTIVE -> data().active().get();
            case RECOVERY -> data().recovery().get();
            case NONE -> 0;
        });
    }

    public final void start(ServerLevel level, LivingEntity target) {
        this.target = target;
        this.aim = target.position();
        enter(Section.STARTUP);
        boss.playAt(boss.position(), type.prepare(), 2.0F, type.pitch());
        onStart(level);
    }

    private void enter(Section section) {
        this.section = section;
        this.ticks = 0;
        this.length = section(section);
        boss.enter(type.state(), section, length);
    }

    /** One tick; true once the move is over. */
    public final boolean tick(ServerLevel level) {
        switch (section) {
            case STARTUP -> startup(level, ticks);
            case ACTIVE -> active(level, ticks);
            default -> recovery(level, ticks);
        }
        ticks++;
        if (ticks < length) {
            return false;
        }
        if (section == Section.STARTUP) {
            enter(Section.ACTIVE);
            onActive(level);
            return false;
        }
        if (section == Section.ACTIVE) {
            enter(Section.RECOVERY);
            onRecovery(level);
            return false;
        }
        return true;
    }

    public final Section currentSection() {
        return section;
    }

    /** The move was cut off: whatever it left in the room has to go. */
    public void cancel(ServerLevel level) {
    }

    protected void onStart(ServerLevel level) {
    }

    protected void startup(ServerLevel level, int tick) {
        if (!targetGone()) {
            boss.lookAt(target.getEyePosition());
        }
        boss.hold();
    }

    protected void onActive(ServerLevel level) {
    }

    protected void active(ServerLevel level, int tick) {
        boss.hold();
    }

    protected void onRecovery(ServerLevel level) {
    }

    protected void recovery(ServerLevel level, int tick) {
        boss.hold();
    }
}
