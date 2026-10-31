package com.pockyl.hextale.thevoid.realm;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

/**
 * A Pit Maw: a mouth of the Void lying flush in the skin of a plate. Shut, it is a low bulge with a ring of short spines
 * (the tell) and one can walk over it. When someone steps on it or right beside it, it tears open into a funnel of
 * petals and its throat pulls them toward the middle and bites. It lets go when it has been hurt enough or nobody is
 * left in reach, folds shut and lies in wait again. It never moves from its place.
 */
public class PitMaw extends Monster {
    public static final int HIDDEN = 0;
    public static final int OPEN = 1;
    public static final int CLOSING = 2;
    /** Ticks it takes to tear open / to fold shut. */
    public static final int OPEN_TICKS = 10;
    public static final int CLOSE_TICKS = 30;
    /** Longest it keeps pulling. */
    private static final int PULL_TICKS = 200;
    /** Ticks after shutting before it can open again. */
    private static final int REST_TICKS = 60;
    /** Damage that makes it let go. */
    private static final float FLINCH = 8;
    /** Reach of its pull (sideways) and of its bite. */
    private static final double PULL_REACH = 3.2;
    private static final double BITE_REACH = 1.0;
    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(PitMaw.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_OPEN = SynchedEntityData.defineId(PitMaw.class, EntityDataSerializers.FLOAT);
    private float openO;
    private int timer;
    private float hurtWhileOpen;
    private int empty;

    public PitMaw(EntityType<? extends PitMaw> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.FOLLOW_RANGE, 8.0).add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, (byte) HIDDEN);
        builder.define(DATA_OPEN, 0.0F);
    }

    public int state() {
        return entityData.get(DATA_STATE);
    }

    /** 0 shut (a bulge in the ground) -> 1 torn fully open. */
    public float open(float partialTick) {
        return Mth.lerp(partialTick, openO, entityData.get(DATA_OPEN));
    }

    /** Open and drawing someone in. */
    public boolean isPulling() {
        return state() == OPEN;
    }

    @Override
    public void tick() {
        openO = entityData.get(DATA_OPEN);
        setDeltaMovement(Vec3.ZERO);
        super.tick();
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        timer++;
        switch (state()) {
            case HIDDEN -> lurk(level);
            case OPEN -> pull(level);
            default -> {
                setOpen(Math.max(0, 1 - timer / (float) CLOSE_TICKS));
                if (timer >= CLOSE_TICKS + REST_TICKS) {
                    setState(HIDDEN);
                }
            }
        }
    }

    private void lurk(ServerLevel level) {
        if (timer % 4 != 0) {
            return;
        }
        AABB reach = getBoundingBox().inflate(0.9, 1.4, 0.9).move(0, 0.6, 0);
        if (!level.getEntitiesOfClass(Player.class, reach, PitMaw::prey).isEmpty()) {
            setState(OPEN);
            hurtWhileOpen = 0;
            empty = 0;
            playSound(ModSounds.PIT_MAW_OPEN.get(), 1.2F, 0.9F + random.nextFloat() * 0.15F);
            level.sendParticles(ModParticles.voidMote(0x3E1838), getX(), getY() + 0.6, getZ(), 24, 0.9, 0.2, 0.9, 0.05);
        }
    }

    private static boolean prey(Player player) {
        return !player.isSpectator() && !player.isCreative() && player.isAlive();
    }

    private void pull(ServerLevel level) {
        setOpen(Math.min(1, timer / (float) OPEN_TICKS));
        Vec3 throat = new Vec3(getX(), getY() + 0.2, getZ());
        boolean anyone = false;
        AABB reach = new AABB(getX() - PULL_REACH, getY() - 1, getZ() - PULL_REACH, getX() + PULL_REACH, getY() + 2.5, getZ() + PULL_REACH);
        for (Player player : level.getEntitiesOfClass(Player.class, reach, PitMaw::prey)) {
            Vec3 to = throat.subtract(player.position());
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            if (flat > PULL_REACH) {
                continue;
            }
            anyone = true;
            // Stronger the closer to the throat; a running player can still break away from the rim.
            double strength = 0.035 + 0.05 * (1 - flat / PULL_REACH);
            if (flat > 0.05) {
                player.push(to.x / flat * strength, Math.min(0, to.y) * 0.02, to.z / flat * strength);
                player.needsSync = true;
            }
            if (flat < BITE_REACH && timer % 20 == 10) {
                swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
                doHurtTarget(level, player);
            }
        }
        if (timer % 30 == 0) {
            playSound(ModSounds.PIT_MAW_PULL.get(), 0.9F, 0.9F + random.nextFloat() * 0.2F);
        }
        empty = anyone ? 0 : empty + 1;
        if (timer >= PULL_TICKS || empty > 40 || hurtWhileOpen >= FLINCH) {
            setState(CLOSING);
            playSound(ModSounds.PIT_MAW_CLOSE.get(), 1.0F, 1.0F);
        }
    }

    private void setState(int state) {
        entityData.set(DATA_STATE, (byte) state);
        timer = 0;
        if (state == HIDDEN) {
            setOpen(0);
        }
    }

    private void setOpen(float open) {
        if (entityData.get(DATA_OPEN) != open) {
            entityData.set(DATA_OPEN, open);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt) {
            if (state() == OPEN) {
                hurtWhileOpen += amount;
            } else if (state() == HIDDEN && source.getEntity() instanceof Player) {
                // Struck while lying in wait: it bursts open at once.
                setState(OPEN);
                playSound(ModSounds.PIT_MAW_OPEN.get(), 1.2F, 1.0F);
            }
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit) {
            playSound(ModSounds.PIT_MAW_BITE.get(), 1.0F, 0.9F + random.nextFloat() * 0.15F);
        }
        return hit;
    }

    /** Shut, it is ground: one walks over it. */
    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return isAlive() && state() == HIDDEN;
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
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("maw_state", state() == HIDDEN ? HIDDEN : CLOSING);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setNoGravity(true);
        if (input.getIntOr("maw_state", HIDDEN) != HIDDEN) {
            setState(CLOSING);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return state() == HIDDEN ? null : ModSounds.PIT_MAW_PULL.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.PIT_MAW_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PIT_MAW_DEATH.get();
    }
}
