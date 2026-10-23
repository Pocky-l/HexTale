package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.BatteryRune;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.ToolEffects;

/**
 * The Crystal Battery's store: up to {@link #CAPACITY} charge (more with Cache), soaked from crystals given to it and
 * handed on every {@link #FEED_TICKS} ticks to the Hex Workbenches within {@link #REACH} blocks (further with Chain
 * Lightning), at most {@link #FEED} to each. A stream of motes shows where the charge goes. Charge and runes stay with
 * the block when it is picked up.
 */
public class CrystalBatteryBlockEntity extends BlockEntity {
    public static final int CAPACITY = 600;
    public static final int REACH = 4;
    public static final int FEED_TICKS = 20;
    public static final int FEED = 6;
    private static final int HEXTECH = 0x7FD8FF;

    private int charge;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;

    public CrystalBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRYSTAL_BATTERY.get(), pos, state);
    }

    public int charge() {
        return charge;
    }

    public int strength(BatteryRune rune) {
        return runes.strength(rune.rune());
    }

    public void setRunes(PickaxeRunes runes) {
        this.runes = runes;
        setChanged();
    }

    public int capacity() {
        int cache = strength(BatteryRune.CACHE);
        return Math.round(CAPACITY * (1 + (cache > 0 ? HolderRune.at(BatteryRune.ROOM, cache) : 0)));
    }

    public int reach() {
        int chain = strength(BatteryRune.CHAIN_LIGHTNING);
        return REACH + (chain > 0 ? HolderRune.at(BatteryRune.REACH, chain) : 0);
    }

    /** Soaks one crystal into charge; false when it has no room for it. */
    public boolean soak(ItemStack crystal) {
        int value = HexWorkbenchBlockEntity.chargeOf(crystal);
        if (value <= 0 || charge + value > capacity()) {
            return false;
        }
        int dam = strength(BatteryRune.DAM);
        boolean bonus = dam > 0 && level != null && level.getRandom().nextFloat() < HolderRune.at(BatteryRune.BONUS, dam);
        charge = Math.min(capacity(), charge + value + (bonus ? value / 2 : 0));
        changed();
        if (level instanceof ServerLevel server) {
            Vec3 at = Vec3.atCenterOf(worldPosition);
            server.sendParticles(ModParticles.ring(HEXTECH), at.x, at.y, at.z, 0, 0.9, 0, 0, 1);
            server.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x, at.y, at.z, 8, 0.2, 0.2, 0.2, 0.08);
            server.playSound(null, worldPosition, ModSounds.ORB_AWAKEN.get(), SoundSource.BLOCKS, 0.5F, 1.6F);
            if (bonus) {
                ToolEffects.puff(server, at.add(0, 0.4, 0), BatteryRune.DAM.rune(), 6, 0.3);
            }
        }
        return true;
    }

    /** Takes up to {@code amount} charge out (the Hexcore and tests draw on it); returns what was taken. */
    public int draw(int amount) {
        int taken = Math.min(amount, charge);
        if (taken > 0) {
            charge -= taken;
            changed();
        }
        return taken;
    }

    public int signal() {
        return charge <= 0 ? 0 : 1 + Math.round(14.0F * charge / capacity());
    }

    public Component status() {
        return Component.translatable("block.hextale.crystal_battery.status", charge, capacity());
    }

    private void changed() {
        setChanged();
        if (level != null) {
            int fill = charge <= 0 ? 0 : Math.min(4, 1 + charge * 4 / Math.max(1, capacity() + 1));
            BlockState state = getBlockState();
            if (state.getValue(CrystalBatteryBlock.LEVEL) != fill) {
                level.setBlock(worldPosition, state.setValue(CrystalBatteryBlock.LEVEL, fill), Block.UPDATE_ALL);
            }
            level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrystalBatteryBlockEntity battery) {
        if (battery.charge <= 0 || level.getGameTime() % FEED_TICKS != Math.floorMod(pos.asLong(), FEED_TICKS)
                || !(level instanceof ServerLevel server)) {
            return;
        }
        int reach = battery.reach();
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-reach, -2, -reach), pos.offset(reach, 2, reach))) {
            if (battery.charge <= 0) {
                break;
            }
            if (level.getBlockEntity(at) instanceof HexWorkbenchBlockEntity bench) {
                int given = bench.receive(Math.min(FEED, battery.charge));
                if (given > 0) {
                    battery.charge -= given;
                    Vec3 to = Vec3.atCenterOf(at).add(0, 0.5, 0);
                    Vec3 from = Vec3.atCenterOf(pos);
                    for (int i = 0; i < 3; i++) {
                        server.sendParticles(ModParticles.flow(HEXTECH), to.x, to.y, to.z, 0, from.x - to.x, from.y - to.y, from.z - to.z, 1.0);
                    }
                }
            }
        }
        battery.changed();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        charge = input.getIntOr("charge", 0);
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("charge", charge);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.BATTERY_RUNES.get(), PickaxeRunes.EMPTY);
        charge = components.getOrDefault(ModDataComponents.BATTERY_CHARGE.get(), 0);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.BATTERY_RUNES.get(), runes);
        }
        if (charge > 0) {
            components.set(ModDataComponents.BATTERY_CHARGE.get(), charge);
        }
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("runes");
        output.discard("charge");
    }
}
