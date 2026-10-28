package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.chem.Flask;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.item.ResearchNotesItem;
import com.pockyl.hextale.knowledge.Research;
import com.pockyl.hextale.menu.LabBenchMenu;
import com.pockyl.hextale.network.ModNetwork;
import com.pockyl.hextale.network.ResearchMessagePayload;
import com.pockyl.hextale.quest.Guide;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.research.ExperimentPlan;
import com.pockyl.hextale.research.ResearchTopic;
import com.pockyl.hextale.research.ResearchTopics;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.LabRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.ToolEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * The Lab Bench: where chemtech is worked out. A glass bottle on the bench is the flask; drops of essence come from the
 * Essence Jars around (within {@link JarReach#RADIUS}, pure ones only, like the Research Table's ink) and stream into it;
 * a flame under it heats it. The researcher picks a chemtech topic from the bench's journal and makes its compound in
 * the flask ({@link ExperimentPlan}): pure enough and plenty of it, sealed with the topic's sample on the bench, it
 * becomes Research Notes. The flask runs its chemistry ({@link Flask}) on the server every tick while anything in it
 * changes; clients get its state a few times a second and run the same rules in between. Compounds seen forming are
 * written into the journal of everyone looking at the bench. A flask that reaches its bursting pressure shatters: its
 * contents and the bottle are lost.
 */
public class LabBenchBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {
    public static final int FLASK = 0;
    public static final int SAMPLE = 1;
    public static final int OUTPUT = 2;
    public static final int SIZE = 3;
    /** Millibuckets of essence one drop takes from the jars. */
    public static final int DROP_MB = 4;
    /** How long a drop's stream runs from its jar into the flask, in ticks. */
    public static final int STREAM_TICKS = 40;
    private static final int SYNC_TICKS = 5;
    private static final int[] TOP = {FLASK, SAMPLE};
    private static final int[] BOTTOM = {OUTPUT};

    public static final int OUTCOME_NONE = 0;
    public static final int OUTCOME_SEALED = 1;
    public static final int OUTCOME_BURST = -1;
    public static final int OUTCOME_CAUGHT = 2;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final Flask flask = new Flask();
    private @Nullable ResearchTopic topic;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    /** The jars the last drops came from and when (game time); synced for the streams. */
    private List<JarReach.Draw> draws = List.of();
    private long drawnAt = -1000;
    /** The last compound that formed and when (synced: bubbles of its colour, the screen's flash). */
    private @Nullable Essence formed;
    private long formedAt = -1000;
    /** How the last experiment ended (synced for the screen and the world). */
    private int outcome;
    private long outcomeAt = -1000;
    private boolean dirty;

    public LabBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAB_BENCH.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LabBenchBlockEntity bench) {
        bench.tick((ServerLevel) level, state);
    }

    /** Clients run the flask between the server's updates, so the gauges move smoothly. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, LabBenchBlockEntity bench) {
        if (bench.flask.active()) {
            bench.flask.tick(bench.tuning());
        }
    }

    private void tick(ServerLevel level, BlockState state) {
        if (flask.active()) {
            List<Essence> made = flask.tick(tuning());
            if (!made.isEmpty()) {
                Essence first = made.getFirst();
                if (first != formed || level.getGameTime() - formedAt > 40) {
                    level.playSound(null, worldPosition, ModSounds.LAB_REACT.get(), SoundSource.BLOCKS, 0.5F, 0.9F + level.getRandom().nextFloat() * 0.2F);
                }
                formed = first;
                formedAt = level.getGameTime();
                for (ServerPlayer viewer : viewers(level)) {
                    Research.observe(viewer, made);
                }
            }
            if (flask.burst()) {
                burst(level);
            }
            dirty = true;
        }
        boolean lit = flask.flame() > 0;
        if (state.hasProperty(LabBenchBlock.LIT) && state.getValue(LabBenchBlock.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(LabBenchBlock.LIT, lit), Block.UPDATE_ALL);
        }
        if (dirty && level.getGameTime() % SYNC_TICKS == 0) {
            dirty = false;
            changed();
        }
    }

    /** The players looking at this bench's screen. */
    private List<ServerPlayer> viewers(ServerLevel level) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.containerMenu instanceof LabBenchMenu menu && menu.pos().equals(worldPosition)) {
                out.add(player);
            }
        }
        return out;
    }

    public Flask.Tuning tuning() {
        int haste = strength(LabRune.HASTE);
        int tempering = strength(LabRune.TEMPERING);
        return new Flask.Tuning(haste > 0 ? HolderRune.at(LabRune.FASTER, haste) : 1,
                tempering > 0 ? HolderRune.at(LabRune.CALMER, tempering) : 1);
    }

    // ---- What the screen reads ------------------------------------------------------------------------------------------

    public Flask flask() {
        return flask;
    }

    public @Nullable ResearchTopic topic() {
        return topic;
    }

    public @Nullable ExperimentPlan plan() {
        return topic == null || level == null ? null : ExperimentPlan.of(topic, level.isClientSide());
    }

    public @Nullable Essence formed() {
        return formed;
    }

    public long formedAt() {
        return formedAt;
    }

    public int outcome() {
        return outcome;
    }

    public long outcomeAt() {
        return outcomeAt;
    }

    public boolean hasFlask() {
        return items.get(FLASK).is(Items.GLASS_BOTTLE);
    }

    /** The essence the jars around can give, per essence (mB). */
    public InkSupply supply() {
        return InkSupply.around(level, worldPosition, facing());
    }

    /** The jars whose drops still stream into the flask, or none. */
    public List<JarReach.Draw> draws() {
        return level != null && level.getGameTime() - drawnAt <= STREAM_TICKS ? draws : List.of();
    }

    public long sinceDrawn() {
        return level == null ? Long.MAX_VALUE : level.getGameTime() - drawnAt;
    }

    /** Whether the flask holds the plan's compound, enough of it and pure enough (the sample is checked apart). */
    public boolean ready() {
        ExperimentPlan plan = plan();
        return plan != null && flask.amount(plan.target()) >= plan.amount() - 0.05F && flask.purity(plan.target()) >= plan.purity() - 0.005F;
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(LabBenchBlock.FACING) ? state.getValue(LabBenchBlock.FACING) : Direction.NORTH;
    }

    // ---- Actions from the screen ----------------------------------------------------------------------------------------

    /** Opens a topic of the journal; only an empty flask can change its topic. */
    public void select(ServerPlayer player, @Nullable ResearchTopic chosen) {
        if (chosen != null && (!chosen.lab() || !Guide.server().canResearch(chosen, Research.of(player)))) {
            return;
        }
        if (!flask.isEmpty() && chosen != topic) {
            message(player, Component.translatable("gui.hextale.lab_bench.say.pour_first"), ResearchMessagePayload.Tone.BAD);
            return;
        }
        topic = chosen;
        changed();
    }

    /** A drop of an essence from the jars around falls into the flask. */
    public void drop(ServerPlayer player, Essence essence) {
        if (!(level instanceof ServerLevel server) || !hasFlask()) {
            return;
        }
        InkSupply supply = supply();
        if (supply.available()[essence.ordinal()] < DROP_MB) {
            return;
        }
        if (!flask.drop(essence)) {
            message(player, Component.translatable("gui.hextale.lab_bench.say.full"), ResearchMessagePayload.Tone.BAD);
            return;
        }
        int dam = strength(LabRune.DAM);
        Map<EssenceTankBlockEntity, Essence> drawn;
        if (dam > 0 && server.getRandom().nextFloat() < HolderRune.at(LabRune.FREE_DROP, dam)) {
            drawn = Map.of();
            ToolEffects.puff(server, flaskPosition(), LabRune.DAM.rune(), 5, 0.12);
        } else {
            int[] ink = new int[Essence.COUNT];
            ink[essence.ordinal()] = DROP_MB;
            drawn = supply.spend(ink);
        }
        if (!drawn.isEmpty()) {
            if (server.getGameTime() - drawnAt > STREAM_TICKS) {
                server.playSound(null, worldPosition, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.35F, 1.3F);
            }
            draws = drawn.keySet().stream().map(jar -> new JarReach.Draw(jar.getBlockPos(), essence)).toList();
            drawnAt = server.getGameTime();
        }
        server.playSound(null, worldPosition, ModSounds.LAB_DROP.get(), SoundSource.BLOCKS, 0.6F, 0.9F + server.getRandom().nextFloat() * 0.25F);
        changed();
    }

    public void setFlame(int flame) {
        if (flame != flask.flame()) {
            boolean lighting = flask.flame() == 0 && flame > 0;
            flask.setFlame(flame);
            if (lighting && level != null) {
                level.playSound(null, worldPosition, ModSounds.CHEM_BURNER_IGNITE.get(), SoundSource.BLOCKS, 0.4F, 1.3F);
            }
            changed();
        }
    }

    /** The vent lets the pressure out with a hiss of vapour. */
    public void vent() {
        if (!(level instanceof ServerLevel server) || flask.pressure() <= 0 && flask.isEmpty()) {
            return;
        }
        flask.vent();
        Vec3 at = flaskPosition().add(0, 0.25, 0);
        server.sendParticles(ModParticles.vapour(ModParticles.STEAM), at.x, at.y, at.z, 10, 0.05, 0.05, 0.05, 0.03);
        server.sendParticles(ModParticles.wisp(flask.color()), at.x, at.y, at.z, 4, 0.08, 0.08, 0.08, 0.01);
        server.playSound(null, worldPosition, ModSounds.LAB_VENT.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
        changed();
    }

    /** Pours the flask out (into the bench's sink): everything in it is lost. */
    public void pour() {
        if (level == null || flask.isEmpty()) {
            return;
        }
        flask.pour();
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.7F, 0.9F);
        changed();
    }

    /**
     * The flask holds what the topic asks and the sample lies on the bench: the flask is sealed with the sample in it and
     * the experiment written out as Research Notes. Radiance pays experience.
     */
    public void seal(ServerPlayer player) {
        ExperimentPlan plan = plan();
        if (!(level instanceof ServerLevel server) || plan == null || !ready()) {
            return;
        }
        ItemStack sample = items.get(SAMPLE);
        if (!plan.sampleFits(sample)) {
            message(player, Component.translatable("gui.hextale.lab_bench.say.sample"), ResearchMessagePayload.Tone.BAD);
            return;
        }
        if (plan.needsSample()) {
            sample.shrink(1);
        }
        ResearchTopic done = plan.topic();
        flask.pour();
        deliver(player, ResearchNotesItem.of(done));
        outcome = OUTCOME_SEALED;
        outcomeAt = server.getGameTime();
        Vec3 at = flaskPosition();
        int radiance = strength(LabRune.RADIANCE);
        if (radiance > 0) {
            ExperienceOrb.award(server, at.add(0, 0.4, 0), HolderRune.at(LabRune.EXPERIENCE, radiance));
            ToolEffects.puff(server, at.add(0, 0.3, 0), LabRune.RADIANCE.rune(), 8, 0.25);
        }
        server.sendParticles(ModParticles.glint(plan.target().color()), at.x, at.y + 0.1, at.z, 16, 0.15, 0.15, 0.15, 0.03);
        server.sendParticles(ModParticles.ring(plan.target().color()), at.x, at.y + 0.1, at.z, 0, 0.7, 0, 0, 1);
        server.sendParticles(ModParticles.glint(ModParticles.HEX_LIGHT), at.x, at.y + 0.2, at.z, 6, 0.08, 0.1, 0.08, 0.03);
        server.playSound(null, worldPosition, ModSounds.LAB_SEAL.get(), SoundSource.BLOCKS, 0.9F, 1.0F);
        message(player, Component.translatable("gui.hextale.lab_bench.say.sealed",
                Component.translatable(done.translationKey()).withColor(done.color())), ResearchMessagePayload.Tone.WIN);
        changed();
    }

    /** The pressure won: the glass shatters (Heat Eater may vent it at the last moment). */
    private void burst(ServerLevel server) {
        Vec3 at = flaskPosition();
        int heatEater = strength(LabRune.HEAT_EATER);
        if (heatEater > 0 && server.getRandom().nextFloat() < HolderRune.at(LabRune.CAUGHT, heatEater)) {
            flask.vent();
            ToolEffects.puff(server, at.add(0, 0.2, 0), LabRune.HEAT_EATER.rune(), 14, 0.2);
            server.playSound(null, worldPosition, ModSounds.LAB_VENT.get(), SoundSource.BLOCKS, 0.8F, 0.8F);
            outcome = OUTCOME_CAUGHT;
            outcomeAt = server.getGameTime();
            return;
        }
        int color = flask.color();
        flask.shatter();
        com.pockyl.hextale.thevoid.Thinning.add(server, worldPosition, com.pockyl.hextale.thevoid.Thinning.LAB_BURST, com.pockyl.hextale.thevoid.Thinning.Source.LAB_BURST);
        items.get(FLASK).shrink(1);
        outcome = OUTCOME_BURST;
        outcomeAt = server.getGameTime();
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()), at.x, at.y, at.z, 24, 0.12, 0.12, 0.12, 0.15);
        server.sendParticles(ModParticles.drop(color), at.x, at.y, at.z, 24, 0.15, 0.1, 0.15, 0.15);
        server.sendParticles(ModParticles.wisp(color), at.x, at.y, at.z, 8, 0.2, 0.15, 0.2, 0.02);
        server.sendParticles(ModParticles.vapour(ModParticles.SOOT), at.x, at.y + 0.1, at.z, 8, 0.15, 0.1, 0.15, 0.03);
        server.playSound(null, worldPosition, ModSounds.LAB_BURST.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        server.playSound(null, worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.8F, 1.1F);
        for (ServerPlayer viewer : viewers(server)) {
            message(viewer, Component.translatable("gui.hextale.lab_bench.say.burst"), ResearchMessagePayload.Tone.BAD);
        }
    }

    /** Where the flask's belly is, in the world. */
    public Vec3 flaskPosition() {
        return TableGeometry.toWorld(worldPosition, facing(), LabBenchBlock.FLASK);
    }

    /** The flask's mouth, where the streams from the jars arrive. */
    public Vec3 mouthPosition() {
        return TableGeometry.toWorld(worldPosition, facing(), LabBenchBlock.MOUTH);
    }

    private void deliver(ServerPlayer player, ItemStack notes) {
        ItemStack older = items.get(OUTPUT);
        items.set(OUTPUT, notes);
        if (!older.isEmpty() && !player.getInventory().add(older)) {
            player.drop(older, false);
        }
    }

    private static void message(ServerPlayer player, Component text, ResearchMessagePayload.Tone tone) {
        ModNetwork.sendToPlayer(player, new ResearchMessagePayload(text, tone));
    }

    // ---- Runes --------------------------------------------------------------------------------------------------------

    public int strength(LabRune rune) {
        return runes.strength(rune.rune());
    }

    public PickaxeRunes runes() {
        return runes;
    }

    public void setRunes(PickaxeRunes runes) {
        this.runes = runes;
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- Container ----------------------------------------------------------------------------------------------------

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hextale.lab_bench");
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
        return new LabBenchMenu(containerId, inventory, this, worldPosition);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);
        changed();
    }

    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case FLASK -> stack.is(Items.GLASS_BOTTLE);
            case SAMPLE -> !stack.is(ModItems.RESEARCH_NOTES.get());
            default -> false;
        };
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM : TOP;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return accepts(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == OUTPUT;
    }

    // ---- Saving -------------------------------------------------------------------------------------------------------

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
        super.loadAdditional(input.tag(), input.registries());
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input.tag(), items, input.registries());
        flask.load(input.childOrEmpty("flask"));
        topic = input.read("topic", ResearchTopics.CODEC).filter(ResearchTopic::lab).orElse(null);
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
        outcome = input.getIntOr("outcome", OUTCOME_NONE);
        outcomeAt = input.getLongOr("outcome_at", -1000);
        formed = Essence.byId(input.getStringOr("formed", ""));
        formedAt = input.getLongOr("formed_at", -1000);
        draws = JarReach.read(input.getIntArray("draws").orElse(new int[0]));
        drawnAt = input.getLongOr("drawn_at", -1000);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
        super.saveAdditional(output.tag(), output.registries());
        ContainerHelper.saveAllItems(output.tag(), items, output.registries());
        flask.save(output.child("flask"));
        if (topic != null) {
            output.store("topic", ResearchTopics.CODEC, topic);
        }
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
        output.putInt("outcome", outcome);
        output.putLong("outcome_at", outcomeAt);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.LAB_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.LAB_RUNES.get(), runes);
        }
    }

    @Override
    public void removeComponentsFromTag(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, net.minecraft.core.RegistryAccess.EMPTY);
        super.removeComponentsFromTag(output.tag());
        output.discard("runes");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = saveCustomOnly(registries);
        if (formed != null) {
            tag.putString("formed", formed.id());
            tag.putLong("formed_at", formedAt);
        }
        if (level != null && level.getGameTime() - drawnAt <= STREAM_TICKS) {
            tag.putIntArray("draws", JarReach.write(draws));
            tag.putLong("drawn_at", drawnAt);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
