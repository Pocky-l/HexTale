package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.block.SieveGlassBlock;
import com.pockyl.hextale.client.render.RuneFx;
import com.pockyl.hextale.network.RuneFxPayload;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

/**
 * A barrier answers the one pushing against it: when the local player walks into Ward Glass (or Veil Glass that keeps
 * them out), a small hexagon of light ripples out on the glass where they touch it - at most every few ticks.
 */
public final class WardRipple {
    private static final int GAP_TICKS = 8;
    /** The ward's amber, as its glass. */
    private static final int AMBER = 0xF0C060;
    private static long next;

    private WardRipple() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.level == null || !player.horizontalCollision || minecraft.level.getGameTime() < next) {
            return;
        }
        Vec3 look = player.getLookAngle();
        double half = player.getBbWidth() / 2;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (look.x * direction.getStepX() + look.z * direction.getStepZ() < 0.3) {
                continue;
            }
            for (double up : new double[] {0.4, 1.4}) {
                Vec3 touch = player.position().add(direction.getStepX() * (half + 0.05), up, direction.getStepZ() * (half + 0.05));
                BlockState state = minecraft.level.getBlockState(BlockPos.containing(touch));
                if (state.getBlock() instanceof SieveGlassBlock glass && !glass.passes(player)) {
                    Vec3 at = player.position().add(direction.getStepX() * (half + 0.02), 1.1, direction.getStepZ() * (half + 0.02));
                    RuneFx.local(new RuneFxPayload(RuneFxPayload.RIPPLE, at, ComplexRunes.of(Essence.LIGHT, Essence.SHADOW), 0.7F, 1,
                            direction.get3DDataValue(), AMBER));
                    next = minecraft.level.getGameTime() + GAP_TICKS;
                    return;
                }
            }
        }
    }
}
