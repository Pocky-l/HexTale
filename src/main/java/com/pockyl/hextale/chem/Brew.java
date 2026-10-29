package com.pockyl.hextale.chem;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * A chemtech brew: what a boiling Crucible turns its water into when a catalyst meets the essences a brew recipe asks
 * for. Brews are bottled into Chem Vials. The fuel feeds a Chem Burner (and any furnace); the splash brews are thrown
 * ({@link BrewEffects}). Shimmer comes in two grades: raw, boiled from shimmerbloom and an axolotl's regenerative slime,
 * burns long in a burner and goes into the Chem Still; refined, out of the still, is drunk for strength and burns
 * longest of all.
 */
public enum Brew implements StringRepresentable {
    /** Combustible mix: fuel for the Chem Burner and for furnaces. */
    FUEL(0xE39A2E, false),
    /** Growth tonic: plants around shoot up, young animals grow up. */
    GROWTH(0x47C74C, true),
    /** Coolant: puts fire out, freezes lava into stone. */
    COOLANT(0x7FD9F0, true),
    /** Glow mix: green patches of cold light where it splashes. */
    GLOW(0xA6DC5C, true),
    /** Acid: eats away stone, rusts iron, burns monsters. */
    ACID(0xBFD83C, true),
    /** Raw shimmer: a cloudy violet fuel, and what the Chem Still refines. */
    RAW_SHIMMER(0x9A4CD0, false),
    /** Refined shimmer: glowing magenta, drunk for strength; the longest-burning fuel there is. */
    REFINED_SHIMMER(0xF052D8, false);

    /** Ticks of flame a vial of combustible mix gives a Chem Burner (a blaze rod's worth). */
    public static final int FUEL_TICKS = 2400;

    public static final Codec<Brew> CODEC = StringRepresentable.fromEnum(Brew::values);
    public static final StreamCodec<ByteBuf, Brew> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Brew::ordinal);

    private final int color;
    private final boolean thrown;

    Brew(int color, boolean thrown) {
        this.color = color;
        this.thrown = thrown;
    }

    /** The liquid's colour, in the crucible and in the vial. */
    public int color() {
        return color;
    }

    /** Whether a vial of it is thrown to splash (fuel is not). */
    public boolean thrown() {
        return thrown;
    }

    /** Ticks of flame a vial of it gives a Chem Burner; 0 for brews that do not burn. */
    public int burnTicks() {
        return switch (this) {
            case FUEL -> FUEL_TICKS;
            case RAW_SHIMMER -> FUEL_TICKS * 2;
            case REFINED_SHIMMER -> FUEL_TICKS * 4;
            default -> 0;
        };
    }

    /** Whether it burns in the pink flame of shimmer. */
    public boolean shimmer() {
        return this == RAW_SHIMMER || this == REFINED_SHIMMER;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return id();
    }

    public String translationKey() {
        return "brew.hextale." + id();
    }

    /** One line: what the vial does. */
    public String useKey() {
        return translationKey() + ".use";
    }
}
