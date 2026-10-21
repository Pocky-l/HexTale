package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.hextale.block.LabBenchBlock;
import com.pockyl.hextale.block.LabBenchBlockEntity;
import com.pockyl.hextale.block.ResearchTableBlock;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.block.TablePart;
import com.pockyl.hextale.chem.Flask;
import com.pockyl.hextale.chem.Reactions;
import com.pockyl.hextale.item.ResearchNotesItem;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.research.ExperimentPlan;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.Essence;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The Lab Bench: the flask's chemistry follows the essences, every chemtech experiment can be finished by plain play,
 * a sealed flask writes the notes, too much pressure bursts the flask, and chemtech is no longer researched at the
 * Research Table.
 */
public final class LabGameTests {
    private static final BlockPos BENCH = new BlockPos(2, 1, 1);

    private LabGameTests() {
    }

    @HexTest
    public static void reactionsFollowTheEssences(GameTestHelper helper) {
        helper.assertTrue(Reactions.product(Essence.EARTH, Essence.LIFE) == Essence.PLANT, "earth and life make plant");
        helper.assertTrue(Reactions.product(Essence.LIFE, Essence.EARTH) == Essence.PLANT, "in either order");
        helper.assertTrue(Reactions.product(Essence.FIRE, Essence.FIRE) == null, "an essence does not react with itself");
        helper.assertTrue(Reactions.zone(Essence.STEAM) == 3, "steam forms hot");
        helper.assertTrue(Reactions.zone(Essence.COLD) == 0, "cold forms cold");
        helper.assertTrue(Reactions.zone(Essence.MAGMA) == 3, "magma forms hot");
        Flask flask = new Flask();
        flask.drop(Essence.FIRE);
        flask.drop(Essence.WATER);
        for (int i = 0; i < 200; i++) {
            flask.tick(Flask.Tuning.NONE);
        }
        helper.assertTrue(flask.amount(Essence.STEAM) == 0, "no steam in a cold flask");
        flask.setFlame(3);
        for (int i = 0; i < 400; i++) {
            flask.tick(Flask.Tuning.NONE);
        }
        helper.assertTrue(flask.amount(Essence.STEAM) > 1.9F, "hot, fire and water turn into two parts of steam");
        helper.assertTrue(flask.pressure() > 0, "steam presses on the flask");
        helper.succeed();
    }

    /** A plain strategy finishes every experiment: make each compound of the chain in its zone, parents in equal parts. */
    @HexTest
    public static void everyExperimentCanBeFinished(GameTestHelper helper) {
        for (ResearchTopic topic : ResearchTopics.lab()) {
            ExperimentPlan plan = ExperimentPlan.of(topic, false);
            // The order of the side reactions is part of the puzzle: try the chain as listed and with its first two swapped.
            List<Essence> chain = plan.chain();
            Flask flask = play(plan, chain);
            if (chain.size() > 2 && (flask.amount(plan.target()) < plan.amount() || flask.purity(plan.target()) < plan.purity())) {
                List<Essence> swapped = new java.util.ArrayList<>(chain);
                java.util.Collections.swap(swapped, 0, 1);
                flask = play(plan, swapped);
            }
            helper.assertFalse(flask.burst(), topic.id() + ": the flask holds");
            helper.assertTrue(flask.amount(plan.target()) >= plan.amount(),
                    topic.id() + ": " + flask.amount(plan.target()) + " of " + plan.amount() + " " + plan.target().id() + " in "
                            + describe(flask));
            helper.assertTrue(flask.purity(plan.target()) >= plan.purity(), topic.id() + ": purity " + flask.purity(plan.target()));
        }
        helper.succeed();
    }

    private static Flask play(ExperimentPlan plan, List<Essence> chain) {
        int depth = plan.target().tier();
        int unit = 1 << depth;
        // A little over the plan: venting lets some of everything out.
        int aim = (plan.amount() + unit - 1) / unit * unit + unit;
        Map<Essence, Integer> wanted = new EnumMap<>(Essence.class);
        wanted.put(plan.target(), aim);
        for (int i = chain.size() - 1; i >= 0; i--) {
            Essence compound = chain.get(i);
            int half = wanted.getOrDefault(compound, 0) / 2;
            for (Essence parent : compound.parents()) {
                wanted.merge(parent, half, Integer::sum);
            }
        }
        Flask flask = new Flask();
        for (Essence compound : chain) {
            int zone = Reactions.zone(compound);
            flask.setFlame(zone);
            for (int t = 0; t < 600 && Reactions.zoneAt(flask.temperature()) != zone; t++) {
                step(flask);
            }
            for (Essence parent : compound.parents()) {
                if (parent.isPrimal()) {
                    for (int i = 0; i < wanted.get(compound) / 2; i++) {
                        flask.drop(parent);
                    }
                }
            }
            for (int t = 0; t < 1200 && flask.amount(compound) < wanted.get(compound) * 0.98F; t++) {
                step(flask);
            }
        }
        return flask;
    }

    private static String describe(Flask flask) {
        return flask.contents().stream().map(e -> e.id() + "=" + flask.amount(e)).toList() + " at " + flask.temperature();
    }

    private static void step(Flask flask) {
        flask.tick(Flask.Tuning.NONE);
        if (flask.pressure() > 70) {
            flask.vent();
        }
    }

    @HexTest(maxTicks = 100)
    public static void aSealedFlaskWritesTheNotes(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Research.learnEverything(player);
        LabBenchBlock.place(helper.getLevel(), helper.absolutePos(BENCH), ModBlocks.LAB_BENCH.get().defaultBlockState());
        LabBenchBlockEntity bench = helper.getBlockEntity(BENCH, LabBenchBlockEntity.class);
        BlockState side = helper.getBlockState(BENCH.relative(TablePart.MAIN.toOther(bench.facing())));
        helper.assertTrue(side.is(ModBlocks.LAB_BENCH.get()) && side.getValue(LabBenchBlock.PART) == TablePart.SIDE, "the bench takes two blocks");
        bench.setItem(LabBenchBlockEntity.FLASK, new ItemStack(Items.GLASS_BOTTLE));
        bench.select(player, ResearchTopics.COMBUSTIBLE_MIX);
        helper.assertTrue(bench.topic() == ResearchTopics.COMBUSTIBLE_MIX, "the journal opens the topic");
        ExperimentPlan plan = bench.plan();
        Flask flask = bench.flask();
        for (int i = 0; i < plan.amount() / 2 + 1; i++) {
            flask.drop(Essence.FIRE);
            flask.drop(Essence.SHADOW);
        }
        flask.setFlame(Reactions.zone(plan.target()));
        for (int i = 0; i < 1200 && !bench.ready(); i++) {
            flask.tick(Flask.Tuning.NONE);
        }
        helper.assertTrue(bench.ready(), "the flask holds enough ash, pure enough");
        bench.seal(player);
        helper.assertTrue(bench.getItem(LabBenchBlockEntity.OUTPUT).isEmpty(), "no notes without the sample");
        bench.setItem(LabBenchBlockEntity.SAMPLE, new ItemStack(Items.COAL, 2));
        bench.seal(player);
        ItemStack notes = bench.getItem(LabBenchBlockEntity.OUTPUT);
        helper.assertTrue(ResearchNotesItem.topic(notes) == ResearchTopics.COMBUSTIBLE_MIX, "the sealed flask is written out as notes");
        helper.assertTrue(bench.getItem(LabBenchBlockEntity.SAMPLE).getCount() == 1, "one sample went into the flask");
        helper.assertTrue(flask.isEmpty(), "the flask is empty again");
        player.discard();
        helper.succeed();
    }

    @HexTest(maxTicks = 1200)
    public static void tooMuchPressureBurstsTheFlask(GameTestHelper helper) {
        LabBenchBlock.place(helper.getLevel(), helper.absolutePos(BENCH), ModBlocks.LAB_BENCH.get().defaultBlockState());
        LabBenchBlockEntity bench = helper.getBlockEntity(BENCH, LabBenchBlockEntity.class);
        bench.setItem(LabBenchBlockEntity.FLASK, new ItemStack(Items.GLASS_BOTTLE));
        for (int i = 0; i < 12; i++) {
            bench.flask().drop(Essence.FIRE);
            bench.flask().drop(Essence.SPARK);
        }
        bench.setFlame(Flask.FLAMES - 1);
        helper.succeedWhen(() -> {
            helper.assertTrue(bench.outcome() == LabBenchBlockEntity.OUTCOME_BURST, "the flask bursts");
            helper.assertTrue(bench.getItem(LabBenchBlockEntity.FLASK).isEmpty(), "the bottle is gone");
            helper.assertTrue(bench.flask().isEmpty() && bench.flask().flame() == 0, "nothing is left, the flame is out");
        });
    }

    @HexTest
    public static void chemtechIsNotResearchedAtTheTable(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Research.learnEverything(player);
        BlockPos table = new BlockPos(2, 1, 3);
        ResearchTableBlock.place(helper.getLevel(), helper.absolutePos(table), ModBlocks.RESEARCH_TABLE.get().defaultBlockState());
        ResearchTableBlockEntity entity = helper.getBlockEntity(table, ResearchTableBlockEntity.class);
        entity.setItem(ResearchTableBlockEntity.DIARY, new ItemStack(com.pockyl.hextale.registry.ModItems.RESEARCHERS_DIARY.get()));
        entity.select(player, ResearchTopics.CHEM_BURNER);
        helper.assertTrue(entity.sheet().topic() == null, "the Chem Burner is worked out at the Lab Bench");
        for (ResearchTopic topic : ResearchTopics.lab()) {
            helper.assertTrue(ExperimentPlan.of(topic, false).chain().contains(ExperimentPlan.of(topic, false).target()), topic.id() + " has its chain");
        }
        player.discard();
        helper.succeed();
    }
}
