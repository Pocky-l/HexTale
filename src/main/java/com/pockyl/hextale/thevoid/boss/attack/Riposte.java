package com.pockyl.hextale.thevoid.boss.attack;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.network.ProsectorMarkerPayload;
import com.pockyl.hextale.network.ProsectorMarkerPayload.Marker;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.boss.Prosector;
import com.pockyl.hextale.thevoid.boss.ProsectorDamage;
import com.pockyl.hextale.thevoid.boss.ProsectorState.Section;

/**
 * The answer to a blade: it closes in, then stops, lays a wide arc on the floor and sweeps it with a giant copy of the
 * weapon that hit it. The swing itself is drawn from the synced state and {@link Prosector#heldCopy()}.
 */
final class Riposte extends Attack {
    private static final int COLOR = 0xE8E0D0;
    /** Cosine of half the arc: 70 degrees to each side. */
    private static final double HALF_ARC = 0.34;
    private static final double SHOVE = 0.9;
    private Vec3 from = Vec3.ZERO;
    private Vec3 facing = new Vec3(1, 0, 0);
    private boolean locked;

    Riposte(Prosector boss) {
        super(boss, AttackType.RIPOSTE);
    }

    @Override
    protected void startup(ServerLevel level, int tick) {
        int length = section(Section.STARTUP);
        // Behind a Proof the body stays high up: the Proof makes the swing where it stands.
        if (!locked && !targetGone() && !boss.proving()) {
            boss.lookAt(target.getEyePosition());
            Vec3 close = target.position().subtract(target.position().subtract(boss.position()).multiply(1, 0, 1).normalize().scale(4.5));
            boss.flyTo(boss.groundAt(close.x, close.z, target.getY()).add(0, 1.2, 0), 0.35);
        } else {
            boss.hold();
        }
        if (!locked && tick >= lockTick()) {
            locked = true;
            from = boss.groundUnder(boss.attackOrigin());
            Vec3 way = (targetGone() ? boss.getLookAngle() : target.position().subtract(from)).multiply(1, 0, 1);
            facing = way.lengthSqr() < 1.0E-3 ? facing : way.normalize();
            ProsectorMarkerPayload.send(level, Marker.ARC, from, from.add(facing), size(), length - tick + 1, COLOR);
        }
    }

    @Override
    protected void onActive(ServerLevel level) {
        boss.playAt(boss.position(), ModSounds.PROSECTOR_SWEEP.get(), 2.0F, 1.0F);
        for (LivingEntity victim : boss.victims(new AABB(from, from).inflate(size(), 4, size()))) {
            Vec3 to = victim.position().subtract(from).multiply(1, 0, 1);
            double distance = to.length();
            if (distance <= size() && (distance < 1 || to.normalize().dot(facing) >= HALF_ARC) && strike(level, victim)) {
                ProsectorDamage.throwAt(victim, to.normalize().scale(SHOVE).add(0, 0.3, 0));
            }
        }
    }
}
