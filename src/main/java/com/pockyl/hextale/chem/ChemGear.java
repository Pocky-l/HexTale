package com.pockyl.hextale.chem;

import com.pockyl.hextale.GearConfig;
import com.pockyl.hextale.Hextale;
import com.pockyl.hextale.item.ChemToolItem;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.entity.EquipmentSlot;
import com.pockyl.hextale.port.ArmorType;
import com.pockyl.hextale.item.ChemArmourItem;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * How chemtech gear draws its strength from the tank on its wearer's back (owner, 2026-11-02). The items are registered
 * at full strength (refined shimmer = hextech level 4) and never break; what the tank holds sets how much of it they
 * really give: Ember Oil = level 2, raw shimmer = level 3, refined shimmer = level 4, an empty tank (or none) an eighth
 * of level 4 ({@link GearConfig#EMPTY_DIVISOR}; owner 2026-11-03: a third felt the same as fuelled gear). The sword
 * never breaks blocks, so a missed swing cannot dig. Digging speed is cut in the break-speed event, damage through a transient
 * modifier on the player while a chemtech tool is in the main hand. Every block broken and every hit spends one action
 * of fuel; using fuel never adds Thinning.
 */
public final class ChemGear {
    private static final ResourceLocation DAMAGE = Hextale.id("chem_power_damage");
    private static final ResourceLocation ARMOR = Hextale.id("chem_power_armor");
    private static final ResourceLocation TOUGHNESS = Hextale.id("chem_power_toughness");
    private static final ResourceLocation KNOCKBACK = Hextale.id("chem_power_knockback");
    private static final EquipmentSlot[] ARMOUR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int UPDATE_TICKS = 5;
    /** The tank the client's own player wears, for item bars and tooltips (set by the client, empty on a server). */
    public static Supplier<ItemStack> clientTank = () -> ItemStack.EMPTY;

    /** The tool kinds and their vanilla shape: damage on top of the material bonus and attack speed. */
    public enum Kind {
        PICKAXE(1.0F, -2.8F), AXE(5.0F, -3.0F), SHOVEL(1.5F, -3.0F), SWORD(0, -2.4F);

        private final float extra;
        private final float speed;

        Kind(float extra, float speed) {
            this.extra = extra;
            this.speed = speed;
        }

        /** Damage this kind adds on top of the material's bonus at a level (the sword's comes from the config). */
        public float extra(GearConfig.ToolLevel level) {
            return switch (this) {
                case SWORD -> level.sword();
                default -> extra;
            };
        }

        public float attackSpeed() {
            return speed;
        }

        /** The whole hit at a level: 1 (the fist) + the bonus + this kind's share. */
        public float damage(GearConfig.ToolLevel level) {
            return 1 + level.attackBonus().get().floatValue() + extra(level);
        }
    }

    private ChemGear() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ChemGear::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(ChemGear::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ChemGear::onIncomingDamage);
    }

    /** The level chemtech gear works at: refined shimmer gives the top one. */
    public static GearConfig.ToolLevel top() {
        return ChemFuel.REFINED_SHIMMER.tools();
    }

    /** The fuel in the tank the entity wears, or null when it has none or it is empty. */
    public static @Nullable ChemFuel power(LivingEntity entity) {
        TankFuel fuel = ChemTank.contents(ChemTank.worn(entity));
        return fuel.isEmpty() ? null : fuel.fuel();
    }

    /** Share of full digging speed this fuel gives (null = empty tank). */
    public static float speedShare(@Nullable ChemFuel fuel) {
        float full = top().speed().get().floatValue();
        return fuel == null ? 1 / GearConfig.EMPTY_DIVISOR.get().floatValue() : fuel.tools().speed().get().floatValue() / full;
    }

    /** Digging speed of a chemtech tool with this fuel, before enchantments and effects (null = empty tank). */
    public static float digSpeed(@Nullable ChemFuel fuel) {
        return top().speed().get().floatValue() * speedShare(fuel);
    }

    /** The whole hit of a tool of this kind with this fuel (null = empty tank). */
    public static float damage(Kind kind, @Nullable ChemFuel fuel) {
        return fuel == null ? kind.damage(top()) / GearConfig.EMPTY_DIVISOR.get().floatValue() : kind.damage(fuel.tools());
    }

    /** The armour level this fuel gives (null = empty tank, worked out from the top level). */
    public static GearConfig.ArmourLevel armourLevel(ChemFuel fuel) {
        return fuel.armour();
    }

    /** Armour points of one piece at the top level. */
    public static int points(GearConfig.ArmourLevel level, ArmorType type) {
        return switch (type) {
            case HELMET -> level.helmet().get();
            case CHESTPLATE -> level.chestplate().get();
            case LEGGINGS -> level.leggings().get();
            default -> level.boots().get();
        };
    }

    /** Armour points, toughness and knockback resistance one piece gives with this fuel (null = empty tank). */
    public static float[] armour(ArmorType type, @Nullable ChemFuel fuel) {
        GearConfig.ArmourLevel top = ChemFuel.REFINED_SHIMMER.armour();
        if (fuel == null) {
            float d = GearConfig.EMPTY_DIVISOR.get().floatValue();
            return new float[]{points(top, type) / d, top.toughness().get().floatValue() / d, top.knockbackResistance().get().floatValue() / d};
        }
        GearConfig.ArmourLevel level = fuel.armour();
        return new float[]{points(level, type), level.toughness().get().floatValue(), level.knockbackResistance().get().floatValue()};
    }

    /** Whether the entity wears any piece of chemtech armour. */
    public static boolean wearsArmour(LivingEntity entity) {
        for (EquipmentSlot slot : ARMOUR_SLOTS) {
            if (entity.getItemBySlot(slot).getItem() instanceof ChemArmourItem) {
                return true;
            }
        }
        return false;
    }

    /** Spends actions of fuel from the tank the entity wears; returns whether there was any to spend. */
    public static boolean spend(LivingEntity entity, int actions) {
        if (entity instanceof Player player && player.getAbilities().instabuild) {
            return true;
        }
        ItemStack tank = ChemTank.worn(entity);
        TankFuel fuel = ChemTank.contents(tank);
        if (fuel.isEmpty()) {
            return false;
        }
        ChemTank.setContents(tank, new TankFuel(fuel.fuel(), Math.max(0, fuel.units() - actions)));
        return true;
    }

    private static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack tool = event.getEntity().getMainHandItem();
        if (tool.getItem() instanceof ChemToolItem chem && chem.kind() == Kind.SWORD) {
            event.setCanceled(true);
        } else if (tool.getItem() instanceof ChemToolItem && tool.isCorrectToolForDrops(event.getState())) {
            event.setNewSpeed(event.getNewSpeed() * speedShare(power(event.getEntity())));
        }
    }

    /** Keeps the damage modifier in step with the tool in hand and the fuel on the back. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % UPDATE_TICKS != 0) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        float delta = 0;
        if (held.getItem() instanceof ChemToolItem tool) {
            delta = damage(tool.kind(), power(player)) - tool.kind().damage(top());
        }
        setModifier(player, Attributes.ATTACK_DAMAGE, DAMAGE, delta);
        float[] armour = new float[3];
        ChemFuel fuel = power(player);
        for (EquipmentSlot slot : ARMOUR_SLOTS) {
            if (player.getItemBySlot(slot).getItem() instanceof ChemArmourItem piece) {
                float[] now = armour(piece.type(), fuel);
                float[] full = armour(piece.type(), ChemFuel.REFINED_SHIMMER);
                for (int i = 0; i < 3; i++) {
                    armour[i] += now[i] - full[i];
                }
            }
        }
        setModifier(player, Attributes.ARMOR, ARMOR, armour[0]);
        setModifier(player, Attributes.ARMOR_TOUGHNESS, TOUGHNESS, armour[1]);
        setModifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, armour[2]);
    }

    /** Every hit taken in chemtech armour spends one action of fuel. */
    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide() && event.getAmount() > 0 && wearsArmour(entity)) {
            spend(entity, 1);
        }
    }

    private static void setModifier(Player player, Holder<Attribute> attribute, ResourceLocation id, float amount) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier current = instance.getModifier(id);
        if (amount == 0) {
            if (current != null) {
                instance.removeModifier(id);
            }
        } else if (current == null || current.amount() != amount) {
            instance.removeModifier(id);
            instance.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
