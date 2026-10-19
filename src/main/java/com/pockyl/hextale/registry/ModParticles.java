package com.pockyl.hextale.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Hextale.MOD_ID);

    /**
     * A glowing mote that flies along an arc into the point it was spawned at: spawn it AT the destination with the
     * offset to its start as speed (like the enchanting table's glyphs). Coloured: essence streams, pedestal draws.
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> FLOW = colored("flow");

    /**
     * A shard of a rune's hieroglyph winding along a corkscrew into the point it was spawned at (spawned like
     * {@link #FLOW}). Rune steles feed the altar with them.
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> RUNE_SHARD = colored("rune_shard");

    /** A glowing leaf of the magic tree drifting down out of its crown. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MAGIC_LEAF = PARTICLES.register("magic_leaf",
            () -> new SimpleParticleType(false));

    private static DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> colored(String name) {
        return PARTICLES.register(name, () -> new ParticleType<ColorParticleOption>(false) {
            @Override
            public MapCodec<ColorParticleOption> codec() {
                return ColorParticleOption.codec(this);
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() {
                return ColorParticleOption.streamCodec(this);
            }
        });
    }

    private ModParticles() {
    }

    public static ColorParticleOption flow(int rgb) {
        return ColorParticleOption.create(FLOW.get(), 0xFF000000 | rgb);
    }

    public static ColorParticleOption runeShard(int rgb) {
        return ColorParticleOption.create(RUNE_SHARD.get(), 0xFF000000 | rgb);
    }

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
