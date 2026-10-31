package com.pockyl.hextale.registry;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.hextale.Hextale;

/**
 * The mod's particles. Most take a colour ({@link ColorParticleOption}): their sprites are white and the colour tints
 * them. Spawned with a count of 0 (or {@code addParticle}), the speed arguments are the particle's velocity.
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Hextale.MOD_ID);

    /** Hextech blue: crystals, sparks, the altar. */
    public static final int HEXTECH = 0x5CC8F2;
    /** Hextech light, almost white: glints of crystals and finished work. */
    public static final int HEX_LIGHT = 0xCFF3FF;
    /** Warm gold: the magic tree, wax and polish, a job done well. */
    public static final int GOLD = 0xFFD27A;
    /** Clean white steam of boiling water and stills. */
    public static final int STEAM = 0xF4F6F8;
    /** Warm grey smoke of fuel and burners. */
    public static final int SMOKE = 0x7A7068;
    /** Heavy dark smoke of a blast or burnt paper. */
    public static final int SOOT = 0x3C3632;
    /** Plain water (axolotl tank, pumps). */
    public static final int WATER = 0xA6DCF6;
    /** An ordinary flame. */
    public static final int FIRE = 0xFFA24A;
    /** A cold blue flame of hextech fuel. */
    public static final int SOUL_FIRE = 0x7BE6F2;
    /** Shimmer's violet-pink. */
    public static final int SHIMMER = 0xE07AF0;
    /** Research ink. */
    public static final int INK = 0x3A2C58;

    /**
     * A glowing mote that flies along an arc into the point it was spawned at: spawn it AT the destination with the
     * offset to its start as speed (like the enchanting table's glyphs). Essence streams, pedestal draws.
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> FLOW = colored("flow");

    /**
     * A shard of a rune's hieroglyph winding along a corkscrew into the point it was spawned at (spawned like
     * {@link #FLOW}). Rune steles feed the altar with them.
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> RUNE_SHARD = colored("rune_shard");

    /** Living essence: a slow, wobbling blob of glowing liquid that drifts upwards and evaporates. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> ESSENCE_WISP = colored("essence_wisp");

    /** A drop of liquid: falls (or is thrown up and falls) and splashes where it lands. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> ESSENCE_DROP = colored("essence_drop");

    /** A bubble rising through liquid, wobbling, and bursting. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> ESSENCE_BUBBLE = colored("essence_bubble");

    /** A soft puff of steam or smoke that rises, swells and thins out. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> VAPOUR = colored("vapour");

    /** The haze of a thinning land: a wide faint veil that hugs the ground and drifts. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> THINNING_HAZE = colored("thinning_haze");

    /** A crackle of hextech lightning: a short-lived flickering spark. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> HEX_SPARK = colored("hex_spark");

    /** A four-pointed glint that twinkles once: polish, magic, a finished job. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> GLINT = colored("glint");

    /**
     * An expanding ring of light. Velocity x = the ring's final radius in blocks (0 = 1), y &gt; 0 lays it flat on the
     * ground, otherwise it faces the camera.
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> HEX_RING = colored("hex_ring");

    /** A tongue of flame licking upwards. */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> HEX_FLAME = colored("hex_flame");

    /** A leaf of the magic tree drifting down out of its crown. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MAGIC_LEAF = PARTICLES.register("magic_leaf",
            () -> new SimpleParticleType(false));

    /** A crystal shard of a cracked Bulwark heart, tumbling as it falls (spawned with its velocity, count 0). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HEART_SHARD = PARTICLES.register("heart_shard",
            () -> new SimpleParticleType(false));

    /** A soft violet-pink mote breathed out by shimmerbloom and anything burning or holding shimmer. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHIMMER_MOTE = PARTICLES.register("shimmer_mote",
            () -> new SimpleParticleType(false));

    /**
     * A mote pulled into the Void: spawned AT the point it falls into with the offset to its start as speed, it
     * drifts at first, swirls, then is dragged in faster and faster and vanishes.
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> VOID_MOTE = colored("void_mote");

    /** A mote of the Void's own air, rising slowly from the abyss (the Void's biomes spawn it round the player). */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> VOID_DRIFT = colored("void_drift");

    /**
     * A fragment of an item's own texture drawn into the altar: spawned AT the altar's item with the offset to the item it
     * comes from as velocity (client: {@code ItemDrawParticle}).
     */
    public static final DeferredHolder<ParticleType<?>, ParticleType<ItemParticleOption>> ITEM_DRAW = PARTICLES.register("item_draw",
            () -> new ParticleType<ItemParticleOption>(false) {
                @Override
                public MapCodec<ItemParticleOption> codec() {
                    return ItemParticleOption.codec(this);
                }

                @Override
                public StreamCodec<? super RegistryFriendlyByteBuf, ItemParticleOption> streamCodec() {
                    return ItemParticleOption.streamCodec(this);
                }
            });

    public static ItemParticleOption itemDraw(ItemStack item) {
        return new ItemParticleOption(ITEM_DRAW.get(), item.getItem());
    }

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

    private static ColorParticleOption of(DeferredHolder<ParticleType<?>, ParticleType<ColorParticleOption>> type, int rgb) {
        return ColorParticleOption.create(type.get(), 0xFF000000 | rgb);
    }

    public static ColorParticleOption flow(int rgb) {
        return of(FLOW, rgb);
    }

    public static ColorParticleOption runeShard(int rgb) {
        return of(RUNE_SHARD, rgb);
    }

    public static ColorParticleOption wisp(int rgb) {
        return of(ESSENCE_WISP, rgb);
    }

    public static ColorParticleOption drop(int rgb) {
        return of(ESSENCE_DROP, rgb);
    }

    public static ColorParticleOption bubble(int rgb) {
        return of(ESSENCE_BUBBLE, rgb);
    }

    public static ColorParticleOption vapour(int rgb) {
        return of(VAPOUR, rgb);
    }

    /** Haze of colour {@code rgb}; {@code density} (0-1) makes the veil thicker. */
    public static ColorParticleOption haze(int rgb, float density) {
        int alpha = Math.round(net.minecraft.util.Mth.clamp(density, 0, 1) * 255);
        return ColorParticleOption.create(THINNING_HAZE.get(), alpha << 24 | rgb);
    }

    public static ColorParticleOption spark(int rgb) {
        return of(HEX_SPARK, rgb);
    }

    public static ColorParticleOption glint(int rgb) {
        return of(GLINT, rgb);
    }

    public static ColorParticleOption ring(int rgb) {
        return of(HEX_RING, rgb);
    }

    public static ColorParticleOption voidMote(int rgb) {
        return of(VOID_MOTE, rgb);
    }

    public static ColorParticleOption flame(int rgb) {
        return of(HEX_FLAME, rgb);
    }

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
    }
}
