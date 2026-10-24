package com.pockyl.hextale.port;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;

/** Projectile spawning of Minecraft 26.x. */
public final class PortProjectiles {
    private PortProjectiles() {
    }

    @FunctionalInterface
    public interface Factory<P extends Projectile> {
        P create(ServerLevel level, LivingEntity shooter, ItemStack stack);
    }

    /** {@code Projectile.spawnProjectileFromRotation}: a projectile of one item shot where the shooter looks. */
    public static <P extends Projectile> P spawnProjectileFromRotation(Factory<P> factory, ServerLevel level, ItemStack stack, LivingEntity shooter,
                                                                       float z, float velocity, float inaccuracy) {
        P projectile = factory.create(level, shooter, stack.copyWithCount(1));
        projectile.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), z, velocity, inaccuracy);
        level.addFreshEntity(projectile);
        return projectile;
    }
}
