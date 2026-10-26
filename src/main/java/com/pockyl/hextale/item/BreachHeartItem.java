package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.thevoid.BreachHeart;
import com.pockyl.hextale.thevoid.Breaches;
import com.pockyl.hextale.thevoid.TheVoid;
import com.pockyl.hextale.thevoid.VoidLayout;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A held breach folded into a fist-sized heart. It beats; whoever carries it is slowed by its weight. Used on the ground
 * it unfolds into the same breach - wild again, until a ring of anchors holds it anew. Dropped or lost, it beats faster
 * and faster and after {@link #LOOSE_TICKS} unfolds where it lies.
 */
public class BreachHeartItem extends Item {
    /** Ticks a heart lies loose before it unfolds by itself. */
    public static final int LOOSE_TICKS = 600;
    /** Ticks between two beats while it is carried. */
    public static final int BEAT_TICKS = 60;
    /** No breach may unfold within this many blocks of another. */
    public static final double KEEP_APART = 24;

    public BreachHeartItem(Properties properties) {
        super(properties);
    }

    public static @Nullable BreachHeart heart(ItemStack stack) {
        return stack.get(ModDataComponents.BREACH_HEART.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BreachHeart heart = heart(context.getItemInHand());
        Player player = context.getPlayer();
        if (heart == null || player == null) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos ground = context.getClickedPos();
        if (context.getClickedFace() != Direction.UP || !level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)) {
            player.sendOverlayMessage(Component.translatable("item.hextale.breach_heart.needs_ground"));
            return InteractionResult.FAIL;
        }
        Vec3 centre = centreOver(ground, heart);
        if (!roomAbove(level, ground, heart.layout())) {
            player.sendOverlayMessage(Component.translatable("item.hextale.breach_heart.no_room"));
            return InteractionResult.FAIL;
        }
        if (!level.getEntitiesOfClass(TheVoid.class, new AABB(centre, centre).inflate(KEEP_APART), TheVoid::isAlive).isEmpty()) {
            player.sendOverlayMessage(Component.translatable("item.hextale.breach_heart.too_near"));
            return InteractionResult.FAIL;
        }
        double dx = player.getX() - centre.x;
        double dz = player.getZ() - centre.z;
        unfold(level, centre, (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG), heart);
        if (player instanceof ServerPlayer serverPlayer) {
            Quests.fire(serverPlayer, Quests.BREACH_UNFOLDED);
        }
        context.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS;
    }

    /** Where the breach's centre goes over {@code ground}: as a breach the Thinning tears open stands. */
    public static Vec3 centreOver(BlockPos ground, BreachHeart heart) {
        VoidLayout layout = heart.layout();
        if (heart.shape().lying()) {
            return new Vec3(ground.getX() + 0.5, ground.getY() + 1 + layout.halfThickness() + 0.03, ground.getZ() + 0.5);
        }
        return new Vec3(ground.getX() + 0.5, ground.getY() + 1 - layout.bottom() + 0.6, ground.getZ() + 0.5);
    }

    /** Nothing but air and plants above the ground where the breach will hang. */
    private static boolean roomAbove(ServerLevel level, BlockPos ground, VoidLayout layout) {
        int height = Mth.clamp(Mth.ceil(layout.reach() * 1.6F), 2, 8);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 1; dy <= height; dy++) {
                    BlockState above = level.getBlockState(ground.offset(dx, dy, dz));
                    if (!above.isAir() && !above.canBeReplaced()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** The heart lets go: the same breach opens at {@code centre}, remembered as a site like any other. */
    public static TheVoid unfold(ServerLevel level, Vec3 centre, float yaw, BreachHeart heart) {
        TheVoid breach = TheVoid.unfold(level, centre, yaw, heart);
        level.addFreshEntity(breach);
        Breaches.sites(level).add(breach.blockPosition());
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.HEART_UNFOLD.get(), SoundSource.AMBIENT, 1.2F, 1.0F);
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.THE_VOID_OMEN.get(), SoundSource.AMBIENT, 1.0F, 1.0F);
        level.sendParticles(ModParticles.ring(TheVoid.COLD), centre.x, centre.y, centre.z, 0, 1.5, 0, 0, 1);
        level.sendParticles(ModParticles.vapour(TheVoid.DEEP), centre.x, centre.y, centre.z, 20, 0.3, 0.3, 0.3, 0.08);
        level.sendParticles(ModParticles.glint(TheVoid.MAGENTA), centre.x, centre.y, centre.z, 16, 0.2, 0.2, 0.2, 0.2);
        return breach;
    }

    /** Carried, it weighs on its bearer and beats now and then. */
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (!(owner instanceof LivingEntity living) || living.isSpectator()) {
            return;
        }
        if (owner.tickCount % 20 == 0 && !(owner instanceof Player player && player.getAbilities().instabuild)) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, true, false, true));
        }
        if (owner.tickCount % BEAT_TICKS == 0) {
            beat(level, owner.position().add(0, owner.getBbHeight() * 0.5, 0), 0.6F);
        }
    }

    /** Lying loose it beats faster and faster, and at last unfolds where it lies. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) {
            return false;
        }
        int age = entity.getAge();
        int every = Math.max(6, Math.round(Mth.lerp(Math.min(1, age / (float) LOOSE_TICKS), BEAT_TICKS, 6)));
        if (age % every == 0) {
            beat(level, entity.position().add(0, 0.25, 0), 0.5F + 0.5F * age / LOOSE_TICKS);
        }
        BreachHeart heart = heart(stack);
        if (age < LOOSE_TICKS || heart == null) {
            return false;
        }
        unfoldLoose(level, entity, heart);
        return true;
    }

    /** A heart left lying unfolds where it lies: over the ground below it, or in the air if there is none near. */
    public static TheVoid unfoldLoose(ServerLevel level, ItemEntity entity, BreachHeart heart) {
        BlockPos ground = groundBelow(level, entity.blockPosition());
        Vec3 centre = ground != null ? centreOver(ground, heart) : entity.position().add(0, heart.size() * 0.5, 0);
        TheVoid breach = unfold(level, centre, level.getRandom().nextFloat() * 360, heart);
        Quests.fireNear(level, centre, Breaches.WATCH_RADIUS, Quests.BREACH_UNFOLDED);
        entity.discard();
        return breach;
    }

    private static @Nullable BlockPos groundBelow(ServerLevel level, BlockPos from) {
        BlockPos.MutableBlockPos pos = from.mutable();
        for (int i = 0; i < 24 && pos.getY() > level.getMinY(); i++) {
            if (level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)) {
                return pos.immutable();
            }
            pos.move(Direction.DOWN);
        }
        return null;
    }

    private static void beat(ServerLevel level, Vec3 at, float volume) {
        level.playSound(null, at.x, at.y, at.z, ModSounds.HEART_BEAT.get(), SoundSource.NEUTRAL, volume, 0.95F + level.getRandom().nextFloat() * 0.1F);
        level.sendParticles(ModParticles.glint(TheVoid.COLD), at.x, at.y, at.z, 3, 0.25, 0.25, 0.25, 0.01);
        level.sendParticles(ModParticles.vapour(TheVoid.SMOKE), at.x, at.y, at.z, 2, 0.15, 0.15, 0.15, 0.01);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        BreachHeart heart = heart(stack);
        if (heart != null) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".holds", Component.translatable(heart.shape().translationKey()),
                    String.format(java.util.Locale.ROOT, "%.1f", heart.size())).withStyle(ChatFormatting.DARK_PURPLE));
        }
        tooltip.accept(Component.translatable(getDescriptionId() + ".controls").withStyle(ChatFormatting.DARK_GRAY));
    }
}
