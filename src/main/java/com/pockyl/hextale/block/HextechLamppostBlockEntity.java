package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.LamppostRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.ToolEffects;
import com.pockyl.hextale.thevoid.Wards;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * A Hextech Lamppost's light (its block entity lives in the lower half). The gem burns light essence, drawn
 * {@value #DRAW_MB} mB at a time out of a jar within {@link JarReach#RADIUS} whenever its store runs low, and shines while
 * the store lasts. In its light - a cube of {@link #reach()} blocks every way - it burns off {@link #share()} of every gain
 * of the Thinning before it reaches the land ({@link Wards}); one mB of light burns off {@value #PER_MB} of it. The
 * burned-off dark is drawn out of the working spot into the gem as a thread of void motes. A lamppost that burns nothing
 * keeps its store: standing lit costs nothing.
 */
public class HextechLamppostBlockEntity extends BlockEntity {
    /** Thinning one mB of light essence burns off. */
    public static final int PER_MB = 8;
    public static final int DRAW_MB = 2;
    /** The store is topped up below this much. */
    public static final float LOW = 16;
    public static final int DRAW_TICKS = 40;
    /** Ticks between two rounds of the light's gifts (Healing, Insight). */
    public static final int GIFT_TICKS = 40;
    /** A jar's stream shows this long after a draw. */
    public static final int STREAM_TICKS = 50;
    /** Ticks between two threads of burned-off dark shown. */
    private static final int THREAD_TICKS = 10;
    /** The dark drawn out of the land. */
    public static final int DARK = 0x2C0A26;
    public static final int PLUM = 0x7A2A6E;

    /** Thinning the light drawn so far still burns off. */
    private float store;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    private @Nullable BlockPos drewFrom;
    private long drewAt = -1000;
    private long burnedAt = -1000;
    private long threadAt = -1000;

    public HextechLamppostBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEXTECH_LAMPPOST.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            Wards.add(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            Wards.remove(level, worldPosition);
        }
        super.setRemoved();
    }

    // ---- Runes --------------------------------------------------------------------------------------------------------

    public PickaxeRunes runes() {
        return runes;
    }

    public void setRunes(PickaxeRunes runes) {
        this.runes = runes;
        setChanged();
    }

    public int strength(LamppostRune rune) {
        return runes.strength(rune.rune());
    }

    /** Blocks every way its light reaches. */
    public int reach() {
        return LamppostRune.reach(strength(LamppostRune.SHIFT));
    }

    /** Share of every gain of the Thinning in its light it burns off. */
    public float share() {
        return LamppostRune.share(strength(LamppostRune.PURIFICATION));
    }

    // ---- State --------------------------------------------------------------------------------------------------------

    public boolean lit() {
        return store > 0;
    }

    public float store() {
        return store;
    }

    /** Where the gem hangs in the lamp head, in world space. */
    public static Vec3 gem(BlockPos lower) {
        return Vec3.atLowerCornerOf(lower).add(0.5, 1 + 10 / 16.0, 0.5);
    }

    public boolean covers(BlockPos pos) {
        int reach = reach();
        return Math.abs(pos.getX() - worldPosition.getX()) <= reach && Math.abs(pos.getY() - worldPosition.getY()) <= reach
                && Math.abs(pos.getZ() - worldPosition.getZ()) <= reach;
    }

    public @Nullable BlockPos drawing() {
        return level != null && drewFrom != null && level.getGameTime() - drewAt <= STREAM_TICKS ? drewFrom : null;
    }

    public long drewAt() {
        return drewAt;
    }

    /** Game time of the last burn (the gem flares). */
    public long burnedAt() {
        return burnedAt;
    }

    // ---- Work ---------------------------------------------------------------------------------------------------------

    /**
     * Burns off its share of a gain of the Thinning at {@code at} (as far as its store goes) and shows the dark drawn into
     * the gem. Returns how much it burned off.
     */
    public float burn(ServerLevel level, BlockPos at, float amount) {
        float burned = Math.min(store, amount * share());
        if (burned <= 0) {
            return 0;
        }
        int dam = strength(LamppostRune.DAM);
        RandomSource random = level.getRandom();
        boolean free = dam > 0 && random.nextFloat() < HolderRune.at(LamppostRune.FREE, dam);
        if (!free) {
            store -= burned;
        }
        long now = level.getGameTime();
        burnedAt = now;
        if (now - threadAt >= THREAD_TICKS) {
            // Shown (and synced for the gem's flare) at most every few ticks, however much work goes on around.
            threadAt = now;
            thread(level, at, random);
            if (free) {
                ToolEffects.puff(level, gem(worldPosition), LamppostRune.DAM.rune(), 5, 0.2);
            }
            changed();
        } else {
            setChanged();
        }
        if (!lit()) {
            lightChanged(level);
        }
        return burned;
    }

    /** The burned-off dark rises out of the working spot and runs into the gem. */
    private void thread(ServerLevel level, BlockPos at, RandomSource random) {
        Vec3 gem = gem(worldPosition);
        Vec3 from = Vec3.atCenterOf(at).add(0, 0.4, 0);
        for (int i = 0; i < 6; i++) {
            Vec3 start = from.add((random.nextDouble() - 0.5) * 0.6, random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.6);
            // A void mote is sent where it ends, with the way back to where it starts.
            level.sendParticles(ModParticles.voidMote(i % 3 == 0 ? PLUM : DARK), gem.x, gem.y, gem.z, 0, start.x - gem.x, start.y - gem.y,
                    start.z - gem.z, 1);
        }
        level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), gem.x, gem.y, gem.z, 3, 0.12, 0.12, 0.12, 0.01);
        if (random.nextInt(3) == 0) {
            level.playSound(null, worldPosition.above(), ModSounds.LAMPPOST_WARD.get(), SoundSource.BLOCKS, 0.45F, 0.9F + random.nextFloat() * 0.2F);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HextechLamppostBlockEntity post) {
        post.tick((ServerLevel) level);
    }

    private void tick(ServerLevel level) {
        long now = level.getGameTime();
        long phase = now + Math.floorMod(worldPosition.asLong(), DRAW_TICKS);
        if (store < LOW && phase % DRAW_TICKS == 0) {
            boolean was = lit();
            if (drawLight(level, now) && !was) {
                lightChanged(level);
            }
        }
        if (lit() && phase % GIFT_TICKS == 0) {
            gifts(level);
        }
    }

    private boolean drawLight(ServerLevel level, long now) {
        for (EssenceTankBlockEntity jar : JarReach.around(level, worldPosition)) {
            int taken = Math.min(DRAW_MB, jar.tank().contents().get(Essence.LIGHT));
            if (taken <= 0) {
                continue;
            }
            if (!jar.isCreative()) {
                jar.tank().remove(EssenceMix.pure(Essence.LIGHT, taken));
                jar.setChanged();
            }
            store += taken * PER_MB;
            drewFrom = jar.getBlockPos();
            drewAt = now;
            Vec3 mouth = JarReach.mouth(drewFrom);
            level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.3F, 1.25F);
            changed();
            return true;
        }
        return false;
    }

    /** The light goes on or out: both halves follow, the gem catches with a soft swell. */
    private void lightChanged(ServerLevel level) {
        boolean lit = lit();
        for (BlockPos half : List.of(worldPosition, worldPosition.above())) {
            BlockState state = level.getBlockState(half);
            if (state.getBlock() instanceof HextechLamppostBlock && state.getValue(HextechLamppostBlock.LIT) != lit) {
                level.setBlock(half, state.setValue(HextechLamppostBlock.LIT, lit), Block.UPDATE_ALL);
            }
        }
        if (lit) {
            Vec3 gem = gem(worldPosition);
            level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), gem.x, gem.y, gem.z, 0, 1.2, 0, 0, 1);
            level.playSound(null, worldPosition.above(), ModSounds.LAMPPOST_LIGHT.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
        } else {
            level.playSound(null, worldPosition.above(), ModSounds.LAMP_FLICKER.get(), SoundSource.BLOCKS, 0.5F, 0.7F);
        }
    }

    /** People in the light heal (Healing); hostile things in it glow (Insight). One box query a round, only with the runes. */
    private void gifts(ServerLevel level) {
        int healing = strength(LamppostRune.HEALING);
        int insight = strength(LamppostRune.INSIGHT);
        if (healing == 0 && insight == 0) {
            return;
        }
        AABB light = new AABB(worldPosition).inflate(reach());
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, light)) {
            if (healing > 0 && entity instanceof Player player) {
                // Kept, not re-added every round: a fresh Regeneration every round would never reach its first heal.
                if (ToolEffects.keep(player, MobEffects.REGENERATION, HolderRune.at(LamppostRune.HEALING_TICKS, healing), 0)) {
                    ToolEffects.puff(level, player.position().add(0, 1, 0), LamppostRune.HEALING.rune(), 3, 0.3);
                }
            } else if (insight > 0 && entity instanceof Enemy) {
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, HolderRune.at(LamppostRune.GLOW_TICKS, insight), 0, true, false));
            }
        }
    }

    /** One line: what the lamppost is doing. */
    public Component status() {
        if (!lit()) {
            return Component.translatable("block.hextale.hextech_lamppost.status.dark");
        }
        return Component.translatable("block.hextale.hextech_lamppost.status.lit", reach(), Math.round(share() * 100),
                Mth.ceil(store / PER_MB));
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        store = input.getFloatOr("store", 0);
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
        drewFrom = input.read("drew_from", BlockPos.CODEC).orElse(null);
        drewAt = input.getLongOr("drew_at", -1000);
        burnedAt = input.getLongOr("burned_at", -1000);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        output.putFloat("store", store);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        if (drewFrom != null) {
            com.pockyl.hextale.port.ValueOutput.of(tag, registries).store("drew_from", BlockPos.CODEC, drewFrom);
            tag.putLong("drew_at", drewAt);
        }
        tag.putLong("burned_at", burnedAt);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.LAMPPOST_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.LAMPPOST_RUNES.get(), runes);
        }
    }

    @Override
    public void removeComponentsFromTag(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, net.minecraft.core.RegistryAccess.EMPTY);
        super.removeComponentsFromTag(output.tag());
        output.discard("runes");
    }

    /** For tests: fills the store straight away. */
    public void setStore(float store) {
        this.store = Math.max(0, store);
    }
}
