package com.pockyl.hextale.block;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.multiblock.MultiblockCore;
import com.pockyl.hextale.multiblock.MultiblockPattern;
import com.pockyl.hextale.multiblock.Multiblocks;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModBlocks;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.VaultRune;
import com.pockyl.hextale.storage.VaultContents;
import com.pockyl.hextale.storage.VaultMemory;
import com.pockyl.hextale.storage.VaultRules;
import com.pockyl.hextale.storage.VaultStore;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A standing Hex Vault. What it holds lives in {@link VaultStore} under its id; the block only shows it: how full it is and the
 * kinds that take up the most room, drifting in the space behind the door. Nothing ticks on the server: a change asks for one
 * scheduled block tick, which sends the new picture, a batch of changes at most once in {@value #SYNC_TICKS} ticks. On clients the
 * door opens by itself for anyone who comes near, so everybody sees it before a screen covers it.
 *
 * <p>Storage Cubes count on the gate's platform or against it; a cube tells the vaults around when it comes or goes, so nobody
 * looks for them.
 */
public class HexVaultBlockEntity extends BlockEntity implements MultiblockCore, com.pockyl.hextale.port.Removal.SideEffects {
    /** Storage Cubes that count at the most. */
    public static final int MOST_CUBES = 12;
    /** Ticks between two looks at the gate's platform and pillars, which do not report their going. */
    private static final int WALL_CHECK_TICKS = 40;
    private static final int SYNC_TICKS = 20;
    /** Ticks between two rounds of Mending. */
    private static final int MEND_TICKS = 100;
    /** Worn entries one round of Mending reaches. */
    private static final int MEND_ENTRIES = 8;
    /** Ticks between two flashes of a refusing vault: a hopper asks again and again. */
    private static final int FLASH_TICKS = 30;
    /** Block event: the vault refused something (clients flash its rings). */
    public static final int REFUSED = 1;
    /** How far over the block's own floor the middle of the door hangs. */
    public static final double DOOR_HEIGHT = 2.5;
    /** A player this close opens the door, one this far away lets it shut; the gap keeps it from fluttering. */
    private static final double DOOR_OPENS = 5.5;
    private static final double DOOR_SHUTS = 7;
    private static final float DOOR_STEP = 1 / 24F;
    /** Ticks the vault takes to rise and unfold when its gate is woken. */
    public static final int FORMING_TICKS = 70;
    /** Kinds that drift behind the door. */
    public static final int SHOWN_KINDS = 6;
    public static final int HEXTECH = 0x7FD8FF;

    @Nullable
    private UUID id;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    private int cubes;
    private MultiblockCore.@Nullable Formed formed;
    /** Stand-ins of the old model round a vault from before the gates are gone from this load on. */
    private boolean standInsCleared;
    private long lastFlash = -1000;
    private long nextMend;
    private int lastSignal;
    /** A creative player broke an empty vault: there is nothing to keep. */
    private boolean dropsNothing;
    // What the clients draw (the server fills it in before it sends it).
    private long used;
    private long capacity = VaultRules.BASE_CAPACITY;
    private int kinds;
    /** Client side: game time of the last refusal, for the renderer. */
    public long refusedAt = -1000;
    /** Game time the gate was woken, for the rising on clients. */
    private long formedAt = -FORMING_TICKS;
    /** The kinds that take up the most room, plain items without their data. */
    private List<ItemStack> shown = List.of();
    /** Client side: whether the door is opening, and how far open it was on this tick and the one before. */
    private boolean doorWanted;
    private float door;
    private float doorBefore;

    public HexVaultBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEX_VAULT.get(), pos, state);
    }

    public UUID id() {
        if (id == null) {
            id = UUID.randomUUID();
            setChanged();
        }
        return id;
    }

    /** What this vault holds; server side only. */
    public VaultContents contents() {
        return VaultStore.of(((ServerLevel) level).getServer()).contents(id());
    }

    public PickaxeRunes runes() {
        return runes;
    }

    public void setRunes(PickaxeRunes runes) {
        this.runes = runes;
        setChanged();
        if (level instanceof ServerLevel) {
            recount();
        }
    }

    private int strength(VaultRune rune) {
        return runes.strength(rune.rune());
    }

    // ---- What the clients see ------------------------------------------------------------------------------------------

    public long used() {
        return used;
    }

    public long capacity() {
        return capacity;
    }

    public int kinds() {
        return kinds;
    }

    public int cubes() {
        return cubes;
    }

    @Override
    public MultiblockCore.@Nullable Formed formed() {
        return formed;
    }

    @Override
    public void setFormed(MultiblockCore.Formed formed) {
        this.formed = formed;
        setChanged();
        askForTick(1);
    }

    /** Whether it works: standing in its finished gate. Asleep, it only gives back by hand. */
    public boolean usable() {
        return getBlockState().getValue(HexVaultBlock.FORMED);
    }

    /** Woken with dust in a finished gate: it rises into its door, and from now on it works. */
    public void formedBy(ServerPlayer player) {
        recount();
        Quests.fire(player, Quests.VAULT_FORMED);
        if (level instanceof ServerLevel server) {
            formedAt = server.getGameTime();
            setChanged();
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            server.playSound(null, worldPosition.above(2), ModSounds.VAULT_FORMED.get(), SoundSource.BLOCKS, 1.4F, 1.0F);
        }
    }

    /** How far the vault has risen into its door, 0 to 1, on this frame. */
    public float forming(float partialTick) {
        return level == null ? 1 : Math.clamp((level.getGameTime() - formedAt + partialTick) / FORMING_TICKS, 0F, 1F);
    }

    /** Client side: the door follows the players around it, with its sound. */
    public static void clientTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, HexVaultBlockEntity vault) {
        double reach = vault.doorWanted ? DOOR_SHUTS : DOOR_OPENS;
        boolean wanted = vault.forming(0) >= 1 && level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + DOOR_HEIGHT, pos.getZ() + 0.5, reach,
                net.minecraft.world.entity.EntitySelector.NO_SPECTATORS) != null;
        if (wanted != vault.doorWanted) {
            vault.doorWanted = wanted;
            level.playLocalSound(pos.above(2), wanted ? ModSounds.VAULT_DOOR_OPEN.get() : ModSounds.VAULT_DOOR_SHUT.get(), SoundSource.BLOCKS, 0.8F, 1.0F, false);
        }
        vault.doorBefore = vault.door;
        vault.door = Math.clamp(vault.door + (wanted ? DOOR_STEP : -DOOR_STEP), 0F, 1F);
    }

    /** How far open the door is, 0 to 1, on this frame. */
    public float doorOpen(float partialTick) {
        return doorBefore + (door - doorBefore) * partialTick;
    }

    /** The kinds that take up the most room, for the renderer. */
    public List<ItemStack> shown() {
        return shown;
    }

    /** The bottom of the gate went: the block turned back into a sleeping vault. */
    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        if (formed != null && !state.getValue(HexVaultBlock.FORMED)) {
            formed = null;
            askForTick(1);
        }
    }

    /** The platform and pillars are the player's own blocks and report nothing: one gone wakes this. */
    private void checkWalls(ServerLevel level) {
        if (formed != null && !Multiblocks.VAULT_GATE.stands(level, formed.placement())) {
            Multiblocks.breakApart(level, worldPosition, worldPosition.below(1024), formed);
            formed = null;
            setChanged();
        }
    }

    /** The old model's invisible stand-ins would stand in the way of the gate round it. */
    private void clearStandIns(ServerLevel level) {
        if (standInsCleared) {
            return;
        }
        standInsCleared = true;
        for (BlockPos cell : BlockPos.betweenClosed(worldPosition.offset(-1, 0, -1), worldPosition.offset(1, ShapeParts.MAX_UP, 1))) {
            BlockState there = level.getBlockState(cell);
            if (there.getBlock() instanceof ShapePartBlock && ShapePartBlock.device(there, cell).equals(worldPosition)) {
                level.removeBlock(cell, false);
            }
        }
    }

    // ---- Coming and going ----------------------------------------------------------------------------------------------

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            contents().placed(GlobalPos.of(server.dimension(), worldPosition), this);
            recount();
            picture();
            lastSignal = contents().signal();
            askForTick(1);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel && id != null) {
            contents().unloaded(this);
        }
        super.setRemoved();
    }

    /**
     * Going for any reason (a player, a blast, a command): the one way out for the vault as an item, so its items can
     * never lose their way home. The block's loot table stays empty for that reason.
     */
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            for (BlockPos cube : shellCubes(server)) {
                StorageCubeBlock.light(server, cube, false);
            }
            Multiblocks.breakApart(server, pos, pos, formed);
            VaultContents contents = contents();
            contents.lifted(GlobalPos.of(server.dimension(), pos));
            if (!dropsNothing) {
                Vec3 at = Vec3.atCenterOf(pos);
                ItemStack vault = new ItemStack(ModItems.HEX_VAULT.get());
                vault.applyComponents(collectComponents());
                ItemEntity item = new ItemEntity(server, at.x, at.y, at.z, vault);
                item.setDefaultPickUpDelay();
                server.addFreshEntity(item);
            }
        }
        com.pockyl.hextale.port.Removal.vanilla(this, pos, state);
    }

    /** A creative player breaks it: an empty vault without runes goes without a trace, like any block. */
    public void brokenInCreative() {
        dropsNothing = contents().used() == 0 && runes.isEmpty();
    }

    // ---- Cubes and runes -----------------------------------------------------------------------------------------------

    /** The Storage Cubes on the gate's platform or against it. None while it is not formed. */
    private List<BlockPos> shellCubes(ServerLevel server) {
        if (formed == null) {
            return List.of();
        }
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos cell : Multiblocks.VAULT_GATE.shell(formed.placement())) {
            if (server.isLoaded(cell) && server.getBlockState(cell).is(ModBlocks.STORAGE_CUBE.get())) {
                found.add(cell);
            }
        }
        return found;
    }

    /** Pipes of other mods remember what they found here: awake or asleep, they must look again. */
    private void reachChanged(ServerLevel server) {
        server.invalidateCapabilities(worldPosition);
        if (formed != null) {
            for (MultiblockPattern.Cell cell : Multiblocks.VAULT_GATE.pattern().cells()) {
                if (!cell.key().kept()) {
                    server.invalidateCapabilities(formed.placement().at(cell.x(), cell.y(), cell.z()));
                }
            }
        }
    }

    /** Counts the cubes again and tells the store what the vault can do now, and whether it is open at all. */
    public void recount() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        List<BlockPos> found = shellCubes(server);
        cubes = Math.min(MOST_CUBES, found.size());
        for (int i = 0; i < found.size(); i++) {
            StorageCubeBlock.light(server, found.get(i), i < MOST_CUBES);
        }
        if (contents().open() != usable()) {
            contents().setOpen(usable());
            reachChanged(server);
        }
        long room = VaultRules.BASE_CAPACITY + cubes * VaultRules.CUBE_CAPACITY;
        int cache = strength(VaultRune.CACHE);
        if (cache > 0) {
            room += (long) (room * HolderRune.at(VaultRune.ROOM, cache));
        }
        contents().setPerks(room, strength(VaultRune.THRIFT), strength(VaultRune.RADIANCE));
        contentsChanged();
    }

    // ---- Changes -------------------------------------------------------------------------------------------------------

    /** Something went in or out: the picture is sent on the next tick asked for here. */
    public void contentsChanged() {
        setChanged();
        askForTick(SYNC_TICKS);
    }

    private void askForTick(int delay) {
        if (level instanceof ServerLevel server && !server.getBlockTicks().hasScheduledTick(worldPosition, getBlockState().getBlock())) {
            server.scheduleTick(worldPosition, getBlockState().getBlock(), delay);
        }
    }

    /** The one tick asked for: new picture, comparators, Radiance's experience, a round of Mending. */
    void tick(ServerLevel level) {
        clearStandIns(level);
        checkWalls(level);
        if (contents().open() != usable()) {
            recount();
        }
        if (formed != null) {
            askForTick(WALL_CHECK_TICKS);
        }
        VaultContents contents = contents();
        picture();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        int signal = contents.signal();
        if (signal != lastSignal) {
            lastSignal = signal;
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
        reward(level, contents);
        int mending = usable() ? strength(VaultRune.MENDING) : 0;
        if (mending > 0) {
            long now = level.getGameTime();
            boolean worn = true;
            if (now >= nextMend) {
                nextMend = now + MEND_TICKS;
                worn = contents.mend(HolderRune.at(VaultRune.MEND_POINTS, mending), MEND_ENTRIES);
            }
            if (worn) {
                askForTick((int) (nextMend - now));
            }
        }
    }

    private void picture() {
        VaultContents contents = contents();
        used = contents.used();
        capacity = contents.capacity();
        kinds = contents.kinds();
        shown = contents.heaviest(SHOWN_KINDS).stream().map(entry -> new ItemStack(entry.resource().getItem())).toList();
    }

    /** Radiance: orbs for every height the vault has not held before, so taking out and putting back earns nothing. */
    private void reward(ServerLevel level, VaultContents contents) {
        long step = HolderRune.at(VaultRune.POINTS_PER_XP, Math.max(1, contents.radiance()));
        long unpaid = contents.unpaidPoints();
        if (unpaid >= step) {
            int experience = (int) Math.min(50, unpaid / step);
            contents.paid(experience * step);
            ExperienceOrb.award(level, Vec3.atCenterOf(worldPosition).add(0, 1, 0), experience);
        }
    }

    /** It said no to something: a flash everyone sees, not more often than {@value #FLASH_TICKS} ticks. */
    public void refused() {
        if (level instanceof ServerLevel server && server.getGameTime() - lastFlash >= FLASH_TICKS) {
            lastFlash = server.getGameTime();
            server.blockEvent(worldPosition, getBlockState().getBlock(), REFUSED, 0);
            server.playSound(null, worldPosition, ModSounds.VAULT_REFUSE.get(), SoundSource.BLOCKS, 0.4F, 1.0F);
        }
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == REFUSED) {
            if (level != null && level.isClientSide()) {
                refusedAt = level.getGameTime();
                Vec3 at = Vec3.atCenterOf(worldPosition).relative(getBlockState().getValue(HexVaultBlock.FACING), 0.6);
                for (int i = 0; i < 18; i++) {
                    double angle = i * Math.PI / 9;
                    level.addParticle(ModParticles.glint(0xFF9A7A), at.x + Math.cos(angle) * 1.4, at.y, at.z + Math.sin(angle) * 1.4,
                            Math.cos(angle) * 0.03, 0, Math.sin(angle) * 0.03);
                }
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }

    // ---- Saving --------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        id = input.read("vault", UUIDUtil.STRING_CODEC).orElse(id);
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
        formed = input.read("formed", MultiblockCore.Formed.CODEC).orElse(null);
        formedAt = input.read("formed_at", Codec.LONG).orElse((long) -FORMING_TICKS);
        shown = input.read("shown", ItemStack.CODEC.listOf()).orElse(List.of());
        cubes = input.getIntOr("cubes", 0);
        used = input.read("used", Codec.LONG).orElse(0L);
        capacity = input.read("capacity", Codec.LONG).orElse(VaultRules.BASE_CAPACITY);
        kinds = input.getIntOr("kinds", 0);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        if (id != null) {
            output.store("vault", UUIDUtil.STRING_CODEC, id);
        }
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
        if (formed != null) {
            output.store("formed", MultiblockCore.Formed.CODEC, formed);
        }
        output.store("formed_at", Codec.LONG, formedAt);
        output.store("shown", ItemStack.CODEC.listOf(), shown);
        output.putInt("cubes", cubes);
        output.store("used", Codec.LONG, used);
        output.store("capacity", Codec.LONG, capacity);
        output.putInt("kinds", kinds);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.VAULT_RUNES.get(), PickaxeRunes.EMPTY);
        VaultMemory memory = components.get(ModDataComponents.VAULT_MEMORY.get());
        if (memory != null) {
            id = memory.vault();
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.VAULT_RUNES.get(), runes);
        }
        if (id != null && level instanceof ServerLevel) {
            VaultContents contents = contents();
            components.set(ModDataComponents.VAULT_MEMORY.get(), new VaultMemory(id, contents.kinds(), contents.used()));
        }
    }

    @Override
    public void removeComponentsFromTag(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, net.minecraft.core.RegistryAccess.EMPTY);
        super.removeComponentsFromTag(output.tag());
        output.discard("vault");
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
