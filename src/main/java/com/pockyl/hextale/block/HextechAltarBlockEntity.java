package com.pockyl.hextale.block;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.infusion.Ritual;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.rune.ComplexRune;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The Hextech Altar: the centre item and the ritual in progress.
 * <p>
 * Idle, it holds its centre item (the client shows ghosts of the pedestal items the circle lacks). A ritual warms up, then
 * draws the pedestal items one by one while it pulls essences from the tanks around; with everything in, a short finale
 * turns the centre item into the result. A ritual short of anything stalls (the circle flickers); stalled too long it
 * collapses: the drawn items are lost, the centre stays, the backlash hurts whoever stands close.
 */
public class HextechAltarBlockEntity extends BlockEntity implements Container {
    /** Height of the centre item over the altar's base (blocks). */
    public static final double ITEM_Y = 1.3;
    private static final int RESCAN_TICKS = 40;
    /** How often the client looks again which pedestal items the circle lacks. */
    private static final int GHOST_TICKS = 10;
    private static final int SYNC_TICKS = 4;
    private static final int ABORT_GRACE = 30;
    private static final Codec<List<Ritual.Input>> INPUTS_CODEC = Codec.list(com.mojang.serialization.codecs.RecordCodecBuilder.create(
            instance -> instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(Ritual.Input::pos),
                    Identifier.CODEC.fieldOf("item").forGetter(Ritual.Input::item)
            ).apply(instance, Ritual.Input::new)));

    private ItemStack center = ItemStack.EMPTY;

    // The ritual in progress (kind == null: idle).
    private Ritual.@Nullable Kind kind;
    private List<Ritual.Input> inputs = new ArrayList<>();
    private EssenceMix essenceLeft = EssenceMix.EMPTY;
    private EssenceMix essenceTotal = EssenceMix.EMPTY;
    private ItemStack result = ItemStack.EMPTY;
    private int elapsed;
    private int duration;
    private int itemTimer;
    private int finale;
    private int stalled;
    private int itemsTotal;
    /** Ticks spent drawing essence so far (paces the draw). */
    private int drawTick;
    /** Game time the ritual started (server). */
    private long startedAt;
    /** Share of a second step per tick the chalk runes around give (rescanned with the circle). */
    private float chalkSpeed;
    private Ritual.@Nullable Circle circle;
    private int rescan;

    // Client side: smooth time and effects.
    private float clientElapsed;
    private long finishedAt = Long.MIN_VALUE;
    private long collapsedAt = Long.MIN_VALUE;
    private float ringSpin = 0.8F;
    private float outerPhase;
    private float innerPhase;
    private float ringTime = Float.NaN;
    private float itemTurn;
    private float itemTime = Float.NaN;

    public HextechAltarBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.HEXTECH_ALTAR.get(), pos, state);
    }

    protected HextechAltarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemStack center() {
        return center;
    }


    public boolean active() {
        return kind != null;
    }

    public Ritual.@Nullable Kind kind() {
        return kind;
    }

    public boolean stalled() {
        return stalled > 10;
    }

    public EssenceMix essenceLeft() {
        return essenceLeft;
    }

    /** The pedestal being drawn now, or null. */
    @Nullable
    public BlockPos drawing() {
        return kind == null || inputs.isEmpty() || elapsed < Ritual.WARMUP_TICKS ? null : inputs.getFirst().pos();
    }

    /** 0 at the start of a ritual, 1 at its end (smooth on the client). */
    public float progress(float partialTick) {
        if (kind == null || duration <= 0) {
            return 0;
        }
        return Mth.clamp((clientElapsed + partialTick) / duration, 0, 1);
    }

    /** Ticks since the warm-up began (client side smooth). */
    public float ritualTime(float partialTick) {
        return kind == null ? 0 : clientElapsed + partialTick;
    }

    /**
     * Client side: advances the gimbal rings to {@code time}, their speed easing towards {@code targetSpin} degrees per
     * tick, and returns their phases {outer, inner}.
     */
    public float[] animateRings(float time, float targetSpin) {
        float delta = Float.isNaN(ringTime) || time < ringTime ? 0 : Math.min(time - ringTime, 5);
        ringTime = time;
        ringSpin = Mth.lerp(Math.min(1, 0.05F * delta), ringSpin, targetSpin);
        outerPhase = (outerPhase + ringSpin * 0.5F * delta) % 360;
        innerPhase = (innerPhase + ringSpin * 0.8F * delta) % 360;
        return new float[]{outerPhase, innerPhase};
    }

    /** Client side: the centre item's turn (degrees) at {@code time}, turning at {@code speed} degrees per tick. */
    public float animateItem(float time, float speed) {
        float delta = Float.isNaN(itemTime) || time < itemTime ? 0 : Math.min(time - itemTime, 5);
        itemTime = time;
        itemTurn = (itemTurn + speed * delta) % 360;
        return itemTurn;
    }

    public long finishedAt() {
        return finishedAt;
    }

    public long collapsedAt() {
        return collapsedAt;
    }

    public boolean finishing() {
        return kind != null && inputs.isEmpty() && essenceLeft.isEmpty();
    }

    // ---- Player use -------------------------------------------------------------------------------------------------

    public boolean place(ItemStack stack) {
        if (active() || !center.isEmpty() || stack.isEmpty()) {
            return false;
        }
        center = stack.copyWithCount(1);
        setChanged();
        return true;
    }

    public ItemStack take() {
        if (active()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = center;
        center = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    /**
     * Starts the ritual the circle allows, or aborts the running one.
     *
     * @param player who started it (null: redstone)
     */
    public void startOrAbort(@Nullable ServerPlayer player) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (active()) {
            // Holding the use key repeats the click every few ticks: only a fresh sneak-use after the start aborts.
            if (player != null && serverLevel.getGameTime() - startedAt > ABORT_GRACE) {
                collapse(serverLevel);
            }
            return;
        }
        circle = Ritual.Circle.scan(serverLevel, worldPosition);
        Ritual.Plan plan = Ritual.plan(center, circle, false);
        if (plan == null) {
            if (player != null) {
                Ritual.Hint hint = center.isEmpty() ? Ritual.Hint.NONE : Ritual.hint(center, circle, false);
                if (com.pockyl.hextale.runework.RuneHolder.of(center) != null && circle.filledPedestals().isEmpty() && !circle.chosenRunes().isEmpty()) {
                    // Why the steles' runes cannot go into this pickaxe.
                    Ritual.Engraving engraving = Ritual.engraving(center, circle);
                    Object[] names = engraving.named().stream()
                            .map(rune -> (Object) Component.translatable(rune.translationKey()).withColor(rune.color())).toArray();
                    player.sendOverlayMessage(Component.translatable("block.hextale.hextech_altar.engrave." + engraving.refusal(), names));
                } else if (Ritual.blankScroll(center)) {
                    // A scroll takes exactly one rune, from one stele, and nothing from the pedestals.
                    player.sendOverlayMessage(Component.translatable("block.hextale.hextech_altar.write.one_rune"));
                } else if (!hint.runes().isEmpty()) {
                    // The recipe this circle is closest to wants runes no stele shows: name them.
                    MutableComponent names = Component.empty();
                    for (int i = 0; i < hint.runes().size(); i++) {
                        ComplexRune rune = hint.runes().get(i);
                        names.append(i == 0 ? Component.empty() : Component.literal(", "))
                                .append(Component.translatable(rune.translationKey()).withColor(rune.color()));
                    }
                    player.sendOverlayMessage(Component.translatable("block.hextale.hextech_altar.nothing.runes_missing", names));
                } else {
                    player.sendOverlayMessage(Component.translatable(center.isEmpty() ? "block.hextale.hextech_altar.empty"
                            : "block.hextale.hextech_altar.nothing"));
                }
            }
            serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.8F, 0.6F);
            return;
        }
        ResearchTopic topic = plan.kind() == Ritual.Kind.INFUSE ? ResearchTopics.of(plan.result().getItem()) : null;
        if (topic != null && !researcherNear(serverLevel, player, topic)) {
            // Nobody here has worked this out at the table yet: the circle does not know how to make it.
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("research.hextale.locked",
                        Component.translatable(topic.translationKey()).withColor(topic.color())));
            }
            serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 0.8F, 0.6F);
            return;
        }
        kind = plan.kind();
        inputs = new ArrayList<>(plan.inputs());
        itemsTotal = inputs.size();
        essenceLeft = plan.essences();
        essenceTotal = plan.essences();
        result = plan.result();
        duration = Math.max(1, plan.duration());
        elapsed = 0;
        itemTimer = 0;
        finale = 0;
        stalled = 0;
        drawTick = 0;
        rescan = 0;
        startedAt = serverLevel.getGameTime();
        chalkSpeed = ChalkMarkBlock.speed(serverLevel, worldPosition);
        ChalkMarkBlock.light(serverLevel, worldPosition, true);
        ritualStarted(serverLevel);
        for (var stele : circle.steles()) {
            if (stele.rune() != null) {
                serverLevel.blockEvent(stele.getBlockPos(), stele.getBlockState().getBlock(), RuneSteleBlockEntity.EVENT_ACTIVE, duration + 20);
            }
        }
        serverLevel.playSound(null, worldPosition, ModSounds.RITUAL_START.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        sync();
    }

    /**
     * Whether the ritual's maker knows how: the player who started it, or - started by
     * redstone - any player near the altar who has researched the topic.
     */
    private boolean researcherNear(ServerLevel level, @Nullable ServerPlayer player, ResearchTopic topic) {
        if (player != null) {
            return Research.of(player).researched(topic);
        }
        for (ServerPlayer near : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(Ritual.TANK_RADIUS * 2))) {
            if (Research.of(near).researched(topic)) {
                return true;
            }
        }
        return false;
    }

    // ---- Ticking ----------------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, HextechAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (altar.kind == null) {
            return;
        }
        if (altar.circle == null || ++altar.rescan >= RESCAN_TICKS) {
            altar.rescan = 0;
            altar.circle = Ritual.Circle.scan(serverLevel, pos);
            altar.chalkSpeed = ChalkMarkBlock.speed(serverLevel, pos);
        }
        boolean progressed = altar.tickInfusion(serverLevel);
        if (progressed) {
            altar.elapsed++;
            altar.stalled = 0;
            // Chalk runes around the altar now and then let the ritual take a second step in the same tick.
            if (altar.kind != null && altar.chalkSpeed > 0 && serverLevel.getRandom().nextFloat() < altar.chalkSpeed
                    && altar.tickInfusion(serverLevel) && altar.kind != null) {
                altar.elapsed++;
            }
        } else if (altar.kind != null && ++altar.stalled > Ritual.STALL_LIMIT) {
            altar.collapse(serverLevel);
        }
        if (altar.kind != null) {
            altar.ritualTick(serverLevel);
        }
        if (altar.kind != null && serverLevel.getGameTime() % SYNC_TICKS == 0) {
            altar.sync();
        }
        if (altar.kind != null && altar.stalled == 0 && serverLevel.getGameTime() % 40 == 0) {
            serverLevel.playSound(null, pos, ModSounds.RITUAL_HUM.get(), SoundSource.BLOCKS, 0.6F, 0.9F + 0.2F * altar.elapsed / Math.max(1F, altar.duration));
        }
        if (altar.kind != null && altar.stalled > 10 && serverLevel.getGameTime() % 40 == 0) {
            // It never says what it waits for: the researcher has to see for themselves what went missing.
            serverLevel.playSound(null, pos, ModSounds.RITUAL_STALL.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
        }
    }

    /** One tick of an infusion, engraving or erasing; false when it is waiting for something. */
    private boolean tickInfusion(ServerLevel level) {
        if (elapsed < Ritual.WARMUP_TICKS) {
            return true;
        }
        boolean waiting = false;
        if (!inputs.isEmpty()) {
            if (++itemTimer >= Ritual.TICKS_PER_ITEM) {
                Ritual.Input input = inputs.getFirst();
                if (level.getBlockEntity(input.pos()) instanceof PedestalBlockEntity pedestal && input.holds(pedestal.item())) {
                    absorb(level, pedestal);
                    inputs.removeFirst();
                    itemTimer = 0;
                    if (!inputs.isEmpty()) {
                        BlockPos next = inputs.getFirst().pos();
                        level.blockEvent(next, level.getBlockState(next).getBlock(), PedestalBlockEntity.EVENT_DRAW, 0);
                    }
                } else {
                    itemTimer = Ritual.TICKS_PER_ITEM;
                    waiting = true;
                }
            } else if (itemTimer == 1) {
                BlockPos next = inputs.getFirst().pos();
                level.blockEvent(next, level.getBlockState(next).getBlock(), PedestalBlockEntity.EVENT_DRAW, 0);
            }
        }
        // The essences follow the items, one after another.
        Essence essence = inputs.isEmpty() ? Ritual.drawingEssence(essenceLeft) : null;
        if (essence != null) {
            int drawn = drawEssence(essence, Ritual.drawnOn(drawTick));
            if (drawn > 0 || Ritual.drawnOn(drawTick) == 0) {
                drawTick++;
            } else {
                waiting = true;
            }
        }
        if (inputs.isEmpty() && essenceLeft.isEmpty()) {
            if (++finale >= Ritual.FINALE_TICKS) {
                finish(level);
            }
            return true;
        }
        return !waiting;
    }

    /** Pulls up to {@code rate} mB of one owed essence from the tanks of the circle; returns the mB drawn. */
    private int drawEssence(Essence essence, int rate) {
        if (circle == null) {
            return 0;
        }
        int owed = Math.min(rate, essenceLeft.get(essence));
        int drawn = 0;
        for (EssenceTankBlockEntity tank : circle.tanks()) {
            if (owed <= 0) {
                break;
            }
            if (tank.isRemoved()) {
                continue;
            }
            int take = Math.min(owed, tank.tank().contents().get(essence));
            if (take > 0) {
                tank.tank().remove(EssenceMix.pure(essence, take));
                tank.setChanged();
                drawn += take;
                owed -= take;
            }
        }
        essenceLeft = essenceLeft.minus(EssenceMix.pure(essence, drawn));
        return drawn;
    }

    private void absorb(ServerLevel level, PedestalBlockEntity pedestal) {
        ItemStack item = pedestal.take();
        Vec3 at = Vec3.atCenterOf(pedestal.getBlockPos()).add(0, 0.6, 0);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item.getItem()), at.x, at.y, at.z, 10, 0.12, 0.12, 0.12, 0.06);
        level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.02);
        level.playSound(null, pedestal.getBlockPos(), ModSounds.RITUAL_ABSORB.get(), SoundSource.BLOCKS, 0.9F,
                0.85F + 0.3F * (itemsTotal - inputs.size()) / Math.max(1, itemsTotal));
    }

    private void finish(ServerLevel level) {
        ChalkMarkBlock.light(level, worldPosition, false);
        finished(level, result, kind);
        com.pockyl.hextale.thevoid.Thinning.add(level, worldPosition, essenceTotal.total() * com.pockyl.hextale.thevoid.Thinning.SPENT, com.pockyl.hextale.thevoid.Thinning.Source.SPENT);
        if (result.is(ModItems.HEXCORE.get()) && !(this instanceof HexcoreBlockEntity)) {
            // The circle's own heart was the work: the altar itself becomes the Hexcore.
            clearRitual();
            resetSteles(level);
            // The gemstone went into the core: the old altar must not drop it when the block is replaced.
            center = ItemStack.EMPTY;
            HexcoreBlockEntity.awaken(level, worldPosition, getBlockState());
            return;
        }
        center = result.copy();
        if (kind == Ritual.Kind.WRITE) {
            // A rune copied onto a scroll: the researchers standing by have learned to pass runes on.
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(Ritual.TANK_RADIUS))) {
                com.pockyl.hextale.quest.Quests.fire(player, com.pockyl.hextale.quest.Quests.RUNE_SCROLL_WRITTEN);
            }
        }
        clearRitual();
        resetSteles(level);
        Vec3 at = itemPosition();
        level.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y, at.z, 40, 0.1, 0.1, 0.1, 0.18);
        level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x, at.y, at.z, 20, 0.3, 0.3, 0.3, 0.3);
        level.sendParticles(ModParticles.ring(ModParticles.HEX_LIGHT), at.x, at.y, at.z, 0, 1.4, 0, 0, 1);
        level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), worldPosition.getX() + 0.5, worldPosition.getY() + 0.05,
                worldPosition.getZ() + 0.5, 0, 3.5, 1, 0, 1);
        level.playSound(null, worldPosition, ModSounds.RITUAL_COMPLETE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
        sync();
    }

    /** Quest makers' trigger: every researcher by the circle saw the work finish. */
    private void finished(ServerLevel level, ItemStack made, Ritual.@Nullable Kind done) {
        String name = done == null ? "infuse" : done.name().toLowerCase(java.util.Locale.ROOT);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(Ritual.TANK_RADIUS))) {
            com.pockyl.hextale.registry.ModTriggers.RITUAL_FINISHED.get().trigger(player, made, name);
        }
    }

    /** The runes went into the work: every stele of the circle goes dark until a rune is chosen again. */
    private void resetSteles(ServerLevel level) {
        for (RuneSteleBlockEntity stele : Ritual.Circle.scan(level, worldPosition).steles()) {
            if (stele.rune() != null) {
                stele.choose(null, 1);
            }
        }
    }

    /** A failed ritual: what was drawn is lost, the centre stays, the backlash strikes whoever is close. */
    private void collapse(ServerLevel level) {
        ChalkMarkBlock.light(level, worldPosition, false);
        clearRitual();
        Vec3 at = itemPosition();
        level.sendParticles(ModParticles.ring(0xFF5A4A), at.x, at.y, at.z, 0, 2.2, 0, 0, 1);
        level.sendParticles(ModParticles.spark(ModParticles.HEXTECH), at.x, at.y, at.z, 50, 0.8, 0.5, 0.8, 0.5);
        level.sendParticles(ModParticles.vapour(ModParticles.SOOT), at.x, at.y, at.z, 16, 0.5, 0.3, 0.5, 0.02);
        level.playSound(null, worldPosition, ModSounds.RITUAL_COLLAPSE.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(4))) {
            player.hurtServer(level, level.damageSources().magic(), 4.0F);
        }
        com.pockyl.hextale.thevoid.Thinning.add(level, worldPosition, com.pockyl.hextale.thevoid.Thinning.RITUAL_COLLAPSE, com.pockyl.hextale.thevoid.Thinning.Source.RITUAL_COLLAPSE);
        level.blockEvent(worldPosition, getBlockState().getBlock(), 2, 0);
        sync();
    }

    private void clearRitual() {
        kind = null;
        inputs = new ArrayList<>();
        essenceLeft = EssenceMix.EMPTY;
        essenceTotal = EssenceMix.EMPTY;
        result = ItemStack.EMPTY;
        elapsed = 0;
        duration = 0;
        itemTimer = 0;
        finale = 0;
        stalled = 0;
        itemsTotal = 0;
        drawTick = 0;
        chalkSpeed = 0;
    }

    public Vec3 itemPosition() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + itemY(), worldPosition.getZ() + 0.5);
    }

    /** Height of the centre item over the block's floor (blocks); the Hexcore holds it inside its floating matrix. */
    protected double itemY() {
        return ITEM_Y;
    }

    // ---- Hooks for the Hexcore --------------------------------------------------------------------------------------

    /** Server: a ritual has just started (its inputs, essences and circle are set). */
    protected void ritualStarted(ServerLevel level) {
    }

    /** Server: one tick of a running ritual, after its progress. */
    protected void ritualTick(ServerLevel level) {
    }

    /** Client: every tick, before the ritual's motes. */
    protected void clientUpdate(Level level) {
    }

    /** Pedestals whose items the ritual still has to draw, the next one first. */
    protected List<Ritual.Input> pendingInputs() {
        return inputs;
    }

    protected int itemsTotal() {
        return itemsTotal;
    }

    protected EssenceMix essenceTotal() {
        return essenceTotal;
    }

    /** Ticks the running ritual has progressed (server). */
    protected int elapsed() {
        return elapsed;
    }

    protected Ritual.@Nullable Circle circle() {
        return circle;
    }

    // ---- Client ---------------------------------------------------------------------------------------------------

    /** Client side: advances the smooth ritual time and throws the ritual's motes. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, HextechAltarBlockEntity altar) {
        altar.clientUpdate(level);
        if (altar.kind == null) {
            altar.clientElapsed = 0;
            if (level.getGameTime() % GHOST_TICKS == 0) {
                altar.updateGhosts(level);
            }
            return;
        }
        altar.ghosts = List.of();
        altar.extras = List.of();
        if (!altar.stalled()) {
            altar.clientElapsed = Math.min(altar.duration, altar.clientElapsed + 1);
        }
        RandomSource random = level.getRandom();
        Vec3 target = altar.itemPosition();
        if (altar.circle == null || level.getGameTime() % RESCAN_TICKS == 0) {
            altar.circle = Ritual.Circle.scan(level, pos);
        }
        Ritual.Circle circle = altar.circle;
        // An essence stream: once the items are in, from every tank holding the essence drawn now.
        Essence drawingEssence = altar.inputs.isEmpty() ? Ritual.drawingEssence(altar.essenceLeft) : null;
        if (drawingEssence != null) {
            Essence essence = drawingEssence;
            for (EssenceTankBlockEntity tank : circle.tanks()) {
                if (tank.tank().contents().get(essence) > 0 && random.nextInt(2) == 0) {
                    Vec3 from = Vec3.atCenterOf(tank.getBlockPos()).add((random.nextDouble() - 0.5) * 0.4, 0.3, (random.nextDouble() - 0.5) * 0.4);
                    spawnFlow(level, target, from, essence.color());
                }
            }
        }
        // Rune steles shed shards of their glyphs into the work for the whole ritual.
        for (RuneSteleBlockEntity stele : circle.steles()) {
            if (stele.rune() != null && random.nextInt(4) == 0) {
                Vec3 from = Vec3.atCenterOf(stele.getBlockPos()).add((random.nextDouble() - 0.5) * 0.3, 1.05 + random.nextDouble() * 0.2,
                        (random.nextDouble() - 0.5) * 0.3);
                Vec3 offset = from.subtract(target);
                level.addParticle(ModParticles.runeShard(stele.rune().color()), target.x, target.y, target.z, offset.x, offset.y, offset.z);
            }
        }
        BlockPos drawing = altar.drawing();
        if (drawing != null && level.getBlockEntity(drawing) instanceof PedestalBlockEntity pedestal && !pedestal.item().isEmpty()) {
            for (int i = 0; i < 2; i++) {
                Vec3 from = Vec3.atCenterOf(drawing).add((random.nextDouble() - 0.5) * 0.3, 0.75, (random.nextDouble() - 0.5) * 0.3);
                spawnFlow(level, target, from, i == 0 ? 0xF0D27A : 0x9FE6FF);
            }
        }
        if (altar.stalled() && random.nextInt(4) == 0) {
            level.addParticle(ModParticles.spark(ModParticles.HEXTECH), target.x + (random.nextDouble() - 0.5) * 0.8, target.y + random.nextDouble() * 0.4,
                    target.z + (random.nextDouble() - 0.5) * 0.8, 0, 0.02, 0);
        }
    }

    /** A missing pedestal item shown as a ghost: where it floats (relative to the altar block) and what it is. */
    public record Ghost(Vec3 at, ItemStack item) {
    }

    private List<Ghost> ghosts = List.of();
    private List<BlockPos> extras = List.of();

    /** Client: ghosts of the items the circle still needs. */
    public List<Ghost> ghosts() {
        return ghosts;
    }

    /** Client: pedestals holding something the ritual being set up does not take. */
    public List<BlockPos> extras() {
        return extras;
    }

    /** Client: works out what the circle lacks; each missing item floats over a free pedestal, the rest around the altar. */
    private void updateGhosts(Level level) {
        Ritual.Circle scanned = Ritual.Circle.scan(level, worldPosition);
        Ritual.Hint hint = Ritual.hint(center, scanned, true);
        List<Ghost> next = new java.util.ArrayList<>();
        List<PedestalBlockEntity> free = scanned.pedestals().stream().filter(pedestal -> pedestal.item().isEmpty()).toList();
        int around = Math.max(0, hint.missing().size() - free.size());
        int aroundIndex = 0;
        for (int i = 0; i < hint.missing().size(); i++) {
            ItemStack item = hint.missing().get(i);
            if (item.isEmpty()) {
                continue;
            }
            if (i < free.size()) {
                BlockPos at = free.get(i).getBlockPos().subtract(worldPosition);
                next.add(new Ghost(new Vec3(at.getX() + 0.5, at.getY() + 1.12, at.getZ() + 0.5), item));
            } else {
                // No free pedestal for it: it circles the altar, telling the player to set one more pedestal.
                double angle = Math.PI * 2 * aroundIndex++ / around;
                next.add(new Ghost(new Vec3(0.5 + Math.sin(angle) * 1.3, 1.0, 0.5 - Math.cos(angle) * 1.3), item));
            }
        }
        ghosts = next;
        extras = hint.extra();
    }

    private static void spawnFlow(Level level, Vec3 to, Vec3 from, int color) {
        Vec3 offset = from.subtract(to);
        level.addParticle(ModParticles.flow(color), to.x, to.y, to.z, offset.x, offset.y, offset.z);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (level != null && level.isClientSide()) {
            if (id == 1) {
                finishedAt = level.getGameTime();
                return true;
            }
            if (id == 2) {
                collapsedAt = level.getGameTime();
                return true;
            }
        }
        return id == 1 || id == 2 || super.triggerEvent(id, param);
    }

    private void sync() {
        setChanged();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Container: hoppers put the centre item in and take the result out while idle --------------------------------

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return center.isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return center;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        return count > 0 ? take() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return take();
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (active()) {
            return;
        }
        center = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !active() && center.isEmpty();
    }

    @Override
    public boolean canTakeItem(Container target, int slot, ItemStack stack) {
        return !active();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        if (!active()) {
            center = ItemStack.EMPTY;
            setChanged();
        }
    }

    // ---- Saving -----------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        center = input.read("center", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        Ritual.Kind before = kind;
        // A ritual of a kind that no longer exists (charging) is dropped.
        kind = input.getString("ritual").flatMap(name -> java.util.Arrays.stream(Ritual.Kind.values())
                .filter(k -> k.name().equals(name)).findFirst()).orElse(null);
        inputs = new ArrayList<>(input.read("inputs", INPUTS_CODEC).orElse(List.of()));
        essenceLeft = input.read("essence_left", EssenceMix.CODEC).orElse(EssenceMix.EMPTY);
        essenceTotal = input.read("essence_total", EssenceMix.CODEC).orElse(EssenceMix.EMPTY);
        result = input.read("result", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        elapsed = input.getIntOr("elapsed", 0);
        duration = input.getIntOr("duration", 0);
        itemTimer = input.getIntOr("item_timer", 0);
        finale = input.getIntOr("finale", 0);
        stalled = input.getIntOr("stalled", 0);
        drawTick = input.getIntOr("draw_tick", 0);
        itemsTotal = input.getIntOr("items_total", 0);
        if (level != null && level.isClientSide() && (before == null || Math.abs(clientElapsed - elapsed) > 10)) {
            clientElapsed = elapsed;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!center.isEmpty()) {
            output.store("center", ItemStack.OPTIONAL_CODEC, center);
        }
        if (kind != null) {
            output.putString("ritual", kind.name());
            output.store("inputs", INPUTS_CODEC, inputs);
            output.store("essence_left", EssenceMix.CODEC, essenceLeft);
            output.store("essence_total", EssenceMix.CODEC, essenceTotal);
            if (!result.isEmpty()) {
                output.store("result", ItemStack.OPTIONAL_CODEC, result);
            }
            output.putInt("elapsed", elapsed);
            output.putInt("duration", duration);
            output.putInt("item_timer", itemTimer);
            output.putInt("finale", finale);
            output.putInt("stalled", stalled);
            output.putInt("draw_tick", drawTick);
            output.putInt("items_total", itemsTotal);
        }
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
