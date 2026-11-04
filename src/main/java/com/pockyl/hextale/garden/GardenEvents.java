package com.pockyl.hextale.garden;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.block.PinkBrazierBlockEntity;
import com.pockyl.hextale.registry.ModParticles;
import com.pockyl.hextale.runework.PlanterRune;
import com.pockyl.hextale.runework.ToolEffects;

import java.util.ArrayList;
import java.util.List;

/**
 * The garden's beds at work, on events only (no area scans): a plant on a dosed bed that grows a stage may grow one
 * more; a ripe harvest over a planter with Fertility may come twice; a vial or a bottle of sap poured on farmland
 * steeps it.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class GardenEvents {
    /** Set while a bed makes a plant grow, so that growth does not count as a new one. */
    private static final ThreadLocal<Boolean> GROWING = ThreadLocal.withInitial(() -> false);

    private GardenEvents() {
    }

    @SubscribeEvent
    public static void cropGrew(CropGrowEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || GROWING.get()) {
            return;
        }
        grew(level, event.getPos());
    }

    /** A plant at {@code plant} grew a stage: the bed under it may make it grow another. */
    public static void grew(ServerLevel level, BlockPos plant) {
        BlockPos below = plant.below();
        BlockState soil = level.getBlockState(below);
        if (soil.getBlock() instanceof SteepedSoilBlock) {
            Dose dose = soil.getValue(SteepedSoilBlock.DOSE);
            if (dose != Dose.NONE && soil.getValue(SteepedSoilBlock.CHARGES) > 0 && level.getRandom().nextFloat() < dose.extraStage()) {
                growMore(level, plant, dose);
                SteepedSoilBlock.spend(level, below, soil);
            }
        }
    }

    /** The plant grows a stage more on a dose: motes of the dose's colour rise from the soil into it. */
    public static void growMore(ServerLevel level, BlockPos plant, Dose dose) {
        BlockState state = level.getBlockState(plant);
        if (!SteepedSoilBlock.grows(state)) {
            return;
        }
        GROWING.set(true);
        try {
            PinkBrazierBlockEntity.growStep(level, plant, state, level.getRandom());
        } finally {
            GROWING.set(false);
        }
        Vec3 at = Vec3.atBottomCenterOf(plant);
        level.sendParticles(ModParticles.wisp(dose.color()), at.x, at.y + 0.1, at.z, 5, 0.2, 0.15, 0.2, 0.01);
        level.sendParticles(ModParticles.glint(dose.color()), at.x, at.y + 0.4, at.z, 4, 0.2, 0.25, 0.2, 0.02);
        level.playSound(null, plant, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 0.25F, 0.8F + level.getRandom().nextFloat() * 0.2F);
    }

    /**
     * A ripe plant broken over a dosed bed may give its crop twice (the seeds once): the dose's chance on steeped soil, the
     * dose's chance plus Fertility on a Chem Planter. The dose is not spent.
     */
    @SubscribeEvent
    public static void drops(BlockDropsEvent event) {
        BlockState state = event.getState();
        if (event.getDrops().isEmpty() || !SteepedSoilBlock.grows(state)
                || state.getBlock() instanceof CropBlock crop && !crop.isMaxAge(state)) {
            return;
        }
        ServerLevel level = event.getLevel();
        BlockPos below = event.getPos().below();
        BlockState soil = level.getBlockState(below);
        float chance;
        boolean planter = false;
        if (soil.getBlock() instanceof SteepedSoilBlock && soil.getValue(SteepedSoilBlock.CHARGES) > 0) {
            chance = soil.getValue(SteepedSoilBlock.DOSE).harvest();
        } else if (level.getBlockEntity(below) instanceof ChemPlanterBlockEntity bed) {
            chance = bed.doubleHarvest();
            planter = true;
        } else {
            return;
        }
        if (chance <= 0 || level.getRandom().nextFloat() >= chance) {
            return;
        }
        net.minecraft.world.item.Item seed = state.getBlock().asItem();
        List<ItemEntity> twice = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            if (!drop.getItem().is(seed)) {
                twice.add(new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy()));
            }
        }
        event.getDrops().addAll(twice);
        if (planter) {
            ToolEffects.puff(level, Vec3.atCenterOf(event.getPos()), PlanterRune.FERTILITY.rune(), 10, 0.3);
        } else {
            Vec3 at = Vec3.atCenterOf(event.getPos());
            level.sendParticles(ModParticles.glint(soil.getValue(SteepedSoilBlock.DOSE).color()), at.x, at.y, at.z, 8, 0.25, 0.25, 0.25, 0.02);
        }
    }

    /** A vial of growth tonic or raw shimmer, or a bottle of sap, poured on farmland or steeped soil. */
    @SubscribeEvent
    public static void pourOnSoil(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos = event.getPos();
        if (GardenPouring.of(event.getItemStack()) == null || !SteepedSoilBlock.takesDose(event.getLevel().getBlockState(pos))) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel level) {
            GardenPouring.pourOnSoil(level, pos, event.getItemStack(), event.getEntity(), event.getHand());
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
