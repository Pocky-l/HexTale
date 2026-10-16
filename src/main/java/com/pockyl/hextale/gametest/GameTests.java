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
    private static final List<Class<?>> HOLDERS = List.of(ModGameTests.class, ResearchGameTests.class, QuestGameTests.class);
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
                TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment, STRUCTURE,
                        test.settings().maxTicks(), test.settings().setupTicks(), true);
                event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id), data));
            }
        });
    }

    private static List<Test> collect() {
        List<Test> tests = new ArrayList<>();
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
                tests.add(new Test(snakeCase(method.getName()), method, settings));
            }
        }
        return tests;
    }

    private static void run(Method method, GameTestHelper helper) {
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

    private static String snakeCase(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
    }
}
