package com.pockyl.hextale.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.pockyl.hextale.rune.Essence;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The essences an item carries (roughly like aspects) and how much of each: up to three, strongest first - an item
 * boils into that many mB of each. {@code tier} says how valuable the item is (1
 * simple, 2 valuable, 3 rare). Entry order matters (strongest first, it breaks ties), so maps keep their insertion order.
 */
public record ItemEssences(Map<Essence, Integer> essences, int tier) {
    private static final Codec<Essence> ELEMENT = Codec.STRING.comapFlatMap(id -> {
        Essence rune = Essence.byId(id);
        return rune != null ? DataResult.success(rune) : DataResult.error(() -> "Unknown element: " + id);
    }, Essence::id);
    public static final Codec<ItemEssences> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ELEMENT, Codec.intRange(1, 64)).fieldOf("essences").forGetter(ItemEssences::essences),
            Codec.intRange(1, 3).optionalFieldOf("tier", 1).forGetter(ItemEssences::tier)
    ).apply(instance, ItemEssences::new));
    public static final StreamCodec<ByteBuf, ItemEssences> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.VAR_INT.map(i -> Essence.values()[i], Essence::ordinal),
                    ByteBufCodecs.VAR_INT), e -> new LinkedHashMap<>(e.essences),
            ByteBufCodecs.VAR_INT, ItemEssences::tier,
            ItemEssences::new);

    public ItemEssences {
        essences = new LinkedHashMap<>(essences);
    }

    /** Builds from alternating element/points pairs, keeping their order. */
    public static ItemEssences of(int tier, Object... pairs) {
        Map<Essence, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((Essence) pairs[i], (Integer) pairs[i + 1]);
        }
        return new ItemEssences(map, tier);
    }

    /** The strongest element (the first of equals): it names the symbol the item writes. */
    public Essence main() {
        Essence best = null;
        int points = 0;
        for (Map.Entry<Essence, Integer> entry : essences.entrySet()) {
            if (entry.getValue() > points) {
                best = entry.getKey();
                points = entry.getValue();
            }
        }
        return best == null ? Essence.FIRE : best;
    }
}
