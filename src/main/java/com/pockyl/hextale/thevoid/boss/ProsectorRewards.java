package com.pockyl.hextale.thevoid.boss;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.item.RuneScrollItem;
import com.pockyl.hextale.network.ProsectorMarkerPayload;
import com.pockyl.hextale.network.ProsectorMarkerPayload.Marker;
import com.pockyl.hextale.quest.Quests;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.research.ResearchBook;
import com.pockyl.hextale.rune.ComplexRune;
import com.pockyl.hextale.runework.ToolRune;

import java.util.ArrayList;
import java.util.List;

/**
 * What a kill leaves. The loot is drawn from its table at random and bursts out of it, only when the long death is over,
 * and it never despawns; the experience comes in a stream over its last moments.
 */
public final class ProsectorRewards {
    /** Ticks of the death's end over which the experience comes out. */
    public static final int XP_STREAM = 30;
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Hextale.id("entities/prosector"));
    private static final int HARD_CHAPTER = 3;
    /** How far and how high the loot is thrown, and how long it cannot be picked up. */
    private static final double SCATTER = 0.3;
    private static final double THROW_UP = 0.35;
    private static final int PICK_UP_DELAY = 20;
    private static final int LOOT_COLOR = 0xFFD27A;

    private ProsectorRewards() {
    }

    public static void streamXp(ServerLevel level, Prosector boss, Vec3 at) {
        int total = (ProsectorRecord.of(level).kills() == 0 ? ProsectorConfig.FIRST_XP : ProsectorConfig.REPEAT_XP).get();
        ExperienceOrb.award(level, at.add(0, 1.2, 0), Math.max(1, total / XP_STREAM));
    }

    public static void finish(ServerLevel level, Prosector boss, Vec3 at, @Nullable DamageSource source) {
        ProsectorRecord.of(level).addKill();
        for (ServerPlayer player : boss.playersInFight(level)) {
            Quests.fire(player, Quests.PROSECTOR_SLAIN);
        }
        RandomSource random = boss.getRandom();
        for (ItemStack stack : loot(level, boss, source != null ? source : level.damageSources().generic())) {
            ItemEntity drop = new ItemEntity(level, at.x, at.y + 1, at.z, stack);
            double angle = random.nextDouble() * Math.PI * 2;
            double out = SCATTER * (0.4 + 0.6 * random.nextDouble());
            drop.setDeltaMovement(Math.cos(angle) * out, THROW_UP + random.nextDouble() * 0.15, Math.sin(angle) * out);
            drop.setPickUpDelay(PICK_UP_DELAY);
            drop.setUnlimitedLifetime();
            level.addFreshEntity(drop);
        }
        ProsectorMarkerPayload.send(level, Marker.ERUPT, at, at, 3, 24, LOOT_COLOR);
    }

    private static List<ItemStack> loot(ServerLevel level, Prosector boss, DamageSource source) {
        List<ItemStack> loot = new ArrayList<>();
        loot.addAll(com.pockyl.hextale.port.PortEntities.loot(level, boss, LOOT, source, true));
        RandomSource random = boss.getRandom();
        List<ComplexRune> runes = ResearchBook.findable(HARD_CHAPTER);
        int min = ProsectorConfig.MIN_SCROLLS.get();
        int count = min + random.nextInt(Math.max(1, ProsectorConfig.MAX_SCROLLS.get() - min + 1));
        for (int i = 0; i < count && !runes.isEmpty(); i++) {
            loot.add(RuneScrollItem.of(runes.get(random.nextInt(runes.size())), ProsectorConfig.SCROLL_STARS.get(), ModItems.RUNE_SCROLL.get()));
        }
        if (random.nextDouble() < ProsectorConfig.ANATOMY_CHANCE.get()) {
            loot.add(RuneScrollItem.of(ToolRune.ANATOMY.rune(), ProsectorConfig.SCROLL_STARS.get(), ModItems.RUNE_SCROLL.get()));
        }
        return loot;
    }
}
