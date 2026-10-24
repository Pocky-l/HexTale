package com.pockyl.hextale.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.AlembicBlock;
import com.pockyl.hextale.block.AlembicBlockEntity;
import com.pockyl.hextale.block.BrassPipeBlock;
import com.pockyl.hextale.block.DrainValveBlock;
import com.pockyl.hextale.block.EssenceMachine;
import com.pockyl.hextale.block.PipeSide;
import com.pockyl.hextale.block.PortMode;
import com.pockyl.hextale.block.PumpBlock;
import com.pockyl.hextale.block.EssenceTankBlockEntity;
import com.pockyl.hextale.block.InkSupply;
import com.pockyl.hextale.block.SeparatorBlock;
import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.block.Requirement;
import com.pockyl.hextale.block.ResearchTableBlock;
import com.pockyl.hextale.block.ResearchTableBlockEntity;
import com.pockyl.hextale.block.TablePart;
import com.pockyl.hextale.item.DiaryOwner;
import com.pockyl.hextale.item.ResearchersDiaryItem;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.HexGrid;
import com.pockyl.hextale.research.ItemEssenceResolver;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.Loupe;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.research.ResearchSheet;
import com.pockyl.hextale.research.ResearchBoard;
import com.pockyl.hextale.research.ResearchBoards;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.item.ResearchNotesItem;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The research loop on a server: boards and threads of the research topics, the prism at the table, lightning making
 * orbs, knowledge kept on the player.
 * Every test runs in its own batch: lightning and mock players must not reach the neighbouring tests.
 */
public final class ResearchGameTests {
    private static final BlockPos TABLE = new BlockPos(0, 1, 0);
    private static final ComplexRune SMELTING = ComplexRunes.byKey("fire_earth");
    /** A chapter-I-like topic (stone walls only) whose step opens right after the ink. */
    private static final ResearchTopic TOPIC = ResearchTopics.CIRCLE;

    private ResearchGameTests() {
    }

    // ---- Boards ---------------------------------------------------------------------------------------------------

    @HexTest(maxTicks = 400)
    public static void everyBoardCanBeFinished(GameTestHelper helper) {
        long start = System.nanoTime();
        int[] points = {0, 3, 4, 6};
        int[][] lengths = new int[4][2];
        int[] darkShare = new int[4];
        int[] curseShare = new int[4];
        int[] boards = new int[4];
        for (int[] range : lengths) {
            range[0] = Integer.MAX_VALUE;
        }
        for (ResearchTopic topic : ResearchTopics.table()) {
            ResearchBoard board = ResearchBoards.get(topic);
            int tier = board.tier();
            ResearchBoard.Par par = board.par();
            helper.assertTrue(board.points().size() == points[tier], topic.id() + " has " + points[tier] + " points");
            int inks = tier < 3 ? 3 : 6;
            helper.assertTrue(board.essences().size() == 6 && board.inks().size() == inks,
                    topic.id() + ": " + inks + " different inks around the hexagon, got " + board.essences());
            for (int d = 0; d < 6 && tier < 3; d++) {
                helper.assertTrue(board.essence(d) == board.essence((d + 3) % 6), topic.id() + ": opposite directions share an ink");
            }
            if (tier == 1) {
                helper.assertTrue(board.inks().stream().allMatch(Essence::isPrimal), topic.id() + ": a first parchment asks for primal inks only");
            }
            helper.assertTrue(board.essences().contains(topic.action()) && board.essences().contains(topic.target()),
                    topic.id() + ": both primals of the theme are there");
            for (ResearchBoard.Point point : board.points()) {
                helper.assertTrue(board.essences().contains(point.element()), topic.id() + ": points carry essences of the parchment");
            }
            helper.assertTrue(par.three() >= 1 && par.three() == par.plain() && par.two() > par.three(), topic.id() + ": sane thresholds " + par);
            helper.assertTrue(!board.wall(HexGrid.CENTRE) && !board.dark(HexGrid.CENTRE), topic.id() + ": the orb is never walled in");
            int darkCount = 0;
            int curseCount = 0;
            for (int cell = 0; cell < HexGrid.COUNT; cell++) {
                if (board.dark(cell)) {
                    darkCount++;
                    helper.assertTrue(HexGrid.steps(HexGrid.CENTRE, cell) > 1, topic.id() + ": the orb's neighbours are never dark");
                    helper.assertTrue(board.pointAt(cell) < 0, topic.id() + ": no point lies in a dark field");
                }
                helper.assertTrue(!board.cursed(cell) || board.dark(cell), topic.id() + ": only dark sections are cursed");
                if (board.cursed(cell)) {
                    curseCount++;
                    helper.assertTrue(HexGrid.steps(HexGrid.CENTRE, cell) > 1 && board.pointAt(cell) < 0, topic.id() + ": curses keep off the orb and the points");
                }
                if (HexGrid.isCorner(cell)) {
                    helper.assertTrue(board.wall(cell) && !board.stone(cell) && !board.dark(cell) && !board.cursed(cell) && board.pointAt(cell) < 0,
                            topic.id() + ": the corners hold the ink wells and nothing else");
                }
            }
            helper.assertTrue(darkCount > 0, topic.id() + ": every parchment has dark fields");
            helper.assertTrue(ResearchBoards.deducible(board), topic.id() + ": solvable without a guess");
            darkShare[tier] += darkCount;
            curseShare[tier] += curseCount;
            boards[tier]++;
            helper.assertTrue(curseCount > 0, topic.id() + ": every parchment can be lost on a curse");
            int[] tree = ResearchBoards.plainTree(board);
            helper.assertTrue(tree != null, topic.id() + ": a plain drawing exists");
            ResearchSheet.Trace trace = ResearchSheet.trace(board, tree);
            helper.assertTrue(trace != null && trace.complete(board), topic.id() + ": the plain drawing follows the rules and reaches every point");
            helper.assertTrue(tree.length / 2 == par.plain(), topic.id() + ": the thresholds come from that drawing");
            lengths[tier][0] = Math.min(lengths[tier][0], par.plain());
            lengths[tier][1] = Math.max(lengths[tier][1], par.plain());
        }
        Hextale.LOGGER.info("Dark sections per board by tier: 1 {}, 2 {}, 3 {}; curses: 1 {}, 2 {}, 3 {}",
                darkShare[1] / Math.max(1, boards[1]), darkShare[2] / Math.max(1, boards[2]), darkShare[3] / Math.max(1, boards[3]),
                curseShare[1] / Math.max(1, boards[1]), curseShare[2] / Math.max(1, boards[2]), curseShare[3] / Math.max(1, boards[3]));
        StringBuilder themes = new StringBuilder();
        for (ResearchTopic topic : ResearchTopics.table()) {
            themes.append(topic.id()).append('=').append(ResearchBoards.get(topic).essences()).append("; ");
        }
        Hextale.LOGGER.info("Board themes: {}", themes);
        Hextale.LOGGER.info("Checked {} research boards in {} ms; plain drawings by tier: 1 {}-{}, 2 {}-{}, 3 {}-{}",
                ResearchTopics.table().size(), (System.nanoTime() - start) / 1_000_000, lengths[1][0], lengths[1][1], lengths[2][0], lengths[2][1],
                lengths[3][0], lengths[3][1]);
        helper.succeed();
    }

    @HexTest
    public static void theInkFollowsTheRules(GameTestHelper helper) {
        ResearchBoard board = ResearchBoards.get(TOPIC);
        int[] plain = ResearchBoards.plainTree(board);
        int first = plain[1];
        helper.assertTrue(ResearchSheet.trace(board, new int[0]) != null, "a bare orb is a drawing");
        int far = -1;
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            if (HexGrid.steps(HexGrid.CENTRE, cell) == 3 && !board.wall(cell)) {
                far = cell;
                break;
            }
        }
        helper.assertTrue(ResearchSheet.trace(board, new int[]{HexGrid.CENTRE, far}) == null, "the ink never jumps");
        helper.assertTrue(ResearchSheet.trace(board, new int[]{first, HexGrid.CENTRE}) == null, "a step starts at inked parchment");
        helper.assertTrue(ResearchSheet.trace(board, new int[]{HexGrid.CENTRE, first, first, HexGrid.CENTRE}) == null,
                "the ink never returns to the orb");
        helper.assertTrue(ResearchSheet.trace(board, new int[]{HexGrid.CENTRE, first, HexGrid.CENTRE, first}) == null,
                "no section is inked twice");

        // Lines may start at the orb again, or branch from the middle of a line.
        int[] around = HexGrid.neighbours(HexGrid.CENTRE);
        int a = -1;
        int b = -1;
        for (int n : around) {
            if (!board.wall(n)) {
                if (a < 0) {
                    a = n;
                } else if (b < 0 && !HexGrid.adjacent(a, n)) {
                    b = n;
                }
            }
        }
        helper.assertTrue(ResearchSheet.trace(board, new int[]{HexGrid.CENTRE, a, HexGrid.CENTRE, b}) != null, "two lines from the orb");
        int branch = -1;
        for (int n : HexGrid.neighbours(a)) {
            if (n != HexGrid.CENTRE && n != b && !board.wall(n)) {
                branch = n;
            }
        }
        helper.assertTrue(ResearchSheet.trace(board, new int[]{HexGrid.CENTRE, a, HexGrid.CENTRE, b, a, branch}) != null,
                "a branch from an inked section");

        // Never through stone.
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
        int[] path = ResearchSheet.fromPath(pathTo(board, before));
        int[] through = Arrays.copyOf(path, path.length + 2);
        through[path.length] = before;
        through[path.length + 1] = wall;
        helper.assertTrue(ResearchSheet.trace(board, path) != null, "the way to the stone is open");
        helper.assertTrue(ResearchSheet.trace(board, through) == null, "stone blocks the ink");
        helper.succeed();
    }

    /** In the later chapters the ink may cross dark fields, but a cursed section bursts the orb and ends the drawing. */
    @HexTest
    public static void cursedSectionsBurstTheOrb(GameTestHelper helper) {
        ResearchBoard board = cursedBoard();
        int cursed = cursedCell(board);
        int[] tree = ResearchSheet.fromPath(pathTo(board, cursed));
        ResearchSheet.Trace trace = ResearchSheet.trace(board, tree);
        helper.assertTrue(trace != null && trace.burst() == tree.length / 2 - 1, "the step onto the curse bursts");
        helper.assertFalse(trace.complete(board), "a burst drawing never binds");
        int counted = 0;
        for (int n : HexGrid.neighbours(tree[tree.length - 2])) {
            if (board.cursed(n)) {
                counted++;
            }
        }
        helper.assertTrue(board.danger(tree[tree.length - 2]) == counted && counted >= 1, "a section counts the curses around it");
        helper.succeed();
    }

    @HexTest
    public static void stepsPayTheInkOfTheirDirection(GameTestHelper helper) {
        ResearchBoard board = ResearchBoards.get(TOPIC);
        int[] tree = ResearchBoards.plainTree(board);
        ResearchSheet.Trace trace = ResearchSheet.trace(board, tree);
        int[] ink = new int[Essence.values().length];
        for (int i = 0; i < tree.length / 2; i++) {
            int point = board.pointAt(tree[2 * i + 1]);
            Essence colour = point >= 0 ? board.points().get(point).element() : ResearchSheet.toward(board, tree[2 * i], tree[2 * i + 1]);
            helper.assertTrue(trace.paying()[i] == colour, "step " + i + " costs the ink of its direction, or of the point it reaches");
            ink[colour.ordinal()] += point >= 0 ? ResearchSheet.POINT_COST : 1;
        }
        java.util.Set<Essence> fromCentre = java.util.EnumSet.noneOf(Essence.class);
        java.util.Set<Essence> anywhere = java.util.EnumSet.noneOf(Essence.class);
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            for (int next : HexGrid.neighbours(cell)) {
                anywhere.add(ResearchSheet.toward(board, cell, next));
                if (cell == HexGrid.CENTRE) {
                    fromCentre.add(ResearchSheet.toward(board, cell, next));
                }
            }
        }
        helper.assertTrue(fromCentre.equals(new java.util.HashSet<>(board.inks())), "the six steps from the orb pay every ink of the parchment, got " + fromCentre);
        helper.assertTrue(anywhere.equals(new java.util.HashSet<>(board.inks())), "the inks are the parchment's essences");
        helper.assertTrue(Arrays.equals(ink, trace.ink()), "the ink needed is the sum of the steps, a point ten of its own essence");
        helper.succeed();
    }

    @HexTest
    public static void chaptersHoldAllRunes(GameTestHelper helper) {
        helper.assertTrue(ResearchBook.runes(1).size() == 6, "chapter I has Smelting and its ring of five runes");
        helper.assertTrue(ResearchBook.runes(1).size() + ResearchBook.runes(2).size() + ResearchBook.runes(3).size() == 72,
                "the chapters hold all 72 runes");
        helper.succeed();
    }

    @HexTest
    public static void everyItemCarriesElements(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        Map<Item, ItemEssences> all = ItemEssenceResolver.resolve(server.getRecipeManager(), server.registryAccess());
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            ItemEssences elements = all.get(item);
            helper.assertTrue(elements != null, BuiltInRegistries.ITEM.getKey(item) + " carries elements");
            helper.assertTrue(!elements.essences().isEmpty() && elements.essences().size() <= 3, item + ": one to three essences");
            elements.essences().values().forEach(points -> helper.assertTrue(points >= 1 && points <= 32, item + ": amount 1-32"));
        }
        // Every essence must be gatherable: carried, as the strongest essence, by some items.
        int[] carriers = new int[Essence.COUNT];
        int[] strongest = new int[Essence.COUNT];
        int[] sizes = new int[7];
        for (ItemEssences essences : all.values()) {
            essences.essences().keySet().forEach(e -> carriers[e.ordinal()]++);
            strongest[essences.main().ordinal()]++;
            sizes[Math.min(6, essences.essences().size())]++;
        }
        List<String> rare = new ArrayList<>();
        List<String> noReagent = new ArrayList<>();
        for (Essence essence : Essence.values()) {
            if (carriers[essence.ordinal()] < 3) {
                rare.add(essence.id() + "=" + carriers[essence.ordinal()]);
            }
            if (strongest[essence.ordinal()] < 1) {
                noReagent.add(essence.id());
            }
        }
        helper.assertTrue(rare.isEmpty(), "every essence is carried by at least three items, short: " + rare);
        helper.assertTrue(noReagent.isEmpty(), "every essence is the strongest of some item (its clearest source), missing: " + noReagent);
        Hextale.LOGGER.info("Items by number of essences 1-3: {} {} {}", sizes[1], sizes[2], sizes[3]);
        for (Map.Entry<Item, ItemEssences> entry : ResearchItems.canonical().entrySet()) {
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

    // ---- The loupe -------------------------------------------------------------------------------------------------

    @HexTest(maxTicks = 200)
    public static void theLoupeStudiesWhatItRestsOn(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        player.getInventory().setItem(0, new ItemStack(Items.GLASS, 3));
        player.getInventory().setItem(1, new ItemStack(Items.DIRT));
        player.containerMenu = new ResearchTableMenu(1, player.getInventory(), table, table.getBlockPos());
        int glass = ResearchTableBlockEntity.SLOTS + 27;
        int dirt = glass + 1;
        helper.assertTrue(player.containerMenu.slots.get(glass).getItem().is(Items.GLASS), "the hotbar's first slot holds the glass");
        ResourceLocation glassId = BuiltInRegistries.ITEM.getKey(Items.GLASS);

        table.loupe(player, glass);
        helper.assertTrue(table.isLooking(), "the arm swings over the desk for everyone");
        table.studyUnderLoupe(player, glass);
        helper.assertFalse(Research.of(player).hasStudied(glassId), "a glance is not a study");
        helper.runAfterDelay(Loupe.STUDY_TICKS, () -> {
            table.studyUnderLoupe(player, dirt);
            helper.assertFalse(Research.of(player).hasStudied(BuiltInRegistries.ITEM.getKey(Items.DIRT)), "only what the loupe rests on");
            table.studyUnderLoupe(player, glass);
            helper.assertTrue(Research.of(player).hasStudied(glassId), "the glass is studied");
            helper.assertTrue(player.getInventory().getItem(0).getCount() == 3, "studying costs nothing");

            // Moving to the dirt starts the lenses over.
            table.loupe(player, dirt);
            table.studyUnderLoupe(player, dirt);
            helper.assertFalse(Research.of(player).hasStudied(BuiltInRegistries.ITEM.getKey(Items.DIRT)), "a new item needs its own look");
            table.loupe(player, -1);
            helper.assertFalse(table.isLooking(), "put back, the arm returns to its rest");
            player.discard();
            helper.succeed();
        });
    }

    // ---- The table ------------------------------------------------------------------------------------------------

    // ---- Essence works --------------------------------------------------------------------------------------------

    @HexTest
    public static void essencePartsKeepTheirProportions(GameTestHelper helper) {
        EssenceMix mixture = EssenceMix.of(java.util.Map.of(Essence.LIFE, 12, Essence.EARTH, 4));
        EssenceMix part = mixture.part(8);
        helper.assertTrue(part.get(Essence.LIFE) == 6 && part.get(Essence.EARTH) == 2, "a part keeps three life to one earth: " + part);
        helper.assertTrue(mixture.minus(part).total() == 8, "what is left is the rest");
        helper.assertTrue(mixture.pureEssence() == null && mixture.kinds() == 2, "two elements make a mixture");
        helper.assertTrue(EssenceMix.pure(Essence.FIRE, 3).pureEssence() == Essence.FIRE, "one element makes a concentrate");
        EssenceMix odd = EssenceMix.of(java.util.Map.of(Essence.FIRE, 1, Essence.WATER, 1, Essence.AIR, 1));
        helper.assertTrue(odd.part(2).total() == 2, "rounding never loses or makes drops");
        helper.succeed();
    }

    @HexTest(maxTicks = 300)
    public static void anAlembicBoilsAnItemIntoAMixture(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at.below(), Blocks.MAGMA_BLOCK);
        helper.setBlock(at, ModBlocks.ALEMBIC.get().defaultBlockState().setValue(AlembicBlock.LIT, true));
        AlembicBlockEntity alembic = helper.<AlembicBlockEntity>getBlockEntity(at);
        alembic.setItem(AlembicBlockEntity.INPUT, new ItemStack(Items.OAK_PLANKS));
        ServerPlayer boiler = player(helper);
        boiler.setGameMode(GameType.SURVIVAL);
        Research.of(boiler).study(ResourceLocation.withDefaultNamespace("oak_planks"));
        alembic.boiledBy(boiler);
        alembic.setItem(AlembicBlockEntity.FLASK_IN, new ItemStack(Items.GLASS_BOTTLE, 2));
        alembic.fillWater(1000, false);
        EssenceMix expected = EssenceMix.boiled(ResearchItems.of(Items.OAK_PLANKS), AlembicBlockEntity.DROPS_PER_POINT);
        helper.succeedWhen(() -> {
            EssenceMix boiled = EssenceFlaskItem.contents(alembic.getItem(AlembicBlockEntity.FLASK_OUT));
            helper.assertTrue(boiled.equals(expected), "oak planks boil into " + expected + ", got " + boiled);
            helper.assertTrue(alembic.getItem(AlembicBlockEntity.FLASK_IN).getCount() == 1, "one glass bottle was used");
            helper.assertTrue(alembic.water() == 1000 - AlembicBlockEntity.WATER_PER_ITEM, "and the item took its water");
        });
    }

    /** An item its boiler has not studied gives only as much dark unknown essence. */
    @HexTest(maxTicks = 300)
    public static void anUnstudiedItemBoilsIntoUnknownEssence(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at.below(), Blocks.MAGMA_BLOCK);
        helper.setBlock(at, ModBlocks.ALEMBIC.get().defaultBlockState().setValue(AlembicBlock.LIT, true));
        AlembicBlockEntity alembic = helper.<AlembicBlockEntity>getBlockEntity(at);
        alembic.setItem(AlembicBlockEntity.INPUT, new ItemStack(Items.OAK_PLANKS));
        ServerPlayer stranger = player(helper);
        stranger.setGameMode(GameType.SURVIVAL);
        alembic.boiledBy(stranger);
        alembic.setItem(AlembicBlockEntity.FLASK_IN, new ItemStack(Items.GLASS_BOTTLE));
        alembic.fillWater(1000, false);
        int total = EssenceMix.boiled(ResearchItems.of(Items.OAK_PLANKS), AlembicBlockEntity.DROPS_PER_POINT).total();
        helper.succeedWhen(() -> {
            EssenceMix boiled = EssenceFlaskItem.contents(alembic.getItem(AlembicBlockEntity.FLASK_OUT));
            helper.assertTrue(boiled.equals(EssenceMix.unknown(total)), "unstudied oak planks boil into " + total + " mB of unknown essence, got " + boiled);
        });
    }

    @HexTest(maxTicks = 200)
    public static void aDryAlembicDoesNotBoil(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at.below(), Blocks.MAGMA_BLOCK);
        helper.setBlock(at, ModBlocks.ALEMBIC.get().defaultBlockState().setValue(AlembicBlock.LIT, true));
        AlembicBlockEntity alembic = helper.<AlembicBlockEntity>getBlockEntity(at);
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
        AlembicBlockEntity alembic = helper.<AlembicBlockEntity>getBlockEntity(alembicAt);
        helper.assertTrue(helper.getBlockState(alembicAt).getValue(EssenceMachine.PORTS.get(Direction.SOUTH)), "the pipe joins the alembic's back");
        helper.succeedWhen(() -> helper.assertTrue(alembic.water() >= 500, "the pump fills the flask, got " + alembic.water() + " mB"));
    }

    /** A separator's inlet draws the mixture through a pipe out of a jar standing beside it. */
    @HexTest(maxTicks = 100)
    public static void aSeparatorDrawsFromAPipedJar(GameTestHelper helper) {
        BlockPos separatorAt = new BlockPos(3, 2, 3);
        BlockPos jarAt = separatorAt.south(2);
        helper.setBlock(separatorAt.below(), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(jarAt.below(), net.minecraft.world.level.block.Blocks.STONE);
        helper.setBlock(separatorAt, ModBlocks.SEPARATOR.get());
        helper.setBlock(jarAt, ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(separatorAt.south(), ModBlocks.BRASS_PIPE.get());
        EssenceTankBlockEntity jar = helper.<EssenceTankBlockEntity>getBlockEntity(jarAt);
        jar.tank().set(EssenceMix.pure(Essence.FIRE, 40));
        com.pockyl.hextale.block.SeparatorBlockEntity separator = helper.<com.pockyl.hextale.block.SeparatorBlockEntity>getBlockEntity(separatorAt);
        helper.succeedWhen(() -> helper.assertTrue(separator.mix().amount() > 0 || jar.tank().amount() < 40,
                "the separator draws from the jar through the pipe: jar " + jar.tank().amount() + ", separator " + separator.mix().amount()));
    }

    /** The same from a Big Essence Jar: the pipe meets one of its glass blocks. */
    @HexTest(maxTicks = 100)
    public static void aSeparatorDrawsFromAPipedBigJar(GameTestHelper helper) {
        BlockPos corner = new BlockPos(1, 2, 4);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 3; x++) {
                for (int z = 0; z < 3; z++) {
                    if (x == 1 && z == 1 && (y == 1 || y == 2)) {
                        continue;
                    }
                    helper.setBlock(corner.offset(x, y, z), y == 3 ? net.minecraft.world.level.block.Blocks.OAK_SLAB
                            : net.minecraft.world.level.block.Blocks.GLASS);
                }
            }
        }
        var found = com.pockyl.hextale.multiblock.Multiblocks.find(helper.getLevel(), helper.absolutePos(corner),
                new ItemStack(ModItems.HEXTECH_DUST.get()));
        helper.assertTrue(found.isPresent(), "the big jar is found");
        com.pockyl.hextale.multiblock.Multiblocks.form(helper.getLevel(), found.get().getKey(), found.get().getValue(), null);
        EssenceTankBlockEntity jar = helper.<EssenceTankBlockEntity>getBlockEntity(corner.offset(1, 0, 1));
        jar.tank().set(EssenceMix.pure(Essence.FIRE, 40));
        // Separator north of the jar, its back (south) piped to the jar's north side.
        BlockPos separatorAt = corner.offset(1, 0, -2);
        helper.setBlock(separatorAt, ModBlocks.SEPARATOR.get());
        helper.setBlock(separatorAt.south(), ModBlocks.BRASS_PIPE.get());
        com.pockyl.hextale.block.SeparatorBlockEntity separator = helper.<com.pockyl.hextale.block.SeparatorBlockEntity>getBlockEntity(separatorAt);
        helper.succeedWhen(() -> helper.assertTrue(separator.mix().amount() > 0,
                "the separator draws from the big jar: jar " + jar.tank().amount() + ", separator " + separator.mix().amount()));
    }

    @HexTest(maxTicks = 100)
    public static void anOpenValveDrainsWhatComes(GameTestHelper helper) {
        BlockPos tankAt = new BlockPos(1, 3, 1);
        BlockPos valveAt = new BlockPos(2, 2, 1);
        helper.setBlock(tankAt, ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(tankAt.below(), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(valveAt, ModBlocks.DRAIN_VALVE.get());
        EssenceTankBlockEntity tank = helper.<EssenceTankBlockEntity>getBlockEntity(tankAt);
        tank.tank().set(EssenceMix.pure(Essence.FIRE, 12));
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
        AlembicBlockEntity alembic = helper.<AlembicBlockEntity>getBlockEntity(at);
        alembic.setItem(AlembicBlockEntity.INPUT, new ItemStack(Items.COAL, 2));
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(alembic.input().getCount() == 2 && alembic.progress() == 0, "no heat, no boiling");
            helper.succeed();
        });
    }

    @HexTest(maxTicks = 400)
    public static void theChosenEssenceIsSeparated(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.SEPARATOR.get().defaultBlockState());
        SeparatorBlockEntity separator = helper.<SeparatorBlockEntity>getBlockEntity(at);
        EssenceMix mixture = EssenceMix.of(java.util.Map.of(Essence.LIFE, 8, Essence.EARTH, 8));
        separator.setItem(SeparatorBlockEntity.POUR, EssenceFlaskItem.filled(mixture));
        separator.setItem(SeparatorBlockEntity.FUEL, new ItemStack(Items.BLAZE_POWDER, 3));
        separator.choose(Essence.EARTH);
        separator.setItem(SeparatorBlockEntity.SUPPLY, new ItemStack(Items.GLASS_BOTTLE, 2));
        helper.succeedWhen(() -> {
            EssenceMix rest = EssenceFlaskItem.contents(separator.getItem(SeparatorBlockEntity.REST_OUT));
            EssenceMix drawn = EssenceFlaskItem.contents(separator.getItem(SeparatorBlockEntity.EXTRACT_OUT));
            // 16 mB of mixture take four steps of 4 mB, each drawing earth and adding its unknown essence.
            helper.assertTrue(rest.equals(EssenceMix.pure(Essence.LIFE, 8).plus(EssenceMix.unknown(4 * SeparatorBlockEntity.UNKNOWN_PER_STEP)))
                    && rest.pureEssence() == null, "drawing earth leaves the life with a dark unknown essence on top, no concentrate, got " + rest);
            helper.assertTrue(drawn.equals(EssenceMix.pure(Essence.EARTH, 8)), "and draws out pure earth, got " + drawn);
            helper.assertTrue(separator.getItem(SeparatorBlockEntity.FUEL).getCount() == 2, "one blaze powder draws the eight mB of earth");
        });
    }

    /** Choosing the unknown essence filters it out: the rest comes out clean, with no new unknown essence on top. */
    @HexTest(maxTicks = 300)
    public static void theUnknownEssenceIsFilteredOut(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.SEPARATOR.get().defaultBlockState());
        SeparatorBlockEntity separator = helper.<SeparatorBlockEntity>getBlockEntity(at);
        separator.setItem(SeparatorBlockEntity.POUR, EssenceFlaskItem.filled(EssenceMix.pure(Essence.FIRE, 8).plus(EssenceMix.unknown(8))));
        separator.setItem(SeparatorBlockEntity.FUEL, new ItemStack(Items.BLAZE_POWDER, 2));
        separator.chooseUnknown();
        separator.setItem(SeparatorBlockEntity.SUPPLY, new ItemStack(Items.GLASS_BOTTLE, 2));
        helper.succeedWhen(() -> {
            EssenceMix rest = EssenceFlaskItem.contents(separator.getItem(SeparatorBlockEntity.REST_OUT));
            EssenceMix drawn = EssenceFlaskItem.contents(separator.getItem(SeparatorBlockEntity.EXTRACT_OUT));
            helper.assertTrue(rest.equals(EssenceMix.pure(Essence.FIRE, 8)), "the fire comes out clean, a concentrate again, got " + rest);
            helper.assertTrue(drawn.equals(EssenceMix.unknown(8)), "the unknown essence is drawn out, got " + drawn);
        });
    }

    @HexTest(maxTicks = 300)
    public static void theSeparatorBurnsOnlyWhileItDrawsAnEssenceOut(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.SEPARATOR.get().defaultBlockState());
        SeparatorBlockEntity separator = helper.<SeparatorBlockEntity>getBlockEntity(at);
        separator.setItem(SeparatorBlockEntity.POUR, EssenceFlaskItem.filled(EssenceMix.of(java.util.Map.of(Essence.FIRE, 8, Essence.EARTH, 8))));
        separator.setItem(SeparatorBlockEntity.SUPPLY, new ItemStack(Items.GLASS_BOTTLE, 2));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(!helper.getBlockState(at).getValue(SeparatorBlock.LIT), "no fire before an essence is chosen");
            separator.choose(Essence.FIRE);
            separator.setItem(SeparatorBlockEntity.FUEL, new ItemStack(Items.BLAZE_POWDER));
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(helper.getBlockState(at).getValue(SeparatorBlock.LIT), "the powder burns while fire is drawn out");
                helper.succeedWhen(() -> {
                    helper.assertTrue(separator.mix().isEmpty(), "the mixture is split");
                    helper.assertTrue(!helper.getBlockState(at).getValue(SeparatorBlock.LIT), "and the fire goes out");
                });
            });
        });
    }

    @HexTest(maxTicks = 300)
    public static void withoutAChosenEssenceNothingIsSplit(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.SEPARATOR.get().defaultBlockState());
        SeparatorBlockEntity separator = helper.<SeparatorBlockEntity>getBlockEntity(at);
        separator.setItem(SeparatorBlockEntity.POUR, EssenceFlaskItem.filled(EssenceMix.of(java.util.Map.of(Essence.LIFE, 8, Essence.EARTH, 8))));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(separator.mix().amount() == 16 && separator.rest().isEmpty(), "the mixture waits for a chosen essence");
            helper.assertTrue("no_essence".equals(separator.blocked()), "and the screen says so");
            separator.choose(Essence.EARTH);
            helper.assertTrue("no_fuel".equals(separator.blocked()), "then for blaze powder");
            separator.choose(Essence.AIR);
            separator.setItem(SeparatorBlockEntity.FUEL, new ItemStack(Items.BLAZE_POWDER));
            helper.runAfterDelay(100, () -> {
                // Air is chosen; the mixture has none, so the separator does nothing with it.
                helper.assertTrue(separator.mix().amount() == 16 && separator.rest().isEmpty() && separator.extract().isEmpty(),
                        "a mixture without the chosen essence stays in the flask, got mix " + separator.mix().contents());
                helper.assertTrue("missing".equals(separator.blocked()), "and the screen says the mixture holds none of it");
                helper.assertTrue(separator.getItem(SeparatorBlockEntity.FUEL).getCount() == 1, "and burns no powder");
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
        // As placed by a player: the separator shows its ports, so the pipes join them.
        helper.setBlock(at, SeparatorBlock.withLooks(helper.getBlockState(at), helper.getLevel(), helper.absolutePos(at)));
        EssenceTankBlockEntity source = helper.<EssenceTankBlockEntity>getBlockEntity(at.south(2).above());
        EssenceTankBlockEntity sink = helper.<EssenceTankBlockEntity>getBlockEntity(at.north(2));
        SeparatorBlockEntity separator = helper.<SeparatorBlockEntity>getBlockEntity(at);
        source.tank().set(EssenceMix.of(java.util.Map.of(Essence.LIFE, 12, Essence.EARTH, 12)));
        separator.setItem(SeparatorBlockEntity.FUEL, new ItemStack(Items.BLAZE_POWDER, 1));
        separator.choose(Essence.EARTH);
        helper.succeedWhen(() -> {
            helper.assertTrue(source.tank().isEmpty() && separator.mix().isEmpty(), "the mixture was worked off");
            helper.assertTrue(sink.tank().contents().get(Essence.LIFE) == 12 && sink.tank().contents().isUnknown()
                    && sink.tank().contents().get(Essence.EARTH) == 0,
                    "the rest ran on into the tank in front, got " + sink.tank().contents() + ", separator rest " + separator.rest().contents());
            helper.assertTrue(separator.extract().contents().equals(EssenceMix.pure(Essence.EARTH, 12)),
                    "the concentrate waits in the separator, got " + separator.extract().contents());
        });
    }

    @HexTest(maxTicks = 400)
    public static void theRestNeverFlowsBackIntoTheSeparator(GameTestHelper helper) {
        // One pipe network touches the separator's front (rest out) and its east side (mixture in), and a tank at its
        // side and its bottom: the rest must not go round in circles. The tank takes it in from below (through the
        // pipe it pours into), never at its side, and what it pours never gets back into the separator.
        BlockPos at = new BlockPos(2, 3, 2);
        helper.setBlock(at, ModBlocks.SEPARATOR.get().defaultBlockState().setValue(SeparatorBlock.FACING, Direction.NORTH));
        for (BlockPos pipe : new BlockPos[]{at.north(), at.north().east(), at.east(), at.north().east().below(), at.north().east(2).below()}) {
            helper.setBlock(pipe, ModBlocks.BRASS_PIPE.get());
        }
        helper.setBlock(at.north().east(2), ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity tank = helper.<EssenceTankBlockEntity>getBlockEntity(at.north().east(2));
        SeparatorBlockEntity separator = helper.<SeparatorBlockEntity>getBlockEntity(at);
        separator.setItem(SeparatorBlockEntity.POUR, EssenceFlaskItem.filled(EssenceMix.of(java.util.Map.of(Essence.EARTH, 8, Essence.AIR, 8))));
        separator.setItem(SeparatorBlockEntity.FUEL, new ItemStack(Items.BLAZE_POWDER, 1));
        separator.choose(Essence.EARTH);
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(separator.mix().isEmpty(), "the mixture was worked off, left " + separator.mix().contents());
            helper.assertTrue(tank.tank().contents().get(Essence.AIR) == 8 && separator.rest().isEmpty(),
                    "the air went into the tank from below, got rest " + separator.rest().contents() + ", tank " + tank.tank().contents());
            helper.succeed();
        });
    }

    @HexTest(maxTicks = 100)
    public static void aSeparatorInletDrawsFromTanks(GameTestHelper helper) {
        // Facing north: one tank stands right at its back (an inlet), another is piped to its left (east) side from the
        // tank's own side - neither lets out at its bottom here, the inlets draw.
        BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, ModBlocks.SEPARATOR.get().defaultBlockState().setValue(SeparatorBlock.FACING, Direction.NORTH));
        helper.setBlock(at.south(), ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(at.east(), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(at.east(2), ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity back = helper.<EssenceTankBlockEntity>getBlockEntity(at.south());
        EssenceTankBlockEntity piped = helper.<EssenceTankBlockEntity>getBlockEntity(at.east(2));
        back.tank().set(EssenceMix.pure(Essence.LIFE, 8));
        piped.tank().set(EssenceMix.pure(Essence.EARTH, 8));
        SeparatorBlockEntity separator = helper.<SeparatorBlockEntity>getBlockEntity(at);
        helper.succeedWhen(() -> {
            helper.assertTrue(back.tank().isEmpty() && piped.tank().isEmpty(), "both tanks were drawn empty");
            helper.assertTrue(separator.mix().amount() == 16, "into the separator's mixture, got " + separator.mix().contents());
        });
    }

    @HexTest(maxTicks = 60)
    public static void aCreativeTankNeverRunsDry(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.CREATIVE_ESSENCE_TANK.get());
        EssenceTankBlockEntity tank = helper.<EssenceTankBlockEntity>getBlockEntity(at);
        helper.runAfterDelay(1, () -> {
            helper.assertTrue(tank.isCreative() && tank.tank().contents().equals(EssenceTankBlockEntity.CREATIVE), "it holds every essence");
            tank.tank().remove(EssenceMix.pure(Essence.MAGMA, 100));
        });
        helper.assertTrue(tank.receive(Direction.NORTH, EssenceMix.pure(Essence.WATER, 4), true) == 0, "it takes nothing in");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(tank.tank().contents().get(Essence.MAGMA) == EssenceTankBlockEntity.CAPACITY, "and is full again the next tick");
            helper.succeed();
        });
    }

    @HexTest
    public static void aTankItemCarriesItsEssence(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity tank = helper.<EssenceTankBlockEntity>getBlockEntity(at);
        tank.tank().set(EssenceMix.pure(Essence.LIGHT, 40));
        ItemStack item = new ItemStack(ModItems.ESSENCE_TANK.get());
        item.applyComponents(tank.collectComponents());
        helper.assertTrue(EssenceMix.pure(Essence.LIGHT, 40).equals(item.get(ModDataComponents.ESSENCE.get())), "the broken tank's item keeps its light");
        helper.setBlock(at.east(), ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity placed = helper.<EssenceTankBlockEntity>getBlockEntity(at.east());
        placed.applyComponentsFromItemStack(item);
        helper.assertTrue(placed.tank().contents().equals(EssenceMix.pure(Essence.LIGHT, 40)), "and a tank placed from it holds it again");
        helper.succeed();
    }

    @HexTest(maxTicks = 200)
    public static void aDrainValveDrainsTanksBesideIt(GameTestHelper helper) {
        BlockPos valveAt = new BlockPos(2, 2, 2);
        helper.setBlock(valveAt, ModBlocks.DRAIN_VALVE.get().defaultBlockState().setValue(DrainValveBlock.OPEN, true));
        helper.setBlock(valveAt.west(), ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(valveAt.east(), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(valveAt.east(2), ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity beside = helper.<EssenceTankBlockEntity>getBlockEntity(valveAt.west());
        EssenceTankBlockEntity piped = helper.<EssenceTankBlockEntity>getBlockEntity(valveAt.east(2));
        beside.tank().set(EssenceMix.pure(Essence.FIRE, 40));
        piped.tank().set(EssenceMix.unknown(40));
        helper.assertTrue(helper.getBlockState(valveAt).getValue(EssenceMachine.PORTS.get(Direction.WEST)), "the valve shows its joint to the tank beside it");
        helper.succeedWhen(() -> helper.assertTrue(beside.tank().isEmpty() && piped.tank().isEmpty(), "the open valve drains both tanks"));
    }

    /** A jar with a sticker takes only its essence, pure; without one it takes anything. */
    @HexTest
    public static void aStickeredJarTakesOnlyItsEssence(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity tank = helper.<EssenceTankBlockEntity>getBlockEntity(at);
        helper.assertTrue(tank.receive(Direction.UP, EssenceMix.pure(Essence.WATER, 8), true) == 8, "a plain jar takes water");
        tank.setSticker(Essence.FIRE);
        helper.assertTrue(tank.receive(Direction.UP, EssenceMix.pure(Essence.WATER, 8), true) == 0, "a fire jar refuses water");
        helper.assertTrue(tank.receive(Direction.UP, EssenceMix.pure(Essence.FIRE, 4).plus(EssenceMix.pure(Essence.WATER, 4)), true) == 0,
                "and a mixture holding fire");
        helper.assertTrue(tank.receive(Direction.UP, EssenceMix.pure(Essence.FIRE, 8), false) == 8, "it takes pure fire");
        helper.succeed();
    }

    /** An Essence Jar shows a fitting on every face a pipe joins - the lid and the foot too - and beside a drain valve. */
    @HexTest
    public static void jarsShowFittingsWherePipesJoin(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(at.above(), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(at.below(), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(at.north(), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(at.east(), ModBlocks.DRAIN_VALVE.get());
        BlockState jar = helper.getBlockState(at);
        for (Direction face : new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.EAST}) {
            helper.assertTrue(jar.getValue(EssenceMachine.PORTS.get(face)), "the jar shows a fitting " + face);
        }
        helper.assertFalse(jar.getValue(EssenceMachine.PORTS.get(Direction.SOUTH)), "and none on a free side");
        helper.assertTrue(helper.getBlockState(at.above()).getValue(BrassPipeBlock.SIDES.get(Direction.DOWN)).open(),
                "the pipe on top joins the lid");
        helper.setBlock(at.above(), Blocks.AIR);
        helper.assertFalse(helper.getBlockState(at).getValue(EssenceMachine.PORTS.get(Direction.UP)), "the fitting goes with the pipe");
        helper.succeed();
    }

    @HexTest
    public static void pipesJoinMachinesOnlyAtPorts(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 2, 2);
        helper.setBlock(at, ModBlocks.SEPARATOR.get().defaultBlockState().setValue(SeparatorBlock.FACING, Direction.NORTH));
        helper.setBlock(at.east(), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(at.below(), ModBlocks.BRASS_PIPE.get());
        BlockState separator = helper.getBlockState(at);
        helper.assertTrue(separator.getValue(SeparatorBlock.LOOKS.get(Direction.EAST)) == PortMode.INLET, "the separator shows an inlet on its east side");
        helper.assertTrue(helper.getBlockState(at.east()).getValue(BrassPipeBlock.SIDES.get(Direction.WEST)).open(), "and the pipe joins it");
        helper.assertFalse(helper.getBlockState(at.below()).getValue(BrassPipeBlock.SIDES.get(Direction.UP)).open(),
                "the bottom is no port: the pipe below stays apart");
        // Facing north, the east face is the separator's left: make it a concentrate outlet.
        SeparatorBlockEntity entity = helper.<SeparatorBlockEntity>getBlockEntity(at);
        entity.cycle(SeparatorBlock.Side.LEFT);
        helper.assertTrue(helper.getBlockState(at).getValue(SeparatorBlock.LOOKS.get(Direction.EAST)) == PortMode.PRODUCT,
                "the joint follows the face's new mode");
        helper.assertTrue(entity.receive(Direction.EAST, EssenceMix.pure(Essence.FIRE, 4), true) == 0, "an outlet takes nothing in");
        // Rest out, then closed: the pipe lets go of it.
        entity.cycle(SeparatorBlock.Side.LEFT);
        entity.cycle(SeparatorBlock.Side.LEFT);
        helper.assertTrue(helper.getBlockState(at).getValue(SeparatorBlock.LOOKS.get(Direction.EAST)) == PortMode.NONE,
                "a closed side shows no joint");
        helper.assertFalse(helper.getBlockState(at.east()).getValue(BrassPipeBlock.SIDES.get(Direction.WEST)).open(),
                "and no pipe joins it");
        entity.cycle(SeparatorBlock.Side.LEFT);
        entity.cycle(SeparatorBlock.Side.LEFT);
        helper.setBlock(at.east(), Blocks.AIR);
        helper.assertTrue(helper.getBlockState(at).getValue(SeparatorBlock.LOOKS.get(Direction.EAST)) == PortMode.PRODUCT,
                "the joint keeps showing the mode without a pipe");
        helper.assertTrue(helper.getBlockState(at).getValue(SeparatorBlock.LOOKS.get(Direction.WEST)) == PortMode.PRODUCT,
                "the right side (west) is a concentrate outlet by default, pipe or not");
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
        EssenceTankBlockEntity source = helper.<EssenceTankBlockEntity>getBlockEntity(from);
        EssenceTankBlockEntity target = helper.<EssenceTankBlockEntity>getBlockEntity(to);
        source.tank().set(EssenceMix.pure(Essence.WATER, 12));
        helper.succeedWhen(() -> {
            helper.assertTrue(target.tank().contents().equals(EssenceMix.pure(Essence.WATER, 12)),
                    "the water ran down the pipe into the other tank, got " + target.tank().contents());
            helper.assertTrue(source.tank().isEmpty(), "the first tank is empty");
        });
    }

    /** A pipe side set to "out only" pumps a jar out through its side, and another jar takes it in from below. */
    @HexTest(maxTicks = 200)
    public static void aPipeSetToOutPumpsAJarIntoAnotherFromBelow(GameTestHelper helper) {
        BlockPos from = new BlockPos(1, 2, 1);
        BlockPos pump = from.east();
        BlockPos under = from.east(2);
        BlockPos to = under.above();
        helper.setBlock(from, ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(pump, ModBlocks.BRASS_PIPE.get());
        helper.setBlock(under, ModBlocks.BRASS_PIPE.get());
        helper.setBlock(to, ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(pump, helper.getBlockState(pump).setValue(BrassPipeBlock.SIDES.get(Direction.WEST), PipeSide.OUT));
        helper.assertTrue(helper.getBlockState(to).getValue(EssenceMachine.PORTS.get(Direction.DOWN)), "the jar shows its foot fitting");
        EssenceTankBlockEntity source = helper.<EssenceTankBlockEntity>getBlockEntity(from);
        EssenceTankBlockEntity target = helper.<EssenceTankBlockEntity>getBlockEntity(to);
        source.tank().set(EssenceMix.pure(Essence.FIRE, 12));
        helper.succeedWhen(() -> {
            helper.assertTrue(target.tank().contents().equals(EssenceMix.pure(Essence.FIRE, 12)),
                    "the fire was pumped into the other jar from below, got " + target.tank().contents());
            helper.assertTrue(source.tank().isEmpty(), "the first jar is empty");
        });
    }

    /** One jar never pours into the bottom of another: two jars on one pipe keep what they hold. */
    @HexTest(maxTicks = 100)
    public static void jarsNeverPourIntoEachOthersBottoms(GameTestHelper helper) {
        BlockPos a = new BlockPos(1, 3, 1);
        BlockPos b = a.east();
        helper.setBlock(a.below(), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(b.below(), ModBlocks.BRASS_PIPE.get());
        helper.setBlock(a, ModBlocks.ESSENCE_TANK.get());
        helper.setBlock(b, ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity first = helper.<EssenceTankBlockEntity>getBlockEntity(a);
        EssenceTankBlockEntity second = helper.<EssenceTankBlockEntity>getBlockEntity(b);
        first.tank().set(EssenceMix.pure(Essence.WATER, 12));
        second.tank().set(EssenceMix.pure(Essence.EARTH, 12));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(first.tank().contents().equals(EssenceMix.pure(Essence.WATER, 12))
                    && second.tank().contents().equals(EssenceMix.pure(Essence.EARTH, 12)),
                    "both jars keep their own, got " + first.tank().contents() + " and " + second.tank().contents());
            helper.succeed();
        });
    }

    /** Sneak-using a pipe's arm shuts the joint on both pipes and opens it again; shut or in-only sides pass nothing. */
    @HexTest(maxTicks = 200)
    public static void pipeSidesAreSetByHand(GameTestHelper helper) {
        BlockPos left = new BlockPos(1, 2, 1);
        BlockPos right = left.east();
        helper.setBlock(left, ModBlocks.BRASS_PIPE.get());
        helper.setBlock(right, ModBlocks.BRASS_PIPE.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        BlockPos at = helper.absolutePos(left);
        // The east arm of the left pipe: 6 px east of its centre.
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at).add(0.375, 0.1, 0), Direction.UP, at, false);
        helper.getBlockState(left).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(BrassPipeBlock.side(helper.getBlockState(left), Direction.EAST) == PipeSide.CLOSED
                && BrassPipeBlock.side(helper.getBlockState(right), Direction.WEST) == PipeSide.CLOSED, "both pipes shut the joint");
        helper.assertFalse(com.pockyl.hextale.essence.EssenceNetwork.network(helper.getLevel(), at).contains(helper.absolutePos(right)),
                "the pipes are apart");
        helper.getBlockState(left).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(BrassPipeBlock.side(helper.getBlockState(left), Direction.EAST) == PipeSide.PIPE
                && BrassPipeBlock.side(helper.getBlockState(right), Direction.WEST) == PipeSide.PIPE, "and open it again");
        // A jar above the right pipe: its side cycles both ways -> in only -> out only -> shut -> both ways.
        BlockPos jarAt = right.above();
        helper.setBlock(jarAt, ModBlocks.ESSENCE_TANK.get());
        BlockPos rightAt = helper.absolutePos(right);
        BlockHitResult up = new BlockHitResult(Vec3.atCenterOf(rightAt).add(0, 0.4, 0), Direction.UP, rightAt, false);
        PipeSide[] cycle = {PipeSide.IN, PipeSide.OUT, PipeSide.CLOSED, PipeSide.PIPE, PipeSide.IN};
        for (PipeSide expected : cycle) {
            helper.getBlockState(right).useWithoutItem(helper.getLevel(), player, up);
            helper.assertTrue(BrassPipeBlock.side(helper.getBlockState(right), Direction.UP) == expected, "the jar side is now " + expected);
        }
        helper.assertTrue(helper.getBlockState(jarAt).getValue(EssenceMachine.PORTS.get(Direction.DOWN)), "the jar shows the fitting");
        // In only: the jar pours nothing down into it, so the jar beside the pipe gets nothing.
        helper.setBlock(right.east(), ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity jar = helper.<EssenceTankBlockEntity>getBlockEntity(jarAt);
        EssenceTankBlockEntity beside = helper.<EssenceTankBlockEntity>getBlockEntity(right.east());
        jar.tank().set(EssenceMix.pure(Essence.WATER, 12));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(beside.tank().isEmpty() && jar.tank().amount() == 12, "an in-only side lets nothing out of the jar");
            // Back to both ways, but the side towards the jar beside is shut: still nothing reaches it.
            for (int i = 0; i < 3; i++) {
                helper.getBlockState(right).useWithoutItem(helper.getLevel(), player, up);
            }
            helper.assertTrue(BrassPipeBlock.side(helper.getBlockState(right), Direction.UP) == PipeSide.PIPE, "back to both ways");
            BlockHitResult east = new BlockHitResult(Vec3.atCenterOf(rightAt).add(0.4, 0, 0), Direction.EAST, rightAt, false);
            for (int i = 0; i < 3; i++) {
                helper.getBlockState(right).useWithoutItem(helper.getLevel(), player, east);
            }
            helper.assertTrue(BrassPipeBlock.side(helper.getBlockState(right), Direction.EAST) == PipeSide.CLOSED, "the side to the jar beside is shut");
            helper.assertFalse(helper.getBlockState(right.east()).getValue(EssenceMachine.PORTS.get(Direction.WEST)), "and it shows no fitting");
            helper.runAfterDelay(40, () -> {
                helper.assertTrue(beside.tank().isEmpty(), "a shut side lets nothing through, got " + beside.tank().contents());
                helper.succeed();
            });
        });
    }

    @HexTest
    public static void theTableDrawsInkFromTanksAround(GameTestHelper helper) {
        // Blocks outside the 1x1x1 test structure outlive their test: no jar left by another test may be in reach.
        int reach = JarReach.RADIUS + 2;
        for (int x = -reach; x <= reach; x++) {
            for (int z = -reach; z <= reach; z++) {
                for (int y = 1; y <= reach; y++) {
                    helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR);
                }
            }
        }
        ResearchTableBlockEntity table = stockedTable(helper);
        int[] none = table.inkSupply().capacity();
        helper.assertTrue(Arrays.stream(none).sum() == 0, "no tanks around: no ink and no storage (0/0)");
        BlockPos mixture = new BlockPos(0, 1, 2);
        BlockPos fire = new BlockPos(1, 1, 2);
        BlockPos farAway = new BlockPos(0, 1, JarReach.RADIUS + 1);
        BlockPos above = new BlockPos(-3, 4, 3);
        for (BlockPos pos : new BlockPos[]{mixture, fire, farAway, above}) {
            helper.setBlock(pos, ModBlocks.ESSENCE_TANK.get());
        }
        helper.<EssenceTankBlockEntity>getBlockEntity(mixture).tank().set(EssenceMix.of(java.util.Map.of(Essence.LIFE, 8, Essence.EARTH, 8)));
        helper.<EssenceTankBlockEntity>getBlockEntity(fire).tank().set(EssenceMix.pure(Essence.FIRE, 16));
        helper.<EssenceTankBlockEntity>getBlockEntity(farAway).tank().set(EssenceMix.pure(Essence.WATER, 16));
        helper.<EssenceTankBlockEntity>getBlockEntity(above).tank().set(EssenceMix.pure(Essence.AIR, 16));
        InkSupply supply = table.inkSupply();
        helper.assertTrue(supply.available()[Essence.FIRE.ordinal()] == 16
                && supply.capacity()[Essence.FIRE.ordinal()] == EssenceTankBlockEntity.CAPACITY, "a fire concentrate next to the desk is fire ink");
        helper.assertTrue(supply.available()[Essence.LIFE.ordinal()] == 0, "a mixture is no ink");
        helper.assertTrue(supply.capacity()[Essence.WATER.ordinal()] == 0, "a tank too far away does not count");
        helper.assertTrue(supply.available()[Essence.AIR.ordinal()] == 16, "a jar a few blocks off and up is in reach");
        supply.spend(ink(Essence.FIRE, 5));
        helper.assertTrue(helper.<EssenceTankBlockEntity>getBlockEntity(fire).tank().amount() == 11, "spent ink leaves the tank");
        helper.succeed();
    }

    @HexTest
    public static void aFinishedThreadWritesTheNotes(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        table.setItem(ResearchTableBlockEntity.PAPER, new ItemStack(Items.PAPER, 2));
        ResearchBoard board = ResearchBoards.get(TOPIC);
        int[] tree = ResearchBoards.plainTree(board);
        ResearchSheet.Trace trace = ResearchSheet.trace(board, tree);

        table.select(player, TOPIC);
        table.thread(player, tree);
        helper.assertTrue(table.sheet().length() == 0, "no drawing without ink");
        Map<Essence, EssenceTankBlockEntity> tanks = inkTanks(helper, EssenceTankBlockEntity.CAPACITY, board.inks());
        int[] part = Arrays.copyOf(tree, 4);
        table.thread(player, part);
        helper.assertTrue(table.sheet().length() == 2 && !table.sheet().isDone(), "a part of the drawing is kept");
        helper.assertTrue(tanks.get(board.essences().getFirst()).tank().amount() == EssenceTankBlockEntity.CAPACITY, "no ink is spent yet");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == 1, "the started attempt took a paper");
        table.thread(player, new int[0]);
        table.thread(player, part);
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == 1, "taking back and drawing again costs no new paper");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.OUTPUT).isEmpty(), "no notes before the research is finished");
        table.thread(player, tree);

        helper.assertTrue(table.sheet().isDone(), "the drawing finished the research");
        // The client is told which jars the ink came from: its renderer streams them into the orb.
        helper.assertTrue(!table.inkDraws().isEmpty() && table.inkDraws().stream().allMatch(draw -> draw.essence() != null),
                "the ink's jars are remembered for the stream");
        helper.assertTrue(table.getUpdateTag(helper.getLevel().registryAccess()).getIntArray("ink_draws").length > 0,
                "the ink's jars travel to the client");
        RuneKnowledge knowledge = Research.of(player);
        helper.assertFalse(knowledge.researched(TOPIC), "the topic is not known until its notes are read");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.HEXTECH).getCount() == 2, "the orb went into the research");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == 1, "finishing takes no more paper");
        for (Essence element : board.inks()) {
            helper.assertTrue(tanks.get(element).tank().amount()
                            == EssenceTankBlockEntity.CAPACITY - trace.ink()[element.ordinal()] * InkSupply.MB_PER_SECTION,
                    element.id() + " ink was spent from its tank by the drawing");
        }
        ItemStack notes = table.getItem(ResearchTableBlockEntity.OUTPUT);
        helper.assertTrue(ResearchNotesItem.topic(notes) == TOPIC, "the circle's research notes come out");
        ItemStack taken = table.removeItem(ResearchTableBlockEntity.OUTPUT, 1);
        helper.assertTrue(table.sheet().topic() == null && !table.sheet().isDone(), "taking the notes takes the drawing off the desk");
        helper.assertTrue(table.acceptsNotes(taken), "the notes may be laid back");
        table.setItem(ResearchTableBlockEntity.OUTPUT, taken);
        helper.assertTrue(table.sheet().isDone() && table.sheet().topic() == TOPIC && Arrays.equals(table.sheet().tree(), tree),
                "notes laid back show their drawing again");
        notes = table.getItem(ResearchTableBlockEntity.OUTPUT);
        player.setItemInHand(InteractionHand.MAIN_HAND, notes.copy());
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(knowledge.researched(TOPIC), "reading the notes teaches the topic");
        helper.assertTrue(knowledge.seen(TOPIC).get(tree[tree.length - 1]), "the parchment the ink passed is remembered");
        DiaryOwner owner = ResearchersDiaryItem.owner(table.getItem(ResearchTableBlockEntity.DIARY));
        helper.assertTrue(owner != null && owner.is(player), "the unsigned diary is signed by the researcher");
        player.discard();
        helper.succeed();
    }

    /**
     * Stepping on a curse at the table: the orb bursts, the sheet burns and the next attempt lies on a new board with
     * nothing of the old one remembered. (The blast's damage is not checked: a mock player counts as still loading and
     * takes none.)
     */
    @HexTest
    public static void aCurseBurstsTheOrbAtTheTable(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        table.setItem(ResearchTableBlockEntity.PAPER, new ItemStack(Items.PAPER, 2));
        ResearchBoard board = cursedBoard();
        inkTanks(helper, EssenceTankBlockEntity.CAPACITY, board.inks());
        openUpTo(Research.of(player), Guide.server().forTopic(board.topic()));
        int cursed = cursedCell(board);
        int[] tree = ResearchSheet.fromPath(pathTo(board, cursed));
        table.select(player, board.topic());
        table.thread(player, tree);
        helper.assertTrue(table.sheet().length() == 0 && !table.sheet().paid(), "the sheet burnt with its paper");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.HEXTECH).getCount() == 2, "the orb burst");
        helper.assertTrue(Research.of(player).attempt(board.topic()) == 1 && table.sheet().attempt() == 1, "the next attempt starts");
        helper.assertTrue(table.sheet().burns() == 1 && table.sheet().burnCell() == cursed, "the screen learns where it burned");
        helper.assertTrue(table.board() != board && !table.board().points().equals(board.points()), "a new board lies on the desk");
        helper.assertTrue(Research.of(player).seen(board.topic()).isEmpty(), "the burnt parchment is forgotten");
        player.discard();
        helper.succeed();
    }

    /** Every topic gets new, finishable boards for the attempts after a burn. */
    @HexTest
    public static void burntParchmentsGetNewBoards(GameTestHelper helper) {
        for (ResearchTopic topic : ResearchTopics.table()) {
            ResearchBoard first = ResearchBoards.get(topic);
            for (int attempt = 1; attempt <= 3; attempt++) {
                ResearchBoard next = ResearchBoards.get(topic, attempt);
                helper.assertTrue(!next.points().equals(first.points()), "attempt " + attempt + " of " + topic.id() + " is a new board");
                helper.assertTrue(ResearchBoards.plainTree(next) != null && ResearchBoards.deducible(next),
                        "attempt " + attempt + " of " + topic.id() + " can be finished without a guess");
            }
        }
        helper.succeed();
    }

    /** Burning the parchment on purpose: a paper burns (a blank sheet takes one from the table), the orb stays, a new board. */
    @HexTest
    public static void aParchmentCanBeBurntOnPurpose(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        table.setItem(ResearchTableBlockEntity.PAPER, new ItemStack(Items.PAPER, 4));
        table.select(player, TOPIC);
        int papers = table.getItem(ResearchTableBlockEntity.PAPER).getCount();
        int orbs = table.getItem(ResearchTableBlockEntity.HEXTECH).getCount();
        table.burn(player);
        helper.assertTrue(table.sheet().attempt() == 1 && table.sheet().burns() == 1, "the sheet burned and the next attempt lies on the desk");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == papers - 1, "a blank sheet costs a paper from the table");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.HEXTECH).getCount() == orbs, "the orb stays");
        helper.assertTrue(player.getHealth() == player.getMaxHealth(), "burning on purpose does not hurt");

        ResearchBoard next = ResearchBoards.get(TOPIC, 1);
        int[] plain = ResearchBoards.plainTree(next);
        inkTanks(helper, EssenceTankBlockEntity.CAPACITY, next.inks());
        table.thread(player, Arrays.copyOf(plain, 2));
        helper.assertTrue(table.sheet().paid() && table.getItem(ResearchTableBlockEntity.PAPER).getCount() == papers - 2, "the new attempt takes its paper");
        table.burn(player);
        helper.assertTrue(table.sheet().attempt() == 2 && table.sheet().length() == 0, "a drawn sheet burns too");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == papers - 2, "its own paper burns with it");
        player.discard();
        helper.succeed();
    }

    @HexTest
    public static void creativeResearchersStillNeedKnowledge(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        player.setGameMode(GameType.CREATIVE);
        player.getInventory().clearContent();
        ResearchTableBlockEntity table = stockedTable(helper);
        table.select(player, TOPIC);
        table.thread(player, ResearchBoards.plainTree(ResearchBoards.get(TOPIC)));
        helper.assertFalse(table.sheet().isDone(), "creative mode gives no ink: knowledge scrolls give progress instead");
        ItemStack pickaxe = new ItemStack(ModItems.pickaxe(com.pockyl.hextale.runework.HextechPickaxe.HEXTECH).get());
        helper.assertFalse(ResearchTopics.mayMake(player, pickaxe, Research.of(player)), "nor any research");
        player.discard();
        helper.succeed();
    }

    @HexTest
    public static void missingMaterialsAreNamed(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ResearchTableBlock.place(helper.getLevel(), helper.absolutePos(TABLE), ModBlocks.RESEARCH_TABLE.get().defaultBlockState());
        ResearchTableBlockEntity table = helper.<ResearchTableBlockEntity>getBlockEntity(TABLE);
        helper.assertTrue(Requirement.firstMissing(table, player) == Requirement.DIARY, "an empty table asks for the diary first");

        ItemStack foreign = new ItemStack(ModItems.RESEARCHERS_DIARY.get());
        foreign.set(ModDataComponents.DIARY_OWNER.get(), new DiaryOwner(UUID.randomUUID(), "Someone"));
        table.setItem(ResearchTableBlockEntity.DIARY, foreign);
        helper.assertTrue(Requirement.firstMissing(table, player) == Requirement.OWN_DIARY, "someone else's diary does not count");
        table.select(player, TOPIC);
        helper.assertTrue(table.sheet().topic() == null, "nothing is researched with someone else's diary");

        table.setItem(ResearchTableBlockEntity.DIARY, new ItemStack(ModItems.RESEARCHERS_DIARY.get()));
        helper.assertTrue(Requirement.firstMissing(table, player) == Requirement.HEXTECH, "then the orb");
        table.setItem(ResearchTableBlockEntity.HEXTECH, new ItemStack(ModItems.HEXTECH_DUST.get()));
        helper.assertTrue(Requirement.firstMissing(table, player) == Requirement.PAPER, "then paper on the table");
        player.getInventory().add(new ItemStack(Items.PAPER));
        helper.assertTrue(Requirement.firstMissing(table, player) == Requirement.PAPER, "paper in the pocket does not count");
        table.select(player, TOPIC);
        inkTanks(helper, EssenceTankBlockEntity.CAPACITY, ResearchBoards.get(TOPIC).inks());
        table.thread(player, Arrays.copyOf(ResearchBoards.plainTree(ResearchBoards.get(TOPIC)), 4));
        helper.assertTrue(table.sheet().length() == 0, "no drawing without paper");
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

    @HexTest(maxTicks = 60)
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

    @HexTest
    public static void knowledgeSurvivesDeath(GameTestHelper helper) {
        ServerPlayer before = player(helper);
        Research.learn(before, ComplexRunes.byKey("water_life"), 3);
        Research.learn(before, ComplexRunes.byKey("life_metal"), 1);
        Research.research(before, TOPIC);
        Research.of(before).study(ResourceLocation.withDefaultNamespace("coal"));
        Research.of(before).see(TOPIC, new int[]{HexGrid.CENTRE, 5});

        ServerPlayer after = player(helper);
        after.restoreFrom(before, false);

        RuneKnowledge knowledge = Research.of(after);
        helper.assertTrue(knowledge.runeCount() == 2 && knowledge.stars(ComplexRunes.byKey("water_life")) == 3, "the runes and stars are kept");
        helper.assertTrue(knowledge.researched(TOPIC), "researched topics are kept");
        helper.assertTrue(knowledge.hasStudied(ResourceLocation.withDefaultNamespace("coal")), "studied items are kept");
        helper.assertTrue(knowledge.seen(TOPIC).get(5), "explored parchment is kept");
        helper.assertTrue(knowledge != Research.of(before), "the new player has its own copy");
        RuneKnowledge decoded = RuneKnowledge.CODEC.parse(NbtOps.INSTANCE,
                RuneKnowledge.CODEC.encodeStart(NbtOps.INSTANCE, knowledge).getOrThrow()).getOrThrow();
        helper.assertTrue(decoded.stars(ComplexRunes.byKey("life_metal")) == 1 && decoded.seen(TOPIC).get(5), "knowledge saves and loads");
        before.discard();
        after.discard();
        helper.succeed();
    }

    @HexTest
    public static void starsOnlyGetBetter(GameTestHelper helper) {
        RuneKnowledge knowledge = new RuneKnowledge();
        helper.assertTrue(knowledge.learn(SMELTING, 2), "the first scroll teaches the rune");
        helper.assertFalse(knowledge.learn(SMELTING, 1), "a later scroll is not a new rune");
        helper.assertTrue(knowledge.stars(SMELTING) == 2, "a worse scroll changes nothing");
        knowledge.learn(SMELTING, 3);
        helper.assertTrue(knowledge.stars(SMELTING) == 3, "a better scroll raises the stars");
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

    /** Runes are found in chests: a plain dungeon's chest holds chapter I scrolls now and then, stars as luck has it. */
    @HexTest
    public static void runeScrollsLieInChests(GameTestHelper helper) {
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(7);
        int[] stars = new int[4];
        for (int i = 0; i < 300; i++) {
            ItemStack scroll = com.pockyl.hextale.loot.RuneScrollLoot.roll(random, List.of(1, 0, 0), List.of(60, 30, 10));
            ComplexRune rune = RuneScrollItem.rune(scroll);
            helper.assertTrue(rune != null && ResearchBook.chapter(rune) == 1, "a chapter I scroll, got " + rune);
            stars[RuneScrollItem.stars(scroll)]++;
        }
        helper.assertTrue(stars[1] > stars[2] && stars[2] > stars[3] && stars[3] > 0, "one star is common, three rare: " + Arrays.toString(stars));
        ServerLevel level = helper.getLevel();
        var table = level.getServer().reloadableRegistries().getLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON);
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, helper.absoluteVec(Vec3.ZERO))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        int found = 0;
        for (int i = 0; i < 200; i++) {
            for (ItemStack stack : table.getRandomItems(params, i)) {
                if (stack.is(ModItems.RUNE_SCROLL.get())) {
                    found++;
                }
            }
        }
        helper.assertTrue(found > 20 && found < 120, "about a third of dungeon chests hold a scroll, got " + found + " of 200");
        helper.succeed();
    }

    /** Devices past the essence works are made only once researched; plain materials need nothing. */
    @HexTest
    public static void unresearchedDevicesCannotBeMade(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        RuneKnowledge knowledge = Research.of(player);
        ItemStack pickaxe = new ItemStack(ModItems.pickaxe(com.pockyl.hextale.runework.HextechPickaxe.HEXTECH).get());
        helper.assertFalse(ResearchTopics.mayMake(player, pickaxe, knowledge), "the pickaxe waits for its research");
        helper.assertFalse(ResearchTopics.mayMake(player, new ItemStack(ModItems.PEDESTAL.get()), knowledge), "the pedestal for the circle's");
        helper.assertTrue(ResearchTopics.mayMake(player, new ItemStack(ModItems.ALEMBIC.get()), knowledge), "the alembic needs no research");
        helper.assertTrue(ResearchTopics.mayMake(player, new ItemStack(ModItems.ESSENCE_TANK.get()), knowledge),
                "the jar itself needs none (only the big jar does)");
        Research.research(player, ResearchTopics.CIRCLE);
        helper.assertTrue(ResearchTopics.mayMake(player, new ItemStack(ModItems.PEDESTAL.get()), knowledge), "one research opens the whole circle");
        ItemStack notes = ResearchNotesItem.of(ResearchTopics.HEXTECH_PICKAXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, notes);
        notes.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(ResearchTopics.mayMake(player, pickaxe, knowledge) && player.getMainHandItem().isEmpty(), "read notes teach the pickaxe");
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
        helper.assertTrue(Research.of(reader).stars(lens) == 2, "studying the scroll teaches the rune with its stars");
        helper.assertTrue(reader.getMainHandItem().getCount() == 2, "one scroll is used up");
        scroll.use(helper.getLevel(), reader, InteractionHand.MAIN_HAND);
        helper.assertTrue(reader.getMainHandItem().getCount() == 2, "a scroll that teaches nothing new is kept");
        reader.discard();
        helper.succeed();
    }

    // ---- Helpers --------------------------------------------------------------------------------------------------

    /** The shortest open way from the orb to a section, around dark runes; the orb itself when unreachable. */
    /** A deeper topic's board (the first one with a reachable curse). */
    private static ResearchBoard cursedBoard() {
        for (ResearchTopic topic : ResearchTopics.table()) {
            ResearchBoard board = ResearchBoards.get(topic);
            if (topic.tier() >= 2 && cursedCell(board) >= 0) {
                return board;
            }
        }
        throw new IllegalStateException("No deeper board has a reachable curse");
    }

    /** A cursed section reachable from the orb over safe parchment, or -1. */
    private static int cursedCell(ResearchBoard board) {
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            if (board.cursed(cell) && pathTo(board, cell).length > 1) {
                return cell;
            }
        }
        return -1;
    }

    /** The shortest path from the orb to the target over parchment that is neither stone nor cursed (but the target). */
    private static int[] pathTo(ResearchBoard board, int target) {
        int[] previous = new int[HexGrid.COUNT];
        Arrays.fill(previous, -2);
        previous[HexGrid.CENTRE] = -1;
        java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>(List.of(HexGrid.CENTRE));
        while (!queue.isEmpty()) {
            int cell = queue.poll();
            for (int next : HexGrid.neighbours(cell)) {
                if (previous[next] == -2 && !board.wall(next) && (!board.cursed(next) || next == target)) {
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
        ResearchTableBlockEntity table = helper.<ResearchTableBlockEntity>getBlockEntity(TABLE);
        table.setItem(ResearchTableBlockEntity.DIARY, new ItemStack(ModItems.RESEARCHERS_DIARY.get()));
        table.setItem(ResearchTableBlockEntity.HEXTECH, new ItemStack(ModItems.UNSTABLE_HEXTECH.get(), 3));
        return table;
    }

    /** An Essence Tank of each of these essences' concentrate next to the test table, {@code drops} in each. */
    private static Map<Essence, EssenceTankBlockEntity> inkTanks(GameTestHelper helper, int drops, List<Essence> essences) {
        Map<Essence, EssenceTankBlockEntity> tanks = new java.util.EnumMap<>(Essence.class);
        Essence[] elements = essences.toArray(new Essence[0]);
        for (int i = 0; i < elements.length; i++) {
            BlockPos pos = new BlockPos(-2 + i % 5, TABLE.getY() + i / 5, TABLE.getZ() + 2);
            helper.setBlock(pos, ModBlocks.ESSENCE_TANK.get());
            EssenceTankBlockEntity tank = helper.<EssenceTankBlockEntity>getBlockEntity(pos);
            tank.tank().set(EssenceMix.pure(elements[i], drops));
            tanks.put(elements[i], tank);
        }
        return tanks;
    }

    private static int[] ink(Essence element, int drops) {
        int[] ink = new int[Essence.COUNT];
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
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);
        // A researcher who got as far as ink at the table: the circle's research is open.
        openUpTo(Research.of(player), Guide.server().forTopic(TOPIC));
        return player;
    }

    /** Marks every step before this one done, so the step is open (the step itself stays to do). */
    private static void openUpTo(com.pockyl.hextale.knowledge.RuneKnowledge knowledge, com.pockyl.hextale.quest.Quest quest) {
        for (com.pockyl.hextale.quest.Quest parent : quest.parents()) {
            knowledge.completeQuest(parent);
            openUpTo(knowledge, parent);
        }
    }
}
