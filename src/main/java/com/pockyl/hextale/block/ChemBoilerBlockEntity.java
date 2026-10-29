package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.mojang.serialization.Codec;
import com.pockyl.hextale.chem.HeatUser;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.farm.SectionScan;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.BoilerRune;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.ToolEffects;
import com.pockyl.hextale.thevoid.Thinning;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The Chem Boiler's work: chemtech's crude and quick way to split essence, the twin of the hextech Separator. A mixture
 * comes into its vessel - pushed through pipes, or drawn by the boiler itself out of the jars within
 * {@link JarReach#RADIUS} that hold a mixture and wear no sticker - and is heated under pressure over the Chem Burner
 * below. Each time the pressure reaches {@value #CRACK_AT} the safety valve knocks and a batch of {@value #BATCH} mB is
 * cracked at once into ALL its essences: every essence runs into the nearest jar within reach whose sticker takes it,
 * whatever finds no jar waits in the boiler until one is there. Nothing to choose, nothing to learn but the boiler -
 * but crude: a share of every batch goes up as fumes that wear the land ({@link Thinning#FUMES}), and a share burns down
 * to unknown essence, which drips into the burner below and feeds its flame. The Separator stays the precise way.
 */
public class ChemBoilerBlockEntity extends BlockEntity implements HeatUser, EssenceNetwork.Receiver {
    /** Millibuckets of mixture the vessel holds, and of cracked essence waiting for a jar. */
    public static final int VESSEL = 256;
    public static final int HELD = 256;
    public static final float MAX_PRESSURE = 100;
    /** Pressure at which a batch cracks. */
    public static final float CRACK_AT = 80;
    /** Pressure the safety valve lets out with a batch. */
    public static final float VENT = 40;
    /** Pressure the flame adds per tick, and what is lost per tick without it. */
    public static final float HEAT_RISE = 2;
    public static final float COOLING = 0.5F;
    /** Millibuckets cracked at a time. */
    public static final int BATCH = 16;
    /** Share of a batch that goes up as fumes. */
    public static final float FUMES_SHARE = 0.1F;
    /** Share of what is left that burns down to unknown essence. */
    public static final float UNKNOWN_SHARE = 0.15F;
    public static final int DRAW_TICKS = 20;
    public static final int DRAW_MB = 16;
    public static final int POUR_TICKS = 10;
    /** Ticks a draw's or a pour's stream shows. */
    public static final int STREAM_TICKS = 40;
    /** Hextech lamps this close choke in the fumes now and then. */
    public static final int LAMP_REACH = 4;
    private static final int SYNC_TICKS = 10;
    private static final Codec<List<JarReach.Draw>> DRAWS = Codec.INT_STREAM.xmap(
            stream -> JarReach.read(stream.toArray()), draws -> java.util.Arrays.stream(JarReach.write(draws)));

    private final EssenceTank mix = new EssenceTank(VESSEL);
    private final EssenceTank held = new EssenceTank(HELD);
    private float pressure;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    private @Nullable BlockPos drewFrom;
    private long drewAt = -1000;
    private List<JarReach.Draw> pours = List.of();
    private long pouredAt = -1000;
    private long crackedAt = -1000;
    private float syncedPressure = -1;
    private boolean dirty;

    // Client: the needle follows the pressure with a little lag; the valve jumps when a batch cracks.
    public float needle;
    public float needleBefore;
    public long knockedAt = -1000;
    private long shownCrack = -1000;

    public ChemBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEM_BOILER.get(), pos, state);
    }

    // ---- Runes --------------------------------------------------------------------------------------------------------

    public PickaxeRunes runes() {
        return runes;
    }

    public int strength(BoilerRune rune) {
        return runes.strength(rune.rune());
    }

    /** Pressure the flame adds per tick (Steam: faster). */
    public float rise() {
        int steam = strength(BoilerRune.STEAM);
        return steam > 0 ? HEAT_RISE * HolderRune.at(BoilerRune.FASTER, steam) : HEAT_RISE;
    }

    /** Share of a batch that goes up as fumes (Dam keeps some in). */
    public float fumesShare() {
        int dam = strength(BoilerRune.DAM);
        return dam > 0 ? FUMES_SHARE * (1 - HolderRune.at(BoilerRune.FUMES_KEPT, dam)) : FUMES_SHARE;
    }

    /** Share of the rest that burns down to unknown essence (Purification saves some). */
    public float unknownShare() {
        int purification = strength(BoilerRune.PURIFICATION);
        return purification > 0 ? UNKNOWN_SHARE * (1 - HolderRune.at(BoilerRune.UNKNOWN_SAVED, purification)) : UNKNOWN_SHARE;
    }

    // ---- State --------------------------------------------------------------------------------------------------------

    public EssenceMix mixture() {
        return mix.contents();
    }

    /** Cracked essence waiting for a jar. */
    public EssenceMix held() {
        return held.contents();
    }

    public float pressure() {
        return pressure;
    }

    /** The jar it drew from lately, or null. */
    public @Nullable BlockPos drawing() {
        return level != null && drewFrom != null && level.getGameTime() - drewAt <= STREAM_TICKS ? drewFrom : null;
    }

    public long drewAt() {
        return drewAt;
    }

    /** The jars it poured into lately (empty when none). */
    public List<JarReach.Draw> pouring() {
        return level != null && level.getGameTime() - pouredAt <= STREAM_TICKS ? pours : List.of();
    }

    public long pouredAt() {
        return pouredAt;
    }

    /** The burner heats it while there is a mixture to crack and room for what comes out. */
    @Override
    public boolean wantsHeat() {
        return !mix.isEmpty() && held.space() > 0;
    }

    /** Pipes push mixtures in through every side but its floor. */
    @Override
    public int receive(Direction side, EssenceMix offer, boolean simulate) {
        if (side == Direction.DOWN) {
            return 0;
        }
        int taken = mix.fill(offer, simulate);
        if (taken > 0 && !simulate) {
            dirty = true;
            setChanged();
        }
        return taken;
    }

    /** Pours a mixture in (a bottle); returns what was taken. */
    public int pourIn(EssenceMix offer) {
        int taken = mix.fill(offer, false);
        if (taken > 0) {
            changed();
        }
        return taken;
    }

    // ---- Work ---------------------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChemBoilerBlockEntity boiler) {
        boiler.tick((ServerLevel) level, state);
    }

    private void tick(ServerLevel level, BlockState state) {
        long now = level.getGameTime();
        boolean heated = ChemBurnerBlockEntity.under(level, worldPosition) != null;
        if (heated) {
            pressure = Math.min(MAX_PRESSURE, pressure + rise());
        } else if (pressure > 0) {
            pressure = Math.max(0, pressure - COOLING);
        }
        if (now % DRAW_TICKS == 0 && mix.space() >= DRAW_MB) {
            draw(level, now);
        }
        if (now % POUR_TICKS == 0 && !held.isEmpty()) {
            pour(level, now);
        }
        if (pressure >= CRACK_AT && !mix.isEmpty() && held.space() >= Math.min(BATCH, mix.amount())) {
            crack(level, now);
        }
        boolean working = heated && wantsHeat();
        if (working != state.getValue(ChemBoilerBlock.WORKING)) {
            level.setBlock(worldPosition, state.setValue(ChemBoilerBlock.WORKING, working), Block.UPDATE_ALL);
            // The tank takes up its steam, or lets the last of it go.
            level.playSound(null, worldPosition, ModSounds.CHEM_BOILER_VENT.get(), SoundSource.BLOCKS, 0.5F, working ? 1.0F : 0.8F);
        }
        if (Math.abs(pressure - syncedPressure) >= 4 || (pressure == 0) != (syncedPressure == 0)) {
            dirty = true;
        }
        if (dirty && now % SYNC_TICKS == 0) {
            changed();
        }
    }

    /** Draws a portion of a mixture out of the nearest jar without a sticker that holds one (pure jars are left alone). */
    private void draw(ServerLevel level, long now) {
        for (EssenceTankBlockEntity jar : JarReach.around(level, worldPosition)) {
            if (jar.sticker() != null || jar.tank().contents().kinds() < 2) {
                continue;
            }
            EssenceMix taken = jar.tank().drain(Math.min(DRAW_MB, mix.space()), jar.isCreative());
            if (taken.isEmpty()) {
                continue;
            }
            if (!jar.isCreative()) {
                jar.setChanged();
            }
            mix.fill(taken, false);
            drewFrom = jar.getBlockPos();
            drewAt = now;
            Vec3 mouth = JarReach.mouth(drewFrom);
            level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.3F, 0.9F);
            changed();
            return;
        }
    }

    /**
     * Cracks one batch: the valve knocks and lets the pressure out, part of the batch goes up as fumes, part burns down to
     * unknown essence, the rest is split into its essences and poured into the jars.
     */
    private void crack(ServerLevel level, long now) {
        RandomSource random = level.getRandom();
        int amount = Math.min(BATCH, Math.min(mix.amount(), held.space()));
        EssenceMix batch = mix.drain(amount, false);
        int purifyingFire = strength(BoilerRune.PURIFYING_FIRE);
        boolean clean = purifyingFire > 0 && random.nextFloat() < HolderRune.at(BoilerRune.CLEAN_BATCH, purifyingFire);
        int fumes = clean ? 0 : Math.round(batch.total() * fumesShare());
        EssenceMix left = batch.part(batch.total() - fumes);
        if (!clean) {
            EssenceMix known = left.minus(EssenceMix.unknown(left.unknown()));
            EssenceMix burnt = known.part(Math.min(known.total(), Math.round(known.total() * unknownShare())));
            left = left.minus(burnt).plus(EssenceMix.unknown(burnt.total()));
        }
        held.fill(left, false);
        pressure = Math.max(0, pressure - VENT);
        crackedAt = now;
        Vec3 valve = ChemBoilerBlock.valve(worldPosition);
        level.playSound(null, worldPosition, ModSounds.CHEM_BOILER_KNOCK.get(), SoundSource.BLOCKS, 0.55F, 0.9F + random.nextFloat() * 0.2F);
        level.sendParticles(ModParticles.vapour(ChemBoilerBlock.STEAM), valve.x, valve.y, valve.z, 4, 0.06, 0.04, 0.06, 0.02);
        if (clean) {
            ToolEffects.puff(level, valve, BoilerRune.PURIFYING_FIRE.rune(), 8, 0.2);
        }
        if (fumes > 0) {
            Thinning.add(level, worldPosition, fumes * Thinning.FUMES, Thinning.Source.FUMES);
            level.sendParticles(ModParticles.vapour(ChemBoilerBlock.FUMES), valve.x, valve.y + 0.1, valve.z, 3, 0.1, 0.05, 0.1, 0.01);
            if (random.nextInt(3) == 0) {
                chokeLamps(level, random);
            }
        }
        pour(level, now);
        changed();
    }

    /** Pours what waits into the jars: each essence into the nearest jar whose sticker takes it, the unknown into the flame. */
    private void pour(ServerLevel level, long now) {
        EssenceMix contents = held.contents();
        List<JarReach.Draw> poured = new ArrayList<>();
        List<EssenceTankBlockEntity> jars = null;
        for (Map.Entry<Essence, Integer> entry : contents.asMap().entrySet()) {
            Essence essence = entry.getKey();
            int left = entry.getValue();
            if (jars == null) {
                jars = JarReach.around(level, worldPosition);
            }
            for (EssenceTankBlockEntity jar : jars) {
                if (jar.sticker() != essence || jar.isCreative()) {
                    continue;
                }
                int took = jar.tank().fill(EssenceMix.pure(essence, left), false);
                if (took <= 0) {
                    continue;
                }
                jar.setChanged();
                held.remove(EssenceMix.pure(essence, took));
                poured.add(new JarReach.Draw(jar.getBlockPos(), essence));
                left -= took;
                if (left <= 0) {
                    break;
                }
            }
        }
        if (contents.unknown() > 0 && level.getBlockEntity(worldPosition.below()) instanceof ChemBurnerBlockEntity burner) {
            EssenceMix fed = burner.pour(EssenceMix.unknown(contents.unknown()));
            held.remove(fed);
        }
        if (!poured.isEmpty()) {
            pours = poured;
            pouredAt = now;
            Vec3 mouth = JarReach.mouth(poured.getFirst().jar());
            level.playSound(null, mouth.x, mouth.y, mouth.z, ModSounds.TANK_GLUG.get(), SoundSource.BLOCKS, 0.3F, 1.1F);
            dirty = true;
        }
    }

    /** Hextech lamps near the fumes choke for a moment: chemistry dims the crystal (only a sign). */
    private void chokeLamps(ServerLevel level, RandomSource random) {
        BlockPos min = worldPosition.offset(-LAMP_REACH, -LAMP_REACH, -LAMP_REACH);
        BlockPos max = worldPosition.offset(LAMP_REACH, LAMP_REACH, LAMP_REACH);
        List<BlockPos> lamps = new ArrayList<>();
        SectionScan.forEach(level, min, max, state -> state.is(ModBlocks.HEXTECH_LAMP.get()), (pos, state) -> lamps.add(pos.immutable()));
        for (BlockPos lamp : lamps) {
            BlockState state = level.getBlockState(lamp);
            if (!state.getValue(HextechLampBlock.FLICKER)) {
                HextechLampBlock.choke(state, level, lamp, random);
            }
        }
    }

    /** One line: what the boiler is doing. */
    public Component status() {
        if (level == null || !(level.getBlockEntity(worldPosition.below()) instanceof ChemBurnerBlockEntity)) {
            return Component.translatable("block.hextale.chem_boiler.status.no_burner");
        }
        if (mix.isEmpty() && held.isEmpty()) {
            return Component.translatable("block.hextale.chem_boiler.status.empty");
        }
        if (held.space() < Math.min(BATCH, Math.max(1, mix.amount()))) {
            return Component.translatable("block.hextale.chem_boiler.status.full");
        }
        if (mix.isEmpty()) {
            return Component.translatable("block.hextale.chem_boiler.status.waiting", held.amount());
        }
        return Component.translatable("block.hextale.chem_boiler.status.cracking", Math.round(pressure));
    }

    // ---- Client -------------------------------------------------------------------------------------------------------

    public static void clientTick(Level level, BlockPos pos, BlockState state, ChemBoilerBlockEntity boiler) {
        boiler.needleBefore = boiler.needle;
        boiler.needle += (boiler.pressure / MAX_PRESSURE - boiler.needle) * 0.2F;
        long now = level.getGameTime();
        if (boiler.crackedAt != boiler.shownCrack) {
            boolean fresh = now - boiler.crackedAt < SYNC_TICKS * 2;
            boiler.shownCrack = boiler.crackedAt;
            if (fresh) {
                boiler.knockedAt = now;
                // The pressure drops at once with the knock.
                boiler.needle = Math.min(boiler.needle, boiler.pressure / MAX_PRESSURE);
            }
        }
        BlockPos jar = boiler.drawing();
        if (jar != null) {
            float age = now - boiler.drewAt;
            int color = JarReach.color(level, new JarReach.Draw(jar, null));
            if (color != -1) {
                JarReach.shed(level, JarReach.mouth(jar), ChemBoilerBlock.inlet(pos), color, age / 12F, (age - (STREAM_TICKS - 14)) / 12F);
            }
        }
        float poured = now - boiler.pouredAt;
        for (JarReach.Draw pour : boiler.pouring()) {
            JarReach.shed(level, ChemBoilerBlock.inlet(pos), JarReach.mouth(pour.jar()), pour.essence() == null ? ChemBoilerBlock.STEAM
                    : pour.essence().color(), poured / 12F, (poured - (STREAM_TICKS - 14)) / 12F);
        }
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    private void changed() {
        dirty = false;
        syncedPressure = pressure;
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mix.load(input, "mix");
        held.load(input, "held");
        pressure = input.getFloatOr("pressure", 0);
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
        drewFrom = input.read("drew_from", BlockPos.CODEC).orElse(null);
        drewAt = input.getLongOr("drew_at", -1000);
        pours = input.read("pours", DRAWS).orElse(List.of());
        pouredAt = input.getLongOr("poured_at", -1000);
        crackedAt = input.getLongOr("cracked_at", -1000);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        mix.save(output, "mix");
        held.save(output, "held");
        output.putFloat("pressure", pressure);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        if (drewFrom != null) {
            tag.store("drew_from", BlockPos.CODEC, drewFrom);
            tag.putLong("drew_at", drewAt);
        }
        if (!pours.isEmpty()) {
            tag.store("pours", DRAWS, pours);
            tag.putLong("poured_at", pouredAt);
        }
        tag.putLong("cracked_at", crackedAt);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.BOILER_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.BOILER_RUNES.get(), runes);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("runes");
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // What was in the vessel is lost on the ground.
        if (level instanceof ServerLevel server) {
            EssenceMix lost = mix.contents().plus(held.contents());
            Thinning.poured(server, pos, lost, lost.total(), false);
        }
        super.preRemoveSideEffects(pos, state);
    }

    /** For tests: sets the pressure straight away. */
    public void setPressure(float pressure) {
        this.pressure = Mth.clamp(pressure, 0, MAX_PRESSURE);
    }

    /** For tests and runes: sets the runes. */
    public void setRunes(PickaxeRunes runes) {
        this.runes = runes;
        setChanged();
    }
}
