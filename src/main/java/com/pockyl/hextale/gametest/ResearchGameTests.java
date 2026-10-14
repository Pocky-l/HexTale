package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The research loop on a server: boards, sheets at the table, lightning making orbs, knowledge kept on the player.
 * Every test runs in its own batch: lightning and mock players must not reach the neighbouring tests.
 */
@GameTestHolder(Hextale.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ResearchGameTests {
    private static final BlockPos TABLE = new BlockPos(0, 1, 0);
    private static final ComplexRune SMELTING = ComplexRunes.byKey("fire_earth");

    private ResearchGameTests() {
    }

    // ---- Boards ---------------------------------------------------------------------------------------------------

    /** Reference values printed by the approved prototype (walls: count and hash of the sorted "q,r:element" list). */
    @GameTest(template = "empty", batch = "boardsMatchThePrototype")
    public static void boardsMatchThePrototype(GameTestHelper helper) {
        expect(helper, "fire_earth", 21, 3624390768L, "fire@-6,2 earth@-3,6", 4, 6, 8, 4);
        expect(helper, "water_light", 21, 1644978348L, "water@-5,5 light@-1,4", 4, 6, 8, 4);
        expect(helper, "air_water", 42, 903288142L, "air@-2,7 water@-3,-1 light@2,5 metal@-3,4", 7, 10, 13, 9);
        expect(helper, "shadow_fire", 59, 1897287124L, "shadow@6,-5 fire@-8,5 metal@5,-8 metal@7,0 shadow@4,-2 water@-4,2", 10, 14, 18, 13);
        expect(helper, "spark_metal", 50, 4208863995L, "spark@2,6 metal@-3,0 fire@-2,5 light@-5,-1 life@4,-2 spark@-6,8", 11, 13, 17, 12);
        expect(helper, "life_shadow", 56, 4014118940L, "life@-6,-2 shadow@3,2 metal@-4,0 air@-2,-5 metal@-6,3 water@5,-1", 9, 11, 15, 10);
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, String key, int walls, long wallHash, String points, int three, int two,
                               int sheet, int plain) {
        RuneBoard board = RuneBoards.get(ComplexRunes.byKey(key));
        List<String> list = new ArrayList<>();
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            if (board.walls()[cell] != null) {
                list.add(HexGrid.q(cell) + "," + HexGrid.r(cell) + ":" + board.walls()[cell].id());
            }
        }
        Collections.sort(list);
        helper.assertTrue(list.size() == walls, key + ": " + list.size() + " dark runes, the prototype has " + walls);
        helper.assertTrue(hash(String.join(";", list)) == wallHash, key + ": the dark runes lie where the prototype has them");
        List<String> found = board.points().stream().map(p -> p.element().id() + "@" + HexGrid.q(p.cell()) + "," + HexGrid.r(p.cell())).toList();
        helper.assertTrue(String.join(" ", found).equals(points), key + ": points " + found + ", the prototype has " + points);
        // The table's rules are stricter than the prototype's (no two equal symbols in a row): thresholds never drop.
        RuneBoard.Par par = board.par();
        helper.assertTrue(par.three() >= three && par.plain() >= plain && par.two() > par.three() && par.sheet() > par.two(),
                key + ": thresholds " + par + ", the prototype has " + three + "/" + two + "/" + sheet + "/" + plain);
    }

    private static long hash(String text) {
        int h = (int) 2166136261L;
        for (int i = 0; i < text.length(); i++) {
            h = (h ^ text.charAt(i)) * 16777619;
        }
        return h & 0xFFFFFFFFL;
    }

    @GameTest(template = "empty", batch = "everyBoardCanBeFinished", timeoutTicks = 400)
    public static void everyBoardCanBeFinished(GameTestHelper helper) {
        long start = System.nanoTime();
        for (ComplexRune rune : ComplexRunes.all()) {
            RuneBoard board = RuneBoards.get(rune);
            int tier = board.tier();
            RuneBoard.Par par = board.par();
            helper.assertTrue(board.points().size() == tier * 2, rune.key() + " has " + tier * 2 + " points");
            helper.assertTrue(board.points().get(0).element() == rune.action() && board.points().get(1).element() == rune.target(),
                    rune.key() + ": the first points are its action and target");
            helper.assertTrue(par.three() >= 1 && par.three() <= par.plain() && par.plain() < par.two() + 1 && par.two() < par.sheet(),
                    rune.key() + ": sane thresholds " + par);
            helper.assertTrue(route(board) != null, rune.key() + ": a plain route exists");
            helper.assertTrue(board.walls()[HexGrid.CENTRE] == null, rune.key() + ": the orb is never walled in");
        }
        Hextale.LOGGER.info("Checked {} research boards in {} ms", ComplexRunes.COUNT, (System.nanoTime() - start) / 1_000_000);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "filledPointsDissolveTheirDarkRunes")
    public static void filledPointsDissolveTheirDarkRunes(GameTestHelper helper) {
        RuneBoard board = RuneBoards.get(ComplexRunes.byKey("air_water"));
        SimpleRune element = board.points().get(0).element();
        boolean[] before = board.activeWalls(0);
        boolean[] after = board.activeWalls(1);
        int gone = 0;
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            if (board.walls()[cell] == element) {
                helper.assertTrue(before[cell] && !after[cell], "a dark rune of the filled point's element dissolves");
                gone++;
            } else if (board.walls()[cell] != null) {
                helper.assertTrue(before[cell] == after[cell], "other dark runes stay");
            }
        }
        helper.assertTrue(gone > 0, "the board has dark runes of " + element.id());
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "chaptersOpenInOrder")
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

    @GameTest(template = "empty", batch = "everyItemCarriesElements")
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
                Items.GLASS, Items.BREAD, Items.TNT, Items.PISTON, Items.ENDER_EYE, Items.WHITE_WOOL, Items.DIAMOND_SWORD,
                Items.COOKED_BEEF, Items.NETHERITE_SWORD, Items.SPONGE, Items.BOOKSHELF, Items.LANTERN, Items.SHULKER_BOX,
                Items.ELYTRA, Items.DRAGON_EGG, Items.MUSIC_DISC_CAT, Items.ZOMBIE_SPAWN_EGG, Items.DIRT, Items.STONE_BRICKS)) {
            sample.append(BuiltInRegistries.ITEM.getKey(item).getPath()).append('=').append(all.get(item)).append("; ");
        }
        Hextale.LOGGER.info("Item elements sample: {}", sample);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "resonanceStringsAreDistinct")
    public static void resonanceStringsAreDistinct(GameTestHelper helper) {
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

    @GameTest(template = "empty", batch = "resonanceAnswersEachGuess")
    public static void resonanceAnswersEachGuess(GameTestHelper helper) {
        ItemElements elements = ItemElements.of(1, SimpleRune.FIRE, 3, SimpleRune.WATER, 1);
        ItemStudy study = new ItemStudy();
        study.begin(Items.STICK, elements);
        int fire = ring(SimpleRune.FIRE);
        int spark = ring(SimpleRune.SPARK);
        int metal = ring(SimpleRune.METAL);
        helper.assertTrue(study.maxGuesses() == 7, "three guesses plus two per element");
        ItemStudy.Answer first = study.guess(ItemStudy.string(fire)[0], elements);
        helper.assertTrue(first != null && first.echo() == ItemStudy.Echo.FARTHER, "fire 3 is farther than the first section");
        helper.assertTrue(study.presence(spark) == ItemStudy.Presence.ABSENT && study.presence(metal) == ItemStudy.Presence.ABSENT,
                "the neighbouring strings answered: no lightning, no metal");
        helper.assertFalse(study.possible(fire, 0), "the guessed section is ruled out");
        helper.assertFalse(study.canGuess(ItemStudy.string(spark)[2], elements), "a dark string takes no guesses");
        ItemStudy.Answer second = study.guess(ItemStudy.string(fire)[3], elements);
        helper.assertTrue(second != null && second.echo() == ItemStudy.Echo.NEARER, "fire 3 is nearer than the fourth section");
        ItemStudy.Answer third = study.guess(ItemStudy.string(fire)[1], elements);
        helper.assertTrue(third != null && third.echo() == ItemStudy.Echo.FARTHER && third.selfRevealed().contains(fire),
                "one section left: fire reveals itself for free");
        helper.assertTrue(study.isRevealed(SimpleRune.FIRE) && study.guesses() == 3, "three guesses so far");
        study.guess(ItemStudy.string(ring(SimpleRune.WATER))[0], elements);
        helper.assertTrue(study.won(elements) && !study.isActive(elements), "both elements found: the item is studied");
        helper.succeed();
    }

    /** A sensible player - spread guesses around the ring, then halve the range - studies almost every item in time. */
    @GameTest(template = "empty", batch = "resonanceIsFairForEveryItem")
    public static void resonanceIsFairForEveryItem(GameTestHelper helper) {
        int items = 0;
        int won = 0;
        int used = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            ItemElements elements = ResearchItems.of(item);
            if (elements == null) {
                continue;
            }
            ItemStudy study = new ItemStudy();
            study.begin(item, elements);
            while (study.isActive(elements)) {
                int cell = sensibleGuess(study);
                helper.assertTrue(cell >= 0 && study.guess(cell, elements) != null, item + ": there is always a guess to make");
            }
            items++;
            if (study.won(elements)) {
                won++;
                used += study.guesses();
            }
        }
        Hextale.LOGGER.info("Resonance: a sensible player studies {}/{} items, {} guesses on average", won, items,
                won == 0 ? "-" : String.format("%.1f", used / (double) won));
        helper.assertTrue(won >= items * 0.9, "a sensible player studies at least 90% of the items, got " + won + "/" + items);
        helper.succeed();
    }

    private static int sensibleGuess(ItemStudy study) {
        // A shimmering string first: halve what is left of it.
        for (int string = 0; string < HexGrid.RING.length; string++) {
            if (study.presence(string) == ItemStudy.Presence.PRESENT && !study.isRevealed(string)) {
                return middle(study, string);
            }
        }
        // Otherwise the unknown string whose neighbours are unknown too: one guess asks three strings.
        int best = -1;
        int bestScore = -1;
        for (int string = 0; string < HexGrid.RING.length; string++) {
            if (study.presence(string) != ItemStudy.Presence.UNKNOWN) {
                continue;
            }
            int score = 0;
            for (int side : new int[]{-1, 1}) {
                if (study.presence(Math.floorMod(string + side, HexGrid.RING.length)) == ItemStudy.Presence.UNKNOWN) {
                    score++;
                }
            }
            if (score > bestScore) {
                best = string;
                bestScore = score;
            }
        }
        return best < 0 ? -1 : middle(study, best);
    }

    private static int middle(ItemStudy study, int string) {
        int[] cells = ItemStudy.string(string);
        List<Integer> open = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) {
            if (study.possible(string, i)) {
                open.add(cells[i]);
            }
        }
        return open.isEmpty() ? -1 : open.get((open.size() - 1) / 2);
    }

    @GameTest(template = "empty", batch = "studyingAnItemAtTheTable")
    public static void studyingAnItemAtTheTable(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        player.getInventory().add(new ItemStack(Items.PAPER, 2));
        ResearchTableBlockEntity table = stockedTable(helper);
        table.setItem(ResearchTableBlockEntity.HEXTECH, ItemStack.EMPTY);
        table.setItem(ResearchTableBlockEntity.STUDY, new ItemStack(Items.GLASS, 3));
        table.selectItem(player);
        helper.assertTrue(table.isStudyingItem() && table.study().item() == Items.GLASS, "glass lies on the parchment, no orb needed");
        ItemElements elements = ResearchItems.of(Items.GLASS);
        for (Map.Entry<SimpleRune, Integer> entry : elements.elements().entrySet()) {
            if (!table.study().isRevealed(entry.getKey())) {
                table.guess(player, ItemStudy.string(ring(entry.getKey()))[ItemStudy.answer(entry.getKey(), entry.getValue())]);
            }
        }
        ResourceLocation glass = BuiltInRegistries.ITEM.getKey(Items.GLASS);
        helper.assertTrue(Research.of(player).hasStudied(glass), "glass is studied");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.STUDY).getCount() == 2, "one glass went into the study");
        helper.assertTrue(player.getInventory().countItem(Items.PAPER) == 1, "the study took one paper");
        table.selectItem(player);
        helper.assertTrue(table.study().won(elements), "a studied item is not studied again");

        // A lost study: guesses everywhere but on the answers, until they run out; the item is used once.
        table.setItem(ResearchTableBlockEntity.STUDY, new ItemStack(Items.FEATHER, 2));
        table.selectItem(player);
        ItemElements feather = ResearchItems.of(Items.FEATHER);
        for (int string = 0; string < HexGrid.RING.length; string++) {
            SimpleRune element = HexGrid.RING[string];
            Integer strength = feather.elements().get(element);
            for (int cell : ItemStudy.string(string)) {
                boolean answer = strength != null && ItemStudy.indexOf(cell) == ItemStudy.answer(element, strength);
                if (!answer && table.study().isActive(feather)) {
                    table.guess(player, cell);
                }
            }
        }
        helper.assertTrue(table.study().guesses() > 0, "guesses were made");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.STUDY).getCount() == 1, "the study used one feather");
        if (table.study().lost(feather)) {
            helper.assertFalse(Research.of(player).hasStudied(BuiltInRegistries.ITEM.getKey(Items.FEATHER)), "a lost study teaches nothing");
        }
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

    @GameTest(template = "empty", batch = "aFinishedSheetTeachesTheRune")
    public static void aFinishedSheetTeachesTheRune(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        RuneBoard board = RuneBoards.get(SMELTING);
        List<Step> route = route(board);
        helper.assertTrue(route != null, "Smelting can be finished");
        Map<Item, Integer> needed = new HashMap<>();
        route.forEach(step -> needed.merge(step.item(), 1, Integer::sum));
        needed.forEach((item, count) -> player.getInventory().add(new ItemStack(item, count)));
        player.getInventory().add(new ItemStack(Items.PAPER, 2));

        table.select(player, SMELTING);
        helper.assertTrue(table.sheet().rune() == SMELTING && !table.sheet().isOpen(), "Smelting lies on the table, no sheet yet");
        Step first = route.get(0);
        table.write(player, first.item(), false, landing(table, board, first.item()));
        helper.assertFalse(table.sheet().isOpen(), "unstudied items cannot write");
        needed.keySet().forEach(item -> Research.study(player, item));

        play(table, player, board, route);
        helper.assertTrue(player.getInventory().countItem(Items.PAPER) == 1, "the sheet took one paper");

        ResearchSheet sheet = table.sheet();
        helper.assertTrue(sheet.isOver() && sheet.complete(board), "every point is filled");
        int stars = board.stars(sheet.used());
        RuneKnowledge knowledge = Research.of(player);
        helper.assertTrue(knowledge.stars(SMELTING) == stars && stars >= 1, "Smelting is learned with " + stars + " stars");
        helper.assertTrue(knowledge.best(SMELTING) == sheet.used(), "the best sheet is remembered");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.HEXTECH).getCount() == 2, "the orb went into the rune");
        ItemStack scroll = table.getItem(ResearchTableBlockEntity.OUTPUT);
        helper.assertTrue(RuneScrollItem.rune(scroll) == SMELTING && RuneScrollItem.stars(scroll) == stars, "a scroll of Smelting comes out");
        helper.assertFalse(player.getInventory().hasAnyMatching(stack -> needed.containsKey(stack.getItem())), "every lever was used up");
        DiaryOwner owner = ResearchersDiaryItem.owner(table.getItem(ResearchTableBlockEntity.DIARY));
        helper.assertTrue(owner != null && owner.is(player), "the unsigned diary is signed by the researcher");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "aFullSheetWithoutTheRuneIsLost")
    public static void aFullSheetWithoutTheRuneIsLost(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        Research.study(player, Items.COAL);
        player.getInventory().add(new ItemStack(Items.COAL, 64));
        table.select(player, SMELTING);
        RuneBoard board = RuneBoards.get(SMELTING);
        // Write anything allowed that keeps away from both points until the sheet is full.
        for (int i = 0; i < 40 && table.sheet().canWrite() || i == 0; i++) {
            Item next = null;
            for (Map.Entry<Item, ItemElements> entry : ResearchItems.canonical().entrySet()) {
                ResearchSheet current = table.sheet().isOpen() ? table.sheet() : null;
                int target = landing(table, board, entry.getKey());
                boolean allowed = current == null ? target != HexGrid.CENTRE
                        : current.refusal(entry.getValue(), current.preview(board, entry.getValue(), false)) == null;
                if (allowed && board.pointAt(target) < 0) {
                    next = entry.getKey();
                    break;
                }
            }
            helper.assertTrue(next != null, "there is always a harmless symbol to write");
            table.write(player, next, false, landing(table, board, next));
        }
        ResearchSheet sheet = table.sheet();
        helper.assertTrue(sheet.used() == sheet.cap() && sheet.failed(board), "the full sheet is lost");
        table.write(player, Items.FEATHER, false, landing(table, board, Items.FEATHER));
        helper.assertTrue(sheet.used() == sheet.cap(), "nothing more can be written on it");
        helper.assertFalse(Research.of(player).knows(SMELTING), "nothing is learned");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.HEXTECH).getCount() == 3, "the orb is kept");
        table.newSheet(player);
        helper.assertTrue(table.sheet().isActive() && table.sheet().used() == 0, "a new sheet is free");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "creativeResearchersHaveEverything")
    public static void creativeResearchersHaveEverything(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setGameMode(GameType.CREATIVE);
        player.getInventory().clearContent();
        ResearchTableBlockEntity table = stockedTable(helper);
        table.select(player, SMELTING);
        RuneBoard board = RuneBoards.get(SMELTING);
        table.write(player, Items.BLAZE_ROD, false, landing(table, board, Items.BLAZE_ROD));
        helper.assertTrue(table.sheet().used() == 1, "an unstudied item not in the inventory writes in creative mode");
        helper.assertTrue(player.getInventory().isEmpty(), "nothing was needed, nothing was taken");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "symbolsFollowTheTableRules")
    public static void symbolsFollowTheTableRules(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        table.select(player, SMELTING);
        RuneBoard board = RuneBoards.get(SMELTING);
        table.write(player, Items.COAL, false, landing(table, board, Items.COAL));
        helper.assertTrue(table.sheet().used() == 1, "coal wrote a fire symbol");
        table.write(player, Items.COAL, false, landing(table, board, Items.COAL));
        table.write(player, Items.BLAZE_POWDER, false, landing(table, board, Items.BLAZE_POWDER));
        helper.assertTrue(table.sheet().used() == 1, "no second fire symbol right after a fire symbol, whatever the item");
        ResearchSheet sheet = table.sheet();
        ItemElements feather = ResearchItems.of(Items.FEATHER);
        helper.assertTrue(sheet.refusal(feather, new HexGrid.Walk(sheet.from(), new int[0], HexGrid.Walk.Stop.WALL))
                == ResearchSheet.Refusal.OCCUPIED, "a symbol never lands where it starts");
        helper.assertTrue(sheet.refusal(feather, new HexGrid.Walk(HexGrid.CENTRE, new int[0], HexGrid.Walk.Stop.NONE))
                == ResearchSheet.Refusal.OCCUPIED, "nor on the orb");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "toolsFollowTheirRunesStars")
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
        table.select(player, SMELTING);
        table.newSheet(player);
        helper.assertTrue(table.sheet().cap() == RuneBoards.get(SMELTING).par().sheet() + 2, "the stretched sheet holds two more symbols");
        helper.assertTrue(table.toolLevel(player, ResearchTool.LENS) == 0, "a tool not on the table does nothing");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "theScraperGivesThePlaceBack")
    public static void theScraperGivesThePlaceBack(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        table.setItem(ResearchTableBlockEntity.TOOLS + ResearchTool.SCRAPER.ordinal(), new ItemStack(ModItems.BRASS_SCRAPER.get()));
        Research.learnAll(player, List.of(ResearchTool.SCRAPER.rune()), 1);
        Research.study(player, Items.COAL);
        player.getInventory().add(new ItemStack(Items.COAL, 64));
        table.select(player, SMELTING);
        RuneBoard board = RuneBoards.get(SMELTING);
        table.write(player, Items.COAL, false, landing(table, board, Items.COAL));
        helper.assertTrue(table.sheet().used() == 1 && table.sheet().from() != HexGrid.CENTRE, "the first click wrote a symbol");
        table.scrape(player);
        helper.assertTrue(table.sheet().used() == 0 && table.sheet().from() == HexGrid.CENTRE, "the symbol is scraped off");
        table.write(player, Items.COAL, false, landing(table, board, Items.COAL));
        table.scrape(player);
        helper.assertTrue(table.sheet().used() == 1, "one star scrapes once per sheet");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "missingMaterialsAreNamed")
    public static void missingMaterialsAreNamed(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        ResearchTableBlock.place(helper.getLevel(), helper.absolutePos(TABLE), ModBlocks.RESEARCH_TABLE.get().defaultBlockState());
        ResearchTableBlockEntity table = helper.getBlockEntity(TABLE);
        helper.assertTrue(Requirement.firstMissing(table, player) == Requirement.DIARY, "an empty table asks for the diary first");

        ItemStack foreign = new ItemStack(ModItems.RESEARCHERS_DIARY.get());
        foreign.set(ModDataComponents.DIARY_OWNER.get(), new DiaryOwner(UUID.randomUUID(), "Someone"));
        table.setItem(ResearchTableBlockEntity.DIARY, foreign);
        helper.assertTrue(Requirement.firstMissing(table, player) == Requirement.OWN_DIARY, "someone else's diary does not count");
        table.select(player, SMELTING);
        helper.assertTrue(table.sheet().rune() == null, "nothing is studied with someone else's diary");

        table.setItem(ResearchTableBlockEntity.DIARY, new ItemStack(ModItems.RESEARCHERS_DIARY.get()));
        helper.assertTrue(Requirement.firstMissing(table, player) == Requirement.HEXTECH, "then the orb");
        table.select(player, SMELTING);
        table.newSheet(player);
        helper.assertFalse(table.sheet().isOpen(), "no sheet without the orb");
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

    // ---- Lightning ------------------------------------------------------------------------------------------------

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
        // The test server may not tick entities in a freshly generated test chunk yet: strike by hand.
        bolt.tick();
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
        bolt.tick();
        helper.runAfterDelay(10, () -> {
            helper.assertBlockPresent(Blocks.STONE, base);
            helper.assertTrue(orbs(helper, base).isEmpty(), "no orbs without amethyst");
            helper.succeed();
        });
    }

    // ---- Knowledge ------------------------------------------------------------------------------------------------

    @GameTest(template = "empty", batch = "knowledgeSurvivesDeath")
    public static void knowledgeSurvivesDeath(GameTestHelper helper) {
        ServerPlayer before = player(helper);
        Research.learn(before, ComplexRunes.byKey("water_life"), 3, 9);
        Research.learn(before, ComplexRunes.byKey("life_metal"), 1, 20);
        Research.of(before).study(ResourceLocation.withDefaultNamespace("coal"));
        Research.of(before).see(SMELTING, new int[]{HexGrid.CENTRE, 5});

        ServerPlayer after = player(helper);
        after.restoreFrom(before, false);

        RuneKnowledge knowledge = Research.of(after);
        helper.assertTrue(knowledge.runeCount() == 2 && knowledge.stars(ComplexRunes.byKey("water_life")) == 3, "the runes and stars are kept");
        helper.assertTrue(knowledge.best(ComplexRunes.byKey("water_life")) == 9, "the best sheets are kept");
        helper.assertTrue(knowledge.hasStudied(ResourceLocation.withDefaultNamespace("coal")), "studied items are kept");
        helper.assertTrue(knowledge.seen(SMELTING).get(5), "explored parchment is kept");
        helper.assertTrue(knowledge != Research.of(before), "the new player has its own copy");
        RuneKnowledge decoded = RuneKnowledge.CODEC.parse(NbtOps.INSTANCE,
                RuneKnowledge.CODEC.encodeStart(NbtOps.INSTANCE, knowledge).getOrThrow()).getOrThrow();
        helper.assertTrue(decoded.stars(ComplexRunes.byKey("life_metal")) == 1 && decoded.seen(SMELTING).get(5), "knowledge saves and loads");
        before.discard();
        after.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "starsOnlyGetBetter")
    public static void starsOnlyGetBetter(GameTestHelper helper) {
        RuneKnowledge knowledge = new RuneKnowledge();
        helper.assertTrue(knowledge.learn(SMELTING, 2, 9), "the first sheet teaches the rune");
        helper.assertFalse(knowledge.learn(SMELTING, 1, 12), "a later sheet is not a new rune");
        helper.assertTrue(knowledge.stars(SMELTING) == 2 && knowledge.best(SMELTING) == 9, "a worse sheet changes nothing");
        knowledge.learn(SMELTING, 3, 7);
        helper.assertTrue(knowledge.stars(SMELTING) == 3 && knowledge.best(SMELTING) == 7, "a better sheet raises the stars");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "learnAllCountsRunes")
    public static void learnAllCountsRunes(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        Research.learnAll(player, ComplexRunes.all().subList(0, 12), 1);
        RuneKnowledge knowledge = Research.of(player);
        helper.assertTrue(knowledge.runeCount() == 12, "twelve runes learned at once");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "scrollsTeachOtherResearchers")
    public static void scrollsTeachOtherResearchers(GameTestHelper helper) {
        ServerPlayer reader = survivalPlayer(helper);
        ComplexRune lens = ComplexRunes.byKey("water_light");
        ItemStack scroll = RuneScrollItem.of(lens, 3, ModItems.RUNE_SCROLL.get());
        scroll.setCount(2);
        reader.setItemInHand(InteractionHand.MAIN_HAND, scroll);

        scroll.use(helper.getLevel(), reader, InteractionHand.MAIN_HAND);
        helper.assertTrue(Research.of(reader).stars(lens) == 1, "studying a scroll teaches the rune with one star");
        helper.assertTrue(reader.getMainHandItem().getCount() == 1, "one scroll is used up");
        scroll.use(helper.getLevel(), reader, InteractionHand.MAIN_HAND);
        helper.assertTrue(reader.getMainHandItem().getCount() == 1, "a known rune's scroll is kept");
        reader.discard();
        helper.succeed();
    }

    // ---- Helpers --------------------------------------------------------------------------------------------------

    /** One move of a route: write with {@code item}, or leave the resource of {@code point}. */
    private record Step(Item item, int point) {
    }

    /** A shortest plain route (no tools) that fills every point, found by breadth-first search; null if none. */
    /**
     * A shortest plain route (no tools) that fills every point under the table's rules - no two symbols of one element
     * in a row, never onto the orb or the spot it starts from - found by breadth-first search; null if none.
     */
    private static List<Step> route(RuneBoard board) {
        int n = board.points().size();
        int lasts = SimpleRune.values().length + 1;
        int per = (int) Math.pow(3, n);
        int states = HexGrid.COUNT * per * lasts;
        int[] pow = new int[n];
        for (int i = 0; i < n; i++) {
            pow[i] = (int) Math.pow(3, i);
        }
        int[] parent = new int[states];
        Step[] how = new Step[states];
        Arrays.fill(parent, -2);
        int start = (HexGrid.CENTRE * per) * lasts + lasts - 1;
        parent[start] = -1;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(start);
        int goal = -1;
        while (!queue.isEmpty()) {
            int state = queue.poll();
            int last = state % lasts;
            int cell = state / lasts / per;
            int p = state / lasts % per;
            if (p == per - 1) {
                goal = state;
                break;
            }
            int filled = 0;
            for (int i = 0; i < n; i++) {
                int digit = p / pow[i] % 3;
                if (digit == 2) {
                    filled |= 1 << i;
                }
                if (digit == 1) {
                    visit(queue, parent, how, state, ((cell * per) + p + pow[i]) * lasts + last, new Step(board.points().get(i).resource(), i));
                }
            }
            boolean[] walls = board.activeWalls(filled);
            for (Map.Entry<Item, ItemElements> entry : ResearchItems.canonical().entrySet()) {
                int element = entry.getValue().main().ordinal();
                int end = HexGrid.walk(walls, cell, entry.getValue().offset(false)).end();
                if (element == last || end == cell || end == HexGrid.CENTRE) {
                    continue;
                }
                int point = board.pointAt(end);
                int np = point >= 0 && p / pow[point] % 3 == 0 ? p + pow[point] : p;
                visit(queue, parent, how, state, ((end * per) + np) * lasts + element, new Step(entry.getKey(), -1));
            }
        }
        if (goal < 0) {
            return null;
        }
        List<Step> steps = new ArrayList<>();
        for (int s = goal; parent[s] >= 0; s = parent[s]) {
            steps.add(how[s]);
        }
        Collections.reverse(steps);
        return steps;
    }

    private static void visit(ArrayDeque<Integer> queue, int[] parent, Step[] how, int from, int to, Step step) {
        if (parent[to] == -2) {
            parent[to] = from;
            how[to] = step;
            queue.add(to);
        }
    }

    private static void play(ResearchTableBlockEntity table, ServerPlayer player, RuneBoard board, List<Step> route) {
        for (Step step : route) {
            if (step.point() >= 0) {
                table.leave(player, step.point());
            } else {
                table.write(player, step.item(), false, landing(table, board, step.item()));
            }
        }
    }

    /** Where the item's next symbol would land, as the screen shows it (from the orb when no sheet is open). */
    private static int landing(ResearchTableBlockEntity table, RuneBoard board, Item item) {
        ResearchSheet sheet = table.sheet();
        if (!sheet.isOpen()) {
            ResearchSheet fresh = new ResearchSheet();
            fresh.begin(board.par().sheet());
            sheet = fresh;
        }
        return sheet.preview(board, ResearchItems.of(item), false).end();
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
        table.setItem(ResearchTableBlockEntity.HEXTECH, new ItemStack(ModItems.UNSTABLE_HEXTECH.get(), 3));
        return table;
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
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);
        return player;
    }
}
