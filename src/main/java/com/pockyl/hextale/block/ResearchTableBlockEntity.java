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

import com.pockyl.hextale.item.HextechInkItem;
import com.pockyl.hextale.item.ResearchersDiaryItem;
import com.pockyl.hextale.item.RuneBrushItem;
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

import org.jetbrains.annotations.Nullable;

/**
 * The Research Table's contents and the server side of the research mini-game: every action a client asks for is
 * checked here against the sheet, the materials on the table and the researcher's inventory before it happens.
 * Contents, the sheet and a "someone is writing" flag are synced to clients; finishing a rune reaches every nearby
 * client as a block event so the orb can flare for all of them.
 */
public class ResearchTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int DIARY = 0;
    public static final int PAPER = 1;
    public static final int HEXTECH = 2;
    public static final int INK = 3;
    public static final int BRUSH = 4;
    public static final int OUTPUT = 5;
    /** Research tools, one slot each in {@link ResearchTool} order. */
    public static final int TOOLS = 6;
    /** The item whose elements are being studied. */
    public static final int STUDY = TOOLS + ResearchTool.values().length;
    public static final int SLOTS = STUDY + 1;

    public static final int EVENT_FUSED = 1;
    public static final int EVENT_FIZZLE = 3;

    private static final int[] SIDE_SLOTS = {PAPER, HEXTECH};
    private static final int[] BOTTOM_SLOTS = {OUTPUT};
    /** How long the orb keeps stirring after the last symbol. */
    private static final int DRAWING_TICKS = 30;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final ResearchSheet sheet = new ResearchSheet();
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

    /** The parchment of the rune or item being studied, or null (none chosen). */
    @Nullable
    public RuneBoard board() {
        return boardOf(sheet);
    }

    /** Same, for a client copy of the sheet. */
    @Nullable
    public static RuneBoard boardOf(ResearchSheet sheet) {
        ComplexRune rune = sheet.rune();
        if (rune != null) {
            return RuneBoards.get(rune);
        }
        Item item = sheet.item();
        ItemElements elements = item == null ? null : ResearchItems.of(item);
        return elements == null ? null : RuneBoards.forItem(item, elements);
    }

    /** Lays the item from the study slot on the table: its elements are hidden on the parchment. */
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
        if (Research.of(player).hasStudied(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
            message(player, Component.translatable("research.hextale.study.known", stack.getHoverName()), ResearchMessagePayload.Tone.BAD);
            return;
        }
        ResearchersDiaryItem.sign(items.get(DIARY), player);
        sheet.selectItem(stack.getItem());
        RuneBoard board = RuneBoards.forItem(stack.getItem(), elements);
        message(player, Component.translatable("research.hextale.study.selected", stack.getHoverName(), board.points().size()),
                ResearchMessagePayload.Tone.INFO);
        setChanged();
    }

    /** A tool's strength for this researcher: 0 without the tool in its slot, else the stars of its rune. */
    public int toolLevel(Player player, ResearchTool tool) {
        return toolLevel(this, tool, Research.of(player));
    }

    /** Same, for a client copy of the slots and of the knowledge. */
    public static int toolLevel(Container table, ResearchTool tool, RuneKnowledge knowledge) {
        return table.getItem(TOOLS + tool.ordinal()).is(tool.item()) ? knowledge.stars(tool.rune()) : 0;
    }

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
        RuneBoard board = RuneBoards.get(rune);
        RuneBoard.Par par = board.par();
        message(player, Component.translatable("research.hextale.selected", name(rune), board.points().size(),
                par.sheet() + toolLevel(player, ResearchTool.STRETCHER), par.two(), par.three()), ResearchMessagePayload.Tone.INFO);
        setChanged();
    }

    /** Takes a fresh sheet (burning the one in progress): one paper, one dose of ink, one use of the brush. */
    public void newSheet(ServerPlayer player) {
        RuneBoard board = board();
        if (board == null) {
            message(player, Component.translatable("research.hextale.no_rune"), ResearchMessagePayload.Tone.BAD);
            return;
        }
        Requirement missing = Requirement.firstMissing(this, player);
        if (missing != null) {
            message(player, Component.translatable(missing.translationKey()), ResearchMessagePayload.Tone.BAD);
            return;
        }
        ResearchersDiaryItem.sign(items.get(DIARY), player);
        items.get(PAPER).shrink(1);
        items.set(INK, HextechInkItem.useDose(items.get(INK)));
        ItemStack brush = RuneBrushItem.wear(items.get(BRUSH));
        if (brush.isEmpty()) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
        items.set(BRUSH, brush);
        int cap = board.par().sheet() + toolLevel(player, ResearchTool.STRETCHER);
        sheet.begin(cap);
        if (board.rune() != null && Research.of(player).see(board.rune(), new int[]{HexGrid.CENTRE})) {
            Research.sync(player);
        }
        level.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 0.8F);
        message(player, board.isItemStudy() ? Component.translatable("research.hextale.study.sheet", cap)
                : Component.translatable("research.hextale.sheet", cap, board.par().three()), ResearchMessagePayload.Tone.INFO);
        setChanged();
    }

    /** Writes the next symbol with one item from the researcher's inventory. */
    public void write(ServerPlayer player, Item item, boolean ground) {
        RuneBoard board = board();
        ItemElements elements = ResearchItems.of(item);
        if (board == null || elements == null || !sheet.canWrite() || Requirement.diary(this, player) != null) {
            return;
        }
        if (ground && toolLevel(player, ResearchTool.MORTAR) < elements.tier()) {
            return;
        }
        if (!take(player, item)) {
            message(player, Component.translatable("research.hextale.no_item", item.getDescription()), ResearchMessagePayload.Tone.BAD);
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        int filledBefore = sheet.filledMask();
        HexGrid.Walk walk = sheet.write(board, elements, id, ground);
        if (walk == null) {
            return;
        }
        RuneKnowledge knowledge = Research.of(player);
        boolean fresh = knowledge.learnItem(id);
        int[] passed = new int[walk.cells().length + 1];
        System.arraycopy(walk.cells(), 0, passed, 0, walk.cells().length);
        passed[walk.cells().length] = walk.end();
        if (board.rune() != null) {
            knowledge.see(board.rune(), passed);
        }
        Research.sync(player);

        MutableComponent text = Component.translatable(ground ? "research.hextale.written.ground" : "research.hextale.written",
                item.getDescription());
        if (fresh) {
            text.append(" ").append(Component.translatable("research.hextale.written.fresh", ResearchText.elements(elements)));
        }
        switch (walk.stop()) {
            case WALL -> text.append(" ").append(Component.translatable("research.hextale.stop.wall"));
            case EDGE -> text.append(" ").append(Component.translatable("research.hextale.stop.edge"));
            default -> {
            }
        }
        if (walk.cells().length == 0) {
            text.append(" ").append(Component.translatable("research.hextale.stop.same"));
        }
        ResearchMessagePayload.Tone tone = ResearchMessagePayload.Tone.INFO;
        int point = board.pointAt(walk.end());
        if (board.isItemStudy() && point >= 0 && (filledBefore & 1 << point) == 0) {
            RuneBoard.Point p = board.points().get(point);
            text.append(" ").append(Component.translatable("research.hextale.study.revealed", ResearchText.element(p.element()), p.strength()));
            tone = ResearchMessagePayload.Tone.GOOD;
            pointEffects(p);
            if (sheet.complete(board)) {
                completeItem(player, board, text);
                markDrawing(player);
                setChanged();
                return;
            }
        } else if (point >= 0 && !sheet.isFilled(point)) {
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

    /** Leaves a point's resource from the researcher's inventory. */
    public void leave(ServerPlayer player, int point) {
        RuneBoard board = board();
        if (board == null || board.isItemStudy() || point < 0 || point >= board.points().size() || !sheet.isActive()
                || !sheet.ready(board, point)
                || Requirement.diary(this, player) != null) {
            return;
        }
        RuneBoard.Point p = board.points().get(point);
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
        pointEffects(p);
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
        if (board != null && Requirement.diary(this, player) == null
                && sheet.scrape(board, toolLevel(player, ResearchTool.SCRAPER))) {
            level.playSound(null, worldPosition, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 0.8F, 1.2F);
            message(player, Component.translatable("research.hextale.scraped"), ResearchMessagePayload.Tone.INFO);
            setChanged();
        }
    }

    public void jump(ServerPlayer player, int cell) {
        RuneBoard board = board();
        int fork = toolLevel(player, ResearchTool.FORK);
        if (board != null && Requirement.diary(this, player) == null && cell >= 0 && cell < HexGrid.COUNT
                && sheet.jump(board, cell, fork)) {
            message(player, Component.translatable(cell == HexGrid.CENTRE ? "research.hextale.fork.orb" : "research.hextale.fork.point",
                    fork - sheet.jumps()), ResearchMessagePayload.Tone.INFO);
            setChanged();
        }
    }

    private void failed(ServerPlayer player, MutableComponent text) {
        message(player, text.append(" ").append(Component.translatable("research.hextale.failed")), ResearchMessagePayload.Tone.BAD);
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

    /** Every hidden element revealed: the item is studied; one of it goes into the orb. */
    private void completeItem(ServerPlayer player, RuneBoard board, MutableComponent text) {
        Item item = board.item();
        ItemStack stack = items.get(STUDY);
        if (!stack.is(item)) {
            message(player, text.append(" ").append(Component.translatable("research.hextale.study.missing", item.getDescription())),
                    ResearchMessagePayload.Tone.BAD);
            return;
        }
        stack.shrink(1);
        Research.study(player, item);
        MutableComponent all = Component.empty();
        boolean first = true;
        for (RuneBoard.Point p : board.points()) {
            if (!first) {
                all.append(", ");
            }
            all.append(ResearchText.element(p.element())).append(" " + p.strength());
            first = false;
        }
        message(player, text.append(" ").append(Component.translatable("research.hextale.study.complete", item.getDescription(), all)),
                ResearchMessagePayload.Tone.WIN);
        if (level instanceof ServerLevel serverLevel) {
            Vec3 orb = orbPosition();
            serverLevel.sendParticles(ParticleTypes.END_ROD, orb.x, orb.y, orb.z, 10, 0.15, 0.1, 0.15, 0.03);
            level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        }
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

    private void pointEffects(RuneBoard.Point point) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 orb = orbPosition();
        serverLevel.sendParticles(new DustParticleOptions(rgb(point.element().color()), 1.0F), orb.x, orb.y, orb.z, 10,
                0.2, 0.15, 0.2, 0.01);
        level.playSound(null, worldPosition, ModSounds.RUNE_RECOGNISED.get(), SoundSource.BLOCKS, 0.8F,
                0.8F + point.element().ordinal() * 0.06F);
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
            case PAPER -> stack.is(Items.PAPER);
            case HEXTECH -> stack.is(ModItems.UNSTABLE_HEXTECH.get());
            case INK -> stack.is(ModItems.HEXTECH_INK.get());
            case BRUSH -> stack.is(ModItems.RUNE_BRUSH.get());
            default -> slot == STUDY ? ResearchItems.of(stack) != null
                    : slot >= TOOLS && slot < STUDY && stack.is(ResearchTool.values()[slot - TOOLS].item());
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
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.put("sheet", sheet.save());
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
