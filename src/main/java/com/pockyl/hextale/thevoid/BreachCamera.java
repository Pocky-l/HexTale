package com.pockyl.hextale.thevoid;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;

/**
 * The eye a player watches a breach open through: a bodiless entity the client's camera follows while the cinematic
 * flies it (it has the player's eye height, so the camera does not dip when it is handed over). Never added to a level.
 */
public class BreachCamera extends Entity {
    public BreachCamera(EntityType<? extends BreachCamera> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueInput input = ValueInput.of(portTag, registryAccess());
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, registryAccess());
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        return false;
    }
}
