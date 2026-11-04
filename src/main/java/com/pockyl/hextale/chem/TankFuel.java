package com.pockyl.hextale.chem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a Chem Tank holds: one fuel and how many actions of it are left (a vial is
 * {@link com.pockyl.hextale.GearConfig#ACTIONS_PER_VIAL} actions). Item component {@code hextale:chem_tank}; the placed
 * tank keeps the same record.
 */
public record TankFuel(ChemFuel fuel, int units) {
    public static final TankFuel EMPTY = new TankFuel(ChemFuel.EMBER_OIL, 0);

    public static final Codec<TankFuel> CODEC = RecordCodecBuilder.create(i -> i.group(
            ChemFuel.CODEC.fieldOf("fuel").forGetter(TankFuel::fuel),
            Codec.INT.fieldOf("units").forGetter(TankFuel::units)
    ).apply(i, TankFuel::new));
    public static final StreamCodec<ByteBuf, TankFuel> STREAM_CODEC = StreamCodec.composite(
            ChemFuel.STREAM_CODEC, TankFuel::fuel, ByteBufCodecs.VAR_INT, TankFuel::units, TankFuel::new);

    public boolean isEmpty() {
        return units <= 0;
    }

    /** Share of a full tank, 0-1. */
    public float level() {
        return Math.min(1, units / (float) ChemFuel.tankActions());
    }

    /** Whole vials' worth left (rounded up), for tooltips. */
    public int vials() {
        int per = com.pockyl.hextale.GearConfig.ACTIONS_PER_VIAL.get();
        return (units + per - 1) / per;
    }

    /** The liquid's colour: the fuel's brew. */
    public int color() {
        return fuel.brew().color();
    }
}
