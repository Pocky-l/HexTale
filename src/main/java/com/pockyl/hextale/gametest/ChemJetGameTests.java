package com.pockyl.hextale.gametest;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemFuel;
import com.pockyl.hextale.chem.ChemJets;
import com.pockyl.hextale.chem.ChemTank;
import com.pockyl.hextale.chem.JetKey;
import com.pockyl.hextale.chem.PneumaticBoots;
import com.pockyl.hextale.chem.TankFuel;
import com.pockyl.hextale.crafting.HexCraftingRecipe;
import com.pockyl.hextale.item.HextechSkypackItem;
import com.pockyl.hextale.item.SkypackFlight;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.thevoid.Thinning;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** The Chem Jet Tank: what a burn costs, what the server refuses, the recipe's fuel, and who goes first. */
public final class ChemJetGameTests {
    private ChemJetGameTests() {
    }

    private static ItemStack jets(Brew brew, int vials) {
        ItemStack tank = new ItemStack(ModItems.CHEM_JET_TANK.get());
        ChemTank.setContents(tank, ChemTank.poured(TankFuel.EMPTY, brew, vials));
        return tank;
    }

    /** A survival player in the air wearing the tank. */
    private static ServerPlayer flyer(GameTestHelper helper, ItemStack tank) {
        ServerPlayer player = MockPlayers.inLevel(helper);
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.CHEST, tank);
        player.setOnGround(false);
        return player;
    }

    private static void burn(ServerPlayer player, int ticks) {
        for (int i = 0; i < ticks; i++) {
            ChemJets.tick(player);
        }
    }

    /** One action per four ticks, also over short burns; the server counts, and nothing is paid on the ground. */
    @HexTest
    public static void theJetsBurnOneActionPerFourTicks(GameTestHelper helper) {
        ItemStack tank = jets(Brew.EMBER_OIL, 1);
        ServerPlayer player = flyer(helper, tank);
        int before = ChemTank.contents(tank).units();
        float thinning = Thinning.get(helper.getLevel(), player.blockPosition());
        ChemJets.ask(player, ChemJets.Mode.THRUST);
        burn(player, 8);
        helper.assertTrue(ChemTank.contents(tank).units() == before - 2, "eight ticks are two actions, left " + ChemTank.contents(tank).units());
        helper.assertTrue(ChemJets.burning(player), "the server sees the burn");
        helper.assertTrue(Thinning.get(helper.getLevel(), player.blockPosition()) == thinning, "no Thinning from use");

        burn(player, 2);
        ChemJets.ask(player, ChemJets.Mode.OFF);
        burn(player, 3);
        helper.assertFalse(ChemJets.burning(player), "off when the keys say so");
        ChemJets.ask(player, ChemJets.Mode.THRUST);
        burn(player, 2);
        helper.assertTrue(ChemTank.contents(tank).units() == before - 3, "taps add up, left " + ChemTank.contents(tank).units());

        player.setOnGround(true);
        burn(player, 8);
        helper.assertFalse(ChemJets.burning(player), "no burn on the ground");
        helper.assertTrue(ChemTank.contents(tank).units() == before - 3, "and nothing paid there");
        ChemJets.ask(player, ChemJets.Mode.OFF);
        burn(player, 1);
        helper.succeed();
    }

    /** The client only asks: no jets, no fuel or a plain tank get no burn however it asks, and the fuel running out ends it. */
    @HexTest
    public static void theServerDecidesWhetherJetsBurn(GameTestHelper helper) {
        ItemStack plain = new ItemStack(ModItems.CHEM_TANK.get());
        ChemTank.setContents(plain, ChemTank.poured(TankFuel.EMPTY, Brew.EMBER_OIL, 1));
        ServerPlayer player = flyer(helper, plain);
        ChemJets.ask(player, ChemJets.Mode.THRUST);
        burn(player, 4);
        helper.assertFalse(ChemJets.burning(player), "a plain tank has no jets");

        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CHEM_JET_TANK.get()));
        burn(player, 4);
        helper.assertFalse(ChemJets.burning(player), "an empty jet tank does not fire");

        ItemStack tank = new ItemStack(ModItems.CHEM_JET_TANK.get());
        ChemTank.setContents(tank, new TankFuel(ChemFuel.RAW_SHIMMER, 1));
        player.setItemSlot(EquipmentSlot.CHEST, tank);
        burn(player, 4);
        helper.assertTrue(ChemTank.contents(tank).isEmpty(), "the last action burns");
        burn(player, 1);
        helper.assertFalse(ChemJets.burning(player), "out of fuel, out of flame");

        ItemStack chestplate = new ItemStack(ModItems.CHEM_JET_TANK_CHESTPLATE.get());
        ChemTank.setContents(chestplate, ChemTank.poured(TankFuel.EMPTY, Brew.EMBER_OIL, 1));
        player.setItemSlot(EquipmentSlot.CHEST, chestplate);
        burn(player, 1);
        helper.assertTrue(ChemJets.burning(player), "the chestplate flies too");
        ChemJets.ask(player, ChemJets.Mode.OFF);
        burn(player, 1);
        helper.succeed();
    }

    /** A fall at speed stays a fall: only a burn that has slowed it forgets it. */
    @HexTest
    public static void aLastMomentBurnSavesNobody(GameTestHelper helper) {
        ServerPlayer player = flyer(helper, jets(Brew.EMBER_OIL, 1));
        ChemJets.ask(player, ChemJets.Mode.THRUST);
        burn(player, 1);
        player.fallDistance = 20;
        player.setPos(player.getX(), player.getY() - 2, player.getZ());
        burn(player, 1);
        helper.assertTrue(player.fallDistance == 20, "two blocks a tick is still falling");
        player.setPos(player.getX(), player.getY() - 0.2, player.getZ());
        burn(player, 1);
        helper.assertTrue(player.fallDistance == 0, "slowed down, the fall is forgotten");
        ChemJets.ask(player, ChemJets.Mode.OFF);
        burn(player, 1);
        helper.succeed();
    }

    /** Top speed by fuel; a hover holds still, climbs on jump and sinks on sneak; a faster rise is left alone. */
    @HexTest
    public static void thrustClimbsByFuelAndHoverHolds(GameTestHelper helper) {
        helper.assertTrue(ChemJets.climbSpeed(ChemFuel.EMBER_OIL) < ChemJets.climbSpeed(ChemFuel.RAW_SHIMMER)
                && ChemJets.climbSpeed(ChemFuel.RAW_SHIMMER) < ChemJets.climbSpeed(ChemFuel.REFINED_SHIMMER), "better fuel climbs faster");
        for (ChemFuel fuel : ChemFuel.values()) {
            Vec3 motion = new Vec3(0, -0.5, 0);
            for (int i = 0; i < 40; i++) {
                motion = ChemJets.thrust(motion, Vec3.ZERO, fuel, false, true, false);
                helper.assertTrue(motion.y <= ChemJets.climbSpeed(fuel) + 1.0E-9, fuel + " stays under its top speed");
                motion = new Vec3(0, (motion.y - 0.08) * 0.98, 0);
            }
            helper.assertTrue(ChemJets.thrust(motion, Vec3.ZERO, fuel, false, true, false).y == ChemJets.climbSpeed(fuel), fuel + " reaches its top speed");
        }
        Vec3 fast = new Vec3(0, 0.9, 0);
        helper.assertTrue(ChemJets.thrust(fast, Vec3.ZERO, ChemFuel.EMBER_OIL, false, true, false).y == 0.9, "thrust does not brake");
        Vec3 still = new Vec3(0, -0.08 * 0.98, 0);
        helper.assertTrue(ChemJets.thrust(still, Vec3.ZERO, ChemFuel.EMBER_OIL, true, false, false).y == 0, "a hover stands still");
        double sinking = ChemJets.thrust(new Vec3(0, -ChemJets.SINK, 0), Vec3.ZERO, ChemFuel.EMBER_OIL, true, false, true).y;
        helper.assertTrue(sinking == -ChemJets.SINK, "sneak sinks at its own speed, " + sinking);
        Vec3 wish = new Vec3(1, 0, 0);
        Vec3 flying = Vec3.ZERO;
        for (int i = 0; i < 40; i++) {
            flying = ChemJets.thrust(flying, wish, ChemFuel.REFINED_SHIMMER, true, false, false).multiply(0.91, 1, 0.91);
        }
        helper.assertTrue(flying.x > 0.4 && flying.x <= ChemJets.cruiseSpeed(ChemFuel.REFINED_SHIMMER), "the keys steer up to the cruise, " + flying.x);
        helper.succeed();
    }

    private static CraftingInput row(Item nozzle, ItemStack tank) {
        return CraftingInput.of(3, 1, List.of(new ItemStack(nozzle), tank, new ItemStack(nozzle)));
    }

    /** Tank or tank chestplate + two nozzles at the Hex Workbench: the fuel goes along. */
    @HexTest
    public static void theRecipeKeepsTheFuel(GameTestHelper helper) {
        ItemStack tank = new ItemStack(ModItems.CHEM_TANK.get());
        ChemTank.setContents(tank, ChemTank.poured(TankFuel.EMPTY, Brew.RAW_SHIMMER, 3));
        CraftingInput grid = row(ModItems.JET_NOZZLE.get(), tank);
        var found = HexCraftingRecipe.find(grid, false);
        helper.assertTrue(found != null, "the tank and two nozzles are a recipe");
        ItemStack made = found.getValue().assemble(grid);
        helper.assertTrue(made.is(ModItems.CHEM_JET_TANK.get()) && ChemTank.hasJets(made), "it makes a jet tank");
        helper.assertTrue(ChemTank.contents(made).equals(ChemTank.contents(tank)), "the fuel is kept");

        ItemStack chestplate = new ItemStack(ModItems.CHEM_STEEL_TANK_CHESTPLATE.get());
        ChemTank.setContents(chestplate, ChemTank.poured(TankFuel.EMPTY, Brew.EMBER_OIL, 2));
        grid = row(ModItems.JET_NOZZLE.get(), chestplate);
        found = HexCraftingRecipe.find(grid, false);
        helper.assertTrue(found != null, "so is the tank chestplate");
        made = found.getValue().assemble(grid);
        helper.assertTrue(made.is(ModItems.CHEM_JET_TANK_CHESTPLATE.get()) && ChemTank.contents(made).equals(ChemTank.contents(chestplate)),
                "and it keeps its fuel");

        grid = row(ModItems.JET_NOZZLE.get(), new ItemStack(ModItems.CHEM_TANK.get()));
        helper.assertTrue(ChemTank.contents(HexCraftingRecipe.find(grid, false).getValue().assemble(grid)).isEmpty(), "an empty tank stays empty");
        helper.succeed();
    }

    /** The jump key with a glider on: a tap belongs to it, a hold to the jets; a plain jump to neither. */
    @HexTest
    public static void theJumpKeyTellsATapFromAHold(GameTestHelper helper) {
        JetKey key = new JetKey();
        helper.assertFalse(key.update(true, true, false, true), "a press alone decides nothing");
        helper.assertFalse(key.lit(), "and lights nothing with a glider on");
        key.update(true, false, false, true);
        helper.assertTrue(key.update(false, false, false, true), "letting go in time is a tap");
        helper.assertFalse(key.update(false, false, false, true), "one tap opens once");

        key.update(true, true, false, true);
        for (int i = 0; i < JetKey.LIGHT_TICKS; i++) {
            helper.assertFalse(key.lit(), "not lit yet at " + i);
            key.update(true, false, false, true);
        }
        helper.assertTrue(key.lit(), "a hold lights the jets");
        helper.assertFalse(key.update(false, false, false, true), "and letting go of it opens no glider");

        key.reset();
        key.update(true, true, true, true);
        key.update(true, false, false, true);
        helper.assertFalse(key.update(false, false, false, true), "a plain jump from the ground is no tap");
        key.update(true, true, false, false);
        helper.assertTrue(key.lit(), "without a glider the press lights them at once");

        ServerPlayer player = MockPlayers.inLevel(helper);
        helper.assertFalse(ChemJets.glider(player), "nobody has a glider yet");
        ItemStack pack = HextechSkypackItem.full();
        player.setItemSlot(EquipmentSlot.CHEST, pack);
        helper.assertTrue(ChemJets.glider(player), "a pack with air is one");
        HextechSkypackItem.setAir(pack, 0);
        helper.assertFalse(ChemJets.glider(player), "an empty pack is not");
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        helper.assertTrue(ChemJets.glider(player), "elytra are");
        helper.succeed();
    }

    /** What the server gets from each: a hold asks for thrust and burns fuel, not air; a tap opens the glide and burns air, not fuel. */
    @HexTest
    public static void aHoldBurnsFuelAndATapLaunchesTheSkypack(GameTestHelper helper) {
        ItemStack tank = jets(Brew.RAW_SHIMMER, 2);
        ServerPlayer player = flyer(helper, tank);
        ItemStack pack = HextechSkypackItem.full();
        long now = helper.getLevel().getGameTime();
        int fuel = ChemTank.contents(tank).units();

        ChemJets.ask(player, ChemJets.Mode.THRUST);
        burn(player, 8);
        helper.assertTrue(ChemJets.burning(player), "a hold: the server sees the jets burn");
        helper.assertTrue(ChemTank.contents(tank).units() == fuel - 2, "on fuel");
        helper.assertFalse(SkypackFlight.tick(player, pack, now), "and no launch");
        helper.assertTrue(HextechSkypackItem.air(pack) == HextechSkypackItem.CAPACITY, "no air is spent");
        ChemJets.ask(player, ChemJets.Mode.OFF);
        burn(player, 1);
        helper.assertFalse(ChemJets.burning(player), "letting go puts them out");

        int left = ChemTank.contents(tank).units();
        player.startFallFlying();
        helper.assertTrue(SkypackFlight.tick(player, pack, now + 1), "a tap: the glide opens with the launch");
        HextechSkypackItem.launch(helper.getLevel(), player, pack);
        helper.assertTrue(SkypackFlight.boosting(player, now + 2), "the pack's jets burn");
        helper.assertTrue(HextechSkypackItem.air(pack) == HextechSkypackItem.CAPACITY - HextechSkypackItem.LAUNCH_AIR, "on air");
        burn(player, 8);
        helper.assertFalse(ChemJets.burning(player), "the tank's jets stay out");
        helper.assertTrue(ChemTank.contents(tank).units() == left, "and take no fuel");
        SkypackFlight.forget(player);
        helper.succeed();
    }

    /** Boots first: thrust waits for the jump, a hover has its own key. Then the jets, and the Skypack waits for them. */
    @HexTest
    public static void theBootsGoFirstAndTheSkypackWaits(GameTestHelper helper) {
        ItemStack tank = jets(Brew.RAW_SHIMMER, 2);
        ServerPlayer player = flyer(helper, tank);
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CHEM_STEEL_BOOTS.get()));
        ChemJets.ask(player, ChemJets.Mode.THRUST);
        burn(player, 4);
        helper.assertFalse(ChemJets.burning(player), "thrust waits while the boots' jump is left");
        ChemJets.ask(player, ChemJets.Mode.HOVER);
        burn(player, 1);
        helper.assertTrue(ChemJets.burning(player), "a hover does not");
        ChemJets.ask(player, ChemJets.Mode.THRUST);
        helper.assertTrue(PneumaticBoots.jump(player), "the boots jump");
        burn(player, 1);
        helper.assertTrue(ChemJets.burning(player), "then the jets fire");

        ItemStack pack = HextechSkypackItem.full();
        long now = helper.getLevel().getGameTime();
        player.startFallFlying();
        helper.assertTrue(player.isFallFlying(), "the test glides");
        helper.assertFalse(SkypackFlight.tick(player, pack, now), "no launch while the jets burn");
        helper.assertFalse(SkypackFlight.boosting(player, now), "and no boost");
        helper.assertFalse(ChemJets.canFire(player), "a glide already open keeps the jets out");
        burn(player, 1);
        helper.assertFalse(ChemJets.burning(player), "so they go off");
        helper.assertTrue(SkypackFlight.tick(player, pack, now + 1), "and the launch that waited goes");
        SkypackFlight.forget(player);
        ChemJets.ask(player, ChemJets.Mode.OFF);
        burn(player, 1);
        helper.succeed();
    }
}
