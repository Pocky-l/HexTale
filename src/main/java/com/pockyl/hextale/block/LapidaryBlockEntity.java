package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.mojang.serialization.Codec;
import com.pockyl.hextale.crafting.Cleavage;
import com.pockyl.hextale.menu.LapidaryMenu;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The lapidary's crystal, disc and finished stone, and the cut in progress ({@link Cleavage}). A crystal put into the
 * clamp gets a fresh seed (its own cleavage planes); every stroke the worker sends is played on the server's copy of the
 * cut, with cooling measured on the server's clock. A cut ends in the next grade of crystal or, with too many cracks, in
 * a handful of hextech dust; either way the disc wears by one plus one per crack.
 */
public class LapidaryBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int INPUT = 0;
    public static final int DISC = 1;
    /** In the model's pixels (built facing north): the crystal held in the clamp, and where it meets the grinding wheel. */
    public static final Vec3 CRYSTAL = new Vec3(5.5, 18.2, 2.4);
    public static final Vec3 CONTACT = new Vec3(5.5, 18, 3.5);
    public static final int OUTPUT = 2;
    public static final int SIZE = 3;
    /** Dust a shattered crystal leaves, by grade. */
    public static final int FIRST_DUST = 2;
    public static final int FINISH_DUST = 5;
    /** Gemstone dust a finished gemstone leaves on the bench. */
    public static final int GEMSTONE_CUTTINGS = 2;
    private static final int[] TOP = {INPUT};
    private static final int[] SIDES = {DISC};
    private static final int[] BOTTOM = {OUTPUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private long seed;
    private int[] ground = new int[0];
    private int cracks;
    private float heat;
    private long lastStroke;
    /** Last result for the screen: 1 = cut, -1 = shattered, 0 = none yet (synced). */
    private int outcome;
    private long outcomeAt = -1000;
    /** The crystal the cut in progress belongs to (one of it): another one in the clamp means a fresh cut. */
    private ItemStack cutCrystal = ItemStack.EMPTY;

    public LapidaryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAPIDARY.get(), pos, state);
    }

    /** The grade the crystal in the clamp is cut to with the disc in place, or null when they do not go together. */
    public static Cleavage.@Nullable Grade grade(ItemStack crystal, ItemStack disc) {
        boolean master = disc.is(ModItems.MASTER_CUTTING_DISC.get());
        if (crystal.is(ModItems.UNSTABLE_HEXTECH.get()) && (master || disc.is(ModItems.CUTTING_DISC.get()))) {
            return Cleavage.Grade.FIRST;
        }
        if (crystal.is(Items.DIAMOND) && master) {
            // Only the diamond-edged master disc bites a diamond: the plain cutting disc slides off it.
            return Cleavage.Grade.ENGRAVE;
        }
        return crystal.is(ModItems.HEXTECH_CRYSTAL.get()) && master ? Cleavage.Grade.FINISH : null;
    }

    public Cleavage.@Nullable Grade grade() {
        return grade(items.get(INPUT), items.get(DISC));
    }

    /** A master disc grinds a first cut twice as fast. */
    public float speed() {
        return grade() == Cleavage.Grade.FIRST && items.get(DISC).is(ModItems.MASTER_CUTTING_DISC.get()) ? 2 : 1;
    }

    /** The cut in progress (a copy), or null when nothing can be cut. */
    public Cleavage.@Nullable Cut cut() {
        Cleavage.Grade grade = grade();
        return grade == null ? null : new Cleavage.Cut(grade, seed, ground, cracks, heat);
    }

    public long seed() {
        return seed;
    }

    public int outcome() {
        return outcome;
    }

    public long outcomeAt() {
        return outcomeAt;
    }

    /** Why a stroke cannot be made now, or null. */
    public @Nullable String blocked() {
        if (items.get(INPUT).isEmpty()) {
            return "no_crystal";
        }
        if (items.get(DISC).isEmpty()) {
            return "no_disc";
        }
        if (grade() == null) {
            return "wrong_disc";
        }
        return items.get(OUTPUT).isEmpty() ? null : "output_full";
    }

    /** A new crystal in the clamp: new planes, a clean cut. */
    private void freshCut() {
        cutCrystal = items.get(INPUT).copyWithCount(1);
        seed = level == null ? 0 : level.getRandom().nextLong();
        Cleavage.Grade grade = grade();
        ground = new int[grade == null ? 0 : grade.facets];
        cracks = 0;
        heat = 0;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        ItemStack before = items.get(slot);
        boolean newCrystal = slot == INPUT && !ItemStack.isSameItemSameComponents(before, stack);
        super.setItem(slot, stack);
        if (newCrystal || slot == DISC && ground.length != (grade() == null ? 0 : grade().facets)) {
            freshCut();
        }
        setChanged();
    }

    /** Plays a stroke sent by the worker: {@code ticks} of grinding at {@code angle}, ending now. */
    public void stroke(ServerPlayer player, float angle, int ticks) {
        Cleavage.Cut cut = cut();
        if (cut == null || blocked() != null || !(level instanceof ServerLevel server)) {
            return;
        }
        ticks = Math.clamp(ticks, 1, 200);
        long now = server.getGameTime();
        int rest = lastStroke == 0 ? 1000 : (int) Math.max(0, now - ticks - lastStroke);
        int facetsBefore = cut.facetsCut();
        int cracksBefore = cut.cracks;
        cut.stroke(rest, angle, ticks, speed());
        lastStroke = now;
        ground = cut.groundCopy();
        cracks = cut.cracks;
        heat = cut.heat;
        Vec3 at = Vec3.atCenterOf(worldPosition).add(0, 0.5, 0);
        // The wheel throws grit of the crystal and sparks where it bites; hotter strokes spark more and redder.
        Vec3 disc = modelPoint(CONTACT);
        int grit = Math.min(10, 2 + ticks / 4);
        server.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM,
                items.get(INPUT).getItem()), disc.x, disc.y, disc.z, grit, 0.12, 0.02, 0.12, 0.08);
        int hot = cut.heat > 0.6F ? ModParticles.FIRE : ModParticles.HEXTECH;
        server.sendParticles(ModParticles.spark(hot), disc.x, disc.y + 0.05, disc.z, Math.min(8, 1 + ticks / 6), 0.1, 0.02, 0.1, 0.25);
        if (cut.cracks > cracksBefore) {
            server.playSound(null, at.x, at.y, at.z, ModSounds.ORB_CRACK.get(), SoundSource.BLOCKS, 0.6F, 1.3F);
        }
        if (cut.facetsCut() > facetsBefore) {
            server.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 0.8F, 1.5F);
            // A new facet catches the light: a glint off the crystal in the clamp.
            Vec3 crystal = modelPoint(CRYSTAL);
            server.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), crystal.x, crystal.y, crystal.z, 4, 0.08, 0.06, 0.08, 0.01);
        }
        if (cut.over()) {
            finish(server, cut);
        }
        setChanged();
    }

    /** The cut is over: the next grade of crystal, or dust when it shattered; the disc wears. */
    private void finish(ServerLevel level, Cleavage.Cut cut) {
        boolean success = cut.finished() && !cut.shattered();
        ItemStack made = success ? new ItemStack(switch (cut.grade) {
            case FIRST -> ModItems.HEXTECH_CRYSTAL.get();
            case FINISH -> ModItems.HEXTECH_GEMSTONE.get();
            case ENGRAVE -> ModItems.ENGRAVED_DIAMOND.get();
        }) : cut.grade == Cleavage.Grade.ENGRAVE
                // A diamond only takes the groove badly: it comes back whole, the disc pays.
                ? new ItemStack(Items.DIAMOND)
                : new ItemStack(ModItems.HEXTECH_DUST.get(), cut.grade == Cleavage.Grade.FIRST ? FIRST_DUST : FINISH_DUST);
        items.get(INPUT).shrink(1);
        items.set(OUTPUT, made);
        if (success && cut.grade == Cleavage.Grade.FINISH) {
            // The finish leaves bright cuttings on the bench: the gold of the Radiant Alloy wants them.
            net.minecraft.world.level.block.Block.popResource(level, worldPosition.above(), new ItemStack(ModItems.GEMSTONE_DUST.get(), GEMSTONE_CUTTINGS));
        }
        ItemStack disc = items.get(DISC);
        disc.hurtAndBreak(1 + cut.cracks, level, null, item -> { });
        outcome = success ? 1 : -1;
        outcomeAt = level.getGameTime();
        Vec3 at = Vec3.atCenterOf(worldPosition).add(0, 0.5, 0);
        if (success) {
            level.playSound(null, at.x, at.y, at.z, ModSounds.CRYSTAL_RESONATE.get(), SoundSource.BLOCKS, 0.9F, 1.15F);
            level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y, at.z, 12, 0.2, 0.2, 0.2, 0.05);
            level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), at.x, at.y, at.z, 0, 0.8, 0, 0, 1);
        } else {
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,
                    net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER.defaultBlockState()), at.x, at.y, at.z, 16, 0.15, 0.1, 0.15, 0.15);
            level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x, at.y, at.z, 14, 0.2, 0.1, 0.2, 0.4);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        freshCut();
    }

    // ---- Container ----------------------------------------------------------------------------------------------------

    @Override
    public void setChanged() {
        super.setChanged();
        Cleavage.Grade grade = grade();
        if (level != null && !level.isClientSide()
                && (!ItemStack.isSameItemSameComponents(cutCrystal, items.get(INPUT)) || ground.length != (grade == null ? 0 : grade.facets))) {
            // Hoppers and pipes put crystals and discs in without the plain setItem: the cut follows here.
            freshCut();
        }
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hextale.hex_lapidary");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new LapidaryMenu(containerId, inventory, this, worldPosition);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT -> stack.is(ModItems.UNSTABLE_HEXTECH.get()) || stack.is(ModItems.HEXTECH_CRYSTAL.get()) || stack.is(Items.DIAMOND);
            case DISC -> stack.is(ModItems.CUTTING_DISC.get()) || stack.is(ModItems.MASTER_CUTTING_DISC.get());
            default -> false;
        };
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return accepts(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == OUTPUT;
    }

    @Override
    public void startOpen(net.minecraft.world.entity.ContainerUser player) {
        super.startOpen(player);
        LapidaryBlock.setWorking(level, worldPosition, true);
    }

    @Override
    public void stopOpen(net.minecraft.world.entity.ContainerUser player) {
        super.stopOpen(player);
        LapidaryBlock.setWorking(level, worldPosition, false);
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        seed = input.getLongOr("seed", 0);
        ground = input.read("ground", Codec.INT.listOf()).orElse(List.of()).stream().mapToInt(Integer::intValue).toArray();
        cracks = input.getIntOr("cracks", 0);
        heat = input.getFloatOr("heat", 0);
        lastStroke = input.getLongOr("last_stroke", 0);
        outcome = input.getIntOr("outcome", 0);
        outcomeAt = input.getLongOr("outcome_at", -1000);
        cutCrystal = items.get(INPUT).copyWithCount(1);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putLong("seed", seed);
        output.store("ground", Codec.INT.listOf(), Arrays.stream(ground).boxed().toList());
        output.putInt("cracks", cracks);
        output.putFloat("heat", heat);
        output.putLong("last_stroke", lastStroke);
        output.putInt("outcome", outcome);
        output.putLong("outcome_at", outcomeAt);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** A point of the model (pixels, built facing north) in the world, turned with the bench like its blockstate turns it. */
    private Vec3 modelPoint(Vec3 px) {
        double x = px.x / 16 - 0.5;
        double z = px.z / 16 - 0.5;
        Direction facing = getBlockState().getValue(LapidaryBlock.FACING);
        double rx = switch (facing) {
            case EAST -> -z;
            case SOUTH -> -x;
            case WEST -> z;
            default -> x;
        };
        double rz = switch (facing) {
            case EAST -> x;
            case SOUTH -> -z;
            case WEST -> -x;
            default -> z;
        };
        return Vec3.atLowerCornerOf(worldPosition).add(0.5 + rx, px.y / 16, 0.5 + rz);
    }
}
