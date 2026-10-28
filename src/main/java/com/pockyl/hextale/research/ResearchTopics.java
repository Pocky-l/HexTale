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

import com.pockyl.hextale.chem.Brew;
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
 * Everything research teaches. The essence works come first and need no research (their ink is what research is written
 * with); every hextech device and tool past them is worked out on a parchment at the Research Table before it can be
 * crafted or made at the altar. Themes follow what the thing does: the circle is earth and lightning, the pylons air, the
 * alloys metal and fire. Chemtech - its devices and its brews - is worked out in the flask of the Lab Bench instead.
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
    public static final ResearchTopic PUMP = add("pump", 1, Essence.AIR, Essence.WATER, ModItems.PUMP);
    public static final ResearchTopic GOGGLES = add("researchers_goggles", 1, Essence.LIGHT, Essence.SHADOW, ModItems.RESEARCHERS_GOGGLES);
    public static final ResearchTopic RESEARCHER_ROBE = add("researcher_robe", 1, Essence.AIR, Essence.LIFE, ModItems.RESEARCHER_ROBE,
            ModItems.RESEARCHER_HOOD, ModItems.RESEARCHER_TROUSERS, ModItems.RESEARCHER_BOOTS);
    public static final ResearchTopic ESSENCE_STICKER = add("essence_sticker", 2, Essence.LIFE, Essence.WATER, ModItems.ESSENCE_STICKER);
    public static final ResearchTopic PYLON_FILTER = add("pylon_filter", 2, Essence.AIR, Essence.LIGHT, ModItems.PYLON_FILTER);
    public static final ResearchTopic CIRCLE = add("hextech_altar", 1, Essence.SPARK, Essence.EARTH,
            ModItems.HEXTECH_ALTAR, ModItems.PEDESTAL, ModItems.RUNE_STELE);
    public static final ResearchTopic BIG_ESSENCE_JAR = add("big_essence_jar", 1, Essence.WATER, Essence.LIGHT, ModItems.ESSENCE_TANK);
    public static final ResearchTopic LAPIDARY = add("hex_lapidary", 1, Essence.EARTH, Essence.SPARK, ModItems.HEX_LAPIDARY,
            ModItems.CUTTING_DISC);
    public static final ResearchTopic HEXSTEEL = add("hexsteel", 1, Essence.METAL, Essence.FIRE, ModItems.HEXSTEEL_INGOT,
            ModItems.HEXSTEEL_SWORD, ModItems.HEXSTEEL_PICKAXE, ModItems.HEXSTEEL_AXE, ModItems.HEXSTEEL_SHOVEL, ModItems.HEXSTEEL_HOE,
            ModItems.HEXSTEEL_HELMET, ModItems.HEXSTEEL_CHESTPLATE, ModItems.HEXSTEEL_LEGGINGS, ModItems.HEXSTEEL_BOOTS);
    public static final ResearchTopic MASTER_DISC = add("master_cutting_disc", 2, Essence.SPARK, Essence.LIGHT, ModItems.MASTER_CUTTING_DISC);
    public static final ResearchTopic HEXTECH_LAMP = add("hextech_lamp", 2, Essence.LIGHT, Essence.FIRE, ModItems.HEXTECH_LAMP);
    public static final ResearchTopic CRYSTAL_BATTERY = add("crystal_battery", 2, Essence.SPARK, Essence.METAL, ModItems.CRYSTAL_BATTERY);
    public static final ResearchTopic CHEM_BURNER = lab("chem_burner", 1, Essence.FIRE, Essence.AIR, ModItems.CHEM_BURNER);
    public static final ResearchTopic MIASMA_VENT = lab("miasma_vent", 2, Essence.DECAY, Essence.AIR, ModItems.MIASMA_VENT);
    public static final ResearchTopic MAGIC_TREE = lab("magic_sapling", 2, Essence.LIFE, Essence.LIGHT, ModItems.MAGIC_SAPLING);
    public static final ResearchTopic AXOLOTL_TANK = lab("axolotl_tank", 1, Essence.WATER, Essence.LIFE, ModItems.AXOLOTL_TANK);
    public static final ResearchTopic CHEM_STILL = lab("chem_still", 2, Essence.FIRE, Essence.EARTH, ModItems.CHEM_STILL);
    public static final ResearchTopic PINK_BRAZIER = lab("pink_brazier", 2, Essence.FIRE, Essence.SPARK, ModItems.PINK_BRAZIER);
    public static final ResearchTopic SYNTHETIC_CRYSTAL = lab("synthetic_crystal", 2, Essence.LIGHT, Essence.EARTH, ModItems.SYNTHETIC_CRYSTAL);
    // Chemtech reactions: each brew is worked out at the Lab Bench before the Crucible brews it for the researcher.
    public static final ResearchTopic COMBUSTIBLE_MIX = brew("combustible_mix", 1, Brew.FUEL, Essence.FIRE, Essence.SHADOW);
    public static final ResearchTopic GROWTH_TONIC = brew("growth_tonic", 2, Brew.GROWTH, Essence.LIFE, Essence.LIGHT);
    public static final ResearchTopic COOLANT = brew("coolant", 2, Brew.COOLANT, Essence.WATER, Essence.SHADOW);
    public static final ResearchTopic GLOW_MIX = brew("glow_mix", 2, Brew.GLOW, Essence.WATER, Essence.LIFE);
    public static final ResearchTopic ACID = brew("acid", 2, Brew.ACID, Essence.SHADOW, Essence.WATER);
    public static final ResearchTopic RAW_SHIMMER = brew("raw_shimmer", 3, Brew.RAW_SHIMMER, Essence.LIGHT, Essence.LIFE);
    public static final ResearchTopic HEXTECH_PICKAXE = add("hextech_pickaxe", 2, Essence.METAL, Essence.EARTH,
            ModItems.pickaxe(HextechPickaxe.HEXTECH));
    public static final ResearchTopic HEXTECH_AXE = add("hextech_axe", 2, Essence.METAL, Essence.LIFE, ModItems.HEXTECH_AXE);
    public static final ResearchTopic HEXTECH_SHOVEL = add("hextech_shovel", 2, Essence.EARTH, Essence.METAL, ModItems.HEXTECH_SHOVEL);
    public static final ResearchTopic HEXTECH_SWORD = add("hextech_sword", 2, Essence.FIRE, Essence.METAL, ModItems.HEXTECH_SWORD);
    public static final ResearchTopic HEX_FOUNDRY = add("hex_foundry", 2, Essence.FIRE, Essence.EARTH, ModItems.HEX_FOUNDRY);
    public static final ResearchTopic WHIRL_REAPER = add("whirl_reaper", 1, Essence.AIR, Essence.LIFE, ModItems.WHIRL_REAPER);
    public static final ResearchTopic HEX_SAWMILL = add("hex_sawmill", 1, Essence.METAL, Essence.LIFE, ModItems.HEX_SAWMILL);
    public static final ResearchTopic GREEDY_FUNNEL = add("greedy_funnel", 2, Essence.SHADOW, Essence.LIFE, ModItems.GREEDY_FUNNEL);
    public static final ResearchTopic ARC_SPIRE = add("arc_spire", 2, Essence.SPARK, Essence.SHADOW, ModItems.ARC_SPIRE);
    public static final ResearchTopic HEXTECH_PURIFIER = add("hextech_purifier", 2, Essence.LIGHT, Essence.SHADOW, ModItems.HEXTECH_PURIFIER);
    public static final ResearchTopic RUNE_SHACKLES = add("rune_shackles", 2, Essence.SHADOW, Essence.EARTH, ModItems.RUNE_SHACKLES);
    public static final ResearchTopic XP_CONDENSER = add("xp_condenser", 2, Essence.LIGHT, Essence.LIFE, ModItems.XP_CONDENSER);
    public static final ResearchTopic FIELD_WARD = add("field_ward", 1, Essence.LIGHT, Essence.EARTH, ModItems.FIELD_WARD);
    public static final ResearchTopic GATHERING_PYLON = add("gathering_pylon", 2, Essence.AIR, Essence.SPARK, ModItems.GATHERING_PYLON);
    public static final ResearchTopic SENDING_PYLON = add("sending_pylon", 2, Essence.AIR, Essence.METAL, ModItems.SENDING_PYLON);
    public static final ResearchTopic RELAY_PYLON = add("relay_pylon", 2, Essence.SPARK, Essence.METAL, ModItems.RELAY_PYLON);
    public static final ResearchTopic SIPHON_PYLON = add("siphon_pylon", 3, Essence.WATER, Essence.AIR, ModItems.SIPHON_PYLON);
    public static final ResearchTopic SORTING_MATRIX = add("sorting_matrix", 3, Essence.LIGHT, Essence.METAL, ModItems.SORTING_MATRIX);
    public static final ResearchTopic HEXCORE = add("hexcore", 3, Essence.SPARK, Essence.LIGHT, ModItems.HEXCORE);
    public static final ResearchTopic HEXTECH_SKYPACK = add("hextech_skypack", 3, Essence.AIR, Essence.SPARK, ModItems.HEXTECH_SKYPACK);
    public static final ResearchTopic REFINED_PICKAXE = add("refined_hextech_pickaxe", 3, Essence.METAL, Essence.SPARK,
            ModItems.pickaxe(HextechPickaxe.REFINED));
    public static final ResearchTopic RADIANT_PICKAXE = add("radiant_hextech_pickaxe", 3, Essence.LIGHT, Essence.METAL,
            ModItems.pickaxe(HextechPickaxe.RADIANT));

    private ResearchTopics() {
    }

    @SafeVarargs
    private static ResearchTopic add(String id, int tier, Essence action, Essence target, Supplier<? extends ItemLike>... items) {
        return register(new ResearchTopic(id, List.of(items), action, target, tier, ResearchTopic.Bench.TABLE, null));
    }

    /** A chemtech device, worked out at the Lab Bench. */
    @SafeVarargs
    private static ResearchTopic lab(String id, int tier, Essence action, Essence target, Supplier<? extends ItemLike>... items) {
        return register(new ResearchTopic(id, List.of(items), action, target, tier, ResearchTopic.Bench.LAB, null));
    }

    /** A chemtech reaction: brewing it in the Crucible, worked out at the Lab Bench. */
    private static ResearchTopic brew(String id, int tier, Brew brew, Essence action, Essence target) {
        return register(new ResearchTopic(id, List.of(), action, target, tier, ResearchTopic.Bench.LAB, brew));
    }

    private static ResearchTopic register(ResearchTopic topic) {
        ALL.add(topic);
        return topic;
    }

    public static List<ResearchTopic> all() {
        return Collections.unmodifiableList(ALL);
    }

    /** The topics worked out at the Research Table (they have parchment boards). */
    public static List<ResearchTopic> table() {
        return ALL.stream().filter(topic -> !topic.lab()).toList();
    }

    /** The chemtech topics worked out at the Lab Bench, in the order its journal lists them. */
    public static List<ResearchTopic> lab() {
        return ALL.stream().filter(ResearchTopic::lab).toList();
    }

    /** The reaction topic that opens brewing this brew, or null (a brew no recipe makes, like refined shimmer). */
    @Nullable
    public static ResearchTopic forBrew(Brew brew) {
        for (ResearchTopic topic : ALL) {
            if (topic.brew() == brew) {
                return topic;
            }
        }
        return null;
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

    /** Whether this researcher may make the item: no topic guards it or it is researched. */
    public static boolean mayMake(Player player, ItemStack stack, RuneKnowledge knowledge) {
        ResearchTopic topic = stack.isEmpty() ? null : of(stack.getItem());
        return topic == null || knowledge.researched(topic);
    }
}
