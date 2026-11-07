package com.pockyl.hextale.thevoid.boss.attack;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.boss.Prosector;
import com.pockyl.hextale.thevoid.boss.ProsectorConfig;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A thin beam that follows the target a little late: strafing dodges it, a column covers. It stops at the first block,
 * and only whoever it really reaches is burned. The end of the beam travels in the synced aim.
 */
final class Probe extends Attack {
    private final Deque<Vec3> trail = new ArrayDeque<>();

    Probe(Prosector boss) {
        super(boss, AttackType.PROBE);
    }

    private void remember() {
        if (!targetGone()) {
            trail.addLast(target.position().add(0, target.getBbHeight() * 0.6, 0));
        }
        while (trail.size() > ProsectorConfig.PROBE_LAG.get() + 1) {
            trail.removeFirst();
        }
    }

    @Override
    protected void startup(ServerLevel level, int tick) {
        super.startup(level, tick);
        remember();
        if (!trail.isEmpty()) {
            boss.setAim(trail.peekFirst());
        }
    }

    @Override
    protected void onActive(ServerLevel level) {
        boss.playAt(boss.position(), ModSounds.PROSECTOR_BEAM.get(), 1.5F, 1.0F);
    }

    @Override
    protected void active(ServerLevel level, int tick) {
        boss.hold();
        remember();
        if (trail.isEmpty()) {
            return;
        }
        Vec3 from = boss.attackOrigin();
        Vec3 way = trail.peekFirst().subtract(from);
        if (way.lengthSqr() < 1.0E-3) {
            return;
        }
        Vec3 past = from.add(way.normalize().scale(data().maxRange().get()));
        Vec3 end = level.clip(new ClipContext(from, past, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss)).getLocation();
        boss.lookAt(end);
        boss.setAim(end);
        if (tick % ProsectorConfig.PROBE_BURN.get() == 0) {
            for (LivingEntity victim : boss.victims(new AABB(from, end).inflate(0.4 + size()))) {
                if (victim.getBoundingBox().inflate(size()).clip(from, end).isPresent()) {
                    strike(level, victim);
                }
            }
        }
    }

    @Override
    protected void onRecovery(ServerLevel level) {
        boss.setAim(boss.attackOrigin());
    }
}
