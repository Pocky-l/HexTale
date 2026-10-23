package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import com.pockyl.hextale.thevoid.TheVoid;
import com.pockyl.hextale.thevoid.VoidLayout;
import com.pockyl.hextale.thevoid.VoidShape;

/** The Void: it hangs where it broke through, lies flat as a breach in the ground, has a body, and goes once shut. */
public final class VoidGameTests {
    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private VoidGameTests() {
    }

    @HexTest
    public static void theVoidClosesAndIsGone(GameTestHelper helper) {
        Vec3 centre = Vec3.atCenterOf(helper.absolutePos(AT));
        TheVoid breach = TheVoid.open(helper.getLevel(), centre, 30, VoidShape.EYE, 3.5F, 12345);
        helper.getLevel().addFreshEntity(breach);
        helper.assertTrue(breach.shape() == VoidShape.EYE && breach.size() == 3.5F && breach.seed() == 12345, "the Void keeps its numbers");
        helper.assertTrue(breach.position().distanceTo(centre) < 1.0E-6, "it hangs where it broke through");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(breach.isAlive(), "an open breach stays");
            breach.close();
            helper.assertTrue(breach.closing(), "it is closing");
        });
        helper.succeedWhen(() -> helper.assertTrue(breach.isRemoved(), "a closed breach is gone"));
    }

    @HexTest
    public static void breachesLieFlat(GameTestHelper helper) {
        for (int yaw = 0; yaw < 360; yaw += 23) {
            Vector3f lying = TheVoid.orientation(yaw, VoidShape.BREACH).transform(new Vector3f(0, 0, 1));
            helper.assertTrue(Math.abs(lying.y) > 0.99F, "a breach faces up or down, got " + lying);
            Vector3f standing = TheVoid.orientation(yaw, VoidShape.GATE).transform(new Vector3f(0, 0, 1));
            helper.assertTrue(Math.abs(standing.y) < 0.01F, "a gate stands upright, got " + standing);
        }
        helper.succeed();
    }

    @HexTest
    public static void everyOutlineHasABodyInItsGrid(GameTestHelper helper) {
        for (VoidShape shape : VoidShape.values()) {
            for (int seed = 0; seed < 40; seed++) {
                float size = 2 + seed % 5;
                VoidLayout layout = VoidLayout.of(shape, size, seed * 7919);
                helper.assertTrue(layout.bodyCells() >= 6, shape + " " + seed + " has a body");
                helper.assertTrue(layout.reach() < size * 1.6F + 2, shape + " " + seed + " stays near its size");
                // The outermost ring of cells stays empty: the glow around the edge needs it.
                for (int i = 0; i < layout.width; i++) {
                    helper.assertTrue(!layout.on(i, 0) && !layout.on(i, layout.height - 1), shape + " " + seed + " keeps its border free");
                }
                for (int j = 0; j < layout.height; j++) {
                    helper.assertTrue(!layout.on(0, j) && !layout.on(layout.width - 1, j), shape + " " + seed + " keeps its border free");
                }
                helper.assertTrue(layout == VoidLayout.of(shape, size, seed * 7919), "layouts are cached");
            }
        }
        helper.succeed();
    }
}
