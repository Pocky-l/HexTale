package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.common.util.TriState;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.item.RiftLinkCrystalItem;
import com.pockyl.hextale.menu.VaultMenu;
import com.pockyl.hextale.multiblock.MultiblockPartBlockEntity;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;

import org.jspecify.annotations.Nullable;

/**
 * The Hex Vault: a heavy lock of brass and walnut that belongs between the pillars of its gate ({@code multiblock.VaultGate}).
 * Set there and woken with Hextech Dust it becomes the structure's controller and rises into the floating door ({@link
 * #FORMED}). A click opens its screen, whatever is in the hand: items go in on that screen, never by the click itself. Unstable
 * Hextech used on it while sneaking becomes a link crystal that ties a Storage Terminal to it. Hoppers, pipes and pylons use it as
 * a normal inventory through the door. A comparator reads how full it is. Out of its gate it sleeps: items can still be taken out
 * by hand, nothing else works.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public class HexVaultBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Standing in a finished gate. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public HexVaultBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FORMED) ? com.pockyl.hextale.multiblock.VaultGate.slabFacing(state.getValue(FACING)) : ModelShapes.of(state);
    }

    /** In its gate the block has left its cell: the renderer draws the door up in the air. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HexVaultBlockEntity(pos, state);
    }

    /** Only the door moves by itself, and only on clients: the server's work comes in scheduled ticks. */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() && state.getValue(FORMED)
                ? createTickerHelper(type, com.pockyl.hextale.registry.ModBlockEntities.HEX_VAULT.get(), HexVaultBlockEntity::clientTick) : null;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof HexVaultBlockEntity vault) {
            vault.tick(level);
        }
    }

    /** A low drone now and then while there is anything inside, and motes round the hexagon or, asleep, the block. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof HexVaultBlockEntity vault) || vault.used() <= 0) {
            return;
        }
        if (random.nextInt(90) == 0) {
            level.playLocalSound(pos, ModSounds.VAULT_HUM.get(), SoundSource.BLOCKS, 0.2F, 0.9F + random.nextFloat() * 0.2F, false);
        }
        if (random.nextInt(3) == 0) {
            double angle = random.nextDouble() * Math.PI * 2;
            boolean formed = state.getValue(FORMED);
            double reach = formed ? 2.4 : 0.8;
            double across = Math.cos(angle) * reach;
            double up = (formed ? HexVaultBlockEntity.DOOR_HEIGHT : 0.5) + Math.sin(angle) * reach;
            boolean alongX = state.getValue(FACING).getAxis() == Direction.Axis.Z;
            level.addParticle(ModParticles.glint(HexVaultBlockEntity.HEXTECH), pos.getX() + 0.5 + (alongX ? across : 0), pos.getY() + up,
                    pos.getZ() + 0.5 + (alongX ? 0 : across), 0, 0.01, 0);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level instanceof ServerLevel && level.getBlockEntity(pos) instanceof HexVaultBlockEntity vault ? vault.contents().signal() : 0;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.getAbilities().instabuild && level.getBlockEntity(pos) instanceof HexVaultBlockEntity vault) {
            vault.brokenInCreative();
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Sneaking with an item never reaches a block by itself: Unstable Hextech has to, on the block or anywhere on its door. */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(ModItems.UNSTABLE_HEXTECH.get())) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ShapePartBlock) {
            state = level.getBlockState(ShapePartBlock.device(state, pos));
        } else if (level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part && part.controllerNear()) {
            state = level.getBlockState(part.controller());
        }
        if (state.getBlock() instanceof HexVaultBlock) {
            event.setUseBlock(TriState.TRUE);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && player instanceof ServerPlayer server && level.getBlockEntity(pos) instanceof HexVaultBlockEntity vault) {
            VaultMenu.open(server, vault);
        }
        return InteractionResult.SUCCESS;
    }

    /** Whatever is held stays in the hand and the screen opens. Only Unstable Hextech, used while sneaking, does a thing of its own. */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (!player.isSecondaryUseActive() || !stack.is(ModItems.UNSTABLE_HEXTECH.get())) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof HexVaultBlockEntity vault) {
            press(server, vault, stack, player);
        }
        return net.minecraft.world.ItemInteractionResult.SUCCESS;
    }

    /** Unstable Hextech used on the vault becomes a link crystal that remembers it. */
    private static void press(ServerLevel level, HexVaultBlockEntity vault, ItemStack orb, Player player) {
        ItemStack crystal = RiftLinkCrystalItem.rememberingVault(vault.id(), vault.getBlockPos());
        orb.consume(1, player);
        if (!player.getInventory().add(crystal)) {
            player.drop(crystal, false);
        }
        level.playSound(null, vault.getBlockPos(), ModSounds.RIFT_LINK.get(), SoundSource.BLOCKS, 0.6F, 1.3F);
        Vec3 centre = Vec3.atCenterOf(vault.getBlockPos()).add(0, 1.3, 0);
        level.sendParticles(ModParticles.glint(RiftGateBlock.RIFT), centre.x, centre.y, centre.z, 14, 0.4, 0.4, 0.4, 0.03);
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        com.pockyl.hextale.port.Removal.before(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, movedByPiston);
        com.pockyl.hextale.port.Removal.after(this, state, level, pos, newState, movedByPiston);
    }

    @Override
    protected com.mojang.serialization.MapCodec<HexVaultBlock> codec() {
        return simpleCodec(HexVaultBlock::new);
    }
}
