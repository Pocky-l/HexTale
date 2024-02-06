package com.pocky.hextale.common.blocks;

import com.pocky.hextale.common.recipe.HexcoreRecipe;
import com.pocky.hextale.common.recipe.containers.HexcoreContainer;
import com.pocky.hextale.common.register.ModBlockEntities;
import com.pocky.hextale.common.register.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class HexcoreEntity extends BlockEntity implements GeoBlockEntity {

    public NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public List<ItemEntity> nearbyItems = new ArrayList<>();

    private AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final RawAnimation IDLE_NOT_ACTIVE = RawAnimation.begin().thenPlay("animation.hexcore.boom").thenLoop("animation.hexcore.idle_not_active");
    private static final RawAnimation IDLE_ACTIVE = RawAnimation.begin().thenPlay("animation.hexcore.activated").thenLoop("animation.hexcore.idle_active");
    private static final RawAnimation CRAFTING = RawAnimation.begin().thenLoop("animation.hexcore.crafting");

    private int craftDuration = 5 * 20;  // 5 seconds in ticks
    public int craftProgress = 0;
    private int attractionTicks = 0;

    public int itemDiscardTick = 0;
    public int itemDiscardIndex = 0;

    private boolean isActive = false;
    public boolean isCrafting = false;

    public HexcoreEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntities.HEXCORE.get(), blockPos, blockState);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle_not_active", 5, this::animation));
    }

    protected <E extends HexcoreEntity> PlayState animation(final AnimationState<E> event) {
        isActive = !this.items.get(0).is(Items.AIR);
        if (!isActive && !isCrafting) {
            return event.setAndContinue(IDLE_NOT_ACTIVE);
        }
        if (isActive && !isCrafting) {
            return event.setAndContinue(IDLE_ACTIVE);
        }
        return event.setAndContinue(CRAFTING);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, HexcoreEntity hexcoreEntity) {
        boolean isCrafting = false;

        spawnEnchantParticles(level, blockPos, hexcoreEntity);

        if (hexcoreEntity.attractionTicks > 0) {
            if (!hexcoreEntity.nearbyItems.isEmpty()) {
                if (hexcoreEntity.itemDiscardIndex < hexcoreEntity.nearbyItems.size()) {
                    ItemEntity itemEntity = hexcoreEntity.nearbyItems.get(hexcoreEntity.itemDiscardIndex);
                    if (hexcoreEntity.itemDiscardTick++ <= 70) {
                        hexcoreEntity.itemAnimation(level, itemEntity);
                    } else {
                        if (itemEntity.isAlive()) {
                            hexcoreEntity.craftProgress++;
                            itemEntity.discard();

                            playSound(level, SoundEvents.ITEM_PICKUP, blockPos, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());

                            eatItemParticle(level, itemEntity);
                        }
                        hexcoreEntity.itemDiscardTick = 0;
                        hexcoreEntity.itemDiscardIndex++;
                    }
                }
            }
            isCrafting = true;
            hexcoreEntity.attractionTicks--;
        }

        if (!(hexcoreEntity.attractionTicks > 0) && isCrafting && hexcoreEntity.craftProgress == hexcoreEntity.nearbyItems.size()) {
            playSound(level, SoundEvents.BEACON_DEACTIVATE, blockPos, blockPos.getX(), blockPos.getY(), blockPos.getZ());
            playSound(level, SoundEvents.END_PORTAL_FRAME_FILL, blockPos, blockPos.getX(), blockPos.getY(), blockPos.getZ());

            hexcoreEntity.isCrafting = false;
            hexcoreEntity.items.set(0, Items.AIR.getDefaultInstance());
            hexcoreEntity.setChanged();
            hexcoreEntity.craftMatchingRecipe(level, true);
        } else if (hexcoreEntity.attractionTicks == 0 && !isCrafting && hexcoreEntity.isCrafting) {
            hexcoreEntity.isCrafting = false;
            hexcoreEntity.items.set(0, Items.AIR.getDefaultInstance());
            hexcoreEntity.setChanged();
            failCraftParticle(level, blockPos);
            playSound(level, SoundEvents.GENERIC_EXPLODE, blockPos, blockPos.getX(), blockPos.getY(), blockPos.getZ());
        }
    }

    private static void playSound(Level level, SoundEvent sound, BlockPos center, double x, double y, double z) {
        List<ServerPlayer> players = new ArrayList<>(level.getEntitiesOfClass(ServerPlayer.class, new AABB(center).inflate(15.0D)));
        ClientboundSoundPacket packet = new ClientboundSoundPacket(Holder.direct(sound), SoundSource.AMBIENT, x, y, z, 1.0F, 1.0F, 0L);
        players.forEach(e -> e.connection.send(packet));
    }

    private static void failCraftParticle(Level level, BlockPos pos) {
        for (int i = 0; i < 50; i++) {
            ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE,
                    pos.getX()-0.2D + (level.random.nextDouble()*1.5),
                    pos.getY()-0.25D + (level.random.nextDouble()*1.5),
                    pos.getZ()-0.2D + (level.random.nextDouble()*1.5),
                    1, 0, 0, 0, 0);
        }
    }

    private static void eatItemParticle(Level level, ItemEntity itemEntity) {
        DustParticleOptions particle = new DustParticleOptions(new Vector3f(80, 240, 242),1);
        for (int i = 0; i < 10; i++) {
            ((ServerLevel) level).sendParticles(particle,
                    itemEntity.getX()-0.2D + (level.random.nextDouble()/2),
                    itemEntity.getY()+0.3D + (level.random.nextDouble()/2),
                    itemEntity.getZ()-0.2D + (level.random.nextDouble()/2),
                    1, 0, 0, 0, 0);
        }
    }

    private static void spawnEnchantParticles(Level level, BlockPos blockPos, HexcoreEntity hexcoreEntity) {
        if (!hexcoreEntity.items.get(0).is(Items.AIR)) {
            ((ServerLevel) level).sendParticles(ParticleTypes.ENCHANT,
                    blockPos.getX()-0.5D + (level.random.nextDouble()*2),
                    blockPos.getY()+0.5D-0.5D + (level.random.nextDouble()*2),
                    blockPos.getZ()-0.5D + (level.random.nextDouble()*2),
                    1, 0, 0, 0, 0);
        }
    }

    private void itemAnimation(Level level, ItemEntity entity) {
        BlockPos blockPos = this.getBlockPos();

        ServerLevel serverLevel = (ServerLevel) level;

        double itemX = entity.getX();
        double itemY = entity.getY() + 0.3D;
        double itemZ = entity.getZ();

        serverLevel.sendParticles(
                ParticleTypes.ENCHANT,
                itemX -0.2D + (serverLevel.random.nextDouble()*0.4D),
                itemY + (serverLevel.random.nextDouble()*0.4D),
                itemZ -0.2D + (serverLevel.random.nextDouble()*0.4D),
                1,  // Number of particles
                0, 0, 0,  // Motion
                0  // Scale
        );

        ItemParticleOption particleData = new ItemParticleOption(ParticleTypes.ITEM, entity.getItem());
        for (int i = 1; i < 15; i++) {
            double particleX = itemX + (blockPos.getX()+0.5D - itemX) * i / 15.0D;
            double particleY = itemY + (blockPos.getY()+1.3D - itemY) * i / 15.0D;
            double particleZ = itemZ + (blockPos.getZ()+0.5D - itemZ) * i / 15.0D;
            serverLevel.sendParticles(
                    particleData,
                    particleX, particleY, particleZ,
                    1,  // Number of particles
                    0, 0, 0,  // Motion
                    0  // Scale
            );
        }
    }

    public boolean craftMatchingRecipe(Level level, boolean isCrafting) {
        // Collect nearby item entities
        HexcoreContainer container = new HexcoreContainer();
        List<ItemEntity> entities = new LinkedList<>();
        int index = 0;
        for (ItemEntity entity : nearbyItems) {
            container.setItem(index, entity.getItem());
            entities.add(entity);
            index++;
        }

        // Check for matching recipes using the correct RecipeType
        Optional<HexcoreRecipe> matchingRecipe = level.getRecipeManager().getRecipeFor(ModRecipeTypes.HEXCORE.get(), container, level);
        if (matchingRecipe.isPresent()) {
            if (isCrafting) {
                ItemStack output = matchingRecipe.get().getResultItem(RegistryAccess.EMPTY);
                ItemEntity itemEntity = new ItemEntity(level, this.getBlockPos().getX(), this.getBlockPos().getY() + 1, this.getBlockPos().getZ(), output);
                entities.forEach(Entity::discard);
                level.addFreshEntity(itemEntity);
            }
            craftDuration = 70 * entities.size() + 70;
            lockItems(entities);
            return true;
        }
        return false;
    }

    public void lockItems(List<ItemEntity> items) {
        items.forEach(e -> e.setPickUpDelay(craftDuration));
    }

    public void playSoundActivate() {
        playSound(this.level, SoundEvents.BEACON_ACTIVATE, this.getBlockPos(),
                this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ());
    }

    public void startAttractingEntities() {
        isCrafting = true;
        setChanged();
        attractionTicks = craftDuration;
    }

    @Override
    public void load(CompoundTag compoundTag) {
        super.load(compoundTag);
        this.items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compoundTag, this.items);
        this.isCrafting = compoundTag.getBoolean("isCrafting");
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag) {
        super.saveAdditional(compoundTag);
        ContainerHelper.saveAllItems(compoundTag, this.items);
        compoundTag.putBoolean("isCrafting", this.isCrafting);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (!level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        load(pkt.getTag());
    }
}
