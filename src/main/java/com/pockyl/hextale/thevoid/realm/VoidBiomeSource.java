package com.pockyl.hextale.thevoid.realm;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/** The Void's zones as biomes: the ring round the nearest vortex a place lies in ({@link VoidZones}). */
public class VoidBiomeSource extends BiomeSource {
    public static final MapCodec<VoidBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.CODEC.fieldOf("threshold").forGetter(s -> s.threshold),
            Biome.CODEC.fieldOf("ribcage_reach").forGetter(s -> s.reach),
            Biome.CODEC.fieldOf("swallowed_shoals").forGetter(s -> s.shoals),
            Biome.CODEC.fieldOf("husk_drift").forGetter(s -> s.drift),
            Biome.CODEC.fieldOf("vortex").forGetter(s -> s.vortex)
    ).apply(i, i.stable(VoidBiomeSource::new)));

    private final Holder<Biome> threshold;
    private final Holder<Biome> reach;
    private final Holder<Biome> shoals;
    private final Holder<Biome> drift;
    private final Holder<Biome> vortex;

    public VoidBiomeSource(Holder<Biome> threshold, Holder<Biome> reach, Holder<Biome> shoals, Holder<Biome> drift, Holder<Biome> vortex) {
        this.threshold = threshold;
        this.reach = reach;
        this.shoals = shoals;
        this.drift = drift;
        this.vortex = vortex;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(threshold, reach, shoals, drift, vortex);
    }

    @Override
    public BiomeResolver createResolver(Climate.Sampler sampler) {
        return (quartX, quartY, quartZ) -> biome(QuartPos.toBlock(quartX), QuartPos.toBlock(quartZ));
    }

    public Holder<Biome> biome(int x, int z) {
        return switch (VoidZones.zone(x, z)) {
            case THRESHOLD -> threshold;
            case REACH -> reach;
            case SHOALS -> shoals;
            case DRIFT -> drift;
            case VORTEX -> vortex;
        };
    }
}
