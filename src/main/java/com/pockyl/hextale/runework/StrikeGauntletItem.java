package com.pockyl.hextale.runework;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.pockyl.hextale.network.WeaponFxPayload;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.world.WorldEdits;

/**
 * The Strike Gauntlet (late, radiant level): a heavy brass fist with piston knuckles. A full swing of the fist hits hard
 * and strikes the ground round the target, hurting what stands there. Held, it charges in three stages (the knuckles
 * light one by one); released, it throws its wearer along the look ({@link StrikeDash}), further with every stage. At
 * the end of that path it punches: a block breaks - a 3 x 3 face from the second stage, two deep at the third - and a
 * shockwave throws the creatures ahead. A wearer on a mount cannot fly and punches on the spot. Held too long it vents
 * its steam and starts over - it never hurts its wearer. One in each hand doubles every blow. It takes the sword's
 * runes.
 */
public class StrikeGauntletItem extends HextechToolItem {
    /** Ticks per charge stage. */
    public static final int STAGE_TICKS = 15;
    public static final int STAGES = 3;
    /** Held this long past full charge, it vents and starts over. */
    public static final int VENT_TICKS = 60;
    /** A fist's blow: a radiant sword deals 10.5 at 1.6 swings a second, a riftforged one 14. */
    public static final float FIST_DAMAGE = 12;
    public static final float FIST_SPEED = 1.2F;
    /** What a full swing deals to the creatures round the target, and how far round. */
    public static final float AREA_DAMAGE = 7;
    public static final double AREA_RADIUS = 3;
    /** Damage of the shockwave by stage. */
    public static final float[] WAVE_DAMAGE = {5, 8, 12};
    /** How far the shockwave reaches by stage, and its push. */
    public static final double[] WAVE_REACH = {4, 5.5, 7};
    private static final double[] WAVE_PUSH = {0.8, 1.3, 1.9};
    /** Blocks harder than this are left alone (obsidian and the like). */
    public static final float MAX_HARDNESS = 10;
    private static final double BLOCK_REACH = 4.5;
    private static final int REST_TICKS = 20;

    public StrikeGauntletItem(Properties properties) {
        super(properties);
    }

    /** The fist's blow in the main hand. */
    public static ItemAttributeModifiers attributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, FIST_DAMAGE - 1, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, FIST_SPEED - 4, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** One gauntlet in each hand doubles every blow. */
    public static boolean paired(Player player) {
        return player.getMainHandItem().getItem() instanceof StrikeGauntletItem && player.getOffhandItem().getItem() instanceof StrikeGauntletItem;
    }

    static float power(Player player) {
        return paired(player) ? 2 : 1;
    }

    @Override
    public float getAttackDamageBonus(Entity victim, float damage, DamageSource source) {
        return source.getEntity() instanceof Player player && paired(player) ? damage : 0;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity mob, LivingEntity attacker) {
        if (!(attacker instanceof Player player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.getAttackStrengthScale(0.5F) > 0.9F) {
            fistArea(level, player, mob);
        }
        if (paired(player)) {
            player.getOffhandItem().hurtAndBreak(1, player, InteractionHand.OFF_HAND);
        }
    }

    /** A full swing: the ground heaves round the target and the creatures there are hurt and thrown off it. */
    private static void fistArea(ServerLevel level, Player player, LivingEntity struck) {
        Vec3 centre = struck.position().add(0, struck.getBbHeight() / 2, 0);
        float damage = AREA_DAMAGE * power(player);
        List<LivingEntity> around = level.getEntitiesOfClass(LivingEntity.class, struck.getBoundingBox().inflate(AREA_RADIUS),
                target -> target != struck && ToolEffects.mayStrike(target, player)
                        && target.position().add(0, target.getBbHeight() / 2, 0).distanceToSqr(centre) <= AREA_RADIUS * AREA_RADIUS);
        ToolEffects.quietly(() -> {
            for (LivingEntity target : around) {
                if (target.hurtServer(level, level.damageSources().playerAttack(player), damage)) {
                    Vec3 away = target.position().subtract(struck.position()).multiply(1, 0, 1);
                    away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                    target.push(away.x * 0.7, 0.3, away.z * 0.7);
                    target.needsSync = true;
                }
            }
        });
        WeaponFxPayload.send(level, WeaponFxPayload.Kind.BURST, centre, centre, ModParticles.HEXTECH, (float) AREA_RADIUS);
        WeaponRunes.quake(level, struck.position(), (int) AREA_RADIUS, ModParticles.HEXTECH, 1);
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.STRIKE_GAUNTLET_FIST.get(), SoundSource.PLAYERS, 0.45F, 1);
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
        if (!player.isPassenger()) {
            StrikeDash.start(player, stage);
        } else if (level instanceof ServerLevel server) {
            punch(server, player, stage);
        }
        if (level instanceof ServerLevel) {
            InteractionHand hand = player.getItemInHand(InteractionHand.OFF_HAND) == stack ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            player.getCooldowns().addCooldown(stack, REST_TICKS);
            stack.hurtAndBreak(stage, player, hand);
            if (paired(player)) {
                InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                player.getItemInHand(other).hurtAndBreak(stage, player, other);
            }
        }
        return true;
    }

    /** The punch without a flight, from a saddle: a block within reach breaks, otherwise a shockwave runs out in front. */
    private static void punch(ServerLevel level, Player player, int stage) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(BLOCK_REACH)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.STRIKE_GAUNTLET_IMPACT.get(), SoundSource.PLAYERS, 0.5F, 1.1F);
        if (hit.getType() == HitResult.Type.BLOCK) {
            smash(level, player, hit.getBlockPos(), hit.getDirection(), stage);
        } else {
            wave(level, player, eye, look, stage, power(player), false, Set.of());
        }
    }

    /** Breaks the struck block; a 3 x 3 face from stage 2, two deep at stage 3. Block entities and hard blocks stay. */
    static void smash(ServerLevel level, Player player, BlockPos centre, Direction face, int stage) {
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

    /**
     * A shockwave from {@code from}: the creatures within reach are hurt and thrown - those ahead of it, or all round
     * when it comes off a wall ({@code round}). The ones in {@code spared} already took their blow.
     */
    static void wave(ServerLevel level, Player player, Vec3 from, Vec3 dir, int stage, float power, boolean round, Set<Integer> spared) {
        double reach = WAVE_REACH[stage - 1];
        List<LivingEntity> caught = level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(reach),
                target -> !spared.contains(target.getId()) && ToolEffects.mayStrike(target, player));
        ToolEffects.quietly(() -> {
            for (LivingEntity target : caught) {
                Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0).subtract(from);
                if (to.length() > reach || !round && to.normalize().dot(dir) < 0.6) {
                    continue;
                }
                if (target.hurtServer(level, level.damageSources().playerAttack(player), WAVE_DAMAGE[stage - 1] * power)) {
                    Vec3 push = to.normalize().scale(WAVE_PUSH[stage - 1]);
                    target.push(push.x, 0.3 + 0.1 * stage, push.z);
                    target.needsSync = true;
                }
            }
        });
        if (!round) {
            for (int i = 1; i <= (int) reach; i++) {
                Vec3 at = from.add(dir.scale(i)).add(0, -0.3, 0);
                level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.hextale.strike_gauntlet.hint").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.strike_gauntlet.controls").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
