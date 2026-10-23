package com.pockyl.hextale.farm;

import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.pockyl.hextale.registry.ModAttachments;

/**
 * Creatures held by Rune Shackles skip their whole tick on the server: no AI, no pathfinding, no physics, no pushing -
 * a held crowd costs about as much as so many statues. A plate renews its hold every second ({@link Shackle#until});
 * when it stops (broken, out of shadow, the creature gone from it) the hold runs out on its own, so no creature is ever
 * left frozen for good. Only what a tick must still do is kept: the blink after a hit wears off (or one arc would make
 * a held creature immune), and a dying creature is let go to fall and vanish.
 */
public final class ShackleEvents {
    private ShackleEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ShackleEvents::onTick);
    }

    private static void onTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide() || !mob.hasData(ModAttachments.SHACKLE)) {
            return;
        }
        Shackle shackle = mob.getData(ModAttachments.SHACKLE);
        if (!held(mob, shackle)) {
            mob.removeData(ModAttachments.SHACKLE);
            return;
        }
        event.setCanceled(true);
        if (mob.getInvulnerableTime() > 0) {
            mob.setInvulnerableTime(mob.getInvulnerableTime() - 1);
        }
        if (mob.damageCooldownTime > 0) {
            mob.damageCooldownTime--;
        }
        if (mob.hurtTime > 0) {
            mob.hurtTime--;
        }
    }

    /** Whether the hold still stands: the creature lives and its plate renewed the hold recently enough. */
    public static boolean held(Mob mob, Shackle shackle) {
        return mob.isAlive() && !mob.isDeadOrDying() && shackle.until() >= mob.level().getGameTime();
    }

    /** A creature's hold: the plate holding it and the tick the hold runs out unless renewed. */
    public record Shackle(long plate, long until) {
    }
}
