package com.pockyl.hextale.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.BrewEffects;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.registry.ModEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;

/** A thrown Chem Vial: it shatters where it lands and splashes its brew ({@link BrewEffects}). */
public class ThrownChemVial extends ThrowableItemProjectile {
    public ThrownChemVial(EntityType<? extends ThrownChemVial> type, Level level) {
        super(type, level);
    }

    public ThrownChemVial(Level level, LivingEntity thrower, ItemStack stack) {
        super(ModEntities.THROWN_CHEM_VIAL.get(), thrower, level);
        setItem(stack);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.CHEM_VIAL.get();
    }

    /** Heavier than a snowball: it arcs like a splash potion. */
    @Override
    protected double getDefaultGravity() {
        return 0.05;
    }

    @Override
    public void tick() {
        super.tick();
        Brew brew = ChemVialItem.brew(getItem());
        if (level().isClientSide() && brew != null && tickCount % 2 == 0) {
            level().addParticle(ModParticles.drop(brew.color()), getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel level) {
            Vec3 at = hit instanceof BlockHitResult block
                    ? hit.getLocation().add(net.minecraft.world.phys.Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(0.25)) : hit.getLocation();
            Brew brew = ChemVialItem.brew(getItem());
            if (brew != null) {
                BrewEffects.splash(level, at, brew, getOwner(),
                        com.pockyl.hextale.world.WorldEdits.actor(level, getOwner(), ownerUUID));
            }
            discard();
        }
    }
}
