package com.pockyl.hextale.gametest;

import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

import com.pockyl.hextale.Hextale;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Registers the {@link HexTest} methods as game tests: each method becomes a test function on the 8x8x8
 * {@code hextale:empty} structure in its own batch (1.21.1 walls the structure in with barriers, so it is as wide as
 * the tests reach), run headless by {@code gradlew runGameTestServer}. (26.x registers
 * them in the test function registry; 1.21.1 takes them from a {@link GameTestGenerator}.)
 */
public final class GameTests {
    private static final ResourceLocation STRUCTURE = Hextale.id("empty");
    private static final List<Class<?>> HOLDERS = List.of(ModGameTests.class, ResearchGameTests.class, QuestGameTests.class,
            RuneWorkGameTests.class, HextechGameTests.class, CraftingGameTests.class, PylonGameTests.class,
            MultiblockGameTests.class, CrucibleGameTests.class, MagicTreeGameTests.class, WhirlReaperGameTests.class, SawmillGameTests.class,
            RuneGiftGameTests.class, ChemGameTests.class, ShimmerGameTests.class, DualityGameTests.class, LabGameTests.class,
            SkypackGameTests.class, VoidGameTests.class, GateGameTests.class, FunnelGameTests.class, SpireGameTests.class, ShackleGameTests.class, CondenserGameTests.class, FoundryGameTests.class, VentGameTests.class, CreativeJarGameTests.class, ThinningGameTests.class, HollowbloomGameTests.class, HealingGameTests.class, VoidRoadGameTests.class, VoidRealmGameTests.class, VoidLifeGameTests.class, StruckGeodeGameTests.class, ForkGameTests.class, GadgetGameTests.class,
            RecipeGameTests.class, ProtectionGameTests.class, AutomationGameTests.class, QuestHookGameTests.class,
            EssenceFluidGameTests.class, WeaponRuneGameTests.class, ArsenalGameTests.class);

    /**
     * Ticks a test may wait for the chunks round its spot to tick their creatures before its body runs; added to every
     * test's {@link HexTest#maxTicks()}, so the wait never eats into the test's own time.
     */
    static final int WARM_UP = 1200;
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private record Test(String name, Method method, HexTest settings) {
    }

    private GameTests() {
    }

    /** Registers the tests when the game test framework is enabled (dev runs and the game test server). */
    public static void register(IEventBus modBus) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        modBus.addListener((RegisterGameTestsEvent event) -> event.register(GameTests.class));
    }

    /** Every {@link HexTest} as a test function; the batch is the test itself, so tests never share a batch. */
    @GameTestGenerator
    public static Collection<TestFunction> generate() {
        List<TestFunction> functions = new ArrayList<>();
        for (Test test : collect()) {
            String name = Hextale.MOD_ID + ":" + test.name();
            // Sky access: no barrier ceiling, so trees grow and things fall from any height.
            functions.add(new TestFunction(name, name, STRUCTURE.toString(), net.minecraft.world.level.block.Rotation.NONE,
                    test.settings().maxTicks() + WARM_UP, test.settings().setupTicks(), true, false, 1, 1, true, helper -> run(test.method(), test.settings(), helper)));
        }
        return functions;
    }

    private static List<Test> collect() {
        List<Test> tests = new ArrayList<>();
        // -Ptests=ClassName,other_test_name (gradle) runs only those classes / tests.
        String only = System.getProperty("hextale.tests", "");
        List<String> wanted = only.isBlank() ? List.of() : List.of(only.split(","));
        for (Class<?> holder : HOLDERS) {
            for (Method method : holder.getDeclaredMethods()) {
                HexTest settings = method.getAnnotation(HexTest.class);
                if (settings == null) {
                    continue;
                }
                if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1
                        || method.getParameterTypes()[0] != GameTestHelper.class) {
                    throw new IllegalStateException("A game test must be static and take a GameTestHelper: " + method);
                }
                String name = snakeCase(method.getName());
                if (wanted.isEmpty() || wanted.contains(holder.getSimpleName()) || wanted.contains(name)) {
                    tests.add(new Test(name, method, settings));
                }
            }
        }
        return tests;
    }

    private static void run(Method method, HexTest settings, GameTestHelper helper) {
        if (settings.padding() > 0) {
            clearAround(helper, settings.padding());
        }
        // Only the spot's own chunk is kept ticking by the framework: a breach hung beside a 1x1x1 spot may fall into
        // the next chunk, where entities stand still. Keep every chunk round the spot fully ticking.
        net.minecraft.core.BlockPos spot = helper.absolutePos(net.minecraft.core.BlockPos.ZERO);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.getLevel().setChunkForced((spot.getX() >> 4) + dx, (spot.getZ() >> 4) + dz, true);
            }
        }
        // A chunk forced just now ticks its creatures only once its entities are loaded, which under a full run can take
        // tens of ticks: creatures of the test standing still for a while made many tests flaky. Start once they tick.
        if (ticking(helper.getLevel(), spot)) {
            start(method, helper, 0);
            return;
        }
        // Waited for in a sequence: the framework's own delayed runs may not be scheduled from inside one of them.
        long asked = helper.getTick();
        helper.startSequence()
                .thenWaitUntil(() -> {
                    if (!ticking(helper.getLevel(), spot) && helper.getTick() - asked < WARM_UP) {
                        throw new net.minecraft.gametest.framework.GameTestAssertException("the chunks round the spot do not tick yet");
                    }
                })
                .thenExecute(() -> start(method, helper, helper.getTick() - asked));
    }

    private static void start(Method method, GameTestHelper helper, long waited) {
        if (waited >= WARM_UP) {
            LOGGER.warn("[hextale] {} starts with chunks round its spot that do not tick their creatures", method.getName());
        } else if (waited > 0) {
            LOGGER.debug("[hextale] {} waited {} ticks for the chunks round its spot", method.getName(), waited);
        }
        STARTED.put(helper, helper.getTick());
        invoke(method, helper);
    }

    private static boolean ticking(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos spot) {
        boolean all = true;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos((spot.getX() >> 4) + dx, (spot.getZ() >> 4) + dz);
                if (!entitiesTick(level, chunk) && !resync(level, chunk)) {
                    all = false;
                }
            }
        }
        return all;
    }

    private static boolean entitiesTick(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.ChunkPos chunk) {
        return level.areEntitiesLoaded(chunk.toLong()) && level.isPositionEntityTicking(chunk.getWorldPosition());
    }

    /**
     * The runner lets go of every forced chunk after each test and the next test, a few blocks away, forces the same
     * chunks again at once. The chunk then ticks its creatures again, but its entity manager can miss that: the step
     * down reaches it at once, the step back up only later and sometimes never, and the chunk's creatures stand still
     * for the whole test (seen with the chunk ENTITY_TICKING and its entities loaded). Tell the manager what the chunk is.
     */
    private static boolean resync(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.ChunkPos chunk) {
        net.minecraft.world.level.chunk.LevelChunk loaded = level.getChunkSource().getChunkNow(chunk.x, chunk.z);
        if (loaded == null || loaded.getFullStatus() != net.minecraft.server.level.FullChunkStatus.ENTITY_TICKING
                || !level.areEntitiesLoaded(chunk.toLong())) {
            return false;
        }
        try {
            if (entityManager == null) {
                entityManager = net.minecraft.server.level.ServerLevel.class.getDeclaredField("entityManager");
                entityManager.setAccessible(true);
            }
            ((net.minecraft.world.level.entity.PersistentEntitySectionManager<?>) entityManager.get(level))
                    .updateChunkStatus(chunk, net.minecraft.server.level.FullChunkStatus.ENTITY_TICKING);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return entitiesTick(level, chunk);
    }

    private static java.lang.reflect.Field entityManager;

    /** The test's tick when its body started (after the warm-up), for tests that measure how long something took. */
    private static final java.util.Map<GameTestHelper, Long> STARTED = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** Ticks since the test's body started. */
    public static long ticksSinceStart(GameTestHelper helper) {
        return helper.getTick() - STARTED.getOrDefault(helper, 0L);
    }

    private static void invoke(Method method, GameTestHelper helper) {
        try {
            method.invoke(null, helper);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * A creative mock player standing a few blocks from {@code near}: one is needed for a breach to send anything out.
     * The framework places mock players at the world spawn, often far from the test spot.
     */
    public static net.minecraft.server.level.ServerPlayer watcher(GameTestHelper helper, net.minecraft.core.BlockPos near) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        player.moveTo(near.getX() + 0.5, near.getY(), near.getZ() - 6.5, 0, 0);
        return player;
    }

    /**
     * 26.x keeps a padded test apart from its neighbours and clears its room; 1.21.1 has no padding, so the room round
     * the structure is cleared of blocks and entities here before the test starts.
     */
    private static void clearAround(GameTestHelper helper, int padding) {
        net.minecraft.world.phys.AABB bounds = helper.getBounds();
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        net.minecraft.core.BlockPos from = net.minecraft.core.BlockPos.containing(bounds.minX - padding, bounds.minY, bounds.minZ - padding);
        net.minecraft.core.BlockPos to = net.minecraft.core.BlockPos.containing(bounds.maxX + padding, bounds.maxY + padding, bounds.maxZ + padding);
        for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(from, to)) {
            if (!bounds.contains(net.minecraft.world.phys.Vec3.atCenterOf(pos)) && !level.getBlockState(pos).isAir()) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2 | 16);
            }
        }
        net.minecraft.world.phys.AABB room = new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(from),
                net.minecraft.world.phys.Vec3.atLowerCornerOf(to.offset(1, 1, 1)));
        for (net.minecraft.world.entity.Entity entity : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, room,
                entity -> !(entity instanceof net.minecraft.world.entity.player.Player))) {
            entity.discard();
        }
    }

    private static String snakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
