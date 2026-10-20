package com.pockyl.hextale.chem;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * A chemtech brew: what a boiling Crucible turns its water into when a catalyst meets the essences a brew recipe asks
 * for. Brews are bottled into Chem Vials. The fuel feeds a Chem Burner (and any furnace); the others are thrown and
 * splash ({@link BrewEffects}).
 */
public enum Brew implements StringRepresentable {
    /** Combustible mix: fuel for the Chem Burner and for furnaces. */
    FUEL(0xE39A2E, false),
    /** Growth tonic: plants around shoot up, young animals grow up. */
    GROWTH(0x47C74C, true),
    /** Coolant: puts fire out, freezes lava into stone. */
    COOLANT(0x7FD9F0, true),
    /** Glow mix: green patches of cold light where it splashes. */
    GLOW(0x9CFF6A, true),
    /** Acid: eats away stone, rusts iron, burns monsters. */
    ACID(0xC9EE24, true);

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
