package com.pockyl.hextale.runework;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.world.WorldEdits;

/**
 * The Strike Gauntlet (late, radiant level): a heavy brass fist with piston knuckles. Held, it charges in three stages
 * (the knuckles light one by one); released, it punches. At a block within reach it breaks the block - a 3 x 3 face from
 * the second stage, two deep at the third; into the air or a creature it sends a shockwave that hurts and throws the
 * creatures in front. Held too long it vents its steam and starts over - it never hurts its wearer. It takes the
 * sword's runes.
 */
public class StrikeGauntletItem extends HextechToolItem {
    /** Ticks per charge stage. */
    public static final int STAGE_TICKS = 15;
    public static final int STAGES = 3;
    /** Held this long past full charge, it vents and starts over. */
    public static final int VENT_TICKS = 60;
    /** Damage of the shockwave by stage. */
    public static final float[] WAVE_DAMAGE = {5, 8, 12};
    /** How far the shockwave reaches by stage, and its push. */
    public static final double[] WAVE_REACH = {4, 5.5, 7};
    private static final double[] WAVE_PUSH = {0.8, 1.3, 1.9};
    /** Blocks harder than this are left alone (obsidian and the like). */
    public static final float MAX_HARDNESS = 10;
    private static final double BLOCK_REACH = 4.5;
    private static final int REST_TICKS = 12;

    public StrikeGauntletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand == InteractionHand.MAIN_HAND && WeaponRunes.strength(stack, ToolRune.BLINK) > 0) {
            return super.use(level, player, hand);
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    /** Charge stage 0-3 after {@code held} ticks. */
    public static int stage(int held) {
        return Math.min(STAGES, held / STAGE_TICKS);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        int held = getUseDuration(stack, user) - remaining;
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 fist = user.getEyePosition().add(user.getLookAngle().scale(0.6)).add(0, -0.3, 0);
        if (held > 0 && held % STAGE_TICKS == 0 && held <= STAGES * STAGE_TICKS) {
            server.sendParticles(ModParticles.glint(ModParticles.HEXTECH), fist.x, fist.y, fist.z, 3 + held / STAGE_TICKS * 3, 0.1, 0.1, 0.1, 0.02);
            server.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS,
                    0.25F, 0.8F + 0.15F * (held / STAGE_TICKS));
        }
        if (held >= STAGES * STAGE_TICKS + VENT_TICKS) {
            // Over-held: the steam goes out of the vents and the charge starts over.
            server.sendParticles(ModParticles.vapour(ModParticles.SOOT), fist.x, fist.y, fist.z, 12, 0.2, 0.2, 0.2, 0.04);
            server.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.3F, 1.2F);
            user.stopUsingItem();
            if (user instanceof Player player) {
                player.getCooldowns().addCooldown(stack, REST_TICKS);
            }
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        int stage = stage(getUseDuration(stack, entity) - remainingTime);
        if (stage == 0 || !(entity instanceof Player player)) {
            return false;
        }
        if (level instanceof ServerLevel server) {
            punch(server, player, stack, stage);
            player.getCooldowns().addCooldown(stack, REST_TICKS);
            stack.hurtAndBreak(stage, player, InteractionHand.MAIN_HAND);
        }
        return true;
    }

    /** The punch: a block within reach breaks, otherwise a shockwave runs out in front. */
    static void punch(ServerLevel level, Player player, ItemStack stack, int stage) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(BLOCK_REACH)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.RITUAL_ABSORB.get(), SoundSource.PLAYERS, 0.5F, 0.5F);
        if (hit.getType() == HitResult.Type.BLOCK) {
            smash(level, player, hit.getBlockPos(), hit.getDirection(), stage);
        } else {
            wave(level, player, eye, look, stage);
        }
    }

    /** Breaks the struck block; a 3 x 3 face from stage 2, two deep at stage 3. Block entities and hard blocks stay. */
    private static void smash(ServerLevel level, Player player, BlockPos centre, Direction face, int stage) {
        int radius = stage >= 2 ? 1 : 0;
        int depth = stage >= 3 ? 2 : 1;
        Direction.Axis axis = face.getAxis();
        for (int d = 0; d < depth; d++) {
            BlockPos layer = centre.relative(face.getOpposite(), d);
            for (int a = -radius; a <= radius; a++) {
                for (int b = -radius; b <= radius; b++) {
                    BlockPos pos = switch (axis) {
                        case X -> layer.offset(0, a, b);
                        case Y -> layer.offset(a, 0, b);
                        case Z -> layer.offset(a, b, 0);
                    };
                    BlockState state = level.getBlockState(pos);
                    float hardness = state.getDestroySpeed(level, pos);
                    if (state.isAir() || hardness < 0 || hardness > MAX_HARDNESS || level.getBlockEntity(pos) != null) {
                        continue;
                    }
                    WorldEdits.destroy(level, pos, true, player);
                }
            }
        }
        Vec3 at = Vec3.atCenterOf(centre);
        level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), at.x, at.y, at.z, 1, 0, 0, 0, 0);
    }

    /** A shockwave in front: the creatures within reach and before the fist are hurt and thrown. */
    private static void wave(ServerLevel level, Player player, Vec3 eye, Vec3 look, int stage) {
        double reach = WAVE_REACH[stage - 1];
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(reach),
                target -> ToolEffects.mayStrike(target, player))) {
            Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0).subtract(eye);
            if (to.length() > reach || to.normalize().dot(look) < 0.6) {
                continue;
            }
            if (target.hurtServer(level, level.damageSources().playerAttack(player), WAVE_DAMAGE[stage - 1])) {
                Vec3 push = to.normalize().scale(WAVE_PUSH[stage - 1]);
                target.push(push.x, 0.3 + 0.1 * stage, push.z);
                target.needsSync = true;
            }
        }
        for (int i = 1; i <= (int) reach; i++) {
            Vec3 at = eye.add(look.scale(i)).add(0, -0.3, 0);
            level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }
}
