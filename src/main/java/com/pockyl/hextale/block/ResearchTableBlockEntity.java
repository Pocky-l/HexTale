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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

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
import com.pockyl.hextale.research.ItemElements;
import com.pockyl.hextale.research.ItemStudy;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.research.ResearchSheet;
import com.pockyl.hextale.research.ResearchText;
import com.pockyl.hextale.research.ResearchTool;
import com.pockyl.hextale.research.RuneBoard;
import com.pockyl.hextale.research.RuneBoards;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.rune.ComplexRunes;
import com.pockyl.hextale.rune.SimpleRune;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * The Research Table's contents and the server side of both mini-games - learning a rune on its parchment and
 * studying an item by resonance. Every action a client asks for is checked here against the sheet, the table and the
 * researcher's knowledge and inventory before it happens. Contents, both games and a "someone is writing" flag are
 * synced to clients; finishing a rune reaches every nearby client as a block event so the orb can flare for all.
 */
public class ResearchTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int DIARY = 0;
    public static final int HEXTECH = 1;
    public static final int OUTPUT = 2;
    /** Research tools, one slot each in {@link ResearchTool} order. */
    public static final int TOOLS = 3;
    /** The item whose elements are being studied. */
    public static final int STUDY = 8;
    public static final int SLOTS = 9;

    public static final int EVENT_FUSED = 1;
    public static final int EVENT_FIZZLE = 3;

    private static final int[] SIDE_SLOTS = {HEXTECH, STUDY};
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
        RandomSource random = level.random;
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

    /** A symbol was written at this table: the orb stirs for everyone and others hear the brush. */
    private void markDrawing(ServerPlayer player) {
        if (level == null) {
            return;
        }
        level.playSound(null, worldPosition, ModSounds.BRUSH_STROKE.get(), SoundSource.BLOCKS, 0.5F,
                0.9F + level.random.nextFloat() * 0.2F);
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

    /** A tool's strength for this researcher: 0 without the tool in its slot, else the stars of its rune. */
    public int toolLevel(Player player, ResearchTool tool) {
        return toolLevel(this, tool, Research.of(player));
    }

    /** Same, for a client copy of the slots and of the knowledge. */
    public static int toolLevel(Container table, ResearchTool tool, RuneKnowledge knowledge) {
        return table.getItem(TOOLS + tool.ordinal()).is(tool.item()) ? knowledge.stars(tool.rune()) : 0;
    }

    private static boolean studied(ServerPlayer player, Item item) {
        return Research.of(player).hasStudied(BuiltInRegistries.ITEM.getKey(item));
    }

    // ---- Learning a rune ------------------------------------------------------------------------------------------

    /** Lays the book's rune on the table: a new rune, a clean parchment. */
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
        RuneBoard board = RuneBoards.get(rune);
        RuneBoard.Par par = board.par();
        message(player, Component.translatable("research.hextale.selected", name(rune), board.points().size(),
                par.sheet() + toolLevel(player, ResearchTool.STRETCHER), par.two(), par.three()), ResearchMessagePayload.Tone.INFO);
        setChanged();
    }

    /** A clean sheet: free, it only wipes the symbols written so far. */
    public void newSheet(ServerPlayer player) {
        RuneBoard board = board();
        if (board == null || studyingItem) {
            return;
        }
        Requirement missing = Requirement.firstMissing(this, player);
        if (missing != null) {
            message(player, Component.translatable(missing.translationKey()), ResearchMessagePayload.Tone.BAD);
            return;
        }
        begin(player, board);
        message(player, Component.translatable("research.hextale.sheet", sheet.cap(), board.par().three()), ResearchMessagePayload.Tone.INFO);
        setChanged();
    }

    private void begin(ServerPlayer player, RuneBoard board) {
        ResearchersDiaryItem.sign(items.get(DIARY), player);
        sheet.begin(board.par().sheet() + toolLevel(player, ResearchTool.STRETCHER));
        if (Research.of(player).see(board.rune(), new int[]{HexGrid.CENTRE})) {
            Research.sync(player);
        }
        level.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    /**
     * Writes the next symbol into {@code target} with one studied item from the researcher's inventory. The first symbol
     * starts a sheet by itself.
     */
    public void write(ServerPlayer player, Item item, boolean ground, int target) {
        RuneBoard board = board();
        ItemElements elements = ResearchItems.of(item);
        if (board == null || studyingItem || elements == null || Requirement.diary(this, player) != null) {
            return;
        }
        if (!studied(player, item)) {
            message(player, Component.translatable("research.hextale.not_studied", item.getDescription()), ResearchMessagePayload.Tone.BAD);
            return;
        }
        if (ground && toolLevel(player, ResearchTool.MORTAR) < elements.tier()) {
            return;
        }
        if (!sheet.isOpen()) {
            Requirement missing = Requirement.firstMissing(this, player);
            if (missing != null) {
                message(player, Component.translatable(missing.translationKey()), ResearchMessagePayload.Tone.BAD);
                return;
            }
            begin(player, board);
        }
        if (!sheet.canWrite() || sheet.preview(board, elements, ground).end() != target) {
            setChanged();
            return;
        }
        if (!take(player, item)) {
            message(player, Component.translatable("research.hextale.no_item", item.getDescription()), ResearchMessagePayload.Tone.BAD);
            return;
        }
        HexGrid.Walk walk = sheet.write(board, elements, BuiltInRegistries.ITEM.getKey(item), ground);
        if (walk == null) {
            return;
        }
        int[] passed = new int[walk.cells().length + 1];
        System.arraycopy(walk.cells(), 0, passed, 0, walk.cells().length);
        passed[walk.cells().length] = walk.end();
        if (Research.of(player).see(board.rune(), passed)) {
            Research.sync(player);
        }

        MutableComponent text = Component.translatable(ground ? "research.hextale.written.ground" : "research.hextale.written",
                item.getDescription());
        switch (walk.stop()) {
            case WALL -> text.append(" ").append(Component.translatable("research.hextale.stop.wall"));
            case EDGE -> text.append(" ").append(Component.translatable("research.hextale.stop.edge"));
            default -> {
            }
        }
        ResearchMessagePayload.Tone tone = ResearchMessagePayload.Tone.INFO;
        int point = board.pointAt(walk.end());
        if (point >= 0 && !sheet.isFilled(point)) {
            RuneBoard.Point p = board.points().get(point);
            text.append(" ").append(Component.translatable("research.hextale.covered", ResearchText.element(p.element()),
                    p.resource().getDescription()));
            tone = ResearchMessagePayload.Tone.GOOD;
        }
        if (sheet.failed(board)) {
            failed(player, text);
        } else {
            if (sheet.used() >= sheet.cap()) {
                text.append(" ").append(Component.translatable("research.hextale.full_but_ready"));
                tone = ResearchMessagePayload.Tone.BAD;
            }
            message(player, text, tone);
        }
        markDrawing(player);
        setChanged();
    }

    /** Leaves a covered point's resource (a studied item) from the researcher's inventory. */
    public void leave(ServerPlayer player, int point) {
        RuneBoard board = board();
        if (board == null || studyingItem || point < 0 || point >= board.points().size() || !sheet.isActive()
                || !sheet.ready(board, point) || Requirement.diary(this, player) != null) {
            return;
        }
        RuneBoard.Point p = board.points().get(point);
        if (!studied(player, p.resource())) {
            message(player, Component.translatable("research.hextale.not_studied", p.resource().getDescription()),
                    ResearchMessagePayload.Tone.BAD);
            return;
        }
        if (!take(player, p.resource())) {
            message(player, Component.translatable("research.hextale.no_item", p.resource().getDescription()),
                    ResearchMessagePayload.Tone.BAD);
            return;
        }
        int dissolvedBefore = board.dissolved(sheet.filledMask());
        ResearchSheet.Result result = sheet.leave(board, point);
        MutableComponent text = Component.translatable("research.hextale.left", p.resource().getDescription(),
                ResearchText.element(p.element()));
        if ((dissolvedBefore & 1 << p.element().ordinal()) == 0 && hasWalls(board, p.element())) {
            text.append(" ").append(Component.translatable("research.hextale.dissolved", ResearchText.element(p.element())));
        }
        pointEffects(p.element());
        switch (result) {
            case COMPLETE -> complete(player, board, text);
            case FAILED -> failed(player, text);
            default -> {
                int left = board.points().size() - Integer.bitCount(sheet.filledMask());
                message(player, text.append(" ").append(Component.translatable("research.hextale.points_left", left)),
                        ResearchMessagePayload.Tone.GOOD);
            }
        }
        setChanged();
    }

    private static boolean hasWalls(RuneBoard board, SimpleRune element) {
        for (SimpleRune wall : board.walls()) {
            if (wall == element) {
                return true;
            }
        }
        return false;
    }

    public void scrape(ServerPlayer player) {
        RuneBoard board = board();
        if (board != null && !studyingItem && Requirement.diary(this, player) == null
                && sheet.scrape(board, toolLevel(player, ResearchTool.SCRAPER))) {
            level.playSound(null, worldPosition, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 0.8F, 1.2F);
            message(player, Component.translatable("research.hextale.scraped"), ResearchMessagePayload.Tone.INFO);
            setChanged();
        }
    }

    public void jump(ServerPlayer player, int cell) {
        RuneBoard board = board();
        int fork = toolLevel(player, ResearchTool.FORK);
        if (board != null && !studyingItem && Requirement.diary(this, player) == null && cell >= 0 && cell < HexGrid.COUNT
                && sheet.jump(board, cell, fork)) {
            message(player, Component.translatable(cell == HexGrid.CENTRE ? "research.hextale.fork.orb" : "research.hextale.fork.point",
                    fork - sheet.jumps()), ResearchMessagePayload.Tone.INFO);
            setChanged();
        }
    }

    private void failed(ServerPlayer player, MutableComponent text) {
        message(player, text.append(" ").append(Component.translatable("research.hextale.failed")), ResearchMessagePayload.Tone.BAD);
        fizzle();
    }

    private void fizzle() {
        if (level instanceof ServerLevel serverLevel) {
            Vec3 orb = orbPosition();
            serverLevel.sendParticles(ParticleTypes.SMOKE, orb.x, orb.y, orb.z, 6, 0.08, 0.05, 0.08, 0.01);
            level.playSound(null, worldPosition, ModSounds.RUNE_FAILED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_FIZZLE, 0);
        }
    }

    /** Every point filled: the orb binds the rune, a scroll comes out, the researcher learns it. */
    private void complete(ServerPlayer player, RuneBoard board, MutableComponent text) {
        ComplexRune rune = board.rune();
        int stars = board.stars(sheet.used());
        int before = Research.of(player).stars(rune);
        items.get(HEXTECH).shrink(1);
        deliver(player, RuneScrollItem.of(rune, stars, ModItems.RUNE_SCROLL.get()));
        boolean fresh = Research.learn(player, rune, stars, sheet.used());
        text.append(" ").append(Component.translatable("research.hextale.complete", name(rune), "★".repeat(stars), sheet.used()));
        if (stars < 3) {
            text.append(" ").append(Component.translatable("research.hextale.next_star", "★".repeat(stars + 1),
                    stars == 1 ? board.par().two() : board.par().three()));
        } else {
            text.append(" ").append(Component.translatable("research.hextale.perfect"));
        }
        for (ResearchTool tool : ResearchTool.values()) {
            if (tool.rune() != rune) {
                continue;
            }
            if (fresh) {
                text.append(" ").append(Component.translatable("research.hextale.tool_unlocked", tool.item().getDescription()));
            } else if (stars > before) {
                text.append(" ").append(Component.translatable("research.hextale.tool_stronger", tool.item().getDescription()));
            }
        }
        message(player, text, ResearchMessagePayload.Tone.WIN);
        completeEffects(rune);
    }

    // ---- Studying an item -----------------------------------------------------------------------------------------

    /** Lays the item from the study slot on the parchment: resonance begins. */
    public void selectItem(ServerPlayer player) {
        Requirement missing = Requirement.diary(this, player);
        if (missing != null) {
            message(player, Component.translatable(missing.translationKey()), ResearchMessagePayload.Tone.BAD);
            return;
        }
        ItemStack stack = items.get(STUDY);
        ItemElements elements = ResearchItems.of(stack);
        if (elements == null) {
            message(player, Component.translatable("research.hextale.study.empty"), ResearchMessagePayload.Tone.BAD);
            return;
        }
        if (studied(player, stack.getItem())) {
            message(player, Component.translatable("research.hextale.study.known", stack.getHoverName()), ResearchMessagePayload.Tone.BAD);
            return;
        }
        ResearchersDiaryItem.sign(items.get(DIARY), player);
        study.begin(stack.getItem(), elements);
        studyingItem = true;
        message(player, Component.translatable("research.hextale.study.selected", stack.getHoverName(), elements.elements().size(),
                study.maxListens()), ResearchMessagePayload.Tone.INFO);
        setChanged();
    }

    /** Puts a tuning mark on a string section, moves it or takes it off. */
    public void mark(ServerPlayer player, int cell) {
        ItemElements elements = study.item() == null ? null : ResearchItems.of(study.item());
        if (!studyingItem || elements == null || Requirement.diary(this, player) != null) {
            return;
        }
        if (study.toggle(cell, elements)) {
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.6F, 1.6F);
            setChanged();
        }
    }

    /** Listens to the marks. The first listen takes one of the item from the study slot. */
    public void listen(ServerPlayer player) {
        Item item = study.item();
        ItemElements elements = item == null ? null : ResearchItems.of(item);
        if (!studyingItem || elements == null || Requirement.diary(this, player) != null || study.markCount() == 0
                || !study.isActive(elements)) {
            return;
        }
        if (!study.started()) {
            if (!items.get(STUDY).is(item)) {
                message(player, Component.translatable("research.hextale.study.missing", item.getDescription()), ResearchMessagePayload.Tone.BAD);
                return;
            }
            items.get(STUDY).shrink(1);
        }
        Map<Integer, ItemStudy.Echo> heard = study.listen(elements);
        if (heard == null) {
            return;
        }
        int exact = 0;
        int humming = 0;
        for (Map.Entry<Integer, ItemStudy.Echo> entry : heard.entrySet()) {
            if (entry.getValue() == ItemStudy.Echo.EXACT) {
                exact++;
                SimpleRune element = HexGrid.RING[ItemStudy.stringOf(entry.getKey())];
                pointEffects(element);
            } else if (entry.getValue() != ItemStudy.Echo.SILENT) {
                humming++;
            }
        }
        level.playSound(null, worldPosition, exact > 0 ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS, 1.0F, exact > 0 ? 1.2F : 0.8F + humming * 0.15F);
        if (study.won(elements)) {
            Research.study(player, item);
            MutableComponent all = Component.empty();
            boolean first = true;
            for (Map.Entry<SimpleRune, Integer> entry : elements.elements().entrySet()) {
                if (!first) {
                    all.append(", ");
                }
                all.append(ResearchText.element(entry.getKey())).append(" " + entry.getValue());
                first = false;
            }
            message(player, Component.translatable("research.hextale.study.complete", item.getDescription(), all),
                    ResearchMessagePayload.Tone.WIN);
            if (level instanceof ServerLevel serverLevel) {
                Vec3 orb = orbPosition();
                serverLevel.sendParticles(ParticleTypes.END_ROD, orb.x, orb.y, orb.z, 10, 0.15, 0.1, 0.15, 0.03);
            }
        } else if (study.lost(elements)) {
            message(player, Component.translatable("research.hextale.study.lost", item.getDescription()), ResearchMessagePayload.Tone.BAD);
            fizzle();
        } else {
            message(player, Component.translatable("research.hextale.study.heard", exact, humming, study.listensLeft()),
                    exact > 0 ? ResearchMessagePayload.Tone.GOOD : ResearchMessagePayload.Tone.INFO);
        }
        setChanged();
    }

    /** Takes one of the item from the player (creative players keep theirs). */
    private static boolean take(ServerPlayer player, Item item) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
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
            player.drop(scroll, false);
        }
    }

    private void pointEffects(SimpleRune element) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 orb = orbPosition();
        serverLevel.sendParticles(new DustParticleOptions(rgb(element.color()), 1.0F), orb.x, orb.y, orb.z, 10,
                0.2, 0.15, 0.2, 0.01);
        level.playSound(null, worldPosition, ModSounds.RUNE_RECOGNISED.get(), SoundSource.BLOCKS, 0.8F,
                0.8F + element.ordinal() * 0.06F);
    }

    private void completeEffects(ComplexRune rune) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 orb = orbPosition();
        serverLevel.sendParticles(new DustParticleOptions(rgb(rune.color()), 1.2F), orb.x, orb.y, orb.z, 28, 0.25, 0.2, 0.25, 0.02);
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, orb.x, orb.y, orb.z, 14, 0.15, 0.15, 0.15, 0.25);
        serverLevel.sendParticles(ParticleTypes.END_ROD, orb.x, orb.y, orb.z, 6, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, worldPosition, ModSounds.RUNE_FUSED.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.blockEvent(worldPosition, getBlockState().getBlock(), EVENT_FUSED, rune.index());
    }

    private static Vector3f rgb(int color) {
        return new Vector3f((color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F);
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

    /** What each slot holds; the output slot is filled by finished runes only. */
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case DIARY -> stack.is(ModItems.RESEARCHERS_DIARY.get());
            case HEXTECH -> stack.is(ModItems.UNSTABLE_HEXTECH.get());
            case STUDY -> ResearchItems.of(stack) != null;
            default -> slot >= TOOLS && slot < STUDY && stack.is(ResearchTool.values()[slot - TOOLS].item());
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
        sheet.load(tag.getCompound("sheet"));
        study.load(tag.getCompound("study"));
        studyingItem = tag.getBoolean("studying_item");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.put("sheet", sheet.save());
        tag.put("study", study.save());
        tag.putBoolean("studying_item", studyingItem);
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
