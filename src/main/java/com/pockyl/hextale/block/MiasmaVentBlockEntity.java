package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A Miasma Vent's state and work - the cheap, dirty way to clear a mob farm. Every {@value #INTERVAL} ticks it asks the
 * level once for the hostile creatures over it and, if there are any, breathes out a cloud of fumes that eats at all of
 * them ({@value #DAMAGE} damage). It burns the unknown essence the separator leaves behind: one mB = {@value #PUFFS_PER_MB}
 * breaths. No player struck them, so they drop no experience and no rare loot - the price of a crystal saved. Nothing to
 * breathe on = it waits longer and longer (up to {@value #MAX_SLEEP} ticks).
 */
public class MiasmaVentBlockEntity extends BlockEntity {
    public static final int INTERVAL = 40;
    public static final int MAX_SLEEP = 80;
    public static final float DAMAGE = 3;
    public static final int PUFFS_PER_MB = 4;
    public static final int DRAW_MB = 4;
    /** The cloud's reach: blocks to each side and up over the vent. */
    public static final int RADIUS = 2;
    public static final int HEIGHT = 3;
    /** Block event: a breath of fumes. */
    public static final int PUFF = 1;
    /** Fumes, sickly green, and the soot they leave. */
    public static final int FUMES = 0x7E9A4E;
    public static final int SOOT = 0x3A4A34;

    private int charges;
    private boolean dry;
    private int wait;
    private int idle;

    public MiasmaVentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIASMA_VENT.get(), pos, state);
    }

    public int charges() {
        return charges;
    }

    /** Whether something stood over it and it had no unknown essence to breathe. */
    public boolean dry() {
        return dry;
    }

    /** The space the fumes fill. */
    public static AABB cloud(BlockPos pos) {
        return new AABB(pos.getX() - RADIUS, pos.getY() + 1, pos.getZ() - RADIUS, pos.getX() + RADIUS + 1, pos.getY() + 1 + HEIGHT,
                pos.getZ() + RADIUS + 1);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MiasmaVentBlockEntity vent) {
        if (--vent.wait > 0) {
            return;
        }
        ServerLevel server = (ServerLevel) level;
        List<Mob> targets = server.getEntitiesOfClass(Mob.class, cloud(pos), ArcSpireBlockEntity::prey);
        if (targets.isEmpty()) {
            vent.idle = Math.min(vent.idle + 1, 2);
            vent.wait = Math.min(MAX_SLEEP, INTERVAL << vent.idle);
            return;
        }
        vent.idle = 0;
        vent.wait = INTERVAL;
        if (vent.charges <= 0 && !vent.drawUnknown(server)) {
            vent.setDry(true);
            return;
        }
        vent.setDry(false);
        vent.charges--;
        for (Mob mob : targets) {
            mob.hurt(server.damageSources().magic(), DAMAGE);
        }
        server.blockEvent(pos, state.getBlock(), PUFF, 0);
        server.playSound(null, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, ModSounds.MIASMA_HISS.get(), SoundSource.BLOCKS, 0.6F,
                0.9F + server.getRandom().nextFloat() * 0.2F);
        vent.setChanged();
    }

    private boolean drawUnknown(ServerLevel level) {
        for (EssenceTankBlockEntity jar : JarReach.around(level, worldPosition)) {
            // A Creative Essence Tank holds every essence but the unknown one; for the vent it is an endless murk too.
            int taken = jar.isCreative() ? DRAW_MB : Math.min(DRAW_MB, jar.tank().contents().unknown());
            if (taken <= 0) {
                continue;
            }
            if (!jar.isCreative()) {
                jar.tank().remove(EssenceMix.unknown(taken));
            }
            jar.setChanged();
            charges += taken * PUFFS_PER_MB;
            Vec3 mouth = JarReach.mouth(jar.getBlockPos());
            level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.3F, 0.6F);
            return true;
        }
        return false;
    }

    private void setDry(boolean value) {
        if (dry != value) {
            dry = value;
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Client: a breath rolls out of the grate and spreads through the cloud's space, settling as soot. */
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id != PUFF) {
            return super.triggerEvent(id, param);
        }
        if (level != null && level.isClientSide()) {
            RandomSource random = level.getRandom();
            double x = worldPosition.getX() + 0.5;
            double y = worldPosition.getY() + 1.05;
            double z = worldPosition.getZ() + 0.5;
            for (int i = 0; i < 36; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = 0.04 + random.nextDouble() * 0.08;
                level.addParticle(ModParticles.vapour(i % 4 == 0 ? SOOT : FUMES), x + (random.nextDouble() - 0.5) * 0.6, y,
                        z + (random.nextDouble() - 0.5) * 0.6, Math.cos(angle) * speed, 0.03 + random.nextDouble() * 0.05,
                        Math.sin(angle) * speed);
            }
        }
        return true;
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        charges = input.getIntOr("charges", 0);
        dry = input.getBooleanOr("dry", false);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        output.putInt("charges", charges);
        output.putBoolean("dry", dry);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
