package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import com.pockyl.hextale.item.HextechInkItem;
import com.pockyl.hextale.item.ResearchersDiaryItem;
import com.pockyl.hextale.item.RuneBrushItem;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.RuneRecognizer;
import com.pockyl.hextale.rune.RuneStrokes;
import com.pockyl.hextale.rune.SimpleRune;

/**
 * The Research Table's contents and the server side of the drawing mini-game: {@link #fuse} re-recognises the
 * strokes a client sends, checks and consumes the materials, teaches the rune and writes it onto a scroll. Contents and
 * a "someone is drawing" flag are synced to clients for the renderer; fusion, crack and fizzle reach every nearby
 * client as block events so the orb can flare for all of them.
 */
public class ResearchTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int DIARY = 0;
    public static final int PAPER = 1;
    public static final int HEXTECH = 2;
    public static final int INK = 3;
    public static final int BRUSH = 4;
    public static final int OUTPUT = 5;
    public static final int SLOTS = 6;

    public static final int EVENT_FUSED = 1;
    public static final int EVENT_CRACKED = 2;
    public static final int EVENT_FIZZLE = 3;

    private static final int[] SIDE_SLOTS = {PAPER, HEXTECH};
    private static final int[] BOTTOM_SLOTS = {OUTPUT};
    /** How long the orb keeps stirring after the last stroke. */
    private static final int DRAWING_TICKS = 30;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private boolean drawing;
    private long drawingUntil;
    private long lastStrokeSound;

    /** Client side: game time of the last fusion flare and its colour, of the last crack and of the last fizzle. */
    public long flareTime = Long.MIN_VALUE / 2;
    public int flareColor = 0x5CC8F2;
    /** Client side: index of the last fused rune (its hieroglyph rises over the orb), or -1. */
    public int flareRune = -1;
    public long crackTime = Long.MIN_VALUE / 2;
    public long fizzleTime = Long.MIN_VALUE / 2;
    /** Client side: ring spin and orb brightness, advanced by the renderer. */
    public final TableAnimation animation = new TableAnimation();

    public ResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESEARCH_TABLE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResearchTableBlockEntity table) {
        if (table.drawing && level.getGameTime() > table.drawingUntil) {
            table.drawing = false;
            table.sync();
        }
    }

    /**
     * Sparks crackle around the orb; while someone draws, glyphs lift off the parchment and stream into it, like
     * letters into an enchanting table's book.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, ResearchTableBlockEntity table) {
        if (table.items.get(HEXTECH).isEmpty()) {
            return;
        }
        RandomSource random = level.random;
        Vec3 orb = table.orbPosition();
        if (random.nextInt(table.drawing ? 4 : 50) == 0) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = table.drawing ? 0.05 : 0.015;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, orb.x, orb.y, orb.z,
                    Math.cos(angle) * speed, (random.nextDouble() - 0.3) * speed, Math.sin(angle) * speed);
        }
        if (table.drawing && !table.items.get(PAPER).isEmpty() && random.nextInt(2) == 0) {
            Vec3 from = TableGeometry.toWorld(pos, table.facing(), TableGeometry.PARCHMENT.add(
                    (random.nextDouble() - 0.5) * 9, 0.4, (random.nextDouble() - 0.5) * 6));
            level.addParticle(ParticleTypes.ENCHANT, orb.x, orb.y, orb.z, from.x - orb.x, from.y - orb.y, from.z - orb.z);
        }
    }

    public boolean isDrawing() {
        return drawing;
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(ResearchTableBlock.FACING) ? state.getValue(ResearchTableBlock.FACING) : Direction.NORTH;
    }

    public Vec3 orbPosition() {
        return TableGeometry.toWorld(worldPosition, facing(), TableGeometry.ORB);
    }

    /** A stroke was drawn at this table: the orb stirs for everyone and others hear the brush. */
    public void markDrawing(ServerPlayer player) {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (now - lastStrokeSound > 8) {
            lastStrokeSound = now;
            level.playSound(player, worldPosition, ModSounds.BRUSH_STROKE.get(), SoundSource.BLOCKS, 0.4F,
                    0.9F + level.random.nextFloat() * 0.2F);
        }
        drawingUntil = now + DRAWING_TICKS;
        if (!drawing) {
            drawing = true;
            sync();
        }
    }

    /** The orb did not recognise a drawing: a flicker and a puff of smoke for everyone around. */
    public void fizzle(ServerPlayer player) {
        if (level instanceof ServerLevel serverLevel) {
            Vec3 orb = orbPosition();
            serverLevel.sendParticles(ParticleTypes.SMOKE, orb.x, orb.y, orb.z, 4, 0.08, 0.05, 0.08, 0.01);
            level.playSound(player, worldPosition, ModSounds.RUNE_FAILED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_FIZZLE, 0);
        }
    }

    /**
     * Fuses the radicals drawn in the action and target circles. Everything is checked here again: the materials on
     * the table and the drawings themselves, recognised from the raw strokes.
     */
    public FusionResult fuse(ServerPlayer player, RuneStrokes actionInk, RuneStrokes targetInk) {
        Requirement missing = Requirement.firstMissing(this, player);
        if (missing != null) {
            return FusionResult.missing(missing);
        }
        SimpleRune action = actionInk.isValid() ? RuneRecognizer.recognise(actionInk.strokes()) : null;
        SimpleRune target = targetInk.isValid() ? RuneRecognizer.recognise(targetInk.strokes()) : null;
        if (action == null || target == null) {
            return FusionResult.unrecognised(action, target);
        }
        Research.recordDrawn(player, action);
        Research.recordDrawn(player, target);
        ResearchersDiaryItem.sign(items.get(DIARY), player);

        if (action == target) {
            items.get(HEXTECH).shrink(1);
            setChanged();
            Research.recordCrack(player);
            crackEffects(player);
            return FusionResult.cracked(action);
        }

        ComplexRune rune = ComplexRunes.of(action, target);
        items.get(PAPER).shrink(1);
        items.get(HEXTECH).shrink(1);
        items.set(INK, HextechInkItem.useDose(items.get(INK)));
        ItemStack brush = RuneBrushItem.wear(items.get(BRUSH));
        if (brush.isEmpty()) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        items.set(BRUSH, brush);
        deliver(player, RuneScrollItem.of(rune, ModItems.RUNE_SCROLL.get()));
        setChanged();
        boolean fresh = Research.learn(player, rune);
        fusionEffects(player, rune);
        return FusionResult.fused(rune, fresh);
    }

    /** The scroll goes to the output slot, stacking on equal scrolls; when that is taken, to the researcher. */
    private void deliver(ServerPlayer player, ItemStack scroll) {
        ItemStack output = items.get(OUTPUT);
        if (output.isEmpty()) {
            items.set(OUTPUT, scroll);
        } else if (ItemStack.isSameItemSameComponents(output, scroll) && output.getCount() < output.getMaxStackSize()) {
            output.grow(1);
        } else if (!player.getInventory().add(scroll)) {
            player.drop(scroll, false);
        }
    }

    private void fusionEffects(ServerPlayer player, ComplexRune rune) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 orb = orbPosition();
        int color = rune.color();
        Vector3f rgb = new Vector3f((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
        serverLevel.sendParticles(new DustParticleOptions(rgb, 1.2F), orb.x, orb.y, orb.z, 28, 0.25, 0.2, 0.25, 0.02);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, orb.x, orb.y, orb.z, 14, 0.15, 0.15, 0.15, 0.25);
        serverLevel.sendParticles(ParticleTypes.END_ROD, orb.x, orb.y, orb.z, 6, 0.1, 0.1, 0.1, 0.05);
        level.playSound(player, worldPosition, ModSounds.RUNE_FUSED.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_FUSED, rune.index());
    }

    private void crackEffects(ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 orb = orbPosition();
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, orb.x, orb.y, orb.z, 8, 0.1, 0.1, 0.1, 0.02);
        serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(ModItems.UNSTABLE_HEXTECH.get())),
                orb.x, orb.y, orb.z, 14, 0.08, 0.08, 0.08, 0.12);
        level.playSound(player, worldPosition, ModSounds.ORB_CRACK.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_CRACKED, 0);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (level != null && level.isClientSide) {
            long now = level.getGameTime();
            switch (id) {
                case EVENT_FUSED -> {
                    flareTime = now;
                    flareRune = param >= 0 && param < ComplexRunes.COUNT ? param : -1;
                    flareColor = flareRune >= 0 ? ComplexRunes.all().get(param).color() : 0x5CC8F2;
                }
                case EVENT_CRACKED -> crackTime = now;
                case EVENT_FIZZLE -> fizzleTime = now;
                default -> {
                    return super.triggerEvent(id, param);
                }
            }
        }
        return true;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        updateLight();
        sync();
    }

    /** Keeps the desk's faint glow in step with the orb lying on it. */
    private void updateLight() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        boolean lit = !items.get(HEXTECH).isEmpty();
        if (state.hasProperty(ResearchTableBlock.LIT) && state.getValue(ResearchTableBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(ResearchTableBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hextale.research_table");
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
        return new ResearchTableMenu(containerId, inventory, this, worldPosition);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(slot, stack);
    }

    /** What each slot holds; the output slot is filled by fusions only. */
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case DIARY -> stack.is(ModItems.RESEARCHERS_DIARY.get());
            case PAPER -> stack.is(Items.PAPER);
            case HEXTECH -> stack.is(ModItems.UNSTABLE_HEXTECH.get());
            case INK -> stack.is(ModItems.HEXTECH_INK.get());
            case BRUSH -> stack.is(ModItems.RUNE_BRUSH.get());
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM_SLOTS : SIDE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == OUTPUT;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
        drawing = tag.getBoolean("drawing");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.putBoolean("drawing", drawing);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
