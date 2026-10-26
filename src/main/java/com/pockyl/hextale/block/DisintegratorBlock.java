package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.research.ItemEssences;
import com.pockyl.hextale.research.ResearchItems;
import com.pockyl.hextale.runework.DisintegratorRune;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.thevoid.Thinning;

import org.jspecify.annotations.Nullable;

/**
 * The Disintegrator: a stone bowl that unmakes whatever is put into it - by hand, hopper, pylon or thrown in - into
 * unknown essence, poured into the jar or pipe under it. One essence point of the thing = 1 mB. The unmaking leaks into
 * the land (the Thinning) twice as much as boiling. Takes runes (Motor, Dam, Radiance, Hush, Lens).
 */
public class DisintegratorBlock extends BaseEntityBlock implements EssenceMachine {
    private static final VoxelShape INSIDE = Block.box(4, 5, 4, 12, 16, 12);
    private static final VoxelShape SHAPE = Shapes.join(Shapes.or(Block.box(1, 0, 1, 15, 11, 15), Block.box(1, 11, 1, 15, 12, 15)),
            INSIDE, BooleanOp.ONLY_FIRST);
    /** Where thrown things are caught, relative to the block. */
    private static final AABB CATCH = new AABB(0.25, 0.3, 0.25, 0.75, 1.4, 0.75);

    public DisintegratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INSIDE;
    }

    @Override
    public boolean isPort(BlockState state, Direction face) {
        return face == Direction.DOWN;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Bowl(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.DISINTEGRATOR.get()) {
            return null;
        }
        return level.isClientSide() ? (BlockEntityTicker<T>) (BlockEntityTicker<Bowl>) Bowl::clientTick
                : (BlockEntityTicker<T>) (BlockEntityTicker<Bowl>) Bowl::serverTick;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof Bowl bowl)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            int before = stack.getCount();
            ItemStack rest = bowl.add(player.getAbilities().instabuild ? stack.copy() : stack);
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, rest);
            }
            if (rest.getCount() == before && !player.getAbilities().instabuild) {
                player.sendOverlayMessage(Component.translatable("block.hextale.disintegrator.full"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Sneaking takes back what is still whole; otherwise the bowl says what it holds. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof Bowl bowl)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (player.isSecondaryUseActive() && !bowl.item.isEmpty()) {
                if (!player.getInventory().add(bowl.item)) {
                    Block.popResource(level, pos.above(), bowl.item);
                }
                bowl.item = ItemStack.EMPTY;
                bowl.progress = 0;
                bowl.setChanged();
            } else {
                player.sendOverlayMessage(Component.translatable("block.hextale.disintegrator.status",
                        bowl.item.isEmpty() ? Component.translatable("block.hextale.disintegrator.nothing") : bowl.item.getHoverName(),
                        bowl.tank.amount(), bowl.tank.capacity()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof Bowl bowl ? bowl.tank.amount() * 15 / bowl.tank.capacity() : 0;
    }

    /** One stack waiting to be unmade, the unknown essence it became, the runes. A hopper fills it from any side. */
    public static class Bowl extends BlockEntity implements Container {
        public static final int CAPACITY = 1000;
        /** The essence it pours down at a time. */
        public static final int FLOW = 50;
        private ItemStack item = ItemStack.EMPTY;
        private final EssenceTank tank = new EssenceTank(CAPACITY);
        private int progress;
        private PickaxeRunes runes = PickaxeRunes.EMPTY;
        /** Client: the unmaking of the current thing, 0-1, for the renderer. */
        public float clientProgress;

        public Bowl(BlockPos pos, BlockState state) {
            super(ModBlockEntities.DISINTEGRATOR.get(), pos, state);
        }

        public ItemStack item() {
            return item;
        }

        public EssenceTank tank() {
            return tank;
        }

        public int strength(DisintegratorRune rune) {
            return runes.strength(rune.rune());
        }

        public int pace() {
            return DisintegratorRune.pace(strength(DisintegratorRune.MOTOR));
        }

        /** Puts as much of {@code stack} in as fits; returns the rest. */
        public ItemStack add(ItemStack stack) {
            if (stack.isEmpty() || !item.isEmpty() && !ItemStack.isSameItemSameComponents(item, stack)) {
                return stack;
            }
            int room = item.isEmpty() ? stack.getMaxStackSize() : item.getMaxStackSize() - item.getCount();
            int moved = Math.min(room, stack.getCount());
            if (moved <= 0) {
                return stack;
            }
            if (item.isEmpty()) {
                item = stack.copyWithCount(moved);
            } else {
                item.grow(moved);
            }
            setChanged();
            ItemStack rest = stack.copy();
            rest.shrink(moved);
            return rest;
        }

        /** Essence one of {@code stack} gives: its essence points, at least 1, more with Dam. */
        public int yield(ItemStack stack) {
            ItemEssences essences = ResearchItems.of(stack);
            int base = essences == null ? 1 : Math.max(1, EssenceMix.boiled(essences, 1).total());
            int dam = strength(DisintegratorRune.DAM);
            return dam <= 0 ? base : Math.round(base * (1 + HolderRune.at(DisintegratorRune.YIELD, dam)));
        }

        static void serverTick(Level level, BlockPos pos, BlockState state, Bowl bowl) {
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            long time = level.getGameTime();
            if (time % 10 == 0) {
                bowl.catchThrown(server);
            }
            if (time % EssenceTankBlockEntity.FLOW_EVERY == 0 && !bowl.tank.isEmpty()) {
                EssenceMix offer = bowl.tank.drain(FLOW, true);
                int moved = EssenceNetwork.push(level, pos, Direction.DOWN, offer);
                if (moved > 0) {
                    bowl.tank.remove(offer.part(moved));
                    bowl.setChanged();
                }
            }
            if (bowl.item.isEmpty()) {
                bowl.progress = 0;
                return;
            }
            int made = bowl.yield(bowl.item);
            if (bowl.tank.space() < made) {
                return;
            }
            if (++bowl.progress < bowl.pace()) {
                return;
            }
            bowl.progress = 0;
            bowl.unmake(server, made);
        }

        private void unmake(ServerLevel level, int made) {
            tank.fill(EssenceMix.unknown(made), false);
            item.shrink(1);
            int hush = strength(DisintegratorRune.HUSH);
            float leak = made * Thinning.BOILED * 2 * (hush <= 0 ? 1 : 1 - HolderRune.at(DisintegratorRune.QUIET, hush));
            Thinning.add(level, worldPosition, leak);
            Vec3 centre = Vec3.atCenterOf(worldPosition).add(0, 0.25, 0);
            RandomSource random = level.getRandom();
            int radiance = strength(DisintegratorRune.RADIANCE);
            if (radiance > 0 && random.nextFloat() < HolderRune.at(DisintegratorRune.EXPERIENCE, radiance)) {
                ExperienceOrb.award(level, centre.add(0, 0.6, 0), 1 + random.nextInt(3));
                level.sendParticles(ModParticles.glint(ModParticles.GOLD), centre.x, centre.y + 0.4, centre.z, 6, 0.2, 0.2, 0.2, 0.02);
            }
            level.sendParticles(ModParticles.wisp(EssenceMix.UNKNOWN_COLOR), centre.x, centre.y, centre.z, 8, 0.15, 0.1, 0.15, 0.01);
            level.sendParticles(ModParticles.ring(0x6A5A7A), centre.x, centre.y - 0.3, centre.z, 1, 0, 0, 0, 0);
            level.playSound(null, worldPosition, ModSounds.NULLSTONE_DRINK.get(), SoundSource.BLOCKS, 0.35F, 0.8F + random.nextFloat() * 0.3F);
            setChanged();
        }

        /** Things thrown into the bowl fall in. */
        private void catchThrown(ServerLevel level) {
            AABB box = CATCH.move(worldPosition);
            for (ItemEntity thrown : level.getEntitiesOfClass(ItemEntity.class, box, ItemEntity::isAlive)) {
                ItemStack rest = add(thrown.getItem());
                if (rest.isEmpty()) {
                    thrown.discard();
                } else {
                    thrown.setItem(rest);
                }
            }
        }

        static void clientTick(Level level, BlockPos pos, BlockState state, Bowl bowl) {
            if (bowl.item.isEmpty()) {
                bowl.clientProgress = 0;
                return;
            }
            bowl.clientProgress = Math.min(0.999F, bowl.clientProgress + 1.0F / bowl.pace());
            RandomSource random = level.getRandom();
            if (random.nextInt(3) == 0) {
                double angle = random.nextDouble() * Math.PI * 2;
                double r = 0.18 + random.nextDouble() * 0.12;
                level.addParticle(ModParticles.wisp(EssenceMix.UNKNOWN_COLOR), pos.getX() + 0.5 + Math.cos(angle) * r, pos.getY() + 0.85,
                        pos.getZ() + 0.5 + Math.sin(angle) * r, -Math.cos(angle) * 0.01, -0.02, -Math.sin(angle) * 0.01);
            }
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        // ---- Container: one stack, for hoppers -----------------------------------------------------------------------

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return item.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return item;
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack out = item.split(count);
            setChanged();
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack out = item;
            item = ItemStack.EMPTY;
            return out;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            item = stack;
            setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(this, player);
        }

        @Override
        public void clearContent() {
            item = ItemStack.EMPTY;
            setChanged();
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            super.preRemoveSideEffects(pos, state);
            if (level != null && !item.isEmpty()) {
                Block.popResource(level, pos, item);
                item = ItemStack.EMPTY;
            }
        }

        @Override
        protected void applyImplicitComponents(DataComponentGetter components) {
            super.applyImplicitComponents(components);
            runes = components.getOrDefault(ModDataComponents.DISINTEGRATOR_RUNES.get(), PickaxeRunes.EMPTY);
        }

        @Override
        protected void collectImplicitComponents(DataComponentMap.Builder components) {
            super.collectImplicitComponents(components);
            if (!runes.isEmpty()) {
                components.set(ModDataComponents.DISINTEGRATOR_RUNES.get(), runes);
            }
        }

        @Override
        public void removeComponentsFromTag(ValueOutput output) {
            super.removeComponentsFromTag(output);
            output.discard("runes");
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            item = input.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            tank.load(input, "tank");
            progress = input.getIntOr("progress", 0);
            runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
            clientProgress = progress / (float) pace();
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            if (!item.isEmpty()) {
                output.store("item", ItemStack.OPTIONAL_CODEC, item);
            }
            tank.save(output, "tank");
            output.putInt("progress", progress);
            if (!runes.isEmpty()) {
                output.store("runes", PickaxeRunes.CODEC, runes);
            }
        }

        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            return saveCustomOnly(registries);
        }

        @Override
        public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
