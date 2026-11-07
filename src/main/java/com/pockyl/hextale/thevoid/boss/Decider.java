package com.pockyl.hextale.thevoid.boss;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.thevoid.boss.attack.AttackType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Picks the next move: a weighted draw in which the last move and anything on cooldown weigh nothing, distance bands
 * and the phase gate the rest, and a pity count forces Excision so the big opening always comes. A target standing
 * too high makes Excision, the plug that falls from above, far more likely.
 */
public final class Decider {
    /** How much more likely Excision is against a target out of reach above the table. */
    private static final double HEIGHT_WEIGHT = 6;
    private final Map<AttackType, Integer> cooldowns = new EnumMap<>(AttackType.class);
    private @Nullable AttackType last;
    private int sinceExcision;

    public void tick() {
        cooldowns.replaceAll((type, left) -> Math.max(0, left - 1));
    }

    public @Nullable AttackType last() {
        return last;
    }

    public int sinceExcision() {
        return sinceExcision;
    }

    public int cooldown(AttackType type) {
        return cooldowns.getOrDefault(type, 0);
    }

    public void setCooldown(AttackType type, int ticks) {
        cooldowns.put(type, ticks);
    }

    public void chosen(AttackType type) {
        last = type;
        cooldowns.put(type, type.data().cooldown().get());
        sinceExcision = type == AttackType.EXCISION ? 0 : sinceExcision + 1;
    }

    /** The move's weight right now: zero means it cannot be picked. */
    public double weight(AttackType type, Prosector boss, LivingEntity target) {
        if (type == last || cooldown(type) > 0 || boss.phase() < type.data().minPhase().get() || !type.ready(boss)) {
            return 0;
        }
        double distance = boss.distanceTo(target);
        double min = type.data().minRange().get();
        double max = type.data().maxRange().get();
        if (distance < min || distance > max) {
            return 0;
        }
        double weight = type.data().weight().get();
        if (type == AttackType.EXCISION && target.getY() > boss.floorHeight() + ProsectorConfig.HEIGHT_ANSWER.get()) {
            weight *= HEIGHT_WEIGHT;
        }
        return weight;
    }

    public @Nullable AttackType choose(Prosector boss, LivingEntity target) {
        if (sinceExcision >= ProsectorConfig.PITY.get()) {
            return AttackType.EXCISION;
        }
        return draw(List.of(AttackType.values()), boss, target, boss.getRandom());
    }

    /** Behind the Proof it only answers with what it took, from all four kinds. */
    public @Nullable AttackType chooseMimic(Prosector boss, LivingEntity target) {
        List<AttackType> mimics = List.of(AttackType.RIPOSTE, AttackType.VOLLEY, AttackType.CHARGE, AttackType.CIRCLE);
        AttackType type = mimics.get(boss.getRandom().nextInt(mimics.size()));
        return type == last ? mimics.get((mimics.indexOf(type) + 1) % mimics.size()) : type;
    }

    private @Nullable AttackType draw(List<AttackType> types, Prosector boss, LivingEntity target, RandomSource random) {
        double total = 0;
        for (AttackType type : types) {
            total += weight(type, boss, target);
        }
        if (total <= 0) {
            return null;
        }
        double pick = random.nextDouble() * total;
        for (AttackType type : types) {
            pick -= weight(type, boss, target);
            if (pick <= 0 && weight(type, boss, target) > 0) {
                return type;
            }
        }
        return null;
    }
}
