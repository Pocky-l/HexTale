package com.pockyl.hextale.garden;

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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.SprinklerRune;
import com.pockyl.hextale.runework.ToolEffects;

import java.util.ArrayList;
import java.util.List;

/**
 * The Essence Sprinkler: a turning copper head on a riser. Pipes bring it water and sap essence (any side but the top),
 * vials pour brews into it. Every {@value #PULSE_TICKS} ticks it sprays the beds around it (radius 3, Shift further,
 * from two blocks below it to its own level): dry farmland it wets stays moist ({@value #WATER_PER_BED} mB of water each),
 * and up to {@value #STEEPS_PER_PULSE} beds running low (fewer than {@value #TOP_UP} doses) get a dose - of its brew
 * first, of sap ({@value Dose#SAP_MB} mB a dose) when the brew is out; a bed holding a better dose is left alone. It only
 * tops up, so it spends what the plants use. Farmland it doses becomes Steeped Soil; a Chem Planter's tank is topped up.
 */
public class EssenceSprinklerBlockEntity extends BlockEntity implements EssenceNetwork.Receiver, EssenceNetwork.WaterReceiver, DoseBed {
    public static final int PULSE_TICKS = 100;
    public static final int STEEPS_PER_PULSE = 6;
    public static final int MAX_CHARGES = 16;
    public static final int SAP_TANK = 256;
    public static final int WATER_TANK = 1000;
    /** Water a pulse spends for every dry farmland it wets. */
    public static final int WATER_PER_BED = 1;
    /** Beds are topped up only while they hold fewer doses than this (a planter: twice as many). */
    public static final int TOP_UP = 2;
    /** How long the head keeps turning after a pulse. */
    public static final int SPIN_TICKS = 60;

    private Dose dose = Dose.NONE;
    private int charges;
    private int sap;
    private int water;
    private long sprayedAt = -1000;
    private int sprayColor = Dose.GROWTH.color();
    private PickaxeRunes runes = PickaxeRunes.EMPTY;

    public EssenceSprinklerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ESSENCE_SPRINKLER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssenceSprinklerBlockEntity sprinkler) {
        if (level.getGameTime() % PULSE_TICKS == Math.floorMod(pos.asLong(), PULSE_TICKS) && level instanceof ServerLevel server) {
            sprinkler.pulse(server);
        }
    }

    public int radius() {
        return SprinklerRune.RADIUS[Math.min(4, strength(SprinklerRune.SHIFT))];
    }

    /** One spray over the beds around it. */
    public void pulse(ServerLevel level) {
        if (charges <= 0 && sap < Dose.SAP_MB && water <= 0) {
            return;
        }
        RandomSource random = level.getRandom();
        int radius = radius();
        List<BlockPos> wetted = new ArrayList<>();
        List<BlockPos> beds = new ArrayList<>();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dy = -2; dy <= 0; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    at.setWithOffset(worldPosition, dx, dy, dz);
                    BlockState state = level.getBlockState(at);
                    if (state.is(Blocks.FARMLAND) || state.getBlock() instanceof SteepedSoilBlock || level.getBlockEntity(at) instanceof ChemPlanterBlockEntity) {
                        beds.add(at.immutable());
                    }
                }
            }
        }
        if (beds.isEmpty()) {
            return;
        }
        boolean wet = water >= WATER_PER_BED || sap >= Dose.SAP_MB || charges > 0;
        java.util.Collections.shuffle(beds, new java.util.Random(random.nextLong()));
        int steeped = 0;
        for (BlockPos bed : beds) {
            BlockState state = level.getBlockState(bed);
            if (wet && state.is(Blocks.FARMLAND) && state.getValue(FarmlandBlock.MOISTURE) < FarmlandBlock.MAX_MOISTURE) {
                level.setBlock(bed, state.setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE), Block.UPDATE_CLIENTS);
                wetted.add(bed);
                water = Math.max(0, water - WATER_PER_BED);
            }
            if (steeped < STEEPS_PER_PULSE && doseBed(level, bed)) {
                steeped++;
                wetted.add(bed);
            }
        }
        if (wetted.isEmpty()) {
            return;
        }
        sprayedAt = level.getGameTime();
        sprayColor = charges > 0 ? dose.color() : sap >= Dose.SAP_MB ? Dose.SAP.color() : 0x8FC8E8;
        for (BlockPos bed : wetted) {
            level.sendParticles(ModParticles.drop(sprayColor), bed.getX() + 0.5, bed.getY() + 1.6, bed.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.02);
        }
        Vec3 head = Vec3.atBottomCenterOf(worldPosition).add(0, 0.85, 0);
        level.sendParticles(ModParticles.drop(sprayColor), head.x, head.y, head.z, 12, 0.25, 0.05, 0.25, 0.12);
        level.playSound(null, worldPosition, SoundEvents.WEATHER_RAIN, SoundSource.BLOCKS, 0.12F, 1.6F);
        int healing = strength(SprinklerRune.HEALING);
        if (healing > 0) {
            net.minecraft.world.phys.AABB spray = new net.minecraft.world.phys.AABB(worldPosition).inflate(radius, 2, radius);
            for (net.minecraft.world.entity.player.Player player : level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class, spray)) {
                if (ToolEffects.keep(player, net.minecraft.world.effect.MobEffects.REGENERATION, HolderRune.at(SprinklerRune.HEALING_TICKS, healing), 0)) {
                    ToolEffects.puff(level, player.position().add(0, 1, 0), SprinklerRune.HEALING.rune(), 6, 0.4);
                }
            }
        }
        changed();
    }

    /** Gives one bed a dose (brew first, then sap); false when it took none. */
    private boolean doseBed(ServerLevel level, BlockPos bed) {
        Dose kind = charges > 0 ? dose : sap >= Dose.SAP_MB ? Dose.SAP : Dose.NONE;
        if (kind == Dose.NONE) {
            return false;
        }
        boolean took;
        if (level.getBlockEntity(bed) instanceof ChemPlanterBlockEntity planter) {
            // Only a tank running low is topped up.
            took = (planter.charges() < TOP_UP * 2 || planter.dose() != kind && planter.charges() == 0) && planter.pour(kind, 1) > 0;
        } else {
            BlockState state = level.getBlockState(bed);
            if (state.getBlock() instanceof SteepedSoilBlock) {
                Dose there = state.getValue(SteepedSoilBlock.DOSE);
                int count = state.getValue(SteepedSoilBlock.CHARGES);
                // Soil that still holds enough of this dose is left alone: the spray only tops up.
                if (there == kind && count >= TOP_UP) {
                    return false;
                }
                // A better dose stays: sap never washes out growth tonic or shimmer.
                if (count > 0 && there != kind && there.extraStage() > kind.extraStage()) {
                    return false;
                }
            }
            took = SteepedSoilBlock.steep(level, bed, kind, 1);
        }
        if (!took) {
            return false;
        }
        int dam = strength(SprinklerRune.DAM);
        boolean free = dam > 0 && level.getRandom().nextFloat() < HolderRune.at(SprinklerRune.THRIFT, dam);
        if (free) {
            ToolEffects.puff(level, Vec3.atCenterOf(worldPosition).add(0, 0.5, 0), SprinklerRune.DAM.rune(), 5, 0.25);
        } else if (kind == Dose.SAP) {
            sap -= Dose.SAP_MB;
        } else if (--charges <= 0) {
            charges = 0;
            dose = Dose.NONE;
        }
        int fertility = strength(SprinklerRune.FERTILITY);
        if (fertility > 0 && level.getRandom().nextFloat() < HolderRune.at(SprinklerRune.QUICKEN, fertility)) {
            GardenEvents.growMore(level, bed.above(), kind);
            ToolEffects.puff(level, Vec3.atCenterOf(bed.above()), SprinklerRune.FERTILITY.rune(), 6, 0.3);
        }
        return true;
    }

    // ---- Tanks -----------------------------------------------------------------------------------------------------

    @Override
    public Dose dose() {
        return charges > 0 ? dose : Dose.NONE;
    }

    @Override
    public int charges() {
        return charges;
    }

    /** Brews only (sap comes as essence through pipes or bottles: those go to the sap tank). */
    @Override
    public int room(Dose kind) {
        if (kind == Dose.SAP) {
            return (SAP_TANK - sap) / Dose.SAP_MB;
        }
        return kind == Dose.NONE ? 0 : charges <= 0 || dose == kind ? MAX_CHARGES - charges : 0;
    }

    @Override
    public int pour(Dose kind, int count) {
        int taken = Math.min(count, room(kind));
        if (taken <= 0) {
            return 0;
        }
        if (kind == Dose.SAP) {
            sap += taken * Dose.SAP_MB;
        } else {
            dose = kind;
            charges += taken;
        }
        changed();
        return taken;
    }

    /** Pure sap through a pipe, into the sap tank. */
    @Override
    public int receive(Direction side, EssenceMix offer, boolean simulate) {
        if (side == Direction.UP || offer.isEmpty() || offer.get(Essence.SAP) != offer.total()) {
            return 0;
        }
        int taken = Math.max(0, Math.min(SAP_TANK - sap, offer.total()));
        if (taken > 0 && !simulate) {
            sap += taken;
            setChanged();
        }
        return taken;
    }

    @Override
    public int receiveWater(Direction side, int amount, boolean simulate) {
        if (side == Direction.UP) {
            return 0;
        }
        int taken = Math.max(0, Math.min(WATER_TANK - water, amount));
        if (taken > 0 && !simulate) {
            water += taken;
            setChanged();
        }
        return taken;
    }

    public int sap() {
        return sap;
    }

    public int water() {
        return water;
    }

    public Component status() {
        Component brew = charges > 0
                ? Component.translatable("block.hextale.essence_sprinkler.brew", Component.translatable(dose.translationKey()).withColor(dose.color()), charges)
                : Component.translatable("block.hextale.essence_sprinkler.no_brew");
        return Component.translatable("block.hextale.essence_sprinkler.status", brew, sap, water);
    }

    /** Client: how far the head has turned, in degrees, easing out after the last spray. */
    public float spin(float partialTick) {
        if (level == null) {
            return 0;
        }
        float age = level.getGameTime() + partialTick - sprayedAt;
        if (age < 0 || age > SPIN_TICKS) {
            return 0;
        }
        float t = age / SPIN_TICKS;
        return 720 * (t * (2 - t));
    }

    public int sprayColor() {
        return sprayColor;
    }

    public long sprayedAt() {
        return sprayedAt;
    }

    void poured(ServerLevel level) {
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6F, 0.9F);
    }

    public int strength(SprinklerRune rune) {
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        dose = input.read("dose", Dose.CODEC).orElse(Dose.NONE);
        charges = Mth.clamp(input.getIntOr("charges", 0), 0, MAX_CHARGES);
        sap = Mth.clamp(input.getIntOr("sap", 0), 0, SAP_TANK);
        water = Mth.clamp(input.getIntOr("water", 0), 0, WATER_TANK);
        sprayedAt = input.getLongOr("sprayed_at", -1000);
        sprayColor = input.getIntOr("spray_color", Dose.GROWTH.color());
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (charges > 0) {
            output.store("dose", Dose.CODEC, dose);
            output.putInt("charges", charges);
        }
        output.putInt("sap", sap);
        output.putInt("water", water);
        output.putLong("sprayed_at", sprayedAt);
        output.putInt("spray_color", sprayColor);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.SPRINKLER_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.SPRINKLER_RUNES.get(), runes);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
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
