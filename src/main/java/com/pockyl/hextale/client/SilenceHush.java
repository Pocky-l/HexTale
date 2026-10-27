package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.hextale.block.NullLanternBlockEntity;

import org.jspecify.annotations.Nullable;

/**
 * The hush of a Null Lantern with Silence: a sound that starts inside its quiet is played quieter (or not at all with
 * the deepest Silence). Sounds that follow something (tickable ones), music, sounds without a place and the player's own
 * interface stay.
 */
public final class SilenceHush {
    private SilenceHush() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SilenceHush::onPlay);
    }

    private static void onPlay(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        Minecraft minecraft = Minecraft.getInstance();
        if (sound == null || minecraft.level == null || sound instanceof TickableSoundInstance || sound.isRelative()
                || sound.getAttenuation() == SoundInstance.Attenuation.NONE || sound.getSource() == SoundSource.MUSIC
                || sound.getSource() == SoundSource.MASTER || sound.getSource() == SoundSource.UI) {
            return;
        }
        float left = NullLanternBlockEntity.soundLeft(minecraft.level, new Vec3(sound.getX(), sound.getY(), sound.getZ()));
        if (left >= 1) {
            return;
        }
        event.setSound(left <= 0 ? null : new Muffled(sound, left));
    }

    /** A sound played at a share of its volume. */
    private record Muffled(SoundInstance sound, float left) implements SoundInstance {
        @Override
        public Identifier getIdentifier() {
            return sound.getIdentifier();
        }

        @Override
        public @Nullable WeighedSoundEvents getOrResolve(SoundManager soundManager) {
            return sound.getOrResolve(soundManager);
        }

        @Override
        public @Nullable Sound getSound() {
            return sound.getSound();
        }

        @Override
        public @Nullable WeighedSoundEvents getSoundEvent() {
            return sound.getSoundEvent();
        }

        @Override
        public SoundSource getSource() {
            return sound.getSource();
        }

        @Override
        public boolean isLooping() {
            return sound.isLooping();
        }

        @Override
        public boolean isRelative() {
            return sound.isRelative();
        }

        @Override
        public int getDelay() {
            return sound.getDelay();
        }

        @Override
        public float getVolume() {
            return sound.getVolume() * left;
        }

        @Override
        public float getPitch() {
            return sound.getPitch();
        }

        @Override
        public double getX() {
            return sound.getX();
        }

        @Override
        public double getY() {
            return sound.getY();
        }

        @Override
        public double getZ() {
            return sound.getZ();
        }

        @Override
        public SoundInstance.Attenuation getAttenuation() {
            return sound.getAttenuation();
        }

        @Override
        public boolean canStartSilent() {
            return sound.canStartSilent();
        }

        @Override
        public boolean canPlaySound() {
            return sound.canPlaySound();
        }
    }
}
