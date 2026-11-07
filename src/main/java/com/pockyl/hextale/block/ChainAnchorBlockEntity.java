package com.pockyl.hextale.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.pockyl.hextale.chem.Zipline;
import com.pockyl.hextale.entity.ZiplineTrolley;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import org.jspecify.annotations.Nullable;

/**
 * The lines held by one end of the Chain Launcher's network: a Chain Post (its base half) or a Chain Claw. Every line
 * is kept by both its ends; the end it was shot from is its owner, draws it and keeps the shot's flight: the chain first
 * unreels for {@link Zipline#flightTicks} ticks, then the far end is set (a post or a claw) or the existing post takes
 * the line. Breaking an end takes its lines down; an end left without lines goes too.
 */
public class ChainAnchorBlockEntity extends BlockEntity {
    /** Ends loaded on this side, for the client's "is the player pointing at a chain" check. */
    private static final Set<ChainAnchorBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());
    /** How often an end checks that the far ends of its lines still hold them (a far end broken while unloaded). */
    private static final int CHECK_EVERY = 40;
    private final List<Link> links = new ArrayList<>();
    /** Set while this end takes its lines down, so it does not take itself down again. */
    private boolean removing;

    /**
     * One line as kept by one end. {@code other} = the far end's position, {@code owner} = this end shot it; t 0 of the
     * line is the owner's eye ({@code start}). While {@code flying}, {@code face} and {@code existing} say what the
     * far end will be.
     */
    public record Link(BlockPos other, boolean owner, Vec3 start, Vec3 end, long shotAt, boolean flying, Direction face, boolean existing) {
        static final Codec<Link> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("other").forGetter(Link::other),
                Codec.BOOL.fieldOf("owner").forGetter(Link::owner),
                Vec3.CODEC.fieldOf("start").forGetter(Link::start),
                Vec3.CODEC.fieldOf("end").forGetter(Link::end),
                Codec.LONG.optionalFieldOf("shot_at", -1000L).forGetter(Link::shotAt),
                Codec.BOOL.optionalFieldOf("flying", false).forGetter(Link::flying),
                Direction.CODEC.optionalFieldOf("face", Direction.UP).forGetter(Link::face),
                Codec.BOOL.optionalFieldOf("existing", false).forGetter(Link::existing)
        ).apply(instance, Link::new));

        public Zipline.Line line() {
            return Zipline.Line.between(start, end);
        }

        Zipline.Anchor target() {
            return new Zipline.Anchor(other, face, existing);
        }

        Link landed() {
            return new Link(other, owner, start, end, shotAt, false, face, existing);
        }

        /**
         * How far the chain has run out (0-1; 1 also once it holds) at {@code time}: the level's real game time plus
         * the partial tick, never a wrapped animation clock.
         */
        public float unreeled(double time) {
            return flying ? (float) Math.clamp((time - shotAt) / Zipline.flightTicks(start.distanceTo(end)), 0, 1) : 1;
        }

        /** The direction the line leaves this end in. */
        public Vec3 away() {
            Zipline.Line line = line();
            return owner ? line.tangent(0).normalize() : line.tangent(1).normalize().reverse();
        }
    }

    public ChainAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHAIN_ANCHOR.get(), pos, state);
    }

    public static Set<ChainAnchorBlockEntity> loaded() {
        synchronized (LOADED) {
            return Set.copyOf(LOADED);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        synchronized (LOADED) {
            LOADED.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        synchronized (LOADED) {
            LOADED.remove(this);
        }
    }

    public List<Link> links() {
        return Collections.unmodifiableList(links);
    }

    public @Nullable Link link(BlockPos other) {
        for (Link link : links) {
            if (link.other().equals(other)) {
                return link;
            }
        }
        return null;
    }

    /** Whether any of its lines holds (riders may hang on it). */
    public boolean holdsAny() {
        return links.stream().anyMatch(link -> !link.flying());
    }

    /** Whether the ends at {@code a} and {@code b} already share a line. */
    public static boolean linked(BlockGetter level, BlockPos a, BlockPos b) {
        return level.getBlockEntity(a) instanceof ChainAnchorBlockEntity anchor && anchor.link(b) != null;
    }

    /** The holding line shot from {@code owner} to {@code other}, or null. */
    public static Zipline.@Nullable Line line(Level level, BlockPos owner, BlockPos other) {
        if (!level.isLoaded(owner) || !(level.getBlockEntity(owner) instanceof ChainAnchorBlockEntity anchor)) {
            return null;
        }
        Link link = anchor.link(other);
        return link == null || !link.owner() || link.flying() ? null : link.line();
    }

    /**
     * Fires a judged shot: raises the start post when there is none yet, and lets the chain fly from it. Returns
     * whether it went off.
     */
    public static boolean fire(ServerLevel level, Zipline.Aim aim) {
        Zipline.Anchor start = aim.start();
        Zipline.Anchor target = aim.target();
        Zipline.Line line = aim.line();
        if (!aim.ok() || start == null || target == null || line == null) {
            return false;
        }
        if (!start.existing()) {
            ChainPostBlock.place(level, start.pos(), start.face(), ModBlocks.CHAIN_POST.get().defaultBlockState());
            level.playSound(null, start.pos(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8F, 0.8F);
        }
        if (!(level.getBlockEntity(start.pos()) instanceof ChainAnchorBlockEntity anchor)) {
            return false;
        }
        anchor.links.add(new Link(target.pos(), true, line.start(), line.end(), level.getGameTime(), true, target.face(), target.existing()));
        anchor.sync();
        Vec3 from = line.start();
        Vec3 dir = line.end().subtract(from).normalize();
        level.sendParticles(ParticleTypes.CLOUD, from.x, from.y, from.z, 6, 0.08, 0.08, 0.08, 0.02);
        level.sendParticles(ParticleTypes.POOF, from.x + dir.x * 0.3, from.y + dir.y * 0.3, from.z + dir.z * 0.3, 3, 0.05, 0.05, 0.05, 0.03);
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChainAnchorBlockEntity anchor) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        long now = server.getGameTime();
        for (Link link : List.copyOf(anchor.links)) {
            if (link.flying() && now >= link.shotAt() + Zipline.flightTicks(link.start().distanceTo(link.end()))) {
                anchor.land(server, link);
            }
        }
        if ((now + pos.asLong()) % CHECK_EVERY == 0) {
            anchor.check(server);
        }
    }

    /** The chain reaches its target: the far end is set if the place still takes it, else the chain falls back. */
    private void land(ServerLevel level, Link link) {
        Zipline.Anchor target = link.target();
        Zipline.Anchor self = Zipline.Anchor.of(level, worldPosition);
        boolean clear = self != null && Zipline.obstacle(level, link.line(), self, target) == null;
        boolean fits = target.existing()
                ? Zipline.Anchor.of(level, target.pos()) != null && level.getBlockEntity(target.pos()) instanceof ChainAnchorBlockEntity far
                        && far.link(worldPosition) == null
                : Zipline.fits(level, target);
        links.remove(link);
        if (!clear || !fits) {
            level.playSound(null, BlockPos.containing(link.end()), SoundEvents.CHAIN_FALL, SoundSource.BLOCKS, 0.6F, 0.9F);
            sync();
            if (links.isEmpty()) {
                removeSelf(level);
            }
            return;
        }
        if (!target.existing()) {
            if (target.post()) {
                ChainPostBlock.place(level, target.pos(), target.face(), ModBlocks.CHAIN_POST.get().defaultBlockState());
            } else {
                level.setBlock(target.pos(), ChainClawBlock.facing(ModBlocks.CHAIN_CLAW.get().defaultBlockState(), target.face()), Block.UPDATE_ALL);
            }
        }
        if (!(level.getBlockEntity(target.pos()) instanceof ChainAnchorBlockEntity far)) {
            sync();
            if (links.isEmpty()) {
                removeSelf(level);
            }
            return;
        }
        far.links.add(new Link(worldPosition, false, link.start(), link.end(), link.shotAt(), false, self.face(), true));
        far.sync();
        links.add(link.landed());
        sync();
        Vec3 eye = link.end();
        level.playSound(null, eye.x, eye.y, eye.z, ModSounds.CHAIN_CLAW_BITE.get(), SoundSource.BLOCKS, 0.7F, 0.9F + level.getRandom().nextFloat() * 0.15F);
        BlockPos support = target.support();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(support)), eye.x, eye.y, eye.z, 10,
                0.15, 0.15, 0.15, 0.08);
    }

    /** Lines whose far end is loaded but no longer holds them (broken while this end was unloaded) come down. */
    private void check(ServerLevel level) {
        boolean changed = links.removeIf(link -> !link.flying() && level.isLoaded(link.other())
                && !(level.getBlockEntity(link.other()) instanceof ChainAnchorBlockEntity far && far.link(worldPosition) != null));
        if (changed) {
            sync();
        }
        if (links.isEmpty()) {
            removeSelf(level);
        }
    }

    /** The far end at {@code other} let go of the line: it comes down, and an end without lines goes. */
    private void unlink(ServerLevel level, BlockPos other) {
        Link link = link(other);
        if (link == null || removing) {
            return;
        }
        links.remove(link);
        sync();
        Vec3 eye = link.owner() ? link.start() : link.end();
        level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.8F, 0.8F);
        if (links.isEmpty()) {
            removeSelf(level);
        }
    }

    private void removeSelf(ServerLevel level) {
        if (!removing && level.getBlockEntity(worldPosition) == this) {
            level.destroyBlock(worldPosition, false);
        }
    }

    /** Hangs the player on the line leaving here nearest to where they look. */
    public void hangOn(ServerLevel level, ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Link best = null;
        double bestDot = -2;
        for (Link link : links) {
            double dot = link.flying() ? -3 : link.away().dot(look);
            if (dot > bestDot) {
                bestDot = dot;
                best = link;
            }
        }
        if (best == null || best.flying()) {
            return;
        }
        double length = best.line().length();
        BlockPos owner = best.owner() ? worldPosition : best.other();
        BlockPos other = best.owner() ? best.other() : worldPosition;
        double t = best.owner() ? Math.min(0.5, 1.2 / length) : Math.max(0.5, 1 - 1.2 / length);
        ZiplineTrolley.mount(level, player, owner, other, t);
    }

    /** The end goes: all its lines come down with it (and far ends left without lines go too). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel server && !links.isEmpty()) {
            removing = true;
            for (Link link : List.copyOf(links)) {
                if (!link.flying() && server.isLoaded(link.other()) && server.getBlockEntity(link.other()) instanceof ChainAnchorBlockEntity far) {
                    far.unlink(server, pos);
                }
            }
            links.clear();
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        links.clear();
        links.addAll(input.read("links", Link.CODEC.listOf()).orElse(List.of()));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("links", Link.CODEC.listOf(), links);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
