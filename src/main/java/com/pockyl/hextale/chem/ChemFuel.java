package com.pockyl.hextale.chem;

import com.mojang.serialization.Codec;
import com.pockyl.hextale.GearConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;

/**
 * The fuels a Chem Tank takes and how strong they make chemtech gear. The better the fuel, the stronger the gear: Ember
 * Oil gives hextech level 2 strength, raw shimmer level 3, refined shimmer level 4 (the stats are read from
 * {@link GearConfig}). An empty tank divides every stat by {@link GearConfig#EMPTY_DIVISOR}. A tank holds one fuel at a
 * time; using the gear never adds Thinning (owner, 2026-11-02).
 */
public enum ChemFuel implements StringRepresentable {
    EMBER_OIL(Brew.EMBER_OIL),
    RAW_SHIMMER(Brew.RAW_SHIMMER),
    REFINED_SHIMMER(Brew.REFINED_SHIMMER);

    public static final Codec<ChemFuel> CODEC = StringRepresentable.fromEnum(ChemFuel::values);
    public static final StreamCodec<ByteBuf, ChemFuel> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], ChemFuel::ordinal);

    private final Brew brew;

    ChemFuel(Brew brew) {
        this.brew = brew;
    }

    public Brew brew() {
        return brew;
    }

    @Override
    public String getSerializedName() {
        return brew.id();
    }

    /** 1 (Ember Oil) to 3 (refined shimmer). */
    public int tier() {
        return ordinal() + 1;
    }

    /** The hextech tool level whose stats this fuel gives (tier 1 = level 2 ... tier 3 = level 4). */
    public GearConfig.ToolLevel tools() {
        return GearConfig.HEXTECH_TOOLS[tier()];
    }

    /** The Bulwark level whose protection this fuel gives. */
    public GearConfig.ArmourLevel armour() {
        return GearConfig.BULWARK[tier()];
    }

    /** The fuel a vial of this brew is, or null for brews a tank does not take. */
    @Nullable
    public static ChemFuel of(@Nullable Brew brew) {
        for (ChemFuel fuel : values()) {
            if (fuel.brew == brew) {
                return fuel;
            }
        }
        return null;
    }

    /** Actions a full tank pays for. */
    public static int tankActions() {
        return GearConfig.TANK_VIALS.get() * GearConfig.ACTIONS_PER_VIAL.get();
    }
}
