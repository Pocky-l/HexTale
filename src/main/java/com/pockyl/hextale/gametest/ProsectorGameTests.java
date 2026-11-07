package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDamageTypes;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.runework.ToolEffects;
import com.pockyl.hextale.runework.ToolRune;
import com.pockyl.hextale.thevoid.boss.CutGround;
import com.pockyl.hextale.thevoid.boss.DamageGate;
import com.pockyl.hextale.thevoid.boss.HexCells;
import com.pockyl.hextale.thevoid.boss.Proof;
import com.pockyl.hextale.thevoid.boss.Prosector;
import com.pockyl.hextale.thevoid.boss.ProsectorConfig;
import com.pockyl.hextale.thevoid.boss.ProsectorDamage;
import com.pockyl.hextale.thevoid.boss.ProsectorState;
import com.pockyl.hextale.thevoid.boss.SeersEyeItem;
import com.pockyl.hextale.thevoid.boss.Specimen;
import com.pockyl.hextale.thevoid.boss.StudyKind;
import com.pockyl.hextale.thevoid.boss.Surface;
import com.pockyl.hextale.thevoid.boss.attack.AttackType;
import com.pockyl.hextale.thevoid.realm.VoidZones;

import java.util.List;
import java.util.Map;

/** The Prosector: Study, the hurt gate, the turns, the milestones, the moves, the call and what a kill leaves. */
public final class ProsectorGameTests {
    /** A fight of its own, high over the test's spot and out of reach of the mock players of other tests. */
    private static final int FIGHT_HEIGHT = 60;

    private ProsectorGameTests() {
    }

    private static Prosector asleep(GameTestHelper helper) {
        return helper.spawn(ModEntities.PROSECTOR.get(), new BlockPos(1, 4, 1));
    }

    /** One that has already woken and hangs between moves, with a long pause so it does not act during the test. */
    private static Prosector awake(GameTestHelper helper) {
        Prosector boss = asleep(helper);
        boss.idle();
        boss.pause(100000);
        return boss;
    }

    /** A blow as if the hit cooldown and the bucket had run out; returns what it cost. */
    private static float hit(Prosector boss, ServerLevel level, DamageSource source, float amount) {
        boss.invulnerableTime = 0;
        boss.gate().clear();
        float before = boss.getHealth();
        boss.hurt(source, amount);
        return before - boss.getHealth();
    }

    private static ServerPlayer near(GameTestHelper helper, Prosector boss, double away) {
        ServerPlayer player = VoidLifeGameTests.survivor(helper);
        player.moveTo(boss.getX() + away, boss.getY(), boss.getZ(), 0, 0);
        return player;
    }

    private static void write(Prosector boss, ServerLevel level, ServerPlayer player) {
        for (int i = 0; i < 12 && !boss.study().written(StudyKind.BLADE); i++) {
            hit(boss, level, level.damageSources().playerAttack(player), 20);
        }
    }

    private static void done(Prosector boss, ServerPlayer... players) {
        for (Specimen specimen : boss.level().getEntitiesOfClass(Specimen.class, boss.getBoundingBox().inflate(40))) {
            specimen.discard();
        }
        boss.discard();
        for (ServerPlayer player : players) {
            player.discard();
        }
    }

    // ---- Study -----------------------------------------------------------------------------------------------------

    /** Each kind is read from the damage source and fills its own study. */
    @HexTest(padding = 6)
    public static void everyKindOfDamageFillsItsOwnStudy(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 3);
        Map<StudyKind, DamageSource> sources = Map.of(StudyKind.BLADE, level.damageSources().playerAttack(player),
                StudyKind.SHOT, level.damageSources().thrown(null, player),
                StudyKind.BLAST, level.damageSources().explosion(null, player),
                StudyKind.RUNE, level.damageSources().magic());
        for (Map.Entry<StudyKind, DamageSource> entry : sources.entrySet()) {
            helper.assertTrue(StudyKind.of(entry.getValue()) == entry.getKey(), "read as " + entry.getKey());
            float before = boss.study().fill(entry.getKey());
            hit(boss, level, entry.getValue(), 10);
            helper.assertTrue(boss.study().fill(entry.getKey()) > before, entry.getKey() + " fills its study");
        }
        helper.assertTrue(StudyKind.of(level.damageSources().lightningBolt()) == StudyKind.RUNE, "lightning is magic to it");
        helper.assertTrue(StudyKind.of(level.damageSources().onFire()) == StudyKind.RUNE, "and so is fire");
        done(boss, player);
        helper.succeed();
    }

    /** The share of its health a kind must deal is what fills the study, and a full one goes to a shelf. */
    @HexTest(padding = 6)
    public static void aFullStudyIsWrittenDownAsASpecimen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 3);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        write(boss, level, player);
        helper.assertTrue(boss.study().written(StudyKind.BLADE), "the blade is written down");
        Specimen specimen = boss.specimen(StudyKind.BLADE);
        helper.assertTrue(specimen != null, "a Specimen stands for it");
        helper.assertTrue(specimen.kind() == StudyKind.BLADE, "of the right kind");
        helper.assertTrue(specimen.copy().is(Items.DIAMOND_SWORD), "holding a copy of the weapon");
        helper.assertTrue(boss.heldCopy().is(Items.DIAMOND_SWORD), "and the forceps hold one too");
        done(boss, player);
        helper.succeed();
    }

    /** Past the phase's limit a kind waits just under full. */
    @HexTest(padding = 6)
    public static void aPhaseHoldsOnlySoManyKinds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 3);
        for (DamageSource source : List.of(level.damageSources().playerAttack(player), level.damageSources().thrown(null, player),
                level.damageSources().explosion(null, player))) {
            for (int i = 0; i < 6; i++) {
                hit(boss, level, source, 20);
            }
        }
        helper.assertTrue(boss.study().writtenCount() == ProsectorConfig.maxWritten(1), "phase one writes two kinds");
        helper.assertTrue(!boss.study().written(StudyKind.BLAST) && boss.study().fill(StudyKind.BLAST) > 0.9F, "the third waits, nearly full");
        done(boss, player);
        helper.succeed();
    }

    /** While the Specimen lives its kind does a quarter; other kinds do not care. */
    @HexTest(padding = 6)
    public static void aWrittenKindDoesLittleWhileItsSpecimenLives(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 3);
        float plain = hit(boss, level, level.damageSources().playerAttack(player), 12);
        write(boss, level, player);
        float resisted = hit(boss, level, level.damageSources().playerAttack(player), 12);
        helper.assertTrue(resisted > 0 && resisted < plain * 0.4F, "a written blade does a quarter: " + resisted + " of " + plain);
        float other = hit(boss, level, level.damageSources().magic(), 12);
        helper.assertTrue(other > plain * 0.9F, "magic still bites: " + other);
        done(boss, player);
        helper.succeed();
    }

    /** Breaking the Specimen, with anything: it forgets and is thrown open. */
    @HexTest(padding = 6)
    public static void breakingASpecimenMakesItForget(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 3);
        float plain = hit(boss, level, level.damageSources().playerAttack(player), 12);
        write(boss, level, player);
        Specimen specimen = boss.specimen(StudyKind.BLADE);
        helper.assertTrue(specimen.getHealth() == ProsectorConfig.SPECIMEN_HEALTH.get().floatValue(), "a Specimen has its own health");
        helper.assertFalse(specimen.hurt(ModDamageTypes.dissection(level, boss), 100), "the fight's own hits leave it alone");
        specimen.hurt(level.damageSources().playerAttack(player), 1000);
        helper.assertTrue(!specimen.isAlive(), "it broke");
        helper.assertTrue(!boss.study().written(StudyKind.BLADE), "the blade is forgotten");
        helper.assertTrue(boss.study().fill(StudyKind.BLADE) == 0, "from the start");
        helper.assertTrue(boss.heldCopy(StudyKind.BLADE).isEmpty(), "and the copy is gone");
        helper.assertTrue(boss.state() == ProsectorState.FORGETTING, "it is thrown into Forgetting");
        float open = hit(boss, level, level.damageSources().playerAttack(player), 12);
        helper.assertTrue(open > plain * 1.3F, "which pays half as much again: " + open + " against " + plain);
        done(boss, player);
        helper.succeed();
    }

    // ---- the gate --------------------------------------------------------------------------------------------------

    /** The cap per hit and the bucket hold, and Forgetting widens the bucket so the window pays. */
    @HexTest(padding = 6)
    public static void theHitCapAndTheDamageBucketHold(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 3);
        DamageSource blow = level.damageSources().playerAttack(player);
        float cap = ProsectorConfig.HIT_CAP.get().floatValue();
        float first = hit(boss, level, blow, 1000);
        helper.assertTrue(first > 0 && first <= cap, "one hit stops at the cap: " + first);
        boss.invulnerableTime = 0;
        float before = boss.getHealth();
        boss.hurt(blow, 1000);
        float second = before - boss.getHealth();
        helper.assertTrue(second > 0 && second <= DamageGate.TRICKLE + 0.01F, "the bucket lets a full second hit through only as a trickle: " + second);
        float bucket = boss.gate().bucket();
        boss.gate().tick(1);
        float drain = bucket - boss.gate().bucket();
        helper.assertTrue(Math.abs(drain - ProsectorConfig.DPS_CAP.get().floatValue() / 20) < 0.01F, "it drains at the rate of the cap: " + drain);
        boss.gate().tick(ProsectorConfig.FORGET_FACTOR.get().floatValue());
        helper.assertTrue(bucket - boss.gate().bucket() > drain * 2.4F, "and faster in Forgetting");
        done(boss, player);
        helper.succeed();
    }

    /** A hit the cooldown swallows costs the bucket nothing. */
    @HexTest(padding = 6)
    public static void aSwallowedHitLeavesTheBucketAlone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 3);
        DamageSource blow = level.damageSources().playerAttack(player);
        hit(boss, level, blow, 10);
        float bucket = boss.gate().bucket();
        helper.assertTrue(!boss.hurt(blow, 5), "a weaker hit in the cooldown does nothing");
        helper.assertTrue(boss.gate().bucket() == bucket, "and is not charged");
        done(boss, player);
        helper.succeed();
    }

    /** {@code /kill} goes past everything, even the shield of sleep. */
    @HexTest(padding = 6)
    public static void killGoesPastTheGate(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = asleep(helper);
        helper.assertTrue(boss.state().shielded(), "asleep nothing gets through");
        ServerPlayer player = near(helper, boss, 3);
        helper.assertFalse(boss.hurt(level.damageSources().playerAttack(player), 1000), "not a blow");
        boss.kill();
        helper.assertTrue(boss.isDeadOrDying(), "but /kill does");
        helper.assertTrue(boss.state() == ProsectorState.DYING, "and the long death starts");
        done(boss, player);
        helper.succeed();
    }

    // ---- turns -----------------------------------------------------------------------------------------------------

    /** A hit past a threshold queues the turn; it halves damage while it waits and the turn itself cannot be skipped. */
    @HexTest(maxTicks = 300, padding = 6)
    public static void turnsAreQueuedAndCannotBeSkipped(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 3);
        player.getAbilities().invulnerable = true;
        DamageSource blow = level.damageSources().playerAttack(player);
        float max = boss.getMaxHealth();
        float plain = hit(boss, level, blow, 10);
        boss.setHealth(max * 0.34F);
        for (int i = 0; i < 20; i++) {
            hit(boss, level, blow, 100);
        }
        helper.assertTrue(boss.getHealth() >= max * ProsectorConfig.PHASE3_AT.get().floatValue() - 0.01F,
                "no combo runs past the next turn: " + boss.getHealth());
        boss.setHealth(max * 0.5F);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(boss.turnQueued(), "the threshold queued the turn");
            float halved = hit(boss, level, blow, 10);
            helper.assertTrue(halved < plain * 0.7F, "damage is halved while it waits: " + halved + " of " + plain);
            boss.pause(0);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(boss.state() == ProsectorState.TURN_DISSECTION, "the turn comes before any move: " + boss.state());
            float health = boss.getHealth();
            helper.assertFalse(boss.hurt(blow, 1000), "it takes nothing while it turns");
            helper.assertTrue(boss.getHealth() == health, "not a point");
        });
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(boss.phase() == 2, "Dissection has begun");
            helper.assertTrue(boss.turnsDone() == 1, "the turn is done");
            done(boss, player);
            helper.succeed();
        });
    }

    /** One hit past both thresholds still plays both turns, in order. */
    @HexTest(maxTicks = 300, padding = 6)
    public static void aJumpPastBothThresholdsStillPlaysBothTurns(GameTestHelper helper) {
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 3);
        player.getAbilities().invulnerable = true;
        boss.setHealth(boss.getMaxHealth() * 0.2F);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(boss.scripted().equals(List.of(ProsectorState.TURN_DISSECTION, ProsectorState.TURN_PROOF)), "both wait: " + boss.scripted());
            boss.pause(0);
        });
        helper.runAfterDelay(6, () -> helper.assertTrue(boss.state() == ProsectorState.TURN_DISSECTION, "first Dissection: " + boss.state()));
        helper.runAfterDelay(100, () -> helper.assertTrue(boss.state() == ProsectorState.TURN_PROOF || boss.phase() == 3, "then Proof: " + boss.state()));
        helper.runAfterDelay(240, () -> {
            helper.assertTrue(boss.phase() == 3, "Proof has begun");
            helper.assertTrue(boss.proofEntity() != null, "and the Proof stands");
            helper.assertTrue(boss.shielded(), "and shields it");
            boss.proofEntity().discard();
            done(boss, player);
            helper.succeed();
        });
    }

    /** The last turn builds a copy of the top dealer; its death drops the Prosector into the last stand, and that can be killed. */
    @HexTest(maxTicks = 700, padding = 6)
    public static void theProofCopiesItsModelAndItsDeathBringsTheEnd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 3);
        player.getAbilities().invulnerable = true;
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        hit(boss, level, level.damageSources().playerAttack(player), 10);
        boss.setHealth(boss.getMaxHealth() * 0.2F);
        helper.runAfterDelay(2, () -> boss.pause(0));
        helper.runAfterDelay(220, () -> {
            Proof proof = boss.proofEntity();
            helper.assertTrue(proof != null, "the Proof stands");
            helper.assertTrue(proof.getMainHandItem().is(Items.IRON_SWORD), "with the weapon of the one who hurt it most");
            helper.assertTrue(proof.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "and their armour");
            helper.assertTrue(proof.getMaxHealth() == ProsectorConfig.PROOF_HEALTH.get().floatValue(), "with its own health");
            helper.assertFalse(boss.hurt(level.damageSources().playerAttack(player), 100), "the Prosector takes nothing behind it");
            proof.hurt(level.damageSources().playerAttack(player), 1000);
        });
        helper.runAfterDelay(240, () -> helper.assertTrue(boss.state() == ProsectorState.FALLEN, "its death brings the Prosector down: " + boss.state()));
        helper.runAfterDelay(240 + ProsectorConfig.FALLEN_TICKS.get() + 10, () -> {
            helper.assertTrue(boss.lastStand(), "then comes the last stand");
            helper.assertTrue(boss.timed(100) == 70, "at seven tenths of the time");
            boss.setHealth(1);
            hit(boss, level, level.damageSources().playerAttack(player), 100);
            helper.assertTrue(boss.isDeadOrDying(), "and now it can die");
            done(boss, player);
            helper.succeed();
        });
    }

    // ---- leaving ---------------------------------------------------------------------------------------------------

    /** With nobody in the room it falls back on the last milestone, lets the Specimens go and goes to sleep. */
    @HexTest(maxTicks = 1000, padding = 6)
    public static void anEmptyRoomKeepsTheLastMilestone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = highUp(helper);
        ServerPlayer player = near(helper, boss, 3);
        write(boss, level, player);
        Specimen specimen = boss.specimen(StudyKind.BLADE);
        player.discard();
        boss.setHealth(boss.getMaxHealth() * 0.5F);
        boss.pause(0);
        helper.runAfterDelay(ProsectorConfig.EMPTY_TICKS.get() + 120, () -> {
            helper.assertTrue(boss.state() == ProsectorState.DORMANT, "it gave up: " + boss.state());
            float milestone = boss.getMaxHealth() * ProsectorConfig.PHASE2_AT.get().floatValue();
            helper.assertTrue(Math.abs(boss.getHealth() - milestone) < 0.01F, "healed only to the last milestone: " + boss.getHealth());
            helper.assertTrue(boss.phase() == 2, "and what it reached stays");
            helper.assertTrue(!specimen.isAlive() && boss.study().writtenCount() == 0, "the Specimens went");
            done(boss);
            helper.succeed();
        });
    }

    /** Every extra player in the room adds to its body, keeping its share of health. */
    @HexTest(padding = 6)
    public static void moreDissectorsMoreBody(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = highUp(helper);
        ServerPlayer one = near(helper, boss, 3);
        ServerPlayer two = near(helper, boss, -3);
        helper.runAfterDelay(1, () -> {
            boss.wake(level);
            int extra = boss.playersInFight(level).size() - 1;
            helper.assertTrue(extra >= 1, "two or more players");
            float wanted = 1 + extra * ProsectorConfig.PER_PLAYER.get().floatValue();
            helper.assertTrue(Math.abs(boss.bulk() - wanted) < 0.01F, "tougher for each extra one: " + boss.bulk());
            helper.assertTrue(Math.abs(boss.getHealth() - ProsectorConfig.HEALTH.get().floatValue()) < 0.5F, "at full health");
            done(boss, one, two);
            helper.succeed();
        });
    }

    // ---- moves -----------------------------------------------------------------------------------------------------

    /** No move twice running, nothing on cooldown, distance bands and the phase gate all weigh nothing. */
    @HexTest(padding = 6)
    public static void theChoiceSkipsTheLastMoveAndCooldowns(GameTestHelper helper) {
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 8);
        var decider = boss.decider();
        helper.assertTrue(decider.weight(AttackType.INCISION, boss, player) > 0, "Incision is on the table");
        decider.chosen(AttackType.INCISION);
        helper.assertTrue(decider.weight(AttackType.INCISION, boss, player) == 0, "not twice in a row");
        decider.chosen(AttackType.PROBE);
        helper.assertTrue(decider.weight(AttackType.INCISION, boss, player) == 0, "nor while on cooldown");
        decider.setCooldown(AttackType.INCISION, 0);
        helper.assertTrue(decider.weight(AttackType.INCISION, boss, player) > 0, "free again");
        helper.assertTrue(decider.weight(AttackType.SECTION, boss, player) == 0, "Section waits for phase two");
        helper.assertTrue(decider.weight(AttackType.RIPOSTE, boss, player) == 0, "no mimic without its Specimen");
        player.moveTo(boss.getX() + 3, boss.getY(), boss.getZ(), 0, 0);
        helper.assertTrue(decider.weight(AttackType.RETRACTOR, boss, player) == 0, "hooks are for the far target");
        done(boss, player);
        helper.succeed();
    }

    /** The opening always comes. */
    @HexTest(padding = 6)
    public static void excisionIsForcedAfterFourOtherMoves(GameTestHelper helper) {
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 8);
        var decider = boss.decider();
        for (int i = 0; i < ProsectorConfig.PITY.get(); i++) {
            decider.chosen(AttackType.INCISION);
        }
        helper.assertTrue(decider.choose(boss, player) == AttackType.EXCISION, "pity forces it");
        decider.chosen(AttackType.EXCISION);
        helper.assertTrue(decider.sinceExcision() == 0, "and the count starts over");
        done(boss, player);
        helper.succeed();
    }

    /** A target up on a pillar is what the plug is for. */
    @HexTest(padding = 6)
    public static void excisionAnswersAPillar(GameTestHelper helper) {
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 8);
        var decider = boss.decider();
        double low = decider.weight(AttackType.EXCISION, boss, player);
        player.moveTo(player.getX(), boss.floorHeight() + 8, player.getZ(), 0, 0);
        helper.assertTrue(decider.weight(AttackType.EXCISION, boss, player) > low * 3, "far likelier against a target up high");
        done(boss, player);
        helper.succeed();
    }

    private static void standOnFloor(GameTestHelper helper) {
        for (int x = -3; x <= 12; x++) {
            for (int z = -3; z <= 12; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
    }

    /** The line opens under whoever stayed on it; a sidestep after the lock is enough. */
    @HexTest(maxTicks = 120, padding = 8)
    public static void incisionCutsWhoStaysAndMissesWhoSteps(GameTestHelper helper) {
        standOnFloor(helper);
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer stays = VoidLifeGameTests.survivor(helper);
        stays.spawnInvulnerableTime = 0;
        Vec3 at = helper.absoluteVec(new Vec3(6.5, 2, 1.5));
        stays.moveTo(at.x, at.y, at.z, 0, 0);
        boss.moveTo(at.x - 5, at.y + 3, at.z, 0, 0);
        boss.startMove(level, AttackType.INCISION, stays);
        float health = stays.getHealth();
        int lock = ProsectorConfig.INCISION.startup().get() - ProsectorConfig.INCISION.lock().get();
        int cut = ProsectorConfig.INCISION.startup().get() + 5;
        int second = cut + ProsectorConfig.INCISION.recovery().get() + 10;
        float[] left = new float[1];
        helper.runAfterDelay(cut, () -> {
            helper.assertTrue(stays.getHealth() < health, "the cut reached the one who stayed");
            left[0] = stays.getHealth();
        });
        helper.runAfterDelay(second, () -> {
            boss.decider().setCooldown(AttackType.INCISION, 0);
            boss.startMove(level, AttackType.INCISION, stays);
        });
        helper.runAfterDelay(second + lock + 3, () -> stays.moveTo(at.x, at.y, at.z + 6, 0, 0));
        helper.runAfterDelay(second + cut, () -> {
            helper.assertTrue(stays.getHealth() == left[0], "a sidestep after the lock beats it");
            done(boss, stays);
            helper.succeed();
        });
    }

    /** The hooks pull a target in; a raised shield snags them and the Prosector reels. */
    @HexTest(maxTicks = 200, padding = 8)
    public static void aRaisedShieldSnagsTheHooks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 10);
        player.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        player.startUsingItem(InteractionHand.OFF_HAND);
        for (int i = 0; i < 8; i++) {
            player.doTick();
        }
        helper.assertTrue(player.isBlocking(), "the shield is up");
        boss.startMove(level, AttackType.RETRACTOR, player);
        int ticks = ProsectorConfig.RETRACTOR.startup().get() + ProsectorConfig.RETRACTOR.active().get() + 3;
        helper.runAfterDelay(ticks, () -> {
            helper.assertTrue(boss.state() == ProsectorState.REELING, "the hooks snagged: " + boss.state());
            done(boss, player);
            helper.succeed();
        });
    }

    /** Only a Probe that reaches its target burns it: a wall is cover. */
    @HexTest(maxTicks = 200, padding = 8)
    public static void theProbeStopsAtAWall(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 8);
        BlockPos wall = BlockPos.containing(boss.getX() + 5, boss.getY(), boss.getZ());
        for (BlockPos pos : BlockPos.betweenClosed(wall.offset(0, -3, -3), wall.offset(0, 6, 3))) {
            level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        float health = player.getHealth();
        boss.startMove(level, AttackType.PROBE, player);
        helper.runAfterDelay(ProsectorConfig.PROBE.startup().get() + ProsectorConfig.PROBE.active().get(), () -> {
            helper.assertTrue(player.getHealth() == health, "behind the wall nothing burns");
            for (BlockPos pos : BlockPos.betweenClosed(wall.offset(0, -3, -3), wall.offset(0, 6, 3))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
            done(boss, player);
            helper.succeed();
        });
    }

    /** A heavy hit stays heavy in the mod's best armour and does not one-shot a plain diamond set. */
    @HexTest(padding = 6)
    public static void hitsStayHeavyThroughTheBestArmour(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer bare = near(helper, boss, 3);
        ServerPlayer armoured = near(helper, boss, -3);
        armoured.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.RIFTFORGED_BULWARK_HELMET.get()));
        armoured.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.RIFTFORGED_BULWARK_CHESTPLATE.get()));
        armoured.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.RIFTFORGED_BULWARK_LEGGINGS.get()));
        armoured.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.RIFTFORGED_BULWARK_BOOTS.get()));
        armoured.doTick();
        DamageSource source = ModDamageTypes.dissection(level, boss);
        float heavy = ProsectorConfig.INCISION.damage().get().floatValue();
        float top = ProsectorDamage.cost(level, armoured, source, heavy);
        float none = ProsectorDamage.cost(level, bare, source, heavy);
        helper.assertTrue(top >= 8 && top <= 11, "a heavy hit costs the best armour about half a health bar: " + top);
        helper.assertTrue(none > top * 2, "and an unarmoured player far more: " + none);
        ServerPlayer diamond = near(helper, boss, 5);
        diamond.setItemSlot(EquipmentSlot.HEAD, new ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET));
        diamond.setItemSlot(EquipmentSlot.CHEST, new ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE));
        diamond.setItemSlot(EquipmentSlot.LEGS, new ItemStack(net.minecraft.world.item.Items.DIAMOND_LEGGINGS));
        diamond.setItemSlot(EquipmentSlot.FEET, new ItemStack(net.minecraft.world.item.Items.DIAMOND_BOOTS));
        diamond.doTick();
        float worst = ProsectorDamage.cost(level, diamond, source, ProsectorConfig.EXCISION.damage().get().floatValue());
        helper.assertTrue(worst < 20, "its heaviest move never one-shots a full diamond set: " + worst);
        done(boss, bare, armoured, diamond);
        helper.succeed();
    }

    // ---- the ground and the Eye ------------------------------------------------------------------------------------

    /** One high over the test's spot, already awake, with a long pause. */
    private static Prosector highUp(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos spot = helper.absolutePos(BlockPos.ZERO).above(FIGHT_HEIGHT);
        Prosector boss = ModEntities.PROSECTOR.get().create(level);
        boss.setPos(Vec3.atBottomCenterOf(spot));
        level.addFreshEntity(boss);
        boss.idle();
        boss.pause(100000);
        return boss;
    }

    /** The ground is found under the air, out of a hill, and past a tree. */
    @HexTest(padding = 4)
    public static void theGroundIsFoundOverHillsAndUnderTrees(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        standOnFloor(helper);
        BlockPos floor = helper.absolutePos(new BlockPos(2, 1, 2));
        double top = floor.getY() + 1;
        helper.assertTrue(Surface.height(level, floor.getX() + 0.5, floor.getZ() + 0.5, top + 8) == top, "down through the air");
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.STONE);
        helper.assertTrue(Surface.height(level, floor.getX() + 0.5, floor.getZ() + 0.5, top) == top + 2, "up out of a hill");
        helper.setBlock(new BlockPos(4, 2, 4), Blocks.OAK_LOG);
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.OAK_LEAVES);
        helper.assertTrue(Surface.height(level, floor.getX() + 2.5, floor.getZ() + 2.5, top) == top, "and trees do not count");
        helper.succeed();
    }

    /** An open cell throws off whoever stands on it, leaves the blocks as they were, and closes. */
    @HexTest(maxTicks = 80, padding = 8)
    public static void openCellsThrowOffAndClose(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        standOnFloor(helper);
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 3);
        player.spawnInvulnerableTime = 0;
        player.getAbilities().invulnerable = false;
        BlockPos under = helper.absolutePos(new BlockPos(5, 1, 5));
        Vec3 middle = Vec3.atBottomCenterOf(under.above());
        player.moveTo(middle.x + 0.5, middle.y, middle.z, 0, 0);
        CutGround cut = boss.cut();
        cut.cut(level, middle, new HexCells.Cell(0, 0), middle, 30);
        helper.assertTrue(cut.open() == 1 && cut.covers(player.position()), "the cell lies under them");
        float before = player.getHealth();
        helper.runAfterDelay(14, () -> {
            helper.assertTrue(player.getHealth() < before, "it hurt them");
            helper.assertTrue(level.getBlockState(under).is(Blocks.STONE), "and no block went");
        });
        helper.runAfterDelay(36, () -> {
            helper.assertTrue(cut.open() == 0, "and it closed");
            done(boss, player);
            helper.succeed();
        });
    }

    /** Every move starts and runs on plain ground, leaves nothing behind and takes no block. */
    @HexTest(maxTicks = 2400, padding = 8)
    public static void everyMoveRunsOnPlainGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        standOnFloor(helper);
        helper.setBlock(new BlockPos(8, 2, 8), Blocks.STONE);
        Prosector boss = awake(helper);
        ServerPlayer player = VoidLifeGameTests.survivor(helper);
        player.spawnInvulnerableTime = 0;
        player.getAbilities().invulnerable = true;
        Vec3 stand = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(8, 3, 8)));
        player.moveTo(stand.x, stand.y, stand.z, 0, 0);
        int every = 190;
        int tick = 2;
        for (AttackType type : AttackType.values()) {
            helper.runAfterDelay(tick, () -> {
                if (boss.attack() != null) {
                    boss.attack().cancel(level);
                }
                boss.startMove(level, type, player);
                helper.assertTrue(boss.state() == type.state(), type + " began");
            });
            tick += every;
        }
        helper.runAfterDelay(tick + ProsectorConfig.CELL_RETURN_TICKS.get() + 20, () -> {
            helper.assertTrue(boss.isAlive(), "it went through its whole table");
            helper.assertTrue(level.getEntitiesOfClass(com.pockyl.hextale.thevoid.boss.HexPlug.class, boss.getBoundingBox().inflate(40)).isEmpty(), "no plug stayed");
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(8, 2, 8));
            helper.assertTrue(boss.cut().open() == 0, "and the ground closed");
            done(boss, player);
            helper.succeed();
        });
    }

    /** There are no walls to keep a bow out, so a hit from outside the fight does nothing. */
    @HexTest(padding = 6)
    public static void aHitFromOutsideTheFightDoesNothing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 3);
        helper.assertTrue(hit(boss, level, level.damageSources().playerAttack(player), 10) > 0, "from inside it counts");
        double out = ProsectorConfig.FIGHT_RADIUS.get() + 10;
        player.moveTo(boss.getX() + out, boss.getY(), boss.getZ(), 0, 0);
        helper.assertTrue(hit(boss, level, level.damageSources().playerAttack(player), 10) == 0, "from outside it does not");
        done(boss, player);
        helper.succeed();
    }

    /** A living jar feeds it, but never past the last milestone. */
    @HexTest(maxTicks = 120, padding = 6)
    public static void aLivingJarHealsItUpToTheMilestone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 3);
        write(boss, level, player);
        float top = boss.getMaxHealth() * ProsectorConfig.PHASE2_AT.get().floatValue();
        boss.setHealth(boss.getMaxHealth() * 0.5F);
        float low = boss.getHealth();
        helper.runAfterDelay(45, () -> {
            helper.assertTrue(boss.getHealth() > low, "it healed");
            boss.setHealth(top - 0.5F);
        });
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(boss.getHealth() <= top + 0.01F, "and stops at the milestone: " + boss.getHealth());
            done(boss, player);
            helper.succeed();
        });
    }

    /** Peaceful is said first, a roof over the spot refuses, and a refused Eye stays whole. */
    @HexTest(padding = 6)
    public static void theEyeIsRefusedOnPeacefulAndUnderARoof(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        standOnFloor(helper);
        ServerPlayer player = VoidLifeGameTests.survivor(helper);
        ItemStack eye = new ItemStack(ModItems.SEERS_EYE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, eye);
        BlockPos ground = helper.absolutePos(new BlockPos(3, 1, 3));
        helper.setBlock(new BlockPos(3, 5, 3), Blocks.STONE);
        Difficulty difficulty = level.getDifficulty();
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        helper.assertTrue(SeersEyeItem.refusal(level, ground.above()) == SeersEyeItem.Refusal.NO_ROOM, "a roof over the spot refuses");
        var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(ground), net.minecraft.core.Direction.UP, ground, false);
        ModItems.SEERS_EYE.get().useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1, "and the Eye stays whole");
        level.getServer().setDifficulty(Difficulty.PEACEFUL, true);
        helper.assertTrue(SeersEyeItem.refusal(level, ground.above()) == SeersEyeItem.Refusal.PEACEFUL, "Peaceful is said first");
        level.getServer().setDifficulty(difficulty, true);
        player.discard();
        helper.succeed();
    }

    /** Called up: under the ground and shielded at first, out and awake once the ritual ends, and a second Eye is refused. */
    @HexTest(maxTicks = 300, padding = 8)
    public static void anEyeCallsItUpOutOfTheGround(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        standOnFloor(helper);
        BlockPos spot = helper.absolutePos(new BlockPos(5, 2, 5));
        Prosector boss = Prosector.summon(level, spot);
        helper.assertTrue(boss.state() == ProsectorState.RISING && boss.shielded(), "it begins rising, out of reach");
        helper.assertTrue(boss.getY() < spot.getY(), "under the ground");
        helper.assertTrue(SeersEyeItem.refusal(level, spot) == SeersEyeItem.Refusal.TAKEN, "a second Eye is refused");
        helper.runAfterDelay(ProsectorConfig.SUMMON_TICKS.get() + 10, () -> {
            helper.assertTrue(boss.state() == ProsectorState.WAKING, "it wakes: " + boss.state());
            helper.assertTrue(boss.getY() > spot.getY(), "over the ground");
            helper.assertTrue(boss.inFight(Vec3.atBottomCenterOf(spot)), "and the spot is the middle of the fight");
            done(boss);
            helper.succeed();
        });
    }

    // ---- what it leaves --------------------------------------------------------------------------------------------

    /** The loot bursts out at random onto the ground: materials, scrolls, and no chest. */
    @HexTest(maxTicks = 300, padding = 8)
    public static void aKillDropsItsLoot(GameTestHelper helper) {
        standOnFloor(helper);
        ServerLevel level = helper.getLevel();
        Prosector boss = awake(helper);
        ServerPlayer player = near(helper, boss, 3);
        player.getAbilities().invulnerable = true;
        hit(boss, level, level.damageSources().magic(), 10);
        Vec3 spot = boss.groundUnder(boss.position());
        boss.kill();
        helper.runAfterDelay(ProsectorConfig.DEATH_TICKS.get() + 5, () -> {
            helper.assertTrue(boss.isRemoved(), "the long death ended");
            BlockPos at = BlockPos.containing(spot);
            helper.assertTrue(level.getBlockState(at).isAir(), "no chest stands there");
            var drops = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(at).inflate(12));
            int scrolls = 0;
            int others = 0;
            for (var drop : drops) {
                boolean scroll = drop.getItem().is(ModItems.RUNE_SCROLL.get());
                scrolls += scroll ? 1 : 0;
                others += scroll ? 0 : 1;
            }
            helper.assertTrue(scrolls >= ProsectorConfig.MIN_SCROLLS.get(), "rune scrolls: " + scrolls);
            helper.assertTrue(others >= 4, "at least four rolls of the table: " + others);
            drops.forEach(net.minecraft.world.entity.Entity::discard);
            done(boss, player);
            for (var orb : level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, new AABB(at).inflate(8))) {
                orb.discard();
            }
            helper.succeed();
        });
    }

    /** Anatomy: the tally on the sword grows with kills of a kind, the bonus stops at its cap, and only the Prosector leaves it. */
    @HexTest(padding = 4)
    public static void anatomyGrowsWithKillsOfAKind(GameTestHelper helper) {
        var husk = helper.spawn(net.minecraft.world.entity.EntityType.HUSK, new BlockPos(2, 2, 2));
        ItemStack sword = new ItemStack(ModItems.HEXTECH_SWORD.get());
        helper.assertTrue(ToolEffects.anatomyBonus(sword, husk, 3) == 0, "no kills, no bonus");
        for (int i = 0; i < ToolRune.ANATOMY_KILLS; i++) {
            ToolEffects.anatomyTally(sword, husk);
        }
        float one = ToolEffects.anatomyBonus(sword, husk, 3);
        helper.assertTrue(one > 0, "ten kills make a step");
        sword.set(ModDataComponents.ANATOMY.get(), Map.of(net.minecraft.world.entity.EntityType.getKey(husk.getType()), 100000));
        helper.assertTrue(ToolEffects.anatomyBonus(sword, husk, 3) == ToolRune.ANATOMY_CAP, "and it stops at the cap");
        for (int chapter = 1; chapter <= 3; chapter++) {
            helper.assertTrue(!ResearchBook.findable(chapter).contains(ToolRune.ANATOMY.rune()), "no chest holds it");
        }
        husk.discard();
        helper.succeed();
    }
}
