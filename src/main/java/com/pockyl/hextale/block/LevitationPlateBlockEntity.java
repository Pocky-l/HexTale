package com.pockyl.hextale.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.pockyl.hextale.registry.ModBlockEntities;
import com.pockyl.hextale.registry.ModDataComponents;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.registry.ModSounds;
import com.pockyl.hextale.runework.HolderRune;
import com.pockyl.hextale.runework.PickaxeRunes;
import com.pockyl.hextale.runework.PlateRune;
import com.pockyl.hextale.runework.ToolEffects;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * The Levitation Plate: a column of still air above it lifts whatever stands in it - creatures, players, dropped items
 * - up to the top of the column and holds it there, bobbing; a sneaking player sinks back down. Nobody in the column is
 * hurt by falling. Runs on both sides like a bubble column: every entity is moved where its movement is simulated
 * ({@link Entity#canSimulateMovement()}), so a player's own client lifts them without lag. The column ends at the first
 * solid block above. Takes runes (Shift, Motor, Breath, Cache: items at the top go into the container that caps the
 * column; Slumber: creatures in it hang asleep).
 */
public class LevitationPlateBlockEntity extends BlockEntity {
    /** How far down a sneaking player sinks a tick. */
    private static final double SINK = 0.18;
    private PickaxeRunes runes = PickaxeRunes.EMPTY;
    /** Entities in the column last tick (server), to know who just came in and who just left it. */
    private final Set<Integer> inside = new HashSet<>();
    /** How many ride the column now (both sides; the clients animate and hum by it). */
    private int riders;
    /** Client: the ring's turn (degrees) this tick and the last, and how fast it turns now. */
    public float spin;
    public float spinO;
    private float spinSpeed = SPIN_IDLE;
    /** Client: 0 idle .. 1 carrying someone, eased. */
    public float lift;
    public float liftO;
    /** Client: starts the column's hum when someone rides (set by the client; the server has none). */
    public static java.util.function.Consumer<LevitationPlateBlockEntity> hum = plate -> {
    };
    private static final float SPIN_IDLE = 1.0F;
    private static final float SPIN_LIFTING = 4.5F;

    public LevitationPlateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LEVITATION_PLATE.get(), pos, state);
    }

    public PickaxeRunes runes() {
        return runes;
    }

    public int riders() {
        return riders;
    }

    private int strength(PlateRune rune) {
        return runes.strength(rune.rune());
    }

    /** How many blocks of open air the column reaches above the plate (stops under the first solid block). */
    public int reach(Level level) {
        int height = PlateRune.height(strength(PlateRune.SHIFT));
        for (int dy = 1; dy <= height; dy++) {
            BlockPos at = worldPosition.above(dy);
            if (!level.getBlockState(at).getCollisionShape(level, at).isEmpty()) {
                return dy - 1;
            }
        }
        return height;
    }

    /** The column: the plate's square, from its top to the column's top. */
    public AABB column(Level level) {
        return new AABB(worldPosition.getX() + 0.05, worldPosition.getY(), worldPosition.getZ() + 0.05,
                worldPosition.getX() + 0.95, worldPosition.getY() + 1 + reach(level), worldPosition.getZ() + 0.95);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LevitationPlateBlockEntity plate) {
        AABB column = plate.column(level);
        double top = column.maxY - 0.6;
        float speed = PlateRune.speed(plate.strength(PlateRune.MOTOR));
        List<Entity> entities = level.getEntities((Entity) null, column, entity -> !entity.isSpectator() && !entity.noPhysics);
        Set<Integer> now = new HashSet<>();
        for (Entity entity : entities) {
            now.add(entity.getId());
            if (entity.canSimulateMovement()) {
                lift(entity, top, speed, level.getGameTime());
            }
            if (!level.isClientSide()) {
                entity.resetFallDistance();
                // Hovering on the column is not flying: no kick on servers that forbid flight.
                if (entity instanceof net.minecraft.server.level.ServerPlayer player && player.connection != null) {
                    player.connection.resetFlyingTicks();
                }
            }
        }
        int before = plate.riders;
        plate.riders = now.size();
        if (level instanceof ServerLevel server) {
            plate.arrive(server, now);
            plate.leave(server, now);
            plate.cache(server, entities, top, column);
            plate.slumber(server, entities);
        } else {
            plate.animate(before);
        }
    }

    /** Client: the ring turns faster while it carries someone and the carrying eases in and out; the hum starts. */
    private void animate(int before) {
        spinO = spin;
        liftO = lift;
        spinSpeed += ((riders > 0 ? SPIN_LIFTING : SPIN_IDLE) - spinSpeed) * 0.08F;
        spin = (spin + spinSpeed) % 360;
        if (spin < spinO) {
            spinO -= 360;
        }
        lift += ((riders > 0 ? 1 : 0) - lift) * 0.1F;
        if (riders > 0 && before == 0) {
            hum.accept(this);
        }
    }

    /**
     * Cache: now and then the items hovering at the top of the column go into the container that caps it (put in from
     * below, like a hopper would), each with a puff of the rune's light.
     */
    private void cache(ServerLevel level, List<Entity> entities, double top, AABB column) {
        int cache = strength(PlateRune.CACHE);
        if (cache <= 0 || level.getGameTime() % HolderRune.at(PlateRune.CACHE_EVERY, cache) != 0) {
            return;
        }
        BlockPos cap = BlockPos.containing(worldPosition.getX(), column.maxY, worldPosition.getZ());
        if (level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Item.BLOCK, cap, net.minecraft.core.Direction.DOWN) == null) {
            return;
        }
        for (Entity entity : entities) {
            if (!(entity instanceof net.minecraft.world.entity.item.ItemEntity item) || !item.isAlive() || item.getY() < top - 1.0) {
                continue;
            }
            ItemStack stack = item.getItem();
            int moved = com.pockyl.hextale.transport.PylonInsert.insert(level, cap, net.minecraft.core.Direction.DOWN,
                    net.neoforged.neoforge.transfer.item.ItemResource.of(stack), stack.getCount(), null, null);
            if (moved <= 0) {
                continue;
            }
            stack.shrink(moved);
            if (stack.isEmpty()) {
                item.discard();
            } else {
                item.setItem(stack);
            }
            ToolEffects.puff(level, item.position().add(0, 0.25, 0), PlateRune.CACHE.rune(), 4, 0.15);
            level.playSound(null, item.getX(), item.getY(), item.getZ(), ModSounds.PYLON_ARRIVE.get(), SoundSource.BLOCKS, 0.35F,
                    1.1F + level.getRandom().nextFloat() * 0.2F);
        }
    }

    /** Slumber: creatures in the column hang asleep - slowed to a stop, harmless, forgetting whom they hunted. */
    private void slumber(ServerLevel level, List<Entity> entities) {
        int slumber = strength(PlateRune.SLUMBER);
        if (slumber <= 0 || level.getGameTime() % 20 != 0) {
            return;
        }
        int ticks = HolderRune.at(PlateRune.SLUMBER_TICKS, slumber);
        for (Entity entity : entities) {
            if (entity instanceof net.minecraft.world.entity.Mob mob && mob.isAlive()) {
                mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 4, true, false, true));
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1, true, false, true));
                mob.setTarget(null);
                if (level.getRandom().nextInt(3) == 0) {
                    level.sendParticles(ModParticles.glint(PlateRune.SLUMBER.rune().color()), mob.getX(), mob.getY() + mob.getBbHeight() + 0.2,
                            mob.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
                }
            }
        }
    }

    /** Whoever just came into the column is taken up with a soft swell of air. */
    private void arrive(ServerLevel level, Set<Integer> now) {
        for (int id : now) {
            if (!inside.contains(id) && level.getEntity(id) instanceof Entity entity && !(entity instanceof net.minecraft.world.entity.item.ItemEntity)) {
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.LEVITATION_LIFT.get(), SoundSource.BLOCKS, 0.6F,
                        0.9F + level.getRandom().nextFloat() * 0.2F);
                level.sendParticles(ModParticles.ring(ModParticles.HEXTECH), worldPosition.getX() + 0.5, worldPosition.getY() + 0.3,
                        worldPosition.getZ() + 0.5, 0, 0.5, 1, 0, 1);
            }
        }
    }

    /** One tick in the column: rise towards the top, hover there with a slow bob, or sink while sneaking. */
    private static void lift(Entity entity, double top, float speed, long time) {
        Vec3 motion = entity.getDeltaMovement();
        double vy;
        if (entity instanceof Player player && player.isShiftKeyDown()) {
            vy = -SINK;
        } else if (entity.getY() < top - 0.3) {
            vy = Mth.lerp(0.35, motion.y, speed);
        } else {
            double bob = Math.sin((time + entity.getId()) * 0.15) * 0.02;
            vy = Mth.clamp((top - entity.getY()) * 0.25, -0.1, 0.1) + bob;
        }
        entity.setDeltaMovement(motion.x * 0.9, vy, motion.z * 0.9);
        entity.resetFallDistance();
    }

    /** Whoever just left the column is let go with a sigh of air; living things float down slowly for a while (Breath). */
    private void leave(ServerLevel level, Set<Integer> now) {
        int breath = strength(PlateRune.BREATH);
        for (int id : inside) {
            if (now.contains(id) || !(level.getEntity(id) instanceof Entity entity) || entity instanceof net.minecraft.world.entity.item.ItemEntity) {
                continue;
            }
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.LEVITATION_RELEASE.get(), SoundSource.BLOCKS, 0.5F,
                    0.9F + level.getRandom().nextFloat() * 0.2F);
            if (breath > 0 && entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, PlateRune.FLOAT_TICKS[Mth.clamp(breath, 1, 4) - 1], 0, true, false, true));
            }
        }
        inside.clear();
        inside.addAll(now);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        runes = components.getOrDefault(ModDataComponents.PLATE_RUNES.get(), PickaxeRunes.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!runes.isEmpty()) {
            components.set(ModDataComponents.PLATE_RUNES.get(), runes);
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
        runes = input.read("runes", PickaxeRunes.CODEC).orElse(PickaxeRunes.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!runes.isEmpty()) {
            output.store("runes", PickaxeRunes.CODEC, runes);
        }
    }

    /** The clients need the runes: the column's height and speed come from them. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
