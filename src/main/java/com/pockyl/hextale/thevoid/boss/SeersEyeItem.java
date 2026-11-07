package com.pockyl.hextale.thevoid.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.item.NotedItem;
import com.pockyl.hextale.network.ProsectorMarkerPayload;
import com.pockyl.hextale.network.ProsectorMarkerPayload.Marker;
import com.pockyl.hextale.registry.ModSounds;

/**
 * Calls the Prosector: right click the ground, anywhere in any world, and the Eye is spent and the thing it reports to
 * comes up out of that spot. On Peaceful there is nothing to fight, so it says so.
 */
public class SeersEyeItem extends NotedItem {
    /** Free blocks it needs over the ground: its body has to fit. */
    private static final int HEADROOM = 6;
    private static final int WAY_COLOR = 0xFFD27A;
    /** Blocks a tick the light of the Eye flies to the ground at. */
    private static final double WAY_SPEED = 0.4;

    /** Why it will not work here, or {@link #NONE}. */
    public enum Refusal {
        NONE, PEACEFUL, NO_ROOM, TAKEN
    }

    public SeersEyeItem(Properties properties) {
        super(properties);
    }

    /** Whether the Eye works on the ground at {@code spot}, the first block over the one clicked. Peaceful goes first. */
    public static Refusal refusal(ServerLevel level, BlockPos spot) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return Refusal.PEACEFUL;
        }
        for (int up = 0; up < HEADROOM; up++) {
            if (!level.getBlockState(spot.above(up)).getCollisionShape(level, spot.above(up)).isEmpty()) {
                return Refusal.NO_ROOM;
            }
        }
        double reach = ProsectorConfig.FIGHT_RADIUS.get() * 2;
        return level.getEntitiesOfClass(Prosector.class, new AABB(spot).inflate(reach), Prosector::isAlive).isEmpty() ? Refusal.NONE : Refusal.TAKEN;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        if (!(world instanceof ServerLevel level) || !(player instanceof ServerPlayer holder)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos spot = context.getClickedPos().above();
        Refusal refusal = refusal(level, spot);
        if (refusal != Refusal.NONE) {
            holder.sendOverlayMessage(Component.translatable("item.hextale.seers_eye." + refusal.name().toLowerCase(java.util.Locale.ROOT)));
            return InteractionResult.FAIL;
        }
        Vec3 from = holder.getEyePosition();
        Vec3 to = Vec3.atBottomCenterOf(spot);
        ProsectorMarkerPayload.send(level, Marker.WAY, from, to, 1, (int) (from.distanceTo(to) / WAY_SPEED), WAY_COLOR);
        level.playSound(null, holder.getX(), holder.getY(), holder.getZ(), ModSounds.PROSECTOR_EYE_WAY.get(), holder.getSoundSource(), 0.8F, 1.0F);
        Prosector.summon(level, spot);
        context.getItemInHand().consume(1, holder);
        return InteractionResult.SUCCESS;
    }
}
