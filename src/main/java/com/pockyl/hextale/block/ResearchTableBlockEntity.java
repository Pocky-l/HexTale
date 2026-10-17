package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.item.ResearchersDiaryItem;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.ResearchMessagePayload;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.research.HexGrid;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.ItemStudy;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.research.ResearchSheet;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.research.RuneBoard;
import com.pockyl.hextale.research.RuneBoards;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.Essence;

import java.util.Arrays;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The Research Table's contents, its ink and the server side of both mini-games - learning a rune by drawing an ink
 * thread on its parchment and studying an item by its echoes. Every action a client asks for is checked here against the sheet, the table and the
 * researcher's knowledge and inventory before it happens. Contents, both games and a "someone is writing" flag are
 * synced to clients; finishing a rune reaches every nearby client as a block event so the orb can flare for all.
 */
public class ResearchTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int DIARY = 0;
    public static final int HEXTECH = 1;
    public static final int OUTPUT = 2;
    /** The item whose essences are being studied. */
    public static final int STUDY = 3;
    /** Paper: every finished research is written on one. */
    public static final int PAPER = 4;
    public static final int SLOTS = 5;

    public static final int EVENT_FUSED = 1;
    public static final int EVENT_FIZZLE = 3;
    /** Health a burst orb takes from the researcher (two hearts). */
    private static final float BURST_DAMAGE = 4.0F;

    private static final int[] SIDE_SLOTS = {HEXTECH, STUDY, PAPER};
    private static final int[] BOTTOM_SLOTS = {OUTPUT};
    /** How long the orb keeps stirring after the last symbol. */
    private static final int DRAWING_TICKS = 30;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final ResearchSheet sheet = new ResearchSheet();
    private final ItemStudy study = new ItemStudy();
    /** Which game lies on the parchment: the item study (true) or the rune (false). */
    private boolean studyingItem;
    private boolean drawing;
    private long drawingUntil;

    /** Client side: game time of the last finished rune and its colour, and of the last lost sheet. */
    public long flareTime = Long.MIN_VALUE / 2;
    public int flareColor = 0x5CC8F2;
    /** Client side: index of the last finished rune (its hieroglyph rises over the orb), or -1. */
    public int flareRune = -1;
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
     * Sparks crackle around the orb; while someone writes, glyphs lift off the parchment and stream into it, like
     * letters into an enchanting table's book.
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, ResearchTableBlockEntity table) {
        if (table.items.get(HEXTECH).isEmpty()) {
            return;
        }
        RandomSource random = level.getRandom();
        Vec3 orb = table.orbPosition();
        if (random.nextInt(table.drawing ? 4 : 50) == 0) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = table.drawing ? 0.05 : 0.015;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, orb.x, orb.y, orb.z,
                    Math.cos(angle) * speed, (random.nextDouble() - 0.3) * speed, Math.sin(angle) * speed);
        }
        if (table.drawing && random.nextInt(2) == 0) {
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

    /** The thread grew at this table: the orb stirs for everyone and others hear the brush. */
    private void markDrawing(ServerPlayer player) {
        if (level == null) {
            return;
        }
        level.playSound(null, worldPosition, ModSounds.BRUSH_STROKE.get(), SoundSource.BLOCKS, 0.5F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        drawingUntil = level.getGameTime() + DRAWING_TICKS;
        drawing = true;
    }

    public ResearchSheet sheet() {
        return sheet;
    }

    public ItemStudy study() {
        return study;
    }

    /** Whether the parchment shows the item study rather than a rune. */
    public boolean isStudyingItem() {
        return studyingItem;
    }

    /** The board of the rune being learned, or null (none chosen). */
    @Nullable
    public RuneBoard board() {
        ComplexRune rune = sheet.rune();
        return rune == null ? null : RuneBoards.get(rune);
    }

    /** In creative mode every item counts as studied, as if the researcher carried them all. */
    private static boolean studied(ServerPlayer player, Item item) {
        return player.getAbilities().instabuild || Research.of(player).hasStudied(BuiltInRegistries.ITEM.getKey(item));
    }

    // ---- Ink ------------------------------------------------------------------------------------------------------

    /** The ink around the desk: Essence Tanks of concentrate (the table keeps none itself). */
    public InkSupply inkSupply() {
        return InkSupply.around(level, worldPosition, facing());
    }

    // ---- Learning a rune ------------------------------------------------------------------------------------------

    /** Lays the book's rune on the table: a new rune, a fresh thread. */
    public void select(ServerPlayer player, ComplexRune rune) {
        Requirement missing = Requirement.diary(this, player);
        if (missing != null) {
            message(player, Component.translatable(missing.translationKey()), ResearchMessagePayload.Tone.BAD);
            return;
        }
        if (!ResearchBook.open(Research.of(player), rune)) {
            message(player, Component.translatable("research.hextale.closed", name(rune)), ResearchMessagePayload.Tone.BAD);
            return;
        }
        ResearchersDiaryItem.sign(items.get(DIARY), player);
        sheet.select(rune);
        studyingItem = false;
        setChanged();
    }

    /**
     * The researcher's new drawing (steps as {@code from, to} pairs). It is checked against the rules, the table and the ink
     * in the tanks around; a drawing that reaches every point binds the rune at once, one that steps on a curse bursts
     * the orb.
     */
    public void thread(ServerPlayer player, int[] cells) {
        RuneBoard board = board();
        if (board == null || studyingItem || Requirement.diary(this, player) != null) {
            return;
        }
        if (cells.length == 0) {
            sheet.setTree(new int[0]);
            setChanged();
            return;
        }
        ResearchSheet.Trace trace = ResearchSheet.trace(board, cells);
        boolean creative = player.getAbilities().instabuild;
        boolean fresh = sheet.isDone() || !sheet.paid();
        if (Requirement.firstMissing(this, player, !fresh) != null || trace == null || !creative && !trace.affordable(inkSupply().sections())) {
            sheet.touch();
            setChanged();
            return;
        }
        if (trace.burst() >= 0) {
            cells = Arrays.copyOf(cells, trace.sections() * 2);
        }
        boolean longer = cells.length > sheet.tree().length;
        sheet.setTree(cells);
        if (fresh) {
            // The attempt starts: it is written on a paper from the table.
            if (!creative) {
                items.get(PAPER).shrink(1);
            }
            sheet.pay();
            level.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        ResearchersDiaryItem.sign(items.get(DIARY), player);
        Research.of(player).see(board.rune(), ResearchSheet.cells(cells));
        if (trace.burst() >= 0) {
            burst(player, board, cells[cells.length - 1], trace);
        } else if (trace.complete(board)) {
            complete(player, board, trace);
        } else if (longer) {
            markDrawing(player);
        }
        setChanged();
    }

    /**
     * Every point passed: the orb binds the rune, the ink is spent and the rune's scroll comes out. The researcher learns
     * the rune only by studying the scroll.
     */
    private void complete(ServerPlayer player, RuneBoard board, ResearchSheet.Trace trace) {
        ComplexRune rune = board.rune();
        int stars = board.stars(sheet.length());
        if (!player.getAbilities().instabuild) {
            inkSupply().spendSections(trace.ink());
        }
        items.get(HEXTECH).shrink(1);
        sheet.finish();
        deliver(player, RuneScrollItem.of(rune, stars, ModItems.RUNE_SCROLL.get()));
        Research.sync(player);
        message(player, Component.translatable("research.hextale.complete", name(rune), "★".repeat(stars) + "☆".repeat(3 - stars)),
                ResearchMessagePayload.Tone.WIN);
        completeEffects(rune);
    }

    /**
     * The ink stepped on a cursed dark rune: the orb bursts. The sheet burns with its paper, the ink drawn so far is spent,
     * the researcher is hurt by the blast and remembers where the curse was.
     */
    private void burst(ServerPlayer player, RuneBoard board, int cell, ResearchSheet.Trace trace) {
        boolean creative = player.getAbilities().instabuild;
        if (!creative) {
            inkSupply().spendSections(trace.ink());
            items.get(HEXTECH).shrink(1);
            player.hurtServer((ServerLevel) level, level.damageSources().magic(), BURST_DAMAGE);
        }
        Research.of(player).curse(board.rune(), cell);
        sheet.burn();
        Research.sync(player);
        message(player, Component.translatable("research.hextale.burst", name(board.rune())), ResearchMessagePayload.Tone.BAD);
        if (level instanceof ServerLevel serverLevel) {
            Vec3 orb = orbPosition();
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, orb.x, orb.y, orb.z, 1, 0, 0, 0, 0);
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, orb.x, orb.y, orb.z, 24, 0.25, 0.15, 0.25, 0.08);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, orb.x, orb.y, orb.z, 20, 0.2, 0.2, 0.2, 0.4);
            level.playSound(null, worldPosition, ModSounds.ORB_CRACK.get(), SoundSource.BLOCKS, 1.0F, 0.7F);
            level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.5F, 1.4F);
            level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_FIZZLE, 0);
        }
    }

    // ---- Studying an item -----------------------------------------------------------------------------------------

    /**
     * The study slot changed in the researcher's hands: an unstudied item starts its study at once, an emptied slot
     * (before the first strike) takes the study off the parchment.
     */
    public void studySlotChanged(ServerPlayer player) {
        ItemStack stack = items.get(STUDY);
        Item current = study.item();
        ItemEssences running = current == null ? null : ResearchItems.of(current);
        if (studyingItem && running != null && study.started() && study.isActive(running)) {
            return;
        }
        ItemEssences elements = ResearchItems.of(stack);
        if (elements != null && !studied(player, stack.getItem()) && Requirement.diary(this, player) == null) {
            if (!studyingItem || current != stack.getItem() || study.started()) {
                ResearchersDiaryItem.sign(items.get(DIARY), player);
                study.begin(stack.getItem(), elements);
                studyingItem = true;
                setChanged();
            }
        } else if (studyingItem && (stack.isEmpty() || !study.started() || current != stack.getItem())) {
            studyingItem = false;
            study.clear();
            setChanged();
        }
    }

    /** A click on a finished study: try a lost item again, or put the study away. */
    public void closeStudy(ServerPlayer player) {
        Item current = study.item();
        ItemEssences running = current == null ? null : ResearchItems.of(current);
        if (!studyingItem || running != null && study.isActive(running)) {
            return;
        }
        study.clear();
        studyingItem = false;
        studySlotChanged(player);
        setChanged();
    }

    /** Strikes a section of the item's parchment. The first strike takes one of the item and a paper. */
    public void strike(ServerPlayer player, int cell) {
        Item item = study.item();
        ItemEssences elements = item == null ? null : ResearchItems.of(item);
        if (!studyingItem || elements == null || Requirement.diary(this, player) != null || !study.canStrike(cell, elements)) {
            return;
        }
        if (!study.started()) {
            if (!items.get(STUDY).is(item) || Requirement.paper(this, player) != null) {
                return;
            }
            if (!player.getAbilities().instabuild) {
                items.get(STUDY).shrink(1);
                items.get(PAPER).shrink(1);
            }
        }
        ItemStudy.Echo echo = study.strike(cell, elements);
        if (echo == null) {
            return;
        }
        if (echo.radius() == 0) {
            Essence found = ItemStudy.essenceOn(elements, ItemStudy.stringOf(cell));
            if (found != null) {
                pointEffects(found);
            }
        } else {
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F,
                    Math.max(0.5F, 1.6F - echo.radius() * 0.08F));
        }
        if (study.won(elements)) {
            Research.study(player, item);
            message(player, Component.translatable("research.hextale.study.complete", ResearchText.item(item)), ResearchMessagePayload.Tone.WIN);
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
            if (level instanceof ServerLevel serverLevel) {
                Vec3 orb = orbPosition();
                serverLevel.sendParticles(ParticleTypes.END_ROD, orb.x, orb.y, orb.z, 10, 0.15, 0.1, 0.15, 0.03);
            }
        } else if (study.lost(elements)) {
            message(player, Component.translatable("research.hextale.study.lost", ResearchText.item(item)), ResearchMessagePayload.Tone.BAD);
            fizzle();
        }
        setChanged();
    }

    private void fizzle() {
        if (level instanceof ServerLevel serverLevel) {
            Vec3 orb = orbPosition();
            serverLevel.sendParticles(ParticleTypes.SMOKE, orb.x, orb.y, orb.z, 6, 0.08, 0.05, 0.08, 0.01);
            level.playSound(null, worldPosition, ModSounds.RUNE_FAILED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_FIZZLE, 0);
        }
    }

    private static Component name(ComplexRune rune) {
        return Component.translatable(rune.translationKey()).withColor(rune.color());
    }

    private static void message(ServerPlayer player, Component text, ResearchMessagePayload.Tone tone) {
        ModNetwork.sendToPlayer(player, new ResearchMessagePayload(text, tone));
    }

    /** The scroll goes to the output slot, stacking on equal scrolls; when that is taken, to the researcher. */
    private void deliver(ServerPlayer player, ItemStack scroll) {
        ItemStack output = items.get(OUTPUT);
        if (output.isEmpty()) {
            items.set(OUTPUT, scroll);
        } else if (ItemStack.isSameItemSameComponents(output, scroll) && output.getCount() < output.getMaxStackSize()) {
            output.grow(1);
        } else if (!player.getInventory().add(scroll)) {
            player.drop(scroll, false, Prediction.SERVER_ONLY);
        }
    }

    private void pointEffects(Essence element) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 orb = orbPosition();
        serverLevel.sendParticles(new DustParticleOptions(element.color(), 1.0F), orb.x, orb.y, orb.z, 10,
                0.2, 0.15, 0.2, 0.01);
        level.playSound(null, worldPosition, ModSounds.RUNE_RECOGNISED.get(), SoundSource.BLOCKS, 0.8F,
                0.8F + element.ordinal() * 0.06F);
    }

    private void completeEffects(ComplexRune rune) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 orb = orbPosition();
        serverLevel.sendParticles(new DustParticleOptions(rune.color(), 1.2F), orb.x, orb.y, orb.z, 28, 0.25, 0.2, 0.25, 0.02);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, orb.x, orb.y, orb.z, 14, 0.15, 0.15, 0.15, 0.25);
        serverLevel.sendParticles(ParticleTypes.END_ROD, orb.x, orb.y, orb.z, 6, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, worldPosition, ModSounds.RUNE_FUSED.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_FUSED, rune.index());
    }


    @Override
    public boolean triggerEvent(int id, int param) {
        if (level != null && level.isClientSide()) {
            long now = level.getGameTime();
            switch (id) {
                case EVENT_FUSED -> {
                    flareTime = now;
                    flareRune = param >= 0 && param < ComplexRunes.COUNT ? param : -1;
                    flareColor = flareRune >= 0 ? ComplexRunes.all().get(param).color() : 0x5CC8F2;
                }
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
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        boolean lit = !items.get(HEXTECH).isEmpty();
        if (state.hasProperty(ResearchTableBlock.LIT) && state.getValue(ResearchTableBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(ResearchTableBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
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

    /** What each slot holds; the output slot is filled by finished runes only. */
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case DIARY -> stack.is(ModItems.RESEARCHERS_DIARY.get());
            case HEXTECH -> stack.is(ModItems.UNSTABLE_HEXTECH.get());
            case STUDY -> ResearchItems.of(stack) != null && !stack.is(ModItems.ESSENCE_FLASK.get());
            case PAPER -> stack.is(Items.PAPER);
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        drawing = input.getBooleanOr("drawing", false);
        sheet.load(input.childOrEmpty("sheet"));
        study.load(input.childOrEmpty("study"));
        studyingItem = input.getBooleanOr("studying_item", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        sheet.save(output.child("sheet"));
        study.save(output.child("study"));
        output.putBoolean("studying_item", studyingItem);
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
