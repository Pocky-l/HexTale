package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.runework.NullRune;
import com.pockyl.hextale.runework.PickaxeRunes;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * A Null Lantern's state: its runes, and the server-side index of loaded lanterns per dimension that the spawn checks
 * ({@link com.pockyl.hextale.event.NullLanternEvents}) walk - there are only ever a handful, so a plain scan is cheap.
 */
public class NullLanternBlockEntity extends BlockEntity {
    private static final Map<ResourceKey<Level>, Set<NullLanternBlockEntity>> LOADED = new ConcurrentHashMap<>();
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
    public static NullLanternBlockEntity covering(ServerLevel level, Vec3 at, @Nullable NullRune needing) {
        for (NullLanternBlockEntity lantern : LOADED.getOrDefault(level.dimension(), Set.of())) {
            if (!lantern.isRemoved() && lantern.covers(at) && (needing == null || lantern.strength(needing) > 0)) {
                return lantern;
            }
        }
        return null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            LOADED.computeIfAbsent(server.dimension(), key -> ConcurrentHashMap.newKeySet()).add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) {
            Set<NullLanternBlockEntity> lanterns = LOADED.get(level.dimension());
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
            victim.hurt(server.damageSources().magic(), 1.0F);
            server.sendParticles(ModParticles.glint(0xC6B4F2), victim.getX(), victim.getY() + victim.getBbHeight() / 2, victim.getZ(),
                    4, 0.2, 0.3, 0.2, 0.02);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
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
    public void removeComponentsFromTag(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, net.minecraft.core.RegistryAccess.EMPTY);
        super.removeComponentsFromTag(output.tag());
        output.discard("runes");
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
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
