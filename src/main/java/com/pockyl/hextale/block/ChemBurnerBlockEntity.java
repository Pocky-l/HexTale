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
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.chem.HeatUser;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.BurnerRune;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.ToolEffects;

import org.jspecify.annotations.Nullable;

/**
 * The Chem Burner: chemtech's cheap heat. It burns whatever it is fed - vials of combustible mix (by hand or hoppers) or
 * essence from pipes at its back and sides, unknown essence (the separator's dark waste) twice as long as any other - in
 * an almost invisible green flame, the hottest heat there is (5). It burns only while the machine standing on it has
 * work for it ({@link HeatUser}): a crucible, an alembic, a separator (which then needs no blaze powder) or a Hex Furnace
 * (which then needs no fuel).
 */
public class ChemBurnerBlockEntity extends BlockEntity implements Container, EssenceNetwork.Receiver {
    /** Essence it holds for burning. */
    public static final int TANK = 1000;
    /** Ticks of flame from a vial of combustible mix (shimmer burns longer, {@link Brew#burnTicks()}), from a millibucket
     *  of essence and of unknown essence. */
    public static final int VIAL_TICKS = Brew.FUEL_TICKS;
    public static final int ESSENCE_TICKS = 20;
    public static final int UNKNOWN_TICKS = 40;
    private static final int SYNC_TICKS = 20;
    private static final int WARMTH_EVERY = 40;

    private ItemStack fuel = ItemStack.EMPTY;
    private EssenceMix tank = EssenceMix.EMPTY;
    private int burn;
    private int burnFull = 1;
    /** Whether the portion burning now is shimmer: the flame turns pink. */
    private boolean pink;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    private boolean dirty;

    public ChemBurnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEM_BURNER.get(), pos, state);
    }

    /** The burner under this machine while it burns, else null. */
    public static @Nullable ChemBurnerBlockEntity under(@Nullable Level level, BlockPos machine) {
        if (level == null) {
            return null;
        }
        BlockState below = level.getBlockState(machine.below());
        return ChemBurnerBlock.isLit(below) && level.getBlockEntity(machine.below()) instanceof ChemBurnerBlockEntity burner ? burner : null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChemBurnerBlockEntity burner) {
        burner.tick((ServerLevel) level, state);
    }

    private void tick(ServerLevel level, BlockState state) {
        boolean lit = false;
        if (wanted()) {
            if (burn <= 0) {
                refuel(level);
            }
            if (burn > 0) {
                burn--;
                lit = true;
                dirty = true;
            }
        }
        if (lit != state.getValue(ChemBurnerBlock.LIT)) {
            level.setBlock(worldPosition, state.setValue(ChemBurnerBlock.LIT, lit), Block.UPDATE_ALL);
            if (lit) {
                level.playSound(null, worldPosition, ModSounds.CHEM_BURNER_IGNITE.get(), SoundSource.BLOCKS, 0.7F,
                        0.9F + level.getRandom().nextFloat() * 0.2F);
            } else {
                level.playSound(null, worldPosition, ModSounds.FLAME_OUT.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            }
            sync();
        }
        long now = level.getGameTime();
        if (lit && now % WARMTH_EVERY == 0) {
            warm(level);
        }
        if (dirty && now % SYNC_TICKS == 0) {
            dirty = false;
            setChanged();
            sync();
        }
    }

    /** Whether the machine on top has work for the flame. */
    private boolean wanted() {
        return level != null && level.getBlockEntity(worldPosition.above()) instanceof HeatUser user && user.wantsHeat();
    }

    /** The next portion of fuel: essence from the tank first, then a vial. Eternal Flame may make it last twice as long. */
    private void refuel(ServerLevel level) {
        int ticks;
        if (tank.unknown() > 0) {
            tank = tank.minus(EssenceMix.unknown(1));
            ticks = UNKNOWN_TICKS;
        } else if (!tank.isEmpty()) {
            tank = tank.minus(tank.part(1));
            ticks = ESSENCE_TICKS;
        } else if (burnTicks(fuel) > 0) {
            Brew brew = ChemVialItem.brew(fuel);
            ticks = brew.burnTicks();
            fuel.shrink(1);
            if (pink != brew.shimmer()) {
                pink = brew.shimmer();
                sync();
            }
        } else {
            return;
        }
        if (ticks < VIAL_TICKS && pink) {
            pink = false;
            sync();
        }
        int flame = strength(BurnerRune.ETERNAL_FLAME);
        if (flame > 0 && level.getRandom().nextFloat() < HolderRune.at(BurnerRune.LONGER_BURN, flame)) {
            ticks *= 2;
            if (ticks >= VIAL_TICKS || level.getRandom().nextInt(8) == 0) {
                ToolEffects.puff(level, Vec3.atCenterOf(worldPosition).add(0, 0.3, 0), BurnerRune.ETERNAL_FLAME.rune(), 6, 0.3);
                // The flame burns for nothing.
                com.pockyl.hextale.network.RuneEffectPayload.show(level, com.pockyl.hextale.network.RuneEffectPayload.Family.RISE, Vec3.atCenterOf(worldPosition).add(0, 0.3, 0), com.pockyl.hextale.registry.ModParticles.FIRE, 0.6F);
            }
        }
        burn = ticks;
        burnFull = ticks;
        setChanged();
    }

    /** Fireproofing: the players beside the lit burner keep Fire Resistance. */
    private void warm(ServerLevel level) {
        int warmth = strength(BurnerRune.FIREPROOFING);
        if (warmth <= 0) {
            return;
        }
        AABB around = new AABB(worldPosition).inflate(BurnerRune.WARMTH_RADIUS);
        for (Player player : level.getEntitiesOfClass(Player.class, around)) {
            if (ToolEffects.keep(player, MobEffects.FIRE_RESISTANCE, HolderRune.at(BurnerRune.WARMTH_TICKS, warmth), 0)) {
                ToolEffects.puff(level, player.position().add(0, 1, 0), BurnerRune.FIREPROOFING.rune(), 8, 0.4);
                // Fire kept off the keeper.
                com.pockyl.hextale.network.RuneEffectPayload.show(level, com.pockyl.hextale.network.RuneEffectPayload.Family.RISE, player.position().add(0, 1, 0), com.pockyl.hextale.registry.ModParticles.FIRE, 1.0F);
            }
        }
    }

    public int strength(BurnerRune rune) {
        return runes.strength(rune.rune());
    }

    public PickaxeRunes runes() {
        return runes;
    }

    public void setRunes(PickaxeRunes runes) {
        this.runes = runes;
        setChanged();
    }

    // ---- Fuel ---------------------------------------------------------------------------------------------------------

    public ItemStack fuel() {
        return fuel;
    }

    public EssenceMix tank() {
        return tank;
    }

    /** Ticks of flame left of the portion burning now, and that portion's whole length. */
    public int burn() {
        return burn;
    }

    public int burnFull() {
        return burnFull;
    }

    /** Ticks of flame in everything it holds. */
    public int fuelTicks() {
        return burn + tank.unknown() * UNKNOWN_TICKS + (tank.total() - tank.unknown()) * ESSENCE_TICKS
                + fuel.getCount() * burnTicks(fuel);
    }

    /** Ticks of flame one vial of this gives (0: it does not burn). */
    public static int burnTicks(ItemStack stack) {
        Brew brew = ChemVialItem.brew(stack);
        return brew == null ? 0 : brew.burnTicks();
    }

    /** Whether the flame is shimmer's pink one. */
    public boolean pink() {
        return pink;
    }

    /** Takes as much of the vials as fits; returns how many. */
    public int feed(ItemStack stack) {
        if (burnTicks(stack) <= 0 || !fuel.isEmpty() && !ItemStack.isSameItemSameComponents(fuel, stack)) {
            return 0;
        }
        int room = stack.getMaxStackSize() - fuel.getCount();
        int taken = Math.min(room, stack.getCount());
        if (taken <= 0) {
            return 0;
        }
        if (fuel.isEmpty()) {
            fuel = stack.copyWithCount(taken);
        } else {
            fuel.grow(taken);
        }
        changed();
        return taken;
    }

    /** Pours essence into the tank, as much as fits; returns what was taken. */
    public EssenceMix pour(EssenceMix offer) {
        int taken = Math.min(TANK - tank.total(), offer.total());
        if (taken <= 0) {
            return EssenceMix.EMPTY;
        }
        EssenceMix part = offer.part(taken);
        tank = tank.plus(part);
        changed();
        return part;
    }

    @Override
    public int receive(Direction side, EssenceMix offer, boolean simulate) {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof ChemBurnerBlock block) || !block.isPort(state, side)) {
            return 0;
        }
        int taken = Math.min(TANK - tank.total(), offer.total());
        if (taken > 0 && !simulate) {
            tank = tank.plus(offer.part(taken));
            dirty = true;
            setChanged();
        }
        return Math.max(0, taken);
    }

    /** "Burning - 2 min of fuel", or why it does not. */
    public Component status() {
        int seconds = fuelTicks() / 20;
        Component time = Component.translatable("block.hextale.chem_burner.time", seconds / 60, String.format("%02d", seconds % 60));
        if (getBlockState().getValue(ChemBurnerBlock.LIT)) {
            return Component.translatable("block.hextale.chem_burner.status.lit", time);
        }
        if (fuelTicks() <= 0) {
            return Component.translatable("block.hextale.chem_burner.status.no_fuel");
        }
        return Component.translatable("block.hextale.chem_burner.status.idle", time);
    }

    private void changed() {
        setChanged();
        sync();
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Container (hoppers feed the vials) -------------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return fuel.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return fuel;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack taken = fuel.split(count);
        changed();
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack taken = fuel;
        fuel = ItemStack.EMPTY;
        return taken;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        fuel = stack;
        changed();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return burnTicks(stack) > 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        fuel = ItemStack.EMPTY;
    }

    // ---- Saving -----------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuel = input.read("fuel", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        tank = input.read("tank", EssenceMix.CODEC).orElse(EssenceMix.EMPTY);
        burn = input.getIntOr("burn", 0);
        burnFull = Math.max(1, input.getIntOr("burn_full", 1));
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
        pink = input.getBooleanOr("pink", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!fuel.isEmpty()) {
            output.store("fuel", ItemStack.CODEC, fuel);
        }
        if (!tank.isEmpty()) {
            output.store("tank", EssenceMix.CODEC, tank);
        }
        output.putInt("burn", burn);
        output.putInt("burn_full", burnFull);
        output.putBoolean("pink", pink);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.BURNER_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.BURNER_RUNES.get(), runes);
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
