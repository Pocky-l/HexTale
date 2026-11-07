package com.pockyl.hextale.thevoid.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import com.pockyl.hextale.port.SavedData;
import com.pockyl.hextale.port.SavedDataType;

import com.pockyl.hextale.Hextale;

/** How many Prosectors this world has killed, whatever dimension they fell in: the first one pays the most. */
public final class ProsectorRecord extends SavedData {
    private static final Codec<ProsectorRecord> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("kills").forGetter(record -> record.kills)
    ).apply(i, ProsectorRecord::new));
    public static final SavedDataType<ProsectorRecord> TYPE = new SavedDataType<>(Hextale.id("prosector_record"), ProsectorRecord::new, CODEC);

    private int kills;

    public ProsectorRecord() {
    }

    private ProsectorRecord(int kills) {
        this.kills = kills;
    }

    public static ProsectorRecord of(ServerLevel level) {
        return TYPE.get(level.getServer().overworld().getDataStorage());
    }

    public int kills() {
        return kills;
    }

    public void addKill() {
        kills++;
        setDirty();
    }
}
