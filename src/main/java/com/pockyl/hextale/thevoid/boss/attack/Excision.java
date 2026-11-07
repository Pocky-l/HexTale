package com.pockyl.hextale.thevoid.boss.attack;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.network.ProsectorMarkerPayload;
import com.pockyl.hextale.network.ProsectorMarkerPayload.Marker;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.boss.HexCells;
import com.pockyl.hextale.thevoid.boss.HexPlug;
import com.pockyl.hextale.thevoid.boss.Prosector;
import com.pockyl.hextale.thevoid.boss.ProsectorState.Section;

/**
 * The heavy one. It lifts a hexagon plug out of the ground under it and brings it down on a marker that follows the target
 * until the lock; the scalpel then sticks and it is open for the whole recovery. Falling from above, the plug is also the
 * answer to a pillar: it hits whoever stands up there and leaves the blocks alone. {@link com.pockyl.hextale.thevoid.boss.Decider}'s pity count
 * makes sure this opening always comes.
 */
final class Excision extends Attack {
    private static final int COLOR = 0xE040C8;
    /** Ticks before the fall at which the plug is cut out and rises. */
    private static final int RISE_BEFORE = 40;
    /** Blocks over the marker it falls from. */
    private static final double FALL_HEIGHT = 12;
    private @Nullable HexPlug plug;
    private boolean locked;
    private boolean cut;

    Excision(Prosector boss) {
        super(boss, AttackType.EXCISION);
    }

    /** The place it will lift the plug from glows from the start. */
    @Override
    protected void onStart(ServerLevel level) {
        aim = boss.groundUnder(target.position());
        Vec3 middle = boss.groundUnder(boss.position());
        ProsectorMarkerPayload.send(level, Marker.HEX, middle, middle, HexCells.CELL, section(Section.STARTUP) - boss.timed(RISE_BEFORE), COLOR);
    }

    @Override
    protected void startup(ServerLevel level, int tick) {
        super.startup(level, tick);
        int length = section(Section.STARTUP);
        if (!locked && !targetGone()) {
            aim = boss.groundUnder(target.position());
            boss.setAim(aim);
        }
        if (!cut && tick >= length - boss.timed(RISE_BEFORE)) {
            cut = true;
            cutPlug(level);
        }
        if (!locked && tick >= lockTick()) {
            locked = true;
            ProsectorMarkerPayload.send(level, Marker.HEX, aim, aim, size(), length - tick + 1, COLOR);
        }
        if (plug != null) {
            plug.moveTo(aim.add(0, FALL_HEIGHT, 0), 0.08);
        }
    }

    /** The plug comes up from under the Prosector. */
    private void cutPlug(ServerLevel level) {
        Vec3 from = boss.groundUnder(boss.position());
        HexPlug made = ModEntities.HEX_PLUG.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (made != null) {
            made.setPos(from);
            level.addFreshEntity(made);
            plug = made;
        }
        boss.playAt(from, ModSounds.PROSECTOR_PLUG_RISE.get(), 2.0F, 1.0F);
    }

    @Override
    protected void active(ServerLevel level, int tick) {
        boss.hold();
        if (plug != null) {
            float fallen = (tick + 1) / (float) section(Section.ACTIVE);
            plug.setPos(aim.add(0, FALL_HEIGHT * (1 - fallen), 0));
        }
    }

    @Override
    protected void onRecovery(ServerLevel level) {
        if (plug != null) {
            plug.discard();
            plug = null;
        }
        boss.playAt(aim, ModSounds.PROSECTOR_PLUG_SLAM.get(), 3.0F, 1.0F);
        ProsectorMarkerPayload.send(level, Marker.SLAM, aim, aim, size(), 24, COLOR);
        AABB box = new AABB(aim, aim).inflate(size(), 0, size()).expandTowards(0, FALL_HEIGHT, 0).expandTowards(0, -1, 0);
        for (LivingEntity victim : boss.victims(box)) {
            if (HexCells.hex(victim.getX() - aim.x, victim.getZ() - aim.z) <= size()) {
                strike(level, victim);
            }
        }
    }

    @Override
    public void cancel(ServerLevel level) {
        if (plug != null) {
            plug.discard();
            plug = null;
        }
    }

    /** The scalpel stuck in the ground: it sinks with the blow and stays there. */
    @Override
    protected void recovery(ServerLevel level, int tick) {
        boss.flyTo(new Vec3(boss.getX(), boss.floorHeight() + 0.8, boss.getZ()), 0.25);
    }
}
