package com.pockyl.hextale.farm;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

import com.pockyl.hextale.registry.ModAttachments;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * What a Herdkeeper takes from an animal: a roll of the animal's own loot table, read without killing it, so a modded
 * animal gives what it would drop. Nothing here touches the animal's health, age or love. Every animal keeps its own rest
 * (attachment {@link ModAttachments#HERD_REST}), so a second Herdkeeper beside the first gains nothing.
 */
public final class Herding {
    private Herding() {
    }

    /** A grown passive animal: no monster, no villager or trader, no golem (those are of the category MISC). */
    public static boolean tendable(Mob mob) {
        MobCategory category = mob.getType().getCategory();
        return mob.isAlive() && !mob.isBaby() && category != MobCategory.MONSTER && category != MobCategory.MISC
                && !(mob instanceof Enemy) && !(mob instanceof Npc);
    }

    public static boolean resting(Mob mob, long now) {
        return now < mob.getData(ModAttachments.HERD_REST.get());
    }

    public static void rest(Mob mob, long until) {
        mob.setData(ModAttachments.HERD_REST.get(), until);
    }

    /** The animal's own loot plus what it gives alive (a chicken's egg, an armadillo's scute); it stays as it was. */
    public static List<ItemStack> gather(ServerLevel level, Mob animal) {
        List<ItemStack> produce = new ArrayList<>();
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            return produce;
        }
        produce.addAll(com.pockyl.hextale.port.PortEntities.loot(level, animal, animal.getLootTable(), level.damageSources().generic(), false));
        // 1.21.1: a chicken's egg and an armadillo's scute are hard-coded items, not gift loot tables.
        if (animal instanceof Chicken) {
            produce.add(new ItemStack(net.minecraft.world.item.Items.EGG));
        } else if (animal instanceof Armadillo) {
            produce.add(new ItemStack(net.minecraft.world.item.Items.ARMADILLO_SCUTE));
        }
        produce.removeIf(ItemStack::isEmpty);
        return produce;
    }
}
