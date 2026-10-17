package com.pockyl.hextale.block;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.infusion.Ritual;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.runework.HextechTools;
import com.pockyl.hextale.runework.RuneSlots;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * The Hextech Altar: the centre item and the ritual in progress.
 * <p>
 * Idle, it looks at its circle twice a second and keeps a preview of what a ritual would make. A ritual warms up, then
 * draws the pedestal items one by one while it pulls essences from the tanks around; with everything in, a short finale
 * turns the centre item into the result. A ritual short of anything stalls (the circle flickers); stalled too long it
 * collapses: the drawn items are lost, the centre stays, the backlash hurts whoever stands close.
 */
public class HextechAltarBlockEntity extends BlockEntity implements Container {
    /** Height of the centre item over the altar's base (blocks). */
    public static final double ITEM_Y = 1.3;
    private static final int PREVIEW_TICKS = 10;
    private static final int RESCAN_TICKS = 40;
    private static final int SYNC_TICKS = 4;
    private static final int ABORT_GRACE = 30;
    private static final Codec<List<Ritual.Input>> INPUTS_CODEC = Codec.list(com.mojang.serialization.codecs.RecordCodecBuilder.create(
            instance -> instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(Ritual.Input::pos),
                    Identifier.CODEC.fieldOf("item").forGetter(Ritual.Input::item)
            ).apply(instance, Ritual.Input::new)));

    private ItemStack center = ItemStack.EMPTY;
    private ItemStack preview = ItemStack.EMPTY;
    private int previewTimer;

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
    /** Game time the ritual started (server). */
    private long startedAt;
    /** What the ritual waits for now, or null. */
    private @Nullable Component waitingFor;
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
        super(ModBlockEntities.HEXTECH_ALTAR.get(), pos, state);
    }

    public ItemStack center() {
        return center;
    }

    public ItemStack preview() {
        return preview;
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
        return kind != null && kind != Ritual.Kind.CHARGE && inputs.isEmpty() && essenceLeft.isEmpty();
    }

    // ---- Player use -------------------------------------------------------------------------------------------------

    public boolean place(ItemStack stack) {
        if (active() || !center.isEmpty() || stack.isEmpty()) {
            return false;
        }
        center = stack.copyWithCount(1);
        refreshPreview();
        setChanged();
        return true;
    }

    public ItemStack take() {
        if (active()) {
            return ItemStack.EMPTY;
        }
        ItemStack out = center;
        center = ItemStack.EMPTY;
        preview = ItemStack.EMPTY;
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
                ComplexRune refused = center.isEmpty() ? null : Ritual.unusable(HextechTools.isTool(center) ? HextechTools.runes(center)
                        : RuneSlots.EMPTY, circle, false);
                if (refused != null && !circle.chosenRunes().isEmpty()) {
                    player.sendOverlayMessage(Component.translatable("block.hextale.hextech_altar.nothing.unusable",
                            Component.translatable(refused.translationKey()).withColor(refused.color())));
                } else {
                    String key = center.isEmpty() ? "block.hextale.hextech_altar.empty"
                            : circle.chosenRunes().isEmpty() ? "block.hextale.hextech_altar.nothing" : "block.hextale.hextech_altar.nothing.runes";
                    player.sendOverlayMessage(Component.translatable(key));
                }
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
        rescan = 0;
        preview = result.copy();
        startedAt = serverLevel.getGameTime();
        for (var stele : circle.steles()) {
            if (stele.rune() != null) {
                serverLevel.blockEvent(stele.getBlockPos(), stele.getBlockState().getBlock(), RuneSteleBlockEntity.EVENT_ACTIVE, duration + 20);
            }
        }
        serverLevel.playSound(null, worldPosition, ModSounds.RITUAL_START.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        sync();
    }

    // ---- Ticking ----------------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, HextechAltarBlockEntity altar) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (altar.kind == null) {
            if (++altar.previewTimer >= PREVIEW_TICKS) {
                altar.previewTimer = 0;
                altar.refreshPreview();
            }
            return;
        }
        if (altar.circle == null || ++altar.rescan >= RESCAN_TICKS) {
            altar.rescan = 0;
            altar.circle = Ritual.Circle.scan(serverLevel, pos);
        }
        boolean progressed = altar.kind == Ritual.Kind.CHARGE ? altar.tickCharge() : altar.tickInfusion(serverLevel);
        if (progressed) {
            altar.elapsed++;
            altar.stalled = 0;
            altar.waitingFor = null;
        } else if (altar.kind != null && ++altar.stalled > (altar.kind == Ritual.Kind.CHARGE ? 60 : Ritual.STALL_LIMIT)) {
            if (altar.kind == Ritual.Kind.CHARGE) {
                altar.finish(serverLevel);
            } else {
                altar.collapse(serverLevel);
            }
        }
        if (altar.kind != null && serverLevel.getGameTime() % SYNC_TICKS == 0) {
            altar.sync();
        }
        if (altar.kind != null && altar.stalled == 0 && serverLevel.getGameTime() % 40 == 0) {
            serverLevel.playSound(null, pos, ModSounds.RITUAL_HUM.get(), SoundSource.BLOCKS, 0.6F, 0.9F + 0.2F * altar.elapsed / Math.max(1F, altar.duration));
        }
        if (altar.kind != null && altar.stalled > 10 && serverLevel.getGameTime() % 40 == 0) {
            serverLevel.playSound(null, pos, ModSounds.RITUAL_STALL.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            if (altar.waitingFor != null) {
                for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(Ritual.TANK_RADIUS))) {
                    player.sendOverlayMessage(altar.waitingFor);
                }
            }
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
                    waitingFor = Component.translatable("block.hextale.hextech_altar.wait.item",
                            new ItemStack(BuiltInRegistries.ITEM.getValue(input.item())).getHoverName());
                }
            } else if (itemTimer == 1) {
                BlockPos next = inputs.getFirst().pos();
                level.blockEvent(next, level.getBlockState(next).getBlock(), PedestalBlockEntity.EVENT_DRAW, 0);
            }
        }
        if (!essenceLeft.isEmpty()) {
            EssenceMix drawn = drawEssence(Ritual.DRAW_PER_TICK);
            if (drawn.isEmpty()) {
                waiting = true;
                waitingFor = Component.translatable("block.hextale.hextech_altar.wait.essence", essenceLeft.describe());
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

    /** One tick of charging: the drawn essence goes straight into the tool's cell. */
    private boolean tickCharge() {
        EssenceMix drawn = drawEssence(Ritual.CHARGE_PER_TICK);
        if (!drawn.isEmpty()) {
            HextechTools.fill(center, drawn);
        }
        if (essenceLeft.isEmpty() && level instanceof ServerLevel serverLevel) {
            finish(serverLevel);
            return true;
        }
        return !drawn.isEmpty();
    }

    /** Pulls up to {@code rate} mB of every essence still owed from the tanks of the circle. */
    private EssenceMix drawEssence(int rate) {
        if (circle == null) {
            return EssenceMix.EMPTY;
        }
        EssenceMix drawn = EssenceMix.EMPTY;
        for (Essence essence : Essence.values()) {
            int owed = Math.min(rate, essenceLeft.get(essence));
            for (EssenceTankBlockEntity tank : circle.tanks()) {
                if (owed <= 0) {
                    break;
                }
                if (tank.isRemoved()) {
                    continue;
                }
                int take = Math.min(owed, tank.tank().contents().get(essence));
                if (take > 0) {
                    EssenceMix part = EssenceMix.pure(essence, take);
                    tank.tank().remove(part);
                    tank.setChanged();
                    drawn = drawn.plus(part);
                    owed -= take;
                }
            }
        }
        essenceLeft = essenceLeft.minus(drawn);
        return drawn;
    }

    private void absorb(ServerLevel level, PedestalBlockEntity pedestal) {
        ItemStack item = pedestal.take();
        Vec3 at = Vec3.atCenterOf(pedestal.getBlockPos()).add(0, 0.6, 0);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item.getItem()), at.x, at.y, at.z, 10, 0.12, 0.12, 0.12, 0.06);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.02);
        level.playSound(null, pedestal.getBlockPos(), ModSounds.RITUAL_ABSORB.get(), SoundSource.BLOCKS, 0.9F,
                0.85F + 0.3F * (itemsTotal - inputs.size()) / Math.max(1, itemsTotal));
    }

    private void finish(ServerLevel level) {
        center = kind == Ritual.Kind.CHARGE ? center : result.copy();
        clearRitual();
        Vec3 at = itemPosition();
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 40, 0.1, 0.1, 0.1, 0.18);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 20, 0.3, 0.3, 0.3, 0.3);
        level.playSound(null, worldPosition, ModSounds.RITUAL_COMPLETE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, 0);
        refreshPreview();
        sync();
    }

    /** A failed ritual: what was drawn is lost, the centre stays, the backlash strikes whoever is close. */
    private void collapse(ServerLevel level) {
        clearRitual();
        Vec3 at = itemPosition();
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 2, 0.3, 0.3, 0.3, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 50, 0.8, 0.5, 0.8, 0.5);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 16, 0.5, 0.3, 0.5, 0.02);
        level.playSound(null, worldPosition, ModSounds.RITUAL_COLLAPSE.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(4))) {
            player.hurtServer(level, level.damageSources().magic(), 4.0F);
        }
        level.blockEvent(worldPosition, getBlockState().getBlock(), 2, 0);
        refreshPreview();
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
    }

    /** Re-plans from the current circle and keeps what it would make, syncing when it changed. */
    private void refreshPreview() {
        if (!(level instanceof ServerLevel serverLevel) || active()) {
            return;
        }
        ItemStack next = ItemStack.EMPTY;
        if (!center.isEmpty()) {
            Ritual.Plan plan = Ritual.plan(center, Ritual.Circle.scan(serverLevel, worldPosition), false);
            if (plan != null) {
                next = plan.result();
            }
        }
        if (!ItemStack.matches(next, preview)) {
            preview = next;
            sync();
        }
    }

    public Vec3 itemPosition() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + ITEM_Y, worldPosition.getZ() + 0.5);
    }

    // ---- Client ---------------------------------------------------------------------------------------------------

    /** Client side: advances the smooth ritual time and throws the ritual's motes. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, HextechAltarBlockEntity altar) {
        if (altar.kind == null) {
            altar.clientElapsed = 0;
            return;
        }
        if (!altar.stalled()) {
            altar.clientElapsed = Math.min(altar.duration, altar.clientElapsed + 1);
        }
        RandomSource random = level.getRandom();
        Vec3 target = altar.itemPosition();
        if (altar.circle == null || level.getGameTime() % RESCAN_TICKS == 0) {
            altar.circle = Ritual.Circle.scan(level, pos);
        }
        Ritual.Circle circle = altar.circle;
        // Essence streams: from every tank holding an essence still owed.
        for (Essence essence : Essence.values()) {
            if (altar.essenceLeft.get(essence) <= 0) {
                continue;
            }
            for (EssenceTankBlockEntity tank : circle.tanks()) {
                if (tank.tank().contents().get(essence) > 0 && random.nextInt(2) == 0) {
                    Vec3 from = Vec3.atCenterOf(tank.getBlockPos()).add((random.nextDouble() - 0.5) * 0.4, 0.3, (random.nextDouble() - 0.5) * 0.4);
                    spawnFlow(level, target, from, essence.color());
                }
            }
        }
        // Rune steles feed the centre glyph light for the whole ritual.
        if (altar.kind != Ritual.Kind.CHARGE) {
            for (RuneSteleBlockEntity stele : circle.steles()) {
                if (stele.rune() != null && random.nextInt(3) == 0) {
                    Vec3 from = Vec3.atCenterOf(stele.getBlockPos()).add(0, 1.05, 0);
                    spawnFlow(level, target, from, stele.rune().color());
                }
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
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, target.x + (random.nextDouble() - 0.5) * 0.8, target.y + random.nextDouble() * 0.4,
                    target.z + (random.nextDouble() - 0.5) * 0.8, 0, 0.02, 0);
        }
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
        preview = input.read("preview", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        Ritual.Kind before = kind;
        kind = input.getString("ritual").map(name -> Ritual.Kind.valueOf(name)).orElse(null);
        inputs = new ArrayList<>(input.read("inputs", INPUTS_CODEC).orElse(List.of()));
        essenceLeft = input.read("essence_left", EssenceMix.CODEC).orElse(EssenceMix.EMPTY);
        essenceTotal = input.read("essence_total", EssenceMix.CODEC).orElse(EssenceMix.EMPTY);
        result = input.read("result", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        elapsed = input.getIntOr("elapsed", 0);
        duration = input.getIntOr("duration", 0);
        itemTimer = input.getIntOr("item_timer", 0);
        finale = input.getIntOr("finale", 0);
        stalled = input.getIntOr("stalled", 0);
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
        if (!preview.isEmpty()) {
            output.store("preview", ItemStack.OPTIONAL_CODEC, preview);
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
