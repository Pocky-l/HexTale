package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.entity.DustCharge;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.runework.PlateRune;

import java.util.List;

/** The early hextech gadgets: the Dust Charge (and the Levitation Plate). */
public final class GadgetGameTests {
    private GadgetGameTests() {
    }

    @HexTest(padding = 4)
    public static void aDustChargeCracksRockButNotBuilds(GameTestHelper helper) {
        BlockPos centre = helper.absolutePos(new BlockPos(2, 3, 2));
        BlockPos stone = centre.east();
        BlockPos ore = centre.west();
        BlockPos planks = centre.north();
        BlockPos obsidian = centre.south();
        helper.getLevel().setBlockAndUpdate(stone, Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(ore, Blocks.IRON_ORE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(planks, Blocks.OAK_PLANKS.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(obsidian, Blocks.OBSIDIAN.defaultBlockState());
        DustCharge charge = new DustCharge(ModEntities.DUST_CHARGE.get(), helper.getLevel());
        List<BlockPos> cracked = DustCharge.burst(helper.getLevel(), Vec3.atCenterOf(centre), charge, null);
        helper.assertTrue(cracked.contains(stone) && cracked.contains(ore), "stone and ore crack: " + cracked);
        helper.assertTrue(helper.getLevel().getBlockState(planks).is(Blocks.OAK_PLANKS), "planks stay");
        helper.assertTrue(helper.getLevel().getBlockState(obsidian).is(Blocks.OBSIDIAN), "obsidian stays");
        List<ItemEntity> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(centre).inflate(4));
        helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(Items.COBBLESTONE)), "stone drops cobblestone");
        helper.assertTrue(drops.stream().anyMatch(item -> item.getItem().is(Items.RAW_IRON)), "ore drops raw iron");
        drops.forEach(ItemEntity::discard);
        helper.getLevel().setBlockAndUpdate(planks, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(obsidian, Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    @HexTest(maxTicks = 200, setupTicks = 40, padding = 2)
    public static void aLevitationPlateLiftsWhatStandsOnIt(GameTestHelper helper) {
        BlockPos plate = new BlockPos(1, 1, 1);
        helper.setBlock(plate, ModBlocks.LEVITATION_PLATE.get());
        ItemEntity stone = helper.spawnItem(Items.COBBLESTONE, new Vec3(1.5, 1.3, 1.5));
        stone.setPickUpDelay(400);
        double floor = helper.absolutePos(plate).getY();
        helper.succeedWhen(() -> {
            double height = stone.getY() - floor;
            helper.assertTrue(height > 4.0, "the stone rises in the column (" + height + ")");
            helper.assertTrue(height < PlateRune.HEIGHT + 1.5, "and hangs at its top, not beyond (" + height + ")");
            stone.discard();
        });
    }

    @HexTest
    public static void sieveGlassChoosesWhoPasses(GameTestHelper helper) {
        com.pockyl.hextale.block.SieveGlassBlock ward = ModBlocks.WARD_GLASS.get();
        com.pockyl.hextale.block.SieveGlassBlock veil = ModBlocks.VEIL_GLASS.get();
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.animal.pig.Pig pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(1, 2, 1));
        helper.assertTrue(!ward.passes(player) && ward.passes(pig), "Ward Glass stops players and lets creatures through");
        helper.assertTrue(veil.passes(player) && !veil.passes(pig), "Veil Glass lets players through and stops creatures");
        pig.discard();
        helper.succeed();
    }

    @HexTest
    public static void anImpulsePlateThrowsAlongItsArrow(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, ModBlocks.IMPULSE_PLATE.get().defaultBlockState()
                .setValue(com.pockyl.hextale.block.ImpulsePlateBlock.FACING, net.minecraft.core.Direction.EAST));
        net.minecraft.world.entity.animal.pig.Pig pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, at.above());
        BlockPos plate = helper.absolutePos(at);
        ModBlocks.IMPULSE_PLATE.get().stepOn(helper.getLevel(), plate, helper.getLevel().getBlockState(plate), pig);
        Vec3 throwVec = pig.getDeltaMovement();
        pig.discard();
        helper.assertTrue(throwVec.y >= 1.0 && throwVec.x > 0.3 && Math.abs(throwVec.z) < 0.01, "thrown up and east: " + throwVec);
        helper.succeed();
    }

    @HexTest(padding = 2)
    public static void hexLiftsFindTheNextLiftWithRoom(GameTestHelper helper) {
        BlockPos low = new BlockPos(1, 1, 1);
        BlockPos high = low.above(6);
        helper.setBlock(low, ModBlocks.HEX_LIFT.get());
        helper.setBlock(high, ModBlocks.HEX_LIFT.get());
        BlockPos from = helper.absolutePos(low);
        BlockPos to = helper.absolutePos(high);
        helper.assertTrue(to.equals(com.pockyl.hextale.block.HexLiftBlock.next(helper.getLevel(), from, true, 16)), "up finds the lift above");
        helper.assertTrue(from.equals(com.pockyl.hextale.block.HexLiftBlock.next(helper.getLevel(), to, false, 16)), "down finds the lift below");
        helper.assertTrue(com.pockyl.hextale.block.HexLiftBlock.next(helper.getLevel(), from, true, 4) == null, "beyond its reach there is none");
        helper.setBlock(high.above(2), net.minecraft.world.level.block.Blocks.STONE);
        helper.assertTrue(com.pockyl.hextale.block.HexLiftBlock.next(helper.getLevel(), from, true, 16) == null, "a lift without room above is skipped");
        for (BlockPos pos : new BlockPos[] {low, high, high.above(2)}) {
            helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        }
        helper.succeed();
    }

    @HexTest
    public static void theRecallCharmRemembersWhereIStood(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 first = player.position();
        com.pockyl.hextale.item.RecallCharmItem.mark(player);
        player.teleportTo(first.x + 5, first.y, first.z);
        for (int i = 0; i < 3; i++) {
            com.pockyl.hextale.item.RecallCharmItem.mark(player);
        }
        com.pockyl.hextale.item.RecallCharmItem.Mark back = com.pockyl.hextale.item.RecallCharmItem.back(player, 5);
        helper.assertTrue(back != null && back.pos().distanceTo(first) < 0.01, "five seconds back it is where I first stood: " + back);
        back = com.pockyl.hextale.item.RecallCharmItem.back(player, 1);
        helper.assertTrue(back != null && back.pos().distanceTo(first) > 4, "one second back I had already moved: " + back);
        helper.succeed();
    }

    @HexTest
    public static void aNullLanternKeepsItsQuiet(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at.below(), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(at, ModBlocks.NULL_LANTERN.get());
        Vec3 lantern = Vec3.atCenterOf(helper.absolutePos(at));
        // A block entity joins the lanterns' index when it loads, a tick after it is set.
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(com.pockyl.hextale.block.NullLanternBlockEntity.covering(helper.getLevel(), lantern.add(10, 0, 10), null) != null,
                    "a spot within 24 blocks is quiet");
            helper.assertTrue(com.pockyl.hextale.block.NullLanternBlockEntity.covering(helper.getLevel(), lantern.add(30, 0, 0), null) == null,
                    "a spot 30 blocks away is not");
            helper.assertTrue(com.pockyl.hextale.block.NullLanternBlockEntity.covering(helper.getLevel(), lantern.add(5, 0, 0),
                    com.pockyl.hextale.runework.NullRune.HUSH) == null, "without Hush it does not keep phantoms off");
            helper.setBlock(at, net.minecraft.world.level.block.Blocks.AIR);
            helper.assertTrue(com.pockyl.hextale.block.NullLanternBlockEntity.covering(helper.getLevel(), lantern, null) == null,
                    "a broken lantern keeps nothing quiet");
            helper.succeed();
        });
    }
}
