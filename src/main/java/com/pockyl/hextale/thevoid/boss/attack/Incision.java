package com.pockyl.hextale.thevoid.boss.attack;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.network.ProsectorMarkerPayload;
import com.pockyl.hextale.network.ProsectorMarkerPayload.Marker;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.boss.Prosector;
import com.pockyl.hextale.thevoid.boss.ProsectorState.Section;

/**
 * A line is drawn along the floor through the target, following them; at the lock it stops, and when the startup ends
 * it opens. A sidestep after the lock is the whole dodge. The live line travels in the Prosector's synced aim, so the
 * client draws it where the server has it.
 */
final class Incision extends Attack {
    private static final int COLOR = 0xE040C8;
    /** How high over the line the cut still reaches. */
    private static final double HEIGHT = 2.5;
    private Vec3 from = Vec3.ZERO;
    private Vec3 end = Vec3.ZERO;
    private boolean locked;

    Incision(Prosector boss) {
        super(boss, AttackType.INCISION);
    }

    @Override
    protected void startup(ServerLevel level, int tick) {
        boss.hold();
        if (locked) {
            return;
        }
        from = boss.groundUnder(boss.attackOrigin());
        if (!targetGone()) {
            boss.lookAt(target.getEyePosition());
            aim = boss.groundUnder(target.position());
        }
        end = boss.groundUnder(reach(from, aim));
        boss.setAim(end);
        if (tick >= lockTick()) {
            locked = true;
            ProsectorMarkerPayload.send(level, Marker.LINE, from, end, size(), section(Section.STARTUP) - tick, COLOR);
        }
    }

    /** The line runs through the target to the end of its reach. */
    private Vec3 reach(Vec3 start, Vec3 through) {
        Vec3 way = through.subtract(start).multiply(1, 0, 1);
        if (way.lengthSqr() < 0.01) {
            way = boss.getLookAngle().multiply(1, 0, 1);
        }
        return start.add(way.normalize().scale(data().maxRange().get()));
    }

    @Override
    protected void onActive(ServerLevel level) {
        boss.playAt(end, ModSounds.PROSECTOR_CUT.get(), 2.0F, 1.0F);
        ProsectorMarkerPayload.send(level, Marker.CUT, from, end, size(), 10, COLOR);
        ProsectorMarkerPayload.send(level, Marker.SHAKE, from, end, 0.4F, 10, 0);
        for (LivingEntity victim : boss.victims(new AABB(from, end).inflate(size(), HEIGHT * 2, size()))) {
            if (onLine(victim.position())) {
                strike(level, victim);
            }
        }
    }

    private boolean onLine(Vec3 at) {
        Vec3 line = end.subtract(from).multiply(1, 0, 1);
        double length = line.length();
        Vec3 way = line.scale(1 / length);
        Vec3 to = at.subtract(from).multiply(1, 0, 1);
        double along = to.dot(way);
        double across = to.subtract(way.scale(along)).length();
        return along >= 0 && along <= length && across <= size() && Math.abs(at.y - boss.groundUnder(at).y) <= HEIGHT;
    }
}
