package com.pockyl.hextale.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.RuneGlyphs;

import java.util.HashSet;
import java.util.Set;

/**
 * In-game tests, run headless by {@code gradlew runGameTestServer}.
 * Tests use the 1x1x1 {@code empty} structure unless they need a prepared one.
 */
@GameTestHolder(Hextale.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModGameTests {
    private ModGameTests() {
    }

    @GameTest(template = "empty")
    public static void modLoads(GameTestHelper helper) {
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void seventyTwoDistinctRunes(GameTestHelper helper) {
        helper.assertTrue(ComplexRunes.all().size() == ComplexRunes.COUNT, "72 complex runes");
        Set<String> keys = new HashSet<>();
        for (ComplexRune rune : ComplexRunes.all()) {
            helper.assertTrue(rune.action() != rune.target(), rune.key() + " uses two different elements");
            helper.assertTrue(keys.add(rune.key()), rune.key() + " is unique");
            helper.assertTrue(ComplexRunes.byKey(rune.key()) == rune, rune.key() + " is found by key");
            helper.assertTrue(rune.reversed() != rune && rune.reversed().reversed() == rune, rune.key() + " reversed is another rune");
            helper.assertFalse(RuneGlyphs.glyph(rune).isEmpty(), rune.key() + " has a hieroglyph");
        }
        helper.succeed();
    }
}
