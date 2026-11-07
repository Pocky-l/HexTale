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
 * The answer to magic: a rune circle on the floor, laid where the target will be if they keep running (the mark is there
 * for all to see, so stopping or turning beats it). It erupts when the startup ends.
 */
final class InvertedCircle extends Attack {
    private static final int COLOR = 0xB070F0;
    private static final double HEIGHT = 5;
    /** Share of the startup the circle leads the target by. */
    private static final double LEAD = 0.6;

    InvertedCircle(Prosector boss) {
        super(boss, AttackType.CIRCLE);
    }

    @Override
    protected void onStart(ServerLevel level) {
        int warning = section(Section.STARTUP);
        Vec3 run = target.getKnownMovement().multiply(1, 0, 1).scale(warning * LEAD);
        aim = boss.groundUnder(target.position().add(run));
        boss.setAim(aim);
        ProsectorMarkerPayload.send(level, Marker.CIRCLE, aim, aim, size(), warning, COLOR);
    }

    @Override
    protected void onActive(ServerLevel level) {
        boss.playAt(aim, ModSounds.PROSECTOR_CIRCLE.get(), 2.0F, 1.0F);
        ProsectorMarkerPayload.send(level, Marker.ERUPT, aim, aim, size(), section(Section.ACTIVE) + 6, COLOR);
        for (LivingEntity victim : boss.victims(new AABB(aim, aim).inflate(size(), 0, size()).expandTowards(0, HEIGHT, 0))) {
            if (victim.position().subtract(aim).multiply(1, 0, 1).length() <= size()) {
                strike(level, victim);
            }
        }
    }
}
