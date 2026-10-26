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
    public static void everyBreachStandsUpright(GameTestHelper helper) {
        for (VoidShape shape : VoidShape.values()) {
            for (int yaw = 0; yaw < 360; yaw += 23) {
                Vector3f normal = TheVoid.orientation(yaw, shape).transform(new Vector3f(0, 0, 1));
                helper.assertTrue(Math.abs(normal.y) < 0.01F, "a " + shape.id() + " stands upright, got " + normal);
            }
        }
        helper.assertTrue(VoidShape.byId("breach") == VoidShape.GATE, "a lying breach of an old save stands up as a gate");
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

    @HexTest
    public static void breachesKeepTheirDistance(GameTestHelper helper) {
        com.pockyl.hextale.thevoid.Breaches.Sites sites = new com.pockyl.hextale.thevoid.Breaches.Sites();
        sites.add(new BlockPos(100, 64, 100));
        helper.assertTrue(sites.near(new BlockPos(100 + 990, 10, 100)) != null, "no second breach within a thousand blocks");
        helper.assertTrue(sites.near(new BlockPos(100 + 700, 200, 100 + 700)) != null, "the distance runs across the ground");
        helper.assertTrue(sites.near(new BlockPos(100 + 1010, 64, 100)) == null, "beyond a thousand blocks another may open");
        helper.succeed();
    }

    @HexTest
    public static void theLandRestsAfterABreachIsGone(GameTestHelper helper) {
        com.pockyl.hextale.thevoid.Breaches.Sites sites = new com.pockyl.hextale.thevoid.Breaches.Sites();
        BlockPos gone = new BlockPos(100, 64, 100);
        sites.rest(gone, 1000);
        helper.assertTrue(sites.resting(new BlockPos(100 + 990, 70, 100), 500), "no new breach near a lately gone one");
        helper.assertTrue(!sites.resting(new BlockPos(100 + 1010, 64, 100), 500), "far off the land does not rest");
        helper.assertTrue(!sites.resting(gone, 1000), "the rest ends");

        // A rooted breach that is gone (starved, folded, closed) leaves its land resting.
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 4, 1));
        TheVoid breach = TheVoid.rooted(level, Vec3.atCenterOf(at), 0, VoidShape.GATE, 3.5F, 885);
        level.addFreshEntity(breach);
        com.pockyl.hextale.thevoid.Breaches.sites(level).add(breach.blockPosition());
        breach.discard();
        com.pockyl.hextale.thevoid.Breaches.Sites real = com.pockyl.hextale.thevoid.Breaches.sites(level);
        helper.assertTrue(real.near(at) == null || real.near(at).distSqr(at) > 16 * 16, "its site is forgotten");
        helper.assertTrue(real.resting(at, level.getGameTime()), "but the land around it rests");
        helper.assertTrue(!real.resting(at, level.getGameTime() + com.pockyl.hextale.Config.BREACH_COOLDOWN.get() * 1200L + 1),
                "for the configured minutes");
        helper.succeed();
    }

    @HexTest
    public static void touchingTheVoidHurtsAndThrowsBack(GameTestHelper helper) {
        Vec3 centre = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 3, 1)));
        TheVoid breach = TheVoid.open(helper.getLevel(), centre, 0, VoidShape.GATE, 3.0F, 4242);
        helper.getLevel().addFreshEntity(breach);
        net.minecraft.world.entity.animal.pig.Pig pig = net.minecraft.world.entity.EntityTypes.PIG.create(helper.getLevel(),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        helper.assertTrue(pig != null, "a pig to touch it");
        pig.snapTo(centre.x, centre.y - pig.getBbHeight() * 0.5, centre.z, 0, 0);
        pig.setNoAi(true);
        helper.getLevel().addFreshEntity(pig);
        float full = pig.getHealth();
        helper.succeedWhen(() -> helper.assertTrue(pig.getHealth() < full && pig.getDeltaMovement().lengthSqr() > 0.01 || pig.isDeadOrDying(),
                "what touches the Void is hurt and thrown back"));
    }

    @HexTest(maxTicks = 400)
    public static void aRootedBreachSendsOutAVoidling(GameTestHelper helper) {
        // A creative player: near enough to call the voidling out, but neither hunted nor bitten.
        GameTests.watcher(helper, helper.absolutePos(new BlockPos(1, 4, 1)));
        Vec3 centre = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 4, 1)));
        TheVoid breach = TheVoid.rooted(helper.getLevel(), centre, 0, VoidShape.EYE, 3.5F, 777);
        helper.getLevel().addFreshEntity(breach);
        helper.assertTrue(breach.foretold() && breach.reveal(0) == 0, "the omen comes first, nothing shows yet");
        helper.succeedWhen(() -> {
            helper.assertTrue(breach.isAlive() && !breach.closing(), "a rooted breach stays open");
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(com.pockyl.hextale.thevoid.Voidling.class,
                    breach.getBoundingBox().inflate(12)).isEmpty(), "a voidling pushes out of it (age " + breach.age(0) + ", push "
                    + breach.push(0) + ", voidlings anywhere " + helper.getLevel().getEntities(com.pockyl.hextale.registry.ModEntities.VOIDLING.get(),
                    v -> true).size() + ", difficulty " + helper.getLevel().getDifficulty() + ", players " + helper.getLevel().players().size() + ")");
        });
    }
}
