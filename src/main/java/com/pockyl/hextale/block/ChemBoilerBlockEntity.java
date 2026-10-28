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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.chem.HeatUser;
import com.pockyl.hextale.chem.Overdrive;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.BoilerRune;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.ToolEffects;
import com.pockyl.hextale.thevoid.Thinning;
import com.pockyl.hextale.transport.WaterInlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The Chem Boiler's water, pressure and the machines its steam drives ({@link Overdrive}). It stands on a Chem Burner and
 * wants its flame only while some machine in reach asks for steam: then the pressure rises {@value #HEAT_RISE} a tick,
 * and from {@value #DRIVE_FROM} on the machines that asked work {@link #push()} times faster. The steam takes a
 * millibucket of water every few ticks of flame; without a flame the pressure sinks again. Every
 * {@value #FUMES_EVERY} ticks of driving the fumes of each driven machine wear the land ({@link Thinning#FUMES}).
 */
public class ChemBoilerBlockEntity extends BlockEntity implements HeatUser, EssenceNetwork.WaterReceiver, WaterInlet.Vessel {
    public static final int WATER_CAPACITY = 4000;
    public static final float MAX_PRESSURE = 100;
    /** Pressure from which the steam drives the machines. */
    public static final float DRIVE_FROM = 40;
    /** Pressure the flame adds per tick. */
    public static final float HEAT_RISE = 2;
    /** Pressure lost per tick without a flame. */
    public static final float COOLING = 0.5F;
    /** Most machines one boiler drives at once. */
    public static final int MAX_DRIVEN = 4;
    /** How many times faster a driven machine works. */
    public static final float PUSH = 2;
    /** Extra push while the burner below burns shimmer. */
    public static final float SHIMMER_PUSH = 0.5F;
    /** A machine stays under steam this many ticks after it last asked. */
    public static final int DRIVE_MEMORY = 40;
    public static final int FUMES_EVERY = 20;
    /** Ticks between two knocks of the safety valve while it drives. */
    public static final int KNOCK_EVERY = 30;
    private static final int SYNC_TICKS = 10;

    private int water;
    private float pressure;
    private int steamTicks;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    /** Server: the machines that asked for steam and the game time they last did. */
    private final Map<BlockPos, Long> asked = new HashMap<>();
    /** The machines under steam as the clients last heard of them (streams run to them). */
    private List<BlockPos> shown = List.of();
    private float syncedPressure = -1;
    private boolean dirty;
    private final WaterInlet waterInlet = new WaterInlet(this);

    // Client: the needle follows the pressure with a little lag.
    public float needle;
    public float needleBefore;
    /** Client: game time of the last knock of the safety valve. */
    public long knockedAt = -1000;

    public ChemBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEM_BOILER.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) {
            Overdrive.add(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            Overdrive.remove(level, worldPosition);
        }
        super.setRemoved();
    }

    // ---- Runes --------------------------------------------------------------------------------------------------------

    public PickaxeRunes runes() {
        return runes;
    }

    public int strength(BoilerRune rune) {
        return runes.strength(rune.rune());
    }

    /** Blocks every way its steam reaches. */
    public int reach() {
        return BoilerRune.reach(strength(BoilerRune.SHIFT));
    }

    /** How many times faster the machines it drives work. */
    public float push() {
        int bellows = strength(BoilerRune.BELLOWS);
        float push = PUSH + (bellows > 0 ? HolderRune.at(BoilerRune.PUSH, bellows) : 0);
        ChemBurnerBlockEntity burner = ChemBurnerBlockEntity.under(level, worldPosition);
        return burner != null && burner.pink() ? push + SHIMMER_PUSH : push;
    }

    /** Share of the fumes that wears the land (Purification scrubs the rest). */
    public float fumeShare() {
        int purification = strength(BoilerRune.PURIFICATION);
        return purification > 0 ? 1 - HolderRune.at(BoilerRune.SCRUBBED, purification) : 1;
    }

    // ---- Steam --------------------------------------------------------------------------------------------------------

    public int water() {
        return water;
    }

    public float pressure() {
        return pressure;
    }

    /** Whether the steam is up and some machine is under it. */
    public boolean driving() {
        return pressure >= DRIVE_FROM && !asked.isEmpty();
    }

    /** The machines under steam (server: those that asked; client: as last synced). */
    public List<BlockPos> driven() {
        return level != null && !level.isClientSide() ? List.copyOf(asked.keySet()) : shown;
    }

    /** Whether {@code pos} lies within its reach (a cube, every way). */
    public boolean reaches(BlockPos pos) {
        int reach = reach();
        return Math.abs(pos.getX() - worldPosition.getX()) <= reach && Math.abs(pos.getY() - worldPosition.getY()) <= reach
                && Math.abs(pos.getZ() - worldPosition.getZ()) <= reach && !pos.equals(worldPosition);
    }

    public boolean drives(BlockPos machine) {
        return asked.containsKey(machine);
    }

    /** Whether it can take on one more machine. */
    public boolean hasRoom() {
        return asked.size() < MAX_DRIVEN;
    }

    /** A machine asks for steam: it is taken on (or kept) and gets the push once the pressure is up. */
    public float drive(BlockPos machine) {
        if (level == null) {
            return 1;
        }
        asked.put(machine.immutable(), level.getGameTime());
        return pressure >= DRIVE_FROM ? push() : 1;
    }

    /** The burner below burns only while a machine wants steam and there is water to make it of. */
    @Override
    public boolean wantsHeat() {
        return water > 0 && !asked.isEmpty();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChemBoilerBlockEntity boiler) {
        boiler.tick((ServerLevel) level, state);
    }

    private void tick(ServerLevel level, BlockState state) {
        long now = level.getGameTime();
        int before = asked.size();
        asked.values().removeIf(time -> now - time > DRIVE_MEMORY);
        if (asked.size() != before) {
            dirty = true;
        }
        if (ChemBurnerBlockEntity.under(level, worldPosition) != null && water > 0) {
            pressure = Math.min(MAX_PRESSURE, pressure + HEAT_RISE);
            if (++steamTicks >= BoilerRune.waterTicks(strength(BoilerRune.DAM))) {
                steamTicks = 0;
                water--;
            }
        } else if (pressure > 0) {
            pressure = Math.max(0, pressure - COOLING);
        }
        boolean driving = driving();
        if (driving && now % FUMES_EVERY == 0) {
            breathe(level, now);
        }
        if (driving != state.getValue(ChemBoilerBlock.DRIVING)) {
            level.setBlock(worldPosition, state.setValue(ChemBoilerBlock.DRIVING, driving), Block.UPDATE_ALL);
            level.playSound(null, worldPosition, driving ? ModSounds.CHEM_BOILER_VENT.get() : ModSounds.FLAME_OUT.get(), SoundSource.BLOCKS,
                    0.6F, driving ? 1.0F : 0.7F);
            dirty = true;
        }
        if (Math.abs(pressure - syncedPressure) >= 4 || (pressure == 0) != (syncedPressure == 0)) {
            dirty = true;
        }
        if (dirty && now % SYNC_TICKS == 0) {
            dirty = false;
            syncedPressure = pressure;
            changed();
        }
    }

    /** The fumes of the work it drives, and the rhythm the Haste rune passes on to the people around. */
    private void breathe(ServerLevel level, long now) {
        int working = 0;
        for (long time : asked.values()) {
            if (now - time <= FUMES_EVERY) {
                working++;
            }
        }
        if (working > 0) {
            Thinning.add(level, worldPosition, working * Thinning.FUMES * fumeShare(), Thinning.Source.FUMES);
        }
        int haste = strength(BoilerRune.HASTE);
        if (haste > 0) {
            int reach = reach();
            List<Player> players = level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(reach));
            for (Player player : players) {
                player.addEffect(new MobEffectInstance(MobEffects.HASTE, FUMES_EVERY * 3, HolderRune.at(BoilerRune.HASTE_LEVEL, haste) - 1,
                        true, false, true));
            }
            if (!players.isEmpty() && level.getRandom().nextInt(6) == 0) {
                ToolEffects.puff(level, Vec3.atCenterOf(worldPosition).add(0, 0.6, 0), BoilerRune.HASTE.rune(), 5, 0.25);
            }
        }
    }

    /** Fills with up to {@code amount} mB of water. */
    public int fillWater(int amount, boolean simulate) {
        int taken = Math.max(0, Math.min(amount, WATER_CAPACITY - water));
        if (taken > 0 && !simulate) {
            water += taken;
            dirty = true;
            setChanged();
        }
        return taken;
    }

    @Override
    public int receiveWater(Direction side, int amount, boolean simulate) {
        return side == Direction.DOWN ? 0 : fillWater(amount, simulate);
    }

    public WaterInlet waterInlet() {
        return waterInlet;
    }

    @Override
    public int waterRoom() {
        return Math.max(0, WATER_CAPACITY - water);
    }

    @Override
    public void pipeWater(int amount) {
        fillWater(amount, false);
    }

    /** One line: what the boiler is doing. */
    public Component status() {
        if (water <= 0) {
            return Component.translatable("block.hextale.chem_boiler.status.dry");
        }
        if (level == null || !(level.getBlockEntity(worldPosition.below()) instanceof ChemBurnerBlockEntity)) {
            return Component.translatable("block.hextale.chem_boiler.status.no_burner");
        }
        if (getBlockState().getValue(ChemBoilerBlock.DRIVING)) {
            return Component.translatable("block.hextale.chem_boiler.status.driving", driven().size(), HolderRune.factor(push()));
        }
        if (pressure >= 1) {
            return Component.translatable("block.hextale.chem_boiler.status.steam", Math.round(pressure));
        }
        return Component.translatable("block.hextale.chem_boiler.status.idle", reach());
    }

    // ---- Client -------------------------------------------------------------------------------------------------------

    public static void clientTick(Level level, BlockPos pos, BlockState state, ChemBoilerBlockEntity boiler) {
        boiler.needleBefore = boiler.needle;
        float target = boiler.pressure / MAX_PRESSURE;
        boiler.needle += (target - boiler.needle) * 0.15F;
        if (!state.getValue(ChemBoilerBlock.DRIVING)) {
            return;
        }
        long now = level.getGameTime();
        RandomSource random = level.getRandom();
        if ((now + Math.floorMod(pos.asLong(), KNOCK_EVERY)) % KNOCK_EVERY == 0) {
            boiler.knockedAt = now;
            Vec3 valve = ChemBoilerBlock.valve(pos);
            for (int i = 0; i < 3; i++) {
                level.addParticle(ModParticles.vapour(ChemBoilerBlock.STEAM), valve.x, valve.y, valve.z,
                        (random.nextDouble() - 0.5) * 0.04, 0.06 + random.nextDouble() * 0.03, (random.nextDouble() - 0.5) * 0.04);
            }
            level.playLocalSound(pos, ModSounds.CHEM_BOILER_KNOCK.get(), SoundSource.BLOCKS, 0.35F, 0.9F + random.nextFloat() * 0.2F, false);
        }
        com.pockyl.hextale.client.render.PressureFx.tick(level, boiler, now);
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        water = input.getIntOr("water", 0);
        pressure = input.getFloatOr("pressure", 0);
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
        shown = input.read("driven", BlockPos.CODEC.listOf()).orElse(List.of());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("water", water);
        output.putFloat("pressure", pressure);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        List<BlockPos> driven = pressure >= DRIVE_FROM ? new ArrayList<>(asked.keySet()) : List.of();
        tag.store("driven", BlockPos.CODEC.listOf(), driven);
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

    /** For tests: sets the pressure straight away. */
    public void setPressure(float pressure) {
        this.pressure = Mth.clamp(pressure, 0, MAX_PRESSURE);
    }
}
