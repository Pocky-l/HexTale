package com.pockyl.hextale.client.render;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/**
 * The Seer's keyframe animations, made in Blockbench on {@code art/models/entity/seer.bbmodel} and exported by its
 * Modded Entity animation export; vanilla keyframes, no animation library. Two idles (each Seer keeps one): it bobs, its
 * cage of petals breathes, the eye darts in snaps and holds, the tendrils sway with a lag down their joints. The cage's
 * open pose ({@link #OPEN}) is weighed by {@link com.pockyl.hextale.thevoid.realm.Seer#open}, so it opens and shuts
 * with the petals' own clock; while the beam marks someone the pupil narrows to a hair and the petals flare
 * ({@link #MARK}, blended in by the beam's strength); drifting, its tendrils trail behind and it leans into the motion
 * ({@link #DRIFT}, blended in by its speed) - shutting the cage as it drifts off is the two together.
 */
public final class SeerAnimations {
    public static final AnimationDefinition IDLE = idle();
    public static final AnimationDefinition IDLE_2 = idle2();
    public static final AnimationDefinition OPEN = open();
    public static final AnimationDefinition MARK = mark();
    public static final AnimationDefinition DRIFT = drift();

    private SeerAnimations() {
    }

    private static AnimationDefinition idle() {
        return AnimationDefinition.Builder.withLength(4.0F).looping()
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.posVec(0.0F, 0.8F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("eye", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.8F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.84F, KeyframeAnimations.degreeVec(4.0F, -14.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.8F, KeyframeAnimations.degreeVec(4.0F, -14.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.84F, KeyframeAnimations.degreeVec(-7.0F, 9.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.9F, KeyframeAnimations.degreeVec(-7.0F, 9.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.94F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("petal_0_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_1_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.05F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_2_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.1F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_3_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.15F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_4_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.2F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_5_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.25F, KeyframeAnimations.degreeVec(-5.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_0", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(7.0499F, 0.0F, 3.3567F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(1.0292F, 0.0F, 5.3646F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-5.5945F, 0.0F, 4.23F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-8.941F, 0.0F, 0.6175F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-7.0499F, 0.0F, -3.3567F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-1.0292F, 0.0F, -5.3646F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(5.5945F, 0.0F, -4.23F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(8.941F, 0.0F, -0.6175F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(7.0499F, 0.0F, 3.3567F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_0_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(8.7646F, 0.0F, -1.2269F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(7.6434F, 0.0F, 2.851F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(2.0448F, 0.0F, 5.2588F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-4.7516F, 0.0F, 4.5861F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-8.7646F, 0.0F, 1.2269F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-7.6434F, 0.0F, -2.851F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-2.0448F, 0.0F, -5.2588F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(4.7516F, 0.0F, -4.5861F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(8.7646F, 0.0F, -1.2269F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_0_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(3.8464F, 0.0F, -4.882F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(8.4733F, 0.0F, -1.8202F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(8.1366F, 0.0F, 2.3079F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(3.0337F, 0.0F, 5.084F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-3.8464F, 0.0F, 4.882F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-8.4733F, 0.0F, 1.8202F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-8.1366F, 0.0F, -2.3079F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-3.0337F, 0.0F, -5.084F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(3.8464F, 0.0F, -4.882F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(8.9962F, 0.0F, 4.4568F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(6.5471F, 0.0F, 5.3075F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(0.2628F, 0.0F, 3.0491F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-6.1754F, 0.0F, -0.9954F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-8.9962F, 0.0F, -4.4568F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-6.5471F, 0.0F, -5.3075F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-0.2628F, 0.0F, -3.0491F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(6.1754F, 0.0F, 0.9954F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(8.9962F, 0.0F, 4.4568F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_1_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(5.3862F, 0.0F, 0.382F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(8.9071F, 0.0F, 4.0789F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(7.2103F, 0.0F, 5.3865F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(1.2898F, 0.0F, 3.5387F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-5.3862F, 0.0F, -0.382F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-8.9071F, 0.0F, -4.0789F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-7.2103F, 0.0F, -5.3865F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-1.2898F, 0.0F, -3.5387F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(5.3862F, 0.0F, 0.382F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_1_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-2.2999F, 0.0F, -3.9819F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(4.5264F, 0.0F, -0.2365F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(8.7012F, 0.0F, 3.6475F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(7.7789F, 0.0F, 5.3948F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(2.2999F, 0.0F, 3.9819F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-4.5264F, 0.0F, 0.2365F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-8.7012F, 0.0F, -3.6475F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-7.7789F, 0.0F, -5.3948F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-2.2999F, 0.0F, -3.9819F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(6.7113F, 0.0F, 5.1588F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(8.9858F, 0.0F, 4.7762F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(5.9965F, 0.0F, 1.5958F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-0.5055F, 0.0F, -2.5194F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-6.7113F, 0.0F, -5.1588F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-8.9858F, 0.0F, -4.7762F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-5.9965F, 0.0F, -1.5958F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(0.5055F, 0.0F, 2.5194F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(6.7113F, 0.0F, 5.1588F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_2_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-0.5254F, 0.0F, 1.9567F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(5.9816F, 0.0F, 4.9425F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(8.9847F, 0.0F, 5.033F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(6.7246F, 0.0F, 2.1753F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(0.5254F, 0.0F, -1.9567F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-5.9816F, 0.0F, -4.9425F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-8.9847F, 0.0F, -5.033F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-6.7246F, 0.0F, -2.1753F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-0.5254F, 0.0F, 1.9567F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_2_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-7.3645F, 0.0F, -2.7262F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-1.5493F, 0.0F, 1.3684F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(5.1734F, 0.0F, 4.6613F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(8.8656F, 0.0F, 5.2238F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(7.3645F, 0.0F, 2.7262F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(1.5493F, 0.0F, -1.3684F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-5.1734F, 0.0F, -4.6613F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-8.8656F, 0.0F, -5.2238F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-7.3645F, 0.0F, -2.7262F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(1.2701F, 0.0F, 5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(7.1984F, 0.0F, 3.8184F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(8.9099F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(5.4022F, 0.0F, -3.8184F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-1.2701F, 0.0F, -5.4F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-7.1984F, 0.0F, -3.8184F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-8.9099F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-5.4022F, 0.0F, 3.8184F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(1.2701F, 0.0F, 5.4F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_3_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-6.1899F, 0.0F, 3.3567F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(0.2429F, 0.0F, 5.3646F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(6.5334F, 0.0F, 4.23F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(8.9967F, 0.0F, 0.6175F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(6.1899F, 0.0F, -3.3567F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-0.2429F, 0.0F, -5.3646F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-6.5334F, 0.0F, -4.23F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-8.9967F, 0.0F, -0.6175F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-6.1899F, 0.0F, 3.3567F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_3_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-8.9655F, 0.0F, -1.2269F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-6.8964F, 0.0F, 2.851F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-0.7875F, 0.0F, 5.2588F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(5.7827F, 0.0F, 4.5861F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(8.9655F, 0.0F, 1.2269F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(6.8964F, 0.0F, -2.851F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(0.7875F, 0.0F, -5.2588F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-5.7827F, 0.0F, -4.5861F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-8.9655F, 0.0F, -1.2269F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();
    }

    private static AnimationDefinition idle2() {
        return AnimationDefinition.Builder.withLength(5.0F).looping()
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.posVec(0.0F, 0.6F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("eye", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(0.63F, KeyframeAnimations.degreeVec(0.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.3F, KeyframeAnimations.degreeVec(0.0F, 16.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(1.33F, KeyframeAnimations.degreeVec(-9.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.1F, KeyframeAnimations.degreeVec(-9.0F, -4.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(2.13F, KeyframeAnimations.degreeVec(6.0F, -12.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.2F, KeyframeAnimations.degreeVec(6.0F, -12.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(3.23F, KeyframeAnimations.degreeVec(-3.0F, 5.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.2F, KeyframeAnimations.degreeVec(-3.0F, 5.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(4.24F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
            ))
            .addAnimation("pupil", new AnimationChannel(AnimationChannel.Targets.SCALE,
                new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.3F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.45F, KeyframeAnimations.scaleVec(1.6F, 1.0F, 1.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.9F, KeyframeAnimations.scaleVec(1.6F, 1.0F, 1.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.1F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("socket", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(0.0F, -7.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_0_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.2F, KeyframeAnimations.degreeVec(-7.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_1_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.55F, KeyframeAnimations.degreeVec(-7.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.95F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_2_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.9F, KeyframeAnimations.degreeVec(-7.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.3F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_3_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.25F, KeyframeAnimations.degreeVec(-7.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.65F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_4_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.6F, KeyframeAnimations.degreeVec(-7.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_5_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.95F, KeyframeAnimations.degreeVec(-7.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.35F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_0", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-4.673F, 0.0F, 6.6316F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-10.2772F, 0.0F, 3.7171F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-11.9558F, 0.0F, -0.6173F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-9.0677F, 0.0F, -4.7159F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-2.7161F, 0.0F, -7.0131F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(4.673F, 0.0F, -6.6316F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(10.2772F, 0.0F, -3.7171F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(11.9558F, 0.0F, 0.6173F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(9.0677F, 0.0F, 4.7159F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.5F, KeyframeAnimations.degreeVec(2.7161F, 0.0F, 7.0131F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(-4.673F, 0.0F, 6.6316F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_0_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(5.7531F, 0.0F, 6.3186F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-1.5356F, 0.0F, 7.1408F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-8.2378F, 0.0F, 5.2355F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-11.7934F, 0.0F, 1.3304F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-10.8443F, 0.0F, -3.0829F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-5.7531F, 0.0F, -6.3186F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(1.5356F, 0.0F, -7.1408F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(8.2378F, 0.0F, -5.2355F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(11.7934F, 0.0F, -1.3304F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.5F, KeyframeAnimations.degreeVec(10.8443F, 0.0F, 3.0829F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(5.7531F, 0.0F, 6.3186F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_0_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(11.8254F, 0.0F, 1.2238F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(8.3681F, 0.0F, 5.1605F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(1.7145F, 0.0F, 7.1261F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-5.594F, 0.0F, 6.3698F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-10.7658F, 0.0F, 3.1804F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-11.8254F, 0.0F, -1.2238F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-8.3681F, 0.0F, -5.1605F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-1.7145F, 0.0F, -7.1261F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(5.594F, 0.0F, -6.3698F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.5F, KeyframeAnimations.degreeVec(10.7658F, 0.0F, -3.1804F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(11.8254F, 0.0F, 1.2238F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(3.5462F, 0.0F, 5.5069F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-3.8694F, 0.0F, 1.7288F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-9.8071F, 0.0F, -2.7096F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-11.9988F, 0.0F, -6.1131F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-9.6074F, 0.0F, -7.1815F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-3.5462F, 0.0F, -5.5069F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(3.8694F, 0.0F, -1.7288F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(9.8071F, 0.0F, 2.7096F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(11.9988F, 0.0F, 6.1131F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.5F, KeyframeAnimations.degreeVec(9.6074F, 0.0F, 7.1815F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(3.5462F, 0.0F, 5.5069F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_1_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(11.1845F, 0.0F, 7.0565F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(6.4926F, 0.0F, 6.5496F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-0.6793F, 0.0F, 3.541F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-7.5917F, 0.0F, -0.8202F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-11.6043F, 0.0F, -4.868F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-11.1845F, 0.0F, -7.0565F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-6.4926F, 0.0F, -6.5496F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(0.6793F, 0.0F, -3.541F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(7.5917F, 0.0F, 0.8202F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.5F, KeyframeAnimations.degreeVec(11.6043F, 0.0F, 4.868F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(11.1845F, 0.0F, 7.0565F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_1_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.3585F, 0.0F, 3.2659F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(11.9411F, 0.0F, 6.4138F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(8.9626F, 0.0F, 7.1119F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(2.5607F, 0.0F, 5.0934F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-4.8193F, 0.0F, 1.1295F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-10.3585F, 0.0F, -3.2659F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-11.9411F, 0.0F, -6.4138F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-8.9626F, 0.0F, -7.1119F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-2.5607F, 0.0F, -5.0934F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.5F, KeyframeAnimations.degreeVec(4.8193F, 0.0F, -1.1295F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(10.3585F, 0.0F, 3.2659F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0977F, 0.0F, 3.8902F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(4.3582F, 0.0F, -0.4139F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(-3.046F, 0.0F, -4.5599F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-9.2866F, 0.0F, -6.9642F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-11.9802F, 0.0F, -6.7084F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-10.0977F, 0.0F, -3.8902F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-4.3582F, 0.0F, 0.4139F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(3.046F, 0.0F, 4.5599F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(9.2866F, 0.0F, 6.9642F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.5F, KeyframeAnimations.degreeVec(11.9802F, 0.0F, 6.7084F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(10.0977F, 0.0F, 3.8902F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_2_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(11.3556F, 0.0F, 7.164F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(11.4672F, 0.0F, 5.3733F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(7.1987F, 0.0F, 1.5302F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(0.1805F, 0.0F, -2.8974F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-6.9066F, 0.0F, -6.2183F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-11.3556F, 0.0F, -7.164F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-11.4672F, 0.0F, -5.3733F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-7.1987F, 0.0F, -1.5302F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-0.1805F, 0.0F, 2.8974F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.5F, KeyframeAnimations.degreeVec(6.9066F, 0.0F, 6.2183F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(11.3556F, 0.0F, 7.164F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_2_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(4.0199F, 0.0F, 5.0163F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(9.898F, 0.0F, 7.0942F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(11.9955F, 0.0F, 6.4623F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(9.5111F, 0.0F, 3.3621F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(3.3938F, 0.0F, -1.0224F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-4.0199F, 0.0F, -5.0163F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-9.898F, 0.0F, -7.0942F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-11.9955F, 0.0F, -6.4623F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-9.5111F, 0.0F, -3.3621F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.5F, KeyframeAnimations.degreeVec(-3.3938F, 0.0F, 1.0224F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(4.0199F, 0.0F, 5.0163F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(11.9F, 0.0F, 1.926F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(10.5361F, 0.0F, -2.5197F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(5.1478F, 0.0F, -6.0029F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(-2.2068F, 0.0F, -7.1932F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(-8.7185F, 0.0F, -5.636F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-11.9F, 0.0F, -1.926F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-10.5361F, 0.0F, 2.5197F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-5.1478F, 0.0F, 6.0029F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(2.2068F, 0.0F, 7.1932F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.5F, KeyframeAnimations.degreeVec(8.7185F, 0.0F, 5.636F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(11.9F, 0.0F, 1.926F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_3_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(6.186F, 0.0F, 6.6316F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(11.0486F, 0.0F, 3.7171F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(11.691F, 0.0F, -0.6173F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(7.8678F, 0.0F, -4.7159F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(1.0394F, 0.0F, -7.0131F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(-6.186F, 0.0F, -6.6316F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-11.0486F, 0.0F, -3.7171F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-11.691F, 0.0F, 0.6173F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-7.8678F, 0.0F, 4.7159F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.5F, KeyframeAnimations.degreeVec(-1.0394F, 0.0F, 7.0131F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(6.186F, 0.0F, 6.6316F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_3_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-4.2094F, 0.0F, 6.3186F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(3.1998F, 0.0F, 7.1408F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.0F, KeyframeAnimations.degreeVec(9.3867F, 0.0F, 5.2355F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(1.5F, KeyframeAnimations.degreeVec(11.9883F, 0.0F, 1.3304F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.0F, KeyframeAnimations.degreeVec(10.0107F, 0.0F, -3.0829F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(2.5F, KeyframeAnimations.degreeVec(4.2094F, 0.0F, -6.3186F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.0F, KeyframeAnimations.degreeVec(-3.1998F, 0.0F, -7.1408F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(3.5F, KeyframeAnimations.degreeVec(-9.3867F, 0.0F, -5.2355F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.0F, KeyframeAnimations.degreeVec(-11.9883F, 0.0F, -1.3304F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(4.5F, KeyframeAnimations.degreeVec(-10.0107F, 0.0F, 3.0829F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(5.0F, KeyframeAnimations.degreeVec(-4.2094F, 0.0F, 6.3186F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();
    }

    private static AnimationDefinition open() {
        return AnimationDefinition.Builder.withLength(0.5F)
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.08F, KeyframeAnimations.posVec(0.0F, 0.4F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.18F, KeyframeAnimations.posVec(0.0F, -0.2F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("pupil", new AnimationChannel(AnimationChannel.Targets.SCALE,
                new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.05F, 1.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.3F, KeyframeAnimations.scaleVec(1.5F, 1.05F, 1.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.scaleVec(1.35F, 1.05F, 1.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_0_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.22F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(-38.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-42.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_0_b", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_0_c", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.28F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_1_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(-38.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-42.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_1_b", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_1_c", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.28F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_2_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.22F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(-38.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-42.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_2_b", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_2_c", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.28F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_3_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(-38.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-42.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_3_b", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_3_c", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.28F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_4_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.22F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(-38.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-42.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_4_b", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_4_c", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.28F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_5_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-50.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.36F, KeyframeAnimations.degreeVec(-38.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-42.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_5_b", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-18.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-12.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_5_c", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.28F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();
    }

    private static AnimationDefinition mark() {
        return AnimationDefinition.Builder.withLength(0.25F)
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.POSITION,
                new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.posVec(0.0F, 0.0F, -0.5F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("pupil", new AnimationChannel(AnimationChannel.Targets.SCALE,
                new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.15F, 1.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.scaleVec(0.3F, 1.15F, 1.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_0_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_1_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_2_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_3_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_4_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_5_a", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(-14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_0", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.25F, KeyframeAnimations.degreeVec(22.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();
    }

    private static AnimationDefinition drift() {
        return AnimationDefinition.Builder.withLength(0.5F)
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(-7.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(-7.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_0_c", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_1_c", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_2_c", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_3_c", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_4_c", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("petal_5_c", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(8.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_0", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(32.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(32.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_0_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_0_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_1", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(29.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(29.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_1_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_1_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(26.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(26.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_2_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_2_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(23.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(23.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_3_2", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(14.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .addAnimation("tendril_3_3", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                new Keyframe(0.0F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM),
                new Keyframe(0.5F, KeyframeAnimations.degreeVec(10.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.CATMULLROM)
            ))
            .build();
    }
}
