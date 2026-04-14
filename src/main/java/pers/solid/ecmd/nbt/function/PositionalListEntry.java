package pers.solid.ecmd.nbt.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record PositionalListEntry<T>(int index, T value) {
  public static <T> MapCodec<PositionalListEntry<T>> mapCodec(Codec<T> valueCodec) {
    return RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.INT.fieldOf("index").forGetter(PositionalListEntry::index),
        valueCodec.fieldOf("value").forGetter(PositionalListEntry::value)
    ).apply(i, PositionalListEntry::new));
  }

  public static <T> Codec<PositionalListEntry<T>> codec(Codec<T> valueCodec) {
    return mapCodec(valueCodec).codec();
  }
}
