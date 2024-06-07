package pers.solid.ecmd.util.codec;

import com.mojang.serialization.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class StrictOptionalFieldCodec<A> extends MapCodec<A> {
  private final @NotNull String name;
  private final @NotNull Codec<A> elementCodec;
  private final @NotNull A defaultValue;
  private final boolean emitWhenDefault;

  public StrictOptionalFieldCodec(@NotNull String name, @NotNull Codec<A> elementCodec, @NotNull A defaultValue, boolean emitWhenDefault) {
    this.name = name;
    this.elementCodec = elementCodec;
    this.defaultValue = defaultValue;
    this.emitWhenDefault = emitWhenDefault;
  }

  @Override
  public <T> Stream<T> keys(DynamicOps<T> ops) {
    return Stream.of(ops.createString(name));
  }

  @Override
  public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> input) {
    final T value = input.get(name);
    if (value == null) {
      return DataResult.success(defaultValue);
    }
    return elementCodec.parse(ops, value);
  }

  @Override
  public <T> RecordBuilder<T> encode(A input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
    if (!emitWhenDefault || !defaultValue.equals(input)) {
      return prefix.add(name, elementCodec.encodeStart(ops, input));
    }
    return prefix;
  }

  @Override
  public final boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StrictOptionalFieldCodec<?> that)) return false;

    return emitWhenDefault == that.emitWhenDefault && name.equals(that.name) && elementCodec.equals(that.elementCodec) && defaultValue.equals(that.defaultValue);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + elementCodec.hashCode();
    result = 31 * result + defaultValue.hashCode();
    result = 31 * result + Boolean.hashCode(emitWhenDefault);
    return result;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("name", name)
        .append("elementCodec", elementCodec)
        .append("defaultValue", defaultValue)
        .append("emitWhenDefault", emitWhenDefault)
        .toString();
  }
}
