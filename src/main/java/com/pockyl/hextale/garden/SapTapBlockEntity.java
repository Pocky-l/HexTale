package com.pockyl.hextale.garden;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.farm.Felling;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModTags;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.TapRune;
import com.pockyl.hextale.runework.ToolEffects;

/**
 * A Sap Tap driven into the trunk of a living tree (logs on soil with grown leaves, like the sawmill's trees: a log
 * wall gives nothing) drips sap essence: {@value #DROP_MB} mB every {@value #DRIP_TICKS} ticks, faster from a magic tree
 * or a jungle trunk. The drops run into a jar, a pipe or a machine right under it; with nothing there they gather in the
 * spout, up to {@value #HELD}, for a glass bottle. Essence without boiling anything.
 */
public class SapTapBlockEntity extends BlockEntity {
    public static final int DRIP_TICKS = 200;
    public static final int DROP_MB = 2;
    public static final int HELD = 32;
    /** How often it looks again whether the trunk is a living tree. */
    private static final int TREE_CHECK_TICKS = 1200;

    private int held;
    private long nextDrip = -1;
    private long treeCheckedAt = -TREE_CHECK_TICKS;
    private boolean alive;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;

    public SapTapBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAP_TAP.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SapTapBlockEntity tap) {
        if (level instanceof ServerLevel server) {
            tap.tick(server, state);
        }
    }

    private void tick(ServerLevel level, BlockState state) {
        long now = level.getGameTime();
        if (now - treeCheckedAt >= TREE_CHECK_TICKS) {
            treeCheckedAt = now;
            boolean was = alive;
            alive = livingTree(level, worldPosition.relative(state.getValue(SapTapBlock.FACING)));
            if (was != alive) {
                changed();
            }
        }
        if (!alive) {
            return;
        }
        if (nextDrip < 0) {
            nextDrip = now + dripTicks(level, state);
        }
        if (now < nextDrip) {
            return;
        }
        nextDrip = now + dripTicks(level, state);
        int drop = DROP_MB;
        int spring = strength(TapRune.SPRING);
        if (spring > 0 && level.getRandom().nextFloat() < HolderRune.at(TapRune.DOUBLE_DROP, spring)) {
            drop *= 2;
            ToolEffects.puff(level, tip(state), TapRune.SPRING.rune(), 5, 0.15);
        }
        int sun = strength(TapRune.PHOTOSYNTHESIS);
        if (sun > 0 && level.isDay() && level.canSeeSky(worldPosition.above())
                && level.getRandom().nextFloat() < HolderRune.at(TapRune.SUN_DROP, sun)) {
            drop += DROP_MB;
            ToolEffects.puff(level, tip(state), TapRune.PHOTOSYNTHESIS.rune(), 5, 0.15);
        }
        if (passDown(level, drop)) {
            level.playSound(null, worldPosition, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, SoundSource.BLOCKS, 0.3F, 0.7F);
            return;
        }
        if (held < HELD) {
            held = Math.min(HELD, held + drop);
            changed();
        }
    }

    /** Drips run into whatever takes essence right under the spout; false when nothing there does. */
    private boolean passDown(ServerLevel level, int amount) {
        BlockPos below = worldPosition.below();
        if (!(level.getBlockEntity(below) instanceof EssenceNetwork.Receiver receiver)) {
            return false;
        }
        int all = amount + held;
        int taken = receiver.receive(Direction.UP, EssenceMix.pure(Essence.SAP, all), false);
        if (taken <= 0) {
            return false;
        }
        int left = all - taken;
        if (left != held) {
            held = Math.min(HELD, left);
            changed();
        }
        return true;
    }

    /** Ticks between drops: a magic tree or a jungle trunk gives faster, Haste quickens it. */
    private int dripTicks(ServerLevel level, BlockState state) {
        BlockState trunk = level.getBlockState(worldPosition.relative(state.getValue(SapTapBlock.FACING)));
        float ticks = DRIP_TICKS;
        if (trunk.is(ModTags.MAGIC_LOGS)) {
            ticks *= 0.5F;
        } else if (trunk.is(BlockTags.JUNGLE_LOGS)) {
            ticks *= 0.7F;
        }
        int haste = strength(TapRune.HASTE);
        if (haste > 0) {
            ticks *= HolderRune.at(TapRune.FASTER, haste);
        }
        return Math.max(20, Math.round(ticks));
    }

    /** Whether the log belongs to a living tree: down the trunk to its foot, and a tree with grown leaves stands on it. */
    static boolean livingTree(Level level, BlockPos log) {
        if (!level.getBlockState(log).is(BlockTags.LOGS)) {
            return false;
        }
        BlockPos foot = log;
        for (int i = 0; i < 16 && level.getBlockState(foot.below()).is(BlockTags.LOGS); i++) {
            foot = foot.below();
        }
        return Felling.find(level, foot) != null;
    }

    /** Where the drops fall from (the spout's lip). */
    public Vec3 tip(BlockState state) {
        Direction facing = state.getValue(SapTapBlock.FACING);
        // Under the drip lip: 2 px from the middle towards the log, 4 px above the bottom (model facing north: (8, 4, 6)).
        return Vec3.atCenterOf(worldPosition).add(facing.getStepX() * 0.125, -0.25, facing.getStepZ() * 0.125);
    }

    /** A glass bottle takes what gathered in the spout; empty when too little has. */
    public EssenceMix bottle() {
        if (held < Dose.SAP_MB) {
            return EssenceMix.EMPTY;
        }
        EssenceMix out = EssenceMix.pure(Essence.SAP, held);
        held = 0;
        changed();
        return out;
    }

    public int held() {
        return held;
    }

    /** Client: whether it is dripping now (a living tree behind it). */
    public boolean dripping() {
        return alive;
    }

    public Component status() {
        if (!alive) {
            return Component.translatable("block.hextale.sap_tap.dry");
        }
        return Component.translatable("block.hextale.sap_tap.status", held, HELD);
    }

    public int strength(TapRune rune) {
        return runes.strength(rune.rune());
    }

    public PickaxeRunes runes() {
        return runes;
    }

    public void setRunes(PickaxeRunes runes) {
        this.runes = runes;
        changed();
    }

    private void changed() {
        setChanged();
        if (level instanceof ServerLevel server) {
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        held = Mth.clamp(input.getIntOr("held", 0), 0, HELD);
        alive = input.getBooleanOr("alive", false);
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        output.putInt("held", held);
        output.putBoolean("alive", alive);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.TAP_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.TAP_RUNES.get(), runes);
        }
    }

    @Override
    public void removeComponentsFromTag(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, net.minecraft.core.RegistryAccess.EMPTY);
        super.removeComponentsFromTag(output.tag());
        output.discard("runes");
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
