package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.FusionResult;
import com.pockyl.hextale.block.Requirement;
import com.pockyl.hextale.block.ResearchTableBlock;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.block.TablePart;
import com.pockyl.hextale.item.DiaryOwner;
import com.pockyl.hextale.item.ResearchersDiaryItem;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.knowledge.DiaryNote;
import com.pockyl.hextale.knowledge.Discoveries;
import com.pockyl.hextale.knowledge.Milestone;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.RuneStrokes;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * The research loop on a server: fusing at the table, the orb cracking, lightning making orbs, knowledge kept on the
 * player. Every test runs in its own batch: lightning and mock players must not reach the neighbouring tests.
 */
@GameTestHolder(Hextale.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchGameTests {
    private static final BlockPos TABLE = new BlockPos(0, 1, 0);

    private ResearchGameTests() {
    }

    @GameTest(template = "empty", batch = "fusionConsumesExactlyTheMaterials")
    public static void fusionConsumesExactlyTheMaterials(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ResearchTableBlockEntity table = stockedTable(helper);

        FusionResult result = table.fuse(player, strokes(SimpleRune.FIRE), strokes(SimpleRune.EARTH));

        ComplexRune smelting = ComplexRunes.byKey("fire_earth");
        helper.assertTrue(result.status() == FusionResult.Status.FUSED, "fire -> earth fuses, got " + result.status());
        helper.assertTrue(result.rune() == smelting && result.fresh(), "the new rune is Smelting");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == 4, "one sheet of paper used");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.HEXTECH).getCount() == 2, "one orb used");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.INK).getDamageValue() == 1, "one dose of ink used");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.BRUSH).getDamageValue() == 1, "the brush wears by one");
        ItemStack scroll = table.getItem(ResearchTableBlockEntity.OUTPUT);
        helper.assertTrue(scroll.is(ModItems.RUNE_SCROLL.get()) && scroll.getCount() == 1, "a scroll lies in the output slot");
        helper.assertTrue(RuneScrollItem.rune(scroll) == smelting, "the scroll carries Smelting");
        RuneKnowledge knowledge = Research.of(player);
        helper.assertTrue(knowledge.knows(smelting) && knowledge.runeCount() == 1, "the player learned Smelting");
        helper.assertTrue(knowledge.hasNote(DiaryNote.FIRST_RUNE), "the diary notes the first rune");
        DiaryOwner owner = ResearchersDiaryItem.owner(table.getItem(ResearchTableBlockEntity.DIARY));
        helper.assertTrue(owner != null && owner.is(player), "the unsigned diary is signed by the researcher");

        FusionResult again = table.fuse(player, strokes(SimpleRune.FIRE), strokes(SimpleRune.EARTH));
        helper.assertTrue(again.status() == FusionResult.Status.FUSED && !again.fresh(), "a known rune still fuses, but is not fresh");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.OUTPUT).getCount() == 2, "equal scrolls stack in the output slot");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "orderChangesTheRune")
    public static void orderChangesTheRune(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        FusionResult forward = table.fuse(player, strokes(SimpleRune.FIRE), strokes(SimpleRune.EARTH));
        FusionResult reversed = table.fuse(player, strokes(SimpleRune.EARTH), strokes(SimpleRune.FIRE));
        helper.assertTrue(forward.rune() == ComplexRunes.byKey("fire_earth"), "fire -> earth is Smelting");
        helper.assertTrue(reversed.rune() == ComplexRunes.byKey("earth_fire"), "earth -> fire is Tempering");
        helper.assertTrue(Research.of(player).runeCount() == 2, "both are learned");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "sameElementCracksTheOrb")
    public static void sameElementCracksTheOrb(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ResearchTableBlockEntity table = stockedTable(helper);

        FusionResult result = table.fuse(player, strokes(SimpleRune.WATER), strokes(SimpleRune.WATER));

        helper.assertTrue(result.status() == FusionResult.Status.CRACKED, "water twice cracks the orb, got " + result.status());
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.HEXTECH).getCount() == 2, "the orb is gone");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == 5, "the paper is kept");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.INK).getDamageValue() == 0, "the ink is kept");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.BRUSH).getDamageValue() == 0, "the brush is kept");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.OUTPUT).isEmpty(), "no scroll");
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "the crack hurts nobody");
        RuneKnowledge knowledge = Research.of(player);
        helper.assertTrue(knowledge.runeCount() == 0, "nothing is learned");
        helper.assertTrue(knowledge.hasNote(DiaryNote.ORB_CRACKED), "the diary explains why the orb cracked");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "unrecognisedDrawingsCostNothing")
    public static void unrecognisedDrawingsCostNothing(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        RuneStrokes scribble = new RuneStrokes(RuneSamples.scribble(new Random(3)));
        RuneStrokes forged = new RuneStrokes(List.of(new float[]{0, 0, Float.NaN, 1}));

        FusionResult result = table.fuse(player, scribble, strokes(SimpleRune.AIR));
        FusionResult invalid = table.fuse(player, strokes(SimpleRune.AIR), forged);

        helper.assertTrue(result.status() == FusionResult.Status.UNRECOGNISED, "a scribble is not fused, got " + result.status());
        helper.assertTrue(invalid.status() == FusionResult.Status.UNRECOGNISED, "malformed strokes are refused");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == 5
                && table.getItem(ResearchTableBlockEntity.HEXTECH).getCount() == 3, "nothing is used up");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "tableIsTwoBlocks")
    public static void tableIsTwoBlocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos main = helper.absolutePos(TABLE);
        BlockState placed = ModBlocks.RESEARCH_TABLE.get().defaultBlockState().setValue(ResearchTableBlock.FACING, Direction.NORTH);
        ResearchTableBlock.place(level, main, placed);
        BlockPos side = main.relative(TablePart.MAIN.toOther(Direction.NORTH));
        BlockState sideState = level.getBlockState(side);
        helper.assertTrue(sideState.is(ModBlocks.RESEARCH_TABLE.get()) && sideState.getValue(ResearchTableBlock.PART) == TablePart.SIDE,
                "the desk has a side half");
        helper.assertTrue(level.getBlockEntity(side) == null, "only the main half has a block entity");
        helper.assertTrue(ModBlocks.RESEARCH_TABLE.get().getContainer(sideState, level, side) == level.getBlockEntity(main),
                "hoppers reach the table's slots through the side half");

        level.destroyBlock(side, true);
        helper.assertTrue(level.getBlockState(main).isAir(), "breaking one half breaks the whole desk");
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(main).inflate(3),
                item -> item.getItem().is(ModItems.RESEARCH_TABLE.get()));
        int count = drops.stream().mapToInt(item -> item.getItem().getCount()).sum();
        drops.forEach(ItemEntity::discard);
        helper.assertTrue(count == 1, "a broken desk drops exactly one table, got " + count);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "missingMaterialsAreNamed")
    public static void missingMaterialsAreNamed(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ResearchTableBlock.place(helper.getLevel(), helper.absolutePos(TABLE), ModBlocks.RESEARCH_TABLE.get().defaultBlockState());
        ResearchTableBlockEntity table = helper.getBlockEntity(TABLE);
        helper.assertTrue(table.fuse(player, strokes(SimpleRune.FIRE), strokes(SimpleRune.AIR)).missing() == Requirement.DIARY,
                "an empty table asks for the diary first");

        ItemStack foreign = new ItemStack(ModItems.RESEARCHERS_DIARY.get());
        foreign.set(ModDataComponents.DIARY_OWNER.get(), new DiaryOwner(UUID.randomUUID(), "Someone"));
        table.setItem(ResearchTableBlockEntity.DIARY, foreign);
        helper.assertTrue(table.fuse(player, strokes(SimpleRune.FIRE), strokes(SimpleRune.AIR)).missing() == Requirement.OWN_DIARY,
                "someone else's diary does not count");

        table.setItem(ResearchTableBlockEntity.DIARY, new ItemStack(ModItems.RESEARCHERS_DIARY.get()));
        table.setItem(ResearchTableBlockEntity.BRUSH, new ItemStack(ModItems.RUNE_BRUSH.get()));
        table.setItem(ResearchTableBlockEntity.INK, new ItemStack(Items.GLASS_BOTTLE));
        helper.assertTrue(table.fuse(player, strokes(SimpleRune.FIRE), strokes(SimpleRune.AIR)).missing() == Requirement.INK,
                "an empty bottle is no ink");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "lastDoseLeavesAnEmptyBottle")
    public static void lastDoseLeavesAnEmptyBottle(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        table.getItem(ResearchTableBlockEntity.INK).setDamageValue(table.getItem(ResearchTableBlockEntity.INK).getMaxDamage() - 1);
        table.getItem(ResearchTableBlockEntity.BRUSH).setDamageValue(table.getItem(ResearchTableBlockEntity.BRUSH).getMaxDamage() - 1);

        table.fuse(player, strokes(SimpleRune.LIFE), strokes(SimpleRune.METAL));

        helper.assertTrue(table.getItem(ResearchTableBlockEntity.INK).is(Items.GLASS_BOTTLE), "the last dose leaves the glass bottle");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.BRUSH).isEmpty(), "a worn-out brush breaks");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 60, batch = "lightningOnRodAndAmethystMakesOrbs")
    public static void lightningOnRodAndAmethystMakesOrbs(GameTestHelper helper) {
        strikeRodOnAmethyst(helper, 1);
    }

    @GameTest(template = "empty", timeoutTicks = 60, batch = "boltInsideTheRodAlsoWorks")
    public static void boltInsideTheRodAlsoWorks(GameTestHelper helper) {
        // A Channeling trident or /summon at the rod's own block puts the bolt inside the rod instead of on top.
        strikeRodOnAmethyst(helper, 0);
    }

    private static void strikeRodOnAmethyst(GameTestHelper helper, int boltAboveRod) {
        BlockPos amethyst = new BlockPos(1, 1, 1);
        BlockPos rod = amethyst.above();
        helper.setBlock(amethyst, Blocks.AMETHYST_BLOCK);
        helper.setBlock(rod, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.UP));
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
        helper.assertTrue(bolt != null, "a bolt can be created");
        bolt.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(rod.above(boltAboveRod))));
        helper.getLevel().addFreshEntity(bolt);
        // Polled every tick: a bolt in a freshly loaded test area may start ticking a few ticks late.
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, amethyst);
            helper.assertBlockPresent(Blocks.LIGHTNING_ROD, rod);
            List<ItemEntity> found = orbs(helper, amethyst);
            int orbs = found.stream().mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertTrue(orbs >= 1 && orbs <= 3, "the amethyst shattered into 1-3 orbs, got " + orbs);
            found.forEach(ItemEntity::discard);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 60, batch = "rodWithoutAmethystMakesNothing")
    public static void rodWithoutAmethystMakesNothing(GameTestHelper helper) {
        BlockPos base = new BlockPos(1, 1, 1);
        BlockPos rod = base.above();
        helper.setBlock(base, Blocks.STONE);
        helper.setBlock(rod, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.UP));
        orbs(helper, base).forEach(ItemEntity::discard);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(helper.getLevel());
        helper.assertTrue(bolt != null, "a bolt can be created");
        bolt.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(rod.above())));
        helper.getLevel().addFreshEntity(bolt);
        helper.runAfterDelay(10, () -> {
            helper.assertBlockPresent(Blocks.STONE, base);
            helper.assertTrue(orbs(helper, base).isEmpty(), "no orbs without amethyst");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "knowledgeSurvivesDeath")
    public static void knowledgeSurvivesDeath(GameTestHelper helper) {
        ServerPlayer before = player(helper);
        Research.learn(before, ComplexRunes.byKey("water_life"));
        Research.learn(before, ComplexRunes.byKey("life_metal"));
        Research.unlockNote(before, DiaryNote.FIRST_HEXTECH);
        Research.recordFailure(before);

        ServerPlayer after = player(helper);
        after.restoreFrom(before, false);

        RuneKnowledge knowledge = Research.of(after);
        helper.assertTrue(knowledge.runeCount() == 2 && knowledge.knows(ComplexRunes.byKey("water_life")), "the runes are kept");
        helper.assertTrue(knowledge.hasNote(DiaryNote.FIRST_HEXTECH) && knowledge.hasNote(DiaryNote.FIRST_FAILURE), "the notes are kept");
        helper.assertTrue(knowledge.failures() == 1, "the attempts are kept");
        helper.assertTrue(knowledge != Research.of(before), "the new player has its own copy");
        before.discard();
        after.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "milestonesWriteNotes")
    public static void milestonesWriteNotes(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        List<ComplexRune> first = Discoveries.suggest(Research.of(player), 1L, 3);
        helper.assertTrue(first.containsAll(Milestone.FIRST_DEVICES.featured()), "a beginner is pointed at the first devices' runes");

        Research.learnAll(player, ComplexRunes.all().subList(0, 12));
        RuneKnowledge knowledge = Research.of(player);
        helper.assertTrue(knowledge.hasNote(DiaryNote.MILESTONE_3) && knowledge.hasNote(DiaryNote.MILESTONE_12), "both milestones noted");
        helper.assertFalse(knowledge.hasNote(DiaryNote.MILESTONE_24), "24 is not reached yet");
        List<ComplexRune> next = Discoveries.suggest(knowledge, 1L, 3);
        helper.assertTrue(next.size() == 3 && next.stream().noneMatch(knowledge::knows), "suggestions are three unknown runes");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "scrollsTeachOtherResearchers")
    public static void scrollsTeachOtherResearchers(GameTestHelper helper) {
        ServerPlayer reader = player(helper);
        // Creative players keep their scrolls, like any other consumed item.
        reader.setGameMode(GameType.SURVIVAL);
        ComplexRune lens = ComplexRunes.byKey("water_light");
        ItemStack scroll = RuneScrollItem.of(lens, ModItems.RUNE_SCROLL.get());
        scroll.setCount(2);
        reader.setItemInHand(InteractionHand.MAIN_HAND, scroll);

        scroll.use(helper.getLevel(), reader, InteractionHand.MAIN_HAND);
        helper.assertTrue(Research.of(reader).knows(lens), "studying the scroll teaches the rune");
        helper.assertTrue(reader.getMainHandItem().getCount() == 1, "one scroll is used up");
        scroll.use(helper.getLevel(), reader, InteractionHand.MAIN_HAND);
        helper.assertTrue(reader.getMainHandItem().getCount() == 1, "a known rune's scroll is kept");
        reader.discard();
        helper.succeed();
    }

    /** Orbs lying around a position; tests clean them up, since the test area is reused by later batches. */
    private static List<ItemEntity> orbs(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(8),
                item -> item.getItem().is(ModItems.UNSTABLE_HEXTECH.get()));
    }

    private static ResearchTableBlockEntity stockedTable(GameTestHelper helper) {
        ResearchTableBlock.place(helper.getLevel(), helper.absolutePos(TABLE), ModBlocks.RESEARCH_TABLE.get().defaultBlockState());
        ResearchTableBlockEntity table = helper.getBlockEntity(TABLE);
        table.setItem(ResearchTableBlockEntity.DIARY, new ItemStack(ModItems.RESEARCHERS_DIARY.get()));
        table.setItem(ResearchTableBlockEntity.PAPER, new ItemStack(Items.PAPER, 5));
        table.setItem(ResearchTableBlockEntity.HEXTECH, new ItemStack(ModItems.UNSTABLE_HEXTECH.get(), 3));
        table.setItem(ResearchTableBlockEntity.INK, new ItemStack(ModItems.HEXTECH_INK.get()));
        table.setItem(ResearchTableBlockEntity.BRUSH, new ItemStack(ModItems.RUNE_BRUSH.get()));
        return table;
    }

    private static RuneStrokes strokes(SimpleRune rune) {
        return new RuneStrokes(RuneSamples.clean(rune));
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(0.5, 1, 2.5));
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);
        return player;
    }
}
