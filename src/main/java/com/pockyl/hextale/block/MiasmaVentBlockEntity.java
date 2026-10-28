package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDamageTypes;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.Thinning;

import java.util.List;

/**
 * A Miasma Vent's work - the cheap, dirty way to clear a mob farm. Built once, it breathes for ever and burns nothing.
 * Every {@value #INTERVAL} ticks it asks the level once for the hostile creatures in its cloud ({@value #RADIUS} blocks to
 * each side, one below and {@value #HEIGHT} above) and, if there are any, breathes out fumes that poison them and eat
 * {@value #DAMAGE} health away - the undead too, whom poison does not touch. Players, animals and pets breathe it
 * unharmed. No player struck them, so they drop no experience and no rare loot. Nothing to breathe on = it waits longer
 * (up to {@value #MAX_SLEEP} ticks); a redstone signal holds it shut. Clients draw a low lingering haze over the cloud.
 */
public class MiasmaVentBlockEntity extends BlockEntity {
    public static final int INTERVAL = 40;
    public static final int MAX_SLEEP = 80;
    public static final float DAMAGE = 2;
    /** Poison I, renewed every breath, so a creature that stays in the cloud stays poisoned. */
    public static final int POISON_TICKS = 70;
    /** The cloud's reach: blocks to each side, one below the vent and this many above it. */
    public static final int RADIUS = 4;
    public static final int HEIGHT = 3;
    /** Block event: a breath of fumes. */
    public static final int PUFF = 1;
    /** Fumes, sickly green, and the soot they leave. */
    public static final int FUMES = 0x7E9A4E;
    public static final int SOOT = 0x3A4A34;
    /** Where the fumes leave the stack: the gap under its cowl, sideways between the struts. */
    private static final double MOUTH_Y = 13 / 16.0;
    /** Players farther than this see no haze: nobody would notice it, and it costs particles. */
    private static final double HAZE_SIGHT = 40;

    private int wait;
    private int idle;

    public MiasmaVentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MIASMA_VENT.get(), pos, state);
    }

    /** The space the fumes fill. */
    public static AABB cloud(BlockPos pos) {
        return new AABB(pos.getX() - RADIUS, pos.getY() - 1, pos.getZ() - RADIUS, pos.getX() + RADIUS + 1, pos.getY() + HEIGHT + 1,
                pos.getZ() + RADIUS + 1);
    }

    public static boolean shut(BlockState state) {
        return state.getValue(MiasmaVentBlock.POWERED);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MiasmaVentBlockEntity vent) {
        if (--vent.wait > 0) {
            return;
        }
        if (shut(state)) {
            vent.wait = INTERVAL;
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
        for (Mob mob : targets) {
            mob.hurt(ModDamageTypes.miasma(server), DAMAGE);
            if (mob.isAlive()) {
                mob.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_TICKS, 0));
            }
        }
        server.blockEvent(pos, state.getBlock(), PUFF, 0);
        Thinning.add(server, pos, Thinning.MIASMA_PUFF);
        server.playSound(null, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, ModSounds.MIASMA_HISS.get(), SoundSource.BLOCKS, 0.6F,
                0.9F + server.getRandom().nextFloat() * 0.2F);
    }

    /**
     * Client: while the vent breathes, a low green-grey haze lies over its whole cloud - a veil of slow, wide wisps hugging
     * the ground, so the player sees where the fumes reach - and fumes curl out sideways from under its cowl.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, MiasmaVentBlockEntity vent) {
        if (shut(state)) {
            return;
        }
        RandomSource random = level.getRandom();
        if (random.nextInt(3) == 0 && level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, HAZE_SIGHT, false) != null) {
            double x = pos.getX() + 0.5 + (random.nextDouble() * 2 - 1) * (RADIUS + 0.4);
            double z = pos.getZ() + 0.5 + (random.nextDouble() * 2 - 1) * (RADIUS + 0.4);
            BlockPos cell = BlockPos.containing(x, pos.getY(), z);
            // The haze settles on the floor of the cloud: the vent's own level, or a step lower where the ground drops.
            double y = level.getBlockState(cell).isCollisionShapeFullBlock(level, cell) ? cell.getY() + 1.1 : cell.getY() + 0.15;
            level.addParticle(ModParticles.haze(random.nextInt(5) == 0 ? SOOT : FUMES, 0.7F), x, y + random.nextDouble() * 0.4, z,
                    (random.nextDouble() - 0.5) * 0.004, 0, (random.nextDouble() - 0.5) * 0.004);
        }
        if (random.nextInt(5) == 0) {
            double angle = random.nextDouble() * Math.PI * 2;
            level.addParticle(ModParticles.vapour(FUMES), pos.getX() + 0.5 + Math.cos(angle) * 0.2, pos.getY() + MOUTH_Y,
                    pos.getZ() + 0.5 + Math.sin(angle) * 0.2, Math.cos(angle) * 0.025, 0.006, Math.sin(angle) * 0.025);
        }
    }

    /** Client: a breath rolls out from under the cowl and spreads low through the cloud, settling as soot. */
    @Override
    public boolean triggerEvent(int id, int param) {
        if (id != PUFF) {
            return super.triggerEvent(id, param);
        }
        if (level != null && level.isClientSide()) {
            RandomSource random = level.getRandom();
            double x = worldPosition.getX() + 0.5;
            double y = worldPosition.getY() + MOUTH_Y;
            double z = worldPosition.getZ() + 0.5;
            for (int i = 0; i < 40; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = 0.06 + random.nextDouble() * 0.12;
                level.addParticle(ModParticles.vapour(i % 4 == 0 ? SOOT : FUMES), x + Math.cos(angle) * 0.2, y,
                        z + Math.sin(angle) * 0.2, Math.cos(angle) * speed, random.nextDouble() * 0.02 - 0.005, Math.sin(angle) * speed);
            }
        }
        return true;
    }
}
