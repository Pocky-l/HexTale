package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.hextale.menu.VaultMenu;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModSounds;

import org.jspecify.annotations.Nullable;

/**
 * The Hex Terminal: a hexagon panel on the floor or a wall, tied to a Hex Vault by a link crystal, from any dimension. It
 * opens the vault's screen and nothing else: it has no item handler, so a terminal is a person's access and never a way
 * for hoppers or pipes to reach the vault from afar. Every take through it pays a toll of rift essence from jars round it.
 */
public class HexTerminalBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<AttachFace> FACE = EnumProperty.create("face", AttachFace.class, AttachFace.FLOOR, AttachFace.WALL);
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");

    public HexTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.FLOOR).setValue(LINKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE, LINKED);
    }

    /** On a wall it looks away from the wall, on the floor at the one who placed it. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isHorizontal()) {
            return defaultBlockState().setValue(FACE, AttachFace.WALL).setValue(FACING, face);
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return ModelShapes.of(state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HexTerminalBlockEntity(pos, state);
    }

    /** A link crystal does its own work ({@code RiftLinkCrystalItem#useOn}). */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return com.pockyl.hextale.port.Interactions.itemOn(stack.has(ModDataComponents.VAULT_LINK.get()) ? net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION : net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof HexTerminalBlockEntity terminal) {
            if (terminal.vault() == null) {
                player.displayClientMessage(Component.translatable("block.hextale.hex_terminal.not_linked"), true);
            } else if (VaultMenu.open(server, terminal.vault(), VaultMenu.Kind.TERMINAL, pos, InteractionHand.MAIN_HAND, state.getBlock().getName())) {
                level.playSound(null, pos, ModSounds.VAULT_TERMINAL.get(), SoundSource.BLOCKS, 0.35F, 1.0F);
            } else {
                player.displayClientMessage(Component.translatable("block.hextale.hex_terminal.vault_gone"), true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<HexTerminalBlock> codec() {
        return simpleCodec(HexTerminalBlock::new);
    }
}
