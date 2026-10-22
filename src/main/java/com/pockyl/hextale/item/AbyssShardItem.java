package com.pockyl.hextale.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.abyss.AbyssRift;
import com.pockyl.hextale.abyss.RiftShape;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModSounds;

import java.util.function.Consumer;

/**
 * A sliver of the abyss (creative only while rifts have no logic): use tears a rift open where the player looks, of
 * the chosen shape or any; sneak + use on a rift closes it, sneak + use elsewhere picks the next shape.
 */
public class AbyssShardItem extends Item {
    /** How far a rift can be torn open. */
    private static final double REACH = 32;

    public AbyssShardItem(Properties properties) {
        super(properties);
    }

    /** The chosen shape, or null for any. */
    public static RiftShape chosen(ItemStack stack) {
        int value = stack.getOrDefault(ModDataComponents.RIFT_SHAPE.get(), 0);
        return value <= 0 ? null : RiftShape.byOrdinal(value - 1);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            AbyssRift aimed = AbyssRift.aimedAt(player, REACH * 2);
            if (aimed != null) {
                aimed.close();
                return InteractionResult.SUCCESS;
            }
            int next = (stack.getOrDefault(ModDataComponents.RIFT_SHAPE.get(), 0) + 1) % (RiftShape.values().length + 1);
            stack.set(ModDataComponents.RIFT_SHAPE.get(), next);
            serverPlayer.sendOverlayMessage(Component.translatable("item.hextale.abyss_shard.shape", shapeName(chosen(stack)))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.SUCCESS;
        }
        RandomSource random = level.getRandom();
        RiftShape shape = chosen(stack);
        if (shape == null) {
            shape = RiftShape.byOrdinal(random.nextInt(RiftShape.values().length));
        }
        float size = shape.randomSize(random);
        Vec3 centre = place(server, player, shape, size);
        float yaw = shape.lying() ? random.nextFloat() * 360 : player.getYRot() + (random.nextFloat() - 0.5F) * 30;
        AbyssRift rift = AbyssRift.open(level, centre, yaw, shape, size, random.nextInt());
        level.addFreshEntity(rift);
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.ABYSS_RIFT_OPEN.get(), SoundSource.AMBIENT,
                0.8F + size * 0.12F, 1.1F - Math.min(size, 8) * 0.05F);
        player.getCooldowns().addCooldown(stack, 10);
        return InteractionResult.SUCCESS;
    }

    /** Where the rift's centre goes: in the air before the player, clear of the ground; a fissure on the ground. */
    private static Vec3 place(ServerLevel level, Player player, RiftShape shape, float size) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(REACH)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double distance = hit.getType() == HitResult.Type.MISS ? REACH : hit.getLocation().distanceTo(eye);
        if (shape.lying()) {
            Vec3 at = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(6 + size)) : hit.getLocation();
            double ground = groundBelow(level, at, 12);
            return new Vec3(at.x, (Double.isNaN(ground) ? at.y : ground) + 0.04, at.z);
        }
        double along = Mth.clamp(Math.min(distance - shape.halfWidth(size) - 0.6, 3 + size * 0.9), 2.0, REACH);
        Vec3 at = eye.add(look.scale(along));
        double ground = groundBelow(level, at, size + 2);
        if (!Double.isNaN(ground)) {
            at = new Vec3(at.x, Math.max(at.y, ground + size * 0.5 + 0.3), at.z);
        }
        return at;
    }

    /** The top of the first solid floor at or below {@code at} within {@code depth} blocks, or NaN. */
    private static double groundBelow(Level level, Vec3 at, double depth) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(at.x, at.y + 0.5, at.z).mutable();
        for (int i = 0; i <= depth; i++) {
            if (level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP)) {
                return pos.getY() + 1;
            }
            pos.move(Direction.DOWN);
        }
        return Double.NaN;
    }

    private static Component shapeName(RiftShape shape) {
        return Component.translatable(shape == null ? "rift_shape.hextale.any" : shape.translationKey());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.abyss_shard.shape", shapeName(chosen(stack)))
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.translatable("item.hextale.abyss_shard.controls").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.hextale.creative_only").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
