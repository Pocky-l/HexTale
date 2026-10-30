package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.RuneHolder;
import com.pockyl.hextale.runework.ToolRune;
import com.pockyl.hextale.runework.WeaponRunes;

import java.util.List;

/**
 * The runes of the hextech weapons, driven through real blows ({@code player.attack}): a test that calls a rune's
 * handler itself would pass while the rune does nothing in game.
 */
public final class WeaponRuneGameTests {
    private static final BlockPos AT = new BlockPos(0, 1, 0);

    private WeaponRuneGameTests() {
    }

    static ItemStack sword(ToolRune rune, int stars) {
        ItemStack sword = new ItemStack(ModItems.HEXTECH_SWORD.get());
        PickaxeRunes.set(sword, PickaxeRunes.EMPTY.with(rune.rune(), stars));
        return sword;
    }

    /** A survival player holding the weapon, a block south of {@code pos}, facing it. */
    static ServerPlayer fighter(GameTestHelper helper, ItemStack weapon, BlockPos pos) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(pos);
        player.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() - 1.0, 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        return player;
    }

    static Mob still(GameTestHelper helper, net.minecraft.world.entity.EntityType<? extends Mob> type, BlockPos pos) {
        Mob mob = helper.spawnWithNoFreeWill(type, pos);
        mob.setPersistenceRequired();
        return mob;
    }

    /** One fully charged blow at the target. */
    static void strike(ServerPlayer player, LivingEntity target) {
        target.damageCooldownTime = 0;
        charge(player);
        player.attack(target);
    }

    /** Chain Lightning: blows on one zombie sooner or later jump to the zombies standing round it. */
    @HexTest(maxTicks = 60, padding = 4)
    public static void chainLightningJumpsToTheNextCreatures(GameTestHelper helper) {
        Mob first = still(helper, EntityTypes.ZOMBIE, AT);
        List<Mob> others = List.of(still(helper, EntityTypes.ZOMBIE, AT.offset(3, 0, 0)), still(helper, EntityTypes.ZOMBIE, AT.offset(-3, 0, 1)));
        ServerPlayer player = fighter(helper, sword(ToolRune.CHAIN_LIGHTNING, 3), AT);
        helper.runAfterDelay(2, () -> {
            for (int i = 0; i < 40 && others.stream().allMatch(mob -> mob.getHealth() >= mob.getMaxHealth()); i++) {
                first.setHealth(first.getMaxHealth());
                strike(player, first);
            }
            helper.assertTrue(others.stream().anyMatch(mob -> mob.getHealth() < mob.getMaxHealth()),
                    "no arc jumped to the zombies around");
            helper.succeed();
        });
    }

    /** A mock player never ticks: its swing is charged by hand, as if it had waited. */
    static void charge(ServerPlayer player) {
        try {
            java.lang.reflect.Field ticker = LivingEntity.class.getDeclaredField("attackStrengthTicker");
            ticker.setAccessible(true);
            ticker.setInt(player, 100);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A stone floor from {@code z0} to {@code z1} along the spot's x (the two blocks above it cleared). */
    static void floor(GameTestHelper helper, int z0, int z1, int halfWidth) {
        for (int z = z0; z <= z1; z++) {
            for (int x = -halfWidth; x <= halfWidth; x++) {
                helper.setBlock(AT.offset(x, -1, z), Blocks.STONE);
                helper.setBlock(AT.offset(x, 0, z), Blocks.AIR);
                helper.setBlock(AT.offset(x, 1, z), Blocks.AIR);
            }
        }
    }

    /** Shockwave: a charged swing at the ground sends a wave along it that hurts and tosses the creatures in its way. */
    @HexTest(maxTicks = 60, padding = 14)
    public static void aShockwaveRunsAlongTheGround(GameTestHelper helper) {
        floor(helper, -2, 12, 4);
        Mob near = still(helper, EntityTypes.ZOMBIE, AT.offset(0, 0, 3));
        Mob far = still(helper, EntityTypes.ZOMBIE, AT.offset(0, 0, 6));
        // A cow: no other test's lightning or burst ever reaches for it.
        Mob beside = still(helper, EntityTypes.COW, AT.offset(3, 0, 4));
        ItemStack sword = sword(ToolRune.SHOCKWAVE, 2);
        ServerPlayer player = fighter(helper, sword, AT);
        BlockPos abs = helper.absolutePos(AT);
        player.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 60);
        helper.runAfterDelay(2, () -> {
            charge(player);
            WeaponRunes.shockwave(player);
            helper.assertTrue(player.getCooldowns().isOnCooldown(sword), "the weapon rests after a wave");
        });
        helper.runAfterDelay(16, () -> {
            helper.assertTrue(near.getHealth() < near.getMaxHealth() && far.getHealth() < far.getMaxHealth(), "the wave hurt both in its way");
            helper.assertTrue(beside.getHealth() >= beside.getMaxHealth(), "the wave stays on its path");
            helper.assertBlockPresent(Blocks.STONE, AT.offset(0, -1, 4));
            helper.succeed();
        });
    }

    /** Blink: right click at a creature steps the player behind it and lands a blow. */
    @HexTest(maxTicks = 40, padding = 10)
    public static void blinkStepsBehindTheCreature(GameTestHelper helper) {
        floor(helper, -2, 10, 1);
        Mob target = still(helper, EntityTypes.ZOMBIE, AT.offset(0, 0, 6));
        ItemStack sword = sword(ToolRune.BLINK, 1);
        ServerPlayer player = fighter(helper, sword, AT);
        BlockPos abs = helper.absolutePos(AT);
        player.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5, 0, 8);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(WeaponRunes.blink(helper.getLevel(), player, sword), "a creature in sight is reached");
            helper.assertTrue(player.getZ() > target.getZ(), "the player stands behind it");
            helper.assertTrue(target.getHealth() < target.getMaxHealth(), "and struck it");
            helper.succeed();
        });
    }

    /** Detonator: a creature the sword kills bursts and hurts the one beside it. */
    @HexTest(maxTicks = 40, padding = 6)
    public static void aDetonatorKillBursts(GameTestHelper helper) {
        floor(helper, -2, 4, 3);
        Mob victim = still(helper, EntityTypes.ZOMBIE, AT);
        Mob beside = still(helper, EntityTypes.ZOMBIE, AT.offset(2, 0, 0));
        ServerPlayer player = fighter(helper, sword(ToolRune.DETONATOR, 2), AT);
        helper.runAfterDelay(2, () -> {
            victim.setHealth(0.5F);
            strike(player, victim);
            helper.assertTrue(!victim.isAlive(), "the blow killed");
            helper.assertTrue(beside.getHealth() < beside.getMaxHealth(), "the burst hurt the one beside");
            helper.succeed();
        });
    }

    /** Whirlwind: the third blow in a row sweeps the creatures round the player. */
    @HexTest(maxTicks = 40, padding = 6)
    public static void theThirdBlowSweepsAround(GameTestHelper helper) {
        floor(helper, -4, 4, 3);
        Mob struck = still(helper, EntityTypes.ZOMBIE, AT);
        Mob behind = still(helper, EntityTypes.ZOMBIE, AT.offset(0, 0, -3));
        ServerPlayer player = fighter(helper, sword(ToolRune.WHIRLWIND, 1), AT);
        helper.runAfterDelay(2, () -> {
            for (int i = 0; i < 2; i++) {
                struck.setHealth(struck.getMaxHealth());
                strike(player, struck);
            }
            helper.assertTrue(behind.getHealth() >= behind.getMaxHealth(), "two blows do not sweep");
            struck.setHealth(struck.getMaxHealth());
            strike(player, struck);
            helper.assertTrue(behind.getHealth() < behind.getMaxHealth(), "the third blow swept round");
            helper.succeed();
        });
    }

    /** Whirlpool: sooner or later a blow opens a whirlpool that drags the creatures round it in. */
    @HexTest(maxTicks = 140, setupTicks = 40, padding = 8)
    public static void aWhirlpoolDragsCreaturesIn(GameTestHelper helper) {
        floor(helper, -2, 8, 5);
        Mob struck = still(helper, EntityTypes.ZOMBIE, AT.offset(0, 0, 1));
        // A creature with its own will: a mob without AI cannot be moved at all.
        net.minecraft.world.entity.animal.cow.Cow other = helper.spawn(EntityTypes.COW, AT.offset(4, 0, 1));
        ServerPlayer player = fighter(helper, sword(ToolRune.WHIRLPOOL, 3), AT);
        double[] before = new double[1];
        helper.runAfterDelay(2, () -> {
            for (int i = 0; i < 60 && !WeaponRunes.busy(helper.getLevel()); i++) {
                struck.setHealth(struck.getMaxHealth());
                strike(player, struck);
            }
            helper.assertTrue(WeaponRunes.busy(helper.getLevel()), "no whirlpool opened");
            before[0] = other.distanceToSqr(struck);
        });
        helper.runAfterDelay(4, () -> helper.succeedWhen(() ->
                helper.assertTrue(other.distanceToSqr(struck) < before[0] - 1, "the other creature was not dragged in")));
    }

    /** A weapon takes one power worked by hand: Shockwave and Blink clash. */
    @HexTest
    public static void aWeaponTakesOnePower(GameTestHelper helper) {
        helper.assertTrue(RuneHolder.SWORD.clashes(ToolRune.SHOCKWAVE.rune(), ToolRune.BLINK.rune()), "two powers clash");
        helper.assertTrue(!RuneHolder.SWORD.clashes(ToolRune.SHOCKWAVE.rune(), ToolRune.DETONATOR.rune()), "a power and a gift do not");
        helper.assertTrue(RuneHolder.AXE.takes(ToolRune.SHOCKWAVE.rune()), "the axe takes the Shockwave");
        helper.succeed();
    }
}
