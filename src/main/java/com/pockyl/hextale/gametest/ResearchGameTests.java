package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.AlembicBlock;
import com.pockyl.hextale.block.AlembicBlockEntity;
import com.pockyl.hextale.block.BrassPipeBlock;
import com.pockyl.hextale.block.DrainValveBlock;
import com.pockyl.hextale.block.EssenceMachine;
import com.pockyl.hextale.block.PortMode;
import com.pockyl.hextale.block.PumpBlock;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.InkSupply;
import com.pockyl.hextale.block.SeparatorBlock;
import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.essence.Essence;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.block.Requirement;
import com.pockyl.hextale.block.ResearchTableBlock;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.block.TablePart;
import com.pockyl.hextale.item.DiaryOwner;
import com.pockyl.hextale.item.ResearchersDiaryItem;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.HexGrid;
import com.pockyl.hextale.research.ItemElementResolver;
import com.pockyl.hextale.research.ItemElements;
import com.pockyl.hextale.research.ItemStudy;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.research.ResearchSheet;
import com.pockyl.hextale.research.ResearchTool;
import com.pockyl.hextale.research.RuneBoard;
import com.pockyl.hextale.research.RuneBoards;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The research loop on a server: boards, threads and echoes at the table, lightning making orbs, knowledge kept on the
 * player.
 * Every test runs in its own batch: lightning and mock players must not reach the neighbouring tests.
 */
public final class ResearchGameTests {
    private static final BlockPos TABLE = new BlockPos(0, 1, 0);
    private static final ComplexRune SMELTING = ComplexRunes.byKey("fire_earth");

    private ResearchGameTests() {
    }

    // ---- Boards ---------------------------------------------------------------------------------------------------

    @HexTest(maxTicks = 400)
    public static void everyBoardCanBeFinished(GameTestHelper helper) {
        long start = System.nanoTime();
        int[] points = {0, 3, 4, 6};
        int[][] lengths = new int[4][2];
        for (int[] range : lengths) {
            range[0] = Integer.MAX_VALUE;
        }
        for (ComplexRune rune : ComplexRunes.all()) {
            RuneBoard board = RuneBoards.get(rune);
            int tier = board.tier();
            RuneBoard.Par par = board.par();
            helper.assertTrue(board.points().size() == points[tier], rune.key() + " has " + points[tier] + " points");
            helper.assertTrue(board.points().get(0).element() == rune.action() && board.points().get(1).element() == rune.target(),
                    rune.key() + ": the first points are its action and target");
            helper.assertTrue(par.three() >= 1 && par.three() == par.plain() && par.two() > par.three(), rune.key() + ": sane thresholds " + par);
            helper.assertTrue(!board.wall(HexGrid.CENTRE), rune.key() + ": the orb is never walled in");
            int[] thread = RuneBoards.plainThread(board);
            helper.assertTrue(thread != null, rune.key() + ": a plain thread exists");
            ResearchSheet.Trace trace = ResearchSheet.trace(board, thread, ResearchSheet.Allowance.NONE);
            helper.assertTrue(trace != null && trace.complete(board), rune.key() + ": the plain thread follows the rules and passes every point");
            helper.assertTrue(thread.length - 1 == par.plain(), rune.key() + ": the thresholds come from that thread");
            lengths[tier][0] = Math.min(lengths[tier][0], par.plain());
            lengths[tier][1] = Math.max(lengths[tier][1], par.plain());
        }
        Hextale.LOGGER.info("Checked {} research boards in {} ms; plain threads by chapter: I {}-{}, II {}-{}, III {}-{}",
                ComplexRunes.COUNT, (System.nanoTime() - start) / 1_000_000, lengths[1][0], lengths[1][1], lengths[2][0], lengths[2][1],
                lengths[3][0], lengths[3][1]);
        helper.succeed();
    }

    @HexTest
    public static void theThreadFollowsTheRules(GameTestHelper helper) {
        RuneBoard board = RuneBoards.get(SMELTING);
        int[] plain = RuneBoards.plainThread(board);
        ResearchSheet.Allowance none = ResearchSheet.Allowance.NONE;
        helper.assertTrue(ResearchSheet.trace(board, new int[]{HexGrid.CENTRE}, none) != null, "a bare orb is a thread");
        helper.assertTrue(ResearchSheet.trace(board, new int[]{plain[1]}, none) == null, "a thread starts at the orb");
        int jump = -1;
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            if (HexGrid.steps(HexGrid.CENTRE, cell) == 3 && !board.wall(cell)) {
                jump = cell;
                break;
            }
        }
        helper.assertTrue(ResearchSheet.trace(board, new int[]{HexGrid.CENTRE, jump}, none) == null, "a thread never jumps");
        helper.assertTrue(ResearchSheet.trace(board, new int[]{HexGrid.CENTRE, plain[1], HexGrid.CENTRE}, none) == null,
                "a thread never returns to the orb");

        // A small loop back onto itself needs the fork quill.
        int a = plain[1];
        int b = -1;
        int c = -1;
        for (int n : HexGrid.neighbours(a)) {
            for (int m : HexGrid.neighbours(n)) {
                if (n != HexGrid.CENTRE && m != HexGrid.CENTRE && m != a && HexGrid.adjacent(m, a) && !board.wall(n) && !board.wall(m)) {
                    b = n;
                    c = m;
                }
            }
        }
        helper.assertTrue(b >= 0, "there is a small loop next to the orb");
        int[] loop = {HexGrid.CENTRE, a, b, c, a};
        helper.assertTrue(ResearchSheet.trace(board, loop, none) == null, "a thread never crosses itself");
        helper.assertTrue(ResearchSheet.trace(board, loop, new ResearchSheet.Allowance(1, 0, 0, 0)) != null, "the fork quill lets it cross once");

        // Through a dark rune only with the scraper.
        int wall = -1;
        int before = -1;
        for (int cell = 0; cell < HexGrid.COUNT && wall < 0; cell++) {
            if (board.wall(cell)) {
                for (int n : HexGrid.neighbours(cell)) {
                    if (!board.wall(n)) {
                        wall = cell;
                        before = n;
                        break;
                    }
                }
            }
        }
        int[] path = pathTo(board, before);
        int[] through = Arrays.copyOf(path, path.length + 1);
        through[path.length] = wall;
        helper.assertTrue(ResearchSheet.trace(board, path, none) != null, "the way to the dark rune is open");
        helper.assertTrue(ResearchSheet.trace(board, through, none) == null, "a dark rune blocks the thread");
        helper.assertTrue(ResearchSheet.trace(board, through, new ResearchSheet.Allowance(0, 1, 0, 0)) != null, "the scraper passes one");
        helper.succeed();
    }

    @HexTest
    public static void stepsPayTheInkOfTheirDirection(GameTestHelper helper) {
        RuneBoard board = RuneBoards.get(SMELTING);
        int[] thread = RuneBoards.plainThread(board);
        ResearchSheet.Trace trace = ResearchSheet.trace(board, thread, ResearchSheet.Allowance.NONE);
        int[] ink = new int[SimpleRune.values().length];
        for (int i = 1; i < thread.length; i++) {
            SimpleRune colour = ResearchSheet.toward(thread[i - 1], thread[i]);
            helper.assertTrue(trace.paying()[i] == colour, "section " + i + " costs the ink of its direction");
            ink[colour.ordinal()]++;
        }
        java.util.Set<SimpleRune> fromCentre = java.util.EnumSet.noneOf(SimpleRune.class);
        java.util.Set<SimpleRune> anywhere = java.util.EnumSet.noneOf(SimpleRune.class);
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            for (int next : HexGrid.neighbours(cell)) {
                anywhere.add(ResearchSheet.toward(cell, next));
                if (cell == HexGrid.CENTRE) {
                    fromCentre.add(ResearchSheet.toward(cell, next));
                }
            }
        }
        helper.assertTrue(fromCentre.size() == 6, "the six steps from the orb pay six different inks, got " + fromCentre);
        helper.assertTrue(anywhere.size() == 9, "every ink is needed somewhere on the parchment");
        helper.assertTrue(Arrays.equals(ink, trace.ink()), "the ink needed is the sum of the sections");
        ResearchSheet.Trace stretched = ResearchSheet.trace(board, thread, ResearchTool.allowance(0, 0, 2, 0));
        helper.assertTrue(Arrays.stream(stretched.ink()).sum() == thread.length - 1 - 8, "a two-star stretcher saves eight sections");
        ResearchSheet.Trace ground = ResearchSheet.trace(board, thread, ResearchTool.allowance(0, 0, 0, 3));
        helper.assertTrue(Arrays.stream(ground.ink()).sum() == thread.length - 1 - (thread.length - 1) / 2,
                "a three-star mortar makes every second section free");
        helper.succeed();
    }

    @HexTest
    public static void chaptersOpenInOrder(GameTestHelper helper) {
        RuneKnowledge knowledge = new RuneKnowledge();
        helper.assertTrue(ResearchBook.runes(1).size() == 6, "chapter I has Smelting and the five tool runes");
        helper.assertTrue(ResearchBook.open(knowledge, SMELTING), "Smelting is open from the start");
        helper.assertFalse(ResearchBook.open(knowledge, ResearchTool.LENS.rune()), "the tool runes wait for Smelting");
        knowledge.learn(SMELTING, 1, 8);
        helper.assertTrue(ResearchBook.open(knowledge, ResearchTool.LENS.rune()), "after Smelting the tool runes open");
        helper.assertFalse(ResearchBook.chapterOpen(knowledge, 2), "chapter II needs four runes of chapter I");
        for (ResearchTool tool : List.of(ResearchTool.LENS, ResearchTool.FORK, ResearchTool.MORTAR)) {
            knowledge.learn(tool.rune(), 1, 0);
        }
        helper.assertTrue(ResearchBook.chapterOpen(knowledge, 2), "four runes of chapter I open chapter II");
        helper.assertFalse(ResearchBook.chapterOpen(knowledge, 3), "chapter III is still closed");
        ResearchBook.runes(2).subList(0, ResearchBook.CHAPTER_III_AFTER).forEach(rune -> knowledge.learn(rune, 2, 0));
        helper.assertTrue(ResearchBook.chapterOpen(knowledge, 3), "ten runes of chapter II open chapter III");
        helper.assertTrue(ResearchBook.runes(1).size() + ResearchBook.runes(2).size() + ResearchBook.runes(3).size() == 72,
                "the chapters hold all 72 runes");
        helper.succeed();
    }

    @HexTest
    public static void everyItemCarriesElements(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        Map<Item, ItemElements> all = ItemElementResolver.resolve(server.getRecipeManager(), server.registryAccess());
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            ItemElements elements = all.get(item);
            helper.assertTrue(elements != null, BuiltInRegistries.ITEM.getKey(item) + " carries elements");
            helper.assertTrue(!elements.elements().isEmpty() && elements.elements().size() <= 3, item + ": one to three elements");
            elements.elements().values().forEach(points -> helper.assertTrue(points >= 1 && points <= 9, item + ": strength 1-9"));
        }
        for (Map.Entry<Item, ItemElements> entry : ResearchItems.canonical().entrySet()) {
            helper.assertTrue(entry.getValue().equals(all.get(entry.getKey())), entry.getKey() + " keeps its base elements");
        }
        StringBuilder sample = new StringBuilder();
        for (Item item : List.of(Items.OAK_LOG, Items.OAK_PLANKS, Items.STICK, Items.CRAFTING_TABLE, Items.IRON_PICKAXE, Items.IRON_BLOCK,
                Items.GLASS, Items.BREAD, Items.TNT, Items.PISTON, Items.ENDER_EYE, Items.WOOL.pick(DyeColor.WHITE), Items.DIAMOND_SWORD,
                Items.COOKED_BEEF, Items.NETHERITE_SWORD, Items.SPONGE, Items.BOOKSHELF, Items.LANTERN, Items.SHULKER_BOX,
                Items.ELYTRA, Items.DRAGON_EGG, Items.MUSIC_DISC_CAT, Items.ZOMBIE_SPAWN_EGG, Items.DIRT, Items.STONE_BRICKS)) {
            sample.append(BuiltInRegistries.ITEM.getKey(item).getPath()).append('=').append(all.get(item)).append("; ");
        }
        Hextale.LOGGER.info("Item elements sample: {}", sample);
        helper.succeed();
    }

    // ---- Echoes ---------------------------------------------------------------------------------------------------

    @HexTest
    public static void echoStringsAreDistinct(GameTestHelper helper) {
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (int string = 0; string < HexGrid.RING.length; string++) {
            int[] cells = ItemStudy.string(string);
            helper.assertTrue(cells.length >= 5, HexGrid.RING[string].id() + " has a string of at least five sections, got " + cells.length);
            for (int cell : cells) {
                helper.assertTrue(used.add(cell), "strings never share a section");
                helper.assertTrue(ItemStudy.stringOf(cell) == string, "a section knows its string");
            }
        }
        helper.assertTrue(ItemStudy.stringOf(HexGrid.CENTRE) < 0, "the centre belongs to no string");
        helper.succeed();
    }

    @HexTest
    public static void echoesStopAtTheNearestElement(GameTestHelper helper) {
        ItemElements elements = ItemElements.of(1, SimpleRune.FIRE, 3, SimpleRune.WATER, 1);
        ItemStudy study = new ItemStudy();
        study.begin(Items.STICK, elements);
        int fire = ItemStudy.hiding(SimpleRune.FIRE, 3);
        int water = ItemStudy.hiding(SimpleRune.WATER, 1);
        helper.assertTrue(study.maxStrikes() == 9, "three strikes plus three per element");
        int probe = ItemStudy.string(ring(SimpleRune.FIRE))[0];
        ItemStudy.Echo first = study.strike(probe, elements);
        int nearest = Math.min(HexGrid.steps(probe, fire), HexGrid.steps(probe, water));
        helper.assertTrue(first != null && first.radius() == nearest, "the ripple stops at the nearest element");
        helper.assertFalse(study.possible(probe), "inside the ripple nothing hides");
        helper.assertTrue(study.strike(probe, elements) == null, "a section is struck once");
        ItemStudy.Echo hit = study.strike(fire, elements);
        helper.assertTrue(hit != null && hit.radius() == 0 && study.isRevealed(SimpleRune.FIRE), "striking the element reveals it");
        helper.assertFalse(study.possible(ItemStudy.string(ring(SimpleRune.FIRE))[0]), "a revealed string is quiet");
        int aside = -1;
        for (int n : HexGrid.neighbours(fire)) {
            if (aside < 0 && n != water && !study.struck(n)) {
                aside = n;
            }
        }
        ItemStudy.Echo next = study.strike(aside, elements);
        helper.assertTrue(next != null && next.radius() == HexGrid.steps(next.cell(), water), "a revealed element no longer answers");
        study.strike(water, elements);
        helper.assertTrue(study.won(elements) && !study.isActive(elements), "both elements found: the item is studied");
        helper.succeed();
    }

    /** A sensible player - strike where the answer splits the possible sections best - studies almost every item. */
    @HexTest(maxTicks = 600)
    public static void echoesAreFairForEveryItem(GameTestHelper helper) {
        int items = 0;
        int won = 0;
        int used = 0;
        Map<Integer, List<Integer>> byCount = new java.util.TreeMap<>();
        for (Item item : BuiltInRegistries.ITEM) {
            ItemElements elements = ResearchItems.of(item);
            if (elements == null) {
                continue;
            }
            ItemStudy study = new ItemStudy();
            study.begin(item, elements);
            while (study.isActive(elements)) {
                int cell = sensibleStrike(study, elements);
                helper.assertTrue(cell >= 0 && study.strike(cell, elements) != null, item + ": there is always a strike to make");
            }
            items++;
            if (study.won(elements)) {
                won++;
                used += study.strikes();
                byCount.computeIfAbsent(elements.elements().size(), k -> new ArrayList<>()).add(study.strikes());
            }
        }
        Hextale.LOGGER.info("Echoes: a sensible player studies {}/{} items, {} strikes on average", won, items,
                won == 0 ? "-" : String.format("%.1f", used / (double) won));
        byCount.forEach((count, strikes) -> {
            Collections.sort(strikes);
            Hextale.LOGGER.info("Echoes, {} elements: {} items, strikes median {}, 90% within {}", count, strikes.size(),
                    strikes.get(strikes.size() / 2), strikes.get(strikes.size() * 9 / 10));
        });
        helper.assertTrue(won >= items * 0.9, "a sensible player studies at least 90% of the items, got " + won + "/" + items);
        helper.succeed();
    }

    /**
     * What the table shows a player: the hot sections on the rim of the newest open echo (else every possible section)
     * are the suspects; strike the section whose answer splits them best, a suspect itself when two or fewer are left.
     */
    private static int sensibleStrike(ItemStudy study, ItemElements elements) {
        List<Integer> suspects = new ArrayList<>();
        ItemStudy.Echo clue = null;
        for (ItemStudy.Echo echo : study.echoes()) {
            if (study.open(echo, elements)) {
                clue = echo;
            }
        }
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            if (study.possible(cell) && (clue == null || HexGrid.steps(clue.cell(), cell) == clue.radius())) {
                suspects.add(cell);
            }
        }
        if (suspects.isEmpty()) {
            for (int cell = 0; cell < HexGrid.COUNT; cell++) {
                if (study.possible(cell)) {
                    suspects.add(cell);
                }
            }
        }
        if (suspects.size() <= 2) {
            return suspects.get(0);
        }
        int best = -1;
        double bestScore = Double.MAX_VALUE;
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            if (!study.canStrike(cell, elements)) {
                continue;
            }
            Map<Integer, Integer> byDistance = new HashMap<>();
            for (int p : suspects) {
                byDistance.merge(HexGrid.steps(cell, p), 1, Integer::sum);
            }
            double score = 0;
            for (int count : byDistance.values()) {
                score += count * (double) count;
            }
            if (suspects.contains(cell)) {
                score -= 0.5;
            }
            if (score < bestScore) {
                bestScore = score;
                best = cell;
            }
        }
        return best;
    }

    @HexTest
    public static void studyingAnItemAtTheTable(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        table.setItem(ResearchTableBlockEntity.HEXTECH, ItemStack.EMPTY);
        table.setItem(ResearchTableBlockEntity.PAPER, new ItemStack(Items.PAPER, 2));
        table.setItem(ResearchTableBlockEntity.STUDY, new ItemStack(Items.GLASS, 3));
        table.studySlotChanged(player);
        helper.assertTrue(table.isStudyingItem() && table.study().item() == Items.GLASS, "glass lies on the parchment, no orb needed");
        ItemElements elements = ResearchItems.of(Items.GLASS);
        for (Map.Entry<SimpleRune, Integer> entry : elements.elements().entrySet()) {
            table.strike(player, ItemStudy.hiding(entry.getKey(), entry.getValue()));
        }
        Identifier glass = BuiltInRegistries.ITEM.getKey(Items.GLASS);
        helper.assertTrue(Research.of(player).hasStudied(glass), "glass is studied");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.STUDY).getCount() == 2, "one glass went into the study");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == 1, "the study took one paper from the table");
        table.closeStudy(player);
        helper.assertFalse(table.isStudyingItem(), "a studied item is not studied again");

        // A lost study: strikes away from the elements until they run out; the item is used once.
        table.setItem(ResearchTableBlockEntity.STUDY, new ItemStack(Items.FEATHER, 2));
        table.studySlotChanged(player);
        ItemElements feather = ResearchItems.of(Items.FEATHER);
        for (int cell = 0; cell < HexGrid.COUNT && table.study().isActive(feather); cell++) {
            boolean answer = false;
            for (Map.Entry<SimpleRune, Integer> entry : feather.elements().entrySet()) {
                answer |= ItemStudy.hiding(entry.getKey(), entry.getValue()) == cell;
            }
            if (!answer) {
                table.strike(player, cell);
            }
        }
        helper.assertTrue(table.study().lost(feather), "the strikes ran out");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.STUDY).getCount() == 1, "the study used one feather");
        helper.assertFalse(Research.of(player).hasStudied(BuiltInRegistries.ITEM.getKey(Items.FEATHER)), "a lost study teaches nothing");
        table.closeStudy(player);
        helper.assertTrue(table.isStudyingItem() && !table.study().started(), "a click on a lost study tries the next feather");
        player.discard();
        helper.succeed();
    }

    private static int ring(SimpleRune element) {
        for (int i = 0; i < HexGrid.RING.length; i++) {
            if (HexGrid.RING[i] == element) {
                return i;
            }
        }
        throw new IllegalArgumentException(element.id());
    }

    // ---- The table ------------------------------------------------------------------------------------------------

    // ---- Essence works --------------------------------------------------------------------------------------------

    @HexTest
    public static void essencePartsKeepTheirProportions(GameTestHelper helper) {
        Essence mixture = Essence.of(java.util.Map.of(SimpleRune.LIFE, 12, SimpleRune.EARTH, 4));
        Essence part = mixture.part(8);
        helper.assertTrue(part.get(SimpleRune.LIFE) == 6 && part.get(SimpleRune.EARTH) == 2, "a part keeps three life to one earth: " + part);
        helper.assertTrue(mixture.minus(part).total() == 8, "what is left is the rest");
        helper.assertTrue(mixture.pureElement() == null && mixture.kinds() == 2, "two elements make a mixture");
        helper.assertTrue(Essence.pure(SimpleRune.FIRE, 3).pureElement() == SimpleRune.FIRE, "one element makes a concentrate");
        Essence odd = Essence.of(java.util.Map.of(SimpleRune.FIRE, 1, SimpleRune.WATER, 1, SimpleRune.AIR, 1));
        helper.assertTrue(odd.part(2).total() == 2, "rounding never loses or makes drops");
        helper.succeed();
    }

    @HexTest(maxTicks = 300)
    public static void anAlembicBoilsAnItemIntoAMixture(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at.below(), Blocks.MAGMA_BLOCK);
        helper.setBlock(at, ModBlocks.ALEMBIC.get().defaultBlockState().setValue(AlembicBlock.LIT, true));
        AlembicBlockEntity alembic = helper.getBlockEntity(at, AlembicBlockEntity.class);
        alembic.setItem(AlembicBlockEntity.INPUT, new ItemStack(Items.OAK_PLANKS));
        alembic.setItem(AlembicBlockEntity.FLASK_IN, new ItemStack(Items.GLASS_BOTTLE, 2));
        alembic.fillWater(1000, false);
        Essence expected = Essence.boiled(ResearchItems.of(Items.OAK_PLANKS), AlembicBlockEntity.DROPS_PER_POINT);
        helper.succeedWhen(() -> {
            Essence boiled = EssenceFlaskItem.contents(alembic.getItem(AlembicBlockEntity.FLASK_OUT));
            helper.assertTrue(boiled.equals(expected), "oak planks boil into " + expected + ", got " + boiled);
            helper.assertTrue(alembic.getItem(AlembicBlockEntity.FLASK_IN).getCount() == 1, "one glass bottle was used");
            helper.assertTrue(alembic.water() == 1000 - AlembicBlockEntity.WATER_PER_ITEM, "and the item took its water");
        });
    }

    @HexTest(maxTicks = 200)
    public static void aDryAlembicDoesNotBoil(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at.below(), Blocks.MAGMA_BLOCK);
        helper.setBlock(at, ModBlocks.ALEMBIC.get().defaultBlockState().setValue(AlembicBlock.LIT, true));
        AlembicBlockEntity alembic = helper.getBlockEntity(at, AlembicBlockEntity.class);
        alembic.setItem(AlembicBlockEntity.INPUT, new ItemStack(Items.OAK_PLANKS));
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(alembic.input().getCount() == 1 && alembic.tank().isEmpty(), "no water, no boiling");
            helper.succeed();
        });
    }

    @HexTest(maxTicks = 100)
    public static void aPumpInWaterFillsTheAlembic(GameTestHelper helper) {
        // pump (in water) -> pipe -> pipe -> the alembic's back (it faces north, so its back is south)
        BlockPos pump = new BlockPos(1, 2, 3);
        BlockPos alembicAt = new BlockPos(1, 3, 2);
        helper.setBlock(pump, ModBlocks.PUMP.get().defaultBlockState().setValue(PumpBlock.WATERLOGGED, true));
        helper.setBlock(pump.above(), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(alembicAt, ModBlocks.ALEMBIC.get().defaultBlockState().setValue(AlembicBlock.FACING, Direction.NORTH));
        AlembicBlockEntity alembic = helper.getBlockEntity(alembicAt, AlembicBlockEntity.class);
        helper.assertTrue(helper.getBlockState(alembicAt).getValue(EssenceMachine.PORTS.get(Direction.SOUTH)), "the pipe joins the alembic's back");
        helper.succeedWhen(() -> helper.assertTrue(alembic.water() >= 500, "the pump fills the flask, got " + alembic.water() + " mB"));
    }

    @HexTest(maxTicks = 100)
    public static void anOpenValveDrainsWhatComes(GameTestHelper helper) {
        BlockPos tankAt = new BlockPos(1, 3, 1);
        BlockPos valveAt = new BlockPos(2, 2, 1);
        helper.setBlock(tankAt, ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(tankAt.below(), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(valveAt, ModBlocks.DRAIN_VALVE.get());
        EssenceTankBlockEntity tank = helper.getBlockEntity(tankAt, EssenceTankBlockEntity.class);
        tank.tank().set(Essence.pure(SimpleRune.FIRE, 12));
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(tank.tank().amount() == 12, "a shut valve lets nothing out");
            helper.setBlock(valveAt, helper.getBlockState(valveAt).setValue(DrainValveBlock.OPEN, true));
            helper.succeedWhen(() -> helper.assertTrue(tank.tank().isEmpty(), "the open valve drains the tank"));
        });
    }

    @HexTest(maxTicks = 120)
    public static void aColdAlembicDoesNothing(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at.below(), Blocks.STONE);
        helper.setBlock(at, ModBlocks.ALEMBIC.get().defaultBlockState());
        AlembicBlockEntity alembic = helper.getBlockEntity(at, AlembicBlockEntity.class);
        alembic.setItem(AlembicBlockEntity.INPUT, new ItemStack(Items.COAL, 2));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(alembic.input().getCount() == 2 && alembic.progress() == 0, "no heat, no boiling");
            helper.succeed();
        });
    }

    @HexTest(maxTicks = 400)
    public static void aReagentSeparatesOneElement(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.SEPARATOR.get().defaultBlockState());
        SeparatorBlockEntity separator = helper.getBlockEntity(at, SeparatorBlockEntity.class);
        Essence mixture = Essence.of(java.util.Map.of(SimpleRune.LIFE, 8, SimpleRune.EARTH, 8));
        separator.setItem(SeparatorBlockEntity.POUR, EssenceFlaskItem.filled(mixture));
        separator.setItem(SeparatorBlockEntity.REAGENT, new ItemStack(Items.CLAY_BALL, 3));
        separator.setItem(SeparatorBlockEntity.SUPPLY, new ItemStack(Items.GLASS_BOTTLE, 2));
        helper.succeedWhen(() -> {
            Essence rest = EssenceFlaskItem.contents(separator.getItem(SeparatorBlockEntity.REST_OUT));
            Essence drawn = EssenceFlaskItem.contents(separator.getItem(SeparatorBlockEntity.EXTRACT_OUT));
            helper.assertTrue(rest.equals(Essence.pure(SimpleRune.LIFE, 8).asUnknown()) && rest.pureElement() == null,
                    "clay leaves the life as an unknown essence, no concentrate, got " + rest);
            helper.assertTrue(drawn.equals(Essence.pure(SimpleRune.EARTH, 8)), "and draws out pure earth, got " + drawn);
            helper.assertTrue(separator.getItem(SeparatorBlockEntity.REAGENT).getCount() == 2, "one clay ball binds eight drops");
        });
    }

    @HexTest(maxTicks = 300)
    public static void withoutAReagentNothingIsSplit(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.SEPARATOR.get().defaultBlockState());
        SeparatorBlockEntity separator = helper.getBlockEntity(at, SeparatorBlockEntity.class);
        separator.setItem(SeparatorBlockEntity.POUR, EssenceFlaskItem.filled(Essence.of(java.util.Map.of(SimpleRune.LIFE, 8, SimpleRune.EARTH, 8))));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(separator.mix().amount() == 16 && separator.rest().isEmpty(), "the mixture waits for a reagent");
            helper.assertTrue("no_reagent".equals(separator.blocked()), "and the screen says so");
            separator.setItem(SeparatorBlockEntity.REAGENT, new ItemStack(Items.FEATHER));
            helper.runAfterDelay(100, () -> {
                // A feather binds air; the mixture has none, so all of it passes on as the rest - bottled in the glass
                // bottle the poured one left.
                Essence rest = EssenceFlaskItem.contents(separator.getItem(SeparatorBlockEntity.REST_OUT));
                helper.assertTrue(separator.mix().isEmpty() && rest.total() == 16 && separator.extract().isEmpty(),
                        "a mixture without the reagent's element passes through, got rest " + rest);
                helper.assertTrue(separator.getItem(SeparatorBlockEntity.REAGENT).getCount() == 1, "and spends no reagent");
                helper.succeed();
            });
        });
    }

    @HexTest(maxTicks = 400)
    public static void aSeparatorChainLosesNoEssence(GameTestHelper helper) {
        // tank (mixture) -> pipe -> separator (facing north, fed at its back) -> pipe in front -> tank (rest);
        // the concentrate stays in the separator: nothing is above it.
        BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at.south(2).above(), ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(at.south(2), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(at.south(), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(at, ModBlocks.SEPARATOR.get().defaultBlockState().setValue(SeparatorBlock.FACING, Direction.NORTH));
        helper.setBlock(at.north(), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(at.north(2), ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity source = helper.getBlockEntity(at.south(2).above(), EssenceTankBlockEntity.class);
        EssenceTankBlockEntity sink = helper.getBlockEntity(at.north(2), EssenceTankBlockEntity.class);
        SeparatorBlockEntity separator = helper.getBlockEntity(at, SeparatorBlockEntity.class);
        source.tank().set(Essence.of(java.util.Map.of(SimpleRune.LIFE, 12, SimpleRune.EARTH, 12)));
        separator.setItem(SeparatorBlockEntity.REAGENT, new ItemStack(Items.CLAY_BALL, 4));
        helper.succeedWhen(() -> {
            helper.assertTrue(source.tank().isEmpty() && separator.mix().isEmpty(), "the mixture was worked off");
            helper.assertTrue(sink.tank().contents().equals(Essence.pure(SimpleRune.LIFE, 12).asUnknown()),
                    "the rest ran on into the tank in front, got " + sink.tank().contents() + ", separator rest " + separator.rest().contents());
            helper.assertTrue(separator.extract().contents().equals(Essence.pure(SimpleRune.EARTH, 12)),
                    "the concentrate waits in the separator, got " + separator.extract().contents());
        });
    }

    @HexTest(maxTicks = 400)
    public static void theRestNeverFlowsBackIntoTheSeparator(GameTestHelper helper) {
        // One pipe network touches the separator's front (rest out) and its east side (mixture in), and a tank that
        // takes from it at its side and lets out at its bottom into the same pipes: the rest must not go round in circles.
        BlockPos at = new BlockPos(2, 3, 2);
        helper.setBlock(at, ModBlocks.SEPARATOR.get().defaultBlockState().setValue(SeparatorBlock.FACING, Direction.NORTH));
        for (BlockPos pipe : new BlockPos[]{at.north(), at.north().east(), at.east(), at.north().east().below(), at.north().east(2).below()}) {
            helper.setBlock(pipe, ModBlocks.BRASS_PIPE.get());
        }
        helper.setBlock(at.north().east(2), ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity tank = helper.getBlockEntity(at.north().east(2), EssenceTankBlockEntity.class);
        SeparatorBlockEntity separator = helper.getBlockEntity(at, SeparatorBlockEntity.class);
        separator.setItem(SeparatorBlockEntity.POUR, EssenceFlaskItem.filled(Essence.of(java.util.Map.of(SimpleRune.EARTH, 8, SimpleRune.AIR, 8))));
        separator.setItem(SeparatorBlockEntity.REAGENT, new ItemStack(Items.CLAY_BALL, 2));
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(separator.mix().isEmpty(), "the mixture was worked off, left " + separator.mix().contents());
            helper.assertTrue(separator.rest().contents().equals(Essence.pure(SimpleRune.AIR, 8).asUnknown()),
                    "the air stays in the separator, got rest " + separator.rest().contents() + ", tank " + tank.tank().contents());
            helper.assertTrue(tank.tank().isEmpty(), "the tank feeding back into the same pipes takes nothing");
            helper.succeed();
        });
    }

    @HexTest
    public static void pipesJoinMachinesOnlyAtPorts(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, ModBlocks.SEPARATOR.get().defaultBlockState().setValue(SeparatorBlock.FACING, Direction.NORTH));
        helper.setBlock(at.east(), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(at.below(), ModBlocks.BRASS_PIPE.get());
        BlockState separator = helper.getBlockState(at);
        helper.assertTrue(separator.getValue(SeparatorBlock.LOOKS.get(Direction.EAST)) == PortMode.INLET, "the separator shows an inlet on its east side");
        helper.assertTrue(helper.getBlockState(at.east()).getValue(BrassPipeBlock.SIDES.get(Direction.WEST)), "and the pipe joins it");
        helper.assertFalse(helper.getBlockState(at.below()).getValue(BrassPipeBlock.SIDES.get(Direction.UP)),
                "the bottom is no port: the pipe below stays apart");
        // Facing north, the east face is the separator's left: make it a concentrate outlet.
        SeparatorBlockEntity entity = helper.getBlockEntity(at, SeparatorBlockEntity.class);
        entity.cycle(SeparatorBlock.Side.LEFT);
        helper.assertTrue(helper.getBlockState(at).getValue(SeparatorBlock.LOOKS.get(Direction.EAST)) == PortMode.PRODUCT,
                "the joint follows the face's new mode");
        helper.assertTrue(entity.receive(Direction.EAST, Essence.pure(SimpleRune.FIRE, 4), true) == 0, "an outlet takes nothing in");
        helper.setBlock(at.east(), Blocks.AIR);
        helper.assertTrue(helper.getBlockState(at).getValue(SeparatorBlock.LOOKS.get(Direction.EAST)) == PortMode.NONE,
                "the joint goes without a pipe");
        helper.succeed();
    }

    @HexTest(maxTicks = 200)
    public static void pipesCarryEssenceBetweenTanks(GameTestHelper helper) {
        BlockPos from = new BlockPos(1, 3, 1);
        BlockPos pipe = new BlockPos(1, 2, 1);
        BlockPos to = new BlockPos(2, 2, 1);
        helper.setBlock(from, ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(pipe, ModBlocks.BRASS_PIPE.get());
        helper.setBlock(to, ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity source = helper.getBlockEntity(from, EssenceTankBlockEntity.class);
        EssenceTankBlockEntity target = helper.getBlockEntity(to, EssenceTankBlockEntity.class);
        source.tank().set(Essence.pure(SimpleRune.WATER, 12));
        helper.succeedWhen(() -> {
            helper.assertTrue(target.tank().contents().equals(Essence.pure(SimpleRune.WATER, 12)),
                    "the water ran down the pipe into the other tank, got " + target.tank().contents());
            helper.assertTrue(source.tank().isEmpty(), "the first tank is empty");
        });
    }

    @HexTest
    public static void theTableDrawsInkFromTanksAround(GameTestHelper helper) {
        ResearchTableBlockEntity table = stockedTable(helper);
        int[] none = table.inkSupply().capacity();
        helper.assertTrue(Arrays.stream(none).sum() == 0, "no tanks around: no ink and no storage (0/0)");
        BlockPos mixture = new BlockPos(0, 1, 2);
        BlockPos fire = new BlockPos(1, 1, 2);
        BlockPos farAway = new BlockPos(0, 1, 6);
        for (BlockPos pos : new BlockPos[]{mixture, fire, farAway}) {
            helper.setBlock(pos, ModBlocks.ESSENCE_TANK.get());
        }
        helper.getBlockEntity(mixture, EssenceTankBlockEntity.class).tank().set(Essence.of(java.util.Map.of(SimpleRune.LIFE, 8, SimpleRune.EARTH, 8)));
        helper.getBlockEntity(fire, EssenceTankBlockEntity.class).tank().set(Essence.pure(SimpleRune.FIRE, 16));
        helper.getBlockEntity(farAway, EssenceTankBlockEntity.class).tank().set(Essence.pure(SimpleRune.WATER, 16));
        InkSupply supply = table.inkSupply();
        helper.assertTrue(supply.available()[SimpleRune.FIRE.ordinal()] == 16
                && supply.capacity()[SimpleRune.FIRE.ordinal()] == EssenceTankBlockEntity.CAPACITY, "a fire concentrate next to the desk is fire ink");
        helper.assertTrue(supply.available()[SimpleRune.LIFE.ordinal()] == 0, "a mixture is no ink");
        helper.assertTrue(supply.capacity()[SimpleRune.WATER.ordinal()] == 0, "a tank too far away does not count");
        supply.spend(ink(SimpleRune.FIRE, 5));
        helper.assertTrue(helper.getBlockEntity(fire, EssenceTankBlockEntity.class).tank().amount() == 11, "spent ink leaves the tank");
        helper.succeed();
    }

    @HexTest
    public static void aFinishedThreadTeachesTheRune(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        table.setItem(ResearchTableBlockEntity.PAPER, new ItemStack(Items.PAPER, 2));
        RuneBoard board = RuneBoards.get(SMELTING);
        int[] thread = RuneBoards.plainThread(board);
        ResearchSheet.Trace trace = ResearchSheet.trace(board, thread, ResearchSheet.Allowance.NONE);

        table.select(player, SMELTING);
        table.thread(player, thread);
        helper.assertTrue(table.sheet().length() == 0, "no thread without ink");
        Map<SimpleRune, EssenceTankBlockEntity> tanks = inkTanks(helper, 64);
        int[] part = Arrays.copyOf(thread, 3);
        table.thread(player, part);
        helper.assertTrue(table.sheet().length() == 2 && !table.sheet().isDone(), "a part of the thread is kept");
        helper.assertTrue(tanks.get(SMELTING.action()).tank().amount() == 64, "no ink is spent yet");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == 1, "the started attempt took a paper");
        table.thread(player, new int[]{HexGrid.CENTRE});
        table.thread(player, part);
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == 1, "winding back and on costs no new paper");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.OUTPUT).isEmpty(), "no scroll before the rune is finished");
        table.thread(player, thread);

        helper.assertTrue(table.sheet().isDone(), "the thread bound the rune");
        int stars = board.stars(thread.length - 1);
        RuneKnowledge knowledge = Research.of(player);
        helper.assertFalse(knowledge.knows(SMELTING), "the rune is not learned until its scroll is studied");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.HEXTECH).getCount() == 2, "the orb went into the rune");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == 1, "finishing takes no more paper"); 
        for (SimpleRune element : SimpleRune.values()) {
            helper.assertTrue(tanks.get(element).tank().amount() == 64 - trace.ink()[element.ordinal()],
                    element.id() + " ink was spent from its tank by the thread");
        }
        ItemStack scroll = table.getItem(ResearchTableBlockEntity.OUTPUT);
        helper.assertTrue(RuneScrollItem.rune(scroll) == SMELTING && RuneScrollItem.stars(scroll) == 3 && stars == 3,
                "a three-star scroll of Smelting comes out");
        player.setItemInHand(InteractionHand.MAIN_HAND, scroll.copy());
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(knowledge.stars(SMELTING) == 3, "studying the scroll teaches Smelting with its three stars");
        helper.assertTrue(knowledge.seen(SMELTING).get(thread[thread.length - 1]), "the parchment the thread passed is remembered");
        DiaryOwner owner = ResearchersDiaryItem.owner(table.getItem(ResearchTableBlockEntity.DIARY));
        helper.assertTrue(owner != null && owner.is(player), "the unsigned diary is signed by the researcher");
        player.discard();
        helper.succeed();
    }

    @HexTest
    public static void creativeResearchersHaveEverything(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setGameMode(GameType.CREATIVE);
        player.getInventory().clearContent();
        ResearchTableBlockEntity table = stockedTable(helper);
        table.select(player, SMELTING);
        table.thread(player, RuneBoards.plainThread(RuneBoards.get(SMELTING)));
        helper.assertTrue(table.sheet().isDone(), "no ink and no paper are needed in creative mode");
        player.discard();
        helper.succeed();
    }

    @HexTest
    public static void toolsFollowTheirRunesStars(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        int slot = ResearchTableBlockEntity.TOOLS + ResearchTool.STRETCHER.ordinal();
        helper.assertTrue(table.canPlaceItem(slot, new ItemStack(ModItems.PARCHMENT_STRETCHER.get())), "the stretcher has its slot");
        helper.assertFalse(table.canPlaceItem(slot, new ItemStack(ModItems.MORTAR.get())), "other tools do not fit there");
        table.setItem(slot, new ItemStack(ModItems.PARCHMENT_STRETCHER.get()));
        helper.assertTrue(table.toolLevel(player, ResearchTool.STRETCHER) == 0, "without its rune the tool does nothing");
        Research.learnAll(player, List.of(ResearchTool.STRETCHER.rune(), SMELTING), 2);
        helper.assertTrue(table.toolLevel(player, ResearchTool.STRETCHER) == 2, "two stars, strength two");
        helper.assertTrue(table.allowance(player).free() == 8, "the stretched parchment gives eight free sections");
        helper.assertTrue(table.toolLevel(player, ResearchTool.LENS) == 0, "a tool not on the table does nothing");
        player.discard();
        helper.succeed();
    }

    @HexTest
    public static void missingMaterialsAreNamed(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ResearchTableBlock.place(helper.getLevel(), helper.absolutePos(TABLE), ModBlocks.RESEARCH_TABLE.get().defaultBlockState());
        ResearchTableBlockEntity table = helper.getBlockEntity(TABLE, ResearchTableBlockEntity.class);
        helper.assertTrue(Requirement.firstMissing(table, player) == Requirement.DIARY, "an empty table asks for the diary first");

        ItemStack foreign = new ItemStack(ModItems.RESEARCHERS_DIARY.get());
        foreign.set(ModDataComponents.DIARY_OWNER.get(), new DiaryOwner(UUID.randomUUID(), "Someone"));
        table.setItem(ResearchTableBlockEntity.DIARY, foreign);
        helper.assertTrue(Requirement.firstMissing(table, player) == Requirement.OWN_DIARY, "someone else's diary does not count");
        table.select(player, SMELTING);
        helper.assertTrue(table.sheet().rune() == null, "nothing is studied with someone else's diary");

        table.setItem(ResearchTableBlockEntity.DIARY, new ItemStack(ModItems.RESEARCHERS_DIARY.get()));
        helper.assertTrue(Requirement.firstMissing(table, player) == Requirement.HEXTECH, "then the orb");
        table.setItem(ResearchTableBlockEntity.HEXTECH, new ItemStack(ModItems.UNSTABLE_HEXTECH.get()));
        helper.assertTrue(Requirement.firstMissing(table, player) == Requirement.PAPER, "then paper on the table");
        player.getInventory().add(new ItemStack(Items.PAPER));
        helper.assertTrue(Requirement.firstMissing(table, player) == Requirement.PAPER, "paper in the pocket does not count");
        table.select(player, SMELTING);
        inkTanks(helper, 64);
        table.thread(player, Arrays.copyOf(RuneBoards.plainThread(RuneBoards.get(SMELTING)), 3));
        helper.assertTrue(table.sheet().length() == 0, "no thread without paper");
        player.discard();
        helper.succeed();
    }

    @HexTest
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

    // ---- Lightning ------------------------------------------------------------------------------------------------

    @HexTest(maxTicks = 60)
    public static void lightningOnRodAndAmethystMakesOrbs(GameTestHelper helper) {
        strikeRodOnAmethyst(helper, 1);
    }

    @HexTest(maxTicks = 60)
    public static void boltInsideTheRodAlsoWorks(GameTestHelper helper) {
        // A Channeling trident or /summon at the rod's own block puts the bolt inside the rod instead of on top.
        strikeRodOnAmethyst(helper, 0);
    }

    private static void strikeRodOnAmethyst(GameTestHelper helper, int boltAboveRod) {
        BlockPos amethyst = new BlockPos(1, 1, 1);
        BlockPos rod = amethyst.above();
        helper.setBlock(amethyst, Blocks.AMETHYST_BLOCK);
        helper.setBlock(rod, Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState().setValue(LightningRodBlock.FACING, Direction.UP));
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(bolt != null, "a bolt can be created");
        bolt.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(rod.above(boltAboveRod))));
        helper.getLevel().addFreshEntity(bolt);
        // The test server may not tick entities in a freshly generated test chunk yet: strike by hand.
        bolt.tick();
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(Blocks.AIR, amethyst);
            helper.assertBlockPresent(Blocks.LIGHTNING_ROD.weathering().unaffected(), rod);
            List<ItemEntity> found = orbs(helper, amethyst);
            int orbs = found.stream().mapToInt(item -> item.getItem().getCount()).sum();
            helper.assertTrue(orbs >= 1 && orbs <= 3, "the amethyst shattered into 1-3 orbs, got " + orbs);
            found.forEach(ItemEntity::discard);
        });
    }

    @HexTest(maxTicks = 60)
    public static void rodWithoutAmethystMakesNothing(GameTestHelper helper) {
        BlockPos base = new BlockPos(1, 1, 1);
        BlockPos rod = base.above();
        helper.setBlock(base, Blocks.STONE);
        helper.setBlock(rod, Blocks.LIGHTNING_ROD.weathering().unaffected().defaultBlockState().setValue(LightningRodBlock.FACING, Direction.UP));
        orbs(helper, base).forEach(ItemEntity::discard);
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        helper.assertTrue(bolt != null, "a bolt can be created");
        bolt.snapTo(Vec3.atBottomCenterOf(helper.absolutePos(rod.above())));
        helper.getLevel().addFreshEntity(bolt);
        bolt.tick();
        helper.runAfterDelay(10, () -> {
            helper.assertBlockPresent(Blocks.STONE, base);
            helper.assertTrue(orbs(helper, base).isEmpty(), "no orbs without amethyst");
            helper.succeed();
        });
    }

    // ---- Knowledge ------------------------------------------------------------------------------------------------

    @HexTest
    public static void knowledgeSurvivesDeath(GameTestHelper helper) {
        ServerPlayer before = player(helper);
        Research.learn(before, ComplexRunes.byKey("water_life"), 3, 9);
        Research.learn(before, ComplexRunes.byKey("life_metal"), 1, 20);
        Research.of(before).study(Identifier.withDefaultNamespace("coal"));
        Research.of(before).see(SMELTING, new int[]{HexGrid.CENTRE, 5});

        ServerPlayer after = player(helper);
        after.restoreFrom(before, false);

        RuneKnowledge knowledge = Research.of(after);
        helper.assertTrue(knowledge.runeCount() == 2 && knowledge.stars(ComplexRunes.byKey("water_life")) == 3, "the runes and stars are kept");
        helper.assertTrue(knowledge.best(ComplexRunes.byKey("water_life")) == 9, "the best sheets are kept");
        helper.assertTrue(knowledge.hasStudied(Identifier.withDefaultNamespace("coal")), "studied items are kept");
        helper.assertTrue(knowledge.seen(SMELTING).get(5), "explored parchment is kept");
        helper.assertTrue(knowledge != Research.of(before), "the new player has its own copy");
        RuneKnowledge decoded = RuneKnowledge.CODEC.parse(NbtOps.INSTANCE,
                RuneKnowledge.CODEC.encodeStart(NbtOps.INSTANCE, knowledge).getOrThrow()).getOrThrow();
        helper.assertTrue(decoded.stars(ComplexRunes.byKey("life_metal")) == 1 && decoded.seen(SMELTING).get(5), "knowledge saves and loads");
        before.discard();
        after.discard();
        helper.succeed();
    }

    @HexTest
    public static void starsOnlyGetBetter(GameTestHelper helper) {
        RuneKnowledge knowledge = new RuneKnowledge();
        helper.assertTrue(knowledge.learn(SMELTING, 2, 9), "the first sheet teaches the rune");
        helper.assertFalse(knowledge.learn(SMELTING, 1, 12), "a later sheet is not a new rune");
        helper.assertTrue(knowledge.stars(SMELTING) == 2 && knowledge.best(SMELTING) == 9, "a worse sheet changes nothing");
        knowledge.learn(SMELTING, 3, 7);
        helper.assertTrue(knowledge.stars(SMELTING) == 3 && knowledge.best(SMELTING) == 7, "a better sheet raises the stars");
        helper.succeed();
    }

    @HexTest
    public static void learnAllCountsRunes(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Research.learnAll(player, ComplexRunes.all().subList(0, 12), 1);
        RuneKnowledge knowledge = Research.of(player);
        helper.assertTrue(knowledge.runeCount() == 12, "twelve runes learned at once");
        player.discard();
        helper.succeed();
    }

    @HexTest
    public static void scrollsTeachOtherResearchers(GameTestHelper helper) {
        ServerPlayer reader = survivalPlayer(helper);
        ComplexRune lens = ComplexRunes.byKey("water_light");
        ItemStack scroll = RuneScrollItem.of(lens, 2, ModItems.RUNE_SCROLL.get());
        scroll.setCount(3);
        reader.setItemInHand(InteractionHand.MAIN_HAND, scroll);

        scroll.use(helper.getLevel(), reader, InteractionHand.MAIN_HAND);
        helper.assertFalse(Research.of(reader).knows(lens), "a tool rune is too early before Smelting");
        helper.assertTrue(reader.getMainHandItem().getCount() == 3, "the early scroll is kept");
        Research.learnAll(reader, List.of(SMELTING), 1);
        scroll.use(helper.getLevel(), reader, InteractionHand.MAIN_HAND);
        helper.assertTrue(Research.of(reader).stars(lens) == 2, "studying the scroll teaches the rune with its stars");
        helper.assertTrue(reader.getMainHandItem().getCount() == 2, "one scroll is used up");
        scroll.use(helper.getLevel(), reader, InteractionHand.MAIN_HAND);
        helper.assertTrue(reader.getMainHandItem().getCount() == 2, "a scroll that teaches nothing new is kept");
        reader.discard();
        helper.succeed();
    }

    // ---- Helpers --------------------------------------------------------------------------------------------------

    /** The shortest open way from the orb to a section, around dark runes; the orb itself when unreachable. */
    private static int[] pathTo(RuneBoard board, int target) {
        int[] previous = new int[HexGrid.COUNT];
        Arrays.fill(previous, -2);
        previous[HexGrid.CENTRE] = -1;
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>(List.of(HexGrid.CENTRE));
        while (!queue.isEmpty()) {
            int cell = queue.poll();
            for (int next : HexGrid.neighbours(cell)) {
                if (previous[next] == -2 && !board.wall(next)) {
                    previous[next] = cell;
                    queue.add(next);
                }
            }
        }
        if (previous[target] == -2) {
            return new int[]{HexGrid.CENTRE};
        }
        List<Integer> path = new ArrayList<>();
        for (int cell = target; cell != -1; cell = previous[cell]) {
            path.add(0, cell);
        }
        return path.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Orbs lying around a position; tests clean them up, since the test area is reused by later batches. */
    private static List<ItemEntity> orbs(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(pos)).inflate(8),
                item -> item.getItem().is(ModItems.UNSTABLE_HEXTECH.get()));
    }

    private static ResearchTableBlockEntity stockedTable(GameTestHelper helper) {
        ResearchTableBlock.place(helper.getLevel(), helper.absolutePos(TABLE), ModBlocks.RESEARCH_TABLE.get().defaultBlockState());
        ResearchTableBlockEntity table = helper.getBlockEntity(TABLE, ResearchTableBlockEntity.class);
        table.setItem(ResearchTableBlockEntity.DIARY, new ItemStack(ModItems.RESEARCHERS_DIARY.get()));
        table.setItem(ResearchTableBlockEntity.HEXTECH, new ItemStack(ModItems.UNSTABLE_HEXTECH.get(), 3));
        return table;
    }

    /** An Essence Tank of each element's concentrate next to the test table, {@code drops} in each. */
    private static Map<SimpleRune, EssenceTankBlockEntity> inkTanks(GameTestHelper helper, int drops) {
        Map<SimpleRune, EssenceTankBlockEntity> tanks = new java.util.EnumMap<>(SimpleRune.class);
        SimpleRune[] elements = SimpleRune.values();
        for (int i = 0; i < elements.length; i++) {
            BlockPos pos = new BlockPos(-2 + i % 5, TABLE.getY() + i / 5, TABLE.getZ() + 2);
            helper.setBlock(pos, ModBlocks.ESSENCE_TANK.get());
            EssenceTankBlockEntity tank = helper.getBlockEntity(pos, EssenceTankBlockEntity.class);
            tank.tank().set(Essence.pure(elements[i], drops));
            tanks.put(elements[i], tank);
        }
        return tanks;
    }

    private static int[] ink(SimpleRune element, int drops) {
        int[] ink = new int[SimpleRune.values().length];
        ink[element.ordinal()] = drops;
        return ink;
    }

    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        return player;
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = helper.absoluteVec(new Vec3(0.5, 1, 2.5));
        player.snapTo(pos.x, pos.y, pos.z, 0, 0);
        return player;
    }
}
