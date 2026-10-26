package com.pockyl.hextale.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.VoidAnchorBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.thevoid.TheVoid;
import com.pockyl.hextale.thevoid.Thinning;
import com.pockyl.hextale.thevoid.VoidShape;
import com.pockyl.hextale.thevoid.Voidling;

/** The Void road: a ring of six anchors subdues a breach, a held breach eats and gives birth, voidlings grow. */
public final class VoidRoadGameTests {
    private VoidRoadGameTests() {
    }

    /** One anchor in every sixth of the circle round (1, 4, 1), five blocks out. */
    private static final int[][] RING = {{5, 0}, {3, 4}, {-3, 4}, {-5, 0}, {-3, -4}, {3, -4}};

    private static List<VoidAnchorBlockEntity> ring(GameTestHelper helper) {
        List<VoidAnchorBlockEntity> anchors = new ArrayList<>();
        for (int[] offset : RING) {
            BlockPos at = new BlockPos(1 + offset[0], 1, 1 + offset[1]);
            helper.setBlock(at, ModBlocks.VOID_ANCHOR.get());
            anchors.add(helper.getBlockEntity(at, VoidAnchorBlockEntity.class));
        }
        return anchors;
    }

    @HexTest(maxTicks = 500)
    public static void sixAnchorsRoundSubdueABreachAndFiveDoNot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(1, 4, 1));
        Thinning.setAround(level, centre, 1, TheVoid.FADES_BELOW + 200);
        List<VoidAnchorBlockEntity> anchors = ring(helper);
        for (int i = 0; i < 5; i++) {
            helper.assertTrue(anchors.get(i).insert(new ItemStack(ModItems.HEXTECH_CRYSTAL.get())), "a crystal goes in");
        }
        TheVoid breach = TheVoid.rooted(level, Vec3.atCenterOf(centre), 0, VoidShape.EYE, 3.5F, 880);
        level.addFreshEntity(breach);
        helper.assertTrue(!breach.eat(level, 100, 0), "a free breach is not fed by hand");
        boolean[] closed = {false};
        helper.succeedWhen(() -> {
            if (!closed[0]) {
                helper.assertTrue(breach.heldSides() == 5, "five sides are held, got " + breach.heldSides());
                helper.assertTrue(!breach.subdued() && anchors.get(0).tied() && !anchors.get(0).subdued(),
                        "five anchors tie it but do not subdue it");
                closed[0] = anchors.get(5).insert(new ItemStack(ModItems.HEXTECH_CRYSTAL.get()));
                helper.assertTrue(false, "the sixth anchor gets its crystal");
            }
            helper.assertTrue(breach.subdued(), "the closed ring subdues it, sides " + breach.heldSides());
            for (VoidAnchorBlockEntity anchor : anchors) {
                helper.assertTrue(anchor.tied() && anchor.subdued() && anchor.clouding() > 0, "every anchor holds it and wears");
            }
            breach.discard();
            Thinning.setAround(level, centre, 1, 0);
        });
    }

    @HexTest(maxTicks = 500)
    public static void aHeldBreachEatsTheSicknessOfTheLand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(1, 4, 1));
        LevelChunk chunk = level.getChunkAt(centre);
        Thinning.setAround(level, centre, 1, TheVoid.FADES_BELOW + 200);
        // The sickest land around is the breach's own: that is where what it eats draws from.
        Thinning.set(level, chunk, TheVoid.FADES_BELOW + 400);
        List<VoidAnchorBlockEntity> anchors = ring(helper);
        anchors.forEach(anchor -> anchor.insert(new ItemStack(ModItems.HEXTECH_CRYSTAL.get())));
        // Waste in a jar beside the first anchor: the anchor passes it along its thread.
        BlockPos jarAt = new BlockPos(1 + RING[0][0], 1, 2 + RING[0][1]);
        helper.setBlock(jarAt, ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity jar = helper.getBlockEntity(jarAt, EssenceTankBlockEntity.class);
        jar.tank().set(EssenceMix.unknown(200));
        TheVoid breach = TheVoid.rooted(level, Vec3.atCenterOf(centre), 0, VoidShape.EYE, 3.5F, 881);
        level.addFreshEntity(breach);
        float[] before = {-1};
        ItemEntity[] thrown = {null};
        helper.succeedWhen(() -> {
            helper.assertTrue(breach.subdued(), "the ring holds it");
            if (before[0] < 0) {
                before[0] = Thinning.get(chunk);
                float clouding = anchors.get(1).clouding();
                helper.assertTrue(breach.eat(level, 100, 1), "a held breach eats");
                float drained = before[0] - Thinning.get(chunk);
                helper.assertTrue(Math.abs(drained - 100 * TheVoid.EAT_DRAIN * 2) < 0.5F,
                        "unknown essence draws twice the sickness out of the land, drained " + drained);
                helper.assertTrue(anchors.get(1).clouding() > clouding, "what it eats wears the anchors");
                thrown[0] = new ItemEntity(level, breach.getX(), breach.getY() - 0.125, breach.getZ(), new ItemStack(Items.ROTTEN_FLESH, 3));
                thrown[0].setNoGravity(true);
                thrown[0].setDeltaMovement(Vec3.ZERO);
                level.addFreshEntity(thrown[0]);
                before[0] = Thinning.get(chunk);
            }
            helper.assertTrue(thrown[0].isRemoved(), "what is thrown into it is swallowed");
            helper.assertTrue(jar.tank().contents().unknown() < 200, "the anchor takes the waste from the jar");
            helper.assertTrue(Thinning.get(chunk) < before[0], "and what it eats heals the land, " + Thinning.get(chunk));
            breach.discard();
            Thinning.setAround(level, centre, 1, 0);
        });
    }

    @HexTest(maxTicks = 600)
    public static void aBigMealIsBornAGrownVoidling(GameTestHelper helper) {
        // A creative player: near enough to call the voidling out, but neither hunted nor bitten.
        helper.makeMockServerPlayerInLevel().setGameMode(GameType.CREATIVE);
        ServerLevel level = helper.getLevel();
        BlockPos centre = helper.absolutePos(new BlockPos(1, 4, 1));
        Thinning.setAround(level, centre, 1, TheVoid.FADES_BELOW + 200);
        ring(helper).forEach(anchor -> anchor.insert(new ItemStack(ModItems.HEXTECH_CRYSTAL.get())));
        TheVoid breach = TheVoid.rooted(level, Vec3.atCenterOf(centre), 0, VoidShape.EYE, 3.5F, 882);
        level.addFreshEntity(breach);
        boolean[] fed = {false};
        helper.succeedWhen(() -> {
            helper.assertTrue(breach.subdued(), "the ring holds it");
            if (!fed[0]) {
                fed[0] = breach.eat(level, TheVoid.BIRTH_MB, 0);
            }
            List<Voidling> born = level.getEntitiesOfClass(Voidling.class, breach.getBoundingBox().inflate(12));
            helper.assertTrue(!born.isEmpty(), "what it ate is born");
            helper.assertTrue(born.get(0).stage() == Voidling.GROWN, "a big meal is born grown, stage " + born.get(0).stage());
            born.forEach(Voidling::discard);
            breach.discard();
            Thinning.setAround(level, centre, 1, 0);
        });
    }

    @HexTest
    public static void aVoidlingGrowsAndMolts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Voidling voidling = ModEntities.VOIDLING.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        helper.assertTrue(voidling != null, "a voidling");
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        voidling.snapTo(at.x, at.y, at.z, 0, 0);
        voidling.setNoAi(true);
        level.addFreshEntity(voidling);
        helper.assertTrue(voidling.stage() == Voidling.YOUNG, "it comes out young");
        double scale = voidling.getAttributeValue(Attributes.SCALE);
        float health = voidling.getMaxHealth();
        voidling.feed(level, Voidling.GROWN_AT);
        helper.assertTrue(voidling.stage() == Voidling.GROWN, "fed enough, it molts into a grown one");
        helper.assertTrue(voidling.getAttributeValue(Attributes.SCALE) > scale && voidling.getMaxHealth() > health,
                "bigger and tougher");
        voidling.feed(level, Voidling.ELDER_AT);
        helper.assertTrue(voidling.stage() == Voidling.ELDER, "and at last an elder");
        voidling.feed(level, Voidling.ELDER_AT);
        helper.assertTrue(voidling.stage() == Voidling.ELDER, "an elder grows no more");
        voidling.discard();
        helper.succeed();
    }
}
