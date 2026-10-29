package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import com.pockyl.hextale.entity.GrappleHook;
import com.pockyl.hextale.essence.EssenceMix;
import com.pockyl.hextale.essence.EssenceNetwork;
import com.pockyl.hextale.essence.EssenceTank;
import com.pockyl.hextale.essence.JarReach;
import com.pockyl.hextale.item.RiftLinkCrystalItem;
import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.transport.PylonInsert;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

/**
 * A Lesser Rift Gate: an upright stone-and-brass ring. Two gates are tied by a link with its own id ({@link RiftLinks}):
 * a hextech crystal pressed to one gate becomes a Rift Link Crystal that remembers it, and pressed to a second gate it
 * ties the two. Whatever then passes into the ring - a creature, a player, a thrown item, items from a hopper or a pylon
 * - comes out of the other gate's front, in any dimension. Items that arrive drop into a container under the gate if
 * there is one. Breaking a gate leaves its partner alone ("partner missing": sparks crackle in its empty ring).
 *
 * <p>Every crossing is paid in rift essence drawn from the jars around the gate it leaves through ({@link Gate#afford}):
 * a creature or a player 1 mB, an item an eighth, essence and fluids an eighth per {@value Gate#ESSENCE_PER_TOLL} /
 * {@value Gate#FLUID_PER_TOLL} mB. A gate whose jars are dry lets nothing through and shows it.
 */
public class RiftGateBlock extends BaseEntityBlock implements EssenceMachine {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LINKED = BooleanProperty.create("linked");
    /** Rift violet. */
    public static final int RIFT = 0xB98FE6;
    /** Ticks something that came out of a gate cannot go in again (renewed while it stays in a ring). */
    private static final int COOLDOWN = 20;
    private static final int SEND_EVENT = 1;
    private static final int ARRIVE_EVENT = 2;
    private static final Map<UUID, Long> COOLING = new ConcurrentHashMap<>();
    private static final Map<Direction, VoxelShape> SHAPES = new ConcurrentHashMap<>();
    private static final Map<Direction, VoxelShape> OPENINGS = new ConcurrentHashMap<>();

    public RiftGateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LINKED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LINKED);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /** A box of the north-facing gate (x across the ring, z through it), turned to {@code facing}. */
    private static VoxelShape box(Direction facing, double x0, double y0, double z0, double x1, double y1, double z1) {
        return facing.getAxis() == Direction.Axis.Z ? Block.box(x0, y0, z0, x1, y1, z1) : Block.box(z0, y0, x0, z1, y1, x1);
    }

    private static VoxelShape shape(Direction facing) {
        return SHAPES.computeIfAbsent(facing, f -> Shapes.or(box(f, 0, 0, 4, 16, 3, 12), box(f, 0, 3, 5, 3, 16, 11), box(f, 13, 3, 5, 16, 16, 11),
                box(f, 3, 15, 5, 13, 16, 11)));
    }

    /** The open middle of the ring: what is in it goes through. */
    private static VoxelShape opening(Direction facing) {
        return OPENINGS.computeIfAbsent(facing, f -> box(f, 3, 3, 6, 13, 15, 10));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return opening(state.getValue(FACING));
    }

    /** Pipes join the gate under it, over it and at its posts - not across the open ring. */
    @Override
    public boolean isPort(BlockState state, Direction face) {
        return face.getAxis() != state.getValue(FACING).getAxis();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Gate(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.RIFT_GATE.get(), Gate::serverTick);
    }

    /** A hextech crystal pressed to the gate becomes a link crystal that remembers it. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        if (stack.is(ModItems.RIFT_LINK_CRYSTAL.get())) {
            // The link crystal does its own work (RiftLinkCrystalItem#useOn).
            return InteractionResult.PASS;
        }
        if (!stack.is(ModItems.UNSTABLE_HEXTECH.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel server) {
            ItemStack crystal = RiftLinkCrystalItem.remembering(UUID.randomUUID(), GlobalPos.of(level.dimension(), pos));
            stack.consume(1, player);
            if (!player.getInventory().add(crystal)) {
                player.drop(crystal, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
            player.sendOverlayMessage(Component.translatable("block.hextale.rift_gate.attuned"));
            server.playSound(null, pos, ModSounds.RIFT_SEND.get(), SoundSource.BLOCKS, 0.6F, 1.5F);
            Vec3 centre = Vec3.atCenterOf(pos);
            server.sendParticles(ModParticles.glint(RIFT), centre.x, centre.y, centre.z, 14, 0.25, 0.4, 0.25, 0.03);
        }
        return InteractionResult.SUCCESS;
    }

    /** An empty hand asks the gate about its link. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof Gate gate) {
            GlobalPos partner = gate.partner();
            Component status;
            if (partner != null && gate.dry) {
                status = Component.translatable("block.hextale.rift_gate.dry");
            } else if (partner != null) {
                status = Component.translatable("block.hextale.rift_gate.status.linked", partner.pos().getX(), partner.pos().getY(),
                        partner.pos().getZ(), partner.dimension().identifier().getPath());
            } else if (gate.link != null) {
                status = Component.translatable("block.hextale.rift_gate.status.missing");
            } else {
                status = Component.translatable("block.hextale.rift_gate.status.none");
            }
            player.sendOverlayMessage(status);
        }
        return InteractionResult.SUCCESS;
    }

    /** Keeps {@code entity} out of the rings until {@code until}; cooled-down entries are dropped on the way. */
    private static void cool(Entity entity, long until, long now) {
        if (COOLING.size() > 64) {
            COOLING.values().removeIf(time -> time <= now);
        }
        COOLING.put(entity.getUUID(), until);
    }

    /** What passes into the ring goes through (server), paid in rift essence. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!(level instanceof ServerLevel server) || !state.getValue(LINKED) || !(level.getBlockEntity(pos) instanceof Gate gate)) {
            return;
        }
        long now = level.getGameTime();
        Long cool = COOLING.get(entity.getUUID());
        if (cool != null && cool > now) {
            // Still in a ring it has just come out of: it must step out before it can go back.
            cool(entity, Math.max(cool, now + 10), now);
            return;
        }
        if (!travels(entity)) {
            return;
        }
        Gate partner = gate.partnerGate(server);
        if (partner == null) {
            return;
        }
        int toll = entity instanceof ItemEntity item ? item.getItem().getCount() : Gate.TOLL_PER_MB;
        if (!gate.afford(server, toll)) {
            // A dry ring: a thing is spat back out of its front; whoever stands in it goes through as soon as the jars
            // are filled again (the gate looks into them once a second), and a player is told why now and then.
            if (entity instanceof ItemEntity) {
                Direction front = state.getValue(FACING);
                entity.setDeltaMovement(front.getStepX() * 0.2, 0.18, front.getStepZ() * 0.2);
            }
            if (entity instanceof Player player && now % 40 == 0) {
                player.sendOverlayMessage(Component.translatable("block.hextale.rift_gate.dry"));
            }
            return;
        }
        gate.spend(toll);
        if (entity instanceof ItemEntity item) {
            ItemStack stack = item.getItem().copy();
            item.discard();
            gate.flare();
            partner.receive(stack);
            return;
        }
        gate.flare();
        partner.welcome(entity);
    }

    /** Whether {@code entity} may go through: not a boss, not a rider or a mount, not the grapple's own claw. */
    private static boolean travels(Entity entity) {
        return entity.isAlive() && !entity.isPassenger() && !entity.isVehicle() && !(entity instanceof GrappleHook)
                && !entity.typeHolder().is(Tags.EntityTypes.BOSSES) && !(entity instanceof Player player && player.isSpectator());
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (level.isClientSide() && (id == SEND_EVENT || id == ARRIVE_EVENT)) {
            if (level.getBlockEntity(pos) instanceof Gate gate) {
                gate.crossedAt = level.getGameTime();
            }
            Vec3 centre = Vec3.atLowerCornerOf(pos).add(0.5, 9 / 16.0, 0.5);
            RandomSource random = level.getRandom();
            for (int i = 0; i < 16; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = 0.35;
                Vec3 edge = centre.add(Math.cos(a) * r * (state.getValue(FACING).getAxis() == Direction.Axis.Z ? 1 : 0), Math.sin(a) * r,
                        Math.cos(a) * r * (state.getValue(FACING).getAxis() == Direction.Axis.X ? 1 : 0));
                Vec3 v = id == SEND_EVENT ? centre.subtract(edge).scale(0.15) : edge.subtract(centre).scale(0.2);
                Vec3 from = id == SEND_EVENT ? edge : centre;
                level.addParticle(ModParticles.glint(RIFT), from.x, from.y, from.z, v.x, v.y, v.z);
            }
            level.addParticle(ModParticles.ring(RIFT), centre.x, centre.y, centre.z, 0.5, 0, 0);
            return true;
        }
        return super.triggerEvent(state, level, pos, id, param);
    }

    /** Linked: rift light drifts into the ring now and then (a dry ring only smokes). Partner missing: the empty ring crackles. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof Gate gate)) {
            return;
        }
        Vec3 centre = Vec3.atLowerCornerOf(pos).add(0.5, 9 / 16.0, 0.5);
        boolean alongX = state.getValue(FACING).getAxis() == Direction.Axis.Z;
        double across = (random.nextDouble() - 0.5) * 0.6;
        double up = (random.nextDouble() - 0.5) * 0.7;
        Vec3 at = centre.add(alongX ? across : 0, up, alongX ? 0 : across);
        if (state.getValue(LINKED) && gate.dry) {
            if (random.nextInt(6) == 0) {
                level.addParticle(ModParticles.vapour(0x4A4058), at.x, at.y, at.z, 0, 0.01, 0);
            }
        } else if (state.getValue(LINKED)) {
            if (random.nextInt(3) == 0) {
                level.addParticle(ModParticles.glint(RIFT), at.x, at.y, at.z, (centre.x - at.x) * 0.05, (centre.y - at.y) * 0.05,
                        (centre.z - at.z) * 0.05);
            }
        } else if (gate.link != null) {
            if (random.nextInt(2) == 0) {
                level.addParticle(ModParticles.spark(RIFT), at.x, at.y, at.z, 0, 0.02, 0);
            }
            if (random.nextInt(14) == 0) {
                level.playLocalSound(pos, ModSounds.LAMP_FLICKER.get(), SoundSource.BLOCKS, 0.25F, 0.6F, false);
            }
        }
    }

    /** A gate's link and its mouth for hoppers and pylons. */
    public static class Gate extends BlockEntity implements WorldlyContainer, EssenceNetwork.Receiver {
        private static final int[] SLOTS = {0};
        /** Essence and fluids waiting to go through (they leave every FLOW_EVERY ticks). */
        public static final int ESSENCE_BUFFER = 1000;
        public static final int FLUID_BUFFER = 4000;
        private static final int FLOW_EVERY = 5;
        /** The toll is counted in eighths of a mB of rift essence: a creature or a player pays a whole one, an item one eighth. */
        public static final int TOLL_PER_MB = 8;
        /** Essence and fluids flowing through pay one eighth per this many mB. */
        public static final int ESSENCE_PER_TOLL = 16;
        public static final int FLUID_PER_TOLL = 125;
        /** Ticks a dry gate waits before it looks into its jars again. */
        private static final int DRY_RETRY = 20;
        private final EssenceTank essence = new EssenceTank(ESSENCE_BUFFER);
        private final FluidStacksResourceHandler fluid = new FluidStacksResourceHandler(1, FLUID_BUFFER) {
            @Override
            protected void onContentsChanged(int index, FluidStack previousContents) {
                setChanged();
            }
        };
        /** What the pipes and tanks of other mods see: fluid goes in while linked, nothing comes out. */
        private final ResourceHandler<FluidResource> fluidMouth = new DelegatingResourceHandler<>(fluid) {
            @Override
            public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
                return getBlockState().getValue(LINKED) ? super.insert(index, resource, amount, transaction) : 0;
            }

            @Override
            public int insert(FluidResource resource, int amount, TransactionContext transaction) {
                return getBlockState().getValue(LINKED) ? super.insert(resource, amount, transaction) : 0;
            }

            @Override
            public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
                return 0;
            }

            @Override
            public int extract(FluidResource resource, int amount, TransactionContext transaction) {
                return 0;
            }
        };
        private long flaredAt;
        @Nullable
        private UUID link;
        private ItemStack mouth = ItemStack.EMPTY;
        private int wait;
        /** Rift essence already drawn and not yet spent, in eighths of a mB (below zero: owed by the last flow). */
        private int credit;
        /** Its jars had no rift essence the last time it looked (synced: the ring smokes). */
        private boolean dry;
        /** Client: game time of the last crossing, for the surface's flare. */
        public long crossedAt = -1000;
        private long retryAt;

        public Gate(BlockPos pos, BlockState state) {
            super(ModBlockEntities.RIFT_GATE.get(), pos, state);
        }

        @Nullable
        public UUID link() {
            return link;
        }

        private GlobalPos here() {
            return GlobalPos.of(level.dimension(), worldPosition);
        }

        /** The partner gate's place (server), or null when unlinked or left alone. */
        @Nullable
        public GlobalPos partner() {
            if (link == null || !(level instanceof ServerLevel server)) {
                return null;
            }
            return RiftLinks.of(server.getServer()).partner(link, here());
        }

        /** The partner gate itself, its chunk loaded for the moment if need be (server). */
        @Nullable
        public Gate partnerGate(ServerLevel server) {
            return partnerGate(server, true);
        }

        /**
         * The partner gate; with {@code now} false an unloaded partner is not loaded on the spot: its chunk is asked to
         * load in the background and stay a while (a portal's ticket), and null comes back until it is there. For the
         * steady flow of essence and fluid, which would otherwise load the far chunk again and again.
         */
        @Nullable
        public Gate partnerGate(ServerLevel server, boolean now) {
            GlobalPos at = partner();
            if (at == null) {
                return null;
            }
            ServerLevel there = server.getServer().getLevel(at.dimension());
            if (there == null) {
                return null;
            }
            if (now) {
                there.getChunkAt(at.pos());
            } else {
                there.getChunkSource().addTicketWithRadius(net.minecraft.server.level.TicketType.PORTAL, ChunkPos.containing(at.pos()), 1);
                if (!there.isLoaded(at.pos())) {
                    return null;
                }
            }
            return there.getBlockEntity(at.pos()) instanceof Gate gate && link.equals(gate.link) ? gate : null;
        }

        /** Ties this gate into {@code id}; an old link it had is cut and its old partner left alone. */
        public void tie(UUID id) {
            if (level instanceof ServerLevel server && link != null && !link.equals(id)) {
                Gate old = partnerGate(server);
                RiftLinks.of(server.getServer()).cut(link, here());
                if (old != null) {
                    old.check();
                }
            }
            link = id;
            setChanged();
            check();
        }

        /** Looks whether the partner is still there and shows it in the block (linked or not). */
        public void check() {
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            boolean linked = partner() != null;
            BlockState state = getBlockState();
            if (state.getValue(LINKED) != linked) {
                if (!linked && link != null) {
                    server.playSound(null, worldPosition, ModSounds.RIFT_BREAK.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
                }
                level.setBlock(worldPosition, state.setValue(LINKED, linked), Block.UPDATE_ALL);
            }
            level.sendBlockUpdated(worldPosition, state, getBlockState(), Block.UPDATE_CLIENTS);
        }

        public boolean dry() {
            return dry;
        }

        /**
         * Whether the gate can pay a passage of {@code toll} eighths: it draws whole mB of rift essence from the jars
         * around it when its credit is short. A dry gate looks again only every {@value #DRY_RETRY} ticks.
         */
        public boolean afford(ServerLevel level, int toll) {
            if (credit >= toll) {
                return true;
            }
            long now = level.getGameTime();
            if (dry && now < retryAt) {
                return false;
            }
            int mb = Math.ceilDiv(toll - credit, TOLL_PER_MB);
            boolean paid = JarReach.pay(level, worldPosition, Vec3.atLowerCornerOf(worldPosition).add(0.5, 9 / 16.0, 0.5), Essence.RIFT, mb);
            if (paid) {
                credit += mb * TOLL_PER_MB;
                setChanged();
            } else {
                retryAt = now + DRY_RETRY;
            }
            setDry(!paid);
            return paid;
        }

        /** Takes a paid passage off the credit (a flow may leave it owing: the next draw pays that back). */
        public void spend(int toll) {
            credit -= toll;
            setChanged();
        }

        private void setDry(boolean value) {
            if (dry != value) {
                dry = value;
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            }
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, Gate gate) {
            ServerLevel server = (ServerLevel) level;
            if (!gate.mouth.isEmpty() && state.getValue(LINKED)) {
                Gate partner = gate.partnerGate(server);
                if (partner != null && gate.afford(server, gate.mouth.getCount())) {
                    gate.spend(gate.mouth.getCount());
                    ItemStack stack = gate.mouth;
                    gate.mouth = ItemStack.EMPTY;
                    gate.setChanged();
                    gate.flare();
                    partner.receive(stack);
                }
            }
            if (level.getGameTime() % FLOW_EVERY == 0 && (!gate.essence.isEmpty() || !gate.fluid.getResource(0).isEmpty())
                    && state.getValue(LINKED)) {
                Gate partner = gate.partnerGate(server, false);
                int toll = partner != null && gate.afford(server, 1) ? partner.pour(gate) : 0;
                if (toll > 0) {
                    gate.spend(toll);
                    long now = level.getGameTime();
                    if (now - gate.flaredAt > 30) {
                        gate.flaredAt = now;
                        gate.flare();
                        partner.arrived();
                    }
                }
            }
            if (++gate.wait >= 20) {
                gate.wait = 0;
                if (gate.link != null) {
                    // A partner replaced without being broken (or lost some other way) is forgotten here.
                    GlobalPos at = gate.partner();
                    if (at != null) {
                        ServerLevel there = server.getServer().getLevel(at.dimension());
                        if (there != null && there.isLoaded(at.pos())
                                && !(there.getBlockEntity(at.pos()) instanceof Gate other && gate.link.equals(other.link))) {
                            RiftLinks.of(server.getServer()).cut(gate.link, at);
                        }
                    }
                }
                gate.check();
            }
        }

        /** The send flare and sound at this gate. */
        void flare() {
            level.blockEvent(worldPosition, getBlockState().getBlock(), SEND_EVENT, 0);
            level.playSound(null, worldPosition, ModSounds.RIFT_SEND.get(), SoundSource.BLOCKS, 0.6F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        }

        private void arrived() {
            level.blockEvent(worldPosition, getBlockState().getBlock(), ARRIVE_EVENT, 0);
            level.playSound(null, worldPosition, ModSounds.RIFT_ARRIVE.get(), SoundSource.BLOCKS, 0.6F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        }

        private Direction front() {
            return getBlockState().getValue(FACING);
        }

        /** The fluid handler the pipes and tanks of other mods use. */
        public ResourceHandler<FluidResource> fluidMouth() {
            return fluidMouth;
        }

        public EssenceTank essence() {
            return essence;
        }

        /** Essence comes in through the pipes and jars joined to the gate, while it is linked. */
        @Override
        public int receive(Direction side, EssenceMix offer, boolean simulate) {
            return getBlockState().getValue(LINKED) ? essence.fill(offer, simulate) : 0;
        }

        /**
         * Essence and fluids waiting in the other gate come out here: essence into the pipes and jars joined to this gate
         * (below first), fluids into the tanks of other mods beside it (below first). Returns the toll of what moved, in
         * eighths of a mB of rift essence (0: nothing moved).
         */
        int pour(Gate from) {
            if (!(level instanceof ServerLevel server)) {
                return 0;
            }
            int essenceMoved = 0;
            int fluidMoved = 0;
            Direction facing = getBlockState().getValue(FACING);
            Direction[] faces = {Direction.DOWN, Direction.UP, facing.getClockWise(), facing.getCounterClockWise()};
            for (Direction face : faces) {
                if (from.essence.isEmpty()) {
                    break;
                }
                int sent = EssenceNetwork.push(server, worldPosition, face, from.essence.contents());
                if (sent > 0) {
                    from.essence.drain(sent, false);
                    from.setChanged();
                    essenceMoved += sent;
                }
            }
            for (Direction face : faces) {
                if (from.fluid.getResource(0).isEmpty()) {
                    break;
                }
                BlockPos next = worldPosition.relative(face);
                ResourceHandler<FluidResource> target = server.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Fluid.BLOCK,
                        next, face.getOpposite());
                if (target != null && !(server.getBlockEntity(next) instanceof Gate)) {
                    try (Transaction transaction = Transaction.openRoot()) {
                        int sent = ResourceHandlerUtil.move(from.fluid, target, resource -> true, FLUID_BUFFER, transaction);
                        if (sent > 0) {
                            transaction.commit();
                            fluidMoved += sent;
                        }
                    }
                }
            }
            return Math.ceilDiv(essenceMoved, ESSENCE_PER_TOLL) + Math.ceilDiv(fluidMoved, FLUID_PER_TOLL);
        }

        /** Items from the other gate: into the container under this one, the rest out of its front. */
        public void receive(ItemStack stack) {
            if (!(level instanceof ServerLevel server) || stack.isEmpty()) {
                return;
            }
            arrived();
            BlockPos below = worldPosition.below();
            int moved = PylonInsert.insert(server, below, Direction.UP, ItemResource.of(stack), stack.getCount(), null, null);
            ItemStack rest = stack.copyWithCount(stack.getCount() - moved);
            if (rest.isEmpty()) {
                return;
            }
            Direction front = front();
            Vec3 out = Vec3.atLowerCornerOf(worldPosition).add(0.5 + front.getStepX() * 0.45, 0.4, 0.5 + front.getStepZ() * 0.45);
            ItemEntity item = new ItemEntity(server, out.x, out.y, out.z, rest, front.getStepX() * 0.18, 0.12, front.getStepZ() * 0.18);
            item.setDefaultPickUpDelay();
            cool(item, level.getGameTime() + COOLDOWN * 3, level.getGameTime());
            server.addFreshEntity(item);
        }

        /** A creature or a player from the other gate steps out of this one's front. */
        public void welcome(Entity entity) {
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            Direction front = front();
            Vec3 inRing = Vec3.atLowerCornerOf(worldPosition).add(0.5, 3 / 16.0, 0.5);
            Vec3 ahead = Vec3.atBottomCenterOf(worldPosition.relative(front));
            Vec3 to = server.noCollision(entity, entity.getBoundingBox().move(ahead.subtract(entity.position()))) ? ahead : inRing;
            cool(entity, level.getGameTime() + COOLDOWN, level.getGameTime());
            Vec3 push = new Vec3(front.getStepX() * 0.15, 0.05, front.getStepZ() * 0.15);
            Entity moved = entity.teleport(new TeleportTransition(server, to, push, front.toYRot(), entity.getXRot(),
                    TeleportTransition.PLACE_PORTAL_TICKET));
            if (moved != null) {
                cool(moved, level.getGameTime() + COOLDOWN, level.getGameTime());
                moved.resetFallDistance();
            }
            arrived();
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            super.preRemoveSideEffects(pos, state);
            if (level instanceof ServerLevel server && link != null) {
                Gate partner = partnerGate(server);
                RiftLinks.of(server.getServer()).cut(link, here());
                if (partner != null) {
                    partner.check();
                }
                if (!mouth.isEmpty()) {
                    Block.popResource(level, pos, mouth);
                    mouth = ItemStack.EMPTY;
                }
            }
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            link = input.read("link", UUIDUtil.STRING_CODEC).orElse(null);
            mouth = input.read("mouth", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
            credit = input.getIntOr("credit", 0);
            dry = input.getBooleanOr("dry", false);
            essence.load(input, "essence");
            input.child("fluid").ifPresent(fluid::deserialize);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            if (link != null) {
                output.store("link", UUIDUtil.STRING_CODEC, link);
            }
            if (!mouth.isEmpty()) {
                output.store("mouth", ItemStack.OPTIONAL_CODEC, mouth);
            }
            output.putInt("credit", credit);
            output.putBoolean("dry", dry);
            essence.save(output, "essence");
            fluid.serialize(output.child("fluid"));
        }

        /** The clients only need to know whether it was ever linked (to tell "partner missing" from "never linked") and is dry. */
        @Override
        public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            if (link != null) {
                tag.putString("link", link.toString());
            }
            tag.putBoolean("dry", dry);
            return tag;
        }

        @Override
        public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
            return ClientboundBlockEntityDataPacket.create(this);
        }

        // The mouth: hoppers and pylons put items in (while the gate is linked), nothing comes out of it.

        @Override
        public int[] getSlotsForFace(Direction side) {
            return SLOTS;
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
            return getBlockState().getValue(LINKED) && mouth.isEmpty();
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
            return false;
        }

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return mouth.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return mouth;
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            ItemStack taken = mouth.split(count);
            setChanged();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack taken = mouth;
            mouth = ItemStack.EMPTY;
            return taken;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            mouth = stack;
            setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }

        @Override
        public void clearContent() {
            mouth = ItemStack.EMPTY;
        }
    }
}
