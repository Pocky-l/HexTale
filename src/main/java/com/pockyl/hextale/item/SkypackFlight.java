package com.pockyl.hextale.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A skypack flight in two parts: a launch - {@link #BOOST_TICKS} of thrust right after the glide starts - and then a
 * plain glide. The pack is armed by standing on the ground, so only a take-off from the ground launches; opening the
 * glide again in mid-air only glides. Server and clients run the same rules on what they see of each wearer (the
 * ground, the glide), so nothing needs syncing: the server spends the launch's air, every client draws the jets while
 * the boost lasts and the wearer's own client pushes.
 */
public final class SkypackFlight {
    /** Ticks of thrust after a launch. */
    public static final int BOOST_TICKS = 40;
    /** Integrated server and client tick in different threads; their entities are different objects. */
    private static final Map<LivingEntity, State> STATES = Collections.synchronizedMap(new WeakHashMap<>());

    private SkypackFlight() {
    }

    private static final class State {
        boolean armed = true;
        boolean flying;
        long boostEnds = Long.MIN_VALUE;
    }

    /** One tick of a wearer; true on the tick it launches. Call once a tick per side. */
    public static boolean tick(LivingEntity wearer, ItemStack pack, long now) {
        State state = STATES.computeIfAbsent(wearer, entity -> new State());
        if (wearer.onGround() || wearer.isInLiquid()) {
            state.armed = true;
        }
        boolean flying = wearer.isFallFlying();
        boolean launch = flying && !state.flying && state.armed && HextechSkypackItem.canLaunch(pack);
        if (launch) {
            state.armed = false;
            state.boostEnds = now + BOOST_TICKS;
        }
        if (!flying) {
            state.boostEnds = Long.MIN_VALUE;
        }
        state.flying = flying;
        return launch;
    }

    /** Whether the wearer's jets burn now: gliding within the boost of a launch. */
    public static boolean boosting(LivingEntity wearer, long now) {
        State state = STATES.get(wearer);
        return state != null && wearer.isFallFlying() && now < state.boostEnds;
    }

    /** Ticks of boost left (0 when none), for fading the jets out. */
    public static int boostLeft(LivingEntity wearer, long now) {
        State state = STATES.get(wearer);
        return state == null || !wearer.isFallFlying() ? 0 : (int) Math.max(0, state.boostEnds - now);
    }

    public static void forget(LivingEntity wearer) {
        STATES.remove(wearer);
    }
}
