package com.pocky.hextale.common.world.entity.projectile;

import com.pocky.hextale.common.register.ModEntities;
import com.pocky.hextale.common.register.ModItems;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

public class ThrownHextech extends ThrowableItemProjectile {

    public ThrownHextech(EntityType<? extends ThrownHextech> p_37473_, Level p_37474_) {
        super(p_37473_, p_37474_);
    }

    public ThrownHextech(Level p_37481_, LivingEntity p_37482_) {
        super(ModEntities.HEXTECH.get(), p_37482_, p_37481_);
    }

    public ThrownHextech(Level p_37476_, double p_37477_, double p_37478_, double p_37479_) {
        super(ModEntities.HEXTECH.get(), p_37477_, p_37478_, p_37479_, p_37476_);
    }

    private ParticleOptions getParticle() {
        ItemStack itemstack = this.getItemRaw();
        return itemstack.isEmpty() ? ParticleTypes.ITEM_SNOWBALL : new ItemParticleOption(ParticleTypes.ITEM, itemstack);
    }

    public void handleEntityEvent(byte b) {
        if (b == 3) {
            ParticleOptions particleoptions = this.getParticle();

            for(int i = 0; i < 8; ++i) {
                this.level().addParticle(particleoptions, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    protected void onHitEntity(@NotNull EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        entityHitResult.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
    }

    protected void onHit(@NotNull HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide) {

            this.level().explode(this, this.getX(), this.getY(0.0625D), this.getZ(), 4.0F, Level.ExplosionInteraction.TNT);

            this.discard();
        }

    }

    protected @NotNull Item getDefaultItem() {
        return ModItems.RAW_HEXTECH_CRYSTAL.get();
    }
}
