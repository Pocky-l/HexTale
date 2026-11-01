package com.pockyl.hextale.block;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Sends a working machine's block entity to the clients at most twice a second: the first change after a quiet spell
 * goes out at once (a click shows at once), later ones within {@value #TICKS} ticks wait for the machine's next tick.
 * The owner calls {@link #request} where it used to send the update and {@link #tick} from its server ticker.
 */
public final class ClientSync {
    private static final int TICKS = 10;
    private long syncedAt = Long.MIN_VALUE / 2;
    private boolean due;

    public void request(BlockEntity entity) {
        Level level = entity.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        long now = level.getGameTime();
        if (now - syncedAt >= TICKS || now < syncedAt) {
            send(entity, level, now);
        } else {
            due = true;
        }
    }

    public void tick(BlockEntity entity) {
        Level level = entity.getLevel();
        if (due && level != null && level.getGameTime() - syncedAt >= TICKS) {
            send(entity, level, level.getGameTime());
        }
    }

    private void send(BlockEntity entity, Level level, long now) {
        syncedAt = now;
        due = false;
        level.sendBlockUpdated(entity.getBlockPos(), entity.getBlockState(), entity.getBlockState(), Block.UPDATE_CLIENTS);
    }
}
