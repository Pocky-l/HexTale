package com.pockyl.hextale.gametest;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Registers the {@link HexTest} methods with the registry-based game test framework: each method becomes a test function
 * and a test instance on the 1x1x1 {@code hextale:empty} structure, run headless by {@code gradlew runGameTestServer}.
 */
public final class GameTests {
    private static final Identifier STRUCTURE = Hextale.id("empty");
    private static final List<Class<?>> HOLDERS = List.of(ModGameTests.class, ResearchGameTests.class, QuestGameTests.class,
            RuneWorkGameTests.class, HextechGameTests.class, CraftingGameTests.class, PylonGameTests.class,
            MultiblockGameTests.class, CrucibleGameTests.class, GardenGameTests.class, MagicTreeGameTests.class, WhirlReaperGameTests.class, SawmillGameTests.class,
            RuneGiftGameTests.class, ChemGameTests.class, ShimmerGameTests.class, DualityGameTests.class, LabGameTests.class,
            SkypackGameTests.class, VoidGameTests.class, GateGameTests.class, FunnelGameTests.class, SpireGameTests.class, ShackleGameTests.class, CondenserGameTests.class, FoundryGameTests.class, VentGameTests.class, CreativeJarGameTests.class, ThinningGameTests.class, HollowbloomGameTests.class, HealingGameTests.class, VoidRoadGameTests.class, VoidRealmGameTests.class, VoidLifeGameTests.class, StruckGeodeGameTests.class, ForkGameTests.class, GadgetGameTests.class,
            RecipeGameTests.class, ProtectionGameTests.class, AutomationGameTests.class, QuestHookGameTests.class,
            EssenceFluidGameTests.class, WeaponRuneGameTests.class, ArsenalGameTests.class, TransmutationGameTests.class);
    /**
     * Ticks a test may wait for the chunks round its spot to tick their creatures before its body runs; added to every
     * test's {@link HexTest#maxTicks()}, so the wait never eats into the test's own time.
     */
    static final int WARM_UP = 1200;
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Hextale.MOD_ID);

    private record Test(String name, Method method, HexTest settings) {
    }

    private GameTests() {
    }

    /** Registers the tests when the game test framework is enabled (dev runs and the game test server). */
    public static void register(IEventBus modBus) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        List<Test> tests = collect();
        for (Test test : tests) {
            FUNCTIONS.register(test.name(), () -> helper -> run(test.method(), helper));
        }
        FUNCTIONS.register(modBus);
        modBus.addListener((RegisterGameTestsEvent event) -> {
            for (Test test : tests) {
                Identifier id = Hextale.id(test.name());
                Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id,
                        new TestEnvironmentDefinition.AllOf(List.of()));
                TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment,
                        net.minecraft.world.level.Level.OVERWORLD, STRUCTURE, test.settings().maxTicks() + WARM_UP, test.settings().setupTicks(),
                        true, net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, test.settings().padding());
                event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), data));
            }
        });
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

    private static void run(Method method, GameTestHelper helper) {
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
                        throw helper.assertionException("the chunks round the spot do not tick yet");
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
                if (!level.areEntitiesActuallyLoadedAndTicking(chunk) && !resync(level, chunk)) {
                    all = false;
                }
            }
        }
        return all;
    }

    /**
     * The runner lets go of every forced chunk after each test and the next test, a few blocks away, forces the same
     * chunks again at once. The chunk then ticks its creatures again, but its entity manager can miss that: the step
     * down reaches it at once, the step back up only later and sometimes never, and the chunk's creatures stand still
     * for the whole test (seen with the chunk ENTITY_TICKING and its entities loaded). Tell the manager what the chunk is.
     */
    private static boolean resync(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.ChunkPos chunk) {
        net.minecraft.world.level.chunk.LevelChunk loaded = level.getChunkSource().getChunkNow(chunk.x(), chunk.z());
        if (loaded == null || loaded.getFullStatus() != net.minecraft.server.level.FullChunkStatus.ENTITY_TICKING
                || !level.areEntitiesLoaded(chunk.pack())) {
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
        return level.areEntitiesActuallyLoadedAndTicking(chunk);
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
        player.snapTo(near.getX() + 0.5, near.getY(), near.getZ() - 6.5, 0, 0);
        return player;
    }

    private static String snakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
