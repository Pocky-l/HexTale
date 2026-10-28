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
        net.minecraft.world.entity.animal.Pig pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(1, 2, 1));
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
        net.minecraft.world.entity.animal.Pig pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, at.above());
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

    @HexTest(padding = 2)
    public static void aSoulFlaskKeepsACreatureWhole(GameTestHelper helper) {
        net.minecraft.world.entity.animal.Pig pig = helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(1, 2, 1));
        pig.setCustomName(net.minecraft.network.chat.Component.literal("Wilbur"));
        helper.assertTrue(com.pockyl.hextale.item.SoulFlaskItem.takes(pig), "a pig fits in a flask");
        net.minecraft.nbt.CompoundTag soul = com.pockyl.hextale.item.SoulFlaskItem.capture(helper.getLevel(), pig);
        pig.discard();
        net.minecraft.world.entity.Entity out = com.pockyl.hextale.item.SoulFlaskItem.release(helper.getLevel(), soul,
                Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
        helper.assertTrue(out instanceof net.minecraft.world.entity.animal.Pig && out.getCustomName() != null
                && out.getCustomName().getString().equals("Wilbur"), "the same pig comes out, name and all: " + out);
        out.discard();
        helper.succeed();
    }

    @HexTest(padding = 2)
    public static void aSpawnerCageMovesASpawnerWhole(GameTestHelper helper) {
        BlockPos from = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos to = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlockAndUpdate(from, net.minecraft.world.level.block.Blocks.SPAWNER.defaultBlockState());
        if (helper.getLevel().getBlockEntity(from) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner) {
            spawner.setEntityId(net.minecraft.world.entity.EntityType.ZOMBIE, helper.getLevel().getRandom());
        }
        net.minecraft.nbt.CompoundTag caged = com.pockyl.hextale.item.SpawnerCageItem.fold(helper.getLevel(), from);
        helper.assertTrue(helper.getLevel().getBlockState(from).isAir(), "the spawner leaves its place");
        com.pockyl.hextale.item.SpawnerCageItem.unfold(helper.getLevel(), to, caged);
        helper.assertTrue(helper.getLevel().getBlockEntity(to) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity,
                "a spawner stands at the new place");
        net.minecraft.nbt.CompoundTag again = helper.getLevel().getBlockEntity(to).saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(again.toString().contains("minecraft:zombie"), "and it still spawns zombies: " + again);
        helper.getLevel().setBlockAndUpdate(to, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    @HexTest
    public static void shadowCoffersShareOnePocket(GameTestHelper helper) {
        com.pockyl.hextale.block.ShadowPockets pockets = com.pockyl.hextale.block.ShadowPockets.of(helper.getLevel().getServer());
        java.util.UUID pair = java.util.UUID.randomUUID();
        pockets.pocket(pair).setItem(3, new net.minecraft.world.item.ItemStack(Items.DIAMOND, 5));
        helper.assertTrue(pockets.pocket(pair).getItem(3).is(Items.DIAMOND), "the same seal opens the same pocket");
        helper.assertTrue(pockets.pocket(java.util.UUID.randomUUID()).isEmpty(), "another seal opens another pocket");
        var ops = helper.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JavaOps.INSTANCE);
        Object saved = com.pockyl.hextale.block.ShadowPockets.CODEC.encodeStart(ops, pockets).getOrThrow();
        com.pockyl.hextale.block.ShadowPockets loaded = com.pockyl.hextale.block.ShadowPockets.CODEC.parse(ops, saved).getOrThrow();
        helper.assertTrue(loaded.pocket(pair).getItem(3).getCount() == 5, "the pocket is kept with the world");
        pockets.pocket(pair).clearContent();
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

    @HexTest
    public static void aWorldAnchorHoldsItsChunk(GameTestHelper helper) {
        BlockPos anchor = new BlockPos(1, 1, 1);
        helper.setBlock(anchor, ModBlocks.WORLD_ANCHOR.get());
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.hasForcedChunks(helper.getLevel()),
                    "the anchor holds its chunk");
            helper.setBlock(anchor, Blocks.AIR);
            helper.succeed();
        });
    }

    @HexTest
    public static void aDisintegratorUnmakesThingsIntoTheJarBelow(GameTestHelper helper) {
        BlockPos jar = new BlockPos(1, 1, 1);
        helper.setBlock(jar, ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(jar.above(), ModBlocks.DISINTEGRATOR.get());
        com.pockyl.hextale.block.DisintegratorBlock.Bowl bowl = helper.<com.pockyl.hextale.block.DisintegratorBlock.Bowl>getBlockEntity(jar.above());
        bowl.add(new net.minecraft.world.item.ItemStack(Items.COBBLESTONE, 2));
        helper.succeedWhen(() -> {
            com.pockyl.hextale.block.EssenceTankBlockEntity tank = helper.<com.pockyl.hextale.block.EssenceTankBlockEntity>getBlockEntity(jar);
            helper.assertTrue(bowl.item().isEmpty(), "every cobblestone is unmade");
            helper.assertTrue(bowl.tank().isEmpty(), "and all of it ran down");
            helper.assertTrue(tank.tank().contents().unknown() >= 3, "the jar holds unknown essence: " + tank.tank().contents());
        });
    }

    @HexTest
    public static void aSilenceWardHushesItsSurroundings(GameTestHelper helper) {
        BlockPos ward = new BlockPos(1, 1, 1);
        helper.setBlock(ward, ModBlocks.SILENCE_WARD.get());
        Vec3 centre = Vec3.atCenterOf(helper.absolutePos(ward));
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(com.pockyl.hextale.block.SilenceWardBlock.Ward.covering(helper.getLevel(), centre.add(5, 0, 0), null) != null,
                    "a sound five blocks away is hushed");
            helper.assertTrue(com.pockyl.hextale.block.SilenceWardBlock.Ward.covering(helper.getLevel(), centre.add(12, 0, 0), null) == null,
                    "twelve blocks away it is not");
            helper.assertTrue(com.pockyl.hextale.block.SilenceWardBlock.Ward.covering(helper.getLevel(), centre,
                    com.pockyl.hextale.runework.SilenceRune.INSULATOR) == null, "without Insulator vibrations pass");
            helper.succeed();
        });
    }

    @HexTest(padding = 4)
    public static void aMagnetDrawsLooseThingsIn(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 spot = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        player.teleportTo(spot.x, spot.y, spot.z);
        ItemEntity near = helper.spawnItem(Items.COBBLESTONE, player.position().add(4, 0.5, 0).subtract(Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO))));
        ItemEntity far = helper.spawnItem(Items.COBBLESTONE, player.position().add(0, 0.5, 9).subtract(Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO))));
        near.setNoPickUpDelay();
        far.setNoPickUpDelay();
        int moved = com.pockyl.hextale.item.HextechMagnetItem.pull(helper.getLevel(), player, com.pockyl.hextale.runework.PickaxeRunes.EMPTY);
        helper.assertTrue(moved == 1, "only the cobblestone within reach is drawn (" + moved + ")");
        helper.assertTrue(near.getDeltaMovement().x < -0.1, "it flies towards me: " + near.getDeltaMovement());
        near.discard();
        far.discard();
        helper.succeed();
    }

    @HexTest(padding = 6)
    public static void aGrappleDrawsItsWearerToTheHook(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 spot = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        player.teleportTo(spot.x, spot.y, spot.z);
        Vec3 hook = spot.add(5, 3, 0);
        com.pockyl.hextale.item.GrappleGauntletItem.start(player, hook, com.pockyl.hextale.runework.PickaxeRunes.EMPTY);
        helper.assertTrue(com.pockyl.hextale.item.GrappleGauntletItem.step(player), "far from the hook the pull goes on");
        player.teleportTo(hook.x, hook.y - 0.9, hook.z);
        helper.assertTrue(!com.pockyl.hextale.item.GrappleGauntletItem.step(player), "at the hook it lets go");
        helper.assertTrue(!com.pockyl.hextale.item.GrappleGauntletItem.pulling(player), "and the pull is over");
        helper.succeed();
    }

    @HexTest
    public static void aSipOfEssenceGivesItsGift(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        int gifts = com.pockyl.hextale.essence.EssenceFlaskItem.drink(com.pockyl.hextale.essence.EssenceMix.pure(com.pockyl.hextale.rune.Essence.FIRE, 16), player);
        helper.assertTrue(gifts == 1 && player.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE), "fire gives fire resistance");
        com.pockyl.hextale.essence.EssenceFlaskItem.drink(com.pockyl.hextale.essence.EssenceMix.unknown(8), player);
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION), "the unknown essence turns the stomach");
        helper.succeed();
    }
}
