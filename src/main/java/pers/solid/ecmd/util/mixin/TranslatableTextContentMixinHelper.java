package pers.solid.ecmd.util.mixin;

import com.mojang.serialization.*;
import net.minecraft.text.TranslatableTextContent;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.util.EnhancedTranslatableTextContent;

import java.util.function.Function;
import java.util.stream.Stream;

public final class TranslatableTextContentMixinHelper {
  public static @NotNull MapCodec<TranslatableTextContent> modifyTranslatableCodec(MapCodec<TranslatableTextContent> original) {
    final String typeKey = "enhanced_commands:enhanced";
    return new MapCodec<>() {
      @Override
      public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.concat(original.keys(ops), Stream.of(ops.createString(typeKey)));
      }

      @Override
      public <T> DataResult<TranslatableTextContent> decode(DynamicOps<T> ops, MapLike<T> input) {
        final T t = input.get(typeKey);
        if (t == null) {
          return original.decode(ops, input).map(Function.identity());
        } else {
          return ops.getBooleanValue(t).flatMap(bl -> bl ? EnhancedTranslatableTextContent.CODEC.decode(ops, input).map(Function.identity()) : original.decode(ops, input));
        }
      }

      @Override
      public <T> RecordBuilder<T> encode(TranslatableTextContent input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        if (input instanceof EnhancedTranslatableTextContent enhanced) {
          final RecordBuilder<T> encode = EnhancedTranslatableTextContent.CODEC.encode(enhanced, ops, prefix);
          encode.add(typeKey, ops.createBoolean(true));
          return encode;
        } else {
          return original.encode(input, ops, prefix);
        }
      }
    };
  }
}
