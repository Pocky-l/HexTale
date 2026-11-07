package com.pockyl.hextale.thevoid.boss;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModSounds;

import java.util.UUID;

/**
 * What the Prosector wrote down: a jar of membrane and chitin holding a copy of the thing that hurt it, hung in the air a
 * way off and tied to its body by a glowing sinew. While it lives its kind does little; break it, with anything at all, and the
 * Prosector forgets. The renderer takes the jar's place from here and the sinew from {@link #ownerId()}.
 */
public class Specimen extends Mob {
    /** Blocks a tick it flies at to its place. */
    private static final double FLIGHT = 0.7;
    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(Specimen.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(Specimen.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_COPY = SynchedEntityData.defineId(Specimen.class, EntityDataSerializers.ITEM_STACK);
    private @Nullable UUID owner;
    private Vec3 shelf = Vec3.ZERO;

    public Specimen(EntityType<? extends Specimen> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        this.noPhysics = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KIND, 0);
        builder.define(DATA_OWNER, -1);
        builder.define(DATA_COPY, ItemStack.EMPTY);
    }

    public void setup(Prosector boss, StudyKind kind, ItemStack copy, Vec3 shelf) {
        owner = boss.getUUID();
        entityData.set(DATA_OWNER, boss.getId());
        entityData.set(DATA_KIND, kind.ordinal());
        entityData.set(DATA_COPY, copy);
        this.shelf = shelf;
        var health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(ProsectorConfig.SPECIMEN_HEALTH.get());
        }
        setHealth(getMaxHealth());
    }

    public StudyKind kind() {
        return StudyKind.byId(entityData.get(DATA_KIND));
    }

    /** The copy inside the jar. */
    public ItemStack copy() {
        return entityData.get(DATA_COPY);
    }

    /** The Prosector's id, for the sinew; -1 before it is known. */
    public int ownerId() {
        return entityData.get(DATA_OWNER);
    }

    public Vec3 shelf() {
        return shelf;
    }

    public void flyTo(Vec3 shelf) {
        this.shelf = shelf;
    }

    private @Nullable Prosector owner(ServerLevel level) {
        return owner != null && level.getEntity(owner) instanceof Prosector boss ? boss : null;
    }

    @Override
    public void tick() {
        noPhysics = true;
        super.tick();
    }

    @Override
    protected void customServerAiStep() {
        ServerLevel level = (ServerLevel) level();
        Vec3 delta = shelf.subtract(position());
        double length = delta.length();
        setDeltaMovement(length < 0.05 ? Vec3.ZERO : delta.scale(Math.min(FLIGHT, length) / length));
        if (tickCount % 20 == 0) {
            Prosector boss = owner(level);
            if (boss == null || !boss.isAlive()) {
                discard();
            } else if (entityData.get(DATA_OWNER) != boss.getId()) {
                entityData.set(DATA_OWNER, boss.getId());
            }
        }
    }

    /** Anything breaks it, but the fight's own hits do not count. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !(source.getEntity() instanceof LivingEntity living && Prosector.hittable(living))) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        boolean first = !dead;
        super.die(source);
        if (first && level() instanceof ServerLevel level) {
            Prosector boss = owner(level);
            if (boss != null) {
                boss.forget(level, kind(), position());
            }
        }
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
    }

    /** It shatters at once: a jar has no death throes. */
    @Override
    protected void tickDeath() {
        if (!level().isClientSide() && !isRemoved()) {
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void push(double x, double y, double z) {
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
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.PROSECTOR_SPECIMEN_HIT.get();
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, registryAccess());
        super.addAdditionalSaveData(output.tag());
        output.putInt("kind", kind().ordinal());
        output.storeNullable("owner", UUIDUtil.CODEC, owner);
        output.store("shelf", Vec3.CODEC, shelf);
        output.store("copy", ItemStack.OPTIONAL_CODEC, copy());
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag portTag) {
        ValueInput input = ValueInput.of(portTag, registryAccess());
        super.readAdditionalSaveData(input.tag());
        entityData.set(DATA_KIND, input.getIntOr("kind", 0));
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        shelf = input.read("shelf", Vec3.CODEC).orElse(position());
        entityData.set(DATA_COPY, input.read("copy", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }
}
