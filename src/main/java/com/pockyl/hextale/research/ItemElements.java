package com.pockyl.hextale.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import com.pockyl.hextale.rune.SimpleRune;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The elements an item carries (roughly like aspects): each point pulls the symbol the item writes one section towards
 * that element's corner. {@code tier} says how valuable the item is (1 simple, 2 valuable, 3 rare); the mortar can
 * only grind items up to its strength. Entry order matters (it breaks ties), so maps keep their insertion order.
 */
public record ItemElements(Map<SimpleRune, Integer> elements, int tier) {
    private static final Codec<SimpleRune> ELEMENT = Codec.STRING.comapFlatMap(id -> {
        SimpleRune rune = SimpleRune.byId(id);
        return rune != null ? DataResult.success(rune) : DataResult.error(() -> "Unknown element: " + id);
    }, SimpleRune::id);
    public static final Codec<ItemElements> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(ELEMENT, Codec.intRange(1, 9)).fieldOf("elements").forGetter(ItemElements::elements),
            Codec.intRange(1, 3).optionalFieldOf("tier", 1).forGetter(ItemElements::tier)
    ).apply(instance, ItemElements::new));
    public static final StreamCodec<ByteBuf, ItemElements> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(LinkedHashMap::new, ByteBufCodecs.VAR_INT.map(i -> SimpleRune.values()[i], SimpleRune::ordinal),
                    ByteBufCodecs.VAR_INT), e -> new LinkedHashMap<>(e.elements),
            ByteBufCodecs.VAR_INT, ItemElements::tier,
            ItemElements::new);

    public ItemElements {
        elements = new LinkedHashMap<>(elements);
    }

    /** Builds from alternating element/points pairs, keeping their order. */
    public static ItemElements of(int tier, Object... pairs) {
        Map<SimpleRune, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((SimpleRune) pairs[i], (Integer) pairs[i + 1]);
        }
        return new ItemElements(map, tier);
    }

    /** The pull in parchment units; ground in a mortar it is half as long. */
    public double[] vector(boolean ground) {
        double x = 0;
        double y = 0;
        for (Map.Entry<SimpleRune, Integer> entry : elements.entrySet()) {
            double[] u = HexGrid.unit(entry.getKey());
            double k = entry.getValue() * HexGrid.CELL * (ground ? 0.5 : 1);
            x = x + u[0] * k;
            y = y + u[1] * k;
        }
        return new double[]{x, y};
    }

    /** The fixed step in sections {@code [dq, dr]}: the pull snapped to the hex grid once. */
    public int[] offset(boolean ground) {
        double[] v = vector(ground);
        return HexGrid.axialAt(v[0], v[1]);
    }

    /** The strongest element (the first of equals): it names the symbol the item writes. */
    public SimpleRune main() {
        SimpleRune best = null;
        int points = 0;
        for (Map.Entry<SimpleRune, Integer> entry : elements.entrySet()) {
            if (entry.getValue() > points) {
                best = entry.getKey();
                points = entry.getValue();
            }
        }
        return best == null ? SimpleRune.FIRE : best;
    }
}
