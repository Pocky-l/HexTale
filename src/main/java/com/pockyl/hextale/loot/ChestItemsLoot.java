package com.pockyl.hextale.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.pockyl.hextale.Hextale;

import java.util.List;
import java.util.Optional;

/**
 * Plain items of the mod in the world's treasure chests ({@code data/hextale/loot_modifiers/hextech_finds_*.json}): hextech
 * dust and now and then a lump of unstable hextech in dungeons, so a researcher may meet hextech before they make any.
 *
 * @param tables  the chest loot tables (as queried)
 * @param entries what may turn up, each rolled on its own
 */
public final class ChestItemsLoot extends LootModifier {
    /** One item that may turn up: its chance per chest and how many. */
    public record Entry(Holder<Item> item, float chance, int min, int max) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(Entry::item),
                Codec.floatRange(0, 1).fieldOf("chance").forGetter(Entry::chance),
                Codec.intRange(1, 64).optionalFieldOf("min", 1).forGetter(Entry::min),
                Codec.intRange(1, 64).optionalFieldOf("max", 1).forGetter(Entry::max)
        ).apply(instance, Entry::new));
    }

    public static final MapCodec<ChestItemsLoot> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).and(instance.group(
            ResourceLocation.CODEC.listOf().fieldOf("tables").forGetter(loot -> loot.tables),
            Entry.CODEC.listOf().fieldOf("entries").forGetter(loot -> loot.entries)
    )).apply(instance, ChestItemsLoot::new));

    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Hextale.MOD_ID);

    static {
        SERIALIZERS.register("chest_items", () -> CODEC);
    }

    private final List<ResourceLocation> tables;
    private final List<Entry> entries;

    public ChestItemsLoot(Optional<Holder<LootItemCondition>> condition, int priority, List<ResourceLocation> tables, List<Entry> entries) {
        super(condition, priority);
        this.tables = List.copyOf(tables);
        this.entries = List.copyOf(entries);
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!tables.contains(context.getQueriedLootTableId())) {
            return loot;
        }
        RandomSource random = context.getRandom();
        for (Entry entry : entries) {
            if (random.nextFloat() < entry.chance()) {
                int count = entry.min() + random.nextInt(Math.max(1, entry.max() - entry.min() + 1));
                loot.add(new ItemStack(entry.item(), count));
            }
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
