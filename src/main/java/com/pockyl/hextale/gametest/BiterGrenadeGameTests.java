package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.entity.BiterGrenade;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchTopics;

import java.util.List;

/** The Biter Grenade: it bites what it hits, rides along, bursts on time, breaks nothing, and a miss costs nothing. */
public final class BiterGrenadeGameTests {
    private BiterGrenadeGameTests() {
    }

    private static Husk target(GameTestHelper helper, BlockPos at) {
        Husk husk = helper.spawn(EntityTypes.HUSK, at);
        husk.setNoAi(true);
        return husk;
    }

    /** A grenade flying from {@code from} (relative), thrown by {@code thrower}. */
    private static BiterGrenade thrown(GameTestHelper helper, ServerPlayer thrower, Vec3 from, Vec3 motion) {
        BiterGrenade grenade = new BiterGrenade(helper.getLevel(), thrower, new ItemStack(ModItems.BITER_GRENADE.get()));
        Vec3 at = helper.absoluteVec(from);
        grenade.setPos(at.x, at.y, at.z);
        grenade.setDeltaMovement(motion);
        helper.getLevel().addFreshEntity(grenade);
        return grenade;
    }

    /** A thrower high above the test, out of the blast. */
    private static ServerPlayer farThrower(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        Vec3 far = helper.absoluteVec(new Vec3(0.5, 40, 0.5));
        player.snapTo(far.x, far.y, far.z, 0, 0);
        return player;
    }

    @HexTest(maxTicks = 120, padding = 4)
    public static void aBiterGrenadeLatchesRidesAndBurstsOnTime(GameTestHelper helper) {
        Husk husk = target(helper, new BlockPos(4, 1, 2));
        float health = husk.getHealth();
        BiterGrenade grenade = thrown(helper, farThrower(helper), new Vec3(1.5, 2.0, 2.5), new Vec3(0.8, 0, 0));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(grenade.target() == husk, "it holds on to the creature it hit");
            helper.assertTrue(grenade.fuseLeft() > 0 && grenade.fuseLeft() < BiterGrenade.FUSE, "its fuse burns: " + grenade.fuseLeft());
            helper.assertValueEqual(husk.getHealth(), health, "the bite itself does not hurt");
            Vec3 moved = husk.position().add(0, 0, 1);
            husk.snapTo(moved.x, moved.y, moved.z, 0, 0);
        });
        helper.runAfterDelay(13, () -> helper.assertTrue(grenade.distanceTo(husk) < 1.5, "it rides along: " + grenade.distanceTo(husk)));
        // Thrown at tick 0, it bites within the first few ticks: still armed well into the fuse, gone after it.
        helper.runAfterDelay(BiterGrenade.FUSE - 5, () -> helper.assertTrue(!grenade.isRemoved(), "it waits for the fuse"));
        helper.runAfterDelay(BiterGrenade.FUSE + 10, () -> {
            helper.assertTrue(grenade.isRemoved(), "it has burst");
            helper.assertTrue(husk.getHealth() <= health - BiterGrenade.BLAST * 0.9F, "the blast hurt: " + husk.getHealth());
            husk.discard();
            helper.succeed();
        });
    }

    @HexTest(maxTicks = 120, padding = 4)
    public static void aBittenCreatureDyingLeavesTheGrenadeArmed(GameTestHelper helper) {
        Husk husk = target(helper, new BlockPos(4, 1, 2));
        BiterGrenade grenade = thrown(helper, farThrower(helper), new Vec3(1.5, 2.0, 2.5), new Vec3(0.8, 0, 0));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(grenade.target() == husk, "it bit");
            husk.discard();
        });
        helper.runAfterDelay(BiterGrenade.FUSE + 10, () -> {
            helper.assertTrue(grenade.isRemoved(), "it burst anyway");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, grenade.getBoundingBox().inflate(6));
            helper.assertTrue(drops.stream().noneMatch(item -> item.getItem().is(ModItems.BITER_GRENADE.get())), "a burst grenade leaves no item");
            helper.succeed();
        });
    }

    @HexTest(maxTicks = 120, padding = 4)
    public static void aMissDropsTheGrenadeUnspent(GameTestHelper helper) {
        Husk husk = target(helper, new BlockPos(1, 1, 1));
        float health = husk.getHealth();
        BiterGrenade grenade = thrown(helper, farThrower(helper), new Vec3(3.5, 3.0, 3.5), new Vec3(0, -0.6, 0));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(grenade.isRemoved(), "it landed");
            List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, grenade.getBoundingBox().inflate(3));
            helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(ModItems.BITER_GRENADE.get())), "it drops as an item");
            helper.assertValueEqual(husk.getHealth(), health, "nothing went off");
            drops.forEach(ItemEntity::discard);
            husk.discard();
            helper.succeed();
        });
    }

    @HexTest(padding = 4)
    public static void theBlastHurtsButBreaksNothingAndSparesPets(GameTestHelper helper) {
        ServerPlayer owner = farThrower(helper);
        BlockPos centre = helper.absolutePos(new BlockPos(2, 2, 2));
        Husk husk = target(helper, new BlockPos(3, 1, 2));
        Wolf pet = helper.spawn(EntityTypes.WOLF, new BlockPos(1, 1, 2));
        pet.tame(owner);
        Wolf stray = helper.spawn(EntityTypes.WOLF, new BlockPos(2, 1, 3));
        List<BlockPos> blocks = List.of(centre.east(2), centre.west(2), centre.above(2), centre.north(2));
        blocks.forEach(pos -> helper.getLevel().setBlockAndUpdate(pos, Blocks.GLASS.defaultBlockState()));
        BiterGrenade source = new BiterGrenade(ModEntities.BITER_GRENADE.get(), helper.getLevel());
        float petHealth = pet.getHealth();
        var hurt = BiterGrenade.blast(helper.getLevel(), Vec3.atCenterOf(centre), source, owner, null);
        helper.assertTrue(hurt.contains(husk) && hurt.contains(stray), "what stands close is hurt: " + hurt);
        helper.assertTrue(!hurt.contains(pet) && pet.getHealth() == petHealth, "the thrower's pet is spared");
        blocks.forEach(pos -> helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.GLASS), "the blocks stay whole"));
        blocks.forEach(pos -> helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState()));
        husk.discard();
        pet.discard();
        stray.discard();
        helper.succeed();
    }

    @HexTest(maxTicks = 80, padding = 4)
    public static void aThrownGrenadeFliesPastAPet(GameTestHelper helper) {
        ServerPlayer owner = farThrower(helper);
        Wolf pet = helper.spawn(EntityTypes.WOLF, new BlockPos(3, 1, 2));
        pet.setNoAi(true);
        pet.tame(owner);
        BiterGrenade grenade = thrown(helper, owner, new Vec3(1.5, 1.5, 2.5), new Vec3(0.5, 0, 0));
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(grenade.isRemoved() || grenade.fuseLeft() < 0, "it did not bite the pet");
            helper.getLevel().getEntitiesOfClass(ItemEntity.class, grenade.getBoundingBox().inflate(12)).forEach(ItemEntity::discard);
            grenade.discard();
            pet.discard();
            helper.succeed();
        });
    }

    @HexTest
    public static void biterGrenadesWaitForTheirResearch(GameTestHelper helper) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.setGameMode(GameType.SURVIVAL);
        var knowledge = Research.of(player);
        ItemStack grenade = new ItemStack(ModItems.BITER_GRENADE.get());
        helper.assertTrue(ResearchTopics.BITER_GRENADE.lab(), "worked out at the Lab Bench");
        helper.assertFalse(ResearchTopics.mayMake(player, grenade, knowledge), "locked before its research");
        Research.research(player, ResearchTopics.BITER_GRENADE);
        helper.assertTrue(ResearchTopics.mayMake(player, grenade, knowledge), "open after it");
        helper.succeed();
    }
}
