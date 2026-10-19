package dev.anvilborn.forge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;

public record ForgeData(int level, int hits, int frenzy, List<Roll> aspects) {
    public static final ForgeData FRESH = new ForgeData(0, 0, 0, List.of());
    public static final Codec<ForgeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("level").forGetter(ForgeData::level),
        Codec.INT.fieldOf("hits").forGetter(ForgeData::hits),
        Codec.INT.fieldOf("frenzy").forGetter(ForgeData::frenzy),
        Roll.CODEC.listOf().fieldOf("aspects").forGetter(ForgeData::aspects)
    ).apply(instance, ForgeData::new));

    public ForgeData {
        level = Math.clamp(level, -2, 10);
        hits = Math.clamp(hits, 0, 6);
        frenzy = Math.clamp(frenzy, 0, 2);
        aspects = List.copyOf(aspects.stream().filter(r -> Aspects.byId(r.id()) != null).limit(4).toList());
    }

    public record Roll(String id, int rank) {
        public static final Codec<Roll> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(Roll::id),
            Codec.INT.fieldOf("rank").forGetter(Roll::rank)
        ).apply(instance, Roll::new));
        public Roll {
            rank = Math.clamp(rank, 1, 3);
        }
    }
}
