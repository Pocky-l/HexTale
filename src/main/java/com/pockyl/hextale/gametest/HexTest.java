package com.pockyl.hextale.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A game test of this mod: a static method taking a {@code GameTestHelper}, registered by {@link GameTests} under the
 * method name in snake case. Every test gets its own environment, so it runs in its own batch: lightning and mock
 * players never reach a neighbouring test.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HexTest {
    int maxTicks() default 100;

    /** Ticks to wait before the test starts, e.g. for the chunks around a fresh test spot to load. */
    int setupTicks() default 0;
}
