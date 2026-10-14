package com.pockyl.hextale.rune;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import static com.pockyl.hextale.rune.RuneCategory.CREATION;
import static com.pockyl.hextale.rune.RuneCategory.DESTRUCTION;
import static com.pockyl.hextale.rune.RuneCategory.ENERGY;
import static com.pockyl.hextale.rune.RuneCategory.LIFE;
import static com.pockyl.hextale.rune.RuneCategory.LIQUIDS;
import static com.pockyl.hextale.rune.RuneCategory.MECHANICS;
import static com.pockyl.hextale.rune.RuneCategory.MOVEMENT;
import static com.pockyl.hextale.rune.RuneCategory.PERCEPTION;
import static com.pockyl.hextale.rune.RuneCategory.PROTECTION;

/**
 * All 72 complex runes: every ordered pair of two different elements, with the category from the approved design.
 * Structure lives here in code (shared by client, server and tests); names and descriptions live in the language files.
 */
public final class ComplexRunes {
    public static final int COUNT = 72;

    private static final List<ComplexRune> ALL = new ArrayList<>();
    private static final Map<String, ComplexRune> BY_KEY = new HashMap<>();
    private static final ComplexRune[][] BY_PAIR = new ComplexRune[SimpleRune.values().length][SimpleRune.values().length];

    /** By key, e.g. {@code "fire_earth"}. */
    public static final Codec<ComplexRune> CODEC = Codec.STRING.comapFlatMap(key -> {
        ComplexRune rune = BY_KEY.get(key);
        return rune != null ? DataResult.success(rune) : DataResult.error(() -> "Unknown rune: " + key);
    }, ComplexRune::key);
    public static final StreamCodec<ByteBuf, ComplexRune> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(ALL::get, ComplexRune::index);

    static {
        SimpleRune fire = SimpleRune.FIRE;
        SimpleRune water = SimpleRune.WATER;
        SimpleRune earth = SimpleRune.EARTH;
        SimpleRune air = SimpleRune.AIR;
        SimpleRune light = SimpleRune.LIGHT;
        SimpleRune shadow = SimpleRune.SHADOW;
        SimpleRune life = SimpleRune.LIFE;
        SimpleRune metal = SimpleRune.METAL;
        SimpleRune spark = SimpleRune.SPARK;

        rune(fire, water, ENERGY);
        rune(fire, earth, DESTRUCTION);
        rune(fire, air, DESTRUCTION);
        rune(fire, light, CREATION);
        rune(fire, shadow, DESTRUCTION);
        rune(fire, life, DESTRUCTION);
        rune(fire, metal, CREATION);
        rune(fire, spark, DESTRUCTION);

        rune(water, fire, PROTECTION);
        rune(water, earth, DESTRUCTION);
        rune(water, air, PROTECTION);
        rune(water, light, PERCEPTION);
        rune(water, shadow, PROTECTION);
        rune(water, life, LIFE);
        rune(water, metal, DESTRUCTION);
        rune(water, spark, ENERGY);

        rune(earth, fire, PROTECTION);
        rune(earth, water, LIQUIDS);
        rune(earth, air, MOVEMENT);
        rune(earth, light, CREATION);
        rune(earth, shadow, PROTECTION);
        rune(earth, life, LIFE);
        rune(earth, metal, PROTECTION);
        rune(earth, spark, PROTECTION);

        rune(air, fire, ENERGY);
        rune(air, water, LIQUIDS);
        rune(air, earth, MOVEMENT);
        rune(air, light, PERCEPTION);
        rune(air, shadow, PROTECTION);
        rune(air, life, LIFE);
        rune(air, metal, MOVEMENT);
        rune(air, spark, ENERGY);

        rune(light, fire, DESTRUCTION);
        rune(light, water, LIFE);
        rune(light, earth, PERCEPTION);
        rune(light, air, PERCEPTION);
        rune(light, shadow, PROTECTION);
        rune(light, life, LIFE);
        rune(light, metal, MECHANICS);
        rune(light, spark, ENERGY);

        rune(shadow, fire, PROTECTION);
        rune(shadow, water, LIQUIDS);
        rune(shadow, earth, MOVEMENT);
        rune(shadow, air, PROTECTION);
        rune(shadow, light, PERCEPTION);
        rune(shadow, life, DESTRUCTION);
        rune(shadow, metal, DESTRUCTION);
        rune(shadow, spark, ENERGY);

        rune(life, fire, LIFE);
        rune(life, water, LIQUIDS);
        rune(life, earth, LIFE);
        rune(life, air, LIFE);
        rune(life, light, PERCEPTION);
        rune(life, shadow, PROTECTION);
        rune(life, metal, CREATION);
        rune(life, spark, ENERGY);

        rune(metal, fire, MECHANICS);
        rune(metal, water, LIQUIDS);
        rune(metal, earth, DESTRUCTION);
        rune(metal, air, MOVEMENT);
        rune(metal, light, PROTECTION);
        rune(metal, shadow, PROTECTION);
        rune(metal, life, MECHANICS);
        rune(metal, spark, MECHANICS);

        rune(spark, fire, DESTRUCTION);
        rune(spark, water, DESTRUCTION);
        rune(spark, earth, DESTRUCTION);
        rune(spark, air, DESTRUCTION);
        rune(spark, light, PROTECTION);
        rune(spark, shadow, MOVEMENT);
        rune(spark, life, MOVEMENT);
        rune(spark, metal, MECHANICS);
    }

    private ComplexRunes() {
    }

    private static void rune(SimpleRune action, SimpleRune target, RuneCategory category) {
        ComplexRune rune = new ComplexRune(ALL.size(), action, target, category);
        ALL.add(rune);
        BY_KEY.put(rune.key(), rune);
        BY_PAIR[action.ordinal()][target.ordinal()] = rune;
    }

    /** All runes, action-major in element order. */
    public static List<ComplexRune> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** The rune of an ordered pair, or null for the same element twice. */
    @Nullable
    public static ComplexRune of(SimpleRune action, SimpleRune target) {
        return BY_PAIR[action.ordinal()][target.ordinal()];
    }

    @Nullable
    public static ComplexRune byKey(String key) {
        return BY_KEY.get(key);
    }
}
