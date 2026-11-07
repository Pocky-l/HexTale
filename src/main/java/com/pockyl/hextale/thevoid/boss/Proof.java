package com.pockyl.hextale.thevoid.boss;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModSounds;

import java.util.UUID;

/**
 * The Prosector's conclusion: a wrong-jointed copy of the player who hurt it most, with their skin, armour and weapon. It
 * walks and strikes by itself; the mimic attacks are the Prosector's, worked through its body. Its gear is an image and
 * never drops. Its death brings the Prosector down.
 */
public class Proof extends Monster {
    /** 1.21.1 has no serializer for a profile: the model player's id, by which clients find the skin. */
    private static final EntityDataAccessor<java.util.Optional<UUID>> DATA_PROFILE = SynchedEntityData.defineId(Proof.class, EntityDataSerializers.OPTIONAL_UUID);
    private @Nullable UUID owner;

    public Proof(EntityType<? extends Proof> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 9.0).add(Attributes.FOLLOW_RANGE, 48.0).add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ARMOR, 8.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PROFILE, java.util.Optional.empty());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
    }

    /** Takes on the look of {@code model}; with nobody to copy it stays a faceless body. */
    public void copyOf(Prosector boss, @Nullable ServerPlayer model) {
        owner = boss.getUUID();
        var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(ProsectorConfig.PROOF_HEALTH.get());
        }
        setHealth(getMaxHealth());
        var attack = getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            attack.setBaseValue(ProsectorConfig.PROOF_DAMAGE.get());
        }
        if (model == null) {
            return;
        }
        entityData.set(DATA_PROFILE, java.util.Optional.of(model.getUUID()));
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR || slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                setItemSlot(slot, model.getItemBySlot(slot).copy());
            }
        }
        setTarget(model);
    }

    public @Nullable UUID profile() {
        return entityData.get(DATA_PROFILE).orElse(null);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        ServerLevel level = (ServerLevel) level();
        if (target instanceof net.minecraft.world.entity.LivingEntity living && Prosector.hittable(living)) {
            float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
            return ProsectorDamage.strike(level, living, this, damage);
        }
        return false;
    }

    @Override
    protected void dropEquipment() {
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean ignorePassenger) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageModifier, DamageSource source) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.PROSECTOR_PROOF_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PROSECTOR_PROOF_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, registryAccess());
        super.addAdditionalSaveData(output.tag());
        output.storeNullable("profile", UUIDUtil.CODEC, profile());
        output.storeNullable("owner", UUIDUtil.CODEC, owner);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueInput input = ValueInput.of(portTag, registryAccess());
        super.readAdditionalSaveData(input.tag());
        input.read("profile", UUIDUtil.CODEC).ifPresent(profile -> entityData.set(DATA_PROFILE, java.util.Optional.of(profile)));
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
    }

    /** Whether it still belongs to a running fight: one left over from an earlier one is just removed. */
    public boolean owned(ServerLevel level) {
        return owner != null && level.getEntity(owner) instanceof Prosector boss && boss.proving(this);
    }

    @Override
    protected void customServerAiStep() {
        ServerLevel level = (ServerLevel) level();
        super.customServerAiStep();
        if (tickCount % 40 == 0 && !owned(level)) {
            discard();
        }
    }

    /** Copies of what the model wore are worth nothing. */
    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public boolean wantsToPickUp(ItemStack stack) {
        return false;
    }
}
