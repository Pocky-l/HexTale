package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.NullRune;
import com.pockyl.hextale.runework.PickaxeRunes;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * A Null Lantern's state: its runes, and the index of loaded lanterns per side and dimension that the spawn checks
 * ({@link com.pockyl.hextale.event.NullLanternEvents}) and the client's hush ({@link com.pockyl.hextale.client.SilenceHush})
 * walk - there are only ever a handful, so a plain scan is cheap.
 */
public class NullLanternBlockEntity extends BlockEntity {
    private static final Map<String, Set<NullLanternBlockEntity>> LOADED = new ConcurrentHashMap<>();
    private PickaxeRunes runes = PickaxeRunes.EMPTY;

    public NullLanternBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NULL_LANTERN.get(), pos, state);
    }

    public int strength(NullRune rune) {
        return runes.strength(rune.rune());
    }

    /** How far its quiet reaches. */
    public int radius() {
        return NullRune.radius(strength(NullRune.SHIFT));
    }

    /** Whether {@code at} lies within its quiet. */
    public boolean covers(Vec3 at) {
        int radius = radius();
        return at.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= (double) radius * radius;
    }

    /** The loaded lanterns of a dimension whose quiet covers {@code at}, the first one found; null when none. */
    @Nullable
    public static NullLanternBlockEntity covering(Level level, Vec3 at, @Nullable NullRune needing) {
        return covering(level, at, needing, 1);
    }

    /** The same, with at least {@code stars} of the rune {@code needing}. */
    @Nullable
    public static NullLanternBlockEntity covering(Level level, Vec3 at, @Nullable NullRune needing, int stars) {
        for (NullLanternBlockEntity lantern : LOADED.getOrDefault(key(level), Set.of())) {
            if (!lantern.isRemoved() && lantern.covers(at) && (needing == null || lantern.strength(needing) >= stars)) {
                return lantern;
            }
        }
        return null;
    }

    /** The share of a sound at {@code at} that the quietest lantern with Silence leaves; 1 where none hushes. */
    public static float soundLeft(Level level, Vec3 at) {
        float left = 1;
        for (NullLanternBlockEntity lantern : LOADED.getOrDefault(key(level), Set.of())) {
            int silence = lantern.strength(NullRune.SILENCE);
            if (silence > 0 && !lantern.isRemoved() && lantern.covers(at)) {
                left = Math.min(left, HolderRune.at(NullRune.HUSHED, silence));
            }
        }
        return left;
    }

    /** Forgets the lanterns of one side ({@code client} or server) when its world closes. */
    public static void forget(boolean client) {
        LOADED.keySet().removeIf(key -> key.startsWith(client ? "c:" : "s:"));
    }

    private static String key(Level level) {
        return (level.isClientSide() ? "c:" : "s:") + level.dimension().identifier();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            LOADED.computeIfAbsent(key(level), key -> ConcurrentHashMap.newKeySet()).add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) {
            Set<NullLanternBlockEntity> lanterns = LOADED.get(key(level));
            if (lanterns != null) {
                lanterns.remove(this);
            }
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        setRemoved();
    }

    /** Banishing: hostile creatures inside the quiet take a small bite of null light now and then. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, NullLanternBlockEntity lantern) {
        int banishing = lantern.strength(NullRune.BANISHING);
        if (banishing <= 0 || !(level instanceof ServerLevel server)
                || level.getGameTime() % NullRune.BURN_TICKS[Math.min(banishing, 4) - 1] != 0) {
            return;
        }
        int radius = lantern.radius();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(radius),
                entity -> entity instanceof Enemy && lantern.covers(entity.position()))) {
            victim.hurtServer(server, server.damageSources().magic(), 1.0F);
            server.sendParticles(ModParticles.glint(0xC6B4F2), victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(),
                    4, 0.2, 0.3, 0.2, 0.02);
            // A thin bolt of null light from the lantern bites it.
            com.pockyl.hextale.network.RuneEffectPayload.show(server, com.pockyl.hextale.network.RuneEffectPayload.Family.ARCS, net.minecraft.world.phys.Vec3.atCenterOf(pos),
                    java.util.List.of(BlockPos.containing(victim.position().add(0, victim.getBbHeight() / 2, 0))), 0xC6B4F2, 1);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.NULL_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.NULL_RUNES.get(), runes);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("runes");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
