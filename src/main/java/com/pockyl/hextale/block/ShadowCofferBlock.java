package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import com.pockyl.hextale.port.ValueInput;
import com.pockyl.hextale.port.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

/**
 * A Shadow Coffer: coffers sealed with the same rune share one pocket in the shadow ({@link ShadowPockets}) - put
 * something in one, take it out of another, anywhere. A coffer placed without a seal makes a new one and hands it to the
 * coffers left in hand; sneak + right click a placed coffer with coffers in hand to seal them with its rune. Hoppers and pylons reach the pocket too.
 */
public class ShadowCofferBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public ShadowCofferBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Coffer(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof Coffer coffer && coffer.container() != null) {
            player.openMenu(new SimpleMenuProvider((id, inventory, who) -> ChestMenu.threeRows(id, inventory, coffer.container()),
                    Component.translatable("block.hextale.shadow_coffer")));
            level.playSound(null, pos, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    /** A coffer placed without a seal makes a new one, and the coffers left in hand take it (a crafted pair stays a pair). */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.@Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof Coffer coffer && coffer.container() != null && coffer.pair != null
                && !stack.has(ModDataComponents.COFFER_PAIR.get())) {
            stack.set(ModDataComponents.COFFER_PAIR.get(), coffer.pair);
        }
    }

    /** Sneak + right click with coffers in hand: they take this coffer's seal. */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!stack.is(ModItems.SHADOW_COFFER.get()) || !player.isSecondaryUseActive()) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof Coffer coffer && coffer.container() != null && coffer.pair != null) {
            stack.set(ModDataComponents.COFFER_PAIR.get(), coffer.pair);
            player.displayClientMessage(Component.translatable("block.hextale.shadow_coffer.sealed"), true);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ModParticles.glint(0xB98FE6), pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 10, 0.3, 0.05, 0.3, 0.02);
                server.playSound(null, pos, SoundEvents.ENDER_CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 1.4F);
            }
        }
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    /** A coffer's seal: the pair it belongs to. */
    public static class Coffer extends BlockEntity {
        @Nullable
        private UUID pair;

        public Coffer(BlockPos pos, BlockState state) {
            super(ModBlockEntities.SHADOW_COFFER.get(), pos, state);
        }

        /** The shared pocket (server side); null on a client or before the coffer has a seal. */
        @Nullable
        public Container container() {
            if (!(level instanceof ServerLevel server)) {
                return null;
            }
            if (pair == null) {
                pair = UUID.randomUUID();
                setChanged();
            }
            return ShadowPockets.of(server.getServer()).pocket(pair);
        }

        @Override
        protected void applyImplicitComponents(DataComponentInput components) {
            super.applyImplicitComponents(components);
            pair = components.get(ModDataComponents.COFFER_PAIR.get());
        }

        @Override
        protected void collectImplicitComponents(DataComponentMap.Builder components) {
            super.collectImplicitComponents(components);
            if (pair != null) {
                components.set(ModDataComponents.COFFER_PAIR.get(), pair);
            }
        }

        @Override
        public void removeComponentsFromTag(net.minecraft.nbt.CompoundTag portTag) {
        ValueOutput output = ValueOutput.of(portTag, net.minecraft.core.RegistryAccess.EMPTY);
            super.removeComponentsFromTag(output.tag());
            output.discard("pair");
        }

        @Override
        protected void loadAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueInput input = ValueInput.of(portTag, portRegistries);
            super.loadAdditional(input.tag(), input.registries());
            pair = input.read("pair", UUIDUtil.STRING_CODEC).orElse(null);
        }

        @Override
        protected void saveAdditional(net.minecraft.nbt.CompoundTag portTag, net.minecraft.core.HolderLookup.Provider portRegistries) {
        ValueOutput output = ValueOutput.of(portTag, portRegistries);
            super.saveAdditional(output.tag(), output.registries());
            if (pair != null) {
                output.store("pair", UUIDUtil.STRING_CODEC, pair);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<ShadowCofferBlock> codec() {
        return simpleCodec(ShadowCofferBlock::new);
    }
}
