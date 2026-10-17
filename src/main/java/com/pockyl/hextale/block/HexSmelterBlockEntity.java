package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import com.pockyl.hextale.essence.EssenceFlaskItem;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.menu.HexSmelterMenu;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.SmelterRune;

import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * The Hex Smelter's contents. It smelts whatever a furnace smelts, twice as fast, and burns no fuel: its heat is the
 * fire in essence. Fiery essence (any essence with fire in its make-up) comes in through pipes at the back and the sides,
 * from bottles, or is drawn by the smelter itself out of Essence Jars around it ({@link JarReach}) while it has
 * something to smelt; each millibucket gives as much heat as its share of fire. An item needs {@value #HEAT_PER_ITEM}
 * heat. Runes burned into the smelter at the altar ({@link SmelterRune}) cut the heat, speed it up or add pieces; they
 * stay with the block and go back onto its item when it is broken.
 */
public class HexSmelterBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, EssenceNetwork.Receiver {
    public static final int INPUT = 0;
    public static final int OUTPUT = 1;
    public static final int BOTTLE_IN = 2;
    public static final int BOTTLE_OUT = 3;
    public static final int SIZE = 4;
    public static final int COOK_TICKS = 100;
    public static final int HEAT_PER_ITEM = 8;
    public static final int CAPACITY = 256;
    /** While smelting it tops its tank up from the jars around: {@value #DRAW_MB} mB every {@value #DRAW_TICKS} ticks below {@value #DRAW_BELOW} mB. */
    public static final int DRAW_MB = 4;
    public static final int DRAW_TICKS = 10;
    public static final int DRAW_BELOW = 32;
    /** The stream from a jar stays up this long after the last draw, even when the smelter rests. */
    public static final int STREAM_LINGER = 40;
    /** Heat is kept in hundredths: a millibucket of half-fire essence gives 50. */
    private static final int HEAT_UNIT = 100;
    private static final int[] TOP = {INPUT};
    private static final int[] SIDES = {INPUT, BOTTLE_IN};
    private static final int[] BOTTOM = {OUTPUT, BOTTLE_OUT};
    private static final TagKey<Item> RAW_MATERIALS = Tags.Items.RAW_MATERIALS;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private final EssenceTank tank = new EssenceTank(CAPACITY);
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    /** Heat drawn from the tank and not yet spent, in hundredths. */
    private int heat;
    private int progress;
    private float experience;
    /** Why it waits, for the screen: "" while working or idle. */
    private String blocked = "";
    /** Client side: smooth progress between syncs. */
    public float clientProgress;
    /** The jar it draws from (synced, so the stream shows); null when its essence comes by pipe or bottle. */
    private @Nullable BlockPos source;
    /** Game time of the last draw from a jar (synced). */
    private long drewAt = -1000;
    /** Client side: how far the stream has grown out of the jar and run into the smelter (see {@code JarStreamFx}). */
    public float streamHead;
    public float streamTail;
    public float streamHeadO;
    public float streamTailO;
    /** Client side: the jar the stream shows and its colour, kept while the stream runs out after the draw stopped. */
    public @Nullable BlockPos streamJar;
    public int streamColor = -1;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> cookTicks();
                default -> heat;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public HexSmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEX_SMELTER.get(), pos, state);
    }

    public EssenceTank tank() {
        return tank;
    }

    public PickaxeRunes runes() {
        return runes;
    }

    public int heat() {
        return heat;
    }

    public String blocked() {
        return blocked;
    }

    public ItemStack input() {
        return items.get(INPUT);
    }

    // ---- Runes --------------------------------------------------------------------------------------------------------

    private int strength(SmelterRune rune) {
        return runes.strength(rune.rune());
    }

    /** Ticks per item with the Bellows. */
    public int cookTicks() {
        return Math.max(10, Math.round(COOK_TICKS / SmelterRune.speed(strength(SmelterRune.BELLOWS))));
    }

    /** Heat per item (hundredths) with Tempering. */
    public int heatPerItem() {
        return Math.round(HEAT_PER_ITEM * HEAT_UNIT * SmelterRune.heatShare(strength(SmelterRune.TEMPERING)));
    }

    // ---- Heat ---------------------------------------------------------------------------------------------------------

    /** The share of fire in an essence mix (0-1); unknown essence gives none. */
    public static double fire(EssenceMix mix) {
        if (mix.total() <= 0) {
            return 0;
        }
        double fire = 0;
        for (Map.Entry<Essence, Integer> entry : mix.asMap().entrySet()) {
            fire += entry.getKey().share(Essence.FIRE) * entry.getValue();
        }
        return fire / mix.total();
    }

    /** Fiery essence: every essence in it carries fire, and nothing unknown. */
    public static boolean fiery(EssenceMix mix) {
        if (mix.isEmpty() || mix.unknown() > 0) {
            return false;
        }
        for (Essence essence : mix.asMap().keySet()) {
            if (essence.share(Essence.FIRE) <= 0) {
                return false;
            }
        }
        return true;
    }

    /** Pipes bring fiery essence in at the back and the sides. */
    @Override
    public int receive(Direction side, EssenceMix offer, boolean simulate) {
        if (!side.getAxis().isHorizontal() || side == getBlockState().getValue(HexSmelterBlock.FACING) || !fiery(offer)) {
            return 0;
        }
        int taken = tank.fill(offer, simulate);
        if (taken > 0 && !simulate) {
            source = null;
            setChanged();
        }
        return taken;
    }

    /**
     * Tops the tank up out of the nearest jar of fiery essence within {@link JarReach#RADIUS} (a Creative Essence Jar
     * gives pure fire). Only every {@value #DRAW_TICKS} ticks and only when low, so idle smelters never look around.
     */
    private void drawFromJars(ServerLevel level) {
        if (Math.floorMod(level.getGameTime() + worldPosition.asLong(), DRAW_TICKS) != 0
                || tank.amount() >= DRAW_BELOW || tank.space() < DRAW_MB) {
            return;
        }
        for (EssenceTankBlockEntity jar : JarReach.around(level, worldPosition)) {
            EssenceMix drawn;
            if (jar.isCreative()) {
                drawn = EssenceMix.pure(Essence.FIRE, DRAW_MB);
            } else if (fiery(jar.tank().contents())) {
                drawn = jar.tank().drain(DRAW_MB, false);
                jar.setChanged();
            } else {
                continue;
            }
            tank.fill(drawn, false);
            if (!jar.getBlockPos().equals(source) || level.getGameTime() - drewAt > STREAM_LINGER) {
                // A new stream: the essence is pulled out of the jar with a soft siphoning whoosh.
                level.playSound(null, worldPosition, ModSounds.ESSENCE_DRAW.get(), SoundSource.BLOCKS, 0.5F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            }
            source = jar.getBlockPos();
            drewAt = level.getGameTime();
            setChanged();
            return;
        }
    }

    /** The jar the stream comes from, or null when there is no stream now. */
    public @Nullable BlockPos streamSource() {
        if (source == null || level == null) {
            return null;
        }
        boolean working = getBlockState().getValue(HexSmelterBlock.LIT) || level.getGameTime() - drewAt <= STREAM_LINGER;
        return working ? source : null;
    }

    /** The essence the stream shows: pure fire from a creative jar, else whatever the jar holds. */
    public @Nullable Essence streamEssence() {
        return source != null && level != null && level.getBlockEntity(source) instanceof EssenceTankBlockEntity jar && jar.isCreative()
                ? Essence.FIRE : null;
    }

    /** Draws essence until the heat covers an item; false when the tank runs dry first. */
    private boolean stoke(int needed) {
        boolean drew = false;
        while (heat < needed && !tank.isEmpty()) {
            EssenceMix drop = tank.drain(1, false);
            heat += (int) Math.round(fire(drop) * HEAT_UNIT);
            drew = true;
        }
        if (drew) {
            setChanged();
        }
        return heat >= needed;
    }

    // ---- Ticking ------------------------------------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, HexSmelterBlockEntity smelter) {
        boolean changed = smelter.pourBottle();
        boolean working = smelter.smelt((ServerLevel) level);
        if (state.getValue(HexSmelterBlock.LIT) != working) {
            level.setBlock(pos, state.setValue(HexSmelterBlock.LIT, working), Block.UPDATE_ALL);
        }
        if (changed) {
            smelter.setChanged();
        }
    }

    /** Pours a bottle of fiery essence into the tank and keeps the empty bottle. */
    private boolean pourBottle() {
        ItemStack bottle = items.get(BOTTLE_IN);
        if (!bottle.is(com.pockyl.hextale.registry.ModItems.ESSENCE_FLASK.get())) {
            return false;
        }
        EssenceMix inside = EssenceFlaskItem.contents(bottle);
        ItemStack out = items.get(BOTTLE_OUT);
        boolean room = out.isEmpty() || out.is(Items.GLASS_BOTTLE) && out.getCount() < out.getMaxStackSize();
        if (!fiery(inside) || tank.space() < inside.total() || !room) {
            return false;
        }
        tank.fill(inside, false);
        source = null;
        bottle.shrink(1);
        if (out.isEmpty()) {
            items.set(BOTTLE_OUT, new ItemStack(Items.GLASS_BOTTLE));
        } else {
            out.grow(1);
        }
        level.playSound(null, worldPosition, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 0.6F, 0.9F);
        return true;
    }

    private Optional<RecipeHolder<SmeltingRecipe>> recipe(ServerLevel level) {
        ItemStack input = input();
        return input.isEmpty() ? Optional.empty()
                : level.recipeAccess().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level);
    }

    /** One tick of smelting; true while it works. */
    private boolean smelt(ServerLevel level) {
        Optional<RecipeHolder<SmeltingRecipe>> found = recipe(level);
        if (found.isEmpty()) {
            return idle("");
        }
        SingleRecipeInput input = new SingleRecipeInput(input());
        ItemStack result = found.get().value().assemble(input);
        if (result.isEmpty()) {
            return idle("");
        }
        ItemStack out = items.get(OUTPUT);
        int extra = ore(input()) ? 1 : 0;
        if (!out.isEmpty() && (!ItemStack.isSameItemSameComponents(out, result)
                || out.getCount() + result.getCount() + extra > out.getMaxStackSize())) {
            return idle("output_full");
        }
        int needed = heatPerItem();
        drawFromJars(level);
        if (!stoke(needed)) {
            // The item stays where it got to: the smelter only cools down.
            blocked = "no_heat";
            return false;
        }
        blocked = "";
        progress++;
        if (progress < cookTicks()) {
            return true;
        }
        progress = 0;
        heat -= needed;
        int count = result.getCount();
        if (extra > 0 && level.getRandom().nextFloat() < SmelterRune.extra(strength(SmelterRune.FORGE))) {
            count++;
        }
        if (out.isEmpty()) {
            result.setCount(count);
            items.set(OUTPUT, result);
        } else {
            out.grow(count);
        }
        input().shrink(1);
        experience += found.get().value().experience();
        level.playSound(null, worldPosition, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.25F, 0.7F + level.getRandom().nextFloat() * 0.2F);
        setChanged();
        return true;
    }

    private boolean idle(String reason) {
        boolean changed = progress != 0 || !blocked.equals(reason);
        progress = 0;
        blocked = reason;
        if (changed) {
            setChanged();
        }
        return false;
    }

    /** Ores and raw metals: the Forge rune may add a piece. */
    private static boolean ore(ItemStack stack) {
        return stack.is(Tags.Items.ORES) || stack.is(RAW_MATERIALS);
    }

    /** The experience of everything smelted since it was last taken out, as orbs at the player. */
    public void awardExperience(Player player) {
        if (!(level instanceof ServerLevel server) || experience <= 0) {
            return;
        }
        int points = Mth.floor(experience);
        if (server.getRandom().nextFloat() < experience - points) {
            points++;
        }
        experience = 0;
        if (points > 0) {
            ExperienceOrb.award(server, player.position(), points);
        }
        setChanged();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, HexSmelterBlockEntity smelter) {
        smelter.clientProgress = state.getValue(HexSmelterBlock.LIT)
                ? Math.min(1, smelter.clientProgress + 1.0F / smelter.cookTicks()) : 0;
        if (smelter.clientProgress >= 1) {
            smelter.clientProgress = 0;
        }
        smelter.tickStream();
    }

    /** Client: the stream grows out of the jar while it draws and runs into the smelter when it stops. */
    private void tickStream() {
        streamHeadO = streamHead;
        streamTailO = streamTail;
        BlockPos from = streamSource();
        int color = from == null ? -1 : JarReach.color(level, new JarReach.Draw(from, streamEssence()));
        boolean on = color >= 0;
        if (on) {
            streamJar = from;
            streamColor = color;
            if (streamTail > 0) {
                // It had begun to run out: start over from the jar.
                streamHead = 0;
                streamTail = 0;
                streamHeadO = 0;
                streamTailO = 0;
            }
            streamHead = Math.min(1, streamHead + 0.08F);
            JarReach.shed(level, JarReach.mouth(from), gemPosition(worldPosition), color, streamHead, streamTail);
        } else if (streamHead > 0) {
            streamTail = Math.min(streamHead, streamTail + 0.06F);
            if (streamTail >= streamHead) {
                streamHead = 0;
                streamTail = 0;
                streamHeadO = 0;
                streamTailO = 0;
            }
        }
    }

    /** Where the gem floats over the smelter: drawn essence flows into it. */
    public static Vec3 gemPosition(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 15.5 / 16, 0.5);
    }


    // ---- Container ----------------------------------------------------------------------------------------------------

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hextale.hex_smelter");
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
        return new HexSmelterMenu(containerId, inventory, this, data, worldPosition);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    /** What each slot takes. */
    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT -> !EssenceFlaskItem.isFlask(stack);
            case BOTTLE_IN -> stack.is(com.pockyl.hextale.registry.ModItems.ESSENCE_FLASK.get()) && fiery(EssenceFlaskItem.contents(stack));
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
        return slot == OUTPUT || slot == BOTTLE_OUT;
    }

    // ---- Saving: the runes travel with the item -------------------------------------------------------------------------

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.SMELTER_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.SMELTER_RUNES.get(), runes);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        tank.load(input, "essence");
        heat = input.getIntOr("heat", 0);
        progress = input.getIntOr("progress", 0);
        experience = input.getFloatOr("experience", 0);
        blocked = input.getStringOr("blocked", "");
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
        source = input.getLong("source").map(BlockPos::of).orElse(null);
        drewAt = input.getLongOr("drew_at", -1000);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        tank.save(output, "essence");
        output.putInt("heat", heat);
        output.putInt("progress", progress);
        output.putFloat("experience", experience);
        output.putString("blocked", blocked);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
        if (source != null) {
            output.putLong("source", source.asLong());
            output.putLong("drew_at", drewAt);
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
