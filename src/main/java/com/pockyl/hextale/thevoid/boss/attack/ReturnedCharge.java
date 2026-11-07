package com.pockyl.hextale.thevoid.boss.attack;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.network.ProsectorMarkerPayload;
import com.pockyl.hextale.network.ProsectorMarkerPayload.Marker;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.thevoid.boss.Cyst;
import com.pockyl.hextale.thevoid.boss.Prosector;
import com.pockyl.hextale.thevoid.boss.ProsectorState.Section;

/**
 * The answer to a grenade: a cyst lobbed where the target stands, a ring on the floor from the moment it leaves the
 * forceps until it bursts. Phase two throws a second one a little ahead of the target, phase three a third.
 */
final class ReturnedCharge extends Attack {
    private static final int COLOR = 0xFFB050;
    /** How far ahead of the target's run the extra cysts land, in ticks of it. */
    private static final double LEAD = 20;

    ReturnedCharge(Prosector boss) {
        super(boss, AttackType.CHARGE);
    }

    @Override
    protected void onActive(ServerLevel level) {
        Vec3 at = targetGone() ? aim : boss.groundUnder(target.position());
        int flight = section(Section.ACTIVE);
        lob(level, at, flight);
        if (!targetGone()) {
            Vec3 run = target.getKnownMovement().multiply(1, 0, 1).scale(LEAD);
            for (int i = 1; i < boss.phase(); i++) {
                lob(level, boss.groundUnder(target.position().add(run.scale(i))), flight);
            }
        }
    }

    private void lob(ServerLevel level, Vec3 landing, int flight) {
        Cyst cyst = ModEntities.CYST.get().create(level);
        if (cyst == null) {
            return;
        }
        cyst.setOwner(boss);
        cyst.setPos(boss.attackOrigin());
        cyst.lob(landing, flight, damage(), size());
        level.addFreshEntity(cyst);
        ProsectorMarkerPayload.send(level, Marker.RING, landing, landing, size(), flight, COLOR);
    }
}
