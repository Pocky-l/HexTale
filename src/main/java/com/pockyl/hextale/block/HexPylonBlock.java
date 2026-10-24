package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.transport.PylonKind;
import com.pockyl.hextale.transport.PylonLinking;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * A hex pylon: a fluted column of magic stone with a hextech gem floating over its crown ({@link HexPylonBlockEntity}
 * does the work). Right click starts a link, then right click a container's face or a relay ({@link PylonLinking});
 * sneak + right click with an empty hand takes all its links away.
 */
public class HexPylonBlock extends BaseEntityBlock {
    private static final VoxelShape GATHERING_SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 2, 11), Block.box(2, 2, 2, 14, 6, 14));
    private static final VoxelShape SENDING_SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 2, 15), Block.box(4, 2, 4, 12, 7, 12));
    private static final VoxelShape RELAY_SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 1, 13), Block.box(5, 1, 5, 11, 15, 11));
    private static final VoxelShape SIPHON_SHAPE = Shapes.or(Block.box(5, 0, 5, 11, 4, 11), Block.box(4, 4, 4, 12, 11, 12),
            Block.box(5, 11, 5, 11, 15, 11));
    private static final VoxelShape SORTING_SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 3, 15), Block.box(3, 3, 3, 13, 14, 13));

    private final PylonKind kind;

    public HexPylonBlock(PylonKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public PylonKind kind() {
        return kind;
    }

    /** With a block in hand, a right click places it as on any block; anything else falls through to {@link #useWithoutItem}. */
    @Override
    protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(com.pockyl.hextale.registry.ModItems.PYLON_FILTER.get()) || stack.is(com.pockyl.hextale.registry.ModItems.ESSENCE_STICKER.get())) {
            if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HexPylonBlockEntity pylon) {
                fit(stack, pylon, player);
            }
            return InteractionResult.SUCCESS;
        }
        return stack.getItem() instanceof net.minecraft.world.item.BlockItem ? InteractionResult.PASS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /**
     * A filter goes into an item pylon (gathering, sending, relay), a sticker onto an essence carrier (siphon, relay); the
     * one already there comes back to the player.
     */
    private static void fit(net.minecraft.world.item.ItemStack stack, HexPylonBlockEntity pylon, Player player) {
        boolean filter = stack.is(com.pockyl.hextale.registry.ModItems.PYLON_FILTER.get());
        PylonKind kind = pylon.kind();
        if (filter ? kind == PylonKind.SIPHON : kind != PylonKind.SIPHON && kind != PylonKind.RELAY) {
            player.displayClientMessage(Component.translatable(filter ? "item.hextale.pylon_filter.not_here" : "item.hextale.essence_sticker.not_here"), true);
            return;
        }
        if (filter) {
            net.minecraft.world.item.ItemStack old = pylon.setFilter(stack);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (!old.isEmpty()) {
                com.pockyl.hextale.item.PylonFilterItem.give(player, old);
            }
            player.displayClientMessage(Component.translatable("item.hextale.pylon_filter.set"), true);
        } else {
            com.pockyl.hextale.rune.Essence essence = com.pockyl.hextale.item.EssenceStickerItem.essence(stack);
            if (essence == null) {
                player.displayClientMessage(Component.translatable("item.hextale.essence_sticker.blank"), true);
                return;
            }
            com.pockyl.hextale.rune.Essence old = pylon.setSticker(essence);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (old != null) {
                com.pockyl.hextale.item.PylonFilterItem.give(player,
                        com.pockyl.hextale.item.EssenceStickerItem.of(com.pockyl.hextale.registry.ModItems.ESSENCE_STICKER.get(), old));
            }
            player.displayClientMessage(Component.translatable("item.hextale.essence_sticker.set", Component.translatable(essence.translationKey())), true);
        }
        pylon.getLevel().playSound(null, pylon.getBlockPos(), net.minecraft.sounds.SoundEvents.ITEM_FRAME_ADD_ITEM,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.7F, 1.2F);
    }

    /**
     * Right click (with an empty hand or any item but a block; vanilla offers the block the use after the item): start a link from here, or
     * give it up when one from here is going. Sneak + right click with an empty hand: take every link away.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof HexPylonBlockEntity pylon)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            if (pylon.clearLinks()) {
                player.displayClientMessage(Component.translatable("block.hextale.hex_pylon.link.cleared"), true);
                level.playSound(null, pos, com.pockyl.hextale.registry.ModSounds.PYLON_UNLINK.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            } else {
                player.displayClientMessage(status(pylon), true);
            }
            return InteractionResult.SUCCESS;
        }
        PylonLinking.start((ServerPlayer) player, pylon);
        return InteractionResult.SUCCESS;
    }

    /** What the pylon does now, in one line: reach, pace, amount, links. */
    public static Component status(HexPylonBlockEntity pylon) {
        String seconds = String.format(Locale.ROOT, "%.1f", pylon.interval() / 20.0F);
        return switch (pylon.kind()) {
            case GATHERING -> Component.translatable("block.hextale.hex_pylon.status.gathering", pylon.radius(), seconds, pylon.amount(),
                    pylon.links().size(), pylon.maxLinks());
            case SENDING -> Component.translatable("block.hextale.hex_pylon.status.sending", pylon.amount(), seconds, pylon.range(),
                    pylon.links().size(), pylon.maxLinks());
            case RELAY -> Component.translatable("block.hextale.hex_pylon.status.relay", pylon.range(), pylon.links().size(),
                    pylon.maxLinks());
            case SIPHON -> Component.translatable("block.hextale.hex_pylon.status.siphon", pylon.essenceAmount(), seconds, pylon.range(),
                    pylon.links().size(), pylon.maxLinks());
            case SORTING -> Component.translatable("block.hextale.hex_pylon.status.sorting", pylon.amount(), seconds, pylon.radius(),
                    pylon.links().size(), pylon.maxLinks());
        };
    }

    /** A container placed or broken under it: look again at once. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level.getBlockEntity(pos) instanceof HexPylonBlockEntity pylon) {
            pylon.wake();
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (kind) {
            case GATHERING -> GATHERING_SHAPE;
            case SENDING -> SENDING_SHAPE;
            case RELAY -> RELAY_SHAPE;
            case SIPHON -> SIPHON_SHAPE;
            case SORTING -> SORTING_SHAPE;
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** The one who places a pylon owns it: it delivers on their behalf. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof HexPylonBlockEntity pylon) {
            pylon.setOwner(player);
        }
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HexPylonBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return createTickerHelper(type, ModBlockEntities.HEX_PYLON.get(), (l, p, s, pylon) -> PylonLinking.clientTick(l, pylon));
        }
        return kind == PylonKind.RELAY ? null : createTickerHelper(type, ModBlockEntities.HEX_PYLON.get(), HexPylonBlockEntity::serverTick);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        PylonLinking.forget(level, pos);
    }
}
