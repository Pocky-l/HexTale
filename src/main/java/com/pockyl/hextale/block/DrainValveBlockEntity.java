package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;

import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModTags;

/**
 * An open Drain Valve takes what the pipes bring - up to {@value #DROPS_PER_TICK} mB of essence and
 * {@value #WATER_PER_TICK} mB of water a tick - and pours it out of its spout. Essence that lands on stone soaks into
 * it: every {@value #ESSENCE_PER_STONE} mB turns one block of the {@link ModTags#SOAKS_ESSENCE} stone in the 3x3 patch
 * where the stream lands (up to {@value #FALL} blocks below) into magic stone. Essence that finds no stone is lost.
 */
public class DrainValveBlockEntity extends BlockEntity implements EssenceNetwork.Receiver, EssenceNetwork.WaterReceiver {
    public static final int DROPS_PER_TICK = 4;
    public static final int WATER_PER_TICK = 100;
    /** Block event: a splash of this colour pours out (param = RGB). */
    private static final int EVENT_POUR = 1;
    public static final int ESSENCE_PER_STONE = 16;
    public static final int FALL = 6;

    private long tick = Long.MIN_VALUE;
    private int dropsThisTick;
    private int waterThisTick;
    private long lastSplash = Long.MIN_VALUE / 2;
    /** Essence poured but not yet soaked into a block. */
    private int soaked;

    public DrainValveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRAIN_VALVE.get(), pos, state);
    }

    /** An open valve drains the tanks at its sides (piped or right beside it). */
    public static void serverTick(Level level, BlockPos pos, BlockState state, DrainValveBlockEntity valve) {
        if (!state.getValue(DrainValveBlock.OPEN) || level.getGameTime() % EssenceTankBlockEntity.FLOW_EVERY != 0) {
            return;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            EssenceMix drawn = EssenceNetwork.draw(level, pos, side, EssenceTankBlockEntity.FLOW, valve);
            if (!drawn.isEmpty()) {
                valve.splash(drawn);
                valve.soak(drawn, drawn.total());
            }
        }
    }

    private boolean open() {
        return getBlockState().getValue(DrainValveBlock.OPEN);
    }

    /** Starts a new tick's allowance when the game time moved on. */
    private void roll() {
        long now = level == null ? 0 : level.getGameTime();
        if (now != tick) {
            tick = now;
            dropsThisTick = 0;
            waterThisTick = 0;
        }
    }

    @Override
    public int receive(Direction side, EssenceMix offer, boolean simulate) {
        if (!open() || !side.getAxis().isHorizontal()) {
            return 0;
        }
        roll();
        int taken = Math.min(offer.total(), DROPS_PER_TICK - dropsThisTick);
        if (taken > 0 && !simulate) {
            dropsThisTick += taken;
            splash(offer);
            soak(offer, taken);
        }
        return Math.max(0, taken);
    }

    @Override
    public int receiveWater(Direction side, int amount, boolean simulate) {
        if (!open() || !side.getAxis().isHorizontal()) {
            return 0;
        }
        roll();
        int taken = Math.min(amount, WATER_PER_TICK - waterThisTick);
        if (taken > 0 && !simulate) {
            waterThisTick += taken;
            splash(null);
        }
        return Math.max(0, taken);
    }

    /**
     * Poured essence soaks into the stone where the stream lands, one block per {@value #ESSENCE_PER_STONE} mB, and wears
     * the edge of the world thin (much less where stone drinks it).
     */
    private void soak(EssenceMix mix, int amount) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        // Essence the stone drinks wears the world little; essence lost on the ground wears it thin.
        com.pockyl.hextale.thevoid.Thinning.poured(server, worldPosition, mix, amount, soakTarget(server) != null);
        soaked += amount;
        while (soaked >= ESSENCE_PER_STONE) {
            BlockPos stone = soakTarget(server);
            if (stone == null) {
                soaked = 0;
                return;
            }
            soaked -= ESSENCE_PER_STONE;
            BlockState magic = ModBlocks.MAGIC_STONE.get().defaultBlockState();
            server.setBlockAndUpdate(stone, magic);
            server.gameEvent(GameEvent.BLOCK_CHANGE, stone, GameEvent.Context.of(magic));
            server.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, stone, Block.getId(magic));
            server.playSound(null, stone, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 0.7F, 0.8F);
        }
        setChanged();
    }

    /**
     * Where the next block soaks: the stream falls from the spout to the first block that is not air; among the
     * soakable stone in the 3x3 patch around it whose top is open (the essence runs over it), the nearest to the stream
     * goes first, ties at random. Null when the stream lands in a liquid or finds no such stone.
     */
    @Nullable
    private BlockPos soakTarget(ServerLevel server) {
        BlockPos.MutableBlockPos at = worldPosition.mutable();
        for (int i = 0; i < FALL; i++) {
            at.move(Direction.DOWN);
            BlockState state = server.getBlockState(at);
            if (!state.getFluidState().isEmpty()) {
                return null;
            }
            if (!state.isAir()) {
                break;
            }
            if (i == FALL - 1) {
                return null;
            }
        }
        BlockPos landing = at.immutable();
        BlockPos best = null;
        int bestDistance = Integer.MAX_VALUE;
        int ties = 0;
        RandomSource random = server.getRandom();
        for (BlockPos pos : BlockPos.betweenClosed(landing.offset(-1, 0, -1), landing.offset(1, 0, 1))) {
            if (!server.getBlockState(pos).is(ModTags.SOAKS_ESSENCE)
                    || !server.getBlockState(pos.above()).getCollisionShape(server, pos.above()).isEmpty()) {
                continue;
            }
            int distance = Math.abs(pos.getX() - landing.getX()) + Math.abs(pos.getZ() - landing.getZ());
            if (distance < bestDistance) {
                best = pos.immutable();
                bestDistance = distance;
                ties = 1;
            } else if (distance == bestDistance && random.nextInt(++ties) == 0) {
                best = pos.immutable();
            }
        }
        return best;
    }

    public int soaked() {
        return soaked;
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        soaked = input.getIntOr("soaked", 0);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        output.putInt("soaked", soaked);
    }

    /**
     * Tells clients now and then to show the liquid pouring out of the spout. The block event carries one byte, so
     * it names the strongest essence (ordinal + 1), 0 for water, 255 for an unknown mixture - never the colour.
     */
    private void splash(@org.jspecify.annotations.Nullable EssenceMix mix) {
        int code = 0;
        if (mix != null) {
            code = 255;
            int most = 0;
            for (var entry : mix.asMap().entrySet()) {
                if (entry.getValue() > most) {
                    most = entry.getValue();
                    code = entry.getKey().ordinal() + 1;
                }
            }
        }
        if (level != null && level.getGameTime() - lastSplash >= 4) {
            lastSplash = level.getGameTime();
            level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_POUR, code);
            if (level.getRandom().nextInt(4) == 0) {
                level.playSound(null, worldPosition, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.25F, 1.4F);
            }
        }
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id != EVENT_POUR || level == null || !level.isClientSide()) {
            return super.triggerEvent(id, param);
        }
        RandomSource random = level.getRandom();
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() - 0.05;
        double z = worldPosition.getZ() + 0.5;
        int color = param == 0 ? ModParticles.WATER : param > com.pockyl.hextale.rune.Essence.COUNT ? EssenceMix.UNKNOWN_COLOR
                : com.pockyl.hextale.rune.Essence.values()[param - 1].color();
        // A short thick stream: drops one after another, falling and splashing below.
        for (int i = 0; i < 5; i++) {
            level.addParticle(ModParticles.drop(color), x + (random.nextDouble() - 0.5) * 0.08, y - i * 0.07,
                    z + (random.nextDouble() - 0.5) * 0.08, 0, -0.08 - i * 0.03, 0);
        }
        if (param != 0 && random.nextInt(3) == 0) {
            level.addParticle(ModParticles.wisp(color), x, y - 0.1, z, (random.nextDouble() - 0.5) * 0.01, 0.004,
                    (random.nextDouble() - 0.5) * 0.01);
        }
        return true;
    }
}
