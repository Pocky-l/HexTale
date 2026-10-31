package com.pockyl.hextale.port.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.jspecify.annotations.Nullable;

import com.pockyl.hextale.Hextale;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Item tints on 1.21.1: the 26.x item definitions ({@code assets/hextale/items/*.json}) name a tint per tint index;
 * here each item with tints gets an item colour that asks those sources. A select model uses the tints of its first
 * case (all cases of the mod's selects share them), a composite those of its first part with tints.
 */
@EventBusSubscriber(modid = Hextale.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ItemTints {
    private static final Map<ResourceLocation, ItemTintSource> SOURCES = new HashMap<>();

    private ItemTints() {
    }

    /** What 26.x's {@code RegisterColorHandlersEvent.ItemTintSources#register} did. */
    public static void register(ResourceLocation id, MapCodec<? extends ItemTintSource> codec) {
        codec.codec().parse(JsonOps.INSTANCE, new JsonObject()).result().ifPresent(source -> SOURCES.put(id, source));
    }

    @SubscribeEvent
    static void registerColors(RegisterColorHandlersEvent.Item event) {
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(Hextale.MOD_ID)) {
                continue;
            }
            Tint[] tints = tints(id);
            if (tints != null) {
                event.register((stack, index) -> {
                    if (index < 0 || index >= tints.length) {
                        return -1;
                    }
                    Tint tint = tints[index];
                    return tint.source != null ? tint.source.calculate(stack, Minecraft.getInstance().level, null) : tint.constant;
                }, item);
            }
        }
    }

    private record Tint(@Nullable ItemTintSource source, int constant) {
    }

    private static Tint @Nullable [] tints(ResourceLocation id) {
        JsonObject model;
        try (InputStream in = ItemTints.class.getResourceAsStream("/assets/" + id.getNamespace() + "/items/" + id.getPath() + ".json")) {
            if (in == null) {
                return null;
            }
            model = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("model");
            // The mod's "animated" wrapper only matters to the 26.x GUI item cache.
            while (model.has("type") && "hextale:animated".equals(model.get("type").getAsString())) {
                model = model.getAsJsonObject("model");
            }
        } catch (Exception e) {
            Hextale.LOGGER.warn("Item definition of {} not readable", id, e);
            return null;
        }
        while (model != null && !model.has("tints") && (model.has("cases") || model.has("models"))) {
            if (model.has("cases")) {
                model = model.getAsJsonArray("cases").get(0).getAsJsonObject().getAsJsonObject("model");
            } else {
                JsonObject tinted = null;
                for (JsonElement part : model.getAsJsonArray("models")) {
                    if (part.getAsJsonObject().has("tints")) {
                        tinted = part.getAsJsonObject();
                        break;
                    }
                }
                model = tinted;
            }
        }
        if (model == null || !model.has("tints")) {
            return null;
        }
        JsonArray list = model.getAsJsonArray("tints");
        Tint[] tints = new Tint[list.size()];
        for (int i = 0; i < tints.length; i++) {
            JsonObject tint = list.get(i).getAsJsonObject();
            ResourceLocation type = ResourceLocation.parse(tint.get("type").getAsString());
            JsonElement value = tint.get("value");
            tints[i] = new Tint(SOURCES.get(type), value != null ? value.getAsInt() : -1);
        }
        return tints;
    }
}
