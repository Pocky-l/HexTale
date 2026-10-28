package com.pockyl.hextale.port.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A 26.x keyframe animation baked onto a model's parts: 1.21.1's {@code KeyframeAnimations.animate} without the
 * hierarchical model, the parts found once.
 *
 * <p>port-of: net.minecraft.client.animation.KeyframeAnimation
 */
public final class KeyframeAnimation {
    private record Bone(ModelPart part, List<AnimationChannel> channels) {
    }

    private final AnimationDefinition definition;
    private final List<Bone> bones = new ArrayList<>();
    private final Vector3f cache = new Vector3f();

    private KeyframeAnimation(AnimationDefinition definition, ModelPart root) {
        this.definition = definition;
        for (Map.Entry<String, List<AnimationChannel>> entry : definition.boneAnimations().entrySet()) {
            String name = entry.getKey();
            root.getAllParts().filter(part -> part.hasChild(name)).findFirst()
                    .ifPresent(parent -> bones.add(new Bone(parent.getChild(name), entry.getValue())));
        }
    }

    /** The 26.x {@code definition.bake(root)}. */
    public static KeyframeAnimation bake(AnimationDefinition definition, ModelPart root) {
        return new KeyframeAnimation(definition, root);
    }

    public void apply(long accumulatedTime, float scale) {
        float seconds = accumulatedTime / 1000.0F;
        float time = definition.looping() ? seconds % definition.lengthInSeconds() : seconds;
        for (Bone bone : bones) {
            for (AnimationChannel channel : bone.channels()) {
                Keyframe[] keyframes = channel.keyframes();
                int from = Math.max(0, Mth.binarySearch(0, keyframes.length, i -> time <= keyframes[i].timestamp()) - 1);
                int to = Math.min(keyframes.length - 1, from + 1);
                float progress = to == from ? 0.0F
                        : Mth.clamp((time - keyframes[from].timestamp()) / (keyframes[to].timestamp() - keyframes[from].timestamp()), 0.0F, 1.0F);
                keyframes[to].interpolation().apply(cache, progress, keyframes, from, to, scale);
                channel.target().apply(bone.part(), cache);
            }
        }
    }

    /** Like vanilla's walk animation: the walk position drives the time, its speed the strength. */
    public void applyWalk(float walkAnimationPos, float walkAnimationSpeed, float timeMultiplier, float speedMultiplier) {
        apply((long) (walkAnimationPos * 50.0F * timeMultiplier), Math.min(walkAnimationSpeed * speedMultiplier, 1.0F));
    }
}
