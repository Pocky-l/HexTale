package com.pockyl.hextale.thevoid.boss.attack;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.network.ProsectorMarkerPayload;
import com.pockyl.hextale.network.ProsectorMarkerPayload.Marker;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.boss.Prosector;
import com.pockyl.hextale.thevoid.boss.ProsectorState.Section;
import com.pockyl.hextale.thevoid.boss.Quill;

/** The answer to a bow: quills fly back along the line the last shots came from, a fan that leaves the sides safe. */
final class ReturnVolley extends Attack {
    private static final int COLOR = 0x8CE0C0;
    /** Ticks between two quills, and how fast a quill flies. */
    private static final int EVERY = 2;
    private static final double SPEED = 1.3;
    private Vec3 from = Vec3.ZERO;
    private Vec3 way = new Vec3(1, 0, 0);

    ReturnVolley(Prosector boss) {
        super(boss, AttackType.VOLLEY);
    }

    @Override
    protected void onStart(ServerLevel level) {
        from = boss.attackOrigin();
        Vec3 shot = boss.lastShotFrom() != null ? boss.lastShotFrom() : target.getEyePosition();
        way = shot.subtract(from).normalize();
        Vec3 end = from.add(way.scale(data().maxRange().get().floatValue() / 2));
        ProsectorMarkerPayload.send(level, Marker.LINE, boss.groundUnder(from), boss.groundUnder(end), 2.4F,
                section(Section.STARTUP) + section(Section.ACTIVE), COLOR);
        boss.setAim(end);
        boss.lookAt(end);
    }

    @Override
    protected void startup(ServerLevel level, int tick) {
        boss.hold();
    }

    @Override
    protected void active(ServerLevel level, int tick) {
        boss.hold();
        if (tick % EVERY != 0) {
            return;
        }
        Quill quill = ModEntities.QUILL.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (quill == null) {
            return;
        }
        Vec3 side = new Vec3(boss.getRandom().nextGaussian(), boss.getRandom().nextGaussian() * 0.4, boss.getRandom().nextGaussian()).scale(size());
        quill.setOwner(boss);
        quill.setPos(from);
        quill.setDamage(damage());
        quill.setDeltaMovement(way.add(side).normalize().scale(SPEED));
        level.addFreshEntity(quill);
        boss.playAt(from, ModSounds.PROSECTOR_QUILL.get(), 1.0F, 0.9F + boss.getRandom().nextFloat() * 0.3F);
    }
}
