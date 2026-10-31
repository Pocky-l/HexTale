package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.block.DeviceCrystals;
import com.pockyl.hextale.block.VoidAnchorBlockEntity;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.thevoid.TheVoid;
import com.pockyl.hextale.thevoid.VoidLayout;

/**
 * Where a ring of anchors goes: while a player holds a Void Anchor or a crystal for one near a breach, the six places
 * round it show on the ground - a side no anchor holds yet pulses magenta with sparks rising from it, a held side
 * glows a calm cold violet. An anchor anywhere in that sixth of the circle, 3 to 12 blocks out, holds it; the marks
 * stand where it fits best.
 */
public final class RingGuide {
    /** How far from a breach a player sees its ring. */
    private static final double SEES = 24;

    private RingGuide() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || minecraft.isPaused() || player.tickCount % 5 != 0 || !building(player)) {
            return;
        }
        AABB around = player.getBoundingBox().inflate(SEES);
        for (TheVoid breach : level.getEntitiesOfClass(TheVoid.class, around, b -> !b.gate() && !b.closing() && !b.foretold())) {
            show(level, breach, player.tickCount);
        }
    }

    /** Holding what a ring is built of: an anchor, or a crystal to set in one. */
    private static boolean building(Player player) {
        for (ItemStack stack : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (stack.is(ModItems.VOID_ANCHOR.get()) || DeviceCrystals.fits(stack)) {
                return true;
            }
        }
        return false;
    }

    private static void show(ClientLevel level, TheVoid breach, int tick) {
        VoidLayout layout = breach.layout();
        double radius = Mth.clamp(layout.reach() + 2.5, 4.5, 9);
        int mask = breach.heldMask();
        RandomSource random = level.getRandom();
        double pulse = (tick / 5) % 4;
        for (int side = 0; side < TheVoid.SIDES; side++) {
            double angle = Math.toRadians(side * 360.0 / TheVoid.SIDES);
            double x = breach.getX() + Math.cos(angle) * radius;
            double z = breach.getZ() + Math.sin(angle) * radius;
            double y = ground(level, x, breach.getY() + layout.bottom() + 1, z);
            boolean held = (mask & 1 << side) != 0;
            if (held) {
                if (pulse == 0) {
                    level.addParticle(ModParticles.ring(VoidAnchorBlockEntity.COLD), x, y + 0.03, z, 0.7, 1, 0);
                }
                continue;
            }
            // A missing side: a ring on the ground every beat and sparks rising, so it can be seen from afar.
            if (pulse == 0 || pulse == 2) {
                level.addParticle(ModParticles.ring(TheVoid.MAGENTA), x, y + 0.03, z, pulse == 0 ? 1.1 : 0.6, 1, 0);
            }
            level.addParticle(ModParticles.glint(random.nextBoolean() ? TheVoid.MAGENTA : TheVoid.PINK),
                    x + (random.nextDouble() - 0.5) * 0.4, y + 0.2 + random.nextDouble() * 1.4, z + (random.nextDouble() - 0.5) * 0.4, 0, 0.02, 0);
        }
    }

    /** The top of the ground under (x, z), looking down from about {@code from}; {@code from} itself when nothing is near. */
    private static double ground(ClientLevel level, double x, double from, double z) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(x, from + 2, z).mutable();
        for (int i = 0; i < 12; i++) {
            if (level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)) {
                return pos.getY() + 1;
            }
            pos.move(Direction.DOWN);
        }
        return Math.floor(from);
    }
}
