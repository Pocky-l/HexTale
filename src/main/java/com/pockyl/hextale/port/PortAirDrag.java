package com.pockyl.hextale.port;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import com.pockyl.hextale.Hextale;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Air drag modifiers of players on 26.1.2, which has no air drag attribute: after the player's movement tick the
 * velocity is rescaled from the vanilla air friction to the modified one, the same formula 26.3 applies inside travel.
 * Modifiers are ADD_MULTIPLIED_TOTAL amounts, like on the attribute.
 * port-of: net.minecraft.world.entity.ai.attributes.Attributes.AIR_DRAG_MODIFIER
 */
@EventBusSubscriber(modid = Hextale.MOD_ID)
public final class PortAirDrag {
    private static final float AIR = 0.91F;
    private static final float VERTICAL = 0.98F;
    private static final Map<Player, Map<Identifier, Double>> ACTIVE = new WeakHashMap<>();

    private PortAirDrag() {
    }

    /** Adds or removes one modifier of the player's air drag. */
    public static void toggle(Player player, AttributeModifier modifier, boolean on) {
        synchronized (ACTIVE) {
            if (on) {
                ACTIVE.computeIfAbsent(player, p -> new HashMap<>()).put(modifier.id(), modifier.amount());
            } else {
                Map<Identifier, Double> modifiers = ACTIVE.get(player);
                if (modifiers != null) {
                    modifiers.remove(modifier.id());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        double value = 1;
        synchronized (ACTIVE) {
            Map<Identifier, Double> modifiers = ACTIVE.get(player);
            if (modifiers == null || modifiers.isEmpty()) {
                return;
            }
            for (double amount : modifiers.values()) {
                value *= 1 + amount;
            }
        }
        if (!player.canSimulateMovement() || player.onGround() || player.isInWater() || player.isInLava() || player.isFallFlying()
                || player.getAbilities().flying || player.isNoGravity()) {
            return;
        }
        float horizontal = friction(AIR, value) / AIR;
        float vertical = friction(VERTICAL, value) / VERTICAL;
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x * horizontal, movement.y * vertical, movement.z * horizontal);
    }

    private static float friction(float friction, double modifier) {
        return Mth.clamp(1.0F - (1.0F - friction) * (float) modifier, 0.0F, 1.0F);
    }
}
