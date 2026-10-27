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
            helper.assertValueEqual(helper.getBlockEntity(plate, com.pockyl.hextale.block.LevitationPlateBlockEntity.class).riders(), 1,
                    "riders the plate counts (it animates and hums by them)");
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

    @HexTest(setupTicks = 40, maxTicks = 200)
    public static void anImpulsePlateThrowsAlongItsArrow(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, ModBlocks.IMPULSE_PLATE.get().defaultBlockState()
                .setValue(com.pockyl.hextale.block.ImpulsePlateBlock.FACING, net.minecraft.core.Direction.EAST));
        // A pig standing on the plate - no direct call: the plate must notice it by itself.
        net.minecraft.world.entity.animal.pig.Pig pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new Vec3(1.5, 1.07, 1.5));
        double floor = helper.absolutePos(at).getY();
        double startX = pig.getX();
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.getY() > floor + 2 && pig.getX() > startX + 0.3, "thrown up and east: " + pig.position());
            pig.discard();
        });
    }

    /**
     * An aimed plate is a faith plate: aimed by hand (an empty-handed click on the plate, then a click on the target,
     * both through the real click path), it throws whatever steps on it so it lands on the target.
     */
    @HexTest(setupTicks = 40, maxTicks = 160, padding = 8)
    public static void anAimedImpulsePlateLandsThingsOnTheTarget(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        BlockPos target = new BlockPos(8, 2, 3);
        helper.setBlock(at, ModBlocks.IMPULSE_PLATE.get());
        helper.setBlock(target, Blocks.STONE);
        net.minecraft.server.level.ServerPlayer player = GameTests.watcher(helper, helper.absolutePos(at));
        net.minecraft.world.InteractionHand hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        player.gameMode.useItemOn(player, helper.getLevel(), net.minecraft.world.item.ItemStack.EMPTY, hand,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(helper.absolutePos(at)), net.minecraft.core.Direction.UP,
                        helper.absolutePos(at), false));
        player.gameMode.useItemOn(player, helper.getLevel(), net.minecraft.world.item.ItemStack.EMPTY, hand,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(helper.absolutePos(target)), net.minecraft.core.Direction.UP,
                        helper.absolutePos(target), false));
        com.pockyl.hextale.block.ImpulsePlateBlock.Plate plate = helper.getBlockEntity(at, com.pockyl.hextale.block.ImpulsePlateBlock.Plate.class);
        helper.assertValueEqual(plate.target(), helper.absolutePos(target), "the plate's target");
        net.minecraft.world.entity.decoration.ArmorStand stand = helper.spawn(net.minecraft.world.entity.EntityTypes.ARMOR_STAND, new Vec3(1.5, 1.1, 1.5));
        Vec3 landing = Vec3.atCenterOf(helper.absolutePos(target)).add(0, 0.5, 0);
        boolean[] flew = {false};
        helper.onEachTick(() -> flew[0] |= stand.getY() > landing.y + 1);
        helper.succeedWhen(() -> {
            helper.assertTrue(flew[0], "it flew (at " + stand.position() + ", target " + plate.target() + ", plate "
                    + helper.getBlockState(at) + ")");
            helper.assertTrue(stand.onGround() && stand.position().distanceTo(landing) < 1.0, "it landed on the target: " + stand.position()
                    + " vs " + landing);
            stand.discard();
        });
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

    /** Cache: an item lifted to the top of the column goes into the chest that caps it. */
    @HexTest(maxTicks = 200, setupTicks = 40, padding = 2)
    public static void aLevitationPlatePutsAwayWhatItLifts(GameTestHelper helper) {
        BlockPos plate = new BlockPos(1, 1, 1);
        BlockPos chest = plate.above(4);
        helper.setBlock(plate, ModBlocks.LEVITATION_PLATE.get());
        helper.setBlock(chest, Blocks.CHEST);
        net.minecraft.world.item.ItemStack runed = new net.minecraft.world.item.ItemStack(ModBlocks.LEVITATION_PLATE.get());
        runed.set(com.pockyl.hextale.registry.ModDataComponents.PLATE_RUNES.get(),
                com.pockyl.hextale.runework.PickaxeRunes.EMPTY.with(PlateRune.CACHE.rune(), 3));
        helper.getBlockEntity(plate, com.pockyl.hextale.block.LevitationPlateBlockEntity.class).applyComponentsFromItemStack(runed);
        ItemEntity stone = helper.spawnItem(Items.COBBLESTONE, new Vec3(1.5, 1.3, 1.5));
        stone.setPickUpDelay(400);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockEntity(chest, net.minecraft.world.level.block.entity.ChestBlockEntity.class)
                    .countItem(Items.COBBLESTONE) == 1, "the stone went into the chest over the column");
            helper.assertFalse(stone.isAlive(), "and is no longer floating");
        });
    }

    /** Banishing: a zombie stepping on the plate is flung back against the arrow and high. */
    @HexTest(setupTicks = 40, maxTicks = 200, padding = 2)
    public static void aBanishingPlateFlingsHostiles(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 1);
        helper.setBlock(at, ModBlocks.IMPULSE_PLATE.get().defaultBlockState()
                .setValue(com.pockyl.hextale.block.ImpulsePlateBlock.FACING, net.minecraft.core.Direction.EAST));
        net.minecraft.world.item.ItemStack runed = new net.minecraft.world.item.ItemStack(ModBlocks.IMPULSE_PLATE.get());
        runed.set(com.pockyl.hextale.registry.ModDataComponents.IMPULSE_RUNES.get(),
                com.pockyl.hextale.runework.PickaxeRunes.EMPTY.with(com.pockyl.hextale.runework.ImpulseRune.BANISHING.rune(), 2));
        helper.getBlockEntity(at, com.pockyl.hextale.block.ImpulsePlateBlock.Plate.class).applyComponentsFromItemStack(runed);
        net.minecraft.world.entity.monster.zombie.Zombie zombie = helper.spawn(net.minecraft.world.entity.EntityTypes.ZOMBIE, new Vec3(2.5, 1.07, 1.5));
        double floor = helper.absolutePos(at).getY();
        double startX = zombie.getX();
        helper.succeedWhen(() -> {
            helper.assertTrue(zombie.getY() > floor + 4 && zombie.getX() < startX - 0.3, "flung up and west: " + zombie.position());
            zombie.discard();
        });
    }

    /** Fireproofing: the way back puts the fire out and keeps it off. */
    @HexTest
    public static void aFireproofRecallPutsTheFireOut(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.item.ItemStack charm = new net.minecraft.world.item.ItemStack(com.pockyl.hextale.registry.ModItems.RECALL_CHARM.get());
        com.pockyl.hextale.runework.RuneHolder.RECALL.set(charm,
                com.pockyl.hextale.runework.PickaxeRunes.EMPTY.with(com.pockyl.hextale.runework.RecallRune.FIREPROOFING.rune(), 1));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, charm);
        com.pockyl.hextale.item.RecallCharmItem.mark(player);
        player.igniteForSeconds(5);
        charm.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertFalse(player.isOnFire(), "the fire is out");
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE), "and fire keeps off for a while");
        player.discard();
        helper.succeed();
    }

    /** Reading the notes of a device that takes runes gives a one-star scroll of one of its runes the reader lacks. */
    @HexTest
    public static void researchNotesHoldARune(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.item.ItemStack notes = com.pockyl.hextale.item.ResearchNotesItem.of(com.pockyl.hextale.research.ResearchTopics.LEVITATION_PLATE);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, notes);
        notes.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        net.minecraft.world.item.ItemStack scroll = net.minecraft.world.item.ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(com.pockyl.hextale.registry.ModItems.RUNE_SCROLL.get())) {
                scroll = player.getInventory().getItem(i);
            }
        }
        var rune = scroll.get(com.pockyl.hextale.registry.ModDataComponents.RUNE.get());
        helper.assertTrue(rune != null && com.pockyl.hextale.runework.RuneHolder.PLATE.takes(rune)
                && scroll.get(com.pockyl.hextale.registry.ModDataComponents.STARS.get()) == 1, "a one-star scroll of a plate rune: " + scroll);
        // Who knows every rune of the device gets none.
        var knowledge = com.pockyl.hextale.knowledge.Research.of(player);
        com.pockyl.hextale.runework.RuneHolder.PLATE.takes().forEach(known -> knowledge.learn(known, 1));
        helper.assertTrue(com.pockyl.hextale.item.ResearchNotesItem.runeFor(player, com.pockyl.hextale.research.ResearchTopics.LEVITATION_PLATE).isEmpty(),
                "nothing new to sketch");
        player.discard();
        helper.succeed();
    }

    /** A charm comes wound; each return spends a winding, none left = no return; a jar of rift essence winds it again. */
    @HexTest
    public static void theRecallCharmWindsAtARiftJar(GameTestHelper helper) {
        net.minecraft.world.item.ItemStack charm = new net.minecraft.world.item.ItemStack(com.pockyl.hextale.registry.ModItems.RECALL_CHARM.get());
        int full = com.pockyl.hextale.item.RecallCharmItem.WINDINGS;
        helper.assertTrue(com.pockyl.hextale.item.RecallCharmItem.windings(charm) == full, "a new charm comes fully wound");
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, charm);
        com.pockyl.hextale.item.RecallCharmItem.mark(player);
        com.pockyl.hextale.item.RecallCharmItem.setWindings(charm, 0);
        helper.assertTrue(charm.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND)
                == net.minecraft.world.InteractionResult.FAIL, "a run-down charm takes nobody back");
        com.pockyl.hextale.item.RecallCharmItem.setWindings(charm, 2);
        charm.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(com.pockyl.hextale.item.RecallCharmItem.windings(charm) == 1, "a return spends one winding");
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, ModBlocks.ESSENCE_TANK.get());
        com.pockyl.hextale.block.EssenceTankBlockEntity jar = helper.getBlockEntity(at, com.pockyl.hextale.block.EssenceTankBlockEntity.class);
        int mb = com.pockyl.hextale.item.RecallCharmItem.MB_PER_WINDING;
        jar.tank().set(com.pockyl.hextale.essence.EssenceMix.pure(com.pockyl.hextale.rune.Essence.RIFT, mb * 2 + 1));
        int wound = com.pockyl.hextale.item.RecallCharmItem.wind(helper.getLevel(), jar, charm, Vec3.atCenterOf(helper.absolutePos(at)));
        helper.assertTrue(wound == 2 && com.pockyl.hextale.item.RecallCharmItem.windings(charm) == 3
                && jar.tank().contents().get(com.pockyl.hextale.rune.Essence.RIFT) == 1, "two windings drank their rift essence, wound " + wound);
        helper.assertTrue(com.pockyl.hextale.item.RecallCharmItem.wind(helper.getLevel(), jar, charm, Vec3.ZERO) == 0,
                "what is left is too little for a winding");
        player.discard();
        helper.succeed();
    }

    @HexTest(padding = 2)
    public static void aSoulFlaskKeepsACreatureWhole(GameTestHelper helper) {
        net.minecraft.world.entity.animal.pig.Pig pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(1, 2, 1));
        pig.setCustomName(net.minecraft.network.chat.Component.literal("Wilbur"));
        helper.assertTrue(com.pockyl.hextale.item.SoulFlaskItem.takes(pig), "a pig fits in a flask");
        net.minecraft.nbt.CompoundTag soul = com.pockyl.hextale.item.SoulFlaskItem.capture(helper.getLevel(), pig);
        pig.discard();
        net.minecraft.world.entity.Entity out = com.pockyl.hextale.item.SoulFlaskItem.release(helper.getLevel(), soul,
                Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 2))));
        helper.assertTrue(out instanceof net.minecraft.world.entity.animal.pig.Pig && out.getCustomName() != null
                && out.getCustomName().getString().equals("Wilbur"), "the same pig comes out, name and all: " + out);
        out.discard();
        helper.succeed();
    }

    /** Someone else's dog stays out of a flask; its owner may fold it. */
    @HexTest(padding = 2)
    public static void aSoulFlaskLeavesOthersPets(GameTestHelper helper) {
        net.minecraft.world.entity.player.Player owner = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.player.Player stranger = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.entity.animal.wolf.Wolf wolf = helper.spawn(net.minecraft.world.entity.EntityTypes.WOLF, new BlockPos(1, 2, 1));
        wolf.tame(owner);
        helper.assertFalse(com.pockyl.hextale.item.SoulFlaskItem.takes(stranger, wolf), "a stranger cannot bottle the dog");
        helper.assertTrue(com.pockyl.hextale.item.SoulFlaskItem.takes(owner, wolf), "its owner can");
        wolf.discard();
        helper.succeed();
    }

    /** In adventure mode the cage cannot lift a spawner out of the world. */
    @HexTest(padding = 2)
    public static void aSpawnerCageAsksForBuildRights(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, net.minecraft.world.level.block.Blocks.SPAWNER);
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.ADVENTURE);
        // A mock player keeps its abilities; a real one in adventure mode has mayBuild off.
        player.getAbilities().mayBuild = false;
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(com.pockyl.hextale.registry.ModItems.SPAWNER_CAGE.get()));
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(helper.absolutePos(at)), net.minecraft.core.Direction.UP,
                helper.absolutePos(at), false);
        player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.SPAWNER, at);
        helper.setBlock(at, Blocks.AIR);
        helper.succeed();
    }

    @HexTest(padding = 2)
    public static void aSpawnerCageMovesASpawnerWhole(GameTestHelper helper) {
        BlockPos from = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos to = helper.absolutePos(new BlockPos(3, 2, 3));
        helper.getLevel().setBlockAndUpdate(from, net.minecraft.world.level.block.Blocks.SPAWNER.defaultBlockState());
        if (helper.getLevel().getBlockEntity(from) instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner) {
            spawner.setEntityId(net.minecraft.world.entity.EntityTypes.ZOMBIE, helper.getLevel().getRandom());
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

    /**
     * Two Lesser Rift Gates tied by hand (a hextech crystal pressed to one, the link crystal it becomes pressed to the
     * other, both through the real click path). With no rift essence beside it the gate is dry and keeps a dropped stone;
     * once a jar of rift essence stands by, a pig standing in one ring comes out of the other's front, the stone lands in
     * the chest under the other gate, the crossings drink their toll, and breaking a gate leaves its partner alone.
     */
    @HexTest(setupTicks = 20, maxTicks = 200, padding = 8)
    public static void riftGatesSendWhatEntersThem(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 1, 1);
        BlockPos b = new BlockPos(7, 1, 5);
        for (BlockPos gate : new BlockPos[] {a, b}) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    helper.setBlock(gate.offset(dx, -1, dz), Blocks.STONE);
                }
            }
        }
        helper.setBlock(b.below(), Blocks.CHEST);
        helper.setBlock(a, ModBlocks.RIFT_GATE.get());
        helper.setBlock(b, ModBlocks.RIFT_GATE.get().defaultBlockState().setValue(com.pockyl.hextale.block.RiftGateBlock.FACING,
                net.minecraft.core.Direction.EAST));
        net.minecraft.server.level.ServerPlayer player = GameTests.watcher(helper, helper.absolutePos(a));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.InteractionHand hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        player.setItemInHand(hand, new net.minecraft.world.item.ItemStack(com.pockyl.hextale.registry.ModItems.HEXTECH_CRYSTAL.get()));
        click(helper, player, a);
        net.minecraft.world.item.ItemStack crystal = null;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(com.pockyl.hextale.registry.ModItems.RIFT_LINK_CRYSTAL.get())) {
                crystal = player.getInventory().getItem(i);
            }
        }
        helper.assertTrue(crystal != null, "the crystal became a link crystal");
        player.setItemInHand(hand, crystal.copy());
        crystal.setCount(0);
        click(helper, player, b);
        helper.assertTrue(helper.getBlockState(a).getValue(com.pockyl.hextale.block.RiftGateBlock.LINKED)
                && helper.getBlockState(b).getValue(com.pockyl.hextale.block.RiftGateBlock.LINKED), "both gates are linked");
        ItemEntity stone = helper.spawnItem(Items.COBBLESTONE, new Vec3(1.5, 1.5, 1.5));
        stone.setDeltaMovement(Vec3.ZERO);
        Vec3 out = Vec3.atBottomCenterOf(helper.absolutePos(b));
        BlockPos jar = a.offset(2, 0, 0);
        net.minecraft.world.entity.animal.pig.Pig[] pigs = new net.minecraft.world.entity.animal.pig.Pig[1];
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(helper.getBlockEntity(a, com.pockyl.hextale.block.RiftGateBlock.Gate.class).dry(), "with no rift essence the gate is dry");
            helper.assertTrue(helper.getBlockEntity(b.below(), net.minecraft.world.level.block.entity.ChestBlockEntity.class)
                    .countItem(Items.COBBLESTONE) == 0, "and the stone stays out");
            helper.setBlock(jar, ModBlocks.ESSENCE_TANK.get());
            helper.getBlockEntity(jar, com.pockyl.hextale.block.EssenceTankBlockEntity.class).tank()
                    .set(com.pockyl.hextale.essence.EssenceMix.pure(com.pockyl.hextale.rune.Essence.RIFT, 10));
            pigs[0] = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new Vec3(1.5, 1.2, 1.5));
            // The dry ring spat the first stone out; another one goes in now.
            helper.spawnItem(Items.COBBLESTONE, new Vec3(1.5, 1.5, 1.5)).setDeltaMovement(Vec3.ZERO);
        });
        helper.runAfterDelay(70, () -> {
            net.minecraft.world.entity.animal.pig.Pig pig = pigs[0];
            helper.assertTrue(pig.isAlive() && pig.position().distanceTo(out) < 2.0, "the pig came out of the other gate: " + pig.position()
                    + " vs " + out);
            net.minecraft.world.level.block.entity.ChestBlockEntity chest = helper.getBlockEntity(b.below(),
                    net.minecraft.world.level.block.entity.ChestBlockEntity.class);
            helper.assertTrue(chest.countItem(Items.COBBLESTONE) == 1, "the stone dropped into the chest under the other gate");
            int left = helper.getBlockEntity(jar, com.pockyl.hextale.block.EssenceTankBlockEntity.class).tank().contents()
                    .get(com.pockyl.hextale.rune.Essence.RIFT);
            helper.assertTrue(left == 8, "the pig and the stone cost 2 mB of rift essence, " + left + " left");
            helper.setBlock(b, Blocks.AIR);
        });
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(!helper.getBlockState(a).getValue(com.pockyl.hextale.block.RiftGateBlock.LINKED), "the gate left alone knows it");
            pigs[0].discard();
            helper.succeed();
        });
    }

    /**
     * Linked gates carry liquids too: a jar of fire essence over one gate pours into it and the essence comes out into the
     * jar under its partner; water put into a gate (as another mod's pipe would) fills the cauldron under its partner.
     */
    @HexTest(setupTicks = 20, maxTicks = 200, padding = 8)
    public static void riftGatesCarryEssenceAndWater(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 2, 1);
        BlockPos b = new BlockPos(7, 2, 5);
        BlockPos c = new BlockPos(1, 2, 5);
        BlockPos d = new BlockPos(7, 2, 1);
        helper.setBlock(a.below(), Blocks.STONE);
        helper.setBlock(c.below(), Blocks.STONE);
        helper.setBlock(b.below(), ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(d.below(), Blocks.CAULDRON);
        for (BlockPos gate : new BlockPos[] {a, b, c, d}) {
            helper.setBlock(gate, ModBlocks.RIFT_GATE.get());
        }
        helper.setBlock(a.above(), ModBlocks.ESSENCE_TANK.get());
        helper.getBlockEntity(a.above(), com.pockyl.hextale.block.EssenceTankBlockEntity.class).tank()
                .set(com.pockyl.hextale.essence.EssenceMix.pure(com.pockyl.hextale.rune.Essence.FIRE, 300));
        // The toll: rift essence beside both sending gates.
        BlockPos toll = new BlockPos(1, 2, 3);
        helper.setBlock(toll, ModBlocks.ESSENCE_TANK.get());
        helper.getBlockEntity(toll, com.pockyl.hextale.block.EssenceTankBlockEntity.class).tank()
                .set(com.pockyl.hextale.essence.EssenceMix.pure(com.pockyl.hextale.rune.Essence.RIFT, 100));
        net.minecraft.server.level.ServerPlayer player = GameTests.watcher(helper, helper.absolutePos(a));
        net.minecraft.world.InteractionHand hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        for (BlockPos[] pair : new BlockPos[][] {{a, b}, {c, d}}) {
            player.setItemInHand(hand, new net.minecraft.world.item.ItemStack(com.pockyl.hextale.registry.ModItems.HEXTECH_CRYSTAL.get()));
            click(helper, player, pair[0]);
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).is(com.pockyl.hextale.registry.ModItems.RIFT_LINK_CRYSTAL.get())) {
                    player.setItemInHand(hand, player.getInventory().getItem(i).copy());
                    player.getInventory().setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
                }
            }
            click(helper, player, pair[1]);
        }
        com.pockyl.hextale.block.RiftGateBlock.Gate gateC = helper.getBlockEntity(c, com.pockyl.hextale.block.RiftGateBlock.Gate.class);
        try (net.neoforged.neoforge.transfer.transaction.Transaction transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            int put = gateC.fluidMouth().insert(0, net.neoforged.neoforge.transfer.fluid.FluidResource.of(net.minecraft.world.level.material.Fluids.WATER),
                    1000, transaction);
            transaction.commit();
            helper.assertTrue(put == 1000, "the gate takes water in (" + put + ")");
        }
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.WATER_CAULDRON, d.below());
            int fire = helper.getBlockEntity(b.below(), com.pockyl.hextale.block.EssenceTankBlockEntity.class).tank().contents()
                    .get(com.pockyl.hextale.rune.Essence.FIRE);
            helper.assertTrue(fire > 0, "fire essence came out under the other gate (" + fire + ")");
        });
    }

    private static void click(GameTestHelper helper, net.minecraft.server.level.ServerPlayer player, BlockPos pos) {
        BlockPos at = helper.absolutePos(pos);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(at), net.minecraft.core.Direction.NORTH, at, false));
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

    /** An anchor fed earth essence holds its chunk and pays for the first minute; one with no jar beside it falls asleep. */
    @HexTest(padding = 8)
    public static void aWorldAnchorHoldsItsChunkWhileFed(GameTestHelper helper) {
        BlockPos anchor = new BlockPos(1, 1, 1);
        BlockPos jar = new BlockPos(2, 1, 1);
        BlockPos hungry = new BlockPos(9, 1, 1);
        helper.setBlock(jar, ModBlocks.ESSENCE_TANK.get());
        helper.getBlockEntity(jar, com.pockyl.hextale.block.EssenceTankBlockEntity.class).tank()
                .set(com.pockyl.hextale.essence.EssenceMix.pure(com.pockyl.hextale.rune.Essence.EARTH, 3));
        helper.setBlock(anchor, ModBlocks.WORLD_ANCHOR.get());
        helper.setBlock(hungry, ModBlocks.WORLD_ANCHOR.get());
        helper.runAfterDelay(2, () -> {
            helper.assertFalse(helper.getBlockEntity(anchor, com.pockyl.hextale.block.WorldAnchorBlock.Anchor.class).asleep(), "the fed anchor is awake");
            helper.assertTrue(net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.hasForcedChunks(helper.getLevel()),
                    "the anchor holds its chunk");
            int left = helper.getBlockEntity(jar, com.pockyl.hextale.block.EssenceTankBlockEntity.class).tank().contents()
                    .get(com.pockyl.hextale.rune.Essence.EARTH);
            helper.assertTrue(left == 3 - com.pockyl.hextale.block.WorldAnchorBlock.cost(0), "one chunk's minute was paid, " + left + " left");
            helper.assertTrue(helper.getBlockEntity(hungry, com.pockyl.hextale.block.WorldAnchorBlock.Anchor.class).asleep(),
                    "the anchor with no earth essence around falls asleep");
            helper.assertTrue(com.pockyl.hextale.block.WorldAnchorBlock.cost(2) == 13, "25 chunks eat 13 mB a minute");
            helper.setBlock(anchor, Blocks.AIR);
            helper.setBlock(hungry, Blocks.AIR);
            helper.succeed();
        });
    }

    @HexTest
    public static void aDisintegratorUnmakesThingsIntoTheJarBelow(GameTestHelper helper) {
        BlockPos jar = new BlockPos(1, 1, 1);
        helper.setBlock(jar, ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(jar.above(), ModBlocks.DISINTEGRATOR.get());
        com.pockyl.hextale.block.DisintegratorBlock.Bowl bowl = helper.getBlockEntity(jar.above(), com.pockyl.hextale.block.DisintegratorBlock.Bowl.class);
        bowl.add(new net.minecraft.world.item.ItemStack(Items.COBBLESTONE, 2));
        helper.succeedWhen(() -> {
            com.pockyl.hextale.block.EssenceTankBlockEntity tank = helper.getBlockEntity(jar, com.pockyl.hextale.block.EssenceTankBlockEntity.class);
            helper.assertTrue(bowl.item().isEmpty(), "every cobblestone is unmade");
            helper.assertTrue(bowl.tank().isEmpty(), "and all of it ran down");
            helper.assertTrue(tank.tank().contents().unknown() >= 3, "the jar holds unknown essence: " + tank.tank().contents());
        });
    }

    @HexTest(padding = 8, maxTicks = 300)
    public static void aGrappleRopeSwingsItsWearer(GameTestHelper helper) {
        BlockPos anchor = new BlockPos(1, 12, 1);
        helper.setBlock(anchor, Blocks.STONE);
        net.minecraft.server.level.ServerPlayer player = selfMovingPlayer(helper);
        Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(-4, 8, 1)));
        Vec3 bite = Vec3.atCenterOf(helper.absolutePos(anchor)).add(0, -0.5, 0);
        Vec3 look = bite.subtract(start.add(0, player.getEyeHeight() - 0.25, 0));
        float yaw = (float) (net.minecraft.util.Mth.atan2(look.z, look.x) * net.minecraft.util.Mth.RAD_TO_DEG) - 90;
        float pitch = (float) -(net.minecraft.util.Mth.atan2(look.y, look.horizontalDistance()) * net.minecraft.util.Mth.RAD_TO_DEG);
        player.snapTo(start.x, start.y, start.z, yaw, pitch);
        net.minecraft.world.item.ItemStack gauntlet = new net.minecraft.world.item.ItemStack(com.pockyl.hextale.registry.ModItems.GRAPPLE_GAUNTLET.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, gauntlet);
        player.gameMode.useItem(player, helper.getLevel(), gauntlet, net.minecraft.world.InteractionHand.MAIN_HAND);
        double[] farthest = {start.x};
        double[] stretch = {0};
        helper.onEachTick(() -> {
            com.pockyl.hextale.entity.GrappleHook flying = com.pockyl.hextale.entity.GrappleHook.of(player);
            if (player.isRemoved() || flying == null || !flying.attached()) {
                // Test spots sometimes start ticking their entities late: the wearer waits in the air for the bite.
                return;
            }
            player.doTick();
            farthest[0] = Math.max(farthest[0], player.getX());
            com.pockyl.hextale.entity.GrappleHook hook = com.pockyl.hextale.entity.GrappleHook.of(player);
            if (hook != null && hook.attached() && hook.length() > 0) {
                stretch[0] = Math.max(stretch[0], com.pockyl.hextale.item.GrappleGauntletItem.grip(player).distanceTo(hook.position()) - hook.length());
            }
        });
        helper.succeedWhen(() -> {
            com.pockyl.hextale.entity.GrappleHook hook = com.pockyl.hextale.entity.GrappleHook.of(player);
            helper.assertTrue(hook != null && hook.attached(), "the claw holds the stone");
            Vec3 pivot = hook.position();
            helper.assertTrue(farthest[0] > pivot.x + 2, "the swing carried me past the claw and up the other side (x " + farthest[0]
                    + ", claw " + pivot.x + ")");
            helper.assertTrue(stretch[0] < 0.4, "the rope never stretched (" + stretch[0] + ")");
            player.stopUsingItem();
            hook.discard();
            player.discard();
        });
    }

    /**
     * A player that moves itself on the server (as a client moves its own player): its ticks are run by the test,
     * through a connection of its own that goes nowhere.
     */
    static net.minecraft.server.level.ServerPlayer selfMovingPlayer(GameTestHelper helper) {
        net.minecraft.server.network.CommonListenerCookie cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-swinger"), false);
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                cookie.gameProfile(), cookie.clientInformation()) {
            @Override
            public boolean isClientAuthoritative() {
                return false;
            }
        };
        net.minecraft.network.Connection connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        new net.minecraft.server.network.ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie);
        return player;
    }

    @HexTest
    public static void aSipOfEssenceGivesItsGift(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        int gifts = com.pockyl.hextale.essence.EssenceFlaskItem.drink(com.pockyl.hextale.essence.EssenceMix.pure(com.pockyl.hextale.rune.Essence.FIRE, 16), player);
        helper.assertTrue(gifts == 1 && player.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE), "fire gives fire resistance");
        com.pockyl.hextale.essence.EssenceFlaskItem.drink(com.pockyl.hextale.essence.EssenceMix.unknown(8), player);
        helper.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.NAUSEA), "the unknown essence turns the stomach");
        helper.succeed();
    }

    /** A lantern with Silence hushes sounds within its quiet; with deep Silence it swallows vibrations as well. */
    @HexTest
    public static void aLanternWithSilenceHushes(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, ModBlocks.NULL_LANTERN.get());
        var lantern = helper.getBlockEntity(at, com.pockyl.hextale.block.NullLanternBlockEntity.class);
        net.minecraft.world.item.ItemStack runed = new net.minecraft.world.item.ItemStack(com.pockyl.hextale.registry.ModItems.NULL_LANTERN.get());
        runed.set(com.pockyl.hextale.registry.ModDataComponents.NULL_RUNES.get(),
                com.pockyl.hextale.runework.PickaxeRunes.EMPTY.with(com.pockyl.hextale.runework.NullRune.SILENCE.rune(), 1));
        lantern.applyComponentsFromItemStack(runed);
        Vec3 centre = Vec3.atCenterOf(helper.absolutePos(at));
        // A lantern joins the index once it has loaded, a tick after it is placed.
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(com.pockyl.hextale.block.NullLanternBlockEntity.soundLeft(helper.getLevel(), centre.add(5, 0, 0)) < 1,
                    "a sound five blocks away is hushed");
            helper.assertTrue(com.pockyl.hextale.block.NullLanternBlockEntity.soundLeft(helper.getLevel(), centre.add(60, 0, 0)) >= 1,
                    "far outside the quiet it is not");
            helper.assertTrue(com.pockyl.hextale.block.NullLanternBlockEntity.covering(helper.getLevel(), centre,
                    com.pockyl.hextale.runework.NullRune.SILENCE, com.pockyl.hextale.runework.NullRune.INSULATING) == null,
                    "one star of Silence lets vibrations pass");
            helper.succeed();
        });
    }

    /** Trousers with Magnet draw a loose item within reach towards the wearer and leave one beyond it. */
    @HexTest(padding = 4)
    public static void magnetTrousersDrawLooseThingsIn(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 spot = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        player.teleportTo(spot.x, spot.y, spot.z);
        ItemEntity near = helper.spawnItem(Items.COBBLESTONE, player.position().add(3, 0.5, 0).subtract(Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO))));
        ItemEntity far = helper.spawnItem(Items.COBBLESTONE, player.position().add(0, 0.5, 9).subtract(Vec3.atLowerCornerOf(helper.absolutePos(BlockPos.ZERO))));
        near.setNoPickUpDelay();
        far.setNoPickUpDelay();
        int moved = com.pockyl.hextale.runework.ClothesEffects.pull(helper.getLevel(), player,
                com.pockyl.hextale.runework.HolderRune.at(com.pockyl.hextale.runework.ClothesRune.PULL_RADIUS, 1));
        helper.assertTrue(moved == 1, "only the cobblestone within reach is drawn (" + moved + ")");
        helper.assertTrue(near.getDeltaMovement().x < -0.1, "it flies towards me: " + near.getDeltaMovement());
        near.discard();
        far.discard();
        helper.succeed();
    }
}
