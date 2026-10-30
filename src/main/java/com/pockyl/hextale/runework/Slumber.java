package com.pockyl.hextale.runework;

import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import com.pockyl.hextale.registry.ModAttachments;

/**
 * The Slumber runes' sleep (Levitation Plate, Soul Flask): a creature put to sleep has no AI for a while, so even archers
 * cannot pick a new target. The sleep is a tick mark ({@link ModAttachments#SLUMBER}) next to vanilla's NoAI flag, and
 * the flag always goes again: when the time is up, when the creature dies, and whenever it comes into a world (loaded
 * from a save, let out of a flask), so a creature is never left without its AI for good. A creature that had NoAI of its
 * own is never touched.
 */
public final class Slumber {
    private Slumber() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Slumber::onTick);
        NeoForge.EVENT_BUS.addListener(Slumber::onJoin);
    }

    /** Puts {@code mob} to sleep for at least {@code ticks} more; false if it already lacked its AI of its own. */
    public static boolean lull(Mob mob, int ticks) {
        long until = mob.level().getGameTime() + ticks;
        if (asleep(mob)) {
            if (mob.getData(ModAttachments.SLUMBER) < until) {
                mob.setData(ModAttachments.SLUMBER, until);
            }
            return true;
        }
        if (mob.isNoAi()) {
            return false;
        }
        mob.setData(ModAttachments.SLUMBER, until);
        mob.setNoAi(true);
        mob.setTarget(null);
        mob.getNavigation().stop();
        return true;
    }

    /** Whether {@code mob} sleeps by a Slumber rune. */
    public static boolean asleep(Mob mob) {
        return mob.hasData(ModAttachments.SLUMBER);
    }

    private static void wake(Mob mob) {
        mob.removeData(ModAttachments.SLUMBER);
        mob.setNoAi(false);
    }

    private static void onTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide() && asleep(mob)
                && (!mob.isAlive() || mob.level().getGameTime() >= mob.getData(ModAttachments.SLUMBER))) {
            wake(mob);
        }
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof Mob mob && !event.getLevel().isClientSide() && asleep(mob)) {
            wake(mob);
        }
    }
}
