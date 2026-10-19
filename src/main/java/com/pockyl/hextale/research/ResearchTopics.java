package com.pockyl.hextale.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import com.pockyl.hextale.knowledge.RuneKnowledge;
import com.pockyl.hextale.registry.ModItems;
import com.pockyl.hextale.rune.Essence;
import com.pockyl.hextale.runework.HextechPickaxe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * Everything the Research Table teaches. The essence works come first and need no research (their ink is what research
 * is written with); every device and tool past them is worked out on a parchment before it can be crafted or made at
 * the altar. Themes follow what the thing does: the circle is earth and lightning, the pylons air, the alloys metal and
 * fire.
 */
public final class ResearchTopics {
    private static final List<ResearchTopic> ALL = new ArrayList<>();
    /** A topic by its id (saved notes keep working when topics are added). */
    public static final Codec<ResearchTopic> CODEC = Codec.STRING.comapFlatMap(id -> {
        ResearchTopic topic = byId(id);
        return topic == null ? DataResult.error(() -> "Unknown research topic " + id) : DataResult.success(topic);
    }, ResearchTopic::id);
    public static final StreamCodec<ByteBuf, ResearchTopic> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(id -> {
        ResearchTopic topic = byId(id);
        return topic == null ? ALL.getFirst() : topic;
    }, ResearchTopic::id);

    public static final ResearchTopic DRAIN_VALVE = add("drain_valve", 1, Essence.WATER, Essence.EARTH, ModItems.DRAIN_VALVE);
    public static final ResearchTopic CIRCLE = add("hextech_altar", 1, Essence.SPARK, Essence.EARTH,
            ModItems.HEXTECH_ALTAR, ModItems.PEDESTAL, ModItems.RUNE_STELE);
    public static final ResearchTopic BIG_ESSENCE_JAR = add("big_essence_jar", 1, Essence.WATER, Essence.LIGHT, ModItems.ESSENCE_TANK);
    public static final ResearchTopic LAPIDARY = add("hex_lapidary", 1, Essence.EARTH, Essence.SPARK, ModItems.HEX_LAPIDARY,
            ModItems.CUTTING_DISC);
    public static final ResearchTopic HEXSTEEL = add("hexsteel", 1, Essence.METAL, Essence.FIRE, ModItems.HEXSTEEL_INGOT);
    public static final ResearchTopic MASTER_DISC = add("master_cutting_disc", 2, Essence.SPARK, Essence.LIGHT, ModItems.MASTER_CUTTING_DISC);
    public static final ResearchTopic HEXTECH_LAMP = add("hextech_lamp", 2, Essence.LIGHT, Essence.FIRE, ModItems.HEXTECH_LAMP);
    public static final ResearchTopic HEXTECH_PICKAXE = add("hextech_pickaxe", 2, Essence.METAL, Essence.EARTH,
            ModItems.pickaxe(HextechPickaxe.HEXTECH));
    public static final ResearchTopic GATHERING_PYLON = add("gathering_pylon", 2, Essence.AIR, Essence.SPARK, ModItems.GATHERING_PYLON);
    public static final ResearchTopic SENDING_PYLON = add("sending_pylon", 2, Essence.AIR, Essence.METAL, ModItems.SENDING_PYLON);
    public static final ResearchTopic RELAY_PYLON = add("relay_pylon", 2, Essence.SPARK, Essence.METAL, ModItems.RELAY_PYLON);
    public static final ResearchTopic SIPHON_PYLON = add("siphon_pylon", 3, Essence.WATER, Essence.AIR, ModItems.SIPHON_PYLON);
    public static final ResearchTopic REFINED_PICKAXE = add("refined_hextech_pickaxe", 3, Essence.METAL, Essence.SPARK,
            ModItems.pickaxe(HextechPickaxe.REFINED));
    public static final ResearchTopic RADIANT_PICKAXE = add("radiant_hextech_pickaxe", 3, Essence.LIGHT, Essence.METAL,
            ModItems.pickaxe(HextechPickaxe.RADIANT));

    private ResearchTopics() {
    }

    @SafeVarargs
    private static ResearchTopic add(String id, int tier, Essence action, Essence target, Supplier<? extends ItemLike>... items) {
        ResearchTopic topic = new ResearchTopic(id, List.of(items), action, target, tier);
        ALL.add(topic);
        return topic;
    }

    public static List<ResearchTopic> all() {
        return Collections.unmodifiableList(ALL);
    }

    @Nullable
    public static ResearchTopic byId(String id) {
        for (ResearchTopic topic : ALL) {
            if (topic.id().equals(id)) {
                return topic;
            }
        }
        return null;
    }

    @Nullable
    public static ResearchTopic byIndex(int index) {
        return index >= 0 && index < ALL.size() ? ALL.get(index) : null;
    }

    /**
     * The topic that must be researched before this item can be made, or null (free to make). The Big Essence Jar is a
     * structure, not an item: {@link #BIG_ESSENCE_JAR} guards its forming, never the jar's own recipe.
     */
    @Nullable
    public static ResearchTopic of(Item item) {
        for (ResearchTopic topic : ALL) {
            if (topic != BIG_ESSENCE_JAR && topic.opens(item)) {
                return topic;
            }
        }
        return null;
    }

    /** Whether this researcher may make the item: no topic guards it, it is researched, or they play in creative. */
    public static boolean mayMake(Player player, ItemStack stack, RuneKnowledge knowledge) {
        ResearchTopic topic = stack.isEmpty() ? null : of(stack.getItem());
        return topic == null || player.getAbilities().instabuild || knowledge.researched(topic);
    }
}
