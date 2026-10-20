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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
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

import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.item.NotesDrawing;
import com.pockyl.hextale.item.ResearchersDiaryItem;
import com.pockyl.hextale.item.ResearchNotesItem;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.menu.ResearchTableMenu;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.ResearchMessagePayload;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.Loupe;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.research.ResearchSheet;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.research.HexGrid;
import com.pockyl.hextale.research.ResearchBoard;
import com.pockyl.hextale.research.ResearchBoards;
import com.pockyl.hextale.rune.Essence;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * The Research Table's contents, its ink and the server side of both mini-games - researching a device or tool of the
 * mod ({@link ResearchTopic}) by drawing an ink thread on its parchment, and studying items under the loupe arm. Every
 * action a client asks for is checked here against the sheet, the table and the researcher's knowledge and inventory
 * before it happens. Contents, both games and a "someone is writing" flag are synced to clients; a finished research
 * reaches every nearby client as a block event so the orb can flare for all.
 */
public class ResearchTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int DIARY = 0;
    public static final int HEXTECH = 1;
    public static final int OUTPUT = 2;
    /** Paper: every finished research is written on one. */
    public static final int PAPER = 3;
    public static final int SLOTS = 4;
    /** Tables saved before the study slot left keep paper at 4 and an item to study at 3. */
    private static final int OLD_SLOTS = 5;
    private static final int LAYOUT = 2;

    public static final int EVENT_FUSED = 1;
    public static final int EVENT_FIZZLE = 3;
    /** How long the ink streams from the jars into the orb, in ticks. */
    public static final int INK_STREAM_TICKS = 60;
    /** Health a burst orb takes from the researcher (two hearts). */
    private static final float BURST_DAMAGE = 4.0F;

    private static final int[] SIDE_SLOTS = {HEXTECH, PAPER};
    private static final int[] BOTTOM_SLOTS = {OUTPUT};
    /** How long the orb keeps stirring after the last symbol. */
    private static final int DRAWING_TICKS = 30;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final ResearchSheet sheet = new ResearchSheet();
    /** Who holds the loupe over which slot of their open table screen, since when (game time); not saved. */
    @Nullable
    private UUID loupeBy;
    private int loupeSlot = -1;
    private long loupeSince;
    /** Synced: someone looks through the loupe, so the arm swings over the desk; client: when that began. */
    private boolean looking;
    public long lookingSince = Long.MIN_VALUE / 2;
    /** Left over from an old table's study slot, handed back on the first tick. */
    private ItemStack orphan = ItemStack.EMPTY;
    private boolean drawing;
    private long drawingUntil;

    /** Client side: game time of the last finished research and its colour, and of the last lost sheet. */
    public long flareTime = Long.MIN_VALUE / 2;
    public int flareColor = 0x5CC8F2;
    /** Client side: index of the last researched topic (its item rises over the orb), or -1. */
    public int flareTopic = -1;
    public long fizzleTime = Long.MIN_VALUE / 2;
    /** The jars the last ink came from and when (game time); synced, the renderer streams them into the orb. */
    private List<JarReach.Draw> inkDraws = List.of();
    private long inkDrawnAt = -1000;
    /** Client side: ring spin and orb brightness, advanced by the renderer. */
    public final TableAnimation animation = new TableAnimation();

    public ResearchTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESEARCH_TABLE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResearchTableBlockEntity table) {
        if (!table.orphan.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, table.orphan);
            table.orphan = ItemStack.EMPTY;
        }
        if (table.looking && level.getGameTime() % 10 == 0 && !(table.loupeBy != null
                && level.getPlayerByUUID(table.loupeBy) instanceof ServerPlayer holder && ResearchTableMenu.openTable(holder, pos) == table)) {
            table.lookAway();
        }
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
        float age = table.sinceInkDrawn();
        for (JarReach.Draw draw : table.inkDraws()) {
            JarReach.shed(level, JarReach.mouth(draw.jar()), table.orbPosition(), draw.essence() == null ? 0x5CC8F2 : draw.essence().color(),
                    Math.min(1, inkHead(age)), Math.max(0, inkTail(age)));
        }
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

    public boolean isLooking() {
        return looking;
    }

    /** The board of the topic being researched, or null (none chosen). */
    @Nullable
    public ResearchBoard board() {
        ResearchTopic topic = sheet.topic();
        return topic == null ? null : ResearchBoards.get(topic, sheet.attempt());
    }

    private static boolean studied(ServerPlayer player, Item item) {
        return Research.of(player).hasStudied(BuiltInRegistries.ITEM.getKey(item));
    }

    // ---- Ink ------------------------------------------------------------------------------------------------------

    /** The ink around the desk: Essence Jars of concentrate (the table keeps none itself). */
    public InkSupply inkSupply() {
        return InkSupply.around(level, worldPosition, facing());
    }

    /** Spends the ink of {@code sections} (per essence); every jar it comes from streams into the orb. */
    private void drawInk(int[] sections) {
        Map<EssenceTankBlockEntity, Essence> drawn = inkSupply().spendSections(sections);
        if (drawn.isEmpty()) {
            return;
        }
        inkDraws = drawn.entrySet().stream().map(entry -> new JarReach.Draw(entry.getKey().getBlockPos(), entry.getValue())).toList();
        inkDrawnAt = level.getGameTime();
        level.playSound(null, worldPosition, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.6F, 1.1F);
        setChanged();
    }

    /** The jars whose ink still streams into the orb, or none. */
    public List<JarReach.Draw> inkDraws() {
        return level != null && level.getGameTime() - inkDrawnAt <= INK_STREAM_TICKS ? inkDraws : List.of();
    }

    /** Ticks since the last ink was drawn. */
    public long sinceInkDrawn() {
        return level == null ? Long.MAX_VALUE : level.getGameTime() - inkDrawnAt;
    }

    /** How far the ink's streams have grown out of their jars, {@code age} ticks after the draw. */
    public static float inkHead(float age) {
        return age / 12;
    }

    /** How far the ink's streams have run into the orb, {@code age} ticks after the draw. */
    public static float inkTail(float age) {
        return (age - 42) / 16;
    }

    // ---- Researching a topic ---------------------------------------------------------------------------------------

    /** Lays the book's topic on the table: a new topic, a fresh thread. */
    public void select(ServerPlayer player, ResearchTopic topic) {
        Requirement missing = Requirement.diary(this, player);
        if (missing != null) {
            message(player, Component.translatable(missing.translationKey()), ResearchMessagePayload.Tone.BAD);
            return;
        }
        if (topic.lab() || !Guide.server().canResearch(topic, Research.of(player))) {
            message(player, Component.translatable("research.hextale.closed", name(topic)), ResearchMessagePayload.Tone.BAD);
            return;
        }
        ResearchersDiaryItem.sign(items.get(DIARY), player);
        sheet.select(topic, Research.of(player).attempt(topic));
        setChanged();
    }

    /**
     * The researcher's new drawing (steps as {@code from, to} pairs). It is checked against the rules, the table and the ink
     * in the tanks around; a drawing that reaches every point finishes the research at once, one that steps on a curse bursts
     * the orb.
     */
    public void thread(ServerPlayer player, int[] cells) {
        ResearchBoard board = board();
        if (board == null || Requirement.diary(this, player) != null) {
            return;
        }
        int attempt = Research.of(player).attempt(board.topic());
        if (attempt != sheet.attempt()) {
            // Someone else's parchment of this topic lies on the desk: this researcher starts on their own.
            sheet.select(board.topic(), attempt);
            setChanged();
            return;
        }
        if (cells.length == 0) {
            sheet.setTree(new int[0]);
            setChanged();
            return;
        }
        ResearchSheet.Trace trace = ResearchSheet.trace(board, cells);
        boolean fresh = sheet.isDone() || !sheet.paid();
        if (Requirement.firstMissing(this, player, !fresh) != null || trace == null || !trace.affordable(inkSupply().sections())) {
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
            items.get(PAPER).shrink(1);
            sheet.pay();
            level.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        ResearchersDiaryItem.sign(items.get(DIARY), player);
        Research.of(player).see(board.topic(), ResearchSheet.cells(cells));
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
     * Every point passed: the orb binds the research, the ink is spent and the notes come out. The researcher may make
     * the topic's items only after reading the notes (and may hand them to someone else instead).
     */
    private void complete(ServerPlayer player, ResearchBoard board, ResearchSheet.Trace trace) {
        ResearchTopic topic = board.topic();
        drawInk(trace.ink());
        items.get(HEXTECH).shrink(1);
        sheet.finish();
        deliver(player, ResearchNotesItem.of(topic, sheet.attempt(), sheet.tree()));
        Research.sync(player);
        message(player, Component.translatable("research.hextale.complete", name(topic)),
                ResearchMessagePayload.Tone.WIN);
        completeEffects(topic);
    }

    /**
     * The ink stepped on a cursed section: the orb bursts. The sheet burns with its paper, the ink drawn so far is spent
     * and the researcher is hurt; the next attempt is drawn on a new parchment with another board.
     */
    private void burst(ServerPlayer player, ResearchBoard board, int cell, ResearchSheet.Trace trace) {
        drawInk(trace.ink());
        items.get(HEXTECH).shrink(1);
        player.hurtServer((ServerLevel) level, level.damageSources().magic(), BURST_DAMAGE);
        message(player, Component.translatable("research.hextale.burst", name(board.topic())), ResearchMessagePayload.Tone.BAD);
        if (level instanceof ServerLevel serverLevel) {
            Vec3 orb = orbPosition();
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, orb.x, orb.y, orb.z, 24, 0.25, 0.15, 0.25, 0.08);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, orb.x, orb.y, orb.z, 20, 0.2, 0.2, 0.2, 0.4);
            level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_FIZZLE, 0);
        }
        burnSheet(player, board, cell);
    }

    /**
     * The researcher gives up on the parchment and burns it: a fresh one with a new board takes its place. Its paper is
     * lost: the one the attempt was written on, or a paper from the table if nothing was drawn yet.
     */
    public void burn(ServerPlayer player) {
        ResearchBoard board = board();
        if (board == null || sheet.isDone() || Requirement.diary(this, player) != null) {
            return;
        }
        if (Research.of(player).attempt(board.topic()) != sheet.attempt()) {
            sheet.select(board.topic(), Research.of(player).attempt(board.topic()));
            setChanged();
            return;
        }
        if (!sheet.paid()) {
            Requirement missing = Requirement.paper(this, player);
            if (missing != null) {
                message(player, Component.translatable(missing.translationKey()), ResearchMessagePayload.Tone.BAD);
                return;
            }
            items.get(PAPER).shrink(1);
        }
        int[] tree = sheet.tree();
        burnSheet(player, board, tree.length == 0 ? HexGrid.CENTRE : tree[tree.length - 1]);
        message(player, Component.translatable("research.hextale.burnt", name(board.topic())), ResearchMessagePayload.Tone.INFO);
    }

    /** The parchment catches fire at a section: flames run over it, ash and smoke rise from the desk. */
    private void burnSheet(ServerPlayer player, ResearchBoard board, int cell) {
        sheet.burn(cell, Research.of(player).burn(board.topic()));
        Research.sync(player);
        setChanged();
        if (level instanceof ServerLevel serverLevel) {
            Vec3 sheetAt = TableGeometry.toWorld(worldPosition, facing(), TableGeometry.PARCHMENT.add(0, 0.6, 0));
            serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, sheetAt.x, sheetAt.y, sheetAt.z, 18, 0.22, 0.02, 0.16, 0.01);
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, sheetAt.x, sheetAt.y + 0.05, sheetAt.z, 6, 0.18, 0.02, 0.12, 0.02);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, sheetAt.x, sheetAt.y + 0.2, sheetAt.z, 30, 0.3, 0.2, 0.25, 0.02);
            level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 0.7F);
        }
    }

    // ---- Studying an item -----------------------------------------------------------------------------------------

    /**
     * The researcher holds the loupe over a slot of their open screen ({@code -1}: put it back). The time it rests on one
     * slot is what {@link #studyUnderLoupe} checks.
     */
    public void loupe(ServerPlayer player, int slot) {
        if (level == null) {
            return;
        }
        if (slot < 0) {
            if (player.getUUID().equals(loupeBy)) {
                lookAway();
            }
            return;
        }
        if (!player.getUUID().equals(loupeBy) || loupeSlot != slot) {
            loupeBy = player.getUUID();
            loupeSlot = slot;
            loupeSince = level.getGameTime();
        }
        if (!looking) {
            looking = true;
            level.playSound(player, worldPosition, ModSounds.LOUPE_LENS.get(), SoundSource.BLOCKS, 0.3F, 0.9F);
            sync();
        }
    }

    private void lookAway() {
        loupeBy = null;
        loupeSlot = -1;
        if (looking) {
            looking = false;
            sync();
        }
    }

    /**
     * The last lens of the loupe has looked at the item in {@code slot} of the researcher's open screen long enough: the
     * item is studied. Free and unlimited; refused unless the loupe really rested there for {@link Loupe#STUDY_TICKS}.
     */
    public void studyUnderLoupe(ServerPlayer player, int slot) {
        if (level == null || !player.getUUID().equals(loupeBy) || loupeSlot != slot
                || level.getGameTime() - loupeSince < Loupe.STUDY_TICKS - Loupe.SLACK_TICKS
                || slot >= player.containerMenu.slots.size()) {
            return;
        }
        ItemStack stack = player.containerMenu.slots.get(slot).getItem();
        if (!Loupe.studiable(stack) || studied(player, stack.getItem())) {
            return;
        }
        Item item = stack.getItem();
        ResearchersDiaryItem.sign(items.get(DIARY), player);
        Research.study(player, item);
        message(player, Component.translatable("research.hextale.study.complete", ResearchText.item(item)), ResearchMessagePayload.Tone.WIN);
        // The researcher hears the loupe on their screen; the others hear the table.
        level.playSound(player, worldPosition, ModSounds.LOUPE_STUDIED.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        if (level instanceof ServerLevel serverLevel) {
            Vec3 at = TableGeometry.toWorld(worldPosition, facing(), TableGeometry.LOUPE_LOOK);
            ItemEssences essences = ResearchItems.of(stack);
            for (Essence essence : essences.essences().keySet()) {
                serverLevel.sendParticles(new DustParticleOptions(essence.color(), 0.8F), at.x, at.y, at.z, 4, 0.08, 0.06, 0.08, 0.01);
            }
            serverLevel.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 4, 0.06, 0.04, 0.06, 0.015);
        }
        loupeSince = level.getGameTime();
    }

    private void fizzle() {
        if (level instanceof ServerLevel serverLevel) {
            Vec3 orb = orbPosition();
            serverLevel.sendParticles(ParticleTypes.SMOKE, orb.x, orb.y, orb.z, 6, 0.08, 0.05, 0.08, 0.01);
            level.playSound(null, worldPosition, ModSounds.RUNE_FAILED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_FIZZLE, 0);
        }
    }

    private static Component name(ResearchTopic topic) {
        return Component.translatable(topic.translationKey()).withColor(topic.color());
    }

    private static void message(ServerPlayer player, Component text, ResearchMessagePayload.Tone tone) {
        ModNetwork.sendToPlayer(player, new ResearchMessagePayload(text, tone));
    }

    /**
     * The notes go to the output slot, where they hold the drawing shown on the parchment; older notes left there go to
     * the researcher.
     */
    private void deliver(ServerPlayer player, ItemStack notes) {
        ItemStack older = items.get(OUTPUT);
        items.set(OUTPUT, notes);
        if (!older.isEmpty() && !player.getInventory().add(older)) {
            player.drop(older, false, Prediction.SERVER_ONLY);
        }
    }

    /**
     * The parchment follows the notes in the output slot: when they are taken out, their finished drawing leaves the desk;
     * notes put back show their drawing again (unless an attempt is under way). {@code show} false only clears.
     */
    private boolean matchNotes(boolean show) {
        ItemStack output = items.get(OUTPUT);
        NotesDrawing drawing = ResearchNotesItem.drawing(output);
        ResearchTopic topic = ResearchNotesItem.topic(output);
        if (drawing != null && topic != null) {
            int[] tree = drawing.tree();
            if (show && !sheet.shows(topic, drawing.attempt(), tree) && !sheet.inProgress()) {
                sheet.show(topic, drawing.attempt(), tree);
                return true;
            }
            return false;
        }
        if (sheet.isDone()) {
            sheet.clear();
            return true;
        }
        return false;
    }

    /** Notes may be laid back into the output slot to show their drawing, but never over an attempt under way. */
    public boolean acceptsNotes(ItemStack stack) {
        return ResearchNotesItem.drawing(stack) != null && !sheet.inProgress();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        if (slot == OUTPUT && matchNotes(true)) {
            setChanged();
        }
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack taken = super.removeItem(slot, amount);
        if (slot == OUTPUT && matchNotes(true)) {
            setChanged();
        }
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack taken = super.removeItemNoUpdate(slot);
        if (slot == OUTPUT && matchNotes(true)) {
            setChanged();
        }
        return taken;
    }

    private void completeEffects(ResearchTopic topic) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 orb = orbPosition();
        serverLevel.sendParticles(new DustParticleOptions(topic.color(), 1.2F), orb.x, orb.y, orb.z, 28, 0.25, 0.2, 0.25, 0.02);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, orb.x, orb.y, orb.z, 14, 0.15, 0.15, 0.15, 0.25);
        serverLevel.sendParticles(ParticleTypes.END_ROD, orb.x, orb.y, orb.z, 6, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, worldPosition, ModSounds.RUNE_FUSED.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_FUSED, topic.index());
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (level != null && level.isClientSide()) {
            long now = level.getGameTime();
            switch (id) {
                case EVENT_FUSED -> {
                    flareTime = now;
                    ResearchTopic topic = ResearchTopics.byIndex(param);
                    flareTopic = topic == null ? -1 : param;
                    flareColor = topic == null ? 0x5CC8F2 : topic.color();
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

    /** What each slot holds; the output slot is filled by finished research only. */
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case DIARY -> stack.is(ModItems.RESEARCHERS_DIARY.get());
            case HEXTECH -> stack.is(ModItems.UNSTABLE_HEXTECH.get());
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
        if (input.getIntOr("layout", 1) < LAYOUT) {
            NonNullList<ItemStack> old = NonNullList.withSize(OLD_SLOTS, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, old);
            for (int i = 0; i < OUTPUT + 1; i++) {
                items.set(i, old.get(i));
            }
            items.set(PAPER, old.get(4));
            orphan = old.get(3);
        } else {
            ContainerHelper.loadAllItems(input, items);
        }
        boolean wasLooking = looking;
        looking = input.getBooleanOr("looking", false);
        if (looking && !wasLooking && level != null) {
            lookingSince = level.getGameTime();
        }
        drawing = input.getBooleanOr("drawing", false);
        sheet.load(input.childOrEmpty("sheet"));
        // Tables saved before the notes kept their drawing may show one whose notes are gone.
        matchNotes(false);
        inkDraws = JarReach.read(input.getIntArray("ink_draws").orElse(new int[0]));
        inkDrawnAt = input.getLongOr("ink_drawn_at", -1000);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("layout", LAYOUT);
        sheet.save(output.child("sheet"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        tag.putBoolean("drawing", drawing);
        tag.putBoolean("looking", looking);
        if (level != null && level.getGameTime() - inkDrawnAt <= INK_STREAM_TICKS) {
            tag.putIntArray("ink_draws", JarReach.write(inkDraws));
            tag.putLong("ink_drawn_at", inkDrawnAt);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
