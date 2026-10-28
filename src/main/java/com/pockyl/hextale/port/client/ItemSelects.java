package com.pockyl.hextale.port.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import com.pockyl.hextale.Hextale;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Item models that pick a model by a component's value ({@code minecraft:select} on {@code minecraft:component} in the
 * 26.x item definitions). The asset port script turns the cases into overrides on the {@code hextale:select} property;
 * this registers that property per item: case n (1-based) gives n / 100, no matching case 0 (the fallback model).
 */
@EventBusSubscriber(modid = Hextale.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ItemSelects {
    private static final ResourceLocation PROPERTY = Hextale.id("select");
    private static final ResourceLocation CONDITION = Hextale.id("condition");

    private ItemSelects() {
    }

    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id.getNamespace().equals(Hextale.MOD_ID)) {
                    register(item, id);
                }
            }
        });
    }

    private static void register(Item item, ResourceLocation id) {
        JsonObject model;
        try (InputStream in = ItemSelects.class.getResourceAsStream("/assets/" + id.getNamespace() + "/items/" + id.getPath() + ".json")) {
            if (in == null) {
                return;
            }
            model = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("model");
            // The mod's "animated" wrapper only matters to the 26.x GUI item cache.
            while (model.has("type") && "hextale:animated".equals(model.get("type").getAsString())) {
                model = model.getAsJsonObject("model");
            }
        } catch (Exception e) {
            return;
        }
        if ("minecraft:condition".equals(model.get("type").getAsString())) {
            registerCondition(item, model);
            return;
        }
        if (!"minecraft:select".equals(model.get("type").getAsString())) {
            return;
        }
        if ("minecraft:custom_model_data".equals(model.get("property").getAsString())) {
            registerCustomModelData(item, model);
            return;
        }
        if (!"minecraft:component".equals(model.get("property").getAsString())) {
            return;
        }
        DataComponentType<?> component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(model.get("component").getAsString()));
        if (component == null) {
            return;
        }
        List<List<JsonElement>> cases = new ArrayList<>();
        for (JsonElement element : model.getAsJsonArray("cases")) {
            JsonElement when = element.getAsJsonObject().get("when");
            List<JsonElement> values = new ArrayList<>();
            if (when instanceof JsonArray array) {
                array.forEach(values::add);
            } else {
                values.add(when);
            }
            cases.add(values);
        }
        ItemProperties.register(item, PROPERTY, (stack, level, entity, seed) -> {
            JsonElement value = encode(stack, component);
            if (value == null) {
                return 0;
            }
            for (int i = 0; i < cases.size(); i++) {
                if (cases.get(i).contains(value)) {
                    return (i + 1) / 100F;
                }
            }
            return 0;
        });
    }

    /**
     * A model picked by a condition ({@code minecraft:condition}): the asset port script makes the model of the false
     * case the base and the true case an override on {@code hextale:condition} = 1. Only {@code minecraft:has_component}.
     */
    private static void registerCondition(Item item, JsonObject model) {
        if (!"minecraft:has_component".equals(model.get("property").getAsString())) {
            Hextale.LOGGER.warn("Item model condition {} of {} is not ported", model.get("property"), item);
            return;
        }
        DataComponentType<?> component = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(model.get("component").getAsString()));
        if (component == null) {
            return;
        }
        ItemProperties.register(item, CONDITION, (stack, level, entity, seed) -> stack.has(component) ? 1 : 0);
    }

    /**
     * 26.x custom model data holds strings; 1.21.1 one number. The data port script stores a string as its Java hash
     * code, so a case matches when the stack's number is the hash of one of its strings.
     */
    private static void registerCustomModelData(Item item, JsonObject model) {
        List<List<Integer>> cases = new ArrayList<>();
        for (JsonElement element : model.getAsJsonArray("cases")) {
            JsonElement when = element.getAsJsonObject().get("when");
            List<Integer> values = new ArrayList<>();
            if (when instanceof JsonArray array) {
                array.forEach(value -> values.add(value.getAsString().hashCode()));
            } else {
                values.add(when.getAsString().hashCode());
            }
            cases.add(values);
        }
        ItemProperties.register(item, PROPERTY, (stack, level, entity, seed) -> {
            net.minecraft.world.item.component.CustomModelData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
            if (data == null) {
                return 0;
            }
            for (int i = 0; i < cases.size(); i++) {
                if (cases.get(i).contains(data.value())) {
                    return (i + 1) / 100F;
                }
            }
            return 0;
        });
    }

    private static <T> JsonElement encode(ItemStack stack, DataComponentType<T> component) {
        T value = stack.get(component);
        if (value == null || component.codec() == null) {
            return null;
        }
        return component.codec().encodeStart(JsonOps.INSTANCE, value).result().orElse(null);
    }
}
