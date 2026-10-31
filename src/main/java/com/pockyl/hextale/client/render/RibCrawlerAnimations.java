package com.pockyl.hextale.client.render;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * The Rib Crawler's keyframe animations, made in Blockbench on {@code art/models/entity/rib_crawler.bbmodel} and
 * exported by its Modded Entity animation export; vanilla keyframes, no animation library. Like every Void creature it
 * moves too still or too fast: two idles (each crawler keeps one) that hold still and then twitch - chattering
 * mandibles, a ripple running down the plates, a head that snaps aside, the front lifting to taste the air, a shiver
 * through the legs; a walk with a metachronal wave down the legs and the body undulating with lag; the upside-down
 * idle under a ceiling ({@link #CLING}, gripping legs, slow mandible work, dry clicks); letting go and curling up
 * ({@link #DROP}, over {@link com.pockyl.hextale.thevoid.realm.RibCrawler#dropping}), springing open on landing
 * ({@link #UNCOIL}, over {@link com.pockyl.hextale.thevoid.realm.RibCrawler#UNCOIL_TICKS}) and the bite
 * ({@link #ATTACK}: the front third rears up with the jaws split wide, then snaps down and shut). Each animation is
 * built in its own method: together they are too large for one static initializer.
 */
public final class RibCrawlerAnimations {
    public static final AnimationDefinition IDLE = idle();
    public static final AnimationDefinition IDLE_2 = idle2();
    public static final AnimationDefinition WALK = walk();
    public static final AnimationDefinition CLING = cling();
    public static final AnimationDefinition DROP = drop();
    public static final AnimationDefinition UNCOIL = uncoil();
    public static final AnimationDefinition ATTACK = attack();

    private RibCrawlerAnimations() {
    }

    private static AnimationDefinition idle() {
        return AnimationDefinition.Builder.withLength(4.0F).looping()
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.SCALE,
                new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.scaleVec(1.02F, 1.04F, 1.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s6", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.4F, KeyframeAnimations.degreeVec(-3.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.52F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s5", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.34F, KeyframeAnimations.degreeVec(-3.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.46F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.18F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.28F, KeyframeAnimations.degreeVec(-3.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.22F, KeyframeAnimations.degreeVec(-3.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.34F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.06F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.16F, KeyframeAnimations.degreeVec(-3.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.28F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.1F, KeyframeAnimations.degreeVec(-3.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.22F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s7", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.36F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.46F, KeyframeAnimations.degreeVec(3.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.58F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s8", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.42F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.52F, KeyframeAnimations.degreeVec(3.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.64F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s9", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.48F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.58F, KeyframeAnimations.degreeVec(3.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s10", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.54F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.64F, KeyframeAnimations.degreeVec(3.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.76F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s11", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.7F, KeyframeAnimations.degreeVec(3.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.82F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s12", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.66F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.76F, KeyframeAnimations.degreeVec(3.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.88F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s2_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.48F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.56F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.62F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 16.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s10_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.36F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.65F, KeyframeAnimations.degreeVec(0.0F, -9.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(0.0F, -9.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.55F, KeyframeAnimations.degreeVec(0.0F, 3.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.62F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("cercus_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.8F, KeyframeAnimations.degreeVec(0.0F, -5.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.6F, KeyframeAnimations.degreeVec(0.0F, 3.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("cercus_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.8F, KeyframeAnimations.degreeVec(0.0F, 5.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.6F, KeyframeAnimations.degreeVec(0.0F, -3.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("jaw_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.65F, KeyframeAnimations.degreeVec(0.0F, 12.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.72F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.78F, KeyframeAnimations.degreeVec(0.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.85F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.05F, KeyframeAnimations.degreeVec(0.0F, 9.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.11F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.16F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.23F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("fang_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.65F, KeyframeAnimations.degreeVec(0.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.72F, KeyframeAnimations.degreeVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.78F, KeyframeAnimations.degreeVec(0.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.85F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("antenna_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(4.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(4.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.65F, KeyframeAnimations.degreeVec(-6.0F, 4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(-2.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("antenna_tip_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(-8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.7F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.9F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("jaw_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.65F, KeyframeAnimations.degreeVec(0.0F, -12.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.72F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.78F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.85F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.05F, KeyframeAnimations.degreeVec(0.0F, -9.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.11F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.16F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.23F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("fang_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.65F, KeyframeAnimations.degreeVec(0.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.72F, KeyframeAnimations.degreeVec(0.0F, 4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.78F, KeyframeAnimations.degreeVec(0.0F, -6.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.85F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("antenna_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(4.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(4.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.65F, KeyframeAnimations.degreeVec(-6.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(-2.0F, -6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("antenna_tip_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(-8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.7F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.9F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();
    }

    private static AnimationDefinition idle2() {
        return AnimationDefinition.Builder.withLength(5.0F).looping()
            .addAnimation("s3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(-7.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(-7.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(-3.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(-3.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s1_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.475F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.515F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.555F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.595F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.635F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s1_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.475F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.515F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.555F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.595F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.635F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s2_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.49F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.53F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.57F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.61F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s2_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.49F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.53F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.57F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.61F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s3_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.425F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.465F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.505F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.545F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.585F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s3_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.425F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.465F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.505F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.545F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.585F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s4_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.44F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.48F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.52F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.56F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s4_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.44F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.48F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.52F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.56F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s5_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.415F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.455F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.495F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.535F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s5_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.415F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.455F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.495F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.535F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s6_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.39F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.43F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.47F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.51F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s6_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.39F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.43F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.47F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.51F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s7_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.325F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.365F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.405F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.445F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.485F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s7_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.325F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.365F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.405F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.445F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.485F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s8_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.34F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.38F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.42F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.46F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s8_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.34F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.38F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.42F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.46F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s9_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.275F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.315F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.355F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.395F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.435F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s9_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.275F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.315F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.355F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.395F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.435F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s10_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.29F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.33F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.37F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.41F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s10_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.29F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.33F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.37F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.41F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s11_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.265F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.305F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.345F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.385F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s11_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.265F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.305F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.345F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.385F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s12_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.28F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.32F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.36F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("leg_s12_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.28F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.32F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.36F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.25F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(0.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.9F, KeyframeAnimations.degreeVec(0.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.95F, KeyframeAnimations.degreeVec(0.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(0.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("jaw_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.95F, KeyframeAnimations.degreeVec(0.0F, 14.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.08F, KeyframeAnimations.degreeVec(0.0F, 12.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.13F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.21F, KeyframeAnimations.degreeVec(0.0F, 13.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.26F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("antenna_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(-20.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.34F, KeyframeAnimations.degreeVec(-4.0F, -12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.48F, KeyframeAnimations.degreeVec(-20.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.62F, KeyframeAnimations.degreeVec(-4.0F, -12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.76F, KeyframeAnimations.degreeVec(-20.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.9F, KeyframeAnimations.degreeVec(-4.0F, -12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.04F, KeyframeAnimations.degreeVec(-20.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.18F, KeyframeAnimations.degreeVec(-4.0F, -12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("jaw_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.9F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.95F, KeyframeAnimations.degreeVec(0.0F, -14.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.08F, KeyframeAnimations.degreeVec(0.0F, -12.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.13F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.21F, KeyframeAnimations.degreeVec(0.0F, -13.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.26F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("antenna_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(-20.0F, -6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.34F, KeyframeAnimations.degreeVec(-4.0F, 12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.48F, KeyframeAnimations.degreeVec(-20.0F, -6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.62F, KeyframeAnimations.degreeVec(-4.0F, 12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.76F, KeyframeAnimations.degreeVec(-20.0F, -6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.9F, KeyframeAnimations.degreeVec(-4.0F, 12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.04F, KeyframeAnimations.degreeVec(-20.0F, -6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.18F, KeyframeAnimations.degreeVec(-4.0F, 12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(-4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();
    }

    private static AnimationDefinition walk() {
        return AnimationDefinition.Builder.withLength(0.6F).looping()
            .addAnimation("s6", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 1.7175F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 4.1555F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 4.1594F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 1.7267F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -1.7175F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -4.1555F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -4.1594F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -1.7267F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 1.7175F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s5", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 3.6382F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 4.4452F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 2.6483F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -0.7F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -3.6382F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -4.4452F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -2.6483F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.7F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 3.6382F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 4.4859F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 3.4238F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.356F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -2.9202F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -4.4859F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -3.4238F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -0.356F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 2.9202F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 4.4859F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 4.0104F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 1.3925F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -2.0412F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -4.2791F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -4.0104F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -1.3925F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 2.0412F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 4.2791F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 4.0104F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 2.3521F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -1.0495F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -3.8364F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -4.3759F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -2.3521F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 1.0495F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 3.8364F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 4.3759F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 2.3521F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -3.182F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -4.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -3.182F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 3.182F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 4.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 3.182F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s7", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.7099F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -2.6402F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -4.4437F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -3.6441F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -0.7099F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 2.6402F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 4.4437F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 3.6441F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.7099F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s8", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 2.9278F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -0.3461F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -3.4173F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -4.4867F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -2.9278F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.3461F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 3.4173F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 4.4867F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 2.9278F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s9", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 4.2822F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 2.0501F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -1.383F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -4.0059F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -4.2822F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -2.0501F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 1.383F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 4.0059F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 4.2822F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s10", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 4.3736F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 3.8416F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 1.0592F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -2.3436F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -4.3736F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -3.8416F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -1.0592F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 2.3436F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 4.3736F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s11", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 3.1749F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 4.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 3.189F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.01F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -3.1749F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -4.5F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -3.189F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -0.01F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 3.1749F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s12", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 1.0399F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 3.8311F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 4.3782F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 2.3606F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -1.0399F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -3.8311F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -4.3782F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -2.3606F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 1.0399F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s1_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, 12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 18.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, 12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s1_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.8F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s1_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, -12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -18.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, -12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s1_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.8F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s2_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, 4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, 9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 5.694F, 17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 15.5885F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s2_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -9.3531F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s2_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, -9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -5.694F, -17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 11.0F, -15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, -4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s2_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s3_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -5.694F, 17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, 9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, 4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 15.5885F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s3_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -9.3531F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s3_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, -4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -11.0F, -15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 5.694F, -17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, -9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s3_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s4_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, 12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 18.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, 12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s4_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.8F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s4_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, -12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -18.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, -12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s4_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.8F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s5_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, 9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 5.694F, 17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, 4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s5_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s5_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 11.0F, -15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, -4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, -9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -5.694F, -17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 11.0F, -15.5885F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s5_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 9.3531F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s6_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, 4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -5.694F, 17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, 9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s6_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s6_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -11.0F, -15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 5.694F, -17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, -9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, -4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -11.0F, -15.5885F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s6_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 9.3531F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s7_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, 12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 18.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, 12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s7_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.8F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s7_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, -12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -18.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, -12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s7_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.8F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s8_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, 4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, 9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 5.694F, 17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 15.5885F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s8_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -9.3531F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s8_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, -9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -5.694F, -17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 11.0F, -15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, -4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s8_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s9_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -5.694F, 17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, 9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, 4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 15.5885F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s9_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -9.3531F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s9_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, -4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -11.0F, -15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 5.694F, -17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, -9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s9_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s10_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, 12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 18.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, 12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s10_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.8F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s10_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -15.5563F, -12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -18.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 15.5563F, -12.7279F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 22.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s10_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.8F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 7.6368F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s11_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, 9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 5.694F, 17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, 4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s11_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s11_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 11.0F, -15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, -4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, -9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -5.694F, -17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 11.0F, -15.5885F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s11_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 9.3531F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s12_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, 4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -5.694F, 17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, 9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s12_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s12_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -11.0F, -15.5885F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 5.694F, -17.3867F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 19.0526F, -9.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 21.2504F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 11.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -5.694F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -19.0526F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -21.2504F, -4.6587F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -11.0F, -15.5885F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s12_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 9.3531F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.432F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.7952F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 9.3531F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 2.8232F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 4.9143F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 4.1267F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.9217F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -2.8232F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -4.9143F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -4.1267F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -0.9217F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 2.8232F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("cercus_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 6.3651F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 6.5606F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 2.913F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -2.441F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -6.3651F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -6.5606F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -2.913F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 2.441F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 6.3651F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("cercus_tip_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 3.0149F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 8.1281F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 8.48F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 3.8644F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -3.0149F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -8.1281F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -8.48F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -3.8644F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 3.0149F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("cercus_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 6.3651F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 6.5606F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 2.913F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, -2.441F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -6.3651F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -6.5606F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -2.913F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 2.441F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 6.3651F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("cercus_tip_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 3.0149F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 8.1281F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 8.48F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 3.8644F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, -3.0149F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -8.1281F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, -8.48F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, -3.8644F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 3.0149F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("jaw_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, 4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, 4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("antenna_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -6.7318F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(-4.2426F, -7.8165F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(-6.0F, -4.3224F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(-4.2426F, 1.7037F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 6.7318F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(4.2426F, 7.8165F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(6.0F, 4.3224F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(4.2426F, -1.7037F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -6.7318F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("antenna_tip_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(6.2666F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.9148F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(-4.9729F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(-7.9475F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(-6.2666F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(-0.9148F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(4.9729F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(7.9475F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(6.2666F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("jaw_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("antenna_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -6.7318F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(4.2426F, -7.8165F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(6.0F, -4.3224F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(4.2426F, 1.7037F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 6.7318F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(-4.2426F, 7.8165F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(-6.0F, 4.3224F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(-4.2426F, -1.7037F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -6.7318F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("antenna_tip_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-6.2666F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.075F, KeyframeAnimations.degreeVec(-0.9148F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(4.9729F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.225F, KeyframeAnimations.degreeVec(7.9475F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(6.2666F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.375F, KeyframeAnimations.degreeVec(0.9148F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(-4.9729F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.525F, KeyframeAnimations.degreeVec(-7.9475F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-6.2666F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();
    }

    private static AnimationDefinition cling() {
        return AnimationDefinition.Builder.withLength(4.0F).looping()
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, -1.2F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F, -1.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.posVec(0.0F, -1.2F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s6", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s5", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.32F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.52F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.72F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.44F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.64F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.16F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.36F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.56F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.08F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.28F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.48F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(-2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s7", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.48F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.68F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.88F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s8", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.56F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.76F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.96F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s9", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.64F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.84F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.04F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s10", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.72F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.92F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s11", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.2F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s12", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.88F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.08F, KeyframeAnimations.degreeVec(2.5F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.28F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s1_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.5125F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.5125F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.5125F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s1_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s1_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.5125F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 14.5125F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.5125F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s1_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s2_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.3833F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -16.3833F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.3833F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s2_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s2_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 14.3833F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 16.3833F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 14.3833F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s2_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s3_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.3887F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -17.3887F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.3887F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s3_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s3_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.3887F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 17.3887F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.3887F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s3_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s4_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -13.2588F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.2588F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -13.2588F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s4_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s4_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 13.2588F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.2588F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 13.2588F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s4_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s5_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.8023F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.8023F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.8023F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s5_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s5_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.8023F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 14.8023F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.8023F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s5_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s6_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0498F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -17.0498F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0498F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s6_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s6_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0498F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 17.0498F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0498F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s6_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s7_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.9272F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -16.9272F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.9272F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s7_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s7_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 14.9272F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 16.9272F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 14.9272F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s7_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s8_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.7113F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.7113F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.7113F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s8_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s8_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.7113F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 14.7113F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.7113F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s8_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s9_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -13.4049F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.4049F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -13.4049F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s9_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s9_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 13.4049F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.4049F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 13.4049F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s9_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s10_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.4421F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -17.4421F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.4421F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s10_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s10_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.4421F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 17.4421F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.4421F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s10_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s11_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.2235F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -16.2235F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.2235F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s11_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s11_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 14.2235F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 16.2235F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 14.2235F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s11_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s12_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.5003F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -14.5003F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.5003F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s12_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s12_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.5003F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 14.5003F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.5003F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s12_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-14.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-12.0F, 4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("cercus_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(10.0F, -6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("cercus_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(10.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("jaw_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.64F, KeyframeAnimations.degreeVec(0.0F, 7.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.68F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.72F, KeyframeAnimations.degreeVec(0.0F, 7.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.76F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(0.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.84F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.88F, KeyframeAnimations.degreeVec(0.0F, 7.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.92F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("antenna_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-18.0F, -6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(-24.0F, -12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-18.0F, -6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("antenna_tip_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("jaw_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, -6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.64F, KeyframeAnimations.degreeVec(0.0F, -7.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.68F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.72F, KeyframeAnimations.degreeVec(0.0F, -7.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.76F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(0.0F, -6.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.84F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.88F, KeyframeAnimations.degreeVec(0.0F, -7.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.92F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("antenna_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-18.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.4F, KeyframeAnimations.degreeVec(-24.0F, 12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.8F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-18.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("antenna_tip_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();
    }

    private static AnimationDefinition drop() {
        return AnimationDefinition.Builder.withLength(1.5F)
            .addAnimation("s6", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(-3.0F, -13.6F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s5", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(-3.0F, -13.6F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(-3.0F, -13.6F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(-3.0F, -13.6F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(-3.0F, -13.6F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(-3.0F, -17.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-4.0F, -20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-4.0F, -20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s7", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(3.0F, 13.6F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s8", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(3.0F, 13.6F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s9", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(3.0F, 13.6F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s10", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(3.0F, 13.6F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s11", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(3.0F, 13.6F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s12", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(3.0F, 10.2F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(4.0F, 12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(4.0F, 12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s1_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s1_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s1_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s1_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s2_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s2_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s2_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s2_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s3_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s3_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s3_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s3_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s4_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s4_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s4_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s4_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s5_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s5_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s5_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s5_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s6_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s6_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s6_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s6_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s7_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s7_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s7_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s7_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s8_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s8_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s8_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s8_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s9_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s9_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s9_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s9_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s10_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s10_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s10_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s10_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s11_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s11_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s11_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s11_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s12_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s12_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s12_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -25.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 10.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s12_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 15.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(-10.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-10.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("cercus_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(10.0F, 20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(10.0F, 20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("cercus_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(10.0F, -20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(10.0F, -20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("fang_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("antenna_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(25.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(25.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("fang_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("antenna_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(-20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(25.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(25.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();
    }

    private static AnimationDefinition uncoil() {
        return AnimationDefinition.Builder.withLength(1.0F)
            .addAnimation("s6", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(-4.0F, -14.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 4.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(0.0F, -1.92F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s5", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(-4.0F, -14.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 4.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(0.0F, -1.92F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(-4.0F, -14.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 4.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(0.0F, -1.92F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(-4.0F, -14.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 4.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(0.0F, -1.92F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-4.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(-4.0F, -14.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 4.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(0.0F, -1.92F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 0.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-4.0F, -20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(-4.0F, -18.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(0.0F, -2.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 1.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s7", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(4.0F, 14.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, -4.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(0.0F, 1.92F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -0.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s8", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(4.0F, 14.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, -4.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(0.0F, 1.92F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -0.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s9", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(4.0F, 14.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, -4.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(0.0F, 1.92F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -0.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s10", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(4.0F, 14.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, -4.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(0.0F, 1.92F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -0.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s11", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(4.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(4.0F, 14.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, -4.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(0.0F, 1.92F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -0.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s12", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(4.0F, 12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.1F, KeyframeAnimations.degreeVec(4.0F, 10.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, -3.6F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(0.0F, 1.44F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -0.6F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s1_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s1_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s1_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s1_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s2_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s2_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s2_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s2_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s3_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s3_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s3_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s3_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s4_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s4_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s4_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s4_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s5_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s5_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s5_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s5_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s6_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s6_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s6_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s6_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s7_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s7_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s7_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s7_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s8_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s8_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s8_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s8_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s9_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s9_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s9_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s9_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s10_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s10_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s10_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s10_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s11_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s11_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s11_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s11_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s12_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s12_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s12_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 34.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -22.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 4.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -3.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("knee_s12_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -40.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 12.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.45F, KeyframeAnimations.degreeVec(0.0F, 0.0F, -2.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-10.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.12F, KeyframeAnimations.degreeVec(-10.0F, -26.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.26F, KeyframeAnimations.degreeVec(4.0F, 12.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.42F, KeyframeAnimations.degreeVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.7F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("cercus_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, 20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(-10.0F, -15.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, 4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("cercus_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, -20.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(-10.0F, 15.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("jaw_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.34F, KeyframeAnimations.degreeVec(0.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.46F, KeyframeAnimations.degreeVec(0.0F, 12.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.52F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("fang_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(0.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("antenna_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(25.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(25.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(-25.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("jaw_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.34F, KeyframeAnimations.degreeVec(0.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.46F, KeyframeAnimations.degreeVec(0.0F, -12.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.52F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("fang_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.2F, KeyframeAnimations.degreeVec(0.0F, -10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("antenna_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(25.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.15F, KeyframeAnimations.degreeVec(25.0F, 10.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(-25.0F, -8.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.55F, KeyframeAnimations.degreeVec(4.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();
    }

    private static AnimationDefinition attack() {
        return AnimationDefinition.Builder.withLength(0.4F)
            .addAnimation("s5", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(-6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.22F, KeyframeAnimations.degreeVec(-1.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(-0.4F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s4", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.22F, KeyframeAnimations.degreeVec(-2.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(-0.8F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(-17.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.22F, KeyframeAnimations.degreeVec(3.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(1.2F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(-13.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.22F, KeyframeAnimations.degreeVec(7.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(2.8F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("s1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(-9.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.22F, KeyframeAnimations.degreeVec(7.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(2.8F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s1_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(0.0F, -24.0F, 26.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.23F, KeyframeAnimations.degreeVec(0.0F, -8.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s1_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(0.0F, 24.0F, -26.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.23F, KeyframeAnimations.degreeVec(0.0F, 8.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s2_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(0.0F, -24.0F, 26.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.23F, KeyframeAnimations.degreeVec(0.0F, -8.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s2_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(0.0F, 24.0F, -26.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.23F, KeyframeAnimations.degreeVec(0.0F, 8.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s3_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(0.0F, -24.0F, 26.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.23F, KeyframeAnimations.degreeVec(0.0F, -8.0F, -6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("leg_s3_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(0.0F, 24.0F, -26.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.23F, KeyframeAnimations.degreeVec(0.0F, 8.0F, 6.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(6.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.22F, KeyframeAnimations.degreeVec(20.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.32F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("jaw_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(0.0F, 28.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.21F, KeyframeAnimations.degreeVec(0.0F, 28.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.23F, KeyframeAnimations.degreeVec(0.0F, -3.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("fang_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(0.0F, 22.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.21F, KeyframeAnimations.degreeVec(0.0F, 22.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.23F, KeyframeAnimations.degreeVec(0.0F, -16.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.33F, KeyframeAnimations.degreeVec(0.0F, -6.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("antenna_r", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(-30.0F, -18.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(10.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("jaw_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(0.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.21F, KeyframeAnimations.degreeVec(0.0F, -28.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.23F, KeyframeAnimations.degreeVec(0.0F, 3.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("fang_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.21F, KeyframeAnimations.degreeVec(0.0F, -22.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.23F, KeyframeAnimations.degreeVec(0.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.33F, KeyframeAnimations.degreeVec(0.0F, 6.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("antenna_l", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.16F, KeyframeAnimations.degreeVec(-30.0F, 18.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.24F, KeyframeAnimations.degreeVec(10.0F, -6.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.4F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();
    }
}
