package com.pockyl.hextale.block;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.SimpleRune;

import org.jetbrains.annotations.Nullable;

/**
 * Outcome of one fusion attempt at the Research Table.
 *
 * @param rune         the complex rune, when fused
 * @param fresh        whether the rune was new to the codex
 * @param action       the radical read in the action circle, if any
 * @param target       the radical read in the target circle, if any
 * @param missing      what the table lacks, when {@link Status#MISSING}
 */
public record FusionResult(Status status, @Nullable ComplexRune rune, boolean fresh, @Nullable SimpleRune action,
                           @Nullable SimpleRune target, @Nullable Requirement missing) {
    public static final StreamCodec<ByteBuf, FusionResult> STREAM_CODEC = StreamCodec.of(FusionResult::write, FusionResult::read);

    public enum Status {
        /** A complex rune was written into the codex and onto a scroll. */
        FUSED,
        /** The same element twice: the orb cracked. */
        CRACKED,
        /** The server did not recognise the drawing in at least one circle. */
        UNRECOGNISED,
        /** Something is missing on the table; see {@link #missing()}. */
        MISSING
    }

    public static FusionResult fused(ComplexRune rune, boolean fresh) {
        return new FusionResult(Status.FUSED, rune, fresh, rune.action(), rune.target(), null);
    }

    public static FusionResult cracked(SimpleRune element) {
        return new FusionResult(Status.CRACKED, null, false, element, element, null);
    }

    public static FusionResult unrecognised(@Nullable SimpleRune action, @Nullable SimpleRune target) {
        return new FusionResult(Status.UNRECOGNISED, null, false, action, target, null);
    }

    public static FusionResult missing(Requirement requirement) {
        return new FusionResult(Status.MISSING, null, false, null, null, requirement);
    }

    private static void write(ByteBuf buf, FusionResult result) {
        buf.writeByte(result.status.ordinal());
        buf.writeByte(result.rune == null ? -1 : result.rune.index());
        buf.writeBoolean(result.fresh);
        buf.writeByte(result.action == null ? -1 : result.action.ordinal());
        buf.writeByte(result.target == null ? -1 : result.target.ordinal());
        buf.writeByte(result.missing == null ? -1 : result.missing.ordinal());
    }

    private static FusionResult read(ByteBuf buf) {
        Status status = Status.values()[buf.readByte()];
        int rune = buf.readByte();
        boolean fresh = buf.readBoolean();
        int action = buf.readByte();
        int target = buf.readByte();
        int missing = buf.readByte();
        return new FusionResult(status, rune < 0 ? null : ComplexRunes.all().get(rune), fresh,
                action < 0 ? null : SimpleRune.values()[action], target < 0 ? null : SimpleRune.values()[target],
                missing < 0 ? null : Requirement.values()[missing]);
    }
}
