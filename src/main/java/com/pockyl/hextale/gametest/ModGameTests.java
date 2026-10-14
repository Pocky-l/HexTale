package com.pockyl.hextale.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.RuneGlyphs;
import com.pockyl.hextale.rune.RuneRecognizer;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * In-game tests, run headless by {@code gradlew runGameTestServer}.
 * Tests use the 1x1x1 {@code empty} structure unless they need a prepared one.
 */
@GameTestHolder(Hextale.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModGameTests {
    private static final int SAMPLES = 80;

    private ModGameTests() {
    }

    @GameTest(template = "empty")
    public static void modLoads(GameTestHelper helper) {
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recognizerAcceptsCleanRadicalsAtAnySize(GameTestHelper helper) {
        for (SimpleRune rune : SimpleRune.values()) {
            for (float scale : new float[]{0.3F, 1.0F, 2.5F}) {
                List<float[]> strokes = RuneSamples.clean(rune).stream().map(stroke -> scaled(stroke, scale)).toList();
                helper.assertTrue(RuneRecognizer.recognise(strokes) == rune, rune + " drawn cleanly at scale " + scale + " is recognised");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recognizerAcceptsShakyRadicals(GameTestHelper helper) {
        Random random = new Random(20261014L);
        for (SimpleRune rune : SimpleRune.values()) {
            int correct = 0;
            for (int i = 0; i < SAMPLES; i++) {
                if (RuneRecognizer.recognise(RuneSamples.wobbly(rune, random, 2)) == rune) {
                    correct++;
                }
            }
            helper.assertTrue(correct >= SAMPLES * 0.95, rune + ": " + correct + "/" + SAMPLES + " shaky drawings recognised");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recognizerRejectsScribbles(GameTestHelper helper) {
        Random random = new Random(7L);
        int accepted = 0;
        int total = 600;
        for (int i = 0; i < total; i++) {
            if (RuneRecognizer.recognise(RuneSamples.scribble(random)) != null) {
                accepted++;
            }
        }
        helper.assertTrue(accepted <= total * 0.04, accepted + "/" + total + " scribbles were taken for runes");
        helper.assertTrue(RuneRecognizer.recognise(List.of(new float[]{0, 0, 0.01F, 0.01F})) == null, "a dot is not a rune");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void extraStrokesAreRefused(GameTestHelper helper) {
        // Earth is three strokes; the same shape split into six pieces is too far from it.
        List<float[]> earth = RuneSamples.clean(SimpleRune.EARTH);
        List<float[]> split = new ArrayList<>();
        for (float[] stroke : earth) {
            split.add(new float[]{stroke[0], stroke[1], (stroke[0] + stroke[2]) / 2, (stroke[1] + stroke[3]) / 2});
            split.add(new float[]{(stroke[0] + stroke[2]) / 2, (stroke[1] + stroke[3]) / 2, stroke[2], stroke[3]});
        }
        helper.assertTrue(RuneRecognizer.recognise(split) == null, "six strokes are not the three-stroke earth radical");
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

    private static float[] scaled(float[] stroke, float scale) {
        float[] out = new float[stroke.length];
        for (int i = 0; i < stroke.length; i++) {
            out[i] = stroke[i] * scale;
        }
        return out;
    }
}
