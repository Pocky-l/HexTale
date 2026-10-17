package com.pockyl.hextale.runework;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** The two block-breaking runes with more logic than fits {@link ToolRuneEffects}: Smelting and Erosion. */
public final class RuneFiring {
    /** Share of a furnace's experience a Smelting rune gives back, by strength 1-4. */
    private static final float[] SMELTING_XP = {0.0F, 0.5F, 1.0F, 1.5F};
    /** Blocks an Erosion rune washes away besides the broken one, by strength 1-4. */
    private static final int[] EROSION_REACH = {4, 8, 12, 16};

    private RuneFiring() {
    }

    /** Smelting: every drop with a furnace recipe comes out smelted, every item. */
    static void smelt(BlockDropsEvent event, ItemStack tool, RuneSlots.Engraving engraving) {
        ServerLevel level = event.getLevel();
        float experience = 0;
        int smelted = 0;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            SingleRecipeInput input = new SingleRecipeInput(stack);
            Optional<RecipeHolder<SmeltingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
            if (recipe.isEmpty()) {
                continue;
            }
            ItemStack result = recipe.get().value().assemble(input);
            if (result.isEmpty()) {
                continue;
            }
            int count = stack.getCount();
            result.setCount(Math.min(result.getMaxStackSize(), result.getCount() * count));
            drop.setItem(result);
            smelted += count;
            experience += recipe.get().value().experience() * count;
        }
        if (smelted <= 0) {
            return;
        }
        float share = SMELTING_XP[Math.min(4, HextechTools.strength(tool, engraving)) - 1];
        int xp = Mth.floor(experience * share);
        if (level.getRandom().nextFloat() < experience * share - xp) {
            xp++;
        }
        event.setDroppedExperience(event.getDroppedExperience() + xp);
        double x = event.getPos().getX() + 0.5;
        double y = event.getPos().getY() + 0.5;
        double z = event.getPos().getZ() + 0.5;
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 6, 0.25, 0.25, 0.25, 0.01);
        level.sendParticles(ParticleTypes.SMOKE, x, y + 0.2, z, 3, 0.2, 0.1, 0.2, 0.01);
        level.playSound(null, event.getPos(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.25F, 1.6F + level.getRandom().nextFloat() * 0.3F);
    }

    /**
     * Erosion: a broken loose block (anything a shovel digs) washes away the same blocks touching it, nearest first, up
     * to the rune's reach. Their drops go through the tool's other runes too.
     */
    static void erode(BlockDropsEvent event, Player player, ItemStack tool, RuneSlots.Engraving engraving) {
        BlockState origin = event.getState();
        if (!origin.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return;
        }
        ServerLevel level = event.getLevel();
        int reach = EROSION_REACH[Math.min(4, HextechTools.strength(tool, engraving)) - 1];
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(event.getPos());
        queue.add(event.getPos());
        int washed = 0;
        ToolRuneEffects.chaining(true);
        try {
            while (!queue.isEmpty() && washed < reach) {
                BlockPos at = queue.poll();
                for (Direction direction : Direction.values()) {
                    BlockPos next = at.relative(direction);
                    if (washed >= reach || !seen.add(next)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(next);
                    if (!state.is(origin.getBlock())) {
                        continue;
                    }
                    Block.dropResources(state, level, next, level.getBlockEntity(next), player, tool);
                    level.destroyBlock(next, false, player);
                    level.sendParticles(ParticleTypes.SPLASH, next.getX() + 0.5, next.getY() + 0.8, next.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.05);
                    washed++;
                    queue.add(next);
                }
            }
        } finally {
            ToolRuneEffects.chaining(false);
            if (washed > 0) {
                level.playSound(null, event.getPos(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5F, 1.4F);
            }
        }
    }
}
