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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.thevoid.TheVoid;
import com.pockyl.hextale.thevoid.VoidLayout;
import com.pockyl.hextale.thevoid.VoidShape;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModSounds;

import java.util.function.Consumer;

/**
 * A sliver of the Void (creative only while the Void has no logic): use breaks it open where the player looks, of
 * the chosen shape or any; sneak + use on a breach closes it, sneak + use elsewhere picks the next shape.
 */
public class VoidShardItem extends Item {
    /** How far away the Void can be opened. */
    private static final double REACH = 32;

    public VoidShardItem(Properties properties) {
        super(properties);
    }

    /** The chosen shape, or null for any. */
    public static VoidShape chosen(ItemStack stack) {
        int value = stack.getOrDefault(ModDataComponents.VOID_SHAPE.get(), 0);
        return value <= 0 ? null : VoidShape.byOrdinal(value - 1);
    }

    public InteractionResult usePort(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            TheVoid aimed = TheVoid.aimedAt(player, REACH * 2);
            if (aimed != null) {
                aimed.close();
                return InteractionResult.SUCCESS;
            }
            int next = (stack.getOrDefault(ModDataComponents.VOID_SHAPE.get(), 0) + 1) % (VoidShape.values().length + 1);
            stack.set(ModDataComponents.VOID_SHAPE.get(), next);
            serverPlayer.displayClientMessage(Component.translatable("item.hextale.void_shard.shape", shapeName(chosen(stack)))
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResult.SUCCESS;
        }
        RandomSource random = level.getRandom();
        VoidShape shape = chosen(stack);
        if (shape == null) {
            shape = VoidShape.byOrdinal(random.nextInt(VoidShape.values().length));
        }
        float size = shape.randomSize(random);
        int seed = random.nextInt();
        Vec3 centre = place(server, player, VoidLayout.of(shape, size, seed));
        // A breach faces the player square on, as the Void tends to.
        float yaw = player.getYRot() + (random.nextFloat() - 0.5F) * 10;
        level.addFreshEntity(TheVoid.open(level, centre, yaw, shape, size, seed));
        level.playSound(null, centre.x, centre.y, centre.z, ModSounds.THE_VOID_OPEN.get(), SoundSource.AMBIENT,
                0.8F + size * 0.12F, 1.1F - Math.min(size, 8) * 0.05F);
        player.getCooldowns().addCooldown(stack.getItem(), 10);
        return InteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return com.pockyl.hextale.port.Interactions.holder(usePort(level, player, hand), player.getItemInHand(hand));
    }

    /** Where the breach's centre goes: in the air before the player, clear of the ground. */
    private static Vec3 place(ServerLevel level, Player player, VoidLayout layout) {
        float size = layout.size;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(REACH)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        double distance = hit.getType() == HitResult.Type.MISS ? REACH : hit.getLocation().distanceTo(eye);
        double along = Mth.clamp(Math.min(distance - 1.0, 3 + size * 0.9), 2.0, REACH);
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

    private static Component shapeName(VoidShape shape) {
        return Component.translatable(shape == null ? "void_shape.hextale.any" : shape.translationKey());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltipLines, TooltipFlag flag) {
        Consumer<Component> tooltip = tooltipLines::add;
        tooltip.accept(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.hextale.void_shard.shape", shapeName(chosen(stack)))
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.accept(Component.translatable("item.hextale.void_shard.controls").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("item.hextale.creative_only").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
