package com.pockyl.hextale.research;

import com.pockyl.hextale.rune.Essence;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.pockyl.hextale.rune.Essence.*;

/**
 * What the words of an item's id say about its essences: "deepslate" brings earth and shadow, "honey" sap, "sword"
 * metal. A word brings one or two essences. Raw materials take their essences from these words alone; crafted things get them on top of what
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
        word("stone", EARTH, 4);
        word("cobblestone", EARTH, 4);
        word("deepslate", EARTH, 4, SHADOW, 2);
        word("tuff", EARTH, 3, ASH, 2);
        word("calcite", EARTH, 2, CRYSTAL, 2);
        word("granite", EARTH, 3, MAGMA, 1);
        word("diorite", EARTH, 3, CRYSTAL, 1);
        word("andesite", EARTH, 4);
        word("dirt", EARTH, 3, PLANT, 1);
        word("rooted", PLANT, 2);
        word("farmland", PLANT, 2, WATER, 1);
        word("grass", PLANT, 3);
        word("podzol", EARTH, 2, DECAY, 1);
        word("mycelium", PLANT, 3, DECAY, 2);
        word("gravel", EARTH, 4);
        word("sand", EARTH, 4);
        word("sandstone", EARTH, 4);
        word("brick", EARTH, 2, FIRE, 1);
        word("terracotta", EARTH, 4, FIRE, 1);
        word("glazed", CRYSTAL, 2, FIRE, 1);
        word("concrete", EARTH, 4);
        word("powder", EARTH, 3);
        word("obsidian", MAGMA, 3, EARTH, 2);
        word("crying", RIFT, 2, MAGIC, 1);
        word("ore", ORE, 4, EARTH, 1);
        word("raw", ORE, 3, METAL, 1);
        word("diamond", CRYSTAL, 9, LIGHT, 2);
        word("emerald", CRYSTAL, 5, LIFE, 2);
        word("amethyst", CRYSTAL, 5, SPARK, 2);
        word("lapis", MAGIC, 3, CRYSTAL, 1);
        word("quartz", CRYSTAL, 3, MAGMA, 1);
        word("flint", EARTH, 2, FIRE, 1);
        word("bedrock", EARTH, 8, SHADOW, 4);
        word("dripstone", EARTH, 3, WATER, 1);
        word("ancient", MAGMA, 3, ORE, 3);
        word("debris", MAGMA, 2, ORE, 2);
        word("netherrack", MAGMA, 3, EARTH, 1);
        word("nether", MAGMA, 2);
        word("basalt", MAGMA, 2, ASH, 1);
        word("blackstone", EARTH, 2, SHADOW, 1);
        word("magma", MAGMA, 4, FIRE, 2);
        word("lava", MAGMA, 6, FIRE, 4);
        word("soul", SHADOW, 3, DECAY, 1);
        word("crimson", MAGMA, 2, PLANT, 2);
        word("warped", MAGMA, 2, PLANT, 2);
        word("sherd", EARTH, 3, MAGIC, 1);
        word("clay", EARTH, 3, WATER, 2);
        word("mud", EARTH, 3, WATER, 2);
        word("moss", PLANT, 3, SAP, 1);
        word("suspicious", EARTH, 2, MAGIC, 1);
        word("cobbled", EARTH, 1);
        word("polished", EARTH, 1);
        word("chiseled", EARTH, 1);
        word("infested", DECAY, 3, BEAST, 1);
        word("end", RIFT, 2, AIR, 1);

        // ---- Water, ice and the sea ----
        word("water", WATER, 5);
        word("wet", WATER, 3);
        word("ice", COLD, 4, WATER, 2);
        word("blue", WATER, 1);
        word("snow", WATER, 2, COLD, 2);
        word("prismarine", WATER, 4, CRYSTAL, 2);
        word("sea", WATER, 3);
        word("coral", WATER, 3, LIFE, 2);
        word("kelp", WATER, 3, PLANT, 1);
        word("dried", STEAM, 1);
        word("cod", BEAST, 3, WATER, 2);
        word("salmon", BEAST, 3, WATER, 2);
        word("fish", WATER, 2, BEAST, 2);
        word("tropical", WATER, 2, BEAST, 2);
        word("pufferfish", DECAY, 3, WATER, 2);
        word("sponge", WATER, 3);
        word("squid", WATER, 2, SHADOW, 2);
        word("ink", SHADOW, 3, WATER, 1);
        word("turtle", WATER, 3, BEAST, 2);
        word("scute", BEAST, 2, METAL, 2);
        word("nautilus", WATER, 3, DECAY, 2);
        word("shell", DECAY, 2, WATER, 1);
        word("heart", WATER, 6, MAGIC, 3);
        word("trident", WATER, 4, METAL, 4);
        word("conduit", MAGIC, 4, WATER, 4);
        word("axolotl", LIFE, 3, SAP, 2);
        word("frog", SAP, 2, WATER, 1);
        word("tadpole", LIFE, 2, SAP, 2);
        word("lily", WATER, 2, PLANT, 2);
        word("dripleaf", WATER, 2, PLANT, 2);
        word("bucket", METAL, 3);
        word("cauldron", METAL, 3, STEAM, 2);
        word("bubble", WATER, 1, BEAST, 1);
        word("fishing", WATER, 1, METAL, 1);

        // ---- Air, sky and sound ----
        word("feather", AIR, 3);
        word("phantom", AIR, 3, RIFT, 2);
        word("membrane", AIR, 2, BEAST, 1);
        word("string", BEAST, 2);
        word("cobweb", BEAST, 2, SHADOW, 2);
        word("wool", BEAST, 4);
        word("carpet", BEAST, 2);
        word("bed", BEAST, 2, RIFT, 2);
        word("wind", AIR, 6);
        word("breeze", AIR, 6, SPARK, 1);
        word("bamboo", PLANT, 4, AIR, 1);
        word("glass", CRYSTAL, 3);
        word("pane", CRYSTAL, 1);
        word("tinted", SHADOW, 2);
        word("bell", METAL, 3, AIR, 2);
        word("horn", BEAST, 3, AIR, 2);
        word("disc", AIR, 3, MAGIC, 1);
        word("jukebox", AIR, 3, CIRCUIT, 1);
        word("note", AIR, 3, PLANT, 1);
        word("firework", FIRE, 3, AIR, 2);
        word("elytra", AIR, 8, RIFT, 2);
        word("spyglass", AIR, 2, CRYSTAL, 2);
        word("spore", PLANT, 2, WATER, 1);
        word("breath", BEAST, 4, MAGIC, 1);
        word("sculk", SHADOW, 4, MAGIC, 1);
        word("echo", SHADOW, 6);
        word("shrieker", SHADOW, 3);
        word("sensor", SHADOW, 2, MAGIC, 1);

        // ---- Light ----
        word("torch", LIGHT, 2, FIRE, 1);
        word("lantern", LIGHT, 3, FIRE, 1);
        word("beacon", MAGIC, 7, LIGHT, 6);
        word("gold", METAL, 3, LIGHT, 2);
        word("golden", METAL, 3, LIGHT, 2);
        word("candle", LIGHT, 2, FIRE, 1);
        word("shroomlight", LIGHT, 3, MAGMA, 1);
        word("froglight", LIGHT, 3, SAP, 1);
        word("sunflower", LIGHT, 3, PLANT, 2);
        word("daylight", LIGHT, 3, CIRCUIT, 2);
        word("light", LIGHT, 2);
        word("glow", LIGHT, 3, MAGIC, 1);
        word("glowstone", LIGHT, 5);
        word("lichen", PLANT, 1, LIGHT, 1);
        word("white", LIGHT, 1);
        word("bulb", LIGHT, 2, CIRCUIT, 1);
        word("jack", LIGHT, 2, SHADOW, 1);

        // ---- Shadow, death and the End ----
        word("spider", BEAST, 2, DECAY, 2);
        word("rotten", DECAY, 5, BEAST, 2);
        word("flesh", BEAST, 5);
        word("bone", DECAY, 4);
        word("meal", DECAY, 2, LIFE, 1);
        word("skull", DECAY, 4, SHADOW, 2);
        word("head", BEAST, 2, DECAY, 1);
        word("wither", DECAY, 6, SHADOW, 3);
        word("zombie", DECAY, 4, BEAST, 1);
        word("ender", RIFT, 4);
        word("pearl", RIFT, 3, AIR, 1);
        word("eye", MAGIC, 2);
        word("chorus", RIFT, 4, PLANT, 2);
        word("purpur", EARTH, 2, RIFT, 2);
        word("shulker", RIFT, 3, METAL, 2);
        word("dragon", RIFT, 4, MAGIC, 3);
        word("crystal", CRYSTAL, 4, MAGIC, 3);
        word("gunpowder", FIRE, 3, ASH, 2);
        word("tnt", FIRE, 5, ASH, 2);
        word("creeper", FIRE, 3, BEAST, 1);
        word("charge", FIRE, 3, SPARK, 1);
        word("black", SHADOW, 1);
        word("dark", SHADOW, 1);
        word("fermented", DECAY, 2, MAGIC, 1);
        word("poisonous", DECAY, 4);
        word("ominous", DECAY, 3, RIFT, 2);
        word("ghast", SHADOW, 2, MAGMA, 2);
        word("tear", WATER, 2, MAGIC, 2);
        word("blaze", FIRE, 6, MAGMA, 2);
        word("fire", FIRE, 3);
        word("charcoal", FIRE, 3, ASH, 1);
        word("coal", FIRE, 4);

        // ---- The Void's own matter ----
        word("void", RIFT, 3, SHADOW, 1);
        word("rift", RIFT, 4);
        word("husk", BEAST, 2, DECAY, 1);
        word("chitin", BEAST, 3, EARTH, 1);
        word("ichor", RIFT, 3, WATER, 2);
        word("sinew", BEAST, 3, LIFE, 1);
        word("cilia", PLANT, 2, RIFT, 1);
        word("gnawed", DECAY, 2, EARTH, 1);
        word("blister", BEAST, 2, AIR, 2);
        word("tendril", PLANT, 2, RIFT, 1);

        // ---- Life: plants, animals, food ----
        word("log", PLANT, 4, SAP, 1);
        word("wood", PLANT, 4);
        word("stem", PLANT, 3, MAGMA, 1);
        word("hyphae", PLANT, 3, MAGMA, 1);
        word("planks", PLANT, 2);
        word("stripped", PLANT, 1);
        word("stick", PLANT, 1);
        word("bowl", PLANT, 1);
        word("sapling", PLANT, 3, LIFE, 2);
        word("propagule", PLANT, 3, WATER, 1);
        word("leaves", PLANT, 3, AIR, 1);
        word("flower", PLANT, 3);
        word("tulip", PLANT, 3);
        word("orchid", PLANT, 3);
        word("allium", PLANT, 3);
        word("dandelion", PLANT, 3, LIGHT, 1);
        word("cornflower", PLANT, 3);
        word("daisy", PLANT, 3, LIGHT, 1);
        word("bluet", PLANT, 3, WATER, 1);
        word("lilac", PLANT, 3);
        word("peony", PLANT, 3);
        word("poppy", PLANT, 3, LIFE, 1);
        word("rose", PLANT, 3, LIFE, 1);
        word("torchflower", PLANT, 3, LIGHT, 2);
        word("pitcher", PLANT, 3, LIFE, 1);
        word("pod", PLANT, 3, LIFE, 1);
        word("seeds", LIFE, 3);
        word("beans", LIFE, 2);
        word("wheat", PLANT, 3, LIFE, 1);
        word("carrot", LIFE, 3, PLANT, 2);
        word("potato", LIFE, 3, PLANT, 1);
        word("beetroot", LIFE, 2, PLANT, 2);
        word("melon", SAP, 3, LIFE, 2);
        word("glistering", LIFE, 3, MAGIC, 1);
        word("pumpkin", LIFE, 2, PLANT, 2);
        word("carved", LIGHT, 1);
        word("apple", LIFE, 3, SAP, 2);
        word("berries", LIFE, 2, SAP, 2);
        word("beef", BEAST, 4, LIFE, 2);
        word("porkchop", BEAST, 4, LIFE, 2);
        word("mutton", BEAST, 4, LIFE, 2);
        word("chicken", BEAST, 3, LIFE, 2);
        word("rabbit", BEAST, 3, LIFE, 2);
        word("foot", AIR, 2, MAGIC, 1);
        word("egg", LIFE, 3, BEAST, 1);
        word("spawn", LIFE, 4, MAGIC, 2);
        word("leather", BEAST, 4);
        word("hide", BEAST, 4);
        word("honey", SAP, 5);
        word("honeycomb", SAP, 3);
        word("slime", SAP, 5, LIFE, 1);
        word("cream", SAP, 2, MAGMA, 1);
        word("mushroom", PLANT, 3, DECAY, 2);
        word("fungus", PLANT, 2, DECAY, 2);
        word("vine", PLANT, 3);
        word("vines", PLANT, 3);
        word("azalea", PLANT, 4);
        word("cactus", PLANT, 3, LIGHT, 1);
        word("cane", PLANT, 2, SAP, 2);
        word("sugar", SAP, 3);
        word("totem", MAGIC, 6, LIFE, 5);
        word("cocoa", LIFE, 2, SAP, 2);
        word("milk", LIFE, 2, WATER, 2);
        word("potion", STEAM, 2, MAGIC, 2);
        word("dye", PLANT, 2);
        word("paper", PLANT, 2, MAGIC, 1);
        word("book", MAGIC, 3);
        word("enchanted", MAGIC, 5, LIGHT, 1);
        word("writable", MAGIC, 3);
        word("written", MAGIC, 3);
        word("map", MAGIC, 2, AIR, 1);
        word("wart", MAGMA, 2, PLANT, 2);
        word("sniffer", BEAST, 3, PLANT, 1);
        word("armadillo", BEAST, 3, EARTH, 1);
        word("hay", PLANT, 4, LIFE, 1);
        word("cake", SAP, 5, LIFE, 3);
        word("cookie", SAP, 3, LIFE, 1);
        word("pie", LIFE, 3, SAP, 2);
        word("stew", LIFE, 4, WATER, 1);
        word("soup", LIFE, 4, WATER, 1);
        word("bread", LIFE, 4, PLANT, 1);
        word("cooked", FIRE, 1, LIFE, 1);

        // ---- Metal, craft and machines ----
        word("iron", METAL, 4);
        word("ingot", METAL, 1);
        word("nugget", METAL, 1);
        word("copper", METAL, 3, SPARK, 1);
        word("oxidized", ORE, 4);
        word("weathered", ORE, 3);
        word("exposed", ORE, 2);
        word("waxed", METAL, 1, SAP, 1);
        word("netherite", METAL, 9, MAGMA, 4);
        word("chain", METAL, 3);
        word("bars", METAL, 3);
        word("anvil", METAL, 8);
        word("chipped", METAL, 1);
        word("damaged", METAL, 1);
        word("rail", METAL, 2, AIR, 1);
        word("powered", SPARK, 2);
        word("minecart", AIR, 3, METAL, 3);
        word("boat", PLANT, 2, WATER, 1);
        word("raft", PLANT, 2, WATER, 1);
        word("saddle", BEAST, 3, AIR, 2);
        word("lead", BEAST, 2);
        word("key", METAL, 2, MAGIC, 1);
        word("trial", MAGIC, 2, RIFT, 1);
        word("vault", METAL, 3, MAGIC, 2);
        word("spawner", SHADOW, 3, MAGIC, 2);
        word("heavy", METAL, 5, EARTH, 2);
        word("mace", METAL, 4, AIR, 2);
        word("template", METAL, 3, MAGIC, 2);
        word("sword", METAL, 2);
        word("axe", METAL, 1);
        word("pickaxe", ORE, 1, METAL, 1);
        word("shovel", EARTH, 1);
        word("hoe", PLANT, 1);
        word("shears", METAL, 1);
        word("brush", METAL, 2, MAGIC, 1);
        word("bow", AIR, 2);
        word("crossbow", CIRCUIT, 2);
        word("arrow", AIR, 2);
        word("shield", METAL, 2);
        word("helmet", METAL, 1);
        word("chestplate", METAL, 2);
        word("leggings", METAL, 1);
        word("boots", AIR, 1);
        word("armor", METAL, 2);
        word("lodestone", ORE, 3, MAGIC, 2);
        word("compass", MAGIC, 2, METAL, 1);
        word("recovery", SHADOW, 2, MAGIC, 2);
        word("clock", METAL, 3, MAGIC, 2);
        word("redstone", SPARK, 3, CIRCUIT, 1);
        word("repeater", CIRCUIT, 3);
        word("comparator", CIRCUIT, 3, MAGIC, 1);
        word("piston", CIRCUIT, 4, AIR, 2);
        word("sticky", SAP, 2);
        word("observer", CIRCUIT, 4, MAGIC, 1);
        word("dispenser", CIRCUIT, 3, AIR, 1);
        word("dropper", CIRCUIT, 3, AIR, 1);
        word("hopper", CIRCUIT, 3, METAL, 2);
        word("lightning", SPARK, 6, METAL, 2);
        word("target", CIRCUIT, 2);
        word("button", CIRCUIT, 2, AIR, 1);
        word("lever", CIRCUIT, 2, AIR, 1);
        word("plate", CIRCUIT, 2);
        word("tripwire", PLANT, 1, CIRCUIT, 1);
        word("crafter", CIRCUIT, 3, METAL, 2);
        word("crafting", METAL, 1);
        word("furnace", FIRE, 3, EARTH, 1);
        word("blast", FIRE, 2, METAL, 2);
        word("smoker", ASH, 2, FIRE, 1);
        word("campfire", FIRE, 3, ASH, 2);
        word("brewing", STEAM, 3, MAGIC, 1);
        word("enchanting", MAGIC, 6);
        word("bookshelf", MAGIC, 5, PLANT, 2);
        word("lectern", MAGIC, 3, PLANT, 2);
        word("loom", BEAST, 2);
        word("cartography", MAGIC, 3, AIR, 1);
        word("fletching", AIR, 2);
        word("smithing", METAL, 4);
        word("grindstone", EARTH, 2, METAL, 2);
        word("stonecutter", METAL, 3, EARTH, 2);
        word("chest", PLANT, 3);
        word("barrel", PLANT, 3);
        word("experience", MAGIC, 4);
        word("star", MAGIC, 8, RIFT, 2);
        word("rod", SPARK, 1);
        word("pottery", EARTH, 2, METAL, 1);
        word("pot", EARTH, 2, PLANT, 1);
        word("painting", PLANT, 2, LIGHT, 1);
        word("frame", PLANT, 1);
        word("banner", BEAST, 3, PLANT, 2);
        word("stand", METAL, 1);
        word("name", MAGIC, 2);
        word("door", PLANT, 1);
        word("fence", PLANT, 1);
        word("gate", AIR, 1);
        word("ladder", AIR, 1, PLANT, 1);
        word("scaffolding", METAL, 2, AIR, 1);
        word("sign", MAGIC, 1);
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
