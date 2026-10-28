package com.pockyl.hextale.crafting;

import com.mojang.serialization.MapCodec;
import com.pockyl.hextale.Hextale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 * One kind of the mod's own recipes. Each kind is a real recipe type ({@code hextale:<name>}) read by the vanilla recipe
 * manager from {@code data/<namespace>/recipe/**.json} with {@code "type": "hextale:<name>"}, so data packs and recipe
 * scripts (KubeJS, KubeGroovy, CraftTweaker) can add, replace and remove them like any other recipe. The machines never
 * ask the recipe manager to match: they keep their own rules and only read the values through {@link #get}.
 * <p>
 * The server reads its recipe manager (rebuilt after every reload); clients keep what the server sent them (the guide and
 * JEI show them). The two live apart so an integrated server and its client never share a half-reloaded set.
 */
public final class DataRecipes<T> {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Hextale.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Hextale.MOD_ID);
    private static final List<DataRecipes<?>> KINDS = new ArrayList<>();

    private final String name;
    private final Function<T, @Nullable Item> output;
    private final StreamCodec<RegistryFriendlyByteBuf, Map<ResourceLocation, T>> syncCodec;
    private final DeferredHolder<RecipeType<?>, RecipeType<Entry<T>>> type;
    private final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<Entry<T>>> serializer;
    private volatile @Nullable RecipeManager seen;
    private volatile Map<ResourceLocation, T> server = Map.of();
    private volatile Map<ResourceLocation, T> client = Map.of();

    /**
     * @param name   the recipe type's path ({@code hextale:<name>})
     * @param output the item a recipe makes, used to find a recipe by what it makes (null when it makes no item)
     */
    public DataRecipes(String name, MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, Function<T, @Nullable Item> output) {
        this.name = name;
        this.output = output;
        this.syncCodec = ByteBufCodecs.map(LinkedHashMap::new, ResourceLocation.STREAM_CODEC, streamCodec);
        ResourceLocation id = Hextale.id(name);
        this.type = TYPES.register(name, () -> RecipeType.simple(id));
        MapCodec<Entry<T>> entryCodec = codec.xmap(value -> new Entry<>(this, value), Entry::value);
        StreamCodec<RegistryFriendlyByteBuf, Entry<T>> entryStream = streamCodec.map(value -> new Entry<>(this, value), Entry::value);
        this.serializer = SERIALIZERS.register(name, () -> new RecipeSerializer<>() {
            @Override
            public MapCodec<Entry<T>> codec() {
                return entryCodec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, Entry<T>> streamCodec() {
                return entryStream;
            }
        });
        KINDS.add(this);
    }

    /** Registers every kind's recipe type and serializer; the kinds' classes must be loaded before (see {@code ModRecipes}). */
    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        SERIALIZERS.register(modBus);
    }

    public static List<DataRecipes<?>> kinds() {
        return Collections.unmodifiableList(KINDS);
    }

    public static @Nullable DataRecipes<?> byName(String name) {
        for (DataRecipes<?> kind : KINDS) {
            if (kind.name.equals(name)) {
                return kind;
            }
        }
        return null;
    }

    /** How the server's recipes of this kind travel to clients (see {@code RecipesPayload}). */
    public StreamCodec<RegistryFriendlyByteBuf, Map<ResourceLocation, T>> syncCodec() {
        return syncCodec;
    }

    public String name() {
        return name;
    }

    public RecipeType<Entry<T>> type() {
        return type.get();
    }

    /** Every recipe of this kind by id, in id order. */
    public Map<ResourceLocation, T> get(boolean clientSide) {
        return clientSide ? client : fromServer();
    }

    public void setClient(Map<ResourceLocation, T> recipes) {
        client = Collections.unmodifiableMap(new LinkedHashMap<>(recipes));
    }

    /**
     * The recipe a guide page or test names: its exact id, the id under this kind's folder ({@code ns:<kind>/path}), or else
     * the first recipe that makes the item with that id - so a pack that replaces a recipe under another id still shows it.
     */
    public @Nullable T named(@Nullable ResourceLocation name, boolean clientSide) {
        if (name == null) {
            return null;
        }
        Map<ResourceLocation, T> all = get(clientSide);
        T exact = all.get(name);
        if (exact != null) {
            return exact;
        }
        T inFolder = all.get(name.withPrefix(this.name + "/"));
        if (inFolder != null) {
            return inFolder;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(name).orElse(null);
        if (item == null) {
            return null;
        }
        for (T recipe : all.values()) {
            if (output.apply(recipe) == item) {
                return recipe;
            }
        }
        return null;
    }

    private Map<ResourceLocation, T> fromServer() {
        MinecraftServer current = ServerLifecycleHooks.getCurrentServer();
        if (current == null) {
            return Map.of();
        }
        // 1.21.1: a reload makes a new recipe manager.
        RecipeManager recipes = current.getRecipeManager();
        if (recipes != seen) {
            server = read(recipes);
            seen = recipes;
        }
        return server;
    }

    /** This kind's recipes in a recipe map, by id in id order. */
    public Map<ResourceLocation, T> read(RecipeManager recipes) {
        Map<ResourceLocation, T> found = new TreeMap<>();
        for (RecipeHolder<Entry<T>> holder : recipes.getAllRecipesFor(type.get())) {
            found.put(holder.id(), holder.value().value());
        }
        return Collections.unmodifiableMap(found);
    }

    /**
     * A recipe of one kind as the recipe manager holds it. It never matches anything on its own (the machines apply their own
     * rules) and is "special", so the recipe book and placement checks leave it alone.
     */
    public record Entry<T>(DataRecipes<T> kind, T value) implements Recipe<RecipeInput> {
        @Override
        public boolean matches(RecipeInput input, Level level) {
            return false;
        }

        @Override
        public ItemStack assemble(RecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean isSpecial() {
            return true;
        }

        @Override
        public boolean showNotification() {
            return false;
        }

        @Override
        public String getGroup() {
            return "";
        }

        @Override
        public RecipeSerializer<Entry<T>> getSerializer() {
            return kind.serializer.get();
        }

        @Override
        public RecipeType<Entry<T>> getType() {
            return kind.type.get();
        }

        @Override
        public boolean canCraftInDimensions(int width, int height) {
            return false;
        }

        @Override
        public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Entry<?> entry && entry.kind == kind && Objects.equals(entry.value, value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind.name, value);
        }
    }
}
