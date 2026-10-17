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
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.HexGrid;
import com.pockyl.hextale.research.ItemEssenceResolver;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.ItemStudy;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.research.ResearchSheet;
import com.pockyl.hextale.research.RuneBoard;
import com.pockyl.hextale.research.RuneBoards;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Arrays;
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
            helper.assertTrue(board.essences().size() == 6 && new java.util.HashSet<>(board.essences()).size() == 6,
                    rune.key() + ": six different essences around the hexagon");
            for (Essence essence : board.essences()) {
                helper.assertTrue(essence.share(rune.action()) + essence.share(rune.target()) > 0,
                        rune.key() + ": " + essence.id() + " belongs to the rune's theme");
            }
            helper.assertTrue(board.essences().stream().anyMatch(e -> e.share(rune.action()) > 0)
                    && board.essences().stream().anyMatch(e -> e.share(rune.target()) > 0), rune.key() + ": both halves of the rune are there");
            for (RuneBoard.Point point : board.points()) {
                helper.assertTrue(board.essences().contains(point.element()), rune.key() + ": points carry essences of the parchment");
            }
            helper.assertTrue(par.three() >= 1 && par.three() == par.plain() && par.two() > par.three(), rune.key() + ": sane thresholds " + par);
            helper.assertTrue(!board.wall(HexGrid.CENTRE) && !board.dark(HexGrid.CENTRE), rune.key() + ": the orb is never walled in");
            int darkCount = 0;
            for (int cell = 0; cell < HexGrid.COUNT; cell++) {
                if (board.dark(cell)) {
                    darkCount++;
                    helper.assertTrue(HexGrid.steps(HexGrid.CENTRE, cell) > 1, rune.key() + ": the orb's neighbours are never dark");
                    helper.assertTrue(board.pointAt(cell) < 0, rune.key() + ": no point lies in a dark field");
                }
                helper.assertTrue(!board.cursed(cell) || board.dark(cell), rune.key() + ": only dark sections are cursed");
            }
            helper.assertTrue(tier == 1 ? darkCount == 0 : darkCount > 0, rune.key() + ": dark fields only after chapter I");
            int[] tree = RuneBoards.plainTree(board);
            helper.assertTrue(tree != null, rune.key() + ": a plain drawing exists");
            ResearchSheet.Trace trace = ResearchSheet.trace(board, tree);
            helper.assertTrue(trace != null && trace.complete(board), rune.key() + ": the plain drawing follows the rules and reaches every point");
            helper.assertTrue(tree.length / 2 == par.plain(), rune.key() + ": the thresholds come from that drawing");
            lengths[tier][0] = Math.min(lengths[tier][0], par.plain());
            lengths[tier][1] = Math.max(lengths[tier][1], par.plain());
        }
        StringBuilder themes = new StringBuilder();
        for (String key : List.of("fire_earth", "earth_fire", "water_light", "air_metal", "life_shadow", "shadow_spark", "spark_life")) {
            RuneBoard board = RuneBoards.get(ComplexRunes.byKey(key));
            themes.append(key).append('=').append(board.essences()).append("; ");
        }
        Hextale.LOGGER.info("Board themes: {}", themes);
        Hextale.LOGGER.info("Checked {} research boards in {} ms; plain drawings by chapter: I {}-{}, II {}-{}, III {}-{}",
                ComplexRunes.COUNT, (System.nanoTime() - start) / 1_000_000, lengths[1][0], lengths[1][1], lengths[2][0], lengths[2][1],
                lengths[3][0], lengths[3][1]);
        helper.succeed();
    }

    @HexTest
    public static void theInkFollowsTheRules(GameTestHelper helper) {
        RuneBoard board = RuneBoards.get(SMELTING);
        int[] plain = RuneBoards.plainTree(board);
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
        RuneBoard board = cursedBoard();
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
        RuneBoard board = RuneBoards.get(SMELTING);
        int[] tree = RuneBoards.plainTree(board);
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
        helper.assertTrue(fromCentre.size() == 6, "the six steps from the orb pay six different inks, got " + fromCentre);
        helper.assertTrue(anywhere.equals(new java.util.HashSet<>(board.essences())), "the six inks are the parchment's six essences");
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
            helper.assertTrue(!elements.essences().isEmpty() && elements.essences().size() <= 6, item + ": one to six essences");
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
        Hextale.LOGGER.info("Items by number of essences 1-6: {} {} {} {} {} {}", sizes[1], sizes[2], sizes[3], sizes[4], sizes[5], sizes[6]);
        for (Map.Entry<Item, ItemEssences> entry : ResearchItems.canonical().entrySet()) {
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

    // ---- The prism ------------------------------------------------------------------------------------------------

    /** A port whose ray runs straight towards the centre for at least three sections. */
    private static int straightPort() {
        for (int i = 0; i < ItemStudy.portCount(); i++) {
            int port = ItemStudy.port(i);
            int d = ItemStudy.portDirection(port);
            int cell = ItemStudy.portTarget(port);
            boolean inward = true;
            for (int k = 0; k < 3 && inward; k++) {
                int next = HexGrid.step(cell, d);
                inward = HexGrid.steps(next, HexGrid.CENTRE) < HexGrid.steps(cell, HexGrid.CENTRE);
                cell = next;
            }
            if (inward) {
                return port;
            }
        }
        throw new IllegalStateException("no straight port");
    }

    private static ItemStudy.Body light(int cell) {
        return new ItemStudy.Body(Essence.FIRE, 3, cell, false);
    }

    @HexTest
    public static void prismRaysFollowTheRules(GameTestHelper helper) {
        helper.assertTrue(ItemStudy.portCount() == 6 + 6 * (ItemStudy.RIM - 1) * 2, "every rim section has its ports, got " + ItemStudy.portCount());
        int port = straightPort();
        int d = ItemStudy.portDirection(port);
        int t1 = ItemStudy.portTarget(port);
        int t2 = HexGrid.step(t1, d);
        int t3 = HexGrid.step(t2, d);

        ItemStudy.Ray free = ItemStudy.trace(List.of(), port);
        helper.assertTrue(free.outcome() == ItemStudy.Outcome.EXIT && free.exit() >= 0 && free.touched() == 0, "an empty field lets a ray through");
        for (int k = 1; k < free.path().length; k++) {
            helper.assertTrue(HexGrid.direction(free.path()[k - 1], free.path()[k]) == d, "and it runs straight");
        }
        helper.assertTrue(ItemStudy.trace(List.of(), free.exit()).exit() == port, "a ray from the far end comes back the same way");

        ItemStudy.Ray hit = ItemStudy.trace(List.of(light(t3)), port);
        helper.assertTrue(hit.outcome() == ItemStudy.Outcome.HIT && hit.path()[hit.path().length - 1] == t3 && hit.touched() == 1,
                "a body in the way absorbs the ray");

        ItemStudy.Ray rim = ItemStudy.trace(List.of(light(HexGrid.step(t1, (d + 1) % 6))), port);
        helper.assertTrue(rim.outcome() == ItemStudy.Outcome.REFLECT, "a body beside the first section reflects the ray at the rim");

        ItemStudy.Ray bent = ItemStudy.trace(List.of(light(HexGrid.step(t3, (d + 1) % 6))), port);
        helper.assertTrue(bent.outcome() == ItemStudy.Outcome.EXIT && bent.touched() == 1 && bent.exit() != free.exit(), "a body beside the path bends it");
        helper.assertTrue(bent.path()[2] == t2 && bent.path()[3] == HexGrid.step(t2, (d + 5) % 6), "away from the body, by one direction");

        ItemStudy.Ray back = ItemStudy.trace(List.of(light(HexGrid.step(t3, (d + 1) % 6)), light(HexGrid.step(t3, (d + 5) % 6))), port);
        helper.assertTrue(back.outcome() == ItemStudy.Outcome.REFLECT && back.touched() == 3, "bodies on both sides turn the ray back");

        ItemStudy.Body big = new ItemStudy.Body(Essence.EARTH, 20, t3, true);
        ItemStudy.Ray heavy = ItemStudy.trace(List.of(big), port);
        helper.assertTrue(heavy.outcome() == ItemStudy.Outcome.HIT && heavy.path()[heavy.path().length - 1] == t2, "a strong essence fills seven sections");
        helper.succeed();
    }

    /** Every item's essences fit into the prism, and every body shows itself to some ray. */
    @HexTest(maxTicks = 600)
    public static void prismBodiesFitEveryItem(GameTestHelper helper) {
        int items = 0;
        int hidden = 0;
        int[] budget = new int[ItemStudy.MAX_BODIES + 1];
        for (Item item : BuiltInRegistries.ITEM) {
            ItemEssences essences = ResearchItems.of(item);
            if (essences == null) {
                continue;
            }
            for (int seed = 0; seed < 3; seed++) {
                List<ItemStudy.Body> bodies = ItemStudy.place(essences, item.hashCode() + seed);
                helper.assertTrue(bodies.size() == Math.min(ItemStudy.MAX_BODIES, essences.essences().size()), item + ": every essence gets a body");
                helper.assertTrue(bodies.equals(ItemStudy.place(essences, item.hashCode() + seed)), item + ": the same seed, the same bodies");
                int seen = 0;
                for (int b = 0; b < bodies.size(); b++) {
                    for (int cell : bodies.get(b).cells()) {
                        helper.assertTrue(ItemStudy.inField(cell), item + ": bodies lie in the field");
                        helper.assertTrue(ItemStudy.bodyAt(bodies, cell) == b, item + ": bodies never overlap");
                    }
                }
                for (int p = 0; p < ItemStudy.portCount(); p++) {
                    ItemStudy.Ray ray = ItemStudy.trace(bodies, ItemStudy.port(p));
                    seen |= ray.touched();
                    for (int cell : ray.empty()) {
                        helper.assertTrue(ItemStudy.bodyAt(bodies, cell) < 0, item + ": a section a ray proves empty holds no essence");
                    }
                }
                hidden += bodies.size() - Integer.bitCount(seen);
                items++;
                budget[bodies.size()] = ItemStudy.maxRays(bodies);
            }
        }
        Hextale.LOGGER.info("Prism: {} placements, {} bodies no ray touches; rays by essences 1-6: {}", items, hidden,
                Arrays.toString(Arrays.copyOfRange(budget, 1, budget.length)));
        helper.assertTrue(hidden <= items / 100, "nearly every body shows itself to some ray, " + hidden + " hidden in " + items);
        helper.succeed();
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
        table.fire(player, ItemStudy.port(0));
        helper.assertTrue(table.study().rays().size() == 1, "a ray was shone");
        for (ItemStudy.Body body : table.study().bodies()) {
            table.strike(player, body.centre());
        }
        Identifier glass = BuiltInRegistries.ITEM.getKey(Items.GLASS);
        helper.assertTrue(Research.of(player).hasStudied(glass), "glass is studied");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.STUDY).getCount() == 2, "one glass went into the study");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == 1, "the study took one paper from the table");
        table.closeStudy(player);
        helper.assertFalse(table.isStudyingItem(), "a studied item is not studied again");

        // A lost study: empty checks until the light runs out; the item is used once.
        table.setItem(ResearchTableBlockEntity.STUDY, new ItemStack(Items.FEATHER, 2));
        table.studySlotChanged(player);
        for (int cell = 0; cell < HexGrid.COUNT && table.study().isActive(); cell++) {
            if (ItemStudy.inField(cell) && ItemStudy.bodyAt(table.study().bodies(), cell) < 0) {
                table.strike(player, cell);
            }
        }
        helper.assertTrue(table.study().lost(), "the light ran out");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.STUDY).getCount() == 1, "the study used one feather");
        helper.assertFalse(Research.of(player).hasStudied(BuiltInRegistries.ITEM.getKey(Items.FEATHER)), "a lost study teaches nothing");
        table.closeStudy(player);
        helper.assertTrue(table.isStudyingItem() && !table.study().started(), "a click on a lost study tries the next feather");
        player.discard();
        helper.succeed();
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
        AlembicBlockEntity alembic = helper.getBlockEntity(at, AlembicBlockEntity.class);
        alembic.setItem(AlembicBlockEntity.INPUT, new ItemStack(Items.OAK_PLANKS));
        ServerPlayer boiler = player(helper);
        boiler.setGameMode(GameType.SURVIVAL);
        Research.of(boiler).study(Identifier.withDefaultNamespace("oak_planks"));
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
        AlembicBlockEntity alembic = helper.getBlockEntity(at, AlembicBlockEntity.class);
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
        AlembicBlockEntity alembic = helper.getBlockEntity(at, AlembicBlockEntity.class);
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
        SeparatorBlockEntity separator = helper.getBlockEntity(at, SeparatorBlockEntity.class);
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

    @HexTest(maxTicks = 300)
    public static void withoutAChosenEssenceNothingIsSplit(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.SEPARATOR.get().defaultBlockState());
        SeparatorBlockEntity separator = helper.getBlockEntity(at, SeparatorBlockEntity.class);
        separator.setItem(SeparatorBlockEntity.POUR, EssenceFlaskItem.filled(EssenceMix.of(java.util.Map.of(Essence.LIFE, 8, Essence.EARTH, 8))));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(separator.mix().amount() == 16 && separator.rest().isEmpty(), "the mixture waits for a chosen essence");
            helper.assertTrue("no_essence".equals(separator.blocked()), "and the screen says so");
            separator.choose(Essence.EARTH);
            helper.assertTrue("no_fuel".equals(separator.blocked()), "then for blaze powder");
            separator.choose(Essence.AIR);
            separator.setItem(SeparatorBlockEntity.FUEL, new ItemStack(Items.BLAZE_POWDER));
            helper.runAfterDelay(100, () -> {
                // Air is chosen; the mixture has none, so all of it passes on as the rest - bottled in the glass
                // bottle the poured one left.
                EssenceMix rest = EssenceFlaskItem.contents(separator.getItem(SeparatorBlockEntity.REST_OUT));
                helper.assertTrue(separator.mix().isEmpty() && rest.total() == 16 && separator.extract().isEmpty(),
                        "a mixture without the chosen essence passes through, got rest " + rest);
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
        EssenceTankBlockEntity source = helper.getBlockEntity(at.south(2).above(), EssenceTankBlockEntity.class);
        EssenceTankBlockEntity sink = helper.getBlockEntity(at.north(2), EssenceTankBlockEntity.class);
        SeparatorBlockEntity separator = helper.getBlockEntity(at, SeparatorBlockEntity.class);
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
        separator.setItem(SeparatorBlockEntity.POUR, EssenceFlaskItem.filled(EssenceMix.of(java.util.Map.of(Essence.EARTH, 8, Essence.AIR, 8))));
        separator.setItem(SeparatorBlockEntity.FUEL, new ItemStack(Items.BLAZE_POWDER, 1));
        separator.choose(Essence.EARTH);
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(separator.mix().isEmpty(), "the mixture was worked off, left " + separator.mix().contents());
            helper.assertTrue(separator.rest().contents().get(Essence.AIR) == 8 && separator.rest().contents().isUnknown(),
                    "the air stays in the separator, got rest " + separator.rest().contents() + ", tank " + tank.tank().contents());
            helper.assertTrue(tank.tank().isEmpty(), "the tank feeding back into the same pipes takes nothing");
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
        EssenceTankBlockEntity back = helper.getBlockEntity(at.south(), EssenceTankBlockEntity.class);
        EssenceTankBlockEntity piped = helper.getBlockEntity(at.east(2), EssenceTankBlockEntity.class);
        back.tank().set(EssenceMix.pure(Essence.LIFE, 8));
        piped.tank().set(EssenceMix.pure(Essence.EARTH, 8));
        SeparatorBlockEntity separator = helper.getBlockEntity(at, SeparatorBlockEntity.class);
        helper.succeedWhen(() -> {
            helper.assertTrue(back.tank().isEmpty() && piped.tank().isEmpty(), "both tanks were drawn empty");
            helper.assertTrue(separator.mix().amount() == 16, "into the separator's mixture, got " + separator.mix().contents());
        });
    }

    @HexTest(maxTicks = 60)
    public static void aCreativeTankNeverRunsDry(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.CREATIVE_ESSENCE_TANK.get());
        EssenceTankBlockEntity tank = helper.getBlockEntity(at, EssenceTankBlockEntity.class);
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
        EssenceTankBlockEntity tank = helper.getBlockEntity(at, EssenceTankBlockEntity.class);
        tank.tank().set(EssenceMix.pure(Essence.LIGHT, 40));
        ItemStack item = new ItemStack(ModItems.ESSENCE_TANK.get());
        item.applyComponents(tank.collectComponents());
        helper.assertTrue(EssenceMix.pure(Essence.LIGHT, 40).equals(item.get(ModDataComponents.ESSENCE.get())), "the broken tank's item keeps its light");
        helper.setBlock(at.east(), ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity placed = helper.getBlockEntity(at.east(), EssenceTankBlockEntity.class);
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
        EssenceTankBlockEntity beside = helper.getBlockEntity(valveAt.west(), EssenceTankBlockEntity.class);
        EssenceTankBlockEntity piped = helper.getBlockEntity(valveAt.east(2), EssenceTankBlockEntity.class);
        beside.tank().set(EssenceMix.pure(Essence.FIRE, 40));
        piped.tank().set(EssenceMix.unknown(40));
        helper.assertTrue(helper.getBlockState(valveAt).getValue(EssenceMachine.PORTS.get(Direction.WEST)), "the valve shows its joint to the tank beside it");
        helper.succeedWhen(() -> helper.assertTrue(beside.tank().isEmpty() && piped.tank().isEmpty(), "the open valve drains both tanks"));
    }

    /** Sneak-use stickers cycle through every essence and back to none, and stay on their face. */
    @HexTest
    public static void tankLabelsCycleThroughTheEssences(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 2, 1);
        helper.setBlock(at, ModBlocks.ESSENCE_TANK.get());
        EssenceTankBlockEntity tank = helper.getBlockEntity(at, EssenceTankBlockEntity.class);
        helper.assertTrue(tank.label(Direction.NORTH) == null, "a new tank has no stickers");
        for (Essence essence : Essence.values()) {
            tank.cycleLabel(Direction.NORTH);
            helper.assertTrue(tank.label(Direction.NORTH) == essence, "the next click shows " + essence);
        }
        tank.cycleLabel(Direction.NORTH);
        helper.assertTrue(tank.label(Direction.NORTH) == null, "after lightning the sticker comes off");
        tank.cycleLabel(Direction.EAST);
        helper.assertTrue(tank.label(Direction.EAST) == Essence.FIRE && tank.label(Direction.WEST) == null, "each face has its own sticker");
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
        helper.assertTrue(helper.getBlockState(at.above()).getValue(BrassPipeBlock.SIDES.get(Direction.DOWN)),
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
        helper.assertTrue(helper.getBlockState(at.east()).getValue(BrassPipeBlock.SIDES.get(Direction.WEST)), "and the pipe joins it");
        helper.assertFalse(helper.getBlockState(at.below()).getValue(BrassPipeBlock.SIDES.get(Direction.UP)),
                "the bottom is no port: the pipe below stays apart");
        // Facing north, the east face is the separator's left: make it a concentrate outlet.
        SeparatorBlockEntity entity = helper.getBlockEntity(at, SeparatorBlockEntity.class);
        entity.cycle(SeparatorBlock.Side.LEFT);
        helper.assertTrue(helper.getBlockState(at).getValue(SeparatorBlock.LOOKS.get(Direction.EAST)) == PortMode.PRODUCT,
                "the joint follows the face's new mode");
        helper.assertTrue(entity.receive(Direction.EAST, EssenceMix.pure(Essence.FIRE, 4), true) == 0, "an outlet takes nothing in");
        // Rest out, then closed: the pipe lets go of it.
        entity.cycle(SeparatorBlock.Side.LEFT);
        entity.cycle(SeparatorBlock.Side.LEFT);
        helper.assertTrue(helper.getBlockState(at).getValue(SeparatorBlock.LOOKS.get(Direction.EAST)) == PortMode.NONE,
                "a closed side shows no joint");
        helper.assertFalse(helper.getBlockState(at.east()).getValue(BrassPipeBlock.SIDES.get(Direction.WEST)),
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
        EssenceTankBlockEntity source = helper.getBlockEntity(from, EssenceTankBlockEntity.class);
        EssenceTankBlockEntity target = helper.getBlockEntity(to, EssenceTankBlockEntity.class);
        source.tank().set(EssenceMix.pure(Essence.WATER, 12));
        helper.succeedWhen(() -> {
            helper.assertTrue(target.tank().contents().equals(EssenceMix.pure(Essence.WATER, 12)),
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
        BlockPos farAway = new BlockPos(0, 1, JarReach.RADIUS + 1);
        BlockPos above = new BlockPos(-3, 4, 3);
        for (BlockPos pos : new BlockPos[]{mixture, fire, farAway, above}) {
            helper.setBlock(pos, ModBlocks.ESSENCE_TANK.get());
        }
        helper.getBlockEntity(mixture, EssenceTankBlockEntity.class).tank().set(EssenceMix.of(java.util.Map.of(Essence.LIFE, 8, Essence.EARTH, 8)));
        helper.getBlockEntity(fire, EssenceTankBlockEntity.class).tank().set(EssenceMix.pure(Essence.FIRE, 16));
        helper.getBlockEntity(farAway, EssenceTankBlockEntity.class).tank().set(EssenceMix.pure(Essence.WATER, 16));
        helper.getBlockEntity(above, EssenceTankBlockEntity.class).tank().set(EssenceMix.pure(Essence.AIR, 16));
        InkSupply supply = table.inkSupply();
        helper.assertTrue(supply.available()[Essence.FIRE.ordinal()] == 16
                && supply.capacity()[Essence.FIRE.ordinal()] == EssenceTankBlockEntity.CAPACITY, "a fire concentrate next to the desk is fire ink");
        helper.assertTrue(supply.available()[Essence.LIFE.ordinal()] == 0, "a mixture is no ink");
        helper.assertTrue(supply.capacity()[Essence.WATER.ordinal()] == 0, "a tank too far away does not count");
        helper.assertTrue(supply.available()[Essence.AIR.ordinal()] == 16, "a jar a few blocks off and up is in reach");
        supply.spend(ink(Essence.FIRE, 5));
        helper.assertTrue(helper.getBlockEntity(fire, EssenceTankBlockEntity.class).tank().amount() == 11, "spent ink leaves the tank");
        helper.succeed();
    }

    @HexTest
    public static void aFinishedThreadTeachesTheRune(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        table.setItem(ResearchTableBlockEntity.PAPER, new ItemStack(Items.PAPER, 2));
        RuneBoard board = RuneBoards.get(SMELTING);
        int[] tree = RuneBoards.plainTree(board);
        ResearchSheet.Trace trace = ResearchSheet.trace(board, tree);

        table.select(player, SMELTING);
        table.thread(player, tree);
        helper.assertTrue(table.sheet().length() == 0, "no drawing without ink");
        Map<Essence, EssenceTankBlockEntity> tanks = inkTanks(helper, EssenceTankBlockEntity.CAPACITY, board.essences());
        int[] part = Arrays.copyOf(tree, 4);
        table.thread(player, part);
        helper.assertTrue(table.sheet().length() == 2 && !table.sheet().isDone(), "a part of the drawing is kept");
        helper.assertTrue(tanks.get(SMELTING.action()).tank().amount() == EssenceTankBlockEntity.CAPACITY, "no ink is spent yet");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == 1, "the started attempt took a paper");
        table.thread(player, new int[0]);
        table.thread(player, part);
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == 1, "taking back and drawing again costs no new paper");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.OUTPUT).isEmpty(), "no scroll before the rune is finished");
        table.thread(player, tree);

        helper.assertTrue(table.sheet().isDone(), "the drawing bound the rune");
        int stars = board.stars(tree.length / 2);
        RuneKnowledge knowledge = Research.of(player);
        helper.assertFalse(knowledge.knows(SMELTING), "the rune is not learned until its scroll is studied");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.HEXTECH).getCount() == 2, "the orb went into the rune");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.PAPER).getCount() == 1, "finishing takes no more paper");
        for (Essence element : board.essences()) {
            helper.assertTrue(tanks.get(element).tank().amount()
                            == EssenceTankBlockEntity.CAPACITY - trace.ink()[element.ordinal()] * InkSupply.MB_PER_SECTION,
                    element.id() + " ink was spent from its tank by the drawing");
        }
        ItemStack scroll = table.getItem(ResearchTableBlockEntity.OUTPUT);
        helper.assertTrue(RuneScrollItem.rune(scroll) == SMELTING && RuneScrollItem.stars(scroll) == 3 && stars == 3,
                "a three-star scroll of Smelting comes out");
        player.setItemInHand(InteractionHand.MAIN_HAND, scroll.copy());
        player.getMainHandItem().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(knowledge.stars(SMELTING) == 3, "studying the scroll teaches Smelting with its three stars");
        helper.assertTrue(knowledge.seen(SMELTING).get(tree[tree.length - 1]), "the parchment the ink passed is remembered");
        DiaryOwner owner = ResearchersDiaryItem.owner(table.getItem(ResearchTableBlockEntity.DIARY));
        helper.assertTrue(owner != null && owner.is(player), "the unsigned diary is signed by the researcher");
        player.discard();
        helper.succeed();
    }

    /**
     * Stepping on a curse at the table: the orb bursts, the sheet burns and the researcher remembers it. (The blast's
     * damage is not checked: a mock player counts as still loading and takes none.)
     */
    @HexTest
    public static void aCurseBurstsTheOrbAtTheTable(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ResearchTableBlockEntity table = stockedTable(helper);
        table.setItem(ResearchTableBlockEntity.PAPER, new ItemStack(Items.PAPER, 2));
        RuneBoard board = cursedBoard();
        inkTanks(helper, EssenceTankBlockEntity.CAPACITY, board.essences());
        Research.learnAll(player, ResearchBook.runes(1), 1);
        Research.learnAll(player, ResearchBook.runes(2), 1);
        int cursed = cursedCell(board);
        int[] tree = ResearchSheet.fromPath(pathTo(board, cursed));
        table.select(player, board.rune());
        table.thread(player, tree);
        helper.assertTrue(table.sheet().length() == 0 && !table.sheet().paid(), "the sheet burnt with its paper");
        helper.assertTrue(table.getItem(ResearchTableBlockEntity.HEXTECH).getCount() == 2, "the orb burst");
        helper.assertTrue(Research.of(player).curses(board.rune()).get(cursed), "the curse is remembered");
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
        table.thread(player, RuneBoards.plainTree(RuneBoards.get(SMELTING)));
        helper.assertTrue(table.sheet().isDone(), "no ink and no paper are needed in creative mode");
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
        inkTanks(helper, EssenceTankBlockEntity.CAPACITY, RuneBoards.get(SMELTING).essences());
        table.thread(player, Arrays.copyOf(RuneBoards.plainTree(RuneBoards.get(SMELTING)), 4));
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
        helper.assertFalse(Research.of(reader).knows(lens), "Lens is too early before Insight");
        helper.assertTrue(reader.getMainHandItem().getCount() == 3, "the early scroll is kept");
        Research.learnAll(reader, List.of(SMELTING, ComplexRunes.byKey("light_earth")), 1);
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
    /** A chapter II rune's board (the first one with a curse). */
    private static RuneBoard cursedBoard() {
        for (ComplexRune rune : ResearchBook.runes(2)) {
            RuneBoard board = RuneBoards.get(rune);
            if (cursedCell(board) >= 0) {
                return board;
            }
        }
        throw new IllegalStateException("No chapter II board has a reachable curse");
    }

    /** A cursed section reachable from the orb over safe parchment, or -1. */
    private static int cursedCell(RuneBoard board) {
        for (int cell = 0; cell < HexGrid.COUNT; cell++) {
            if (board.cursed(cell) && pathTo(board, cell).length > 1) {
                return cell;
            }
        }
        return -1;
    }

    /** The shortest path from the orb to the target over parchment that is neither stone nor cursed (but the target). */
    private static int[] pathTo(RuneBoard board, int target) {
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
        ResearchTableBlockEntity table = helper.getBlockEntity(TABLE, ResearchTableBlockEntity.class);
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
            EssenceTankBlockEntity tank = helper.getBlockEntity(pos, EssenceTankBlockEntity.class);
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
        player.snapTo(pos.x, pos.y, pos.z, 0, 0);
        // A researcher who got as far as ink at the table: Smelting is open.
        for (com.pockyl.hextale.quest.Quest quest : com.pockyl.hextale.quest.Guide.SMELTING.parents()) {
            openUpTo(Research.of(player), quest);
        }
        return player;
    }

    private static void openUpTo(com.pockyl.hextale.knowledge.RuneKnowledge knowledge, com.pockyl.hextale.quest.Quest quest) {
        knowledge.completeQuest(quest);
        quest.parents().forEach(parent -> openUpTo(knowledge, parent));
    }
}
