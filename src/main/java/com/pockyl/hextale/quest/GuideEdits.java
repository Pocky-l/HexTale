package com.pockyl.hextale.quest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.pockyl.hextale.Hextale;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * Changes made to the guide in the book's dev editor, kept as data over the steps written in {@link Guide}: steps moved
 * by hand, steps added (devices still being worked out, or item steps done by holding their icon's item), steps removed,
 * new parents, icons, auras and workstations. They live in {@link #FILE} in the mod's resources (the editor writes it to
 * the sources, so the next build ships it) and are applied when the guide is built. Names and descriptions are not here:
 * the editor writes them to the language files.
 */
public final class GuideEdits {
    public static final String FILE = "hextale/guide_edits.json";
    /** The first editor's file (moves only); still read, and merged into {@link #FILE} on the next save. */
    public static final String OLD_FILE = "hextale/guide_layout.json";

    private static final Map<String, int[]> MOVED = new TreeMap<>();
    /** Added steps in the order they were added (parents always come first): id and kind. */
    private static final Map<String, Quest.Kind> ADDED = new java.util.LinkedHashMap<>();
    private static final Set<String> REMOVED = new TreeSet<>();
    private static final Map<String, List<String>> PARENTS = new TreeMap<>();
    private static final Map<String, String> ICONS = new TreeMap<>();
    private static final Map<String, Quest.Aura> AURAS = new TreeMap<>();
    private static final Map<String, Boolean> STATIONS = new TreeMap<>();

    private GuideEdits() {
    }

    // ---- Reading and writing ----------------------------------------------------------------------------------------

    /** Reads the edits file on the classpath and applies it (called while the guide is built, before the layout). */
    static void apply() {
        try (var in = Guide.class.getResourceAsStream("/" + OLD_FILE)) {
            if (in != null) {
                for (var entry : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().entrySet()) {
                    Quest quest = Guide.byId(entry.getKey());
                    if (quest != null) {
                        JsonArray at = entry.getValue().getAsJsonArray();
                        moveTo(quest, at.get(0).getAsInt(), at.get(1).getAsInt());
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            Hextale.LOGGER.warn("Could not read the old guide layout file", e);
        }
        try (var in = Guide.class.getResourceAsStream("/" + FILE)) {
            if (in == null) {
                return;
            }
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : object(json, "parents").entrySet()) {
                List<String> ids = new ArrayList<>();
                entry.getValue().getAsJsonArray().forEach(id -> ids.add(id.getAsString()));
                PARENTS.put(entry.getKey(), ids);
            }
            object(json, "icons").entrySet().forEach(entry -> ICONS.put(entry.getKey(), entry.getValue().getAsString()));
            for (var entry : object(json, "added").entrySet()) {
                Quest.Kind kind = Quest.Kind.valueOf(entry.getValue().getAsString().toUpperCase(java.util.Locale.ROOT));
                List<Quest> parents = resolve(PARENTS.getOrDefault(entry.getKey(), List.of()));
                if (!parents.isEmpty() && Guide.byId(entry.getKey()) == null) {
                    create(entry.getKey(), kind, parents);
                }
            }
            if (json.has("removed")) {
                json.getAsJsonArray("removed").forEach(id -> {
                    Quest quest = Guide.byId(id.getAsString());
                    if (quest != null && quest.children().isEmpty()) {
                        REMOVED.add(quest.id());
                        drop(quest);
                    }
                });
            }
            PARENTS.forEach((id, ids) -> {
                Quest quest = Guide.byId(id);
                if (quest != null && !ADDED.containsKey(id)) {
                    List<Quest> parents = resolve(ids);
                    if (check(quest, parents) == null) {
                        quest.setParents(parents);
                    }
                }
            });
            ICONS.forEach((id, item) -> {
                Quest quest = Guide.byId(id);
                if (quest != null) {
                    quest.setIcon(icon(item));
                }
            });
            for (var entry : object(json, "auras").entrySet()) {
                Quest quest = Guide.byId(entry.getKey());
                if (quest != null) {
                    Quest.Aura aura = Quest.Aura.valueOf(entry.getValue().getAsString());
                    AURAS.put(quest.id(), aura);
                    quest.setAura(aura);
                }
            }
            for (var entry : object(json, "stations").entrySet()) {
                Quest quest = Guide.byId(entry.getKey());
                if (quest != null) {
                    STATIONS.put(quest.id(), entry.getValue().getAsBoolean());
                    quest.station = entry.getValue().getAsBoolean();
                }
            }
            for (var entry : object(json, "moved").entrySet()) {
                Quest quest = Guide.byId(entry.getKey());
                if (quest != null) {
                    JsonArray at = entry.getValue().getAsJsonArray();
                    moveTo(quest, at.get(0).getAsInt(), at.get(1).getAsInt());
                }
            }
        } catch (IOException | RuntimeException e) {
            Hextale.LOGGER.warn("Could not read the guide edits file", e);
        }
    }

    private static JsonObject object(JsonObject json, String key) {
        return json.has(key) ? json.getAsJsonObject(key) : new JsonObject();
    }

    /** Everything edited so far, as the edits file holds it. */
    public static JsonObject toJson() {
        JsonObject json = new JsonObject();
        JsonObject added = new JsonObject();
        ADDED.forEach((id, kind) -> added.addProperty(id, kind.name().toLowerCase(java.util.Locale.ROOT)));
        json.add("added", added);
        JsonArray removed = new JsonArray();
        REMOVED.forEach(removed::add);
        json.add("removed", removed);
        JsonObject parents = new JsonObject();
        PARENTS.forEach((id, ids) -> {
            JsonArray list = new JsonArray();
            ids.forEach(list::add);
            parents.add(id, list);
        });
        json.add("parents", parents);
        JsonObject icons = new JsonObject();
        ICONS.forEach(icons::addProperty);
        json.add("icons", icons);
        JsonObject auras = new JsonObject();
        AURAS.forEach((id, aura) -> auras.addProperty(id, aura.name()));
        json.add("auras", auras);
        JsonObject stations = new JsonObject();
        STATIONS.forEach(stations::addProperty);
        json.add("stations", stations);
        JsonObject moved = new JsonObject();
        MOVED.forEach((id, at) -> {
            JsonArray pair = new JsonArray();
            pair.add(at[0]);
            pair.add(at[1]);
            moved.add(id, pair);
        });
        json.add("moved", moved);
        return json;
    }

    // ---- Editing ------------------------------------------------------------------------------------------------------

    /** Moves a step while it is dragged: only the step itself, the rest is laid out on {@link #move}. */
    public static void drag(Quest quest, int x, int y) {
        quest.x = x;
        quest.y = y;
    }

    /** Places a step by hand and lays the map out again around it. */
    public static void move(Quest quest, int x, int y) {
        moveTo(quest, x, y);
        Guide.layout();
    }

    private static void moveTo(Quest quest, int x, int y) {
        Guide.at(quest, x, y);
        MOVED.put(quest.id(), new int[]{x, y});
    }

    /** Whether the step was added in the editor (it can then be removed whatever its kind). */
    public static boolean added(Quest quest) {
        return ADDED.containsKey(quest.id());
    }

    /**
     * Adds a step after {@code parent}: a device still being worked out, or an item step (done once the player holds its
     * icon's item; it starts as a book and stands next to its parent, to be dragged where it belongs).
     */
    public static Quest add(Quest parent, Quest.Kind kind) {
        String base = kind == Quest.Kind.WIP ? "wip_custom_" : "custom_";
        int n = 1;
        while (Guide.byId(base + n) != null || REMOVED.contains(base + n)) {
            n++;
        }
        String id = base + n;
        PARENTS.put(id, List.of(parent.id()));
        Quest quest = create(id, kind, List.of(parent));
        if (kind == Quest.Kind.ITEM) {
            moveTo(quest, quest.x(), quest.y());
        }
        Guide.layout();
        return quest;
    }

    private static Quest create(String id, Quest.Kind kind, List<Quest> parents) {
        ADDED.put(id, kind);
        Quest[] self = new Quest[1];
        Supplier<ItemStack> icon = icon(ICONS.getOrDefault(id, "minecraft:book"));
        self[0] = Guide.add(parents.getFirst().region(), id, kind, parents, icon,
                kind == Quest.Kind.ITEM ? Quests.has(() -> self[0].icon().getItem()) : player -> false, null, null, Quest.Aura.NONE);
        if (kind == Quest.Kind.ITEM && !Guide.PLACED.contains(self[0])) {
            int[] spot = freeSpot(parents.getFirst());
            Guide.at(self[0], spot[0], spot[1]);
        }
        return self[0];
    }

    /** A place next to a step that keeps clear of every other step. */
    private static int[] freeSpot(Quest near) {
        for (int ring = 0; ring < 20; ring++) {
            for (int turn = 0; turn < 12; turn++) {
                double angle = turn * Math.PI / 6;
                int x = (int) Math.round(near.x() + Math.cos(angle) * (70 + ring * 12));
                int y = (int) Math.round(near.y() + Math.sin(angle) * (70 + ring * 12));
                boolean free = true;
                for (Quest other : Guide.all()) {
                    if (Math.hypot(other.x() - x, other.y() - y) < 50) {
                        free = false;
                        break;
                    }
                }
                if (free) {
                    return new int[]{x, y};
                }
            }
        }
        return new int[]{near.x() + 70, near.y()};
    }

    /** Removes a step; returns why it cannot be removed, or null. */
    @Nullable
    public static String remove(Quest quest) {
        if (quest.kind() != Quest.Kind.WIP && !added(quest)) {
            return "Only devices still being worked out and steps added here can be removed";
        }
        if (!quest.children().isEmpty()) {
            return "Remove or move its children first";
        }
        if (ADDED.remove(quest.id()) == null) {
            REMOVED.add(quest.id());
        }
        PARENTS.remove(quest.id());
        ICONS.remove(quest.id());
        AURAS.remove(quest.id());
        STATIONS.remove(quest.id());
        MOVED.remove(quest.id());
        drop(quest);
        Guide.layout();
        return null;
    }

    private static void drop(Quest quest) {
        quest.detach();
        Guide.ALL.remove(quest);
        Guide.BY_ID.remove(quest.id());
        Guide.PLACED.remove(quest);
        for (int i = 0; i < Guide.ALL.size(); i++) {
            Guide.ALL.get(i).index = i;
        }
    }

    /** Gives a step new parents (ids); returns what is wrong with them, or null. */
    @Nullable
    public static String setParents(Quest quest, List<String> ids) {
        List<Quest> parents = new ArrayList<>();
        for (String id : ids) {
            Quest parent = Guide.byId(id);
            if (parent == null) {
                return "No step " + id;
            }
            parents.add(parent);
        }
        String problem = check(quest, parents);
        if (problem != null) {
            return problem;
        }
        quest.setParents(parents);
        PARENTS.put(quest.id(), List.copyOf(ids));
        Guide.layout();
        return null;
    }

    @Nullable
    private static String check(Quest quest, List<Quest> parents) {
        if (parents.isEmpty() && !quest.parents().isEmpty()) {
            return "A step needs at least one parent";
        }
        for (Quest parent : parents) {
            if (parent == quest) {
                return "A step cannot wait for itself";
            }
            if (parent.index() > quest.index()) {
                return parent.id() + " comes after " + quest.id() + " in the guide";
            }
            if (quest.kind() == Quest.Kind.ITEM && parent.kind() == Quest.Kind.WIP) {
                return "An item step cannot wait for a device still being worked out";
            }
        }
        return null;
    }

    /** Gives a step another icon (an item id); returns what is wrong, or null. */
    @Nullable
    public static String setIcon(Quest quest, String item) {
        Identifier id = Identifier.tryParse(item);
        if (id == null || BuiltInRegistries.ITEM.getOptional(id).isEmpty()) {
            return "No item " + item;
        }
        ICONS.put(quest.id(), id.toString());
        quest.setIcon(icon(id.toString()));
        return null;
    }

    public static void setAura(Quest quest, Quest.Aura aura) {
        AURAS.put(quest.id(), aura);
        quest.setAura(aura);
        Guide.layout();
    }

    public static void setStation(Quest quest, boolean station) {
        STATIONS.put(quest.id(), station);
        quest.station = station;
        Guide.layout();
    }

    private static List<Quest> resolve(List<String> ids) {
        List<Quest> out = new ArrayList<>();
        for (String id : ids) {
            Quest quest = Guide.byId(id);
            if (quest != null) {
                out.add(quest);
            }
        }
        return out;
    }

    private static Supplier<ItemStack> icon(String item) {
        return () -> {
            Item found = BuiltInRegistries.ITEM.getOptional(Identifier.parse(item)).orElse(Items.BOOK);
            return new ItemStack(found);
        };
    }
}
