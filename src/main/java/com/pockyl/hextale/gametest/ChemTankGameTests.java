package com.pockyl.hextale.gametest;

import com.pockyl.hextale.GearConfig;
import com.pockyl.hextale.block.ChemTankBlockEntity;
import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemFuel;
import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.chem.TankFuel;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * The Chem Tank: one fuel at a time, filled with vials (by hand or hopper), pumps into the tank a player wears, keeps
 * its fuel when broken, and is found on the back in the chest slot.
 */
public final class ChemTankGameTests {
    private static final BlockPos AT = new BlockPos(2, 1, 2);

    private ChemTankGameTests() {
    }

    @HexTest
    public static void aTankHoldsOneFuelAndPumpsIntoTheWornOne(GameTestHelper helper) {
        int per = GearConfig.ACTIONS_PER_VIAL.get();
        TankFuel oil = ChemTank.poured(TankFuel.EMPTY, Brew.EMBER_OIL, 3);
        helper.assertTrue(oil.fuel() == ChemFuel.EMBER_OIL && oil.units() == 3 * per, "three vials of ember oil");
        helper.assertTrue(ChemTank.room(oil, Brew.RAW_SHIMMER) == 0, "no shimmer on top of oil");
        helper.assertTrue(ChemTank.room(oil, Brew.ACID) == 0, "acid is no fuel");
        helper.assertTrue(ChemTank.room(TankFuel.EMPTY, Brew.REFINED_SHIMMER) == GearConfig.TANK_VIALS.get(), "an empty tank takes a full load");

        helper.setBlock(AT, ModBlocks.CHEM_TANK.get());
        ChemTankBlockEntity placed = helper.getBlockEntity(AT, ChemTankBlockEntity.class);
        helper.assertTrue(placed.canPlaceItem(0, ChemVialItem.of(Brew.REFINED_SHIMMER, 4)), "a hopper may pour shimmer in");
        placed.setItem(0, ChemVialItem.of(Brew.REFINED_SHIMMER, 4));
        helper.assertTrue(placed.contents().fuel() == ChemFuel.REFINED_SHIMMER && placed.contents().vials() == 4, "four vials in");
        helper.assertFalse(placed.canPlaceItem(0, ChemVialItem.of(Brew.EMBER_OIL)), "oil does not go into a shimmer tank");

        ServerPlayer player = MockPlayers.inLevel(helper);
        ItemStack worn = new ItemStack(ModItems.CHEM_TANK.get());
        player.setItemSlot(EquipmentSlot.CHEST, worn);
        helper.assertTrue(ChemTank.worn(player) == worn, "the tank on the back is found");
        TankFuel[] after = ChemTank.pump(placed.contents(), ChemTank.contents(worn));
        placed.setContents(after[0]);
        ChemTank.setContents(worn, after[1]);
        helper.assertTrue(placed.contents().isEmpty() && ChemTank.contents(worn).vials() == 4, "everything pumped into the worn tank");

        placed.setContents(ChemTank.poured(TankFuel.EMPTY, Brew.EMBER_OIL, 2));
        AABB around = new AABB(helper.absolutePos(AT)).inflate(3);
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).forEach(net.minecraft.world.entity.Entity::discard);
        helper.getLevel().destroyBlock(helper.absolutePos(AT), true);
        var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around, e -> e.getItem().is(ModItems.CHEM_TANK.get()));
        helper.assertTrue(drops.size() == 1 && ChemTank.contents(drops.getFirst().getItem()).vials() == 2, "the broken tank keeps its oil");
        helper.succeed();
    }

    /** Chemtech tools: as strong as the fuel on the back, an eighth of the top with an empty tank, never broken, spend fuel. */
    @HexTest
    public static void chemToolsRunOnTheTank(GameTestHelper helper) {
        var kind = com.pockyl.hextale.chem.ChemGear.Kind.SWORD;
        float top = com.pockyl.hextale.chem.ChemGear.damage(kind, ChemFuel.REFINED_SHIMMER);
        float oil = com.pockyl.hextale.chem.ChemGear.damage(kind, ChemFuel.EMBER_OIL);
        float empty = com.pockyl.hextale.chem.ChemGear.damage(kind, null);
        helper.assertTrue(top > com.pockyl.hextale.chem.ChemGear.damage(kind, ChemFuel.RAW_SHIMMER) && oil < top, "better fuel, harder hits");
        helper.assertTrue(oil > 8, "even ember oil beats a netherite sword");
        helper.assertTrue(Math.abs(empty * GearConfig.EMPTY_DIVISOR.get() - top) < 0.01, "an empty tank divides the hit");
        helper.assertTrue(empty < 4, "an empty sword hits weaker than a wooden one");
        helper.assertTrue(com.pockyl.hextale.chem.ChemGear.speedShare(ChemFuel.REFINED_SHIMMER) == 1, "refined shimmer digs at full speed");
        helper.assertTrue(com.pockyl.hextale.chem.ChemGear.digSpeed(null) < 4, "an empty pickaxe digs slower than a stone one");

        ItemStack sword = new ItemStack(ModItems.CHEM_STEEL_SWORD.get());
        helper.assertTrue(!sword.isDamageableItem() || sword.has(net.minecraft.core.component.DataComponents.UNBREAKABLE), "it never breaks");
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack tank = new ItemStack(ModItems.CHEM_TANK.get());
        ChemTank.setContents(tank, ChemTank.poured(TankFuel.EMPTY, Brew.EMBER_OIL, 1));
        player.setItemSlot(EquipmentSlot.CHEST, tank);
        helper.assertTrue(com.pockyl.hextale.chem.ChemGear.power(player) == ChemFuel.EMBER_OIL, "the gear runs on the oil on the back");
        int before = ChemTank.contents(tank).units();
        com.pockyl.hextale.chem.ChemGear.spend(player, 1);
        helper.assertTrue(ChemTank.contents(ChemTank.worn(player)).units() == before - 1, "a blow spends one action");
        // The sword never breaks blocks, so a missed swing cannot dig.
        net.minecraft.core.BlockPos stone = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        helper.getLevel().setBlockAndUpdate(stone, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        helper.assertFalse(sword.canDestroyBlock(helper.getLevel().getBlockState(stone), helper.getLevel(), stone, player), "the sword leaves blocks alone");
        helper.assertTrue(new ItemStack(ModItems.CHEM_STEEL_PICKAXE.get()).canDestroyBlock(helper.getLevel().getBlockState(stone), helper.getLevel(), stone, player),
                "the pickaxe still digs");
        helper.succeed();
    }

    /** Chemtech armour: refined shimmer gives the top protection, ember oil less, an empty tank a third; it is found when worn. */
    @HexTest
    public static void chemArmourRunsOnTheTank(GameTestHelper helper) {
        var chest = net.minecraft.world.item.equipment.ArmorType.CHESTPLATE;
        float[] top = com.pockyl.hextale.chem.ChemGear.armour(chest, ChemFuel.REFINED_SHIMMER);
        float[] oil = com.pockyl.hextale.chem.ChemGear.armour(chest, ChemFuel.EMBER_OIL);
        float[] empty = com.pockyl.hextale.chem.ChemGear.armour(chest, null);
        helper.assertTrue(top[0] > oil[0] && top[1] > oil[1], "better fuel, better protection");
        helper.assertTrue(oil[0] >= 8, "ember oil already guards like netherite");
        helper.assertTrue(Math.abs(empty[0] * GearConfig.EMPTY_DIVISOR.get() - top[0]) < 0.01, "an empty tank divides the protection");
        helper.assertTrue(empty[0] < 2, "an empty chestplate guards like leather");
        ServerPlayer player = MockPlayers.inLevel(helper);
        helper.assertFalse(com.pockyl.hextale.chem.ChemGear.wearsArmour(player), "nothing worn yet");
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CHEM_STEEL_BOOTS.get()));
        helper.assertTrue(com.pockyl.hextale.chem.ChemGear.wearsArmour(player), "the boots are chemtech armour");
        // The chestplate with a built-in tank is both: armour and the tank on the back.
        ItemStack cuirass = new ItemStack(ModItems.CHEM_STEEL_TANK_CHESTPLATE.get());
        ChemTank.setContents(cuirass, ChemTank.poured(TankFuel.EMPTY, Brew.RAW_SHIMMER, 2));
        player.setItemSlot(EquipmentSlot.CHEST, cuirass);
        helper.assertTrue(ChemTank.worn(player) == cuirass, "the chestplate is the tank on the back");
        helper.assertTrue(com.pockyl.hextale.chem.ChemGear.power(player) == ChemFuel.RAW_SHIMMER, "the gear runs on its shimmer");
        helper.succeed();
    }

    /** A survival player with this tank on the back. */
    private static ServerPlayer wearer(GameTestHelper helper, ItemStack tank) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.CHEST, tank);
        return player;
    }

    private static ItemStack tank(Brew brew, int vials) {
        ItemStack tank = new ItemStack(ModItems.CHEM_TANK.get());
        ChemTank.setContents(tank, ChemTank.poured(TankFuel.EMPTY, brew, vials));
        return tank;
    }

    /** The helmet keeps the air bar up under water for one action per ten seconds; an empty tank lets it drop. */
    @HexTest
    public static void theRespiratorBreathesOnFuel(GameTestHelper helper) {
        helper.setBlock(AT, net.minecraft.world.level.block.Blocks.WATER);
        helper.setBlock(AT.above(), net.minecraft.world.level.block.Blocks.WATER);
        ItemStack tank = tank(Brew.EMBER_OIL, 1);
        ServerPlayer player = wearer(helper, tank);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CHEM_STEEL_HELMET.get()));
        net.minecraft.world.phys.Vec3 at = helper.absoluteVec(net.minecraft.world.phys.Vec3.atBottomCenterOf(AT));
        player.setPos(at.x, at.y, at.z);
        player.setAirSupply(100);
        int before = ChemTank.contents(tank).units();
        for (int i = 0; i < com.pockyl.hextale.chem.Respirator.SIP_TICKS; i++) {
            player.baseTick();
        }
        helper.assertTrue(player.getAirSupply() >= 100, "the air bar does not drop, it is at " + player.getAirSupply());
        helper.assertTrue(ChemTank.contents(tank).units() == before - 1, "ten seconds cost one action");

        ChemTank.setContents(tank, TankFuel.EMPTY);
        int air = player.getAirSupply();
        player.baseTick();
        helper.assertTrue(player.getAirSupply() < air, "with an empty tank the air runs out");
        helper.succeed();
    }

    /** One jump per time in the air, one action each, as high as the fuel allows; nothing on the ground or without fuel. */
    @HexTest
    public static void theBootsJumpOnFuel(GameTestHelper helper) {
        for (ChemFuel fuel : ChemFuel.values()) {
            double height = 0;
            for (double speed = com.pockyl.hextale.chem.PneumaticBoots.jumpSpeed(fuel); speed > 0; speed = (speed - 0.08) * 0.98) {
                height += speed;
            }
            helper.assertTrue(Math.abs(height - com.pockyl.hextale.chem.PneumaticBoots.jumpHeight(fuel)) < 0.01, fuel + " rises " + height);
        }
        ItemStack tank = tank(Brew.RAW_SHIMMER, 1);
        ServerPlayer player = wearer(helper, tank);
        int before = ChemTank.contents(tank).units();
        player.setOnGround(false);
        helper.assertFalse(com.pockyl.hextale.chem.PneumaticBoots.jump(player), "no boots, no jump");

        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CHEM_STEEL_BOOTS.get()));
        player.setOnGround(true);
        helper.assertFalse(com.pockyl.hextale.chem.PneumaticBoots.jump(player), "not from the ground");
        player.setOnGround(false);
        player.fallDistance = 6;
        helper.assertTrue(com.pockyl.hextale.chem.PneumaticBoots.jump(player), "a jump in the air");
        helper.assertTrue(ChemTank.contents(tank).units() == before - 1, "it costs one action");
        helper.assertTrue(player.fallDistance == 0, "the fall so far is forgotten");
        helper.assertFalse(com.pockyl.hextale.chem.PneumaticBoots.jump(player), "only one until the next landing");
        helper.assertTrue(ChemTank.contents(tank).units() == before - 1, "a refused jump costs nothing");

        player.setOnGround(true);
        com.pockyl.hextale.chem.PneumaticBoots.refresh(player);
        player.setOnGround(false);
        ChemTank.setContents(tank, TankFuel.EMPTY);
        helper.assertFalse(com.pockyl.hextale.chem.PneumaticBoots.jump(player), "nothing with an empty tank");
        ChemTank.setContents(tank, ChemTank.poured(TankFuel.EMPTY, Brew.RAW_SHIMMER, 1));
        helper.assertTrue(com.pockyl.hextale.chem.PneumaticBoots.jump(player), "a landing gives the jump back");
        helper.succeed();
    }

    /** A fall in the boots hurts less by the fuel's share for one action in all; an empty tank softens nothing. */
    @HexTest
    public static void theBootsSoftenAFall(GameTestHelper helper) {
        var level = helper.getLevel();
        var zombie = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.ZOMBIE, AT);
        ItemStack tank = tank(Brew.REFINED_SHIMMER, 1);
        zombie.setItemSlot(EquipmentSlot.CHEST, tank);
        zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CHEM_STEEL_BOOTS.get()));
        int before = ChemTank.contents(tank).units();
        float full = zombie.getHealth();
        zombie.hurtServer(level, level.damageSources().fall(), 10);
        float expected = 10 * (1 - com.pockyl.hextale.chem.PneumaticBoots.cushion(ChemFuel.REFINED_SHIMMER));
        helper.assertTrue(Math.abs(full - zombie.getHealth() - expected) < 0.01, "the fall took " + (full - zombie.getHealth()));
        helper.assertTrue(ChemTank.contents(tank).units() == before - 1, "one action for the hit, not two");

        var bare = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.ZOMBIE, AT);
        bare.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CHEM_TANK.get()));
        bare.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CHEM_STEEL_BOOTS.get()));
        float health = bare.getHealth();
        bare.hurtServer(level, level.damageSources().fall(), 10);
        helper.assertTrue(Math.abs(health - bare.getHealth() - 10) < 0.01, "an empty tank softens nothing");
        helper.succeed();
    }

    /** Both set-down tanks take fuel vials from pipes as they do from a hopper: nothing else, nothing out, a trial pours nothing. */
    @HexTest
    public static void pipesFuelASetDownTank(GameTestHelper helper) {
        for (BlockPos at : new BlockPos[] {AT, AT.east(2)}) {
            helper.setBlock(at, at.equals(AT) ? ModBlocks.CHEM_TANK.get() : ModBlocks.CHEM_JET_TANK.get());
            ChemTankBlockEntity tank = helper.getBlockEntity(at, ChemTankBlockEntity.class);
            ResourceHandler<ItemResource> handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(at), Direction.NORTH);
            helper.assertTrue(handler != null, "the tank has a handler");
            helper.assertTrue(ChemCarboyGameTests.put(handler, ChemVialItem.of(Brew.EMBER_OIL, 3), false) == 3 && tank.contents().isEmpty(), "a trial pours nothing");
            helper.assertTrue(ChemCarboyGameTests.put(handler, ChemVialItem.of(Brew.EMBER_OIL, 3), true) == 3 && tank.contents().vials() == 3, "three vials in");
            helper.assertTrue(ChemCarboyGameTests.put(handler, ChemVialItem.of(Brew.RAW_SHIMMER, 1), true) == 0, "no second fuel");
            helper.assertTrue(ChemCarboyGameTests.put(handler, ChemVialItem.of(Brew.ACID, 1), true) == 0, "acid is no fuel");
            helper.assertTrue(ChemCarboyGameTests.put(handler, new ItemStack(Items.DIRT), true) == 0, "no dirt");
            helper.assertTrue(ChemCarboyGameTests.take(handler, ChemVialItem.of(Brew.EMBER_OIL, 1), true) == 0, "nothing comes out");
        }
        helper.succeed();
    }
}
