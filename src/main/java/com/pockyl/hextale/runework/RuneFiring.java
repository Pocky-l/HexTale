package com.pockyl.hextale.runework;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import com.pockyl.hextale.rune.ComplexRune;

import java.util.Optional;

/** Where the runes on hextech tools fire: server-side game events. */
public final class RuneFiring {
    /** Share of a furnace's experience a Smelting rune gives back, by stars. */
    private static final float[] SMELTING_XP = {0.0F, 0.5F, 1.0F};

    private RuneFiring() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RuneFiring::onBlockDrops);
    }

    private static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (!HextechTools.isTool(tool) || !(event.getBreaker() instanceof Player)) {
            return;
        }
        RuneSlots.Engraving smelting = HextechTools.engraving(tool, ToolRunes.SMELTING);
        if (smelting != null) {
            smelt(event, tool, smelting);
        }
    }

    /** Smelting: every drop with a furnace recipe comes out smelted, one mB of fire per item. */
    private static void smelt(BlockDropsEvent event, ItemStack tool, RuneSlots.Engraving engraving) {
        ServerLevel level = event.getLevel();
        ComplexRune rune = engraving.rune();
        int cost = ToolRunes.cost(rune);
        float experience = 0;
        int smelted = 0;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            SingleRecipeInput input = new SingleRecipeInput(stack);
            Optional<RecipeHolder<SmeltingRecipe>> recipe = level.recipeAccess().getRecipeFor(RecipeType.SMELTING, input, level);
            if (recipe.isEmpty()) {
                continue;
            }
            int count = Math.min(stack.getCount(), HextechTools.affordable(tool, rune.action(), cost));
            if (count <= 0) {
                break;
            }
            ItemStack result = recipe.get().value().assemble(input);
            if (result.isEmpty()) {
                continue;
            }
            HextechTools.spend(tool, rune.action(), count * cost);
            result.setCount(Math.min(result.getMaxStackSize(), result.getCount() * count));
            ItemStack rest = stack.copyWithCount(stack.getCount() - count);
            drop.setItem(result);
            smelted += count;
            experience += recipe.get().value().experience() * count;
            if (!rest.isEmpty()) {
                // Out of fire halfway: the rest drops as it was mined.
                ItemEntity leftover = new ItemEntity(level, drop.getX(), drop.getY(), drop.getZ(), rest);
                leftover.setDefaultPickUpDelay();
                event.getDrops().add(leftover);
                break;
            }
        }
        if (smelted <= 0) {
            return;
        }
        float share = SMELTING_XP[engraving.stars() - 1];
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
}
