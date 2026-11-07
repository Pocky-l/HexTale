package com.pockyl.hextale.thevoid.boss;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * The plug of floor Excision lifts: a hexagon of table carried by the move itself, which sets where it is. Only an
 * image: the hit is the move's. It lives as long as the move and is never saved.
 */
public class HexPlug extends Entity {
    /** It should never outlive its move; this is the net under a move that stopped running. */
    private static final int LIFETIME = 200;

    public HexPlug(EntityType<? extends HexPlug> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setNoGravity(true);
    }

    /** Eases towards a point. */
    public void moveTo(Vec3 to, double share) {
        setPos(position().lerp(to, share));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount > LIFETIME) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueInput input = ValueInput.of(portTag, registryAccess());
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, registryAccess());
    }
}
