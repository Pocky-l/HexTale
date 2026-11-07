package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.runework.BulwarkHeart;
import com.pockyl.hextale.runework.ClothesRune;
import com.pockyl.hextale.runework.RuneHolder;

/** The Arsenal: Bulwark armour (runes of the clothes, places by level, the crystal heart). */
public final class ArsenalGameTests {
    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private ArsenalGameTests() {
    }

    /** Bulwark takes the runes of the clothes piece of its slot, as many as its level holds. */
    @HexTest
    public static void bulwarkTakesTheClothesRunes(GameTestHelper helper) {
        ItemStack helmet = new ItemStack(ModItems.BULWARK_HELMET.get());
        helper.assertTrue(RuneHolder.of(helmet) == RuneHolder.HOOD && RuneHolder.HOOD.slots(helmet) == 1, "a first helmet takes one hood rune");
        ItemStack boots = new ItemStack(ModItems.RIFTFORGED_BULWARK_BOOTS.get());
        helper.assertTrue(RuneHolder.of(boots) == RuneHolder.BOOTS && RuneHolder.BOOTS.slots(boots) == 4, "riftforged boots take four");
        helper.assertTrue(RuneHolder.ROBE.slots(new ItemStack(ModItems.REFINED_BULWARK_CHESTPLATE.get())) == 2
                && RuneHolder.TROUSERS.slots(new ItemStack(ModItems.RADIANT_BULWARK_LEGGINGS.get())) == 3, "and the levels between");
        helper.assertTrue(RuneHolder.of(boots).holderRunes().contains(ClothesRune.BOOTS_WAVEWALK), "the boots' own runes");
        helper.succeed();
    }

    /** A charged heart soaks part of a blow and pays for it; run dry, it cracks and throws the creatures round back. */
    @HexTest(maxTicks = 60)
    public static void theBulwarkHeartSoaksAndCracks(GameTestHelper helper) {
        // Any creature can wear it; a fresh mock player would be invulnerable for its first seconds.
        Mob wearer = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, AT);
        ItemStack chest = new ItemStack(ModItems.BULWARK_CHESTPLATE.get());
        BulwarkHeart.set(chest, new BulwarkHeart.Charge(BulwarkHeart.MAX, false));
        wearer.setItemSlot(EquipmentSlot.CHEST, chest);
        Mob near = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, AT.east(2));
        helper.runAfterDelay(3, () -> {
            float before = wearer.getHealth();
            wearer.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(near), 10);
            ItemStack worn = wearer.getItemBySlot(EquipmentSlot.CHEST);
            int left = BulwarkHeart.of(worn).charge();
            helper.assertTrue(before - wearer.getHealth() > 0, "the blow landed, health " + before + " -> " + wearer.getHealth());
            helper.assertTrue(left < BulwarkHeart.MAX && left >= BulwarkHeart.MAX - 4, "the heart paid for what it soaked, left " + left);
            helper.assertTrue(before - wearer.getHealth() < 10, "the blow was softened");
            BulwarkHeart.set(worn, new BulwarkHeart.Charge(1, false));
            wearer.invulnerableTime = 0;
            wearer.invulnerableTime = 0;
            wearer.hurtServer(helper.getLevel(), helper.getLevel().damageSources().mobAttack(near), 6);
            helper.assertTrue(BulwarkHeart.of(worn).cracked(), "run dry, the heart cracks");
            Vec3 start = near.position();
            helper.succeedWhen(() -> helper.assertTrue(near.getDeltaMovement().horizontalDistance() > 0.05 || near.position().distanceTo(start) > 0.3,
                    "and the flash throws the zombie back"));
        });
    }

    /** A thrown dagger bites the creature it meets and turns back to its thrower at once. */
    // Entities in a fresh spot may not tick for a while: give the flight time to start.
    @HexTest(maxTicks = 200, setupTicks = 40)
    public static void aThrownDaggerBitesAndComesBack(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer thrower = MockPlayers.inLevel(helper);
        Vec3 from = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        thrower.snapTo(from.x, from.y, from.z, 0, 0);
        Mob target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 5));
        com.pockyl.hextale.entity.ThrownDagger dagger = new com.pockyl.hextale.entity.ThrownDagger(helper.getLevel(), thrower,
                new ItemStack(ModItems.TENDRIL_DAGGER.get()));
        dagger.setPos(from.x, from.y + 1.2, from.z + 1);
        dagger.setDeltaMovement(new Vec3(0, 0, 1.2));
        dagger.setNoGravity(true);
        helper.getLevel().addFreshEntity(dagger);
        helper.succeedWhen(() -> {
            helper.assertTrue(target.getHealth() < target.getMaxHealth(), "the dagger bit the zombie");
            helper.assertTrue(dagger.returning() || dagger.isRemoved(), "and turns back to its thrower");
        });
    }

    /** A dagger in the off hand sometimes finds a gap and strikes along with the main hand's blow - never on every blow. */
    @HexTest
    public static void anOffHandDaggerStrikesAlong(GameTestHelper helper) {
        Mob plain = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 2, 1));
        Mob paired = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(5, 2, 1));
        net.minecraft.server.level.ServerPlayer one = MockPlayers.inLevel(helper);
        one.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        net.minecraft.server.level.ServerPlayer two = MockPlayers.inLevel(helper);
        two.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
        two.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(ModItems.TENDRIL_DAGGER.get()));
        ItemStack dagger = new ItemStack(ModItems.TENDRIL_DAGGER.get());
        helper.assertTrue(com.pockyl.hextale.runework.TendrilDaggerItem.offHandExtra(dagger, 0) > 0
                && com.pockyl.hextale.runework.TendrilDaggerItem.offHandExtra(dagger, 0.99F) == 0, "a lucky roll bites, an unlucky one not");
        plain.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(one), 6);
        float plainLost = plain.getMaxHealth() - plain.getHealth();
        // Thirty blows: the dagger finds a gap in some of them (missing all thirty at 30% is a 1 in 40000 chance).
        boolean bit = false;
        for (int blow = 0; blow < 30 && !bit; blow++) {
            paired.setHealth(paired.getMaxHealth());
            paired.invulnerableTime = 0;
            paired.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(two), 6);
            bit = paired.getMaxHealth() - paired.getHealth() >= plainLost + 1.5F;
        }
        helper.assertTrue(bit, "the off-hand dagger adds its blow now and then");
        helper.succeed();
    }

    /** The server's side of a flight: moves the wearer the way their client would, up to the first wall, till it ends. */
    private static void flyOut(net.minecraft.server.level.ServerPlayer flier) {
        for (int i = 0; i < 40 && com.pockyl.hextale.runework.StrikeDash.flying(flier); i++) {
            Vec3 dir = com.pockyl.hextale.runework.StrikeDash.of(flier).dir();
            Vec3 middle = flier.getBoundingBox().getCenter();
            var hit = flier.level().clip(new net.minecraft.world.level.ClipContext(middle, middle.add(dir.scale(4)),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, flier));
            double gap = hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK ? hit.getLocation().distanceTo(middle) - 0.31 : 4;
            Vec3 next = flier.position().add(dir.scale(Math.max(0, Math.min(com.pockyl.hextale.runework.StrikeDash.SPEED, gap))));
            flier.snapTo(next.x, next.y, next.z, flier.getYRot(), flier.getXRot());
            com.pockyl.hextale.runework.StrikeDash.tick(flier);
        }
    }

    /** Releases a gauntlet in the main hand at the given charge stage. */
    private static void release(GameTestHelper helper, net.minecraft.server.level.ServerPlayer flier, int stage) {
        ItemStack fist = flier.getMainHandItem();
        flier.getCooldowns().removeCooldown(flier.getCooldowns().getCooldownGroup(fist));
        fist.releaseUsing(helper.getLevel(), flier, fist.getUseDuration(flier) - stage * com.pockyl.hextale.runework.StrikeGauntletItem.STAGE_TICKS);
    }

    private static net.minecraft.server.level.ServerPlayer gauntleted(GameTestHelper helper, boolean pair, double x, double y, double z, float yaw) {
        net.minecraft.server.level.ServerPlayer player = MockPlayers.inLevel(helper);
        Vec3 at = helper.absoluteVec(new Vec3(x, y, z));
        player.snapTo(at.x, at.y, at.z, yaw, 0);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.STRIKE_GAUNTLET.get()));
        if (pair) {
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(ModItems.STRIKE_GAUNTLET.get()));
        }
        return player;
    }

    /**
     * The server runs its own {@code travel} for a player, so its copy would sink under gravity between the client's
     * positions: the flight's wall check would then look a block too low, and the blow would dig into the floor.
     */
    @HexTest(padding = 4)
    public static void aDashStrikesAtTheHeightItFlies(GameTestHelper helper) {
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 10; z++) {
                helper.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.DIRT);
                for (int y = 1; y <= 5 && z >= 8; y++) {
                    helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.STONE);
                }
            }
        }
        double height = 2.1;
        net.minecraft.server.level.ServerPlayer flier = gauntleted(helper, false, 1.5, height, 1.5, 0);
        release(helper, flier, 3);
        double reported = flier.getY();
        for (int i = 0; i < 20 && com.pockyl.hextale.runework.StrikeDash.flying(flier); i++) {
            Vec3 next = flier.position().add(0, 0, 0.5);
            flier.snapTo(next.x, reported, next.z, flier.getYRot(), flier.getXRot());
            flier.doTick();
            helper.assertTrue(Math.abs(flier.getY() - reported) < 1.0E-6, "the server's copy stays where the client says: " + flier.getY());
        }
        helper.assertTrue(!com.pockyl.hextale.runework.StrikeDash.flying(flier), "the wall ends it");
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR, new BlockPos(1, 2, 8));
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.DIRT, new BlockPos(1, 0, 8));
        helper.succeed();
    }

    /** A full charge flown at stone: the end of the path breaks a 3 x 3 face two deep and throws what stands by the wall. */
    @HexTest(padding = 4)
    public static void aDashBreaksRockAtTheEndOfItsPath(GameTestHelper helper) {
        for (int x = 0; x <= 2; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 6; z <= 8; z++) {
                    helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.STONE);
                }
            }
        }
        Mob bystander = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
        net.minecraft.server.level.ServerPlayer flier = gauntleted(helper, false, 1.5, 1.4, 1.5, 0);
        release(helper, flier, 3);
        helper.assertTrue(com.pockyl.hextale.runework.StrikeDash.flying(flier), "the release starts a flight");
        flyOut(flier);
        helper.assertTrue(!com.pockyl.hextale.runework.StrikeDash.flying(flier), "the wall ends it");
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR, new BlockPos(1, 2, 6));
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR, new BlockPos(0, 2, 7));
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.AIR, new BlockPos(2, 2, 6));
        helper.assertBlockPresent(net.minecraft.world.level.block.Blocks.STONE, new BlockPos(1, 2, 8));
        helper.assertTrue(bystander.getHealth() < bystander.getMaxHealth(), "the bystander was on the way or in the wave");
        helper.succeed();
    }

    /** A flight hurts every enemy on its path once, the further ones too, and leaves villagers and whoever is off the path. */
    @HexTest(padding = 8)
    public static void aDashHurtsEveryEnemyOnThePathOnce(GameTestHelper helper) {
        Mob first = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(1, 1, 3));
        Mob second = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(1, 1, 7));
        Mob aside = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(7, 1, 5));
        Mob villager = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(1, 1, 5));
        net.minecraft.server.level.ServerPlayer flier = gauntleted(helper, false, 1.5, 1.4, 0.5, 0);
        release(helper, flier, 1);
        flyOut(flier);
        float path = com.pockyl.hextale.runework.StrikeDash.PATH_DAMAGE[0];
        helper.assertTrue(first.getMaxHealth() - first.getHealth() == path, "the first takes the path's damage once: " + first.getHealth());
        helper.assertTrue(second.getMaxHealth() - second.getHealth() == path, "and so does the one behind it: " + second.getHealth());
        helper.assertTrue(aside.getHealth() == aside.getMaxHealth(), "off the path nothing happens");
        helper.assertTrue(villager.getHealth() == villager.getMaxHealth(), "a villager on the path is spared");
        helper.succeed();
    }

    /** A pair doubles the flight's damage; a longer charge hurts more and flies longer. */
    @HexTest(padding = 8)
    public static void aPairDoublesTheDash(GameTestHelper helper) {
        Mob golem = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(1, 1, 3));
        net.minecraft.server.level.ServerPlayer flier = gauntleted(helper, true, 1.5, 1.4, 0.5, 0);
        release(helper, flier, 2);
        flyOut(flier);
        float lost = golem.getMaxHealth() - golem.getHealth();
        helper.assertTrue(lost == 2 * com.pockyl.hextale.runework.StrikeDash.PATH_DAMAGE[1], "a pair hits twice as hard: " + lost);
        helper.assertTrue(com.pockyl.hextale.runework.StrikeDash.TICKS[2] > com.pockyl.hextale.runework.StrikeDash.TICKS[0], "and a full charge flies longer");
        helper.succeed();
    }

    /** Neither the flight nor the first landing after it hurts; the next fall does. */
    @HexTest(padding = 8)
    public static void theLandingAfterADashIsSoft(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer flier = gauntleted(helper, false, 1.5, 4, 0.5, 0);
        flier.fallDistance = 40;
        release(helper, flier, 1);
        helper.assertTrue(flier.fallDistance == 0, "taking off clears the fall");
        flyOut(flier);
        // The mock player is creative and takes no fall damage: ask the event that cancels it.
        var bus = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        helper.assertTrue(bus.post(new net.neoforged.neoforge.event.entity.living.LivingFallEvent(flier, 30, 1)).isCanceled(), "the first landing is soft");
        helper.assertTrue(!bus.post(new net.neoforged.neoforge.event.entity.living.LivingFallEvent(flier, 30, 1)).isCanceled(), "the second fall is a fall");
        helper.succeed();
    }

    /** A real blow: hard on the target, the area round it hurt too, nothing far off; a pair doubles both. */
    @HexTest(padding = 8)
    public static void fistBlowsHitHardAndAround(GameTestHelper helper) {
        for (boolean pair : new boolean[]{false, true}) {
            int z = pair ? 6 : 1;
            Mob target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(1, 1, z));
            Mob near = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(3, 1, z));
            Mob far = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(9, 1, z));
            net.minecraft.server.level.ServerPlayer boxer = gauntleted(helper, pair, 0.5, 1, z + 0.5, -90);
            for (int i = 0; i < 40; i++) {
                boxer.doTick();
            }
            boxer.attack(target);
            float power = pair ? 2 : 1;
            float lost = target.getMaxHealth() - target.getHealth();
            helper.assertTrue(lost == com.pockyl.hextale.runework.StrikeGauntletItem.FIST_DAMAGE * power, "the blow: " + lost);
            float side = near.getMaxHealth() - near.getHealth();
            helper.assertTrue(side == com.pockyl.hextale.runework.StrikeGauntletItem.AREA_DAMAGE * power, "round the target: " + side);
            helper.assertTrue(far.getHealth() == far.getMaxHealth(), "far off nothing happens");
        }
        helper.succeed();
    }

    /** A half-charged swing strikes only its target. */
    @HexTest(padding = 8)
    public static void aHastySwingStrikesOnlyTheTarget(GameTestHelper helper) {
        Mob target = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(1, 1, 1));
        Mob near = helper.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(3, 1, 1));
        net.minecraft.server.level.ServerPlayer boxer = gauntleted(helper, false, 0.5, 1, 1.5, -90);
        boxer.doTick();
        boxer.attack(target);
        helper.assertTrue(near.getHealth() == near.getMaxHealth(), "no area without a full swing");
        helper.succeed();
    }

    /** The only recipe of the Strike Gauntlet in the whole game is the one on the Hexcore. */
    @HexTest
    public static void theGauntletIsMadeOnlyOnTheHexcore(GameTestHelper helper) {
        var resources = helper.getLevel().getServer().getResourceManager();
        java.util.List<String> found = new java.util.ArrayList<>();
        resources.listResources("recipe", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (var reader = resource.openAsReader()) {
                com.google.gson.JsonObject recipe = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                com.google.gson.JsonElement result = recipe.get("result");
                if (result != null && result.toString().contains("hextale:strike_gauntlet")) {
                    found.add(id.getPath() + (recipe.has("hexcore") && recipe.get("hexcore").getAsBoolean() ? " on the hexcore" : ""));
                }
            } catch (java.io.IOException | RuntimeException e) {
                helper.fail(net.minecraft.network.chat.Component.literal(id + ": " + e));
            }
        });
        helper.assertTrue(found.equals(java.util.List.of("recipe/infusion/strike_gauntlet.json on the hexcore")), "recipes found: " + found);
        helper.succeed();
    }

    /** A Crystal Battery feeds the heart of a Bulwark cuirass worn within its reach. */
    @HexTest(maxTicks = 60)
    public static void aBatteryFeedsTheBulwarkHeart(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), com.pockyl.hextale.registry.ModBlocks.CRYSTAL_BATTERY.get());
        var battery = helper.getBlockEntity(new BlockPos(1, 1, 1), com.pockyl.hextale.block.CrystalBatteryBlockEntity.class);
        battery.soak(new ItemStack(ModItems.UNSTABLE_HEXTECH.get()), 8);
        net.minecraft.server.level.ServerPlayer wearer = MockPlayers.inLevel(helper);
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 1, 1.5));
        wearer.snapTo(at.x, at.y, at.z, 0, 0);
        wearer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.BULWARK_CHESTPLATE.get()));
        helper.succeedWhen(() -> helper.assertTrue(BulwarkHeart.of(wearer.getItemBySlot(EquipmentSlot.CHEST)).charge() > 0,
                "the battery fed the heart"));
    }

    /** Backstab: the same blow lands harder on a creature that turns its back. */
    @HexTest
    public static void backstabStrikesHarderFromBehind(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer striker = MockPlayers.inLevel(helper);
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 1, 3.5));
        striker.snapTo(at.x, at.y, at.z, 0, 0);
        ItemStack blade = new ItemStack(ModItems.TENDRIL_DAGGER.get());
        RuneHolder.SWORD.set(blade, com.pockyl.hextale.runework.PickaxeRunes.EMPTY.with(com.pockyl.hextale.runework.ToolRune.BACKSTAB.rune(), 3));
        striker.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, blade);
        // Both face south (+Z): the first stands south of the striker (its back to them), the second north (face to face).
        Mob away = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 5));
        Mob facing = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
        for (Mob mob : java.util.List.of(away, facing)) {
            mob.setYRot(0);
            mob.setYHeadRot(0);
            mob.setYBodyRot(0);
        }
        away.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(striker), 4);
        facing.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(striker), 4);
        float back = away.getMaxHealth() - away.getHealth();
        float front = facing.getMaxHealth() - facing.getHealth();
        helper.assertTrue(back > front + 1.5F, "the blow from behind lands harder, " + back + " vs " + front);
        helper.succeed();
    }
}
