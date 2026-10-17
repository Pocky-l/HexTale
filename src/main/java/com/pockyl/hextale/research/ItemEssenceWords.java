package com.pockyl.hextale.research;

import com.pockyl.hextale.rune.Essence;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.pockyl.hextale.rune.Essence.*;

/**
 * What the words of an item's id say about its essences: "deepslate" brings earth and depths, "honey" sweetness and
 * sap, "sword" weapon. Raw materials take their essences from these words alone; crafted things get them on top of what
 * their ingredients pass on (a sword is iron and wood, and also a weapon). Amounts matter: a word gives as much of an
 * essence as fits - lava more fire than a torch. A token matches a word when it equals it, its plural, or (for words of
 * four letters or more) contains it.
 */
final class ItemEssenceWords {
    static final Map<String, Object[]> WORDS = new LinkedHashMap<>();

    private ItemEssenceWords() {
    }

    private static void word(String word, Object... pairs) {
        WORDS.put(word, pairs);
    }

    static {
        // ---- Stone, soil and the deep ----
        word("stone", EARTH, 4, DUST, 1);
        word("cobblestone", EARTH, 4, DUST, 2);
        word("deepslate", EARTH, 4, DEPTHS, 3);
        word("tuff", EARTH, 3, ASH, 2, DUST, 1);
        word("calcite", EARTH, 2, CRYSTAL, 2, BONE, 1);
        word("granite", EARTH, 3, MAGMA, 1, CRYSTAL, 1);
        word("diorite", EARTH, 3, CRYSTAL, 1, PURITY, 1);
        word("andesite", EARTH, 3, DUST, 1);
        word("dirt", SOIL, 4, EARTH, 2);
        word("rooted", SOIL, 2, PLANT, 1);
        word("farmland", SOIL, 4, WATER, 1);
        word("grass", PLANT, 3, SOIL, 2);
        word("podzol", SOIL, 3, DECAY, 1, PLANT, 1);
        word("mycelium", SOIL, 2, DECAY, 2, PLANT, 1);
        word("gravel", EARTH, 3, DUST, 2);
        word("sand", DUST, 4, EARTH, 1, SUN, 1);
        word("sandstone", DUST, 3, EARTH, 2);
        word("brick", EARTH, 2, ORDER, 1, CRAFT, 1);
        word("terracotta", CLAY, 4, FIRE, 1);
        word("glazed", GLASS, 1, DYE, 2);
        word("concrete", DUST, 2, EARTH, 2, ORDER, 1);
        word("powder", DUST, 3);
        word("obsidian", MAGMA, 3, DEPTHS, 2, PROTECTION, 2, EARTH, 2);
        word("crying", RIFT, 2, MAGIC, 1);
        word("ore", ORE, 5, EARTH, 2, DEPTHS, 1);
        word("raw", ORE, 4, METAL, 2);
        word("diamond", GEM, 6, CRYSTAL, 3, GLEAM, 2, EARTH, 1);
        word("emerald", GEM, 5, LIFE, 2, CRAFT, 1);
        word("amethyst", CRYSTAL, 4, SPARK, 2, GLEAM, 1, GEM, 1);
        word("lapis", GEM, 2, MAGIC, 2, WATER, 1);
        word("quartz", CRYSTAL, 3, HELL, 1, PURITY, 1);
        word("flint", EARTH, 2, FIRE, 1, TOOL, 1);
        word("bedrock", EARTH, 8, ORDER, 4, DEPTHS, 4);
        word("dripstone", EARTH, 2, WATER, 1, CRYSTAL, 1, QUAKE, 1);
        word("ancient", HELL, 3, ORE, 3, PROTECTION, 2, TIME, 1);
        word("debris", HELL, 2, ORE, 2);
        word("netherrack", HELL, 3, MAGMA, 1, EARTH, 1);
        word("nether", HELL, 2);
        word("basalt", MAGMA, 2, EARTH, 2, ASH, 1);
        word("blackstone", HELL, 2, EARTH, 2, SHADOW, 1);
        word("magma", MAGMA, 4, FIRE, 2);
        word("lava", MAGMA, 6, FIRE, 4);
        word("soul", SOUL, 4, DEPTHS, 1, DECAY, 1);
        word("crimson", HELL, 2, PLANT, 2, BLOOD, 2);
        word("warped", HELL, 2, PLANT, 2, RIFT, 1);
        word("sherd", CLAY, 2, KNOWLEDGE, 1, TIME, 2);
        word("clay", CLAY, 4, WATER, 1);
        word("mud", CLAY, 3, WATER, 2, SOIL, 1);
        word("packed", ORDER, 1);
        word("moss", PLANT, 3, SOIL, 2, SAP, 1);
        word("suspicious", TIME, 2, DUST, 2);
        word("cobbled", EARTH, 1);
        word("polished", ORDER, 1, GLEAM, 1);
        word("chiseled", CRAFT, 1, ORDER, 1);
        word("infested", CORRUPTION, 3, BEAST, 1);
        word("end", ENDER, 2, SKY, 1, SILENCE, 1);

        // ---- Water, ice and the sea ----
        word("water", WATER, 4, PURITY, 1);
        word("wet", WATER, 2, MIST, 1);
        word("ice", ICE, 4, COLD, 2);
        word("blue", COLD, 1);
        word("snow", COLD, 3, ICE, 1, AIR, 1);
        word("prismarine", OCEAN, 4, CRYSTAL, 2);
        word("sea", OCEAN, 3);
        word("coral", OCEAN, 3, LIFE, 2, BONE, 1);
        word("kelp", OCEAN, 2, PLANT, 2, SAP, 1, BREATH, 1);
        word("dried", SUN, 1, AIR, 1);
        word("cod", OCEAN, 2, FLESH, 3, FOOD, 1);
        word("salmon", OCEAN, 2, FLESH, 3, FOOD, 1);
        word("fish", OCEAN, 2, FLESH, 2, FOOD, 1);
        word("tropical", OCEAN, 2, FLESH, 2, DYE, 1);
        word("pufferfish", OCEAN, 2, VENOM, 3, FLESH, 1);
        word("sponge", OCEAN, 2, WATER, 2, LIFE, 1);
        word("squid", OCEAN, 2, SHADOW, 2, FLESH, 1);
        word("ink", SHADOW, 3, WATER, 1, KNOWLEDGE, 1, DYE, 1);
        word("turtle", OCEAN, 3, PROTECTION, 3, BREATH, 2, TIME, 1);
        word("scute", PROTECTION, 3, OCEAN, 1, BEAST, 1);
        word("nautilus", OCEAN, 3, BONE, 2, ECHO, 1);
        word("shell", BONE, 2, OCEAN, 1, PROTECTION, 1);
        word("heart", OCEAN, 6, MAGIC, 3, PURITY, 2, LIFE, 2);
        word("trident", OCEAN, 4, WEAPON, 4, STORM, 3, THUNDER, 1);
        word("conduit", OCEAN, 5, ENERGY, 3, MAGIC, 2, BREATH, 2);
        word("axolotl", SAP, 2, LIFE, 3, OCEAN, 1);
        word("frog", SAP, 2, BEAST, 1, WATER, 1);
        word("tadpole", SAP, 2, LIFE, 2, WATER, 1);
        word("lily", PLANT, 2, WATER, 2);
        word("dripleaf", PLANT, 2, WATER, 1, MIST, 1);
        word("bucket", METAL, 3, CRAFT, 1);
        word("cauldron", METAL, 3, STEAM, 2, WATER, 1);
        word("bubble", WATER, 1, BREATH, 1);
        word("fishing", TOOL, 1, OCEAN, 1);

        // ---- Air, sky and sound ----
        word("feather", AIR, 3, WIND, 2, BEAST, 1);
        word("phantom", DREAM, 3, AIR, 2, DECAY, 1, SKY, 1);
        word("membrane", SKY, 2, FLESH, 1);
        word("string", FIBER, 3, BEAST, 1);
        word("cobweb", FIBER, 3, SILENCE, 1, SHADOW, 1);
        word("wool", FIBER, 3, BEAST, 2, CLOTH, 1, SILENCE, 1);
        word("carpet", CLOTH, 3, FIBER, 1, SILENCE, 1);
        word("bed", CLOTH, 2, DREAM, 3, CRAFT, 1);
        word("wind", WIND, 5, AIR, 3, MOTION, 2);
        word("breeze", WIND, 5, AIR, 3, STORM, 1, MOTION, 1);
        word("bamboo", PLANT, 3, WOOD, 1, AIR, 1);
        word("glass", GLASS, 4, PURITY, 1);
        word("pane", GLASS, 1);
        word("tinted", SHADOW, 2, ILLUSION, 2);
        word("bell", METAL, 2, ECHO, 4, GOLD, 1);
        word("horn", ECHO, 3, BEAST, 2, WIND, 1, THUNDER, 1);
        word("disc", ECHO, 3, MAGIC, 1, ORDER, 1);
        word("jukebox", ECHO, 3, WOOD, 1, MECHANISM, 1);
        word("note", ECHO, 3, WOOD, 1);
        word("firework", BLAST, 3, SKY, 2, DYE, 1);
        word("elytra", SKY, 6, WIND, 4, ENDER, 2);
        word("spyglass", SKY, 2, GLASS, 2, KNOWLEDGE, 1);
        word("spore", MIST, 3, PLANT, 1, LIFE, 1);
        word("breath", BREATH, 4, MAGIC, 1);
        word("sculk", ECHO, 4, DEPTHS, 2, SOUL, 2);
        word("echo", ECHO, 6, DEPTHS, 3, SILENCE, 2);
        word("shrieker", ECHO, 2, SILENCE, 1);
        word("sensor", ECHO, 2, KNOWLEDGE, 1);

        // ---- Light ----
        word("torch", FIRE, 2, LIGHT, 2, WOOD, 1);
        word("lantern", LIGHT, 3, METAL, 1, FIRE, 1);
        word("beacon", LIGHT, 6, STAR, 4, MAGIC, 3, ORDER, 2, GLASS, 1);
        word("gold", GOLD, 4, GLEAM, 2);
        word("golden", GOLD, 4, GLEAM, 2, MAGIC, 1);
        word("candle", FIRE, 1, LIGHT, 2, SWEET, 1);
        word("shroomlight", LIGHT, 3, HELL, 1, PLANT, 1);
        word("froglight", LIGHT, 3, SAP, 1);
        word("sunflower", SUN, 3, PLANT, 2);
        word("daylight", SUN, 3, CIRCUIT, 2);
        word("light", LIGHT, 2);
        word("glow", LIGHT, 3, ENERGY, 1);
        word("glowstone", SUN, 3, LIGHT, 3, HELL, 1);
        word("lichen", PLANT, 1, DREAM, 1);
        word("white", PURITY, 1);
        word("bulb", LIGHT, 2, CIRCUIT, 1);
        word("jack", LIGHT, 2, SOUL, 1, ILLUSION, 1);

        // ---- Shadow, death and the End ----
        word("spider", BEAST, 2, VENOM, 2, SHADOW, 1);
        word("rotten", DECAY, 4, FLESH, 2, CORRUPTION, 1);
        word("flesh", FLESH, 4, BLOOD, 1);
        word("bone", BONE, 4, DECAY, 1);
        word("meal", BONE, 2, DUST, 1, SOIL, 1);
        word("skull", BONE, 3, SOUL, 2, DECAY, 1);
        word("head", SOUL, 2, BEAST, 1);
        word("wither", DECAY, 4, SOUL, 3, CHAOS, 2, CORRUPTION, 2);
        word("zombie", DECAY, 2, CORRUPTION, 2, FLESH, 1);
        word("ender", ENDER, 4, RIFT, 2);
        word("pearl", RIFT, 3, MOTION, 2, ENDER, 1);
        word("eye", KNOWLEDGE, 2, SKY, 1);
        word("chorus", ENDER, 2, PLANT, 2, RIFT, 1, DREAM, 1);
        word("purpur", ENDER, 2, EARTH, 2, ORDER, 1);
        word("shulker", ENDER, 3, PROTECTION, 2, CRAFT, 1);
        word("dragon", ENDER, 4, FURY, 4, MAGIC, 3, SKY, 2);
        word("crystal", CRYSTAL, 4, ENERGY, 2, MAGIC, 1);
        word("gunpowder", BLAST, 4, DUST, 2);
        word("tnt", BLAST, 6, CHAOS, 2, QUAKE, 2);
        word("creeper", BLAST, 3, BEAST, 1, CHAOS, 1);
        word("charge", BLAST, 2, FURY, 1, SPARK, 1);
        word("black", SHADOW, 1);
        word("dark", SHADOW, 1);
        word("fermented", DECAY, 2, MAGIC, 1, ILLUSION, 1);
        word("poisonous", VENOM, 3, CORRUPTION, 1);
        word("ominous", CORRUPTION, 3, CHAOS, 2);
        word("ghast", SOUL, 2, HELL, 2, FURY, 1);
        word("tear", PURITY, 2, SPIRIT, 2);
        word("blaze", FIRE, 4, HELL, 2, FURY, 2, VIGOR, 1);
        word("fire", FIRE, 3);
        word("charcoal", FIRE, 3, WOOD, 1, ASH, 1);
        word("coal", FIRE, 3, DEPTHS, 1, ASH, 1);

        // ---- Life: plants, animals, food ----
        word("log", WOOD, 4, PLANT, 1, SAP, 1);
        word("wood", WOOD, 4, PLANT, 1);
        word("stem", WOOD, 3, HELL, 1);
        word("hyphae", WOOD, 3, HELL, 1);
        word("planks", WOOD, 3, CRAFT, 1);
        word("stripped", WOOD, 1, CRAFT, 1);
        word("stick", WOOD, 1);
        word("bowl", WOOD, 1);
        word("sapling", SEED, 3, PLANT, 2);
        word("propagule", SEED, 3, OCEAN, 1);
        word("leaves", PLANT, 4, AIR, 1, SAP, 1);
        word("flower", PLANT, 3, DYE, 2);
        word("tulip", PLANT, 3, DYE, 2);
        word("orchid", PLANT, 3, DYE, 2);
        word("allium", PLANT, 3, DYE, 2);
        word("dandelion", PLANT, 3, DYE, 1, SUN, 1);
        word("cornflower", PLANT, 3, DYE, 2);
        word("daisy", PLANT, 3, DYE, 1, PURITY, 1);
        word("bluet", PLANT, 3, DYE, 1, WATER, 1);
        word("lilac", PLANT, 3, DYE, 2);
        word("peony", PLANT, 3, DYE, 2);
        word("poppy", PLANT, 3, DYE, 2, BLOOD, 1);
        word("rose", PLANT, 3, DYE, 2, BLOOD, 1);
        word("torchflower", PLANT, 3, SUN, 2);
        word("pitcher", PLANT, 3, TIME, 1);
        word("pod", SEED, 3, TIME, 1);
        word("seeds", SEED, 4);
        word("beans", SEED, 2);
        word("wheat", PLANT, 3, FOOD, 2, SEED, 1, SUN, 1);
        word("carrot", FOOD, 3, PLANT, 2);
        word("potato", FOOD, 3, SOIL, 1);
        word("beetroot", FOOD, 2, PLANT, 2, BLOOD, 1);
        word("melon", FOOD, 2, SAP, 2, SWEET, 1);
        word("glistering", VIGOR, 3, MAGIC, 1, GOLD, 1);
        word("pumpkin", FOOD, 2, PLANT, 2);
        word("carved", CRAFT, 1, ILLUSION, 1);
        word("apple", FOOD, 3, SWEET, 2);
        word("berries", FOOD, 2, SWEET, 2);
        word("beef", FLESH, 4, FOOD, 2, BLOOD, 1);
        word("porkchop", FLESH, 4, FOOD, 2, BLOOD, 1);
        word("mutton", FLESH, 4, FOOD, 2, BLOOD, 1);
        word("chicken", FLESH, 3, FOOD, 2, AIR, 1);
        word("rabbit", FLESH, 3, FOOD, 1, MOTION, 1, VIGOR, 1);
        word("foot", MOTION, 2, MAGIC, 1);
        word("egg", LIFE, 3, FOOD, 1, BEAST, 1);
        word("spawn", LIFE, 4, MAGIC, 2);
        word("leather", BEAST, 3, PROTECTION, 1);
        word("hide", BEAST, 3, PROTECTION, 1);
        word("honey", SWEET, 5, SAP, 2);
        word("honeycomb", SWEET, 3, ORDER, 2);
        word("slime", SLIME, 5, LIFE, 1);
        word("cream", SLIME, 2, MAGMA, 1);
        word("mushroom", DECAY, 2, PLANT, 2, SOIL, 1);
        word("fungus", HELL, 1, DECAY, 2, PLANT, 2);
        word("vine", PLANT, 3);
        word("vines", PLANT, 3);
        word("azalea", PLANT, 3, DYE, 1);
        word("cactus", PLANT, 3, VENOM, 1, SUN, 1, SAP, 1);
        word("cane", PLANT, 2, SWEET, 1, SAP, 1);
        word("sugar", SWEET, 4, DUST, 1, MOTION, 1, VIGOR, 1);
        word("totem", LIFE, 5, SPIRIT, 4, MAGIC, 3, TIME, 2);
        word("cocoa", FOOD, 1, SWEET, 2, SUN, 1, VIGOR, 1);
        word("milk", FOOD, 2, PURITY, 2, BEAST, 1);
        word("potion", PURITY, 2, MAGIC, 2, WATER, 1, STEAM, 1);
        word("dye", DYE, 4);
        word("paper", FIBER, 2, KNOWLEDGE, 2);
        word("book", KNOWLEDGE, 4, CLOTH, 1);
        word("enchanted", MAGIC, 4, KNOWLEDGE, 1, GLEAM, 1);
        word("writable", KNOWLEDGE, 3);
        word("written", KNOWLEDGE, 3);
        word("map", KNOWLEDGE, 3, SKY, 1);
        word("wart", HELL, 2, PLANT, 2, MAGIC, 1, BLOOD, 1);
        word("sniffer", TIME, 3, BEAST, 2, SEED, 1);
        word("armadillo", PROTECTION, 3, BEAST, 2);
        word("hay", PLANT, 3, FOOD, 1, FIBER, 1);
        word("cake", SWEET, 5, FOOD, 3);
        word("cookie", SWEET, 3, FOOD, 1);
        word("pie", FOOD, 3, SWEET, 2);
        word("stew", FOOD, 4, WATER, 1);
        word("soup", FOOD, 4, WATER, 1);
        word("bread", FOOD, 4, PLANT, 1);
        word("cooked", FIRE, 1, FOOD, 1);

        // ---- Metal, craft and machines ----
        word("iron", METAL, 4, ORE, 1);
        word("ingot", FORGE, 1);
        word("nugget", METAL, 1);
        word("copper", METAL, 3, CIRCUIT, 1, RUST, 1, SPARK, 1);
        word("oxidized", RUST, 4);
        word("weathered", RUST, 3);
        word("exposed", RUST, 2);
        word("waxed", SWEET, 1, PROTECTION, 1);
        word("netherite", METAL, 6, HELL, 4, PROTECTION, 3);
        word("chain", METAL, 2, ORDER, 1);
        word("bars", ORDER, 2, METAL, 1);
        word("anvil", METAL, 5, FORGE, 3, CRAFT, 2);
        word("chipped", RUST, 2);
        word("damaged", RUST, 3);
        word("rail", METAL, 2, MOTION, 2, CIRCUIT, 1);
        word("powered", ENERGY, 2);
        word("minecart", METAL, 3, MOTION, 3, MECHANISM, 1);
        word("boat", WOOD, 2, MOTION, 2, OCEAN, 1);
        word("raft", WOOD, 2, MOTION, 2, OCEAN, 1);
        word("saddle", BEAST, 2, MOTION, 2, CLOTH, 1);
        word("lead", FIBER, 2, BEAST, 1, ORDER, 1);
        word("key", ORDER, 2, METAL, 1);
        word("trial", CHAOS, 2, ORDER, 2, METAL, 1);
        word("vault", ORDER, 3, METAL, 2);
        word("spawner", SOUL, 4, CHAOS, 2, METAL, 2);
        word("heavy", METAL, 5, ORDER, 3, QUAKE, 2);
        word("mace", WEAPON, 5, QUAKE, 4, WIND, 2);
        word("template", KNOWLEDGE, 2, CRAFT, 3);
        word("sword", WEAPON, 3);
        word("axe", TOOL, 2, WEAPON, 1);
        word("pickaxe", TOOL, 3, ORE, 1);
        word("shovel", TOOL, 2, SOIL, 1);
        word("hoe", TOOL, 2, SOIL, 1, PLANT, 1);
        word("shears", TOOL, 2, FIBER, 1);
        word("brush", TOOL, 2, TIME, 2, KNOWLEDGE, 1);
        word("bow", WEAPON, 3, WOOD, 1, MOTION, 1);
        word("crossbow", WEAPON, 3, MECHANISM, 2);
        word("arrow", WEAPON, 2, MOTION, 1);
        word("shield", PROTECTION, 4, WOOD, 1);
        word("helmet", PROTECTION, 3);
        word("chestplate", PROTECTION, 4);
        word("leggings", PROTECTION, 3);
        word("boots", PROTECTION, 2, MOTION, 1);
        word("armor", PROTECTION, 3);
        word("lodestone", ORDER, 2, ENERGY, 2, ORE, 2);
        word("compass", ORDER, 2, KNOWLEDGE, 1, ENERGY, 1);
        word("recovery", SOUL, 2, TIME, 2);
        word("clock", TIME, 4, GOLD, 1, MECHANISM, 1);
        word("redstone", CIRCUIT, 4, ENERGY, 2, DUST, 1);
        word("repeater", CIRCUIT, 3, TIME, 2);
        word("comparator", CIRCUIT, 3, ORDER, 2);
        word("piston", MECHANISM, 4, MOTION, 2, QUAKE, 1);
        word("sticky", SLIME, 2);
        word("observer", MECHANISM, 2, KNOWLEDGE, 1, CIRCUIT, 2);
        word("dispenser", MECHANISM, 3, MOTION, 1);
        word("dropper", MECHANISM, 3, MOTION, 1);
        word("hopper", MECHANISM, 3, METAL, 2);
        word("lightning", SPARK, 5, STORM, 3, THUNDER, 2, METAL, 1);
        word("target", CIRCUIT, 2, ORDER, 1);
        word("button", CIRCUIT, 2, MOTION, 1);
        word("lever", CIRCUIT, 2, MOTION, 1);
        word("plate", CIRCUIT, 2);
        word("tripwire", CIRCUIT, 1, FIBER, 1);
        word("crafter", CRAFT, 4, MECHANISM, 3);
        word("crafting", CRAFT, 4, WOOD, 2);
        word("furnace", FORGE, 3, FIRE, 2, EARTH, 2, SMOKE, 1);
        word("blast", FORGE, 4);
        word("smoker", SMOKE, 3, FOOD, 1);
        word("campfire", FIRE, 3, SMOKE, 2, WOOD, 1);
        word("brewing", MAGIC, 2, PURITY, 2, STEAM, 2);
        word("enchanting", MAGIC, 5, KNOWLEDGE, 3);
        word("bookshelf", KNOWLEDGE, 5, WOOD, 2);
        word("lectern", KNOWLEDGE, 3, WOOD, 2);
        word("loom", CLOTH, 3, CRAFT, 2);
        word("cartography", KNOWLEDGE, 3, SKY, 1);
        word("fletching", WEAPON, 2, AIR, 1);
        word("smithing", FORGE, 3, CRAFT, 3);
        word("grindstone", TOOL, 2, EARTH, 2, MOTION, 1);
        word("stonecutter", TOOL, 3, EARTH, 2);
        word("chest", WOOD, 3, ORDER, 2);
        word("barrel", WOOD, 3, ORDER, 2);
        word("experience", MAGIC, 3, ENERGY, 2, SPIRIT, 1);
        word("star", STAR, 8, MAGIC, 5, CHAOS, 3);
        word("rod", ENERGY, 1);
        word("pottery", CLAY, 2, CRAFT, 1);
        word("pot", CLAY, 2, PLANT, 1);
        word("painting", DYE, 2, CRAFT, 1, ILLUSION, 2);
        word("frame", CRAFT, 1, ORDER, 1);
        word("banner", CLOTH, 3, DYE, 2);
        word("stand", CRAFT, 2, PROTECTION, 1);
        word("name", KNOWLEDGE, 2, SOUL, 1);
        word("door", ORDER, 2);
        word("fence", ORDER, 2);
        word("gate", ORDER, 1, MOTION, 1);
        word("ladder", MOTION, 1, WOOD, 1);
        word("scaffolding", CRAFT, 2, MOTION, 1);
        word("sign", KNOWLEDGE, 2);
    }

    /** Words that bring the given essence, for tests and tooling. */
    static boolean mentions(Essence essence) {
        for (Object[] pairs : WORDS.values()) {
            for (int i = 0; i < pairs.length; i += 2) {
                if (pairs[i] == essence) {
                    return true;
                }
            }
        }
        return false;
    }
}
