package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import com.pockyl.hextale.abyss.AbyssRift;
import com.pockyl.hextale.abyss.RiftShape;

/** Abyss rifts: they hang where they tore, lie flat when they are fissures, and go once they have shut. */
public final class AbyssGameTests {
    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private AbyssGameTests() {
    }

    @HexTest
    public static void aRiftClosesAndIsGone(GameTestHelper helper) {
        Vec3 centre = Vec3.atCenterOf(helper.absolutePos(AT));
        AbyssRift rift = AbyssRift.open(helper.getLevel(), centre, 30, RiftShape.CLEFT, 4.5F, 12345);
        helper.getLevel().addFreshEntity(rift);
        helper.assertTrue(rift.shape() == RiftShape.CLEFT && rift.size() == 4.5F && rift.seed() == 12345, "the rift keeps its numbers");
        helper.assertTrue(rift.position().distanceTo(centre) < 1.0E-6, "it hangs where it tore");
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(rift.isAlive(), "an open rift stays");
            rift.close();
            helper.assertTrue(rift.closing(), "it is closing");
        });
        helper.succeedWhen(() -> helper.assertTrue(rift.isRemoved(), "a closed rift is gone"));
    }

    @HexTest
    public static void fissuresLieFlat(GameTestHelper helper) {
        for (int seed = 0; seed < 64; seed++) {
            Vector3f lying = AbyssRift.orientation(77, RiftShape.FISSURE, seed * 977).transform(new Vector3f(0, 0, 1));
            helper.assertTrue(Math.abs(lying.y) > 0.9F, "a fissure faces up or down, got " + lying);
            Vector3f standing = AbyssRift.orientation(77, RiftShape.TEAR, seed * 977).transform(new Vector3f(0, 0, 1));
            helper.assertTrue(Math.abs(standing.y) < 0.1F, "a tear stands upright, got " + standing);
        }
        helper.succeed();
    }
}
