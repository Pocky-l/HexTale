package com.pockyl.hextale.block;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

/**
 * The crystals that drive the hextech devices against the Void (the Hextech Purifier and the Void Anchors): a hextech
 * crystal cut at the lapidary, or a crystal grown in shimmer - chemtech's cheap one, made without the lapidary's skill.
 * The grown crystal holds only {@value #SYNTHETIC_SHARE} of what a cut one does and, spent, crumbles to dust instead of
 * clouding over (so it never becomes a Clouded Crystal).
 */
public final class DeviceCrystals {
    /** Share of a cut crystal's hold that a crystal grown in shimmer has. */
    public static final float SYNTHETIC_SHARE = 0.4F;

    private DeviceCrystals() {
    }

    /** Whether the stack can power such a device. */
    public static boolean fits(ItemStack stack) {
        return stack.is(ModItems.HEXTECH_CRYSTAL.get()) || synthetic(stack);
    }

    public static boolean synthetic(ItemStack stack) {
        return stack.is(ModItems.SYNTHETIC_CRYSTAL.get());
    }

    /** How much of a cut crystal's hold the crystal has. */
    public static float share(ItemStack stack) {
        return synthetic(stack) ? SYNTHETIC_SHARE : 1;
    }

    /** A spent crystal grown in shimmer crumbles: shards and dust burst from it, a pinch of hextech dust falls. */
    public static void crumble(ServerLevel level, Vec3 at) {
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModItems.SYNTHETIC_CRYSTAL.get())), at.x, at.y, at.z,
                14, 0.12, 0.12, 0.12, 0.08);
        level.sendParticles(ModParticles.glint(0x9FE6FF), at.x, at.y, at.z, 6, 0.2, 0.2, 0.2, 0.02);
        level.playSound(null, at.x, at.y, at.z, ModSounds.RUNE_CRUMBLE.get(), SoundSource.BLOCKS, 0.8F, 0.9F);
        Containers.dropItemStack(level, at.x, at.y, at.z, new ItemStack(ModItems.HEXTECH_DUST.get()));
    }
}
