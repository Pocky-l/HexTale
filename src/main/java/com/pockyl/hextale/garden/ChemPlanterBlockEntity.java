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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.PlanterRune;
import com.pockyl.hextale.runework.ToolEffects;

/**
 * A Chem Planter's tank: doses of one kind ({@link Dose}), up to {@value #MAX_CHARGES}, poured from vials and bottles
 * or piped in as sap essence ({@value Dose#SAP_MB} mB a dose, through its sides and bottom). When the plant on it grows a
 * stage ({@link GardenEvents}), a dose may make it grow one more; its runes add to that.
 */
public class ChemPlanterBlockEntity extends BlockEntity implements EssenceNetwork.Receiver, DoseBed {
    public static final int MAX_CHARGES = 16;
    /** Ticks between the stages the planter feeds its plant (Growth makes it sooner). */
    public static final int FEED_TICKS = 100;

    private Dose dose = Dose.NONE;
    private int charges;
    /** Sap piped in that is not a whole dose yet. */
    private int sap;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;

    public ChemPlanterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEM_PLANTER.get(), pos, state);
    }

    @Override
    public Dose dose() {
        return charges > 0 ? dose : Dose.NONE;
    }

    @Override
    public int charges() {
        return charges;
    }

    @Override
    public int room(Dose kind) {
        return kind == Dose.NONE ? 0 : charges <= 0 || dose == kind ? MAX_CHARGES - charges : 0;
    }

    /**
     * Pours doses into the tank: the same kind fills up, another kind only into an empty tank (doses never mix).
     *
     * @return how many it took
     */
    @Override
    public int pour(Dose kind, int count) {
        int taken = Math.min(count, room(kind));
        if (taken <= 0) {
            return 0;
        }
        dose = kind;
        charges += taken;
        changed();
        return taken;
    }

    /** Sap through a pipe: only pure sap, whole doses of it (what is short of a dose waits in the pipe's stead here). */
    @Override
    public int receive(Direction side, EssenceMix offer, boolean simulate) {
        if (side == Direction.UP || offer.isEmpty() || offer.get(Essence.SAP) != offer.total()) {
            return 0;
        }
        int roomMb = room(Dose.SAP) * Dose.SAP_MB - sap;
        int taken = Math.max(0, Math.min(roomMb, offer.total()));
        if (taken > 0 && !simulate) {
            sap += taken;
            int whole = sap / Dose.SAP_MB;
            if (whole > 0) {
                sap -= whole * Dose.SAP_MB;
                pour(Dose.SAP, whole);
            } else {
                setChanged();
            }
        }
        return taken;
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, ChemPlanterBlockEntity planter) {
        if (level instanceof ServerLevel server && planter.charges > 0 && server.getGameTime() >= planter.nextFeed) {
            planter.feed(server);
        }
    }

    /** When the planter next feeds its plant (game time). */
    private long nextFeed;

    /** Ticks to the next stage it feeds: {@value #FEED_TICKS}, shortened by Growth. */
    private int feedTicks() {
        int growth = strength(PlanterRune.GROWTH);
        return growth > 0 ? Math.round(FEED_TICKS * HolderRune.at(PlanterRune.SOONER, growth)) : FEED_TICKS;
    }

    /**
     * Feeds the plant on it: a stage of growth for a dose, by day or night, light or none (Dam may keep the dose, Pollen
     * may let a plant beside the planter grow too). A ripe plant takes nothing.
     */
    void feed(ServerLevel level) {
        nextFeed = level.getGameTime() + feedTicks();
        BlockPos plant = worldPosition.above();
        BlockState state = level.getBlockState(plant);
        if (!SteepedSoilBlock.grows(state) || !state.isRandomlyTicking()) {
            return;
        }
        RandomSource random = level.getRandom();
        GardenEvents.growMore(level, plant, dose);
        int dam = strength(PlanterRune.DAM);
        if (dam > 0 && random.nextFloat() < HolderRune.at(PlanterRune.THRIFT, dam)) {
            ToolEffects.puff(level, Vec3.atCenterOf(worldPosition).add(0, 0.6, 0), PlanterRune.DAM.rune(), 6, 0.3);
        } else if (--charges <= 0) {
            charges = 0;
            dose = Dose.NONE;
        }
        int pollen = strength(PlanterRune.POLLEN);
        if (pollen > 0 && random.nextFloat() < HolderRune.at(PlanterRune.SPREAD, pollen)) {
            Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos beside = plant.relative(side);
            BlockState next = level.getBlockState(beside);
            if (SteepedSoilBlock.grows(next)) {
                GardenEvents.growMore(level, beside, dose == Dose.NONE ? Dose.GROWTH : dose);
                ToolEffects.puff(level, Vec3.atCenterOf(beside), PlanterRune.POLLEN.rune(), 8, 0.3);
            }
        }
        changed();
    }

    /** The chance a ripe harvest of the plant on it comes twice: its dose's, and Fertility's on top. */
    float doubleHarvest() {
        int fertility = strength(PlanterRune.FERTILITY);
        return (charges > 0 ? dose.harvest() : 0) + (fertility > 0 ? HolderRune.at(PlanterRune.DOUBLE_HARVEST, fertility) : 0);
    }

    public Component status() {
        if (charges <= 0) {
            return Component.translatable("block.hextale.chem_planter.empty");
        }
        return Component.translatable("block.hextale.chem_planter.status",
                Component.translatable(dose.translationKey()).withColor(dose.color()), charges, MAX_CHARGES);
    }

    /** How full the tank is, 0-1 (the renderer's liquid level behind the glass). */
    public float fill() {
        return Mth.clamp(charges / (float) MAX_CHARGES, 0, 1);
    }

    public int strength(PlanterRune rune) {
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

    /** A pour lands: a splash of the dose's colour on the soil, a glug. */
    void poured(ServerLevel level) {
        level.sendParticles(ModParticles.drop(dose.color()), worldPosition.getX() + 0.5, worldPosition.getY() + 1.02, worldPosition.getZ() + 0.5,
                10, 0.25, 0.02, 0.25, 0.04);
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6F, 0.9F);
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        dose = input.read("dose", Dose.CODEC).orElse(Dose.NONE);
        charges = Mth.clamp(input.getIntOr("charges", 0), 0, MAX_CHARGES);
        sap = Math.max(0, input.getIntOr("sap", 0));
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        if (charges > 0) {
            output.store("dose", Dose.CODEC, dose);
            output.putInt("charges", charges);
        }
        if (sap > 0) {
            output.putInt("sap", sap);
        }
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.PLANTER_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.PLANTER_RUNES.get(), runes);
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
