package com.pockyl.hextale.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.block.HexFoundryBlock;
import com.pockyl.hextale.block.HexFoundryBlockEntity;
import com.pockyl.hextale.block.HexFurnaceBlock;
import com.pockyl.hextale.block.HexFurnaceBlockEntity;
import com.pockyl.hextale.block.HexPylonBlockEntity;
import com.pockyl.hextale.block.HextechAltarBlockEntity;
import com.pockyl.hextale.block.HextechPurifierBlockEntity;
import com.pockyl.hextale.block.SeparatorBlock;
import com.pockyl.hextale.block.SeparatorBlockEntity;
import com.pockyl.hextale.registry.ModSounds;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Working sounds that follow a machine while it works (a ritual, the Hex Furnace and Foundry, the separator, a pylon
 * moving things, a Hextech Purifier drawing): every half second the client looks for working machines near
 * the listener (the block entities of the chunks around, never block by block) and gives each one a looping sound that
 * fades in, follows the machine's state (an altar's loop rises with the ritual and sinks when it stalls) and fades out
 * and stops when the work ends or the machine is gone.
 */
public final class MachineLoops {
    /** Blocks around the listener where machines are heard. */
    private static final int REACH = 20;
    private static final int SCAN_TICKS = 10;
    /** Loops playing at once, at most (the nearest machines win), and pylons among them (a base may hold many). */
    private static final int MAX_LOOPS = 4;
    private static final int MAX_PYLONS = 2;
    /** Ticks a pylon's loop goes on after its last move. */
    private static final int PYLON_LINGER = 80;
    private static final Map<BlockPos, Loop> PLAYING = new HashMap<>();

    /** One kind of working machine: its sound, how loud at full and whether (and how) it works now. */
    private enum Kind {
        ALTAR(ModSounds.RITUAL_LOOP, 0.55F) {
            @Override
            float pitch(BlockEntity entity) {
                HextechAltarBlockEntity altar = (HextechAltarBlockEntity) entity;
                return altar.stalled() ? 0.82F : 0.9F + 0.25F * altar.progress(0);
            }

            @Override
            float level(BlockEntity entity) {
                HextechAltarBlockEntity altar = (HextechAltarBlockEntity) entity;
                return !altar.active() ? 0 : altar.stalled() ? 0.45F : 1;
            }
        },
        FURNACE(ModSounds.FORGE_BURN, 0.4F) {
            @Override
            float level(BlockEntity entity) {
                BlockState state = entity.getBlockState();
                return state.hasProperty(HexFurnaceBlock.LIT) && state.getValue(HexFurnaceBlock.LIT) ? 1 : 0;
            }
        },
        FOUNDRY(ModSounds.FOUNDRY_ROAR, 0.6F) {
            @Override
            float level(BlockEntity entity) {
                BlockState state = entity.getBlockState();
                return state.hasProperty(HexFoundryBlock.LIT) && state.getValue(HexFoundryBlock.FORMED) && state.getValue(HexFoundryBlock.LIT) ? 1 : 0;
            }
        },
        SEPARATOR(ModSounds.SEPARATOR_SIMMER, 0.3F) {
            @Override
            float level(BlockEntity entity) {
                BlockState state = entity.getBlockState();
                return state.hasProperty(SeparatorBlock.LIT) && state.getValue(SeparatorBlock.LIT) ? 1 : 0;
            }
        },
        /** Heard while it moves things (and a little after), fading away when it rests: a base may hold many. */
        PYLON(ModSounds.PYLON_HUM, 0.3F) {
            @Override
            float level(BlockEntity entity) {
                HexPylonBlockEntity pylon = (HexPylonBlockEntity) entity;
                long since = entity.getLevel() == null ? Long.MAX_VALUE : entity.getLevel().getGameTime() - pylon.movedAt;
                return since < 0 || since > PYLON_LINGER ? 0 : 1 - since / (float) PYLON_LINGER * 0.5F;
            }

            @Override
            float pitch(BlockEntity entity) {
                return 0.95F + 0.1F * (entity.getBlockPos().hashCode() & 7) / 7F;
            }
        },
        /** Heard while it keeps drawing: from one draw to the next, and a little after the last. */
        PURIFIER(ModSounds.PURIFIER_LOOP, 0.5F) {
            @Override
            float level(BlockEntity entity) {
                HextechPurifierBlockEntity purifier = (HextechPurifierBlockEntity) entity;
                long since = entity.getLevel() == null ? Long.MAX_VALUE : entity.getLevel().getGameTime() - purifier.drewAt();
                return !purifier.crystal().isEmpty() && since >= 0 && since <= purifier.interval() + HextechPurifierBlockEntity.STREAM_TICKS ? 1 : 0;
            }
        };

        final Supplier<SoundEvent> sound;
        final float full;

        Kind(Supplier<SoundEvent> sound, float full) {
            this.sound = sound;
            this.full = full;
        }

        /** 0 = not working, up to 1 = working at full. */
        abstract float level(BlockEntity entity);

        float pitch(BlockEntity entity) {
            return 1;
        }

        static @Nullable Kind of(BlockEntity entity) {
            if (entity instanceof HextechAltarBlockEntity) {
                return ALTAR;
            }
            if (entity instanceof HexFurnaceBlockEntity) {
                return FURNACE;
            }
            if (entity instanceof HexFoundryBlockEntity) {
                return FOUNDRY;
            }
            if (entity instanceof HexPylonBlockEntity) {
                return PYLON;
            }
            if (entity instanceof HextechPurifierBlockEntity) {
                return PURIFIER;
            }
            return entity instanceof SeparatorBlockEntity ? SEPARATOR : null;
        }
    }

    private MachineLoops() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick());
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            PLAYING.clear();
            return;
        }
        PLAYING.values().removeIf(Loop::isStopped);
        if (level.getGameTime() % SCAN_TICKS != 0) {
            return;
        }
        Vec3 ear = minecraft.player.getEyePosition();
        java.util.List<BlockEntity> working = new java.util.ArrayList<>();
        int cx0 = ((int) Math.floor(ear.x) - REACH) >> 4;
        int cx1 = ((int) Math.floor(ear.x) + REACH) >> 4;
        int cz0 = ((int) Math.floor(ear.z) - REACH) >> 4;
        int cz1 = ((int) Math.floor(ear.z) + REACH) >> 4;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos pos = entity.getBlockPos();
                    if (PLAYING.containsKey(pos) || Vec3.atCenterOf(pos).distanceToSqr(ear) > REACH * REACH) {
                        continue;
                    }
                    Kind kind = Kind.of(entity);
                    if (kind != null && kind.level(entity) > 0) {
                        working.add(entity);
                    }
                }
            }
        }
        // The nearest first, within the caps.
        working.sort(java.util.Comparator.comparingDouble(entity -> Vec3.atCenterOf(entity.getBlockPos()).distanceToSqr(ear)));
        long pylons = PLAYING.values().stream().filter(loop -> loop.kind == Kind.PYLON).count();
        for (BlockEntity entity : working) {
            if (PLAYING.size() >= MAX_LOOPS) {
                break;
            }
            Kind kind = Kind.of(entity);
            if (kind == Kind.PYLON && pylons >= MAX_PYLONS) {
                continue;
            }
            Loop loop = new Loop(kind, entity);
            PLAYING.put(entity.getBlockPos().immutable(), loop);
            minecraft.getSoundManager().play(loop);
            if (kind == Kind.PYLON) {
                pylons++;
            }
        }
    }

    /** The loop of one machine: fades towards the machine's working level, stops once silent and idle. */
    private static final class Loop extends AbstractTickableSoundInstance {
        private final Kind kind;
        private final BlockEntity entity;

        Loop(Kind kind, BlockEntity entity) {
            super(kind.sound.get(), SoundSource.BLOCKS, RandomSource.create());
            this.kind = kind;
            this.entity = entity;
            looping = true;
            delay = 0;
            volume = 0.01F;
            pitch = kind.pitch(entity);
            x = entity.getBlockPos().getX() + 0.5;
            y = entity.getBlockPos().getY() + 0.5;
            z = entity.getBlockPos().getZ() + 0.5;
        }

        @Override
        public void tick() {
            float target = entity.isRemoved() ? 0 : kind.full * kind.level(entity);
            volume = volume < target ? Math.min(target, volume + 0.03F) : Math.max(target, volume - 0.03F);
            if (!entity.isRemoved()) {
                pitch += (kind.pitch(entity) - pitch) * 0.1F;
            }
            if (volume <= 0.001F && target <= 0) {
                stop();
            }
        }
    }
}
