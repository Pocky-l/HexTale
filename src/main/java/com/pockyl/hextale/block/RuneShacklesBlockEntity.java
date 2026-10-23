package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.farm.ShackleEvents;
import com.pockyl.hextale.registry.ModAttachments;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A Rune Shackles plate's state and work. Once a second it asks the level for the creatures standing over it (one small
 * box query) and holds up to {@value #CAPACITY} of them in its glyph: they are lifted a hair off the floor and skip their
 * ticks entirely ({@link ShackleEvents}) until the hold runs out. A Hex Arc Spire or a player can still strike them.
 *
 * <p>Holding costs shadow essence: one mB drawn out of a jar within {@link JarReach#RADIUS} holds one creature for
 * {@value #HOLD_PER_MB} ticks. Without shadow the plate goes dark and lets them go.
 */
public class RuneShacklesBlockEntity extends BlockEntity {
    /** Creatures one plate holds. */
    public static final int CAPACITY = 4;
    /** Creature-ticks of holding one mB of shadow pays for. */
    public static final int HOLD_PER_MB = 600;
    public static final int DRAW_MB = 4;
    /** Ticks between two looks; a hold lasts a little longer so it never lapses between them. */
    public static final int PERIOD = 20;
    private static final int HOLD = PERIOD + 10;
    /** How far over the plate the held hang. */
    public static final double LIFT = 0.2;

    /** Creature-ticks of holding paid for. */
    private int budget;
    private boolean dark;
    private int wait;
    /** The creatures held now (entity ids; the clients draw the glyph under them). */
    private int[] held = new int[0];
    private long lookedAt = -1;

    public RuneShacklesBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RUNE_SHACKLES.get(), pos, state);
    }

    public int budget() {
        return budget;
    }

    /** Whether something stood on it and it had no shadow to hold it with. */
    public boolean dark() {
        return dark;
    }

    public int[] held() {
        return held;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RuneShacklesBlockEntity plate) {
        if (--plate.wait > 0) {
            return;
        }
        plate.wait = PERIOD;
        plate.hold((ServerLevel) level);
    }

    /** The space over the plate where creatures are held: its own block and the one above. */
    public static AABB space(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY() + 0.05, pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
    }

    /** Holds (or keeps holding) the creatures over the plate, as many as it can pay for. */
    void hold(ServerLevel level) {
        lookedAt = level.getGameTime();
        Vec3 centre = Vec3.atBottomCenterOf(worldPosition);
        List<Mob> over = level.getEntitiesOfClass(Mob.class, space(worldPosition), RuneShacklesBlockEntity::holdable);
        over.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(centre)));
        List<Integer> now = new ArrayList<>();
        long until = level.getGameTime() + HOLD;
        boolean caught = false;
        for (Mob mob : over) {
            if (now.size() >= CAPACITY) {
                break;
            }
            if (budget < PERIOD && !drawShadow(level)) {
                break;
            }
            budget -= PERIOD;
            boolean fresh = !mob.hasData(ModAttachments.SHACKLE);
            mob.setData(ModAttachments.SHACKLE, new ShackleEvents.Shackle(worldPosition.asLong(), until));
            if (fresh) {
                // Lifted into the glyph and still (a held creature does not tick, so it does not fall either).
                mob.setDeltaMovement(Vec3.ZERO);
                mob.setPos(mob.getX(), worldPosition.getY() + 2.0 / 16 + LIFT, mob.getZ());
                caught = true;
            }
            now.add(mob.getId());
        }
        // Dark only while there is someone to hold and no shadow to hold them with.
        setDark(now.size() < Math.min(over.size(), CAPACITY));
        if (caught) {
            level.playSound(null, centre.x, centre.y, centre.z, ModSounds.SHACKLE_BIND.get(), SoundSource.BLOCKS, 0.5F,
                    0.9F + level.getRandom().nextFloat() * 0.2F);
        }
        int[] ids = now.stream().mapToInt(Integer::intValue).toArray();
        if (!Arrays.equals(ids, held)) {
            held = ids;
            changed();
        } else if (caught) {
            setChanged();
        }
    }

    /**
     * A creature has just stepped onto the plate: it is caught at once, not at the next look - a walking creature
     * crosses a plate in a few ticks. Nothing happens while the plate is full or dark (the looks deal with those), and
     * at most one look a tick however many step on.
     */
    public void steppedOn(ServerLevel level, Mob mob) {
        if (!dark && held.length < CAPACITY && !mob.hasData(ModAttachments.SHACKLE) && holdable(mob)
                && lookedAt != level.getGameTime()) {
            hold(level);
        }
    }

    /** Any creature but a boss and a player's own tamed one. */
    public static boolean holdable(Mob mob) {
        return mob.isAlive() && !mob.isDeadOrDying() && !mob.typeHolder().is(Tags.EntityTypes.BOSSES)
                && !(mob instanceof net.minecraft.world.entity.TamableAnimal tame && tame.isTame());
    }

    private boolean drawShadow(ServerLevel level) {
        for (EssenceTankBlockEntity jar : JarReach.around(level, worldPosition)) {
            int taken = Math.min(DRAW_MB, jar.tank().contents().get(Essence.SHADOW));
            if (taken <= 0) {
                continue;
            }
            jar.tank().remove(EssenceMix.pure(Essence.SHADOW, taken));
            jar.setChanged();
            budget += taken * HOLD_PER_MB;
            Vec3 mouth = JarReach.mouth(jar.getBlockPos());
            level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.3F, 0.7F);
            setChanged();
            return true;
        }
        return false;
    }

    private void setDark(boolean value) {
        if (dark != value) {
            dark = value;
            changed();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        budget = input.getIntOr("budget", 0);
        dark = input.getBooleanOr("dark", false);
        held = input.getIntArray("held").orElse(new int[0]);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("budget", budget);
        output.putBoolean("dark", dark);
    }

    /** The clients get the held creatures too (entity ids are only good for this session, so they are never saved). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.putIntArray("held", held);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
