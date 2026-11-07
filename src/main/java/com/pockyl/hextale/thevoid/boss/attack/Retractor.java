package com.pockyl.hextale.thevoid.boss.attack;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.network.ProsectorMarkerPayload;
import com.pockyl.hextale.network.ProsectorMarkerPayload.Marker;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.boss.Prosector;
import com.pockyl.hextale.thevoid.boss.ProsectorDamage;
import com.pockyl.hextale.thevoid.boss.ProsectorState.Section;

/**
 * Two hooks fly out and pull the target in. A raised shield catches them: they snag, and it is the Prosector that reels.
 * That is the shield's one job in this fight. A block in the way stops the hooks. The tips travel in the synced aim.
 */
final class Retractor extends Attack {
    Retractor(Prosector boss) {
        super(boss, AttackType.RETRACTOR);
    }

    /** The line the hooks will take is there to see while they draw back. */
    @Override
    protected void startup(ServerLevel level, int tick) {
        super.startup(level, tick);
        if (!targetGone()) {
            boss.setAim(target.position().add(0, target.getBbHeight() * 0.6, 0));
        }
    }

    @Override
    protected void onActive(ServerLevel level) {
        boss.playAt(boss.position(), ModSounds.PROSECTOR_HOOK.get(), 1.5F, 1.0F);
    }

    @Override
    protected void active(ServerLevel level, int tick) {
        boss.hold();
        if (targetGone()) {
            return;
        }
        Vec3 from = boss.attackOrigin();
        aim = target.position().add(0, target.getBbHeight() * 0.6, 0);
        float flown = (tick + 1) / (float) section(Section.ACTIVE);
        boss.lookAt(aim);
        boss.setAim(from.lerp(aim, flown));
    }

    @Override
    protected void onRecovery(ServerLevel level) {
        if (targetGone()) {
            return;
        }
        Vec3 from = boss.attackOrigin();
        Vec3 at = target.getEyePosition();
        if (level.clip(new ClipContext(from, at, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss)).getType() == HitResult.Type.BLOCK) {
            return;
        }
        if (target.isBlocking()) {
            boss.playAt(at, ModSounds.PROSECTOR_SNAG.get(), 1.6F, 1.0F);
            ProsectorMarkerPayload.send(level, Marker.GLYPH, at, at, 1.5F, 16, 0xFFD27A);
            boss.reel();
            return;
        }
        strike(level, target);
        Vec3 way = from.subtract(target.position());
        ProsectorDamage.throwAt(target, way.normalize().scale(size()).add(0, 0.25, 0));
    }
}
