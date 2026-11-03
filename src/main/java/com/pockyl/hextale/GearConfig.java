package com.pockyl.hextale;

import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Strength of the mod's tools and armour, a STARTUP config ({@code hextale-startup.toml}): it is read before the items
 * register, so a modpack can retune every level without patches. Changes need a restart and the same file on the
 * server and every client.
 *
 * <p>The ladder: level 1 is a little better than diamond, level 2 better than netherite, level 3 a clear step above,
 * level 4 is meant to feel overwhelming (speed 20 + Efficiency V instamines stone, the sword one-hits ordinary mobs with
 * Sharpness V, the armour reaches the damage cap). Chemtech gear borrows levels 2-4 for its fuels.
 */
public final class GearConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** One level of tools: mining speed, attack bonus of every tool, durability and the sword's own extra damage. */
    public record ToolLevel(ModConfigSpec.DoubleValue speed, ModConfigSpec.DoubleValue attackBonus,
                            ModConfigSpec.IntValue durability, ModConfigSpec.DoubleValue swordDamage) {
        /** The vanilla tool material for this level (read only once the config is loaded). */
        public ToolMaterial material(TagKey<Block> incorrect, int enchantability, TagKey<Item> repairs) {
            return new ToolMaterial(incorrect, durability.get(), speed.get().floatValue(), attackBonus.get().floatValue(),
                    enchantability, repairs);
        }

        /** The sword's damage on top of the material's bonus (a sword deals 1 + bonus + this). */
        public float sword() {
            return swordDamage.get().floatValue();
        }
    }

    /** One level of armour: protection per piece, toughness and knockback resistance per piece, durability multiplier. */
    public record ArmourLevel(ModConfigSpec.IntValue boots, ModConfigSpec.IntValue leggings, ModConfigSpec.IntValue chestplate,
                              ModConfigSpec.IntValue helmet, ModConfigSpec.DoubleValue toughness,
                              ModConfigSpec.DoubleValue knockbackResistance, ModConfigSpec.IntValue durability) {
        /** The vanilla armour material for this level; {@code body} is the horse/wolf value, kept from the code. */
        public ArmorMaterial material(int body, int enchantability, Holder<SoundEvent> equipSound, TagKey<Item> repairs,
                                      ResourceKey<EquipmentAsset> asset) {
            return new ArmorMaterial(durability.get(),
                    Map.of(ArmorType.BOOTS, boots.get(), ArmorType.LEGGINGS, leggings.get(), ArmorType.CHESTPLATE, chestplate.get(),
                            ArmorType.HELMET, helmet.get(), ArmorType.BODY, body),
                    enchantability, equipSound, toughness.get().floatValue(), knockbackResistance.get().floatValue(), repairs, asset);
        }
    }

    /** Hextech tools by level: runed, refined (charged hexsteel), radiant, riftforged. */
    public static final ToolLevel[] HEXTECH_TOOLS = {
            tool("hextechTools.level1", "Runed hextech tools (level 1)", 8.5, 3.0, 1600, 3.5),
            tool("hextechTools.level2", "Refined hextech tools (level 2)", 11.0, 4.0, 2400, 4.0),
            tool("hextechTools.level3", "Radiant hextech tools (level 3)", 14.0, 5.0, 3200, 4.5),
            tool("hextechTools.level4", "Riftforged tools (level 4)", 20.0, 6.0, 6000, 7.0)};
    /** Plain hexsteel tools: no runes, a cheap step above diamond. */
    public static final ToolLevel HEXSTEEL_TOOLS = tool("hexsteelTools", "Hexsteel tools (no runes)", 8.5, 3.5, 2000, 3.0);

    /** Bulwark armour by level. */
    public static final ArmourLevel[] BULWARK = {
            armour("bulwark.level1", "Bulwark armour (level 1)", 3, 6, 7, 2, 1.0, 0.0, 25),
            armour("bulwark.level2", "Bulwark armour (level 2)", 3, 6, 8, 3, 3.0, 0.1, 37),
            armour("bulwark.level3", "Bulwark armour (level 3)", 3, 8, 10, 3, 4.0, 0.15, 42),
            armour("bulwark.level4", "Bulwark armour (level 4)", 4, 10, 12, 4, 5.0, 0.25, 60)};
    /** Plain hexsteel armour: diamond protection, a little tougher. */
    public static final ArmourLevel HEXSTEEL_ARMOUR = armour("hexsteelArmour", "Hexsteel armour (no runes)", 3, 6, 8, 3, 2.5, 0.0, 35);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private GearConfig() {
    }

    private static ToolLevel tool(String path, String title, double speed, double bonus, int durability, double sword) {
        BUILDER.comment(title).push(java.util.List.of(path.split("\\.")));
        ToolLevel level = new ToolLevel(
                BUILDER.comment("Mining speed (vanilla: iron 6, diamond 8, netherite 9).").defineInRange("miningSpeed", speed, 1.0, 100.0),
                BUILDER.comment("Attack damage every tool of this level adds (vanilla: diamond 3, netherite 4).")
                        .defineInRange("attackBonus", bonus, 0.0, 100.0),
                BUILDER.comment("Uses before it breaks (vanilla: diamond 1561, netherite 2031).").defineInRange("durability", durability, 1, 100000),
                BUILDER.comment("The sword's own damage on top of the bonus; a sword hits for 1 + attackBonus + this (vanilla swords: 3).")
                        .defineInRange("swordDamage", sword, 0.0, 100.0));
        BUILDER.pop(path.split("\\.").length);
        return level;
    }

    private static ArmourLevel armour(String path, String title, int boots, int leggings, int chestplate, int helmet,
                                      double toughness, double knockback, int durability) {
        BUILDER.comment(title).push(java.util.List.of(path.split("\\.")));
        ArmourLevel level = new ArmourLevel(
                BUILDER.comment("Armour points of the boots.").defineInRange("boots", boots, 0, 30),
                BUILDER.comment("Armour points of the leggings.").defineInRange("leggings", leggings, 0, 30),
                BUILDER.comment("Armour points of the chestplate.").defineInRange("chestplate", chestplate, 0, 30),
                BUILDER.comment("Armour points of the helmet.").defineInRange("helmet", helmet, 0, 30),
                BUILDER.comment("Armour toughness of each piece (vanilla: diamond 2, netherite 3).").defineInRange("toughness", toughness, 0.0, 20.0),
                BUILDER.comment("Knockback resistance of each piece, 0-1 (vanilla netherite 0.1).")
                        .defineInRange("knockbackResistance", knockback, 0.0, 1.0),
                BUILDER.comment("Durability multiplier (vanilla: diamond 33, netherite 37).").defineInRange("durability", durability, 1, 1000));
        BUILDER.pop(path.split("\\.").length);
        return level;
    }
}
