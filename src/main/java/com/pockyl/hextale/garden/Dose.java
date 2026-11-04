package com.pockyl.hextale.garden;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * What a bed of the garden is soaked with: steeped soil, a Chem Planter and an Essence Sprinkler hold doses of one kind.
 * Every time a plant on the bed grows a stage it may spend a dose and grow one more. Growth tonic is the common dose,
 * sap essence the cheap and weak one, raw shimmer the strongest. {@link #NONE} is a bed with nothing in it (steeped soil
 * that ran dry still never dries out or gets trampled).
 */
public enum Dose implements StringRepresentable {
    NONE(0x6B4A2E, 0, 0),
    GROWTH(0x94C23E, 0.6F, 0.4F),
    SAP(0xD08A3A, 0.35F, 0.25F),
    SHIMMER(0xB053BD, 0.85F, 0.6F);

    public static final Codec<Dose> CODEC = StringRepresentable.fromEnum(Dose::values);
    /** Doses a vial of growth tonic or raw shimmer pours into a bed. */
    public static final int PER_VIAL = 4;
    /** mB of sap essence a dose of sap takes. */
    public static final int SAP_MB = 4;
    /** A vial or a bottle poured on farmland steeps the 3x3 round the block it is poured on. */
    public static final int POUR_RADIUS = 1;

    private final int color;
    private final float extraStage;
    private final float harvest;

    Dose(int color, float extraStage, float harvest) {
        this.color = color;
        this.extraStage = extraStage;
        this.harvest = harvest;
    }

    /** The chance that a ripe harvest from a bed holding this dose comes with its crop twice. */
    public float harvest() {
        return harvest;
    }

    /** The colour the soil and the planter's liquid show. */
    public int color() {
        return color;
    }

    /** The chance that a plant on the bed, having grown a stage, grows one more on this dose. */
    public float extraStage() {
        return extraStage;
    }

    /** The dose a vial pours (growth tonic, raw shimmer), null for other vials and items. */
    public static @Nullable Dose ofVial(ItemStack stack) {
        Brew brew = ChemVialItem.brew(stack);
        if (brew == Brew.GROWTH) {
            return GROWTH;
        }
        return brew == Brew.RAW_SHIMMER ? SHIMMER : null;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return id();
    }

    public String translationKey() {
        return "dose.hextale." + id();
    }
}
