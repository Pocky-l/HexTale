package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.chem.Brew;
import com.pockyl.hextale.chem.ChemVialItem;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.BrazierRune;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.ToolEffects;

/**
 * The Pink Brazier: shimmer burning in an open iron bowl, a pink flame whose light makes everything green around it grow
 * wild - every two seconds a few plants in its light grow a stage. It burns vials of refined shimmer (raw
 * shimmer burns too, half as long); hoppers feed it from any side.
 */
public class PinkBrazierBlockEntity extends BlockEntity implements Container {
    /** Ticks of flame from a vial of refined shimmer; raw burns half as long. */
    public static final int REFINED_TICKS = 6000;
    public static final int RAW_TICKS = REFINED_TICKS / 2;
    /** Ticks between two breaths of growth, and how many spots each tries. */
    public static final int PULSE_TICKS = 40;
    public static final int PULSE_TRIES = 6;

    private ItemStack fuel = ItemStack.EMPTY;
    private int burn;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;

    public PinkBrazierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PINK_BRAZIER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PinkBrazierBlockEntity brazier) {
        brazier.tick((ServerLevel) level, state);
    }

    private void tick(ServerLevel level, BlockState state) {
        if (burn <= 0) {
            refuel(level);
        }
        boolean lit = burn > 0;
        if (lit) {
            burn--;
            if (level.getGameTime() % PULSE_TICKS == 0) {
                pulse(level);
            }
            if (burn % 200 == 0) {
                setChanged();
            }
        }
        if (lit != state.getValue(PinkBrazierBlock.LIT)) {
            level.setBlock(worldPosition, state.setValue(PinkBrazierBlock.LIT, lit), Block.UPDATE_ALL);
            if (lit) {
                level.playSound(null, worldPosition, ModSounds.PINK_BRAZIER_IGNITE.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
            } else {
                level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.4F);
            }
        }
    }

    public static int burnTicks(ItemStack stack) {
        Brew brew = ChemVialItem.brew(stack);
        if (brew == Brew.REFINED_SHIMMER) {
            return REFINED_TICKS;
        }
        return brew == Brew.RAW_SHIMMER ? RAW_TICKS : 0;
    }

    private void refuel(ServerLevel level) {
        int ticks = burnTicks(fuel);
        if (ticks <= 0) {
            return;
        }
        fuel.shrink(1);
        int flame = strength(BrazierRune.ETERNAL_FLAME);
        if (flame > 0 && level.getRandom().nextFloat() < HolderRune.at(BrazierRune.LONGER_BURN, flame)) {
            ticks *= 2;
            ToolEffects.puff(level, Vec3.atCenterOf(worldPosition).add(0, 0.5, 0), BrazierRune.ETERNAL_FLAME.rune(), 10, 0.3);
        }
        burn = ticks;
        changed();
    }

    public int radius() {
        return BrazierRune.RADIUS[Math.min(4, strength(BrazierRune.SHIFT))];
    }

    /** A breath of growth: a few plants in the light grow (Pollen may make it a whole bone-meal), players heal. */
    private void pulse(ServerLevel level) {
        RandomSource random = level.getRandom();
        int radius = radius();
        int pollen = strength(BrazierRune.POLLEN);
        for (int i = 0; i < PULSE_TRIES; i++) {
            BlockPos at = worldPosition.offset(random.nextInt(radius * 2 + 1) - radius, random.nextInt(5) - 2,
                    random.nextInt(radius * 2 + 1) - radius);
            BlockState plant = level.getBlockState(at);
            if (!grows(plant)) {
                continue;
            }
            if (pollen > 0 && plant.getBlock() instanceof BonemealableBlock bonemealable && bonemealable.isValidBonemealTarget(level, at, plant, BonemealSource.INTERACTION)
                    && random.nextFloat() < HolderRune.at(BrazierRune.BLOOM, pollen)) {
                bonemealable.performBonemeal(level, random, at, plant, BonemealSource.INTERACTION);
                ToolEffects.puff(level, Vec3.atCenterOf(at), BrazierRune.POLLEN.rune(), 8, 0.3);
            } else {
                growStep(level, at, plant, random);
            }
            level.sendParticles(ModParticles.SHIMMER_MOTE.get(), at.getX() + 0.5, at.getY() + 0.6, at.getZ() + 0.5, 3, 0.25, 0.2, 0.25, 0.005);
        }
        int healing = strength(BrazierRune.HEALING);
        if (healing > 0) {
            AABB around = new AABB(worldPosition).inflate(radius);
            for (Player player : level.getEntitiesOfClass(Player.class, around)) {
                if (ToolEffects.keep(player, MobEffects.REGENERATION, HolderRune.at(BrazierRune.HEALING_TICKS, healing), 0)) {
                    ToolEffects.puff(level, player.position().add(0, 1, 0), BrazierRune.HEALING.rune(), 8, 0.4);
                }
            }
        }
    }

    /** One step of growth: a plant with an age grows one stage at once; anything else gets a random tick. */
    private static void growStep(ServerLevel level, BlockPos at, BlockState plant, RandomSource random) {
        for (Property<?> property : plant.getProperties()) {
            if (property instanceof IntegerProperty age && property.getName().equals("age")) {
                int now = plant.getValue(age);
                if (age.getPossibleValues().contains(now + 1)) {
                    level.setBlock(at, plant.setValue(age, now + 1), Block.UPDATE_CLIENTS);
                    return;
                }
                break;
            }
        }
        plant.randomTick(level, at, random);
    }

    /** What its light makes grow: anything green that grows by itself (crops, saplings, stems, cane, vines...). */
    public static boolean grows(BlockState state) {
        if (!state.isRandomlyTicking() || state.is(BlockTags.DIRT)) {
            return false;
        }
        return state.getBlock() instanceof BonemealableBlock || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.CROPS)
                || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.CACTUS);
    }

    public ItemStack fuel() {
        return fuel;
    }

    public int burn() {
        return burn;
    }

    /** Takes as many shimmer vials as fit; returns how many. */
    public int feed(ItemStack stack) {
        if (burnTicks(stack) <= 0 || !fuel.isEmpty() && !ItemStack.isSameItemSameComponents(fuel, stack)) {
            return 0;
        }
        int taken = Math.min(stack.getCount(), stack.getMaxStackSize() - fuel.getCount());
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

    public Component status() {
        int seconds = (burn + fuel.getCount() * burnTicks(fuel)) / 20;
        if (seconds <= 0) {
            return Component.translatable("block.hextale.pink_brazier.status.out");
        }
        return Component.translatable("block.hextale.pink_brazier.status.lit", seconds / 60, String.format("%02d", seconds % 60), radius());
    }

    public int strength(BrazierRune rune) {
        return runes.strength(rune.rune());
    }

    public PickaxeRunes runes() {
        return runes;
    }

    public void setRunes(PickaxeRunes runes) {
        this.runes = runes;
        setChanged();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Container ----------------------------------------------------------------------------------------------------

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

    // ---- Saving -------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuel = input.read("fuel", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        burn = input.getIntOr("burn", 0);
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!fuel.isEmpty()) {
            output.store("fuel", ItemStack.CODEC, fuel);
        }
        output.putInt("burn", burn);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.BRAZIER_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.BRAZIER_RUNES.get(), runes);
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
