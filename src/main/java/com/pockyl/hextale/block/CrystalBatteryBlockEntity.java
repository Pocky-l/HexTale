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

import com.pockyl.hextale.network.EssenceFxPayload;
import com.pockyl.hextale.network.RuneFxPayload;
import com.pockyl.hextale.network.WeaponFxPayload;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.BatteryRune;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.ToolEffects;
import com.pockyl.hextale.transport.ItemIntake;

import org.jspecify.annotations.Nullable;

/**
 * The Crystal Battery's store: up to {@link #CAPACITY} charge (more with Cache), soaked from crystals given to it and
 * handed on every {@link #FEED_TICKS} ticks to the Hex Workbenches within {@link #REACH} blocks (further with Chain
 * Lightning), at most {@link #FEED} to each. A stream of motes shows where the charge goes. Charge and runes stay with
 * the block when it is picked up. With a Sunlight Cell it drinks a point of charge now and then while it stands under the
 * open sky by day in dry weather ({@link #sunlit}), drawn as a shaft of light with motes drifting down into it.
 */
public class CrystalBatteryBlockEntity extends BlockEntity {
    public static final int CAPACITY = 600;
    public static final int REACH = 4;
    public static final int FEED_TICKS = 20;
    public static final int FEED = 6;
    private static final int HEXTECH = 0x7FD8FF;
    /** How high over its top the shaft of sunlight starts. */
    private static final float SUN_SHAFT = 2.5F;
    /** Every this many points drunk from the sun there is a soft sound (the light alone shows the rest). */
    private static final int SUN_SOUND_EVERY = 6;

    private int charge;
    private int sunDrunk;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    /** Hoppers, pylons and other mods' pipes feed it crystals; they soak in as they arrive. */
    private final ItemIntake intake = new ItemIntake(new ItemIntake.Mouth() {
        @Override
        public int room(net.neoforged.neoforge.transfer.item.ItemResource resource, java.util.List<ItemIntake.Arrival> pending,
                        java.util.@Nullable UUID deliverer) {
            int value = HexWorkbenchBlockEntity.chargeOf(resource.toStack(1));
            if (value <= 0) {
                return 0;
            }
            int promised = 0;
            for (ItemIntake.Arrival arrival : pending) {
                promised += HexWorkbenchBlockEntity.chargeOf(arrival.resource().toStack(1)) * arrival.count();
            }
            return Math.max(0, (capacity() - charge - promised) / value);
        }

        @Override
        public void arrive(ItemStack stack, java.util.@Nullable UUID deliverer) {
            soak(stack, stack.getCount());
        }
    });

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

    /** The item handler hoppers and pipes put crystals in through (insert only). */
    public ItemIntake intake() {
        return intake;
    }

    /** Soaks one crystal into charge; false when it has no room for it. */
    public boolean soak(ItemStack crystal) {
        return soak(crystal, 1) > 0;
    }

    /** Soaks up to {@code count} of the crystal into charge, as many as there is room for; returns how many. */
    public int soak(ItemStack crystal, int count) {
        int value = HexWorkbenchBlockEntity.chargeOf(crystal);
        int dam = strength(BatteryRune.DAM);
        int soaked = 0;
        boolean bonus = false;
        while (soaked < count && value > 0 && charge + value <= capacity()) {
            boolean lucky = dam > 0 && level != null && level.getRandom().nextFloat() < HolderRune.at(BatteryRune.BONUS, dam);
            charge = Math.min(capacity(), charge + value + (lucky ? (value + 1) / 2 : 0));
            bonus |= lucky;
            soaked++;
        }
        if (soaked == 0) {
            return 0;
        }
        changed();
        if (level instanceof ServerLevel server) {
            Vec3 at = Vec3.atCenterOf(worldPosition);
            // The crystal comes apart into the battery: its pieces whirl into the core, sparks are drawn in from round the
            // cube and a hexagon flares on its top.
            for (int i = 0; i < 6; i++) {
                // Each piece is spawned at the core with its way back to where it starts (over the cube) as velocity.
                double angle = i * Math.PI / 3;
                server.sendParticles(ModParticles.itemDraw(crystal), at.x, at.y + 0.1, at.z, 0, Math.cos(angle) * 0.35, 0.85,
                        Math.sin(angle) * 0.35, 1);
            }
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                server.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x + Math.cos(angle) * 0.7, at.y + 0.2, at.z + Math.sin(angle) * 0.7, 0,
                        -Math.cos(angle), -0.2, -Math.sin(angle), 0.09);
            }
            RuneFxPayload.send(server, RuneFxPayload.sigil(at.add(0, 0.52, 0), 0.6F, HEXTECH));
            server.playSound(null, worldPosition, ModSounds.ORB_AWAKEN.get(), SoundSource.BLOCKS, 0.5F, 1.6F);
            if (bonus) {
                ToolEffects.puff(server, at.add(0, 0.4, 0), BatteryRune.DAM.rune(), 6, 0.3);
            }
        }
        return soaked;
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

    /** Whether the sun reaches the block: nothing over it up to the sky, day, no rain. */
    public static boolean sunlit(Level level, BlockPos pos) {
        return level.isBrightOutside() && !level.isRaining()
                && level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() + 1
                && level.canSeeSky(pos.above());
    }

    /** Sunlight Cell: a point of charge every {@link BatteryRune#SUN_TICKS} while sunlit. */
    private void bask(ServerLevel level) {
        int sun = strength(BatteryRune.SUNLIGHT_CELL);
        if (sun <= 0) {
            return;
        }
        int every = HolderRune.at(BatteryRune.SUN_TICKS, sun);
        if (level.getGameTime() % every != Math.floorMod(worldPosition.asLong(), every) || charge >= capacity() || !sunlit(level, worldPosition)) {
            return;
        }
        charge++;
        sunDrunk++;
        changed();
        Vec3 top = Vec3.atCenterOf(worldPosition).add(0, 0.5, 0);
        WeaponFxPayload.send(level, WeaponFxPayload.Kind.SUNBEAM, top, top, BatteryRune.SUNLIGHT_CELL.rune().color(), SUN_SHAFT);
        if (sunDrunk % SUN_SOUND_EVERY == 1) {
            level.playSound(null, worldPosition, ModSounds.RUNE_BLOOM.get(), SoundSource.BLOCKS, 0.15F, 1.3F + level.getRandom().nextFloat() * 0.2F);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrystalBatteryBlockEntity battery) {
        if (level instanceof ServerLevel server) {
            battery.bask(server);
        }
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
                    // A thread of hextech light from the battery's top into the worktop, renewed every other feed so
                    // a steady charge reads as one steady stream.
                    if (level.getGameTime() / FEED_TICKS % 2 == 0) {
                        EssenceFxPayload.pour(server, Vec3.atCenterOf(pos).add(0, 0.5, 0),
                                Vec3.atCenterOf(at).add(0, 0.45, 0), HEXTECH, FEED_TICKS * 2 - 4);
                    }
                }
            }
        }
        // A Bulwark heart within reach is fed like a workbench.
        for (net.minecraft.world.entity.player.Player wearer : server.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                new net.minecraft.world.phys.AABB(pos).inflate(reach, 2, reach))) {
            if (battery.charge <= 0) {
                break;
            }
            int given = com.pockyl.hextale.runework.BulwarkHeart.feed(wearer, Math.min(FEED, battery.charge));
            if (given > 0) {
                battery.charge -= given;
                if (level.getGameTime() / FEED_TICKS % 2 == 0) {
                    EssenceFxPayload.pour(server, Vec3.atCenterOf(pos).add(0, 0.5, 0), wearer.position().add(0, wearer.getBbHeight() * 0.65, 0),
                            HEXTECH, FEED_TICKS * 2 - 4);
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
